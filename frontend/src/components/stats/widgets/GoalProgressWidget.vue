<template>
  <KpiCard
    :title="titleLabel"
    subtitle="Progression objectif"
    :accent="accent"
    surface="kpi"
    :loading="loading"
    :error="error"
    :widget-width="props.widgetWidth"
    :widget-height="props.widgetHeight"
    :widget-base-width="props.widgetBaseWidth"
    :widget-base-height="props.widgetBaseHeight"
    :valueText="progressText"
    :deltaPct="kpi.deltaPct"
    :deltaText="deltaText"
    :hint="valueHint"
    :spark="spark"
  >
    <div class="goal-track">
      <div class="goal-track__fill" :style="{ width: `${clampedProgress}%` }"></div>
      <div class="goal-track__target" :style="{ left: `${targetMarker}%` }"></div>
    </div>
    <div class="goal-meta">
      <span>Actuel: {{ currentText }}</span>
      <span>Cible: {{ targetText }}</span>
      <span>{{ statusLabel }}</span>
    </div>
  </KpiCard>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import StatsServices from '@/services/StatsServices'
import { normalizeKpi, normalizeSeries } from '@/services/statsAdapters'
import { formatEUR, formatPct, signFmt } from '@/utils/formatters'
import KpiCard from './_parts/KpiCard.vue'

const props = defineProps({
  from: String,
  to: String,
  metric: { type: String, default: 'netProfit' },
  target: { type: Number, default: 10000 },
  unit: { type: String, default: 'EUR' },
  bucket: { type: String, default: 'week' },
  categories: { type: Array, default: () => [] },
  types: { type: Array, default: () => [] },
  widgetWidth: { type: Number, default: 720 },
  widgetHeight: { type: Number, default: 340 },
  widgetBaseWidth: { type: Number, default: 720 },
  widgetBaseHeight: { type: Number, default: 340 },
})

const accent = '#22c55e'
const loading = ref(false)
const error = ref('')
const kpi = ref({ value: 0, deltaPct: null })
const series = ref([])
let requestSeq = 0

async function load() {
  const seq = ++requestSeq
  loading.value = true
  error.value = ''
  try {
    const [k, s] = await Promise.all([
      StatsServices.kpi(props.metric, props.from, props.to, props.categories, props.types),
      StatsServices.series(props.metric, props.from, props.to, props.bucket, props.categories, props.types),
    ])
    if (seq !== requestSeq) return
    kpi.value = normalizeKpi(k.data)
    series.value = normalizeSeries(s.data)
  } catch (err) {
    if (seq !== requestSeq) return
    error.value = err?.response?.data?.message ?? err?.message ?? 'Impossible de charger'
  } finally {
    if (seq === requestSeq) loading.value = false
  }
}

onMounted(load)
watch(() => [props.from, props.to, props.metric, props.target, props.bucket, props.categories, props.types], load)

const metricLabelMap = {
  netProfit: 'Objectif profit',
  grossRevenue: 'Objectif revenu',
  cashAvailable: 'Objectif cash',
  avgMargin: 'Objectif marge',
  roi: 'Objectif ROI',
  sellThrough: 'Objectif ecoulement',
}

const targetValue = computed(() => Math.max(Number(props.target || 0), 0.0001))
const progress = computed(() => (Number(kpi.value.value || 0) / targetValue.value) * 100)
const clampedProgress = computed(() => Math.max(0, Math.min(progress.value, 100)))
const targetMarker = computed(() => 100)
const spark = computed(() => series.value.slice(-18).map((entry) => entry.value))

function formatValue(value) {
  if (String(props.unit).toLowerCase() === '%') {
    return formatPct(value, { digits: 1 })
  }
  return formatEUR(value, { compact: true, digits: 1 })
}

const titleLabel = computed(() => metricLabelMap[props.metric] || 'Objectif')
const progressText = computed(() => `${Math.round(progress.value)}%`)
const currentText = computed(() => formatValue(Number(kpi.value.value || 0)))
const targetText = computed(() => formatValue(targetValue.value))
const deltaText = computed(() => (kpi.value.deltaPct == null ? '' : signFmt(kpi.value.deltaPct)))
const valueHint = computed(() => `${currentText.value} / ${targetText.value}`)
const statusLabel = computed(() => {
  if (progress.value >= 100) return 'Cible atteinte'
  if (progress.value >= 75) return 'Bonne trajectoire'
  if (progress.value >= 45) return 'Sous surveillance'
  return 'En retard'
})
</script>

<style scoped>
.goal-track {
  position: relative;
  height: 12px;
  border-radius: 999px;
  overflow: hidden;
  background: rgba(15, 23, 42, 0.72);
  border: 1px solid rgba(255, 255, 255, 0.12);
}

.goal-track__fill {
  height: 100%;
  background: linear-gradient(90deg, #22c55e, #14b8a6);
  transition: width 260ms ease;
}

.goal-track__target {
  position: absolute;
  top: 1px;
  bottom: 1px;
  width: 2px;
  background: rgba(255, 255, 255, 0.86);
  transform: translateX(-1px);
}

.goal-meta {
  margin-top: 8px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  font-size: 11px;
  color: rgba(226, 232, 240, 0.8);
}
</style>

