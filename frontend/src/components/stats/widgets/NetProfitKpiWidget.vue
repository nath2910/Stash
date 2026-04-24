<template>
  <section class="npk-root" :class="layoutClass" :style="layoutVars">
    <header class="npk-head">
      <p class="npk-overline">Periode analysee</p>
      <p class="npk-period" :title="periodText">{{ periodText }}</p>
    </header>

    <section class="npk-main" :class="`is-${valueTone}`">
      <p class="npk-main__label">Benefice net</p>
      <p class="npk-value">{{ valueText }}</p>
      <div v-if="showComparison" class="npk-delta" :class="`is-${deltaTone}`">
        <span class="npk-delta__value">{{ deltaValueText }}</span>
        <span class="npk-delta__pct">{{ deltaPctText }}</span>
        <span class="npk-delta__meta">vs periode precedente</span>
      </div>
      <p v-else class="npk-main__meta">Total net sur la periode</p>
    </section>

    <section v-if="secondaryCards.length" class="npk-secondary" :class="secondaryGridClass">
      <article
        v-for="card in secondaryCards"
        :key="card.key"
        class="npk-card"
        :class="`is-${card.tone}`"
      >
        <p class="npk-card__label">{{ card.label }}</p>
        <p></p>
        <p></p>
        <p></p>

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
  }>(),
  {
    showComparison: true,
    showSalesKpi: true,
    showAvgProfitPerSale: true,
    showNetMargin: true,
    showBestPeriod: true,
    secondaryLimit: 4,
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
const valueTone = computed(() => toneFromValue(props.metrics.value))
const showComparison = computed(() => props.showComparison !== false)

const valueText = computed(() => {
  const compact = props.layout.mode !== 'large' || Math.abs(props.metrics.value) >= 1_000_000
  return formatEUR(props.metrics.value, { compact, digits: compact ? 1 : 0 })
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

  if (props.showAvgProfitPerSale !== false && props.metrics.avgProfitPerSale != null) {
    cards.push({
      key: 'avg-profit',
      label: 'Benefice moyen',
      value: formatEUR(props.metrics.avgProfitPerSale, { compact: true, digits: 1 }),
      meta: props.metrics.salesCount
        ? `${formatNumber(props.metrics.salesCount, { digits: 0 })} ventes`
        : '',
      tone: toneFromValue(props.metrics.avgProfitPerSale),
    })
  }

  if (props.showSalesKpi !== false && props.metrics.salesCount != null) {
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
  'has-secondary': secondaryCards.value.length > 0,
}))

const layoutVars = computed(() => {
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
  color: rgba(148, 163, 184, 0.82);
}

.npk-period {
  margin: 0;
  font-size: var(--npk-sub-size);
  color: rgba(203, 213, 225, 0.92);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.npk-main {
  min-width: 0;
  border-radius: 12px;
  border: 1px solid rgba(148, 163, 184, 0.22);
  background:
    linear-gradient(180deg, rgba(15, 23, 42, 0.56), rgba(15, 23, 42, 0.44)),
    radial-gradient(circle at 12% 0%, rgba(59, 130, 246, 0.08), transparent 55%);
  padding: 8px 10px;
  display: grid;
  gap: 4px;
}

.npk-main.is-positive {
  border-color: rgba(0, 210, 106, 0.45);
  background:
    linear-gradient(180deg, rgba(15, 23, 42, 0.56), rgba(15, 23, 42, 0.44)),
    radial-gradient(circle at 12% 0%, rgba(0, 210, 106, 0.16), transparent 58%);
}

.npk-main.is-negative {
  border-color: rgba(225, 29, 72, 0.36);
}

.npk-main__label {
  margin: 0;
  font-size: var(--npk-label-size);
  letter-spacing: 0.1em;
  text-transform: uppercase;
  font-weight: 700;
  color: rgba(148, 163, 184, 0.82);
}

.npk-main__meta {
  margin: 0;
  font-size: var(--npk-label-size);
  color: rgba(148, 163, 184, 0.88);
}

.npk-value {
  margin: 0;
  font-size: var(--npk-main-size);
  line-height: 0.94;
  letter-spacing: -0.04em;
  font-weight: 760;
  color: rgba(248, 250, 252, 0.98);
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
  color: rgba(226, 232, 240, 0.96);
}

.npk-delta__pct,
.npk-delta__meta {
  font-size: var(--npk-label-size);
  color: rgba(148, 163, 184, 0.92);
}

.npk-delta.is-positive .npk-delta__value,
.npk-delta.is-positive .npk-delta__pct {
  color: rgba(52, 255, 156, 0.98);
}

.npk-delta.is-negative .npk-delta__value,
.npk-delta.is-negative .npk-delta__pct {
  color: rgba(251, 146, 160, 0.97);
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
  border-radius: 10px;
  border: 1px solid rgba(148, 163, 184, 0.2);
  background: rgba(15, 23, 42, 0.34);
  padding: 6px 8px;
  display: grid;
  gap: 2px;
}

.npk-card__label {
  margin: 0;
  font-size: var(--npk-label-size);
  text-transform: uppercase;
  letter-spacing: 0.1em;
  color: rgba(148, 163, 184, 0.86);
}

.npk-card__value {
  margin: 0;
  font-size: var(--npk-card-size);
  line-height: 1;
  letter-spacing: -0.02em;
  font-weight: 700;
  color: rgba(241, 245, 249, 0.97);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.npk-card__meta {
  margin: 0;
  font-size: var(--npk-label-size);
  color: rgba(148, 163, 184, 0.82);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.npk-card.is-positive .npk-card__value {
  color: rgba(52, 255, 156, 0.97);
}

.npk-card.is-negative .npk-card__value {
  color: rgba(253, 164, 175, 0.96);
}

.npk-root.is-compact .npk-main {
  padding: 7px 9px;
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
