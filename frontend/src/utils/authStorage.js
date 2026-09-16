export const AUTH_STORAGE_KEYS = ['snk_token', 'snk_user']
export const AUTH_SYNC_EVENT = 'snk:auth-storage-sync'

function removeLegacyLocalValue(key) {
  try {
    localStorage.removeItem(key)
  } catch {
    // ignore unavailable browser storage
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
  // Access tokens are intentionally scoped to one browser tab. Do not restore
  // legacy localStorage tokens: they survive browser restarts and remain
  // unnecessarily available to any script that later runs on this origin.
  removeLegacyLocalValue(key)
  return safeSessionGet(key)
}

export function safeStorageSet(key, value) {
  try {
    sessionStorage.setItem(key, value)
    return
  } catch {
    // Storage can be unavailable in private browsing; keep the current
    // in-memory auth state rather than persisting a credential elsewhere.
  }
}

export function safeStorageRemove(key) {
  removeLegacyLocalValue(key)

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
