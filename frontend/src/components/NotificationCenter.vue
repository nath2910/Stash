<template>
  <Transition name="notification-center-fade">
    <section
      v-if="open"
      class="fixed z-[85] right-2 left-2 bottom-[5.25rem] sm:left-auto sm:right-6 sm:w-[380px] max-h-[68vh] rounded-3xl border border-slate-700/80 bg-slate-950/92 backdrop-blur-xl shadow-[0_30px_80px_rgba(2,6,23,0.58)] overflow-hidden"
      role="dialog"
      aria-label="Centre de notifications"
    >
      <header class="px-4 py-3 border-b border-slate-800/95">
        <div class="flex items-center justify-between gap-3">
          <div>
            <p class="text-[10px] uppercase tracking-[0.22em] text-slate-400">Centre</p>
            <h3 class="text-sm font-semibold text-slate-100">Notifications</h3>
          </div>

          <div class="flex items-center gap-2">
            <span
              class="inline-flex items-center justify-center min-w-6 h-6 rounded-full bg-emerald-500/15 border border-emerald-400/35 text-[11px] font-semibold text-emerald-100 px-1.5"
            >
              {{ unreadCount }}
            </span>

            <button
              type="button"
              class="rounded-full border border-slate-700 px-2.5 py-1 text-[11px] text-slate-200 hover:border-emerald-400/50 disabled:opacity-45"
              :disabled="!unreadCount"
              @click="$emit('read-all')"
            >
              Tout lire
            </button>

            <button
              type="button"
              class="rounded-full p-1.5 text-slate-300 hover:bg-slate-800/70 hover:text-white"
              aria-label="Fermer"
              @click="$emit('close')"
            >
              <X class="h-4 w-4" />
            </button>
          </div>
        </div>
      </header>

      <div class="max-h-[calc(68vh-62px)] overflow-auto">
        <div v-if="loading && !notifications.length" class="px-4 py-8 text-center text-sm text-slate-400">
          Chargement...
        </div>

        <div v-else-if="!notifications.length" class="px-4 py-8 text-center">
          <p class="text-sm text-slate-300">Aucune notification recente.</p>
          <p class="mt-1 text-xs text-slate-500">Les nouvelles alertes apparaitront ici.</p>
        </div>

        <ul v-else class="divide-y divide-slate-800/80">
          <li
            v-for="notification in notifications"
            :key="notification.id"
            class="px-4 py-3 transition-colors duration-150"
            :class="notification.isRead ? 'bg-transparent' : 'bg-emerald-400/4'"
          >
            <div class="flex items-start gap-3">
              <span
                class="mt-1 inline-flex h-2.5 w-2.5 rounded-full"
                :class="notification.isRead ? 'bg-slate-600' : 'bg-emerald-300 animate-pulse'"
              ></span>

              <div class="min-w-0 flex-1">
                <button
                  type="button"
                  class="block w-full text-left"
                  @click="$emit('open-notification', notification)"
                >
                  <h4 class="text-sm font-medium text-slate-100 truncate">{{ notification.title }}</h4>
                  <p class="mt-1 text-xs text-slate-300 leading-relaxed">{{ notification.message }}</p>
                </button>

                <div class="mt-2.5 flex items-center gap-2 text-[11px]">
                  <button
                    v-if="!notification.isRead"
                    type="button"
                    class="rounded-full border border-slate-600/80 px-2.5 py-1 text-slate-100 hover:border-emerald-300/50"
                    @click="$emit('mark-read', notification.id)"
                  >
                    Marquer lu
                  </button>

                  <button
                    v-if="notification.ctaRoute"
                    type="button"
                    class="rounded-full border border-emerald-400/40 px-2.5 py-1 text-emerald-100 hover:border-emerald-300/80"
                    @click="$emit('open-notification', notification)"
                  >
                    {{ notification.ctaLabel || 'Ouvrir' }}
                  </button>

                  <button
                    type="button"
                    class="rounded-full border border-slate-700 px-2.5 py-1 text-slate-300 hover:border-slate-500"
                    @click="$emit('dismiss', notification.id)"
                  >
                    Masquer
                  </button>

                  <span class="ml-auto text-slate-500">{{ formatDate(notification.createdAt) }}</span>
                </div>
              </div>
            </div>
          </li>
        </ul>

        <div v-if="hasNext" class="px-4 py-3 border-t border-slate-800/80">
          <button
            type="button"
            class="w-full rounded-xl border border-slate-700/85 bg-slate-900/70 px-3 py-2 text-xs text-slate-200 hover:border-emerald-400/45"
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
import { X } from 'lucide-vue-next'

defineProps({
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
})

defineEmits(['close', 'mark-read', 'read-all', 'dismiss', 'load-more', 'open-notification'])

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
.notification-center-fade-enter-active,
.notification-center-fade-leave-active {
  transition: opacity 180ms ease, transform 180ms ease;
}

.notification-center-fade-enter-from,
.notification-center-fade-leave-to {
  opacity: 0;
  transform: translateY(8px) scale(0.985);
}

@media (prefers-reduced-motion: reduce) {
  .notification-center-fade-enter-active,
  .notification-center-fade-leave-active {
    transition: none;
  }
}
</style>