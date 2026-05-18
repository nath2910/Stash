import Panzoom from '@panzoom/panzoom'
import type { Ref } from 'vue'
import { ref } from 'vue'

type InitOptions = {
  boardWidth: number
  boardHeight: number
  maxScale?: number
  minScale?: number
  contain?: any
  excludeClass?: string
  panIgnoreSelector?: string
}

const ALWAYS_IGNORE_PAN_SELECTOR = [
  'button',
  '[role="button"]',
  'a[href]',
  'input',
  'select',
  'textarea',
  'option',
  'summary',
  '[contenteditable="true"]',
  '[data-pan-ignore]',
  '[data-widget-control]',
  '.interactive',
  '.widget-control',
  '.widget__actions',
  '.widget__floating-actions',
  '.widget-action-menu',
  '.text-toolbar',
  '.text-inline-editor',
  '.resize-handle',
  '.resize-edge',
  '.text-scale-handle',
  '.group-selection-handle',
  '.canvas-empty-guide',
  '.template-rail',
  '.template-picker-modal',
  '.profile-modal',
  '.shortcut-modal',
  '.echarts',
  '.vue-echarts',
].join(', ')

const DEFAULT_PAN_CONTENT_SELECTOR = ['.widget', '.widget-card'].join(', ')
const WHEEL_PAN_SPEED = 1.55

export function useCanvasCamera(
  viewportEl: Ref<HTMLElement | null>,
  boardEl: Ref<HTMLElement | null>,
  opts: InitOptions,
) {
  const scale = ref(1)
  let panzoom: any = null
  let ro: ResizeObserver | null = null
  let didReady = false
  let resizeRaf: number | null = null
  let wheelZoomRaf: number | null = null
  let wheelTargetScale: number | null = null
  let wheelAnchor: { x: number; y: number } | null = null
  let panRaf: number | null = null
  let pendingPanDx = 0
  let pendingPanDy = 0
  let pointerPanState:
    | {
        pointerId: number
        lastX: number
        lastY: number
        captureTarget: HTMLElement | null
      }
    | null = null
  let wheelHandler: ((e: WheelEvent) => void) | null = null
  let pointerDownHandler: ((e: PointerEvent) => void) | null = null
  let pointerMoveHandler: ((e: PointerEvent) => void) | null = null
  let pointerUpHandler: ((e: PointerEvent) => void) | null = null
  let customPanEnabled = true
  let fitScale = opts.minScale ?? 0.15
  const activeTouchPointers = new Map<
    number,
    { clientX: number; clientY: number; target: EventTarget | null }
  >()
  let touchPanState: { lastX: number; lastY: number } | null = null
  let touchPinchState:
    | {
        lastDistance: number
        anchorX: number
        anchorY: number
      }
    | null = null

  const BOARD_W = opts.boardWidth
  const BOARD_H = opts.boardHeight
  const EXCLUDE_CLASS = opts.excludeClass ?? 'panzoom-exclude'
  const PAN_CONTENT_SELECTOR = opts.panIgnoreSelector ?? DEFAULT_PAN_CONTENT_SELECTOR

  function clampNumber(value: number, min: number, max: number) {
    return Math.min(Math.max(value, min), max)
  }

  function eventElement(target: EventTarget | null) {
    return target instanceof Element ? target : null
  }

  function closestFrom(el: Element | null, selector: string) {
    if (!el || !selector) return null
    try {
      return el.closest(selector)
    } catch {
      return null
    }
  }

  function htmlElementFrom(target: EventTarget | null) {
    const el = eventElement(target)
    if (!el) return null
    if (el instanceof HTMLElement) return el
    return el.parentElement
  }

  function getRects() {
    const vp = viewportEl.value
    const board = boardEl.value
    if (!vp || !board) return null

    const vpRect = vp.getBoundingClientRect()
    const boardRect = board.getBoundingClientRect()
    if (vpRect.width < 50 || vpRect.height < 50) return null
    if (boardRect.width < 50 || boardRect.height < 50) return null

    const s = boardRect.width / BOARD_W
    if (!Number.isFinite(s) || s <= 0) return null

    return { vpRect, boardRect, s }
  }

  function onPanzoomChange() {
    if (!panzoom) return
    scale.value = Number(panzoom.getScale?.() ?? 1)
  }

  function setPanningCursor(active: boolean) {
    document.body.classList.toggle('canvas-pan-cursor', active)
  }

  function cancelQueuedPan() {
    if (panRaf != null) {
      cancelAnimationFrame(panRaf)
      panRaf = null
    }
    pendingPanDx = 0
    pendingPanDy = 0
  }

  function queuePanBy(dx: number, dy: number) {
    if (!panzoom || !Number.isFinite(dx) || !Number.isFinite(dy)) return
    if (Math.abs(dx) < 0.01 && Math.abs(dy) < 0.01) return
    pendingPanDx += dx
    pendingPanDy += dy
    if (panRaf != null) return

    panRaf = requestAnimationFrame(() => {
      panRaf = null
      const nextDx = pendingPanDx
      const nextDy = pendingPanDy
      pendingPanDx = 0
      pendingPanDy = 0
      if (!panzoom || (Math.abs(nextDx) < 0.01 && Math.abs(nextDy) < 0.01)) return
      panzoom.pan(nextDx, nextDy, { relative: true, force: true } as any)
      onPanzoomChange()
    })
  }

  function panByNow(dx: number, dy: number) {
    if (!panzoom || !Number.isFinite(dx) || !Number.isFinite(dy)) return
    if (Math.abs(dx) < 0.01 && Math.abs(dy) < 0.01) return
    panzoom.pan(dx, dy, { relative: true, force: true } as any)
    onPanzoomChange()
  }

  function cancelWheelZoom() {
    if (wheelZoomRaf != null) {
      cancelAnimationFrame(wheelZoomRaf)
      wheelZoomRaf = null
    }
    wheelTargetScale = null
    wheelAnchor = null
  }

  function normalizeWheelDelta(event: WheelEvent) {
    const axisDelta =
      event.shiftKey && Math.abs(event.deltaX) > Math.abs(event.deltaY) ? event.deltaX : event.deltaY
    if (!Number.isFinite(axisDelta) || axisDelta === 0) return 0
    if (event.deltaMode === WheelEvent.DOM_DELTA_LINE) return axisDelta * 16
    if (event.deltaMode === WheelEvent.DOM_DELTA_PAGE) return axisDelta * 120
    return axisDelta
  }

  function normalizeWheelPanDelta(event: WheelEvent) {
    const multiplier =
      event.deltaMode === WheelEvent.DOM_DELTA_LINE
        ? 16
        : event.deltaMode === WheelEvent.DOM_DELTA_PAGE
          ? Math.max(window.innerHeight * 0.86, 120)
          : 1
    let dx = event.deltaX * multiplier
    let dy = event.deltaY * multiplier

    if (event.shiftKey && Math.abs(dx) < Math.abs(dy)) {
      dx = dy
      dy = 0
    }

    return { dx, dy }
  }

  function shouldZoomFromWheel(event: WheelEvent) {
    if (event.ctrlKey || event.metaKey) return true
    if (event.deltaMode !== WheelEvent.DOM_DELTA_PIXEL) return true
    if (Math.abs(event.deltaX) > Math.abs(event.deltaY) * 0.35) return false
    return Math.abs(event.deltaY) >= 48
  }

  function shouldIgnorePanTarget(target: EventTarget | null) {
    const el = eventElement(target)
    if (!el) return false
    if (closestFrom(el, ALWAYS_IGNORE_PAN_SELECTOR)) return true

    const configuredExclude = panzoom?.getOptions?.().excludeClass
    const activeExclude = configuredExclude === undefined ? EXCLUDE_CLASS : configuredExclude
    if (activeExclude && closestFrom(el, `.${activeExclude}`)) return true

    const isExplicitSpacePan = Boolean(closestFrom(el, '.canvas-root.is-space-pan'))
    if (!isExplicitSpacePan && closestFrom(el, PAN_CONTENT_SELECTOR)) return true

    return false
  }

  function boardPointFromViewport(vx: number, vy: number) {
    const r = getRects()
    if (!r) return { x: BOARD_W / 2, y: BOARD_H / 2 }

    const x = (r.vpRect.left + vx - r.boardRect.left) / r.s
    const y = (r.vpRect.top + vy - r.boardRect.top) / r.s

    return {
      x: Number.isFinite(x) ? x : BOARD_W / 2,
      y: Number.isFinite(y) ? y : BOARD_H / 2,
    }
  }

  function boardPointFromViewportCenter() {
    const r = getRects()
    if (!r) return { x: BOARD_W / 2, y: BOARD_H / 2 }
    return boardPointFromViewport(r.vpRect.width / 2, r.vpRect.height / 2)
  }

  function findScrollableAncestor(target: HTMLElement | null, stopAt: HTMLElement | null) {
    let el = target
    while (el && el !== stopAt && el !== document.body) {
      const style = window.getComputedStyle(el)
      const overflowY = style.overflowY
      const overflowX = style.overflowX
      const canScrollY =
        (overflowY === 'auto' || overflowY === 'scroll') && el.scrollHeight > el.clientHeight
      const canScrollX =
        (overflowX === 'auto' || overflowX === 'scroll') && el.scrollWidth > el.clientWidth
      if (canScrollY || canScrollX) return el
      el = el.parentElement
    }
    return null
  }

  function centerOn(boardX: number, boardY: number) {
    if (!panzoom) return
    const r = getRects()
    if (!r) return

    const px = r.boardRect.left + boardX * r.s - r.vpRect.left
    const py = r.boardRect.top + boardY * r.s - r.vpRect.top

    const dx = r.vpRect.width / 2 - px
    const dy = r.vpRect.height / 2 - py

    panzoom.pan(dx, dy, { relative: true, force: true } as any)
  }

  function computeFitScale() {
    const r = getRects()
    if (!r) return null
    const scaleX = r.vpRect.width / BOARD_W
    const scaleY = r.vpRect.height / BOARD_H
    const target = Math.min(scaleX, scaleY) * 0.94
    const min = opts.minScale ?? 0.15
    const max = opts.maxScale ?? 3
    const clamped = Math.min(Math.max(target, min), max)
    return Number.isFinite(clamped) ? clamped : null
  }

  function syncScaleBounds(nextFitScale?: number) {
    if (!panzoom) return
    const fit = nextFitScale ?? fitScale ?? opts.minScale ?? 0.15
    const absoluteMin = opts.minScale ?? 0.15
    const absoluteMax = opts.maxScale ?? 3
    const dynamicMin = Math.max(absoluteMin, Math.min(fit * 0.72, fit))
    panzoom.setOptions?.({ minScale: dynamicMin, maxScale: absoluteMax })
  }

  function applyFit(center = true, animate = false) {
    if (!panzoom) return
    const nextFitScale = computeFitScale()
    if (!nextFitScale) return
    fitScale = nextFitScale
    syncScaleBounds(nextFitScale)
    panzoom.zoom(nextFitScale, { animate } as any)
    if (center) centerOn(BOARD_W / 2, BOARD_H / 2)
    onPanzoomChange()
  }

  function clampScale(targetScale: number) {
    const minScale = Number(panzoom?.getOptions?.().minScale ?? opts.minScale ?? 0.15)
    const maxScale = Number(panzoom?.getOptions?.().maxScale ?? opts.maxScale ?? 3)
    return Math.min(Math.max(targetScale, minScale), maxScale)
  }

  function zoomToPoint(targetScale: number, vx: number, vy: number, options?: Record<string, unknown>) {
    if (!panzoom) return
    const r = getRects()
    if (!r) return
    const nextScale = clampScale(targetScale)
    panzoom.zoomToPoint(
      nextScale,
      {
        clientX: r.vpRect.left + vx,
        clientY: r.vpRect.top + vy,
      },
      options as any,
    )
    onPanzoomChange()
  }

  function scheduleSmoothWheelZoom() {
    if (wheelZoomRaf != null) return
    wheelZoomRaf = requestAnimationFrame(() => {
      wheelZoomRaf = null
      if (!panzoom || !wheelAnchor || wheelTargetScale == null) return
      const targetScale = clampScale(wheelTargetScale)
      const anchor = wheelAnchor
      wheelTargetScale = null
      wheelAnchor = null

      zoomToPoint(targetScale, anchor.x, anchor.y, {
        animate: false,
        force: true,
      })
    })
  }

  function zoomWithWheel(event: WheelEvent) {
    if (!panzoom) return
    const vp = viewportEl.value
    if (!vp) return
    const delta = clampNumber(normalizeWheelDelta(event), -220, 220)
    if (!delta) return
    cancelQueuedPan()
    const currentScale = Number(wheelTargetScale ?? panzoom.getScale?.() ?? scale.value ?? 1)
    const intensity = event.ctrlKey || event.metaKey ? 0.0022 : 0.00135
    const nextScale = clampScale(currentScale * Math.exp(-delta * intensity))
    const rect = vp.getBoundingClientRect()
    wheelTargetScale = nextScale
    wheelAnchor = {
      x: event.clientX - rect.left,
      y: event.clientY - rect.top,
    }
    scheduleSmoothWheelZoom()
  }

  function endPointerPan(event?: PointerEvent) {
    if (!pointerPanState) return
    if (event && event.pointerId !== pointerPanState.pointerId) return
    const { pointerId, captureTarget } = pointerPanState
    pointerPanState = null
    if (captureTarget?.hasPointerCapture?.(pointerId)) {
      try {
        captureTarget.releasePointerCapture(pointerId)
      } catch {
        // Ignore stale pointer capture errors from interrupted gestures.
      }
    }
    setPanningCursor(false)
  }

  function beginPointerPan(event: PointerEvent) {
    if (!panzoom || !customPanEnabled) return false
    if (event.pointerType === 'touch') return false
    if (event.button !== 0 && event.button !== 1) return false
    if (event.shiftKey || event.ctrlKey || event.metaKey || event.altKey) return false

    const vp = viewportEl.value
    if (!vp) return false
    const target = htmlElementFrom(event.target)
    if (shouldIgnorePanTarget(event.target)) return false
    if (findScrollableAncestor(target, vp)) return false

    event.preventDefault()
    cancelWheelZoom()
    cancelQueuedPan()

    pointerPanState = {
      pointerId: event.pointerId,
      lastX: event.clientX,
      lastY: event.clientY,
      captureTarget: vp,
    }

    if (vp.setPointerCapture) {
      try {
        vp.setPointerCapture(event.pointerId)
      } catch {
        // Pointer capture is a progressive enhancement here.
      }
    }

    setPanningCursor(true)
    return true
  }

  function getTouchPair() {
    const points = Array.from(activeTouchPointers.values())
    if (points.length < 2) return null
    return [points[0], points[1]] as const
  }

  function distanceBetween(
    a: { clientX: number; clientY: number },
    b: { clientX: number; clientY: number },
  ) {
    return Math.hypot(b.clientX - a.clientX, b.clientY - a.clientY)
  }

  function midpointBetween(
    a: { clientX: number; clientY: number },
    b: { clientX: number; clientY: number },
  ) {
    return {
      x: (a.clientX + b.clientX) / 2,
      y: (a.clientY + b.clientY) / 2,
    }
  }

  function beginTouchPan(point: { clientX: number; clientY: number; target: EventTarget | null }) {
    if (!customPanEnabled) {
      touchPanState = null
      return
    }
    const vp = viewportEl.value
    const target = htmlElementFrom(point.target)
    if (!vp) return
    if (shouldIgnorePanTarget(point.target)) {
      touchPanState = null
      return
    }
    if (findScrollableAncestor(target, vp)) {
      touchPanState = null
      return
    }
    cancelWheelZoom()
    touchPanState = { lastX: point.clientX, lastY: point.clientY }
  }

  function syncTouchGesture() {
    if (!panzoom) return
    if (activeTouchPointers.size >= 2) {
      touchPanState = null
      const pair = getTouchPair()
      if (!pair) return
      const [a, b] = pair
      const center = midpointBetween(a, b)
      const distance = distanceBetween(a, b)
      if (!Number.isFinite(distance) || distance <= 0) return
      const vp = viewportEl.value
      if (!vp) return
      const rect = vp.getBoundingClientRect()
      if (!touchPinchState) {
        touchPinchState = {
          lastDistance: distance,
          anchorX: center.x - rect.left,
          anchorY: center.y - rect.top,
        }
        return
      }

      const distanceDelta = distance - touchPinchState.lastDistance
      if (Math.abs(distanceDelta) < 2) return

      const currentScale = Number(panzoom.getScale?.() ?? scale.value ?? 1)
      const rawRatio = distance / touchPinchState.lastDistance
      const dampedRatio = 1 + (rawRatio - 1) * 0.42
      const nextScale = currentScale * dampedRatio
      touchPinchState.lastDistance = distance
      zoomToPoint(nextScale, touchPinchState.anchorX, touchPinchState.anchorY, { animate: false })
      return
    }

    touchPinchState = null
    const remaining = activeTouchPointers.values().next().value as
      | { clientX: number; clientY: number; target: EventTarget | null }
      | undefined
    if (!remaining) {
      touchPanState = null
      return
    }
    if (!touchPanState) beginTouchPan(remaining)
  }

  function zoomIn() {
    const r = getRects()
    if (!r) return
    const currentScale = Number(panzoom?.getScale?.() ?? scale.value ?? 1)
    zoomToPoint(currentScale * 1.12, r.vpRect.width / 2, r.vpRect.height / 2, { animate: true })
  }

  function zoomOut() {
    const r = getRects()
    if (!r) return
    const currentScale = Number(panzoom?.getScale?.() ?? scale.value ?? 1)
    zoomToPoint(currentScale / 1.12, r.vpRect.width / 2, r.vpRect.height / 2, { animate: true })
  }

  function zoomTo(targetScale: number, options?: Record<string, unknown>) {
    const r = getRects()
    if (!r) {
      panzoom?.zoom(targetScale, options as any)
      onPanzoomChange()
      return
    }
    zoomToPoint(targetScale, r.vpRect.width / 2, r.vpRect.height / 2, options)
  }

  function resetZoom() {
    applyFit(true, true)
  }

  function init(onReadyOnce?: () => void) {
    const board = boardEl.value
    const vp = viewportEl.value
    if (!board || !vp) return

    panzoom = Panzoom(board, {
      maxScale: opts.maxScale ?? 3.5,
      minScale: opts.minScale ?? 0.15,
      contain: opts.contain ?? 'outside',
      excludeClass: EXCLUDE_CLASS,
      touchAction: 'none',
      canvas: true,
      noBind: true,
      step: 0.12,
      duration: 180,
      easing: 'cubic-bezier(0.22, 1, 0.36, 1)',
      cursor: 'grab',
      handleStartEvent: (e: Event) => {
        e.preventDefault()
      },
    })

    wheelHandler = (e: WheelEvent) => {
      if (!panzoom) return

      const target = htmlElementFrom(e.target)
      if (shouldIgnorePanTarget(e.target)) return
      if (findScrollableAncestor(target, vp)) return

      const isZoomGesture = e.ctrlKey || e.metaKey
      if (isZoomGesture || shouldZoomFromWheel(e)) {
        e.preventDefault()
        zoomWithWheel(e)
        return
      }

      e.preventDefault()
      cancelWheelZoom()
      const { dx, dy } = normalizeWheelPanDelta(e)
      queuePanBy(-dx * WHEEL_PAN_SPEED, -dy * WHEEL_PAN_SPEED)
    }

    vp.addEventListener('wheel', wheelHandler, { passive: false })
    board.addEventListener('panzoomchange', onPanzoomChange as any)

    pointerDownHandler = (event: PointerEvent) => {
      if (event.pointerType === 'touch') {
        const target = htmlElementFrom(event.target)
        if (shouldIgnorePanTarget(event.target)) return
        if (findScrollableAncestor(target, vp)) return
        event.preventDefault()
        activeTouchPointers.set(event.pointerId, {
          clientX: event.clientX,
          clientY: event.clientY,
          target: event.target,
        })
        syncTouchGesture()
        return
      }
      beginPointerPan(event)
    }

    pointerMoveHandler = (event: PointerEvent) => {
      if (event.pointerType === 'touch') {
        const tracked = activeTouchPointers.get(event.pointerId)
        if (!tracked) return
        event.preventDefault()
        tracked.clientX = event.clientX
        tracked.clientY = event.clientY

        if (activeTouchPointers.size >= 2) {
          syncTouchGesture()
          return
        }

        if (!touchPanState || !customPanEnabled) return
        const dx = event.clientX - touchPanState.lastX
        const dy = event.clientY - touchPanState.lastY
        touchPanState.lastX = event.clientX
        touchPanState.lastY = event.clientY
        if (dx || dy) {
          panByNow(dx, dy)
        }
        return
      }
      if (!pointerPanState || event.pointerId !== pointerPanState.pointerId) return
      event.preventDefault()
      const dx = event.clientX - pointerPanState.lastX
      const dy = event.clientY - pointerPanState.lastY
      pointerPanState.lastX = event.clientX
      pointerPanState.lastY = event.clientY
      panByNow(dx, dy)
    }

    pointerUpHandler = (event: PointerEvent) => {
      if (event.pointerType === 'touch') {
        activeTouchPointers.delete(event.pointerId)
        syncTouchGesture()
        return
      }
      endPointerPan(event)
    }

    vp.addEventListener('pointerdown', pointerDownHandler, { capture: true })
    window.addEventListener('pointermove', pointerMoveHandler, { passive: false })
    window.addEventListener('pointerup', pointerUpHandler, { passive: false })
    window.addEventListener('pointercancel', pointerUpHandler, { passive: false })

    panzoom.reset?.({ force: true } as any)
    applyFit(false)
    onPanzoomChange()

    didReady = false
    const syncViewportSize = () => {
      const r = getRects()
      if (!r) return
      if (!didReady) {
        didReady = true
        requestAnimationFrame(() => {
          requestAnimationFrame(() => {
            applyFit(true)
            onReadyOnce?.()
          })
        })
        return
      }

      const center = boardPointFromViewportCenter()
      const currentScale = Number(panzoom.getScale?.() ?? scale.value ?? fitScale ?? 1)
      const nextFitScale = computeFitScale()
      if (!nextFitScale) return
      fitScale = nextFitScale
      syncScaleBounds(nextFitScale)
      const minScale = Number(panzoom.getOptions?.().minScale ?? opts.minScale ?? 0.15)
      panzoom.zoom(Math.max(currentScale, minScale), { force: true } as any)
      centerOn(center.x, center.y)
      onPanzoomChange()
    }

    ro = new ResizeObserver(() => {
      if (resizeRaf != null) return
      resizeRaf = requestAnimationFrame(() => {
        resizeRaf = null
        syncViewportSize()
      })
    })
    ro.observe(vp)
  }

  function destroy() {
    const board = boardEl.value
    const vp = viewportEl.value

    if (ro) {
      ro.disconnect()
      ro = null
    }
    if (resizeRaf != null) {
      cancelAnimationFrame(resizeRaf)
      resizeRaf = null
    }
    if (wheelZoomRaf != null) {
      cancelAnimationFrame(wheelZoomRaf)
      wheelZoomRaf = null
    }
    wheelTargetScale = null
    wheelAnchor = null
    cancelQueuedPan()
    endPointerPan()
    setPanningCursor(false)
    if (vp && wheelHandler) vp.removeEventListener('wheel', wheelHandler)
    if (vp && pointerDownHandler) vp.removeEventListener('pointerdown', pointerDownHandler, true)
    if (pointerMoveHandler) window.removeEventListener('pointermove', pointerMoveHandler)
    if (pointerUpHandler) {
      window.removeEventListener('pointerup', pointerUpHandler)
      window.removeEventListener('pointercancel', pointerUpHandler)
    }
    if (board) board.removeEventListener('panzoomchange', onPanzoomChange as any)

    panzoom?.destroy?.()
    panzoom = null
    wheelHandler = null
    pointerDownHandler = null
    pointerMoveHandler = null
    pointerUpHandler = null
    customPanEnabled = true
    fitScale = opts.minScale ?? 0.15
    activeTouchPointers.clear()
    touchPanState = null
    touchPinchState = null
  }

  return {
    scale,
    init,
    destroy,
    zoomIn,
    zoomOut,
    resetZoom,
    zoomTo,
    centerOn,
    setPanEnabled(enabled: boolean) {
      customPanEnabled = enabled
      if (!enabled) {
        endPointerPan()
        cancelQueuedPan()
      }
      panzoom?.setOptions?.({ disablePan: !enabled })
    },
    boardPointFromViewport,
    boardPointFromViewportCenter,
    fitToViewport: () => applyFit(true, true),
    getPanzoom: () => panzoom,
  }
}
