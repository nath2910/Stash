<template>
  <section
    class="min-w-0 rounded-[24px] border border-slate-200 bg-[#fbfaf7] p-4 shadow-[0_12px_30px_rgba(15,23,42,0.055)]"
  >
    <div class="flex flex-wrap items-center justify-between gap-3">
      <div>
        <h2 class="text-base font-semibold text-slate-900">Import Gmail</h2>
        <p class="mt-1 text-xs text-slate-500">
          {{ accounts.length ? '1 compte Gmail connecte.' : 'Aucun compte Gmail connecte.' }} Le scan relit uniquement les emails Colissimo des 15 derniers jours.
        </p>
      </div>
      <button
        v-if="accounts.length"
        type="button"
        class="inline-flex h-8 w-full items-center justify-center gap-2 rounded-full border border-slate-200 bg-white px-3 text-xs font-semibold text-slate-700 transition hover:border-sky-300 hover:text-slate-900 disabled:cursor-wait disabled:opacity-50 sm:w-auto"
        :disabled="loading || scanningAll"
        @click="$emit('scan-all')"
      >
        <RefreshCw class="h-3.5 w-3.5" :class="{ 'animate-spin': scanningAll }" />
        <span>Rechercher des suivis</span>
      </button>
    </div>

    <form class="mt-4 grid gap-2" @submit.prevent="connectWithEmail">
      <p class="text-xs text-slate-500">
        Un seul Gmail est gere ici. La connexion Google sert uniquement a lire les emails Colissimo recents.
      </p>

      <button
        type="button"
        class="inline-flex h-10 w-full items-center justify-center gap-2 rounded-full border border-teal-600/20 bg-teal-700 px-4 text-sm font-semibold text-white transition hover:bg-teal-600 disabled:cursor-wait disabled:opacity-60 sm:w-auto"
        :disabled="connecting"
        @click="connectWithEmail"
      >
        <LinkIcon class="h-4 w-4" />
        <span>{{
          connecting ? 'Connexion...' : accounts.length ? 'Reconnecter Gmail' : 'Connecter Gmail'
        }}</span>
      </button>

      <button
        type="button"
        class="justify-self-center text-xs font-medium text-slate-500 transition hover:text-slate-700"
        @click="showEmailHint = !showEmailHint"
      >
        {{ showEmailHint ? 'Masquer l adresse' : 'Preciser une adresse' }}
      </button>

      <Transition
        enter-active-class="transition duration-150 ease-out"
        enter-from-class="opacity-0 -translate-y-1"
        enter-to-class="opacity-100 translate-y-0"
        leave-active-class="transition duration-100 ease-in"
        leave-from-class="opacity-100 translate-y-0"
        leave-to-class="opacity-0 -translate-y-1"
      >
        <input
          v-if="showEmailHint"
          id="delivery-gmail-email"
          v-model.trim="emailAddress"
          type="email"
          inputmode="email"
          autocomplete="email"
          class="h-10 min-w-0 flex-1 rounded-full border border-slate-200 bg-white px-4 text-sm text-slate-900 outline-none transition placeholder:text-slate-400 focus:border-sky-300 focus:ring-2 focus:ring-sky-100"
          placeholder="adresse@gmail.com"
        />
      </Transition>
    </form>

    <div
      v-if="error"
      class="mt-4 rounded-2xl border border-red-300/40 bg-red-50 px-3 py-2.5 text-sm text-red-700"
    >
      {{ error }}
    </div>

    <div v-if="loading" class="mt-5 grid gap-3">
      <div
        v-for="index in 2"
        :key="index"
        class="h-16 animate-pulse rounded-2xl bg-slate-200/70"
      />
    </div>

    <div
      v-else-if="!accounts.length"
      class="mt-5 rounded-2xl border border-dashed border-slate-300 bg-white/70 p-5 text-center"
    >
      <Mail class="mx-auto h-7 w-7 text-slate-400" />
      <p class="mt-3 text-sm font-medium text-slate-800">Aucun compte lie</p>
      <p class="mt-2 text-xs leading-relaxed text-slate-500">
        Connecte ton Gmail Google pour scanner uniquement les emails Colissimo des 15 derniers jours.
      </p>
    </div>

    <div
      v-else
      class="delivery-accounts-scroll mt-5 grid max-h-none gap-2.5 overflow-visible pr-0 lg:max-h-[340px] lg:overflow-y-auto lg:pr-1"
    >
      <article
        v-for="account in accounts"
        :key="account.id"
        class="rounded-2xl border border-slate-200 bg-white p-3"
      >
        <div class="flex flex-col items-start gap-3 min-[420px]:flex-row min-[420px]:items-center min-[420px]:justify-between">
          <div class="min-w-0">
            <p class="truncate text-sm font-semibold text-slate-900">{{ account.emailAddress }}</p>
            <p class="mt-1 text-xs text-slate-500">
              {{ accountHelperText(account) }}
            </p>
          </div>

          <div class="flex shrink-0 items-center gap-2 self-stretch min-[420px]:self-auto">
            <button
              type="button"
              class="inline-flex h-8 flex-1 items-center justify-center rounded-full border border-slate-200 bg-slate-50 text-slate-700 transition hover:border-sky-300 hover:text-slate-900 disabled:cursor-wait disabled:opacity-50 min-[420px]:h-8 min-[420px]:w-8 min-[420px]:flex-none"
              :disabled="scanningAll || scanningId === account.id"
              title="Scanner maintenant"
              @click="$emit('scan-now', account.id)"
            >
              <RefreshCw
                class="h-3.5 w-3.5"
                :class="{ 'animate-spin': scanningAll || scanningId === account.id }"
              />
            </button>
            <button
              type="button"
              class="inline-flex h-8 flex-1 items-center justify-center rounded-full border border-red-300/40 bg-red-50 text-red-700 transition hover:border-red-400 hover:bg-red-100 min-[420px]:h-8 min-[420px]:w-8 min-[420px]:flex-none"
              title="Supprimer"
              @click="$emit('delete-account', account.id)"
            >
              <Trash2 class="h-3.5 w-3.5" />
            </button>
          </div>
        </div>
      </article>
    </div>
  </section>
</template>

<script setup>
import { ref } from 'vue'
import { LinkIcon, Mail, RefreshCw, Trash2 } from 'lucide-vue-next'

defineProps({
  accounts: {
    type: Array,
    default: () => [],
  },
  loading: {
    type: Boolean,
    default: false,
  },
  error: {
    type: String,
    default: '',
  },
  connecting: {
    type: Boolean,
    default: false,
  },
  scanningId: {
    type: [Number, String],
    default: null,
  },
  scanningAll: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits(['connect-gmail', 'scan-now', 'scan-all', 'delete-account'])

const emailAddress = ref('')
const showEmailHint = ref(false)

const connectWithEmail = () => {
  emit('connect-gmail', emailAddress.value)
}

const accountHelperText = (account) => {
  const lastScan = formatDateTime(account?.lastScanAt)
  switch (account?.status) {
    case 'ERROR':
    case 'REVOKED':
      return `Connexion a verifier. Dernier scan ${lastScan}.`
    case 'DISABLED':
      return `Source en pause. Dernier scan ${lastScan}.`
    default:
      return `Dernier scan ${lastScan}.`
  }
}

const formatDateTime = (value) => {
  if (!value) return 'jamais'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return 'jamais'
  return new Intl.DateTimeFormat('fr-FR', {
    dateStyle: 'short',
    timeStyle: 'short',
  }).format(date)
}
</script>

<style scoped>
.delivery-accounts-scroll {
  scrollbar-width: thin;
  scrollbar-color: rgba(148, 163, 184, 0.9) rgba(226, 232, 240, 0.9);
}

.delivery-accounts-scroll::-webkit-scrollbar {
  width: 8px;
}

.delivery-accounts-scroll::-webkit-scrollbar-track {
  background: rgba(226, 232, 240, 0.9);
  border-radius: 999px;
}

.delivery-accounts-scroll::-webkit-scrollbar-thumb {
  border: 2px solid rgba(226, 232, 240, 0.9);
  border-radius: 999px;
  background: linear-gradient(180deg, rgba(15, 118, 110, 0.72), rgba(56, 189, 248, 0.56));
}
</style>
