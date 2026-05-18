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
  align-content: start;
  gap: 6px;
}

.alert-empty {
  display: grid;
  place-items: center;
  min-height: 160px;
  color: #64748b;
  font-size: 13px;
}

.alert-card {
  border-radius: 8px;
  border: 1px solid rgba(148, 163, 184, 0.24);
  background: rgba(248, 250, 252, 0.82);
  padding: 8px 9px;
  display: grid;
  gap: 3px;
}

.alert-card header {
  display: flex;
  align-items: center;
  gap: 7px;
}

.alert-card strong {
  font-size: 13px;
  line-height: 1.25;
  color: #111827;
}

.alert-card p {
  font-size: 12px;
  color: #64748b;
  line-height: 1.3;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
}

.alert-pill {
  font-size: 10px;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  padding: 2px 6px;
  border-radius: 999px;
}

.alert-card.tone-high .alert-pill {
  color: #be123c;
  border: 1px solid rgba(244, 63, 94, 0.24);
  background: rgba(255, 228, 230, 0.72);
}

.alert-card.tone-medium .alert-pill {
  color: #b45309;
  border: 1px solid rgba(245, 158, 11, 0.24);
  background: rgba(254, 243, 199, 0.78);
}

.alert-card.tone-info .alert-pill {
  color: #0369a1;
  border: 1px solid rgba(14, 165, 233, 0.24);
  background: rgba(224, 242, 254, 0.78);
}
</style>
