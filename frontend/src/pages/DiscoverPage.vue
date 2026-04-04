<template>
  <div class="min-h-screen bg-slate-950 text-slate-100">
    <main class="mx-auto w-full max-w-5xl space-y-6 px-4 py-8 sm:space-y-8 sm:px-6 sm:py-10 lg:px-8 lg:py-12">
      <section class="rounded-2xl border border-slate-800 bg-slate-900/70 p-6 sm:p-8">
        <div class="flex flex-wrap items-center justify-between gap-3">
          <p class="text-xs uppercase tracking-[0.28em] text-slate-400">Stash</p>
          <router-link
            :to="{ name: 'auth', query: { mode: 'login' } }"
            class="inline-flex items-center rounded-lg border border-slate-700 px-3 py-1.5 text-xs font-medium text-slate-200 transition hover:border-slate-500 hover:text-white"
          >
            Retour a la connexion
          </router-link>
        </div>

        <div class="mt-5 grid gap-6 lg:grid-cols-[1.25fr_0.75fr] lg:items-start">
          <div>
            <h1 class="max-w-3xl text-3xl font-semibold leading-tight text-white sm:text-4xl">
              Gere ton stock simplement.
            </h1>
            <p class="mt-4 max-w-3xl text-sm text-slate-300 sm:text-base">
              Stash aide les revendeurs, petites structures et boutiques a suivre leurs articles,
              leurs entrees/sorties et leur stock disponible sans outil trop lourd.
            </p>
            <div class="mt-6 flex flex-wrap gap-3">
              <router-link
                :to="{ name: 'auth', query: { mode: 'signup' } }"
                class="inline-flex items-center justify-center rounded-lg bg-violet-500 px-4 py-2.5 text-sm font-semibold text-slate-950 transition hover:bg-violet-400"
              >
                Creer un compte
              </router-link>
              <router-link
                :to="{ name: 'auth', query: { mode: 'login' } }"
                class="inline-flex items-center justify-center rounded-lg border border-slate-600 px-4 py-2.5 text-sm font-semibold text-slate-100 transition hover:border-slate-400"
              >
                Se connecter
              </router-link>
            </div>
          </div>

          <aside class="rounded-xl border border-slate-800 bg-slate-950/65 p-4">
            <p class="text-xs font-semibold uppercase tracking-wide text-slate-400">Ce que tu gagnes</p>
            <ul class="mt-3 space-y-2 text-sm text-slate-300">
              <li v-for="item in quickWins" :key="item" class="flex items-start gap-2">
                <span class="mt-1 h-1.5 w-1.5 rounded-full bg-violet-400"></span>
                <span>{{ item }}</span>
              </li>
            </ul>
          </aside>
        </div>
      </section>

      <section class="rounded-2xl border border-slate-800 bg-slate-900/60 p-6 sm:p-8">
        <h2 class="text-xl font-semibold text-white sm:text-2xl">Le probleme</h2>
        <p class="mt-3 max-w-4xl text-sm text-slate-300 sm:text-base">
          Quand le stock est gere entre notes, fichiers Excel, messages et habitudes bricolees, on
          perd vite du temps et de la visibilite.
        </p>
        <div class="mt-4 grid gap-3 sm:grid-cols-3">
          <div
            v-for="pain in painPoints"
            :key="pain"
            class="rounded-xl border border-slate-800 bg-slate-950/60 p-3 text-sm text-slate-300"
          >
            {{ pain }}
          </div>
        </div>
      </section>

      <section class="rounded-2xl border border-slate-800 bg-slate-900/60 p-6 sm:p-8">
        <h2 class="text-xl font-semibold text-white sm:text-2xl">Ce que permet Stash</h2>
        <div class="mt-4 grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
          <article
            v-for="benefit in benefits"
            :key="benefit"
            class="rounded-xl border border-slate-800 bg-slate-950/60 p-4"
          >
            <p class="text-sm font-medium text-slate-100">{{ benefit }}</p>
          </article>
        </div>
      </section>

      <section class="rounded-2xl border border-slate-800 bg-slate-900/60 p-6 sm:p-8">
        <div class="flex flex-wrap items-center justify-between gap-3">
          <h2 class="text-xl font-semibold text-white sm:text-2xl">Demonstration video</h2>
          <p class="text-xs text-slate-400">
            Modifie juste
            <code class="rounded bg-slate-800 px-1.5 py-0.5 text-slate-200">videoCatalog</code>
            dans ce composant.
          </p>
        </div>

        <div class="mt-4 grid gap-4 lg:grid-cols-3">
          <article
            v-for="video in videos"
            :key="video.title"
            class="overflow-hidden rounded-xl border border-slate-800 bg-slate-950/60"
          >
            <div class="aspect-video border-b border-slate-800 bg-slate-950">
              <iframe
                v-if="video.embedUrl"
                class="h-full w-full"
                :src="video.embedUrl"
                :title="`Video: ${video.title}`"
                loading="lazy"
                allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share"
                allowfullscreen
              ></iframe>
              <div v-else class="flex h-full items-center justify-center px-4 text-center">
                <p class="text-sm text-slate-400">
                  Ajoute une URL YouTube dans
                  <code class="rounded bg-slate-800 px-1 py-0.5 text-slate-200">youtubeUrl</code>
                </p>
              </div>
            </div>
            <div class="p-4">
              <h3 class="text-sm font-semibold text-white sm:text-base">{{ video.title }}</h3>
              <p class="mt-1 text-sm text-slate-300">{{ video.description }}</p>
            </div>
          </article>
        </div>
      </section>

      <section class="rounded-2xl border border-slate-800 bg-slate-900/60 p-6 sm:p-8">
        <h2 class="text-xl font-semibold text-white sm:text-2xl">Pour qui c'est fait</h2>
        <div class="mt-4 grid gap-3 md:grid-cols-3">
          <article
            v-for="audience in audiences"
            :key="audience.title"
            class="rounded-xl border border-slate-800 bg-slate-950/60 p-4"
          >
            <h3 class="text-base font-semibold text-white">{{ audience.title }}</h3>
            <p class="mt-2 text-sm text-slate-300">{{ audience.description }}</p>
          </article>
        </div>
      </section>

      <section class="rounded-2xl border border-violet-400/25 bg-slate-900/75 p-6 sm:p-8">
        <h2 class="text-xl font-semibold text-white sm:text-2xl">
          Decouvre Stash et commence a organiser ton stock plus clairement.
        </h2>
        <div class="mt-5 flex flex-wrap gap-3">
          <router-link
            :to="{ name: 'auth', query: { mode: 'signup' } }"
            class="inline-flex items-center justify-center rounded-lg bg-violet-500 px-4 py-2.5 text-sm font-semibold text-slate-950 transition hover:bg-violet-400"
          >
            Creer un compte
          </router-link>
          <router-link
            :to="{ name: 'auth', query: { mode: 'login' } }"
            class="inline-flex items-center justify-center rounded-lg border border-slate-600 px-4 py-2.5 text-sm font-semibold text-slate-100 transition hover:border-slate-400"
          >
            Se connecter
          </router-link>
        </div>
      </section>
    </main>
  </div>
</template>

<script setup>
const quickWins = [
  'Une vision claire du stock disponible',
  'Moins d\'erreurs sur les entrees et sorties',
  'Un suivi plus simple au quotidien',
]

const painPoints = [
  'Infos dispersees',
  'Perte de temps',
  'Manque de visibilite',
]

const benefits = [
  'Ajoute tes articles rapidement',
  'Visualise ton stock disponible en un coup d\'oeil',
  'Suis les entrees et sorties plus simplement',
  'Retrouve facilement tes references',
  'Garde une organisation claire au quotidien',
  'Pilote ton stock avec plus de visibilite',
]

const audiences = [
  {
    title: 'Revendeurs independants',
    description: 'Centralise ton stock, tes mouvements et tes references en un seul endroit.',
  },
  {
    title: 'Petites equipes',
    description: 'Partage une base claire pour suivre les entrees/sorties sans process lourd.',
  },
  {
    title: 'Boutiques independantes',
    description: 'Garde une vision nette du disponible et reduis les erreurs de suivi.',
  },
]

const videoCatalog = [
  {
    title: 'Presentation rapide',
    description: 'Vue d\'ensemble de Stash',
    youtubeUrl: '',
  },
  {
    title: 'Ajouter un article',
    description: 'Comment enregistrer un nouvel article',
    youtubeUrl: '',
  },
  {
    title: 'Suivre le stock et les sorties',
    description: 'Comprendre les entrees, sorties et disponibilites',
    youtubeUrl: '',
  },
]

const extractYouTubeId = (url) => {
  if (!url) return ''

  try {
    const parsed = new URL(url)

    if (parsed.hostname.includes('youtu.be')) {
      return parsed.pathname.split('/').filter(Boolean)[0] || ''
    }

    if (parsed.hostname.includes('youtube.com')) {
      if (parsed.pathname === '/watch') {
        return parsed.searchParams.get('v') || ''
      }

      if (parsed.pathname.startsWith('/embed/') || parsed.pathname.startsWith('/shorts/')) {
        return parsed.pathname.split('/')[2] || ''
      }
    }
  } catch {
    return ''
  }

  return ''
}

const toEmbedUrl = (url) => {
  const videoId = extractYouTubeId(url)
  return videoId ? `https://www.youtube.com/embed/${videoId}` : ''
}

const videos = videoCatalog.map((video) => ({
  ...video,
  embedUrl: toEmbedUrl(video.youtubeUrl),
}))
</script>
