#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/kind-tool.sh"
kind_executable="$(get_kind_executable)"
"$kind_executable" delete cluster --name marketplace
