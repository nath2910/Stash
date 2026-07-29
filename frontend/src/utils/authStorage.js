export const AUTH_STORAGE_KEYS = ['snk_token', 'snk_user']
export const AUTH_SYNC_EVENT = 'snk:auth-storage-sync'

function safeLocalGet(key) {
  try {
    return localStorage.getItem(key)
  } catch {
    return null
  }
}

function safeSessionGet(key) {
  try {
    return sessionStorage.getItem(key)
  } catch {
    return null
  }
}

export function safeStorageGet(key) {
  const localValue = safeLocalGet(key)
  if (localValue != null) return localValue
  return safeSessionGet(key)
}

export function safeStorageSet(key, value) {
  try {
    localStorage.setItem(key, value)
    return
  } catch {
    // Ignore and fallback to session storage.
  }

  try {
    sessionStorage.setItem(key, value)
  } catch {
    // ignore
  }
}

export function safeStorageRemove(key) {
  try {
    localStorage.removeItem(key)
  } catch {
    // ignore
  }

  try {
    sessionStorage.removeItem(key)
  } catch {
    // ignore
  }
}

export function notifyAuthStorageSync() {
  if (typeof window === 'undefined') return
  window.dispatchEvent(new CustomEvent(AUTH_SYNC_EVENT))
}

export function readAuthToken() {
  return safeStorageGet('snk_token') || ''
}

export function readStoredUser() {
  try {
    return JSON.parse(safeStorageGet('snk_user') || 'null')
  } catch {
    return null
  }
}

export function writeAuthState({ token, user } = {}) {
  if (token) safeStorageSet('snk_token', String(token))
  else safeStorageRemove('snk_token')

  if (user) safeStorageSet('snk_user', JSON.stringify(user))
  else safeStorageRemove('snk_user')

  notifyAuthStorageSync()
}

export function clearAuthState() {
  safeStorageRemove('snk_token')
  safeStorageRemove('snk_user')
  notifyAuthStorageSync()
}
