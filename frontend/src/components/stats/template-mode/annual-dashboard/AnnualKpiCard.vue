<template>
  <article class="annual-kpi" :class="toneClass">
    <div class="annual-kpi__head">
      <span class="annual-kpi__label">{{ label }}</span>
      <component :is="icon" v-if="icon" class="annual-kpi__icon" aria-hidden="true" />
    </div>
    <div class="annual-kpi__value">{{ value }}</div>
    <div v-if="detail" class="annual-kpi__detail">{{ detail }}</div>
  </article>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(
  defineProps<{
    label: string
    value: string
    detail?: string
    tone?: 'primary' | 'profit' | 'warning' | 'neutral'
    icon?: unknown
  }>(),
  {
    detail: '',
    tone: 'neutral',
    icon: null,
  },
)

const toneClass = computed(() => `annual-kpi--${props.tone}`)
</script>

<style scoped>
.annual-kpi {
  min-width: 0;
  border: 1px solid rgba(148, 163, 184, 0.28);
  border-radius: 8px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.94), rgba(248, 250, 252, 0.9)),
    radial-gradient(circle at top right, rgba(99, 102, 241, 0.08), transparent 34%);
  box-shadow: 0 12px 30px rgba(31, 41, 55, 0.08);
  padding: clamp(14px, 1.6vw, 18px);
  display: grid;
  gap: 10px;
}

.annual-kpi__head {
  min-width: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.annual-kpi__label {
  min-width: 0;
  color: #64748b;
  font-size: 0.74rem;
  font-weight: 760;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  white-space: normal;
}

.annual-kpi__icon {
  width: 18px;
  height: 18px;
  flex: 0 0 auto;
  color: #6d5dfc;
}

.annual-kpi__value {
  min-width: 0;
  color: #111827;
  font-size: clamp(1.45rem, 2.6vw, 2.2rem);
  line-height: 1.05;
  font-weight: 820;
  overflow-wrap: anywhere;
}

.annual-kpi__detail {
  min-width: 0;
  color: #64748b;
  font-size: 0.82rem;
  line-height: 1.35;
  overflow-wrap: anywhere;
}

.annual-kpi--primary .annual-kpi__icon {
  color: #4f46e5;
}

.annual-kpi--profit .annual-kpi__icon,
.annual-kpi--profit .annual-kpi__value {
  color: #047857;
}

.annual-kpi--warning .annual-kpi__icon,
.annual-kpi--warning .annual-kpi__value {
  color: #b45309;
}

.annual-kpi--neutral .annual-kpi__icon {
  color: #475569;
}
</style>
