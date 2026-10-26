import { useRef, useState, type FormEvent } from 'react'
import { Field } from './Field'
import { submitClaim, type SubmitResult } from './api'
import { EMPTY_FORM, LOSS_TYPES, PROVINCES, type ClaimFormValues, type FieldErrors, type FieldName } from './types'
import { DESCRIPTION_MAX, oneYearBefore, todayInWinnipeg, validate } from './validation'

type Props = {
  onAccepted: (claimNumber: string, email: string) => void
  submit?: (values: ClaimFormValues, idempotencyKey: string) => Promise<SubmitResult>
}

const FIELD_LABELS: Record<FieldName, string> = {
  firstName: 'First name',
  lastName: 'Last name',
  email: 'Email address',
  phone: 'Phone number',
  policyNumber: 'Insurance policy number',
  lossDate: 'Date of loss',
  lossType: 'Type of loss',
  'address.line1': 'Street address',
  'address.line2': 'Apartment, unit or suite',
  'address.city': 'City',
  'address.province': 'Province',
  'address.postalCode': 'Postal code',
  description: 'Tell us what happened',
}

// On-screen order, so the error summary reads top to bottom like the form.
const FIELD_ORDER: FieldName[] = [
  'policyNumber', 'firstName', 'lastName', 'email', 'phone',
  'address.line1', 'address.line2', 'address.city', 'address.province', 'address.postalCode',
  'lossDate', 'lossType', 'description',
]

// randomUUID only exists on HTTPS or localhost; fall back to 128 random bits so plain-HTTP access still works.
const newIdempotencyKey = () =>
  crypto.randomUUID?.() ?? Array.from(crypto.getRandomValues(new Uint8Array(16)), (b) => b.toString(16).padStart(2, '0')).join('')

const fieldId = (name: FieldName) => `claim-${name.replace('.', '-')}`

export function ClaimForm({ onAccepted, submit = submitClaim }: Props) {
  const [values, setValues] = useState<ClaimFormValues>(EMPTY_FORM)
  const [errors, setErrors] = useState<FieldErrors>({})
  const [failure, setFailure] = useState<{ message: string; reference?: string } | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const summaryRef = useRef<HTMLDivElement>(null)
  // One key per claim being reported, reused on retries. "Report another claim" mounts a new form and a new key.
  const [idempotencyKey] = useState(newIdempotencyKey)

  const today = todayInWinnipeg()

  const update = (name: FieldName) => (e: { target: { value: string } }) => {
    setValues((v) => ({ ...v, [name]: e.target.value }))
    if (errors[name]) setErrors((errs) => ({ ...errs, [name]: undefined }))
  }

  const showErrors = (errs: FieldErrors) => {
    setErrors(errs)
    // Move focus to the summary so keyboard and screen-reader users learn what to fix.
    requestAnimationFrame(() => summaryRef.current?.focus())
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    if (submitting) return
    setFailure(null)

    const clientErrors = validate(values, today)
    if (Object.keys(clientErrors).length > 0) {
      showErrors(clientErrors)
      return
    }

    setSubmitting(true)
    const result = await submit(values, idempotencyKey)
    setSubmitting(false)

    if (result.kind === 'accepted') onAccepted(result.claimNumber, values.email.trim())
    else if (result.kind === 'invalid') showErrors(result.fieldErrors)
    else setFailure({ message: result.message, reference: result.reference })
  }

  const errorEntries = FIELD_ORDER.filter((name) => errors[name]).map((name) => [name, errors[name]] as const)

  const text = (name: FieldName, props: { type?: string; autoComplete?: string; hint?: string; optional?: boolean } = {}) => (
    <Field id={fieldId(name)} label={FIELD_LABELS[name]} hint={props.hint} error={errors[name]} optional={props.optional}>
      {(aria) => (
        <input {...aria} name={name} type={props.type ?? 'text'} autoComplete={props.autoComplete} value={values[name]} onChange={update(name)} />
      )}
    </Field>
  )

  return (
    <form className="claim-form" onSubmit={handleSubmit} noValidate>
      <h2 className="form-title">Online Claims Form</h2>
      <p className="lead">
        Have your policy documents handy, complete the form and select <strong>Submit Claim</strong>. We'll email you a
        claim number right away, and a claims specialist will be assigned to contact you. Your broker can also help you
        complete this form.
      </p>
      <p className="required-note">
        Fields marked with an asterisk (<span aria-hidden="true">*</span>
        <span className="visually-hidden">star</span>) are required.
      </p>

      {errorEntries.length > 0 && (
        <div className="error-summary" role="alert" tabIndex={-1} ref={summaryRef}>
          <h2>Please fix the following</h2>
          <ul>
            {errorEntries.map(([name, msg]) => (
              <li key={name}>
                <a href={`#${fieldId(name)}`}>{msg}</a>
              </li>
            ))}
          </ul>
        </div>
      )}

      {failure && (
        <div className="banner banner--error" role="alert">
          <p>{failure.message}</p>
          {failure.reference && <p className="reference">Reference: {failure.reference}</p>}
        </div>
      )}

      <fieldset>
        <legend>Policyholder</legend>
        {text('policyNumber', { hint: 'Find this on your policy documents, for example HO-12345678.' })}
        <div className="row">
          {text('firstName', { autoComplete: 'given-name' })}
          {text('lastName', { autoComplete: 'family-name' })}
        </div>
        <div className="row">
          {text('email', { type: 'email', autoComplete: 'email', hint: "We'll send your claim confirmation here." })}
          {text('phone', { type: 'tel', autoComplete: 'tel', optional: true, hint: 'Optional. The best number to reach you.' })}
        </div>
      </fieldset>

      <fieldset>
        <legend>Location of loss</legend>
        {text('address.line1', { autoComplete: 'address-line1' })}
        {text('address.line2', { autoComplete: 'address-line2', optional: true })}
        <div className="row row--three">
          {text('address.city', { autoComplete: 'address-level2' })}
          <Field id={fieldId('address.province')} label={FIELD_LABELS['address.province']} error={errors['address.province']}>
            {(aria) => (
              <select {...aria} name="address.province" autoComplete="address-level1" value={values['address.province']} onChange={update('address.province')}>
                {PROVINCES.map((p) => (
                  <option key={p.value} value={p.value}>
                    {p.label}
                  </option>
                ))}
              </select>
            )}
          </Field>
          {text('address.postalCode', { autoComplete: 'postal-code' })}
        </div>
      </fieldset>

      <fieldset>
        <legend>About the loss</legend>
        <div className="row">
          <Field id={fieldId('lossDate')} label={FIELD_LABELS.lossDate} error={errors.lossDate}>
            {(aria) => (
              <input {...aria} name="lossDate" type="date" min={oneYearBefore(today)} max={today} value={values.lossDate} onChange={update('lossDate')} />
            )}
          </Field>
          <Field id={fieldId('lossType')} label={FIELD_LABELS.lossType} error={errors.lossType}>
            {(aria) => (
              <select {...aria} name="lossType" value={values.lossType} onChange={update('lossType')}>
                <option value="">Select one</option>
                {LOSS_TYPES.map((t) => (
                  <option key={t.value} value={t.value}>
                    {t.label}
                  </option>
                ))}
              </select>
            )}
          </Field>
        </div>
        <Field
          id={fieldId('description')}
          label={FIELD_LABELS.description}
          hint="Include the cause of loss and the extent of damage."
          error={errors.description}
        >
          {(aria) => (
            <textarea {...aria} name="description" rows={6} maxLength={DESCRIPTION_MAX} value={values.description} onChange={update('description')} />
          )}
        </Field>
        <p className="counter" aria-hidden="true">
          {values.description.length} / {DESCRIPTION_MAX}
        </p>
        {/* Announced only near the limit; announcing every keystroke would drown out what the user types. */}
        <p className="visually-hidden" aria-live="polite">
          {DESCRIPTION_MAX - values.description.length <= 100 ? `${DESCRIPTION_MAX - values.description.length} characters left` : ''}
        </p>
      </fieldset>

      <button type="submit" className="primary" disabled={submitting} aria-disabled={submitting}>
        {submitting ? 'Submitting…' : 'Submit Claim'}
      </button>
    </form>
  )
}
