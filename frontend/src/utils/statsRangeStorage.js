export const STATS_RANGE_STORAGE_KEY_PREFIX = 'snk_stats_range_v2'

function toYmdLocal(value) {
  const date = value instanceof Date ? value : new Date(value)
  if (Number.isNaN(date.getTime())) return ''
  const pad = (part) => String(part).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}

export function getMonthAnchor(baseDate = new Date()) {
  const month = String(baseDate.getMonth() + 1).padStart(2, '0')
  return `${baseDate.getFullYear()}-${month}`
}

export function getMonthToDateRange(baseDate = new Date()) {
  return {
    from: toYmdLocal(new Date(baseDate.getFullYear(), baseDate.getMonth(), 1)),
    to: toYmdLocal(baseDate),
  }
}

export function normalizeSimpleRange(raw) {
  if (!raw || typeof raw !== 'object') return null
  const from = typeof raw.from === 'string' ? raw.from : ''
  const to = typeof raw.to === 'string' ? raw.to : ''
  if (!/^\d{4}-\d{2}-\d{2}$/.test(from) || !/^\d{4}-\d{2}-\d{2}$/.test(to)) return null
  return from <= to ? { from, to } : { from: to, to: from }
}

export function buildStoredStatsRange(from, to, options = {}) {
  const range = normalizeSimpleRange({ from, to })
  if (!range) return null
  const baseDate = options.baseDate instanceof Date ? options.baseDate : new Date()
  const preset =
    options.preset || (isMonthToDateRange(range, baseDate) ? 'month_to_date' : 'custom')

  return {
    ...range,
    preset,
    anchor: preset === 'month_to_date' ? getMonthAnchor(baseDate) : null,
    touchedAt: baseDate.toISOString(),
  }
}

export function resolveStoredStatsRange(raw, options = {}) {
  const baseDate = options.baseDate instanceof Date ? options.baseDate : new Date()
  const fallbackPreset = options.fallbackPreset || 'month_to_date'
  const defaultRange = getMonthToDateRange(baseDate)

  if (!raw || typeof raw !== 'object') {
    return buildStoredStatsRange(defaultRange.from, defaultRange.to, {
      baseDate,
      preset: fallbackPreset,
    })
  }

  const preset = typeof raw.preset === 'string' ? raw.preset : ''
  const anchor = typeof raw.anchor === 'string' ? raw.anchor : ''
  const range = normalizeSimpleRange(raw)

  if (!preset || !range) {
    return buildStoredStatsRange(defaultRange.from, defaultRange.to, {
      baseDate,
      preset: fallbackPreset,
    })
  }

  if (preset === 'month_to_date' && anchor !== getMonthAnchor(baseDate)) {
    return buildStoredStatsRange(defaultRange.from, defaultRange.to, {
      baseDate,
      preset: 'month_to_date',
    })
  }

  return buildStoredStatsRange(range.from, range.to, { baseDate, preset })
}

export function isMonthToDateRange(range, baseDate = new Date()) {
  const normalized = normalizeSimpleRange(range)
  if (!normalized) return false
  const current = getMonthToDateRange(baseDate)
  return normalized.from === current.from && normalized.to === current.to
}
