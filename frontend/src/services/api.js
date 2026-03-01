import axios from 'axios'

const baseURL =
  import.meta.env.VITE_API_URL || import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

if (import.meta.env.DEV) {
  console.log('API baseURL =', baseURL)
}

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

api.interceptors.request.use((config) => {
  const token = getToken()
  if (token) {
    // plus robuste avec AxiosHeaders
    config.headers = config.headers || {}
    config.headers.Authorization = `Bearer ${token}`
  }

  if (import.meta.env.DEV) {
    console.log('REQ', config.method?.toUpperCase(), config.baseURL + config.url, {
      hasToken: !!token,
    })
  }
  return config
})

let redirectingOnAuth = false

api.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error?.response?.status
    const url = error?.config?.url || ''

    if ((status === 401 || status === 403) && !url.startsWith('/auth')) {
      localStorage.removeItem('snk_token')
      localStorage.removeItem('snk_user')
      sessionStorage.removeItem('snk_token')
      sessionStorage.removeItem('snk_user')

      if (!redirectingOnAuth && window.location.pathname !== '/auth') {
        redirectingOnAuth = true
        window.location.assign('/auth?mode=login')
      }
    }

    return Promise.reject(error)
  },
)

export default api
