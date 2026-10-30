#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
bash "$SCRIPT_DIR/create-cluster.sh"
bash "$SCRIPT_DIR/install-ingress.sh"
bash "$SCRIPT_DIR/build-images.sh"
bash "$SCRIPT_DIR/deploy.sh"
