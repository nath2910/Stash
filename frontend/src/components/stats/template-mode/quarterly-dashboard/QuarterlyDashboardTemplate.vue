<template>
  <section class="quarterly-dashboard" aria-label="Dashboard trimestriel" @wheel="onWheel">
    <div class="quarterly-dashboard__inner">
      <header class="quarterly-header">
        <div class="quarterly-header__copy">
          <p class="quarterly-header__kicker">Template</p>
          <h1>Dashboard trimestriel</h1>
          <p>
            Lecture {{ selectedQuarterLongLabel }} des ventes, du profit, des achats et du stock
            au {{ formatDate(periodRange.to) }}.
          </p>
        </div>

        <div class="quarterly-quarter">
          <div class="quarterly-quarter__head">
            <span>Trimestre selectionne</span>
            <small>{{ periodShortLabel }}</small>
          </div>

          <div class="quarterly-quarter__control">
            <button
              type="button"
              aria-label="Trimestre precedent"
              :disabled="!canGoPreviousQuarter"
              @click="changeQuarter(-1)"
            >
              <ChevronLeft aria-hidden="true" />
            </button>

            <div class="quarterly-quarter__summary">
              <strong>{{ selectedQuarterLabel }}</strong>
              <input
                :value="yearDraft"
                inputmode="numeric"
                pattern="[0-9]*"
                autocomplete="off"
                aria-label="Selectionner une annee"
                @input="onYearInput"
                @blur="commitYear()"
                @keydown.enter.prevent="commitYear()"
              />
            </div>

            <button
              type="button"
              aria-label="Trimestre suivant"
              :disabled="!canGoNextQuarter"
              @click="changeQuarter(1)"
            >
              <ChevronRight aria-hidden="true" />
            </button>
          </div>

          <div class="quarterly-quarter__tabs" role="tablist" aria-label="Trimestres">
            <button
              v-for="quarter in quarterOptions"
              :key="quarter.value"
              type="button"
              class="quarterly-quarter__tab"
              :class="{ 'is-active': selectedQuarter === quarter.value }"
              :aria-selected="selectedQuarter === quarter.value"
              role="tab"
              @click="setQuarter(quarter.value)"
            >
              {{ quarter.label }}
            </button>
          </div>
        </div>
      </header>

      <div v-if="loading && !hasLoadedOnce" class="quarterly-state quarterly-state--loading" role="status">
        <div class="quarterly-state__pulse"></div>
        <h2>Chargement du dashboard</h2>
        <p>Preparation des indicateurs trimestriels.</p>
      </div>

      <div v-else-if="error" class="quarterly-state quarterly-state--error" role="alert">
        <h2>Impossible de charger le template</h2>
        <p>{{ error }}</p>
        <button type="button" @click="loadDashboard">Reessayer</button>
      </div>

      <template v-else>
        <div v-if="!hasQuarterData" class="quarterly-state quarterly-state--empty" role="status">
          <h2>Aucune donnee disponible pour {{ selectedQuarterLabel }}.</h2>
          <p>
            Les statistiques apparaitront des qu'une vente, un achat ou un article en stock sera
            present sur cette periode.
          </p>
        </div>

        <template v-else>
          <nav class="quarterly-page-nav" aria-label="Navigation du dashboard trimestriel">
            <button
              type="button"
              class="quarterly-page-nav__arrow"
              :disabled="activePage === 0"
              aria-label="Page precedente"
              @click="previousPage"
            >
              <ChevronLeft aria-hidden="true" />
            </button>

            <div class="quarterly-page-nav__center">
              <span class="quarterly-page-nav__count">{{ activePage + 1 }} / {{ pages.length }}</span>
              <strong>{{ currentPage.label }}</strong>
              <div class="quarterly-page-nav__dots" role="tablist" aria-label="Pages">
                <button
                  v-for="(page, index) in pages"
                  :key="page.key"
                  type="button"
                  class="quarterly-page-nav__dot"
                  :class="{ 'is-active': activePage === index }"
                  :aria-label="`Afficher ${page.label}`"
                  :aria-selected="activePage === index"
                  role="tab"
                  @click="goToPage(index)"
                ></button>
              </div>
            </div>

            <button
              type="button"
              class="quarterly-page-nav__arrow"
              :disabled="activePage === pages.length - 1"
              aria-label="Page suivante"
              @click="nextPage"
            >
              <ChevronRight aria-hidden="true" />
            </button>
          </nav>

          <section
            class="quarterly-stage"
            :class="{ 'is-dragging': pointerDrag.active }"
            aria-live="polite"
            @pointerdown="onPointerDown"
            @pointerup="onPointerUp"
            @pointercancel="resetPointerDrag"
            @lostpointercapture="resetPointerDrag"
          >
            <div class="quarterly-pages" :style="pageTrackStyle">
              <article
                v-if="activePage === 0"
                class="quarterly-page quarterly-page--flow"
                aria-label="Flux du trimestre"
              >
                <div class="quarterly-page__heading">
                  <div>
                    <p>Flux trimestriel</p>
                    <h2>CA, profit et mix du trimestre</h2>
                  </div>
                  <span>{{ quarterMonthListLabel }}</span>
                </div>

                <div class="quarterly-flow-layout">
                  <section class="quarterly-panel quarterly-panel--performance">
                    <div class="quarterly-panel__head">
                      <div>
                        <p>Tendance</p>
                        <h2>Performance sur 3 mois</h2>
                      </div>
                      <span>{{ formatMoney(quarterTotals.revenue) }} encaisses</span>
                    </div>
                    <VChart class="quarterly-chart" :option="quarterPerformanceOption" autoresize />
                  </section>

                  <section class="quarterly-panel quarterly-panel--category-profit">
                    <div class="quarterly-panel__head">
                      <div>
                        <p>Mix</p>
                        <h2>Categories profitables</h2>
                      </div>
                      <span>Top {{ categoryProfit.length }}</span>
                    </div>
                    <div v-if="categoryProfit.length" class="quarterly-chart-wrap">
                      <VChart class="quarterly-chart" :option="categoryProfitOption" autoresize />
                    </div>
                    <div v-else class="quarterly-mini-empty">
                      Aucune categorie rentable sur ce trimestre.
                    </div>
                  </section>
                </div>
              </article>

              <article
                v-else-if="activePage === 1"
                class="quarterly-page quarterly-page--main"
                aria-label="Pilotage trimestriel"
              >
                <div class="quarterly-main-layout">
                  <div class="quarterly-main-primary">
                    <section class="quarterly-kpi-grid" aria-label="KPI trimestriels">
                      <QuarterlyKpiCard
                        v-for="card in kpiCards"
                        :key="card.label"
                        :label="card.label"
                        :value="card.value"
                        :detail="card.detail"
                        :tone="card.tone"
                        :icon="card.icon"
                      />
                    </section>

                    <section
                      class="quarterly-panel quarterly-panel--quick-strip"
                      aria-label="Indicateurs complementaires"
                    >
                      <div class="quarterly-quick-grid">
                        <article
                          v-for="item in quickStripItems"
                          :key="item.label"
                          class="quarterly-quick-card"
                        >
                          <span>{{ item.label }}</span>
                          <strong>{{ item.value }}</strong>
                        </article>
                      </div>
                    </section>
                  </div>

                  <section
                    class="quarterly-panel quarterly-panel--brand-summary"
                    aria-label="Marques vendues"
                  >
                    <div class="quarterly-panel__head">
                      <div>
                        <p>Mix trimestriel</p>
                        <h2>Marques vendues</h2>
                      </div>
                      <span>{{ formatNumber(brands.length) }} marques</span>
                    </div>

                    <div
                      v-if="visibleBrandPreview.length"
                      class="quarterly-brand-list quarterly-brand-list--summary"
                    >
                      <div
                        v-for="brand in visibleBrandPreview"
                        :key="brand.label"
                        class="quarterly-brand-row"
                      >
                        <span>{{ brand.label }}</span>
                        <strong>{{ formatNumber(brand.nb) }}</strong>
                      </div>
                    </div>

                    <div v-else class="quarterly-mini-empty">
                      Aucun mix disponible sur ce trimestre.
                    </div>
                  </section>
                </div>
              </article>

              <article
                v-else-if="activePage === 2"
                class="quarterly-page quarterly-page--details"
                aria-label="Details du trimestre"
              >
                <div class="quarterly-page__heading">
                  <div>
                    <p>Analyse operationnelle</p>
                    <h2>Top ventes et contexte annuel</h2>
                  </div>
                  <span>{{ selectedYear }}</span>
                </div>

                <section class="quarterly-table-grid" aria-label="Analyses metier">
                  <article class="quarterly-panel quarterly-table-card">
                    <div class="quarterly-panel__head">
                      <div>
                        <p>Meilleures ventes</p>
                        <h2>Benefices du trimestre</h2>
                      </div>
                      <span>{{ topSales.length }} lignes</span>
                    </div>

                    <div v-if="topSales.length" class="quarterly-table-scroll">
                      <table class="quarterly-table">
                        <thead>
                          <tr>
                            <th>#</th>
                            <th>Produit</th>
                            <th>Benefice</th>
                          </tr>
                        </thead>
                        <tbody>
                          <tr v-for="(sale, index) in topSales" :key="`${sale.nomItem}-${index}`">
                            <td>{{ index + 1 }}</td>
                            <td>
                              <strong :title="sale.nomItem">{{ sale.nomItem }}</strong>
                              <span>Top vente trimestrielle</span>
                            </td>
                            <td :class="profitClass(sale.benefice)">
                              {{ formatMoney(sale.benefice) }}
                            </td>
                          </tr>
                        </tbody>
                      </table>
                    </div>

                    <div v-else class="quarterly-mini-empty">
                      Aucune vente classee sur ce trimestre.
                    </div>
                  </article>

                  <article class="quarterly-panel quarterly-table-card">
                    <div class="quarterly-panel__head">
                      <div>
                        <p>Contexte</p>
                        <h2>Position du trimestre dans l'annee</h2>
                      </div>
                      <span>{{ selectedYear }}</span>
                    </div>
                    <VChart class="quarterly-chart quarterly-chart--context" :option="yearContextOption" autoresize />
                  </article>
                </section>
              </article>
            </div>
          </section>
        </template>
      </template>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import {
  BadgeEuro,
  Boxes,
  ChevronLeft,
  ChevronRight,
  CirclePercent,
  ShoppingBag,
  TrendingUp,
  Wallet,
} from 'lucide-vue-next'
import StatsServices from '@/services/StatsServices'
import {
  normalizeBrands,
  normalizeRank,
  normalizeSummary,
  normalizeTimeseries,
  normalizeTopSales,
  prevPeriod,
} from '@/services/statsAdapters'
import QuarterlyKpiCard from './QuarterlyKpiCard.vue'

type QuarterlyTemplateState = {
  year?: number
  quarter?: number
}

type StatsSummary = {
  ca: number
  profit: number
  profitMargin: number
  itemsVendues: number
  itemsEnStock: number
  valeurStock: number
}

type TimePoint = {
  date: string
  ca: number
  profit: number
}

type BrandRow = {
  label: string
  nb: number
}

type TopSaleRow = {
  nomItem: string
  benefice: number
}

type RankRow = {
  label: string
  value: number
}

type DashboardMonth = {
  month: number
  revenue: number
  profit: number
  purchaseSpend: number
  itemsSold: number
  itemsBought: number
}

const props = defineProps<{
  initialState?: QuarterlyTemplateState
}>()

const emit = defineEmits<{
  (event: 'state-change', state: QuarterlyTemplateState): void
}>()

const monthLabelsShort = ['Jan', 'Fev', 'Mar', 'Avr', 'Mai', 'Juin', 'Juil', 'Aout', 'Sep', 'Oct', 'Nov', 'Dec']
const quarterLabels = ['T1', 'T2', 'T3', 'T4']
const quarterLongLabels = ['1er trimestre', '2e trimestre', '3e trimestre', '4e trimestre']
const today = new Date()
const currentYear = today.getFullYear()
const currentQuarter = quarterFromMonth(today.getMonth() + 1)

const selectedYear = ref(normalizeInitialYear(props.initialState))
const selectedQuarter = ref(normalizeInitialQuarter(props.initialState))
const yearDraft = ref(String(selectedYear.value))
const minDate = ref('')
const maxDate = ref(formatYmd(today))
const minQuarterSerial = ref<number | null>(null)
const maxQuarterSerial = ref<number | null>(null)
const loading = ref(false)
const error = ref('')
const hasLoadedOnce = ref(false)
const activePage = ref(1)
const pointerDrag = ref({
  active: false,
  pointerId: -1,
  startX: 0,
  startY: 0,
  startedAt: 0,
})
const summary = ref<StatsSummary>(emptySummary())
const previousSummary = ref<StatsSummary>(emptySummary())
const timeseries = ref<TimePoint[]>([])
const brands = ref<BrandRow[]>([])
const topSales = ref<TopSaleRow[]>([])
const categoryProfit = ref<RankRow[]>([])
const annualMonths = ref<DashboardMonth[]>(emptyAnnualMonths())
let requestId = 0
let lastWheelPageChangeAt = 0

const pages = [
  { key: 'flow', label: 'Flux & mix' },
  { key: 'pilotage', label: 'Pilotage trimestriel' },
  { key: 'details', label: 'Details & contexte' },
]

const quarterOptions = [
  { value: 1, label: 'T1' },
  { value: 2, label: 'T2' },
  { value: 3, label: 'T3' },
  { value: 4, label: 'T4' },
]

const moneyFormatter = new Intl.NumberFormat('fr-FR', {
  style: 'currency',
  currency: 'EUR',
  maximumFractionDigits: 0,
})
const compactMoneyFormatter = new Intl.NumberFormat('fr-FR', {
  style: 'currency',
  currency: 'EUR',
  notation: 'compact',
  maximumFractionDigits: 1,
})
const numberFormatter = new Intl.NumberFormat('fr-FR', { maximumFractionDigits: 0 })
const percentFormatter = new Intl.NumberFormat('fr-FR', {
  style: 'percent',
  maximumFractionDigits: 1,
})
const signedPercentFormatter = new Intl.NumberFormat('fr-FR', {
  style: 'percent',
  signDisplay: 'exceptZero',
  maximumFractionDigits: 1,
})

const selectedQuarterLabel = computed(() => `${quarterLabels[selectedQuarter.value - 1]} ${selectedYear.value}`)
const selectedQuarterLongLabel = computed(
  () => `${quarterLongLabels[selectedQuarter.value - 1]} ${selectedYear.value}`,
)
const periodRange = computed(() => buildQuarterRange(selectedYear.value, selectedQuarter.value))
const periodShortLabel = computed(() => {
  return `${formatDateCompact(periodRange.value.from)} -> ${formatDateCompact(periodRange.value.to)}`
})
const currentPage = computed(() => pages[activePage.value] ?? pages[1])
const pageTrackStyle = computed(() => ({
  transform: 'translate3d(0, 0, 0)',
}))
const canGoPreviousQuarter = computed(() => {
  if (minQuarterSerial.value == null) return true
  return quarterSerial(selectedYear.value, selectedQuarter.value) > minQuarterSerial.value
})
const canGoNextQuarter = computed(() => {
  if (maxQuarterSerial.value == null) return true
  return quarterSerial(selectedYear.value, selectedQuarter.value) < maxQuarterSerial.value
})

const quarterMonthRows = computed(() => {
  const startMonth = (selectedQuarter.value - 1) * 3 + 1
  const months = annualMonths.value.filter(
    (row) => row.month >= startMonth && row.month < startMonth + 3,
  )
  const seriesByMonth = new Map(
    timeseries.value.map((row) => [monthKeyFromYmd(row.date), row]),
  )
  return months.map((row) => {
    const monthKey = `${selectedYear.value}-${pad(row.month)}`
    const point = seriesByMonth.get(monthKey)
    return {
      ...row,
      revenue: point?.ca ?? row.revenue,
      profit: point?.profit ?? row.profit,
    }
  })
})

const quarterMonthListLabel = computed(() =>
  quarterMonthRows.value.map((row) => monthLabelsShort[row.month - 1]).join(' • '),
)

const quarterTotals = computed(() => {
  const rows = quarterMonthRows.value
  const revenue = summary.value.ca || rows.reduce((sum, row) => sum + row.revenue, 0)
  const profit = summary.value.profit || rows.reduce((sum, row) => sum + row.profit, 0)
  const itemsSold = summary.value.itemsVendues || rows.reduce((sum, row) => sum + row.itemsSold, 0)
  const purchaseSpend = rows.reduce((sum, row) => sum + row.purchaseSpend, 0)
  const itemsBought = rows.reduce((sum, row) => sum + row.itemsBought, 0)
  const marginRate = summary.value.profitMargin || (revenue > 0 ? profit / revenue : 0)
  const averageSalePrice = itemsSold > 0 ? revenue / itemsSold : 0
  const averageProfit = itemsSold > 0 ? profit / itemsSold : 0
  return {
    revenue,
    profit,
    itemsSold,
    purchaseSpend,
    itemsBought,
    marginRate,
    averageSalePrice,
    averageProfit,
    remainingStockCount: summary.value.itemsEnStock,
    remainingStockValue: summary.value.valeurStock,
    cashNet: revenue - purchaseSpend,
  }
})

const previousTotals = computed(() => ({
  revenue: previousSummary.value.ca,
  profit: previousSummary.value.profit,
  itemsSold: previousSummary.value.itemsVendues,
  stockValue: previousSummary.value.valeurStock,
}))

const brandPreview = computed(() => brands.value.slice(0, 5))
const hasQuarterData = computed(() => {
  const totals = quarterTotals.value
  return (
    totals.revenue !== 0 ||
    totals.profit !== 0 ||
    totals.itemsSold > 0 ||
    totals.purchaseSpend !== 0 ||
    totals.itemsBought > 0 ||
    totals.remainingStockCount > 0 ||
    totals.remainingStockValue !== 0 ||
    topSales.value.length > 0 ||
    brands.value.length > 0
  )
})

const kpiCards = computed(() => [
  {
    label: "Chiffre d'affaires",
    value: formatMoney(quarterTotals.value.revenue),
    detail: formatDelta(quarterTotals.value.revenue, previousTotals.value.revenue),
    tone: 'primary' as const,
    icon: BadgeEuro,
  },
  {
    label: 'Benefice net',
    value: formatMoney(quarterTotals.value.profit),
    detail: `${formatMoney(quarterTotals.value.averageProfit)} par vente`,
    tone: quarterTotals.value.profit >= 0 ? ('profit' as const) : ('warning' as const),
    icon: TrendingUp,
  },
  {
    label: 'Marge du trimestre',
    value: formatRatio(quarterTotals.value.marginRate),
    detail: 'Profit / chiffre d affaires',
    tone: quarterTotals.value.marginRate >= 0 ? ('profit' as const) : ('warning' as const),
    icon: CirclePercent,
  },
  {
    label: 'Ventes du trimestre',
    value: formatNumber(quarterTotals.value.itemsSold),
    detail: `${formatMoney(quarterTotals.value.averageSalePrice)} panier moyen`,
    tone: 'primary' as const,
    icon: ShoppingBag,
  },
  {
    label: 'Achats du trimestre',
    value: formatMoney(quarterTotals.value.purchaseSpend),
    detail: `${formatNumber(quarterTotals.value.itemsBought)} achats de stock`,
    tone: 'primary' as const,
    icon: Wallet,
  },
  {
    label: 'Stock fin de trimestre',
    value: `${formatNumber(quarterTotals.value.remainingStockCount)} articles`,
    detail: `${formatMoney(quarterTotals.value.remainingStockValue)} au ${formatDate(periodRange.value.to)}`,
    tone:
      quarterTotals.value.remainingStockCount > 0 ? ('warning' as const) : ('neutral' as const),
    icon: Boxes,
  },
])

const visibleBrandPreview = computed(() => brandPreview.value.slice(0, 3))
const quickStripItems = computed(() => [
  { label: 'Panier moyen', value: formatMoney(quarterTotals.value.averageSalePrice) },
  { label: 'Depenses achat', value: formatMoney(quarterTotals.value.purchaseSpend) },
  { label: 'Profit moyen', value: formatMoney(quarterTotals.value.averageProfit) },
  { label: 'Cash net', value: formatMoney(quarterTotals.value.cashNet) },
])

const quarterPerformanceOption = computed(() => ({
  color: ['#4f46e5', '#d97706', '#059669'],
  grid: { left: 8, right: 12, top: 38, bottom: 12, containLabel: true },
  tooltip: {
    trigger: 'axis',
    confine: true,
    axisPointer: { type: 'shadow' },
    valueFormatter: (value: number) => compactMoneyFormatter.format(Number(value ?? 0)),
  },
  legend: {
    top: 0,
    right: 2,
    itemWidth: 10,
    itemHeight: 10,
    textStyle: { color: '#64748b', fontSize: 12 },
  },
  xAxis: {
    type: 'category',
    data: quarterMonthRows.value.map((row) => monthLabelsShort[row.month - 1]),
    axisTick: { show: false },
    axisLine: { lineStyle: { color: '#cbd5e1' } },
    axisLabel: { color: '#64748b', interval: 0 },
  },
  yAxis: {
    type: 'value',
    splitLine: { lineStyle: { color: 'rgba(148, 163, 184, 0.22)' } },
    axisLabel: {
      color: '#64748b',
      formatter: (value: number) => compactMoneyFormatter.format(Number(value ?? 0)),
    },
  },
  series: [
    {
      name: 'CA',
      type: 'bar',
      barMaxWidth: 28,
      data: quarterMonthRows.value.map((row) => row.revenue),
      itemStyle: { borderRadius: [6, 6, 0, 0], color: 'rgba(79, 70, 229, 0.68)' },
    },
    {
      name: 'Achats',
      type: 'bar',
      barMaxWidth: 28,
      data: quarterMonthRows.value.map((row) => row.purchaseSpend),
      itemStyle: { borderRadius: [6, 6, 0, 0], color: 'rgba(217, 119, 6, 0.58)' },
    },
    {
      name: 'Profit',
      type: 'line',
      smooth: true,
      symbolSize: 7,
      data: quarterMonthRows.value.map((row) => row.profit),
      lineStyle: { width: 3.2 },
      areaStyle: { color: 'rgba(5, 150, 105, 0.1)' },
    },
  ],
}))

const categoryProfitOption = computed(() => {
  const rows = [...categoryProfit.value].sort((a, b) => a.value - b.value)
  return {
    grid: { left: 8, right: 20, top: 10, bottom: 10, containLabel: true },
    tooltip: {
      trigger: 'axis',
      confine: true,
      axisPointer: { type: 'shadow' },
      formatter: (params: Array<{ dataIndex: number }>) => {
        const item = rows[params?.[0]?.dataIndex ?? 0]
        return item ? `${item.label}<br/>Profit: ${formatMoney(item.value)}` : ''
      },
    },
    xAxis: {
      type: 'value',
      splitLine: { lineStyle: { color: 'rgba(148, 163, 184, 0.2)' } },
      axisLabel: {
        color: '#64748b',
        formatter: (value: number) => compactMoneyFormatter.format(Number(value ?? 0)),
      },
    },
    yAxis: {
      type: 'category',
      data: rows.map((item) => item.label),
      axisTick: { show: false },
      axisLine: { show: false },
      axisLabel: { color: '#334155', width: 110, overflow: 'truncate' },
    },
    series: [
      {
        name: 'Profit',
        type: 'bar',
        barMaxWidth: 24,
        data: rows.map((item) => item.value),
        itemStyle: {
          borderRadius: [0, 6, 6, 0],
          color: (params: { value: number }) =>
            Number(params.value ?? 0) >= 0 ? '#10b981' : '#f97316',
        },
      },
    ],
  }
})

const yearContextOption = computed(() => {
  const startMonth = (selectedQuarter.value - 1) * 3 + 1
  const endMonth = startMonth + 2
  return {
    color: ['#4f46e5', '#059669', '#d97706'],
    grid: { left: 8, right: 10, top: 38, bottom: 12, containLabel: true },
    tooltip: {
      trigger: 'axis',
      confine: true,
      axisPointer: { type: 'shadow' },
      valueFormatter: (value: number) => compactMoneyFormatter.format(Number(value ?? 0)),
    },
    legend: {
      top: 0,
      right: 2,
      itemWidth: 10,
      itemHeight: 10,
      textStyle: { color: '#64748b', fontSize: 12 },
    },
    xAxis: {
      type: 'category',
      data: monthLabelsShort,
      axisTick: { show: false },
      axisLine: { lineStyle: { color: '#cbd5e1' } },
      axisLabel: { color: '#64748b', interval: 0 },
    },
    yAxis: {
      type: 'value',
      splitLine: { lineStyle: { color: 'rgba(148, 163, 184, 0.22)' } },
      axisLabel: {
        color: '#64748b',
        formatter: (value: number) => compactMoneyFormatter.format(Number(value ?? 0)),
      },
    },
    series: [
      {
        name: 'CA',
        type: 'bar',
        barMaxWidth: 18,
        data: annualMonths.value.map((row) => row.revenue),
        itemStyle: {
          borderRadius: [5, 5, 0, 0],
          color: (params: { dataIndex: number }) =>
            params.dataIndex + 1 >= startMonth && params.dataIndex + 1 <= endMonth
              ? '#4f46e5'
              : 'rgba(79, 70, 229, 0.18)',
        },
      },
      {
        name: 'Profit',
        type: 'line',
        smooth: true,
        symbolSize: 6,
        data: annualMonths.value.map((row) => row.profit),
        lineStyle: { width: 3.2 },
        areaStyle: { color: 'rgba(5, 150, 105, 0.08)' },
      },
      {
        name: 'Achats',
        type: 'bar',
        barMaxWidth: 18,
        data: annualMonths.value.map((row) => row.purchaseSpend),
        itemStyle: {
          borderRadius: [5, 5, 0, 0],
          color: (params: { dataIndex: number }) =>
            params.dataIndex + 1 >= startMonth && params.dataIndex + 1 <= endMonth
              ? '#d97706'
              : 'rgba(217, 119, 6, 0.18)',
        },
      },
    ],
  }
})

watch(
  () => props.initialState,
  (state) => {
    const next = clampSelection(normalizeInitialYear(state), normalizeInitialQuarter(state))
    if (selectedYear.value !== next.year) selectedYear.value = next.year
    if (selectedQuarter.value !== next.quarter) selectedQuarter.value = next.quarter
  },
  { deep: true },
)

watch([selectedYear, selectedQuarter], () => {
  yearDraft.value = String(selectedYear.value)
  emit('state-change', {
    year: selectedYear.value,
    quarter: selectedQuarter.value,
  })
  loadDashboard()
}, { immediate: true })

function normalizeInitialYear(state?: QuarterlyTemplateState) {
  const next = Math.trunc(Number(state?.year))
  return Number.isFinite(next) && next > 2000 ? next : currentYear
}

function normalizeInitialQuarter(state?: QuarterlyTemplateState) {
  const next = Math.trunc(Number(state?.quarter))
  return next >= 1 && next <= 4 ? next : currentQuarter
}

function emptySummary(): StatsSummary {
  return {
    ca: 0,
    profit: 0,
    profitMargin: 0,
    itemsVendues: 0,
    itemsEnStock: 0,
    valeurStock: 0,
  }
}

function emptyAnnualMonths(): DashboardMonth[] {
  return Array.from({ length: 12 }, (_, index) => ({
    month: index + 1,
    revenue: 0,
    profit: 0,
    purchaseSpend: 0,
    itemsSold: 0,
    itemsBought: 0,
  }))
}

function normalizeAnnualMonths(raw: unknown): DashboardMonth[] {
  const rows = emptyAnnualMonths()
  if (!Array.isArray(raw)) return rows
  for (const item of raw) {
    const month = Math.trunc(Number(item?.month ?? 0))
    if (month < 1 || month > 12) continue
    rows[month - 1] = {
      month,
      revenue: toNumber(item?.revenue),
      profit: toNumber(item?.profit),
      purchaseSpend: toNumber(item?.purchaseSpend),
      itemsSold: toNumber(item?.itemsSold),
      itemsBought: toNumber(item?.itemsBought),
    }
  }
  return rows
}

function quarterFromMonth(month: number) {
  return Math.max(1, Math.min(4, Math.floor((month - 1) / 3) + 1))
}

function quarterSerial(year: number, quarter: number) {
  return year * 4 + (quarter - 1)
}

function quarterFromYmd(value: string) {
  const [year, month] = String(value).split('-').map(Number)
  return { year: year || currentYear, quarter: quarterFromMonth(month || 1) }
}

function clampSelection(year: number, quarter: number) {
  let serial = quarterSerial(year, quarter)
  if (minQuarterSerial.value != null) serial = Math.max(serial, minQuarterSerial.value)
  if (maxQuarterSerial.value != null) serial = Math.min(serial, maxQuarterSerial.value)
  return {
    year: Math.floor(serial / 4),
    quarter: (serial % 4) + 1,
  }
}

function buildQuarterRange(year: number, quarter: number) {
  const startMonth = (quarter - 1) * 3
  const first = new Date(year, startMonth, 1)
  const last = new Date(year, startMonth + 3, 0)
  let from = formatYmd(first)
  let to = formatYmd(last)
  const minQuarter = minDate.value ? quarterFromYmd(minDate.value) : null
  const maxQuarter = maxDate.value ? quarterFromYmd(maxDate.value) : null
  if (minQuarter && minQuarter.year === year && minQuarter.quarter === quarter && from < minDate.value) {
    from = minDate.value
  }
  if (maxQuarter && maxQuarter.year === year && maxQuarter.quarter === quarter && to > maxDate.value) {
    to = maxDate.value
  }
  if (from > to) from = to
  return { from, to }
}

function setQuarter(quarter: number) {
  const next = clampSelection(selectedYear.value, quarter)
  selectedYear.value = next.year
  selectedQuarter.value = next.quarter
}

function changeQuarter(delta: number) {
  const nextSerial = quarterSerial(selectedYear.value, selectedQuarter.value) + delta
  const next = clampSelection(Math.floor(nextSerial / 4), (nextSerial % 4) + 1)
  selectedYear.value = next.year
  selectedQuarter.value = next.quarter
}

function onYearInput(event: Event) {
  const input = event.target as HTMLInputElement | null
  yearDraft.value = String(input?.value ?? '').replace(/[^\d]/g, '').slice(0, 4)
}

function commitYear(fallback = selectedYear.value) {
  const parsed = Math.trunc(Number(yearDraft.value || fallback))
  const safeYear = Number.isFinite(parsed) && parsed > 2000 ? parsed : fallback
  const next = clampSelection(safeYear, selectedQuarter.value)
  selectedYear.value = next.year
  selectedQuarter.value = next.quarter
  yearDraft.value = String(next.year)
}

function monthKeyFromYmd(value: string) {
  return /^\d{4}-\d{2}-\d{2}$/.test(value) ? value.slice(0, 7) : ''
}

function parseYmd(value: string) {
  const [year, month, day] = String(value).split('-').map(Number)
  return new Date(year, (month || 1) - 1, day || 1)
}

function formatYmd(date: Date) {
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}

function formatDate(value: unknown) {
  if (!value) return '--'
  const date = parseYmd(String(value))
  if (Number.isNaN(date.getTime())) return '--'
  return date.toLocaleDateString('fr-FR', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
  })
}

function formatDateCompact(value: string) {
  const date = parseYmd(value)
  if (Number.isNaN(date.getTime())) return value
  return date.toLocaleDateString('fr-FR', {
    day: '2-digit',
    month: 'short',
  })
}

function pad(value: number) {
  return String(value).padStart(2, '0')
}

function toNumber(value: unknown, fallback = 0) {
  const next = Number(value ?? fallback)
  return Number.isFinite(next) ? next : fallback
}

function formatMoney(value: unknown) {
  const next = toNumber(value)
  return Math.abs(next) >= 100000 ? compactMoneyFormatter.format(next) : moneyFormatter.format(next)
}

function formatNumber(value: unknown) {
  return numberFormatter.format(toNumber(value))
}

function formatRatio(value: unknown) {
  return percentFormatter.format(toNumber(value))
}

function formatSignedRatio(value: unknown) {
  return signedPercentFormatter.format(toNumber(value) / 100)
}

function deltaPct(current: number, previous: number) {
  const curr = toNumber(current)
  const prev = toNumber(previous)
  if (prev === 0) return null
  return ((curr - prev) / Math.abs(prev)) * 100
}

function formatDelta(current: number, previous: number) {
  const delta = deltaPct(current, previous)
  if (delta == null) return 'Comparaison indisponible'
  return `${formatSignedRatio(delta)} vs periode precedente`
}

function profitClass(value: unknown) {
  return toNumber(value) >= 0 ? 'is-positive' : 'is-negative'
}

function goToPage(index: number) {
  activePage.value = Math.max(0, Math.min(pages.length - 1, index))
}

function previousPage() {
  goToPage(activePage.value - 1)
}

function nextPage() {
  goToPage(activePage.value + 1)
}

function canStartPointerSlide(target: EventTarget | null) {
  if (!(target instanceof HTMLElement)) return true
  return !target.closest('button, input, select, textarea, a, [contenteditable="true"]')
}

function onPointerDown(event: PointerEvent) {
  if (event.button !== 0 || !canStartPointerSlide(event.target)) return
  pointerDrag.value = {
    active: true,
    pointerId: event.pointerId,
    startX: event.clientX,
    startY: event.clientY,
    startedAt: performance.now(),
  }
  const target = event.currentTarget
  if (target instanceof HTMLElement && target.setPointerCapture) {
    target.setPointerCapture(event.pointerId)
  }
}

function onPointerUp(event: PointerEvent) {
  const drag = pointerDrag.value
  if (!drag.active || drag.pointerId !== event.pointerId) return
  const deltaX = event.clientX - drag.startX
  const deltaY = event.clientY - drag.startY
  const duration = performance.now() - drag.startedAt
  const target = event.currentTarget
  if (target instanceof HTMLElement && target.releasePointerCapture) {
    try {
      target.releasePointerCapture(event.pointerId)
    } catch {
      // Pointer capture may already be released by the browser.
    }
  }
  resetPointerDrag()

  const horizontalIntent = Math.abs(deltaX) > Math.abs(deltaY) * 1.25
  const strongDrag = Math.abs(deltaX) >= 72
  const quickFlick = Math.abs(deltaX) >= 38 && duration <= 260
  if (!horizontalIntent || (!strongDrag && !quickFlick)) return

  if (deltaX < 0) nextPage()
  else previousPage()
}

function resetPointerDrag() {
  if (!pointerDrag.value.active) return
  pointerDrag.value = {
    active: false,
    pointerId: -1,
    startX: 0,
    startY: 0,
    startedAt: 0,
  }
}

function onWheel(event: WheelEvent) {
  if (!canStartPointerSlide(event.target)) return
  const delta =
    Math.abs(event.deltaX) >= Math.abs(event.deltaY) ? event.deltaX : event.shiftKey ? event.deltaY : 0
  const horizontalIntent = Math.abs(event.deltaX) > Math.abs(event.deltaY) * 1.15 || event.shiftKey
  if (!horizontalIntent || Math.abs(delta) < 28) return

  const now = performance.now()
  if (now - lastWheelPageChangeAt < 520) {
    event.preventDefault()
    return
  }

  event.preventDefault()
  lastWheelPageChangeAt = now
  if (delta > 0) nextPage()
  else previousPage()
}

function onKeyDown(event: KeyboardEvent) {
  const target = event.target as HTMLElement | null
  if (target?.closest('input, select, textarea, [contenteditable="true"]')) return
  if (event.key === 'ArrowLeft') {
    event.preventDefault()
    previousPage()
  } else if (event.key === 'ArrowRight') {
    event.preventDefault()
    nextPage()
  }
}

async function loadDashboard() {
  const id = ++requestId
  loading.value = true
  error.value = ''
  try {
    const period = periodRange.value
    const previousPeriod = prevPeriod(period.from, period.to)
    const [
      summaryResponse,
      previousSummaryResponse,
      timeseriesResponse,
      brandsResponse,
      topSalesResponse,
      categoryProfitResponse,
      annualResponse,
    ] = await Promise.all([
      StatsServices.summary({ from: period.from, to: period.to, asOf: period.to }),
      StatsServices.summary({
        from: previousPeriod.from,
        to: previousPeriod.to,
        asOf: previousPeriod.to,
      }),
      StatsServices.timeseries(period.from, period.to, 'month'),
      StatsServices.brands(period.from, period.to),
      StatsServices.topSales(period.from, period.to, 6),
      StatsServices.rank('topCategoriesProfit', period.from, period.to, 5),
      StatsServices.annualDashboard(selectedYear.value),
    ])
    if (id !== requestId) return
    summary.value = normalizeSummary(summaryResponse.data)
    previousSummary.value = normalizeSummary(previousSummaryResponse.data)
    timeseries.value = normalizeTimeseries(timeseriesResponse.data)
    brands.value = normalizeBrands(brandsResponse.data)
    topSales.value = normalizeTopSales(topSalesResponse.data)
    categoryProfit.value = normalizeRank(categoryProfitResponse.data)
    annualMonths.value = normalizeAnnualMonths(annualResponse.data?.monthly)
    hasLoadedOnce.value = true
  } catch (err: unknown) {
    if (id !== requestId) return
    error.value = err?.response?.data?.message ?? err?.message ?? 'Erreur inconnue'
  } finally {
    if (id === requestId) loading.value = false
  }
}

async function loadQuarterBounds() {
  try {
    const { data } = await StatsServices.dateBounds()
    minDate.value = typeof data?.minDate === 'string' ? data.minDate : ''
    maxDate.value = typeof data?.maxDate === 'string' ? data.maxDate : formatYmd(today)
    minQuarterSerial.value = minDate.value
      ? quarterSerial(quarterFromYmd(minDate.value).year, quarterFromYmd(minDate.value).quarter)
      : null
    maxQuarterSerial.value = maxDate.value
      ? quarterSerial(quarterFromYmd(maxDate.value).year, quarterFromYmd(maxDate.value).quarter)
      : quarterSerial(currentYear, currentQuarter)
    const next = clampSelection(selectedYear.value, selectedQuarter.value)
    selectedYear.value = next.year
    selectedQuarter.value = next.quarter
  } catch {
    minDate.value = ''
    maxDate.value = formatYmd(today)
    minQuarterSerial.value = null
    maxQuarterSerial.value = quarterSerial(currentYear, currentQuarter)
    const next = clampSelection(selectedYear.value, selectedQuarter.value)
    selectedYear.value = next.year
    selectedQuarter.value = next.quarter
  }
}

onMounted(async () => {
  window.addEventListener('keydown', onKeyDown)
  await loadQuarterBounds()
})

onBeforeUnmount(() => {
  window.removeEventListener('keydown', onKeyDown)
})
</script>

<style scoped>
.quarterly-dashboard {
  --quarterly-gap: clamp(10px, 1.35vh, 16px);
  --quarterly-bg: #f7f4ee;
  --quarterly-muted-bg: #fbfaf6;
  width: 100%;
  height: 100%;
  min-width: 0;
  min-height: 0;
  overflow: auto;
  overscroll-behavior: contain;
  overscroll-behavior-x: none;
  scrollbar-width: thin;
  scrollbar-color: rgba(100, 116, 139, 0.34) rgba(234, 229, 219, 0.9);
  background: var(--quarterly-bg);
}

.quarterly-dashboard::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}

.quarterly-dashboard::-webkit-scrollbar-track {
  background: rgba(234, 229, 219, 0.9);
}

.quarterly-dashboard::-webkit-scrollbar-thumb {
  border-radius: 999px;
  background: rgba(100, 116, 139, 0.34);
}

.quarterly-dashboard__inner {
  width: min(1860px, 100%);
  min-height: 100%;
  min-width: 0;
  margin: 0 auto;
  padding: clamp(14px, 1.8vw, 24px) clamp(14px, 2.2vw, 28px) clamp(16px, 2vw, 24px)
    calc(96px + clamp(14px, 2.2vw, 28px));
  display: grid;
  grid-template-rows: auto auto auto;
  gap: var(--quarterly-gap);
  align-content: start;
}

.quarterly-header {
  min-width: 0;
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 18px;
}

.quarterly-header__copy {
  min-width: 0;
}

.quarterly-header__kicker,
.quarterly-page__heading p,
.quarterly-panel__head p {
  color: #5b5ce2;
  font-weight: 820;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

.quarterly-header__kicker {
  margin: 0 0 7px;
  font-size: 0.72rem;
  letter-spacing: 0.16em;
}

.quarterly-header h1 {
  margin: 0;
  color: #111827;
  font-size: clamp(1.62rem, 2.8vw, 2.45rem);
  line-height: 1.02;
  font-weight: 860;
}

.quarterly-header p {
  margin: 7px 0 0;
  max-width: 72ch;
  color: #64748b;
  font-size: 0.96rem;
  line-height: 1.45;
}

.quarterly-quarter {
  flex: 0 0 auto;
  width: min(380px, 100%);
  border: 1px solid rgba(99, 102, 241, 0.2);
  border-radius: 12px;
  background: #ffffff;
  box-shadow:
    0 14px 34px rgba(31, 41, 55, 0.1),
    inset 0 1px 0 rgba(255, 255, 255, 0.8);
  padding: 10px;
  display: grid;
  gap: 10px;
}

.quarterly-quarter__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.quarterly-quarter__head span {
  color: #64748b;
  font-size: 0.68rem;
  font-weight: 780;
  letter-spacing: 0.11em;
  text-transform: uppercase;
}

.quarterly-quarter__head small {
  color: #5b5ce2;
  font-size: 0.75rem;
  font-weight: 700;
}

.quarterly-quarter__control {
  display: grid;
  grid-template-columns: 42px minmax(0, 1fr) 42px;
  gap: 8px;
}

.quarterly-quarter__control button {
  border: 1px solid rgba(148, 163, 184, 0.26);
  border-radius: 10px;
  background: #f8fafc;
  color: #334155;
  display: grid;
  place-items: center;
}

.quarterly-quarter__control button:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.quarterly-quarter__summary {
  min-width: 0;
  border: 1px solid rgba(148, 163, 184, 0.24);
  border-radius: 10px;
  background: linear-gradient(180deg, #ffffff, #f8fafc);
  padding: 9px 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.quarterly-quarter__summary strong {
  color: #111827;
  font-size: 1rem;
  font-weight: 800;
}

.quarterly-quarter__summary input {
  width: 82px;
  border: 0;
  background: transparent;
  color: #475569;
  font-size: 0.92rem;
  font-weight: 700;
  text-align: right;
  outline: none;
}

.quarterly-quarter__tabs {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 8px;
}

.quarterly-quarter__tab {
  min-height: 38px;
  border: 1px solid rgba(148, 163, 184, 0.22);
  border-radius: 10px;
  background: #f8fafc;
  color: #475569;
  font-size: 0.9rem;
  font-weight: 800;
  transition:
    border-color 140ms ease,
    color 140ms ease,
    background 140ms ease,
    transform 140ms ease;
}

.quarterly-quarter__tab:hover {
  transform: translateY(-1px);
}

.quarterly-quarter__tab.is-active {
  border-color: rgba(91, 92, 226, 0.28);
  background: rgba(91, 92, 226, 0.12);
  color: #3730a3;
}

.quarterly-state,
.quarterly-page,
.quarterly-panel,
.quarterly-action-summary,
.quarterly-action-card,
.quarterly-table-card {
  border: 1px solid rgba(148, 163, 184, 0.18);
  border-radius: 12px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(250, 250, 248, 0.96));
  box-shadow: 0 14px 34px rgba(31, 41, 55, 0.08);
}

.quarterly-state {
  min-height: 280px;
  padding: clamp(24px, 4vw, 40px);
  display: grid;
  place-items: center;
  text-align: center;
  gap: 10px;
}

.quarterly-state h2 {
  margin: 0;
  color: #111827;
  font-size: clamp(1.3rem, 2vw, 1.7rem);
  font-weight: 820;
}

.quarterly-state p {
  margin: 0;
  color: #64748b;
  max-width: 56ch;
  line-height: 1.5;
}

.quarterly-state button {
  min-height: 42px;
  padding: 0 16px;
  border: 0;
  border-radius: 999px;
  background: linear-gradient(135deg, #5b5ce2, #3b82f6);
  color: #fff;
  font-weight: 760;
}

.quarterly-state__pulse {
  width: 58px;
  height: 58px;
  border-radius: 999px;
  background: radial-gradient(circle, rgba(91, 92, 226, 0.32) 0%, rgba(91, 92, 226, 0) 70%);
  animation: quarterly-pulse 1.2s ease-in-out infinite;
}

.quarterly-page-nav {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
}

.quarterly-page-nav__arrow {
  width: 42px;
  height: 42px;
  flex: 0 0 auto;
  border: 1px solid rgba(148, 163, 184, 0.2);
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.9);
  color: #334155;
}

.quarterly-page-nav__arrow:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.quarterly-page-nav__center {
  min-width: 0;
  display: grid;
  justify-items: center;
  gap: 5px;
  text-align: center;
}

.quarterly-page-nav__count {
  color: #64748b;
  font-size: 0.72rem;
  font-weight: 780;
  letter-spacing: 0.11em;
  text-transform: uppercase;
}

.quarterly-page-nav__center strong {
  color: #111827;
  font-size: 1rem;
  font-weight: 800;
}

.quarterly-page-nav__dots {
  display: flex;
  align-items: center;
  gap: 8px;
}

.quarterly-page-nav__dot {
  width: 8px;
  height: 8px;
  border: 0;
  border-radius: 999px;
  background: rgba(100, 116, 139, 0.24);
}

.quarterly-page-nav__dot.is-active {
  width: 24px;
  background: #5b5ce2;
}

.quarterly-stage {
  min-width: 0;
}

.quarterly-pages,
.quarterly-page {
  min-width: 0;
}

.quarterly-page {
  padding: clamp(14px, 1.6vw, 20px);
  display: grid;
  gap: var(--quarterly-gap);
}

.quarterly-page__heading {
  min-width: 0;
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 12px;
}

.quarterly-page__heading p,
.quarterly-panel__head p {
  margin: 0 0 6px;
  font-size: 0.7rem;
}

.quarterly-page__heading h2,
.quarterly-panel__head h2 {
  margin: 0;
  color: #111827;
  font-size: clamp(1.1rem, 2vw, 1.45rem);
  font-weight: 820;
  line-height: 1.1;
}

.quarterly-page__heading span,
.quarterly-panel__head span {
  color: #64748b;
  font-size: 0.84rem;
  font-weight: 700;
}

.quarterly-flow-layout,
.quarterly-table-grid {
  min-width: 0;
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) minmax(320px, 0.92fr);
  gap: var(--quarterly-gap);
}

.quarterly-flow-side {
  display: grid;
  gap: var(--quarterly-gap);
}

.quarterly-panel {
  min-width: 0;
  padding: clamp(14px, 1.5vw, 18px);
  display: grid;
  gap: 14px;
}

.quarterly-panel__head {
  min-width: 0;
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 12px;
}

.quarterly-chart,
.quarterly-chart-wrap {
  min-width: 0;
  min-height: 0;
}

.quarterly-chart {
  width: 100%;
  height: 320px;
}

.quarterly-chart--context {
  height: 260px;
}

.quarterly-mini-empty {
  min-height: 180px;
  border: 1px dashed rgba(148, 163, 184, 0.26);
  border-radius: 10px;
  background: var(--quarterly-muted-bg);
  color: #64748b;
  display: grid;
  place-items: center;
  text-align: center;
  padding: 16px;
}

.quarterly-insights {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.quarterly-insight,
.quarterly-brand-row {
  border: 1px solid rgba(148, 163, 184, 0.18);
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.76);
  padding: 12px;
}

.quarterly-insight {
  display: grid;
  gap: 4px;
}

.quarterly-insight span,
.quarterly-brand-row span {
  color: #64748b;
  font-size: 0.78rem;
  font-weight: 700;
}

.quarterly-insight strong,
.quarterly-brand-row strong {
  color: #111827;
  font-size: 1rem;
  font-weight: 820;
}

.quarterly-kpi-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.quarterly-action-overview {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.quarterly-action-summary,
.quarterly-action-card {
  padding: 14px;
  display: grid;
  gap: 6px;
}

.quarterly-action-summary span,
.quarterly-action-card span {
  color: #5b5ce2;
  font-size: 0.72rem;
  font-weight: 800;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.quarterly-action-summary strong,
.quarterly-action-card strong {
  color: #111827;
  font-size: 1.1rem;
  font-weight: 840;
  line-height: 1.1;
}

.quarterly-action-summary small,
.quarterly-action-card p {
  margin: 0;
  color: #64748b;
  line-height: 1.45;
}

.quarterly-action-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.quarterly-action-summary.is-positive,
.quarterly-action-card.is-positive {
  border-color: rgba(16, 185, 129, 0.22);
  background: linear-gradient(180deg, rgba(240, 253, 247, 0.94), rgba(255, 255, 255, 0.98));
}

.quarterly-action-summary.is-warning,
.quarterly-action-card.is-warning {
  border-color: rgba(245, 158, 11, 0.24);
  background: linear-gradient(180deg, rgba(255, 251, 235, 0.94), rgba(255, 255, 255, 0.98));
}

.quarterly-table-card {
  min-width: 0;
}

.quarterly-table-scroll {
  overflow: auto;
  max-height: 420px;
}

.quarterly-table {
  width: 100%;
  border-collapse: collapse;
}

.quarterly-table th,
.quarterly-table td {
  padding: 11px 10px;
  border-bottom: 1px solid rgba(226, 232, 240, 0.9);
  text-align: left;
  vertical-align: top;
}

.quarterly-table th {
  color: #64748b;
  font-size: 0.76rem;
  font-weight: 800;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.quarterly-table td strong {
  display: block;
  color: #111827;
  font-weight: 780;
}

.quarterly-table td span {
  display: block;
  margin-top: 4px;
  color: #64748b;
  font-size: 0.82rem;
}

.quarterly-table .is-positive {
  color: #047857;
  font-weight: 800;
}

.quarterly-table .is-negative {
  color: #b45309;
  font-weight: 800;
}

.quarterly-brand-list {
  display: grid;
  gap: 8px;
}

.quarterly-brand-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.quarterly-brand-row--empty strong {
  color: #94a3b8;
}

@keyframes quarterly-pulse {
  0%,
  100% {
    transform: scale(0.92);
    opacity: 0.72;
  }
  50% {
    transform: scale(1.04);
    opacity: 1;
  }
}

@media (max-width: 1280px) {
  .quarterly-flow-layout,
  .quarterly-table-grid,
  .quarterly-action-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 1080px) {
  .quarterly-dashboard__inner {
    padding-left: clamp(14px, 2vw, 24px);
  }

  .quarterly-header {
    align-items: stretch;
    flex-direction: column;
  }

  .quarterly-quarter {
    width: 100%;
  }

  .quarterly-kpi-grid,
  .quarterly-action-overview {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .quarterly-dashboard__inner {
    padding: 14px 14px 18px;
  }

  .quarterly-kpi-grid,
  .quarterly-action-overview,
  .quarterly-insights {
    grid-template-columns: 1fr;
  }

  .quarterly-page {
    padding: 14px;
  }

  .quarterly-page__heading,
  .quarterly-panel__head {
    align-items: start;
    flex-direction: column;
  }

  .quarterly-quarter__tabs {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .quarterly-chart {
    height: 280px;
  }
}
</style>
<style scoped>
.quarterly-dashboard {
  overflow: hidden;
}

.quarterly-dashboard__inner {
  height: 100%;
  padding-top: 12px;
  padding-bottom: 12px;
  gap: 8px;
}

.quarterly-header p {
  margin-top: 5px;
  font-size: 0.9rem;
}

.quarterly-page {
  gap: 10px;
}

.quarterly-quarter {
  gap: 6px;
  padding: 8px;
}

.quarterly-quarter__summary {
  height: 36px;
  padding-inline: 10px;
}

.quarterly-quarter__tab {
  min-height: 30px;
}

.quarterly-page-nav {
  padding: 6px 8px;
}

.quarterly-page__heading h2 {
  font-size: clamp(1rem, 1.35vw, 1.18rem);
}

.quarterly-page--flow .quarterly-flow-layout {
  gap: 12px;
}

.quarterly-page--flow .quarterly-panel--performance,
.quarterly-page--flow .quarterly-panel--category-profit {
  min-height: clamp(250px, 28vh, 340px);
}

.quarterly-panel {
  padding: clamp(12px, 1.4vw, 15px);
  gap: 10px;
}

.quarterly-panel__head h2 {
  font-size: clamp(0.94rem, 1.2vw, 1.08rem);
}

.quarterly-chart--context {
  height: 210px;
}

.quarterly-insights {
  padding: 8px;
  gap: 6px;
}

.quarterly-insights--panel {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.quarterly-insight {
  padding: 8px 10px;
}

.quarterly-table-grid {
  gap: 12px;
}

.quarterly-table-scroll {
  max-height: clamp(170px, 22vh, 230px);
}

.quarterly-brand-row {
  padding: 8px 10px;
}

.quarterly-brand-list--inline {
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}

.quarterly-brand-list--inline .quarterly-brand-row {
  min-height: 0;
}

.quarterly-page--main .quarterly-kpi-grid :deep(.quarterly-kpi) {
  min-height: 0;
}

@media (max-width: 720px) {
  .quarterly-dashboard {
    overflow: auto;
  }
}
</style>
<style scoped>
.quarterly-dashboard {
  --quarterly-bg: #f7f4ee;
  --quarterly-muted-bg: #fbfaf7;
  background: #f7f4ee;
}

.quarterly-dashboard__inner {
  width: min(1760px, 100%);
}

.quarterly-quarter,
.quarterly-page-nav,
.quarterly-panel,
.quarterly-state {
  border-color: rgba(148, 163, 184, 0.24);
  background: #fbfaf7;
  box-shadow: 0 6px 16px rgba(31, 41, 55, 0.045);
}

.quarterly-page {
  border: 0;
  border-radius: 0;
  background: transparent;
  box-shadow: none;
  padding: 2px;
  gap: 12px;
  overflow: visible;
  align-content: start;
}

.quarterly-stage {
  min-height: 0;
  height: auto;
  overflow: hidden;
  border-radius: 10px;
  cursor: grab;
  touch-action: pan-y;
  user-select: none;
}

.quarterly-stage.is-dragging {
  cursor: grabbing;
}

.quarterly-pages {
  display: flex;
  transition:
    transform 320ms cubic-bezier(0.22, 1, 0.36, 1),
    opacity 180ms ease;
  will-change: transform;
}

.quarterly-page--flow {
  grid-template-rows: auto minmax(0, 1fr) auto;
}

.quarterly-page--main {
  grid-template-rows: auto;
  gap: 10px;
}

.quarterly-main-layout {
  display: grid;
  grid-template-columns: minmax(0, 1.45fr) minmax(350px, 0.78fr);
  gap: 14px;
  align-items: stretch;
}

.quarterly-main-primary {
  min-width: 0;
  display: grid;
  gap: 14px;
  align-content: start;
}

.quarterly-page--details {
  grid-template-rows: auto auto;
  gap: 10px;
}

.quarterly-quarter {
  width: min(360px, 100%);
  gap: 8px;
}

.quarterly-quarter__head small {
  font-size: 0.68rem;
  font-weight: 760;
}

.quarterly-quarter__control {
  grid-template-columns: 34px minmax(0, 1fr) 34px;
  align-items: center;
  gap: 6px;
}

.quarterly-quarter__control button {
  width: 34px;
  height: 36px;
  border-color: rgba(99, 102, 241, 0.22);
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.8);
  color: #4f46e5;
}

.quarterly-quarter__control button:disabled {
  opacity: 0.36;
  pointer-events: none;
}

.quarterly-quarter__summary {
  height: 38px;
  border-color: rgba(99, 102, 241, 0.26);
  background: rgba(255, 255, 255, 0.96);
  padding: 0 12px;
}

.quarterly-quarter__summary strong {
  font-size: 1.02rem;
  font-weight: 820;
}

.quarterly-quarter__summary input {
  width: 72px;
  color: #111827;
  font-size: 1rem;
  font-weight: 820;
}

.quarterly-quarter__tabs {
  gap: 6px;
}

.quarterly-quarter__tab {
  min-height: 34px;
  border-color: rgba(99, 102, 241, 0.16);
  background: rgba(248, 250, 252, 0.9);
  color: #64748b;
  font-size: 0.82rem;
  font-weight: 780;
  transition:
    border-color 140ms ease,
    color 140ms ease,
    background 140ms ease;
}

.quarterly-quarter__tab:hover {
  transform: none;
}

.quarterly-quarter__tab.is-active {
  border-color: rgba(91, 92, 226, 0.28);
  background: rgba(91, 92, 226, 0.12);
  color: #3730a3;
}

.quarterly-page-nav {
  border-radius: 8px;
  padding: 7px 9px;
  display: grid;
  grid-template-columns: 40px minmax(0, 1fr) 40px;
  gap: 10px;
}

.quarterly-page-nav__arrow {
  width: 36px;
  height: 36px;
  border-color: rgba(99, 102, 241, 0.22);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.78);
  color: #4338ca;
  display: grid;
  place-items: center;
}

.quarterly-page-nav__arrow:disabled {
  opacity: 0.36;
  pointer-events: none;
}

.quarterly-page-nav__center {
  grid-template-columns: auto minmax(0, auto) auto;
  justify-content: center;
  align-items: center;
  gap: 10px;
}

.quarterly-page-nav__count {
  letter-spacing: 0.08em;
}

.quarterly-page-nav__center strong {
  font-size: 0.92rem;
  white-space: nowrap;
}

.quarterly-page-nav__dots {
  gap: 6px;
}

.quarterly-page-nav__dot {
  width: 7px;
  height: 7px;
  background: #cbd5e1;
  padding: 0;
}

.quarterly-page-nav__dot.is-active {
  width: 22px;
  background: #5b5ce2;
}

.quarterly-page__heading p {
  margin: 0 0 4px;
  font-size: 0.68rem;
}

.quarterly-page__heading h2 {
  font-size: clamp(1.08rem, 1.5vw, 1.28rem);
  line-height: 1.15;
}

.quarterly-page__heading span {
  max-width: 58ch;
  font-size: 0.86rem;
  line-height: 1.35;
  text-align: right;
}

.quarterly-page--flow .quarterly-flow-layout {
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
  align-items: start;
}

.quarterly-page--flow .quarterly-flow-side {
  gap: 14px;
}

.quarterly-page--flow .quarterly-panel--performance,
.quarterly-page--flow .quarterly-panel--category-profit {
  min-height: clamp(300px, 34vh, 430px);
}

.quarterly-panel {
  border: 1px solid rgba(148, 163, 184, 0.28);
  border-radius: 8px;
  padding: clamp(14px, 1.8vw, 18px);
  grid-template-rows: auto minmax(0, 1fr);
  gap: 12px;
}

.quarterly-panel__head {
  align-items: flex-start;
}

.quarterly-panel__head p {
  margin: 0 0 4px;
  font-size: 0.7rem;
}

.quarterly-panel__head h2 {
  font-size: clamp(1rem, 1.4vw, 1.24rem);
  line-height: 1.15;
  font-weight: 790;
}

.quarterly-panel__head span {
  max-width: 45%;
  border-radius: 999px;
  background: var(--quarterly-muted-bg);
  color: #4338ca;
  padding: 6px 10px;
  font-size: 0.74rem;
  font-weight: 760;
  text-align: right;
}

.quarterly-page--main .quarterly-kpi-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  align-items: stretch;
}

.quarterly-page--main .quarterly-kpi-grid :deep(.quarterly-kpi) {
  min-height: 0;
  padding: 12px 14px;
  gap: 8px;
}

.quarterly-page--main .quarterly-kpi-grid :deep(.quarterly-kpi__label) {
  font-size: 0.68rem;
  letter-spacing: 0.07em;
}

.quarterly-page--main .quarterly-kpi-grid :deep(.quarterly-kpi__icon) {
  width: 17px;
  height: 17px;
}

.quarterly-page--main .quarterly-kpi-grid :deep(.quarterly-kpi__value) {
  font-size: clamp(1.25rem, 2.1vw, 1.9rem);
  line-height: 1;
}

.quarterly-page--main .quarterly-kpi-grid :deep(.quarterly-kpi__detail) {
  font-size: 0.74rem;
  line-height: 1.22;
}

.quarterly-panel--brand-summary {
  padding: 14px 16px;
  gap: 12px;
  align-content: start;
  height: 100%;
}

.quarterly-panel--brand-summary .quarterly-panel__head {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: start;
  gap: 12px;
}

.quarterly-panel--brand-summary .quarterly-panel__head h2 {
  font-size: clamp(1.05rem, 1.35vw, 1.3rem);
  line-height: 1.02;
}

.quarterly-panel--brand-summary .quarterly-panel__head span {
  max-width: none;
  min-width: 76px;
  padding: 5px 10px;
  font-size: 0.72rem;
  line-height: 1.15;
  text-align: center;
}

.quarterly-brand-list--summary {
  grid-template-columns: 1fr;
  gap: 10px;
}

.quarterly-panel--brand-summary .quarterly-brand-row {
  min-height: 58px;
  padding: 10px 14px;
}

.quarterly-panel--brand-summary .quarterly-brand-row span {
  font-size: 0.82rem;
}

.quarterly-panel--brand-summary .quarterly-brand-row strong {
  font-size: 1.08rem;
}

.quarterly-panel--quick-strip {
  padding: 10px 12px;
  gap: 0;
}

.quarterly-quick-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.quarterly-quick-card {
  min-width: 0;
  border: 1px solid rgba(148, 163, 184, 0.16);
  border-radius: 8px;
  background: #fffdf9;
  padding: 12px 14px;
  display: grid;
  gap: 8px;
}

.quarterly-quick-card span {
  color: #64748b;
  font-size: 0.68rem;
  font-weight: 800;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  line-height: 1.15;
}

.quarterly-quick-card strong {
  color: #1f2937;
  font-size: clamp(1.1rem, 1.5vw, 1.55rem);
  font-weight: 820;
  line-height: 1;
}

.quarterly-panel--main-chart {
  padding: 12px;
  gap: 10px;
  grid-template-rows: auto auto;
}

.quarterly-panel--main-chart .quarterly-panel__head {
  align-items: center;
  gap: 8px;
}

.quarterly-panel--main-chart .quarterly-panel__head p {
  display: none;
}

.quarterly-panel--main-chart .quarterly-panel__head h2 {
  font-size: 0.92rem;
  line-height: 1.1;
}

.quarterly-panel--main-chart .quarterly-panel__head span {
  padding: 4px 8px;
  font-size: 0.68rem;
}

.quarterly-chart {
  height: 100%;
}

.quarterly-chart--context {
  height: 260px;
}

.quarterly-insights {
  border: 1px solid rgba(148, 163, 184, 0.26);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.68);
  padding: 10px;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 8px;
}

.quarterly-insights--panel {
  padding: 8px;
  gap: 8px;
}

.quarterly-insight,
.quarterly-brand-row,
.quarterly-mini-empty,
.quarterly-action-card {
  border-color: rgba(148, 163, 184, 0.18);
  background: #fffdf9;
  box-shadow: none;
}

.quarterly-insight {
  border-radius: 7px;
  padding: 8px 10px;
}

.quarterly-insight span {
  font-size: 0.68rem;
  font-weight: 720;
  text-transform: uppercase;
  letter-spacing: 0.06em;
}

.quarterly-insight strong {
  font-size: 0.92rem;
  line-height: 1.08;
}

.quarterly-action-grid {
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 9px;
}

.quarterly-action-card {
  border: 1px solid rgba(148, 163, 184, 0.22);
  border-radius: 8px;
  padding: 10px;
  grid-template-rows: auto auto auto auto;
  align-content: start;
}

.quarterly-action-card span {
  width: fit-content;
  border-radius: 999px;
  background: rgba(99, 102, 241, 0.1);
  color: #4f46e5;
  padding: 3px 7px;
  font-size: 0.58rem;
}

.quarterly-action-card h3 {
  color: #64748b;
  font-size: 0.72rem;
  line-height: 1.15;
}

.quarterly-action-card strong {
  font-size: clamp(1rem, 1.18vw, 1.18rem);
  line-height: 1.05;
}

.quarterly-action-card p {
  font-size: 0.68rem;
  line-height: 1.25;
  font-weight: 680;
}

.quarterly-table-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
  align-items: start;
}

.quarterly-table-card {
  align-content: start;
  overflow: hidden;
}

.quarterly-table-scroll {
  max-height: clamp(260px, 38vh, 430px);
  overflow-y: auto;
  overflow-x: hidden;
}

.quarterly-table {
  table-layout: fixed;
}

.quarterly-table th {
  position: sticky;
  top: 0;
  z-index: 1;
  background: rgba(255, 255, 255, 0.96);
  font-size: 0.7rem;
  font-weight: 780;
  text-align: right;
  padding: 8px 7px;
  border-bottom: 1px solid rgba(148, 163, 184, 0.24);
}

.quarterly-table th:first-child,
.quarterly-table td:first-child {
  text-align: left;
}

.quarterly-table td {
  color: #334155;
  font-size: 0.78rem;
  text-align: right;
  padding: 9px 7px;
  border-bottom: 1px solid rgba(148, 163, 184, 0.16);
  white-space: nowrap;
}

.quarterly-table td:first-child {
  white-space: normal;
}

.quarterly-table strong {
  font-weight: 760;
  line-height: 1.25;
}

.quarterly-table td span {
  margin-top: 3px;
  font-size: 0.74rem;
}

.quarterly-table .is-negative {
  color: #dc2626;
  font-weight: 790;
}

.quarterly-brand-row {
  border-radius: 7px;
  padding: 10px 12px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
}

.quarterly-brand-row span {
  font-size: 0.74rem;
  font-weight: 720;
}

.quarterly-brand-row strong {
  font-size: 0.94rem;
  font-weight: 800;
}

@media (max-width: 1100px) {
  .quarterly-page--flow .quarterly-flow-layout,
  .quarterly-table-grid,
  .quarterly-action-grid {
    grid-template-columns: 1fr;
  }

  .quarterly-main-layout {
    grid-template-columns: 1fr;
  }

  .quarterly-panel--brand-summary {
    width: 100%;
    height: auto;
  }

  .quarterly-page--main .quarterly-kpi-grid,
  .quarterly-brand-list--summary,
  .quarterly-insights {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .quarterly-quick-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .quarterly-page-nav {
    grid-template-columns: 34px minmax(0, 1fr) 34px;
    gap: 6px;
    padding: 6px;
  }

  .quarterly-page-nav__center {
    grid-template-columns: 1fr;
    gap: 4px;
    justify-items: center;
  }

  .quarterly-page-nav__count {
    display: none;
  }

  .quarterly-page__heading {
    display: grid;
    align-items: start;
  }

  .quarterly-page__heading span,
  .quarterly-panel__head span {
    max-width: none;
    text-align: left;
  }

  .quarterly-panel__head {
    display: grid;
  }

  .quarterly-quarter__tabs {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .quarterly-main-layout,
  .quarterly-page--main .quarterly-kpi-grid,
  .quarterly-insights--panel,
  .quarterly-brand-list--inline {
    grid-template-columns: 1fr;
  }

  .quarterly-brand-list--summary {
    grid-template-columns: 1fr;
  }

  .quarterly-quick-grid {
    grid-template-columns: 1fr;
  }
}

</style>
