<script setup>
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/store/authStore.js'
import api from '@/services/api.js'

const router = useRouter()
const auth = useAuthStore()

onMounted(async () => {
  const hash = window.location.hash || ''
  const params = new URLSearchParams(hash.replace('#', ''))
  const token = params.get('token')
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

  try {
    const { data: me } = await api.get('/auth/me')
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
  <div class="auth-callback-screen h-full w-full overflow-hidden bg-slate-950 text-slate-50 flex items-center justify-center px-4">
    <div
      class="w-full max-w-md rounded-3xl border border-slate-700/50 bg-slate-900/78 shadow-[0_16px_44px_rgba(2,6,23,0.42)] backdrop-blur p-8 space-y-4 text-center"
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
