<template>
  <div class="min-h-screen text-slate-100">
    <section class="relative w-full app-page-stack">
      <teleport to="body">
        <div v-if="showOnboarding" class="fixed inset-0 z-50">
          <div class="absolute inset-0 bg-black/60 backdrop-blur-sm" @click="closeOnboarding"></div>
          <div
            class="absolute inset-0 flex items-center justify-center px-4"
            aria-modal="true"
            role="dialog"
          >
            <div class="relative w-full max-w-lg rounded-3xl border border-slate-800 bg-slate-900/90 p-6 shadow-2xl">
              <button
                type="button"
                class="absolute right-3 top-3 text-slate-400 hover:text-white"
                aria-label="Fermer"
                @click="closeOnboarding"
              >
                ✕
              </button>
              <p class="text-xs uppercase tracking-[0.3em] text-emerald-200/80 mb-2">Bienvenue</p>
              <h2 class="text-2xl font-semibold text-white mb-2">Abonnement actif</h2>
              <p class="text-sm text-slate-300 mb-4">
                Explore les tableaux Stats, suit ton stock dans Gestion et retrouve tout ton compte ici.
              </p>
              <div class="flex flex-wrap gap-3">
                <button
                  type="button"
                  class="rounded-xl bg-emerald-500 px-4 py-2 text-sm font-semibold text-slate-900 shadow-md hover:bg-emerald-400"
                  @click="goToStatsFromModal"
                >
                  Ouvrir les stats
                </button>
                <button
                  type="button"
                  class="rounded-xl border border-slate-700 px-4 py-2 text-sm font-semibold text-slate-100 hover:border-emerald-300/60"
                  @click="goToGestionFromModal"
                >
                  Voir Gestion
                </button>
                <button
                  type="button"
                  class="rounded-xl border border-slate-700 px-4 py-2 text-sm text-slate-200 hover:border-slate-500"
                  @click="closeOnboarding"
                >
                  Plus tard
                </button>
              </div>
            </div>
          </div>
        </div>
      </teleport>

      <!-- Hero -->
      <header
        class="relative overflow-hidden rounded-[28px] border border-slate-800/70 bg-[radial-gradient(circle_at_top_left,_rgba(15,23,42,0.85),_rgba(2,6,23,0.95))] px-6 py-7 shadow-[0_30px_90px_rgba(0,0,0,0.45)] backdrop-blur sm:px-8"
      >
        <div class="absolute right-0 top-0 h-40 w-40 -translate-y-10 translate-x-16 rounded-full bg-emerald-400/8 blur-3xl"></div>
        <div class="flex flex-wrap items-start justify-between gap-6">
          <div class="max-w-2xl space-y-4">
            <p class="text-xs uppercase tracking-[0.35em] text-amber-200/80">
              Tableau de bord
            </p>
            <h1 class="text-3xl font-semibold text-white sm:text-4xl">
              Accueil
            </h1>
            <p class="text-base text-slate-400">
              Vue synthetique du stock, des ventes et du mois en cours.
            </p>
            <div class="mt-2 flex flex-wrap gap-2 text-xs">
              <span
                class="inline-flex items-center rounded-full border border-emerald-300/30 bg-emerald-300/10 px-3 py-1 text-emerald-100"
              >
                Temps reel
              </span>
              <span
                class="inline-flex items-center rounded-full border border-slate-700 bg-slate-900/70 px-3 py-1 text-slate-300"
              >
                Stock 
              </span>
              <span
                class="inline-flex items-center rounded-full border border-slate-700 bg-slate-900/70 px-3 py-1 text-slate-300"
              >
                Focus mois en cours
              </span>
            </div>
          </div>
        </div>
      </header>

      <!-- Contenu principal -->
      <div class="grid grid-cols-1 gap-6 sm:gap-8 lg:grid-cols-12 lg:items-start">
        <!-- Colonne droite (Overview) : en haut sur mobile, à droite sur desktop -->
        <aside class="order-1 lg:order-2 lg:col-span-4 w-full">
          <div
            class="lg:sticky lg:top-6 rounded-3xl border border-slate-800/80 bg-gradient-to-b from-slate-900/95 via-slate-900/80 to-slate-950/70 p-4 shadow-2xl backdrop-blur sm:p-5"
          >
            <HomeOverview
              :total-benefice="monthlyTotalBenefice"
              :total-c-a="monthlyTotalCA"
              :nb-vendues="monthlyNbVendues"
              :nb-en-stock="nbEnStock"
              :loading="loading"
              @go-gestion="goToGestion"
              @go-stats="goToStats"
            />
          </div>
        </aside>

        <!-- Colonne gauche : liste/dernier items -->
        <main class="order-2 lg:order-1 lg:col-span-8 min-w-0 space-y-6">
          <Affichage10 />
        </main>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import Affichage10 from '@/components/AcceuilDernierItem.vue'
import HomeOverview from '@/components/AcceuilWidgetLateral.vue'
import SnkVenteServices from '@/services/SnkVenteServices.js'
import { useAuthStore } from '@/store/authStore'
import { isVendue, prixRetailOf, prixResellOf } from '@/utils/snkVente'
import { formatEUR, formatNumber } from '@/utils/formatters'

const router = useRouter()
const route = useRoute()

const ONBOARD_PENDING = 'snk_onboarding_pending'
const ONBOARD_SEEN = 'snk_onboarding_seen'
const showOnboarding = ref(false)

const { user } = useAuthStore()
const currentUser = user

const snkVentes = ref<unknown[]>([])
const loading = ref(false)

const chargerVentes = async () => {
  // ✅ ne lance pas l’API si pas connecté
  if (!currentUser.value) {
    snkVentes.value = []
    loading.value = false
    return
  }

  loading.value = true
  try {
    const { data } = await SnkVenteServices.getSnkVente()
    snkVentes.value = Array.isArray(data) ? data : []
  } catch (e) {
    console.error('Erreur chargement ventes (Accueil)', e)
    snkVentes.value = []
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  chargerVentes()
  try {
    const pending = localStorage.getItem(ONBOARD_PENDING) === '1' || route.query.onboarding === '1'
    const seen = localStorage.getItem(ONBOARD_SEEN) === '1'
    if (pending && !seen) {
      showOnboarding.value = true
      localStorage.setItem(ONBOARD_SEEN, '1')
    }
    if (pending) {
      localStorage.removeItem(ONBOARD_PENDING)
    }
  } catch (e) {
    console.warn('onboarding check', e)
  }
})

/**
 * Stats globales stock
 */
const nbEnStock = computed(() => snkVentes.value.filter((v) => !isVendue(v)).length)

/**
 * Stats du mois en cours
 */
const now = new Date()
const currentMonth = now.getMonth()
const currentYear = now.getFullYear()

const ventesDuMois = computed(() =>
  snkVentes.value.filter((v) => {
    const raw = v.dateVente ?? v.date_vente
    if (!raw) return false
    const d = new Date(raw)
    if (Number.isNaN(d.getTime())) return false
    return d.getMonth() === currentMonth && d.getFullYear() === currentYear
  }),
)

const monthlyNbVendues = computed(() => ventesDuMois.value.length)

const monthlyTotalCA = computed(() =>
  ventesDuMois.value.reduce((sum, v) => {
    const resell = prixResellOf(v)
    if (Number.isNaN(resell)) return sum
    return sum + resell
  }, 0),
)

const monthlyTotalBenefice = computed(() =>
  ventesDuMois.value.reduce((sum, v) => {
    const retail = prixRetailOf(v)
    const resell = prixResellOf(v)
    if (Number.isNaN(retail) || Number.isNaN(resell)) return sum
    return sum + (resell - retail)
  }, 0),
)

const monthlyAvgResell = computed(() => {
  if (!monthlyNbVendues.value) return 0
  return monthlyTotalCA.value / monthlyNbVendues.value
})

const formattedMonthlyCA = computed(() => formatEUR(monthlyTotalCA.value))
const formattedMonthlyBenefice = computed(() => formatEUR(monthlyTotalBenefice.value))
const formattedMonthlyAvgResell = computed(() => formatEUR(monthlyAvgResell.value))
const formattedMonthlyNbVendues = computed(() =>
  formatNumber(monthlyNbVendues.value, { compact: true }),
)
const formattedNbEnStock = computed(() => formatNumber(nbEnStock.value, { compact: true }))
const beneficePositive = computed(() => monthlyTotalBenefice.value >= 0)

const closeOnboarding = () => {
  showOnboarding.value = false
}

const goToStatsFromModal = () => {
  closeOnboarding()
  goToStats()
}

const goToGestionFromModal = () => {
  closeOnboarding()
  goToGestion()
}

const goToGestion = () => router.push('/gestion')
const goToStats = () => router.push('/stats')
</script>
