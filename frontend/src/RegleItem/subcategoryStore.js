import {
  ITEM_TYPE_KEYS,
  isItemCategoryAlias,
  normalizeItemType as normalizeCategoryItemType,
  resolveItemTypeOptions,
} from './itemCategoryStore'

const STORAGE_PREFIX = 'snk_item_subcategories_v1'
const MAX_SUBCATEGORY_LENGTH = 60

export const DEFAULT_SUBCATEGORIES = {
  SNEAKER: ['Jordan', 'Dunk', 'Yeezy', 'Running', 'New Balance', 'Asics'],
  CLOTHING: ['Hoodie', 'T-shirt', 'Veste', 'Pantalon', 'Casquette'],
  ACCESSORY: ['Sac', 'Bijou', 'Lunettes', 'Ceinture'],
  WATCH: ['Automatique', 'Quartz', 'Connectee', 'Vintage'],
  ELECTRONICS: ['Console', 'Smartphone', 'Audio', 'Photo', 'Ordinateur'],
  COLLECTIBLE: ['Figurine', 'Carte', 'Vinyle', 'Art toy', 'Edition limitee'],
  HOME: ['Mobilier', 'Decoration', 'Luminaire', 'Cuisine'],
  POKEMON_CARD: ['Booster', 'Display', 'Carte gradee', 'Coffret', 'ETB'],
  TICKET: ['Concert', 'Evenement sportif', 'Festival', 'Theatre', 'Spectacle'],
  OTHER: ['Collection', 'Accessoire', 'Mode', 'Electronique'],
}

export function normalizeItemType(type) {
  return normalizeCategoryItemType(type)
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

function isPlainObject(value) {
  return value && typeof value === 'object' && !Array.isArray(value)
}

function removeMainCategoryAliases(values = [], aliases) {
  return values.filter((value) => {
    if (!value) return false
    const cleaned = normalizeSubcategoryName(value).toLocaleLowerCase('fr')
    if (aliases && aliases.has(cleaned)) return false
    return !isItemCategoryAlias(value)
  })
}

function collectTypes(input, fallback, categoryLabels) {
  const types = new Set(ITEM_TYPE_KEYS)
  if (isPlainObject(fallback)) {
    Object.keys(fallback).forEach((type) => types.add(normalizeItemType(type)))
  }
  if (isPlainObject(input)) {
    Object.keys(input).forEach((type) => types.add(normalizeItemType(type)))
  }
  resolveItemTypeOptions(categoryLabels || {}).forEach((option) => types.add(normalizeItemType(option.value)))
  return Array.from(types).filter(Boolean)
}

export function sanitizeSubcategoryMap(value, fallback = DEFAULT_SUBCATEGORIES, categoryLabels) {
  const input = isPlainObject(value) ? value : {}
  const fallbackInput = isPlainObject(fallback) ? fallback : {}
  const out = {}
  for (const type of collectTypes(input, fallbackInput, categoryLabels)) {
    const raw = Array.isArray(input[type]) ? input[type] : fallbackInput[type]
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

export function readStoredSubcategories(userId, storage, categoryLabels) {
  const target = getStorage(storage)
  if (!target) return sanitizeSubcategoryMap(DEFAULT_SUBCATEGORIES, DEFAULT_SUBCATEGORIES, categoryLabels)

  try {
    const raw = target.getItem(subcategoryStorageKey(userId))
    if (!raw) {
      const defaults = sanitizeSubcategoryMap(DEFAULT_SUBCATEGORIES, DEFAULT_SUBCATEGORIES, categoryLabels)
      target.setItem(subcategoryStorageKey(userId), JSON.stringify(defaults))
      return defaults
    }
    return sanitizeSubcategoryMap(JSON.parse(raw), DEFAULT_SUBCATEGORIES, categoryLabels)
  } catch {
    return sanitizeSubcategoryMap(DEFAULT_SUBCATEGORIES, DEFAULT_SUBCATEGORIES, categoryLabels)
  }
}

export function writeStoredSubcategories(userId, value, storage, categoryLabels) {
  const target = getStorage(storage)
  const sanitized = sanitizeSubcategoryMap(value, DEFAULT_SUBCATEGORIES, categoryLabels)
  if (!target) return sanitized
  target.setItem(subcategoryStorageKey(userId), JSON.stringify(sanitized))
  if (typeof window !== 'undefined' && !storage) {
    window.dispatchEvent(
      new CustomEvent('snk:item-subcategories-change', {
        detail: { userId: String(userId || 'guest'), subcategories: sanitized },
      }),
    )
  }
  return sanitized
}

export function extractSubcategoriesByType(items = [], categoryLabels) {
  const out = {}
  for (const item of Array.isArray(items) ? items : []) {
    const type = normalizeItemType(item?.type)
    const category = normalizeSubcategoryName(item?.categorie)
    if (!category) continue
    if (!out[type]) out[type] = []
    out[type].push(category)
  }
  return sanitizeSubcategoryMap(out, {}, categoryLabels)
}

export function resolveSubcategoryOptions(type, options = {}) {
  const itemType = normalizeItemType(type)
  const stored = sanitizeSubcategoryMap(options.stored, DEFAULT_SUBCATEGORIES, options.categoryLabels)
  const discovered = sanitizeSubcategoryMap(options.discovered, {}, options.categoryLabels)
  const current = normalizeSubcategoryName(options.currentValue)
  const aliases = options.mainCategoryAliases instanceof Set ? options.mainCategoryAliases : null
  return removeMainCategoryAliases(
    dedupeSubcategories([...(stored[itemType] || []), ...(discovered[itemType] || []), current]),
    aliases,
  )
}

export function addSubcategory(map, type, name, categoryLabels) {
  const itemType = normalizeItemType(type)
  const cleaned = normalizeSubcategoryName(name)
  const next = sanitizeSubcategoryMap(map, DEFAULT_SUBCATEGORIES, categoryLabels)
  if (!cleaned) return next
  next[itemType] = dedupeSubcategories([...(next[itemType] || []), cleaned])
  return next
}

export function removeSubcategory(map, type, name, categoryLabels) {
  const itemType = normalizeItemType(type)
  const cleanedKey = normalizeSubcategoryName(name).toLocaleLowerCase('fr')
  const next = sanitizeSubcategoryMap(map, DEFAULT_SUBCATEGORIES, categoryLabels)
  next[itemType] = (next[itemType] || []).filter(
    (item) => item.toLocaleLowerCase('fr') !== cleanedKey,
  )
  return next
}

export function renameSubcategory(map, type, from, to, categoryLabels) {
  const itemType = normalizeItemType(type)
  const fromKey = normalizeSubcategoryName(from).toLocaleLowerCase('fr')
  const cleanedTo = normalizeSubcategoryName(to)
  const next = sanitizeSubcategoryMap(map, DEFAULT_SUBCATEGORIES, categoryLabels)
  if (!fromKey || !cleanedTo) return next
  next[itemType] = dedupeSubcategories(
    (next[itemType] || []).map((item) =>
      item.toLocaleLowerCase('fr') === fromKey ? cleanedTo : item,
    ),
  )
  return next
}
