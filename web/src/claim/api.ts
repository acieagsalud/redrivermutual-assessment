import type { ClaimFormValues, FieldErrors, FieldName } from './types'

export type SubmitResult =
  | { kind: 'accepted'; claimNumber: string }
  | { kind: 'invalid'; fieldErrors: FieldErrors }
  | { kind: 'failed'; message: string; reference?: string }

const GENERIC_FAILURE = 'We could not submit your claim right now. Please try again in a few minutes.'

function toRequestBody(v: ClaimFormValues) {
  const blankToNull = (s: string) => (s.trim() ? s.trim() : null)
  return {
    firstName: v.firstName.trim(),
    lastName: v.lastName.trim(),
    email: v.email.trim(),
    phone: blankToNull(v.phone),
    policyNumber: v.policyNumber.trim().toUpperCase(),
    lossDate: v.lossDate,
    lossType: v.lossType,
    address: {
      line1: v['address.line1'].trim(),
      line2: blankToNull(v['address.line2']),
      city: v['address.city'].trim(),
      province: v['address.province'],
      postalCode: v['address.postalCode'].trim(),
    },
    description: v.description.trim(),
  }
}

export async function submitClaim(values: ClaimFormValues, fetchImpl: typeof fetch = fetch): Promise<SubmitResult> {
  let response: Response
  try {
    response = await fetchImpl('/api/v1/claims', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
      body: JSON.stringify(toRequestBody(values)),
    })
  } catch {
    return { kind: 'failed', message: GENERIC_FAILURE }
  }

  const body = await response.json().catch(() => ({}))

  if (response.status === 201) {
    return { kind: 'accepted', claimNumber: body.claimNumber }
  }
  if (response.status === 400 && Array.isArray(body.errors)) {
    const fieldErrors: FieldErrors = {}
    for (const e of body.errors as { field: string; message: string }[]) {
      fieldErrors[e.field as FieldName] = e.message
    }
    return { kind: 'invalid', fieldErrors }
  }
  return { kind: 'failed', message: GENERIC_FAILURE, reference: body.correlationId }
}
