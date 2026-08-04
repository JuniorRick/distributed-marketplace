# Distributed Marketplace

This repository is intended to grow as a self-contained systems playground. Each business capability owns its backend, frontend, and database migrations.

Current systems:

- `apps/catalog`: product catalog SCS
- `apps/cart`: active cart and product snapshot SCS
- `apps/orders`: synchronous checkout and immutable order snapshot SCS

Suggested future systems:

- `apps/inventory`
- `apps/payments`
- `apps/notifications`

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

Compose builds and starts PostgreSQL, the three Spring Boot backends, and the three Nginx-served React frontends. The containers communicate through the internal Compose network, so no Java, Maven, Node.js, or npm installation is required on the host.

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

Default URLs:

- Catalog API: `http://localhost:8081/api/products`
- Catalog UI: `http://localhost:5173`
- Cart API: `http://localhost:8082/api/carts`
- Cart UI: `http://localhost:5174`
- Orders API: `http://localhost:8083/api/orders`
- Orders UI: `http://localhost:5175`

## Event-Driven Checkout

Orders reads Cart once to create an immutable snapshot. The state-changing workflow then runs asynchronously through RabbitMQ:

1. Orders commits a `PENDING` Order and `CheckoutCartCommand.v1` outbox record in one transaction.
2. The Orders outbox publisher sends the request to RabbitMQ.
3. Cart consumes the request idempotently, checks out its Cart, and commits a result outbox record.
4. Cart publishes either `CartCheckedOutEvent.v1` or `CartCheckoutRejectedEvent.v1`.
5. Orders consumes the result idempotently and transitions to `CONFIRMED` or `REJECTED`.

Outbox delivery is at-least-once. Consumer inbox tables make duplicate events harmless, and failed listener deliveries are routed to dead-letter queues. `orders.source_cart_id` also remains unique, so duplicate HTTP order requests return the existing Order.

RabbitMQ management is available at `http://localhost:15672` using `marketplace` for both username and password.

## First Practice Goals

1. Add product search and category filtering in catalog.
2. Add cart as a separate SCS that calls catalog for product snapshots.
3. Add orders as a separate SCS and keep order product snapshots immutable.
4. Introduce catalog events only after the REST flow works.

## Shared Frontend Styles

Reusable UI tokens, base styles, and layout primitives live in `packages/marketplace-ui`.

Each frontend imports the shared stylesheet before app-specific CSS:

```ts
import '@marketplace/ui/styles.css';
import './styles.css';
```

Keep cross-module styling in the shared package and keep only SCS-specific presentation in each app frontend.
