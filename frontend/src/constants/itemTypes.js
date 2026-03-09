export const ITEM_TYPES = [
  { value: 'SNEAKER', label: 'Sneaker' },
  { value: 'POKEMON_CARD', label: 'Carte Pokémon' },
  { value: 'TICKET', label: 'Ticket / Billet' },
  { value: 'OTHER', label: 'Autre' },
]

export const METADATA_FIELDS = {
  SNEAKER: [
    { key: 'size', label: 'Pointure', placeholder: '42 EU' },
    { key: 'sku', label: 'SKU', placeholder: 'DZ5485-612' },
    { key: 'colorway', label: 'Coloris', placeholder: 'University Red/White' },
    { key: 'condition', label: 'État', placeholder: 'DS / VNDS / Used' },
    { key: 'boxCondition', label: 'État boîte', placeholder: 'OG all / Damagée' },
  ],
  POKEMON_CARD: [
    { key: 'set', label: 'Set', placeholder: '151 / Base Set' },
    { key: 'language', label: 'Langue', placeholder: 'FR / EN / JP' },
    { key: 'rarity', label: 'Rareté', placeholder: 'Secret Rare' },
    { key: 'condition', label: 'Condition', placeholder: 'NM / LP / HP' },
    { key: 'grade', label: 'Grade', placeholder: 'PSA 9 / BGS 9.5' },
  ],
  TICKET: [
    { key: 'eventDate', label: "Date de l'évènement", placeholder: '2026-05-01' },
    { key: 'venue', label: 'Salle / Lieu', placeholder: 'Accor Arena' },
    { key: 'section', label: 'Section', placeholder: 'Bloc K' },
    { key: 'row', label: 'Rang', placeholder: 'Rang 8' },
    { key: 'seat', label: 'Siège', placeholder: 'Siège 12' },
    { key: 'status', label: 'Statut', placeholder: 'valide / transféré / utilisé' },
  ],
  OTHER: [
  ],
}

export const typeLabel = (type) =>
  (ITEM_TYPES.find((t) => t.value === type)?.label || 'Autre').toUpperCase()
