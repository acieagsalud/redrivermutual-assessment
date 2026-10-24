import { readFileSync } from 'node:fs'
import { expect, test, type APIRequestContext } from '@playwright/test'

const PROMETHEUS_URL = process.env.E2E_PROMETHEUS_URL ?? 'http://localhost:9090'
const GRAFANA_URL = process.env.E2E_GRAFANA_URL ?? 'http://localhost:3000'

type Dashboard = { panels: { title: string; targets: { expr: string }[] }[] }
const dashboard: Dashboard = JSON.parse(
  readFileSync(new URL('../../infra/grafana/dashboards/claim-portal.json', import.meta.url), 'utf8'),
)

type QueryResult = { status: string; data: { result: { metric: Record<string, string>; value: [number, string] }[] } }

async function query(request: APIRequestContext, expr: string): Promise<QueryResult> {
  const res = await request.get(`${PROMETHEUS_URL}/api/v1/query`, { params: { query: expr } })
  return (await res.json()) as QueryResult
}

test('Prometheus scrapes both services', async ({ request }) => {
  await expect
    .poll(
      async () => {
        const { data } = await query(request, 'up{job=~"claims-api|notification-service"}')
        return Object.fromEntries(data.result.map((r) => [r.metric.job, r.value[1]]))
      },
      { timeout: 30_000, message: 'both services should be scraped successfully' },
    )
    .toEqual({ 'claims-api': '1', 'notification-service': '1' })
})

test('every Grafana dashboard query returns data', async ({ request }) => {
  // Rate queries need a couple of scrapes after the first request, so allow for several scrape intervals.
  test.setTimeout(90_000)
  // Ensures the claims endpoint has HTTP metrics to chart even if this test runs first.
  await request.post('/api/v1/claims', { data: {} })

  // A metric renamed in code but not on the dashboard would leave its panel empty; this catches that.
  for (const panel of dashboard.panels) {
    for (const { expr } of panel.targets) {
      const promql = expr.replaceAll('$__range', '15m')
      await expect
        .poll(
          async () => {
            const result = await query(request, promql)
            return result.status === 'success' ? result.data.result.length : -1
          },
          { timeout: 30_000, message: `panel "${panel.title}" should return data for: ${promql}` },
        )
        .toBeGreaterThan(0)
    }
  }
})

test('Grafana serves the provisioned dashboard and can reach Prometheus', async ({ request }) => {
  const dash = await request.get(`${GRAFANA_URL}/api/dashboards/uid/claim-portal`)
  expect(dash.ok()).toBe(true)
  expect((await dash.json()).dashboard.title).toBe('Claim Portal')

  const health = await request.get(`${GRAFANA_URL}/api/datasources/uid/prometheus/health`)
  expect((await health.json()).status).toBe('OK')
})
