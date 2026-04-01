<template>
  <div class="min-h-screen bg-slate-950 px-4 py-6 sm:py-10">
    <div class="mx-auto w-full max-w-6xl">
      <div class="mb-6 flex items-center justify-between">
        <button
          type="button"
          @click="goBack"
          class="inline-flex items-center gap-2 rounded-xl border border-slate-800 bg-slate-900/70 px-3 py-2 text-xs font-medium text-slate-200 transition hover:border-violet-400/50 hover:text-white"
        >
          <span class="text-sm"><-</span>
          <span>Retour</span>
        </button>
      </div>

      <div class="grid gap-8 lg:grid-cols-[0.9fr_1.1fr]">
        <section
          class="rounded-3xl border border-slate-800/80 bg-gradient-to-b from-slate-900/95 via-slate-900/80 to-slate-950/70 p-6 shadow-2xl backdrop-blur sm:p-7"
        >
          <div class="flex flex-wrap items-center gap-4">
            <div
              class="flex h-14 w-14 items-center justify-center rounded-2xl border border-violet-400/30 bg-violet-400/10 text-lg font-semibold text-violet-200"
            >
              {{ initials }}
            </div>
            <div>
              <h1 class="text-xl font-semibold text-white sm:text-2xl">Mon compte</h1>
              <p class="mt-1 text-sm text-slate-400">{{ currentUser.email || '-' }}</p>
            </div>
            <div class="w-full sm:ml-auto sm:w-auto">
              <button
                type="button"
                class="w-full rounded-xl border border-emerald-300/40 bg-emerald-300/10 px-3 py-2 text-xs font-semibold text-emerald-100 hover:bg-emerald-300/15 sm:w-auto"
                @click="goAbo"
              >
                Gérer l'abonnement
              </button>
            </div>
          </div>

          <div class="mt-6 grid gap-3 text-sm text-slate-200 sm:grid-cols-2">
            <div>
              <p class="text-xs uppercase tracking-[0.2em] text-slate-500">Prenom</p>
              <p class="mt-1 font-medium">{{ currentUser.firstName || '-' }}</p>
            </div>
            <div>
              <p class="text-xs uppercase tracking-[0.2em] text-slate-500">Nom</p>
              <p class="mt-1 font-medium">{{ currentUser.lastName || '-' }}</p>
            </div>
          </div>

          <div class="mt-6 flex flex-wrap gap-2">
            <span
              class="inline-flex items-center rounded-full border border-slate-700 bg-slate-900/70 px-3 py-1 text-xs text-slate-300"
            >
              Email:
              <span class="ml-1 font-semibold">
                {{ currentUser.emailVerified ? 'verifie' : 'non verifie' }}
              </span>
            </span>
            <span
              class="inline-flex items-center rounded-full border border-violet-400/30 bg-violet-400/10 px-3 py-1 text-xs text-violet-200"
            >
              Compte protege
            </span>
          </div>

          <div class="mt-6 border-t border-slate-800/80 pt-6">
            <h3 class="text-base font-semibold text-red-200">Zone dangereuse</h3>
            <p class="mt-2 text-sm text-slate-400">
              Cette action supprime definitivement ton compte et tes donnees.
            </p>

            <div class="mt-4 space-y-3 text-sm text-slate-200">
              <label class="flex items-center gap-2">
                <input v-model="deleteConfirmChecked" type="checkbox" class="h-4 w-4" />
                <span>Je comprends que cette action est irreversible.</span>
              </label>

              <div>
                <label class="block text-xs uppercase tracking-[0.2em] text-slate-500">
                  Tape SUPPRIMER pour confirmer
                </label>
                <input
                  v-model="deleteConfirmText"
                  type="text"
                  placeholder="SUPPRIMER"
                  class="mt-2 block w-full rounded-xl border border-red-500/40 bg-slate-900/70 px-4 py-2.5 text-sm text-slate-100 shadow-sm transition focus:outline-none focus:ring-2 focus:ring-red-400 focus:border-red-400"
                />
              </div>
            </div>

            <div
              v-if="deleteError"
              class="mt-3 rounded-xl border border-red-500/70 bg-red-500/10 p-3 text-sm text-red-200"
            >
              {{ deleteError }}
            </div>

            <button
              type="button"
              :disabled="deleting || !canDelete"
              @click="submitDelete"
              class="mt-4 inline-flex items-center justify-center gap-2 rounded-xl bg-red-500 px-4 py-2.5 text-sm font-semibold text-slate-950 transition hover:bg-red-400 focus:outline-none focus:ring-2 focus:ring-red-400 disabled:cursor-not-allowed disabled:opacity-50"
            >
              {{ deleting ? 'Suppression...' : 'Supprimer mon compte' }}
            </button>
          </div>
        </section>

        <section
          class="rounded-3xl border border-slate-800/80 bg-gradient-to-b from-slate-900/95 via-slate-900/80 to-slate-950/70 p-6 shadow-2xl backdrop-blur sm:p-7"
        >
          <h2 class="text-lg font-semibold text-white">Securite</h2>
          <p class="mt-1 text-sm text-slate-400">Modifie ton mot de passe.</p>

          <div
            v-if="error"
            class="mt-4 rounded-xl border border-red-500/70 bg-red-500/10 p-3 text-sm text-red-200"
          >
            {{ error }}
          </div>
          <div
            v-if="success"
            class="mt-4 rounded-xl border border-emerald-500/60 bg-emerald-500/10 p-3 text-sm text-emerald-100"
          >
            {{ success }}
          </div>

          <form class="mt-5 space-y-4" @submit.prevent="submitChangePassword">
            <div>
              <label class="block text-sm font-medium text-slate-200">Mot de passe actuel</label>
              <input
                v-model="form.currentPassword"
                type="password"
                required
                class="mt-2 block w-full rounded-xl border border-slate-700 bg-slate-900/70 px-4 py-2.5 text-sm text-slate-100 shadow-sm transition focus:outline-none focus:ring-2 focus:ring-violet-400 focus:border-violet-400"
              />
            </div>

            <div>
              <label class="block text-sm font-medium text-slate-200">Nouveau mot de passe</label>
              <input
                v-model="form.newPassword"
                type="password"
                required
                class="mt-2 block w-full rounded-xl border border-slate-700 bg-slate-900/70 px-4 py-2.5 text-sm text-slate-100 shadow-sm transition focus:outline-none focus:ring-2 focus:ring-violet-400 focus:border-violet-400"
              />
            </div>

            <div>
              <label class="block text-sm font-medium text-slate-200">
                Confirmer le nouveau mot de passe
              </label>
              <input
                v-model="form.confirmPassword"
                type="password"
                required
                class="mt-2 block w-full rounded-xl border border-slate-700 bg-slate-900/70 px-4 py-2.5 text-sm text-slate-100 shadow-sm transition focus:outline-none focus:ring-2 focus:ring-violet-400 focus:border-violet-400"
              />
            </div>

            <button
              type="submit"
              :disabled="loading"
              class="w-full inline-flex items-center justify-center gap-2 rounded-xl bg-violet-500 px-4 py-2.5 text-sm font-semibold text-slate-950 transition hover:bg-violet-400 focus:outline-none focus:ring-2 focus:ring-violet-400 disabled:opacity-60 disabled:cursor-not-allowed"
            >
              {{ loading ? 'Modification...' : 'Mettre a jour' }}
            </button>
          </form>
        </section>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/store/authStore'
import AuthService from '@/services/AuthService'

const router = useRouter()
const auth = useAuthStore()

const currentUser = computed(() => auth.user.value || {})

const initials = computed(() => {
  const first = currentUser.value.firstName?.[0] || ''
  const last = currentUser.value.lastName?.[0] || ''
  return (first + last || 'U').toUpperCase()
})

const providerLabel = computed(() => {
  const provider = currentUser.value.provider || 'LOCAL'
  return String(provider).toUpperCase()
})

const form = ref({
  currentPassword: '',
  newPassword: '',
  confirmPassword: '',
})

const loading = ref(false)
const error = ref('')
const success = ref('')

const deleteConfirmChecked = ref(false)
const deleteConfirmText = ref('')
const deleting = ref(false)
const deleteError = ref('')

const canDelete = computed(
  () => deleteConfirmChecked.value && deleteConfirmText.value.trim() === 'SUPPRIMER',
)

const resetMessages = () => {
  error.value = ''
  success.value = ''
}

const submitChangePassword = async () => {
  resetMessages()

  if (form.value.newPassword !== form.value.confirmPassword) {
    error.value = 'Les nouveaux mots de passe ne correspondent pas.'
    return
  }
  if (form.value.newPassword.length < 6) {
    error.value = 'Le nouveau mot de passe doit faire au moins 6 caracteres.'
    return
  }

  loading.value = true
  try {
    await AuthService.changePassword({
      currentPassword: form.value.currentPassword,
      newPassword: form.value.newPassword,
    })

    success.value = 'Mot de passe modifie avec succes.'
    form.value.currentPassword = ''
    form.value.newPassword = ''
    form.value.confirmPassword = ''
  } catch (err) {
    console.error(err)
    error.value = err.response?.data?.message || 'Erreur lors de la modification du mot de passe.'
  } finally {
    loading.value = false
  }
}

const submitDelete = async () => {
  deleteError.value = ''
  if (!canDelete.value) {
    deleteError.value = 'Confirmation invalide.'
    return
  }

  deleting.value = true
  try {
    await AuthService.deleteAccount()
    auth.logout()
    router.replace({ name: 'auth', query: { mode: 'login' } })
  } catch (err) {
    console.error(err)
    deleteError.value = err.response?.data?.message || 'Erreur lors de la suppression du compte.'
  } finally {
    deleting.value = false
  }
}

const goBack = () => {
  if (window.history.length > 1) {
    router.back()
  } else {
    router.push({ name: 'home' })
  }
}

const goAbo = () => {
  router.push({ name: 'abo' })
}
</script>
