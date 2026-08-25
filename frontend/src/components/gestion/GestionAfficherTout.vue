<template>
  <div class="gestion-list space-y-3">
    <div
      v-if="selectable"
      class="gestion-selection-bar mb-3 flex items-center justify-between"
      :class="{ 'is-idle': !modelValue.length }"
    >
      <p class="text-xs text-gray-400">
        {{ modelValue.length ? `${modelValue.length} selectionnee(s)` : 'Aucune selection' }}
      </p>

      <button
        type="button"
        class="gestion-selection-toggle"
        :disabled="!visibleIds.length"
        @click="toggleAll"
      >
        {{ allSelected && modelValue.length ? 'Tout deselectionner' : 'Tout selectionner' }}
      </button>
    </div>

    <div v-if="!isDesktop && snkVentes.length" class="gestion-mobile-list space-y-3 lg:hidden">
      <article
        v-for="vente in snkVentes"
        :key="vente.id"
        class="gestion-list-row gestion-mobile-card rounded-2xl border border-gray-700 bg-gray-900/60 p-3"
      >
        <div class="flex items-start justify-between gap-3">
          <button
            type="button"
            class="gestion-mobile-main min-w-0 flex-1 text-left"
            @click="emit('edit', vente)"
          >
            <div class="flex items-center gap-2">
              <p class="gestion-mobile-title text-sm font-semibold text-gray-100">
                {{ vente.nomItem || vente.nom_item }}
              </p>
              <span v-if="isGroup(vente)" class="gestion-qty-pill">
                {{ quantityLabel(vente) }}
              </span>
            </div>
            <p
              v-if="groupSubtitle(vente)"
              class="gestion-mobile-description line-clamp-2 text-xs text-gray-400"
            >
              {{ groupSubtitle(vente) }}
            </p>
          </button>

          <div class="flex items-center gap-2">
            <input
              v-if="selectable"
              type="checkbox"
              class="accent-purple-500"
              :checked="isGroupChecked(vente)"
              @click.stop
              @change="toggleGroup(vente)"
            />

            <button
              v-if="isGroup(vente)"
              type="button"
              class="gestion-icon-button"
              :aria-label="isExpanded(vente.id) ? 'Replier le groupe' : 'Deplier le groupe'"
              @click.stop="toggleExpanded(vente.id)"
            >
              <ChevronDown v-if="isExpanded(vente.id)" class="h-4 w-4" aria-hidden="true" />
              <ChevronRight v-else class="h-4 w-4" aria-hidden="true" />
            </button>

            <button
              type="button"
              class="gestion-icon-button"
              aria-label="Modifier l'item"
              title="Modifier"
              @click.stop="emit('edit', vente)"
            >
              <Pencil class="h-4 w-4" aria-hidden="true" />
            </button>

            <button
              type="button"
              class="gestion-icon-button gestion-icon-button--danger"
              aria-label="Supprimer l'item"
              title="Supprimer"
              @click.stop="emit('delete', deleteIdsFor(vente))"
            >
              <Trash2 class="h-4 w-4" aria-hidden="true" />
            </button>
          </div>
        </div>

        <div class="mt-3 flex items-center justify-between gap-2">
          <div class="flex min-w-0 flex-wrap items-center gap-2">
            <span
              class="inline-flex items-center rounded-full px-2 py-0.5 text-[11px] font-semibold uppercase tracking-wide"
              :class="typeBadgeClass(vente.type)"
            >
              {{ typeLabelDisplay(vente.type) }}
            </span>
            <span
              v-if="subcategoryLabel(vente)"
              class="gestion-mobile-pill inline-flex max-w-[11rem] items-center rounded-full border border-gray-700 bg-gray-950/60 px-2 py-0.5 text-[11px] font-medium text-gray-300"
            >
              <span class="gestion-mobile-pill-text">{{ subcategoryLabel(vente) }}</span>
            </span>
          </div>
          <span
            class="text-sm font-semibold"
            :class="profitValue(vente) >= 0 ? 'text-emerald-400' : 'text-red-400'"
          >
            {{ formatCurrency(profitValue(vente)) }}
          </span>
        </div>

        <div class="mt-3 grid grid-cols-1 gap-2 min-[380px]:grid-cols-2">
          <div class="rounded-lg border border-gray-700/80 bg-gray-950/40 px-2.5 py-2">
            <p class="text-[10px] uppercase tracking-wide text-gray-500">Retail</p>
            <p class="mt-1 text-xs font-medium text-gray-200">
              {{ retailLabel(vente) }}
            </p>
          </div>
          <div class="rounded-lg border border-gray-700/80 bg-gray-950/40 px-2.5 py-2">
            <p class="text-[10px] uppercase tracking-wide text-gray-500">Resell</p>
            <p class="mt-1 text-xs font-medium text-gray-200">
              {{ resellLabel(vente) }}
            </p>
          </div>
          <div class="rounded-lg border border-gray-700/80 bg-gray-950/40 px-2.5 py-2">
            <p class="text-[10px] uppercase tracking-wide text-gray-500">Date achat</p>
            <p class="mt-1 text-xs font-medium text-gray-300">
              {{ formatDate(vente.dateAchat ?? vente.date_achat) }}
            </p>
          </div>
          <div class="rounded-lg border border-gray-700/80 bg-gray-950/40 px-2.5 py-2">
            <p class="text-[10px] uppercase tracking-wide text-gray-500">Date vente</p>
            <p class="mt-1 text-xs font-medium text-gray-300">
              {{ saleDateLabel(vente) }}
            </p>
          </div>
        </div>

        <div v-if="isGroup(vente) && isExpanded(vente.id)" class="gestion-child-stack mt-3">
          <article
            v-for="child in childRows(vente)"
            :key="child.id"
            class="gestion-child-card"
          >
            <div class="flex items-start justify-between gap-3">
              <button
                type="button"
                class="gestion-child-main min-w-0 flex-1 text-left"
                @click="emit('edit', child)"
              >
                <p class="text-sm font-semibold text-slate-900">
                  {{ childLabel(child) }}
                </p>
                <p class="mt-1 text-xs text-slate-500">
                  <span class="gestion-child-meta-badge">{{ childLineLabel(child) }}</span>
                  <span class="gestion-child-meta-separator">-</span>
                  {{ formatCurrency(child.prixRetail ?? child.prix_retail) }}
                  -
                  {{
                    isVendue(child)
                      ? `${formatCurrency(child.prixResell ?? child.prix_resell)} vendu`
                      : 'non vendu'
                  }}
                </p>
              </button>

              <div class="flex items-center gap-2">
                <input
                  v-if="selectable"
                  type="checkbox"
                  class="accent-purple-500"
                  :checked="isSelected(child.id)"
                  @click.stop
                  @change="toggleOne(child.id)"
                />

                <button
                  type="button"
                  class="gestion-icon-button"
                  aria-label="Modifier le sous-item"
                  @click.stop="emit('edit', child)"
                >
                  <Pencil class="h-4 w-4" aria-hidden="true" />
                </button>

                <button
                  type="button"
                  class="gestion-icon-button gestion-icon-button--danger"
                  aria-label="Supprimer le sous-item"
                  @click.stop="emit('delete', [child.id])"
                >
                  <Trash2 class="h-4 w-4" aria-hidden="true" />
                </button>
              </div>
            </div>
          </article>
        </div>
      </article>
    </div>

    <div
      v-else-if="!isDesktop"
      class="rounded-xl border border-gray-700/70 bg-gray-900/40 px-4 py-8 text-center text-sm text-gray-400 lg:hidden"
    >
      Aucun item a afficher pour le moment.
    </div>

    <div v-if="isDesktop" class="hidden lg:block">
      <table class="gestion-desktop-table w-full text-sm text-gray-100">
        <colgroup>
          <col v-if="selectable" class="gestion-col-select" />
          <col class="gestion-col-name" />
          <col class="gestion-col-type" />
          <col class="gestion-col-subcategory" />
          <col class="gestion-col-quantity" />
          <col class="gestion-col-retail" />
          <col class="gestion-col-resell" />
          <col class="gestion-col-date" />
          <col class="gestion-col-date" />
          <col class="gestion-col-profit" />
          <col class="gestion-col-actions" />
        </colgroup>
        <thead class="border-b border-gray-700 bg-gray-900">
          <tr>
            <th
              v-if="selectable"
              class="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-gray-400"
            >
              <input
                type="checkbox"
                :checked="allSelected"
                class="accent-purple-500"
                @change="toggleAll"
              />
            </th>
            <th class="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-gray-400">
              nom de l'item
            </th>
            <th class="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-gray-400">
              type
            </th>
            <th class="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-gray-400">
              sous-categorie
            </th>
            <th class="px-4 py-3 text-center text-xs font-semibold uppercase tracking-wide text-gray-400">
              quantite
            </th>
            <th class="px-4 py-3 text-right text-xs font-semibold uppercase tracking-wide text-gray-400">
              prix_retail
            </th>
            <th class="px-4 py-3 text-right text-xs font-semibold uppercase tracking-wide text-gray-400">
              prix_resell
            </th>
            <th class="px-4 py-3 text-center text-xs font-semibold uppercase tracking-wide text-gray-400">
              date achat
            </th>
            <th class="px-4 py-3 text-center text-xs font-semibold uppercase tracking-wide text-gray-400">
              date vente
            </th>
            <th class="px-4 py-3 text-right text-xs font-semibold uppercase tracking-wide text-gray-400">
              profit
            </th>
            <th class="px-4 py-3 text-center text-xs font-semibold uppercase tracking-wide text-gray-400">
              actions
            </th>
          </tr>
        </thead>

        <tbody>
          <template v-for="vente in snkVentes" :key="vente.id">
            <tr
              class="gestion-list-row border-b border-gray-800"
              :class="{ 'gestion-parent-row': isGroup(vente) }"
              tabindex="0"
              @click="emit('edit', vente)"
              @keydown.enter.prevent="emit('edit', vente)"
              @keydown.space.prevent="emit('edit', vente)"
            >
              <td v-if="selectable" class="px-4 py-3">
                <input
                  type="checkbox"
                  class="accent-purple-500"
                  :checked="isGroupChecked(vente)"
                  @click.stop
                  @change="toggleGroup(vente)"
                />
              </td>

              <td class="gestion-cell-name px-4 py-3">
                <div class="flex min-w-0 items-start gap-2">
                  <button
                    v-if="isGroup(vente)"
                    type="button"
                    class="gestion-expand-button"
                    :aria-label="isExpanded(vente.id) ? 'Replier le groupe' : 'Deplier le groupe'"
                    @click.stop="toggleExpanded(vente.id)"
                  >
                    <ChevronDown v-if="isExpanded(vente.id)" class="h-4 w-4" aria-hidden="true" />
                    <ChevronRight v-else class="h-4 w-4" aria-hidden="true" />
                  </button>
                  <div class="gestion-name-block flex min-w-0 flex-col">
                    <span class="gestion-name-text font-medium text-gray-100">
                      {{ vente.nomItem || vente.nom_item }}
                    </span>
                    <span v-if="groupSubtitle(vente)" class="gestion-name-subtitle text-[11px] text-gray-400">
                      {{ groupSubtitle(vente) }}
                    </span>
                  </div>
                </div>
              </td>

              <td class="px-4 py-3">
                <span
                  class="inline-flex items-center rounded-full px-2 py-0.5 text-[11px] font-semibold uppercase tracking-wide"
                  :class="typeBadgeClass(vente.type)"
                >
                  {{ typeLabelDisplay(vente.type) }}
                </span>
              </td>

              <td class="gestion-cell-subcategory px-4 py-3 text-xs text-gray-300">
                {{ subcategoryLabel(vente) || '--' }}
              </td>

              <td class="gestion-cell-quantity px-4 py-3 text-center text-xs text-gray-300">
                {{ quantityLabel(vente) }}
              </td>

              <td class="gestion-cell-retail px-4 py-3 text-right text-xs">
                {{ retailLabel(vente) }}
              </td>

              <td class="gestion-cell-resell px-4 py-3 text-right text-xs">
                {{ resellLabel(vente) }}
              </td>

              <td class="gestion-cell-date px-4 py-3 text-center text-xs text-gray-300">
                {{ formatDate(vente.dateAchat ?? vente.date_achat) }}
              </td>

              <td class="gestion-cell-date px-4 py-3 text-center text-xs text-gray-300">
                {{ saleDateLabel(vente) }}
              </td>

              <td class="gestion-cell-profit px-4 py-3 text-right">
                <span
                  class="font-semibold"
                  :class="profitValue(vente) >= 0 ? 'text-emerald-400' : 'text-red-400'"
                >
                  {{ formatCurrency(profitValue(vente)) }}
                </span>
              </td>

              <td class="px-4 py-3 text-center">
                <div class="gestion-row-actions">
                  <button
                    type="button"
                    class="gestion-icon-button"
                    aria-label="Modifier l'item"
                    title="Modifier"
                    @click.stop="emit('edit', vente)"
                  >
                    <Pencil class="h-4 w-4" aria-hidden="true" />
                  </button>

                  <button
                    type="button"
                    class="gestion-icon-button gestion-icon-button--danger"
                    aria-label="Supprimer l'item"
                    title="Supprimer"
                    @click.stop="emit('delete', deleteIdsFor(vente))"
                  >
                    <Trash2 class="h-4 w-4" aria-hidden="true" />
                  </button>
                </div>
              </td>
            </tr>

            <tr
              v-for="child in isExpanded(vente.id) ? childRows(vente) : []"
              :key="child.id"
              class="gestion-child-row border-b border-gray-100"
              tabindex="0"
              @click="emit('edit', child)"
              @keydown.enter.prevent="emit('edit', child)"
              @keydown.space.prevent="emit('edit', child)"
            >
              <td v-if="selectable" class="px-4 py-3">
                <input
                  type="checkbox"
                  class="accent-purple-500"
                  :checked="isSelected(child.id)"
                  @click.stop
                  @change="toggleOne(child.id)"
                />
              </td>

              <td class="gestion-cell-name px-4 py-3">
                <div class="pl-7">
                  <div class="gestion-name-block flex min-w-0 flex-col gap-1">
                    <span class="gestion-name-text font-medium text-slate-900">{{ childLabel(child) }}</span>
                    <span class="gestion-child-inline-badge">{{ childLineLabel(child) }}</span>
                  </div>
                </div>
              </td>

              <td class="px-4 py-3 text-xs text-gray-300">
                {{ typeLabelDisplay(child.type) }}
              </td>

              <td class="gestion-cell-subcategory px-4 py-3 text-xs text-gray-300">
                {{ subcategoryLabel(child) || '--' }}
              </td>

              <td class="gestion-cell-quantity px-4 py-3 text-center text-xs text-gray-300">
                1
              </td>

              <td class="gestion-cell-retail px-4 py-3 text-right">
                {{ formatCurrency(child.prixRetail ?? child.prix_retail) }}
              </td>

              <td class="gestion-cell-resell px-4 py-3 text-right">
                {{
                  isVendue(child)
                    ? formatCurrency(child.prixResell ?? child.prix_resell)
                    : '--'
                }}
              </td>

              <td class="gestion-cell-date px-4 py-3 text-center text-xs text-gray-300">
                {{ formatDate(child.dateAchat ?? child.date_achat) }}
              </td>

              <td class="gestion-cell-date px-4 py-3 text-center text-xs text-gray-300">
                {{
                  child.dateVente || child.date_vente
                    ? formatDate(child.dateVente ?? child.date_vente)
                    : '-'
                }}
              </td>

              <td class="gestion-cell-profit px-4 py-3 text-right">
                <span
                  class="font-semibold"
                  :class="profitValue(child) >= 0 ? 'text-emerald-400' : 'text-red-400'"
                >
                  {{ formatCurrency(profitValue(child)) }}
                </span>
              </td>

              <td class="px-4 py-3 text-center">
                <div class="gestion-row-actions">
                  <button
                    type="button"
                    class="gestion-icon-button"
                    aria-label="Modifier le sous-item"
                    @click.stop="emit('edit', child)"
                  >
                    <Pencil class="h-4 w-4" aria-hidden="true" />
                  </button>

                  <button
                    type="button"
                    class="gestion-icon-button gestion-icon-button--danger"
                    aria-label="Supprimer le sous-item"
                    @click.stop="emit('delete', [child.id])"
                  >
                    <Trash2 class="h-4 w-4" aria-hidden="true" />
                  </button>
                </div>
              </td>
            </tr>
          </template>

          <tr v-if="!snkVentes.length">
            <td :colspan="selectable ? 11 : 10" class="px-4 py-8 text-center text-sm text-gray-400">
              Aucun item a afficher pour le moment.
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { ChevronDown, ChevronRight, Pencil, Trash2 } from 'lucide-vue-next'
import { formatDateFR, formatEUR } from '@/utils/formatters'
import {
  childItemsOf,
  isGroupedItem,
  isVendue,
  itemQuantityOf,
  soldCountOf,
  totalProfitOf,
  totalResellOf,
  totalRetailOf,
} from '@/utils/snkVente'
import {
  isItemCategoryAlias,
  itemTypeLabel,
  readStoredItemCategories,
} from '@/RegleItem/itemCategoryStore'
import { useAuthStore } from '@/store/authStore'

const props = defineProps({
  snkVentes: { type: Array, required: true },
  selectable: { type: Boolean, default: false },
  modelValue: { type: Array, default: () => [] },
})

const emit = defineEmits(['edit', 'update:modelValue', 'delete'])
const authStore = useAuthStore()
const currentUserId = computed(() => authStore.user?.value?.id ?? authStore.user?.id ?? 'guest')
const categoryLabels = ref(readStoredItemCategories(currentUserId.value))
const isDesktop = ref(
  typeof window === 'undefined' ? true : window.matchMedia('(min-width: 1024px)').matches,
)
const expandedIds = ref([])
let desktopMediaQuery = null

const selectedSet = computed(() => new Set(props.modelValue))
const expandedSet = computed(() => new Set(expandedIds.value))
const visibleIds = computed(() => {
  const ids = []
  for (const vente of props.snkVentes) {
    ids.push(vente.id)
    if (expandedSet.value.has(vente.id)) {
      childRows(vente).forEach((child) => ids.push(child.id))
    }
  }
  return ids
})

const allSelected = computed(() => {
  if (!visibleIds.value.length) return false
  return visibleIds.value.every((id) => selectedSet.value.has(id))
})

const onDesktopChange = (event) => {
  isDesktop.value = event.matches
}

const isSelected = (id) => selectedSet.value.has(id)
const isGroup = (vente) => isGroupedItem(vente)
const isExpanded = (id) => expandedSet.value.has(id)
const childRows = (vente) => childItemsOf(vente)
const quantityOf = (vente) => itemQuantityOf(vente)
const soldCount = (vente) => soldCountOf(vente)

const groupIds = (vente) => [vente.id, ...childRows(vente).map((child) => child.id)]
const deleteIdsFor = (vente) => (isGroup(vente) ? groupIds(vente) : [vente.id])
const isGroupChecked = (vente) => groupIds(vente).every((id) => selectedSet.value.has(id))

const toggleOne = (id) => {
  const next = new Set(props.modelValue)
  if (next.has(id)) next.delete(id)
  else next.add(id)
  emit('update:modelValue', Array.from(next))
}

const toggleGroup = (vente) => {
  const next = new Set(props.modelValue)
  const ids = groupIds(vente)
  const shouldClear = ids.every((id) => next.has(id))
  ids.forEach((id) => {
    if (shouldClear) next.delete(id)
    else next.add(id)
  })
  emit('update:modelValue', Array.from(next))
}

const toggleAll = () => {
  if (allSelected.value) {
    const visible = new Set(visibleIds.value)
    const next = props.modelValue.filter((id) => !visible.has(id))
    emit('update:modelValue', next)
    return
  }

  const next = new Set(props.modelValue)
  visibleIds.value.forEach((id) => next.add(id))
  emit('update:modelValue', Array.from(next))
}

const toggleExpanded = (id) => {
  expandedIds.value = isExpanded(id)
    ? expandedIds.value.filter((currentId) => currentId !== id)
    : [...expandedIds.value, id]
}

const formatCurrency = (val) => {
  const num = Number(val)
  if (val === null || val === undefined || Number.isNaN(num)) return '--'
  return formatEUR(num, { digits: 0 })
}

const formatDate = (val) => formatDateFR(val, { fallback: '--' })

const subcategoryLabel = (vente) => {
  const label = String(vente?.categorie ?? '').trim()
  return isItemCategoryAlias(label, categoryLabels.value) ? '' : label
}

const typeBadgeClass = (type) => {
  switch (type) {
    case 'TICKET':
      return 'gestion-type-badge gestion-type-badge--ticket'
    case 'POKEMON_CARD':
      return 'gestion-type-badge gestion-type-badge--pokemon'
    case 'OTHER':
      return 'gestion-type-badge gestion-type-badge--other'
    default:
      return 'gestion-type-badge gestion-type-badge--default'
  }
}

const typeLabelDisplay = (type) =>
  itemTypeLabel(type || 'SNEAKER', categoryLabels.value).toUpperCase()

const quantityLabel = (vente) => {
  const quantity = quantityOf(vente)
  if (!isGroup(vente)) return String(quantity)
  return quantity > 1 ? `${quantity} lignes` : '1 ligne'
}

const groupSubtitle = (vente) => {
  if (!isGroup(vente)) return vente?.description || ''
  const quantity = quantityOf(vente)
  const sold = soldCount(vente)
  if (!sold) return quantityLabel(vente)
  if (sold >= quantity) return `${quantityLabel(vente)} - tout vendu`
  return `${quantityLabel(vente)} - ${sold} vendu${sold > 1 ? 's' : ''}`
}

const retailLabel = (vente) => {
  if (!isGroup(vente)) return formatCurrency(vente.prixRetail ?? vente.prix_retail)
  const unitRetail = childRows(vente)[0]?.prixRetail ?? childRows(vente)[0]?.prix_retail
  return `${formatCurrency(unitRetail)} / unite - ${formatCurrency(totalRetailOf(vente))} total`
}

const resellLabel = (vente) => {
  if (!isGroup(vente)) {
    return isVendue(vente) ? formatCurrency(vente.prixResell ?? vente.prix_resell) : '--'
  }
  if (!soldCount(vente)) return '--'
  if (soldCount(vente) < quantityOf(vente)) return `${soldCount(vente)}/${quantityOf(vente)} vendus`
  return formatCurrency(totalResellOf(vente))
}

const saleDateLabel = (vente) => {
  if (!isGroup(vente)) {
    return vente.dateVente || vente.date_vente ? formatDate(vente.dateVente ?? vente.date_vente) : '-'
  }
  if (!soldCount(vente)) return '-'
  if (soldCount(vente) < quantityOf(vente)) return 'Vente partielle'
  return formatDate(vente.dateVente ?? vente.date_vente)
}

const profitValue = (vente) => totalProfitOf(vente)

const childLabel = (child) => {
  return child?.nomItem || child?.nom_item || 'Sous-item'
}

const childLineLabel = (child) => {
  const unitIndex = Number(child?.unitIndex ?? 0)
  return unitIndex > 0 ? `Ligne ${unitIndex}` : 'Ligne'
}

watch(
  () => currentUserId.value,
  (userId) => {
    categoryLabels.value = readStoredItemCategories(userId)
  },
)

watch(
  () => props.snkVentes,
  (rows) => {
    const visibleParents = new Set((rows || []).map((row) => row.id))
    expandedIds.value = expandedIds.value.filter((id) => visibleParents.has(id))
  },
)

function onCategoryLabelsChange(event) {
  const detail = event?.detail || {}
  if (String(detail.userId || 'guest') !== String(currentUserId.value || 'guest')) return
  categoryLabels.value = readStoredItemCategories(currentUserId.value)
}

onMounted(() => {
  window.addEventListener('snk:item-categories-change', onCategoryLabelsChange)
  desktopMediaQuery = window.matchMedia('(min-width: 1024px)')
  isDesktop.value = desktopMediaQuery.matches
  desktopMediaQuery.addEventListener('change', onDesktopChange)
})

onBeforeUnmount(() => {
  window.removeEventListener('snk:item-categories-change', onCategoryLabelsChange)
  desktopMediaQuery?.removeEventListener('change', onDesktopChange)
})
</script>

<style scoped>
.gestion-list {
  color: #0f172a;
  width: 100%;
  min-width: 0;
  max-width: 100%;
}

.gestion-list p,
.gestion-list span,
.gestion-list td,
.gestion-list th {
  letter-spacing: 0;
}

.gestion-list :is(.text-gray-100, .text-gray-200, .text-gray-300, .text-gray-400) {
  color: #0f172a;
}

.gestion-list :is(.text-gray-500) {
  color: #64748b;
}

.gestion-list
  :is(.bg-gray-900, .bg-gray-900\/60, .bg-gray-900\/40, .bg-gray-950\/60, .bg-gray-950\/40) {
  background: rgba(255, 255, 255, 0.98);
}

.gestion-list
  :is(.border-gray-700, .border-gray-700\/70, .border-gray-700\/80, .border-gray-800) {
  border-color: rgba(148, 163, 184, 0.24);
}

.gestion-list > div:first-child {
  border: 1px solid rgba(148, 163, 184, 0.18);
  border-radius: 18px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(248, 250, 252, 0.94));
  padding: 0.62rem 0.9rem;
}

.gestion-list > div:first-child p,
.gestion-list > div:first-child button {
  color: #0f172a;
  font-weight: 800;
}

.gestion-list-row {
  outline: none;
  transition:
    border-color 160ms ease,
    background-color 160ms ease,
    box-shadow 160ms ease,
    transform 140ms ease;
}

.gestion-list article {
  width: 100%;
  min-width: 0;
  max-width: 100%;
  border-color: rgba(148, 163, 184, 0.2);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.99), rgba(248, 250, 252, 0.95)),
    #ffffff;
  box-shadow:
    0 10px 24px rgba(15, 23, 42, 0.05),
    inset 0 1px 0 rgba(255, 255, 255, 0.6);
}

.gestion-list article:hover,
.gestion-list article:focus-visible,
.gestion-list tbody tr:hover td,
.gestion-list tbody tr:focus-visible td {
  border-color: rgba(45, 212, 191, 0.28);
  background: rgba(248, 250, 252, 0.96);
}

.gestion-list input[type='checkbox'] {
  min-width: 1rem;
  min-height: 1rem;
  accent-color: #0f766e;
  cursor: pointer;
}

.gestion-selection-bar {
  position: sticky;
  top: 0.35rem;
  z-index: 8;
  width: 100%;
  min-width: 0;
  min-height: 44px;
  gap: 0.55rem;
  flex-wrap: wrap;
  border: 1px solid rgba(148, 163, 184, 0.2);
  border-radius: 18px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(248, 250, 252, 0.94)),
    #ffffff;
  padding: 0.62rem 0.92rem;
  box-shadow: 0 10px 22px rgba(15, 23, 42, 0.05);
  transition:
    border-color 160ms ease,
    background-color 160ms ease,
    box-shadow 160ms ease;
}

.gestion-selection-bar.is-idle {
  border-color: rgba(148, 163, 184, 0.2);
  background: rgba(248, 250, 252, 0.88);
  box-shadow: none;
}

.gestion-selection-bar p,
.gestion-selection-bar button {
  color: #0f172a;
  font-weight: 800;
}

.gestion-selection-bar.is-idle p,
.gestion-selection-bar.is-idle button {
  color: #64748b;
}

.gestion-selection-toggle {
  display: inline-flex;
  align-items: center;
  justify-content: flex-end;
  min-height: 2rem;
  min-width: 0;
  max-width: 100%;
  border-radius: 999px;
  padding: 0 0.65rem;
  cursor: pointer;
  text-decoration: underline;
  text-underline-offset: 3px;
}

.gestion-selection-toggle:disabled {
  cursor: default;
  opacity: 0.6;
  text-decoration: none;
}

.gestion-mobile-list,
.gestion-mobile-card {
  width: 100%;
  min-width: 0;
  max-width: 100%;
}

.gestion-mobile-main,
.gestion-child-main {
  min-width: 0;
}

.gestion-mobile-title,
.gestion-mobile-description {
  overflow-wrap: anywhere;
}

.gestion-mobile-title {
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
  line-height: 1.25;
}

.gestion-mobile-pill {
  min-width: 0;
  max-width: min(100%, 11rem);
}

.gestion-mobile-pill-text {
  display: block;
  min-width: 0;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.gestion-qty-pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 1px solid rgba(45, 212, 191, 0.24);
  border-radius: 999px;
  background: #f0fdfa;
  color: #0f766e;
  padding: 0.16rem 0.56rem;
  font-size: 0.68rem;
  font-weight: 800;
}

.gestion-child-meta-badge,
.gestion-child-inline-badge {
  display: inline-flex;
  width: fit-content;
  align-items: center;
  justify-content: center;
  border: 1px solid rgba(148, 163, 184, 0.2);
  border-radius: 999px;
  background: rgba(248, 250, 252, 0.94);
  color: #475569;
  padding: 0.1rem 0.45rem;
  font-size: 0.68rem;
  font-weight: 800;
}

.gestion-child-meta-separator {
  margin: 0 0.3rem;
  color: #94a3b8;
}

.gestion-list button {
  border-color: rgba(148, 163, 184, 0.24);
  background: rgba(255, 255, 255, 0.96);
  color: #0f766e;
  cursor: pointer;
  transition:
    border-color 160ms ease,
    background-color 160ms ease,
    color 160ms ease,
    box-shadow 160ms ease,
    transform 140ms ease;
}

.gestion-list button:hover {
  border-color: rgba(45, 212, 191, 0.34);
  background: rgba(240, 253, 250, 0.9);
  color: #0f766e;
}

.gestion-row-actions {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.45rem;
}

.gestion-icon-button,
.gestion-expand-button {
  display: inline-grid;
  width: 2.35rem;
  height: 2.35rem;
  place-items: center;
  border-radius: 999px;
  box-shadow: 0 6px 16px rgba(15, 23, 42, 0.05);
}

.gestion-expand-button {
  flex: 0 0 auto;
}

.gestion-icon-button--danger {
  border-color: rgba(239, 68, 68, 0.24) !important;
  color: #b91c1c !important;
}

.gestion-icon-button--danger:hover {
  border-color: rgba(239, 68, 68, 0.46) !important;
  background: #fef2f2 !important;
  color: #991b1b !important;
}

.gestion-child-stack {
  display: grid;
  gap: 0.55rem;
  border-top: 1px solid rgba(148, 163, 184, 0.18);
  padding-top: 0.75rem;
}

.gestion-child-card {
  border: 1px solid rgba(148, 163, 184, 0.18);
  border-radius: 16px;
  background: rgba(248, 250, 252, 0.86);
  padding: 0.75rem;
}

.gestion-list :is(.text-emerald-400) {
  color: #047857;
}

.gestion-list :is(.text-red-400) {
  color: #dc2626;
}

.gestion-list table {
  width: 100%;
  min-width: 0;
  table-layout: fixed;
  overflow: hidden;
  border: 1px solid rgba(148, 163, 184, 0.18);
  border-radius: 18px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.985), rgba(248, 250, 252, 0.97)),
    #ffffff;
  color: #0f172a;
  box-shadow: 0 12px 28px rgba(15, 23, 42, 0.04);
}

.gestion-col-select {
  width: 3%;
}

.gestion-col-name {
  width: 21%;
}

.gestion-col-type {
  width: 10%;
}

.gestion-col-subcategory {
  width: 9%;
}

.gestion-col-quantity {
  width: 7%;
}

.gestion-col-retail {
  width: 13%;
}

.gestion-col-resell {
  width: 10%;
}

.gestion-col-date {
  width: 8%;
}

.gestion-col-profit {
  width: 6%;
}

.gestion-col-actions {
  width: 5%;
}

.gestion-list thead {
  position: sticky;
  top: 0;
  z-index: 7;
  border-color: rgba(148, 163, 184, 0.2);
  background:
    linear-gradient(180deg, rgba(250, 252, 255, 0.99), rgba(241, 245, 249, 0.98)),
    #f8fafc;
}

.gestion-list th {
  color: #1e293b;
  font-weight: 900;
}

.gestion-list th,
.gestion-list td {
  min-width: 0;
  padding-inline: 0.7rem;
  overflow-wrap: anywhere;
  word-break: break-word;
  vertical-align: middle;
}

.gestion-list tbody tr {
  border-color: rgba(226, 232, 240, 0.78);
}

.gestion-parent-row td {
  background: rgba(255, 255, 255, 0.96);
}

.gestion-child-row td {
  background: rgba(248, 250, 252, 0.74);
}

.gestion-list td {
  color: #334155;
}

.gestion-list td:first-child,
.gestion-list th:first-child {
  padding-left: 0.85rem;
}

.gestion-list td:last-child,
.gestion-list th:last-child {
  padding-right: 0.85rem;
}

.gestion-cell-name,
.gestion-name-block {
  min-width: 0;
}

.gestion-name-text {
  display: -webkit-box;
  min-width: 0;
  overflow: hidden;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.gestion-name-subtitle {
  display: block;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.gestion-cell-subcategory,
.gestion-cell-quantity,
.gestion-cell-date,
.gestion-cell-profit {
  line-height: 1.25;
}

.gestion-cell-retail,
.gestion-cell-resell {
  line-height: 1.35;
  white-space: normal;
  color: #475569;
}

.gestion-row-actions {
  flex-wrap: nowrap;
}

.gestion-type-badge {
  border-width: 1px;
  font-weight: 800;
  letter-spacing: 0.06em;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.58);
}

.gestion-type-badge--default {
  border-color: rgba(14, 165, 233, 0.2);
  background: rgba(239, 246, 255, 0.96);
  color: #0369a1;
}

.gestion-type-badge--ticket {
  border-color: rgba(245, 158, 11, 0.22);
  background: rgba(255, 247, 237, 0.96);
  color: #b45309;
}

.gestion-type-badge--pokemon {
  border-color: rgba(20, 184, 166, 0.22);
  background: rgba(240, 253, 250, 0.96);
  color: #0f766e;
}

.gestion-type-badge--other {
  border-color: rgba(148, 163, 184, 0.24);
  background: rgba(248, 250, 252, 0.96);
  color: #475569;
}

@media (max-width: 640px) {
  .gestion-child-card {
    padding: 0.68rem;
  }
}
</style>
