import { scrapeTrackingPage } from './browser-scrape-common.mjs'

const trackingUrl = process.argv[2]
const parsedUrl = trackingUrl ? new URL(trackingUrl) : null
const trackingNumber = parsedUrl?.searchParams.get('numeroExpedition')?.trim() || ''
const postalCode = parsedUrl?.searchParams.get('codePostal')?.trim() || ''

const payload = await scrapeTrackingPage({
  trackingUrl,
  trackingNumber,
  trackingInputHints: [
    'expedition',
    'suivi',
    'colis',
    'tracking',
    'numero',
  ],
  additionalFields: postalCode
    ? [
        {
          key: 'postal-code',
          value: postalCode,
          hints: ['code postal', 'postal', 'cp', 'postcode'],
        },
      ]
    : [],
  submitButtonHints: [
    'suivre',
    'rechercher',
    'track',
    'search',
  ],
  waitPatterns: [
    'expedition',
    'point relais',
    'locker',
    'colis',
    'pris en charge',
    'disponible',
    'livre',
    'retire',
    'code postal',
    'aucune information',
    'introuvable',
  ],
  blockPatterns: [
    'attention required',
    'sorry, you have been blocked',
    'cloudflare',
    'just a moment',
  ],
  timeoutMs: 18000,
  extraWaitMs: 1200,
  preparePage: async (_page, tools) => {
    await tools.dismissConsentBanners()
    if (trackingNumber) {
      await tools.populateTrackingForm()
    }
  },
})

process.stdout.write(JSON.stringify(payload))
