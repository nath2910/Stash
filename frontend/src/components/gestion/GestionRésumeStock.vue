<template>
  <div class="stock-summary-compact" aria-label="Resume stock">
    <article class="stock-summary-item">
      <span class="stock-summary-label">
        <i aria-hidden="true"></i>
        Total achete
      </span>
      <strong>{{ totalPaires }}</strong>
    </article>
    <article class="stock-summary-item is-success">
      <span class="stock-summary-label">
        <i aria-hidden="true"></i>
        En stock
      </span>
      <strong>{{ nbEnStock }}</strong>
    </article>
    <article class="stock-summary-item is-accent">
      <span class="stock-summary-label">
        <i aria-hidden="true"></i>
        Valeur stock
      </span>
      <strong>{{ formattedStockValue }} <small>EUR</small></strong>
    </article>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

interface Props {
  totalPaires: number
  nbEnStock: number
  valeurStock: number
}

const props = defineProps<Props>()
const compactNumberFormatter = new Intl.NumberFormat('fr-FR', {
  notation: 'compact',
  maximumFractionDigits: 1,
})

const formattedStockValue = computed(() => {
  const value = Math.max(0, Math.round(Number(props.valeurStock) || 0))
  if (value < 10_000) {
    return String(value).replace(/\B(?=(\d{3})+(?!\d))/g, ' ')
  }
  return compactNumberFormatter.format(value).replace(/\s+/g, '')
})
</script>

<style scoped>
.stock-summary-compact {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.7rem;
  height: auto;
  min-width: 0;
}

.stock-summary-item {
  display: flex;
  min-height: 64px;
  min-width: 0;
  align-items: center;
  justify-content: space-between;
  gap: 0.85rem;
  overflow: hidden;
  border: 1px solid rgba(148, 163, 184, 0.22);
  border-radius: 8px;
  background: #ffffff;
  padding: 0.78rem 0.9rem;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.04);
}

.stock-summary-item:hover {
  border-color: rgba(20, 184, 166, 0.28);
  box-shadow: 0 8px 20px rgba(15, 23, 42, 0.06);
}

.stock-summary-label {
  display: inline-flex;
  min-width: 0;
  align-items: center;
  gap: 0.45rem;
  color: #64748b;
  font-size: 0.72rem;
  font-weight: 850;
  letter-spacing: 0.03em;
  line-height: 1.15;
  text-transform: uppercase;
}

.stock-summary-label i {
  width: 0.45rem;
  height: 0.45rem;
  flex: 0 0 auto;
  border-radius: 999px;
  background: #94a3b8;
}

.stock-summary-item.is-success .stock-summary-label i {
  background: #14b8a6;
}

.stock-summary-item.is-accent .stock-summary-label i {
  background: #f59e0b;
}

.stock-summary-item strong {
  display: flex;
  align-items: baseline;
  justify-content: flex-end;
  gap: 0.25rem;
  min-width: 0;
  color: #0f172a;
  font-size: clamp(1.35rem, 2.2vw, 1.7rem);
  font-weight: 950;
  line-height: 1;
  letter-spacing: 0;
  white-space: nowrap;
}

.stock-summary-item small {
  color: #64748b;
  font-size: 0.68rem;
  font-weight: 850;
  letter-spacing: 0.03em;
}

.stock-summary-item.is-success strong {
  color: #0f766e;
}

.stock-summary-item.is-accent strong {
  color: #b45309;
}

@media (max-width: 760px) {
  .stock-summary-compact {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .stock-summary-item {
    display: grid;
    min-height: 72px;
    min-width: 0;
    align-content: center;
    gap: 0.5rem;
  }

  .stock-summary-item strong {
    justify-content: flex-start;
  }
}

@media (max-width: 520px) {
  .stock-summary-compact {
    grid-template-columns: 1fr;
  }

  .stock-summary-item {
    display: flex;
    min-height: 58px;
  }

  .stock-summary-item strong,
  .stock-summary-item.is-accent strong {
    justify-content: flex-end;
    font-size: 1.35rem;
  }
}
</style>
