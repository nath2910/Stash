<template>
  <teleport to="body">
    <Transition name="quick-intro-fade">
      <div v-if="open" class="quick-intro-layer" role="dialog" aria-modal="true">
        <div class="quick-intro-backdrop" @click="$emit('close')"></div>

        <div class="quick-intro-shell">
          <section class="quick-intro-card">
            <button
              type="button"
              class="quick-intro-close"
              aria-label="Fermer l'introduction"
              @click="$emit('close')"
            >
              <span aria-hidden="true">×</span>
            </button>

            <p class="quick-intro-kicker">{{ kicker }}</p>
            <h2 class="quick-intro-title">{{ title }}</h2>
            <p class="quick-intro-description">{{ description }}</p>
            <p v-if="detail" class="quick-intro-detail">{{ detail }}</p>

            <ul v-if="points.length" class="quick-intro-points">
              <li v-for="point in points" :key="point">{{ point }}</li>
            </ul>

            <div class="quick-intro-actions">
              <button type="button" class="quick-intro-primary" @click="$emit('close')">
                {{ primaryLabel }}
              </button>
            </div>
          </section>
        </div>
      </div>
    </Transition>
  </teleport>
</template>

<script setup>
defineProps({
  open: {
    type: Boolean,
    default: false,
  },
  kicker: {
    type: String,
    default: 'Quick intro',
  },
  title: {
    type: String,
    default: '',
  },
  description: {
    type: String,
    default: '',
  },
  detail: {
    type: String,
    default: '',
  },
  points: {
    type: Array,
    default: () => [],
  },
  primaryLabel: {
    type: String,
    default: "J'ai compris",
  },
})

defineEmits(['close'])
</script>

<style scoped>
.quick-intro-layer {
  position: fixed;
  inset: 0;
  z-index: 190;
}

.quick-intro-backdrop {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at top, rgba(45, 212, 191, 0.18), transparent 34%),
    linear-gradient(180deg, rgba(15, 23, 42, 0.36), rgba(2, 6, 23, 0.62));
  backdrop-filter: blur(18px);
}

.quick-intro-shell {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 1rem;
}

.quick-intro-card {
  position: relative;
  width: min(100%, 34rem);
  border-radius: 1.75rem;
  border: 1px solid rgba(148, 163, 184, 0.22);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.92), rgba(248, 250, 252, 0.98)),
    linear-gradient(135deg, rgba(20, 184, 166, 0.12), rgba(14, 165, 233, 0.08));
  box-shadow: 0 28px 90px rgba(15, 23, 42, 0.22);
  padding: 1.5rem;
  color: #0f172a;
}

.quick-intro-close {
  position: absolute;
  top: 0.9rem;
  right: 0.9rem;
  width: 2.25rem;
  height: 2.25rem;
  border: 0;
  border-radius: 999px;
  background: rgba(148, 163, 184, 0.12);
  color: #475569;
  cursor: pointer;
}

.quick-intro-kicker {
  margin: 0 0 0.45rem;
  font-size: 0.72rem;
  font-weight: 800;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: #0f766e;
}

.quick-intro-title {
  margin: 0;
  font-size: clamp(1.55rem, 3vw, 2rem);
  line-height: 1.05;
  font-weight: 900;
}

.quick-intro-description {
  margin: 0.85rem 0 0;
  font-size: 0.98rem;
  line-height: 1.55;
  color: #334155;
}

.quick-intro-detail {
  margin: 0.9rem 0 0;
  padding: 0.85rem 0.95rem;
  border-radius: 1rem;
  background: rgba(15, 23, 42, 0.05);
  color: #0f172a;
  font-size: 0.9rem;
  line-height: 1.45;
}

.quick-intro-points {
  margin: 1rem 0 0;
  padding: 0;
  list-style: none;
  display: grid;
  gap: 0.6rem;
}

.quick-intro-points li {
  display: flex;
  gap: 0.65rem;
  align-items: flex-start;
  padding: 0.85rem 0.95rem;
  border-radius: 1rem;
  background: linear-gradient(180deg, rgba(240, 253, 250, 0.92), rgba(236, 253, 245, 0.88));
  color: #134e4a;
  font-size: 0.9rem;
  line-height: 1.45;
}

.quick-intro-points li::before {
  content: '';
  width: 0.55rem;
  height: 0.55rem;
  margin-top: 0.35rem;
  border-radius: 999px;
  flex: 0 0 auto;
  background: linear-gradient(135deg, #14b8a6, #0ea5e9);
  box-shadow: 0 0 0 0.24rem rgba(20, 184, 166, 0.14);
}

.quick-intro-actions {
  margin-top: 1.15rem;
  display: flex;
  justify-content: flex-start;
}

.quick-intro-primary {
  min-height: 2.9rem;
  border: 0;
  border-radius: 999px;
  padding: 0 1.15rem;
  background: linear-gradient(135deg, #0f766e, #0ea5e9);
  color: #f8fafc;
  font-weight: 800;
  cursor: pointer;
  box-shadow: 0 14px 30px rgba(14, 116, 144, 0.22);
}

.quick-intro-fade-enter-active,
.quick-intro-fade-leave-active {
  transition:
    opacity 180ms ease,
    transform 180ms ease;
}

.quick-intro-fade-enter-from,
.quick-intro-fade-leave-to {
  opacity: 0;
}

@media (max-width: 640px) {
  .quick-intro-card {
    padding: 1.2rem;
    border-radius: 1.4rem;
  }

  .quick-intro-title {
    padding-right: 2.25rem;
  }
}
</style>
