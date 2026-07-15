import fs from 'node:fs'
import puppeteer from 'puppeteer-core'

const executableCandidates = [
  process.env.PUPPETEER_EXECUTABLE_PATH,
  'C:\\Program Files\\Microsoft\\Edge\\Application\\msedge.exe',
  'C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe',
  'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe',
  'C:\\Program Files (x86)\\Google\\Chrome\\Application\\chrome.exe',
].filter(Boolean)

function resolveExecutablePath() {
  const executablePath = executableCandidates.find((candidate) => fs.existsSync(candidate))
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
    '#popin_tc_privacy_button_2',
    '#didomi-notice-disagree-button',
    'button[aria-label*="cookie" i]',
    'button[aria-label*="consent" i]',
  ]
  const textLabels = [
    'ne pas accepter',
    'essential cookies only',
    'refuser',
    'reject all',
    'reject',
    'continuer sans accepter',
    'allow essential',
    'continue without accepting',
  ]

  for (let attempt = 0; attempt < 3; attempt += 1) {
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
