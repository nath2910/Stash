<template>
  <div class="gestion-page-light text-slate-900">
    <div class="relative w-full app-page-stack gestion-page-stack">
      <header
        class="gestion-hero-panel"
        :class="{
          'is-admin-context': activeGestionTab === 'admin',
          'is-delivery-context': activeGestionTab === 'delivery',
        }"
      >
        <div class="flex flex-wrap items-end justify-between gap-4">
          <div class="space-y-2">
            <p class="gestion-eyebrow">{{ gestionHero.eyebrow }}</p>
            <h1 class="gestion-title">{{ gestionHero.title }}</h1>
            <p class="gestion-subtitle">
              {{ gestionHero.subtitle }}
            </p>
          </div>
          <div class="gestion-hero-pills">
            <span v-for="pill in gestionHero.pills" :key="pill">{{ pill }}</span>
          </div>
        </div>
      </header>

      <div class="gestion-tab-shell">
        <nav class="gestion-tab-nav" aria-label="Sous-onglets gestion">
          <button
            type="button"
            class="gestion-tab-button"
            :class="{ 'is-active': activeGestionTab === 'inventory' }"
            @click="setGestionTab('inventory')"
          >
            <PackageSearch class="h-4 w-4" aria-hidden="true" />
            <span>
              <strong>Inventaire</strong>
              <small>Items et stock</small>
            </span>
          </button>
          <button
            type="button"
            class="gestion-tab-button"
            :class="{ 'is-active': activeGestionTab === 'delivery' }"
            @click="setGestionTab('delivery')"
          >
            <Truck class="h-4 w-4" aria-hidden="true" />
            <span>
              <strong>Suivi livraison</strong>
              <small>Colis et mails</small>
            </span>
          </button>
          <button
            type="button"
            class="gestion-tab-button"
            :class="{ 'is-active': activeGestionTab === 'admin' }"
            @click="setGestionTab('admin')"
          >
            <ClipboardList class="h-4 w-4" aria-hidden="true" />
            <span>
              <strong>Administratif</strong>
              <small>Declaration et docs</small>
            </span>
          </button>
        </nav>
      </div>

      <Transition name="gestion-tab-view" mode="out-in">
        <div :key="activeGestionTab">
          <section v-if="activeGestionTab === 'inventory'" class="space-y-5">
            <section class="gestion-summary-panel relative z-20" aria-label="Resume inventaire">
              <div class="command-panel-grid">
                <StockSummaryRow
                  :total-paires="totalPaires"
                  :nb-en-stock="nbEnStock"
                  :valeur-stock="valeurStock"
                />
              </div>
            </section>

            <!-- Tableau -->
            <div class="inventory-list-panel relative z-0">
              <div class="inventory-sticky-tools" :class="{ 'is-condensed': hasScrolledInventory }">
                <div class="inventory-toolbar">
                  <div class="inventory-toolbar-copy">
                    <h2>Liste des items</h2>
                    <p>
                      {{ filteredVentes.length }} resultat(s)
                      <span v-if="hasMoreFilteredVentes">
                        - {{ visibleFilteredVentes.length }} affiche(s)
                      </span>
                      <span v-if="selectedIds.length">
                        - {{ selectedIds.length }} selectionne(s)
                      </span>
                    </p>
                  </div>

                  <div class="inventory-toolbar-actions">
                    <GestionActionsPanel
                      :items="snkVentes"
                      @vente-ajoutee="handleVenteAjouteeFromActions"
                    />
                    <button
                      type="button"
                      class="inventory-danger-button"
                      @click="openDeleteFromActions"
                    >
                      {{
                        selectedIds.length
                          ? `Supprimer (${selectedIds.length})`
                          : 'Supprimer un item'
                      }}
                    </button>
                  </div>
                </div>

                <div class="inventory-control-row">
                  <SearchBarre v-model="searchTerm" />
                  <button
                    type="button"
                    class="filter-panel-toggle"
                    :class="{ 'is-active': filtersPanelOpen || activeFilterCount }"
                    aria-controls="gestion-filter-panel"
                    :aria-expanded="filtersPanelOpen"
                    @click="filtersPanelOpen = !filtersPanelOpen"
                  >
                    <SlidersHorizontal class="h-4 w-4" aria-hidden="true" />
                    <span>{{ filtersPanelOpen ? 'Masquer' : 'Filtres' }}</span>
                    <span v-if="activeFilterCount" class="filter-toggle-badge">
                      {{ activeFilterCount }}
                    </span>
                  </button>
                  <button
                    type="button"
                    class="filter-reset-button filter-reset-button--inline"
                    :disabled="!hasActiveFilters"
                    :title="hasActiveFilters ? 'Reinitialiser les filtres' : 'Aucun filtre actif'"
                    :aria-label="
                      hasActiveFilters ? 'Reinitialiser les filtres' : 'Aucun filtre actif'
                    "
                    @click="resetFilters"
                  >
                    <RotateCcw class="h-3.5 w-3.5" />
                    <span class="filter-reset-text">Reset</span>
                  </button>
                </div>

                <div
                  class="inventory-filter-shell"
                  :class="{ 'is-open': filtersPanelOpen, 'has-active': activeFilterCount }"
                >
                  <div
                    id="gestion-filter-panel"
                    class="filter-compact-grid"
                    :class="{ 'is-open': filtersPanelOpen }"
                  >
                    <button
                      type="button"
                      class="filter-reset-button filter-reset-button--panel"
                      :disabled="!hasActiveFilters"
                      :title="hasActiveFilters ? 'Reinitialiser les filtres' : 'Aucun filtre actif'"
                      :aria-label="
                        hasActiveFilters ? 'Reinitialiser les filtres' : 'Aucun filtre actif'
                      "
                      @click="resetFilters"
                    >
                      <RotateCcw class="h-3.5 w-3.5" />
                      <span class="filter-reset-text">Reset</span>
                      <span v-if="activeFilterCount" class="filter-count-badge">
                        {{ activeFilterCount }}
                      </span>
                    </button>

                    <GestionFilterDropdown
                      v-model="filters.itemType"
                      label="Type item"
                      :options="filterItemTypeOptions"
                      icon-mode="type"
                    />

                    <GestionFilterDropdown
                      v-model="filters.category"
                      label="Sous-categorie"
                      :options="filterCategoryOptions"
                      :disabled="!selectedItemType"
                      icon-mode="subcategory"
                      :placeholder="selectedItemType ? 'Toutes' : 'Choisir un type'"
                    />

                    <label class="filter-field filter-field--status">
                      <span>Statut</span>
                      <select v-model="filters.status" class="filter-control">
                        <option
                          v-for="option in statusOptions"
                          :key="option.value"
                          :value="option.value"
                        >
                          {{ option.label }}
                        </option>
                      </select>
                    </label>

                    <label class="filter-field">
                      <span>Tri</span>
                      <select v-model="filters.sort" class="filter-control">
                        <option
                          v-for="option in sortOptions"
                          :key="option.value"
                          :value="option.value"
                        >
                          {{ option.label }}
                        </option>
                      </select>
                    </label>

                    <section class="date-range-compact">
                      <div class="date-range-title">
                        <CalendarDays class="h-3.5 w-3.5" />
                        <span>Achat</span>
                      </div>
                      <div class="date-range-inputs">
                        <CompactDateInput
                          v-model="filters.purchaseFrom"
                          size="md"
                          aria-label="Date d'achat debut"
                        />
                        <span class="date-range-separator" aria-hidden="true">-</span>
                        <CompactDateInput
                          v-model="filters.purchaseTo"
                          size="md"
                          aria-label="Date d'achat fin"
                        />
                      </div>
                    </section>

                    <section class="date-range-compact">
                      <div class="date-range-title">
                        <CalendarDays class="h-3.5 w-3.5" />
                        <span>Vente</span>
                      </div>
                      <div class="date-range-inputs">
                        <CompactDateInput
                          v-model="filters.saleFrom"
                          size="md"
                          aria-label="Date de vente debut"
                        />
                        <span class="date-range-separator" aria-hidden="true">-</span>
                        <CompactDateInput
                          v-model="filters.saleTo"
                          size="md"
                          aria-label="Date de vente fin"
                        />
                      </div>
                    </section>
                  </div>
                </div>
              </div>

              <div class="inventory-list-body">
                <div
                  ref="inventoryScrollRef"
                  class="inventory-list-scroll"
                  @scroll.passive="handleInventoryScroll"
                >
                  <afficherTout
                    :snkVentes="visibleFilteredVentes"
                    selectable
                    v-model="selectedIds"
                    @edit="openEditModal"
                    @delete="requestDeleteItems"
                  />
                  <div v-if="hasMoreFilteredVentes" class="inventory-load-more">
                    <button type="button" @click="loadMoreFilteredVentes">
                      Afficher plus
                      <span>{{ visibleFilteredVentes.length }} / {{ filteredVentes.length }}</span>
                    </button>
                  </div>
                </div>
              </div>
            </div>
          </section>

          <DeliveryTrackingPanel v-else-if="activeGestionTab === 'delivery'" />
          <AdminPage v-else embedded />
        </div>
      </Transition>

      <template v-if="activeGestionTab === 'inventory'">
        <!-- Edition -->
        <EditVenteModal
          v-model="showEditModal"
          :vente="venteToEdit"
          :items="snkVentes"
          @saved="handleVenteUpdated"
        />

        <!-- Delete modal (unique) -->
        <SupprimerModal
          v-if="showDeleteModal"
          :snkVentes="snkVentes"
          :selectedIds="selectedIds"
          :defaultMode="deleteMode"
          @close="showDeleteModal = false"
          @delete-requested="requestDeleteItems"
        />
        <div class="gestion-import-widget">
          <CsvImportExportWidget :filteredRows="filteredVentes" @imported="reloadVentes" />
        </div>

        <Transition name="back-to-top">
          <button
            v-if="showBackToTop"
            type="button"
            class="gestion-back-to-top"
            aria-label="Retour en haut de la liste"
            @click="scrollInventoryToTop"
          >
            <ArrowUp class="h-4 w-4" aria-hidden="true" />
          </button>
        </Transition>

        <TransitionGroup name="gestion-undo-toast" tag="div" class="gestion-undo-stack">
          <article
            v-for="toast in pendingDeleteToasts"
            :key="toast.id"
            class="gestion-undo-toast"
            :class="`is-${toast.state}`"
          >
            <div>
              <p>{{ toast.title }}</p>
              <span>{{ toast.message }}</span>
            </div>
            <button
              v-if="toast.state === 'pending'"
              type="button"
              @click="undoPendingDelete(toast.id)"
            >
              Annuler
            </button>
          </article>
        </TransitionGroup>
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, computed, watch, defineAsyncComponent } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  ArrowUp,
  CalendarDays,
  ClipboardList,
  PackageSearch,
  RotateCcw,
  SlidersHorizontal,
  Truck,
} from 'lucide-vue-next'
import { useAuthStore } from '@/store/authStore'
import SnkVenteServices from '@/services/SnkVenteServices.js'
import StockSummaryRow from '@/components/gestion/GestionRésumeStock.vue'
import SearchBarre from '@/components/gestion/GestionSearchBarre.vue'
import GestionActionsPanel from '@/components/gestion/GestionBlocBoutonAddDelete.vue'
import afficherTout from '@/components/gestion/GestionAfficherTout.vue'
import GestionFilterDropdown from '@/components/gestion/GestionFilterDropdown.vue'
import CompactDateInput from '@/components/ui/CompactDateInput.vue'
import { getField, isVendue, prixRetailOf } from '@/utils/snkVente'
import { normalizeSearchText, searchTokens } from '@/utils/homeDashboard'
import { METADATA_FIELDS } from '@/RegleItem/CategorieItem'
import {
  buildItemCategoryAliases,
  itemTypeLabel,
  normalizeItemType,
  readStoredItemCategories,
  resolveItemTypeOptions,
} from '@/RegleItem/itemCategoryStore'
import {
  extractSubcategoriesByType,
  readStoredSubcategories,
  resolveSubcategoryOptions,
} from '@/RegleItem/subcategoryStore'

const EditVenteModal = defineAsyncComponent(() => import('@/components/gestion/GestionModifierItem.vue'))
const SupprimerModal = defineAsyncComponent(() => import('@/components/gestion/GestionSupprimerModal.vue'))
const CsvImportExportWidget = defineAsyncComponent(() => import('@/components/gestion/CsvImportExportWidget.vue'))
const DeliveryTrackingPanel = defineAsyncComponent(() => import('@/components/gestion/DeliveryTrackingPanel.vue'))
const AdminPage = defineAsyncComponent(() => import('@/pages/adminPage.vue'))

const snkVentes = ref([])
const searchTerm = ref('')
const selectedIds = ref([])

const showEditModal = ref(false)
const venteToEdit = ref(null)

const showDeleteModal = ref(false)
const deleteMode = ref('name') // 'name' | 'bulk'
const pendingOpenItemId = ref(null)

const { user } = useAuthStore()
const currentUser = user
const currentUserId = computed(() => currentUser.value?.id ?? 'guest')
const categoryLabels = ref(readStoredItemCategories(currentUserId.value))
const storedSubcategories = ref(
  readStoredSubcategories(currentUserId.value, undefined, categoryLabels.value),
)
const route = useRoute()
const router = useRouter()
const tabFromRoute = () => {
  if (route.query?.tab === 'delivery') return 'delivery'
  if (route.query?.tab === 'admin') return 'admin'
  return 'inventory'
}
const activeGestionTab = ref(tabFromRoute())
const filtersPanelOpen = ref(false)
const inventoryScrollRef = ref(null)
const showBackToTop = ref(false)
const hasScrolledInventory = ref(false)
const renderLimit = ref(80)
const pendingDeleteToasts = ref([])
const pendingDeleteTimers = new Map()
let pendingDeleteSequence = 0

const EMPTY_CATEGORY_VALUE = '__empty_category__'
const INITIAL_RENDER_LIMIT = 80
const RENDER_BATCH_SIZE = 80
const DELETE_UNDO_DELAY_MS = 5200
const MAX_SEARCH_WORDS = 120
const SEARCH_EXTRA_FIELDS = [
  'marque',
  'brand',
  'tags',
  'tag',
  'labels',
  'label',
  'size',
  'taille',
  'pointure',
  'sku',
  'reference',
  'colorway',
  'couleur',
  'condition',
  'status',
  'statut',
  'venue',
  'lieu',
]

const statusOptions = [
  { value: 'all', label: 'Tous' },
  { value: 'stock', label: 'En stock' },
  { value: 'sold', label: 'Vendus' },
]

const sortOptions = [
  { value: 'none', label: 'Defaut' },
  { value: 'az', label: 'A -> Z' },
  { value: 'za', label: 'Z -> A' },
]

const setGestionTab = (tab) => {
  const nextTab = tab === 'delivery' || tab === 'admin' ? tab : 'inventory'
  activeGestionTab.value = nextTab
  filtersPanelOpen.value = false

  const nextQuery = { ...route.query }
  if (nextTab === 'delivery' || nextTab === 'admin') {
    nextQuery.tab = nextTab
  } else {
    delete nextQuery.tab
  }
  router.replace({ query: nextQuery }).catch(() => {})
}

const emptyFilters = () => ({
  itemType: 'all',
  category: 'all',
  status: 'all',
  sort: 'none',
  purchaseFrom: '',
  purchaseTo: '',
  saleFrom: '',
  saleTo: '',
})

const filters = ref(emptyFilters())
const debouncedSearchTerm = ref('')
let searchDebounceTimer = null

const normalizeText = (value) => normalizeSearchText(value)
const compactText = (value) => normalizeText(value).replace(/\s+/g, '')

const appendSearchPart = (parts, value) => {
  if (value === null || value === undefined || value === '') return
  if (Array.isArray(value)) {
    value.forEach((item) => appendSearchPart(parts, item))
    return
  }
  if (typeof value === 'object') {
    Object.entries(value).forEach(([key, nestedValue]) => {
      appendSearchPart(parts, key)
      appendSearchPart(parts, nestedValue)
    })
    return
  }
  parts.push(String(value))
}

const toDateKey = (value) => {
  if (!value) return ''
  if (value instanceof Date && !Number.isNaN(value.getTime())) {
    const y = value.getFullYear()
    const m = String(value.getMonth() + 1).padStart(2, '0')
    const d = String(value.getDate()).padStart(2, '0')
    return `${y}-${m}-${d}`
  }

  const text = String(value).trim()
  const match = text.match(/^(\d{4})-(\d{2})-(\d{2})/)
  if (match) return `${match[1]}-${match[2]}-${match[3]}`

  const parsed = new Date(text)
  if (Number.isNaN(parsed.getTime())) return ''
  const y = parsed.getFullYear()
  const m = String(parsed.getMonth() + 1).padStart(2, '0')
  const d = String(parsed.getDate()).padStart(2, '0')
  return `${y}-${m}-${d}`
}

const normalizeDateRange = (from, to) => {
  const fromKey = toDateKey(from)
  const toKey = toDateKey(to)
  if (fromKey && toKey && fromKey > toKey) {
    return { from: toKey, to: fromKey }
  }
  return { from: fromKey, to: toKey }
}

const dateInRange = (value, from, to) => {
  if (!from && !to) return true
  const key = toDateKey(value)
  if (!key) return false
  if (from && key < from) return false
  if (to && key > to) return false
  return true
}

const categoryValueOf = (vente) => {
  const raw = String(vente?.categorie ?? '').trim()
  if (isMainCategoryAliasValue(raw)) return EMPTY_CATEGORY_VALUE
  return raw || EMPTY_CATEGORY_VALUE
}

const itemTypeOptions = computed(() => resolveItemTypeOptions(categoryLabels.value))
const filterItemTypeOptions = computed(() => [
  { value: 'all', label: 'Tous' },
  ...itemTypeOptions.value.map((option) => ({
    value: option.value,
    label: option.label,
    detail: option.label !== option.defaultLabel ? option.defaultLabel : '',
  })),
])
const selectedItemType = computed(() => {
  const raw = filters.value.itemType
  if (!raw || raw === 'all') return ''
  const normalized = normalizeItemType(raw)
  return itemTypeOptions.value.some((option) => option.value === normalized) ? normalized : ''
})
const mainCategoryAliases = computed(() => buildItemCategoryAliases(categoryLabels.value))
const mainCategoryAliasKeys = computed(
  () => new Set(Array.from(mainCategoryAliases.value, (alias) => normalizeText(alias))),
)
const isMainCategoryAliasValue = (value) => {
  const cleaned = normalizeText(value)
  return Boolean(cleaned && mainCategoryAliasKeys.value.has(cleaned))
}
const discoveredSubcategories = computed(() =>
  extractSubcategoriesByType(snkVentes.value, categoryLabels.value),
)

const categoryOptions = computed(() => {
  if (!selectedItemType.value) return []

  const options = new Map([
    [
      EMPTY_CATEGORY_VALUE,
      {
        value: EMPTY_CATEGORY_VALUE,
        label: 'Sans sous-categorie',
      },
    ],
  ])

  for (const label of resolveSubcategoryOptions(selectedItemType.value, {
    stored: storedSubcategories.value,
    discovered: discoveredSubcategories.value,
    currentValue: filters.value.category === 'all' ? '' : filters.value.category,
    mainCategoryAliases: mainCategoryAliases.value,
    categoryLabels: categoryLabels.value,
  })) {
    const value = String(label || '').trim()
    if (!value || options.has(value)) continue
    options.set(value, { value, label: value })
  }

  return Array.from(options.values()).sort((a, b) =>
    a.label.localeCompare(b.label, 'fr', { sensitivity: 'base', numeric: true }),
  )
})

const filterCategoryOptions = computed(() => {
  if (!selectedItemType.value) return [{ value: 'all', label: "Choisir un type d'abord" }]
  return [{ value: 'all', label: 'Toutes' }, ...categoryOptions.value]
})

const buildVenteSearchRecord = (vente) => {
  const type = normalizeItemType(vente?.type || 'SNEAKER')
  const metadata = vente?.metadata && typeof vente.metadata === 'object' ? vente.metadata : {}
  const metadataFieldLabels = new Map(
    (METADATA_FIELDS[type] || []).map((field) => [field.key, field.label]),
  )
  const parts = []

  ;[
    vente?.id,
    vente?.nomItem ?? vente?.nom_item,
    vente?.categorie,
    itemTypeLabel(type, categoryLabels.value),
    type,
    vente?.description,
    getField(vente, 'prixRetail'),
    getField(vente, 'prixResell'),
    getField(vente, 'dateAchat'),
    getField(vente, 'dateVente'),
    isVendue(vente) ? 'vendu vendue vente sold' : 'stock disponible en stock',
  ].forEach((value) => appendSearchPart(parts, value))

  SEARCH_EXTRA_FIELDS.forEach((field) => appendSearchPart(parts, getField(vente, field)))

  Object.entries(metadata).forEach(([key, value]) => {
    appendSearchPart(parts, key)
    appendSearchPart(parts, metadataFieldLabels.get(key))
    appendSearchPart(parts, value)
  })

  const text = normalizeText(parts.join(' '))
  return {
    vente,
    text,
    compact: text.replace(/\s+/g, ''),
    words: Array.from(new Set(text.split(/\s+/).filter(Boolean))).slice(0, MAX_SEARCH_WORDS),
  }
}

const indexedVentes = computed(() => snkVentes.value.map((vente) => buildVenteSearchRecord(vente)))

const searchDescriptor = computed(() => {
  const query = normalizeText(debouncedSearchTerm.value)
  return {
    query,
    compact: compactText(query),
    tokens: searchTokens(query),
  }
})

const maxDistanceForToken = (token) => {
  if (token.length < 3) return 0
  if (token.length <= 5) return 1
  return 2
}

const editDistanceWithin = (a, b, maxDistance) => {
  if (!maxDistance && a !== b) return false
  if (Math.abs(a.length - b.length) > maxDistance) return false
  if (a.length > 36 || b.length > 36) return false

  let previous = Array.from({ length: b.length + 1 }, (_, index) => index)
  for (let i = 1; i <= a.length; i += 1) {
    const current = [i]
    let rowMin = current[0]

    for (let j = 1; j <= b.length; j += 1) {
      const cost = a[i - 1] === b[j - 1] ? 0 : 1
      const value = Math.min(previous[j] + 1, current[j - 1] + 1, previous[j - 1] + cost)
      current[j] = value
      rowMin = Math.min(rowMin, value)
    }

    if (rowMin > maxDistance) return false
    previous = current
  }

  return previous[b.length] <= maxDistance
}

const hasSimpleTransposition = (a, b) => {
  if (a.length !== b.length || a.length < 4 || a === b) return false

  const diffs = []
  for (let index = 0; index < a.length; index += 1) {
    if (a[index] !== b[index]) diffs.push(index)
    if (diffs.length > 2) return false
  }

  return (
    diffs.length === 2 &&
    a[diffs[0]] === b[diffs[1]] &&
    a[diffs[1]] === b[diffs[0]]
  )
}

const searchScoreForWord = (word, token) => {
  if (!word || !token) return 0
  if (word === token) return 48
  if (word.startsWith(token)) return 38
  if (word.includes(token)) return 30
  if (token.length >= 4 && token.includes(word)) return 20
  if (hasSimpleTransposition(word, token)) return 24

  const maxDistance = maxDistanceForToken(token)
  if (!maxDistance) return 0

  if (editDistanceWithin(token, word, maxDistance)) return 18

  const prefix = word.slice(0, Math.min(word.length, token.length + maxDistance))
  if (prefix.length >= token.length - maxDistance && editDistanceWithin(token, prefix, maxDistance)) {
    return 16
  }

  return 0
}

const searchScoreForRecord = (record, search) => {
  if (!search.query) return 1
  if (record.text.includes(search.query)) return 1000 + search.query.length
  if (search.compact.length >= 3 && record.compact.includes(search.compact)) {
    return 900 + search.compact.length
  }

  let score = 0

  for (const token of search.tokens) {
    if (!token) continue
    if (token.length === 1) {
      const exactShortToken = record.words.some((word) => word === token)
      if (!exactShortToken) return 0
      score += 8
      continue
    }

    if (record.text.includes(token)) {
      score += 72
      continue
    }

    if (token.length >= 3 && record.compact.includes(token)) {
      score += 56
      continue
    }

    let bestWordScore = 0
    for (const word of record.words) {
      bestWordScore = Math.max(bestWordScore, searchScoreForWord(word, token))
      if (bestWordScore >= 48) break
    }

    if (!bestWordScore) return 0
    score += bestWordScore
  }

  return score
}

const sanitizeFilters = (rawFilters) => {
  const itemType =
    rawFilters?.itemType === 'all'
      ? 'all'
      : itemTypeOptions.value.some((option) => option.value === normalizeItemType(rawFilters?.itemType))
        ? normalizeItemType(rawFilters?.itemType)
        : 'all'
  const category = rawFilters?.category || 'all'
  const purchaseRange = normalizeDateRange(rawFilters?.purchaseFrom, rawFilters?.purchaseTo)
  const saleRange = normalizeDateRange(rawFilters?.saleFrom, rawFilters?.saleTo)

  return {
    itemType,
    category:
      itemType !== 'all' &&
      (category === 'all' || categoryOptions.value.some((option) => option.value === category))
        ? category
        : 'all',
    status: statusOptions.some((option) => option.value === rawFilters?.status)
      ? rawFilters.status
      : 'all',
    sort: sortOptions.some((option) => option.value === rawFilters?.sort)
      ? rawFilters.sort
      : 'none',
    purchaseFrom: purchaseRange.from,
    purchaseTo: purchaseRange.to,
    saleFrom: saleRange.from,
    saleTo: saleRange.to,
  }
}

const venteMatchesFilters = (vente, activeFilters) => {
  const venteType = normalizeItemType(vente?.type || 'SNEAKER')
  if (activeFilters.itemType !== 'all' && venteType !== activeFilters.itemType) return false
  if (
    activeFilters.category !== 'all' &&
    normalizeText(categoryValueOf(vente)) !== normalizeText(activeFilters.category)
  )
    return false
  if (activeFilters.status === 'stock' && isVendue(vente)) return false
  if (activeFilters.status === 'sold' && !isVendue(vente)) return false
  if (
    !dateInRange(
      vente.dateAchat ?? vente.date_achat,
      activeFilters.purchaseFrom,
      activeFilters.purchaseTo,
    )
  )
    return false
  if (
    !dateInRange(vente.dateVente ?? vente.date_vente, activeFilters.saleFrom, activeFilters.saleTo)
  )
    return false
  return true
}

const sortVentes = (list, sort) => {
  if (sort !== 'az' && sort !== 'za') return list
  const direction = sort === 'az' ? 1 : -1
  return [...list].sort((a, b) => {
    const aName = String(a.nomItem ?? a.nom_item ?? '')
    const bName = String(b.nomItem ?? b.nom_item ?? '')
    return aName.localeCompare(bName, 'fr', { sensitivity: 'base', numeric: true }) * direction
  })
}

const buildFilteredVentes = () => {
  const activeFilters = sanitizeFilters(filters.value)
  const search = searchDescriptor.value
  let records = indexedVentes.value

  if (search.query) {
    records = records
      .map((record) => ({ ...record, searchScore: searchScoreForRecord(record, search) }))
      .filter((record) => record.searchScore > 0)
  }

  records = records.filter((record) => venteMatchesFilters(record.vente, activeFilters))

  if (search.query && activeFilters.sort === 'none') {
    records = [...records].sort((a, b) => (b.searchScore || 0) - (a.searchScore || 0))
  }

  const list = records.map((record) => record.vente)
  return sortVentes(list, activeFilters.sort)
}

const hasActiveFilters = computed(() => {
  const activeFilters = sanitizeFilters(filters.value)
  return (
    Boolean(normalizeText(searchTerm.value)) ||
    activeFilters.itemType !== 'all' ||
    activeFilters.category !== 'all' ||
    activeFilters.status !== 'all' ||
    activeFilters.sort !== 'none' ||
    Boolean(activeFilters.purchaseFrom || activeFilters.purchaseTo) ||
    Boolean(activeFilters.saleFrom || activeFilters.saleTo)
  )
})

const activeFilterCount = computed(() => {
  const activeFilters = sanitizeFilters(filters.value)
  return [
    Boolean(normalizeText(searchTerm.value)),
    activeFilters.itemType !== 'all',
    activeFilters.category !== 'all',
    activeFilters.status !== 'all',
    activeFilters.sort !== 'none',
    Boolean(activeFilters.purchaseFrom || activeFilters.purchaseTo),
    Boolean(activeFilters.saleFrom || activeFilters.saleTo),
  ].filter(Boolean).length
})

const chargerVentes = async () => {
  if (!currentUser.value) {
    snkVentes.value = []
    selectedIds.value = []
    return
  }

  try {
    const { data } = await SnkVenteServices.getSnkVente()
    snkVentes.value = Array.isArray(data) ? data : []
    tryOpenPendingItem()
  } catch (e) {
    console.error('Erreur chargement ventes', e)
    snkVentes.value = []
  }
}

onMounted(chargerVentes)

watch(
  () => currentUserId.value,
  (userId) => {
    categoryLabels.value = readStoredItemCategories(userId)
    storedSubcategories.value = readStoredSubcategories(userId, undefined, categoryLabels.value)
  },
)

const onCategoryLabelsChange = (event) => {
  const detail = event?.detail || {}
  if (String(detail.userId || 'guest') !== String(currentUserId.value || 'guest')) return
  categoryLabels.value = readStoredItemCategories(currentUserId.value)
  storedSubcategories.value = readStoredSubcategories(
    currentUserId.value,
    undefined,
    categoryLabels.value,
  )
}

const onSubcategoriesChange = (event) => {
  const detail = event?.detail || {}
  if (String(detail.userId || 'guest') !== String(currentUserId.value || 'guest')) return
  storedSubcategories.value = readStoredSubcategories(
    currentUserId.value,
    undefined,
    categoryLabels.value,
  )
}

watch(
  () => filters.value.itemType,
  () => {
    filters.value.category = 'all'
  },
)

watch(searchTerm, (value) => {
  const nextValue = String(value ?? '')
  if (searchDebounceTimer) {
    window.clearTimeout(searchDebounceTimer)
    searchDebounceTimer = null
  }

  if (!normalizeText(nextValue)) {
    debouncedSearchTerm.value = ''
    return
  }

  searchDebounceTimer = window.setTimeout(() => {
    debouncedSearchTerm.value = nextValue
    searchDebounceTimer = null
  }, 80)
})

onMounted(() => {
  window.addEventListener('snk:item-categories-change', onCategoryLabelsChange)
  window.addEventListener('snk:item-subcategories-change', onSubcategoriesChange)
  window.addEventListener('scroll', updateBackToTopState, { passive: true })
  updateBackToTopState()
})

onBeforeUnmount(() => {
  window.removeEventListener('snk:item-categories-change', onCategoryLabelsChange)
  window.removeEventListener('snk:item-subcategories-change', onSubcategoriesChange)
  window.removeEventListener('scroll', updateBackToTopState)
  if (searchDebounceTimer) {
    window.clearTimeout(searchDebounceTimer)
    searchDebounceTimer = null
  }
  for (const toastId of pendingDeleteTimers.keys()) {
    clearPendingDeleteTimer(toastId)
  }
})

// Stats
const inventorySummary = computed(() => {
  let stockCount = 0
  let stockValue = 0
  for (const v of snkVentes.value) {
    if (!isVendue(v)) {
      stockCount += 1
      const prix = prixRetailOf(v)
      if (!Number.isNaN(prix)) stockValue += prix
    }
  }
  return {
    totalPaires: snkVentes.value.length,
    nbEnStock: stockCount,
    valeurStock: stockValue,
  }
})
const totalPaires = computed(() => inventorySummary.value.totalPaires)
const nbEnStock = computed(() => inventorySummary.value.nbEnStock)
const valeurStock = computed(() => inventorySummary.value.valeurStock)
const gestionHero = computed(() => {
  if (activeGestionTab.value === 'admin') {
    return {
      eyebrow: 'Dossier legal',
      title: 'Declaration URSSAF et documents',
      subtitle: 'Un ecran de travail pour copier le CA a declarer, corriger les blocages et sortir les registres.',
      pills: ['Montant pret', 'Champs URSSAF', 'PDF / CSV', 'Archive'],
    }
  }

  if (activeGestionTab.value === 'delivery') {
    return {
      eyebrow: 'Suivi operationnel',
      title: 'Suivi livraison',
      subtitle: 'Centralise les colis, les mails de transporteurs et les actions a faire sur les livraisons.',
      pills: ['Colis', 'Mails', 'Suivi'],
    }
  }

  return {
    eyebrow: 'Inventaire centralise',
    title: 'Gestion',
    subtitle: "Ajoute, modifie, filtre et suis tes items dans un espace coherent avec l'accueil.",
    pills: [`${totalPaires.value} items`, `${nbEnStock.value} en stock`, 'Actions rapides'],
  }
})

// Recherche + filtres
const filteredVentes = computed(() => buildFilteredVentes())
const visibleFilteredVentes = computed(() => filteredVentes.value.slice(0, renderLimit.value))
const hasMoreFilteredVentes = computed(() => visibleFilteredVentes.value.length < filteredVentes.value.length)

const loadMoreFilteredVentes = () => {
  renderLimit.value = Math.min(renderLimit.value + RENDER_BATCH_SIZE, filteredVentes.value.length)
}

const updateBackToTopState = () => {
  const listTop = inventoryScrollRef.value?.scrollTop ?? 0
  const pageTop = window.scrollY || document.documentElement.scrollTop || 0
  hasScrolledInventory.value = listTop > 10
  showBackToTop.value = listTop > 240 || pageTop > 520
}

const handleInventoryScroll = (event) => {
  const target = event?.currentTarget
  updateBackToTopState()

  if (!target || !hasMoreFilteredVentes.value) return
  const distanceToBottom = target.scrollHeight - target.scrollTop - target.clientHeight
  if (distanceToBottom < 180) loadMoreFilteredVentes()
}

const scrollInventoryToTop = () => {
  inventoryScrollRef.value?.scrollTo({ top: 0, behavior: 'smooth' })
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

const resetFilters = () => {
  searchTerm.value = ''
  filters.value = emptyFilters()
  filtersPanelOpen.value = false
}

// Selection logique : si tu filtres, on garde seulement ce qui est visible
watch(filteredVentes, (list) => {
  renderLimit.value = Math.min(INITIAL_RENDER_LIMIT, Math.max(list.length, INITIAL_RENDER_LIMIT))
  const visible = new Set(list.map((v) => v.id))
  selectedIds.value = selectedIds.value.filter((id) => visible.has(id))
  requestAnimationFrame(updateBackToTopState)
})

// Ajout
const handleVenteAjoutee = async () => {
  await chargerVentes()
}

const handleVenteAjouteeFromActions = async () => {
  await handleVenteAjoutee()
}

// Edition
const openEditModal = (vente) => {
  venteToEdit.value = { ...vente }
  showEditModal.value = true
}

const handleVenteUpdated = (updated) => {
  const index = snkVentes.value.findIndex((v) => v.id === updated.id)
  if (index !== -1) snkVentes.value[index] = updated
}

const openDeleteFromActions = () => {
  if (selectedIds.value.length) {
    requestDeleteItems(selectedIds.value)
    return
  }

  deleteMode.value = 'name'
  showDeleteModal.value = true
}

const itemName = (vente) => String(vente?.nomItem ?? vente?.nom_item ?? 'Item').trim() || 'Item'

const clearPendingDeleteTimer = (toastId) => {
  const timer = pendingDeleteTimers.get(toastId)
  if (timer) window.clearTimeout(timer)
  pendingDeleteTimers.delete(toastId)
}

const updatePendingDeleteToast = (toastId, patch) => {
  pendingDeleteToasts.value = pendingDeleteToasts.value.map((toast) =>
    toast.id === toastId ? { ...toast, ...patch } : toast,
  )
}

const removePendingDeleteToast = (toastId) => {
  clearPendingDeleteTimer(toastId)
  pendingDeleteToasts.value = pendingDeleteToasts.value.filter((toast) => toast.id !== toastId)
}

const restoreDeletedSnapshots = (snapshots) => {
  const next = [...snkVentes.value]
  for (const snapshot of [...snapshots].sort((a, b) => a.index - b.index)) {
    if (!snapshot.vente || next.some((vente) => vente.id === snapshot.vente.id)) continue
    next.splice(Math.min(snapshot.index, next.length), 0, snapshot.vente)
  }
  snkVentes.value = next
}

const commitPendingDelete = async (toastId) => {
  const toast = pendingDeleteToasts.value.find((item) => item.id === toastId)
  if (!toast || toast.state !== 'pending') return

  clearPendingDeleteTimer(toastId)
  updatePendingDeleteToast(toastId, {
    state: 'syncing',
    message: 'Suppression en cours...',
  })

  try {
    if (toast.ids.length === 1) {
      await SnkVenteServices.supprimer(toast.ids[0])
    } else {
      const response = await SnkVenteServices.supprimerEnMasse(toast.ids)
      const deleted = response?.data?.deleted ?? toast.ids.length
      if (deleted < toast.ids.length) {
        await reloadVentes()
        updatePendingDeleteToast(toastId, {
          state: 'error',
          title: 'Suppression partielle',
          message: `${toast.ids.length - deleted} item(s) n'ont pas pu etre supprime(s).`,
        })
        window.setTimeout(() => removePendingDeleteToast(toastId), 3600)
        return
      }
    }

    removePendingDeleteToast(toastId)
  } catch (error) {
    console.error('Erreur suppression differee', error)
    restoreDeletedSnapshots(toast.snapshots)
    updatePendingDeleteToast(toastId, {
      state: 'error',
      title: 'Suppression annulee',
      message: "L'appel serveur a echoue. Les items sont revenus dans la liste.",
    })
    window.setTimeout(() => removePendingDeleteToast(toastId), 4200)
  }
}

const requestDeleteItems = (ids = []) => {
  const uniqueIds = Array.from(new Set(ids)).filter((id) => id !== null && id !== undefined)
  if (!uniqueIds.length) return

  showDeleteModal.value = false

  const idSet = new Set(uniqueIds)
  const snapshots = []
  snkVentes.value.forEach((vente, index) => {
    if (idSet.has(vente.id)) snapshots.push({ vente, index })
  })

  if (!snapshots.length) return

  const deletedIds = snapshots.map((snapshot) => snapshot.vente.id)
  const deletedSet = new Set(deletedIds)
  snkVentes.value = snkVentes.value.filter((vente) => !deletedSet.has(vente.id))
  selectedIds.value = selectedIds.value.filter((id) => !deletedSet.has(id))

  pendingDeleteSequence += 1
  const toastId = `gestion-delete-${Date.now()}-${pendingDeleteSequence}`
  const count = snapshots.length
  const title = count === 1 ? `${itemName(snapshots[0].vente)} retire` : `${count} items retires`
  const message =
    count === 1
      ? 'Suppression definitive dans quelques secondes.'
      : 'Suppression definitive de la selection dans quelques secondes.'

  pendingDeleteToasts.value = [
    {
      id: toastId,
      ids: deletedIds,
      snapshots,
      state: 'pending',
      title,
      message,
    },
    ...pendingDeleteToasts.value,
  ].slice(0, 3)

  const timer = window.setTimeout(() => {
    void commitPendingDelete(toastId)
  }, DELETE_UNDO_DELAY_MS)
  pendingDeleteTimers.set(toastId, timer)
}

const undoPendingDelete = (toastId) => {
  const toast = pendingDeleteToasts.value.find((item) => item.id === toastId)
  if (!toast || toast.state !== 'pending') return

  restoreDeletedSnapshots(toast.snapshots)
  removePendingDeleteToast(toastId)
}

const reloadVentes = async () => {
  await chargerVentes()
}

const parseOpenItemIdFromQuery = () => {
  const raw = route.query?.openItemId
  const value = Array.isArray(raw) ? raw[0] : raw
  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed : null
}

const clearOpenItemQuery = () => {
  if (!('openItemId' in route.query) && !('source' in route.query)) return
  const nextQuery = { ...route.query }
  delete nextQuery.openItemId
  delete nextQuery.source
  router.replace({ query: nextQuery }).catch(() => {})
}

const tryOpenPendingItem = () => {
  const targetId = pendingOpenItemId.value
  if (!targetId) return

  if (showEditModal.value && Number(venteToEdit.value?.id) === targetId) {
    pendingOpenItemId.value = null
    clearOpenItemQuery()
    return
  }

  const targetItem = snkVentes.value.find((v) => Number(v.id) === targetId)
  if (!targetItem) return

  searchTerm.value = ''
  resetFilters()
  selectedIds.value = [targetItem.id]
  openEditModal(targetItem)
  pendingOpenItemId.value = null
  clearOpenItemQuery()
}

watch(
  () => route.query.tab,
  () => {
    activeGestionTab.value = tabFromRoute()
    filtersPanelOpen.value = false
  },
)

watch(
  () => route.query.openItemId,
  () => {
    pendingOpenItemId.value = parseOpenItemIdFromQuery()
    tryOpenPendingItem()
  },
  { immediate: true },
)
</script>

<style scoped>
.gestion-command-panel {
  container-type: inline-size;
  overflow: visible;
}

.command-panel-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 0.75rem;
  align-items: start;
}

.command-controls {
  display: grid;
  min-width: 0;
  align-content: center;
  gap: 0.55rem;
  border: 1px solid rgba(51, 65, 85, 0.86);
  border-radius: 18px;
  background:
    linear-gradient(180deg, rgba(15, 23, 42, 0.78), rgba(15, 23, 42, 0.56)),
    rgba(2, 6, 23, 0.24);
  padding: 0.62rem;
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.04),
    0 14px 34px rgba(2, 6, 23, 0.18);
}

.command-primary-row {
  display: grid;
  grid-template-columns: 1fr;
  gap: 0.55rem;
  align-items: center;
  min-width: 0;
}

.filter-compact-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 0.45rem;
  align-items: center;
  min-width: 0;
}

.filter-panel-toggle {
  display: inline-flex;
  position: relative;
  min-height: 40px;
  align-items: center;
  justify-content: center;
  gap: 0.45rem;
  border: 1px solid rgba(71, 85, 105, 0.78);
  border-radius: 14px;
  background: rgba(15, 23, 42, 0.6);
  color: rgb(226 232 240);
  padding: 0 0.9rem;
  font-size: 12px;
  font-weight: 850;
  transition:
    border-color 140ms ease,
    background 140ms ease,
    color 140ms ease;
}

.filter-panel-toggle:hover,
.filter-panel-toggle.is-active {
  border-color: rgba(167, 139, 250, 0.6);
  background: rgba(76, 29, 149, 0.2);
  color: white;
}

.filter-toggle-badge {
  display: inline-grid;
  min-width: 18px;
  height: 18px;
  place-items: center;
  border-radius: 999px;
  background: rgb(196 181 253);
  color: rgb(30 27 75);
  font-size: 11px;
  font-weight: 900;
  line-height: 1;
  padding-inline: 0.25rem;
}

.filter-field {
  display: grid;
  flex: 1 1 176px;
  grid-template-columns: auto minmax(0, 1fr);
  align-items: center;
  min-width: 158px;
  max-width: 258px;
  height: 36px;
  overflow: hidden;
  border: 1px solid rgba(71, 85, 105, 0.82);
  border-radius: 999px;
  background: rgba(15, 23, 42, 0.54);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.035);
  transition:
    border-color 140ms ease,
    background 140ms ease,
    box-shadow 140ms ease;
}

.filter-field:focus-within {
  border-color: rgba(167, 139, 250, 0.76);
  box-shadow: 0 0 0 3px rgba(139, 92, 246, 0.15);
}

.filter-field:hover {
  border-color: rgba(100, 116, 139, 0.95);
  background: rgba(15, 23, 42, 0.72);
}

.filter-field > span {
  color: rgb(148 163 184);
  font-size: 9.5px;
  font-weight: 800;
  letter-spacing: 0.1em;
  line-height: 1;
  padding-left: 0.82rem;
  text-transform: uppercase;
  white-space: nowrap;
}

.filter-control {
  appearance: none;
  height: 34px;
  width: 100%;
  min-width: 0;
  border: 0;
  border-radius: 0;
  background:
    linear-gradient(45deg, transparent 50%, rgb(148, 163, 184) 50%) calc(100% - 14px) 15px /
      5px 5px no-repeat,
    linear-gradient(135deg, rgb(148, 163, 184) 50%, transparent 50%) calc(100% - 9px) 15px /
      5px 5px no-repeat;
  color: rgb(241 245 249);
  font-size: 12.5px;
  font-weight: 800;
  line-height: 1;
  outline: none;
  padding: 0 1.75rem 0 0.45rem;
  text-overflow: ellipsis;
}

.filter-control:disabled {
  cursor: not-allowed;
  color: rgb(100 116 139);
  background:
    linear-gradient(45deg, transparent 50%, rgb(100, 116, 139) 50%) calc(100% - 14px) 15px /
      5px 5px no-repeat,
    linear-gradient(135deg, rgb(100, 116, 139) 50%, transparent 50%) calc(100% - 9px) 15px /
      5px 5px no-repeat;
}

.filter-control:hover {
  background-color: transparent;
}

.filter-control:focus {
  box-shadow: none;
}

.filter-status-group {
  display: inline-grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  width: 100%;
  max-width: 310px;
  min-width: 0;
  height: 40px;
  border: 1px solid rgba(71, 85, 105, 0.78);
  border-radius: 999px;
  background: rgba(2, 6, 23, 0.28);
  padding: 3px;
}

.filter-status-group--panel {
  display: none;
}

.filter-status-button {
  min-width: 0;
  border-radius: 999px;
  color: rgb(203 213 225);
  font-size: 12px;
  font-weight: 850;
  line-height: 1;
  overflow: hidden;
  padding: 0 0.45rem;
  text-overflow: ellipsis;
  white-space: nowrap;
  transition:
    background 140ms ease,
    color 140ms ease,
    box-shadow 140ms ease;
}

.filter-status-button:hover {
  background: rgba(30, 41, 59, 0.9);
  color: white;
}

.filter-status-button.is-active {
  background: rgb(241 245 249);
  color: rgb(15 23 42);
  box-shadow: 0 8px 18px rgba(2, 6, 23, 0.24);
}

.date-range-compact {
  display: grid;
  flex: 1 1 306px;
  grid-template-columns: auto minmax(0, 1fr);
  align-items: center;
  gap: 0.48rem;
  min-width: 278px;
  max-width: 380px;
  min-height: 36px;
  border: 1px solid rgba(71, 85, 105, 0.82);
  border-radius: 999px;
  background: rgba(15, 23, 42, 0.54);
  padding: 4px 6px 4px 0.78rem;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.035);
  transition:
    border-color 140ms ease,
    background 140ms ease;
}

.date-range-compact:hover {
  border-color: rgba(100, 116, 139, 0.95);
  background: rgba(15, 23, 42, 0.72);
}

.date-range-title {
  display: inline-flex;
  align-items: center;
  gap: 0.32rem;
  min-width: max-content;
  color: rgb(196 181 253);
  font-size: 9.5px;
  font-weight: 800;
  letter-spacing: 0.1em;
  line-height: 1;
  text-transform: uppercase;
}

.date-range-inputs {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto minmax(0, 1fr);
  align-items: center;
  gap: 0.35rem;
  min-width: 0;
}

.date-range-inputs :deep(.cd-root) {
  min-width: 0;
  width: 100%;
  gap: 0;
}

.date-range-inputs :deep(.cd-picker) {
  min-width: 0;
}

.date-range-inputs :deep(.cd-input) {
  height: 28px;
  width: 100%;
  border: 0;
  border-radius: 999px;
  background: rgba(30, 41, 59, 0.66);
  color: rgb(226 232 240);
  font-size: 11.5px;
  font-weight: 760;
  line-height: 1;
  outline: none;
  padding: 0 0.35rem;
  text-align: center;
  transition:
    background 140ms ease,
    box-shadow 140ms ease;
}

.date-range-inputs :deep(.cd-input:hover) {
  background: rgba(30, 41, 59, 0.9);
}

.date-range-inputs :deep(.cd-input:focus-visible) {
  background: rgba(30, 41, 59, 0.9);
  box-shadow: 0 0 0 2px rgba(139, 92, 246, 0.22);
}

.date-range-separator {
  color: rgb(100 116 139);
  font-size: 12px;
  font-weight: 800;
}

.filter-reset-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.4rem;
  flex: 0 0 auto;
  height: 36px;
  width: 36px;
  min-width: 36px;
  white-space: nowrap;
  border: 1px solid rgba(71, 85, 105, 0.72);
  border-radius: 999px;
  background: rgba(15, 23, 42, 0.46);
  color: rgb(203 213 225);
  font-size: 12px;
  font-weight: 800;
  line-height: 1;
  padding: 0;
  position: relative;
  transition:
    border-color 140ms ease,
    background 140ms ease,
    color 140ms ease;
}

.filter-reset-button:not(:disabled):hover {
  border-color: rgba(148, 163, 184, 0.7);
  background: rgba(30, 41, 59, 0.75);
  color: white;
}

.filter-reset-button:disabled {
  cursor: default;
  opacity: 0.42;
}

.filter-reset-text {
  position: absolute;
  width: 1px;
  height: 1px;
  overflow: hidden;
  clip: rect(0 0 0 0);
  white-space: nowrap;
}

.filter-count-badge {
  display: inline-grid;
  position: absolute;
  top: -5px;
  right: -5px;
  min-width: 17px;
  height: 17px;
  place-items: center;
  border-radius: 999px;
  background: rgb(196 181 253);
  color: rgb(30 27 75);
  font-size: 11px;
  font-weight: 900;
  line-height: 1;
}

.inventory-actions {
  display: flex;
  width: 100%;
  min-width: 0;
  justify-content: flex-end;
}

.inventory-actions-toggle {
  display: none;
  width: 100%;
  min-height: 40px;
  align-items: center;
  justify-content: center;
  gap: 0.45rem;
  border: 1px solid rgba(71, 85, 105, 0.78);
  border-radius: 999px;
  background: rgba(15, 23, 42, 0.7);
  color: rgb(226 232 240);
  font-size: 12px;
  font-weight: 850;
}

.inventory-actions-toggle:hover {
  border-color: rgba(148, 163, 184, 0.74);
  background: rgba(30, 41, 59, 0.82);
  color: white;
}

.inventory-actions-panel {
  display: flex;
  min-width: 0;
  flex-wrap: wrap;
  align-items: center;
  justify-content: flex-end;
  gap: 0.5rem;
}

.inventory-actions--list {
  justify-content: flex-end;
  border-bottom: 1px solid rgba(148, 163, 184, 0.16);
  background: #fbfaf7;
  padding: 0.78rem clamp(1rem, 2vw, 1.35rem);
}

.inventory-actions-panel--list {
  gap: 0.65rem;
}

@media (min-width: 641px) {
  .inventory-actions {
    width: auto;
  }

  .inventory-actions--list {
    width: 100%;
  }
}

@media (min-width: 760px) {
  .command-primary-row {
    grid-template-columns: minmax(320px, 1fr) minmax(248px, 310px);
  }

  .filter-status-group {
    justify-self: end;
  }
}

@media (min-width: 1180px) {
  .command-controls {
    padding: 0.68rem;
  }
}

@media (min-width: 1440px) {
  .filter-compact-grid {
    flex-wrap: nowrap;
  }

  .date-range-compact {
    flex-basis: 298px;
  }
}

@media (max-width: 920px) {
  .command-controls {
    gap: 0.5rem;
    border-radius: 18px;
    padding: 0.55rem;
  }

  .command-primary-row {
    grid-template-columns: minmax(0, 1fr) auto;
    gap: 0.5rem;
  }

  .filter-panel-toggle {
    display: inline-flex;
  }

  .filter-status-group--desktop {
    display: none;
  }

  .filter-compact-grid {
    display: none;
    max-height: none;
    overflow: visible;
    border: 1px solid rgba(71, 85, 105, 0.72);
    border-radius: 16px;
    background:
      linear-gradient(180deg, rgba(15, 23, 42, 0.86), rgba(15, 23, 42, 0.68)),
      rgba(2, 6, 23, 0.3);
    padding: 0.6rem;
    scrollbar-width: thin;
  }

  .filter-compact-grid.is-open {
    display: flex;
  }

  .filter-status-group--panel {
    display: inline-grid;
    flex: 1 1 100%;
    max-width: none;
  }

  .filter-field {
    max-width: none;
  }
}

@media (max-width: 640px) {
  .command-primary-row {
    grid-template-columns: 1fr;
  }

  .filter-panel-toggle {
    width: 100%;
  }

  .filter-status-group {
    max-width: none;
  }

  .filter-field,
  .date-range-compact,
  .filter-reset-button {
    flex: 1 1 100%;
    min-width: 0;
    max-width: none;
  }

  .filter-reset-button {
    width: 100%;
  }

  .filter-reset-text {
    position: static;
    width: auto;
    height: auto;
    clip: auto;
  }

  .date-range-compact {
    grid-template-columns: 1fr;
  }

  .inventory-actions {
    display: grid;
    justify-content: stretch;
  }

  .inventory-actions-toggle {
    display: inline-flex;
  }

  .inventory-actions-panel {
    display: none;
    margin-top: 0.55rem;
    border: 1px solid rgba(71, 85, 105, 0.7);
    border-radius: 16px;
    background: rgba(15, 23, 42, 0.66);
    padding: 0.6rem;
  }

  .inventory-actions-panel.is-open {
    display: grid;
  }

  .inventory-actions--list {
    display: block;
    width: 100%;
    padding: 0.65rem clamp(0.85rem, 1.8vw, 1.2rem);
  }

  .inventory-actions--list .inventory-actions-panel {
    display: grid;
    width: 100%;
    margin-top: 0;
    border: 0;
    background: transparent;
    padding: 0;
  }
}

@media (max-width: 420px) {
  .date-range-title {
    min-width: 0;
  }

  .date-range-inputs {
    grid-template-columns: 1fr;
  }

  .date-range-separator {
    display: none;
  }
}

.gestion-tab-view-enter-active,
.gestion-tab-view-leave-active {
  transition:
    opacity 140ms ease,
    transform 140ms ease;
}

.gestion-tab-view-enter-from {
  opacity: 0;
  transform: translateY(4px);
}

.gestion-tab-view-leave-to {
  opacity: 0;
  transform: translateY(-2px);
}

.gestion-page-light {
  --gestion-border: rgba(148, 163, 184, 0.24);
  --gestion-border-strong: rgba(14, 116, 144, 0.34);
  --gestion-text: #0f172a;
  --gestion-muted: #64748b;
  --gestion-soft: rgba(241, 245, 249, 0.86);
  --gestion-surface: rgba(255, 255, 255, 0.92);
  --gestion-shadow: 0 24px 60px rgba(15, 23, 42, 0.07), 0 10px 32px rgba(14, 165, 233, 0.06);
  background: transparent;
}

.gestion-page-stack {
  gap: clamp(1rem, 1.7vw, 1.45rem);
}

.gestion-hero-panel,
.inventory-list-panel,
.command-controls {
  position: relative;
  border: 1px solid var(--gestion-border);
  border-radius: 20px;
  background:
    linear-gradient(135deg, rgba(14, 165, 233, 0.08), transparent 42%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.97), rgba(248, 250, 252, 0.94));
  box-shadow: var(--gestion-shadow);
  backdrop-filter: blur(16px) saturate(120%);
}

.gestion-hero-panel,
.inventory-list-panel {
  overflow: hidden;
}

.gestion-hero-panel {
  padding: clamp(1.05rem, 2.4vw, 1.55rem);
}

.gestion-hero-panel.is-admin-context {
  border-radius: 14px;
  border-color: rgba(15, 118, 110, 0.26);
  background:
    linear-gradient(135deg, rgba(15, 118, 110, 0.06), transparent 38%),
    #ffffff;
  box-shadow: 0 10px 28px rgba(15, 23, 42, 0.055);
  padding: clamp(0.9rem, 1.7vw, 1.2rem);
}

.gestion-hero-panel.is-admin-context::before {
  background: linear-gradient(90deg, #0f766e, #2563eb, #64748b);
}

.gestion-hero-panel.is-admin-context .gestion-eyebrow {
  color: #0f766e;
}

.gestion-hero-panel.is-admin-context .gestion-title {
  max-width: 62rem;
  font-size: clamp(1.65rem, 2.8vw, 2.35rem);
  line-height: 1.08;
}

.gestion-hero-panel.is-admin-context .gestion-subtitle {
  max-width: 66rem;
  font-size: clamp(0.88rem, 1.1vw, 0.98rem);
  line-height: 1.42;
}

.gestion-hero-panel.is-admin-context .gestion-hero-pills span {
  min-height: 1.85rem;
  border-color: rgba(15, 118, 110, 0.2);
  background: rgba(236, 253, 245, 0.86);
  color: #115e59;
  font-size: 0.74rem;
}

.gestion-hero-panel.is-delivery-context {
  border-color: rgba(14, 116, 144, 0.24);
}

.gestion-hero-panel::before,
.inventory-list-panel::before,
.command-controls::before {
  content: '';
  position: absolute;
  inset: 0 1rem auto;
  height: 3px;
  border-radius: 999px;
  background: linear-gradient(90deg, #0ea5e9, #14b8a6, #f59e0b);
}

.gestion-eyebrow {
  color: #0369a1;
  font-size: 0.74rem;
  font-weight: 900;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.gestion-title {
  margin-top: 0.1rem;
  color: var(--gestion-text);
  font-size: clamp(1.8rem, 4vw, 3rem);
  font-weight: 800;
  letter-spacing: 0;
}

.gestion-subtitle {
  max-width: 46rem;
  color: var(--gestion-muted);
  font-size: clamp(0.92rem, 1.4vw, 1.06rem);
  font-weight: 650;
}

.gestion-hero-pills {
  display: flex;
  flex-wrap: wrap;
  gap: 0.55rem;
  justify-content: flex-end;
}

.gestion-hero-pills span {
  display: inline-flex;
  min-height: 2rem;
  align-items: center;
  border: 1px solid rgba(100, 116, 139, 0.2);
  border-radius: 999px;
  background: rgba(248, 250, 252, 0.88);
  color: #334155;
  padding: 0 0.78rem;
  font-size: 0.78rem;
  font-weight: 900;
  white-space: nowrap;
}

.gestion-tab-shell {
  display: flex;
  justify-content: center;
  overflow-x: auto;
  overflow-y: visible;
  padding-bottom: 0.25rem;
  scrollbar-width: none;
}

.gestion-tab-shell::-webkit-scrollbar {
  display: none;
}

.gestion-tab-nav {
  display: inline-flex;
  gap: 0.55rem;
  min-width: max-content;
  border: 0;
  border-radius: 999px;
  background: transparent;
  padding: 0;
  box-shadow: none;
}

.gestion-tab-button {
  min-width: 128px;
  min-height: 44px;
  border-radius: 999px;
  color: #475569;
  padding: 0.62rem 1rem;
  font-size: 0.88rem;
  font-weight: 850;
  transition:
    background 150ms ease,
    color 150ms ease,
    box-shadow 150ms ease;
}

.gestion-tab-button:hover {
  background: rgba(14, 116, 144, 0.08);
  color: #0f766e;
}

.gestion-tab-button.is-active {
  background: #0f766e;
  color: #ffffff;
  box-shadow: 0 10px 18px rgba(14, 116, 144, 0.14);
}

.command-controls {
  gap: 0.62rem;
  border-color: rgba(125, 211, 252, 0.34);
  border-radius: 20px;
  padding: 0.72rem;
  box-shadow: 0 18px 44px rgba(15, 23, 42, 0.07);
}

.filter-panel-toggle,
.filter-field,
.filter-status-group,
.date-range-compact,
.filter-reset-button,
.inventory-actions-toggle {
  border-color: rgba(100, 116, 139, 0.24);
  background: rgba(248, 250, 252, 0.92);
  color: #334155;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.8);
}

.filter-panel-toggle:hover,
.filter-panel-toggle.is-active,
.filter-field:hover,
.filter-field:focus-within,
.date-range-compact:hover,
.filter-reset-button:not(:disabled):hover,
.inventory-actions-toggle:hover {
  border-color: rgba(20, 184, 166, 0.42);
  background: rgba(241, 245, 249, 0.96);
  color: #0f766e;
  box-shadow: 0 0 0 3px rgba(45, 212, 191, 0.12);
}

.filter-toggle-badge,
.filter-count-badge {
  background: #14b8a6;
  color: #ffffff;
}

.filter-field > span,
.date-range-title {
  color: #0f766e;
}

.filter-control {
  background:
    linear-gradient(45deg, transparent 50%, #475569 50%) calc(100% - 14px) 15px / 5px 5px
      no-repeat,
    linear-gradient(135deg, #475569 50%, transparent 50%) calc(100% - 9px) 15px / 5px 5px
      no-repeat;
  color: #0f172a;
}

.filter-control:disabled {
  color: #94a3b8;
  background:
    linear-gradient(45deg, transparent 50%, #94a3b8 50%) calc(100% - 14px) 15px / 5px 5px
      no-repeat,
    linear-gradient(135deg, #94a3b8 50%, transparent 50%) calc(100% - 9px) 15px / 5px 5px
      no-repeat;
}

.filter-status-group {
  background: rgba(241, 245, 249, 0.86);
}

.filter-status-button {
  color: #475569;
}

.filter-status-button:hover {
  background: rgba(14, 116, 144, 0.08);
  color: #0f766e;
}

.filter-status-button.is-active {
  background: linear-gradient(135deg, #0f766e, #0e7490);
  color: #ffffff;
  box-shadow: 0 9px 18px rgba(14, 116, 144, 0.18);
}

.date-range-inputs :deep(.cd-input) {
  background: #ffffff;
  color: #0f172a;
  border: 1px solid rgba(148, 163, 184, 0.22);
}

.date-range-inputs :deep(.cd-input:hover),
.date-range-inputs :deep(.cd-input:focus-visible) {
  background: #f8fafc;
  box-shadow: 0 0 0 2px rgba(45, 212, 191, 0.16);
}

.date-range-separator {
  color: #94a3b8;
}

.inventory-list-header {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  border-bottom: 1px solid rgba(125, 211, 252, 0.24);
  padding: 1rem clamp(1rem, 2vw, 1.35rem);
}

.inventory-list-body {
  padding: clamp(0.85rem, 1.6vw, 1.1rem);
}

.inventory-list-scroll {
  max-height: none;
  overflow-x: auto;
  overflow-y: visible;
  overscroll-behavior: auto;
  padding-right: 0;
  scrollbar-width: thin;
  scrollbar-color: rgba(14, 116, 144, 0.42) rgba(248, 250, 252, 0.82);
}

.inventory-list-scroll::-webkit-scrollbar {
  width: 0.55rem;
  height: 0.55rem;
}

.inventory-list-scroll::-webkit-scrollbar-track {
  background: rgba(248, 250, 252, 0.82);
  border-radius: 999px;
}

.inventory-list-scroll::-webkit-scrollbar-thumb {
  border: 2px solid rgba(248, 250, 252, 0.82);
  border-radius: 999px;
  background: linear-gradient(180deg, rgba(14, 165, 233, 0.56), rgba(20, 184, 166, 0.56));
}

.inventory-danger-button {
  display: inline-flex;
  width: 100%;
  min-height: 2.35rem;
  align-items: center;
  justify-content: center;
  border: 1px solid rgba(248, 113, 113, 0.32);
  border-radius: 999px;
  background: #fffafa;
  color: #b91c1c;
  padding: 0.55rem 0.9rem;
  font-size: 0.78rem;
  font-weight: 850;
  white-space: nowrap;
}

.inventory-danger-button:hover {
  border-color: rgba(239, 68, 68, 0.5);
  background: #fef2f2;
}

.gestion-import-widget {
  border: 1px solid rgba(125, 211, 252, 0.28);
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.78);
  padding: 0.45rem;
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.05);
}

@media (max-width: 920px) {
  .filter-compact-grid {
    border-color: rgba(148, 163, 184, 0.28);
    background: rgba(255, 255, 255, 0.94);
  }
}

@media (max-width: 640px) {
  .gestion-hero-panel {
    border-radius: 18px;
  }

  .gestion-hero-pills {
    justify-content: flex-start;
  }

  .gestion-tab-nav {
    width: 100%;
  }

  .gestion-tab-button {
    flex: 1 1 0;
    min-width: 0;
  }

  .inventory-actions-panel {
    border-color: rgba(125, 211, 252, 0.32);
    background: rgba(255, 255, 255, 0.86);
  }
}

.gestion-summary-panel {
  border: 1px solid rgba(148, 163, 184, 0.24);
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.78);
  padding: clamp(0.75rem, 1.3vw, 1rem);
  box-shadow: 0 18px 40px rgba(15, 23, 42, 0.06);
  backdrop-filter: blur(14px) saturate(115%);
}

.gestion-tab-shell {
  justify-content: center;
}

.gestion-tab-nav {
  width: min(100%, 720px);
  border: 0;
  border-radius: 0;
  background: transparent;
  padding: 0;
  box-shadow: none;
}

.gestion-tab-button {
  display: flex;
  min-width: 0;
  flex: 1 1 0;
  align-items: center;
  justify-content: center;
  gap: 0.7rem;
  border-radius: 17px;
  padding: 0.72rem 0.85rem;
  text-align: left;
}

.gestion-tab-button > svg {
  flex: 0 0 auto;
  color: #0e7490;
}

.gestion-tab-button > span {
  display: grid;
  min-width: 0;
  gap: 0.1rem;
}

.gestion-tab-button strong,
.gestion-tab-button small {
  display: block;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.gestion-tab-button strong {
  font-size: 0.9rem;
  line-height: 1.05;
}

.gestion-tab-button small {
  color: #64748b;
  font-size: 0.68rem;
  font-weight: 800;
  line-height: 1.1;
}

.gestion-tab-button.is-active {
  background: #0f766e;
  color: #ffffff;
}

.gestion-tab-button.is-active small {
  color: rgba(255, 255, 255, 0.74);
}

.gestion-tab-button.is-active > svg {
  color: rgba(255, 255, 255, 0.88);
}

.inventory-list-panel {
  display: grid;
  overflow: visible;
}

.inventory-list-header {
  background: rgba(255, 255, 255, 0.72);
}

.inventory-search-strip {
  display: grid;
  grid-template-columns: minmax(160px, 260px) minmax(0, 1fr);
  align-items: center;
  gap: clamp(0.8rem, 1.4vw, 1rem);
  border-bottom: 1px solid rgba(125, 211, 252, 0.22);
  padding: clamp(0.95rem, 1.8vw, 1.25rem) clamp(1rem, 2vw, 1.35rem);
  background: rgba(248, 250, 252, 0.82);
}

.inventory-search-label {
  min-width: 0;
}

.inventory-search-label p,
.inventory-filter-heading p {
  color: #0f172a;
  font-size: 0.78rem;
  font-weight: 950;
  letter-spacing: 0.08em;
  line-height: 1.15;
  text-transform: uppercase;
}

.inventory-search-label span,
.inventory-filter-heading span {
  display: block;
  margin-top: 0.2rem;
  color: #64748b;
  font-size: 0.78rem;
  font-weight: 700;
  line-height: 1.25;
}

.inventory-search-strip :deep(.gestion-search-field) {
  min-height: 58px;
  border-radius: 18px;
  border-color: rgba(14, 165, 233, 0.32);
  background: rgba(255, 255, 255, 0.96);
  padding-inline: 1rem;
  box-shadow:
    0 14px 34px rgba(14, 165, 233, 0.08),
    inset 0 1px 0 rgba(255, 255, 255, 0.9);
}

.inventory-search-strip :deep(.gestion-search-icon) {
  width: 1.25rem;
  height: 1.25rem;
}

.inventory-search-strip :deep(.gestion-search-input) {
  font-size: clamp(1rem, 1.6vw, 1.18rem);
}

.inventory-filter-shell {
  display: grid;
  gap: 0.7rem;
  border-bottom: 1px solid rgba(125, 211, 252, 0.2);
  background: rgba(241, 245, 249, 0.66);
  padding: 0.85rem clamp(1rem, 2vw, 1.35rem) 1rem;
}

.inventory-filter-heading {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
}

.filter-toolbar-actions {
  display: flex;
  min-width: max-content;
  flex-wrap: nowrap;
  align-items: center;
  justify-content: flex-end;
  gap: 0.5rem;
}

.filter-compact-grid {
  display: none;
  grid-template-columns: repeat(3, minmax(150px, 1fr)) repeat(2, minmax(260px, 1.4fr));
  gap: 0.62rem;
  align-items: stretch;
}

.filter-compact-grid.is-open {
  display: grid;
}

.filter-panel-toggle {
  width: 8.4rem;
  flex: 0 0 8.4rem;
  height: 40px;
  min-height: 40px;
  white-space: nowrap;
}

.filter-field,
.date-range-compact {
  min-width: 0;
  max-width: none;
  height: auto;
  min-height: 48px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.9);
}

.filter-field {
  grid-template-columns: 1fr;
  gap: 0.25rem;
  padding: 0.55rem 0.7rem;
}

.filter-field > span {
  padding-left: 0;
  font-size: 0.66rem;
}

.filter-control {
  height: 24px;
  padding-left: 0;
}

.date-range-compact {
  grid-template-columns: 1fr;
  gap: 0.45rem;
  padding: 0.55rem 0.7rem;
}

.date-range-inputs :deep(.cd-input) {
  height: 30px;
}

.filter-reset-button {
  width: auto;
  min-width: 0;
  height: 40px;
  padding: 0 0.85rem;
}

.filter-reset-button--panel {
  width: 100%;
  min-height: 48px;
  height: auto;
  justify-self: stretch;
}

.inventory-filter-shell .filter-reset-text {
  position: static;
  width: auto;
  height: auto;
  overflow: visible;
  clip: auto;
}

.inventory-actions-panel {
  align-items: stretch;
}

.gestion-import-widget {
  background: rgba(255, 255, 255, 0.84);
}

@media (max-width: 1180px) {
  .filter-compact-grid {
    grid-template-columns: repeat(3, minmax(150px, 1fr));
  }

  .date-range-compact {
    grid-column: span 1;
  }
}

@media (max-width: 920px) {
  .inventory-search-strip {
    grid-template-columns: 1fr;
  }

  .filter-panel-toggle {
    display: inline-flex;
  }

  .filter-status-group--desktop {
    display: none;
  }

  .filter-compact-grid {
    display: none;
    grid-template-columns: 1fr 1fr;
    max-height: none;
    overflow: visible;
  }

  .filter-compact-grid.is-open {
    display: grid;
  }

  .filter-status-group--panel {
    display: inline-grid;
    grid-column: 1 / -1;
    max-width: none;
  }
}

@media (max-width: 640px) {
  .gestion-summary-panel,
  .inventory-list-panel {
    border-radius: 18px;
  }

  .gestion-tab-button {
    justify-content: center;
    padding-inline: 0.65rem;
  }

  .gestion-tab-button small {
    display: none;
  }

  .inventory-list-header,
  .inventory-filter-heading {
    align-items: stretch;
  }

  .inventory-actions {
    width: 100%;
  }

  .filter-toolbar-actions {
    display: flex;
    width: auto;
    align-items: center;
    justify-content: flex-end;
  }

  .filter-compact-grid {
    grid-template-columns: 1fr;
  }

  .inventory-list-scroll {
    max-height: none;
  }
}

.gestion-page-light {
  min-height: 100%;
  background: #f7f4ee;
}

.gestion-hero-panel,
.inventory-list-panel,
.gestion-summary-panel,
.command-controls {
  border-color: rgba(148, 163, 184, 0.22);
  background: #fbfaf7;
  box-shadow: 0 12px 30px rgba(15, 23, 42, 0.055);
}

.gestion-hero-panel::before,
.inventory-list-panel::before,
.command-controls::before {
  display: none;
}

.inventory-list-header,
.inventory-search-strip,
.inventory-filter-shell {
  background: #fbfaf7;
  border-bottom-color: rgba(148, 163, 184, 0.16);
}

.inventory-filter-shell {
  gap: 0;
  padding: 0.42rem clamp(0.85rem, 1.8vw, 1.2rem);
}

.inventory-filter-shell.is-open {
  gap: 0.55rem;
  padding-block: 0.62rem 0.72rem;
}

.inventory-filter-shell:not(.is-open) .inventory-filter-heading {
  min-height: 34px;
  width: 100%;
}

.inventory-filter-shell:not(.is-open) .inventory-filter-heading > div:first-child {
  display: inline-flex;
  min-width: 0;
  align-items: center;
  gap: 0.45rem;
}

.inventory-filter-shell:not(.is-open) .inventory-filter-heading p {
  font-size: 0.68rem;
  letter-spacing: 0.06em;
}

.inventory-filter-shell:not(.is-open) .inventory-filter-heading span {
  display: inline;
  margin-top: 0;
  font-size: 0.7rem;
}

.inventory-filter-shell:not(.is-open):not(.has-active) .inventory-filter-heading > div:first-child > span,
.inventory-filter-shell:not(.is-open) .filter-status-group--desktop,
.inventory-filter-shell:not(.is-open):not(.has-active) .filter-reset-button {
  display: none;
}

.inventory-filter-shell:not(.is-open) .filter-toolbar-actions {
  gap: 0.35rem;
}

.inventory-filter-shell:not(.is-open) .filter-panel-toggle {
  min-height: 40px;
  height: 40px;
  border-radius: 999px;
  padding-inline: 0.7rem;
}

.inventory-filter-shell:not(.is-open) .filter-reset-button {
  min-height: 32px;
  height: 32px;
  border-radius: 999px;
  padding-inline: 0.7rem;
}

.inventory-filter-shell:not(.is-open) .filter-compact-grid {
  display: none;
}

.gestion-import-widget {
  border: 0;
  background: transparent;
  padding: 0;
  box-shadow: none;
}

.inventory-list-panel {
  overflow: hidden;
}

.inventory-list-scroll {
  max-height: clamp(420px, 56dvh, 690px);
  overflow-x: auto;
  overflow-y: scroll;
  overscroll-behavior: contain;
  padding-right: 0.15rem;
  scrollbar-gutter: stable;
  scrollbar-width: thin !important;
  scrollbar-color: rgba(14, 116, 144, 0.52) rgba(248, 250, 252, 0.92) !important;
  -ms-overflow-style: auto !important;
}

.inventory-list-scroll::-webkit-scrollbar {
  display: block !important;
  width: 0.6rem !important;
  height: 0.6rem !important;
}

.inventory-list-scroll::-webkit-scrollbar-track {
  border-radius: 999px;
  background: rgba(248, 250, 252, 0.92) !important;
}

.inventory-list-scroll::-webkit-scrollbar-thumb {
  border: 2px solid rgba(248, 250, 252, 0.92) !important;
  border-radius: 999px;
  background: rgba(14, 116, 144, 0.56) !important;
}

.inventory-filter-shell.is-open {
  gap: 0.42rem;
  padding-block: 0.5rem 0.6rem;
}

.filter-compact-grid.is-open {
  grid-template-columns:
    minmax(5.8rem, 0.38fr)
    minmax(8.4rem, 0.72fr)
    minmax(9.4rem, 0.78fr)
    minmax(7.1rem, 0.5fr)
    minmax(6.4rem, 0.44fr)
    minmax(10.8rem, 0.9fr)
    minmax(10.8rem, 0.9fr);
  gap: 0.42rem;
  align-items: stretch;
}

.filter-field,
.date-range-compact,
.filter-reset-button--panel {
  min-height: 38px;
  border-radius: 14px;
}

.filter-field {
  gap: 0.12rem;
  padding: 0.38rem 0.55rem;
}

.filter-field > span,
.date-range-title {
  font-size: 0.58rem;
  letter-spacing: 0.08em;
}

.filter-control {
  height: 20px;
  font-size: 0.84rem;
  line-height: 1;
}

.date-range-compact {
  gap: 0.26rem;
  padding: 0.38rem 0.5rem;
}

.date-range-title {
  gap: 0.25rem;
}

.date-range-inputs {
  gap: 0.26rem;
}

.date-range-inputs :deep(.cd-input) {
  height: 24px;
  min-height: 24px;
  border-radius: 10px;
  font-size: 0.72rem;
  padding-inline: 0.24rem;
}

.filter-reset-button--panel {
  width: auto;
  min-width: 0;
  height: 38px;
  padding-inline: 0.7rem;
}

@media (max-width: 1180px) {
  .filter-compact-grid.is-open {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .filter-compact-grid.is-open {
    grid-template-columns: 1fr;
  }

  .inventory-list-scroll {
    max-height: clamp(380px, 62dvh, 620px);
  }
}

@media (max-width: 420px) {
  .gestion-page-stack {
    gap: 0.75rem;
  }

  .gestion-hero-panel,
  .gestion-summary-panel,
  .inventory-list-panel {
    border-radius: 16px;
  }

  .gestion-hero-panel {
    padding: 0.9rem;
  }

  .gestion-title {
    font-size: 1.8rem;
  }

  .gestion-subtitle {
    font-size: 0.88rem;
  }

  .gestion-tab-button {
    gap: 0.35rem;
    padding: 0.62rem 0.45rem;
  }

  .gestion-tab-button strong {
    font-size: 0.76rem;
  }

  .inventory-list-header,
  .inventory-search-strip,
  .inventory-filter-shell {
    padding-inline: 0.85rem;
  }

  .inventory-search-strip :deep(.gestion-search-field) {
    min-height: 50px;
    border-radius: 14px;
  }

  .inventory-list-body {
    padding: 0.7rem;
  }
}

@media (min-width: 1800px) {
  .gestion-page-stack {
    gap: clamp(1.2rem, 1.25vw, 1.7rem);
  }

  .inventory-list-scroll {
    max-height: clamp(540px, 64dvh, 900px);
  }
}

.gestion-page-light :is(button, [role='button'], select, summary) {
  cursor: pointer;
}

.gestion-page-light :is(button, [role='button']):active {
  transform: translateY(0) scale(0.97);
}

.gestion-tab-button,
.filter-panel-toggle,
.filter-reset-button,
.filter-control,
.inventory-danger-button {
  transition:
    border-color 170ms ease,
    background-color 170ms ease,
    color 170ms ease,
    box-shadow 170ms ease,
    transform 140ms ease;
}

.gestion-tab-button:hover,
.filter-panel-toggle:hover,
.filter-reset-button:not(:disabled):hover,
.inventory-danger-button:hover {
  transform: translateY(-1px);
}

.inventory-list-panel {
  overflow: visible;
}

.inventory-sticky-tools {
  position: sticky;
  top: max(0.75rem, env(safe-area-inset-top));
  z-index: 24;
  overflow: hidden;
  border-radius: 20px 20px 16px 16px;
  background: rgba(251, 250, 247, 0.94);
  box-shadow:
    0 16px 34px rgba(15, 23, 42, 0.08),
    0 1px 0 rgba(255, 255, 255, 0.8) inset;
  backdrop-filter: blur(14px) saturate(115%);
  transition:
    box-shadow 180ms ease,
    transform 180ms ease,
    background-color 180ms ease;
}

.inventory-sticky-tools.is-condensed {
  background: rgba(251, 250, 247, 0.98);
  box-shadow:
    0 18px 38px rgba(15, 23, 42, 0.12),
    0 1px 0 rgba(255, 255, 255, 0.88) inset;
}

.inventory-list-scroll {
  scroll-behavior: smooth;
  will-change: scroll-position;
}

.inventory-load-more {
  display: flex;
  justify-content: center;
  padding: 0.85rem 0 0.2rem;
}

.inventory-load-more button {
  display: inline-flex;
  min-height: 2.45rem;
  align-items: center;
  justify-content: center;
  gap: 0.55rem;
  border: 1px solid rgba(20, 184, 166, 0.3);
  border-radius: 999px;
  background: #ecfdf5;
  color: #0f766e;
  padding: 0 0.95rem;
  font-size: 0.82rem;
  font-weight: 900;
  box-shadow: 0 10px 22px rgba(14, 116, 144, 0.08);
}

.inventory-load-more button:hover {
  border-color: rgba(20, 184, 166, 0.5);
  background: #ccfbf1;
  transform: translateY(-1px);
}

.inventory-load-more button span {
  color: #64748b;
  font-size: 0.75rem;
}

.gestion-back-to-top {
  position: fixed;
  right: max(1rem, env(safe-area-inset-right));
  bottom: max(1rem, env(safe-area-inset-bottom));
  z-index: 35;
  display: inline-grid;
  width: 2.8rem;
  height: 2.8rem;
  place-items: center;
  border: 1px solid rgba(20, 184, 166, 0.34);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.92);
  color: #0f766e;
  box-shadow: 0 16px 34px rgba(15, 23, 42, 0.16);
  backdrop-filter: blur(10px);
}

.gestion-back-to-top:hover {
  border-color: rgba(20, 184, 166, 0.56);
  background: #ecfdf5;
  transform: translateY(-2px);
}

.back-to-top-enter-active,
.back-to-top-leave-active {
  transition:
    opacity 170ms ease,
    transform 170ms ease;
}

.back-to-top-enter-from,
.back-to-top-leave-to {
  opacity: 0;
  transform: translateY(8px) scale(0.94);
}

.gestion-undo-stack {
  position: fixed;
  right: max(1rem, env(safe-area-inset-right));
  bottom: max(4.35rem, calc(env(safe-area-inset-bottom) + 4.35rem));
  z-index: 45;
  display: grid;
  width: min(24rem, calc(100vw - 2rem));
  gap: 0.6rem;
  pointer-events: none;
}

.gestion-undo-toast {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  gap: 0.75rem;
  border: 1px solid rgba(20, 184, 166, 0.32);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.96);
  color: #0f172a;
  padding: 0.78rem;
  box-shadow: 0 18px 42px rgba(15, 23, 42, 0.16);
  backdrop-filter: blur(12px) saturate(115%);
  pointer-events: auto;
}

.gestion-undo-toast.is-syncing {
  border-color: rgba(14, 165, 233, 0.32);
}

.gestion-undo-toast.is-error {
  border-color: rgba(239, 68, 68, 0.34);
  background: #fff7f7;
}

.gestion-undo-toast p {
  color: #0f172a;
  font-size: 0.85rem;
  font-weight: 900;
  line-height: 1.15;
}

.gestion-undo-toast span {
  display: block;
  margin-top: 0.16rem;
  color: #64748b;
  font-size: 0.75rem;
  font-weight: 700;
  line-height: 1.25;
}

.gestion-undo-toast button {
  min-height: 2.2rem;
  border: 1px solid rgba(20, 184, 166, 0.34);
  border-radius: 999px;
  background: #ecfdf5;
  color: #0f766e;
  padding: 0 0.78rem;
  font-size: 0.78rem;
  font-weight: 950;
}

.gestion-undo-toast button:hover {
  border-color: rgba(20, 184, 166, 0.56);
  background: #ccfbf1;
  transform: translateY(-1px);
}

.gestion-undo-toast-enter-active,
.gestion-undo-toast-leave-active,
.gestion-undo-toast-move {
  transition:
    opacity 180ms ease,
    transform 180ms ease;
}

.gestion-undo-toast-enter-from,
.gestion-undo-toast-leave-to {
  opacity: 0;
  transform: translateY(10px) scale(0.96);
}

@media (max-width: 760px) {
  .inventory-sticky-tools {
    top: 0.45rem;
    border-radius: 18px;
  }

  .gestion-undo-stack {
    right: 0.75rem;
    bottom: 4rem;
    width: calc(100vw - 1.5rem);
  }

  .gestion-back-to-top {
    right: 0.75rem;
    bottom: 0.75rem;
  }
}

.inventory-sticky-tools {
  border: 1px solid rgba(148, 163, 184, 0.18);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 12px 28px rgba(15, 23, 42, 0.075);
}

.inventory-sticky-tools.is-condensed {
  box-shadow: 0 14px 30px rgba(15, 23, 42, 0.12);
}

.inventory-toolbar {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  gap: 0.9rem;
  border-bottom: 1px solid rgba(148, 163, 184, 0.14);
  padding: 0.86rem clamp(0.95rem, 1.8vw, 1.25rem) 0.72rem;
}

.inventory-toolbar-copy {
  min-width: 0;
}

.inventory-toolbar-copy h2 {
  color: #0f172a;
  font-size: 1.05rem;
  font-weight: 950;
  line-height: 1.1;
}

.inventory-toolbar-copy p {
  margin-top: 0.22rem;
  color: #64748b;
  font-size: 0.78rem;
  font-weight: 800;
  line-height: 1.25;
}

.inventory-toolbar-actions {
  display: inline-flex;
  min-width: 0;
  align-items: center;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 0.55rem;
}

.inventory-toolbar-actions > * {
  min-width: 0;
}

.inventory-control-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto auto;
  align-items: center;
  gap: 0.6rem;
  padding: 0.68rem clamp(0.95rem, 1.8vw, 1.25rem);
}

.inventory-control-row > * {
  min-width: 0;
}

.inventory-control-row :deep(.gestion-search-field) {
  min-height: 46px;
  border-radius: 14px;
  border-color: rgba(148, 163, 184, 0.26);
  box-shadow: none;
}

.inventory-control-row :deep(.gestion-search-field:hover),
.inventory-control-row :deep(.gestion-search-field:focus-within) {
  border-color: rgba(20, 184, 166, 0.42);
  box-shadow: 0 0 0 3px rgba(45, 212, 191, 0.11);
}

.inventory-control-row :deep(.gestion-search-icon) {
  width: 1.08rem;
  height: 1.08rem;
}

.inventory-control-row :deep(.gestion-search-input) {
  font-size: 0.94rem;
}

.inventory-control-row .filter-panel-toggle {
  width: auto;
  min-width: 7rem;
  height: 46px;
  border-radius: 14px;
  padding-inline: 0.9rem;
}

.filter-reset-button--inline {
  width: 46px;
  min-width: 46px;
  height: 46px;
  border-radius: 14px;
}

.inventory-filter-shell:not(.is-open) {
  display: none;
}

.inventory-filter-shell.is-open {
  display: grid;
  border-top: 1px solid rgba(148, 163, 184, 0.14);
  border-bottom: 0;
  background: rgba(248, 250, 252, 0.76);
  padding: 0.68rem clamp(0.95rem, 1.8vw, 1.25rem) 0.8rem;
}

.inventory-filter-shell.is-open .filter-compact-grid.is-open {
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
}

.inventory-danger-button {
  width: auto;
  min-height: 44px;
  border-radius: 14px;
  padding-inline: 1rem;
  font-size: 0.82rem;
}

.inventory-toolbar-actions :deep(button) {
  min-height: 44px;
  border-radius: 14px;
}

.inventory-list-body {
  padding-top: 0.8rem;
}

@media (max-width: 920px) {
  .gestion-tab-shell {
    justify-content: stretch;
  }

  .gestion-tab-nav {
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
    width: 100%;
    min-width: 0;
  }

  .gestion-tab-button {
    min-width: 0;
    padding-inline: 0.8rem;
  }

  .inventory-toolbar {
    grid-template-columns: 1fr;
    align-items: stretch;
  }

  .inventory-toolbar-actions {
    display: grid;
    grid-template-columns: 1fr 1fr;
    min-width: 0;
  }

  .inventory-control-row {
    grid-template-columns: minmax(0, 1fr) auto;
  }

  .filter-reset-button--inline {
    display: none;
  }
}

@media (max-width: 720px) {
  .inventory-control-row {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .inventory-control-row > :first-child {
    grid-column: 1 / -1;
  }

  .inventory-control-row .filter-panel-toggle,
  .filter-reset-button--inline {
    width: 100%;
    min-width: 0;
  }

  .inventory-list-scroll {
    overflow-x: hidden;
  }
}

@media (max-width: 560px) {
  .gestion-tab-nav {
    gap: 0.4rem;
  }

  .gestion-tab-button {
    padding-inline: 0.55rem;
  }

  .inventory-toolbar-actions {
    grid-template-columns: 1fr;
  }

  .inventory-control-row {
    grid-template-columns: 1fr;
  }

  .inventory-control-row .filter-panel-toggle {
    width: 100%;
  }
}

@media (max-width: 1023px) {
  .inventory-list-scroll {
    max-height: none;
    overflow-x: hidden;
    overflow-y: visible;
    padding-right: 0;
    scrollbar-gutter: auto;
  }
}

@media (hover: none) and (pointer: coarse) {
  .filter-control,
  .date-range-inputs :deep(.cd-input),
  .inventory-control-row :deep(.gestion-search-input) {
    font-size: 16px;
  }

  .inventory-control-row :deep(.gestion-search-field),
  .inventory-control-row .filter-panel-toggle,
  .filter-reset-button--inline,
  .inventory-danger-button,
  .inventory-toolbar-actions :deep(button) {
    min-height: 48px;
  }
}
</style>
