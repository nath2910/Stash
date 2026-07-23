export function describeBillingError(
  error,
  fallback = 'Impossible de lancer le paiement pour le moment.',
) {
  const responseData = error?.response?.data
  const responseMessage =
    (typeof responseData?.message === 'string' && responseData.message.trim()) ||
    (typeof responseData?.error === 'string' && responseData.error.trim()) ||
    ''

  if (responseMessage) {
    return responseMessage
  }

  const code = String(error?.code || '').toUpperCase()
  const message = String(error?.message || '').trim()
  const normalizedMessage = message.toLowerCase()

  if (code === 'ECONNABORTED' || normalizedMessage.includes('timeout')) {
    return 'Le service de paiement met trop de temps a repondre. Reessaie dans quelques instants.'
  }

  if (!error?.response && normalizedMessage.includes('network')) {
    return 'Impossible de joindre Stripe ou le backend paiement pour le moment.'
  }

  return fallback
}
