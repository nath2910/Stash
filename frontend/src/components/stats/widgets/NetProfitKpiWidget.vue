<template>
  <section class="npk-root" :class="layoutClass" :style="layoutVars">
    <header class="npk-head">
      <p class="npk-overline">Periode analysee</p>
      <p class="npk-period" :title="periodText">{{ periodText }}</p>
    </header>

    <section class="npk-main" :class="`is-${valueTone}`">
      <p class="npk-main__label">{{ primaryLabel }}</p>
      <p class="npk-value">{{ valueText }}</p>
      <div v-if="showPrimaryComparison" class="npk-delta" :class="`is-${deltaTone}`">
        <span class="npk-delta__value">{{ deltaValueText }}</span>
        <span class="npk-delta__pct">{{ deltaPctText }}</span>
        <span class="npk-delta__meta">vs periode precedente</span>
      </div>
      <p v-else class="npk-main__meta">{{ primaryMeta }}</p>
    </section>

    <section v-if="secondaryCards.length" class="npk-secondary" :class="secondaryGridClass">
      <article
        v-for="card in secondaryCards"
        :key="card.key"
        class="npk-card"
        :class="`is-${card.tone}`"
      >
        <p class="npk-card__label">{{ card.label }}</p>
        <p class="npk-card__value">{{ card.value }}</p>
        <p v-if="card.meta" class="npk-card__meta">{{ card.meta }}</p>
      </article>
    </section>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { formatEUR, formatNumber, formatPct } from '@/utils/formatters'

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
  if (isSalesOnly.value) {
    const valueSize = clamp(
      Math.round(Math.min(props.layout.width * 0.22, props.layout.height * 0.42)),
      34,
      72,
    )
    return {
      '--npk-gap': `${clamp(Math.round(Math.min(props.layout.width * 0.012, props.layout.height * 0.028)), 4, 7)}px`,
      '--npk-main-size': `${valueSize}px`,
      '--npk-label-size': `${clamp(Math.round(Math.min(props.layout.width * 0.013, props.layout.height * 0.034)), 9, 11)}px`,
      '--npk-sub-size': `${clamp(Math.round(Math.min(props.layout.width * 0.017, props.layout.height * 0.048)), 11, 13)}px`,
      '--npk-card-size': '13px',
    }
  }

  const mainSize = clamp(
    Math.round(Math.min(props.layout.width * 0.08, props.layout.height * 0.27)),
    24,
    54,
  )
  const mainLen = valueText.value.replace(/\s+/g, '').length
  const lenScale = mainLen >= 12 ? 0.78 : mainLen >= 10 ? 0.9 : 1

  return {
    '--npk-gap': `${clamp(Math.round(Math.min(props.layout.width * 0.012, props.layout.height * 0.045)), 4, 8)}px`,
    '--npk-main-size': `${clamp(Math.round(mainSize * lenScale), 22, 54)}px`,
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
  align-content: start;
  overflow: hidden;
}

.npk-root.has-secondary {
  grid-template-rows: auto auto auto;
  align-content: start;
}

.npk-head {
  min-width: 0;
  display: grid;
  gap: 1px;
}

.npk-overline {
  margin: 0;
  font-size: var(--npk-label-size);
  text-transform: uppercase;
  letter-spacing: 0.09em;
  font-weight: 700;
  color: #64748b;
}

.npk-period {
  margin: 0;
  font-size: var(--npk-sub-size);
  color: #475569;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.npk-main {
  min-width: 0;
  border-radius: 8px;
  border: 1px solid rgba(148, 163, 184, 0.24);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.78), rgba(248, 250, 252, 0.7)),
    radial-gradient(circle at 12% 0%, rgba(91, 92, 226, 0.08), transparent 55%);
  padding: 8px 10px;
  display: grid;
  align-content: start;
  gap: 4px;
}

.npk-main.is-positive {
  border-color: rgba(16, 185, 129, 0.28);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.8), rgba(240, 253, 244, 0.68)),
    radial-gradient(circle at 12% 0%, rgba(16, 185, 129, 0.1), transparent 58%);
}

.npk-main.is-negative {
  border-color: rgba(225, 29, 72, 0.26);
}

.npk-main__label {
  margin: 0;
  font-size: var(--npk-label-size);
  letter-spacing: 0.1em;
  text-transform: uppercase;
  font-weight: 700;
  color: #64748b;
}

.npk-main__meta {
  margin: 0;
  font-size: var(--npk-label-size);
  color: #64748b;
}

.npk-value {
  margin: 0;
  font-size: var(--npk-main-size);
  line-height: 0.94;
  letter-spacing: 0;
  font-weight: 820;
  color: #111827;
  font-variant-numeric: tabular-nums;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.npk-delta {
  display: inline-flex;
  align-items: baseline;
  flex-wrap: wrap;
  gap: 3px 6px;
}

.npk-delta__value {
  font-size: var(--npk-sub-size);
  font-weight: 730;
  color: #334155;
}

.npk-delta__pct,
.npk-delta__meta {
  font-size: var(--npk-label-size);
  color: #64748b;
}

.npk-delta.is-positive .npk-delta__value,
.npk-delta.is-positive .npk-delta__pct {
  color: #047857;
}

.npk-delta.is-negative .npk-delta__value,
.npk-delta.is-negative .npk-delta__pct {
  color: #be123c;
}

.npk-secondary {
  min-height: 0;
  display: grid;
  gap: var(--npk-gap);
  align-items: start;
  align-content: start;
}

.npk-secondary--one {
  grid-template-columns: 1fr;
}

.npk-secondary--two {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.npk-card {
  min-width: 0;
  border-radius: 8px;
  border: 1px solid rgba(148, 163, 184, 0.22);
  background: rgba(248, 250, 252, 0.78);
  padding: 6px 8px;
  display: grid;
  gap: 2px;
}

.npk-card__label {
  margin: 0;
  font-size: var(--npk-label-size);
  text-transform: uppercase;
  letter-spacing: 0.1em;
  color: #64748b;
}

.npk-card__value {
  margin: 0;
  font-size: var(--npk-card-size);
  line-height: 1;
  letter-spacing: 0;
  font-weight: 700;
  color: #111827;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.npk-card__meta {
  margin: 0;
  font-size: var(--npk-label-size);
  color: #64748b;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.npk-card.is-positive .npk-card__value {
  color: #047857;
}

.npk-card.is-negative .npk-card__value {
  color: #be123c;
}

.npk-root.is-compact .npk-main {
  padding: 7px 9px;
}

.npk-root.is-sales-only {
  grid-template-rows: auto auto;
  gap: clamp(8px, calc(var(--npk-gap) * 1.4), 12px);
}

.npk-root.is-sales-only .npk-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 10px;
  padding: 0 1px;
}

.npk-root.is-sales-only .npk-overline {
  letter-spacing: 0.08em;
}

.npk-root.is-sales-only .npk-period {
  max-width: 58%;
  text-align: right;
}

.npk-root.is-sales-only .npk-main {
  min-height: 0;
  padding: clamp(10px, 4.2%, 16px);
  gap: clamp(4px, 1.8%, 8px);
}

.npk-root.is-sales-only .npk-main__label {
  letter-spacing: 0.08em;
}

.npk-root.is-sales-only .npk-main__meta {
  font-size: var(--npk-sub-size);
}

.npk-root.is-tiny .npk-main {
  gap: 4px;
}

.npk-root.is-tiny .npk-delta {
  gap: 2px 6px;
}

.npk-root.is-tiny .npk-secondary {
  display: none;
}
</style>
