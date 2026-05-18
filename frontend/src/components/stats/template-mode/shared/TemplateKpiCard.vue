<template>
  <article :class="rootClasses">
    <div :class="partClasses('head')">
      <span :class="partClasses('label')">{{ label }}</span>
      <component :is="icon" v-if="icon" :class="partClasses('icon')" aria-hidden="true" />
    </div>
    <div :class="partClasses('value')">{{ value }}</div>
    <div v-if="detail" :class="partClasses('detail')">{{ detail }}</div>
  </article>
</template>

<script setup lang="ts">
import { computed } from 'vue'

type TemplateKpiNamespace = 'annual' | 'monthly' | 'category'
type TemplateKpiTone = 'primary' | 'profit' | 'warning' | 'neutral'

const props = withDefaults(
  defineProps<{
    namespace: TemplateKpiNamespace
    label: string
    value: string
    detail?: string
    tone?: TemplateKpiTone
    icon?: unknown
  }>(),
  {
    detail: '',
    tone: 'neutral',
    icon: null,
  },
)

const rootClasses = computed(() => [
  'template-kpi',
  `template-kpi--${props.tone}`,
  `${props.namespace}-kpi`,
  `${props.namespace}-kpi--${props.tone}`,
])

function partClasses(part: 'head' | 'label' | 'icon' | 'value' | 'detail') {
  return [`template-kpi__${part}`, `${props.namespace}-kpi__${part}`]
}
</script>

<style scoped>
.template-kpi {
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

.template-kpi__head {
  min-width: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.template-kpi__label {
  min-width: 0;
  color: #64748b;
  font-size: 0.74rem;
  font-weight: 760;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  white-space: normal;
}

.template-kpi__icon {
  width: 18px;
  height: 18px;
  flex: 0 0 auto;
  color: #6d5dfc;
}

.template-kpi__value {
  min-width: 0;
  color: #111827;
  font-size: clamp(1.45rem, 2.6vw, 2.2rem);
  line-height: 1.05;
  font-weight: 820;
  overflow-wrap: anywhere;
}

.template-kpi__detail {
  min-width: 0;
  color: #64748b;
  font-size: 0.82rem;
  line-height: 1.35;
  overflow-wrap: anywhere;
}

.template-kpi--primary .template-kpi__icon {
  color: #4f46e5;
}

.template-kpi--profit .template-kpi__icon,
.template-kpi--profit .template-kpi__value {
  color: #047857;
}

.template-kpi--warning .template-kpi__icon,
.template-kpi--warning .template-kpi__value {
  color: #b45309;
}

.template-kpi--neutral .template-kpi__icon {
  color: #475569;
}
</style>
