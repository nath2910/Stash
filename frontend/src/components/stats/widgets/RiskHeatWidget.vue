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
    <div class="risk-wrap">
      <VChart class="risk-chart" :option="option" autoresize />
      <div v-if="showLegend" class="risk-legend">
        <span><i class="dot low"></i>Faible</span>
        <span><i class="dot mid"></i>Moyen</span>
        <span><i class="dot high"></i>Eleve</span>
      </div>
    </div>
  </WidgetCard>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import StatsServices from '@/services/StatsServices'
import { normalizeBreakdown, normalizeRank } from '@/services/statsAdapters'
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
let requestSeq = 0

async function load() {
  const seq = ++requestSeq
  loading.value = true
  error.value = ''
  try {
    const [agingRes, profitRes] = await Promise.all([
      StatsServices.breakdown('stockAging', props.from, props.to, props.categories, props.types),
      StatsServices.rank('topCategoriesProfit', props.from, props.to, 5, props.categories, props.types),
    ])
    if (seq !== requestSeq) return
    stockAging.value = normalizeBreakdown(agingRes.data)
    topProfit.value = normalizeRank(profitRes.data)
  } catch (err) {
    if (seq !== requestSeq) return
    error.value = err?.response?.data?.message ?? err?.message ?? 'Impossible de charger'
  } finally {
    if (seq === requestSeq) loading.value = false
  }
}

onMounted(load)
watch(() => [props.from, props.to, props.categories, props.types], load)

const xLabels = computed(() => {
  const labels = topProfit.value.map((entry) => entry.label).slice(0, 5)
  if (!labels.length) return ['Segment A', 'Segment B', 'Segment C']
  return labels
})

const yLabels = ['Anciennete stock', 'Marge fragile', 'Cash immobilise']

const heatValues = computed(() => {
  const riskFromAging = stockAging.value.reduce((sum, entry) => sum + Number(entry.value || 0), 0)
  const baseline = Math.max(riskFromAging, 1)
  return xLabels.value.flatMap((label, xIndex) => {
    const profit = topProfit.value.find((entry) => entry.label === label)?.value ?? 0
    const agingScore = Math.min(100, Math.round((baseline / (profit + baseline)) * 100))
    const marginScore = Math.min(100, Math.max(8, 100 - Math.round(profit / 220)))
    const cashScore = Math.min(100, 48 + xIndex * 10)
    return [
      [xIndex, 0, agingScore],
      [xIndex, 1, marginScore],
      [xIndex, 2, cashScore],
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
        return `${metric} - ${segment}<br/>Niveau de risque: ${value}/100`
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
