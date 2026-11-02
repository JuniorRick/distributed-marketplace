#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
for command in helm kubectl; do
  command -v "$command" >/dev/null 2>&1 || { echo "Required command '$command' was not found." >&2; exit 1; }
done

helm upgrade --install ingress-nginx ingress-nginx \
  --repo https://kubernetes.github.io/ingress-nginx \
  --version 4.15.1 \
  --namespace ingress-nginx \
  --create-namespace \
  --values "$SCRIPT_DIR/../infrastructure/ingress-nginx-values.yaml" \
  --wait \
  --timeout 5m

kubectl rollout status deployment/ingress-nginx-controller \
  --namespace ingress-nginx \
  --timeout=180s
