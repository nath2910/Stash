<template>
  <div class="min-h-full bg-slate-950 text-slate-100">
    <div class="app-shell app-page-stack max-w-5xl pb-8">
      <div class="app-topbar">
        <button
          type="button"
          @click="goBack"
          class="app-touch-btn inline-flex items-center gap-2 rounded-full border border-slate-800 bg-slate-900/70 px-4 py-2 text-xs font-semibold text-slate-200 transition hover:border-emerald-400/50 hover:text-white"
        >
          <span class="text-sm"><-</span>
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
              Resume rapide de ton offre actuelle et acces au portail Stripe.
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
        </div>
        <div class="rounded-2xl border border-slate-800/80 bg-slate-900/70 p-4 space-y-1">
          <p class="text-xs uppercase tracking-[0.2em] text-slate-500">Plan</p>
          <p class="text-lg font-semibold text-white">Premium mensuel</p>
          <p class="text-sm text-slate-400">9,90 EUR, annulation a tout moment.</p>
        </div>
        <div class="rounded-2xl border border-slate-800/80 bg-slate-900/70 p-4 space-y-1">
          <p class="text-xs uppercase tracking-[0.2em] text-slate-500">Support</p>
          <p class="text-lg font-semibold text-white">Discord + email</p>
          <p class="text-sm text-slate-400">Role Premium applique automatiquement apres lien.</p>
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
            Telecharger mes factures
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

const router = useRouter()
const billing = useBillingStore()

const status = computed(() => billing.status.value)
const portalUrl = computed(() => billing.portalUrl.value)
const portalBusy = ref(false)
const portalError = ref('')

const statusMeta = computed(() => {
  switch (status.value) {
    case 'active':
      return {
        label: 'Actif',
        note: 'Acces total debloque.',
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
        label: 'Annule',
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

const canOpenPortal = computed(() => ['active', 'past_due', 'canceled'].includes(status.value))

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
    const msg = e instanceof Error ? e.message : 'Erreur lors de l ouverture du portail Stripe.'
    portalError.value = msg
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
  await billing.fetchStatus(true, true)
})
</script>
