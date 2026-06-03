export function toNumber(value, fallback = 0) {
  const v = Number(value ?? fallback)
  return Number.isFinite(v) ? v : fallback
}

export function formatEUR(value, opts = {}) {
  const v = toNumber(value, 0)
  const { compact = false, digits } = opts
  const maxDigits = digits != null ? digits : compact ? 1 : 2
  return new Intl.NumberFormat('fr-FR', {
    style: 'currency',
    currency: 'EUR',
    notation: compact ? 'compact' : 'standard',
    maximumFractionDigits: maxDigits,
  }).format(v)
}

export const formatCurrency = formatEUR

export function formatNumber(value, opts = {}) {
  const v = toNumber(value, 0)
  const { compact = false, digits = compact ? 1 : 0 } = opts
  return new Intl.NumberFormat('fr-FR', {
    notation: compact ? 'compact' : 'standard',
    maximumFractionDigits: digits,
  }).format(v)
}

export function formatPct(value, opts = {}) {
  const v = toNumber(value, 0) / 100
  const { digits = 1 } = opts
  return new Intl.NumberFormat('fr-FR', {
    style: 'percent',
    maximumFractionDigits: digits,
  }).format(v)
}

export const formatPercent = formatPct

export function signFmt(value, suffix = '%') {
  const v = toNumber(value, 0)
  const s = v > 0 ? '+' : ''
  return `${s}${v.toFixed(1)}${suffix}`
}

export function formatDateFR(value, opts = {}) {
  const { fallback = '--', day = '2-digit', month = '2-digit', year = 'numeric' } = opts
  if (!value) return fallback
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return fallback
  return d.toLocaleDateString('fr-FR', { day, month, year })
}

export const formatDate = formatDateFR
