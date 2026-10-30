#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
REPOSITORY_ROOT="$(cd -- "$SCRIPT_DIR/../../.." && pwd)"
source "$SCRIPT_DIR/kind-tool.sh"

command -v docker >/dev/null 2>&1 || { echo "Required command 'docker' was not found." >&2; exit 1; }
kind_executable="$(get_kind_executable)"

for service in catalog cart orders inventory payments notifications customers gateway; do
  image="marketplace/$service:local"
  docker build \
    --file "$REPOSITORY_ROOT/docker/backend.Dockerfile" \
    --build-arg "SERVICE=$service" \
    --tag "$image" \
    "$REPOSITORY_ROOT"
  "$kind_executable" load docker-image "$image" --name marketplace
done

for service in catalog cart orders; do
  frontend_image="marketplace/$service-frontend:local"
  docker build \
    --file "$REPOSITORY_ROOT/docker/frontend.Dockerfile" \
    --build-arg "SERVICE=$service" \
    --build-arg VITE_MARKETPLACE_URL=http://localhost:8088 \
    --build-arg VITE_OIDC_URL=http://localhost:8088/auth \
    --build-arg VITE_AUTH_ENABLED=true \
    --tag "$frontend_image" \
    "$REPOSITORY_ROOT"
  "$kind_executable" load docker-image "$frontend_image" --name marketplace
done

keycloak_image="marketplace/keycloak:local"
docker build \
  --file "$REPOSITORY_ROOT/docker/keycloak.Dockerfile" \
  --tag "$keycloak_image" \
  "$REPOSITORY_ROOT"
"$kind_executable" load docker-image "$keycloak_image" --name marketplace
