import test, { beforeEach, mock } from 'node:test'
import assert from 'node:assert/strict'
import DeliveryTrackingService from '../src/services/DeliveryTrackingService.js'
import api from '../src/services/api.js'

const getMock = mock.method(api, 'get', async () => ({}))
const postMock = mock.method(api, 'post', async () => ({}))
const deleteMock = mock.method(api, 'delete', async () => ({}))

beforeEach(() => {
  getMock.mock.resetCalls()
  postMock.mock.resetCalls()
  deleteMock.mock.resetCalls()
})

test('DeliveryTrackingService utilise les endpoints mail accounts', async () => {
  await DeliveryTrackingService.listMailAccounts()
  assert.equal(getMock.mock.calls[0].arguments[0], '/delivery/mail-accounts')

  await DeliveryTrackingService.connectGmail()
  assert.equal(postMock.mock.calls[0].arguments[0], '/delivery/mail-accounts/gmail/connect')

  await DeliveryTrackingService.connectGmail('user@gmail.com')
  assert.equal(postMock.mock.calls[1].arguments[0], '/delivery/mail-accounts/gmail/connect')
  assert.deepEqual(postMock.mock.calls[1].arguments[1], { emailAddress: 'user@gmail.com' })

  await DeliveryTrackingService.scanNow(42)
  assert.equal(postMock.mock.calls[2].arguments[0], '/delivery/mail-accounts/42/scan-now')

  await DeliveryTrackingService.scanAll()
  assert.equal(postMock.mock.calls[3].arguments[0], '/delivery/mail-accounts/scan-all')

  await DeliveryTrackingService.listTrackingCandidates()
  assert.equal(getMock.mock.calls[1].arguments[0], '/delivery/tracking-candidates')

  await DeliveryTrackingService.confirmTrackingCandidate(9)
  assert.equal(postMock.mock.calls[4].arguments[0], '/delivery/tracking-candidates/9/confirm')

  await DeliveryTrackingService.ignoreTrackingCandidate(9)
  assert.equal(postMock.mock.calls[5].arguments[0], '/delivery/tracking-candidates/9/ignore')

  await DeliveryTrackingService.deleteMailAccount(42)
  assert.equal(deleteMock.mock.calls[0].arguments[0], '/delivery/mail-accounts/42')
})

test('DeliveryTrackingService utilise les endpoints parcels', async () => {
  await DeliveryTrackingService.listParcels()
  assert.equal(getMock.mock.calls[0].arguments[0], '/delivery/parcels')

  await DeliveryTrackingService.createParcel({
    trackingNumber: '1Z999AA10123456784',
    carrierSlug: 'ups',
  })
  assert.equal(postMock.mock.calls[0].arguments[0], '/delivery/parcels')
  assert.deepEqual(postMock.mock.calls[0].arguments[1], {
    trackingNumber: '1Z999AA10123456784',
    carrierSlug: 'ups',
  })

  await DeliveryTrackingService.getParcel(7)
  assert.equal(getMock.mock.calls[1].arguments[0], '/delivery/parcels/7')

  await DeliveryTrackingService.refreshParcel(7)
  assert.equal(postMock.mock.calls[1].arguments[0], '/delivery/parcels/7/refresh')

  await DeliveryTrackingService.deleteParcel(7)
  assert.equal(deleteMock.mock.calls[0].arguments[0], '/delivery/parcels/7')
})
