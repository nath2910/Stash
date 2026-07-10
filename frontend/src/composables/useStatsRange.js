import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useAuthStore } from '@/store/authStore'
import {
  STATS_RANGE_STORAGE_KEY_PREFIX,
  getMonthToDateRange,
  resolveStoredStatsRange,
  buildStoredStatsRange,
} from '@/utils/statsRangeStorage'

function safeGet(key) {
  try {
    return localStorage.getItem(key)
  } catch {
    return null
  }
}

function safeSet(key, value) {
  try {
    localStorage.setItem(key, value)
  } catch {
    // ignore
  }
}

export function useStatsRange() {
  const { user } = useAuthStore()
  const userId = computed(() => user.value?.id ?? 'guest')
  const from = ref('')
  const to = ref('')

  let rolloverTimer = null

  function storageKey() {
    return `${STATS_RANGE_STORAGE_KEY_PREFIX}_${userId.value}`
  }

  function applyStoredRecord(record) {
    if (!record) return false
    let changed = false
    if (from.value !== record.from) {
      from.value = record.from
      changed = true
    }
    if (to.value !== record.to) {
      to.value = record.to
      changed = true
    }
    return changed
  }

  function saveCurrentRange(baseDate = new Date()) {
    if (!from.value || !to.value) return
    const record = buildStoredStatsRange(from.value, to.value, { baseDate })
    if (!record) return
    safeSet(storageKey(), JSON.stringify(record))
  }

  function loadForUser(baseDate = new Date()) {
    const raw = safeGet(storageKey())
    if (raw) {
      try {
        const resolved = resolveStoredStatsRange(JSON.parse(raw), { baseDate })
        applyStoredRecord(resolved)
        safeSet(storageKey(), JSON.stringify(resolved))
        return
      } catch {
        // ignore malformed storage and reset below
      }
    }

    const next = resolveStoredStatsRange(getMonthToDateRange(baseDate), { baseDate })
    applyStoredRecord(next)
    safeSet(storageKey(), JSON.stringify(next))
  }

  function msUntilNextRefresh(baseDate = new Date()) {
    const next = new Date(baseDate)
    next.setHours(24, 0, 5, 0)
    return Math.max(30_000, next.getTime() - baseDate.getTime())
  }

  function clearRolloverTimer() {
    if (!rolloverTimer) return
    window.clearTimeout(rolloverTimer)
    rolloverTimer = null
  }

  function scheduleRolloverCheck() {
    clearRolloverTimer()
    if (typeof window === 'undefined') return
    rolloverTimer = window.setTimeout(() => {
      const now = new Date()
      loadForUser(now)
      scheduleRolloverCheck()
    }, msUntilNextRefresh())
  }

  function handleVisibilityChange() {
    if (document.visibilityState !== 'visible') return
    loadForUser(new Date())
    scheduleRolloverCheck()
  }

  loadForUser()

  watch(userId, () => {
    loadForUser(new Date())
  })

  watch(
    [from, to],
    () => {
      saveCurrentRange(new Date())
    },
    { deep: false },
  )

  onMounted(() => {
    if (typeof document !== 'undefined') {
      document.addEventListener('visibilitychange', handleVisibilityChange)
    }
    scheduleRolloverCheck()
  })

  onBeforeUnmount(() => {
    clearRolloverTimer()
    if (typeof document !== 'undefined') {
      document.removeEventListener('visibilitychange', handleVisibilityChange)
    }
  })

  function setRange(nextFrom, nextTo) {
    const resolved = resolveStoredStatsRange({ from: nextFrom, to: nextTo, preset: 'custom' }, {
      baseDate: new Date(),
      fallbackPreset: 'custom',
    })
    return applyStoredRecord(resolved)
  }

  return { from, to, setRange }
}
