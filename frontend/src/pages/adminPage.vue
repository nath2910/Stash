<template>
  <div class="admin-page" :class="{ 'is-embedded': props.embedded }">
    <main class="admin-shell">
      <div v-if="error" class="notice danger" role="alert">
        <AlertTriangle class="notice-icon" aria-hidden="true" />
        <span>{{ error }}</span>
      </div>

      <div v-if="feedback" class="notice ok" role="status">
        <CheckCircle2 class="notice-icon" aria-hidden="true" />
        <span>{{ feedback }}</span>
      </div>

      <div v-if="loading" class="loading-box" aria-live="polite">
        <RefreshCw class="button-icon spinning" aria-hidden="true" />
        <span>Preparation du dossier administratif...</span>
      </div>

      <template v-else>
        <header class="admin-header">
          <div>
            <h1>Assistant declaration</h1>
            <p>
              <strong>{{ legalStatusLabel }}</strong>
              <span aria-hidden="true">/</span>
              <span>{{ periodDisplayLabel }}</span>
            </p>
          </div>

          <div class="header-actions">
            <div class="period-controls" aria-label="Periode administrative">
              <label>
                <span>Annee</span>
                <select v-model.number="periodYear" @change="handlePeriodChange">
                  <option v-for="year in yearOptions" :key="year" :value="year">{{ year }}</option>
                </select>
              </label>

              <label v-if="isQuarterlyDeclaration">
                <span>Trimestre</span>
                <select v-model.number="selectedQuarter" @change="handlePeriodChange">
                  <option v-for="quarter in quarterOptions" :key="quarter.value" :value="quarter.value">
                    {{ quarter.shortLabel }}
                  </option>
                </select>
              </label>

              <label v-else>
                <span>Mois</span>
                <select v-model.number="selectedMonth" @change="handlePeriodChange">
                  <option v-for="month in monthOptions" :key="month.value" :value="month.value">
                    {{ month.label }}
                  </option>
                </select>
              </label>
            </div>

            <button type="button" class="icon-button ghost" @click="openSettings">
              <Settings class="button-icon" aria-hidden="true" />
              <span>Parametres</span>
            </button>
          </div>
        </header>

        <section class="command-center" aria-labelledby="next-action-title">
          <article class="next-action-card">
            <div class="action-head">
              <div>
                <p class="section-kicker">A declarer</p>
                <h2 id="next-action-title">{{ nextAction.title }}</h2>
                <p>{{ nextAction.detail }}</p>
              </div>
              <span class="status-pill" :class="statusClass(declarationState)">
                {{ declarationStateLabel }}
              </span>
            </div>

            <div class="amount-block">
              <span>{{ declarationAmountLabel }}</span>
              <strong>{{ money(declarationSummary.amount) }}</strong>
              <p>{{ amountInstruction }}</p>
            </div>

            <div class="primary-actions">
              <button type="button" class="icon-button primary" @click="copyDeclarationAmount">
                <Copy class="button-icon" aria-hidden="true" />
                <span>{{ copiedKey === 'amount' ? 'Montant copie' : primaryCopyLabel }}</span>
              </button>

              <button
                v-if="isMicroProfile"
                type="button"
                class="icon-button secondary"
                :disabled="urssafDocumentRow?.disabled || generating === urssafDocumentRow?.documentType"
                @click="generateUrssafSheet"
              >
                <FileText class="button-icon" aria-hidden="true" />
                <span>{{ generating === urssafDocumentRow?.documentType ? 'Generation...' : 'Generer fiche URSSAF' }}</span>
              </button>

              <a
                v-if="isMicroProfile"
                class="icon-button secondary"
                href="https://www.autoentrepreneur.urssaf.fr/"
                target="_blank"
                rel="noreferrer"
              >
                <ExternalLink class="button-icon" aria-hidden="true" />
                <span>Ouvrir URSSAF</span>
              </a>

              <button
                v-if="isMicroProfile"
                type="button"
                class="icon-button secondary"
                :disabled="markingDeclaration || isDeclarationMarked"
                @click="markDeclarationDone"
              >
                <CheckCircle2 class="button-icon" aria-hidden="true" />
                <span>{{ isDeclarationMarked ? 'Declare' : 'Marquer declare' }}</span>
              </button>
            </div>
          </article>

          <aside class="declaration-side" aria-label="Etat de la declaration">
            <section class="profile-summary-card" aria-labelledby="profile-summary-title">
              <div class="side-card-head">
                <div>
                  <p class="section-kicker">Profil utilise</p>
                  <h2 id="profile-summary-title">{{ legalStatusLabel }}</h2>
                  <p class="profile-mode-line">{{ profileModeSentence }}</p>
                </div>
                <span class="status-pill" :class="statusClass(profileNeedsAttention ? 'incomplete' : 'complete')">
                  {{ profileNeedsAttention ? 'A completer' : 'OK' }}
                </span>
              </div>

              <dl class="profile-summary-list">
                <div v-for="row in profileSummaryRows" :key="row.label">
                  <dt>{{ row.label }}</dt>
                  <dd>{{ row.value }}</dd>
                </div>
              </dl>

              <button type="button" class="text-button" @click="openSettings">
                <Settings class="button-icon" aria-hidden="true" />
                <span>Modifier le profil</span>
              </button>
            </section>

            <section class="checklist-card" aria-labelledby="declaration-checklist-title">
              <p class="section-kicker">Parcours</p>
              <h2 id="declaration-checklist-title">Avant de declarer</h2>
              <div class="declaration-step-list">
                <button
                  v-for="step in declarationSteps"
                  :key="step.id"
                  type="button"
                  class="declaration-step"
                  :class="statusClass(step.status)"
                  :disabled="!step.action"
                  @click="handleDeclarationStep(step)"
                >
                  <span class="step-index">{{ step.index }}</span>
                  <span>
                    <strong>{{ step.title }}</strong>
                    <small>{{ step.detail }}</small>
                  </span>
                </button>
              </div>
            </section>
          </aside>
        </section>

        <section class="services-panel" aria-labelledby="services-title">
          <div class="section-heading">
            <div>
              <p class="section-kicker">{{ servicesKicker }}</p>
              <h2 id="services-title">{{ servicesTitle }}</h2>
            </div>
            <span class="status-pill" :class="statusClass(servicesStatus)">
              {{ servicesStatusLabel }}
            </span>
          </div>

          <div class="service-grid">
            <article
              v-for="service in administrativeServices"
              :key="service.id"
              class="service-card"
              :class="[statusClass(service.status), { 'is-primary-service': service.primary }]"
            >
              <div class="service-card-head">
                <span class="service-icon" :class="statusClass(service.status)">
                  <component :is="service.icon" class="button-icon" aria-hidden="true" />
                </span>
                <span class="status-pill" :class="statusClass(service.status)">
                  {{ service.badge }}
                </span>
              </div>

              <strong>{{ service.title }}</strong>
              <p>{{ service.detail }}</p>

              <div v-if="service.metrics?.length" class="service-metrics">
                <span v-for="metric in service.metrics" :key="metric">{{ metric }}</span>
              </div>

              <button
                type="button"
                class="icon-button"
                :class="service.primary ? 'primary' : 'secondary'"
                :disabled="service.disabled || service.busy"
                @click="handleAdministrativeService(service)"
              >
                <component :is="service.actionIcon || service.icon" class="button-icon" aria-hidden="true" />
                <span>{{ service.busy ? service.busyLabel : service.actionLabel }}</span>
              </button>
            </article>
          </div>
        </section>

        <section class="copy-workbench" aria-labelledby="copy-workbench-title">
          <div class="section-heading">
            <div>
              <p class="section-kicker">{{ copyWorkbenchKicker }}</p>
              <h2 id="copy-workbench-title">{{ copyWorkbenchTitle }}</h2>
            </div>
          </div>

          <div class="copy-grid">
            <article
              v-for="field in declarationFields"
              :key="field.id"
              class="copy-field"
              :class="{ 'is-missing': field.missing }"
            >
              <span>{{ field.label }}</span>
              <strong>{{ field.value }}</strong>
              <p v-if="field.detail">{{ field.detail }}</p>
              <button
                v-if="field.copyValue"
                type="button"
                class="mini-copy-button"
                :class="{ 'is-copied': copiedKey === field.id }"
                @click="copyDeclarationField(field)"
              >
                <Copy class="button-icon" aria-hidden="true" />
                <span>{{ copiedKey === field.id ? 'Copie' : 'Copier' }}</span>
              </button>
            </article>
          </div>
        </section>

        <section v-if="visibleBlockingIssues.length" class="blockers-panel" aria-labelledby="blockers-title">
          <div class="section-heading">
            <div>
              <p class="section-kicker">Ce qui bloque</p>
              <h2 id="blockers-title">{{ visibleBlockingIssues.length }} point(s) a corriger</h2>
            </div>
          </div>

          <div class="issue-list">
            <article v-for="issue in visibleBlockingIssues" :key="issue.id" class="issue-card">
              <div>
                <strong>{{ issue.title }}</strong>
                <p>{{ issue.message }}</p>
              </div>
              <button type="button" class="text-button" @click="handleIssueAction(issue)">
                <span>{{ issueActionLabel(issue) }}</span>
                <ArrowRight class="button-icon" aria-hidden="true" />
              </button>
            </article>
          </div>
        </section>

        <p v-else class="ready-line">
          <ShieldCheck class="button-icon" aria-hidden="true" />
          <span>Donnees pretes pour cette periode.</span>
        </p>

        <div class="progressive-panels" aria-label="Details administratifs">
          <details class="simple-panel disclosure-panel" :open="profileNeedsAttention || Boolean(hardBlockingCount)">
            <summary>
              <div>
                <p class="section-kicker">Reglage services</p>
                <h2>{{ profilePlanTitle }}</h2>
              </div>
              <span class="status-pill" :class="statusClass(profilePlanStatus)">
                {{ profilePlanStatusLabel }}
              </span>
            </summary>

            <div class="disclosure-body">
              <div class="profile-plan-list">
                <article
                  v-for="item in profileActionItems"
                  :key="item.id"
                  class="profile-plan-row"
                  :class="statusClass(item.status)"
                >
                  <div class="profile-plan-copy">
                    <span class="status-pill" :class="statusClass(item.status)">
                      {{ statusLabel(item.status) }}
                    </span>
                    <strong>{{ item.title }}</strong>
                    <p>{{ item.description }}</p>
                  </div>

                  <button
                    v-if="item.action"
                    type="button"
                    class="text-button"
                    :disabled="item.disabled || item.busy"
                    @click="handleProfilePlanAction(item)"
                  >
                    <span>{{ profilePlanActionLabel(item) }}</span>
                    <ArrowRight class="button-icon" aria-hidden="true" />
                  </button>
                </article>
              </div>
            </div>
          </details>

          <details class="simple-panel disclosure-panel">
            <summary>
              <div>
                <p class="section-kicker">Controle</p>
                <h2>Donnees utilisees</h2>
              </div>
              <span class="status-pill" :class="statusClass(dataPanelStatus)">
                {{ dataPanelStatusLabel }}
              </span>
            </summary>

            <div class="disclosure-body">
              <div class="data-summary-grid">
                <div v-for="row in dataSummaryRows" :key="row.label" class="data-summary-item">
                  <span>{{ row.label }}</span>
                  <strong>{{ row.value }}</strong>
                  <p v-if="row.detail">{{ row.detail }}</p>
                </div>
              </div>

              <p v-if="summary?.revenueRule" class="calculation-note">
                {{ summary.revenueRule }}
              </p>

              <div class="quality-list">
                <article
                  v-for="item in dataQualityItems"
                  :key="item.id"
                  class="quality-row"
                  :class="statusClass(item.status)"
                >
                  <div>
                    <strong>{{ item.title }}</strong>
                    <p>{{ item.message }}</p>
                  </div>

                  <button
                    v-if="item.action"
                    type="button"
                    class="text-button"
                    :disabled="item.busy"
                    @click="handleQualityAction(item)"
                  >
                    <span>{{ qualityActionLabel(item) }}</span>
                    <ArrowRight class="button-icon" aria-hidden="true" />
                  </button>
                </article>
              </div>

              <div v-if="sourceRows.length" class="source-list" aria-label="Sources administratives">
                <span>Sources</span>
                <a
                  v-for="source in sourceRows"
                  :key="source.label"
                  :href="source.url"
                  target="_blank"
                  rel="noreferrer"
                >
                  {{ source.label }}
                  <ExternalLink class="button-icon" aria-hidden="true" />
                </a>
              </div>
            </div>
          </details>
        </div>

        <details class="simple-panel optional-panel disclosure-panel">
          <summary>
            <div>
              <p class="section-kicker">Optionnel</p>
              <h2 id="legal-folder-title">Exports et documents</h2>
            </div>
            <span class="status-pill is-neutral">{{ legalFolderRows.length }} document(s)</span>
          </summary>

          <div class="optional-panel-body">
            <button
              v-if="missingInvoices.length"
              type="button"
              class="icon-button secondary"
              :disabled="generatingInvoices"
              @click="generateMissingInvoices"
            >
              <ReceiptText class="button-icon" aria-hidden="true" />
              <span>{{ generatingInvoices ? 'Generation...' : invoiceActionLabel }}</span>
            </button>

            <div class="document-grid">
              <article v-for="row in legalFolderRows" :key="row.id" class="document-card">
                <div>
                  <span class="document-type">{{ row.format || row.type || documentFormat(row) }}</span>
                  <h3>{{ row.name }}</h3>
                  <p>{{ row.period || periodDisplayLabel }}</p>
                </div>

                <div class="document-footer">
                  <span class="status-pill" :class="statusClass(row.status)">
                    {{ simpleDocumentStatus(row) }}
                  </span>
                  <button
                    type="button"
                    class="text-button"
                    :disabled="row.disabled || generating === row.documentType"
                    :title="row.missingReason"
                    @click="generateExportRow(row)"
                  >
                    <Download class="button-icon" aria-hidden="true" />
                    <span>{{ generating === row.documentType ? 'Generation...' : documentActionLabel(row) }}</span>
                  </button>
                </div>
              </article>
            </div>
          </div>
        </details>

        <details ref="settingsPanelRef" class="settings-panel" :open="settingsOpen">
          <summary @click.prevent="toggleSettings">
            <span>
              <Settings class="button-icon" aria-hidden="true" />
              Parametres administratifs
            </span>
            <span class="status-pill" :class="statusClass(profileNeedsAttention ? 'incomplete' : 'complete')">
              {{ profileNeedsAttention ? 'A completer' : 'Pret' }}
            </span>
          </summary>

          <form class="profile-form" @submit.prevent="saveProfile">
            <div v-if="profileEditError" class="notice danger" role="alert">
              <AlertTriangle class="notice-icon" aria-hidden="true" />
              <span>{{ profileEditError }}</span>
            </div>

            <div class="form-grid">
              <label>
                <span>Statut juridique</span>
                <select v-model="profileForm.legalStatus" @change="handleProfileStatusChange">
                  <option v-for="option in legalStatusOptions" :key="option.value" :value="option.value">
                    {{ option.label }}
                  </option>
                </select>
              </label>

              <label>
                <span>SIRET</span>
                <input v-model.trim="profileForm.siret" inputmode="numeric" maxlength="14" placeholder="14 chiffres" />
                <small v-if="profileSiretMessage">{{ profileSiretMessage }}</small>
              </label>

              <label>
                <span>Nom commercial</span>
                <input v-model.trim="profileForm.businessName" placeholder="MyStash Shop" />
              </label>

              <label>
                <span>Nom du dirigeant</span>
                <input v-model.trim="profileForm.ownerName" placeholder="Nom prenom" />
              </label>

              <label>
                <span>Regime TVA</span>
                <select v-model="profileForm.vatRegime">
                  <option v-for="option in vatOptions" :key="option.value" :value="option.value">
                    {{ option.label }}
                  </option>
                </select>
              </label>

              <label>
                <span>Periodicite URSSAF</span>
                <select
                  v-model="profileForm.declarationFrequency"
                  :disabled="!profileFormIsMicro"
                  @change="handleProfileFrequencyChange"
                >
                  <option value="UNKNOWN">A verifier</option>
                  <option value="MONTHLY">Mensuelle</option>
                  <option value="QUARTERLY">Trimestrielle</option>
                </select>
              </label>

              <label>
                <span>Date de debut d'activite</span>
                <input v-model="profileForm.activityStartDate" type="date" />
              </label>

              <label>
                <span>Rubrique URSSAF</span>
                <input
                  v-model.trim="profileForm.urssafCategory"
                  :disabled="!profileFormIsMicro"
                  placeholder="Vente de marchandises"
                />
              </label>

              <label class="span-2">
                <span>Adresse entreprise</span>
                <textarea v-model.trim="profileForm.address" rows="3" placeholder="Adresse utilisee sur les documents"></textarea>
              </label>
            </div>

            <div class="form-actions">
              <button type="button" class="icon-button secondary" @click="resetProfileForm">
                <RefreshCw class="button-icon" aria-hidden="true" />
                <span>Recharger</span>
              </button>
              <button type="submit" class="icon-button primary" :disabled="profileSaving || Boolean(profileSiretMessage)">
                <CheckCircle2 class="button-icon" aria-hidden="true" />
                <span>{{ profileSaving ? 'Enregistrement...' : 'Enregistrer' }}</span>
              </button>
            </div>
          </form>
        </details>
      </template>
    </main>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  AlertTriangle,
  ArrowRight,
  CheckCircle2,
  Copy,
  Download,
  ExternalLink,
  FileText,
  ReceiptText,
  RefreshCw,
  Settings,
  ShieldCheck,
} from 'lucide-vue-next'
import AdminService from '@/services/AdminService'
import {
  ADMIN_LEGAL_STATUS_OPTIONS,
  ADMIN_VAT_OPTIONS,
  defaultActivitiesForStatus,
  defaultDeclarationsForStatus,
  getDeclarationLabel,
  getLegalStatusLabel,
  getVatRegimeLabel,
  isCompanyStatus,
  isMicroStatus,
  normalizeAdministrativeProfile,
} from '@/constants/adminModule'
import {
  getAdministrativeProfile,
  getBlockingIssues,
  getDeclarationFields,
  getDeclarationSummary,
  getDeclarativePeriod,
  getDocumentRows,
  shouldUseQuarterPeriod,
} from '@/rules/administrativeRules'

const props = defineProps({
  embedded: {
    type: Boolean,
    default: false,
  },
})

const router = useRouter()
const GENERATED_DOCUMENTS_STORAGE_KEY = 'stash_admin_generated_documents_v1'
const STATUS_LABELS = Object.freeze({
  complete: 'Pret',
  checked: 'Pret',
  ready: 'Pret',
  to_verify: 'A verifier',
  incomplete: 'Bloque',
  pending: 'A preparer',
  draft: 'Brouillon',
  archived: 'Archive',
  not_applicable: 'N/A',
  data_missing: 'Bloque',
  regenerable: 'A generer',
  generated: 'Pret',
  done: 'Fait',
})

const today = new Date()
const initialYear = today.getFullYear()

const loading = ref(true)
const summaryLoading = ref(false)
const generating = ref('')
const generatingService = ref('')
const generatingInvoices = ref(false)
const markingDeclaration = ref(false)
const error = ref('')
const feedback = ref('')
const copiedKey = ref('')
const profile = ref(null)
const summary = ref(null)
const documents = ref([])
const invoices = ref([])
const documentRecords = ref([])
const generatedDocuments = ref(readGeneratedDocuments())
const periodTouched = ref(false)
const periodYear = ref(initialYear)
const selectedMonth = ref(today.getMonth() + 1)
const selectedQuarter = ref(Math.floor(today.getMonth() / 3) + 1)
const profileSaving = ref(false)
const profileEditError = ref('')
const settingsOpen = ref(false)
const settingsPanelRef = ref(null)
const profileForm = reactive({
  legalStatus: 'none',
  activities: [],
  vatRegime: 'unknown',
  declarations: [],
  siret: '',
  businessName: '',
  ownerName: '',
  tradeName: '',
  address: '',
  legalForm: '',
  mainActivity: '',
  fiscalRegime: '',
  declarationFrequency: 'UNKNOWN',
  declarationPeriodicity: '',
  withholdingTaxOption: 'UNKNOWN',
  vatFranchise: 'UNKNOWN',
  activityStartDate: '',
  fiscalYearEndMonth: 12,
  fiscalYearEndDay: 31,
  urssafCategory: 'Vente de marchandises',
  notes: '',
  verificationStatus: 'A_VERIFIER',
  usesOnlinePlatforms: false,
  buysForResale: false,
})
let copiedResetTimer = 0

const legalStatusOptions = ADMIN_LEGAL_STATUS_OPTIONS
const vatOptions = ADMIN_VAT_OPTIONS
const normalizedProfile = computed(() => normalizeAdministrativeProfile(profile.value || {}))
const administrativeProfile = computed(() => getAdministrativeProfile(normalizedProfile.value))
const isMicroProfile = computed(() => isMicroStatus(normalizedProfile.value.legalStatus))
const isCompanyProfile = computed(() => administrativeProfile.value.family === 'company')
const profileFormIsMicro = computed(() => isMicroStatus(profileForm.legalStatus))
const profileFormIsBusiness = computed(() =>
  profileFormIsMicro.value || isCompanyStatus(profileForm.legalStatus) || profileForm.legalStatus === 'ei_real',
)
const profileFormRequiresSiret = computed(() => profileFormIsMicro.value || profileFormIsBusiness.value)
const profileSiretMessage = computed(() => {
  if (!profileForm.siret) return ''
  return /^\d{14}$/.test(profileForm.siret) ? '' : 'Le SIRET doit contenir exactement 14 chiffres.'
})
const legalStatusLabel = computed(() => getLegalStatusLabel(normalizedProfile.value.legalStatus))
const declarationPeriodicityLabel = computed(() => {
  if (normalizedProfile.value.declarationFrequency === 'MONTHLY') return 'Mensuelle'
  if (normalizedProfile.value.declarationFrequency === 'QUARTERLY') return 'Trimestrielle'
  return getDeclarationLabel(normalizedProfile.value.declarationPeriodicity) || 'A verifier'
})

const isQuarterlyDeclaration = computed(() => shouldUseQuarterPeriod(normalizedProfile.value))
const selectedYear = computed(() => Number(periodYear.value) || initialYear)
const periodRange = computed(() => {
  const year = selectedYear.value
  if (isQuarterlyDeclaration.value) {
    const quarter = clampNumber(selectedQuarter.value, 1, 4)
    const startMonth = (quarter - 1) * 3
    return {
      start: dateToInputValue(new Date(year, startMonth, 1)),
      end: dateToInputValue(new Date(year, startMonth + 3, 0)),
    }
  }

  const month = clampNumber(selectedMonth.value, 1, 12)
  const startMonth = month - 1
  return {
    start: dateToInputValue(new Date(year, startMonth, 1)),
    end: dateToInputValue(new Date(year, startMonth + 1, 0)),
  }
})
const periodStart = computed(() => periodRange.value.start)
const periodEnd = computed(() => periodRange.value.end)
const declarativePeriod = computed(() => getDeclarativePeriod(normalizedProfile.value, periodStart.value, periodEnd.value))
const periodDisplayLabel = computed(() => declarativePeriod.value.label || `${formatDate(periodStart.value)} - ${formatDate(periodEnd.value)}`)
const allRecords = computed(() => mergeRecords([...documentRecords.value, ...generatedDocuments.value]))
const blockingIssues = computed(() => getBlockingIssues(normalizedProfile.value, summary.value || {}, declarativePeriod.value))
const visibleBlockingIssues = computed(() =>
  blockingIssues.value.filter((issue) => issue.id !== 'no-sales' && issue.severity !== 'info'),
)
const hardBlockingCount = computed(() => visibleBlockingIssues.value.filter((issue) => issue.severity === 'danger').length)
const declarationSummary = computed(() => getDeclarationSummary(normalizedProfile.value, summary.value || {}, declarativePeriod.value))
const declarationFields = computed(() => getDeclarationFields(normalizedProfile.value, summary.value || {}, declarativePeriod.value))
const administrativeDocumentRows = computed(() => getDocumentRows(
  normalizedProfile.value,
  summary.value || {},
  declarativePeriod.value,
  documents.value,
  allRecords.value,
))
const legalFolderRows = computed(() => {
  const wanted = ['urssaf-summary', 'receipts-register', 'purchases-register', 'accounting-export-csv', 'fiscal-summary']
  return wanted
    .map((id) => administrativeDocumentRows.value.find((row) => row.id === id || row.documentType === id))
    .filter(Boolean)
})
const urssafDocumentRow = computed(() =>
  administrativeDocumentRows.value.find((row) => row.id === 'urssaf-summary' || row.documentType === 'urssaf-summary'),
)
const invoiceSaleIds = computed(() =>
  new Set(invoices.value
    .map((invoice) => invoice?.relatedSaleId || invoice?.saleId || invoice?.venteId)
    .filter((id) => id !== undefined && id !== null && String(id).trim() !== '')
    .map((id) => String(id))),
)
const missingInvoices = computed(() =>
  (summary.value?.sales || []).filter((sale) => sale?.id && !invoiceSaleIds.value.has(String(sale.id))),
)
const invoiceActionLabel = computed(() => {
  const count = missingInvoices.value.length
  return count ? `Generer ${number(count)} facture(s)` : 'Factures a jour'
})
const isDeclarationMarked = computed(() =>
  allRecords.value.some((record) =>
    recordType(record) === 'urssaf-declaration'
      && record.periodStart === periodStart.value
      && record.periodEnd === periodEnd.value,
  ),
)
const declarationState = computed(() => {
  if (hardBlockingCount.value) return 'data_missing'
  if (isDeclarationMarked.value) return 'done'
  return declarationSummary.value.status || 'ready'
})
const declarationStateLabel = computed(() => {
  if (isDeclarationMarked.value) return 'Declare'
  return statusLabel(declarationState.value)
})
const declarationAmountLabel = computed(() => {
  if (isMicroProfile.value) return 'CA encaisse a declarer'
  if (isCompanyProfile.value) return 'CA encaisse sur la periode'
  return 'Total encaisse'
})
const primaryCopyLabel = computed(() => (isMicroProfile.value ? 'Copier le CA URSSAF' : 'Copier le montant'))
const copyWorkbenchKicker = computed(() => {
  if (isMicroProfile.value) return 'Champs a recopier'
  if (isCompanyProfile.value) return 'Preparation comptable'
  if (administrativeProfile.value.family === 'personal') return 'Recapitulatif ventes'
  return 'Diagnostic administratif'
})
const copyWorkbenchTitle = computed(() => {
  if (isMicroProfile.value) return 'Saisie URSSAF preparee'
  if (isCompanyProfile.value) return 'Montants pre-comptables'
  if (administrativeProfile.value.family === 'personal') return 'Donnees de ventes preparees'
  return 'Donnees a qualifier'
})
const servicesKicker = computed(() => {
  if (isMicroProfile.value) return 'Services micro'
  if (isCompanyProfile.value) return 'Services compta'
  if (administrativeProfile.value.family === 'personal') return 'Services recap'
  return 'Services de mise au propre'
})
const servicesTitle = computed(() => {
  if (isMicroProfile.value) return 'Ce que MyStash peut faire maintenant'
  if (isCompanyProfile.value) return 'Preparer le dossier pour la compta'
  if (administrativeProfile.value.family === 'personal') return 'Sortir un recap propre'
  return 'Remettre le dossier en etat'
})
const administrativeServices = computed(() => {
  const services = []
  const amount = money(declarationSummary.value.amount)
  const sales = `${number(summary.value?.periodSaleCount)} vente(s)`
  const hasHardBlocks = Boolean(hardBlockingCount.value)
  const hasIssues = Boolean(visibleBlockingIssues.value.length)
  const missingInvoiceCount = Number(summary.value?.missingInvoiceCount ?? missingInvoices.value.length)

  if (isMicroProfile.value) {
    services.push(serviceCard({
      id: 'prepare-urssaf',
      icon: FileText,
      actionIcon: FileText,
      title: 'Preparer ma declaration URSSAF',
      detail: hasHardBlocks
        ? 'Corrige d abord les ventes qui empechent un montant fiable.'
        : profileNeedsAttention.value
          ? 'Complete le profil, puis MyStash prepare la fiche et le montant a saisir.'
          : 'Copie le CA, genere la fiche URSSAF et garde la periode prete a archiver.',
      status: profileNeedsAttention.value || hasHardBlocks ? 'incomplete' : isDeclarationMarked.value ? 'complete' : 'ready',
      badge: isDeclarationMarked.value ? 'Archive' : profileNeedsAttention.value || hasHardBlocks ? 'A finir' : 'Pret',
      action: profileNeedsAttention.value ? 'profile' : hasHardBlocks ? 'fix-sales' : 'prepare-urssaf',
      actionLabel: profileNeedsAttention.value ? 'Completer profil' : hasHardBlocks ? 'Corriger ventes' : 'Lancer',
      metrics: [amount, sales, declarationPeriodicityLabel.value],
      primary: true,
    }))
    services.push(serviceCard({
      id: 'missing-invoices',
      icon: ReceiptText,
      actionIcon: ReceiptText,
      title: 'Creer les factures manquantes',
      detail: missingInvoiceCount
        ? 'Genere les factures absentes pour les ventes encaissees de la periode.'
        : 'Toutes les ventes detectees ont deja une facture rattachee.',
      status: missingInvoiceCount ? 'to_verify' : 'complete',
      badge: missingInvoiceCount ? `${number(missingInvoiceCount)} a creer` : 'A jour',
      action: missingInvoiceCount ? 'missing-invoices' : 'refresh',
      actionLabel: missingInvoiceCount ? 'Generer' : 'Recontroler',
      metrics: [`${number(missingInvoiceCount)} facture(s)`],
      disabled: generatingInvoices.value,
    }))
    services.push(serviceCard({
      id: 'micro-pack',
      icon: Download,
      actionIcon: Download,
      title: 'Generer le dossier complet',
      detail: 'Produit la fiche URSSAF, le livre des recettes, le registre achats et les exports utiles disponibles.',
      status: hasHardBlocks ? 'data_missing' : 'ready',
      badge: hasHardBlocks ? 'Bloque' : 'Pack',
      action: hasHardBlocks ? 'fix-sales' : 'bundle',
      actionLabel: hasHardBlocks ? 'Corriger ventes' : 'Generer pack',
      metrics: [`${legalFolderRows.value.length} document(s)`],
      documentTypes: ['urssaf-summary', 'receipts-register', 'purchases-register', 'accounting-export-csv', 'fiscal-summary'],
    }))
    services.push(serviceCard({
      id: 'mark-declared',
      icon: CheckCircle2,
      actionIcon: CheckCircle2,
      title: 'Archiver la periode declaree',
      detail: 'Apres validation officielle, marque la periode comme faite dans MyStash.',
      status: isDeclarationMarked.value ? 'complete' : 'pending',
      badge: isDeclarationMarked.value ? 'Fait' : 'A faire',
      action: 'mark-declared',
      actionLabel: isDeclarationMarked.value ? 'Deja archive' : 'Marquer fait',
      metrics: [periodDisplayLabel.value],
      disabled: isDeclarationMarked.value,
    }))
    return services
  }

  if (isCompanyProfile.value) {
    services.push(serviceCard({
      id: 'company-pack',
      icon: Download,
      actionIcon: Download,
      title: 'Generer le dossier comptable',
      detail: hasHardBlocks
        ? 'Les exports comptables attendent la correction des ventes bloquantes.'
        : 'Genere CSV comptable, registres ventes/achats et recap de resultat.',
      status: profileNeedsAttention.value || hasHardBlocks ? 'incomplete' : 'ready',
      badge: profileNeedsAttention.value || hasHardBlocks ? 'A finir' : 'Pret',
      action: profileNeedsAttention.value ? 'profile' : hasHardBlocks ? 'fix-sales' : 'bundle',
      actionLabel: profileNeedsAttention.value ? 'Completer profil' : hasHardBlocks ? 'Corriger ventes' : 'Generer pack',
      metrics: [amount, sales, money(summary.value?.periodPurchaseTotal)],
      documentTypes: ['accounting-export-csv', 'receipts-register', 'purchases-register', 'fiscal-summary'],
      primary: true,
    }))
    services.push(serviceCard({
      id: 'company-invoices',
      icon: ReceiptText,
      actionIcon: ReceiptText,
      title: 'Creer les factures clients',
      detail: missingInvoiceCount
        ? 'Genere les factures de vente manquantes avant transmission comptable.'
        : 'Les factures de la periode sont a jour.',
      status: missingInvoiceCount ? 'to_verify' : 'complete',
      badge: missingInvoiceCount ? `${number(missingInvoiceCount)} a creer` : 'A jour',
      action: missingInvoiceCount ? 'missing-invoices' : 'refresh',
      actionLabel: missingInvoiceCount ? 'Generer' : 'Recontroler',
      metrics: [`${number(missingInvoiceCount)} facture(s)`],
    }))
    services.push(serviceCard({
      id: 'company-control',
      icon: ShieldCheck,
      actionIcon: hasIssues ? ArrowRight : RefreshCw,
      title: 'Controler les donnees comptables',
      detail: hasIssues
        ? 'Va directement aux ventes ou parametres qui empechent un dossier propre.'
        : 'Relance le calcul pour verifier que la periode reste propre.',
      status: hasIssues ? 'to_verify' : 'complete',
      badge: hasIssues ? `${number(visibleBlockingIssues.value.length)} point(s)` : 'OK',
      action: hasIssues ? 'first-issue' : 'refresh',
      actionLabel: hasIssues ? 'Corriger' : 'Recalculer',
      metrics: [`${number(summary.value?.missingProofCount)} justificatif(s) manquant(s)`],
    }))
    return services
  }

  if (administrativeProfile.value.family === 'personal') {
    services.push(serviceCard({
      id: 'personal-recap',
      icon: FileText,
      actionIcon: Download,
      title: 'Generer un recap de ventes',
      detail: 'Sort un document simple des ventes de la periode pour garder une trace propre.',
      status: hasHardBlocks ? 'data_missing' : 'ready',
      badge: hasHardBlocks ? 'Bloque' : 'Pret',
      action: hasHardBlocks ? 'fix-sales' : 'bundle',
      actionLabel: hasHardBlocks ? 'Corriger ventes' : 'Generer recap',
      metrics: [amount, sales],
      documentTypes: ['receipts-register'],
      primary: true,
    }))
    services.push(serviceCard({
      id: 'personal-status',
      icon: ShieldCheck,
      actionIcon: Settings,
      title: 'Verifier le cadre de vente',
      detail: 'Ajuste le profil si les ventes ressemblent a une activite d achat-revente.',
      status: summary.value?.individualDiagnosticLevel && summary.value.individualDiagnosticLevel !== 'ok' ? 'to_verify' : 'complete',
      badge: summary.value?.individualDiagnosticLabel || 'OK',
      action: 'profile',
      actionLabel: 'Voir profil',
      metrics: [periodDisplayLabel.value],
    }))
    return services
  }

  services.push(serviceCard({
    id: 'regularize-profile',
    icon: Settings,
    actionIcon: Settings,
    title: 'Choisir le bon profil',
    detail: 'Debloque les services utiles en indiquant si tu es particulier, micro ou societe.',
    status: 'incomplete',
    badge: 'Requis',
    action: 'profile',
    actionLabel: 'Configurer',
    metrics: [legalStatusLabel.value],
    primary: true,
  }))
  services.push(serviceCard({
    id: 'regularize-sales',
    icon: ShieldCheck,
    actionIcon: ArrowRight,
    title: 'Nettoyer les ventes',
    detail: 'Corrige dates, prix et pieces avant de preparer une declaration.',
    status: hasIssues ? 'to_verify' : 'complete',
    badge: hasIssues ? `${number(visibleBlockingIssues.value.length)} point(s)` : 'OK',
    action: hasIssues ? 'first-issue' : 'refresh',
    actionLabel: hasIssues ? 'Corriger' : 'Recalculer',
    metrics: [sales],
  }))

  return services
})
const servicesStatus = computed(() => {
  if (administrativeServices.value.some((service) => service.status === 'incomplete' || service.status === 'data_missing')) return 'incomplete'
  if (administrativeServices.value.some((service) => service.status === 'to_verify' || service.status === 'pending')) return 'to_verify'
  return 'complete'
})
const servicesStatusLabel = computed(() => {
  if (servicesStatus.value === 'complete') return 'Services prets'
  if (servicesStatus.value === 'incomplete') return 'A debloquer'
  return 'Action possible'
})
const profileModeSentence = computed(() => {
  if (isMicroProfile.value) return 'Priorite URSSAF, factures et registres en support.'
  if (isCompanyProfile.value) return 'Priorite exports comptables, TVA et justificatifs.'
  if (administrativeProfile.value.family === 'personal') return 'Priorite recap ventes et verification du cadre.'
  return 'Choisir le bon statut avant de declarer.'
})
const profilePlanTitle = computed(() => {
  if (isMicroProfile.value) return 'Plan micro-entreprise'
  if (isCompanyProfile.value) return 'Plan comptable'
  if (administrativeProfile.value.family === 'personal') return 'Plan particulier'
  return 'Plan de regularisation'
})
const profileActionItems = computed(() => {
  const obligations = Array.isArray(summary.value?.obligations) && summary.value.obligations.length
    ? summary.value.obligations
    : fallbackProfileObligations()

  return obligations.map((obligation) => normalizeProfileAction(obligation))
})
const profilePlanStatus = computed(() => {
  if (profileActionItems.value.some((item) => item.status === 'incomplete' || item.status === 'data_missing')) return 'incomplete'
  if (profileActionItems.value.some((item) => item.status === 'to_verify' || item.status === 'regenerable')) return 'to_verify'
  return 'complete'
})
const profilePlanStatusLabel = computed(() => {
  if (profilePlanStatus.value === 'complete') return 'Pret'
  if (profilePlanStatus.value === 'incomplete') return 'A completer'
  return 'A verifier'
})
const dataSummaryRows = computed(() => {
  const revenue = Number(summary.value?.periodRevenue || 0)
  const purchases = Number(summary.value?.periodPurchaseTotal || 0)
  const rows = [
    {
      label: 'CA periode',
      value: money(revenue),
      detail: `${number(summary.value?.periodSaleCount)} vente(s) retenue(s)`,
    },
    {
      label: 'CA annuel connu',
      value: money(summary.value?.annualRevenue),
      detail: `${number(summary.value?.annualSaleCount)} vente(s) sur l annee`,
    },
  ]

  if (isCompanyProfile.value) {
    rows.push(
      {
        label: 'Achats periode',
        value: money(purchases),
        detail: `${number(summary.value?.periodPurchaseCount)} achat(s) rattache(s)`,
      },
      {
        label: 'Resultat estime',
        value: money(revenue - purchases),
        detail: 'Avant validation comptable',
      },
    )
  } else if (isMicroProfile.value) {
    rows.push(
      {
        label: 'Periodicite',
        value: declarationPeriodicityLabel.value,
        detail: normalizedProfile.value.urssafCategory || 'Rubrique a verifier',
      },
      {
        label: 'Seuils',
        value: thresholdWatchLabel.value,
        detail: 'Surveillance annuelle indicative',
      },
    )
  } else {
    rows.push(
      {
        label: 'Diagnostic',
        value: summary.value?.individualDiagnosticLabel || 'A verifier',
        detail: summary.value?.individualDiagnosticMessage || 'Cadre personnel ou activite a qualifier',
      },
    )
  }

  rows.push(
    {
      label: 'Factures manquantes',
      value: number(summary.value?.missingInvoiceCount ?? missingInvoices.value.length),
      detail: isMicroProfile.value || isCompanyProfile.value ? 'Generables depuis les ventes' : 'Non prioritaire pour ce profil',
    },
    {
      label: 'Justificatifs',
      value: number(summary.value?.missingProofCount),
      detail: 'Pieces detectees comme manquantes',
    },
  )

  return rows
})
const dataQualityItems = computed(() => {
  const checks = Array.isArray(summary.value?.qualityChecks) ? summary.value.qualityChecks : []
  if (!checks.length) {
    return [{
      id: 'data-clean',
      title: 'Controle des donnees',
      message: 'Aucun point bloquant detecte sur cette periode.',
      status: 'complete',
      action: '',
    }]
  }

  return checks.map((check) => normalizeQualityItem(check))
})
const dataPanelStatus = computed(() => {
  if (dataQualityItems.value.some((item) => item.status === 'incomplete' || item.status === 'data_missing')) return 'incomplete'
  if (dataQualityItems.value.some((item) => item.status === 'to_verify' || item.status === 'regenerable')) return 'to_verify'
  return 'complete'
})
const dataPanelStatusLabel = computed(() => {
  if (dataPanelStatus.value === 'complete') return 'OK'
  if (dataPanelStatus.value === 'incomplete') return 'A corriger'
  return 'A verifier'
})
const sourceRows = computed(() =>
  (Array.isArray(summary.value?.sources) ? summary.value.sources : [])
    .filter((source) => source?.label && source?.url)
    .slice(0, 3),
)
const thresholdWatchLabel = computed(() => {
  const annualRevenue = Number(summary.value?.annualRevenue || 0)
  if (annualRevenue >= 203100) return 'Seuil micro depasse'
  if (annualRevenue >= 85000) return 'TVA a surveiller'
  return 'OK'
})
const profileSummaryRows = computed(() => {
  if (isMicroProfile.value) {
    return [
      { label: 'SIRET', value: normalizedProfile.value.siret || 'A completer' },
      { label: 'Periodicite', value: declarationPeriodicityLabel.value },
      { label: 'Rubrique', value: normalizedProfile.value.urssafCategory || 'A verifier' },
    ]
  }

  if (isCompanyProfile.value) {
    return [
      { label: 'SIRET', value: normalizedProfile.value.siret || 'A completer' },
      { label: 'TVA', value: getVatRegimeLabel(normalizedProfile.value.vatRegime) },
      { label: 'Cloture', value: `${number(normalizedProfile.value.fiscalYearEndDay)}/${number(normalizedProfile.value.fiscalYearEndMonth)}` },
    ]
  }

  return [
    { label: 'Statut', value: legalStatusLabel.value },
    { label: 'Periode', value: periodDisplayLabel.value },
    { label: 'Ventes', value: `${number(summary.value?.periodSaleCount)} retenue(s)` },
  ]
})
const declarationSteps = computed(() => [
  {
    id: 'profile',
    index: 1,
    title: 'Profil',
    detail: profileNeedsAttention.value ? 'A completer' : `${legalStatusLabel.value}, ${declarationPeriodicityLabel.value}`,
    status: profileNeedsAttention.value ? 'incomplete' : 'complete',
    action: 'profile',
  },
  {
    id: 'data',
    index: 2,
    title: 'Donnees',
    detail: visibleBlockingIssues.value.length
      ? `${number(visibleBlockingIssues.value.length)} point(s) a corriger`
      : `${number(summary.value?.periodSaleCount)} vente(s) retenue(s)`,
    status: visibleBlockingIssues.value.length ? 'incomplete' : 'complete',
    action: visibleBlockingIssues.value.length ? 'issues' : '',
  },
  {
    id: 'amount',
    index: 3,
    title: 'Montant',
    detail: isDeclarationMarked.value ? 'Declaration marquee faite' : `${money(declarationSummary.value.amount)} pret a copier`,
    status: hardBlockingCount.value ? 'incomplete' : isDeclarationMarked.value ? 'complete' : 'to_verify',
    action: hardBlockingCount.value ? '' : 'copy',
  },
  {
    id: 'archive',
    index: 4,
    title: 'Archive',
    detail: isDeclarationMarked.value ? 'Periode archivee' : 'A marquer apres validation',
    status: isDeclarationMarked.value ? 'complete' : 'pending',
    action: isMicroProfile.value && !isDeclarationMarked.value && !hardBlockingCount.value ? 'mark-done' : '',
  },
])
const nextAction = computed(() => {
  const amount = money(declarationSummary.value.amount)
  const sales = number(summary.value?.periodSaleCount)
  const category = normalizedProfile.value.urssafCategory || 'Vente de marchandises'

  if (visibleBlockingIssues.value.length) {
    return {
      title: 'Corriger les blocages avant declaration',
      detail: `${periodDisplayLabel.value} : MyStash a repere ${number(visibleBlockingIssues.value.length)} point(s) qui empechent un dossier fiable.`,
    }
  }

  if (isDeclarationMarked.value) {
    return {
      title: `Declaration ${periodDisplayLabel.value} archivee`,
      detail: `${amount} a ete marque comme declare. Les documents restent disponibles dans le dossier legal.`,
    }
  }

  if (isMicroProfile.value) {
    return {
      title: 'Declarer le CA URSSAF',
      detail: `${periodDisplayLabel.value} : ${amount} pret a recopier, ${sales} vente(s) retenue(s), rubrique ${category}.`,
    }
  }

  if (isCompanyProfile.value) {
    return {
      title: 'Exporter le dossier comptable',
      detail: `${periodDisplayLabel.value} : ventes, achats, registres et CSV sont regroupes pour la compta.`,
    }
  }

  return {
    title: 'Preparer le dossier de ventes',
    detail: `${periodDisplayLabel.value} : MyStash rassemble les montants, documents et erreurs a corriger.`,
  }
})
const amountInstruction = computed(() => {
  if (visibleBlockingIssues.value.length) {
    return 'Montant provisoire : corrige les blocages avant de le declarer.'
  }

  if (isMicroProfile.value) {
    return `A recopier tel quel dans la declaration ${declarationPeriodicityLabel.value.toLowerCase()} URSSAF.`
  }

  if (isCompanyProfile.value) {
    return 'Montant utilise pour les exports comptables et les registres de la periode.'
  }

  return 'Montant calcule a partir des ventes encaissees connues.'
})
const profileNeedsAttention = computed(() => {
  const frequency = normalizedProfile.value.declarationFrequency
  return normalizedProfile.value.legalStatus === 'none'
    || normalizedProfile.value.completed === false
    || (isMicroProfile.value && !normalizedProfile.value.siret)
    || (isMicroProfile.value && (!frequency || frequency === 'UNKNOWN'))
    || (isCompanyProfile.value && !normalizedProfile.value.siret)
    || (isCompanyProfile.value && normalizedProfile.value.vatRegime === 'unknown')
})
const yearOptions = computed(() => Array.from({ length: 6 }, (_, index) => initialYear - index))
const monthOptions = computed(() => [
  { value: 1, label: 'Janvier' },
  { value: 2, label: 'Fevrier' },
  { value: 3, label: 'Mars' },
  { value: 4, label: 'Avril' },
  { value: 5, label: 'Mai' },
  { value: 6, label: 'Juin' },
  { value: 7, label: 'Juillet' },
  { value: 8, label: 'Aout' },
  { value: 9, label: 'Septembre' },
  { value: 10, label: 'Octobre' },
  { value: 11, label: 'Novembre' },
  { value: 12, label: 'Decembre' },
])
const quarterOptions = computed(() => [
  { value: 1, label: 'T1 - Janvier a mars', shortLabel: 'T1' },
  { value: 2, label: 'T2 - Avril a juin', shortLabel: 'T2' },
  { value: 3, label: 'T3 - Juillet a septembre', shortLabel: 'T3' },
  { value: 4, label: 'T4 - Octobre a decembre', shortLabel: 'T4' },
])

onMounted(loadAll)
onBeforeUnmount(clearCopiedTimer)

async function loadAll() {
  loading.value = true
  error.value = ''
  feedback.value = ''
  try {
    profile.value = await AdminService.administrativeProfile()
    applyDefaultPeriodForProfile()
    await Promise.all([loadSummary(), loadDocuments(), loadDocumentRecords(), loadInvoices()])
    resetProfileForm()
  } catch (err) {
    error.value = errorMessage(err, 'Impossible de charger le dossier administratif.')
  } finally {
    loading.value = false
  }
}

async function loadSummary() {
  if (!periodStart.value || !periodEnd.value) return
  summaryLoading.value = true
  error.value = ''
  try {
    summary.value = await AdminService.administrativeSummary({
      periodStart: periodStart.value,
      periodEnd: periodEnd.value,
    })
    if (summary.value?.profile) {
      profile.value = summary.value.profile
    }
  } catch (err) {
    error.value = errorMessage(err, 'Impossible de calculer les donnees administratives.')
  } finally {
    summaryLoading.value = false
  }
}

async function loadDocuments() {
  try {
    documents.value = await AdminService.administrativeDocuments()
  } catch {
    documents.value = []
  }
}

async function loadDocumentRecords() {
  try {
    documentRecords.value = await AdminService.administrativeDocumentRecords()
  } catch {
    documentRecords.value = []
  }
}

async function loadInvoices() {
  try {
    invoices.value = await AdminService.invoices({ limit: 300 })
  } catch {
    invoices.value = []
  }
}

function handlePeriodChange() {
  periodTouched.value = true
  void Promise.all([loadSummary(), loadInvoices(), loadDocumentRecords()])
}

function applyDefaultPeriodForProfile() {
  if (periodTouched.value) return
  periodYear.value = initialYear
  selectedMonth.value = today.getMonth() + 1
  selectedQuarter.value = Math.floor(today.getMonth() / 3) + 1
}

function resetProfileForm() {
  const source = normalizedProfile.value
  profileForm.legalStatus = source.legalStatus || 'none'
  profileForm.activities = [...(source.activities || defaultActivitiesForStatus(source.legalStatus))]
  profileForm.vatRegime = source.vatRegime || 'unknown'
  profileForm.declarations = [...(source.declarations || defaultDeclarationsForStatus(source.legalStatus, source.declarationFrequency))]
  profileForm.siret = source.siret || ''
  profileForm.businessName = source.businessName || ''
  profileForm.ownerName = source.ownerName || ''
  profileForm.tradeName = source.tradeName || ''
  profileForm.address = source.address || ''
  profileForm.legalForm = source.legalForm || getLegalStatusLabel(source.legalStatus)
  profileForm.mainActivity = source.mainActivity || ''
  profileForm.fiscalRegime = source.fiscalRegime || ''
  profileForm.declarationFrequency = source.declarationFrequency || 'UNKNOWN'
  profileForm.declarationPeriodicity = source.declarationPeriodicity || profileForm.declarations[0] || ''
  profileForm.withholdingTaxOption = source.withholdingTaxOption || 'UNKNOWN'
  profileForm.vatFranchise = source.vatFranchise || 'UNKNOWN'
  profileForm.activityStartDate = source.activityStartDate || ''
  profileForm.fiscalYearEndMonth = Number(source.fiscalYearEndMonth || 12)
  profileForm.fiscalYearEndDay = Number(source.fiscalYearEndDay || 31)
  profileForm.urssafCategory = source.urssafCategory || 'Vente de marchandises'
  profileForm.notes = source.notes || ''
  profileForm.verificationStatus = source.verificationStatus || 'A_VERIFIER'
  profileForm.usesOnlinePlatforms = Boolean(source.usesOnlinePlatforms)
  profileForm.buysForResale = Boolean(source.buysForResale)
  profileEditError.value = ''
}

function handleProfileStatusChange() {
  profileForm.legalForm = getLegalStatusLabel(profileForm.legalStatus)
  profileForm.activities = defaultActivitiesForStatus(profileForm.legalStatus)
  profileForm.declarations = defaultDeclarationsForStatus(profileForm.legalStatus, profileForm.declarationFrequency)
  profileForm.declarationPeriodicity = profileForm.declarations[0] || ''
  if (!profileFormRequiresSiret.value) {
    profileForm.siret = ''
  }
  if (!profileFormIsMicro.value) {
    profileForm.urssafCategory = ''
    profileForm.withholdingTaxOption = 'UNKNOWN'
  } else if (!profileForm.urssafCategory) {
    profileForm.urssafCategory = 'Vente de marchandises'
  }
}

function handleProfileFrequencyChange() {
  profileForm.declarations = defaultDeclarationsForStatus(profileForm.legalStatus, profileForm.declarationFrequency)
  profileForm.declarationPeriodicity = profileForm.declarations[0] || ''
}

function profilePayload() {
  return {
    legalStatus: profileForm.legalStatus,
    profileType: profileForm.legalStatus,
    activities: [...profileForm.activities],
    vatRegime: profileForm.vatRegime,
    declarations: [...profileForm.declarations],
    siret: profileFormRequiresSiret.value ? profileForm.siret : '',
    businessName: profileForm.businessName,
    ownerName: profileForm.ownerName,
    tradeName: profileForm.tradeName || profileForm.businessName,
    address: profileForm.address,
    legalForm: profileForm.legalForm,
    mainActivity: profileForm.mainActivity,
    fiscalRegime: profileForm.fiscalRegime,
    declarationFrequency: profileFormIsMicro.value ? profileForm.declarationFrequency : 'UNKNOWN',
    declarationPeriodicity: profileForm.declarationPeriodicity,
    withholdingTaxOption: profileFormIsMicro.value ? profileForm.withholdingTaxOption : 'UNKNOWN',
    vatFranchise: profileFormIsBusiness.value ? profileForm.vatFranchise : 'UNKNOWN',
    activityStartDate: profileForm.activityStartDate || null,
    fiscalYearEndMonth: Number(profileForm.fiscalYearEndMonth || 12),
    fiscalYearEndDay: Number(profileForm.fiscalYearEndDay || 31),
    urssafCategory: profileFormIsMicro.value ? profileForm.urssafCategory : '',
    defaultVatRate: normalizedProfile.value.defaultVatRate || null,
    notes: profileForm.notes,
    verificationStatus: profileForm.verificationStatus,
    usesOnlinePlatforms: profileForm.usesOnlinePlatforms,
    buysForResale: profileForm.buysForResale,
  }
}

async function saveProfile() {
  profileEditError.value = ''
  error.value = ''
  feedback.value = ''
  if (profileSiretMessage.value) {
    profileEditError.value = profileSiretMessage.value
    return
  }

  profileSaving.value = true
  try {
    profile.value = await AdminService.saveAdministrativeProfile(profilePayload())
    await Promise.all([loadSummary(), loadDocuments(), loadDocumentRecords()])
    resetProfileForm()
    feedback.value = 'Parametres administratifs enregistres.'
  } catch (err) {
    profileEditError.value = errorMessage(err, "Impossible d'enregistrer le profil administratif.")
  } finally {
    profileSaving.value = false
  }
}

async function copyDeclarationField(field) {
  const value = cleanText(field?.copyValue)
  if (!value) return
  await copyText(value, `${field.label} copie.`, `${field.label} a recopier : ${value}`, field.id)
}

async function copyDeclarationAmount() {
  const amountField = declarationFields.value.find((field) => field.id === 'revenue')
  if (amountField?.copyValue) {
    await copyDeclarationField({ ...amountField, id: 'amount', label: 'Montant' })
    return
  }
  await copyText(formatAmountForCopy(summary.value?.periodRevenue), 'Montant copie.', `Montant a recopier : ${formatAmountForCopy(summary.value?.periodRevenue)}`, 'amount')
}

async function copyText(value, successMessage, fallbackMessage, key) {
  try {
    if (!navigator?.clipboard) throw new Error('Clipboard unavailable')
    await navigator.clipboard.writeText(value)
    feedback.value = successMessage
  } catch {
    feedback.value = fallbackMessage
  } finally {
    markCopied(key)
  }
}

function markCopied(key) {
  if (!key) return
  clearCopiedTimer()
  copiedKey.value = key
  copiedResetTimer = window.setTimeout(() => {
    copiedKey.value = ''
    copiedResetTimer = 0
  }, 2000)
}

function clearCopiedTimer() {
  if (!copiedResetTimer) return
  window.clearTimeout(copiedResetTimer)
  copiedResetTimer = 0
}

async function generateUrssafSheet() {
  if (!urssafDocumentRow.value) return
  await generateExportRow(urssafDocumentRow.value)
}

async function generateExportRow(row) {
  if (!row || row.disabled) return
  await generateAdministrativeExport(row.documentType || row.id)
}

async function generateAdministrativeExport(documentId) {
  if (!documentId) return
  generating.value = documentId
  error.value = ''
  feedback.value = ''
  try {
    const payload = {
      periodStart: periodStart.value,
      periodEnd: periodEnd.value,
      year: selectedYear.value,
    }
    const filename = await AdminService.generateAdministrativeExport(documentId, payload)
    rememberGeneratedDocument(documentId, filename)
    await loadDocumentRecords()
    feedback.value = `Document genere : ${filename}.`
  } catch (err) {
    error.value = errorMessage(err, 'Impossible de generer le document.')
  } finally {
    generating.value = ''
  }
}

async function generateMissingInvoices() {
  if (!missingInvoices.value.length || generatingInvoices.value) return
  generatingInvoices.value = true
  error.value = ''
  feedback.value = ''
  try {
    const result = await AdminService.generateMissingAdministrativeInvoices({
      periodStart: periodStart.value,
      periodEnd: periodEnd.value,
    })
    await Promise.all([loadInvoices(), loadDocumentRecords()])
    const count = result?.generatedCount ?? result?.count ?? missingInvoices.value.length
    feedback.value = `${number(count)} facture(s) generee(s).`
  } catch (err) {
    error.value = errorMessage(err, 'Impossible de generer les factures manquantes.')
  } finally {
    generatingInvoices.value = false
  }
}

async function markDeclarationDone() {
  if (markingDeclaration.value || isDeclarationMarked.value) return
  markingDeclaration.value = true
  error.value = ''
  feedback.value = ''
  try {
    const record = await AdminService.markAdministrativeDeclarationDone({
      periodStart: periodStart.value,
      periodEnd: periodEnd.value,
      amount: Number(summary.value?.periodRevenue || 0),
      title: `Declaration URSSAF ${periodDisplayLabel.value} marquee comme faite`,
      profileId: administrativeProfile.value.id,
    })
    documentRecords.value = mergeRecords([record, ...documentRecords.value])
    feedback.value = 'Declaration archivee pour cette periode.'
  } catch (err) {
    error.value = errorMessage(err, "Impossible d'archiver la declaration.")
  } finally {
    markingDeclaration.value = false
  }
}

function serviceCard(options) {
  const busy = generatingService.value === options.id
    || (options.action === 'missing-invoices' && generatingInvoices.value)
    || (options.action === 'mark-declared' && markingDeclaration.value)
  return {
    icon: FileText,
    actionIcon: null,
    status: 'pending',
    badge: 'A faire',
    action: '',
    actionLabel: 'Lancer',
    busyLabel: 'Traitement...',
    metrics: [],
    documentTypes: [],
    primary: false,
    disabled: false,
    ...options,
    metrics: (options.metrics || []).filter(Boolean),
    busy,
  }
}

async function handleAdministrativeService(service) {
  if (!service || service.disabled || service.busy) return

  if (service.action === 'profile') {
    await openSettings()
    return
  }
  if (service.action === 'fix-sales') {
    await router.push({ name: 'gestion' })
    return
  }
  if (service.action === 'first-issue') {
    const firstIssue = visibleBlockingIssues.value[0]
    if (firstIssue) handleIssueAction(firstIssue)
    return
  }
  if (service.action === 'refresh') {
    generatingService.value = service.id
    try {
      await Promise.all([loadSummary(), loadInvoices(), loadDocumentRecords()])
      feedback.value = 'Dossier recalcule.'
    } finally {
      generatingService.value = ''
    }
    return
  }
  if (service.action === 'missing-invoices') {
    await generateMissingInvoices()
    return
  }
  if (service.action === 'prepare-urssaf') {
    generatingService.value = service.id
    try {
      await copyDeclarationAmount()
      await generateUrssafSheet()
      feedback.value = 'Declaration URSSAF preparee : montant copie et fiche generee.'
    } finally {
      generatingService.value = ''
    }
    return
  }
  if (service.action === 'bundle') {
    await generateDocumentBundle(service)
    return
  }
  if (service.action === 'mark-declared') {
    await markDeclarationDone()
  }
}

async function generateDocumentBundle(service) {
  const documentTypes = service?.documentTypes || []
  if (!documentTypes.length) return

  generatingService.value = service.id
  let generatedCount = 0
  try {
    for (const documentType of documentTypes) {
      const row = documentRowFor(documentType)
      if (!row || row.disabled) continue
      await generateExportRow(row)
      generatedCount += 1
    }
    if (!error.value) {
      feedback.value = generatedCount
        ? `${number(generatedCount)} document(s) genere(s) pour le dossier.`
        : 'Aucun document disponible a generer pour cette periode.'
    }
  } finally {
    generatingService.value = ''
  }
}

function fallbackProfileObligations() {
  if (isMicroProfile.value) {
    return [
      fallbackObligation('micro-profile', 'Profil URSSAF', 'SIRET, periodicite et rubrique doivent etre prets avant la saisie officielle.', 'profile'),
      fallbackObligation('micro-invoices', 'Factures et justificatifs', 'Les ventes de la periode doivent avoir des pieces propres pour l archive.', 'invoices'),
      fallbackObligation('micro-registers', 'Registres', 'Livre des recettes, registre des achats et recap annuel restent disponibles en exports.', 'document', 'receipts-register'),
    ]
  }

  if (isCompanyProfile.value) {
    return [
      fallbackObligation('company-profile', 'Profil comptable', 'SIRET, TVA et cloture fiscale structurent les exports.', 'profile'),
      fallbackObligation('company-package', 'Dossier comptable', 'Regrouper ventes, achats, justificatifs et resultat estime pour le comptable.', 'document', 'fiscal-summary'),
      fallbackObligation('company-invoices', 'Factures et pieces', 'Generer les factures manquantes et garder les justificatifs rattaches.', 'invoices'),
    ]
  }

  if (administrativeProfile.value.family === 'personal') {
    return [
      fallbackObligation('personal-recap', 'Recap ventes', 'Garder un resume clair des ventes sans presenter cela comme une declaration officielle.', 'document', 'receipts-register'),
      fallbackObligation('personal-bic', 'Cadre a verifier', 'Verifier si les ventes restent personnelles ou relevent d une activite imposable.', ''),
    ]
  }

  return [
    fallbackObligation('regularization-profile', 'Statut a choisir', 'Configurer le statut avant de preparer une declaration professionnelle.', 'profile'),
    fallbackObligation('regularization-data', 'Donnees a nettoyer', 'Corriger les ventes incompletes avant d utiliser les montants.', 'sales'),
  ]
}

function fallbackObligation(id, title, description, action = '', documentType = '') {
  return {
    id,
    title,
    description,
    status: action === 'profile' ? 'incomplete' : 'to_verify',
    severity: action === 'profile' ? 'warning' : 'info',
    actionLabel: '',
    documentType,
    fallbackAction: action,
  }
}

function normalizeProfileAction(obligation) {
  const documentType = obligation?.documentType || ''
  const documentRow = documentType ? documentRowFor(documentType) : null
  const action = profileActionFor(obligation, documentRow)
  const status = normalizeItemStatus(documentRow?.status || obligation?.status, obligation?.severity)
  return {
    id: obligation?.id || obligation?.title || documentType,
    title: obligation?.title || 'Point administratif',
    description: obligation?.description || 'Information a verifier avant declaration.',
    status,
    action,
    actionLabel: obligation?.actionLabel || '',
    documentType,
    disabled: Boolean(documentRow?.disabled),
    busy: (action === 'document' && generating.value === documentType) || (action === 'invoices' && generatingInvoices.value),
  }
}

function normalizeQualityItem(check) {
  const action = qualityActionFor(check)
  return {
    id: check?.id || check?.title || 'quality-check',
    title: check?.title || 'Controle des donnees',
    message: check?.message || 'Point a verifier.',
    status: normalizeItemStatus(check?.status, check?.severity),
    target: check?.target || '',
    action,
    actionLabel: check?.actionLabel || '',
    busy: action === 'invoices' && generatingInvoices.value,
  }
}

function normalizeItemStatus(status, severity = '') {
  if (status === 'complete' || status === 'checked' || status === 'generated' || status === 'ready' || status === 'done') return status
  if (status === 'incomplete' || status === 'data_missing') return status
  if (status === 'to_verify' || status === 'regenerable') return status
  if (status === 'pending' || status === 'draft' || status === 'archived' || status === 'not_applicable') return status
  if (severity === 'danger') return 'incomplete'
  if (severity === 'warning') return 'to_verify'
  return 'pending'
}

function profileActionFor(obligation, documentRow) {
  const id = String(obligation?.id || '')
  const category = String(obligation?.category || '')
  const status = String(obligation?.status || '')
  if (obligation?.fallbackAction === 'document') return documentRow ? 'document' : ''
  if (obligation?.fallbackAction === 'invoices') return missingInvoices.value.length ? 'invoices' : ''
  if (obligation?.fallbackAction) return obligation.fallbackAction
  if (status === 'incomplete' && (id.includes('urssaf') || id.includes('profile'))) return 'profile'
  if (category === 'tva' && normalizedProfile.value.vatRegime === 'unknown') return 'profile'
  if ((id.endsWith('-invoicing') || id.endsWith('-invoices')) && missingInvoices.value.length) return 'invoices'
  if (documentRow) return 'document'
  return ''
}

function qualityActionFor(check) {
  if (check?.target === 'profile') return 'profile'
  if (check?.target === 'invoices' && missingInvoices.value.length) return 'invoices'
  if (check?.target === 'period') return 'period'
  if (check?.target === 'sales') return 'sales'
  return ''
}

function documentRowFor(documentType) {
  return administrativeDocumentRows.value.find((row) => row.id === documentType || row.documentType === documentType)
}

function profilePlanActionLabel(item) {
  if (item?.action === 'document' && generating.value === item.documentType) return 'Generation...'
  if (item?.action === 'invoices' && generatingInvoices.value) return 'Generation...'
  if (item?.action === 'profile') return 'Modifier le profil'
  if (item?.action === 'invoices') return item.actionLabel || invoiceActionLabel.value
  if (item?.action === 'sales') return 'Corriger les ventes'
  if (item?.action === 'document') return item.actionLabel || 'Generer'
  return item?.actionLabel || 'Ouvrir'
}

function qualityActionLabel(item) {
  if (item?.action === 'invoices' && generatingInvoices.value) return 'Generation...'
  if (item?.action === 'profile') return 'Configurer'
  if (item?.action === 'period') return 'Changer la periode'
  if (item?.action === 'sales') return 'Corriger dans Gestion'
  if (item?.action === 'invoices') return item.actionLabel || invoiceActionLabel.value
  return item?.actionLabel || 'Corriger'
}

async function handleProfilePlanAction(item) {
  if (item?.action === 'profile') {
    await openSettings()
    return
  }
  if (item?.action === 'sales') {
    await router.push({ name: 'gestion' })
    return
  }
  if (item?.action === 'invoices') {
    await generateMissingInvoices()
    return
  }
  if (item?.action === 'document') {
    const row = documentRowFor(item.documentType)
    if (row) await generateExportRow(row)
  }
}

async function handleQualityAction(item) {
  if (item?.action === 'profile') {
    await openSettings()
    return
  }
  if (item?.action === 'period') {
    scrollToTop()
    return
  }
  if (item?.action === 'invoices') {
    await generateMissingInvoices()
    return
  }
  if (item?.action === 'sales') {
    await router.push({ name: 'gestion' })
  }
}

function handleIssueAction(issue) {
  if (issue?.target === 'profile') {
    openSettings()
    return
  }
  if (issue?.target === 'invoices') {
    void generateMissingInvoices()
    return
  }
  if (issue?.target === 'period') {
    scrollToTop()
    return
  }
  void router.push({ name: 'gestion' })
}

function handleDeclarationStep(step) {
  if (step?.action === 'profile') {
    openSettings()
    return
  }
  if (step?.action === 'issues') {
    const invoiceIssue = visibleBlockingIssues.value.find((issue) => issue.target === 'invoices')
    if (invoiceIssue && visibleBlockingIssues.value.length === 1) {
      void generateMissingInvoices()
      return
    }
    void router.push({ name: 'gestion' })
    return
  }
  if (step?.action === 'copy') {
    void copyDeclarationAmount()
    return
  }
  if (step?.action === 'mark-done') {
    void markDeclarationDone()
  }
}

function issueActionLabel(issue) {
  if (issue?.target === 'profile') return 'Configurer'
  if (issue?.target === 'invoices') return 'Generer les factures'
  if (issue?.target === 'period') return 'Changer la periode'
  return 'Corriger dans Gestion'
}

function toggleSettings() {
  settingsOpen.value = !settingsOpen.value
}

async function openSettings() {
  settingsOpen.value = true
  await nextTick()
  settingsPanelRef.value?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

function scrollToTop() {
  if (typeof window === 'undefined') return
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

function rememberGeneratedDocument(documentId, filename) {
  const row = administrativeDocumentRows.value.find((document) => document.documentType === documentId || document.id === documentId)
  const record = {
    id: `${documentId}:${periodStart.value}:${periodEnd.value}`,
    documentType: documentId,
    type: documentId,
    title: row?.name || filename || documentId,
    filename,
    filePath: filename,
    periodStart: periodStart.value,
    periodEnd: periodEnd.value,
    period: row?.period || periodDisplayLabel.value,
    status: 'generated',
    generatedAt: new Date().toISOString(),
    updatedAt: new Date().toISOString(),
  }
  const next = mergeRecords([
    record,
    ...generatedDocuments.value.filter((item) => recordKey(item) !== recordKey(record)),
  ]).slice(0, 30)
  generatedDocuments.value = next
  writeGeneratedDocuments(next)
}

function documentActionLabel(row) {
  return documentFormat(row) === 'CSV' ? 'Telecharger CSV' : 'Generer PDF'
}

function documentFormat(row) {
  return String(row?.format || row?.type || row?.documentType || '').toUpperCase().includes('CSV') ? 'CSV' : 'PDF'
}

function simpleDocumentStatus(row) {
  if (row?.disabled) return 'Bloque'
  if (row?.status === 'generated') return 'Pret'
  return 'A generer'
}

function statusLabel(status) {
  return STATUS_LABELS[status] || STATUS_LABELS.pending
}

function statusClass(status) {
  if (status === 'complete' || status === 'checked' || status === 'generated' || status === 'ready' || status === 'done') return 'is-success'
  if (status === 'incomplete' || status === 'data_missing') return 'is-danger'
  if (status === 'to_verify' || status === 'regenerable') return 'is-warning'
  if (status === 'archived') return 'is-neutral'
  return 'is-info'
}

function money(value) {
  return new Intl.NumberFormat('fr-FR', {
    style: 'currency',
    currency: 'EUR',
    maximumFractionDigits: 2,
  }).format(Number(value || 0))
}

function number(value) {
  return new Intl.NumberFormat('fr-FR', {
    maximumFractionDigits: 0,
  }).format(Number(value || 0))
}

function formatAmountForCopy(value) {
  return Number(value || 0).toFixed(2).replace('.', ',')
}

function formatDate(value) {
  if (!value) return ''
  return new Intl.DateTimeFormat('fr-FR', { day: '2-digit', month: '2-digit', year: 'numeric' }).format(
    new Date(`${String(value).slice(0, 10)}T00:00:00`),
  )
}

function dateToInputValue(date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function cleanText(value) {
  const text = value == null ? '' : String(value).trim()
  return text === '-' ? '' : text
}

function clampNumber(value, min, max) {
  const numberValue = Number(value)
  if (!Number.isFinite(numberValue)) return min
  return Math.min(max, Math.max(min, Math.trunc(numberValue)))
}

function recordType(record) {
  return record?.type || record?.documentType || ''
}

function recordKey(record) {
  return record?.id
    || `${recordType(record)}:${record?.periodStart || ''}:${record?.periodEnd || ''}:${record?.generatedAt || record?.createdAt || ''}`
}

function mergeRecords(records = []) {
  const map = new Map()
  for (const record of records) {
    if (!record) continue
    const key = recordKey(record)
    const current = map.get(key)
    const currentDate = String(current?.updatedAt || current?.generatedAt || current?.createdAt || '')
    const nextDate = String(record.updatedAt || record.generatedAt || record.createdAt || '')
    if (!current || nextDate >= currentDate) {
      map.set(key, record)
    }
  }
  return [...map.values()]
}

function readGeneratedDocuments() {
  if (typeof localStorage === 'undefined') return []
  try {
    const value = JSON.parse(localStorage.getItem(GENERATED_DOCUMENTS_STORAGE_KEY) || '[]')
    return Array.isArray(value) ? value : []
  } catch {
    return []
  }
}

function writeGeneratedDocuments(value) {
  if (typeof localStorage === 'undefined') return
  localStorage.setItem(GENERATED_DOCUMENTS_STORAGE_KEY, JSON.stringify(value))
}

function errorMessage(errorObject, fallback) {
  return errorObject?.response?.data?.message || errorObject?.message || fallback
}
</script>

<style scoped>
.admin-page {
  min-height: 100%;
  background: #f7f8fa;
  color: #111827;
}

.admin-page.is-embedded {
  background: transparent;
}

.admin-page.is-embedded .admin-shell {
  width: 100%;
  max-width: none;
  padding: 0;
}

.admin-shell {
  width: min(1440px, 100%);
  margin: 0 auto;
  padding: clamp(16px, 2.2vw, 32px) clamp(12px, 2vw, 24px) 40px;
}

.admin-header,
.next-action-card,
.profile-summary-card,
.checklist-card,
.copy-workbench,
.simple-panel,
.blockers-panel,
.settings-panel {
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 18px;
  box-shadow: 0 10px 28px rgba(15, 23, 42, 0.05);
}

.admin-header {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: end;
  justify-content: space-between;
  gap: 20px;
  padding: 18px 20px;
  margin-bottom: 18px;
}

.admin-header h1 {
  margin: 0;
  font-size: 1.55rem;
  line-height: 1.2;
  font-weight: 760;
  letter-spacing: 0;
}

.admin-header p {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin: 6px 0 0;
  color: #6b7280;
  font-size: 0.92rem;
}

.header-actions {
  display: flex;
  align-items: flex-end;
  justify-content: flex-end;
  gap: 10px;
  flex-wrap: wrap;
  min-width: 0;
}

.period-controls {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(132px, 1fr));
  align-items: end;
  gap: 8px;
  min-width: min(100%, 28rem);
}

.period-controls label,
.profile-form label {
  display: grid;
  gap: 6px;
  color: #4b5563;
  font-size: 0.78rem;
  font-weight: 700;
}

.period-controls select,
.profile-form select,
.profile-form input,
.profile-form textarea {
  width: 100%;
  min-height: 38px;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  background: #ffffff;
  color: #111827;
  font: inherit;
  font-size: 0.92rem;
  font-weight: 600;
  padding: 8px 10px;
  outline: none;
}

.period-controls select:focus,
.profile-form select:focus,
.profile-form input:focus,
.profile-form textarea:focus {
  border-color: #2563eb;
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.12);
}

.notice {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 14px;
  border-radius: 8px;
  margin-bottom: 14px;
  font-size: 0.92rem;
  font-weight: 650;
}

.notice.danger {
  color: #991b1b;
  background: #fef2f2;
  border: 1px solid #fecaca;
}

.notice.ok {
  color: #065f46;
  background: #ecfdf5;
  border: 1px solid #a7f3d0;
}

.notice-icon,
.button-icon {
  width: 18px;
  height: 18px;
  flex: 0 0 auto;
}

.loading-box {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  min-height: 220px;
  color: #4b5563;
  font-weight: 700;
}

.spinning {
  animation: spin 0.9s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.icon-button,
.text-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  min-height: 38px;
  border: 1px solid transparent;
  border-radius: 8px;
  padding: 8px 12px;
  font: inherit;
  font-size: 0.9rem;
  font-weight: 760;
  text-decoration: none;
  cursor: pointer;
  white-space: normal;
  transition: background 0.15s ease, border-color 0.15s ease, color 0.15s ease, opacity 0.15s ease;
}

.icon-button:disabled,
.text-button:disabled {
  cursor: not-allowed;
  opacity: 0.55;
}

.icon-button.primary {
  background: #2563eb;
  border-color: #2563eb;
  color: #ffffff;
}

.icon-button.primary:not(:disabled):hover {
  background: #1d4ed8;
}

.icon-button.secondary {
  background: #ffffff;
  border-color: #d1d5db;
  color: #111827;
}

.icon-button.secondary:not(:disabled):hover,
.icon-button.ghost:not(:disabled):hover {
  background: #f3f4f6;
}

.icon-button.ghost {
  background: #f9fafb;
  border-color: #e5e7eb;
  color: #374151;
}

.text-button {
  min-height: 34px;
  padding: 6px 0;
  color: #2563eb;
  background: transparent;
}

.text-button:not(:disabled):hover {
  color: #1d4ed8;
}

.section-kicker,
.document-type {
  margin: 0 0 6px;
  color: #6b7280;
  font-size: 0.75rem;
  line-height: 1.2;
  font-weight: 800;
  text-transform: uppercase;
  letter-spacing: 0.08em;
}

.command-center {
  display: grid;
  grid-template-columns: minmax(0, 1.28fr) minmax(20rem, 0.92fr);
  gap: 16px;
  margin-bottom: 16px;
}

.next-action-card {
  padding: 24px;
  border-left: 4px solid #2563eb;
}

.declaration-side {
  display: grid;
  gap: 14px;
}

.profile-summary-card,
.checklist-card {
  padding: 18px;
}

.side-card-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
}

.profile-summary-card h2,
.checklist-card h2 {
  margin: 0;
  color: #111827;
  font-size: 1.15rem;
  line-height: 1.25;
  letter-spacing: 0;
}

.profile-mode-line {
  margin: 6px 0 0;
  color: #4b5563;
  font-size: 0.86rem;
  line-height: 1.35;
  font-weight: 680;
}

.profile-summary-list {
  display: grid;
  gap: 10px;
  margin-top: 14px;
  margin-bottom: 12px;
}

.profile-summary-list div {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
  min-width: 0;
  padding: 9px 0;
  border-top: 1px solid #e5e7eb;
}

.profile-summary-list div:first-child {
  border-top: 0;
  padding-top: 0;
}

.profile-summary-list dt {
  min-width: 0;
  color: #6b7280;
  font-size: 0.78rem;
  font-weight: 800;
}

.profile-summary-list dd {
  margin: 0;
  color: #111827;
  font-size: 0.92rem;
  font-weight: 850;
  text-align: right;
  overflow-wrap: anywhere;
}

.declaration-step-list {
  display: grid;
  gap: 8px;
  margin-top: 14px;
}

.declaration-step {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  width: 100%;
  min-height: 54px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #f9fafb;
  color: #111827;
  padding: 10px;
  text-align: left;
  font: inherit;
  cursor: pointer;
}

.declaration-step:disabled {
  cursor: default;
}

.declaration-step.is-success {
  border-color: #a7f3d0;
  background: #ecfdf5;
}

.declaration-step.is-danger {
  border-color: #fecaca;
  background: #fff7f7;
}

.declaration-step.is-warning {
  border-color: #fde68a;
  background: #fffbeb;
}

.step-index {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
  width: 26px;
  height: 26px;
  border-radius: 999px;
  background: #111827;
  color: #ffffff;
  font-size: 0.78rem;
  font-weight: 850;
}

.declaration-step strong,
.declaration-step small {
  display: block;
}

.declaration-step strong {
  font-size: 0.9rem;
  line-height: 1.2;
  font-weight: 820;
}

.declaration-step small {
  margin-top: 3px;
  color: #4b5563;
  font-size: 0.78rem;
  line-height: 1.25;
  font-weight: 650;
}

.action-head,
.section-heading,
.document-footer,
.settings-panel summary {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.action-head h2,
.section-heading h2 {
  margin: 0;
  font-size: 1.25rem;
  line-height: 1.25;
  letter-spacing: 0;
}

.action-head h2 {
  max-width: 760px;
  color: #111827;
  font-size: clamp(1.55rem, 3vw, 2.35rem);
  font-weight: 820;
}

.action-head p:not(.section-kicker) {
  max-width: 720px;
  margin: 8px 0 0;
  color: #4b5563;
  font-size: 0.98rem;
  font-weight: 680;
  line-height: 1.45;
}

.period-label {
  margin: 5px 0 0;
  color: #4b5563;
  font-size: 0.98rem;
  font-weight: 650;
}

.amount-block {
  margin: 28px 0 20px;
}

.amount-block span {
  display: block;
  color: #4b5563;
  font-size: 0.95rem;
  font-weight: 700;
}

.amount-block strong {
  display: block;
  margin-top: 6px;
  color: #0f172a;
  font-size: clamp(2.7rem, 7vw, 5.4rem);
  line-height: 0.95;
  letter-spacing: 0;
}

.amount-block p {
  margin: 10px 0 0;
  color: #374151;
  font-size: 0.96rem;
  font-weight: 760;
}

.primary-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  margin-top: 22px;
}

.primary-actions > * {
  flex: 1 1 220px;
}

.services-panel {
  margin-bottom: 14px;
}

.service-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 12px;
  margin-top: 14px;
}

.service-card {
  display: grid;
  align-content: start;
  gap: 12px;
  min-width: 0;
  min-height: 252px;
  padding: 16px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #ffffff;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.04);
}

.service-card.is-primary-service {
  border-color: #bfdbfe;
  box-shadow: 0 8px 18px rgba(37, 99, 235, 0.08);
}

.service-card.is-danger {
  border-color: #fecaca;
  background: #fff7f7;
}

.service-card.is-warning {
  border-color: #fde68a;
  background: #fffbeb;
}

.service-card.is-success {
  border-color: #bbf7d0;
}

.service-card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.service-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 38px;
  height: 38px;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
  background: #f9fafb;
  color: #374151;
}

.service-icon.is-success {
  color: #047857;
  background: #ecfdf5;
  border-color: #a7f3d0;
}

.service-icon.is-danger {
  color: #b91c1c;
  background: #fef2f2;
  border-color: #fecaca;
}

.service-icon.is-warning {
  color: #92400e;
  background: #fffbeb;
  border-color: #fde68a;
}

.service-card > strong {
  color: #111827;
  font-size: 1rem;
  line-height: 1.25;
  font-weight: 860;
}

.service-card > p {
  margin: 0;
  color: #4b5563;
  font-size: 0.88rem;
  line-height: 1.4;
  font-weight: 640;
}

.service-metrics {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
  margin-top: 2px;
}

.service-metrics span {
  display: inline-flex;
  min-height: 25px;
  align-items: center;
  border: 1px solid #e5e7eb;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.72);
  color: #374151;
  padding: 3px 8px;
  font-size: 0.76rem;
  line-height: 1.2;
  font-weight: 780;
}

.service-card .icon-button {
  width: 100%;
  margin-top: auto;
}

.copy-workbench {
  padding: 20px;
  margin-bottom: 14px;
}

.copy-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 10px;
  margin-top: 14px;
}

.copy-field {
  display: grid;
  align-content: start;
  gap: 8px;
  min-height: 142px;
  min-width: 0;
  padding: 14px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #f9fafb;
}

.copy-field.is-missing {
  border-color: #fecaca;
  background: #fff7f7;
}

.copy-field span {
  color: #6b7280;
  font-size: 0.76rem;
  line-height: 1.25;
  font-weight: 850;
  text-transform: uppercase;
  letter-spacing: 0.06em;
}

.copy-field strong {
  color: #111827;
  font-size: 1.02rem;
  line-height: 1.25;
  font-weight: 830;
  overflow-wrap: anywhere;
}

.copy-field p {
  margin: 0;
  color: #4b5563;
  font-size: 0.86rem;
  line-height: 1.35;
}

.mini-copy-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  width: fit-content;
  min-height: 32px;
  margin-top: auto;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  background: #ffffff;
  color: #2563eb;
  padding: 6px 10px;
  font: inherit;
  font-size: 0.84rem;
  font-weight: 800;
  cursor: pointer;
}

.mini-copy-button.is-copied {
  color: #047857;
  border-color: #a7f3d0;
  background: #ecfdf5;
}

.status-pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 28px;
  border-radius: 999px;
  padding: 5px 10px;
  border: 1px solid #e5e7eb;
  background: #f9fafb;
  color: #4b5563;
  font-size: 0.78rem;
  font-weight: 800;
  white-space: normal;
  text-align: center;
}

.status-pill.is-success {
  color: #047857;
  background: #ecfdf5;
  border-color: #a7f3d0;
}

.status-pill.is-danger {
  color: #b91c1c;
  background: #fef2f2;
  border-color: #fecaca;
}

.status-pill.is-warning {
  color: #92400e;
  background: #fffbeb;
  border-color: #fde68a;
}

.status-pill.is-info,
.status-pill.is-neutral {
  color: #374151;
  background: #f3f4f6;
  border-color: #e5e7eb;
}

.blockers-panel,
.simple-panel,
.settings-panel {
  padding: 20px;
  margin-top: 14px;
}

.issue-list {
  display: grid;
  gap: 10px;
  margin-top: 14px;
}

.issue-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 14px;
  border: 1px solid #fecaca;
  border-radius: 8px;
  background: #fff7f7;
}

.issue-card strong {
  display: block;
  color: #111827;
  font-size: 0.98rem;
}

.issue-card p {
  margin: 4px 0 0;
  color: #6b7280;
  font-size: 0.9rem;
}

.ready-line {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 14px 2px 0;
  color: #047857;
  font-size: 0.92rem;
  font-weight: 760;
}

.progressive-panels {
  display: grid;
  gap: 14px;
  margin-top: 14px;
}

.progressive-panels .simple-panel {
  margin-top: 0;
}

.disclosure-panel {
  padding: 0;
}

.disclosure-panel summary,
.optional-panel summary {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 18px 20px;
  cursor: pointer;
  list-style: none;
}

.disclosure-panel summary::-webkit-details-marker,
.optional-panel summary::-webkit-details-marker {
  display: none;
}

.disclosure-panel summary h2,
.optional-panel summary h2 {
  margin: 0;
  color: #111827;
  font-size: 1.15rem;
  line-height: 1.25;
  letter-spacing: 0;
}

.disclosure-body,
.optional-panel-body {
  display: grid;
  gap: 14px;
  border-top: 1px solid #e5e7eb;
  padding: 18px 20px 20px;
}

.profile-plan-intro {
  display: grid;
  gap: 5px;
}

.profile-plan-intro strong {
  color: #111827;
  font-size: 1rem;
  font-weight: 840;
}

.profile-plan-intro p,
.calculation-note {
  margin: 0;
  color: #4b5563;
  font-size: 0.9rem;
  line-height: 1.45;
  font-weight: 650;
}

.profile-plan-list,
.quality-list {
  display: grid;
  gap: 8px;
}

.profile-plan-row,
.quality-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  min-width: 0;
  padding: 12px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #ffffff;
}

.profile-plan-row.is-success,
.quality-row.is-success {
  border-color: #bbf7d0;
  background: #f0fdf4;
}

.profile-plan-row.is-warning,
.quality-row.is-warning {
  border-color: #fde68a;
  background: #fffbeb;
}

.profile-plan-row.is-danger,
.quality-row.is-danger {
  border-color: #fecaca;
  background: #fff7f7;
}

.profile-plan-copy {
  display: grid;
  gap: 6px;
  min-width: 0;
}

.profile-plan-copy .status-pill {
  width: fit-content;
}

.profile-plan-copy strong,
.quality-row strong {
  color: #111827;
  font-size: 0.94rem;
  line-height: 1.25;
  font-weight: 830;
}

.profile-plan-copy p,
.quality-row p {
  margin: 0;
  color: #4b5563;
  font-size: 0.86rem;
  line-height: 1.4;
  font-weight: 620;
}

.data-summary-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 10px;
}

.data-summary-item {
  display: grid;
  align-content: start;
  gap: 6px;
  min-width: 0;
  padding: 12px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #f9fafb;
}

.data-summary-item span {
  color: #6b7280;
  font-size: 0.74rem;
  line-height: 1.2;
  font-weight: 850;
  text-transform: uppercase;
  letter-spacing: 0.06em;
}

.data-summary-item strong {
  color: #111827;
  font-size: 1rem;
  line-height: 1.25;
  font-weight: 850;
  overflow-wrap: anywhere;
}

.data-summary-item p {
  margin: 0;
  color: #4b5563;
  font-size: 0.82rem;
  line-height: 1.35;
  font-weight: 620;
}

.source-list {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  padding-top: 4px;
}

.source-list span {
  color: #6b7280;
  font-size: 0.76rem;
  font-weight: 850;
  text-transform: uppercase;
  letter-spacing: 0.06em;
}

.source-list a {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  color: #2563eb;
  font-size: 0.84rem;
  font-weight: 760;
  text-decoration: none;
}

.source-list a:hover {
  color: #1d4ed8;
}

.document-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(210px, 1fr));
  gap: 12px;
  margin-top: 16px;
}

.document-card {
  display: grid;
  align-content: space-between;
  min-height: 174px;
  padding: 14px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #ffffff;
}

.document-card h3 {
  margin: 0;
  font-size: 1rem;
  line-height: 1.28;
  letter-spacing: 0;
}

.document-card p {
  margin: 7px 0 0;
  color: #6b7280;
  font-size: 0.88rem;
}

.document-footer {
  align-items: center;
  margin-top: 18px;
}

.settings-panel {
  padding: 0;
}

.settings-panel summary {
  align-items: center;
  padding: 18px 20px;
  cursor: pointer;
  list-style: none;
}

.settings-panel summary::-webkit-details-marker {
  display: none;
}

.settings-panel summary > span:first-child {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 1rem;
  font-weight: 800;
}

.profile-form {
  border-top: 1px solid #e5e7eb;
  padding: 20px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 14px;
}

.profile-form textarea {
  resize: vertical;
}

.profile-form small {
  color: #b91c1c;
  font-size: 0.78rem;
}

.span-2 {
  grid-column: span 2;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  flex-wrap: wrap;
  margin-top: 18px;
}

@media (max-width: 1180px) {
  .admin-header {
    align-items: flex-start;
    grid-template-columns: 1fr;
  }

  .command-center {
    grid-template-columns: 1fr;
  }

  .header-actions {
    justify-content: flex-start;
    width: 100%;
  }

  .period-controls {
    width: 100%;
    min-width: 0;
  }
}

@media (max-width: 640px) {
  .admin-shell {
    padding: 14px 10px 30px;
  }

  .admin-header,
  .next-action-card,
  .profile-summary-card,
  .checklist-card,
  .copy-workbench,
  .simple-panel,
  .blockers-panel {
    padding: 16px;
  }

  .period-controls,
  .period-controls label,
  .icon-button,
  .primary-actions,
  .header-actions,
  .issue-card,
  .section-heading,
  .document-footer {
    width: 100%;
  }

  .period-controls {
    grid-template-columns: 1fr;
  }

  .issue-card,
  .action-head,
  .section-heading,
  .document-footer {
    align-items: stretch;
  }

  .primary-actions .icon-button,
  .header-actions .icon-button {
    justify-content: center;
  }

  .amount-block strong {
    font-size: clamp(2.25rem, 16vw, 3.4rem);
  }

  .issue-card,
  .profile-plan-row,
  .quality-row,
  .action-head,
  .section-heading,
  .document-footer {
    flex-direction: column;
  }

  .profile-plan-row,
  .quality-row,
  .disclosure-panel summary,
  .optional-panel summary {
    align-items: stretch;
  }

  .disclosure-panel,
  .optional-panel {
    padding: 0;
  }

  .source-list {
    align-items: flex-start;
    flex-direction: column;
  }

  .copy-field {
    min-height: auto;
  }

  .service-card {
    min-height: auto;
  }

  .span-2 {
    grid-column: span 1;
  }
}
</style>
