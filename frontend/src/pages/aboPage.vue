<template>
  <div class="min-h-full bg-slate-950 text-slate-100">
    <div class="app-shell app-page-stack app-page-stack--abo max-w-6xl">
      <div class="app-topbar">
        <button
          type="button"
          class="app-touch-btn inline-flex items-center gap-2 rounded-xl border border-slate-800 bg-slate-900/70 px-3 py-2 text-xs font-medium text-slate-200 transition hover:border-emerald-300/40 hover:text-white"
          @click="goToLogin"
        >
          <span class="text-sm"><-</span>
          <span>Retour a la connexion</span>
        </button>

        <span class="app-pill border-slate-700 bg-slate-900/70 text-slate-300">
          {{ statusMeta.label }}
        </span>
      </div>

      <header
        class="rounded-3xl border border-slate-800/80 bg-slate-900/85 p-6 shadow-[0_30px_90px_rgba(0,0,0,0.45)] backdrop-blur sm:p-7"
      >
        <div class="grid gap-6 lg:grid-cols-[1.1fr_0.9fr]">
          <section class="space-y-5">
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
              <p class="max-w-3xl text-base text-slate-300">
                Une version plus simple a piloter: stats en temps reel, sauvegarde cloud,
                export et support prioritaire dans une seule formule.
              </p>
              <div class="flex flex-wrap items-center gap-2 text-xs">
                <span
                  class="inline-flex items-center rounded-full border border-slate-700 bg-slate-900/70 px-3 py-1 text-slate-200"
                >
                  {{ priceLabel }}
                </span>
                <span
                  class="inline-flex items-center rounded-full border border-slate-700 bg-slate-900/70 px-3 py-1 text-slate-200"
                >
                  Sans engagement
                </span>
                <span
                  class="inline-flex items-center rounded-full border border-slate-700 bg-slate-900/70 px-3 py-1 text-slate-200"
                >
                  Activation immediate
                </span>
              </div>
            </div>

            <div class="grid gap-3 sm:grid-cols-3">
              <div
                v-for="highlight in highlights"
                :key="highlight.title"
                class="rounded-2xl border border-slate-800/80 bg-slate-900/70 p-4"
              >
                <p class="text-xs uppercase tracking-[0.2em] text-slate-500">{{ highlight.eyebrow }}</p>
                <p class="mt-2 text-sm font-semibold text-white">{{ highlight.title }}</p>
                <p class="mt-1 text-sm text-slate-300">{{ highlight.desc }}</p>
              </div>
            </div>
          </section>

          <aside class="rounded-2xl border border-slate-800/80 bg-slate-900/75 p-5 shadow-2xl">
            <p class="text-xs uppercase tracking-[0.2em] text-slate-500">Activation rapide</p>
            <p class="mt-2 text-lg font-semibold text-white">1 minute pour activer</p>
            <p class="mt-2 text-sm text-slate-400">
              Paiement CB, Apple Pay ou Google Pay. Factures, carte et annulation sont
              ensuite geres dans le portail Stripe.
            </p>

            <div class="mt-4 rounded-2xl border border-emerald-300/15 bg-emerald-300/5 p-4">
              <div class="flex items-start justify-between gap-4">
                <div>
                  <p class="text-sm font-semibold text-white">{{ priceLabel }}</p>
                  <p class="mt-1 text-xs text-slate-400">
                    Acces immediat apres paiement, sans etape supplementaire.
                  </p>
                </div>
                <span
                  class="rounded-full border border-slate-700 bg-slate-900/80 px-3 py-1 text-[11px] text-slate-300"
                >
                  Mensuel
                </span>
              </div>
            </div>

            <button
              type="button"
              class="mt-4 h-11 w-full rounded-xl bg-emerald-500 font-semibold text-slate-900 shadow-lg shadow-emerald-500/25 transition hover:bg-emerald-400 disabled:cursor-not-allowed disabled:opacity-60"
              :disabled="ctaDisabled"
              @click="startCheckout"
            >
              {{ ctaLabel }}
            </button>

            <p v-if="error" class="mt-3 text-sm text-red-300">{{ error }}</p>
            <div v-else class="mt-3 flex items-center gap-2 text-xs text-slate-400">
              <span
                class="h-2.5 w-2.5 rounded-full"
                :class="stripeReady ? 'bg-emerald-400' : 'bg-amber-300'"
              ></span>
              <span>{{ stripeStatusCopy }}</span>
            </div>

            <div
              v-if="status === 'active'"
              class="mt-4 flex items-center gap-2 rounded-xl border border-emerald-300/20 bg-slate-800/60 px-3 py-2"
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
          </aside>
        </div>
      </header>

      <section class="grid gap-6 lg:grid-cols-[1.15fr_0.85fr]">
        <div class="rounded-3xl border border-slate-800/80 bg-slate-900/70 p-6 shadow-2xl space-y-5">
          <div>
            <p class="text-xs uppercase tracking-[0.2em] text-slate-500">Premium</p>
            <h2 class="mt-2 text-lg font-semibold text-white">
              Ce que tu debloques
            </h2>
            <p class="mt-2 max-w-2xl text-sm text-slate-400">
              L'essentiel, sans doublons: plus de suivi, plus de sauvegarde et une gestion plus simple.
            </p>
          </div>

          <div class="grid gap-3 sm:grid-cols-2">
            <div
              v-for="feature in features"
              :key="feature.title"
              class="rounded-2xl border border-slate-800/80 bg-slate-950/40 p-4"
            >
              <p class="text-sm font-semibold text-white">{{ feature.title }}</p>
              <p class="mt-1 text-sm text-slate-300">{{ feature.desc }}</p>
            </div>
          </div>
        </div>

        <aside class="rounded-3xl border border-slate-800/80 bg-slate-900/60 p-5 space-y-4">
          <div class="space-y-2">
            <p class="text-xs uppercase tracking-[0.2em] text-slate-500">Conditions</p>
            <h3 class="text-base font-semibold text-white">Simple a gerer</h3>
            <p class="text-sm text-slate-400">
              Paiement, factures et annulation restent centralises dans le portail Stripe.
            </p>
          </div>
          <div
            v-for="policy in policies"
            :key="policy.title"
            class="rounded-2xl border border-slate-800/80 bg-slate-950/30 p-4"
          >
            <p class="text-sm font-semibold text-white">{{ policy.title }}</p>
            <p class="mt-1 text-sm text-slate-300">{{ policy.desc }}</p>
          </div>
        </aside>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import BillingService from '@/services/BillingService'
import { useAuthStore } from '@/store/authStore'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const status = ref<'unknown' | 'inactive' | 'active' | 'past_due' | 'canceled'>('unknown')
const portalUrl = ref('')
const loading = ref(false)
const error = ref('')
const stripeReady = ref(true)
const priceLabel = '9,90 EUR / mois'
const returnTo = computed(() => (route.query.returnTo as string) || '')
const successRedirect = computed(
  () => (route.query.successRedirect as string) || returnTo.value || '/',
)
const onboardingFlag = 'snk_onboarding_pending'

let poll: number | null = null
let previousStatus: string | null = null
const shouldPollAfterCheckout = computed(
  () => route.query.success === '1' || typeof route.query.session_id === 'string',
)

const highlights = [
  {
    eyebrow: 'Pilotage',
    title: 'Stats en temps reel',
    desc: 'Vue claire sur ventes, marges et stocks sans attendre un export.',
  },
  {
    eyebrow: 'Cloud',
    title: 'Sauvegarde continue',
    desc: 'Tes donnees restent restaurees et synchronisees sans manipulation manuelle.',
  },
  {
    eyebrow: 'Support',
    title: 'Acces prioritaire',
    desc: 'Traitement plus rapide si tu bloques sur un point de config ou d usage.',
  },
]

const features = [
  {
    title: 'Exports et suivi',
    desc: 'Exports CSV, filtres multi-profils et lecture plus rapide des performances.',
  },
  {
    title: 'Role Discord Premium',
    desc: 'Application automatique apres activation, sans etape manuelle cote utilisateur.',
  },
  {
    title: 'Restauration instantanee',
    desc: 'Recuperation rapide des donnees en cas de besoin ou de changement de poste.',
  },
  {
    title: 'Gestion autonome',
    desc: 'Carte, factures et annulation directement disponibles dans le portail Stripe.',
  },
]

const policies = [
  {
    title: 'Sans engagement',
    desc: 'Tu peux annuler ou re-activer quand tu veux depuis le portail de paiement.',
  },
  {
    title: 'Paiement securise',
    desc: 'Le paiement est traite par Stripe. Les donnees carte ne sont pas stockees ici.',
  },
  {
    title: 'Factures accessibles',
    desc: 'Les factures PDF et les informations de paiement restent au meme endroit.',
  },
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

const fetchStatus = async (includePortal = false) => {
  try {
    const res = await BillingService.status(includePortal)
    status.value = (res?.data?.status as typeof status.value.value) || 'inactive'
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
  } catch {
    stripeReady.value = false
  }
}

const goToLogin = () => {
  auth.logout()
  router.replace({ name: 'auth', query: { mode: 'login' } })
}

const startCheckout = async () => {
  if (!stripeReady.value || status.value === 'active') return

  loading.value = true
  error.value = ''

  try {
    const res = await BillingService.checkout(undefined, undefined)
    const url = res?.data?.url

    if (url) {
      window.location.assign(url)
      return
    }

    throw new Error('URL de paiement manquante')
  } catch (e: any) {
    error.value = e?.message || 'Impossible de lancer le paiement pour le moment'
  } finally {
    loading.value = false
  }
}

const openPortal = async () => {
  if (!portalUrl.value) {
    await fetchStatus(true)
  }
  if (portalUrl.value) window.open(portalUrl.value, '_blank', 'noopener')
}

const redirectIfActive = () => {
  if (status.value === 'active') {
    router.replace(successRedirect.value || '/')
  }
}

onMounted(async () => {
  await fetchStatus(true)
  redirectIfActive()

  if (shouldPollAfterCheckout.value) {
    poll = window.setInterval(async () => {
      await fetchStatus(false)
      redirectIfActive()
    }, 15000)
  }
})

onBeforeUnmount(() => {
  if (poll) window.clearInterval(poll)
})
</script>

<style scoped>
.app-page-stack--abo {
  padding-top: clamp(0.45rem, 1vw, 0.95rem);
}
</style>
