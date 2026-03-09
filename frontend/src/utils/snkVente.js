import { toNumber } from './formatters'

export function getField(obj, key, fallback = null) {
  if (!obj || !key) return fallback
  if (obj[key] != null) return obj[key]
  const snake = String(key).replace(/[A-Z]/g, (m) => `_${m.toLowerCase()}`)
  return obj[snake] ?? fallback
}

export function isVendue(vente) {
  return Boolean(getField(vente, 'dateVente'))
}

export function prixRetailOf(vente) {
  return toNumber(getField(vente, 'prixRetail'), 0)
}

export function prixResellOf(vente) {
  return toNumber(getField(vente, 'prixResell'), 0)
}

export function profitOf(vente) {
  return prixResellOf(vente) - prixRetailOf(vente)
}

export function hasResell(vente) {
  return prixResellOf(vente) > 0
}

export function typeOf(vente) {
  return (
    vente.type ||
    (vente.metadata?.type ? vente.metadata.type : vente.categorie === 'Pokemon' ? 'POKEMON_CARD' : 'SNEAKER')
  )
}
