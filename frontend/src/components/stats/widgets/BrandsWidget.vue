<template>
  <WidgetCard
    class="brands-widget"
    title="Top marques"
    subtitle="Volume des ventes"
    :accent="accent"
    surface="distribution"
    :loading="loading"
    :error="error"
    :auto-height="props.autoHeight"
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
  ([nextHeight, autoHeight, _view, _count, isLoading]) => {
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

const option = computed(() => {
  const items = visibleItems.value
  const labels = items.map((i) => i.label)
  const values = items.map((i) => i.nb)

  if (props.view === 'treemap') {
    return {
      backgroundColor: 'transparent',
      tooltip: {
        trigger: 'item',
        confine: true,
        transitionDuration: 0,
        formatter: ({ name, value }) => `${name}<br/>${formatNumber(value)} ventes`,
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
          data: items.map((r, i) => ({
            name: r.label,
            value: r.nb,
            itemStyle: { color: getBrandColor(r.label) },
          })),
        },
      ],
    }
  }

  if (props.view === 'heatmap') {
    return {
      backgroundColor: 'transparent',
      tooltip: {
        trigger: 'item',
        confine: true,
        transitionDuration: 0,
        formatter: (p) => {
          const v = Array.isArray(p?.value) ? p.value[2] : p?.value
          return `${p?.name ?? ''}<br/>${formatNumber(v)} ventes`
        },
      },
      grid: { left: 62, right: 12, top: 8, bottom: 26, containLabel: true },
      xAxis: {
        type: 'category',
        data: labels,
        axisLabel: { color: '#E5E7EB', fontSize: 10, interval: 0 },
        axisLine: { lineStyle: { color: '#334155' } },
        axisTick: { show: false },
      },
      yAxis: {
        type: 'category',
        data: ['Ventes'],
        axisLabel: { color: '#9CA3AF', fontSize: 10 },
        axisLine: { lineStyle: { color: '#334155' } },
        axisTick: { show: false },
      },
      series: [
        {
          type: 'heatmap',
          data: labels.map((label, i) => ({
            value: [i, 0, values[i] ?? 0],
            itemStyle: { color: getBrandColor(label) },
          })),
          itemStyle: { borderColor: 'rgba(255,255,255,0.08)', borderWidth: 1 },
          label: {
            show: true,
            color: '#E2E8F0',
            fontSize: 10,
            formatter: (p) => formatNumber(p.value?.[2] ?? 0),
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
      formatter: (params) => {
        const point = Array.isArray(params) ? params[0] : params
        if (!point) return ''
        const value = Number(point.value ?? 0)
        return `${point.name}<br/>${formatNumber(value)} ventes`
      },
    },
    grid: {
      left: clamp(Math.round(layoutWidth.value * 0.2), 94, 186),
      right: 16,
      top: effectiveRows.value <= 2 ? 4 : 8,
      bottom: 18,
      containLabel: false,
    },
    xAxis: {
      type: 'value',
      boundaryGap: [0, 0.06],
      minInterval: 1,
      axisLabel: { color: '#9CA3AF', fontSize: 10 },
      axisLine: { show: false },
      axisTick: { show: false },
      splitNumber: 4,
      splitLine: { lineStyle: { color: 'rgba(148,163,184,0.14)' } },
    },
    yAxis: {
      type: 'category',
      data: labels,
      inverse: true,
      axisLabel: {
        color: '#E5E7EB',
        fontSize: 11,
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
          14,
          28,
        ),
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
