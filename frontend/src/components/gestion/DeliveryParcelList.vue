<template>
  <section
    class="min-w-0 rounded-[22px] border border-slate-700/70 bg-slate-900/70 shadow-xl shadow-slate-950/20 backdrop-blur"
  >
    <div
      class="flex flex-wrap items-center justify-between gap-3 border-b border-slate-700/70 px-4 py-3 sm:px-5"
    >
      <div>
        <h2 class="text-base font-semibold leading-tight text-slate-100">Colis</h2>
        <p class="text-xs text-slate-400">{{ countLabel }}</p>
      </div>
    </div>

    <div
      v-if="error"
      class="m-4 rounded-2xl border border-red-400/30 bg-red-500/10 px-3 py-2.5 text-sm text-red-100"
    >
      {{ error }}
    </div>

    <div v-if="loading" class="grid gap-3 p-4">
      <div v-for="index in 5" :key="index" class="h-20 animate-pulse rounded-2xl bg-slate-800/55" />
    </div>

    <div v-else-if="!parcels.length" class="p-4">
      <div class="rounded-2xl border border-dashed border-slate-700/80 p-6 text-center">
        <PackageSearch class="mx-auto h-8 w-8 text-slate-500" />
        <p class="mt-3 text-sm font-medium text-slate-200">Aucun colis</p>
      </div>
    </div>

    <div v-else class="delivery-list-scroll max-h-[690px] overflow-y-auto p-3 pr-2 sm:p-4 sm:pr-2">
      <div class="grid gap-2.5">
        <button
          v-for="parcel in parcels"
          :key="parcel.id"
          type="button"
          class="group w-full rounded-2xl border p-3 text-left transition"
          :class="
            parcel.id === selectedId
              ? 'border-violet-400/60 bg-violet-500/10 shadow-lg shadow-violet-950/20'
              : 'border-slate-700/70 bg-slate-950/35 hover:border-slate-500/80 hover:bg-slate-900/85'
          "
          @click="$emit('select', parcel.id)"
        >
          <div class="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
            <div class="min-w-0">
              <p class="break-all text-sm font-semibold text-white">{{ parcel.trackingNumber }}</p>
              <div class="mt-2 flex flex-wrap items-center gap-x-2 gap-y-1 text-xs text-slate-400">
                <span class="inline-flex items-center gap-1.5">
                  <Truck class="h-3.5 w-3.5 text-slate-500" />
                  {{ carrierLabel(parcel.carrierSlug) }}
                </span>
                <span>{{
                  formatDateTime(parcel.lastEventAt || parcel.updatedAt || parcel.firstSeenAt)
                }}</span>
              </div>
              <p
                v-if="parcel.statusLabel"
                class="mt-1 max-w-[520px] truncate text-xs text-slate-500"
              >
                {{ parcel.statusLabel }}
              </p>
              <p
                v-if="parcel.destinationAddress"
                class="mt-1 max-w-[520px] truncate text-xs text-slate-400"
              >
                {{ parcel.destinationAddress }}
              </p>
            </div>

            <div class="flex shrink-0 flex-row flex-wrap items-center gap-2 sm:flex-col sm:items-end">
              <span
                class="rounded-full border px-2.5 py-1 text-[11px] font-semibold"
                :class="statusMeta(parcel.status).class"
              >
                {{ statusMeta(parcel.status).label }}
              </span>
              <span v-if="eventCount(parcel)" class="text-xs text-slate-500"
                >{{ eventCount(parcel) }} evt.</span
              >
            </div>
          </div>
        </button>
      </div>
    </div>
  </section>
</template>

<script setup>
import { computed } from 'vue'
import { PackageSearch, Truck } from 'lucide-vue-next'

const props = defineProps({
  parcels: {
    type: Array,
    default: () => [],
  },
  loading: {
    type: Boolean,
    default: false,
  },
  error: {
    type: String,
    default: '',
  },
  selectedId: {
    type: [Number, String],
    default: null,
  },
  totalCount: {
    type: Number,
    default: 0,
  },
})

defineEmits(['select'])

const countLabel = computed(() => {
  if (!props.totalCount || props.totalCount === props.parcels.length) {
    return `${props.parcels.length} colis`
  }
  return `${props.parcels.length} sur ${props.totalCount} colis`
})

const statusMeta = (status) => {
  switch (status) {
    case 'DELIVERED':
      return { label: 'Livre', class: 'border-emerald-400/50 bg-emerald-500/10 text-emerald-200' }
    case 'OUT_FOR_DELIVERY':
      return { label: 'En livraison', class: 'border-sky-400/50 bg-sky-500/10 text-sky-200' }
    case 'IN_TRANSIT':
      return { label: 'En transit', class: 'border-violet-400/50 bg-violet-500/10 text-violet-100' }
    case 'EXCEPTION':
      return { label: 'Incident', class: 'border-red-400/50 bg-red-500/10 text-red-200' }
    case 'REGISTERED':
    case 'PENDING':
      return { label: 'En attente', class: 'border-amber-400/50 bg-amber-500/10 text-amber-100' }
    default:
      return { label: 'Inconnu', class: 'border-slate-600 bg-slate-900 text-slate-300' }
  }
}

const carrierLabel = (carrier) => {
  if (!carrier || carrier === 'unknown') return 'Transporteur'
  return String(carrier)
    .split('-')
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(' ')
}

const eventCount = (parcel) => (Array.isArray(parcel?.events) ? parcel.events.length : 0)

const formatDateTime = (value) => {
  if (!value) return 'Date inconnue'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return 'Date inconnue'
  return new Intl.DateTimeFormat('fr-FR', {
    dateStyle: 'short',
    timeStyle: 'short',
  }).format(date)
}
</script>

<style scoped>
.delivery-list-scroll {
  scrollbar-width: thin;
  scrollbar-color: rgba(139, 92, 246, 0.55) rgba(15, 23, 42, 0.35);
}

.delivery-list-scroll::-webkit-scrollbar {
  width: 8px;
}

.delivery-list-scroll::-webkit-scrollbar-track {
  background: rgba(15, 23, 42, 0.35);
  border-radius: 999px;
}

.delivery-list-scroll::-webkit-scrollbar-thumb {
  border: 2px solid rgba(15, 23, 42, 0.35);
  border-radius: 999px;
  background: linear-gradient(180deg, rgba(167, 139, 250, 0.78), rgba(14, 165, 233, 0.62));
}
</style>
