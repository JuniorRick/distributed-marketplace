#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
DATABASE_USERNAME="${MARKETPLACE_DB_USERNAME:-marketplace}"
DATABASE_PASSWORD="${MARKETPLACE_DB_PASSWORD:-marketplace-local}"
RABBITMQ_USERNAME="${MARKETPLACE_RABBITMQ_USERNAME:-marketplace}"
RABBITMQ_PASSWORD="${MARKETPLACE_RABBITMQ_PASSWORD:-marketplace-local}"
KEYCLOAK_DB_PASSWORD="${MARKETPLACE_KEYCLOAK_DB_PASSWORD:-keycloak-local}"
KEYCLOAK_ADMIN_USERNAME="${MARKETPLACE_KEYCLOAK_ADMIN_USERNAME:-admin}"
KEYCLOAK_ADMIN_PASSWORD="${MARKETPLACE_KEYCLOAK_ADMIN_PASSWORD:-admin-local}"
GRAFANA_ADMIN_USERNAME="${MARKETPLACE_GRAFANA_ADMIN_USERNAME:-admin}"
GRAFANA_ADMIN_PASSWORD="${MARKETPLACE_GRAFANA_ADMIN_PASSWORD:-admin-local}"

command -v kubectl >/dev/null 2>&1 || { echo "Required command 'kubectl' was not found." >&2; exit 1; }
if ! command -v curl >/dev/null 2>&1 && ! command -v wget >/dev/null 2>&1; then
  echo "Either 'curl' or 'wget' is required to verify the catalog endpoint." >&2
  exit 1
fi

kubectl apply --filename "$SCRIPT_DIR/../base/namespace.yaml"
kubectl create secret generic marketplace-db-credentials \
  --namespace marketplace \
  --from-literal="username=$DATABASE_USERNAME" \
  --from-literal="password=$DATABASE_PASSWORD" \
  --dry-run=client \
  --output=yaml | kubectl apply --filename=-
kubectl create secret generic marketplace-rabbitmq-credentials \
  --namespace marketplace \
  --from-literal="username=$RABBITMQ_USERNAME" \
  --from-literal="password=$RABBITMQ_PASSWORD" \
  --dry-run=client \
  --output=yaml | kubectl apply --filename=-
kubectl create secret generic marketplace-rabbitmq-runtime \
  --namespace marketplace \
  --from-literal="RABBITMQ_USER=$RABBITMQ_USERNAME" \
  --from-literal="RABBITMQ_PASSWORD=$RABBITMQ_PASSWORD" \
  --dry-run=client \
  --output=yaml | kubectl apply --filename=-
kubectl create secret generic marketplace-keycloak-db-credentials \
  --namespace marketplace \
  --from-literal="username=keycloak" \
  --from-literal="password=$KEYCLOAK_DB_PASSWORD" \
  --dry-run=client \
  --output=yaml | kubectl apply --filename=-
kubectl create secret generic marketplace-keycloak-admin-credentials \
  --namespace marketplace \
  --from-literal="username=$KEYCLOAK_ADMIN_USERNAME" \
  --from-literal="password=$KEYCLOAK_ADMIN_PASSWORD" \
  --dry-run=client \
  --output=yaml | kubectl apply --filename=-
kubectl create secret generic marketplace-grafana-admin-credentials \
  --namespace marketplace \
  --from-literal="username=$GRAFANA_ADMIN_USERNAME" \
  --from-literal="password=$GRAFANA_ADMIN_PASSWORD" \
  --dry-run=client \
  --output=yaml | kubectl apply --filename=-

kubectl delete job marketplace-db-init --namespace marketplace --ignore-not-found
kubectl apply --kustomize "$SCRIPT_DIR/../overlays/local"
kubectl rollout status statefulset/marketplace-db --namespace marketplace --timeout=180s
kubectl rollout status statefulset/keycloak-db --namespace marketplace --timeout=180s
kubectl rollout status statefulset/rabbitmq --namespace marketplace --timeout=240s
kubectl rollout status statefulset/kafka --namespace marketplace --timeout=240s
kubectl wait --for=condition=complete job/marketplace-db-init --namespace marketplace --timeout=180s

deployments=(
  loki otel-collector prometheus grafana keycloak \
  catalog cart orders inventory payments notifications customers \
  catalog-frontend cart-frontend orders-frontend gateway
)

# Local images keep the stable :local tag, so explicitly refresh existing pods after loading a rebuild into kind.
kubectl rollout restart "${deployments[@]/#/deployment/}" --namespace marketplace

for deployment in "${deployments[@]}"; do
  kubectl rollout status "deployment/$deployment" --namespace marketplace --timeout=240s
done

catalog_page_url="http://localhost:8088/catalog/products"
catalog_data_url="http://localhost:8088/api/products"
cart_page_url="http://localhost:8088/cart/"
orders_page_url="http://localhost:8088/orders/"
keycloak_url="http://localhost:8088/auth/realms/marketplace/.well-known/openid-configuration"
for ((attempt = 1; attempt <= 20; attempt++)); do
  if command -v curl >/dev/null 2>&1; then
    endpoint_ready=false
    curl --fail --silent "$catalog_page_url" >/dev/null \
      && curl --fail --silent "$catalog_data_url" >/dev/null \
      && curl --fail --silent "$cart_page_url" >/dev/null \
      && curl --fail --silent "$orders_page_url" >/dev/null \
      && curl --fail --silent "$keycloak_url" >/dev/null \
      && endpoint_ready=true
  else
    endpoint_ready=false
    wget --quiet --spider "$catalog_page_url" \
      && wget --quiet --spider "$catalog_data_url" \
      && wget --quiet --spider "$cart_page_url" \
      && wget --quiet --spider "$orders_page_url" \
      && wget --quiet --spider "$keycloak_url" \
      && endpoint_ready=true
  fi
  if [[ "$endpoint_ready" == "true" ]]; then
    echo "Catalog page is ready at $catalog_page_url"
    echo "Catalog data is ready at $catalog_data_url"
    echo "Cart page is ready at $cart_page_url"
    echo "Orders page is ready at $orders_page_url"
    echo "Keycloak is ready at http://localhost:8088/auth"
    exit 0
  fi
  if [[ "$attempt" -eq 20 ]]; then
    echo "The catalog endpoints did not become available." >&2
    exit 1
  fi
  sleep 2
done
