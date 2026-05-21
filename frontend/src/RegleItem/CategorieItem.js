export const DEFAULT_ITEM_TYPES = [
  { value: 'SNEAKER', label: 'Sneakers' },
  { value: 'POKEMON_CARD', label: 'Pokemon' },
  { value: 'TICKET', label: 'Tickets' },
  { value: 'OTHER', label: 'Autre' },
]

export const ITEM_TYPES = DEFAULT_ITEM_TYPES

export const ITEM_TYPE_KEYS = DEFAULT_ITEM_TYPES.map((item) => item.value)

export const DEFAULT_ITEM_TYPE_LABELS = Object.fromEntries(
  DEFAULT_ITEM_TYPES.map((item) => [item.value, item.label]),
)

const ITEM_TYPE_ALIASES = {
  SNEAKERS: 'SNEAKER',
  SHOE: 'SNEAKER',
  SHOES: 'SNEAKER',
  CHAUSSURE: 'SNEAKER',
  CHAUSSURES: 'SNEAKER',
  POKEMON: 'POKEMON_CARD',
  POKEMON_CARDS: 'POKEMON_CARD',
  CARTE_POKEMON: 'POKEMON_CARD',
  CARTES_POKEMON: 'POKEMON_CARD',
  TICKETS: 'TICKET',
  AUTRE: 'OTHER',
  AUTRES: 'OTHER',
}

export function normalizeItemTypeValue(value, fallback = 'OTHER') {
  const raw = String(value ?? '').trim()
  if (!raw) return fallback
  const normalized = raw
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .toUpperCase()
    .replace(/[^A-Z0-9]+/g, '_')
    .replace(/^_+|_+$/g, '')
    .slice(0, 80)
    .replace(/_+$/g, '')
  if (!normalized) return fallback
  return ITEM_TYPE_ALIASES[normalized] || normalized
}

export function formatItemTypeLabel(type) {
  const normalized = normalizeItemTypeValue(type, '')
  if (!normalized) return 'Autre'
  const known = DEFAULT_ITEM_TYPE_LABELS[normalized]
  if (known) return known
  return normalized
    .toLowerCase()
    .split('_')
    .filter(Boolean)
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(' ')
}

export const METADATA_FIELDS = {
  SNEAKER: [
    { key: 'size', label: 'Pointure', placeholder: '42 EU' },
    { key: 'sku', label: 'SKU', placeholder: 'DZ5485-612' },
    { key: 'colorway', label: 'Coloris', placeholder: 'University Red/White' },
    { key: 'condition', label: 'Etat', placeholder: 'DS / Used' },
    { key: 'boxCondition', label: 'Etat boite', placeholder: 'OG all / Damagee' },
  ],
  POKEMON_CARD: [
    { key: 'set', label: 'Set', placeholder: '151 / Base Set' },
    { key: 'language', label: 'Langue', placeholder: 'FR / EN / JP' },
    { key: 'rarity', label: 'Rarete', placeholder: 'Secret Rare' },
    { key: 'condition', label: 'Condition', placeholder: 'NEUF / TRES BON ETAT / BON ETAT' },
  ],
  TICKET: [
    { key: 'eventDate', label: "Date de l'evenement", placeholder: '2026-05-01' },
    { key: 'venue', label: 'Salle / Lieu', placeholder: 'Accor Arena' },
    { key: 'section', label: 'Section', placeholder: 'Bloc K' },
    { key: 'row', label: 'Rang', placeholder: 'Rang 8' },
    { key: 'seat', label: 'Siege', placeholder: 'Siege 12' },
    { key: 'status', label: 'Statut', placeholder: 'valide / transfere / utilise' },
  ],
  OTHER: [],
}

export const typeLabel = (type) => formatItemTypeLabel(type).toUpperCase()
