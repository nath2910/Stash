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

export function resolveHomeInventoryCount({ stockLoaded, stockItems = [], apiSummary = null }) {
  if (stockLoaded) return Array.isArray(stockItems) ? stockItems.length : 0
  const normalized = normalizeHomeApiSummary(apiSummary)
  if (normalized) return normalized.itemsEnStock
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
