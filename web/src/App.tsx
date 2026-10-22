import { useState } from 'react'
import { ClaimForm } from './claim/ClaimForm'
import { Confirmation } from './claim/Confirmation'
import type { SubmitResult } from './claim/api'
import type { ClaimFormValues } from './claim/types'

type Props = { submit?: (values: ClaimFormValues) => Promise<SubmitResult> }

export function App({ submit }: Props) {
  const [accepted, setAccepted] = useState<{ claimNumber: string; email: string } | null>(null)

  return (
    <>
      <header className="site-header">
        <span className="brand">Home Claims</span>
      </header>
      <main className="container">
        {accepted ? (
          <Confirmation {...accepted} onStartOver={() => setAccepted(null)} />
        ) : (
          <ClaimForm submit={submit} onAccepted={(claimNumber, email) => setAccepted({ claimNumber, email })} />
        )}
      </main>
    </>
  )
}
