<template>
  <WidgetCard
    title="Stock dormant"
    subtitle="Age des paires en stock"
    :accent="accent"
    :loading="loading"
    :error="error"
  >
    <div class="dp-header">
      <div class="dp-tabs">
        <span class="dp-pill dp-pill--static">Age des paires</span>
      </div>
      <div class="period-chip">
        <span class="period-label">Periode</span>
        <span class="period-value">{{ fromLabel }} -> {{ toLabel }}</span>
      </div>
    </div>
    <div class="dp-body">
      <div class="dp-note">
        Plus c'est rouge, plus c'est dormant.
      </div>

      <VChart class="dp-chart" :option="option" autoresize />
      <div class="dp-scale">
        <div class="dp-scale-title">Legende</div>
        <div class="dp-scale-bar"></div>
        <div class="dp-scale-labels">
          <span>recent</span>
          <span>moyen</span>
          <span>lent</span>
          <span>dormant</span>
        </div>
      </div>
      <div class="dp-legend">
        <div v-if="!rows.length" class="dp-empty">Aucune donnee sur la periode.</div>
        <div v-else class="dp-list">
         
        </div>
      </div>
    </div>
  </WidgetCard>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import StatsServices from '@/services/StatsServices'
import { normalizeBreakdown } from '@/services/statsAdapters'
import { formatDateFR, formatNumber } from '@/utils/formatters'
import WidgetCard from './_parts/WidgetCard.vue'

const props = defineProps({
  from: String,
  to: String,
  categories: { type: Array, default: () => [] },
})
const accent = '#EF4444'

const loading = ref(false)
const error = ref('')
const buckets = ref([]) // label: "0-30", "31-90", "91-180", "180+"
let req = 0

async function load() {
  const id = ++req
  loading.value = true
  error.value = ''
  try {
    const { data } = await StatsServices.breakdown(
      'deathPileAge',
      props.from,
      props.to,
      props.categories,
    )
    if (id !== req) return
    buckets.value = normalizeBreakdown(data)
  } catch (e) {
    if (id !== req) return
    error.value = e?.response?.data?.message ?? e?.message ?? 'Impossible de charger'
  } finally {
    if (id === req) loading.value = false
  }
}

onMounted(load)
watch(() => [props.from, props.to, props.categories], load)

function formatLabel(raw) {
  const s = String(raw ?? '').trim()
  if (!s) return ''
  if (s.includes('+')) return s.replace('+', '+ jours')
  if (s.includes('-')) {
    const parts = s.split('-').map((x) => x.trim())
    if (parts.length === 2) return `${parts[0]}-${parts[1]} jours`
  }
  return `${s} jours`
}

function barColor(label, index, totalCount) {
  const s = String(label).toLowerCase()
  if (s.includes('180')) return '#DC2626'
  if (s.includes('91') || s.includes('90')) return '#F97316'
  if (s.includes('31')) return '#F59E0B'
  if (s.includes('0-30')) return '#22C55E'

  const count = Math.max(1, Number(totalCount || 1))
  const t = count <= 1 ? 0 : index / (count - 1)
  const hue = 120 - Math.round(120 * t) // 120=green -> 0=red
  return `hsl(${hue} 85% 55%)`
}

const option = computed(() => {
  const labels = buckets.value.map((b) => formatLabel(b.label))
  const values = buckets.value.map((b) => Number(b.value ?? 0))
  const colors = labels.map((l, i) => barColor(l, i, labels.length))

  return {
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'axis',
      valueFormatter: (v) => `${formatNumber(v)} paires`,
    },
    grid: { left: 90, right: 20, top: 6, bottom: 6, containLabel: true },
    xAxis: {
      type: 'value',
      axisLabel: { color: '#9CA3AF', formatter: '{value}', fontSize: 10 },
      splitLine: { lineStyle: { color: 'rgba(148,163,184,0.15)' } },
    },
    yAxis: {
      type: 'category',
      data: labels,
      axisLabel: { color: '#E5E7EB', fontSize: 11, margin: 16 },
      axisTick: { show: false },
      axisLine: { lineStyle: { color: '#334155' } },
    },
    series: [
      {
        type: 'bar',
        data: values.map((v, i) => ({
          value: v,
          itemStyle: {
            borderRadius: [8, 8, 8, 8],
            color: colors[i],
          },
        })),
        barWidth: 14,
        label: {
          show: true,
          position: 'right',
          color: '#E2E8F0',
          fontSize: 11,
          formatter: (p) => `${formatNumber(p.value)} paires`,
        },
      },
    ],
  }
})

const items = computed(() => buckets.value)
const total = computed(() => items.value.reduce((acc, i) => acc + Number(i.value ?? 0), 0))
const rows = computed(() =>
  items.value.map((i, index) => {
    const value = Number(i.value ?? 0)
    const pct = total.value > 0 ? Math.round((value / total.value) * 100) : 0
    const label = formatLabel(i.label)
    return {
      label,
      value,
      pct,
      valueText: `${formatNumber(value)} paires`,
      color: barColor(label, index, items.value.length),
    }
  }),
)
const fromLabel = computed(() =>
  formatDateFR(props.from, { day: '2-digit', month: 'short', year: 'numeric' }),
)
const toLabel = computed(() =>
  formatDateFR(props.to, { day: '2-digit', month: 'short', year: 'numeric' }),
)
</script>

<style scoped>
.dp-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 6px;
}
.dp-tabs {
  display: inline-flex;
  gap: 6px;
}
.dp-pill {
  font-size: 11px;
  padding: 4px 10px;
  border-radius: 999px;
  border: 1px solid transparent;
}
.dp-pill--static {
  border: 1px solid rgba(255, 255, 255, 0.12);
  background: rgba(148, 163, 184, 0.12);
  color: rgba(226, 232, 240, 0.85);
}
.dp-body {
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.dp-note {
  font-size: 11px;
  color: rgba(226, 232, 240, 0.6);
  letter-spacing: 0.02em;
  align-self: flex-start;
}
.dp-chart {
  width: 100%;
  height: 200px;
  min-height: 200px;
}
.dp-scale {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-top: 2px;
}
.dp-scale-title {
  font-size: 9px;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: rgba(226, 232, 240, 0.55);
}
.dp-scale-bar {
  height: 8px;
  border-radius: 999px;
  background: linear-gradient(
    90deg,
    #22C55E 0%,
    #84CC16 20%,
    #FDE047 45%,
    #F59E0B 62%,
    #F97316 78%,
    #DC2626 100%
  );
  border: 1px solid rgba(255, 255, 255, 0.12);
  box-shadow:
    inset 0 0 0 1px rgba(255, 255, 255, 0.06),
    0 6px 16px rgba(0, 0, 0, 0.2);
}
.dp-scale-labels {
  display: flex;
  justify-content: space-between;
  font-size: 10px;
  color: rgba(226, 232, 240, 0.7);
  text-transform: uppercase;
  letter-spacing: 0.04em;
}
.dp-legend {
  margin-top: 4px;
}
.dp-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.dp-row {
  display: grid;
  grid-template-columns: 12px 1fr auto auto;
  gap: 8px;
  align-items: center;
  padding: 4px 8px;
  border-radius: 12px;
  background: rgba(15, 23, 42, 0.45);
  border: 1px solid rgba(255, 255, 255, 0.06);
}
.dp-dot {
  width: 8px;
  height: 8px;
  border-radius: 999px;
  box-shadow: 0 0 8px rgba(0, 0, 0, 0.25);
}
.dp-name {
  font-size: 12px;
  color: rgba(226, 232, 240, 0.92);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.dp-pct {
  font-size: 11px;
  color: rgba(226, 232, 240, 0.6);
}
.dp-val {
  font-size: 12px;
  color: rgba(226, 232, 240, 0.85);
}
.dp-empty {
  font-size: 12px;
  color: rgba(226, 232, 240, 0.5);
}
.period-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 10px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: rgba(226, 232, 240, 0.6);
  white-space: nowrap;
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(15, 23, 42, 0.65);
  border: 1px solid rgba(255, 255, 255, 0.12);
  box-shadow:
    inset 0 0 0 1px rgba(255, 255, 255, 0.03),
    0 6px 16px rgba(0, 0, 0, 0.25);
  margin-left: auto;
}
.period-label {
  opacity: 0.7;
}
.period-value {
  font-size: 11px;
  letter-spacing: 0.02em;
  text-transform: none;
  color: rgba(226, 232, 240, 0.9);
}
</style>
