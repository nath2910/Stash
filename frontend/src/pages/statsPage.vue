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

    <StatsCanvas v-else :key="statsCanvasKey" v-model:from="from" v-model:to="to" />
  </div>
</template>

<script setup lang="ts">
import StatsCanvas from '@/components/stats/StatsCanvas.vue'
import { INVENTORY_CHANGED_EVENT } from '@/utils/inventoryEvents'
import { useStatsRange } from '@/composables/useStatsRange'
import { computed, onBeforeUnmount, onErrorCaptured, onMounted, ref, watch } from 'vue'

const { from, to } = useStatsRange()
const rangeRefreshing = ref(false)
const statsCanvasKey = ref(0)
const templateModeActive = ref(false)
const canvasError = ref('')
let rangeRefreshTimer: number | null = null
let inventoryRefreshTimer: number | null = null

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
  canvasError.value = String((error as Error)?.message || error || 'Erreur inconnue')
  console.error('[stats] Canvas render failed', error)
  return false
})

onBeforeUnmount(() => {
  if (rangeRefreshTimer) window.clearTimeout(rangeRefreshTimer)
  if (inventoryRefreshTimer) window.clearTimeout(inventoryRefreshTimer)
  window.removeEventListener(INVENTORY_CHANGED_EVENT, onInventoryChanged)
  window.removeEventListener('snk:stats-template-mode', onTemplateModeChange)
})

onMounted(() => {
  window.addEventListener(INVENTORY_CHANGED_EVENT, onInventoryChanged)
  window.addEventListener('snk:stats-template-mode', onTemplateModeChange)
})

function onInventoryChanged() {
  rangeRefreshing.value = true
  if (inventoryRefreshTimer) window.clearTimeout(inventoryRefreshTimer)
  inventoryRefreshTimer = window.setTimeout(() => {
    statsCanvasKey.value += 1
    rangeRefreshing.value = false
    inventoryRefreshTimer = null
  }, 120)
}

function onTemplateModeChange(event: Event) {
  templateModeActive.value = Boolean((event as CustomEvent)?.detail?.active)
}

function retryCanvas() {
  canvasError.value = ''
  statsCanvasKey.value += 1
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
  min-height: 0;
  overflow: hidden;
  background: #f7f4ee;
}

.stats-page-shell--light {
  background: #f7f4ee;
  color: #000;
}

.stats-page-shell--template-mode {
  height: auto;
  min-height: 100%;
  overflow: visible;
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
</style>
