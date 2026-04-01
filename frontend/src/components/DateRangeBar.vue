<template>
  <div class="flex flex-wrap items-center gap-3">
    <div class="rounded-2xl border border-white/10 bg-white/5 px-3 py-2">
      <div class="text-xs text-white/60 mb-1">Période</div>

      <VueDatePicker
        v-model="range"
        range
        :enable-time-picker="false"
        :dark="true"
        auto-apply
        format="yyyy-MM-dd"
        class="dp"
      />
    </div>

    <div class="flex items-center gap-2">
      <button class="chip" @click="preset('month')">Mois</button>
      <button class="chip" @click="preset('ytd')">YTD</button>
      <button class="chip" @click="preset('year')">Année</button>
    </div>

    <div class="ml-auto text-sm text-white/60">{{ from }} → {{ to }}</div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { VueDatePicker } from '@vuepic/vue-datepicker'
import '@vuepic/vue-datepicker/dist/main.css'

const props = defineProps({
  from: { type: String, required: true },
  to: { type: String, required: true },
})
const emit = defineEmits(['update:from', 'update:to'])

function toISO(d: Date) {
  // (tu peux laisser, c’est ok pour tester)
  return new Date(d).toISOString().slice(0, 10)
}

const range = ref<[Date, Date]>([new Date(props.from), new Date(props.to)])

watch(
  () => [props.from, props.to],
  () => {
    range.value = [new Date(props.from), new Date(props.to)]
  },
)

watch(range, (val) => {
  if (!val?.[0] || !val?.[1]) return
  emit('update:from', toISO(val[0]))
  emit('update:to', toISO(val[1]))
})

function preset(kind: 'month' | 'ytd' | 'year') {
  const now = new Date()
  if (kind === 'month')
    range.value = [
      new Date(now.getFullYear(), now.getMonth(), 1),
      new Date(now.getFullYear(), now.getMonth() + 1, 0),
    ]
  if (kind === 'ytd') range.value = [new Date(now.getFullYear(), 0, 1), now]
  if (kind === 'year')
    range.value = [new Date(now.getFullYear(), 0, 1), new Date(now.getFullYear(), 11, 31)]
}
</script>

<style scoped>
.dp :deep(.dp__input) {
  border: none !important;
  background: transparent !important;
  color: rgba(255, 255, 255, 0.92) !important;
  padding: 0 !important;
  min-width: 260px;
}
.chip {
  height: 40px;
  padding: 0 14px;
  border-radius: 999px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  background: rgba(255, 255, 255, 0.06);
  color: rgba(255, 255, 255, 0.9);
}
.chip:hover {
  background: rgba(255, 255, 255, 0.1);
}
</style>
