import test, { beforeEach, mock } from 'node:test'
import assert from 'node:assert/strict'
import StatsServices from '../src/services/StatsServices.js'
import StatsTemplatesService from '../src/services/StatsTemplatesService.js'

const getLayoutMock = mock.method(StatsServices, 'getLayout', async () => ({
  data: {
    layout: {
      version: 1,
      profiles: { p1: [] },
      customTemplates: [
        {
          id: 'custom_a',
          name: 'Template perso',
          category: 'General',
          tags: ['a'],
          accent: '#0ea5e9',
          recommendedRange: { kind: 'month' },
          layout: [{ type: 'netProfit' }],
        },
      ],
    },
  },
}))

const saveLayoutMock = mock.method(StatsServices, 'saveLayout', async () => ({
  data: { ok: true },
}))

beforeEach(() => {
  getLayoutMock.mock.resetCalls()
  saveLayoutMock.mock.resetCalls()
})

test('extractCustomTemplates lit et normalise customTemplates depuis le layout', () => {
  const output = StatsTemplatesService.extractCustomTemplates({
    version: 1,
    profiles: { p1: [] },
    customTemplates: [
      {
        id: 'custom_a',
        name: 'Template perso',
        category: 'General',
        tags: ['alpha', 'alpha'],
        accent: '#09f',
        recommendedRange: { kind: 'month' },
        layout: [{ type: 'netProfit' }],
      },
    ],
  })

  assert.equal(output.length, 1)
  assert.equal(output[0].id, 'custom_a')
  assert.deepEqual(output[0].tags, ['alpha'])
  assert.equal(output[0].accent, '#0099ff')
})

test('loadCustomTemplates utilise getLayout', async () => {
  const templates = await StatsTemplatesService.loadCustomTemplates()
  assert.equal(getLayoutMock.mock.calls.length, 1)
  assert.equal(templates.length, 1)
  assert.equal(templates[0].id, 'custom_a')
})

test('saveCustomTemplates merge le layout puis appelle saveLayout', async () => {
  const result = await StatsTemplatesService.saveCustomTemplates([
    {
      id: 'custom_b',
      name: 'Template B',
      category: 'Finance',
      tags: ['x'],
      accent: '#16a34a',
      recommendedRange: { kind: 'ytd' },
      layout: [{ type: 'grossRevenue' }],
    },
  ])

  assert.equal(getLayoutMock.mock.calls.length, 1)
  assert.equal(saveLayoutMock.mock.calls.length, 1)
  const payload = saveLayoutMock.mock.calls[0].arguments[0]
  assert.equal(payload.version, 1)
  assert.ok(payload.profiles)
  assert.equal(payload.customTemplates.length, 1)
  assert.equal(payload.customTemplates[0].id, 'custom_b')
  assert.deepEqual(result, { data: { ok: true } })
})
