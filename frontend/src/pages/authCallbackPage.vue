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
  <div style="padding: 24px">Connexion en cours...</div>
</template>
