import { ref } from 'vue'
import AdminService from '@/services/AdminService'
import NotificationService from '@/services/NotificationService'
import {
  ADMIN_NOTIFICATION_PREFIX,
  buildAdministrativeReminderNotifications,
  deriveAdministrativeSummaryParams,
} from '@/utils/adminNotificationBuilder'
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

const serverNotifications = ref([])
const reminderNotifications = ref([])
const serverUnreadCount = ref(0)
const serverTotal = ref(0)

let activeToken = ''
let pollTimer = null
let initPromise = null
let backgroundRefreshTimer = null
const displayedToastIds = new Set()
const toastTimers = new Map()

function getUserId() {
  const auth = useAuthStore()
  return auth.user.value?.id ?? 'anon'
}

function getLastSeenKey() {
  return `snk_notifications_last_seen_${getUserId()}`
}

function getLocalStateKey() {
  return `snk_notifications_local_state_${getUserId()}`
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

function readLocalStateMap() {
  try {
    const raw = localStorage.getItem(getLocalStateKey())
    if (!raw) return {}
    const parsed = JSON.parse(raw)
    return parsed && typeof parsed === 'object' ? parsed : {}
  } catch {
    return {}
  }
}

function writeLocalStateMap(map) {
  try {
    localStorage.setItem(getLocalStateKey(), JSON.stringify(map))
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

function isSyntheticNotificationId(notificationId) {
  return String(notificationId || '').startsWith(ADMIN_NOTIFICATION_PREFIX)
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

function sortNotifications(items) {
  return [...items].sort((a, b) => {
    if (Boolean(a?.isRead) !== Boolean(b?.isRead)) return a?.isRead ? 1 : -1
    return Date.parse(b?.createdAt || 0) - Date.parse(a?.createdAt || 0)
  })
}

function getReminderNotification(notificationId) {
  return reminderNotifications.value.find((item) => item.id === notificationId) || null
}

function syncCombinedState() {
  const visibleReminders = reminderNotifications.value.filter((item) => !item.dismissedAt)
  notifications.value = sortNotifications(
    dedupeById([...visibleReminders, ...serverNotifications.value]),
  )
  total.value = serverTotal.value + visibleReminders.length
  unreadCount.value =
    serverUnreadCount.value + visibleReminders.filter((item) => !item.isRead).length
}

function applyReminderLocalState(items) {
  const localState = readLocalStateMap()
  return items.map((item) => {
    const state = localState[item.id]
    if (!state) return item
    return {
      ...item,
      isRead: Boolean(state.isRead ?? item.isRead),
      readAt: state.readAt || item.readAt || null,
      dismissedAt: state.dismissedAt || item.dismissedAt || null,
    }
  })
}

function setReminderNotifications(items) {
  reminderNotifications.value = sortNotifications(dedupeById(applyReminderLocalState(items)))
  syncCombinedState()
}

function mergeServerNotifications(items, append) {
  if (!append) {
    serverNotifications.value = dedupeById(items)
  } else {
    serverNotifications.value = dedupeById([...serverNotifications.value, ...items])
  }
  syncCombinedState()
}

function setReminderState(notificationId, patch) {
  const localState = readLocalStateMap()
  localState[notificationId] = {
    ...(localState[notificationId] || {}),
    ...patch,
  }
  writeLocalStateMap(localState)
}

function hideReminder(notificationId, { markRead = true } = {}) {
  const now = new Date().toISOString()
  reminderNotifications.value = reminderNotifications.value.map((item) =>
    item.id === notificationId
      ? {
          ...item,
          isRead: markRead ? true : item.isRead,
          readAt: markRead ? now : item.readAt || null,
          dismissedAt: now,
        }
      : item,
  )
  setReminderState(notificationId, {
    isRead: markRead,
    readAt: markRead ? now : null,
    dismissedAt: now,
  })
  dismissToast(notificationId)
  syncCombinedState()
}

async function fetchAdministrativeReminders() {
  try {
    const profile = await AdminService.administrativeProfile()
    const params = deriveAdministrativeSummaryParams(profile, new Date())
    const summary = await AdminService.administrativeSummary(params)
    return buildAdministrativeReminderNotifications(profile, summary, { now: new Date() })
  } catch {
    return []
  }
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
    const [notificationResponse, administrativeReminders] = await Promise.all([
      NotificationService.list({
        page: safePage,
        size: safeSize,
        unreadFirst,
      }),
      safePage === 0 ? fetchAdministrativeReminders() : Promise.resolve(reminderNotifications.value),
    ])

    const data = notificationResponse?.data || {}
    const serverItems = Array.isArray(data?.items) ? data.items : []
    mergeServerNotifications(serverItems, append)

    if (safePage === 0) {
      setReminderNotifications(administrativeReminders)
    }

    page.value = Number.isFinite(data?.page) ? data.page : safePage
    size.value = Number.isFinite(data?.size) ? data.size : safeSize
    hasNext.value = Boolean(data?.hasNext)
    serverTotal.value = Number.isFinite(data?.total) ? data.total : serverNotifications.value.length
    serverUnreadCount.value = Number.isFinite(data?.unreadCount)
      ? data.unreadCount
      : serverNotifications.value.filter((item) => !item.isRead).length
    syncCombinedState()

    if (captureToasts) {
      captureToastsFrom([
        ...serverItems,
        ...(safePage === 0 ? administrativeReminders : reminderNotifications.value),
      ])
    }

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
    serverUnreadCount.value = Number.isFinite(data?.unreadCount)
      ? data.unreadCount
      : serverUnreadCount.value
    syncCombinedState()
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

async function markAsRead(notificationId) {
  if (isSyntheticNotificationId(notificationId)) {
    const previous = getReminderNotification(notificationId)
    if (!previous) return null
    hideReminder(notificationId, { markRead: true })
    return previous
  }

  const index = serverNotifications.value.findIndex((item) => item.id === notificationId)
  if (index < 0) return null

  const previous = { ...serverNotifications.value[index] }
  serverNotifications.value = serverNotifications.value.filter((item) => item.id !== notificationId)
  dismissToast(notificationId)
  if (!previous.isRead) {
    serverUnreadCount.value = Math.max(0, serverUnreadCount.value - 1)
  }
  serverTotal.value = Math.max(0, serverTotal.value - 1)
  syncCombinedState()

  try {
    const { data } = await NotificationService.markRead(notificationId)
    return data || previous
  } catch (err) {
    const restored = [...serverNotifications.value]
    restored.splice(index, 0, previous)
    serverNotifications.value = restored
    if (!previous.isRead) serverUnreadCount.value += 1
    serverTotal.value += 1
    syncCombinedState()
    throw err
  }
}

async function markAllAsRead() {
  const visibleServerIds = serverNotifications.value.map((item) => item.id).filter(Boolean)
  const visibleReminderIds = reminderNotifications.value
    .filter((item) => !item.dismissedAt)
    .map((item) => item.id)
    .filter(Boolean)

  if (visibleServerIds.length === 0 && visibleReminderIds.length === 0) return 0

  const previousServerNotifications = serverNotifications.value
  const previousReminderNotifications = reminderNotifications.value
  const previousToasts = toastItems.value
  const previousServerTotal = serverTotal.value
  const previousServerUnread = serverUnreadCount.value
  const previousLocalState = readLocalStateMap()

  const visibleIdSet = new Set([...visibleServerIds, ...visibleReminderIds])
  toastItems.value = toastItems.value.filter((toast) => !visibleIdSet.has(toast.id))
  visibleIdSet.forEach(clearToastTimer)

  for (const notificationId of visibleReminderIds) {
    hideReminder(notificationId, { markRead: true })
  }

  serverNotifications.value = []
  serverUnreadCount.value = 0
  serverTotal.value = Math.max(0, serverTotal.value - visibleServerIds.length)
  syncCombinedState()

  try {
    if (visibleServerIds.length > 0) {
      const { data } = await NotificationService.markAllRead()
      return Number.isFinite(data?.updated)
        ? data.updated + visibleReminderIds.length
        : visibleServerIds.length + visibleReminderIds.length
    }
    return visibleReminderIds.length
  } catch (err) {
    serverNotifications.value = previousServerNotifications
    reminderNotifications.value = previousReminderNotifications
    toastItems.value = previousToasts
    serverTotal.value = previousServerTotal
    serverUnreadCount.value = previousServerUnread
    writeLocalStateMap(previousLocalState)
    syncCombinedState()
    throw err
  }
}

async function dismissNotification(notificationId) {
  if (isSyntheticNotificationId(notificationId)) {
    hideReminder(notificationId, { markRead: true })
    return
  }

  const index = serverNotifications.value.findIndex((item) => item.id === notificationId)
  if (index < 0) return

  const previous = serverNotifications.value[index]
  serverNotifications.value = serverNotifications.value.filter((item) => item.id !== notificationId)
  dismissToast(notificationId)

  if (!previous.isRead) {
    serverUnreadCount.value = Math.max(0, serverUnreadCount.value - 1)
  }
  serverTotal.value = Math.max(0, serverTotal.value - 1)
  syncCombinedState()

  try {
    await NotificationService.dismiss(notificationId)
  } catch (err) {
    const restored = [...serverNotifications.value]
    restored.splice(index, 0, previous)
    serverNotifications.value = restored
    if (!previous.isRead) serverUnreadCount.value += 1
    serverTotal.value += 1
    syncCombinedState()
    throw err
  }
}

function openCenter() {
  centerOpen.value = true
  if (!notifications.value.length && !loading.value) {
    refreshLatest({ captureToasts: false }).catch(() => {})
  }
}

function closeCenter() {
  centerOpen.value = false
}

function toggleCenter() {
  centerOpen.value = !centerOpen.value
  if (centerOpen.value && !notifications.value.length && !loading.value) {
    refreshLatest({ captureToasts: false }).catch(() => {})
  }
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
    refreshUnreadCount().catch(() => {})
  }, POLL_INTERVAL_MS)
}

function stopPolling() {
  if (!pollTimer) return
  window.clearInterval(pollTimer)
  pollTimer = null
}

function resetState() {
  stopPolling()
  if (backgroundRefreshTimer) {
    window.clearTimeout(backgroundRefreshTimer)
    backgroundRefreshTimer = null
  }
  clearAllToastTimers()
  displayedToastIds.clear()

  notifications.value = []
  serverNotifications.value = []
  reminderNotifications.value = []
  unreadCount.value = 0
  serverUnreadCount.value = 0
  loading.value = false
  error.value = ''
  page.value = 0
  size.value = DEFAULT_PAGE_SIZE
  hasNext.value = false
  total.value = 0
  serverTotal.value = 0
  centerOpen.value = false
  syncing.value = false
  toastItems.value = []
  initPromise = null
}

function scheduleBackgroundRefresh() {
  if (backgroundRefreshTimer || !activeToken) return
  backgroundRefreshTimer = window.setTimeout(() => {
    backgroundRefreshTimer = null
    syncNow()
      .then(() => refreshLatest({ captureToasts: true }))
      .then(() => refreshUnreadCount())
      .catch(() => {})
  }, 1800)
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
    await refreshUnreadCount()
    startPolling()
    scheduleBackgroundRefresh()
  })()

  try {
    await initPromise
  } finally {
    initPromise = null
  }
}

function teardown({ clearState = false } = {}) {
  stopPolling()
  if (backgroundRefreshTimer) {
    window.clearTimeout(backgroundRefreshTimer)
    backgroundRefreshTimer = null
  }
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
