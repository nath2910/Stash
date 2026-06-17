<template>
  <Teleport to="body">
    <div v-if="modelValue" class="legal-profile-overlay" role="presentation">
      <section
        class="legal-profile-modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="legal-profile-title"
        @click.stop
      >
        <header class="legal-profile-header">
          <p class="legal-profile-kicker">Profil administratif</p>
          <h2 id="legal-profile-title">{{ modalTitle }}</h2>
          <p>{{ modalDescription }}</p>
        </header>

        <div v-if="step === 'choice'" class="legal-profile-choice-grid">
          <button
            v-for="option in LEGAL_PROFILE_OPTIONS"
            :key="option.type"
            type="button"
            class="legal-profile-choice"
            :class="{ 'is-selected': selectedType === option.type }"
            :disabled="saving"
            @click="selectType(option.type)"
          >
            <span class="legal-profile-choice-head">
              <strong>{{ option.title }}</strong>
              <CheckCircle2 v-if="selectedType === option.type" class="legal-profile-choice-icon" />
            </span>
            <span>{{ option.description }}</span>
            <small v-if="option.detail">{{ option.detail }}</small>
            <span class="legal-profile-traits">
              <span v-for="trait in option.traits" :key="trait">{{ trait }}</span>
            </span>
          </button>
        </div>

        <form v-else class="legal-profile-form" @submit.prevent="submit">
          <div>
            <p class="legal-profile-form-title">Informations micro-entreprise</p>
            <p class="legal-profile-form-help">
              Renseigne les informations obligatoires pour valider ton profil.
            </p>
          </div>

          <label class="legal-profile-field">
            <span>Numéro SIRET</span>
            <input
              v-model.trim="form.siret"
              type="text"
              inputmode="numeric"
              autocomplete="off"
              maxlength="14"
              placeholder="14 chiffres"
              :aria-invalid="Boolean(siretMessage)"
            />
            <small v-if="siretMessage">{{ siretMessage }}</small>
          </label>

          <label class="legal-profile-field">
            <span>Periodicite URSSAF</span>
            <select v-model="form.declarationFrequency">
              <option value="MONTHLY">Mensuelle</option>
              <option value="QUARTERLY">Trimestrielle</option>
              <option value="UNKNOWN">Je ne sais pas encore</option>
            </select>
          </label>

          <label class="legal-profile-field">
            <span>Versement liberatoire</span>
            <select v-model="form.withholdingTaxOption">
              <option value="UNKNOWN">Inconnu</option>
              <option value="YES">Oui</option>
              <option value="NO">Non</option>
            </select>
          </label>

          <label class="legal-profile-field">
            <span>Franchise en base de TVA</span>
            <select v-model="form.vatFranchise">
              <option value="UNKNOWN">Inconnu</option>
              <option value="YES">Oui</option>
              <option value="NO">Non</option>
            </select>
          </label>

          <label class="legal-profile-field">
            <span>Date de debut d'activite</span>
            <input v-model="form.activityStartDate" type="date" />
          </label>

          <div class="legal-profile-readonly-list" aria-label="Paramètres retenus">
            <p>Catégorie fiscale : BIC - bénéfices industriels et commerciaux</p>
            <p>Régime : micro-entreprise</p>
            <p>Activité : achat-revente / vente de marchandises</p>
            <p>CA a declarer : montant brut encaisse, sans deduction des achats ou frais</p>
          </div>
        </form>

        <div v-if="apiError" class="legal-profile-error" role="alert">
          {{ apiError }}
        </div>

        <footer class="legal-profile-actions">
          <button
            v-if="step === 'micro'"
            type="button"
            class="legal-profile-button is-soft"
            :disabled="saving"
            @click="step = 'choice'"
          >
            <ArrowLeft class="legal-profile-button-icon" />
            <span>Retour</span>
          </button>

          <button
            v-if="!mandatory"
            type="button"
            class="legal-profile-button is-ghost"
            :disabled="saving"
            @click="cancel"
          >
            Annuler
          </button>

          <button
            type="button"
            class="legal-profile-button is-primary"
            :disabled="saving || !selectedType"
            @click="submit"
          >
            <RefreshCw v-if="saving" class="legal-profile-button-icon is-spinning" />
            <span>{{ primaryButtonLabel }}</span>
          </button>
        </footer>
      </section>
    </div>
  </Teleport>
</template>

<script setup>
import { computed, onBeforeUnmount, reactive, ref, watch } from 'vue'
import { ArrowLeft, CheckCircle2, RefreshCw } from 'lucide-vue-next'
import {
  buildLegalProfilePayload,
  LEGAL_PROFILE_OPTIONS,
  LEGAL_PROFILE_TYPE,
  MICRO_LEGAL_PROFILE_DEFAULTS,
  isValidSiret,
  normalizeLegalProfile,
} from '@/constants/legalProfile'
import LegalProfileService from '@/services/LegalProfileService'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false,
  },
  initialProfile: {
    type: Object,
    default: null,
  },
  mandatory: {
    type: Boolean,
    default: true,
  },
  title: {
    type: String,
    default: "Avant d'accéder à l'administratif",
  },
  description: {
    type: String,
    default: "Pour adapter les informations administratives à ton activité, indique ton profil actuel.",
  },
})

const emit = defineEmits(['update:modelValue', 'saved', 'cancel'])

const step = ref('choice')
const selectedType = ref('')
const saving = ref(false)
const apiError = ref('')
const form = reactive({
  siret: '',
  taxCategory: MICRO_LEGAL_PROFILE_DEFAULTS.taxCategory,
  businessRegime: MICRO_LEGAL_PROFILE_DEFAULTS.businessRegime,
  businessActivityType: MICRO_LEGAL_PROFILE_DEFAULTS.businessActivityType,
  declaredRevenueThreshold: MICRO_LEGAL_PROFILE_DEFAULTS.declaredRevenueThreshold,
  vatNumber: '',
  vatStatus: 'UNKNOWN',
  declarationFrequency: 'UNKNOWN',
  withholdingTaxOption: 'UNKNOWN',
  vatFranchise: 'UNKNOWN',
  activityStartDate: '',
})

const modalTitle = computed(() => props.title)
const modalDescription = computed(() => props.description)
const primaryButtonLabel = computed(() => {
  if (saving.value) return 'Validation...'
  if (step.value === 'choice' && selectedType.value === LEGAL_PROFILE_TYPE.micro) return 'Continuer'
  return 'Valider mon profil'
})
const siretMessage = computed(() => {
  if (step.value !== 'micro') return ''
  if (!form.siret) return ''
  return isValidSiret(form.siret) ? '' : 'Le SIRET doit contenir exactement 14 chiffres.'
})

watch(
  () => props.modelValue,
  (visible) => {
    if (typeof document !== 'undefined') {
      document.body.classList.toggle('legal-profile-modal-open', visible)
    }
    if (visible) resetFromInitialProfile()
  },
  { immediate: true },
)

onBeforeUnmount(() => {
  if (typeof document !== 'undefined') {
    document.body.classList.remove('legal-profile-modal-open')
  }
})

function resetFromInitialProfile() {
  const profile = normalizeLegalProfile(props.initialProfile || {})
  selectedType.value = profile.legalProfileType || ''
  step.value = 'choice'
  apiError.value = ''
  form.siret = profile.siret || ''
  form.taxCategory = profile.taxCategory || MICRO_LEGAL_PROFILE_DEFAULTS.taxCategory
  form.businessRegime = profile.businessRegime || MICRO_LEGAL_PROFILE_DEFAULTS.businessRegime
  form.businessActivityType =
    profile.businessActivityType || MICRO_LEGAL_PROFILE_DEFAULTS.businessActivityType
  form.declaredRevenueThreshold =
    profile.declaredRevenueThreshold || MICRO_LEGAL_PROFILE_DEFAULTS.declaredRevenueThreshold
  form.vatNumber = profile.vatNumber || ''
  form.vatStatus = profile.vatStatus || 'UNKNOWN'
  form.declarationFrequency = profile.declarationFrequency || 'UNKNOWN'
  form.withholdingTaxOption = profile.withholdingTaxOption || 'UNKNOWN'
  form.vatFranchise = profile.vatFranchise || 'UNKNOWN'
  form.activityStartDate = profile.activityStartDate || ''
}

function selectType(type) {
  selectedType.value = type
  apiError.value = ''
}

function cancel() {
  emit('cancel')
  emit('update:modelValue', false)
}

async function submit() {
  apiError.value = ''

  if (!selectedType.value) {
    apiError.value = 'Choisis un profil avant de continuer.'
    return
  }

  if (step.value === 'choice' && selectedType.value === LEGAL_PROFILE_TYPE.micro) {
    step.value = 'micro'
    return
  }

  if (selectedType.value === LEGAL_PROFILE_TYPE.micro && !isValidSiret(form.siret)) {
    apiError.value = 'Renseigne un SIRET valide de 14 chiffres.'
    return
  }

  saving.value = true
  try {
    const payload = buildLegalProfilePayload(selectedType.value, form)
    const savedProfile = await LegalProfileService.updateLegalProfile(payload)
    emit('saved', savedProfile)
    emit('update:modelValue', false)
  } catch (error) {
    apiError.value =
      error?.response?.data?.message || "Impossible d'enregistrer le profil administratif."
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
:global(body.legal-profile-modal-open) {
  overflow: hidden;
}

.legal-profile-overlay {
  position: fixed;
  inset: 0;
  z-index: 10000;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: clamp(1rem, 3vw, 2rem);
  background: rgba(15, 23, 42, 0.38);
  backdrop-filter: blur(9px) saturate(120%);
}

.legal-profile-modal {
  display: grid;
  width: min(100%, 680px);
  max-height: min(92dvh, 780px);
  overflow-y: auto;
  gap: 1rem;
  border: 1px solid rgba(148, 163, 184, 0.42);
  border-radius: 8px;
  background: #ffffff;
  color: #172033;
  padding: clamp(1rem, 2.4vw, 1.4rem);
  box-shadow: 0 28px 80px rgba(15, 23, 42, 0.26);
}

.legal-profile-header {
  display: grid;
  gap: 0.4rem;
}

.legal-profile-kicker {
  margin: 0;
  color: #0f766e;
  font-size: 0.72rem;
  font-weight: 900;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.legal-profile-header h2,
.legal-profile-header p,
.legal-profile-form-title,
.legal-profile-form-help {
  margin: 0;
}

.legal-profile-header h2 {
  color: #111827;
  font-size: clamp(1.35rem, 2.2vw, 1.8rem);
  line-height: 1.1;
}

.legal-profile-header p,
.legal-profile-form-help {
  color: #64748b;
  line-height: 1.45;
  font-size: 0.94rem;
  font-weight: 650;
}

.legal-profile-choice-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.75rem;
}

.legal-profile-choice {
  display: grid;
  gap: 0.55rem;
  min-height: 180px;
  border: 1px solid #dbe4f2;
  border-radius: 8px;
  background: #f8fafc;
  color: #334155;
  padding: 0.9rem;
  text-align: left;
  cursor: pointer;
  transition:
    border-color 140ms ease,
    background 140ms ease,
    box-shadow 140ms ease;
}

.legal-profile-choice:hover,
.legal-profile-choice.is-selected {
  border-color: rgba(15, 118, 110, 0.7);
  background: #ecfdf5;
  box-shadow: 0 14px 32px rgba(15, 118, 110, 0.11);
}

.legal-profile-choice:disabled {
  cursor: not-allowed;
  opacity: 0.72;
}

.legal-profile-choice-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
}

.legal-profile-choice strong {
  color: #111827;
  font-size: 1rem;
}

.legal-profile-choice span,
.legal-profile-choice small {
  line-height: 1.4;
}

.legal-profile-choice > span:not(.legal-profile-choice-head):not(.legal-profile-traits),
.legal-profile-choice > small {
  color: #64748b;
  font-size: 0.88rem;
  font-weight: 700;
}

.legal-profile-choice-icon,
.legal-profile-button-icon {
  width: 17px;
  height: 17px;
  flex: 0 0 auto;
}

.legal-profile-choice-icon {
  color: #0f766e;
}

.legal-profile-traits {
  display: flex;
  flex-wrap: wrap;
  gap: 0.4rem;
  margin-top: auto;
}

.legal-profile-traits span {
  border-radius: 999px;
  background: #e0f2fe;
  color: #0369a1;
  padding: 0.3rem 0.55rem;
  font-size: 0.72rem;
  font-weight: 850;
}

.legal-profile-form {
  display: grid;
  gap: 0.9rem;
}

.legal-profile-form-title {
  color: #111827;
  font-size: 1.05rem;
  font-weight: 850;
}

.legal-profile-field {
  display: grid;
  gap: 0.45rem;
  color: #334155;
  font-size: 0.86rem;
  font-weight: 850;
}

.legal-profile-field input,
.legal-profile-field select {
  min-height: 42px;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  background: #ffffff;
  color: #111827;
  font: inherit;
  padding: 0 0.8rem;
  outline: none;
}

.legal-profile-field input:focus,
.legal-profile-field select:focus {
  border-color: #0f766e;
  box-shadow: 0 0 0 3px rgba(15, 118, 110, 0.14);
}

.legal-profile-field small {
  color: #be123c;
}

.legal-profile-readonly-list {
  display: grid;
  gap: 0.45rem;
  border: 1px solid #dbe4f2;
  border-radius: 8px;
  background: #f8fafc;
  padding: 0.85rem;
}

.legal-profile-readonly-list p {
  margin: 0;
  color: #475569;
  font-size: 0.84rem;
  font-weight: 750;
}

.legal-profile-error {
  border: 1px solid #fecdd3;
  border-radius: 8px;
  background: #fff1f2;
  color: #be123c;
  padding: 0.8rem;
  font-size: 0.86rem;
  font-weight: 800;
}

.legal-profile-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: flex-end;
  gap: 0.6rem;
}

.legal-profile-button {
  display: inline-flex;
  min-height: 40px;
  align-items: center;
  justify-content: center;
  gap: 0.45rem;
  border: 0;
  border-radius: 8px;
  padding: 0 0.9rem;
  font: inherit;
  font-size: 0.86rem;
  font-weight: 900;
  cursor: pointer;
}

.legal-profile-button:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.legal-profile-button.is-primary {
  background: #0f766e;
  color: #ffffff;
}

.legal-profile-button.is-soft {
  background: #ccfbf1;
  color: #0f766e;
}

.legal-profile-button.is-ghost {
  background: #f1f5f9;
  color: #475569;
}

.is-spinning {
  animation: legal-profile-spin 900ms linear infinite;
}

@keyframes legal-profile-spin {
  to {
    transform: rotate(360deg);
  }
}

@media (max-width: 680px) {
  .legal-profile-overlay {
    align-items: stretch;
    padding: 0.75rem;
  }

  .legal-profile-modal {
    max-height: calc(100dvh - 1.5rem);
  }

  .legal-profile-choice-grid {
    grid-template-columns: 1fr;
  }

  .legal-profile-actions,
  .legal-profile-button {
    width: 100%;
  }
}
</style>
