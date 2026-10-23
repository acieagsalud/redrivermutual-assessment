import type { ReactNode } from 'react'

type FieldProps = {
  id: string
  label: string
  hint?: string
  error?: string
  optional?: boolean
  children: (aria: { id: string; 'aria-invalid': boolean; 'aria-required': boolean; 'aria-describedby'?: string }) => ReactNode
}

/** Label, hint and error wired to the control with ids so screen readers announce them together. */
export function Field({ id, label, hint, error, optional, children }: FieldProps) {
  const hintId = hint ? `${id}-hint` : undefined
  const errorId = error ? `${id}-error` : undefined
  const describedBy = [hintId, errorId].filter(Boolean).join(' ') || undefined

  return (
    <div className={`field${error ? ' field--error' : ''}`}>
      <label htmlFor={id}>
        {label}
        {/* Visual marker only: the control itself carries aria-required. */}
        {!optional && (
          <span className="required" aria-hidden="true">
            *
          </span>
        )}
      </label>
      {hint && (
        <p id={hintId} className="hint">
          {hint}
        </p>
      )}
      {error && (
        <p id={errorId} className="error-message">
          {error}
        </p>
      )}
      {children({ id, 'aria-invalid': Boolean(error), 'aria-required': !optional, 'aria-describedby': describedBy })}
    </div>
  )
}
