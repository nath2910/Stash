<template>
  <div class="admin-page" :class="{ 'is-embedded': embedded }">
    <section class="admin-workspace">
      <header class="admin-header">
        <div>
          <p class="admin-eyebrow">Dashboard administratif</p>
          <h1>Administratif</h1>
          <p class="admin-subtitle">{{ dashboardSubtitle }}</p>
        </div>

        <div class="header-tools">
          <button type="button" class="icon-button soft" :disabled="saving" @click="saveNow">
            <RefreshCw class="button-icon" aria-hidden="true" />
            <span>{{ saving ? 'Sauvegarde...' : 'Sauvegarder' }}</span>
          </button>
        </div>
      </header>

      <div v-if="loading" class="loading-grid" aria-live="polite">
        <div v-for="item in 4" :key="item" class="skeleton-block"></div>
      </div>

      <template v-else>
        <div v-if="apiWarning || adminError" class="notice-row">
          <AlertTriangle class="notice-icon" aria-hidden="true" />
          <span>{{ apiWarning || adminError }}</span>
        </div>

        <div v-if="feedback" class="success-row">
          <CheckCircle2 class="notice-icon" aria-hidden="true" />
          <span>{{ feedback }}</span>
        </div>

        <section class="profile-context section-block">
          <div class="section-heading">
            <div>
              <p class="admin-eyebrow">Profil actif</p>
              <h2>{{ currentProfile.label }}</h2>
            </div>
            <RouterLink class="icon-button soft profile-edit-link" :to="{ name: 'account' }">
              <UserRoundCheck class="button-icon" aria-hidden="true" />
              <span>Changer dans mon profil</span>
            </RouterLink>
          </div>

          <div class="profile-trait-row" aria-label="Particularites du profil">
            <span
              v-for="trait in currentProfile.traits"
              :key="trait"
              class="profile-trait-pill"
            >
              {{ trait }}
            </span>
          </div>
        </section>

        <section class="summary-grid" aria-label="Résumé personnalisé">
          <article
            v-for="card in dashboardCards"
            :key="card.label"
            class="summary-tile"
            :class="card.className"
          >
            <span>{{ card.label }}</span>
            <strong>{{ card.value }}</strong>
            <small v-if="card.detail">{{ card.detail }}</small>
          </article>
        </section>

        <section v-if="showThresholdPanel" class="section-block threshold-panel">
          <div class="section-heading">
            <div>
              <p class="admin-eyebrow">Seuils</p>
              <h2>{{ thresholdSectionTitle }}</h2>
            </div>
            <span class="section-hint">{{ thresholdSummary }}</span>
          </div>

          <div class="threshold-grid">
            <article
              v-for="threshold in dashboardThresholds"
              :key="threshold.key"
              class="threshold-card"
              :class="{ 'is-reached': threshold.isReached }"
            >
              <div class="threshold-card-head">
                <span>{{ threshold.label }}</span>
                <strong>{{ threshold.remainingLabel }}</strong>
              </div>
              <div class="threshold-progress" aria-hidden="true">
                <span :style="{ width: `${threshold.progress}%` }"></span>
              </div>
              <small>{{ threshold.currentLabel }} / {{ threshold.limitLabel }}</small>
            </article>
          </div>
        </section>

        <section v-if="isSimpleResellerProfile" class="section-block declaration-focus">
          <div class="section-heading">
            <div>
              <p class="admin-eyebrow">Déclaration</p>
              <h2>CA à déclarer</h2>
            </div>
            <span class="section-hint">{{ activeDeclarationStatus }}</span>
          </div>

          <div class="declaration-layout">
            <article class="declaration-card primary-declaration">
              <span>{{ activeDeclaration.periodLabel }}</span>
              <strong>{{ money(activeDeclaration.revenue) }}</strong>
              <div class="declaration-actions">
                <button type="button" class="icon-button primary" @click="prepareDeclaration">
                  <FileText class="button-icon" aria-hidden="true" />
                  <span>Exporter récap</span>
                </button>
                <button type="button" class="icon-button soft" @click="markDeclarationDone">
                  <CheckCircle2 class="button-icon" aria-hidden="true" />
                  <span>Marquer déclaré</span>
                </button>
              </div>
            </article>

            <article class="declaration-card">
              <span>Cotisations estimées</span>
              <strong>{{ money(activeDeclaration.socialEstimate) }}</strong>
              <p>{{ SOCIAL_RATE_GOODS }}% vente marchandises</p>
            </article>

            <article class="declaration-card">
              <span>Échéance</span>
              <h3>{{ formatDate(activeDeclaration.dueDate) }}</h3>
              <p>{{ declarationModeLabel }}</p>
            </article>
          </div>
        </section>

        <main class="admin-main-grid">
          <section class="section-block">
            <div class="section-heading">
              <div>
                <p class="admin-eyebrow">Actions</p>
                <h2>{{ actionSectionTitle }}</h2>
              </div>
              <span class="section-hint">{{ tasksToDoCount }} à faire</span>
            </div>

            <div class="task-stack">
              <article v-for="task in visibleTasks" :key="task.id" class="task-card">
                <div class="task-status-dot" :class="task.status"></div>
                <div class="task-content">
                  <div class="task-title-row">
                    <h3>{{ task.title }}</h3>
                    <span class="status-badge" :class="task.status">
                      {{ taskStatusLabel(task.status) }}
                    </span>
                  </div>
                  <p>{{ task.description }}</p>
                  <div v-if="!isStarterProfile || task.dueDate" class="task-meta">
                    <span v-if="!isStarterProfile" :class="task.obligation">
                      {{ obligationLabel(task.obligation) }}
                    </span>
                    <span v-if="task.dueDate">Échéance {{ formatDate(task.dueDate) }}</span>
                    <span v-if="!isStarterProfile">{{ task.sourceLabel }}</span>
                  </div>
                </div>
                <div class="task-actions">
                  <button type="button" class="icon-button soft" @click="handleTaskAction(task)">
                    <component :is="taskActionIcon(task)" class="button-icon" aria-hidden="true" />
                    <span>{{ task.actionLabel }}</span>
                  </button>
                  <button
                    v-if="task.status !== 'done'"
                    type="button"
                    class="icon-only"
                    title="Marquer comme fait"
                    @click="markTaskDone(task.id)"
                  >
                    <CheckCircle2 class="button-icon" aria-hidden="true" />
                  </button>
                </div>
              </article>

              <div v-if="!visibleTasks.length" class="empty-state">
                Aucune tâche applicable à ce profil pour le moment.
              </div>
            </div>
          </section>

          <aside class="section-block form-panel">
            <div class="section-heading compact">
              <div>
                <p class="admin-eyebrow">Saisie</p>
                <h2>{{ settingsPanelTitle }}</h2>
              </div>
              <span
                v-if="!isStarterProfile"
                class="status-badge"
                :class="missingRequiredFields.length ? 'todo' : 'done'"
              >
                {{ missingRequiredFields.length ? `${missingRequiredFields.length} manquant(s)` : 'Complet' }}
              </span>
            </div>

            <div v-if="currentProfile.requiredFields.length" class="form-grid">
              <label v-if="currentProfile.requiredFields.includes('tradeName')">
                Nom commercial
                <input v-model.trim="settings.tradeName" placeholder="Nom de la boutique" />
              </label>
              <label v-if="currentProfile.requiredFields.includes('legalName')">
                Nom légal
                <input v-model.trim="settings.legalName" placeholder="Entreprise individuelle, société..." />
              </label>
              <label v-if="currentProfile.requiredFields.includes('siren')">
                SIREN
                <input v-model.trim="settings.siren" inputmode="numeric" placeholder="9 chiffres" />
              </label>
              <label v-if="currentProfile.requiredFields.includes('siret')">
                SIRET
                <input v-model.trim="settings.siret" inputmode="numeric" placeholder="14 chiffres" />
              </label>
              <label v-if="currentProfile.requiredFields.includes('address')" class="wide">
                Adresse administrative
                <input v-model.trim="settings.address" placeholder="Adresse complète" />
              </label>
              <label v-if="currentProfile.requiredFields.includes('email')">
                Email administratif
                <input v-model.trim="settings.email" type="email" placeholder="contact@exemple.fr" />
              </label>
              <label v-if="currentProfile.requiredFields.includes('declarationFrequency')">
                Déclarations
                <select v-model="settings.declarationFrequency">
                  <option value="monthly">Mensuelles</option>
                  <option value="quarterly">Trimestrielles</option>
                </select>
              </label>
              <label v-if="currentProfile.requiredFields.includes('vatMode')">
                TVA
                <select v-model="settings.vatMode">
                  <option value="franchise">Franchise en base</option>
                  <option value="liable">TVA collectée</option>
                  <option value="unknown">À vérifier</option>
                </select>
              </label>
            </div>

            <div class="switch-stack">
              <label class="switch-row">
                <input v-model="settings.usesPlatforms" type="checkbox" />
                <span>
                  <strong>Plateformes</strong>
                  <small v-if="!isStarterProfile">Active le suivi des relevés et récapitulatifs.</small>
                </span>
              </label>
              <label v-if="!isStarterProfile && !isSimpleResellerProfile" class="switch-row">
                <input v-model="settings.sellsToProfessionals" type="checkbox" />
                <span>
                  <strong>Clients professionnels</strong>
                  <small>Met la facturation au premier plan.</small>
                </span>
              </label>
              <label class="switch-row">
                <input v-model="settings.buysFromIndividuals" type="checkbox" />
                <span>
                  <strong>Achats particuliers</strong>
                  <small v-if="!isStarterProfile">Renforce le suivi de provenance.</small>
                </span>
              </label>
              <label v-if="currentProfile.key === 'independent_shop'" class="switch-row">
                <input v-model="settings.hasCashSoftware" type="checkbox" />
                <span>
                  <strong>Logiciel ou système de caisse</strong>
                  <small>Affiche le contrôle caisse si vous l'utilisez.</small>
                </span>
              </label>
            </div>

            <div class="metrics-strip">
              <article>
                <span>CA année</span>
                <strong>{{ money(inventoryMetrics.currentYearRevenue) }}</strong>
              </article>
              <article>
                <span>Justificatifs</span>
                <strong>{{ inventoryMetrics.missingProofs }}</strong>
                <small>manquant(s)</small>
              </article>
            </div>

            <div v-if="showItemProofPanel" class="proof-panel">
              <div class="proof-panel-head">
                <h3>Justificatifs par item</h3>
                <span class="status-badge" :class="inventoryMetrics.missingProofs ? 'todo' : 'done'">
                  {{ inventoryMetrics.missingProofs }}
                </span>
              </div>

              <div v-if="missingProofItems.length" class="proof-item-list">
                <RouterLink
                  v-for="item in missingProofItems"
                  :key="item.id || itemLabel(item)"
                  class="proof-item-row"
                  :to="inventoryItemRoute(item)"
                >
                  <span>{{ itemLabel(item) }}</span>
                  <small>{{ money(prixRetailOf(item)) }}</small>
                  <strong>Ajouter</strong>
                </RouterLink>
              </div>

              <div v-else class="empty-state compact-empty">
                Tous les items ont une pièce jointe.
              </div>
            </div>
          </aside>
        </main>

        <section v-if="visibleDocuments.length" class="section-block">
          <div class="section-heading">
            <div>
              <p class="admin-eyebrow">Documents</p>
              <h2>{{ documentsSectionTitle }}</h2>
            </div>
            <button
              v-if="!isStarterProfile && !isSimpleResellerProfile"
              type="button"
              class="icon-button soft"
              @click="generateDocumentById('accounting_export')"
            >
              <Download class="button-icon" aria-hidden="true" />
              <span>Export global</span>
            </button>
          </div>

          <div class="document-grid">
            <article
              v-for="documentItem in visibleDocuments"
              :key="documentItem.id"
              class="document-card"
              :class="documentItem.status"
            >
              <div class="document-card-head">
                <FileText class="document-icon" aria-hidden="true" />
                <span class="status-badge" :class="documentItem.status">
                  {{ documentStatusLabel(documentItem) }}
                </span>
              </div>
              <h3>{{ documentItem.title }}</h3>
              <p v-if="!isStarterProfile">{{ documentItem.description }}</p>
              <div v-if="!isStarterProfile" class="task-meta">
                <span :class="documentItem.requirement">{{ obligationLabel(documentItem.requirement) }}</span>
                <span>{{ documentItem.format }}</span>
              </div>
              <div class="document-actions">
                <button
                  v-if="documentItem.canGenerate"
                  type="button"
                  class="icon-button soft"
                  :disabled="documentItem.requiresIdentity && missingRequiredFields.length > 0"
                  @click="generateDocument(documentItem)"
                >
                  <Download class="button-icon" aria-hidden="true" />
                  <span>Générer</span>
                </button>
                <button
                  v-if="documentItem.canImport && !isStarterProfile && !isSimpleResellerProfile"
                  type="button"
                  class="icon-button soft"
                  @click="requestImport(documentItem.id)"
                >
                  <Upload class="button-icon" aria-hidden="true" />
                  <span>Importer</span>
                </button>
              </div>
              <small v-if="documentItem.requiresIdentity && missingRequiredFields.length > 0" class="missing-note">
                Complétez les informations nécessaires avant génération.
              </small>
            </article>
          </div>
        </section>

        <section v-if="!isStarterProfile && !isSimpleResellerProfile" class="section-block">
          <div class="section-heading">
            <div>
              <p class="admin-eyebrow">Échéances</p>
              <h2>Déclarations et rappels</h2>
            </div>
            <span class="section-hint">{{ declarationModeLabel }}</span>
          </div>

          <div class="declaration-layout">
            <article class="declaration-card primary-declaration">
              <span>Déclaration CA à préparer</span>
              <h3>{{ activeDeclaration.periodLabel }}</h3>
              <strong>{{ money(activeDeclaration.revenue) }}</strong>
              <p>Cotisations estimées vente marchandises : {{ money(activeDeclaration.socialEstimate) }}</p>
              <div class="declaration-actions">
                <button type="button" class="icon-button primary" @click="prepareDeclaration">
                  <FileText class="button-icon" aria-hidden="true" />
                  <span>Préparer</span>
                </button>
                <button type="button" class="icon-button soft" @click="markDeclarationDone">
                  <CheckCircle2 class="button-icon" aria-hidden="true" />
                  <span>Marquer déclarée</span>
                </button>
              </div>
            </article>

            <article class="declaration-card">
              <span>Échéance estimée</span>
              <h3>{{ formatDate(activeDeclaration.dueDate) }}</h3>
              <p>{{ activeDeclaration.detail }}</p>
            </article>

            <article class="declaration-card">
              <span>TVA</span>
              <h3>{{ vatStatus.label }}</h3>
              <p>{{ vatStatus.detail }}</p>
            </article>
          </div>

          <div class="history-list">
            <div class="history-heading">
              <h3>Historique</h3>
              <span>{{ declarationHistory.length }} entrée(s)</span>
            </div>
            <article v-for="entry in declarationHistory.slice(0, 5)" :key="entry.id" class="history-row">
              <span>{{ entry.periodLabel }}</span>
              <strong>{{ money(entry.revenue) }}</strong>
              <small>{{ formatDate(entry.createdAt) }}</small>
            </article>
            <div v-if="!declarationHistory.length" class="empty-state compact-empty">
              Aucune déclaration préparée ou marquée pour l'instant.
            </div>
          </div>
        </section>

        <section v-if="!isStarterProfile || visibleArchivedDocuments.length" class="section-block">
          <div class="section-heading">
            <div>
              <p class="admin-eyebrow">Archive</p>
              <h2>{{ archiveSectionTitle }}</h2>
            </div>
            <span class="section-hint">{{ visibleArchivedDocuments.length }} document(s)</span>
          </div>

          <div class="archive-table">
            <div class="archive-head">
              <span>Document</span>
              <span>Origine</span>
              <span>Date</span>
              <span>Statut</span>
              <span>Action</span>
            </div>
            <article v-for="documentItem in visibleArchivedDocuments" :key="documentItem.id" class="archive-row">
              <div>
                <strong>{{ documentItem.title }}</strong>
                <small>{{ documentItem.filename }}</small>
              </div>
              <span>{{ originLabel(documentItem.origin) }}</span>
              <span>{{ formatDate(documentItem.createdAt) }}</span>
              <span class="status-badge" :class="documentItem.status">
                {{ archiveStatusLabel(documentItem.status) }}
              </span>
              <button type="button" class="icon-only" title="Télécharger" @click="downloadArchiveDocument(documentItem)">
                <Download class="button-icon" aria-hidden="true" />
              </button>
            </article>
            <div v-if="!visibleArchivedDocuments.length" class="empty-state compact-empty">
              {{ archiveEmptyText }}
            </div>
          </div>
        </section>

      </template>
    </section>

    <input
      ref="fileInput"
      class="visually-hidden"
      type="file"
      @change="handleFileImport"
    />
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import {
  AlertTriangle,
  CheckCircle2,
  Download,
  ExternalLink,
  FileText,
  ListChecks,
  RefreshCw,
  Upload,
  UserRoundCheck,
} from 'lucide-vue-next'
import AdminService from '../services/AdminService.js'
import SnkVenteServices from '../services/SnkVenteServices.js'
import {
  ADMIN_ACTION_PROFILES as SHARED_PROFILE_DEFINITIONS,
  ADMIN_ACTION_STORAGE_KEY,
  DEFAULT_ADMIN_ACTION_SETTINGS,
  normalizeAdminActionProfileKey,
} from '../constants/adminActionProfiles.js'
import { formatDateFR, formatEUR, toNumber } from '../utils/formatters.js'
import { getField, isVendue, prixResellOf, prixRetailOf } from '../utils/snkVente.js'

defineProps({
  embedded: {
    type: Boolean,
    default: false,
  },
})

const STORAGE_KEY = ADMIN_ACTION_STORAGE_KEY
const CURRENT_YEAR = new Date().getFullYear()
const SOCIAL_RATE_GOODS = 12.3
const VAT_BASE_THRESHOLD_GOODS = 85000
const VAT_TOLERANCE_THRESHOLD_GOODS = 93500
const STARTER_REVENUE_THRESHOLD = 2000
const STARTER_PROFIT_THRESHOLD = 305
const STARTER_SALES_THRESHOLD = 30
const CFE_REVENUE_THRESHOLD = 5000
const MICRO_GOODS_THRESHOLD = 203100
const RESELLER_REGISTRY_URL = 'https://www.formulaires.service-public.fr/gf/cerfa_11733_02.do'

const PROFILE_DEFINITIONS = SHARED_PROFILE_DEFINITIONS

const TASK_DEFINITIONS = [
  {
    id: 'keep_purchase_proofs',
    title: 'Ajouter les preuves d’achat',
    description: 'Factures, reçus ou captures.',
    profiles: ['light_reseller', 'regular_reseller', 'strict_reseller', 'independent_shop'],
    obligation: 'required',
    actionType: 'inventory',
    sourceLabel: 'Preuves',
  },
  {
    id: 'monitor_platform_reports',
    title: 'Exporter les relevés plateformes',
    description: 'Vinted, Leboncoin, eBay : garde le récap annuel.',
    profiles: ['light_reseller', 'regular_reseller'],
    obligation: 'recommended',
    actionType: 'mark',
    dueType: 'yearEnd',
    sourceLabel: 'Plateformes',
    when: (settings) => settings.usesPlatforms,
  },
  {
    id: 'complete_identity',
    title: 'Renseigner l’identité professionnelle',
    description: 'Nom légal, SIREN et informations utiles aux documents.',
    profiles: ['regular_reseller', 'strict_reseller', 'independent_shop'],
    obligation: 'required',
    actionType: 'settings',
    sourceLabel: 'Facturation',
  },
  {
    id: 'prepare_ca_declaration',
    title: 'Préparer la déclaration de chiffre d’affaires',
    description: 'Récapitulatif de CA encaissé avant saisie sur le portail officiel.',
    profiles: ['regular_reseller', 'strict_reseller', 'independent_shop'],
    obligation: 'required',
    actionType: 'declaration',
    documentId: 'urssaf_recap',
    dueType: 'declaration',
    sourceLabel: 'Urssaf',
  },
  {
    id: 'revenue_book',
    title: 'Tenir le livre des recettes',
    description: 'Montant, origine, mode de règlement et pièce justificative.',
    profiles: ['regular_reseller', 'strict_reseller', 'independent_shop'],
    obligation: 'required',
    actionType: 'generate',
    documentId: 'sales_register',
    sourceLabel: 'Économie',
  },
  {
    id: 'purchase_register',
    title: 'Tenir le registre des achats',
    description: 'Suivi annuel des achats et références des justificatifs.',
    profiles: ['regular_reseller', 'strict_reseller', 'independent_shop'],
    obligation: 'required',
    actionType: 'generate',
    documentId: 'purchase_register',
    sourceLabel: 'Économie',
    when: (settings, profile) =>
      profile.key !== 'regular_reseller' || settings.buysFromIndividuals || settings.usesPlatforms,
  },
  {
    id: 'invoice_drafts',
    title: 'Préparer les factures ou brouillons',
    description: 'Numérotation, identité, lignes, TVA et mentions à vérifier.',
    profiles: ['strict_reseller', 'independent_shop'],
    obligation: 'required',
    actionType: 'generate',
    documentId: 'invoice_draft',
    sourceLabel: 'Légifrance',
  },
  {
    id: 'secondhand_reseller_declaration',
    title: 'Déclaration revendeur d’occasion',
    description: 'À faire si vous achetez des biens d’occasion à des particuliers.',
    profiles: ['regular_reseller', 'strict_reseller', 'independent_shop'],
    obligation: 'required',
    actionType: 'external',
    url: RESELLER_REGISTRY_URL,
    sourceLabel: 'Préfecture',
    when: (settings) => settings.buysFromIndividuals,
  },
  {
    id: 'vat_watch',
    title: 'Surveiller les seuils TVA',
    description: 'Comparez le CA aux seuils de franchise et de tolérance.',
    profiles: ['strict_reseller', 'independent_shop'],
    obligation: 'required',
    actionType: 'generate',
    documentId: 'vat_watch',
    sourceLabel: 'Impôts',
  },
  {
    id: 'archive_10_years',
    title: 'Archiver les pièces comptables',
    description: 'Factures, registres et justificatifs doivent rester retrouvables.',
    profiles: ['regular_reseller', 'strict_reseller', 'independent_shop'],
    obligation: 'required',
    actionType: 'mark',
    sourceLabel: 'Entreprendre',
  },
  {
    id: 'cash_register',
    title: 'Contrôler le journal de caisse',
    description: 'À suivre si vous utilisez un logiciel ou système de caisse.',
    profiles: ['independent_shop'],
    obligation: 'recommended',
    actionType: 'generate',
    documentId: 'cash_register',
    sourceLabel: 'Impôts',
    when: (settings) => settings.hasCashSoftware,
  },
  {
    id: 'accounting_export',
    title: 'Préparer l’export comptable',
    description: 'Fichier propre pour comptable ou logiciel de gestion.',
    profiles: ['strict_reseller', 'independent_shop'],
    obligation: 'recommended',
    actionType: 'generate',
    documentId: 'accounting_export',
    sourceLabel: 'Entreprendre',
  },
]

const DOCUMENT_DEFINITIONS = [
  {
    id: 'purchase_proofs',
    title: 'Justificatifs d’achat',
    description: 'Factures fournisseurs, reçus, captures de commande et preuves de provenance.',
    format: 'Import fichier',
    canImport: true,
    canGenerate: false,
    requiredProfiles: ['strict_reseller', 'independent_shop'],
  },
  {
    id: 'sales_register',
    title: 'Registre des ventes',
    description: 'Export chronologique des ventes et encaissements disponibles.',
    format: 'CSV',
    canImport: true,
    canGenerate: true,
    requiredProfiles: ['regular_reseller', 'strict_reseller', 'independent_shop'],
  },
  {
    id: 'purchase_register',
    title: 'Registre des achats',
    description: 'Export annuel des achats avec coût, statut et référence de pièce.',
    format: 'CSV',
    canImport: true,
    canGenerate: true,
    requiredProfiles: ['strict_reseller', 'independent_shop'],
    recommendedProfiles: ['regular_reseller'],
  },
  {
    id: 'invoice_draft',
    title: 'Brouillon de facture',
    description: 'Document préparatoire avec les champs obligatoires à vérifier.',
    format: 'TXT',
    canImport: true,
    canGenerate: true,
    requiresIdentity: true,
    requiredProfiles: ['strict_reseller', 'independent_shop'],
  },
  {
    id: 'urssaf_recap',
    title: 'Récapitulatif déclaration CA',
    description: 'CA encaissé de la période et estimation des cotisations sociales.',
    format: 'CSV',
    canImport: true,
    canGenerate: true,
    requiredProfiles: ['regular_reseller', 'strict_reseller', 'independent_shop'],
  },
  {
    id: 'vat_watch',
    title: 'Suivi franchise TVA',
    description: 'Synthèse des seuils 85 000 € / 93 500 € pour achat-revente.',
    format: 'TXT',
    canImport: false,
    canGenerate: true,
    requiredProfiles: ['strict_reseller', 'independent_shop'],
  },
  {
    id: 'cash_register',
    title: 'Journal de caisse',
    description: 'Brouillon d’export des ventes comptoir et modes de paiement.',
    format: 'CSV',
    canImport: true,
    canGenerate: true,
    requiredProfiles: [],
    recommendedProfiles: ['independent_shop'],
    when: (settings) => settings.hasCashSoftware,
  },
  {
    id: 'accounting_export',
    title: 'Export comptable',
    description: 'Fichier ventes + achats pour traitement comptable.',
    format: 'CSV',
    canImport: false,
    canGenerate: true,
    requiredProfiles: [],
    recommendedProfiles: ['strict_reseller', 'independent_shop'],
  },
]

const loading = ref(true)
const saving = ref(false)
const apiWarning = ref('')
const adminError = ref('')
const feedback = ref('')
const stockItems = ref([])
const archivedDocuments = ref([])
const declarationHistory = ref([])
const fileInput = ref(null)
const pendingImportDocumentId = ref('')
const hasHydrated = ref(false)
const lastServerState = ref({})
let saveTimer = null

const settings = reactive({ ...DEFAULT_ADMIN_ACTION_SETTINGS })

const taskStatus = reactive({})
const router = useRouter()

const currentProfile = computed(() => {
  return PROFILE_DEFINITIONS.find((profile) => profile.key === settings.profileType) || PROFILE_DEFINITIONS[0]
})

const isStarterProfile = computed(() => currentProfile.value.key === 'light_reseller')
const isSimpleResellerProfile = computed(() => currentProfile.value.key === 'regular_reseller')
const showItemProofPanel = computed(() => isStarterProfile.value || isSimpleResellerProfile.value)

const dashboardSubtitle = computed(() => {
  if (isSimpleResellerProfile.value) {
    return 'Déclarer le CA, sortir les registres et garder les preuves item par item.'
  }
  return isStarterProfile.value
    ? 'Le minimum utile pour suivre ventes, preuves et documents.'
    : 'Suivez vos actions, documents et déclarations.'
})

const actionSectionTitle = computed(() => {
  if (isSimpleResellerProfile.value) return 'À faire pour être carré'
  return isStarterProfile.value ? 'Actions à faire' : 'Checklist administrative'
})

const settingsPanelTitle = computed(() => {
  return isStarterProfile.value ? 'Réglages rapides' : 'Informations nécessaires'
})

const documentsSectionTitle = computed(() => {
  if (isSimpleResellerProfile.value) return 'Exports utiles'
  return isStarterProfile.value ? 'Documents utiles' : 'Documents requis et générables'
})

const archiveSectionTitle = computed(() => {
  return isStarterProfile.value ? 'Documents générés' : 'Documents générés ou indexés'
})

const archiveEmptyText = computed(() => {
  return isStarterProfile.value
    ? 'Aucun document généré.'
    : 'Aucun document généré ou importé. Utilisez les actions des cartes documents.'
})

const missingRequiredFields = computed(() => {
  return currentProfile.value.requiredFields.filter((field) => !String(settings[field] ?? '').trim())
})

const inventoryMetrics = computed(() => {
  const now = new Date()
  const currentYearStart = new Date(now.getFullYear(), 0, 1)
  const currentYearEnd = new Date(now.getFullYear(), 11, 31, 23, 59, 59, 999)
  const declaration = activeDeclaration.value
  const declarationStart = parseYmd(declaration.from)
  const declarationEnd = endOfDay(parseYmd(declaration.to))

  const metrics = stockItems.value.reduce(
    (acc, item) => {
      const sold = isVendue(item)
      const saleDate = parseItemDate(item, 'dateVente')
      const purchaseDate = parseItemDate(item, 'dateAchat')
      const revenue = prixResellOf(item)
      const cost = prixRetailOf(item)

      acc.totalItems += 1
      if (!itemHasProof(item)) acc.missingProofs += 1

      if (sold) {
        acc.soldItems += 1
        acc.totalRevenue += revenue
        acc.totalCost += cost
      } else {
        acc.stockValue += cost
      }

      if (sold && saleDate && saleDate >= currentYearStart && saleDate <= currentYearEnd) {
        acc.currentYearRevenue += revenue
        acc.currentYearCost += cost
      }

      if (sold && saleDate && saleDate >= declarationStart && saleDate <= declarationEnd) {
        acc.declarationRevenue += revenue
      }

      if (purchaseDate && purchaseDate >= declarationStart && purchaseDate <= declarationEnd) {
        acc.declarationPurchases += cost
      }

      return acc
    },
    {
      totalItems: 0,
      soldItems: 0,
      totalRevenue: 0,
      totalCost: 0,
      currentYearRevenue: 0,
      currentYearCost: 0,
      declarationRevenue: 0,
      declarationPurchases: 0,
      stockValue: 0,
      missingProofs: 0,
    },
  )

  metrics.currentYearProfit = metrics.currentYearRevenue - metrics.currentYearCost
  return metrics
})

const missingProofItems = computed(() => {
  return stockItems.value.filter((item) => !itemHasProof(item)).slice(0, 5)
})

const starterThresholds = computed(() => {
  return [
    buildMoneyThreshold(
      'revenue',
      'CA',
      inventoryMetrics.value.currentYearRevenue,
      STARTER_REVENUE_THRESHOLD,
    ),
    buildMoneyThreshold(
      'profit',
      'Bénéfice',
      inventoryMetrics.value.currentYearProfit,
      STARTER_PROFIT_THRESHOLD,
    ),
    buildCountThreshold(
      'sales',
      'Ventes',
      inventoryMetrics.value.soldItems,
      STARTER_SALES_THRESHOLD,
    ),
  ]
})

const regularResellerThresholds = computed(() => {
  const revenue = inventoryMetrics.value.currentYearRevenue
  return [
    buildMoneyThreshold('cfe', 'CFE', revenue, CFE_REVENUE_THRESHOLD, {
      reachedLabel: 'À prévoir',
    }),
    buildMoneyThreshold('vat', 'TVA', revenue, VAT_BASE_THRESHOLD_GOODS),
    buildMoneyThreshold('micro', 'Micro', revenue, MICRO_GOODS_THRESHOLD),
  ]
})

const showThresholdPanel = computed(() => isStarterProfile.value || isSimpleResellerProfile.value)

const dashboardThresholds = computed(() => {
  return isSimpleResellerProfile.value ? regularResellerThresholds.value : starterThresholds.value
})

const thresholdSectionTitle = computed(() => {
  return isSimpleResellerProfile.value ? 'Seuils à surveiller' : 'Encore avant déclaration'
})

const thresholdSummary = computed(() => {
  const reached = dashboardThresholds.value.filter((threshold) => threshold.isReached).length
  if (isSimpleResellerProfile.value) {
    return reached ? `${reached} alerte(s)` : 'OK'
  }
  return reached ? `${reached} seuil(s) atteint(s)` : 'Sous les seuils'
})

const activeDeclaration = computed(() => {
  if (settings.declarationFrequency === 'quarterly') {
    return buildQuarterlyDeclarationPeriod(new Date())
  }
  return buildMonthlyDeclarationPeriod(new Date())
})

const declarationModeLabel = computed(() => {
  return settings.declarationFrequency === 'quarterly'
    ? 'Déclaration trimestrielle'
    : 'Déclaration mensuelle'
})

const activeDeclarationStatus = computed(() => {
  return declarationDoneForActivePeriod() ? 'Déclaré' : 'À faire'
})

const visibleTasks = computed(() => {
  return TASK_DEFINITIONS.filter((task) => {
    const matchesProfile = task.profiles.includes(currentProfile.value.key)
    const matchesCondition = typeof task.when === 'function' ? task.when(settings, currentProfile.value) : true
    return matchesProfile && matchesCondition
  }).map((task) => {
    const dueDate = resolveTaskDueDate(task)
    const status = resolveTaskStatus(task, dueDate)
    return {
      ...task,
      dueDate,
      status,
      actionLabel: actionLabelForTask(task),
    }
  })
})

const visibleDocuments = computed(() => {
  return DOCUMENT_DEFINITIONS.filter((documentItem) => {
    const matchesCondition =
      typeof documentItem.when === 'function' ? documentItem.when(settings, currentProfile.value) : true
    return matchesCondition && documentRequirement(documentItem, currentProfile.value.key)
  }).map((documentItem) => {
    const requirement = documentRequirement(documentItem, currentProfile.value.key)
    const hasArchive = hasArchivedDocument(documentItem.id)
    return {
      ...documentItem,
      requirement,
      status: hasArchive ? 'done' : requirement === 'required' ? 'missing' : 'todo',
    }
  })
})

const visibleArchivedDocuments = computed(() => {
  if (!isStarterProfile.value) return archivedDocuments.value
  return archivedDocuments.value.filter((documentItem) => documentItem.documentId !== 'sale_attestation')
})

const missingDocuments = computed(() => {
  return visibleDocuments.value.filter((documentItem) => {
    return documentItem.requirement === 'required' && !hasArchivedDocument(documentItem.id)
  })
})

const tasksToDoCount = computed(() => {
  return visibleTasks.value.filter((task) => task.status !== 'done').length
})

const completedTasksCount = computed(() => {
  return visibleTasks.value.filter((task) => task.status === 'done').length
})

const generatedDocumentsCount = computed(() => {
  return archivedDocuments.value.filter((documentItem) => documentItem.origin === 'generated').length
})

const importedDocumentsCount = computed(() => {
  return archivedDocuments.value.filter((documentItem) => documentItem.origin === 'imported').length
})

const nextDeadline = computed(() => {
  if (currentProfile.value.key === 'light_reseller') {
    return {
      label: '31/12',
      detail: 'Contrôle annuel des relevés',
      date: `${CURRENT_YEAR}-12-31`,
    }
  }

  return {
    label: formatDate(activeDeclaration.value.dueDate),
    detail: activeDeclaration.value.periodLabel,
    date: activeDeclaration.value.dueDate,
  }
})

const globalStatus = computed(() => {
  const hasOverdue = visibleTasks.value.some((task) => task.status === 'overdue')
  if (hasOverdue) {
    return {
      label: 'Urgent',
      detail: 'Au moins une échéance est dépassée.',
      className: 'urgent',
    }
  }

  if (tasksToDoCount.value || missingDocuments.value.length || missingRequiredFields.value.length) {
    return {
      label: 'Attention',
      detail: 'Des actions restent à terminer.',
      className: 'attention',
    }
  }

  return {
    label: 'À jour',
    detail: 'Aucun point bloquant détecté.',
    className: 'ready',
  }
})

const dashboardCards = computed(() => {
  if (isStarterProfile.value) {
    return [
      {
        label: 'CA année',
        value: money(inventoryMetrics.value.currentYearRevenue),
        detail: `${inventoryMetrics.value.soldItems} vente(s)`,
      },
      {
        label: 'Stock',
        value: inventoryMetrics.value.totalItems,
        detail: `${money(inventoryMetrics.value.stockValue)} en stock`,
      },
      {
        label: 'Preuves manquantes',
        value: inventoryMetrics.value.missingProofs,
        detail: 'Factures, reçus, captures',
        className: inventoryMetrics.value.missingProofs ? 'attention' : 'ready',
      },
      {
        label: 'Actions',
        value: tasksToDoCount.value,
        detail: `${completedTasksCount.value} terminée(s)`,
        className: tasksToDoCount.value ? 'attention' : 'ready',
      },
    ]
  }

  if (isSimpleResellerProfile.value) {
    return [
      {
        label: 'CA période',
        value: money(activeDeclaration.value.revenue),
        detail: activeDeclaration.value.periodLabel,
        className: declarationDoneForActivePeriod() ? 'ready' : 'attention',
      },
      {
        label: 'Cotisations',
        value: money(activeDeclaration.value.socialEstimate),
        detail: `${SOCIAL_RATE_GOODS}% vente marchandises`,
      },
      {
        label: 'CA année',
        value: money(inventoryMetrics.value.currentYearRevenue),
        detail: `${inventoryMetrics.value.soldItems} vente(s)`,
      },
      {
        label: 'Preuves manquantes',
        value: inventoryMetrics.value.missingProofs,
        detail: 'À rattacher aux items',
        className: inventoryMetrics.value.missingProofs ? 'attention' : 'ready',
      },
    ]
  }

  return [
    {
      label: 'Statut global',
      value: globalStatus.value.label,
      detail: globalStatus.value.detail,
      className: globalStatus.value.className,
    },
    {
      label: 'Tâches à faire',
      value: tasksToDoCount.value,
      detail: `${completedTasksCount.value} terminée(s)`,
    },
    {
      label: 'Documents manquants',
      value: missingDocuments.value.length,
      detail: `${generatedDocumentsCount.value} généré(s), ${importedDocumentsCount.value} importé(s)`,
    },
    {
      label: 'Prochaine échéance',
      value: nextDeadline.value.label,
      detail: nextDeadline.value.detail,
    },
  ]
})

const vatStatus = computed(() => {
  const revenue = inventoryMetrics.value.currentYearRevenue
  if (settings.vatMode === 'liable') {
    return {
      label: 'TVA collectée',
      detail: 'Exports à vérifier avec votre régime réel ou comptable.',
    }
  }
  if (revenue >= VAT_TOLERANCE_THRESHOLD_GOODS) {
    return {
      label: 'Seuil dépassé',
      detail: 'Le seuil de tolérance achat-revente est atteint ou dépassé.',
    }
  }
  if (revenue >= VAT_BASE_THRESHOLD_GOODS) {
    return {
      label: 'À surveiller',
      detail: 'Le seuil de base est atteint, vérifiez la sortie de franchise.',
    }
  }
  return {
    label: 'Franchise suivie',
    detail: `${money(revenue)} / ${money(VAT_BASE_THRESHOLD_GOODS)} cette année.`,
  }
})

watch(
  [settings, taskStatus, archivedDocuments, declarationHistory],
  () => {
    if (!hasHydrated.value) return
    scheduleSave()
  },
  { deep: true },
)

onMounted(async () => {
  loading.value = true
  const [stateResult, stockResult] = await Promise.allSettled([loadAdminState(), loadStockItems()])

  if (stateResult.status === 'rejected') {
    adminError.value = 'État administratif indisponible : sauvegarde locale utilisée.'
    loadLocalState()
  }

  if (stockResult.status === 'rejected') {
    apiWarning.value = 'Inventaire indisponible : les montants seront mis à jour au prochain chargement.'
  }

  hasHydrated.value = true
  loading.value = false
})

function handleTaskAction(task) {
  if (task.actionType === 'generate' && task.documentId) {
    generateDocumentById(task.documentId)
    markTaskDone(task.id)
    return
  }
  if (task.actionType === 'import' && task.documentId) {
    requestImport(task.documentId)
    return
  }
  if (task.actionType === 'declaration') {
    prepareDeclaration()
    markTaskDone(task.id)
    return
  }
  if (task.actionType === 'inventory') {
    openFirstMissingProofItem()
    return
  }
  if (task.actionType === 'external' && task.url) {
    window.open(task.url, '_blank', 'noopener,noreferrer')
    return
  }
  if (task.actionType === 'settings') {
    feedback.value = 'Complétez les champs requis dans le panneau de saisie.'
    return
  }
  markTaskDone(task.id)
}

function markTaskDone(taskId) {
  taskStatus[taskId] = 'done'
}

function openFirstMissingProofItem() {
  const [firstItem] = missingProofItems.value
  if (!firstItem) {
    feedback.value = 'Tous les items ont une pièce jointe.'
    markTaskDone('keep_purchase_proofs')
    return
  }
  router.push(inventoryItemRoute(firstItem)).catch(() => {})
}

function inventoryItemRoute(item) {
  const id = itemId(item)
  return id
    ? { name: 'gestion', query: { openItemId: String(id), source: 'admin-proofs' } }
    : { name: 'gestion' }
}

function requestImport(documentId) {
  pendingImportDocumentId.value = documentId
  fileInput.value?.click()
}

function handleFileImport(event) {
  const [file] = event.target.files || []
  const documentId = pendingImportDocumentId.value
  const definition = documentDefinitionById(documentId)
  if (!file || !definition) return

  archiveDocument({
    documentId,
    title: definition.title,
    origin: 'imported',
    status: 'done',
    filename: file.name,
    mime: file.type || 'application/octet-stream',
    fileSize: file.size,
    content: '',
    verification: 'Original à conserver dans votre espace documentaire.',
  })

  pendingImportDocumentId.value = ''
  event.target.value = ''
  feedback.value = 'Document importé.'
}

function generateDocumentById(documentId) {
  const definition = documentDefinitionById(documentId)
  if (!definition) return
  generateDocument({
    ...definition,
    requirement: documentRequirement(definition, currentProfile.value.key) || 'recommended',
  })
}

function generateDocument(documentItem) {
  if (documentItem.requiresIdentity && missingRequiredFields.value.length > 0) {
    feedback.value = 'Complétez les informations nécessaires avant génération.'
    return
  }

  const generated = buildGeneratedDocument(documentItem)
  archiveDocument(generated)
  downloadContent(generated.content, generated.filename, generated.mime)
  feedback.value = 'Document généré.'
}

function prepareDeclaration() {
  const definition = documentDefinitionById('urssaf_recap')
  const generated = buildGeneratedDocument(definition)
  archiveDocument(generated)
  downloadContent(generated.content, generated.filename, generated.mime)
  upsertDeclarationHistory('prepared')
  feedback.value = 'Récapitulatif de déclaration généré.'
}

function markDeclarationDone() {
  upsertDeclarationHistory('declared')
  taskStatus.prepare_ca_declaration = 'done'
  feedback.value = 'Déclaration marquée comme traitée pour la période.'
}

function upsertDeclarationHistory(status) {
  const declaration = activeDeclaration.value
  const existingIndex = declarationHistory.value.findIndex((entry) => entry.periodKey === declaration.periodKey)
  const entry = {
    id: `${declaration.periodKey}-${Date.now()}`,
    periodKey: declaration.periodKey,
    periodLabel: declaration.periodLabel,
    revenue: declaration.revenue,
    status,
    createdAt: new Date().toISOString(),
  }

  if (existingIndex >= 0) {
    declarationHistory.value.splice(existingIndex, 1, entry)
  } else {
    declarationHistory.value.unshift(entry)
  }
}

function archiveDocument(documentItem) {
  archivedDocuments.value.unshift({
    id: `${documentItem.documentId || documentItem.id}-${Date.now()}`,
    createdAt: new Date().toISOString(),
    ...documentItem,
  })
}

function downloadArchiveDocument(documentItem) {
  if (!documentItem.content) {
    feedback.value = 'Ce fichier importé est indexé, mais son contenu original reste à conserver séparément.'
    return
  }
  downloadContent(documentItem.content, documentItem.filename, documentItem.mime)
}

function buildGeneratedDocument(documentItem) {
  const builders = {
    sales_register: buildSalesRegister,
    purchase_register: buildPurchaseRegister,
    invoice_draft: buildInvoiceDraft,
    urssaf_recap: buildUrssafRecap,
    vat_watch: buildVatWatch,
    cash_register: buildCashRegister,
    accounting_export: buildAccountingExport,
  }
  const builder = builders[documentItem.id] || buildGenericDocument
  const payload = builder()

  return {
    documentId: documentItem.id,
    title: documentItem.title,
    origin: 'generated',
    status: 'draft',
    filename: payload.filename,
    mime: payload.mime,
    content: payload.content,
    verification: payload.verification || 'À relire et valider selon votre situation exacte.',
  }
}

function buildSalesRegister() {
  const rows = [['date', 'type', 'designation', 'montant_ttc', 'cout', 'profit', 'piece']]
  stockItems.value
    .filter((item) => isVendue(item))
    .forEach((item) => {
      const revenue = prixResellOf(item)
      const cost = prixRetailOf(item)
      rows.push([
        toYmd(parseItemDate(item, 'dateVente')),
        'vente',
        itemLabel(item),
        revenue,
        cost,
        revenue - cost,
        itemHasProof(item) ? 'ok' : 'a_completer',
      ])
    })

  return csvPayload(`registre-ventes-${CURRENT_YEAR}.csv`, rows)
}

function buildPurchaseRegister() {
  const rows = [['date', 'designation', 'cout_ttc', 'statut', 'piece']]
  stockItems.value.forEach((item) => {
    rows.push([
      toYmd(parseItemDate(item, 'dateAchat')),
      itemLabel(item),
      prixRetailOf(item),
      isVendue(item) ? 'vendu' : 'stock',
      itemHasProof(item) ? 'ok' : 'a_completer',
    ])
  })

  return csvPayload(`registre-achats-${CURRENT_YEAR}.csv`, rows)
}

function buildInvoiceDraft() {
  return {
    filename: `brouillon-facture-${todayYmd()}.txt`,
    mime: 'text/plain;charset=utf-8',
    content: [
      'BROUILLON DE FACTURE - À VÉRIFIER',
      '',
      `Vendeur : ${settings.legalName || settings.tradeName || 'à compléter'}`,
      `SIREN : ${settings.siren || 'à compléter'}`,
      `Adresse : ${settings.address || 'à compléter'}`,
      `Email : ${settings.email || 'à compléter'}`,
      '',
      'Client : à compléter',
      'Adresse client : à compléter',
      'Date de vente : à compléter',
      'Numéro de facture : à compléter',
      '',
      'Lignes : quantité, désignation précise, prix unitaire HT/TTC, TVA ou exonération.',
      settings.vatMode === 'franchise'
        ? 'Mention à vérifier : TVA non applicable, art. 293 B du CGI.'
        : 'TVA : taux et montants à vérifier.',
      '',
      'Ce brouillon prépare les informations, il ne remplace pas une validation comptable.',
    ].join('\n'),
  }
}

function buildUrssafRecap() {
  const declaration = activeDeclaration.value
  const rows = [
    ['periode', declaration.periodLabel],
    ['date_debut', declaration.from],
    ['date_fin', declaration.to],
    ['echeance_estimee', declaration.dueDate],
    ['ca_encaisse', declaration.revenue],
    ['taux_cotisations_vente_marchandises', `${SOCIAL_RATE_GOODS}%`],
    ['cotisations_estimees', declaration.socialEstimate],
    ['verification', 'Saisir uniquement sur le portail officiel apres validation'],
  ]

  return csvPayload(`recap-declaration-ca-${declaration.periodKey}.csv`, rows)
}

function buildVatWatch() {
  const revenue = inventoryMetrics.value.currentYearRevenue
  return {
    filename: `suivi-tva-${CURRENT_YEAR}.txt`,
    mime: 'text/plain;charset=utf-8',
    content: [
      'SUIVI FRANCHISE EN BASE TVA - BROUILLON',
      '',
      `CA achat-revente estimé ${CURRENT_YEAR} : ${money(revenue)}`,
      `Seuil de base suivi : ${money(VAT_BASE_THRESHOLD_GOODS)}`,
      `Seuil de tolérance suivi : ${money(VAT_TOLERANCE_THRESHOLD_GOODS)}`,
      `Statut : ${vatStatus.value.label}`,
      '',
      vatStatus.value.detail,
      '',
      'À vérifier selon votre régime, la composition de votre activité et les sources fiscales à jour.',
    ].join('\n'),
  }
}

function buildCashRegister() {
  const rows = [['date', 'designation', 'montant_ttc', 'mode_reglement', 'piece']]
  stockItems.value
    .filter((item) => isVendue(item))
    .forEach((item) => {
      rows.push([
        toYmd(parseItemDate(item, 'dateVente')),
        itemLabel(item),
        prixResellOf(item),
        'a_preciser',
        itemHasProof(item) ? 'ok' : 'a_completer',
      ])
    })

  return csvPayload(`journal-caisse-${CURRENT_YEAR}.csv`, rows)
}

function buildAccountingExport() {
  const rows = [['date', 'journal', 'libelle', 'debit', 'credit', 'piece']]
  stockItems.value.forEach((item) => {
    rows.push([
      toYmd(parseItemDate(item, 'dateAchat')),
      'achats',
      itemLabel(item),
      prixRetailOf(item),
      '',
      itemHasProof(item) ? 'ok' : 'a_completer',
    ])
    if (isVendue(item)) {
      rows.push([
        toYmd(parseItemDate(item, 'dateVente')),
        'ventes',
        itemLabel(item),
        '',
        prixResellOf(item),
        itemHasProof(item) ? 'ok' : 'a_completer',
      ])
    }
  })

  return csvPayload(`export-comptable-${todayYmd()}.csv`, rows)
}

function buildGenericDocument() {
  return {
    filename: `document-administratif-${todayYmd()}.txt`,
    mime: 'text/plain;charset=utf-8',
    content: 'Document administratif à compléter et vérifier selon votre situation exacte.',
  }
}

function csvPayload(filename, rows) {
  return {
    filename,
    mime: 'text/csv;charset=utf-8',
    content: `\uFEFF${rows.map((row) => row.map(csvCell).join(';')).join('\n')}`,
  }
}

async function loadAdminState() {
  try {
    const serverState = await AdminService.state()
    lastServerState.value = serverState && typeof serverState === 'object' ? serverState : {}
    hydrateState(lastServerState.value.adminActionPage || lastServerState.value)
  } catch (error) {
    apiWarning.value = 'Backend admin indisponible : sauvegarde locale active.'
    loadLocalState()
  }
}

async function loadStockItems() {
  const response = await SnkVenteServices.getSnkVente({ limit: 1000 })
  stockItems.value = normalizeList(response.data)
}

function loadLocalState() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (!raw) return
    hydrateState(JSON.parse(raw))
  } catch {
    adminError.value = 'Impossible de relire la sauvegarde locale administrative.'
  }
}

function hydrateState(payload = {}) {
  const source = payload.adminActionPage || payload
  const sourceSettings = source.settings || {}

  Object.keys(settings).forEach((key) => {
    if (Object.prototype.hasOwnProperty.call(sourceSettings, key)) {
      settings[key] = sourceSettings[key]
    }
  })
  settings.profileType = normalizeAdminActionProfileKey(
    source.selectedProfile || sourceSettings.profileType,
  )

  replaceReactiveMap(taskStatus, source.taskStatus)
  archivedDocuments.value = normalizeDocuments(source.documents || source.archivedDocuments)
  declarationHistory.value = Array.isArray(source.declarations || source.declarationHistory)
    ? [...(source.declarations || source.declarationHistory)]
    : []
}

function normalizeDocuments(documents) {
  if (!Array.isArray(documents)) return []
  return documents.map((documentItem, index) => ({
    id: documentItem.id || documentItem.backendId || `${documentItem.documentId || 'doc'}-${index}`,
    documentId: documentItem.documentId || documentItem.type || documentItem.documentType || 'document',
    title: documentItem.title || documentItem.label || documentItem.number || 'Document administratif',
    origin: documentItem.origin || 'imported',
    status: documentItem.status || 'done',
    filename: documentItem.filename || documentItem.number || 'document',
    mime: documentItem.mime || 'text/plain;charset=utf-8',
    content: documentItem.content || '',
    createdAt: documentItem.createdAt || new Date().toISOString(),
    verification: documentItem.verification || '',
  }))
}

function replaceReactiveMap(target, source) {
  Object.keys(target).forEach((key) => {
    delete target[key]
  })
  if (!source || typeof source !== 'object' || Array.isArray(source)) return
  Object.entries(source).forEach(([key, value]) => {
    target[key] = value
  })
}

function buildStatePayload() {
  return {
    ...lastServerState.value,
    adminActionPage: {
      version: 1,
      selectedProfile: settings.profileType,
      settings: { ...settings },
      taskStatus: { ...taskStatus },
      documents: archivedDocuments.value,
      declarations: declarationHistory.value,
      updatedAt: new Date().toISOString(),
    },
    settings: { ...settings, profileType: settings.profileType },
    documents: archivedDocuments.value,
  }
}

function scheduleSave() {
  clearTimeout(saveTimer)
  saveTimer = setTimeout(() => {
    saveNow({ silent: true })
  }, 700)
}

async function saveNow(options = {}) {
  const payload = buildStatePayload()
  localStorage.setItem(STORAGE_KEY, JSON.stringify(payload.adminActionPage))

  try {
    saving.value = true
    await AdminService.saveState(payload)
    lastServerState.value = payload
    if (!options.silent) feedback.value = 'État administratif sauvegardé.'
  } catch {
    apiWarning.value = 'État sauvegardé localement. Backend admin indisponible.'
    if (!options.silent) feedback.value = 'Sauvegarde locale effectuée.'
  } finally {
    saving.value = false
  }
}

function resolveTaskDueDate(task) {
  if (task.dueType === 'declaration') return activeDeclaration.value.dueDate
  if (task.dueType === 'yearEnd') return `${CURRENT_YEAR}-12-31`
  return ''
}

function resolveTaskStatus(task, dueDate) {
  if (taskStatus[task.id] === 'done') return 'done'
  if (task.id === 'complete_identity') return missingRequiredFields.value.length ? 'todo' : 'done'
  if (task.documentId && hasArchivedDocument(task.documentId)) return 'done'
  if (task.id === 'keep_purchase_proofs' && inventoryMetrics.value.totalItems > 0) {
    return inventoryMetrics.value.missingProofs ? 'todo' : 'done'
  }
  if (task.id === 'prepare_ca_declaration' && declarationDoneForActivePeriod()) return 'done'
  if (dueDate && parseYmd(dueDate) < startOfDay(new Date())) return 'overdue'
  return task.obligation === 'required' ? 'todo' : 'attention'
}

function declarationDoneForActivePeriod() {
  return declarationHistory.value.some((entry) => {
    return entry.periodKey === activeDeclaration.value.periodKey && entry.status === 'declared'
  })
}

function actionLabelForTask(task) {
  const labels = {
    generate: 'Générer',
    import: 'Importer',
    declaration: 'Préparer',
    inventory: 'Ouvrir items',
    external: 'Ouvrir',
    settings: 'Remplir',
    mark: 'Marquer comme fait',
  }
  return labels[task.actionType] || 'Traiter'
}

function taskActionIcon(task) {
  const icons = {
    generate: Download,
    import: Upload,
    declaration: FileText,
    inventory: ListChecks,
    external: ExternalLink,
    settings: ListChecks,
    mark: CheckCircle2,
  }
  return icons[task.actionType] || CheckCircle2
}

function documentRequirement(documentItem, profileKey) {
  if (documentItem.requiredProfiles?.includes(profileKey)) return 'required'
  if (documentItem.recommendedProfiles?.includes(profileKey)) return 'recommended'
  return ''
}

function documentDefinitionById(documentId) {
  return DOCUMENT_DEFINITIONS.find((documentItem) => documentItem.id === documentId)
}

function buildMoneyThreshold(key, label, value, limit, options = {}) {
  const current = Math.max(0, toNumber(value, 0))
  const threshold = Math.max(1, toNumber(limit, 1))
  const remaining = Math.max(0, threshold - current)
  const reachedLabel = options.reachedLabel || 'Seuil atteint'
  return {
    key,
    label,
    isReached: remaining <= 0,
    progress: thresholdProgress(current, threshold),
    remainingLabel: remaining <= 0 ? reachedLabel : `${money(remaining)} restants`,
    currentLabel: money(current),
    limitLabel: money(threshold),
  }
}

function buildCountThreshold(key, label, value, limit) {
  const current = Math.max(0, Math.round(toNumber(value, 0)))
  const threshold = Math.max(1, Math.round(toNumber(limit, 1)))
  const remaining = Math.max(0, threshold - current)
  return {
    key,
    label,
    isReached: remaining <= 0,
    progress: thresholdProgress(current, threshold),
    remainingLabel: remaining <= 0 ? 'Seuil atteint' : `${remaining} restantes`,
    currentLabel: `${current}`,
    limitLabel: `${threshold}`,
  }
}

function thresholdProgress(value, limit) {
  return Math.min(100, Math.round((toNumber(value, 0) / Math.max(1, toNumber(limit, 1))) * 100))
}

function hasArchivedDocument(documentId) {
  return archivedDocuments.value.some((documentItem) => documentItem.documentId === documentId)
}

function taskStatusLabel(status) {
  const labels = {
    done: 'Fait',
    todo: 'À faire',
    attention: 'À vérifier',
    overdue: 'En retard',
  }
  return labels[status] || 'À faire'
}

function documentStatusLabel(documentItem) {
  if (documentItem.status === 'done') return 'Archivé'
  if (documentItem.status === 'missing') return 'Manquant'
  return 'À préparer'
}

function archiveStatusLabel(status) {
  if (status === 'draft') return 'Brouillon'
  if (status === 'done') return 'Indexé'
  return 'À vérifier'
}

function obligationLabel(obligation) {
  return obligation === 'required' ? 'Obligatoire' : 'Recommandé'
}

function originLabel(origin) {
  return origin === 'generated' ? 'Généré' : 'Importé'
}

function money(value) {
  return formatEUR(toNumber(value, 0))
}

function formatDate(value) {
  return formatDateFR(value)
}

function normalizeList(payload) {
  if (Array.isArray(payload)) return payload
  if (!payload || typeof payload !== 'object') return []
  for (const key of ['items', 'data', 'content', 'results']) {
    if (Array.isArray(payload[key])) return payload[key]
  }
  return []
}

function buildMonthlyDeclarationPeriod(date) {
  const previousMonth = addMonths(startOfMonth(date), -1)
  const from = startOfMonth(previousMonth)
  const to = endOfMonth(previousMonth)
  const due = endOfMonth(date)
  const periodKey = `${from.getFullYear()}-${String(from.getMonth() + 1).padStart(2, '0')}`
  const revenue = declarationRevenueForRange(from, to)

  return {
    periodKey,
    periodLabel: `${monthName(from)} ${from.getFullYear()}`,
    from: toYmd(from),
    to: toYmd(to),
    dueDate: toYmd(due),
    revenue,
    socialEstimate: revenue * (SOCIAL_RATE_GOODS / 100),
    detail: 'À déposer avant la fin du mois suivant la période.',
  }
}

function buildQuarterlyDeclarationPeriod(date) {
  const quarter = Math.floor(date.getMonth() / 3)
  const from = new Date(date.getFullYear(), quarter * 3, 1)
  const to = endOfMonth(new Date(date.getFullYear(), quarter * 3 + 2, 1))
  const due = endOfMonth(new Date(date.getFullYear(), quarter * 3 + 3, 1))
  const periodKey = `${date.getFullYear()}-Q${quarter + 1}`
  const revenue = declarationRevenueForRange(from, to)

  return {
    periodKey,
    periodLabel: `T${quarter + 1} ${date.getFullYear()}`,
    from: toYmd(from),
    to: toYmd(to),
    dueDate: toYmd(due),
    revenue,
    socialEstimate: revenue * (SOCIAL_RATE_GOODS / 100),
    detail: 'Échéance estimée à la fin du mois suivant le trimestre.',
  }
}

function declarationRevenueForRange(from, to) {
  const rangeEnd = endOfDay(to)
  return stockItems.value.reduce((total, item) => {
    if (!isVendue(item)) return total
    const saleDate = parseItemDate(item, 'dateVente')
    if (!saleDate || saleDate < from || saleDate > rangeEnd) return total
    return total + prixResellOf(item)
  }, 0)
}

function itemId(item) {
  return getField(item, 'id') || getField(item, 'itemId') || getField(item, 'snkVenteId')
}

function itemLabel(item) {
  return (
    getField(item, 'nomItem') ||
    getField(item, 'nom') ||
    getField(item, 'name') ||
    getField(item, 'reference') ||
    'Item sans libellé'
  )
}

function itemHasProof(item) {
  if (Array.isArray(item?.attachments) && item.attachments.length > 0) return true
  const metadata = normalizeMetadata(item?.metadata)
  return ['proof', 'proofUrl', 'factureUrl', 'invoiceUrl', 'receiptUrl', 'attachmentUrl', 'hasAttachment'].some(
    (key) => {
      const value = metadata[key]
      if (typeof value === 'boolean') return value
      return value != null && String(value).trim() !== ''
    },
  )
}

function normalizeMetadata(metadata) {
  if (!metadata) return {}
  if (typeof metadata === 'object') return metadata
  try {
    return JSON.parse(metadata)
  } catch {
    return {}
  }
}

function parseItemDate(item, key) {
  return parseDate(getField(item, key))
}

function parseDate(value) {
  if (!value) return null
  const date = value instanceof Date ? value : new Date(value)
  return Number.isNaN(date.getTime()) ? null : date
}

function parseYmd(value) {
  if (!value) return startOfDay(new Date())
  const [year, month, day] = String(value).slice(0, 10).split('-').map(Number)
  return new Date(year, month - 1, day)
}

function toYmd(value) {
  const date = value instanceof Date ? value : parseDate(value)
  if (!date) return ''
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function todayYmd() {
  return toYmd(new Date())
}

function startOfDay(date) {
  return new Date(date.getFullYear(), date.getMonth(), date.getDate())
}

function endOfDay(date) {
  return new Date(date.getFullYear(), date.getMonth(), date.getDate(), 23, 59, 59, 999)
}

function startOfMonth(date) {
  return new Date(date.getFullYear(), date.getMonth(), 1)
}

function endOfMonth(date) {
  return new Date(date.getFullYear(), date.getMonth() + 1, 0)
}

function addMonths(date, count) {
  return new Date(date.getFullYear(), date.getMonth() + count, 1)
}

function monthName(date) {
  return date.toLocaleDateString('fr-FR', { month: 'long' })
}

function csvCell(value) {
  return `"${String(value ?? '').replaceAll('"', '""')}"`
}

function downloadContent(content, filename, mime) {
  const blob = new Blob([content], { type: mime || 'text/plain;charset=utf-8' })
  const url = window.URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  document.body.appendChild(link)
  link.click()
  link.remove()
  window.URL.revokeObjectURL(url)
}
</script>

<style scoped>
.admin-page {
  min-height: calc(100dvh - 7rem);
  margin-inline: calc(clamp(16px, 2.2vw, 32px) * -1);
  margin-top: clamp(0.75rem, 1vw, 1.25rem);
  margin-bottom: 4rem;
  background:
    linear-gradient(135deg, rgba(14, 165, 233, 0.06), transparent 34%),
    linear-gradient(180deg, rgba(248, 250, 252, 0.98), rgba(241, 245, 249, 0.74)),
    #f8fafc;
  color: #172033;
}

.admin-page.is-embedded {
  min-height: auto;
  margin: 0;
  background: transparent;
}

.admin-workspace {
  width: min(100%, 1540px);
  display: grid;
  gap: 1rem;
  margin-inline: auto;
  padding: clamp(1rem, 2vw, 1.6rem) clamp(16px, 2.2vw, 32px) clamp(5rem, 9vw, 7rem);
}

.admin-page.is-embedded .admin-workspace {
  width: 100%;
  padding: 0;
}

.admin-header,
.section-block,
.summary-tile {
  border: 1px solid rgba(148, 163, 184, 0.24);
  border-radius: 8px;
  background:
    linear-gradient(135deg, rgba(14, 165, 233, 0.06), transparent 42%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.97), rgba(248, 250, 252, 0.94));
  box-shadow: 0 24px 60px rgba(15, 23, 42, 0.07), 0 10px 32px rgba(14, 165, 233, 0.06);
}

.admin-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
  padding: clamp(1rem, 2vw, 1.5rem);
}

.admin-eyebrow {
  margin: 0 0 0.35rem;
  color: #0369a1;
  font-size: 0.72rem;
  font-weight: 900;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

h1,
h2,
h3,
p {
  margin: 0;
}

h1 {
  color: #111827;
  font-size: clamp(2rem, 3.5vw, 3.35rem);
  line-height: 1;
  letter-spacing: 0;
}

h2 {
  color: #172033;
  font-size: clamp(1.15rem, 1.5vw, 1.45rem);
  letter-spacing: 0;
}

h3 {
  color: #172033;
  font-size: 1rem;
  letter-spacing: 0;
}

.admin-subtitle {
  margin-top: 0.55rem;
  color: #64748b;
  font-weight: 750;
}

.header-tools,
.section-heading,
.task-title-row,
.document-card-head,
.document-actions,
.declaration-actions,
.history-heading,
.notice-row,
.success-row {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.header-tools {
  flex-wrap: wrap;
  justify-content: flex-end;
}

.legal-note,
.section-hint {
  display: inline-flex;
  align-items: center;
  min-height: 30px;
  border-radius: 999px;
  background: #ecfeff;
  color: #0f766e;
  padding: 0 0.8rem;
  font-size: 0.78rem;
  font-weight: 850;
}

.section-block {
  display: grid;
  gap: 1rem;
  padding: 1rem;
}

.section-heading {
  justify-content: space-between;
}

.section-heading.compact {
  align-items: flex-start;
}

.profile-context {
  border-color: rgba(139, 92, 246, 0.18);
}

.profile-context-description {
  max-width: 820px;
  color: #64748b;
  line-height: 1.5;
  font-weight: 720;
}

.profile-edit-link {
  text-decoration: none;
}

.profile-trait-row {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.profile-trait-pill {
  display: inline-flex;
  align-items: center;
  min-height: 30px;
  border-radius: 999px;
  background: #f1f5f9;
  color: #475569;
  padding: 0 0.75rem;
  font-size: 0.78rem;
  font-weight: 900;
}

.summary-grid,
.document-grid,
.declaration-layout,
.metrics-strip {
  display: grid;
  gap: 0.85rem;
}

.task-card,
.document-card,
.declaration-card,
.switch-row,
.history-row,
.empty-state {
  border: 1px solid #dbe4f2;
  border-radius: 8px;
  background: #ffffff;
}

.document-icon,
.empty-icon {
  width: 22px;
  height: 22px;
  color: #0e7490;
}

.task-card p,
.document-card p,
.declaration-card p,
.empty-state span,
.switch-row small,
.summary-tile small,
.archive-row small {
  color: #64748b;
  line-height: 1.45;
}

.level-pill,
.status-badge,
.task-meta span {
  display: inline-flex;
  align-items: center;
  min-height: 26px;
  border-radius: 999px;
  padding: 0 0.65rem;
  font-size: 0.74rem;
  font-weight: 900;
  white-space: nowrap;
}

.level-pill.low,
.status-badge.done,
.summary-tile.ready {
  color: #047857;
  background: #dff7ea;
}

.level-pill.medium,
.status-badge.attention,
.summary-tile.attention {
  color: #92400e;
  background: #fff3d6;
}

.level-pill.high,
.level-pill.shop,
.status-badge.overdue,
.summary-tile.urgent {
  color: #be123c;
  background: #ffe4e6;
}

.status-badge.todo,
.status-badge.missing {
  color: #31539c;
  background: #e0ecff;
}

.task-meta span.required {
  color: #0369a1;
  background: #e0f2fe;
}

.task-meta span.recommended {
  color: #0f766e;
  background: #ccfbf1;
}

.summary-grid {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.summary-tile {
  display: grid;
  gap: 0.3rem;
  padding: 1rem;
}

.summary-tile span {
  color: #64748b;
  font-size: 0.8rem;
  font-weight: 850;
}

.summary-tile strong {
  color: #111827;
  font-size: clamp(1.35rem, 2.1vw, 1.9rem);
  line-height: 1.1;
}

.admin-main-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) minmax(340px, 0.75fr);
  gap: 1rem;
  align-items: start;
}

.task-stack {
  display: grid;
  gap: 0.75rem;
}

.task-card {
  display: grid;
  grid-template-columns: 12px minmax(0, 1fr) auto;
  gap: 0.85rem;
  align-items: center;
  padding: 0.9rem;
}

.task-status-dot {
  width: 10px;
  height: 100%;
  min-height: 58px;
  border-radius: 999px;
  background: #dbeafe;
}

.task-status-dot.done {
  background: #34d399;
}

.task-status-dot.attention {
  background: #fbbf24;
}

.task-status-dot.overdue {
  background: #fb7185;
}

.task-content {
  min-width: 0;
}

.task-title-row {
  justify-content: space-between;
  align-items: flex-start;
}

.task-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 0.4rem;
  margin-top: 0.6rem;
}

.task-meta span {
  color: #475569;
  background: #f1f5f9;
}

.task-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 0.45rem;
}

.icon-button,
.icon-only {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.45rem;
  border: 0;
  border-radius: 8px;
  font: inherit;
  font-size: 0.84rem;
  font-weight: 900;
  cursor: pointer;
  transition:
    transform 0.16s ease,
    box-shadow 0.16s ease,
    background-color 0.16s ease;
}

.icon-button {
  min-height: 38px;
  padding: 0 0.8rem;
}

.icon-button.primary {
  color: #ffffff;
  background: #0f766e;
}

.icon-button.selected {
  color: #047857;
  background: #dff7ea;
}

.icon-button.soft {
  color: #0f766e;
  background: #ccfbf1;
}

.icon-button:disabled {
  cursor: not-allowed;
  opacity: 0.55;
}

.icon-button:not(:disabled):hover,
.icon-only:hover {
  transform: translateY(-1px);
  box-shadow: 0 10px 20px rgba(38, 52, 84, 0.12);
}

.icon-only {
  width: 38px;
  height: 38px;
  color: #0f766e;
  background: #ecfdf5;
}

.button-icon,
.notice-icon {
  width: 16px;
  height: 16px;
  flex: 0 0 auto;
}

.form-panel {
  position: sticky;
  top: 1rem;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.75rem;
}

.form-grid label,
.switch-row {
  display: grid;
  gap: 0.38rem;
  color: #334155;
  font-size: 0.83rem;
  font-weight: 900;
}

.form-grid label.wide {
  grid-column: 1 / -1;
}

input,
select {
  width: 100%;
  min-height: 40px;
  box-sizing: border-box;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  background: #ffffff;
  color: #111827;
  font: inherit;
  padding: 0 0.75rem;
  outline: none;
}

input:focus,
select:focus {
  border-color: #0f766e;
  box-shadow: 0 0 0 3px rgba(15, 118, 110, 0.14);
}

.switch-stack {
  display: grid;
  gap: 0.6rem;
}

.switch-row {
  grid-template-columns: auto minmax(0, 1fr);
  align-items: flex-start;
  padding: 0.75rem;
}

.switch-row input {
  width: auto;
  min-height: auto;
  margin-top: 0.2rem;
}

.switch-row strong,
.switch-row small {
  display: block;
}

.metrics-strip {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.metrics-strip article {
  border-radius: 8px;
  background: #f8fafc;
  padding: 0.8rem;
}

.metrics-strip span,
.metrics-strip small {
  color: #64748b;
  font-size: 0.78rem;
  font-weight: 850;
}

.metrics-strip strong {
  display: block;
  margin-top: 0.25rem;
  color: #111827;
  font-size: 1.25rem;
}

.threshold-panel {
  gap: 0.85rem;
}

.threshold-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.85rem;
}

.threshold-card {
  display: grid;
  gap: 0.65rem;
  border: 1px solid #dbe4f2;
  border-radius: 8px;
  background: #ffffff;
  padding: 0.9rem;
}

.threshold-card.is-reached {
  border-color: #fecdd3;
  background: #fff1f2;
}

.threshold-card-head {
  display: flex;
  justify-content: space-between;
  gap: 0.8rem;
  align-items: flex-start;
}

.threshold-card-head span,
.threshold-card small {
  color: #64748b;
  font-size: 0.78rem;
  font-weight: 850;
}

.threshold-card-head strong {
  color: #111827;
  font-size: 1rem;
  text-align: right;
}

.threshold-progress {
  height: 9px;
  overflow: hidden;
  border-radius: 999px;
  background: #e2e8f0;
}

.threshold-progress span {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: #0f766e;
}

.threshold-card.is-reached .threshold-progress span {
  background: #e11d48;
}

.proof-panel {
  display: grid;
  gap: 0.65rem;
  border-top: 1px solid #e2e8f0;
  padding-top: 0.85rem;
}

.proof-panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
}

.proof-item-list {
  display: grid;
  gap: 0.5rem;
}

.proof-item-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto auto;
  gap: 0.7rem;
  align-items: center;
  border: 1px solid #dbe4f2;
  border-radius: 8px;
  background: #ffffff;
  color: inherit;
  padding: 0.65rem 0.75rem;
  text-decoration: none;
}

.proof-item-row span {
  overflow: hidden;
  color: #172033;
  font-weight: 900;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.proof-item-row small {
  color: #64748b;
  font-weight: 850;
}

.proof-item-row strong {
  color: #0f766e;
  font-size: 0.78rem;
  font-weight: 950;
}

.document-grid {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.document-card {
  display: grid;
  gap: 0.65rem;
  padding: 0.9rem;
}

.document-card.missing {
  border-color: #bfdbfe;
  background: #f8fbff;
}

.document-card.done {
  border-color: #bbf7d0;
  background: #f7fdf9;
}

.document-card-head {
  justify-content: space-between;
}

.document-actions {
  flex-wrap: wrap;
}

.missing-note {
  color: #92400e;
  font-weight: 800;
}

.declaration-layout {
  grid-template-columns: minmax(0, 1.2fr) repeat(2, minmax(0, 0.8fr));
}

.declaration-card {
  display: grid;
  gap: 0.55rem;
  padding: 1rem;
}

.declaration-card > span,
.history-heading span {
  color: #64748b;
  font-size: 0.78rem;
  font-weight: 900;
}

.declaration-card strong {
  color: #111827;
  font-size: 2rem;
  line-height: 1;
}

.primary-declaration {
  background: #ecfdf5;
  border-color: #a7f3d0;
}

.history-list {
  display: grid;
  gap: 0.55rem;
}

.history-heading {
  justify-content: space-between;
}

.history-row {
  display: grid;
  grid-template-columns: 1fr auto auto;
  gap: 0.75rem;
  align-items: center;
  padding: 0.7rem 0.85rem;
}

.archive-table {
  overflow-x: auto;
  border: 1px solid #dbe4f2;
  border-radius: 8px;
}

.archive-head,
.archive-row {
  display: grid;
  grid-template-columns: minmax(240px, 1.4fr) 0.7fr 0.75fr 0.7fr 52px;
  gap: 0.85rem;
  align-items: center;
  min-width: 780px;
  padding: 0.75rem 0.9rem;
}

.archive-head {
  color: #64748b;
  background: #f1f5f9;
  font-size: 0.76rem;
  font-weight: 900;
  text-transform: uppercase;
}

.archive-row {
  border-top: 1px solid #e2e8f0;
  background: #ffffff;
}

.archive-row strong,
.archive-row small {
  display: block;
}

.empty-state {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.9rem;
  color: #64748b;
  font-weight: 750;
}

.action-empty {
  justify-content: flex-start;
}

.compact-empty {
  justify-content: center;
}

.notice-row,
.success-row {
  border-radius: 8px;
  padding: 0.8rem 1rem;
  font-weight: 850;
}

.notice-row {
  border: 1px solid #fed7aa;
  background: #fff7ed;
  color: #9a3412;
}

.success-row {
  border: 1px solid #bbf7d0;
  background: #f0fdf4;
  color: #047857;
}

.source-strip {
  display: flex;
  flex-wrap: wrap;
  gap: 0.55rem;
  align-items: center;
  color: #64748b;
  font-size: 0.78rem;
  font-weight: 850;
}

.source-strip a {
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.76);
  color: #0f766e;
  padding: 0.35rem 0.65rem;
  text-decoration: none;
}

.loading-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 0.85rem;
}

.skeleton-block {
  height: 140px;
  border-radius: 8px;
  background:
    linear-gradient(90deg, rgba(255, 255, 255, 0.42), rgba(255, 255, 255, 0.9), rgba(255, 255, 255, 0.42)),
    #e0f2fe;
  background-size: 240% 100%;
  animation: shimmer 1.4s infinite;
}

.visually-hidden {
  position: absolute;
  width: 1px;
  height: 1px;
  overflow: hidden;
  clip: rect(0 0 0 0);
  white-space: nowrap;
}

@keyframes shimmer {
  0% {
    background-position: 120% 0;
  }
  100% {
    background-position: -120% 0;
  }
}

@media (max-width: 1280px) {
  .document-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .summary-grid,
  .declaration-layout,
  .threshold-grid,
  .loading-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .admin-main-grid {
    grid-template-columns: 1fr;
  }

  .form-panel {
    position: static;
  }
}

@media (max-width: 760px) {
  .admin-workspace {
    padding-inline-start: max(12px, env(safe-area-inset-left));
    padding-inline-end: max(12px, env(safe-area-inset-right));
  }

  .admin-header,
  .section-heading,
  .header-tools,
  .task-title-row {
    align-items: flex-start;
    flex-direction: column;
  }

  .summary-grid,
  .document-grid,
  .declaration-layout,
  .threshold-grid,
  .form-grid,
  .metrics-strip,
  .loading-grid {
    grid-template-columns: 1fr;
  }

  .task-card {
    grid-template-columns: 10px minmax(0, 1fr);
  }

  .task-actions {
    grid-column: 2;
    justify-content: flex-start;
    flex-wrap: wrap;
  }

  .document-actions,
  .declaration-actions {
    align-items: stretch;
    flex-direction: column;
  }

  .icon-button {
    width: 100%;
  }

  .proof-item-row {
    grid-template-columns: minmax(0, 1fr) auto;
  }

  .proof-item-row strong {
    grid-column: 1 / -1;
  }
}
</style>
