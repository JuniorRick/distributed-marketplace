# Distributed Marketplace

This repository is intended to grow as a self-contained systems playground. Each business capability owns its backend, frontend, and database migrations.

Current systems:

- `apps/catalog`: product catalog SCS
- `apps/cart`: active cart and product snapshot SCS
- `apps/orders`: checkout orchestration and immutable order snapshot SCS
- `apps/inventory`: stock balances and order reservation SCS
- `apps/payments`: payment capture and payment history SCS
- `apps/notifications`: order lifecycle notification SCS
- `apps/customers`: customer contact and notification preference SCS
- `apps/gateway`: authenticated edge routing for browser traffic

## Catalog System

The catalog owns product identity, names, descriptions, prices, and catalog visibility. Other systems should treat catalog data as external data and either call the catalog API or consume future catalog events.

Owned data:

- products
- product price display data
- product publication status

Not owned by catalog:

- inventory quantities
- cart items
- orders
- payment state

## Run Locally

Start the complete marketplace from the repository root:

```shell
docker compose up --build
```

Compose builds and starts PostgreSQL, Keycloak with its own PostgreSQL database, the marketplace backends, the API gateway, and the three Nginx-served React frontends. The containers communicate through the internal Compose network, so no Java, Maven, Node.js, or npm installation is required on the host.

Stop the stack while preserving PostgreSQL data:

```shell
docker compose down
```

Stop the stack and delete its PostgreSQL volume:

```shell
docker compose down --volumes
```

Rebuild one service after changing its dependencies:

```shell
docker compose up --build catalog-backend
```

Follow logs for the complete stack:


```shell
docker compose logs --follow
```

restart container, e.g. restart otel-collector
```shell
docker-compose up -d --force-recreate otel-collector
```


Default URLs:

- Marketplace entry point: `http://localhost:8080`
- Catalog UI: `http://localhost:8080/catalog/`
- Cart UI: `http://localhost:8080/cart/`
- Orders UI: `http://localhost:8080/orders/`
- Catalog API through gateway: `http://localhost:8080/api/catalog/products`
- Cart API through gateway: `http://localhost:8080/api/carts`
- Orders API through gateway: `http://localhost:8080/api/orders`
- Current customer API through gateway: `http://localhost:8080/api/customers/me`
- Keycloak: `http://localhost:8090`
- Inventory health: `http://localhost:8084/actuator/health`
- Inventory replenishment: `POST http://localhost:8084/api/inventory/{productId}/replenishments`
- Payments health: `http://localhost:8085/actuator/health`
- Payment by order: `http://localhost:8085/api/payments/by-order/{orderId}`
- Notifications health: `http://localhost:8086/actuator/health`
- Customer notifications: `http://localhost:8086/api/notifications/by-customer/{customerId}`
- Customers API: `http://localhost:8087/api/customers`

## Authentication

Keycloak owns credentials and authentication. The React applications use OpenID Connect Authorization Code flow
with PKCE through the public `marketplace-spa` client. Access and refresh tokens stay in memory and are not written
to browser storage.

After authentication, the frontend provisions `POST /api/customers/me`. Customers uses the validated JWT `sub`
claim as the customer UUID and publishes contact and preference events for Notifications. Cart ignores client-supplied
customer identifiers while security is enabled, and Cart and Orders enforce resource ownership against the same `sub`.
The gateway validates JWTs and forwards them downstream, where protected services validate them again.

Self-registration is enabled in the local realm. The local Keycloak administration credentials are `admin` / `admin`;
they are development credentials and must be replaced outside this local Compose environment.

Keycloak owns authentication and token issuance. The gateway validates tokens and applies route-level access rules,
while Cart, Orders, and Customers validate tokens again and enforce customer ownership using the JWT `sub` claim.
Business authorization must remain in the owning service because the gateway does not own carts, orders, or customer data.

Keycloak uses a dedicated `keycloak` PostgreSQL database and the `marketplace-keycloak-db-data` volume. Realm JSON is
only bootstrap configuration: `--import-realm` skips the import after the realm exists. Apply later realm/client changes
through Keycloak administration or a repeatable identity-configuration deployment, not by editing the JSON and restarting.

### Production identity configuration

The production Compose overlay removes direct host ports from databases, brokers, backends, frontends, and observability
services. Only the gateway and Keycloak retain their base published ports so an external TLS reverse proxy can route to
them. Copy the variable names from `.env.production.example` into your deployment secret store and inject their values;
do not commit a populated `.env.production` file.

Validate the merged configuration:

```shell
docker compose --env-file .env.production \
  -f compose.yaml -f compose.production.yaml config --quiet
```

Build and start it:

```shell
docker compose --env-file .env.production \
  -f compose.yaml -f compose.production.yaml up -d --build
```

`KEYCLOAK_PUBLIC_URL` and `MARKETPLACE_PUBLIC_URL` must be public HTTPS URLs. The reverse proxy must overwrite and pass
the `X-Forwarded-*` headers because Keycloak is configured with `KC_PROXY_HEADERS=xforwarded`. The production realm
allows redirects only to `MARKETPLACE_PUBLIC_URL`, requires PKCE, disables implicit and password grants, enables brute-force
protection, and applies a stronger password policy. Configure SMTP and then enable email verification before accepting
real customer registrations.

This overlay is a hardened single-host reference, not a high-availability production topology. A real deployment should
use a managed or highly available PostgreSQL database, at least two Keycloak instances, encrypted backups, TLS at the
ingress, restricted administration access, and credentials supplied by the platform's secret manager. Treat the bootstrap
administrator as an installation account and manage named administrator accounts through normal Keycloak administration.

## Event-Driven Checkout

Orders reads Cart once to create an immutable snapshot. The state-changing workflow then runs asynchronously through RabbitMQ:

1. Orders commits a `CHECKOUT_PENDING` Order and `CheckoutCartCommand.v1` outbox record in one transaction.
2. The Orders outbox publisher sends the request to RabbitMQ.
3. Cart consumes the request idempotently, checks out its Cart, and commits a result outbox record.
4. Cart publishes either `CartCheckedOutEvent.v1` or `CartCheckoutRejectedEvent.v1`.
5. When Cart succeeds, Orders commits a `ReserveInventoryCommand.v1` outbox record containing the immutable order lines.
6. Inventory consumes the command idempotently and reserves every requested item in one transaction.
7. Inventory publishes either `InventoryReservedEvent.v1` or `InventoryReservationRejectedEvent.v1` through its outbox.
8. When Inventory succeeds, Orders commits a `CapturePaymentCommand.v1` outbox record with the immutable order total.
9. Payments consumes the command idempotently and invokes its gateway adapter.
10. Payments records a `CAPTURED` or `FAILED` payment and the matching result event in one transaction.
11. For a captured payment, Orders transitions to `INVENTORY_COMMIT_PENDING` and publishes `CommitInventoryCommand.v1`.
12. Inventory commits the reservation, publishes `InventoryCommittedEvent.v1`, and Orders transitions to `CONFIRMED`.
13. For a failed payment, Orders transitions to `INVENTORY_RELEASE_PENDING` and publishes `ReleaseInventoryCommand.v1`.
14. Inventory releases the reservation, publishes `InventoryReleasedEvent.v1`, and Orders transitions to `REJECTED`.

If inventory commit remains unacknowledged after the configured reconciliation attempts, Orders first requests an
inventory release. A confirmed release moves the order to `REFUND_PENDING` and emits `RefundPaymentCommand.v1`.
Payments uses the payment ID as the gateway idempotency key and publishes either `PaymentRefundedEvent.v1` or
`PaymentRefundFailedEvent.v1`. A successful refund finishes the order as `REFUNDED`; exhausted release or refund
retries move it to `MANUAL_REVIEW`.

Orders publishes confirmed, rejected, and refunded lifecycle events from the same transaction that completes the
state transition. Notifications consumes those events through one durable queue, records them idempotently, and
delivers them asynchronously through a simulated sender. Customers publishes versioned contact and preference
events through its transactional outbox. Notifications maintains a local, idempotent recipient projection and uses
that projection for delivery without synchronously calling Customers.

The Orders reconciliation scheduler scans stale non-terminal phases, republishes their commands, and increments
`reconciliation_attempts`. Its defaults are a one-minute stale threshold, a 30-second scan interval, and five
attempts. Configure them with `SAGA_RECONCILIATION_STALE_AFTER`, `SAGA_RECONCILIATION_FIXED_DELAY`, and
`SAGA_RECONCILIATION_MAX_ATTEMPTS`.

The local gateway simulator captures payments by default. Start the stack with
`PAYMENT_SIMULATOR_OUTCOME=FAILED` to exercise payment rejection.

Add stock before exercising checkout:

```shell
curl -X POST http://localhost:8084/api/inventory/PRODUCT_UUID/replenishments \
  -H "Content-Type: application/json" \
  -d '{"quantity":20}'
```

Outbox delivery is at-least-once. Consumer inbox tables make duplicate events harmless, and failed listener deliveries are routed to dead-letter queues. `orders.source_cart_id` also remains unique, so duplicate HTTP order requests return the existing Order.

RabbitMQ management is available at `http://localhost:15672` using `marketplace` for both username and password.

## End-to-End Tests

The `marketplace-e2e` Maven module owns an isolated Docker Compose environment. It allocates dynamic host ports,
builds and starts the seven backends and their infrastructure, runs the checkout scenarios, and removes the containers
and volumes afterward.

Saga E2E tests explicitly disable HTTP security for Customers, Cart, and Orders. Authentication and authorization are
tested separately so the checkout suite can concentrate on messaging, persistence, and compensation behavior.

Run the successful-checkout scenario from the repository root:

```shell
mvn --projects marketplace-e2e verify
```

Docker with either `docker compose` or `docker-compose` must be available. Set `E2E_KEEP_ENVIRONMENT=true` to keep
the environment running after a test for investigation. The Compose project name and allocated API ports are printed
at startup.

## Shared Frontend Styles

Reusable UI tokens, base styles, and layout primitives live in `packages/marketplace-ui`.

Each frontend imports the shared stylesheet before app-specific CSS:

```ts
import '@marketplace/ui/styles.css';
import './styles.css';
```
