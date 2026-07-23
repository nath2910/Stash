import { scrapeTrackingPage } from './browser-scrape-common.mjs'

const trackingUrl = process.argv[2]
const parsedUrl = trackingUrl ? new URL(trackingUrl) : null
const trackingNumber = parsedUrl?.searchParams.get('tracknum')?.trim() || ''

const payload = await scrapeTrackingPage({
  trackingUrl,
  trackingNumber,
  trackingInputHints: [
    'track',
    'tracking',
    'shipment',
    'package',
    'reference',
    'number',
  ],
  submitButtonHints: [
    'track',
    'search',
  ],
  waitPatterns: [
    'on the way',
    'out for delivery',
    'delivered',
    'delivered on',
    'estimated delivery',
    'label created',
    'information not available',
    'we could not locate the shipment details',
  ],
  blockPatterns: [
    'access denied',
    'just a moment',
    'enable javascript and cookies to continue',
    'sorry, you have been blocked',
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
