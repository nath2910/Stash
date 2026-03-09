<template>
  <section
    class="rounded-3xl border border-white/10 bg-gradient-to-br from-slate-900/80 via-slate-900/60 to-slate-950/80 p-5 sm:p-6 shadow-2xl space-y-4 relative overflow-hidden"
  >
    <div class="pointer-events-none absolute inset-0 bg-[radial-gradient(circle_at_20%_20%,#7c3aed22,transparent_35%),radial-gradient(circle_at_80%_0%,#22c55e22,transparent_35%)]"></div>
    <div class="relative flex items-start justify-between gap-3">
      <div>
        <p class="text-[11px] uppercase tracking-[0.2em] text-violet-200/70">Données</p>
        <h3 class="text-xl font-semibold text-white">Import / Export</h3>
      </div>
      <div class="hidden sm:flex items-center gap-2 text-xs text-slate-300/70">
        <span class="h-2 w-2 rounded-full bg-emerald-400 shadow-[0_0_10px_#34d39999]"></span>
        Prêt
      </div>
    </div>

    <!-- Actions -->
    <div class="relative grid grid-cols-1 sm:grid-cols-2 gap-3">
      <div
        v-if="importing"
        class="absolute inset-0 rounded-2xl border border-violet-400/20 bg-slate-950/70 backdrop-blur-sm flex flex-col gap-3 items-center justify-center z-10 text-white text-sm"
      >
        <div class="loading-spinner h-8 w-8 border-2 border-violet-200 border-t-transparent rounded-full"></div>
        <div class="text-center leading-tight">
          <div class="font-semibold">Import en cours...</div>
          <div class="text-xs text-slate-300">{{ progress }}%</div>
        </div>
        <div class="w-40 h-2 bg-slate-800 rounded-full overflow-hidden">
          <div
            class="h-full bg-gradient-to-r from-violet-500 via-blue-400 to-emerald-400 transition-all duration-200"
            :style="{ width: progress + '%' }"
          ></div>
        </div>
      </div>

      <!-- EXPORT -->
      <button
        type="button"
        class="w-full px-4 py-3 rounded-2xl bg-slate-800/70 hover:bg-slate-800 border border-white/10 text-white transition flex items-center justify-between gap-3 disabled:opacity-50 relative overflow-hidden"
        :disabled="!rowsToExport.length"
        @click="exportCsv"
      >
        <div class="flex items-center gap-3">
          <span
            class="inline-flex items-center justify-center h-10 w-10 rounded-2xl bg-white/5 border border-white/10 text-xs font-semibold"
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
      <div class="rounded-2xl border border-white/10 bg-slate-900/50 p-4 space-y-3">
        <div class="flex items-center justify-between gap-3">
          <div class="min-w-0">
            <div class="text-sm font-semibold text-white flex items-center gap-2">
              <span class="h-2 w-2 rounded-full bg-violet-400 shadow-[0_0_10px_#a78bfa]"></span>
              Importer un fichier
            </div>
            <div class="text-xs text-white/60 truncate flex items-center gap-1">
              {{ fileName ? fileName : 'Choisis un fichier CSV (export Excel)' }}
              <span v-if="selectedFile" class="text-[10px] text-violet-200/70">({{ prettySize }})</span>
            </div>
          </div>

          <div class="flex items-center gap-2">
            <button
              type="button"
              class="px-3 py-2 rounded-xl bg-white/5 hover:bg-white/10 border border-white/10 text-white text-sm transition"
              @click="pickFile"
              :disabled="importing"
            >
              Parcourir
            </button>

            <button
              type="button"
              class="px-3 py-2 rounded-xl bg-violet-600 hover:bg-violet-500 text-white text-sm disabled:opacity-50 transition inline-flex items-center gap-2"
              :disabled="importing || !selectedFile"
              @click="importNow"
            >
              <span v-if="importing" class="loading-spinner h-4 w-4 border-2 border-white/50 border-t-transparent rounded-full"></span>
              {{ importing ? 'Import...' : 'Importer' }}
            </button>
          </div>
        </div>

        <input
          ref="fileInput"
          type="file"
          class="hidden"
          accept=".csv,text/csv,application/vnd.ms-excel,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
          @change="onFilePicked"
        />

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
import { computed, ref } from 'vue'
import Papa from 'papaparse'
import * as XLSX from 'xlsx'
import SnkVenteServices from '@/services/SnkVenteServices.js'

/**
 * Exporte par defaut ce que tu passes dans filteredRows.
 */
const props = defineProps<{
  filteredRows: any[]
}>()

const emit = defineEmits<{
  (e: 'imported'): void
}>()

/* ------------------ EXPORT ------------------ */
const rowsToExport = computed(() => (Array.isArray(props.filteredRows) ? props.filteredRows : []))

function exportCsv() {
  const rows = rowsToExport.value
  if (!rows.length) return

  const safe = (v: any) => {
    const s = String(v ?? '')
    return `"${s.replaceAll('"', '""')}"`
  }

  const toIsoDate = (d: any) => {
    if (!d) return ''
    const date = new Date(d)
    if (Number.isNaN(date.getTime())) return ''
    return date.toISOString().slice(0, 10)
  }

  const headers = [
    'nomItem',
    'categorie',
    'prixRetail',
    'prixResell',
    'dateAchat',
    'dateVente',
    'description',
  ]

  const lines = [
    headers.join(';'),
    ...rows.map((v: any) => {
      const row = {
        nomItem: v.nomItem ?? v.nom_item ?? '',
        categorie: v.categorie ?? '',
        prixRetail: v.prixRetail ?? v.prix_retail ?? '',
        prixResell: v.prixResell ?? v.prix_resell ?? '',
        dateAchat: toIsoDate(v.dateAchat ?? v.date_achat),
        dateVente: toIsoDate(v.dateVente ?? v.date_vente),
        description: v.description ?? '',
      }

      return headers.map((h) => safe((row as any)[h])).join(';')
    }),
  ]

  const csvContent = '\uFEFF' + lines.join('\n') // BOM Excel FR
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
type CsvRow = Record<string, string>

const fileInput = ref<HTMLInputElement | null>(null)
const selectedFile = ref<File | null>(null)
const fileName = ref('')

const importing = ref(false)
const progress = ref(0)
const successMsg = ref('')
const errorMsg = ref('')
let progressTimer: number | null = null

const prettySize = computed(() => {
  const f = selectedFile.value
  if (!f) return ''
  if (f.size < 1024) return f.size + ' o'
  if (f.size < 1024 * 1024) return (f.size / 1024).toFixed(1) + ' ko'
  return (f.size / (1024 * 1024)).toFixed(2) + ' Mo'
})

function pickFile() {
  successMsg.value = ''
  errorMsg.value = ''
  fileInput.value?.click()
}

function onFilePicked(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0] ?? null
  selectedFile.value = file
  fileName.value = file?.name ?? ''
  successMsg.value = ''
  errorMsg.value = ''
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
  progress.value = finalValue
  setTimeout(() => {
    progress.value = 0
  }, 600)
}

function normalize(s: string) {
  return (s || '')
    .toString()
    .trim()
    .toLowerCase()
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/[^a-z0-9]+/g, ' ')
    .trim()
}

/** matching safe : exact puis startsWith */
function findHeader(headers: string[], synonyms: string[]) {
  const map = new Map<string, string>()
  for (const h of headers) map.set(normalize(h), h)

  for (const syn of synonyms) {
    const exact = map.get(normalize(syn))
    if (exact) return exact
  }

  for (const syn of synonyms) {
    const ns = normalize(syn)
    for (const [nh, orig] of map.entries()) {
      if (nh.startsWith(ns)) return orig
    }
  }

  return ''
}

function looksBadName(name: string) {
  const t = (name || '').trim()
  if (!t) return true
  if (t.length <= 1) return true
  if (t === '-' || t === '--') return true
  return false
}

function toNumberSmart(v: unknown) {
  if (v == null) return null
  let s = String(v).trim()
  if (!s) return null
  s = s.replace(/\s/g, '').replace(/[€$£]/g, '')
  if (s.includes(',') && !s.includes('.')) s = s.replace(',', '.')
  s = s.replace(/,(?=\d{3}\b)/g, '').replace(/(?<=\d)\.(?=\d{3}\b)/g, '')
  const n = Number(s)
  return Number.isFinite(n) ? n : null
}

function excelSerialToIso(value: number) {
  // Excel's day 1 = 1899-12-31, but there is an extra fake 1900 leap day; XLSX handles it via SSF
  const parsed = XLSX.SSF.parse_date_code(value)
  if (!parsed || !parsed.y || !parsed.m || !parsed.d) return null
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${parsed.y}-${pad(parsed.m)}-${pad(parsed.d)}`
}

function parseDateSmart(v: unknown) {
  if (v == null) return null

  // Excel numeric serials or numeric strings
  if (typeof v === 'number') {
    if (v > 30000 && v < 60000) {
      const iso = excelSerialToIso(v)
      if (iso) return iso
    }
  }

  const s = String(v).trim()
  if (!s) return null
  if (/^\d{4}-\d{2}-\d{2}/.test(s)) return s.slice(0, 10)

  // numeric string that looks like Excel serial
  if (/^\d{4,6}(\.\d+)?$/.test(s)) {
    const num = Number(s)
    const iso = excelSerialToIso(num)
    if (iso) return iso
  }

  const x = s.replace(/\./g, '/').replace(/-/g, '/')
  const parts = x.split('/')
  if (parts.length === 3) {
    const [a, b, c] = parts
    if (c.length === 4) {
      const day = Number(a)
      const month = Number(b)
      const year = Number(c)
      const d = new Date(year, month - 1, day)
      return Number.isNaN(d.getTime()) ? null : d.toISOString().slice(0, 10)
    }
  }

  const d = new Date(s)
  return Number.isNaN(d.getTime()) ? null : d.toISOString().slice(0, 10)
}

type ParsedTable = { headers: string[]; rows: CsvRow[] }

function isExcelFile(file: File) {
  return (
    file.type.includes('spreadsheet') ||
    /\.xlsx?$/i.test(file.name || '') ||
    file.type === 'application/vnd.ms-excel'
  )
}

function extractTableFrom2D(rows2D: any[][]): ParsedTable {
  // trouve la première ligne qui contient au moins 2 valeurs non vides pour servir d'en-têtes
  const headerIndex = rows2D.findIndex((row) => (row || []).filter((c) => String(c).trim()).length >= 2)
  if (headerIndex === -1) return { headers: [], rows: [] }

  const rawHeaders = rows2D[headerIndex] || []
  const headers = rawHeaders.map((h, idx) => {
    const s = String(h ?? '').trim()
    return s || `col${idx + 1}`
  })

  const dataRows = rows2D.slice(headerIndex + 1).map((r) => {
    const obj: CsvRow = {}
    headers.forEach((h, i) => {
      obj[h] = (r && r[i] !== undefined ? r[i] : '') as any
    })
    return obj
  })

  return { headers, rows: dataRows }
}

async function parseExcel(file: File): Promise<ParsedTable> {
  const buffer = await file.arrayBuffer()
  const workbook = XLSX.read(buffer, { type: 'array' })
  const sheetNameWithData =
    workbook.SheetNames.find((name) => {
      const rows = XLSX.utils.sheet_to_json<any[]>(workbook.Sheets[name], { header: 1, defval: '' }) as
        | any[][]
        | undefined
      if (!rows) return false
      return rows.some((row) => (row || []).some((c) => String(c).trim()))
    }) || workbook.SheetNames[0]

  const sheet = workbook.Sheets[sheetNameWithData]
  if (!sheet) return { headers: [], rows: [] }

  const rows2D = XLSX.utils.sheet_to_json<any[]>(sheet, { header: 1, defval: '' }) as any[][] | undefined
  return extractTableFrom2D(rows2D || [])
}

async function parseCsv(file: File): Promise<ParsedTable> {
  const attempt = (delimiter?: string) =>
    new Promise<{ headers: string[]; rows: CsvRow[] }>((resolve) => {
      Papa.parse<CsvRow>(file, {
        header: true,
        skipEmptyLines: 'greedy',
        delimiter,
        worker: true,
        complete: (res) => {
          resolve({
            headers: (res.meta.fields ?? []) as string[],
            rows: (res.data ?? []).filter(Boolean) as CsvRow[],
          })
        },
      })
    })

  const auto = await attempt()
  if (auto.headers.length >= 2) return auto

  const semicolon = await attempt(';')
  const best = semicolon.headers.length > auto.headers.length ? semicolon : auto

  // fallback : reparser sans header pour récupérer la première ligne non vide comme entête
  const fallback = await new Promise<ParsedTable>((resolve) => {
    Papa.parse<string[]>(file, {
      header: false,
      skipEmptyLines: 'greedy',
      delimiter: best.headers.length ? undefined : ';',
      complete: (res) => {
        const rows2D = res.data as any[][]
        resolve(extractTableFrom2D(rows2D || []))
      },
    })
  })

  return fallback.headers.length >= 2 ? fallback : best
}

async function parseFileSmart(file: File): Promise<ParsedTable> {
  const table = isExcelFile(file) ? await parseExcel(file) : await parseCsv(file)
  // si le header est vide (ex: 1re ligne vide ou colonnes fusionnées), on reprend les clés détectées
  if (table.headers.length < 2 && table.rows.length) {
    table.headers = Object.keys(table.rows[0] ?? {}).map((h) => h || '')
  }
  return table
}

function buildPayload(rows: CsvRow[], headers: string[]) {
  const colNomItem = findHeader(headers, [
    'nom item',
    "nom de l'item",
    'nom de l item',
    'nom',
    'item',
    'produit',
    'product',
    'name',
    'model',
    'modele',
  ])

  const colPrixRetail = findHeader(headers, [
    'prix retail',
    'PrixRetail',
    'prix achat',
    'achat',
    'retail',
    'buy',
    'purchase',
    'cost',
  ])

  const colPrixResell = findHeader(headers, [
    'prix resell',
    'prix revente',
    'prixResell',
    'revente',
    'resell',
    'sell',
    'sold',
    'sale',
  ])

  const colDateAchat = findHeader(headers, [
    'date achat',
    'date d achat',
    "date d'achat",
    'purchase date',
    'buy date',
    'acquired',
    'acquisition',
    'date',
  ])

  const colDateVente = findHeader(headers, [
    'date vente',
    'date de vente',
    'sold date',
    'sale date',
    'resell date',
  ])

  const colCategorie = findHeader(headers, [
    'categorie',
    'category',
    'brand',
    'marque',
  ])

  const colDescription = findHeader(headers, [
    'description',
    'desc',
    'notes',
    'commentaire',
    'comment',
  ])

  if (!colNomItem) {
    throw new Error(
      'Impossible de trouver la colonne du NOM (nom / item / produit / nom item). Renomme une colonne et re-essaie.',
    )
  }

  // eviter achat=vente si meme colonne
  const dateAchatCol = colDateAchat
  let dateVenteCol = colDateVente
  if (dateAchatCol && dateVenteCol && dateAchatCol === dateVenteCol) {
    dateVenteCol = ''
  }

  const payload = rows
    .map((r) => {
      const nomItem = String(r[colNomItem] ?? '').trim()
      if (looksBadName(nomItem)) return null

      return {
        nomItem,
        prixRetail: colPrixRetail ? toNumberSmart(r[colPrixRetail]) : null,
        prixResell: colPrixResell ? toNumberSmart(r[colPrixResell]) : null,
        dateAchat: dateAchatCol ? parseDateSmart(r[dateAchatCol]) : null,
        dateVente: dateVenteCol ? parseDateSmart(r[dateVenteCol]) : null,
        categorie: colCategorie ? String(r[colCategorie] ?? '').trim() : null,
        description: colDescription ? String(r[colDescription] ?? '').trim() : null,
      }
    })
    .filter(Boolean)

  if (!payload.length) {
    throw new Error('Aucune ligne importable : colonne NOM vide ou fichier mal parse.')
  }

  return payload
}

async function importNow() {
  const file = selectedFile.value
  if (!file) return

  importing.value = true
  startProgress()
  successMsg.value = ''
  errorMsg.value = ''

  try {
    const parsed = await parseFileSmart(file)
    const headers = parsed.headers
    const rows = parsed.rows

    if (headers.length < 2) {
      throw new Error('CSV illisible : trop peu de colonnes detectees (mauvais separateur).')
    }

    const payload = buildPayload(rows, headers)
    const res = await SnkVenteServices.importBulk(payload)

    const created = res?.data?.created ?? null
    if (created === 0) throw new Error('Import fait mais 0 ligne créee.')

    successMsg.value = `Import OK (${created ?? payload.length} lignes)`
    emit('imported')

    selectedFile.value = null
    fileName.value = ''
    stopProgress(100)
  } catch (err: any) {
    console.error(err)
    errorMsg.value =
      err?.response?.data?.message ||
      err?.response?.data?.error ||
      err?.message ||
      'Erreur import CSV'
    stopProgress(0)
  } finally {
    importing.value = false
    if (fileInput.value) fileInput.value.value = ''
  }
}
</script>

<style scoped>
.loading-spinner {
  animation: spin 0.9s linear infinite;
}
@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
