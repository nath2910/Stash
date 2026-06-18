<template>
  <div class="flex w-full justify-end sm:w-auto">
    <boutonOnAdd :items="items" @vente-ajoutee="$emit('vente-ajoutee')" />
  </div>
</template>

<script setup>
import boutonOnAdd from '@/components/gestion/GestionBoutonOnAdd.vue'

defineProps({
  items: { type: Array, default: () => [] },
})

defineEmits(['vente-ajoutee'])
</script>
