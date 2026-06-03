import { getField, isVendue, prixResellOf, prixRetailOf } from './snkVente'

export function toYmdLocal(date) {
  const d = date instanceof Date ? date : new Date(date)
  if (Number.isNaN(d.getTime())) return ''
  const pad = (value) => String(value).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

export function getCurrentMonthRange(baseDate = new Date()) {
  const year = baseDate.getFullYear()
  const month = baseDate.getMonth()
  return {
    from: toYmdLocal(new Date(year, month, 1)),
    to: toYmdLocal(new Date(year, month + 1, 0)),
  }
}

export function getCurrentYearRange(baseDate = new Date()) {
  const year = baseDate.getFullYear()
  return {
    from: toYmdLocal(new Date(year, 0, 1)),
    to: toYmdLocal(new Date(year, 11, 31)),
  }
}

export function normalizeSearchText(value) {
  return String(value ?? '')
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .toLowerCase()
    .trim()
}

export function numberOrNull(value) {
  if (value === '' || value === null || value === undefined) return null
  const n = Number(value)
  return Number.isFinite(n) ? n : null
}

export function hasRawNumber(value) {
  return value !== null && value !== undefined && value !== '' && Number.isFinite(Number(value))
}

function parseYmdLocal(value) {
  if (!value) return null
  const [year, month, day] = String(value).slice(0, 10).split('-').map(Number)
  if (!year || !month || !day) return null
  const d = new Date(year, month - 1, day)
  return Number.isNaN(d.getTime()) ? null : d
}

function inRange(value, from, to) {
  const d = parseYmdLocal(value)
  const start = parseYmdLocal(from)
  const end = parseYmdLocal(to)
  if (!d || !start || !end) return false
  return d >= start && d <= end
}

export function calculatePeriodStats(items = [], range = getCurrentMonthRange()) {
  const rows = Array.isArray(items) ? items : []
  let ca = 0
  let profit = 0
  let itemsVendues = 0
  let itemsEnStock = 0
  let valeurStock = 0
  let estimatedStockValue = 0

  for (const item of rows) {
    const sold = isVendue(item)
    const dateVente = getField(item, 'dateVente')

    if (!sold) {
      itemsEnStock += 1
      const retail = prixRetailOf(item)
      const resell = prixResellOf(item)
      valeurStock += retail
      estimatedStockValue += resell > 0 ? resell : retail
    }

    if (!inRange(dateVente, range.from, range.to)) continue

    itemsVendues += 1
    const rawRetail = getField(item, 'prixRetail')
    const rawResell = getField(item, 'prixResell')
    if (hasRawNumber(rawResell)) ca += Number(rawResell)
    if (hasRawNumber(rawRetail) && hasRawNumber(rawResell)) {
      profit += Number(rawResell) - Number(rawRetail)
    }
  }

  return {
    ca,
    profit,
    profitMargin: ca > 0 ? profit / ca : 0,
    itemsVendues,
    itemsEnStock,
    valeurStock,
    estimatedStockValue,
  }
}

export const calculateMonthlyStats = calculatePeriodStats
