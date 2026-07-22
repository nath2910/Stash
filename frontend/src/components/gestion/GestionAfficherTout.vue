<template>
  <div class="gestion-list space-y-3">
    <div
      v-if="selectable && (!isDesktop || modelValue.length)"
      class="gestion-selection-bar mb-3 flex items-center justify-between"
    >
      <p class="text-xs text-gray-400">{{ modelValue.length }} selectionnee(s)</p>

      <button type="button" class="gestion-selection-toggle" @click="toggleAll">
        {{ allSelected ? 'Tout deselectionner' : 'Tout selectionner' }}
      </button>
    </div>

    <div v-if="!isDesktop && snkVentes.length" class="gestion-mobile-list space-y-3 lg:hidden">
      <article
        v-for="vente in snkVentes"
        :key="vente.id"
        class="gestion-list-row gestion-mobile-card rounded-2xl border border-gray-700 bg-gray-900/60 p-3"
        role="button"
        tabindex="0"
        @click="emit('edit', vente)"
        @keydown.enter.prevent="emit('edit', vente)"
        @keydown.space.prevent="emit('edit', vente)"
      >
        <div class="flex items-start justify-between gap-3">
          <div class="min-w-0 space-y-1">
            <p class="gestion-mobile-title text-sm font-semibold text-gray-100">
              {{ vente.nomItem || vente.nom_item }}
            </p>
            <p v-if="vente.description" class="gestion-mobile-description line-clamp-2 text-xs text-gray-400">
              {{ vente.description }}
            </p>
          </div>

          <div class="flex items-center gap-2">
            <input
              v-if="selectable"
              type="checkbox"
              class="accent-purple-500"
              :checked="isSelected(vente.id)"
              @click.stop
              @change="toggleOne(vente.id)"
            />

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
              @click.stop="emit('delete', [vente.id])"
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
            :class="profit(vente) >= 0 ? 'text-emerald-400' : 'text-red-400'"
          >
            {{ formatCurrency(profit(vente)) }}
          </span>
        </div>

        <div class="mt-3 grid grid-cols-1 gap-2 min-[380px]:grid-cols-2">
          <div class="rounded-lg border border-gray-700/80 bg-gray-950/40 px-2.5 py-2">
            <p class="text-[10px] uppercase tracking-wide text-gray-500">Retail</p>
            <p class="mt-1 text-xs font-medium text-gray-200">
              {{ formatCurrency(vente.prixRetail ?? vente.prix_retail) }}
            </p>
          </div>
          <div class="rounded-lg border border-gray-700/80 bg-gray-950/40 px-2.5 py-2">
            <p class="text-[10px] uppercase tracking-wide text-gray-500">Resell</p>
            <p class="mt-1 text-xs font-medium text-gray-200">
              {{ formatCurrency(vente.prixResell ?? vente.prix_resell) }}
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
              {{
                vente.dateVente || vente.date_vente
                  ? formatDate(vente.dateVente ?? vente.date_vente)
                  : '-'
              }}
            </p>
          </div>
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
      <table class="min-w-[920px] w-full text-sm text-gray-100">
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

            <th
              class="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-gray-400"
            >
              nom de l'item
            </th>
            <th
              class="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-gray-400"
            >
              type
            </th>
            <th
              class="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-gray-400"
            >
              sous-categorie
            </th>
            <th
              class="px-4 py-3 text-right text-xs font-semibold uppercase tracking-wide text-gray-400"
            >
              prix_retail
            </th>
            <th
              class="px-4 py-3 text-right text-xs font-semibold uppercase tracking-wide text-gray-400"
            >
              prix_resell
            </th>
            <th
              class="px-4 py-3 text-center text-xs font-semibold uppercase tracking-wide text-gray-400"
            >
              date achat
            </th>
            <th
              class="px-4 py-3 text-center text-xs font-semibold uppercase tracking-wide text-gray-400"
            >
              date vente
            </th>
            <th
              class="px-4 py-3 text-right text-xs font-semibold uppercase tracking-wide text-gray-400"
            >
              profit
            </th>
            <th
              class="px-4 py-3 text-center text-xs font-semibold uppercase tracking-wide text-gray-400"
            >
              actions
            </th>
          </tr>
        </thead>

        <tbody>
          <tr
            v-for="vente in snkVentes"
            :key="vente.id"
            class="gestion-list-row border-b border-gray-800"
            tabindex="0"
            @click="emit('edit', vente)"
            @keydown.enter.prevent="emit('edit', vente)"
            @keydown.space.prevent="emit('edit', vente)"
          >
            <td v-if="selectable" class="px-4 py-3">
              <input
                type="checkbox"
                class="accent-purple-500"
                :checked="isSelected(vente.id)"
                @click.stop
                @change="toggleOne(vente.id)"
              />
            </td>

            <td class="px-4 py-3">
              <div class="flex flex-col">
                <span class="font-medium text-gray-100">
                  {{ vente.nomItem || vente.nom_item }}
                </span>
                <span v-if="vente.description" class="line-clamp-1 text-[11px] text-gray-400">
                  {{ vente.description }}
                </span>
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

            <td class="px-4 py-3 text-xs text-gray-300">
              {{ subcategoryLabel(vente) || '--' }}
            </td>

            <td class="px-4 py-3 text-right">
              {{ formatCurrency(vente.prixRetail ?? vente.prix_retail) }}
            </td>

            <td class="px-4 py-3 text-right">
              {{ formatCurrency(vente.prixResell ?? vente.prix_resell) }}
            </td>

            <td class="px-4 py-3 text-center text-xs text-gray-300">
              {{ formatDate(vente.dateAchat ?? vente.date_achat) }}
            </td>

            <td class="px-4 py-3 text-center text-xs text-gray-300">
              {{
                vente.dateVente || vente.date_vente
                  ? formatDate(vente.dateVente ?? vente.date_vente)
                  : '-'
              }}
            </td>

            <td class="px-4 py-3 text-right">
              <span
                class="font-semibold"
                :class="profit(vente) >= 0 ? 'text-emerald-400' : 'text-red-400'"
              >
                {{ formatCurrency(profit(vente)) }}
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
                  @click.stop="emit('delete', [vente.id])"
                >
                  <Trash2 class="h-4 w-4" aria-hidden="true" />
                </button>
              </div>
            </td>
          </tr>

          <tr v-if="!snkVentes.length">
            <td :colspan="selectable ? 10 : 9" class="px-4 py-8 text-center text-sm text-gray-400">
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
import { Pencil, Trash2 } from 'lucide-vue-next'
import { formatDateFR, formatEUR } from '@/utils/formatters'
import { profitOf } from '@/utils/snkVente'
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
let desktopMediaQuery = null
const onDesktopChange = (event) => {
  isDesktop.value = event.matches
}

const selectedSet = computed(() => new Set(props.modelValue))
const visibleIds = computed(() => props.snkVentes.map((v) => v.id))

const isSelected = (id) => selectedSet.value.has(id)

const allSelected = computed(() => {
  if (!props.snkVentes.length) return false
  return visibleIds.value.every((id) => selectedSet.value.has(id))
})

const toggleOne = (id) => {
  const next = new Set(props.modelValue)
  if (next.has(id)) next.delete(id)
  else next.add(id)
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

const formatCurrency = (val) => {
  const num = Number(val)
  if (val === null || val === undefined || Number.isNaN(num)) return '--'
  return formatEUR(num, { digits: 0 })
}

const formatDate = (val) => formatDateFR(val, { fallback: '--' })

const profit = (vente) => profitOf(vente)
const subcategoryLabel = (vente) => {
  const label = String(vente?.categorie ?? '').trim()
  return isItemCategoryAlias(label, categoryLabels.value) ? '' : label
}
const typeBadgeClass = (type) => {
  switch (type) {
    case 'TICKET':
      return 'bg-amber-500/15 text-amber-200 border border-amber-400/60'
    case 'POKEMON_CARD':
      return 'bg-cyan-500/15 text-cyan-200 border border-cyan-400/60'
    case 'OTHER':
      return 'bg-slate-500/15 text-slate-200 border border-slate-400/60'
    default:
      return 'bg-purple-500/10 text-purple-200 border border-purple-400/60'
  }
}
const typeLabelDisplay = (type) =>
  itemTypeLabel(type || 'SNEAKER', categoryLabels.value).toUpperCase()

watch(
  () => currentUserId.value,
  (userId) => {
    categoryLabels.value = readStoredItemCategories(userId)
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
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.96), rgba(248, 250, 252, 0.9)),
    #ffffff;
}

.gestion-list
  :is(.border-gray-700, .border-gray-700\/70, .border-gray-700\/80, .border-gray-800) {
  border-color: rgba(125, 211, 252, 0.32);
}

.gestion-list > div:first-child {
  border: 1px solid rgba(125, 211, 252, 0.28);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.74);
  padding: 0.55rem 0.85rem;
}

.gestion-list > div:first-child p,
.gestion-list > div:first-child button {
  color: #0f766e;
  font-weight: 800;
}

.gestion-list-row {
  cursor: pointer;
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
  border-color: rgba(125, 211, 252, 0.34);
  background:
    linear-gradient(135deg, rgba(236, 253, 245, 0.5), transparent 42%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(248, 250, 252, 0.92));
  box-shadow: 0 14px 30px rgba(15, 23, 42, 0.06);
}

.gestion-list article:hover,
.gestion-list article:focus-visible {
  border-color: rgba(20, 184, 166, 0.46);
  box-shadow: 0 18px 38px rgba(14, 116, 144, 0.09);
  transform: translateY(-1px);
}

.gestion-list article:active {
  transform: translateY(0) scale(0.995);
}

.gestion-list input[type='checkbox'] {
  min-width: 1rem;
  min-height: 1rem;
  accent-color: #0f766e;
  cursor: pointer;
}

.gestion-selection-bar {
  position: sticky;
  top: 0;
  z-index: 8;
  width: 100%;
  min-width: 0;
  min-height: 44px;
  gap: 0.55rem;
  flex-wrap: wrap;
  border: 1px solid rgba(125, 211, 252, 0.34);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.96);
  padding: 0.55rem 0.9rem;
  box-shadow: 0 12px 24px rgba(15, 23, 42, 0.06);
}

.gestion-selection-bar p,
.gestion-selection-bar button {
  color: #0f766e;
  font-weight: 900;
}

.gestion-selection-toggle {
  min-height: 2rem;
  min-width: 0;
  max-width: 100%;
  border-radius: 999px;
  padding: 0 0.65rem;
  cursor: pointer;
  text-decoration: underline;
  text-underline-offset: 3px;
}

.gestion-mobile-list,
.gestion-mobile-card {
  width: 100%;
  min-width: 0;
  max-width: 100%;
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

.gestion-list button {
  border-color: rgba(14, 116, 144, 0.28);
  background: rgba(255, 255, 255, 0.82);
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
  border-color: rgba(20, 184, 166, 0.5);
  background: #ecfdf5;
  color: #0f766e;
}

.gestion-list button:active {
  transform: translateY(0) scale(0.96);
}

.gestion-row-actions {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.45rem;
}

.gestion-icon-button {
  display: inline-grid;
  width: 2.35rem;
  height: 2.35rem;
  place-items: center;
  border-radius: 999px;
  box-shadow: 0 8px 18px rgba(14, 116, 144, 0.08);
}

.gestion-icon-button:hover {
  box-shadow: 0 12px 22px rgba(14, 116, 144, 0.13);
  transform: translateY(-1px);
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

.gestion-list :is(.text-emerald-400) {
  color: #047857;
}

.gestion-list :is(.text-red-400) {
  color: #dc2626;
}

.gestion-list table {
  overflow: hidden;
  border: 1px solid rgba(125, 211, 252, 0.3);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.92);
  color: #0f172a;
}

.gestion-list thead {
  position: sticky;
  top: 0;
  z-index: 7;
  border-color: rgba(125, 211, 252, 0.28);
  background: linear-gradient(135deg, #ecfdf5, #e0f2fe);
}

.gestion-list th {
  color: #0f766e;
  font-weight: 900;
}

.gestion-list tbody tr {
  border-color: rgba(226, 232, 240, 0.9);
}

.gestion-list tbody tr td {
  transition:
    background-color 160ms ease,
    color 160ms ease;
}

.gestion-list tbody tr.gestion-list-row:hover td,
.gestion-list tbody tr.gestion-list-row:focus-visible td {
  background: rgba(236, 253, 245, 0.78);
}

.gestion-list tbody tr.gestion-list-row:active td {
  background: rgba(204, 251, 241, 0.9);
}

.gestion-list td {
  color: #334155;
}

.gestion-list td:first-child,
.gestion-list th:first-child {
  padding-left: 1rem;
}

.gestion-list :is(.bg-purple-500\/10, .bg-cyan-500\/15, .bg-amber-500\/15, .bg-slate-500\/15) {
  background: rgba(240, 253, 250, 0.86);
}

.gestion-list :is(.text-purple-200, .text-cyan-200, .text-amber-200, .text-slate-200) {
  color: #0f766e;
}

.gestion-list
  :is(
    .border-purple-400\/60,
    .border-cyan-400\/60,
    .border-amber-400\/60,
    .border-slate-400\/60
  ) {
  border-color: rgba(20, 184, 166, 0.4);
}
</style>
