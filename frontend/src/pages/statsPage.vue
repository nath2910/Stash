<template>
  <div
    class="stats-page-shell stats-page-shell--light"
    :class="{ 'stats-page-shell--template-mode': templateModeActive }"
  >
    <Transition name="stats-range-loader">
      <div v-if="rangeRefreshing" class="stats-range-loader" role="status" aria-live="polite">
        <span class="stats-range-loader__dot"></span>
        <div class="stats-range-loader__content">
          <strong>Mise a jour des stats</strong>
          <span>{{ rangeLabel }}</span>
        </div>
      </div>
    </Transition>

    <div v-if="canvasError" class="stats-canvas-error" role="alert">
      <strong>Stats indisponibles</strong>
      <span>Recharge la page ou reviens dans un instant.</span>
      <button type="button" @click="retryCanvas">Recharger</button>
    </div>

    <StatsCanvas
      v-else
      :key="statsCanvasKey"
      v-model:from="from"
      v-model:to="to"
      @ready="onCanvasReady"
    />

    <div v-if="showCanvasWatchdog" class="stats-canvas-watchdog" role="status" aria-live="polite">
      <span class="stats-canvas-watchdog__ring" aria-hidden="true"></span>
      <strong>{{ canvasStalled ? 'Chargement interrompu' : 'Chargement des stats' }}</strong>
      <span>
        {{
          canvasStalled
            ? "On relance l'affichage sans toucher a tes donnees."
            : 'Preparation de ton espace statistiques...'
        }}
      </span>
      <button v-if="canvasStalled" type="button" @click="retryCanvas">Relancer</button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { INVENTORY_CHANGED_EVENT } from '@/utils/inventoryEvents'
import { useStatsRange } from '@/composables/useStatsRange'
import { computed, defineAsyncComponent, h, onBeforeUnmount, onErrorCaptured, onMounted, ref, watch } from 'vue'

const CANVAS_WATCHDOG_VISIBLE_MS = 1200
const CANVAS_STALLED_MS = 8000
const CANVAS_AUTO_RETRY_MS = 10_000
const MAX_AUTO_RETRIES = 2

const StatsCanvas = defineAsyncComponent({
  loader: () => import('@/components/stats/StatsCanvas.vue'),
  delay: 0,
  timeout: 20_000,
  loadingComponent: {
    name: 'StatsCanvasLoadingFallback',
    render: () =>
      h('div', { class: 'stats-canvas-bootstrap', role: 'status', 'aria-live': 'polite' }, [
        h('span', { class: 'stats-canvas-watchdog__ring', 'aria-hidden': 'true' }),
        h('strong', 'Chargement des stats'),
        h('span', 'Preparation de ton espace statistiques...'),
      ]),
  },
  errorComponent: {
    name: 'StatsCanvasErrorFallback',
    render: () =>
      h('div', { class: 'stats-canvas-bootstrap stats-canvas-bootstrap--error', role: 'alert' }, [
        h('strong', 'Stats indisponibles'),
        h('span', "Le module stats n'a pas charge correctement. Recharge la page."),
      ]),
  },
})

const { from, to } = useStatsRange()
const rangeRefreshing = ref(false)
const statsCanvasKey = ref(0)
const templateModeActive = ref(false)
const canvasError = ref('')
const canvasReady = ref(false)
const canvasStalled = ref(false)
const watchdogVisible = ref(false)
let rangeRefreshTimer: number | null = null
let inventoryRefreshTimer: number | null = null
let watchdogVisibleTimer: number | null = null
let stalledTimer: number | null = null
let autoRetryTimer: number | null = null
let autoRetryCount = 0

const showCanvasWatchdog = computed(
  () => !canvasError.value && !canvasReady.value && watchdogVisible.value,
)

const rangeLabel = computed(() => {
  if (!from.value || !to.value) return 'Chargement de la nouvelle période…'
  return `${formatDateLabel(from.value)} -> ${formatDateLabel(to.value)}`
})

watch(
  () => [from.value, to.value],
  ([nextFrom, nextTo], [prevFrom, prevTo]) => {
    if (!prevFrom || !prevTo || (nextFrom === prevFrom && nextTo === prevTo)) return
    rangeRefreshing.value = true
    if (rangeRefreshTimer) window.clearTimeout(rangeRefreshTimer)
    rangeRefreshTimer = window.setTimeout(() => {
      rangeRefreshing.value = false
      rangeRefreshTimer = null
    }, 700)
  },
)

onErrorCaptured((error) => {
  clearCanvasWatchdogTimers()
  canvasError.value = String((error as Error)?.message || error || 'Erreur inconnue')
  console.error('[stats] Canvas render failed', error)
  return false
})

onBeforeUnmount(() => {
  if (rangeRefreshTimer) window.clearTimeout(rangeRefreshTimer)
  if (inventoryRefreshTimer) window.clearTimeout(inventoryRefreshTimer)
  clearCanvasWatchdogTimers()
  window.removeEventListener(INVENTORY_CHANGED_EVENT, onInventoryChanged)
  window.removeEventListener('snk:stats-template-mode', onTemplateModeChange)
})

onMounted(() => {
  window.addEventListener(INVENTORY_CHANGED_EVENT, onInventoryChanged)
  window.addEventListener('snk:stats-template-mode', onTemplateModeChange)
  armCanvasWatchdog()
})

function onInventoryChanged() {
  rangeRefreshing.value = true
  if (inventoryRefreshTimer) window.clearTimeout(inventoryRefreshTimer)
  inventoryRefreshTimer = window.setTimeout(() => {
    remountCanvas({ resetRetries: true })
    rangeRefreshing.value = false
    inventoryRefreshTimer = null
  }, 120)
}

function onTemplateModeChange(event: Event) {
  templateModeActive.value = Boolean((event as CustomEvent)?.detail?.active)
}

function retryCanvas() {
  canvasError.value = ''
  remountCanvas({ resetRetries: true })
}

function onCanvasReady() {
  canvasReady.value = true
  canvasStalled.value = false
  watchdogVisible.value = false
  autoRetryCount = 0
  clearCanvasWatchdogTimers()
}

function remountCanvas({ resetRetries = false } = {}) {
  if (resetRetries) autoRetryCount = 0
  canvasReady.value = false
  canvasStalled.value = false
  watchdogVisible.value = false
  statsCanvasKey.value += 1
  armCanvasWatchdog()
}

function clearCanvasWatchdogTimers() {
  if (watchdogVisibleTimer) {
    window.clearTimeout(watchdogVisibleTimer)
    watchdogVisibleTimer = null
  }
  if (stalledTimer) {
    window.clearTimeout(stalledTimer)
    stalledTimer = null
  }
  if (autoRetryTimer) {
    window.clearTimeout(autoRetryTimer)
    autoRetryTimer = null
  }
}

function armCanvasWatchdog() {
  clearCanvasWatchdogTimers()
  if (canvasReady.value || canvasError.value) return
  watchdogVisibleTimer = window.setTimeout(() => {
    if (canvasReady.value || canvasError.value) return
    watchdogVisible.value = true
  }, CANVAS_WATCHDOG_VISIBLE_MS)
  stalledTimer = window.setTimeout(() => {
    if (canvasReady.value || canvasError.value) return
    canvasStalled.value = true
  }, CANVAS_STALLED_MS)
  autoRetryTimer = window.setTimeout(() => {
    if (canvasReady.value || canvasError.value) return
    if (autoRetryCount >= MAX_AUTO_RETRIES) return
    autoRetryCount += 1
    remountCanvas()
  }, CANVAS_AUTO_RETRY_MS)
}

function formatDateLabel(value: string) {
  const [year, month, day] = value.split('-').map(Number)
  const date = new Date(year, (month || 1) - 1, day || 1)
  return new Intl.DateTimeFormat('fr-FR', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
  }).format(date)
}
</script>

<style scoped>
.stats-page-shell {
  position: relative;
  width: 100%;
  height: 100%;
  min-height: 100dvh;
  overflow: hidden;
  background: #f7f4ee;
}

.stats-page-shell--light {
  background: #f7f4ee;
  color: #000;
}

.stats-page-shell--template-mode {
  height: 100dvh;
  max-height: 100dvh;
  min-height: 0;
  overflow-y: auto;
  overflow-x: hidden;
  overscroll-behavior-y: contain;
  scrollbar-width: none;
  -ms-overflow-style: none;
  -webkit-overflow-scrolling: touch;
}

.stats-page-shell--template-mode::-webkit-scrollbar {
  display: none;
}

.stats-range-loader {
  position: absolute;
  top: 1rem;
  right: 1rem;
  z-index: 28;
  display: inline-flex;
  align-items: center;
  gap: 0.75rem;
  border: 1px solid rgba(148, 163, 184, 0.18);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.9);
  padding: 0.65rem 0.9rem;
  color: #0f172a;
  box-shadow: 0 18px 36px rgba(15, 23, 42, 0.12);
  backdrop-filter: blur(12px);
}

.stats-range-loader__dot {
  width: 0.75rem;
  height: 0.75rem;
  border-radius: 999px;
  background: linear-gradient(135deg, #14b8a6, #0ea5e9);
  box-shadow: 0 0 0 0 rgba(14, 165, 233, 0.34);
  animation: stats-range-pulse 1.15s ease-in-out infinite;
}

.stats-range-loader__content {
  display: grid;
  gap: 0.1rem;
}

.stats-range-loader__content strong {
  font-size: 0.78rem;
  font-weight: 900;
}

.stats-range-loader__content span {
  font-size: 0.7rem;
  opacity: 0.8;
}

.stats-canvas-error {
  position: absolute;
  inset: 0;
  z-index: 20;
  display: grid;
  place-content: center;
  gap: 0.85rem;
  padding: 1.5rem;
  text-align: center;
  color: #111827;
  background:
    radial-gradient(circle at 50% 32%, rgba(14, 165, 233, 0.12), transparent 34%),
    #f7f4ee;
}

.stats-canvas-error strong {
  font-size: 1.05rem;
  font-weight: 900;
}

.stats-canvas-error span {
  color: #4b5563;
  font-size: 0.92rem;
}

.stats-canvas-error button {
  justify-self: center;
  border: 1px solid rgba(17, 24, 39, 0.16);
  border-radius: 8px;
  background: #111827;
  padding: 0.65rem 1rem;
  color: #fff;
  font-weight: 800;
  cursor: pointer;
}

.stats-canvas-error button:hover {
  background: #1f2937;
}

.stats-canvas-watchdog {
  position: absolute;
  left: 50%;
  top: 50%;
  z-index: 18;
  display: grid;
  width: min(100% - 32px, 360px);
  transform: translate(-50%, -50%);
  place-items: center;
  gap: 0.65rem;
  border: 1px solid rgba(148, 163, 184, 0.24);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.92);
  padding: 1.25rem;
  text-align: center;
  color: #0f172a;
  box-shadow: 0 24px 60px rgba(15, 23, 42, 0.13);
  backdrop-filter: blur(14px);
}

.stats-canvas-bootstrap {
  position: absolute;
  inset: 0;
  z-index: 16;
  display: grid;
  place-content: center;
  justify-items: center;
  gap: 0.65rem;
  padding: 1.25rem;
  text-align: center;
  color: #0f172a;
  background:
    radial-gradient(circle at 50% 38%, rgba(14, 165, 233, 0.12), transparent 34%),
    #f7f4ee;
}

.stats-canvas-bootstrap strong {
  font-size: 0.98rem;
  font-weight: 900;
}

.stats-canvas-bootstrap span:not(.stats-canvas-watchdog__ring) {
  max-width: min(100% - 32px, 360px);
  color: #475569;
  font-size: 0.88rem;
  line-height: 1.45;
}

.stats-canvas-bootstrap--error strong {
  color: #991b1b;
}

.stats-canvas-watchdog__ring {
  width: 34px;
  height: 34px;
  border: 3px solid rgba(14, 165, 233, 0.18);
  border-top-color: #0ea5e9;
  border-radius: 999px;
  animation: stats-canvas-spin 760ms linear infinite;
}

.stats-canvas-watchdog strong {
  font-size: 0.98rem;
  font-weight: 900;
}

.stats-canvas-watchdog span:not(.stats-canvas-watchdog__ring) {
  color: #475569;
  font-size: 0.88rem;
  line-height: 1.45;
}

.stats-canvas-watchdog button {
  margin-top: 0.2rem;
  border: 1px solid rgba(17, 24, 39, 0.16);
  border-radius: 8px;
  background: #111827;
  padding: 0.55rem 0.9rem;
  color: #fff;
  font-weight: 800;
  cursor: pointer;
}

.stats-canvas-watchdog button:hover {
  background: #1f2937;
}

.stats-range-loader-enter-active,
.stats-range-loader-leave-active {
  transition:
    opacity 160ms ease,
    transform 160ms ease;
}

.stats-range-loader-enter-from,
.stats-range-loader-leave-to {
  opacity: 0;
  transform: translateY(-6px) scale(0.98);
}

@keyframes stats-range-pulse {
  0% {
    box-shadow: 0 0 0 0 rgba(14, 165, 233, 0.34);
  }
  100% {
    box-shadow: 0 0 0 10px rgba(14, 165, 233, 0);
  }
}

@keyframes stats-canvas-spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
