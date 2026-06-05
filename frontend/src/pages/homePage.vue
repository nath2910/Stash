<template>
  <div class="home-page-light">
    <teleport to="body">
      <div v-if="showOnboarding" class="fixed inset-0 z-50">
        <div class="absolute inset-0 bg-slate-950/55 backdrop-blur-sm" @click="closeOnboarding"></div>
        <div class="absolute inset-0 flex items-center justify-center px-4" aria-modal="true" role="dialog">
          <div class="relative max-h-[calc(100dvh-2rem)] w-full max-w-lg overflow-y-auto rounded-3xl border border-slate-200 bg-white p-5 text-slate-950 shadow-2xl sm:p-6">
            <button
              type="button"
              class="absolute right-3 top-3 rounded-full p-2 text-slate-500 hover:bg-slate-100 hover:text-slate-900"
              aria-label="Fermer"
              @click="closeOnboarding"
            >
              <X class="h-4 w-4" aria-hidden="true" />
            </button>
            <p class="mb-2 text-xs font-extrabold uppercase text-teal-700">Bienvenue</p>
            <h2 class="mb-2 text-2xl font-extrabold text-slate-950">Abonnement actif</h2>
            <p class="mb-4 text-sm text-slate-600">
              Retrouve tes actions rapides, tes stats et ton inventaire depuis l'accueil.
            </p>
            <div class="flex flex-col gap-3 min-[420px]:flex-row min-[420px]:flex-wrap">
              <button
                type="button"
                class="rounded-xl bg-teal-700 px-4 py-2 text-sm font-bold text-white shadow-md hover:bg-teal-600"
                @click="goToStatsFromModal"
              >
                Ouvrir les stats
              </button>
              <button
                type="button"
                class="rounded-xl border border-slate-300 px-4 py-2 text-sm font-bold text-slate-800 hover:border-teal-500"
                @click="goToGestionFromModal"
              >
                Voir Gestion
              </button>
              <button
                type="button"
                class="rounded-xl border border-slate-300 px-4 py-2 text-sm font-bold text-slate-600 hover:border-slate-500"
                @click="closeOnboarding"
              >
                Plus tard
              </button>
            </div>
          </div>
        </div>
      </div>
    </teleport>

    <section class="home-action-shell" aria-label="Actions rapides">
      <QuickSearchBar :items="stockItems" :loading="stockLoading" @select="openItemModal" />

      <QuickAddItemForm
        :items="stockItems"
        :saving="quickAddSaving"
        :success-key="quickAddSuccessKey"
        @error="handleQuickAddValidationError"
        @submit="handleQuickAdd"
      />

      <HomeMonthlyKpis :summary="kpiSummary" :loading="kpiLoading" :error="kpiError" />
    </section>

    <QuickItemModal
      :open="Boolean(selectedItem)"
      :item="selectedItem"
      :items="stockItems"
      :saving="modalSaving"
      :error="modalError"
      :success-key="modalSuccessKey"
      @close="closeItemModal"
      @save="handleQuickUpdate"
    />

    <teleport to="body">
      <Transition name="quick-add-toast">
        <div
          v-if="quickAddToast.visible"
          class="quick-add-toast"
          :class="`quick-add-toast--${quickAddToast.type || 'success'}`"
          role="status"
          aria-live="polite"
        >
          <span class="quick-add-toast__icon">
            <CheckCircle2 v-if="quickAddToast.type !== 'error'" class="h-4 w-4" aria-hidden="true" />
            <CircleAlert v-else class="h-4 w-4" aria-hidden="true" />
          </span>
          <span>{{ quickAddToast.message }}</span>
        </div>
      </Transition>
    </teleport>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { CheckCircle2, CircleAlert, X } from 'lucide-vue-next'
import QuickSearchBar from '@/components/home/QuickSearchBar.vue'
import QuickAddItemForm from '@/components/home/QuickAddItemForm.vue'
import HomeMonthlyKpis from '@/components/home/HomeMonthlyKpis.vue'
import QuickItemModal from '@/components/home/QuickItemModal.vue'
import SnkVenteServices from '@/services/SnkVenteServices.js'
import StatsServices from '@/services/StatsServices.js'
import { useAuthStore } from '@/store/authStore'
import { calculatePeriodStats, getCurrentYearRange } from '@/utils/homeDashboard'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()

const ONBOARD_PENDING = 'snk_onboarding_pending'
const ONBOARD_SEEN = 'snk_onboarding_seen'

const showOnboarding = ref(false)
const stockItems = ref([])
const stockLoading = ref(false)
const stockLoaded = ref(false)
const stockError = ref('')
const statsLoading = ref(false)
const statsError = ref('')
const apiSummary = ref(null)
const selectedItem = ref(null)
const modalSaving = ref(false)
const modalError = ref('')
const modalSuccessKey = ref(0)
const quickAddSaving = ref(false)
const quickAddSuccessKey = ref(0)
const quickAddToast = ref({ visible: false, message: '', type: 'success' })
const annualRange = getCurrentYearRange()
let quickAddToastTimer = null

const localSummary = computed(() => calculatePeriodStats(stockItems.value, annualRange))

const normalizedApiSummary = computed(() => {
  const data = apiSummary.value
  if (!data) return null
  return {
    ca: Number(data.ca ?? 0),
    profit: Number(data.profit ?? 0),
    profitMargin: Number(data.profitMargin ?? 0),
    itemsVendues: Number(data.itemsVendues ?? 0),
    itemsEnStock: Number(data.itemsEnStock ?? 0),
    valeurStock: Number(data.valeurStock ?? 0),
    estimatedStockValue: Number(data.valeurStock ?? 0),
  }
})

const kpiSummary = computed(() => (stockLoaded.value ? localSummary.value : normalizedApiSummary.value || localSummary.value))
const kpiLoading = computed(() => (statsLoading.value || stockLoading.value) && !stockLoaded.value && !apiSummary.value)
const kpiError = computed(() => stockError.value || statsError.value)

function notifyStockChanged(items = stockItems.value) {
  if (typeof window === 'undefined') return
  window.dispatchEvent(
    new CustomEvent('snk:stock-items-change', {
      detail: { items, source: 'home' },
    }),
  )
}

async function chargerVentes() {
  if (!auth.token.value) {
    stockItems.value = []
    stockLoaded.value = true
    return
  }

  stockLoading.value = true
  stockError.value = ''
  try {
    const { data } = await SnkVenteServices.getSnkVente()
    stockItems.value = Array.isArray(data) ? data : []
    stockLoaded.value = true
    notifyStockChanged()
  } catch (error) {
    console.error('Erreur chargement stock accueil', error)
    stockError.value = "Impossible de charger l'inventaire."
    stockItems.value = []
    stockLoaded.value = false
  } finally {
    stockLoading.value = false
  }
}

async function chargerStatsAnnuelles() {
  if (!auth.token.value) return

  statsLoading.value = true
  statsError.value = ''
  try {
    const { data } = await StatsServices.summary(annualRange.from, annualRange.to)
    apiSummary.value = data || null
  } catch (error) {
    console.error('Erreur chargement KPI accueil', error)
    statsError.value = "KPI calcules localement si l'inventaire est disponible."
  } finally {
    statsLoading.value = false
  }
}

onMounted(() => {
  chargerVentes()
  chargerStatsAnnuelles()
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
  } catch (error) {
    console.warn('onboarding check', error)
  }
})

onBeforeUnmount(() => {
  if (quickAddToastTimer) {
    window.clearTimeout(quickAddToastTimer)
  }
})

function showQuickAddToast(message, type = 'success') {
  if (quickAddToastTimer) {
    window.clearTimeout(quickAddToastTimer)
  }
  quickAddToast.value = { visible: true, message, type }
  quickAddToastTimer = window.setTimeout(() => {
    quickAddToast.value = { visible: false, message: '', type }
    quickAddToastTimer = null
  }, 2000)
}

function resolveQuickAddError(error) {
  const data = error?.response?.data
  const rawMessage =
    (typeof data === 'string' && data) ||
    data?.message ||
    data?.error ||
    error?.message ||
    ''
  const message = String(rawMessage || '').trim()
  if (!message || message === 'Network Error') {
    return "Item non ajoute. Verifie la connexion et les champs."
  }
  return message
}

function handleQuickAddValidationError(message) {
  showQuickAddToast(message || "Item non ajoute. Verifie les champs.", 'error')
}

function openItemModal(item) {
  selectedItem.value = item
  modalError.value = ''
}

function closeItemModal() {
  selectedItem.value = null
  modalError.value = ''
}

async function handleQuickUpdate({ id, payload }) {
  modalSaving.value = true
  modalError.value = ''
  try {
    const { data } = await SnkVenteServices.update(id, payload)
    const saved = data || { ...payload, id }
    const index = stockItems.value.findIndex((item) => item.id === id)
    if (index >= 0) {
      stockItems.value = stockItems.value.map((item, i) => (i === index ? saved : item))
    } else {
      stockItems.value = [saved, ...stockItems.value]
    }
    selectedItem.value = saved
    modalSuccessKey.value += 1
    notifyStockChanged()
    await chargerStatsAnnuelles()
  } catch (error) {
    modalError.value = error?.response?.data?.message || 'Erreur lors de la modification.'
  } finally {
    modalSaving.value = false
  }
}

async function handleQuickAdd({ payload, quantity }) {
  quickAddSaving.value = true
  try {
    const safeQuantity = Math.min(50, Math.max(1, Math.trunc(Number(quantity || 1))))
    const { data } = await SnkVenteServices.createMany(payload, safeQuantity)
    const created = Array.isArray(data) ? data : data ? [data] : []
    if (created.length > 0) {
      stockItems.value = [...created, ...stockItems.value]
      stockLoaded.value = true
    } else {
      await chargerVentes()
    }
    notifyStockChanged()
    await chargerStatsAnnuelles()
    quickAddSuccessKey.value += 1
    showQuickAddToast(safeQuantity > 1 ? `${safeQuantity} items ajoutes` : 'Item ajoute')
  } catch (error) {
    showQuickAddToast(resolveQuickAddError(error), 'error')
  } finally {
    quickAddSaving.value = false
  }
}

function closeOnboarding() {
  showOnboarding.value = false
}

function goToStatsFromModal() {
  closeOnboarding()
  router.push('/stats')
}

function goToGestionFromModal() {
  closeOnboarding()
  router.push('/gestion')
}
</script>

<style scoped>
.home-page-light {
  position: relative;
  min-height: calc(100dvh - 7rem);
  margin-inline: calc(clamp(16px, 2.2vw, 32px) * -1);
  margin-top: clamp(0.75rem, 1vw, 1.25rem);
  margin-bottom: 4rem;
  overflow-x: clip;
  background: #f7f4ee;
  color: #0f172a;
}

.home-page-light::before {
  display: none;
  content: none;
}

.home-action-shell {
  width: min(100%, 1540px);
  max-width: 100%;
  box-sizing: border-box;
  display: grid;
  gap: clamp(1rem, 2vw, 1.35rem);
  margin-inline: auto;
  padding:
    clamp(1rem, 2vw, 1.6rem)
    clamp(16px, 2.2vw, 32px)
    clamp(6rem, 10vw, 8rem);
}

.quick-add-toast {
  position: fixed;
  right: clamp(1rem, 2vw, 1.5rem);
  bottom: clamp(1rem, 2vw, 1.5rem);
  z-index: 10020;
  display: inline-flex;
  max-width: min(22rem, calc(100vw - 2rem));
  align-items: center;
  gap: 0.6rem;
  border: 1px solid rgba(20, 184, 166, 0.28);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.96);
  padding: 0.72rem 0.9rem 0.72rem 0.72rem;
  color: #0f172a;
  font-size: 0.9rem;
  font-weight: 850;
  box-shadow:
    0 18px 44px rgba(15, 23, 42, 0.16),
    inset 0 1px 0 rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(14px);
}

.quick-add-toast__icon {
  display: inline-grid;
  width: 1.85rem;
  height: 1.85rem;
  flex: 0 0 auto;
  place-items: center;
  border-radius: 999px;
  background: #ccfbf1;
  color: #0f766e;
}

.quick-add-toast--error {
  border-color: rgba(248, 113, 113, 0.34);
  background: rgba(255, 251, 251, 0.98);
}

.quick-add-toast--error .quick-add-toast__icon {
  background: #fee2e2;
  color: #b91c1c;
}

.quick-add-toast-enter-active,
.quick-add-toast-leave-active {
  transition:
    opacity 180ms ease,
    transform 180ms ease;
}

.quick-add-toast-enter-from,
.quick-add-toast-leave-to {
  opacity: 0;
  transform: translateY(10px) scale(0.98);
}

@media (max-width: 640px) {
  .home-page-light {
    margin-top: 0.25rem;
  }

  .home-action-shell {
    gap: 0.85rem;
    padding-inline-start: max(12px, env(safe-area-inset-left));
    padding-inline-end: max(12px, env(safe-area-inset-right));
  }

  .quick-add-toast {
    right: 1rem;
    bottom: calc(1rem + env(safe-area-inset-bottom, 0px));
    left: 1rem;
    justify-content: center;
  }
}
</style>
