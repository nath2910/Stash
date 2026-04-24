<template>
  <WidgetCard
    title="Checklist actions"
    subtitle="Execution immediate"
    :accent="accent"
    surface="utility"
    :loading="loading"
    :error="error"
    :widget-width="props.widgetWidth"
    :widget-height="props.widgetHeight"
    :widget-base-width="props.widgetBaseWidth"
    :widget-base-height="props.widgetBaseHeight"
  >
    <div class="checklist">
      <label v-for="item in actions" :key="item.id" class="check-item" :class="{ done: doneSet.has(item.id) }">
        <input type="checkbox" :checked="doneSet.has(item.id)" @change="toggle(item.id)" />
        <div>
          <strong>{{ item.title }}</strong>
          <p>{{ item.description }}</p>
        </div>
      </label>
    </div>
  </WidgetCard>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import StatsServices from '@/services/StatsServices'
import { normalizeKpi } from '@/services/statsAdapters'
import WidgetCard from './_parts/WidgetCard.vue'

const props = defineProps({
  from: String,
  to: String,
  categories: { type: Array, default: () => [] },
  types: { type: Array, default: () => [] },
  widgetWidth: { type: Number, default: 1180 },
  widgetHeight: { type: Number, default: 390 },
  widgetBaseWidth: { type: Number, default: 1180 },
  widgetBaseHeight: { type: Number, default: 390 },
})

const accent = '#14b8a6'
const loading = ref(false)
const error = ref('')
const actions = ref([])
const doneSet = ref(new Set())
let requestSeq = 0

function toggle(id) {
  const next = new Set(doneSet.value)
  if (next.has(id)) next.delete(id)
  else next.add(id)
  doneSet.value = next
}

function buildActions(metrics) {
  const list = []
  if (metrics.sellThrough < 40) {
    list.push({
      id: 'stock_rotation',
      title: 'Lancer plan rotation SKU lents',
      description: "Isoler 20% des SKU les plus lents et declencher remise ou bundle sous 48h.",
    })
  }
  if (metrics.avgMargin < 24) {
    list.push({
      id: 'pricing_review',
      title: 'Revoir pricing faible marge',
      description: 'Identifier les references sous 20% de marge et appliquer un repricing progressif.',
    })
  }
  if (metrics.cashAvailable < 12000) {
    list.push({
      id: 'cash_protection',
      title: 'Activer mode protection cash',
      description: "Geler les achats non prioritaires et reallouer vers les meilleures rotations.",
    })
  }
  if (metrics.netProfitDelta < 0) {
    list.push({
      id: 'margin_bridge',
      title: 'Analyser bridge profit semaine',
      description: 'Comparer mix prix, volume et couts pour isoler la source du recul.',
    })
  }
  if (!list.length) {
    list.push(
      {
        id: 'scale_winners',
        title: 'Scaler les best sellers',
        description: 'Renforcer les references top-profit et monter le niveau de stock securise.',
      },
      {
        id: 'stability_check',
        title: 'Stabiliser execution',
        description: 'Maintenir le plan hebdo et suivre les alertes medium pour prevenir les derives.',
      },
    )
  }
  return list
}

async function load() {
  const seq = ++requestSeq
  loading.value = true
  error.value = ''
  try {
    const [sellThroughRes, cashRes, marginRes, profitRes] = await Promise.all([
      StatsServices.kpi('sellThrough', props.from, props.to, props.categories, props.types),
      StatsServices.kpi('cashAvailable', props.from, props.to, props.categories, props.types),
      StatsServices.kpi('avgMargin', props.from, props.to, props.categories, props.types),
      StatsServices.kpi('netProfit', props.from, props.to, props.categories, props.types),
    ])
    if (seq !== requestSeq) return
    actions.value = buildActions({
      sellThrough: normalizeKpi(sellThroughRes.data).value,
      cashAvailable: normalizeKpi(cashRes.data).value,
      avgMargin: normalizeKpi(marginRes.data).value,
      netProfitDelta: normalizeKpi(profitRes.data).deltaPct ?? 0,
    })
  } catch (err) {
    if (seq !== requestSeq) return
    error.value = err?.response?.data?.message ?? err?.message ?? 'Impossible de charger'
  } finally {
    if (seq === requestSeq) loading.value = false
  }
}

onMounted(load)
watch(() => [props.from, props.to, props.categories, props.types], load)
</script>

<style scoped>
.checklist {
  height: 100%;
  overflow: auto;
  display: grid;
  gap: 8px;
}

.check-item {
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 10px;
  align-items: flex-start;
  border-radius: 12px;
  border: 1px solid rgba(148, 163, 184, 0.22);
  background: rgba(15, 23, 42, 0.56);
  padding: 10px;
}

.check-item.done {
  border-color: rgba(20, 184, 166, 0.44);
  background: rgba(15, 118, 110, 0.22);
}

.check-item input {
  margin-top: 4px;
}

.check-item strong {
  font-size: 13px;
  color: rgba(248, 250, 252, 0.96);
}

.check-item p {
  margin-top: 4px;
  font-size: 12px;
  color: rgba(203, 213, 225, 0.84);
  line-height: 1.35;
}
</style>

