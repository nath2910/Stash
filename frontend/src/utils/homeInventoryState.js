import { isGroupedItem, isVendue, itemQuantityOf, soldCountOf } from './snkVente'

export function normalizeHomeApiSummary(data) {
  if (!data) return null
  return {
    ca: Number(data.ca ?? 0),
    profit: Number(data.profit ?? 0),
    profitMargin: Number(data.profitMargin ?? 0),
    itemsVendues: Number(data.itemsVendues ?? 0),
    itemsEnStock: Number(data.itemsEnStock ?? 0),
    valeurStock: Number(data.valeurStock ?? 0),
    estimatedStockValue: Number(data.valeurStock ?? 0),
  }
}

export function countHomeInventoryUnits(items = []) {
  if (!Array.isArray(items)) return 0
  return items.reduce((total, item) => total + itemQuantityOf(item), 0)
}

export function countHomeStockUnits(items = []) {
  if (!Array.isArray(items)) return 0
  return items.reduce((total, item) => {
    if (isGroupedItem(item)) {
      return total + Math.max(0, itemQuantityOf(item) - soldCountOf(item))
    }
    return total + (isVendue(item) ? 0 : 1)
  }, 0)
}

export function resolveHomeInventoryCount({ stockLoaded, stockItems = [], apiSummary = null }) {
  const normalized = normalizeHomeApiSummary(apiSummary)
  if (apiSummary && Number.isFinite(Number(apiSummary.itemsEnStock))) {
    return normalized.itemsEnStock
  }
  if (stockLoaded) return countHomeStockUnits(stockItems)
  return null
}

export function resolveHomeKpiError({ statsError = '' }) {
  return statsError || ''
}

export function shouldShowHomeImportPrompt({
  hasToken,
  stockLoaded,
  stockLoading,
  stockError = '',
  stockItems = [],
}) {
  return Boolean(
    hasToken &&
      stockLoaded &&
      !stockLoading &&
      !stockError &&
      Array.isArray(stockItems) &&
      stockItems.length === 0,
  )
}
