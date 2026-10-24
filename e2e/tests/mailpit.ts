import { expect, type APIRequestContext } from '@playwright/test'

const MAILPIT_URL = process.env.E2E_MAILPIT_URL ?? 'http://localhost:8025'

type Message = { ID: string; To: { Address: string }[]; Subject: string }
type MessageDetail = { Text: string; HTML: string; Subject: string }

/** Waits for the consumer to deliver an email to `to`; delivery is asynchronous, so poll Mailpit's API. */
export async function waitForEmailTo(request: APIRequestContext, to: string): Promise<MessageDetail[]> {
  let messages: Message[] = []
  await expect
    .poll(
      async () => {
        const res = await request.get(`${MAILPIT_URL}/api/v1/search`, { params: { query: `to:"${to}"` } })
        messages = ((await res.json()) as { messages: Message[] }).messages
        return messages.length
      },
      { timeout: 30_000, message: `email to ${to} should arrive in Mailpit` },
    )
    .toBeGreaterThan(0)

  return Promise.all(
    messages.map(async (m) => (await (await request.get(`${MAILPIT_URL}/api/v1/message/${m.ID}`)).json()) as MessageDetail),
  )
}
