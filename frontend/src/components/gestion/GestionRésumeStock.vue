<template>
  <div class="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-3">
    <StatBadge label="Nombre total d'item achete" :value="totalPaires" />
    <StatBadge label="En stock" tone="success" :value="nbEnStock" />
    <StatBadge label="Vendues" tone="accent" :value="nbVendues" />
    <StatBadge label="Valeur stock" :value="valeurStock.toFixed(2)" unit="EUR" />
  </div>
</template>

<script setup lang="ts">
import StatBadge from '@/components/StatBadge.vue'

interface Props {
  totalPaires: number
  nbEnStock: number
  nbVendues: number
  valeurStock: number
}

defineProps<Props>()
</script>
