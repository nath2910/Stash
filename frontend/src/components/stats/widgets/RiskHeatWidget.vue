<template>
  <WidgetCard
    title="Risk heat"
    subtitle="Concentration des risques"
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
      <div class="risk-legend">
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

const yLabels = ['Aging', 'Margin', 'Cash pressure']

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

const option = computed(() => ({
  backgroundColor: 'transparent',
  grid: { left: 72, right: 10, top: 12, bottom: 34 },
  xAxis: {
    type: 'category',
    data: xLabels.value,
    axisLine: { lineStyle: { color: 'rgba(148, 163, 184, 0.36)' } },
    axisLabel: { color: '#64748b', fontSize: 10, interval: 0 },
    axisTick: { show: false },
  },
  yAxis: {
    type: 'category',
    data: yLabels,
    axisLine: { show: false },
    axisTick: { show: false },
    axisLabel: { color: '#64748b', fontSize: 10 },
  },
  visualMap: {
    min: 0,
    max: 100,
    orient: 'horizontal',
    left: 'center',
    bottom: 2,
    itemWidth: 90,
    itemHeight: 10,
    text: ['Eleve', 'Faible'],
    textStyle: { color: '#64748b', fontSize: 10 },
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
      return `${yLabels[params?.value?.[1] ?? 0]} - ${xLabels.value[params?.value?.[0] ?? 0]}<br/>Risque: ${value}`
    },
  },
  series: [
    {
      type: 'heatmap',
      data: heatValues.value,
      label: {
        show: true,
        fontSize: 10,
        color: '#ffffff',
        formatter: ({ value }) => `${value[2]}`,
      },
      itemStyle: {
        borderColor: 'rgba(248, 250, 252, 0.86)',
        borderWidth: 1.4,
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
}))
</script>

<style scoped>
.risk-wrap {
  height: 100%;
  display: grid;
  grid-template-rows: minmax(0, 1fr) auto;
  gap: 8px;
}

.risk-chart {
  min-height: 0;
}

.risk-legend {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  font-size: 11px;
  color: #64748b;
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
