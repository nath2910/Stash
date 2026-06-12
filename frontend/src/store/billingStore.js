import { ref } from 'vue'
import BillingService from '@/services/BillingService'

const status = ref('unknown') // unknown | active | past_due | canceled | inactive
const portalUrl = ref('')
const lastFetchedAt = ref(0)
let inflightBasic = null
let inflightWithPortal = null

const CACHE_KEY = 'snk_billing_status_cache'
const STATUS_CACHE_TTL_MS = 2 * 60 * 1000
const ACTIVE_STALE_REFRESH_MS = 5 * 60 * 1000

function safeGet(key) {
  try {
    return localStorage.getItem(key)
  } catch {
    try {
      return sessionStorage.getItem(key)
    } catch {
      return null
    }
  }
}

function safeSet(key, value) {
  try {
    localStorage.setItem(key, value)
  } catch {
    try {
      sessionStorage.setItem(key, value)
    } catch {
      // Storage can be unavailable in private contexts.
    }
  }
}

function safeRemove(key) {
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

function normalizeStatus(value) {
  const next = String(value || '').trim().toLowerCase()
  return next || 'inactive'
}

function persistStatus() {
  if (status.value === 'unknown') {
    safeRemove(CACHE_KEY)
    return
  }
  safeSet(
    CACHE_KEY,
    JSON.stringify({
      status: status.value,
      fetchedAt: lastFetchedAt.value || Date.now(),
    }),
  )
}

function loadFromStorage() {
  try {
    const cached = JSON.parse(safeGet(CACHE_KEY) || 'null')
    if (!cached?.status || !cached?.fetchedAt) return
    status.value = normalizeStatus(cached.status)
    lastFetchedAt.value = Number(cached.fetchedAt) || 0
  } catch {
    safeRemove(CACHE_KEY)
  }
}

function isFresh(ttlMs = STATUS_CACHE_TTL_MS) {
  return status.value !== 'unknown' && Date.now() - lastFetchedAt.value < ttlMs
}

function applyStatus(nextStatus, nextPortalUrl = '') {
  status.value = normalizeStatus(nextStatus)
  lastFetchedAt.value = Date.now()
  if (nextPortalUrl) portalUrl.value = nextPortalUrl
  persistStatus()
  return status.value
}

function refreshInBackground(includePortal = false) {
  fetchStatus(true, includePortal).catch(() => {})
}

async function fetchStatus(force = false, includePortal = false) {
  if (!force && !includePortal && status.value === 'active') {
    if (!isFresh(ACTIVE_STALE_REFRESH_MS)) refreshInBackground(false)
    return status.value
  }

  if (!force && !includePortal && isFresh()) return status.value

  if (!force && includePortal && portalUrl.value && isFresh()) return status.value

  const currentInflight = includePortal ? inflightWithPortal : inflightBasic
  if (!force && currentInflight) return currentInflight

  const previousStatus = status.value
  const request = BillingService.status(includePortal, force)
    .then((res) => {
      applyStatus(res?.data?.status || 'inactive', res?.data?.portalUrl || '')
      if (includePortal) {
        portalUrl.value = res?.data?.portalUrl || ''
      }
      return status.value
    })
    .catch(() => {
      if (previousStatus !== 'unknown') return previousStatus
      applyStatus('inactive')
      if (includePortal) {
        portalUrl.value = ''
      }
      return status.value
    })
    .finally(() => {
      if (includePortal) inflightWithPortal = null
      else inflightBasic = null
    })

  if (includePortal) inflightWithPortal = request
  else inflightBasic = request

  return request
}

function seedStatus(nextStatus) {
  if (!nextStatus) return status.value
  return applyStatus(nextStatus)
}

function seedFromUser(user) {
  return seedStatus(user?.subscriptionStatus)
}

function reset() {
  status.value = 'unknown'
  portalUrl.value = ''
  lastFetchedAt.value = 0
  safeRemove(CACHE_KEY)
}

loadFromStorage()

export function useBillingStore() {
  return { status, portalUrl, lastFetchedAt, fetchStatus, seedStatus, seedFromUser, reset }
}
