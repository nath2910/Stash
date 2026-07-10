<template>
  <div class="stats-page-shell" :class="{ 'stats-page-shell--light': themeMode === 'light' }">
    <Transition name="stats-range-loader">
      <div v-if="rangeRefreshing" class="stats-range-loader" role="status" aria-live="polite">
        <span class="stats-range-loader__dot"></span>
        <div class="stats-range-loader__content">
          <strong>Mise a jour des stats</strong>
          <span>{{ rangeLabel }}</span>
        </div>
      </div>
    </Transition>

    <StatsCanvas v-model:from="from" v-model:to="to" />
  </div>
</template>

<script setup lang="ts">
import { useStatsRange } from '@/composables/useStatsRange'
import { computed, defineAsyncComponent, onBeforeUnmount, ref, watch } from 'vue'
import { useTheme } from '@/composables/useTheme'

const StatsCanvas = defineAsyncComponent(() => import('@/components/stats/StatsCanvas.vue'))
const { from, to } = useStatsRange()
const { theme } = useTheme()
const themeMode = computed(() => (theme.value === 'light' ? 'light' : 'dark'))
const rangeRefreshing = ref(false)
let rangeRefreshTimer: number | null = null

const rangeLabel = computed(() => {
  if (!from.value || !to.value) return 'Chargement de la nouvelle periode...'
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

onBeforeUnmount(() => {
  if (rangeRefreshTimer) window.clearTimeout(rangeRefreshTimer)
})

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
  background: #060a12;
}

.stats-page-shell--light {
  background: #f7f4ee;
  color: #000;
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

.stats-page-shell:not(.stats-page-shell--light) .stats-range-loader {
  border-color: rgba(148, 163, 184, 0.18);
  background: rgba(15, 23, 42, 0.84);
  color: #e2e8f0;
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
