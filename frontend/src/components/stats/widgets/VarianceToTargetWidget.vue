<template>
  <KpiCard
    title="Variance cible"
    subtitle="Actuel vs objectif"
    :accent="accent"
    surface="kpi"
    :loading="loading"
    :error="error"
    :widget-width="props.widgetWidth"
    :widget-height="props.widgetHeight"
    :widget-base-width="props.widgetBaseWidth"
    :widget-base-height="props.widgetBaseHeight"
    :valueText="varianceText"
    :deltaPct="kpi.deltaPct"
    :deltaText="deltaText"
    :hint="hintText"
    :spark="spark"
  >
    <div class="variance-grid">
      <div class="variance-cell">
        <span>Actuel</span>
        <strong>{{ currentText }}</strong>
      </div>
      <div class="variance-cell">
        <span>Objectif</span>
        <strong>{{ targetText }}</strong>
      </div>
      <div class="variance-cell">
        <span>Statut</span>
        <strong :class="statusClass">{{ statusLabel }}</strong>
      </div>
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
  metric: { type: String, default: 'grossRevenue' },
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

const accent = '#0ea5e9'
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

function formatMetric(value) {
  if (String(props.unit).toLowerCase() === '%') {
    return formatPct(value, { digits: 1 })
  }
  return formatEUR(value, { compact: true, digits: 1 })
}

const target = computed(() => Number(props.target || 0))
const variance = computed(() => Number(kpi.value.value || 0) - target.value)
const variancePct = computed(() => {
  if (!target.value) return 0
  return (variance.value / target.value) * 100
})
const varianceText = computed(() => signFmt(variancePct.value))
const currentText = computed(() => formatMetric(Number(kpi.value.value || 0)))
const targetText = computed(() => formatMetric(target.value))
const deltaText = computed(() => (kpi.value.deltaPct == null ? '' : signFmt(kpi.value.deltaPct)))
const hintText = computed(() => `${currentText.value} vs ${targetText.value}`)
const spark = computed(() => series.value.slice(-18).map((entry) => entry.value))
const statusLabel = computed(() => {
  if (variancePct.value >= 0) return 'Au-dessus'
  if (variancePct.value > -10) return 'Quasi cible'
  return 'Sous cible'
})
const statusClass = computed(() => {
  if (variancePct.value >= 0) return 'is-good'
  if (variancePct.value > -10) return 'is-mid'
  return 'is-low'
})
</script>

<style scoped>
.variance-grid {
  margin-top: 8px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}

.variance-cell {
  border: 1px solid rgba(148, 163, 184, 0.22);
  border-radius: 10px;
  padding: 8px;
  background: rgba(15, 23, 42, 0.55);
  display: grid;
  gap: 2px;
}

.variance-cell span {
  font-size: 10px;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  color: rgba(203, 213, 225, 0.68);
}

.variance-cell strong {
  font-size: 13px;
  color: rgba(248, 250, 252, 0.96);
}

.variance-cell strong.is-good {
  color: #86efac;
}

.variance-cell strong.is-mid {
  color: #fde047;
}

.variance-cell strong.is-low {
  color: #fda4af;
}
</style>

