<template>
  <div class="gestion-list space-y-3">
    <!-- mini barre selection -->
    <div v-if="selectable" class="gestion-selection-bar mb-3 flex items-center justify-between">
      <p class="text-xs text-gray-400">{{ modelValue.length }} selectionnee(s)</p>

      <button
        type="button"
        class="text-xs text-gray-300 underline hover:text-white"
        @click="toggleAll"
      >
        {{ allSelected ? 'Tout deselectionner' : 'Tout selectionner' }}
      </button>
    </div>

    <div v-if="snkVentes.length" class="space-y-3 lg:hidden">
      <article
        v-for="vente in snkVentes"
        :key="vente.id"
        class="rounded-2xl border border-gray-700 bg-gray-900/60 p-3"
      >
        <div class="flex items-start justify-between gap-3">
          <div class="min-w-0 space-y-1">
            <p class="truncate text-sm font-semibold text-gray-100">
              {{ vente.nomItem || vente.nom_item }}
            </p>
            <p v-if="vente.description" class="line-clamp-2 text-xs text-gray-400">
              {{ vente.description }}
            </p>
          </div>

          <div class="flex items-center gap-2">
            <input
              v-if="selectable"
              type="checkbox"
              class="accent-purple-500"
              :checked="isSelected(vente.id)"
              @change="toggleOne(vente.id)"
            />

            <button
              type="button"
              class="inline-flex h-8 w-8 items-center justify-center rounded-full border border-purple-500/60 bg-gray-800 text-purple-200 transition hover:border-purple-300 hover:bg-purple-600/30 hover:text-white"
              @click="$emit('edit', vente)"
            >
              <svg class="h-3.5 w-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  stroke-width="2"
                  d="M15.232 5.232l3.536 3.536M4 20h4.586a1 1 0 00.707-.293l9.414-9.414a2 2 0 000-2.828l-2.172-2.172a2 2 0 00-2.828 0L4 14.586A1 1 0 003.707 15.293L4 20z"
                />
              </svg>
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
              class="inline-flex max-w-[11rem] items-center rounded-full border border-gray-700 bg-gray-950/60 px-2 py-0.5 text-[11px] font-medium text-gray-300"
            >
              <span class="truncate">{{ subcategoryLabel(vente) }}</span>
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
      v-else
      class="rounded-xl border border-gray-700/70 bg-gray-900/40 px-4 py-8 text-center text-sm text-gray-400 lg:hidden"
    >
      Aucun item a afficher pour le moment.
    </div>

    <div class="hidden lg:block">
      <table class="min-w-[920px] w-full text-sm text-gray-100">
        <thead class="border-b border-gray-700 bg-gray-900">
          <tr>
            <!-- nouvelle colonne selection -->
            <th
              v-if="selectable"
              class="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-gray-400"
            >
              <input
                type="checkbox"
                :checked="allSelected"
                @change="toggleAll"
                class="accent-purple-500"
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

        <tbody class="[&>tr:hover>td]:bg-gray-900/70 [&>tr:hover>td]:transition-colors">
          <tr v-for="vente in snkVentes" :key="vente.id" class="border-b border-gray-800">
            <!-- checkbox -->
            <td v-if="selectable" class="px-4 py-3">
              <input
                type="checkbox"
                class="accent-purple-500"
                :checked="isSelected(vente.id)"
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
              <button
                type="button"
                class="inline-flex h-8 w-8 items-center justify-center rounded-full border border-purple-500/60 bg-gray-800 text-purple-200 transition hover:border-purple-300 hover:bg-purple-600/30 hover:text-white"
                @click="$emit('edit', vente)"
              >
                <svg class="h-3.5 w-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path
                    stroke-linecap="round"
                    stroke-linejoin="round"
                    stroke-width="2"
                    d="M15.232 5.232l3.536 3.536M4 20h4.586a1 1 0 00.707-.293l9.414-9.414a2 2 0 000-2.828l-2.172-2.172a2 2 0 00-2.828 0L4 14.586A1 1 0 003.707 15.293L4 20z"
                  />
                </svg>
              </button>
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
import { formatDateFR, formatEUR } from '@/utils/formatters'
import { profitOf } from '@/utils/snkVente'
import { isItemCategoryAlias, itemTypeLabel, readStoredItemCategories } from '@/RegleItem/itemCategoryStore'
import { useAuthStore } from '@/store/authStore'

const props = defineProps({
  snkVentes: { type: Array, required: true },

  // mode selection
  selectable: { type: Boolean, default: false },

  // v-model = liste d'IDs selectionnes
  modelValue: { type: Array, default: () => [] },
})

const emit = defineEmits(['edit', 'update:modelValue'])
const authStore = useAuthStore()
const currentUserId = computed(() => authStore.user?.value?.id ?? authStore.user?.id ?? 'guest')
const categoryLabels = ref(readStoredItemCategories(currentUserId.value))

const isSelected = (id) => props.modelValue.includes(id)

const allSelected = computed(() => {
  if (!props.snkVentes.length) return false
  return props.snkVentes.every((v) => props.modelValue.includes(v.id))
})

const toggleOne = (id) => {
  const next = new Set(props.modelValue)
  if (next.has(id)) next.delete(id)
  else next.add(id)
  emit('update:modelValue', Array.from(next))
}

const toggleAll = () => {
  const visibleIds = props.snkVentes.map((v) => v.id)

  if (allSelected.value) {
    // enleve ceux visibles
    const next = props.modelValue.filter((id) => !visibleIds.includes(id))
    emit('update:modelValue', next)
  } else {
    // ajoute ceux visibles
    const next = new Set(props.modelValue)
    visibleIds.forEach((id) => next.add(id))
    emit('update:modelValue', Array.from(next))
  }
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
const typeLabelDisplay = (type) => itemTypeLabel(type || 'SNEAKER', categoryLabels.value).toUpperCase()

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
})

onBeforeUnmount(() => {
  window.removeEventListener('snk:item-categories-change', onCategoryLabelsChange)
})
</script>

<style scoped>
.gestion-list {
  color: #0f172a;
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

.gestion-list :is(.bg-gray-900, .bg-gray-900\/60, .bg-gray-900\/40, .bg-gray-950\/60, .bg-gray-950\/40) {
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.96), rgba(248, 250, 252, 0.9)),
    #ffffff;
}

.gestion-list :is(.border-gray-700, .border-gray-700\/70, .border-gray-700\/80, .border-gray-800) {
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

.gestion-list article {
  border-color: rgba(125, 211, 252, 0.34);
  background:
    linear-gradient(135deg, rgba(236, 253, 245, 0.5), transparent 42%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(248, 250, 252, 0.92));
  box-shadow: 0 14px 30px rgba(15, 23, 42, 0.06);
}

.gestion-list article:hover {
  border-color: rgba(20, 184, 166, 0.46);
  box-shadow: 0 18px 38px rgba(14, 116, 144, 0.09);
}

.gestion-list input[type='checkbox'] {
  accent-color: #0f766e;
}

.gestion-selection-bar {
  position: sticky;
  top: 0;
  z-index: 8;
  min-height: 44px;
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

.gestion-list button {
  border-color: rgba(14, 116, 144, 0.28);
  background: rgba(255, 255, 255, 0.82);
  color: #0f766e;
}

.gestion-list button:hover {
  border-color: rgba(20, 184, 166, 0.5);
  background: #ecfdf5;
  color: #0f766e;
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
  top: 56px;
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

.gestion-list tbody tr:hover td {
  background: rgba(236, 253, 245, 0.7);
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

.gestion-list :is(.border-purple-400\/60, .border-cyan-400\/60, .border-amber-400\/60, .border-slate-400\/60) {
  border-color: rgba(20, 184, 166, 0.4);
}
</style>
