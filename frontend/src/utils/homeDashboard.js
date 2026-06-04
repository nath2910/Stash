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
    .replace(/['’`]/g, ' ')
    .replace(/[^a-zA-Z0-9]+/g, ' ')
    .toLowerCase()
    .trim()
    .replace(/\s+/g, ' ')
}

export function searchTokens(value) {
  return normalizeSearchText(value).split(/\s+/).filter(Boolean)
}

function compactSearchText(value) {
  return normalizeSearchText(value).replace(/\s+/g, '')
}

function maxDistanceForToken(token) {
  if (token.length < 3) return 0
  if (token.length <= 5) return 1
  return 2
}

function editDistanceWithin(a, b, maxDistance) {
  if (!maxDistance && a !== b) return false
  if (Math.abs(a.length - b.length) > maxDistance) return false
  if (a.length > 32 || b.length > 32) return false

  let previous = Array.from({ length: b.length + 1 }, (_, index) => index)
  for (let i = 1; i <= a.length; i += 1) {
    const current = [i]
    let rowMin = current[0]
    for (let j = 1; j <= b.length; j += 1) {
      const cost = a[i - 1] === b[j - 1] ? 0 : 1
      const value = Math.min(
        previous[j] + 1,
        current[j - 1] + 1,
        previous[j - 1] + cost,
      )
      current[j] = value
      rowMin = Math.min(rowMin, value)
    }
    if (rowMin > maxDistance) return false
    previous = current
  }

  return previous[b.length] <= maxDistance
}

function tokenMatchesWord(token, word) {
  if (!token || !word) return false
  if (word.includes(token)) return true
  if (token.length >= 4 && word.length >= 3 && token.includes(word)) return true

  const maxDistance = maxDistanceForToken(token)
  return maxDistance > 0 && editDistanceWithin(token, word, maxDistance)
}

export function matchesSearchQuery(source, query) {
  const normalizedQuery = normalizeSearchText(query)
  if (!normalizedQuery) return true

  const rawSource = Array.isArray(source) ? source.join(' ') : source
  const haystack = normalizeSearchText(rawSource)
  if (!haystack) return false
  if (haystack.includes(normalizedQuery)) return true

  const compactHaystack = compactSearchText(haystack)
  const compactQuery = compactSearchText(normalizedQuery)
  if (compactQuery.length >= 3 && compactHaystack.includes(compactQuery)) return true

  const words = haystack.split(/\s+/).filter(Boolean)
  return searchTokens(normalizedQuery).every((token) => {
    if (compactHaystack.includes(token)) return true
    return words.some((word) => tokenMatchesWord(token, word))
  })
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
