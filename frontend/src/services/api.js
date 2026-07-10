import axios from 'axios'

const metaEnv = typeof import.meta !== 'undefined' ? import.meta.env : {}
const LOCAL_BACKEND_ORIGIN = 'http://localhost:8080'

function isLocalHostname(hostname = '') {
  return (
    hostname === 'localhost' ||
    hostname === '127.0.0.1' ||
    hostname === '[::1]' ||
    hostname.endsWith('.localhost')
  )
}

function inferApiBaseURL() {
  if (typeof window === 'undefined' || !window.location) return ''

  const { origin, protocol, hostname } = window.location
  if (!hostname) return ''
  if (isLocalHostname(hostname)) return LOCAL_BACKEND_ORIGIN
  if (/^api[.-]/i.test(hostname)) return origin

  const rootHost = hostname.replace(/^(www\.|app\.|frontend\.)/i, '')
  return `${protocol}//api.${rootHost}`
}
const rawBaseURL =
  metaEnv?.VITE_API_URL ||
  metaEnv?.VITE_API_BASE_URL ||
  process.env.VITE_API_URL ||
  process.env.VITE_API_BASE_URL ||
  inferApiBaseURL() ||
  LOCAL_BACKEND_ORIGIN
const baseURL = rawBaseURL.replace(/\/+$/, '') // évite les doubles slash dans les appels

const api = axios.create({
  baseURL,
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json',
    Accept: 'application/json',
  },
})

function getToken() {
  return localStorage.getItem('snk_token') || sessionStorage.getItem('snk_token') || ''
}

function clearAuthStorage() {
  localStorage.removeItem('snk_token')
  localStorage.removeItem('snk_user')
  sessionStorage.removeItem('snk_token')
  sessionStorage.removeItem('snk_user')
}

function redirectToLogin(reason = 'auth') {
  if (redirectingOnAuth || window.location.pathname === '/auth') return
  redirectingOnAuth = true
  const params = new URLSearchParams({ mode: 'login', reason })
  window.location.assign(`/auth?${params.toString()}`)
}

api.interceptors.request.use((config) => {
  const token = getToken()
  if (token) {
    // plus robuste avec AxiosHeaders
    config.headers = config.headers || {}
    config.headers.Authorization = `Bearer ${token}`
  }

  return config
})

let redirectingOnAuth = false

api.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error?.response?.status
    const url = error?.config?.url || ''
    const isNetworkError =
      !error?.response &&
      (error?.code === 'ECONNABORTED' ||
        error?.message?.toLowerCase?.().includes('network') ||
        error?.message?.toLowerCase?.().includes('timeout'))

    if (isNetworkError || status === 0) {
      return Promise.reject(error)
    }

    // Token expirÃ© ou non autorisÃ©
    if ((status === 401 || status === 403) && !url.startsWith('/auth')) {
      clearAuthStorage()
      redirectToLogin('unauthorized')
    }

    return Promise.reject(error)
  },
)

export default api
