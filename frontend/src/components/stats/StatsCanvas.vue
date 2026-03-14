<template>
  <div
    class="canvas-root"
    :data-edit="editMode ? 'true' : 'false'"
    @pointerdown="onCanvasPointerDown"
  >
    <CanvasDock
      v-show="!fullscreenActive"
      :edit-mode="editMode"
      :scale="scale"
      :palette-open="paletteOpen"
      @toggleEdit="toggleEditMode"
      @openPalette="paletteOpen = true"
      @zoomIn="zoomIn"
      @zoomOut="zoomOut"
      @resetZoom="resetZoom"
      @centerView="centerView"
      @resetLayout="resetLayout"
    />

    <!-- Canvas -->
    <div ref="viewportEl" class="viewport">
      <button
        v-if="isCompact && !paletteOpen && !fullscreenActive"
        type="button"
        class="date-toggle"
        :class="{ 'date-toggle--compact': isCompact }"
        :aria-pressed="datePanelOpen"
        @click.stop="datePanelOpen = !datePanelOpen"
      >
        <CalendarRange class="h-4 w-4" />
      </button>
      <div
        class="date-panel"
        :class="{ 'date-panel--compact': isCompact }"
        v-show="!paletteOpen && !fullscreenActive && (!isCompact || datePanelOpen)"
      >
        <div class="date-title">Periode</div>
        <div class="date-row">
          <CompactDateInput
            label="Du"
            :model-value="localFrom"
            :min-date="minDate"
            :max-date="maxDate"
            @update:modelValue="setFrom"
          />
          <CompactDateInput
            label="Au"
            :model-value="localTo"
            :min-date="minDate"
            :max-date="maxDate"
            @update:modelValue="setTo"
          />
        </div>
        <div class="date-actions">
          <button type="button" class="date-chip" @click="preset('month')">Mois</button>
          <button type="button" class="date-chip" @click="preset('ytd')">YTD</button>
          <button type="button" class="date-chip" @click="preset('year')">Annee</button>
        </div>
      </div>
      <div v-if="editMode" class="edit-grid" aria-hidden="true"></div>
      <div
        v-if="snapGuides.x !== null"
        class="snap-guide snap-guide--x"
        :style="{ left: `${snapGuides.x}px` }"
        aria-hidden="true"
      ></div>
      <div
        v-if="snapGuides.y !== null"
        class="snap-guide snap-guide--y"
        :style="{ top: `${snapGuides.y}px` }"
        aria-hidden="true"
      ></div>
      <div ref="boardEl" class="board">
        <WidgetFrame
          v-for="w in widgets"
          :key="w.id"
          :widget="w"
          :edit-mode="editMode"
          :drag-armed="dragArmedId === w.id"
          :comp="getComp(w.type)"
          :from="widgetFrom(w)"
          :to="widgetTo(w)"
          :style="widgetStyle(w)"
          :ref="(c: any) => setWidgetRef(w.id, c)"
          @dragStart="startDrag(w.id, $event)"
          @resizeStart="startResize(w.id, $event.dir, $event.event)"
          @fullscreen-change="onWidgetFullscreenChange"
          @autoResize="autoResize(w.id, $event)"
          @settings="openSettings(w)"
          @remove="removeWidget(w.id)"
        />
      </div>
    </div>

    <!-- Palette -->
    <WidgetPalette
      :open="paletteOpen"
      :groups="paletteGroups"
      @close="paletteOpen = false"
      @add="addWidget"
    />

    <!-- Settings -->
    <WidgetSettingsModal
      :open="settingsOpen"
      :title="settingsTitle"
      :fields="settingsFields"
      :model="settingsModel"
      :min-date="minDate"
      :max-date="maxDate"
      @close="closeSettings"
      @save="applySettings"
    />

    <Transition name="save-toast">
      <div v-if="showSaveToast" class="save-toast" role="status">Layout enregistre</div>
    </Transition>

    <div v-show="!paletteOpen && !fullscreenActive" class="profile-switcher" role="group" aria-label="Profils">
      <button
        v-for="p in PROFILES"
        :key="p.id"
        type="button"
        class="profile-pill"
        :class="{ 'is-active': activeProfile === p.id }"
        @click="switchProfile(p.id)"
      >
        <span class="profile-dot" :style="{ background: profileColors[p.id] ?? p.color }"></span>
        <span class="profile-label">{{ profileNames[p.id] ?? p.label }}</span>
      </button>
      <button type="button" class="profile-edit" @click="openProfileEditor">
        <Paintbrush class="w-4 h-4" />
      </button>
    </div>

    <teleport to="body">
      <div v-if="profileEditorOpen" class="profile-modal" role="dialog" aria-modal="true">
        <div class="profile-backdrop" @click="closeProfileEditor"></div>
        <div class="profile-panel glass-panel rounded-[22px]" @click.stop>
          <div class="glass-header h-11 px-4 flex items-center">
            <div class="text-white/90 font-semibold text-[15px]">Renommer les profils</div>
            <button
              class="glass-iconbtn ml-auto h-8 w-8 grid place-items-center rounded-xl"
              @click="closeProfileEditor"
            >
              <span class="close-x" aria-hidden="true"></span>
            </button>
          </div>
          <div class="p-4 profile-grid">
            <div class="profile-help">
              Renomme tes profils et choisis une couleur visible dans le sélecteur.
            </div>

            <section class="profile-card">
              <div class="profile-head">
                <div class="profile-title">Profil 1</div>
                <div class="profile-preview">
                  <span class="profile-dot" :style="{ background: profileDraft.p1Color }"></span>
                  <span class="profile-preview-name">{{ profileDraft.p1 || 'Profil 1' }}</span>
                </div>
              </div>
              <div class="profile-row">
                <input v-model="profileDraft.p1" class="glass-field h-11 rounded-xl px-3" />
                <input v-model="profileDraft.p1Color" type="color" class="color-swatch is-round" />
              </div>
            </section>

            <section class="profile-card">
              <div class="profile-head">
                <div class="profile-title">Profil 2</div>
                <div class="profile-preview">
                  <span class="profile-dot" :style="{ background: profileDraft.p2Color }"></span>
                  <span class="profile-preview-name">{{ profileDraft.p2 || 'Profil 2' }}</span>
                </div>
              </div>
              <div class="profile-row">
                <input v-model="profileDraft.p2" class="glass-field h-11 rounded-xl px-3" />
                <input v-model="profileDraft.p2Color" type="color" class="color-swatch is-round" />
              </div>
            </section>

            <section class="profile-card">
              <div class="profile-head">
                <div class="profile-title">Profil 3</div>
                <div class="profile-preview">
                  <span class="profile-dot" :style="{ background: profileDraft.p3Color }"></span>
                  <span class="profile-preview-name">{{ profileDraft.p3 || 'Profil 3' }}</span>
                </div>
              </div>
              <div class="profile-row">
                <input v-model="profileDraft.p3" class="glass-field h-11 rounded-xl px-3" />
                <input v-model="profileDraft.p3Color" type="color" class="color-swatch is-round" />
              </div>
            </section>
          </div>
          <div class="glass-footer px-4 py-3 flex justify-end gap-2">
            <button type="button" class="glass-btn px-4 h-10 rounded-xl" @click.stop="closeProfileEditor">
              Annuler
            </button>
            <button
              type="button"
              class="glass-btn glass-btn-primary px-4 h-10 rounded-xl"
              @click.stop="saveProfileEditor"
            >
              Sauver
            </button>
          </div>
        </div>
      </div>
    </teleport>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, toRefs, watch } from 'vue'
import CanvasDock from './canvas/CanvasDock.vue'
import WidgetFrame from './canvas/WidgetFrame.vue'
import { useCanvasCamera } from './canvas/useCanvaCamera'
import { Paintbrush, CalendarRange } from 'lucide-vue-next'

import CompactDateInput from '@/components/ui/CompactDateInput.vue'
import WidgetPalette from './WidgetPalette.vue'
import WidgetSettingsModal from './WidgetSettingsModal.vue'
import { WIDGET_DEFS, getCategoryColor, getWidgetDef, newWidget } from './widgetRegistry'
import { useAuthStore } from '@/store/authStore'
import StatsServices from '@/services/StatsServices'

type Widget = {
  id: string
  type: string
  title: string
  x: number
  y: number
  w: number
  h: number
  props?: Record<string, unknown>
  z?: number
}

type ResizeDir = 'n' | 's' | 'e' | 'w' | 'ne' | 'nw' | 'se' | 'sw'

type LayoutBundle = {
  version: number
  activeProfile?: string
  profiles: Record<string, Array<Record<string, unknown>>>
  profileNames?: Record<string, string>
  profileColors?: Record<string, string>
}

/* props/emit */
const props = defineProps({
  from: { type: String, required: true },
  to: { type: String, required: true },
})
const emit = defineEmits(['update:from', 'update:to'])
const { from, to } = toRefs(props)

const { user } = useAuthStore()
// Chaque utilisateur a une cle de layout isolee; guest reste en stockage local.
const userId = computed(() => user.value?.id ?? 'guest')

const PROFILE_COLORS = { p1: '#22C55E', p2: '#3B82F6', p3: '#F59E0B' }
const DEFAULT_PROFILE_NAMES = { p1: 'Profil 1', p2: 'Profil 2', p3: 'Profil 3' }
const PROFILES = [
  { id: 'p1', label: DEFAULT_PROFILE_NAMES.p1, color: PROFILE_COLORS.p1 },
  { id: 'p2', label: DEFAULT_PROFILE_NAMES.p2, color: PROFILE_COLORS.p2 },
  { id: 'p3', label: DEFAULT_PROFILE_NAMES.p3, color: PROFILE_COLORS.p3 },
]
const activeProfile = ref('p1')
const layoutBundle = ref<LayoutBundle>({ version: 1, activeProfile: 'p1', profiles: {} })
const profileNames = ref({ ...DEFAULT_PROFILE_NAMES })
const profileColors = ref({ ...PROFILE_COLORS })
const profileEditorOpen = ref(false)
const profileDraft = ref({ ...DEFAULT_PROFILE_NAMES, p1Color: PROFILE_COLORS.p1, p2Color: PROFILE_COLORS.p2, p3Color: PROFILE_COLORS.p3 })

const minDate = ref('')
const maxDate = ref('')
const settingsCategories = ref<string[]>([])
const categoriesCache = new Map<string, string[]>()
const ITEM_TYPE_OPTIONS = [
  { label: 'Sneakers', value: 'SNEAKER' },
  { label: 'Cartes', value: 'POKEMON_CARD' },
  { label: 'Tickets', value: 'TICKET' },
  { label: 'Autres', value: 'OTHER' },
]

const categoryOptions = computed(() =>
  settingsCategories.value.map((c) => ({ label: c, value: c })),
)
const typeOptions = computed(() => ITEM_TYPE_OPTIONS)

/* ===== Mode édition/figé ===== */
// Le mode edition est stocke par utilisateur pour eviter les fuites d'etat UI.
const EDIT_KEY_PREFIX = 'snk_stats_canvas_edit_v1'
const editKey = computed(() => `${EDIT_KEY_PREFIX}_${userId.value}`)
const editMode = ref(true)

function loadEditMode() {
  const raw = localStorage.getItem(editKey.value)
  editMode.value = raw ? raw === 'true' : true
}
loadEditMode()

function persistEditMode() {
  localStorage.setItem(editKey.value, String(editMode.value))
}
function toggleEditMode() {
  editMode.value = !editMode.value
  persistEditMode()
}

/* ===== Dates (local + safe sync) ===== */
const localFrom = ref(from.value)
const localTo = ref(to.value)

watch(
  () => [from.value, to.value],
  () => {
    localFrom.value = from.value
    localTo.value = to.value
  },
)

function setFrom(v: string) {
  const next = clampDate(v)
  if (!next) return
  localFrom.value = next
  emit('update:from', next)
  if (next > localTo.value) {
    localTo.value = next
    emit('update:to', next)
  }
}
function setTo(v: string) {
  const next = clampDate(v)
  if (!next) return
  localTo.value = next
  emit('update:to', next)
  if (next < localFrom.value) {
    localFrom.value = next
    emit('update:from', next)
  }
}

function clampDate(v: string) {
  if (!v) return ''
  let out = v
  if (minDate.value && out < minDate.value) out = minDate.value
  if (maxDate.value && out > maxDate.value) out = maxDate.value
  return out
}

/* ===== Range persist per profile ===== */
const RANGE_KEY_PREFIX = 'snk_stats_range_v1'
const rangeKey = (profileId: string) => `${RANGE_KEY_PREFIX}_${userId.value}_${profileId}`

function loadRangeForProfile(profileId: string) {
  try {
    const raw = localStorage.getItem(rangeKey(profileId))
    if (!raw) return null
    const parsed = JSON.parse(raw)
    const f = typeof parsed?.from === 'string' ? parsed.from : ''
    const t = typeof parsed?.to === 'string' ? parsed.to : ''
    if (!f || !t) return null
    return f <= t ? { from: f, to: t } : { from: t, to: f }
  } catch {
    return null
  }
}

function saveRangeForProfile(profileId: string, f: string, t: string) {
  if (!f || !t) return
  try {
    localStorage.setItem(rangeKey(profileId), JSON.stringify({ from: f, to: t }))
  } catch {
    // ignore
  }
}

function preset(kind: 'month' | 'ytd' | 'year') {
  /**
   * 1) On se met sur "aujourd'hui" à 00:00 (heure locale)
   *    -> évite les décalages quand on formate en YYYY-MM-DD
   */
  const today = new Date()
  today.setHours(0, 0, 0, 0)

  /**
   * 2) Helpers de formatage en "YYYY-MM-DD"
   */
  const pad = (n: number) => String(n).padStart(2, '0')
  const ymd = (d: Date) => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`

  /**
   * 3) Helper: "soustraire N mois" en gardant le jour si possible,
   *    sinon on "clamp" au dernier jour du mois cible.
   *
   *    Exemple:
   *    - 2026-03-31 - 1 mois => 2026-02-28 (ou 29 si bissextile)
   */
  const subMonthsClamp = (date: Date, months: number) => {
    const day = date.getDate()

    // On construit le 1er jour du mois cible.
    // new Date(year, monthIndex, 1) gère automatiquement les passages d'année.
    const firstOfTargetMonth = new Date(date.getFullYear(), date.getMonth() - months, 1)

    // Dernier jour du mois cible: jour 0 du mois suivant
    const lastDayOfTargetMonth = new Date(
      firstOfTargetMonth.getFullYear(),
      firstOfTargetMonth.getMonth() + 1,
      0,
    ).getDate()

    // Clamp du jour pour éviter que JS "déborde" sur le mois suivant
    const clampedDay = Math.min(day, lastDayOfTargetMonth)

    // Date finale (à 00:00)
    return new Date(firstOfTargetMonth.getFullYear(), firstOfTargetMonth.getMonth(), clampedDay)
  }

  /**
   * 4) Helper: "soustraire N années" en gardant mois+jour si possible,
   *    sinon clamp (cas typique: 29/02).
   *
   *    Exemple:
   *    - 2024-02-29 - 1 an => 2023-02-28
   */
  const subYearsClamp = (date: Date, years: number) => {
    const targetYear = date.getFullYear() - years
    const month = date.getMonth()
    const day = date.getDate()

    // Dernier jour du mois dans l'année cible
    const lastDayOfTargetMonth = new Date(targetYear, month + 1, 0).getDate()
    const clampedDay = Math.min(day, lastDayOfTargetMonth)

    return new Date(targetYear, month, clampedDay)
  }

  /**
   * 5) Applique la plage au composant + émet les events
   */
  const applyRange = (fromDate: Date, toDate: Date) => {
    const fromStr = ymd(fromDate)
    const toStr = ymd(toDate)

    localFrom.value = fromStr
    localTo.value = toStr

    emit('update:from', fromStr)
    emit('update:to', toStr)
  }

  /**
   * 6) Presets
   * - month : 1 mois glissant en arrière depuis aujourd'hui
   * - ytd   : du 1er janvier (année courante) à aujourd'hui
   * - year  : 1 an glissant en arrière depuis aujourd'hui
   */
  if (kind === 'month') {
    applyRange(subMonthsClamp(today, 1), today)
  } else if (kind === 'ytd') {
    applyRange(new Date(today.getFullYear(), 0, 1), today)
  } else {
    applyRange(subYearsClamp(today, 1), today)
  }
}

watch(
  () => [from.value, to.value, activeProfile.value],
  () => {
    saveRangeForProfile(activeProfile.value, from.value, to.value)
  },
)

/* ===== Layout ===== */
const GRID = 10
const MIN_W = 320
const MIN_H = 220
const BOARD_W = 9000
const BOARD_H = 6000

const clamp = (n: number, a: number, b: number) => Math.max(a, Math.min(b, n))
const snap = (n: number) => Math.round(n / GRID) * GRID

function minSizeFor(w: Widget) {
  const def = getWidgetDef(w.type)
  return {
    w: def?.minSize?.w ?? MIN_W,
    h: def?.minSize?.h ?? MIN_H,
  }
}

function clampWidget(w: Widget) {
  const minSize = minSizeFor(w)
  w.w = clamp(w.w, minSize.w, BOARD_W)
  w.h = clamp(w.h, minSize.h, BOARD_H)
  w.x = clamp(w.x, 0, BOARD_W - w.w)
  w.y = clamp(w.y, 0, BOARD_H - w.h)
}

// Layout persiste par utilisateur; fallback sur l'ancienne cle globale si presente.
const STORAGE_KEY_PREFIX = 'snk_stats_canvas_layout_v4'
const layoutKey = computed(() => `${STORAGE_KEY_PREFIX}_${userId.value}`)

const widgets = ref<Widget[]>([])

function loadLayout(key: string): unknown | null {
  try {
    const raw = localStorage.getItem(key)
    return raw ? JSON.parse(raw) : null
  } catch {
    return null
  }
}

// Widget d'accueil par defaut quand aucun layout n'existe.
const defaultLayout = (): Widget[] => {
  const def = getWidgetDef('textBlock')
  if (!def) return []

  const w: Widget = {
    id: 'textBlock_welcome',
    type: def.type,
    title: def.title,
    x: (BOARD_W - def.defaultSize.w) / 2,
    y: (BOARD_H - def.defaultSize.h) / 2,
    w: def.defaultSize.w,
    h: def.defaultSize.h,
    props: {
      ...def.defaultProps,
      content:
        'Bienvenue sur ton espace stats. Ajoute des widgets depuis la palette pour composer ton dashboard.',
      align: 'center',
    },
  }

  clampWidget(w)
  return [w]
}

function normalizeLayout(raw: unknown): Widget[] | null {
  if (!Array.isArray(raw)) return null

  const list: Widget[] = []
  for (const item of raw) {
    let type = (item as any)?.type
    let def = getWidgetDef(type)
    if (!def && type === 'textSection') {
      type = 'textBlock'
      def = getWidgetDef(type)
    }
    if (!def) continue

    const legacyTitles: Record<string, string> = {
      sellThrough: 'Sell-through',
      deathPile: 'Death pile',
      topProfitDrivers: 'Top profit (marques/cat)',
    }
    const nextTitle =
      typeof (item as any)?.title === 'string' ? (item as any).title : def.title
    const normalizedTitle =
      legacyTitles[def.type] && nextTitle === legacyTitles[def.type] ? def.title : nextTitle

    const w: Widget = {
      id:
        typeof (item as any)?.id === 'string'
          ? (item as any).id
          : `${def.type}_${Date.now()}_${Math.random().toString(16).slice(2)}`,
      type: def.type,
      title: normalizedTitle,
      x: Number.isFinite((item as any)?.x) ? Number((item as any).x) : 0,
      y: Number.isFinite((item as any)?.y) ? Number((item as any).y) : 0,
      w: Number.isFinite((item as any)?.w) ? Number((item as any).w) : def.defaultSize.w,
      h: Number.isFinite((item as any)?.h) ? Number((item as any).h) : def.defaultSize.h,
      props: { ...def.defaultProps, ...((item as any)?.props ?? {}) },
    }

    if (typeof (w.props as any)?.category === 'string' && !(w.props as any)?.categories) {
      const legacy = String((w.props as any).category || '').trim()
      if (legacy) (w.props as any).categories = [legacy]
      delete (w.props as any).category
    }
    if (typeof (w.props as any)?.type === 'string' && !(w.props as any)?.types) {
      const legacyType = String((w.props as any).type || '').trim()
      if (legacyType) (w.props as any).types = [legacyType]
      delete (w.props as any).type
    }

    if (
      def.type === 'roi' ||
      def.type === 'textTitle' ||
      def.type === 'textBlock' ||
      def.type === 'grossRevenue' ||
      def.type === 'avgMargin' ||
      def.type === 'sellThrough' ||
      def.type === 'topProfitDrivers' ||
      def.type === 'topSales'
    ) {
      w.props = { ...(w.props ?? {}), autoHeight: true }
    }

    clampWidget(w)
    list.push(w)
  }

  if (list.length === 1 && list[0].id === 'textBlock_welcome') {
    const w = list[0]
    w.x = (BOARD_W - w.w) / 2
    w.y = (BOARD_H - w.h) / 2
    clampWidget(w)
  }

  return list
}

function normalizeBundle(raw: unknown): LayoutBundle {
  if (raw && typeof raw === 'object' && !Array.isArray(raw) && (raw as any).profiles) {
    const obj = raw as LayoutBundle
    return {
      version: Number(obj.version || 1),
      activeProfile: typeof obj.activeProfile === 'string' ? obj.activeProfile : 'p1',
      profiles: obj.profiles ?? {},
      profileNames: obj.profileNames ?? {},
      profileColors: obj.profileColors ?? {},
    }
  }

  if (Array.isArray(raw)) {
    return {
      version: 1,
      activeProfile: 'p1',
      profiles: { p1: raw },
      profileNames: {},
      profileColors: {},
    }
  }

  return { version: 1, activeProfile: 'p1', profiles: {}, profileNames: {}, profileColors: {} }
}

function pickProfileId(id?: string) {
  return PROFILES.some((p) => p.id === id) ? (id as string) : 'p1'
}

function applyProfileLayout(bundle: LayoutBundle, profileId: string) {
  const picked = pickProfileId(profileId)
  activeProfile.value = picked
  bundle.activeProfile = picked
  const raw = bundle.profiles?.[picked]
  profileNames.value = { ...DEFAULT_PROFILE_NAMES, ...(bundle.profileNames ?? {}) }
  profileColors.value = { ...PROFILE_COLORS, ...(bundle.profileColors ?? {}) }
  const normalized = normalizeLayout(raw)
  widgets.value = normalized ?? defaultLayout()
}

function loadLayoutForUser() {
  if (userId.value === 'guest') {
    const raw = loadLayout(STORAGE_KEY_PREFIX)
    const bundle = normalizeBundle(raw)
    layoutBundle.value = bundle
    applyProfileLayout(bundle, bundle.activeProfile)
    return
  }
  const raw = loadLayout(layoutKey.value)
  const bundle = normalizeBundle(raw)
  layoutBundle.value = bundle
  applyProfileLayout(bundle, bundle.activeProfile)
  return
}

loadLayoutForUser()

function applyStoredRangeForActiveProfile() {
  const stored = loadRangeForProfile(activeProfile.value)
  if (!stored) return
  localFrom.value = stored.from
  localTo.value = stored.to
  emit('update:from', stored.from)
  emit('update:to', stored.to)
}

let saveTimer: number | null = null
let toastTimer: number | null = null
let remoteSaveTimer: number | null = null
const showSaveToast = ref(false)

function showSavedToast() {
  showSaveToast.value = true
  if (toastTimer) window.clearTimeout(toastTimer)
  toastTimer = window.setTimeout(() => {
    showSaveToast.value = false
    toastTimer = null
  }, 1600)
}

// Chargement du layout serveur pour plus de robustesse (prive / multi-appareil).
async function loadLayoutFromServer() {
  try {
    const res = await StatsServices.getLayout()
    const payload = res?.data?.layout
    if (payload === null || typeof payload === 'undefined') {
      return
    }
    const bundle = normalizeBundle(payload)
    layoutBundle.value = bundle
    applyProfileLayout(bundle, bundle.activeProfile)
    localStorage.setItem(layoutKey.value, JSON.stringify(bundle))
  } catch (err) {
    console.warn('[stats] remote load failed', err)
  }
}

function serializeWidgets() {
  return widgets.value.map(({ id, type, title, x, y, w, h, props }) => ({
    id,
    type,
    title,
    x,
    y,
    w,
    h,
    props,
  }))
}

function saveBundleNow(showToast = true) {
  const bundle = layoutBundle.value
  bundle.activeProfile = activeProfile.value
  bundle.profiles = bundle.profiles ?? {}
  bundle.profiles[activeProfile.value] = serializeWidgets()
  bundle.profileNames = { ...profileNames.value }
  bundle.profileColors = { ...profileColors.value }

  const storageKey = userId.value === 'guest' ? STORAGE_KEY_PREFIX : layoutKey.value
  localStorage.setItem(storageKey, JSON.stringify(bundle))
  if (showToast) showSavedToast()
  scheduleRemoteSave(bundle)
}

function saveLayoutNow() {
  saveBundleNow(true)
}
function scheduleSave() {
  if (saveTimer) window.clearTimeout(saveTimer)
  saveTimer = window.setTimeout(() => {
    saveLayoutNow()
    saveTimer = null
  }, 250)
}

// Sauvegarde distante debounce pour eviter de spammer l'API.
function scheduleRemoteSave(payload: unknown) {
  if (userId.value === 'guest') return
  if (remoteSaveTimer) window.clearTimeout(remoteSaveTimer)
  remoteSaveTimer = window.setTimeout(async () => {
    try {
      await StatsServices.saveLayout(payload)
    } catch (err) {
      console.warn('[stats] remote save failed', err)
    } finally {
      remoteSaveTimer = null
    }
  }, 800)
}

function widgetStyle(w: Widget) {
  return {
    width: `${w.w}px`,
    height: `${w.h}px`,
    transform: `translate3d(${w.x}px, ${w.y}px, 0)`,
    zIndex: w.z ?? 1,
  }
}

/* ===== Widget registry ===== */
const paletteOpen = ref(false)
const PALETTE_ORDER = ['Texte', 'Finance', 'Stock', 'Performance', 'Bonus']
const paletteGroups = computed(() => {
  const grouped = new Map<string, Array<Record<string, unknown>>>()
  for (const w of WIDGET_DEFS) {
    const key = w.category ?? 'Autres'
    const list = grouped.get(key) ?? []
    list.push({
      type: w.type,
      title: w.title,
      icon: w.icon,
      help: w.help ?? 'Ajoute ce widget au canvas',
      forms: w.forms ?? [],
      formPicker: w.formPicker !== false,
    })
    grouped.set(key, list)
  }

  const ordered = PALETTE_ORDER.filter((k) => grouped.has(k)).map((k) => {
    const tone = getCategoryColor(k)
    return {
      title: k,
      color: tone.color,
      glow: tone.glow,
      items: grouped.get(k) ?? [],
    }
  })

  const leftovers = Array.from(grouped.entries())
    .filter(([k]) => !PALETTE_ORDER.includes(k))
    .map(([k, items]) => {
      const tone = getCategoryColor(k)
      return { title: k, color: tone.color, glow: tone.glow, items }
    })

  return [...ordered, ...leftovers]
})
const dragArmedId = ref<string | null>(null)

function disarmWidget() {
  dragArmedId.value = null
}

function getComp(type: string) {
  return getWidgetDef(type)?.component
}

function widgetFrom(w: Widget) {
  const useGlobal = w.props?.useGlobalRange !== false
  return useGlobal ? from.value : (w.props?.from ?? from.value)
}

function widgetTo(w: Widget) {
  const useGlobal = w.props?.useGlobalRange !== false
  return useGlobal ? to.value : (w.props?.to ?? to.value)
}

function widgetCategoryRange(w: Widget) {
  const def = getWidgetDef(w.type)
  const useGlobal = w.props?.useGlobalRange !== false
  if (def?.dateMode === 'asOf') {
    const base = useGlobal
      ? (to.value || from.value)
      : (w.props?.asOf ?? w.props?.to ?? w.props?.from ?? to.value ?? from.value)
    const baseStr = typeof base === 'string' ? base : ''
    return { from: baseStr, to: baseStr }
  }
  return { from: widgetFrom(w), to: widgetTo(w) }
}

/* ===== Settings ===== */
const settingsOpen = ref(false)
const settingsWidgetId = ref<string | null>(null)

const settingsWidget = computed(
  () => widgets.value.find((x) => x.id === settingsWidgetId.value) ?? null,
)
const settingsDef = computed(() =>
  settingsWidget.value ? getWidgetDef(settingsWidget.value.type) : null,
)

const settingsTitle = computed(() => settingsWidget.value?.title ?? 'Réglages')
const settingsFields = computed(() => {
  const def = settingsDef.value
  const base = def?.settings ?? []
  const hideRange = def?.hideGlobalRange === true
  if (hideRange) return [...base]

  const dateMode = def?.dateMode ?? 'range'
  const rangeFields =
    dateMode === 'range'
      ? [
          { key: 'from', label: 'Du', type: 'date', hideWhenGlobalRange: true },
          { key: 'to', label: 'Au', type: 'date', hideWhenGlobalRange: true },
        ]
      : []

  const mappedBase = base.map((f) => {
    if (f?.type === 'date' && dateMode === 'asOf') {
      return { ...f, hideWhenGlobalRange: true }
    }
    return f
  })

  const filterFields = def?.categoryFilter
    ? [
        {
          key: 'categories',
          label: 'Categories',
          type: 'multiselect',
          options: categoryOptions.value,
          placeholder: 'Toutes categories',
        },
        {
          key: 'types',
          label: "Types d'items",
          type: 'multiselect',
          options: typeOptions.value,
          placeholder: 'Tous types',
        },
      ]
    : []

  return [
    {
      key: 'useGlobalRange',
      label: 'Utiliser periode globale',
      type: 'toggle',
      hint: 'Active par defaut',
    },
    ...rangeFields,
    ...filterFields,
    ...mappedBase,
  ]
})
const settingsModel = computed(() => {
  const base = settingsWidget.value?.props ?? {}
  return {
    useGlobalRange: base.useGlobalRange ?? true,
    from: base.from ?? localFrom.value,
    to: base.to ?? localTo.value,
    asOf: base.asOf ?? localTo.value,
    categories:
      Array.isArray(base.categories) && base.categories.length
        ? base.categories
        : typeof base.category === 'string' && base.category
          ? [base.category]
          : [],
    types:
      Array.isArray(base.types) && base.types.length
        ? base.types
        : typeof base.type === 'string' && base.type
          ? [base.type]
          : [],
    ...base,
  }
})

function openSettings(w: Widget) {
  settingsWidgetId.value = w.id
  settingsOpen.value = true
  const range = widgetCategoryRange(w)
  loadCategories(range.from, range.to)
}
function closeSettings() {
  settingsOpen.value = false
  settingsWidgetId.value = null
}

function onCanvasPointerDown(e: PointerEvent) {
  if (!editMode.value) return
  const target = e.target as HTMLElement | null
  if (target?.closest('.widget')) return
  dragArmedId.value = null
}
function applySettings(newModel: Record<string, unknown>) {
  const w = settingsWidget.value
  if (!w) return
  const next = { ...(w.props ?? {}), ...newModel }
  if (Array.isArray((next as any).categories)) {
    ;(next as any).categories = (next as any).categories
      .map((v: unknown) => String(v ?? '').trim())
      .filter((v: string) => v.length > 0)
  } else if (typeof (next as any).categories === 'string') {
    const v = String((next as any).categories || '').trim()
    ;(next as any).categories = v ? [v] : []
  }
  if (Array.isArray((next as any).types)) {
    ;(next as any).types = (next as any).types
      .map((v: unknown) => String(v ?? '').trim())
      .filter((v: string) => v.length > 0)
  } else if (typeof (next as any).types === 'string') {
    const v = String((next as any).types || '').trim()
    ;(next as any).types = v ? [v] : []
  }
  delete (next as any).category
  delete (next as any).type
  const fromVal = typeof next.from === 'string' ? next.from : ''
  const toVal = typeof next.to === 'string' ? next.to : ''
  if (fromVal) next.from = clampDate(fromVal)
  if (toVal) next.to = clampDate(toVal)
  if (typeof next.from === 'string' && typeof next.to === 'string' && next.from > next.to) {
    const tmp = next.from
    next.from = next.to
    next.to = tmp
  }
  if (typeof next.asOf === 'string' && next.asOf) {
    next.asOf = clampDate(next.asOf)
  }
  w.props = next
  scheduleSave()
  closeSettings()
}

/* ===== Camera (Panzoom) ===== */
const viewportEl = ref<HTMLElement | null>(null)
const boardEl = ref<HTMLElement | null>(null)

const camera = useCanvasCamera(viewportEl, boardEl, {
  boardWidth: BOARD_W,
  boardHeight: BOARD_H,
  maxScale: 3,
  minScale: 0.15,
  contain: 'outside',
  excludeClass: 'panzoom-exclude',
})

const scale = computed(() => camera.scale.value)
const isCompact = ref(false)
const datePanelOpen = ref(true)

function fitToWidgets(padding = 120, animate = true) {
  const vp = viewportEl.value
  if (!vp || widgets.value.length === 0) return camera.fitToViewport?.()

  let minX = Infinity,
    minY = Infinity,
    maxX = -Infinity,
    maxY = -Infinity

  widgets.value.forEach((w) => {
    minX = Math.min(minX, w.x)
    minY = Math.min(minY, w.y)
    maxX = Math.max(maxX, w.x + w.w)
    maxY = Math.max(maxY, w.y + w.h)
  })

  if (!Number.isFinite(minX) || !Number.isFinite(maxX)) return camera.fitToViewport?.()

  const bboxW = Math.max(maxX - minX, 10)
  const bboxH = Math.max(maxY - minY, 10)
  const targetW = bboxW + padding * 2
  const targetH = bboxH + padding * 2

  const scaleW = vp.clientWidth / targetW
  const scaleH = vp.clientHeight / targetH
  const targetScale = clamp(Math.min(scaleW, scaleH), 0.25, 2.5)

  const cx = (minX + maxX) / 2
  const cy = (minY + maxY) / 2

  camera.zoomTo?.(targetScale, { animate })
  requestAnimationFrame(() => {
    requestAnimationFrame(() => {
      camera.centerOn(cx, cy)
    })
  })
}

function widgetsBounds() {
  const list = widgets.value
  if (!list.length) return { cx: BOARD_W / 2, cy: BOARD_H / 2 }

  let minX = Infinity,
    minY = Infinity,
    maxX = -Infinity,
    maxY = -Infinity

  for (const w of list) {
    minX = Math.min(minX, w.x)
    minY = Math.min(minY, w.y)
    maxX = Math.max(maxX, w.x + w.w)
    maxY = Math.max(maxY, w.y + w.h)
  }
  return { cx: (minX + maxX) / 2, cy: (minY + maxY) / 2 }
}

function centerView() {
  fitToWidgets(80, true)
}

function zoomIn() {
  camera.zoomIn()
}
function zoomOut() {
  camera.zoomOut()
}
function resetZoom() {
  fitToWidgets(120, true)
}
function zoomToFitContent() {
  fitToWidgets(80, true)
}

/* ===== Drag ===== */
const widgetEls = new Map<string, HTMLElement>()
type DragState = {
  x: number
  y: number
  scale: number
  lastX: number
  lastY: number
  raf: number | null
}
const dragStates = new Map<string, DragState>()
let activeDragId: string | null = null
let zTop = 10
const snapGuides = ref<{ x: number | null; y: number | null }>({ x: null, y: null })
const SNAP_DIST = 8

type ResizeState = {
  x: number
  y: number
  w: number
  h: number
  scale: number
  dir: ResizeDir
  lastX: number
  lastY: number
  raf: number | null
}
const resizeStates = new Map<string, ResizeState>()
let activeResizeId: string | null = null

function setWidgetRef(id: string, c: any) {
  // c est le composant; on garde la derniere ref DOM valide, et on ne purge
  // que lorsqu'on recoit null (démontage). Cela évite de perdre la ref pendant
  // les phases de montage/teleport et de casser le drag.
  if (c == null) {
    widgetEls.delete(id)
    clearDragState(id)
    return
  }
  const el = (c?.root?.value ?? c?.$el ?? null) as any
  if (el && el.nodeType === 1 && el.classList) {
    widgetEls.set(id, el as HTMLElement)
  }
}

function applyWidgetDOM(el: HTMLElement, w: Widget) {
  el.style.width = `${w.w}px`
  el.style.height = `${w.h}px`
  el.style.transform = `translate3d(${w.x}px, ${w.y}px, 0)`
}

function applyWidgetDOMAt(el: HTMLElement, w: Widget, x: number, y: number) {
  el.style.width = `${w.w}px`
  el.style.height = `${w.h}px`
  el.style.transform = `translate3d(${x}px, ${y}px, 0)`
}

function clampWidgetPosition(x: number, y: number, w: Widget) {
  return {
    x: clamp(x, 0, BOARD_W - w.w),
    y: clamp(y, 0, BOARD_H - w.h),
  }
}

function snapValue(value: number, targets: number[]) {
  let best = value
  let bestDiff = SNAP_DIST + 1
  for (const t of targets) {
    const diff = Math.abs(value - t)
    if (diff < bestDiff) {
      bestDiff = diff
      best = t
    }
  }
  return best
}

function rectsOverlap(
  a: { x: number; y: number; w: number; h: number },
  b: { x: number; y: number; w: number; h: number },
  pad = GRID,
) {
  return !(
    a.x + a.w + pad <= b.x ||
    b.x + b.w + pad <= a.x ||
    a.y + a.h + pad <= b.y ||
    b.y + b.h + pad <= a.y
  )
}

function placeWidget(w: Widget, centerX: number, centerY: number) {
  const baseX = snap(centerX - w.w / 2)
  const baseY = snap(centerY - w.h / 2)
  const step = GRID * 6
  const maxR = Math.max(BOARD_W, BOARD_H)

  const hasOverlap = (x: number, y: number) =>
    widgets.value.some((o) => rectsOverlap({ x, y, w: w.w, h: w.h }, o))

  const base = clampWidgetPosition(baseX, baseY, w)
  if (!hasOverlap(base.x, base.y)) return base

  for (let r = step; r <= maxR; r += step) {
    for (let dx = -r; dx <= r; dx += step) {
      for (let dy = -r; dy <= r; dy += step) {
        if (Math.abs(dx) !== r && Math.abs(dy) !== r) continue
        const p = clampWidgetPosition(baseX + dx, baseY + dy, w)
        if (!hasOverlap(p.x, p.y)) return p
      }
    }
  }

  return base
}

function clearDragState(id: string) {
  const state = dragStates.get(id)
  if (!state) return
  if (state.raf) cancelAnimationFrame(state.raf)
  dragStates.delete(id)
}

function scheduleDragApply(el: HTMLElement, w: Widget, state: DragState) {
  if (state.raf) return
  state.raf = requestAnimationFrame(() => {
    state.raf = null
    applyWidgetDOMAt(el, w, state.x, state.y)
  })
}

function clearResizeState(id: string) {
  const state = resizeStates.get(id)
  if (!state) return
  if (state.raf) cancelAnimationFrame(state.raf)
  resizeStates.delete(id)
}

function scheduleResizeApply(el: HTMLElement, w: Widget, state: ResizeState) {
  if (state.raf) return
  state.raf = requestAnimationFrame(() => {
    state.raf = null
    const view = { ...w, w: state.w, h: state.h } as Widget
    applyWidgetDOMAt(el, view, state.x, state.y)
  })
}

function startDrag(id: string, event: PointerEvent) {
  if (!editMode.value) return
  const w = widgets.value.find((x) => x.id === id)
  let el = widgetEls.get(id)
  // Fallback: récupère l'élément DOM depuis l'event si la ref n'est pas encore disposée.
  if (!el) {
    const target = (event.target as HTMLElement | null)?.closest('.widget') as HTMLElement | null
    if (target) {
      widgetEls.set(id, target)
      el = target
    }
  }
  if (!w || !el) return

  event.preventDefault()
  event.stopPropagation()

  dragArmedId.value = id
  w.z = ++zTop
  activeDragId = id

  const s = Number(camera.scale.value || 1)
  dragStates.set(id, {
    x: w.x,
    y: w.y,
    scale: s > 0 ? s : 1,
    lastX: event.clientX,
    lastY: event.clientY,
    raf: null,
  })

  el.classList.add('is-dragging')
  setCanvasPanEnabled(false)

  window.addEventListener('pointermove', onGlobalPointerMove)
  window.addEventListener('pointerup', onGlobalPointerUp, { once: true })
  window.addEventListener('pointercancel', onGlobalPointerUp, { once: true })
}

function onGlobalPointerMove(event: PointerEvent) {
  if (!activeDragId) return
  const state = dragStates.get(activeDragId)
  const w = widgets.value.find((x) => x.id === activeDragId)
  let el = widgetEls.get(activeDragId)
  if (!el) {
    el = document.querySelector(`[data-id="${activeDragId}"]`) as HTMLElement | null
    if (el) widgetEls.set(activeDragId, el)
  }
  if (!state || !w || !el) return

  const dx = event.clientX - state.lastX
  const dy = event.clientY - state.lastY
  state.lastX = event.clientX
  state.lastY = event.clientY

  state.x += dx / state.scale
  state.y += dy / state.scale

  // snap vs other widgets (center + edges)
  const targetsX: number[] = []
  const targetsY: number[] = []
  for (const other of widgets.value) {
    if (other.id === activeDragId) continue
    targetsX.push(other.x, other.x + other.w / 2, other.x + other.w)
    targetsY.push(other.y, other.y + other.h / 2, other.y + other.h)
  }

  const snappedX = snapValue(state.x, targetsX)
  const snappedY = snapValue(state.y, targetsY)
  const showX = Math.abs(snappedX - state.x) <= SNAP_DIST
  const showY = Math.abs(snappedY - state.y) <= SNAP_DIST
  state.x = showX ? snappedX : state.x
  state.y = showY ? snappedY : state.y

  const clamped = clampWidgetPosition(state.x, state.y, w)
  state.x = clamped.x
  state.y = clamped.y

  snapGuides.value = {
    x: showX ? state.x : null,
    y: showY ? state.y : null,
  }

  scheduleDragApply(el, w, state)
}

function finishDrag(id: string) {
  const state = dragStates.get(id)
  const w = widgets.value.find((x) => x.id === id)
  const el = widgetEls.get(id)
  if (el) el.classList.remove('is-dragging')

  if (!state || !w) {
    clearDragState(id)
    setCanvasPanEnabled(true)
    activeDragId = null
    return
  }

  const snapped = clampWidgetPosition(snap(state.x), snap(state.y), w)
  w.x = snapped.x
  w.y = snapped.y
  clampWidget(w)
  if (el) applyWidgetDOM(el, w)

  snapGuides.value = { x: null, y: null }

  clearDragState(id)
  setCanvasPanEnabled(true)
  scheduleSave()
  activeDragId = null
  if (dragArmedId.value === id) dragArmedId.value = null
}

function onGlobalPointerUp() {
  window.removeEventListener('pointermove', onGlobalPointerMove)
  if (!activeDragId) return
  if (!dragStates.has(activeDragId)) {
    activeDragId = null
    setCanvasPanEnabled(true)
    return
  }
  finishDrag(activeDragId)
}
/** active/désactive le pan du board */
function setCanvasPanEnabled(enabled: boolean) {
  camera.setPanEnabled(enabled)
}

function syncPanzoomExclude(enabled: boolean) {
  const pz = camera.getPanzoom()
  pz?.setOptions?.({ excludeClass: enabled ? 'panzoom-exclude' : null })
}

function startResize(id: string, dir: ResizeDir, event: PointerEvent) {
  if (!editMode.value) return
  if (fullscreenActive.value) return
  const w = widgets.value.find((x) => x.id === id)
  let el = widgetEls.get(id)
  if (!el) {
    const target = (event.target as HTMLElement | null)?.closest('.widget') as HTMLElement | null
    if (target) {
      widgetEls.set(id, target)
      el = target
    }
  }
  if (!w || !el) return

  event.preventDefault()
  event.stopPropagation()

  dragArmedId.value = null
  w.z = ++zTop
  activeResizeId = id

  const s = Number(camera.scale.value || 1)
  resizeStates.set(id, {
    x: w.x,
    y: w.y,
    w: w.w,
    h: w.h,
    dir,
    scale: s > 0 ? s : 1,
    lastX: event.clientX,
    lastY: event.clientY,
    raf: null,
  })

  el.classList.add('is-resizing')
  setCanvasPanEnabled(false)

  window.addEventListener('pointermove', onResizePointerMove)
  window.addEventListener('pointerup', onResizePointerUp, { once: true })
  window.addEventListener('pointercancel', onResizePointerUp, { once: true })
}

function onResizePointerMove(event: PointerEvent) {
  if (!activeResizeId) return
  const state = resizeStates.get(activeResizeId)
  const w = widgets.value.find((x) => x.id === activeResizeId)
  let el = widgetEls.get(activeResizeId)
  if (!el) {
    el = document.querySelector(`[data-id=\"${activeResizeId}\"]`) as HTMLElement | null
    if (el) widgetEls.set(activeResizeId, el)
  }
  if (!state || !w || !el) return

  const dx = (event.clientX - state.lastX) / state.scale
  const dy = (event.clientY - state.lastY) / state.scale
  state.lastX = event.clientX
  state.lastY = event.clientY

  const minSize = minSizeFor(w)
  let left = state.x
  let top = state.y
  let right = state.x + state.w
  let bottom = state.y + state.h

  if (state.dir.includes('e')) right += dx
  if (state.dir.includes('w')) left += dx
  if (state.dir.includes('s')) bottom += dy
  if (state.dir.includes('n')) top += dy

  left = clamp(left, 0, BOARD_W - minSize.w)
  right = clamp(right, minSize.w, BOARD_W)
  top = clamp(top, 0, BOARD_H - minSize.h)
  bottom = clamp(bottom, minSize.h, BOARD_H)

  if (right - left < minSize.w) {
    if (state.dir.includes('w') && !state.dir.includes('e')) {
      left = right - minSize.w
    } else {
      right = left + minSize.w
    }
  }
  if (bottom - top < minSize.h) {
    if (state.dir.includes('n') && !state.dir.includes('s')) {
      top = bottom - minSize.h
    } else {
      bottom = top + minSize.h
    }
  }

  state.x = clamp(left, 0, BOARD_W - minSize.w)
  state.y = clamp(top, 0, BOARD_H - minSize.h)
  state.w = clamp(right - left, minSize.w, BOARD_W)
  state.h = clamp(bottom - top, minSize.h, BOARD_H)

  state.x = clamp(state.x, 0, BOARD_W - state.w)
  state.y = clamp(state.y, 0, BOARD_H - state.h)

  scheduleResizeApply(el, w, state)
}

function finishResize(id: string) {
  const state = resizeStates.get(id)
  const w = widgets.value.find((x) => x.id === id)
  const el = widgetEls.get(id)
  if (el) el.classList.remove('is-resizing')

  if (!state || !w) {
    clearResizeState(id)
    setCanvasPanEnabled(true)
    activeResizeId = null
    return
  }

  const minSize = minSizeFor(w)
  const snappedW = clamp(snap(state.w), minSize.w, BOARD_W)
  const snappedH = clamp(snap(state.h), minSize.h, BOARD_H)
  const snappedX = clamp(snap(state.x), 0, BOARD_W - snappedW)
  const snappedY = clamp(snap(state.y), 0, BOARD_H - snappedH)

  w.w = snappedW
  w.h = snappedH
  w.x = snappedX
  w.y = snappedY
  clampWidget(w)
  if (el) applyWidgetDOM(el, w)

  clearResizeState(id)
  setCanvasPanEnabled(true)
  scheduleSave()
  activeResizeId = null
}

function onResizePointerUp() {
  window.removeEventListener('pointermove', onResizePointerMove)
  if (!activeResizeId) return
  if (!resizeStates.has(activeResizeId)) {
    activeResizeId = null
    setCanvasPanEnabled(true)
    return
  }
  finishResize(activeResizeId)
}

function detachAllInteract() {
  window.removeEventListener('pointermove', onGlobalPointerMove)
  window.removeEventListener('pointermove', onResizePointerMove)
  widgetEls.forEach((el) => {
    if (!el || !el.classList) return
    el.classList.remove('is-dragging')
    el.classList.remove('is-resizing')
  })
  dragStates.clear()
  activeDragId = null
  resizeStates.clear()
  activeResizeId = null
  setCanvasPanEnabled(true)
}

watch(
  editMode,
  async (enabled) => {
    syncPanzoomExclude(enabled)
    if (!enabled) {
      dragArmedId.value = null
      detachAllInteract()
      return
    }
  },
  { immediate: true },
)

watch(
  userId,
  async () => {
    loadEditMode()
    detachAllInteract()
    loadLayoutForUser()
    applyStoredRangeForActiveProfile()
    await nextTick()
    widgets.value.forEach((w) => clampWidget(w))
    centerView()
    if (userId.value !== 'guest') {
      await loadLayoutFromServer()
    }
    await loadDateBounds()
    await loadCategories(from.value, to.value)
  },
  { immediate: false },
)

async function loadDateBounds() {
  try {
    const res = await StatsServices.dateBounds()
    minDate.value = res?.data?.minDate ?? ''
    maxDate.value = res?.data?.maxDate ?? ''
  } catch {
    minDate.value = ''
    maxDate.value = ''
  }
}

async function loadCategories(fromVal?: string, toVal?: string) {
  const key = `${fromVal || ''}__${toVal || ''}`
  const cached = categoriesCache.get(key)
  if (cached) {
    settingsCategories.value = cached
    return
  }
  try {
    const res = await StatsServices.categories(fromVal, toVal)
    const raw = Array.isArray(res?.data) ? res.data : []
    const cleaned = raw
      .map((v) => String(v ?? '').trim())
      .filter((v) => v.length > 0)
    const unique = Array.from(
      new Map(cleaned.map((v) => [v.toLowerCase(), v])).values(),
    )
    if (unique.length) {
      settingsCategories.value = unique
      categoriesCache.set(key, unique)
      return
    }
    const fallback = await StatsServices.categories()
    const fallbackRaw = Array.isArray(fallback?.data) ? fallback.data : []
    const fallbackClean = fallbackRaw
      .map((v) => String(v ?? '').trim())
      .filter((v) => v.length > 0)
    const fallbackUnique = Array.from(
      new Map(fallbackClean.map((v) => [v.toLowerCase(), v])).values(),
    )
    settingsCategories.value = fallbackUnique
    if (fallbackUnique.length) {
      categoriesCache.set('all', fallbackUnique)
    }
  } catch {
    settingsCategories.value = []
  }
}

/* ===== Actions ===== */
function removeWidget(id: string) {
  if (!editMode.value) return
  widgets.value = widgets.value.filter((w) => w.id !== id)
  scheduleSave()
}


function resetLayout() {
  if (!editMode.value) return

  // on ferme les modales si besoin
  paletteOpen.value = false
  closeSettings()

  // on débranche tout et on vide
  detachAllInteract()
  widgets.value = []

  nextTick(() => {
    // recentre la caméra au milieu de la board (vu que plus de widgets)
    centerView()
    // save immédiat (pour être sûr que le layout vide est persisté)
    saveLayoutNow()
  })
}

function addWidget(payload: string | { type: string; view?: string }) {
  if (!editMode.value) return
  paletteOpen.value = false

  const type = typeof payload === 'string' ? payload : payload?.type
  const view = typeof payload === 'string' ? undefined : payload?.view

  let w: Widget
  try {
    w = newWidget(type, 0, 0) as Widget
  } catch (err) {
    console.warn('[stats] addWidget failed', err)
    return
  }

  const p = camera.boardPointFromViewportCenter()
  const placed = placeWidget(w, p.x, p.y)
  w.x = placed.x
  w.y = placed.y
  w.z = ++zTop
  clampWidget(w)

  if (view) {
    w.props = { ...(w.props ?? {}), view }
  }

  widgets.value.push(w)

  nextTick(() => {
    scheduleSave()
  })
}

const fullscreenActive = ref(false)
function onWidgetFullscreenChange(active: boolean) {
  fullscreenActive.value = active
  if (active) {
    document.body.classList.add('widget-fullscreen-open')
  } else {
    document.body.classList.remove('widget-fullscreen-open')
  }
}

function autoResize(id: string, height: number) {
  const w = widgets.value.find((x) => x.id === id)
  if (!w) return
  if (!Number.isFinite(height)) return
  const minSize = minSizeFor(w)
  const nextH = clamp(height, minSize.h, BOARD_H)
  if (Math.abs(nextH - w.h) < 2) return
  w.h = nextH
  clampWidget(w)
  scheduleSave()
}

/* ===== Lifecycle ===== */
onMounted(async () => {
  // init camera + centre quand la vue est prête
  camera.init(() => {
    centerView()
    syncPanzoomExclude(editMode.value)
    if (window.innerWidth < 1024) zoomToFitContent()
  })

  await nextTick()

  widgets.value.forEach((w) => clampWidget(w))
  if (editMode.value) disarmWidget()

  if (userId.value !== 'guest') {
    await loadLayoutFromServer()
  }
  await loadDateBounds()
  await loadCategories(from.value, to.value)
  applyStoredRangeForActiveProfile()

  const resizeHandler = () => {
    const compact = window.innerWidth < 1024
    const wasCompact = isCompact.value
    if (compact !== isCompact.value) {
      isCompact.value = compact
      datePanelOpen.value = !compact // ouvert par défaut en large, fermé en compact
    }
    if (!wasCompact && compact) zoomToFitContent()
  }
  resizeHandler()
  window.addEventListener('resize', resizeHandler, { passive: true })
  onBeforeUnmount(() => window.removeEventListener('resize', resizeHandler))
})

onBeforeUnmount(() => {
  detachAllInteract()
  camera.destroy()
})

function switchProfile(profileId: string) {
  const next = pickProfileId(profileId)
  if (next === activeProfile.value) return
  saveRangeForProfile(activeProfile.value, from.value, to.value)
  saveBundleNow(false)
  applyProfileLayout(layoutBundle.value, next)
  saveBundleNow(false)
  const nextRange = loadRangeForProfile(next)
  if (nextRange) {
    localFrom.value = nextRange.from
    localTo.value = nextRange.to
    emit('update:from', nextRange.from)
    emit('update:to', nextRange.to)
  }
  nextTick(() => {
    widgets.value.forEach((w) => clampWidget(w))
    centerView()
  })
}

function openProfileEditor() {
  profileDraft.value = {
    ...profileNames.value,
    p1Color: profileColors.value.p1 ?? PROFILE_COLORS.p1,
    p2Color: profileColors.value.p2 ?? PROFILE_COLORS.p2,
    p3Color: profileColors.value.p3 ?? PROFILE_COLORS.p3,
  }
  profileEditorOpen.value = true
}
function closeProfileEditor() {
  profileEditorOpen.value = false
}
function saveProfileEditor() {
  profileNames.value = {
    p1: (profileDraft.value.p1 || DEFAULT_PROFILE_NAMES.p1).trim(),
    p2: (profileDraft.value.p2 || DEFAULT_PROFILE_NAMES.p2).trim(),
    p3: (profileDraft.value.p3 || DEFAULT_PROFILE_NAMES.p3).trim(),
  }
  profileColors.value = {
    p1: profileDraft.value.p1Color || PROFILE_COLORS.p1,
    p2: profileDraft.value.p2Color || PROFILE_COLORS.p2,
    p3: profileDraft.value.p3Color || PROFILE_COLORS.p3,
  }
  layoutBundle.value.profileNames = { ...profileNames.value }
  layoutBundle.value.profileColors = { ...profileColors.value }
  saveBundleNow(false)
  profileEditorOpen.value = false
}

</script>

<style scoped>
.canvas-root {
  --bg: #060a12;
  --stats-top-gap: max(16px, env(safe-area-inset-top, 0px) + 10px);
  --stats-side-gap: clamp(10px, 2.8vw, 18px);
  --stats-bottom-gap: max(16px, env(safe-area-inset-bottom, 0px) + 10px);
  --stats-toolbar-clearance: 84px;
  --stats-profile-clearance: 76px;
  position: relative;
  width: 100%;
  height: 100%;
  min-height: 100svh;
  min-height: 100dvh;
  overflow: hidden;
  background: var(--bg);
}

.viewport {
  position: absolute;
  inset: 0;
  overflow: hidden;
  overscroll-behavior: contain;
  touch-action: none;
}
.viewport,
.board {
  touch-action: none;
}

.board {
  width: 9000px;
  height: 6000px;
  position: relative;
  z-index: 1;
  background:
    radial-gradient(circle at 1px 1px, rgba(170, 200, 255, 0.1) 1px, transparent 1.6px) 0 0 / 28px
      28px,
    radial-gradient(circle at 1px 1px, rgba(255, 255, 255, 0.04) 1px, transparent 1.8px) 0 0 / 8px
      8px,
    linear-gradient(180deg, #050812 0%, #040611 100%);
}

.edit-grid {
  position: absolute;
  inset: 0;
  pointer-events: none;
  opacity: 0.35;
  z-index: 2;
  background:
    linear-gradient(transparent 23px, rgba(148, 163, 184, 0.08) 24px) 0 0 / 24px 24px,
    linear-gradient(90deg, transparent 23px, rgba(148, 163, 184, 0.08) 24px) 0 0 / 24px 24px;
  mix-blend-mode: scréen;
}

.date-panel {
  position: fixed;
  top: var(--stats-top-gap);
  left: var(--stats-side-gap);
  width: min(280px, calc(100vw - (var(--stats-side-gap) * 2) - 84px));
  max-width: calc(100vw - (var(--stats-side-gap) * 2) - 84px);
  z-index: 60;
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 8px 10px;
  border-radius: 12px;
  background: rgba(2, 6, 23, 0.4);
  border: 1px solid rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(12px);
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.35);
  touch-action: manipulation;
}
.date-toggle {
  position: fixed;
  top: var(--stats-top-gap);
  left: var(--stats-side-gap);
  z-index: 61;
  width: 34px;
  height: 34px;
  border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.14);
  background: rgba(7, 11, 20, 0.7);
  color: rgba(226, 232, 240, 0.9);
  display: grid;
  place-items: center;
  box-shadow: 0 10px 24px rgba(0, 0, 0, 0.35);
}
.date-panel--compact {
  top: auto;
  left: var(--stats-side-gap);
  right: var(--stats-side-gap);
  bottom: calc(var(--stats-bottom-gap) + var(--stats-profile-clearance) + 18px);
  width: auto;
  max-width: none;
}
.date-toggle--compact {
  top: auto;
  left: var(--stats-side-gap);
  bottom: calc(var(--stats-bottom-gap) + var(--stats-profile-clearance) + 8px);
}
.date-toggle:hover {
  border-color: rgba(148, 163, 184, 0.35);
  background: rgba(255, 255, 255, 0.06);
}

.date-title {
  font-size: 0.58rem;
  text-transform: uppercase;
  letter-spacing: 0.22em;
  color: rgba(148, 163, 184, 0.8);
}

.date-row {
  display: grid;
  gap: 8px;
}

@media (min-width: 640px) {
  .date-row {
    grid-template-columns: 1fr 1fr;
  }
}

.date-actions {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.date-chip {
  height: 24px;
  padding: 0 10px;
  border-radius: 999px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  background: rgba(255, 255, 255, 0.06);
  color: rgba(255, 255, 255, 0.85);
  font-size: 0.7rem;
  transition:
    border-color 160ms ease,
    background 160ms ease,
    transform 140ms ease;
  touch-action: manipulation;
}
.date-chip:hover {
  border-color: rgba(148, 163, 184, 0.35);
  background: rgba(255, 255, 255, 0.08);
  transform: translateY(-1px);
}

@media (hover: none) and (pointer: coarse) {
  .date-panel {
    padding: 10px 12px;
    gap: 8px;
    border-radius: 14px;
  }
  .date-title {
    font-size: 0.68rem;
  }
  .date-chip {
    height: 30px;
    padding: 0 12px;
    font-size: 0.78rem;
  }
}

.snap-guide {
  position: absolute;
  z-index: 3;
  pointer-events: none;
  background: rgba(139, 92, 246, 0.35);
  box-shadow: 0 0 8px rgba(139, 92, 246, 0.2);
}
.snap-guide--x {
  top: 0;
  bottom: 0;
  width: 1px;
}
.snap-guide--y {
  left: 0;
  right: 0;
  height: 1px;
}

.save-toast {
  position: fixed;
  right: var(--stats-side-gap);
  bottom: calc(var(--stats-bottom-gap) + var(--stats-profile-clearance) + 10px);
  z-index: 80;
  padding: 10px 14px;
  border-radius: 12px;
  background: rgba(15, 23, 42, 0.85);
  border: 1px solid rgba(148, 163, 184, 0.25);
  color: rgba(226, 232, 240, 0.95);
  font-size: 0.85rem;
  letter-spacing: 0.01em;
  pointer-events: none;
  box-shadow: 0 12px 30px rgba(0, 0, 0, 0.35);
}

.save-toast-enter-active,
.save-toast-leave-active {
  transition:
    opacity 160ms ease,
    transform 160ms ease;
}
.save-toast-enter-from,
.save-toast-leave-to {
  opacity: 0;
  transform: translateY(6px);
}

/* Profil selector */
.profile-switcher {
  position: fixed;
  left: var(--stats-side-gap);
  right: auto;
  bottom: var(--stats-bottom-gap);
  z-index: 70;
  display: inline-flex;
  max-width: min(calc(100vw - (var(--stats-side-gap) * 2) - 72px), 560px);
  gap: 6px;
  padding: 6px;
  border-radius: 999px;
  background: rgba(10, 12, 18, 0.7);
  border: 1px solid rgba(255, 255, 255, 0.16);
  backdrop-filter: blur(12px);
  box-shadow: 0 12px 30px rgba(0, 0, 0, 0.35);
  overflow-x: auto;
  scrollbar-width: none;
}
.profile-switcher::-webkit-scrollbar {
  display: none;
}
.profile-pill {
  height: 30px;
  padding: 0 10px;
  border-radius: 999px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  background: rgba(255, 255, 255, 0.06);
  color: rgba(226, 232, 240, 0.9);
  font-size: 12px;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  transition:
    border-color 160ms ease,
    background 160ms ease,
    transform 140ms ease;
}
.profile-edit {
  width: 32px;
  height: 32px;
  border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  background: rgba(255, 255, 255, 0.08);
  color: rgba(226, 232, 240, 0.9);
  display: grid;
  place-items: center;
  transition:
    border-color 160ms ease,
    background 160ms ease,
    transform 140ms ease;
}
.profile-edit:hover {
  border-color: rgba(148, 163, 184, 0.4);
  background: rgba(255, 255, 255, 0.14);
  transform: translateY(-1px);
}
.profile-pill:hover {
  border-color: rgba(148, 163, 184, 0.4);
  background: rgba(255, 255, 255, 0.1);
  transform: translateY(-1px);
}
.profile-pill.is-active {
  border-color: rgba(255, 255, 255, 0.35);
  background: rgba(255, 255, 255, 0.14);
}
.profile-dot {
  width: 8px;
  height: 8px;
  border-radius: 999px;
}
.profile-label {
  letter-spacing: 0.02em;
}

@media (max-width: 768px) {
  .canvas-root {
    --stats-toolbar-clearance: 72px;
    --stats-profile-clearance: 64px;
  }
  .profile-switcher {
    left: var(--stats-side-gap);
    right: var(--stats-side-gap);
    bottom: var(--stats-bottom-gap);
    max-width: none;
    gap: 5px;
    padding: 6px;
    border-radius: 16px;
    flex-wrap: nowrap;
  }
  .profile-pill {
    height: 28px;
    padding: 0 8px;
    gap: 6px;
    font-size: 11px;
    flex: 0 0 auto;
  }
  .profile-label {
    display: none;
  }
  .profile-edit {
    width: 30px;
    height: 30px;
    flex: 0 0 auto;
  }
}

@media (max-width: 1024px) {
  .canvas-root {
    --stats-profile-clearance: 70px;
  }
  .date-panel {
    width: min(300px, calc(100vw - (var(--stats-side-gap) * 2) - 72px));
    max-width: calc(100vw - (var(--stats-side-gap) * 2) - 72px);
  }
}

@media (max-width: 640px) {
  .canvas-root {
    --stats-side-gap: 12px;
    --stats-toolbar-clearance: 68px;
    --stats-profile-clearance: 60px;
  }
  .date-panel {
    padding: 9px 10px;
    border-radius: 14px;
    width: min(260px, calc(100vw - (var(--stats-side-gap) * 2) - 68px));
    max-width: calc(100vw - (var(--stats-side-gap) * 2) - 68px);
  }
  .date-panel--compact {
    bottom: calc(var(--stats-bottom-gap) + var(--stats-profile-clearance) + 14px);
  }
  .date-toggle--compact {
    bottom: calc(var(--stats-bottom-gap) + var(--stats-profile-clearance) + 6px);
  }
  .save-toast {
    left: var(--stats-side-gap);
    right: var(--stats-side-gap);
    bottom: calc(var(--stats-bottom-gap) + var(--stats-profile-clearance) + 14px);
    text-align: center;
  }
  .profile-panel {
    top: auto;
    left: 12px;
    right: 12px;
    bottom: calc(env(safe-area-inset-bottom, 0px) + 12px);
    transform: none;
    width: auto;
    max-height: min(78dvh, 720px);
    overflow: auto;
  }
}

@media (max-width: 420px) {
  .canvas-root {
    --stats-side-gap: 10px;
    --stats-profile-clearance: 56px;
  }
  .date-toggle {
    width: 36px;
    height: 36px;
  }
  .date-panel {
    width: min(232px, calc(100vw - (var(--stats-side-gap) * 2) - 62px));
    max-width: calc(100vw - (var(--stats-side-gap) * 2) - 62px);
  }
  .profile-switcher {
    padding: 5px;
    gap: 4px;
  }
  .profile-pill {
    min-width: 28px;
    padding: 0 7px;
  }
}

.profile-modal {
  position: fixed;
  inset: 0;
  z-index: 80;
}
.profile-backdrop {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.55);
  backdrop-filter: blur(6px);
}
.profile-panel {
  position: fixed;
  left: 50%;
  top: 50%;
  transform: translate(-50%, -50%);
  width: min(520px, 92vw);
  box-shadow:
    0 30px 80px rgba(0, 0, 0, 0.55),
    inset 0 1px 0 rgba(255, 255, 255, 0.12);
}
.glass-footer {
  border-top: 1px solid rgba(255, 255, 255, 0.12);
  background: rgba(10, 12, 18, 0.6);
  backdrop-filter: blur(12px);
}
.glass-btn {
  position: relative;
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.18);
  background: rgba(255, 255, 255, 0.12);
  color: rgba(255, 255, 255, 0.95);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.18),
    0 8px 18px rgba(0, 0, 0, 0.25);
}
.glass-btn:hover {
  background: rgba(255, 255, 255, 0.18);
}
.glass-btn-primary {
  background: rgba(99, 102, 241, 0.4);
  border-color: rgba(99, 102, 241, 0.55);
}
.glass-btn-primary:hover {
  background: rgba(99, 102, 241, 0.55);
}
.profile-grid {
  display: grid;
  gap: 10px;
}
.profile-help {
  font-size: 12px;
  color: rgba(226, 232, 240, 0.6);
}
.profile-card {
  display: grid;
  gap: 8px;
  padding: 10px;
  border-radius: 16px;
  background: rgba(12, 16, 24, 0.6);
  border: 1px solid rgba(255, 255, 255, 0.08);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.08),
    0 14px 30px rgba(0, 0, 0, 0.35);
}
.profile-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.profile-title {
  font-size: 12px;
  color: rgba(226, 232, 240, 0.85);
  text-transform: uppercase;
  letter-spacing: 0.18em;
}
.profile-preview {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: rgba(226, 232, 240, 0.8);
}
.profile-preview-name {
  font-weight: 600;
}
.profile-row {
  display: grid;
  grid-template-columns: 1fr 42px;
  gap: 6px;
  align-items: center;
}
.color-swatch {
  width: 42px;
  height: 40px;
  border-radius: 12px;
  padding: 0;
  border: 1px solid rgba(255, 255, 255, 0.14);
  background: transparent;
}
.color-swatch.is-round {
  width: 30px;
  height: 30px;
  border-radius: 999px;
  border: 2px solid rgba(255, 255, 255, 0.28);
  background-color: transparent;
  box-shadow:
    0 0 0 3px rgba(0, 0, 0, 0.35),
    inset 0 0 0 2px rgba(255, 255, 255, 0.08);
  padding: 0;
  overflow: hidden;
}
.color-swatch.is-round::-webkit-color-swatch-wrapper {
  padding: 0;
}
.color-swatch.is-round::-webkit-color-swatch {
  border: none;
  border-radius: 999px;
}
.color-swatch.is-round::-moz-color-swatch {
  border: none;
  border-radius: 999px;
}

/* ✅ en mode édition, on peut drag “partout” (même sur les charts) */
</style>
