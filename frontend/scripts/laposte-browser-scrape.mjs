import { scrapeTrackingPage } from './browser-scrape-common.mjs'

const trackingUrl = process.argv[2]
const parsedUrl = trackingUrl ? new URL(trackingUrl) : null
const trackingNumber = parsedUrl?.searchParams.get('code')?.trim() || ''

const payload = await scrapeTrackingPage({
  trackingUrl,
  trackingNumber,
  trackingInputHints: [
    'suivi',
    'tracking',
    'colis',
    'courrier',
    'numero',
    'n°',
    'envoi',
  ],
  submitButtonHints: [
    'suivre',
    'rechercher',
    'valider',
    'search',
  ],
  waitPatterns: [
    'votre colis',
    'pris en charge',
    'en cours d acheminement',
    'en cours de livraison',
    'distribution',
    'livre',
    'aucune information',
    'aucun resultat',
    'aucun envoi',
    'introuvable',
  ],
  blockPatterns: [
    'access denied',
    'just a moment',
    'enable javascript and cookies to continue',
    'maintenance',
    'incident',
  ],
  timeoutMs: 18000,
  extraWaitMs: 1200,
  preparePage: async (page, tools) => {
    await tools.dismissConsentBanners()
    if (!trackingNumber) {
      return
    }
    await tools.populateTrackingForm()
    await tools.dismissConsentBanners()
    if (page.url() === 'https://www.laposte.fr/outils/suivre-vos-envois') {
      await page.goto(`https://www.laposte.fr/outils/suivre-vos-envois?code=${encodeURIComponent(trackingNumber)}`, {
        waitUntil: 'domcontentloaded',
        timeout: 18000,
      }).catch(() => {})
    }
  },
})

process.stdout.write(JSON.stringify(payload))
