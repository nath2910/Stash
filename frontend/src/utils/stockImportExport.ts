import { METADATA_FIELDS, typeLabel } from '@/constants/itemTypes'

export type CsvRow = Record<string, any>
export type ImportMapping = Record<string, string>

export type ImportPayloadItem = {
  nomItem: string
  prixRetail: number | null
  prixResell: number | null
  dateAchat: string | null
  dateVente: string | null
  categorie: string | null
  description: string | null
  type: string | null
  metadata: Record<string, string> | null
}

export type ImportPreviewRow = {
  rowNumber: number
  status: 'valid' | 'invalid'
  name: string
  quantity: number
  errors: string[]
  warnings: string[]
  item: ImportPayloadItem | null
}

export type ImportPreview = {
  headers: string[]
  rowsDetected: number
  validRows: number
  invalidRows: number
  validItems: number
  duplicateRows: number
  unknownHeaders: string[]
  mapping: ImportMapping
  rows: ImportPreviewRow[]
  payload: ImportPayloadItem[]
}

export type ParsedTable = { headers: string[]; rows: CsvRow[] }

export const EXPORT_METADATA_COLUMNS = [
  { key: 'size', header: 'pointure' },
  { key: 'sku', header: 'sku' },
  { key: 'colorway', header: 'coloris' },
  { key: 'condition', header: 'etat' },
  { key: 'boxCondition', header: 'etat boite' },
  { key: 'set', header: 'set' },
  { key: 'language', header: 'langue' },
  { key: 'rarity', header: 'rarete' },
  { key: 'grade', header: 'grade' },
  { key: 'eventDate', header: 'date evenement' },
  { key: 'venue', header: 'lieu' },
  { key: 'section', header: 'section' },
  { key: 'row', header: 'rang' },
  { key: 'seat', header: 'siege' },
  { key: 'status', header: 'statut' },
]

export const EXPORT_HEADERS = [
  'nom item',
  'type',
  'type label',
  'categorie',
  'prix retail',
  'prix resell',
  'profit',
  'date achat',
  'date vente',
  'description',
  ...EXPORT_METADATA_COLUMNS.map((column) => column.header),
]

function quoteCsvCell(value: any) {
  const s = String(value ?? '')
  return `"${s.replaceAll('"', '""')}"`
}

function toIsoDate(value: any) {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return ''
  return date.toISOString().slice(0, 10)
}

function toCsvNumber(value: any) {
  if (value === null || value === undefined || value === '') return ''
  const num = Number(value)
  if (!Number.isFinite(num)) return ''
  return String(num).replace('.', ',')
}

export function buildStockExportCsv(rows: any[]) {
  const safeRows = Array.isArray(rows) ? rows : []
  const lines = [
    EXPORT_HEADERS.join(';'),
    ...safeRows.map((vente: any) => {
      const metadata = vente.metadata && typeof vente.metadata === 'object' ? vente.metadata : {}
      const prixRetail = vente.prixRetail ?? vente.prix_retail
      const prixResell = vente.prixResell ?? vente.prix_resell
      const type = vente.type ?? 'SNEAKER'
      const profit =
        Number.isFinite(Number(prixRetail)) && Number.isFinite(Number(prixResell))
          ? Number(prixResell) - Number(prixRetail)
          : ''

      const row = {
        'nom item': vente.nomItem ?? vente.nom_item ?? '',
        type,
        'type label': typeLabel(type),
        categorie: vente.categorie ?? '',
        'prix retail': toCsvNumber(prixRetail),
        'prix resell': toCsvNumber(prixResell),
        profit: toCsvNumber(profit),
        'date achat': toIsoDate(vente.dateAchat ?? vente.date_achat),
        'date vente': toIsoDate(vente.dateVente ?? vente.date_vente),
        description: vente.description ?? '',
        ...Object.fromEntries(
          EXPORT_METADATA_COLUMNS.map((column) => [column.header, metadata?.[column.key] ?? '']),
        ),
      }

      return EXPORT_HEADERS.map((header) => quoteCsvCell((row as any)[header])).join(';')
    }),
  ]

  return '\uFEFF' + lines.join('\r\n')
}

export function normalizeImportText(value: string) {
  return (value || '')
    .toString()
    .trim()
    .toLowerCase()
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/[^a-z0-9]+/g, ' ')
    .trim()
}

function compactNormalize(value: string) {
  return normalizeImportText(value).replace(/\s+/g, '')
}

const GENERIC_SINGLE_TOKEN_SYNONYMS = new Set([
  'achat',
  'buy',
  'cost',
  'date',
  'item',
  'model',
  'name',
  'nom',
  'purchase',
  'sale',
  'sell',
  'sold',
  'stock',
  'type',
  'vente',
])

export function scoreHeaderMatch(header: string, synonym: string) {
  const normalizedHeader = normalizeImportText(header)
  const normalizedSynonym = normalizeImportText(synonym)
  if (!normalizedHeader || !normalizedSynonym) return 0
  if (normalizedHeader === normalizedSynonym) return 1
  if (compactNormalize(header) === compactNormalize(synonym)) return 0.98

  const headerTokens = normalizedHeader.split(' ').filter(Boolean)
  const synonymTokens = normalizedSynonym.split(' ').filter(Boolean)
  const isGenericSingle =
    synonymTokens.length === 1 && GENERIC_SINGLE_TOKEN_SYNONYMS.has(synonymTokens[0])

  if (!isGenericSingle && normalizedHeader.startsWith(`${normalizedSynonym} `)) return 0.9
  if (!isGenericSingle && synonymTokens.length > 1) {
    const headerSet = new Set(headerTokens)
    if (synonymTokens.every((token) => headerSet.has(token))) return 0.86
  }
  if (!isGenericSingle && compactNormalize(header).includes(compactNormalize(synonym))) return 0.82

  return 0
}

export function findHeaderMatch(headers: string[], synonyms: string[]) {
  const candidates: Array<{ header: string; score: number }> = []

  for (const synonym of synonyms) {
    for (const header of headers) {
      const score = scoreHeaderMatch(header, synonym)
      if (score >= 0.82) candidates.push({ header, score })
    }
  }

  candidates.sort((a, b) => b.score - a.score)
  const best = candidates[0]
  if (!best) return { header: '', score: 0, ambiguous: false }

  const competing = candidates.find((candidate) => candidate.header !== best.header)
  const ambiguous = Boolean(competing && best.score - competing.score < 0.04)
  return { header: ambiguous ? '' : best.header, score: best.score, ambiguous }
}

function findHeader(headers: string[], synonyms: string[]) {
  return findHeaderMatch(headers, synonyms).header
}

function looksBadName(name: string) {
  const trimmed = (name || '').trim()
  if (!trimmed) return true
  if (trimmed.length <= 1) return true
  if (trimmed === '-' || trimmed === '--') return true
  return false
}

export function toNumberSmart(value: unknown) {
  if (value == null) return null
  let text = String(value).trim()
  if (!text) return null
  text = text.replace(/\s/g, '').replace(/[^\d,.\-+]/g, '')
  if (text.includes(',') && !text.includes('.')) text = text.replace(',', '.')
  text = text.replace(/,(?=\d{3}\b)/g, '').replace(/(?<=\d)\.(?=\d{3}\b)/g, '')
  const number = Number(text)
  return Number.isFinite(number) ? number : null
}

function excelSerialToIso(value: number) {
  if (!Number.isFinite(value)) return null
  const date = new Date(Math.round((value - 25569) * 86400 * 1000))
  if (Number.isNaN(date.getTime())) return null
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${date.getUTCFullYear()}-${pad(date.getUTCMonth() + 1)}-${pad(date.getUTCDate())}`
}

function formatLocalIsoDate(date: Date) {
  if (Number.isNaN(date.getTime())) return null
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}

export function parseDateSmart(value: unknown) {
  if (value == null) return null

  if (typeof value === 'number' && value > 30000 && value < 60000) {
    const iso = excelSerialToIso(value)
    if (iso) return iso
  }

  const text = String(value).trim()
  if (!text) return null
  if (/^\d{4}-\d{2}-\d{2}/.test(text)) return text.slice(0, 10)

  if (/^\d{4,6}(\.\d+)?$/.test(text)) {
    const iso = excelSerialToIso(Number(text))
    if (iso) return iso
  }

  const parts = text.replace(/\./g, '/').replace(/-/g, '/').split('/')
  if (parts.length === 3) {
    const [dayPart, monthPart, yearPart] = parts
    if (yearPart.length === 4) {
      const day = Number(dayPart)
      const month = Number(monthPart)
      const year = Number(yearPart)
      const date = new Date(year, month - 1, day)
      return date.getFullYear() === year && date.getMonth() === month - 1 && date.getDate() === day
        ? formatLocalIsoDate(date)
        : null
    }
  }

  return formatLocalIsoDate(new Date(text))
}

function parseMetadata(value: unknown) {
  if (value == null) return null
  if (typeof value === 'object') return value as Record<string, unknown>
  const text = String(value).trim()
  if (!text) return null
  try {
    const parsed = JSON.parse(text)
    if (parsed && typeof parsed === 'object') return parsed as Record<string, unknown>
  } catch {
    // invalid metadata cells are ignored so the rest of the row can still be imported
  }
  return null
}

function buildMetadataFromColumns(row: CsvRow, headers: string[], typeValue: string | null) {
  const metadata: Record<string, string> = {}
  const normalizedType = String(typeValue || '').trim().toUpperCase()
  const metadataFields = [
    ...EXPORT_METADATA_COLUMNS,
    ...Object.values(METADATA_FIELDS).flat().map((field: any) => ({
      key: field.key,
      header: field.label,
    })),
  ]

  const allowedKeys =
    normalizedType && METADATA_FIELDS[normalizedType as keyof typeof METADATA_FIELDS]
      ? new Set(
          METADATA_FIELDS[normalizedType as keyof typeof METADATA_FIELDS].map((field: any) => field.key),
        )
      : null

  for (const field of metadataFields) {
    if (allowedKeys && !allowedKeys.has(field.key)) continue
    const header = findHeader(headers, [field.header, field.key])
    if (!header) continue
    const value = String(row[header] ?? '').trim()
    if (!value) continue
    metadata[field.key] = value
  }

  return Object.keys(metadata).length ? metadata : null
}

export function isExcelFile(file: File) {
  return (
    file.type.includes('spreadsheet') ||
    /\.xlsx?$/i.test(file.name || '') ||
    file.type === 'application/vnd.ms-excel'
  )
}

export function isJsonFile(file: File) {
  return file.type === 'application/json' || /\.json$/i.test(file.name || '')
}

export function extractTableFrom2D(rows2D: any[][]): ParsedTable {
  const headerIndex = rows2D.findIndex((row) => (row || []).filter((cell) => String(cell).trim()).length >= 2)
  if (headerIndex === -1) return { headers: [], rows: [] }

  const rawHeaders = rows2D[headerIndex] || []
  const headers = rawHeaders.map((header, index) => {
    const text = String(header ?? '').trim()
    return text || `col${index + 1}`
  })

  const rows = rows2D.slice(headerIndex + 1).map((sourceRow) => {
    const row: CsvRow = {}
    headers.forEach((header, index) => {
      row[header] = sourceRow && sourceRow[index] !== undefined ? sourceRow[index] : ''
    })
    return row
  })

  return { headers, rows }
}

export function tableFromObjectRows(items: any[]): ParsedTable {
  const objectRows = items.filter((item) => item && typeof item === 'object' && !Array.isArray(item))
  if (!objectRows.length) return { headers: [], rows: [] }

  const headers = Array.from(
    objectRows.reduce((set, item) => {
      Object.keys(item).forEach((key) => set.add(key))
      return set
    }, new Set<string>()),
  )

  const rows = objectRows.map((item) => {
    const row: CsvRow = {}
    headers.forEach((header) => {
      row[header] = item?.[header] ?? ''
    })
    return row
  })

  return { headers, rows }
}

export function findJsonArray(value: any): any[] | null {
  if (Array.isArray(value)) return value
  if (!value || typeof value !== 'object') return null
  for (const key of ['items', 'rows', 'data', 'stock', 'inventory', 'products']) {
    if (Array.isArray(value[key])) return value[key]
  }
  return null
}

export function parseJsonContent(text: string): ParsedTable {
  const parsed = JSON.parse(text)
  const array = findJsonArray(parsed)
  if (!array) return { headers: [], rows: [] }
  if (array.every((row) => Array.isArray(row))) return extractTableFrom2D(array as any[][])
  return tableFromObjectRows(array)
}

export function detectDelimiter(text: string) {
  const candidates = [',', ';', '\t', '|']
  const lines = text
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter(Boolean)
    .slice(0, 12)

  let best = { delimiter: '', score: 0 }
  for (const delimiter of candidates) {
    const counts = lines.map((line) => line.split(delimiter).length).filter((count) => count > 1)
    if (!counts.length) continue
    const consistency = counts.filter((count) => count === counts[0]).length / counts.length
    const score = Math.max(...counts) * consistency + counts.length / 10
    if (score > best.score) best = { delimiter, score }
  }

  return best.delimiter
}

export function parseLooseKeyValueText(text: string): ParsedTable {
  const groups = text
    .split(/\n\s*\n/g)
    .map((group) => group.trim())
    .filter(Boolean)

  const rows = groups
    .map((group) => {
      const row: CsvRow = {}
      group.split(/\r?\n/).forEach((line) => {
        const match = line.match(/^\s*([^:=-]{2,60})\s*[:=-]\s*(.+?)\s*$/)
        if (!match) return
        row[match[1].trim()] = match[2].trim()
      })
      return row
    })
    .filter((row) => Object.keys(row).length >= 2)

  if (!rows.length) return { headers: [], rows: [] }
  return tableFromObjectRows(rows)
}

export function resolveImportMapping(headers: string[]) {
  const name = findHeader(headers, [
    'nom item',
    "nom de l'item",
    'nom de l item',
    'nom',
    'item',
    'produit',
    'product name',
    'product',
    'name',
    'nom produit',
    'model',
    'modele',
    'modele sneaker',
    'sneaker',
    'sneakers',
  ])
  const brand = findHeader(headers, ['marque', 'brand', 'fabricant', 'manufacturer', 'maker', 'categorie', 'category'])
  const retail = findHeader(headers, [
    'prix retail',
    'PrixRetail',
    'prix achat',
    "prix d'achat",
    'prix d achat',
    'prix fournisseur',
    'purchase price',
    'buy price',
    'cost price',
    'achat',
    'retail',
    'buy',
    'purchase',
    'cost',
  ])
  const resell = findHeader(headers, [
    'prix resell',
    'prix revente',
    'prix vente',
    'prix de vente',
    'prix estime',
    'prix estime vente',
    'prix vente estime',
    'prix de vente estime',
    'prix vente reel',
    'prix de vente reel',
    'prixResell',
    'prix revente estime',
    'prix revente reel',
    'revente',
    'resale price',
    'resell',
    'sell price',
    'sell',
    'sold',
    'sold price',
    'sale',
    'sale price',
    'estimated sale price',
    'actual sale price',
  ])
  const dateAchat = findHeader(headers, [
    'date achat',
    'date d achat',
    "date d'achat",
    'purchase date',
    'bought at',
    'buy date',
    'acquired',
    'acquisition',
    'date',
  ])
  const dateVente = findHeader(headers, ['date vente', 'date de vente', 'sold date', 'sale date', 'resell date', 'sold at'])
  const mapping = {
    name,
    brand,
    retail,
    resell,
    dateAchat,
    dateVente: dateAchat && dateVente === dateAchat ? '' : dateVente,
    notes: findHeader(headers, ['description', 'desc', 'notes', 'note', 'commentaire', 'comment']),
    type: findHeader(headers, ['type', 'type item', "type d'item"]),
    metadata: findHeader(headers, ['metadata', 'meta']),
    size: findHeader(headers, ['pointure', 'taille', 'size', 'eu size', 'us size', 'uk size']),
    sku: findHeader(headers, ['sku', 'reference', 'reference produit', 'product id', 'id produit', 'ref', 'style code', 'code']),
    condition: findHeader(headers, ['etat', 'condition', 'state', 'status', 'statut']),
    colorway: findHeader(headers, ['coloris', 'colorway', 'couleur']),
    boxCondition: findHeader(headers, ['etat boite', 'box condition', 'boite']),
    quantity: findHeader(headers, ['quantite', 'quantite stock', 'qty', 'quantity', 'stock', 'nombre']),
    supplier: findHeader(headers, ['fournisseur', 'supplier', 'source', 'vendeur', 'seller']),
  }

  return dedupeMapping(mapping, [
    'name',
    'brand',
    'size',
    'retail',
    'resell',
    'quantity',
    'condition',
    'sku',
    'dateAchat',
    'dateVente',
    'supplier',
    'notes',
    'type',
    'colorway',
    'boxCondition',
    'metadata',
  ])
}

function dedupeMapping(mapping: ImportMapping, priority: string[]) {
  const used = new Set<string>()
  const deduped: ImportMapping = { ...mapping }
  priority.forEach((key) => {
    const value = deduped[key]
    if (!value) return
    if (used.has(value)) {
      deduped[key] = ''
      return
    }
    used.add(value)
  })
  return deduped
}

export function normalizeMapping(headers: string[], mapping: ImportMapping) {
  const allowed = new Set(headers)
  return Object.fromEntries(
    Object.entries(mapping).map(([key, value]) => [key, allowed.has(value) ? value : '']),
  ) as ImportMapping
}

export function recognizedImportHeaders(headers: string[], mapping: ImportMapping) {
  const recognized = new Set(Object.values(mapping).filter(Boolean))
  const metadataFields = [
    ...EXPORT_METADATA_COLUMNS,
    ...Object.values(METADATA_FIELDS).flat().map((field: any) => ({
      key: field.key,
      header: field.label,
    })),
  ]

  metadataFields.forEach((field) => {
    const header = findHeader(headers, [field.header, field.key])
    if (header) recognized.add(header)
  })

  return recognized
}

function isEmptyImportRow(row: CsvRow, headers: string[]) {
  return !headers.some((header) => String(row?.[header] ?? '').trim())
}

function normalizeItemType(value: unknown) {
  const raw = String(value ?? '').trim().toUpperCase()
  if (!raw) return null
  const cleaned = raw
    .replace(/[\s-]+/g, '_')
    .replace('POKEMON', 'POKEMON_CARD')
    .replace('CARTE_POKEMON', 'POKEMON_CARD')
  return ['SNEAKER', 'POKEMON_CARD', 'TICKET', 'OTHER'].includes(cleaned) ? cleaned : null
}

function readCell(row: CsvRow, column?: string) {
  if (!column) return ''
  return String(row[column] ?? '').trim()
}

function parseOptionalNumber(row: CsvRow, column: string, label: string, errors: string[]) {
  const raw = readCell(row, column)
  if (!raw) return null
  const value = toNumberSmart(raw)
  if (value === null || value < 0) {
    errors.push(`${label} invalide`)
    return null
  }
  return value
}

function parseOptionalDate(row: CsvRow, column: string, label: string, errors: string[]) {
  const raw = readCell(row, column)
  if (!raw) return null
  const value = parseDateSmart(raw)
  if (!value) {
    errors.push(`${label} invalide`)
    return null
  }
  return value
}

function compactDescription(parts: Array<string | null>) {
  const value = parts.map((part) => String(part ?? '').trim()).filter(Boolean).join(' | ')
  if (!value) return null
  return value.length > 500 ? `${value.slice(0, 497)}...` : value
}

function createDuplicateKey(item: ImportPayloadItem) {
  const name = normalizeImportText(item.nomItem)
  const sku = normalizeImportText(String(item.metadata?.sku ?? ''))
  const size = normalizeImportText(String(item.metadata?.size ?? ''))
  if (sku || size) return `${name}|sku:${sku}|size:${size}`
  return `${name}|date:${item.dateAchat ?? ''}|retail:${item.prixRetail ?? ''}`
}

function existingDuplicateKeys(rows: any[] = []) {
  const keys = new Set<string>()
  const sourceRows = Array.isArray(rows) ? rows : []
  sourceRows.forEach((row) => {
    const metadata = row?.metadata && typeof row.metadata === 'object' ? row.metadata : {}
    const item: ImportPayloadItem = {
      nomItem: String(row?.nomItem ?? row?.nom_item ?? '').trim(),
      prixRetail: toNumberSmart(row?.prixRetail ?? row?.prix_retail),
      prixResell: toNumberSmart(row?.prixResell ?? row?.prix_resell),
      dateAchat: parseDateSmart(row?.dateAchat ?? row?.date_achat),
      dateVente: parseDateSmart(row?.dateVente ?? row?.date_vente),
      categorie: row?.categorie ?? null,
      description: row?.description ?? null,
      type: row?.type ?? null,
      metadata,
    }
    if (item.nomItem) keys.add(createDuplicateKey(item))
  })
  return keys
}

function buildPreviewItem(row: CsvRow, headers: string[], mapping: ImportMapping) {
  const errors: string[] = []
  const warnings: string[] = []
  const nomItem = readCell(row, mapping.name)
  if (looksBadName(nomItem)) errors.push('Modele / nom manquant')
  if (nomItem.length > 200) errors.push('Modele trop long (max 200 caracteres)')

  const quantityRaw = readCell(row, mapping.quantity)
  const quantityParsed = quantityRaw ? toNumberSmart(quantityRaw) : 1
  const quantity = Number.isFinite(Number(quantityParsed)) ? Math.trunc(Number(quantityParsed)) : 0
  if (quantityRaw && quantityParsed !== null && !Number.isInteger(Number(quantityParsed))) {
    errors.push('Quantite doit etre un nombre entier')
  }
  if (quantity < 1) errors.push('Quantite invalide')
  if (quantity > 50) errors.push('Quantite trop elevee par ligne (max 50)')

  const typeValue = normalizeItemType(readCell(row, mapping.type)) ?? 'SNEAKER'
  const metadataFromJson =
    (mapping.metadata ? parseMetadata(row[mapping.metadata]) : null) ||
    buildMetadataFromColumns(row, headers, typeValue)
  const metadata = {
    ...(metadataFromJson ?? {}),
    ...(readCell(row, mapping.size) ? { size: readCell(row, mapping.size) } : {}),
    ...(readCell(row, mapping.sku) ? { sku: readCell(row, mapping.sku) } : {}),
    ...(readCell(row, mapping.condition) ? { condition: readCell(row, mapping.condition) } : {}),
    ...(readCell(row, mapping.colorway) ? { colorway: readCell(row, mapping.colorway) } : {}),
    ...(readCell(row, mapping.boxCondition) ? { boxCondition: readCell(row, mapping.boxCondition) } : {}),
  } as Record<string, string>
  if (typeValue === 'SNEAKER' && mapping.size && !metadata.size) {
    errors.push('Taille manquante')
  }

  const categoryRaw = readCell(row, mapping.brand)
  const categorie = categoryRaw ? categoryRaw.slice(0, 60) : null
  if (categoryRaw.length > 60) warnings.push('Marque tronquee a 60 caracteres')

  const notes = readCell(row, mapping.notes)
  const supplier = readCell(row, mapping.supplier)
  const description = compactDescription([notes, supplier ? `Fournisseur: ${supplier}` : null])
  if ((notes || supplier) && description && description.endsWith('...')) {
    warnings.push('Notes tronquees a 500 caracteres')
  }

  const dateAchat = parseOptionalDate(row, mapping.dateAchat, "Date d'achat", errors)
  const dateVente = parseOptionalDate(row, mapping.dateVente, 'Date de vente', errors)
  if (dateAchat && dateVente && dateVente < dateAchat) {
    warnings.push('Date de vente anterieure a la date achat')
  }

  const item: ImportPayloadItem = {
    nomItem,
    prixRetail: parseOptionalNumber(row, mapping.retail, "Prix d'achat", errors),
    prixResell: parseOptionalNumber(row, mapping.resell, 'Prix de vente', errors),
    dateAchat,
    dateVente,
    categorie,
    description,
    type: typeValue,
    metadata: Object.keys(metadata).length ? metadata : null,
  }

  return { item, quantity, errors, warnings }
}

export function analyzeImportRows(
  rows: CsvRow[],
  headers: string[],
  mappingOverride?: ImportMapping,
  existingRows: any[] = [],
): ImportPreview {
  const mapping = normalizeMapping(headers, mappingOverride ?? resolveImportMapping(headers))
  const recognizedHeaders = recognizedImportHeaders(headers, mapping)
  const unknownHeaders = headers.filter((header) => String(header).trim() && !recognizedHeaders.has(header))
  const payload: ImportPayloadItem[] = []
  const previewRows: ImportPreviewRow[] = []
  const existingKeys = existingDuplicateKeys(existingRows)
  const seenKeys = new Set<string>()
  let duplicateRows = 0
  const rowsWithNumbers = rows
    .map((row, index) => ({ row, rowNumber: index + 2 }))
    .filter(({ row }) => !isEmptyImportRow(row, headers))

  if (!mapping.name) {
    return {
      headers,
      rowsDetected: rowsWithNumbers.length,
      validRows: 0,
      invalidRows: rowsWithNumbers.length,
      validItems: 0,
      duplicateRows: 0,
      unknownHeaders,
      mapping,
      rows: rowsWithNumbers.slice(0, 12).map(({ rowNumber }) => ({
        rowNumber,
        status: 'invalid',
        name: '',
        quantity: 0,
        errors: ['Colonne modele / nom introuvable'],
        warnings: [],
        item: null,
      })),
      payload,
    }
  }

  rowsWithNumbers.forEach(({ row, rowNumber }) => {
    const { item, quantity, errors, warnings } = buildPreviewItem(row, headers, mapping)
    const key = item.nomItem ? createDuplicateKey(item) : ''
    if (key && (seenKeys.has(key) || existingKeys.has(key))) {
      errors.push('Doublon detecte')
      duplicateRows += 1
    }

    if (!errors.length && payload.length + quantity > 500) {
      errors.push('Import trop volumineux (max 500 items)')
    }

    const status = errors.length ? 'invalid' : 'valid'
    if (status === 'valid') {
      seenKeys.add(key)
      for (let index = 0; index < quantity; index += 1) {
        payload.push({ ...item, metadata: item.metadata ? { ...item.metadata } : null })
      }
    }

    previewRows.push({
      rowNumber,
      status,
      name: item.nomItem,
      quantity,
      errors,
      warnings,
      item: status === 'valid' ? item : null,
    })
  })

  const validRows = previewRows.filter((row) => row.status === 'valid').length
  return {
    headers,
    rowsDetected: rowsWithNumbers.length,
    validRows,
    invalidRows: previewRows.length - validRows,
    validItems: payload.length,
    duplicateRows,
    unknownHeaders,
    mapping,
    rows: previewRows,
    payload,
  }
}
