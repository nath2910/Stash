<!-- src/components/boutonOnAdd.vue -->
<template>
  <div>
    <div>
      <button
        v-if="!showAdd"
        type="button"
        @click="showAdd = true"
        class="inline-flex w-full min-h-10 items-center justify-center rounded-full border border-teal-700/20 bg-gradient-to-br from-teal-700 to-cyan-700 px-4 py-2 text-xs font-extrabold text-white shadow-[0_12px_24px_rgba(15,118,110,0.18)] transition active:scale-95 focus:outline-none focus:ring-2 focus:ring-teal-400/30 sm:w-auto"
      >
        Ajouter un item
      </button>
    </div>
    <div>
      <AjoutPaire v-if="showAdd" @close="showAdd = false" @added="handleAdded" />
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AjoutPaire from '@/components/gestion/GestionAjoutPaire.vue'

const emit = defineEmits(['vente-ajoutee'])
const showAdd = ref(false)
const route = useRoute()
const router = useRouter()

const consumeAddQuery = async () => {
  if (route.query.action !== 'add') return
  showAdd.value = true
  const nextQuery = { ...route.query }
  delete nextQuery.action
  await router.replace({ query: nextQuery })
}

onMounted(() => {
  void consumeAddQuery()
})

watch(
  () => route.query.action,
  (action) => {
    if (action !== 'add') return
    void consumeAddQuery()
  },
)

const handleAdded = () => {
  showAdd.value = false
  emit('vente-ajoutee')
}
</script>
