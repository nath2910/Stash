<template>
  <div class="results-dropdown" role="listbox">
    <div v-if="loading" class="dropdown-state">Chargement des items...</div>
    <div v-else-if="empty" class="dropdown-state dropdown-state--empty">
      Aucun item ne correspond a cette recherche.
    </div>
    <button
      v-else
      v-for="(item, index) in items"
      :id="optionId(index)"
      :key="item.id ?? `${item.nomItem}-${index}`"
      type="button"
      class="result-row"
      :class="{ 'is-active': index === activeIndex }"
      role="option"
      :aria-selected="index === activeIndex"
      @mouseenter="$emit('hover', index)"
      @mousedown.prevent="$emit('select', item)"
    >
      <span class="result-icon">
        <Package class="h-4 w-4" aria-hidden="true" />
      </span>
      <span class="result-main">
        <span class="result-name">{{ itemName(item) }}</span>
        <span class="result-meta">
          <span>{{ category(item) }}</span>
          <span v-if="subcategory(item)">{{ subcategory(item) }}</span>
          <span v-if="size(item)">{{ size(item) }}</span>
          <span class="status-chip" :class="statusClass(item)">{{ status(item) }}</span>
        </span>
      </span>
      <span class="result-side">
        <span class="result-price">{{ price(item) }}</span>
        <span class="result-action">Ouvrir</span>
      </span>
    </button>
  </div>
</template>

<script setup>
import { Package } from 'lucide-vue-next'
import { itemTypeLabel } from '@/RegleItem/itemCategoryStore'
import { formatEUR } from '@/utils/formatters'
import { getField, prixResellOf, prixRetailOf, typeOf } from '@/utils/snkVente'
import { normalizeItemStatus } from '@/constants/statuses'

const props = defineProps({
  items: { type: Array, default: () => [] },
  activeIndex: { type: Number, default: -1 },
  loading: { type: Boolean, default: false },
  empty: { type: Boolean, default: false },
  optionId: { type: Function, required: true },
  categoryLabels: { type: Object, default: () => ({}) },
})

defineEmits(['select', 'hover'])

const itemName = (item) => getField(item, 'nomItem', 'Item sans nom')
const category = (item) => itemTypeLabel(typeOf(item), props.categoryLabels)
const subcategory = (item) => getField(item, 'categorie', '')
const size = (item) => item?.metadata?.size || ''
const status = (item) => normalizeItemStatus(item).label
const statusClass = (item) => `is-${normalizeItemStatus(item).key.replace('_', '-')}`
const price = (item) => {
  const resell = prixResellOf(item)
  const retail = prixRetailOf(item)
  if (resell > 0) return formatEUR(resell)
  return retail > 0 ? formatEUR(retail) : '--'
}
</script>

<style scoped>
.results-dropdown {
  position: absolute;
  inset-inline: 0;
  top: calc(100% + 0.5rem);
  z-index: 30;
  overflow: hidden;
  border: 1px solid rgba(125, 211, 252, 0.28);
  border-radius: 18px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.99), rgba(248, 250, 252, 0.97)),
    #ffffff;
  box-shadow:
    0 24px 56px rgba(15, 23, 42, 0.18),
    inset 0 1px 0 rgba(255, 255, 255, 0.86);
  transform-origin: top;
  animation: resultsIn 140ms ease-out both;
}

.dropdown-state {
  padding: 1rem 1.1rem;
  color: #64748b;
  font-size: 0.9rem;
  font-weight: 700;
}

.dropdown-state--empty {
  color: #475569;
}

.result-row {
  width: 100%;
  min-height: 72px;
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: 0.85rem;
  border: 0;
  border-bottom: 1px solid rgba(226, 232, 240, 0.9);
  background: transparent;
  padding: 0.85rem 1rem;
  text-align: left;
  transition:
    background 140ms ease,
    color 140ms ease;
}

.result-row:last-child {
  border-bottom: 0;
}

.result-row:hover,
.result-row.is-active {
  background: linear-gradient(135deg, #f0fdfa, #eff6ff);
}

.result-icon {
  display: inline-grid;
  width: 2.25rem;
  height: 2.25rem;
  place-items: center;
  border: 1px solid rgba(14, 165, 233, 0.18);
  border-radius: 12px;
  background: linear-gradient(135deg, #e0f2fe, #ecfdf5);
  color: #0369a1;
}

.result-main {
  min-width: 0;
  display: grid;
  gap: 0.25rem;
}

.result-name {
  overflow: hidden;
  color: #0f172a;
  font-size: 0.95rem;
  font-weight: 800;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.result-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 0.4rem;
  min-width: 0;
  color: #64748b;
  font-size: 0.74rem;
  font-weight: 600;
}

.result-meta span {
  max-width: 100%;
  min-width: 0;
  overflow: hidden;
  border-radius: 999px;
  background: rgba(241, 245, 249, 0.95);
  padding: 0.18rem 0.5rem;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.status-chip.is-stock,
.status-chip.is-in-stock {
  background: #ecfdf5;
  color: #047857;
}

.status-chip.is-sold {
  background: #f1f5f9;
  color: #475569;
}

.result-side {
  display: grid;
  justify-items: end;
  gap: 0.18rem;
}

.result-price {
  color: #0369a1;
  font-size: 0.9rem;
  font-weight: 800;
  white-space: nowrap;
}

.result-action {
  color: #94a3b8;
  font-size: 0.68rem;
  font-weight: 800;
  text-transform: uppercase;
}

@keyframes resultsIn {
  from {
    opacity: 0;
    transform: translateY(-4px) scale(0.985);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

@media (max-width: 560px) {
  .result-row {
    grid-template-columns: auto minmax(0, 1fr);
    gap: 0.45rem;
  }

  .result-side {
    grid-column: 2;
    justify-self: start;
    justify-items: start;
  }

  .result-meta span {
    max-width: min(100%, 12rem);
  }
}

@media (prefers-reduced-motion: reduce) {
  .results-dropdown {
    animation: none;
  }
}
</style>
