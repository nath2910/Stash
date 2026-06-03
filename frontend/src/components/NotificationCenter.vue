<template>
  <Transition name="notification-center-fade">
    <section
      v-if="open"
      class="notification-center-panel"
      :class="panelThemeClass"
      role="dialog"
      aria-label="Centre de notifications"
    >
      <header class="notification-center-header">
        <div class="min-w-0">
          <p class="notification-center-eyebrow">Alertes Stash</p>
          <h3 class="notification-center-title">Notifications</h3>
        </div>

        <div class="notification-center-tools">
          <span class="notification-count" :aria-label="`${unreadCount} notifications non lues`">
            {{ unreadCount }}
          </span>

          <button
            type="button"
            class="notification-action notification-action--soft"
            :disabled="!unreadCount"
            @click="$emit('read-all')"
          >
            Tout lire
          </button>

          <button
            type="button"
            class="notification-icon-button"
            aria-label="Fermer"
            @click="$emit('close')"
          >
            <X class="h-4 w-4" aria-hidden="true" />
          </button>
        </div>
      </header>

      <div class="notification-center-body">
        <div v-if="loading && !notifications.length" class="notification-state">
          Chargement...
        </div>

        <div v-else-if="!notifications.length" class="notification-state notification-state--empty">
          <Inbox class="h-6 w-6" aria-hidden="true" />
          <p>Aucune notification recente.</p>
          <span>Les nouvelles alertes apparaitront ici.</span>
        </div>

        <ul v-else class="notification-list">
          <li
            v-for="notification in notifications"
            :key="notification.id"
            class="notification-item"
            :class="[
              notification.isRead ? 'is-read' : 'is-unread',
              severityClass(notification.severity),
            ]"
          >
            <span class="notification-item-icon" aria-hidden="true">
              <component :is="severityIcon(notification.severity)" class="h-4 w-4" />
            </span>

            <div class="notification-item-content">
              <button
                type="button"
                class="notification-item-main"
                @click="$emit('open-notification', notification)"
              >
                <span class="notification-item-meta">
                  {{ severityLabel(notification.severity) }} - {{ typeLabel(notification.type) }}
                </span>
                <strong>{{ notification.title }}</strong>
                <span>{{ notification.message }}</span>
              </button>

              <div class="notification-item-actions">
                <button
                  v-if="!notification.isRead"
                  type="button"
                  class="notification-action"
                  @click="$emit('mark-read', notification.id)"
                >
                  <Check class="h-3.5 w-3.5" aria-hidden="true" />
                  Lu
                </button>

                <button
                  v-if="notification.ctaRoute"
                  type="button"
                  class="notification-action notification-action--primary"
                  @click="$emit('open-notification', notification)"
                >
                  <ExternalLink class="h-3.5 w-3.5" aria-hidden="true" />
                  {{ notification.ctaLabel || 'Ouvrir' }}
                </button>

                <button
                  type="button"
                  class="notification-action"
                  @click="$emit('dismiss', notification.id)"
                >
                  <Archive class="h-3.5 w-3.5" aria-hidden="true" />
                  Masquer
                </button>

                <time class="notification-item-date" :datetime="notification.createdAt">
                  {{ formatDate(notification.createdAt) }}
                </time>
              </div>
            </div>
          </li>
        </ul>

        <div v-if="hasNext" class="notification-load-more">
          <button
            type="button"
            class="notification-action notification-action--wide"
            :disabled="loading"
            @click="$emit('load-more')"
          >
            {{ loading ? 'Chargement...' : 'Charger plus' }}
          </button>
        </div>
      </div>
    </section>
  </Transition>
</template>

<script setup>
import { computed } from 'vue'
import { AlertTriangle, Archive, BellRing, Check, ExternalLink, Inbox, OctagonAlert, X } from 'lucide-vue-next'

const props = defineProps({
  open: {
    type: Boolean,
    default: false,
  },
  notifications: {
    type: Array,
    default: () => [],
  },
  unreadCount: {
    type: Number,
    default: 0,
  },
  loading: {
    type: Boolean,
    default: false,
  },
  hasNext: {
    type: Boolean,
    default: false,
  },
  theme: {
    type: String,
    default: 'dark',
  },
})

defineEmits(['close', 'mark-read', 'read-all', 'dismiss', 'load-more', 'open-notification'])

const panelThemeClass = computed(() =>
  props.theme === 'home' || props.theme === 'light'
    ? 'notification-center-panel--home'
    : 'notification-center-panel--dark',
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

function severityIcon(severity) {
  const normalized = normalizeSeverity(severity)
  if (normalized === 'WARNING') return AlertTriangle
  if (normalized === 'CRITICAL') return OctagonAlert
  return BellRing
}

function severityLabel(severity) {
  const normalized = normalizeSeverity(severity)
  if (normalized === 'WARNING') return 'Attention'
  if (normalized === 'CRITICAL') return 'Critique'
  return 'Info'
}

function typeLabel(type) {
  const normalized = String(type || '').toUpperCase()
  if (normalized === 'SUBSCRIPTION_EXPIRING') return 'Abonnement'
  if (normalized === 'STOCK_AGING') return 'Inventaire'
  return 'Stash'
}

function formatDate(value) {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return ''
  return date.toLocaleString('fr-FR', {
    day: '2-digit',
    month: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}
</script>

<style scoped>
.notification-center-panel {
  position: fixed;
  right: max(0.5rem, env(safe-area-inset-right));
  bottom: calc(5.25rem + env(safe-area-inset-bottom, 0px));
  left: max(0.5rem, env(safe-area-inset-left));
  z-index: 85;
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  max-height: min(68vh, 640px);
  overflow: hidden;
  border: 1px solid var(--notification-border);
  border-radius: 1.35rem;
  color: var(--notification-text);
  background: var(--notification-surface);
  box-shadow: var(--notification-shadow);
  backdrop-filter: blur(18px) saturate(135%);
}

.notification-center-panel--home {
  --notification-surface: linear-gradient(145deg, rgba(255, 255, 255, 0.96), rgba(236, 254, 255, 0.92));
  --notification-border: rgba(45, 212, 191, 0.42);
  --notification-text: #0f172a;
  --notification-muted: #64748b;
  --notification-subtle: rgba(14, 116, 144, 0.12);
  --notification-card: rgba(255, 255, 255, 0.74);
  --notification-card-hover: rgba(240, 253, 250, 0.9);
  --notification-action: rgba(255, 255, 255, 0.76);
  --notification-action-border: rgba(125, 211, 252, 0.58);
  --notification-shadow: 0 24px 70px rgba(14, 116, 144, 0.18), 0 10px 28px rgba(15, 23, 42, 0.08);
}

.notification-center-panel--dark {
  --notification-surface: linear-gradient(145deg, rgba(15, 23, 42, 0.96), rgba(2, 6, 23, 0.9));
  --notification-border: rgba(148, 163, 184, 0.24);
  --notification-text: #f8fafc;
  --notification-muted: #94a3b8;
  --notification-subtle: rgba(45, 212, 191, 0.12);
  --notification-card: rgba(15, 23, 42, 0.68);
  --notification-card-hover: rgba(30, 41, 59, 0.78);
  --notification-action: rgba(15, 23, 42, 0.58);
  --notification-action-border: rgba(148, 163, 184, 0.32);
  --notification-shadow: 0 30px 80px rgba(2, 6, 23, 0.58);
}

.notification-center-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.9rem;
  border-bottom: 1px solid var(--notification-action-border);
  padding: 1rem;
}

.notification-center-eyebrow {
  color: var(--notification-muted);
  font-size: 0.66rem;
  font-weight: 800;
  letter-spacing: 0.18em;
  text-transform: uppercase;
}

.notification-center-title {
  margin-top: 0.1rem;
  color: var(--notification-text);
  font-size: 1rem;
  font-weight: 800;
}

.notification-center-tools,
.notification-item-actions {
  display: flex;
  align-items: center;
  gap: 0.45rem;
  min-width: 0;
}

.notification-count {
  display: inline-flex;
  min-width: 1.65rem;
  height: 1.65rem;
  align-items: center;
  justify-content: center;
  border: 1px solid rgba(45, 212, 191, 0.42);
  border-radius: 999px;
  background: rgba(45, 212, 191, 0.14);
  color: #0f766e;
  font-size: 0.72rem;
  font-weight: 900;
  padding-inline: 0.45rem;
}

.notification-center-panel--dark .notification-count {
  color: #ccfbf1;
}

.notification-icon-button,
.notification-action {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.3rem;
  min-height: 1.85rem;
  border: 1px solid var(--notification-action-border);
  border-radius: 999px;
  background: var(--notification-action);
  color: var(--notification-text);
  font-size: 0.72rem;
  font-weight: 800;
  line-height: 1;
  transition:
    border-color 150ms ease,
    background-color 150ms ease,
    color 150ms ease,
    transform 150ms ease;
}

.notification-icon-button {
  width: 1.9rem;
  height: 1.9rem;
}

.notification-action {
  padding: 0.45rem 0.68rem;
}

.notification-action:hover,
.notification-icon-button:hover {
  border-color: rgba(20, 184, 166, 0.72);
  background: var(--notification-card-hover);
  transform: translateY(-1px);
}

.notification-action:disabled {
  cursor: not-allowed;
  opacity: 0.46;
  transform: none;
}

.notification-action--primary {
  border-color: rgba(20, 184, 166, 0.56);
  background: linear-gradient(135deg, rgba(20, 184, 166, 0.18), rgba(14, 165, 233, 0.14));
  color: #0f766e;
}

.notification-center-panel--dark .notification-action--primary {
  color: #ccfbf1;
}

.notification-action--wide {
  width: 100%;
  min-height: 2.4rem;
}

.notification-center-body {
  min-height: 0;
  max-height: 100%;
  overflow: auto;
  overscroll-behavior: contain;
  padding: 0.7rem;
  -webkit-overflow-scrolling: touch;
  scrollbar-gutter: stable;
}

.notification-center-body::-webkit-scrollbar {
  width: 0.55rem;
}

.notification-center-body::-webkit-scrollbar-track {
  background: transparent;
}

.notification-center-body::-webkit-scrollbar-thumb {
  border: 2px solid transparent;
  border-radius: 999px;
  background: rgba(14, 116, 144, 0.34);
  background-clip: padding-box;
}

.notification-center-panel--dark .notification-center-body::-webkit-scrollbar-thumb {
  background: rgba(148, 163, 184, 0.34);
  background-clip: padding-box;
}

.notification-state {
  display: grid;
  justify-items: center;
  gap: 0.35rem;
  padding: 2rem 1rem;
  color: var(--notification-muted);
  text-align: center;
  font-size: 0.88rem;
}

.notification-state--empty p {
  color: var(--notification-text);
  font-weight: 800;
}

.notification-state--empty span {
  font-size: 0.78rem;
}

.notification-list {
  display: grid;
  gap: 0.55rem;
}

.notification-item {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 0.7rem;
  border: 1px solid var(--notification-action-border);
  border-radius: 1rem;
  background: var(--notification-card);
  padding: 0.78rem;
  transition:
    border-color 150ms ease,
    background-color 150ms ease,
    transform 150ms ease;
}

.notification-item:hover {
  border-color: rgba(45, 212, 191, 0.48);
  background: var(--notification-card-hover);
  transform: translateY(-1px);
}

.notification-item.is-unread {
  box-shadow: inset 3px 0 0 var(--notification-tone);
}

.notification-item.tone-info {
  --notification-tone: #14b8a6;
}

.notification-item.tone-warning {
  --notification-tone: #f59e0b;
}

.notification-item.tone-critical {
  --notification-tone: #ef4444;
}

.notification-item-icon {
  display: inline-flex;
  width: 2rem;
  height: 2rem;
  align-items: center;
  justify-content: center;
  border: 1px solid color-mix(in srgb, var(--notification-tone) 38%, transparent);
  border-radius: 0.78rem;
  background: color-mix(in srgb, var(--notification-tone) 12%, transparent);
  color: var(--notification-tone);
}

.notification-item-content {
  min-width: 0;
}

.notification-item-main {
  display: grid;
  width: 100%;
  min-width: 0;
  gap: 0.22rem;
  text-align: left;
}

.notification-item-main strong {
  overflow: hidden;
  color: var(--notification-text);
  font-size: 0.9rem;
  font-weight: 850;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.notification-item-main span:last-child {
  color: var(--notification-muted);
  font-size: 0.78rem;
  line-height: 1.45;
}

.notification-item-meta {
  color: var(--notification-tone);
  font-size: 0.66rem;
  font-weight: 900;
  letter-spacing: 0.09em;
  text-transform: uppercase;
}

.notification-item-actions {
  flex-wrap: wrap;
  margin-top: 0.68rem;
}

.notification-item-date {
  margin-left: auto;
  color: var(--notification-muted);
  font-size: 0.7rem;
  white-space: nowrap;
}

.notification-load-more {
  border-top: 1px solid var(--notification-action-border);
  margin-top: 0.65rem;
  padding-top: 0.65rem;
}

.notification-center-fade-enter-active,
.notification-center-fade-leave-active {
  transition:
    opacity 180ms ease,
    transform 180ms ease;
}

.notification-center-fade-enter-from,
.notification-center-fade-leave-to {
  opacity: 0;
  transform: translateY(8px) scale(0.985);
}

@media (min-width: 640px) {
  .notification-center-panel {
    left: auto;
    right: 1.5rem;
    width: 390px;
  }
}

@media (max-width: 420px) {
  .notification-center-header {
    align-items: flex-start;
    flex-direction: column;
  }

  .notification-center-tools {
    width: 100%;
  }

  .notification-item {
    grid-template-columns: 1fr;
  }

  .notification-item-date {
    margin-left: 0;
    width: 100%;
  }
}

@media (prefers-reduced-motion: reduce) {
  .notification-center-fade-enter-active,
  .notification-center-fade-leave-active,
  .notification-action,
  .notification-icon-button,
  .notification-item {
    transition: none;
  }

  .notification-action:hover,
  .notification-icon-button:hover,
  .notification-item:hover {
    transform: none;
  }
}
</style>
