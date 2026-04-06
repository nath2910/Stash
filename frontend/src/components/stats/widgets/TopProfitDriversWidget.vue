<template>
  <WidgetCard
    title="Top profit"
    subtitle="Categories"
    :accent="accent"
    surface="distribution"
    :loading="loading"
    :error="error"
    :widget-width="props.widgetWidth"
    :widget-height="props.widgetHeight"
    :widget-base-width="props.widgetBaseWidth"
    :widget-base-height="props.widgetBaseHeight"
  >
    <div
      class="tp-root"
      :class="{ 'is-compact': compactMode, 'is-dense': denseMode, 'is-pie': props.view === 'pie' }"
    >
      <div class="tp-header">
        <div class="tp-tabs">
          <span class="tp-pill tp-pill--static">Categories</span>
        </div>
        <div class="period-chip">
          <span class="period-label">Periode</span>
          <span class="period-value">{{ fromLabel }} -> {{ toLabel }}</span>
        </div>
      </div>

      <div v-if="props.view === 'pie'" class="tp-main">
        <div class="tp-legend">
          <div v-if="!items.length" class="tp-empty">Aucune donnee sur la periode.</div>
          <div v-else class="tp-list">
            <div v-for="row in rows" :key="row.label" class="tp-row">
              <span class="tp-dot" :style="{ background: row.color }"></span>
              <span class="tp-name">{{ row.label }}</span>
              <span class="tp-metric">
                <span class="tp-pct">{{ row.pct }}%</span>
                <span class="tp-val">{{ row.valueText }}</span>
              </span>
            </div>
          </div>
        </div>

        <VChart class="tp-chart" :style="chartStyle" :option="option" autoresize />
      </div>

      <div v-else class="tp-stack">
        <VChart class="tp-chart" :style="chartStyle" :option="option" autoresize />
        <div class="tp-legend">
          <div v-if="!items.length" class="tp-empty">Aucune donnee sur la periode.</div>
          <div v-else class="tp-list">
            <div v-for="row in rows" :key="row.label" class="tp-row">
              <span class="tp-dot" :style="{ background: row.color }"></span>
              <span class="tp-name">{{ row.label }}</span>
              <span class="tp-metric">
                <span class="tp-pct">{{ row.pct }}%</span>
                <span class="tp-val">{{ row.valueText }}</span>
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
import { normalizeRank } from '@/services/statsAdapters'
import { formatDateFR, formatEUR } from '@/utils/formatters'
import WidgetCard from './_parts/WidgetCard.vue'

const props = defineProps({
  from: String,
  to: String,
  top: { type: Number, default: 8 },
  view: { type: String, default: 'bars' },
  widgetWidth: { type: Number, default: 720 },
  widgetHeight: { type: Number, default: 360 },
  widgetBaseWidth: { type: Number, default: 0 },
  widgetBaseHeight: { type: Number, default: 0 },
  widgetRenderWidth: { type: Number, default: 0 },
  widgetRenderHeight: { type: Number, default: 0 },
  categories: { type: Array, default: () => [] },
  types: { type: Array, default: () => [] },
})
const accent = '#8B5CF6'
const loading = ref(false)
const error = ref('')
const rankItems = ref([])
let req = 0

async function load() {
  const id = ++req
  loading.value = true
  error.value = ''
  try {
    const c = await StatsServices.rank(
      'topCategoriesProfit',
      props.from,
      props.to,
      props.top,
      props.categories,
      props.types,
    )
    if (id !== req) return
    rankItems.value = normalizeRank(c.data)
  } catch (e) {
    if (id !== req) return
    error.value = e?.response?.data?.message ?? e?.message ?? 'Impossible de charger'
  } finally {
    if (id === req) loading.value = false
  }
}

onMounted(load)
watch(() => [props.from, props.to, props.top, props.categories, props.types], load)

const PALETTE = [
  '#22C55E',
  '#3B82F6',
  '#F59E0B',
  '#A855F7',
  '#EF4444',
  '#14B8A6',
  '#F97316',
  '#84CC16',
  '#06B6D4',
  '#E11D48',
  '#8B5CF6',
  '#10B981',
]

const clamp = (value, min, max) => Math.max(min, Math.min(max, value))

function pickColorByIndex(index) {
  return PALETTE[index % PALETTE.length]
}

const layoutWidth = computed(() => {
  return Math.max(Number(props.widgetWidth ?? 0), 1)
})
const layoutHeight = computed(() => {
  return Math.max(Number(props.widgetHeight ?? 0), 1)
})

const compactMode = computed(() => layoutWidth.value < 680 || layoutHeight.value < 340)
const denseMode = computed(() => layoutWidth.value < 560 || layoutHeight.value < 300)

const items = computed(() => rankItems.value)
const chartHeight = computed(() => {
  const reserved = props.view === 'pie' ? 92 : 150
  const available = Math.max(layoutHeight.value - reserved, 120)

  if (props.view === 'pie') {
    return clamp(Math.min(available, layoutWidth.value * 0.52), 150, 320)
  }

  if (props.view === 'treemap' || props.view === 'heatmap') {
    return clamp(available, 140, 280)
  }

  const count = Math.max(2, items.value.length)
  const base = 110
  const per = denseMode.value ? 20 : 26
  return clamp(Math.min(available, base + count * per), 120, 280)
})
const chartStyle = computed(() => ({ height: `${chartHeight.value}px`, minHeight: '0px' }))

const total = computed(() => items.value.reduce((acc, i) => acc + Number(i.value ?? 0), 0))
const rows = computed(() =>
  items.value.map((i, index) => {
    const value = Number(i.value ?? 0)
    const pct = total.value > 0 ? Math.round((value / total.value) * 100) : 0
    return {
      label: i.label,
      value,
      pct,
      valueText: formatEUR(value, { compact: true }),
      color: pickColorByIndex(index),
    }
  }),
)

const option = computed(() => {
  const labels = items.value.map((i) => i.label)
  const values = items.value.map((i) => i.value)

  if (props.view === 'pie') {
    return {
      backgroundColor: 'transparent',
      tooltip: {
        trigger: 'item',
        confine: true,
        transitionDuration: 0,
        formatter: ({ name, value, percent }) =>
          `${name}<br/>${formatEUR(value)} - ${percent}%`,
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
          data: rows.value.map((r) => ({
            name: r.label,
            value: r.value,
            itemStyle: { color: r.color },
          })),
        },
      ],
    }
  }

  if (props.view === 'treemap') {
    return {
      backgroundColor: 'transparent',
      tooltip: {
        trigger: 'item',
        confine: true,
        transitionDuration: 0,
        formatter: ({ name, value }) => `${name}<br/>${formatEUR(value)}`,
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
          data: rows.value.map((r) => ({
            name: r.label,
            value: r.value,
            itemStyle: { color: r.color },
          })),
        },
      ],
    }
  }

  if (props.view === 'heatmap') {
    const maxVal = values.reduce((m, v) => Math.max(m, Number(v || 0)), 0)
    return {
      backgroundColor: 'transparent',
      tooltip: {
        trigger: 'item',
        confine: true,
        transitionDuration: 0,
        formatter: (p) => {
          const v = Array.isArray(p?.value) ? p.value[2] : p?.value
          return `${p?.name ?? ''}<br/>${formatEUR(v)}`
        },
      },
      grid: { left: 60, right: 16, top: 16, bottom: 30, containLabel: true },
      xAxis: {
        type: 'category',
        data: labels,
        axisLabel: { color: '#E5E7EB', fontSize: 10, interval: 0 },
        axisLine: { lineStyle: { color: '#334155' } },
        axisTick: { show: false },
      },
      yAxis: {
        type: 'category',
        data: ['Profit'],
        axisLabel: { color: '#9CA3AF', fontSize: 10 },
        axisLine: { lineStyle: { color: '#334155' } },
        axisTick: { show: false },
      },
      visualMap: {
        min: 0,
        max: maxVal || 1,
        show: false,
        inRange: { color: ['#1E293B', '#7C3AED', '#F59E0B'] },
      },
      series: [
        {
          type: 'heatmap',
          data: labels.map((_, i) => [i, 0, values[i] ?? 0]),
          itemStyle: { borderColor: 'rgba(255,255,255,0.08)', borderWidth: 1 },
          label: {
            show: true,
            color: '#E2E8F0',
            fontSize: 10,
            formatter: (p) => formatEUR(p.value?.[2] ?? 0, { compact: true }),
          },
          emphasis: { disabled: true },
        },
      ],
    }
  }

  return {
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'axis',
      valueFormatter: (v) => formatEUR(v),
    },
    grid: { left: 90, right: 20, top: 6, bottom: 6, containLabel: true },
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
        data: values.map((v, i) => ({
          value: v,
          itemStyle: { borderRadius: [8, 8, 8, 8], color: rows.value[i]?.color },
        })),
        barWidth: 14,
        label: {
          show: true,
          position: 'right',
          color: '#E2E8F0',
          fontSize: 11,
          formatter: (p) => formatEUR(p.value, { compact: true }),
        },
      },
    ],
    color: [accent],
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
.tp-root {
  height: 100%;
  min-height: 0;
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  gap: 8px;
}

.tp-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}
.tp-tabs {
  display: inline-flex;
  gap: 6px;
}
.tp-pill {
  font-size: 11px;
  padding: 4px 10px;
  border-radius: 999px;
  border: 1px solid transparent;
}
.tp-pill--static {
  border: 1px solid rgba(255, 255, 255, 0.12);
  background: rgba(148, 163, 184, 0.12);
  color: rgba(226, 232, 240, 0.85);
}
.tp-main {
  min-height: 0;
  display: grid;
  grid-template-columns: minmax(200px, 1fr) minmax(260px, 1.1fr);
  gap: 14px;
  align-items: stretch;
}
.tp-stack {
  min-height: 0;
  display: grid;
  grid-template-rows: minmax(0, 1fr) auto;
  gap: 8px;
}
.tp-chart {
  width: 100%;
  min-height: 0;
}
.tp-legend {
  margin-top: 0;
  min-height: 0;
  overflow: hidden;
}
.tp-list {
  min-height: 0;
  overflow: auto;
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding-right: 2px;
}
.tp-row {
  display: grid;
  grid-template-columns: 12px 1fr auto;
  gap: 8px;
  align-items: center;
  padding: 4px 8px;
  border-radius: 12px;
  background: rgba(15, 23, 42, 0.45);
  border: 1px solid rgba(255, 255, 255, 0.06);
}
.tp-dot {
  width: 8px;
  height: 8px;
  border-radius: 999px;
  box-shadow: 0 0 8px rgba(0, 0, 0, 0.25);
}
.tp-name {
  font-size: 12px;
  color: rgba(226, 232, 240, 0.92);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.tp-val {
  font-size: 12px;
  color: rgba(226, 232, 240, 0.85);
}
.tp-metric {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  justify-self: end;
  min-width: 0;
}
.tp-pct {
  font-size: 11px;
  color: rgba(226, 232, 240, 0.6);
}
.tp-empty {
  font-size: 12px;
  color: rgba(226, 232, 240, 0.5);
}
.period-chip {
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
  max-width: min(100%, 360px);
}
.period-label {
  opacity: 0.7;
}
.period-value {
  font-size: 11px;
  letter-spacing: 0.02em;
  text-transform: none;
  color: rgba(226, 232, 240, 0.9);
  overflow: hidden;
  text-overflow: ellipsis;
}

.is-compact .tp-main {
  grid-template-columns: 1fr;
}

.is-compact .period-chip {
  max-width: 100%;
}

.is-dense .tp-row {
  padding: 3px 7px;
  gap: 7px;
}

.is-dense .tp-name,
.is-dense .tp-val {
  font-size: 11px;
}
</style>
