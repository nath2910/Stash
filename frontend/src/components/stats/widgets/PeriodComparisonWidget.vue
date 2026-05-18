<template>
  <WidgetCard
    title="Comparaison periode"
    subtitle="Actuel vs precedent"
    :accent="accent"
    surface="trend"
    :loading="loading"
    :error="error"
    :widget-width="props.widgetWidth"
    :widget-height="props.widgetHeight"
    :widget-base-width="props.widgetBaseWidth"
    :widget-base-height="props.widgetBaseHeight"
  >
    <div class="comparison-grid">
      <VChart class="comparison-chart" :option="option" autoresize />
      <div class="comparison-side">
        <div class="comparison-kpi">
          <span>Periode actuelle</span>
          <strong>{{ currentText }}</strong>
        </div>
        <div class="comparison-kpi">
          <span>Periode precedente</span>
          <strong>{{ previousText }}</strong>
        </div>
        <div class="comparison-kpi">
          <span>Delta</span>
          <strong :class="deltaClass">{{ deltaText }}</strong>
        </div>
      </div>
    </div>
  </WidgetCard>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import StatsServices from '@/services/StatsServices'
import { normalizeKpi, prevPeriod } from '@/services/statsAdapters'
import { formatEUR, formatPct, signFmt } from '@/utils/formatters'
import WidgetCard from './_parts/WidgetCard.vue'

const props = defineProps({
  from: String,
  to: String,
  metric: { type: String, default: 'netProfit' },
  bucket: { type: String, default: 'week' },
  categories: { type: Array, default: () => [] },
  types: { type: Array, default: () => [] },
  widgetWidth: { type: Number, default: 980 },
  widgetHeight: { type: Number, default: 420 },
  widgetBaseWidth: { type: Number, default: 980 },
  widgetBaseHeight: { type: Number, default: 420 },
})

const accent = '#38bdf8'
const loading = ref(false)
const error = ref('')
const currentKpi = ref({ value: 0, deltaPct: null })
const previousKpi = ref({ value: 0, deltaPct: null })
let requestSeq = 0

async function load() {
  if (!props.from || !props.to) return
  const seq = ++requestSeq
  loading.value = true
  error.value = ''
  try {
    const prev = prevPeriod(props.from, props.to)
    const [currentRes, previousRes] = await Promise.all([
      StatsServices.kpi(props.metric, props.from, props.to, props.categories, props.types),
      StatsServices.kpi(props.metric, prev.from, prev.to, props.categories, props.types),
    ])
    if (seq !== requestSeq) return
    currentKpi.value = normalizeKpi(currentRes.data)
    previousKpi.value = normalizeKpi(previousRes.data)
  } catch (err) {
    if (seq !== requestSeq) return
    error.value = err?.response?.data?.message ?? err?.message ?? 'Impossible de charger'
  } finally {
    if (seq === requestSeq) loading.value = false
  }
}

onMounted(load)
watch(() => [props.from, props.to, props.metric, props.bucket, props.categories, props.types], load)

const usesPercent = computed(() => ['roi', 'avgMargin', 'sellThrough'].includes(String(props.metric)))

function formatMetric(value) {
  if (usesPercent.value) return formatPct(value, { digits: 1 })
  return formatEUR(value, { compact: true, digits: 1 })
}

const currentValue = computed(() => Number(currentKpi.value.value || 0))
const previousValue = computed(() => Number(previousKpi.value.value || 0))
const deltaPct = computed(() => {
  if (!previousValue.value) return currentValue.value ? 100 : 0
  return ((currentValue.value - previousValue.value) / previousValue.value) * 100
})
const currentText = computed(() => formatMetric(currentValue.value))
const previousText = computed(() => formatMetric(previousValue.value))
const deltaText = computed(() => signFmt(deltaPct.value))
const deltaClass = computed(() => (deltaPct.value >= 0 ? 'is-good' : 'is-low'))

const option = computed(() => ({
  backgroundColor: 'transparent',
  grid: { left: 40, right: 14, top: 20, bottom: 26, containLabel: true },
  tooltip: {
    trigger: 'axis',
    confine: true,
    transitionDuration: 0,
    backgroundColor: 'rgba(255,255,255,0.98)',
    borderColor: 'rgba(148,163,184,0.32)',
    textStyle: { color: '#0f172a', fontSize: 12, fontWeight: 600 },
    extraCssText: 'border-radius:10px;box-shadow:0 12px 28px rgba(15,23,42,0.14);',
  },
  xAxis: {
    type: 'category',
    data: ['Precedent', 'Actuel'],
    axisLine: { lineStyle: { color: 'rgba(148, 163, 184, 0.32)' } },
    axisLabel: { color: '#64748b', fontSize: 11 },
    axisTick: { show: false },
  },
  yAxis: {
    type: 'value',
    axisLabel: { color: '#64748b', fontSize: 10 },
    splitLine: { lineStyle: { color: 'rgba(148, 163, 184, 0.18)' } },
  },
  series: [
    {
      type: 'bar',
      barWidth: 34,
      itemStyle: {
        color: '#cbd5e1',
        borderRadius: [8, 8, 0, 0],
      },
      data: [previousValue.value, currentValue.value],
    },
    {
      type: 'line',
      symbolSize: 8,
      smooth: true,
      lineStyle: { color: accent, width: 2 },
      itemStyle: { color: accent },
      data: [previousValue.value, currentValue.value],
    },
  ],
}))
</script>

<style scoped>
.comparison-grid {
  height: 100%;
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 0;
}

.comparison-chart {
  width: 100%;
  height: 100%;
  min-height: 0;
}

.comparison-side {
  display: none;
}

.comparison-kpi {
  border: 1px solid rgba(148, 163, 184, 0.24);
  border-radius: 8px;
  background: rgba(248, 250, 252, 0.82);
  padding: 10px;
  display: grid;
  gap: 4px;
}

.comparison-kpi span {
  font-size: 10px;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  color: #64748b;
}

.comparison-kpi strong {
  font-size: 16px;
  color: #111827;
}

.comparison-kpi strong.is-good {
  color: #047857;
}

.comparison-kpi strong.is-low {
  color: #be123c;
}
</style>
