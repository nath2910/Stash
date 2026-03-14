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
}

export function useCanvasCamera(
  viewportEl: Ref<HTMLElement | null>,
  boardEl: Ref<HTMLElement | null>,
  opts: InitOptions,
) {
  const scale = ref(1)
  let panzoom: any = null
  let ro: ResizeObserver | null = null
  let didReady = false
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
  let touchPinchState: { startScale: number; startDistance: number } | null = null

  const BOARD_W = opts.boardWidth
  const BOARD_H = opts.boardHeight
  const EXCLUDE_CLASS = opts.excludeClass ?? 'panzoom-exclude'

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

  function normalizeWheelDelta(event: WheelEvent) {
    const axisDelta =
      event.shiftKey && Math.abs(event.deltaX) > Math.abs(event.deltaY) ? event.deltaX : event.deltaY
    if (!Number.isFinite(axisDelta) || axisDelta === 0) return 0
    if (event.deltaMode === WheelEvent.DOM_DELTA_LINE) return axisDelta * 16
    if (event.deltaMode === WheelEvent.DOM_DELTA_PAGE) return axisDelta * 120
    return axisDelta
  }

  function shouldZoomFromWheel(event: WheelEvent) {
    if (event.ctrlKey || event.metaKey) return true
    return event.deltaMode !== 0 || Math.abs(event.deltaY) >= 24 || Math.abs(event.deltaX) >= 24
  }

  function isExcludedTarget(target: EventTarget | null) {
    const el = target instanceof HTMLElement ? target : null
    if (!el) return false
    if (el.closest('input, textarea, select, option, [contenteditable="true"]')) return true
    const activeExclude = panzoom?.getOptions?.().excludeClass ?? EXCLUDE_CLASS
    return !!(activeExclude && el.closest(`.${activeExclude}`))
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

  function zoomToPoint(targetScale: number, vx: number, vy: number, options?: Record<string, unknown>) {
    if (!panzoom) return
    const r = getRects()
    if (!r) return
    const minScale = Number(panzoom.getOptions?.().minScale ?? opts.minScale ?? 0.15)
    const maxScale = Number(panzoom.getOptions?.().maxScale ?? opts.maxScale ?? 3)
    const nextScale = Math.min(Math.max(targetScale, minScale), maxScale)
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

  function zoomWithWheel(event: WheelEvent) {
    if (!panzoom) return
    const vp = viewportEl.value
    if (!vp) return
    const delta = normalizeWheelDelta(event)
    if (!delta) return
    const currentScale = Number(panzoom.getScale?.() ?? scale.value ?? 1)
    const intensity = event.ctrlKey || event.metaKey ? 0.0036 : 0.00235
    const nextScale = currentScale * Math.exp(-delta * intensity)
    const rect = vp.getBoundingClientRect()
    zoomToPoint(nextScale, event.clientX - rect.left, event.clientY - rect.top, {
      animate: false,
    })
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
    const target = point.target instanceof HTMLElement ? point.target : null
    if (!vp) return
    if (isExcludedTarget(point.target)) {
      touchPanState = null
      return
    }
    if (findScrollableAncestor(target, vp)) {
      touchPanState = null
      return
    }
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
      if (!touchPinchState) {
        touchPinchState = {
          startScale: Number(panzoom.getScale?.() ?? scale.value ?? 1),
          startDistance: distance,
        }
        return
      }

      const vp = viewportEl.value
      if (!vp) return
      const rect = vp.getBoundingClientRect()
      const nextScale = touchPinchState.startScale * (distance / touchPinchState.startDistance)
      zoomToPoint(nextScale, center.x - rect.left, center.y - rect.top, { animate: false })
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

      const isZoomGesture = e.ctrlKey || e.metaKey
      if (isZoomGesture || shouldZoomFromWheel(e)) {
        e.preventDefault()
        zoomWithWheel(e)
        return
      }

      const activeExclude = panzoom?.getOptions?.().excludeClass ?? EXCLUDE_CLASS
      const target = e.target as HTMLElement | null
      if (target && activeExclude && target.closest(`.${activeExclude}`)) return
      if (target && target.closest('input, textarea, select, option, [contenteditable="true"]')) {
        return
      }
      if (findScrollableAncestor(target, vp)) return

      e.preventDefault()
      panzoom.pan(e.deltaX, e.deltaY, { relative: true, force: true } as any)
    }

    vp.addEventListener('wheel', wheelHandler, { passive: false })
    board.addEventListener('panzoomchange', onPanzoomChange as any)

    pointerDownHandler = (event: PointerEvent) => {
      if (event.pointerType === 'touch') {
        event.preventDefault()
        activeTouchPointers.set(event.pointerId, {
          clientX: event.clientX,
          clientY: event.clientY,
          target: event.target,
        })
        syncTouchGesture()
        return
      }
      panzoom.handleDown?.(event)
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
          panzoom.pan(dx, dy, { relative: true, force: true } as any)
          onPanzoomChange()
        }
        return
      }
      panzoom.handleMove?.(event)
    }

    pointerUpHandler = (event: PointerEvent) => {
      if (event.pointerType === 'touch') {
        activeTouchPointers.delete(event.pointerId)
        syncTouchGesture()
        return
      }
      panzoom.handleUp?.(event)
    }

    vp.addEventListener('pointerdown', pointerDownHandler, { capture: true })
    window.addEventListener('pointermove', pointerMoveHandler, { passive: false })
    window.addEventListener('pointerup', pointerUpHandler, { passive: false })
    window.addEventListener('pointercancel', pointerUpHandler, { passive: false })

    panzoom.reset?.({ force: true } as any)
    applyFit(false)
    onPanzoomChange()

    didReady = false
    ro = new ResizeObserver(() => {
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
      panzoom?.setOptions?.({ disablePan: !enabled })
    },
    boardPointFromViewport,
    boardPointFromViewportCenter,
    fitToViewport: () => applyFit(true, true),
    getPanzoom: () => panzoom,
  }
}
