<template>
  <div class="stock-summary-compact" aria-label="Resume stock">
    <article class="stock-summary-item">
      <span>Total achete</span>
      <strong>{{ totalPaires }}</strong>
    </article>
    <article class="stock-summary-item is-success">
      <span>En stock</span>
      <strong>{{ nbEnStock }}</strong>
    </article>
    <article class="stock-summary-item is-accent">
      <span>Valeur stock</span>
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
  grid-template-columns: 1fr;
  align-content: center;
  gap: 0.55rem;
  height: auto;
  min-width: 0;
}

.stock-summary-item {
  position: relative;
  display: flex;
  min-height: 72px;
  min-width: 0;
  flex-direction: column;
  justify-content: space-between;
  overflow: hidden;
  border: 1px solid rgba(71, 85, 105, 0.82);
  border-radius: 14px;
  background:
    linear-gradient(180deg, rgba(30, 41, 59, 0.42), rgba(15, 23, 42, 0.68)),
    rgba(15, 23, 42, 0.72);
  padding: 0.68rem 0.76rem;
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.045),
    0 12px 28px rgba(2, 6, 23, 0.14);
}

.stock-summary-item::before {
  position: absolute;
  top: 0;
  right: 0.9rem;
  left: 0.9rem;
  height: 2px;
  border-radius: 999px;
  background: rgba(226, 232, 240, 0.34);
  content: '';
}

.stock-summary-item.is-success::before {
  background: rgba(45, 212, 191, 0.9);
}

.stock-summary-item.is-accent::before {
  background: rgba(250, 204, 21, 0.88);
}

.stock-summary-item span {
  display: block;
  color: rgb(148 163 184);
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.11em;
  line-height: 1.15;
  text-transform: uppercase;
}

.stock-summary-item strong {
  display: flex;
  align-items: baseline;
  gap: 0.24rem;
  min-width: 0;
  color: rgb(248 250 252);
  font-size: 1.75rem;
  font-weight: 900;
  line-height: 1;
  letter-spacing: 0;
  white-space: nowrap;
}

.stock-summary-item small {
  color: rgb(203 213 225);
  font-size: 11px;
  font-weight: 850;
  letter-spacing: 0.04em;
}

.stock-summary-item.is-success strong {
  color: rgb(94 234 212);
}

.stock-summary-item.is-accent strong {
  color: rgb(253 224 71);
  font-size: 1.55rem;
}

@media (min-width: 520px) {
  .stock-summary-compact {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (min-width: 1440px) {
  .stock-summary-compact {
    height: 100%;
  }

  .stock-summary-item {
    min-height: 86px;
  }

  .stock-summary-item strong {
    font-size: 2rem;
  }

  .stock-summary-item.is-accent strong {
    font-size: 1.78rem;
  }
}
</style>
