import fs from 'node:fs'
import path from 'node:path'
import puppeteer from 'puppeteer-core'

const executableCandidates = [
  process.env.PUPPETEER_EXECUTABLE_PATH,
  'C:/Program Files/Microsoft/Edge/Application/msedge.exe',
  'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe',
  'C:/Program Files/Google/Chrome/Application/chrome.exe',
  'C:/Program Files (x86)/Google/Chrome/Application/chrome.exe',
].filter(Boolean)

function resolveExecutablePath() {
  const executablePath = executableCandidates.find((candidate) => fs.existsSync(candidate))
  if (!executablePath) {
    throw new Error('No supported browser executable found')
  }
  return executablePath
}

function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

const baseUrl = process.argv[2] || 'http://127.0.0.1:4173'
const outputDir = path.resolve(process.cwd(), '..', 'tmp', 'mobile-audit')

const fixtureItems = Array.from({ length: 12 }, (_, index) => ({
  id: index + 1,
  nomItem: [
    'Air Jordan 1 Retro High OG',
    'Nike SB Dunk Low Pro',
    'New Balance 2002R Protection Pack',
    'ASICS Gel-Kayano 14',
    'Salomon XT-6 Gore-Tex',
    'Adidas Samba OG',
    'Mizuno Wave Rider 10',
    'Hoka Clifton LS',
    'Nike Air Max Plus Drift',
    'Saucony ProGrid Omni 9',
    'Puma Palermo Vintage',
    'Merrell Moab Speed 2',
  ][index],
  categorie: [
    'Sneakers premium tres longue categorie',
    'Skateboarding',
    'Running Lifestyle',
    'Performance',
    'Trail',
    'Terrace',
    'Retro running',
    'Daily trainer',
    'Streetwear',
    'Archive runner',
    'Terrace pack',
    'Outdoor',
  ][index],
  type: 'SNEAKER',
  prixAchat: 110 + index * 11,
  prixVente: index % 4 === 0 ? null : 220 + index * 17,
  dateAchat: `2026-0${(index % 6) + 1}-0${(index % 9) + 1}`,
  dateVente: index % 4 === 0 ? null : `2026-0${(index % 6) + 1}-1${(index % 8) + 1}`,
  taille: ['42', '43', '44', '45'][index % 4],
  status: index % 4 === 0 ? 'STOCK' : 'VENDU',
  metadata: {
    brand: ['Nike', 'Nike', 'New Balance', 'ASICS', 'Salomon', 'Adidas', 'Mizuno', 'Hoka', 'Nike', 'Saucony', 'Puma', 'Merrell'][index],
    colorway: 'White / Blue / Silver',
    sku: `SKU-${index + 1}`,
  },
}))

const annualSummary = {
  ca: 16890,
  profit: 4920,
  profitMargin: 29.1,
  itemsVendues: 83,
  itemsEnStock: 157,
  valeurStock: 18420,
}

const statsSeries = {
  points: [
    { date: '2026-01-01', value: 2200 },
    { date: '2026-02-01', value: 1800 },
    { date: '2026-03-01', value: 2400 },
    { date: '2026-04-01', value: 3100 },
    { date: '2026-05-01', value: 2800 },
    { date: '2026-06-01', value: 4590 },
  ],
}

const topSales = fixtureItems
  .filter((item) => item.prixVente)
  .slice(0, 6)
  .map((item) => ({
    id: item.id,
    name: item.nomItem,
    brand: item.metadata.brand,
    salePrice: item.prixVente,
    purchasePrice: item.prixAchat,
    profit: item.prixVente - item.prixAchat,
  }))

const legalProfile = {
  legalStatus: 'micro_bic',
  legalProfileType: 'micro_bic',
  completed: true,
  siren: '123456789',
  siret: '12345678900012',
  vatNumber: '',
  declarationFrequency: 'monthly',
  legalForm: 'Micro-entreprise',
  activities: ['achat revente sneakers', 'accessoires'],
  declarations: ['urssaf', 'livre recettes'],
}

const deliveryAccounts = [
  {
    id: 1,
    email: 'tracking@stash.test',
    provider: 'gmail',
    status: 'CONNECTED',
    syncedAt: '2026-07-18T08:30:00Z',
  },
]

const deliveryParcels = [
  {
    id: 1,
    trackingNumber: 'XR646836167TS',
    normalizedTrackingNumber: 'XR646836167TS',
    carrierSlug: 'chronopost',
    status: 'DELIVERED',
    statusLabel: 'Livraison effectuee',
    deliveredAt: '2026-06-23T15:26:00Z',
    trackingUrl: 'https://www.chronopost.fr/tracking-no-cms/suivi-page?langue=fr&listeNumerosLT=XR646836167TS',
  },
  {
    id: 2,
    trackingNumber: '6A04296519970',
    normalizedTrackingNumber: '6A04296519970',
    carrierSlug: 'colissimo',
    status: 'DELIVERED',
    statusLabel: 'Votre colis est livre dans votre boite aux lettres.',
    deliveredAt: '2026-05-11T10:10:24+02:00',
    trackingUrl: 'https://www.laposte.fr/outils/suivre-vos-envois?code=6A04296519970',
  },
]

const routeConfigs = [
  {
    name: 'home',
    path: '/',
    auth: true,
    subscriptionStatus: 'active',
    selector: '.home-page-light',
  },
  {
    name: 'gestion-inventory',
    path: '/gestion',
    auth: true,
    subscriptionStatus: 'active',
    selector: '.gestion-page-stack',
  },
  {
    name: 'gestion-delivery',
    path: '/gestion?tab=delivery',
    auth: true,
    subscriptionStatus: 'active',
    selector: '.gestion-page-stack',
  },
  {
    name: 'gestion-admin',
    path: '/gestion?tab=admin',
    auth: true,
    subscriptionStatus: 'active',
    selector: '.gestion-page-stack',
  },
  {
    name: 'stats',
    path: '/stats',
    auth: true,
    subscriptionStatus: 'active',
    selector: '.stats-page-shell',
  },
  {
    name: 'account',
    path: '/compte',
    auth: true,
    subscriptionStatus: 'active',
    selector: '.account-page-dark',
  },
  {
    name: 'abo',
    path: '/abo',
    auth: true,
    subscriptionStatus: 'inactive',
    selector: '.app-page-stack--abo',
  },
  {
    name: 'abo-view',
    path: '/mon-abonnement',
    auth: true,
    subscriptionStatus: 'active',
    selector: '.app-page-stack',
  },
  {
    name: 'discover',
    path: '/discover',
    auth: false,
    selector: 'main',
  },
]

function createFakeUser(subscriptionStatus = 'active') {
  return {
    id: 1,
    firstName: 'Nathan',
    lastName: 'Test',
    email: 'test@example.com',
    subscriptionStatus,
  }
}

function buildFakeToken() {
  const now = Math.floor(Date.now() / 1000)
  const base64url = (value) => Buffer.from(JSON.stringify(value)).toString('base64url')
  return `${base64url({ alg: 'HS256', typ: 'JWT' })}.${base64url({ sub: '1', exp: now + 86400, email: 'test@example.com' })}.sig`
}

function createStatsPayload(url) {
  if (url.includes('/stats/date-bounds')) {
    return { min: '2026-01-01', max: '2026-07-20' }
  }
  if (url.includes('/stats/categories')) {
    return ['Sneakers premium tres longue categorie', 'Skateboarding', 'Trail']
  }
  if (url.includes('/stats/layout')) {
    return { layout: {} }
  }
  if (url.includes('/stats/summary')) {
    return annualSummary
  }
  if (url.includes('/stats/timeseries') || url.includes('/stats/series/')) {
    return statsSeries
  }
  if (url.includes('/stats/brands')) {
    return [
      { brand: 'Nike', value: 42 },
      { brand: 'Adidas', value: 18 },
      { brand: 'New Balance', value: 12 },
    ]
  }
  if (url.includes('/stats/top-sales')) {
    return topSales
  }
  if (url.includes('/stats/breakdown/')) {
    return [
      { label: 'Stock', value: 157 },
      { label: 'Vendu', value: 83 },
    ]
  }
  if (url.includes('/stats/rank/')) {
    return topSales
  }
  if (url.includes('/stats/kpi/')) {
    return { value: 42, delta: 7.5 }
  }
  if (url.includes('/stats/annual-dashboard')) {
    return { summary: annualSummary, topSales }
  }
  if (url.includes('/stats/monthly-dashboard')) {
    return { summary: annualSummary, topSales: topSales.slice(0, 3) }
  }
  return {}
}

function createResponseBody(url, method, routeConfig) {
  if (method === 'OPTIONS') return {}
  if (url.includes('/auth/me')) return createFakeUser(routeConfig.subscriptionStatus)
  if (url.includes('/billing/status')) {
    return {
      status: routeConfig.subscriptionStatus,
      portalUrl: routeConfig.subscriptionStatus === 'active' ? 'https://billing.stripe.test/portal' : '',
    }
  }
  if (url.includes('/user/legal-profile')) return legalProfile
  if (url.includes('/snkVente/recent')) return fixtureItems.slice(0, 5)
  if (url.includes('/snkVente/topVentes')) return topSales
  if (url.includes('/snkVente/total')) return { total: fixtureItems.length }
  if (url.includes('/snkVente/ca')) return { ca: annualSummary.ca }
  if (url.includes('/snkVente/marque')) return [{ marque: 'Nike', total: 5 }]
  if (url.includes('/snkVente')) return fixtureItems
  if (url.includes('/stats/')) return createStatsPayload(url)
  if (url.includes('/delivery/mail-accounts')) return deliveryAccounts
  if (url.includes('/delivery/parcels')) return deliveryParcels
  if (url.includes('/delivery/dashboard')) {
    return {
      parcels: deliveryParcels,
      accounts: deliveryAccounts,
      counters: {
        total: deliveryParcels.length,
        delivered: deliveryParcels.filter((parcel) => parcel.status === 'DELIVERED').length,
        inTransit: deliveryParcels.filter((parcel) => parcel.status === 'IN_TRANSIT').length,
      },
    }
  }
  if (url.includes('/delivery/tracking')) {
    return deliveryParcels[0]
  }
  return {}
}

async function setSessionState(page, routeConfig) {
  const token = routeConfig.auth ? buildFakeToken() : ''
  const user = createFakeUser(routeConfig.subscriptionStatus)
  await page.evaluateOnNewDocument(
    (authEnabled, fakeToken, fakeUser, subscriptionStatus) => {
      if (authEnabled) {
        localStorage.setItem('snk_token', fakeToken)
        localStorage.setItem('snk_user', JSON.stringify(fakeUser))
      } else {
        localStorage.removeItem('snk_token')
        localStorage.removeItem('snk_user')
      }
      localStorage.setItem(
        'snk_billing_status_cache',
        JSON.stringify({ status: subscriptionStatus, fetchedAt: Date.now() }),
      )
    },
    routeConfig.auth,
    token,
    user,
    routeConfig.subscriptionStatus || 'inactive',
  )
}

async function auditRoute(browser, routeConfig) {
  const page = await browser.newPage()
  await page.setViewport({
    width: 390,
    height: 844,
    isMobile: true,
    deviceScaleFactor: 3,
    hasTouch: true,
  })
  await page.setRequestInterception(true)

  page.on('request', async (request) => {
    const url = request.url()
    if (url.startsWith('http://localhost:8080') || url.startsWith('http://127.0.0.1:8080')) {
      const body = createResponseBody(url, request.method(), routeConfig)
      return request.respond({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(body),
        headers: { 'Access-Control-Allow-Origin': '*' },
      })
    }
    return request.continue()
  })

  await setSessionState(page, routeConfig)
  const targetUrl = new URL(routeConfig.path, `${baseUrl}/`).toString()
  await page.goto(targetUrl, { waitUntil: 'networkidle0', timeout: 60000 })
  if (routeConfig.selector) {
    await page.waitForSelector(routeConfig.selector, { timeout: 30000 }).catch(() => {})
  }
  await sleep(1200)

  const screenshotPath = path.join(outputDir, `${routeConfig.name}.png`)
  await page.screenshot({ path: screenshotPath, fullPage: true })

  const report = await page.evaluate(() => {
    const viewportWidth = window.innerWidth
    const docWidth = document.documentElement.scrollWidth
    const bodyWidth = document.body.scrollWidth
    const offenders = Array.from(document.querySelectorAll('body *'))
      .map((element) => {
        const rect = element.getBoundingClientRect()
        const style = window.getComputedStyle(element)
        return {
          tag: element.tagName.toLowerCase(),
          className: typeof element.className === 'string' ? element.className : '',
          left: Math.round(rect.left),
          right: Math.round(rect.right),
          width: Math.round(rect.width),
          whiteSpace: style.whiteSpace,
          overflowX: style.overflowX,
        }
      })
      .filter((item) => item.width > viewportWidth + 4 || item.right > viewportWidth + 4 || item.left < -4)
      .slice(0, 25)

    return {
      path: window.location.pathname + window.location.search,
      viewportWidth,
      docWidth,
      bodyWidth,
      hasHorizontalOverflow: docWidth > viewportWidth + 2,
      offenders,
    }
  })

  await page.close()
  return {
    name: routeConfig.name,
    screenshotPath,
    report,
  }
}

const browser = await puppeteer.launch({
  executablePath: resolveExecutablePath(),
  headless: true,
  args: ['--no-sandbox', '--disable-dev-shm-usage'],
})

try {
  fs.mkdirSync(outputDir, { recursive: true })
  const results = []
  for (const routeConfig of routeConfigs) {
    // sequential on purpose to keep screenshots and request mocks deterministic
    // eslint-disable-next-line no-await-in-loop
    results.push(await auditRoute(browser, routeConfig))
  }

  const failed = results.filter((result) => result.report.hasHorizontalOverflow)
  process.stdout.write(JSON.stringify({ baseUrl, results, failed: failed.map((item) => item.name) }, null, 2))
  if (failed.length) {
    process.exitCode = 1
  }
} finally {
  await browser.close()
}
