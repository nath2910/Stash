<script setup>
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/store/authStore.js'
import AuthService from '@/services/AuthService.js'

const router = useRouter()
const auth = useAuthStore()

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
    router.replace({ name: 'home' })
    return
  }

  try {
    const me = await AuthService.me()
    auth.setAuth({ user: me, token })
  } catch (e) {
    console.error('Erreur /auth/me apres SSO', e)
    auth.logout()
    router.replace({ name: 'auth', query: { mode: 'login' } })
    return
  }

  router.replace({ name: 'home' })
})
</script>

<template>
  <div class="auth-callback-screen flex min-h-dvh w-full items-center justify-center overflow-hidden bg-slate-950 px-4 text-slate-50">
    <div
      class="w-full max-w-md space-y-4 rounded-3xl border border-slate-700/50 bg-slate-900/78 p-6 text-center shadow-[0_16px_44px_rgba(2,6,23,0.42)] backdrop-blur sm:p-8"
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
  overscroll-behavior: none;
}
</style>
