import { formatNumber } from './formatters'
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

function formatDiscordPrice(value) {
  if (value === null) return 'Prix sur demande'
  const digits = Number.isInteger(value) ? 0 : 2
  return `${formatNumber(value, { digits })} EUR`
}

function normalizeDiscordListing(item, index) {
  const name = readTextField(item, ['nomItem', 'name', 'nom']) || `Item ${index + 1}`
  const size = readTextField(item, ['size', 'taille', 'pointure'])
  const reference = readTextField(item, ['sku', 'reference', 'model'])
  const condition = readTextField(item, ['condition', 'etat', 'boxCondition'])
  const price = numberOrNull(getField(item, 'prixResell', null)) ?? numberOrNull(getField(item, 'prixRetail', null))
  const marketUrl = readTextField(item, ['marketUrl'])

  return {
    id: item?.id ?? `stock-${index}`,
    name,
    size,
    reference,
    condition,
    price,
    priceLabel: formatDiscordPrice(price),
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

export function buildDiscordStockListings(items = []) {
  return items
    .filter((item) => !isVendue(item))
    .map((item, index) => normalizeDiscordListing(item, index))
    .sort((a, b) => a.name.localeCompare(b.name, 'fr', { sensitivity: 'base' }))
}

export function buildDiscordStockMessage(items = []) {
  const listings = buildDiscordStockListings(items)

  if (!listings.length) {
    return [
      'Bonjour a tous,',
      '',
      "Le stock a vendre est en cours de mise a jour. Il n'y a aucun item disponible pour le moment.",
      '',
      'Merci et a tres vite.',
    ].join('\n')
  }

  return [
    'Bonjour a tous,',
    '',
    'Voici le stock actuellement disponible :',
    '',
    ...listings.map(formatDiscordLine),
    '',
    "Si quelque chose vous interesse, envoyez-moi un message prive.",
    'Je peux envoyer plus de photos et de details si besoin.',
  ].join('\n')
}
