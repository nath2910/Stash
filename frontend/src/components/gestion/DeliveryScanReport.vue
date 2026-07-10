<template>
  <section
    v-if="loading || hasReport"
    class="min-w-0 rounded-[24px] border border-slate-200 bg-[#fbfaf7] p-4 shadow-[0_12px_30px_rgba(15,23,42,0.055)]"
  >
    <div class="flex flex-wrap items-center justify-between gap-3">
      <div>
        <h2 class="text-sm font-semibold text-slate-900">Dernier scan</h2>
        <p class="mt-1 text-xs text-slate-500">
          {{ headerLabel }}
        </p>
      </div>
      <span class="inline-flex items-center gap-2 rounded-full border border-slate-200 bg-white px-3 py-1 text-[11px] font-semibold text-slate-700">
        <Mail class="h-3.5 w-3.5" />
        {{ report?.scannedAccounts || 0 }} compte(s)
      </span>
    </div>

    <div v-if="loading" class="mt-4 rounded-2xl border border-sky-200 bg-sky-50 px-4 py-3">
      <div class="flex items-center justify-between gap-3">
        <p class="text-sm font-medium text-sky-900">Analyse Gmail en cours...</p>
        <span class="text-xs text-sky-700">Import et verification</span>
      </div>
      <div class="mt-2 h-1.5 overflow-hidden rounded-full bg-sky-100">
        <div class="delivery-inline-loader h-full rounded-full bg-sky-500" />
      </div>
    </div>

    <template v-if="report">
      <div class="mt-4 flex flex-wrap gap-2">
        <article
          v-for="metric in metrics"
          :key="metric.label"
          class="inline-flex items-center gap-2 rounded-full border border-slate-200 bg-white px-3 py-1.5 text-xs text-slate-600"
        >
          <component :is="metric.icon" class="h-3.5 w-3.5" :class="metric.iconClass" />
          <span>{{ metric.label }}</span>
          <span class="font-semibold text-slate-900">{{ metric.value }}</span>
        </article>
      </div>

      <div class="mt-4 rounded-2xl border border-slate-200 bg-white px-4 py-3 text-sm text-slate-700">
        {{ report.message }}
      </div>
    </template>
  </section>
</template>

<script setup>
import { computed } from 'vue'
import { AlertTriangle, CheckCircle2, Mail, PackageCheck, Search } from 'lucide-vue-next'

const props = defineProps({
  report: {
    type: Object,
    default: null,
  },
  loading: {
    type: Boolean,
    default: false,
  },
})

const hasReport = computed(() => Boolean(props.report))

const headerLabel = computed(() => {
  if (props.loading) return 'Lecture des boites Gmail et verification des numeros detectes.'
  if (!props.report) return ''
  return `${props.report.scannedMessages || 0} email(s) relus, ${props.report.deliveryMessages || 0} email(s) livraison, ${props.report.rejectedCount || 0} faux positif(s) rejetes.`
})

const metrics = computed(() => [
  {
    label: 'Vrais suivis',
    value: props.report?.importedCount || 0,
    icon: CheckCircle2,
    iconClass: 'text-emerald-600',
  },
  {
    label: 'Deja suivis',
    value: props.report?.duplicateCount || 0,
    icon: PackageCheck,
    iconClass: 'text-sky-600',
  },
  {
    label: 'A verifier',
    value: props.report?.reviewCount || 0,
    icon: Search,
    iconClass: 'text-amber-600',
  },
  {
    label: 'Faux positifs',
    value: props.report?.rejectedCount || 0,
    icon: AlertTriangle,
    iconClass: 'text-rose-600',
  },
])
</script>

<style scoped>
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
