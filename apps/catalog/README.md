# Catalog SCS

Catalog is the first self-contained system in the distributed marketplace.

It owns:

- product SKU
- product name and description
- display price
- publication status

It exposes:

- `GET /api/products`
- `GET /api/products/{id}`
- `POST /api/products`

It intentionally does not own inventory, carts, orders, or payments.

Database schema changes are managed by the Liquibase XML master changelog at `backend/src/main/resources/db/changelog/db.changelog-master.xml`, which includes formatted SQL changesets from `backend/src/main/resources/db/changelog/changes`.

## Local Development

Infra:

```powershell

docker compose -f infra/docker-compose.yml up -d catalog-db


```

Backend:

```powershell
cd apps/catalog/backend
mvn spring-boot:run
```

Frontend:

```powershell
cd apps/catalog/frontend
npm install
npm run dev
```
