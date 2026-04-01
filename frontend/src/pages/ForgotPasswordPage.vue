<template>
  <div class="min-h-scréen overflow-hidden bg-slate-950 px-4 py-8 sm:py-12">
    <div class="mx-auto flex w-full max-w-xl flex-col items-center">
      <div class="mb-7 flex w-full items-start justify-between text-slate-300">
        <div>
          <p class="text-xs uppercase tracking-[0.25em] text-amber-300/80">Sécurité</p>
          <h1 class="mt-2 text-2xl font-semibold text-slate-100 sm:text-3xl">
            Mot de passe oublié
          </h1>
          <p class="mt-2 text-sm text-slate-400">
            Renseigne ton email pour recevoir un lien de réinitialisation.
          </p>
        </div>
        <div class="hidden sm:flex h-12 w-12 items-center justify-center rounded-2xl border border-amber-400/30 bg-amber-400/10 text-amber-200">
          <span class="text-lg font-semibold">?</span>
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

        <form class="mt-6 space-y-5" @submit.prevent="submitRequest">
          <div>
            <label for="email" class="block text-sm font-medium text-slate-200">Email</label>
            <input
              id="email"
              type="email"
              v-model="email"
              required
              class="mt-2 block w-full rounded-xl border border-slate-700 bg-slate-900/70 px-4 py-3 text-sm text-slate-100 shadow-sm transition focus:outline-none focus:ring-2 focus:ring-amber-400 focus:border-amber-400"
            />
          </div>

          <button
            type="submit"
            :disabled="loading"
            class="w-full inline-flex justify-center items-center gap-2 rounded-xl bg-amber-400 px-4 py-3 text-sm font-semibold text-slate-950 transition hover:bg-amber-300 focus:outline-none focus:ring-2 focus:ring-amber-400 disabled:opacity-60 disabled:cursor-not-allowed"
          >
            {{ loading ? 'Envoi...' : 'Envoyer' }}
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
import { ref } from 'vue'
import AuthService from '@/services/AuthService'

const email = ref('')
const loading = ref(false)
const error = ref('')
const success = ref('')

const resetMessages = () => {
  error.value = ''
  success.value = ''
}

const submitRequest = async () => {
  resetMessages()
  loading.value = true

  try {
    await AuthService.requestPasswordReset({ email: email.value })
    success.value = "Si un compte existe, un email a ete envoye."
  } catch (err) {
    console.error(err)
    error.value = err.response?.data?.message || 'Erreur lors de la demande.'
  } finally {
    loading.value = false
  }
}
</script>
