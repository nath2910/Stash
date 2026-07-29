<template>
  <section class="space-y-4" aria-label="Outils de diffusion">
    <div
      class="rounded-[24px] border border-slate-200 bg-[#fbfaf7] p-4 shadow-[0_12px_30px_rgba(15,23,42,0.055)] sm:p-5"
    >
      <div class="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
        <div class="min-w-0">
          <p class="text-[11px] font-semibold uppercase tracking-[0.18em] text-teal-700">
            Diffusion ventes
          </p>
          <h2 class="mt-2 text-xl font-semibold text-slate-900 sm:text-2xl">
            Discord et marketplaces
          </h2>
          <p class="mt-1 max-w-2xl text-sm text-slate-500">
            Un espace simple pour copier un message Discord et preparer les futurs templates.
          </p>
        </div>

        <div class="grid grid-cols-3 gap-2 sm:w-auto">
          <article
            class="rounded-[18px] border border-slate-200 bg-white px-3 py-2 shadow-[0_8px_20px_rgba(15,23,42,0.04)]"
          >
            <p class="text-[11px] font-semibold uppercase tracking-[0.12em] text-slate-400">Stock</p>
            <strong class="mt-1 block text-base font-semibold text-slate-900">
              {{ discordListings.length }}
            </strong>
          </article>
          <article
            class="rounded-[18px] border border-slate-200 bg-white px-3 py-2 shadow-[0_8px_20px_rgba(15,23,42,0.04)]"
          >
            <p class="text-[11px] font-semibold uppercase tracking-[0.12em] text-slate-400">Valeur</p>
            <strong class="mt-1 block text-base font-semibold text-slate-900">
              {{ estimatedSellValueLabel }}
            </strong>
          </article>
          <article
            class="rounded-[18px] border border-slate-200 bg-white px-3 py-2 shadow-[0_8px_20px_rgba(15,23,42,0.04)]"
          >
            <p class="text-[11px] font-semibold uppercase tracking-[0.12em] text-slate-400">Fiches</p>
            <strong class="mt-1 block text-base font-semibold text-slate-900">
              {{ sourceCoverageLabel }}
            </strong>
          </article>
        </div>
      </div>

      <div class="mt-4 inline-flex flex-wrap gap-2 rounded-full border border-slate-200 bg-white p-1">
        <button
          v-for="tab in tabs"
          :key="tab.id"
          type="button"
          class="inline-flex items-center gap-2 rounded-full px-4 py-2 text-sm font-semibold transition"
          :class="
            activeTab === tab.id
              ? 'bg-teal-700 text-white shadow-[0_10px_18px_rgba(15,118,110,0.18)]'
              : 'text-slate-600 hover:bg-slate-50 hover:text-slate-900'
          "
          :aria-selected="activeTab === tab.id"
          @click="activeTab = tab.id"
        >
          <component :is="tab.icon" class="h-4 w-4" aria-hidden="true" />
          <span>{{ tab.label }}</span>
        </button>
      </div>
    </div>

    <section v-if="activeTab === 'discord'" class="grid gap-4 xl:grid-cols-[minmax(0,1.45fr)_360px]">
      <article
        class="rounded-[24px] border border-slate-200 bg-[#fbfaf7] p-4 shadow-[0_12px_30px_rgba(15,23,42,0.055)] sm:p-5"
      >
        <div class="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
          <div>
            <p class="text-[11px] font-semibold uppercase tracking-[0.18em] text-slate-400">Discord</p>
            <h3 class="mt-2 text-lg font-semibold text-slate-900">Message pret a copier</h3>
            <p class="mt-1 text-sm text-slate-500">
              Le texte est genere depuis le stock disponible.
            </p>
          </div>

          <button
            type="button"
            class="inline-flex h-10 items-center justify-center gap-2 rounded-full border border-teal-600/20 bg-teal-700 px-4 text-sm font-semibold text-white transition hover:bg-teal-600"
            @click="copyDiscordMessage"
          >
            <component :is="copyButtonIcon" class="h-4 w-4" aria-hidden="true" />
            <span>{{ copyButtonLabel }}</span>
          </button>
        </div>

        <div class="mt-4 grid gap-2 sm:grid-cols-3">
          <div class="rounded-[18px] border border-slate-200 bg-white px-3 py-3">
            <p class="text-xs font-medium text-slate-500">Annonces</p>
            <p class="mt-1 text-lg font-semibold text-slate-900">{{ discordListings.length }}</p>
          </div>
          <div class="rounded-[18px] border border-slate-200 bg-white px-3 py-3">
            <p class="text-xs font-medium text-slate-500">Prix moyen</p>
            <p class="mt-1 text-lg font-semibold text-slate-900">{{ averageListingPriceLabel }}</p>
          </div>
          <div class="rounded-[18px] border border-slate-200 bg-white px-3 py-3">
            <p class="text-xs font-medium text-slate-500">Apercu</p>
            <p class="mt-1 text-lg font-semibold text-slate-900">{{ discordCharacterCount }} car.</p>
          </div>
        </div>

        <div class="mt-4 overflow-hidden rounded-[22px] border border-slate-900/10 bg-slate-950">
          <div
            class="flex items-center justify-between border-b border-white/8 px-4 py-3 text-xs font-medium text-slate-400"
          >
            <span>Salon #annonces</span>
            <span>Preview</span>
          </div>

          <pre
            class="max-h-[420px] overflow-auto px-4 py-4 font-mono text-[13px] leading-7 text-slate-100 whitespace-pre-wrap break-words"
            aria-label="Message Discord genere"
          >{{ previewMessage }}</pre>
        </div>
      </article>

      <aside class="space-y-4">
        <article
          class="rounded-[24px] border border-slate-200 bg-[#fbfaf7] p-4 shadow-[0_12px_30px_rgba(15,23,42,0.055)] sm:p-5"
        >
          <div class="flex items-center justify-between gap-3">
            <div>
              <p class="text-[11px] font-semibold uppercase tracking-[0.18em] text-slate-400">Fiche source</p>
              <h3 class="mt-2 text-lg font-semibold text-slate-900">URL produit</h3>
            </div>
            <span
              class="inline-flex rounded-full border border-slate-200 bg-white px-3 py-1 text-xs font-semibold text-slate-600"
            >
              {{ sourceCoverageLabel }}
            </span>
          </div>

          <p class="mt-3 text-sm text-slate-500">
            {{ sourceHelpText }}
          </p>

          <div class="mt-4 rounded-[20px] border border-slate-200 bg-white p-4">
            <p class="text-xs font-semibold uppercase tracking-[0.14em] text-slate-400">Ouverture rapide</p>
            <p class="mt-2 text-sm text-slate-600">
              {{ primarySourceUrl ? "Ouvre une fiche deja renseignee dans le stock." : "Aucune URL produit enregistree pour le moment." }}
            </p>

            <a
              v-if="primarySourceUrl"
              class="mt-4 inline-flex items-center gap-2 rounded-full border border-teal-600/20 bg-teal-700 px-4 py-2 text-sm font-semibold text-white transition hover:bg-teal-600"
              :href="primarySourceUrl"
              target="_blank"
              rel="noreferrer"
            >
              <span>Ouvrir la fiche</span>
              <ExternalLink class="h-3.5 w-3.5" aria-hidden="true" />
            </a>
          </div>

          <div v-if="primarySourceLabel" class="mt-3 text-sm text-slate-500">
            {{ primarySourceLabel }}
          </div>
        </article>
      </aside>
    </section>

    <section v-else>
      <article
        class="rounded-[24px] border border-slate-200 bg-[#fbfaf7] p-4 shadow-[0_12px_30px_rgba(15,23,42,0.055)] sm:p-5"
      >
        <div class="flex items-center justify-between gap-3">
          <div>
            <p class="text-[11px] font-semibold uppercase tracking-[0.18em] text-slate-400">Template</p>
            <h3 class="mt-2 text-lg font-semibold text-slate-900">{{ activeTabData.label }}</h3>
          </div>
          <span
            class="inline-flex rounded-full border border-slate-200 bg-white px-3 py-1 text-xs font-semibold text-slate-600"
          >
            Bientot
          </span>
        </div>

        <p class="mt-3 max-w-2xl text-sm text-slate-500">{{ activeTabData.description }}</p>
      </article>
    </section>
  </section>
</template>

<script setup>
import { computed, onBeforeUnmount, ref } from 'vue'
import {
  Check,
  Copy,
  ExternalLink,
  Globe2,
  MessageSquareQuote,
  ShoppingBag,
  Store,
  TriangleAlert,
} from 'lucide-vue-next'
import { formatEUR } from '@/utils/formatters'
import {
  buildDiscordStockListings,
  buildDiscordStockMessage,
} from '@/utils/marketplaceMessageBuilder'

const props = defineProps({
  items: {
    type: Array,
    default: () => [],
  },
})

const tabs = [
  {
    id: 'discord',
    label: 'Discord',
    icon: MessageSquareQuote,
    description: 'Message de vente copiable depuis le stock.',
  },
  {
    id: 'vinted',
    label: 'Vinted',
    icon: ShoppingBag,
    description: 'Template compact centre sur le titre, le prix et les photos.',
  },
  {
    id: 'leboncoin',
    label: 'Le Bon Coin',
    icon: Store,
    description: "Template plus descriptif pour une annonce generaliste.",
  },
  {
    id: 'ebay',
    label: 'eBay',
    icon: Globe2,
    description: 'Template structure autour de la reference produit.',
  },
]

const activeTab = ref('discord')
const copyState = ref('idle')
let copyFeedbackTimer = null

const discordListings = computed(() => buildDiscordStockListings(props.items))
const discordMessage = computed(() => buildDiscordStockMessage(props.items))
const estimatedSellValue = computed(() =>
  discordListings.value.reduce((sum, listing) => sum + (listing.price ?? 0), 0),
)
const estimatedSellValueLabel = computed(() => formatEUR(estimatedSellValue.value, { digits: 0 }))
const averageListingPriceLabel = computed(() => {
  if (!discordListings.value.length) return 'Aucun prix'
  return formatEUR(estimatedSellValue.value / discordListings.value.length, { digits: 0 })
})
const sourcedListings = computed(() =>
  discordListings.value.filter((listing) => String(listing.marketUrl || '').trim()),
)
const sourceCount = computed(() => sourcedListings.value.length)
const sourceCoverageLabel = computed(() =>
  discordListings.value.length ? `${sourceCount.value}/${discordListings.value.length}` : '0/0',
)
const discordCharacterCount = computed(() => discordMessage.value.length)
const previewMessage = computed(() => discordMessage.value)
const activeTabData = computed(() => tabs.find((tab) => tab.id === activeTab.value) || tabs[0])
const primarySourceListing = computed(() => sourcedListings.value[0] ?? null)
const primarySourceUrl = computed(() => primarySourceListing.value?.marketUrl || '')
const primarySourceLabel = computed(() => {
  const listing = primarySourceListing.value
  if (!listing) return ''
  const parts = [listing.name, listing.reference].filter(Boolean)
  return parts.join(' · ')
})
const sourceHelpText = computed(() => {
  if (sourceCount.value) {
    return `${sourceCount.value} item(s) ont deja une URL produit enregistree.`
  }
  return "Ajoute une URL Amazon, eBay ou autre fiche directement dans la fiche item."
})
const copyButtonIcon = computed(() => {
  if (copyState.value === 'success') return Check
  if (copyState.value === 'error') return TriangleAlert
  return Copy
})
const copyButtonLabel = computed(() => {
  if (copyState.value === 'success') return 'Copie'
  if (copyState.value === 'error') return 'Erreur'
  return 'Copier'
})

function clearCopyFeedbackTimer() {
  if (copyFeedbackTimer) {
    window.clearTimeout(copyFeedbackTimer)
    copyFeedbackTimer = null
  }
}

async function copyDiscordMessage() {
  clearCopyFeedbackTimer()

  try {
    if (navigator?.clipboard?.writeText) {
      await navigator.clipboard.writeText(discordMessage.value)
    } else {
      const textarea = document.createElement('textarea')
      textarea.value = discordMessage.value
      textarea.setAttribute('readonly', '')
      textarea.style.position = 'absolute'
      textarea.style.left = '-9999px'
      document.body.appendChild(textarea)
      textarea.select()
      document.execCommand('copy')
      document.body.removeChild(textarea)
    }
    copyState.value = 'success'
  } catch {
    copyState.value = 'error'
  }

  copyFeedbackTimer = window.setTimeout(() => {
    copyState.value = 'idle'
    copyFeedbackTimer = null
  }, 2200)
}

onBeforeUnmount(() => {
  clearCopyFeedbackTimer()
})
</script>
