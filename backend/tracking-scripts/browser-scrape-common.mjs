import fs from 'node:fs'
import path from 'node:path'
import puppeteer from 'puppeteer-core'

const executableCandidates = [
  process.env.PUPPETEER_EXECUTABLE_PATH,
  'C:\\Program Files\\Microsoft\\Edge\\Application\\msedge.exe',
  'C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe',
  'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe',
  'C:\\Program Files (x86)\\Google\\Chrome\\Application\\chrome.exe',
  'C:\\Program Files\\Chromium\\Application\\chrome.exe',
  'C:\\Program Files\\Chromium\\chrome.exe',
  '/opt/google/chrome/chrome',
  '/usr/bin/chromium',
  '/usr/bin/chromium-browser',
  '/usr/lib/chromium/chromium',
  '/usr/lib/chromium-browser/chromium-browser',
  '/usr/bin/google-chrome',
  '/usr/bin/google-chrome-stable',
  '/usr/bin/microsoft-edge',
  '/usr/bin/microsoft-edge-stable',
  '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome',
  '/Applications/Microsoft Edge.app/Contents/MacOS/Microsoft Edge',
  '/snap/bin/chromium',
].filter(Boolean)

const browserCommandCandidates = [
  'msedge.exe',
  'msedge',
  'chrome.exe',
  'chrome',
  'chromium.exe',
  'chromium',
  'chromium-browser',
  'google-chrome',
  'google-chrome-stable',
  'microsoft-edge',
  'microsoft-edge-stable',
]

function stripQuotes(value) {
  if (typeof value !== 'string') {
    return ''
  }
  const normalized = value.trim()
  if (normalized.startsWith('"') && normalized.endsWith('"')) {
    return normalized.slice(1, -1)
  }
  return normalized
}

function executableNameCandidates(command) {
  const normalizedCommand = stripQuotes(command)
  if (!normalizedCommand) {
    return []
  }
  if (process.platform !== 'win32') {
    return [normalizedCommand]
  }
  const pathExt = (process.env.PATHEXT || '.EXE;.CMD;.BAT;.COM')
    .split(';')
    .map((extension) => extension.trim().toLowerCase())
    .filter(Boolean)
    .map((extension) => (extension.startsWith('.') ? extension : `.${extension}`))
  const candidates = new Set([normalizedCommand])
  if (!/\.(exe|cmd|bat|com)$/i.test(normalizedCommand)) {
    for (const extension of pathExt) {
      candidates.add(`${normalizedCommand}${extension}`)
    }
  }
  return [...candidates]
}

function pathExecutableCandidates(commands = browserCommandCandidates) {
  const pathValue = process.env.PATH || ''
  if (!pathValue) {
    return []
  }
  const candidates = new Set()
  for (const entry of pathValue.split(path.delimiter)) {
    const directory = stripQuotes(entry)
    if (!directory) {
      continue
    }
    for (const command of commands) {
      for (const executableName of executableNameCandidates(command)) {
        candidates.add(path.join(directory, executableName))
      }
    }
  }
  return [...candidates]
}

export function resolveExecutablePath() {
  const configuredExecutable = stripQuotes(process.env.PUPPETEER_EXECUTABLE_PATH)
  const candidatePool = [
    configuredExecutable,
    ...executableCandidates,
    ...pathExecutableCandidates(configuredExecutable ? [configuredExecutable, ...browserCommandCandidates] : browserCommandCandidates),
  ].filter(Boolean)
  const executablePath = candidatePool.find((candidate) => fs.existsSync(candidate))
  if (!executablePath) {
    throw new Error('No supported browser executable found')
  }
  return executablePath
}

function lower(values = []) {
  return values.map((value) => String(value || '').toLowerCase()).filter(Boolean)
}

function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

async function clickFirstVisible(page, selectors = []) {
  for (const selector of selectors) {
    try {
      const handle = await page.$(selector)
      if (!handle) {
        continue
      }
      const visible = await handle.evaluate((element) => {
        const style = window.getComputedStyle(element)
        const rect = element.getBoundingClientRect()
        return style.visibility !== 'hidden'
          && style.display !== 'none'
          && rect.width > 0
          && rect.height > 0
      })
      if (!visible) {
        continue
      }
      await handle.click()
      return true
    } catch {
      // try next selector
    }
  }
  return false
}

async function clickButtonByText(page, labels = []) {
  const normalizedLabels = lower(labels)
  if (!normalizedLabels.length) {
    return false
  }
  return page.evaluate((texts) => {
    const isVisible = (element) => {
      if (!(element instanceof HTMLElement)) {
        return false
      }
      const style = window.getComputedStyle(element)
      const rect = element.getBoundingClientRect()
      return style.visibility !== 'hidden'
        && style.display !== 'none'
        && rect.width > 0
        && rect.height > 0
    }

    const elements = Array.from(document.querySelectorAll('button, a, [role="button"], input[type="button"], input[type="submit"]'))
    const match = elements.find((element) => {
      if (!isVisible(element)) {
        return false
      }
      const text = [
        element.textContent,
        element.getAttribute('aria-label'),
        element.getAttribute('title'),
        element.getAttribute('value'),
      ]
        .filter(Boolean)
        .join(' ')
        .replace(/\s+/g, ' ')
        .trim()
        .toLowerCase()
      return texts.some((candidate) => text.includes(candidate))
    })
    if (!match) {
      return false
    }
    match.click()
    return true
  }, normalizedLabels)
}

async function dismissConsentBanners(page) {
  const selectorLabels = [
    '#onetrust-reject-all-handler',
    '#onetrust-accept-btn-handler',
    '#popin_tc_privacy_button_2',
    '#popin_tc_privacy_button',
    '#didomi-notice-agree-button',
    '#didomi-notice-disagree-button',
    'button[aria-label*="cookie" i]',
    'button[aria-label*="consent" i]',
    'button[aria-label*="accepter" i]',
    'button[title*="accepter" i]',
  ]
  const textLabels = [
    'ne pas accepter',
    'ne pas accepter et fermer',
    'essential cookies only',
    'refuser',
    'reject all',
    'reject',
    'continuer sans accepter',
    'allow essential',
    'continue without accepting',
    'accepter et fermer',
    'tout accepter',
    'accepter',
    "j'accepte",
    'accept all',
    'allow all',
  ]

  for (let attempt = 0; attempt < 5; attempt += 1) {
    const clicked = await clickFirstVisible(page, selectorLabels) || await clickButtonByText(page, textLabels)
    if (!clicked) {
      return
    }
    await sleep(900)
  }
}

async function populateInputByHints(page, value, inputHints = [], marker = 'tracking') {
  if (!value) {
    return false
  }

  return page.evaluate((fieldValue, hints, fieldMarker) => {
    const hintList = (hints || []).map((hint) => String(hint || '').toLowerCase()).filter(Boolean)
    const candidates = Array.from(document.querySelectorAll('input'))
    const nativeSetter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value')?.set

    const best = candidates
      .map((input) => {
        if (!(input instanceof HTMLInputElement) || input.disabled || input.readOnly) {
          return null
        }
        const style = window.getComputedStyle(input)
        const rect = input.getBoundingClientRect()
        if (style.visibility === 'hidden' || style.display === 'none' || rect.width < 80 || rect.height < 18) {
          return null
        }
        const text = [
          input.name,
          input.id,
          input.placeholder,
          input.getAttribute('aria-label'),
          input.getAttribute('autocomplete'),
          input.type,
        ]
          .filter(Boolean)
          .join(' ')
          .toLowerCase()

        let score = 0
        for (const hint of hintList) {
          if (text.includes(hint)) {
            score += 3
          }
        }
        if (input.form) {
          score += 1
        }
        if (input.hasAttribute('data-codex-field')) {
          score -= 2
        }
        return { input, score }
      })
      .filter(Boolean)
      .sort((left, right) => right.score - left.score)[0]

    if (!best || best.score <= 0) {
      return false
    }

    const input = best.input
    input.focus()
    if (nativeSetter) {
      nativeSetter.call(input, '')
      input.dispatchEvent(new Event('input', { bubbles: true }))
      nativeSetter.call(input, fieldValue)
    } else {
      input.value = fieldValue
    }
    input.dispatchEvent(new Event('input', { bubbles: true }))
    input.dispatchEvent(new Event('change', { bubbles: true }))
    input.setAttribute('data-codex-field', fieldMarker)
    return true
  }, value, inputHints, marker)
}

async function populateTrackingForm(page, trackingNumber, inputHints = [], submitHints = [], additionalFields = []) {
  if (!trackingNumber && !(additionalFields || []).some((field) => field?.value)) {
    return false
  }

  const filled = await populateInputByHints(
    page,
    trackingNumber,
    [...inputHints, 'track', 'suivi', 'colis', 'expedition'],
    'tracking',
  )

  let additionalFilled = false
  for (const field of additionalFields || []) {
    if (!field?.value) {
      continue
    }
    const currentFilled = await populateInputByHints(page, field.value, field.hints || [], field.key || 'extra')
    additionalFilled = additionalFilled || currentFilled
  }

  if (!filled && !additionalFilled) {
    return false
  }

  await sleep(500)

  const submitted = await clickButtonByText(page, submitHints)
  if (!submitted) {
    try {
      await page.focus('[data-codex-field="tracking"], [data-codex-field]')
      await page.keyboard.press('Enter')
    } catch {
      // fallback below
    }
  }

  await sleep(1800)
  return true
}

export async function scrapeTrackingPage({
  trackingUrl,
  trackingNumber = '',
  trackingInputHints = [],
  submitButtonHints = [],
  additionalFields = [],
  waitPatterns = [],
  blockPatterns = [],
  timeoutMs = 20000,
  extraWaitMs = 1200,
  preparePage,
  extractPayload,
}) {
  if (!trackingUrl) {
    throw new Error('Missing tracking URL')
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
      timeout: timeoutMs,
    })

    await dismissConsentBanners(page)

    if (typeof preparePage === 'function') {
      await preparePage(page, {
        dismissConsentBanners: () => dismissConsentBanners(page),
        populateTrackingForm: () => populateTrackingForm(
          page,
          trackingNumber,
          trackingInputHints,
          submitButtonHints,
          additionalFields,
        ),
        sleep,
      })
    } else if (trackingNumber && trackingInputHints.length) {
      await populateTrackingForm(page, trackingNumber, trackingInputHints, submitButtonHints, additionalFields)
    }

    await dismissConsentBanners(page)

    const readyPatterns = lower(waitPatterns)
    const challengePatterns = lower(blockPatterns)

    await page
      .waitForFunction(
        (patterns, blocked) => {
          const title = (document.title || '').toLowerCase()
          const text = (document.body?.innerText || '').toLowerCase()
          if (!text && !title) {
            return false
          }
          if (blocked.some((pattern) => title.includes(pattern) || text.includes(pattern))) {
            return true
          }
          if (!patterns.length) {
            return text.length > 60
          }
          return patterns.some((pattern) => title.includes(pattern) || text.includes(pattern))
        },
        {
          timeout: timeoutMs,
          polling: 500,
        },
        readyPatterns,
        challengePatterns,
      )
      .catch(() => {})

    if (extraWaitMs > 0) {
      await new Promise((resolve) => setTimeout(resolve, extraWaitMs))
    }

    if (typeof extractPayload === 'function') {
      const extracted = await extractPayload(page)
      if (extracted && typeof extracted === 'object') {
        return {
          source: 'local_browser',
          title: extracted.title || '',
          text: extracted.text || '',
          html: extracted.html || '',
          currentUrl: extracted.currentUrl || page.url(),
        }
      }
    }

    return await page.evaluate(() => ({
      source: 'local_browser',
      title: document.title || '',
      text: document.body?.innerText || '',
      html: document.body?.innerHTML || '',
      currentUrl: window.location.href,
    }))
  } finally {
    await browser.close()
  }
}
