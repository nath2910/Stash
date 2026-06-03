<template>
  <section class="home-panel">
    <div class="panel-heading">
      <div>
        <p class="panel-eyebrow">Annee en cours</p>
        <h2>KPI essentiels</h2>
      </div>
      <p v-if="error" class="panel-error">{{ error }}</p>
    </div>

    <div class="kpi-grid">
      <KpiCard title="C.A. annuel" :value="formatEUR(summaryValue('ca'))" :loading="loading" />
      <KpiCard
        title="Benefices annuels"
        :value="formatEUR(summaryValue('profit'))"
        :tone="summaryValue('profit') >= 0 ? 'success' : 'danger'"
        :loading="loading"
      />
      <KpiCard
        title="En stock"
        :value="formatNumber(summaryValue('itemsEnStock'))"
        subtitle="Items disponibles"
        tone="accent"
        :loading="loading"
      />
      <KpiCard
        title="Valeur inventaire"
        :value="formatEUR(summaryValue('valeurStock'))"
        :subtitle="estimatedSubtitle"
        :loading="loading"
      />
    </div>
  </section>
</template>

<script setup>
import { computed } from 'vue'
import KpiCard from './KpiCard.vue'
import { formatEUR, formatNumber } from '@/utils/formatters'

const props = defineProps({
  summary: { type: Object, default: null },
  loading: { type: Boolean, default: false },
  error: { type: String, default: '' },
})

const summaryValue = (key) => Number(props.summary?.[key] ?? 0)

const estimatedSubtitle = computed(() => {
  const estimated = Number(props.summary?.estimatedStockValue ?? 0)
  const invested = Number(props.summary?.valeurStock ?? 0)
  if (estimated > 0 && Math.abs(estimated - invested) >= 0.01) {
    return `Valeur estimee ${formatEUR(estimated)}`
  }
  return 'Montant investi'
})
</script>

<style scoped>
.home-panel {
  position: relative;
  display: grid;
  gap: 1rem;
  border: 1px solid rgba(203, 213, 225, 0.72);
  border-radius: 20px;
  background: #ffffff;
  padding: clamp(1rem, 2vw, 1.35rem);
  box-shadow: 0 18px 42px rgba(15, 23, 42, 0.07);
}

.home-panel::before {
  content: '';
  position: absolute;
  inset: 0 1rem auto;
  height: 3px;
  border-radius: 999px;
  background: linear-gradient(90deg, #f59e0b, #14b8a6, #0ea5e9);
}

.panel-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
}

.panel-eyebrow {
  color: #b45309;
  font-size: 0.72rem;
  font-weight: 800;
  text-transform: uppercase;
}

h2 {
  margin-top: 0.15rem;
  color: #0f172a;
  font-size: clamp(1.15rem, 2.4vw, 1.45rem);
  font-weight: 800;
}

.panel-error {
  max-width: 28rem;
  color: #b91c1c;
  font-size: 0.82rem;
  font-weight: 600;
  text-align: right;
}

.kpi-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 0.9rem;
}

@media (max-width: 980px) {
  .kpi-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 560px) {
  .panel-heading {
    display: grid;
  }

  .panel-error {
    text-align: left;
  }

  .kpi-grid {
    grid-template-columns: 1fr;
  }
}
</style>
