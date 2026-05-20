import {
  DEFAULT_ITEM_TYPE_LABELS,
  DEFAULT_ITEM_TYPES,
  ITEM_TYPE_KEYS,
} from './CategorieItem'

const STORAGE_PREFIX = 'snk_item_categories_v1'
const MAX_CATEGORY_LABEL_LENGTH = 40

export { DEFAULT_ITEM_TYPES, ITEM_TYPE_KEYS }

export function normalizeItemType(type) {
  const value = String(type || '').trim()
  return ITEM_TYPE_KEYS.includes(value) ? value : 'OTHER'
}

export function normalizeCategoryLabel(value) {
  return String(value ?? '')
    .replace(/\s+/g, ' ')
    .trim()
    .slice(0, MAX_CATEGORY_LABEL_LENGTH)
}

export function sanitizeItemCategoryLabels(value, fallback = DEFAULT_ITEM_TYPE_LABELS) {
  const input = value && typeof value === 'object' && !Array.isArray(value) ? value : {}
  const out = {}
  for (const type of ITEM_TYPE_KEYS) {
    const label = normalizeCategoryLabel(input[type] ?? fallback[type])
    out[type] = label || DEFAULT_ITEM_TYPE_LABELS[type]
  }
  return out
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
  const itemType = normalizeItemType(type)
  return sanitizeItemCategoryLabels({
    ...labels,
    [itemType]: normalizeCategoryLabel(label),
  })
}

export function resetItemCategory(labels, type) {
  const itemType = normalizeItemType(type)
  return sanitizeItemCategoryLabels({
    ...labels,
    [itemType]: DEFAULT_ITEM_TYPE_LABELS[itemType],
  })
}

export function resolveItemTypeOptions(labels = DEFAULT_ITEM_TYPE_LABELS) {
  const sanitized = sanitizeItemCategoryLabels(labels)
  return DEFAULT_ITEM_TYPES.map((item) => ({
    ...item,
    label: sanitized[item.value] || item.label,
    defaultLabel: item.label,
  }))
}

export function itemTypeLabel(type, labels = DEFAULT_ITEM_TYPE_LABELS) {
  const itemType = normalizeItemType(type)
  const sanitized = sanitizeItemCategoryLabels(labels)
  return sanitized[itemType] || DEFAULT_ITEM_TYPE_LABELS[itemType] || 'Autre'
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
  Object.values(sanitizeItemCategoryLabels(labels)).forEach(add)
  ;[
    'sneaker',
    'sneakers',
    'pokemon',
    'pokémon',
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
