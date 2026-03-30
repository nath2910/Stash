<template>
  <div
    class="relative h-full w-full overflow-hidden rounded-[30px] px-4 py-3.5 text-slate-50"
    :style="shellStyle"
  >
    <div
      class="pointer-events-none absolute inset-0 opacity-[0.02] [background-image:radial-gradient(rgba(255,255,255,0.9)_0.45px,transparent_0.45px)] [background-size:4px_4px]"
      aria-hidden="true"
    ></div>
    <div
      class="pointer-events-none absolute inset-0 bg-[linear-gradient(138deg,rgba(245,248,255,0.28)_0%,rgba(233,239,249,0.12)_34%,rgba(177,188,205,0.16)_68%,rgba(138,151,172,0.22)_100%)]"
      aria-hidden="true"
    ></div>
    <div
      class="pointer-events-none absolute inset-x-0 top-0 h-[42%] bg-[linear-gradient(180deg,rgba(255,255,255,0.42)_0%,rgba(255,255,255,0.16)_42%,rgba(255,255,255,0)_100%)]"
      aria-hidden="true"
    ></div>
    <div
      class="pointer-events-none absolute -left-[14%] -top-12 h-36 w-[62%] rounded-full bg-white/[0.2] blur-[44px]"
      aria-hidden="true"
    ></div>
    <div
      class="pointer-events-none absolute right-[4%] top-[22%] h-32 w-44 rounded-full blur-[44px]"
      :style="{ background: palette.tint }"
      aria-hidden="true"
    ></div>
    <div
      class="pointer-events-none absolute inset-x-0 -bottom-16 h-36 bg-[radial-gradient(ellipse_at_center,rgba(26,36,50,0.16)_0%,rgba(26,36,50,0)_72%)]"
      aria-hidden="true"
    ></div>
    <div
      class="pointer-events-none absolute left-[8%] bottom-[8%] h-16 w-40 rounded-full bg-[rgba(201,215,186,0.12)] blur-[34px]"
      aria-hidden="true"
    ></div>
    <div
      class="pointer-events-none absolute right-[9%] bottom-[10%] h-16 w-44 rounded-full bg-[rgba(176,201,199,0.11)] blur-[36px]"
      aria-hidden="true"
    ></div>
    <div
      class="pointer-events-none absolute inset-0 bg-[linear-gradient(138deg,rgba(255,255,255,0.22)_0%,rgba(255,255,255,0.06)_44%,rgba(255,255,255,0)_76%)] mix-blend-screen opacity-68"
      aria-hidden="true"
    ></div>

    <template v-if="dataState === 'ready'">
      <div
        class="relative z-10 grid h-full min-h-0 gap-3.5"
        :class="[rootRowsClass, canvasEditMode ? 'pt-9' : '']"
      >
        <header class="flex items-start">
          <div class="min-w-0">
            <p class="font-semibold uppercase tracking-[0.16em] text-white/66" :class="periodLabelClass">Période</p>
            <p class="mt-1 truncate font-semibold tabular-nums text-white/96" :class="periodValueClass">{{ periodCompact }}</p>
          </div>
        </header>

        <section :class="[mainGridClass, mainAlignClass]" class="grid min-h-0">
          <div class="min-w-0" :class="showSpark ? 'self-end' : 'self-start'">
            <p class="font-semibold uppercase tracking-[0.16em] text-white/64" :class="kpiLabelClass">Bénéfice net</p>
            <p
              class="mt-1 font-semibold leading-[0.9] tracking-[-0.045em] tabular-nums"
              :class="valueSizeClass"
              :style="{ color: palette.primaryMetric, textShadow: '0 10px 22px rgba(6, 10, 18, 0.36)' }"
            >
              {{ valueText }}
            </p>
            <p class="mt-1.5 font-semibold tabular-nums" :class="deltaValueClass" :style="{ color: deltaColor }">
              {{ deltaValueText }}
              <span class="text-white/70">({{ deltaPctShort }})</span>
            </p>
          </div>

          <div v-if="showSpark" class="flex min-w-0 flex-col justify-end gap-1.5">
            <div class="flex items-center justify-between gap-2 text-[10px] uppercase tracking-[0.12em] text-white/62">
              <span class="truncate">Tendance {{ bucketLabel }}</span>
              <span class="font-semibold normal-case tracking-normal" :class="trendValueClass" :style="{ color: palette.secondaryMetric }">{{ latestPointText }}</span>
            </div>

            <svg
              class="h-14 w-full"
              viewBox="0 0 176 56"
              role="img"
              :aria-label="`Tendance bénéfice net (${bucketLabel})`"
            >
              <defs>
                <linearGradient :id="areaGradientId" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="0%" :stop-color="palette.line" stop-opacity="0.28" />
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
                r="2.3"
                :fill="palette.line"
              />
            </svg>
          </div>

          <div v-else class="flex items-end font-medium text-white/78" :class="trendEmptyClass">Pas de tendance disponible.</div>
        </section>

        <footer :class="footerGridClass" class="grid items-start pt-1.5">
          <div class="min-w-0">
            <p class="font-semibold uppercase tracking-[0.14em] text-white/60" :class="footerLabelClass">Pic</p>
            <p class="mt-1 font-semibold tabular-nums" :class="footerValueClass" :style="{ color: palette.secondaryMetric }">{{ maxValueText }}</p>
            <p class="mt-0.5 truncate font-medium text-white/76" :class="footerSubClass">{{ maxDateText }}</p>
          </div>

          <div class="min-w-0" :class="sizeMode === 'small' ? 'pt-1' : ''">
            <p class="font-semibold uppercase tracking-[0.14em] text-white/60" :class="footerLabelClass">Marge nette</p>
            <p class="mt-1 font-semibold tabular-nums" :class="footerValueClass" :style="{ color: palette.secondaryMetric }">{{ marginText }}</p>
            <p class="mt-0.5 font-medium text-white/76" :class="footerSubClass">sur la période</p>
          </div>
        </footer>
      </div>
    </template>

    <template v-else-if="dataState === 'loading'">
      <div class="relative z-10 flex h-full flex-col justify-center gap-3">
        <div class="h-2 w-28 animate-pulse rounded-full bg-white/15"></div>
        <div class="h-9 w-40 animate-pulse rounded-md bg-white/16"></div>
        <div class="h-2 w-44 animate-pulse rounded-full bg-white/12"></div>
      </div>
    </template>

    <template v-else-if="dataState === 'empty'">
      <div class="relative z-10 grid h-full place-content-center gap-1 text-center">
        <p class="text-[14px] font-medium text-white/90">Aucun résultat exploitable</p>
        <p class="text-[11px] text-white/58">Aucune vente nette sur la période.</p>
      </div>
    </template>

    <template v-else-if="dataState === 'error'">
      <div class="relative z-10 grid h-full place-content-center gap-1 text-center">
        <p class="text-[14px] font-medium text-rose-100/95">Erreur de chargement</p>
        <p class="text-[11px] text-white/58">{{ error }}</p>
      </div>
    </template>

    <template v-else>
      <div class="relative z-10 grid h-full place-content-center gap-1 text-center">
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
type SizeMode = 'small' | 'medium' | 'large' | 'xlarge'
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

const loading = ref(false)
const error = ref('')
const kpi = ref<KpiValue>({ value: 0, deltaPct: null })
const previousValue = ref(0)
const series = ref<SeriesPoint[]>([])
const marginRaw = ref<number | null>(null)
let req = 0

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
      StatsServices.series('netProfit', props.from, props.to, props.bucket, props.categories, props.types),
      StatsServices.summary(props.from, props.to, props.categories, props.types),
    ])

    if (id !== req) return

    const prevKpi = normalizeKpi(kPrev.data)
    const summaryProfitMargin = summary?.data?.profitMargin

    kpi.value = normalizeKpi(kNow.data)
    previousValue.value = Number(prevKpi.value ?? 0)
    series.value = normalizeSeries(s.data)
    marginRaw.value = summaryProfitMargin == null ? null : Number(summaryProfitMargin)
  } catch (e: unknown) {
    if (id !== req) return
    const err = e as { response?: { data?: { message?: string } }; message?: string }
    error.value = err?.response?.data?.message ?? err?.message ?? 'Impossible de charger les données.'
  } finally {
    if (id === req) loading.value = false
  }
}

onMounted(load)
const filtersKey = computed(() => `${props.categories.join('|')}::${props.types.join('|')}`)
watch(
  () => [props.from, props.to, props.bucket, filtersKey.value],
  load,
)

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

const sizeMode = computed<SizeMode>(() => {
  const w = Number(props.widgetWidth ?? 520)
  const h = Number(props.widgetHeight ?? 240)
  if (w < 430 || h < 178) return 'small'
  if (w < 640 || h < 228) return 'medium'
  if (w < 820 || h < 320) return 'large'
  return 'xlarge'
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
      primaryMetric: 'rgba(215, 255, 235, 0.98)',
      secondaryMetric: 'rgba(194, 244, 219, 0.96)',
      tint: 'radial-gradient(circle at center, rgba(191, 212, 201, 0.18), rgba(191, 212, 201, 0.01) 72%)',
    }
  }

  if (tone.value === 'negative') {
    return {
      line: '#E6DADF',
      primaryMetric: 'rgba(255, 220, 231, 0.98)',
      secondaryMetric: 'rgba(244, 198, 213, 0.96)',
      tint: 'radial-gradient(circle at center, rgba(208, 192, 198, 0.16), rgba(208, 192, 198, 0.01) 72%)',
    }
  }

  return {
    line: '#E0E5EC',
    primaryMetric: 'rgba(225, 236, 255, 0.98)',
    secondaryMetric: 'rgba(199, 218, 246, 0.96)',
    tint: 'radial-gradient(circle at center, rgba(196, 203, 214, 0.18), rgba(196, 203, 214, 0.01) 74%)',
  }
})

const shellStyle = computed(() => ({
  background: 'rgba(173, 185, 202, 0.22)',
  backdropFilter: 'blur(38px) saturate(142%) brightness(1.06)',
  WebkitBackdropFilter: 'blur(38px) saturate(142%) brightness(1.06)',
  boxShadow:
    'inset 0 1px 0 rgba(255,255,255,0.72), inset 0 -1px 0 rgba(255,255,255,0.16), inset 10px 0 24px rgba(255,255,255,0.04), inset -12px -18px 28px rgba(33,43,57,0.12), 0 12px 24px rgba(5,10,20,0.18)',
  fontFamily: '"SF Pro Display", "Inter", "Segoe UI", sans-serif',
}))

const maxPoint = computed<SeriesPoint | null>(() => {
  if (!series.value.length) return null
  return series.value.reduce((best, point) => (point.value > best.value ? point : best), series.value[0])
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

  if (!values.length) return { linePath: '', areaPath: '', lastPoint: null as [number, number] | null }

  if (values.length === 1) {
    const y = Math.round(height * 0.5)
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

  const linePath = points.map((point, index) => `${index ? 'L' : 'M'}${point[0]},${point[1]}`).join(' ')
  const first = points[0]
  const last = points[points.length - 1]
  const areaPath = `${linePath} L${last[0]},${height - pad} L${first[0]},${height - pad} Z`

  return { linePath, areaPath, lastPoint: last }
})

const areaGradientId = `np-area-${Math.random().toString(36).slice(2, 9)}`

const valueText = computed(() =>
  formatEUR(currentValue.value, {
    compact: sizeMode.value !== 'large',
    digits: sizeMode.value === 'small' ? 1 : 0,
  }),
)

const signedCurrency = (value: number) => {
  const sign = value > 0 ? '+' : ''
  return `${sign}${formatEUR(value, { compact: true, digits: 1 })}`
}

const deltaValueText = computed(() => signedCurrency(deltaValue.value))

const deltaPctShort = computed(() => {
  if (deltaPct.value == null || !Number.isFinite(deltaPct.value)) return 'n/d'
  const sign = deltaPct.value > 0 ? '+' : ''
  return `${sign}${deltaPct.value.toFixed(1)}%`
})

const deltaColor = computed(() => {
  if (deltaPct.value == null || !Number.isFinite(deltaPct.value) || Math.abs(deltaPct.value) < EPS) {
    return 'rgba(240, 244, 252, 0.92)'
  }
  return deltaPct.value > 0 ? 'rgba(216, 244, 231, 0.97)' : 'rgba(248, 220, 227, 0.97)'
})

const periodCompact = computed(() => {
  const fromLabel = formatDateFR(props.from, {
    day: '2-digit',
    month: 'short',
    year: sizeMode.value === 'small' ? '2-digit' : 'numeric',
  })
  const toLabel = formatDateFR(props.to, {
    day: '2-digit',
    month: 'short',
    year: sizeMode.value === 'small' ? '2-digit' : 'numeric',
  })
  return `${fromLabel} au ${toLabel}`
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

const valueSizeClass = computed(() => {
  if (sizeMode.value === 'small') return 'text-[38px]'
  if (sizeMode.value === 'medium') return 'text-[48px]'
  if (sizeMode.value === 'large') return 'text-[62px]'
  return 'text-[84px]'
})

const rootRowsClass = computed(() => {
  if (showSpark.value) return 'grid-rows-[auto,1fr,auto]'
  return 'grid-rows-[auto,auto,auto]'
})

const mainAlignClass = computed(() => {
  if (showSpark.value) return 'items-end'
  if (sizeMode.value === 'xlarge') return 'items-center'
  return 'items-start'
})

const mainGridClass = computed(() => {
  if (sizeMode.value === 'small') return 'grid-cols-1 gap-3'
  if (sizeMode.value === 'medium') return 'grid-cols-[minmax(0,1fr)_164px] gap-3.5'
  if (sizeMode.value === 'large') return 'grid-cols-[minmax(0,1fr)_184px] gap-4'
  return 'grid-cols-[minmax(0,1fr)_232px] gap-6'
})

const footerGridClass = computed(() => {
  if (sizeMode.value === 'small') return 'grid-cols-1 gap-3'
  if (sizeMode.value === 'xlarge') return 'grid-cols-2 gap-7'
  return 'grid-cols-2 gap-5'
})

const periodLabelClass = computed(() => (sizeMode.value === 'xlarge' ? 'text-[12px]' : 'text-[10px]'))
const periodValueClass = computed(() => (sizeMode.value === 'xlarge' ? 'text-[17px]' : 'text-[12px]'))
const kpiLabelClass = computed(() => (sizeMode.value === 'xlarge' ? 'text-[12px]' : 'text-[10px]'))
const deltaValueClass = computed(() => (sizeMode.value === 'xlarge' ? 'text-[16px]' : 'text-[12px]'))
const trendValueClass = computed(() => (sizeMode.value === 'xlarge' ? 'text-[16px]' : 'text-[12px]'))
const trendEmptyClass = computed(() => (sizeMode.value === 'xlarge' ? 'text-[14px]' : 'text-[11px]'))
const footerLabelClass = computed(() => (sizeMode.value === 'xlarge' ? 'text-[12px]' : 'text-[10px]'))
const footerValueClass = computed(() => {
  if (sizeMode.value === 'xlarge') return 'text-[24px]'
  if (sizeMode.value === 'large') return 'text-[17px]'
  return 'text-[15px]'
})
const footerSubClass = computed(() => (sizeMode.value === 'xlarge' ? 'text-[14px]' : 'text-[11px]'))

const showSpark = computed(() => {
  if (!sparkValues.value.length) return false
  return sizeMode.value !== 'small' || Number(props.widgetWidth ?? 0) >= 390
})

const bucketLabel = computed(() => {
  if (props.bucket === 'day') return 'jour'
  if (props.bucket === 'month') return 'mois'
  return 'semaine'
})
</script>
