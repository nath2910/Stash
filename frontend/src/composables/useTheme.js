import { computed, ref } from 'vue'

const THEME_KEY = 'snk_theme_mode_v1'
const FALLBACK_THEME = 'dark'
const VALID_THEMES = ['dark', 'light']

const theme = ref(FALLBACK_THEME)

let initialized = false
let storageListenerBound = false

function sanitizeTheme(value) {
  return VALID_THEMES.includes(value) ? value : null
}

function readStoredTheme() {
  try {
    const fromLocal = sanitizeTheme(localStorage.getItem(THEME_KEY))
    if (fromLocal) return fromLocal
  } catch {}

  try {
    const fromSession = sanitizeTheme(sessionStorage.getItem(THEME_KEY))
    if (fromSession) return fromSession
  } catch {}

  return null
}

function inferSystemTheme() {
  if (typeof window === 'undefined' || typeof window.matchMedia !== 'function') return FALLBACK_THEME
  return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
}

function persistTheme(nextTheme) {
  try {
    localStorage.setItem(THEME_KEY, nextTheme)
    return
  } catch {}

  try {
    sessionStorage.setItem(THEME_KEY, nextTheme)
  } catch {}
}

function setTheme(nextTheme) {
  const safeTheme = sanitizeTheme(nextTheme) ?? FALLBACK_THEME
  theme.value = safeTheme
  persistTheme(safeTheme)
}

function toggleTheme() {
  setTheme(theme.value === 'dark' ? 'light' : 'dark')
}

function bindStorageListener() {
  if (typeof window === 'undefined' || storageListenerBound) return
  window.addEventListener('storage', (event) => {
    if (event.key !== THEME_KEY) return
    const nextTheme = sanitizeTheme(event.newValue)
    if (!nextTheme || nextTheme === theme.value) return
    theme.value = nextTheme
  })
  storageListenerBound = true
}

function initTheme() {
  if (!initialized) {
    theme.value = readStoredTheme() ?? inferSystemTheme()
    initialized = true
  }
  bindStorageListener()
  return theme.value
}

const isDarkTheme = computed(() => theme.value === 'dark')

export function useTheme() {
  initTheme()
  return {
    theme,
    isDarkTheme,
    setTheme,
    toggleTheme,
  }
}

export { initTheme, THEME_KEY }
