<template>
  <WidgetCard
    class="brands-widget"
    title="Top marques"
    subtitle="Volume des ventes"
    :accent="accent"
    surface="distribution"
    :loading="loading"
    :error="error"
    :widget-width="layoutWidth"
    :widget-height="layoutHeight"
    :widget-base-width="props.widgetBaseWidth"
    :widget-base-height="props.widgetBaseHeight"
  >
    <div class="brands-widget__content">
      <VChart class="chart" :option="option" autoresize />
    </div>
  </WidgetCard>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import StatsServices from '@/services/StatsServices'
import { normalizeBrands } from '@/services/statsAdapters'
import { formatNumber } from '@/utils/formatters'
import { getBrandColor } from '@/components/stats/brandColors'
import WidgetCard from './_parts/WidgetCard.vue'

const emit = defineEmits(['auto-resize'])
const props = defineProps({
  from: String,
  to: String,
  top: { type: Number, default: 8 },
  view: { type: String, default: 'bars' },
  autoHeight: { type: Boolean, default: true },
  widgetWidth: { type: Number, default: 720 },
  widgetHeight: { type: Number, default: 420 },
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
const brands = ref([])
let req = 0
let lastAutoResizeHeight = 0

async function load() {
  const id = ++req
  loading.value = true
  error.value = ''
  try {
    const { data } = await StatsServices.brands(props.from, props.to, props.categories, props.types)
    if (id !== req) return
    brands.value = normalizeBrands(data)
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
  const renderWidth = Number(props.widgetRenderWidth ?? 0)
  if (Number.isFinite(renderWidth) && renderWidth > 0) return renderWidth
  return Math.max(Number(props.widgetWidth ?? 0), 1)
})
const layoutHeight = computed(() => {
  const renderHeight = Number(props.widgetRenderHeight ?? 0)
  if (Number.isFinite(renderHeight) && renderHeight > 0) return renderHeight
  return Math.max(Number(props.widgetHeight ?? 0), 1)
})

const visibleItems = computed(() => brands.value.slice(0, props.top))
const itemCount = computed(() => visibleItems.value.length)
const minDisplayRows = computed(() => (props.view === 'bars' ? 1 : 3))
const effectiveRows = computed(() => Math.max(itemCount.value, minDisplayRows.value))

const cardChromeHeight = computed(() => {
  if (props.view !== 'bars') return layoutWidth.value < 620 ? 88 : 94
  return layoutWidth.value < 620 ? 80 : 86
})

const barRowHeight = computed(() => {
  if (effectiveRows.value <= 1) return layoutWidth.value < 620 ? 38 : 42
  if (effectiveRows.value <= 3) return layoutWidth.value < 620 ? 34 : 38
  if (effectiveRows.value >= 10) return 28
  return layoutWidth.value < 620 ? 30 : 34
})

const desiredChartHeight = computed(() => {
  if (props.view !== 'bars') {
    return clamp(Math.round(Math.min(layoutWidth.value * 0.55, 320)), 170, 360)
  }
  const axisSpace = 34
  return clamp(effectiveRows.value * barRowHeight.value + axisSpace, 120, 760)
})

const currentChartHeight = computed(() =>
  clamp(Math.round(layoutHeight.value - cardChromeHeight.value), 96, 900),
)
const denseMode = computed(() => layoutWidth.value < 560 || layoutHeight.value < 300)
const axisFont = computed(() =>
  clamp(Math.round(Math.min(layoutWidth.value * 0.014, layoutHeight.value * 0.034)), 9, 12),
)
const valueLabelVisible = computed(() => layoutWidth.value >= 420 && layoutHeight.value >= 190)
const longestLabel = computed(() =>
  visibleItems.value.reduce((max, item) => Math.max(max, String(item.label ?? '').length), 0),
)
const barGrid = computed(() => ({
  left: clamp(
    Math.round(longestLabel.value * axisFont.value * 0.52 + 30),
    70,
    layoutWidth.value < 560 ? 124 : 178,
  ),
  right: valueLabelVisible.value ? clamp(Math.round(layoutWidth.value * 0.08), 34, 76) : 14,
  top: clamp(Math.round(layoutHeight.value * 0.03), 8, 18),
  bottom: clamp(Math.round(layoutHeight.value * 0.05), 12, 28),
  containLabel: false,
}))
const tileGrid = computed(() => {
  const count = Math.max(visibleItems.value.length, 1)
  if (count <= 1) return { columns: 1, rows: 1 }
  const ratio = Math.max(layoutWidth.value / Math.max(layoutHeight.value, 1), 0.8)
  const maxColumns = denseMode.value ? 4 : 6
  const columns = clamp(Math.ceil(Math.sqrt(count * ratio)), 2, Math.min(maxColumns, count))
  const rows = Math.ceil(count / columns)
  return { columns, rows }
})

const recommendedWidgetHeight = computed(() => {
  const widgetHeader = 44
  const total = widgetHeader + cardChromeHeight.value + desiredChartHeight.value
  if (props.view === 'bars') {
    // Keep dynamic height by row count, but avoid cramped bars on small datasets.
    return clamp(Math.round(total), 260, 980)
  }
  const baseContent = Number(props.widgetBaseHeight ?? 0)
  const baseTotal = Number.isFinite(baseContent) && baseContent > 0 ? baseContent + widgetHeader : 0
  const floor = clamp(Math.round(baseTotal * 0.78), 320, 560)
  return clamp(Math.round(total), floor, 980)
})

watch(
  [recommendedWidgetHeight, () => props.autoHeight, () => props.view, itemCount, loading],
  ([nextHeight, autoHeight, , , isLoading]) => {
    if (!autoHeight || isLoading) return
    if (!Number.isFinite(nextHeight) || nextHeight <= 0) return
    if (Math.abs(nextHeight - lastAutoResizeHeight) < 8) return
    lastAutoResizeHeight = nextHeight
    emit('auto-resize', nextHeight)
  },
  { immediate: true },
)

function formatLabel(label) {
  const text = String(label ?? '').trim()
  if (text.length <= 18) return text
  return `${text.slice(0, 17)}...`
}

function formatTileLabel(label) {
  const text = String(label ?? '').trim()
  const limit = denseMode.value ? 9 : 14
  if (text.length <= limit) return text
  return `${text.slice(0, Math.max(1, limit - 1))}...`
}

const option = computed(() => {
  const items = visibleItems.value
  const labels = items.map((i) => i.label)
  const values = items.map((i) => i.nb)

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
        formatter: ({ name, value }) => `${name}<br/>${formatNumber(value)} ventes`,
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
            formatter: ({ name, value }) => `${formatTileLabel(name)}\n${formatNumber(value)}`,
          },
          upperLabel: { show: false },
          itemStyle: {
            borderColor: 'rgba(255,255,255,0.92)',
            borderWidth: 2,
            gapWidth: denseMode.value ? 2 : 3,
            borderRadius: 6,
          },
          data: items.map((r) => ({
            name: r.label,
            value: r.nb,
            itemStyle: { color: getBrandColor(r.label) },
          })),
        },
      ],
    }
  }

  if (props.view === 'heatmap') {
    const { columns, rows } = tileGrid.value
    const x = Array.from({ length: columns }, (_, index) => String(index + 1))
    const y = Array.from({ length: rows }, (_, index) => String(index + 1))

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
        formatter: (p) => {
          const v = Array.isArray(p?.value) ? p.value[2] : p?.value
          return `${p?.name ?? ''}<br/>${formatNumber(v)} ventes`
        },
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
          data: labels.map((label, i) => ({
            name: label,
            value: [i % columns, Math.floor(i / columns), values[i] ?? 0],
            itemStyle: { color: getBrandColor(label) },
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
              const value = formatNumber(p.value?.[2] ?? 0)
              return denseMode.value ? value : `${formatTileLabel(p?.name ?? '')}\n${value}`
            },
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
      confine: true,
      transitionDuration: 0,
      axisPointer: { type: 'shadow', shadowStyle: { color: 'rgba(100,116,139,0.14)' } },
      backgroundColor: 'rgba(255,255,255,0.98)',
      borderColor: 'rgba(148,163,184,0.32)',
      textStyle: { color: '#0f172a', fontSize: 12, fontWeight: 600 },
      extraCssText: 'border-radius:10px;box-shadow:0 12px 28px rgba(15,23,42,0.14);',
      formatter: (params) => {
        const point = Array.isArray(params) ? params[0] : params
        if (!point) return ''
        const value = Number(point.value ?? 0)
        return `${point.name}<br/>${formatNumber(value)} ventes`
      },
    },
    grid: barGrid.value,
    xAxis: {
      type: 'value',
      boundaryGap: [0, 0.06],
      minInterval: 1,
      axisLabel: { color: '#64748b', fontSize: axisFont.value, fontWeight: 650 },
      axisLine: { show: false },
      axisTick: { show: false },
      splitNumber: 4,
      splitLine: { lineStyle: { color: 'rgba(148,163,184,0.18)' } },
    },
    yAxis: {
      type: 'category',
      data: labels,
      inverse: true,
      axisLabel: {
        color: '#334155',
        fontSize: axisFont.value,
        fontWeight: 700,
        margin: 12,
        formatter: (value) => formatLabel(value),
      },
      axisTick: { show: false },
      axisLine: { show: false },
    },
    series: [
      {
        type: 'bar',
        barCategoryGap: effectiveRows.value <= 2 ? '48%' : '30%',
        data: values.map((value, index) => ({
          value,
          itemStyle: { color: getBrandColor(labels[index]), borderRadius: [10, 10, 10, 10] },
        })),
        barWidth: clamp(
          Math.round((currentChartHeight.value / Math.max(effectiveRows.value, 1)) * 0.42),
          denseMode.value ? 10 : 14,
          28,
        ),
        label: {
          show: valueLabelVisible.value,
          position: 'right',
          color: '#334155',
          fontSize: axisFont.value,
          fontWeight: 750,
          formatter: (p) => formatNumber(p.value),
        },
      },
    ],
  }
})
</script>

<style scoped>
.brands-widget__content {
  width: 100%;
  height: 100%;
  min-height: 0;
  display: grid;
}

.chart {
  width: 100%;
  height: 100%;
  min-height: 120px;
}

.brands-widget :deep(.widget-card__content),
.brands-widget :deep(.widget-card__slot) {
  overflow: hidden;
}

.brands-widget :deep(.widget-card__slot) {
  padding-right: 0;
}
</style>
