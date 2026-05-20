type WidgetLike = {
  x?: unknown
  y?: unknown
  props?: Record<string, unknown> | null
  rotation?: unknown
  angle?: unknown
  orientation?: unknown
}

export function normalizeWidgetRotation(value: unknown): number {
  const raw = Number(value)
  if (!Number.isFinite(raw)) return 0
  const normalized = ((raw % 360) + 360) % 360
  return normalized > 180 ? normalized - 360 : normalized
}

export function rotationFromOrientation(value: unknown): number | null {
  const orientation = String(value ?? '')
    .trim()
    .toLowerCase()
  if (!orientation || orientation === 'horizontal') return null
  if (orientation === 'vertical' || orientation === 'vertical-left') return -90
  if (orientation === 'vertical-right') return 90
  if (orientation === 'inverted' || orientation === 'reverse') return 180
  return null
}

export function resolveWidgetRotation(widget: WidgetLike | null | undefined): number {
  const props = widget?.props ?? {}
  const direct =
    props.rotation ??
    props.rotate ??
    props.angle ??
    widget?.rotation ??
    widget?.angle
  if (direct !== undefined && direct !== null && direct !== '') {
    return normalizeWidgetRotation(direct)
  }
  return normalizeWidgetRotation(
    rotationFromOrientation(props.orientation ?? widget?.orientation) ?? 0,
  )
}

export function buildWidgetTransform(x: number, y: number, rotation = 0): string {
  const rotate = normalizeWidgetRotation(rotation)
  const base = `translate3d(${x}px, ${y}px, 0)`
  return rotate ? `${base} rotate(${rotate}deg)` : base
}
