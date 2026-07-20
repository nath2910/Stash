import { scrapeTrackingPage } from './browser-scrape-common.mjs'

const trackingUrl = process.argv[2]
const parsedUrl = trackingUrl ? new URL(trackingUrl) : null
const trackingNumber = parsedUrl?.searchParams.get('code')?.trim() || ''
const baseTrackingUrl = 'https://www.laposte.fr/outils/suivre-vos-envois'
const endpointBaseUrl = 'https://www.laposte.fr/ssu/sun/back/suivi-unifie'

async function pageContainsTracking(page) {
  if (!trackingNumber) {
    return false
  }
  return page.evaluate((needle) => {
    const text = (document.body?.innerText || '').toLowerCase()
    return text.includes(String(needle || '').toLowerCase())
  }, trackingNumber)
}

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

    if (!(await pageContainsTracking(page))) {
      if (page.url() !== baseTrackingUrl) {
        await page.goto(baseTrackingUrl, {
          waitUntil: 'domcontentloaded',
          timeout: 18000,
        }).catch(() => {})
      }
      await tools.dismissConsentBanners()
      await tools.populateTrackingForm()
    }

    await tools.dismissConsentBanners()

    if (!(await pageContainsTracking(page))) {
      await page.goto(`${baseTrackingUrl}?code=${encodeURIComponent(trackingNumber)}`, {
        waitUntil: 'domcontentloaded',
        timeout: 18000,
      }).catch(() => {})
      await tools.dismissConsentBanners()
    }
  },
  extractPayload: async (page) => {
    if (!trackingNumber) {
      return null
    }

    const endpointPayload = await page.evaluate(async ({ endpointBaseUrl, trackingNumber }) => {
      const response = await fetch(
        `${endpointBaseUrl}/${encodeURIComponent(trackingNumber)}?lang=fr`,
        { credentials: 'include' },
      )
      const text = await response.text()
      return {
        ok: response.ok,
        status: response.status,
        text,
        currentUrl: window.location.href,
      }
    }, { endpointBaseUrl, trackingNumber }).catch(() => null)

    if (!endpointPayload?.ok || !endpointPayload?.text) {
      return null
    }

    let parsed = null
    try {
      parsed = JSON.parse(endpointPayload.text)
    } catch {
      return null
    }

    const root = Array.isArray(parsed) ? parsed[0] : parsed
    const shipment = root?.shipment
    if (!shipment || Number(root?.returnCode || 0) !== 200) {
      return null
    }

    const timelineLines = Array.isArray(shipment.timeline)
      ? shipment.timeline
          .map((step) => String(step?.shortLabel || step?.longLabel || '').trim())
          .filter(Boolean)
      : []
    const eventLines = Array.isArray(shipment.event)
      ? shipment.event
          .map((event) => {
            const date = String(event?.date || '').trim()
            const label = String(event?.label || '').trim()
            return [date, label].filter(Boolean).join(' ')
          })
          .filter(Boolean)
      : []

    const summaryLines = [
      `Numero de suivi: ${shipment.idShip || trackingNumber}`,
      `Produit: ${shipment.product || ''}`.trim(),
      `Statut: ${shipment.event?.[0]?.label || shipment.timeline?.at(-1)?.shortLabel || ''}`.trim(),
      `Livraison: ${shipment.deliveryDate || shipment.estimDate || ''}`.trim(),
      ...timelineLines,
      ...eventLines,
    ].filter(Boolean)

    const syntheticHtml = [
      `<div class="tracking-provider" data-provider="laposte" data-product="${String(shipment.product || '').replace(/"/g, '&quot;')}">`,
      `<div class="tracking-number">${shipment.idShip || trackingNumber}</div>`,
      `<div class="tracking-status">${shipment.event?.[0]?.label || shipment.timeline?.at(-1)?.shortLabel || ''}</div>`,
      `<div class="tracking-delivery-date">${shipment.deliveryDate || shipment.estimDate || ''}</div>`,
      ...eventLines.map((line) => `<div class="tracking-event">${line}</div>`),
      `<script type="application/json" data-role="laposte-tracking-response">${endpointPayload.text.replace(/<\//g, '<\\/')}</script>`,
      '</div>',
    ].join('')

    return {
      title: 'Suivi La Poste',
      text: summaryLines.join('\n'),
      html: syntheticHtml,
      currentUrl: String(shipment.urlDetail || shipment.url || endpointPayload.currentUrl || '').trim(),
    }
  },
})

process.stdout.write(JSON.stringify(payload))
