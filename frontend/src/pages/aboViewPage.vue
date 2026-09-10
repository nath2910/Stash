<template>
  <div class="subscription-view-page min-h-full overflow-x-hidden bg-slate-950 text-slate-100">
    <div class="app-shell app-page-stack max-w-5xl pb-8">
      <div class="app-topbar">
        <button
          type="button"
          @click="goBack"
          class="app-touch-btn inline-flex items-center gap-2 rounded-full border border-slate-800 bg-slate-900/70 px-4 py-2 text-xs font-semibold text-slate-200 transition hover:border-emerald-400/50 hover:text-white"
        >
          <span class="text-sm">&lt;-</span>
          <span>Retour profil</span>
        </button>
        <span class="app-pill border-slate-700 bg-slate-900/70 text-slate-300">
          {{ statusMeta.label }}
        </span>
      </div>

      <header
        class="rounded-3xl border border-slate-800/80 bg-slate-900/80 p-6 sm:p-8 shadow-2xl backdrop-blur space-y-6"
      >
        <div class="flex flex-col gap-6 lg:flex-row lg:items-center lg:justify-between">
          <div class="space-y-3">
            <p class="text-xs uppercase tracking-[0.32em] text-emerald-200/80">Abonnement</p>
            <h1 class="text-3xl font-semibold text-white sm:text-4xl">Mon abonnement</h1>
            <p class="text-sm text-slate-300">
              Résumé rapide de ton offre actuelle et accès au portail Stripe.
            </p>
            <div class="flex flex-wrap items-center gap-2 text-xs">
              <span
                class="inline-flex items-center gap-2 rounded-full border px-3 py-1"
                :class="statusMeta.badge"
              >
                <span class="h-2 w-2 rounded-full" :class="statusMeta.dot"></span>
                {{ statusMeta.label }}
              </span>
              <span
                class="inline-flex items-center rounded-full border border-slate-700 bg-slate-900/70 px-3 py-1 text-slate-200"
              >
                9,90 EUR / mois
              </span>
            </div>
          </div>

          <div class="w-full max-w-sm rounded-2xl border border-slate-800/80 bg-slate-900/70 p-5 space-y-2">
            <p class="text-xs uppercase tracking-[0.2em] text-slate-500">Portail Stripe</p>
            <p class="text-lg font-semibold text-white">Gerer le paiement</p>
            <p class="text-sm text-slate-400">Moyen de paiement, factures, annulation.</p>
            <button
              type="button"
              class="mt-3 w-full inline-flex items-center justify-center rounded-xl border border-slate-700 bg-slate-900/70 px-3 py-2 text-sm font-semibold text-slate-100 hover:border-emerald-300/40 transition disabled:opacity-60"
              :disabled="portalBusy || !canOpenPortal"
              @click="openPortal"
            >
              {{ portalBusy ? 'Ouverture...' : 'Ouvrir le portail Stripe' }}
            </button>
            <p v-if="portalError" class="text-xs text-red-300">{{ portalError }}</p>
          </div>
        </div>
      </header>

      <section class="grid gap-4 sm:grid-cols-3">
        <div class="rounded-2xl border border-slate-800/80 bg-slate-900/70 p-4 space-y-1">
          <p class="text-xs uppercase tracking-[0.2em] text-slate-500">Etat</p>
          <p class="text-lg font-semibold text-white">{{ statusMeta.label }}</p>
          <p class="text-sm text-slate-400">{{ statusMeta.note }}</p>
          <p v-if="periodEnd" class="text-sm text-slate-200">
            {{ cancelAtPeriodEnd ? 'Votre abonnement reste actif jusqu’au' : 'Prochaine échéance le' }} {{ periodEnd }}.
          </p>
          <button v-if="canCancel" type="button" class="mt-3 rounded-xl border border-red-300/40 px-4 py-3 text-red-100" @click="cancelConfirm = true">Résilier mon abonnement</button>
          <div v-if="cancelConfirm" class="mt-3 space-y-3 text-sm">
            <p>Confirmer la résiliation ? L’accès reste ouvert jusqu’à la fin de la période payée. Votre compte et vos données sont conservés.</p>
            <button type="button" :disabled="portalBusy" class="rounded-xl bg-red-500 px-4 py-3 text-white" @click="cancelSubscription">Confirmer la résiliation</button>
            <button type="button" class="ml-3 underline" @click="cancelConfirm = false">Revenir</button>
          </div>
        </div>
        <div class="rounded-2xl border border-slate-800/80 bg-slate-900/70 p-4 space-y-1">
          <p class="text-xs uppercase tracking-[0.2em] text-slate-500">Plan</p>
          <p class="text-lg font-semibold text-white">Premium mensuel</p>
          <p class="text-sm text-slate-400">9,90 EUR, annulation a tout moment.</p>
        </div>
        <div class="rounded-2xl border border-slate-800/80 bg-slate-900/70 p-4 space-y-1">
          <p class="text-xs uppercase tracking-[0.2em] text-slate-500">Support</p>
          <p class="text-lg font-semibold text-white">Discord + email</p>
          <p class="text-sm text-slate-400">Rôle Premium appliqué automatiquement après liaison.</p>
        </div>
      </section>

      <section class="rounded-3xl border border-slate-800/80 bg-slate-900/75 p-5 shadow-2xl space-y-4">
        <div class="flex flex-wrap items-center justify-between gap-2">
          <h2 class="text-lg font-semibold text-white">Actions rapides</h2>
          <span class="text-xs text-slate-500">Portail Stripe requis</span>
        </div>
        <div class="grid gap-3 sm:grid-cols-2">
          <button
            type="button"
            class="rounded-2xl border border-slate-700 bg-slate-900/70 px-4 py-3 text-left text-sm text-slate-200 hover:border-emerald-300/40 transition disabled:opacity-60 disabled:cursor-not-allowed"
            :disabled="portalBusy || !canOpenPortal"
            @click="openPortal"
          >
            Mettre a jour le moyen de paiement
          </button>
          <button
            type="button"
            class="rounded-2xl border border-slate-700 bg-slate-900/70 px-4 py-3 text-left text-sm text-slate-200 hover:border-emerald-300/40 transition disabled:opacity-60 disabled:cursor-not-allowed"
            :disabled="portalBusy || !canOpenPortal"
            @click="openPortal"
          >
            Télécharger mes factures
          </button>
        </div>
        <button
          v-if="status === 'inactive'"
          type="button"
          class="w-full sm:w-auto rounded-xl border border-emerald-300/40 bg-emerald-300/10 px-4 py-2 text-sm font-semibold text-emerald-100 hover:bg-emerald-300/15 transition"
          @click="goToUpgrade"
        >
          Passer Premium
        </button>
        <p class="text-xs text-slate-500">
          Pour toute modification avancee, passe par le portail Stripe.
        </p>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useBillingStore } from '@/store/billingStore'
import { describeBillingError } from '@/utils/billingErrors'
import BillingService from '@/services/BillingService'

const router = useRouter()
const billing = useBillingStore()

const status = computed(() => billing.status.value)
const portalUrl = computed(() => billing.portalUrl.value)
const portalBusy = ref(false)
const portalError = ref('')
const cancelConfirm = ref(false)
const cancelAtPeriodEnd = ref(false)
const periodEnd = ref('')
const canCancel = computed(() => ['active', 'trialing', 'past_due', 'unpaid', 'paused'].includes(status.value) && !cancelAtPeriodEnd.value)
const applySnapshot = (data: { status: string; currentPeriodEnd?: string; cancelAtPeriodEnd?: boolean }) => {
  billing.seedStatus(data.status)
  cancelAtPeriodEnd.value = Boolean(data.cancelAtPeriodEnd)
  periodEnd.value = data.currentPeriodEnd ? new Date(data.currentPeriodEnd).toLocaleDateString('fr-FR') : ''
}
const cancelSubscription = async () => {
  portalBusy.value = true
  try {
    applySnapshot((await BillingService.cancel()).data)
    cancelConfirm.value = false
  } catch (error) {
    portalError.value = describeBillingError(error, 'Résiliation temporairement indisponible. Réessayez.')
  } finally { portalBusy.value = false }
}

const statusMeta = computed(() => {
  switch (status.value) {
    case 'active':
      return {
        label: 'Actif',
        note: 'Accès total débloqué.',
        badge: 'border-emerald-300/30 bg-emerald-300/10 text-emerald-100',
        dot: 'bg-emerald-300',
      }
    case 'past_due':
      return {
        label: 'Paiement en attente',
        note: 'Mets a jour ta carte dans le portail.',
        badge: 'border-amber-300/40 bg-amber-300/10 text-amber-100',
        dot: 'bg-amber-300',
      }
    case 'canceled':
      return {
        label: 'Annulé',
        note: 'Relance un checkout pour reactiver.',
        badge: 'border-red-300/40 bg-red-300/10 text-red-100',
        dot: 'bg-red-300',
      }
    case 'inactive':
    default:
      return {
        label: 'Inactif',
        note: 'Souscris pour debloquer les pages.',
        badge: 'border-slate-700 bg-slate-900/70 text-slate-200',
        dot: 'bg-slate-400',
      }
  }
})

const canOpenPortal = computed(() => status.value !== 'inactive' && status.value !== 'unknown')

const openPortal = async () => {
  portalError.value = ''
  if (!canOpenPortal.value) {
    portalError.value = 'Portail Stripe indisponible pour cet etat d abonnement.'
    return
  }

  portalBusy.value = true
  try {
    await billing.fetchStatus(true, true)
    if (!portalUrl.value) {
      throw new Error('Impossible de recuperer le portail Stripe pour le moment.')
    }
    window.location.assign(portalUrl.value)
  } catch (e: unknown) {
    portalError.value = describeBillingError(
      e,
      'Erreur lors de l ouverture du portail Stripe.',
    )
  } finally {
    portalBusy.value = false
  }
}

const goBack = () => {
  if (window.history.length > 1) router.back()
  else router.push({ name: 'account' })
}

const goToUpgrade = () => {
  router.push({ name: 'abo' })
}

onMounted(async () => {
  try { applySnapshot((await BillingService.status(false, true)).data) }
  catch (error) { portalError.value = describeBillingError(error, 'État temporairement indisponible.') }
})
</script>

<style scoped>
@media (max-width: 640px) {
  .subscription-view-page {
    min-height: 100dvh;
  }

  .subscription-view-page :deep(.app-shell) {
    padding-inline: max(0.85rem, env(safe-area-inset-left)) max(0.85rem, env(safe-area-inset-right));
  }

  .subscription-view-page :deep(.app-page-stack) {
    gap: 1rem;
    padding-block: 0.9rem calc(1.25rem + env(safe-area-inset-bottom, 0px));
  }

  .subscription-view-page :deep(header),
  .subscription-view-page :deep(section),
  .subscription-view-page :deep(section > div) {
    border-radius: 18px !important;
  }

  .subscription-view-page :deep(button) {
    width: 100%;
    min-height: 44px;
    justify-content: center;
    margin-left: 0 !important;
  }

  .subscription-view-page :deep(.app-topbar) {
    display: grid;
    grid-template-columns: minmax(0, 1fr) auto;
  }

  .subscription-view-page h1 {
    font-size: clamp(2rem, 10vw, 2.6rem);
  }
}

@media (max-width: 420px) {
  .subscription-view-page :deep(.app-shell) {
    padding-inline: max(0.65rem, env(safe-area-inset-left)) max(0.65rem, env(safe-area-inset-right));
  }
}
</style>
