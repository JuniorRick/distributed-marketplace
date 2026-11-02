# Local Kubernetes

The local kind environment runs the complete marketplace behind ingress-nginx. It includes PostgreSQL, RabbitMQ, Kafka,
Keycloak, all eight Spring services, all three React frontends, and the OpenTelemetry/Loki/Prometheus/Grafana stack.
Backends and infrastructure use Kubernetes `ClusterIP` services. Ingress is the only host-facing application entry point.

## Prerequisites

- A Docker-compatible engine. Rancher Desktop with `dockerd (moby)` is recommended on Windows and macOS.
- `kubectl`
- Helm 3
- Bash. Use Git Bash on Windows.
- `curl` or `wget`
- Host ports `8088` and `8443` available

kind `v0.33.0` is downloaded automatically into `.tools` when a script first needs it. The installer chooses the host
artifact and verifies its pinned SHA-256 checksum.

## Start

From the repository root:

```bash
bash deploy/kubernetes/scripts/start.sh
```

The script creates the cluster, installs ingress-nginx, builds and loads all application images, creates local Secrets,
applies the Kustomize overlay, initializes the database schemas, and waits for every workload and public route.

Public routes:

- Catalog UI: `http://localhost:8088/catalog/products`
- Cart UI: `http://localhost:8088/cart/`
- Orders UI: `http://localhost:8088/orders/`
- Product JSON API: `http://localhost:8088/api/products`
- Keycloak: `http://localhost:8088/auth`

Product reads and React pages allow anonymous access. `/api/carts`, `/api/orders`, and the exact `/api/customers/me`
path are forwarded to Gateway and require a valid bearer token. Gateway forwards the token to each service, which also
validates it and enforces customer ownership. The customer profile endpoint supports GET and POST only.

Inventory, Payments, Notifications, customer administration, catalog mutations, and direct cart checkout are not exposed
through Gateway. Checkout starts through `POST /api/orders`, which runs the order saga.

## Inspect

```bash
kubectl get pods,services,ingress,pvc -n marketplace
kubectl logs -n marketplace deployment/orders
kubectl describe pod -n marketplace -l app.kubernetes.io/name=orders
```

Observability and broker administration remain internal. Access them temporarily with port forwarding:

```bash
kubectl port-forward -n marketplace service/grafana 3000:3000
kubectl port-forward -n marketplace service/prometheus 9090:9090
kubectl port-forward -n marketplace service/rabbitmq 15672:15672
```

Local defaults are `marketplace` / `marketplace-local` for PostgreSQL and RabbitMQ, and `admin` / `admin-local` for both
Keycloak and Grafana. Override them with the corresponding `MARKETPLACE_*` environment variables accepted by
`deploy/kubernetes/scripts/deploy.sh`. These values are for local practice only.

Changing a bootstrap database password does not update an existing persistent data directory. Recreate the kind cluster
when changing bootstrap credentials.

## Rebuild

```bash
bash deploy/kubernetes/scripts/build-images.sh
bash deploy/kubernetes/scripts/deploy.sh
```

The deploy script explicitly restarts deployments because local images intentionally retain the stable `:local` tag.

## Stop

```bash
bash deploy/kubernetes/scripts/stop.sh
```

Deleting the kind cluster also deletes its local persistent volumes. Docker Compose remains the environment used by the
existing end-to-end test module.
