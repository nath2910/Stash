<template>
  <section class="mt-6 space-y-3 rounded-2xl border border-slate-700 bg-slate-900/70 p-5 text-slate-100">
    <h2 class="font-semibold">Mes données personnelles</h2>
    <form class="flex flex-wrap gap-3" @submit.prevent="save">
      <label>Prénom <input v-model="firstName" maxlength="100" class="block rounded-lg bg-slate-800 p-2" /></label>
      <label>Nom <input v-model="lastName" maxlength="100" class="block rounded-lg bg-slate-800 p-2" /></label>
      <button type="submit" :disabled="busy" class="self-end rounded-lg border border-slate-500 px-3 py-2">Enregistrer</button>
    </form>
    <button type="button" :disabled="busy" class="underline" @click="download">Exporter mes données (JSON)</button>
    <p role="status" class="text-sm">{{ message }}</p>
    <RouterLink to="/privacy" class="text-sm underline">Confidentialité et exercice de mes droits</RouterLink>
  </section>
</template>
<script setup>
import { ref } from 'vue'
import { RouterLink } from 'vue-router'
import api from '@/services/api'
import { useAuthStore } from '@/store/authStore'
const auth = useAuthStore()
const firstName = ref(auth.user.value?.firstName || '')
const lastName = ref(auth.user.value?.lastName || '')
const message = ref('')
const busy = ref(false)
async function save() {
  busy.value = true
  try {
    auth.setUser((await api.put('/user/profile', { firstName: firstName.value, lastName: lastName.value })).data)
    message.value = 'Informations enregistrées.'
  } catch { message.value = 'Enregistrement impossible. Réessayez.' }
  finally { busy.value = false }
}
async function download() {
  busy.value = true
  try {
    const data = (await api.get('/user/export')).data
    const url = URL.createObjectURL(new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' }))
    const link = document.createElement('a')
    link.href = url; link.download = 'stash-donnees.json'; link.click()
    setTimeout(() => URL.revokeObjectURL(url), 1000)
    message.value = 'Export préparé. Les pièces jointes se téléchargent depuis vos articles.'
  } catch { message.value = 'Export impossible. Réessayez ou contactez le responsable des données.' }
  finally { busy.value = false }
}
</script>
