<template>
  <WidgetCard
    title="Mix par type"
    :subtitle="subtitle"
    :accent="accent"
    surface="distribution"
    :loading="loading"
    :error="error"
    :widget-width="props.widgetWidth"
    :widget-height="props.widgetHeight"
    :widget-base-width="props.widgetBaseWidth"
    :widget-base-height="props.widgetBaseHeight"
  >
    <div class="tm-root" :class="{ 'is-compact': compactMode, 'is-dense': denseMode }">
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

      <div v-else class="tm-stack">
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
      </div>
    </div>
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
  widgetWidth: { type: Number, default: 720 },
  widgetHeight: { type: Number, default: 420 },
  widgetBaseWidth: { type: Number, default: 0 },
  widgetBaseHeight: { type: Number, default: 0 },
  widgetRenderWidth: { type: Number, default: 0 },
  widgetRenderHeight: { type: Number, default: 0 },
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

const clamp = (value, min, max) => Math.max(min, Math.min(max, value))

async function load() {
  const id = ++req
  loading.value = true
  error.value = ''
  try {
    const cfg = metricConfig[props.metric] ?? metricConfig.profit
    const { data } = await StatsServices.breakdown(
      cfg.endpoint,
      props.from,
      props.to,
      props.categories,
      props.types,
    )
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

const layoutWidth = computed(() => {
  return Math.max(Number(props.widgetWidth ?? 0), 1)
})
const layoutHeight = computed(() => {
  return Math.max(Number(props.widgetHeight ?? 0), 1)
})
const compactMode = computed(() => layoutWidth.value < 700 || layoutHeight.value < 360)
const denseMode = computed(() => layoutWidth.value < 560 || layoutHeight.value < 300)
const axisFont = computed(() =>
  clamp(Math.round(Math.min(layoutWidth.value * 0.014, layoutHeight.value * 0.034)), 9, 12),
)
const valueLabelVisible = computed(() => layoutWidth.value >= 420 && layoutHeight.value >= 200)

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

const longestLabel = computed(() =>
  rows.value.reduce((max, row) => Math.max(max, String(row.prettyLabel ?? '').length), 0),
)

const barGrid = computed(() => ({
  left: clamp(
    Math.round(longestLabel.value * axisFont.value * 0.54 + 28),
    66,
    layoutWidth.value < 560 ? 118 : 160,
  ),
  right: valueLabelVisible.value ? clamp(Math.round(layoutWidth.value * 0.08), 34, 76) : 14,
  top: clamp(Math.round(layoutHeight.value * 0.03), 8, 18),
  bottom: clamp(Math.round(layoutHeight.value * 0.04), 10, 24),
  containLabel: false,
}))

function formatTileLabel(label) {
  const text = String(label ?? '').trim()
  const limit = denseMode.value ? 9 : 14
  if (text.length <= limit) return text
  return `${text.slice(0, Math.max(1, limit - 1))}...`
}

const chartHeight = computed(() => {
  const reserved = props.view === 'pie' ? 96 : 156
  const available = Math.max(layoutHeight.value - reserved, 120)
  if (props.view === 'pie') {
    return clamp(Math.min(available, layoutWidth.value * 0.5), 150, 320)
  }
  if (props.view === 'treemap') {
    return clamp(available, 140, 280)
  }
  return clamp(available, 120, 270)
})
const chartStyle = computed(() => ({ height: '100%', minHeight: '0px' }))

const option = computed(() => {
  const cfg = metricConfig[props.metric] ?? metricConfig.profit
  const labels = rows.value.map((row) => row.prettyLabel)
  const values = rows.value.map((row) => row.value)

  if (props.view === 'treemap') {
    const labelFont = clamp(
      Math.round(Math.min(layoutWidth.value * 0.013, layoutHeight.value * 0.032)),
      9,
      13,
    )
    return {
      backgroundColor: 'transparent',
      tooltip: {
        trigger: 'item',
        confine: true,
        transitionDuration: 0,
        backgroundColor: 'rgba(255,255,255,0.98)',
        borderColor: 'rgba(148,163,184,0.32)',
        textStyle: { color: '#0f172a', fontSize: 12, fontWeight: 600 },
        extraCssText: 'border-radius:10px;box-shadow:0 12px 28px rgba(15,23,42,0.14);',
        formatter: ({ name, value }) => `${name}<br/>${cfg.tooltip(value)}`,
      },
      series: [
        {
          type: 'treemap',
          left: 0,
          right: 0,
          top: 0,
          bottom: 0,
          width: '100%',
          height: '100%',
          roam: false,
          nodeClick: false,
          breadcrumb: { show: false },
          squareRatio: layoutWidth.value >= layoutHeight.value ? 1.55 : 0.95,
          label: {
            show: true,
            color: '#ffffff',
            fontSize: labelFont,
            fontWeight: 780,
            lineHeight: Math.round(labelFont * 1.16),
            overflow: 'truncate',
            formatter: ({ name, value }) => `${formatTileLabel(name)}\n${cfg.format(value)}`,
          },
          upperLabel: { show: false },
          itemStyle: {
            borderColor: 'rgba(255,255,255,0.92)',
            borderWidth: 2,
            gapWidth: denseMode.value ? 2 : 3,
            borderRadius: 6,
          },
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
        backgroundColor: 'rgba(255,255,255,0.98)',
        borderColor: 'rgba(148,163,184,0.32)',
        textStyle: { color: '#0f172a', fontSize: 12, fontWeight: 600 },
        extraCssText: 'border-radius:10px;box-shadow:0 12px 28px rgba(15,23,42,0.14);',
      },
      grid: barGrid.value,
      xAxis: {
        type: 'value',
        axisLabel: {
          color: '#64748b',
          fontSize: axisFont.value,
          fontWeight: 650,
          formatter: (value) => cfg.format(value),
        },
        axisLine: { show: false },
        axisTick: { show: false },
        splitLine: { lineStyle: { color: 'rgba(148,163,184,0.18)' } },
      },
      yAxis: {
        type: 'category',
        data: labels,
        axisLabel: { color: '#334155', fontSize: axisFont.value, fontWeight: 700 },
        axisTick: { show: false },
        axisLine: { show: false },
      },
      series: [
        {
          type: 'bar',
          data: values.map((value, index) => ({
            value,
            itemStyle: { color: rows.value[index]?.color, borderRadius: [8, 8, 8, 8] },
          })),
          barWidth: clamp(
            Math.round((layoutHeight.value / Math.max(rows.value.length, 1)) * 0.18),
            denseMode.value ? 10 : 14,
            28,
          ),
          label: {
            show: valueLabelVisible.value,
            position: 'right',
            color: '#334155',
            fontSize: axisFont.value,
            fontWeight: 750,
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
      backgroundColor: 'rgba(255,255,255,0.98)',
      borderColor: 'rgba(148,163,184,0.32)',
      textStyle: { color: '#0f172a', fontSize: 12, fontWeight: 600 },
      extraCssText: 'border-radius:10px;box-shadow:0 12px 28px rgba(15,23,42,0.14);',
      formatter: ({ name, value, percent }) => `${name}<br/>${cfg.tooltip(value)} - ${percent}%`,
    },
    legend: {
      show: !denseMode.value && layoutHeight.value >= 250,
      type: 'scroll',
      bottom: 0,
      itemWidth: 8,
      itemHeight: 8,
      textStyle: { color: '#64748b', fontSize: axisFont.value, fontWeight: 650 },
      pageIconColor: '#94a3b8',
      pageTextStyle: { color: '#64748b' },
    },
    series: [
      {
        type: 'pie',
        radius: denseMode.value ? ['46%', '76%'] : ['42%', '68%'],
        center: ['50%', denseMode.value ? '50%' : '43%'],
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
.tm-root {
  height: 100%;
  min-height: 0;
  display: grid;
  grid-template-rows: minmax(0, 1fr);
  gap: 0;
}

.tm-head {
  display: none;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: space-between;
}
.tm-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(248, 250, 252, 0.82);
  border: 1px solid rgba(148, 163, 184, 0.26);
  color: #64748b;
  font-size: 10px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  max-width: 100%;
}
.tm-chip-value {
  color: #334155;
  font-size: 11px;
  letter-spacing: 0.02em;
  text-transform: none;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.tm-split {
  min-height: 0;
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 0;
  align-items: stretch;
}
.tm-stack {
  min-height: 0;
  display: grid;
  grid-template-rows: minmax(0, 1fr);
  gap: 0;
}
.tm-chart {
  width: 100%;
  height: 100%;
  min-height: 0;
}
.tm-legend {
  display: none;
}
.tm-list {
  min-height: 0;
  overflow: auto;
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding-right: 2px;
}
.tm-row {
  display: grid;
  grid-template-columns: 12px 1fr auto;
  gap: 8px;
  align-items: center;
  padding: 4px 8px;
  border-radius: 8px;
  background: rgba(248, 250, 252, 0.82);
  border: 1px solid rgba(148, 163, 184, 0.22);
}
.tm-dot {
  width: 8px;
  height: 8px;
  border-radius: 999px;
}
.tm-name {
  font-size: 12px;
  color: #111827;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.tm-metric {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}
.tm-pct {
  font-size: 11px;
  color: #64748b;
}
.tm-val {
  font-size: 12px;
  color: #334155;
}
.tm-empty {
  font-size: 12px;
  color: #64748b;
}

.is-compact .tm-split {
  grid-template-columns: 1fr;
}

.is-dense .tm-row {
  padding: 3px 7px;
}

.is-dense .tm-name,
.is-dense .tm-val {
  font-size: 11px;
}
</style>
