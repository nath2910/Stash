<template>
  <label class="cd-root" :class="{ 'is-light': light }" :data-size="size">
    <span v-if="label" class="cd-label">{{ label }}</span>
    <VueDatePicker
      v-model="localValue"
      :time-picker="false"
      :time-config="timeConfig"
      :dark="!light"
      :auto-apply="true"
      :teleport="true"
      :clearable="false"
      :hide-input-icon="true"
      :text-input="false"
      :hide-navigation="['time']"
      :formats="formats"
      :min-date="minDateValue"
      :max-date="maxDateValue"
      :locale="localeFr"
      :week-start="1"
      :floating="floating"
      :six-weeks="false"
      model-type="yyyy-MM-dd"
      class="cd-picker"
    >
      <template #trigger="{ openMenu }">
        <button type="button" class="cd-input cd-input--btn" @click="openMenu()">
          {{ displayValue || '--' }}
        </button>
      </template>
    </VueDatePicker>
  </label>
</template>

<script setup>
import { computed } from 'vue'
import { VueDatePicker } from '@vuepic/vue-datepicker'
import '@vuepic/vue-datepicker/dist/main.css'
import fr from 'date-fns/locale/fr'

const props = defineProps({
  modelValue: { type: String, default: '' },
  label: { type: String, default: '' },
  size: { type: String, default: 'sm' },
  minDate: { type: String, default: '' },
  maxDate: { type: String, default: '' },
  light: { type: Boolean, default: false },
})
const emit = defineEmits(['update:modelValue'])
const light = computed(() => props.light)

const localValue = computed({
  get() {
    return props.modelValue || null
  },
  set(v) {
    emit('update:modelValue', toYmd(v))
  },
})

function toYmd(val) {
  if (!val) return ''
  if (val instanceof Date) {
    const pad = (n) => String(n).padStart(2, '0')
    return `${val.getFullYear()}-${pad(val.getMonth() + 1)}-${pad(val.getDate())}`
  }
  const s = String(val)
  if (/^\d{4}-\d{2}-\d{2}$/.test(s)) return s
  const m = s.match(/^(\d{2})\/(\d{2})\/(\d{4})$/)
  if (m) {
    const [, dd, mm, yyyy] = m
    return `${yyyy}-${mm}-${dd}`
  }
  return ''
}

function formatDisplay(date) {
  if (!date) return ''
  const d = typeof date === 'string' ? new Date(date) : date
  if (Number.isNaN(d.getTime())) return ''
  const pad = (n) => String(n).padStart(2, '0')
  return `${pad(d.getDate())}/${pad(d.getMonth() + 1)}/${d.getFullYear()}`
}
const displayValue = computed(() => formatDisplay(props.modelValue))
const formats = { input: formatDisplay, preview: formatDisplay }
const timeConfig = {
  enableTimePicker: false,
  enableSeconds: false,
  enableMinutes: false,
  timePickerInline: false,
}
const localeFr = fr
const floating = { placement: 'bottom-start', offset: 14 }

function parseYmdLocal(ymd) {
  if (!ymd) return null
  const [y, m, d] = String(ymd).split('-').map(Number)
  if (!y || !m || !d) return null
  return new Date(y, m - 1, d)
}

const minDateValue = computed(() => parseYmdLocal(props.minDate))
const maxDateValue = computed(() => parseYmdLocal(props.maxDate))
</script>

<style scoped>
.cd-root {
  display: grid;
  gap: 4px;
}
.cd-label {
  font-size: 0.55rem;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: rgba(148, 163, 184, 0.78);
}
.cd-input {
  height: 26px;
  width: 100%;
  padding: 0 8px;
  border-radius: 8px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  background: rgba(255, 255, 255, 0.05);
  color: rgba(255, 255, 255, 0.9);
  font-size: 0.72rem;
  font-weight: 600;
  letter-spacing: 0.01em;
  transition:
    border-color 160ms ease,
    background 160ms ease,
    box-shadow 160ms ease;
}
.cd-input--btn {
  text-align: left;
  cursor: pointer;
}
.cd-picker :deep(.dp__input_wrap) {
  gap: 0;
}
.cd-picker :deep(.dp__input_icon),
.cd-picker :deep(.dp__clear_icon) {
  display: none !important;
}
.cd-input::placeholder {
  color: rgba(148, 163, 184, 0.7);
}
.cd-input:hover {
  border-color: rgba(148, 163, 184, 0.45);
  background: rgba(255, 255, 255, 0.08);
}
.cd-input:focus {
  border-color: rgba(14, 165, 233, 0.6);
  box-shadow: 0 0 0 3px rgba(14, 165, 233, 0.18);
}
.cd-root[data-size='md'] .cd-input {
  height: 30px;
  font-size: 0.76rem;
}

.cd-root.is-light .cd-label {
  color: #000;
}

.cd-root.is-light .cd-input {
  border-color: rgba(148, 163, 184, 0.48);
  background: rgba(248, 250, 252, 0.96);
  color: #000;
}

.cd-root.is-light .cd-input:hover {
  border-color: rgba(100, 116, 139, 0.62);
  background: rgba(255, 255, 255, 0.98);
}

@media (hover: none) and (pointer: coarse) {
  .cd-label {
    font-size: 0.68rem;
  }
  .cd-input {
    height: 34px;
    padding: 0 10px;
    font-size: 16px;
  }
  .cd-root[data-size='md'] .cd-input {
    height: 38px;
    font-size: 16px;
  }
  :global(.dp__menu) {
    width: min(320px, calc(100vw - 20px));
    min-width: min(320px, calc(100vw - 20px));
  }
  :global(.dp__calendar_header_item),
  :global(.dp__cell_inner) {
    font-size: 0.85rem;
  }
  :global(.dp__cell_inner) {
    width: 32px;
    height: 32px;
  }
}

:global(.dp__menu) {
  border-radius: 14px;
  border: 1px solid rgba(255, 255, 255, 0.14);
  background: rgba(8, 10, 16, 0.92);
  backdrop-filter: blur(18px) saturate(140%);
  box-shadow: 0 18px 50px rgba(0, 0, 0, 0.5);
  margin-top: 8px;
  z-index: 9999;
  width: min(220px, calc(100vw - 20px));
  min-width: min(220px, calc(100vw - 20px));
}
:global(.dp__arrow_top),
:global(.dp__arrow_bottom) {
  display: none !important;
}
:global(.dp__calendar_header_item),
:global(.dp__cell_inner) {
  font-size: 0.65rem;
}
:global(.dp__month_year_row) {
  padding: 4px 6px 2px;
}
:global(.dp__calendar_header) {
  padding: 2px 6px;
}
:global(.dp__calendar_header_separator) {
  margin: 2px 6px 6px;
}
:global(.dp__calendar) {
  gap: 1px;
}
:global(.dp__cell_inner) {
  width: 24px;
  height: 24px;
}
:global(.dp__menu_inner) {
  padding: 4px 6px 6px;
}
.cd-picker :deep(.dp__calendar_header),
.cd-picker :deep(.dp__month_year_row),
.cd-picker :deep(.dp__time_input),
.cd-picker :deep(.dp__action_row) {
  background: transparent;
}
.cd-picker :deep(.dp__action_row),
.cd-picker :deep(.dp__action_buttons) {
  display: none;
}
.cd-picker :deep(.dp__time_input),
.cd-picker :deep(.dp__time_icon),
.cd-picker :deep(.dp__clock_icon),
.cd-picker :deep(.dp__time_picker),
.cd-picker :deep(.dp__time_col),
.cd-picker :deep(.dp__time_display),
.cd-picker :deep(.dp__icon_clock),
.cd-picker :deep(.dp__hours),
.cd-picker :deep(.dp__minutes),
.cd-picker :deep(.dp__seconds),
.cd-picker :deep(.dp__action_row),
.cd-picker :deep(.dp__action_buttons),
.cd-picker :deep(.dp__calendar_time),
.cd-picker :deep(.dp__calendar_time_row),
.cd-picker :deep(.dp__time_overlay),
.cd-picker :deep(.dp__time_overlay_container) {
  display: none !important;
}
.cd-picker :deep(.dp__icon) {
  color: rgba(148, 163, 184, 0.8);
}
.cd-picker :deep(.dp__clear_icon) {
  display: none;
}
.cd-picker :deep(.dp__calendar_header_item) {
  color: rgba(226, 232, 240, 0.6);
  font-size: 0.7rem;
  text-transform: lowercase;
}
.cd-picker :deep(.dp__month_year_select) {
  color: rgba(226, 232, 240, 0.9);
  font-size: 0.8rem;
  font-weight: 600;
}
.cd-picker :deep(.dp__arrow_bottom),
.cd-picker :deep(.dp__arrow_top) {
  display: none;
}
.cd-picker :deep(.dp__cell_inner) {
  border-radius: 8px;
  font-size: 0.75rem;
  color: rgba(226, 232, 240, 0.9);
}
.cd-picker :deep(.dp__active_date) {
  background: rgba(14, 165, 233, 0.22);
  border: 1px solid rgba(14, 165, 233, 0.5);
}
.cd-picker :deep(.dp__today) {
  border: 1px dashed rgba(148, 163, 184, 0.5);
}
.cd-picker :deep(.dp__arrow_top) {
  display: none;
}
.cd-picker :deep(.dp__overlay) {
  background: rgba(8, 10, 16, 0.95);
}
</style>
