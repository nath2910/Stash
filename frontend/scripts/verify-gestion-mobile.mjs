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

const targetUrl = process.argv[2] || 'http://127.0.0.1:4173/gestion'
const screenshotPath = path.resolve(process.cwd(), '..', 'tmp', 'gestion-mobile-check.png')

const fixtureItems = Array.from({ length: 8 }, (_, index) => ({
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
  ][index],
  type: 'SNEAKER',
  prixAchat: 110 + index * 15,
  prixVente: index % 3 === 0 ? null : 220 + index * 20,
  dateAchat: '2026-07-01',
  dateVente: index % 3 === 0 ? null : '2026-07-15',
  taille: '44',
  status: index % 3 === 0 ? 'STOCK' : 'VENDU',
  metadata: {
    brand: ['Nike', 'Nike', 'New Balance', 'ASICS', 'Salomon', 'Adidas', 'Mizuno', 'Hoka'][index],
    colorway: 'White / Blue / Silver',
    sku: `SKU-${index + 1}`,
  },
}))

const now = Math.floor(Date.now() / 1000)
const base64url = (value) => Buffer.from(JSON.stringify(value)).toString('base64url')
const fakeToken = `${base64url({ alg: 'HS256', typ: 'JWT' })}.${base64url({ sub: '1', exp: now + 86400, email: 'test@example.com' })}.sig`
const fakeUser = {
  id: 1,
  firstName: 'Nathan',
  lastName: 'Test',
  email: 'test@example.com',
  subscriptionStatus: 'active',
}

const browser = await puppeteer.launch({
  executablePath: resolveExecutablePath(),
  headless: true,
  args: ['--no-sandbox', '--disable-dev-shm-usage'],
})

try {
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
    const method = request.method()
    if (url.startsWith('http://localhost:8080') || url.startsWith('http://127.0.0.1:8080')) {
      const respond = (body, status = 200) => request.respond({
        status,
        contentType: 'application/json',
        body: JSON.stringify(body),
        headers: { 'Access-Control-Allow-Origin': '*' },
      })

      if (method === 'OPTIONS') return respond({})
      if (url.includes('/auth/me')) return respond(fakeUser)
      if (url.includes('/billing/status')) return respond({ status: 'active', portalUrl: '' })
      if (url.includes('/snkVente')) return respond(fixtureItems)
      return respond({})
    }
    return request.continue()
  })

  await page.evaluateOnNewDocument((token, user) => {
    localStorage.setItem('snk_token', token)
    localStorage.setItem('snk_user', JSON.stringify(user))
    localStorage.setItem(
      'snk_billing_status_cache',
      JSON.stringify({ status: 'active', fetchedAt: Date.now() }),
    )
  }, fakeToken, fakeUser)

  await page.goto(targetUrl, { waitUntil: 'networkidle0', timeout: 60000 })
  await page.waitForSelector('.gestion-page-stack', { timeout: 30000 })
  fs.mkdirSync(path.dirname(screenshotPath), { recursive: true })
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
      .slice(0, 20)

    return {
      path: window.location.pathname + window.location.search,
      viewportWidth,
      docWidth,
      bodyWidth,
      hasHorizontalOverflow: docWidth > viewportWidth + 2,
      offenders,
    }
  })

  process.stdout.write(JSON.stringify({ screenshotPath, report }, null, 2))
  if (report.hasHorizontalOverflow) {
    process.exitCode = 1
  }
} finally {
  await browser.close()
}
