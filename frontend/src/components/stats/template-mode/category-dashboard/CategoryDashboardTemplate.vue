<template>
  <section class="category-dashboard" aria-label="Dashboard par categorie">
    <div v-if="loading && !hasLoadedOnce" class="category-state category-state--loading" role="status">
      <div class="category-state__pulse"></div>
      <h2>Chargement du template</h2>
      <p>Preparation de l'inventaire, des categories et des sous-categories.</p>
    </div>

    <div v-else-if="error" class="category-state category-state--error" role="alert">
      <h2>Impossible de charger le template</h2>
      <p>{{ error }}</p>
      <button type="button" @click="loadDataset">Reessayer</button>
    </div>

    <div v-else-if="!availableTypeOptions.length" class="category-state category-state--empty" role="status">
      <h2>Aucune categorie exploitable pour le moment.</h2>
      <p>
        Ajoute d'abord des items dans Gestion. Le template construira ensuite un univers principal
        puis ses sous-categories associees.
      </p>
    </div>

    <DashboardLayout
      v-else
      title="Dashboard par categorie"
      description=""
      analytics-kicker="Lecture par univers"
      analytics-title="Selection active"
      :analytics-meta="scopeMeta"
      :show-unified-content="false"
      :fit-screen="true"
    >
      <template #selector>
        <div class="category-month">
          <div class="category-month__head">
            <div class="category-month__meta">
              <span>{{ selectedPeriodMode === 'month' ? 'Mois selectionne' : 'Annee selectionnee' }}</span>
              <small>{{ periodShortLabel }}</small>
            </div>
            <div class="category-mode-switch" role="tablist" aria-label="Mode de periode">
              <button
                type="button"
                class="category-mode-switch__button"
                :class="{ 'is-active': selectedPeriodMode === 'month' }"
                :aria-selected="selectedPeriodMode === 'month'"
                role="tab"
                @click="setPeriodMode('month')"
              >
                Mois
              </button>
              <button
                type="button"
                class="category-mode-switch__button"
                :class="{ 'is-active': selectedPeriodMode === 'year' }"
                :aria-selected="selectedPeriodMode === 'year'"
                role="tab"
                @click="setPeriodMode('year')"
              >
                Annee
              </button>
            </div>
          </div>
          <div class="category-month__control">
            <button
              type="button"
              :aria-label="selectedPeriodMode === 'month' ? 'Mois precedent' : 'Annee precedente'"
              :disabled="!canGoPreviousMonth"
              @click="changePeriod(-1)"
            >
              <ChevronLeft aria-hidden="true" />
            </button>
            <input
              v-if="selectedPeriodMode === 'month'"
              :value="selectedMonthKey"
              type="month"
              :min="minMonthKey || undefined"
              :max="maxMonthKey || undefined"
              aria-label="Selectionner un mois"
              @input="onMonthInput"
            />
            <input
              v-else
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
              :aria-label="selectedPeriodMode === 'month' ? 'Mois suivant' : 'Annee suivante'"
              :disabled="!canGoNextMonth"
              @click="changePeriod(1)"
            >
              <ChevronRight aria-hidden="true" />
            </button>
          </div>
        </div>
      </template>

      <template #navigation>
        <nav class="category-page-nav" aria-label="Navigation du dashboard categorie">
          <button
            type="button"
            class="category-page-nav__arrow"
            :disabled="activePage === 0"
            aria-label="Page precedente"
            @click="goToPage(activePage - 1)"
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
            @click="goToPage(activePage + 1)"
          >
            <ChevronRight aria-hidden="true" />
          </button>
          <button
            type="button"
            class="category-page-nav__scope-toggle"
            :class="{ 'is-open': isScopeExpanded }"
            :aria-expanded="isScopeExpanded"
            aria-label="Afficher les filtres categorie"
            @click="toggleScopePanel"
          >
            <ChevronDown aria-hidden="true" />
          </button>
        </nav>
      </template>

      <section class="category-scope-card" aria-label="Selection categorie et sous-categories">
        <div class="category-scope-card__launcher">
          <button
            type="button"
            class="category-scope-handle"
            :class="{ 'is-open': isScopeExpanded }"
            :aria-expanded="isScopeExpanded"
            @click="toggleScopePanel"
          >
            <SlidersHorizontal aria-hidden="true" />
            <div class="category-scope-handle__copy">
              <strong>Filtres</strong>
              <small>{{ selectedTypeLabel }} · {{ bannerSelectionText }}</small>
            </div>
            <span class="category-scope-handle__count">{{ formatNumber(filteredItems.length) }}</span>
            <ChevronDown aria-hidden="true" />
          </button>
        </div>

        <Transition name="category-scope-slide">
          <div v-if="isScopeExpanded" class="category-scope-card__body">
            <div class="category-scope-card__head category-scope-card__head--compact">
              <div>
                <p>Selection</p>
                <h2>{{ selectedTypeLabel }}</h2>
              </div>
              <div class="category-scope-card__meta">
                <span>{{ formatNumber(filteredItems.length) }} item(s) visibles</span>
                <button
                  v-if="selectedSubcategories.length"
                  type="button"
                  class="category-scope-card__reset"
                  @click.stop="clearSelectedSubcategories"
                >
                  <RotateCcw aria-hidden="true" />
                  <span>Tout voir</span>
                </button>
              </div>
            </div>

            <div class="category-toolbar" aria-label="Filtres du dashboard categorie">
              <div ref="typePickerRef" class="category-picker-field">
                <span class="category-picker-field__label">Univers</span>
                <button
                  type="button"
                  class="category-picker"
                  :class="{ 'is-open': isTypeMenuOpen }"
                  aria-haspopup="dialog"
                  :aria-expanded="isTypeMenuOpen"
                  @click.stop="toggleTypeMenu"
                >
                  <div class="category-picker__copy">
                    <small>Type item</small>
                    <strong>{{ selectedTypeLabel }}</strong>
                  </div>
                  <span class="category-picker__meta">{{ formatNumber(typeItemCount) }} item(s)</span>
                  <ChevronsUpDown aria-hidden="true" />
                </button>

                <div
                  v-if="isTypeMenuOpen"
                  class="category-picker-menu category-picker-menu--type"
                  role="dialog"
                  aria-label="Choisir un univers"
                >
                  <button
                    v-for="option in availableTypeOptions"
                    :key="option.value"
                    type="button"
                    class="category-picker-option"
                    :class="{ 'is-active': selectedType === option.value }"
                    @click.stop="selectType(option.value)"
                  >
                    <div>
                      <span>{{ option.label }}</span>
                      <small>{{ formatNumber(option.count) }} item(s)</small>
                    </div>
                    <Check v-if="selectedType === option.value" aria-hidden="true" />
                  </button>
                </div>
              </div>

              <div ref="subcategoryPickerRef" class="category-picker-field">
                <span class="category-picker-field__label">Sous-categories</span>
                <button
                  type="button"
                  class="category-picker"
                  :class="{ 'is-open': isSubcategoryMenuOpen }"
                  :disabled="!subcategoryOptions.length"
                  aria-haspopup="dialog"
                  :aria-expanded="isSubcategoryMenuOpen"
                  @click.stop="toggleSubcategoryMenu"
                >
                  <div class="category-picker__copy">
                    <small>Filtre</small>
                    <strong>{{ selectedSubcategoryLabel }}</strong>
                  </div>
                  <span class="category-picker__meta">{{ selectedSubcategoryMeta }}</span>
                  <ChevronsUpDown aria-hidden="true" />
                </button>

                <div
                  v-if="isSubcategoryMenuOpen && subcategoryOptions.length"
                  class="category-picker-menu category-picker-menu--subcategory"
                  role="dialog"
                  aria-label="Choisir des sous-categories"
                >
                  <button
                    type="button"
                    class="category-picker-option"
                    :class="{ 'is-active': !selectedSubcategories.length }"
                    @click.stop="clearSelectedSubcategories"
                  >
                    <div>
                      <span>Toutes les sous-categories</span>
                      <small>{{ formatNumber(typeItemCount) }} item(s)</small>
                    </div>
                    <Check v-if="!selectedSubcategories.length" aria-hidden="true" />
                  </button>

                  <button
                    v-for="option in subcategoryOptions"
                    :key="option.key"
                    type="button"
                    class="category-picker-option"
                    :class="{ 'is-active': selectedSubcategorySet.has(option.key) }"
                    @click.stop="toggleSubcategory(option)"
                  >
                    <div>
                      <span>{{ option.label }}</span>
                      <small>{{ formatNumber(option.count) }} item(s)</small>
                    </div>
                    <Check v-if="selectedSubcategorySet.has(option.key)" aria-hidden="true" />
                  </button>
                </div>
              </div>
            </div>

            <div v-if="selectedSubcategories.length" class="category-active-row">
              <span class="category-active-row__label">Actif</span>
              <div class="category-active-row__chips">
                <span
                  v-for="label in selectedSubcategoryPreview"
                  :key="label"
                  class="category-active-chip"
                >
                  {{ label }}
                </span>
                <span v-if="hiddenSelectedSubcategoryCount" class="category-active-chip category-active-chip--count">
                  +{{ hiddenSelectedSubcategoryCount }}
                </span>
              </div>
            </div>

            <p class="category-scope-card__summary">{{ selectionSummary }}</p>
          </div>
        </Transition>
      </section>

      <div v-if="!filteredItems.length" class="category-state category-state--empty" role="status">
        <h2>Aucun item dans cette selection.</h2>
        <p>
          Change d'univers ou retire le filtre de sous-categorie pour revenir a une vue plus large.
        </p>
      </div>

      <div v-else-if="!hasPeriodData" class="category-state category-state--empty" role="status">
        <h2>Aucune donnee visible pour {{ selectedMonthLabel }}.</h2>
        <p>
          L'univers selectionne existe bien, mais il n'a ni achat, ni vente, ni stock exploitable
          sur ce mois.
        </p>
        <button
          v-if="latestActiveMonthKey"
          type="button"
          @click="selectedMonthKey = latestActiveMonthKey"
        >
          Aller au dernier mois actif
        </button>
      </div>

      <template v-else>
        <article v-if="activePage === 0" class="category-page" aria-label="Vue d'ensemble categorie">
          <div class="category-page__heading">
            <div>
              <p>Vue d'ensemble</p>
              <h2>{{ selectedTypeLabel }}</h2>
            </div>
            <span>{{ scopeMeta }}</span>
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

          <div class="category-main-grid">
            <section class="category-panel">
              <div class="category-panel__head">
                <div>
                  <p>{{ selectedPeriodMode === 'year' ? 'Tendance de l annee' : 'Tendance du mois' }}</p>
                  <h2>{{ selectedPeriodMode === 'year' ? 'CA et profit mois par mois' : 'CA et profit jour par jour' }}</h2>
                </div>
                <span>{{ formatNumber(dailyRows.length) }} {{ selectedPeriodMode === 'year' ? 'mois' : 'jour(s)' }}</span>
              </div>
              <VChart v-if="hasDailyChart" class="category-chart" :option="dailyTrendOption" autoresize />
              <div v-else class="category-mini-empty">Aucune vente visible sur cette periode.</div>
            </section>

            <section class="category-panel">
              <div class="category-panel__head">
                <div>
                  <p>Lecture par sous-categorie</p>
                  <h2>{{ segmentPanelTitle }}</h2>
                </div>
                <span>{{ formatNumber(segmentRows.length) }} segment(s)</span>
              </div>
              <VChart
                v-if="segmentRows.length"
                class="category-chart"
                :option="segmentChartOption"
                autoresize
              />
              <div v-else class="category-mini-empty">Aucun segment exploitable pour cette selection.</div>
            </section>
          </div>

          <section class="category-module-grid" aria-label="Synthese actionnable">
            <article
              v-for="item in insightCards"
              :key="item.title"
              class="category-module-card"
              :class="item.tone ? `is-${item.tone}` : ''"
            >
              <span v-if="item.badge">{{ item.badge }}</span>
              <h3>{{ item.title }}</h3>
              <strong>{{ item.value }}</strong>
              <p>{{ item.detail }}</p>
            </article>
          </section>
        </article>

        <article v-else class="category-page" aria-label="Details categorie">
          <div class="category-page__heading">
            <div>
              <p>Details</p>
              <h2>Top ventes et stock restant</h2>
            </div>
            <span>{{ selectedMonthLabel }}</span>
          </div>

          <div class="category-detail-grid">
            <section class="category-panel category-table-card">
              <div class="category-panel__head">
                <div>
                  <p>Ventes</p>
                  <h2>Items les plus rentables</h2>
                </div>
                <span>{{ topSoldRows.length }} ligne(s)</span>
              </div>

              <div v-if="topSoldRows.length" class="category-table-scroll">
                <table class="category-table">
                  <thead>
                    <tr>
                      <th>Produit</th>
                      <th>Sous-categorie</th>
                      <th>Vente</th>
                      <th>Profit</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="item in topSoldRows" :key="item.id">
                      <td>
                        <strong :title="item.name">{{ item.name }}</strong>
                        <span>{{ formatDate(item.soldAt) }}</span>
                      </td>
                      <td>{{ item.scopeLabel }}</td>
                      <td>{{ formatMoney(item.salePrice) }}</td>
                      <td :class="profitClass(item.profit)">{{ formatMoney(item.profit) }}</td>
                    </tr>
                  </tbody>
                </table>
              </div>
              <div v-else class="category-mini-empty">Aucune vente sur cette periode.</div>
            </section>

            <section class="category-panel category-table-card">
              <div class="category-panel__head">
                <div>
                  <p>Stock</p>
                  <h2>Articles encore immobilises</h2>
                </div>
                <span>{{ stockRows.length }} ligne(s)</span>
              </div>

              <div v-if="stockRows.length" class="category-table-scroll">
                <table class="category-table">
                  <thead>
                    <tr>
                      <th>Produit</th>
                      <th>Sous-categorie</th>
                      <th>Valeur</th>
                      <th>Age</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="item in stockRows" :key="item.id">
                      <td>
                        <strong :title="item.name">{{ item.name }}</strong>
                        <span>{{ formatDate(item.purchasedAt) }}</span>
                      </td>
                      <td>{{ item.scopeLabel }}</td>
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
              <div v-else class="category-mini-empty">Aucun stock restant pour cette selection.</div>
            </section>
          </div>

          <div class="category-detail-grid category-detail-grid--bottom">
            <section class="category-panel">
              <div class="category-panel__head">
                <div>
                  <p>Classement</p>
                  <h2>Profit par item vendu</h2>
                </div>
                <span>Top {{ formatNumber(topProfitChartRows.length) }}</span>
              </div>
              <VChart v-if="topProfitChartRows.length" class="category-chart" :option="topItemsOption" autoresize />
              <div v-else class="category-mini-empty">Aucun item vendu a classer.</div>
            </section>

            <section class="category-panel">
              <div class="category-panel__head">
                <div>
                  <p>Lecture rapide</p>
                  <h2>{{ selectedPeriodMode === 'year' ? 'Sous-categories de l annee' : 'Sous-categories du mois' }}</h2>
                </div>
                <span>{{ formatNumber(segmentRows.length) }} vue(s)</span>
              </div>

              <div v-if="segmentRows.length" class="category-segment-list">
                <article v-for="row in segmentRows.slice(0, 6)" :key="row.key" class="category-segment-card">
                  <div>
                    <strong>{{ row.label }}</strong>
                    <span>{{ formatNumber(row.sold) }} vente(s) · {{ formatNumber(row.stockCount) }} stock</span>
                  </div>
                  <div class="category-segment-card__values">
                    <b>{{ formatMoney(row.profit) }}</b>
                    <small>{{ formatMoney(row.revenue) }} CA</small>
                  </div>
                </article>
              </div>
              <div v-else class="category-mini-empty">Aucune sous-categorie a afficher.</div>
            </section>
          </div>
        </article>
      </template>
    </DashboardLayout>
  </section>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import {
  BadgeEuro,
  Boxes,
  Check,
  ChevronsUpDown,
  ChevronDown,
  ChevronLeft,
  ChevronRight,
  CirclePercent,
  RotateCcw,
  SlidersHorizontal,
  TrendingUp,
  Wallet,
} from 'lucide-vue-next'
import { useAuthStore } from '@/store/authStore'
import StatsServices from '@/services/StatsServices'
import SnkVenteServices from '@/services/SnkVenteServices'
import {
  buildItemCategoryAliases,
  isItemCategoryAlias,
  itemTypeLabel,
  normalizeItemType,
  readStoredItemCategories,
} from '@/RegleItem/itemCategoryStore'
import {
  extractSubcategoriesByType,
  readStoredSubcategories,
  resolveSubcategoryOptions,
} from '@/RegleItem/subcategoryStore'
import DashboardLayout from '../shared/DashboardLayout.vue'
import TemplateKpiCard from '../shared/TemplateKpiCard.vue'

type CategoryTemplateState = {
  periodMode?: 'month' | 'year'
  monthKey?: string
  month?: string | number
  year?: number
  type?: string
  categories?: string[]
}

type RawItem = Record<string, unknown>

type InventoryItem = {
  id: string
  name: string
  type: string
  typeLabel: string
  subcategory: string
  subcategoryKey: string
  scopeLabel: string
  purchasePrice: number
  salePrice: number
  profit: number
  roi: number
  purchasedAt: string
  soldAt: string
  ageInDays: number
}

type TypeOption = {
  value: string
  label: string
  count: number
}

type SubcategoryOption = {
  key: string
  label: string
  count: number
}

type SegmentRow = {
  key: string
  label: string
  revenue: number
  profit: number
  sold: number
  bought: number
  purchaseSpend: number
  stockCount: number
  stockValue: number
}

type DailyRow = {
  date: string
  label: string
  revenue: number
  profit: number
}

const props = defineProps<{
  initialState?: CategoryTemplateState
}>()

const emit = defineEmits<{
  (event: 'state-change', state: CategoryTemplateState): void
}>()

const auth = useAuthStore()
const monthLabels = ['Jan', 'Fev', 'Mar', 'Avr', 'Mai', 'Juin', 'Juil', 'Aout', 'Sep', 'Oct', 'Nov', 'Dec']
const today = new Date()
const currentMonthKey = formatMonthKey(today)
const currentYear = today.getFullYear()
const UNKNOWN_SCOPE_LABEL = 'Sans sous-categorie'

const selectedPeriodMode = ref(normalizeInitialPeriodMode(props.initialState))
const selectedMonthKey = ref(normalizeInitialMonth(props.initialState))
const selectedType = ref(normalizeItemType(props.initialState?.type || ''))
const selectedSubcategories = ref(sanitizeSelection(props.initialState?.categories))
const activePage = ref(0)
const isScopeExpanded = ref(false)
const isTypeMenuOpen = ref(false)
const isSubcategoryMenuOpen = ref(false)
const typePickerRef = ref<HTMLElement | null>(null)
const subcategoryPickerRef = ref<HTMLElement | null>(null)
const yearDraft = ref(String(parseMonthKey(selectedMonthKey.value).getFullYear()))
const minDate = ref('')
const maxDate = ref(formatYmd(today))
const minMonthKey = ref('')
const maxMonthKey = ref(currentMonthKey)
const loading = ref(false)
const hasLoadedOnce = ref(false)
const error = ref('')
const rawItems = ref<RawItem[]>([])
const categoryLabels = ref(readStoredItemCategories(currentUserId()))
const storedSubcategories = ref(readStoredSubcategories(currentUserId(), undefined, categoryLabels.value))
let requestId = 0

const pages = [
  { key: 'overview', label: "Vue d'ensemble" },
  { key: 'details', label: 'Details' },
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

const currentPage = computed(() => pages[activePage.value] ?? pages[0])
const selectedYear = computed(() => parseMonthKey(selectedMonthKey.value).getFullYear())
const selectedMonthLabel = computed(() =>
  selectedPeriodMode.value === 'year'
    ? formatYearLong(selectedYear.value)
    : formatMonthLong(selectedMonthKey.value),
)
const periodRange = computed(() =>
  selectedPeriodMode.value === 'year'
    ? buildYearRange(selectedYear.value)
    : buildMonthRange(selectedMonthKey.value),
)
const periodShortLabel = computed(() => {
  const start = parseYmd(periodRange.value.from)
  const end = parseYmd(periodRange.value.to)
  if (selectedPeriodMode.value === 'year') {
    return `${monthLabels[start.getMonth()]}-${monthLabels[end.getMonth()]} ${end.getFullYear()}`
  }
  return `${start.getDate()}-${end.getDate()} ${monthLabels[end.getMonth()]}`
})
const minYear = computed(() => (minMonthKey.value ? Number(minMonthKey.value.slice(0, 4)) : currentYear - 5))
const maxYear = computed(() => (maxMonthKey.value ? Number(maxMonthKey.value.slice(0, 4)) : currentYear))
const canGoPreviousMonth = computed(() =>
  selectedPeriodMode.value === 'year'
    ? selectedYear.value > minYear.value
    : !minMonthKey.value || selectedMonthKey.value > minMonthKey.value,
)
const canGoNextMonth = computed(() =>
  selectedPeriodMode.value === 'year'
    ? selectedYear.value < maxYear.value
    : !maxMonthKey.value || selectedMonthKey.value < maxMonthKey.value,
)
const mainCategoryAliases = computed(() => buildItemCategoryAliases(categoryLabels.value))
const discoveredSubcategories = computed(() =>
  extractSubcategoriesByType(rawItems.value, categoryLabels.value),
)
const items = computed(() => rawItems.value.map((row) => normalizeItem(row)))

const availableTypeOptions = computed<TypeOption[]>(() => {
  const map = new Map<string, TypeOption>()
  for (const item of items.value) {
    const current = map.get(item.type) ?? { value: item.type, label: item.typeLabel, count: 0 }
    current.count += 1
    current.label = item.typeLabel
    map.set(item.type, current)
  }
  return Array.from(map.values()).sort(
    (a, b) =>
      b.count - a.count || a.label.localeCompare(b.label, 'fr', { sensitivity: 'base', numeric: true }),
  )
})

const typeItems = computed(() => items.value.filter((item) => item.type === selectedType.value))
const typeItemCount = computed(() => typeItems.value.length)
const selectedTypeLabel = computed(() => {
  const current = availableTypeOptions.value.find((option) => option.value === selectedType.value)
  if (current) return current.label
  return availableTypeOptions.value[0]?.label || 'Categorie'
})

const subcategoryOptions = computed<SubcategoryOption[]>(() => {
  if (!selectedType.value) return []
  const base = resolveSubcategoryOptions(selectedType.value, {
    stored: storedSubcategories.value,
    discovered: discoveredSubcategories.value,
    categoryLabels: categoryLabels.value,
    mainCategoryAliases: mainCategoryAliases.value,
  })

  const counts = new Map<string, SubcategoryOption>()
  for (const label of base) {
    const key = normalizeKey(label)
    counts.set(key, { key, label, count: 0 })
  }

  let uncategorizedCount = 0
  for (const item of typeItems.value) {
    if (!item.subcategory) {
      uncategorizedCount += 1
      continue
    }
    const current = counts.get(item.subcategoryKey) ?? {
      key: item.subcategoryKey,
      label: item.subcategory,
      count: 0,
    }
    current.count += 1
    counts.set(item.subcategoryKey, current)
  }

  const rows = Array.from(counts.values()).sort(
    (a, b) =>
      b.count - a.count || a.label.localeCompare(b.label, 'fr', { sensitivity: 'base', numeric: true }),
  )

  if (uncategorizedCount > 0) {
    rows.push({
      key: normalizeKey(UNKNOWN_SCOPE_LABEL),
      label: UNKNOWN_SCOPE_LABEL,
      count: uncategorizedCount,
    })
  }

  return rows
})

const selectedSubcategorySet = computed(
  () => new Set(selectedSubcategories.value.map((value) => normalizeKey(value))),
)
const selectedSubcategoryLabel = computed(() => {
  if (!selectedSubcategories.value.length) return 'Toutes les sous-categories'
  if (selectedSubcategories.value.length === 1) return selectedSubcategories.value[0]
  return `${formatNumber(selectedSubcategories.value.length)} sous-categories`
})
const selectedSubcategoryMeta = computed(() => {
  if (!subcategoryOptions.value.length) return 'Aucune option'
  if (!selectedSubcategories.value.length) return `${formatNumber(subcategoryOptions.value.length)} option(s)`
  return `${formatNumber(filteredItems.value.length)} item(s) visibles`
})
const selectedSubcategoryPreview = computed(() => selectedSubcategories.value.slice(0, 3))
const hiddenSelectedSubcategoryCount = computed(() =>
  Math.max(0, selectedSubcategories.value.length - selectedSubcategoryPreview.value.length),
)
const bannerSelectionText = computed(() => {
  if (!selectedSubcategories.value.length) return 'Toutes les sous-categories'
  if (selectedSubcategories.value.length === 1) return selectedSubcategories.value[0]
  return `${formatNumber(selectedSubcategories.value.length)} sous-categories actives`
})
const filteredItems = computed(() => {
  if (!selectedType.value) return []
  if (!selectedSubcategorySet.value.size) return typeItems.value
  return typeItems.value.filter((item) => selectedSubcategorySet.value.has(item.subcategoryKey))
})

const soldInPeriod = computed(() =>
  filteredItems.value
    .filter((item) => item.soldAt && isDateBetween(item.soldAt, periodRange.value.from, periodRange.value.to))
    .map((item) => enrichItem(item, periodRange.value.to)),
)
const purchasedInPeriod = computed(() =>
  filteredItems.value
    .filter((item) => item.purchasedAt && isDateBetween(item.purchasedAt, periodRange.value.from, periodRange.value.to))
    .map((item) => enrichItem(item, periodRange.value.to)),
)
const stockAtEnd = computed(() =>
  filteredItems.value
    .filter((item) => {
      const purchasedBeforeEnd = !item.purchasedAt || item.purchasedAt <= periodRange.value.to
      const soldAfterEnd = !item.soldAt || item.soldAt > periodRange.value.to
      return purchasedBeforeEnd && soldAfterEnd
    })
    .map((item) => enrichItem(item, periodRange.value.to)),
)

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
  if (selectedPeriodMode.value === 'year') {
    const byMonth = new Map<string, DailyRow>()
    for (let month = 1; month <= 12; month += 1) {
      const key = `${selectedYear.value}-${pad(month)}`
      byMonth.set(key, { date: `${key}-01`, label: monthLabels[month - 1], revenue: 0, profit: 0 })
    }
    for (const item of soldInPeriod.value) {
      const key = monthKeyFromYmd(item.soldAt)
      const row = byMonth.get(key)
      if (!row) continue
      row.revenue += item.salePrice
      row.profit += item.profit
    }
    return Array.from(byMonth.values())
  }

  const byDate = new Map<string, DailyRow>()
  for (const day of listDays(periodRange.value.from, periodRange.value.to)) {
    byDate.set(day, { date: day, label: formatDayShort(day), revenue: 0, profit: 0 })
  }
  for (const item of soldInPeriod.value) {
    const row = byDate.get(item.soldAt)
    if (!row) continue
    row.revenue += item.salePrice
    row.profit += item.profit
  }
  return Array.from(byDate.values())
})

const segmentRows = computed<SegmentRow[]>(() => {
  const map = new Map<string, SegmentRow>()
  const ensureRow = (key: string, label: string) => {
    const current = map.get(key) ?? {
      key,
      label,
      revenue: 0,
      profit: 0,
      sold: 0,
      bought: 0,
      purchaseSpend: 0,
      stockCount: 0,
      stockValue: 0,
    }
    map.set(key, current)
    return current
  }

  for (const item of soldInPeriod.value) {
    const row = ensureRow(item.subcategoryKey, item.scopeLabel)
    row.revenue += item.salePrice
    row.profit += item.profit
    row.sold += 1
  }
  for (const item of purchasedInPeriod.value) {
    const row = ensureRow(item.subcategoryKey, item.scopeLabel)
    row.bought += 1
    row.purchaseSpend += item.purchasePrice
  }
  for (const item of stockAtEnd.value) {
    const row = ensureRow(item.subcategoryKey, item.scopeLabel)
    row.stockCount += 1
    row.stockValue += item.purchasePrice
  }

  return Array.from(map.values()).sort(
    (a, b) => b.profit - a.profit || b.revenue - a.revenue || a.label.localeCompare(b.label, 'fr'),
  )
})

const bestSegment = computed(() => segmentRows.value[0] ?? null)
const topSoldRows = computed(() => [...soldInPeriod.value].sort((a, b) => b.profit - a.profit).slice(0, 6))
const stockRows = computed(() =>
  [...stockAtEnd.value]
    .sort((a, b) => b.ageInDays - a.ageInDays || b.purchasePrice - a.purchasePrice)
    .slice(0, 6),
)
const topProfitChartRows = computed(() => [...topSoldRows.value].slice(0, 6).reverse())
const latestActiveMonthKey = computed(() => {
  const keys = filteredItems.value
    .flatMap((item) => [monthKeyFromYmd(item.purchasedAt), monthKeyFromYmd(item.soldAt)])
    .filter(Boolean)
    .sort()
  return keys.length ? keys[keys.length - 1] : ''
})

const scopeMeta = computed(() => {
  if (!selectedSubcategories.value.length) return `${selectedTypeLabel.value} · vue complete`
  if (selectedSubcategories.value.length === 1) return `${selectedTypeLabel.value} · ${selectedSubcategories.value[0]}`
  return `${selectedTypeLabel.value} · ${selectedSubcategories.value.length} sous-categories`
})
const selectionSummary = computed(() => {
  if (!selectedSubcategories.value.length) {
    return `${selectedTypeLabel.value} est analyse en entier. Active une ou plusieurs sous-categories seulement si tu veux zoomer sur un segment precis.`
  }
  if (selectedSubcategories.value.length === 1) {
    return `Le dashboard est actuellement focalise sur ${selectedSubcategories.value[0]} dans ${selectedTypeLabel.value}.`
  }
  return `${selectedSubcategories.value.length} sous-categories sont actives dans ${selectedTypeLabel.value}.`
})

const kpiCards = computed(() => [
  {
    label: "Chiffre d'affaires",
    value: formatMoney(totals.value.revenue),
    detail: `${formatNumber(totals.value.itemsSold)} vente(s)`,
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
    detail: 'Profit / CA sur la periode',
    tone: totals.value.marginRate >= 0 ? ('profit' as const) : ('warning' as const),
    icon: CirclePercent,
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
    detail: `${formatNumber(totals.value.stockCount)} article(s) encore en stock`,
    tone: 'neutral' as const,
    icon: Boxes,
  },
])

const insightCards = computed(() => [
  {
    badge: 'Leader',
    title: 'Sous-categorie forte',
    value: bestSegment.value?.label ?? selectedTypeLabel.value,
    detail: bestSegment.value
      ? `${formatMoney(bestSegment.value.profit)} de profit sur ${formatNumber(bestSegment.value.sold)} vente(s).`
      : 'Aucun segment ne ressort encore clairement.',
    tone: (bestSegment.value?.profit ?? 0) >= 0 ? 'profit' : 'warning',
  },
  {
    badge: 'Cash',
    title: 'Net du mois',
    value: formatMoney(totals.value.cashNet),
    detail:
      totals.value.cashNet >= 0
        ? 'Les ventes couvrent les achats sur cette periode.'
        : 'Les achats depassent encore les ventes sur cette periode.',
    tone: totals.value.cashNet >= 0 ? 'positive' : 'warning',
  },
  {
    badge: 'Stock',
    title: 'Point d attention',
    value: stockRows.value[0] ? formatDays(stockRows.value[0].ageInDays) : '--',
    detail: stockRows.value[0]
      ? `${stockRows.value[0].name} est l'article le plus ancien encore en stock.`
      : 'Aucun stock ancien a surveiller actuellement.',
    tone: stockRows.value[0]?.ageInDays >= 90 ? 'warning' : 'neutral',
  },
  {
    badge: 'Lecture',
    title: 'Panier moyen',
    value: formatMoney(totals.value.averageSalePrice),
    detail: `${formatMoney(totals.value.averagePurchasePrice)} en prix d'achat moyen.`,
    tone: 'primary',
  },
])

const hasDailyChart = computed(() => dailyRows.value.some((row) => row.revenue || row.profit))
const segmentPanelTitle = computed(() => {
  if (segmentRows.value.length <= 1) return 'Volume et stock du segment'
  return 'Performance des sous-categories'
})

const dailyTrendOption = computed(() => ({
  color: ['#4f46e5', '#059669'],
  grid: { left: 10, right: 12, top: 38, bottom: 12, containLabel: true },
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
    data: dailyRows.value.map((row) => row.label),
    axisTick: { show: false },
    axisLine: { lineStyle: { color: '#cbd5e1' } },
    axisLabel: {
      color: '#64748b',
      interval: selectedPeriodMode.value === 'year' ? 0 : Math.max(0, Math.floor(dailyRows.value.length / 9)),
    },
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

const segmentChartOption = computed(() => {
  if (segmentRows.value.length <= 1) {
    return {
      color: ['#4f46e5', '#d97706', '#059669'],
      grid: { left: 8, right: 12, top: 12, bottom: 12, containLabel: true },
      tooltip: { trigger: 'axis', confine: true, axisPointer: { type: 'shadow' } },
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
          barMaxWidth: 44,
          data: [
            { value: totals.value.itemsSold, itemStyle: { color: '#4f46e5' } },
            { value: totals.value.itemsBought, itemStyle: { color: '#d97706' } },
            { value: totals.value.stockCount, itemStyle: { color: '#059669' } },
          ],
          itemStyle: { borderRadius: [7, 7, 0, 0] },
        },
      ],
    }
  }

  const rows = [...segmentRows.value].sort((a, b) => a.profit - b.profit || a.revenue - b.revenue)
  return {
    color: ['#4f46e5', '#059669'],
    grid: { left: 8, right: 18, top: 36, bottom: 10, containLabel: true },
    tooltip: {
      trigger: 'axis',
      confine: true,
      axisPointer: { type: 'shadow' },
      formatter: (params: Array<{ dataIndex: number }>) => {
        const row = rows[params?.[0]?.dataIndex ?? 0]
        if (!row) return ''
        return `${row.label}<br/>CA: ${formatMoney(row.revenue)}<br/>Profit: ${formatMoney(row.profit)}<br/>Ventes: ${formatNumber(row.sold)}`
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
      data: rows.map((row) => row.label),
      axisTick: { show: false },
      axisLine: { show: false },
      axisLabel: { color: '#334155', width: 120, overflow: 'truncate' },
    },
    series: [
      {
        name: 'CA',
        type: 'bar',
        barMaxWidth: 24,
        data: rows.map((row) => row.revenue),
        itemStyle: { borderRadius: [0, 6, 6, 0], color: 'rgba(79, 70, 229, 0.7)' },
      },
      {
        name: 'Profit',
        type: 'bar',
        barMaxWidth: 24,
        data: rows.map((row) => row.profit),
        itemStyle: {
          borderRadius: [0, 6, 6, 0],
          color: (params: { value: number }) => (Number(params.value ?? 0) >= 0 ? '#10b981' : '#f97316'),
        },
      },
    ],
  }
})

const topItemsOption = computed(() => ({
  grid: { left: 8, right: 18, top: 8, bottom: 8, containLabel: true },
  tooltip: {
    trigger: 'axis',
    confine: true,
    axisPointer: { type: 'shadow' },
    formatter: (params: Array<{ dataIndex: number }>) => {
      const item = topProfitChartRows.value[params?.[0]?.dataIndex ?? 0]
      return item
        ? `${item.name}<br/>Profit: ${formatMoney(item.profit)}<br/>Sous-categorie: ${item.scopeLabel}`
        : ''
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
    selectedPeriodMode.value = normalizeInitialPeriodMode(state)
    const nextMonth = clampMonthKey(normalizeInitialMonth(state))
    if (selectedMonthKey.value !== nextMonth) selectedMonthKey.value = nextMonth
    selectedType.value = normalizeItemType(state?.type || selectedType.value)
    selectedSubcategories.value = sanitizeSelection(state?.categories)
  },
  { deep: true },
)

watch(
  availableTypeOptions,
  (options) => {
    if (!options.length) {
      selectedType.value = ''
      return
    }
    if (options.some((option) => option.value === selectedType.value)) return

    const inferred = inferTypeFromSelection(selectedSubcategories.value, options)
    selectedType.value = inferred || options[0].value
  },
  { immediate: true },
)

watch(
  [selectedType, subcategoryOptions],
  () => {
    const allowed = new Map(subcategoryOptions.value.map((option) => [option.key, option.label]))
    selectedSubcategories.value = selectedSubcategories.value.filter((value) =>
      allowed.has(normalizeKey(value)),
    )
    if (!subcategoryOptions.value.length) isSubcategoryMenuOpen.value = false
  },
  { deep: true },
)

watch(
  selectedMonthKey,
  (monthKey) => {
    yearDraft.value = String(parseMonthKey(monthKey).getFullYear())
  },
  { immediate: true },
)

watch(
  [selectedPeriodMode, selectedMonthKey, selectedType, selectedSubcategories],
  () => {
    emit('state-change', {
      periodMode: selectedPeriodMode.value,
      monthKey: selectedMonthKey.value,
      month: Number(selectedMonthKey.value.slice(5, 7)),
      year: Number(selectedMonthKey.value.slice(0, 4)),
      type: selectedType.value,
      categories: [...selectedSubcategories.value],
    })
  },
  { deep: true, immediate: true },
)

watch(
  () => auth.user.value,
  () => {
    refreshStoredCategoryMetadata()
  },
  { deep: true },
)

function currentUserId() {
  return String(
    auth.user.value?.id ?? auth.user.value?.email ?? auth.user.value?.username ?? 'guest',
  )
}

function normalizeInitialPeriodMode(state?: CategoryTemplateState) {
  return state?.periodMode === 'year' ? 'year' : 'month'
}

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

function buildYearRange(year: number) {
  const first = new Date(year, 0, 1)
  const last = new Date(year, 11, 31)
  let from = formatYmd(first)
  let to = formatYmd(last)
  if (minDate.value && minDate.value.slice(0, 4) === String(year) && from < minDate.value) {
    from = minDate.value
  }
  if (maxDate.value && maxDate.value.slice(0, 4) === String(year) && to > maxDate.value) {
    to = maxDate.value
  }
  if (from > to) from = to
  return { from, to }
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

function monthKeyFromYmd(value: string) {
  return /^\d{4}-\d{2}-\d{2}$/.test(value) ? value.slice(0, 7) : ''
}

function formatMonthKey(date: Date) {
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}`
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
  const type = normalizeItemType(readField(row, 'type'))
  const typeLabel = itemTypeLabel(type, categoryLabels.value)
  const rawSubcategory = normalizeLabel(readField(row, 'categorie'))
  const subcategory = rawSubcategory && !isItemCategoryAlias(rawSubcategory, categoryLabels.value)
    ? rawSubcategory
    : ''
  const purchasePrice = toNumber(readField(row, 'prixRetail'))
  const salePrice = toNumber(readField(row, 'prixResell'))
  const purchasedAt = toDateKey(readField(row, 'dateAchat'))
  const soldAt = toDateKey(readField(row, 'dateVente'))
  const profit = salePrice - purchasePrice
  return {
    id: String(readField(row, 'id') ?? `${readField(row, 'nomItem') ?? 'item'}-${purchasedAt}-${soldAt}`),
    name: normalizeItemName(readField(row, 'nomItem')),
    type,
    typeLabel,
    subcategory,
    subcategoryKey: normalizeKey(subcategory || UNKNOWN_SCOPE_LABEL),
    scopeLabel: subcategory || UNKNOWN_SCOPE_LABEL,
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

function normalizeLabel(value: unknown) {
  return String(value ?? '').replace(/\s+/g, ' ').trim()
}

function normalizeKey(value: unknown) {
  return normalizeLabel(value).toLocaleLowerCase('fr')
}

function sanitizeSelection(value: unknown) {
  if (!Array.isArray(value)) return []
  const map = new Map<string, string>()
  for (const entry of value) {
    const label = normalizeLabel(entry)
    if (!label) continue
    map.set(normalizeKey(label), label)
  }
  return Array.from(map.values())
}

function inferTypeFromSelection(selection: string[], options: TypeOption[]) {
  if (!selection.length) return ''
  const optionTypes = options.map((option) => option.value)
  for (const type of optionTypes) {
    const labels = resolveSubcategoryOptions(type, {
      stored: storedSubcategories.value,
      discovered: discoveredSubcategories.value,
      categoryLabels: categoryLabels.value,
      mainCategoryAliases: mainCategoryAliases.value,
    })
    const keys = new Set(labels.map(normalizeKey))
    if (selection.some((entry) => keys.has(normalizeKey(entry)))) return type
  }
  return ''
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

function formatYearLong(year: number) {
  return String(year)
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

function goToPage(index: number) {
  activePage.value = Math.max(0, Math.min(pages.length - 1, index))
}

function setPeriodMode(mode: 'month' | 'year') {
  selectedPeriodMode.value = mode
}

function commitYear(value: unknown = yearDraft.value) {
  const digits = String(value ?? '').replace(/[^\d]/g, '').slice(0, 4)
  const parsed = Math.trunc(Number(digits || selectedYear.value))
  const nextYear = Number.isFinite(parsed)
    ? Math.max(minYear.value, Math.min(maxYear.value, parsed))
    : selectedYear.value
  const current = parseMonthKey(selectedMonthKey.value)
  current.setFullYear(nextYear)
  selectedMonthKey.value = clampMonthKey(formatMonthKey(current))
  yearDraft.value = String(selectedYear.value)
}

function changePeriod(delta: number) {
  if (selectedPeriodMode.value === 'year') {
    commitYear(selectedYear.value + delta)
    return
  }
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

function onYearInput(event: Event) {
  const input = event.target as HTMLInputElement | null
  const value = String(input?.value ?? '').replace(/[^\d]/g, '').slice(0, 4)
  yearDraft.value = value
  if (input && input.value !== value) input.value = value
  if (value.length === 4) commitYear(value)
}

function closePickerMenus() {
  isTypeMenuOpen.value = false
  isSubcategoryMenuOpen.value = false
}

function toggleScopePanel() {
  isScopeExpanded.value = !isScopeExpanded.value
  if (!isScopeExpanded.value) closePickerMenus()
}

function toggleTypeMenu() {
  isTypeMenuOpen.value = !isTypeMenuOpen.value
  if (isTypeMenuOpen.value) isSubcategoryMenuOpen.value = false
}

function toggleSubcategoryMenu() {
  if (!subcategoryOptions.value.length) return
  isSubcategoryMenuOpen.value = !isSubcategoryMenuOpen.value
  if (isSubcategoryMenuOpen.value) isTypeMenuOpen.value = false
}

function onDocumentPointerDown(event: Event) {
  const target = event.target as Node | null
  if (!target) return
  if (typePickerRef.value?.contains(target) || subcategoryPickerRef.value?.contains(target)) return
  closePickerMenus()
}

function onDocumentKeydown(event: KeyboardEvent) {
  if (event.key === 'Escape') closePickerMenus()
}

function selectType(type: string) {
  if (selectedType.value === type) return
  selectedType.value = type
  selectedSubcategories.value = []
  activePage.value = 0
  closePickerMenus()
}

function toggleSubcategory(option: SubcategoryOption) {
  const key = option.key
  const next = new Map(selectedSubcategories.value.map((value) => [normalizeKey(value), value]))
  if (next.has(key)) next.delete(key)
  else next.set(key, option.label)
  selectedSubcategories.value = Array.from(next.values())
  activePage.value = 0
}

function clearSelectedSubcategories() {
  selectedSubcategories.value = []
  isSubcategoryMenuOpen.value = false
}

function refreshStoredCategoryMetadata() {
  const userId = currentUserId()
  categoryLabels.value = readStoredItemCategories(userId)
  storedSubcategories.value = readStoredSubcategories(userId, undefined, categoryLabels.value)
}

async function loadDataset() {
  const id = ++requestId
  loading.value = true
  error.value = ''
  try {
    const [boundsResult, itemsResult] = await Promise.allSettled([
      StatsServices.dateBounds(),
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

    if (itemsResult.status === 'fulfilled') {
      rawItems.value = Array.isArray(itemsResult.value?.data) ? itemsResult.value.data : []
    } else {
      rawItems.value = []
    }

    if (boundsResult.status === 'rejected' && itemsResult.status === 'rejected') {
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

function onCategoryLabelsChange(event: Event) {
  const detail = (event as CustomEvent)?.detail || {}
  if (String(detail.userId || 'guest') !== currentUserId()) return
  refreshStoredCategoryMetadata()
}

function onSubcategoriesChange(event: Event) {
  const detail = (event as CustomEvent)?.detail || {}
  if (String(detail.userId || 'guest') !== currentUserId()) return
  refreshStoredCategoryMetadata()
}

onMounted(async () => {
  window.addEventListener('snk:item-categories-change', onCategoryLabelsChange)
  window.addEventListener('snk:item-subcategories-change', onSubcategoriesChange)
  document.addEventListener('pointerdown', onDocumentPointerDown)
  document.addEventListener('keydown', onDocumentKeydown)
  await loadDataset()
})

onBeforeUnmount(() => {
  window.removeEventListener('snk:item-categories-change', onCategoryLabelsChange)
  window.removeEventListener('snk:item-subcategories-change', onSubcategoriesChange)
  document.removeEventListener('pointerdown', onDocumentPointerDown)
  document.removeEventListener('keydown', onDocumentKeydown)
})
</script>

<style scoped>
.category-dashboard {
  --category-bg: #f7f4ee;
  --category-muted-bg: #fbfaf7;
  width: 100%;
  min-width: 0;
  min-height: 0;
  background: var(--category-bg);
  box-sizing: border-box;
}

.category-state,
.category-scope-card__body,
.category-page-nav,
.category-panel,
.category-module-card {
  border: 1px solid rgba(148, 163, 184, 0.26);
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 18px 42px rgba(15, 23, 42, 0.06);
}

.category-state {
  width: min(100%, 1120px);
  margin: 0 auto;
  padding: clamp(1.4rem, 2.4vw, 2rem);
  text-align: center;
}

.category-state h2 {
  margin: 0;
  color: #111827;
  font-size: clamp(1.4rem, 2vw, 1.8rem);
  font-weight: 900;
}

.category-state p {
  margin: 0.75rem auto 0;
  max-width: 44rem;
  color: #64748b;
  line-height: 1.6;
}

.category-state button {
  margin-top: 1rem;
  min-height: 44px;
  border-radius: 14px;
  background: linear-gradient(135deg, #0f766e, #0ea5e9);
  padding: 0 1rem;
  color: #fff;
  font-weight: 800;
}

.category-state__pulse {
  width: 0.95rem;
  height: 0.95rem;
  margin: 0 auto 0.9rem;
  border-radius: 999px;
  background: linear-gradient(135deg, #14b8a6, #0ea5e9);
  box-shadow: 0 0 0 0 rgba(14, 165, 233, 0.24);
  animation: category-pulse 1.2s ease-in-out infinite;
}

.category-month {
  display: grid;
  gap: 0.6rem;
  border: 1px solid rgba(148, 163, 184, 0.24);
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.9);
  padding: 0.9rem 1rem;
}

.category-month__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
}

.category-month__meta {
  display: grid;
  gap: 0.18rem;
}

.category-month__head span {
  color: #0f172a;
  font-size: 0.8rem;
  font-weight: 900;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.category-month__head small {
  color: #0f766e;
  font-size: 0.78rem;
  font-weight: 800;
}

.category-mode-switch {
  display: inline-flex;
  align-items: center;
  gap: 0.3rem;
  border: 1px solid rgba(148, 163, 184, 0.22);
  border-radius: 999px;
  background: rgba(248, 250, 252, 0.9);
  padding: 0.24rem;
}

.category-mode-switch__button {
  min-height: 30px;
  border-radius: 999px;
  padding: 0 0.78rem;
  color: #64748b;
  font-size: 0.78rem;
  font-weight: 900;
  transition:
    background 140ms ease,
    color 140ms ease,
    box-shadow 140ms ease;
}

.category-mode-switch__button.is-active {
  background: #0f766e;
  color: #fff;
  box-shadow: 0 8px 18px rgba(15, 118, 110, 0.16);
}

.category-month__control {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: 0.6rem;
}

.category-month__control button,
.category-month__control input {
  min-height: 44px;
  border: 1px solid rgba(148, 163, 184, 0.28);
  border-radius: 14px;
  background: #fff;
  color: #0f172a;
}

.category-month__control button {
  width: 44px;
  display: inline-grid;
  place-items: center;
}

.category-month__control button:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.category-month__control input {
  width: 100%;
  padding: 0 0.9rem;
  font-size: 0.95rem;
  font-weight: 800;
}

.category-page-nav {
  position: relative;
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: 1rem;
  padding: 0.8rem 1rem;
  margin-bottom: 0.55rem;
}

.category-page-nav__arrow {
  width: 46px;
  height: 46px;
  border: 1px solid rgba(148, 163, 184, 0.24);
  border-radius: 14px;
  background: #fff;
  color: #0f172a;
}

.category-page-nav__arrow:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.category-page-nav__center {
  display: grid;
  justify-items: center;
  gap: 0.25rem;
  min-width: 0;
}

.category-page-nav__count {
  color: #64748b;
  font-size: 0.74rem;
  font-weight: 800;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.category-page-nav__center strong {
  min-width: 0;
  color: #111827;
  font-size: 1.02rem;
  font-weight: 900;
}

.category-page-nav__dots {
  display: inline-flex;
  gap: 0.45rem;
}

.category-page-nav__dot {
  width: 0.7rem;
  height: 0.7rem;
  border-radius: 999px;
  background: rgba(148, 163, 184, 0.36);
  transition: transform 140ms ease, background 140ms ease;
}

.category-page-nav__dot.is-active {
  transform: scale(1.08);
  background: #0f766e;
}

.category-page-nav__scope-toggle {
  position: absolute;
  left: 50%;
  bottom: -18px;
  transform: translateX(-50%);
  width: 38px;
  height: 38px;
  border: 1px solid rgba(148, 163, 184, 0.24);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.98);
  color: #0f172a;
  box-shadow: 0 12px 26px rgba(15, 23, 42, 0.08);
  z-index: 2;
  transition:
    transform 160ms ease,
    border-color 160ms ease,
    box-shadow 160ms ease;
}

.category-page-nav__scope-toggle.is-open {
  border-color: rgba(15, 118, 110, 0.28);
  transform: translateX(-50%) rotate(180deg);
  box-shadow: 0 14px 28px rgba(15, 118, 110, 0.14);
}

.category-scope-card {
  display: grid;
  gap: 0;
  padding: 0;
  overflow: visible;
}

.category-scope-card__launcher,
.category-scope-handle {
  display: none !important;
}

.category-scope-card__body {
  display: grid;
  gap: 0.8rem;
  padding: clamp(1.2rem, 1.6vw, 1.35rem) clamp(0.95rem, 1.4vw, 1.2rem)
    clamp(0.95rem, 1.4vw, 1.2rem);
}

.category-scope-card__head {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 0.7rem;
}

.category-scope-card__meta {
  display: inline-flex;
  align-items: center;
  gap: 0.65rem;
  color: #64748b;
  font-size: 0.84rem;
  font-weight: 700;
}

.category-scope-card__head p,
.category-page__heading p,
.category-panel__head p {
  margin: 0;
  color: #0f766e;
  font-size: 0.74rem;
  font-weight: 900;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.category-scope-card__head h2,
.category-page__heading h2,
.category-panel__head h2 {
  margin: 0.25rem 0 0;
  color: #111827;
  font-size: clamp(1.05rem, 1.35vw, 1.28rem);
  line-height: 1.04;
  font-weight: 900;
}

.category-scope-card__reset {
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
  min-height: 40px;
  border-radius: 999px;
  border: 1px solid rgba(148, 163, 184, 0.26);
  background: #fff;
  padding: 0 0.82rem;
  color: #0f172a;
  font-size: 0.8rem;
  font-weight: 800;
}

.category-toolbar {
  display: grid;
  grid-template-columns: minmax(0, 0.94fr) minmax(0, 1.06fr);
  gap: 0.8rem;
}

.category-picker-field {
  position: relative;
  min-width: 0;
}

.category-picker-field__label {
  display: block;
  margin-bottom: 0.42rem;
  color: #0f766e;
  font-size: 0.7rem;
  font-weight: 900;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.category-picker {
  width: 100%;
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto auto;
  align-items: center;
  gap: 0.7rem;
  min-height: 56px;
  border-radius: 18px;
  border: 1px solid rgba(148, 163, 184, 0.28);
  background: #fff;
  padding: 0.72rem 0.88rem;
  color: #0f172a;
  font-weight: 800;
  text-align: left;
  transition:
    border-color 140ms ease,
    background 140ms ease,
    color 140ms ease,
    box-shadow 140ms ease,
    transform 140ms ease;
}

.category-picker:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.category-picker.is-open,
.category-picker:hover:not(:disabled) {
  border-color: rgba(15, 118, 110, 0.36);
  background: linear-gradient(135deg, rgba(20, 184, 166, 0.14), rgba(14, 165, 233, 0.12));
  box-shadow: 0 12px 24px rgba(15, 118, 110, 0.12);
  transform: translateY(-1px);
}

.category-picker__copy {
  display: grid;
  gap: 0.12rem;
  min-width: 0;
}

.category-picker__copy small {
  color: #64748b;
  font-size: 0.72rem;
  font-weight: 800;
}

.category-picker__copy strong {
  min-width: 0;
  color: #111827;
  font-size: 1rem;
  font-weight: 900;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.category-picker__meta {
  color: #64748b;
  font-size: 0.78rem;
  font-weight: 800;
  white-space: nowrap;
}

.category-picker-menu {
  position: absolute;
  top: calc(100% + 0.55rem);
  left: 0;
  right: 0;
  z-index: 30;
  display: grid;
  gap: 0.55rem;
  border: 1px solid rgba(148, 163, 184, 0.24);
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 18px 40px rgba(15, 23, 42, 0.12);
  padding: 0.72rem;
  backdrop-filter: blur(12px);
}

.category-picker-menu--type {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.category-picker-menu--subcategory {
  grid-template-columns: repeat(2, minmax(0, 1fr));
  max-height: min(320px, 52vh);
  overflow: auto;
}

.category-picker-option {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.8rem;
  min-height: 52px;
  border: 1px solid rgba(148, 163, 184, 0.24);
  border-radius: 16px;
  background: #fff;
  padding: 0.72rem 0.82rem;
  text-align: left;
  transition:
    border-color 140ms ease,
    background 140ms ease,
    box-shadow 140ms ease;
}

.category-picker-option > div {
  display: grid;
  gap: 0.12rem;
  min-width: 0;
}

.category-picker-option span {
  min-width: 0;
  color: #111827;
  font-size: 0.96rem;
  font-weight: 900;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.category-picker-option small {
  color: #64748b;
  font-size: 0.74rem;
  font-weight: 700;
}

.category-picker-option.is-active,
.category-picker-option:hover {
  border-color: rgba(15, 118, 110, 0.34);
  background: linear-gradient(135deg, rgba(20, 184, 166, 0.14), rgba(14, 165, 233, 0.08));
  box-shadow: 0 10px 22px rgba(15, 118, 110, 0.1);
}

.category-active-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.55rem;
}

.category-active-row__label {
  color: #64748b;
  font-size: 0.74rem;
  font-weight: 900;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.category-active-row__chips {
  display: flex;
  flex-wrap: wrap;
  gap: 0.45rem;
}

.category-active-chip {
  display: inline-flex;
  align-items: center;
  min-height: 1.95rem;
  border-radius: 999px;
  background: rgba(15, 118, 110, 0.08);
  padding: 0.16rem 0.72rem;
  color: #0f766e;
  font-size: 0.78rem;
  font-weight: 800;
}

.category-active-chip--count {
  background: rgba(148, 163, 184, 0.14);
  color: #475569;
}

.category-page__heading span,
.category-panel__head span,
.category-scope-card__summary {
  color: #64748b;
  font-size: 0.88rem;
  line-height: 1.45;
}

.category-scope-card__summary {
  margin: 0;
}

.category-scope-slide-enter-active,
.category-scope-slide-leave-active {
  transition:
    opacity 180ms ease,
    transform 180ms ease;
  transform-origin: top center;
}

.category-scope-slide-enter-from,
.category-scope-slide-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}

.category-page {
  display: grid;
  gap: 1rem;
}

.category-page__heading,
.category-panel__head {
  display: flex;
  flex-wrap: wrap;
  align-items: start;
  justify-content: space-between;
  gap: 0.8rem;
}

.category-kpi-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(210px, 1fr));
  gap: 0.9rem;
}

.category-main-grid,
.category-detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 1rem;
}

.category-detail-grid--bottom {
  align-items: start;
}

.category-panel {
  display: grid;
  gap: 0.9rem;
  padding: clamp(1rem, 1.6vw, 1.35rem);
}

.category-chart {
  min-height: 320px;
}

.category-mini-empty {
  display: grid;
  place-items: center;
  min-height: 220px;
  border-radius: 18px;
  background: var(--category-muted-bg);
  color: #64748b;
  text-align: center;
  padding: 1rem;
}

.category-module-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 0.9rem;
}

.category-module-card {
  display: grid;
  gap: 0.45rem;
  padding: 1rem 1.05rem;
}

.category-module-card span {
  color: #0f766e;
  font-size: 0.72rem;
  font-weight: 900;
  letter-spacing: 0.1em;
  text-transform: uppercase;
}

.category-module-card h3 {
  margin: 0;
  color: #111827;
  font-size: 1.02rem;
  font-weight: 900;
}

.category-module-card strong {
  color: #111827;
  font-size: clamp(1.2rem, 1.8vw, 1.8rem);
  font-weight: 900;
}

.category-module-card p {
  margin: 0;
  color: #64748b;
  line-height: 1.55;
}

.category-module-card.is-profit strong,
.category-module-card.is-positive strong {
  color: #047857;
}

.category-module-card.is-warning strong {
  color: #c2410c;
}

.category-table-scroll {
  overflow: auto;
}

.category-table {
  width: 100%;
  min-width: 0;
  border-collapse: collapse;
}

.category-table th,
.category-table td {
  padding: 0.85rem 0.5rem;
  border-bottom: 1px solid rgba(226, 232, 240, 0.82);
  vertical-align: top;
  text-align: left;
}

.category-table th {
  color: #64748b;
  font-size: 0.74rem;
  font-weight: 900;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.category-table td {
  color: #0f172a;
  font-size: 0.92rem;
}

.category-table strong,
.category-table td span {
  display: block;
}

.category-table td span {
  margin-top: 0.2rem;
  color: #64748b;
  font-size: 0.8rem;
}

.category-age {
  display: inline-flex;
  align-items: center;
  min-height: 2rem;
  border-radius: 999px;
  background: rgba(15, 118, 110, 0.08);
  padding: 0.2rem 0.65rem;
  color: #0f766e;
  font-size: 0.8rem;
  font-weight: 800;
}

.category-age.is-old {
  background: rgba(234, 88, 12, 0.12);
  color: #c2410c;
}

.category-segment-list {
  display: grid;
  gap: 0.7rem;
}

.category-segment-card {
  display: flex;
  align-items: start;
  justify-content: space-between;
  gap: 1rem;
  border: 1px solid rgba(226, 232, 240, 0.9);
  border-radius: 18px;
  background: var(--category-muted-bg);
  padding: 0.9rem 1rem;
}

.category-segment-card strong {
  display: block;
  color: #111827;
  font-size: 0.98rem;
  font-weight: 900;
}

.category-segment-card span,
.category-segment-card small {
  color: #64748b;
}

.category-segment-card__values {
  display: grid;
  justify-items: end;
  gap: 0.15rem;
  text-align: right;
}

.category-segment-card__values b {
  color: #047857;
  font-size: 1rem;
  font-weight: 900;
}

.is-positive {
  color: #047857;
}

.is-negative {
  color: #c2410c;
}

@keyframes category-pulse {
  0% {
    box-shadow: 0 0 0 0 rgba(14, 165, 233, 0.24);
  }
  100% {
    box-shadow: 0 0 0 12px rgba(14, 165, 233, 0);
  }
}

@media (min-width: 961px) {
  .category-dashboard {
    padding-left: clamp(5.4rem, 6vw, 6.4rem);
    padding-right: clamp(0.4rem, 1vw, 0.9rem);
  }

  .category-dashboard :deep(.dashboard-layout__inner) {
    width: min(100%, 1760px);
    padding-top: 0.85rem;
    padding-right: clamp(0.9rem, 1.2vw, 1.3rem);
    padding-bottom: 1rem;
    padding-left: clamp(0.9rem, 1.2vw, 1.25rem);
    gap: 0.9rem;
  }

  .category-dashboard :deep(.dashboard-layout__header) {
    grid-template-columns: minmax(0, 1fr) minmax(300px, 400px);
    gap: 0.95rem;
    align-items: start;
  }

  .category-dashboard :deep(.dashboard-layout__meta) {
    margin-bottom: 0.38rem;
  }

  .category-dashboard :deep(.dashboard-layout__kicker) {
    font-size: 0.72rem;
    letter-spacing: 0.14em;
  }

  .category-dashboard :deep(.dashboard-layout__period) {
    min-height: 1.75rem;
    padding: 0.18rem 0.68rem;
    font-size: 0.72rem;
  }

  .category-dashboard :deep(.dashboard-layout__copy h1) {
    font-size: clamp(1.9rem, 2.35vw, 2.65rem);
    line-height: 0.94;
    letter-spacing: -0.05em;
  }

  .category-dashboard :deep(.dashboard-layout__description) {
    margin-top: 0.46rem;
    max-width: 50rem;
    font-size: 0.92rem;
    line-height: 1.35;
  }

  .category-dashboard :deep(.dashboard-layout__selector) {
    width: min(100%, 400px);
  }

  .category-month {
    gap: 0.5rem;
    padding: 0.78rem 0.88rem;
    border-radius: 20px;
  }

  .category-month__head span {
    font-size: 0.74rem;
  }

  .category-month__head small {
    font-size: 0.72rem;
  }

  .category-mode-switch__button {
    min-height: 28px;
    padding: 0 0.68rem;
    font-size: 0.74rem;
  }

  .category-month__control {
    gap: 0.5rem;
  }

  .category-month__control button,
  .category-month__control input {
    min-height: 40px;
    border-radius: 13px;
  }

  .category-month__control button {
    width: 40px;
  }

  .category-month__control input {
    padding: 0 0.8rem;
    font-size: 0.9rem;
  }

  .category-page-nav {
    padding: 0.68rem 0.88rem;
    gap: 0.8rem;
    border-radius: 20px;
  }

  .category-page-nav__arrow {
    width: 42px;
    height: 42px;
  }

  .category-page-nav__count {
    font-size: 0.68rem;
  }

  .category-page-nav__center strong {
    font-size: 0.94rem;
  }

  .category-scope-card__body,
  .category-panel {
    padding: 1rem 1.08rem;
  }

  .category-toolbar {
    grid-template-columns: minmax(0, 1fr);
  }
}

@media (max-width: 1180px) {
  .category-main-grid,
  .category-detail-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 760px) {
  .category-scope-card__launcher {
    justify-content: stretch;
  }

  .category-scope-handle {
    width: 100%;
    min-width: 0;
  }

  .category-picker-menu {
    position: static;
    margin-top: 0.5rem;
  }

  .category-picker-menu--type,
  .category-picker-menu--subcategory {
    grid-template-columns: 1fr;
    max-height: none;
  }

  .category-month__head {
    align-items: start;
  }

  .category-mode-switch {
    width: fit-content;
  }

  .category-page-nav {
    grid-template-columns: 1fr;
  }

  .category-page-nav__center {
    order: -1;
  }

  .category-page-nav__arrow {
    width: 100%;
  }

  .category-month__control {
    grid-template-columns: 1fr;
  }

  .category-month__control button {
    width: 100%;
  }

  .category-chart {
    min-height: 260px;
  }
}
</style>
