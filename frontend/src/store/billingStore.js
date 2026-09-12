import { ref } from 'vue'
import BillingService from '@/services/BillingService'
import { readStoredUser } from '@/utils/authStorage'

const status = ref('unknown') // unknown | active | past_due | canceled | inactive
const hasAccess = ref(false)
const portalUrl = ref('')
const lastFetchedAt = ref(0)
const loading = ref(false)
let inflightBasic = null
let inflightWithPortal = null

const BASE_CACHE_KEY = 'snk_billing_status_cache'
const STATUS_CACHE_TTL_MS = 2 * 60 * 1000
const ACTIVE_STALE_REFRESH_MS = 5 * 60 * 1000
let scopedUserId = null

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

function normalizeUserId(value) {
  const next = String(value || '').trim()
  return next || 'guest'
}

function currentUserId() {
  if (scopedUserId) return scopedUserId
  return normalizeUserId(readStoredUser()?.id)
}

function cacheKey() {
  return `${BASE_CACHE_KEY}:${currentUserId()}`
}

function forgetLegacyCache() {
  safeRemove(BASE_CACHE_KEY)
}

function accessFromStatus(value) {
  return ['active', 'trialing'].includes(normalizeStatus(value))
}

function persistStatus() {
  if (status.value === 'unknown') {
    safeRemove(cacheKey())
    return
  }
  safeSet(
    cacheKey(),
    JSON.stringify({
      status: status.value,
      hasAccess: hasAccess.value,
      fetchedAt: lastFetchedAt.value || Date.now(),
    }),
  )
}

function loadFromStorage() {
  try {
    forgetLegacyCache()
    const cached = JSON.parse(safeGet(cacheKey()) || 'null')
    if (!cached?.status || !cached?.fetchedAt) return
    status.value = normalizeStatus(cached.status)
    hasAccess.value = Boolean(cached.hasAccess) || accessFromStatus(cached.status)
    lastFetchedAt.value = Number(cached.fetchedAt) || 0
  } catch {
    safeRemove(cacheKey())
  }
}

function isFresh(ttlMs = STATUS_CACHE_TTL_MS) {
  return status.value !== 'unknown' && Date.now() - lastFetchedAt.value < ttlMs
}

function applyStatus(nextStatus, nextPortalUrl = '', nextHasAccess = undefined) {
  status.value = normalizeStatus(nextStatus)
  hasAccess.value = typeof nextHasAccess === 'boolean' ? nextHasAccess : accessFromStatus(status.value)
  lastFetchedAt.value = Date.now()
  if (nextPortalUrl) portalUrl.value = nextPortalUrl
  persistStatus()
  return status.value
}

function refreshInBackground(includePortal = false) {
  fetchStatus(true, includePortal).catch(() => {})
}

async function fetchStatus(force = false, includePortal = false) {
  if (!force && !includePortal && hasAccess.value) {
    if (!isFresh(ACTIVE_STALE_REFRESH_MS)) refreshInBackground(false)
    return status.value
  }

  if (!force && !includePortal && isFresh()) return status.value

  if (!force && includePortal && portalUrl.value && isFresh()) return status.value

  const currentInflight = includePortal ? inflightWithPortal : inflightBasic
  if (!force && currentInflight) return currentInflight

  const previousStatus = status.value
  loading.value = true
  const request = BillingService.status(includePortal, force)
    .then((res) => {
      applyStatus(res?.data?.status || 'inactive', res?.data?.portalUrl || '', res?.data?.hasAccess)
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
      if (!inflightBasic && !inflightWithPortal) loading.value = false
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
  scopedUserId = normalizeUserId(user?.id)
  return applyStatus(user?.subscriptionStatus, '', user?.hasAccess)
}

function reset() {
  safeRemove(cacheKey())
  forgetLegacyCache()
  scopedUserId = null
  status.value = 'unknown'
  hasAccess.value = false
  portalUrl.value = ''
  lastFetchedAt.value = 0
  loading.value = false
  inflightBasic = null
  inflightWithPortal = null
}

loadFromStorage()

export function useBillingStore() {
  return { status, hasAccess, portalUrl, lastFetchedAt, loading, fetchStatus, seedStatus, seedFromUser, reset }
}
