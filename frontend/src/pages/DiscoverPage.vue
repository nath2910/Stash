<template>
  <div class="min-h-dvh overflow-x-hidden bg-[#070b16] text-white">
    <main class="mx-auto flex w-full max-w-6xl flex-col gap-5 px-4 py-5 sm:gap-8 sm:px-6 sm:py-8 lg:px-8 lg:py-10">
      <section class="overflow-hidden rounded-2xl border border-white/10 bg-white/[0.04]">
        <div class="flex flex-wrap items-center justify-between gap-3 border-b border-white/10 px-5 py-4 sm:px-7">
          <router-link
            :to="{ name: 'auth', query: { mode: 'login' } }"
            class="inline-flex min-h-10 items-center rounded-xl border border-white/10 px-4 text-sm font-bold text-slate-200 transition hover:border-white/25 hover:bg-white/[0.04] hover:text-white"
          >
            Retour connexion
          </router-link>
          <span class="rounded-full border border-emerald-300/20 bg-emerald-300/10 px-3 py-1 text-xs font-bold text-emerald-200">
            Offre mensuelle
          </span>
        </div>

        <div class="grid gap-6 p-5 sm:p-7 lg:grid-cols-[1.08fr_0.92fr] lg:items-center">
          <div class="max-w-2xl">
            <p class="text-xs font-black uppercase tracking-[0.28em] text-emerald-300">MyStash</p>
            <h1 class="mt-4 text-4xl font-black leading-[0.98] tracking-tight sm:text-5xl lg:text-6xl">
              Ton stock, tes ventes et tes marges au même endroit.
            </h1>
            <p class="mt-5 text-base leading-8 text-slate-300 sm:text-lg">
              MyStash t'aide à garder une vision claire de ton activité : ce que tu as en stock,
              ce que tu as vendu, ce que tu gagnes vraiment, et ce qui mérite ton attention.
            </p>
            <div class="mt-6 flex flex-col gap-3 min-[460px]:flex-row">
              <router-link
                :to="{ name: 'auth', query: { mode: 'signup' } }"
                class="inline-flex min-h-12 items-center justify-center rounded-xl bg-gradient-to-r from-emerald-400 to-sky-400 px-5 text-sm font-black text-slate-950 shadow-[0_18px_50px_rgba(14,165,233,0.22)] transition hover:brightness-110"
              >
                Commencer
              </router-link>
              <router-link
                :to="{ name: 'auth', query: { mode: 'login' } }"
                class="inline-flex min-h-12 items-center justify-center rounded-xl border border-white/12 px-5 text-sm font-black text-white transition hover:border-white/28 hover:bg-white/[0.05]"
              >
                J'ai déjà un compte
              </router-link>
            </div>
          </div>

          <aside class="rounded-2xl border border-white/10 bg-[#0d1424] p-4 shadow-2xl shadow-black/25">
            <div class="flex items-center justify-between gap-3 border-b border-white/10 pb-3">
              <div>
                <p class="text-xs font-black uppercase tracking-[0.18em] text-slate-400">Vue d'ensemble</p>
                <h2 class="mt-1 text-lg font-black">Activité mensuelle</h2>
              </div>
              <span class="rounded-full bg-emerald-400/15 px-3 py-1 text-xs font-bold text-emerald-200">Actif</span>
            </div>

            <div class="mt-4 grid gap-3 sm:grid-cols-3 lg:grid-cols-1 xl:grid-cols-3">
              <div v-for="metric in previewMetrics" :key="metric.label" class="rounded-xl border border-white/10 bg-white/[0.04] p-3">
                <p class="text-xs font-bold uppercase tracking-[0.14em] text-slate-500">{{ metric.label }}</p>
                <p class="mt-2 text-2xl font-black">{{ metric.value }}</p>
                <p class="mt-1 text-xs text-slate-400">{{ metric.caption }}</p>
              </div>
            </div>

            <div class="mt-3 rounded-xl border border-white/10 bg-white/[0.035] p-3">
              <div class="mb-3 flex items-center justify-between text-xs text-slate-400">
                <span>Stock à suivre</span>
                <span>Priorités</span>
              </div>
              <div class="grid gap-2">
                <div v-for="row in previewRows" :key="row.name" class="grid grid-cols-[1fr_auto] items-center gap-3 rounded-lg bg-white/[0.04] px-3 py-2">
                  <span class="truncate text-sm font-bold text-slate-100">{{ row.name }}</span>
                  <span class="rounded-full px-2 py-1 text-xs font-bold" :class="row.class">{{ row.status }}</span>
                </div>
              </div>
            </div>
          </aside>
        </div>
      </section>

      <section class="grid gap-4 md:grid-cols-3">
        <article v-for="item in pillars" :key="item.title" class="rounded-2xl border border-white/10 bg-white/[0.045] p-5">
          <p class="text-xs font-black uppercase tracking-[0.18em] text-emerald-300">{{ item.kicker }}</p>
          <h2 class="mt-3 text-xl font-black">{{ item.title }}</h2>
          <p class="mt-3 text-sm leading-6 text-slate-300">{{ item.text }}</p>
        </article>
      </section>

      <section class="rounded-2xl border border-white/10 bg-white/[0.04] p-5 sm:p-7">
        <div class="max-w-3xl">
          <p class="text-xs font-black uppercase tracking-[0.24em] text-slate-500">Ce que tu débloques</p>
          <h2 class="mt-3 text-2xl font-black sm:text-3xl">Un espace propre pour suivre ton activité sans te disperser.</h2>
        </div>

        <div class="mt-6 grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
          <article v-for="feature in features" :key="feature.title" class="rounded-xl border border-white/10 bg-[#0d1424] p-4">
            <h3 class="text-base font-black">{{ feature.title }}</h3>
            <p class="mt-2 text-sm leading-6 text-slate-400">{{ feature.text }}</p>
          </article>
        </div>
      </section>

      <section class="grid gap-4 lg:grid-cols-[0.9fr_1.1fr]">
        <div class="rounded-2xl border border-white/10 bg-white/[0.04] p-5 sm:p-7">
          <p class="text-xs font-black uppercase tracking-[0.24em] text-slate-500">Pour qui</p>
          <h2 class="mt-3 text-2xl font-black">Pensé pour les vendeurs qui veulent savoir où ils en sont.</h2>
          <p class="mt-4 text-sm leading-7 text-slate-300">
            Sneakers, collectibles, pièces, accessoires ou stock multi-catégories : l'idée est
            simple, garder une base fiable et lisible avant que tout devienne compliqué.
          </p>
        </div>

        <div class="grid gap-3 sm:grid-cols-2">
          <div v-for="proof in trustPoints" :key="proof" class="rounded-xl border border-white/10 bg-[#0d1424] p-4 text-sm font-bold leading-6 text-slate-200">
            {{ proof }}
          </div>
        </div>
      </section>

      <section class="rounded-2xl border border-emerald-300/20 bg-emerald-300/[0.07] p-5 sm:p-7">
        <div class="flex flex-col gap-5 lg:flex-row lg:items-center lg:justify-between">
          <div>
            <p class="text-xs font-black uppercase tracking-[0.24em] text-emerald-200">Prêt à essayer</p>
            <h2 class="mt-3 text-2xl font-black">Crée ton compte, ajoute tes premiers items et garde le contrôle.</h2>
          </div>
          <router-link
            :to="{ name: 'auth', query: { mode: 'signup' } }"
            class="inline-flex min-h-12 shrink-0 items-center justify-center rounded-xl bg-white px-5 text-sm font-black text-slate-950 transition hover:bg-emerald-50"
          >
            Créer mon compte
          </router-link>
        </div>
      </section>
    </main>
  </div>
</template>

<script setup>
const previewMetrics = [
  { label: 'CA', value: '4 242 EUR', caption: '46 articles vendus' },
  { label: 'Profit', value: '1 229 EUR', caption: 'Marge suivie' },
  { label: 'Stock', value: '120', caption: 'Items actifs' },
]

const previewRows = [
  { name: 'Paires à relancer', status: 'À suivre', class: 'bg-sky-400/12 text-sky-200' },
  { name: 'Articles anciens', status: 'Priorité', class: 'bg-amber-300/12 text-amber-200' },
  { name: 'Ventes récentes', status: 'OK', class: 'bg-emerald-300/12 text-emerald-200' },
]

const pillars = [
  {
    kicker: 'Inventaire',
    title: 'Stock centralisé',
    text: 'Ajoute tes articles, retrouve-les vite et garde les infos utiles au même endroit.',
  },
  {
    kicker: 'Pilotage',
    title: 'Stats exploitables',
    text: 'Suis ton chiffre, ton profit et tes marges avec des indicateurs faciles à lire.',
  },
  {
    kicker: 'Suivi',
    title: "Moins d'oublis",
    text: "Repère les articles qui dorment, les actions à faire et les données à garder propres.",
  },
]

const features = [
  {
    title: 'Gestion rapide',
    text: "Création, recherche, filtres, catégories et édition des items pour aller plus vite au quotidien.",
  },
  {
    title: 'Tableaux de bord',
    text: "Des vues prêtes à l'emploi pour comprendre ton activité sans repartir de zéro.",
  },
  {
    title: 'Import / export',
    text: "Importe un fichier ou récupère tes données quand tu veux garder la main sur ton inventaire.",
  },
  {
    title: 'Abonnement souple',
    text: "Ton accès se gère en ligne, avec un portail Stripe pour le paiement et la résiliation.",
  },
]

const trustPoints = [
  'Accès réservé aux comptes autorisés.',
  'Données séparées par utilisateur.',
  'Paiement et abonnement gérés via Stripe.',
  'Interface pensée pour mobile et ordinateur.',
]
</script>
