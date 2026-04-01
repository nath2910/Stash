<template>
  <aside class="w-full max-w-xl mx-auto lg:max-w-sm space-y-4">
    <div class="rounded-3xl border border-slate-800/70 bg-slate-900/80 p-5 shadow-[0_24px_60px_rgba(0,0,0,0.45)] backdrop-blur space-y-3">
      <div>
        <p class="text-xs uppercase tracking-[0.25em] text-amber-200/80">Resume du mois</p>
        <p class="mt-2 text-xs text-slate-400">Apercu rapide des performances du mois.</p>
      </div>

      <div class="grid grid-cols-2 gap-3 text-center text-xs">
        <StatBadge label="Benefice du mois" :tone="beneficeTone" :value="formattedBenefice" />
        <StatBadge label="C.A. du mois" tone="default" :value="formattedCA" />
        <StatBadge label="Items vendus (mois)" tone="accent" :value="loading ? '...' : nbVendues" />
        <StatBadge label="Items en stock" tone="default" :value="loading ? '...' : nbEnStock" />
      </div>
    </div>

    <div class="rounded-3xl border border-slate-800/70 bg-slate-900/80 p-5 shadow-[0_24px_60px_rgba(0,0,0,0.45)] backdrop-blur space-y-3">
      <div>
        <p class="text-xs uppercase tracking-[0.25em] text-amber-200/80">Actions rapides</p>
        <p class="mt-2 text-xs text-slate-400">Raccourcis vers les sections cles.</p>
      </div>

      <div class="flex flex-col gap-2 pt-1">
        <button
          class="w-full rounded-xl border border-emerald-300/40 bg-emerald-300/10 px-4 py-3 text-sm font-medium text-emerald-100 transition hover:bg-emerald-300/20"
          @click="$emit('go-gestion')"
        >
          Gerer les ventes
        </button>
        <button
          class="w-full rounded-xl border border-slate-700 bg-slate-900/70 px-4 py-3 text-sm font-medium text-slate-100 transition hover:bg-slate-800/80"
          @click="$emit('go-stats')"
        >
          Voir les stats detaillees
        </button>
      </div>
    </div>
  </aside>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import StatBadge from '@/components/StatBadge.vue'
import { formatEUR } from '@/utils/formatters'

interface Props {
  totalBenefice: number
  totalCA: number
  nbVendues: number
  nbEnStock: number
  loading?: boolean
}

const props = defineProps<Props>()

defineEmits<{
  (e: 'go-gestion'): void
  (e: 'go-stats'): void
}>()

const beneficeTone = computed(() =>
  props.totalBenefice >= 0 ? ('success' as const) : ('danger' as const),
)

const formattedBenefice = computed(() =>
  props.loading ? '...' : formatEUR(props.totalBenefice),
)

const formattedCA = computed(() => (props.loading ? '...' : formatEUR(props.totalCA)))
</script>
