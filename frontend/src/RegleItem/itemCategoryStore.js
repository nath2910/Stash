import {
  DEFAULT_ITEM_TYPE_LABELS,
  DEFAULT_ITEM_TYPES,
  ITEM_TYPE_KEYS,
  formatItemTypeLabel,
  normalizeItemTypeValue,
} from './CategorieItem'

const STORAGE_PREFIX = 'snk_item_categories_v1'
const MAX_CATEGORY_LABEL_LENGTH = 40
const REMOVED_ITEM_TYPES_KEY = '__removedItemTypes'

export { DEFAULT_ITEM_TYPES, ITEM_TYPE_KEYS }

export function normalizeItemType(type) {
  return normalizeItemTypeValue(type, 'OTHER')
}

export function normalizeCategoryLabel(value) {
  return String(value ?? '')
    .replace(/\s+/g, ' ')
    .trim()
    .slice(0, MAX_CATEGORY_LABEL_LENGTH)
}

function isPlainObject(value) {
  return value && typeof value === 'object' && !Array.isArray(value)
}

function hasOwn(value, key) {
  return Object.prototype.hasOwnProperty.call(value, key)
}

function isInternalCategoryKey(key) {
  return key === REMOVED_ITEM_TYPES_KEY
}

function normalizeRemovedItemTypes(value) {
  const out = new Set()
  if (!Array.isArray(value)) return out
  for (const type of value) {
    const itemType = normalizeItemTypeValue(type, '')
    if (itemType) out.add(itemType)
  }
  return out
}

function withRemovedItemTypes(labels, removedTypes) {
  const removed = Array.from(removedTypes || [])
    .map((type) => normalizeItemTypeValue(type, ''))
    .filter(Boolean)
    .sort((a, b) => a.localeCompare(b, 'fr', { sensitivity: 'base' }))
  if (removed.length) labels[REMOVED_ITEM_TYPES_KEY] = removed
  return labels
}

function applyLabel(out, type, label, fallbackLabel) {
  const itemType = normalizeItemTypeValue(type, '')
  if (!itemType) return
  const cleaned = normalizeCategoryLabel(label ?? fallbackLabel)
  out[itemType] = cleaned || DEFAULT_ITEM_TYPE_LABELS[itemType] || formatItemTypeLabel(itemType)
}

export function sanitizeItemCategoryLabels(value, fallback = DEFAULT_ITEM_TYPE_LABELS) {
  const input = isPlainObject(value) ? value : {}
  const fallbackInput = isPlainObject(fallback) ? fallback : DEFAULT_ITEM_TYPE_LABELS
  const removedTypes = normalizeRemovedItemTypes(input[REMOVED_ITEM_TYPES_KEY])
  const out = {}

  for (const item of DEFAULT_ITEM_TYPES) {
    const hasInputLabel = hasOwn(input, item.value) && normalizeCategoryLabel(input[item.value])
    if (removedTypes.has(item.value) && !hasInputLabel) continue
    applyLabel(
      out,
      item.value,
      hasInputLabel ? input[item.value] : fallbackInput[item.value],
      item.label,
    )
    removedTypes.delete(item.value)
  }

  for (const [type, label] of Object.entries(fallbackInput)) {
    if (isInternalCategoryKey(type)) continue
    if (ITEM_TYPE_KEYS.includes(normalizeItemTypeValue(type, ''))) continue
    applyLabel(out, type, input[type] ?? label, label)
  }

  for (const [type, label] of Object.entries(input)) {
    if (isInternalCategoryKey(type)) continue
    const itemType = normalizeItemTypeValue(type, '')
    if (!itemType || ITEM_TYPE_KEYS.includes(itemType)) continue
    applyLabel(out, itemType, label, formatItemTypeLabel(itemType))
    removedTypes.delete(itemType)
  }

  return withRemovedItemTypes(out, removedTypes)
}

export function itemCategoryStorageKey(userId) {
  const suffix = String(userId || 'guest').trim() || 'guest'
  return `${STORAGE_PREFIX}_${suffix}`
}

function getStorage(storage) {
  if (storage) return storage
  if (typeof window === 'undefined') return null
  return window.localStorage ?? null
}

export function readStoredItemCategories(userId, storage) {
  const target = getStorage(storage)
  if (!target) return sanitizeItemCategoryLabels(DEFAULT_ITEM_TYPE_LABELS)

  try {
    const raw = target.getItem(itemCategoryStorageKey(userId))
    if (!raw) {
      const defaults = sanitizeItemCategoryLabels(DEFAULT_ITEM_TYPE_LABELS)
      target.setItem(itemCategoryStorageKey(userId), JSON.stringify(defaults))
      return defaults
    }
    return sanitizeItemCategoryLabels(JSON.parse(raw))
  } catch {
    return sanitizeItemCategoryLabels(DEFAULT_ITEM_TYPE_LABELS)
  }
}

export function writeStoredItemCategories(userId, value, storage) {
  const target = getStorage(storage)
  const sanitized = sanitizeItemCategoryLabels(value)
  if (!target) return sanitized
  target.setItem(itemCategoryStorageKey(userId), JSON.stringify(sanitized))
  if (typeof window !== 'undefined' && !storage) {
    window.dispatchEvent(
      new CustomEvent('snk:item-categories-change', {
        detail: { userId: String(userId || 'guest'), labels: sanitized },
      }),
    )
  }
  return sanitized
}

export function renameItemCategory(labels, type, label) {
  const itemType = normalizeItemTypeValue(type, '')
  if (!itemType) return sanitizeItemCategoryLabels(labels)
  return sanitizeItemCategoryLabels({
    ...labels,
    [itemType]: normalizeCategoryLabel(label),
  })
}

export function resetItemCategory(labels, type) {
  const itemType = normalizeItemTypeValue(type, '')
  if (!itemType) return sanitizeItemCategoryLabels(labels)
  return sanitizeItemCategoryLabels({
    ...labels,
    [itemType]: DEFAULT_ITEM_TYPE_LABELS[itemType] || formatItemTypeLabel(itemType),
  })
}

export function addItemCategory(labels, label) {
  const cleaned = normalizeCategoryLabel(label)
  const sanitized = sanitizeItemCategoryLabels(labels)
  if (!cleaned) return { labels: sanitized, type: '' }

  const existing = new Set(Object.keys(sanitized).filter((key) => !isInternalCategoryKey(key)))
  const base = normalizeItemTypeValue(cleaned, 'CUSTOM').slice(0, 72).replace(/_+$/g, '') || 'CUSTOM'
  let type = base
  let suffix = 2
  while (existing.has(type)) {
    type = `${base}_${suffix}`.slice(0, 80).replace(/_+$/g, '')
    suffix += 1
  }

  return {
    labels: sanitizeItemCategoryLabels({
      ...sanitized,
      [type]: cleaned,
    }),
    type,
  }
}

export function canRemoveItemCategory(type) {
  const itemType = normalizeItemTypeValue(type, '')
  return Boolean(itemType)
}

export function removeItemCategory(labels, type) {
  const itemType = normalizeItemTypeValue(type, '')
  const sanitized = sanitizeItemCategoryLabels(labels)
  if (!itemType || !canRemoveItemCategory(itemType)) return sanitized
  const next = { ...sanitized }
  const removedTypes = normalizeRemovedItemTypes(sanitized[REMOVED_ITEM_TYPES_KEY])
  delete next[itemType]
  delete next[REMOVED_ITEM_TYPES_KEY]
  if (ITEM_TYPE_KEYS.includes(itemType)) removedTypes.add(itemType)
  else removedTypes.delete(itemType)
  withRemovedItemTypes(next, removedTypes)
  return sanitizeItemCategoryLabels(next)
}

export function resolveItemTypeOptions(labels = DEFAULT_ITEM_TYPE_LABELS) {
  const sanitized = sanitizeItemCategoryLabels(labels)
  const options = DEFAULT_ITEM_TYPES
    .filter((item) => sanitized[item.value])
    .map((item) => ({
      ...item,
      label: sanitized[item.value] || item.label,
      defaultLabel: item.label,
      custom: false,
    }))

  Object.keys(sanitized)
    .filter((type) => !isInternalCategoryKey(type) && !ITEM_TYPE_KEYS.includes(type))
    .forEach((type) => {
      options.push({
        value: type,
        label: sanitized[type] || formatItemTypeLabel(type),
        defaultLabel: formatItemTypeLabel(type),
        custom: true,
      })
    })

  return options
}

export function itemTypeLabel(type, labels = DEFAULT_ITEM_TYPE_LABELS) {
  const itemType = normalizeItemTypeValue(type, '')
  if (!itemType) return DEFAULT_ITEM_TYPE_LABELS.OTHER
  const sanitized = sanitizeItemCategoryLabels(labels)
  return sanitized[itemType] || DEFAULT_ITEM_TYPE_LABELS[itemType] || formatItemTypeLabel(itemType)
}

export function buildItemCategoryAliases(labels = DEFAULT_ITEM_TYPE_LABELS) {
  const aliases = new Set()
  const add = (value) => {
    const cleaned = normalizeCategoryLabel(value).toLocaleLowerCase('fr')
    if (cleaned) aliases.add(cleaned)
  }

  DEFAULT_ITEM_TYPES.forEach((item) => {
    add(item.value)
    add(item.label)
  })
  Object.entries(sanitizeItemCategoryLabels(labels)).forEach(([type, label]) => {
    if (isInternalCategoryKey(type)) return
    add(type)
    add(label)
  })
  ;[
    'sneaker',
    'sneakers',
    'pokemon',
    'ticket',
    'tickets',
    'other',
    'autre',
    'autres',
  ].forEach(add)

  return aliases
}

export function isItemCategoryAlias(value, labels = DEFAULT_ITEM_TYPE_LABELS) {
  const cleaned = normalizeCategoryLabel(value).toLocaleLowerCase('fr')
  return Boolean(cleaned && buildItemCategoryAliases(labels).has(cleaned))
}
