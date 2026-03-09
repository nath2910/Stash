<template>
  <div class="min-h-screen bg-slate-950 text-slate-100">
    <div class="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8 py-10 space-y-10">
      <header
        class="rounded-3xl border border-slate-800/80 bg-slate-900/85 p-6 shadow-[0_30px_90px_rgba(0,0,0,0.45)] backdrop-blur space-y-6"
      >
        <div class="grid gap-6 lg:grid-cols-[1.1fr_0.9fr]">
          <div class="space-y-4">
            <p class="text-xs uppercase tracking-[0.32em] text-emerald-200/80">Abonnement</p>
            <div class="flex flex-wrap items-center gap-3">
              <h1 class="text-3xl font-semibold text-white sm:text-4xl">Passe en Premium</h1>
              <span
                class="inline-flex items-center gap-2 rounded-full border px-3 py-1 text-xs"
                :class="statusMeta.badge"
              >
                <span class="h-2 w-2 rounded-full" :class="statusMeta.dot"></span>
                {{ statusMeta.label }}
              </span>
            </div>
            <p class="text-base text-slate-300 max-w-3xl">
              Offre Premium: stats avancees en temps reel, sauvegarde cloud illimitee, export, role Discord Premium, support prioritaire.
            </p>
            <div class="flex flex-wrap items-center gap-2 text-xs">
              <span
                class="inline-flex items-center rounded-full border border-slate-700 bg-slate-900/70 px-3 py-1 text-slate-200"
              >
                {{ priceLabel }}
              </span>
              <span class="inline-flex items-center rounded-full border border-slate-700 bg-slate-900/70 px-3 py-1 text-slate-200">
                Sans engagement · annulation rapide
              </span>
            </div>
            <div class="grid gap-3 sm:grid-cols-2">
              <div
                v-for="perk in perks"
                :key="perk.title"
                class="rounded-2xl border border-slate-800/80 bg-slate-900/70 p-4 space-y-1"
              >
                <p class="text-sm font-semibold text-white">{{ perk.title }}</p>
                <p class="text-sm text-slate-300">{{ perk.desc }}</p>
              </div>
            </div>
          </div>

          <div class="w-full rounded-2xl border border-slate-800/80 bg-slate-900/75 p-5 space-y-4 shadow-2xl">
            <p class="text-xs uppercase tracking-[0.2em] text-slate-500">Activation rapide</p>
            <p class="text-lg font-semibold text-white">1 minute pour activer</p>
            <p class="text-sm text-slate-400">
              Paiement CB, Apple Pay ou Google Pay. Gestion des factures et de la carte dans le portail de paiement.
            </p>

            <div class="space-y-3">
              <div class="space-y-1">
                <label class="text-xs text-slate-400">Code promo (optionnel)</label>
                <input
                  v-model="promo"
                  type="text"
                  placeholder="Promo (si tu en as une)"
                  class="w-full rounded-xl bg-slate-800/70 border border-slate-700 px-3 py-2 text-sm focus:outline-none focus:border-emerald-300/60"
                />
              </div>
            </div>

            <button
              type="button"
              class="w-full h-11 rounded-xl bg-emerald-500 text-slate-900 font-semibold shadow-lg shadow-emerald-500/25 hover:bg-emerald-400 transition disabled:opacity-60 disabled:cursor-not-allowed"
              :disabled="ctaDisabled"
              @click="startCheckout"
            >
              {{ ctaLabel }}
            </button>

            <p v-if="error" class="text-sm text-red-300">{{ error }}</p>
            <div v-else class="text-xs text-slate-400 flex items-center gap-2">
              <span
                class="h-2.5 w-2.5 rounded-full"
                :class="stripeReady ? 'bg-emerald-400' : 'bg-amber-300'"
              ></span>
              <span>{{ stripeStatusCopy }}</span>
            </div>

            <div
              v-if="status === 'active' && portalUrl"
              class="flex items-center gap-2 rounded-xl bg-slate-800/60 border border-emerald-300/20 px-3 py-2"
            >
              <span class="text-sm text-slate-200">Deja abonne ?</span>
              <button
                type="button"
                class="text-sm font-semibold text-emerald-300 hover:text-emerald-200"
                @click="openPortal"
              >
                Ouvrir le portail Stripe
              </button>
            </div>
          </div>
        </div>
      </header>

      <section class="grid gap-4 sm:grid-cols-3">
        <div class="rounded-2xl border border-slate-800/80 bg-slate-900/60 p-4 space-y-1">
          <p class="text-xs uppercase tracking-[0.2em] text-slate-500">Etat</p>
          <p class="text-lg font-semibold text-white">{{ statusMeta.label }}</p>
          <p class="text-sm text-slate-400">{{ statusMeta.note }}</p>
        </div>
        <div class="rounded-2xl border border-slate-800/80 bg-slate-900/60 p-4 space-y-2">
          <p class="text-xs uppercase tracking-[0.2em] text-slate-500">Ce qui est inclus</p>
          <ul class="space-y-1 text-sm text-slate-300">
            <li v-for="item in included" :key="item">• {{ item }}</li>
          </ul>
        </div>
        <div class="rounded-2xl border border-slate-800/80 bg-slate-900/60 p-4 space-y-1">
          <p class="text-xs uppercase tracking-[0.2em] text-slate-500">Annulation</p>
          <p class="text-sm text-slate-300">Sans engagement, annulation et factures gerees dans le portail de paiement.</p>
        </div>
      </section>

      <section class="grid gap-6 lg:grid-cols-[1.1fr_0.9fr]">
        <div class="rounded-3xl border border-slate-800/80 bg-slate-900/70 p-6 shadow-2xl space-y-4">
          <h2 class="text-lg font-semibold text-white">Ce que tu debloques</h2>
          <ul class="space-y-2 text-sm text-slate-200">
            <li v-for="benefit in benefits" :key="benefit" class="flex items-start gap-2">
              <span class="text-emerald-300">•</span>
              <span>{{ benefit }}</span>
            </li>
          </ul>
        </div>

        <aside class="rounded-3xl border border-slate-800/80 bg-slate-900/60 p-5 space-y-3">
          <h3 class="text-base font-semibold text-white">Garantie et securite</h3>
          <div v-for="policy in policies" :key="policy.title" class="space-y-1">
            <p class="text-sm font-semibold text-white">{{ policy.title }}</p>
            <p class="text-sm text-slate-300">{{ policy.desc }}</p>
          </div>
        </aside>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import BillingService from '@/services/BillingService'

const route = useRoute()
const router = useRouter()

const status = ref<'unknown' | 'inactive' | 'active' | 'past_due' | 'canceled'>('unknown')
const portalUrl = ref('')
const promo = ref('')
const loading = ref(false)
const error = ref('')
const stripeReady = ref(true)
const priceLabel = '9,90 EUR / mois'
const returnTo = computed(() => (route.query.returnTo as string) || '')
let poll: number | null = null
const onboardingFlag = 'snk_onboarding_pending'
let previousStatus: string | null = null
const successRedirect = computed(() => (route.query.successRedirect as string) || returnTo.value || '/')

const perks = [
  { title: 'Stats et pilotage', desc: 'Tableaux temps reel, filtres multi-profils, exports CSV.' },
  { title: 'Sauvegarde cloud', desc: 'Sauvegardes automatiques et restauration instantanee.' },
  { title: 'Discord Premium', desc: 'Role applique automatiquement apres paiement.' },
  { title: 'Support prioritaire', desc: 'Acces direct, reponse rapide, onboarding si besoin.' },
]

const included = [
  'Role Discord Premium applique automatiquement',
  'Sauvegarde cloud illimitee et restauration',
  'Exports CSV / partage equipe',
  'Support prioritaire',
]

const benefits = [
  'Vue temps reel des ventes, marges et stocks.',
  'Sauvegarde cloud securisee et synchronisee.',
  'Automatisation Discord (role Premium) sans action manuelle.',
  'Portail Stripe pour factures, CB, annulation.',
  'Aucun engagement: upgrade/downgrade libre.',
]

const policies = [
  { title: 'Engagement', desc: 'Sans engagement. Tu peux annuler ou re-activer en un clic dans Stripe.' },
  { title: 'Securite paiement', desc: 'Paiement traite par le prestataire bancaire, CB non stockee.' },
  { title: 'Factures', desc: 'Factures PDF et TVA disponibles dans le portail de paiement.' },
]

const ctaDisabled = computed(() => loading.value || status.value === 'active' || !stripeReady.value)
const ctaLabel = computed(() => {
  if (status.value === 'active') return 'Deja abonne'
  if (loading.value) return 'Redirection...'
  if (!stripeReady.value) return 'Paiement indisponible'
  return 'Passer Premium'
})

const stripeStatusCopy = computed(() =>
  stripeReady.value
    ? 'Checkout Stripe securise, redirection immediatement apres paiement.'
    : 'Service de paiement indisponible. Reessaie dans quelques minutes ou contacte le support.',
)

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
        note: 'Mets a jour ta carte dans Stripe.',
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
        note: 'Souscris pour debloquer les pages protegees.',
        badge: 'border-slate-700 bg-slate-900/70 text-slate-200',
        dot: 'bg-slate-400',
      }
  }
})

const fetchStatus = async () => {
  try {
    const res = await BillingService.status()
    status.value = (res?.data?.status as any) || 'inactive'
    portalUrl.value = res?.data?.portalUrl || ''
    if (previousStatus !== 'active' && status.value === 'active') {
      try {
        localStorage.setItem(onboardingFlag, '1')
      } catch (e) {
        console.warn('onboarding flag', e)
      }
    }
    previousStatus = status.value
    stripeReady.value = true
  } catch (e) {
    stripeReady.value = false
  }
}

const startCheckout = async () => {
  if (!stripeReady.value || status.value === 'active') return
  loading.value = true
  error.value = ''
  try {
    const res = await BillingService.checkout(promo.value || undefined, undefined)
    const url = res?.data?.url
    if (url) {
      window.location.assign(url)
    } else {
      throw new Error('URL de paiement manquante')
    }
  } catch (e: any) {
    error.value = e?.message || 'Impossible de lancer le paiement pour le moment'
  } finally {
    loading.value = false
  }
}

const openPortal = () => {
  if (portalUrl.value) window.open(portalUrl.value, '_blank', 'noopener')
}

const redirectIfActive = () => {
  if (status.value === 'active') {
    const target = successRedirect.value || '/'
    router.replace(target)
  }
}

onMounted(async () => {
  await fetchStatus()
  redirectIfActive()
  poll = window.setInterval(async () => {
    await fetchStatus()
    redirectIfActive()
  }, 3000)
})

onBeforeUnmount(() => {
  if (poll) window.clearInterval(poll)
})
</script>
