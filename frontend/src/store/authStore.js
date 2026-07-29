// src/store/authStore.js
import { ref } from 'vue'
import { useBillingStore } from './billingStore'

const user = ref(null)
const token = ref('')
const AUTH_STORAGE_KEYS = ['snk_token', 'snk_user']
const AUTH_SYNC_EVENT = 'snk:auth-storage-sync'

function safeGet(key) {
  try {
    const localValue = localStorage.getItem(key)
    if (localValue != null) return localValue
  } catch {
    // Ignore and fallback to session storage below.
  }
  try {
    return sessionStorage.getItem(key)
  } catch {
    return null
  }
}
function safeSet(key, value) {
  try {
    localStorage.setItem(key, value)
  } catch {
    sessionStorage.setItem(key, value)
  }
}
function safeRemove(key) {
  try {
    localStorage.removeItem(key)
  } catch {
    // Ignore and continue with session storage cleanup below.
  }
  try {
    sessionStorage.removeItem(key)
  } catch {
    // ignore
  }
}

function syncBillingFromAuth(nextUser, nextToken, previousToken = token.value) {
  try {
    const billing = useBillingStore()
    if (!nextToken) {
      billing.reset()
      return
    }
    if (nextToken !== previousToken) {
      billing.reset()
    }
    if (nextUser?.subscriptionStatus) {
      billing.seedFromUser(nextUser)
    }
  } catch (e) {
    console.warn('billingStore sync failed', e)
  }
}

function loadFromStorage() {
  const previousToken = token.value
  let parsedUser = null
  try {
    parsedUser = JSON.parse(safeGet('snk_user') || 'null')
  } catch {
    parsedUser = null
  }
  token.value = safeGet('snk_token') || ''
  user.value = token.value ? parsedUser : null
  if (!token.value && parsedUser) {
    safeRemove('snk_user')
  }
  syncBillingFromAuth(user.value, token.value, previousToken)
}
loadFromStorage()

function notifyAuthStorageSync() {
  if (typeof window === 'undefined') return
  window.dispatchEvent(new CustomEvent(AUTH_SYNC_EVENT))
}

/**
 * payload attendu:
 * { user?: object|null, token?: string|null }
 */
function setAuth(payload) {
  const previousToken = token.value
  token.value = payload?.token ? String(payload.token) : ''
  user.value = token.value ? (payload?.user ?? null) : null

  if (user.value) safeSet('snk_user', JSON.stringify(user.value))
  else safeRemove('snk_user')

  if (token.value) safeSet('snk_token', token.value)
  else safeRemove('snk_token')

  syncBillingFromAuth(user.value, token.value, previousToken)
  notifyAuthStorageSync()
}

function setToken(newToken) {
  setAuth({ user: user.value, token: newToken })
}

function setUser(newUser) {
  setAuth({ user: newUser, token: token.value })
}

function logout() {
  setAuth(null)
}

function handleStorageChange(event) {
  if (event?.key && !AUTH_STORAGE_KEYS.includes(event.key)) return
  loadFromStorage()
}

if (typeof window !== 'undefined') {
  window.addEventListener('storage', handleStorageChange)
  window.addEventListener(AUTH_SYNC_EVENT, loadFromStorage)
}

export function useAuthStore() {
  return { user, token, setAuth, setToken, setUser, logout, loadFromStorage }
}
