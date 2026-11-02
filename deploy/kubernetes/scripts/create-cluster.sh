#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/kind-tool.sh"

for command in docker kubectl; do
  command -v "$command" >/dev/null 2>&1 || { echo "Required command '$command' was not found." >&2; exit 1; }
done

kind_executable="$(get_kind_executable)"
cluster_name="marketplace"
config="$SCRIPT_DIR/../cluster/kind-config.yaml"

if "$kind_executable" get clusters | grep -Fxq "$cluster_name"; then
  echo "kind cluster '$cluster_name' already exists."
  "$kind_executable" export kubeconfig --name "$cluster_name"
  exit 0
fi

"$kind_executable" create cluster --name "$cluster_name" --config "$config" --wait 120s
