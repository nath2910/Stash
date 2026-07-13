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
      <QuickSearchBar
        :items="stockItems"
        :loading="stockLoading"
        @select="openItemModal"
        @add-requested="focusQuickAddForm"
      />

      <QuickAddItemForm
        ref="quickAddFormRef"
        :items="stockItems"
        :saving="quickAddSaving"
        :success-key="quickAddSuccessKey"
        @error="handleQuickAddValidationError"
        @submit="handleQuickAdd"
      />

      <HomeMonthlyKpis
        :summary="kpiSummary"
        :loading="kpiLoading"
        :error="kpiError"
        @add-requested="focusQuickAddForm"
      />
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
        <div v-if="quickAddToast.visible" class="quick-add-toast-layer" aria-live="polite">
          <div class="quick-add-toast-backdrop" aria-hidden="true"></div>
          <div
            class="quick-add-toast"
            :class="`quick-add-toast--${quickAddToast.type || 'success'}`"
            role="status"
          >
            <span class="quick-add-toast__icon">
              <CheckCircle2 v-if="quickAddToast.type !== 'error'" class="h-5 w-5" aria-hidden="true" />
              <CircleAlert v-else class="h-5 w-5" aria-hidden="true" />
            </span>
            <div class="quick-add-toast__copy">
              <strong>{{ quickAddToast.type === 'error' ? 'Ajout a verifier' : 'Ajout confirme' }}</strong>
              <span>{{ quickAddToast.message }}</span>
            </div>
          </div>
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
const quickAddFormRef = ref(null)
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
const clockTick = ref(Date.now())
const annualRange = computed(() => getCurrentYearRange(new Date(clockTick.value)))
let quickAddToastTimer = null
let clockTimer = null

const localSummary = computed(() => calculatePeriodStats(stockItems.value, annualRange.value))

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
    const { data } = await StatsServices.summary(annualRange.value.from, annualRange.value.to)
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
  scheduleClockRefresh()
  if (typeof document !== 'undefined') {
    document.addEventListener('visibilitychange', handleVisibilityChange)
  }
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
  if (clockTimer) {
    window.clearTimeout(clockTimer)
  }
  if (typeof document !== 'undefined') {
    document.removeEventListener('visibilitychange', handleVisibilityChange)
  }
})

function showQuickAddToast(message, type = 'success', duration = type === 'error' ? 1900 : 1400) {
  if (quickAddToastTimer) {
    window.clearTimeout(quickAddToastTimer)
  }
  quickAddToast.value = { visible: true, message, type }
  quickAddToastTimer = window.setTimeout(() => {
    quickAddToast.value = { visible: false, message: '', type }
    quickAddToastTimer = null
  }, duration)
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
    showQuickAddToast('Modifications enregistrees !')
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

function focusQuickAddForm() {
  quickAddFormRef.value?.focusFirstField?.()
}

function refreshClock() {
  const previousYear = new Date(clockTick.value).getFullYear()
  clockTick.value = Date.now()
  const nextYear = new Date(clockTick.value).getFullYear()
  if (nextYear !== previousYear) {
    void chargerStatsAnnuelles()
  }
}

function scheduleClockRefresh() {
  if (typeof window === 'undefined') return
  if (clockTimer) window.clearTimeout(clockTimer)
  const now = new Date()
  const next = new Date(now)
  next.setHours(24, 0, 5, 0)
  clockTimer = window.setTimeout(() => {
    refreshClock()
    scheduleClockRefresh()
  }, Math.max(30_000, next.getTime() - now.getTime()))
}

function handleVisibilityChange() {
  if (document.visibilityState !== 'visible') return
  refreshClock()
  scheduleClockRefresh()
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
  width: min(100%, 1700px);
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

.quick-add-toast-layer {
  position: fixed;
  inset: 0;
  z-index: 10020;
  display: grid;
  place-items: center;
  padding: 1.25rem;
  pointer-events: none;
}

.quick-add-toast-backdrop {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at center, rgba(15, 118, 110, 0.12), transparent 52%),
    rgba(15, 23, 42, 0.14);
  backdrop-filter: blur(6px);
}

.quick-add-toast {
  position: relative;
  z-index: 1;
  display: inline-flex;
  width: min(100%, 30rem);
  align-items: center;
  gap: 0.9rem;
  border: 1px solid rgba(20, 184, 166, 0.22);
  border-radius: 24px;
  background:
    linear-gradient(145deg, rgba(255, 255, 255, 0.98), rgba(240, 253, 250, 0.96));
  padding: 1rem 1.1rem;
  color: #0f172a;
  font-size: 0.96rem;
  font-weight: 800;
  box-shadow:
    0 26px 60px rgba(15, 23, 42, 0.2),
    inset 0 1px 0 rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(18px);
}

.quick-add-toast__icon {
  display: inline-grid;
  width: 3rem;
  height: 3rem;
  flex: 0 0 auto;
  place-items: center;
  border-radius: 18px;
  background: linear-gradient(135deg, #ccfbf1, #dbeafe);
  color: #0f766e;
}

.quick-add-toast__copy {
  display: grid;
  gap: 0.15rem;
}

.quick-add-toast__copy strong {
  font-size: 1rem;
  font-weight: 900;
}

.quick-add-toast__copy span {
  color: #475569;
  line-height: 1.4;
}

.quick-add-toast--error {
  border-color: rgba(248, 113, 113, 0.34);
  background: linear-gradient(145deg, rgba(255, 251, 251, 0.99), rgba(254, 242, 242, 0.97));
}

.quick-add-toast--error .quick-add-toast__icon {
  background: linear-gradient(135deg, #fee2e2, #fef2f2);
  color: #b91c1c;
}

.quick-add-toast-enter-active,
.quick-add-toast-leave-active {
  transition:
    opacity 140ms ease,
    transform 140ms ease;
}

.quick-add-toast-enter-from,
.quick-add-toast-leave-to {
  opacity: 0;
  transform: translateY(16px) scale(0.94);
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

  .quick-add-toast-layer {
    padding:
      max(1rem, env(safe-area-inset-top))
      1rem
      max(1rem, env(safe-area-inset-bottom))
      1rem;
  }

  .quick-add-toast {
    width: min(100%, 24rem);
    align-items: flex-start;
  }
}

@media (max-width: 420px) {
  .home-page-light {
    margin-inline: -10px;
  }

  .home-action-shell {
    gap: 0.75rem;
    padding-top: 0.7rem;
    padding-bottom: calc(5.25rem + env(safe-area-inset-bottom, 0px));
    padding-inline-start: max(10px, env(safe-area-inset-left));
    padding-inline-end: max(10px, env(safe-area-inset-right));
  }

  .quick-add-toast {
    gap: 0.75rem;
    border-radius: 18px;
    padding: 0.9rem;
    font-size: 0.82rem;
  }

  .quick-add-toast__icon {
    width: 2.7rem;
    height: 2.7rem;
  }

  .quick-add-toast__copy strong {
    font-size: 0.92rem;
  }
}

@media (min-width: 1800px) {
  .home-action-shell {
    width: min(100%, 1840px);
    gap: clamp(1.25rem, 1.4vw, 1.75rem);
    padding-inline: clamp(32px, 3vw, 56px);
  }
}
</style>
