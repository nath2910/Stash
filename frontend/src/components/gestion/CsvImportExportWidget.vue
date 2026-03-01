<template>
  <section class="rounded-2xl border border-white/10 bg-slate-900/60 p-4 sm:p-5 space-y-4">
    <!-- Header -->
    <div class="flex items-start justify-between gap-3">
      <div>
        <h3 class="text-lg font-semibold text-white">Export / Import CSV</h3>
        <p class="text-sm text-white/70">Reutilise ta base de donnee existante.</p>
      </div>
    </div>

    <!-- Actions -->
    <div class="grid grid-cols-1 sm:grid-cols-2 gap-3">
      <!-- EXPORT -->
      <button
        type="button"
        class="w-full px-4 py-3 rounded-2xl bg-gray-700/70 hover:bg-gray-700 border border-white/10 text-white transition flex items-center justify-center gap-2 disabled:opacity-50"
        :disabled="!rowsToExport.length"
        @click="exportCsv"
      >
        <span
          class="inline-flex items-center justify-center h-9 w-9 rounded-xl bg-white/5 border border-white/10"
        >
          Export
        </span>
        <div class="text-left">
          <div class="text-sm font-semibold">Exporter le CSV</div>
          <div class="text-xs text-white/60">{{ rowsToExport.length }} ligne(s)</div>
        </div>
      </button>

      <!-- IMPORT -->
      <div class="rounded-2xl border border-white/10 bg-white/0 p-3">
        <div class="flex items-center justify-between gap-3">
          <div class="min-w-0">
            <div class="text-sm font-semibold text-white">Importer un CSV</div>
            <div class="text-xs text-white/60 truncate">
              {{ fileName ? fileName : 'Choisis un fichier CSV (export Excel)' }}
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
              class="px-3 py-2 rounded-xl bg-violet-600 hover:bg-violet-500 text-white text-sm disabled:opacity-50 transition"
              :disabled="importing || !selectedFile"
              @click="importNow"
            >
              {{ importing ? 'Import...' : 'Importer' }}
            </button>
          </div>
        </div>

        <input
          ref="fileInput"
          type="file"
          class="hidden"
          accept=".csv,text/csv"
          @change="onFilePicked"
        />

        <div
          v-if="successMsg"
          class="mt-3 rounded-xl border border-emerald-400/20 bg-emerald-400/10 p-3 text-sm text-emerald-100"
        >
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
import Papa, { type ParseResult } from 'papaparse'
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
const successMsg = ref('')
const errorMsg = ref('')

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

function parseDateSmart(v: unknown) {
  if (v == null) return null
  const s = String(v).trim()
  if (!s) return null
  if (/^\d{4}-\d{2}-\d{2}/.test(s)) return s.slice(0, 10)

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

/** parse best delimiter : ; , tab, auto */
async function parseBest(file: File): Promise<ParseResult<CsvRow>> {
  const tries: Array<string | undefined> = [';', ',', '\t', undefined]

  const results = await Promise.all(
    tries.map(
      (delimiter) =>
        new Promise<ParseResult<CsvRow>>((resolve) => {
          Papa.parse<CsvRow>(file, {
            header: true,
            skipEmptyLines: 'greedy',
            delimiter: delimiter as any,
            complete: (res) => resolve(res),
          })
        }),
    ),
  )

  return results.sort((a, b) => (b.meta.fields?.length ?? 0) - (a.meta.fields?.length ?? 0))[0]
}

function buildPayload(rows: CsvRow[], headers: string[]) {
  const colNomItem = findHeader(headers, [
    'nom item',
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
  successMsg.value = ''
  errorMsg.value = ''

  try {
    const parsed = await parseBest(file)
    const headers = (parsed.meta.fields ?? []) as string[]
    const rows = (parsed.data ?? []).filter(Boolean)

    if (headers.length < 2) {
      throw new Error('CSV illisible : trop peu de colonnes detectees (mauvais separateur).')
    }

    const payload = buildPayload(rows, headers)
    const res = await SnkVenteServices.importBulk(payload)

    const created = res?.data?.created ?? null
    if (created === 0) throw new Error('Import fait mais 0 ligne creee.')

    successMsg.value = `Import OK (${created ?? payload.length} lignes)`
    emit('imported')

    selectedFile.value = null
    fileName.value = ''
  } catch (err: any) {
    console.error(err)
    errorMsg.value =
      err?.response?.data?.message ||
      err?.response?.data?.error ||
      err?.message ||
      'Erreur import CSV'
  } finally {
    importing.value = false
    if (fileInput.value) fileInput.value.value = ''
  }
}
</script>
