import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { App } from './App'
import type { SubmitResult } from './claim/api'
import { todayInWinnipeg } from './claim/validation'

async function fillValidForm(user: ReturnType<typeof userEvent.setup>) {
  await user.type(screen.getByLabelText(/^First name/), 'Dana')
  await user.type(screen.getByLabelText(/^Last name/), 'Smith')
  await user.type(screen.getByLabelText(/^Email address/), 'dana@example.com')
  await user.type(screen.getByLabelText(/^Insurance policy number/), 'HO-12345678')
  await user.type(screen.getByLabelText(/^Date of loss/), todayInWinnipeg())
  await user.selectOptions(screen.getByLabelText(/^Type of loss/), 'SEWER_BACKUP')
  await user.type(screen.getByLabelText(/^Street address/), '123 Main St')
  await user.type(screen.getByLabelText(/^City/), 'Winnipeg')
  await user.type(screen.getByLabelText(/^Postal code/), 'R3C 1A1')
  await user.type(screen.getByLabelText(/^Tell us what happened/), 'Basement flooded after heavy rain; carpet damaged.')
}

describe('claim submission', () => {
  it('shows the claim number and tells the customer to check their email', async () => {
    const user = userEvent.setup()
    const submit = vi.fn<() => Promise<SubmitResult>>().mockResolvedValue({ kind: 'accepted', claimNumber: 'CLM-7K4Q-9XHM' })
    render(<App submit={submit} />)

    await fillValidForm(user)
    await user.click(screen.getByRole('button', { name: 'Submit Claim' }))

    expect(await screen.findByRole('heading', { name: "We've received your claim" })).toHaveFocus()
    expect(screen.getByTestId('claim-number')).toHaveTextContent('CLM-7K4Q-9XHM')
    expect(screen.getByText(/confirmation email to/)).toHaveTextContent('dana@example.com')
    expect(submit).toHaveBeenCalledTimes(1)
  })

  it('does not call the API when the form is invalid, and summarises the problems', async () => {
    const user = userEvent.setup()
    const submit = vi.fn<() => Promise<SubmitResult>>()
    render(<App submit={submit} />)

    await user.click(screen.getByRole('button', { name: 'Submit Claim' }))

    expect(screen.getByRole('alert')).toHaveTextContent('Please fix the following')
    expect(screen.getByLabelText(/^First name/)).toHaveAttribute('aria-invalid', 'true')
    expect(screen.getByLabelText(/^First name/)).toHaveAccessibleDescription('Enter your first name')
    expect(submit).not.toHaveBeenCalled()
  })

  it('marks required fields for assistive tech, not just with the visual asterisk', () => {
    render(<App />)

    expect(screen.getByLabelText(/^First name/)).toHaveAttribute('aria-required', 'true')
    expect(screen.getByLabelText(/^Phone number/)).toHaveAttribute('aria-required', 'false')
  })

  it('shows server-side validation errors on the matching fields', async () => {
    const user = userEvent.setup()
    const submit = vi.fn<() => Promise<SubmitResult>>().mockResolvedValue({
      kind: 'invalid',
      fieldErrors: { 'address.postalCode': 'must be a Canadian postal code like R3C 1A1' },
    })
    render(<App submit={submit} />)

    await fillValidForm(user)
    await user.click(screen.getByRole('button', { name: 'Submit Claim' }))

    expect(await screen.findByLabelText(/^Postal code/)).toHaveAccessibleDescription('must be a Canadian postal code like R3C 1A1')
  })

  it('shows a retry message and reference when the server fails', async () => {
    const user = userEvent.setup()
    const submit = vi.fn<() => Promise<SubmitResult>>().mockResolvedValue({
      kind: 'failed',
      message: 'We could not submit your claim right now. Please try again in a few minutes.',
      reference: 'abc-123',
    })
    render(<App submit={submit} />)

    await fillValidForm(user)
    await user.click(screen.getByRole('button', { name: 'Submit Claim' }))

    expect(await screen.findByText(/try again in a few minutes/)).toBeInTheDocument()
    expect(screen.getByText('Reference: abc-123')).toBeInTheDocument()
    // Shown right above the button the customer just pressed, not at the top of a long form.
    expect(screen.getByText('Reference: abc-123').closest('[role="alert"]')?.nextElementSibling).toBe(
      screen.getByRole('button', { name: 'Submit Claim' }),
    )
    expect(screen.queryByTestId('claim-number')).not.toBeInTheDocument()
  })

  it('retries with the same idempotency key so a lost response cannot create a second claim', async () => {
    const user = userEvent.setup()
    const submit = vi.fn<(values: unknown, key: string) => Promise<SubmitResult>>()
      .mockResolvedValueOnce({ kind: 'failed', message: 'We could not submit your claim right now. Please try again in a few minutes.' })
      .mockResolvedValueOnce({ kind: 'accepted', claimNumber: 'CLM-7K4Q-9XHM' })
    render(<App submit={submit} />)

    await fillValidForm(user)
    await user.click(screen.getByRole('button', { name: 'Submit Claim' }))
    await screen.findByText(/try again in a few minutes/)
    await user.click(screen.getByRole('button', { name: 'Submit Claim' }))

    await screen.findByTestId('claim-number')
    const [first, second] = submit.mock.calls.map(([, key]) => key)
    expect(first).toMatch(/^[0-9a-f-]{36}$/)
    expect(second).toBe(first)
  })
})
