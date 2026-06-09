<template>
  <section
    class="category-dashboard"
    :class="dashboardSizeClass"
    aria-label="Dashboard par categories"
    @wheel="onWheel"
  >
    <div class="category-dashboard__inner">
      <header class="category-header">
        <div class="category-header__copy">
          <p class="category-header__kicker">Template</p>
          <h1>Statistiques par categories</h1>
          <p>
            Analyse {{ selectedMonthLabel }} filtree sur {{ selectedCategoryLabel }}.
          </p>
        </div>

        <div class="category-controls">
          <button
            type="button"
            class="category-controls__category"
            :disabled="!categoryOptions.length && !selectedCategories.length"
            @click="openCategoryPicker"
          >
            <Tags aria-hidden="true" />
            <span>Categories</span>
            <strong>{{ selectedCategories.length || '--' }}</strong>
          </button>

          <div class="category-month">
            <div class="category-month__head">
              <span>Mois selectionne</span>
              <small>{{ periodShortLabel }}</small>
            </div>
            <div class="category-month__control">
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
        </div>
      </header>

      <div v-if="loading && !hasLoadedOnce" class="category-state category-state--loading" role="status">
        <div class="category-state__pulse"></div>
        <h2>Chargement du template</h2>
        <p>Preparation des categories et des donnees de stock.</p>
      </div>

      <div v-else-if="error" class="category-state category-state--error" role="alert">
        <h2>Impossible de charger le template</h2>
        <p>{{ error }}</p>
        <button type="button" @click="loadDataset">Reessayer</button>
      </div>

      <template v-else>
        <section v-if="!selectedCategories.length" class="category-setup" aria-label="Selection des categories">
          <div class="category-setup__head">
            <p>Configuration</p>
            <h2>Choisis les categories a analyser</h2>
            <span>
              Le template utilisera ensuite le mois selectionne pour recalculer ventes, achats,
              marge et stock.
            </span>
          </div>

          <div class="category-selector">
            <div class="category-selector__toolbar">
              <label class="category-selector__search">
                <span>Rechercher</span>
                <input v-model="categorySearch" type="search" placeholder="Pokemon, Sneakers..." />
              </label>
              <div class="category-selector__actions">
                <button type="button" @click="selectAllCategories">Tout selectionner</button>
                <button type="button" @click="clearCategoryDraft">Effacer</button>
              </div>
            </div>

            <div v-if="filteredCategoryOptions.length" class="category-selector__grid">
              <button
                v-for="option in filteredCategoryOptions"
                :key="option.value"
                type="button"
                class="category-chip"
                :class="{ 'is-active': draftCategorySet.has(normalizeCategoryKey(option.value)) }"
                @click="toggleDraftCategory(option.value)"
              >
                <span>{{ option.label }}</span>
                <strong>{{ formatNumber(option.count) }}</strong>
              </button>
            </div>

            <div v-else class="category-selector__empty" role="status">
              Aucune categorie disponible dans les donnees actuelles.
            </div>

            <div class="category-selector__footer">
              <span>{{ categoryDraft.length }} categorie(s) selectionnee(s)</span>
              <button
                type="button"
                class="category-selector__apply"
                :disabled="!categoryDraft.length"
                @click="applyCategoryDraft"
              >
                Afficher le template
              </button>
            </div>
          </div>
        </section>

        <div v-else-if="!hasPeriodData" class="category-state category-state--empty" role="status">
          <h2>Aucune donnee disponible pour {{ selectedMonthLabel }}.</h2>
          <p>
            Les categories selectionnees n'ont ni vente, ni achat, ni stock visible sur cette
            periode.
          </p>
          <button type="button" @click="openCategoryPicker">Modifier les categories</button>
        </div>

        <template v-else>
          <nav class="category-page-nav" aria-label="Navigation du dashboard categories">
            <button
              type="button"
              class="category-page-nav__arrow"
              :disabled="activePage === 0"
              aria-label="Page precedente"
              @click="previousPage"
            >
              <ChevronLeft aria-hidden="true" />
            </button>

            <div class="category-page-nav__center">
              <span class="category-page-nav__count">{{ activePage + 1 }} / {{ pages.length }}</span>
              <strong>{{ currentPage.label }}</strong>
              <div class="category-page-nav__dots" role="tablist" aria-label="Pages">
                <button
                  v-for="(page, index) in pages"
                  :key="page.key"
                  type="button"
                  class="category-page-nav__dot"
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
              class="category-page-nav__arrow"
              :disabled="activePage === pages.length - 1"
              aria-label="Page suivante"
              @click="nextPage"
            >
              <ChevronRight aria-hidden="true" />
            </button>
          </nav>

          <section
            class="category-stage"
            :class="{ 'is-dragging': pointerDrag.active }"
            aria-live="polite"
            @pointerdown="onPointerDown"
            @pointerup="onPointerUp"
            @pointercancel="resetPointerDrag"
            @lostpointercapture="resetPointerDrag"
          >
            <div class="category-pages">
              <article v-if="activePage === 0" class="category-page category-page--overview" aria-label="Vue d'ensemble">
                <div class="category-page__heading">
                  <div>
                    <p>Vue d'ensemble</p>
                    <h2>{{ selectedCategoryLabel }}</h2>
                  </div>
                  <span>{{ monthlyInsight }}</span>
                </div>

                <section class="category-kpi-grid" aria-label="KPI categories">
                  <TemplateKpiCard
                    v-for="card in kpiCards"
                    :key="card.label"
                    namespace="category"
                    :label="card.label"
                    :value="card.value"
                    :detail="card.detail"
                    :tone="card.tone"
                    :icon="card.icon"
                  />
                </section>

                <div class="category-overview-grid">
                  <section class="category-panel category-panel--hero-chart">
                    <div class="category-panel__head">
                      <div>
                        <p>{{ overviewChartKicker }}</p>
                        <h2>{{ overviewChartTitle }}</h2>
                      </div>
                      <span>{{ selectedMonthLabel }}</span>
                    </div>
                    <VChart
                      v-if="overviewChartAvailable"
                      class="category-chart"
                      :option="overviewChartOption"
                      autoresize
                    />
                    <div v-else class="category-mini-empty">{{ overviewEmptyLabel }}</div>
                  </section>

                  <section class="category-insight-panel" aria-label="Insight du mois">
                    <div class="category-insight-panel__head">
                      <Sparkles aria-hidden="true" />
                      <div>
                        <p>Insight du mois</p>
                        <h2>{{ insightTitle }}</h2>
                      </div>
                    </div>
                    <strong>{{ insightValue }}</strong>
                    <span>{{ insightDetail }}</span>
                    <div class="category-insight-panel__meta">
                      <div>
                        <small>Panier moyen</small>
                        <b>{{ formatMoney(totals.averageSalePrice) }}</b>
                      </div>
                      <div>
                        <small>Prix achat moyen</small>
                        <b>{{ formatMoney(totals.averagePurchasePrice) }}</b>
                      </div>
                    </div>
                  </section>
                </div>
              </article>

              <article v-if="activePage === 1" class="category-page category-page--analysis" aria-label="Analyse des categories">
                <div class="category-page__heading">
                  <div>
                    <p>{{ isMultiCategory ? 'Comparaison' : 'Analyse detaillee' }}</p>
                    <h2>{{ analysisTitle }}</h2>
                  </div>
                  <span>{{ analysisSubtitle }}</span>
                </div>

                <div class="category-analysis-grid">
                  <section class="category-panel category-panel--analysis-chart">
                    <div class="category-panel__head">
                      <div>
                        <p>{{ analysisChartKicker }}</p>
                        <h2>{{ analysisChartTitle }}</h2>
                      </div>
                      <span>{{ formatNumber(categoryRows.length) }} categorie(s)</span>
                    </div>
                    <VChart class="category-chart" :option="analysisChartOption" autoresize />
                  </section>

                  <section class="category-rank-panel" aria-label="Performance par categorie">
                    <article
                      v-for="item in performanceCards"
                      :key="item.label"
                      class="category-performance-card"
                      :class="item.tone ? `is-${item.tone}` : ''"
                    >
                      <span>{{ item.label }}</span>
                      <strong>{{ item.value }}</strong>
                      <p>{{ item.detail }}</p>
                    </article>
                  </section>
                </div>
              </article>

              <article v-if="activePage === 2" class="category-page category-page--details" aria-label="Details et top performances">
                <div class="category-page__heading">
                  <div>
                    <p>Details</p>
                    <h2>Top items et stock restant</h2>
                  </div>
                  <span>{{ formatNumber(soldInPeriod.length) }} vente(s) sur la periode</span>
                </div>

                <section class="category-table-grid" aria-label="Details operationnels">
                  <article class="category-panel category-table-card">
                    <div class="category-panel__head">
                      <div>
                        <p>Ventes</p>
                        <h2>Items les plus rentables</h2>
                      </div>
                      <span>{{ topSoldRows.length }} lignes</span>
                    </div>

                    <div v-if="topSoldRows.length" class="category-table-scroll">
                      <table class="category-table">
                        <thead>
                          <tr>
                            <th>Produit</th>
                            <th>Categorie</th>
                            <th>Vente</th>
                            <th>Profit</th>
                            <th>ROI</th>
                          </tr>
                        </thead>
                        <tbody>
                          <tr v-for="item in topSoldRows" :key="item.id">
                            <td>
                              <strong :title="item.name">{{ item.name }}</strong>
                              <span>{{ formatDate(item.soldAt) }}</span>
                            </td>
                            <td>{{ item.category }}</td>
                            <td>{{ formatMoney(item.salePrice) }}</td>
                            <td :class="profitClass(item.profit)">{{ formatMoney(item.profit) }}</td>
                            <td>{{ formatRatio(item.roi) }}</td>
                          </tr>
                        </tbody>
                      </table>
                    </div>

                    <div v-else class="category-mini-empty">Aucune vente sur cette periode.</div>
                  </article>

                  <article class="category-panel category-table-card">
                    <div class="category-panel__head">
                      <div>
                        <p>Stock</p>
                        <h2>Articles encore en stock</h2>
                      </div>
                      <span>Au {{ formatDate(periodRange.to) }}</span>
                    </div>

                    <div v-if="stockPreview.length" class="category-table-scroll">
                      <table class="category-table category-table--stock">
                        <thead>
                          <tr>
                            <th>Produit</th>
                            <th>Categorie</th>
                            <th>Achat</th>
                            <th>Cout</th>
                            <th>Age</th>
                          </tr>
                        </thead>
                        <tbody>
                          <tr v-for="item in stockPreview" :key="item.id">
                            <td>
                              <strong :title="item.name">{{ item.name }}</strong>
                              <span>{{ item.purchasedAt ? formatDate(item.purchasedAt) : 'Date inconnue' }}</span>
                            </td>
                            <td>{{ item.category }}</td>
                            <td>{{ item.purchasedAt ? formatDate(item.purchasedAt) : '--' }}</td>
                            <td>{{ formatMoney(item.purchasePrice) }}</td>
                            <td>
                              <span class="category-age" :class="{ 'is-old': item.ageInDays >= 90 }">
                                {{ formatDays(item.ageInDays) }}
                              </span>
                            </td>
                          </tr>
                        </tbody>
                      </table>
                    </div>

                    <div v-else class="category-mini-empty">Aucun stock restant pour ces categories.</div>
                  </article>
                </section>

                <section class="category-panel category-panel--top-chart">
                  <div class="category-panel__head">
                    <div>
                      <p>Top performance</p>
                      <h2>Profit par item vendu</h2>
                    </div>
                    <span>Top {{ topProfitChartRows.length }}</span>
                  </div>
                  <VChart
                    v-if="topProfitChartRows.length"
                    class="category-chart category-chart--compact"
                    :option="topItemsOption"
                    autoresize
                  />
                  <div v-else class="category-mini-empty">Aucun item vendu a classer.</div>
                </section>
              </article>
            </div>
          </section>
        </template>
      </template>
    </div>

    <teleport to="body">
      <div
        v-if="categoryPickerOpen"
        class="category-picker-modal"
        role="dialog"
        aria-modal="true"
        aria-label="Modifier les categories"
      >
        <div class="category-picker-backdrop" @click="closeCategoryPicker"></div>
        <div class="category-picker-panel" @click.stop>
          <div class="category-picker-head">
            <div>
              <p>Categories</p>
              <h2>Modifier la selection</h2>
            </div>
            <button type="button" aria-label="Fermer" @click="closeCategoryPicker">x</button>
          </div>

          <div class="category-selector">
            <div class="category-selector__toolbar">
              <label class="category-selector__search">
                <span>Rechercher</span>
                <input v-model="categorySearch" type="search" placeholder="Nom de categorie" />
              </label>
              <div class="category-selector__actions">
                <button type="button" @click="selectAllCategories">Tout selectionner</button>
                <button type="button" @click="clearCategoryDraft">Effacer</button>
              </div>
            </div>

            <div v-if="filteredCategoryOptions.length" class="category-selector__grid">
              <button
                v-for="option in filteredCategoryOptions"
                :key="option.value"
                type="button"
                class="category-chip"
                :class="{ 'is-active': draftCategorySet.has(normalizeCategoryKey(option.value)) }"
                @click="toggleDraftCategory(option.value)"
              >
                <span>{{ option.label }}</span>
                <strong>{{ formatNumber(option.count) }}</strong>
              </button>
            </div>

            <div v-else class="category-selector__empty" role="status">
              Aucune categorie ne correspond a la recherche.
            </div>
          </div>

          <div class="category-picker-footer">
            <span>{{ categoryDraft.length }} categorie(s) selectionnee(s)</span>
            <div>
              <button type="button" @click="closeCategoryPicker">Annuler</button>
              <button
                type="button"
                class="is-primary"
                :disabled="!categoryDraft.length"
                @click="applyCategoryDraft"
              >
                Appliquer
              </button>
            </div>
          </div>
        </div>
      </div>
    </teleport>
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
  Sparkles,
  Tags,
  TrendingUp,
  Wallet,
} from 'lucide-vue-next'
import StatsServices from '@/services/StatsServices'
import SnkVenteServices from '@/services/SnkVenteServices'
import TemplateKpiCard from '../shared/TemplateKpiCard.vue'

type CategoryTemplateState = {
  monthKey?: string
  month?: string | number
  year?: number
  categories?: string[]
}

type CategoryOption = {
  value: string
  label: string
  count: number
}

type RawItem = Record<string, unknown>

type InventoryItem = {
  id: string
  name: string
  category: string
  purchasePrice: number
  salePrice: number
  profit: number
  roi: number
  purchasedAt: string
  soldAt: string
  ageInDays: number
}

type CategoryRow = {
  category: string
  revenue: number
  profit: number
  sold: number
  bought: number
  purchaseSpend: number
  stockCount: number
  stockValue: number
  averageSalePrice: number
  marginRate: number
}

type DailyRow = {
  date: string
  revenue: number
  profit: number
  sold: number
}

const props = defineProps<{
  initialState?: CategoryTemplateState
}>()

const emit = defineEmits<{
  (event: 'state-change', state: CategoryTemplateState): void
}>()

const monthLabels = ['Jan', 'Fev', 'Mar', 'Avr', 'Mai', 'Juin', 'Juil', 'Aout', 'Sep', 'Oct', 'Nov', 'Dec']
const today = new Date()
const currentYear = today.getFullYear()
const currentMonthKey = formatMonthKey(today)
const UNKNOWN_CATEGORY = 'Autre'
const selectedMonthKey = ref(normalizeInitialMonth(props.initialState))
const selectedCategories = ref<string[]>(sanitizeCategories(props.initialState?.categories))
const categoryDraft = ref<string[]>([...selectedCategories.value])
const categorySearch = ref('')
const categoryPickerOpen = ref(false)
const activePage = ref(0)
const viewportWidth = ref(typeof window === 'undefined' ? 1440 : window.innerWidth)
const viewportHeight = ref(typeof window === 'undefined' ? 900 : window.innerHeight)
const minDate = ref('')
const maxDate = ref(formatYmd(today))
const minMonthKey = ref('')
const maxMonthKey = ref(currentMonthKey)
const loading = ref(false)
const hasLoadedOnce = ref(false)
const error = ref('')
const apiCategories = ref<string[]>([])
const rawItems = ref<RawItem[]>([])
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
  { key: 'overview', label: "Vue d'ensemble" },
  { key: 'analysis', label: 'Analyse categories' },
  { key: 'details', label: 'Details & top' },
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

const selectedYear = computed(() => Number(selectedMonthKey.value.slice(0, 4)) || currentYear)
const selectedMonthNumber = computed(() => Number(selectedMonthKey.value.slice(5, 7)) || 1)
const selectedMonthLabel = computed(() => formatMonthLong(selectedMonthKey.value))
const periodRange = computed(() => buildMonthRange(selectedMonthKey.value))
const currentPage = computed(() => pages[activePage.value] ?? pages[0])
const canGoPreviousMonth = computed(() => !minMonthKey.value || selectedMonthKey.value > minMonthKey.value)
const canGoNextMonth = computed(() => !maxMonthKey.value || selectedMonthKey.value < maxMonthKey.value)
const periodShortLabel = computed(() => {
  const start = parseYmd(periodRange.value.from)
  const end = parseYmd(periodRange.value.to)
  return `${start.getDate()}-${end.getDate()} ${monthLabels[end.getMonth()]}`
})
const dashboardSizeClass = computed(() => ({
  'is-compact-height': viewportHeight.value <= 860,
  'is-short-height': viewportHeight.value <= 740,
  'is-narrow-template': viewportWidth.value <= 1180,
}))
const detailRowLimit = computed(() => {
  const heightLimit = viewportHeight.value <= 740 ? 4 : viewportHeight.value <= 860 ? 5 : 7
  return viewportWidth.value <= 1100 ? Math.min(heightLimit, 5) : heightLimit
})
const topChartRowLimit = computed(() => (viewportHeight.value <= 740 ? 4 : 5))

const items = computed(() => rawItems.value.map(normalizeItem))
const categoryOptions = computed<CategoryOption[]>(() => {
  const map = new Map<string, CategoryOption>()

  for (const category of apiCategories.value) {
    const label = normalizeCategoryLabel(category)
    const key = normalizeCategoryKey(label)
    if (!key) continue
    map.set(key, { value: label, label, count: 0 })
  }

  for (const item of items.value) {
    const label = normalizeCategoryLabel(item.category)
    const key = normalizeCategoryKey(label)
    if (!key) continue
    const current = map.get(key) ?? { value: label, label, count: 0 }
    current.count += 1
    map.set(key, current)
  }

  for (const category of selectedCategories.value) {
    const label = normalizeCategoryLabel(category)
    const key = normalizeCategoryKey(label)
    if (!key || map.has(key)) continue
    map.set(key, { value: label, label, count: 0 })
  }

  return Array.from(map.values()).sort((a, b) =>
    a.label.localeCompare(b.label, 'fr', { sensitivity: 'base', numeric: true }),
  )
})
const filteredCategoryOptions = computed(() => {
  const search = normalizeSearchKey(categorySearch.value)
  if (!search) return categoryOptions.value
  return categoryOptions.value.filter((option) => normalizeSearchKey(option.label).includes(search))
})
const draftCategorySet = computed(() => new Set(categoryDraft.value.map(normalizeCategoryKey)))
const selectedCategorySet = computed(() => new Set(selectedCategories.value.map(normalizeCategoryKey)))
const isMultiCategory = computed(() => selectedCategories.value.length > 1)
const selectedCategoryLabel = computed(() => {
  if (!selectedCategories.value.length) return 'aucune categorie'
  if (selectedCategories.value.length === 1) return selectedCategories.value[0]
  if (selectedCategories.value.length <= 3) return selectedCategories.value.join(', ')
  return `${selectedCategories.value.length} categories`
})
const selectedItems = computed(() => {
  if (!selectedCategorySet.value.size) return []
  return items.value.filter((item) => selectedCategorySet.value.has(normalizeCategoryKey(item.category)))
})
const soldInPeriod = computed(() =>
  selectedItems.value
    .filter((item) => item.soldAt && isDateBetween(item.soldAt, periodRange.value.from, periodRange.value.to))
    .map((item) => enrichItem(item, periodRange.value.to)),
)
const purchasedInPeriod = computed(() =>
  selectedItems.value
    .filter((item) => item.purchasedAt && isDateBetween(item.purchasedAt, periodRange.value.from, periodRange.value.to))
    .map((item) => enrichItem(item, periodRange.value.to)),
)
const stockAtEnd = computed(() =>
  selectedItems.value
    .filter((item) => {
      const purchasedBeforeEnd = !item.purchasedAt || item.purchasedAt <= periodRange.value.to
      const soldAfterEnd = !item.soldAt || item.soldAt > periodRange.value.to
      return purchasedBeforeEnd && soldAfterEnd
    })
    .map((item) => enrichItem(item, periodRange.value.to)),
)
const topSoldRows = computed(() =>
  [...soldInPeriod.value].sort((a, b) => b.profit - a.profit).slice(0, detailRowLimit.value),
)
const stockPreview = computed(() =>
  [...stockAtEnd.value]
    .sort((a, b) => b.ageInDays - a.ageInDays || b.purchasePrice - a.purchasePrice)
    .slice(0, detailRowLimit.value),
)
const topProfitChartRows = computed(() => [...topSoldRows.value].slice(0, topChartRowLimit.value).reverse())
const totals = computed(() => {
  const revenue = sum(soldInPeriod.value, (item) => item.salePrice)
  const profit = sum(soldInPeriod.value, (item) => item.profit)
  const soldCost = sum(soldInPeriod.value, (item) => item.purchasePrice)
  const purchaseSpend = sum(purchasedInPeriod.value, (item) => item.purchasePrice)
  const stockValue = sum(stockAtEnd.value, (item) => item.purchasePrice)
  const itemsSold = soldInPeriod.value.length
  const itemsBought = purchasedInPeriod.value.length
  const stockCount = stockAtEnd.value.length
  return {
    revenue,
    profit,
    soldCost,
    purchaseSpend,
    stockValue,
    itemsSold,
    itemsBought,
    stockCount,
    averageSalePrice: itemsSold > 0 ? revenue / itemsSold : 0,
    averageProfit: itemsSold > 0 ? profit / itemsSold : 0,
    averagePurchasePrice: itemsBought > 0 ? purchaseSpend / itemsBought : 0,
    marginRate: revenue > 0 ? profit / revenue : 0,
    roi: soldCost > 0 ? profit / soldCost : 0,
    sellThroughRate: itemsSold + stockCount > 0 ? itemsSold / (itemsSold + stockCount) : 0,
    cashNet: revenue - purchaseSpend,
  }
})
const hasPeriodData = computed(() => {
  if (!selectedCategories.value.length) return false
  return (
    soldInPeriod.value.length > 0 ||
    purchasedInPeriod.value.length > 0 ||
    stockAtEnd.value.length > 0 ||
    totals.value.revenue !== 0 ||
    totals.value.profit !== 0 ||
    totals.value.purchaseSpend !== 0 ||
    totals.value.stockValue !== 0
  )
})
const dailyRows = computed<DailyRow[]>(() => {
  const byDate = new Map<string, DailyRow>()
  for (const day of listDays(periodRange.value.from, periodRange.value.to)) {
    byDate.set(day, { date: day, revenue: 0, profit: 0, sold: 0 })
  }
  for (const item of soldInPeriod.value) {
    const row = byDate.get(item.soldAt)
    if (!row) continue
    row.revenue += item.salePrice
    row.profit += item.profit
    row.sold += 1
  }
  return Array.from(byDate.values())
})
const categoryRows = computed<CategoryRow[]>(() =>
  selectedCategories.value.map((category) => {
    const key = normalizeCategoryKey(category)
    const sold = soldInPeriod.value.filter((item) => normalizeCategoryKey(item.category) === key)
    const bought = purchasedInPeriod.value.filter((item) => normalizeCategoryKey(item.category) === key)
    const stock = stockAtEnd.value.filter((item) => normalizeCategoryKey(item.category) === key)
    const revenue = sum(sold, (item) => item.salePrice)
    const profit = sum(sold, (item) => item.profit)
    const purchaseSpend = sum(bought, (item) => item.purchasePrice)
    const stockValue = sum(stock, (item) => item.purchasePrice)
    return {
      category,
      revenue,
      profit,
      sold: sold.length,
      bought: bought.length,
      purchaseSpend,
      stockCount: stock.length,
      stockValue,
      averageSalePrice: sold.length > 0 ? revenue / sold.length : 0,
      marginRate: revenue > 0 ? profit / revenue : 0,
    }
  }),
)
const sortedCategoryRows = computed(() =>
  [...categoryRows.value].sort((a, b) => b.profit - a.profit || b.revenue - a.revenue),
)
const bestProfitCategory = computed(() => sortedCategoryRows.value[0] ?? null)
const bestRevenueCategory = computed(() => [...categoryRows.value].sort((a, b) => b.revenue - a.revenue)[0] ?? null)
const bestVolumeCategory = computed(() => [...categoryRows.value].sort((a, b) => b.sold - a.sold)[0] ?? null)
const kpiCards = computed(() => [
  {
    label: "Chiffre d'affaires",
    value: formatMoney(totals.value.revenue),
    detail: `${formatNumber(totals.value.itemsSold)} vente(s) filtrees`,
    tone: 'primary' as const,
    icon: BadgeEuro,
  },
  {
    label: 'Benefice',
    value: formatMoney(totals.value.profit),
    detail: `${formatMoney(totals.value.averageProfit)} par vente`,
    tone: totals.value.profit >= 0 ? ('profit' as const) : ('warning' as const),
    icon: TrendingUp,
  },
  {
    label: 'Marge',
    value: formatRatio(totals.value.marginRate),
    detail: 'Profit / chiffre d affaires',
    tone: totals.value.marginRate >= 0 ? ('profit' as const) : ('warning' as const),
    icon: CirclePercent,
  },
  {
    label: 'Ventes',
    value: formatNumber(totals.value.itemsSold),
    detail: `${formatMoney(totals.value.averageSalePrice)} panier moyen`,
    tone: 'primary' as const,
    icon: ShoppingBag,
  },
  {
    label: 'Achats',
    value: formatMoney(totals.value.purchaseSpend),
    detail: `${formatNumber(totals.value.itemsBought)} item(s) achete(s)`,
    tone: 'neutral' as const,
    icon: Wallet,
  },
  {
    label: 'Stock restant',
    value: formatMoney(totals.value.stockValue),
    detail: `${formatNumber(totals.value.stockCount)} article(s) en stock`,
    tone: 'neutral' as const,
    icon: Boxes,
  },
])
const monthlyInsight = computed(() => {
  if (isMultiCategory.value && bestProfitCategory.value) {
    return `${bestProfitCategory.value.category} genere le meilleur profit: ${formatMoney(bestProfitCategory.value.profit)}.`
  }
  if (!isMultiCategory.value) {
    return `${formatNumber(totals.value.itemsSold)} vente(s), ${formatMoney(totals.value.stockValue)} de stock restant.`
  }
  return 'Ajoute des ventes ou achats pour obtenir un insight exploitable.'
})
const insightTitle = computed(() => {
  if (totals.value.cashNet >= 0 && totals.value.itemsSold > 0) return 'Cash positif'
  if (stockAtEnd.value.length > soldInPeriod.value.length && stockAtEnd.value.length >= 3) return 'Stock a suivre'
  if (bestProfitCategory.value?.category) return 'Categorie forte'
  return 'Signal faible'
})
const insightValue = computed(() => {
  if (totals.value.cashNet !== 0) return formatMoney(totals.value.cashNet)
  if (bestProfitCategory.value?.category) return bestProfitCategory.value.category
  return selectedCategoryLabel.value
})
const insightDetail = computed(() => {
  if (totals.value.cashNet >= 0 && totals.value.itemsSold > 0) {
    return 'Les ventes du mois couvrent les achats filtres sur ces categories.'
  }
  if (totals.value.cashNet < 0) {
    return 'Les achats depassent les ventes sur ce mois: surveille le stock restant.'
  }
  if (stockAtEnd.value.length) {
    return 'Aucune vente forte sur le mois, mais le stock reste visible pour la prochaine periode.'
  }
  return 'Les donnees restent limitees sur cette selection.'
})
const performanceCards = computed(() => {
  if (!isMultiCategory.value) {
    return [
      {
        label: 'Rotation',
        value: formatRatio(totals.value.sellThroughRate),
        detail: `${formatNumber(totals.value.itemsSold)} vendu(s), ${formatNumber(totals.value.stockCount)} en stock.`,
        tone: totals.value.sellThroughRate >= 0.5 ? 'positive' : 'neutral',
      },
      {
        label: 'Cash net',
        value: formatMoney(totals.value.cashNet),
        detail: 'CA du mois moins achats du mois.',
        tone: totals.value.cashNet >= 0 ? 'positive' : 'warning',
      },
      {
        label: 'ROI',
        value: formatRatio(totals.value.roi),
        detail: 'Profit sur cout des items vendus.',
        tone: totals.value.roi >= 0 ? 'positive' : 'warning',
      },
      {
        label: 'Stock',
        value: formatMoney(totals.value.stockValue),
        detail: `${formatNumber(totals.value.stockCount)} article(s) encore immobilise(s).`,
        tone: totals.value.stockCount > totals.value.itemsSold ? 'warning' : 'neutral',
      },
    ]
  }

  return [
    {
      label: 'Meilleur profit',
      value: bestProfitCategory.value?.category ?? '--',
      detail: bestProfitCategory.value ? formatMoney(bestProfitCategory.value.profit) : 'Pas de profit calcule.',
      tone: (bestProfitCategory.value?.profit ?? 0) >= 0 ? 'positive' : 'warning',
    },
    {
      label: 'Plus gros CA',
      value: bestRevenueCategory.value?.category ?? '--',
      detail: bestRevenueCategory.value ? formatMoney(bestRevenueCategory.value.revenue) : 'Pas de CA.',
      tone: 'neutral',
    },
    {
      label: 'Plus gros volume',
      value: bestVolumeCategory.value?.category ?? '--',
      detail: bestVolumeCategory.value ? `${formatNumber(bestVolumeCategory.value.sold)} vente(s)` : 'Pas de vente.',
      tone: 'neutral',
    },
    {
      label: 'Categories actives',
      value: formatNumber(categoryRows.value.filter((row) => row.sold || row.bought || row.stockCount).length),
      detail: `${formatNumber(categoryRows.value.length)} categorie(s) selectionnee(s).`,
      tone: 'positive',
    },
  ]
})
const overviewChartKicker = computed(() => (isMultiCategory.value ? 'Repartition' : 'Tendance'))
const overviewChartTitle = computed(() =>
  isMultiCategory.value ? distributionTitle.value : 'Evolution des ventes du mois',
)
const overviewChartAvailable = computed(() =>
  isMultiCategory.value ? distributionRows.value.length > 0 : dailyRows.value.some((row) => row.revenue || row.profit),
)
const overviewEmptyLabel = computed(() =>
  isMultiCategory.value
    ? 'Pas assez de valeurs pour afficher une repartition.'
    : 'Aucune vente quotidienne a afficher.',
)
const distributionRows = computed(() => {
  const revenueRows = categoryRows.value.filter((row) => row.revenue > 0)
  if (revenueRows.length) return revenueRows.map((row) => ({ label: row.category, value: row.revenue }))
  const soldRows = categoryRows.value.filter((row) => row.sold > 0)
  if (soldRows.length) return soldRows.map((row) => ({ label: row.category, value: row.sold }))
  const stockRows = categoryRows.value.filter((row) => row.stockValue > 0)
  return stockRows.map((row) => ({ label: row.category, value: row.stockValue }))
})
const distributionValueKind = computed(() => {
  if (categoryRows.value.some((row) => row.revenue > 0)) return 'money'
  if (categoryRows.value.some((row) => row.sold > 0)) return 'number'
  return 'money'
})
const distributionTitle = computed(() => {
  if (distributionValueKind.value === 'number') return 'Repartition des ventes'
  if (categoryRows.value.some((row) => row.revenue > 0)) return 'Repartition du chiffre d affaires'
  return 'Repartition de la valeur stock'
})
const overviewChartOption = computed(() => (isMultiCategory.value ? distributionOption.value : dailyPerformanceOption.value))
const distributionOption = computed(() => ({
  color: ['#4f46e5', '#059669', '#d97706', '#0284c7', '#be123c', '#64748b'],
  tooltip: {
    trigger: 'item',
    confine: true,
    formatter: (params: { name: string; value: number; percent: number }) =>
      `${params.name}<br/>${distributionValueKind.value === 'number' ? formatNumber(params.value) : formatMoney(params.value)} - ${formatNumber(params.percent)}%`,
  },
  legend: {
    bottom: 0,
    left: 'center',
    itemWidth: 10,
    itemHeight: 10,
    textStyle: { color: '#64748b', fontSize: 12 },
  },
  series: [
    {
      name: 'Categories',
      type: 'pie',
      radius: ['52%', '76%'],
      center: ['50%', '45%'],
      avoidLabelOverlap: true,
      label: {
        color: '#334155',
        formatter: '{b}',
      },
      labelLine: { lineStyle: { color: '#94a3b8' } },
      data: distributionRows.value.map((row) => ({ name: row.label, value: row.value })),
    },
  ],
}))
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
      data: dailyRows.value.map((row) => row.revenue),
      itemStyle: { borderRadius: [6, 6, 0, 0], color: 'rgba(79, 70, 229, 0.68)' },
    },
    {
      name: 'Profit',
      type: 'line',
      smooth: true,
      symbolSize: 6,
      data: dailyRows.value.map((row) => row.profit),
      lineStyle: { width: 3 },
      areaStyle: { color: 'rgba(5, 150, 105, 0.1)' },
    },
  ],
}))
const analysisTitle = computed(() =>
  isMultiCategory.value ? 'Comparer les categories selectionnees' : `Detail ${selectedCategoryLabel.value}`,
)
const analysisSubtitle = computed(() =>
  isMultiCategory.value
    ? 'CA, profit, ventes et stock par categorie.'
    : 'Ventes, achats et stock restant pour cette categorie.',
)
const analysisChartKicker = computed(() => (isMultiCategory.value ? 'CA vs profit' : 'Volume'))
const analysisChartTitle = computed(() =>
  isMultiCategory.value ? 'Performance par categorie' : 'Vendus, achetes et stock',
)
const analysisChartOption = computed(() => (isMultiCategory.value ? comparisonOption.value : stockSoldOption.value))
const comparisonOption = computed(() => {
  const rows = [...categoryRows.value].sort((a, b) => a.profit - b.profit || a.revenue - b.revenue)
  return {
    color: ['#4f46e5', '#059669'],
    grid: { left: 8, right: 18, top: 36, bottom: 10, containLabel: true },
    tooltip: {
      trigger: 'axis',
      confine: true,
      axisPointer: { type: 'shadow' },
      formatter: (params: Array<{ dataIndex: number }>) => {
        const item = rows[params?.[0]?.dataIndex ?? 0]
        if (!item) return ''
        return `${item.category}<br/>CA: ${formatMoney(item.revenue)}<br/>Profit: ${formatMoney(item.profit)}<br/>Ventes: ${formatNumber(item.sold)}`
      },
    },
    legend: {
      top: 0,
      right: 2,
      itemWidth: 10,
      itemHeight: 10,
      textStyle: { color: '#64748b', fontSize: 12 },
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
      data: rows.map((item) => item.category),
      axisTick: { show: false },
      axisLine: { show: false },
      axisLabel: { color: '#334155', width: 110, overflow: 'truncate' },
    },
    series: [
      {
        name: 'CA',
        type: 'bar',
        barMaxWidth: 24,
        data: rows.map((item) => item.revenue),
        itemStyle: { borderRadius: [0, 6, 6, 0], color: 'rgba(79, 70, 229, 0.7)' },
      },
      {
        name: 'Profit',
        type: 'bar',
        barMaxWidth: 24,
        data: rows.map((item) => item.profit),
        itemStyle: {
          borderRadius: [0, 6, 6, 0],
          color: (params: { value: number }) => (Number(params.value ?? 0) >= 0 ? '#10b981' : '#f97316'),
        },
      },
    ],
  }
})
const stockSoldOption = computed(() => ({
  color: ['#4f46e5', '#d97706', '#059669'],
  grid: { left: 8, right: 12, top: 12, bottom: 10, containLabel: true },
  tooltip: {
    trigger: 'axis',
    confine: true,
    axisPointer: { type: 'shadow' },
  },
  xAxis: {
    type: 'category',
    data: ['Vendus', 'Achetes', 'Stock'],
    axisTick: { show: false },
    axisLine: { lineStyle: { color: '#cbd5e1' } },
    axisLabel: { color: '#64748b' },
  },
  yAxis: {
    type: 'value',
    minInterval: 1,
    splitLine: { lineStyle: { color: 'rgba(148, 163, 184, 0.22)' } },
    axisLabel: { color: '#64748b' },
  },
  series: [
    {
      name: 'Articles',
      type: 'bar',
      barMaxWidth: 46,
      data: [
        { value: totals.value.itemsSold, itemStyle: { color: '#4f46e5' } },
        { value: totals.value.itemsBought, itemStyle: { color: '#d97706' } },
        { value: totals.value.stockCount, itemStyle: { color: '#059669' } },
      ],
      itemStyle: { borderRadius: [7, 7, 0, 0] },
    },
  ],
}))
const topItemsOption = computed(() => ({
  grid: { left: 8, right: 18, top: 8, bottom: 8, containLabel: true },
  tooltip: {
    trigger: 'axis',
    confine: true,
    axisPointer: { type: 'shadow' },
    formatter: (params: Array<{ dataIndex: number }>) => {
      const item = topProfitChartRows.value[params?.[0]?.dataIndex ?? 0]
      return item ? `${item.name}<br/>Profit: ${formatMoney(item.profit)}<br/>Categorie: ${item.category}` : ''
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
    data: topProfitChartRows.value.map((item) => item.name),
    axisTick: { show: false },
    axisLine: { show: false },
    axisLabel: { color: '#334155', width: 160, overflow: 'truncate' },
  },
  series: [
    {
      name: 'Profit',
      type: 'bar',
      barMaxWidth: 22,
      data: topProfitChartRows.value.map((item) => item.profit),
      itemStyle: {
        borderRadius: [0, 6, 6, 0],
        color: (params: { value: number }) => (Number(params.value ?? 0) >= 0 ? '#059669' : '#f97316'),
      },
    },
  ],
}))

watch(
  () => props.initialState,
  (state) => {
    const nextMonth = clampMonthKey(normalizeInitialMonth(state))
    if (selectedMonthKey.value !== nextMonth) selectedMonthKey.value = nextMonth
    const nextCategories = sanitizeCategories(state?.categories)
    if (!sameCategories(selectedCategories.value, nextCategories)) {
      selectedCategories.value = nextCategories
      categoryDraft.value = [...nextCategories]
    }
  },
  { deep: true },
)

watch(
  [selectedMonthKey, selectedCategories],
  () => {
    emit('state-change', {
      monthKey: selectedMonthKey.value,
      month: selectedMonthNumber.value,
      year: selectedYear.value,
      categories: [...selectedCategories.value],
    })
  },
  { deep: true, immediate: true },
)

function normalizeInitialMonth(state?: CategoryTemplateState) {
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

function formatYmd(date: Date) {
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}

function pad(value: number) {
  return String(value).padStart(2, '0')
}

function toDateKey(value: unknown) {
  if (!value) return ''
  if (value instanceof Date && !Number.isNaN(value.getTime())) return formatYmd(value)
  const text = String(value).trim()
  const match = text.match(/^(\d{4})-(\d{2})-(\d{2})/)
  if (match) return `${match[1]}-${match[2]}-${match[3]}`
  return ''
}

function isDateBetween(value: string, from: string, to: string) {
  return value >= from && value <= to
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

function daysBetween(from: string, to: string) {
  if (!from || !to) return 0
  const start = parseYmd(from)
  const end = parseYmd(to)
  if (Number.isNaN(start.getTime()) || Number.isNaN(end.getTime())) return 0
  return Math.max(0, Math.round((end.getTime() - start.getTime()) / 86400000))
}

function readField(row: RawItem, camel: string) {
  if (row[camel] != null) return row[camel]
  const snake = camel.replace(/[A-Z]/g, (match) => `_${match.toLowerCase()}`)
  return row[snake]
}

function normalizeItem(row: RawItem): InventoryItem {
  const purchasePrice = toNumber(readField(row, 'prixRetail'))
  const salePrice = toNumber(readField(row, 'prixResell'))
  const purchasedAt = toDateKey(readField(row, 'dateAchat'))
  const soldAt = toDateKey(readField(row, 'dateVente'))
  const profit = salePrice - purchasePrice
  return {
    id: String(readField(row, 'id') ?? `${readField(row, 'nomItem') ?? 'item'}-${purchasedAt}-${soldAt}`),
    name: normalizeItemName(readField(row, 'nomItem')),
    category: normalizeCategoryLabel(readField(row, 'categorie')),
    purchasePrice,
    salePrice,
    profit,
    roi: purchasePrice > 0 ? profit / purchasePrice : 0,
    purchasedAt,
    soldAt,
    ageInDays: 0,
  }
}

function enrichItem(item: InventoryItem, asOf: string): InventoryItem {
  return {
    ...item,
    ageInDays: item.purchasedAt ? daysBetween(item.purchasedAt, asOf) : 0,
  }
}

function normalizeItemName(value: unknown) {
  const text = String(value ?? '').trim()
  return text || 'Article sans nom'
}

function normalizeCategoryLabel(value: unknown) {
  const text = String(value ?? '').trim()
  return text || UNKNOWN_CATEGORY
}

function normalizeCategoryKey(value: unknown) {
  return normalizeCategoryLabel(value).toLowerCase()
}

function normalizeSearchKey(value: unknown) {
  return String(value ?? '').trim().toLowerCase()
}

function sanitizeCategories(value: unknown) {
  if (!Array.isArray(value)) return []
  const map = new Map<string, string>()
  for (const entry of value) {
    if (!String(entry ?? '').trim()) continue
    const label = normalizeCategoryLabel(entry)
    const key = normalizeCategoryKey(label)
    if (!key) continue
    map.set(key, label)
  }
  return Array.from(map.values())
}

function sameCategories(a: string[], b: string[]) {
  if (a.length !== b.length) return false
  const keys = new Set(a.map(normalizeCategoryKey))
  return b.every((item) => keys.has(normalizeCategoryKey(item)))
}

function toNumber(value: unknown, fallback = 0) {
  const next = Number(value ?? fallback)
  return Number.isFinite(next) ? next : fallback
}

function sum<T>(list: T[], getter: (item: T) => number) {
  return list.reduce((total, item) => total + getter(item), 0)
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

function formatDate(value: unknown) {
  const key = toDateKey(value)
  if (!key) return '--'
  const date = parseYmd(key)
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

function formatDays(value: unknown) {
  const days = Math.max(0, Math.round(toNumber(value)))
  if (days <= 0) return '--'
  return `${formatNumber(days)} j`
}

function profitClass(value: unknown) {
  return toNumber(value) >= 0 ? 'is-positive' : 'is-negative'
}

function updateViewportSize() {
  if (typeof window === 'undefined') return
  viewportWidth.value = window.innerWidth
  viewportHeight.value = window.innerHeight
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

function openCategoryPicker() {
  categoryDraft.value = [...selectedCategories.value]
  categorySearch.value = ''
  categoryPickerOpen.value = true
}

function closeCategoryPicker() {
  categoryPickerOpen.value = false
}

function toggleDraftCategory(category: string) {
  const key = normalizeCategoryKey(category)
  const next = new Map(categoryDraft.value.map((item) => [normalizeCategoryKey(item), item]))
  if (next.has(key)) next.delete(key)
  else next.set(key, normalizeCategoryLabel(category))
  categoryDraft.value = Array.from(next.values())
}

function selectAllCategories() {
  categoryDraft.value = categoryOptions.value.map((option) => option.value)
}

function clearCategoryDraft() {
  categoryDraft.value = []
}

function applyCategoryDraft() {
  if (!categoryDraft.value.length) return
  const selectedKeys = new Set(categoryDraft.value.map(normalizeCategoryKey))
  const ordered = categoryOptions.value
    .filter((option) => selectedKeys.has(normalizeCategoryKey(option.value)))
    .map((option) => option.value)
  const unknown = categoryDraft.value.filter(
    (category) => !ordered.some((item) => normalizeCategoryKey(item) === normalizeCategoryKey(category)),
  )
  selectedCategories.value = sanitizeCategories([...ordered, ...unknown])
  categoryDraft.value = [...selectedCategories.value]
  categoryPickerOpen.value = false
  activePage.value = 0
}

function canStartPointerSlide(target: EventTarget | null) {
  if (!(target instanceof HTMLElement)) return true
  return !target.closest('button, input, select, textarea, a, [contenteditable="true"], .category-picker-modal')
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
  if (categoryPickerOpen.value) {
    if (event.key === 'Escape') closeCategoryPicker()
    return
  }
  if (target?.closest('input, select, textarea, [contenteditable="true"]')) return
  if (event.key === 'ArrowLeft') {
    event.preventDefault()
    previousPage()
  } else if (event.key === 'ArrowRight') {
    event.preventDefault()
    nextPage()
  }
}

async function loadDataset() {
  const id = ++requestId
  loading.value = true
  error.value = ''
  try {
    const [boundsResult, categoriesResult, itemsResult] = await Promise.allSettled([
      StatsServices.dateBounds(),
      StatsServices.categories(),
      SnkVenteServices.getSnkVente(),
    ])

    if (id !== requestId) return

    if (boundsResult.status === 'fulfilled') {
      const data = boundsResult.value?.data
      minDate.value = typeof data?.minDate === 'string' ? data.minDate : ''
      maxDate.value = typeof data?.maxDate === 'string' ? data.maxDate : formatYmd(today)
      minMonthKey.value = minDate.value ? monthKeyFromYmd(minDate.value) : ''
      maxMonthKey.value = maxDate.value ? monthKeyFromYmd(maxDate.value) : currentMonthKey
      selectedMonthKey.value = clampMonthKey(selectedMonthKey.value)
    }

    if (categoriesResult.status === 'fulfilled') {
      const raw = Array.isArray(categoriesResult.value?.data) ? categoriesResult.value.data : []
      apiCategories.value = sanitizeCategories(raw)
    } else {
      apiCategories.value = []
    }

    if (itemsResult.status === 'fulfilled') {
      rawItems.value = Array.isArray(itemsResult.value?.data) ? itemsResult.value.data : []
    } else {
      rawItems.value = []
    }

    selectedMonthKey.value = clampMonthKey(selectedMonthKey.value)

    if (itemsResult.status === 'rejected' && categoriesResult.status === 'rejected') {
      throw itemsResult.reason
    }

    hasLoadedOnce.value = true
  } catch (err: unknown) {
    if (id !== requestId) return
    error.value = getErrorMessage(err)
  } finally {
    if (id === requestId) loading.value = false
  }
}

function getErrorMessage(err: unknown) {
  if (err && typeof err === 'object') {
    const record = err as { response?: { data?: { message?: unknown } }; message?: unknown }
    if (typeof record.response?.data?.message === 'string') return record.response.data.message
    if (typeof record.message === 'string') return record.message
  }
  return 'Erreur inconnue'
}

onMounted(async () => {
  updateViewportSize()
  window.addEventListener('resize', updateViewportSize, { passive: true })
  window.addEventListener('keydown', onKeyDown)
  await loadDataset()
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', updateViewportSize)
  window.removeEventListener('keydown', onKeyDown)
})
</script>

<style scoped>
.category-dashboard {
  --category-template-gap: clamp(10px, 1.35vh, 16px);
  --category-bg: #f7f4ee;
  --category-muted-bg: #fbfaf6;
  width: 100%;
  height: 100%;
  min-width: 0;
  min-height: 0;
  overflow: auto;
  overscroll-behavior: contain;
  overscroll-behavior-x: none;
  scrollbar-width: thin;
  scrollbar-color: rgba(100, 116, 139, 0.34) rgba(234, 229, 219, 0.9);
  background: var(--category-bg);
}

.category-dashboard::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}

.category-dashboard::-webkit-scrollbar-track {
  background: rgba(234, 229, 219, 0.9);
}

.category-dashboard::-webkit-scrollbar-thumb {
  border-radius: 999px;
  background: rgba(100, 116, 139, 0.34);
}

.category-dashboard__inner {
  width: min(1680px, 100%);
  min-height: 100%;
  min-width: 0;
  margin: 0 auto;
  padding: clamp(14px, 1.8vw, 24px) clamp(14px, 2.2vw, 28px) clamp(16px, 2vw, 24px)
    calc(96px + clamp(14px, 2.2vw, 28px));
  display: grid;
  grid-template-rows: auto auto auto;
  gap: var(--category-template-gap);
  align-content: start;
}

.category-header {
  min-width: 0;
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 18px;
}

.category-header__copy {
  min-width: 0;
}

.category-header__kicker {
  margin: 0 0 7px;
  color: #5b5ce2;
  font-size: 0.72rem;
  font-weight: 820;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

.category-header h1 {
  margin: 0;
  color: #111827;
  font-size: clamp(1.62rem, 2.8vw, 2.45rem);
  line-height: 1.02;
  font-weight: 860;
}

.category-header p {
  margin: 7px 0 0;
  max-width: 72ch;
  color: #64748b;
  font-size: 0.96rem;
  line-height: 1.45;
}

.category-controls {
  flex: 0 0 auto;
  display: grid;
  grid-template-columns: repeat(2, minmax(260px, 315px));
  gap: 10px;
  align-items: stretch;
}

.category-controls__category,
.category-month {
  border: 1px solid rgba(99, 102, 241, 0.2);
  border-radius: 12px;
  background: #ffffff;
  box-shadow:
    0 14px 34px rgba(31, 41, 55, 0.1),
    inset 0 1px 0 rgba(255, 255, 255, 0.8);
}

.category-controls__category {
  width: 100%;
  min-width: 0;
  min-height: 46px;
  box-sizing: border-box;
  padding: 7px 9px 7px 11px;
  color: #4f46e5;
  display: inline-flex;
  gap: 10px;
  align-items: center;
  font-size: 0.88rem;
  font-weight: 840;
  text-align: left;
  align-self: stretch;
  justify-self: stretch;
  cursor: pointer;
  transition:
    border-color 160ms ease,
    box-shadow 160ms ease,
    transform 160ms ease,
    background 160ms ease;
}

.category-controls__category svg {
  width: 18px;
  height: 18px;
  flex: 0 0 auto;
  box-sizing: content-box;
  padding: 5px;
  border-radius: 10px;
  background: rgba(79, 70, 229, 0.1);
  color: #4f46e5;
  stroke-width: 2.15;
}

.category-controls__category span {
  flex: 1 1 auto;
  min-width: 0;
  white-space: nowrap;
}

.category-controls__category strong {
  min-width: 30px;
  height: 30px;
  box-sizing: border-box;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  background: linear-gradient(135deg, #5b5ce2, #4338ca);
  color: #ffffff;
  padding: 0 9px;
  box-shadow: 0 8px 18px rgba(79, 70, 229, 0.22);
  font-size: 0.78rem;
  line-height: 1;
  text-align: center;
}

.category-controls__category:not(:disabled):hover {
  border-color: rgba(79, 70, 229, 0.38);
  background: var(--category-muted-bg);
  box-shadow:
    0 16px 34px rgba(31, 41, 55, 0.12),
    0 0 0 3px rgba(79, 70, 229, 0.08),
    inset 0 1px 0 rgba(255, 255, 255, 0.9);
  transform: translateY(-1px);
}

.category-controls__category:focus-visible {
  outline: none;
  border-color: rgba(79, 70, 229, 0.5);
  box-shadow:
    0 14px 34px rgba(31, 41, 55, 0.1),
    0 0 0 3px rgba(79, 70, 229, 0.16);
}

.category-controls__category:disabled {
  opacity: 0.52;
  pointer-events: none;
}

.category-month {
  width: 100%;
  box-sizing: border-box;
  padding: 10px;
  display: grid;
  gap: 8px;
}

.category-month__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.category-month__head span {
  color: #64748b;
  font-size: 0.68rem;
  font-weight: 780;
  letter-spacing: 0.11em;
  text-transform: uppercase;
}

.category-month__head small {
  color: #5b5ce2;
  font-size: 0.68rem;
  font-weight: 760;
}

.category-month__control {
  display: grid;
  grid-template-columns: 34px minmax(0, 1fr) 34px;
  align-items: center;
  gap: 6px;
}

.category-month__control button {
  width: 34px;
  height: 36px;
  border: 1px solid rgba(99, 102, 241, 0.22);
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.8);
  color: #4f46e5;
  display: inline-grid;
  place-items: center;
}

.category-month__control button:disabled {
  opacity: 0.36;
  pointer-events: none;
}

.category-month__control svg {
  width: 16px;
  height: 16px;
}

.category-month__control input {
  width: 100%;
  min-width: 0;
  height: 38px;
  border: 1px solid rgba(99, 102, 241, 0.26);
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.96);
  color: #111827;
  font-size: 1rem;
  font-weight: 820;
  text-align: center;
  padding: 0 8px;
  outline: none;
}

.category-page-nav {
  min-width: 0;
  border: 1px solid rgba(148, 163, 184, 0.24);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.6);
  box-shadow: 0 10px 24px rgba(31, 41, 55, 0.06);
  padding: 7px 9px;
  display: grid;
  grid-template-columns: 40px minmax(0, 1fr) 40px;
  align-items: center;
  gap: 10px;
}

.category-page-nav__arrow {
  width: 36px;
  height: 36px;
  border: 1px solid rgba(99, 102, 241, 0.22);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.78);
  color: #4338ca;
  display: grid;
  place-items: center;
}

.category-page-nav__arrow svg {
  width: 18px;
  height: 18px;
}

.category-page-nav__arrow:disabled {
  opacity: 0.36;
  pointer-events: none;
}

.category-page-nav__center {
  min-width: 0;
  display: grid;
  grid-template-columns: auto minmax(0, auto) auto;
  justify-content: center;
  align-items: center;
  gap: 10px;
}

.category-page-nav__count {
  color: #64748b;
  font-size: 0.72rem;
  font-weight: 780;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.category-page-nav__center strong {
  color: #111827;
  font-size: 0.92rem;
  font-weight: 800;
  white-space: nowrap;
}

.category-page-nav__dots {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.category-page-nav__dot {
  width: 7px;
  height: 7px;
  border: 0;
  border-radius: 999px;
  background: #cbd5e1;
  padding: 0;
}

.category-page-nav__dot.is-active {
  width: 22px;
  background: #5b5ce2;
}

.category-stage {
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

.category-stage.is-dragging {
  cursor: grabbing;
}

.category-pages {
  width: 100%;
  height: auto;
  min-height: 0;
  display: block;
}

.category-page {
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

.category-page__heading {
  min-width: 0;
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 12px;
}

.category-page__heading p {
  margin: 0 0 4px;
  color: #5b5ce2;
  font-size: 0.68rem;
  font-weight: 820;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

.category-page__heading h2 {
  margin: 0;
  color: #111827;
  font-size: clamp(1.08rem, 1.5vw, 1.28rem);
  line-height: 1.15;
  font-weight: 820;
}

.category-page__heading span {
  max-width: 58ch;
  color: #64748b;
  font-size: 0.86rem;
  line-height: 1.35;
  text-align: right;
}

.category-kpi-grid {
  min-width: 0;
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(142px, 1fr));
  gap: 12px;
}

.category-kpi-grid :deep(.category-kpi) {
  padding: clamp(11px, 1.2vw, 14px);
  gap: 7px;
}

.category-kpi-grid :deep(.category-kpi__value) {
  font-size: clamp(1.28rem, 2vw, 1.86rem);
}

.category-kpi-grid :deep(.category-kpi__detail) {
  font-size: 0.78rem;
}

.category-overview-grid,
.category-analysis-grid,
.category-table-grid {
  min-width: 0;
  min-height: 0;
  display: grid;
  gap: 14px;
  align-items: stretch;
}

.category-overview-grid {
  grid-template-columns: minmax(0, 1.35fr) minmax(300px, 0.65fr);
}

.category-analysis-grid {
  grid-template-columns: minmax(0, 1fr) minmax(300px, 0.52fr);
}

.category-table-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.category-panel {
  min-width: 0;
  min-height: 0;
  height: auto;
  border: 1px solid rgba(148, 163, 184, 0.28);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 14px 34px rgba(31, 41, 55, 0.08);
  padding: clamp(14px, 1.8vw, 18px);
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  gap: 12px;
}

.category-panel--hero-chart,
.category-panel--analysis-chart {
  min-height: clamp(330px, 42vh, 520px);
}

.category-panel--top-chart {
  min-height: clamp(230px, 28vh, 360px);
}

.category-panel__head {
  min-width: 0;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.category-panel__head div {
  min-width: 0;
}

.category-panel__head p {
  margin: 0 0 4px;
  color: #5b5ce2;
  font-size: 0.7rem;
  font-weight: 820;
  letter-spacing: 0.13em;
  text-transform: uppercase;
}

.category-panel__head h2 {
  margin: 0;
  color: #111827;
  font-size: clamp(1rem, 1.4vw, 1.24rem);
  line-height: 1.15;
  font-weight: 790;
}

.category-panel__head span {
  flex: 0 0 auto;
  max-width: 45%;
  border-radius: 999px;
  background: var(--category-muted-bg);
  color: #4338ca;
  padding: 6px 10px;
  font-size: 0.74rem;
  font-weight: 760;
  overflow-wrap: anywhere;
  text-align: right;
}

.category-chart {
  width: 100%;
  height: 100%;
  min-width: 0;
  min-height: 0;
}

.category-chart--compact {
  min-height: 210px;
}

.category-insight-panel,
.category-rank-panel {
  min-width: 0;
  border: 1px solid rgba(148, 163, 184, 0.28);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.84);
  box-shadow: 0 14px 34px rgba(31, 41, 55, 0.08);
  padding: 14px;
}

.category-insight-panel {
  display: grid;
  align-content: start;
  gap: 12px;
}

.category-insight-panel__head {
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 10px;
  align-items: center;
}

.category-insight-panel__head svg {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background: var(--category-muted-bg);
  color: #4f46e5;
  padding: 8px;
}

.category-insight-panel__head p {
  margin: 0 0 3px;
  color: #5b5ce2;
  font-size: 0.68rem;
  font-weight: 820;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.category-insight-panel__head h2 {
  margin: 0;
  color: #111827;
  font-size: 1.05rem;
  font-weight: 820;
}

.category-insight-panel > strong {
  color: #111827;
  font-size: clamp(1.45rem, 2.3vw, 2rem);
  line-height: 1.05;
  overflow-wrap: anywhere;
}

.category-insight-panel > span {
  color: #64748b;
  font-size: 0.9rem;
  line-height: 1.45;
}

.category-insight-panel__meta {
  display: grid;
  gap: 8px;
}

.category-insight-panel__meta div,
.category-performance-card {
  min-width: 0;
  border: 1px solid rgba(148, 163, 184, 0.22);
  border-radius: 8px;
  background: var(--category-muted-bg);
  padding: 10px;
  display: grid;
  gap: 4px;
}

.category-insight-panel__meta small,
.category-performance-card span {
  color: #64748b;
  font-size: 0.66rem;
  font-weight: 820;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.category-insight-panel__meta b,
.category-performance-card strong {
  color: #111827;
  font-size: 1.05rem;
  line-height: 1.1;
  overflow-wrap: anywhere;
}

.category-rank-panel {
  display: grid;
  grid-auto-rows: minmax(92px, auto);
  gap: 9px;
}

.category-performance-card p {
  margin: 0;
  color: #64748b;
  font-size: 0.76rem;
  line-height: 1.32;
}

.category-performance-card.is-positive strong {
  color: #047857;
}

.category-performance-card.is-warning strong {
  color: #b45309;
}

.category-table-card {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  align-content: start;
  overflow: hidden;
}

.category-table-scroll {
  min-width: 0;
  min-height: 0;
  max-height: clamp(280px, 38vh, 430px);
  overflow-y: auto;
  overflow-x: hidden;
  overscroll-behavior: contain;
  scrollbar-width: thin;
  scrollbar-color: rgba(91, 92, 226, 0.32) rgba(226, 232, 240, 0.58);
}

.category-table-scroll::-webkit-scrollbar {
  width: 7px;
}

.category-table-scroll::-webkit-scrollbar-track {
  background: rgba(226, 232, 240, 0.58);
}

.category-table-scroll::-webkit-scrollbar-thumb {
  border-radius: 999px;
  background: rgba(91, 92, 226, 0.34);
}

.category-table {
  width: 100%;
  min-width: 0;
  border-collapse: collapse;
  table-layout: fixed;
}

.category-table th {
  position: sticky;
  top: 0;
  z-index: 1;
  color: #64748b;
  background: rgba(255, 255, 255, 0.96);
  font-size: 0.68rem;
  font-weight: 780;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  text-align: right;
  padding: 8px 7px;
  border-bottom: 1px solid rgba(148, 163, 184, 0.24);
  white-space: nowrap;
}

.category-table th:first-child,
.category-table td:first-child {
  text-align: left;
}

.category-table td {
  color: #334155;
  font-size: 0.76rem;
  text-align: right;
  padding: 8px 7px;
  border-bottom: 1px solid rgba(148, 163, 184, 0.16);
  vertical-align: top;
  white-space: nowrap;
}

.category-table td:first-child {
  white-space: normal;
}

.category-table strong {
  display: block;
  color: #111827;
  font-weight: 760;
  line-height: 1.22;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.category-table td span {
  display: block;
  margin-top: 3px;
  color: #64748b;
  font-size: 0.7rem;
}

.category-table .is-positive {
  color: #047857;
  font-weight: 790;
}

.category-table .is-negative {
  color: #dc2626;
  font-weight: 790;
}

.category-age {
  display: inline-flex !important;
  width: fit-content;
  margin-left: auto;
  border-radius: 999px;
  background: var(--category-muted-bg);
  color: #4338ca !important;
  padding: 4px 8px;
  font-weight: 760;
}

.category-age.is-old {
  background: #fff7ed;
  color: #c2410c !important;
}

.category-state,
.category-setup {
  min-height: 360px;
  border: 1px dashed rgba(148, 163, 184, 0.42);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.84);
  padding: clamp(22px, 4vw, 36px);
  display: grid;
  place-items: center;
  align-content: center;
  text-align: center;
  gap: 14px;
}

.category-state h2,
.category-setup__head h2 {
  margin: 0;
  color: #111827;
  font-size: clamp(1.18rem, 2vw, 1.6rem);
  line-height: 1.15;
  font-weight: 800;
}

.category-state p,
.category-setup__head span {
  margin: 0;
  max-width: 64ch;
  color: #64748b;
  line-height: 1.5;
}

.category-state button {
  min-height: 40px;
  border: 1px solid rgba(99, 102, 241, 0.32);
  border-radius: 8px;
  background: #4f46e5;
  color: #fff;
  padding: 0 14px;
  font-weight: 760;
}

.category-state__pulse {
  width: 46px;
  height: 46px;
  border-radius: 999px;
  border: 4px solid rgba(99, 102, 241, 0.16);
  border-top-color: #5b5ce2;
  animation: category-spin 850ms linear infinite;
}

.category-setup {
  place-items: stretch;
  text-align: left;
}

.category-setup__head {
  display: grid;
  gap: 7px;
}

.category-setup__head p {
  margin: 0;
  color: #5b5ce2;
  font-size: 0.72rem;
  font-weight: 820;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

.category-selector {
  width: 100%;
  display: grid;
  gap: 12px;
}

.category-selector__toolbar {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 10px;
  align-items: end;
}

.category-selector__search {
  display: grid;
  gap: 5px;
}

.category-selector__search span {
  color: #64748b;
  font-size: 0.68rem;
  font-weight: 800;
  letter-spacing: 0.1em;
  text-transform: uppercase;
}

.category-selector__search input {
  width: 100%;
  min-height: 40px;
  border: 1px solid rgba(148, 163, 184, 0.34);
  border-radius: 8px;
  background: #fff;
  color: #111827;
  padding: 0 12px;
  outline: none;
}

.category-selector__search input:focus {
  border-color: rgba(91, 92, 226, 0.56);
  box-shadow: 0 0 0 3px rgba(91, 92, 226, 0.12);
}

.category-selector__actions {
  display: flex;
  gap: 7px;
}

.category-selector__actions button,
.category-picker-footer button {
  min-height: 38px;
  border: 1px solid rgba(148, 163, 184, 0.34);
  border-radius: 8px;
  background: var(--category-muted-bg);
  color: #334155;
  padding: 0 12px;
  font-size: 0.78rem;
  font-weight: 760;
}

.category-selector__grid {
  max-height: min(320px, 42vh);
  overflow-y: auto;
  padding-right: 2px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.category-chip {
  min-height: 38px;
  border: 1px solid rgba(148, 163, 184, 0.3);
  border-radius: 999px;
  background: var(--category-muted-bg);
  color: #334155;
  padding: 7px 10px;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 0.8rem;
  font-weight: 760;
}

.category-chip strong {
  min-width: 24px;
  border-radius: 999px;
  background: #e2e8f0;
  color: #475569;
  padding: 3px 6px;
  font-size: 0.68rem;
  text-align: center;
}

.category-chip.is-active {
  border-color: rgba(79, 70, 229, 0.48);
  background: var(--category-muted-bg);
  color: #3730a3;
}

.category-chip.is-active strong {
  background: #4f46e5;
  color: #fff;
}

.category-selector__empty,
.category-mini-empty {
  min-height: clamp(130px, 18vh, 220px);
  height: auto;
  border: 1px dashed rgba(148, 163, 184, 0.34);
  border-radius: 8px;
  background: var(--category-muted-bg);
  color: #64748b;
  display: grid;
  place-items: center;
  padding: 18px;
  text-align: center;
  line-height: 1.45;
}

.category-selector__footer {
  border-top: 1px solid rgba(148, 163, 184, 0.22);
  padding-top: 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.category-selector__footer span,
.category-picker-footer span {
  color: #64748b;
  font-size: 0.82rem;
  font-weight: 700;
}

.category-selector__apply,
.category-picker-footer .is-primary {
  border-color: rgba(79, 70, 229, 0.4) !important;
  background: #4f46e5 !important;
  color: #fff !important;
}

.category-selector__apply:disabled,
.category-picker-footer .is-primary:disabled {
  opacity: 0.45;
  pointer-events: none;
}

.category-picker-modal {
  position: fixed;
  inset: 0;
  z-index: 190;
  display: grid;
  place-items: center;
  padding: 18px;
}

.category-picker-backdrop {
  position: absolute;
  inset: 0;
  background: rgba(15, 23, 42, 0.56);
  backdrop-filter: blur(4px);
}

.category-picker-panel {
  position: relative;
  width: min(640px, 100%);
  max-height: min(760px, calc(100dvh - 28px));
  overflow: auto;
  border: 1px solid rgba(148, 163, 184, 0.34);
  border-radius: 12px;
  background: #ffffff;
  box-shadow: 0 26px 70px rgba(2, 6, 23, 0.36);
  padding: 16px;
  display: grid;
  gap: 14px;
}

.category-picker-head,
.category-picker-footer {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 10px;
}

.category-picker-head p {
  margin: 0 0 4px;
  color: #5b5ce2;
  font-size: 0.68rem;
  font-weight: 820;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

.category-picker-head h2 {
  margin: 0;
  color: #111827;
  font-size: 1.22rem;
  line-height: 1.14;
  font-weight: 820;
}

.category-picker-head > button {
  width: 32px;
  height: 32px;
  border: 1px solid rgba(148, 163, 184, 0.34);
  border-radius: 8px;
  background: var(--category-muted-bg);
  color: #334155;
  font-weight: 760;
}

.category-picker-footer {
  align-items: center;
  border-top: 1px solid rgba(148, 163, 184, 0.22);
  padding-top: 12px;
}

.category-picker-footer div {
  display: flex;
  gap: 8px;
}

@keyframes category-spin {
  to {
    transform: rotate(360deg);
  }
}

@media (min-width: 961px) {
  .category-dashboard {
    overflow: hidden;
  }

  .category-dashboard__inner {
    height: 100%;
    min-height: 0;
    padding-top: clamp(10px, 1.4vh, 18px);
    padding-right: clamp(12px, 1.8vw, 24px);
    padding-bottom: clamp(10px, 1.4vh, 18px);
    padding-left: calc(88px + clamp(12px, 1.8vw, 24px));
    grid-template-rows: auto auto auto;
    gap: clamp(7px, 1.05vh, 12px);
    align-content: stretch;
  }

  .category-header {
    align-items: center;
    gap: 12px;
  }

  .category-header__kicker {
    margin-bottom: 4px;
  }

  .category-header h1 {
    font-size: clamp(1.38rem, 2.15vw, 2.05rem);
  }

  .category-header p {
    margin-top: 4px;
    max-width: 78ch;
    font-size: 0.86rem;
    line-height: 1.32;
    overflow: hidden;
    display: -webkit-box;
    -webkit-line-clamp: 1;
    -webkit-box-orient: vertical;
  }

  .category-controls {
    grid-template-columns: repeat(2, minmax(238px, 292px));
    gap: 8px;
  }

  .category-controls__category {
    min-width: 0;
    min-height: 42px;
    padding: 6px 8px 6px 10px;
    gap: 8px;
    font-size: 0.82rem;
  }

  .category-controls__category svg {
    width: 16px;
    height: 16px;
    padding: 5px;
  }

  .category-controls__category strong {
    min-width: 28px;
    height: 28px;
    padding: 0 8px;
    font-size: 0.74rem;
  }

  .category-month {
    padding: 8px;
    gap: 6px;
  }

  .category-month__control {
    grid-template-columns: 32px minmax(0, 1fr) 32px;
  }

  .category-month__control button {
    width: 32px;
    height: 34px;
  }

  .category-month__control input {
    height: 34px;
    font-size: 0.92rem;
  }

  .category-page-nav {
    grid-template-columns: 36px minmax(0, 1fr) 36px;
    padding: 5px 7px;
  }

  .category-page-nav__arrow {
    width: 32px;
    height: 32px;
  }

  .category-stage,
  .category-pages {
    width: 100%;
    min-height: 0;
    overflow: hidden;
  }

  .category-stage {
    height: max(420px, calc(100dvh - 190px));
  }

  .category-pages {
    height: 100%;
  }

  .category-page {
    height: 100%;
    min-height: 0;
    gap: clamp(7px, 1vh, 10px);
    align-content: stretch;
    overflow-y: hidden;
    overflow-x: hidden;
  }

  .category-page--overview {
    grid-template-rows: auto auto minmax(0, 1fr);
  }

  .category-page--analysis {
    grid-template-rows: auto minmax(0, 1fr);
  }

  .category-page--details {
    grid-template-rows: auto minmax(0, 1fr) minmax(138px, 0.42fr);
  }

  .category-page__heading {
    align-items: center;
    gap: 10px;
  }

  .category-page__heading p {
    margin-bottom: 2px;
    font-size: 0.62rem;
  }

  .category-page__heading h2 {
    font-size: 1.02rem;
  }

  .category-page__heading span {
    max-width: 44ch;
    font-size: 0.76rem;
    line-height: 1.24;
    overflow: hidden;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
  }

  .category-kpi-grid {
    grid-template-columns: repeat(6, minmax(0, 1fr));
    gap: 8px;
  }

  .category-kpi-grid :deep(.category-kpi) {
    min-height: 76px;
    padding: 9px 10px;
    gap: 4px;
  }

  .category-kpi-grid :deep(.category-kpi__head) {
    gap: 6px;
  }

  .category-kpi-grid :deep(.category-kpi__label) {
    font-size: 0.58rem;
    line-height: 1.12;
  }

  .category-kpi-grid :deep(.category-kpi__icon) {
    width: 15px;
    height: 15px;
  }

  .category-kpi-grid :deep(.category-kpi__value) {
    font-size: clamp(1.02rem, 1.45vw, 1.42rem);
  }

  .category-kpi-grid :deep(.category-kpi__detail) {
    font-size: 0.64rem;
    line-height: 1.18;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .category-panel--hero-chart,
  .category-panel--analysis-chart,
  .category-panel--top-chart {
    min-height: 0;
  }

  .category-overview-grid,
  .category-analysis-grid,
  .category-table-grid {
    height: 100%;
    min-height: 0;
    gap: 8px;
  }

  .category-panel,
  .category-insight-panel,
  .category-rank-panel {
    overflow: hidden;
    padding: 10px;
    gap: 8px;
  }

  .category-panel__head {
    align-items: center;
    gap: 8px;
  }

  .category-panel__head p {
    margin-bottom: 2px;
    font-size: 0.6rem;
  }

  .category-panel__head h2 {
    font-size: 0.95rem;
  }

  .category-panel__head span {
    padding: 4px 8px;
    font-size: 0.64rem;
  }

  .category-chart--compact {
    min-height: 0;
  }

  .category-insight-panel__head {
    gap: 8px;
  }

  .category-insight-panel__head svg {
    width: 28px;
    height: 28px;
    padding: 7px;
  }

  .category-insight-panel__head p {
    margin-bottom: 1px;
    font-size: 0.6rem;
  }

  .category-insight-panel__head h2 {
    font-size: 0.92rem;
  }

  .category-insight-panel > strong {
    font-size: clamp(1.08rem, 1.7vw, 1.46rem);
  }

  .category-insight-panel > span {
    font-size: 0.76rem;
    line-height: 1.3;
    overflow: hidden;
    display: -webkit-box;
    -webkit-line-clamp: 3;
    -webkit-box-orient: vertical;
  }

  .category-insight-panel__meta {
    gap: 6px;
  }

  .category-insight-panel__meta div,
  .category-performance-card {
    padding: 8px;
    gap: 3px;
  }

  .category-insight-panel__meta small,
  .category-performance-card span {
    font-size: 0.58rem;
  }

  .category-insight-panel__meta b,
  .category-performance-card strong {
    font-size: 0.9rem;
  }

  .category-rank-panel {
    grid-auto-rows: minmax(0, 1fr);
    gap: 7px;
  }

  .category-performance-card p {
    font-size: 0.66rem;
    line-height: 1.18;
    overflow: hidden;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
  }

  .category-table-card {
    min-height: 0;
  }

  .category-table-scroll {
    height: 100%;
    max-height: none;
    overflow: hidden;
  }

  .category-table th {
    position: static;
    padding: 6px 5px;
    font-size: 0.58rem;
  }

  .category-table td {
    padding: 6px 5px;
    font-size: 0.68rem;
  }

  .category-table strong {
    line-height: 1.12;
    -webkit-line-clamp: 1;
  }

  .category-table td span {
    margin-top: 2px;
    font-size: 0.62rem;
  }

  .category-age {
    padding: 3px 6px;
    font-size: 0.66rem;
  }
}

@media (min-width: 961px) {
  .category-dashboard.is-compact-height {
    --category-template-gap: 7px;
  }

  .category-dashboard.is-compact-height .category-dashboard__inner {
    padding-top: 9px;
    padding-bottom: 9px;
  }

  .category-dashboard.is-compact-height .category-header__kicker {
    display: none;
  }

  .category-dashboard.is-compact-height .category-header p {
    font-size: 0.78rem;
  }

  .category-dashboard.is-compact-height .category-month__head {
    display: none;
  }

  .category-dashboard.is-compact-height .category-page-nav {
    padding-block: 4px;
  }

  .category-dashboard.is-compact-height .category-kpi-grid :deep(.category-kpi) {
    min-height: 68px;
    padding: 7px 8px;
  }

  .category-dashboard.is-compact-height .category-kpi-grid :deep(.category-kpi__value) {
    font-size: clamp(0.96rem, 1.25vw, 1.22rem);
  }

  .category-dashboard.is-compact-height .category-panel,
  .category-dashboard.is-compact-height .category-insight-panel,
  .category-dashboard.is-compact-height .category-rank-panel {
    padding: 8px;
    gap: 6px;
  }

  .category-dashboard.is-short-height .category-header p,
  .category-dashboard.is-short-height .category-page__heading span,
  .category-dashboard.is-short-height .category-panel__head p,
  .category-dashboard.is-short-height .category-kpi-grid :deep(.category-kpi__detail) {
    display: none;
  }

  .category-dashboard.is-short-height .category-header h1 {
    font-size: 1.28rem;
  }

  .category-dashboard.is-short-height .category-month__control button,
  .category-dashboard.is-short-height .category-month__control input {
    height: 30px;
    min-height: 30px;
  }

  .category-dashboard.is-short-height .category-controls__category {
    min-height: 42px;
    padding: 4px 7px;
    gap: 7px;
  }

  .category-dashboard.is-short-height .category-controls__category svg {
    width: 14px;
    height: 14px;
    padding: 4px;
    border-radius: 8px;
  }

  .category-dashboard.is-short-height .category-controls__category strong {
    min-width: 24px;
    height: 24px;
    padding: 0 7px;
    font-size: 0.7rem;
  }

  .category-dashboard.is-short-height .category-month {
    padding: 6px;
  }

  .category-dashboard.is-short-height .category-page-nav__arrow {
    width: 28px;
    height: 28px;
  }

  .category-dashboard.is-short-height .category-page-nav {
    grid-template-columns: 32px minmax(0, 1fr) 32px;
  }

  .category-dashboard.is-short-height .category-page {
    gap: 6px;
  }

  .category-dashboard.is-short-height .category-kpi-grid {
    gap: 6px;
  }

  .category-dashboard.is-short-height .category-kpi-grid :deep(.category-kpi) {
    min-height: 54px;
    padding: 6px 7px;
  }

  .category-dashboard.is-short-height .category-panel__head h2,
  .category-dashboard.is-short-height .category-page__heading h2 {
    font-size: 0.86rem;
  }

  .category-dashboard.is-short-height .category-insight-panel > span {
    -webkit-line-clamp: 2;
  }
}

@media (max-width: 1200px) {
  .category-controls {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 960px) {
  .category-dashboard__inner {
    padding-left: clamp(14px, 2.2vw, 28px);
    padding-bottom: 96px;
  }

  .category-header {
    align-items: stretch;
    flex-direction: column;
  }

  .category-controls {
    width: 100%;
  }

  .category-month,
  .category-controls__category {
    width: 100%;
  }

  .category-overview-grid,
  .category-analysis-grid,
  .category-table-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .category-kpi-grid {
    grid-template-columns: 1fr;
  }

  .category-page-nav {
    grid-template-columns: 34px minmax(0, 1fr) 34px;
    gap: 6px;
    padding: 6px;
  }

  .category-page-nav__arrow {
    width: 32px;
    height: 32px;
  }

  .category-page-nav__center {
    grid-template-columns: 1fr;
    gap: 4px;
    justify-items: center;
  }

  .category-page-nav__count {
    display: none;
  }

  .category-page__heading,
  .category-panel__head {
    display: grid;
    align-items: start;
  }

  .category-page__heading span,
  .category-panel__head span {
    max-width: 100%;
    width: fit-content;
    text-align: left;
  }

  .category-selector__toolbar {
    grid-template-columns: 1fr;
  }

  .category-selector__actions,
  .category-selector__footer,
  .category-picker-footer {
    display: grid;
    grid-template-columns: 1fr;
  }

  .category-picker-footer div {
    display: grid;
    grid-template-columns: 1fr;
  }
}

.category-dashboard {
  --category-bg: #f7f4ee;
  --category-muted-bg: #fbfaf7;
  background: #f7f4ee;
}

.category-controls__category,
.category-month,
.category-page-nav,
.category-panel,
.category-insight-panel,
.category-rank-panel,
.category-state,
.category-setup {
  border-color: rgba(148, 163, 184, 0.24);
  background: #fbfaf7;
  box-shadow: 0 6px 16px rgba(31, 41, 55, 0.045);
}

.category-performance-card,
.category-insight-panel__meta div,
.category-age,
.category-mini-empty,
.category-chip {
  border-color: rgba(148, 163, 184, 0.18);
  background: #fffdf9;
  box-shadow: none;
}
</style>
