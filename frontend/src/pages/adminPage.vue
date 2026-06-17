<template>
  <div class="admin-page" :class="{ 'is-embedded': embedded }">
    <main class="admin-shell">
      <div v-if="error" class="notice danger" role="alert">
        <AlertTriangle class="notice-icon" aria-hidden="true" />
        <span>{{ error }}</span>
      </div>

      <div v-if="feedback" class="notice ok" role="status">
        <CheckCircle2 class="notice-icon" aria-hidden="true" />
        <span>{{ feedback }}</span>
      </div>

      <div v-if="loading" class="loading-box" aria-live="polite"></div>

      <template v-else>
        <section v-if="!isMicroProfile" class="simple-panel">
          <div class="panel-heading">
            <p class="eyebrow">{{ profileType ? profileTypeLabel : 'Profil non renseigne' }}</p>
            <h2>Cette page est surtout utile au statut micro-entrepreneur.</h2>
          </div>
          <p class="muted">
            Le profil revendeur se modifie dans Mon compte. Une fois le statut micro-entrepreneur
            renseigne, cette page affichera le montant URSSAF a recopier.
          </p>
          <RouterLink :to="{ name: 'account' }" class="icon-button secondary">
            <span>{{ profileType ? 'Voir Mon compte' : 'Completer Mon compte' }}</span>
          </RouterLink>
        </section>

        <template v-else>
          <nav class="tab-nav" aria-label="Sections administratives" role="tablist">
            <button
              v-for="tab in tabItems"
              :key="tab.id"
              type="button"
              class="tab-button"
              :class="{ 'is-active': activeTab === tab.id }"
              role="tab"
              :aria-selected="activeTab === tab.id"
              @click="activeTab = tab.id"
            >
              <component :is="tab.icon" class="tab-icon" aria-hidden="true" />
              <span>{{ tab.label }}</span>
            </button>
          </nav>

          <section v-if="activeTab === 'general'" class="tab-panel">
            <div class="panel-heading">
              <p class="eyebrow">Generalites</p>
              <h2>Informations connues de la micro-entreprise</h2>
            </div>

            <div class="info-grid">
              <article v-for="row in generalRows" :key="row.label" class="info-row">
                <span>{{ row.label }}</span>
                <strong v-if="!row.missing">{{ row.value }}</strong>
                <RouterLink v-else :to="{ name: 'account' }" class="missing-link">
                  A completer dans Mon compte
                </RouterLink>
              </article>
            </div>
          </section>

          <section v-else-if="activeTab === 'urssaf'" class="tab-panel urssaf-panel">
            <div class="urssaf-header">
              <div class="panel-heading">
                <p class="eyebrow">Declaration URSSAF</p>
                <h2>Montant a declarer</h2>
                <p class="muted">
                  Recopie le chiffre d'affaires brut encaisse sur la periode. Les couts et achats
                  ne se deduisent pas dans cette case.
                </p>
              </div>

              <div class="period-card" aria-label="Periode declarative">
                <div class="period-card-title">
                  <CalendarDays class="button-icon" aria-hidden="true" />
                  <span>Periode</span>
                </div>
                <div class="period-controls">
                  <label>
                    Debut
                    <input v-model="periodStart" type="date" @change="handlePeriodChange" />
                  </label>
                  <label>
                    Fin
                    <input v-model="periodEnd" type="date" @change="handlePeriodChange" />
                  </label>
                </div>
                <button
                  type="button"
                  class="icon-button ghost icon-button--compact"
                  :disabled="summaryLoading"
                  @click="loadSummary"
                >
                  <RefreshCw class="button-icon" :class="{ spinning: summaryLoading }" aria-hidden="true" />
                  <span>Recalculer</span>
                </button>
              </div>
            </div>

            <div class="urssaf-summary">
              <article class="amount-block">
                <span>Montant a saisir</span>
                <strong>{{ money(summary?.periodRevenue) }}</strong>
                <p>Vente de marchandises / achat-revente</p>
              </article>

              <dl class="declaration-list">
                <div>
                  <dt>Periode retenue</dt>
                  <dd>{{ periodLabel }}</dd>
                </div>
                <div>
                  <dt>Periodicite</dt>
                  <dd>{{ declarationFrequencyLabel(profile?.declarationFrequency, 'Inconnue') }}</dd>
                </div>
                <div>
                  <dt>Case a remplir</dt>
                  <dd>Vente de marchandises</dd>
                </div>
                <div>
                  <dt>Base de calcul</dt>
                  <dd>Chiffre d'affaires brut encaisse</dd>
                </div>
              </dl>
            </div>

            <div class="rule-line">
              <Info class="notice-icon" aria-hidden="true" />
              <span>Ne deduis pas les achats, frais, commissions, livraison, charges ou autres couts.</span>
            </div>

            <div v-if="visibleAlerts.length" class="alert-list">
              <div
                v-for="alert in visibleAlerts"
                :key="`${alert.title}-${alert.message}`"
                class="inline-alert"
                :class="alertClass(alert.severity)"
              >
                <AlertTriangle v-if="alert.severity !== 'info'" class="notice-icon" aria-hidden="true" />
                <Info v-else class="notice-icon" aria-hidden="true" />
                <div>
                  <strong>{{ alert.title }}</strong>
                  <p>{{ alert.message }}</p>
                </div>
              </div>
            </div>

            <div class="primary-actions">
              <button type="button" class="icon-button primary" :disabled="summaryLoading" @click="copyRevenue">
                <Copy class="button-icon" aria-hidden="true" />
                <span>Copier le montant</span>
              </button>

              <a
                class="icon-button secondary"
                href="https://www.autoentrepreneur.urssaf.fr/"
                target="_blank"
                rel="noreferrer"
              >
                <ExternalLink class="button-icon" aria-hidden="true" />
                <span>Ouvrir l'URSSAF</span>
              </a>

              <button
                v-if="hasDocument('urssaf-summary')"
                type="button"
                class="icon-button ghost"
                :disabled="generating === 'urssaf-summary'"
                @click="generateDocument('urssaf-summary')"
              >
                <Download class="button-icon" aria-hidden="true" />
                <span>{{ generating === 'urssaf-summary' ? 'Generation...' : 'Telecharger une fiche de saisie' }}</span>
              </button>
            </div>
          </section>

          <section v-else-if="activeTab === 'documents'" class="tab-panel">
            <div class="panel-heading">
              <p class="eyebrow">Documents</p>
              <h2>Aides preparatoires</h2>
              <p class="muted">
                Ces documents sont des aides preparatoires. Le document officiel URSSAF est celui
                genere apres validation sur le portail URSSAF.
              </p>
            </div>

            <div class="document-list">
              <article v-for="document in availableDocuments" :key="document.id" class="document-row">
                <FileText class="document-icon" aria-hidden="true" />
                <div>
                  <h3>{{ documentTitle(document) }}</h3>
                  <p>{{ documentDescription(document) }}</p>
                </div>
                <button
                  type="button"
                  class="icon-button ghost"
                  :disabled="generating === document.id"
                  @click="generateDocument(document.id)"
                >
                  <Download class="button-icon" aria-hidden="true" />
                  <span>{{ generating === document.id ? 'Generation...' : 'Telecharger' }}</span>
                </button>
              </article>
            </div>
          </section>
        </template>
      </template>
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import {
  AlertTriangle,
  CalendarDays,
  CheckCircle2,
  Copy,
  Download,
  ExternalLink,
  FileText,
  Info,
  RefreshCw,
} from 'lucide-vue-next'
import AdminService from '@/services/AdminService'

defineProps({
  embedded: {
    type: Boolean,
    default: false,
  },
})

const PROFILE = Object.freeze({
  individual: 'PARTICULIER',
  micro: 'MICRO_ENTREPRISE',
})

const DOCUMENT_ORDER = ['urssaf-summary', 'receipts-register', 'purchases-register', 'fiscal-summary']
const DOCUMENT_TITLES = Object.freeze({
  'urssaf-summary': 'Fiche de saisie URSSAF',
  'receipts-register': 'Registre des recettes',
  'purchases-register': 'Registre des achats',
  'fiscal-summary': 'Recap annuel fiscal',
})
const DOCUMENT_DESCRIPTIONS = Object.freeze({
  'urssaf-summary': 'Montant brut encaisse a recopier sur autoentrepreneur.urssaf.fr.',
  'receipts-register': "Liste chronologique des recettes connues dans l'application.",
  'purchases-register': "Recapitulatif des achats rattaches a l'activite d'achat-revente.",
  'fiscal-summary': 'Aide preparatoire pour la declaration annuelle.',
})

const today = new Date()
const initialYear = today.getFullYear()

const loading = ref(true)
const summaryLoading = ref(false)
const generating = ref('')
const error = ref('')
const feedback = ref('')
const profile = ref(null)
const summary = ref(null)
const documents = ref([])
const activeTab = ref('urssaf')
const periodTouched = ref(false)
const periodStart = ref(dateToInputValue(new Date(today.getFullYear(), today.getMonth(), 1)))
const periodEnd = ref(dateToInputValue(today))

const profileType = computed(() => profile.value?.profileType || '')
const isMicroProfile = computed(() => profileType.value === PROFILE.micro)
const profileTypeLabel = computed(() => {
  if (profileType.value === PROFILE.micro) return 'Micro-entrepreneur'
  if (profileType.value === PROFILE.individual) return 'Particulier'
  return 'Non renseigne'
})
const selectedYear = computed(() => Number(String(periodStart.value || '').slice(0, 4)) || initialYear)
const periodLabel = computed(() => `${formatDate(periodStart.value)} - ${formatDate(periodEnd.value)}`)
const sirenValue = computed(() => cleanText(profile.value?.siren) || deriveSiren(profile.value?.siret))
const generalRows = computed(() => [
  row('SIRET', profile.value?.siret, true),
  row('SIREN', sirenValue.value, true),
  row('Nom / nom commercial', profile.value?.tradeName || profile.value?.displayName, true),
  row('Adresse administrative', profile.value?.address, true),
  row('Date de debut d activite', formatDate(profile.value?.activityStartDate), Boolean(profile.value?.activityStartDate)),
  row('Activite principale', profile.value?.mainActivity, true),
  row('Categorie fiscale / sociale', profile.value?.fiscalRegime, true),
  row(
    'Periodicite URSSAF',
    declarationFrequencyLabel(profile.value?.declarationFrequency, ''),
    isKnownFrequency(profile.value?.declarationFrequency),
  ),
  row('Versement liberatoire', triStateLabel(profile.value?.withholdingTaxOption), true),
  row('Franchise en base de TVA', triStateLabel(profile.value?.vatFranchise), true),
])
const documentsById = computed(() => {
  const map = new Map()
  for (const document of documents.value || []) {
    if (document?.id) map.set(document.id, document)
  }
  return map
})
const availableDocuments = computed(() =>
  DOCUMENT_ORDER.map((id) => documentsById.value.get(id)).filter(Boolean),
)
const isDocumentsTabAvailable = computed(() => isMicroProfile.value && availableDocuments.value.length > 0)
const tabItems = computed(() => {
  const tabs = [
    { id: 'general', label: 'Generalites', icon: Info },
    { id: 'urssaf', label: 'URSSAF', icon: AlertTriangle },
  ]
  if (isDocumentsTabAvailable.value) {
    tabs.push({ id: 'documents', label: 'Documents', icon: FileText })
  }
  return tabs
})
const visibleAlerts = computed(() => {
  if (!isMicroProfile.value) return []
  return Array.isArray(summary.value?.alerts) ? summary.value.alerts : []
})

watch(tabItems, (items) => {
  if (!items.some((tab) => tab.id === activeTab.value)) {
    activeTab.value = items[0]?.id || 'general'
  }
})

onMounted(loadAll)

async function loadAll() {
  loading.value = true
  error.value = ''
  feedback.value = ''
  try {
    profile.value = await AdminService.administrativeProfile()
    applyDefaultPeriodForProfile()
    await Promise.all([loadSummary(), loadDocuments()])
    if (isMicroProfile.value) activeTab.value = 'urssaf'
  } catch (err) {
    error.value = errorMessage(err, 'Impossible de charger la page administrative.')
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
    if (summary.value?.profile) profile.value = summary.value.profile
  } catch (err) {
    error.value = errorMessage(err, 'Impossible de calculer le montant a declarer.')
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

function handlePeriodChange() {
  periodTouched.value = true
  void loadSummary()
}

async function copyRevenue() {
  const value = formatAmountForCopy(summary.value?.periodRevenue)
  try {
    await navigator.clipboard.writeText(value)
    feedback.value = `Montant copie : ${value} EUR.`
  } catch {
    feedback.value = `Montant a recopier : ${value} EUR.`
  }
}

async function generateDocument(documentId) {
  generating.value = documentId
  error.value = ''
  feedback.value = ''
  try {
    const filename = await AdminService.generateAdministrativeDocument(documentId, {
      periodStart: periodStart.value,
      periodEnd: periodEnd.value,
      year: selectedYear.value,
    })
    feedback.value = `Document genere : ${filename}.`
  } catch (err) {
    error.value = errorMessage(err, 'Impossible de generer le document.')
  } finally {
    generating.value = ''
  }
}

function applyDefaultPeriodForProfile() {
  if (periodTouched.value) return
  if (profile.value?.declarationFrequency === 'QUARTERLY') {
    const quarterMonth = Math.floor(today.getMonth() / 3) * 3
    periodStart.value = dateToInputValue(new Date(today.getFullYear(), quarterMonth, 1))
    periodEnd.value = dateToInputValue(today)
    return
  }
  periodStart.value = dateToInputValue(new Date(today.getFullYear(), today.getMonth(), 1))
  periodEnd.value = dateToInputValue(today)
}

function hasDocument(documentId) {
  return documentsById.value.has(documentId)
}

function documentTitle(document) {
  return DOCUMENT_TITLES[document.id] || document.title || 'Document'
}

function documentDescription(document) {
  return DOCUMENT_DESCRIPTIONS[document.id] || document.description || 'Aide preparatoire.'
}

function row(label, rawValue, present) {
  const value = cleanText(rawValue)
  return {
    label,
    value: value || '',
    missing: !present || !value,
  }
}

function money(value) {
  const amount = Number(value || 0)
  return new Intl.NumberFormat('fr-FR', { style: 'currency', currency: 'EUR' }).format(amount)
}

function formatAmountForCopy(value) {
  return Number(value || 0).toFixed(2).replace('.', ',')
}

function formatDate(value) {
  if (!value) return ''
  return new Intl.DateTimeFormat('fr-FR', { day: '2-digit', month: '2-digit', year: 'numeric' }).format(
    new Date(`${value}T00:00:00`),
  )
}

function dateToInputValue(date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function deriveSiren(siret) {
  const text = String(siret || '').trim()
  return text.length >= 9 ? text.slice(0, 9) : ''
}

function cleanText(value) {
  const text = value == null ? '' : String(value).trim()
  return text === '-' ? '' : text
}

function declarationFrequencyLabel(value, fallback = 'A completer') {
  if (value === 'MONTHLY') return 'Mensuelle'
  if (value === 'QUARTERLY') return 'Trimestrielle'
  return fallback
}

function isKnownFrequency(value) {
  return value === 'MONTHLY' || value === 'QUARTERLY'
}

function triStateLabel(value) {
  if (value === 'YES' || value === true) return 'Oui'
  if (value === 'NO' || value === false) return 'Non'
  return 'Inconnu'
}

function alertClass(severity) {
  if (severity === 'danger') return 'danger'
  if (severity === 'info') return 'info'
  return 'warning'
}

function errorMessage(errorObject, fallback) {
  return errorObject?.response?.data?.message || errorObject?.message || fallback
}
</script>

<style scoped>
.admin-page {
  min-height: calc(100dvh - 7rem);
  margin-inline: calc(clamp(16px, 2vw, 32px) * -1);
  margin-top: 0.75rem;
  margin-bottom: 4rem;
  background: #f7f4ee;
  color: #172033;
}

.admin-page.is-embedded {
  min-height: auto;
  margin: 0;
  background: transparent;
}

.admin-shell {
  display: grid;
  width: min(100%, 1180px);
  gap: 0.9rem;
  margin-inline: auto;
  padding: clamp(1rem, 2vw, 1.5rem) clamp(16px, 2vw, 32px) clamp(4rem, 8vw, 6rem);
}

.admin-page.is-embedded .admin-shell {
  width: 100%;
  padding: 0;
}

.tab-panel,
.simple-panel,
.loading-box {
  border: 1px solid #d8e1eb;
  border-radius: 8px;
  background: #ffffff;
  box-shadow: 0 14px 34px rgba(15, 23, 42, 0.05);
}

.eyebrow,
.info-row span,
.declaration-list dt,
.amount-block span,
.document-row p,
.muted {
  color: #64748b;
}

.eyebrow {
  margin: 0 0 0.25rem;
  font-size: 0.72rem;
  font-weight: 900;
  letter-spacing: 0;
  text-transform: uppercase;
}

h1,
h2,
h3,
p,
dl,
dd {
  margin: 0;
}

h1 {
  color: #0f172a;
  font-size: 1.35rem;
  line-height: 1.15;
  letter-spacing: 0;
}

h2 {
  color: #0f172a;
  font-size: 1.12rem;
  line-height: 1.25;
  letter-spacing: 0;
}

h3 {
  color: #172033;
  font-size: 0.95rem;
  line-height: 1.25;
}

.tab-nav {
  display: inline-flex;
  width: fit-content;
  max-width: 100%;
  gap: 0.35rem;
  border: 1px solid #d8e1eb;
  border-radius: 8px;
  background: #ffffff;
  padding: 0.35rem;
  overflow-x: auto;
}

.tab-button {
  display: inline-flex;
  min-height: 38px;
  align-items: center;
  justify-content: center;
  gap: 0.4rem;
  border: 0;
  border-radius: 6px;
  background: transparent;
  color: #475569;
  padding: 0 0.8rem;
  font: inherit;
  font-size: 0.84rem;
  font-weight: 900;
  white-space: nowrap;
  cursor: pointer;
}

.tab-button.is-active {
  background: #0f766e;
  color: #ffffff;
}

.tab-icon {
  width: 15px;
  height: 15px;
}

.tab-panel,
.simple-panel {
  display: grid;
  gap: 1rem;
  padding: clamp(1rem, 2vw, 1.25rem);
}

.panel-heading {
  display: grid;
  gap: 0.35rem;
}

.panel-heading.split {
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: end;
  gap: 1rem;
}

.muted {
  max-width: 760px;
  font-size: 0.9rem;
  font-weight: 650;
  line-height: 1.45;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.75rem;
}

.info-row {
  display: grid;
  gap: 0.35rem;
  min-height: 76px;
  border: 1px solid #dce5f1;
  border-radius: 8px;
  background: #f8fafc;
  padding: 0.85rem;
}

.info-row span {
  font-size: 0.78rem;
  font-weight: 850;
}

.info-row strong,
.missing-link {
  color: #0f172a;
  font-size: 0.94rem;
  font-weight: 850;
  line-height: 1.3;
}

.missing-link {
  color: #0f766e;
  text-decoration: none;
}

.urssaf-panel {
  gap: 1.05rem;
}

.urssaf-header {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(320px, 410px);
  gap: 1rem;
  align-items: start;
}

.period-card {
  display: grid;
  gap: 0.75rem;
  border: 1px solid #dce5f1;
  border-radius: 8px;
  background: #f8fafc;
  padding: 0.85rem;
}

.period-card-title {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  color: #334155;
  font-size: 0.78rem;
  font-weight: 900;
  text-transform: uppercase;
}

.period-controls {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.55rem;
}

.period-controls label {
  display: grid;
  gap: 0.28rem;
  color: #475569;
  font-size: 0.74rem;
  font-weight: 900;
}

input {
  width: 100%;
  min-height: 38px;
  box-sizing: border-box;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  background: #ffffff;
  color: #0f172a;
  font: inherit;
  font-size: 0.84rem;
  padding: 0 0.65rem;
  outline: none;
}

input:focus {
  border-color: #0f766e;
  box-shadow: 0 0 0 3px rgba(15, 118, 110, 0.14);
}

.urssaf-summary {
  display: grid;
  grid-template-columns: minmax(260px, 0.72fr) minmax(0, 1.28fr);
  gap: 0.8rem;
  align-items: stretch;
}

.amount-block {
  display: grid;
  align-content: center;
  gap: 0.34rem;
  min-height: 190px;
  border: 1px solid #a7e3d7;
  border-radius: 8px;
  background: linear-gradient(180deg, #f9fffd 0%, #edf8f5 100%);
  padding: clamp(1rem, 2vw, 1.2rem);
}

.amount-block span,
.amount-block p {
  font-size: 0.84rem;
  font-weight: 900;
}

.amount-block strong {
  color: #0f766e;
  font-size: clamp(2.35rem, 4vw, 3.4rem);
  line-height: 1;
  letter-spacing: 0;
}

.amount-block p {
  color: #115e59;
}

.declaration-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.6rem;
}

.declaration-list div {
  display: grid;
  align-content: start;
  gap: 0.32rem;
  min-height: 92px;
  border: 1px solid #dce5f1;
  border-radius: 8px;
  background: #fbfdff;
  padding: 0.8rem;
}

.declaration-list dt {
  font-size: 0.75rem;
  font-weight: 850;
}

.declaration-list dd {
  color: #0f172a;
  font-size: 0.9rem;
  font-weight: 850;
  line-height: 1.3;
}

.rule-line,
.notice,
.inline-alert,
.primary-actions,
.document-row {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.rule-line {
  align-items: flex-start;
  border: 1px solid #c8eadf;
  border-radius: 8px;
  background: #f5fbf8;
  color: #0f766e;
  padding: 0.75rem 0.85rem;
  font-size: 0.86rem;
  font-weight: 850;
}

.alert-list {
  display: grid;
  gap: 0.6rem;
}

.inline-alert {
  align-items: flex-start;
  border-radius: 8px;
  padding: 0.72rem 0.82rem;
}

.inline-alert strong,
.inline-alert p {
  font-size: 0.86rem;
  line-height: 1.35;
}

.inline-alert p {
  margin-top: 0.12rem;
  font-weight: 650;
}

.inline-alert.warning,
.notice.warning {
  border: 1px solid #f6d982;
  background: #fffaf0;
  color: #92400e;
}

.inline-alert.danger,
.notice.danger {
  border: 1px solid #fecdd3;
  background: #fff1f2;
  color: #be123c;
}

.inline-alert.info {
  border: 1px solid #c8d9f4;
  background: #f5f8ff;
  color: #1e40af;
}

.notice {
  border-radius: 8px;
  padding: 0.8rem 1rem;
  font-weight: 800;
}

.notice.ok {
  border: 1px solid #bbf7d0;
  background: #f0fdf4;
  color: #047857;
}

.primary-actions {
  flex-wrap: wrap;
  padding-top: 0.1rem;
}

.icon-button {
  display: inline-flex;
  min-height: 40px;
  align-items: center;
  justify-content: center;
  gap: 0.45rem;
  border: 0;
  border-radius: 8px;
  padding: 0 0.85rem;
  font: inherit;
  font-size: 0.84rem;
  font-weight: 900;
  text-decoration: none;
  cursor: pointer;
}

.icon-button.primary {
  background: #0f766e;
  color: #ffffff;
}

.icon-button.secondary {
  background: #e8f6f3;
  color: #0f766e;
}

.icon-button.ghost {
  background: #f1f5f9;
  color: #334155;
}

.icon-button--compact {
  min-height: 36px;
  width: fit-content;
  justify-self: end;
  padding-inline: 0.75rem;
}

.icon-button:disabled {
  cursor: not-allowed;
  opacity: 0.58;
}

.button-icon,
.notice-icon,
.document-icon {
  width: 16px;
  height: 16px;
  flex: 0 0 auto;
}

.document-list {
  display: grid;
  gap: 0.7rem;
}

.document-row {
  align-items: flex-start;
  border: 1px solid #dce5f1;
  border-radius: 8px;
  background: #f8fafc;
  padding: 0.85rem;
}

.document-row > div {
  min-width: 0;
  flex: 1 1 auto;
}

.document-row p {
  margin-top: 0.2rem;
  font-size: 0.84rem;
  line-height: 1.35;
  font-weight: 650;
}

.document-icon {
  margin-top: 0.12rem;
  color: #0f766e;
}

.loading-box {
  height: 220px;
  background: linear-gradient(90deg, #e5e7eb, #f8fafc, #e5e7eb);
  background-size: 220% 100%;
  animation: shimmer 1.3s infinite;
}

.spinning {
  animation: spin 850ms linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

@keyframes shimmer {
  0% {
    background-position: 120% 0;
  }
  100% {
    background-position: -120% 0;
  }
}

@media (max-width: 980px) {
  .urssaf-header,
  .urssaf-summary {
    grid-template-columns: 1fr;
  }

  .icon-button--compact {
    justify-self: stretch;
    width: 100%;
  }
}

@media (max-width: 700px) {
  .info-grid,
  .declaration-list,
  .period-controls {
    grid-template-columns: 1fr;
  }

  .tab-nav {
    width: 100%;
  }

  .tab-button {
    flex: 1 0 auto;
  }

  .icon-button,
  .document-row .icon-button {
    width: 100%;
  }

  .document-row {
    display: grid;
  }
}
</style>
