<template>
  <div class="min-h-screen overflow-hidden bg-slate-950 px-4 py-4 sm:py-6 flex items-center">
    <div class="mx-auto w-full max-w-5xl">
      <div class="grid items-center gap-6 md:grid-cols-[0.9fr_1.1fr]">
        <div class="text-slate-200">
          <p class="text-xs uppercase tracking-[0.3em] text-violet-300/80">Compte</p>
          <h1 class="mt-2 text-2xl font-semibold text-white sm:text-3xl">
            {{ mode === 'login' ? 'Connexion' : 'Inscription' }}
          </h1>
          <p class="mt-2 text-sm text-slate-400">
            {{
              mode === 'login'
                ? 'Accède à ton espace Stash.'
                : 'Crée ton compte en quelques secondes.'
            }}
          </p>
          <div
            class="mt-4 hidden md:inline-flex items-center rounded-2xl border border-violet-400/30 bg-violet-400/10 px-4 py-2 text-xs text-violet-200"
          >
            {{ mode === 'login' ? 'Connexion sécurisée' : 'Création rapide' }}
          </div>
        </div>

        <div
          class="w-full rounded-3xl border border-slate-800/80 bg-gradient-to-b from-slate-900/95 via-slate-900/80 to-slate-950/70 p-5 shadow-2xl backdrop-blur sm:p-6"
        >
          <div class="mb-3 flex items-center justify-center">
            <div
              class="inline-flex items-center rounded-full bg-slate-950/70 p-1 border border-slate-800"
            >
              <button
                class="px-4 py-1.5 text-sm font-medium rounded-full transition"
                :class="
                  mode === 'login'
                    ? 'bg-violet-500 text-slate-950 shadow'
                    : 'text-slate-300 hover:text-white'
                "
                @click="setMode('login')"
              >
                Connexion
              </button>
              <button
                class="px-4 py-1.5 text-sm font-medium rounded-full transition"
                :class="
                  mode === 'signup'
                    ? 'bg-violet-500 text-slate-950 shadow'
                    : 'text-slate-300 hover:text-white'
                "
                @click="setMode('signup')"
              >
                Inscription
              </button>
            </div>
          </div>

          <div
            v-if="error"
            class="mb-4 rounded-xl border border-red-500/70 bg-red-500/10 p-3 text-sm text-red-200"
          >
            {{ error }}
          </div>
          <div
            v-if="success"
            class="mb-4 rounded-xl border border-emerald-500/60 bg-emerald-500/10 p-3 text-sm text-emerald-100"
          >
            {{ success }}
          </div>

          <form v-if="mode === 'login'" class="space-y-3" @submit.prevent="submitLogin">
            <div>
              <label for="loginEmail" class="block text-sm font-medium text-slate-200">Email</label>
              <input
                type="email"
                id="loginEmail"
                v-model="loginForm.email"
                required
                class="mt-2 block w-full rounded-xl border border-slate-700 bg-slate-900/70 px-4 py-2 text-sm text-slate-100 shadow-sm transition focus:outline-none focus:ring-2 focus:ring-violet-400 focus:border-violet-400"
              />
            </div>

            <div>
              <label for="loginPassword" class="block text-sm font-medium text-slate-200"
                >Mot de passe</label
              >

              <div class="relative mt-2">
                <input
                  :type="showLoginPassword ? 'text' : 'password'"
                  id="loginPassword"
                  v-model="loginForm.password"
                  required
                  class="block w-full pr-12 rounded-xl border border-slate-700 bg-slate-900/70 px-4 py-2 text-sm text-slate-100 shadow-sm transition focus:outline-none focus:ring-2 focus:ring-violet-400 focus:border-violet-400"
                />

                <button
                  type="button"
                  class="absolute inset-y-0 right-3 flex items-center text-slate-400 transition hover:text-slate-200"
                  @click="showLoginPassword = !showLoginPassword"
                  :aria-label="
                    showLoginPassword ? 'Masquer le mot de passe' : 'Afficher le mot de passe'
                  "
                >
                  <span class="text-xs font-medium">
                    {{ showLoginPassword ? 'Masquer' : 'Afficher' }}
                  </span>
                </button>
              </div>
              <div class="mt-2 flex justify-end">
                <router-link
                  class="text-xs text-slate-400 transition hover:text-slate-200"
                  :to="{ name: 'forgot-password' }"
                >
                  Mot de passe oublié ?
                </router-link>
              </div>
              <div class="mt-2 grid grid-cols-2 gap-2">
                <button
                  type="button"
                  @click="loginWithGoogle"
                  class="inline-flex items-center justify-center gap-2 rounded-lg border border-slate-700 bg-white/90 px-3 py-2 text-xs font-semibold text-slate-900 shadow-sm transition hover:bg-white focus:outline-none focus:ring-2 focus:ring-violet-400"
                >
                  <svg
                    class="h-4 w-4"
                    viewBox="0 0 48 48"
                    fill="none"
                    xmlns="http://www.w3.org/2000/svg"
                    aria-hidden="true"
                  >
                    <path
                      d="M43.6 24.5c0-1.5-.1-3-.4-4.4H24v8.4h11.1c-.5 2.7-2 5-4.3 6.5v5.4h6.9c4.1-3.7 5.9-9.2 5.9-15.9Z"
                      fill="#4285F4"
                    />
                    <path
                      d="M24 44c5.9 0 10.8-1.9 14.4-5.1l-6.9-5.4c-1.9 1.3-4.4 2.1-7.5 2.1-5.7 0-10.4-3.8-12.1-9H4.8v5.6C8.3 39.7 15.5 44 24 44Z"
                      fill="#34A853"
                    />
                    <path
                      d="M11.9 26.6c-.4-1.3-.7-2.6-.7-4 0-1.4.3-2.7.7-4V13H4.8C3.3 16 2.5 19.4 2.5 23s.8 7 2.3 10l7.1-5.4Z"
                      fill="#FBBC05"
                    />
                    <path
                      d="M24 9.9c3.2 0 6.1 1.1 8.3 3.2l6.2-6.2C34.7 3.4 29.8 1.5 24 1.5 15.5 1.5 8.3 5.8 4.8 13l7.1 5.6C13.6 13.7 18.3 9.9 24 9.9Z"
                      fill="#EA4335"
                    />
                    <path d="M2.5 2.5h43v43h-43v-43Z" fill="none" />
                  </svg>
                  <span>Google</span>
                </button>
                <button
                  type="button"
                  @click="loginWithDiscord"
                  class="inline-flex items-center justify-center gap-2 rounded-lg bg-[#5865F2] px-3 py-2 text-xs font-semibold text-white shadow-sm transition hover:bg-[#4752C4] focus:outline-none focus:ring-2 focus:ring-[#5865F2]/60"
                >
                  <svg
                    class="h-4 w-4"
                    viewBox="0 0 24 24"
                    fill="currentColor"
                    aria-hidden="true"
                    xmlns="http://www.w3.org/2000/svg"
                  >
                    <path
                      d="M20 0H4C1.8 0 0 1.8 0 4v16c0 2.2 1.8 4 4 4h14l-.7-2.3 1.7 1.6L21 24l3-3V4c0-2.2-1.8-4-4-4Zm-3.9 16.3s-.4-.5-.7-.9c1.3-.4 1.8-1.3 1.8-1.3-.4.3-.8.5-1.2.6-.5.2-1 .3-1.4.4-.9.2-1.7.1-2.5 0-.6-.1-1.1-.3-1.5-.4-.2-.1-.5-.2-.7-.3-.1-.1-.2-.1-.3-.2-.1-.1-.1-.1-.2-.1 0 0-.1-.1-.1-.1 0 0-.1 0 0 0-.2-.1-.3-.2-.3-.2-.2-.1-.3-.2-.3-.2-.3-.2-.5-.4-.5-.4 0 0 .5.9 1.7 1.3-.3.4-.7.9-.7.9-2.3-.1-3.1-1.6-3.1-1.6 0-3.4 1.5-6.1 1.5-6.1 1.5-1.2 3-1.1 3-1.1l.1.1c-1.9.6-2.8 1.5-2.8 1.5s.2-.1.6-.3c1.1-.4 2-.5 2.4-.6.1 0 .2 0 .3-.1.6-.1 1.3-.1 2-.1.9 0 1.8.1 2.8.4.5.2 1.1.4 1.7.8 0 0-.8-.7-2.5-1.4l.2-.2s1.6-.1 3 1.1c0 0 1.5 2.7 1.5 6.1 0 0-.9 1.5-3.2 1.6Z"
                    />
                    <path
                      d="M9.5 11.1c-.6 0-1.1.5-1.1 1.1 0 .6.5 1.1 1.1 1.1.6 0 1.1-.5 1.1-1.1.1-.6-.4-1.1-1.1-1.1Zm4.9 0c-.6 0-1.1.5-1.1 1.1 0 .6.5 1.1 1.1 1.1.6 0 1.1-.5 1.1-1.1 0-.6-.5-1.1-1.1-1.1Z"
                    />
                  </svg>
                  <span>Discord</span>
                </button>
              </div>
            </div>

            <button
              type="submit"
              :disabled="loading"
              class="w-full inline-flex justify-center items-center gap-2 rounded-xl bg-violet-500 px-4 py-2 text-sm font-semibold text-slate-950 transition hover:bg-violet-400 focus:outline-none focus:ring-2 focus:ring-violet-400 disabled:opacity-60 disabled:cursor-not-allowed"
            >
              {{ loading ? 'Connexion...' : 'Se connecter' }}
            </button>
          </form>

          <form v-else class="space-y-3" @submit.prevent="submitSignup">
            <div class="grid grid-cols-1 gap-3 sm:grid-cols-2">
              <div>
                <label for="firstName" class="block text-sm font-medium text-slate-200"
                  >Prenom</label
                >
                <input
                  type="text"
                  id="firstName"
                  v-model="signupForm.firstName"
                  required
                  class="mt-2 block w-full rounded-xl border border-slate-700 bg-slate-900/70 px-4 py-2 text-sm text-slate-100 shadow-sm transition focus:outline-none focus:ring-2 focus:ring-violet-400 focus:border-violet-400"
                />
              </div>
              <div>
                <label for="lastName" class="block text-sm font-medium text-slate-200">Nom</label>
                <input
                  type="text"
                  id="lastName"
                  v-model="signupForm.lastName"
                  required
                  class="mt-2 block w-full rounded-xl border border-slate-700 bg-slate-900/70 px-4 py-2 text-sm text-slate-100 shadow-sm transition focus:outline-none focus:ring-2 focus:ring-violet-400 focus:border-violet-400"
                />
              </div>
            </div>

            <div>
              <label for="signupEmail" class="block text-sm font-medium text-slate-200"
                >Email</label
              >
              <input
                type="email"
                id="signupEmail"
                v-model="signupForm.email"
                required
                class="mt-2 block w-full rounded-xl border border-slate-700 bg-slate-900/70 px-4 py-2 text-sm text-slate-100 shadow-sm transition focus:outline-none focus:ring-2 focus:ring-violet-400 focus:border-violet-400"
              />
            </div>

            <div>
              <label for="signupPassword" class="block text-sm font-medium text-slate-200"
                >Mot de passe</label
              >

              <div class="relative mt-2">
                <input
                  :type="showSignupPassword ? 'text' : 'password'"
                  id="signupPassword"
                  v-model="signupForm.password"
                  required
                  class="block w-full pr-12 rounded-xl border border-slate-700 bg-slate-900/70 px-4 py-2 text-sm text-slate-100 shadow-sm transition focus:outline-none focus:ring-2 focus:ring-violet-400 focus:border-violet-400"
                />

                <button
                  type="button"
                  class="absolute inset-y-0 right-3 flex items-center text-slate-400 transition hover:text-slate-200"
                  @click="showSignupPassword = !showSignupPassword"
                  :aria-label="
                    showSignupPassword ? 'Masquer le mot de passe' : 'Afficher le mot de passe'
                  "
                >
                  <span class="text-xs font-medium">
                    {{ showSignupPassword ? 'Masquer' : 'Afficher' }}
                  </span>
                </button>
              </div>
              <div class="mt-2">
                <div class="flex items-center justify-between text-xs text-slate-400">
                  <span>Force du mot de passe</span>
                  <span :class="signupStrengthLabelColor">{{ signupStrengthLabel }}</span>
                </div>
                <div class="mt-2 h-1.5 w-full rounded-full bg-slate-800">
                  <div
                    class="h-1.5 rounded-full transition-all"
                    :class="signupStrengthBarColor"
                    :style="{ width: signupStrengthPercent + '%' }"
                  ></div>
                </div>
              </div>
            </div>

            <div>
              <label for="confirmPassword" class="block text-sm font-medium text-slate-200">
                Confirmer le mot de passe
              </label>

              <div class="relative mt-2">
                <input
                  :type="showConfirmPassword ? 'text' : 'password'"
                  id="confirmPassword"
                  v-model="signupForm.confirmPassword"
                  required
                  class="block w-full pr-12 rounded-xl border border-slate-700 bg-slate-900/70 px-4 py-2 text-sm text-slate-100 shadow-sm transition focus:outline-none focus:ring-2 focus:ring-violet-400 focus:border-violet-400"
                />

                <button
                  type="button"
                  class="absolute inset-y-0 right-3 flex items-center text-slate-400 transition hover:text-slate-200"
                  @click="showConfirmPassword = !showConfirmPassword"
                  :aria-label="
                    showConfirmPassword ? 'Masquer le mot de passe' : 'Afficher le mot de passe'
                  "
                >
                  <span class="text-xs font-medium">
                    {{ showConfirmPassword ? 'Masquer' : 'Afficher' }}
                  </span>
                </button>
              </div>
            </div>

            <button
              type="submit"
              :disabled="loading"
              class="w-full inline-flex justify-center items-center gap-2 rounded-xl bg-violet-500 px-4 py-2 text-sm font-semibold text-slate-950 transition hover:bg-violet-400 focus:outline-none focus:ring-2 focus:ring-violet-400 disabled:opacity-60 disabled:cursor-not-allowed"
            >
              {{ loading ? 'Création du compte...' : 'Créer mon compte' }}
            </button>
          </form>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AuthService from '@/services/AuthService'
import { useAuthStore } from '@/store/authStore'

const showLoginPassword = ref(false)
const showSignupPassword = ref(false)
const showConfirmPassword = ref(false)

const route = useRoute()
const router = useRouter()

const { setAuth } = useAuthStore()

const mode = ref(route.query.mode === 'signup' ? 'signup' : 'login')

const setMode = (m) => {
  mode.value = m
  router.replace({ name: 'auth', query: { mode: m } })
}

watch(
  () => route.query.mode,
  (m) => {
    if (m === 'signup' || m === 'login') {
      mode.value = m
    }
  },
)

const loading = ref(false)
const error = ref('')
const success = ref('')

const loginForm = ref({
  email: '',
  password: '',
  remember: false,
})

const signupForm = ref({
  firstName: '',
  lastName: '',
  email: '',
  password: '',
  confirmPassword: '',
  acceptTerms: false,
})

const signupStrengthScore = computed(() => {
  const value = signupForm.value.password
  if (!value) return 0

  let score = 0
  if (value.length >= 8) score += 1
  if (value.length >= 12) score += 1
  if (/[A-Z]/.test(value)) score += 1
  if (/[0-9]/.test(value)) score += 1
  if (/[^A-Za-z0-9]/.test(value)) score += 1
  return Math.min(score, 5)
})

const signupStrengthPercent = computed(() => (signupStrengthScore.value / 5) * 100)

const signupStrengthLabel = computed(() => {
  if (signupStrengthScore.value <= 1) return 'Faible'
  if (signupStrengthScore.value <= 3) return 'Moyen'
  if (signupStrengthScore.value <= 4) return 'Bon'
  return 'Fort'
})

const signupStrengthLabelColor = computed(() => {
  if (signupStrengthScore.value <= 1) return 'text-red-300'
  if (signupStrengthScore.value <= 3) return 'text-amber-300'
  if (signupStrengthScore.value <= 4) return 'text-violet-300'
  return 'text-emerald-300'
})

const signupStrengthBarColor = computed(() => {
  if (signupStrengthScore.value <= 1) return 'bg-red-500/80'
  if (signupStrengthScore.value <= 3) return 'bg-amber-400/80'
  if (signupStrengthScore.value <= 4) return 'bg-violet-400/80'
  return 'bg-emerald-400/80'
})

const resetMessages = () => {
  error.value = ''
  success.value = ''
}

// Gestion des erreurs SSO (callback /auth/callback#error=...)
const ssoErrorMessages = {
  discord_not_allowed: 'Accès réservé aux membres du Discord autorisé.',
  oauth_error: 'La connexion OAuth a échoué. Merci de réessayer.',
}

const applySsoErrorFromRoute = () => {
  const code = route.query.ssoError
  if (code && ssoErrorMessages[code]) {
    error.value = ssoErrorMessages[code]
  }
}

applySsoErrorFromRoute()

watch(
  () => route.query.ssoError,
  () => applySsoErrorFromRoute(),
)

const submitLogin = async () => {
  resetMessages()
  loading.value = true

  try {
    const { user, token } = await AuthService.login({
      email: loginForm.value.email,
      password: loginForm.value.password,
    })

    setAuth({ user, token })

    success.value = 'Connexion reussie'
    await router.replace({ name: 'home' })
  } catch (err) {
    console.error(err)
    error.value = err.response?.data?.message || err.message || 'Email ou mot de passe invalide.'
  } finally {
    loading.value = false
  }
}

const submitSignup = async () => {
  resetMessages()

  if (signupForm.value.password !== signupForm.value.confirmPassword) {
    error.value = 'Les mots de passe ne correspondent pas.'
    return
  }

  loading.value = true
  try {
    await AuthService.register({
      email: signupForm.value.email,
      firstName: signupForm.value.firstName,
      lastName: signupForm.value.lastName,
      password: signupForm.value.password,
    })

    success.value = 'Compte cree, verification email envoyee.'
    router.replace({ name: 'verify-email', query: { email: signupForm.value.email } })
  } catch (err) {
    console.error(err)
    error.value = err.response?.data?.message || 'Erreur lors de la creation du compte.'
  } finally {
    loading.value = false
  }
}

const apiBase =
  (import.meta.env.VITE_API_URL || import.meta.env.VITE_API_BASE_URL || '').replace(/\/+$/, '') ||
  window.location.origin

const googleAuthUrl =
  import.meta.env.VITE_GOOGLE_OAUTH_URL || `${apiBase}/oauth2/authorization/google`

const loginWithGoogle = () => {
  window.location.href = googleAuthUrl
}

const discordAuthUrl =
  import.meta.env.VITE_DISCORD_OAUTH_URL || `${apiBase}/oauth2/authorization/discord`

const loginWithDiscord = () => {
  window.location.href = discordAuthUrl
}
</script>
