import { readFile, writeFile } from 'node:fs/promises'
import { resolve } from 'node:path'

const defaultApiUrl = 'https://api.mystash.fr'
const configuredApiUrl = String(process.env.VITE_API_URL || defaultApiUrl).trim()

let apiOrigin
try {
  apiOrigin = new URL(configuredApiUrl).origin
} catch {
  throw new Error('VITE_API_URL must be an absolute URL when building the frontend.')
}

const headersPath = resolve('dist/_headers')
const headers = await readFile(headersPath, 'utf8')
const updatedHeaders = headers.replace(
  "connect-src 'self' https://api.mystash.fr;",
  `connect-src 'self' ${apiOrigin};`,
)

await writeFile(headersPath, updatedHeaders)
