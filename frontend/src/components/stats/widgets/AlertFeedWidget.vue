<template>
  <WidgetCard
    title="Alertes"
    subtitle="Signaux de pilotage"
    :accent="accent"
    surface="utility"
    :loading="loading"
    :error="error"
    :widget-width="props.widgetWidth"
    :widget-height="props.widgetHeight"
    :widget-base-width="props.widgetBaseWidth"
    :widget-base-height="props.widgetBaseHeight"
  >
    <div class="alert-feed">
      <div v-if="!items.length" class="alert-empty">Aucune alerte prioritaire.</div>
      <article v-for="item in visibleItems" :key="item.id" class="alert-card" :class="`tone-${item.tone}`">
        <header>
          <span class="alert-pill">{{ item.level }}</span>
          <strong>{{ item.title }}</strong>
        </header>
        <p>{{ item.message }}</p>
      </article>
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
  maxItems: { type: Number, default: 8 },
  categories: { type: Array, default: () => [] },
  types: { type: Array, default: () => [] },
  widgetWidth: { type: Number, default: 1080 },
  widgetHeight: { type: Number, default: 390 },
  widgetBaseWidth: { type: Number, default: 1080 },
  widgetBaseHeight: { type: Number, default: 390 },
})

const accent = '#f59e0b'
const loading = ref(false)
const error = ref('')
const items = ref([])
let requestSeq = 0

function buildAlerts(metrics) {
  const alerts = []

  const push = (tone, title, message) => {
    alerts.push({
      id: `${tone}_${alerts.length}`,
      tone,
      level: tone === 'high' ? 'High' : tone === 'medium' ? 'Medium' : 'Info',
      title,
      message,
    })
  }

  if (metrics.sellThrough < 35) {
    push('high', 'Rotation insuffisante', "Le taux d'ecoulement est sous 35%. Prioriser les SKU dormants.")
  }
  if (metrics.cashAvailable < 12000) {
    push('high', 'Tension cash', 'Cash disponible bas. Revoir rythme d achat et plan liquidation.')
  }
  if (metrics.avgMargin < 24) {
    push('medium', 'Marge sous objectif', 'La marge moyenne est sous 24%. Ajuster pricing et remises.')
  }
  if (metrics.netProfitDelta < 0) {
    push('medium', 'Profit en recul', 'Le benefice net recule vs periode precedente. Verifier mix et couts.')
  }
  if (metrics.roi >= 35) {
    push('info', 'ROI solide', 'Le ROI reste au-dessus de 35%. Maintenir les references performantes.')
  }
  return alerts
}

async function load() {
  const seq = ++requestSeq
  loading.value = true
  error.value = ''
  try {
    const [sellThroughRes, cashRes, marginRes, profitRes, roiRes] = await Promise.all([
      StatsServices.kpi('sellThrough', props.from, props.to, props.categories, props.types),
      StatsServices.kpi('cashAvailable', props.from, props.to, props.categories, props.types),
      StatsServices.kpi('avgMargin', props.from, props.to, props.categories, props.types),
      StatsServices.kpi('netProfit', props.from, props.to, props.categories, props.types),
      StatsServices.kpi('roi', props.from, props.to, props.categories, props.types),
    ])
    if (seq !== requestSeq) return
    items.value = buildAlerts({
      sellThrough: normalizeKpi(sellThroughRes.data).value,
      cashAvailable: normalizeKpi(cashRes.data).value,
      avgMargin: normalizeKpi(marginRes.data).value,
      netProfitDelta: normalizeKpi(profitRes.data).deltaPct ?? 0,
      roi: normalizeKpi(roiRes.data).value,
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

const visibleItems = computed(() => items.value.slice(0, Math.max(1, Number(props.maxItems || 8))))
</script>

<style scoped>
.alert-feed {
  height: 100%;
  overflow: auto;
  display: grid;
  gap: 8px;
}

.alert-empty {
  display: grid;
  place-items: center;
  min-height: 160px;
  color: rgba(203, 213, 225, 0.7);
  font-size: 13px;
}

.alert-card {
  border-radius: 12px;
  border: 1px solid rgba(148, 163, 184, 0.22);
  background: rgba(15, 23, 42, 0.56);
  padding: 10px;
  display: grid;
  gap: 5px;
}

.alert-card header {
  display: flex;
  align-items: center;
  gap: 8px;
}

.alert-card strong {
  font-size: 13px;
  color: rgba(248, 250, 252, 0.96);
}

.alert-card p {
  font-size: 12px;
  color: rgba(203, 213, 225, 0.86);
  line-height: 1.35;
}

.alert-pill {
  font-size: 10px;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  padding: 3px 7px;
  border-radius: 999px;
}

.alert-card.tone-high .alert-pill {
  color: #fecdd3;
  border: 1px solid rgba(244, 63, 94, 0.38);
  background: rgba(190, 24, 93, 0.22);
}

.alert-card.tone-medium .alert-pill {
  color: #fde68a;
  border: 1px solid rgba(245, 158, 11, 0.34);
  background: rgba(180, 83, 9, 0.24);
}

.alert-card.tone-info .alert-pill {
  color: #bae6fd;
  border: 1px solid rgba(14, 165, 233, 0.34);
  background: rgba(2, 132, 199, 0.2);
}
</style>

