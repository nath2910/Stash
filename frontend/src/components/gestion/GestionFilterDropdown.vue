<template>
  <div
    ref="rootEl"
    class="filter-choice"
    :class="{ 'is-open': menuOpen, 'is-disabled': disabled }"
  >
    <button
      ref="triggerEl"
      type="button"
      class="filter-choice__trigger"
      :disabled="disabled"
      :aria-expanded="menuOpen ? 'true' : 'false'"
      @click="toggleMenu"
    >
      <span class="filter-choice__label">{{ label }}</span>
      <span class="filter-choice__trigger-row">
        <span class="filter-choice__icon" :class="iconClass(selectedOption.value)">
          <component :is="iconFor(selectedOption.value)" class="h-4 w-4" aria-hidden="true" />
        </span>
        <span
          class="filter-choice__value"
          :class="{ 'is-placeholder': selectedOption.placeholder }"
        >
          {{ selectedOption.label }}
        </span>
        <ChevronDown class="filter-choice__chevron h-4 w-4" aria-hidden="true" />
      </span>
    </button>

    <Teleport to="body">
      <div
        v-if="menuOpen"
        ref="menuEl"
        class="filter-choice__menu"
        :class="{
          'opens-upward': openUpward,
          'is-subcategory': iconMode === 'subcategory',
          'is-type': iconMode === 'type',
        }"
        :style="menuStyles"
      >
        <button
          v-for="option in safeOptions"
          :key="option.value"
          type="button"
          class="filter-choice__option"
          :class="{ 'is-active': option.value === modelValue }"
          @click="selectOption(option.value)"
        >
          <span class="filter-choice__option-icon" :class="iconClass(option.value)">
            <component :is="iconFor(option.value)" class="h-4 w-4" aria-hidden="true" />
          </span>
          <span class="filter-choice__option-text">
            <span>{{ option.label }}</span>
            <small v-if="option.detail">{{ option.detail }}</small>
          </span>
          <Check v-if="option.value === modelValue" class="filter-choice__check h-4 w-4" />
        </button>
      </div>
    </Teleport>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { Check, ChevronDown, CircleHelp, Package, Tag, Ticket } from 'lucide-vue-next'

const props = defineProps({
  modelValue: { type: String, default: '' },
  label: { type: String, required: true },
  placeholder: { type: String, default: 'Choisir' },
  options: { type: Array, default: () => [] },
  disabled: { type: Boolean, default: false },
  iconMode: { type: String, default: 'type' },
  menuPlacement: { type: String, default: 'auto' },
})

const emit = defineEmits(['update:modelValue'])

const rootEl = ref(null)
const triggerEl = ref(null)
const menuEl = ref(null)
const menuOpen = ref(false)
const openUpward = ref(false)
const menuStyles = ref({})

const safeOptions = computed(() =>
  props.options
    .map((option) => ({
      value: String(option?.value ?? ''),
      label: String(option?.label ?? option?.value ?? ''),
      detail: option?.detail ? String(option.detail) : '',
    }))
    .filter((option) => option.label),
)

const selectedOption = computed(() => {
  const current = safeOptions.value.find((option) => option.value === props.modelValue)
  if (current) return current
  return {
    value: props.modelValue || '',
    label: props.placeholder,
    placeholder: true,
  }
})

function toggleMenu() {
  if (props.disabled) return
  menuOpen.value = !menuOpen.value
}

function selectOption(value) {
  emit('update:modelValue', value)
  menuOpen.value = false
}

function iconFor(value) {
  if (props.iconMode === 'subcategory') return Tag
  if (value === 'all') return CircleHelp
  if (value === 'TICKET') return Ticket
  if (value === 'POKEMON_CARD') return Tag
  if (value === 'OTHER') return CircleHelp
  return Package
}

function iconClass(value) {
  if (value === 'all') return 'is-all'
  return `is-${String(value || 'empty').toLowerCase().replace(/[^a-z0-9]+/g, '-')}`
}

function updateMenuPosition() {
  if (!menuOpen.value || !rootEl.value || typeof window === 'undefined') return

  const viewportWidth = window.innerWidth
  const viewportHeight = window.innerHeight
  const rootRect = rootEl.value.getBoundingClientRect()
  const viewportPadding = 8
  const menuGap = 6
  const naturalHeight =
    menuEl.value?.scrollHeight ?? Math.min(Math.max(safeOptions.value.length * 52, 168), 320)
  const preferredHeight = Math.min(308, naturalHeight)
  const preferredWidth = Math.max(
    rootRect.width,
    props.iconMode === 'subcategory' ? 520 : 460,
  )
  const width = Math.min(preferredWidth, viewportWidth - viewportPadding * 2)
  const left = Math.min(
    Math.max(viewportPadding, rootRect.left),
    viewportWidth - width - viewportPadding,
  )
  const spaceBelow = viewportHeight - rootRect.bottom - menuGap - viewportPadding
  const spaceAbove = rootRect.top - menuGap - viewportPadding
  let shouldOpenUpward = false
  if (props.menuPlacement === 'up') {
    shouldOpenUpward = spaceAbove >= 140 || spaceAbove >= spaceBelow
  } else if (props.menuPlacement === 'down') {
    shouldOpenUpward = false
  } else {
    shouldOpenUpward = spaceBelow < Math.min(preferredHeight, 220) && spaceAbove > spaceBelow
  }
  const availableHeight = Math.max(
    124,
    Math.min(320, shouldOpenUpward ? spaceAbove : spaceBelow),
  )
  const renderedHeight = Math.min(preferredHeight, availableHeight)
  const top = shouldOpenUpward
    ? Math.max(viewportPadding, rootRect.top - renderedHeight - menuGap)
    : Math.min(viewportHeight - viewportPadding - renderedHeight, rootRect.bottom + menuGap)

  openUpward.value = shouldOpenUpward
  menuStyles.value = {
    left: `${Math.round(left)}px`,
    top: `${Math.round(top)}px`,
    width: `${Math.round(width)}px`,
    maxHeight: `${Math.round(availableHeight)}px`,
  }
}

function onDocumentPointerDown(event) {
  if (!menuOpen.value) return
  if (rootEl.value?.contains(event.target)) return
  if (menuEl.value?.contains(event.target)) return
  menuOpen.value = false
}

function onDocumentKeydown(event) {
  if (event.key === 'Escape') menuOpen.value = false
}

function onViewportChange() {
  updateMenuPosition()
}

watch(menuOpen, async (isOpen) => {
  if (!isOpen) {
    openUpward.value = false
    menuStyles.value = {}
    return
  }

  await nextTick()
  updateMenuPosition()
  window.requestAnimationFrame(() => {
    updateMenuPosition()
  })
})

onMounted(() => {
  document.addEventListener('pointerdown', onDocumentPointerDown, true)
  document.addEventListener('keydown', onDocumentKeydown)
  window.addEventListener('resize', onViewportChange)
  document.addEventListener('scroll', onViewportChange, true)
})

onBeforeUnmount(() => {
  document.removeEventListener('pointerdown', onDocumentPointerDown, true)
  document.removeEventListener('keydown', onDocumentKeydown)
  window.removeEventListener('resize', onViewportChange)
  document.removeEventListener('scroll', onViewportChange, true)
})
</script>

<style scoped>
.filter-choice {
  position: relative;
  display: block;
  min-width: 0;
  border: 1px solid rgba(100, 116, 139, 0.24);
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.9);
  color: #0f172a;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.8);
  transition:
    border-color 140ms ease,
    background 140ms ease,
    box-shadow 140ms ease;
}

.filter-choice:hover,
.filter-choice.is-open {
  border-color: rgba(20, 184, 166, 0.42);
  background: rgba(241, 245, 249, 0.96);
  box-shadow: 0 0 0 3px rgba(45, 212, 191, 0.12);
}

.filter-choice.is-open {
  z-index: 160;
}

.filter-choice.is-disabled {
  opacity: 0.72;
}

.filter-choice__label {
  color: #0f766e;
  font-size: 0.54rem;
  font-weight: 900;
  letter-spacing: 0.07em;
  line-height: 1;
  text-transform: uppercase;
}

.filter-choice__trigger {
  display: grid;
  min-width: 0;
  width: 100%;
  gap: 0.28rem;
  color: #0f172a;
  text-align: left;
  padding: 0.46rem 0.62rem 0.5rem;
}

.filter-choice__trigger-row {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  min-width: 0;
  min-height: 1.9rem;
  align-items: center;
  gap: 0.48rem;
}

.filter-choice__trigger:disabled {
  cursor: not-allowed;
}

.filter-choice__icon,
.filter-choice__option-icon {
  display: inline-grid;
  flex: 0 0 auto;
  place-items: center;
  border: 1px solid rgba(20, 184, 166, 0.28);
  background: #ecfdf5;
  color: #0f766e;
}

.filter-choice__icon {
  width: 1.4rem;
  height: 1.4rem;
  border-radius: 0.5rem;
}

.filter-choice__option-icon {
  width: 1.72rem;
  height: 1.72rem;
  border-radius: 0.56rem;
}

.filter-choice__icon.is-all,
.filter-choice__option-icon.is-all {
  border-color: rgba(14, 116, 144, 0.24);
  background: #eff6ff;
  color: #0e7490;
}

.filter-choice__value {
  min-width: 0;
  overflow: hidden;
  color: #0f172a;
  font-size: 0.82rem;
  font-weight: 850;
  line-height: 1.15;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.filter-choice__value.is-placeholder {
  color: #94a3b8;
}

.filter-choice__chevron {
  color: #475569;
  transition: transform 140ms ease;
}

.filter-choice.is-open .filter-choice__chevron {
  transform: rotate(180deg);
}

.filter-choice__menu {
  position: fixed;
  z-index: 10020;
  display: grid;
  min-width: 0;
  grid-template-columns: 1fr;
  gap: 0.5rem;
  align-content: start;
  overflow-y: auto;
  overscroll-behavior: contain;
  border: 1px solid rgba(203, 213, 225, 0.9);
  border-radius: 0.9rem;
  background: #ffffff;
  padding: 0.62rem;
  box-shadow: 0 18px 44px rgba(15, 23, 42, 0.14);
  scrollbar-width: thin;
  scrollbar-color: rgba(15, 118, 110, 0.42) rgba(241, 245, 249, 0.9);
  transform-origin: top left;
}

.filter-choice__menu.is-type {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.filter-choice__menu.is-subcategory {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.filter-choice__menu.opens-upward {
  transform-origin: bottom left;
}

.filter-choice__menu::-webkit-scrollbar {
  width: 7px;
}

.filter-choice__menu::-webkit-scrollbar-track {
  border-radius: 999px;
  background: rgba(241, 245, 249, 0.9);
}

.filter-choice__menu::-webkit-scrollbar-thumb {
  border-radius: 999px;
  background: rgba(15, 118, 110, 0.42);
}

.filter-choice__option {
  display: flex;
  min-width: 0;
  min-height: 2.6rem;
  align-items: center;
  gap: 0.62rem;
  border: 1px solid rgba(203, 213, 225, 0.76);
  border-radius: 0.74rem;
  background: #ffffff;
  padding: 0.5rem 0.62rem;
  text-align: left;
  transition:
    border-color 140ms ease,
    background 140ms ease,
    box-shadow 140ms ease;
}

.filter-choice__option:hover,
.filter-choice__option.is-active {
  border-color: rgba(20, 184, 166, 0.5);
  background: #ecfdf5;
}

.filter-choice__option.is-active {
  box-shadow: inset 0 0 0 1px rgba(20, 184, 166, 0.22);
}

.filter-choice__option-text {
  display: grid;
  min-width: 0;
  gap: 0.02rem;
  flex: 1;
}

.filter-choice__option-text span {
  min-width: 0;
  color: #0f172a;
  font-size: 0.84rem;
  font-weight: 850;
  line-height: 1.14;
  display: -webkit-box;
  overflow: hidden;
  text-overflow: ellipsis;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
}

.filter-choice__option-text small {
  min-width: 0;
  overflow: hidden;
  color: #64748b;
  font-size: 0.67rem;
  font-weight: 750;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.filter-choice__check {
  flex: 0 0 auto;
  color: #0f766e;
}

@media (max-width: 760px) {
  .filter-choice__menu {
    width: min(100%, calc(100vw - 2rem));
    grid-template-columns: 1fr;
  }

  .filter-choice__option {
    min-height: 2.5rem;
  }
}
</style>
