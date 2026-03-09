// src/store/authStore.js
import { ref } from 'vue'
import { useBillingStore } from './billingStore'

const user = ref(null)
const token = ref('')

function safeGet(key) {
  try {
    return localStorage.getItem(key)
  } catch {
    return sessionStorage.getItem(key)
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
    sessionStorage.removeItem(key)
  }
}

function loadFromStorage() {
  // user
  try {
    user.value = JSON.parse(safeGet('snk_user') || 'null')
  } catch {
    user.value = null
  }
  // token (toujours string)
  token.value = safeGet('snk_token') || ''
}
loadFromStorage()

/**
 * payload attendu:
 * { user?: object|null, token?: string|null }
 */
function setAuth(payload) {
  const previousToken = token.value
  user.value = payload?.user ?? null

  // IMPORTANT: token = string, jamais null
  token.value = payload?.token ? String(payload.token) : ''

  if (user.value) safeSet('snk_user', JSON.stringify(user.value))
  else safeRemove('snk_user')

  if (token.value) safeSet('snk_token', token.value)
  else safeRemove('snk_token')

  // Reset billing cache when token changes (login/logout/switch account)
  if (token.value !== previousToken) {
    try {
      useBillingStore().reset()
    } catch (e) {
      console.warn('billingStore reset failed', e)
    }
  }
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

export function useAuthStore() {
  return { user, token, setAuth, setToken, setUser, logout }
}
