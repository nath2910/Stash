<template>
  <div
    class="h-screen flex flex-col font-poppins bg-slate-950"
    :class="isStatsLight ? 'text-black' : 'text-slate-100'"
    :style="layoutVars"
  >
    <!-- Header -->
    <template v-if="!isAuthRoute">
      <!-- Header for stats: simple bar -->
      <header
        class="fixed left-0 right-0 z-50 pointer-events-none"
        :class="isStats ? 'top-2' : 'top-4'"
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
              class="md:hidden text-gray-300 hover:text-white p-2 rounded-xl hover:bg-white/5 transition"
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
            v-motion
            :initial="false"
            :variants="navVariants"
            :animate="navBubble ? 'compact' : 'normal'"
            :transition="navSpring"
            class="hidden md:flex items-center justify-center pointer-events-auto mx-auto"
            :class="
              navBubble || isStatsLight
                ? isStatsLight
                  ? 'gap-3 px-4 py-2 rounded-full bg-white/82 border border-slate-200/90 shadow-[0_12px_28px_rgba(15,23,42,0.15)] backdrop-blur-md'
                  : 'gap-3 px-4 py-2 rounded-full bg-slate-900/70 border border-white/10 shadow-[0_12px_30px_rgba(0,0,0,0.35)] backdrop-blur-md'
                : 'gap-8 px-8 py-3 rounded-full bg-transparent text-white'
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
                class="h-9 w-9 rounded-full flex items-center justify-center border transition focus:outline-none backdrop-blur-md"
                :class="
                  isStatsLight
                    ? 'bg-white/85 border-slate-300 hover:border-emerald-500/45'
                    : 'bg-slate-900/70 border-white/10 hover:border-emerald-300/50'
                "
                aria-label="Menu utilisateur"
                :aria-expanded="menuOpen"
              >
                <span class="text-sm font-semibold" :class="isStatsLight ? 'text-black' : 'text-white'">{{ initials }}</span>
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
          <div class="mt-2 rounded-2xl border backdrop-blur p-2 bg-gray-900/70 border-white/10">
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
    <main class="flex-1 min-h-0 bg-slate-950/30">
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

      <div v-else ref="pageScroll" class="h-full overflow-auto">
        <div class="relative min-h-full">
          <div class="pointer-events-none absolute inset-0 -z-10 overflow-hidden" aria-hidden="true">
            <div class="absolute -top-40 right-[-10%] h-96 w-96 rounded-full blur-3xl bg-emerald-400/4"></div>
            <div class="absolute bottom-[-30%] left-[-10%] h-[30rem] w-[30rem] rounded-full blur-3xl bg-amber-400/4"></div>
            <div class="absolute inset-0 bg-[radial-gradient(circle_at_top,_rgba(15,23,42,0.25),_transparent_60%)]"></div>
          </div>
          <div class="layout-shell-row min-h-full py-10 sm:py-12 pb-16">
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
        class="pointer-events-auto inline-flex items-center gap-6 px-7 py-3 rounded-full border backdrop-blur-md text-sm shadow-[0_14px_32px_rgba(0,0,0,0.38)] border-white/10 bg-slate-900/85"
      >
        <span class="font-jetbrains-mono">&copy; {{ new Date().getFullYear() }} - Stash</span>
        <a href="#" class="hover:underline">A propos</a>
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
        <Bell class="h-4 w-4" :class="notificationButtonExpanded ? 'text-emerald-100' : 'text-slate-100'" />
        <span
          v-if="notificationButtonExpanded"
          class="text-xs font-semibold tracking-[0.02em]"
          :class="isStatsLight ? 'text-slate-900' : 'text-slate-100'"
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
const isFullBleedRoute = computed(() => route.meta.fullBleed === true)
const layoutVars = computed(() => {
  const edgeGap = isFullBleedRoute.value ? 'clamp(12px, 2.4vw, 28px)' : 'clamp(16px, 2.2vw, 32px)'
  return {
    '--layout-shell-max-width': isFullBleedRoute.value ? '100%' : '1536px',
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
const showHeaderNav = computed(() => showPrimaryNav.value && !isStats.value)
const showHeaderUserMenu = computed(() => !isStats.value)
const fullBleedHeaderOffsetClass = computed(() => {
  const needsOffset = route.meta.fullBleed === true && route.meta.allowScroll === true && !isAuthRoute.value
  if (!needsOffset) return ''
  if (showHeaderNav.value && isStats.value) return 'layout-fullbleed--with-header-stats'
  return showHeaderNav.value ? 'layout-fullbleed--with-header' : 'layout-fullbleed--with-header-compact'
})

const navSpring = {
  type: 'spring',
  stiffness: 260,
  damping: 32,
  mass: 1.05,
}

const navVariants = {
  normal: {
    opacity: 1,
    y: 0,
    scale: 1,
    filter: 'blur(0px)',
  },
  compact: {
    opacity: 1,
    y: 0,
    scale: 0.975,
    rotateZ: 0.0001,
  },
}

const navBubble = ref(false)
const footerVisible = ref(false)
const pageScroll = ref(null)
const scrollTarget = ref(null)

const compactLink = (path) => {
  const active = route.path === path
  const idleClass = isStatsLight.value
    ? 'text-black hover:text-black hover:bg-slate-900/8'
    : 'text-white/80 hover:text-white hover:bg-white/5'
  const activeClass = isStatsLight.value
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
const notificationButtonClass = computed(() => {
  const expanded = notificationButtonExpanded.value
  if (isStatsLight.value) {
    return expanded
      ? 'bg-white/90 border-slate-300 text-slate-900 px-3.5 py-2'
      : 'bg-white/84 border-slate-300 text-slate-900 p-2'
  }
  return expanded
    ? 'bg-slate-900/88 border-slate-600 text-slate-100 px-3.5 py-2'
    : 'bg-slate-900/80 border-slate-600 text-slate-100 p-2'
})

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
    ? 'bg-gray-900 text-purple-400'
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

const handleScroll = () => {
  const scroller = pageScroll.value
  const top = scroller?.scrollTop ?? window.scrollY
  const client = scroller?.clientHeight ?? window.innerHeight
  const scrollHeight = scroller?.scrollHeight ?? document.body.scrollHeight
  navBubble.value = !isStats.value && top > 12
  footerVisible.value = !isStats.value && top + client >= scrollHeight - 140
}

const attachScroll = () => {
  const target = pageScroll.value || window
  scrollTarget.value = target
  target.addEventListener('scroll', handleScroll, { passive: true })
  handleScroll()
}

const detachScroll = () => {
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
      notificationItem.entityType === 'STOCK_ITEM' && Number.isFinite(Number(notificationItem.entityId))
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
  if (e.key === 'Escape') {
    closeMenus()
    notification.closeCenter()
  }
}

onMounted(() => {
  window.addEventListener('click', onWindowClick)
  window.addEventListener('keydown', onKeyDown)
  attachScroll()
  startIdleWatch()
  if (showNotificationSystem.value) {
    notification.init().catch((e) => {
      console.warn('notification init failed', e)
    })
  }
})

onBeforeUnmount(() => {
  window.removeEventListener('click', onWindowClick)
  window.removeEventListener('keydown', onKeyDown)
  window.removeEventListener('snk:stats-template-mode', onStatsTemplateModeChange)
  detachScroll()
  stopIdleWatch()
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
  () => route.fullPath,
  async () => {
    if (route.path !== '/stats') statsTemplateModeActive.value = false
    closeMenus()
    notification.closeCenter()
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
    if (!token) {
      notification.teardown({ clearState: true })
      return
    }

    if (authRoute) {
      notification.teardown()
      return
    }

    notification.init().catch((e) => {
      console.warn('notification init failed', e)
    })
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

.layout-shell-row {
  width: min(100%, var(--layout-shell-max-width, 1536px));
  margin-inline: auto;
  padding-inline: var(--layout-shell-gutter, clamp(16px, 2.2vw, 32px));
}

.layout-shell-row--header {
  width: 100%;
  max-width: none;
}

.layout-fullbleed {
  position: relative;
  height: 100%;
  width: 100%;
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
  transition: background-color 140ms ease, color 140ms ease;
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

@media (max-width: 767px) {
  .layout-fullbleed--with-header {
    padding-top: calc(66px + env(safe-area-inset-top, 0px));
  }

  .layout-fullbleed--with-header-stats {
    padding-top: calc(44px + env(safe-area-inset-top, 0px));
  }

  .layout-fullbleed--with-header-compact {
    padding-top: calc(18px + env(safe-area-inset-top, 0px));
  }
}

</style>
