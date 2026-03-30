<template>
  <KpiCard
    title="Taux d'ecoulement"
    subtitle="Part du stock vendu"
    :accent="accent"
    surface="kpi"
    :loading="loading"
    :error="error"
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
      <div class="mt-1 text-[10px] text-white/45 flex justify-between">
        <span>0%</span>
        <span class="text-white/70">objectif 50%</span>
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
  background: linear-gradient(180deg, rgba(30, 41, 59, 0.9), rgba(15, 23, 42, 0.9));
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.1);
  box-shadow:
    inset 0 0 0 1px rgba(255, 255, 255, 0.03),
    inset 0 6px 10px rgba(0, 0, 0, 0.35);
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
  background: rgba(255, 255, 255, 0.5);
  box-shadow: 0 0 10px rgba(255, 255, 255, 0.35);
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
  color: rgba(226, 232, 240, 0.6);
  white-space: nowrap;
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(15, 23, 42, 0.65);
  border: 1px solid rgba(255, 255, 255, 0.12);
  box-shadow:
    inset 0 0 0 1px rgba(255, 255, 255, 0.03),
    0 6px 16px rgba(0, 0, 0, 0.25);
}
.st-period-label {
  opacity: 0.7;
}
.st-period-value {
  font-size: 11px;
  letter-spacing: 0.02em;
  text-transform: none;
  color: rgba(226, 232, 240, 0.9);
}
.st-status-label {
  font-size: 11px;
  color: rgba(226, 232, 240, 0.55);
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
  color: #fca5a5;
  background: rgba(239, 68, 68, 0.12);
  border-color: rgba(239, 68, 68, 0.35);
}
.st-status-pill.is-mid {
  color: #fbbf24;
  background: rgba(245, 158, 11, 0.14);
  border-color: rgba(245, 158, 11, 0.35);
}
.st-status-pill.is-good {
  color: #86efac;
  background: rgba(34, 197, 94, 0.14);
  border-color: rgba(34, 197, 94, 0.35);
}
.st-goal-copy {
  margin-top: 6px;
  font-size: 11px;
  color: rgba(226, 232, 240, 0.68);
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
  color: rgba(226, 232, 240, 0.72);
  border: 1px solid rgba(255, 255, 255, 0.12);
  background: rgba(15, 23, 42, 0.6);
  box-shadow:
    inset 0 0 0 1px rgba(255, 255, 255, 0.02),
    0 6px 14px rgba(0, 0, 0, 0.25);
}
.objective-label {
  opacity: 0.7;
}
.objective-value {
  font-size: 11px;
  text-transform: none;
  letter-spacing: 0.02em;
  color: rgba(226, 232, 240, 0.92);
}
.objective-note {
  font-size: 10px;
  letter-spacing: 0.04em;
  text-transform: none;
  color: rgba(226, 232, 240, 0.55);
}
</style>
