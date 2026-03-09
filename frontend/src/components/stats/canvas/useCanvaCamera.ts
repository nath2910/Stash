import Panzoom from '@panzoom/panzoom'
import type { Ref } from 'vue'
import { ref } from 'vue'

// gere la camera de la page stat

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
  let touchStartHandler: ((e: TouchEvent) => void) | null = null
  let touchEndHandler: ((e: TouchEvent) => void) | null = null
  let touchBase: { excludeClass: any; disablePan: boolean; disableZoom: boolean } | null = null

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

    // ✅ échelle VISUELLE réelle (hyper robuste)
    const s = boardRect.width / BOARD_W
    if (!Number.isFinite(s) || s <= 0) return null

    return { vpRect, boardRect, s }
  }

  function onPanzoomChange() {
    if (!panzoom) return
    scale.value = Number(panzoom.getScale?.() ?? 1)
  }

  function boardPointFromViewport(vx: number, vy: number) {
    const r = getRects()
    if (!r) return { x: BOARD_W / 2, y: BOARD_H / 2 }

    // vx/vy = coords dans le viewport (0..width / 0..height)
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

  function captureTouchBase() {
    if (!panzoom || touchBase) return
    const opts = panzoom.getOptions?.() ?? {}
    touchBase = {
      excludeClass: opts.excludeClass ?? EXCLUDE_CLASS,
      disablePan: !!opts.disablePan,
      disableZoom: !!opts.disableZoom,
    }
  }

  function restoreTouchBase() {
    if (!panzoom || !touchBase) return
    panzoom.setOptions?.({
      excludeClass: touchBase.excludeClass,
      disablePan: touchBase.disablePan,
      disableZoom: touchBase.disableZoom,
    })
    touchBase = null
  }

  function applyTouchMode(touchCount: number) {
    if (!panzoom) return
    captureTouchBase()
    if (!touchBase) return

    if (touchCount >= 2) {
      panzoom.setOptions?.({ excludeClass: null, disablePan: false, disableZoom: false })
      return
    }

    if (touchCount === 1) {
      panzoom.setOptions?.({ excludeClass: null, disablePan: false, disableZoom: true })
      return
    }

    restoreTouchBase()
  }
  /** centre un point board au centre du viewport (pan RELATIF => zéro drift) */
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

  function applyFit(center = true, animate = false) {
    if (!panzoom) return
    const s = computeFitScale()
    if (!s) return
    panzoom.setOptions?.({ minScale: Math.max(s * 0.9, opts.minScale ?? 0.15) })
    panzoom.zoom(s, { animate } as any)
    if (center) centerOn(BOARD_W / 2, BOARD_H / 2)
  }

  function zoomIn() {
    if (!panzoom) return
    const anchor = boardPointFromViewportCenter()
    panzoom.zoomIn?.()
    centerOn(anchor.x, anchor.y)
  }
  function zoomOut() {
    if (!panzoom) return
    const anchor = boardPointFromViewportCenter()
    panzoom.zoomOut?.()
    centerOn(anchor.x, anchor.y)
  }
  function zoomTo(targetScale: number, options?: Record<string, unknown>) {
    if (!panzoom) return
    panzoom.zoom(targetScale, options as any)
  }
  function resetZoom() {
    if (!panzoom) return
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
      touchAction: 'pan-x pan-y',
      canvas: true, // ✅ Lucidchart feel (drag la “feuille”)
      cursor: 'grab',

      handleStartEvent: (e: Event) => {
        e.preventDefault()
        // PAS de e.stopPropagation()
      },
    })

    wheelHandler = (e: WheelEvent) => {
      if (!panzoom) return

      const isZoomGesture = e.ctrlKey || e.metaKey
      const isMouseWheel =
        e.deltaMode !== 0 || (Number.isInteger(e.deltaY) && Math.abs(e.deltaY) >= 50)
      if (isZoomGesture || isMouseWheel) {
        e.preventDefault()
        panzoom.zoomWithWheel?.(e)
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

    touchStartHandler = (e: TouchEvent) => {
      if (!panzoom) return
      applyTouchMode(e.touches ? e.touches.length : 0)
    }

    touchEndHandler = (e: TouchEvent) => {
      if (!panzoom) return
      applyTouchMode(e.touches ? e.touches.length : 0)
    }

    vp.addEventListener('touchstart', touchStartHandler, { passive: true, capture: true })
    vp.addEventListener('touchend', touchEndHandler, { passive: true })
    vp.addEventListener('touchcancel', touchEndHandler, { passive: true })

    // reset propre (évite les anciens transforms)
    panzoom.reset?.({ force: true } as any)
    applyFit(false)
    onPanzoomChange()

    // ✅ ready = quand le viewport a une vraie taille
    didReady = false
    ro = new ResizeObserver(() => {
      const r = getRects()
      if (!r) return
      if (!didReady) {
        didReady = true
        // double RAF = layout ultra stable
        requestAnimationFrame(() => {
          requestAnimationFrame(() => {
            applyFit(true)
            onReadyOnce?.()
          })
        })
      } else {
        applyFit(true)
      }
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
    if (vp && touchStartHandler)
      vp.removeEventListener('touchstart', touchStartHandler, { capture: true })
    if (vp && touchEndHandler) {
      vp.removeEventListener('touchend', touchEndHandler)
      vp.removeEventListener('touchcancel', touchEndHandler)
    }
    if (board) board.removeEventListener('panzoomchange', onPanzoomChange as any)

    panzoom = null
    wheelHandler = null
    touchStartHandler = null
    touchEndHandler = null
    touchBase = null
    touchBase = null
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
    boardPointFromViewport,
    boardPointFromViewportCenter,
    fitToViewport: () => applyFit(true, true),
    getPanzoom: () => panzoom,
  }
}
