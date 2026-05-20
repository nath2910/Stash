<template>
  <section
    class="rounded-[22px] border border-slate-700/70 bg-slate-900/70 p-4 shadow-xl shadow-slate-950/20 backdrop-blur"
  >
    <div class="flex flex-wrap items-center justify-between gap-3">
      <div>
        <h2 class="text-base font-semibold text-white">Sources mail</h2>
        <p class="mt-1 text-xs text-slate-400">{{ accounts.length }} compte(s)</p>
      </div>
      <button
        v-if="accounts.length"
        type="button"
        class="inline-flex h-8 items-center justify-center gap-2 rounded-full border border-slate-700/80 bg-slate-950/35 px-3 text-xs font-semibold text-slate-200 transition hover:border-violet-400/60 hover:text-white disabled:cursor-wait disabled:opacity-50"
        :disabled="loading || scanningAll"
        @click="$emit('scan-all')"
      >
        <RefreshCw class="h-3.5 w-3.5" :class="{ 'animate-spin': scanningAll }" />
        <span>Scanner</span>
      </button>
    </div>

    <form class="mt-4 grid gap-2" @submit.prevent="connectWithEmail">
      <button
        type="button"
        class="inline-flex h-10 items-center justify-center gap-2 rounded-full border border-violet-400/50 bg-violet-500/10 px-4 text-sm font-semibold text-violet-100 transition hover:border-violet-300/70 hover:bg-violet-500/20 disabled:cursor-wait disabled:opacity-60"
        :disabled="connecting"
        @click="connectWithEmail"
      >
        <LinkIcon class="h-4 w-4" />
        <span>{{
          connecting ? 'Connexion...' : accounts.length ? 'Lier un autre Gmail' : 'Connecter Gmail'
        }}</span>
      </button>

      <button
        type="button"
        class="justify-self-center text-xs font-medium text-slate-500 transition hover:text-slate-300"
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
          class="h-10 min-w-0 flex-1 rounded-full border border-slate-700/80 bg-slate-950/45 px-4 text-sm text-slate-100 outline-none transition placeholder:text-slate-600 focus:border-violet-400/70 focus:ring-2 focus:ring-violet-500/20"
          placeholder="adresse@gmail.com"
        />
      </Transition>
    </form>

    <div
      v-if="error"
      class="mt-4 rounded-2xl border border-red-400/30 bg-red-500/10 px-3 py-2.5 text-sm text-red-100"
    >
      {{ error }}
    </div>

    <div v-if="loading" class="mt-5 grid gap-3">
      <div v-for="index in 2" :key="index" class="h-16 animate-pulse rounded-2xl bg-slate-800/55" />
    </div>

    <div
      v-else-if="!accounts.length"
      class="mt-5 rounded-2xl border border-dashed border-slate-700/80 p-5 text-center"
    >
      <Mail class="mx-auto h-7 w-7 text-slate-500" />
      <p class="mt-3 text-sm font-medium text-slate-300">Aucun compte lie</p>
    </div>

    <div
      v-else
      class="delivery-accounts-scroll mt-5 grid max-h-[340px] gap-2.5 overflow-y-auto pr-1"
    >
      <article
        v-for="account in accounts"
        :key="account.id"
        class="rounded-2xl border border-slate-700/70 bg-slate-950/35 p-3"
      >
        <div class="flex items-center justify-between gap-3">
          <div class="min-w-0">
            <div class="flex min-w-0 items-center gap-2">
              <p class="truncate text-sm font-semibold text-white">{{ account.emailAddress }}</p>
              <span
                class="rounded-full border px-2.5 py-1 text-[11px] font-semibold"
                :class="statusClass(account.status)"
              >
                {{ statusLabel(account.status) }}
              </span>
            </div>
            <p class="mt-1 text-xs text-slate-500">
              {{ providerLabel(account.provider) }} - Scan {{ formatDateTime(account.lastScanAt) }}
            </p>
          </div>

          <div class="flex shrink-0 items-center gap-2">
            <button
              type="button"
              class="inline-flex h-8 w-8 items-center justify-center rounded-full border border-slate-700/80 bg-slate-900/75 text-slate-200 transition hover:border-violet-400/60 hover:text-white disabled:cursor-wait disabled:opacity-50"
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
              class="inline-flex h-8 w-8 items-center justify-center rounded-full border border-red-500/25 bg-red-500/10 text-red-200 transition hover:border-red-400/80 hover:bg-red-500/20"
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

const statusLabel = (status) => {
  switch (status) {
    case 'ACTIVE':
      return 'Actif'
    case 'ERROR':
      return 'Erreur'
    case 'REVOKED':
      return 'Revoque'
    case 'DISABLED':
      return 'Desactive'
    default:
      return 'Inconnu'
  }
}

const statusClass = (status) => {
  switch (status) {
    case 'ACTIVE':
      return 'border-emerald-400/40 bg-emerald-500/10 text-emerald-200'
    case 'ERROR':
    case 'REVOKED':
      return 'border-red-400/40 bg-red-500/10 text-red-200'
    default:
      return 'border-slate-600 bg-slate-900 text-slate-300'
  }
}

const providerLabel = (provider) => {
  if (!provider) return 'Mail'
  return String(provider)
    .toLowerCase()
    .split('_')
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(' ')
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
  scrollbar-color: rgba(139, 92, 246, 0.55) rgba(15, 23, 42, 0.35);
}

.delivery-accounts-scroll::-webkit-scrollbar {
  width: 8px;
}

.delivery-accounts-scroll::-webkit-scrollbar-track {
  background: rgba(15, 23, 42, 0.35);
  border-radius: 999px;
}

.delivery-accounts-scroll::-webkit-scrollbar-thumb {
  border: 2px solid rgba(15, 23, 42, 0.35);
  border-radius: 999px;
  background: linear-gradient(180deg, rgba(167, 139, 250, 0.78), rgba(14, 165, 233, 0.62));
}
</style>
