<template>
  <section class="delivery-tracking-panel min-w-0 space-y-4">
    <Transition
      enter-active-class="transition duration-200 ease-out"
      enter-from-class="opacity-0 -translate-y-1"
      enter-to-class="opacity-100 translate-y-0"
      leave-active-class="transition duration-150 ease-in"
      leave-from-class="opacity-100 translate-y-0"
      leave-to-class="opacity-0 -translate-y-1"
    >
      <div
        v-if="callbackNotice"
        class="rounded-2xl border px-4 py-3 text-sm"
        :class="
          callbackNotice.kind === 'success'
            ? 'border-emerald-300/40 bg-emerald-50 text-emerald-800'
            : 'border-red-300/40 bg-red-50 text-red-700'
        "
      >
        {{ callbackNotice.message }}
      </div>
    </Transition>

    <Transition
      enter-active-class="transition duration-200 ease-out"
      enter-from-class="opacity-0 -translate-y-1"
      enter-to-class="opacity-100 translate-y-0"
      leave-active-class="transition duration-150 ease-in"
      leave-from-class="opacity-100 translate-y-0"
      leave-to-class="opacity-0 -translate-y-1"
    >
      <div
        v-if="scanNotice"
        class="rounded-2xl border px-4 py-3 text-sm"
        :class="
          scanNotice.kind === 'success'
            ? 'border-emerald-300/40 bg-emerald-50 text-emerald-800'
            : scanNotice.kind === 'warning'
              ? 'border-amber-300/40 bg-amber-50 text-amber-800'
              : 'border-slate-200 bg-white text-slate-700'
        "
      >
        {{ scanNotice.message }}
      </div>
    </Transition>

    <div class="min-w-0 space-y-4">
      <Transition
        enter-active-class="transition duration-200 ease-out"
        enter-from-class="opacity-0"
        enter-to-class="opacity-100"
        leave-active-class="transition duration-150 ease-in"
        leave-from-class="opacity-100"
        leave-to-class="opacity-0"
      >
        <div
          v-if="manualModalOpen"
          class="fixed inset-0 z-40 flex items-center justify-center bg-slate-950/45 p-4 backdrop-blur-sm"
          @click.self="closeManualModal"
        >
          <div
            class="w-full max-w-xl rounded-[28px] border border-slate-200 bg-[#fbfaf7] p-5 shadow-[0_24px_70px_rgba(15,23,42,0.20)] sm:p-6"
          >
            <div class="flex items-start justify-between gap-4">
              <div>
                <p class="text-lg font-semibold text-slate-900">Ajouter un suivi</p>
                <p class="mt-1 text-sm text-slate-500">
                  Colle un ou plusieurs numeros puis laisse l'app les integrer au tableau.
                </p>
              </div>
              <button
                type="button"
                class="inline-flex h-10 w-10 items-center justify-center rounded-full border border-slate-200 bg-white text-slate-500 transition hover:border-sky-300 hover:text-slate-900"
                aria-label="Fermer la fenetre d'ajout manuel"
                @click="closeManualModal"
              >
                <X class="h-4 w-4" />
              </button>
            </div>

            <div class="mt-5 rounded-[24px] border border-slate-200 bg-white p-4 sm:p-5">
              <DeliveryManualParcelForm
                ref="manualFormRef"
                embedded
                :loading="creatingManualParcel"
                :error="manualParcelError"
                :success-token="manualSuccessToken"
                @create-parcel="createManualParcel"
              />
            </div>
          </div>
        </div>
      </Transition>

      <Transition
        enter-active-class="transition duration-200 ease-out"
        enter-from-class="opacity-0 -translate-y-1"
        enter-to-class="opacity-100 translate-y-0"
        leave-active-class="transition duration-150 ease-in"
        leave-from-class="opacity-100 translate-y-0"
        leave-to-class="opacity-0 -translate-y-1"
      >
        <section
          v-if="activityState"
          class="rounded-[24px] border border-sky-200 bg-sky-50 px-4 py-4 shadow-[0_10px_24px_rgba(56,189,248,0.10)]"
        >
          <div class="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
            <div>
              <p class="text-sm font-semibold text-sky-950">{{ activityState.title }}</p>
              <p class="mt-1 text-xs text-sky-800">{{ activityState.detail }}</p>
            </div>
            <span class="inline-flex items-center rounded-full bg-white px-3 py-1 text-[11px] font-semibold text-sky-800">
              {{ activityState.badge }}
            </span>
          </div>
          <div class="mt-3 h-1.5 overflow-hidden rounded-full bg-sky-100">
            <div class="delivery-panel-loader h-full rounded-full bg-sky-500" />
          </div>
        </section>
      </Transition>

      <section
        class="rounded-[24px] border border-slate-200 bg-[#fbfaf7] p-4 shadow-[0_12px_30px_rgba(15,23,42,0.055)] sm:p-5"
      >
        <div class="flex flex-col gap-3 lg:flex-row lg:items-start lg:justify-between">
          <div>
            <h2 class="text-xl font-semibold text-slate-900 sm:text-2xl">Suivi livraison</h2>
            <p class="mt-1 text-sm text-slate-600">
              {{ automationLabel }}
            </p>
            <p class="mt-2 text-xs text-slate-500">
              {{ lastSyncLabel }}
            </p>
          </div>
          <div class="flex flex-wrap items-center gap-2">
            <button
              v-if="hasMailAccounts"
              type="button"
              class="inline-flex h-9 items-center justify-center gap-2 rounded-full border border-teal-600/20 bg-teal-700 px-3 text-xs font-semibold text-white transition hover:bg-teal-600 disabled:cursor-wait disabled:opacity-60"
              :disabled="scanningAll || accountsLoading || parcelsLoading"
              title="Analyse les mails Gmail et importe les nouveaux numeros de suivi"
              @click="scanAllNow"
            >
              <RefreshCw class="h-4 w-4" :class="{ 'animate-spin': scanningAll }" />
              <span>{{ scanningAll ? 'Scan...' : 'Scanner mails' }}</span>
            </button>
            <button
              type="button"
              class="inline-flex h-9 items-center justify-center gap-2 rounded-full border border-slate-200 bg-white px-3 text-xs font-semibold text-slate-700 transition hover:border-sky-300 hover:bg-slate-50 disabled:cursor-wait disabled:opacity-60"
              :disabled="scanningAll || accountsLoading || parcelsLoading"
              title="Recharge la liste sans scanner les mails"
              @click="refreshAll"
            >
              <RefreshCw
                class="h-4 w-4"
                :class="{ 'animate-spin': !scanningAll && (accountsLoading || parcelsLoading) }"
              />
              <span>Rafraichir liste</span>
            </button>
          </div>
        </div>

        <div class="mt-4 flex flex-wrap gap-2">
          <span
            v-for="metric in metrics"
            :key="metric.label"
            class="inline-flex items-center gap-2 rounded-full border border-slate-200 bg-white px-3 py-1.5 text-xs text-slate-600"
          >
            <component :is="metric.icon" class="h-3.5 w-3.5" :class="metric.iconClass" />
            <span>{{ metric.label }}</span>
            <span class="font-semibold text-slate-900">{{ metric.value }}</span>
          </span>
        </div>

        <div class="mt-4 grid gap-2 lg:grid-cols-3">
          <button
            type="button"
            class="flex items-center justify-between rounded-[20px] border border-slate-200 bg-white px-4 py-3 text-left transition hover:border-sky-300 hover:bg-slate-50"
            @click="openManualModal"
          >
            <div>
              <p class="text-sm font-semibold text-slate-900">Ajouter un suivi</p>
              <p class="mt-1 text-xs text-slate-500">Ajout manuel rapide</p>
            </div>
            <PencilLine class="h-4 w-4 text-slate-400" />
          </button>
          <button
            type="button"
            class="flex items-center justify-between rounded-[20px] border border-teal-600/20 bg-teal-50 px-4 py-3 text-left transition hover:border-teal-500/30 hover:bg-teal-100/70"
            @click="handlePrimaryAction"
          >
            <div>
              <p class="text-sm font-semibold text-teal-900">{{ primaryActionLabel }}</p>
              <p class="mt-1 text-xs text-teal-700">
                {{ hasMailAccounts ? 'Import auto depuis Gmail' : 'Lier un compte pour scanner' }}
              </p>
            </div>
            <MailPlus class="h-4 w-4 text-teal-700" />
          </button>
          <button
            type="button"
            class="flex items-center justify-between rounded-[20px] border border-slate-200 bg-white px-4 py-3 text-left transition hover:border-sky-300 hover:bg-slate-50"
            @click="selectionMode ? clearParcelSelection() : startParcelSelection()"
          >
            <div>
              <p class="text-sm font-semibold text-slate-900">Selection multiple</p>
              <p class="mt-1 text-xs text-slate-500">
                {{ selectionMode ? `${selectedParcelIds.length} colis coches` : 'Supprimer en lot' }}
              </p>
            </div>
            <Layers3 class="h-4 w-4 text-slate-400" />
          </button>
        </div>

        <div class="mt-4 flex flex-col gap-3 lg:flex-row lg:items-center">
          <label class="relative min-w-0 flex-1">
            <Search
              class="pointer-events-none absolute left-4 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-500"
            />
            <input
              ref="searchInput"
              v-model="searchTerm"
              type="search"
              class="h-10 w-full rounded-full border border-slate-200 bg-white pl-10 pr-4 text-sm text-slate-900 outline-none transition placeholder:text-slate-400 focus:border-sky-300 focus:ring-2 focus:ring-sky-100"
              placeholder="Tracking, transporteur, statut (/)"
            />
          </label>
          <div class="flex w-full flex-wrap gap-2 lg:w-auto">
            <button
              v-for="quick in quickFilters"
              :key="quick.value"
              type="button"
              class="flex-1 rounded-full border px-3 py-1.5 text-xs font-semibold transition sm:flex-none"
              :class="
                activeStatusFilter === quick.value
                  ? 'border-teal-600/20 bg-teal-700 text-white'
                  : 'border-slate-200 bg-white text-slate-600 hover:border-sky-300 hover:text-slate-900'
              "
              @click="activeStatusFilter = quick.value"
            >
              {{ quick.label }}
            </button>
          </div>
        </div>
      </section>

      <div class="grid gap-4 2xl:grid-cols-[minmax(0,1.15fr)_360px]">
        <div class="min-w-0 space-y-4">
          <DeliveryParcelList
            :parcels="filteredParcels"
            :loading="parcelsLoading"
            :error="parcelsError"
            :selected-id="selectedParcelId"
            :selected-ids="selectedParcelIds"
            :selection-mode="selectionMode"
            :deleting-selection="bulkDeletingParcels"
            :total-count="parcels.length"
            @select="selectParcel"
            @start-selection="startParcelSelection"
            @toggle-selection="toggleParcelSelection"
            @toggle-all-visible="toggleAllVisibleParcels"
            @clear-selection="clearParcelSelection"
            @delete-selected="deleteSelectedParcels"
          />

          <DeliveryCandidateReviewList
            :candidates="trackingCandidates"
            :loading="candidatesLoading"
            :error="candidatesError"
            :confirming-id="confirmingCandidateId"
            :ignoring-id="ignoringCandidateId"
            @confirm="confirmCandidate"
            @ignore="ignoreCandidate"
          />
        </div>

        <div class="min-w-0 space-y-4">
          <DeliveryParcelTimeline
            :parcel="selectedParcel"
            :refreshing="refreshingParcelId === selectedParcel?.id"
            :deleting="deletingParcelIds.includes(selectedParcel?.id)"
            @refresh="refreshParcel"
            @delete="deleteParcel"
          />

          <div ref="mailAccountsSectionRef">
            <DeliveryMailAccounts
              :accounts="mailAccounts"
              :loading="accountsLoading"
              :error="accountsError"
              :connecting="connectingGmail"
              :scanning-id="scanningAccountId"
              :scanning-all="scanningAll"
              @connect-gmail="connectGmail"
              @scan-now="scanNow"
              @scan-all="scanAllNow"
              @delete-account="deleteAccount"
            />
          </div>

          <DeliveryScanReport
            :report="latestScanReport"
            :loading="scanningAll || scanningAccountId !== null"
          />
        </div>
      </div>
    </div>

    <Transition
      enter-active-class="transition duration-200 ease-out"
      enter-from-class="translate-y-3 opacity-0"
      enter-to-class="translate-y-0 opacity-100"
      leave-active-class="transition duration-150 ease-in"
      leave-from-class="translate-y-0 opacity-100"
      leave-to-class="translate-y-3 opacity-0"
    >
      <div
        v-if="selectionMode"
        class="sticky bottom-4 z-20 rounded-[22px] border border-slate-200 bg-white/95 p-3 shadow-[0_14px_34px_rgba(15,23,42,0.12)] backdrop-blur"
      >
        <div class="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <p class="text-sm font-semibold text-slate-900">
              {{ selectedParcelIds.length }} colis selectionne(s)
            </p>
            <p class="mt-1 text-xs text-slate-500">
              Raccourcis: `/` recherche, `N` ajout manuel, `M` mode multi-selection.
            </p>
          </div>
          <div class="flex flex-wrap items-center gap-2">
            <button
              type="button"
              class="inline-flex h-9 items-center justify-center rounded-full border border-slate-200 bg-slate-50 px-3 text-xs font-semibold text-slate-700 transition hover:border-sky-300 hover:bg-white"
              @click="toggleAllVisibleParcels"
            >
              {{ filteredParcels.length && filteredParcels.every((parcel) => selectedParcelIds.includes(parcel.id)) ? 'Tout deselectionner' : 'Tout selectionner' }}
            </button>
            <button
              type="button"
              class="inline-flex h-9 items-center justify-center rounded-full border border-slate-200 bg-white px-3 text-xs font-semibold text-slate-700 transition hover:border-sky-300 hover:bg-slate-50"
              @click="clearParcelSelection"
            >
              Annuler
            </button>
            <button
              type="button"
              class="inline-flex h-9 items-center justify-center rounded-full border border-red-300/40 bg-red-50 px-3 text-xs font-semibold text-red-700 transition hover:border-red-400 hover:bg-red-100 disabled:cursor-wait disabled:opacity-60"
              :disabled="!selectedParcelIds.length || bulkDeletingParcels"
              @click="deleteSelectedParcels"
            >
              {{ bulkDeletingParcels ? 'Suppression...' : `Supprimer (${selectedParcelIds.length})` }}
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </section>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import {
  AlertTriangle,
  CheckCircle2,
  Clock3,
  Layers3,
  MailPlus,
  PencilLine,
  PackageCheck,
  PackageSearch,
  RefreshCw,
  Search,
  Truck,
  X,
} from 'lucide-vue-next'
import DeliveryTrackingService from '@/services/DeliveryTrackingService.js'
import DeliveryMailAccounts from '@/components/gestion/DeliveryMailAccounts.vue'
import DeliveryManualParcelForm from '@/components/gestion/DeliveryManualParcelForm.vue'
import DeliveryCandidateReviewList from '@/components/gestion/DeliveryCandidateReviewList.vue'
import DeliveryScanReport from '@/components/gestion/DeliveryScanReport.vue'
import DeliveryParcelList from '@/components/gestion/DeliveryParcelList.vue'
import DeliveryParcelTimeline from '@/components/gestion/DeliveryParcelTimeline.vue'
import { isActiveParcelStatus } from '@/utils/deliveryPresentation.js'

const route = useRoute()
const mailAccounts = ref([])
const parcels = ref([])
const trackingCandidates = ref([])
const accountsLoading = ref(false)
const parcelsLoading = ref(false)
const candidatesLoading = ref(false)
const accountsError = ref('')
const parcelsError = ref('')
const candidatesError = ref('')
const manualParcelError = ref('')
const connectingGmail = ref(false)
const creatingManualParcel = ref(false)
const scanningAccountId = ref(null)
const scanningAll = ref(false)
const refreshingParcelId = ref(null)
const deletingParcelId = ref(null)
const deletingParcelIds = ref([])
const bulkDeletingParcels = ref(false)
const confirmingCandidateId = ref(null)
const ignoringCandidateId = ref(null)
const selectedParcelId = ref(null)
const selectedParcelIds = ref([])
const activeStatusFilter = ref('all')
const searchTerm = ref('')
const autoScanAfterCallbackStarted = ref(false)
const scanNotice = ref(null)
const latestScanReport = ref(null)
const manualSuccessToken = ref(0)
const manualModalOpen = ref(false)
const searchInput = ref(null)
const manualFormRef = ref(null)
const mailAccountsSectionRef = ref(null)
const lastSuccessfulSyncAt = ref(null)

const filteredParcels = computed(() => {
  const term = searchTerm.value.trim().toLowerCase()
  const filtered = parcels.value.filter((parcel) => {
    if (!matchesStatusFilter(parcel, activeStatusFilter.value)) return false
    if (!term) return true
    return [
      parcel.trackingNumber,
      parcel.normalizedTrackingNumber,
      parcel.carrierSlug,
      parcel.status,
      parcel.statusLabel,
      parcel.originAddress,
      parcel.destinationAddress,
      parcel.shipmentType,
    ]
      .filter(Boolean)
      .some((value) => String(value).toLowerCase().includes(term))
  })

  return [...filtered].sort((a, b) => {
    const priorityDiff = parcelPriority(a) - parcelPriority(b)
    if (priorityDiff !== 0) {
      return priorityDiff
    }
    return sortableDate(b) - sortableDate(a)
  })
})

const selectedParcel = computed(
  () =>
    filteredParcels.value.find((parcel) => parcel.id === selectedParcelId.value) ||
    filteredParcels.value[0] ||
    null,
)
const selectionMode = computed(() => selectedParcelIds.value.length > 0 || bulkDeletingParcels.value)
const primaryActionLabel = computed(() =>
  hasMailAccounts.value ? 'Scanner mes mails' : 'Connecter Gmail',
)
const activityState = computed(() => {
  if (scanningAll.value) {
    return {
      title: 'Scan Gmail en cours',
      detail: 'Lecture des boites mail, detection des vrais numeros de suivi et mise a jour de la liste.',
      badge: 'Scan global',
    }
  }
  if (scanningAccountId.value !== null) {
    return {
      title: 'Scan du compte Gmail en cours',
      detail: 'Analyse d une source mail et verification des suivis detectes.',
      badge: 'Scan source',
    }
  }
  if (refreshingParcelId.value !== null) {
    return {
      title: 'Rafraichissement du colis en cours',
      detail: 'Interrogation du transporteur pour recuperer le dernier avancement.',
      badge: 'Colis',
    }
  }
  if (accountsLoading.value || parcelsLoading.value || candidatesLoading.value) {
    return {
      title: 'Synchronisation de l onglet livraison',
      detail: 'Mise a jour des colis, comptes Gmail et candidats a verifier.',
      badge: 'Refresh',
    }
  }
  if (creatingManualParcel.value) {
    return {
      title: 'Ajout du suivi en cours',
      detail: 'Enregistrement du numero puis tentative de detection transporteur.',
      badge: 'Ajout',
    }
  }
  return null
})
const lastSyncLabel = computed(() => {
  if (!lastSuccessfulSyncAt.value) {
    return 'Aucune synchronisation recente visible.'
  }
  return `Derniere synchronisation: ${formatShortDateTime(lastSuccessfulSyncAt.value)}`
})

const activeMailAccounts = computed(() =>
  mailAccounts.value.filter((account) => account.status === 'ACTIVE'),
)
const hasMailAccounts = computed(() => mailAccounts.value.length > 0)
const automationLabel = computed(() => {
  if (!hasMailAccounts.value) {
    return 'Connecte un ou plusieurs Gmail, puis scanne uniquement les nouveaux emails de livraison.'
  }
  if (scanningAll.value) {
    return 'Scan des boites Gmail en cours. Seuls les vrais numeros de suivi sont gardes.'
  }
  return `${activeMailAccounts.value.length || mailAccounts.value.length} source(s) surveillee(s), suivi gratuit via Gmail + transporteurs compatibles.`
})

const activeParcelCount = computed(
  () => parcels.value.filter((parcel) => isActiveParcelStatus(parcel.status)).length,
)
const deliveredParcelCount = computed(
  () => parcels.value.filter((parcel) => parcel.status === 'DELIVERED').length,
)
const exceptionParcelCount = computed(
  () => parcels.value.filter((parcel) => parcel.status === 'EXCEPTION').length,
)
const candidateReviewCount = computed(() => trackingCandidates.value.length)
const outForDeliveryCount = computed(
  () => parcels.value.filter((parcel) => parcel.status === 'OUT_FOR_DELIVERY').length,
)
const inTransitCount = computed(
  () => parcels.value.filter((parcel) => parcel.status === 'IN_TRANSIT').length,
)

const metrics = computed(() => [
  {
    label: 'Total',
    value: parcels.value.length,
    icon: PackageSearch,
    iconClass: 'text-slate-400',
    valueClass: 'text-white',
  },
  {
    label: 'En cours',
    value: activeParcelCount.value,
    icon: Truck,
    iconClass: 'text-violet-300',
    valueClass: 'text-violet-100',
  },
  {
    label: 'Livres',
    value: deliveredParcelCount.value,
    icon: CheckCircle2,
    iconClass: 'text-emerald-300',
    valueClass: 'text-emerald-200',
  },
  {
    label: 'A verifier',
    value: candidateReviewCount.value,
    icon: AlertTriangle,
    iconClass: 'text-amber-300',
    valueClass: 'text-amber-100',
  },
  {
    label: 'Incidents',
    value: exceptionParcelCount.value,
    icon: AlertTriangle,
    iconClass: 'text-red-300',
    valueClass: 'text-red-200',
  },
])

const statusFilters = computed(() => [
  { value: 'all', label: 'Tous les colis', count: parcels.value.length, icon: PackageSearch },
  { value: 'active', label: 'En cours', count: activeParcelCount.value, icon: Clock3 },
  { value: 'IN_TRANSIT', label: 'En transit', count: inTransitCount.value, icon: Truck },
  {
    value: 'OUT_FOR_DELIVERY',
    label: 'En livraison',
    count: outForDeliveryCount.value,
    icon: PackageCheck,
  },
  { value: 'DELIVERED', label: 'Livres', count: deliveredParcelCount.value, icon: CheckCircle2 },
  {
    value: 'EXCEPTION',
    label: 'Incidents',
    count: exceptionParcelCount.value,
    icon: AlertTriangle,
  },
])

const quickFilters = computed(() => statusFilters.value.slice(0, 6))

const callbackNotice = computed(() => {
  if (route.query?.gmail === 'connected') {
    return { kind: 'success', message: 'Compte Gmail lie. Scan automatique lance.' }
  }
  if (route.query?.gmail === 'error') {
    return { kind: 'error', message: 'Connexion Gmail interrompue.' }
  }
  return null
})

const loadMailAccounts = async () => {
  accountsLoading.value = true
  accountsError.value = ''
  try {
    const { data } = await DeliveryTrackingService.listMailAccounts()
    mailAccounts.value = Array.isArray(data) ? data : []
  } catch (error) {
    accountsError.value = error?.response?.data?.message || 'Chargement des comptes impossible'
    mailAccounts.value = []
  } finally {
    accountsLoading.value = false
  }
}

const loadParcels = async () => {
  parcelsLoading.value = true
  parcelsError.value = ''
  try {
    const { data } = await DeliveryTrackingService.listParcels()
    parcels.value = Array.isArray(data) ? data : []
  } catch (error) {
    parcelsError.value = error?.response?.data?.message || 'Chargement des colis impossible'
    parcels.value = []
  } finally {
    parcelsLoading.value = false
  }
}

const loadTrackingCandidates = async () => {
  candidatesLoading.value = true
  candidatesError.value = ''
  try {
    const { data } = await DeliveryTrackingService.listTrackingCandidates()
    trackingCandidates.value = Array.isArray(data) ? data : []
  } catch (error) {
    candidatesError.value =
      error?.response?.data?.message || 'Chargement des candidats impossible'
    trackingCandidates.value = []
  } finally {
    candidatesLoading.value = false
  }
}

const refreshAll = async () => {
  await Promise.all([loadMailAccounts(), loadParcels(), loadTrackingCandidates()])
  lastSuccessfulSyncAt.value = new Date().toISOString()
}

const openManualModal = async () => {
  manualModalOpen.value = true
  await nextTick()
  window.setTimeout(() => {
    manualFormRef.value?.focusFirstField?.()
  }, 120)
}

const closeManualModal = () => {
  if (creatingManualParcel.value) return
  manualModalOpen.value = false
  manualParcelError.value = ''
}

const scrollToMailAccounts = () => {
  mailAccountsSectionRef.value?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

const handlePrimaryAction = async () => {
  if (hasMailAccounts.value) {
    await scanAllNow()
    return
  }
  scrollToMailAccounts()
}

const connectGmail = async (emailAddress = '') => {
  connectingGmail.value = true
  accountsError.value = ''
  try {
    const { data } = await DeliveryTrackingService.connectGmail(emailAddress)
    const url = data?.authorizationUrl
    if (url) {
      window.location.assign(url)
    }
  } catch (error) {
    accountsError.value = error?.response?.data?.message || 'Connexion Gmail impossible'
  } finally {
    connectingGmail.value = false
  }
}

const scanNow = async (accountId) => {
  await scanAccount(accountId)
}

const scanAccount = async (accountId, refresh = true, showNotice = true) => {
  scanningAccountId.value = accountId
  accountsError.value = ''
  try {
    const { data } = await DeliveryTrackingService.scanNow(accountId)
    latestScanReport.value = normalizeScanBatchReport({
      scannedAccounts: data?.success ? 1 : 0,
      scannedMessages: Number(data?.scannedMessages || 0),
      deliveryMessages: Number(data?.deliveryMessages || 0),
      importedCount: Number(data?.importedCount || 0),
      reviewCount: Number(data?.reviewCount || 0),
      rejectedCount: Number(data?.rejectedCount || 0),
      duplicateCount: Number(data?.duplicateCount || 0),
      message: data?.message || '',
      accountReports: data ? [data] : [],
      importedParcels: Array.isArray(data?.importedParcels) ? data.importedParcels : [],
      duplicateParcels: Array.isArray(data?.duplicateParcels) ? data.duplicateParcels : [],
      candidatesToReview: Array.isArray(data?.candidatesToReview) ? data.candidatesToReview : [],
    })
    if (showNotice) {
      scanNotice.value = scanNoticeFromSummary(data)
    }
    if (refresh) {
      await refreshAll()
    } else {
      lastSuccessfulSyncAt.value = new Date().toISOString()
    }
    return data
  } catch (error) {
    accountsError.value = error?.response?.data?.message || 'Scan Gmail impossible'
    throw error
  } finally {
    scanningAccountId.value = null
  }
}

const scanAllNow = async () => {
  if (!mailAccounts.value.length) return

  scanningAll.value = true
  accountsError.value = ''
  try {
    const { data } = await DeliveryTrackingService.scanAll()
    latestScanReport.value = normalizeScanBatchReport(data)
    scanNotice.value = scanNoticeFromSummary(data)
    await refreshAll()
  } catch (error) {
    accountsError.value = error?.response?.data?.message || 'Scan Gmail impossible'
  } finally {
    scanningAll.value = false
  }
}

const scanNewestAccountAfterCallback = async () => {
  if (autoScanAfterCallbackStarted.value || route.query?.gmail !== 'connected') return

  const accountsToScan = [...activeMailAccounts.value].sort((a, b) => {
    const dateA = a.createdAt ? new Date(a.createdAt).getTime() : 0
    const dateB = b.createdAt ? new Date(b.createdAt).getTime() : 0
    return dateB - dateA
  })

  const account = accountsToScan[0]
  if (!account?.id) return

  autoScanAfterCallbackStarted.value = true
  await scanAccount(account.id)
}

const createManualParcel = async (payload) => {
  creatingManualParcel.value = true
  manualParcelError.value = ''
  try {
    const numbers = parseManualTrackingEntries(payload?.trackingInput)
    if (!numbers.length) {
      manualParcelError.value = 'Ajoute au moins un numero de suivi valide.'
      return
    }

    const createdParcels = []
    const failedNumbers = []
    for (const trackingNumber of numbers) {
      try {
        const { data } = await DeliveryTrackingService.createParcel({
          trackingNumber,
          carrierSlug: payload?.carrierSlug || null,
        })
        if (data?.id) {
          createdParcels.push(data)
        }
      } catch {
        failedNumbers.push(trackingNumber)
      }
    }

    if (!createdParcels.length) {
      manualParcelError.value = "Aucun suivi n'a pu etre ajoute."
      return
    }

    await loadParcels()
    lastSuccessfulSyncAt.value = new Date().toISOString()
    const latestParcel = createdParcels[createdParcels.length - 1]
    if (latestParcel?.id) {
      selectedParcelId.value = latestParcel.id
    }
    manualSuccessToken.value += 1
    manualModalOpen.value = false
    scanNotice.value = {
      kind: failedNumbers.length ? 'warning' : 'success',
      message: failedNumbers.length
        ? `${createdParcels.length} suivi(s) ajoute(s), ${failedNumbers.length} refuse(s).`
        : `${createdParcels.length} suivi(s) ajoute(s) au tableau de livraison.`,
    }
  } catch (error) {
    manualParcelError.value = error?.response?.data?.message || 'Ajout du colis impossible'
  } finally {
    creatingManualParcel.value = false
  }
}

const refreshParcel = async (parcelId) => {
  if (!parcelId) return
  refreshingParcelId.value = parcelId
  parcelsError.value = ''
  try {
    const { data } = await DeliveryTrackingService.refreshParcel(parcelId)
    if (data?.id) {
      parcels.value = parcels.value.map((parcel) => (parcel.id === data.id ? data : parcel))
      selectedParcelId.value = data.id
      lastSuccessfulSyncAt.value = new Date().toISOString()
    } else {
      await loadParcels()
      lastSuccessfulSyncAt.value = new Date().toISOString()
    }
  } catch (error) {
    parcelsError.value = error?.response?.data?.message || 'Mise a jour du suivi impossible'
  } finally {
    refreshingParcelId.value = null
  }
}

const confirmCandidate = async (candidateId) => {
  if (!candidateId) return
  confirmingCandidateId.value = candidateId
  candidatesError.value = ''
  try {
    const { data } = await DeliveryTrackingService.confirmTrackingCandidate(candidateId)
    trackingCandidates.value = trackingCandidates.value.filter(
      (candidate) => candidate.id !== candidateId,
    )
    if (data?.id) {
      const existing = parcels.value.some((parcel) => parcel.id === data.id)
      parcels.value = existing
        ? parcels.value.map((parcel) => (parcel.id === data.id ? data : parcel))
        : [data, ...parcels.value]
      selectedParcelId.value = data.id
    } else {
    await loadParcels()
    lastSuccessfulSyncAt.value = new Date().toISOString()
    }
    scanNotice.value = { kind: 'success', message: 'Candidat valide et ajoute au suivi.' }
  } catch (error) {
    candidatesError.value = error?.response?.data?.message || 'Validation impossible'
  } finally {
    confirmingCandidateId.value = null
  }
}

const ignoreCandidate = async (candidateId) => {
  if (!candidateId) return
  ignoringCandidateId.value = candidateId
  candidatesError.value = ''
  try {
    await DeliveryTrackingService.ignoreTrackingCandidate(candidateId)
    trackingCandidates.value = trackingCandidates.value.filter(
      (candidate) => candidate.id !== candidateId,
    )
    scanNotice.value = { kind: 'info', message: 'Candidat ignore.' }
  } catch (error) {
    candidatesError.value = error?.response?.data?.message || 'Action impossible'
  } finally {
    ignoringCandidateId.value = null
  }
}

const deleteParcel = async (parcelId) => {
  if (!parcelId) return
  const confirmed = window.confirm('Supprimer ce suivi de livraison ?')
  if (!confirmed) return

  deletingParcelId.value = parcelId
  deletingParcelIds.value = [parcelId]
  parcelsError.value = ''
  try {
    await DeliveryTrackingService.deleteParcel(parcelId)
    parcels.value = parcels.value.filter((parcel) => parcel.id !== parcelId)
    selectedParcelIds.value = selectedParcelIds.value.filter((id) => id !== parcelId)
    if (selectedParcelId.value === parcelId) {
      selectedParcelId.value = filteredParcels.value[0]?.id || null
    }
  } catch (error) {
    parcelsError.value = error?.response?.data?.message || 'Suppression du suivi impossible'
  } finally {
    deletingParcelId.value = null
    deletingParcelIds.value = []
  }
}

const deleteAccount = async (accountId) => {
  accountsError.value = ''
  try {
    await DeliveryTrackingService.deleteMailAccount(accountId)
    await refreshAll()
  } catch (error) {
    accountsError.value = error?.response?.data?.message || 'Suppression du compte impossible'
  }
}

const selectParcel = (parcelId) => {
  selectedParcelId.value = parcelId
}

const startParcelSelection = () => {
  if (!selectedParcel.value?.id) return
  selectedParcelIds.value = [selectedParcel.value.id]
}

const toggleParcelSelection = (parcelId) => {
  if (!parcelId) return
  if (selectedParcelIds.value.includes(parcelId)) {
    selectedParcelIds.value = selectedParcelIds.value.filter((id) => id !== parcelId)
    return
  }
  selectedParcelIds.value = [...selectedParcelIds.value, parcelId]
}

const toggleAllVisibleParcels = () => {
  const visibleIds = filteredParcels.value.map((parcel) => parcel.id).filter(Boolean)
  if (!visibleIds.length) return

  const allSelected = visibleIds.every((id) => selectedParcelIds.value.includes(id))
  selectedParcelIds.value = allSelected
    ? selectedParcelIds.value.filter((id) => !visibleIds.includes(id))
    : Array.from(new Set([...selectedParcelIds.value, ...visibleIds]))
}

const clearParcelSelection = () => {
  selectedParcelIds.value = []
}

const deleteSelectedParcels = async () => {
  const ids = [...selectedParcelIds.value]
  if (!ids.length) return

  const confirmed = window.confirm(`Supprimer ${ids.length} suivi(s) de livraison ?`)
  if (!confirmed) return

  bulkDeletingParcels.value = true
  deletingParcelIds.value = ids
  parcelsError.value = ''

  const failedIds = []
  for (const parcelId of ids) {
    try {
      await DeliveryTrackingService.deleteParcel(parcelId)
      parcels.value = parcels.value.filter((parcel) => parcel.id !== parcelId)
    } catch {
      failedIds.push(parcelId)
    }
  }

  selectedParcelIds.value = failedIds
  deletingParcelIds.value = []
  bulkDeletingParcels.value = false

  if (selectedParcelId.value && !parcels.value.some((parcel) => parcel.id === selectedParcelId.value)) {
    selectedParcelId.value = filteredParcels.value[0]?.id || null
  }

  if (failedIds.length) {
    parcelsError.value = `${failedIds.length} suppression(s) ont echoue.`
    scanNotice.value = {
      kind: 'warning',
      message: `${ids.length - failedIds.length} suivi(s) supprime(s), ${failedIds.length} en erreur.`,
    }
    return
  }

  scanNotice.value = {
    kind: 'success',
    message: `${ids.length} suivi(s) supprime(s) de la liste.`,
  }
}

const matchesStatusFilter = (parcel, filter) => {
  if (filter === 'all') return true
  if (filter === 'active') return isActiveParcelStatus(parcel.status)
  return parcel.status === filter
}

const parcelPriority = (parcel) => {
  switch (parcel?.status) {
    case 'EXCEPTION':
      return 0
    case 'OUT_FOR_DELIVERY':
      return 1
    case 'IN_TRANSIT':
      return 2
    case 'REGISTERED':
    case 'PENDING':
    case 'UNKNOWN':
      return 3
    case 'DELIVERED':
      return 4
    default:
      return 5
  }
}

const sortableDate = (parcel) => {
  const raw = parcel.lastEventAt || parcel.updatedAt || parcel.firstSeenAt
  const date = raw ? new Date(raw) : null
  return date && !Number.isNaN(date.getTime()) ? date.getTime() : 0
}

const normalizeScanBatchReport = (report = {}) => ({
  scannedAccounts: Number(report?.scannedAccounts || 0),
  scannedMessages: Number(report?.scannedMessages || 0),
  deliveryMessages: Number(report?.deliveryMessages || 0),
  importedCount: Number(report?.importedCount || 0),
  reviewCount: Number(report?.reviewCount || 0),
  rejectedCount: Number(report?.rejectedCount || 0),
  duplicateCount: Number(report?.duplicateCount || 0),
  message: String(report?.message || '').trim(),
  accountReports: Array.isArray(report?.accountReports) ? report.accountReports : [],
  importedParcels: Array.isArray(report?.importedParcels) ? report.importedParcels : [],
  duplicateParcels: Array.isArray(report?.duplicateParcels) ? report.duplicateParcels : [],
  candidatesToReview: Array.isArray(report?.candidatesToReview) ? report.candidatesToReview : [],
})

const parseManualTrackingEntries = (rawValue) => {
  const source = String(rawValue || '')
  return Array.from(
    new Set(
      source
        .split(/[\n,;]+/)
        .map((value) => String(value || '').trim())
        .filter(Boolean),
    ),
  )
}

const formatShortDateTime = (value) => {
  if (!value) return 'inconnue'
  const date = value instanceof Date ? value : new Date(value)
  if (Number.isNaN(date.getTime())) return 'inconnue'
  return new Intl.DateTimeFormat('fr-FR', {
    dateStyle: 'short',
    timeStyle: 'short',
  }).format(date)
}

const scanNoticeFromSummary = (summary = {}) => {
  if (summary.message) {
    return {
      kind:
        Number(summary.importedCount || 0) > 0
          ? 'success'
          : Number(summary.reviewCount || 0) > 0
            ? 'warning'
            : 'info',
      message: summary.message,
    }
  }
  if (summary.importedCount > 0 || summary.reviewCount > 0) {
    return {
      kind: summary.importedCount > 0 ? 'success' : 'warning',
      message: `${summary.importedCount} suivi(s) importe(s), ${summary.reviewCount} candidat(s) a verifier.`,
    }
  }
  if (summary.scannedMessages === 0) {
    return { kind: 'info', message: 'Aucun nouvel email de livraison a analyser.' }
  }
  return {
    kind: 'info',
    message:
      'Aucun numero de suivi fiable trouve. Les numeros faibles ont ete ignores.',
  }
}

const shouldIgnoreShortcut = (target) => {
  if (!(target instanceof HTMLElement)) return false
  const tag = target.tagName?.toLowerCase()
  return (
    tag === 'input' ||
    tag === 'textarea' ||
    tag === 'select' ||
    target.isContentEditable
  )
}

const handleGlobalShortcut = (event) => {
  if (manualModalOpen.value && event.key === 'Escape') {
    event.preventDefault()
    closeManualModal()
    return
  }

  if (shouldIgnoreShortcut(event.target)) return

  if (event.key === '/') {
    event.preventDefault()
    searchInput.value?.focus()
    return
  }

  if (event.key.toLowerCase() === 'n') {
    event.preventDefault()
    openManualModal()
    return
  }

  if (event.key.toLowerCase() === 'm') {
    event.preventDefault()
    if (selectionMode.value) {
      clearParcelSelection()
    } else {
      startParcelSelection()
    }
  }
}

watch(
  filteredParcels,
  (next) => {
    if (!next.length) {
      selectedParcelId.value = null
      return
    }
    if (!next.some((parcel) => parcel.id === selectedParcelId.value)) {
      selectedParcelId.value = next[0].id
    }
  },
  { immediate: true },
)

watch(
  parcels,
  (next) => {
    const validIds = new Set(next.map((parcel) => parcel.id))
    selectedParcelIds.value = selectedParcelIds.value.filter((id) => validIds.has(id))
  },
  { deep: true },
)

const bootstrapDelivery = async () => {
  await refreshAll()
  await scanNewestAccountAfterCallback()
  window.addEventListener('keydown', handleGlobalShortcut)
}

onMounted(bootstrapDelivery)
onBeforeUnmount(() => {
  window.removeEventListener('keydown', handleGlobalShortcut)
})
</script>

<style scoped>
.delivery-panel-loader {
  width: 34%;
  animation: delivery-panel-progress 1.2s ease-in-out infinite;
}

@keyframes delivery-panel-progress {
  0% {
    transform: translateX(-110%);
  }

  50% {
    transform: translateX(105%);
  }

  100% {
    transform: translateX(250%);
  }
}
</style>
