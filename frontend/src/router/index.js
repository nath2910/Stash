import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/store/authStore.js'
import { useBillingStore } from '@/store/billingStore.js'
import AuthService from '@/services/AuthService.js'

const HomePage = () => import('@/pages/homePage.vue')
const StatsPage = () => import('@/pages/statsPage.vue')
const GestionPage = () => import('@/pages/gestionPage.vue')
const AboPage = () => import('@/pages/aboPage.vue')
const AboViewPage = () => import('@/pages/aboViewPage.vue')
const AuthPage = () => import('@/pages/AuthPage.vue')
const DiscoverPage = () => import('@/pages/DiscoverPage.vue')
const AccountPage = () => import('@/pages/accountPage.vue')
const AuthCallbackPage = () => import('@/pages/authCallbackPage.vue')
const ForgotPasswordPage = () => import('@/pages/ForgotPasswordPage.vue')
const ResetPasswordPage = () => import('@/pages/ResetPasswordPage.vue')
const VerifyEmailPage = () => import('@/pages/VerifyEmailPage.vue')

function decodeJwtPayload(token) {
  try {
    const payload = token.split('.')[1]
    if (!payload) return null
    const base = payload.replace(/-/g, '+').replace(/_/g, '/')
    const padded = base + '==='.slice((base.length + 3) % 4)
    return JSON.parse(atob(padded))
  } catch {
    return null
  }
}

function isTokenExpired(token) {
  const payload = decodeJwtPayload(token)
  const exp = payload?.exp
  if (!exp) return true
  const now = Date.now()
  return now >= exp * 1000 - 30_000
}

let protectedChunksWarmed = false

function canWarmRouteChunks() {
  if (typeof navigator === 'undefined') return true
  const connection = navigator.connection || navigator.mozConnection || navigator.webkitConnection
  if (!connection) return true
  if (connection.saveData) return false
  return !['slow-2g', '2g'].includes(connection.effectiveType)
}

function runWhenIdle(callback) {
  if (typeof window === 'undefined') return
  if ('requestIdleCallback' in window) {
    window.requestIdleCallback(callback, { timeout: 2500 })
    return
  }
  window.setTimeout(callback, 1200)
}

function warmProtectedRouteChunks() {
  if (protectedChunksWarmed || !canWarmRouteChunks()) return
  protectedChunksWarmed = true
  runWhenIdle(() => {
    Promise.allSettled([GestionPage(), StatsPage(), AccountPage()]).catch(() => {})
  })
}

const router = createRouter({
  history: createWebHistory(),
  routes: [
    // ✅ callback OAuth en premier (avant le catch-all)
    {
      path: '/auth/callback',
      name: 'authCallback',
      component: AuthCallbackPage,
      meta: { fullBleed: true },
    },

    // auth
    {
      path: '/auth',
      name: 'auth',
      component: AuthPage,
      meta: { fullBleed: true, allowScroll: true },
    },
    {
      path: '/discover',
      name: 'discover',
      component: DiscoverPage,
      meta: { fullBleed: true, allowScroll: true },
    },
    {
      path: '/admin',
      name: 'admin',
      redirect: (to) => ({
        name: 'gestion',
        query: { ...to.query, tab: 'admin' },
      }),
      meta: { requiresAuth: true },
    },
    {
      path: '/forgot-password',
      name: 'forgot-password',
      component: ForgotPasswordPage,
      meta: { fullBleed: true, allowScroll: true },
    },
    {
      path: '/reset-password',
      name: 'reset-password',
      component: ResetPasswordPage,
      meta: { fullBleed: true, allowScroll: true },
    },
    {
      path: '/verify-email',
      name: 'verify-email',
      component: VerifyEmailPage,
      meta: { fullBleed: true, allowScroll: true },
    },
    // lien court depuis l'email: /v/<token>
    {
      path: '/v/:token',
      name: 'verify-email-short',
      component: VerifyEmailPage,
      meta: { fullBleed: true, allowScroll: true },
    },

    // ✅ pages protégées, required auth permet de bloque des pages si pas connecte
    {
      path: '/',
      name: 'home',
      component: HomePage,
      meta: { requiresAuth: true, wideContent: true },
    },
    {
      path: '/stats',
      name: 'stats',
      component: StatsPage,
      meta: {
        fullBleed: true,
        requiresAuth: true,
        transition: 'page-canvas',
        transitionMode: 'out-in',
      },
    },
    {
      path: '/gestion',
      name: 'gestion',
      component: GestionPage,
      meta: { requiresAuth: true, wideContent: true },
    },
    {
      path: '/compte',
      name: 'account',
      component: AccountPage,
      meta: {
        requiresAuth: true,
        fullBleed: true,
        allowScroll: true,
        hidePrimaryNav: true,
        transition: 'page-canvas',
        transitionMode: 'out-in',
      },
    },
    {
      path: '/abo',
      name: 'abo',
      component: AboPage,
      meta: {
        requiresAuth: true,
        allowInactive: true,
        fullBleed: true,
        allowScroll: true,
        hidePrimaryNav: true,
      },
    },
    {
      path: '/mon-abonnement',
      name: 'abo-view',
      component: AboViewPage,
      meta: {
        requiresAuth: true,
        allowInactive: true,
        fullBleed: true,
        allowScroll: true,
        hidePrimaryNav: true,
      },
    },

    // ✅ toujours en dernier
    { path: '/:pathMatch(.*)*', name: 'not-found', redirect: '/' },
  ],
  scrollBehavior() {
    return { top: 0 }
  },
})

const publicRoutes = new Set([
  'auth',
  'discover',
  'authCallback',
  'forgot-password',
  'reset-password',
  'verify-email',
  'verify-email-short',
])

router.beforeEach(async (to) => {
  const auth = useAuthStore()
  const billing = useBillingStore()
  const token = auth.token.value
  const hasToken = !!token
  const isPublic = publicRoutes.has(to.name)

  if (hasToken && isTokenExpired(token)) {
    auth.logout()
    if (!isPublic) {
      return { name: 'auth', query: { mode: 'login' } }
    }
  }

  if (!hasToken && !isPublic) {
    return { name: 'auth', query: { mode: 'login' } }
  }

  if (hasToken && !auth.user.value) {
    try {
      const me = await AuthService.me()
      auth.setUser(me)
    } catch {
      auth.logout()
      if (!isPublic) {
        return { name: 'auth', query: { mode: 'login' } }
      }
    }
  }

  if (to.name === 'auth' && auth.token.value) {
    await billing.fetchStatus()
    return billing.status.value === 'active' ? { name: 'home' } : { name: 'abo' }
  }

  // Si page protégée, vérifier l'abo (sauf si allowInactive)
  const requiresAuth = to.meta.requiresAuth === true
  const allowInactive = to.meta.allowInactive === true
  if (requiresAuth) {
    await billing.fetchStatus()
    if (!allowInactive && billing.status.value !== 'active') {
      return { name: 'abo', query: { returnTo: to.fullPath } }
    }
    // Si abo actif et il vient sur /abo, on redirige vers la vue abonnement
    if (to.name === 'abo' && billing.status.value === 'active') {
      const target = to.query?.returnTo || '/'
      if (target && typeof target === 'string') {
        return target.startsWith('/') ? target : { path: target }
      }
      return { name: 'home' }
    }
  }
})

router.afterEach((to) => {
  if (!publicRoutes.has(to.name) && useAuthStore().token.value) {
    warmProtectedRouteChunks()
  }
})

export default router
