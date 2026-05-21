export type CanvasWidget = {
  id: string
  type: string
  title?: string
  x: number
  y: number
  w: number
  h: number
  props?: Record<string, unknown>
  z?: number
}

export type WidgetDateDef = {
  hideGlobalRange?: boolean
  dateMode?: 'range' | 'asOf' | string
}

type DuplicateGroupOptions = {
  createId: (type: string, source: CanvasWidget) => string
  cloneProps: (props: Record<string, unknown>) => Record<string, unknown>
  boardWidth: number
  boardHeight: number
  offset?: number
  zStart?: number
}

function clampCoord(value: number, size: number, boardSize: number) {
  const max = Math.max(0, boardSize - Math.max(0, size))
  return Math.max(0, Math.min(value, max))
}

function chooseAxisOffset(min: number, max: number, boardSize: number, offset: number) {
  if (max + offset <= boardSize) return offset
  if (min - offset >= 0) return -offset

  const forwardRoom = Math.max(0, boardSize - max)
  if (forwardRoom > 0) return Math.min(offset, forwardRoom)

  const backwardRoom = Math.max(0, min)
  if (backwardRoom > 0) return -Math.min(offset, backwardRoom)

  return 0
}

export function duplicateWidgetGroup<TWidget extends CanvasWidget>(
  sources: TWidget[],
  options: DuplicateGroupOptions,
) {
  if (!Array.isArray(sources) || sources.length === 0) {
    return { duplicates: [] as TWidget[], nextZ: options.zStart ?? 0, offset: { x: 0, y: 0 } }
  }

  const offset = Number.isFinite(options.offset) ? Number(options.offset) : 60
  const bounds = sources.reduce(
    (acc, widget) => ({
      left: Math.min(acc.left, widget.x),
      top: Math.min(acc.top, widget.y),
      right: Math.max(acc.right, widget.x + widget.w),
      bottom: Math.max(acc.bottom, widget.y + widget.h),
    }),
    { left: Infinity, top: Infinity, right: -Infinity, bottom: -Infinity },
  )

  const dx = chooseAxisOffset(bounds.left, bounds.right, options.boardWidth, offset)
  const dy = chooseAxisOffset(bounds.top, bounds.bottom, options.boardHeight, offset)
  let z = Number.isFinite(options.zStart) ? Number(options.zStart) : 0

  const duplicates = sources.map((source) => {
    z += 1
    return {
      ...source,
      id: options.createId(source.type, source),
      x: clampCoord(source.x + dx, source.w, options.boardWidth),
      y: clampCoord(source.y + dy, source.h, options.boardHeight),
      props: options.cloneProps(source.props ?? {}),
      z,
    } as TWidget
  })

  return { duplicates, nextZ: z, offset: { x: dx, y: dy } }
}

export function buildCommonDatePatch(def: WidgetDateDef | null | undefined, date: string) {
  return buildCommonDateRangePatch(def, date, date)
}

export function buildCommonDateRangePatch(
  def: WidgetDateDef | null | undefined,
  from: string,
  to: string,
) {
  const start = String(from ?? '').trim()
  const end = String(to ?? '').trim()
  if (!start || !end || !def || def.hideGlobalRange === true) return null
  const normalizedFrom = start <= end ? start : end
  const normalizedTo = start <= end ? end : start

  if (def.dateMode === 'asOf') {
    return {
      useGlobalRange: false,
      asOf: normalizedTo,
    }
  }

  const mode = def.dateMode ?? 'range'
  if (mode !== 'range') return null

  return {
    useGlobalRange: false,
    from: normalizedFrom,
    to: normalizedTo,
  }
}
