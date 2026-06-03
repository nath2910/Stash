<template>
  <section
    v-if="loading || error || candidates.length"
    class="min-w-0 rounded-[22px] border border-amber-300/25 bg-amber-500/10 p-4 shadow-xl shadow-slate-950/15 backdrop-blur sm:p-5"
  >
    <div class="flex flex-wrap items-center justify-between gap-3">
      <div>
        <h2 class="text-base font-semibold text-amber-50">Candidats a verifier</h2>
        <p class="mt-1 text-xs text-amber-100/70">
          {{ candidates.length }} numero(s) avec confiance moyenne
        </p>
      </div>
      <AlertTriangle class="h-5 w-5 text-amber-200" />
    </div>

    <div
      v-if="error"
      class="mt-4 rounded-2xl border border-red-400/30 bg-red-500/10 px-3 py-2.5 text-sm text-red-100"
    >
      {{ error }}
    </div>

    <div v-if="loading" class="mt-4 grid gap-2">
      <div v-for="index in 2" :key="index" class="h-24 animate-pulse rounded-2xl bg-slate-900/35" />
    </div>

    <div v-else class="mt-4 grid gap-3">
      <article
        v-for="candidate in candidates"
        :key="candidate.id"
        class="rounded-2xl border border-amber-200/20 bg-slate-950/35 p-3"
      >
        <div class="flex flex-col gap-3 lg:flex-row lg:items-start lg:justify-between">
          <div class="min-w-0 flex-1">
            <div class="flex flex-wrap items-center gap-2">
              <p class="break-all text-sm font-semibold text-white">
                {{ candidate.trackingNumber }}
              </p>
              <span
                class="rounded-full border border-amber-200/35 bg-amber-300/10 px-2 py-0.5 text-[11px] font-semibold text-amber-100"
              >
                Score {{ candidate.confidenceScore }}
              </span>
            </div>

            <div class="mt-2 flex flex-wrap items-center gap-x-3 gap-y-1 text-xs text-amber-100/70">
              <span class="inline-flex items-center gap-1.5">
                <Truck class="h-3.5 w-3.5" />
                {{ carrierLabel(candidate.carrierSlug) }}
              </span>
              <span v-if="candidate.merchantName || candidate.sourceSender">
                {{ candidate.merchantName || candidate.sourceSender }}
              </span>
              <span>{{ formatDateTime(candidate.receivedAt || candidate.createdAt) }}</span>
            </div>

            <p
              v-if="candidate.contextSnippet"
              class="mt-2 line-clamp-2 text-xs leading-relaxed text-slate-300"
            >
              {{ candidate.contextSnippet }}
            </p>
            <p v-if="candidate.reason" class="mt-2 text-xs text-amber-100/60">
              {{ candidate.reason }}
            </p>
          </div>

          <div class="flex shrink-0 flex-wrap items-center gap-2 lg:justify-end">
            <a
              v-if="candidate.trackingUrl"
              :href="candidate.trackingUrl"
              target="_blank"
              rel="noreferrer"
              class="inline-flex h-9 items-center justify-center gap-2 rounded-full border border-slate-600/80 bg-slate-900/75 px-3 text-xs font-semibold text-slate-100 transition hover:border-amber-200/60 hover:text-white"
            >
              <ExternalLink class="h-3.5 w-3.5" />
              <span>Ouvrir</span>
            </a>
            <button
              type="button"
              class="inline-flex h-9 items-center justify-center gap-2 rounded-full border border-emerald-300/45 bg-emerald-500/10 px-3 text-xs font-semibold text-emerald-100 transition hover:border-emerald-200/80 hover:bg-emerald-500/20 disabled:cursor-wait disabled:opacity-60"
              :disabled="confirmingId === candidate.id || ignoringId === candidate.id"
              @click="$emit('confirm', candidate.id)"
            >
              <Check class="h-3.5 w-3.5" />
              <span>{{ confirmingId === candidate.id ? 'Validation...' : 'Valider' }}</span>
            </button>
            <button
              type="button"
              class="inline-flex h-9 items-center justify-center gap-2 rounded-full border border-slate-600/80 bg-slate-900/75 px-3 text-xs font-semibold text-slate-200 transition hover:border-red-300/60 hover:text-red-100 disabled:cursor-wait disabled:opacity-60"
              :disabled="confirmingId === candidate.id || ignoringId === candidate.id"
              @click="$emit('ignore', candidate.id)"
            >
              <X class="h-3.5 w-3.5" />
              <span>{{ ignoringId === candidate.id ? 'Ignore...' : 'Ignorer' }}</span>
            </button>
          </div>
        </div>
      </article>
    </div>
  </section>
</template>

<script setup>
import { AlertTriangle, Check, ExternalLink, Truck, X } from 'lucide-vue-next'

defineProps({
  candidates: {
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
  confirmingId: {
    type: [Number, String],
    default: null,
  },
  ignoringId: {
    type: [Number, String],
    default: null,
  },
})

defineEmits(['confirm', 'ignore'])

const carrierLabel = (carrier) => {
  if (!carrier || carrier === 'unknown') return 'Transporteur a confirmer'
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
