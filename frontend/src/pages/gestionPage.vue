<template>
  <div class="min-h-screen text-slate-100">
    <div class="relative w-full app-page-stack">
      <header
        class="rounded-[24px] border border-slate-700/60 bg-slate-900/70 px-5 py-5 shadow-xl shadow-slate-950/20 backdrop-blur sm:px-7"
      >
        <div class="flex flex-wrap items-end justify-between gap-4">
          <div class="space-y-2">
            <p class="text-[11px] uppercase tracking-[0.3em] text-violet-300/80">
              Backoffice Stash
            </p>
            <h1 class="text-2xl font-semibold text-white sm:text-3xl">Gestion des ventes</h1>
            <p class="max-w-2xl text-sm text-slate-400 sm:text-base">
              Ajoute, modifie, recherche, selectionne et supprime en un seul endroit.
            </p>
          </div>
          <div class="hidden flex-wrap gap-2 text-xs md:flex">
            <span
              class="inline-flex items-center rounded-full border border-slate-700/80 bg-slate-800/70 px-3 py-1 text-slate-300"
            >
              Inventaire centralise
            </span>
            <span
              class="inline-flex items-center rounded-full border border-violet-300/25 bg-violet-400/10 px-3 py-1 text-violet-200"
            >
              Actions rapides
            </span>
          </div>
        </div>
      </header>

      <div class="flex justify-center">
        <nav
          class="inline-flex rounded-full border border-slate-700/70 bg-slate-900/65 p-1 shadow-lg shadow-slate-950/20 backdrop-blur"
          aria-label="Sous-onglets gestion"
        >
          <button
            type="button"
            class="min-w-[118px] rounded-full px-4 py-2 text-sm font-semibold transition"
            :class="
              activeGestionTab === 'inventory'
                ? 'bg-slate-100 text-slate-950 shadow-sm'
                : 'text-slate-300 hover:bg-slate-800/70 hover:text-white'
            "
            @click="setGestionTab('inventory')"
          >
            Inventaire
          </button>
          <button
            type="button"
            class="min-w-[138px] rounded-full px-4 py-2 text-sm font-semibold transition"
            :class="
              activeGestionTab === 'delivery'
                ? 'bg-slate-100 text-slate-950 shadow-sm'
                : 'text-slate-300 hover:bg-slate-800/70 hover:text-white'
            "
            @click="setGestionTab('delivery')"
          >
            Suivi Livraison
          </button>
        </nav>
      </div>

      <Transition name="gestion-tab-view" mode="out-in">
        <div :key="activeGestionTab">
          <section v-if="activeGestionTab === 'inventory'" class="space-y-5">
            <div class="relative z-30 grid gap-6 xl:grid-cols-[0.78fr_1.22fr]">
              <div
                class="relative z-40 overflow-visible rounded-[24px] border border-slate-700/70 bg-slate-900/70 p-4 shadow-xl shadow-slate-950/20 backdrop-blur sm:p-5"
              >
                <StockSummaryRow
                  :total-paires="totalPaires"
                  :nb-en-stock="nbEnStock"
                  :valeur-stock="valeurStock"
                />
              </div>
              <div
                class="rounded-[24px] border border-slate-700/70 bg-slate-900/70 p-4 shadow-xl shadow-slate-950/20 backdrop-blur sm:p-5"
              >
                <div class="flex items-center justify-between">
                  <div>
                    <h2 class="text-base font-semibold text-white">Recherche rapide</h2>
                    <p class="text-xs text-slate-400">Nom, sous-categorie, description.</p>
                  </div>
                </div>
                <div ref="filterRoot" class="relative mt-3">
                  <div class="flex flex-col gap-2 sm:flex-row sm:items-center">
                    <div class="min-w-0 flex-1">
                      <SearchBarre v-model="searchTerm" />
                    </div>
                    <button
                      type="button"
                      class="inline-flex h-10 items-center justify-center gap-2 rounded-full border px-3.5 text-sm font-semibold transition focus:outline-none focus:ring-2 focus:ring-violet-500/30"
                      :class="
                        activeFilterCount
                          ? 'border-violet-400/60 bg-violet-500/15 text-violet-100 shadow-lg shadow-violet-950/20'
                          : 'border-slate-700 bg-slate-900/80 text-slate-200 hover:border-slate-500 hover:bg-slate-800/80'
                      "
                      :aria-expanded="filterPanelOpen"
                      aria-controls="gestion-filter-panel"
                      @click="toggleFilterPanel"
                    >
                      <svg class="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path
                          stroke-linecap="round"
                          stroke-linejoin="round"
                          stroke-width="2"
                          d="M3 5h18M6 12h12M10 19h4"
                        />
                      </svg>
                      <span>Filtres</span>
                      <span
                        v-if="activeFilterCount"
                        class="inline-flex h-5 min-w-5 items-center justify-center rounded-full bg-violet-400 px-1.5 text-[11px] font-bold text-slate-950"
                      >
                        {{ activeFilterCount }}
                      </span>
                    </button>
                  </div>

                  <div
                    v-if="activeFilterChips.length"
                    class="mt-3 flex flex-wrap items-center gap-2"
                  >
                    <button
                      v-for="chip in activeFilterChips"
                      :key="chip.key"
                      type="button"
                      class="inline-flex items-center gap-1.5 rounded-full border border-slate-700/80 bg-slate-950/70 px-3 py-1.5 text-xs font-medium text-slate-200 transition hover:border-violet-400/60 hover:text-white"
                      @click="removeActiveFilter(chip)"
                    >
                      <span>{{ chip.label }}</span>
                      <span class="text-slate-500" aria-hidden="true">&times;</span>
                    </button>
                    <button
                      type="button"
                      class="text-xs font-semibold text-slate-400 underline-offset-4 transition hover:text-white hover:underline"
                      @click="resetFilters"
                    >
                      Tout effacer
                    </button>
                  </div>

                  <Transition
                    enter-active-class="transition duration-150 ease-out"
                    enter-from-class="opacity-0 translate-y-1 scale-[0.99]"
                    enter-to-class="opacity-100 translate-y-0 scale-100"
                    leave-active-class="transition duration-100 ease-in"
                    leave-from-class="opacity-100 translate-y-0 scale-100"
                    leave-to-class="opacity-0 translate-y-1 scale-[0.99]"
                  >
                    <div
                      v-if="filterPanelOpen"
                      id="gestion-filter-panel"
                      class="gestion-filter-panel absolute right-0 top-[calc(100%+0.75rem)] z-40 w-full rounded-2xl border border-slate-700/80 bg-slate-950/95 p-4 shadow-2xl shadow-slate-950/70 backdrop-blur-xl sm:w-[760px] sm:max-w-[calc(100vw-3rem)]"
                      @keydown.esc.stop.prevent="filterPanelOpen = false"
                    >
                      <div class="flex flex-wrap items-start justify-between gap-3">
                        <div>
                          <h3 class="text-sm font-semibold text-white">Filtres de liste</h3>
                          <p class="mt-1 text-xs text-slate-400">
                            Combine recherche, statut, sous-categories et dates.
                          </p>
                        </div>
                        <span class="rounded-full bg-slate-900 px-3 py-1 text-xs text-slate-300">
                          {{ filteredPreviewCount }} item(s)
                        </span>
                      </div>

                      <div class="mt-4 grid gap-4">
                        <section v-if="categoryOptions.length" class="grid gap-2">
                          <div class="flex items-center justify-between gap-3">
                            <p
                              class="text-xs font-semibold uppercase tracking-[0.16em] text-slate-400"
                            >
                              Sous-categorie
                            </p>
                            <button
                              v-if="draftFilters.categories.length"
                              type="button"
                              class="text-[11px] font-semibold text-slate-500 transition hover:text-slate-200"
                              @click="draftFilters.categories = []"
                            >
                              Effacer
                            </button>
                          </div>
                          <div
                            class="gestion-filter-options-scroll flex max-h-28 flex-wrap gap-2 overflow-y-auto pr-1"
                          >
                            <button
                              v-for="option in categoryOptions"
                              :key="option.value"
                              type="button"
                              class="rounded-full border px-3 py-1.5 text-xs font-semibold transition"
                              :class="
                                draftFilters.categories.includes(option.value)
                                  ? filterActiveClass
                                  : filterIdleClass
                              "
                              @click="toggleDraftCategory(option.value)"
                            >
                              {{ option.label }}
                            </button>
                          </div>
                        </section>

                        <div class="grid gap-4 md:grid-cols-2">
                          <section class="grid gap-2">
                            <p
                              class="text-xs font-semibold uppercase tracking-[0.16em] text-slate-400"
                            >
                              Statut
                            </p>
                            <div class="grid grid-cols-3 gap-2">
                              <button
                                v-for="option in statusOptions"
                                :key="option.value"
                                type="button"
                                class="rounded-xl border px-3 py-2 text-xs font-semibold transition"
                                :class="
                                  draftFilters.status === option.value
                                    ? filterActiveClass
                                    : filterIdleClass
                                "
                                @click="draftFilters.status = option.value"
                              >
                                {{ option.label }}
                              </button>
                            </div>
                          </section>

                          <section class="grid gap-2">
                            <p
                              class="text-xs font-semibold uppercase tracking-[0.16em] text-slate-400"
                            >
                              Tri
                            </p>
                            <div class="grid grid-cols-3 gap-2">
                              <button
                                v-for="option in sortOptions"
                                :key="option.value"
                                type="button"
                                class="rounded-xl border px-3 py-2 text-xs font-semibold transition"
                                :class="
                                  draftFilters.sort === option.value
                                    ? filterActiveClass
                                    : filterIdleClass
                                "
                                @click="draftFilters.sort = option.value"
                              >
                                {{ option.label }}
                              </button>
                            </div>
                          </section>
                        </div>

                        <div class="grid gap-3 lg:grid-cols-2">
                          <section
                            v-for="dateFilter in dateFilterSections"
                            :key="dateFilter.key"
                            class="rounded-2xl border border-slate-800 bg-slate-900/70 p-3"
                          >
                            <div class="flex items-center justify-between gap-3">
                              <p
                                class="text-xs font-semibold uppercase tracking-[0.16em] text-slate-400"
                              >
                                {{ dateFilter.label }}
                              </p>
                              <button
                                v-if="isDateFilterDraftActive(dateFilter.key)"
                                type="button"
                                class="text-[11px] font-semibold text-slate-500 transition hover:text-slate-200"
                                @click="clearDraftDateFilter(dateFilter.key)"
                              >
                                Effacer
                              </button>
                            </div>

                            <div class="mt-2 grid grid-cols-4 gap-1.5">
                              <button
                                v-for="option in dateModeOptions"
                                :key="`${dateFilter.key}-${option.value}`"
                                type="button"
                                class="rounded-lg border px-2 py-1.5 text-[11px] font-semibold transition"
                                :class="
                                  draftFilters[dateFilter.key].mode === option.value
                                    ? filterActiveClass
                                    : filterIdleClass
                                "
                                @click="setDraftDateMode(dateFilter.key, option.value)"
                              >
                                {{ option.label }}
                              </button>
                            </div>

                            <div
                              v-if="draftFilters[dateFilter.key].mode !== 'all'"
                              class="mt-3 grid gap-2 sm:grid-cols-2"
                            >
                              <CompactDateInput
                                v-if="showDateFromInput(draftFilters[dateFilter.key].mode)"
                                v-model="draftFilters[dateFilter.key].from"
                                :label="dateFromLabel(draftFilters[dateFilter.key].mode)"
                                size="md"
                              />
                              <CompactDateInput
                                v-if="showDateToInput(draftFilters[dateFilter.key].mode)"
                                v-model="draftFilters[dateFilter.key].to"
                                :label="dateToLabel(draftFilters[dateFilter.key].mode)"
                                size="md"
                              />
                            </div>
                            <p v-else class="mt-3 text-xs text-slate-500">
                              Aucune contrainte de date.
                            </p>
                          </section>
                        </div>
                      </div>

                      <div
                        class="mt-4 flex flex-col-reverse gap-2 border-t border-slate-800 pt-4 sm:flex-row sm:items-center sm:justify-end"
                      >
                        <button
                          type="button"
                          class="rounded-xl border border-slate-700 px-4 py-2 text-sm font-semibold text-slate-300 transition hover:border-slate-500 hover:bg-slate-800"
                          @click="resetFilters"
                        >
                          Reinitialiser
                        </button>
                        <button
                          type="button"
                          class="rounded-xl bg-violet-500 px-4 py-2 text-sm font-semibold text-white transition hover:bg-violet-400 focus:outline-none focus:ring-2 focus:ring-violet-400/40"
                          @click="applyDraftFilters"
                        >
                          Appliquer
                        </button>
                      </div>
                    </div>
                  </Transition>
                </div>
              </div>
            </div>

            <!-- Tableau -->
            <div
              class="relative z-0 overflow-hidden rounded-[24px] border border-slate-700/70 bg-slate-900/70 shadow-xl shadow-slate-950/20 backdrop-blur"
            >
              <!-- Header tableau -->
              <div
                class="flex flex-wrap items-center justify-between gap-4 border-b border-slate-800/80 px-4 py-4 sm:px-6"
              >
                <!-- Gauche : titre -->
                <div>
                  <h2 class="text-lg font-semibold text-slate-100 leading-tight">
                    Liste des items
                  </h2>
                  <p class="text-xs text-slate-400">
                    {{ filteredVentes.length }} item(s) trouvee(s)
                    <span v-if="selectedIds.length">
                      - {{ selectedIds.length }} selectionnee(s)
                    </span>
                  </p>
                </div>

                <!-- Droite : actions -->
                <div class="flex w-full flex-wrap items-center gap-2 sm:w-auto sm:justify-end">
                  <GestionActionsPanel @vente-ajoutee="handleVenteAjoutee" />
                  <div class="[&_button:hover]:bg-red-900">
                    <button
                      type="button"
                      class="w-full whitespace-nowrap rounded-full border border-red-400/40 bg-red-500/10 px-3.5 py-2 text-xs font-semibold text-red-100 transition hover:border-red-300/70 hover:bg-red-500/20 disabled:opacity-60 sm:w-auto"
                      @click="openDeleteBulk"
                    >
                      Supprimer un item
                    </button>
                  </div>
                </div>
              </div>

              <!-- Liste -->
              <div class="p-4">
                <div class="max-h-[68vh] overflow-y-auto pr-1 sm:max-h-[560px] sm:pr-2">
                  <afficherTout
                    :snkVentes="filteredVentes"
                    selectable
                    v-model="selectedIds"
                    @edit="openEditModal"
                  />
                </div>
              </div>
            </div>
          </section>

          <DeliveryTrackingPanel v-else />
        </div>
      </Transition>

      <template v-if="activeGestionTab === 'inventory'">
        <!-- Edition -->
        <EditVenteModal v-model="showEditModal" :vente="venteToEdit" @saved="handleVenteUpdated" />

        <!-- Delete modal (unique) -->
        <SupprimerModal
          v-if="showDeleteModal"
          :snkVentes="snkVentes"
          :selectedIds="selectedIds"
          :defaultMode="deleteMode"
          @close="showDeleteModal = false"
          @deleted="handleDeleted"
        />
        <div class="[&_button:hover]:bg-gray-800">
          <CsvImportExportWidget :filteredRows="filteredVentes" @imported="reloadVentes" />
        </div>
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/store/authStore'
import SnkVenteServices from '@/services/SnkVenteServices.js'
import StockSummaryRow from '@/components/gestion/GestionRésumeStock.vue'
import SearchBarre from '@/components/gestion/GestionSearchBarre.vue'
import CompactDateInput from '@/components/ui/CompactDateInput.vue'
import GestionActionsPanel from '@/components/gestion/GestionBlocBoutonAddDelete.vue'
import afficherTout from '@/components/gestion/GestionAfficherTout.vue'
import EditVenteModal from '@/components/gestion/GestionModifierItem.vue'
import SupprimerModal from '@/components/gestion/GestionSupprimerModal.vue'
import CsvImportExportWidget from '@/components/gestion/CsvImportExportWidget.vue'
import DeliveryTrackingPanel from '@/components/gestion/DeliveryTrackingPanel.vue'
import { isVendue, prixRetailOf } from '@/utils/snkVente'
import { isItemCategoryAlias, itemTypeLabel, readStoredItemCategories } from '@/RegleItem/itemCategoryStore'

const snkVentes = ref([])
const searchTerm = ref('')
const selectedIds = ref([])
const filterRoot = ref(null)
const filterPanelOpen = ref(false)

const showEditModal = ref(false)
const venteToEdit = ref(null)

const showDeleteModal = ref(false)
const deleteMode = ref('name') // 'name' | 'bulk'
const pendingOpenItemId = ref(null)

const { user } = useAuthStore()
const currentUser = user
const currentUserId = computed(() => currentUser.value?.id ?? 'guest')
const categoryLabels = ref(readStoredItemCategories(currentUserId.value))
const route = useRoute()
const router = useRouter()
const tabFromRoute = () => (route.query?.tab === 'delivery' ? 'delivery' : 'inventory')
const activeGestionTab = ref(tabFromRoute())

const EMPTY_CATEGORY_VALUE = '__empty_category__'
const filterActiveClass = 'border-violet-400/70 bg-violet-500/15 text-violet-100'
const filterIdleClass =
  'border-slate-700 bg-slate-950/50 text-slate-300 hover:border-slate-500 hover:bg-slate-800/70 hover:text-white'

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

const dateModeOptions = [
  { value: 'all', label: 'Tous' },
  { value: 'after', label: 'Apres' },
  { value: 'before', label: 'Avant' },
  { value: 'between', label: 'Entre' },
]

const dateFilterSections = [
  { key: 'dateAchat', label: "Date d'achat" },
  { key: 'dateVente', label: 'Date de vente' },
]

const setGestionTab = (tab) => {
  const nextTab = tab === 'delivery' ? 'delivery' : 'inventory'
  activeGestionTab.value = nextTab

  const nextQuery = { ...route.query }
  if (nextTab === 'delivery') {
    nextQuery.tab = 'delivery'
  } else {
    delete nextQuery.tab
  }
  router.replace({ query: nextQuery }).catch(() => {})
}

const emptyFilters = () => ({
  categories: [],
  status: 'all',
  sort: 'none',
  dateAchat: { mode: 'all', from: '', to: '' },
  dateVente: { mode: 'all', from: '', to: '' },
})

const appliedFilters = ref(emptyFilters())
const draftFilters = ref(emptyFilters())

const cloneFilters = (filters) => ({
  categories: Array.isArray(filters?.categories) ? [...filters.categories] : [],
  status: filters?.status || 'all',
  sort: filters?.sort || 'none',
  dateAchat: { ...emptyFilters().dateAchat, ...(filters?.dateAchat || {}) },
  dateVente: { ...emptyFilters().dateVente, ...(filters?.dateVente || {}) },
})

const normalizeText = (value) =>
  String(value ?? '')
    .trim()
    .toLowerCase()

const categoryValueOf = (vente) => {
  const raw = String(vente?.categorie ?? '').trim()
  if (isItemCategoryAlias(raw, categoryLabels.value)) return EMPTY_CATEGORY_VALUE
  return raw ? normalizeText(raw) : EMPTY_CATEGORY_VALUE
}

const categoryLabelOf = (value) =>
  value === EMPTY_CATEGORY_VALUE ? 'Sans sous-categorie' : String(value)

const categoryOptions = computed(() => {
  const options = new Map()

  for (const vente of snkVentes.value) {
    const value = categoryValueOf(vente)
    if (options.has(value)) continue
    const rawLabel = String(vente?.categorie ?? '').trim()
    options.set(value, {
      value,
      label: value === EMPTY_CATEGORY_VALUE ? 'Sans sous-categorie' : rawLabel,
    })
  }

  return Array.from(options.values()).sort((a, b) =>
    a.label.localeCompare(b.label, 'fr', { sensitivity: 'base', numeric: true }),
  )
})

const categoryLabelMap = computed(() => {
  const map = new Map()
  for (const option of categoryOptions.value) {
    map.set(option.value, option.label)
  }
  return map
})

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

const formatDateChip = (value) => {
  const key = toDateKey(value)
  if (!key) return ''
  const [y, m, d] = key.split('-')
  return `${d}/${m}/${y}`
}

const isDateFilterActive = (filter) => {
  const mode = filter?.mode || 'all'
  if (mode === 'all') return false
  if (mode === 'after') return Boolean(toDateKey(filter.from))
  if (mode === 'before') return Boolean(toDateKey(filter.to))
  if (mode === 'between') return Boolean(toDateKey(filter.from) || toDateKey(filter.to))
  return false
}

const sanitizeDateFilter = (filter) => {
  const mode = ['all', 'after', 'before', 'between'].includes(filter?.mode) ? filter.mode : 'all'
  const next = {
    mode,
    from: toDateKey(filter?.from),
    to: toDateKey(filter?.to),
  }
  if (!isDateFilterActive(next)) return { mode: 'all', from: '', to: '' }
  if (next.mode === 'after') next.to = ''
  if (next.mode === 'before') next.from = ''
  return next
}

const sanitizeFilters = (filters) => ({
  categories: Array.isArray(filters?.categories) ? [...new Set(filters.categories)] : [],
  status: statusOptions.some((option) => option.value === filters?.status) ? filters.status : 'all',
  sort: sortOptions.some((option) => option.value === filters?.sort) ? filters.sort : 'none',
  dateAchat: sanitizeDateFilter(filters?.dateAchat),
  dateVente: sanitizeDateFilter(filters?.dateVente),
})

const dateMatches = (value, filter) => {
  if (!isDateFilterActive(filter)) return true
  const key = toDateKey(value)
  if (!key) return false

  const from = toDateKey(filter.from)
  const to = toDateKey(filter.to)

  if (filter.mode === 'after') return !from || key > from
  if (filter.mode === 'before') return !to || key < to
  if (filter.mode === 'between') {
    if (from && key < from) return false
    if (to && key > to) return false
    return true
  }
  return true
}

const venteMatchesSearch = (vente, term) => {
  if (!term) return true
  const idStr = String(vente.id ?? '')
  const name = String(vente.nomItem ?? vente.nom_item ?? '').toLowerCase()
  const rawCat = String(vente.categorie ?? '').trim()
  const cat = isItemCategoryAlias(rawCat, categoryLabels.value) ? '' : rawCat.toLowerCase()
  const desc = String(vente.description ?? '').toLowerCase()
  const type = itemTypeLabel(vente.type || 'SNEAKER', categoryLabels.value).toLowerCase()
  return (
    idStr.includes(term) ||
    name.includes(term) ||
    cat.includes(term) ||
    desc.includes(term) ||
    type.includes(term)
  )
}

const venteMatchesFilters = (vente, filters) => {
  if (filters.categories.length && !filters.categories.includes(categoryValueOf(vente)))
    return false
  if (filters.status === 'stock' && isVendue(vente)) return false
  if (filters.status === 'sold' && !isVendue(vente)) return false
  if (!dateMatches(vente.dateAchat ?? vente.date_achat, filters.dateAchat)) return false
  if (!dateMatches(vente.dateVente ?? vente.date_vente, filters.dateVente)) return false
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

const buildFilteredVentes = (filters) => {
  const sanitized = sanitizeFilters(filters)
  const term = (searchTerm.value || '').trim().toLowerCase()
  let list = snkVentes.value

  if (term) {
    list = list.filter((vente) => venteMatchesSearch(vente, term))
  }

  list = list.filter((vente) => venteMatchesFilters(vente, sanitized))
  return sortVentes(list, sanitized.sort)
}

const activeFilterChips = computed(() => {
  const filters = sanitizeFilters(appliedFilters.value)
  const chips = []

  for (const category of filters.categories) {
    chips.push({
      key: `category-${category}`,
      type: 'category',
      value: category,
      label: categoryLabelMap.value.get(category) || categoryLabelOf(category),
    })
  }

  if (filters.status !== 'all') {
    chips.push({
      key: 'status',
      type: 'status',
      label: statusOptions.find((option) => option.value === filters.status)?.label || 'Statut',
    })
  }

  if (filters.sort !== 'none') {
    chips.push({
      key: 'sort',
      type: 'sort',
      label: sortOptions.find((option) => option.value === filters.sort)?.label || 'Tri',
    })
  }

  for (const section of dateFilterSections) {
    const filter = filters[section.key]
    if (!isDateFilterActive(filter)) continue
    if (filter.mode === 'after') {
      chips.push({
        key: `${section.key}-after`,
        type: 'date',
        field: section.key,
        label: `${section.label} apres ${formatDateChip(filter.from)}`,
      })
    } else if (filter.mode === 'before') {
      chips.push({
        key: `${section.key}-before`,
        type: 'date',
        field: section.key,
        label: `${section.label} avant ${formatDateChip(filter.to)}`,
      })
    } else {
      const from = formatDateChip(filter.from)
      const to = formatDateChip(filter.to)
      chips.push({
        key: `${section.key}-between`,
        type: 'date',
        field: section.key,
        label: `${section.label} ${from || 'debut'} -> ${to || 'fin'}`,
      })
    }
  }

  return chips
})

const activeFilterCount = computed(() => activeFilterChips.value.length)
const filteredPreviewCount = computed(() => buildFilteredVentes(draftFilters.value).length)

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
  },
)

const onCategoryLabelsChange = (event) => {
  const detail = event?.detail || {}
  if (String(detail.userId || 'guest') !== String(currentUserId.value || 'guest')) return
  categoryLabels.value = readStoredItemCategories(currentUserId.value)
}

onMounted(() => {
  window.addEventListener('snk:item-categories-change', onCategoryLabelsChange)
})

onBeforeUnmount(() => {
  window.removeEventListener('snk:item-categories-change', onCategoryLabelsChange)
})

const onDocumentPointerDown = (event) => {
  if (!filterPanelOpen.value) return
  if (filterRoot.value?.contains(event.target)) return
  filterPanelOpen.value = false
}

onMounted(() => {
  document.addEventListener('pointerdown', onDocumentPointerDown, true)
})

onBeforeUnmount(() => {
  document.removeEventListener('pointerdown', onDocumentPointerDown, true)
})

// Stats
const totalPaires = computed(() => snkVentes.value.length)
const nbEnStock = computed(() => snkVentes.value.filter((v) => !isVendue(v)).length)
const valeurStock = computed(() =>
  snkVentes.value
    .filter((v) => !isVendue(v))
    .reduce((sum, v) => {
      const prix = prixRetailOf(v)
      if (Number.isNaN(prix)) return sum
      return sum + prix
    }, 0),
)

// Recherche + filtres
const filteredVentes = computed(() => buildFilteredVentes(appliedFilters.value))

const toggleFilterPanel = () => {
  if (!filterPanelOpen.value) {
    draftFilters.value = cloneFilters(appliedFilters.value)
  }
  filterPanelOpen.value = !filterPanelOpen.value
}

const applyDraftFilters = () => {
  appliedFilters.value = sanitizeFilters(draftFilters.value)
  draftFilters.value = cloneFilters(appliedFilters.value)
  filterPanelOpen.value = false
}

const resetFilters = () => {
  appliedFilters.value = emptyFilters()
  draftFilters.value = emptyFilters()
}

const removeActiveFilter = (chip) => {
  const next = cloneFilters(appliedFilters.value)

  if (chip.type === 'category') {
    next.categories = next.categories.filter((category) => category !== chip.value)
  } else if (chip.type === 'status') {
    next.status = 'all'
  } else if (chip.type === 'sort') {
    next.sort = 'none'
  } else if (chip.type === 'date' && chip.field) {
    next[chip.field] = { mode: 'all', from: '', to: '' }
  }

  appliedFilters.value = sanitizeFilters(next)
  if (filterPanelOpen.value) {
    draftFilters.value = cloneFilters(appliedFilters.value)
  }
}

const toggleDraftCategory = (category) => {
  const selected = new Set(draftFilters.value.categories)
  if (selected.has(category)) selected.delete(category)
  else selected.add(category)
  draftFilters.value.categories = Array.from(selected)
}

const setDraftDateMode = (key, mode) => {
  draftFilters.value[key].mode = mode
  if (mode === 'all') {
    clearDraftDateFilter(key)
  }
}

const clearDraftDateFilter = (key) => {
  draftFilters.value[key] = { mode: 'all', from: '', to: '' }
}

const isDateFilterDraftActive = (key) => isDateFilterActive(draftFilters.value[key])
const showDateFromInput = (mode) => mode === 'after' || mode === 'between'
const showDateToInput = (mode) => mode === 'before' || mode === 'between'
const dateFromLabel = (mode) => (mode === 'between' ? 'Du' : 'Apres le')
const dateToLabel = (mode) => (mode === 'between' ? 'Au' : 'Avant le')

// Selection logique : si tu filtres, on garde seulement ce qui est visible
watch(filteredVentes, (list) => {
  const visible = new Set(list.map((v) => v.id))
  selectedIds.value = selectedIds.value.filter((id) => visible.has(id))
})

// Ajout
const handleVenteAjoutee = async () => {
  await chargerVentes()
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

const openDeleteBulk = () => {
  deleteMode.value = 'bulk'
  showDeleteModal.value = true
}

const handleDeleted = (ids) => {
  const set = new Set(ids)
  snkVentes.value = snkVentes.value.filter((v) => !set.has(v.id))
  selectedIds.value = selectedIds.value.filter((id) => !set.has(id))
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
.gestion-filter-panel,
.gestion-filter-options-scroll {
  scrollbar-width: thin;
  scrollbar-color: rgba(139, 92, 246, 0.55) rgba(15, 23, 42, 0.35);
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

.gestion-filter-panel {
  max-height: min(76vh, 680px);
  overflow-y: auto;
  overscroll-behavior: contain;
}

.gestion-filter-panel::-webkit-scrollbar,
.gestion-filter-options-scroll::-webkit-scrollbar {
  width: 8px;
}

.gestion-filter-panel::-webkit-scrollbar-track,
.gestion-filter-options-scroll::-webkit-scrollbar-track {
  background: rgba(15, 23, 42, 0.35);
  border-radius: 999px;
}

.gestion-filter-panel::-webkit-scrollbar-thumb,
.gestion-filter-options-scroll::-webkit-scrollbar-thumb {
  border: 2px solid rgba(15, 23, 42, 0.35);
  border-radius: 999px;
  background: linear-gradient(180deg, rgba(167, 139, 250, 0.78), rgba(14, 165, 233, 0.62));
}

@media (max-width: 640px) {
  .gestion-filter-panel {
    left: 0;
    right: 0;
    max-height: min(72vh, 620px);
  }
}
</style>
