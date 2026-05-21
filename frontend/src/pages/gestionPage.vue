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
            <div class="gestion-command-panel relative z-30">
              <div class="command-panel-grid">
                <StockSummaryRow
                  :total-paires="totalPaires"
                  :nb-en-stock="nbEnStock"
                  :valeur-stock="valeurStock"
                />

                <div class="command-controls">
                  <div class="command-primary-row">
                    <SearchBarre v-model="searchTerm" />

                    <div class="filter-status-group" aria-label="Filtrer par statut">
                      <button
                        v-for="option in statusOptions"
                        :key="option.value"
                        type="button"
                        class="filter-status-button"
                        :class="{ 'is-active': filters.status === option.value }"
                        @click="filters.status = option.value"
                      >
                        {{ option.label }}
                      </button>
                    </div>
                  </div>

                  <div class="filter-compact-grid">
                    <label class="filter-field">
                      <span>Type item</span>
                      <select v-model="filters.itemType" class="filter-control">
                        <option value="all">Tous</option>
                        <option
                          v-for="option in itemTypeOptions"
                          :key="option.value"
                          :value="option.value"
                        >
                          {{ option.label }}
                        </option>
                      </select>
                    </label>

                    <label class="filter-field">
                      <span>Sous-categorie</span>
                      <select
                        v-model="filters.category"
                        class="filter-control"
                        :disabled="!selectedItemType"
                      >
                        <option value="all">
                          {{ selectedItemType ? 'Toutes' : "Choisir un type d'abord" }}
                        </option>
                        <option
                          v-for="option in categoryOptions"
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

                    <button
                      type="button"
                      class="filter-reset-button"
                      :disabled="!hasActiveFilters"
                      @click="resetFilters"
                    >
                      <RotateCcw class="h-3.5 w-3.5" />
                      <span>Reset</span>
                      <span v-if="activeFilterCount" class="filter-count-badge">
                        {{ activeFilterCount }}
                      </span>
                    </button>

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
import { CalendarDays, RotateCcw } from 'lucide-vue-next'
import { useAuthStore } from '@/store/authStore'
import SnkVenteServices from '@/services/SnkVenteServices.js'
import StockSummaryRow from '@/components/gestion/GestionRésumeStock.vue'
import SearchBarre from '@/components/gestion/GestionSearchBarre.vue'
import GestionActionsPanel from '@/components/gestion/GestionBlocBoutonAddDelete.vue'
import afficherTout from '@/components/gestion/GestionAfficherTout.vue'
import EditVenteModal from '@/components/gestion/GestionModifierItem.vue'
import SupprimerModal from '@/components/gestion/GestionSupprimerModal.vue'
import CsvImportExportWidget from '@/components/gestion/CsvImportExportWidget.vue'
import DeliveryTrackingPanel from '@/components/gestion/DeliveryTrackingPanel.vue'
import CompactDateInput from '@/components/ui/CompactDateInput.vue'
import { isVendue, prixRetailOf } from '@/utils/snkVente'
import {
  buildItemCategoryAliases,
  isItemCategoryAlias,
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
const tabFromRoute = () => (route.query?.tab === 'delivery' ? 'delivery' : 'inventory')
const activeGestionTab = ref(tabFromRoute())

const EMPTY_CATEGORY_VALUE = '__empty_category__'

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

const normalizeText = (value) =>
  String(value ?? '')
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .trim()
    .toLowerCase()

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
  if (isItemCategoryAlias(raw, categoryLabels.value)) return EMPTY_CATEGORY_VALUE
  return raw || EMPTY_CATEGORY_VALUE
}

const itemTypeOptions = computed(() => resolveItemTypeOptions(categoryLabels.value))
const selectedItemType = computed(() => {
  const raw = filters.value.itemType
  if (!raw || raw === 'all') return ''
  const normalized = normalizeItemType(raw)
  return itemTypeOptions.value.some((option) => option.value === normalized) ? normalized : ''
})
const mainCategoryAliases = computed(() => buildItemCategoryAliases(categoryLabels.value))
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

const venteMatchesSearch = (vente, term) => {
  if (!term) return true
  const idStr = String(vente.id ?? '')
  const name = normalizeText(vente.nomItem ?? vente.nom_item)
  const rawCat = String(vente.categorie ?? '').trim()
  const cat = isItemCategoryAlias(rawCat, categoryLabels.value) ? '' : normalizeText(rawCat)
  const desc = normalizeText(vente.description)
  const type = normalizeText(itemTypeLabel(vente.type || 'SNEAKER', categoryLabels.value))
  return (
    idStr.includes(term) ||
    name.includes(term) ||
    cat.includes(term) ||
    desc.includes(term) ||
    type.includes(term)
  )
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
  const term = normalizeText(searchTerm.value)
  let list = snkVentes.value

  if (term) {
    list = list.filter((vente) => venteMatchesSearch(vente, term))
  }

  list = list.filter((vente) => venteMatchesFilters(vente, activeFilters))
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

watch(
  () => filters.value.itemType,
  () => {
    filters.value.category = 'all'
  },
)

onMounted(() => {
  window.addEventListener('snk:item-categories-change', onCategoryLabelsChange)
})

onBeforeUnmount(() => {
  window.removeEventListener('snk:item-categories-change', onCategoryLabelsChange)
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
const filteredVentes = computed(() => buildFilteredVentes())

const resetFilters = () => {
  searchTerm.value = ''
  filters.value = emptyFilters()
}

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
.gestion-command-panel {
  overflow: hidden;
  border: 1px solid rgba(71, 85, 105, 0.72);
  border-radius: 18px;
  background:
    linear-gradient(180deg, rgba(15, 23, 42, 0.94), rgba(15, 23, 42, 0.78)),
    rgba(15, 23, 42, 0.82);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.05),
    0 18px 44px rgba(2, 6, 23, 0.24);
  padding: 0.75rem;
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
  gap: 0.65rem;
}

.command-primary-row {
  display: grid;
  grid-template-columns: 1fr;
  gap: 0.65rem;
  align-items: center;
}

.filter-compact-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 0.6rem;
  align-items: end;
}

.filter-field {
  display: grid;
  min-width: 0;
  gap: 0.3rem;
}

.filter-field > span {
  color: rgb(148 163 184);
  font-size: 10.5px;
  font-weight: 800;
  letter-spacing: 0.14em;
  line-height: 1;
  text-transform: uppercase;
}

.filter-control {
  appearance: none;
  height: 40px;
  width: 100%;
  border: 1px solid rgba(71, 85, 105, 0.9);
  border-radius: 14px;
  background:
    linear-gradient(45deg, transparent 50%, rgb(148, 163, 184) 50%) calc(100% - 16px) 18px /
      5px 5px no-repeat,
    linear-gradient(135deg, rgb(148, 163, 184) 50%, transparent 50%) calc(100% - 11px) 18px /
      5px 5px no-repeat,
    rgba(15, 23, 42, 0.76);
  color: rgb(241 245 249);
  font-size: 13px;
  font-weight: 750;
  line-height: 1;
  outline: none;
  padding: 0 2.15rem 0 0.9rem;
  text-overflow: ellipsis;
  transition:
    border-color 140ms ease,
    background 140ms ease,
    box-shadow 140ms ease;
}

.filter-control:disabled {
  cursor: not-allowed;
  color: rgb(100 116 139);
  background:
    linear-gradient(45deg, transparent 50%, rgb(100, 116, 139) 50%) calc(100% - 16px) 18px /
      5px 5px no-repeat,
    linear-gradient(135deg, rgb(100, 116, 139) 50%, transparent 50%) calc(100% - 11px) 18px /
      5px 5px no-repeat,
    rgba(15, 23, 42, 0.42);
}

.filter-control:hover {
  border-color: rgb(100 116 139);
  background-color: rgba(15, 23, 42, 0.95);
}

.filter-control:focus {
  border-color: rgb(167 139 250);
  box-shadow: 0 0 0 3px rgba(139, 92, 246, 0.18);
}

.filter-status-group {
  display: inline-grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  width: 100%;
  min-width: 0;
  height: 40px;
  border: 1px solid rgba(71, 85, 105, 0.86);
  border-radius: 15px;
  background: rgba(2, 6, 23, 0.26);
  padding: 3px;
}

.filter-status-button {
  min-width: 0;
  border-radius: 12px;
  color: rgb(203 213 225);
  font-size: 13px;
  font-weight: 850;
  line-height: 1;
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
  box-shadow: 0 10px 22px rgba(2, 6, 23, 0.28);
}

.date-range-compact {
  display: grid;
  grid-template-columns: 1fr;
  align-items: center;
  gap: 0.45rem;
  min-width: 0;
  min-height: 40px;
  border: 1px solid rgba(71, 85, 105, 0.86);
  border-radius: 14px;
  background: rgba(15, 23, 42, 0.56);
  padding: 0.55rem;
}

.date-range-title {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  min-width: max-content;
  color: rgb(196 181 253);
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.11em;
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
  height: 30px;
  width: 100%;
  border: 0;
  border-radius: 10px;
  background: rgba(30, 41, 59, 0.74);
  color: rgb(226 232 240);
  font-size: 12px;
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
  background: rgba(30, 41, 59, 0.95);
}

.date-range-inputs :deep(.cd-input:focus-visible) {
  background: rgba(30, 41, 59, 0.95);
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
  height: 40px;
  min-width: 0;
  white-space: nowrap;
  border: 1px solid rgba(71, 85, 105, 0.86);
  border-radius: 14px;
  background: rgba(15, 23, 42, 0.42);
  color: rgb(203 213 225);
  font-size: 13px;
  font-weight: 800;
  line-height: 1;
  padding: 0 0.8rem;
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

.filter-count-badge {
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
}

@media (min-width: 560px) {
  .gestion-command-panel {
    padding: 0.85rem;
  }

  .filter-compact-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .filter-reset-button {
    justify-self: stretch;
  }

  .date-range-compact {
    grid-template-columns: auto minmax(0, 1fr);
    padding: 5px 6px 5px 0.72rem;
  }
}

@media (min-width: 760px) {
  .command-primary-row {
    grid-template-columns: minmax(260px, 1fr) minmax(252px, auto);
  }

  .filter-status-group {
    width: auto;
    min-width: 252px;
  }

  .filter-compact-grid {
    grid-template-columns: minmax(0, 1fr) minmax(0, 1fr) minmax(120px, 0.7fr);
  }

  .date-range-compact {
    grid-column: span 3;
  }
}

@media (min-width: 1180px) {
  .command-panel-grid {
    gap: 0.9rem;
  }

  .filter-compact-grid {
    grid-template-columns:
      minmax(148px, 0.85fr)
      minmax(170px, 1fr)
      minmax(112px, 0.55fr)
      auto;
  }

  .date-range-compact {
    grid-column: span 2;
  }
}

@media (min-width: 1440px) {
  .gestion-command-panel {
    padding: 0.9rem;
  }

  .command-panel-grid {
    grid-template-columns: minmax(320px, 360px) minmax(0, 1fr);
    align-items: stretch;
  }

  .filter-compact-grid {
    grid-template-columns:
      minmax(128px, 0.72fr)
      minmax(170px, 0.95fr)
      minmax(108px, 0.52fr)
      auto
      minmax(268px, 1fr)
      minmax(268px, 1fr);
  }

  .date-range-compact {
    grid-column: auto;
  }
}

@media (min-width: 1680px) {
  .command-panel-grid {
    grid-template-columns: minmax(330px, 380px) minmax(0, 1fr);
  }

  .filter-compact-grid {
    grid-template-columns:
      minmax(140px, 0.68fr)
      minmax(190px, 0.92fr)
      minmax(110px, 0.48fr)
      auto
      minmax(290px, 1fr)
      minmax(290px, 1fr);
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
</style>
