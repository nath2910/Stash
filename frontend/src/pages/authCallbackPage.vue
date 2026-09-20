<script setup>
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/store/authStore.js'
import AuthService from '@/services/AuthService.js'

const router = useRouter()
const auth = useAuthStore()
const POST_AUTH_REDIRECT_KEY = 'snk_post_auth_redirect'
const CALLBACK_NAVIGATION_TIMEOUT_MS = 3500

function decodeUserPayload(value) {
  if (!value) return null
  try {
    const normalized = value.replace(/-/g, '+').replace(/_/g, '/')
    const padded = normalized + '='.repeat((4 - (normalized.length % 4)) % 4)
    const binary = window.atob(padded)
    const bytes = Uint8Array.from(binary, (char) => char.charCodeAt(0))
    return JSON.parse(new TextDecoder().decode(bytes))
  } catch {
    return null
  }
}

function resolvePostAuthTarget() {
  // The OAuth profile does not contain the live Discord eligibility result.
  // Always request the intended app route; its guard resolves access before
  // redirecting a genuinely inactive account to the subscription page.
  return { name: 'home' }
}

function persistPostAuthRedirect(target) {
  try {
    window.sessionStorage.setItem(
      POST_AUTH_REDIRECT_KEY,
      JSON.stringify({
        name: String(target?.name || 'home'),
        ts: Date.now(),
      }),
    )
  } catch {
    // Ignore storage failures and rely on the direct fallback below.
  }
}

function targetPath() {
  return '/'
}

async function navigateAfterSso(target) {
  persistPostAuthRedirect(target)

  try {
    await Promise.race([
      router.replace(target),
      new Promise((_, reject) => {
        window.setTimeout(() => reject(new Error('post_auth_navigation_timeout')), CALLBACK_NAVIGATION_TIMEOUT_MS)
      }),
    ])
  } catch (error) {
    console.warn('Navigation post-SSO forcee', error)
    window.location.replace(targetPath(target))
  }
}

onMounted(async () => {
  const hash = window.location.hash || ''
  const params = new URLSearchParams(hash.replace('#', ''))
  const token = params.get('token')
  const userPayload = decodeUserPayload(params.get('user'))
  const error = params.get('error')

  if (error) {
    router.replace({ name: 'auth', query: { mode: 'login', ssoError: error } })
    return
  }

  if (!token) {
    router.replace({ name: 'auth', query: { mode: 'login' } })
    return
  }

  auth.setToken(token)
  window.history.replaceState({}, document.title, window.location.pathname)

  if (userPayload) {
    auth.setAuth({ user: userPayload, token })
    await navigateAfterSso(resolvePostAuthTarget())
    return
  }

  try {
    const me = await AuthService.me()
    auth.setAuth({ user: me, token })
    await navigateAfterSso(resolvePostAuthTarget())
  } catch (e) {
    console.error('Erreur /auth/me après SSO', e)
    auth.logout()
    router.replace({ name: 'auth', query: { mode: 'login' } })
    return
  }
})
</script>

<template>
  <div class="auth-callback-screen">
    <div
      class="auth-callback-card w-full max-w-md space-y-4 rounded-3xl border border-slate-700/50 bg-slate-900/78 p-6 text-center shadow-[0_16px_44px_rgba(2,6,23,0.42)] backdrop-blur sm:p-8"
    >
      <div class="mx-auto h-12 w-12 rounded-full border-[3px] border-slate-700/90 border-t-emerald-300 animate-spin" />
      <div class="space-y-2">
        <p class="text-xs uppercase tracking-[0.24em] text-emerald-200/80">Connexion</p>
        <p class="text-xl font-semibold text-white">Redirection en cours</p>
        <p class="text-sm text-slate-400 leading-relaxed">
          Nous securisons ta session et preparons ton espace. Merci de patienter quelques secondes.
        </p>
      </div>
    </div>
  </div>
</template>

<style scoped>
.auth-callback-screen {
  position: fixed;
  inset: 0;
  z-index: 120;
  display: grid;
  min-height: 100dvh;
  width: 100%;
  place-items: center;
  overflow: hidden;
  background: #020617;
  color: #f8fafc;
  overscroll-behavior: none;
  padding: max(16px, env(safe-area-inset-top)) max(16px, env(safe-area-inset-right))
    max(16px, env(safe-area-inset-bottom)) max(16px, env(safe-area-inset-left));
}

.auth-callback-card {
  margin: auto;
}
</style>
