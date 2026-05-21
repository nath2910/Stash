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
      <div class="comparison-periods" aria-label="Periodes comparees">
        <span>{{ metricLabel }}</span>
        <strong>{{ currentPeriodText }}</strong>
        <span class="comparison-periods__sep">vs</span>
        <strong>{{ previousPeriodText }}</strong>
      </div>
      <VChart class="comparison-chart" :option="option" autoresize />
      <div class="comparison-side">
        <div class="comparison-kpi">
          <span>Periode actuelle - {{ currentPeriodText }}</span>
          <strong>{{ currentText }}</strong>
        </div>
        <div class="comparison-kpi">
          <span>Periode precedente - {{ previousPeriodText }}</span>
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
import { formatDateFR, formatEUR, formatPct, signFmt } from '@/utils/formatters'
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
const previousRange = computed(() => prevPeriod(props.from, props.to))
const metricLabel = computed(
  () =>
    ({
      grossRevenue: 'CA',
      netProfit: 'Profit',
      avgMargin: 'Marge',
      roi: 'ROI',
      sellThrough: 'Ecoulement',
    })[String(props.metric)] || 'Mesure',
)

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
const currentPeriodText = computed(() => formatPeriod(props.from, props.to))
const previousPeriodText = computed(() =>
  formatPeriod(previousRange.value.from, previousRange.value.to),
)
const currentPeriodShort = computed(() => formatPeriod(props.from, props.to, true))
const previousPeriodShort = computed(() =>
  formatPeriod(previousRange.value.from, previousRange.value.to, true),
)
const clamp = (value, min, max) => Math.max(min, Math.min(max, value))
const layoutWidth = computed(() => Math.max(Number(props.widgetWidth ?? 0), 1))
const layoutHeight = computed(() => Math.max(Number(props.widgetHeight ?? 0), 1))

const option = computed(() => {
  const axisFont = clamp(
    Math.round(Math.min(layoutWidth.value * 0.012, layoutHeight.value * 0.034)),
    9,
    12,
  )
  const labelVisible = layoutWidth.value >= 420 && layoutHeight.value >= 210

  return {
    backgroundColor: 'transparent',
    grid: {
      left: clamp(Math.round(layoutWidth.value * 0.045), 34, 68),
      right: clamp(Math.round(layoutWidth.value * 0.025), 12, 28),
      top: labelVisible ? clamp(Math.round(layoutHeight.value * 0.08), 18, 34) : 10,
      bottom: clamp(Math.round(layoutHeight.value * 0.09), 26, 44),
      containLabel: true,
    },
    tooltip: {
      trigger: 'axis',
      confine: true,
      transitionDuration: 0,
      backgroundColor: 'rgba(255,255,255,0.98)',
      borderColor: 'rgba(148,163,184,0.32)',
      textStyle: { color: '#0f172a', fontSize: 12, fontWeight: 600 },
      extraCssText: 'border-radius:10px;box-shadow:0 12px 28px rgba(15,23,42,0.14);',
      axisPointer: {
        type: 'shadow',
        shadowStyle: { color: 'rgba(56,189,248,0.12)' },
      },
      formatter: formatComparisonTooltip,
    },
    xAxis: {
      type: 'category',
      data: [previousPeriodShort.value, currentPeriodShort.value],
      axisLine: { lineStyle: { color: 'rgba(148, 163, 184, 0.32)' } },
      axisLabel: {
        color: '#64748b',
        fontSize: axisFont,
        fontWeight: 650,
        interval: 0,
        hideOverlap: true,
      },
      axisTick: { show: false },
    },
    yAxis: {
      type: 'value',
      axisLabel: {
        color: '#64748b',
        fontSize: axisFont,
        fontWeight: 650,
        formatter: (value) => formatMetric(value),
      },
      axisLine: { show: false },
      axisTick: { show: false },
      splitLine: { lineStyle: { color: 'rgba(148, 163, 184, 0.18)' } },
    },
    series: [
      {
        type: 'bar',
        barWidth: clamp(Math.round(layoutWidth.value * 0.04), 22, 44),
        data: [
          {
            value: previousValue.value,
            itemStyle: { color: '#94a3b8', borderRadius: [8, 8, 0, 0] },
          },
          {
            value: currentValue.value,
            itemStyle: { color: accent, borderRadius: [8, 8, 0, 0] },
          },
        ],
        label: {
          show: labelVisible,
          position: 'top',
          color: '#334155',
          fontSize: axisFont,
          fontWeight: 750,
          formatter: (params) => formatMetric(params.value),
        },
      },
      {
        type: 'line',
        symbolSize: clamp(Math.round(layoutWidth.value * 0.008), 6, 9),
        smooth: true,
        lineStyle: { color: accent, width: 2 },
        itemStyle: { color: accent },
        data: [previousValue.value, currentValue.value],
      },
    ],
  }
})

function formatPeriod(from, to, short = false) {
  const dateOptions = short
    ? { day: '2-digit', month: 'short', year: '2-digit' }
    : { day: '2-digit', month: 'short', year: 'numeric' }
  return `${formatDateFR(from, dateOptions)} - ${formatDateFR(to, dateOptions)}`
}

function formatComparisonTooltip(params) {
  const entry = Array.isArray(params) ? params[0] : params
  const index = Number(entry?.dataIndex ?? 0)
  const isCurrent = index === 1
  const label = isCurrent ? 'Periode actuelle' : 'Periode precedente'
  const period = isCurrent ? currentPeriodText.value : previousPeriodText.value
  const value = isCurrent ? currentText.value : previousText.value
  const delta =
    isCurrent && Number.isFinite(deltaPct.value)
      ? `<div style="margin-top:4px;color:#64748b;font-size:11px;">Ecart vs precedent: <strong style="color:${
          deltaPct.value >= 0 ? '#047857' : '#be123c'
        };">${escapeHtml(deltaText.value)}</strong></div>`
      : ''
  return `<div style="font-size:11px;color:#64748b;font-weight:800;text-transform:uppercase;letter-spacing:.06em;">${escapeHtml(label)}</div><div style="margin-top:3px;color:#334155;font-size:11px;">${escapeHtml(period)}</div><div style="margin-top:5px;color:#111827;font-size:15px;font-weight:850;">${escapeHtml(value)}</div>${delta}`
}

function escapeHtml(value) {
  return String(value ?? '').replace(/[&<>"']/g, (char) => {
    const entities = {
      '&': '&amp;',
      '<': '&lt;',
      '>': '&gt;',
      '"': '&quot;',
      "'": '&#39;',
    }
    return entities[char] || char
  })
}
</script>

<style scoped>
.comparison-grid {
  height: 100%;
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  grid-template-columns: minmax(0, 1fr);
  gap: 8px;
}

.comparison-periods {
  display: flex;
  min-width: 0;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
  color: #64748b;
  font-size: 11px;
  font-weight: 720;
  line-height: 1.2;
}

.comparison-periods strong {
  color: #111827;
  font-weight: 820;
}

.comparison-periods__sep {
  color: #94a3b8;
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
