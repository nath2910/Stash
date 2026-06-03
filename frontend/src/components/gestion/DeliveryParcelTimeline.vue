<template>
  <aside
    class="min-w-0 rounded-[22px] border border-slate-700/70 bg-slate-900/70 p-4 shadow-xl shadow-slate-950/20 backdrop-blur sm:p-5"
  >
    <div class="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
      <div class="min-w-0">
        <h3 class="text-base font-semibold text-white">Detail livraison</h3>
        <p class="mt-1 truncate text-xs text-slate-400">
          {{ parcel ? parcel.trackingNumber : 'Aucun colis' }}
        </p>
      </div>
      <div v-if="parcel" class="flex shrink-0 flex-wrap items-center gap-2 sm:justify-end">
        <a
          v-if="parcel.trackingUrl"
          :href="parcel.trackingUrl"
          target="_blank"
          rel="noreferrer"
          class="inline-flex h-9 items-center justify-center gap-2 rounded-full border border-violet-400/50 bg-violet-500/10 px-3 text-xs font-semibold text-violet-100 transition hover:border-violet-300/70 hover:bg-violet-500/20"
        >
          <ExternalLink class="h-3.5 w-3.5" />
          <span>Transporteur</span>
        </a>
        <button
          type="button"
          class="inline-flex h-9 w-9 items-center justify-center rounded-full border border-slate-700/80 bg-slate-900/75 text-slate-200 transition hover:border-violet-400/60 hover:text-white disabled:cursor-wait disabled:opacity-60"
          title="Mettre a jour le suivi"
          :disabled="refreshing || deleting"
          @click="$emit('refresh', parcel.id)"
        >
          <RefreshCw class="h-4 w-4" :class="{ 'animate-spin': refreshing }" />
        </button>
        <button
          type="button"
          class="inline-flex h-9 w-9 items-center justify-center rounded-full border border-red-500/30 bg-red-500/10 text-red-200 transition hover:border-red-400/80 hover:bg-red-500/20 disabled:cursor-wait disabled:opacity-60"
          title="Supprimer ce suivi"
          :disabled="refreshing || deleting"
          @click="$emit('delete', parcel.id)"
        >
          <RefreshCw v-if="deleting" class="h-4 w-4 animate-spin" />
          <Trash2 v-else class="h-4 w-4" />
        </button>
      </div>
    </div>

    <div v-if="parcel" class="mt-4 rounded-2xl border border-slate-700/70 bg-slate-950/35 p-3">
      <div class="flex flex-wrap items-center justify-between gap-2">
        <span class="text-xs text-slate-400">Statut</span>
        <span
          class="inline-flex rounded-full border px-2.5 py-1 text-[11px] font-semibold"
          :class="statusMeta(parcel.status).class"
        >
          {{ statusMeta(parcel.status).label }}
        </span>
      </div>
      <p v-if="parcel.statusLabel" class="mt-2 text-sm font-medium text-slate-100">
        {{ parcel.statusLabel }}
      </p>
    </div>

    <div v-if="parcel && detailItems.length" class="mt-3 grid gap-2 sm:grid-cols-2 2xl:grid-cols-1">
      <div
        v-for="item in detailItems"
        :key="item.label"
        class="rounded-2xl border border-slate-700/60 bg-slate-950/30 p-3"
      >
        <p class="text-xs text-slate-500">{{ item.label }}</p>
        <p class="mt-1 break-words text-sm font-semibold text-slate-100">{{ item.value }}</p>
      </div>
    </div>

    <div
      v-if="!parcel"
      class="mt-8 rounded-2xl border border-dashed border-slate-700/80 p-6 text-center text-sm text-slate-400"
    >
      Aucun colis selectionne.
    </div>

    <div v-else-if="events.length" class="mt-5">
      <p class="mb-3 text-xs font-semibold uppercase tracking-[0.18em] text-slate-500">
        Historique
      </p>
      <ol class="delivery-timeline-scroll max-h-[520px] space-y-3 overflow-y-auto pr-1">
        <li
          v-for="event in events"
          :key="event.id || `${event.eventTime}-${event.description}`"
          class="relative pl-6"
        >
          <span
            class="absolute left-0 top-1.5 h-2.5 w-2.5 rounded-full border border-violet-300 bg-violet-500 shadow shadow-violet-950/40"
          />
          <span class="absolute bottom-[-1rem] left-[4px] top-5 w-px bg-slate-800 last:hidden" />
          <div class="rounded-2xl border border-slate-700/70 bg-slate-950/35 p-3">
            <div class="flex flex-wrap items-center justify-between gap-2">
              <p class="text-sm font-semibold text-slate-100">
                {{ event.description || statusMeta(event.status).label }}
              </p>
              <span class="text-xs text-slate-500">{{ formatDateTime(event.eventTime) }}</span>
            </div>
            <p v-if="event.location" class="mt-1 text-xs text-slate-400">{{ event.location }}</p>
            <p
              v-if="event.substatus"
              class="mt-2 text-[11px] uppercase tracking-[0.16em] text-slate-500"
            >
              {{ event.substatus }}
            </p>
          </div>
        </li>
      </ol>
    </div>

    <div
      v-else
      class="mt-5 rounded-2xl border border-dashed border-slate-700/80 p-5 text-center text-sm text-slate-400"
    >
      Aucun evenement local.
    </div>
  </aside>
</template>

<script setup>
import { computed } from 'vue'
import { ExternalLink, RefreshCw, Trash2 } from 'lucide-vue-next'

const props = defineProps({
  parcel: {
    type: Object,
    default: null,
  },
  refreshing: {
    type: Boolean,
    default: false,
  },
  deleting: {
    type: Boolean,
    default: false,
  },
})

defineEmits(['refresh', 'delete'])

const events = computed(() => (Array.isArray(props.parcel?.events) ? props.parcel.events : []))

const detailItems = computed(() => {
  const parcel = props.parcel
  if (!parcel) return []

  const lastUpdate = parcel.lastEventAt || parcel.updatedAt || parcel.firstSeenAt
  return [
    {
      label: 'Transporteur',
      value: carrierLabel(parcel.carrierSlug),
      show: parcel.carrierSlug && parcel.carrierSlug !== 'unknown',
    },
    {
      label: 'Derniere maj',
      value: formatDateTime(lastUpdate),
      show: Boolean(lastUpdate),
    },
    {
      label: 'Livraison estimee',
      value: formatDateTime(parcel.estimatedDeliveryAt),
      show: Boolean(parcel.estimatedDeliveryAt),
    },
    {
      label: 'Destination',
      value: parcel.destinationAddress,
      show: Boolean(parcel.destinationAddress),
    },
    {
      label: 'Origine',
      value: parcel.originAddress,
      show: Boolean(parcel.originAddress),
    },
    {
      label: 'Service',
      value: parcel.shipmentType,
      show: Boolean(parcel.shipmentType),
    },
    {
      label: 'Reception',
      value: parcel.signedBy,
      show: Boolean(parcel.signedBy),
    },
  ].filter((item) => item.show && item.value)
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
.delivery-timeline-scroll {
  scrollbar-width: thin;
  scrollbar-color: rgba(139, 92, 246, 0.55) rgba(15, 23, 42, 0.35);
}

.delivery-timeline-scroll::-webkit-scrollbar {
  width: 8px;
}

.delivery-timeline-scroll::-webkit-scrollbar-track {
  background: rgba(15, 23, 42, 0.35);
  border-radius: 999px;
}

.delivery-timeline-scroll::-webkit-scrollbar-thumb {
  border: 2px solid rgba(15, 23, 42, 0.35);
  border-radius: 999px;
  background: linear-gradient(180deg, rgba(167, 139, 250, 0.78), rgba(14, 165, 233, 0.62));
}
</style>
