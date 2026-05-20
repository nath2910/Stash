import { ITEM_TYPE_KEYS, isItemCategoryAlias } from './itemCategoryStore'

const STORAGE_PREFIX = 'snk_item_subcategories_v1'
const MAX_SUBCATEGORY_LENGTH = 60

export const DEFAULT_SUBCATEGORIES = {
  SNEAKER: ['Jordan', 'Dunk', 'Yeezy', 'Running', 'New Balance', 'Asics'],
  POKEMON_CARD: ['Booster', 'Display', 'Carte gradee', 'Coffret', 'ETB'],
  TICKET: ['Concert', 'Evenement sportif', 'Festival', 'Theatre', 'Spectacle'],
  OTHER: ['Collection', 'Accessoire', 'Mode', 'Electronique'],
}

export function normalizeItemType(type) {
  const value = String(type || '').trim()
  return ITEM_TYPE_KEYS.includes(value) ? value : 'OTHER'
}

export function normalizeSubcategoryName(value) {
  return String(value ?? '')
    .replace(/\s+/g, ' ')
    .trim()
    .slice(0, MAX_SUBCATEGORY_LENGTH)
}

export function dedupeSubcategories(values = []) {
  const map = new Map()
  for (const value of values) {
    const cleaned = normalizeSubcategoryName(value)
    if (!cleaned) continue
    const key = cleaned.toLocaleLowerCase('fr')
    if (!map.has(key)) map.set(key, cleaned)
  }
  return Array.from(map.values()).sort((a, b) =>
    a.localeCompare(b, 'fr', { sensitivity: 'base', numeric: true }),
  )
}

function removeMainCategoryAliases(values = [], aliases) {
  return values.filter((value) => {
    if (!value) return false
    if (aliases && aliases.has(normalizeSubcategoryName(value).toLocaleLowerCase('fr'))) {
      return false
    }
    return !isItemCategoryAlias(value)
  })
}

export function sanitizeSubcategoryMap(value, fallback = DEFAULT_SUBCATEGORIES) {
  const input = value && typeof value === 'object' && !Array.isArray(value) ? value : {}
  const out = {}
  for (const type of ITEM_TYPE_KEYS) {
    const raw = Array.isArray(input[type]) ? input[type] : fallback[type]
    out[type] = dedupeSubcategories(raw || [])
  }
  return out
}

export function subcategoryStorageKey(userId) {
  const suffix = String(userId || 'guest').trim() || 'guest'
  return `${STORAGE_PREFIX}_${suffix}`
}

function getStorage(storage) {
  if (storage) return storage
  if (typeof window === 'undefined') return null
  return window.localStorage ?? null
}

export function readStoredSubcategories(userId, storage) {
  const target = getStorage(storage)
  if (!target) return sanitizeSubcategoryMap(DEFAULT_SUBCATEGORIES)

  try {
    const raw = target.getItem(subcategoryStorageKey(userId))
    if (!raw) {
      const defaults = sanitizeSubcategoryMap(DEFAULT_SUBCATEGORIES)
      target.setItem(subcategoryStorageKey(userId), JSON.stringify(defaults))
      return defaults
    }
    return sanitizeSubcategoryMap(JSON.parse(raw))
  } catch {
    return sanitizeSubcategoryMap(DEFAULT_SUBCATEGORIES)
  }
}

export function writeStoredSubcategories(userId, value, storage) {
  const target = getStorage(storage)
  const sanitized = sanitizeSubcategoryMap(value)
  if (!target) return sanitized
  target.setItem(subcategoryStorageKey(userId), JSON.stringify(sanitized))
  return sanitized
}

export function extractSubcategoriesByType(items = []) {
  const out = Object.fromEntries(ITEM_TYPE_KEYS.map((type) => [type, []]))
  for (const item of Array.isArray(items) ? items : []) {
    const type = normalizeItemType(item?.type)
    const category = normalizeSubcategoryName(item?.categorie)
    if (category) out[type].push(category)
  }
  return sanitizeSubcategoryMap(out, {})
}

export function resolveSubcategoryOptions(type, options = {}) {
  const itemType = normalizeItemType(type)
  const stored = sanitizeSubcategoryMap(options.stored)
  const discovered = sanitizeSubcategoryMap(options.discovered, {})
  const current = normalizeSubcategoryName(options.currentValue)
  const aliases = options.mainCategoryAliases instanceof Set ? options.mainCategoryAliases : null
  return removeMainCategoryAliases(
    dedupeSubcategories([...(stored[itemType] || []), ...(discovered[itemType] || []), current]),
    aliases,
  )
}

export function addSubcategory(map, type, name) {
  const itemType = normalizeItemType(type)
  const cleaned = normalizeSubcategoryName(name)
  const next = sanitizeSubcategoryMap(map)
  if (!cleaned) return next
  next[itemType] = dedupeSubcategories([...(next[itemType] || []), cleaned])
  return next
}

export function removeSubcategory(map, type, name) {
  const itemType = normalizeItemType(type)
  const cleanedKey = normalizeSubcategoryName(name).toLocaleLowerCase('fr')
  const next = sanitizeSubcategoryMap(map)
  next[itemType] = (next[itemType] || []).filter(
    (item) => item.toLocaleLowerCase('fr') !== cleanedKey,
  )
  return next
}

export function renameSubcategory(map, type, from, to) {
  const itemType = normalizeItemType(type)
  const fromKey = normalizeSubcategoryName(from).toLocaleLowerCase('fr')
  const cleanedTo = normalizeSubcategoryName(to)
  const next = sanitizeSubcategoryMap(map)
  if (!fromKey || !cleanedTo) return next
  next[itemType] = dedupeSubcategories(
    (next[itemType] || []).map((item) =>
      item.toLocaleLowerCase('fr') === fromKey ? cleanedTo : item,
    ),
  )
  return next
}
