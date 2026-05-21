<template>
  <section class="npk-root" :class="[layoutClass, `is-${valueTone}`]" :style="layoutVars">
    <span class="npk-accent" aria-hidden="true"></span>
    <p class="npk-value">{{ valueText }}</p>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { formatEUR, formatNumber } from '@/utils/formatters'
import { fitKpiValueSize } from './_parts/kpiTextFit'

type Bucket = 'day' | 'week' | 'month'
type Tone = 'positive' | 'negative' | 'neutral'

type LayoutMode = 'compact' | 'medium' | 'large'

interface SeriesPoint {
  date: string
  value: number
}

interface NetProfitMetrics {
  value: number
  previousValue: number
  deltaValue: number
  deltaPct: number | null
  series: SeriesPoint[]
  bestPoint: SeriesPoint | null
  lastPoint: SeriesPoint | null
  salesCount: number | null
  avgProfitPerSale: number | null
  marginPct: number | null
}

interface LayoutInfo {
  width: number
  height: number
  mode: LayoutMode
  tiny: boolean
}

const props = withDefaults(
  defineProps<{
    metrics: NetProfitMetrics
    periodText: string
    bucket: Bucket
    layout: LayoutInfo
    showComparison?: boolean
    showSalesKpi?: boolean
    showAvgProfitPerSale?: boolean
    showNetMargin?: boolean
    showBestPeriod?: boolean
    secondaryLimit?: number
    kpiVariant?: string
  }>(),
  {
    showComparison: true,
    showSalesKpi: true,
    showAvgProfitPerSale: true,
    showNetMargin: true,
    showBestPeriod: true,
    secondaryLimit: 4,
    kpiVariant: 'total',
  },
)

const EPS = 0.0001
const clamp = (value: number, min: number, max: number) => Math.max(min, Math.min(max, value))

function toneFromValue(value: number): Tone {
  if (value > EPS) return 'positive'
  if (value < -EPS) return 'negative'
  return 'neutral'
}

const primaryKind = computed(() => String(props.kpiVariant ?? 'total'))
const primaryRawValue = computed(() => {
  if (primaryKind.value === 'avgProfit') return Number(props.metrics.avgProfitPerSale ?? 0)
  if (primaryKind.value === 'sales') return Number(props.metrics.salesCount ?? 0)
  return Number(props.metrics.value ?? 0)
})
const valueTone = computed(() =>
  primaryKind.value === 'sales' ? 'neutral' : toneFromValue(primaryRawValue.value),
)
const isSalesOnly = computed(() => primaryKind.value === 'sales')

const valueText = computed(() => {
  if (primaryKind.value === 'sales') {
    return formatNumber(primaryRawValue.value, { digits: 0 })
  }
  const compact = props.layout.mode !== 'large' || Math.abs(primaryRawValue.value) >= 1_000_000
  return formatEUR(primaryRawValue.value, { compact, digits: compact ? 1 : 0 })
})

const layoutClass = computed(() => ({
  'is-compact': props.layout.mode === 'compact',
  'is-tiny': props.layout.tiny,
  'is-sales-only': isSalesOnly.value,
}))

const layoutVars = computed(() => {
  const mainSize = fitKpiValueSize(valueText.value, props.layout.width, props.layout.height, {
    min: 22,
    max: 76,
    paddingX: Math.max(54, props.layout.width * 0.3),
    paddingY: Math.max(18, props.layout.height * 0.2),
    heightRatio: 0.48,
  })

  return {
    '--npk-gap': `${clamp(Math.round(Math.min(props.layout.width * 0.018, props.layout.height * 0.04)), 4, 7)}px`,
    '--npk-main-size': `${mainSize}px`,
    '--npk-label-size': `${clamp(Math.round(Math.min(props.layout.width * 0.015, props.layout.height * 0.05)), 10, 12)}px`,
    '--npk-sub-size': `${clamp(Math.round(Math.min(props.layout.width * 0.018, props.layout.height * 0.06)), 11, 14)}px`,
    '--npk-card-size': `${clamp(Math.round(Math.min(props.layout.width * 0.024, props.layout.height * 0.075)), 13, 19)}px`,
  }
})
</script>

<style scoped>
.npk-root {
  height: 100%;
  min-height: 0;
  display: grid;
  grid-template-rows: auto auto;
  gap: var(--npk-gap);
  place-content: center;
  justify-items: center;
  overflow: hidden;
}

.npk-value {
  margin: 0;
  font-size: var(--npk-main-size);
  line-height: 0.98;
  letter-spacing: 0;
  font-weight: 820;
  color: #111827;
  font-variant-numeric: tabular-nums;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: clip;
}

.npk-root.is-positive .npk-value {
  color: #047857;
}

.npk-root.is-negative .npk-value {
  color: #be123c;
}

.npk-accent {
  display: block;
  width: clamp(36px, 18%, 64px);
  height: 3px;
  border-radius: 999px;
  background: linear-gradient(90deg, #5b5ce2, rgba(37, 99, 235, 0.72));
  box-shadow: 0 8px 18px rgba(91, 92, 226, 0.16);
}

.npk-root.is-sales-only {
  grid-template-rows: auto auto;
  gap: clamp(8px, calc(var(--npk-gap) * 1.4), 12px);
}

</style>
