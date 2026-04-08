import { ref } from 'vue'
import NotificationService from '@/services/NotificationService'
import { useAuthStore } from './authStore'

const DEFAULT_PAGE_SIZE = 20
const MAX_PAGE_SIZE = 50
const POLL_INTERVAL_MS = 60_000
const TOAST_LIMIT = 3
const TOAST_AUTO_DISMISS_MS = 5200

const notifications = ref([])
const unreadCount = ref(0)
const loading = ref(false)
const error = ref('')
const page = ref(0)
const size = ref(DEFAULT_PAGE_SIZE)
const hasNext = ref(false)
const total = ref(0)
const centerOpen = ref(false)
const syncing = ref(false)
const toastItems = ref([])

let activeToken = ''
let pollTimer = null
let initPromise = null
const displayedToastIds = new Set()
const toastTimers = new Map()

function getUserId() {
  const auth = useAuthStore()
  return auth.user.value?.id ?? 'anon'
}

function getLastSeenKey() {
  return `snk_notifications_last_seen_${getUserId()}`
}

function readLastSeenMs() {
  try {
    const raw = localStorage.getItem(getLastSeenKey())
    if (!raw) return null
    const parsed = Number(raw)
    return Number.isFinite(parsed) ? parsed : null
  } catch {
    return null
  }
}

function writeLastSeenMs(value) {
  try {
    localStorage.setItem(getLastSeenKey(), String(value))
  } catch {
    // ignore storage failures
  }
}

function ensureLastSeenBaseline() {
  const current = readLastSeenMs()
  if (current != null) return current
  const baseline = Date.now()
  writeLastSeenMs(baseline)
  return baseline
}

function clearToastTimer(notificationId) {
  const timer = toastTimers.get(notificationId)
  if (!timer) return
  window.clearTimeout(timer)
  toastTimers.delete(notificationId)
}

function clearAllToastTimers() {
  for (const timer of toastTimers.values()) {
    window.clearTimeout(timer)
  }
  toastTimers.clear()
}

function isInfo(notification) {
  return String(notification?.severity || '').toUpperCase() === 'INFO'
}

function dismissToast(notificationId) {
  clearToastTimer(notificationId)
  toastItems.value = toastItems.value.filter((toast) => toast.id !== notificationId)
}

function queueToast(notification) {
  if (!notification?.id || displayedToastIds.has(notification.id)) return
  if (notification.dismissedAt || notification.isRead) return
  if (toastItems.value.length >= TOAST_LIMIT) return

  displayedToastIds.add(notification.id)
  toastItems.value = [...toastItems.value, notification]

  if (isInfo(notification)) {
    const timer = window.setTimeout(() => {
      dismissToast(notification.id)
    }, TOAST_AUTO_DISMISS_MS)
    toastTimers.set(notification.id, timer)
  }
}

function captureToastsFrom(items) {
  if (!Array.isArray(items) || items.length === 0) return

  const lastSeen = ensureLastSeenBaseline()
  let nextLastSeen = lastSeen

  const ordered = [...items].sort((a, b) => {
    const aTs = Date.parse(a?.createdAt || 0)
    const bTs = Date.parse(b?.createdAt || 0)
    return aTs - bTs
  })

  for (const notification of ordered) {
    const createdAtMs = Date.parse(notification?.createdAt || 0)
    if (Number.isFinite(createdAtMs) && createdAtMs > nextLastSeen) {
      nextLastSeen = createdAtMs
    }
    if (!Number.isFinite(createdAtMs) || createdAtMs <= lastSeen) {
      continue
    }
    queueToast(notification)
  }

  writeLastSeenMs(nextLastSeen)
}

function dedupeById(items) {
  const seen = new Set()
  const deduped = []
  for (const item of items) {
    if (!item?.id || seen.has(item.id)) continue
    seen.add(item.id)
    deduped.push(item)
  }
  return deduped
}

function mergeItems(items, append) {
  if (!append) {
    notifications.value = dedupeById(items)
    return
  }
  notifications.value = dedupeById([...notifications.value, ...items])
}

async function fetchNotifications({
  pageIndex = 0,
  pageSize = DEFAULT_PAGE_SIZE,
  append = false,
  unreadFirst = true,
  captureToasts = false,
} = {}) {
  const safePage = Math.max(pageIndex, 0)
  const safeSize = Math.min(Math.max(pageSize, 1), MAX_PAGE_SIZE)

  loading.value = true
  error.value = ''

  try {
    const { data } = await NotificationService.list({
      page: safePage,
      size: safeSize,
      unreadFirst,
    })

    const items = Array.isArray(data?.items) ? data.items : []
    if (captureToasts) captureToastsFrom(items)

    mergeItems(items, append)

    page.value = Number.isFinite(data?.page) ? data.page : safePage
    size.value = Number.isFinite(data?.size) ? data.size : safeSize
    hasNext.value = Boolean(data?.hasNext)
    total.value = Number.isFinite(data?.total) ? data.total : notifications.value.length
    unreadCount.value = Number.isFinite(data?.unreadCount)
      ? data.unreadCount
      : notifications.value.filter((item) => !item.isRead).length

    return notifications.value
  } catch (err) {
    error.value = err?.response?.data?.message || 'Impossible de charger les notifications.'
    throw err
  } finally {
    loading.value = false
  }
}

async function refreshUnreadCount() {
  try {
    const { data } = await NotificationService.unreadCount()
    unreadCount.value = Number.isFinite(data?.unreadCount) ? data.unreadCount : unreadCount.value
  } catch {
    // keep current value
  }
}

async function refreshLatest({ captureToasts = true } = {}) {
  return fetchNotifications({
    pageIndex: 0,
    pageSize: size.value || DEFAULT_PAGE_SIZE,
    append: false,
    unreadFirst: true,
    captureToasts,
  })
}

async function loadMore() {
  if (!hasNext.value || loading.value) return
  const nextPage = page.value + 1
  return fetchNotifications({
    pageIndex: nextPage,
    pageSize: size.value || DEFAULT_PAGE_SIZE,
    append: true,
    unreadFirst: true,
    captureToasts: false,
  })
}

function applyUpdatedNotification(updated) {
  if (!updated?.id) return
  const index = notifications.value.findIndex((item) => item.id === updated.id)
  if (index >= 0) {
    const clone = [...notifications.value]
    clone[index] = updated
    notifications.value = clone
  }
}

async function markAsRead(notificationId) {
  const index = notifications.value.findIndex((item) => item.id === notificationId)
  if (index < 0) return null

  const previous = { ...notifications.value[index] }
  if (previous.isRead) return previous

  const optimistic = {
    ...previous,
    isRead: true,
    readAt: previous.readAt || new Date().toISOString(),
  }
  applyUpdatedNotification(optimistic)
  unreadCount.value = Math.max(0, unreadCount.value - 1)

  try {
    const { data } = await NotificationService.markRead(notificationId)
    if (data) {
      applyUpdatedNotification(data)
      return data
    }
    return optimistic
  } catch (err) {
    applyUpdatedNotification(previous)
    unreadCount.value += 1
    throw err
  }
}

async function markAllAsRead() {
  const unreadIds = notifications.value.filter((item) => !item.isRead).map((item) => item.id)
  if (unreadIds.length === 0) return 0

  const nowIso = new Date().toISOString()
  notifications.value = notifications.value.map((item) =>
    item.isRead ? item : { ...item, isRead: true, readAt: item.readAt || nowIso },
  )
  const before = unreadCount.value
  unreadCount.value = 0

  try {
    const { data } = await NotificationService.markAllRead()
    return Number.isFinite(data?.updated) ? data.updated : unreadIds.length
  } catch (err) {
    const rollback = new Set(unreadIds)
    notifications.value = notifications.value.map((item) =>
      rollback.has(item.id) ? { ...item, isRead: false, readAt: null } : item,
    )
    unreadCount.value = before
    throw err
  }
}

async function dismissNotification(notificationId) {
  const index = notifications.value.findIndex((item) => item.id === notificationId)
  if (index < 0) return

  const previous = notifications.value[index]
  notifications.value = notifications.value.filter((item) => item.id !== notificationId)
  dismissToast(notificationId)

  if (!previous.isRead) {
    unreadCount.value = Math.max(0, unreadCount.value - 1)
  }

  try {
    await NotificationService.dismiss(notificationId)
  } catch (err) {
    const restored = [...notifications.value]
    restored.splice(index, 0, previous)
    notifications.value = restored
    if (!previous.isRead) unreadCount.value += 1
    throw err
  }
}

function openCenter() {
  centerOpen.value = true
}

function closeCenter() {
  centerOpen.value = false
}

function toggleCenter() {
  centerOpen.value = !centerOpen.value
}

async function syncNow() {
  syncing.value = true
  try {
    const { data } = await NotificationService.sync()
    return Number.isFinite(data?.created) ? data.created : 0
  } catch {
    return 0
  } finally {
    syncing.value = false
  }
}

function startPolling() {
  if (pollTimer || !activeToken) return
  pollTimer = window.setInterval(() => {
    refreshLatest({ captureToasts: true }).catch(() => {
      // silent: polling must remain non intrusive
    })
  }, POLL_INTERVAL_MS)
}

function stopPolling() {
  if (!pollTimer) return
  window.clearInterval(pollTimer)
  pollTimer = null
}

function resetState() {
  stopPolling()
  clearAllToastTimers()
  displayedToastIds.clear()

  notifications.value = []
  unreadCount.value = 0
  loading.value = false
  error.value = ''
  page.value = 0
  size.value = DEFAULT_PAGE_SIZE
  hasNext.value = false
  total.value = 0
  centerOpen.value = false
  syncing.value = false
  toastItems.value = []
  initPromise = null
}

async function init({ force = false } = {}) {
  const auth = useAuthStore()
  const token = auth.token.value || ''

  if (!token) {
    activeToken = ''
    resetState()
    return
  }

  if (!force && initPromise) return initPromise

  if (activeToken && activeToken !== token) {
    resetState()
  }
  activeToken = token

  initPromise = (async () => {
    ensureLastSeenBaseline()
    await syncNow()
    await refreshLatest({ captureToasts: true })
    await refreshUnreadCount()
    startPolling()
  })()

  try {
    await initPromise
  } finally {
    initPromise = null
  }
}

function teardown({ clearState = false } = {}) {
  stopPolling()
  if (clearState) {
    activeToken = ''
    resetState()
  }
}

export function useNotificationStore() {
  return {
    notifications,
    unreadCount,
    loading,
    error,
    page,
    size,
    hasNext,
    total,
    centerOpen,
    syncing,
    toastItems,
    init,
    syncNow,
    refreshLatest,
    refreshUnreadCount,
    loadMore,
    markAsRead,
    markAllAsRead,
    dismissNotification,
    openCenter,
    closeCenter,
    toggleCenter,
    dismissToast,
    teardown,
  }
}