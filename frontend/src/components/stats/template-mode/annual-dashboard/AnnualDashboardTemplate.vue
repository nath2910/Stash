<template>
  <section class="annual-dashboard" aria-label="Dashboard annuel">
    <div class="annual-dashboard__inner">
      <header class="annual-header">
        <div class="annual-header__copy">
          <p class="annual-header__kicker">Template</p>
          <h1>Dashboard annuel</h1>
          <p>
            Vue metier du {{ selectedYear }} avec ventes, profit, achats et stock immobilise.
          </p>
        </div>

        <label class="annual-year">
          <span>Annee selectionnee</span>
          <select v-model.number="selectedYear" aria-label="Selectionner une annee">
            <option v-for="year in yearOptions" :key="year" :value="year">
              {{ year }}
            </option>
          </select>
        </label>
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
        <div v-if="dashboard.partialData" class="annual-quality" role="status">
          <AlertTriangle class="annual-quality__icon" aria-hidden="true" />
          <div>
            <strong>Donnees partielles</strong>
            <p>{{ partialDataText }}</p>
          </div>
        </div>

        <div v-if="!dashboard.hasData" class="annual-state annual-state--empty" role="status">
          <h2>Aucune donnee disponible pour cette annee.</h2>
          <p>
            Les statistiques apparaitront des qu'une vente, un achat ou un article en stock sera
            renseigne sur {{ selectedYear }}.
          </p>
        </div>

        <template v-else>
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

          <section class="annual-insights" aria-label="Indicateurs complementaires">
            <div v-for="item in insightItems" :key="item.label" class="annual-insight">
              <span>{{ item.label }}</span>
              <strong>{{ item.value }}</strong>
            </div>
          </section>

          <section class="annual-panel annual-panel--wide">
            <div class="annual-panel__head">
              <div>
                <p>Performance mensuelle</p>
                <h2>Chiffre d'affaires et profit</h2>
              </div>
              <span>{{ selectedYear }}</span>
            </div>
            <VChart class="annual-chart annual-chart--main" :option="performanceOption" autoresize />
          </section>

          <section class="annual-two-col">
            <article class="annual-panel">
              <div class="annual-panel__head">
                <div>
                  <p>Flux de cash</p>
                  <h2>Achats vs ventes</h2>
                </div>
                <span>{{ formatMoney(summary.purchaseSpend) }} investis</span>
              </div>
              <VChart class="annual-chart" :option="cashflowOption" autoresize />
            </article>

            <article class="annual-panel">
              <div class="annual-panel__head">
                <div>
                  <p>Mix produit</p>
                  <h2>Profit par categorie</h2>
                </div>
                <span>Top {{ dashboard.topCategories.length }}</span>
              </div>
              <div v-if="dashboard.topCategories.length" class="annual-chart-wrap">
                <VChart class="annual-chart" :option="categoryOption" autoresize />
              </div>
              <div v-else class="annual-mini-empty">Aucune categorie vendue sur cette annee.</div>
            </article>
          </section>

          <section class="annual-table-grid" aria-label="Tableaux metier">
            <article class="annual-panel annual-table-card">
              <div class="annual-panel__head">
                <div>
                  <p>Rentabilite</p>
                  <h2>Top produits</h2>
                </div>
                <span>{{ dashboard.topProducts.length }} ventes</span>
              </div>

              <div v-if="dashboard.topProducts.length" class="annual-table-scroll">
                <table class="annual-table">
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
                    <tr v-for="product in dashboard.topProducts" :key="product.id">
                      <td>
                        <strong>{{ product.name }}</strong>
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
                  <p>Stock immobilise</p>
                  <h2>Articles les plus anciens</h2>
                </div>
                <span>Au {{ formatDate(dashboard.asOf) }}</span>
              </div>

              <div v-if="dashboard.inventoryAging.length" class="annual-table-scroll">
                <table class="annual-table">
                  <thead>
                    <tr>
                      <th>Produit</th>
                      <th>Achat</th>
                      <th>Cout</th>
                      <th>Age</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="item in dashboard.inventoryAging" :key="item.id">
                      <td>
                        <strong>{{ item.name }}</strong>
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
        </template>
      </template>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import {
  AlertTriangle,
  BadgeEuro,
  Boxes,
  ChartNoAxesCombined,
  CirclePercent,
  PackageCheck,
  TrendingUp,
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

const monthLabels = ['Jan', 'Fev', 'Mar', 'Avr', 'Mai', 'Juin', 'Juil', 'Aout', 'Sep', 'Oct', 'Nov', 'Dec']
const currentYear = new Date().getFullYear()
const selectedYear = ref(currentYear)
const minYear = ref(currentYear - 5)
const maxYear = ref(currentYear)
const dashboard = ref<AnnualDashboard | null>(null)
const loading = ref(false)
const error = ref('')
let requestId = 0

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
const yearOptions = computed(() => {
  const start = Math.max(maxYear.value, currentYear)
  const end = Math.min(minYear.value, currentYear - 5)
  const years: number[] = []
  for (let year = start; year >= end; year--) {
    years.push(year)
  }
  return years
})
const partialDataText = computed(() => {
  const reasons = dashboard.value?.partialDataReasons ?? []
  return reasons.length ? reasons.join(' ') : 'Certaines lignes ne permettent pas de calculer toutes les metriques.'
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
    label: 'Sell-through',
    value: formatRatio(summary.value.sellThroughRate),
    detail: `${formatNumber(summary.value.itemsBought)} achats sur l annee`,
    tone: 'neutral' as const,
    icon: PackageCheck,
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

const performanceOption = computed(() => {
  const rows = monthlyRows.value
  return {
    color: ['#5b5ce2', '#10b981'],
    grid: { left: 8, right: 14, top: 36, bottom: 14, containLabel: true },
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      valueFormatter: (value: number) => compactMoneyFormatter.format(Number(value ?? 0)),
    },
    legend: {
      top: 0,
      right: 4,
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
        name: "Chiffre d'affaires",
        type: 'bar',
        barMaxWidth: 28,
        data: rows.map((row) => row.revenue),
        itemStyle: { borderRadius: [6, 6, 0, 0] },
      },
      {
        name: 'Profit',
        type: 'line',
        smooth: true,
        symbolSize: 7,
        lineStyle: { width: 3 },
        areaStyle: { opacity: 0.08 },
        data: rows.map((row) => row.profit),
      },
    ],
  }
})

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
  loadDashboard()
})

onMounted(async () => {
  await loadYearBounds()
  await loadDashboard()
})
</script>

<style scoped>
.annual-dashboard {
  width: 100%;
  height: 100%;
  min-width: 0;
  min-height: 0;
  overflow: auto;
  scrollbar-width: none;
  -ms-overflow-style: none;
  background:
    linear-gradient(180deg, rgba(246, 248, 255, 0.96), rgba(236, 241, 249, 0.98)),
    radial-gradient(circle at top left, rgba(129, 140, 248, 0.14), transparent 34%);
}

.annual-dashboard::-webkit-scrollbar {
  width: 0;
  height: 0;
  display: none;
}

.annual-dashboard__inner {
  width: min(1480px, 100%);
  min-width: 0;
  margin: 0 auto;
  padding: clamp(18px, 2.4vw, 30px) calc(96px + clamp(14px, 2.2vw, 28px)) clamp(22px, 3vw, 34px)
    clamp(14px, 2.2vw, 28px);
  display: grid;
  gap: 18px;
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
  font-size: clamp(1.7rem, 3.1vw, 2.7rem);
  line-height: 1.02;
  font-weight: 860;
}

.annual-header p {
  margin: 9px 0 0;
  max-width: 72ch;
  color: #64748b;
  font-size: 0.96rem;
  line-height: 1.45;
}

.annual-year {
  flex: 0 0 auto;
  min-width: min(220px, 100%);
  border: 1px solid rgba(148, 163, 184, 0.35);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.82);
  box-shadow: 0 12px 28px rgba(31, 41, 55, 0.08);
  padding: 9px 11px;
  display: grid;
  gap: 6px;
}

.annual-year span {
  color: #64748b;
  font-size: 0.68rem;
  font-weight: 780;
  letter-spacing: 0.11em;
  text-transform: uppercase;
}

.annual-year select {
  width: 100%;
  min-width: 0;
  height: 38px;
  border: 1px solid rgba(99, 102, 241, 0.24);
  border-radius: 6px;
  background: #fff;
  color: #111827;
  font-size: 0.98rem;
  font-weight: 760;
  padding: 0 10px;
}

.annual-quality {
  border: 1px solid rgba(245, 158, 11, 0.28);
  border-radius: 8px;
  background: #fff7ed;
  color: #92400e;
  padding: 12px 14px;
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 10px;
  align-items: start;
}

.annual-quality__icon {
  width: 18px;
  height: 18px;
  margin-top: 1px;
}

.annual-quality strong {
  display: block;
  color: #78350f;
  font-size: 0.88rem;
}

.annual-quality p {
  margin: 3px 0 0;
  font-size: 0.84rem;
  line-height: 1.42;
}

.annual-kpi-grid {
  min-width: 0;
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 12px;
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
  border: 1px solid rgba(148, 163, 184, 0.28);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.9);
  box-shadow: 0 14px 34px rgba(31, 41, 55, 0.08);
  padding: clamp(14px, 1.8vw, 18px);
  display: grid;
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

.annual-chart {
  width: 100%;
  height: 300px;
  min-width: 0;
}

.annual-chart--main {
  height: 360px;
}

.annual-chart-wrap {
  min-width: 0;
}

.annual-two-col {
  min-width: 0;
  display: grid;
  grid-template-columns: minmax(0, 1.1fr) minmax(340px, 0.9fr);
  gap: 14px;
}

.annual-table-grid {
  min-width: 0;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.annual-table-card {
  align-content: start;
}

.annual-table-scroll {
  min-width: 0;
  overflow-x: auto;
  scrollbar-width: thin;
}

.annual-table {
  width: 100%;
  min-width: 620px;
  border-collapse: collapse;
}

.annual-table th {
  color: #64748b;
  font-size: 0.7rem;
  font-weight: 780;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  text-align: right;
  padding: 9px 8px;
  border-bottom: 1px solid rgba(148, 163, 184, 0.24);
  white-space: nowrap;
}

.annual-table th:first-child,
.annual-table td:first-child {
  text-align: left;
}

.annual-table td {
  color: #334155;
  font-size: 0.82rem;
  text-align: right;
  padding: 11px 8px;
  border-bottom: 1px solid rgba(148, 163, 184, 0.16);
  vertical-align: top;
  white-space: nowrap;
}

.annual-table td:first-child {
  min-width: 210px;
  white-space: normal;
}

.annual-table strong {
  display: block;
  color: #111827;
  font-weight: 760;
  line-height: 1.25;
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
  min-height: 220px;
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
  .annual-table-grid {
    grid-template-columns: 1fr;
  }

  .annual-insights {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 960px) {
  .annual-dashboard__inner {
    padding-right: clamp(14px, 2.2vw, 28px);
    padding-bottom: 96px;
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
    height: 300px;
  }

  .annual-table {
    min-width: 560px;
  }
}
</style>
