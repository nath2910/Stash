<template>
  <article class="kpi-card" :class="toneClass">
    <p class="kpi-title">{{ title }}</p>
    <div class="kpi-value">
      <span v-if="loading" class="kpi-skeleton"></span>
      <span v-else>{{ value }}</span>
    </div>
    <p v-if="subtitle && !loading" class="kpi-subtitle">{{ subtitle }}</p>
  </article>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  title: { type: String, required: true },
  value: { type: [String, Number], default: '--' },
  subtitle: { type: String, default: '' },
  tone: { type: String, default: 'neutral' },
  loading: { type: Boolean, default: false },
})

const toneClass = computed(() => `kpi-card--${props.tone}`)
</script>

<style scoped>
.kpi-card {
  position: relative;
  min-height: 112px;
  display: grid;
  align-content: space-between;
  gap: 0.75rem;
  border: 1px solid rgba(148, 163, 184, 0.22);
  border-radius: 16px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(248, 250, 252, 0.92)),
    #ffffff;
  padding: 1rem;
  box-shadow: 0 16px 34px rgba(15, 23, 42, 0.07);
  transition:
    border-color 160ms ease,
    box-shadow 160ms ease,
    transform 160ms ease;
}

.kpi-card:hover {
  border-color: rgba(15, 118, 110, 0.24);
  box-shadow: 0 20px 40px rgba(15, 23, 42, 0.1);
  transform: translateY(-1px);
}

.kpi-card::before {
  content: '';
  position: absolute;
  inset: 0 auto 0 0;
  width: 4px;
  border-radius: 16px 0 0 16px;
  background: #94a3b8;
  opacity: 0.55;
}

.kpi-title {
  color: #64748b;
  font-size: 0.72rem;
  font-weight: 700;
  text-transform: uppercase;
}

.kpi-value {
  min-height: 2.1rem;
  color: #0f172a;
  font-size: clamp(1.45rem, 3vw, 2rem);
  font-weight: 800;
  line-height: 1;
}

.kpi-subtitle {
  min-height: 1rem;
  color: #64748b;
  font-size: 0.78rem;
  font-weight: 500;
}

.kpi-card--success .kpi-value {
  color: #047857;
}

.kpi-card--success {
  border-color: rgba(16, 185, 129, 0.24);
  background:
    linear-gradient(135deg, rgba(16, 185, 129, 0.08), transparent 42%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(240, 253, 244, 0.74));
}

.kpi-card--success::before {
  background: #10b981;
}

.kpi-card--danger .kpi-value {
  color: #b91c1c;
}

.kpi-card--danger {
  border-color: rgba(248, 113, 113, 0.24);
  background:
    linear-gradient(135deg, rgba(248, 113, 113, 0.08), transparent 42%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(254, 242, 242, 0.7));
}

.kpi-card--danger::before {
  background: #ef4444;
}

.kpi-card--accent {
  border-color: rgba(14, 165, 233, 0.24);
  background:
    linear-gradient(135deg, rgba(14, 165, 233, 0.09), transparent 42%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(239, 246, 255, 0.76));
}

.kpi-card--accent::before {
  background: #0ea5e9;
}

.kpi-skeleton {
  display: block;
  width: min(72%, 180px);
  height: 1.9rem;
  border-radius: 999px;
  background: linear-gradient(90deg, #e2e8f0, #f8fafc, #e2e8f0);
  background-size: 200% 100%;
  animation: pulse 1.2s ease-in-out infinite;
}

@keyframes pulse {
  from {
    background-position: 100% 0;
  }
  to {
    background-position: -100% 0;
  }
}
</style>
