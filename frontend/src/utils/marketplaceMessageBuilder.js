import { getField, isVendue } from './snkVente'

function readMetadataValue(item, key) {
  if (!item?.metadata || !key) return ''
  if (item.metadata[key] != null) return String(item.metadata[key]).trim()

  const snake = String(key).replace(/[A-Z]/g, (match) => `_${match.toLowerCase()}`)
  if (item.metadata[snake] != null) return String(item.metadata[snake]).trim()

  return ''
}

function readTextField(item, keys = []) {
  for (const key of keys) {
    const directValue = getField(item, key, '')
    if (String(directValue || '').trim()) return String(directValue).trim()

    const metadataValue = readMetadataValue(item, key)
    if (metadataValue) return metadataValue
  }
  return ''
}

function numberOrNull(value) {
  if (value === null || value === undefined || value === '') return null
  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed : null
}

function clamp(value, min, max) {
  return Math.min(max, Math.max(min, value))
}

function formatPlainNumber(value, digits = 0) {
  const parsed = Number(value)
  if (!Number.isFinite(parsed)) return '0'
  const fixed = parsed.toFixed(digits)
  const [integerPart, decimalPart] = fixed.split('.')
  const sign = integerPart.startsWith('-') ? '-' : ''
  const absoluteInteger = sign ? integerPart.slice(1) : integerPart
  const groupedInteger = absoluteInteger.replace(/\B(?=(\d{3})+(?!\d))/g, ' ')
  if (!decimalPart) return `${sign}${groupedInteger}`
  const normalizedDecimal = decimalPart.replace(/0+$/, '')
  return normalizedDecimal ? `${sign}${groupedInteger}.${normalizedDecimal}` : `${sign}${groupedInteger}`
}

function formatDiscordPrice(value) {
  if (value === null) return 'Prix sur demande'
  const digits = Number.isInteger(value) ? 0 : 2
  return `${formatPlainNumber(value, digits)} EUR`
}

function resolveTargetProfitPercent(options = {}) {
  const raw = numberOrNull(options?.targetProfitPercent)
  if (raw === null) return null
  return clamp(raw, 0, 200)
}

function resolveDiscordPrice(item, options = {}) {
  const retailPrice = numberOrNull(getField(item, 'prixRetail', null))
  const resellPrice = numberOrNull(getField(item, 'prixResell', null))
  const targetProfitPercent = resolveTargetProfitPercent(options)

  if (targetProfitPercent !== null && retailPrice !== null && retailPrice > 0) {
    return Math.max(0, Math.round(retailPrice * (1 + targetProfitPercent / 100)))
  }

  return resellPrice ?? retailPrice
}

function normalizeDiscordListing(item, index, options = {}) {
  const name = readTextField(item, ['nomItem', 'name', 'nom']) || `Item ${index + 1}`
  const size = readTextField(item, ['size', 'taille', 'pointure'])
  const reference = readTextField(item, ['sku', 'reference', 'model'])
  const condition = readTextField(item, ['condition', 'etat', 'boxCondition'])
  const retailPrice = numberOrNull(getField(item, 'prixRetail', null))
  const price = resolveDiscordPrice(item, options)
  const marketUrl = readTextField(item, ['marketUrl'])

  return {
    id: item?.id ?? `stock-${index}`,
    name,
    size,
    reference,
    condition,
    price,
    priceLabel: formatDiscordPrice(price),
    retailPrice,
    estimatedProfit: price !== null && retailPrice !== null ? price - retailPrice : null,
    marketUrl,
  }
}

function formatDiscordLine(listing) {
  const details = []
  if (listing.size) details.push(`Taille ${listing.size}`)
  if (listing.condition) details.push(`Etat ${listing.condition}`)
  details.push(`Prix ${listing.priceLabel}`)

  return `- ${listing.name}${details.length ? ` | ${details.join(' | ')}` : ''}`
}

export function estimateDiscordProfitPercent(items = []) {
  const rows = (Array.isArray(items) ? items : [])
    .filter((item) => !isVendue(item))
    .map((item) => {
      const retailPrice = numberOrNull(getField(item, 'prixRetail', null))
      const resellPrice = numberOrNull(getField(item, 'prixResell', null))
      if (retailPrice === null || retailPrice <= 0 || resellPrice === null || resellPrice <= 0) return null
      return ((resellPrice - retailPrice) / retailPrice) * 100
    })
    .filter((value) => value !== null)

  if (!rows.length) return 20

  const average = rows.reduce((sum, value) => sum + value, 0) / rows.length
  return clamp(Math.round(average / 5) * 5, 0, 200)
}

export function buildDiscordStockListings(items = [], options = {}) {
  return items
    .filter((item) => !isVendue(item))
    .map((item, index) => normalizeDiscordListing(item, index, options))
    .sort((a, b) => a.name.localeCompare(b.name, 'fr', { sensitivity: 'base' }))
}

export function buildDiscordStockMessage(items = [], options = {}) {
  const listings = buildDiscordStockListings(items, options)

  if (!listings.length) {
    return [
      'WTS :',
      '',
      "Le stock a vendre est en cours de mise a jour. Il n'y a aucun item disponible pour le moment.",
    ].join('\n')
  }

  return [
    'WTS :',
    '',
    ...listings.map(formatDiscordLine),
    '',
    'Si quelque chose vous interesse, envoyez-moi un message prive.',
    'Je peux envoyer plus de photos et de details si besoin.',
  ].join('\n')
}

function pushChunk(chunks, chunk) {
  const normalized = String(chunk || '').trim()
  if (normalized) chunks.push(normalized)
}

function splitLongDiscordLine(line, maxLength) {
  const text = String(line || '').trim()
  if (!text) return []
  if (text.length <= maxLength) return [text]

  const parts = []
  let remaining = text

  while (remaining.length > maxLength) {
    let splitAt = remaining.lastIndexOf(' ', maxLength)
    if (splitAt <= 0) splitAt = maxLength
    parts.push(remaining.slice(0, splitAt).trim())
    remaining = remaining.slice(splitAt).trim()
  }

  if (remaining) parts.push(remaining)
  return parts
}

export function splitDiscordMessageForClipboard(message = '', maxLength = 1900) {
  const normalizedLimit = Math.max(200, Number(maxLength) || 1900)
  const lines = String(message || '').replace(/\r\n/g, '\n').split('\n')
  const chunks = []
  let currentChunk = ''

  const appendLine = (line) => {
    const nextChunk = currentChunk ? `${currentChunk}\n${line}` : line
    if (nextChunk.length <= normalizedLimit) {
      currentChunk = nextChunk
      return
    }

    pushChunk(chunks, currentChunk)
    currentChunk = ''

    if (line.length <= normalizedLimit) {
      currentChunk = line
      return
    }

    const lineParts = splitLongDiscordLine(line, normalizedLimit)
    const lastPart = lineParts.pop() || ''
    lineParts.forEach((part) => pushChunk(chunks, part))
    currentChunk = lastPart
  }

  for (const line of lines) {
    if (!line.trim()) {
      if (currentChunk) {
        appendLine('')
      }
      continue
    }
    appendLine(line)
  }

  pushChunk(chunks, currentChunk)
  return chunks.length ? chunks : ['']
}
