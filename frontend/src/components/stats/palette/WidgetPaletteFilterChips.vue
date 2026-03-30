<template>
  <div class="palette-filters">
    <div class="palette-filter-group" role="tablist" aria-label="Filtrer par categorie">
      <button
        v-for="cat in categories"
        :key="cat.value"
        type="button"
        role="tab"
        class="filter-chip"
        :class="{ 'is-active': category === cat.value }"
        :aria-selected="category === cat.value"
        @click="emit('update:category', cat.value)"
      >
        <span
          v-if="cat.color"
          class="filter-chip__dot"
          :style="{ backgroundColor: cat.color }"
          aria-hidden="true"
        ></span>
        {{ cat.label }}
      </button>
    </div>

    <div class="palette-filter-group" role="tablist" aria-label="Filtrer par type de donnees">
      <button
        v-for="type in dataTypes"
        :key="type.value"
        type="button"
        role="tab"
        class="filter-chip filter-chip--soft"
        :class="{ 'is-active': dataType === type.value }"
        :aria-selected="dataType === type.value"
        @click="emit('update:dataType', type.value)"
      >
        {{ type.label }}
      </button>
    </div>

  </div>
</template>

<script setup lang="ts">
import type { WidgetDataType } from './types'

type Chip = { label: string; value: string; color?: string }
type DataChip = { label: string; value: WidgetDataType }

defineProps<{
  categories: Chip[]
  dataTypes: DataChip[]
  category: string
  dataType: WidgetDataType
}>()

const emit = defineEmits<{
  (event: 'update:category', value: string): void
  (event: 'update:dataType', value: WidgetDataType): void
}>()
</script>

<style scoped>
.palette-filters {
  display: grid;
  grid-template-columns: 1fr;
  gap: 10px;
}

.palette-filter-group {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.filter-chip {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  height: 30px;
  padding: 0 12px;
  border-radius: 999px;
  border: 1px solid rgba(148, 163, 184, 0.62);
  background: rgba(226, 232, 240, 0.78);
  color: #475569;
  font-size: 12px;
  font-weight: 600;
  transition: border-color 130ms ease, color 130ms ease, background 130ms ease;
}

.filter-chip__dot {
  width: 7px;
  height: 7px;
  border-radius: 999px;
}

.filter-chip.is-active {
  border-color: rgba(37, 99, 235, 0.52);
  color: #1e3a8a;
  background: rgba(191, 219, 254, 0.76);
}

.filter-chip--soft {
  background: rgba(203, 213, 225, 0.68);
}

@media (max-width: 720px) {
  .palette-filter-group {
    flex-wrap: nowrap;
    overflow-x: auto;
    scrollbar-width: none;
  }

  .palette-filter-group::-webkit-scrollbar {
    display: none;
  }

  .filter-chip {
    flex: 0 0 auto;
  }
}
</style>
