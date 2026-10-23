import { useEffect, useRef } from 'react'

type Props = { claimNumber: string; email: string; onStartOver: () => void }

export function Confirmation({ claimNumber, email, onStartOver }: Props) {
  const headingRef = useRef<HTMLHeadingElement>(null)
  // Announce the new page to screen readers and keyboard users.
  useEffect(() => headingRef.current?.focus(), [])

  return (
    <section className="confirmation" aria-labelledby="confirmation-heading">
      <h1 id="confirmation-heading" tabIndex={-1} ref={headingRef}>
        We've received your claim
      </h1>
      <p className="claim-number-label">Your claim number</p>
      <p className="claim-number" data-testid="claim-number">
        {claimNumber}
      </p>
      <p>
        We're sending a confirmation email to <strong>{email}</strong>. It should arrive within a few minutes. Please
        check your inbox, and your spam folder if you don't see it.
      </p>
      <p>Keep your claim number handy. An adjuster will contact you about next steps.</p>
      <button type="button" className="secondary" onClick={onStartOver}>
        Report another claim
      </button>
    </section>
  )
}
