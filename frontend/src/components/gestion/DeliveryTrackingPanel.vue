<template>
  <section class="delivery-tracking-panel space-y-4">
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
            ? 'border-emerald-400/30 bg-emerald-500/10 text-emerald-100'
            : 'border-red-400/30 bg-red-500/10 text-red-100'
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
            ? 'border-emerald-400/30 bg-emerald-500/10 text-emerald-100'
            : scanNotice.kind === 'warning'
              ? 'border-amber-300/30 bg-amber-400/10 text-amber-100'
              : 'border-slate-600/70 bg-slate-900/70 text-slate-200'
        "
      >
        {{ scanNotice.message }}
      </div>
    </Transition>

    <div class="grid gap-4 xl:grid-cols-[320px_minmax(0,1fr)]">
      <div class="space-y-4 xl:sticky xl:top-4 xl:self-start">
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

        <div class="space-y-2">
          <button
            type="button"
            class="flex w-full items-center justify-between gap-3 rounded-2xl border border-slate-700/70 bg-slate-900/55 px-4 py-3 text-left text-sm font-semibold text-slate-200 transition hover:bg-slate-800/65 hover:text-white"
            @click="showManualForm = !showManualForm"
          >
            <span class="inline-flex items-center gap-2">
              <Plus class="h-4 w-4 text-slate-500" />
              Ajouter un numero manuellement
            </span>
            <ChevronDown
              class="h-4 w-4 text-slate-500 transition"
              :class="{ 'rotate-180': showManualForm }"
            />
          </button>
          <Transition
            enter-active-class="transition duration-150 ease-out"
            enter-from-class="opacity-0 -translate-y-1"
            enter-to-class="opacity-100 translate-y-0"
            leave-active-class="transition duration-100 ease-in"
            leave-from-class="opacity-100 translate-y-0"
            leave-to-class="opacity-0 -translate-y-1"
          >
            <div v-if="showManualForm" class="mt-3">
              <DeliveryManualParcelForm
                :loading="creatingManualParcel"
                :error="manualParcelError"
                @create-parcel="createManualParcel"
              />
            </div>
          </Transition>
        </div>
      </div>

      <div class="min-w-0 space-y-4">
        <section
          class="rounded-[24px] border border-slate-700/70 bg-slate-900/70 p-4 shadow-xl shadow-slate-950/20 backdrop-blur sm:p-5"
        >
          <div class="flex flex-wrap items-center justify-between gap-3">
            <div>
              <p class="text-[11px] uppercase tracking-[0.22em] text-violet-300/80">
                Suivi livraison
              </p>
              <h2 class="mt-1 text-xl font-semibold text-white sm:text-2xl">Colis centralises</h2>
              <p class="mt-1 text-sm text-slate-400">
                {{ automationLabel }}
              </p>
            </div>
            <div class="flex flex-wrap items-center gap-2">
              <span
                class="hidden rounded-full border px-3 py-1 text-xs font-medium sm:inline-flex"
                :class="
                  hasMailAccounts
                    ? 'border-emerald-300/25 bg-emerald-400/10 text-emerald-100'
                    : 'border-amber-300/25 bg-amber-400/10 text-amber-100'
                "
              >
                {{ hasMailAccounts ? 'Scan auto actif' : 'Gmail a connecter' }}
              </span>
              <button
                v-if="hasMailAccounts"
                type="button"
                class="inline-flex h-9 items-center justify-center gap-2 rounded-full border border-violet-400/50 bg-violet-500/10 px-3 text-xs font-semibold text-violet-100 transition hover:border-violet-300/70 hover:bg-violet-500/20 disabled:cursor-wait disabled:opacity-60"
                :disabled="scanningAll || accountsLoading || parcelsLoading"
                title="Analyse les mails Gmail et importe les nouveaux numeros de suivi"
                @click="scanAllNow"
              >
                <RefreshCw class="h-4 w-4" :class="{ 'animate-spin': scanningAll }" />
                <span>{{ scanningAll ? 'Scan...' : 'Scanner mails' }}</span>
              </button>
              <button
                type="button"
                class="inline-flex h-9 items-center justify-center gap-2 rounded-full border border-slate-700/80 bg-slate-800/70 px-3 text-xs font-semibold text-slate-200 transition hover:border-slate-500 hover:bg-slate-800 disabled:cursor-wait disabled:opacity-60"
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

          <div class="mt-4 grid gap-2 sm:grid-cols-2 xl:grid-cols-5">
            <div
              v-for="metric in metrics"
              :key="metric.label"
              class="rounded-2xl border border-slate-700/70 bg-slate-950/35 p-3"
            >
              <div class="flex items-center justify-between gap-3">
                <p class="text-xs text-slate-400">{{ metric.label }}</p>
                <component :is="metric.icon" class="h-4 w-4" :class="metric.iconClass" />
              </div>
              <p class="mt-1 text-xl font-semibold" :class="metric.valueClass">
                {{ metric.value }}
              </p>
            </div>
          </div>

          <div class="mt-4 flex flex-col gap-3 lg:flex-row lg:items-center">
            <label class="relative min-w-0 flex-1">
              <Search
                class="pointer-events-none absolute left-4 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-500"
              />
              <input
                v-model="searchTerm"
                type="search"
                class="h-10 w-full rounded-full border border-slate-700/80 bg-slate-950/45 pl-10 pr-4 text-sm text-slate-100 outline-none transition placeholder:text-slate-600 focus:border-violet-400/70 focus:ring-2 focus:ring-violet-500/20"
                placeholder="Tracking, transporteur, statut"
              />
            </label>
            <div class="flex flex-wrap gap-2">
              <button
                v-for="quick in quickFilters"
                :key="quick.value"
                type="button"
                class="rounded-full border px-3 py-1.5 text-xs font-semibold transition"
                :class="
                  activeStatusFilter === quick.value
                    ? 'border-slate-100 bg-slate-100 text-slate-950'
                    : 'border-slate-700/80 bg-slate-950/35 text-slate-300 hover:border-slate-500 hover:text-white'
                "
                @click="activeStatusFilter = quick.value"
              >
                {{ quick.label }}
              </button>
            </div>
          </div>
        </section>

        <DeliveryCandidateReviewList
          :candidates="trackingCandidates"
          :loading="candidatesLoading"
          :error="candidatesError"
          :confirming-id="confirmingCandidateId"
          :ignoring-id="ignoringCandidateId"
          @confirm="confirmCandidate"
          @ignore="ignoreCandidate"
        />

        <div class="grid gap-4 2xl:grid-cols-[minmax(0,1.05fr)_minmax(340px,0.95fr)]">
          <DeliveryParcelList
            :parcels="filteredParcels"
            :loading="parcelsLoading"
            :error="parcelsError"
            :selected-id="selectedParcelId"
            :total-count="parcels.length"
            @select="selectParcel"
          />
          <DeliveryParcelTimeline
            :parcel="selectedParcel"
            :refreshing="refreshingParcelId === selectedParcel?.id"
            :deleting="deletingParcelId === selectedParcel?.id"
            @refresh="refreshParcel"
            @delete="deleteParcel"
          />
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import {
  AlertTriangle,
  ChevronDown,
  CheckCircle2,
  Clock3,
  PackageCheck,
  PackageSearch,
  Plus,
  RefreshCw,
  Search,
  Truck,
} from 'lucide-vue-next'
import DeliveryTrackingService from '@/services/DeliveryTrackingService.js'
import DeliveryMailAccounts from '@/components/gestion/DeliveryMailAccounts.vue'
import DeliveryManualParcelForm from '@/components/gestion/DeliveryManualParcelForm.vue'
import DeliveryCandidateReviewList from '@/components/gestion/DeliveryCandidateReviewList.vue'
import DeliveryParcelList from '@/components/gestion/DeliveryParcelList.vue'
import DeliveryParcelTimeline from '@/components/gestion/DeliveryParcelTimeline.vue'

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
const confirmingCandidateId = ref(null)
const ignoringCandidateId = ref(null)
const selectedParcelId = ref(null)
const activeStatusFilter = ref('all')
const searchTerm = ref('')
const showManualForm = ref(false)
const autoScanAfterCallbackStarted = ref(false)
const scanNotice = ref(null)

const activeStatuses = new Set([
  'PENDING',
  'REGISTERED',
  'IN_TRANSIT',
  'OUT_FOR_DELIVERY',
  'UNKNOWN',
])

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

  return [...filtered].sort((a, b) => sortableDate(b) - sortableDate(a))
})

const selectedParcel = computed(
  () =>
    filteredParcels.value.find((parcel) => parcel.id === selectedParcelId.value) ||
    filteredParcels.value[0] ||
    null,
)

const activeMailAccounts = computed(() =>
  mailAccounts.value.filter((account) => account.status === 'ACTIVE'),
)
const hasMailAccounts = computed(() => mailAccounts.value.length > 0)
const automationLabel = computed(() => {
  if (!hasMailAccounts.value) {
    return 'Connecte Gmail une fois, puis les nouveaux suivis remontent automatiquement.'
  }
  if (scanningAll.value) {
    return 'Scan des mails transporteurs en cours.'
  }
  return `${activeMailAccounts.value.length || mailAccounts.value.length} source(s) surveillee(s).`
})

const activeParcelCount = computed(
  () => parcels.value.filter((parcel) => activeStatuses.has(parcel.status)).length,
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
    if (showNotice) {
      scanNotice.value = scanNoticeFromSummary(data)
    }
    if (refresh) {
      await refreshAll()
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
  const accountsToScan = activeMailAccounts.value.length
    ? activeMailAccounts.value
    : mailAccounts.value
  if (!accountsToScan.length) return

  scanningAll.value = true
  accountsError.value = ''
  try {
    const totals = emptyScanSummary()
    for (const account of accountsToScan) {
      const summary = await scanAccount(account.id, false, false)
      addScanSummary(totals, summary)
    }
    scanNotice.value = scanNoticeFromSummary(totals)
    await refreshAll()
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
    const { data } = await DeliveryTrackingService.createParcel(payload)
    await loadParcels()
    if (data?.id) {
      selectedParcelId.value = data.id
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
    } else {
      await loadParcels()
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
  parcelsError.value = ''
  try {
    await DeliveryTrackingService.deleteParcel(parcelId)
    parcels.value = parcels.value.filter((parcel) => parcel.id !== parcelId)
    if (selectedParcelId.value === parcelId) {
      selectedParcelId.value = filteredParcels.value[0]?.id || null
    }
  } catch (error) {
    parcelsError.value = error?.response?.data?.message || 'Suppression du suivi impossible'
  } finally {
    deletingParcelId.value = null
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

const matchesStatusFilter = (parcel, filter) => {
  if (filter === 'all') return true
  if (filter === 'active') return activeStatuses.has(parcel.status)
  return parcel.status === filter
}

const sortableDate = (parcel) => {
  const raw = parcel.lastEventAt || parcel.updatedAt || parcel.firstSeenAt
  const date = raw ? new Date(raw) : null
  return date && !Number.isNaN(date.getTime()) ? date.getTime() : 0
}

const emptyScanSummary = () => ({
  scannedMessages: 0,
  deliveryMessages: 0,
  importedCount: 0,
  reviewCount: 0,
  rejectedCount: 0,
  duplicateCount: 0,
})

const addScanSummary = (target, source = {}) => {
  target.scannedMessages += Number(source.scannedMessages || 0)
  target.deliveryMessages += Number(source.deliveryMessages || 0)
  target.importedCount += Number(source.importedCount || 0)
  target.reviewCount += Number(source.reviewCount || 0)
  target.rejectedCount += Number(source.rejectedCount || 0)
  target.duplicateCount += Number(source.duplicateCount || 0)
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

const bootstrapDelivery = async () => {
  await refreshAll()
  await scanNewestAccountAfterCallback()
}

onMounted(bootstrapDelivery)
</script>
