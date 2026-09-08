<template>
  <section class="legal-document">
    <header><p class="kicker">Stash · Informations légales</p><h1>{{ title }}</h1><p>Version du 8 septembre 2026</p></header>
    <nav aria-label="Informations légales">
      <RouterLink to="/legal">Mentions légales</RouterLink>
      <RouterLink to="/legal/cgv">CGV et abonnement</RouterLink>
      <RouterLink to="/privacy">Confidentialité</RouterLink>
      <RouterLink to="/cookies">Cookies et stockage</RouterLink>
    </nav>
    <div class="legal-content"><slot /></div>
  </section>
</template>
<script setup>
import { RouterLink } from 'vue-router'
defineProps({ title: { type: String, required: true } })
</script>
<style scoped>
.legal-document { max-width: 900px; margin: auto; padding: 6rem 1rem 8rem; color: #334155; }
header, nav, .legal-content { padding: clamp(1.2rem, 3vw, 2rem); margin-bottom: 1rem; border: 1px solid #e2e8f0; border-radius: 1.5rem; background: #f8fafc; }
h1 { font-size: clamp(1.8rem, 4vw, 3rem); color: #0f172a; font-weight: 800; margin: .6rem 0; }
.kicker { color: #1d4ed8; font-size: .8rem; }
nav { display: flex; gap: 1rem; flex-wrap: wrap; }
:deep(a) { color: #1d4ed8; text-decoration: underline; }
:deep(h2) { font-size: 1.2rem; font-weight: 700; color: #0f172a; margin: 1.5rem 0 .6rem; }
:deep(p), :deep(li) { line-height: 1.8; margin-bottom: .7rem; }
:deep(ul) { padding-left: 1.2rem; list-style: disc; }
:deep(table) { width: 100%; font-size: .9rem; border-collapse: collapse; }
:deep(td), :deep(th) { text-align: left; border-bottom: 1px solid #cbd5e1; padding: .6rem; vertical-align: top; }
</style>

