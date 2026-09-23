# Subscription API

Spring Boot service for mocked auto insurance quotations and persisted, customer-owned subscription requests.

## Customer journey

1. `POST /api/insurance/auto/quotes` validates vehicle and insured-person details, stores a private quote snapshot, and returns three mock packages when comprehensive coverage is selected (otherwise the third-party option only). Quotes expire after 24 hours. Responses set `mock: true` so the app labels the prices as indicative.
2. `GET /api/insurance/auto/quotes/{quoteId}` reloads a quote owned by the current user.
3. `POST /api/subscriptions` converts one available quote into a `REQUESTED` subscription after the user accepts the data-processing terms. A quote can only be used once.
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

Swagger UI: `/swagger-ui.html`; OpenAPI: `/v3/api-docs`. Both are public; customer endpoints require a Keycloak bearer token.

## Storage and configuration

By default the service stores data in an H2 file database under `./data`. Production deployments should mount `/data` on a persistent volume or set `DATABASE_URL` to a managed PostgreSQL JDBC URL and provide `DATABASE_USERNAME` / `DATABASE_PASSWORD`. The Kubernetes chart defaults to one replica and a persistent claim because H2 file storage is single-writer.

| Variable | Purpose |
|---|---|
| `PORT` | HTTP port, default `8080` |
| `KEYCLOAK_ISSUER_URI` | JWT issuer URL |
| `KEYCLOAK_JWK_SET_URI` | Optional explicit JWKS endpoint |
| `DISABLE_AUTH` | Local development only; `true` permits requests under a shared `local-demo` identity |
| `DATABASE_URL` | JDBC URL; default H2 file database |
| `DATABASE_USERNAME` / `DATABASE_PASSWORD` | Database credentials |
| `JPA_DDL_AUTO` | Schema policy, default `update` |

Local run:

```sh
DISABLE_AUTH=true mvn spring-boot:run
```

Docker image: `nexusbank/subscription-api`. The GitHub Actions workflows publish multi-architecture `latest` and short-SHA images for branch pushes and versioned images for releases.

## GitOps

`core-api-charts/subscription-api` packages the service as a subchart of `core-api`. The TN dev overlay deploys the public API at `subscription-api.api.dev.sandbox.internal.nbank.fr`, mounts its H2 data PVC, and configures the Keycloak issuer. The mobile app uses `EXPO_PUBLIC_SUBSCRIPTION_API_URL` including the `/api` prefix; see `.env.example` in `nexus-bank-app`.
