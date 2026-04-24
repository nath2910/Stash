<template>
  <section class="template-page template-page--monthly-focus">
    <section class="template-month-strip" aria-label="Mois">
      <div class="template-year-box">
        <label class="template-year-box__label" for="template-year-select-monthly">Annee</label>
        <select
          id="template-year-select-monthly"
          class="template-year-box__select"
          :value="props.selectedYear"
          @change="emit('year-change', $event)"
        >
          <option v-for="year in props.yearOptions" :key="`template-monthly-year-${year}`" :value="year">
            {{ year }}
          </option>
        </select>
      </div>
      <div class="template-month-strip__main">
        <div class="template-month-strip__label">Focus mois</div>
        <div class="template-month-strip__grid">
          <button
            v-for="month in props.monthChips"
            :key="`monthly-month-${month.index}`"
            type="button"
            class="template-month-chip"
            :class="{
              'is-active': month.active,
              'is-available': month.available,
              'is-unavailable': !month.available,
            }"
            :disabled="!month.available"
            title="Clic: 1 mois | Ctrl/Cmd+clic: plusieurs mois"
            @click="month.available && emit('month-select', month.index, $event)"
          >
            {{ month.label }}
          </button>
        </div>
      </div>
    </section>

    <section class="template-dashboard-grid">
      <section class="template-left-stack">
        <article class="template-panel template-panel--pie">
          <div class="template-graph-head">
            <h3 class="template-graph-title">Poids marques du mois</h3>
            <p class="template-graph-subtitle">{{ monthlyRangeLabel }}</p>
          </div>
          <div class="template-pie-layout">
            <div class="template-pie-chart" :style="props.pieChartStyle"></div>
            <ul class="template-pie-legend">
              <li v-for="slice in props.pieSlices" :key="`monthly-slice-${slice.label}`">
                <span class="template-pie-dot" :style="{ backgroundColor: slice.color }"></span>
                <span class="template-pie-label">{{ slice.label }}</span>
                <strong class="template-pie-value">{{ slice.percentText }}</strong>
              </li>
            </ul>
          </div>
        </article>
      </section>

      <section class="template-right-stack">
        <section class="template-kpi-band">
          <div class="template-kpi-row">
            <article v-for="kpi in monthlyKpiCards" :key="kpi.key" class="template-kpi-card">
              <p class="template-kpi-card__label">{{ kpi.label }}</p>
              <p class="template-kpi-card__value" :class="kpi.tone">{{ kpi.value }}</p>
            </article>
          </div>
        </section>
      </section>

      <section class="template-bottom-info">
        <article class="template-panel template-panel--table template-panel--brands">
          <h3 class="template-panel__title">Top jours du mois</h3>
          <div class="template-table-wrap">
            <table class="template-table">
              <thead>
                <tr>
                  <th>Jour</th>
                  <th class="is-number">CA</th>
                </tr>
              </thead>
              <tbody>
                <template v-if="topDayRows.length">
                  <tr v-for="day in topDayRows" :key="`day-${day.date}`">
                    <td>{{ day.label }}</td>
                    <td class="is-number">{{ formatEUR(day.value, { compact: true }) }}</td>
                  </tr>
                </template>
                <tr v-else class="template-table__empty-row">
                  <td colspan="2">Pas de jour fort pour la periode.</td>
                </tr>
              </tbody>
            </table>
          </div>
        </article>

        <article class="template-panel template-panel--facts">
          <h3 class="template-panel__title">Signaux mensuels</h3>
          <div class="template-facts-grid">
            <div v-for="fact in monthlyFocusFacts" :key="fact.key" class="template-fact">
              <p class="template-fact__label">{{ fact.label }}</p>
              <p class="template-fact__value">{{ fact.value }}</p>
            </div>
          </div>
        </article>

        <article class="template-panel template-panel--table template-panel--sales">
          <h3 class="template-panel__title">Top ventes du mois</h3>
          <div class="template-table-wrap">
            <table class="template-table">
              <thead>
                <tr>
                  <th>Produit</th>
                  <th class="is-number">Benefice</th>
                </tr>
              </thead>
              <tbody>
                <template v-if="props.topSalesRows.length">
                  <tr v-for="item in props.topSalesRows" :key="`monthly-sale-${item.nomItem}`">
                    <td class="truncate">{{ item.nomItem }}</td>
                    <td class="is-number">{{ formatEUR(item.benefice, { compact: true }) }}</td>
                  </tr>
                </template>
                <tr v-else class="template-table__empty-row">
                  <td colspan="2">Aucune vente sur la periode.</td>
                </tr>
              </tbody>
            </table>
          </div>
        </article>
      </section>

      <section class="template-bottom-charts">
        <article class="template-panel template-panel--trend">
          <div class="template-graph-head">
            <h3 class="template-graph-title">CA journalier</h3>
            <p class="template-graph-subtitle">{{ props.cadenceText }}</p>
          </div>
          <div class="template-chart-shell" :class="{ 'is-empty': props.miniDataState !== 'ready' }">
            <svg
              v-if="props.miniDataState === 'ready'"
              class="template-chart-svg"
              :viewBox="`0 0 ${TEMPLATE_CHART_W} ${TEMPLATE_CHART_H}`"
              preserveAspectRatio="xMidYMid meet"
              role="img"
              aria-label="Courbe CA mensuel"
              @mousemove="emit('chart-hover', $event, 'mini')"
              @mouseleave="emit('chart-clear', 'mini')"
            >
              <defs>
                <linearGradient id="templateChartAreaFillMonthly" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="0%" stop-color="rgba(37, 99, 235, 0.36)" />
                  <stop offset="100%" stop-color="rgba(37, 99, 235, 0.04)" />
                </linearGradient>
              </defs>
              <line
                v-for="lineY in props.chartGridLines"
                :key="`monthly-grid-ca-${lineY}`"
                class="template-chart-grid"
                :x1="TEMPLATE_CHART_PAD_X"
                :y1="lineY"
                :x2="TEMPLATE_CHART_W - TEMPLATE_CHART_PAD_X"
                :y2="lineY"
              />
              <path class="template-chart-area-monthly" :d="props.miniChartAreaPath" />
              <path class="template-chart-line" :d="props.miniChartLinePath" />
              <circle
                v-if="props.miniLastPointCoord"
                class="template-chart-dot"
                :cx="props.miniLastPointCoord.x"
                :cy="props.miniLastPointCoord.y"
                r="3.8"
              />
              <circle
                v-if="props.miniHover.visible"
                class="template-chart-hover-dot"
                :cx="props.miniHover.x"
                :cy="props.miniHover.y"
                r="4.2"
              />
            </svg>
            <div v-else class="template-chart-empty template-chart-empty--compact">
              <p class="template-chart-empty__title">{{ props.emptyTitle }}</p>
            </div>
            <div
              v-if="props.miniHover.visible"
              class="template-chart-tooltip"
              :style="{ left: `${props.miniHover.xPct}%`, top: `${props.miniHover.yPct}%` }"
            >
              <strong>{{ props.miniHover.valueText }}</strong>
              <span>{{ props.miniHover.dateText }}</span>
            </div>
          </div>
          <div class="template-chart-meta">
            <div>Debut: {{ props.localFrom }}</div>
            <div>Fin: {{ props.localTo }}</div>
          </div>
        </article>

        <article class="template-kpi-trend template-kpi-trend--square">
          <div class="template-kpi-trend__head">
            <h3 class="template-kpi-trend__title">Profit journalier</h3>
            <p class="template-kpi-trend__subtitle">Net quotidien</p>
          </div>
          <div class="template-kpi-trend__chart" :class="{ 'is-empty': props.mainDataState !== 'ready' }">
            <svg
              v-if="props.mainDataState === 'ready'"
              class="template-kpi-trend__svg"
              :viewBox="`0 0 ${TEMPLATE_CHART_W} ${TEMPLATE_CHART_H}`"
              preserveAspectRatio="xMidYMid meet"
              role="img"
              aria-label="Courbe profit mensuel"
              @mousemove="emit('chart-hover', $event, 'main')"
              @mouseleave="emit('chart-clear', 'main')"
            >
              <line
                v-for="lineY in props.chartGridLines"
                :key="`monthly-grid-profit-${lineY}`"
                class="template-kpi-trend__grid"
                :x1="TEMPLATE_CHART_PAD_X"
                :y1="lineY"
                :x2="TEMPLATE_CHART_W - TEMPLATE_CHART_PAD_X"
                :y2="lineY"
              />
              <path class="template-kpi-trend__area" :d="props.mainChartAreaPath" />
              <path class="template-kpi-trend__line" :d="props.mainChartLinePath" />
              <circle
                v-if="props.mainLastPointCoord"
                class="template-kpi-trend__dot"
                :cx="props.mainLastPointCoord.x"
                :cy="props.mainLastPointCoord.y"
                r="5"
              />
              <circle
                v-if="props.mainHover.visible"
                class="template-chart-hover-dot"
                :cx="props.mainHover.x"
                :cy="props.mainHover.y"
                r="5"
              />
            </svg>
            <p v-else class="template-kpi-trend__empty">{{ props.emptyTitle }}</p>
            <div
              v-if="props.mainHover.visible"
              class="template-chart-tooltip template-chart-tooltip--mini"
              :style="{ left: `${props.mainHover.xPct}%`, top: `${props.mainHover.yPct}%` }"
            >
              <strong>{{ props.mainHover.valueText }}</strong>
              <span>{{ props.mainHover.dateText }}</span>
            </div>
          </div>
        </article>

        <article class="template-panel template-panel--success template-panel--success-right">
          <h3 class="template-panel__title">Objectif mensuel</h3>
          <div class="template-success-top">
            <div class="template-success-note">
              <p>Progression vers l'objectif dynamique</p>
              <strong>{{ formatEUR(monthlyGoalValue, { compact: true }) }}</strong>
            </div>
          </div>
          <div class="template-gauge-wrap">
            <svg class="template-gauge" viewBox="0 0 300 180" role="img" aria-label="Progression mensuelle">
              <path
                class="template-gauge-track"
                d="M 30 150 A 120 120 0 0 1 270 150"
                pathLength="100"
              />
              <path
                class="template-gauge-fill"
                d="M 30 150 A 120 120 0 0 1 270 150"
                pathLength="100"
                :stroke-dasharray="monthlyGoalDasharray"
              />
              <text x="150" y="132" text-anchor="middle" class="template-gauge-value">
                {{ monthlyGoalRateText }}
              </text>
            </svg>
            <div class="template-gauge-labels">
              <span>{{ formatEUR(props.totalNumber, { compact: true }) }}</span>
              <span>Cible</span>
            </div>
          </div>
        </article>
      </section>
    </section>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { formatEUR, formatNumber, signFmt } from '@/utils/formatters'
import { TEMPLATE_CHART_H, TEMPLATE_CHART_PAD_X, TEMPLATE_CHART_W } from '../../templateModeEngine'
import type { TemplateChartTarget, TemplateSharedViewProps } from '../templateViewTypes'

const props = defineProps<TemplateSharedViewProps>()

const emit = defineEmits<{
  (e: 'year-change', event: Event): void
  (e: 'month-select', monthIndex: number, event: MouseEvent): void
  (e: 'chart-hover', event: MouseEvent, target: TemplateChartTarget): void
  (e: 'chart-clear', target: TemplateChartTarget): void
}>()

function parseYmdLocal(value: string) {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(String(value ?? ''))) return new Date(Number.NaN)
  const [y, m, d] = value.split('-').map(Number)
  return new Date(y, (m ?? 1) - 1, d ?? 1)
}

function formatPercent(value: number | null, digits = 1) {
  if (value == null || !Number.isFinite(value)) return 'n/d'
  return `${value.toFixed(digits)}%`
}

const fromDate = computed(() => parseYmdLocal(props.localFrom))
const toDate = computed(() => parseYmdLocal(props.localTo))

const periodDays = computed(() => {
  const from = fromDate.value
  const to = toDate.value
  if (Number.isNaN(from.getTime()) || Number.isNaN(to.getTime())) return 1
  const diff = Math.floor((to.getTime() - from.getTime()) / 86400000) + 1
  return Math.max(diff, 1)
})

const monthDays = computed(() => {
  const to = toDate.value
  if (Number.isNaN(to.getTime())) return 30
  return new Date(to.getFullYear(), to.getMonth() + 1, 0).getDate()
})

const monthlyRangeLabel = computed(() => {
  const from = fromDate.value
  const to = toDate.value
  if (Number.isNaN(from.getTime()) || Number.isNaN(to.getTime())) return 'Periode custom'
  const fromText = from.toLocaleDateString('fr-FR', { day: '2-digit', month: 'short' })
  const toText = to.toLocaleDateString('fr-FR', { day: '2-digit', month: 'short' })
  return `${fromText} -> ${toText}`
})

const dailyRevenue = computed(() => props.totalNumber / periodDays.value)
const dailyProfit = computed(() => Number(props.summary.profit || 0) / periodDays.value)
const salesPerDay = computed(() => Number(props.summary.itemsVendues || 0) / periodDays.value)

const avgTicket = computed(() => {
  const sales = Number(props.summary.itemsVendues || 0)
  if (!sales) return 0
  return props.totalNumber / sales
})

const monthlyProjection = computed(() => {
  if (!periodDays.value) return 0
  const from = fromDate.value
  const to = toDate.value
  const sameMonth =
    !Number.isNaN(from.getTime()) &&
    !Number.isNaN(to.getTime()) &&
    from.getFullYear() === to.getFullYear() &&
    from.getMonth() === to.getMonth()
  if (sameMonth) {
    const elapsed = Math.max(to.getDate(), 1)
    return (props.totalNumber / elapsed) * monthDays.value
  }
  return dailyRevenue.value * monthDays.value
})

const monthlyGoalValue = computed(() => {
  const base = Math.max(monthlyProjection.value, props.totalNumber, 1)
  return base * 1.08
})

const monthlyGoalRate = computed(() => {
  if (monthlyGoalValue.value <= 0) return 0
  return Math.max(0, Math.min(100, (props.totalNumber / monthlyGoalValue.value) * 100))
})

const monthlyGoalDasharray = computed(() => `${monthlyGoalRate.value.toFixed(2)} 100`)
const monthlyGoalRateText = computed(() => `${monthlyGoalRate.value.toFixed(1)}%`)

const sellThroughRate = computed(() => {
  const sales = Number(props.summary.itemsVendues || 0)
  const stock = Number(props.summary.itemsEnStock || 0)
  const volume = sales + stock
  if (!volume) return null
  return (sales / volume) * 100
})

const momentumScore = computed(() => {
  const rows = props.series ?? []
  if (rows.length < 2) return 50
  const sample = Math.min(7, rows.length)
  const first = rows.slice(0, sample)
  const last = rows.slice(-sample)
  const firstAvg = first.reduce((sum, row) => sum + Number(row.value || 0), 0) / sample
  const lastAvg = last.reduce((sum, row) => sum + Number(row.value || 0), 0) / sample
  if (!Number.isFinite(firstAvg) || !Number.isFinite(lastAvg)) return 50
  const deltaRatio = (lastAvg - firstAvg) / Math.max(Math.abs(firstAvg), 1)
  return Math.max(0, Math.min(100, 50 + deltaRatio * 40))
})

const regularityScore = computed(() => {
  const rows = props.series ?? []
  if (!rows.length) return 50
  const values = rows.map((row) => Number(row.value || 0))
  const avg = values.reduce((sum, value) => sum + value, 0) / values.length
  if (!Number.isFinite(avg) || avg === 0) return 50
  const variance = values.reduce((sum, value) => sum + (value - avg) ** 2, 0) / values.length
  const std = Math.sqrt(Math.max(variance, 0))
  const ratio = std / Math.max(Math.abs(avg), 1)
  return Math.max(0, Math.min(100, 100 - ratio * 35))
})

const profitPerSale = computed(() => {
  const sales = Number(props.summary.itemsVendues || 0)
  if (!sales) return 0
  return Number(props.summary.profit || 0) / sales
})

const deltaText = computed(() => {
  if (props.deltaPct == null || !Number.isFinite(props.deltaPct)) return 'n/d'
  return signFmt(props.deltaPct)
})

const deltaTone = computed(() => {
  if (props.deltaPct == null || !Number.isFinite(props.deltaPct)) return ''
  if (props.deltaPct > 0) return 'is-positive'
  if (props.deltaPct < 0) return 'is-negative'
  return ''
})

const monthlyKpiCards = computed(() => [
  { key: 'ca_month', label: 'CA du mois', value: formatEUR(props.totalNumber), tone: '' },
  {
    key: 'projection',
    label: 'Projection fin mois',
    value: formatEUR(monthlyProjection.value, { compact: true }),
    tone: '',
  },
  {
    key: 'daily_profit',
    label: 'Profit / jour',
    value: formatEUR(dailyProfit.value, { compact: true }),
    tone: dailyProfit.value > 0 ? 'is-positive' : dailyProfit.value < 0 ? 'is-negative' : '',
  },
  {
    key: 'avg_ticket',
    label: 'Ticket moyen',
    value: formatEUR(avgTicket.value, { compact: true }),
    tone: '',
  },
  {
    key: 'sales_day',
    label: 'Ventes / jour',
    value: formatNumber(salesPerDay.value),
    tone: '',
  },
  { key: 'delta', label: 'Variation', value: deltaText.value, tone: deltaTone.value },
])

const monthlyFocusFacts = computed(() => [
  { key: 'daily_rev', label: 'CA / jour', value: formatEUR(dailyRevenue.value, { compact: true }) },
  { key: 'sell_through', label: 'Sell-through', value: formatPercent(sellThroughRate.value) },
  { key: 'momentum', label: 'Indice momentum', value: `${momentumScore.value.toFixed(0)}/100` },
  { key: 'regularity', label: 'Score regularite', value: `${regularityScore.value.toFixed(0)}/100` },
  { key: 'profit_sale', label: 'Profit / vente', value: formatEUR(profitPerSale.value, { compact: true }) },
  { key: 'stock_left', label: 'Stock restant', value: formatNumber(Number(props.summary.itemsEnStock || 0)) },
])

const topDayRows = computed(() => {
  const rows = (props.series ?? []).map((point) => ({
    date: point.date,
    value: Number(point.value || 0),
    label: formatDay(point.date),
  }))
  return rows.sort((a, b) => b.value - a.value).slice(0, 5)
})

function formatDay(value: string) {
  const date = parseYmdLocal(value)
  if (Number.isNaN(date.getTime())) return value
  return date.toLocaleDateString('fr-FR', { day: '2-digit', month: 'short' })
}
</script>
