<template>
  <div
    class="app-layout-root flex flex-col font-poppins"
    :class="[
      isLightAppShell ? 'layout-home-bg layout-document-flow' : 'bg-slate-950',
      isGestionRoute ? 'layout-gestion-bg' : '',
      isLightAppShell ? 'text-slate-900' : isStatsLight ? 'text-black' : 'text-slate-100',
    ]"
    :style="layoutVars"
  >
    <!-- Header -->
    <template v-if="showAppHeader">
      <!-- Header for stats: simple bar -->
      <header
        class="layout-app-header fixed left-0 right-0 z-50 pointer-events-none transition-all duration-200"
        :class="[
          (route.path === '/' || route.path === '/gestion') && homeHeaderHidden && !mobileMenuOpen && !menuOpen
            ? 'layout-app-header--hidden'
            : '',
          isStats ? 'top-2' : navBubble || mobileMenuOpen ? 'top-0 is-stuck' : 'top-4',
          isLightAppShell || isStatsLight ? 'is-light' : 'is-dark',
        ]"
      >
        <div
          class="layout-shell-row layout-shell-row--header flex items-center justify-between pointer-events-none"
          :class="isStats ? 'h-11' : 'h-14'"
        >
          <!-- Left spacer / burger -->
          <div class="flex items-center pointer-events-auto">
            <button
              v-if="showHeaderNav"
              type="button"
              class="md:hidden p-2 rounded-xl transition"
              :class="
                isLightAppShell
                  ? 'text-gray-600 hover:text-black hover:bg-black/5'
                  : 'text-gray-300 hover:text-white hover:bg-white/5'
              "
              @click.stop="toggleMobileMenu"
              aria-label="Ouvrir le menu"
              :aria-expanded="mobileMenuOpen"
            >
              <svg class="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path
                  v-if="!mobileMenuOpen"
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  stroke-width="2"
                  d="M4 6h16M4 12h16M4 18h16"
                />
                <path
                  v-else
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  stroke-width="2"
                  d="M6 18L18 6M6 6l12 12"
                />
              </svg>
            </button>
            <span v-else class="h-9 w-9" aria-hidden="true"></span>
          </div>

          <!-- Center nav -->
          <nav
            v-if="showHeaderNav"
            class="hidden md:flex items-center justify-center pointer-events-auto mx-auto"
            :class="
              navBubble || isStatsLight || isLightAppShell
                ? isStatsLight || isLightAppShell
                  ? 'gap-3 px-4 py-2 rounded-full bg-white/95 border border-slate-200/90 shadow-[0_8px_20px_rgba(15,23,42,0.12)]'
                  : 'gap-3 px-4 py-2 rounded-full bg-slate-900/92 border border-white/10 shadow-[0_10px_24px_rgba(0,0,0,0.3)]'
                : `gap-8 px-8 py-3 rounded-full bg-transparent ${isLightAppShell ? 'text-slate-900' : 'text-white'}`
            "
          >
            <RouterLink to="/" :class="compactLink('/')">
              <Home class="h-4 w-4" aria-hidden="true" />
              <span>Accueil</span>
            </RouterLink>
            <RouterLink to="/stats" :class="compactLink('/stats')">
              <BarChart3 class="h-4 w-4" aria-hidden="true" />
              <span>Stats</span>
            </RouterLink>
            <RouterLink to="/gestion" :class="compactLink('/gestion')">
              <Boxes class="h-4 w-4" aria-hidden="true" />
              <span>Gestion</span>
            </RouterLink>
          </nav>

          <!-- Right user menu -->
          <div v-if="showHeaderUserMenu" class="flex items-center justify-end pointer-events-auto">
            <div class="relative" @click.stop>
              <button
                type="button"
                @click.stop="toggleUserMenu"
                class="h-9 w-9 rounded-full flex items-center justify-center border transition focus:outline-none"
                :class="
                  isStatsLight || isLightAppShell
                    ? 'bg-white/85 border-slate-300 hover:border-emerald-500/45'
                    : 'bg-slate-900/70 border-white/10 hover:border-emerald-300/50'
                "
                aria-label="Menu utilisateur"
                :aria-expanded="menuOpen"
              >
                <span
                  class="text-sm font-semibold"
                  :class="isStatsLight || isLightAppShell ? 'text-slate-900' : 'text-white'"
                  >{{ initials }}</span
                >
              </button>

              <div
                v-if="menuOpen"
                class="absolute right-0 mt-2 w-64 rounded-lg shadow-lg border z-50 bg-slate-900 text-slate-100 border-slate-700"
              >
                <div class="px-4 py-3 border-b border-slate-700">
                  <p class="text-sm text-slate-300">
                    {{ currentUser ? 'Connecte' : 'Non Connecte' }}
                  </p>
                  <p v-if="currentUser" class="text-sm font-medium truncate text-slate-100">
                    Bienvenue {{ currentUser.firstName }}
                  </p>
                </div>

                <div class="py-2 layout-menu-actions is-dark">
                  <button v-if="!currentUser" type="button" @click="goToAuth('login')">
                    Se connecter
                  </button>

                  <button v-if="!currentUser" type="button" @click="goToAuth('signup')">
                    Creer un compte
                  </button>

                  <button v-if="currentUser" type="button" @click="goToAccount">
                    Gerer mon compte
                  </button>

                  <button
                    v-if="currentUser"
                    type="button"
                    @click="logout"
                    class="text-red-600 [&:hover]:bg-red-50 [&:active]:bg-red-100"
                  >
                    Se deconnecter
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Mobile menu -->
        <div
          v-if="showHeaderNav && mobileMenuOpen"
          class="md:hidden layout-shell-row layout-shell-row--header pb-3 pointer-events-auto"
          @click.stop
        >
          <div
            class="layout-mobile-menu-panel mt-2 rounded-2xl border p-2 shadow-lg"
            :class="
              isLightAppShell || isStatsLight
                ? 'bg-white/92 border-slate-200/90 shadow-slate-950/10'
                : 'bg-gray-900/70 border-white/10 shadow-slate-950/30'
            "
          >
            <RouterLink
              to="/"
              class="block px-3 py-2 rounded-md text-base font-medium"
              :class="isActiveMobile('/')"
              @click="closeMenus"
            >
              Accueil
            </RouterLink>

            <RouterLink
              to="/stats"
              class="block px-3 py-2 rounded-md text-base font-medium"
              :class="isActiveMobile('/stats')"
              @click="closeMenus"
            >
              Stats
            </RouterLink>

            <RouterLink
              to="/gestion"
              class="block px-3 py-2 rounded-md text-base font-medium"
              :class="isActiveMobile('/gestion')"
              @click="closeMenus"
            >
              Gestion
            </RouterLink>
          </div>
        </div>
      </header>
    </template>

    <!-- Body -->
    <main
      class="layout-main flex-1 min-h-0"
      :class="isLightAppShell ? 'layout-main--document-flow' : 'bg-slate-950/30'"
    >
      <div
        v-if="route.meta.fullBleed"
        class="layout-fullbleed"
        :class="[
          route.meta.allowScroll ? 'overflow-auto' : 'overflow-hidden',
          fullBleedHeaderOffsetClass,
        ]"
      >
        <slot />
      </div>

      <div
        v-else
        ref="pageScroll"
        class="layout-scroll h-full overflow-auto"
        :class="{
          'layout-scroll--document-flow': isLightAppShell,
          'layout-scroll--hidden-scrollbar': isLightAppShell,
        }"
      >
        <div
          class="layout-scroll-inner relative min-h-full"
          :class="{ 'layout-scroll-inner--document-flow': isLightAppShell }"
        >
          <div
            v-if="!isLightAppShell"
            class="pointer-events-none absolute inset-0 -z-10 overflow-hidden"
            aria-hidden="true"
          >
            <div
              class="absolute -top-40 right-[-10%] h-96 w-96 rounded-full blur-3xl bg-emerald-400/4"
            ></div>
            <div
              class="absolute bottom-[-30%] left-[-10%] h-[30rem] w-[30rem] rounded-full blur-3xl bg-amber-400/4"
            ></div>
            <div
              class="absolute inset-0 bg-[radial-gradient(circle_at_top,_rgba(15,23,42,0.25),_transparent_60%)]"
            ></div>
          </div>
          <div
            class="layout-shell-row layout-page-content min-h-full"
            :class="{ 'layout-page-content--document-flow': isLightAppShell }"
          >
            <slot />
          </div>
        </div>
      </div>
    </main>

    <!-- Footer -->
    <footer
      v-if="!route.meta.fullBleed && !isAuthRoute"
      class="fixed left-0 right-0 bottom-4 flex justify-center transition-opacity duration-500 ease-out pointer-events-none"
      :class="footerVisible ? 'opacity-100' : 'opacity-0'"
    >
      <div
        class="layout-footer-pill pointer-events-auto inline-flex items-center gap-6 px-7 py-3 rounded-full border text-sm"
        :class="
          isStatsLight || isLightAppShell
            ? 'bg-white/95 border-slate-200/90 shadow-[0_8px_20px_rgba(15,23,42,0.12)]'
            : 'bg-slate-900/92 border-white/10 shadow-[0_10px_24px_rgba(0,0,0,0.3)]'
        "
      >
        <span class="font-jetbrains-mono">&copy; {{ new Date().getFullYear() }} - Stash</span>
        <RouterLink to="/a-propos" class="hover:underline">A propos</RouterLink>
        <RouterLink to="/confidentialite" class="hover:underline">Confidentialite</RouterLink>
        <a href="mailto:nathantalvasson@gmail.com" class="hover:underline">contact</a>
      </div>
    </footer>

    <NotificationCenter
      v-if="showNotificationSystem"
      class="notification-center-root"
      :open="notification.centerOpen.value"
      :notifications="notification.notifications.value"
      :unread-count="notification.unreadCount.value"
      :loading="notification.loading.value"
      :has-next="notification.hasNext.value"
      :theme="notificationTheme"
      @close="notification.closeCenter()"
      @mark-read="markNotificationRead"
      @read-all="markAllNotificationsRead"
      @dismiss="dismissNotification"
      @load-more="notification.loadMore()"
      @open-notification="openNotificationTarget"
    />

    <NotificationToastStack
      v-if="showNotificationSystem"
      :toasts="notification.toastItems.value"
      :theme="notificationTheme"
      @close="notification.dismissToast"
      @open-center="notification.openCenter()"
      @cta="openNotificationTarget"
    />

    <div
      v-if="showNotificationSystem"
      class="fixed right-3 bottom-3 sm:right-6 sm:bottom-5 z-[84] transition-all duration-300"
    >
      <button
        type="button"
        class="notification-trigger group relative inline-flex items-center gap-2 rounded-full border shadow-[0_14px_35px_rgba(2,6,23,0.5)] backdrop-blur-md transition-all duration-300 focus:outline-none"
        :class="notificationButtonClass"
        :aria-expanded="notification.centerOpen.value"
        aria-label="Ouvrir le centre de notifications"
        @click.stop="notification.toggleCenter()"
        @mouseenter="notificationButtonExpanded = true"
        @mouseleave="notificationButtonExpanded = false"
        @focusin="notificationButtonExpanded = true"
        @focusout="notificationButtonExpanded = false"
      >
        <Bell class="h-4 w-4" :class="notificationIconClass" />
        <span
          v-if="notificationButtonExpanded"
          class="text-xs font-semibold tracking-[0.02em]"
          :class="notificationLabelClass"
        >
          Notifications
        </span>
        <span
          v-if="notification.unreadCount.value > 0"
          class="absolute -top-1.5 -right-1.5 inline-flex min-w-[20px] h-5 items-center justify-center rounded-full border border-emerald-200/45 bg-emerald-400 text-[11px] font-bold text-slate-900 px-1.5"
        >
          {{ unreadBadge }}
        </span>
      </button>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, watch, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useRoute, useRouter, RouterLink } from 'vue-router'
import { useAuthStore } from '@/store/authStore'
import { useNotificationStore } from '@/store/notificationStore'
import { useTheme } from '@/composables/useTheme'
import NotificationCenter from '@/components/NotificationCenter.vue'
import NotificationToastStack from '@/components/NotificationToastStack.vue'
import { Home, BarChart3, Boxes, Bell } from 'lucide-vue-next'

const route = useRoute()
const router = useRouter()
const { theme } = useTheme()

const isStats = computed(() => route.path === '/stats')
const isStatsLight = computed(() => isStats.value && theme.value === 'light')
const isGestionRoute = computed(() => route.path === '/gestion')
const isLightAppShell = computed(() =>
  ['/', '/gestion', '/a-propos', '/confidentialite'].includes(route.path),
)
const isFullBleedRoute = computed(() => route.meta.fullBleed === true)
const layoutVars = computed(() => {
  const edgeGap = isFullBleedRoute.value ? 'clamp(10px, 2vw, 32px)' : 'clamp(14px, 2vw, 40px)'
  const shellMaxWidth = route.meta.wideContent ? '1840px' : '1640px'
  return {
    '--layout-shell-max-width': isFullBleedRoute.value ? '100%' : shellMaxWidth,
    '--layout-shell-gutter': edgeGap,
    '--app-edge-gap': edgeGap,
  }
})
const isAuthRoute = computed(() =>
  [
    'auth',
    'discover',
    'forgot-password',
    'reset-password',
    'authCallback',
    'verify-email',
    'verify-email-short',
  ].includes(route.name),
)
const showPrimaryNav = computed(() => !isAuthRoute.value && route.meta.hidePrimaryNav !== true)
const statsTemplateModeActive = ref(false)
const isAccountRoute = computed(() => route.name === 'account')
const showHeaderNav = computed(() => showPrimaryNav.value && !isStats.value)
const showHeaderUserMenu = computed(
  () => showPrimaryNav.value && !isStats.value && !isAccountRoute.value,
)
const showAppHeader = computed(() => showHeaderNav.value || showHeaderUserMenu.value)
const fullBleedHeaderOffsetClass = computed(() => {
  const needsOffset =
    route.meta.fullBleed === true && route.meta.allowScroll === true && !isAuthRoute.value
  if (!needsOffset) return ''
  if (!showAppHeader.value) return ''
  if (showHeaderNav.value && isStats.value) return 'layout-fullbleed--with-header-stats'
  return showHeaderNav.value
    ? 'layout-fullbleed--with-header'
    : 'layout-fullbleed--with-header-compact'
})

const navBubble = ref(false)
const homeHeaderHidden = ref(false)
const footerVisible = ref(false)
const pageScroll = ref(null)
const scrollTarget = ref(null)
let scrollFrame = 0

const compactLink = (path) => {
  const active = route.path === path
  const idleClass =
    isStatsLight.value || isLightAppShell.value
      ? 'text-slate-900/80 hover:text-black hover:bg-slate-900/8'
      : 'text-white/80 hover:text-white hover:bg-white/5'
  const activeClass =
    isStatsLight.value || isLightAppShell.value
      ? 'bg-emerald-500/14 text-black'
      : 'bg-white/10 text-emerald-200'
  return [
    'nav-link px-4 py-2 rounded-full text-sm font-medium transition-colors duration-200 inline-flex items-center gap-2',
    active ? activeClass : idleClass,
  ]
}

const menuOpen = ref(false)
const mobileMenuOpen = ref(false)

const auth = useAuthStore()
const notification = useNotificationStore()
let notificationInitTimer = null
const idleTimeoutMs = 10 * 60 * 1000
const lastActivity = ref(Date.now())
let idleTimer = null

const notificationHiddenRoutes = new Set(['stats', 'gestion'])
const showNotificationSystem = computed(
  () =>
    !!auth.token?.value &&
    !isAuthRoute.value &&
    !notificationHiddenRoutes.has(String(route.name || '')),
)
const unreadBadge = computed(() =>
  notification.unreadCount.value > 99 ? '99+' : String(notification.unreadCount.value),
)
const notificationButtonExpanded = ref(false)
const notificationTheme = computed(() => (route.path === '/' || isStatsLight.value ? 'home' : 'dark'))
const notificationButtonClass = computed(() => {
  const expanded = notificationButtonExpanded.value
  if (notificationTheme.value === 'home') {
    return expanded
      ? 'bg-gradient-to-br from-teal-600 via-cyan-600 to-sky-700 border-white/70 text-white px-3.5 py-2 shadow-[0_16px_36px_rgba(14,116,144,0.28)] hover:shadow-[0_18px_44px_rgba(14,116,144,0.34)]'
      : 'bg-gradient-to-br from-teal-600 via-cyan-600 to-sky-700 border-white/70 text-white p-2.5 shadow-[0_16px_36px_rgba(14,116,144,0.28)] hover:shadow-[0_18px_44px_rgba(14,116,144,0.34)]'
  }
  return expanded
    ? 'bg-slate-900/88 border-slate-600 text-slate-100 px-3.5 py-2'
    : 'bg-slate-900/80 border-slate-600 text-slate-100 p-2'
})
const notificationIconClass = computed(() =>
  notificationTheme.value === 'home' ? 'text-white' : 'text-emerald-100',
)
const notificationLabelClass = computed(() =>
  notificationTheme.value === 'home' ? 'text-white' : 'text-slate-100',
)

const onStatsTemplateModeChange = (event) => {
  const active = Boolean(event?.detail?.active)
  statsTemplateModeActive.value = active
}

if (typeof window !== 'undefined') {
  window.addEventListener('snk:stats-template-mode', onStatsTemplateModeChange)
}

const currentUser = computed(() => {
  const u = auth.user
  return u && typeof u === 'object' && 'value' in u ? u.value : u
})

const initials = computed(() => {
  const u = currentUser.value
  if (!u) return 'NN'
  const f = u.firstName?.[0] || ''
  const l = u.lastName?.[0] || ''
  return (f + l || 'NN').toUpperCase()
})

const shouldTrackIdle = computed(() => !!auth.token?.value && !isAuthRoute.value)

function markActivity() {
  lastActivity.value = Date.now()
}

function handleIdleCheck() {
  if (!shouldTrackIdle.value) return
  const now = Date.now()
  if (now - lastActivity.value >= idleTimeoutMs) {
    stopIdleWatch()
    auth.logout()
    router.replace({ name: 'auth', query: { mode: 'login', reason: 'idle' } })
  }
}

function startIdleWatch() {
  stopIdleWatch()
  if (!shouldTrackIdle.value) return
  markActivity()
  const events = ['pointerdown', 'keydown', 'visibilitychange']
  events.forEach((evt) => window.addEventListener(evt, markActivity, { passive: true }))
  idleTimer = window.setInterval(handleIdleCheck, 30_000)
}

function stopIdleWatch() {
  const events = ['pointerdown', 'keydown', 'visibilitychange']
  events.forEach((evt) => window.removeEventListener(evt, markActivity))
  if (idleTimer) {
    clearInterval(idleTimer)
    idleTimer = null
  }
}

const isActiveMobile = (path) =>
  route.path === path
    ? isStatsLight.value || isLightAppShell.value
      ? 'bg-emerald-500/14 text-slate-950'
      : 'bg-gray-900 text-purple-400'
    : isStatsLight.value || isLightAppShell.value
      ? 'text-slate-700 hover:bg-slate-900/6 hover:text-slate-950'
      : 'text-gray-300 hover:bg-gray-700 hover:text-white'

const closeMenus = () => {
  menuOpen.value = false
  mobileMenuOpen.value = false
}

const toggleUserMenu = () => {
  menuOpen.value = !menuOpen.value
  if (menuOpen.value) mobileMenuOpen.value = false
}

const toggleMobileMenu = () => {
  mobileMenuOpen.value = !mobileMenuOpen.value
  if (mobileMenuOpen.value) menuOpen.value = false
}

const updateScrollState = () => {
  scrollFrame = 0
  const scroller = pageScroll.value
  const useDocumentScroll = isLightAppShell.value || !scroller
  const top = useDocumentScroll ? window.scrollY : scroller.scrollTop
  const client = useDocumentScroll ? window.innerHeight : scroller.clientHeight
  const scrollHeight = useDocumentScroll
    ? Math.max(document.body.scrollHeight, document.documentElement.scrollHeight)
    : scroller.scrollHeight
  const nextHomeHeaderHidden = isLightAppShell.value && top > 110
  const nextNavBubble = !isStats.value && !isLightAppShell.value && top > 12
  const nextFooterVisible = !isStats.value && top + client >= scrollHeight - 140

  if (homeHeaderHidden.value !== nextHomeHeaderHidden) homeHeaderHidden.value = nextHomeHeaderHidden
  if (navBubble.value !== nextNavBubble) navBubble.value = nextNavBubble
  if (footerVisible.value !== nextFooterVisible) footerVisible.value = nextFooterVisible
}

const handleScroll = () => {
  if (scrollFrame) return
  scrollFrame = window.requestAnimationFrame(updateScrollState)
}

const attachScroll = () => {
  const target = isLightAppShell.value ? window : pageScroll.value || window
  scrollTarget.value = target
  target.addEventListener('scroll', handleScroll, { passive: true })
  updateScrollState()
}

const detachScroll = () => {
  if (scrollFrame) {
    window.cancelAnimationFrame(scrollFrame)
    scrollFrame = 0
  }
  if (scrollTarget.value) {
    scrollTarget.value.removeEventListener('scroll', handleScroll)
    scrollTarget.value = null
  }
}

const markNotificationRead = async (notificationId) => {
  try {
    await notification.markAsRead(notificationId)
  } catch (e) {
    console.warn('notification mark-read failed', e)
  }
}

const markAllNotificationsRead = async () => {
  try {
    await notification.markAllAsRead()
  } catch (e) {
    console.warn('notification read-all failed', e)
  }
}

const dismissNotification = async (notificationId) => {
  try {
    await notification.dismissNotification(notificationId)
  } catch (e) {
    console.warn('notification dismiss failed', e)
  }
}

const openNotificationTarget = async (notificationItem) => {
  if (!notificationItem) return
  if (notificationItem.id) {
    await markNotificationRead(notificationItem.id)
  }
  if (notificationItem.ctaRoute) {
    notification.closeCenter()
    const isStockNotification =
      notificationItem.entityType === 'STOCK_ITEM' &&
      Number.isFinite(Number(notificationItem.entityId))
    if (isStockNotification && notificationItem.ctaRoute === '/gestion') {
      router
        .push({
          path: '/gestion',
          query: {
            openItemId: String(notificationItem.entityId),
            source: 'notification',
          },
        })
        .catch(() => {})
      return
    }
    router.push(notificationItem.ctaRoute).catch(() => {})
  }
}

const onWindowClick = (e) => {
  if (e.target.closest('header')) return
  if (
    notification.centerOpen.value &&
    !e.target.closest('.notification-center-root') &&
    !e.target.closest('.notification-trigger')
  ) {
    notification.closeCenter()
  }
  closeMenus()
}

const onKeyDown = (e) => {
  const target = e.target
  const isTypingTarget =
    target instanceof HTMLElement &&
    (target.closest('input, textarea, select, [contenteditable="true"]') ||
      target.getAttribute('role') === 'textbox')

  if (!isTypingTarget && (e.key === '/' || e.key.toLowerCase() === 'r')) {
    e.preventDefault()
    focusGlobalSearch()
    return
  }

  if (e.key === 'Escape') {
    closeMenus()
    notification.closeCenter()
  }
}

const focusGlobalSearch = async () => {
  if (route.name !== 'home') {
    await router.push({ name: 'home' }).catch(() => {})
    await nextTick()
  }

  window.dispatchEvent(new CustomEvent('snk:focus-global-search'))
}

const clearNotificationInitTimer = () => {
  if (notificationInitTimer) {
    window.clearTimeout(notificationInitTimer)
    notificationInitTimer = null
  }
}

const scheduleNotificationInit = () => {
  if (typeof window === 'undefined' || !showNotificationSystem.value) return
  clearNotificationInitTimer()
  notificationInitTimer = window.setTimeout(() => {
    notification.init().catch((e) => {
      console.warn('notification init failed', e)
    })
    notificationInitTimer = null
  }, 180)
}

onMounted(() => {
  window.addEventListener('click', onWindowClick)
  window.addEventListener('keydown', onKeyDown)
  attachScroll()
  startIdleWatch()
})

onBeforeUnmount(() => {
  window.removeEventListener('click', onWindowClick)
  window.removeEventListener('keydown', onKeyDown)
  window.removeEventListener('snk:stats-template-mode', onStatsTemplateModeChange)
  document.documentElement.classList.remove('layout-light-document-scroll')
  document.body.classList.remove('layout-light-document-scroll')
  detachScroll()
  stopIdleWatch()
  clearNotificationInitTimer()
  notification.teardown({ clearState: true })
})

watch(
  () => route.meta.fullBleed,
  (v) => {
    document.body.classList.toggle('no-scroll', !!v)
  },
  { immediate: true },
)

watch(
  () => isLightAppShell.value,
  (v) => {
    document.documentElement.classList.toggle('layout-light-document-scroll', !!v)
    document.body.classList.toggle('layout-light-document-scroll', !!v)
  },
  { immediate: true },
)

watch(
  () => route.fullPath,
  async () => {
    if (route.path !== '/stats') statsTemplateModeActive.value = false
    closeMenus()
    notification.closeCenter()
    notificationButtonExpanded.value = false
    detachScroll()
    await nextTick()
    attachScroll()
    markActivity()
  },
)

watch(
  () => shouldTrackIdle.value,
  () => {
    startIdleWatch()
  },
)

watch(
  () => showNotificationSystem.value,
  (visible) => {
    if (visible) return
    notification.closeCenter()
    notificationButtonExpanded.value = false
  },
)

watch(
  () => [auth.token?.value, isAuthRoute.value],
  ([token, authRoute]) => {
    clearNotificationInitTimer()
    if (!token) {
      notification.teardown({ clearState: true })
      return
    }

    if (authRoute) {
      notification.teardown()
      return
    }

    scheduleNotificationInit()
  },
  { immediate: true },
)

const goToAuth = (mode) => {
  closeMenus()
  router.push({ name: 'auth', query: { mode } })
}

const goToAccount = () => {
  closeMenus()
  router.push({ name: 'account' })
}

const logout = () => {
  auth.logout()
  closeMenus()
  router.push({ name: 'auth', query: { mode: 'login' } })
}
</script>

<style>
.widget-fullscreen-open header {
  opacity: 0;
  pointer-events: none;
  transition: opacity 120ms ease;
}

.toolbar {
  animation: toolbarIn 280ms cubic-bezier(0.2, 0.9, 0.2, 1) both;
  top: 76px;
}

@keyframes toolbarIn {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

html,
body,
#app {
  height: 100%;
}

html.layout-light-document-scroll,
body.layout-light-document-scroll {
  height: auto;
  min-height: 100%;
  scrollbar-width: none;
  -ms-overflow-style: none;
}

html.layout-light-document-scroll body,
html.layout-light-document-scroll #app {
  height: auto;
  min-height: 100%;
}

body.layout-light-document-scroll {
  overflow-x: hidden;
  overflow-y: auto;
  background: #f7f4ee;
}

html.layout-light-document-scroll::-webkit-scrollbar,
body.layout-light-document-scroll::-webkit-scrollbar {
  width: 0;
  height: 0;
  display: none;
}

.app-layout-root {
  min-height: 100vh;
  height: 100dvh;
  min-width: 0;
  overflow: hidden;
}

.app-layout-root.layout-home-bg {
  height: auto !important;
  min-height: 100dvh;
  overflow: visible;
}

.app-layout-root.layout-document-flow {
  display: block;
}

.layout-main--document-flow {
  display: block;
  min-height: 0 !important;
  overflow: visible;
}

.layout-home-bg {
  background: #f7f4ee;
}

.app-layout-root.layout-gestion-bg {
  background: #f7f4ee;
}

.app-layout-root.layout-home-bg .layout-main--document-flow,
.app-layout-root.layout-home-bg .layout-scroll-inner--document-flow,
.app-layout-root.layout-home-bg .layout-page-content--document-flow,
.app-layout-root.layout-gestion-bg .layout-main--document-flow,
.app-layout-root.layout-gestion-bg .layout-scroll-inner--document-flow,
.app-layout-root.layout-gestion-bg .layout-page-content--document-flow {
  background: #f7f4ee;
}

.app-layout-root.layout-gestion-bg .layout-app-header.is-stuck.is-light {
  background: rgba(247, 244, 238, 0.92);
}

.layout-shell-row {
  width: min(100%, var(--layout-shell-max-width, 1536px));
  max-width: 100%;
  box-sizing: border-box;
  margin-inline: auto;
  padding-inline: var(--layout-shell-gutter, clamp(16px, 2.2vw, 32px));
}

.layout-shell-row--header {
  width: 100%;
  max-width: none;
}

.layout-app-header {
  padding-top: env(safe-area-inset-top, 0px);
}

.layout-app-header--hidden {
  opacity: 0;
  pointer-events: none;
  transform: translateY(calc(-100% - 1rem));
}

.layout-app-header.is-stuck {
  border-bottom: 1px solid transparent;
}

.layout-app-header.is-stuck.is-light {
  border-bottom-color: rgba(203, 213, 225, 0.72);
  background: rgba(247, 244, 238, 0.92);
  box-shadow: 0 10px 28px rgba(15, 23, 42, 0.08);
  backdrop-filter: blur(16px) saturate(130%);
}

.layout-app-header.is-stuck.is-dark {
  border-bottom-color: rgba(148, 163, 184, 0.14);
  background: rgba(2, 6, 23, 0.78);
  box-shadow: 0 12px 32px rgba(2, 6, 23, 0.28);
  backdrop-filter: blur(16px) saturate(130%);
}

.layout-fullbleed {
  position: relative;
  height: 100%;
  width: 100%;
  min-width: 0;
}

.layout-main,
.layout-scroll-inner,
.layout-page-content {
  min-width: 0;
}

.layout-page-content > * {
  min-width: 0;
}

.layout-scroll {
  overflow-x: hidden;
  overflow-y: auto;
  overscroll-behavior-y: contain;
  overscroll-behavior-x: none;
  scrollbar-gutter: stable;
  -webkit-overflow-scrolling: touch;
}

.layout-scroll--document-flow {
  display: block;
  height: auto !important;
  min-height: 0;
  overflow: visible !important;
  overscroll-behavior: auto;
}

.layout-scroll-inner--document-flow,
.layout-page-content--document-flow,
.layout-document-flow .page-view {
  height: auto !important;
  min-height: 0 !important;
}

.layout-scroll--hidden-scrollbar {
  scrollbar-gutter: auto;
  scrollbar-width: none;
}

.layout-scroll--hidden-scrollbar::-webkit-scrollbar {
  width: 0;
  height: 0;
  display: none;
}

.layout-page-content {
  padding-top: clamp(4.5rem, 7vh, 5.25rem);
  padding-bottom: calc(5.25rem + env(safe-area-inset-bottom, 0px));
}

.layout-home-bg .layout-scroll {
  scrollbar-color: rgba(14, 116, 144, 0.5) rgba(248, 250, 252, 0.78);
}

.layout-home-bg .layout-scroll::-webkit-scrollbar-track {
  background: rgba(248, 250, 252, 0.78);
}

.layout-home-bg .layout-scroll::-webkit-scrollbar-thumb {
  border-color: rgba(248, 250, 252, 0.78);
  background-color: rgba(14, 116, 144, 0.5);
}

.layout-home-bg .layout-scroll.layout-scroll--hidden-scrollbar {
  scrollbar-gutter: auto;
  scrollbar-width: none;
}

.layout-home-bg .layout-scroll.layout-scroll--hidden-scrollbar::-webkit-scrollbar {
  width: 0 !important;
  height: 0 !important;
  display: none !important;
}

.layout-home-bg .layout-scroll.layout-scroll--hidden-scrollbar::-webkit-scrollbar-track,
.layout-home-bg .layout-scroll.layout-scroll--hidden-scrollbar::-webkit-scrollbar-thumb {
  border: 0 !important;
  background: transparent !important;
}

.layout-home-bg,
.layout-home-bg * {
  scrollbar-width: none !important;
  -ms-overflow-style: none;
}

.layout-home-bg::-webkit-scrollbar,
.layout-home-bg *::-webkit-scrollbar {
  width: 0 !important;
  height: 0 !important;
  display: none !important;
}

.layout-fullbleed--with-header {
  padding-top: calc(72px + env(safe-area-inset-top, 0px));
}

.layout-fullbleed--with-header-stats {
  padding-top: calc(46px + env(safe-area-inset-top, 0px));
}

.layout-fullbleed--with-header-compact {
  padding-top: calc(24px + env(safe-area-inset-top, 0px));
}

.layout-menu-actions {
  display: grid;
  gap: 2px;
}

.layout-menu-actions > button {
  width: 100%;
  text-align: left;
  padding: 8px 16px;
  font-size: 0.875rem;
  transition:
    background-color 140ms ease,
    color 140ms ease;
}

.layout-menu-actions:not(.is-dark) > button:hover,
.layout-menu-actions:not(.is-dark) > button:active {
  background: #e2e8f0;
}

.layout-menu-actions.is-dark > button {
  color: #e2e8f0;
}

.layout-menu-actions.is-dark > button:hover,
.layout-menu-actions.is-dark > button:active {
  background: rgba(148, 163, 184, 0.18);
}

.layout-mobile-menu-panel {
  max-height: calc(100dvh - 5.5rem);
  overflow-y: auto;
}

.layout-footer-pill {
  max-width: calc(100vw - 24px);
  min-width: 0;
}

.layout-footer-pill > * {
  white-space: nowrap;
}

@media (max-width: 767px) {
  .layout-shell-row {
    padding-inline-start: max(12px, env(safe-area-inset-left));
    padding-inline-end: max(12px, env(safe-area-inset-right));
  }

  .layout-page-content {
    padding-top: calc(4.5rem + env(safe-area-inset-top, 0px));
    padding-bottom: calc(5.75rem + env(safe-area-inset-bottom, 0px));
  }

  .layout-fullbleed--with-header {
    padding-top: calc(66px + env(safe-area-inset-top, 0px));
  }

  .layout-fullbleed--with-header-stats {
    padding-top: calc(44px + env(safe-area-inset-top, 0px));
  }

  .layout-fullbleed--with-header-compact {
    padding-top: calc(18px + env(safe-area-inset-top, 0px));
  }

  .layout-footer-pill {
    gap: 0.75rem;
    overflow-x: auto;
    padding: 0.65rem 0.9rem;
    scrollbar-width: none;
    font-size: 0.75rem;
  }

  .layout-footer-pill::-webkit-scrollbar {
    display: none;
  }
}

@media (max-width: 420px) {
  .layout-shell-row {
    padding-inline-start: max(10px, env(safe-area-inset-left));
    padding-inline-end: max(10px, env(safe-area-inset-right));
  }

  .layout-page-content {
    padding-top: calc(4rem + env(safe-area-inset-top, 0px));
    padding-bottom: calc(5.25rem + env(safe-area-inset-bottom, 0px));
  }
}

@media (min-width: 1800px) {
  .layout-shell-row:not(.layout-shell-row--header) {
    padding-inline: clamp(32px, 3vw, 56px);
  }

  .layout-page-content {
    padding-top: clamp(5rem, 5vh, 6rem);
  }
}
</style>
