<template>
  <div class="min-h-screen min-h-[100dvh] overflow-y-auto bg-slate-950 text-slate-100 px-4 py-6 sm:py-8 lg:py-10">
    <div class="max-w-5xl mx-auto space-y-6 sm:space-y-8 pb-8">
      <div class="flex items-center justify-between">
        <button
          type="button"
          @click="goBack"
          class="inline-flex items-center gap-2 rounded-full border border-slate-800 bg-slate-900/70 px-4 py-2 text-xs font-semibold text-slate-200 transition hover:border-emerald-400/50 hover:text-white"
        >
          <span class="text-sm">←</span>
          <span>Retour profil</span>
        </button>
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
                9,90 € / mois
              </span>
            </div>
          </div>

          <div class="w-full max-w-sm rounded-2xl border border-slate-800/80 bg-slate-900/70 p-5 space-y-2">
            <p class="text-xs uppercase tracking-[0.2em] text-slate-500">Portail Stripe</p>
            <p class="text-lg font-semibold text-white">Gérer le paiement</p>
            <p class="text-sm text-slate-400">Moyen de paiement, factures, annulation.</p>
            <button
              type="button"
              class="mt-3 w-full inline-flex items-center justify-center rounded-xl border border-slate-700 bg-slate-900/70 px-3 py-2 text-sm font-semibold text-slate-100 hover:border-emerald-300/40 transition disabled:opacity-60"
              :disabled="status !== 'active'"
              @click="openPortal"
            >
              Ouvrir le portail Stripe
            </button>
          </div>
        </div>
      </header>

      <section class="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        <div class="rounded-2xl border border-slate-800/80 bg-slate-900/70 p-4 space-y-1">
          <p class="text-xs uppercase tracking-[0.2em] text-slate-500">État</p>
          <p class="text-lg font-semibold text-white">{{ statusMeta.label }}</p>
          <p class="text-sm text-slate-400">{{ statusMeta.note }}</p>
        </div>
        <div class="rounded-2xl border border-slate-800/80 bg-slate-900/70 p-4 space-y-1">
          <p class="text-xs uppercase tracking-[0.2em] text-slate-500">Plan</p>
          <p class="text-lg font-semibold text-white">Premium mensuel</p>
          <p class="text-sm text-slate-400">9,90 €, annulation à tout moment.</p>
        </div>
        <div class="rounded-2xl border border-slate-800/80 bg-slate-900/70 p-4 space-y-1">
          <p class="text-xs uppercase tracking-[0.2em] text-slate-500">Support</p>
          <p class="text-lg font-semibold text-white">Discord + email</p>
          <p class="text-sm text-slate-400">Rôle Premium appliqué automatiquement après lien.</p>
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
            class="rounded-2xl border border-slate-700 bg-slate-900/70 px-4 py-3 text-left text-sm text-slate-200 hover:border-emerald-300/40 transition"
            @click="openPortal"
          >
            Mettre à jour le moyen de paiement
          </button>
          <button
            type="button"
            class="rounded-2xl border border-slate-700 bg-slate-900/70 px-4 py-3 text-left text-sm text-slate-200 hover:border-emerald-300/40 transition"
            @click="openPortal"
          >
            Télécharger mes factures
          </button>
        </div>
        <p class="text-xs text-slate-500">
          Pour toute modification avancée, passe par le portail Stripe.
        </p>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useBillingStore } from '@/store/billingStore'

const router = useRouter()
const billing = useBillingStore()
const status = computed(() => billing.status.value)
const portalUrl = computed(() => billing.portalUrl.value)

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
        note: 'Met à jour ta carte dans le portail.',
        badge: 'border-amber-300/40 bg-amber-300/10 text-amber-100',
        dot: 'bg-amber-300',
      }
    case 'canceled':
      return {
        label: 'Annulé',
        note: 'Relance un checkout pour réactiver.',
        badge: 'border-red-300/40 bg-red-300/10 text-red-100',
        dot: 'bg-red-300',
      }
    case 'inactive':
    default:
      return {
        label: 'Inactif',
        note: 'Souscris pour débloquer les pages.',
        badge: 'border-slate-700 bg-slate-900/70 text-slate-200',
        dot: 'bg-slate-400',
      }
  }
})

const openPortal = async () => {
  if (!portalUrl.value) {
    await billing.fetchStatus(true, true)
  }
  if (portalUrl.value) window.open(portalUrl.value, '_blank', 'noopener')
}

const goBack = () => {
  if (window.history.length > 1) router.back()
  else router.push({ name: 'account' })
}

onMounted(() => {
  billing.fetchStatus(false, true)
})
</script>
