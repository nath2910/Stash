<template>
  <div class="min-h-full overflow-x-hidden bg-slate-950 text-slate-100">
    <div class="mx-auto flex min-h-dvh w-full max-w-6xl flex-col px-4 py-5 sm:px-6 lg:px-8">
      <div class="flex items-center justify-between gap-3">
        <button
          type="button"
          class="inline-flex h-10 items-center gap-2 rounded-lg border border-slate-800 bg-slate-900/80 px-3 text-sm font-semibold text-slate-200 transition hover:border-emerald-300/50 hover:text-white"
          @click="goToLogin"
        >
          <ArrowLeft class="h-4 w-4" />
          Connexion
        </button>

        <span
          class="inline-flex items-center gap-2 rounded-full border px-3 py-1 text-xs font-semibold"
          :class="statusMeta.badge"
        >
          <span class="h-2 w-2 rounded-full" :class="statusMeta.dot"></span>
          {{ statusMeta.label }}
        </span>
      </div>

      <main class="grid flex-1 items-center gap-6 py-8 lg:grid-cols-[1fr_420px] lg:py-10">
        <section class="min-w-0">
          <p class="text-xs font-bold uppercase tracking-[0.24em] text-emerald-300">
            {{ stripeTestMode ? 'Checkout test' : 'Abonnement mensuel' }}
          </p>
          <h1 class="mt-4 max-w-3xl text-3xl font-bold leading-tight text-white sm:text-5xl">
            MyStash Premium
          </h1>
          <p class="mt-4 max-w-2xl text-base leading-7 text-slate-300 sm:text-lg">
            Debloque les fonctions avancees pour suivre ton stock, tes ventes et tes performances sans gestion manuelle.
          </p>

          <div class="mt-7 grid gap-3 sm:grid-cols-2">
            <div
              v-for="feature in features"
              :key="feature.title"
              class="flex gap-3 rounded-xl border border-slate-800 bg-slate-900/65 p-4"
            >
              <span class="mt-0.5 inline-flex h-8 w-8 shrink-0 items-center justify-center rounded-lg bg-emerald-400/10 text-emerald-300">
                <component :is="feature.icon" class="h-4 w-4" />
              </span>
              <span class="min-w-0">
                <strong class="block text-sm font-semibold text-white">{{ feature.title }}</strong>
                <span class="mt-1 block text-sm leading-6 text-slate-400">{{ feature.desc }}</span>
              </span>
            </div>
          </div>

          <div class="mt-6 flex flex-wrap items-center gap-3 text-sm text-slate-400">
            <span class="inline-flex items-center gap-2">
              <ShieldCheck class="h-4 w-4 text-emerald-300" />
              Paiement securise par Stripe
            </span>
            <span class="inline-flex items-center gap-2">
              <RefreshCcw class="h-4 w-4 text-emerald-300" />
              Resiliable en ligne
            </span>
          </div>
        </section>

        <aside class="rounded-2xl border border-slate-800 bg-slate-900/80 p-5 shadow-2xl shadow-black/30 sm:p-6">
          <div class="flex items-start justify-between gap-4">
            <div>
              <p class="text-sm font-semibold text-slate-400">Offre mensuelle</p>
              <p class="mt-2 text-3xl font-bold text-white">{{ priceLabel }}</p>
            </div>
            <span
              class="rounded-full border px-3 py-1 text-xs font-semibold"
              :class="monthlyOfferAvailable ? 'border-emerald-300/30 bg-emerald-300/10 text-emerald-100' : 'border-amber-300/30 bg-amber-300/10 text-amber-100'"
            >
              {{ monthlyOfferAvailable ? 'Disponible' : 'Indisponible' }}
            </span>
          </div>

          <p class="mt-4 text-sm leading-6 text-slate-300">
            Acces immediat apres paiement. Renouvellement automatique chaque mois, annulable depuis le portail Stripe.
          </p>

          <div class="mt-5 space-y-3 border-y border-slate-800 py-4">
            <div
              v-for="item in checkoutDetails"
              :key="item"
              class="flex items-start gap-2 text-sm text-slate-300"
            >
              <CheckCircle2 class="mt-0.5 h-4 w-4 shrink-0 text-emerald-300" />
              <span>{{ item }}</span>
            </div>
          </div>

          <label class="mt-5 flex items-start gap-3 text-sm leading-6 text-slate-300">
            <input
              v-model="termsAccepted"
              type="checkbox"
              class="mt-1 h-4 w-4 rounded border-slate-700 bg-slate-950 text-emerald-400"
            />
            <span>
              J'accepte les
              <RouterLink to="/legal/cgv" target="_blank" class="font-semibold text-emerald-300 underline underline-offset-4">
                conditions de vente
              </RouterLink>.
            </span>
          </label>

          <button
            type="button"
            class="mt-5 inline-flex h-12 w-full items-center justify-center gap-2 rounded-xl bg-emerald-400 px-4 text-sm font-bold text-slate-950 shadow-lg shadow-emerald-500/20 transition hover:bg-emerald-300 disabled:cursor-not-allowed disabled:opacity-55"
            :disabled="ctaDisabled"
            @click="startCheckout"
          >
            <CreditCard class="h-4 w-4" />
            {{ ctaLabel }}
          </button>

          <button
            v-if="status === 'active'"
            type="button"
            class="mt-3 inline-flex h-11 w-full items-center justify-center rounded-xl border border-slate-700 bg-slate-950/50 px-4 text-sm font-semibold text-slate-100 transition hover:border-emerald-300/40"
            @click="openPortal"
          >
            Gerer mon abonnement
          </button>

          <p v-if="error" class="mt-4 rounded-lg border border-red-400/25 bg-red-400/10 px-3 py-2 text-sm text-red-200">
            {{ error }}
          </p>
          <p v-else class="mt-4 text-xs leading-5 text-slate-500">
            {{ stripeStatusCopy }}
          </p>
        </aside>
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import {
  ArrowLeft,
  BarChart3,
  CheckCircle2,
  CreditCard,
  DatabaseBackup,
  Headphones,
  RefreshCcw,
  ShieldCheck,
} from 'lucide-vue-next'
import { scopedStorageKey } from '@/RegleItem/storageScope'
import BillingService from '@/services/BillingService'
import { useAuthStore } from '@/store/authStore'
import { useBillingStore } from '@/store/billingStore'
import { describeBillingError } from '@/utils/billingErrors'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const billing = useBillingStore()

const status = ref<'unknown' | 'inactive' | 'active' | 'past_due' | 'canceled'>('unknown')
const portalUrl = ref('')
const loading = ref(false)
const error = ref('')
const stripeReady = ref(true)
type Plan = { id: string; amount: number; available: boolean; testMode: boolean }
const plans = ref<Plan[]>([])
const termsAccepted = ref(false)

const monthlyOffer = computed(() => plans.value.find((plan) => plan.id === 'monthly'))
const monthlyOfferAvailable = computed(() => Boolean(monthlyOffer.value?.available))
const stripeTestMode = computed(() => Boolean(monthlyOffer.value?.testMode))
const returnTo = computed(() => (route.query.returnTo as string) || '')
const successRedirect = computed(() => (route.query.successRedirect as string) || returnTo.value || '/')
const currentUserId = computed(() => auth.user.value?.id ?? 'guest')
const onboardingPendingStorageKey = computed(() =>
  scopedStorageKey('snk_onboarding_pending', currentUserId.value),
)
const shouldPollAfterCheckout = computed(
  () => route.query.success === '1' || typeof route.query.session_id === 'string',
)

let poll: number | null = null
let previousStatus: string | null = null

const formatPrice = (amount: number) =>
  new Intl.NumberFormat('fr-FR', { style: 'currency', currency: 'EUR' }).format(amount / 100)

const priceLabel = computed(() => {
  if (!monthlyOffer.value) return 'Tarif indisponible'
  return `${formatPrice(monthlyOffer.value.amount)} / mois`
})

const features = [
  {
    icon: BarChart3,
    title: 'Stats avancees',
    desc: 'Ventes, marges, stock et performances au meme endroit.',
  },
  {
    icon: DatabaseBackup,
    title: 'Sauvegarde cloud',
    desc: 'Tes donnees restent accessibles et synchronisees.',
  },
  {
    icon: Headphones,
    title: 'Support prioritaire',
    desc: 'Aide plus rapide si tu bloques sur un point important.',
  },
  {
    icon: ShieldCheck,
    title: 'Gestion Stripe',
    desc: 'Factures, carte et annulation dans un portail securise.',
  },
]

const checkoutDetails = computed(() =>
  stripeTestMode.value
    ? ['Mode test Stripe : aucun debit reel.', 'Activation simulee apres le checkout.', 'Webhook et portail testes de bout en bout.']
    : [
        `${priceLabel.value} TTC si ton tarif Stripe est actif.`,
        'Carte bancaire, Apple Pay ou Google Pay selon Stripe.',
        'Aucune donnee de carte stockee par MyStash.',
      ],
)

const ctaDisabled = computed(
  () =>
    loading.value ||
    status.value === 'active' ||
    !stripeReady.value ||
    !termsAccepted.value ||
    !monthlyOffer.value?.available,
)

const ctaLabel = computed(() => {
  if (status.value === 'active') return 'Abonnement actif'
  if (loading.value) return 'Redirection...'
  if (!stripeReady.value) return 'Paiement indisponible'
  if (!monthlyOffer.value?.available) return 'Tarif indisponible'
  if (!termsAccepted.value) return 'Accepte les conditions'
  return stripeTestMode.value ? 'Tester le checkout' : `S'abonner - ${formatPrice(monthlyOffer.value.amount)}`
})

const stripeStatusCopy = computed(() => {
  if (!stripeReady.value) return 'Stripe est temporairement indisponible. Reessaie dans quelques minutes.'
  if (!monthlyOffer.value?.available) return "Le prix mensuel Stripe n'est pas disponible pour le moment."
  return stripeTestMode.value
    ? 'Tu es en mode test Stripe : aucun paiement reel ne sera debite.'
    : 'Tu seras redirige vers Stripe pour finaliser le paiement.'
})

const statusMeta = computed(() => {
  switch (status.value) {
    case 'active':
      return {
        label: 'Actif',
        badge: 'border-emerald-300/30 bg-emerald-300/10 text-emerald-100',
        dot: 'bg-emerald-300',
      }
    case 'past_due':
      return {
        label: 'Paiement en attente',
        badge: 'border-amber-300/40 bg-amber-300/10 text-amber-100',
        dot: 'bg-amber-300',
      }
    case 'canceled':
      return {
        label: 'Annule',
        badge: 'border-red-300/40 bg-red-300/10 text-red-100',
        dot: 'bg-red-300',
      }
    case 'inactive':
    default:
      return {
        label: 'Inactif',
        badge: 'border-slate-700 bg-slate-900/70 text-slate-200',
        dot: 'bg-slate-400',
      }
  }
})

const fetchStatus = async (includePortal = false, forceRefresh = false) => {
  try {
    const res = await BillingService.status(includePortal, forceRefresh)
    status.value = (res?.data?.status as typeof status.value) || 'inactive'
    portalUrl.value = res?.data?.portalUrl || ''
    billing.seedStatus(status.value)

    if (previousStatus !== 'active' && status.value === 'active') {
      try {
        localStorage.setItem(onboardingPendingStorageKey.value, '1')
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
  if (ctaDisabled.value) return

  loading.value = true
  error.value = ''

  try {
    const res = await BillingService.checkout('monthly', termsAccepted.value)
    const url = res?.data?.url

    if (url) {
      window.location.assign(url)
      return
    }

    throw new Error('URL de paiement manquante')
  } catch (e: unknown) {
    error.value = describeBillingError(e, 'Impossible de lancer le paiement pour le moment.')
  } finally {
    loading.value = false
  }
}

const openPortal = async () => {
  if (!portalUrl.value) {
    await fetchStatus(true, false)
  }
  if (portalUrl.value) window.open(portalUrl.value, '_blank', 'noopener')
}

const redirectIfActive = () => {
  if (status.value === 'active') {
    router.replace(successRedirect.value || '/')
  }
}

onMounted(async () => {
  try {
    plans.value = (await BillingService.plans()).data
  } catch {
    error.value = 'Les tarifs sont temporairement indisponibles.'
  }
  await fetchStatus(false, shouldPollAfterCheckout.value)
  redirectIfActive()

  if (shouldPollAfterCheckout.value) {
    let attempts = 0
    poll = window.setInterval(async () => {
      if (++attempts > 12 || status.value === 'active') {
        if (poll) window.clearInterval(poll)
        return
      }
      if (document.visibilityState === 'hidden') return
      await fetchStatus(false, true)
      redirectIfActive()
    }, 15000)
  }
})

onBeforeUnmount(() => {
  if (poll) window.clearInterval(poll)
})
</script>
