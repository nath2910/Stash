import { computed, ref, watch } from 'vue'
import { useAuthStore } from '@/store/authStore'

const STORAGE_KEY_PREFIX = 'snk_stats_range_v1'

function ymd(d) {
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

function defaultRange() {
  const now = new Date()
  const oneMonthAgo = new Date(now)
  oneMonthAgo.setMonth(now.getMonth() - 1)
  return { from: ymd(oneMonthAgo), to: ymd(now) }
}

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

function normalizeRange(raw) {
  if (!raw || typeof raw !== 'object') return null
  const from = typeof raw.from === 'string' ? raw.from : ''
  const to = typeof raw.to === 'string' ? raw.to : ''
  if (!/^\d{4}-\d{2}-\d{2}$/.test(from) || !/^\d{4}-\d{2}-\d{2}$/.test(to)) return null
  if (from <= to) return { from, to }
  return { from: to, to: from }
}

export function useStatsRange() {
  const { user } = useAuthStore()
  const userId = computed(() => user.value?.id ?? 'guest')
  const from = ref('')
  const to = ref('')

  function applyRange(nextFrom, nextTo) {
    const normalized = normalizeRange({ from: nextFrom, to: nextTo })
    if (!normalized) return false
    let changed = false
    if (from.value !== normalized.from) {
      from.value = normalized.from
      changed = true
    }
    if (to.value !== normalized.to) {
      to.value = normalized.to
      changed = true
    }
    return changed
  }

  function loadForUser() {
    const key = `${STORAGE_KEY_PREFIX}_${userId.value}`
    const raw = safeGet(key)
    if (raw) {
      try {
        const parsed = JSON.parse(raw)
        const normalized = normalizeRange(parsed)
        if (normalized) {
          applyRange(normalized.from, normalized.to)
          return
        }
      } catch {
        // ignore
      }
    }
    const def = defaultRange()
    applyRange(def.from, def.to)
  }

  loadForUser()

  watch(userId, () => {
    loadForUser()
  })

  watch(
    [from, to],
    () => {
      if (!from.value || !to.value) return
      const key = `${STORAGE_KEY_PREFIX}_${userId.value}`
      safeSet(key, JSON.stringify({ from: from.value, to: to.value }))
    },
    { deep: false },
  )

  return { from, to, setRange: applyRange }
}
