<template>
  <div class="grid h-full grid-cols-1 gap-3 sm:grid-cols-3">
    <StatBadge label="Total achete" :value="totalPaires" variant="dashboard" />
    <StatBadge label="En stock" tone="success" :value="nbEnStock" variant="dashboard" />
    <StatBadge
      label="Valeur stock"
      tone="accent"
      :value="formattedStockValue"
      unit="EUR"
      variant="dashboard"
    />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import StatBadge from '@/components/StatBadge.vue'

interface Props {
  totalPaires: number
  nbEnStock: number
  valeurStock: number
}

const props = defineProps<Props>()
const compactNumberFormatter = new Intl.NumberFormat('fr-FR', {
  notation: 'compact',
  maximumFractionDigits: 1,
})

const formattedStockValue = computed(() => {
  const value = Math.max(0, Math.round(Number(props.valeurStock) || 0))
  if (value < 10_000) {
    return String(value).replace(/\B(?=(\d{3})+(?!\d))/g, ' ')
  }
  return compactNumberFormatter.format(value).replace(/\s+/g, ' ')
})
</script>
