<template>
  <section
    class="min-w-0 rounded-[24px] border border-slate-200 bg-[#fbfaf7] shadow-[0_12px_30px_rgba(15,23,42,0.055)]"
  >
    <div
      class="flex flex-wrap items-center justify-between gap-3 border-b border-slate-200 px-4 py-3 sm:px-5"
    >
      <div>
        <h2 class="text-base font-semibold leading-tight text-slate-900">Colis</h2>
        <p class="text-xs text-slate-500">{{ countLabel }}</p>
      </div>
      <div v-if="parcels.length" class="flex w-full flex-wrap items-center gap-2 sm:w-auto">
        <template v-if="selectionMode">
          <button
            type="button"
            class="inline-flex h-8 w-full items-center justify-center rounded-full border border-slate-200 bg-white px-3 text-xs font-semibold text-slate-700 transition hover:border-sky-300 hover:text-slate-900 sm:w-auto"
            @click="$emit('toggle-all-visible')"
          >
            {{ allVisibleSelected ? 'Tout deselectionner' : 'Tout selectionner' }}
          </button>
          <button
            type="button"
            class="inline-flex h-8 w-full items-center justify-center rounded-full border border-slate-200 bg-white px-3 text-xs font-semibold text-slate-700 transition hover:border-sky-300 hover:text-slate-900 sm:w-auto"
            @click="$emit('clear-selection')"
          >
            Annuler
          </button>
          <button
            type="button"
            class="inline-flex h-8 w-full items-center justify-center rounded-full border border-red-300/40 bg-red-50 px-3 text-xs font-semibold text-red-700 transition hover:border-red-400 hover:bg-red-100 disabled:cursor-wait disabled:opacity-60 sm:w-auto"
            :disabled="!selectedIds.length || deletingSelection"
            @click="$emit('delete-selected')"
          >
            {{ deletingSelection ? 'Suppression...' : `Supprimer (${selectedIds.length})` }}
          </button>
        </template>
        <button
          v-else
          type="button"
          class="inline-flex h-8 w-full items-center justify-center rounded-full border border-slate-200 bg-white px-3 text-xs font-semibold text-slate-700 transition hover:border-sky-300 hover:text-slate-900 sm:w-auto"
          @click="$emit('start-selection')"
        >
          Selectionner
        </button>
      </div>
    </div>

    <div
      v-if="error"
      class="m-4 rounded-2xl border border-red-300/40 bg-red-50 px-3 py-2.5 text-sm text-red-700"
    >
      {{ error }}
    </div>

    <div v-if="loading && !parcels.length" class="grid gap-3 p-4">
      <div
        v-for="index in 5"
        :key="index"
        class="h-20 animate-pulse rounded-2xl bg-slate-200/70"
      />
    </div>

    <div v-else-if="!parcels.length" class="p-4">
      <div class="rounded-2xl border border-dashed border-slate-300 bg-white/70 p-6 text-center">
        <PackageSearch class="mx-auto h-8 w-8 text-slate-400" />
        <p class="mt-3 text-sm font-medium text-slate-800">Aucun colis</p>
        <p class="mt-1 text-xs text-slate-500">Ajoute un suivi manuel ou lance un scan Gmail.</p>
      </div>
    </div>

    <div class="p-3 sm:p-4" v-else>
      <div class="delivery-list-scroll max-h-none overflow-visible pr-0 lg:max-h-[690px] lg:overflow-y-auto lg:pr-2">
      <div
        v-if="loading"
        class="mb-3 rounded-2xl border border-sky-200 bg-sky-50 px-3 py-3 text-sm text-sky-900"
      >
        <div class="flex items-center justify-between gap-3">
          <p class="font-medium">Rafraichissement de la liste en cours...</p>
          <span class="text-xs text-sky-700">Mise a jour transporteurs</span>
        </div>
        <div class="mt-2 h-1.5 overflow-hidden rounded-full bg-sky-100">
          <div class="delivery-inline-loader h-full rounded-full bg-sky-500" />
        </div>
      </div>
      <div class="grid gap-2.5">
        <button
          v-for="parcel in parcels"
          :key="parcel.id"
          type="button"
          class="delivery-parcel-card group w-full rounded-2xl border p-3 text-left transition"
          :class="
            parcel.id === selectedId
              ? 'delivery-parcel-card--selected'
              : 'delivery-parcel-card--idle'
          "
          @click="handleCardClick(parcel.id)"
        >
          <span v-if="parcel.id === selectedId" class="delivery-parcel-card__rail" aria-hidden="true" />
          <div class="flex flex-col gap-3 md:flex-row md:items-start md:justify-between">
            <div class="flex min-w-0 gap-3">
              <label
                v-if="selectionMode"
                class="mt-0.5 flex h-5 w-5 shrink-0 cursor-pointer items-center justify-center"
                @click.stop
              >
                <input
                  type="checkbox"
                  class="h-4 w-4 rounded border-slate-300 text-teal-700 focus:ring-teal-200"
                  :checked="selectedIds.includes(parcel.id)"
                  :disabled="deletingSelection"
                  @change="$emit('toggle-selection', parcel.id)"
                />
              </label>
              <div class="min-w-0">
              <p class="break-all text-sm font-semibold text-slate-900">{{ displayTracking(parcel) }}</p>
              <div class="mt-2 flex flex-wrap items-center gap-x-2 gap-y-1 text-xs text-slate-500">
                <span class="inline-flex items-center gap-1.5">
                  <Truck class="h-3.5 w-3.5 text-slate-400" />
                  {{ carrierLabel(parcel.carrierSlug) }}
                </span>
                <span class="rounded-full bg-slate-100 px-2 py-0.5 text-[11px] text-slate-600">
                  {{ getDeliveryStatusMeta(parcel).shortLabel }}
                </span>
                <span v-if="parcel.estimatedDeliveryAt">
                  ETA {{ formatDeliveryDate(parcel.estimatedDeliveryAt) }}
                </span>
                <span>{{
                  formatDateTime(parcel.lastEventAt || parcel.updatedAt || parcel.firstSeenAt)
                }}</span>
              </div>
              </div>
            </div>

            <div class="flex shrink-0 flex-row flex-wrap items-center gap-2 md:max-w-[11rem] md:justify-end lg:flex-col lg:items-end">
              <span
                v-if="parcel.id === selectedId"
                class="rounded-full border border-teal-300/60 bg-white px-2.5 py-1 text-[11px] font-semibold text-teal-700 shadow-sm"
              >
                Selectionne
              </span>
              <span
                class="rounded-full border px-2.5 py-1 text-[11px] font-semibold"
                :class="statusMeta(parcel).class"
              >
                {{ statusMeta(parcel).label }}
              </span>
              <span v-if="eventCount(parcel)" class="text-xs text-slate-400"
                >{{ eventCount(parcel) }} evt.</span
              >
            </div>
          </div>
        </button>
      </div>
      </div>
    </div>
  </section>
</template>

<script setup>
import { computed } from 'vue'
import { PackageSearch, Truck } from 'lucide-vue-next'
import {
  carrierLabel,
  formatDeliveryDate,
  formatDeliveryDateTime,
  getDeliveryStatusMeta,
} from '@/utils/deliveryPresentation.js'

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
  selectedIds: {
    type: Array,
    default: () => [],
  },
  selectionMode: {
    type: Boolean,
    default: false,
  },
  deletingSelection: {
    type: Boolean,
    default: false,
  },
  totalCount: {
    type: Number,
    default: 0,
  },
})

const emit = defineEmits([
  'select',
  'start-selection',
  'toggle-selection',
  'toggle-all-visible',
  'clear-selection',
  'delete-selected',
])

const countLabel = computed(() => {
  if (!props.totalCount || props.totalCount === props.parcels.length) {
    return `${props.parcels.length} colis`
  }
  return `${props.parcels.length} sur ${props.totalCount} colis`
})

const allVisibleSelected = computed(
  () =>
    props.parcels.length > 0 &&
    props.parcels.every((parcel) => props.selectedIds.includes(parcel.id)),
)

const statusMeta = (parcel) => {
  const meta = getDeliveryStatusMeta(parcel)
  return { label: meta.label, class: meta.lightBadgeClass }
}

const eventCount = (parcel) => (Array.isArray(parcel?.events) ? parcel.events.length : 0)

const formatDateTime = formatDeliveryDateTime
const displayTracking = (parcel) => parcel?.normalizedTrackingNumber || parcel?.trackingNumber || '-'

function handleCardClick(parcelId) {
  if (!parcelId) return
  if (props.selectionMode) {
    emit('toggle-selection', parcelId)
  }
  emit('select', parcelId)
}
</script>

<style scoped>
.delivery-list-scroll {
  scrollbar-width: thin;
  scrollbar-color: rgba(148, 163, 184, 0.9) rgba(226, 232, 240, 0.9);
}

.delivery-list-scroll::-webkit-scrollbar {
  width: 8px;
}

.delivery-list-scroll::-webkit-scrollbar-track {
  background: rgba(226, 232, 240, 0.9);
  border-radius: 999px;
}

.delivery-list-scroll::-webkit-scrollbar-thumb {
  border: 2px solid rgba(226, 232, 240, 0.9);
  border-radius: 999px;
  background: linear-gradient(180deg, rgba(15, 118, 110, 0.72), rgba(56, 189, 248, 0.56));
}

.delivery-parcel-card {
  position: relative;
  overflow: hidden;
}

.delivery-parcel-card--selected {
  border-color: rgba(13, 148, 136, 0.45);
  background: linear-gradient(145deg, rgba(240, 253, 250, 0.98), rgba(255, 255, 255, 0.98));
  box-shadow:
    0 16px 34px rgba(15, 118, 110, 0.14),
    0 0 0 1px rgba(20, 184, 166, 0.1);
}

.delivery-parcel-card--selected:hover {
  border-color: rgba(13, 148, 136, 0.56);
}

.delivery-parcel-card--idle {
  border-color: rgba(226, 232, 240, 1);
  background: #ffffff;
}

.delivery-parcel-card--idle:hover {
  border-color: rgba(125, 211, 252, 0.82);
  background: rgba(248, 250, 252, 0.96);
}

.delivery-parcel-card__rail {
  position: absolute;
  left: 0;
  top: 12px;
  bottom: 12px;
  width: 5px;
  border-radius: 999px;
  background: linear-gradient(180deg, #14b8a6, #0ea5e9);
}

.delivery-inline-loader {
  width: 38%;
  animation: delivery-inline-progress 1.25s ease-in-out infinite;
}

@keyframes delivery-inline-progress {
  0% {
    transform: translateX(-110%);
  }

  50% {
    transform: translateX(95%);
  }

  100% {
    transform: translateX(240%);
  }
}
</style>
