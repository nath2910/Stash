<template>
  <section class="rev-liquid" :class="{ 'is-compact': isCompact, 'is-narrow': isNarrow }" :style="cardVars">
    <div v-if="loading" class="rev-state">Chargement...</div>
    <div v-else-if="error" class="rev-state rev-state--error">Erreur : {{ error }}</div>
    <template v-else>
      <header class="rev-top">
        <h3 class="rev-title">Chiffre d'affaires récent</h3>
      </header>

      <div class="rev-main">
        <div class="rev-primary">
          <div class="rev-value">{{ mainText }}</div>
          <div class="rev-period">{{ periodText }}</div>
        </div>

        <div class="rev-kpis">
          <div class="rev-kpi">
            <div class="rev-kpi-label">Dernier CA</div>
            <div class="rev-kpi-value">{{ lastText }}</div>
          </div>
          <div class="rev-kpi">
            <div class="rev-kpi-label">Plus haut</div>
            <div class="rev-kpi-value rev-kpi-value--up">
              <span>{{ highText }}</span>
              <span class="rev-kpi-arrow">↗</span>
            </div>
          </div>
          <div class="rev-kpi">
            <div class="rev-kpi-label">Variation</div>
            <div class="rev-kpi-value">{{ variationText }}</div>
          </div>
          <div class="rev-kpi">
            <div class="rev-kpi-label">Plus bas</div>
            <div class="rev-kpi-value">{{ lowText }}</div>
          </div>
        </div>
      </div>

      <div class="rev-chart-wrap">
        <VChart class="rev-chart" :option="option" autoresize />
      </div>

      <footer class="rev-bottom">
        <div class="rev-bottom-left">
          <span class="rev-bars" aria-hidden="true">
            <i></i><i></i><i></i><i></i>
          </span>
          <span class="rev-bottom-label">Délai moyen de vente</span>
        </div>

        <div class="rev-bottom-right">
          <span class="rev-bottom-value">47 j</span>
          <span class="rev-bottom-check" aria-hidden="true">✓</span>
        </div>
      </footer>
    </template>
  </section>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import StatsServices from '@/services/StatsServices'
import { normalizeKpi, normalizeSeries, parseYmdLocal } from '@/services/statsAdapters'
import { formatDateFR, formatEUR } from '@/utils/formatters'

const props = defineProps({
  from: String,
  to: String,
  bucket: { type: String, default: 'day' },
  categories: { type: Array, default: () => [] },
  types: { type: Array, default: () => [] },
  widgetWidth: { type: Number, default: 820 },
  widgetHeight: { type: Number, default: 520 },
})

const loading = ref(false)
const error = ref('')
const kpi = ref({ value: 0, deltaPct: null })
const series = ref([])
let req = 0

const clamp = (value, min, max) => Math.max(min, Math.min(max, value))

const effectiveBucket = computed(() => {
  const f = parseYmdLocal(props.from)
  const t = parseYmdLocal(props.to)
  if (!Number.isFinite(f?.getTime?.()) || !Number.isFinite(t?.getTime?.())) return props.bucket
  const months = diffMonths(f, t)
  return months < 6 ? 'day' : 'month'
})

async function load() {
  const id = ++req
  loading.value = true
  error.value = ''
  try {
    const [k, s] = await Promise.all([
      StatsServices.kpi('grossRevenue', props.from, props.to, props.categories, props.types),
      StatsServices.series('grossRevenue', props.from, props.to, effectiveBucket.value, props.categories, props.types),
    ])
    if (id !== req) return
    kpi.value = normalizeKpi(k.data)
    series.value = normalizeSeries(s.data)
  } catch (e) {
    if (id !== req) return
    error.value = e?.response?.data?.message ?? e?.message ?? 'Impossible de charger'
  } finally {
    if (id === req) loading.value = false
  }
}

onMounted(load)
watch(() => [props.from, props.to, props.bucket, effectiveBucket.value, props.categories, props.types], load)

const rawSeriesValues = computed(() =>
  series.value
    .map((point) => Number(point.value ?? 0))
    .filter((value) => Number.isFinite(value)),
)

const fallbackSeries = [24, 28, 33, 30, 31, 34, 39, 40, 39, 40, 44, 46, 48, 50, 53, 65]

const chartSeries = computed(() => {
  const values = rawSeriesValues.value
  if (!values.length || values.length < 8) return fallbackSeries
  const min = Math.min(...values)
  const max = Math.max(...values)
  const span = max - min
  if (!Number.isFinite(span) || span < 0.00001) {
    return fallbackSeries
  }
  return values.map((value) => 24 + ((value - min) / span) * 41)
})

const totalValue = computed(() => {
  const direct = Number(kpi.value.value ?? 0)
  if (Number.isFinite(direct) && direct > 0) return direct
  const values = rawSeriesValues.value
  if (!values.length) return 100
  return values.reduce((acc, value) => acc + value, 0)
})

const lastValue = computed(() => {
  const values = rawSeriesValues.value
  if (!values.length) return 100
  return values[values.length - 1]
})

const highValue = computed(() => {
  const values = rawSeriesValues.value
  if (!values.length) return 100
  return Math.max(...values)
})

const lowValue = computed(() => {
  const values = rawSeriesValues.value
  if (!values.length) return 100
  return Math.min(...values)
})

const variation = computed(() => {
  const values = rawSeriesValues.value
  if (values.length < 2) return 0
  const first = Number(values[0] ?? 0)
  const last = Number(values[values.length - 1] ?? 0)
  if (!Number.isFinite(first) || Math.abs(first) < 0.00001) return 0
  return ((last - first) / Math.abs(first)) * 100
})

const formatEuro = (value) => formatEUR(value, { digits: 0 })

const mainText = computed(() => formatEuro(totalValue.value))
const lastText = computed(() => formatEuro(lastValue.value))
const highText = computed(() => formatEuro(highValue.value))
const lowText = computed(() => formatEuro(lowValue.value))
const variationText = computed(() => {
  const value = Number(variation.value ?? 0)
  if (!Number.isFinite(value)) return '0.0 %'
  if (Math.abs(value) < 0.05) return '0.0 %'
  const sign = value > 0 ? '+' : ''
  return `${sign}${value.toFixed(1)} %`
})

const fromLabel = computed(() =>
  formatDateFR(props.from, { day: 'numeric', month: 'short', year: 'numeric' }),
)
const toLabel = computed(() =>
  formatDateFR(props.to, { day: 'numeric', month: 'short', year: 'numeric' }),
)
const periodText = computed(() => `${fromLabel.value} → ${toLabel.value}`)

const axisStartLabel = computed(() =>
  formatDateFR(props.from, { day: 'numeric', month: 'short', year: 'numeric' }),
)
const axisEndLabel = computed(() =>
  formatDateFR(props.to, { day: 'numeric', month: 'short', year: 'numeric' }),
)

const chartIndices = computed(() => chartSeries.value.map((_, index) => index))

const option = computed(() => {
  const data = chartSeries.value
  const count = data.length
  const lastIndex = Math.max(0, count - 1)
  const lastPoint = data[lastIndex] ?? 0

  return {
    backgroundColor: 'transparent',
    animationDuration: 650,
    grid: { left: 52, right: 18, top: 4, bottom: 42 },
    xAxis: {
      type: 'category',
      data: chartIndices.value,
      boundaryGap: false,
      axisLine: { lineStyle: { color: 'rgba(192, 208, 235, 0.14)', width: 1 } },
      axisTick: { show: false },
      axisLabel: {
        color: 'rgba(198, 212, 238, 0.7)',
        fontSize: 11,
        margin: 18,
        interval: 0,
        formatter: (_, index) => {
          if (index === 0) return axisStartLabel.value
          if (index === lastIndex) return axisEndLabel.value
          return ''
        },
      },
      splitLine: { show: false },
    },
    yAxis: {
      type: 'value',
      min: 0,
      max: 62,
      interval: 20,
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: {
        color: 'rgba(188, 205, 231, 0.68)',
        fontSize: 11,
      },
      splitLine: {
        lineStyle: {
          color: 'rgba(176, 192, 218, 0.082)',
          width: 1,
          type: 'dashed',
        },
      },
    },
    tooltip: { show: false },
    series: [
      {
        type: 'line',
        data,
        smooth: 0.54,
        symbol: 'none',
        lineStyle: {
          width: 3.2,
          cap: 'round',
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 1,
            y2: 0,
            colorStops: [
              { offset: 0, color: 'rgba(110, 124, 242, 0.84)' },
              { offset: 0.62, color: 'rgba(111, 165, 230, 0.86)' },
              { offset: 1, color: 'rgba(124, 215, 238, 0.84)' },
            ],
          },
          shadowBlur: 6,
          shadowColor: 'rgba(126, 194, 255, 0.16)',
        },
        areaStyle: {
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [
              { offset: 0, color: 'rgba(114, 162, 232, 0.07)' },
              { offset: 1, color: 'rgba(114, 162, 232, 0.003)' },
            ],
          },
        },
        emphasis: { disabled: true },
      },
      {
        type: 'scatter',
        data: [[lastIndex, lastPoint]],
        symbolSize: 9,
        itemStyle: {
          color: 'rgba(228, 250, 255, 0.98)',
          borderColor: 'rgba(146, 226, 255, 0.82)',
          borderWidth: 1.4,
          shadowBlur: 16,
          shadowColor: 'rgba(134, 229, 255, 0.74)',
        },
        tooltip: { show: false },
        z: 8,
      },
    ],
  }
})

const isCompact = computed(() => Number(props.widgetWidth ?? 0) > 0 && Number(props.widgetWidth ?? 0) < 760)
const isNarrow = computed(() => Number(props.widgetWidth ?? 0) > 0 && Number(props.widgetWidth ?? 0) < 620)

const cardVars = computed(() => {
  const width = Number(props.widgetWidth ?? 820)
  const height = Number(props.widgetHeight ?? 520)

  return {
    '--rev-shell-gap': `${clamp(Math.round(Math.min(width, height) * 0.02), 10, 20)}px`,
    '--rev-title-size': `${clamp(Math.round(width * 0.05), 28, 44)}px`,
    '--rev-main-size': `${clamp(Math.round(width * 0.098), 50, 84)}px`,
    '--rev-period-size': `${clamp(Math.round(width * 0.031), 16, 22)}px`,
    '--rev-kpi-label-size': `${clamp(Math.round(width * 0.018), 11, 16)}px`,
    '--rev-kpi-value-size': `${clamp(Math.round(width * 0.039), 26, 44)}px`,
    '--rev-bottom-size': `${clamp(Math.round(width * 0.036), 24, 42)}px`,
    '--rev-card-radius': `${clamp(Math.round(Math.min(width * 0.052, height * 0.12)), 20, 42)}px`,
    '--rev-chart-height': `${clamp(Math.round(height * 0.34), 154, 244)}px`,
  }
})

function diffMonths(a, b) {
  const start = a < b ? a : b
  const end = a < b ? b : a
  return (end.getFullYear() - start.getFullYear()) * 12 + (end.getMonth() - start.getMonth())
}
</script>

<style scoped>
.rev-liquid {
  position: relative;
  width: calc(100% - (var(--rev-shell-gap) * 2));
  height: calc(100% - (var(--rev-shell-gap) * 2));
  margin: var(--rev-shell-gap);
  padding: 26px 30px 20px;
  border-radius: var(--rev-card-radius);
  overflow: hidden;
  color: rgba(241, 246, 255, 0.95);
  background:
    radial-gradient(118% 88% at -8% 118%, rgba(154, 170, 198, 0.08), rgba(154, 170, 198, 0) 62%),
    radial-gradient(88% 70% at 106% 24%, rgba(132, 162, 188, 0.055), rgba(132, 162, 188, 0) 70%),
    radial-gradient(56% 54% at 44% 34%, rgba(216, 224, 238, 0.075), rgba(216, 224, 238, 0) 72%),
    linear-gradient(164deg, rgba(56, 64, 82, 0.32) 0%, rgba(34, 42, 60, 0.3) 46%, rgba(12, 17, 30, 0.5) 100%);
  border: 1px solid rgba(220, 230, 246, 0.042);
  box-shadow:
    0 28px 58px rgba(2, 9, 25, 0.42),
    0 10px 22px rgba(3, 11, 30, 0.24),
    0 0 30px rgba(133, 164, 214, 0.055),
    inset 0 1px 0 rgba(245, 249, 255, 0.12),
    inset 0 -1px 0 rgba(122, 138, 170, 0.09),
    inset 0 0 0 1px rgba(188, 203, 228, 0.018);
  backdrop-filter: blur(34px) saturate(94%);
  -webkit-backdrop-filter: blur(34px) saturate(94%);
}

.rev-liquid::before {
  content: '';
  position: absolute;
  inset: 0;
  border-radius: inherit;
  pointer-events: none;
  background:
    linear-gradient(180deg, rgba(250, 252, 255, 0.17) 0%, rgba(250, 252, 255, 0.06) 7%, rgba(250, 252, 255, 0) 18%),
    radial-gradient(72% 26% at 50% 0%, rgba(220, 230, 246, 0.16), rgba(220, 230, 246, 0) 78%),
    radial-gradient(34% 28% at 28% 40%, rgba(214, 226, 242, 0.075), rgba(214, 226, 242, 0) 86%),
    radial-gradient(30% 24% at 72% 54%, rgba(168, 197, 224, 0.06), rgba(168, 197, 224, 0) 84%);
  mix-blend-mode: screen;
  opacity: 0.62;
}

.rev-liquid::after {
  content: '';
  position: absolute;
  inset: 0;
  border-radius: inherit;
  pointer-events: none;
  background:
    radial-gradient(36% 40% at 32% 42%, rgba(208, 220, 240, 0.055), rgba(208, 220, 240, 0) 74%),
    radial-gradient(42% 44% at 75% 56%, rgba(149, 181, 206, 0.048), rgba(149, 181, 206, 0) 80%),
    radial-gradient(94% 60% at 50% 112%, rgba(4, 8, 18, 0.24), rgba(4, 8, 18, 0) 74%);
  box-shadow:
    inset 0 14px 26px rgba(183, 205, 232, 0.04),
    inset 0 -20px 28px rgba(2, 8, 20, 0.16),
    inset 0 0 52px rgba(157, 183, 220, 0.045);
}

.rev-state {
  position: absolute;
  inset: 0;
  display: grid;
  place-items: center;
  color: rgba(223, 232, 250, 0.9);
  font-size: 1rem;
  font-weight: 560;
  z-index: 2;
}

.rev-state--error {
  color: rgba(255, 196, 196, 0.96);
}

.rev-top,
.rev-main,
.rev-chart-wrap,
.rev-bottom {
  position: relative;
  z-index: 1;
}

.rev-top {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  gap: 12px;
}

.rev-title {
  margin: 0;
  font-size: var(--rev-title-size);
  line-height: 1.06;
  letter-spacing: -0.015em;
  font-weight: 575;
  color: rgba(248, 251, 255, 0.95);
}

.rev-main {
  margin-top: 15px;
  display: grid;
  grid-template-columns: minmax(240px, 1fr) minmax(280px, 47%);
  gap: 16px;
  align-items: stretch;
}

.rev-primary {
  min-width: 0;
  padding-top: 6px;
}

.rev-value {
  font-size: var(--rev-main-size);
  line-height: 0.94;
  font-weight: 590;
  letter-spacing: -0.04em;
  color: rgba(248, 251, 255, 0.97);
}

.rev-period {
  margin-top: 14px;
  font-size: var(--rev-period-size);
  line-height: 1.18;
  font-weight: 430;
  letter-spacing: 0.005em;
  color: rgba(188, 201, 224, 0.8);
}

.rev-kpis {
  display: grid;
  grid-template-columns: 1fr 1fr;
  align-self: end;
}

.rev-kpi {
  padding: 7px 0 8px 15px;
  min-height: 84px;
}

.rev-kpi:nth-child(odd) {
  border-right: 1px solid rgba(174, 192, 220, 0.04);
}

.rev-kpi:nth-child(-n + 2) {
  border-bottom: 1px solid rgba(174, 192, 220, 0.04);
}

.rev-kpi-label {
  font-size: var(--rev-kpi-label-size);
  font-weight: 430;
  letter-spacing: 0.01em;
  color: rgba(190, 203, 226, 0.7);
}

.rev-kpi-value {
  margin-top: 5px;
  font-size: var(--rev-kpi-value-size);
  line-height: 1.04;
  letter-spacing: -0.02em;
  font-weight: 540;
  color: rgba(244, 248, 255, 0.96);
}

.rev-kpi-value--up {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  color: rgba(108, 239, 188, 0.96);
}

.rev-kpi-arrow {
  font-size: 0.82em;
  transform: translateY(-1px);
}

.rev-chart-wrap {
  position: relative;
  margin-top: 1px;
  height: var(--rev-chart-height);
  border-radius: 0;
  overflow: visible;
}

.rev-chart-wrap::before {
  content: '';
  position: absolute;
  inset: 0;
  pointer-events: none;
  background:
    linear-gradient(180deg, rgba(194, 209, 232, 0.03), rgba(194, 209, 232, 0) 38%),
    radial-gradient(64% 36% at 50% 100%, rgba(107, 134, 182, 0.07), rgba(107, 134, 182, 0) 72%);
}

.rev-chart {
  width: 100%;
  height: 100%;
}

.rev-bottom {
  margin-top: 10px;
  padding-top: 14px;
  border-top: 1px solid rgba(176, 193, 222, 0.06);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
}

.rev-bottom-left {
  min-width: 0;
  display: inline-flex;
  align-items: center;
  gap: 12px;
}

.rev-bars {
  display: inline-flex;
  align-items: flex-end;
  gap: 4px;
  width: 40px;
  height: 30px;
}

.rev-bars i {
  width: 4px;
  border-radius: 999px;
  background: linear-gradient(180deg, rgba(106, 178, 242, 0.88), rgba(71, 118, 195, 0.82));
  box-shadow: 0 0 6px rgba(111, 176, 239, 0.12);
}

.rev-bars i:nth-child(1) {
  height: 15px;
}

.rev-bars i:nth-child(2) {
  height: 23px;
}

.rev-bars i:nth-child(3) {
  height: 29px;
}

.rev-bars i:nth-child(4) {
  height: 20px;
}

.rev-bottom-label {
  font-size: clamp(1.08rem, 1.32vw, 2.06rem);
  line-height: 1.08;
  letter-spacing: -0.01em;
  color: rgba(191, 202, 222, 0.88);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.rev-bottom-right {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  flex: 0 0 auto;
}

.rev-bottom-value {
  font-size: var(--rev-bottom-size);
  line-height: 1;
  font-weight: 590;
  color: rgba(248, 251, 255, 0.98);
  letter-spacing: -0.02em;
}

.rev-bottom-check {
  display: inline-grid;
  place-items: center;
  width: 29px;
  height: 29px;
  border-radius: 999px;
  color: rgba(240, 248, 255, 0.98);
  font-size: 0.9rem;
  font-weight: 700;
  background: linear-gradient(180deg, rgba(101, 149, 224, 0.82), rgba(76, 112, 186, 0.8));
  box-shadow:
    inset 0 1px 0 rgba(220, 236, 255, 0.24),
    0 5px 11px rgba(5, 14, 39, 0.24);
}

.rev-liquid.is-compact {
  padding: 20px 20px 16px;
}

.rev-liquid.is-compact .rev-main {
  margin-top: 14px;
  grid-template-columns: 1fr;
  gap: 8px;
}

.rev-liquid.is-compact .rev-kpi {
  min-height: 74px;
  padding-left: 12px;
}

.rev-liquid.is-compact .rev-chart-wrap {
  margin-top: 4px;
}

.rev-liquid.is-narrow .rev-title {
  letter-spacing: -0.01em;
}

.rev-liquid.is-narrow .rev-kpi {
  min-height: 62px;
  padding: 8px 0 8px 10px;
}

.rev-liquid.is-narrow .rev-bottom {
  padding-top: 10px;
  margin-top: 6px;
}

.rev-liquid.is-narrow .rev-bars {
  width: 32px;
  height: 24px;
  gap: 3px;
}

.rev-liquid.is-narrow .rev-bars i {
  width: 3px;
}

.rev-liquid.is-narrow .rev-bottom-check {
  width: 24px;
  height: 24px;
  font-size: 0.76rem;
}
</style>
