<template>
  <div ref="dockEl" class="dock">
    <button
      type="button"
      class="fab"
      @click.stop="toggleDock"
      @pointerdown.stop
      :aria-expanded="dockOpen"
      title="Outils"
    >
      <Plus v-if="!dockOpen" class="fab-icon w-6 h-6" />
      <X v-else class="fab-icon w-6 h-6" />
    </button>

    <Transition name="dock">
      <div v-if="dockOpen" class="dock-panel" @click.stop @pointerdown.stop>
        <div class="dock-panel__header">
          <div class="dock-panel__title">Outils</div>
          <div class="dock-panel__hint">Canvas</div>
        </div>
        <button
          type="button"
          class="dock-btn"
          @click="$emit('toggleEdit')"
          title="Mode édition / figé"
        >
          <component :is="editMode ? LockOpen : Lock" class="w-5 h-5" />
          <span>{{ editMode ? 'Mode édition' : 'Mode figé' }}</span>
        </button>

        <div class="dock-sep"></div>

        <button
          type="button"
          class="dock-btn dock-btn--accent"
          :disabled="!editMode"
          :class="{ disabled: !editMode }"
          @click="$emit('openPalette')"
          title="Ajouter un bloc"
        >
          <PlusSquare class="w-5 h-5" />
          <span>Ajouter</span>
        </button>

        <div class="dock-section">
          <div class="dock-title">Vue</div>

          <div class="dock-row">
            <button type="button" class="btn btn-icon" @click="$emit('zoomOut')" title="Dézoomer">
              <Minus class="w-5 h-5" />
            </button>

            <button
              type="button"
              class="btn btn-pill"
              @click="$emit('resetZoom')"
              title="Reset zoom"
            >
              {{ Math.round(scale * 100) }}%
            </button>

            <button type="button" class="btn btn-icon" @click="$emit('zoomIn')" title="Zoomer">
              <PlusSquare class="w-5 h-5" />
            </button>
          </div>

          <div class="dock-row">
            <button type="button" class="btn btn-icon" @click="$emit('centerView')" title="Centrer">
              <LocateFixed class="w-5 h-5" />
            </button>

            <button
              type="button"
              class="btn btn-icon"
              :disabled="!editMode"
              :class="{ disabled: !editMode }"
              @click="$emit('resetLayout')"
              title="Tout supprimer"
            >
              <RotateCcw class="w-5 h-5" />
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { Plus, Minus, PlusSquare, LocateFixed, RotateCcw, X, Lock, LockOpen } from 'lucide-vue-next'

const props = defineProps<{
  editMode: boolean
  scale: number
  paletteOpen?: boolean
}>()

defineEmits<{
  (e: 'toggleEdit'): void
  (e: 'openPalette'): void
  (e: 'zoomIn'): void
  (e: 'zoomOut'): void
  (e: 'resetZoom'): void
  (e: 'centerView'): void
  (e: 'resetLayout'): void
}>()

const dockOpen = ref(false)
const dockEl = ref<HTMLElement | null>(null)

function toggleDock() {
  dockOpen.value = !dockOpen.value
}

watch(
  () => props.paletteOpen,
  (open) => {
    if (open) dockOpen.value = false
  },
)

function onWindowPointerDown(e: PointerEvent) {
  if (!dockOpen.value) return
  const path = (e.composedPath?.() ?? []) as EventTarget[]
  if (dockEl.value && path.includes(dockEl.value)) return
  dockOpen.value = false
}
function onKeyDown(e: KeyboardEvent) {
  if (e.key === 'Escape') dockOpen.value = false
}

onMounted(() => {
  window.addEventListener('pointerdown', onWindowPointerDown)
  window.addEventListener('keydown', onKeyDown)
})
onBeforeUnmount(() => {
  window.removeEventListener('pointerdown', onWindowPointerDown)
  window.removeEventListener('keydown', onKeyDown)
})
</script>

<style scoped>
.dock {
  position: fixed;
  top: max(78px, env(safe-area-inset-top, 0px) + 68px);
  right: clamp(10px, 2.8vw, 18px);
  z-index: 60;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 16px;
}

@media (max-width: 1024px) {
  .dock {
    top: auto;
    bottom: calc(env(safe-area-inset-bottom, 0px) + 86px);
    right: 12px;
    gap: 10px;
  }
  .dock-panel {
    width: min(340px, calc(100vw - 24px));
    max-height: min(62dvh, 520px);
    overflow: auto;
  }
}

@media (max-width: 768px) {
  .dock {
    bottom: calc(env(safe-area-inset-bottom, 0px) + 82px);
    right: 12px;
    gap: 12px;
  }
  .dock-panel {
    width: min(320px, calc(100vw - 20px));
    padding: 10px;
    border-radius: 16px;
  }
  .dock-btn {
    height: 38px;
    padding: 0 10px;
  }
  .dock-btn span {
    font-size: 0.88rem;
  }
  .btn {
    height: 36px;
  }
  .btn-pill {
    width: 78px;
  }
}

@media (max-width: 480px) {
  .dock {
    right: 10px;
    bottom: calc(env(safe-area-inset-bottom, 0px) + 74px);
  }
  .fab {
    width: 42px;
    height: 42px;
    border-radius: 13px;
  }
  .dock-panel {
    width: calc(100vw - 20px);
    max-height: min(58dvh, 460px);
  }
  .dock-panel__header {
    margin-bottom: 8px;
  }
  .dock-section {
    gap: 8px;
    margin-top: 8px;
  }
}

.fab {
  width: 44px;
  height: 44px;
  border-radius: 14px;
  display: grid;
  place-items: center;
  border: 1px solid rgba(255, 255, 255, 0.12);
  background: radial-gradient(
      90% 120% at 20% 0%,
      rgba(99, 102, 241, 0.15) 0%,
      rgba(15, 23, 42, 0.4) 55%,
      rgba(2, 6, 23, 0.55) 100%
    );
  backdrop-filter: blur(14px);
  color: rgba(255, 255, 255, 0.92);
  box-shadow:
    0 10px 30px rgba(0, 0, 0, 0.35),
    inset 0 0 0 1px rgba(255, 255, 255, 0.03);
  transition:
    transform 140ms ease,
    box-shadow 160ms ease,
    border-color 160ms ease;
}
.fab:hover {
  transform: translateY(-1px);
  border-color: rgba(129, 140, 248, 0.45);
  box-shadow:
    0 14px 36px rgba(0, 0, 0, 0.45),
    inset 0 0 0 1px rgba(255, 255, 255, 0.06);
}
.fab-icon,
.fab-icon * {
  pointer-events: none;
}

.dock-panel {
  width: 316px;
  padding: 12px;
  border-radius: 18px;
  background:
    linear-gradient(180deg, rgba(8, 13, 24, 0.72) 0%, rgba(5, 9, 18, 0.68) 100%),
    radial-gradient(120% 120% at 15% 0%, rgba(129, 140, 248, 0.12), transparent 60%);
  border: 1px solid rgba(255, 255, 255, 0.12);
  backdrop-filter: blur(14px);
  box-shadow:
    0 16px 40px rgba(0, 0, 0, 0.45),
    inset 0 0 0 1px rgba(255, 255, 255, 0.03);
}
.dock-panel__header {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-bottom: 10px;
  padding: 2px 2px 0;
}
.dock-panel__title {
  font-size: 0.82rem;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: rgba(226, 232, 240, 0.8);
}
.dock-panel__hint {
  font-size: 0.7rem;
  color: rgba(148, 163, 184, 0.7);
}
.dock-btn {
  width: 100%;
  height: 40px;
  padding: 0 12px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  gap: 10px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  background: rgba(255, 255, 255, 0.05);
  color: rgba(255, 255, 255, 0.92);
  transition:
    border-color 160ms ease,
    background 160ms ease,
    transform 140ms ease;
}
.dock-btn:hover {
  border-color: rgba(148, 163, 184, 0.35);
  background: rgba(255, 255, 255, 0.08);
  transform: translateY(-1px);
}
.dock-btn span {
  font-weight: 600;
  font-size: 0.92rem;
}
.dock-btn.disabled,
.dock-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  pointer-events: none;
}
.dock-btn--accent {
  border-color: rgba(129, 140, 248, 0.35);
  background: rgba(99, 102, 241, 0.14);
}
.dock-btn--accent:hover {
  border-color: rgba(129, 140, 248, 0.6);
  background: rgba(99, 102, 241, 0.22);
}

.dock-sep {
  height: 1px;
  margin: 12px 0;
  background: linear-gradient(
    90deg,
    rgba(255, 255, 255, 0.02),
    rgba(255, 255, 255, 0.14),
    rgba(255, 255, 255, 0.02)
  );
}
.dock-section {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-top: 10px;
}
.dock-title {
  font-size: 0.78rem;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: rgba(148, 163, 184, 0.8);
}
.dock-row {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
.btn {
  height: 38px;
  padding: 0 12px;
  border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  background: rgba(255, 255, 255, 0.05);
  color: rgba(255, 255, 255, 0.92);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  transition:
    border-color 160ms ease,
    background 160ms ease,
    transform 140ms ease;
}
.btn:hover {
  border-color: rgba(148, 163, 184, 0.35);
  background: rgba(255, 255, 255, 0.08);
  transform: translateY(-1px);
}
.btn-icon {
  width: 40px;
  padding: 0;
}
.btn-pill {
  width: 84px;
  font-variant-numeric: tabular-nums;
}
.btn.disabled,
.btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  pointer-events: none;
}

.chip {
  height: 32px;
  padding: 0 10px;
  border-radius: 999px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  background: rgba(255, 255, 255, 0.05);
  color: rgba(255, 255, 255, 0.8);
  font-size: 0.82rem;
}

/* Anim */
.dock-enter-active,
.dock-leave-active {
  transition:
    opacity 180ms ease,
    transform 200ms ease;
}
.dock-enter-from,
.dock-leave-to {
  opacity: 0;
  transform: translateX(10px) scale(0.98);
}
</style>
