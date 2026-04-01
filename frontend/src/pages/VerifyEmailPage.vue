<template>
  <div class="relative min-h-screen bg-slate-950 px-4 py-10 sm:py-14">
    <div class="mx-auto w-full max-w-2xl">
      <div
        class="rounded-2xl border border-slate-800 bg-gradient-to-b from-slate-900/90 to-slate-900/60 p-6 shadow-2xl sm:rounded-3xl sm:p-8"
      >
        <div class="flex items-center gap-4">
          <div
            class="relative flex h-14 w-14 items-center justify-center rounded-2xl bg-emerald-500/15 text-emerald-300"
          >
            <span v-if="status === 'success'" class="text-2xl">✓</span>
            <span v-else-if="status === 'error'" class="text-2xl">!</span>
            <div v-else class="spinner-ring"></div>
          </div>
          <div class="min-w-0">
            <p class="text-xs uppercase tracking-[0.3em] text-slate-400">Verification email</p>
            <h1 class="text-xl font-semibold text-slate-100 sm:text-2xl">
              {{ title }}
            </h1>
          </div>
        </div>

        <p class="mt-6 text-sm leading-relaxed text-slate-300 sm:text-[0.95rem]">
          {{ description }}
        </p>

        <div v-if="status === 'pending'" class="mt-6 grid gap-3 text-sm text-slate-300 sm:grid-cols-2">
          <div class="rounded-2xl border border-slate-800 bg-slate-900/60 p-4">
            <p class="text-xs uppercase tracking-[0.2em] text-slate-400">Etape 1</p>
            <p class="mt-2 font-medium text-slate-200">Ouvre ta boite mail</p>
            <p class="mt-1 text-xs text-slate-400">Email envoye a {{ emailLabel }}</p>
          </div>
          <div class="rounded-2xl border border-slate-800 bg-slate-900/60 p-4">
            <p class="text-xs uppercase tracking-[0.2em] text-slate-400">Etape 2</p>
            <p class="mt-2 font-medium text-slate-200">Clique sur le lien de validation</p>
            <p class="mt-1 text-xs text-slate-400">Pense a vérifiér les spams.</p>
          </div>
        </div>

        <div v-if="status === 'error'" class="mt-6 rounded-2xl border border-red-500/50 bg-red-500/10 p-4">
          <p class="text-sm text-red-200">{{ errorMessage }}</p>
        </div>

        <div v-if="resendMessage" class="mt-6 rounded-2xl border border-emerald-500/40 bg-emerald-500/10 p-3">
          <p class="text-sm text-emerald-200">{{ resendMessage }}</p>
        </div>

        <div class="mt-8 flex flex-wrap gap-3">
          <button
            v-if="(status === 'pending' || status === 'error') && email"
            type="button"
            :disabled="resendLoading"
            class="inline-flex w-full items-center justify-center rounded-xl border border-slate-700 px-4 py-2 text-sm font-semibold text-slate-200 transition hover:border-slate-500 hover:text-white disabled:opacity-60 sm:w-auto"
            @click="resendEmail"
          >
            {{ resendLoading ? 'Envoi...' : 'Renvoyer le mail' }}
          </button>
          <router-link
            v-if="status === 'success'"
            class="inline-flex w-full items-center justify-center rounded-xl bg-emerald-500 px-4 py-2 text-sm font-semibold text-slate-950 transition hover:bg-emerald-400 sm:w-auto"
            :to="{ name: 'auth', query: { mode: 'login' } }"
          >
            Aller a la connexion
          </router-link>
          <router-link
            v-else
            class="inline-flex w-full items-center justify-center rounded-xl border border-slate-700 px-4 py-2 text-sm font-semibold text-slate-200 transition hover:border-slate-500 hover:text-white sm:w-auto"
            :to="{ name: 'auth', query: { mode: 'login' } }"
          >
            Retour
          </router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, onBeforeUnmount, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AuthService from '@/services/AuthService'
import { useAuthStore } from '@/store/authStore'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const status = ref('pending')
const errorMessage = ref('')
const resendLoading = ref(false)
const resendMessage = ref('')

const token = computed(() => (route.query.token || route.params.token || '').toString())
// L'email est optionnel ici; il sert seulement au bouton de renvoi.
const email = computed(() => (route.query.email || '').toString())

const emailLabel = computed(() => (email.value ? email.value : 'ton adresse'))

const title = computed(() => {
  if (status.value === 'success') return 'Email confirme'
  if (status.value === 'error') return 'Lien invalide'
  if (token.value) return 'Verification en cours'
  return 'En attente de confirmation'
})

const description = computed(() => {
  if (status.value === 'success') {
    return "Top. Ton compte est pret, tu peux te connecter tout de suite."
  }
  if (status.value === 'error') {
    return "Ce lien n'est plus valide. Recommence l'inscription ou demande un nouvel email."
  }
  if (token.value) {
    return "On vérifié ton lien de confirmation. Cela prend quelques secondes."
  }
  return "On t'a envoye un email avec un lien de confirmation. Clique dessus pour activer ton compte."
})

const verify = async () => {
  if (!token.value) {
    status.value = 'pending'
    return
  }

  status.value = 'loading'
  errorMessage.value = ''

  try {
    const payload = await AuthService.verifyEmail({ token: token.value })
    status.value = 'success'
    if (payload?.token) {
      auth.setAuth({ user: payload.user ?? null, token: payload.token })
      await router.replace({ name: 'home' })
    }
  } catch (err) {
    console.error(err)
    status.value = 'error'
    errorMessage.value =
      err.response?.data?.message || err.message || "Impossible de vérifiér l'email."
  }
}

const resendEmail = async () => {
  if (!email.value) {
    return
  }
  resendLoading.value = true
  resendMessage.value = ''
  try {
    await AuthService.resendVerification({ email: email.value })
    resendMessage.value = 'Email de verification renvoye.'
  } catch (err) {
    console.error(err)
    resendMessage.value = "Impossible d'envoyer l'email."
  } finally {
    resendLoading.value = false
  }
}

const handleLoginFromStorage = async () => {
  const storedToken = localStorage.getItem('snk_token')
  if (!storedToken) return

  let storedUser = null
  try {
    storedUser = JSON.parse(localStorage.getItem('snk_user') || 'null')
  } catch {
    storedUser = null
  }

  auth.setAuth({ user: storedUser, token: storedToken })
  try {
    const me = await AuthService.me()
    auth.setUser(me)
  } catch (err) {
    console.warn('Unable to refresh user after storage event', err)
  }

  await router.replace({ name: 'home' })
}

const onStorage = (event) => {
  if (event.key === 'snk_token' && event.newValue) {
    handleLoginFromStorage()
  }
}

onMounted(() => {
  window.addEventListener('storage', onStorage)
  verify()
  // Si l'autre onglet a deja mis le token avant l'event storage
  handleLoginFromStorage()
})

onBeforeUnmount(() => {
  window.removeEventListener('storage', onStorage)
})

watch(token, verify)
</script>

<style scoped>
.spinner-ring {
  width: 28px;
  height: 28px;
  border-radius: 9999px;
  border: 3px solid rgba(226, 232, 240, 0.2);
  border-top-color: rgba(52, 211, 153, 0.9);
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
