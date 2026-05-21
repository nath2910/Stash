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
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.45rem;
  height: auto;
  min-width: 0;
}

.stock-summary-item {
  position: relative;
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  align-items: baseline;
  column-gap: 0.55rem;
  min-height: 42px;
  min-width: 132px;
  overflow: hidden;
  border: 1px solid rgba(51, 65, 85, 0.76);
  border-radius: 999px;
  background: rgba(15, 23, 42, 0.48);
  padding: 0.42rem 0.72rem 0.42rem 0.9rem;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.035);
}

.stock-summary-item::before {
  position: absolute;
  top: 50%;
  left: 0.5rem;
  width: 5px;
  height: 5px;
  border-radius: 999px;
  background: rgba(226, 232, 240, 0.34);
  transform: translateY(-50%);
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
  font-size: 9.5px;
  font-weight: 800;
  letter-spacing: 0.08em;
  line-height: 1.15;
  text-transform: uppercase;
  white-space: nowrap;
}

.stock-summary-item strong {
  display: flex;
  align-items: baseline;
  justify-content: flex-end;
  gap: 0.2rem;
  min-width: 0;
  color: rgb(248 250 252);
  font-size: 1.08rem;
  font-weight: 900;
  line-height: 1;
  letter-spacing: 0;
  white-space: nowrap;
}

.stock-summary-item small {
  color: rgb(203 213 225);
  font-size: 9px;
  font-weight: 850;
  letter-spacing: 0.03em;
}

.stock-summary-item.is-success strong {
  color: rgb(94 234 212);
}

.stock-summary-item.is-accent strong {
  color: rgb(253 224 71);
  font-size: 1.05rem;
}

@media (max-width: 720px) {
  .stock-summary-compact {
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .stock-summary-item {
    min-width: 0;
  }
}

@media (max-width: 520px) {
  .stock-summary-compact {
    grid-template-columns: 1fr;
  }

  .stock-summary-item {
    min-height: 42px;
  }

  .stock-summary-item strong,
  .stock-summary-item.is-accent strong {
    font-size: 1.05rem;
  }
}
</style>
