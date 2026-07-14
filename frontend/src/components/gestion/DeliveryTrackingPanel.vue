<template>
  <section class="delivery-tracking-panel min-w-0 space-y-4">
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
          class="fixed inset-0 z-40 flex items-end justify-center bg-slate-950/45 p-3 backdrop-blur-sm sm:items-center sm:p-4"
          @click.self="closeManualModal"
        >
          <div
            class="max-h-[calc(100dvh-1rem)] w-full max-w-xl overflow-y-auto rounded-[28px] border border-slate-200 bg-[#fbfaf7] p-4 shadow-[0_24px_70px_rgba(15,23,42,0.20)] sm:max-h-[calc(100dvh-2rem)] sm:p-6"
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
        enter-from-class="opacity-0"
        enter-to-class="opacity-100"
        leave-active-class="transition duration-150 ease-in"
        leave-from-class="opacity-100"
        leave-to-class="opacity-0"
      >
        <div
          v-if="deliveredCleanupParcel && !manualModalOpen"
          class="fixed inset-0 z-40 flex items-end justify-center bg-slate-950/45 p-3 backdrop-blur-sm sm:items-center sm:p-4"
          @click.self="snoozeDeliveredCleanup"
        >
          <div
            class="max-h-[calc(100dvh-1rem)] w-full max-w-lg overflow-y-auto rounded-[28px] border border-slate-200 bg-[#fbfaf7] p-4 shadow-[0_24px_70px_rgba(15,23,42,0.20)] sm:max-h-[calc(100dvh-2rem)] sm:p-6"
          >
            <div class="flex items-start justify-between gap-4">
              <div>
                <p class="text-lg font-semibold text-slate-900">Nettoyer les colis livres</p>
                <p class="mt-1 text-sm text-slate-500">
                  Ce colis est livre depuis plus de deux semaines. Tu peux le retirer de la liste.
                </p>
              </div>
              <button
                type="button"
                class="inline-flex h-10 w-10 items-center justify-center rounded-full border border-slate-200 bg-white text-slate-500 transition hover:border-sky-300 hover:text-slate-900"
                aria-label="Fermer le rappel de suppression"
                @click="snoozeDeliveredCleanup"
              >
                <X class="h-4 w-4" />
              </button>
            </div>

            <div class="mt-5 rounded-[24px] border border-emerald-200 bg-white p-4 sm:p-5">
              <div class="flex flex-wrap items-start justify-between gap-3">
                <div class="min-w-0">
                  <p class="text-xs font-semibold uppercase tracking-[0.18em] text-emerald-700">
                    {{ carrierLabel(deliveredCleanupParcel.carrierSlug) }}
                  </p>
                  <p class="mt-2 break-all text-lg font-semibold text-slate-900">
                    {{ deliveredCleanupParcel.trackingNumber }}
                  </p>
                </div>
                <span class="inline-flex rounded-full border border-emerald-200 bg-emerald-50 px-3 py-1 text-xs font-semibold text-emerald-800">
                  Livre depuis {{ deliveredParcelAgeDays(deliveredCleanupParcel) }} jours
                </span>
              </div>
              <p class="mt-3 text-sm text-slate-600">
                Livraison confirmee le
                <span class="font-semibold text-slate-900">
                  {{ formatDeliveryPopupDate(deliveredCleanupParcel) }}
                </span>
              </p>
              <p v-if="deliveredCleanupCount > 1" class="mt-2 text-xs text-slate-500">
                {{ deliveredCleanupCount }} colis livres de plus de 14 jours attendent encore dans la liste.
              </p>
            </div>

            <div class="mt-5 flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
              <button
                type="button"
                class="inline-flex h-10 items-center justify-center rounded-full border border-slate-200 bg-white px-4 text-sm font-semibold text-slate-700 transition hover:border-sky-300 hover:bg-slate-50"
                @click="snoozeDeliveredCleanup"
              >
                Plus tard
              </button>
              <button
                type="button"
                class="inline-flex h-10 items-center justify-center rounded-full border border-red-300/40 bg-red-50 px-4 text-sm font-semibold text-red-700 transition hover:border-red-400 hover:bg-red-100 disabled:cursor-wait disabled:opacity-60"
                :disabled="deletingDeliveredCleanupParcel"
                @click="deleteDeliveredCleanupParcel"
              >
                {{ deletingDeliveredCleanupParcel ? 'Suppression...' : 'Supprimer ce colis' }}
              </button>
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
              type="button"
              class="inline-flex h-9 items-center justify-center gap-2 rounded-full border border-teal-600/20 bg-teal-700 px-3 text-xs font-semibold text-white transition hover:bg-teal-600 disabled:cursor-wait disabled:opacity-60"
              :disabled="refreshingAllParcels || scanningAll || accountsLoading || parcelsLoading"
              :title="refreshActionDetail"
              @click="handleRefreshAction"
            >
              <RefreshCw
                class="h-4 w-4"
                :class="{ 'animate-spin': refreshingAllParcels || scanningAll || accountsLoading || parcelsLoading }"
              />
              <span>{{ refreshActionLabel }}</span>
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

        <div class="mt-4 grid gap-2 lg:grid-cols-2">
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
                {{ hasMailAccounts ? 'Gerer tes comptes Gmail et le scan automatique' : 'Lier Gmail pour importer automatiquement les suivis' }}
              </p>
            </div>
            <MailPlus class="h-4 w-4 text-teal-700" />
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
              placeholder="Rechercher un numero, un transporteur ou un statut"
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

      <div class="grid gap-4 xl:grid-cols-[minmax(0,1.08fr)_minmax(300px,0.92fr)] 2xl:grid-cols-[minmax(0,1.15fr)_360px]">
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

        </div>
      </div>
    </div>

    <teleport to="body">
      <Transition
        enter-active-class="transition duration-200 ease-out"
        enter-from-class="translate-y-3 opacity-0"
        enter-to-class="translate-y-0 opacity-100"
        leave-active-class="transition duration-150 ease-in"
        leave-from-class="translate-y-0 opacity-100"
        leave-to-class="translate-y-3 opacity-0"
      >
        <div v-if="feedbackToast" class="delivery-toast-layer" aria-live="polite" role="status">
          <div class="delivery-toast" :class="`is-${feedbackToast.kind}`">
            <span class="delivery-toast__icon">
              <CheckCircle2 v-if="feedbackToast.kind === 'success'" class="h-5 w-5" />
              <AlertTriangle v-else-if="feedbackToast.kind === 'warning' || feedbackToast.kind === 'error'" class="h-5 w-5" />
              <MailPlus v-else class="h-5 w-5" />
            </span>
            <div class="delivery-toast__copy">
              <strong>{{ feedbackToast.title }}</strong>
              <span>{{ feedbackToast.message }}</span>
            </div>
          </div>
        </div>
      </Transition>
    </teleport>

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
  MailPlus,
  PencilLine,
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
import DeliveryParcelList from '@/components/gestion/DeliveryParcelList.vue'
import DeliveryParcelTimeline from '@/components/gestion/DeliveryParcelTimeline.vue'
import { carrierLabel, isActiveParcelStatus } from '@/utils/deliveryPresentation.js'

const route = useRoute()
const DELIVERED_CLEANUP_DAYS = 14
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
const refreshingAllParcels = ref(false)
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
const manualSuccessToken = ref(0)
const manualModalOpen = ref(false)
const searchInput = ref(null)
const manualFormRef = ref(null)
const mailAccountsSectionRef = ref(null)
const lastSuccessfulSyncAt = ref(null)
const feedbackToast = ref(null)
const deliveredCleanupSnoozed = ref(false)
let feedbackToastTimer = null

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
const deliveredCleanupCandidates = computed(() =>
  parcels.value
    .filter(isDeliveredCleanupCandidate)
    .sort((a, b) => deliveredReferenceTime(a) - deliveredReferenceTime(b)),
)
const deliveredCleanupParcel = computed(() =>
  deliveredCleanupSnoozed.value ? null : deliveredCleanupCandidates.value[0] || null,
)
const deliveredCleanupCount = computed(() => deliveredCleanupCandidates.value.length)
const deletingDeliveredCleanupParcel = computed(
  () => !!deliveredCleanupParcel.value?.id && deletingParcelIds.value.includes(deliveredCleanupParcel.value.id),
)
const selectionMode = computed(() => selectedParcelIds.value.length > 0 || bulkDeletingParcels.value)
const primaryActionLabel = computed(() =>
  hasMailAccounts.value ? 'Sources Gmail' : 'Connecter Gmail',
)
const refreshActionLabel = computed(() =>
  refreshingAllParcels.value ? 'Mise a jour...' : 'Actualiser tout',
)
const refreshActionDetail = computed(() =>
  'Rafraichit tous les colis en interrogeant directement leurs pages de suivi',
)
const activityState = computed(() => {
  if (refreshingAllParcels.value) {
    return {
      title: 'Rafraichissement global des colis',
      detail: 'Lecture rapide des pages de suivi pour mettre a jour le statut de chaque colis.',
      badge: 'Tous les colis',
    }
  }
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
    return 'Connecte Gmail pour relire tes emails transporteur et recuperer automatiquement les suivis.'
  }
  if (scanningAll.value) {
    return 'Lecture Gmail en cours. Seuls les vrais numeros de suivi sont gardes.'
  }
  if (refreshingAllParcels.value) {
    return 'Mise a jour en cours des statuts via les pages de suivi transporteur.'
  }
  return `${activeMailAccounts.value.length || mailAccounts.value.length} source(s) Gmail relues pour detecter de nouveaux suivis.`
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
const inTransitCount = computed(
  () => parcels.value.filter((parcel) => ['IN_TRANSIT', 'OUT_FOR_DELIVERY'].includes(parcel.status)).length,
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
  { value: 'DELIVERED', label: 'Livres', count: deliveredParcelCount.value, icon: CheckCircle2 },
  {
    value: 'EXCEPTION',
    label: 'Incidents',
    count: exceptionParcelCount.value,
    icon: AlertTriangle,
  },
])

const quickFilters = computed(() => statusFilters.value.slice(0, 6))

const toastTitleByKind = {
  success: 'Action terminee',
  warning: 'A verifier',
  error: 'Action impossible',
  info: 'Info livraison',
}

const clearFeedbackToast = () => {
  if (feedbackToastTimer) {
    window.clearTimeout(feedbackToastTimer)
    feedbackToastTimer = null
  }
}

const showFeedbackToast = (toast) => {
  if (!toast?.message) return
  clearFeedbackToast()
  const kind = toast.kind || 'info'
  feedbackToast.value = {
    kind,
    title: toast.title || toastTitleByKind[kind] || toastTitleByKind.info,
    message: toast.message,
  }
  feedbackToastTimer = window.setTimeout(() => {
    feedbackToast.value = null
    feedbackToastTimer = null
  }, kind === 'error' ? 4200 : 3000)
}

const showFeedbackSummary = (summary, title) => {
  const notice = scanNoticeFromSummary(summary)
  if (!notice) return
  showFeedbackToast({ ...notice, title })
}

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

const snoozeDeliveredCleanup = () => {
  deliveredCleanupSnoozed.value = true
}

const scrollToMailAccounts = () => {
  mailAccountsSectionRef.value?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

const handlePrimaryAction = async () => {
  scrollToMailAccounts()
}

const handleRefreshAction = async () => {
  await refreshAllParcelStatuses()
}

const refreshAllParcelStatuses = async () => {
  if (!parcels.value.length) {
    await refreshAll()
    return
  }

  refreshingAllParcels.value = true
  parcelsError.value = ''
  try {
    const { data } = await DeliveryTrackingService.refreshAllParcels()
    parcels.value = Array.isArray(data) ? data : []
    if (selectedParcelId.value && !parcels.value.some((parcel) => parcel.id === selectedParcelId.value)) {
      selectedParcelId.value = parcels.value[0]?.id ?? null
    }
    lastSuccessfulSyncAt.value = new Date().toISOString()
    showFeedbackToast({
      kind: 'success',
      title: 'Suivis mis a jour',
      message: `${parcels.value.length} colis rafraichi(s).`,
    })
  } catch (error) {
    parcelsError.value = error?.response?.data?.message || 'Mise a jour globale des suivis impossible'
  } finally {
    refreshingAllParcels.value = false
  }
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
      showFeedbackSummary(data, 'Scan Gmail termine')
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
    showFeedbackSummary(data, 'Import Gmail termine')
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
    showFeedbackToast({
      kind: failedNumbers.length ? 'warning' : 'success',
      title: failedNumbers.length ? 'Ajout partiel' : 'Suivi ajoute',
      message: failedNumbers.length
        ? `${createdParcels.length} suivi(s) ajoute(s), ${failedNumbers.length} refuse(s).`
        : `${createdParcels.length} suivi(s) ajoute(s) au tableau de livraison.`,
    })
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
    showFeedbackToast({
      kind: 'success',
      title: 'Colis ajoute',
      message: 'Candidat valide et ajoute au suivi.',
    })
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
    showFeedbackToast({
      kind: 'info',
      title: 'Candidat retire',
      message: 'Le candidat ne sera plus propose.',
    })
  } catch (error) {
    candidatesError.value = error?.response?.data?.message || 'Action impossible'
  } finally {
    ignoringCandidateId.value = null
  }
}

const removeParcelFromState = (parcelId) => {
  parcels.value = parcels.value.filter((parcel) => parcel.id !== parcelId)
  selectedParcelIds.value = selectedParcelIds.value.filter((id) => id !== parcelId)
  if (selectedParcelId.value === parcelId) {
    selectedParcelId.value = filteredParcels.value[0]?.id || null
  }
}

const performDeleteParcel = async (
  parcelId,
  {
    confirmMessage = null,
    successTitle = 'Suivi supprime',
    successMessage = 'Le colis a ete retire de la liste.',
  } = {},
) => {
  if (!parcelId) return
  if (confirmMessage && !window.confirm(confirmMessage)) return false

  deletingParcelId.value = parcelId
  deletingParcelIds.value = [parcelId]
  parcelsError.value = ''
  try {
    await DeliveryTrackingService.deleteParcel(parcelId)
    removeParcelFromState(parcelId)
    showFeedbackToast({
      kind: 'success',
      title: successTitle,
      message: successMessage,
    })
    return true
  } catch (error) {
    parcelsError.value = error?.response?.data?.message || 'Suppression du suivi impossible'
    return false
  } finally {
    deletingParcelId.value = null
    deletingParcelIds.value = []
  }
}

const deleteParcel = async (parcelId) =>
  performDeleteParcel(parcelId, {
    confirmMessage: 'Supprimer ce suivi de livraison ?',
  })

const deleteDeliveredCleanupParcel = async () => {
  const parcel = deliveredCleanupParcel.value
  if (!parcel?.id) return
  deliveredCleanupSnoozed.value = false
  await performDeleteParcel(parcel.id, {
    successTitle: 'Colis retire',
    successMessage: 'Le colis livre depuis plus de deux semaines a ete supprime.',
  })
}

const deleteAccount = async (accountId) => {
  accountsError.value = ''
  try {
    await DeliveryTrackingService.deleteMailAccount(accountId)
    await refreshAll()
    showFeedbackToast({
      kind: 'success',
      title: 'Compte retire',
      message: 'La source Gmail a ete supprimee.',
    })
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
    showFeedbackToast({
      kind: 'warning',
      title: 'Suppression partielle',
      message: `${ids.length - failedIds.length} suivi(s) supprime(s), ${failedIds.length} en erreur.`,
    })
    return
  }

  showFeedbackToast({
    kind: 'success',
    title: 'Selection supprimee',
    message: `${ids.length} suivi(s) supprime(s) de la liste.`,
  })
}

const matchesStatusFilter = (parcel, filter) => {
  if (filter === 'all') return true
  if (filter === 'active') return isActiveParcelStatus(parcel.status)
  if (filter === 'IN_TRANSIT') return ['IN_TRANSIT', 'OUT_FOR_DELIVERY'].includes(parcel.status)
  return parcel.status === filter
}

const parcelPriority = (parcel) => {
  switch (parcel?.status) {
    case 'EXCEPTION':
      return 0
    case 'IN_TRANSIT':
    case 'OUT_FOR_DELIVERY':
      return 1
    case 'REGISTERED':
    case 'PENDING':
    case 'UNKNOWN':
      return 2
    case 'DELIVERED':
      return 3
    default:
      return 4
  }
}

const sortableDate = (parcel) => {
  const raw = parcel.lastEventAt || parcel.updatedAt || parcel.firstSeenAt
  const date = raw ? new Date(raw) : null
  return date && !Number.isNaN(date.getTime()) ? date.getTime() : 0
}

const deliveredReferenceTime = (parcel) => {
  if (!parcel) return Number.POSITIVE_INFINITY
  const raw = parcel.deliveredAt || parcel.lastEventAt || parcel.updatedAt
  const date = raw ? new Date(raw) : null
  return date && !Number.isNaN(date.getTime()) ? date.getTime() : Number.POSITIVE_INFINITY
}

const isDeliveredCleanupCandidate = (parcel) => {
  if (!parcel || parcel.status !== 'DELIVERED') return false
  const deliveredTime = deliveredReferenceTime(parcel)
  if (!Number.isFinite(deliveredTime)) return false
  return Date.now() - deliveredTime >= DELIVERED_CLEANUP_DAYS * 24 * 60 * 60 * 1000
}

const deliveredParcelAgeDays = (parcel) => {
  const deliveredTime = deliveredReferenceTime(parcel)
  if (!Number.isFinite(deliveredTime)) return DELIVERED_CLEANUP_DAYS
  return Math.max(DELIVERED_CLEANUP_DAYS, Math.floor((Date.now() - deliveredTime) / (24 * 60 * 60 * 1000)))
}

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

const formatDeliveryPopupDate = (parcel) => {
  const raw = parcel?.deliveredAt || parcel?.lastEventAt || parcel?.updatedAt
  if (!raw) return 'date inconnue'
  const date = new Date(raw)
  if (Number.isNaN(date.getTime())) return 'date inconnue'
  return new Intl.DateTimeFormat('fr-FR', {
    dateStyle: 'medium',
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
    return null
  }
  return null
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
  if (deliveredCleanupParcel.value && event.key === 'Escape') {
    event.preventDefault()
    snoozeDeliveredCleanup()
    return
  }

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
  if (route.query?.gmail === 'connected') {
    showFeedbackToast({
      kind: 'success',
      title: 'Gmail connecte',
      message: 'Compte Gmail lie. Scan automatique lance.',
    })
  } else if (route.query?.gmail === 'error') {
    showFeedbackToast({
      kind: 'error',
      title: 'Connexion interrompue',
      message: 'La connexion Gmail a ete interrompue.',
    })
  }
  await scanNewestAccountAfterCallback()
  window.addEventListener('keydown', handleGlobalShortcut)
}

onMounted(bootstrapDelivery)
onBeforeUnmount(() => {
  clearFeedbackToast()
  window.removeEventListener('keydown', handleGlobalShortcut)
})
</script>

<style scoped>
.delivery-toast-layer {
  position: fixed;
  right: 1rem;
  bottom: calc(1rem + env(safe-area-inset-bottom, 0px));
  z-index: 110;
  width: min(100vw - 2rem, 26rem);
  pointer-events: none;
}

.delivery-toast {
  display: flex;
  align-items: flex-start;
  gap: 0.9rem;
  border: 1px solid rgba(148, 163, 184, 0.2);
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.97);
  padding: 0.95rem 1rem;
  box-shadow: 0 18px 44px rgba(15, 23, 42, 0.18);
  backdrop-filter: blur(14px);
}

.delivery-toast.is-success {
  border-color: rgba(16, 185, 129, 0.28);
  background: linear-gradient(145deg, rgba(255, 255, 255, 0.98), rgba(236, 253, 245, 0.96));
}

.delivery-toast.is-warning,
.delivery-toast.is-error {
  border-color: rgba(245, 158, 11, 0.28);
  background: linear-gradient(145deg, rgba(255, 255, 255, 0.98), rgba(255, 251, 235, 0.96));
}

.delivery-toast.is-error {
  border-color: rgba(248, 113, 113, 0.28);
  background: linear-gradient(145deg, rgba(255, 255, 255, 0.98), rgba(254, 242, 242, 0.97));
}

.delivery-toast__icon {
  display: inline-flex;
  height: 2.75rem;
  width: 2.75rem;
  flex: 0 0 auto;
  align-items: center;
  justify-content: center;
  border-radius: 16px;
  background: rgba(15, 118, 110, 0.1);
  color: #0f766e;
}

.delivery-toast.is-warning .delivery-toast__icon {
  background: rgba(245, 158, 11, 0.14);
  color: #b45309;
}

.delivery-toast.is-error .delivery-toast__icon {
  background: rgba(248, 113, 113, 0.14);
  color: #b91c1c;
}

.delivery-toast__copy {
  display: grid;
  gap: 0.15rem;
  min-width: 0;
}

.delivery-toast__copy strong {
  color: #0f172a;
  font-size: 0.95rem;
  font-weight: 800;
}

.delivery-toast__copy span {
  color: #475569;
  font-size: 0.85rem;
  line-height: 1.4;
}

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

@media (max-width: 640px) {
  .delivery-toast-layer {
    left: 1rem;
    right: 1rem;
    width: auto;
  }
}
</style>
