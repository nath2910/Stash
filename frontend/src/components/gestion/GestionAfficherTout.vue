<template>
  <div class="overflow-x-auto">
    <!-- mini barre selection -->
    <div v-if="selectable" class="mb-3 flex items-center justify-between">
      <p class="text-xs text-gray-400">{{ modelValue.length }} selectionnee(s)</p>

      <button
        type="button"
        class="text-xs text-gray-300 underline hover:text-white"
        @click="toggleAll"
      >
        {{ allSelected ? 'Tout deselectionner' : 'Tout selectionner' }}
      </button>
    </div>

    <table class="min-w-full text-sm text-gray-100">
      <thead class="bg-gray-900 border-b border-gray-700">
        <tr>
          <!-- nouvelle colonne selection -->
          <th
            v-if="selectable"
            class="px-4 py-3 text-left font-semibold text-xs uppercase tracking-wide text-gray-400"
          >
            <input
              type="checkbox"
              :checked="allSelected"
              @change="toggleAll"
              class="accent-purple-500"
            />
          </th>

          <th
            class="px-4 py-3 text-left font-semibold text-xs uppercase tracking-wide text-gray-400"
          >
            nom de l'item
          </th>
          <th
            class="px-4 py-3 text-left font-semibold text-xs uppercase tracking-wide text-gray-400"
          >
            type
          </th>
          <th
            class="px-4 py-3 text-left font-semibold text-xs uppercase tracking-wide text-gray-400"
          >
            categorie
          </th>
          <th
            class="px-4 py-3 text-right font-semibold text-xs uppercase tracking-wide text-gray-400"
          >
            prix_retail
          </th>
          <th
            class="px-4 py-3 text-right font-semibold text-xs uppercase tracking-wide text-gray-400"
          >
            prix_resell
          </th>
          <th
            class="px-4 py-3 text-center font-semibold text-xs uppercase tracking-wide text-gray-400"
          >
            date achat
          </th>
          <th
            class="px-4 py-3 text-center font-semibold text-xs uppercase tracking-wide text-gray-400"
          >
            date vente
          </th>
          <th
            class="px-4 py-3 text-right font-semibold text-xs uppercase tracking-wide text-gray-400"
          >
            profit
          </th>
          <th
            class="px-4 py-3 text-center font-semibold text-xs uppercase tracking-wide text-gray-400"
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
              <span v-if="vente.description" class="text-[11px] text-gray-400 line-clamp-1">
                {{ vente.description }}
              </span>
            </div>
          </td>

          <td class="px-4 py-3">
            <span
              class="inline-flex items-center px-2 py-0.5 rounded-full text-[11px] font-semibold uppercase tracking-wide"
              :class="typeBadgeClass(vente.type)"
            >
              {{ typeLabelDisplay(vente.type) }}
            </span>
          </td>

          <td class="px-4 py-3">
            <span
              class="inline-flex items-center px-2.5 py-1 rounded-full text-[11px] font-medium"
              :class="badgeClass(vente.categorie)"
            >
              {{ vente.categorie || '-' }}
            </span>
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
              class="inline-flex items-center justify-center w-8 h-8 rounded-full bg-gray-800 border border-purple-500/60 text-purple-200 hover:bg-purple-600/30 hover:border-purple-300 hover:text-white transition"
              @click="$emit('edit', vente)"
            >
              <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
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
</template>

<script setup>
import { computed } from 'vue'
import { formatDateFR, formatEUR } from '@/utils/formatters'
import { profitOf } from '@/utils/snkVente'
import { typeLabel } from '@/constants/itemTypes'

const props = defineProps({
  snkVentes: { type: Array, required: true },

  // mode selection
  selectable: { type: Boolean, default: false },

  // v-model = liste d'IDs selectionnes
  modelValue: { type: Array, default: () => [] },
})

const emit = defineEmits(['edit', 'update:modelValue'])

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
const badgeClass = (cat) => {
  if (!cat) return 'bg-gray-700 text-gray-100 border border-gray-500/60'
  const c = cat.toLowerCase()
  if (c.includes('jordan') || c.includes('sneakers') || c.includes('basket')) {
    return 'bg-purple-500/10 text-purple-200 border border-purple-400/60'
  }
  if (c.includes('running')) {
    return 'bg-emerald-500/10 text-emerald-200 border border-emerald-400/60'
  }
  return 'bg-gray-700 text-gray-100 border border-gray-500/60'
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
const typeLabelDisplay = (type) => typeLabel(type || 'SNEAKER')
</script>
