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
            <div
              v-for="row in rows"
              :key="row.label"
              class="tp-row"
              :title="`${row.label} - ${row.pct}% - ${row.valueText}`"
            >
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
            <div
              v-for="row in rows"
              :key="row.label"
              class="tp-row"
              :title="`${row.label} - ${row.pct}% - ${row.valueText}`"
            >
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
import { getBrandColor } from '@/components/stats/brandColors'
import WidgetCard from './_parts/WidgetCard.vue'

const props = defineProps({
  from: String,
  to: String,
  top: { type: Number, default: 8 },
  view: { type: String, default: 'bars' },
  autoHeight: { type: Boolean, default: true },
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

const clamp = (value, min, max) => Math.max(min, Math.min(max, value))

const layoutWidth = computed(() => {
  return Math.max(Number(props.widgetWidth ?? 0), 1)
})
const layoutHeight = computed(() => {
  return Math.max(Number(props.widgetHeight ?? 0), 1)
})

const compactMode = computed(() => layoutWidth.value < 680 || layoutHeight.value < 340)
const denseMode = computed(() => layoutWidth.value < 560 || layoutHeight.value < 300)
const axisFont = computed(() =>
  clamp(Math.round(Math.min(layoutWidth.value * 0.0135, layoutHeight.value * 0.032)), 9, 12),
)
const valueLabelVisible = computed(() => layoutWidth.value >= 480 && layoutHeight.value >= 220)

const items = computed(() => rankItems.value)
const chartStyle = computed(() => ({ height: '100%', minHeight: '0px' }))

const total = computed(() => items.value.reduce((acc, i) => acc + Number(i.value ?? 0), 0))
const rows = computed(() =>
  items.value.map((i) => {
    const value = Number(i.value ?? 0)
    const pct = total.value > 0 ? Math.round((value / total.value) * 100) : 0
    return {
      label: i.label,
      value,
      pct,
      valueText: formatEUR(value, { compact: true }),
      color: getBrandColor(i.label),
    }
  }),
)

const longestLabel = computed(() =>
  rows.value.reduce((max, row) => Math.max(max, String(row.label ?? '').length), 0),
)

const barGrid = computed(() => ({
  left: clamp(
    Math.round(longestLabel.value * axisFont.value * 0.5 + 28),
    72,
    layoutWidth.value < 560 ? 128 : 176,
  ),
  right: valueLabelVisible.value ? clamp(Math.round(layoutWidth.value * 0.1), 44, 96) : 16,
  top: clamp(Math.round(layoutHeight.value * 0.03), 8, 18),
  bottom: clamp(Math.round(layoutHeight.value * 0.05), 12, 28),
  containLabel: false,
}))

const tileGrid = computed(() => {
  const count = Math.max(rows.value.length, 1)
  if (count <= 1) return { columns: 1, rows: 1 }
  const ratio = Math.max(layoutWidth.value / Math.max(layoutHeight.value, 1), 0.8)
  const maxColumns = denseMode.value ? 4 : 6
  const columns = clamp(Math.ceil(Math.sqrt(count * ratio)), 2, Math.min(maxColumns, count))
  const gridRows = Math.ceil(count / columns)
  return { columns, rows: gridRows }
})

function formatTileLabel(label) {
  const text = String(label ?? '').trim()
  const limit = denseMode.value ? 9 : 14
  if (text.length <= limit) return text
  return `${text.slice(0, Math.max(1, limit - 1))}...`
}

function formatAxisLabel(label) {
  const text = String(label ?? '').trim()
  const limit = denseMode.value ? 14 : 20
  if (text.length <= limit) return text
  return `${text.slice(0, Math.max(1, limit - 1))}...`
}

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
        backgroundColor: 'rgba(255,255,255,0.98)',
        borderColor: 'rgba(148,163,184,0.32)',
        textStyle: { color: '#0f172a', fontSize: 12, fontWeight: 600 },
        extraCssText: 'border-radius:10px;box-shadow:0 12px 28px rgba(15,23,42,0.14);',
        formatter: ({ name, value, percent }) =>
          `${name}<br/>${formatEUR(value)} - ${percent}%`,
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
        formatter: ({ name, value }) => `${name}<br/>${formatEUR(value)}`,
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
            formatter: ({ name, value }) =>
              `${formatTileLabel(name)}\n${formatEUR(value, { compact: true })}`,
          },
          upperLabel: { show: false },
          itemStyle: {
            borderColor: 'rgba(255,255,255,0.92)',
            borderWidth: 2,
            gapWidth: denseMode.value ? 2 : 3,
            borderRadius: 6,
          },
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
    const { columns, rows: gridRows } = tileGrid.value
    const x = Array.from({ length: columns }, (_, index) => String(index + 1))
    const y = Array.from({ length: gridRows }, (_, index) => String(index + 1))

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
        formatter: (p) => `${p?.name ?? ''}<br/>${formatEUR(p?.value?.[2] ?? 0)}`,
      },
      grid: { left: 4, right: 4, top: 4, bottom: 4, containLabel: false },
      xAxis: {
        type: 'category',
        data: x,
        axisLabel: { show: false },
        axisLine: { show: false },
        axisTick: { show: false },
        splitArea: { show: false },
      },
      yAxis: {
        type: 'category',
        data: y,
        inverse: true,
        axisLabel: { show: false },
        axisLine: { show: false },
        axisTick: { show: false },
        splitArea: { show: false },
      },
      series: [
        {
          type: 'heatmap',
          data: rows.value.map((row, index) => ({
            name: row.label,
            value: [index % columns, Math.floor(index / columns), row.value],
            itemStyle: { color: row.color },
          })),
          itemStyle: {
            borderColor: 'rgba(255,255,255,0.9)',
            borderWidth: denseMode.value ? 2 : 3,
            borderRadius: 6,
          },
          label: {
            show: true,
            color: '#ffffff',
            fontSize: axisFont.value,
            fontWeight: 780,
            lineHeight: Math.round(axisFont.value * 1.16),
            formatter: (p) => {
              const value = formatEUR(p.value?.[2] ?? 0, { compact: true })
              return denseMode.value ? value : `${formatTileLabel(p?.name ?? '')}\n${value}`
            },
          },
          emphasis: {
            itemStyle: {
              opacity: 0.92,
            },
          },
        },
      ],
    }
  }

  return {
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'axis',
      valueFormatter: (v) => formatEUR(v),
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
        formatter: (value) => formatEUR(value, { compact: true, digits: 1 }),
      },
      axisLine: { show: false },
      axisTick: { show: false },
      splitLine: { lineStyle: { color: 'rgba(148,163,184,0.18)' } },
    },
    yAxis: {
      type: 'category',
      data: labels,
      axisLabel: {
        color: '#334155',
        fontSize: axisFont.value,
        fontWeight: 700,
        formatter: (value) => formatAxisLabel(value),
      },
      axisTick: { show: false },
      axisLine: { show: false },
    },
    series: [
      {
        type: 'bar',
        data: values.map((v, i) => ({
          value: v,
          itemStyle: {
            color: rows.value[i]?.color,
            borderRadius: [8, 8, 8, 8],
          },
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
  grid-template-rows: minmax(0, 1fr);
  gap: 0;
}

.tp-header {
  display: none;
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
  border: 1px solid rgba(148, 163, 184, 0.24);
  background: rgba(248, 250, 252, 0.82);
  color: #334155;
}
.tp-main {
  min-height: 0;
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 0;
  align-items: stretch;
}
.tp-stack {
  min-height: 0;
  display: grid;
  grid-template-rows: minmax(0, 1fr);
  gap: 0;
  align-content: stretch;
}
.tp-chart {
  width: 100%;
  height: 100%;
  min-height: 0;
}
.tp-legend {
  display: none;
}
.tp-stack .tp-legend {
  padding-top: 8px;
  border-top: 1px solid rgba(148, 163, 184, 0.16);
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
  border-radius: 8px;
  background: rgba(248, 250, 252, 0.82);
  border: 1px solid rgba(148, 163, 184, 0.22);
  transition:
    background-color 140ms ease,
    border-color 140ms ease;
}
.tp-row:hover {
  background: rgba(241, 245, 249, 0.95);
  border-color: rgba(148, 163, 184, 0.24);
}
.tp-dot {
  width: 8px;
  height: 8px;
  border-radius: 999px;
  box-shadow: 0 0 8px rgba(0, 0, 0, 0.25);
}
.tp-name {
  font-size: 12px;
  color: #111827;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.tp-val {
  font-size: 12px;
  color: #334155;
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
  color: #64748b;
}
.tp-empty {
  font-size: 12px;
  color: #64748b;
}
.period-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 10px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: #64748b;
  white-space: nowrap;
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(248, 250, 252, 0.82);
  border: 1px solid rgba(148, 163, 184, 0.26);
  max-width: min(100%, 360px);
}
.period-label {
  opacity: 0.7;
}
.period-value {
  font-size: 11px;
  letter-spacing: 0.02em;
  text-transform: none;
  color: #334155;
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
