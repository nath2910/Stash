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

    <div v-if="showEmptyState" class="home-empty-state">
      <span class="home-empty-state__icon" aria-hidden="true">+</span>
      <div class="home-empty-state__copy">
        <strong>Aucune vente enregistree pour l'instant.</strong>
        <span>Ajoute un premier item pour lancer les KPI de l'accueil.</span>
      </div>
      <button type="button" class="home-empty-state__action" @click="$emit('add-requested')">
        Ajouter une vente
      </button>
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

defineEmits(['add-requested'])

const summaryValue = (key) => Number(props.summary?.[key] ?? 0)
const showEmptyState = computed(
  () =>
    !props.loading &&
    !props.error &&
    summaryValue('ca') <= 0 &&
    summaryValue('itemsVendues') <= 0,
)

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
  min-width: 0;
}

.panel-heading > * {
  min-width: 0;
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
  overflow-wrap: anywhere;
}

.kpi-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(min(100%, 13.5rem), 1fr));
  gap: 0.9rem;
}

.home-empty-state {
  display: flex;
  align-items: center;
  gap: 0.9rem;
  border: 1px dashed rgba(14, 165, 233, 0.28);
  border-radius: 18px;
  background: linear-gradient(135deg, rgba(236, 254, 255, 0.78), rgba(239, 246, 255, 0.76));
  padding: 0.95rem 1rem;
}

.home-empty-state__icon {
  display: inline-grid;
  width: 2.4rem;
  height: 2.4rem;
  flex: 0 0 auto;
  place-items: center;
  border-radius: 16px;
  background: linear-gradient(135deg, #0ea5e9, #14b8a6);
  color: white;
  font-size: 1.15rem;
  font-weight: 900;
}

.home-empty-state__copy {
  display: grid;
  gap: 0.15rem;
}

.home-empty-state__copy strong {
  color: #0f172a;
  font-size: 0.92rem;
  font-weight: 850;
}

.home-empty-state__copy span {
  color: #475569;
  font-size: 0.8rem;
}

.home-empty-state__action {
  margin-left: auto;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 2.5rem;
  border-radius: 999px;
  background: #0f172a;
  color: white;
  font-size: 0.78rem;
  font-weight: 850;
  padding: 0.55rem 0.9rem;
}

@media (max-width: 920px) {
  .home-empty-state {
    align-items: flex-start;
    flex-direction: column;
  }

  .home-empty-state__action {
    margin-left: 0;
  }
}

@media (max-width: 620px) {
  .panel-heading {
    display: grid;
  }

  .panel-error {
    text-align: left;
  }
}
</style>
