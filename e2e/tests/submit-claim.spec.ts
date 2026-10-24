import { expect, test } from '@playwright/test'
import { waitForEmailTo } from './mailpit'

const todayInWinnipeg = () => new Intl.DateTimeFormat('en-CA', { timeZone: 'America/Winnipeg' }).format(new Date())

test('a submitted claim gets a claim number on screen and exactly one confirmation email', async ({ page, request }) => {
  // Unique per run, so the Mailpit search only matches this test's email.
  const email = `e2e+${Date.now()}@example.com`

  await page.goto('/')
  await page.getByLabel(/^Insurance policy number/).fill('HO-12345678')
  await page.getByLabel(/^First name/).fill('Dana')
  await page.getByLabel(/^Last name/).fill('Smith')
  await page.getByLabel(/^Email address/).fill(email)
  await page.getByLabel(/^Street address/).fill('123 Main St')
  await page.getByLabel(/^City/).fill('Winnipeg')
  await page.getByLabel(/^Postal code/).fill('r3c1a1')
  await page.getByLabel(/^Date of loss/).fill(todayInWinnipeg())
  await page.getByLabel(/^Type of loss/).selectOption({ index: 1 })
  await page.getByLabel(/^Tell us what happened/).fill('Basement flooded after heavy rain; carpet and drywall damaged.')
  await page.getByRole('button', { name: 'Submit Claim' }).click()

  await expect(page.getByRole('heading', { name: "We've received your claim" })).toBeVisible()
  const claimNumber = (await page.getByTestId('claim-number').textContent())?.trim() ?? ''
  expect(claimNumber).toMatch(/^CLM-[2-9A-HJKMNP-TV-Z]{4}-[2-9A-HJKMNP-TV-Z]{4}$/)

  // The email is sent by the separate notification service after the event crosses RabbitMQ.
  const emails = await waitForEmailTo(request, email)
  expect(emails).toHaveLength(1)
  expect(emails[0].Text).toContain(claimNumber)
})

test('an incomplete form is stopped in the browser with a summary of what to fix', async ({ page }) => {
  let apiCalled = false
  page.on('request', (req) => {
    if (req.url().includes('/api/v1/claims')) apiCalled = true
  })

  await page.goto('/')
  await page.getByRole('button', { name: 'Submit Claim' }).click()

  const summary = page.getByRole('alert')
  await expect(summary).toContainText('Please fix the following')
  await expect(summary).toBeFocused()
  await expect(page.getByLabel(/^Insurance policy number/)).toHaveAttribute('aria-invalid', 'true')
  expect(apiCalled).toBe(false)
})

test('the API rejects invalid data even if the browser checks are bypassed', async ({ request }) => {
  const res = await request.post('/api/v1/claims', {
    headers: { 'X-Correlation-Id': 'e2e-invalid-1' },
    data: { firstName: '', email: 'not-an-email' },
  })

  expect(res.status()).toBe(400)
  const problem = await res.json()
  expect(problem.correlationId).toBe('e2e-invalid-1')
  expect(problem.errors.map((e: { field: string }) => e.field)).toEqual(expect.arrayContaining(['firstName', 'email']))
})
