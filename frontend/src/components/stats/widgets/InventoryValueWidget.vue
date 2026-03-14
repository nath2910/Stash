<template>
  <KpiCard
    title="Valeur du stock"
    subtitle="Capital immobilise"
    :accent="accent"
    :loading="loading"
    :error="error"
    :valueText="valueText"
    hint="au cout d'achat"
  >
    <div class="accent-strip" />
    <div class="asof-row">
      <span class="asof-label">Valeur au</span>
      <span class="asof-value">{{ asOfLabel }}</span>
    </div>
  </KpiCard>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import StatsServices from '@/services/StatsServices'
import { normalizeSummary, parseYmdLocal, prevPeriod } from '@/services/statsAdapters'
import { formatEUR, signFmt } from '@/utils/formatters'
import KpiCard from './_parts/KpiCard.vue'

const props = defineProps({
  from: String,
  to: String,
  asOf: String,
  useGlobalRange: { type: Boolean, default: true },
  categories: { type: Array, default: () => [] },
  types: { type: Array, default: () => [] },
})
const accent = '#06B6D4'

const loading = ref(false)
const error = ref('')
const summary = ref({ valeurStock: 0 })
let req = 0

async function load() {
  const useGlobal = props.useGlobalRange !== false
  const baseDate = useGlobal ? (props.to || props.from) : (props.asOf || props.to || props.from)
  if (!baseDate) return
  const id = ++req
  loading.value = true
  error.value = ''
  try {
    const currRes = await StatsServices.summary({
      from: baseDate,
      to: baseDate,
      asOf: baseDate,
      categories: props.categories,
      types: props.types,
    })
    if (id !== req) return
    const curr = normalizeSummary(currRes.data)
    summary.value = curr
  } catch (e) {
    if (id !== req) return
    error.value = e?.response?.data?.message ?? e?.message ?? 'Impossible de charger'
  } finally {
    if (id === req) loading.value = false
  }
}

onMounted(load)
watch(() => [props.from, props.to, props.asOf, props.useGlobalRange, props.categories, props.types], load)

const valueText = computed(() => formatEUR(summary.value.valeurStock, { compact: true }))
const asOfLabel = computed(() => {
  const useGlobal = props.useGlobalRange !== false
  const raw = useGlobal ? (props.to || props.from) : (props.asOf || props.to || props.from)
  if (!raw) return '--'
  const d = parseYmdLocal(raw)
  return d.toLocaleDateString('fr-FR')
})
</script>

<style scoped>
.asof-row {
  margin-top: 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding-top: 10px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
}
.accent-strip {
  margin-top: 10px;
  height: 6px;
  border-radius: 999px;
  background: linear-gradient(90deg, rgba(6, 182, 212, 0.2), rgba(6, 182, 212, 0.9));
  box-shadow: 0 0 14px rgba(6, 182, 212, 0.25);
}
.asof-label {
  font-size: 0.7rem;
  letter-spacing: 0.04em;
  text-transform: uppercase;
  color: rgba(226, 232, 240, 0.6);
}
.asof-value {
  font-size: 0.75rem;
  color: rgba(226, 232, 240, 0.9);
  padding: 2px 8px;
  border-radius: 999px;
  border: 1px solid rgba(6, 182, 212, 0.25);
  background: rgba(6, 182, 212, 0.08);
}
</style>
