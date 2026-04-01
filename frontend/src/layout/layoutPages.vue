<template>
  <div
    class="min-h-screen min-h-[100dvh] flex flex-col font-poppins bg-slate-950"
    :class="isStatsLight ? 'text-black' : 'text-slate-100'"
  >
    <!-- Header -->
    <template v-if="!isAuthRoute">
      <!-- Header for stats: simple bar -->
      <header class="fixed top-2 sm:top-4 left-0 right-0 z-50 pointer-events-none">
        <div class="max-w-screen-2xl mx-auto px-4 sm:px-6 lg:px-8 h-14 flex items-center justify-between pointer-events-none">
          <!-- Left spacer / burger -->
          <div class="flex items-center pointer-events-auto">
            <button
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
          </div>

          <!-- Center nav -->
          <nav
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
          <div class="flex items-center justify-end pointer-events-auto">
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
        <div v-if="mobileMenuOpen" class="md:hidden px-4 pb-3 pointer-events-auto" @click.stop>
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
        class="relative h-full w-full"
        :class="allowFullBleedScroll ? 'overflow-auto' : 'overflow-hidden'"
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
          <div class="min-h-full max-w-screen-2xl mx-auto px-4 sm:px-6 lg:px-8 py-6 sm:py-8 lg:py-10 pb-16">
            <slot />
          </div>
        </div>
      </div>
    </main>

    <!-- Footer -->
    <footer
      v-if="!route.meta.fullBleed && !isAuthRoute"
      class="fixed left-0 right-0 bottom-2 sm:bottom-4 flex justify-center transition-opacity duration-500 ease-out pointer-events-none"
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

  </div>
</template>

<script setup>
import { computed, ref, watch, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useRoute, useRouter, RouterLink } from 'vue-router'
import { useAuthStore } from '@/store/authStore'
import { useTheme } from '@/composables/useTheme'
import { Home, BarChart3, Boxes } from 'lucide-vue-next'

const route = useRoute()
const router = useRouter()
const { theme } = useTheme()

const isStats = computed(() => route.path === '/stats')
const isStatsLight = computed(() => isStats.value && theme.value === 'light')
const isAuthRoute = computed(() =>
  [
    'auth',
    'forgot-password',
    'reset-password',
    'authCallback',
    'verify-email',
    'account',
    'abo',
    'abo-view',
  ].includes(route.name),
)
const allowFullBleedScroll = computed(() => route.meta.fullBleed && route.meta.allowScroll !== false)

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
const idleTimeoutMs = 10 * 60 * 1000
const lastActivity = ref(Date.now())
let idleTimer = null

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

const onWindowClick = (e) => {
  if (e.target.closest('header')) return
  closeMenus()
}

const onKeyDown = (e) => {
  if (e.key === 'Escape') closeMenus()
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
  detachScroll()
  stopIdleWatch()
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
    closeMenus()
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

</style>

