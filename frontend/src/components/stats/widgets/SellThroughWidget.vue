<template>
  <KpiCard
    title="Taux d’écoulement"
    subtitle="Part du stock vendu"
    :accent="accent"
    surface="kpi"
    :loading="loading"
    :error="error"
    :widget-width="props.widgetWidth"
    :widget-height="props.widgetHeight"
    :widget-base-width="props.widgetBaseWidth"
    :widget-base-height="props.widgetBaseHeight"
    :valueText="valueText"
    :spark="spark"
  >
    <div class="st-top">
      <div class="st-status">
        <span class="st-status-label">Niveau</span>
        <span class="st-status-pill" :class="status.tone">{{ status.label }}</span>
      </div>
      <div class="st-period">
        <span class="st-period-label">Periode</span>
        <span class="st-period-value">{{ fromLabel }} → {{ toLabel }}</span>
      </div>
    </div>
    <div class="mt-4">
      <div class="st-track">
        <div
          class="st-bar"
          :style="{
            width: `${clamped}%`,
            background: tone.bar,
            boxShadow: tone.glow,
          }"
        />
        <div class="st-goal" aria-hidden="true"></div>
        <div class="st-glow" aria-hidden="true"></div>
      </div>
      <div class="st-scale">
        <span>0%</span>
        <span>objectif 50%</span>
        <span>100%</span>
      </div>
      <div class="objective-chip st-objective">
        <span class="objective-label">Objectif</span>
        <span class="objective-value">>= 50%</span>
        <span class="objective-note">stock qui tourne vite</span>
      </div>
    </div>
  </KpiCard>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import StatsServices from '@/services/StatsServices'
import { normalizeKpi, normalizeSeries } from '@/services/statsAdapters'
import { formatDateFR, formatPct } from '@/utils/formatters'
import KpiCard from './_parts/KpiCard.vue'

const props = defineProps({
  from: String,
  to: String,
  bucket: { type: String, default: 'week' },
  categories: { type: Array, default: () => [] },
  types: { type: Array, default: () => [] },
  widgetWidth: { type: Number, default: 520 },
  widgetHeight: { type: Number, default: 260 },
  widgetBaseWidth: { type: Number, default: 520 },
  widgetBaseHeight: { type: Number, default: 260 },
})
const accent = '#38BDF8'
const target = 50

const loading = ref(false)
const error = ref('')
const kpi = ref({ value: 0, deltaPct: null }) // value = %
const series = ref([])
let req = 0

async function load() {
  const id = ++req
  loading.value = true
  error.value = ''
  try {
    const [k, s] = await Promise.all([
      StatsServices.kpi('sellThrough', props.from, props.to, props.categories, props.types),
      StatsServices.series('sellThrough', props.from, props.to, props.bucket, props.categories, props.types),
    ])
    if (id !== req) return
    kpi.value = normalizeKpi(k.data)
    series.value = normalizeSeries(s.data)
  } catch (e) {
    if (id !== req) return
    error.value = e?.response?.data?.message ?? e?.message ?? 'Impossible de charger'
  } finally {
    if (id === req) loading.value = false
  }
}

onMounted(load)
watch(() => [props.from, props.to, props.bucket, props.categories, props.types], load)

const valueText = computed(() => formatPct(kpi.value.value, { digits: 1 }))
const spark = computed(() => series.value.slice(-18).map((p) => p.value))
const clamped = computed(() => Math.max(0, Math.min(100, Number(kpi.value.value ?? 0))))
const status = computed(() => {
  const v = clamped.value
  if (v >= target) return { label: 'bon', tone: 'is-good' }
  if (v >= 30) return { label: 'moyen', tone: 'is-mid' }
  return { label: 'faible', tone: 'is-low' }
})
const fromLabel = computed(() =>
  formatDateFR(props.from, { day: '2-digit', month: 'short', year: 'numeric' }),
)
const toLabel = computed(() =>
  formatDateFR(props.to, { day: '2-digit', month: 'short', year: 'numeric' }),
)
const tone = computed(() => {
  const v = clamped.value
  if (v >= 50) {
    return {
      bar: 'linear-gradient(90deg, #22C55E, #16A34A)',
      glow: '0 0 14px rgba(34, 197, 94, 0.4)',
    }
  }
  if (v >= 20) {
    return {
      bar: 'linear-gradient(90deg, #F59E0B, #F97316)',
      glow: '0 0 14px rgba(245, 158, 11, 0.35)',
    }
  }
  return {
    bar: 'linear-gradient(90deg, #EF4444, #F97316)',
    glow: '0 0 14px rgba(239, 68, 68, 0.35)',
  }
})
</script>

<style scoped>
.st-track {
  position: relative;
  height: 12px;
  border-radius: 999px;
  background: linear-gradient(180deg, rgba(226, 232, 240, 0.92), rgba(203, 213, 225, 0.86));
  overflow: hidden;
  border: 1px solid rgba(148, 163, 184, 0.26);
}
.st-bar {
  height: 100%;
  border-radius: 999px;
  transition: width 320ms ease;
  position: relative;
}
.st-bar::after {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.35), transparent 60%);
  opacity: 0.6;
}
.st-goal {
  position: absolute;
  left: 50%;
  top: 2px;
  bottom: 2px;
  width: 2px;
  border-radius: 999px;
  background: rgba(71, 85, 105, 0.62);
}
.st-glow {
  position: absolute;
  inset: -12px;
  background: radial-gradient(circle at 20% 50%, rgba(59, 130, 246, 0.12), transparent 60%),
    radial-gradient(circle at 80% 50%, rgba(168, 85, 247, 0.1), transparent 60%);
  pointer-events: none;
}
.st-status {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.st-scale {
  margin-top: 4px;
  display: flex;
  justify-content: space-between;
  color: #64748b;
  font-size: 10px;
}
.st-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 6px;
}
.st-period {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 10px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: #64748b;
  white-space: nowrap;
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(248, 250, 252, 0.82);
  border: 1px solid rgba(148, 163, 184, 0.26);
}
.st-period-label {
  opacity: 0.7;
}
.st-period-value {
  font-size: 11px;
  letter-spacing: 0.02em;
  text-transform: none;
  color: #334155;
}
.st-status-label {
  font-size: 11px;
  color: #64748b;
  letter-spacing: 0.04em;
  text-transform: uppercase;
}
.st-status-pill {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 999px;
  border: 1px solid transparent;
  text-transform: uppercase;
  letter-spacing: 0.08em;
}
.st-status-pill.is-low {
  color: #be123c;
  background: rgba(255, 228, 230, 0.72);
  border-color: rgba(244, 63, 94, 0.24);
}
.st-status-pill.is-mid {
  color: #b45309;
  background: rgba(254, 243, 199, 0.78);
  border-color: rgba(245, 158, 11, 0.24);
}
.st-status-pill.is-good {
  color: #047857;
  background: rgba(209, 250, 229, 0.68);
  border-color: rgba(16, 185, 129, 0.24);
}
.st-goal-copy {
  margin-top: 6px;
  font-size: 11px;
  color: #64748b;
  letter-spacing: 0.02em;
  display: inline-flex;
  align-items: center;
  gap: 6px;
}
.st-goal-copy::before {
  content: '';
  width: 8px;
  height: 8px;
  border-radius: 999px;
  background: linear-gradient(90deg, #22C55E, #16A34A);
  box-shadow: 0 0 8px rgba(34, 197, 94, 0.35);
}
.objective-chip {
  margin-top: 6px;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 5px 10px;
  border-radius: 999px;
  font-size: 10px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: #64748b;
  border: 1px solid rgba(148, 163, 184, 0.26);
  background: rgba(248, 250, 252, 0.82);
}
.objective-label {
  opacity: 0.7;
}
.objective-value {
  font-size: 11px;
  text-transform: none;
  letter-spacing: 0.02em;
  color: #111827;
}
.objective-note {
  font-size: 10px;
  letter-spacing: 0.04em;
  text-transform: none;
  color: #64748b;
}
</style>
