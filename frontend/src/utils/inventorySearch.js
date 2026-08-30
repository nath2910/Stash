import { METADATA_FIELDS } from '@/RegleItem/CategorieItem'
import { itemTypeLabel } from '@/RegleItem/itemCategoryStore'
import { normalizeSearchText, searchTokens } from './homeDashboard'
import {
  childItemsOf,
  getField,
  isGroupedItem,
  isVendue,
  itemQuantityOf,
  soldCountOf,
} from './snkVente'

const MAX_SEARCH_WORDS = 120
const SEARCH_EXTRA_FIELDS = [
  'marque',
  'brand',
  'tags',
  'tag',
  'labels',
  'label',
  'size',
  'taille',
  'pointure',
  'sku',
  'reference',
  'colorway',
  'couleur',
  'condition',
  'status',
  'statut',
  'venue',
  'lieu',
]

const compactText = (value) => normalizeSearchText(value).replace(/\s+/g, '')

function appendSearchPart(parts, value) {
  if (value === null || value === undefined || value === '') return
  if (Array.isArray(value)) {
    value.forEach((item) => appendSearchPart(parts, item))
    return
  }
  if (typeof value === 'object') {
    Object.entries(value).forEach(([key, nestedValue]) => {
      appendSearchPart(parts, key)
      appendSearchPart(parts, nestedValue)
    })
    return
  }
  parts.push(String(value))
}

export function createInventorySearchDescriptor(query) {
  const normalizedQuery = normalizeSearchText(query)
  return {
    query: normalizedQuery,
    compact: compactText(normalizedQuery),
    tokens: searchTokens(normalizedQuery),
  }
}

export function buildInventorySearchRecord(vente, categoryLabels = {}) {
  const type = vente?.type || 'SNEAKER'
  const metadata = vente?.metadata && typeof vente.metadata === 'object' ? vente.metadata : {}
  const metadataFieldLabels = new Map(
    (METADATA_FIELDS[type] || []).map((field) => [field.key, field.label]),
  )
  const children = childItemsOf(vente)
  const quantity = itemQuantityOf(vente)
  const soldCount = soldCountOf(vente)
  const grouped = isGroupedItem(vente)
  const parts = []

  ;[
    vente?.id,
    vente?.nomItem ?? vente?.nom_item,
    vente?.categorie,
    itemTypeLabel(type, categoryLabels),
    type,
    vente?.description,
    getField(vente, 'prixRetail'),
    getField(vente, 'prixResell'),
    getField(vente, 'dateAchat'),
    getField(vente, 'dateVente'),
    isVendue(vente) ? 'vendu vendue vente sold' : 'stock disponible en stock',
  ].forEach((value) => appendSearchPart(parts, value))

  SEARCH_EXTRA_FIELDS.forEach((field) => appendSearchPart(parts, getField(vente, field)))

  Object.entries(metadata).forEach(([key, value]) => {
    appendSearchPart(parts, key)
    appendSearchPart(parts, metadataFieldLabels.get(key))
    appendSearchPart(parts, value)
  })

  children.forEach((child) => {
    ;[
      child?.nomItem,
      child?.description,
      child?.categorie,
      getField(child, 'prixRetail'),
      getField(child, 'prixResell'),
      getField(child, 'dateAchat'),
      getField(child, 'dateVente'),
      child?.unitIndex,
    ].forEach((value) => appendSearchPart(parts, value))

    const childMetadata = child?.metadata && typeof child.metadata === 'object' ? child.metadata : {}
    Object.entries(childMetadata).forEach(([key, value]) => {
      appendSearchPart(parts, key)
      appendSearchPart(parts, value)
    })
  })

  const text = normalizeSearchText(parts.join(' '))
  return {
    vente,
    text,
    compact: text.replace(/\s+/g, ''),
    words: Array.from(new Set(text.split(/\s+/).filter(Boolean))).slice(0, MAX_SEARCH_WORDS),
    hasStock: grouped ? soldCount < quantity : !isVendue(vente),
  }
}

function maxDistanceForToken(token) {
  if (token.length < 3) return 0
  if (token.length <= 5) return 1
  return 2
}

function editDistanceWithin(a, b, maxDistance) {
  if (!maxDistance && a !== b) return false
  if (Math.abs(a.length - b.length) > maxDistance) return false
  if (a.length > 36 || b.length > 36) return false

  let previous = Array.from({ length: b.length + 1 }, (_, index) => index)
  for (let i = 1; i <= a.length; i += 1) {
    const current = [i]
    let rowMin = current[0]

    for (let j = 1; j <= b.length; j += 1) {
      const cost = a[i - 1] === b[j - 1] ? 0 : 1
      const value = Math.min(previous[j] + 1, current[j - 1] + 1, previous[j - 1] + cost)
      current[j] = value
      rowMin = Math.min(rowMin, value)
    }

    if (rowMin > maxDistance) return false
    previous = current
  }

  return previous[b.length] <= maxDistance
}

function hasSimpleTransposition(a, b) {
  if (a.length !== b.length || a.length < 4 || a === b) return false

  const diffs = []
  for (let index = 0; index < a.length; index += 1) {
    if (a[index] !== b[index]) diffs.push(index)
    if (diffs.length > 2) return false
  }

  return (
    diffs.length === 2 &&
    a[diffs[0]] === b[diffs[1]] &&
    a[diffs[1]] === b[diffs[0]]
  )
}

function searchScoreForWord(word, token) {
  if (!word || !token) return 0
  if (word === token) return 48
  if (word.startsWith(token)) return 38
  if (word.includes(token)) return 30
  if (token.length >= 4 && token.includes(word)) return 20
  if (hasSimpleTransposition(word, token)) return 24

  const maxDistance = maxDistanceForToken(token)
  if (!maxDistance) return 0

  if (editDistanceWithin(token, word, maxDistance)) return 18

  const prefix = word.slice(0, Math.min(word.length, token.length + maxDistance))
  if (prefix.length >= token.length - maxDistance && editDistanceWithin(token, prefix, maxDistance)) {
    return 16
  }

  return 0
}

export function searchScoreForInventoryRecord(record, search) {
  if (!search.query) return 1
  if (record.text.includes(search.query)) return 1000 + search.query.length
  if (search.compact.length >= 3 && record.compact.includes(search.compact)) {
    return 900 + search.compact.length
  }

  let score = 0

  for (const token of search.tokens) {
    if (!token) continue
    if (token.length === 1) {
      const exactShortToken = record.words.some((word) => word === token)
      if (!exactShortToken) return 0
      score += 8
      continue
    }

    if (record.text.includes(token)) {
      score += 72
      continue
    }

    if (token.length >= 3 && record.compact.includes(token)) {
      score += 56
      continue
    }

    let bestWordScore = 0
    for (const word of record.words) {
      bestWordScore = Math.max(bestWordScore, searchScoreForWord(word, token))
      if (bestWordScore >= 48) break
    }

    if (!bestWordScore) return 0
    score += bestWordScore
  }

  return score
}

export function searchInventoryItems(items = [], query, options = {}) {
  const search = createInventorySearchDescriptor(query)
  if (!search.query) return []

  const categoryLabels = options.categoryLabels || {}
  const limit = Number.isFinite(options.limit) && options.limit > 0 ? options.limit : Number.POSITIVE_INFINITY
  const matches = []

  for (const item of Array.isArray(items) ? items : []) {
    const record = buildInventorySearchRecord(item, categoryLabels)
    const searchScore = searchScoreForInventoryRecord(record, search)
    if (searchScore <= 0) continue
    matches.push({ item, searchScore, hasStock: record.hasStock })
  }

  matches.sort((a, b) => {
    if (b.searchScore !== a.searchScore) return b.searchScore - a.searchScore
    if (a.hasStock !== b.hasStock) return a.hasStock ? -1 : 1
    const aName = String(a.item?.nomItem ?? a.item?.nom_item ?? '')
    const bName = String(b.item?.nomItem ?? b.item?.nom_item ?? '')
    return aName.localeCompare(bName, 'fr', { sensitivity: 'base', numeric: true })
  })

  return matches.slice(0, limit).map((entry) => entry.item)
}
