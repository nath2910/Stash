<template>
  <div
    class="pointer-events-none fixed right-3 bottom-[5.75rem] z-[90] flex w-[min(92vw,380px)] flex-col gap-3 sm:right-6 sm:bottom-24"
    aria-live="polite"
    aria-atomic="false"
  >
    <TransitionGroup name="notification-toast" tag="div" class="flex flex-col gap-3">
      <article
        v-for="toast in limitedToasts"
        :key="toast.id"
        class="pointer-events-auto rounded-2xl border backdrop-blur-md shadow-[0_16px_38px_rgba(2,6,23,0.5)] p-3.5"
        :class="severityClass(toast.severity)"
      >
        <div class="flex items-start gap-3">
          <div class="mt-0.5 shrink-0">
            <BellRing v-if="normalizeSeverity(toast.severity) === 'INFO'" class="h-4 w-4 text-emerald-200" />
            <AlertTriangle v-else-if="normalizeSeverity(toast.severity) === 'WARNING'" class="h-4 w-4 text-amber-200" />
            <OctagonAlert v-else class="h-4 w-4 text-red-200" />
          </div>

          <div class="min-w-0 flex-1">
            <p class="text-xs uppercase tracking-[0.16em] text-slate-300/80">Notification</p>
            <h4 class="text-sm font-semibold text-slate-50 leading-tight mt-0.5">{{ toast.title }}</h4>
            <p class="text-xs text-slate-200/85 mt-1.5 leading-relaxed">{{ toast.message }}</p>

            <div class="mt-2.5 flex items-center gap-2">
              <button
                type="button"
                class="rounded-full border border-slate-500/40 bg-slate-800/60 px-3 py-1 text-[11px] font-medium text-slate-100 hover:border-emerald-300/50"
                @click="$emit('open-center')"
              >
                Ouvrir le centre
              </button>
              <button
                v-if="toast.ctaRoute"
                type="button"
                class="rounded-full border border-emerald-300/35 bg-emerald-400/10 px-3 py-1 text-[11px] font-medium text-emerald-100 hover:border-emerald-200/70"
                @click="$emit('cta', toast)"
              >
                {{ toast.ctaLabel || 'Voir' }}
              </button>
            </div>
          </div>

          <button
            type="button"
            class="shrink-0 rounded-full p-1 text-slate-300 hover:text-white hover:bg-slate-700/60"
            aria-label="Fermer"
            @click="$emit('close', toast.id)"
          >
            <X class="h-4 w-4" />
          </button>
        </div>
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
})

defineEmits(['close', 'cta', 'open-center'])

const limitedToasts = computed(() => props.toasts.slice(0, 3))

function normalizeSeverity(severity) {
  return String(severity || 'INFO').toUpperCase()
}

function severityClass(severity) {
  const normalized = normalizeSeverity(severity)
  if (normalized === 'WARNING') {
    return 'border-amber-300/25 bg-[linear-gradient(135deg,rgba(120,53,15,0.42),rgba(51,65,85,0.78))]'
  }
  if (normalized === 'CRITICAL') {
    return 'border-red-300/30 bg-[linear-gradient(135deg,rgba(127,29,29,0.45),rgba(51,65,85,0.82))]'
  }
  return 'border-emerald-300/25 bg-[linear-gradient(135deg,rgba(6,78,59,0.45),rgba(51,65,85,0.82))]'
}
</script>

<style scoped>
.notification-toast-enter-active,
.notification-toast-leave-active {
  transition: opacity 220ms ease, transform 220ms ease;
}

.notification-toast-enter-from,
.notification-toast-leave-to {
  opacity: 0;
  transform: translateY(10px) scale(0.985);
}

.notification-toast-move {
  transition: transform 220ms ease;
}

@media (prefers-reduced-motion: reduce) {
  .notification-toast-enter-active,
  .notification-toast-leave-active,
  .notification-toast-move {
    transition: none;
  }
}
</style>