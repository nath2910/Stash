<template>
  <section class="monthly-dashboard" aria-label="Dashboard mensuel" @wheel="onWheel">
    <div class="monthly-dashboard__inner">
      <header class="monthly-header">
        <div class="monthly-header__copy">
          <p class="monthly-header__kicker">Template</p>
          <h1>Dashboard mensuel</h1>
          <p>
            Lecture {{ selectedMonthLabel }} des ventes, du profit, des achats et du stock au
            {{ formatDate(periodRange.to) }}.
          </p>
        </div>

        <div class="monthly-month">
          <div class="monthly-month__head">
            <span>Mois selectionne</span>
            <small>{{ periodShortLabel }}</small>
          </div>
          <div class="monthly-month__control">
            <button
              type="button"
              aria-label="Mois precedent"
              :disabled="!canGoPreviousMonth"
              @click="changeMonth(-1)"
            >
              <ChevronLeft aria-hidden="true" />
            </button>
            <input
              :value="selectedMonthKey"
              type="month"
              :min="minMonthKey || undefined"
              :max="maxMonthKey || undefined"
              aria-label="Selectionner un mois"
              @input="onMonthInput"
            />
            <button
              type="button"
              aria-label="Mois suivant"
              :disabled="!canGoNextMonth"
              @click="changeMonth(1)"
            >
              <ChevronRight aria-hidden="true" />
            </button>
          </div>
        </div>
      </header>

      <div v-if="loading && !hasLoadedOnce" class="monthly-state monthly-state--loading" role="status">
        <div class="monthly-state__pulse"></div>
        <h2>Chargement du dashboard</h2>
        <p>Preparation des indicateurs mensuels.</p>
      </div>

      <div v-else-if="error" class="monthly-state monthly-state--error" role="alert">
        <h2>Impossible de charger le template</h2>
        <p>{{ error }}</p>
        <button type="button" @click="loadDashboard">Reessayer</button>
      </div>

      <template v-else>
        <div v-if="!hasMonthData" class="monthly-state monthly-state--empty" role="status">
          <h2>Aucune donnee disponible pour {{ selectedMonthLabel }}.</h2>
          <p>
            Les statistiques apparaitront des qu'une vente, un achat ou un article en stock sera
            present sur cette periode.
          </p>
        </div>

        <template v-else>
          <nav class="monthly-page-nav" aria-label="Navigation du dashboard mensuel">
            <button
              type="button"
              class="monthly-page-nav__arrow"
              :disabled="activePage === 0"
              aria-label="Page precedente"
              @click="previousPage"
            >
              <ChevronLeft aria-hidden="true" />
            </button>

            <div class="monthly-page-nav__center">
              <span class="monthly-page-nav__count">{{ activePage + 1 }} / {{ pages.length }}</span>
              <strong>{{ currentPage.label }}</strong>
              <div class="monthly-page-nav__dots" role="tablist" aria-label="Pages">
                <button
                  v-for="(page, index) in pages"
                  :key="page.key"
                  type="button"
                  class="monthly-page-nav__dot"
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
              class="monthly-page-nav__arrow"
              :disabled="activePage === pages.length - 1"
              aria-label="Page suivante"
              @click="nextPage"
            >
              <ChevronRight aria-hidden="true" />
            </button>
          </nav>

          <section
            class="monthly-stage"
            :class="{ 'is-dragging': pointerDrag.active }"
            aria-live="polite"
            @pointerdown="onPointerDown"
            @pointerup="onPointerUp"
            @pointercancel="resetPointerDrag"
            @lostpointercapture="resetPointerDrag"
          >
            <div class="monthly-pages" :style="pageTrackStyle">
              <article class="monthly-page monthly-page--flow" aria-label="Flux du mois">
                <div class="monthly-page__heading">
                  <div>
                    <p>Flux quotidien</p>
                    <h2>CA, profit et mix vendu</h2>
                  </div>
                  <span>{{ daysInPeriod.length }} jours analyses</span>
                </div>

                <div class="monthly-flow-layout">
                  <section class="monthly-panel monthly-panel--daily-performance">
                    <div class="monthly-panel__head">
                      <div>
                        <p>Tendance</p>
                        <h2>Performance du jour</h2>
                      </div>
                      <span>{{ formatMoney(monthTotals.revenue) }} encaisses</span>
                    </div>
                    <VChart class="monthly-chart" :option="dailyPerformanceOption" autoresize />
                  </section>

                  <div class="monthly-flow-side">
                    <section class="monthly-panel monthly-panel--category-profit">
                      <div class="monthly-panel__head">
                        <div>
                          <p>Mix</p>
                          <h2>Categories profitables</h2>
                        </div>
                        <span>Top {{ categoryProfit.length }}</span>
                      </div>
                      <div v-if="categoryProfit.length" class="monthly-chart-wrap">
                        <VChart class="monthly-chart" :option="categoryProfitOption" autoresize />
                      </div>
                      <div v-else class="monthly-mini-empty">Aucune categorie rentable sur ce mois.</div>
                    </section>

                    <section class="monthly-insights monthly-insights--flow" aria-label="Indicateurs rapides">
                      <div v-for="item in insightItems" :key="item.label" class="monthly-insight">
                        <span>{{ item.label }}</span>
                        <strong>{{ item.value }}</strong>
                      </div>
                    </section>
                  </div>
                </div>
              </article>

              <article class="monthly-page monthly-page--main" aria-label="Pilotage mensuel">
                <div class="monthly-main-stack">
                  <section class="monthly-kpi-grid" aria-label="KPI mensuels">
                    <MonthlyKpiCard
                      v-for="card in kpiCards"
                      :key="card.label"
                      :label="card.label"
                      :value="card.value"
                      :detail="card.detail"
                      :tone="card.tone"
                      :icon="card.icon"
                    />
                  </section>
                </div>

                <section class="monthly-panel monthly-panel--main-chart">
                  <div class="monthly-panel__head">
                    <div>
                      <p>Pilotage revendeur</p>
                      <h2>Plan d'action mensuel</h2>
                    </div>
                    <span>{{ selectedMonthLabel }}</span>
                  </div>
                  <div class="monthly-action-overview" aria-label="Resume du plan d'action">
                    <div
                      v-for="item in actionSummaryItems"
                      :key="item.label"
                      class="monthly-action-summary"
                      :class="item.tone ? `is-${item.tone}` : ''"
                    >
                      <span>{{ item.label }}</span>
                      <strong>{{ item.value }}</strong>
                      <small>{{ item.detail }}</small>
                    </div>
                  </div>
                  <div class="monthly-action-grid" aria-label="Plan d'action revendeur">
                    <article
                      v-for="item in resellerSignals"
                      :key="item.title"
                      class="monthly-action-card"
                      :class="item.tone ? `is-${item.tone}` : ''"
                    >
                      <span>{{ item.badge }}</span>
                      <h3>{{ item.title }}</h3>
                      <strong>{{ item.value }}</strong>
                      <p>{{ item.detail }}</p>
                    </article>
                  </div>
                </section>
              </article>

              <article class="monthly-page monthly-page--details" aria-label="Details du mois">
                <div class="monthly-page__heading">
                  <div>
                    <p>Analyse operationnelle</p>
                    <h2>Top ventes et contexte annuel</h2>
                  </div>
                  <span>{{ periodShortLabel }}</span>
                </div>

                <section class="monthly-table-grid" aria-label="Analyses metier">
                  <article class="monthly-panel monthly-table-card monthly-panel--benefit-list">
                    <div class="monthly-panel__head">
                      <div>
                        <p>Meilleures ventes</p>
                        <h2>Benefices du mois</h2>
                      </div>
                      <span>{{ topSales.length }} lignes</span>
                    </div>

                    <div v-if="topSales.length" class="monthly-table-scroll">
                      <table class="monthly-table">
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
                              <span>Top vente mensuelle</span>
                            </td>
                            <td :class="profitClass(sale.benefice)">
                              {{ formatMoney(sale.benefice) }}
                            </td>
                          </tr>
                        </tbody>
                      </table>
                    </div>

                    <div v-else class="monthly-mini-empty">Aucune vente classee sur ce mois.</div>
                  </article>

                  <article class="monthly-panel monthly-table-card monthly-panel--year-context">
                    <div class="monthly-panel__head">
                      <div>
                        <p>Contexte</p>
                        <h2>Position du mois dans l'annee</h2>
                      </div>
                      <span>{{ selectedYear }}</span>
                    </div>
                    <VChart class="monthly-chart monthly-chart--context" :option="yearContextOption" autoresize />
                    <div class="monthly-brand-list" aria-label="Marques vendues">
                      <div v-for="brand in brandPreview" :key="brand.label" class="monthly-brand-row">
                        <span>{{ brand.label }}</span>
                        <strong>{{ formatNumber(brand.nb) }}</strong>
                      </div>
                      <div v-if="!brandPreview.length" class="monthly-brand-row monthly-brand-row--empty">
                        <span>Aucun mix disponible</span>
                        <strong>--</strong>
                      </div>
                    </div>
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
import MonthlyKpiCard from './MonthlyKpiCard.vue'

type MonthlyTemplateState = {
  monthKey?: string
  month?: string | number
  year?: number
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
  initialState?: MonthlyTemplateState
}>()

const emit = defineEmits<{
  (event: 'state-change', state: MonthlyTemplateState): void
}>()

const monthLabels = ['Jan', 'Fev', 'Mar', 'Avr', 'Mai', 'Juin', 'Juil', 'Aout', 'Sep', 'Oct', 'Nov', 'Dec']
const today = new Date()
const currentYear = today.getFullYear()
const currentMonthKey = formatMonthKey(today)

const selectedMonthKey = ref(normalizeInitialMonth(props.initialState))
const minMonthKey = ref('')
const maxMonthKey = ref('')
const minDate = ref('')
const maxDate = ref(formatYmd(today))
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
const summary = ref<StatsSummary>(normalizeSummary({}))
const previousSummary = ref<StatsSummary>(normalizeSummary({}))
const timeseries = ref<TimePoint[]>([])
const brands = ref<BrandRow[]>([])
const topSales = ref<TopSaleRow[]>([])
const categoryProfit = ref<RankRow[]>([])
const annualMonths = ref<DashboardMonth[]>(normalizeAnnualMonths([]))
let requestId = 0
let lastWheelPageChangeAt = 0

const pages = [
  { key: 'flow', label: 'Flux & mix' },
  { key: 'pilotage', label: 'Pilotage mensuel' },
  { key: 'details', label: 'Details & contexte' },
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

const selectedYear = computed(() => Number(selectedMonthKey.value.slice(0, 4)) || currentYear)
const selectedMonthNumber = computed(() => Number(selectedMonthKey.value.slice(5, 7)) || 1)
const selectedMonthLabel = computed(() => formatMonthLong(selectedMonthKey.value))
const periodRange = computed(() => buildMonthRange(selectedMonthKey.value))
const periodShortLabel = computed(() => {
  const start = parseYmd(periodRange.value.from)
  const end = parseYmd(periodRange.value.to)
  if (start.getMonth() === end.getMonth() && start.getDate() === 1 && end.getDate() === monthLastDay(start)) {
    return selectedMonthLabel.value
  }
  return `${start.getDate()}-${end.getDate()} ${monthLabels[end.getMonth()]}`
})
const currentPage = computed(() => pages[activePage.value] ?? pages[1])
const pageTrackStyle = computed(() => ({
  transform: `translate3d(-${activePage.value * 100}%, 0, 0)`,
}))
const canGoPreviousMonth = computed(() => !minMonthKey.value || selectedMonthKey.value > minMonthKey.value)
const canGoNextMonth = computed(() => !maxMonthKey.value || selectedMonthKey.value < maxMonthKey.value)
const selectedAnnualMonth = computed(
  () => annualMonths.value[selectedMonthNumber.value - 1] ?? normalizeAnnualMonths([])[selectedMonthNumber.value - 1],
)
const monthTotals = computed(() => {
  const month = selectedAnnualMonth.value
  const revenue = summary.value.ca || month.revenue
  const profit = summary.value.profit || month.profit
  const itemsSold = summary.value.itemsVendues || month.itemsSold
  const purchaseSpend = month.purchaseSpend
  const itemsBought = month.itemsBought
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
const daysInPeriod = computed(() => listDays(periodRange.value.from, periodRange.value.to))
const dailyRows = computed(() => {
  const byDate = new Map(timeseries.value.map((point) => [point.date, point]))
  return daysInPeriod.value.map((date) => {
    const point = byDate.get(date)
    return {
      date,
      ca: point?.ca ?? 0,
      profit: point?.profit ?? 0,
    }
  })
})
const brandPreview = computed(() => brands.value.slice(0, 5))
const hasMonthData = computed(() => {
  const totals = monthTotals.value
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
const insightItems = computed(() => [
  { label: 'Panier moyen', value: formatMoney(monthTotals.value.averageSalePrice) },
  { label: 'Profit moyen', value: formatMoney(monthTotals.value.averageProfit) },
  { label: 'Depenses achat', value: formatMoney(monthTotals.value.purchaseSpend) },
  { label: 'Cash net', value: formatMoney(monthTotals.value.cashNet) },
])
const kpiCards = computed(() => [
  {
    label: "Chiffre d'affaires",
    value: formatMoney(monthTotals.value.revenue),
    detail: formatDelta(monthTotals.value.revenue, previousTotals.value.revenue),
    tone: 'primary' as const,
    icon: BadgeEuro,
  },
  {
    label: 'Benefice net',
    value: formatMoney(monthTotals.value.profit),
    detail: `${formatMoney(monthTotals.value.averageProfit)} par vente`,
    tone: monthTotals.value.profit >= 0 ? ('profit' as const) : ('warning' as const),
    icon: TrendingUp,
  },
  {
    label: 'Marge du mois',
    value: formatRatio(monthTotals.value.marginRate),
    detail: 'Profit / chiffre d affaires',
    tone: monthTotals.value.marginRate >= 0 ? ('profit' as const) : ('warning' as const),
    icon: CirclePercent,
  },
  {
    label: 'Ventes du mois',
    value: formatNumber(monthTotals.value.itemsSold),
    detail: `${formatMoney(monthTotals.value.averageSalePrice)} panier moyen`,
    tone: 'primary' as const,
    icon: ShoppingBag,
  },
  {
    label: 'Achats du mois',
    value: formatMoney(monthTotals.value.purchaseSpend),
    detail: `${formatNumber(monthTotals.value.itemsBought)} achats de stock`,
    tone: 'primary' as const,
    icon: Wallet,
  },
  {
    label: 'Stock fin de mois',
    value: `${formatNumber(monthTotals.value.remainingStockCount)} articles`,
    detail: `${formatMoney(monthTotals.value.remainingStockValue)} au ${formatDate(periodRange.value.to)}`,
    tone: monthTotals.value.remainingStockCount > 0 ? ('warning' as const) : ('neutral' as const),
    icon: Boxes,
  },
])
const bestSale = computed(() => topSales.value[0] ?? null)
const bestCategory = computed(() => categoryProfit.value[0] ?? null)
const activeDayCount = computed(() => dailyRows.value.filter((row) => row.ca !== 0 || row.profit !== 0).length)
const stockValueDelta = computed(() => deltaPct(monthTotals.value.remainingStockValue, previousTotals.value.stockValue))
const profitDelta = computed(() => deltaPct(monthTotals.value.profit, previousTotals.value.profit))
const actionSummaryItems = computed(() => [
  {
    label: 'Profit vs M-1',
    value: formatDelta(monthTotals.value.profit, previousTotals.value.profit),
    detail: 'Comparaison au mois precedent.',
    tone: profitDelta.value == null ? 'neutral' : profitDelta.value >= 0 ? 'positive' : 'warning',
  },
  {
    label: 'Jours actifs',
    value: `${formatNumber(activeDayCount.value)} / ${formatNumber(daysInPeriod.value.length)}`,
    detail: 'Jours avec CA ou profit enregistre.',
    tone: activeDayCount.value > 0 ? 'positive' : 'neutral',
  },
  {
    label: 'Priorite mix',
    value: bestCategory.value?.label || 'Aucune',
    detail: bestCategory.value?.label
      ? `${formatMoney(bestCategory.value.value)} de profit categorie.`
      : 'Categorie non disponible sur ce mois.',
    tone: bestCategory.value?.value && bestCategory.value.value >= 0 ? 'positive' : 'neutral',
  },
])
const resellerSignals = computed(() => {
  const sale = bestSale.value
  const category = bestCategory.value
  const stockDelta = stockValueDelta.value
  return [
    {
      badge: 'Cash',
      title: monthTotals.value.cashNet >= 0 ? 'Cash positif' : 'Cash sous pression',
      value: formatMoney(monthTotals.value.cashNet),
      detail:
        monthTotals.value.cashNet >= 0
          ? 'Les ventes couvrent les achats du mois.'
          : 'Les achats depassent les ventes: surveille le stock lent.',
      tone: monthTotals.value.cashNet >= 0 ? 'positive' : 'warning',
    },
    {
      badge: 'Top',
      title: sale?.nomItem ? 'Meilleure vente' : 'Vente a creer',
      value: sale?.nomItem || 'Aucune',
      detail: sale?.nomItem
        ? `${formatMoney(sale.benefice)} de benefice sur la meilleure ligne.`
        : 'Aucune vente classee sur cette periode.',
      tone: sale?.benefice && sale.benefice >= 0 ? 'positive' : 'neutral',
    },
    {
      badge: 'Mix',
      title: category?.label ? 'Categorie forte' : 'Mix incomplet',
      value: category?.label || 'Aucune',
      detail: category?.label
        ? `${formatMoney(category.value)} de profit categorie.`
        : 'Ajoute categories/marques pour affiner la lecture.',
      tone: category?.value && category.value >= 0 ? 'positive' : 'neutral',
    },
    {
      badge: 'Stock',
      title: 'Valeur immobilisee',
      value: formatMoney(monthTotals.value.remainingStockValue),
      detail: stockDelta == null ? 'Comparaison indisponible.' : `${formatSignedRatio(stockDelta)} vs mois precedent.`,
      tone: stockDelta != null && stockDelta > 0 ? 'warning' : 'neutral',
    },
    {
      badge: 'Cadence',
      title: 'Jours actifs',
      value: `${formatNumber(activeDayCount.value)} j`,
      detail: activeDayCount.value > 0 ? 'Jours avec CA ou profit enregistre.' : 'Aucun mouvement quotidien detecte.',
      tone: activeDayCount.value > 0 ? 'positive' : 'neutral',
    },
    {
      badge: 'ROI',
      title: 'Rentabilite',
      value: formatRatio(monthRoi.value),
      detail: 'Profit / cout des achats vendus estime depuis CA et profit.',
      tone: monthRoi.value >= 0 ? 'profit' : 'warning',
    },
  ]
})
const monthRoi = computed(() => {
  const soldCost = monthTotals.value.revenue - monthTotals.value.profit
  return soldCost > 0 ? monthTotals.value.profit / soldCost : 0
})
const dailyPerformanceOption = computed(() => ({
  color: ['#4f46e5', '#059669'],
  grid: { left: 8, right: 12, top: 38, bottom: 12, containLabel: true },
  tooltip: {
    trigger: 'axis',
    confine: true,
    axisPointer: { type: 'cross', label: { backgroundColor: '#111827' } },
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
    data: dailyRows.value.map((row) => formatDayShort(row.date)),
    axisTick: { show: false },
    axisLine: { lineStyle: { color: '#cbd5e1' } },
    axisLabel: { color: '#64748b', interval: Math.max(0, Math.floor(dailyRows.value.length / 9)) },
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
      barMaxWidth: 24,
      data: dailyRows.value.map((row) => row.ca),
      itemStyle: { borderRadius: [6, 6, 0, 0], color: 'rgba(79, 70, 229, 0.66)' },
      emphasis: { itemStyle: { color: 'rgba(79, 70, 229, 0.86)' } },
    },
    {
      name: 'Profit',
      type: 'line',
      smooth: true,
      symbolSize: 6,
      data: dailyRows.value.map((row) => row.profit),
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
          color: (params: { value: number }) => (Number(params.value ?? 0) >= 0 ? '#10b981' : '#f97316'),
        },
      },
    ],
  }
})
const yearContextOption = computed(() => ({
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
    data: monthLabels,
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
          params.dataIndex === selectedMonthNumber.value - 1 ? '#4f46e5' : 'rgba(79, 70, 229, 0.2)',
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
          params.dataIndex === selectedMonthNumber.value - 1 ? '#d97706' : 'rgba(217, 119, 6, 0.2)',
      },
    },
  ],
}))

watch(
  () => props.initialState,
  (state) => {
    const next = clampMonthKey(normalizeInitialMonth(state))
    if (selectedMonthKey.value !== next) selectedMonthKey.value = next
  },
  { deep: true },
)

watch(
  selectedMonthKey,
  () => {
    emit('state-change', {
      monthKey: selectedMonthKey.value,
      month: selectedMonthNumber.value,
      year: selectedYear.value,
    })
    loadDashboard()
  },
  { immediate: true },
)

function normalizeInitialMonth(state?: MonthlyTemplateState) {
  const rawMonthKey = state?.monthKey ?? state?.month
  const normalized = normalizeMonthKey(rawMonthKey, '')
  if (normalized) return normalized
  const year = Math.trunc(Number(state?.year))
  const month = Math.trunc(Number(state?.month))
  if (Number.isFinite(year) && Number.isFinite(month) && month >= 1 && month <= 12) {
    return `${year}-${pad(month)}`
  }
  return currentMonthKey
}

function normalizeMonthKey(value: unknown, fallback = currentMonthKey) {
  const text = String(value ?? '').trim()
  if (/^\d{4}-\d{2}$/.test(text)) {
    const year = Number(text.slice(0, 4))
    const month = Number(text.slice(5, 7))
    if (Number.isFinite(year) && month >= 1 && month <= 12) return `${year}-${pad(month)}`
  }
  if (/^\d{4}-\d{2}-\d{2}$/.test(text)) {
    return normalizeMonthKey(text.slice(0, 7), fallback)
  }
  return fallback
}

function clampMonthKey(value: string) {
  let next = normalizeMonthKey(value)
  if (minMonthKey.value && next < minMonthKey.value) next = minMonthKey.value
  if (maxMonthKey.value && next > maxMonthKey.value) next = maxMonthKey.value
  return next
}

function buildMonthRange(monthKey: string) {
  const first = parseMonthKey(monthKey)
  const last = new Date(first.getFullYear(), first.getMonth() + 1, 0)
  let from = formatYmd(first)
  let to = formatYmd(last)
  if (minDate.value && monthKeyFromYmd(minDate.value) === monthKey && from < minDate.value) {
    from = minDate.value
  }
  if (maxDate.value && monthKeyFromYmd(maxDate.value) === monthKey && to > maxDate.value) {
    to = maxDate.value
  }
  if (from > to) from = to
  return { from, to }
}

function previousMonthRange() {
  const currentStart = parseYmd(periodRange.value.from)
  const currentEnd = parseYmd(periodRange.value.to)
  const previousStart = new Date(currentStart.getFullYear(), currentStart.getMonth() - 1, 1)
  const previousEndDay = Math.min(currentEnd.getDate(), monthLastDay(previousStart))
  const previousEnd = new Date(previousStart.getFullYear(), previousStart.getMonth(), previousEndDay)
  return {
    from: formatYmd(previousStart),
    to: formatYmd(previousEnd),
  }
}

function formatMonthKey(date: Date) {
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}`
}

function monthKeyFromYmd(value: string) {
  return /^\d{4}-\d{2}-\d{2}$/.test(value) ? value.slice(0, 7) : ''
}

function parseMonthKey(monthKey: string) {
  const [year, month] = normalizeMonthKey(monthKey).split('-').map(Number)
  return new Date(year, month - 1, 1)
}

function parseYmd(value: string) {
  const [year, month, day] = String(value).split('-').map(Number)
  return new Date(year, (month || 1) - 1, day || 1)
}

function monthLastDay(date: Date) {
  return new Date(date.getFullYear(), date.getMonth() + 1, 0).getDate()
}

function formatYmd(date: Date) {
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}

function pad(value: number) {
  return String(value).padStart(2, '0')
}

function listDays(from: string, to: string) {
  const days: string[] = []
  const cursor = parseYmd(from)
  const end = parseYmd(to)
  while (cursor <= end) {
    days.push(formatYmd(cursor))
    cursor.setDate(cursor.getDate() + 1)
  }
  return days
}

function normalizeSummary(raw: unknown): StatsSummary {
  return {
    ca: toNumber(raw?.ca),
    profit: toNumber(raw?.profit),
    profitMargin: toNumber(raw?.profitMargin),
    itemsVendues: toNumber(raw?.itemsVendues),
    itemsEnStock: toNumber(raw?.itemsEnStock),
    valeurStock: toNumber(raw?.valeurStock),
  }
}

function normalizeTimeseries(raw: unknown): TimePoint[] {
  return Array.isArray(raw)
    ? raw
        .map((item) => ({
          date: String(item?.date ?? ''),
          ca: toNumber(item?.ca),
          profit: toNumber(item?.profit),
        }))
        .filter((item) => item.date)
    : []
}

function normalizeBrands(raw: unknown): BrandRow[] {
  return Array.isArray(raw)
    ? raw
        .map((item) => ({
          label: String(item?.label ?? '').trim(),
          nb: toNumber(item?.nb),
        }))
        .filter((item) => item.label)
    : []
}

function normalizeTopSales(raw: unknown): TopSaleRow[] {
  return Array.isArray(raw)
    ? raw
        .map((item) => ({
          nomItem: String(item?.nomItem ?? '').trim(),
          benefice: toNumber(item?.benefice),
        }))
        .filter((item) => item.nomItem)
    : []
}

function normalizeRank(raw: unknown): RankRow[] {
  return Array.isArray(raw)
    ? raw
        .map((item) => ({
          label: String(item?.label ?? '').trim(),
          value: toNumber(item?.value ?? item?.nb),
        }))
        .filter((item) => item.label)
    : []
}

function normalizeAnnualMonths(raw: unknown): DashboardMonth[] {
  const rows = Array.from({ length: 12 }, (_, index) => ({
    month: index + 1,
    revenue: 0,
    profit: 0,
    purchaseSpend: 0,
    itemsSold: 0,
    itemsBought: 0,
  }))
  if (!Array.isArray(raw)) return rows
  for (const item of raw) {
    const month = Math.trunc(toNumber(item?.month))
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

function formatDelta(current: number, previous: number) {
  const delta = deltaPct(current, previous)
  if (delta == null) return 'Comparaison indisponible'
  return `${formatSignedRatio(delta)} vs mois precedent`
}

function deltaPct(current: number, previous: number) {
  const curr = toNumber(current)
  const prev = toNumber(previous)
  if (prev === 0) return null
  return ((curr - prev) / Math.abs(prev)) * 100
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

function formatMonthLong(monthKey: string) {
  const date = parseMonthKey(monthKey)
  const label = date.toLocaleDateString('fr-FR', { month: 'long', year: 'numeric' })
  return label.charAt(0).toUpperCase() + label.slice(1)
}

function formatDayShort(value: string) {
  const date = parseYmd(value)
  return Number.isNaN(date.getTime()) ? value : String(date.getDate()).padStart(2, '0')
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

function changeMonth(delta: number) {
  const date = parseMonthKey(selectedMonthKey.value)
  date.setMonth(date.getMonth() + delta)
  selectedMonthKey.value = clampMonthKey(formatMonthKey(date))
}

function onMonthInput(event: Event) {
  const input = event.target as HTMLInputElement | null
  const value = normalizeMonthKey(input?.value, selectedMonthKey.value)
  selectedMonthKey.value = clampMonthKey(value)
  if (input && input.value !== selectedMonthKey.value) input.value = selectedMonthKey.value
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
  const delta = Math.abs(event.deltaX) >= Math.abs(event.deltaY) ? event.deltaX : event.shiftKey ? event.deltaY : 0
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
  const range = periodRange.value
  const previous = previousMonthRange()
  try {
    const [summaryRes, previousSummaryRes, timeseriesRes, brandsRes, topSalesRes, categoryRes, annualRes] =
      await Promise.all([
        StatsServices.summary({ from: range.from, to: range.to, asOf: range.to }),
        StatsServices.summary({ from: previous.from, to: previous.to, asOf: previous.to }),
        StatsServices.timeseries(range.from, range.to, 'day'),
        StatsServices.brands(range.from, range.to),
        StatsServices.topSales(range.from, range.to, 8),
        StatsServices.rank('topCategoriesProfit', range.from, range.to, 6),
        StatsServices.annualDashboard(selectedYear.value),
      ])

    if (id !== requestId) return
    summary.value = normalizeSummary(summaryRes.data)
    previousSummary.value = normalizeSummary(previousSummaryRes.data)
    timeseries.value = normalizeTimeseries(timeseriesRes.data)
    brands.value = normalizeBrands(brandsRes.data)
    topSales.value = normalizeTopSales(topSalesRes.data)
    categoryProfit.value = normalizeRank(categoryRes.data)
    annualMonths.value = normalizeAnnualMonths(annualRes.data?.monthly)
    hasLoadedOnce.value = true
  } catch (err: unknown) {
    if (id !== requestId) return
    error.value = err?.response?.data?.message ?? err?.message ?? 'Erreur inconnue'
  } finally {
    if (id === requestId) loading.value = false
  }
}

async function loadMonthBounds() {
  try {
    const { data } = await StatsServices.dateBounds()
    minDate.value = typeof data?.minDate === 'string' ? data.minDate : ''
    maxDate.value = typeof data?.maxDate === 'string' ? data.maxDate : formatYmd(today)
    minMonthKey.value = minDate.value ? monthKeyFromYmd(minDate.value) : ''
    maxMonthKey.value = maxDate.value ? monthKeyFromYmd(maxDate.value) : currentMonthKey
    const clamped = clampMonthKey(selectedMonthKey.value)
    if (selectedMonthKey.value !== clamped) selectedMonthKey.value = clamped
  } catch {
    minDate.value = ''
    maxDate.value = formatYmd(today)
    minMonthKey.value = ''
    maxMonthKey.value = currentMonthKey
    selectedMonthKey.value = clampMonthKey(selectedMonthKey.value)
  }
}

onMounted(async () => {
  window.addEventListener('keydown', onKeyDown)
  await loadMonthBounds()
})

onBeforeUnmount(() => {
  window.removeEventListener('keydown', onKeyDown)
})
</script>

<style scoped>
.monthly-dashboard {
  --monthly-template-gap: clamp(10px, 1.35vh, 16px);
  --monthly-bg: #f7f4ee;
  --monthly-muted-bg: #fbfaf6;
  width: 100%;
  height: 100%;
  min-width: 0;
  min-height: 0;
  overflow: auto;
  overscroll-behavior: contain;
  overscroll-behavior-x: none;
  scrollbar-width: thin;
  scrollbar-color: rgba(100, 116, 139, 0.34) rgba(234, 229, 219, 0.9);
  background: var(--monthly-bg);
}

.monthly-dashboard::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}

.monthly-dashboard::-webkit-scrollbar-track {
  background: rgba(234, 229, 219, 0.9);
}

.monthly-dashboard::-webkit-scrollbar-thumb {
  border-radius: 999px;
  background: rgba(100, 116, 139, 0.34);
}

.monthly-dashboard__inner {
  width: min(1640px, 100%);
  min-height: 100%;
  min-width: 0;
  margin: 0 auto;
  padding: clamp(14px, 1.8vw, 24px) clamp(14px, 2.2vw, 28px) clamp(16px, 2vw, 24px)
    calc(96px + clamp(14px, 2.2vw, 28px));
  display: grid;
  grid-template-rows: auto auto auto;
  gap: var(--monthly-template-gap);
  align-content: start;
}

.monthly-header {
  min-width: 0;
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 18px;
}

.monthly-header__copy {
  min-width: 0;
}

.monthly-header__kicker,
.monthly-page__heading p,
.monthly-panel__head p {
  color: #5b5ce2;
  font-weight: 820;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

.monthly-header__kicker {
  margin: 0 0 7px;
  font-size: 0.72rem;
  letter-spacing: 0.16em;
}

.monthly-header h1 {
  margin: 0;
  color: #111827;
  font-size: clamp(1.62rem, 2.8vw, 2.45rem);
  line-height: 1.02;
  font-weight: 860;
}

.monthly-header p {
  margin: 7px 0 0;
  max-width: 72ch;
  color: #64748b;
  font-size: 0.96rem;
  line-height: 1.45;
}

.monthly-month {
  flex: 0 0 auto;
  width: min(340px, 100%);
  border: 1px solid rgba(99, 102, 241, 0.2);
  border-radius: 12px;
  background: #ffffff;
  box-shadow:
    0 14px 34px rgba(31, 41, 55, 0.1),
    inset 0 1px 0 rgba(255, 255, 255, 0.8);
  padding: 10px;
  display: grid;
  gap: 8px;
}

.monthly-month__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.monthly-month__head span {
  color: #64748b;
  font-size: 0.68rem;
  font-weight: 780;
  letter-spacing: 0.11em;
  text-transform: uppercase;
}

.monthly-month__head small {
  color: #5b5ce2;
  font-size: 0.68rem;
  font-weight: 760;
}

.monthly-month__control {
  display: grid;
  grid-template-columns: 34px minmax(0, 1fr) 34px;
  align-items: center;
  gap: 6px;
}

.monthly-month__control button {
  width: 34px;
  height: 36px;
  border: 1px solid rgba(99, 102, 241, 0.22);
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.8);
  color: #4f46e5;
  display: inline-grid;
  place-items: center;
}

.monthly-month__control button:hover:not(:disabled) {
  border-color: rgba(79, 70, 229, 0.42);
  background: var(--monthly-muted-bg);
}

.monthly-month__control button:disabled {
  opacity: 0.38;
  pointer-events: none;
}

.monthly-month__control svg {
  width: 16px;
  height: 16px;
}

.monthly-month__control input {
  width: 100%;
  min-width: 0;
  height: 38px;
  border: 1px solid rgba(99, 102, 241, 0.26);
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.96);
  color: #111827;
  font-size: 0.98rem;
  font-weight: 820;
  text-align: center;
  padding: 0 8px;
  outline: none;
}

.monthly-month__control input:focus {
  border-color: rgba(91, 92, 226, 0.58);
  box-shadow: 0 0 0 3px rgba(91, 92, 226, 0.12);
}

.monthly-page-nav {
  min-width: 0;
  border: 1px solid rgba(148, 163, 184, 0.24);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.82);
  box-shadow: 0 10px 24px rgba(31, 41, 55, 0.06);
  padding: 7px 9px;
  display: grid;
  grid-template-columns: 40px minmax(0, 1fr) 40px;
  align-items: center;
  gap: 10px;
}

.monthly-page-nav__arrow {
  width: 36px;
  height: 36px;
  border: 1px solid rgba(99, 102, 241, 0.22);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.78);
  color: #4338ca;
  display: grid;
  place-items: center;
}

.monthly-page-nav__arrow svg {
  width: 18px;
  height: 18px;
}

.monthly-page-nav__arrow:disabled {
  opacity: 0.36;
  pointer-events: none;
}

.monthly-page-nav__center {
  min-width: 0;
  display: grid;
  grid-template-columns: auto minmax(0, auto) auto;
  justify-content: center;
  align-items: center;
  gap: 10px;
}

.monthly-page-nav__count {
  color: #64748b;
  font-size: 0.72rem;
  font-weight: 780;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.monthly-page-nav__center strong {
  color: #111827;
  font-size: 0.92rem;
  font-weight: 800;
  white-space: nowrap;
}

.monthly-page-nav__dots {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.monthly-page-nav__dot {
  width: 7px;
  height: 7px;
  border: 0;
  border-radius: 999px;
  background: #cbd5e1;
  padding: 0;
}

.monthly-page-nav__dot.is-active {
  width: 22px;
  background: #5b5ce2;
}

.monthly-stage {
  min-width: 0;
  min-height: 0;
  height: auto;
  overflow: hidden;
  overscroll-behavior: contain;
  overscroll-behavior-x: none;
  border-radius: 10px;
  cursor: grab;
  touch-action: pan-y;
  user-select: none;
}

.monthly-stage.is-dragging {
  cursor: grabbing;
}

.monthly-stage.is-dragging .monthly-pages {
  transition-duration: 180ms;
}

.monthly-pages {
  height: auto;
  min-height: 0;
  display: flex;
  transition:
    transform 320ms cubic-bezier(0.22, 1, 0.36, 1),
    opacity 180ms ease;
  will-change: transform;
}

.monthly-page {
  flex: 0 0 100%;
  width: 100%;
  min-width: 0;
  min-height: 0;
  height: auto;
  padding: 2px;
  display: grid;
  gap: 12px;
  overflow: visible;
  align-content: start;
}

.monthly-page--flow {
  grid-template-rows: auto minmax(0, 1fr);
}

.monthly-page--main {
  grid-template-rows: auto auto;
  align-content: start;
}

.monthly-page--details {
  grid-template-rows: auto minmax(0, 1fr);
  align-content: stretch;
}

.monthly-page__heading {
  min-width: 0;
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 12px;
}

.monthly-page__heading p {
  margin: 0 0 4px;
  font-size: 0.68rem;
}

.monthly-page__heading h2 {
  margin: 0;
  color: #111827;
  font-size: clamp(1.08rem, 1.5vw, 1.28rem);
  line-height: 1.15;
  font-weight: 820;
}

.monthly-page__heading span {
  max-width: 58ch;
  color: #64748b;
  font-size: 0.86rem;
  line-height: 1.35;
  text-align: right;
}

.monthly-main-stack {
  min-width: 0;
  display: grid;
  gap: 10px;
}

.monthly-kpi-grid {
  min-width: 0;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: clamp(9px, 1vw, 12px);
}

.monthly-page--main :deep(.monthly-kpi) {
  padding: clamp(11px, 1.2vw, 14px);
  gap: 7px;
}

.monthly-page--main :deep(.monthly-kpi__value) {
  font-size: clamp(1.22rem, 1.7vw, 1.82rem);
}

.monthly-page--main :deep(.monthly-kpi__detail) {
  font-size: 0.78rem;
}

.monthly-page-grid {
  min-width: 0;
  min-height: 0;
  height: auto;
  display: grid;
  gap: 14px;
  align-items: start;
}

.monthly-page-grid--two {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.monthly-page-grid--two .monthly-panel {
  min-height: clamp(300px, 34vh, 430px);
}

.monthly-flow-layout {
  min-width: 0;
  min-height: clamp(520px, 62vh, 760px);
  display: grid;
  grid-template-columns: minmax(0, 1.45fr) minmax(330px, 0.75fr);
  gap: 14px;
  align-items: stretch;
}

.monthly-flow-side {
  min-width: 0;
  min-height: 0;
  display: grid;
  grid-template-rows: minmax(280px, 1fr) auto;
  gap: 14px;
}

.monthly-panel--daily-performance {
  min-height: clamp(480px, 58vh, 720px);
}

.monthly-panel--daily-performance .monthly-chart {
  min-height: 360px;
}

.monthly-panel--category-profit {
  min-height: 280px;
}

.monthly-panel {
  min-width: 0;
  min-height: 0;
  height: auto;
  border: 1px solid rgba(148, 163, 184, 0.28);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.9);
  box-shadow: 0 14px 34px rgba(31, 41, 55, 0.08);
  padding: clamp(14px, 1.8vw, 18px);
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  gap: 12px;
}

.monthly-panel__head {
  min-width: 0;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.monthly-panel__head div {
  min-width: 0;
}

.monthly-panel__head p {
  margin: 0 0 4px;
  font-size: 0.7rem;
}

.monthly-panel__head h2 {
  margin: 0;
  color: #111827;
  font-size: clamp(1rem, 1.4vw, 1.24rem);
  line-height: 1.15;
  font-weight: 790;
}

.monthly-panel__head span {
  flex: 0 0 auto;
  max-width: 45%;
  border-radius: 999px;
  background: var(--monthly-muted-bg);
  color: #4338ca;
  padding: 6px 10px;
  font-size: 0.74rem;
  font-weight: 760;
  overflow-wrap: anywhere;
  text-align: right;
}

.monthly-chart {
  width: 100%;
  height: 100%;
  min-width: 0;
  min-height: 0;
}

.monthly-chart-wrap {
  min-width: 0;
  min-height: 0;
  height: 100%;
}

.monthly-chart--context {
  min-height: 230px;
}

.monthly-panel--main-chart {
  height: auto;
  min-height: 0;
  padding: clamp(12px, 1.2vw, 16px);
  gap: 10px;
  grid-template-rows: auto auto minmax(0, 1fr);
  background: #ffffff;
}

.monthly-panel--main-chart .monthly-panel__head {
  align-items: center;
  gap: 8px;
}

.monthly-panel--main-chart .monthly-panel__head p {
  display: none;
}

.monthly-panel--main-chart .monthly-panel__head h2 {
  font-size: 0.92rem;
  line-height: 1.1;
}

.monthly-panel--main-chart .monthly-panel__head span {
  padding: 4px 8px;
  font-size: 0.68rem;
}

.monthly-action-grid {
  min-width: 0;
  min-height: 0;
  height: auto;
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(166px, 1fr));
  grid-auto-rows: minmax(116px, auto);
  align-items: start;
  gap: 9px;
}

.monthly-action-overview {
  min-width: 0;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 9px;
}

.monthly-action-summary {
  min-width: 0;
  border: 1px solid rgba(148, 163, 184, 0.22);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.72);
  padding: 10px 12px;
  display: grid;
  gap: 4px;
}

.monthly-action-summary span {
  color: #64748b;
  font-size: 0.64rem;
  font-weight: 820;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.monthly-action-summary strong {
  min-width: 0;
  color: #111827;
  font-size: clamp(0.98rem, 1.15vw, 1.2rem);
  line-height: 1.1;
  font-weight: 820;
  overflow-wrap: anywhere;
}

.monthly-action-summary small {
  color: #64748b;
  font-size: 0.68rem;
  line-height: 1.25;
  font-weight: 680;
}

.monthly-action-summary.is-positive strong {
  color: #047857;
}

.monthly-action-summary.is-warning strong {
  color: #b45309;
}

.monthly-action-card {
  min-width: 0;
  min-height: 116px;
  border: 1px solid rgba(148, 163, 184, 0.22);
  border-radius: 8px;
  background: var(--monthly-muted-bg);
  padding: 10px;
  display: grid;
  grid-template-rows: auto auto auto auto;
  align-content: start;
  gap: 6px;
}

.monthly-action-card span {
  width: fit-content;
  min-width: 0;
  border-radius: 999px;
  background: rgba(99, 102, 241, 0.1);
  color: #4f46e5;
  padding: 3px 7px;
  font-size: 0.58rem;
  font-weight: 820;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.monthly-action-card h3 {
  min-width: 0;
  margin: 0;
  color: #64748b;
  font-size: 0.72rem;
  line-height: 1.15;
  font-weight: 820;
}

.monthly-action-card strong {
  min-width: 0;
  color: #111827;
  font-size: clamp(1rem, 1.18vw, 1.18rem);
  line-height: 1.05;
  font-weight: 820;
  overflow-wrap: anywhere;
}

.monthly-action-card p {
  min-width: 0;
  margin: 0;
  color: #64748b;
  font-size: 0.68rem;
  line-height: 1.25;
  font-weight: 680;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.monthly-action-card.is-positive strong,
.monthly-action-card.is-profit strong {
  color: #047857;
}

.monthly-action-card.is-warning strong {
  color: #b45309;
}

.monthly-action-card.is-warning span {
  background: rgba(245, 158, 11, 0.12);
  color: #b45309;
}

.monthly-action-card.is-positive span,
.monthly-action-card.is-profit span {
  background: rgba(16, 185, 129, 0.12);
  color: #047857;
}

.monthly-insights {
  border: 1px solid rgba(148, 163, 184, 0.26);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.68);
  padding: 10px;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 8px;
}

.monthly-insight {
  min-width: 0;
  border-radius: 7px;
  background: var(--monthly-muted-bg);
  padding: 10px 12px;
  display: grid;
  gap: 4px;
}

.monthly-insight span {
  color: #64748b;
  font-size: 0.72rem;
  font-weight: 720;
  text-transform: uppercase;
  letter-spacing: 0.06em;
}

.monthly-insight strong {
  color: #1f2937;
  font-size: 1rem;
  overflow-wrap: anywhere;
}

.monthly-table-grid {
  min-width: 0;
  min-height: 0;
  height: auto;
  display: grid;
  grid-template-columns: minmax(0, 0.96fr) minmax(0, 1.04fr);
  gap: 14px;
  align-items: stretch;
}

.monthly-table-card {
  min-height: 0;
  align-content: start;
  overflow: hidden;
}

.monthly-page--details .monthly-table-card {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
}

.monthly-panel--benefit-list {
  grid-template-rows: auto minmax(0, 1fr);
}

.monthly-panel--year-context {
  grid-template-rows: auto minmax(250px, 1fr) auto;
}

.monthly-page--details .monthly-table-card {
  padding: clamp(12px, 1.4vw, 16px);
  gap: 9px;
}

.monthly-page--details .monthly-panel__head {
  align-items: center;
}

.monthly-page--details .monthly-panel__head p {
  margin-bottom: 3px;
  font-size: 0.66rem;
}

.monthly-page--details .monthly-panel__head h2 {
  font-size: clamp(0.98rem, 1.25vw, 1.16rem);
}

.monthly-page--details .monthly-panel__head span {
  padding: 5px 9px;
  font-size: 0.68rem;
}

.monthly-table-scroll {
  min-width: 0;
  min-height: 0;
  height: auto;
  max-height: none;
  overflow-y: visible;
  overflow-x: hidden;
  overscroll-behavior: contain;
  scrollbar-width: thin;
  scrollbar-color: rgba(91, 92, 226, 0.32) rgba(226, 232, 240, 0.58);
}

.monthly-table-scroll::-webkit-scrollbar {
  width: 7px;
}

.monthly-table-scroll::-webkit-scrollbar-track {
  background: rgba(226, 232, 240, 0.58);
}

.monthly-table-scroll::-webkit-scrollbar-thumb {
  border-radius: 999px;
  background: rgba(91, 92, 226, 0.34);
}

.monthly-table {
  width: 100%;
  min-width: 0;
  border-collapse: collapse;
  table-layout: fixed;
}

.monthly-table th {
  position: sticky;
  top: 0;
  z-index: 1;
  color: #64748b;
  background: rgba(255, 255, 255, 0.96);
  font-size: 0.7rem;
  font-weight: 780;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  text-align: right;
  padding: 8px 7px;
  border-bottom: 1px solid rgba(148, 163, 184, 0.24);
  white-space: nowrap;
}

.monthly-table th:first-child,
.monthly-table td:first-child,
.monthly-table th:nth-child(2),
.monthly-table td:nth-child(2) {
  text-align: left;
}

.monthly-table td {
  color: #334155;
  font-size: 0.78rem;
  text-align: right;
  padding: 9px 7px;
  border-bottom: 1px solid rgba(148, 163, 184, 0.16);
  vertical-align: top;
  white-space: nowrap;
}

.monthly-table td:nth-child(2) {
  white-space: normal;
}

.monthly-table strong {
  display: block;
  color: #111827;
  font-weight: 760;
  line-height: 1.25;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.monthly-table td span {
  display: block;
  margin-top: 3px;
  color: #64748b;
  font-size: 0.74rem;
}

.monthly-table .is-positive {
  color: #047857;
  font-weight: 790;
}

.monthly-table .is-negative {
  color: #dc2626;
  font-weight: 790;
}

.monthly-brand-list {
  min-width: 0;
  display: grid;
  gap: 7px;
  align-content: start;
}

.monthly-brand-row {
  min-width: 0;
  border: 1px solid rgba(148, 163, 184, 0.2);
  border-radius: 7px;
  background: var(--monthly-muted-bg);
  padding: 8px 10px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.monthly-brand-row span {
  min-width: 0;
  color: #334155;
  font-size: 0.78rem;
  font-weight: 720;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.monthly-brand-row strong {
  flex: 0 0 auto;
  color: #4338ca;
  font-size: 0.78rem;
  font-weight: 820;
}

.monthly-brand-row--empty span {
  color: #64748b;
}

.monthly-state {
  min-height: 360px;
  border: 1px dashed rgba(148, 163, 184, 0.42);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.82);
  padding: clamp(22px, 4vw, 36px);
  display: grid;
  place-items: center;
  align-content: center;
  text-align: center;
  gap: 8px;
}

.monthly-state h2 {
  margin: 0;
  color: #111827;
  font-size: clamp(1.18rem, 2vw, 1.6rem);
  line-height: 1.15;
  font-weight: 800;
}

.monthly-state p {
  margin: 0;
  max-width: 56ch;
  color: #64748b;
  line-height: 1.5;
}

.monthly-state button {
  margin-top: 10px;
  min-height: 40px;
  border: 1px solid rgba(99, 102, 241, 0.32);
  border-radius: 8px;
  background: #4f46e5;
  color: #fff;
  padding: 0 14px;
  font-weight: 760;
}

.monthly-state__pulse {
  width: 46px;
  height: 46px;
  border-radius: 999px;
  border: 4px solid rgba(99, 102, 241, 0.16);
  border-top-color: #5b5ce2;
  animation: monthly-spin 850ms linear infinite;
}

.monthly-mini-empty {
  min-height: clamp(130px, 18vh, 220px);
  height: auto;
  border: 1px dashed rgba(148, 163, 184, 0.34);
  border-radius: 8px;
  background: var(--monthly-muted-bg);
  color: #64748b;
  display: grid;
  place-items: center;
  padding: 18px;
  text-align: center;
  line-height: 1.45;
}

@media (min-width: 961px) {
  .monthly-dashboard {
    overflow: hidden;
  }

  .monthly-dashboard__inner {
    height: 100%;
    min-height: 0;
    grid-template-rows: auto auto minmax(0, 1fr);
    align-content: stretch;
  }

  .monthly-stage,
  .monthly-pages {
    height: 100%;
    min-height: 0;
  }

  .monthly-page {
    height: 100%;
    min-height: 0;
    overflow-y: auto;
    overflow-x: hidden;
    overscroll-behavior: contain;
    scrollbar-width: thin;
    scrollbar-color: rgba(91, 92, 226, 0.28) transparent;
  }

  .monthly-page::-webkit-scrollbar {
    width: 6px;
  }

  .monthly-page::-webkit-scrollbar-thumb {
    border-radius: 999px;
    background: rgba(91, 92, 226, 0.28);
  }

  .monthly-page--flow {
    grid-template-rows: auto minmax(0, 1fr);
  }

  .monthly-page--main {
    grid-template-rows: auto auto;
    align-content: start;
  }

  .monthly-page--details {
    grid-template-rows: auto minmax(0, 1fr);
  }

  .monthly-flow-layout,
  .monthly-table-grid {
    height: 100%;
    min-height: 0;
  }

  .monthly-panel--daily-performance,
  .monthly-panel--category-profit {
    min-height: 0;
  }

  .monthly-page-grid--two {
    align-items: stretch;
  }

  .monthly-page-grid--two .monthly-panel {
    min-height: clamp(260px, 32vh, 360px);
  }

  .monthly-panel--main-chart {
    min-height: 0;
  }

  .monthly-action-grid {
    height: auto;
    grid-auto-rows: minmax(116px, auto);
    align-items: start;
  }

  .monthly-page--details .monthly-table-scroll {
    height: 100%;
    overflow-y: auto;
  }
}

@keyframes monthly-spin {
  to {
    transform: rotate(360deg);
  }
}

@media (max-width: 1280px) {
  .monthly-action-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 1100px) {
  .monthly-flow-layout,
  .monthly-page-grid--two,
  .monthly-table-grid {
    grid-template-columns: 1fr;
  }

  .monthly-flow-layout {
    min-height: 0;
  }

  .monthly-flow-side {
    grid-template-rows: auto auto;
  }

  .monthly-panel--daily-performance {
    min-height: clamp(420px, 58vh, 620px);
  }

  .monthly-panel--daily-performance .monthly-chart {
    min-height: clamp(320px, 46vh, 520px);
  }

  .monthly-page-grid--two {
    grid-template-rows: repeat(2, auto);
  }

  .monthly-insights {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 960px) {
  .monthly-dashboard__inner {
    padding-left: clamp(14px, 2.2vw, 28px);
    padding-bottom: 96px;
  }
}

@media (min-width: 961px) and (max-height: 760px) {
  .monthly-dashboard__inner {
    --monthly-template-gap: 8px;
    padding-top: 10px;
    padding-bottom: 12px;
  }

  .monthly-header p {
    margin-top: 5px;
    font-size: 0.86rem;
  }

  .monthly-month {
    padding: 8px;
    gap: 6px;
  }

  .monthly-page-nav {
    padding-block: 5px;
  }
}

@media (min-width: 961px) and (max-height: 860px) {
  .monthly-dashboard__inner {
    --monthly-template-gap: 8px;
    padding-top: 10px;
    padding-bottom: 10px;
  }

  .monthly-header {
    align-items: center;
  }

  .monthly-header__kicker {
    margin-bottom: 4px;
    font-size: 0.66rem;
  }

  .monthly-header h1 {
    font-size: clamp(1.48rem, 2.45vw, 2.06rem);
  }

  .monthly-header p {
    margin-top: 5px;
    font-size: 0.86rem;
  }

  .monthly-month {
    width: min(310px, 100%);
    padding: 8px;
    gap: 6px;
  }

  .monthly-month__control {
    grid-template-columns: 32px minmax(0, 1fr) 32px;
  }

  .monthly-month__control button {
    width: 32px;
    height: 34px;
  }

  .monthly-month__control input {
    height: 34px;
    font-size: 0.92rem;
  }

  .monthly-page-nav {
    grid-template-columns: 36px minmax(0, 1fr) 36px;
    padding: 5px 7px;
  }

  .monthly-page-nav__arrow {
    width: 32px;
    height: 32px;
  }

  .monthly-page {
    gap: 8px;
  }

  .monthly-page__heading p {
    margin-bottom: 2px;
    font-size: 0.62rem;
  }

  .monthly-page__heading h2 {
    font-size: clamp(0.98rem, 1.25vw, 1.16rem);
  }

  .monthly-page__heading span {
    font-size: 0.76rem;
  }

  .monthly-kpi-grid {
    gap: 8px;
  }

  .monthly-page-grid,
  .monthly-table-grid {
    gap: 8px;
  }

  .monthly-page-grid--two .monthly-panel {
    min-height: clamp(220px, 28vh, 300px);
  }

  .monthly-panel {
    padding: 10px;
    gap: 8px;
  }

  .monthly-panel__head {
    gap: 8px;
  }

  .monthly-panel__head p {
    margin-bottom: 2px;
    font-size: 0.62rem;
  }

  .monthly-panel__head h2 {
    font-size: clamp(0.9rem, 1.12vw, 1.05rem);
  }

  .monthly-panel__head span {
    padding: 4px 8px;
    font-size: 0.66rem;
  }

  .monthly-page--main :deep(.monthly-kpi) {
    padding: 9px 10px;
    gap: 4px;
  }

  .monthly-page--main :deep(.monthly-kpi__label) {
    font-size: 0.64rem;
  }

  .monthly-page--main :deep(.monthly-kpi__icon) {
    width: 16px;
    height: 16px;
  }

  .monthly-page--main :deep(.monthly-kpi__value) {
    font-size: clamp(1.15rem, 2vw, 1.58rem);
  }

  .monthly-page--main :deep(.monthly-kpi__detail) {
    font-size: 0.68rem;
    line-height: 1.24;
  }

  .monthly-panel--main-chart {
    padding: 10px;
    gap: 8px;
  }

  .monthly-action-grid {
    gap: 7px;
  }

  .monthly-action-card {
    padding: 8px;
    gap: 5px;
  }

  .monthly-action-card span {
    padding: 2px 6px;
    font-size: 0.52rem;
  }

  .monthly-action-card h3 {
    font-size: 0.68rem;
  }

  .monthly-action-card strong {
    font-size: clamp(0.98rem, 1.2vw, 1.16rem);
    overflow-wrap: anywhere;
  }

  .monthly-action-card p {
    font-size: 0.66rem;
    line-height: 1.2;
    -webkit-line-clamp: 2;
  }

  .monthly-insights {
    padding: 8px;
    gap: 6px;
  }

  .monthly-insight {
    padding: 7px 9px;
    gap: 2px;
  }

  .monthly-insight span {
    font-size: 0.6rem;
  }

  .monthly-insight strong {
    font-size: 0.84rem;
  }

  .monthly-chart--context {
    min-height: 190px;
  }

  .monthly-page--details .monthly-table-scroll {
    max-height: none;
  }
}

@media (min-width: 961px) and (max-height: 700px) {
  .monthly-dashboard__inner {
    --monthly-template-gap: 6px;
    padding-top: 8px;
    padding-bottom: 8px;
  }

  .monthly-header__kicker {
    display: none;
  }

  .monthly-header h1 {
    font-size: clamp(1.32rem, 2.2vw, 1.82rem);
  }

  .monthly-header p {
    font-size: 0.78rem;
  }

  .monthly-month__head {
    display: none;
  }

  .monthly-page-nav__count {
    display: none;
  }

  .monthly-page--main :deep(.monthly-kpi) {
    padding: 8px 9px;
  }

  .monthly-page--main :deep(.monthly-kpi__value) {
    font-size: clamp(1.05rem, 1.8vw, 1.42rem);
  }

  .monthly-page-grid--two .monthly-panel {
    min-height: clamp(200px, 26vh, 250px);
  }

  .monthly-action-grid {
    grid-template-columns: repeat(6, minmax(0, 1fr));
  }

  .monthly-action-card {
    padding: 7px;
    gap: 4px;
  }

  .monthly-action-card h3 {
    font-size: 0.64rem;
  }

  .monthly-action-card strong {
    font-size: clamp(0.86rem, 1.05vw, 1rem);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .monthly-action-card p {
    font-size: 0.61rem;
    line-height: 1.18;
    -webkit-line-clamp: 1;
  }

  .monthly-insights {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }

  .monthly-chart--context {
    min-height: 165px;
  }

  .monthly-page--details .monthly-table-scroll {
    max-height: none;
  }
}

@media (max-width: 720px) {
  .monthly-header {
    align-items: stretch;
    flex-direction: column;
  }

  .monthly-month {
    width: 100%;
  }

  .monthly-kpi-grid,
  .monthly-insights,
  .monthly-action-overview,
  .monthly-action-grid {
    grid-template-columns: 1fr;
  }

  .monthly-flow-layout,
  .monthly-flow-side {
    gap: 9px;
  }

  .monthly-panel--daily-performance {
    min-height: clamp(360px, 62vh, 520px);
  }

  .monthly-panel--daily-performance .monthly-chart {
    min-height: clamp(280px, 48vh, 420px);
  }

  .monthly-dashboard__inner {
    grid-template-rows: auto auto auto;
    gap: 9px;
  }

  .monthly-page {
    gap: 9px;
  }

  .monthly-page-nav {
    grid-template-columns: 34px minmax(0, 1fr) 34px;
    gap: 6px;
    padding: 6px;
  }

  .monthly-page-nav__arrow {
    width: 32px;
    height: 32px;
  }

  .monthly-page-nav__center {
    grid-template-columns: 1fr;
    gap: 4px;
    justify-items: center;
  }

  .monthly-page-nav__count {
    display: none;
  }

  .monthly-page__heading {
    display: grid;
    align-items: start;
  }

  .monthly-page__heading span {
    max-width: none;
    text-align: left;
  }

  .monthly-panel__head {
    display: grid;
  }

  .monthly-panel__head span {
    max-width: 100%;
    width: fit-content;
    text-align: left;
  }
}

.monthly-dashboard {
  --monthly-bg: #f7f4ee;
  --monthly-muted-bg: #fbfaf7;
  background: #f7f4ee;
}

.monthly-month,
.monthly-page-nav,
.monthly-panel,
.monthly-state {
  border-color: rgba(148, 163, 184, 0.24);
  background: #fbfaf7;
  box-shadow: 0 6px 16px rgba(31, 41, 55, 0.045);
}

.monthly-action-card,
.monthly-action-summary,
.monthly-insight,
.monthly-brand-row,
.monthly-mini-empty {
  border-color: rgba(148, 163, 184, 0.18);
  background: #fffdf9;
  box-shadow: none;
}
</style>
