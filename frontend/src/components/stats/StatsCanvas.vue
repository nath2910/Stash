<template>
  <div
    class="canvas-root"
    :class="{ 'is-space-pan': spacePanActive, 'theme-light': themeMode === 'light' }"
    :data-edit="editMode ? 'true' : 'false'"
    @pointerdown="onCanvasPointerDown"
    @contextmenu="onCanvasContextMenu"
  >
    <CanvasDock
      v-show="!fullscreenActive && !isCompact && !paletteOpen"
      :edit-mode="editMode"
      :scale="scale"
      :palette-open="paletteOpen"
      :theme-mode="themeMode"
      @toggleEdit="toggleEditMode"
      @toggleTheme="toggleThemeMode"
      @openPalette="paletteOpen = true"
      @zoomIn="zoomIn"
      @zoomOut="zoomOut"
      @resetZoom="resetZoom"
      @centerView="centerView"
      @resetLayout="resetLayout"
    />

    <!-- Canvas -->
    <div ref="viewportEl" class="viewport">
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
            :light="themeMode === 'light'"
            @update:modelValue="setFrom"
          />
          <CompactDateInput
            label="Au"
            :model-value="localTo"
            :min-date="minDate"
            :max-date="maxDate"
            :light="themeMode === 'light'"
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
      <div
        v-if="dragAssistLines.length || dragAssistGaps.length"
        class="drag-assist-overlay"
        aria-hidden="true"
      >
        <div
          v-for="line in dragAssistLines"
          :key="line.id"
          class="drag-assist-line"
          :class="[
            `drag-assist-line--${line.orientation}`,
            `drag-assist-line--${line.kind}`,
            { 'is-board': line.source === 'board' },
          ]"
          :style="dragAssistLineStyle(line)"
        ></div>
        <div
          v-for="gap in dragAssistGaps"
          :key="gap.id"
          class="drag-assist-gap"
          :class="`drag-assist-gap--${gap.orientation}`"
          :style="dragAssistGapStyle(gap)"
        >
          <span class="drag-assist-gap__label">{{ gap.label }}</span>
        </div>
      </div>
      <div ref="boardEl" class="board">
        <div
          v-if="marqueeSelectionStyle"
          class="marquee-selection"
          :style="marqueeSelectionStyle"
          aria-hidden="true"
        ></div>
        <WidgetFrame
          v-for="w in visibleWidgets"
          :key="w.id"
          v-memo="widgetMemoDeps(w)"
          :widget="w"
          :canvas-scale="scale"
          :edit-mode="editMode"
          :selected="isWidgetSelected(w.id) && !isGroupSelectionActive"
          :group-selected="isGroupSelectionActive && isWidgetSelected(w.id)"
          :drag-armed="dragArmedId === w.id"
          :text-active="activeTextWidgetId === w.id"
          :comp="getComp(w.type)"
          :from="widgetFrom(w)"
          :to="widgetTo(w)"
          :style="widgetStyle(w)"
          :ref="(c: any) => setWidgetRef(w.id, c)"
          @activate="setActiveTextWidget(w.id)"
          @dragStart="startDrag(w.id, $event)"
          @resizeStart="startResize(w.id, $event.dir, $event.event)"
          @textPropsChange="updateTextWidgetProps(w.id, $event)"
          @propsPatch="updateWidgetProps(w.id, $event)"
          @textScaleStart="startTextScale(w.id, $event)"
          @fullscreen-change="onWidgetFullscreenChange"
          @autoResize="autoResize(w.id, $event)"
          @duplicate="duplicateWidget(w.id)"
          @settings="openSettings(w)"
          @remove="onWidgetRemove(w.id)"
        />
        <div
          v-if="groupSelectionFrameStyle"
          class="group-selection-frame"
          :style="groupSelectionFrameStyle"
          aria-hidden="true"
        >
          <button
            v-for="h in GROUP_RESIZE_HANDLES"
            :key="`group-resize-${h.dir}`"
            type="button"
            class="group-selection-handle"
            :class="`group-selection-handle--${h.dir}`"
            :title="h.title"
            :aria-label="h.title"
            @pointerdown.stop.prevent="onGroupResizeHandleDown(h.dir, $event)"
          >
            <span class="group-selection-handle__dot"></span>
          </button>
        </div>
      </div>
    </div>

    <div
      v-if="isCompact && !paletteOpen && !fullscreenActive"
      class="mobile-toolbar"
      role="toolbar"
      aria-label="Outils mobile"
    >
      <button
        type="button"
        class="mobile-toolbtn"
        :class="{ 'is-active': datePanelOpen }"
        @click.stop="datePanelOpen = !datePanelOpen"
        aria-label="Afficher la periode"
      >
        <CalendarRange class="h-4 w-4" />
      </button>
      <button type="button" class="mobile-toolbtn" @click.stop="zoomOut" aria-label="Dezoomer">
        <Minus class="h-4 w-4" />
      </button>
      <button type="button" class="mobile-toolbtn mobile-toolbtn--scale" @click.stop="resetZoom">
        {{ Math.round(scale * 100) }}%
      </button>
      <button type="button" class="mobile-toolbtn" @click.stop="zoomIn" aria-label="Zoomer">
        <Plus class="h-4 w-4" />
      </button>
      <button type="button" class="mobile-toolbtn" @click.stop="centerView" aria-label="Centrer">
        <LocateFixed class="h-4 w-4" />
      </button>
      <button
        type="button"
        class="mobile-toolbtn"
        :class="{ 'is-active': editMode }"
        @click.stop="toggleEditMode"
        :aria-label="editMode ? 'Desactiver edition' : 'Activer edition'"
      >
        <component :is="editMode ? LockOpen : Lock" class="h-4 w-4" />
      </button>
      <button
        type="button"
        class="mobile-toolbtn mobile-toolbtn--accent"
        :disabled="!editMode"
        @click.stop="paletteOpen = true"
        aria-label="Ajouter un widget"
      >
        <PlusSquare class="h-4 w-4" />
      </button>
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

    <div
      v-show="!paletteOpen && !fullscreenActive"
      class="profile-switcher"
      :class="{ 'is-collapsed': isCompact && !profileSwitcherExpanded }"
      role="group"
      aria-label="Profils"
    >
      <template v-if="!isCompact || profileSwitcherExpanded">
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
      </template>

      <button
        v-if="isCompact"
        type="button"
        class="profile-toggle"
        :aria-expanded="profileSwitcherExpanded"
        :aria-label="profileSwitcherExpanded ? 'Replier les profils' : 'Deplier les profils'"
        @click.stop="toggleProfileSwitcher"
      >
        <component :is="profileSwitcherExpanded ? ChevronLeft : ChevronRight" class="h-3.5 w-3.5" />
      </button>
    </div>

    <teleport to="body">
      <div
        v-if="shortcutHelpOpen"
        class="shortcut-modal"
        :class="{ 'theme-light': themeMode === 'light' }"
        role="dialog"
        aria-modal="true"
      >
        <div class="shortcut-backdrop" @click="shortcutHelpOpen = false"></div>
        <div class="shortcut-panel" @click.stop>
          <div class="shortcut-head">
            <div>
              <div class="shortcut-kicker">Navigation</div>
              <h3 class="shortcut-title">Raccourcis canvas</h3>
            </div>
            <button
              type="button"
              class="shortcut-close"
              aria-label="Fermer"
              @click="shortcutHelpOpen = false"
            >
              <span class="close-x" aria-hidden="true"></span>
            </button>
          </div>

          <ul class="shortcut-list">
            <li><span>Espace (maintenir)</span><kbd>Pan temporaire</kbd></li>
            <li><span>+</span><kbd>Zoom avant</kbd></li>
            <li><span>-</span><kbd>Zoom arriere</kbd></li>
            <li><span>0</span><kbd>Reset zoom</kbd></li>
            <li><span>F</span><kbd>Centrer le contenu</kbd></li>
            <li><span>P</span><kbd>Ouvrir la palette</kbd></li>
            <li><span>E</span><kbd>Basculer edition</kbd></li>
            <li><span>?</span><kbd>Afficher cette aide</kbd></li>
            <li><span>Shift + resize</span><kbd>Conserver les proportions</kbd></li>
            <li><span>Alt + resize</span><kbd>Redimensionner depuis le centre</kbd></li>
            <li><span>Ctrl/Cmd + resize</span><kbd>Desactiver le snap</kbd></li>
            <li><span>Esc</span><kbd>Fermer les panneaux ouverts</kbd></li>
          </ul>
        </div>
      </div>
    </teleport>

    <teleport to="body">
      <div
        v-if="profileEditorOpen"
        class="profile-modal"
        :class="{ 'theme-light': themeMode === 'light' }"
        role="dialog"
        aria-modal="true"
      >
        <div class="profile-backdrop" @click="closeProfileEditor"></div>
        <div class="profile-panel" @click.stop>
          <div class="profile-header">
            <div class="profile-header__copy">
              <div class="profile-header__title">Renommer les profils</div>
              <p class="profile-header__subtitle">
                Definis des noms clairs et une couleur facile a reconnaitre.
              </p>
            </div>
            <button
              class="profile-header__close"
              type="button"
              aria-label="Fermer"
              @click="closeProfileEditor"
            >
              x
            </button>
          </div>

          <div class="profile-grid">
            <div class="profile-help">
              Ces noms apparaissent dans le selecteur en bas du canvas.
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
                <input
                  v-model="profileDraft.p1"
                  class="profile-input"
                  placeholder="Nom du profil"
                />
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
                <input
                  v-model="profileDraft.p2"
                  class="profile-input"
                  placeholder="Nom du profil"
                />
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
                <input
                  v-model="profileDraft.p3"
                  class="profile-input"
                  placeholder="Nom du profil"
                />
                <input v-model="profileDraft.p3Color" type="color" class="color-swatch is-round" />
              </div>
            </section>
          </div>

          <div class="profile-footer">
            <button type="button" class="profile-btn" @click.stop="closeProfileEditor">
              Annuler
            </button>
            <button
              type="button"
              class="profile-btn profile-btn--primary"
              @click.stop="saveProfileEditor"
            >
              Enregistrer
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
import { useCanvasShortcuts } from './canvas/useCanvasShortcuts'
import {
  Paintbrush,
  CalendarRange,
  Minus,
  Plus,
  LocateFixed,
  Lock,
  LockOpen,
  PlusSquare,
  ChevronLeft,
  ChevronRight,
} from 'lucide-vue-next'

import CompactDateInput from '@/components/ui/CompactDateInput.vue'
import WidgetPalette from './WidgetPalette.vue'
import WidgetSettingsModal from './WidgetSettingsModal.vue'
import {
  WIDGET_DEFS,
  cloneWidgetProps,
  getCategoryColor,
  getWidgetDef,
  newWidget,
} from './widgetRegistry'
import { getWidgetPaletteMeta } from './palette/widgetPaletteMeta'
import { useTheme } from '@/composables/useTheme'
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

type ProfileRange = { from: string; to: string }

type LayoutBundle = {
  version: number
  activeProfile?: string
  profiles: Record<string, Array<Record<string, unknown>>>
  profileNames?: Record<string, string>
  profileColors?: Record<string, string>
  ranges?: Record<string, ProfileRange>
}

/* props/emit */
const props = defineProps({
  from: { type: String, required: true },
  to: { type: String, required: true },
})
const emit = defineEmits(['update:from', 'update:to'])
const { from, to } = toRefs(props)

const { user } = useAuthStore()
const { theme, toggleTheme } = useTheme()
const themeMode = computed(() => (theme.value === 'light' ? 'light' : 'dark'))
// Chaque utilisateur a une cle de layout isolee; guest reste en stockage local.
const userId = computed(() => user.value?.id ?? 'guest')

const PROFILE_COLORS = { p1: '#22C55E', p2: '#3B82F6', p3: '#F59E0B' }
const DEFAULT_PROFILE_NAMES = { p1: 'Profil 1', p2: 'Profil 2', p3: 'Profil 3' }
const PROFILES = [
  { id: 'p1', label: DEFAULT_PROFILE_NAMES.p1, color: PROFILE_COLORS.p1 },
  { id: 'p2', label: DEFAULT_PROFILE_NAMES.p2, color: PROFILE_COLORS.p2 },
  { id: 'p3', label: DEFAULT_PROFILE_NAMES.p3, color: PROFILE_COLORS.p3 },
]
const COMPACT_BREAKPOINT = 1100
const activeProfile = ref('p1')
const layoutBundle = ref<LayoutBundle>({
  version: 1,
  activeProfile: 'p1',
  profiles: {},
  ranges: {},
})
const profileNames = ref({ ...DEFAULT_PROFILE_NAMES })
const profileColors = ref({ ...PROFILE_COLORS })
const profileEditorOpen = ref(false)
const profileDraft = ref({
  ...DEFAULT_PROFILE_NAMES,
  p1Color: PROFILE_COLORS.p1,
  p2Color: PROFILE_COLORS.p2,
  p3Color: PROFILE_COLORS.p3,
})

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

/* ===== Mode edition/fige ===== */
// Le mode edition est stocke par utilisateur pour eviter les fuites d'etat UI.
const EDIT_KEY_PREFIX = 'snk_stats_canvas_edit_v1'
const editKey = computed(() => `${EDIT_KEY_PREFIX}_${userId.value}`)
const editMode = ref(true)

function loadEditMode() {
  const raw = localStorage.getItem(editKey.value)
  const compactDefault = typeof window !== 'undefined' && window.innerWidth < COMPACT_BREAKPOINT
  editMode.value = raw ? raw === 'true' : !compactDefault
}
loadEditMode()

function persistEditMode() {
  localStorage.setItem(editKey.value, String(editMode.value))
}
function toggleEditMode() {
  editMode.value = !editMode.value
  persistEditMode()
}

function toggleThemeMode() {
  toggleTheme()
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

function normalizeProfileRange(raw: unknown): ProfileRange | null {
  if (!raw || typeof raw !== 'object') return null
  const fromVal =
    typeof (raw as { from?: unknown }).from === 'string'
      ? String((raw as { from?: string }).from)
      : ''
  const toVal =
    typeof (raw as { to?: unknown }).to === 'string' ? String((raw as { to?: string }).to) : ''
  if (!fromVal || !toVal) return null
  return fromVal <= toVal ? { from: fromVal, to: toVal } : { from: toVal, to: fromVal }
}

function loadRangeForProfile(profileId: string) {
  const fromBundle = normalizeProfileRange(layoutBundle.value?.ranges?.[profileId])
  if (fromBundle) return fromBundle

  try {
    const raw = localStorage.getItem(rangeKey(profileId))
    if (!raw) return null
    const fromStorage = normalizeProfileRange(JSON.parse(raw))
    if (fromStorage) {
      layoutBundle.value.ranges = layoutBundle.value.ranges ?? {}
      layoutBundle.value.ranges[profileId] = fromStorage
    }
    return fromStorage
  } catch {
    return null
  }
}

function saveRangeForProfile(
  profileId: string,
  f: string,
  t: string,
  options: { persistLocal?: boolean } = {},
) {
  const normalized = normalizeProfileRange({ from: f, to: t })
  if (!normalized) return
  const persistLocal = options.persistLocal !== false

  layoutBundle.value.ranges = layoutBundle.value.ranges ?? {}
  layoutBundle.value.ranges[profileId] = normalized

  if (!persistLocal) return
  try {
    localStorage.setItem(rangeKey(profileId), JSON.stringify(normalized))
  } catch {
    // ignore
  }
}

function preset(kind: 'month' | 'ytd' | 'year') {
  /**
   * 1) On se met sur "aujourd'hui" a 00:00 (heure locale)
   *    -> evite les decalages quand on formate en YYYY-MM-DD
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
    // new Date(year, monthIndex, 1) gere automatiquement les passages d'annee.
    const firstOfTargetMonth = new Date(date.getFullYear(), date.getMonth() - months, 1)

    // Dernier jour du mois cible: jour 0 du mois suivant
    const lastDayOfTargetMonth = new Date(
      firstOfTargetMonth.getFullYear(),
      firstOfTargetMonth.getMonth() + 1,
      0,
    ).getDate()

    // Clamp du jour pour eviter que JS "deborde" sur le mois suivant
    const clampedDay = Math.min(day, lastDayOfTargetMonth)

    // Date finale (a 00:00)
    return new Date(firstOfTargetMonth.getFullYear(), firstOfTargetMonth.getMonth(), clampedDay)
  }

  /**
   * 4) Helper: "soustraire N annees" en gardant mois+jour si possible,
   *    sinon clamp (cas typique: 29/02).
   *
   *    Exemple:
   *    - 2024-02-29 - 1 an => 2023-02-28
   */
  const subYearsClamp = (date: Date, years: number) => {
    const targetYear = date.getFullYear() - years
    const month = date.getMonth()
    const day = date.getDate()

    // Dernier jour du mois dans l'annee cible
    const lastDayOfTargetMonth = new Date(targetYear, month + 1, 0).getDate()
    const clampedDay = Math.min(day, lastDayOfTargetMonth)

    return new Date(targetYear, month, clampedDay)
  }

  /**
   * 5) Applique la plage au composant + emet les events
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
   * - month : 1 mois glissant en arriere depuis aujourd'hui
   * - ytd   : du 1er janvier (annee courante) a aujourd'hui
   * - year  : 1 an glissant en arriere depuis aujourd'hui
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
  () => [from.value, to.value],
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
const NON_TEXT_MAX_SCALE = 2.4
const SNAP_DIST = 8
const GRID_SNAP_DIST = 6
const SIZE_SNAP_DIST = 10
const DRAG_ASSIST_GUIDE_PAD = GRID * 4
const DRAG_ASSIST_GAP_MIN = 4
const DRAG_ASSIST_GAP_MAX = GRID * 18
const DRAG_ASSIST_MIN_OVERLAP = GRID

const clamp = (n: number, a: number, b: number) => Math.max(a, Math.min(b, n))
const snap = (n: number) => Math.round(n / GRID) * GRID

function minSizeFor(w: Widget) {
  const def = getWidgetDef(w.type)
  if (isTextWidget(w)) {
    return {
      w: textWidgetMinimumWidth(w),
      h: textWidgetMinimumHeight(w),
    }
  }
  return {
    w: def?.minSize?.w ?? MIN_W,
    h: def?.minSize?.h ?? MIN_H,
  }
}

function maxSizeFor(w: Widget) {
  if (isTextWidget(w)) {
    return {
      w: BOARD_W,
      h: BOARD_H,
    }
  }
  const def = getWidgetDef(w.type)
  const baseW = Math.max(Number(def?.defaultSize?.w ?? w.w ?? MIN_W), 1)
  const baseH = Math.max(Number(def?.defaultSize?.h ?? w.h ?? MIN_H), 1)
  const maxCfgW = Number(def?.maxSize?.w ?? Number.NaN)
  const maxCfgH = Number(def?.maxSize?.h ?? Number.NaN)
  const maxW = Number.isFinite(maxCfgW) && maxCfgW > 0 ? maxCfgW : BOARD_W
  const maxH = Number.isFinite(maxCfgH) && maxCfgH > 0 ? maxCfgH : BOARD_H
  return {
    w: clamp(Math.round(baseW * NON_TEXT_MAX_SCALE), MIN_W, Math.min(maxW, BOARD_W)),
    h: clamp(Math.round(baseH * NON_TEXT_MAX_SCALE), MIN_H, Math.min(maxH, BOARD_H)),
  }
}

function clampWidget(w: Widget) {
  const minSize = minSizeFor(w)
  const maxSize = maxSizeFor(w)
  w.w = clamp(w.w, minSize.w, maxSize.w)
  w.h = clamp(w.h, minSize.h, maxSize.h)
  w.x = clamp(w.x, 0, BOARD_W - w.w)
  w.y = clamp(w.y, 0, BOARD_H - w.h)
}

function isTextWidget(widget: Pick<Widget, 'type'> | null | undefined) {
  return widget?.type === 'textTitle' || widget?.type === 'textBlock'
}

function shouldPreserveTextWidth(
  widget: Widget,
  mode: 'content' | 'resize' | 'scale' | 'duplicate' = 'content',
) {
  if (!isTextWidget(widget)) return false
  if (mode === 'resize') return true
  if (mode === 'scale') return true
  return true
}

const TEXT_FONT_STACKS: Record<string, string> = {
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

function textWidgetFontFamily(widget: Widget) {
  return (
    TEXT_FONT_STACKS[String(widget.props?.fontFamily ?? 'open-sans')] ??
    TEXT_FONT_STACKS['open-sans']
  )
}

function textWidgetFontBounds(widget: Widget) {
  if (widget.type === 'textTitle') return { min: 16, max: 620, fallback: 52 }
  return { min: 12, max: 620, fallback: 17 }
}

function textWidgetResolvedFontSize(widget: Widget) {
  const bounds = textWidgetFontBounds(widget)
  const raw =
    Number(widget.props?.fontSize) ||
    (widget.type === 'textTitle'
      ? widget.props?.size === 'sm'
        ? 28
        : widget.props?.size === 'md'
          ? 38
          : widget.props?.size === 'xl'
            ? 68
            : bounds.fallback
      : widget.props?.size === 'sm'
        ? 14
        : widget.props?.size === 'lg'
          ? 20
          : widget.props?.size === 'xl'
            ? 26
            : bounds.fallback)
  return clamp(Math.round(raw), bounds.min, bounds.max)
}

function textWidgetPaddingPx(widget: Widget) {
  const rawPadding = String(widget.props?.padding ?? '').trim()
  const paddingMap: Record<string, number> = {
    none: 0,
    sm: 8,
    md: 12,
    lg: 18,
    xl: 24,
  }
  if (rawPadding && rawPadding in paddingMap) return paddingMap[rawPadding]
  return widget.props?.tight ? 8 : 12
}

function textWidgetMinimumWidth(widget: Widget) {
  if (!isTextWidget(widget)) return MIN_W
  const pad = textWidgetPaddingPx(widget)
  const floor = widget.type === 'textTitle' ? 56 : 72
  const ceiling = widget.type === 'textTitle' ? 180 : 220
  return clamp(Math.ceil(pad * 2 + 24), floor, ceiling)
}

function titleLineHeightFactor(widget: Widget) {
  const sizeKey = String(widget.props?.size ?? 'lg')
  const map: Record<string, number> = { sm: 0.98, md: 0.96, lg: 0.94, xl: 0.9 }
  return map[sizeKey] ?? 0.94
}

function textBlockLineHeightFactor(widget: Widget) {
  const sizeKey = String(widget.props?.size ?? 'md')
  const map: Record<string, number> = { sm: 1.6, md: 1.58, lg: 1.5, xl: 1.42 }
  return map[sizeKey] ?? 1.58
}

function textWidgetMinimumHeight(widget: Widget) {
  if (!isTextWidget(widget)) return MIN_H
  const pad = textWidgetPaddingPx(widget)
  const fontPx = textWidgetResolvedFontSize(widget)
  const lineHeightPx =
    widget.type === 'textTitle'
      ? fontPx * titleLineHeightFactor(widget)
      : fontPx * textBlockLineHeightFactor(widget)
  const floor = widget.type === 'textTitle' ? 24 : 30
  const ceiling = widget.type === 'textTitle' ? 320 : 460
  return clamp(Math.ceil(lineHeightPx + pad * 2 + 2), floor, ceiling)
}

function estimateTextWidgetSize(
  widget: Widget,
  options: { targetWidth?: number; targetHeight?: number; lockWidth?: boolean } = {},
) {
  const normalized = String(widget.props?.content ?? widget.title ?? '').replace(/\r\n?/g, '\n')
  const content = normalized.length ? normalized : widget.title || 'Texte'
  const lines = content.split('\n')
  const longestLine = lines.reduce(
    (longest, line) => (line.length > longest.length ? line : longest),
    lines[0] ?? content,
  )
  const measureCanvas = typeof document !== 'undefined' ? document.createElement('canvas') : null
  const ctx = measureCanvas?.getContext?.('2d') ?? null
  const widthHint = Math.max(options.targetWidth ?? widget.w ?? 0, 0)
  const heightHint = Math.max(options.targetHeight ?? widget.h ?? 0, 0)
  const lockWidth = options.lockWidth === true
  const minWidth = textWidgetMinimumWidth(widget)
  const minHeight = textWidgetMinimumHeight(widget)
  const pad = textWidgetPaddingPx(widget)
  const padX = pad * 2
  const padY = pad * 2

  if (widget.type === 'textTitle') {
    const baseSize = textWidgetResolvedFontSize(widget)
    const fontWeight =
      widget.props?.weight === 'heavy'
        ? 800
        : widget.props?.weight === 'bold'
          ? 700
          : widget.props?.weight === 'semibold'
            ? 600
            : widget.props?.weight === 'medium'
              ? 500
              : widget.props?.weight === 'regular'
                ? 400
                : 700
    const fontPx = clamp(Math.round(baseSize), 16, 620)
    if (ctx) {
      ctx.font = `${widget.props?.italic ? 'italic ' : ''}${fontWeight} ${fontPx}px ${textWidgetFontFamily(widget)}`
    }
    const measuredLine = (line: string) =>
      ctx ? ctx.measureText(line || ' ').width : Math.max(line.length, 1) * fontPx * 0.62
    const measured = measuredLine(longestLine)
    const width = lockWidth
      ? clamp(Math.ceil(widthHint || measured + padX + 2), minWidth, 2600)
      : clamp(Math.ceil(measured + padX + 2), minWidth, Math.max(widthHint || 0, 2600))
    const usableWidth = Math.max(width - padX, 1)
    const visualLines = lines.reduce(
      (count, line) => count + Math.max(1, Math.ceil(measuredLine(line) / usableWidth)),
      0,
    )
    const lineHeightPx = fontPx * titleLineHeightFactor(widget)
    const height = clamp(
      Math.ceil(visualLines * lineHeightPx + padY + 2),
      minHeight,
      Math.max(heightHint || 0, 1200),
    )
    return { w: width, h: height }
  }

  const fontBase = textWidgetResolvedFontSize(widget)
  const fontWeight =
    widget.props?.weight === 'bold'
      ? 700
      : widget.props?.weight === 'semibold'
        ? 600
        : widget.props?.weight === 'medium'
          ? 500
          : 400
  const fontPx = clamp(Math.round(fontBase), 12, 180)
  const lineHeightFactor = textBlockLineHeightFactor(widget)
  if (ctx) {
    ctx.font = `${widget.props?.italic ? 'italic ' : ''}${fontWeight} ${fontPx}px ${textWidgetFontFamily(widget)}`
  }
  const measuredLine = (line: string) =>
    ctx ? ctx.measureText(line || ' ').width : Math.max(line.length, 1) * fontPx * 0.56
  const measured = measuredLine(longestLine)
  const width = lockWidth
    ? clamp(Math.ceil(widthHint || Math.max(measured + padX + 2, minWidth)), minWidth, 1800)
    : clamp(
        Math.ceil(Math.max(measured + padX + 2, minWidth)),
        minWidth,
        Math.max(widthHint || 0, 1800),
      )
  const usableWidth = Math.max(width - padX, 1)
  const spaceWidth = measuredLine(' ')
  const wrappedLineCount = (line: string) => {
    const trimmed = line.trim()
    if (!trimmed.length) return 1
    const words = trimmed.split(/\s+/).filter(Boolean)
    const placeWord = (wordWidth: number) => {
      if (wordWidth <= usableWidth) {
        return { extraLines: 0, lineWidth: wordWidth }
      }
      const linesNeeded = Math.ceil(wordWidth / usableWidth)
      const remainder = wordWidth % usableWidth
      return {
        extraLines: Math.max(0, linesNeeded - 1),
        lineWidth: remainder > 0 ? remainder : usableWidth,
      }
    }
    let count = 1
    let currentWidth = 0
    for (const word of words) {
      const wordWidth = measuredLine(word)
      if (currentWidth <= 0) {
        const placed = placeWord(wordWidth)
        count += placed.extraLines
        currentWidth = placed.lineWidth
        continue
      }
      const nextWidth = currentWidth + spaceWidth + wordWidth
      if (nextWidth <= usableWidth) {
        currentWidth = nextWidth
      } else {
        count += 1
        const placed = placeWord(wordWidth)
        count += placed.extraLines
        currentWidth = placed.lineWidth
      }
    }
    return count
  }
  const visualLines = lines.reduce((count, line) => count + wrappedLineCount(line), 0)
  const height = clamp(
    Math.ceil(visualLines * fontPx * lineHeightFactor + padY + 2),
    minHeight,
    Math.max(heightHint || 0, 1800),
  )
  return { w: width, h: height }
}

function fitWidgetToContent(
  widget: Widget,
  options: { targetWidth?: number; targetHeight?: number; lockWidth?: boolean } = {},
) {
  if (!isTextWidget(widget)) return
  const next = estimateTextWidgetSize(widget, options)
  widget.w = next.w
  widget.h = next.h
  clampWidget(widget)
}

function fitTextWidgetToRenderedContent(
  widget: Widget,
  el: HTMLElement | null | undefined,
  options: { preserveWidth?: boolean } = {},
) {
  if (!isTextWidget(widget) || !el) return
  const body = el.querySelector('.widget__body') as HTMLElement | null
  const header = el.querySelector('.widget__header') as HTMLElement | null
  const headerTitle = el.querySelector('.widget__title') as HTMLElement | null
  const headerActions = el.querySelector('.widget__actions') as HTMLElement | null
  const textNode = el.querySelector('.text-title-copy, .text-block-copy') as HTMLElement | null
  if (!body || !textNode) {
    fitWidgetToContent(widget, {
      targetWidth: widget.w,
      targetHeight: widget.h,
      lockWidth: options.preserveWidth === true,
    })
    return
  }

  const bodyStyles = window.getComputedStyle(body)
  const padLeft = parseFloat(bodyStyles.paddingLeft || '0') || 0
  const padRight = parseFloat(bodyStyles.paddingRight || '0') || 0
  const padTop = parseFloat(bodyStyles.paddingTop || '0') || 0
  const padBottom = parseFloat(bodyStyles.paddingBottom || '0') || 0
  let textWidth = 0
  let textHeight = 0
  try {
    const range = document.createRange()
    range.selectNodeContents(textNode)
    const lineRects = Array.from(range.getClientRects()).filter(
      (rect) => rect.width > 0 || rect.height > 0,
    )
    if (lineRects.length) {
      const top = Math.min(...lineRects.map((rect) => rect.top))
      const bottom = Math.max(...lineRects.map((rect) => rect.bottom))
      textWidth = Math.ceil(lineRects.reduce((max, rect) => Math.max(max, rect.width), 0))
      textHeight = Math.ceil(bottom - top)
    } else {
      const rangeRect = range.getBoundingClientRect()
      textWidth = Math.ceil(rangeRect.width || 0)
      textHeight = Math.ceil(rangeRect.height || 0)
    }
    if (textWidth <= 0 || textHeight <= 0) {
      const nodeRect = textNode.getBoundingClientRect()
      textWidth = Math.ceil(Math.max(textWidth, textNode.scrollWidth || 0, nodeRect.width || 0))
      textHeight = Math.ceil(Math.max(textHeight, textNode.scrollHeight || 0, nodeRect.height || 0))
    }
  } catch {
    const nodeRect = textNode.getBoundingClientRect()
    textWidth = Math.ceil(Math.max(textNode.scrollWidth || 0, nodeRect.width || 0))
    textHeight = Math.ceil(Math.max(textNode.scrollHeight || 0, nodeRect.height || 0))
  }
  let headerWidth = 0
  let headerHeight = 0
  if (header) {
    const headerStyles = window.getComputedStyle(header)
    const headerPadLeft = parseFloat(headerStyles.paddingLeft || '0') || 0
    const headerPadRight = parseFloat(headerStyles.paddingRight || '0') || 0
    const headerGap = parseFloat(headerStyles.columnGap || headerStyles.gap || '0') || 0
    const titleWidth = Math.ceil(headerTitle?.getBoundingClientRect().width ?? 0)
    const actionsWidth = Math.ceil(headerActions?.getBoundingClientRect().width ?? 0)
    headerWidth = Math.ceil(
      headerPadLeft +
        titleWidth +
        (actionsWidth > 0 ? headerGap + actionsWidth : 0) +
        headerPadRight +
        2,
    )
    headerHeight = Math.ceil(header.getBoundingClientRect().height)
  }
  const minSize = minSizeFor(widget)

  const nextWidth = options.preserveWidth
    ? clamp(widget.w, minSize.w, BOARD_W)
    : clamp(Math.max(textWidth + padLeft + padRight + 2, headerWidth), minSize.w, BOARD_W)

  widget.w = nextWidth
  widget.h = clamp(
    Math.max(Math.ceil(headerHeight + textHeight + padTop + padBottom + 2), minSize.h),
    minSize.h,
    BOARD_H,
  )
  clampWidget(widget)
}

function fitTextWidgetAfterRender(
  widget: Widget,
  el: HTMLElement | null | undefined,
  options: { preserveWidth?: boolean } = {},
) {
  nextTick(() => {
    const run = () => {
      requestAnimationFrame(() => {
        const boardWidgetEl =
          boardEl.value?.querySelector<HTMLElement>(`.widget[data-id="${widget.id}"]`) ?? null
        const liveEl = widgetEls.get(widget.id) ?? el ?? boardWidgetEl
        fitTextWidgetToRenderedContent(widget, liveEl, options)
      })
    }

    const fonts = (document as Document & { fonts?: { ready?: Promise<unknown> } }).fonts
    if (fonts?.ready) {
      fonts.ready.then(run).catch(run)
      return
    }
    run()
  })
}

// Layout persiste par utilisateur; fallback sur l'ancienne cle globale si presente.
const STORAGE_KEY_PREFIX = 'snk_stats_canvas_layout_v4'
const layoutKey = computed(() => `${STORAGE_KEY_PREFIX}_${userId.value}`)

const widgets = ref<Widget[]>([])
const widgetStyleCache = new Map<
  string,
  { x: number; y: number; w: number; h: number; z: number; style: Record<string, string | number> }
>()
const widgetById = computed(() => {
  const map = new Map<string, Widget>()
  for (const widget of widgets.value) {
    map.set(widget.id, widget)
  }
  return map
})

function getWidgetById(id: string) {
  return widgetById.value.get(id) ?? null
}

function clearWidgetStyleCacheFor(id: string) {
  widgetStyleCache.delete(id)
}

function pruneWidgetStyleCache() {
  if (!widgetStyleCache.size) return
  const existing = new Set(widgets.value.map((widget) => widget.id))
  for (const id of widgetStyleCache.keys()) {
    if (!existing.has(id)) widgetStyleCache.delete(id)
  }
}

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
      ...cloneWidgetProps(def.defaultProps),
      content:
        'Bienvenue sur ton espace stats. Ajoute des widgets depuis la palette pour composer ton dashboard.',
      align: 'center',
    },
  }

  fitWidgetToContent(w)
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
    const nextTitle = typeof (item as any)?.title === 'string' ? (item as any).title : def.title
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
      props: {
        ...cloneWidgetProps(def.defaultProps),
        ...cloneWidgetProps((item as any)?.props ?? {}),
      },
    }

    if (def.type === 'grossRevenue') {
      const legacySizes = [
        { w: 820, h: 520 },
        { w: 980, h: 620 },
      ]
      const isLegacyDefault = legacySizes.some(
        (size) => Math.abs(w.w - size.w) < 1 && Math.abs(w.h - size.h) < 1,
      )
      if (isLegacyDefault) {
        w.w = def.defaultSize.w
        w.h = def.defaultSize.h
      }
      const view = String((w.props as any)?.view ?? 'number')
      if (view !== 'line' && view !== 'number') {
        ;(w.props as any).view = 'number'
      }
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

    if (isTextWidget(w)) {
      fitWidgetToContent(w)
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
  const normalizeRanges = (ranges: unknown): Record<string, ProfileRange> => {
    if (!ranges || typeof ranges !== 'object' || Array.isArray(ranges)) return {}
    const map: Record<string, ProfileRange> = {}
    for (const [profileId, value] of Object.entries(ranges as Record<string, unknown>)) {
      const normalized = normalizeProfileRange(value)
      if (!normalized) continue
      map[pickProfileId(profileId)] = normalized
    }
    return map
  }

  if (raw && typeof raw === 'object' && !Array.isArray(raw) && (raw as any).profiles) {
    const obj = raw as LayoutBundle
    return {
      version: Number(obj.version || 1),
      activeProfile: typeof obj.activeProfile === 'string' ? obj.activeProfile : 'p1',
      profiles: obj.profiles ?? {},
      profileNames: obj.profileNames ?? {},
      profileColors: obj.profileColors ?? {},
      ranges: normalizeRanges((obj as any).ranges),
    }
  }

  if (Array.isArray(raw)) {
    return {
      version: 1,
      activeProfile: 'p1',
      profiles: { p1: raw },
      profileNames: {},
      profileColors: {},
      ranges: {},
    }
  }

  return {
    version: 1,
    activeProfile: 'p1',
    profiles: {},
    profileNames: {},
    profileColors: {},
    ranges: {},
  }
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
  widgetStyleCache.clear()
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
let layoutLoadSeq = 0
const showSaveToast = ref(false)
const LOCAL_SAVE_DEBOUNCE_MS = 520
const REMOTE_SAVE_DEBOUNCE_MS = 1500

function clearPendingSaves() {
  if (saveTimer) {
    window.clearTimeout(saveTimer)
    saveTimer = null
  }
  if (remoteSaveTimer) {
    window.clearTimeout(remoteSaveTimer)
    remoteSaveTimer = null
  }
}

function showSavedToast() {
  showSaveToast.value = true
  if (toastTimer) window.clearTimeout(toastTimer)
  toastTimer = window.setTimeout(() => {
    showSaveToast.value = false
    toastTimer = null
  }, 1600)
}

// Chargement du layout serveur pour plus de robustesse (prive / multi-appareil).
async function loadLayoutFromServer(expectedUserId = String(userId.value)) {
  if (expectedUserId === 'guest') return
  const seq = ++layoutLoadSeq
  try {
    const res = await StatsServices.getLayout()
    if (seq !== layoutLoadSeq || String(userId.value) !== expectedUserId) return
    const payload = res?.data?.layout
    if (payload === null || typeof payload === 'undefined') {
      return
    }
    const bundle = normalizeBundle(payload)
    layoutBundle.value = bundle
    applyProfileLayout(bundle, bundle.activeProfile)
    applyStoredRangeForActiveProfile()
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

function saveBundleNow(showToast = false) {
  const bundle = layoutBundle.value
  bundle.activeProfile = activeProfile.value
  bundle.profiles = bundle.profiles ?? {}
  saveRangeForProfile(activeProfile.value, from.value, to.value, { persistLocal: false })
  bundle.profiles[activeProfile.value] = serializeWidgets()
  bundle.profileNames = { ...profileNames.value }
  bundle.profileColors = { ...profileColors.value }
  bundle.ranges = { ...(bundle.ranges ?? {}) }

  const storageKey = userId.value === 'guest' ? STORAGE_KEY_PREFIX : layoutKey.value
  localStorage.setItem(storageKey, JSON.stringify(bundle))
  if (showToast) showSavedToast()
  scheduleRemoteSave()
}

function saveLayoutNow(showToast = false) {
  saveBundleNow(showToast)
}
function scheduleSave() {
  if (saveTimer) window.clearTimeout(saveTimer)
  saveTimer = window.setTimeout(() => {
    saveLayoutNow(false)
    saveTimer = null
  }, LOCAL_SAVE_DEBOUNCE_MS)
}

// Sauvegarde distante debounce pour eviter de spammer l'API.
function scheduleRemoteSave(payload: unknown = layoutBundle.value) {
  const expectedUserId = String(userId.value)
  if (expectedUserId === 'guest') return
  if (remoteSaveTimer) window.clearTimeout(remoteSaveTimer)
  remoteSaveTimer = window.setTimeout(async () => {
    if (String(userId.value) !== expectedUserId) {
      remoteSaveTimer = null
      return
    }
    try {
      await StatsServices.saveLayout(payload)
    } catch (err) {
      console.warn('[stats] remote save failed', err)
    } finally {
      remoteSaveTimer = null
    }
  }, REMOTE_SAVE_DEBOUNCE_MS)
}

function widgetStyle(w: Widget) {
  const rect = liveWidgetRect(w)
  const z = Number(w.z ?? 1)
  const cached = widgetStyleCache.get(w.id)
  if (
    cached &&
    cached.x === rect.x &&
    cached.y === rect.y &&
    cached.w === rect.w &&
    cached.h === rect.h &&
    cached.z === z
  ) {
    return cached.style
  }

  const style = {
    width: `${rect.w}px`,
    height: `${rect.h}px`,
    transform: `translate3d(${rect.x}px, ${rect.y}px, 0)`,
    zIndex: z,
  }
  widgetStyleCache.set(w.id, {
    x: rect.x,
    y: rect.y,
    w: rect.w,
    h: rect.h,
    z,
    style,
  })
  return style
}

function widgetMemoDeps(w: Widget) {
  const selected = isWidgetSelected(w.id)
  const grouped = isGroupSelectionActive.value && selected
  const useGlobal = w.props?.useGlobalRange !== false
  const fromVal = useGlobal ? from.value : (w.props?.from ?? from.value)
  const toVal = useGlobal ? to.value : (w.props?.to ?? to.value)
  return [
    w.id,
    w.type,
    w.title,
    w.x,
    w.y,
    w.w,
    w.h,
    Number(w.z ?? 1),
    w.props,
    editMode.value,
    dragArmedId.value === w.id,
    activeTextWidgetId.value === w.id,
    selected,
    grouped,
    fromVal,
    toVal,
    themeMode.value,
  ]
}

watch(
  () => widgets.value.map((widget) => widget.id).join('|'),
  () => {
    pruneWidgetStyleCache()
  },
)

/* ===== Widget registry ===== */
const paletteOpen = ref(false)
const fullscreenActive = ref(false)
const PALETTE_ORDER = ['Texte', 'Finance', 'Stock', 'Performance', 'Bonus']
const paletteGroups = computed(() => {
  const grouped = new Map<string, Array<Record<string, unknown>>>()
  for (const w of WIDGET_DEFS) {
    const key = w.category ?? 'Autres'
    const list = grouped.get(key) ?? []
    const meta = getWidgetPaletteMeta(w.type)
    list.push({
      type: w.type,
      title: w.title,
      icon: w.icon,
      help: w.help ?? 'Ajoute ce widget au canvas',
      forms: w.forms ?? [],
      formPicker: w.formPicker !== false,
      tags: meta.tags,
      dataType: meta.dataType,
      preview: meta.preview,
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
const activeTextWidgetId = ref<string | null>(null)
const selectedWidgetIds = ref<string[]>([])
const selectedWidgetIdSet = computed(() => new Set(selectedWidgetIds.value))
const marqueeSelection = ref<{
  startX: number
  startY: number
  x: number
  y: number
  additive: boolean
} | null>(null)
const isGroupSelectionActive = computed(() => editMode.value && selectedWidgetIds.value.length > 1)

const GROUP_RESIZE_HANDLES: Array<{ dir: ResizeDir; title: string }> = [
  { dir: 'ne', title: 'Redimensionner le groupe (coin haut droit)' },
  { dir: 'nw', title: 'Redimensionner le groupe (coin haut gauche)' },
  { dir: 'se', title: 'Redimensionner le groupe (coin bas droit)' },
  { dir: 'sw', title: 'Redimensionner le groupe (coin bas gauche)' },
]

function disarmWidget() {
  dragArmedId.value = null
}

function isWidgetSelected(id: string) {
  return selectedWidgetIdSet.value.has(id)
}

function setSelectedWidgets(ids: string[]) {
  const existing = new Set(widgets.value.map((item) => item.id))
  const unique = Array.from(new Set(ids)).filter((id) => existing.has(id))
  selectedWidgetIds.value = unique
}

function clearSelection() {
  selectedWidgetIds.value = []
}

function selectSingleWidget(id: string) {
  setSelectedWidgets([id])
}

function toggleWidgetSelection(id: string) {
  if (isWidgetSelected(id)) {
    setSelectedWidgets(selectedWidgetIds.value.filter((item) => item !== id))
    return
  }
  setSelectedWidgets([...selectedWidgetIds.value, id])
}

function setActiveTextWidget(id: string | null) {
  if (!id) {
    activeTextWidgetId.value = null
    return
  }
  const widget = getWidgetById(id)
  if (!widget || !isTextWidget(widget)) {
    activeTextWidgetId.value = null
    return
  }
  widget.z = ++zTop
  activeTextWidgetId.value = id
}

function eventToBoardPoint(event: PointerEvent) {
  const vp = viewportEl.value
  if (!vp) return null
  const rect = vp.getBoundingClientRect()
  return camera.boardPointFromViewport(event.clientX - rect.left, event.clientY - rect.top)
}

function normalizeMarqueeRect(state: { startX: number; startY: number; x: number; y: number }) {
  const left = Math.min(state.startX, state.x)
  const right = Math.max(state.startX, state.x)
  const top = Math.min(state.startY, state.y)
  const bottom = Math.max(state.startY, state.y)
  return {
    left: clamp(left, 0, BOARD_W),
    right: clamp(right, 0, BOARD_W),
    top: clamp(top, 0, BOARD_H),
    bottom: clamp(bottom, 0, BOARD_H),
  }
}

const marqueeSelectionStyle = computed(() => {
  const state = marqueeSelection.value
  if (!state) return null
  const rect = normalizeMarqueeRect(state)
  const width = Math.max(0, rect.right - rect.left)
  const height = Math.max(0, rect.bottom - rect.top)
  if (width < 1 && height < 1) return null
  return {
    left: `${rect.left}px`,
    top: `${rect.top}px`,
    width: `${width}px`,
    height: `${height}px`,
  }
})

function dragAssistLineStyle(line: DragAssistLine) {
  if (line.orientation === 'vertical') {
    const top = Math.min(line.start, line.end)
    return {
      left: `${line.position}px`,
      top: `${top}px`,
      height: `${Math.max(1, Math.abs(line.end - line.start))}px`,
    }
  }
  const left = Math.min(line.start, line.end)
  return {
    left: `${left}px`,
    top: `${line.position}px`,
    width: `${Math.max(1, Math.abs(line.end - line.start))}px`,
  }
}

function dragAssistGapStyle(gap: DragAssistGap) {
  if (gap.orientation === 'horizontal') {
    const left = Math.min(gap.start, gap.end)
    return {
      left: `${left}px`,
      top: `${gap.cross}px`,
      width: `${Math.max(1, Math.abs(gap.end - gap.start))}px`,
    }
  }
  const top = Math.min(gap.start, gap.end)
  return {
    left: `${gap.cross}px`,
    top: `${top}px`,
    height: `${Math.max(1, Math.abs(gap.end - gap.start))}px`,
  }
}

function liveWidgetRect(widget: Widget) {
  const groupMember = activeGroupResizeState?.members.find((member) => member.id === widget.id)
  if (groupMember) {
    return {
      x: groupMember.x,
      y: groupMember.y,
      w: groupMember.w,
      h: groupMember.h,
    }
  }
  const resizeState = resizeStates.get(widget.id)
  if (resizeState) {
    return {
      x: resizeState.x,
      y: resizeState.y,
      w: resizeState.w,
      h: resizeState.h,
    }
  }
  const dragState = dragStates.get(widget.id)
  if (dragState) {
    return {
      x: dragState.x,
      y: dragState.y,
      w: widget.w,
      h: widget.h,
    }
  }
  return {
    x: widget.x,
    y: widget.y,
    w: widget.w,
    h: widget.h,
  }
}

function selectedGroupBounds() {
  const ids =
    activeGroupResizeState?.members.length && activeGroupResizeState.members.length > 1
      ? activeGroupResizeState.members.map((member) => member.id)
      : activeDragIds.length > 1
        ? [...activeDragIds]
        : selectedWidgetIds.value.length > 1
          ? [...selectedWidgetIds.value]
          : []
  if (ids.length < 2) return null

  const members = ids
    .map((id) => getWidgetById(id))
    .filter((widget): widget is Widget => Boolean(widget))
  if (members.length < 2) return null

  let minX = Infinity
  let minY = Infinity
  let maxX = -Infinity
  let maxY = -Infinity
  let maxZ = 1
  for (const widget of members) {
    const rect = liveWidgetRect(widget)
    minX = Math.min(minX, rect.x)
    minY = Math.min(minY, rect.y)
    maxX = Math.max(maxX, rect.x + rect.w)
    maxY = Math.max(maxY, rect.y + rect.h)
    maxZ = Math.max(maxZ, Number(widget.z ?? 1))
  }

  const width = Math.max(0, maxX - minX)
  const height = Math.max(0, maxY - minY)
  if (width < 1 || height < 1) return null
  return { left: minX, top: minY, right: maxX, bottom: maxY, width, height, maxZ }
}

const groupSelectionFrameStyle = computed(() => {
  interactionTick.value
  if (!editMode.value) return null
  const bounds = selectedGroupBounds()
  if (!bounds) return null

  return {
    left: `${bounds.left}px`,
    top: `${bounds.top}px`,
    width: `${bounds.width}px`,
    height: `${bounds.height}px`,
    zIndex: String(bounds.maxZ + 24),
  }
})

function onGroupResizeHandleDown(dir: ResizeDir, event: PointerEvent) {
  if (!isGroupSelectionActive.value) return
  const anchorId = selectedWidgetIds.value[0]
  if (!anchorId) return
  startResize(anchorId, dir, event)
}

function widgetIntersectsMarquee(
  widget: Widget,
  rect: { left: number; right: number; top: number; bottom: number },
) {
  return !(
    widget.x > rect.right ||
    widget.x + widget.w < rect.left ||
    widget.y > rect.bottom ||
    widget.y + widget.h < rect.top
  )
}

function finishMarqueeSelection() {
  const state = marqueeSelection.value
  marqueeSelection.value = null
  setCanvasPanEnabled(true)
  if (!state) return
  const rect = normalizeMarqueeRect(state)
  const width = rect.right - rect.left
  const height = rect.bottom - rect.top
  if (width < 3 && height < 3) return

  const hitIds = widgets.value
    .filter((widget) => widgetIntersectsMarquee(widget, rect))
    .map((widget) => widget.id)
  if (state.additive) {
    setSelectedWidgets([...selectedWidgetIds.value, ...hitIds])
  } else {
    setSelectedWidgets(hitIds)
  }

  if (selectedWidgetIds.value.length === 1) {
    const onlyId = selectedWidgetIds.value[0]
    const onlyWidget = getWidgetById(onlyId)
    setActiveTextWidget(onlyWidget && isTextWidget(onlyWidget) ? onlyId : null)
  } else {
    setActiveTextWidget(null)
  }
}

function onMarqueePointerMove(event: PointerEvent) {
  const state = marqueeSelection.value
  if (!state) return
  const point = eventToBoardPoint(event)
  if (!point) return
  state.x = point.x
  state.y = point.y
}

function onMarqueePointerUp() {
  window.removeEventListener('pointermove', onMarqueePointerMove)
  finishMarqueeSelection()
}

function startMarqueeSelection(event: PointerEvent, additive = false) {
  if (event.button !== 0 && event.button !== 2) return
  const point = eventToBoardPoint(event)
  if (!point) return
  marqueeSelection.value = {
    startX: point.x,
    startY: point.y,
    x: point.x,
    y: point.y,
    additive,
  }
  setCanvasPanEnabled(false)
  window.addEventListener('pointermove', onMarqueePointerMove)
  window.addEventListener('pointerup', onMarqueePointerUp, { once: true })
  window.addEventListener('pointercancel', onMarqueePointerUp, { once: true })
}

function isBlankTextWidgetContent(widget: Widget | null | undefined) {
  if (!widget || !isTextWidget(widget)) return false
  const raw = String(widget.props?.content ?? '')
  const normalized = raw.replace(/\u00a0/g, ' ').trim()
  return normalized.length === 0
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
      ? to.value || from.value
      : (w.props?.asOf ?? w.props?.to ?? w.props?.from ?? to.value ?? from.value)
    const baseStr = typeof base === 'string' ? base : ''
    return { from: baseStr, to: baseStr }
  }
  return { from: widgetFrom(w), to: widgetTo(w) }
}

function widgetNeedsCategoryFilter(w: Widget | null | undefined) {
  if (!w) return false
  return getWidgetDef(w.type)?.categoryFilter === true
}

/* ===== Settings ===== */
const settingsOpen = ref(false)
const settingsWidgetId = ref<string | null>(null)

const settingsWidget = computed(
  () => (settingsWidgetId.value ? getWidgetById(settingsWidgetId.value) : null) ?? null,
)
const settingsDef = computed(() =>
  settingsWidget.value ? getWidgetDef(settingsWidget.value.type) : null,
)

const settingsTitle = computed(() => settingsWidget.value?.title ?? 'Reglages')
const SIMPLE_TEXT_SETTINGS = new Set(['content', 'fontSize', 'align', 'color'])
const SIMPLE_COMMON_HIDDEN_SETTINGS = new Set(['types'])

function simplifySettingsFields(widget: Widget | null, fields: Array<Record<string, unknown>>) {
  if (!widget) return fields
  if (isTextWidget(widget)) {
    return fields.filter((field) => SIMPLE_TEXT_SETTINGS.has(String(field?.key ?? '')))
  }
  return fields.filter((field) => !SIMPLE_COMMON_HIDDEN_SETTINGS.has(String(field?.key ?? '')))
}

const settingsFields = computed(() => {
  const def = settingsDef.value
  const base = def?.settings ?? []
  const hideRange = def?.hideGlobalRange === true

  const dateMode = def?.dateMode ?? 'range'
  const rangeFields =
    !hideRange && dateMode === 'range'
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

  const rawFields: Array<Record<string, unknown>> = [
    ...(!hideRange
      ? [
          {
            key: 'useGlobalRange',
            label: 'Utiliser periode globale',
            type: 'toggle',
            hint: 'Active par defaut',
          },
        ]
      : []),
    ...rangeFields,
    ...filterFields,
    ...mappedBase,
  ]

  return simplifySettingsFields(settingsWidget.value, rawFields)
})
const settingsModel = computed(() => {
  const base = settingsWidget.value?.props ?? {}
  const categories =
    Array.isArray(base.categories) && base.categories.length
      ? base.categories
      : typeof base.category === 'string' && base.category
        ? [base.category]
        : []
  const types =
    Array.isArray(base.types) && base.types.length
      ? base.types
      : typeof base.type === 'string' && base.type
        ? [base.type]
        : []
  return {
    ...base,
    useGlobalRange: base.useGlobalRange ?? true,
    from: base.from ?? localFrom.value,
    to: base.to ?? localTo.value,
    asOf: base.asOf ?? localTo.value,
    categories: categories.map((value) => String(value ?? '').trim()).filter(Boolean),
    types: types.map((value) => String(value ?? '').trim()).filter(Boolean),
  }
})

function openSettings(w: Widget) {
  settingsWidgetId.value = w.id
  settingsOpen.value = true
  if (widgetNeedsCategoryFilter(w)) {
    const range = widgetCategoryRange(w)
    void loadCategories(range.from, range.to)
  }
}
function closeSettings() {
  settingsOpen.value = false
  settingsWidgetId.value = null
}

const {
  shortcutHelpOpen,
  spacePanActive,
  shouldUsePanzoomExclude,
  setSpacePanState,
  onCanvasKeyDown,
  onCanvasKeyUp,
  onWindowBlur,
  onVisibilityChange,
} = useCanvasShortcuts({
  editMode,
  fullscreenActive,
  paletteOpen,
  settingsOpen,
  profileEditorOpen,
  dragArmedId,
  closeSettings,
  toggleEditMode,
  centerView,
  resetZoom,
  zoomOut,
  zoomIn,
  syncPanzoomExclude,
  setCanvasPanEnabled,
})

function onCanvasPointerDown(e: PointerEvent) {
  if (!editMode.value) return
  if (spacePanActive.value) return
  const isPrimary = e.button === 0
  const isSecondary = e.button === 2
  if (!isPrimary && !isSecondary) return
  const target = e.target as HTMLElement | null
  const insideBoard = Boolean(target?.closest('.board'))
  if (!insideBoard) return
  // Ignore controls inside widgets (actions/menu/inputs) so click intent
  // does not conflict with canvas selection logic.
  const interactiveTarget = Boolean(
    target?.closest(
      '.widget__actions, .widget-action-menu, .iconbtn, .text-toolbar, .text-inline-editor, [contenteditable="true"], button, a, input, select, textarea',
    ),
  )
  if (interactiveTarget) return
  const additive = e.shiftKey || e.ctrlKey || e.metaKey

  if (isSecondary) {
    e.preventDefault()
    e.stopPropagation()
    dragArmedId.value = null
    activeTextWidgetId.value = null
    if (!additive) {
      clearSelection()
    }
    startMarqueeSelection(e, additive)
    return
  }

  const widgetEl = target?.closest('.widget') as HTMLElement | null
  if (widgetEl) {
    const clickedId = String(widgetEl.dataset.id ?? '')
    if (!clickedId) return
    const dragAlreadyStarted =
      activeDragId === clickedId || activeDragIds.includes(clickedId) || dragStates.has(clickedId)
    if (additive) {
      toggleWidgetSelection(clickedId)
      if (!isWidgetSelected(clickedId) && activeTextWidgetId.value === clickedId) {
        activeTextWidgetId.value = null
      }
      return
    }
    if (dragAlreadyStarted) return

    if (!isWidgetSelected(clickedId)) {
      selectSingleWidget(clickedId)
    }

    if (selectedWidgetIds.value.length !== 1) {
      activeTextWidgetId.value = null
      return
    }

    const onlyId = selectedWidgetIds.value[0]
    const onlyWidget = getWidgetById(onlyId)
    if (!onlyWidget || !isTextWidget(onlyWidget)) {
      activeTextWidgetId.value = null
      return
    }
    if (activeTextWidgetId.value && activeTextWidgetId.value !== onlyId) {
      activeTextWidgetId.value = null
    }
    return
  }

  if (isGroupSelectionActive.value && !additive) {
    const point = eventToBoardPoint(e)
    const bounds = selectedGroupBounds()
    if (
      point &&
      bounds &&
      point.x >= bounds.left &&
      point.x <= bounds.right &&
      point.y >= bounds.top &&
      point.y <= bounds.bottom
    ) {
      const anchorId =
        selectedWidgetIds.value.find((id) => widgetEls.has(id)) ??
        selectedWidgetIds.value.find((id) => widgets.value.some((w) => w.id === id)) ??
        null
      if (anchorId) {
        startDrag(anchorId, e)
        return
      }
    }
  }

  dragArmedId.value = null
  activeTextWidgetId.value = null
  if (additive) {
    startMarqueeSelection(e, true)
    return
  }
  clearSelection()
  // Fond du board: on laisse panzoom gerer le drag pour deplacer la page.
  // La selection rectangle reste disponible en mode additif (Shift/Ctrl/Cmd + drag).
}

function onCanvasContextMenu(event: MouseEvent) {
  if (!editMode.value) return
  const target = event.target as HTMLElement | null
  if (!target?.closest('.board')) return
  event.preventDefault()
}

watch(
  () => activeTextWidgetId.value,
  (nextId, prevId) => {
    if (!prevId || prevId === nextId) return
    const previous = getWidgetById(prevId)
    if (!isBlankTextWidgetContent(previous)) return
    removeWidget(prevId)
  },
)

const TEXT_LAYOUT_KEYS = new Set([
  'content',
  'fontFamily',
  'fontSize',
  'size',
  'weight',
  'italic',
  'underline',
  'padding',
])

function patchTouchesTextLayout(patch: Record<string, unknown>) {
  return Object.keys(patch).some((key) => TEXT_LAYOUT_KEYS.has(key))
}

function textLayoutChanged(prev: Record<string, unknown>, next: Record<string, unknown>) {
  for (const key of TEXT_LAYOUT_KEYS) {
    if (prev[key] !== next[key]) return true
  }
  return false
}

function applySettings(newModel: Record<string, unknown>) {
  const w = settingsWidget.value
  if (!w) return
  const def = settingsDef.value
  const prevProps = { ...(w.props ?? {}) }
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
  if (!isTextWidget(w)) {
    next.types = []
    if (def?.hideGlobalRange === true) {
      next.useGlobalRange = true
    } else {
      next.useGlobalRange = next.useGlobalRange !== false
    }

    if (def?.dateMode === 'asOf') {
      if (!next.useGlobalRange) {
        const asOfRaw = typeof next.asOf === 'string' ? next.asOf : ''
        next.asOf = clampDate(asOfRaw || to.value || from.value || '')
      }
    } else if (next.useGlobalRange) {
      delete next.from
      delete next.to
    } else {
      const localFromVal = typeof next.from === 'string' && next.from ? next.from : localFrom.value
      const localToVal = typeof next.to === 'string' && next.to ? next.to : localTo.value
      next.from = clampDate(localFromVal)
      next.to = clampDate(localToVal)
      if (typeof next.from === 'string' && typeof next.to === 'string' && next.from > next.to) {
        const tmp = next.from
        next.from = next.to
        next.to = tmp
      }
    }
  }
  w.props = next
  const shouldFitText = isTextWidget(w) && textLayoutChanged(prevProps, next)
  closeSettings()
  if (shouldFitText) {
    fitTextWidgetAfterRender(w, widgetEls.get(w.id) ?? null, {
      preserveWidth: shouldPreserveTextWidth(w, 'content'),
    })
  }
  scheduleSave()
}
function updateTextWidgetProps(id: string, patch: Record<string, unknown>) {
  const w = getWidgetById(id)
  if (!w || !isTextWidget(w)) return
  const shouldFitText = patchTouchesTextLayout(patch)
  w.props = {
    ...(w.props ?? {}),
    ...patch,
  }
  if (shouldFitText) {
    fitTextWidgetAfterRender(w, widgetEls.get(w.id) ?? null, {
      preserveWidth: shouldPreserveTextWidth(w, 'content'),
    })
  }
  scheduleSave()
}

function updateWidgetProps(id: string, patch: Record<string, unknown>) {
  const w = getWidgetById(id)
  if (!w || !patch || typeof patch !== 'object') return
  w.props = {
    ...(w.props ?? {}),
    ...patch,
  }
  scheduleSave()
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
const profileSwitcherExpanded = ref(true)
const visibleRect = ref<{ left: number; top: number; right: number; bottom: number } | null>(null)
let visibleRectRaf: number | null = null
const cameraInteracting = ref(false)
const CAMERA_IDLE_DELAY_MS = 160
let cameraInteractionTimer: number | null = null

function clearVisibleRectRaf() {
  if (visibleRectRaf != null) {
    cancelAnimationFrame(visibleRectRaf)
    visibleRectRaf = null
  }
}

function clearCameraInteractionTimer() {
  if (cameraInteractionTimer != null) {
    window.clearTimeout(cameraInteractionTimer)
    cameraInteractionTimer = null
  }
}

function markCameraInteracting() {
  cameraInteracting.value = true
  clearCameraInteractionTimer()
  cameraInteractionTimer = window.setTimeout(() => {
    cameraInteracting.value = false
    cameraInteractionTimer = null
  }, CAMERA_IDLE_DELAY_MS)
}

function updateVisibleRectNow() {
  const vp = viewportEl.value
  if (!vp) {
    visibleRect.value = null
    return
  }

  const p1 = camera.boardPointFromViewport(0, 0)
  const p2 = camera.boardPointFromViewport(vp.clientWidth, vp.clientHeight)
  const left = Math.min(p1.x, p2.x)
  const right = Math.max(p1.x, p2.x)
  const top = Math.min(p1.y, p2.y)
  const bottom = Math.max(p1.y, p2.y)
  const safeScale = Math.max(Number(scale.value) || 1, 0.15)
  const pad = clamp(420 / safeScale, 220, 860)

  visibleRect.value = {
    left: clamp(left - pad, 0, BOARD_W),
    top: clamp(top - pad, 0, BOARD_H),
    right: clamp(right + pad, 0, BOARD_W),
    bottom: clamp(bottom + pad, 0, BOARD_H),
  }
}

function scheduleVisibleRectUpdate() {
  if (visibleRectRaf != null) return
  visibleRectRaf = requestAnimationFrame(() => {
    visibleRectRaf = null
    updateVisibleRectNow()
  })
}

function normalizeVisibleTextWidgets(preserveWidth = true) {
  const textWidgets = widgets.value.filter((widget) => isTextWidget(widget))
  if (!textWidgets.length) return
  for (const widget of textWidgets) {
    fitTextWidgetAfterRender(widget, widgetEls.get(widget.id) ?? null, { preserveWidth })
  }
}

function toggleProfileSwitcher() {
  profileSwitcherExpanded.value = !profileSwitcherExpanded.value
}

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
      scheduleVisibleRectUpdate()
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
  markCameraInteracting()
  fitToWidgets(80, true)
  scheduleVisibleRectUpdate()
}

function zoomIn() {
  markCameraInteracting()
  camera.zoomIn()
  scheduleVisibleRectUpdate()
}
function zoomOut() {
  markCameraInteracting()
  camera.zoomOut()
  scheduleVisibleRectUpdate()
}
function resetZoom() {
  markCameraInteracting()
  fitToWidgets(120, true)
  scheduleVisibleRectUpdate()
}
function zoomToFitContent() {
  markCameraInteracting()
  fitToWidgets(80, true)
  scheduleVisibleRectUpdate()
}

/* ===== Drag ===== */
const widgetEls = new Map<string, HTMLElement>()
type SnapAnchor = 'start' | 'center' | 'end'
type SnapAxis = 'x' | 'y'
type ViewProjection = {
  offsetX: number
  offsetY: number
  scaleX: number
  scaleY: number
}
type DragAssistLine = {
  id: string
  orientation: 'horizontal' | 'vertical'
  kind: 'edge' | 'center'
  source: 'board' | 'widget'
  position: number
  start: number
  end: number
}
type DragAssistGap = {
  id: string
  orientation: 'horizontal' | 'vertical'
  start: number
  end: number
  cross: number
  label: string
}

type DragState = {
  x: number
  y: number
  scale: number
  lastX: number
  lastY: number
  raf: number | null
  widget: Widget
  el: HTMLElement | null
  xTargets: SnapTarget[]
  yTargets: SnapTarget[]
  isText: boolean
  projection: ViewProjection | null
}
const dragStates = new Map<string, DragState>()
let activeDragId: string | null = null
let activeDragIds: string[] = []
let dragPointerMoveRaf: number | null = null
let queuedDragPointerEvent: PointerEvent | null = null
let zTop = 10
const snapGuides = ref<{ x: number | null; y: number | null }>({ x: null, y: null })
const dragAssistLines = ref<DragAssistLine[]>([])
const dragAssistGaps = ref<DragAssistGap[]>([])
const interactionTick = ref(0)

type ResizeState = {
  x: number
  y: number
  w: number
  h: number
  scale: number
  dir: ResizeDir
  pointerStartX: number
  pointerStartY: number
  originX: number
  originY: number
  originW: number
  originH: number
  originCenterX: number
  originCenterY: number
  aspect: number
  originFontSize: number
  raf: number | null
  widget: Widget
  el: HTMLElement
  metricsEl: HTMLElement | null
  xTargets: SnapTarget[]
  yTargets: SnapTarget[]
  widthTargets: number[]
  heightTargets: number[]
  minW: number
  minH: number
  maxW: number
  maxH: number
  isText: boolean
  previewFontSize: number | null
}
const resizeStates = new Map<string, ResizeState>()
let activeResizeId: string | null = null
let activeResizeCursorClass: string | null = null
let resizePointerMoveRaf: number | null = null
let queuedResizePointerEvent: PointerEvent | null = null

type GroupResizeMember = {
  id: string
  widget: Widget
  el: HTMLElement | null
  metricsEl: HTMLElement | null
  originX: number
  originY: number
  originW: number
  originH: number
  originFontSize: number
  minW: number
  minH: number
  isText: boolean
  x: number
  y: number
  w: number
  h: number
  previewFontSize: number | null
}

type GroupResizeState = {
  dir: ResizeDir
  scale: number
  pointerStartX: number
  pointerStartY: number
  originLeft: number
  originTop: number
  originRight: number
  originBottom: number
  originW: number
  originH: number
  originCenterX: number
  originCenterY: number
  aspect: number
  minScaleX: number
  minScaleY: number
  members: GroupResizeMember[]
  raf: number | null
}
let activeGroupResizeState: GroupResizeState | null = null

type TextScaleState = {
  widgetId: string
  lastX: number
  lastY: number
  pendingDelta: number
  raf: number | null
  appliedScale: number
}
let activeTextScaleState: TextScaleState | null = null

function cursorClassForResizeDir(dir: ResizeDir) {
  if (dir === 'n' || dir === 's') return 'canvas-resize-cursor--ns'
  if (dir === 'e' || dir === 'w') return 'canvas-resize-cursor--ew'
  if (dir === 'nw' || dir === 'se') return 'canvas-resize-cursor--nwse'
  return 'canvas-resize-cursor--nesw'
}

function setGlobalResizeCursor(dir: ResizeDir) {
  if (typeof document === 'undefined') return
  const nextClass = cursorClassForResizeDir(dir)
  if (activeResizeCursorClass === nextClass) return
  clearGlobalResizeCursor()
  document.body.classList.add('canvas-resize-cursor')
  document.body.classList.add(nextClass)
  activeResizeCursorClass = nextClass
}

function clearGlobalResizeCursor() {
  if (typeof document === 'undefined') return
  document.body.classList.remove('canvas-resize-cursor')
  if (activeResizeCursorClass) {
    document.body.classList.remove(activeResizeCursorClass)
    activeResizeCursorClass = null
  }
}

function widgetIntersectsVisibleRect(
  w: Widget,
  rect: { left: number; top: number; right: number; bottom: number },
) {
  return !(w.x > rect.right || w.x + w.w < rect.left || w.y > rect.bottom || w.y + w.h < rect.top)
}

const visibleWidgets = computed(() => {
  if (cameraInteracting.value) return widgets.value
  const rect = visibleRect.value
  if (!rect) return widgets.value

  const pinned = new Set<string>()
  if (activeDragId) pinned.add(activeDragId)
  for (const id of activeDragIds) pinned.add(id)
  if (activeResizeId) pinned.add(activeResizeId)
  if (activeGroupResizeState) {
    for (const member of activeGroupResizeState.members) pinned.add(member.id)
  }
  if (activeTextScaleState?.widgetId) pinned.add(activeTextScaleState.widgetId)
  if (dragArmedId.value) pinned.add(dragArmedId.value)

  return widgets.value.filter((w) => pinned.has(w.id) || widgetIntersectsVisibleRect(w, rect))
})

function setWidgetRef(id: string, c: any) {
  // c est le composant; on garde la derniere ref DOM valide, et on ne purge
  // que lorsqu'on recoit null (demontage). Cela evite de perdre la ref pendant
  // les phases de montage/teleport et de casser le drag.
  if (c == null) {
    widgetEls.delete(id)
    return
  }
  const el = (c?.root?.value ?? c?.$el ?? null) as any
  if (el && el.nodeType === 1 && el.classList) {
    const nextEl = el as HTMLElement
    widgetEls.set(id, nextEl)

    const dragState = dragStates.get(id)
    if (dragState) {
      dragState.el = nextEl
      nextEl.classList.add('is-dragging')
      applyWidgetTransform(nextEl, dragState.x, dragState.y)
    }

    const resizeState = resizeStates.get(id)
    if (resizeState) {
      resizeState.el = nextEl
      resizeState.metricsEl = readResizeMetricsEl(nextEl)
      nextEl.classList.add('is-resizing')
      applyWidgetResizePreview(
        nextEl,
        resizeState.originW,
        resizeState.originH,
        resizeState.x,
        resizeState.y,
        resizeState.w,
        resizeState.h,
      )
      updateResizeMetricsText(resizeState.metricsEl, resizeState.w, resizeState.h)
    }

    const groupMember = activeGroupResizeState?.members.find((member) => member.id === id)
    if (groupMember) {
      groupMember.el = nextEl
      groupMember.metricsEl = readResizeMetricsEl(nextEl)
      nextEl.classList.add('is-resizing')
      applyWidgetResizePreview(
        nextEl,
        groupMember.originW,
        groupMember.originH,
        groupMember.x,
        groupMember.y,
        groupMember.w,
        groupMember.h,
      )
      updateResizeMetricsText(groupMember.metricsEl, groupMember.w, groupMember.h)
    }
  }
}

function applyWidgetDOM(el: HTMLElement, w: Widget) {
  el.style.width = `${w.w}px`
  el.style.height = `${w.h}px`
  el.style.transform = `translate3d(${w.x}px, ${w.y}px, 0)`
}

function applyWidgetTransform(el: HTMLElement, x: number, y: number) {
  el.style.transform = `translate3d(${x}px, ${y}px, 0)`
}

function applyWidgetDOMRect(el: HTMLElement, x: number, y: number, w: number, h: number) {
  el.style.width = `${w}px`
  el.style.height = `${h}px`
  el.style.transform = `translate3d(${x}px, ${y}px, 0)`
}

function applyWidgetResizePreview(
  el: HTMLElement,
  originW: number,
  originH: number,
  x: number,
  y: number,
  w: number,
  h: number,
) {
  const safeOriginW = Math.max(originW, 1)
  const safeOriginH = Math.max(originH, 1)
  const scaleX = clamp(w / safeOriginW, 0.01, 24)
  const scaleY = clamp(h / safeOriginH, 0.01, 24)
  el.style.width = `${safeOriginW}px`
  el.style.height = `${safeOriginH}px`
  el.style.transform = `translate3d(${x}px, ${y}px, 0) scale(${scaleX}, ${scaleY})`
}

function readResizeMetricsEl(el: HTMLElement | null) {
  if (!el) return null
  return el.querySelector('.resize-metrics') as HTMLElement | null
}

function updateResizeMetricsText(el: HTMLElement | null, w: number, h: number) {
  if (!el) return
  const next = `${Math.round(w)} x ${Math.round(h)}`
  if (el.textContent !== next) el.textContent = next
}

function clampWidgetPosition(x: number, y: number, w: Widget) {
  return {
    x: clamp(x, 0, BOARD_W - w.w),
    y: clamp(y, 0, BOARD_H - w.h),
  }
}

function setSnapGuides(x: number | null, y: number | null) {
  if (snapGuides.value.x === x && snapGuides.value.y === y) return
  snapGuides.value = { x, y }
}

type SnapTarget = {
  value: number
  guide: number
  source: 'board' | 'widget'
  anchor: SnapAnchor
  widgetId?: string
  spanStart?: number
  spanEnd?: number
  crossCenter?: number
}

type SnapOriginResult = {
  value: number
  guide: number | null
  target: SnapTarget | null
  anchor: SnapAnchor | null
}

function shouldUseSmartSnap(event: PointerEvent) {
  return !(event.ctrlKey || event.metaKey)
}

function roundAssistCoord(value: number) {
  return Math.round(value * 10) / 10
}

function sameDragAssistLines(a: DragAssistLine[], b: DragAssistLine[]) {
  if (a.length !== b.length) return false
  for (let i = 0; i < a.length; i += 1) {
    const left = a[i]
    const right = b[i]
    if (
      left.id !== right.id ||
      left.orientation !== right.orientation ||
      left.kind !== right.kind ||
      left.source !== right.source ||
      left.position !== right.position ||
      left.start !== right.start ||
      left.end !== right.end
    ) {
      return false
    }
  }
  return true
}

function sameDragAssistGaps(a: DragAssistGap[], b: DragAssistGap[]) {
  if (a.length !== b.length) return false
  for (let i = 0; i < a.length; i += 1) {
    const left = a[i]
    const right = b[i]
    if (
      left.id !== right.id ||
      left.orientation !== right.orientation ||
      left.start !== right.start ||
      left.end !== right.end ||
      left.cross !== right.cross ||
      left.label !== right.label
    ) {
      return false
    }
  }
  return true
}

function setDragAssist(lines: DragAssistLine[], gaps: DragAssistGap[]) {
  if (
    sameDragAssistLines(lines, dragAssistLines.value) &&
    sameDragAssistGaps(gaps, dragAssistGaps.value)
  ) {
    return
  }
  dragAssistLines.value = lines
  dragAssistGaps.value = gaps
}

function clearDragAssist() {
  if (!dragAssistLines.value.length && !dragAssistGaps.value.length) return
  dragAssistLines.value = []
  dragAssistGaps.value = []
}

function getViewportProjection(): ViewProjection | null {
  const vp = viewportEl.value
  const board = boardEl.value
  if (!vp || !board) return null

  const vpRect = vp.getBoundingClientRect()
  const boardRect = board.getBoundingClientRect()
  if (vpRect.width <= 0 || vpRect.height <= 0) return null

  const scaleX = boardRect.width / BOARD_W
  const scaleY = boardRect.height / BOARD_H
  if (!Number.isFinite(scaleX) || !Number.isFinite(scaleY) || scaleX <= 0 || scaleY <= 0) {
    return null
  }

  return {
    offsetX: boardRect.left - vpRect.left,
    offsetY: boardRect.top - vpRect.top,
    scaleX,
    scaleY,
  }
}

function boardToViewportX(value: number, projection: ViewProjection) {
  return roundAssistCoord(projection.offsetX + value * projection.scaleX)
}

function boardToViewportY(value: number, projection: ViewProjection) {
  return roundAssistCoord(projection.offsetY + value * projection.scaleY)
}

function snapAnchorOffset(size: number, anchor: SnapAnchor) {
  if (anchor === 'center') return size / 2
  if (anchor === 'end') return size
  return 0
}

function overlapLength(aStart: number, aEnd: number, bStart: number, bEnd: number) {
  return Math.min(aEnd, bEnd) - Math.max(aStart, bStart)
}

function buildAlignmentAssistLine(
  axis: SnapAxis,
  rect: { x: number; y: number; w: number; h: number },
  match: SnapOriginResult,
  projection: ViewProjection,
): DragAssistLine | null {
  if (!match.target || !match.anchor || match.guide === null) return null

  const activeAnchorValue =
    axis === 'x'
      ? rect.x + snapAnchorOffset(rect.w, match.anchor)
      : rect.y + snapAnchorOffset(rect.h, match.anchor)
  if (Math.abs(activeAnchorValue - match.target.value) > SNAP_DIST + 0.75) return null

  const crossRectStart = axis === 'x' ? rect.y : rect.x
  const crossRectEnd = axis === 'x' ? rect.y + rect.h : rect.x + rect.w
  const crossLimit = axis === 'x' ? BOARD_H : BOARD_W
  const targetStart =
    match.target.source === 'board' ? crossRectStart : (match.target.spanStart ?? crossRectStart)
  const targetEnd =
    match.target.source === 'board' ? crossRectEnd : (match.target.spanEnd ?? crossRectEnd)
  const lineStartBoard = clamp(
    Math.min(crossRectStart, targetStart) - DRAG_ASSIST_GUIDE_PAD,
    0,
    crossLimit,
  )
  const lineEndBoard = clamp(
    Math.max(crossRectEnd, targetEnd) + DRAG_ASSIST_GUIDE_PAD,
    0,
    crossLimit,
  )
  if (lineEndBoard - lineStartBoard < 1) return null

  const kind: DragAssistLine['kind'] =
    match.anchor === 'center' || match.target.anchor === 'center' ? 'center' : 'edge'

  if (axis === 'x') {
    return {
      id: `x-${match.target.source}-${match.target.widgetId ?? 'board'}-${match.target.anchor}-${match.anchor}`,
      orientation: 'vertical',
      kind,
      source: match.target.source,
      position: boardToViewportX(match.guide, projection),
      start: boardToViewportY(lineStartBoard, projection),
      end: boardToViewportY(lineEndBoard, projection),
    }
  }

  return {
    id: `y-${match.target.source}-${match.target.widgetId ?? 'board'}-${match.target.anchor}-${match.anchor}`,
    orientation: 'horizontal',
    kind,
    source: match.target.source,
    position: boardToViewportY(match.guide, projection),
    start: boardToViewportX(lineStartBoard, projection),
    end: boardToViewportX(lineEndBoard, projection),
  }
}

type GapAssistCandidate = {
  distance: number
  overlap: number
  gap: DragAssistGap
}

function pickGapCandidate(current: GapAssistCandidate | null, candidate: GapAssistCandidate) {
  if (!current) return candidate
  if (candidate.distance < current.distance) return candidate
  if (candidate.distance === current.distance && candidate.overlap > current.overlap) {
    return candidate
  }
  return current
}

function collectNearestGapAssist(
  activeId: string,
  rect: { x: number; y: number; w: number; h: number },
  projection: ViewProjection,
) {
  let bestHorizontal: GapAssistCandidate | null = null
  let bestVertical: GapAssistCandidate | null = null

  for (const other of widgets.value) {
    if (other.id === activeId) continue

    const overlapY = overlapLength(rect.y, rect.y + rect.h, other.y, other.y + other.h)
    if (overlapY >= DRAG_ASSIST_MIN_OVERLAP) {
      let startBoard = 0
      let endBoard = 0
      if (other.x + other.w <= rect.x) {
        startBoard = other.x + other.w
        endBoard = rect.x
      } else if (rect.x + rect.w <= other.x) {
        startBoard = rect.x + rect.w
        endBoard = other.x
      }

      const distance = Math.abs(endBoard - startBoard)
      if (distance >= DRAG_ASSIST_GAP_MIN && distance <= DRAG_ASSIST_GAP_MAX) {
        const overlapStart = Math.max(rect.y, other.y)
        const overlapEnd = Math.min(rect.y + rect.h, other.y + other.h)
        const crossBoard = (overlapStart + overlapEnd) / 2
        bestHorizontal = pickGapCandidate(bestHorizontal, {
          distance,
          overlap: overlapY,
          gap: {
            id: `gap-h-${other.id}`,
            orientation: 'horizontal',
            start: boardToViewportX(startBoard, projection),
            end: boardToViewportX(endBoard, projection),
            cross: boardToViewportY(crossBoard, projection),
            label: `${Math.round(distance)} px`,
          },
        })
      }
    }

    const overlapX = overlapLength(rect.x, rect.x + rect.w, other.x, other.x + other.w)
    if (overlapX >= DRAG_ASSIST_MIN_OVERLAP) {
      let startBoard = 0
      let endBoard = 0
      if (other.y + other.h <= rect.y) {
        startBoard = other.y + other.h
        endBoard = rect.y
      } else if (rect.y + rect.h <= other.y) {
        startBoard = rect.y + rect.h
        endBoard = other.y
      }

      const distance = Math.abs(endBoard - startBoard)
      if (distance >= DRAG_ASSIST_GAP_MIN && distance <= DRAG_ASSIST_GAP_MAX) {
        const overlapStart = Math.max(rect.x, other.x)
        const overlapEnd = Math.min(rect.x + rect.w, other.x + other.w)
        const crossBoard = (overlapStart + overlapEnd) / 2
        bestVertical = pickGapCandidate(bestVertical, {
          distance,
          overlap: overlapX,
          gap: {
            id: `gap-v-${other.id}`,
            orientation: 'vertical',
            start: boardToViewportY(startBoard, projection),
            end: boardToViewportY(endBoard, projection),
            cross: boardToViewportX(crossBoard, projection),
            label: `${Math.round(distance)} px`,
          },
        })
      }
    }
  }

  const out: DragAssistGap[] = []
  if (bestHorizontal) out.push(bestHorizontal.gap)
  if (bestVertical) out.push(bestVertical.gap)
  return out
}

function updateDragAssist(state: DragState, snapX: SnapOriginResult, snapY: SnapOriginResult) {
  const projection = state.projection ?? getViewportProjection()
  if (!projection) {
    clearDragAssist()
    return
  }
  state.projection = projection

  const rect = {
    x: state.x,
    y: state.y,
    w: state.widget.w,
    h: state.widget.h,
  }

  const lines: DragAssistLine[] = []
  const verticalLine = buildAlignmentAssistLine('x', rect, snapX, projection)
  if (verticalLine) lines.push(verticalLine)
  const horizontalLine = buildAlignmentAssistLine('y', rect, snapY, projection)
  if (horizontalLine) lines.push(horizontalLine)

  const gaps = collectNearestGapAssist(state.widget.id, rect, projection)
  setDragAssist(lines, gaps)
}

function axisSign(dir: ResizeDir, positive: 'e' | 's', negative: 'w' | 'n') {
  if (dir.includes(positive)) return 1
  if (dir.includes(negative)) return -1
  return 0
}

function collectAxisTargets(activeId: string, axis: 'x' | 'y'): SnapTarget[] {
  const limit = axis === 'x' ? BOARD_W : BOARD_H
  const crossLimit = axis === 'x' ? BOARD_H : BOARD_W
  const targets: SnapTarget[] = [
    {
      value: 0,
      guide: 0,
      source: 'board',
      anchor: 'start',
      spanStart: 0,
      spanEnd: crossLimit,
      crossCenter: crossLimit / 2,
    },
    {
      value: limit / 2,
      guide: limit / 2,
      source: 'board',
      anchor: 'center',
      spanStart: 0,
      spanEnd: crossLimit,
      crossCenter: crossLimit / 2,
    },
    {
      value: limit,
      guide: limit,
      source: 'board',
      anchor: 'end',
      spanStart: 0,
      spanEnd: crossLimit,
      crossCenter: crossLimit / 2,
    },
  ]

  for (const other of widgets.value) {
    if (other.id === activeId) continue
    const start = axis === 'x' ? other.x : other.y
    const size = axis === 'x' ? other.w : other.h
    const spanStart = axis === 'x' ? other.y : other.x
    const spanEnd = spanStart + (axis === 'x' ? other.h : other.w)
    const crossCenter = (spanStart + spanEnd) / 2
    targets.push(
      {
        value: start,
        guide: start,
        source: 'widget',
        anchor: 'start',
        widgetId: other.id,
        spanStart,
        spanEnd,
        crossCenter,
      },
      {
        value: start + size / 2,
        guide: start + size / 2,
        source: 'widget',
        anchor: 'center',
        widgetId: other.id,
        spanStart,
        spanEnd,
        crossCenter,
      },
      {
        value: start + size,
        guide: start + size,
        source: 'widget',
        anchor: 'end',
        widgetId: other.id,
        spanStart,
        spanEnd,
        crossCenter,
      },
    )
  }

  return targets
}

function collectSizeTargets(activeId: string, axis: 'w' | 'h') {
  const out: number[] = []
  for (const other of widgets.value) {
    if (other.id === activeId) continue
    const size = axis === 'w' ? other.w : other.h
    if (!Number.isFinite(size) || size <= 0) continue
    out.push(Math.round(size))
  }
  return Array.from(new Set(out)).sort((a, b) => a - b)
}

function snapOriginToTargets(
  origin: number,
  size: number,
  targets: SnapTarget[],
  enabled: boolean,
  activeCrossCenter: number,
): SnapOriginResult {
  if (!enabled) return { value: origin, guide: null, target: null, anchor: null }

  const anchors: Array<{ value: number; anchor: SnapAnchor }> = [
    { value: 0, anchor: 'start' },
    { value: size / 2, anchor: 'center' },
    { value: size, anchor: 'end' },
  ]
  let bestValue = origin
  let bestGuide: number | null = null
  let bestTarget: SnapTarget | null = null
  let bestAnchor: SnapAnchor | null = null
  let bestDiff = SNAP_DIST + 1
  let bestTargetPriority = Number.POSITIVE_INFINITY
  let bestCrossDistance = Number.POSITIVE_INFINITY

  for (const anchor of anchors) {
    for (const target of targets) {
      const diff = Math.abs(origin + anchor.value - target.value)
      const targetPriority = target.source === 'widget' ? 0 : 1
      const targetCross =
        Number.isFinite(target.crossCenter) && Number.isFinite(activeCrossCenter)
          ? Math.abs((target.crossCenter as number) - activeCrossCenter)
          : Number.POSITIVE_INFINITY
      const hasSameDiff = Math.abs(diff - bestDiff) < 0.001
      const isBetter =
        diff < bestDiff ||
        (hasSameDiff && targetPriority < bestTargetPriority) ||
        (hasSameDiff && targetPriority === bestTargetPriority && targetCross < bestCrossDistance)
      if (isBetter) {
        bestDiff = diff
        bestValue = target.value - anchor.value
        bestGuide = target.guide
        bestTarget = target
        bestAnchor = anchor.anchor
        bestTargetPriority = targetPriority
        bestCrossDistance = targetCross
      }
    }
  }

  if (bestDiff <= SNAP_DIST) {
    return {
      value: bestValue,
      guide: bestGuide,
      target: bestTarget,
      anchor: bestAnchor,
    }
  }

  const gridValue = snap(origin)
  if (Math.abs(gridValue - origin) <= GRID_SNAP_DIST) {
    return { value: gridValue, guide: gridValue, target: null, anchor: null }
  }

  return { value: origin, guide: null, target: null, anchor: null }
}

function snapEdgeToTargets(edge: number, targets: SnapTarget[], enabled: boolean) {
  if (!enabled) return { value: edge, guide: null as number | null }

  let bestValue = edge
  let bestGuide: number | null = null
  let bestDiff = SNAP_DIST + 1
  for (const target of targets) {
    const diff = Math.abs(edge - target.value)
    if (diff < bestDiff) {
      bestDiff = diff
      bestValue = target.value
      bestGuide = target.guide
    }
  }

  if (bestDiff <= SNAP_DIST) {
    return { value: bestValue, guide: bestGuide }
  }

  const gridValue = snap(edge)
  if (Math.abs(gridValue - edge) <= GRID_SNAP_DIST) {
    return { value: gridValue, guide: gridValue }
  }

  return { value: edge, guide: null as number | null }
}

function snapSizeToTargets(value: number, targets: number[], enabled: boolean) {
  if (!enabled || !targets.length) return { value, snapped: false }

  let best = value
  let bestDiff = SIZE_SNAP_DIST + 1
  for (const target of targets) {
    const diff = Math.abs(value - target)
    if (diff < bestDiff) {
      best = target
      bestDiff = diff
    }
  }

  if (bestDiff <= SIZE_SNAP_DIST) {
    return { value: best, snapped: true }
  }

  const gridValue = snap(value)
  if (Math.abs(gridValue - value) <= GRID_SNAP_DIST) {
    return { value: gridValue, snapped: true }
  }

  return { value, snapped: false }
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

function bumpInteractionTick() {
  interactionTick.value = (interactionTick.value + 1) % 1_000_000_000
}

function scheduleDragApply(state: DragState) {
  if (state.raf) return
  state.raf = requestAnimationFrame(() => {
    state.raf = null
    if (state.el) {
      applyWidgetTransform(state.el, state.x, state.y)
    }
    if (activeDragIds.length > 1) {
      bumpInteractionTick()
    }
  })
}

function clearResizeState(id: string) {
  const state = resizeStates.get(id)
  if (!state) return
  if (state.raf) cancelAnimationFrame(state.raf)
  applyWidgetDOM(state.el, state.widget)
  resizeStates.delete(id)
}

function clearGroupResizeState() {
  const state = activeGroupResizeState
  if (!state) return
  if (state.raf) cancelAnimationFrame(state.raf)
  for (const member of state.members) {
    member.el?.classList.remove('is-resizing')
    if (member.el) applyWidgetDOM(member.el, member.widget)
  }
  activeGroupResizeState = null
  bumpInteractionTick()
}

function scheduleGroupResizeApply(state: GroupResizeState) {
  if (state.raf) return
  state.raf = requestAnimationFrame(() => {
    state.raf = null
    for (const member of state.members) {
      if (!member.el) continue
      applyWidgetResizePreview(
        member.el,
        member.originW,
        member.originH,
        member.x,
        member.y,
        member.w,
        member.h,
      )
      updateResizeMetricsText(member.metricsEl, member.w, member.h)
    }
    bumpInteractionTick()
  })
}

function clearTextScaleState() {
  if (activeTextScaleState?.raf) {
    cancelAnimationFrame(activeTextScaleState.raf)
  }
  activeTextScaleState = null
}

function scheduleResizeApply(state: ResizeState) {
  if (state.raf) return
  state.raf = requestAnimationFrame(() => {
    state.raf = null
    applyWidgetResizePreview(
      state.el,
      state.originW,
      state.originH,
      state.x,
      state.y,
      state.w,
      state.h,
    )
    updateResizeMetricsText(state.metricsEl, state.w, state.h)
  })
}

function startDrag(id: string, event: PointerEvent) {
  if (!editMode.value) return
  if (spacePanActive.value) return
  if (marqueeSelection.value) return
  setSnapGuides(null, null)
  clearDragAssist()
  const additive = event.shiftKey || event.ctrlKey || event.metaKey
  if (additive) {
    return
  }
  const w = getWidgetById(id)
  let el = widgetEls.get(id)
  // Fallback: recupere l'element DOM depuis l'event si la ref n'est pas encore disponible.
  if (!el) {
    const target = (event.target as HTMLElement | null)?.closest('.widget') as HTMLElement | null
    if (target) {
      widgetEls.set(id, target)
      el = target
    }
  }
  if (!w || !el) return
  const wasSelected = isWidgetSelected(id)
  if (!wasSelected) {
    selectSingleWidget(id)
  }
  const dragIds = selectedWidgetIds.value.includes(id) ? [...selectedWidgetIds.value] : [id]
  if (!dragIds.length) {
    selectSingleWidget(id)
    dragIds.push(id)
  }
  const shouldActivateText = dragIds.length === 1 && isTextWidget(w)
  const shouldPrimeTextSelection =
    shouldActivateText && (!wasSelected || activeTextWidgetId.value !== id)
  if (shouldPrimeTextSelection) {
    setActiveTextWidget(id)
    dragArmedId.value = id
    return
  }
  setActiveTextWidget(shouldActivateText ? id : null)

  event.preventDefault()
  event.stopPropagation()

  dragArmedId.value = id
  for (const dragId of dragIds) {
    const widget = getWidgetById(dragId)
    if (!widget) continue
    widget.z = ++zTop
  }
  activeDragId = id
  activeDragIds = dragIds
  bumpInteractionTick()

  const s = Number(camera.scale.value || 1)
  const projection = getViewportProjection()
  for (const dragId of dragIds) {
    const dragWidget = getWidgetById(dragId)
    if (!dragWidget) continue
    let dragEl = widgetEls.get(dragId)
    if (!dragEl && dragId === id) dragEl = el
    const isText = isTextWidget(dragWidget)
    dragStates.set(dragId, {
      x: dragWidget.x,
      y: dragWidget.y,
      scale: s > 0 ? s : 1,
      lastX: event.clientX,
      lastY: event.clientY,
      raf: null,
      widget: dragWidget,
      el: dragEl ?? null,
      xTargets: collectAxisTargets(dragId, 'x'),
      yTargets: collectAxisTargets(dragId, 'y'),
      isText,
      projection,
    })
    dragEl?.classList.add('is-dragging')
  }
  setCanvasPanEnabled(false)

  window.addEventListener('pointermove', onGlobalPointerMove)
  window.addEventListener('pointerup', onGlobalPointerUp, { once: true })
  window.addEventListener('pointercancel', onGlobalPointerUp, { once: true })
}

function applyGlobalPointerMove(event: PointerEvent) {
  if (!activeDragId) return
  const state = dragStates.get(activeDragId)
  if (!state) return
  const w = state.widget

  const dx = event.clientX - state.lastX
  const dy = event.clientY - state.lastY
  for (const dragId of activeDragIds) {
    const dragState = dragStates.get(dragId)
    if (!dragState) continue
    dragState.lastX = event.clientX
    dragState.lastY = event.clientY
  }

  if (activeDragIds.length > 1) {
    const states = activeDragIds
      .map((dragId) => dragStates.get(dragId))
      .filter((dragState): dragState is DragState => Boolean(dragState))
    if (!states.length) return

    const moveX = dx / state.scale
    const moveY = dy / state.scale
    let minDx = -Infinity
    let maxDx = Infinity
    let minDy = -Infinity
    let maxDy = Infinity

    for (const dragState of states) {
      minDx = Math.max(minDx, -dragState.x)
      maxDx = Math.min(maxDx, BOARD_W - dragState.widget.w - dragState.x)
      minDy = Math.max(minDy, -dragState.y)
      maxDy = Math.min(maxDy, BOARD_H - dragState.widget.h - dragState.y)
    }

    const clampedDx = clamp(moveX, minDx, maxDx)
    const clampedDy = clamp(moveY, minDy, maxDy)
    for (const dragState of states) {
      dragState.x += clampedDx
      dragState.y += clampedDy
      scheduleDragApply(dragState)
    }
    setSnapGuides(null, null)
    clearDragAssist()
    return
  }

  state.x += dx / state.scale
  state.y += dy / state.scale

  const smartSnap = state.isText ? false : shouldUseSmartSnap(event)
  const nextX = snapOriginToTargets(state.x, w.w, state.xTargets, smartSnap, state.y + w.h / 2)
  const nextY = snapOriginToTargets(state.y, w.h, state.yTargets, smartSnap, state.x + w.w / 2)
  state.x = nextX.value
  state.y = nextY.value

  const clamped = clampWidgetPosition(state.x, state.y, w)
  state.x = clamped.x
  state.y = clamped.y

  setSnapGuides(null, null)
  if (smartSnap) {
    updateDragAssist(state, nextX, nextY)
  } else {
    clearDragAssist()
  }

  scheduleDragApply(state)
}

function flushQueuedDragPointerMove() {
  if (dragPointerMoveRaf != null) {
    cancelAnimationFrame(dragPointerMoveRaf)
    dragPointerMoveRaf = null
  }
  const queued = queuedDragPointerEvent
  queuedDragPointerEvent = null
  if (queued) applyGlobalPointerMove(queued)
}

function onGlobalPointerMove(event: PointerEvent) {
  if (!activeDragId) return
  queuedDragPointerEvent = event
  if (dragPointerMoveRaf != null) return
  dragPointerMoveRaf = requestAnimationFrame(() => {
    dragPointerMoveRaf = null
    const queued = queuedDragPointerEvent
    queuedDragPointerEvent = null
    if (!queued) return
    applyGlobalPointerMove(queued)
  })
}

function finishDrag(id: string) {
  const state = dragStates.get(id)
  const w = state?.widget ?? getWidgetById(id)
  const el = state?.el ?? widgetEls.get(id)
  if (el) el.classList.remove('is-dragging')

  if (!state || !w) {
    clearDragState(id)
    clearDragAssist()
    setCanvasPanEnabled(true)
    activeDragId = null
    return
  }

  const snapped = clampWidgetPosition(
    isTextWidget(w) ? Math.round(state.x) : snap(state.x),
    isTextWidget(w) ? Math.round(state.y) : snap(state.y),
    w,
  )
  w.x = snapped.x
  w.y = snapped.y
  clampWidget(w)
  if (el) applyWidgetDOM(el, w)

  setSnapGuides(null, null)
  clearDragAssist()

  clearDragState(id)
  setCanvasPanEnabled(true)
  scheduleSave()
  activeDragId = null
  activeDragIds = []
  if (dragArmedId.value === id) dragArmedId.value = null
  bumpInteractionTick()
}

function finishDragGroup(ids: string[]) {
  const uniqueIds = Array.from(new Set(ids))
  if (uniqueIds.length <= 1) {
    if (uniqueIds[0]) finishDrag(uniqueIds[0])
    return
  }

  for (const id of uniqueIds) {
    const state = dragStates.get(id)
    const w = state?.widget ?? getWidgetById(id)
    const el = state?.el ?? widgetEls.get(id)
    if (el) el.classList.remove('is-dragging')
    if (!state || !w) {
      clearDragState(id)
      continue
    }
    const snapped = clampWidgetPosition(
      isTextWidget(w) ? Math.round(state.x) : snap(state.x),
      isTextWidget(w) ? Math.round(state.y) : snap(state.y),
      w,
    )
    w.x = snapped.x
    w.y = snapped.y
    clampWidget(w)
    if (el) applyWidgetDOM(el, w)
    clearDragState(id)
  }

  setSnapGuides(null, null)
  clearDragAssist()
  setCanvasPanEnabled(true)
  scheduleSave()
  activeDragId = null
  activeDragIds = []
  if (dragArmedId.value && uniqueIds.includes(dragArmedId.value)) {
    dragArmedId.value = null
  }
  bumpInteractionTick()
}

function onGlobalPointerUp() {
  window.removeEventListener('pointermove', onGlobalPointerMove)
  flushQueuedDragPointerMove()
  const ids = activeDragIds.length ? [...activeDragIds] : activeDragId ? [activeDragId] : []
  if (!ids.length) {
    setSnapGuides(null, null)
    clearDragAssist()
    return
  }
  if (!ids.some((id) => dragStates.has(id))) {
    setSnapGuides(null, null)
    clearDragAssist()
    activeDragId = null
    activeDragIds = []
    setCanvasPanEnabled(true)
    return
  }
  finishDragGroup(ids)
}
/** active/desactive le pan du board */
function setCanvasPanEnabled(enabled: boolean) {
  camera.setPanEnabled(enabled)
}

function syncPanzoomExclude(enabled: boolean) {
  const pz = camera.getPanzoom()
  pz?.setOptions?.({ excludeClass: enabled ? 'panzoom-exclude' : null })
}

function startGroupResize(ids: string[], dir: ResizeDir, event: PointerEvent) {
  const members: GroupResizeMember[] = []
  let minX = Infinity
  let minY = Infinity
  let maxX = -Infinity
  let maxY = -Infinity

  for (const memberId of ids) {
    const widget = getWidgetById(memberId)
    if (!widget) continue
    const minSize = minSizeFor(widget)
    const isText = isTextWidget(widget)
    if (!isText && widget.props?.autoHeight === true) {
      widget.props = { ...(widget.props ?? {}), autoHeight: false }
    }
    const el = widgetEls.get(memberId) ?? null
    const member: GroupResizeMember = {
      id: memberId,
      widget,
      el,
      metricsEl: readResizeMetricsEl(el),
      originX: widget.x,
      originY: widget.y,
      originW: widget.w,
      originH: widget.h,
      originFontSize: isText ? textWidgetResolvedFontSize(widget) : 0,
      minW: minSize.w,
      minH: minSize.h,
      isText,
      x: widget.x,
      y: widget.y,
      w: widget.w,
      h: widget.h,
      previewFontSize: null,
    }
    members.push(member)
    minX = Math.min(minX, member.originX)
    minY = Math.min(minY, member.originY)
    maxX = Math.max(maxX, member.originX + member.originW)
    maxY = Math.max(maxY, member.originY + member.originH)
  }

  if (members.length < 2) return false

  const originW = Math.max(maxX - minX, 1)
  const originH = Math.max(maxY - minY, 1)
  const minScaleX = members.reduce((maxScale, member) => {
    if (member.originW <= 0) return maxScale
    return Math.max(maxScale, member.minW / member.originW)
  }, 0.01)
  const minScaleY = members.reduce((maxScale, member) => {
    if (member.originH <= 0) return maxScale
    return Math.max(maxScale, member.minH / member.originH)
  }, 0.01)

  activeGroupResizeState = {
    dir,
    scale: Math.max(Number(camera.scale.value || 1), 0.01),
    pointerStartX: event.clientX,
    pointerStartY: event.clientY,
    originLeft: minX,
    originTop: minY,
    originRight: maxX,
    originBottom: maxY,
    originW,
    originH,
    originCenterX: minX + originW / 2,
    originCenterY: minY + originH / 2,
    aspect: originW / originH,
    minScaleX,
    minScaleY,
    members,
    raf: null,
  }

  for (const member of members) {
    member.widget.z = ++zTop
    member.el?.classList.add('is-resizing')
    updateResizeMetricsText(member.metricsEl, member.w, member.h)
  }

  return true
}

function applyGroupResizeStep(event: PointerEvent) {
  const state = activeGroupResizeState
  if (!state) return

  const dx = (event.clientX - state.pointerStartX) / state.scale
  const dy = (event.clientY - state.pointerStartY) / state.scale
  const xSign = axisSign(state.dir, 'e', 'w')
  const ySign = axisSign(state.dir, 's', 'n')
  const fromCenter = event.altKey
  const cornerResize = xSign !== 0 && ySign !== 0
  const keepRatio = state.aspect > 0 && (event.shiftKey || cornerResize)

  let scaleX = 1
  let scaleY = 1
  if (cornerResize) {
    const outwardX = xSign * dx
    const outwardY = ySign * dy
    const dominant = Math.abs(outwardX) >= Math.abs(outwardY) ? outwardX : outwardY
    const delta = dominant * (fromCenter ? 2 : 1)
    const minUniformScale = Math.max(state.minScaleX, state.minScaleY, 0.01)
    const rawScale = (state.originW + delta) / Math.max(state.originW, 1)
    let uniformScale = Math.max(rawScale, minUniformScale)

    let maxUniformScale = Infinity
    if (fromCenter) {
      const maxW = Math.max(2, 2 * Math.min(state.originCenterX, BOARD_W - state.originCenterX))
      const maxH = Math.max(2, 2 * Math.min(state.originCenterY, BOARD_H - state.originCenterY))
      maxUniformScale = Math.min(maxW / state.originW, maxH / state.originH)
    } else {
      if (xSign > 0) {
        maxUniformScale = Math.min(maxUniformScale, (BOARD_W - state.originLeft) / state.originW)
      } else if (xSign < 0) {
        maxUniformScale = Math.min(maxUniformScale, state.originRight / state.originW)
      }
      if (ySign > 0) {
        maxUniformScale = Math.min(maxUniformScale, (BOARD_H - state.originTop) / state.originH)
      } else if (ySign < 0) {
        maxUniformScale = Math.min(maxUniformScale, state.originBottom / state.originH)
      }
    }

    if (Number.isFinite(maxUniformScale)) {
      uniformScale = Math.min(uniformScale, Math.max(maxUniformScale, minUniformScale))
    }

    scaleX = uniformScale
    scaleY = uniformScale
  } else {
    if (xSign !== 0) {
      const rawW = state.originW + dx * xSign * (fromCenter ? 2 : 1)
      scaleX = rawW / state.originW
    }
    if (ySign !== 0) {
      const rawH = state.originH + dy * ySign * (fromCenter ? 2 : 1)
      scaleY = rawH / state.originH
    }

    if (keepRatio) {
      if (xSign !== 0 && ySign !== 0) {
        if (Math.abs(scaleX - 1) >= Math.abs(scaleY - 1)) scaleY = scaleX
        else scaleX = scaleY
      } else if (xSign !== 0) {
        scaleY = scaleX
      } else if (ySign !== 0) {
        scaleX = scaleY
      }
    }

    if (xSign !== 0) {
      scaleX = Math.max(scaleX, state.minScaleX)
    } else {
      scaleX = 1
    }
    if (ySign !== 0) {
      scaleY = Math.max(scaleY, state.minScaleY)
    } else {
      scaleY = 1
    }

    if (fromCenter) {
      if (xSign !== 0) {
        const maxW = Math.max(2, 2 * Math.min(state.originCenterX, BOARD_W - state.originCenterX))
        scaleX = Math.min(scaleX, maxW / state.originW)
      }
      if (ySign !== 0) {
        const maxH = Math.max(2, 2 * Math.min(state.originCenterY, BOARD_H - state.originCenterY))
        scaleY = Math.min(scaleY, maxH / state.originH)
      }
    } else {
      if (xSign > 0) {
        scaleX = Math.min(scaleX, Math.max(1, (BOARD_W - state.originLeft) / state.originW))
      } else if (xSign < 0) {
        scaleX = Math.min(scaleX, Math.max(1, state.originRight / state.originW))
      }
      if (ySign > 0) {
        scaleY = Math.min(scaleY, Math.max(1, (BOARD_H - state.originTop) / state.originH))
      } else if (ySign < 0) {
        scaleY = Math.min(scaleY, Math.max(1, state.originBottom / state.originH))
      }
    }
  }

  const nextW = state.originW * scaleX
  const nextH = state.originH * scaleY
  let left = state.originLeft
  let top = state.originTop

  if (xSign !== 0) {
    if (fromCenter) left = state.originCenterX - nextW / 2
    else if (xSign < 0) left = state.originRight - nextW
  }
  if (ySign !== 0) {
    if (fromCenter) top = state.originCenterY - nextH / 2
    else if (ySign < 0) top = state.originBottom - nextH
  }

  left = clamp(left, 0, BOARD_W - nextW)
  top = clamp(top, 0, BOARD_H - nextH)

  let fontScale = 1
  if (cornerResize) {
    fontScale = Math.max(0.2, Math.abs(scaleX))
  } else if (xSign !== 0 && ySign !== 0) {
    fontScale = Math.max(0.2, (Math.abs(scaleX) + Math.abs(scaleY)) / 2)
  } else if (xSign !== 0) {
    fontScale = Math.max(0.2, Math.abs(scaleX))
  } else if (ySign !== 0) {
    fontScale = Math.max(0.2, Math.abs(scaleY))
  }

  for (const member of state.members) {
    const relX = (member.originX - state.originLeft) / state.originW
    const relY = (member.originY - state.originTop) / state.originH
    const relW = member.originW / state.originW
    const relH = member.originH / state.originH

    member.x = left + relX * nextW
    member.y = top + relY * nextH
    member.w = Math.max(member.minW, relW * nextW)
    member.h = Math.max(member.minH, relH * nextH)

    if (member.isText) {
      const bounds = textWidgetFontBounds(member.widget)
      member.previewFontSize = clamp(
        Math.round(member.originFontSize * fontScale),
        bounds.min,
        bounds.max,
      )
    } else {
      member.previewFontSize = null
    }
  }

  setSnapGuides(null, null)
  scheduleGroupResizeApply(state)
}

function finishGroupResize() {
  const state = activeGroupResizeState
  if (!state) return
  const cornerResize =
    (state.dir.includes('e') || state.dir.includes('w')) &&
    (state.dir.includes('n') || state.dir.includes('s'))

  if (state.raf) {
    cancelAnimationFrame(state.raf)
    state.raf = null
  }

  for (const member of state.members) {
    member.el?.classList.remove('is-resizing')
    const fineSnap = true
    const snappedW = clamp(
      fineSnap ? Math.round(member.w) : snap(member.w),
      member.minW,
      BOARD_W,
    )
    const snappedH = clamp(
      fineSnap ? Math.round(member.h) : snap(member.h),
      member.minH,
      BOARD_H,
    )
    member.widget.x = clamp(fineSnap ? Math.round(member.x) : snap(member.x), 0, BOARD_W - snappedW)
    member.widget.y = clamp(fineSnap ? Math.round(member.y) : snap(member.y), 0, BOARD_H - snappedH)
    member.widget.w = snappedW
    member.widget.h = snappedH
    if (member.isText && member.previewFontSize != null) {
      if (Number(member.widget.props?.fontSize) !== member.previewFontSize) {
        member.widget.props = {
          ...(member.widget.props ?? {}),
          fontSize: member.previewFontSize,
        }
      }
    }
    clampWidget(member.widget)
    if (member.el) applyWidgetDOM(member.el, member.widget)
    if (member.isText) {
      const preserveMode: 'resize' | 'scale' = cornerResize ? 'scale' : 'resize'
      fitTextWidgetAfterRender(member.widget, widgetEls.get(member.id) ?? member.el ?? null, {
        preserveWidth: shouldPreserveTextWidth(member.widget, preserveMode),
      })
    }
  }

  activeGroupResizeState = null
  setSnapGuides(null, null)
  clearGlobalResizeCursor()
  setCanvasPanEnabled(true)
  scheduleSave()
  bumpInteractionTick()
}

function startResize(id: string, dir: ResizeDir, event: PointerEvent) {
  if (!editMode.value) return
  if (spacePanActive.value) return
  if (fullscreenActive.value) return
  const w = getWidgetById(id)
  const groupIds = selectedWidgetIds.value.includes(id) ? [...selectedWidgetIds.value] : [id]
  const useGroupResize = groupIds.length > 1
  let el = widgetEls.get(id)
  if (!el) {
    const target = (event.target as HTMLElement | null)?.closest('.widget') as HTMLElement | null
    if (target) {
      widgetEls.set(id, target)
      el = target
    }
  }
  if (!w) return
  if (!useGroupResize && !el) return
  if (!useGroupResize) {
    selectSingleWidget(id)
    setActiveTextWidget(isTextWidget(w) ? id : null)
  } else {
    setActiveTextWidget(null)
  }

  event.preventDefault()
  event.stopPropagation()

  dragArmedId.value = null
  if (useGroupResize) {
    clearResizeState(id)
    activeResizeId = null
    if (!startGroupResize(groupIds, dir, event)) return
    setGlobalResizeCursor(dir)
    setCanvasPanEnabled(false)
    window.addEventListener('pointermove', onResizePointerMove)
    window.addEventListener('pointerup', onResizePointerUp, { once: true })
    window.addEventListener('pointercancel', onResizePointerUp, { once: true })
    return
  }

  w.z = ++zTop
  activeResizeId = id

  const s = Number(camera.scale.value || 1)
  const minSize = minSizeFor(w)
  const maxSize = maxSizeFor(w)
  const isText = isTextWidget(w)
  if (!isText && w.props?.autoHeight === true) {
    w.props = { ...(w.props ?? {}), autoHeight: false }
  }
  resizeStates.set(id, {
    x: w.x,
    y: w.y,
    w: w.w,
    h: w.h,
    dir,
    scale: s > 0 ? s : 1,
    pointerStartX: event.clientX,
    pointerStartY: event.clientY,
    originX: w.x,
    originY: w.y,
    originW: w.w,
    originH: w.h,
    originCenterX: w.x + w.w / 2,
    originCenterY: w.y + w.h / 2,
    aspect: w.w > 0 && w.h > 0 ? w.w / w.h : 1,
    originFontSize: isText ? textWidgetResolvedFontSize(w) : 0,
    raf: null,
    widget: w,
    el,
    metricsEl: readResizeMetricsEl(el),
    xTargets: collectAxisTargets(id, 'x'),
    yTargets: collectAxisTargets(id, 'y'),
    widthTargets: collectSizeTargets(id, 'w'),
    heightTargets: collectSizeTargets(id, 'h'),
    minW: minSize.w,
    minH: minSize.h,
    maxW: maxSize.w,
    maxH: maxSize.h,
    isText,
    previewFontSize: null,
  })

  el.classList.add('is-resizing')
  const resizeState = resizeStates.get(id)
  if (resizeState) {
    updateResizeMetricsText(resizeState.metricsEl, resizeState.w, resizeState.h)
  }
  setGlobalResizeCursor(dir)
  setCanvasPanEnabled(false)

  window.addEventListener('pointermove', onResizePointerMove)
  window.addEventListener('pointerup', onResizePointerUp, { once: true })
  window.addEventListener('pointercancel', onResizePointerUp, { once: true })
}

function startTextScale(id: string, event: PointerEvent) {
  if (!editMode.value) return
  if (spacePanActive.value) return
  if (fullscreenActive.value) return
  const w = getWidgetById(id)
  if (!w || !isTextWidget(w)) return
  selectSingleWidget(id)
  setActiveTextWidget(id)

  const bounds = textWidgetFontBounds(w)
  const baseScale = clamp(
    Number(w.props?.fontSize ?? bounds.fallback) || bounds.fallback,
    bounds.min,
    bounds.max,
  )
  activeTextScaleState = {
    widgetId: id,
    lastX: event.clientX,
    lastY: event.clientY,
    pendingDelta: 0,
    raf: null,
    appliedScale: baseScale,
  }

  event.preventDefault()
  event.stopPropagation()
  dragArmedId.value = null
  w.z = ++zTop
  setCanvasPanEnabled(false)

  window.addEventListener('pointermove', onTextScalePointerMove)
  window.addEventListener('pointerup', onTextScalePointerUp, { once: true })
  window.addEventListener('pointercancel', onTextScalePointerUp, { once: true })
}

function applyResizePointerMove(event: PointerEvent) {
  if (activeGroupResizeState) {
    applyGroupResizeStep(event)
    return
  }
  if (!activeResizeId) return
  const state = resizeStates.get(activeResizeId)
  if (!state) return
  const w = state.widget

  const dx = (event.clientX - state.pointerStartX) / state.scale
  const dy = (event.clientY - state.pointerStartY) / state.scale
  const minW = state.minW
  const minH = state.minH
  const stateMaxW = state.maxW
  const stateMaxH = state.maxH
  const xSign = axisSign(state.dir, 'e', 'w')
  const ySign = axisSign(state.dir, 's', 'n')
  const cornerResize = xSign !== 0 && ySign !== 0
  const fromCenter = event.altKey

  if (state.isText) {
    const smartSnap = shouldUseSmartSnap(event) && !fromCenter
    let guideX: number | null = null
    let guideY: number | null = null
    let nextW = state.originW
    let nextH = state.originH

    if (cornerResize) {
      const outwardX = xSign * dx
      const outwardY = ySign * dy
      const dominant = Math.abs(outwardX) >= Math.abs(outwardY) ? outwardX : outwardY
      const delta = dominant * (fromCenter ? 2 : 1)
      const minUniformScale = Math.max(
        minW / Math.max(state.originW, 1),
        minH / Math.max(state.originH, 1),
        0.01,
      )
      const maxUniformScaleByWidget = Math.min(
        stateMaxW / Math.max(state.originW, 1),
        stateMaxH / Math.max(state.originH, 1),
      )
      const rawUniformScale = (state.originW + delta) / Math.max(state.originW, 1)
      let uniformScale = Math.max(rawUniformScale, minUniformScale)

      let maxUniformScale = Infinity
      if (fromCenter) {
        const maxW = Math.max(2, 2 * Math.min(state.originCenterX, BOARD_W - state.originCenterX))
        const maxH = Math.max(2, 2 * Math.min(state.originCenterY, BOARD_H - state.originCenterY))
        maxUniformScale = Math.min(
          maxW / Math.max(state.originW, 1),
          maxH / Math.max(state.originH, 1),
        )
      } else {
        if (xSign > 0) {
          maxUniformScale = Math.min(
            maxUniformScale,
            (BOARD_W - state.originX) / Math.max(state.originW, 1),
          )
        } else if (xSign < 0) {
          const rightAnchor = state.originX + state.originW
          maxUniformScale = Math.min(maxUniformScale, rightAnchor / Math.max(state.originW, 1))
        }
        if (ySign > 0) {
          maxUniformScale = Math.min(
            maxUniformScale,
            (BOARD_H - state.originY) / Math.max(state.originH, 1),
          )
        } else if (ySign < 0) {
          const bottomAnchor = state.originY + state.originH
          maxUniformScale = Math.min(maxUniformScale, bottomAnchor / Math.max(state.originH, 1))
        }
      }

      if (Number.isFinite(maxUniformScale)) {
        uniformScale = Math.min(uniformScale, Math.max(maxUniformScale, minUniformScale))
      }
      uniformScale = Math.min(uniformScale, Math.max(maxUniformScaleByWidget, minUniformScale))

      nextW = clamp(state.originW * uniformScale, minW, stateMaxW)
      nextH = clamp(state.originH * uniformScale, minH, stateMaxH)
      const bounds = textWidgetFontBounds(w)
      state.previewFontSize = clamp(
        Math.round(state.originFontSize * uniformScale),
        bounds.min,
        bounds.max,
      )
    } else {
      if (xSign !== 0) {
        nextW = state.originW + dx * xSign * (fromCenter ? 2 : 1)
      }
      if (ySign !== 0) {
        nextH = state.originH + dy * ySign * (fromCenter ? 2 : 1)
      }
      nextW = clamp(nextW, minW, stateMaxW)
      nextH = clamp(nextH, minH, stateMaxH)
      state.previewFontSize = null
    }

    let nextX = state.originX
    let nextY = state.originY
    if (fromCenter) {
      nextX = clamp(state.originCenterX - nextW / 2, 0, BOARD_W - nextW)
      nextY = clamp(state.originCenterY - nextH / 2, 0, BOARD_H - nextH)
    } else {
      if (xSign > 0) {
        nextX = clamp(state.originX, 0, BOARD_W - nextW)
      } else if (xSign < 0) {
        const rightAnchor = state.originX + state.originW
        nextX = clamp(rightAnchor - nextW, 0, BOARD_W - nextW)
      } else {
        nextX = clamp(state.originX, 0, BOARD_W - nextW)
      }

      if (ySign > 0) {
        nextY = clamp(state.originY, 0, BOARD_H - nextH)
      } else if (ySign < 0) {
        const bottomAnchor = state.originY + state.originH
        nextY = clamp(bottomAnchor - nextH, 0, BOARD_H - nextH)
      } else {
        nextY = clamp(state.originY, 0, BOARD_H - nextH)
      }
    }

    if (smartSnap && !cornerResize) {
      if (xSign > 0) {
        const left = nextX
        const right = nextX + nextW
        const snappedRight = snapEdgeToTargets(right, state.xTargets, true)
        if (snappedRight.guide !== null && snappedRight.value - left >= minW) {
          nextW = clamp(snappedRight.value - left, minW, stateMaxW)
          guideX = snappedRight.guide
        }
      } else if (xSign < 0) {
        const right = nextX + nextW
        const snappedLeft = snapEdgeToTargets(nextX, state.xTargets, true)
        if (snappedLeft.guide !== null && right - snappedLeft.value >= minW) {
          nextX = clamp(snappedLeft.value, 0, right - minW)
          nextW = clamp(right - nextX, minW, stateMaxW)
          guideX = snappedLeft.guide
        }
      }

      if (ySign > 0) {
        const top = nextY
        const bottom = nextY + nextH
        const snappedBottom = snapEdgeToTargets(bottom, state.yTargets, true)
        if (snappedBottom.guide !== null && snappedBottom.value - top >= minH) {
          nextH = clamp(snappedBottom.value - top, minH, stateMaxH)
          guideY = snappedBottom.guide
        }
      } else if (ySign < 0) {
        const bottom = nextY + nextH
        const snappedTop = snapEdgeToTargets(nextY, state.yTargets, true)
        if (snappedTop.guide !== null && bottom - snappedTop.value >= minH) {
          nextY = clamp(snappedTop.value, 0, bottom - minH)
          nextH = clamp(bottom - nextY, minH, stateMaxH)
          guideY = snappedTop.guide
        }
      }
    }

    nextW = clamp(nextW, minW, stateMaxW)
    nextH = clamp(nextH, minH, stateMaxH)
    nextX = clamp(nextX, 0, BOARD_W - nextW)
    nextY = clamp(nextY, 0, BOARD_H - nextH)

    state.x = nextX
    state.y = nextY
    state.w = nextW
    state.h = nextH
    setSnapGuides(smartSnap ? guideX : null, smartSnap ? guideY : null)
    scheduleResizeApply(state)
    return
  }

  state.previewFontSize = null
  let nextW = state.originW
  let nextH = state.originH

  if (cornerResize) {
    const outwardX = xSign * dx
    const outwardY = ySign * dy
    const dominant = Math.abs(outwardX) >= Math.abs(outwardY) ? outwardX : outwardY
    const delta = dominant * (fromCenter ? 2 : 1)
    const minUniformScale = Math.max(
      minW / Math.max(state.originW, 1),
      minH / Math.max(state.originH, 1),
      0.01,
    )
    const maxUniformScaleByWidget = Math.min(
      stateMaxW / Math.max(state.originW, 1),
      stateMaxH / Math.max(state.originH, 1),
    )
    const rawUniformScale = (state.originW + delta) / Math.max(state.originW, 1)
    let uniformScale = Math.max(rawUniformScale, minUniformScale)

    let maxUniformScale = Infinity
    if (fromCenter) {
      const maxW = Math.max(2, 2 * Math.min(state.originCenterX, BOARD_W - state.originCenterX))
      const maxH = Math.max(2, 2 * Math.min(state.originCenterY, BOARD_H - state.originCenterY))
      maxUniformScale = Math.min(
        maxW / Math.max(state.originW, 1),
        maxH / Math.max(state.originH, 1),
      )
    } else {
      if (xSign > 0) {
        maxUniformScale = Math.min(
          maxUniformScale,
          (BOARD_W - state.originX) / Math.max(state.originW, 1),
        )
      } else if (xSign < 0) {
        const rightAnchor = state.originX + state.originW
        maxUniformScale = Math.min(maxUniformScale, rightAnchor / Math.max(state.originW, 1))
      }
      if (ySign > 0) {
        maxUniformScale = Math.min(
          maxUniformScale,
          (BOARD_H - state.originY) / Math.max(state.originH, 1),
        )
      } else if (ySign < 0) {
        const bottomAnchor = state.originY + state.originH
        maxUniformScale = Math.min(maxUniformScale, bottomAnchor / Math.max(state.originH, 1))
      }
    }

    if (Number.isFinite(maxUniformScale)) {
      uniformScale = Math.min(uniformScale, Math.max(maxUniformScale, minUniformScale))
    }
    uniformScale = Math.min(uniformScale, Math.max(maxUniformScaleByWidget, minUniformScale))

    nextW = clamp(state.originW * uniformScale, minW, stateMaxW)
    nextH = clamp(state.originH * uniformScale, minH, stateMaxH)
  } else {
    if (xSign !== 0) {
      nextW = state.originW + dx * xSign * (fromCenter ? 2 : 1)
    }
    if (ySign !== 0) {
      nextH = state.originH + dy * ySign * (fromCenter ? 2 : 1)
    }

    nextW = clamp(nextW, minW, stateMaxW)
    nextH = clamp(nextH, minH, stateMaxH)
  }

  let nextX = state.originX
  let nextY = state.originY
  const smartSnap = shouldUseSmartSnap(event) && !fromCenter
  let guideX: number | null = null
  let guideY: number | null = null

  if (fromCenter) {
    nextX = clamp(state.originCenterX - nextW / 2, 0, BOARD_W - nextW)
    nextY = clamp(state.originCenterY - nextH / 2, 0, BOARD_H - nextH)
  } else {
    if (xSign > 0) {
      nextX = clamp(state.originX, 0, BOARD_W - nextW)
    } else if (xSign < 0) {
      const rightAnchor = state.originX + state.originW
      nextX = clamp(rightAnchor - nextW, 0, BOARD_W - nextW)
    } else {
      nextX = clamp(state.originCenterX - nextW / 2, 0, BOARD_W - nextW)
    }

    if (ySign > 0) {
      nextY = clamp(state.originY, 0, BOARD_H - nextH)
    } else if (ySign < 0) {
      const bottomAnchor = state.originY + state.originH
      nextY = clamp(bottomAnchor - nextH, 0, BOARD_H - nextH)
    } else {
      nextY = clamp(state.originY, 0, BOARD_H - nextH)
    }
  }

  if (smartSnap) {
    if (xSign > 0) {
      const left = nextX
      const right = nextX + nextW
      const snappedRight = snapEdgeToTargets(right, state.xTargets, true)
      if (snappedRight.guide !== null && snappedRight.value - left >= minW) {
        nextW = clamp(snappedRight.value - left, minW, stateMaxW)
        guideX = snappedRight.guide
      }
    } else if (xSign < 0) {
      const right = nextX + nextW
      const snappedLeft = snapEdgeToTargets(nextX, state.xTargets, true)
      if (snappedLeft.guide !== null && right - snappedLeft.value >= minW) {
        nextX = clamp(snappedLeft.value, 0, right - minW)
        nextW = clamp(right - nextX, minW, stateMaxW)
        guideX = snappedLeft.guide
      }
    }

    if (ySign > 0) {
      const top = nextY
      const bottom = nextY + nextH
      const snappedBottom = snapEdgeToTargets(bottom, state.yTargets, true)
      if (snappedBottom.guide !== null && snappedBottom.value - top >= minH) {
        nextH = clamp(snappedBottom.value - top, minH, stateMaxH)
        guideY = snappedBottom.guide
      }
    } else if (ySign < 0) {
      const bottom = nextY + nextH
      const snappedTop = snapEdgeToTargets(nextY, state.yTargets, true)
      if (snappedTop.guide !== null && bottom - snappedTop.value >= minH) {
        nextY = clamp(snappedTop.value, 0, bottom - minH)
        nextH = clamp(bottom - nextY, minH, stateMaxH)
        guideY = snappedTop.guide
      }
    }

    if (!cornerResize) {
      if (xSign !== 0) {
        const rightAnchor = nextX + nextW
        const sizeSnapX = snapSizeToTargets(nextW, state.widthTargets, true)
        if (sizeSnapX.snapped) {
          nextW = clamp(sizeSnapX.value, minW, stateMaxW)
          if (xSign < 0) {
            nextX = clamp(rightAnchor - nextW, 0, BOARD_W - nextW)
          }
        }
      }
      if (ySign !== 0) {
        const bottomAnchor = nextY + nextH
        const sizeSnapY = snapSizeToTargets(nextH, state.heightTargets, true)
        if (sizeSnapY.snapped) {
          nextH = clamp(sizeSnapY.value, minH, stateMaxH)
          if (ySign < 0) {
            nextY = clamp(bottomAnchor - nextH, 0, BOARD_H - nextH)
          }
        }
      }
    }

    nextW = clamp(nextW, minW, stateMaxW)
    nextH = clamp(nextH, minH, stateMaxH)
    nextX = clamp(nextX, 0, BOARD_W - nextW)
    nextY = clamp(nextY, 0, BOARD_H - nextH)
  }

  state.x = nextX
  state.y = nextY
  state.w = nextW
  state.h = nextH

  setSnapGuides(smartSnap ? guideX : null, smartSnap ? guideY : null)
  scheduleResizeApply(state)
}

function flushQueuedResizePointerMove() {
  if (resizePointerMoveRaf != null) {
    cancelAnimationFrame(resizePointerMoveRaf)
    resizePointerMoveRaf = null
  }
  const queued = queuedResizePointerEvent
  queuedResizePointerEvent = null
  if (queued) applyResizePointerMove(queued)
}

function onResizePointerMove(event: PointerEvent) {
  if (!activeResizeId && !activeGroupResizeState) return
  queuedResizePointerEvent = event
  if (resizePointerMoveRaf != null) return
  resizePointerMoveRaf = requestAnimationFrame(() => {
    resizePointerMoveRaf = null
    const queued = queuedResizePointerEvent
    queuedResizePointerEvent = null
    if (!queued) return
    applyResizePointerMove(queued)
  })
}

function finishResize(id: string) {
  const state = resizeStates.get(id)
  const w = state?.widget ?? getWidgetById(id)
  const el = state?.el ?? widgetEls.get(id)
  if (el) el.classList.remove('is-resizing')

  if (!state || !w) {
    clearResizeState(id)
    clearGlobalResizeCursor()
    setCanvasPanEnabled(true)
    activeResizeId = null
    return
  }

  const cornerResize =
    (state.dir.includes('e') || state.dir.includes('w')) &&
    (state.dir.includes('n') || state.dir.includes('s'))
  const fineSnap = true
  const snappedW = clamp(fineSnap ? Math.round(state.w) : snap(state.w), state.minW, state.maxW)
  const snappedH = clamp(fineSnap ? Math.round(state.h) : snap(state.h), state.minH, state.maxH)
  const snappedX = clamp(fineSnap ? Math.round(state.x) : snap(state.x), 0, BOARD_W - snappedW)
  const snappedY = clamp(fineSnap ? Math.round(state.y) : snap(state.y), 0, BOARD_H - snappedH)

  w.w = snappedW
  w.h = snappedH
  w.x = snappedX
  w.y = snappedY
  clampWidget(w)

  setSnapGuides(null, null)

  clearResizeState(id)
  clearGlobalResizeCursor()
  setCanvasPanEnabled(true)
  activeResizeId = null

  if (state.isText) {
    if (state.previewFontSize != null && Number(w.props?.fontSize) !== state.previewFontSize) {
      w.props = {
        ...(w.props ?? {}),
        fontSize: state.previewFontSize,
      }
    }
    const preserveMode: 'resize' | 'scale' = cornerResize ? 'scale' : 'resize'
    fitTextWidgetAfterRender(w, widgetEls.get(id) ?? el ?? null, {
      preserveWidth: shouldPreserveTextWidth(w, preserveMode),
    })
    nextTick(() => {
      const liveEl = widgetEls.get(id) ?? el ?? null
      if (liveEl) applyWidgetDOM(liveEl, w)
      scheduleSave()
    })
    return
  }

  if (el) applyWidgetDOM(el, w)
  scheduleSave()
}

function applyTextScaleStep(state: TextScaleState) {
  if (!Number.isFinite(state.pendingDelta) || Math.abs(state.pendingDelta) < 0.001) return
  const w = getWidgetById(state.widgetId)
  if (!w || !isTextWidget(w)) {
    state.pendingDelta = 0
    return
  }

  const bounds = textWidgetFontBounds(w)
  const nextScale = clamp(state.appliedScale + state.pendingDelta, bounds.min, bounds.max)
  state.pendingDelta = 0
  state.appliedScale = nextScale

  const nextFontSize = Math.round(nextScale)
  if (Number(w.props?.fontSize) !== nextFontSize) {
    w.props = {
      ...(w.props ?? {}),
      fontSize: nextFontSize,
    }
  }

  const minSize = minSizeFor(w)
  const preserveWidthOnScale = shouldPreserveTextWidth(w, 'scale')
  const measured = estimateTextWidgetSize(w, {
    targetWidth: w.w,
    targetHeight: w.h,
    lockWidth: preserveWidthOnScale,
  })
  const nextWidth = preserveWidthOnScale
    ? clamp(w.w, minSize.w, BOARD_W)
    : clamp(measured.w, minSize.w, BOARD_W)
  const nextHeight = clamp(measured.h, minSize.h, BOARD_H)
  if (w.w !== nextWidth) {
    w.w = nextWidth
  }
  if (w.h !== nextHeight) {
    w.h = nextHeight
  }
}

function scheduleTextScaleApply(state: TextScaleState) {
  if (state.raf) return
  state.raf = requestAnimationFrame(() => {
    state.raf = null
    applyTextScaleStep(state)
    if (Math.abs(state.pendingDelta) >= 0.001) {
      scheduleTextScaleApply(state)
    }
  })
}

function onTextScalePointerMove(event: PointerEvent) {
  const state = activeTextScaleState
  if (!state) return

  const dx = event.clientX - state.lastX
  const dy = event.clientY - state.lastY
  state.lastX = event.clientX
  state.lastY = event.clientY
  state.pendingDelta += Math.max(dx, dy) / 2.2
  scheduleTextScaleApply(state)
}

function finishTextScale() {
  const state = activeTextScaleState
  if (state?.raf) {
    cancelAnimationFrame(state.raf)
    state.raf = null
  }
  if (state) {
    applyTextScaleStep(state)
  }
  clearTextScaleState()
  setSnapGuides(null, null)
  setCanvasPanEnabled(true)
  if (!state) return

  const w = getWidgetById(state.widgetId)
  if (!w || !isTextWidget(w)) return

  fitTextWidgetAfterRender(w, widgetEls.get(w.id) ?? null, {
    preserveWidth: shouldPreserveTextWidth(w, 'scale'),
  })
  scheduleSave()
}

function onTextScalePointerUp() {
  window.removeEventListener('pointermove', onTextScalePointerMove)
  finishTextScale()
}

function onResizePointerUp() {
  window.removeEventListener('pointermove', onResizePointerMove)
  flushQueuedResizePointerMove()
  if (activeGroupResizeState) {
    finishGroupResize()
    return
  }
  if (!activeResizeId) return
  if (!resizeStates.has(activeResizeId)) {
    setSnapGuides(null, null)
    clearGlobalResizeCursor()
    activeResizeId = null
    setCanvasPanEnabled(true)
    return
  }
  finishResize(activeResizeId)
}

function detachAllInteract() {
  window.removeEventListener('pointermove', onGlobalPointerMove)
  window.removeEventListener('pointermove', onResizePointerMove)
  window.removeEventListener('pointermove', onTextScalePointerMove)
  window.removeEventListener('pointermove', onMarqueePointerMove)
  window.removeEventListener('pointerup', onMarqueePointerUp)
  window.removeEventListener('pointercancel', onMarqueePointerUp)
  if (dragPointerMoveRaf != null) {
    cancelAnimationFrame(dragPointerMoveRaf)
    dragPointerMoveRaf = null
  }
  queuedDragPointerEvent = null
  if (resizePointerMoveRaf != null) {
    cancelAnimationFrame(resizePointerMoveRaf)
    resizePointerMoveRaf = null
  }
  queuedResizePointerEvent = null
  widgetEls.forEach((el) => {
    if (!el || !el.classList) return
    el.classList.remove('is-dragging')
    el.classList.remove('is-resizing')
    const id = String(el.dataset.id ?? '')
    if (id) {
      const widget = getWidgetById(id)
      if (widget) applyWidgetDOM(el, widget)
    }
  })
  dragStates.clear()
  activeDragId = null
  activeDragIds = []
  resizeStates.clear()
  activeResizeId = null
  clearGroupResizeState()
  clearTextScaleState()
  marqueeSelection.value = null
  activeTextWidgetId.value = null
  clearSelection()
  setSnapGuides(null, null)
  clearDragAssist()
  clearGlobalResizeCursor()
  setCanvasPanEnabled(true)
}

watch(
  editMode,
  async (enabled) => {
    if (!enabled) setSpacePanState(false)
    syncPanzoomExclude(shouldUsePanzoomExclude())
    if (!enabled) {
      dragArmedId.value = null
      activeTextWidgetId.value = null
      detachAllInteract()
      return
    }
  },
  { immediate: true },
)

watch(scale, () => {
  scheduleVisibleRectUpdate()
})

watch(
  () => widgets.value.length,
  () => {
    scheduleVisibleRectUpdate()
  },
)

watch(
  [paletteOpen, settingsOpen, profileEditorOpen, shortcutHelpOpen],
  ([palette, settings, profileEditor, shortcutOpen]) => {
    if (palette || settings || profileEditor || shortcutOpen) {
      setSpacePanState(false)
    }
  },
)

watch(
  userId,
  async () => {
    const expectedUserId = String(userId.value)
    clearPendingSaves()
    loadEditMode()
    detachAllInteract()
    loadLayoutForUser()
    applyStoredRangeForActiveProfile()
    await nextTick()
    if (String(userId.value) !== expectedUserId) return
    widgets.value.forEach((w) => clampWidget(w))
    centerView()
    if (expectedUserId !== 'guest') {
      await loadLayoutFromServer(expectedUserId)
    }
    if (String(userId.value) !== expectedUserId) return
    await loadDateBounds()
    if (String(userId.value) !== expectedUserId) return
    if (widgets.value.some((w) => widgetNeedsCategoryFilter(w))) {
      void loadCategories(from.value, to.value)
    }
    if (String(userId.value) !== expectedUserId) return
    applyStoredRangeForActiveProfile()
    normalizeVisibleTextWidgets(true)
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
    const cleaned = raw.map((v) => String(v ?? '').trim()).filter((v) => v.length > 0)
    const unique = Array.from(new Map(cleaned.map((v) => [v.toLowerCase(), v])).values())
    if (unique.length) {
      settingsCategories.value = unique
      categoriesCache.set(key, unique)
      return
    }
    const fallback = await StatsServices.categories()
    const fallbackRaw = Array.isArray(fallback?.data) ? fallback.data : []
    const fallbackClean = fallbackRaw.map((v) => String(v ?? '').trim()).filter((v) => v.length > 0)
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
  const idx = widgets.value.findIndex((w) => w.id === id)
  if (idx < 0) return
  clearDragState(id)
  clearResizeState(id)
  if (activeDragId === id) activeDragId = null
  if (activeResizeId === id) activeResizeId = null
  if (activeTextScaleState?.widgetId === id) {
    clearTextScaleState()
    setCanvasPanEnabled(true)
  }
  if (dragArmedId.value === id) dragArmedId.value = null
  if (activeTextWidgetId.value === id) activeTextWidgetId.value = null
  if (selectedWidgetIds.value.length) {
    selectedWidgetIds.value = selectedWidgetIds.value.filter((item) => item !== id)
  }
  widgets.value.splice(idx, 1)
  clearWidgetStyleCacheFor(id)
  setSnapGuides(null, null)
  scheduleSave()
}

function removeSelectedWidgets() {
  if (!editMode.value) return
  if (!selectedWidgetIds.value.length) return
  const ids = new Set(selectedWidgetIds.value)
  const beforeLen = widgets.value.length

  for (const id of ids) {
    clearDragState(id)
    clearResizeState(id)
    clearWidgetStyleCacheFor(id)
  }

  activeDragIds = activeDragIds.filter((id) => !ids.has(id))
  if (activeDragId && ids.has(activeDragId)) activeDragId = null
  if (activeResizeId && ids.has(activeResizeId)) {
    activeResizeId = null
    clearGlobalResizeCursor()
    setCanvasPanEnabled(true)
  }
  if (dragArmedId.value && ids.has(dragArmedId.value)) dragArmedId.value = null
  if (activeTextWidgetId.value && ids.has(activeTextWidgetId.value)) activeTextWidgetId.value = null
  if (activeTextScaleState?.widgetId && ids.has(activeTextScaleState.widgetId)) {
    clearTextScaleState()
    setCanvasPanEnabled(true)
  }
  if (activeGroupResizeState?.members.some((member) => ids.has(member.id))) {
    clearGroupResizeState()
    clearGlobalResizeCursor()
    setCanvasPanEnabled(true)
  }

  widgets.value = widgets.value.filter((widget) => !ids.has(widget.id))
  if (widgets.value.length === beforeLen) return

  setSnapGuides(null, null)
  scheduleSave()
  clearSelection()
}

function onWidgetRemove(id: string) {
  if (selectedWidgetIds.value.length > 1 && selectedWidgetIds.value.includes(id)) {
    removeSelectedWidgets()
    return
  }
  removeWidget(id)
}

function createWidgetId(type: string) {
  const uid =
    globalThis.crypto?.randomUUID?.() ?? `${Date.now()}_${Math.random().toString(16).slice(2)}`
  return `${type}_${uid}`
}

function duplicateWidget(id: string) {
  if (!editMode.value) return
  const source = getWidgetById(id)
  if (!source) return

  const offset = GRID * 6
  const duplicate: Widget = {
    ...source,
    id: createWidgetId(source.type),
    x: source.x + offset,
    y: source.y + offset,
    props: cloneWidgetProps(source.props ?? {}),
    z: ++zTop,
  }

  const placed = placeWidget(
    duplicate,
    duplicate.x + duplicate.w / 2,
    duplicate.y + duplicate.h / 2,
  )
  duplicate.x = placed.x
  duplicate.y = placed.y
  clampWidget(duplicate)

  widgets.value.push(duplicate)
  setSelectedWidgets([duplicate.id])
  dragArmedId.value = duplicate.id
  setActiveTextWidget(isTextWidget(duplicate) ? duplicate.id : null)
  if (isTextWidget(duplicate)) {
    fitTextWidgetAfterRender(duplicate, widgetEls.get(duplicate.id) ?? null, {
      preserveWidth: shouldPreserveTextWidth(duplicate, 'duplicate'),
    })
  }
  scheduleSave()
}

function resetLayout() {
  if (!editMode.value) return

  // on ferme les modales si besoin
  paletteOpen.value = false
  closeSettings()

  // on debranche tout et on vide
  detachAllInteract()
  widgets.value = []
  widgetStyleCache.clear()

  nextTick(() => {
    // recentre la camera au milieu de la board (vu que plus de widgets)
    centerView()
    // save immediat (pour etre sur que le layout vide est persiste)
    scheduleVisibleRectUpdate()
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

  if (isTextWidget(w)) {
    const textDefaultColor = themeMode.value === 'light' ? '#000000' : '#f8fafc'
    w.props = { ...(w.props ?? {}), color: textDefaultColor }
    fitWidgetToContent(w)
  }

  widgets.value.push(w)
  setSelectedWidgets([w.id])
  scheduleVisibleRectUpdate()

  if (isTextWidget(w)) {
    setActiveTextWidget(w.id)
    fitTextWidgetAfterRender(w, widgetEls.get(w.id) ?? null, {
      preserveWidth: shouldPreserveTextWidth(w, 'content'),
    })
  }
  scheduleSave()
}

function onWidgetFullscreenChange(active: boolean) {
  fullscreenActive.value = active
  if (active) {
    document.body.classList.add('widget-fullscreen-open')
  } else {
    document.body.classList.remove('widget-fullscreen-open')
  }
}

watch(fullscreenActive, (active) => {
  if (active) setSpacePanState(false)
})

function autoResize(id: string, height: number) {
  const w = getWidgetById(id)
  if (!w) return
  if (!Number.isFinite(height)) return
  const isSingleResizeActive = activeResizeId === id && resizeStates.has(id)
  const isGroupResizeActive = Boolean(
    activeGroupResizeState?.members.some((member) => member.id === id),
  )
  const isTextScaleActive = activeTextScaleState?.widgetId === id
  if (isSingleResizeActive || isGroupResizeActive || isTextScaleActive) return
  const minSize = minSizeFor(w)
  const nextH = clamp(height, minSize.h, BOARD_H)
  if (Math.abs(nextH - w.h) < 2) return
  w.h = nextH
  clampWidget(w)
  scheduleSave()
}

function onSelectionKeyDown(event: KeyboardEvent) {
  if (!editMode.value) return
  const target = event.target as HTMLElement | null
  if (target?.closest('input, textarea, select, [contenteditable="true"]')) return
  if (event.key === 'Delete' || event.key === 'Backspace') {
    if (!selectedWidgetIds.value.length) return
    event.preventDefault()
    event.stopPropagation()
    removeSelectedWidgets()
    return
  }
  if (event.key !== 'Enter' && event.key !== 'F2') return
  if (selectedWidgetIds.value.length !== 1) return
  const selectedId = selectedWidgetIds.value[0]
  const selectedWidget = getWidgetById(selectedId)
  if (!selectedWidget) return
  event.preventDefault()
  event.stopPropagation()
  if (isTextWidget(selectedWidget)) {
    setActiveTextWidget(selectedId)
    return
  }
  openSettings(selectedWidget)
}

/* ===== Lifecycle ===== */
onMounted(async () => {
  // init camera + centre quand la vue est prete
  camera.init(() => {
    centerView()
    syncPanzoomExclude(shouldUsePanzoomExclude())
    if (window.innerWidth < COMPACT_BREAKPOINT) zoomToFitContent()
    scheduleVisibleRectUpdate()
  })

  window.addEventListener('keydown', onCanvasKeyDown, { capture: true })
  window.addEventListener('keydown', onSelectionKeyDown, { capture: true })
  window.addEventListener('keyup', onCanvasKeyUp, { capture: true })
  window.addEventListener('blur', onWindowBlur)
  document.addEventListener('visibilitychange', onVisibilityChange)

  await nextTick()
  const board = boardEl.value
  if (board) {
    const onPanzoomChange = () => {
      markCameraInteracting()
      scheduleVisibleRectUpdate()
    }
    board.addEventListener('panzoomchange', onPanzoomChange as EventListener)
    onBeforeUnmount(() =>
      board.removeEventListener('panzoomchange', onPanzoomChange as EventListener),
    )
  }
  scheduleVisibleRectUpdate()

  widgets.value.forEach((w) => clampWidget(w))
  if (editMode.value) disarmWidget()

  const expectedUserId = String(userId.value)
  if (expectedUserId !== 'guest') {
    await loadLayoutFromServer(expectedUserId)
  }
  if (String(userId.value) !== expectedUserId) return
  await loadDateBounds()
  if (String(userId.value) !== expectedUserId) return
  if (widgets.value.some((w) => widgetNeedsCategoryFilter(w))) {
    void loadCategories(from.value, to.value)
  }
  if (String(userId.value) !== expectedUserId) return
  applyStoredRangeForActiveProfile()
  normalizeVisibleTextWidgets(true)

  const resizeHandler = () => {
    const compact = window.innerWidth < COMPACT_BREAKPOINT
    const wasCompact = isCompact.value
    if (compact !== isCompact.value) {
      isCompact.value = compact
      datePanelOpen.value = !compact
      profileSwitcherExpanded.value = !compact
    }
    if (!wasCompact && compact) zoomToFitContent()
    scheduleVisibleRectUpdate()
  }
  resizeHandler()
  window.addEventListener('resize', resizeHandler, { passive: true })
  onBeforeUnmount(() => window.removeEventListener('resize', resizeHandler))
})

onBeforeUnmount(() => {
  setSpacePanState(false)
  clearVisibleRectRaf()
  clearCameraInteractionTimer()
  window.removeEventListener('keydown', onCanvasKeyDown, true)
  window.removeEventListener('keydown', onSelectionKeyDown, true)
  window.removeEventListener('keyup', onCanvasKeyUp, true)
  window.removeEventListener('blur', onWindowBlur)
  document.removeEventListener('visibilitychange', onVisibilityChange)
  clearPendingSaves()
  if (toastTimer) {
    window.clearTimeout(toastTimer)
    toastTimer = null
  }
  detachAllInteract()
  clearGlobalResizeCursor()
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
  if (isCompact.value) profileSwitcherExpanded.value = false
  nextTick(() => {
    normalizeVisibleTextWidgets(true)
    widgets.value.forEach((w) => clampWidget(w))
    centerView()
    scheduleVisibleRectUpdate()
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

<style scoped src="./StatsCanvas.css"></style>
