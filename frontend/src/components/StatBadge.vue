<template>
  <div
    class="stat-badge"
    :class="[
      variant === 'dashboard' ? 'stat-badge--dashboard' : 'stat-badge--default',
      `stat-badge--${toneName}`,
      unit ? 'stat-badge--with-unit' : '',
    ]"
  >
    <div class="stat-badge__content">
      <p class="stat-badge__label">
        {{ label }}
      </p>
      <div class="stat-badge__value-row">
        <p class="stat-badge__value" :class="valueClasses">
          <slot>{{ value }}</slot>
        </p>
        <span v-if="unit" class="stat-badge__unit">
          {{ unit }}
        </span>
      </div>
      <p v-if="$slots.footer" class="stat-badge__footer">
        <slot name="footer" />
      </p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

interface Props {
  label: string
  value?: string | number
  tone?: 'default' | 'success' | 'danger' | 'accent'
  unit?: string
  variant?: 'default' | 'dashboard'
}

const props = defineProps<Props>()
const toneName = computed(() => props.tone || 'default')

const valueClasses = computed(() => {
  switch (props.tone) {
    case 'success':
      return 'text-emerald-300'
    case 'danger':
      return 'text-rose-300'
    case 'accent':
      return 'text-amber-200'
    default:
      return 'text-slate-100'
  }
})
</script>

<style scoped>
.stat-badge {
  position: relative;
  overflow: hidden;
  text-align: left;
}

.stat-badge--default {
  border-radius: 1rem;
  border: 1px solid rgba(30, 41, 59, 0.7);
  background: rgba(15, 23, 42, 0.7);
  padding: 0.75rem 1rem;
}

.stat-badge--dashboard {
  display: flex;
  min-height: 128px;
  border-radius: 18px;
  border: 1px solid rgba(100, 116, 139, 0.28);
  background: rgba(15, 23, 42, 0.72);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.04),
    0 10px 22px rgba(2, 6, 23, 0.12);
  padding: 1.25rem;
}

.stat-badge--dashboard::after {
  content: '';
  position: absolute;
  inset: auto 1.25rem 1.05rem 1.25rem;
  height: 1px;
  background: rgba(148, 163, 184, 0.12);
}

.stat-badge__content {
  position: relative;
  z-index: 1;
  display: grid;
  width: 100%;
  gap: 0.55rem;
  min-width: 0;
}

.stat-badge__label {
  color: rgb(148 163 184);
  font-size: 0.7rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  line-height: 1.25;
  text-transform: uppercase;
}

.stat-badge--dashboard .stat-badge__label {
  max-width: 15rem;
}

.stat-badge__value-row {
  display: flex;
  align-items: baseline;
  gap: 0.5rem;
  min-width: 0;
}

.stat-badge--dashboard .stat-badge__value-row {
  margin-top: 0.05rem;
  flex-wrap: nowrap;
}

.stat-badge__value {
  color: rgb(241 245 249);
  font-size: 1.125rem;
  font-weight: 700;
  line-height: 1.1;
}

.stat-badge--dashboard .stat-badge__value {
  max-width: 100%;
  overflow: hidden;
  font-size: 2.35rem;
  font-weight: 800;
  letter-spacing: 0;
  white-space: nowrap;
}

.stat-badge--dashboard.stat-badge--with-unit .stat-badge__value {
  flex: 0 1 auto;
  min-width: 0;
  font-size: 1.9rem;
}

.stat-badge--success .stat-badge__value {
  color: #5eead4;
}

.stat-badge--accent .stat-badge__value {
  color: #fde68a;
}

.stat-badge--danger .stat-badge__value {
  color: #fda4af;
}

.stat-badge__unit {
  border-radius: 999px;
  background: rgba(30, 41, 59, 0.86);
  color: rgb(226 232 240);
  padding: 0.2rem 0.55rem;
  font-size: 0.65rem;
  font-weight: 800;
  letter-spacing: 0.06em;
  white-space: nowrap;
}

.stat-badge--dashboard .stat-badge__unit {
  flex: 0 0 auto;
}

.stat-badge__footer {
  margin-top: 0.25rem;
  color: rgb(100 116 139);
  font-size: 0.7rem;
}
</style>
