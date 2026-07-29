// src/store/authStore.js
import { ref } from 'vue'
import { useBillingStore } from './billingStore'
import {
  AUTH_STORAGE_KEYS,
  AUTH_SYNC_EVENT,
  readAuthToken,
  readStoredUser,
  safeStorageRemove,
  writeAuthState,
} from '@/utils/authStorage'

const user = ref(null)
const token = ref('')

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
  const parsedUser = readStoredUser()
  token.value = readAuthToken()
  user.value = token.value ? parsedUser : null
  if (!token.value && parsedUser) {
    safeStorageRemove('snk_user')
  }
  syncBillingFromAuth(user.value, token.value, previousToken)
}
loadFromStorage()

/**
 * payload attendu:
 * { user?: object|null, token?: string|null }
 */
function setAuth(payload) {
  const previousToken = token.value
  token.value = payload?.token ? String(payload.token) : ''
  user.value = token.value ? (payload?.user ?? null) : null

  writeAuthState({ token: token.value, user: user.value })
  syncBillingFromAuth(user.value, token.value, previousToken)
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
