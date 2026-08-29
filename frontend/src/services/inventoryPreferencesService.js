import AdminService from './AdminService.js'

export const INVENTORY_ITEM_CATEGORIES_SETTINGS_KEY = 'inventoryItemCategories'
export const INVENTORY_SUBCATEGORIES_SETTINGS_KEY = 'inventorySubcategories'

const settingsCache = new Map()
const settingsInflight = new Map()
const saveQueues = new Map()
const syncInflight = new Map()

function normalizeUserKey(userId) {
  const value = String(userId ?? '').trim()
  return value || 'guest'
}

function isPlainObject(value) {
  return value && typeof value === 'object' && !Array.isArray(value)
}

function cloneJsonValue(value) {
  if (Array.isArray(value)) return value.map((entry) => cloneJsonValue(entry))
  if (isPlainObject(value)) {
    return Object.fromEntries(
      Object.entries(value).map(([key, entry]) => [key, cloneJsonValue(entry)]),
    )
  }
  return value
}

function normalizeSettings(value) {
  return isPlainObject(value) ? cloneJsonValue(value) : {}
}

async function loadSettings(userId) {
  const userKey = normalizeUserKey(userId)
  if (userKey === 'guest') return null

  if (settingsCache.has(userKey)) return cloneJsonValue(settingsCache.get(userKey))
  if (settingsInflight.has(userKey)) return settingsInflight.get(userKey)

  const request = AdminService.settings()
    .then((payload) => {
      const settings = normalizeSettings(payload)
      settingsCache.set(userKey, settings)
      return cloneJsonValue(settings)
    })
    .catch(() => null)
    .finally(() => {
      settingsInflight.delete(userKey)
    })

  settingsInflight.set(userKey, request)
  return request
}

export function syncInventoryPreference(userId, key, localValue, options = {}) {
  const userKey = normalizeUserKey(userId)
  if (userKey === 'guest') return Promise.resolve(null)

  const syncKey = `${userKey}:${key}`
  if (syncInflight.has(syncKey)) return syncInflight.get(syncKey)

  const request = loadSettings(userId)
    .then((settings) => {
      if (!settings) return null

      if (Object.prototype.hasOwnProperty.call(settings, key)) {
        const remoteValue = cloneJsonValue(settings[key])
        options.applyRemote?.(remoteValue)
        return remoteValue
      }

      if (options.shouldSeed?.(localValue)) {
        return saveInventoryPreference(userId, key, localValue)
      }

      return null
    })
    .finally(() => {
      syncInflight.delete(syncKey)
    })

  syncInflight.set(syncKey, request)
  return request
}

export function saveInventoryPreference(userId, key, value) {
  const userKey = normalizeUserKey(userId)
  if (userKey === 'guest') return Promise.resolve(null)

  const previous = saveQueues.get(userKey) || Promise.resolve()
  const request = previous
    .catch(() => null)
    .then(async () => {
      const settings = (await loadSettings(userId)) || {}
      const nextSettings = {
        ...settings,
        [key]: cloneJsonValue(value),
      }
      const saved = normalizeSettings(await AdminService.saveSettings(nextSettings))
      settingsCache.set(userKey, saved)
      return Object.prototype.hasOwnProperty.call(saved, key) ? cloneJsonValue(saved[key]) : null
    })
    .catch(() => null)

  saveQueues.set(
    userKey,
    request.then(
      () => null,
      () => null,
    ),
  )
  return request
}

export function resetInventoryPreferencesServiceCache() {
  settingsCache.clear()
  settingsInflight.clear()
  saveQueues.clear()
  syncInflight.clear()
}
