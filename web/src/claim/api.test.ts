import { describe, expect, it, vi } from 'vitest'
import { submitClaim } from './api'
import { EMPTY_FORM } from './types'

const respond = (status: number, body: unknown) =>
  vi.fn().mockResolvedValue(new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } }))

describe('submitClaim', () => {
  it('posts the nested request body and returns the claim number', async () => {
    const fetchImpl = respond(201, { claimNumber: 'CLM-7K4Q-9XHM', status: 'SUBMITTED' })
    const values = { ...EMPTY_FORM, policyNumber: 'ho-12345678', 'address.line1': '123 Main St', phone: ' ' }

    const result = await submitClaim(values, 'key-1', fetchImpl)

    expect(result).toEqual({ kind: 'accepted', claimNumber: 'CLM-7K4Q-9XHM' })
    const [url, init] = fetchImpl.mock.calls[0]
    expect(url).toBe('/api/v1/claims')
    expect(init.headers['Idempotency-Key']).toBe('key-1')
    const body = JSON.parse(init.body)
    expect(body.address.line1).toBe('123 Main St')
    expect(body.policyNumber).toBe('HO-12345678')
    expect(body.phone).toBeNull()
  })

  it('maps 400 problem details to field errors', async () => {
    const fetchImpl = respond(400, { errors: [{ field: 'email', message: 'must be a well-formed email address' }] })

    expect(await submitClaim(EMPTY_FORM, 'key-1', fetchImpl)).toEqual({
      kind: 'invalid',
      fieldErrors: { email: 'must be a well-formed email address' },
    })
  })

  it('returns a generic failure with the correlation id on 500', async () => {
    const result = await submitClaim(EMPTY_FORM, 'key-1', respond(500, { correlationId: 'abc-123' }))

    expect(result).toMatchObject({ kind: 'failed', reference: 'abc-123' })
  })

  it('returns a generic failure when the network is down', async () => {
    const result = await submitClaim(EMPTY_FORM, 'key-1', vi.fn().mockRejectedValue(new TypeError('Failed to fetch')))

    expect(result.kind).toBe('failed')
  })
})
