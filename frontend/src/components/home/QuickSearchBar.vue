<template>
  <section class="quick-search-panel">
    <div class="search-heading">
      <div>
        <p class="search-eyebrow">Inventaire</p>
        <h1>Accueil</h1>
      </div>
      <span class="search-count">{{ stockLabel }}</span>
    </div>

    <div
      class="search-box"
      @keydown.down.prevent="move(1)"
      @keydown.up.prevent="move(-1)"
      @keydown.enter.prevent="confirmActive"
      @keydown.esc.prevent="close"
    >
      <Search class="search-icon" aria-hidden="true" />
      <input
        ref="inputEl"
        v-model="query"
        type="search"
        autocomplete="off"
        placeholder="Rechercher un item, une categorie, une marque..."
        :aria-expanded="dropdownOpen"
        :aria-activedescendant="activeDescendant"
        aria-label="Rechercher un item"
        aria-controls="home-search-results"
        role="combobox"
        class="search-input"
        @focus="open"
        @blur="scheduleClose"
      />

      <button v-if="query" type="button" class="clear-button" aria-label="Effacer la recherche" @click="reset">
        <X class="h-4 w-4" aria-hidden="true" />
      </button>

      <SearchResultsDropdown
        v-if="dropdownOpen"
        id="home-search-results"
        :items="filteredItems"
        :active-index="activeIndex"
        :loading="loading"
        :empty="showEmpty"
        :option-id="optionId"
        :category-labels="categoryLabels"
        @hover="activeIndex = $event"
        @select="selectItem"
      />
    </div>
  </section>
</template>

<script setup>
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { Search, X } from 'lucide-vue-next'
import SearchResultsDropdown from './SearchResultsDropdown.vue'
import { useAuthStore } from '@/store/authStore'
import { itemTypeLabel, readStoredItemCategories } from '@/RegleItem/itemCategoryStore'
import { formatNumber } from '@/utils/formatters'
import { getField, typeOf } from '@/utils/snkVente'
import { normalizeSearchText } from '@/utils/homeDashboard'

const props = defineProps({
  items: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false },
})

const emit = defineEmits(['select'])

const auth = useAuthStore()
const currentUserId = computed(() => auth.user?.value?.id ?? auth.user?.id ?? 'guest')
const categoryLabels = ref(readStoredItemCategories(currentUserId.value))
const query = ref('')
const debouncedQuery = ref('')
const focused = ref(false)
const activeIndex = ref(-1)
const inputEl = ref(null)
let debounceTimer = null
let closeTimer = null

watch(
  query,
  (value) => {
    window.clearTimeout(debounceTimer)
    debounceTimer = window.setTimeout(() => {
      debouncedQuery.value = normalizeSearchText(value)
      activeIndex.value = -1
    }, 110)
  },
  { immediate: true },
)

const stockLabel = computed(() => {
  if (props.loading) return 'Chargement'
  const count = props.items.length
  return `${formatNumber(count)} item${count > 1 ? 's' : ''}`
})

const searchableText = (item) => {
  const metadata = item?.metadata || {}
  return normalizeSearchText(
    [
      getField(item, 'nomItem', ''),
      getField(item, 'categorie', ''),
      getField(item, 'description', ''),
      itemTypeLabel(typeOf(item), categoryLabels.value),
      typeOf(item),
      metadata.size,
      metadata.sku,
      metadata.reference,
      metadata.model,
      metadata.colorway,
      metadata.condition,
    ].join(' '),
  )
}

const filteredItems = computed(() => {
  const q = debouncedQuery.value
  if (!q) return []
  const tokens = q.split(/\s+/).filter(Boolean)
  return props.items
    .filter((item) => {
      const haystack = searchableText(item)
      return tokens.every((token) => haystack.includes(token))
    })
    .slice(0, 9)
})

const dropdownOpen = computed(() => focused.value && Boolean(query.value.trim()))
const showEmpty = computed(() => Boolean(debouncedQuery.value) && !props.loading && filteredItems.value.length === 0)
const activeDescendant = computed(() => (activeIndex.value >= 0 ? optionId(activeIndex.value) : undefined))
const optionId = (index) => `home-search-option-${index}`

function open() {
  window.clearTimeout(closeTimer)
  focused.value = true
}

function close() {
  window.clearTimeout(closeTimer)
  focused.value = false
  activeIndex.value = -1
  inputEl.value?.blur()
}

function scheduleClose() {
  window.clearTimeout(closeTimer)
  closeTimer = window.setTimeout(() => {
    focused.value = false
    activeIndex.value = -1
  }, 120)
}

function reset() {
  query.value = ''
  debouncedQuery.value = ''
  activeIndex.value = -1
  inputEl.value?.focus()
}

function move(direction) {
  if (!dropdownOpen.value || filteredItems.value.length === 0) return
  const max = filteredItems.value.length - 1
  if (activeIndex.value < 0) {
    activeIndex.value = direction > 0 ? 0 : max
    return
  }
  activeIndex.value = activeIndex.value + direction
  if (activeIndex.value < 0) activeIndex.value = max
  if (activeIndex.value > max) activeIndex.value = 0
}

function confirmActive() {
  if (activeIndex.value < 0) return
  const item = filteredItems.value[activeIndex.value]
  if (item) selectItem(item)
}

function selectItem(item) {
  emit('select', item)
  focused.value = false
  activeIndex.value = -1
}

watch(
  () => currentUserId.value,
  (userId) => {
    categoryLabels.value = readStoredItemCategories(userId)
  },
)

function onCategoryLabelsChange(event) {
  const detail = event?.detail || {}
  if (String(detail.userId || 'guest') !== String(currentUserId.value || 'guest')) return
  categoryLabels.value = readStoredItemCategories(currentUserId.value)
}

if (typeof window !== 'undefined') {
  window.addEventListener('snk:item-categories-change', onCategoryLabelsChange)
}

onBeforeUnmount(() => {
  window.clearTimeout(debounceTimer)
  window.clearTimeout(closeTimer)
  if (typeof window !== 'undefined') {
    window.removeEventListener('snk:item-categories-change', onCategoryLabelsChange)
  }
})
</script>

<style scoped>
.quick-search-panel {
  position: relative;
  display: grid;
  gap: 1rem;
  border: 1px solid rgba(203, 213, 225, 0.72);
  border-radius: 20px;
  background: #ffffff;
  padding: clamp(1rem, 2.2vw, 1.5rem);
  box-shadow: 0 18px 42px rgba(15, 23, 42, 0.07);
}

.quick-search-panel::before {
  content: '';
  position: absolute;
  inset: 0 1rem auto;
  height: 3px;
  border-radius: 999px;
  background: linear-gradient(90deg, #0ea5e9, #14b8a6, #f59e0b);
}

.search-heading {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 1rem;
}

.search-eyebrow {
  color: #0369a1;
  font-size: 0.74rem;
  font-weight: 800;
  text-transform: uppercase;
}

h1 {
  margin-top: 0.2rem;
  color: #0f172a;
  font-size: clamp(1.8rem, 4vw, 3rem);
  font-weight: 800;
}

.search-count {
  border: 1px solid rgba(14, 165, 233, 0.24);
  border-radius: 999px;
  background: linear-gradient(135deg, #e0f2fe, #ecfdf5);
  color: #075985;
  padding: 0.35rem 0.7rem;
  font-size: 0.78rem;
  font-weight: 800;
  white-space: nowrap;
}

.search-box {
  position: relative;
  display: grid;
  align-items: center;
}

.search-icon {
  position: absolute;
  left: 1rem;
  z-index: 1;
  width: 1.2rem;
  height: 1.2rem;
  color: #0ea5e9;
}

.search-input {
  width: 100%;
  min-height: 58px;
  border: 1px solid rgba(100, 116, 139, 0.26);
  border-radius: 16px;
  background: #f8fafc;
  color: #0f172a;
  -webkit-appearance: none;
  appearance: none;
  padding: 0 3rem 0 3rem;
  font-size: 1rem;
  font-weight: 700;
  outline: none;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.9);
  transition:
    border-color 160ms ease,
    background 160ms ease,
    box-shadow 160ms ease;
}

.search-input::placeholder {
  color: #94a3b8;
}

.search-input::-webkit-search-cancel-button,
.search-input::-webkit-search-decoration {
  display: none;
  -webkit-appearance: none;
}

.search-input:focus {
  border-color: rgba(14, 165, 233, 0.58);
  background: #ffffff;
  box-shadow: 0 0 0 4px rgba(14, 165, 233, 0.12), 0 12px 30px rgba(14, 165, 233, 0.08);
}

.clear-button {
  position: absolute;
  right: 0.8rem;
  z-index: 2;
  display: inline-flex;
  width: 2rem;
  height: 2rem;
  align-items: center;
  justify-content: center;
  border: 1px solid rgba(148, 163, 184, 0.28);
  border-radius: 999px;
  background: #ffffff;
  color: #475569;
}

.clear-button:hover {
  border-color: rgba(14, 165, 233, 0.3);
  color: #0369a1;
}

@media (max-width: 560px) {
  .search-heading {
    display: grid;
    align-items: start;
  }

  .search-count {
    justify-self: start;
  }
}
</style>
