# Distributed Marketplace

This repository is intended to grow as a self-contained systems playground. Each business capability owns its backend, frontend, and database migrations.

Current systems:

- `apps/catalog`: product catalog SCS
- `apps/cart`: active cart and product snapshot SCS
- `apps/orders`: checkout orchestration and immutable order snapshot SCS
- `apps/inventory`: stock balances and order reservation SCS
- `apps/payments`: payment capture and payment history SCS

Suggested future systems:

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

Compose builds and starts PostgreSQL, the five Spring Boot backends, and the three Nginx-served React frontends. The containers communicate through the internal Compose network, so no Java, Maven, Node.js, or npm installation is required on the host.

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
- Inventory health: `http://localhost:8084/actuator/health`
- Inventory replenishment: `POST http://localhost:8084/api/inventory/{productId}/replenishments`
- Payments health: `http://localhost:8085/actuator/health`
- Payment by order: `http://localhost:8085/api/payments/by-order/{orderId}`

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

## Shared Frontend Styles

Reusable UI tokens, base styles, and layout primitives live in `packages/marketplace-ui`.

Each frontend imports the shared stylesheet before app-specific CSS:

```ts
import '@marketplace/ui/styles.css';
import './styles.css';
```
