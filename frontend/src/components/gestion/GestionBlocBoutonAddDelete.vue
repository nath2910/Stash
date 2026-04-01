<template>
  <div class="flex w-full justify-end sm:w-auto">
    <boutonOnAdd @vente-ajoutee="$emit('vente-ajoutee')" />
  </div>
</template>

<script setup>
import boutonOnAdd from '@/components/gestion/GestionBoutonOnAdd.vue'
defineEmits(['vente-ajoutee'])
</script>
