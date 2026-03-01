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
                ? 'Accede a ton espace Stash.'
                : 'Cree ton compte en quelques secondes.'
            }}
          </p>
          <div
            class="mt-4 hidden md:inline-flex items-center rounded-2xl border border-violet-400/30 bg-violet-400/10 px-4 py-2 text-xs text-violet-200"
          >
            {{ mode === 'login' ? 'Connexion securisee' : 'Creation rapide' }}
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
                  Mot de passe oublie ?
                </router-link>
              </div>
              <div class="mt-2">
                <button
                  type="button"
                  @click="loginWithGoogle"
                  class="w-full inline-flex justify-center items-center gap-2 rounded-xl border border-slate-700 bg-slate-900/70 px-4 py-2 text-sm font-medium text-slate-100 transition hover:bg-slate-800/80 focus:outline-none focus:ring-2 focus:ring-violet-400"
                >
                  <span>Continuer avec Google</span>
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
              {{ loading ? 'Creation du compte...' : 'Creer mon compte' }}
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

const googleAuthUrl =
  import.meta.env.VITE_GOOGLE_OAUTH_URL ||
  'https://governing-irina-sneaknik-1b4023c1.koyeb.app/oauth2/authorization/google'

const loginWithGoogle = () => {
  window.location.href = googleAuthUrl
}
</script>
