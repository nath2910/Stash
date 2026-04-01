<template>
  <div class="min-h-screen overflow-x-hidden bg-slate-950 px-4 py-8 sm:py-12">
    <div class="mx-auto flex w-full max-w-xl flex-col items-center">
      <div class="mb-7 flex w-full items-start justify-between text-slate-300">
        <div>
          <p class="text-xs uppercase tracking-[0.25em] text-amber-300/80">Sécurité</p>
          <h1 class="mt-2 text-2xl font-semibold text-slate-100 sm:text-3xl">
            Nouveau mot de passe
          </h1>
          <p class="mt-2 text-sm text-slate-400">
            Choisis un nouveau mot de passe pour ton compte.
          </p>
        </div>
        <div class="hidden sm:flex h-12 w-12 items-center justify-center rounded-2xl border border-amber-400/30 bg-amber-400/10 text-amber-200">
          <span class="text-lg font-semibold">✓</span>
        </div>
      </div>

      <div
        class="w-full rounded-3xl border border-slate-800/80 bg-gradient-to-b from-slate-900/95 via-slate-900/80 to-slate-950/70 p-7 shadow-2xl backdrop-blur sm:p-9"
      >
      <div
        v-if="error"
        class="mb-4 rounded-xl border border-red-500/70 bg-red-500/10 p-3 text-sm text-red-200"
      >
        {{ error }}
      </div>
      <div
        v-if="success"
        class="mb-4 rounded-xl border border-amber-500/60 bg-amber-500/10 p-3 text-sm text-amber-100"
      >
        {{ success }}
      </div>

        <form class="mt-6 space-y-5" @submit.prevent="submitReset">
          <div>
            <label for="newPassword" class="block text-sm font-medium text-slate-200"
              >Nouveau mot de passe</label
            >
            <div class="relative mt-2">
              <input
                :type="showPassword ? 'text' : 'password'"
                id="newPassword"
                v-model="newPassword"
                required
                class="block w-full rounded-xl border border-slate-700 bg-slate-900/70 px-4 py-3 pr-12 text-sm text-slate-100 shadow-sm transition focus:outline-none focus:ring-2 focus:ring-amber-400 focus:border-amber-400"
              />
              <button
                type="button"
                class="absolute inset-y-0 right-3 flex items-center text-slate-400 transition hover:text-slate-200"
                @click="showPassword = !showPassword"
              >
                <span class="text-xs font-medium">
                  {{ showPassword ? 'Masquer' : 'Afficher' }}
                </span>
              </button>
            </div>
            <div class="mt-3">
              <div class="flex items-center justify-between text-xs text-slate-400">
                <span>Force du mot de passe</span>
                <span :class="strengthLabelColor">{{ strengthLabel }}</span>
              </div>
              <div class="mt-2 h-1.5 w-full rounded-full bg-slate-800">
                <div
                  class="h-1.5 rounded-full transition-all"
                  :class="strengthBarColor"
                  :style="{ width: strengthPercent + '%' }"
                ></div>
              </div>
            </div>
          </div>

          <div>
            <label for="confirmPassword" class="block text-sm font-medium text-slate-200"
              >Confirmer le mot de passe</label
            >
            <div class="relative mt-2">
              <input
                :type="showConfirmPassword ? 'text' : 'password'"
                id="confirmPassword"
                v-model="confirmPassword"
                required
                class="block w-full rounded-xl border border-slate-700 bg-slate-900/70 px-4 py-3 pr-12 text-sm text-slate-100 shadow-sm transition focus:outline-none focus:ring-2 focus:ring-amber-400 focus:border-amber-400"
              />
              <button
                type="button"
                class="absolute inset-y-0 right-3 flex items-center text-slate-400 transition hover:text-slate-200"
                @click="showConfirmPassword = !showConfirmPassword"
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
            class="w-full inline-flex justify-center items-center gap-2 rounded-xl bg-amber-400 px-4 py-3 text-sm font-semibold text-slate-950 transition hover:bg-amber-300 focus:outline-none focus:ring-2 focus:ring-amber-400 disabled:opacity-60 disabled:cursor-not-allowed"
          >
            {{ loading ? 'Modification...' : 'Modifier le mot de passe' }}
          </button>
        </form>

        <div class="mt-6 border-t border-slate-800/80 pt-4 text-center">
          <router-link class="text-xs text-slate-400 transition hover:text-slate-200" :to="{ name: 'auth' }">
            Retour a la connexion
          </router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AuthService from '@/services/AuthService'

const route = useRoute()
const router = useRouter()
const token = ref(route.query.token || '')

watch(
  () => route.query.token,
  (value) => {
    token.value = value || ''
  },
)

const newPassword = ref('')
const confirmPassword = ref('')
const showPassword = ref(false)
const showConfirmPassword = ref(false)
const loading = ref(false)
const error = ref('')
const success = ref('')

const strengthScore = computed(() => {
  const value = newPassword.value
  if (!value) return 0

  let score = 0
  if (value.length >= 8) score += 1
  if (value.length >= 12) score += 1
  if (/[A-Z]/.test(value)) score += 1
  if (/[0-9]/.test(value)) score += 1
  if (/[^A-Za-z0-9]/.test(value)) score += 1
  return Math.min(score, 5)
})

const strengthPercent = computed(() => (strengthScore.value / 5) * 100)

const strengthLabel = computed(() => {
  if (strengthScore.value <= 1) return 'Faible'
  if (strengthScore.value <= 3) return 'Moyen'
  if (strengthScore.value <= 4) return 'Bon'
  return 'Fort'
})

const strengthLabelColor = computed(() => {
  if (strengthScore.value <= 1) return 'text-red-300'
  if (strengthScore.value <= 3) return 'text-amber-300'
  if (strengthScore.value <= 4) return 'text-amber-200'
  return 'text-emerald-300'
})

const strengthBarColor = computed(() => {
  if (strengthScore.value <= 1) return 'bg-red-500/80'
  if (strengthScore.value <= 3) return 'bg-amber-400/80'
  if (strengthScore.value <= 4) return 'bg-amber-300/80'
  return 'bg-emerald-400/80'
})

const resetMessages = () => {
  error.value = ''
  success.value = ''
}

const submitReset = async () => {
  resetMessages()

  if (!token.value) {
    error.value = "Lien invalide ou manquant."
    return
  }

  if (newPassword.value !== confirmPassword.value) {
    error.value = 'Les mots de passe ne correspondent pas.'
    return
  }

  loading.value = true
  try {
    await AuthService.resetPassword({
      token: token.value,
      newPassword: newPassword.value,
    })
    success.value = 'Mot de passe modifie avec succes.'
    setTimeout(() => {
      router.push({ name: 'auth' })
    }, 2500)
  } catch (err) {
    console.error(err)
    error.value = err.response?.data?.message || 'Erreur lors de la modification.'
  } finally {
    loading.value = false
  }
}
</script>
