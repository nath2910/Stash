import assert from 'node:assert/strict'
import { existsSync, readFileSync } from 'node:fs'
import path from 'node:path'
import test from 'node:test'

const dist = path.resolve('dist')
const indexPath = path.join(dist, 'index.html')

test('the production entry references existing hashed modules and no dev client', () => {
  assert.ok(existsSync(indexPath), 'dist/index.html must be generated before this test')
  const html = readFileSync(indexPath, 'utf8')

  assert.doesNotMatch(html, /@vite\/client|127\.0\.0\.1:5500|reload\.js|live-server/i)
  const modules = [...html.matchAll(/<script[^>]+type="module"[^>]+src="([^"]+)"/g)].map((match) => match[1])
  assert.ok(modules.length > 0, 'the production entry must contain a module bundle')

  for (const modulePath of modules) {
    assert.match(modulePath, /^\/assets\/[\w.-]+\.js$/)
    assert.ok(existsSync(path.join(dist, modulePath.slice(1))), `missing module ${modulePath}`)
  }
})

test('access credentials are session-scoped rather than persisted in localStorage', () => {
  const storageSource = readFileSync(path.resolve('src/utils/authStorage.js'), 'utf8')
  assert.match(storageSource, /sessionStorage\.setItem/)
  assert.doesNotMatch(storageSource, /localStorage\.setItem|localStorage\.getItem/)
})
