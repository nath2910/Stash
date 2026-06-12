<template>
  <div class="account-page-dark min-h-full overflow-x-hidden text-slate-100">
    <div class="app-shell app-page-stack">
      <div class="app-topbar">
        <button
          type="button"
          @click="goBack"
          class="app-touch-btn inline-flex items-center gap-2 rounded-xl border border-slate-800 bg-slate-900/70 px-3 py-2 text-xs font-medium text-slate-200 transition hover:border-violet-400/50 hover:text-white"
        >
          <span class="text-sm">&lt;-</span>
          <span>Retour</span>
        </button>
      </div>

      <div class="grid items-start gap-4 sm:gap-6 xl:grid-cols-[minmax(0,0.92fr)_minmax(0,1.08fr)] 2xl:gap-8">
        <section
          class="rounded-2xl border border-slate-800/80 bg-gradient-to-b from-slate-900/95 via-slate-900/80 to-slate-950/70 p-4 shadow-2xl backdrop-blur sm:rounded-3xl sm:p-6 lg:p-7"
        >
          <div class="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
            <div class="flex min-w-0 items-center gap-4">
              <div
                class="flex h-14 w-14 shrink-0 items-center justify-center rounded-2xl border border-violet-400/30 bg-violet-400/10 text-lg font-semibold text-violet-200"
              >
                {{ initials }}
              </div>
              <div class="min-w-0">
                <h1 class="text-xl font-semibold text-white sm:text-2xl">Mon compte</h1>
                <p class="mt-1 break-all text-sm text-slate-400 sm:break-normal sm:truncate">{{ currentUser.email || '-' }}</p>
              </div>
            </div>

            <button
              v-if="showSubscriptionButton"
              type="button"
              class="w-full rounded-xl border border-emerald-300/40 bg-emerald-300/10 px-3 py-2 text-xs font-semibold text-emerald-100 transition hover:bg-emerald-300/15 sm:w-auto"
              @click="goAbo"
            >
              Gerer l'abonnement
            </button>
          </div>

          <div class="mt-5 grid gap-3 text-sm text-slate-200 sm:mt-6 sm:grid-cols-2">
            <div class="rounded-xl border border-slate-800/80 bg-slate-900/55 px-4 py-3">
              <p class="text-xs uppercase tracking-[0.2em] text-slate-500">Prenom</p>
              <p class="mt-1 break-words font-medium">{{ currentUser.firstName || '-' }}</p>
            </div>
            <div class="rounded-xl border border-slate-800/80 bg-slate-900/55 px-4 py-3">
              <p class="text-xs uppercase tracking-[0.2em] text-slate-500">Nom</p>
              <p class="mt-1 break-words font-medium">{{ currentUser.lastName || '-' }}</p>
            </div>
          </div>

          <div class="account-profile-panel account-profile-panel--legal mt-6">
            <div class="account-profile-head">
              <div class="min-w-0">
                <p class="account-profile-eyebrow">Administratif</p>
                <h2>Profil légal</h2>
              </div>
              <button
                type="button"
                class="account-profile-change"
                :disabled="legalProfileLoading"
                @click="openLegalProfileEdit"
              >
                <RefreshCw
                  v-if="legalProfileLoading"
                  class="h-4 w-4 animate-spin"
                  aria-hidden="true"
                />
                <ChevronDown v-else class="h-4 w-4" aria-hidden="true" />
                <span>{{ legalProfileCompleted ? 'Modifier' : 'Compléter' }}</span>
              </button>
            </div>

            <div class="account-profile-current">
              <UserRoundCheck class="account-profile-icon" aria-hidden="true" />
              <div class="min-w-0">
                <div class="account-profile-title-row">
                  <strong>{{ legalProfileTitle }}</strong>
                  <span
                    class="account-profile-level"
                    :class="legalProfileCompleted ? 'low' : 'medium'"
                  >
                    {{ legalProfileCompleted ? 'Valide' : 'À compléter' }}
                  </span>
                </div>
                <p>{{ legalProfileDescription }}</p>
              </div>
            </div>

            <div v-if="legalProfileDetails.length" class="account-profile-traits">
              <span v-for="detail in legalProfileDetails" :key="detail">{{ detail }}</span>
            </div>

            <div v-if="legalProfileError" class="account-profile-message is-error">
              {{ legalProfileError }}
            </div>
            <div v-if="legalProfileSuccess" class="account-profile-message is-success">
              {{ legalProfileSuccess }}
            </div>

            <div v-if="legalProfileConfirmOpen" class="account-profile-confirm">
              <p>
                Changer de profil administratif peut nécessiter de ressaisir certaines informations
                obligatoires. Tu devras valider à nouveau tes informations avant d'accéder à la
                partie administrative.
              </p>
              <div class="account-profile-confirm-actions">
                <button type="button" class="account-profile-secondary" @click="cancelLegalProfileEdit">
                  Annuler
                </button>
                <button type="button" class="account-profile-primary" @click="confirmLegalProfileEdit">
                  Continuer
                </button>
              </div>
            </div>
          </div>

          <div class="mt-6 rounded-2xl border border-red-500/20 bg-red-500/5 p-4 sm:p-5">
            <p class="text-sm text-slate-400">
              Cette action supprime définitivement ton compte et tes données.
            </p>

            <div class="mt-4 space-y-3 text-sm text-slate-200">
              <label class="flex items-start gap-2">
                <input v-model="deleteConfirmChecked" type="checkbox" class="mt-0.5 h-4 w-4" />
                <span>Je comprends que cette action est irréversible.</span>
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
              class="mt-4 inline-flex w-full items-center justify-center gap-2 rounded-xl bg-red-500 px-4 py-2.5 text-sm font-semibold text-slate-950 transition hover:bg-red-400 focus:outline-none focus:ring-2 focus:ring-red-400 disabled:cursor-not-allowed disabled:opacity-50 sm:w-auto"
            >
              {{ deleting ? 'Suppression...' : 'Supprimer mon compte' }}
            </button>
          </div>

        </section>

        <section
          class="rounded-2xl border border-slate-800/80 bg-gradient-to-b from-slate-900/95 via-slate-900/80 to-slate-950/70 p-4 shadow-2xl backdrop-blur sm:rounded-3xl sm:p-6 lg:p-7"
        >
          <h2 class="text-lg font-semibold text-white">Sécurité</h2>
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
              {{ loading ? 'Modification...' : 'Mettre à jour' }}
            </button>
          </form>
        </section>
      </div>
    </div>

    <LegalProfileModal
      v-model="legalProfileModalOpen"
      :initial-profile="legalProfile"
      :mandatory="false"
      @saved="handleLegalProfileSaved"
      @cancel="cancelLegalProfileEdit"
    />
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ChevronDown, RefreshCw, UserRoundCheck } from 'lucide-vue-next'
import LegalProfileModal from '@/components/legal/LegalProfileModal.vue'
import { useAuthStore } from '@/store/authStore'
import { useBillingStore } from '@/store/billingStore'
import {
  getLegalProfileDetails,
  getLegalProfileOption,
  normalizeLegalProfile,
} from '@/constants/legalProfile'
import AuthService from '@/services/AuthService'
import LegalProfileService from '@/services/LegalProfileService'

const router = useRouter()
const auth = useAuthStore()
const billing = useBillingStore()

const currentUser = computed(() => auth.user.value || {})

const initials = computed(() => {
  const first = currentUser.value.firstName?.[0] || ''
  const last = currentUser.value.lastName?.[0] || ''
  return (first + last || 'U').toUpperCase()
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
const legalProfile = ref(normalizeLegalProfile(currentUser.value || {}))
const legalProfileLoading = ref(false)
const legalProfileModalOpen = ref(false)
const legalProfileConfirmOpen = ref(false)
const legalProfileError = ref('')
const legalProfileSuccess = ref('')
let legalProfileMessageTimer = null

const canDelete = computed(
  () => deleteConfirmChecked.value && deleteConfirmText.value.trim() === 'SUPPRIMER',
)
const showSubscriptionButton = computed(() =>
  ['active', 'past_due', 'canceled'].includes(billing.status.value),
)
const legalProfileCompleted = computed(() => Boolean(legalProfile.value.completed))
const legalProfileOption = computed(() => getLegalProfileOption(legalProfile.value.legalProfileType))
const legalProfileTitle = computed(() => legalProfileOption.value.title)
const legalProfileDescription = computed(() => legalProfileOption.value.description)
const legalProfileDetails = computed(() => getLegalProfileDetails(legalProfile.value))

onMounted(() => {
  void billing.fetchStatus()
  void loadLegalProfile()
})

onBeforeUnmount(() => {
  clearTimeout(legalProfileMessageTimer)
})

const resetMessages = () => {
  error.value = ''
  success.value = ''
}

const clearLegalProfileMessageLater = () => {
  clearTimeout(legalProfileMessageTimer)
  legalProfileMessageTimer = setTimeout(() => {
    legalProfileSuccess.value = ''
  }, 2600)
}

const applyLegalProfile = (profile) => {
  const nextProfile = normalizeLegalProfile(profile)
  legalProfile.value = nextProfile
  if (auth.user.value) {
    auth.setUser({
      ...auth.user.value,
      legalProfile: nextProfile,
      legalProfileCompleted: nextProfile.completed,
    })
  }
}

const loadLegalProfile = async () => {
  legalProfileLoading.value = true
  legalProfileError.value = ''

  try {
    applyLegalProfile(await LegalProfileService.getLegalProfile())
  } catch {
    legalProfile.value = normalizeLegalProfile(currentUser.value || {})
    legalProfileError.value = 'Profil administratif chargé depuis le compte local.'
  } finally {
    legalProfileLoading.value = false
  }
}

const openLegalProfileEdit = () => {
  legalProfileError.value = ''
  legalProfileSuccess.value = ''
  if (legalProfileCompleted.value) {
    legalProfileConfirmOpen.value = true
    return
  }
  legalProfileModalOpen.value = true
}

const confirmLegalProfileEdit = () => {
  legalProfileConfirmOpen.value = false
  legalProfileModalOpen.value = true
}

const cancelLegalProfileEdit = () => {
  legalProfileConfirmOpen.value = false
  legalProfileModalOpen.value = false
}

const handleLegalProfileSaved = async (savedProfile) => {
  applyLegalProfile(savedProfile)
  legalProfileConfirmOpen.value = false
  legalProfileModalOpen.value = false
  legalProfileSuccess.value = 'Profil administratif mis à jour.'

  try {
    auth.setUser(await AuthService.me())
    legalProfile.value = normalizeLegalProfile(auth.user.value || {})
  } catch {
    applyLegalProfile(savedProfile)
  } finally {
    clearLegalProfileMessageLater()
  }
}

const submitChangePassword = async () => {
  resetMessages()

  if (form.value.newPassword !== form.value.confirmPassword) {
    error.value = 'Les nouveaux mots de passe ne correspondent pas.'
    return
  }
  if (form.value.newPassword.length < 6) {
    error.value = 'Le nouveau mot de passe doit faire au moins 6 caractères.'
    return
  }

  loading.value = true
  try {
    await AuthService.changePassword({
      currentPassword: form.value.currentPassword,
      newPassword: form.value.newPassword,
    })

    success.value = 'Mot de passe modifié avec succès.'
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
  router.push({ name: 'abo-view' })
}
</script>

<style scoped>
.account-page-dark {
  min-height: 100dvh;
  background: #020617;
  color: #e2e8f0;
}

.account-page-dark :deep(button:not(:disabled)) {
  border-radius: 12px;
}

.account-profile-panel {
  display: grid;
  gap: 1rem;
  border: 1px solid rgba(139, 92, 246, 0.24);
  border-radius: 18px;
  background:
    linear-gradient(180deg, rgba(30, 41, 59, 0.66), rgba(15, 23, 42, 0.58)),
    rgba(15, 23, 42, 0.74);
  padding: 1rem;
}

.account-profile-head,
.account-profile-title-row {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.account-profile-head {
  justify-content: space-between;
}

.account-profile-eyebrow {
  margin: 0 0 0.25rem;
  color: rgb(196 181 253);
  font-size: 0.72rem;
  font-weight: 800;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.account-profile-head h2 {
  margin: 0;
  color: #ffffff;
  font-size: 1rem;
  font-weight: 700;
}

.account-profile-change {
  border: 1px solid rgba(71, 85, 105, 0.82);
  background: rgba(15, 23, 42, 0.74);
  color: rgb(226 232 240);
  transition:
    border-color 140ms ease,
    background 140ms ease,
    color 140ms ease,
    transform 140ms ease;
}

.account-profile-change {
  display: inline-flex;
  min-height: 38px;
  align-items: center;
  justify-content: center;
  gap: 0.45rem;
  padding: 0 0.85rem;
  font-size: 0.82rem;
  font-weight: 800;
}

.account-profile-change:hover {
  border-color: rgba(167, 139, 250, 0.72);
  background: rgba(76, 29, 149, 0.32);
  color: #ffffff;
}

.account-profile-current {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 0.85rem;
  align-items: flex-start;
}

.account-profile-icon {
  width: 22px;
  height: 22px;
  color: rgb(196 181 253);
}

.account-profile-title-row {
  flex-wrap: wrap;
  justify-content: space-between;
}

.account-profile-title-row strong {
  color: #ffffff;
  font-size: 0.96rem;
  line-height: 1.25;
}

.account-profile-current p {
  margin-top: 0.35rem;
  color: rgb(148 163 184);
  font-size: 0.86rem;
  line-height: 1.45;
}

.account-profile-level {
  display: inline-flex;
  min-height: 24px;
  align-items: center;
  border-radius: 999px;
  padding: 0 0.62rem;
  font-size: 0.72rem;
  font-weight: 850;
}

.account-profile-level.low {
  background: rgba(16, 185, 129, 0.15);
  color: rgb(167 243 208);
}

.account-profile-level.medium {
  background: rgba(245, 158, 11, 0.16);
  color: rgb(252 211 77);
}

.account-profile-traits {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.account-profile-traits span {
  display: inline-flex;
  min-height: 28px;
  align-items: center;
  border-radius: 999px;
  background: rgba(30, 41, 59, 0.82);
  color: rgb(203 213 225);
  padding: 0 0.7rem;
  font-size: 0.76rem;
  font-weight: 800;
}

.account-profile-message {
  border-radius: 12px;
  padding: 0.72rem 0.85rem;
  font-size: 0.84rem;
  font-weight: 750;
}

.account-profile-message.is-error {
  border: 1px solid rgba(251, 113, 133, 0.38);
  background: rgba(127, 29, 29, 0.18);
  color: rgb(254 202 202);
}

.account-profile-message.is-success {
  border: 1px solid rgba(52, 211, 153, 0.32);
  background: rgba(6, 78, 59, 0.2);
  color: rgb(167 243 208);
}

.account-profile-confirm {
  display: grid;
  gap: 0.85rem;
  border: 1px solid rgba(251, 191, 36, 0.3);
  border-radius: 12px;
  background: rgba(120, 53, 15, 0.18);
  padding: 0.9rem;
}

.account-profile-confirm p {
  margin: 0;
  color: rgb(253 230 138);
  font-size: 0.86rem;
  line-height: 1.5;
  font-weight: 700;
}

.account-profile-confirm-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 0.55rem;
}

.account-profile-primary,
.account-profile-secondary {
  min-height: 38px;
  border: 0;
  border-radius: 12px;
  padding: 0 0.9rem;
  font-size: 0.82rem;
  font-weight: 850;
}

.account-profile-primary {
  background: rgb(16 185 129);
  color: rgb(2 6 23);
}

.account-profile-secondary {
  background: rgba(30, 41, 59, 0.9);
  color: rgb(226 232 240);
}

@media (max-width: 640px) {
  .account-profile-head {
    align-items: stretch;
    flex-direction: column;
  }

  .account-profile-change {
    width: 100%;
  }

  .account-profile-confirm-actions,
  .account-profile-primary,
  .account-profile-secondary {
    width: 100%;
  }
}
</style>
