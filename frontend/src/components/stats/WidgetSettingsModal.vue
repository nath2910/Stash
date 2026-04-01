<template>
  <TransitionRoot appear :show="open" as="template">
    <Dialog as="div" class="relative z-50" @close="$emit('close')">
      <TransitionChild
        as="template"
        enter="duration-160 ease-out"
        enterFrom="opacity-0"
        enterTo="opacity-100"
        leave="duration-120 ease-in"
        leaveFrom="opacity-100"
        leaveTo="opacity-0"
      >
        <div class="fixed inset-0 bg-slate-950/60 backdrop-blur-[2px]" />
      </TransitionChild>

      <div class="fixed inset-0 overflow-y-auto">
        <div class="flex min-h-full items-center justify-center p-4">
          <TransitionChild
            as="template"
            enter="duration-170 ease-out"
            enterFrom="opacity-0 scale-95 translate-y-1"
            enterTo="opacity-100 scale-100 translate-y-0"
            leave="duration-120 ease-in"
            leaveFrom="opacity-100 scale-100 translate-y-0"
            leaveTo="opacity-0 scale-95 translate-y-1"
          >
            <DialogPanel
              class="glass-panel w-full rounded-[22px]"
              :class="isTextWidgetSettings ? 'max-w-4xl' : 'max-w-lg'"
            >
              <div class="glass-header px-4 py-3 flex items-center">
                <DialogTitle class="text-white font-semibold text-lg">{{ title }}</DialogTitle>
                <button
                  type="button"
                  class="glass-iconbtn ml-auto h-8 w-8 grid place-items-center rounded-xl"
                  aria-label="Fermer les reglages"
                  @click="$emit('close')"
                >
                  <span class="close-x" aria-hidden="true"></span>
                </button>
              </div>

              <div class="settings-body p-4" :class="{ 'settings-body--text': isTextWidgetSettings }">
                <div v-if="!orderedFields.length" class="text-sm text-white/60">
                  Pas de reglages pour ce widget.
                </div>

                <template v-for="f in orderedFields" :key="f.key">
                  <div
                    v-if="!(f.hideWhenGlobalRange && draft.useGlobalRange !== false)"
                    class="setting-card space-y-2"
                    :class="settingCardClass(f)"
                  >
                    <div v-if="f.type !== 'toggle'" class="text-sm text-white/80 font-semibold">{{ f.label }}</div>

                    <div v-if="f.type === 'select' && useTileSelect(f)" class="tile-grid" :class="tileGridClass(f)">
                      <button
                        v-for="o in f.options"
                        :key="o.value"
                        type="button"
                        class="tile-option"
                        :class="{ 'is-active': draft[f.key] === o.value, 'tile-option--font': f.key === 'fontFamily' }"
                        :style="optionStyle(f, o)"
                        @click="draft[f.key] = o.value"
                      >
                        {{ o.label }}
                      </button>
                    </div>
                    <div v-else-if="f.type === 'select'" class="select-wrap">
                      <select v-model="draft[f.key]" class="glass-field select-field w-full h-10 rounded-xl px-3">
                        <option v-for="o in f.options" :key="o.value" :value="o.value">
                          {{ o.label }}
                        </option>
                      </select>
                      <span class="select-chevron" aria-hidden="true"></span>
                    </div>
                    <div v-else-if="f.type === 'multiselect'" class="multi-select">
                      <div class="multi-preview">
                        <template v-if="(draft[f.key] || []).length">
                          <span
                            v-for="tag in draft[f.key]"
                            :key="tag"
                            class="multi-pill"
                          >
                            {{ resolveMultiLabel(f.options, tag) }}
                          </span>
                        </template>
                        <span v-else class="multi-placeholder">{{ f.placeholder || 'Tout' }}</span>
                      </div>
                      <div class="multi-actions">
                        <button
                          type="button"
                          class="multi-btn"
                          @click="clearMulti(f.key)"
                        >
                          Retirer filtre
                        </button>
                        <button
                          type="button"
                          class="multi-btn"
                          @click="selectAllMulti(f.key, f.options)"
                        >
                          Tout selectionner
                        </button>
                      </div>
                      <div class="multi-list">
                        <label
                          v-for="o in f.options"
                          :key="o.value"
                          class="multi-option"
                        >
                          <input
                            type="checkbox"
                            :checked="Array.isArray(draft[f.key]) && draft[f.key].includes(o.value)"
                            @change="toggleMulti(f.key, o.value)"
                          />
                          <span>{{ o.label }}</span>
                        </label>
                      </div>
                    </div>

                    <div v-else-if="f.type === 'number'" class="number-field">
                      <button
                        type="button"
                        class="glass-iconbtn number-btn"
                        @click="stepNumber(f, -1)"
                      >
                        -
                      </button>
                      <input
                        type="number"
                        v-model.number="draft[f.key]"
                        :min="f.min"
                        :max="f.max"
                        :step="f.step ?? 1"
                        class="glass-field number-input"
                        @blur="clampNumberField(f)"
                      />
                      <button
                        type="button"
                        class="glass-iconbtn number-btn"
                        @click="stepNumber(f, 1)"
                      >
                        +
                      </button>
                      <div v-if="hasNumberMeta(f)" class="number-meta">
                        <span v-if="f.min !== undefined">min {{ f.min }}</span>
                        <span v-if="f.max !== undefined">max {{ f.max }}</span>
                        <span v-if="f.step">pas {{ f.step }}</span>
                        <span v-if="f.unit">{{ f.unit }}</span>
                      </div>
                    </div>

                    <input
                      v-else-if="f.type === 'text'"
                      type="text"
                      v-model="draft[f.key]"
                      class="glass-field w-full h-10 rounded-xl px-3"
                    />
                    <div v-else-if="f.type === 'color'" class="color-row">
                      <input
                        type="color"
                        v-model="draft[f.key]"
                        class="color-field"
                      />
                      <input
                        type="text"
                        v-model="draft[f.key]"
                        class="glass-field color-text"
                      />
                    </div>
                    <CompactDateInput
                      v-else-if="f.type === 'date'"
                      v-model="draft[f.key]"
                      :min-date="minDate"
                      :max-date="maxDate"
                      class="w-full"
                    />
                    <textarea
                      v-else-if="f.type === 'textarea'"
                      v-model="draft[f.key]"
                      rows="4"
                      class="glass-field w-full rounded-xl px-3 py-2"
                    ></textarea>

                    <label
                      v-else-if="f.type === 'toggle'"
                      class="toggle-tile"
                      :class="{ 'is-active': !!draft[f.key] }"
                    >
                      <input type="checkbox" v-model="draft[f.key]" class="sr-only" />
                      <span class="text-sm">{{ f.label }}</span>
                    </label>

                    <div v-if="f.help" class="text-xs text-white/50">{{ f.help }}</div>
                  </div>
                </template>
              </div>

              <div class="glass-footer px-4 py-3 flex justify-end gap-2">
                <button
                  type="button"
                  class="glass-btn px-4 h-10 rounded-xl"
                  @click="$emit('close')"
                >
                  Annuler
                </button>
                <button
                  type="button"
                  class="glass-btn glass-btn-primary px-4 h-10 rounded-xl"
                  @click="onSave"
                >
                  Sauver
                </button>
              </div>
            </DialogPanel>
          </TransitionChild>
        </div>
      </div>
    </Dialog>
  </TransitionRoot>
</template>

<script setup>
import { computed, reactive, watch } from 'vue'
import { Dialog, DialogPanel, DialogTitle, TransitionChild, TransitionRoot } from '@headlessui/vue'
import CompactDateInput from '@/components/ui/CompactDateInput.vue'

const props = defineProps({
  open: { type: Boolean, default: false },
  title: { type: String, default: 'Reglages' },
  fields: { type: Array, default: () => [] },
  model: { type: Object, default: () => ({}) },
  minDate: { type: String, default: '' },
  maxDate: { type: String, default: '' },
})
const emit = defineEmits(['close', 'save'])

const draft = reactive({})

const FONT_PREVIEW = {
  'open-sans': '"Open Sans", Arial, sans-serif',
  'pt-sans': '"PT Sans", Arial, sans-serif',
  'pt-serif': '"PT Serif", Georgia, serif',
  roboto: 'Roboto, Arial, sans-serif',
  'roboto-slab': '"Roboto Slab", Georgia, serif',
  'ibm-plex-sans': '"IBM Plex Sans", Arial, sans-serif',
  'ibm-plex-mono': '"IBM Plex Mono", monospace',
  georgia: 'Georgia, serif',
  arial: 'Arial, sans-serif',
  'permanent-marker': '"Permanent Marker", "Comic Sans MS", cursive',
  caveat: 'Caveat, "Comic Sans MS", cursive',
}

const isTextWidgetSettings = computed(() =>
  (props.fields || []).some((field) =>
    ['fontFamily', 'fontSize', 'weight', 'italic', 'underline', 'color'].includes(field?.key),
  ),
)

const TEXT_FIELD_ORDER = {
  content: 0,
  fontFamily: 1,
  fontSize: 2,
  weight: 3,
  italic: 4,
  underline: 5,
  color: 6,
  align: 7,
  valign: 8,
  padding: 9,
}

const orderedFields = computed(() => {
  const base = Array.isArray(props.fields) ? [...props.fields] : []
  if (!isTextWidgetSettings.value) return base
  return base.sort((a, b) => {
    const rankA = TEXT_FIELD_ORDER[a?.key] ?? 100
    const rankB = TEXT_FIELD_ORDER[b?.key] ?? 100
    if (rankA !== rankB) return rankA - rankB
    return String(a?.label ?? '').localeCompare(String(b?.label ?? ''))
  })
})

function useTileSelect(field) {
  return isTextWidgetSettings.value && ['align', 'valign', 'fontFamily', 'weight', 'padding'].includes(field?.key)
}

function tileGridClass(field) {
  if (field?.key === 'fontFamily') return 'tile-grid--fonts'
  if (field?.key === 'align' || field?.key === 'valign' || field?.key === 'weight') return 'tile-grid--3'
  return 'tile-grid--2'
}

function settingCardClass(field) {
  if (!isTextWidgetSettings.value) return ''
  if (field?.key === 'content') return 'setting-card--wide'
  if (field?.key === 'fontFamily') return 'setting-card--wide'
  return 'setting-card--compact'
}

function optionStyle(field, option) {
  if (field?.key === 'fontFamily') {
    return { fontFamily: FONT_PREVIEW[option?.value] ?? FONT_PREVIEW['open-sans'] }
  }
  return {}
}

function clampNumberField(field) {
  const raw = Number(draft[field.key] ?? 0)
  const min = field.min ?? -Infinity
  const max = field.max ?? Infinity
  const next = Math.min(Math.max(raw, min), max)
  if (!Number.isFinite(next)) return
  draft[field.key] = next
}

function stepNumber(field, direction) {
  const step = Number(field.step ?? 1)
  const current = Number(draft[field.key] ?? 0)
  const next = current + step * direction
  draft[field.key] = next
  clampNumberField(field)
}

function hasNumberMeta(field) {
  return field.min !== undefined || field.max !== undefined || field.step || field.unit
}

function normalizeDraft() {
  for (const field of props.fields || []) {
    if (field?.type !== 'number') continue
    const raw = Number(draft[field.key] ?? 0)
    const min = field.min ?? -Infinity
    const max = field.max ?? Infinity
    const next = Math.min(Math.max(raw, min), max)
    if (Number.isFinite(next)) {
      draft[field.key] = next
    }
  }
}

function onSave() {
  normalizeDraft()
  emit('save', { ...draft })
}

function toggleMulti(key, value) {
  const list = ensureMultiList(key)
  const idx = list.indexOf(value)
  if (idx >= 0) list.splice(idx, 1)
  else list.push(value)
}

function selectAllMulti(key, options) {
  const list = ensureMultiList(key)
  if (!Array.isArray(options) || options.length === 0) return
  const values = options
    .map((o) => o?.value)
    .filter((v) => v !== undefined && v !== null)
  list.splice(0, list.length, ...Array.from(new Set(values)))
}

function clearMulti(key) {
  const list = ensureMultiList(key)
  list.splice(0, list.length)
}

function ensureMultiList(key) {
  if (!Array.isArray(draft[key])) {
    draft[key] = []
  }
  return draft[key]
}

function resolveMultiLabel(options, value) {
  if (!Array.isArray(options)) return value
  const match = options.find((option) => option?.value === value)
  return match?.label ?? value
}

watch(
  () => props.open,
  (isOpen) => {
    if (!isOpen) return
    Object.keys(draft).forEach((k) => delete draft[k])
    Object.assign(draft, props.model || {})
    for (const field of props.fields || []) {
      if (field?.type === 'multiselect' && !Array.isArray(draft[field.key])) {
        const raw = draft[field.key]
        draft[field.key] = typeof raw === 'string' && raw ? [raw] : []
      }
    }
  },
  { immediate: true },
)
</script>

<style scoped>
.close-x {
  position: relative;
  width: 12px;
  height: 12px;
  display: inline-block;
}
.close-x::before,
.close-x::after {
  content: '';
  position: absolute;
  left: 50%;
  top: 50%;
  width: 12px;
  height: 2px;
  background: currentColor;
  transform-origin: center;
}
.close-x::before {
  transform: translate(-50%, -50%) rotate(45deg);
}
.close-x::after {
  transform: translate(-50%, -50%) rotate(-45deg);
}
.glass-panel {
  position: relative;
  overflow: hidden;
  border: 1px solid rgba(148, 163, 184, 0.34);
  background:
    radial-gradient(circle at top right, rgba(59, 130, 246, 0.14), transparent 28%),
    linear-gradient(180deg, rgba(15, 23, 42, 0.98), rgba(15, 23, 42, 0.92));
  box-shadow:
    0 24px 60px rgba(2, 6, 23, 0.62),
    0 1px 0 rgba(255, 255, 255, 0.06) inset;
}
.glass-panel::before {
  display: none;
}
.glass-panel::after {
  display: none;
}
.glass-panel > * {
  position: relative;
  z-index: 1;
}
.glass-header {
  border-bottom: 1px solid rgba(148, 163, 184, 0.22);
  background: rgba(15, 23, 42, 0.96);
}
.glass-footer {
  border-top: 1px solid rgba(148, 163, 184, 0.22);
  background: rgba(15, 23, 42, 0.9);
}
.glass-iconbtn {
  border: 1px solid rgba(148, 163, 184, 0.32);
  background: rgba(15, 23, 42, 0.82);
  color: rgba(241, 245, 249, 0.94);
  transition:
    border-color 130ms ease,
    background 130ms ease;
}
.glass-iconbtn:hover {
  border-color: rgba(96, 165, 250, 0.62);
  background: rgba(30, 41, 59, 0.94);
}
.glass-iconbtn::before {
  display: none;
}
.glass-field {
  border: 1px solid rgba(148, 163, 184, 0.28);
  background: rgba(15, 23, 42, 0.76);
  color: rgba(255, 255, 255, 0.95);
  transition:
    border-color 120ms ease,
    background 120ms ease;
}
.glass-btn {
  border: 1px solid rgba(148, 163, 184, 0.32);
  background: rgba(15, 23, 42, 0.8);
  color: rgba(226, 232, 240, 0.96);
  transition:
    border-color 130ms ease,
    background 130ms ease,
    transform 130ms ease;
}
.glass-btn:hover {
  border-color: rgba(96, 165, 250, 0.64);
  background: rgba(30, 41, 59, 0.94);
  transform: translateY(-1px);
}
.glass-btn::before {
  display: none;
}
.glass-btn-primary {
  border-color: rgba(96, 165, 250, 0.72);
  background: rgba(37, 99, 235, 0.24);
}
.glass-btn-primary::before {
  display: none;
}
.settings-body {
  display: grid;
  gap: 12px;
}
.settings-body--text {
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}
.setting-card {
  padding: 12px;
  border-radius: 14px;
  border: 1px solid rgba(148, 163, 184, 0.2);
  background: rgba(15, 23, 42, 0.52);
}
.setting-card--wide {
  grid-column: 1 / -1;
}
.setting-card--compact {
  min-height: 0;
}
.select-wrap {
  position: relative;
}
.select-field {
  appearance: none;
  -webkit-appearance: none;
  -moz-appearance: none;
  padding-right: 38px;
  cursor: pointer;
  background: rgba(15, 23, 42, 0.8);
}
.select-field:focus {
  outline: none;
  box-shadow:
    0 0 0 1px rgba(96, 165, 250, 0.72),
    0 0 0 3px rgba(96, 165, 250, 0.22);
}
.select-chevron {
  position: absolute;
  right: 12px;
  top: 50%;
  width: 10px;
  height: 10px;
  transform: translateY(-50%) rotate(45deg);
  border-right: 2px solid rgba(226, 232, 240, 0.8);
  border-bottom: 2px solid rgba(226, 232, 240, 0.8);
  pointer-events: none;
}
.multi-select {
  display: grid;
  gap: 8px;
}
.multi-actions {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
}
.multi-btn {
  height: 28px;
  padding: 0 10px;
  border-radius: 10px;
  border: 1px solid rgba(148, 163, 184, 0.3);
  background: rgba(15, 23, 42, 0.78);
  color: rgba(226, 232, 240, 0.85);
  font-size: 11px;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  transition:
    border-color 120ms ease,
    background 120ms ease;
}
.multi-btn:hover {
  border-color: rgba(96, 165, 250, 0.62);
  background: rgba(30, 41, 59, 0.94);
}
.multi-preview {
  min-height: 34px;
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  padding: 6px 8px;
  border-radius: 12px;
  border: 1px solid rgba(148, 163, 184, 0.24);
  background: rgba(15, 23, 42, 0.78);
}
.multi-pill {
  padding: 4px 8px;
  border-radius: 999px;
  font-size: 11px;
  color: rgba(226, 232, 240, 0.9);
  border: 1px solid rgba(148, 163, 184, 0.35);
  background: rgba(148, 163, 184, 0.12);
}
.multi-placeholder {
  font-size: 12px;
  color: rgba(226, 232, 240, 0.45);
}
.multi-list {
  max-height: none;
  overflow: visible;
  padding: 6px;
  border-radius: 12px;
  border: 1px solid rgba(148, 163, 184, 0.24);
  background: rgba(15, 23, 42, 0.74);
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 6px 10px;
}
.multi-list .multi-option {
  min-height: 28px;
}
.multi-list .multi-option span {
  line-height: 1.1;
}
@media (min-width: 520px) {
  .multi-list {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}
.multi-option {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: rgba(226, 232, 240, 0.85);
  padding: 6px 6px;
  border-radius: 10px;
  border: 1px solid transparent;
}
.multi-option:hover {
  border-color: rgba(148, 163, 184, 0.4);
  background: rgba(30, 41, 59, 0.56);
}
.multi-option input {
  accent-color: #60a5fa;
}
.number-field {
  display: grid;
  grid-template-columns: 34px 1fr 34px;
  align-items: center;
  gap: 8px;
}
.number-input {
  height: 40px;
  border-radius: 12px;
  padding: 0 12px;
  text-align: center;
  font-weight: 600;
  letter-spacing: 0.02em;
}
.number-btn {
  height: 40px;
  width: 34px;
  border-radius: 12px;
  font-weight: 700;
  font-size: 16px;
}
.number-meta {
  grid-column: 1 / -1;
  display: flex;
  gap: 10px;
  font-size: 11px;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  color: rgba(226, 232, 240, 0.5);
}
.tile-grid {
  display: grid;
  gap: 10px;
}
.tile-grid--2 {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}
.tile-grid--3 {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}
.tile-grid--fonts {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}
.tile-option {
  min-height: 52px;
  padding: 10px 10px;
  border-radius: 12px;
  border: 1px solid rgba(148, 163, 184, 0.24);
  background: rgba(15, 23, 42, 0.7);
  color: rgba(241, 245, 249, 0.92);
  font-size: 0.92rem;
  font-weight: 650;
  text-align: center;
  transition:
    border-color 130ms ease,
    background 130ms ease,
    transform 120ms ease;
}
.tile-option:hover {
  border-color: rgba(96, 165, 250, 0.58);
  background: rgba(30, 41, 59, 0.92);
  transform: translateY(-1px);
}
.tile-option.is-active {
  border-color: rgba(96, 165, 250, 0.82);
  background: rgba(37, 99, 235, 0.22);
}
.tile-option--font {
  justify-content: center;
  text-wrap: balance;
}
.toggle-tile {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 54px;
  padding: 0 14px;
  border-radius: 12px;
  border: 1px solid rgba(148, 163, 184, 0.24);
  background: rgba(15, 23, 42, 0.72);
  color: rgba(241, 245, 249, 0.9);
  cursor: pointer;
  transition:
    border-color 120ms ease,
    background 120ms ease,
    transform 120ms ease;
}
.toggle-tile:hover {
  border-color: rgba(96, 165, 250, 0.58);
  transform: translateY(-1px);
}
.toggle-tile.is-active {
  border-color: rgba(96, 165, 250, 0.82);
  background: rgba(37, 99, 235, 0.22);
}
.color-field {
  width: 56px;
  height: 36px;
  padding: 0;
  border: 1px solid rgba(148, 163, 184, 0.3);
  border-radius: 12px;
  background: transparent;
  overflow: hidden;
}
.color-row {
  display: flex;
  align-items: center;
  gap: 12px;
}
.color-text {
  width: 100%;
  height: 36px;
  border-radius: 12px;
  padding: 0 12px;
  font-size: 0.92rem;
}
.color-field::-webkit-color-swatch-wrapper {
  padding: 0;
}
.color-field::-webkit-color-swatch {
  border: none;
  border-radius: 10px;
}
.color-field::-moz-color-swatch {
  border: none;
  border-radius: 10px;
}
.glass-iconbtn:focus-visible,
.glass-btn:focus-visible,
.glass-field:focus-visible,
.tile-option:focus-visible,
.multi-btn:focus-visible,
.toggle-tile:focus-within {
  outline: 2px solid rgba(96, 165, 250, 0.96);
  outline-offset: 2px;
}
@media (max-width: 860px) {
  .settings-body--text {
    grid-template-columns: 1fr;
  }
  .tile-grid--3,
  .tile-grid--fonts {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
