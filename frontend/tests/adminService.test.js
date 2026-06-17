import test, { beforeEach, mock } from 'node:test'
import assert from 'node:assert/strict'
import AdminService from '../src/services/AdminService.js'
import api from '../src/services/api.js'

const getMock = mock.method(api, 'get', async () => ({ data: {} }))
const putMock = mock.method(api, 'put', async () => ({ data: {} }))
const postMock = mock.method(api, 'post', async () => ({ data: new Blob(['pdf']) }))

beforeEach(() => {
  getMock.mock.resetCalls()
  putMock.mock.resetCalls()
  postMock.mock.resetCalls()
  global.window = {
    URL: {
      createObjectURL: () => 'blob:test',
      revokeObjectURL: () => {},
    },
  }
  global.document = {
    body: { appendChild: () => {} },
    createElement: () => ({
      click: () => {},
      remove: () => {},
      set href(value) {
        this._href = value
      },
      set download(value) {
        this._download = value
      },
    }),
  }
})

test('administrativeSummary appelle le nouvel endpoint avec la periode', async () => {
  await AdminService.administrativeSummary({ periodStart: '2026-01-01', periodEnd: '2026-01-31' })
  const call = getMock.mock.calls[0]
  assert.equal(call.arguments[0], '/administrative/summary')
  assert.deepEqual(call.arguments[1], {
    params: { periodStart: '2026-01-01', periodEnd: '2026-01-31' },
  })
})

test('saveAdministrativeProfile persiste le profil administratif', async () => {
  const payload = { profileType: 'MICRO_ENTREPRISE', siret: '12345678901234' }
  await AdminService.saveAdministrativeProfile(payload)
  const call = putMock.mock.calls[0]
  assert.equal(call.arguments[0], '/administrative/profile')
  assert.deepEqual(call.arguments[1], payload)
})

test('generateAdministrativeDocument demande un PDF', async () => {
  await AdminService.generateAdministrativeDocument('urssaf-summary', {
    periodStart: '2026-01-01',
    periodEnd: '2026-01-31',
    year: 2026,
  })
  const call = postMock.mock.calls[0]
  assert.equal(call.arguments[0], '/administrative/documents/urssaf-summary')
  assert.deepEqual(call.arguments[1], {
    periodStart: '2026-01-01',
    periodEnd: '2026-01-31',
    year: 2026,
  })
  assert.deepEqual(call.arguments[2], {
    responseType: 'blob',
    headers: { Accept: 'application/pdf' },
  })
})
