<template>
  <aside
    class="min-w-0 rounded-[24px] border border-slate-200 bg-[#fbfaf7] p-4 shadow-[0_12px_30px_rgba(15,23,42,0.055)] sm:p-5"
  >
    <div class="delivery-timeline-head">
      <div class="delivery-timeline-copy">
        <h3 class="text-base font-semibold text-slate-900">Detail livraison</h3>
        <p class="delivery-timeline-number">
          {{ parcel ? parcel.normalizedTrackingNumber || parcel.trackingNumber : 'Aucun colis' }}
        </p>
      </div>
      <div v-if="parcel" class="delivery-timeline-actions">
        <span
          class="delivery-timeline-status inline-flex rounded-full border px-2.5 py-1 text-[11px] font-semibold"
          :class="statusMeta(parcel).class"
        >
          {{ statusMeta(parcel).label }}
        </span>
        <div class="delivery-timeline-buttons">
          <a
            v-if="parcel.trackingUrl"
            :href="parcel.trackingUrl"
            target="_blank"
            rel="noreferrer"
            class="delivery-timeline-link inline-flex h-9 items-center justify-center gap-2 rounded-full border border-teal-600/20 bg-teal-700 px-3 text-xs font-semibold text-white transition hover:bg-teal-600"
          >
            <ExternalLink class="h-3.5 w-3.5" />
            <span>Transporteur</span>
          </a>
          <button
            type="button"
            class="delivery-timeline-delete inline-flex h-9 w-9 items-center justify-center rounded-full border border-red-300/40 bg-red-50 text-red-700 transition hover:border-red-400 hover:bg-red-100 disabled:cursor-wait disabled:opacity-60"
            title="Supprimer ce suivi"
            :disabled="refreshing || deleting"
            @click="$emit('delete', parcel.id)"
          >
            <RefreshCw v-if="deleting" class="h-4 w-4 animate-spin" />
            <Trash2 v-else class="h-4 w-4" />
          </button>
        </div>
      </div>
    </div>

    <div
      v-if="parcel && refreshing"
      class="mt-3 rounded-2xl border border-sky-200 bg-sky-50 px-3 py-3"
    >
      <div class="flex items-center justify-between gap-3">
        <p class="text-sm font-medium text-sky-900">Rafraichissement du suivi...</p>
        <span class="text-xs text-sky-700">Interrogation transporteur</span>
      </div>
      <div class="mt-2 h-1.5 overflow-hidden rounded-full bg-sky-100">
        <div class="delivery-inline-loader h-full rounded-full bg-sky-500" />
      </div>
    </div>

    <div
      v-if="parcel && trackingHealth && trackingHealth.tone === 'warning'"
      class="mt-3 rounded-2xl border px-3 py-3"
      :class="
        trackingHealth.tone === 'warning'
          ? 'border-amber-300/50 bg-amber-50'
          : 'border-sky-200/70 bg-sky-50/70'
      "
    >
      <p
        class="text-sm font-semibold"
        :class="trackingHealth.tone === 'warning' ? 'text-amber-900' : 'text-sky-900'"
      >
        {{ trackingHealth.title }}
      </p>
      <p
        class="mt-1 text-xs leading-relaxed"
        :class="trackingHealth.tone === 'warning' ? 'text-amber-800' : 'text-sky-800'"
      >
        {{ trackingHealth.message }}
      </p>

    </div>

    <div v-if="parcel" class="mt-3 rounded-2xl border border-slate-200 bg-white p-3">
      <div class="flex flex-wrap items-center justify-between gap-2">
        <p class="text-xs font-semibold uppercase tracking-[0.18em] text-slate-500">Avancement transporteur</p>
        <span class="text-xs text-slate-500">{{ progressHeadline }}</span>
      </div>

      <div class="mt-3 h-2 overflow-hidden rounded-full bg-slate-200">
        <div
          class="h-full rounded-full bg-teal-600 transition-all duration-300"
          :style="{ width: `${progressPercent}%` }"
        />
      </div>

      <div class="mt-3 flex flex-wrap gap-2">
        <span
          v-for="step in progressSteps"
          :key="step.key"
          class="rounded-full border px-2.5 py-1 text-[11px] font-semibold"
          :class="
            step.state === 'done'
              ? 'border-emerald-300/60 bg-emerald-50 text-emerald-700'
              : step.state === 'current'
                ? 'border-sky-300/60 bg-sky-50 text-sky-700'
                : 'border-slate-200 bg-slate-50 text-slate-500'
          "
        >
          {{ step.label }}
        </span>
      </div>
    </div>

    <div v-if="parcel && detailItems.length" class="mt-3 grid gap-2 sm:grid-cols-2 xl:grid-cols-1 2xl:grid-cols-2">
      <div
        v-for="item in detailItems"
        :key="item.label"
        class="rounded-2xl border border-slate-200 bg-white p-3"
      >
        <p class="text-xs text-slate-500">{{ item.label }}</p>
        <p class="mt-1 break-words text-sm font-semibold text-slate-800">{{ item.value }}</p>
      </div>
    </div>

    <div
      v-if="!parcel"
      class="mt-8 rounded-2xl border border-dashed border-slate-300 bg-white/70 p-6 text-center text-sm text-slate-500"
    >
      Aucun colis selectionne.
    </div>

    <div v-else-if="events.length" class="mt-5">
      <p class="mb-3 text-xs font-semibold uppercase tracking-[0.18em] text-slate-500">
        Evenements transporteur
      </p>
      <ol class="delivery-timeline-scroll max-h-none space-y-3 overflow-visible pr-0 lg:max-h-[520px] lg:overflow-y-auto lg:pr-1">
        <li
          v-for="event in events"
          :key="event.id || `${event.eventTime}-${event.description}`"
          class="relative pl-6"
        >
          <span
            class="absolute left-0 top-1.5 h-2.5 w-2.5 rounded-full border border-teal-200 bg-teal-500 shadow shadow-teal-200/60"
          />
          <span class="absolute bottom-[-1rem] left-[4px] top-5 w-px bg-slate-200 last:hidden" />
          <div class="rounded-2xl border border-slate-200 bg-white p-3">
            <div class="flex flex-wrap items-center justify-between gap-2">
              <p class="text-sm font-semibold text-slate-800">
                {{ event.description || statusMeta(event).label }}
              </p>
              <span class="text-xs text-slate-500">{{ formatDateTime(event.eventTime) }}</span>
            </div>
            <p v-if="event.location" class="mt-1 text-xs text-slate-500">{{ event.location }}</p>
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
      class="mt-5 rounded-2xl border border-dashed border-slate-300 bg-white/70 p-5 text-center text-sm text-slate-500"
    >
      Aucun evenement transporteur pour l'instant.
    </div>
  </aside>
</template>

<script setup>
import { computed } from 'vue'
import { ExternalLink, RefreshCw, Trash2 } from 'lucide-vue-next'
import {
  carrierLabel,
  formatDeliveryDateTime,
  getDeliveryTrackingHealth,
  getDeliveryStatusMeta,
} from '@/utils/deliveryPresentation.js'

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
const trackingHealth = computed(() => getDeliveryTrackingHealth(props.parcel))

const progressOrder = ['PENDING', 'REGISTERED', 'IN_TRANSIT', 'DELIVERED']

const currentProgressIndex = computed(() => {
  const status = props.parcel?.status
  if (status === 'EXCEPTION') {
    return 2
  }
  const normalizedStatus = status === 'OUT_FOR_DELIVERY' ? 'IN_TRANSIT' : status
  const index = progressOrder.indexOf(normalizedStatus)
  return index >= 0 ? index : 0
})

const progressSteps = computed(() => [
  {
    key: 'detected',
    label: 'Detection',
  },
  {
    key: 'registered',
    label: 'Bordereau',
  },
  {
    key: 'transit',
    label: props.parcel?.status === 'EXCEPTION' ? 'Blocage' : 'Transit',
  },
  {
    key: 'delivered',
    label: 'Reception',
  },
].map((step, index) => ({
  ...step,
  state:
    index < currentProgressIndex.value
      ? 'done'
      : index === currentProgressIndex.value
        ? 'current'
        : 'upcoming',
})))

const progressPercent = computed(() => {
  if (!props.parcel) return 0
  return Math.max(20, ((currentProgressIndex.value + 1) / progressSteps.value.length) * 100)
})

const progressHeadline = computed(() => {
  if (!props.parcel) return ''
  if (props.parcel.status === 'EXCEPTION') {
    return 'Incident detecte sur le parcours'
  }
  return getDeliveryStatusMeta(props.parcel).label
})

const detailItems = computed(() => {
  const parcel = props.parcel
  if (!parcel) return []

  const lastUpdate = parcel.lastEventAt || parcel.updatedAt || parcel.firstSeenAt
  return [
    {
      label: 'Statut detaille',
      value: parcel.statusLabel,
      show: Boolean(parcel.statusLabel),
    },
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
  ].filter((item) => item.show && item.value)
})

const statusMeta = (input) => {
  const meta = getDeliveryStatusMeta(input)
  return { label: meta.label, class: meta.lightBadgeClass }
}

const formatDateTime = formatDeliveryDateTime
</script>

<style scoped>
.delivery-timeline-head {
  display: grid;
  gap: 0.85rem;
  min-width: 0;
}

.delivery-timeline-copy {
  min-width: 0;
}

.delivery-timeline-number {
  margin-top: 0.25rem;
  color: #64748b;
  font-size: 0.75rem;
  line-height: 1.35;
  overflow-wrap: anywhere;
}

.delivery-timeline-actions {
  display: grid;
  gap: 0.65rem;
  min-width: 0;
}

.delivery-timeline-status {
  width: fit-content;
  max-width: 100%;
}

.delivery-timeline-buttons {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  min-width: 0;
}

.delivery-timeline-link {
  min-width: 0;
  max-width: 100%;
  padding-inline: 0.85rem;
}

.delivery-timeline-link span {
  overflow-wrap: anywhere;
}

.delivery-timeline-delete {
  flex: 0 0 auto;
}

.delivery-timeline-scroll {
  scrollbar-width: thin;
  scrollbar-color: rgba(148, 163, 184, 0.9) rgba(226, 232, 240, 0.9);
}

.delivery-timeline-scroll::-webkit-scrollbar {
  width: 8px;
}

.delivery-timeline-scroll::-webkit-scrollbar-track {
  background: rgba(226, 232, 240, 0.9);
  border-radius: 999px;
}

.delivery-timeline-scroll::-webkit-scrollbar-thumb {
  border: 2px solid rgba(226, 232, 240, 0.9);
  border-radius: 999px;
  background: linear-gradient(180deg, rgba(15, 118, 110, 0.72), rgba(56, 189, 248, 0.56));
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

@media (min-width: 560px) {
  .delivery-timeline-head {
    grid-template-columns: minmax(0, 1fr) auto;
    align-items: start;
  }

  .delivery-timeline-actions {
    justify-items: end;
  }

  .delivery-timeline-buttons {
    justify-content: flex-end;
  }
}
</style>
