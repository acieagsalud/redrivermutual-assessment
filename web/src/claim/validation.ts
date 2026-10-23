import type { ClaimFormValues, FieldErrors } from './types'

/**
 * Mirrors the server's rules for fast feedback. The server remains the source of truth:
 * anything it rejects is shown on the same fields.
 */
export const DESCRIPTION_MIN = 20
export const DESCRIPTION_MAX = 2000

const EMAIL = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
const PHONE = /^\(?\d{3}\)?[ .-]?\d{3}[ .-]?\d{4}$/
const POLICY = /^HO-\d{8}$/
const POSTAL_CODE = /^[A-Za-z]\d[A-Za-z][ -]?\d[A-Za-z]\d$/

/** Today's date in Winnipeg as YYYY-MM-DD, matching the server's loss-date rule. */
export function todayInWinnipeg(now: Date = new Date()): string {
  return new Intl.DateTimeFormat('en-CA', { timeZone: 'America/Winnipeg' }).format(now)
}

export function oneYearBefore(isoDate: string): string {
  const [y, m, d] = isoDate.split('-')
  return `${Number(y) - 1}-${m}-${d}`
}

export function validate(values: ClaimFormValues, today: string = todayInWinnipeg()): FieldErrors {
  const errors: FieldErrors = {}
  const required = (field: keyof ClaimFormValues, label: string) => {
    if (!values[field].trim()) errors[field] = `Enter ${label}`
  }

  required('firstName', 'your first name')
  required('lastName', 'your last name')

  if (!values.email.trim()) errors.email = 'Enter your email address'
  else if (!EMAIL.test(values.email.trim())) errors.email = 'Enter an email address like name@example.com'

  if (values.phone.trim() && !PHONE.test(values.phone.trim())) errors.phone = 'Enter a 10-digit phone number'

  if (!values.policyNumber.trim()) errors.policyNumber = 'Enter your policy number'
  else if (!POLICY.test(values.policyNumber.trim())) errors.policyNumber = 'Policy numbers look like HO-12345678'

  if (!values.lossDate) errors.lossDate = 'Enter the date of loss'
  else if (values.lossDate > today) errors.lossDate = 'The date of loss cannot be in the future'
  else if (values.lossDate < oneYearBefore(today)) errors.lossDate = 'Claims must be reported within one year of the loss'

  if (!values.lossType) errors.lossType = 'Choose the type of loss'

  required('address.line1', 'the street address')
  required('address.city', 'the city or town')
  if (!values['address.province']) errors['address.province'] = 'Choose a province or territory'
  if (!values['address.postalCode'].trim()) errors['address.postalCode'] = 'Enter the postal code'
  else if (!POSTAL_CODE.test(values['address.postalCode'].trim()))
    errors['address.postalCode'] = 'Enter a postal code like R3C 1A1'

  const description = values.description.trim()
  if (!description) errors.description = 'Describe what happened and the damage'
  else if (description.length < DESCRIPTION_MIN)
    errors.description = `Add a little more detail (at least ${DESCRIPTION_MIN} characters)`
  else if (description.length > DESCRIPTION_MAX)
    errors.description = `Keep the description under ${DESCRIPTION_MAX} characters`

  return errors
}
