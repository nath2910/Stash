<template>
  <section class="legal-document">
    <header>
      <p class="kicker">MyStash - Transparence et informations legales</p>
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
import { RouterLink } from 'vue-router'

defineProps({
  title: { type: String, required: true },
  description: { type: String, default: '' },
})
</script>

<style scoped>
.legal-document { max-width: 900px; margin: auto; padding: clamp(2rem, 5vw, 4rem) 1rem 5rem; color: #334155; }
header, nav, .legal-content { padding: clamp(1.2rem, 3vw, 2rem); margin-bottom: 1rem; border: 1px solid #e2e8f0; border-radius: 1rem; background: #ffffff; box-shadow: 0 18px 48px rgba(15, 23, 42, .06); }
h1 { font-size: clamp(1.8rem, 4vw, 3rem); color: #0f172a; font-weight: 800; margin: .6rem 0; }
.kicker { color: #1d4ed8; font-size: .75rem; font-weight: 800; letter-spacing: .08em; text-transform: uppercase; }
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
  .legal-document { padding-top: 1.25rem; }
  :deep(table) { display: block; overflow-x: auto; white-space: normal; }
}
</style>
