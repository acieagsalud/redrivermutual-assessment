import { describe, expect, it } from 'vitest'
import { EMPTY_FORM, type ClaimFormValues } from './types'
import { oneYearBefore, todayInWinnipeg, validate } from './validation'

const TODAY = '2026-10-22'

const valid: ClaimFormValues = {
  ...EMPTY_FORM,
  firstName: 'Dana',
  lastName: 'Smith',
  email: 'dana@example.com',
  policyNumber: 'HO-12345678',
  lossDate: '2026-10-20',
  lossType: 'SEWER_BACKUP',
  'address.line1': '123 Main St',
  'address.city': 'Winnipeg',
  'address.postalCode': 'R3C 1A1',
  description: 'Basement flooded after heavy rain; carpet damaged.',
}

describe('validate', () => {
  it('accepts a complete claim', () => {
    expect(validate(valid, TODAY)).toEqual({})
  })

  it('flags every required field on an empty form', () => {
    const errors = validate({ ...EMPTY_FORM, 'address.province': '' }, TODAY)
    expect(Object.keys(errors).sort()).toEqual(
      [
        'address.city',
        'address.line1',
        'address.postalCode',
        'address.province',
        'description',
        'email',
        'firstName',
        'lastName',
        'lossDate',
        'lossType',
        'policyNumber',
      ].sort(),
    )
  })

  it('treats phone and unit as optional but checks phone format when given', () => {
    expect(validate({ ...valid, phone: '' }, TODAY).phone).toBeUndefined()
    expect(validate({ ...valid, phone: '204-555-0142' }, TODAY).phone).toBeUndefined()
    expect(validate({ ...valid, phone: '555' }, TODAY).phone).toBeDefined()
  })

  it.each([
    ['2026-10-23', 'cannot be in the future'],
    ['2025-10-21', 'within one year'],
  ])('rejects loss date %s', (lossDate, message) => {
    expect(validate({ ...valid, lossDate }, TODAY).lossDate).toContain(message)
  })

  it('accepts today and exactly one year ago', () => {
    expect(validate({ ...valid, lossDate: TODAY }, TODAY).lossDate).toBeUndefined()
    expect(validate({ ...valid, lossDate: '2025-10-22' }, TODAY).lossDate).toBeUndefined()
  })

  it.each(['R3C1A1', 'r3c 1a1', 'R3C-1A1'])('accepts postal code %s', (code) => {
    expect(validate({ ...valid, 'address.postalCode': code }, TODAY)['address.postalCode']).toBeUndefined()
  })

  it.each(['90210', 'R3C 1A', 'ABC DEF'])('rejects postal code %s', (code) => {
    expect(validate({ ...valid, 'address.postalCode': code }, TODAY)['address.postalCode']).toBeDefined()
  })

  it('requires the policy number format', () => {
    expect(validate({ ...valid, policyNumber: '12345678' }, TODAY).policyNumber).toContain('HO-12345678')
  })

  it('requires a meaningful description', () => {
    expect(validate({ ...valid, description: 'Too short' }, TODAY).description).toContain('at least 20')
  })
})

describe('dates', () => {
  it('uses the Winnipeg calendar day, not UTC', () => {
    // 01:00 UTC on Oct 23 is still Oct 22 in Winnipeg.
    expect(todayInWinnipeg(new Date('2026-10-23T01:00:00Z'))).toBe('2026-10-22')
  })

  it('computes one year earlier', () => {
    expect(oneYearBefore('2026-10-22')).toBe('2025-10-22')
  })
})
