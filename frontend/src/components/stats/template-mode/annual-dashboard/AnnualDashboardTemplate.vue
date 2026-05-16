<template>
  <section class="annual-dashboard" aria-label="Dashboard annuel" @wheel="onWheel">
    <div class="annual-dashboard__inner">
      <header class="annual-header">
        <div class="annual-header__copy">
          <p class="annual-header__kicker">Template</p>
          <h1>Dashboard annuel</h1>
          <p>
            Synthese {{ selectedYear }} des ventes, du profit, des achats et du stock restant.
          </p>
        </div>

        <div class="annual-year">
          <div class="annual-year__head">
            <span>Annee selectionnee</span>
            <small>Saisie libre</small>
          </div>
          <div class="annual-year__control">
            <button
              type="button"
              aria-label="Annee precedente"
              @click="changeYear(-1)"
            >
              <ChevronLeft aria-hidden="true" />
            </button>
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
            <button
              type="button"
              aria-label="Annee suivante"
              @click="changeYear(1)"
            >
              <ChevronRight aria-hidden="true" />
            </button>
          </div>
        </div>
      </header>

      <div v-if="loading && !dashboard" class="annual-state annual-state--loading" role="status">
        <div class="annual-state__pulse"></div>
        <h2>Chargement du dashboard</h2>
        <p>Preparation des indicateurs annuels.</p>
      </div>

      <div v-else-if="error" class="annual-state annual-state--error" role="alert">
        <h2>Impossible de charger le template</h2>
        <p>{{ error }}</p>
        <button type="button" @click="loadDashboard">Reessayer</button>
      </div>

      <template v-else-if="dashboard">
        <div v-if="!dashboard.hasData" class="annual-state annual-state--empty" role="status">
          <h2>Aucune donnee disponible pour cette annee.</h2>
          <p>
            Les statistiques apparaitront des qu'une vente, un achat ou un article en stock sera
            renseigne sur {{ selectedYear }}.
          </p>
        </div>

        <template v-else>
          <nav class="annual-page-nav" aria-label="Navigation du dashboard annuel">
            <button
              type="button"
              class="annual-page-nav__arrow"
              :disabled="activePage === 0"
              aria-label="Page precedente"
              @click="previousPage"
            >
              <ChevronLeft aria-hidden="true" />
            </button>

            <div class="annual-page-nav__center">
              <span class="annual-page-nav__count">{{ activePage + 1 }} / {{ pages.length }}</span>
              <strong>{{ currentPage.label }}</strong>
              <div class="annual-page-nav__dots" role="tablist" aria-label="Pages">
                <button
                  v-for="(page, index) in pages"
                  :key="page.key"
                  type="button"
                  class="annual-page-nav__dot"
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
              class="annual-page-nav__arrow"
              :disabled="activePage === pages.length - 1"
              aria-label="Page suivante"
              @click="nextPage"
            >
              <ChevronRight aria-hidden="true" />
            </button>
          </nav>

          <section
            class="annual-stage"
            :class="{ 'is-dragging': pointerDrag.active }"
            aria-live="polite"
            @pointerdown="onPointerDown"
            @pointerup="onPointerUp"
            @pointercancel="resetPointerDrag"
            @lostpointercapture="resetPointerDrag"
          >
            <div class="annual-pages" :style="pageTrackStyle">
              <article class="annual-page annual-page--side" aria-label="Flux et mix produit">
                <div class="annual-page__heading">
                  <p>Flux de tresorerie</p>
                  <h2>Achats, ventes et categories</h2>
                  
                </div>

                <div class="annual-page-grid annual-page-grid--two">
                  <section class="annual-panel">
                    <div class="annual-panel__head">
                      <div>
                        <p>Flux de cash</p>
                        <h2>Cash investi vs encaissé</h2>
                      </div>
                      <span>{{ formatMoney(summary.purchaseSpend) }} investis</span>
                    </div>
                    <VChart class="annual-chart" :option="cashflowOption" autoresize />
                  </section>

                  <section class="annual-panel">
                    <div class="annual-panel__head">
                      <div>
                        <p>Categories</p>
                        <h2>Contribution au profit</h2>
                      </div>
                      <span>Top {{ dashboard.topCategories.length }}</span>
                    </div>
                    <div v-if="dashboard.topCategories.length" class="annual-chart-wrap">
                      <VChart class="annual-chart" :option="categoryOption" autoresize />
                    </div>
                    <div v-else class="annual-mini-empty">Aucune categorie vendue sur cette annee.</div>
                  </section>
                </div>

                <section class="annual-insights" aria-label="Indicateurs de flux">
                  <div v-for="item in insightItems" :key="item.label" class="annual-insight">
                    <span>{{ item.label }}</span>
                    <strong>{{ item.value }}</strong>
                  </div>
                </section>
              </article>

              <article class="annual-page annual-page--main" aria-label="Pilotage annuel">
                <div class="annual-main-stack">
                  <section class="annual-kpi-grid" aria-label="KPI annuels">
                    <AnnualKpiCard
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

                <section class="annual-panel annual-panel--main-chart">
                  <div class="annual-panel__head">
                    <div>
                      <p>Pilotage revendeur</p>
                      <h2>Plan d'action</h2>
                    </div>
                    <span>{{ selectedYear }}</span>
                  </div>
                  <div class="annual-action-grid" aria-label="Plan d'action revendeur">
                    <article
                      v-for="item in resellerSignals"
                      :key="item.title"
                      class="annual-action-card"
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

              <article class="annual-page annual-page--details" aria-label="Details metier">
                <div class="annual-page__heading">
                  <p>Analyse operationnelle</p>
                  <h2>Rentabilite et stock dormant</h2>
                </div>

                <section class="annual-table-grid" aria-label="Tableaux metier">
                  <article class="annual-panel annual-table-card">
                    <div class="annual-panel__head">
                      <div>
                        <p>Meilleures ventes</p>
                        <h2>Produits les plus rentables</h2>
                      </div>
                      <span>{{ dashboard.topProducts.length }} ventes</span>
                    </div>

                    <div v-if="topProductsPreview.length" class="annual-table-scroll">
                      <table class="annual-table annual-table--products">
                        <thead>
                          <tr>
                            <th>Produit</th>
                            <th>Date</th>
                            <th>Achat</th>
                            <th>Vente</th>
                            <th>Profit</th>
                            <th>ROI</th>
                          </tr>
                        </thead>
                        <tbody>
                          <tr v-for="product in topProductsPreview" :key="product.id">
                            <td>
                              <strong :title="product.name">{{ product.name }}</strong>
                              <span>{{ product.category }}</span>
                            </td>
                            <td>{{ formatDate(product.soldAt) }}</td>
                            <td>{{ formatMoney(product.purchasePrice) }}</td>
                            <td>{{ formatMoney(product.salePrice) }}</td>
                            <td :class="profitClass(product.profit)">
                              {{ formatMoney(product.profit) }}
                            </td>
                            <td>{{ formatRatio(product.roi) }}</td>
                          </tr>
                        </tbody>
                      </table>
                    </div>

                    <div v-else class="annual-mini-empty">Aucune vente avec profit sur cette annee.</div>
                  </article>

                  <article class="annual-panel annual-table-card">
                    <div class="annual-panel__head">
                      <div>
                        <p>Stock a surveiller</p>
                        <h2>Articles immobilises</h2>
                      </div>
                      <span>Au {{ formatDate(dashboard.asOf) }}</span>
                    </div>

                    <div v-if="inventoryPreview.length" class="annual-table-scroll">
                      <table class="annual-table annual-table--inventory">
                        <thead>
                          <tr>
                            <th>Produit</th>
                            <th>Achat</th>
                            <th>Cout</th>
                            <th>Age</th>
                          </tr>
                        </thead>
                        <tbody>
                          <tr v-for="item in inventoryPreview" :key="item.id">
                            <td>
                              <strong :title="item.name">{{ item.name }}</strong>
                              <span>{{ item.category }}</span>
                            </td>
                            <td>{{ formatDate(item.purchasedAt) }}</td>
                            <td>{{ formatMoney(item.purchasePrice) }}</td>
                            <td>
                              <span class="annual-age" :class="{ 'is-old': Number(item.ageInDays || 0) >= 120 }">
                                {{ formatDays(item.ageInDays) }}
                              </span>
                            </td>
                          </tr>
                        </tbody>
                      </table>
                    </div>

                    <div v-else class="annual-mini-empty">Aucun stock immobilise a cette date.</div>
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
  ChartNoAxesCombined,
  ChevronLeft,
  ChevronRight,
  CirclePercent,
  TrendingUp,
  Wallet,
} from 'lucide-vue-next'
import StatsServices from '@/services/StatsServices'
import AnnualKpiCard from './AnnualKpiCard.vue'

type DashboardSummary = {
  revenue: number
  profit: number
  marginRate: number
  roi: number
  itemsSold: number
  averageSalePrice: number
  averageProfit: number
  purchaseSpend: number
  itemsBought: number
  remainingStockCount: number
  remainingStockValue: number
  sellThroughRate: number
  averageHoldDays: number
}

type DashboardMonth = {
  month: number
  revenue: number
  profit: number
  purchaseSpend: number
  itemsSold: number
  itemsBought: number
}

type DashboardCategory = {
  name: string
  revenue: number
  profit: number
  count: number
}

type DashboardProduct = {
  id: number
  name: string
  category: string
  purchasePrice: number
  salePrice: number
  profit: number
  roi: number
  soldAt: string
}

type DashboardInventoryItem = {
  id: number
  name: string
  category: string
  purchasePrice: number
  purchasedAt: string | null
  ageInDays: number | null
}

type AnnualDashboard = {
  year: number
  asOf: string
  hasData: boolean
  partialData: boolean
  partialDataReasons: string[]
  summary: DashboardSummary
  monthly: DashboardMonth[]
  topCategories: DashboardCategory[]
  topProducts: DashboardProduct[]
  inventoryAging: DashboardInventoryItem[]
}

type AnnualTemplateState = {
  year?: number
}

const props = defineProps<{
  initialState?: AnnualTemplateState
}>()

const emit = defineEmits<{
  (event: 'state-change', state: AnnualTemplateState): void
}>()

const monthLabels = ['Jan', 'Fev', 'Mar', 'Avr', 'Mai', 'Juin', 'Juil', 'Aout', 'Sep', 'Oct', 'Nov', 'Dec']
const currentYear = new Date().getFullYear()

function normalizeYearValue(value: unknown, fallback = currentYear) {
  if (typeof value === 'string' && !value.trim()) return fallback
  const year = Math.trunc(Number(value))
  if (!Number.isFinite(year)) return fallback
  return Math.max(2000, Math.min(currentYear + 1, year))
}

function normalizeInitialYear(value: unknown) {
  return normalizeYearValue(value, currentYear)
}

const selectedYear = ref(normalizeInitialYear(props.initialState?.year))
const yearDraft = ref(String(selectedYear.value))
const minYear = ref(currentYear - 5)
const maxYear = ref(currentYear)
const dashboard = ref<AnnualDashboard | null>(null)
const loading = ref(false)
const error = ref('')
const activePage = ref(1)
const DETAILS_ROW_LIMIT = 20
const pointerDrag = ref({
  active: false,
  pointerId: -1,
  startX: 0,
  startY: 0,
  startedAt: 0,
})
let requestId = 0
let lastWheelPageChangeAt = 0

const pages = [
  { key: 'flow', label: 'Flux & categories' },
  { key: 'pilotage', label: 'Pilotage annuel' },
  { key: 'details', label: 'Rentabilite & stock' },
]

watch(
  () => props.initialState,
  (state) => {
    const nextYear = normalizeInitialYear(state?.year)
    if (selectedYear.value !== nextYear) selectedYear.value = nextYear
    if (activePage.value !== 1) activePage.value = 1
  },
  { deep: true },
)

watch(
  selectedYear,
  () => {
    emit('state-change', {
      year: selectedYear.value,
    })
  },
  { immediate: true },
)

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

const emptySummary: DashboardSummary = {
  revenue: 0,
  profit: 0,
  marginRate: 0,
  roi: 0,
  itemsSold: 0,
  averageSalePrice: 0,
  averageProfit: 0,
  purchaseSpend: 0,
  itemsBought: 0,
  remainingStockCount: 0,
  remainingStockValue: 0,
  sellThroughRate: 0,
  averageHoldDays: 0,
}

const summary = computed(() => dashboard.value?.summary ?? emptySummary)
const monthlyRows = computed(() => normalizeMonthly(dashboard.value?.monthly ?? []))
const currentPage = computed(() => pages[activePage.value] ?? pages[1])
const pageTrackStyle = computed(() => ({
  transform: `translate3d(-${activePage.value * 100}%, 0, 0)`,
}))
const topProductsPreview = computed(() => dashboard.value?.topProducts.slice(0, DETAILS_ROW_LIMIT) ?? [])
const inventoryPreview = computed(() => dashboard.value?.inventoryAging.slice(0, DETAILS_ROW_LIMIT) ?? [])
const resellerSignals = computed(() => {
  const oldStock = (dashboard.value?.inventoryAging ?? []).filter((item) => Number(item.ageInDays || 0) >= 120)
  const cashNet = summary.value.revenue - summary.value.purchaseSpend
  const bestCategory = dashboard.value?.topCategories?.[0]
  const holdDays = Number(summary.value.averageHoldDays || 0)
  const rotation =
    holdDays <= 0
      ? { value: 'A suivre', detail: 'Ajoute les dates d achat/vente pour mesurer la rotation.', tone: 'neutral' }
      : holdDays <= 45
        ? { value: 'Rapide', detail: `${formatDays(holdDays)} en moyenne avant revente. Continue sur ces formats.`, tone: 'positive' }
        : holdDays <= 120
          ? { value: 'Correcte', detail: `${formatDays(holdDays)} en moyenne. Surveille les tailles/modeles lents.`, tone: 'neutral' }
          : { value: 'Lente', detail: `${formatDays(holdDays)} en moyenne. Priorise les articles faciles a sortir.`, tone: 'warning' }

  return [
    {
      badge: 'Cash',
      title: cashNet >= 0 ? 'Cycle sain' : 'Cash sous pression',
      value: formatMoney(cashNet),
      detail: cashNet >= 0 ? 'Encaissements superieurs aux achats sur l annee.' : 'Achats superieurs aux ventes: reduis le stock lent.',
      tone: cashNet >= 0 ? 'positive' : 'warning',
    },
    {
      badge: 'Stock',
      title: oldStock.length ? 'Stock dormant' : 'Stock sous controle',
      value: oldStock.length ? `${oldStock.length} a traiter` : 'RAS',
      detail: oldStock.length ? 'Articles a relister, remiser ou sortir en bundle.' : 'Pas d alerte agee dans les articles les plus anciens.',
      tone: oldStock.length ? 'warning' : 'positive',
    },
    {
      badge: 'Sourcing',
      title: bestCategory?.name ? 'Categorie forte' : 'Categorie a identifier',
      value: bestCategory?.name || 'Aucune',
      detail: bestCategory?.name
        ? `${formatMoney(bestCategory.profit)} de profit: a prioriser si le stock tourne.`
        : 'Ajoute categories/marques pour voir ce qui rapporte vraiment.',
      tone: bestCategory?.profit && bestCategory.profit > 0 ? 'positive' : 'neutral',
    },
    {
      badge: 'Rotation',
      title: 'Vitesse de revente',
      value: rotation.value,
      detail: rotation.detail,
      tone: rotation.tone,
    },
  ]
})
const kpiCards = computed(() => [
  {
    label: "Chiffre d'affaires",
    value: formatMoney(summary.value.revenue),
    detail: `${formatNumber(summary.value.itemsSold)} articles vendus`,
    tone: 'primary' as const,
    icon: BadgeEuro,
  },
  {
    label: 'Profit brut',
    value: formatMoney(summary.value.profit),
    detail: `${formatMoney(summary.value.averageProfit)} par vente`,
    tone: summary.value.profit >= 0 ? ('profit' as const) : ('warning' as const),
    icon: TrendingUp,
  },
  {
    label: 'Taux de marge',
    value: formatRatio(summary.value.marginRate),
    detail: 'Profit / chiffre d affaires',
    tone: summary.value.marginRate >= 0 ? ('profit' as const) : ('warning' as const),
    icon: CirclePercent,
  },
  {
    label: 'ROI moyen',
    value: formatRatio(summary.value.roi),
    detail: 'Profit / cout vendu',
    tone: summary.value.roi >= 0 ? ('profit' as const) : ('warning' as const),
    icon: ChartNoAxesCombined,
  },
  {
    label: 'Capital investi',
    value: formatMoney(summary.value.purchaseSpend),
    detail: `${formatNumber(summary.value.itemsBought)} achats de stock`,
    tone: 'primary' as const,
    icon: Wallet,
  },
  {
    label: 'Stock restant',
    value: `${formatNumber(summary.value.remainingStockCount)} articles`,
    detail: `${formatMoney(summary.value.remainingStockValue)} immobilises`,
    tone: summary.value.remainingStockCount > 0 ? ('warning' as const) : ('neutral' as const),
    icon: Boxes,
  },
])

const insightItems = computed(() => [
  { label: 'Panier moyen', value: formatMoney(summary.value.averageSalePrice) },
  { label: 'Depenses achat', value: formatMoney(summary.value.purchaseSpend) },
  { label: 'Profit moyen', value: formatMoney(summary.value.averageProfit) },
  { label: 'Detention moyenne', value: formatDays(summary.value.averageHoldDays) },
])

const cashflowOption = computed(() => {
  const rows = monthlyRows.value
  return {
    color: ['#2563eb', '#f59e0b'],
    grid: { left: 8, right: 10, top: 34, bottom: 12, containLabel: true },
    tooltip: {
      trigger: 'axis',
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
        name: 'Ventes',
        type: 'bar',
        barMaxWidth: 22,
        data: rows.map((row) => row.revenue),
        itemStyle: { borderRadius: [5, 5, 0, 0] },
      },
      {
        name: 'Achats',
        type: 'bar',
        barMaxWidth: 22,
        data: rows.map((row) => row.purchaseSpend),
        itemStyle: { borderRadius: [5, 5, 0, 0] },
      },
    ],
  }
})

const categoryOption = computed(() => {
  const categories = [...(dashboard.value?.topCategories ?? [])].sort((a, b) => a.profit - b.profit)
  return {
    grid: { left: 8, right: 20, top: 10, bottom: 10, containLabel: true },
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: (params: Array<{ dataIndex: number; value: number }>) => {
        const item = categories[params?.[0]?.dataIndex ?? 0]
        if (!item) return ''
        return `${item.name}<br/>Profit: ${formatMoney(item.profit)}<br/>CA: ${formatMoney(item.revenue)}<br/>Ventes: ${formatNumber(item.count)}`
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
      data: categories.map((item) => item.name),
      axisTick: { show: false },
      axisLine: { show: false },
      axisLabel: { color: '#334155', width: 96, overflow: 'truncate' },
    },
    series: [
      {
        name: 'Profit',
        type: 'bar',
        barMaxWidth: 24,
        data: categories.map((item) => item.profit),
        itemStyle: {
          borderRadius: [0, 6, 6, 0],
          color: (params: { value: number }) => (Number(params.value ?? 0) >= 0 ? '#10b981' : '#f97316'),
        },
      },
    ],
  }
})

function normalizeDashboard(raw: any): AnnualDashboard {
  return {
    year: Number(raw?.year ?? selectedYear.value),
    asOf: String(raw?.asOf ?? ''),
    hasData: Boolean(raw?.hasData),
    partialData: Boolean(raw?.partialData),
    partialDataReasons: Array.isArray(raw?.partialDataReasons) ? raw.partialDataReasons.map(String) : [],
    summary: normalizeSummary(raw?.summary),
    monthly: normalizeMonthly(raw?.monthly),
    topCategories: Array.isArray(raw?.topCategories)
      ? raw.topCategories.map(normalizeCategory).filter((item: DashboardCategory) => item.name)
      : [],
    topProducts: Array.isArray(raw?.topProducts)
      ? raw.topProducts.map(normalizeProduct).filter((item: DashboardProduct) => item.name)
      : [],
    inventoryAging: Array.isArray(raw?.inventoryAging)
      ? raw.inventoryAging.map(normalizeInventoryItem).filter((item: DashboardInventoryItem) => item.name)
      : [],
  }
}

function normalizeSummary(raw: any): DashboardSummary {
  return {
    revenue: toNumber(raw?.revenue),
    profit: toNumber(raw?.profit),
    marginRate: toNumber(raw?.marginRate),
    roi: toNumber(raw?.roi),
    itemsSold: toNumber(raw?.itemsSold),
    averageSalePrice: toNumber(raw?.averageSalePrice),
    averageProfit: toNumber(raw?.averageProfit),
    purchaseSpend: toNumber(raw?.purchaseSpend),
    itemsBought: toNumber(raw?.itemsBought),
    remainingStockCount: toNumber(raw?.remainingStockCount),
    remainingStockValue: toNumber(raw?.remainingStockValue),
    sellThroughRate: toNumber(raw?.sellThroughRate),
    averageHoldDays: toNumber(raw?.averageHoldDays),
  }
}

function normalizeMonthly(raw: any): DashboardMonth[] {
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

function normalizeCategory(raw: any): DashboardCategory {
  return {
    name: String(raw?.name ?? ''),
    revenue: toNumber(raw?.revenue),
    profit: toNumber(raw?.profit),
    count: toNumber(raw?.count),
  }
}

function normalizeProduct(raw: any): DashboardProduct {
  return {
    id: toNumber(raw?.id),
    name: String(raw?.name ?? ''),
    category: String(raw?.category ?? 'Autre'),
    purchasePrice: toNumber(raw?.purchasePrice),
    salePrice: toNumber(raw?.salePrice),
    profit: toNumber(raw?.profit),
    roi: toNumber(raw?.roi),
    soldAt: String(raw?.soldAt ?? ''),
  }
}

function normalizeInventoryItem(raw: any): DashboardInventoryItem {
  return {
    id: toNumber(raw?.id),
    name: String(raw?.name ?? ''),
    category: String(raw?.category ?? 'Autre'),
    purchasePrice: toNumber(raw?.purchasePrice),
    purchasedAt: raw?.purchasedAt ? String(raw.purchasedAt) : null,
    ageInDays: raw?.ageInDays == null ? null : toNumber(raw.ageInDays),
  }
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

function formatDays(value: unknown) {
  if (value == null || value === '') return '--'
  const next = toNumber(value)
  return `${numberFormatter.format(next)} j`
}

function formatDate(value: unknown) {
  if (!value) return '--'
  const date = new Date(`${String(value)}T00:00:00`)
  if (Number.isNaN(date.getTime())) return '--'
  return date.toLocaleDateString('fr-FR', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
  })
}

function profitClass(value: unknown) {
  return toNumber(value) >= 0 ? 'is-positive' : 'is-negative'
}

function goToPage(index: number) {
  activePage.value = Math.max(0, Math.min(pages.length - 1, index))
}

function commitYear(value: unknown = yearDraft.value) {
  const next = normalizeYearValue(value, selectedYear.value)
  selectedYear.value = next
  yearDraft.value = String(next)
}

function changeYear(delta: number) {
  commitYear(selectedYear.value + delta)
}

function onYearInput(event: Event) {
  const input = event.target as HTMLInputElement | null
  const value = String(input?.value ?? '').replace(/[^\d]/g, '').slice(0, 4)
  yearDraft.value = value
  if (input && input.value !== value) input.value = value
  if (value.length === 4) commitYear(value)
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

  if (deltaX < 0) {
    nextPage()
  } else {
    previousPage()
  }
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
  if (delta > 0) {
    nextPage()
  } else {
    previousPage()
  }
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
    const { data } = await StatsServices.annualDashboard(selectedYear.value)
    if (id !== requestId) return
    dashboard.value = normalizeDashboard(data)
  } catch (err: any) {
    if (id !== requestId) return
    error.value = err?.response?.data?.message ?? err?.message ?? 'Erreur inconnue'
    dashboard.value = null
  } finally {
    if (id === requestId) loading.value = false
  }
}

async function loadYearBounds() {
  try {
    const { data } = await StatsServices.dateBounds()
    const years = [data?.minDate, data?.maxDate]
      .map((value) => Number(String(value || '').slice(0, 4)))
      .filter((value) => Number.isFinite(value))
    if (!years.length) return
    minYear.value = Math.min(...years, currentYear - 5)
    maxYear.value = Math.min(Math.max(...years, currentYear), currentYear)
  } catch {
    minYear.value = currentYear - 5
    maxYear.value = currentYear
  }
}

watch(selectedYear, () => {
  yearDraft.value = String(selectedYear.value)
  loadDashboard()
})

onMounted(async () => {
  window.addEventListener('keydown', onKeyDown)
  await loadYearBounds()
  await loadDashboard()
})

onBeforeUnmount(() => {
  window.removeEventListener('keydown', onKeyDown)
})
</script>

<style scoped>
.annual-dashboard {
  --annual-template-gap: clamp(10px, 1.35vh, 16px);
  width: 100%;
  height: 100%;
  min-width: 0;
  min-height: 0;
  overflow: auto;
  overscroll-behavior: contain;
  overscroll-behavior-x: none;
  scrollbar-width: thin;
  scrollbar-color: rgba(91, 92, 226, 0.42) rgba(226, 232, 240, 0.72);
  background:
    linear-gradient(180deg, rgba(246, 248, 255, 0.96), rgba(236, 241, 249, 0.98)),
    radial-gradient(circle at top left, rgba(129, 140, 248, 0.14), transparent 34%);
}

.annual-dashboard::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}

.annual-dashboard::-webkit-scrollbar-track {
  background: rgba(226, 232, 240, 0.72);
}

.annual-dashboard::-webkit-scrollbar-thumb {
  border-radius: 999px;
  background: rgba(91, 92, 226, 0.42);
}

.annual-dashboard__inner {
  width: min(1480px, 100%);
  min-height: 100%;
  min-width: 0;
  margin: 0 auto;
  padding: clamp(14px, 1.8vw, 24px) clamp(14px, 2.2vw, 28px) clamp(16px, 2vw, 24px)
    calc(96px + clamp(14px, 2.2vw, 28px));
  display: grid;
  grid-template-rows: auto auto auto;
  gap: var(--annual-template-gap);
  align-content: start;
}

.annual-header {
  min-width: 0;
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 18px;
}

.annual-header__copy {
  min-width: 0;
}

.annual-header__kicker {
  margin: 0 0 7px;
  color: #5b5ce2;
  font-size: 0.72rem;
  font-weight: 820;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

.annual-header h1 {
  margin: 0;
  color: #111827;
  font-size: clamp(1.62rem, 2.8vw, 2.45rem);
  line-height: 1.02;
  font-weight: 860;
}

.annual-header p {
  margin: 7px 0 0;
  max-width: 72ch;
  color: #64748b;
  font-size: 0.96rem;
  line-height: 1.45;
}

.annual-year {
  flex: 0 0 auto;
  width: min(315px, 100%);
  border: 1px solid rgba(99, 102, 241, 0.2);
  border-radius: 12px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.92), rgba(247, 249, 255, 0.88)),
    linear-gradient(135deg, rgba(91, 92, 226, 0.08), rgba(59, 130, 246, 0.05));
  box-shadow:
    0 14px 34px rgba(31, 41, 55, 0.1),
    inset 0 1px 0 rgba(255, 255, 255, 0.8);
  padding: 10px;
  display: grid;
  gap: 8px;
}

.annual-year__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.annual-year__head span {
  color: #64748b;
  font-size: 0.68rem;
  font-weight: 780;
  letter-spacing: 0.11em;
  text-transform: uppercase;
}

.annual-year__head small {
  color: #5b5ce2;
  font-size: 0.68rem;
  font-weight: 760;
}

.annual-year__control {
  display: grid;
  grid-template-columns: 34px minmax(0, 1fr) 34px;
  align-items: center;
  gap: 6px;
}

.annual-year__control button {
  border: 1px solid rgba(99, 102, 241, 0.22);
  background: rgba(255, 255, 255, 0.8);
  color: #4f46e5;
  display: inline-grid;
  place-items: center;
}

.annual-year__control button {
  width: 34px;
  height: 36px;
  border-radius: 10px;
}

.annual-year__control button:hover {
  border-color: rgba(79, 70, 229, 0.42);
  background: #eef2ff;
}

.annual-year__control svg {
  width: 16px;
  height: 16px;
}

.annual-year__control input {
  width: 100%;
  min-width: 0;
  height: 38px;
  border: 1px solid rgba(99, 102, 241, 0.26);
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.96);
  color: #111827;
  font-size: 1.08rem;
  font-weight: 820;
  text-align: center;
  padding: 0 8px;
  outline: none;
}

.annual-year__control input:focus {
  border-color: rgba(91, 92, 226, 0.58);
  box-shadow: 0 0 0 3px rgba(91, 92, 226, 0.12);
}

.annual-page-nav {
  min-width: 0;
  border: 1px solid rgba(148, 163, 184, 0.24);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.58);
  box-shadow: 0 10px 24px rgba(31, 41, 55, 0.06);
  padding: 7px 9px;
  display: grid;
  grid-template-columns: 40px minmax(0, 1fr) 40px;
  align-items: center;
  gap: 10px;
}

.annual-page-nav__arrow {
  width: 36px;
  height: 36px;
  border: 1px solid rgba(99, 102, 241, 0.22);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.78);
  color: #4338ca;
  display: grid;
  place-items: center;
}

.annual-page-nav__arrow svg {
  width: 18px;
  height: 18px;
}

.annual-page-nav__arrow:disabled {
  opacity: 0.36;
  pointer-events: none;
}

.annual-page-nav__center {
  min-width: 0;
  display: grid;
  grid-template-columns: auto minmax(0, auto) auto;
  justify-content: center;
  align-items: center;
  gap: 10px;
}

.annual-page-nav__count {
  color: #64748b;
  font-size: 0.72rem;
  font-weight: 780;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.annual-page-nav__center strong {
  color: #111827;
  font-size: 0.92rem;
  font-weight: 800;
  white-space: nowrap;
}

.annual-page-nav__dots {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.annual-page-nav__dot {
  width: 7px;
  height: 7px;
  border: 0;
  border-radius: 999px;
  background: #cbd5e1;
  padding: 0;
}

.annual-page-nav__dot.is-active {
  width: 22px;
  background: #5b5ce2;
}

.annual-stage {
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

.annual-stage.is-dragging {
  cursor: grabbing;
}

.annual-stage.is-dragging .annual-pages {
  transition-duration: 180ms;
}

.annual-pages {
  height: auto;
  min-height: 0;
  display: flex;
  transition:
    transform 320ms cubic-bezier(0.22, 1, 0.36, 1),
    opacity 180ms ease;
  will-change: transform;
}

.annual-page {
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

.annual-page--side {
  grid-template-rows: auto minmax(0, 1fr) auto;
}

.annual-page--main {
  grid-template-rows: auto auto;
  align-content: start;
  gap: 12px;
}

.annual-page--details {
  grid-template-rows: auto auto;
  align-content: start;
  gap: 10px;
}

.annual-page__heading {
  min-width: 0;
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 12px;
}

.annual-page__heading p {
  margin: 0 0 4px;
  color: #5b5ce2;
  font-size: 0.68rem;
  font-weight: 820;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

.annual-page__heading h2 {
  margin: 0;
  color: #111827;
  font-size: clamp(1.08rem, 1.5vw, 1.28rem);
  line-height: 1.15;
  font-weight: 820;
}

.annual-page__heading span {
  max-width: 58ch;
  color: #64748b;
  font-size: 0.86rem;
  line-height: 1.35;
  text-align: right;
}

.annual-main-stack {
  min-width: 0;
  display: grid;
  gap: 10px;
}

.annual-kpi-grid {
  min-width: 0;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.annual-page--main :deep(.annual-kpi) {
  padding: clamp(11px, 1.2vw, 14px);
  gap: 7px;
}

.annual-page--main :deep(.annual-kpi__value) {
  font-size: clamp(1.35rem, 2.25vw, 2rem);
}

.annual-page--main :deep(.annual-kpi__detail) {
  font-size: 0.78rem;
}

.annual-insights {
  border: 1px solid rgba(148, 163, 184, 0.26);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.68);
  padding: 10px;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 8px;
}

.annual-insight {
  min-width: 0;
  border-radius: 7px;
  background: rgba(248, 250, 252, 0.96);
  padding: 10px 12px;
  display: grid;
  gap: 4px;
}

.annual-insight span {
  color: #64748b;
  font-size: 0.72rem;
  font-weight: 720;
  text-transform: uppercase;
  letter-spacing: 0.06em;
}

.annual-insight strong {
  color: #1f2937;
  font-size: 1rem;
  overflow-wrap: anywhere;
}

.annual-panel {
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

.annual-panel__head {
  min-width: 0;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.annual-panel__head div {
  min-width: 0;
}

.annual-panel__head p {
  margin: 0 0 4px;
  color: #5b5ce2;
  font-size: 0.7rem;
  font-weight: 820;
  letter-spacing: 0.13em;
  text-transform: uppercase;
}

.annual-panel__head h2 {
  margin: 0;
  color: #111827;
  font-size: clamp(1rem, 1.4vw, 1.24rem);
  line-height: 1.15;
  font-weight: 790;
}

.annual-panel__head span {
  flex: 0 0 auto;
  max-width: 45%;
  border-radius: 999px;
  background: #eef2ff;
  color: #4338ca;
  padding: 6px 10px;
  font-size: 0.74rem;
  font-weight: 760;
  overflow-wrap: anywhere;
  text-align: right;
}

.annual-panel--main-chart {
  padding: 12px;
  gap: 10px;
  grid-template-rows: auto auto;
}

.annual-panel--main-chart .annual-panel__head {
  align-items: center;
  gap: 8px;
}

.annual-panel--main-chart .annual-panel__head p {
  display: none;
}

.annual-panel--main-chart .annual-panel__head h2 {
  font-size: 0.92rem;
  line-height: 1.1;
}

.annual-panel--main-chart .annual-panel__head span {
  padding: 4px 8px;
  font-size: 0.68rem;
}

.annual-chart {
  width: 100%;
  height: 100%;
  min-width: 0;
  min-height: 0;
}

.annual-chart-wrap {
  min-width: 0;
  min-height: 0;
  height: 100%;
}

.annual-action-grid {
  min-width: 0;
  min-height: 0;
  height: auto;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  grid-auto-rows: auto;
  align-items: start;
  gap: 9px;
}

.annual-action-card {
  min-width: 0;
  border: 1px solid rgba(148, 163, 184, 0.22);
  border-radius: 8px;
  background:
    linear-gradient(180deg, rgba(248, 250, 252, 0.9), rgba(241, 245, 249, 0.78)),
    radial-gradient(circle at top right, rgba(99, 102, 241, 0.08), transparent 36%);
  padding: 10px;
  display: grid;
  grid-template-rows: auto auto auto auto;
  align-content: start;
  gap: 6px;
}

.annual-action-card span {
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

.annual-action-card h3 {
  min-width: 0;
  margin: 0;
  color: #64748b;
  font-size: 0.72rem;
  line-height: 1.15;
  font-weight: 820;
}

.annual-action-card strong {
  min-width: 0;
  color: #111827;
  font-size: clamp(1rem, 1.18vw, 1.18rem);
  line-height: 1.05;
  font-weight: 820;
  overflow-wrap: anywhere;
}

.annual-action-card p {
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

.annual-action-card.is-positive strong {
  color: #047857;
}

.annual-action-card.is-warning strong {
  color: #b45309;
}

.annual-action-card.is-warning span {
  background: rgba(245, 158, 11, 0.12);
  color: #b45309;
}

.annual-action-card.is-positive span {
  background: rgba(16, 185, 129, 0.12);
  color: #047857;
}

.annual-two-col {
  min-width: 0;
  display: grid;
  grid-template-columns: minmax(0, 1.1fr) minmax(340px, 0.9fr);
  gap: 14px;
}

.annual-page-grid {
  min-width: 0;
  min-height: 0;
  height: auto;
  display: grid;
  gap: 14px;
  align-items: start;
}

.annual-page-grid--two {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.annual-page-grid--two .annual-panel {
  min-height: clamp(300px, 34vh, 430px);
}

.annual-table-grid {
  min-width: 0;
  min-height: 0;
  height: auto;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
  align-items: start;
}

.annual-table-card {
  align-content: start;
  overflow: hidden;
}

.annual-page--details .annual-table-card {
  padding: clamp(12px, 1.4vw, 16px);
  gap: 9px;
}

.annual-page--details .annual-panel__head {
  align-items: center;
}

.annual-page--details .annual-panel__head p {
  margin-bottom: 3px;
  font-size: 0.66rem;
}

.annual-page--details .annual-panel__head h2 {
  font-size: clamp(0.98rem, 1.25vw, 1.16rem);
}

.annual-page--details .annual-panel__head span {
  padding: 5px 9px;
  font-size: 0.68rem;
}

.annual-table-scroll {
  min-width: 0;
  min-height: 0;
  height: auto;
  max-height: clamp(260px, 38vh, 430px);
  overflow-y: auto;
  overflow-x: hidden;
  overscroll-behavior: contain;
  scrollbar-width: thin;
  scrollbar-color: rgba(91, 92, 226, 0.32) rgba(226, 232, 240, 0.58);
}

.annual-table-scroll::-webkit-scrollbar {
  width: 7px;
}

.annual-table-scroll::-webkit-scrollbar-track {
  background: rgba(226, 232, 240, 0.58);
}

.annual-table-scroll::-webkit-scrollbar-thumb {
  border-radius: 999px;
  background: rgba(91, 92, 226, 0.34);
}

.annual-table {
  width: 100%;
  min-width: 0;
  border-collapse: collapse;
  table-layout: fixed;
}

.annual-table th {
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

.annual-table th:first-child,
.annual-table td:first-child {
  text-align: left;
}

.annual-table td {
  color: #334155;
  font-size: 0.78rem;
  text-align: right;
  padding: 9px 7px;
  border-bottom: 1px solid rgba(148, 163, 184, 0.16);
  vertical-align: top;
  white-space: nowrap;
}

.annual-table td:first-child {
  white-space: normal;
}

.annual-table strong {
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

.annual-table td span {
  display: block;
  margin-top: 3px;
  color: #64748b;
  font-size: 0.74rem;
}

.annual-table .is-positive {
  color: #047857;
  font-weight: 790;
}

.annual-table .is-negative {
  color: #dc2626;
  font-weight: 790;
}

.annual-page--details .annual-table th {
  padding: 7px 6px;
  font-size: 0.66rem;
}

.annual-page--details .annual-table td {
  padding: 7px 6px;
  font-size: 0.76rem;
}

.annual-page--details .annual-table strong {
  line-height: 1.18;
  -webkit-line-clamp: 1;
}

.annual-page--details .annual-table td span {
  margin-top: 2px;
  font-size: 0.7rem;
}

.annual-page--details .annual-age {
  padding: 3px 7px;
}

.annual-age {
  display: inline-flex !important;
  width: fit-content;
  margin-left: auto;
  border-radius: 999px;
  background: #eef2ff;
  color: #4338ca !important;
  padding: 4px 8px;
  font-weight: 760;
}

.annual-age.is-old {
  background: #fff7ed;
  color: #c2410c !important;
}

.annual-state {
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

.annual-state h2 {
  margin: 0;
  color: #111827;
  font-size: clamp(1.18rem, 2vw, 1.6rem);
  line-height: 1.15;
  font-weight: 800;
}

.annual-state p {
  margin: 0;
  max-width: 56ch;
  color: #64748b;
  line-height: 1.5;
}

.annual-state button {
  margin-top: 10px;
  min-height: 40px;
  border: 1px solid rgba(99, 102, 241, 0.32);
  border-radius: 8px;
  background: #4f46e5;
  color: #fff;
  padding: 0 14px;
  font-weight: 760;
}

.annual-state__pulse {
  width: 46px;
  height: 46px;
  border-radius: 999px;
  border: 4px solid rgba(99, 102, 241, 0.16);
  border-top-color: #5b5ce2;
  animation: annual-spin 850ms linear infinite;
}

.annual-mini-empty {
  min-height: clamp(130px, 18vh, 220px);
  height: auto;
  border: 1px dashed rgba(148, 163, 184, 0.34);
  border-radius: 8px;
  background: #f8fafc;
  color: #64748b;
  display: grid;
  place-items: center;
  padding: 18px;
  text-align: center;
  line-height: 1.45;
}

@keyframes annual-spin {
  to {
    transform: rotate(360deg);
  }
}

@media (max-width: 1320px) {
  .annual-kpi-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 1100px) {
  .annual-two-col,
  .annual-page-grid--two,
  .annual-table-grid {
    grid-template-columns: 1fr;
  }

  .annual-page--side {
    grid-template-rows: auto minmax(0, 1fr) auto;
  }

  .annual-page-grid--two {
    grid-template-rows: repeat(2, auto);
  }

  .annual-page--details .annual-table-grid {
    grid-template-rows: repeat(2, auto);
  }

  .annual-insights {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 960px) {
  .annual-dashboard__inner {
    padding-left: clamp(14px, 2.2vw, 28px);
    padding-bottom: 96px;
  }
}

@media (min-width: 961px) and (min-height: 980px) {
  .annual-dashboard__inner {
    padding-top: clamp(18px, 2vh, 30px);
    padding-bottom: clamp(20px, 2.2vh, 34px);
  }

  .annual-stage {
    min-height: 0;
  }
}

@media (min-width: 961px) and (max-height: 760px) {
  .annual-dashboard__inner {
    --annual-template-gap: 8px;
    padding-top: 10px;
    padding-bottom: 12px;
  }

  .annual-header p {
    margin-top: 5px;
    font-size: 0.86rem;
  }

  .annual-year {
    padding: 8px;
    gap: 6px;
  }

  .annual-page-nav {
    padding-block: 5px;
  }
}

@media (max-width: 720px) {
  .annual-header {
    align-items: stretch;
    flex-direction: column;
  }

  .annual-year {
    width: 100%;
  }

  .annual-kpi-grid,
  .annual-insights {
    grid-template-columns: 1fr;
  }

  .annual-dashboard__inner {
    grid-template-rows: auto auto auto;
    gap: 9px;
  }

  .annual-page {
    gap: 9px;
  }

  .annual-page-nav {
    grid-template-columns: 34px minmax(0, 1fr) 34px;
    gap: 6px;
    padding: 6px;
  }

  .annual-page-nav__arrow {
    width: 32px;
    height: 32px;
  }

  .annual-page-nav__center {
    grid-template-columns: 1fr;
    gap: 4px;
    justify-items: center;
  }

  .annual-page-nav__count {
    display: none;
  }

  .annual-page__heading {
    display: grid;
    align-items: start;
  }

  .annual-page__heading span {
    max-width: none;
    text-align: left;
  }

  .annual-panel__head {
    display: grid;
  }

  .annual-panel__head span {
    max-width: 100%;
    width: fit-content;
    text-align: left;
  }

  .annual-chart,
  .annual-chart--main {
    height: 100%;
  }

  .annual-table {
    min-width: 0;
  }
}
</style>
