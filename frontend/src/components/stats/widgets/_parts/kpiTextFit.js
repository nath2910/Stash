const DEFAULT_CHAR_WEIGHT = 0.58

function clamp(value, min, max) {
  return Math.max(min, Math.min(max, value))
}

function charWeight(char) {
  if (/[0-9]/.test(char)) return 0.58
  if (char === ',' || char === '.') return 0.24
  if (char === '-' || char === '+' || char === '/') return 0.34
  if (char === ' ' || char === '\u00a0' || char === '\u202f') return 0.26
  if (char === '%' || char === '€' || char === '$') return 0.58
  if (/[A-Z]/.test(char)) return 0.68
  return DEFAULT_CHAR_WEIGHT
}

export function estimateKpiTextWeight(value) {
  const text = String(value ?? '--').trim()
  if (!text) return 2
  return Math.max(
    Array.from(text).reduce((sum, char) => sum + charWeight(char), 0),
    1.6,
  )
}

export function fitKpiValueSize(value, width, height, options = {}) {
  const safeWidth = Math.max(Number(width ?? 0) || 0, 1)
  const safeHeight = Math.max(Number(height ?? 0) || 0, 1)
  const min = Number(options.min ?? 22)
  const max = Number(options.max ?? 72)
  const paddingX = Number(options.paddingX ?? Math.max(44, safeWidth * 0.24))
  const paddingY = Number(options.paddingY ?? Math.max(14, safeHeight * 0.14))
  const heightRatio = Number(options.heightRatio ?? 0.48)
  const widthFudge = Number(options.widthFudge ?? 1.08)

  const availableWidth = Math.max(safeWidth - paddingX, 32)
  const availableHeight = Math.max(safeHeight - paddingY, 32)
  const widthBound = availableWidth / (estimateKpiTextWeight(value) * widthFudge)
  const heightBound = availableHeight * heightRatio

  return clamp(Math.floor(Math.min(widthBound, heightBound)), min, max)
}
