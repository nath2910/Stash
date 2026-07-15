<template>
  <section
    :class="
      embedded
        ? 'min-w-0'
        : 'min-w-0 rounded-[24px] border border-slate-200 bg-[#fbfaf7] p-4 shadow-[0_12px_30px_rgba(15,23,42,0.055)]'
    "
  >
    <div class="flex items-center justify-between gap-3">
      <div>
        <h2 class="text-base font-semibold text-slate-900">Suivi manuel</h2>
        <p class="mt-1 text-xs text-slate-500">Un ou plusieurs numeros de suivi</p>
      </div>
      <span
        class="inline-flex h-9 w-9 items-center justify-center rounded-full border border-teal-600/20 bg-teal-50 text-teal-700"
      >
        <PackagePlus class="h-4 w-4" />
      </span>
    </div>

    <form class="mt-4 grid gap-3" @submit.prevent="submit">
      <label class="grid gap-2">
        <span class="text-xs font-semibold uppercase tracking-[0.16em] text-slate-500"
          >Tracking</span
        >
        <textarea
          ref="trackingTextarea"
          v-model.trim="trackingInput"
          rows="3"
          autocomplete="off"
          spellcheck="false"
          class="min-h-[96px] w-full min-w-0 rounded-[20px] border border-slate-200 bg-white px-4 py-3 text-sm uppercase text-slate-900 outline-none transition placeholder:text-slate-400 focus:border-sky-300 focus:ring-2 focus:ring-sky-100"
          placeholder="1Z999AA10123456784&#10;LA123456789FR&#10;ou separes par des virgules"
        ></textarea>
        <p class="text-[11px] leading-relaxed text-slate-500">
          Collez une ou plusieurs references. Separez-les par une ligne, une virgule ou un point-virgule.
        </p>
      </label>

      <label class="grid gap-2">
        <span class="text-xs font-semibold uppercase tracking-[0.16em] text-slate-500"
          >Transporteur</span
        >
        <select
          v-model="carrierSlug"
          class="h-10 w-full min-w-0 rounded-full border border-slate-200 bg-white px-4 text-sm text-slate-900 outline-none transition focus:border-sky-300 focus:ring-2 focus:ring-sky-100"
        >
          <option value="">Detection auto</option>
          <option value="colissimo">Colissimo / La Poste</option>
          <option value="chronopost">Chronopost</option>
          <option value="mondial-relay">Mondial Relay</option>
        </select>
        <p class="text-[11px] leading-relaxed text-slate-500">
          Suivi live gratuit branche ici: Colissimo / La Poste, Chronopost et Mondial Relay. Les autres transporteurs ne sont pas proposes tant qu'aucun connecteur fiable n'est actif.
        </p>
      </label>

      <label v-if="carrierSlug === 'mondial-relay'" class="grid gap-2">
        <span class="text-xs font-semibold uppercase tracking-[0.16em] text-slate-500"
          >Code postal</span
        >
        <input
          v-model.trim="postalCode"
          inputmode="numeric"
          maxlength="5"
          autocomplete="postal-code"
          class="h-10 w-full min-w-0 rounded-full border border-slate-200 bg-white px-4 text-sm text-slate-900 outline-none transition placeholder:text-slate-400 focus:border-sky-300 focus:ring-2 focus:ring-sky-100"
          placeholder="17000"
        />
        <p class="text-[11px] leading-relaxed text-slate-500">
          Optionnel a l'ajout. Sans ce code postal, le suivi Mondial Relay sera cree en mode a completer.
        </p>
      </label>

      <button
        type="submit"
        class="inline-flex h-10 items-center justify-center gap-2 rounded-full border border-teal-600/20 bg-teal-700 px-4 text-sm font-semibold text-white transition hover:bg-teal-600 disabled:cursor-wait disabled:opacity-60"
        :disabled="loading || !trackingInput"
      >
        <RefreshCw v-if="loading" class="h-4 w-4 animate-spin" />
        <PackagePlus v-else class="h-4 w-4" />
        <span>{{ loading ? 'Recherche...' : 'Ajouter au suivi' }}</span>
      </button>
    </form>

    <div
      v-if="error"
      class="mt-4 rounded-2xl border border-red-300/40 bg-red-50 px-3 py-2.5 text-sm text-red-700"
    >
      {{ error }}
    </div>
  </section>
</template>

<script setup>
import { ref, watch } from 'vue'
import { PackagePlus, RefreshCw } from 'lucide-vue-next'

const props = defineProps({
  loading: {
    type: Boolean,
    default: false,
  },
  embedded: {
    type: Boolean,
    default: false,
  },
  error: {
    type: String,
    default: '',
  },
  successToken: {
    type: Number,
    default: 0,
  },
})

const emit = defineEmits(['create-parcel'])

const trackingInput = ref('')
const carrierSlug = ref('')
const postalCode = ref('')
const trackingTextarea = ref(null)

const submit = () => {
  if (!trackingInput.value) return
  emit('create-parcel', {
    trackingInput: trackingInput.value,
    carrierSlug: carrierSlug.value || null,
    postalCode: carrierSlug.value === 'mondial-relay' ? postalCode.value || null : null,
  })
}

watch(
  () => props.successToken,
  () => {
    trackingInput.value = ''
    carrierSlug.value = ''
    postalCode.value = ''
  },
)

watch(carrierSlug, (value) => {
  if (value !== 'mondial-relay') {
    postalCode.value = ''
  }
})

const focusFirstField = () => {
  trackingTextarea.value?.focus()
}

defineExpose({
  focusFirstField,
})
</script>
