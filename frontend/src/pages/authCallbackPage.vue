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
  <div class="min-h-screen bg-slate-950 text-slate-50 flex items-center justify-center px-4">
    <div
      class="w-full max-w-md rounded-3xl border border-slate-800/80 bg-slate-900/70 shadow-[0_25px_80px_rgba(0,0,0,0.45)] backdrop-blur p-8 space-y-4 text-center"
    >
      <div class="mx-auto h-14 w-14 rounded-full border-4 border-slate-700 border-t-emerald-400 animate-spin" />
      <div class="space-y-2">
        <p class="text-sm uppercase tracking-[0.24em] text-emerald-200/80">Connexion</p>
        <p class="text-xl font-semibold text-white">Redirection en cours</p>
        <p class="text-sm text-slate-400">
          Nous sécurisons ta session et préparons ton espace. Merci de patienter quelques secondes.
        </p>
      </div>
    </div>
  </div>
</template>
