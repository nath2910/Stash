import test, { beforeEach, mock } from 'node:test'
import assert from 'node:assert/strict'
import StatsServices from '../src/services/StatsServices.js'
import api from '../src/services/api.js'

const getMock = mock.method(api, 'get', async () => ({}))

beforeEach(() => {
  getMock.mock.resetCalls()
})

test('summary envoie from/to + start/end', async () => {
  await StatsServices.summary('2026-01-01', '2026-02-01')
  const call = getMock.mock.calls[0]
  assert.equal(call.arguments[0], '/stats/summary')
  assert.deepEqual(call.arguments[1], {
    params: {
      from: '2026-01-01',
      to: '2026-02-01',
      start: '2026-01-01',
      end: '2026-02-01',
      asOf: undefined,
      categories: undefined,
    },
  })
})

test('categories utilise le filtre seulement si les dates sont valides', async () => {
  await StatsServices.categories('2026-01-01', '2026-01-31')
  let call = getMock.mock.calls.at(-1)
  assert.equal(call.arguments[0], '/stats/categories')
  assert.deepEqual(call.arguments[1], { params: { from: '2026-01-01', to: '2026-01-31' } })

  getMock.mock.resetCalls()
  await StatsServices.categories('bad', 'date')
  call = getMock.mock.calls.at(-1)
  assert.equal(call.arguments[0], '/stats/categories')
  assert.deepEqual(call.arguments[1], { params: {} })
})
