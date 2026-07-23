import puppeteer from 'puppeteer-core'
import { resolveExecutablePath } from './browser-scrape-common.mjs'

const trackingUrl = process.argv[2]

if (!trackingUrl) {
  console.error('Missing tracking URL')
  process.exit(1)
}

const browser = await puppeteer.launch({
  executablePath: resolveExecutablePath(),
  headless: true,
  args: [
    '--disable-blink-features=AutomationControlled',
    '--disable-dev-shm-usage',
    '--lang=fr-FR',
    '--no-first-run',
    '--no-sandbox',
  ],
  defaultViewport: {
    width: 1440,
    height: 1200,
  },
})

try {
  const page = await browser.newPage()
  await page.setUserAgent(
    'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 '
      + '(KHTML, like Gecko) Chrome/138.0.0.0 Safari/537.36',
  )
  await page.setExtraHTTPHeaders({
    'accept-language': 'fr-FR,fr;q=0.9,en-US;q=0.8,en;q=0.7',
  })

  await page.evaluateOnNewDocument(() => {
    Object.defineProperty(navigator, 'webdriver', {
      get: () => false,
    })
  })

  await page.goto(trackingUrl, {
    waitUntil: 'domcontentloaded',
    timeout: 15000,
  })

  await page.waitForFunction(
    () => {
      const title = document.title || ''
      if (title.toLowerCase().includes('just a moment')) {
        return false
      }
      return Boolean(
        document.querySelector('.ch-suivi-colis-light-info')
          || document.querySelector('tr.toggleElmt')
          || document.body?.innerText?.includes('En cours d')
          || document.body?.innerText?.includes('LivrÃ©')
          || document.body?.innerText?.includes('Pris en charge'),
      )
    },
    {
      timeout: 15000,
      polling: 500,
    },
  )

  const payload = await page.evaluate(() => ({
    title: document.title || '',
    text: document.body?.innerText || '',
    body: document.body?.innerHTML || '',
    currentUrl: window.location.href,
  }))

  if (!payload.body || payload.title.toLowerCase().includes('just a moment')) {
    throw new Error('Chronopost page still blocked by Cloudflare')
  }

  process.stdout.write(
    JSON.stringify({
      source: 'local_browser',
      title: payload.title,
      text: payload.text,
      html: payload.body,
      currentUrl: payload.currentUrl,
    }),
  )
} finally {
  await browser.close()
}
