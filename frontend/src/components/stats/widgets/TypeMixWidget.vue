<template>
  <WidgetCard
    title="Mix par type"
    :subtitle="subtitle"
    :accent="accent"
    :loading="loading"
    :error="error"
  >
    <div class="tm-head">
      <div class="tm-chip">
        <span class="tm-chip-label">Mesure</span>
        <span class="tm-chip-value">{{ metricLabel }}</span>
      </div>
      <div class="tm-chip">
        <span class="tm-chip-label">Periode</span>
        <span class="tm-chip-value">{{ fromLabel }} -> {{ toLabel }}</span>
      </div>
    </div>

    <div v-if="view === 'pie'" class="tm-split">
      <div class="tm-legend">
        <div v-if="!rows.length" class="tm-empty">Aucune donnee sur la periode.</div>
        <div v-else class="tm-list">
          <div v-for="row in rows" :key="row.label" class="tm-row">
            <span class="tm-dot" :style="{ background: row.color }"></span>
            <span class="tm-name">{{ row.prettyLabel }}</span>
            <span class="tm-metric">
              <span class="tm-pct">{{ row.pct }}%</span>
              <span class="tm-val">{{ row.valueText }}</span>
            </span>
          </div>
        </div>
      </div>
      <VChart class="tm-chart" :style="chartStyle" :option="option" autoresize />
    </div>

    <template v-else>
      <VChart class="tm-chart" :style="chartStyle" :option="option" autoresize />
      <div class="tm-legend">
        <div v-if="!rows.length" class="tm-empty">Aucune donnee sur la periode.</div>
        <div v-else class="tm-list">
          <div v-for="row in rows" :key="row.label" class="tm-row">
            <span class="tm-dot" :style="{ background: row.color }"></span>
            <span class="tm-name">{{ row.prettyLabel }}</span>
            <span class="tm-metric">
              <span class="tm-pct">{{ row.pct }}%</span>
              <span class="tm-val">{{ row.valueText }}</span>
            </span>
          </div>
        </div>
      </div>
    </template>
  </WidgetCard>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import StatsServices from '@/services/StatsServices'
import { normalizeBreakdown } from '@/services/statsAdapters'
import { formatDateFR, formatEUR, formatNumber } from '@/utils/formatters'
import WidgetCard from './_parts/WidgetCard.vue'

const props = defineProps({
  from: String,
  to: String,
  categories: { type: Array, default: () => [] },
  types: { type: Array, default: () => [] },
  metric: { type: String, default: 'profit' },
  view: { type: String, default: 'pie' },
})

const accent = '#F59E0B'
const loading = ref(false)
const error = ref('')
const items = ref([])
let req = 0

const metricConfig = {
  profit: {
    endpoint: 'typeProfit',
    subtitle: 'Profit realise par type',
    label: 'Profit',
    format: (v) => formatEUR(v, { compact: true }),
    tooltip: (v) => formatEUR(v),
  },
  sold: {
    endpoint: 'typeSoldCount',
    subtitle: 'Ventes par type',
    label: 'Ventes',
    format: (v) => formatNumber(v, { compact: true }),
    tooltip: (v) => formatNumber(v),
  },
  stock: {
    endpoint: 'typeStockCount',
    subtitle: 'Stock actuel par type',
    label: 'Stock',
    format: (v) => formatNumber(v, { compact: true }),
    tooltip: (v) => formatNumber(v),
  },
}

const TYPE_LABELS = {
  SNEAKER: 'Sneakers',
  POKEMON_CARD: 'Cartes',
  TICKET: 'Tickets',
  OTHER: 'Autres',
}

async function load() {
  const id = ++req
  loading.value = true
  error.value = ''
  try {
    const cfg = metricConfig[props.metric] ?? metricConfig.profit
    const { data } = await StatsServices.breakdown(cfg.endpoint, props.from, props.to, props.categories, props.types)
    if (id !== req) return
    items.value = normalizeBreakdown(data)
  } catch (e) {
    if (id !== req) return
    error.value = e?.response?.data?.message ?? e?.message ?? 'Impossible de charger'
  } finally {
    if (id === req) loading.value = false
  }
}

onMounted(load)
watch(() => [props.from, props.to, props.categories, props.types, props.metric, props.view], load)

const palette = ['#F59E0B', '#F97316', '#22C55E', '#3B82F6', '#A855F7', '#EC4899']
const pickColor = (index) => palette[index % palette.length]

const total = computed(() => items.value.reduce((acc, item) => acc + Number(item.value ?? 0), 0))
const subtitle = computed(() => (metricConfig[props.metric] ?? metricConfig.profit).subtitle)
const metricLabel = computed(() => (metricConfig[props.metric] ?? metricConfig.profit).label)

const rows = computed(() =>
  items.value.map((item, index) => {
    const value = Number(item.value ?? 0)
    const pct = total.value > 0 ? Math.round((value / total.value) * 100) : 0
    const cfg = metricConfig[props.metric] ?? metricConfig.profit
    return {
      label: item.label,
      prettyLabel: TYPE_LABELS[item.label] ?? item.label,
      value,
      pct,
      color: pickColor(index),
      valueText: cfg.format(value),
    }
  }),
)

const chartStyle = computed(() => ({ height: props.view === 'pie' ? '320px' : '280px' }))

const option = computed(() => {
  const cfg = metricConfig[props.metric] ?? metricConfig.profit
  const labels = rows.value.map((row) => row.prettyLabel)
  const values = rows.value.map((row) => row.value)

  if (props.view === 'treemap') {
    return {
      backgroundColor: 'transparent',
      tooltip: {
        trigger: 'item',
        confine: true,
        transitionDuration: 0,
        formatter: ({ name, value }) => `${name}<br/>${cfg.tooltip(value)}`,
      },
      series: [
        {
          type: 'treemap',
          roam: false,
          nodeClick: false,
          breadcrumb: { show: false },
          label: { show: true, formatter: '{b}\n{c}' },
          upperLabel: { show: false },
          itemStyle: { borderColor: 'rgba(2,6,23,0.6)', borderWidth: 2, gapWidth: 2 },
          data: rows.value.map((row) => ({
            name: row.prettyLabel,
            value: row.value,
            itemStyle: { color: row.color },
          })),
        },
      ],
    }
  }

  if (props.view === 'bars') {
    return {
      backgroundColor: 'transparent',
      tooltip: {
        trigger: 'axis',
        valueFormatter: (v) => cfg.tooltip(v),
      },
      grid: { left: 92, right: 20, top: 10, bottom: 8, containLabel: true },
      xAxis: {
        type: 'value',
        axisLabel: { color: '#9CA3AF', fontSize: 10 },
        splitLine: { lineStyle: { color: 'rgba(148,163,184,0.15)' } },
      },
      yAxis: {
        type: 'category',
        data: labels,
        axisLabel: { color: '#E5E7EB', fontSize: 11 },
        axisTick: { show: false },
        axisLine: { lineStyle: { color: '#334155' } },
      },
      series: [
        {
          type: 'bar',
          data: values.map((value, index) => ({
            value,
            itemStyle: { color: rows.value[index]?.color, borderRadius: [8, 8, 8, 8] },
          })),
          barWidth: 16,
          label: {
            show: true,
            position: 'right',
            color: '#E2E8F0',
            fontSize: 11,
            formatter: (p) => cfg.format(p.value),
          },
        },
      ],
    }
  }

  return {
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'item',
      confine: true,
      transitionDuration: 0,
      formatter: ({ name, value, percent }) => `${name}<br/>${cfg.tooltip(value)} • ${percent}%`,
    },
    series: [
      {
        type: 'pie',
        radius: ['45%', '78%'],
        center: ['50%', '50%'],
        label: { show: false },
        labelLine: { show: false },
        animationDurationUpdate: 0,
        emphasis: { scale: false },
        data: rows.value.map((row) => ({
          name: row.prettyLabel,
          value: row.value,
          itemStyle: { color: row.color },
        })),
      },
    ],
  }
})

const fromLabel = computed(() =>
  formatDateFR(props.from, { day: '2-digit', month: 'short', year: 'numeric' }),
)
const toLabel = computed(() =>
  formatDateFR(props.to, { day: '2-digit', month: 'short', year: 'numeric' }),
)
</script>

<style scoped>
.tm-head {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: space-between;
  margin-bottom: 10px;
}
.tm-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(15, 23, 42, 0.65);
  border: 1px solid rgba(255, 255, 255, 0.12);
  color: rgba(226, 232, 240, 0.78);
  font-size: 10px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}
.tm-chip-value {
  color: rgba(226, 232, 240, 0.95);
  font-size: 11px;
  letter-spacing: 0.02em;
  text-transform: none;
}
.tm-split {
  display: grid;
  grid-template-columns: minmax(180px, 1fr) minmax(220px, 1.1fr);
  gap: 14px;
  align-items: center;
}
.tm-chart {
  width: 100%;
}
.tm-legend {
  margin-top: 12px;
}
.tm-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.tm-row {
  display: grid;
  grid-template-columns: 12px 1fr auto;
  gap: 8px;
  align-items: center;
  padding: 4px 8px;
  border-radius: 12px;
  background: rgba(15, 23, 42, 0.45);
  border: 1px solid rgba(255, 255, 255, 0.06);
}
.tm-dot {
  width: 8px;
  height: 8px;
  border-radius: 999px;
}
.tm-name {
  font-size: 12px;
  color: rgba(226, 232, 240, 0.92);
}
.tm-metric {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}
.tm-pct {
  font-size: 11px;
  color: rgba(226, 232, 240, 0.58);
}
.tm-val {
  font-size: 12px;
  color: rgba(226, 232, 240, 0.86);
}
.tm-empty {
  font-size: 12px;
  color: rgba(226, 232, 240, 0.5);
}

@media (max-width: 900px) {
  .tm-split {
    grid-template-columns: 1fr;
  }
}
</style>
