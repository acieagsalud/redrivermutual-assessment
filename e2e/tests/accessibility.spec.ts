import AxeBuilder from '@axe-core/playwright'
import { expect, test, type Page } from '@playwright/test'

// WCAG 2.2 Level AA, plus the 2.0 and 2.1 A/AA rules it includes.
const WCAG_22_AA = ['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa', 'wcag22aa']

const axeViolations = async (page: Page) =>
  (await new AxeBuilder({ page }).withTags(WCAG_22_AA).analyze()).violations.map((v) => `${v.id}: ${v.help}`)

const todayInWinnipeg = () => new Intl.DateTimeFormat('en-CA', { timeZone: 'America/Winnipeg' }).format(new Date())

test('the form, its error state and the confirmation have no automatically detectable WCAG 2.2 AA failures', async ({ page }) => {
  await page.goto('/')
  expect(await axeViolations(page), 'empty form').toEqual([])

  await page.getByRole('button', { name: 'Submit Claim' }).click()
  await expect(page.getByRole('alert')).toBeFocused()
  expect(await axeViolations(page), 'error summary shown').toEqual([])

  await page.getByLabel(/^Insurance policy number/).fill('HO-12345678')
  await page.getByLabel(/^First name/).fill('Dana')
  await page.getByLabel(/^Last name/).fill('Smith')
  await page.getByLabel(/^Email address/).fill(`a11y+${Date.now()}@example.com`)
  await page.getByLabel(/^Street address/).fill('123 Main St')
  await page.getByLabel(/^City/).fill('Winnipeg')
  await page.getByLabel(/^Postal code/).fill('R3C 1A1')
  await page.getByLabel(/^Date of loss/).fill(todayInWinnipeg())
  await page.getByLabel(/^Type of loss/).selectOption({ index: 1 })
  await page.getByLabel(/^Tell us what happened/).fill('Basement flooded after heavy rain; carpet and drywall damaged.')
  await page.getByRole('button', { name: 'Submit Claim' }).click()

  await expect(page.getByRole('heading', { name: "We've received your claim" })).toBeFocused()
  expect(await axeViolations(page), 'confirmation').toEqual([])
})

test('the page reflows at 320 CSS pixels without horizontal scrolling (WCAG 1.4.10)', async ({ page }) => {
  await page.setViewportSize({ width: 320, height: 640 })
  await page.goto('/')
  await page.getByRole('button', { name: 'Submit Claim' }).click()

  expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(true)
})
