<template>
  <section
    class="rounded-[22px] border border-slate-700/70 bg-slate-900/70 p-4 shadow-xl shadow-slate-950/20 backdrop-blur"
  >
    <div class="flex items-center justify-between gap-3">
      <div>
        <h2 class="text-base font-semibold text-white">Suivi manuel</h2>
        <p class="mt-1 text-xs text-slate-400">Numero de suivi</p>
      </div>
      <span
        class="inline-flex h-9 w-9 items-center justify-center rounded-full border border-violet-400/30 bg-violet-500/10 text-violet-200"
      >
        <PackagePlus class="h-4 w-4" />
      </span>
    </div>

    <form class="mt-4 grid gap-3" @submit.prevent="submit">
      <label class="grid gap-2">
        <span class="text-xs font-semibold uppercase tracking-[0.16em] text-slate-500"
          >Tracking</span
        >
        <input
          v-model.trim="trackingNumber"
          type="text"
          autocomplete="off"
          spellcheck="false"
          class="h-10 w-full rounded-full border border-slate-700/80 bg-slate-950/45 px-4 text-sm uppercase text-slate-100 outline-none transition placeholder:text-slate-600 focus:border-violet-400/70 focus:ring-2 focus:ring-violet-500/20"
          placeholder="1Z999AA10123456784"
        />
      </label>

      <label class="grid gap-2">
        <span class="text-xs font-semibold uppercase tracking-[0.16em] text-slate-500"
          >Transporteur</span
        >
        <select
          v-model="carrierSlug"
          class="h-10 w-full rounded-full border border-slate-700/80 bg-slate-950/45 px-4 text-sm text-slate-100 outline-none transition focus:border-violet-400/70 focus:ring-2 focus:ring-violet-500/20"
        >
          <option value="">Detection auto</option>
          <option value="colissimo">Colissimo</option>
          <option value="chronopost">Chronopost</option>
          <option value="mondial-relay">Mondial Relay</option>
          <option value="ups">UPS</option>
          <option value="fedex">FedEx</option>
          <option value="dhl">DHL</option>
        </select>
      </label>

      <button
        type="submit"
        class="inline-flex h-10 items-center justify-center gap-2 rounded-full border border-violet-400/50 bg-violet-500/10 px-4 text-sm font-semibold text-violet-100 transition hover:border-violet-300/70 hover:bg-violet-500/20 disabled:cursor-wait disabled:opacity-60"
        :disabled="loading || !trackingNumber"
      >
        <RefreshCw v-if="loading" class="h-4 w-4 animate-spin" />
        <PackagePlus v-else class="h-4 w-4" />
        <span>{{ loading ? 'Recherche...' : 'Ajouter le suivi' }}</span>
      </button>
    </form>

    <div
      v-if="error"
      class="mt-4 rounded-2xl border border-red-400/30 bg-red-500/10 px-3 py-2.5 text-sm text-red-100"
    >
      {{ error }}
    </div>
  </section>
</template>

<script setup>
import { ref } from 'vue'
import { PackagePlus, RefreshCw } from 'lucide-vue-next'

defineProps({
  loading: {
    type: Boolean,
    default: false,
  },
  error: {
    type: String,
    default: '',
  },
})

const emit = defineEmits(['create-parcel'])

const trackingNumber = ref('')
const carrierSlug = ref('')

const submit = () => {
  if (!trackingNumber.value) return
  emit('create-parcel', {
    trackingNumber: trackingNumber.value,
    carrierSlug: carrierSlug.value || null,
  })
}
</script>
