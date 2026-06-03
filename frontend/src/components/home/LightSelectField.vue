<template>
  <div ref="rootEl" class="light-select-field">
    <span v-if="label" class="select-label">{{ label }}</span>

    <button
      :id="buttonId"
      type="button"
      class="select-trigger"
      :class="{ 'is-open': menuOpen, 'is-placeholder': !modelValue }"
      :disabled="disabled"
      :aria-expanded="menuOpen ? 'true' : 'false'"
      aria-haspopup="listbox"
      @click="toggleMenu"
      @keydown.down.prevent="move(1)"
      @keydown.up.prevent="move(-1)"
      @keydown.enter.prevent="confirmActive"
      @keydown.space.prevent="confirmActive"
      @keydown.esc.prevent="closeMenu"
    >
      <span class="select-value">{{ selectedLabel }}</span>
      <ChevronDown class="select-icon" aria-hidden="true" />
    </button>

    <div v-if="menuOpen" class="select-menu" role="listbox" :aria-labelledby="buttonId">
      <button
        v-for="(option, index) in menuOptions"
        :id="optionId(index)"
        :key="`${option.value}-${index}`"
        type="button"
        class="select-option"
        :class="{
          'is-active': option.value === modelValue,
          'is-focused': index === activeIndex,
          'is-empty': option.empty,
        }"
        role="option"
        :aria-selected="option.value === modelValue ? 'true' : 'false'"
        @mouseenter="activeIndex = index"
        @click="selectValue(option.value)"
      >
        <span class="option-main">
          <span class="option-label">{{ option.label }}</span>
          <span v-if="option.meta" class="option-meta">{{ option.meta }}</span>
        </span>
        <Check v-if="option.value === modelValue" class="check-icon" aria-hidden="true" />
      </button>

      <p v-if="!menuOptions.length" class="select-empty">Aucune option disponible</p>
    </div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { Check, ChevronDown } from 'lucide-vue-next'

const props = defineProps({
  modelValue: { type: String, default: '' },
  label: { type: String, default: '' },
  options: { type: Array, default: () => [] },
  placeholder: { type: String, default: 'Choisir' },
  emptyLabel: { type: String, default: 'Aucune selection' },
  clearable: { type: Boolean, default: false },
  disabled: { type: Boolean, default: false },
})

const emit = defineEmits(['update:modelValue'])

const rootEl = ref(null)
const menuOpen = ref(false)
const activeIndex = ref(-1)
const buttonId = `light-select-${Math.random().toString(16).slice(2)}`
const optionId = (index) => `${buttonId}-option-${index}`

const normalizedOptions = computed(() =>
  props.options
    .map((option) => {
      if (typeof option === 'string') {
        return { value: option, label: option, meta: '' }
      }
      const value = String(option?.value ?? option?.label ?? '')
      return {
        value,
        label: String(option?.label ?? value),
        meta: String(option?.meta ?? ''),
      }
    })
    .filter((option) => option.value),
)

const menuOptions = computed(() => {
  const options = normalizedOptions.value
  if (!props.clearable) return options
  return [{ value: '', label: props.emptyLabel, meta: '', empty: true }, ...options]
})

const selectedLabel = computed(() => {
  if (!props.modelValue) return props.placeholder
  return (
    normalizedOptions.value.find((option) => option.value === props.modelValue)?.label ||
    props.modelValue
  )
})

watch(menuOpen, (open) => {
  if (!open) {
    activeIndex.value = -1
    return
  }
  const selectedIndex = menuOptions.value.findIndex((option) => option.value === props.modelValue)
  activeIndex.value = selectedIndex >= 0 ? selectedIndex : 0
})

function toggleMenu() {
  if (props.disabled) return
  menuOpen.value = !menuOpen.value
}

function closeMenu() {
  menuOpen.value = false
}

function move(direction) {
  if (!menuOpen.value) {
    menuOpen.value = true
    return
  }
  if (!menuOptions.value.length) return
  const max = menuOptions.value.length - 1
  activeIndex.value += direction
  if (activeIndex.value < 0) activeIndex.value = max
  if (activeIndex.value > max) activeIndex.value = 0
}

function confirmActive() {
  if (!menuOpen.value) {
    menuOpen.value = true
    return
  }
  const option = menuOptions.value[activeIndex.value]
  if (option) selectValue(option.value)
}

function selectValue(value) {
  emit('update:modelValue', value)
  closeMenu()
}

function onDocumentPointerDown(event) {
  if (!menuOpen.value) return
  if (rootEl.value?.contains(event.target)) return
  closeMenu()
}

onMounted(() => {
  document.addEventListener('pointerdown', onDocumentPointerDown, true)
})

onBeforeUnmount(() => {
  document.removeEventListener('pointerdown', onDocumentPointerDown, true)
})
</script>

<style scoped>
.light-select-field {
  position: relative;
  display: grid;
  width: 100%;
  max-width: 100%;
  box-sizing: border-box;
  min-width: 0;
  gap: 0.3rem;
}

.select-label {
  color: #334155;
  font-size: 0.76rem;
  font-weight: 800;
}

.select-trigger {
  display: flex;
  width: 100%;
  max-width: 100%;
  box-sizing: border-box;
  min-width: 0;
  min-height: 40px;
  align-items: center;
  justify-content: space-between;
  gap: 0.65rem;
  border: 1px solid rgba(100, 116, 139, 0.24);
  border-radius: 12px;
  background: rgba(248, 250, 252, 0.88);
  color: #0f172a;
  padding: 0.5rem 0.72rem;
  text-align: left;
  font-size: 0.88rem;
  font-weight: 750;
  outline: none;
  transition:
    background 140ms ease,
    border-color 140ms ease,
    box-shadow 140ms ease;
}

.select-trigger:hover {
  border-color: rgba(14, 165, 233, 0.34);
  background: #ffffff;
}

.select-trigger:focus,
.select-trigger.is-open {
  border-color: rgba(20, 184, 166, 0.58);
  background: #ffffff;
  box-shadow: 0 0 0 4px rgba(20, 184, 166, 0.12), 0 10px 24px rgba(14, 165, 233, 0.08);
}

.select-trigger.is-placeholder {
  color: #94a3b8;
}

.select-trigger:disabled {
  cursor: not-allowed;
  opacity: 0.58;
}

.select-value {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.select-icon {
  width: 1rem;
  height: 1rem;
  flex: 0 0 auto;
  color: #475569;
  transition: transform 140ms ease;
}

.select-trigger.is-open .select-icon {
  transform: rotate(180deg);
}

.select-menu {
  position: absolute;
  left: 0;
  right: 0;
  top: calc(100% + 0.45rem);
  z-index: 70;
  width: 100%;
  max-width: 100%;
  box-sizing: border-box;
  max-height: 16rem;
  overflow-y: auto;
  border: 1px solid rgba(125, 211, 252, 0.34);
  border-radius: 14px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.99), rgba(248, 250, 252, 0.97)),
    #ffffff;
  padding: 0.35rem;
  box-shadow:
    0 18px 46px rgba(15, 23, 42, 0.16),
    0 1px 0 rgba(255, 255, 255, 0.8) inset;
  backdrop-filter: blur(12px);
}

.select-option {
  display: flex;
  width: 100%;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  border-radius: 10px;
  padding: 0.62rem 0.7rem;
  text-align: left;
  color: #0f172a;
  transition:
    background 120ms ease,
    color 120ms ease;
}

.select-option:hover,
.select-option.is-focused {
  background: linear-gradient(135deg, #f0fdfa, #eff6ff);
  color: #075985;
}

.select-option.is-active {
  background: linear-gradient(135deg, #ccfbf1, #dbeafe);
  color: #0f766e;
}

.select-option.is-empty {
  color: #64748b;
}

.option-main {
  display: grid;
  min-width: 0;
  gap: 0.05rem;
}

.option-label {
  overflow: hidden;
  font-size: 0.88rem;
  font-weight: 800;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.option-meta {
  overflow: hidden;
  color: #64748b;
  font-size: 0.7rem;
  font-weight: 650;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.check-icon {
  width: 0.95rem;
  height: 0.95rem;
  flex: 0 0 auto;
  color: #0f766e;
}

.select-empty {
  padding: 0.9rem 0.75rem;
  color: #64748b;
  font-size: 0.82rem;
  font-weight: 700;
  text-align: center;
}
</style>
