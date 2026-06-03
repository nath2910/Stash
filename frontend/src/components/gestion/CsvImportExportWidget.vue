<template>
  <section
    class="csv-widget rounded-2xl border p-3 shadow-sm space-y-2 relative overflow-hidden"
  >
    <div class="pointer-events-none absolute inset-0 bg-[radial-gradient(circle_at_20%_20%,#7c3aed22,transparent_35%),radial-gradient(circle_at_80%_0%,#22c55e22,transparent_35%)]"></div>
    <div class="relative flex items-center justify-between gap-3">
      <div class="min-w-0">
        <p class="text-[11px] uppercase tracking-[0.2em] text-violet-200/70">Données</p>
        <h3 class="text-lg font-semibold leading-tight text-white">Import / export</h3>
      </div>
      <div class="hidden sm:flex items-center gap-2 text-xs text-slate-300/70">
        <span class="h-2 w-2 rounded-full bg-emerald-400 shadow-[0_0_10px_#34d39999]"></span>
        Prêt
      </div>
    </div>

    <!-- Actions -->
    <div class="relative grid grid-cols-1 sm:grid-cols-[0.8fr_1.2fr] gap-3">
      <div
        v-if="importing || parsing"
        class="absolute inset-0 rounded-2xl border border-violet-400/20 bg-slate-950/70 backdrop-blur-sm flex flex-col gap-3 items-center justify-center z-10 text-white text-sm"
      >
        <div class="loading-spinner h-8 w-8 border-2 border-violet-200 border-t-transparent rounded-full"></div>
        <div class="text-center leading-tight">
          <div class="font-semibold">{{ parsing ? 'Analyse du fichier...' : 'Import en cours...' }}</div>
          <div class="text-xs text-slate-300">{{ parsing ? 'Validation des colonnes et lignes' : `${progress}%` }}</div>
        </div>
        <div v-if="importing" class="w-40 h-2 bg-slate-800 rounded-full overflow-hidden">
          <div
            class="h-full bg-gradient-to-r from-violet-500 via-blue-400 to-emerald-400 transition-all duration-200"
            :style="{ width: progress + '%' }"
          ></div>
        </div>
      </div>

      <!-- EXPORT -->
      <button
        type="button"
        class="w-full px-3 py-2.5 rounded-2xl bg-slate-800/70 hover:bg-slate-800 border border-white/10 text-white transition flex items-center justify-between gap-3 disabled:opacity-50 relative overflow-hidden"
        :disabled="!rowsToExport.length"
        @click="exportCsv"
      >
        <div class="flex items-center gap-3">
          <span
            class="inline-flex items-center justify-center h-9 w-9 rounded-2xl bg-white/5 border border-white/10 text-xs font-semibold"
          >
            CSV
          </span>
          <div class="text-left">
            <div class="text-sm font-semibold">Exporter le CSV</div>
            <div class="text-xs text-white/60">{{ rowsToExport.length }} ligne(s)</div>
          </div>
        </div>
        <div class="text-[10px] px-2 py-1 rounded-full bg-emerald-500/15 border border-emerald-400/20 text-emerald-200">
          1 clic
        </div>
      </button>

      <!-- IMPORT -->
      <div
        class="rounded-2xl border bg-slate-900/50 p-3 space-y-2 transition"
        :class="dragOver ? 'border-violet-300/70 bg-violet-500/10' : 'border-white/10'"
        @dragover.prevent="onDragOver"
        @dragleave.prevent="onDragLeave"
        @drop.prevent="onDropFile"
      >
        <div class="flex items-center justify-between gap-3">
          <div class="min-w-0">
            <div class="text-sm font-semibold text-white flex items-center gap-2">
              <span class="h-2 w-2 rounded-full bg-violet-400 shadow-[0_0_10px_#a78bfa]"></span>
              Importer un fichier
            </div>
            <div class="text-xs text-white/60 truncate flex items-center gap-1">
              {{ fileName ? fileName : 'CSV, XLSX, XLS, TSV ou TXT' }}
              <span v-if="selectedFile" class="text-[10px] text-violet-200/70">({{ prettySize }})</span>
            </div>
          </div>

          <div class="flex items-center gap-2">
            <button
              type="button"
              class="px-3 py-1.5 rounded-xl bg-white/5 hover:bg-white/10 border border-white/10 text-white text-sm transition"
              @click="pickFile"
              :disabled="importing || parsing"
            >
              Parcourir
            </button>

            <button
              type="button"
              class="px-3 py-1.5 rounded-xl bg-violet-600 hover:bg-violet-500 text-white text-sm disabled:opacity-50 transition inline-flex items-center gap-2"
              :disabled="importing || parsing || !canConfirmImport"
              @click="importNow"
            >
              <span v-if="importing" class="loading-spinner h-4 w-4 border-2 border-white/50 border-t-transparent rounded-full"></span>
              {{ importing ? 'Import...' : importButtonLabel }}
            </button>
          </div>
        </div>

        <input
          ref="fileInput"
          type="file"
          class="hidden"
          accept=".csv,.tsv,.txt,.json,.xls,.xlsx,text/csv,text/plain,text/tab-separated-values,application/json,application/vnd.ms-excel,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
          @change="onFilePicked"
        />

        <div class="rounded-xl border border-white/10 bg-white/[0.03] px-3 py-2 text-xs text-slate-300">
          <span class="font-semibold text-white">Formats</span>
          <span class="ml-2 text-slate-400">CSV, XLSX, XLS, TSV, TXT, JSON</span>
        </div>

        <div
          v-if="preview"
          class="mt-3 space-y-3 rounded-2xl border border-white/10 bg-slate-950/40 p-3"
        >
          <div class="grid grid-cols-2 gap-2 text-xs sm:grid-cols-4">
            <div class="rounded-xl border border-white/10 bg-white/[0.03] p-2">
              <div class="text-white/50">Detectees</div>
              <div class="text-base font-semibold text-white">{{ preview.rowsDetected }}</div>
            </div>
            <div class="rounded-xl border border-emerald-400/20 bg-emerald-400/10 p-2">
              <div class="text-emerald-100/70">Valides</div>
              <div class="text-base font-semibold text-emerald-100">{{ preview.validRows }}</div>
            </div>
            <div class="rounded-xl border border-red-400/20 bg-red-400/10 p-2">
              <div class="text-red-100/70">Invalides</div>
              <div class="text-base font-semibold text-red-100">{{ preview.invalidRows }}</div>
            </div>
            <div class="rounded-xl border border-sky-400/20 bg-sky-400/10 p-2">
              <div class="text-sky-100/70">Items crees</div>
              <div class="text-base font-semibold text-sky-100">{{ preview.validItems }}</div>
            </div>
          </div>

          <div
            v-if="preview.invalidRows"
            class="rounded-xl border border-amber-300/20 bg-amber-300/10 p-3 text-xs text-amber-100"
          >
            {{ preview.invalidRows }} ligne(s) invalide(s) ne seront pas importees. Corrige le fichier ou ajuste le mapping si besoin.
          </div>

          <div class="rounded-xl border border-white/10 bg-slate-900/60 p-3 text-xs text-slate-200">
            <div class="flex items-center justify-between gap-3">
              <div>
                <div class="font-semibold text-white">Mapping des colonnes</div>
                <p class="mt-1 text-slate-400">Corrige les colonnes reconnues avant de confirmer.</p>
              </div>
              <button
                type="button"
                class="rounded-lg border border-white/10 bg-white/5 px-2.5 py-1.5 text-[11px] font-semibold text-slate-100 transition hover:bg-white/10"
                @click="rebuildPreviewFromMapping"
              >
                Reanalyser
              </button>
            </div>
            <div class="mt-3 grid gap-2 sm:grid-cols-2 xl:grid-cols-3">
              <label
                v-for="field in mappingFields"
                :key="field.key"
                class="grid gap-1 rounded-lg border border-white/10 bg-white/[0.03] p-2"
              >
                <span class="text-[11px] font-semibold text-slate-300">
                  {{ mappingLabels[field.key] ?? field.key }}
                  <span v-if="field.required" class="text-red-200">*</span>
                </span>
                <select
                  v-model="mappingDraft[field.key]"
                  class="min-h-9 rounded-lg border border-slate-700 bg-slate-950 px-2 text-xs text-slate-100"
                  @change="rebuildPreviewFromMapping"
                >
                  <option value="">Non utilise</option>
                  <option v-for="header in headerOptions" :key="`${field.key}-${header}`" :value="header">
                    {{ header }}
                  </option>
                </select>
              </label>
            </div>
          </div>

          <div class="rounded-xl border border-white/10 bg-slate-900/60 p-3 text-xs text-slate-200">
            <div class="font-semibold text-white">Colonnes reconnues</div>
            <div class="mt-2 flex flex-wrap gap-1.5">
              <span
                v-for="entry in mappingEntries"
                :key="entry.key"
                class="rounded-full border border-white/10 bg-white/5 px-2 py-1"
              >
                {{ entry.label }} : {{ entry.value }}
              </span>
            </div>
            <p class="mt-2 text-slate-400">
              Google Sheets et Apple Numbers : exporte en CSV ou XLSX, sans identifiant ni secret.
            </p>
          </div>

          <div
            v-if="preview.unknownHeaders.length"
            class="rounded-xl border border-amber-300/20 bg-amber-300/10 p-3 text-xs text-amber-100"
          >
            <div class="font-semibold">Colonnes non reconnues ou trop ambigues</div>
            <div class="mt-2 flex flex-wrap gap-1.5">
              <span
                v-for="header in preview.unknownHeaders"
                :key="header"
                class="rounded-full border border-amber-200/20 bg-amber-200/10 px-2 py-1"
              >
                {{ header }}
              </span>
            </div>
            <p class="mt-2 text-amber-100/80">
              Elles sont ignorees par defaut. Tu peux les mapper manuellement avant de confirmer.
            </p>
          </div>

          <div v-if="previewErrors.length" class="rounded-xl border border-red-400/20 bg-red-400/10 p-3">
            <div class="text-xs font-semibold text-red-100">Erreurs a corriger</div>
            <ul class="mt-2 space-y-1 text-xs text-red-100/90">
              <li v-for="err in previewErrors" :key="err">{{ err }}</li>
            </ul>
          </div>

          <div v-if="previewRows.length" class="overflow-hidden rounded-xl border border-white/10">
            <div class="max-h-56 overflow-auto">
              <table class="w-full text-left text-xs">
                <thead class="sticky top-0 bg-slate-950 text-slate-300">
                  <tr>
                    <th class="px-3 py-2 font-semibold">Ligne</th>
                    <th class="px-3 py-2 font-semibold">Statut</th>
                    <th class="px-3 py-2 font-semibold">Item</th>
                    <th class="px-3 py-2 font-semibold">Details</th>
                  </tr>
                </thead>
                <tbody class="divide-y divide-white/10 bg-slate-950/40 text-slate-200">
                  <tr v-for="row in previewRows" :key="row.rowNumber">
                    <td class="px-3 py-2 text-slate-400">{{ row.rowNumber }}</td>
                    <td class="px-3 py-2">
                      <span
                        class="rounded-full px-2 py-1 text-[11px] font-semibold"
                        :class="row.status === 'valid' ? 'bg-emerald-400/10 text-emerald-100' : 'bg-red-400/10 text-red-100'"
                      >
                        {{ row.status === 'valid' ? 'Valide' : 'Invalide' }}
                      </span>
                    </td>
                    <td class="max-w-[220px] truncate px-3 py-2">{{ row.name || '-' }}</td>
                    <td class="px-3 py-2 text-slate-300">
                      {{ row.errors.length ? row.errors.join(' | ') : row.warnings.join(' | ') || `${row.quantity} item(s)` }}
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>

        <div
          v-if="successMsg"
          class="mt-3 rounded-xl border border-emerald-400/30 bg-emerald-400/10 p-3 text-sm text-emerald-100 flex items-center gap-2"
        >
          <span class="h-2 w-2 rounded-full bg-emerald-300 shadow-[0_0_8px_#34d399]"></span>
          {{ successMsg }}
        </div>

        <div
          v-if="errorMsg"
          class="mt-3 rounded-xl border border-red-500/30 bg-red-500/10 p-3 text-sm text-red-200"
        >
          {{ errorMsg }}
        </div>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, ref } from 'vue'
import type { ParseResult } from 'papaparse'
import SnkVenteServices from '@/services/SnkVenteServices.js'
import {
  analyzeImportRows,
  buildStockExportCsv,
  detectDelimiter,
  extractTableFrom2D,
  isExcelFile,
  isJsonFile,
  parseJsonContent,
  parseLooseKeyValueText,
  resolveImportMapping,
  type CsvRow,
  type ImportMapping,
  type ImportPreview,
  type ParsedTable,
} from '@/utils/stockImportExport'

/**
 * Exporte par defaut ce que tu passes dans filteredRows.
 */
const props = defineProps<{
  filteredRows: unknown[]
}>()

const emit = defineEmits<{
  (e: 'imported'): void
}>()

/* ------------------ EXPORT ------------------ */
const rowsToExport = computed(() => (Array.isArray(props.filteredRows) ? props.filteredRows : []))

function exportCsv() {
  const rows = rowsToExport.value
  if (!rows.length) return

  const csvContent = buildStockExportCsv(rows)
  const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)

  const a = document.createElement('a')
  a.href = url
  a.download = `stash_export_${new Date().toISOString().slice(0, 10)}.csv`
  document.body.appendChild(a)
  a.click()
  a.remove()

  URL.revokeObjectURL(url)
}

/* ------------------ IMPORT ------------------ */
const fileInput = ref<HTMLInputElement | null>(null)
const selectedFile = ref<File | null>(null)
const fileName = ref('')
const dragOver = ref(false)

const parsing = ref(false)
const importing = ref(false)
const progress = ref(0)
const successMsg = ref('')
const errorMsg = ref('')
const preview = ref<ImportPreview | null>(null)
const parsedTable = ref<ParsedTable | null>(null)
const mappingDraft = ref<ImportMapping>({})
let progressTimer: number | null = null
let progressResetTimer: number | null = null
type PapaModule = typeof import('papaparse')
type PapaModuleCompat = PapaModule & { default?: PapaModule }

let papaModulePromise: Promise<PapaModuleCompat> | null = null
let xlsxModulePromise: Promise<typeof import('xlsx')> | null = null

async function loadPapa() {
  papaModulePromise ??= import('papaparse') as Promise<PapaModuleCompat>
  const module = await papaModulePromise
  return module.default ?? module
}

async function loadXlsx() {
  xlsxModulePromise ??= import('xlsx')
  return xlsxModulePromise
}

const prettySize = computed(() => {
  const f = selectedFile.value
  if (!f) return ''
  if (f.size < 1024) return f.size + ' o'
  if (f.size < 1024 * 1024) return (f.size / 1024).toFixed(1) + ' ko'
  return (f.size / (1024 * 1024)).toFixed(2) + ' Mo'
})

const canConfirmImport = computed(() => Boolean(preview.value?.payload?.length))
const importButtonLabel = computed(() => {
  if (!preview.value) return 'Importer'
  return `Importer ${preview.value.validItems} item(s)`
})
const previewRows = computed(() => preview.value?.rows.slice(0, 12) ?? [])
const previewErrors = computed(() =>
  (preview.value?.rows ?? [])
    .filter((row) => row.errors.length)
    .slice(0, 6)
    .map((row) => `Ligne ${row.rowNumber} : ${row.errors.join(' | ')}`),
)
const mappingLabels: Record<string, string> = {
  name: 'Modele',
  brand: 'Marque / categorie',
  size: 'Taille',
  retail: "Prix d'achat",
  resell: 'Prix vente reel/estime',
  quantity: 'Quantite',
  condition: 'Etat',
  sku: 'SKU',
  dateAchat: "Date d'achat",
  dateVente: 'Date de vente',
  supplier: 'Fournisseur',
  notes: 'Notes',
  type: "Type d'item",
  colorway: 'Coloris',
  boxCondition: 'Etat boite',
  metadata: 'Metadata JSON',
}
const mappingFields = [
  { key: 'name', required: true },
  { key: 'brand' },
  { key: 'size' },
  { key: 'retail' },
  { key: 'resell' },
  { key: 'quantity' },
  { key: 'condition' },
  { key: 'sku' },
  { key: 'dateAchat' },
  { key: 'dateVente' },
  { key: 'supplier' },
  { key: 'notes' },
  { key: 'type' },
  { key: 'colorway' },
  { key: 'boxCondition' },
  { key: 'metadata' },
]
const headerOptions = computed(() => preview.value?.headers ?? parsedTable.value?.headers ?? [])
const mappingEntries = computed(() =>
  Object.entries(preview.value?.mapping ?? {})
    .filter(([, value]) => Boolean(value))
    .map(([key, value]) => ({ key, label: mappingLabels[key] ?? key, value })),
)

function pickFile() {
  successMsg.value = ''
  errorMsg.value = ''
  preview.value = null
  parsedTable.value = null
  mappingDraft.value = {}
  selectedFile.value = null
  fileName.value = ''
  if (fileInput.value) fileInput.value.value = ''
  fileInput.value?.click()
}

function resetPreviewState() {
  successMsg.value = ''
  errorMsg.value = ''
  preview.value = null
  parsedTable.value = null
  mappingDraft.value = {}
}

function onFilePicked(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0] ?? null
  selectedFile.value = file
  fileName.value = file?.name ?? ''
  resetPreviewState()
  if (file) void preparePreview(file)
}

function onDragOver() {
  if (importing.value || parsing.value) return
  dragOver.value = true
}

function onDragLeave() {
  dragOver.value = false
}

function onDropFile(event: DragEvent) {
  dragOver.value = false
  if (importing.value || parsing.value) return
  const file = event.dataTransfer?.files?.[0] ?? null
  if (!file) return
  selectedFile.value = file
  fileName.value = file.name
  resetPreviewState()
  void preparePreview(file)
}

function startProgress() {
  progress.value = 8
  if (progressTimer) window.clearInterval(progressTimer)
  progressTimer = window.setInterval(() => {
    // ease toward 90%
    if (progress.value < 90) {
      progress.value = Math.min(90, progress.value + Math.random() * 10)
    }
  }, 200)
}

function stopProgress(finalValue = 100) {
  if (progressTimer) {
    window.clearInterval(progressTimer)
    progressTimer = null
  }
  if (progressResetTimer) {
    window.clearTimeout(progressResetTimer)
    progressResetTimer = null
  }
  progress.value = finalValue
  progressResetTimer = window.setTimeout(() => {
    progress.value = 0
    progressResetTimer = null
  }, 600)
}

async function parseExcel(file: File): Promise<ParsedTable> {
  const XLSX = await loadXlsx()
  const buffer = await file.arrayBuffer()
  const workbook = XLSX.read(buffer, { type: 'array' })
  const sheetNameWithData =
    workbook.SheetNames.find((name) => {
      const rows = XLSX.utils.sheet_to_json<unknown[]>(workbook.Sheets[name], { header: 1, defval: '' }) as
        | unknown[][]
        | undefined
      if (!rows) return false
      return rows.some((row) => (row || []).some((c) => String(c).trim()))
    }) || workbook.SheetNames[0]

  const sheet = workbook.Sheets[sheetNameWithData]
  if (!sheet) return { headers: [], rows: [] }

  const rows2D = XLSX.utils.sheet_to_json<unknown[]>(sheet, { header: 1, defval: '' }) as
    | unknown[][]
    | undefined
  return extractTableFrom2D(rows2D || [])
}

async function parseDelimitedText(text: string): Promise<ParsedTable> {
  const Papa = await loadPapa()
  const preferredDelimiter = detectDelimiter(text)
  const delimiters = Array.from(new Set([preferredDelimiter, undefined, ';', '\t', ',', '|'].filter(Boolean))) as string[]
  const attempt = (delimiter?: string) =>
    new Promise<ParsedTable>((resolve) => {
      Papa.parse<CsvRow>(text, {
        header: true,
        skipEmptyLines: 'greedy',
        delimiter,
        complete: (res: ParseResult<CsvRow>) => {
          resolve({
            headers: ((res.meta.fields ?? []) as string[]).filter((header) => String(header).trim()),
            rows: (res.data ?? []).filter(Boolean) as CsvRow[],
          })
        },
      })
    })

  const attempts = await Promise.all((delimiters.length ? delimiters : [undefined]).map((delimiter) => attempt(delimiter)))
  const best = attempts.sort((a, b) => {
    const scoreA = a.headers.length * 100 + a.rows.length
    const scoreB = b.headers.length * 100 + b.rows.length
    return scoreB - scoreA
  })[0] ?? { headers: [], rows: [] }

  if (best.headers.length >= 2) return best

  const fallback = await new Promise<ParsedTable>((resolve) => {
    Papa.parse<string[]>(text, {
      header: false,
      skipEmptyLines: 'greedy',
      delimiter: preferredDelimiter || undefined,
      complete: (res: ParseResult<string[]>) => {
        const rows2D = res.data as unknown[][]
        resolve(extractTableFrom2D(rows2D || []))
      },
    })
  })

  return fallback.headers.length >= 2 ? fallback : best
}

async function parseTextContent(text: string): Promise<ParsedTable> {
  const content = text.replace(/^\uFEFF/, '').trim()
  if (!content) return { headers: [], rows: [] }

  if (/^[\[{]/.test(content)) {
    try {
      const jsonTable = parseJsonContent(content)
      if (jsonTable.headers.length >= 2) return jsonTable
    } catch {
      // fallback to delimited parsing
    }
  }

  const delimitedTable = await parseDelimitedText(content)
  if (delimitedTable.headers.length >= 2) return delimitedTable

  return parseLooseKeyValueText(content)
}

async function parseCsv(file: File): Promise<ParsedTable> {
  return parseTextContent(await file.text())
}

async function parseFileSmart(file: File): Promise<ParsedTable> {
  const table = isExcelFile(file)
    ? await parseExcel(file)
    : isJsonFile(file)
      ? parseJsonContent(await file.text())
      : await parseCsv(file)
  // si le header est vide (ex: 1re ligne vide ou colonnes fusionnées), on reprend les clés détectées
  if (table.headers.length < 2 && table.rows.length) {
    table.headers = Object.keys(table.rows[0] ?? {}).map((h) => h || '')
  }
  return table
}

function applyPreviewFromMapping(mapping: ImportMapping) {
  const table = parsedTable.value
  if (!table) return
  const analysis = analyzeImportRows(table.rows, table.headers, mapping, rowsToExport.value)
  preview.value = analysis
  mappingDraft.value = { ...analysis.mapping }
  errorMsg.value = analysis.payload.length
    ? ''
    : analysis.rows[0]?.errors?.[0] || 'Aucune ligne valide a importer.'
}

function rebuildPreviewFromMapping() {
  applyPreviewFromMapping(mappingDraft.value)
}

function errorMessage(err: unknown, fallback: string) {
  if (!err || typeof err !== 'object') return fallback
  const error = err as {
    response?: { data?: { message?: unknown; error?: unknown } }
    message?: unknown
  }
  return (
    String(error.response?.data?.message ?? '') ||
    String(error.response?.data?.error ?? '') ||
    String(error.message ?? '') ||
    fallback
  )
}

async function preparePreview(file: File) {
  parsing.value = true
  errorMsg.value = ''
  successMsg.value = ''
  preview.value = null
  parsedTable.value = null
  mappingDraft.value = {}
  try {
    const parsed = await parseFileSmart(file)
    const headers = parsed.headers
    const rows = parsed.rows

    if (headers.length < 2) {
      throw new Error('Fichier illisible : trop peu de colonnes detectees ou structure non reconnue.')
    }

    parsedTable.value = { headers, rows }
    applyPreviewFromMapping(resolveImportMapping(headers))
  } catch (err: unknown) {
    console.error(err)
    preview.value = null
    parsedTable.value = null
    mappingDraft.value = {}
    errorMsg.value = errorMessage(err, 'Erreur analyse import')
  } finally {
    parsing.value = false
  }
}

async function importNow() {
  const file = selectedFile.value
  if (!preview.value && file) {
    await preparePreview(file)
  }

  const payload = preview.value?.payload ?? []
  if (!payload.length) {
    errorMsg.value = 'Aucune ligne valide a importer.'
    return
  }

  importing.value = true
  startProgress()
  successMsg.value = ''
  errorMsg.value = ''

  try {
    const res = await SnkVenteServices.importBulk(payload)

    const created = res?.data?.created ?? null
    if (created === 0) throw new Error('Import fait mais 0 ligne créee.')

    successMsg.value = `Import OK (${created ?? payload.length} item(s))`
    emit('imported')

    selectedFile.value = null
    fileName.value = ''
    preview.value = null
    parsedTable.value = null
    mappingDraft.value = {}
    stopProgress(100)
  } catch (err: unknown) {
    console.error(err)
    errorMsg.value = errorMessage(err, 'Erreur import')
    stopProgress(0)
  } finally {
    importing.value = false
    if (fileInput.value) fileInput.value.value = ''
  }
}

onBeforeUnmount(() => {
  if (progressTimer) {
    window.clearInterval(progressTimer)
    progressTimer = null
  }
  if (progressResetTimer) {
    window.clearTimeout(progressResetTimer)
    progressResetTimer = null
  }
})
</script>

<style scoped>
.csv-widget {
  border-color: rgba(148, 163, 184, 0.24);
  background: #fbfaf7;
  color: #0f172a;
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.05);
}

.csv-widget::before {
  display: none;
}

.csv-widget > .pointer-events-none {
  display: none;
}

.csv-widget :is(.text-white, .text-slate-100, .text-slate-200) {
  color: #0f172a;
}

.csv-widget :is(.text-violet-200\/70, .text-slate-300, .text-slate-300\/70, .text-slate-400, .text-white\/60, .text-white\/50) {
  color: #64748b;
}

.csv-widget :is(.text-emerald-100, .text-emerald-200) {
  color: #0f766e;
}

.csv-widget :is(.text-red-100, .text-red-200) {
  color: #b91c1c;
}

.csv-widget :is(.text-amber-100, .text-amber-100\/80) {
  color: #b45309;
}

.csv-widget [class*='bg-slate-'],
.csv-widget [class*='bg-white/'],
.csv-widget [class*='bg-white\\['] {
  background: rgba(255, 255, 255, 0.72);
}

.csv-widget [class*='border-white'],
.csv-widget [class*='border-slate-'] {
  border-color: rgba(148, 163, 184, 0.22);
}

.csv-widget button {
  border-color: rgba(148, 163, 184, 0.24);
  background: rgba(255, 255, 255, 0.78);
  color: #0f172a;
  font-weight: 800;
}

.csv-widget button:not(:disabled):hover {
  border-color: rgba(20, 184, 166, 0.34);
  background: #ffffff;
  color: #0f766e;
}

.csv-widget button.bg-violet-600 {
  background: linear-gradient(135deg, #0f766e, #0e7490);
  color: #ffffff;
}

.csv-widget select {
  border-color: rgba(148, 163, 184, 0.28);
  background: #ffffff;
  color: #0f172a;
}

.csv-widget thead {
  background: #f0fdfa;
  color: #0f766e;
}

.csv-widget tbody {
  background: rgba(255, 255, 255, 0.84);
  color: #0f172a;
}

.loading-spinner {
  animation: spin 0.9s linear infinite;
}
@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
