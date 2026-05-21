<template>
  <WidgetCard
    title="Carte des risques"
    subtitle="Stock dormant, marge et cash immobilise"
    :accent="accent"
    surface="distribution"
    :loading="loading"
    :error="error"
    :widget-width="props.widgetWidth"
    :widget-height="props.widgetHeight"
    :widget-base-width="props.widgetBaseWidth"
    :widget-base-height="props.widgetBaseHeight"
  >
    <div v-if="hasRiskData" class="risk-wrap">
      <VChart class="risk-chart" :option="option" autoresize />
      <div v-if="showLegend" class="risk-legend">
        <span><i class="dot low"></i>Faible</span>
        <span><i class="dot mid"></i>Moyen</span>
        <span><i class="dot high"></i>Eleve</span>
      </div>
    </div>
    <div v-else class="risk-empty">
      <strong>Aucun risque lisible</strong>
      <span>Ajoute du stock ou des ventes categorisees pour alimenter la carte.</span>
    </div>
  </WidgetCard>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import StatsServices from '@/services/StatsServices'
import { normalizeBreakdown, normalizeRank, normalizeSummary } from '@/services/statsAdapters'
import WidgetCard from './_parts/WidgetCard.vue'

const props = defineProps({
  from: String,
  to: String,
  categories: { type: Array, default: () => [] },
  types: { type: Array, default: () => [] },
  widgetWidth: { type: Number, default: 720 },
  widgetHeight: { type: Number, default: 360 },
  widgetBaseWidth: { type: Number, default: 720 },
  widgetBaseHeight: { type: Number, default: 360 },
})

const accent = '#f43f5e'
const loading = ref(false)
const error = ref('')
const stockAging = ref([])
const topProfit = ref([])
const summary = ref(normalizeSummary({}))
let requestSeq = 0

async function load() {
  const seq = ++requestSeq
  loading.value = true
  error.value = ''
  try {
    const [agingRes, profitRes, summaryRes] = await Promise.all([
      StatsServices.breakdown('deathPileAge', props.from, props.to, props.categories, props.types),
      StatsServices.rank('topCategoriesProfit', props.from, props.to, 5, props.categories, props.types),
      StatsServices.summary(props.from, props.to, props.categories, props.types),
    ])
    if (seq !== requestSeq) return
    stockAging.value = normalizeBreakdown(agingRes.data)
    topProfit.value = normalizeRank(profitRes.data)
    summary.value = normalizeSummary(summaryRes.data)
  } catch (err) {
    if (seq !== requestSeq) return
    error.value = err?.response?.data?.message ?? err?.message ?? 'Impossible de charger'
  } finally {
    if (seq === requestSeq) loading.value = false
  }
}

onMounted(load)
watch(() => [props.from, props.to, props.categories, props.types], load)

const xEntries = computed(() => {
  const entries = topProfit.value
    .map((entry) => ({
      label: String(entry.label ?? '').trim(),
      value: Number(entry.value ?? 0),
    }))
    .filter((entry) => entry.label)
    .slice(0, 5)

  if (entries.length) return entries

  const hasGlobalSignal =
    totalStockCount.value > 0 ||
    Math.abs(Number(summary.value.profit ?? 0)) > 0 ||
    Number(summary.value.valeurStock ?? 0) > 0
  return hasGlobalSignal ? [{ label: 'Global', value: Number(summary.value.profit ?? 0) }] : []
})

const xLabels = computed(() => xEntries.value.map((entry) => entry.label))
const hasRiskData = computed(() => xEntries.value.length > 0)

const yLabels = ['Stock dormant', 'Marge fragile', 'Cash immobilise']

const totalStockCount = computed(() =>
  stockAging.value.reduce((sum, entry) => sum + Math.max(0, Number(entry.value || 0)), 0),
)

function ageBucketWeight(label, index, totalCount) {
  const text = String(label ?? '').toLowerCase()
  if (text.includes('180')) return 100
  if (text.includes('91') || text.includes('90')) return 82
  if (text.includes('31')) return 58
  if (text.includes('0-30') || text.includes('30')) return 22
  const count = Math.max(1, Number(totalCount || 1))
  return Math.round(24 + (76 * index) / count)
}

const agingRiskScore = computed(() => {
  const total = totalStockCount.value
  if (total <= 0) return Number(summary.value.valeurStock ?? 0) > 0 ? 45 : 0
  const weighted = stockAging.value.reduce((sum, entry, index) => {
    const value = Math.max(0, Number(entry.value || 0))
    return sum + value * ageBucketWeight(entry.label, index, stockAging.value.length)
  }, 0)
  return clamp(Math.round(weighted / total), 0, 100)
})

const maxPositiveProfit = computed(() =>
  Math.max(0, ...xEntries.value.map((entry) => Math.max(0, Number(entry.value || 0)))),
)

function profitRiskScore(entry) {
  const value = Number(entry?.value ?? 0)
  if (value <= 0) return 100
  const max = Math.max(maxPositiveProfit.value, value, 1)
  return clamp(Math.round(100 - (value / max) * 72), 18, 92)
}

function cashRiskScore(entry, index) {
  const stockValue = Math.max(0, Number(summary.value.valeurStock ?? 0))
  if (stockValue <= 0) return Number(entry?.value ?? 0) <= 0 ? 52 : 24
  const stockPressure = clamp(44 + Math.log10(stockValue + 1) * 9, 48, 86)
  const weakProfitPenalty = Number(entry?.value ?? 0) <= 0 ? 12 : 0
  const spreadPenalty = xEntries.value.length > 1 ? index * 3 : 0
  return clamp(Math.round(stockPressure + weakProfitPenalty + spreadPenalty), 18, 100)
}

const heatValues = computed(() => {
  return xEntries.value.flatMap((entry, xIndex) => {
    return [
      [xIndex, 0, agingRiskScore.value],
      [xIndex, 1, profitRiskScore(entry)],
      [xIndex, 2, cashRiskScore(entry, xIndex)],
    ]
  })
})
const clamp = (value, min, max) => Math.max(min, Math.min(max, value))
const layoutWidth = computed(() => Math.max(Number(props.widgetWidth ?? 0), 1))
const layoutHeight = computed(() => Math.max(Number(props.widgetHeight ?? 0), 1))
const denseMode = computed(() => layoutWidth.value < 560 || layoutHeight.value < 300)
const showLegend = computed(() => layoutWidth.value >= 520 && layoutHeight.value >= 320)

function formatAxisLabel(label) {
  const text = String(label ?? '').trim()
  const limit = denseMode.value ? 10 : 14
  if (text.length <= limit) return text
  return `${text.slice(0, Math.max(1, limit - 1))}...`
}

const option = computed(() => {
  const axisFont = clamp(
    Math.round(Math.min(layoutWidth.value * 0.013, layoutHeight.value * 0.032)),
    9,
    12,
  )
  return {
    backgroundColor: 'transparent',
    grid: {
      left: clamp(Math.round(layoutWidth.value * 0.11), 58, 92),
      right: clamp(Math.round(layoutWidth.value * 0.025), 10, 24),
      top: clamp(Math.round(layoutHeight.value * 0.05), 10, 20),
      bottom: clamp(Math.round(layoutHeight.value * 0.09), 26, 38),
      containLabel: false,
    },
    xAxis: {
      type: 'category',
      data: xLabels.value,
      axisLine: { lineStyle: { color: 'rgba(148, 163, 184, 0.28)' } },
      axisLabel: {
        color: '#64748b',
        fontSize: axisFont,
        fontWeight: 650,
        interval: 0,
        hideOverlap: true,
        formatter: (value) => formatAxisLabel(value),
      },
      axisTick: { show: false },
    },
    yAxis: {
      type: 'category',
      data: yLabels,
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: {
        color: '#64748b',
        fontSize: axisFont,
        fontWeight: 650,
        formatter: (value) => {
          const text = String(value ?? '')
          return denseMode.value ? text.split(' ')[0] : text
        },
      },
    },
    visualMap: {
      show: false,
      min: 0,
      max: 100,
      orient: 'horizontal',
      left: 'center',
      bottom: 4,
      itemWidth: clamp(Math.round(layoutWidth.value * 0.12), 64, 110),
      itemHeight: 8,
      text: ['Eleve', 'Faible'],
      textStyle: { color: '#64748b', fontSize: axisFont },
      inRange: {
        color: ['#22c55e', '#f59e0b', '#f43f5e'],
      },
      calculable: false,
    },
    tooltip: {
      position: 'top',
      confine: true,
      backgroundColor: 'rgba(255,255,255,0.98)',
      borderColor: 'rgba(148,163,184,0.32)',
      textStyle: { color: '#0f172a', fontSize: 12, fontWeight: 600 },
      extraCssText: 'border-radius:10px;box-shadow:0 12px 28px rgba(15,23,42,0.14);',
      formatter: (params) => {
        const value = params?.value?.[2] ?? 0
        const metric = yLabels[params?.value?.[1] ?? 0]
        const segment = xLabels.value[params?.value?.[0] ?? 0]
        return `${metric} - ${segment}<br/>Score estime: ${value}/100`
      },
    },
    series: [
      {
        type: 'heatmap',
        data: heatValues.value,
        label: {
          show: !denseMode.value || layoutHeight.value >= 240,
          fontSize: axisFont,
          fontWeight: 780,
          color: '#ffffff',
          formatter: ({ value }) => `${value[2]}`,
        },
        itemStyle: {
          borderColor: 'rgba(248, 250, 252, 0.9)',
          borderWidth: denseMode.value ? 1.5 : 2,
          borderRadius: 6,
        },
        emphasis: {
          itemStyle: {
            borderColor: '#111827',
            borderWidth: 2,
          },
        },
      },
    ],
  }
})
</script>

<style scoped>
.risk-wrap {
  height: 100%;
  display: grid;
  grid-template-rows: minmax(0, 1fr) auto;
  gap: 8px;
}

.risk-chart {
  width: 100%;
  height: 100%;
  min-height: 0;
}

.risk-legend {
  display: flex;
  min-width: 0;
  flex-wrap: wrap;
  align-items: center;
  justify-content: center;
  gap: 8px 12px;
  color: #64748b;
  font-size: 11px;
  font-weight: 720;
  line-height: 1;
}

.risk-empty {
  height: 100%;
  min-height: 120px;
  display: grid;
  place-content: center;
  gap: 6px;
  text-align: center;
  color: #64748b;
  padding: 12px;
}

.risk-empty strong {
  color: #111827;
  font-size: 13px;
  font-weight: 780;
}

.risk-empty span {
  max-width: 34ch;
  font-size: 12px;
  line-height: 1.35;
}

.risk-legend span {
  display: inline-flex;
  align-items: center;
  gap: 5px;
}

.dot {
  width: 8px;
  height: 8px;
  border-radius: 999px;
}

.dot.low {
  background: #22c55e;
}

.dot.mid {
  background: #f59e0b;
}

.dot.high {
  background: #f43f5e;
}
</style>
