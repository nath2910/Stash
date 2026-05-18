<template>
  <section class="npk-root" :class="[layoutClass, `is-${valueTone}`]" :style="layoutVars">
    <span class="npk-accent" aria-hidden="true"></span>
    <p class="npk-value">{{ valueText }}</p>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { formatEUR, formatNumber, formatPct } from '@/utils/formatters'
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

interface SecondaryCard {
  key: string
  label: string
  value: string
  meta: string
  tone: Tone
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

function signedCurrency(value: number) {
  const abs = formatEUR(Math.abs(value), { compact: true, digits: 1 })
  if (value > 0) return `+${abs}`
  if (value < 0) return `-${abs}`
  return abs
}

function signedPercent(value: number | null) {
  if (value == null || !Number.isFinite(value)) return 'n/d'
  const sign = value > 0 ? '+' : ''
  return `${sign}${value.toFixed(1)}%`
}

function formatBestDate(date: string) {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(String(date ?? ''))) return String(date ?? '--')
  const raw = new Date(`${date}T00:00:00`)
  if (Number.isNaN(raw.getTime())) return date
  if (props.bucket === 'month') {
    return raw.toLocaleDateString('fr-FR', { month: 'short', year: '2-digit' })
  }
  if (props.bucket === 'week') {
    return `sem. du ${raw.toLocaleDateString('fr-FR', { day: '2-digit', month: 'short' })}`
  }
  return raw.toLocaleDateString('fr-FR', { day: '2-digit', month: 'short', year: 'numeric' })
}

function bucketUnitLabel(bucket: Bucket, count: number) {
  if (bucket === 'month') return count > 1 ? 'mois' : 'mois'
  if (bucket === 'week') return count > 1 ? 'semaines' : 'semaine'
  return count > 1 ? 'jours' : 'jour'
}

const deltaTone = computed(() => toneFromValue(props.metrics.deltaValue))
const primaryKind = computed(() => String(props.kpiVariant ?? 'total'))
const primaryRawValue = computed(() => {
  if (primaryKind.value === 'avgProfit') return Number(props.metrics.avgProfitPerSale ?? 0)
  if (primaryKind.value === 'sales') return Number(props.metrics.salesCount ?? 0)
  return Number(props.metrics.value ?? 0)
})
const valueTone = computed(() =>
  primaryKind.value === 'sales' ? 'neutral' : toneFromValue(primaryRawValue.value),
)
const showComparison = computed(() => props.showComparison !== false)
const showPrimaryComparison = computed(() => showComparison.value && primaryKind.value === 'total')
const isSalesOnly = computed(() => primaryKind.value === 'sales')

const primaryLabel = computed(() => {
  if (primaryKind.value === 'avgProfit') return 'Bénéfice moyen'
  if (primaryKind.value === 'sales') return 'Nombre de ventes'
  return 'Bénéfice net'
})

const valueText = computed(() => {
  if (primaryKind.value === 'sales') {
    return formatNumber(primaryRawValue.value, { digits: 0 })
  }
  const compact = props.layout.mode !== 'large' || Math.abs(primaryRawValue.value) >= 1_000_000
  return formatEUR(primaryRawValue.value, { compact, digits: compact ? 1 : 0 })
})

const primaryMeta = computed(() => {
  if (primaryKind.value === 'avgProfit') {
    return props.metrics.salesCount
      ? `${formatNumber(props.metrics.salesCount, { digits: 0 })} ventes analysees`
      : 'Moyenne par vente'
  }
  if (primaryKind.value === 'sales') return 'Ventes sur la periode'
  return 'Total net sur la periode'
})

const deltaValueText = computed(() => signedCurrency(props.metrics.deltaValue))
const deltaPctText = computed(() => signedPercent(props.metrics.deltaPct))

const maxSecondaryBySize = computed(() => {
  if (props.layout.tiny) return 0
  if (props.layout.mode === 'compact') return 2
  if (props.layout.mode === 'medium') {
    if (props.layout.width >= 620 || props.layout.height >= 320) return 4
    return 3
  }
  return 4
})

const secondaryLimit = computed(() => {
  const requested = clamp(
    Math.round(Number(props.secondaryLimit ?? 4)),
    0,
    Math.min(4, maxSecondaryBySize.value),
  )
  if (
    (props.layout.mode === 'medium' || props.layout.mode === 'large') &&
    maxSecondaryBySize.value >= 4
  ) {
    return Math.max(requested, 4)
  }
  return requested
})

const bestPeriodLabel = computed(() => {
  if (props.bucket === 'month') return 'Meilleur mois'
  if (props.bucket === 'week') return 'Meilleure semaine'
  return 'Meilleur jour'
})

const worstPeriodLabel = computed(() => {
  if (props.bucket === 'month') return 'Pire mois'
  if (props.bucket === 'week') return 'Pire semaine'
  return 'Pire jour'
})

const profitableRate = computed<number | null>(() => {
  const points = props.metrics.series ?? []
  if (points.length < 2) return null
  const total = points.length
  const positive = points.filter((p) => Number(p?.value ?? 0) > 0).length
  return total > 0 ? (positive / total) * 100 : null
})

const worstPoint = computed<SeriesPoint | null>(() => {
  const points = props.metrics.series ?? []
  if (!points.length) return null
  return points.reduce((worst, point) => (point.value < worst.value ? point : worst), points[0])
})

const secondaryCards = computed<SecondaryCard[]>(() => {
  if (isSalesOnly.value) return []

  const cards: SecondaryCard[] = []

  if (props.showNetMargin !== false && props.metrics.marginPct != null) {
    cards.push({
      key: 'margin',
      label: 'Marge nette',
      value: formatPct(props.metrics.marginPct, { digits: 1 }),
      meta: 'sur la periode',
      tone: toneFromValue(props.metrics.marginPct),
    })
  }

  if (
    props.showAvgProfitPerSale !== false &&
    props.metrics.avgProfitPerSale != null &&
    primaryKind.value !== 'avgProfit'
  ) {
    cards.push({
      key: 'avg-profit',
      label: 'Bénéfice moyen',
      value: formatEUR(props.metrics.avgProfitPerSale, { compact: true, digits: 1 }),
      meta: props.metrics.salesCount
        ? `${formatNumber(props.metrics.salesCount, { digits: 0 })} ventes`
        : '',
      tone: toneFromValue(props.metrics.avgProfitPerSale),
    })
  }

  if (
    props.showSalesKpi !== false &&
    props.metrics.salesCount != null &&
    primaryKind.value !== 'sales'
  ) {
    cards.push({
      key: 'sales',
      label: 'Ventes',
      value: formatNumber(props.metrics.salesCount, { digits: 0 }),
      meta: 'sur la periode',
      tone: 'neutral',
    })
  }

  if (profitableRate.value != null && Number.isFinite(profitableRate.value)) {
    const total = props.metrics.series.length
    const positives = props.metrics.series.filter((point) => Number(point.value ?? 0) > 0).length
    const tone: Tone =
      profitableRate.value >= 60 ? 'positive' : profitableRate.value < 40 ? 'negative' : 'neutral'
    cards.push({
      key: 'profitable-rate',
      label: 'Periodes rentables',
      value: formatPct(profitableRate.value, { digits: 0 }),
      meta: `${positives}/${total} ${bucketUnitLabel(props.bucket, total)} positifs`,
      tone,
    })
  }

  if (props.showBestPeriod !== false && props.metrics.bestPoint) {
    cards.push({
      key: 'best',
      label: bestPeriodLabel.value,
      value: formatEUR(props.metrics.bestPoint.value, { compact: true, digits: 1 }),
      meta: formatBestDate(props.metrics.bestPoint.date),
      tone: toneFromValue(props.metrics.bestPoint.value),
    })
  }

  if (worstPoint.value) {
    cards.push({
      key: 'worst',
      label: worstPeriodLabel.value,
      value: formatEUR(worstPoint.value.value, { compact: true, digits: 1 }),
      meta: formatBestDate(worstPoint.value.date),
      tone: toneFromValue(worstPoint.value.value),
    })
  }

  return cards.slice(0, secondaryLimit.value)
})

const secondaryGridClass = computed(() => {
  if (secondaryCards.value.length <= 1) return 'npk-secondary--one'
  if (props.layout.tiny || props.layout.mode === 'compact' || props.layout.width < 600) {
    return 'npk-secondary--one'
  }
  return 'npk-secondary--two'
})

const layoutClass = computed(() => ({
  'is-compact': props.layout.mode === 'compact',
  'is-tiny': props.layout.tiny,
  'is-sales-only': isSalesOnly.value,
  'has-secondary': secondaryCards.value.length > 0,
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

.npk-root.has-secondary {
  grid-template-rows: auto auto;
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

.npk-root.is-tiny .npk-secondary {
  display: none;
}
</style>
