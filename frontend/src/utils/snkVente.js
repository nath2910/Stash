import { toNumber } from './formatters'

export function getField(obj, key, fallback = null) {
  if (!obj || !key) return fallback
  if (obj[key] != null) return obj[key]
  const snake = String(key).replace(/[A-Z]/g, (m) => `_${m.toLowerCase()}`)
  return obj[snake] ?? fallback
}

export function childItemsOf(vente) {
  return Array.isArray(vente?.children) ? vente.children : []
}

export function isGroupedItem(vente) {
  return Boolean(vente?.groupParent) && childItemsOf(vente).length > 0
}

export function itemQuantityOf(vente) {
  const quantity = Number(getField(vente, 'quantity', null))
  if (Number.isFinite(quantity) && quantity > 0) return quantity
  return isGroupedItem(vente) ? childItemsOf(vente).length : 1
}

export function soldCountOf(vente) {
  const soldCount = Number(getField(vente, 'soldCount', null))
  if (Number.isFinite(soldCount) && soldCount >= 0) return soldCount
  if (isGroupedItem(vente)) return childItemsOf(vente).filter((child) => isVendue(child)).length
  return isVendue(vente) ? 1 : 0
}

export function isVendue(vente) {
  if (isGroupedItem(vente)) return soldCountOf(vente) >= itemQuantityOf(vente)
  return Boolean(getField(vente, 'dateVente'))
}

export function prixRetailOf(vente) {
  return toNumber(getField(vente, 'prixRetail'), 0)
}

export function prixResellOf(vente) {
  return toNumber(getField(vente, 'prixResell'), 0)
}

export function totalRetailOf(vente) {
  return toNumber(getField(vente, 'totalRetail', getField(vente, 'prixRetail')), 0)
}

export function totalResellOf(vente) {
  return toNumber(getField(vente, 'totalResell', getField(vente, 'prixResell')), 0)
}

export function profitOf(vente) {
  if (isGroupedItem(vente)) return totalProfitOf(vente)
  return prixResellOf(vente) - prixRetailOf(vente)
}

export function totalProfitOf(vente) {
  return toNumber(getField(vente, 'totalProfit', profitOfSimple(vente)), 0)
}

function profitOfSimple(vente) {
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
