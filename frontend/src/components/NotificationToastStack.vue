<template>
  <div
    class="notification-toast-stack"
    :class="stackThemeClass"
    aria-live="polite"
    aria-atomic="false"
  >
    <TransitionGroup name="notification-toast" tag="div" class="notification-toast-list">
      <article
        v-for="toast in limitedToasts"
        :key="toast.id"
        class="notification-toast-card"
        :class="severityClass(toast.severity)"
      >
        <span class="notification-toast-icon" aria-hidden="true">
          <BellRing v-if="normalizeSeverity(toast.severity) === 'INFO'" class="h-4 w-4" />
          <AlertTriangle
            v-else-if="normalizeSeverity(toast.severity) === 'WARNING'"
            class="h-4 w-4"
          />
          <OctagonAlert v-else class="h-4 w-4" />
        </span>

        <div class="notification-toast-content">
          <p>Notification</p>
          <h4>{{ toast.title }}</h4>
          <span>{{ toast.message }}</span>

          <div class="notification-toast-actions">
            <button type="button" class="notification-toast-action" @click="$emit('open-center')">
              Centre
            </button>
            <button
              v-if="toast.ctaRoute"
              type="button"
              class="notification-toast-action notification-toast-action--primary"
              @click="$emit('cta', toast)"
            >
              {{ toast.ctaLabel || 'Voir' }}
            </button>
          </div>
        </div>

        <button
          type="button"
          class="notification-toast-close"
          aria-label="Fermer"
          @click="$emit('close', toast.id)"
        >
          <X class="h-4 w-4" aria-hidden="true" />
        </button>
      </article>
    </TransitionGroup>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { AlertTriangle, BellRing, OctagonAlert, X } from 'lucide-vue-next'

const props = defineProps({
  toasts: {
    type: Array,
    default: () => [],
  },
  theme: {
    type: String,
    default: 'dark',
  },
})

defineEmits(['close', 'cta', 'open-center'])

const limitedToasts = computed(() => props.toasts.slice(0, 3))
const stackThemeClass = computed(() =>
  props.theme === 'home' || props.theme === 'light'
    ? 'notification-toast-stack--home'
    : 'notification-toast-stack--dark',
)

function normalizeSeverity(severity) {
  return String(severity || 'INFO').toUpperCase()
}

function severityClass(severity) {
  const normalized = normalizeSeverity(severity)
  if (normalized === 'WARNING') return 'tone-warning'
  if (normalized === 'CRITICAL') return 'tone-critical'
  return 'tone-info'
}
</script>

<style scoped>
.notification-toast-stack {
  pointer-events: none;
  position: fixed;
  right: max(0.75rem, env(safe-area-inset-right));
  bottom: calc(5.75rem + env(safe-area-inset-bottom, 0px));
  z-index: 90;
  width: min(92vw, 390px);
  color: var(--toast-text);
}

.notification-toast-stack--home {
  --toast-surface: linear-gradient(145deg, rgba(255, 255, 255, 0.97), rgba(236, 254, 255, 0.94));
  --toast-border: rgba(45, 212, 191, 0.44);
  --toast-text: #0f172a;
  --toast-muted: #64748b;
  --toast-action: rgba(255, 255, 255, 0.76);
  --toast-shadow: 0 18px 46px rgba(14, 116, 144, 0.18), 0 8px 24px rgba(15, 23, 42, 0.1);
}

.notification-toast-stack--dark {
  --toast-surface: linear-gradient(145deg, rgba(15, 23, 42, 0.94), rgba(2, 6, 23, 0.9));
  --toast-border: rgba(148, 163, 184, 0.24);
  --toast-text: #f8fafc;
  --toast-muted: #cbd5e1;
  --toast-action: rgba(15, 23, 42, 0.68);
  --toast-shadow: 0 20px 48px rgba(2, 6, 23, 0.52);
}

.notification-toast-list {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.notification-toast-card {
  pointer-events: auto;
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  gap: 0.75rem;
  border: 1px solid var(--toast-border);
  border-radius: 1.1rem;
  background: var(--toast-surface);
  box-shadow: var(--toast-shadow);
  padding: 0.82rem;
  backdrop-filter: blur(16px) saturate(135%);
}

.notification-toast-card.tone-info {
  --toast-tone: #14b8a6;
}

.notification-toast-card.tone-warning {
  --toast-tone: #f59e0b;
}

.notification-toast-card.tone-critical {
  --toast-tone: #ef4444;
}

.notification-toast-icon {
  display: inline-flex;
  width: 2rem;
  height: 2rem;
  align-items: center;
  justify-content: center;
  border: 1px solid color-mix(in srgb, var(--toast-tone) 38%, transparent);
  border-radius: 0.78rem;
  background: color-mix(in srgb, var(--toast-tone) 12%, transparent);
  color: var(--toast-tone);
}

.notification-toast-content {
  min-width: 0;
}

.notification-toast-content p {
  color: var(--toast-tone);
  font-size: 0.66rem;
  font-weight: 900;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.notification-toast-content h4 {
  margin-top: 0.12rem;
  overflow: hidden;
  color: var(--toast-text);
  font-size: 0.92rem;
  font-weight: 850;
  line-height: 1.2;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.notification-toast-content span {
  display: block;
  margin-top: 0.35rem;
  color: var(--toast-muted);
  font-size: 0.78rem;
  line-height: 1.45;
}

.notification-toast-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.45rem;
  margin-top: 0.7rem;
}

.notification-toast-action,
.notification-toast-close {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 1px solid var(--toast-border);
  border-radius: 999px;
  background: var(--toast-action);
  color: var(--toast-text);
  font-size: 0.72rem;
  font-weight: 800;
  transition:
    border-color 150ms ease,
    background-color 150ms ease,
    transform 150ms ease;
}

.notification-toast-action {
  min-height: 1.85rem;
  padding: 0.42rem 0.7rem;
}

.notification-toast-action--primary {
  border-color: color-mix(in srgb, var(--toast-tone) 56%, transparent);
  color: var(--toast-tone);
}

.notification-toast-close {
  width: 1.9rem;
  height: 1.9rem;
}

.notification-toast-action:hover,
.notification-toast-close:hover {
  border-color: color-mix(in srgb, var(--toast-tone) 72%, transparent);
  transform: translateY(-1px);
}

.notification-toast-enter-active,
.notification-toast-leave-active {
  transition:
    opacity 220ms ease,
    transform 220ms ease;
}

.notification-toast-enter-from,
.notification-toast-leave-to {
  opacity: 0;
  transform: translateY(10px) scale(0.985);
}

.notification-toast-move {
  transition: transform 220ms ease;
}

@media (max-width: 420px) {
  .notification-toast-stack {
    right: max(0.5rem, env(safe-area-inset-right));
    bottom: calc(5.45rem + env(safe-area-inset-bottom, 0px));
    width: calc(100vw - 1rem);
  }

  .notification-toast-card {
    grid-template-columns: minmax(0, 1fr) auto;
  }

  .notification-toast-icon {
    display: none;
  }
}

@media (prefers-reduced-motion: reduce) {
  .notification-toast-enter-active,
  .notification-toast-leave-active,
  .notification-toast-move,
  .notification-toast-action,
  .notification-toast-close {
    transition: none;
  }

  .notification-toast-action:hover,
  .notification-toast-close:hover {
    transform: none;
  }
}
</style>
