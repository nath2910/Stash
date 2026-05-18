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
const formattedStockValue = computed(() =>
  String(Math.round(props.valeurStock)).replace(/\B(?=(\d{3})+(?!\d))/g, ' '),
)
</script>
