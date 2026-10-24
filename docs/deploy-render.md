# Free cloud deploy: Render + Neon + CloudAMQP

This deploy lives only on the `deploy/render` branch. `main` stays the local Docker Compose version.

| Piece | Free host | What runs there |
| --- | --- | --- |
| Claim form (nginx) | Render web service | `web/Dockerfile`, proxies `/api` to claims-api |
| claims-api | Render web service | `claims-api/Dockerfile` |
| notification-service | Render web service | `notification-service/Dockerfile`, `resend` profile |
| PostgreSQL | Neon | Two databases, one user each |
| RabbitMQ | CloudAMQP (Little Lemur plan) | One vhost |
| Email | Resend | Your existing account and API key |

Setup takes about 30–45 minutes. Never commit any of the values below; they go only into the Render dashboard.

## 1. Neon (PostgreSQL)

1. Sign up at neon.tech and create a project in a Canadian or US East region.
2. Under **Roles**, add two roles: `claims` and `notifications`. Save each generated password.
3. Under **Databases**, add `claims` owned by `claims`, and `notifications` owned by `notifications`.
4. Note the host from **Connection details**, for example `ep-xxx.us-east-2.aws.neon.tech`.
   The JDBC URLs are then:
   - `jdbc:postgresql://<host>/claims?sslmode=require`
   - `jdbc:postgresql://<host>/notifications?sslmode=require`

Flyway creates the tables on first start, as it does locally.

## 2. CloudAMQP (RabbitMQ)

1. Sign up at cloudamqp.com and create an instance on the free **Little Lemur** plan.
2. Open the instance's details. Note the **host**, the **user & vhost** (the same value) and the **password**.

The services connect over TLS on port 5671; `render.yaml` already sets that.

## 3. Render

1. Sign up at render.com with GitHub and give it access to this repository.
2. **New → Blueprint**, pick this repository and the `deploy/render` branch. Render reads `render.yaml` and lists three services.
3. Fill in the secret values it asks for:

| Key | rrm-claims-api | rrm-notification-service |
| --- | --- | --- |
| `DB_URL` | the `claims` JDBC URL | the `notifications` JDBC URL |
| `DB_USER` / `DB_PASSWORD` | `claims` role | `notifications` role |
| `RABBITMQ_HOST` | CloudAMQP host | same |
| `SPRING_RABBITMQ_VIRTUAL_HOST`, `RABBITMQ_USER` | CloudAMQP user & vhost | same |
| `RABBITMQ_PASSWORD` | CloudAMQP password | same |
| `RESEND_API_KEY` | not used | your Resend key |

4. For **rrm-claim-portal**, set `CLAIMS_API_URL` to the claims-api's URL shown on its Render page, for example `https://rrm-claims-api.onrender.com` (no trailing slash). You can set it after the first deploy and redeploy the portal.
5. Apply the Blueprint. The first build takes several minutes per Java service.

**Start order matters once:** open the notification-service's URL first (`…/actuator/health`) and wait for `UP`. On its first start it creates the queue and dead-letter queue in CloudAMQP. A claim published before that queue exists has nowhere to go. After that the queue is durable, so a sleeping consumer just picks up waiting messages when it wakes.

## 4. Using it

- Open the portal's Render URL and submit a claim **with your Resend account's email address**. Without a verified domain, Resend only delivers there.
- Check the Resend dashboard and your inbox, including spam.

## Free-tier limits to know before a demo

- **Services sleep after 15 minutes without traffic** and take about a minute to wake. About 5 minutes before presenting, open all three URLs: the portal, `…/actuator/health` on claims-api, and the same on notification-service.
- **A sleeping notification-service does not consume.** Claims still save; their emails wait in CloudAMQP and are sent when it wakes. This is the outbox and queue design working, and is worth saying in the demo.
- **512 MB of memory per service.** `JAVA_OPTS` caps the heap at 55% of it.
- **750 free instance hours a month** across the account, which covers occasional use but not three services running all month.
- **Metrics:** `https://<claims-api>/actuator/prometheus` is public on this deploy. That is acceptable for a demo; production would put it behind a private network or authentication.

## Going back

Nothing here changes `main`. To stop the deploy, suspend or delete the three services in Render, and the Neon project and CloudAMQP instance.
