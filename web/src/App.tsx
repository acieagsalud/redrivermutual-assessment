import { useState } from 'react'
import { ClaimForm } from './claim/ClaimForm'
import { Confirmation } from './claim/Confirmation'
import type { SubmitResult } from './claim/api'
import type { ClaimFormValues } from './claim/types'

type Props = { submit?: (values: ClaimFormValues, idempotencyKey: string) => Promise<SubmitResult> }

export function App({ submit }: Props) {
  const [accepted, setAccepted] = useState<{ claimNumber: string; email: string } | null>(null)

  return (
    <>
      <a className="skip-link" href="#main">
        Skip to main content
      </a>
      <header className="site-header">
        <div className="site-header__inner">
          <span className="brand">Red River Mutual</span>
          <span className="brand-tag">Claims portal (assessment demo)</span>
        </div>
      </header>
      <div className="page-banner">
        <div className="page-banner__inner">
          <h1>Make a Claim</h1>
        </div>
      </div>
      <main id="main" className="container">
        {accepted ? (
          <Confirmation {...accepted} onStartOver={() => setAccepted(null)} />
        ) : (
          <ClaimForm submit={submit} onAccepted={(claimNumber, email) => setAccepted({ claimNumber, email })} />
        )}
      </main>
      <footer className="site-footer">
        <div className="site-footer__inner">Demo built for the Red River Mutual Software Developer III assessment.</div>
      </footer>
    </>
  )
}
