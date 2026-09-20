<template>
  <section class="legal-document">
    <header>
      <div class="legal-document__topline">
        <p class="kicker">MyStash - Transparence et informations legales</p>
        <button
          type="button"
          class="legal-app-return"
          :disabled="returningToApp"
          @click="returnToApp"
        >
          {{ returningToApp ? 'Ouverture…' : isAuthenticated ? 'Retour à l’application' : 'Se connecter' }}
          <span aria-hidden="true">→</span>
        </button>
      </div>
      <h1>{{ title }}</h1>
      <p v-if="description" class="description">{{ description }}</p>
      <p class="version">Derniere mise a jour : 9 septembre 2026</p>
    </header>

    <nav aria-label="Informations legales">
      <RouterLink to="/legal">Mentions legales</RouterLink>
      <RouterLink to="/legal/cgu">Conditions d'utilisation</RouterLink>
      <RouterLink to="/privacy">Confidentialite</RouterLink>
      <RouterLink to="/cookies">Cookies et stockage</RouterLink>
    </nav>

    <div class="legal-content"><slot /></div>
  </section>
</template>

<script setup>
import { computed, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { useAuthStore } from '@/store/authStore'
import { useBillingStore } from '@/store/billingStore'

defineProps({
  title: { type: String, required: true },
  description: { type: String, default: '' },
})

const auth = useAuthStore()
const billing = useBillingStore()
const router = useRouter()
const isAuthenticated = computed(() => Boolean(auth.token.value))
const returningToApp = ref(false)

async function returnToApp() {
  if (returningToApp.value) return

  if (!isAuthenticated.value) {
    await router.push({ name: 'auth', query: { mode: 'login' } })
    return
  }

  returningToApp.value = true
  try {
    // Resolve billing before leaving this public page. The home guard can then use
    // the fresh cached answer instead of briefly sending an entitled user to /abo.
    await billing.fetchStatus()
    await router.push({ name: 'home' })
  } finally {
    returningToApp.value = false
  }
}
</script>

<style scoped>
.legal-document { max-width: 900px; margin: auto; padding: clamp(2rem, 5vw, 4rem) 1rem 5rem; color: #334155; }
header, nav, .legal-content { padding: clamp(1.2rem, 3vw, 2rem); margin-bottom: 1rem; border: 1px solid #e2e8f0; border-radius: 1rem; background: #ffffff; box-shadow: 0 18px 48px rgba(15, 23, 42, .06); }
.legal-document__topline { display: flex; align-items: center; justify-content: space-between; gap: 1rem; }
h1 { font-size: clamp(1.8rem, 4vw, 3rem); color: #0f172a; font-weight: 800; margin: .6rem 0; }
.kicker { color: #1d4ed8; font-size: .75rem; font-weight: 800; letter-spacing: .08em; text-transform: uppercase; }
.legal-app-return { display: inline-flex; flex: 0 0 auto; align-items: center; gap: .45rem; border: 1px solid #bfdbfe; border-radius: .7rem; background: #eff6ff; color: #1d4ed8; padding: .55rem .75rem; font: inherit; font-size: .8rem; font-weight: 800; line-height: 1; text-decoration: none; cursor: pointer; transition: background-color .16s ease, border-color .16s ease, transform .16s ease; }
.legal-app-return:hover:not(:disabled) { border-color: #60a5fa; background: #dbeafe; color: #1e40af; transform: translateY(-1px); }
.legal-app-return:disabled { cursor: wait; opacity: .72; }
.legal-app-return:focus-visible { outline: 3px solid rgba(59, 130, 246, .35); outline-offset: 3px; }
.description { max-width: 680px; color: #475569; font-size: 1.05rem; line-height: 1.65; }
.version { margin-top: 1rem; color: #64748b; font-size: .82rem; }
nav { display: flex; gap: 1rem; flex-wrap: wrap; }
:deep(a) { color: #1d4ed8; text-decoration: underline; }
:deep(nav a) { border-radius: .7rem; padding: .45rem .7rem; font-weight: 700; text-decoration: none; }
:deep(nav a.router-link-active) { background: #dbeafe; color: #1e3a8a; }
:deep(h2) { font-size: 1.2rem; font-weight: 700; color: #0f172a; margin: 1.5rem 0 .6rem; }
:deep(p), :deep(li) { line-height: 1.8; margin-bottom: .7rem; }
:deep(ul) { padding-left: 1.2rem; list-style: disc; }
:deep(table) { width: 100%; margin: 1rem 0 1.5rem; font-size: .9rem; border-collapse: collapse; }
:deep(td), :deep(th) { text-align: left; border-bottom: 1px solid #cbd5e1; padding: .6rem; vertical-align: top; }
:deep(th) { color: #0f172a; background: #f1f5f9; font-weight: 800; }
:deep(code) { border-radius: .35rem; background: #e2e8f0; color: #0f172a; padding: .1rem .3rem; font-size: .82em; }
:deep(.legal-summary) { margin-bottom: 1.5rem; border-left: 4px solid #2563eb; border-radius: .8rem; background: #eff6ff; padding: 1rem 1.1rem; }
:deep(.legal-summary strong) { color: #1e3a8a; }
:deep(.legal-summary p) { margin: .35rem 0 0; color: #334155; }
@media (max-width: 680px) {
  .legal-document { padding: 1rem 0.85rem 3rem; }
  header, nav, .legal-content { padding: 1rem; border-radius: .8rem; }
  .legal-document__topline { align-items: flex-start; flex-direction: column; }
  nav { display: grid; gap: .45rem; }
  :deep(nav a) { width: 100%; padding: .55rem .65rem; }
  :deep(table),
  :deep(thead),
  :deep(tbody),
  :deep(tr),
  :deep(th),
  :deep(td) {
    display: block;
    width: 100%;
  }
  :deep(table) {
    border-collapse: separate;
    border-spacing: 0;
    font-size: .86rem;
  }
  :deep(thead) {
    display: none;
  }
  :deep(tr) {
    margin: .75rem 0;
    border: 1px solid #dbe4ef;
    border-radius: .85rem;
    background: #f8fafc;
    overflow: hidden;
  }
  :deep(td),
  :deep(th) {
    border-bottom: 1px solid #e2e8f0;
    padding: .7rem .75rem;
    overflow-wrap: anywhere;
  }
  :deep(td:last-child),
  :deep(th:last-child) {
    border-bottom: 0;
  }
}
</style>
