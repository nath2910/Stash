<template>
  <div class="relative h-full w-full overflow-hidden rounded-2xl text-slate-100">
    <template v-if="dataState === 'ready'">
      <div
        class="relative z-10 grid h-full min-h-0 px-4 pb-3"
        :style="{
          paddingTop: `${topInset}px`,
          gridTemplateRows: showFooter ? 'auto minmax(0,1fr) auto' : 'auto minmax(0,1fr)',
        }"
      >
        <header class="min-w-0">
          <p
            class="font-semibold uppercase tracking-[0.16em] text-white/58"
            :class="periodLabelClass"
          >
            Période
          </p>

          <p
            class="mt-1 truncate font-semibold tabular-nums text-white/96"
            :class="periodValueClass"
          >
            {{ periodText }}
          </p>
        </header>

        <section class="grid min-h-0" :class="[mainGridClass, mainSpacingClass]">
          <div class="min-w-0 flex min-h-0 flex-col justify-start">
            <p
              class="font-semibold uppercase tracking-[0.16em] text-white/60"
              :class="kpiLabelClass"
            >
              Bénéfice net
            </p>

            <div class="mt-2 min-w-0">
              <p
                class="min-w-0 max-w-full font-semibold leading-[0.96] tabular-nums tracking-[-0.05em]"
                :style="valueStyle"
              >
                {{ valueText }}
              </p>

              <p
                v-if="showDelta"
                class="mt-2 font-semibold tabular-nums"
                :class="deltaValueClass"
                :style="{ color: deltaColor }"
              >
                {{ deltaValueText }}
                <span class="text-white/64">({{ deltaPctText }})</span>
              </p>
            </div>

            <div v-if="showInlineMeta" class="mt-3 flex flex-wrap gap-2">
              <div class="rounded-xl border border-white/8 bg-white/[0.035] px-2.5 py-1.5">
                <p class="text-[9px] font-semibold uppercase tracking-[0.14em] text-white/52">
                  Pic
                </p>
                <p class="mt-0.5 text-[12px] font-semibold tabular-nums text-white/88">
                  {{ maxValueText }}
                </p>
              </div>

              <div class="rounded-xl border border-white/8 bg-white/[0.035] px-2.5 py-1.5">
                <p class="text-[9px] font-semibold uppercase tracking-[0.14em] text-white/52">
                  Marge
                </p>
                <p class="mt-0.5 text-[12px] font-semibold tabular-nums text-white/88">
                  {{ marginText }}
                </p>
              </div>
            </div>
          </div>

          <aside
            v-if="showSpark"
            class="min-w-0 rounded-2xl border border-white/8 bg-white/[0.035] px-3 py-2.5 shadow-[inset_0_1px_0_rgba(255,255,255,0.03)]"
          >
            <div
              class="mb-2 flex items-center justify-between gap-2 text-[10px] uppercase tracking-[0.12em] text-white/56"
            >
              <span class="truncate">{{ sparkTitle }}</span>

              <span
                class="shrink-0 font-semibold normal-case tracking-normal"
                :class="trendValueClass"
                :style="{ color: palette.secondaryMetric }"
              >
                {{ latestPointText }}
              </span>
            </div>

            <svg
              class="h-12 w-full"
              viewBox="0 0 176 56"
              role="img"
              :aria-label="`Tendance bénéfice net (${bucketLabel})`"
            >
              <defs>
                <linearGradient :id="areaGradientId" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="0%" :stop-color="palette.line" stop-opacity="0.24" />
                  <stop offset="100%" :stop-color="palette.line" stop-opacity="0" />
                </linearGradient>
              </defs>

              <path
                v-if="sparkGeometry.areaPath"
                :d="sparkGeometry.areaPath"
                :fill="`url(#${areaGradientId})`"
              />

              <path
                v-if="sparkGeometry.linePath"
                :d="sparkGeometry.linePath"
                :stroke="palette.line"
                stroke-width="2"
                stroke-linecap="round"
                stroke-linejoin="round"
                fill="none"
              />

              <circle
                v-if="sparkGeometry.lastPoint"
                :cx="sparkGeometry.lastPoint[0]"
                :cy="sparkGeometry.lastPoint[1]"
                r="2.25"
                :fill="palette.line"
              />
            </svg>
          </aside>
        </section>

        <footer v-if="showFooter" class="mt-3 grid gap-2.5" :class="footerGridClass">
          <article
            class="rounded-2xl border border-white/8 bg-white/[0.035] px-3 py-2.5 shadow-[inset_0_1px_0_rgba(255,255,255,0.03)]"
          >
            <p
              class="font-semibold uppercase tracking-[0.14em] text-white/54"
              :class="footerLabelClass"
            >
              Pic
            </p>

            <p
              class="mt-1 font-semibold tabular-nums"
              :class="footerValueClass"
              :style="{ color: palette.secondaryMetric }"
            >
              {{ maxValueText }}
            </p>

            <p
              v-if="showFooterSub"
              class="mt-0.5 truncate font-medium text-white/72"
              :class="footerSubClass"
            >
              {{ maxDateText }}
            </p>
          </article>

          <article
            class="rounded-2xl border border-white/8 bg-white/[0.035] px-3 py-2.5 shadow-[inset_0_1px_0_rgba(255,255,255,0.03)]"
          >
            <p
              class="font-semibold uppercase tracking-[0.14em] text-white/54"
              :class="footerLabelClass"
            >
              Marge nette
            </p>

            <p
              class="mt-1 font-semibold tabular-nums"
              :class="footerValueClass"
              :style="{ color: palette.secondaryMetric }"
            >
              {{ marginText }}
            </p>

            <p
              v-if="showFooterSub"
              class="mt-0.5 font-medium text-white/72"
              :class="footerSubClass"
            >
              sur la période
            </p>
          </article>
        </footer>
      </div>
    </template>

    <template v-else-if="dataState === 'loading'">
      <div class="relative z-10 flex h-full flex-col justify-center gap-3 px-4 py-3">
        <div class="h-2 w-28 animate-pulse rounded-full bg-white/15"></div>
        <div class="h-9 w-40 animate-pulse rounded-md bg-white/16"></div>
        <div class="h-2 w-44 animate-pulse rounded-full bg-white/12"></div>
      </div>
    </template>

    <template v-else-if="dataState === 'empty'">
      <div class="relative z-10 grid h-full place-content-center gap-1 px-4 py-3 text-center">
        <p class="text-[14px] font-medium text-white/90">Aucun résultat exploitable</p>
        <p class="text-[11px] text-white/58">Aucune vente nette sur la période.</p>
      </div>
    </template>

    <template v-else-if="dataState === 'error'">
      <div class="relative z-10 grid h-full place-content-center gap-1 px-4 py-3 text-center">
        <p class="text-[14px] font-medium text-rose-100/95">Erreur de chargement</p>
        <p class="text-[11px] text-white/58">{{ error }}</p>
      </div>
    </template>

    <template v-else>
      <div class="relative z-10 grid h-full place-content-center gap-1 px-4 py-3 text-center">
        <p class="text-[14px] font-medium text-white/90">Aucune donnée</p>
        <p class="text-[11px] text-white/58">Sélectionnez une période valide.</p>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import StatsServices from '@/services/StatsServices'
import { normalizeKpi, normalizeSeries, prevPeriod } from '@/services/statsAdapters'
import { formatDateFR, formatEUR, formatPct } from '@/utils/formatters'

type Bucket = 'day' | 'week' | 'month' | string
type Tone = 'positive' | 'neutral' | 'negative'
type DataState = 'loading' | 'ready' | 'empty' | 'no-data' | 'error'

interface WidgetProps {
  from?: string
  to?: string
  bucket?: Bucket
  categories?: string[]
  types?: string[]
  widgetWidth?: number
  widgetHeight?: number
  widgetBaseWidth?: number
  widgetBaseHeight?: number
  canvasEditMode?: boolean
}

interface KpiValue {
  value: number
  deltaPct: number | null
}

interface SeriesPoint {
  date: string
  value: number
}

const props = withDefaults(defineProps<WidgetProps>(), {
  from: '',
  to: '',
  bucket: 'week',
  categories: () => [],
  types: () => [],
  widgetWidth: 520,
  widgetHeight: 240,
  widgetBaseWidth: 520,
  widgetBaseHeight: 240,
  canvasEditMode: false,
})

const EPS = 0.0001
const areaGradientId = `np-area-${Math.random().toString(36).slice(2, 9)}`

const loading = ref(false)
const error = ref('')
const kpi = ref<KpiValue>({ value: 0, deltaPct: null })
const previousValue = ref(0)
const series = ref<SeriesPoint[]>([])
const marginRaw = ref<number | null>(null)

let req = 0

function clamp(value: number, min: number, max: number) {
  return Math.min(Math.max(value, min), max)
}

const hasValidRange = computed(
  () =>
    /^\d{4}-\d{2}-\d{2}$/.test(String(props.from ?? '')) &&
    /^\d{4}-\d{2}-\d{2}$/.test(String(props.to ?? '')),
)

function resetData() {
  kpi.value = { value: 0, deltaPct: null }
  previousValue.value = 0
  series.value = []
  marginRaw.value = null
}

async function load() {
  const id = ++req

  if (!hasValidRange.value) {
    resetData()
    error.value = ''
    loading.value = false
    return
  }

  loading.value = true
  error.value = ''

  try {
    const { from: prevFrom, to: prevTo } = prevPeriod(props.from, props.to)

    const [kNow, kPrev, s, summary] = await Promise.all([
      StatsServices.kpi('netProfit', props.from, props.to, props.categories, props.types),
      StatsServices.kpi('netProfit', prevFrom, prevTo, props.categories, props.types),
      StatsServices.series(
        'netProfit',
        props.from,
        props.to,
        props.bucket,
        props.categories,
        props.types,
      ),
      StatsServices.summary(props.from, props.to, props.categories, props.types),
    ])

    if (id !== req) return

    const prevKpi = normalizeKpi(kPrev.data)

    kpi.value = normalizeKpi(kNow.data)
    previousValue.value = Number(prevKpi.value ?? 0)
    series.value = normalizeSeries(s.data)
    marginRaw.value = summary?.data?.profitMargin == null ? null : Number(summary.data.profitMargin)
  } catch (e: unknown) {
    if (id !== req) return

    const err = e as { response?: { data?: { message?: string } }; message?: string }

    error.value =
      err?.response?.data?.message ?? err?.message ?? 'Impossible de charger les données.'
  } finally {
    if (id === req) loading.value = false
  }
}

onMounted(load)

const filtersKey = computed(() => `${props.categories.join('|')}::${props.types.join('|')}`)

watch(() => [props.from, props.to, props.bucket, filtersKey.value], load)

const currentValue = computed(() => Number(kpi.value.value ?? 0))
const deltaValue = computed(() => currentValue.value - Number(previousValue.value ?? 0))

const deltaPct = computed<number | null>(() => {
  const prevAbs = Math.abs(Number(previousValue.value ?? 0))

  if (prevAbs < EPS) {
    if (Math.abs(currentValue.value) < EPS) return 0
    return kpi.value.deltaPct == null ? null : Number(kpi.value.deltaPct)
  }

  return (deltaValue.value / prevAbs) * 100
})

const tone = computed<Tone>(() => {
  if (currentValue.value > EPS) return 'positive'
  if (currentValue.value < -EPS) return 'negative'
  return 'neutral'
})

const dataState = computed<DataState>(() => {
  if (loading.value) return 'loading'
  if (error.value) return 'error'
  if (!hasValidRange.value) return 'no-data'

  const hasSeries = series.value.length > 0
  const hasMain = Math.abs(currentValue.value) >= EPS
  const hasPrev = Math.abs(Number(previousValue.value ?? 0)) >= EPS

  if (!hasSeries && !hasMain && !hasPrev) return 'empty'
  return 'ready'
})

const palette = computed(() => {
  if (tone.value === 'positive') {
    return {
      line: '#D9E7DE',
      primaryMetric: 'rgba(219, 255, 239, 0.98)',
      secondaryMetric: 'rgba(194, 244, 219, 0.96)',
    }
  }

  if (tone.value === 'negative') {
    return {
      line: '#E6DADF',
      primaryMetric: 'rgba(255, 220, 231, 0.98)',
      secondaryMetric: 'rgba(244, 198, 213, 0.96)',
    }
  }

  return {
    line: '#E0E5EC',
    primaryMetric: 'rgba(225, 236, 255, 0.98)',
    secondaryMetric: 'rgba(199, 218, 246, 0.96)',
  }
})

const maxPoint = computed<SeriesPoint | null>(() => {
  if (!series.value.length) return null
  return series.value.reduce(
    (best, point) => (point.value > best.value ? point : best),
    series.value[0],
  )
})

const latestPoint = computed<SeriesPoint | null>(() => {
  if (!series.value.length) return null
  return series.value[series.value.length - 1]
})

const marginPct = computed<number | null>(() => {
  const raw = Number(marginRaw.value)
  if (!Number.isFinite(raw)) return null
  return Math.abs(raw) <= 1 ? raw * 100 : raw
})

const sparkValues = computed(() => series.value.slice(-24).map((point) => Number(point.value ?? 0)))

const sparkGeometry = computed(() => {
  const width = 176
  const height = 56
  const pad = 6
  const values = sparkValues.value

  if (!values.length) {
    return { linePath: '', areaPath: '', lastPoint: null as [number, number] | null }
  }

  if (values.length === 1) {
    const y = Math.round(height / 2)
    const linePath = `M${pad},${y} L${width - pad},${y}`
    const areaPath = `M${pad},${height - pad} L${pad},${y} L${width - pad},${y} L${width - pad},${height - pad} Z`
    return { linePath, areaPath, lastPoint: [width - pad, y] as [number, number] }
  }

  const min = Math.min(...values)
  const max = Math.max(...values)
  const range = max - min || 1
  const step = (width - pad * 2) / (values.length - 1)

  const points = values.map((value, index) => {
    const x = pad + index * step
    const y = pad + ((max - value) / range) * (height - pad * 2)
    return [Number(x.toFixed(2)), Number(y.toFixed(2))] as [number, number]
  })

  const linePath = points
    .map((point, index) => `${index ? 'L' : 'M'}${point[0]},${point[1]}`)
    .join(' ')

  const first = points[0]
  const last = points[points.length - 1]
  const areaPath = `${linePath} L${last[0]},${height - pad} L${first[0]},${height - pad} Z`

  return { linePath, areaPath, lastPoint: last }
})

const bucketLabel = computed(() => {
  if (props.bucket === 'day') return 'jour'
  if (props.bucket === 'month') return 'mois'
  return 'semaine'
})

const topInset = computed(() => (props.canvasEditMode ? 0 : 12))
const layoutWidth = computed(() => Math.max(0, Number(props.widgetWidth ?? 520) - 24))
const layoutHeight = computed(
  () => Math.max(0, Number(props.widgetHeight ?? 240) - topInset.value - 16),
)

const showDelta = computed(() => layoutHeight.value >= 170)

const showSpark = computed(() => {
  if (sparkValues.value.length < 2) return false
  return layoutWidth.value >= 520 && layoutHeight.value >= 165
})

const showFooter = computed(() => layoutWidth.value >= 520 && layoutHeight.value >= 240)
const showFooterSub = computed(() => layoutHeight.value >= 275)
const showInlineMeta = computed(() => !showFooter.value && layoutHeight.value >= 180)

const mainSpacingClass = computed(() =>
  layoutHeight.value < 185 ? 'mt-2 gap-2.5' : 'mt-3 gap-3.5',
)

const mainGridClass = computed(() => {
  if (showSpark.value && layoutWidth.value >= 700) {
    return 'grid-cols-[minmax(0,1fr)_200px] items-end'
  }

  return 'grid-cols-1 items-start'
})

const footerGridClass = computed(() => (layoutWidth.value < 620 ? 'grid-cols-1' : 'grid-cols-2'))

const valueStyle = computed(() => {
  const sizeByWidth = layoutWidth.value * 0.11
  const sizeByHeight = showFooter.value
    ? layoutHeight.value * 0.18
    : showSpark.value
      ? layoutHeight.value * 0.22
      : layoutHeight.value * 0.24
  const valueLen = formatEUR(currentValue.value, { compact: true, digits: 0 })
    .replace(/\s+/g, '')
    .length
  const lengthScale = valueLen >= 13 ? 0.72 : valueLen >= 11 ? 0.8 : valueLen >= 9 ? 0.9 : 1
  const size = clamp(Math.min(sizeByWidth, sizeByHeight) * lengthScale, 22, 54)

  return {
    fontSize: `${Math.round(size)}px`,
    color: palette.value.primaryMetric,
    textShadow: '0 10px 22px rgba(6, 10, 18, 0.34)',
  }
})

const sparkTitle = computed(() => {
  if (props.bucket === 'day') return 'Tendance jour'
  if (props.bucket === 'month') return 'Tendance mois'
  return layoutWidth.value < 620 ? 'Tendance sem.' : 'Tendance semaine'
})

const periodText = computed(() => {
  const shortYear = layoutWidth.value < 560 || layoutHeight.value < 170

  const fromLabel = formatDateFR(props.from, {
    day: '2-digit',
    month: 'short',
    year: shortYear ? '2-digit' : 'numeric',
  })

  const toLabel = formatDateFR(props.to, {
    day: '2-digit',
    month: 'short',
    year: shortYear ? '2-digit' : 'numeric',
  })

  return `${fromLabel} au ${toLabel}`
})

const valueText = computed(() =>
  formatEUR(currentValue.value, {
    compact: true,
    digits: 0,
  }),
)

function signedCurrency(value: number) {
  const sign = value > 0 ? '+' : ''
  return `${sign}${formatEUR(value, { compact: true, digits: 1 })}`
}

const deltaValueText = computed(() => signedCurrency(deltaValue.value))

const deltaPctText = computed(() => {
  if (deltaPct.value == null || !Number.isFinite(deltaPct.value)) return 'n/d'
  const sign = deltaPct.value > 0 ? '+' : ''
  return `${sign}${deltaPct.value.toFixed(1)}%`
})

const deltaColor = computed(() => {
  if (
    deltaPct.value == null ||
    !Number.isFinite(deltaPct.value) ||
    Math.abs(deltaPct.value) < EPS
  ) {
    return 'rgba(240, 244, 252, 0.92)'
  }

  return deltaPct.value > 0 ? 'rgba(216, 244, 231, 0.97)' : 'rgba(248, 220, 227, 0.97)'
})

const maxValueText = computed(() =>
  maxPoint.value ? formatEUR(maxPoint.value.value, { compact: true, digits: 1 }) : '--',
)

const maxDateText = computed(() =>
  maxPoint.value
    ? formatDateFR(maxPoint.value.date, { day: '2-digit', month: 'short', year: 'numeric' })
    : 'aucun pic',
)

const latestPointText = computed(() =>
  latestPoint.value ? formatEUR(latestPoint.value.value, { compact: true, digits: 1 }) : '--',
)

const marginText = computed(() =>
  marginPct.value == null ? 'n/d' : formatPct(marginPct.value, { digits: 1 }),
)

const periodLabelClass = computed(() => 'text-[10px]')
const periodValueClass = computed(() => (layoutWidth.value < 560 ? 'text-[13px]' : 'text-[16px]'))
const kpiLabelClass = computed(() => (layoutWidth.value < 560 ? 'text-[10px]' : 'text-[11px]'))
const deltaValueClass = computed(() => (layoutWidth.value < 560 ? 'text-[11px]' : 'text-[13px]'))
const trendValueClass = computed(() => (layoutWidth.value < 720 ? 'text-[12px]' : 'text-[13px]'))
const footerLabelClass = computed(() => (layoutWidth.value < 560 ? 'text-[10px]' : 'text-[11px]'))
const footerValueClass = computed(() => (layoutWidth.value < 560 ? 'text-[15px]' : 'text-[18px]'))
const footerSubClass = computed(() => (layoutWidth.value < 560 ? 'text-[10px]' : 'text-[11px]'))
</script>
