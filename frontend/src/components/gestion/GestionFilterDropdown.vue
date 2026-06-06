<template>
  <div
    ref="rootEl"
    class="filter-choice"
    :class="{ 'is-open': menuOpen, 'is-disabled': disabled }"
  >
    <span class="filter-choice__label">{{ label }}</span>
    <button
      type="button"
      class="filter-choice__trigger"
      :disabled="disabled"
      :aria-expanded="menuOpen ? 'true' : 'false'"
      @click="toggleMenu"
    >
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
    </button>

    <div v-if="menuOpen" class="filter-choice__menu">
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
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { Check, ChevronDown, CircleHelp, Package, Tag, Ticket } from 'lucide-vue-next'

const props = defineProps({
  modelValue: { type: String, default: '' },
  label: { type: String, required: true },
  placeholder: { type: String, default: 'Choisir' },
  options: { type: Array, default: () => [] },
  disabled: { type: Boolean, default: false },
  iconMode: { type: String, default: 'type' },
})

const emit = defineEmits(['update:modelValue'])

const rootEl = ref(null)
const menuOpen = ref(false)

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

function onDocumentPointerDown(event) {
  if (!menuOpen.value) return
  if (rootEl.value?.contains(event.target)) return
  menuOpen.value = false
}

function onDocumentKeydown(event) {
  if (event.key === 'Escape') menuOpen.value = false
}

onMounted(() => {
  document.addEventListener('pointerdown', onDocumentPointerDown, true)
  document.addEventListener('keydown', onDocumentKeydown)
})

onBeforeUnmount(() => {
  document.removeEventListener('pointerdown', onDocumentPointerDown, true)
  document.removeEventListener('keydown', onDocumentKeydown)
})
</script>

<style scoped>
.filter-choice {
  position: relative;
  display: grid;
  min-width: 0;
  gap: 0.12rem;
  border: 1px solid rgba(100, 116, 139, 0.24);
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.9);
  padding: 0.38rem 0.55rem;
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

.filter-choice.is-disabled {
  opacity: 0.72;
}

.filter-choice__label {
  color: #0f766e;
  font-size: 0.58rem;
  font-weight: 900;
  letter-spacing: 0.08em;
  line-height: 1;
  text-transform: uppercase;
}

.filter-choice__trigger {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  min-width: 0;
  height: 22px;
  align-items: center;
  gap: 0.45rem;
  color: #0f172a;
  text-align: left;
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
  border-radius: 0.48rem;
}

.filter-choice__option-icon {
  width: 1.85rem;
  height: 1.85rem;
  border-radius: 0.65rem;
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
  font-size: 0.84rem;
  font-weight: 850;
  line-height: 1.05;
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
  position: absolute;
  left: 0;
  top: calc(100% + 0.45rem);
  z-index: 120;
  display: grid;
  width: max(100%, min(31rem, calc(100vw - 2rem)));
  max-height: min(360px, calc(100dvh - 10rem));
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.45rem;
  align-content: start;
  overflow-y: auto;
  overscroll-behavior: contain;
  border: 1px solid rgba(203, 213, 225, 0.9);
  border-radius: 1rem;
  background: #ffffff;
  padding: 0.55rem;
  box-shadow: 0 24px 70px rgba(15, 23, 42, 0.18);
  scrollbar-width: thin;
  scrollbar-color: rgba(15, 118, 110, 0.42) rgba(241, 245, 249, 0.9);
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
  min-height: 2.8rem;
  align-items: center;
  gap: 0.6rem;
  border: 1px solid rgba(203, 213, 225, 0.76);
  border-radius: 0.85rem;
  background: #ffffff;
  padding: 0.5rem 0.6rem;
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
  gap: 0.05rem;
  flex: 1;
}

.filter-choice__option-text span {
  min-width: 0;
  overflow: hidden;
  color: #0f172a;
  font-size: 0.84rem;
  font-weight: 850;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.filter-choice__option-text small {
  min-width: 0;
  overflow: hidden;
  color: #64748b;
  font-size: 0.68rem;
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
}
</style>
