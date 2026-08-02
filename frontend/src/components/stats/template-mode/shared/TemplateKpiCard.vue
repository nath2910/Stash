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

type TemplateKpiNamespace = 'annual' | 'monthly' | 'quarterly' | 'category'
type TemplateKpiTone = 'primary' | 'profit' | 'warning' | 'neutral'

const props = withDefaults(
  defineProps<{
    namespace: TemplateKpiNamespace
    label: string
    value: string
    detail?: string
    tone?: TemplateKpiTone
    icon?: unknown | null
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
  min-height: clamp(112px, 11.8vw, 156px);
  border: 1px solid rgba(148, 163, 184, 0.28);
  border-radius: 8px;
  background: #fbfaf7;
  box-shadow: 0 6px 16px rgba(31, 41, 55, 0.045);
  padding: clamp(15px, 1.75vw, 20px);
  display: grid;
  align-content: start;
  gap: 12px;
  overflow: hidden;
  container-type: inline-size;
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
  font-size: 0.76rem;
  font-weight: 760;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  white-space: normal;
  line-height: 1.16;
  overflow-wrap: anywhere;
}

.template-kpi__icon {
  width: 19px;
  height: 19px;
  flex: 0 0 auto;
  color: #6d5dfc;
}

.template-kpi__value {
  min-width: 0;
  color: #243b53;
  font-size: clamp(1.56rem, 2.85vw, 2.44rem);
  line-height: 1.05;
  font-weight: 820;
  overflow-wrap: anywhere;
}

.template-kpi__detail {
  min-width: 0;
  color: #64748b;
  font-size: 0.86rem;
  line-height: 1.38;
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

@container (max-width: 240px) {
  .template-kpi {
    min-height: 96px;
    padding: 12px;
    gap: 8px;
  }

  .template-kpi__label {
    font-size: 0.68rem;
  }

  .template-kpi__icon {
    width: 16px;
    height: 16px;
  }

  .template-kpi__value {
    font-size: 1.78rem;
  }

  .template-kpi__detail {
    font-size: 0.76rem;
    line-height: 1.28;
  }
}

@container (max-width: 200px) {
  .template-kpi {
    min-height: 88px;
    padding: 10px;
    gap: 6px;
  }

  .template-kpi__label {
    font-size: 0.62rem;
    letter-spacing: 0.06em;
  }

  .template-kpi__icon {
    width: 14px;
    height: 14px;
  }

  .template-kpi__value {
    font-size: 1.44rem;
  }

  .template-kpi__detail {
    font-size: 0.72rem;
    line-height: 1.22;
  }
}
</style>
