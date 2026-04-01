<!-- src/components/boutonOnAdd.vue -->
<template>
  <div>
    <div class="[&_button:hover]:bg-emerald-800">
      <button
        v-if="!showAdd"
        type="button"
        @click="showAdd = true"
        class="w-full rounded border border-emerald-300/30 bg-emerald-600 px-3 py-2 text-xs text-white transition active:scale-95 focus:outline-none focus:ring-2 focus:ring-emerald-400/40 sm:w-auto"
      >
        Ajouter un item
      </button>
    </div>
    <div class="[&_button:hover]:bg-gray-500">
      <AjoutPaire v-if="showAdd" @close="showAdd = false" @added="handleAdded" />
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import AjoutPaire from '@/components/gestion/GestionAjoutPaire.vue'

const emit = defineEmits(['vente-ajoutee'])
const showAdd = ref(false)

const handleAdded = () => {
  showAdd.value = false
  emit('vente-ajoutee')
}
</script>
