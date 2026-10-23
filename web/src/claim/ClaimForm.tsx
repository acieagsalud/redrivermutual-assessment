import { useRef, useState, type FormEvent } from 'react'
import { Field } from './Field'
import { submitClaim, type SubmitResult } from './api'
import { EMPTY_FORM, LOSS_TYPES, PROVINCES, type ClaimFormValues, type FieldErrors, type FieldName } from './types'
import { DESCRIPTION_MAX, oneYearBefore, todayInWinnipeg, validate } from './validation'

type Props = {
  onAccepted: (claimNumber: string, email: string) => void
  submit?: (values: ClaimFormValues) => Promise<SubmitResult>
}

const FIELD_LABELS: Record<FieldName, string> = {
  firstName: 'First name',
  lastName: 'Last name',
  email: 'Email address',
  phone: 'Phone number',
  policyNumber: 'Policy number',
  lossDate: 'Date of loss',
  lossType: 'Type of loss',
  'address.line1': 'Street address',
  'address.line2': 'Apartment, unit or suite',
  'address.city': 'City or town',
  'address.province': 'Province or territory',
  'address.postalCode': 'Postal code',
  description: 'What happened?',
}

const fieldId = (name: FieldName) => `claim-${name.replace('.', '-')}`

export function ClaimForm({ onAccepted, submit = submitClaim }: Props) {
  const [values, setValues] = useState<ClaimFormValues>(EMPTY_FORM)
  const [errors, setErrors] = useState<FieldErrors>({})
  const [failure, setFailure] = useState<{ message: string; reference?: string } | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const summaryRef = useRef<HTMLDivElement>(null)

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
    const result = await submit(values)
    setSubmitting(false)

    if (result.kind === 'accepted') onAccepted(result.claimNumber, values.email.trim())
    else if (result.kind === 'invalid') showErrors(result.fieldErrors)
    else setFailure({ message: result.message, reference: result.reference })
  }

  const errorEntries = (Object.entries(errors) as [FieldName, string | undefined][]).filter(([, msg]) => msg)

  const text = (name: FieldName, props: { type?: string; autoComplete?: string; hint?: string; optional?: boolean } = {}) => (
    <Field id={fieldId(name)} label={FIELD_LABELS[name]} hint={props.hint} error={errors[name]} optional={props.optional}>
      {(aria) => (
        <input {...aria} name={name} type={props.type ?? 'text'} autoComplete={props.autoComplete} value={values[name]} onChange={update(name)} />
      )}
    </Field>
  )

  return (
    <form className="claim-form" onSubmit={handleSubmit} noValidate>
      <h1>Report a home insurance claim</h1>
      <p className="lead">It takes about 5 minutes. We'll email you a claim number as soon as it's submitted.</p>

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
        <legend>Your contact details</legend>
        <div className="row">
          {text('firstName', { autoComplete: 'given-name' })}
          {text('lastName', { autoComplete: 'family-name' })}
        </div>
        {text('email', { type: 'email', autoComplete: 'email', hint: "We'll send your claim confirmation here." })}
        {text('phone', { type: 'tel', autoComplete: 'tel', optional: true })}
      </fieldset>

      <fieldset>
        <legend>Your policy and the loss</legend>
        {text('policyNumber', { hint: 'Find this on your policy documents, for example HO-12345678.' })}
        <div className="row">
          <Field id={fieldId('lossDate')} label={FIELD_LABELS.lossDate} error={errors.lossDate}>
            {(aria) => (
              <input {...aria} name="lossDate" type="date" min={oneYearBefore(today)} max={today} value={values.lossDate} onChange={update('lossDate')} />
            )}
          </Field>
          <Field id={fieldId('lossType')} label={FIELD_LABELS.lossType} error={errors.lossType}>
            {(aria) => (
              <select {...aria} name="lossType" value={values.lossType} onChange={update('lossType')}>
                <option value="">Choose one</option>
                {LOSS_TYPES.map((t) => (
                  <option key={t.value} value={t.value}>
                    {t.label}
                  </option>
                ))}
              </select>
            )}
          </Field>
        </div>
      </fieldset>

      <fieldset>
        <legend>Address of the damaged property</legend>
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
        <legend>Damage</legend>
        <Field
          id={fieldId('description')}
          label={FIELD_LABELS.description}
          hint="Tell us what happened, when you noticed it, and what was damaged."
          error={errors.description}
        >
          {(aria) => (
            <textarea {...aria} name="description" rows={6} maxLength={DESCRIPTION_MAX} value={values.description} onChange={update('description')} />
          )}
        </Field>
        <p className="counter" aria-live="polite">
          {values.description.length} / {DESCRIPTION_MAX}
        </p>
      </fieldset>

      <button type="submit" className="primary" disabled={submitting} aria-disabled={submitting}>
        {submitting ? 'Submitting…' : 'Submit claim'}
      </button>
    </form>
  )
}
