# Distributed Marketplace

This repository is intended to grow as a self-contained systems playground. Each business capability owns its backend, frontend, and database migrations.

Current systems:

- `apps/catalog`: product catalog SCS

Suggested future systems:

- `apps/cart`
- `apps/orders`
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

Start the catalog database:

```powershell
cd C:\Users\estinca\IdeaProjects\distributed-marketplace

# reset already-ran changesets
docker compose -f infra/docker-compose.yml down -v --remove-orphans

docker compose -f infra/docker-compose.yml up -d catalog-db
```

Start the backend:

```powershell
cd apps/catalog/backend
mvn spring-boot:run
```

Start the frontend:

```powershell
cd apps/catalog/frontend
npm install
npm run dev
```

Default URLs:

- Catalog API: `http://localhost:8081/api/products`
- Catalog health: `http://localhost:8081/actuator/health`
- Catalog UI: `http://localhost:5173`

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