# Subscription API

Spring Boot service for mocked auto insurance quotations and persisted, customer-owned subscription requests.

## Customer journey

1. `POST /api/insurance/auto/quotes` validates vehicle and insured-person details, stores a private quote snapshot, and returns selectable mock packages (the third-party package is always available; comprehensive coverage also returns comfort and all-risk packages). Quotes expire after 24 hours. Responses set `mock: true` so the app labels the prices as indicative.
2. `GET /api/insurance/auto/quotes/{quoteId}` reloads a quote owned by the current user.
3. `POST /api/subscriptions` converts one available quote into a `REQUESTED` subscription after the user accepts the data-processing terms. The selected offer language (`en`, `fr`, or `ar`) is stored with the user's subscription. A quote can only be used once.
4. `GET /api/subscriptions` and `GET /api/subscriptions/{id}` show only records belonging to the JWT subject. `POST /api/subscriptions/{id}/cancel` cancels a pending request. A request cannot be cancelled after it becomes active.

The mock quote is not an insurance contract. A `REQUESTED` record represents an application awaiting partner processing; it does not charge the customer or activate a policy.

## API example

```http
POST /api/insurance/auto/quotes
Authorization: Bearer <customer-jwt>
Content-Type: application/json

{
  "registrationNumber": "123TUN4567",
  "make": "Renault",
  "model": "Clio",
  "firstRegistrationYear": 2021,
  "horsepower": 6,
  "insuredName": "Example Customer",
  "nationalId": "01234567",
  "phone": "20123456",
  "email": "customer@example.test",
  "coverage": "comprehensive",
  "startDate": "2026-10-01"
}
```

Create a subscription by choosing one returned package:

```json
{
  "quoteId": "<quote-id>",
  "planCode": "CONFORT",
  "termsAccepted": true,
  "language": "en"
}
```

Swagger UI: `/swagger-ui.html`; OpenAPI: `/v3/api-docs`. Both are public; customer endpoints require a bearer token issued by the `customer` realm. The resource-server decoder accepts RS256 and RS512 signatures and validates the configured issuer and JWKS endpoint. `SUBSCRIPTION_JWKS_TLS_VERIFY=false` is available only for development environments with a private IAM certificate; it relaxes TLS checks for JWKS retrieval only.

## Storage and configuration

For a local `mvn spring-boot:run`, the service uses an H2 file database under `./data`. The Kubernetes Helm chart runs the official `postgres:16-alpine` image as a single-replica StatefulSet with a persistent claim, then injects the PostgreSQL JDBC URL and credentials into the API. The database password is generated into a Kubernetes Secret and retained across Helm upgrades; you can provide `postgresql.auth.existingSecret` to use a separately managed Secret with a `password` key.

The chart keeps the existing H2 `/data` claim during the transition so an upgrade does not delete that volume, but the API no longer connects to it. H2 records are not automatically copied to PostgreSQL. Export/import existing records separately before removing the old claim.

| Variable | Purpose |
|---|---|
| `PORT` | HTTP port, default `8080` |
| `KEYCLOAK_ISSUER_URI` | JWT issuer URL |
| `KEYCLOAK_JWK_SET_URI` | Optional explicit JWKS endpoint |
| `SUBSCRIPTION_JWKS_TLS_VERIFY` | Verify the JWKS TLS certificate (default `true`; set `false` only for development with a private certificate) |
| `DISABLE_AUTH` | Local development only; `true` permits requests under a shared `local-demo` identity |
| `DATABASE_URL` | JDBC URL; local default is H2. Helm sets a PostgreSQL URL when its database is enabled. |
| `DATABASE_USERNAME` / `DATABASE_PASSWORD` | Database credentials |
| `JPA_DDL_AUTO` | Schema policy, default `update` |

Local run:

```sh
DISABLE_AUTH=true mvn spring-boot:run
```

Docker image: `nexusbank/subscription-api`. The GitHub Actions workflows publish multi-architecture `latest` and short-SHA images for branch pushes and versioned images for releases.

## GitOps

`core-api-charts/subscription-api` packages the service as a subchart of `core-api`. The TN dev overlay deploys the public API at `subscription-api.api.dev.sandbox.internal.nbank.fr` and PostgreSQL in a persistent StatefulSet, and configures the Keycloak issuer. The mobile app uses `EXPO_PUBLIC_SUBSCRIPTION_API_URL` including the `/api` prefix; see `.env.example` in `nexus-bank-app`.
