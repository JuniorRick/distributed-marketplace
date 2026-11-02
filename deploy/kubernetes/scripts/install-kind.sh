#!/usr/bin/env bash
set -euo pipefail

KIND_VERSION="${KIND_VERSION:-v0.33.0}"
SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
REPOSITORY_ROOT="$(cd -- "$SCRIPT_DIR/../../.." && pwd)"
TOOLS_DIRECTORY="$REPOSITORY_ROOT/.tools"

case "$(uname -s)-$(uname -m)" in
  MINGW*-x86_64|MSYS*-x86_64|CYGWIN*-x86_64)
    artifact="kind-windows-amd64"
    checksum="4b22adaa135368c5a465d56bbd8e520cbea87272a06ca00b6078e7b81515c9fc"
    executable_name="kind.exe"
    ;;
  Linux-x86_64|Linux-amd64)
    artifact="kind-linux-amd64"
    checksum="aee6151561422756b764a4ae28e7f44cda5af5a9eead3cc9985112b1de8d8e0d"
    executable_name="kind"
    ;;
  Linux-aarch64|Linux-arm64)
    artifact="kind-linux-arm64"
    checksum="20022bee6cfcd5086cb7234d218e3454e6090022f2a8f55d1fa7fcf42c3867a2"
    executable_name="kind"
    ;;
  Darwin-x86_64|Darwin-amd64)
    artifact="kind-darwin-amd64"
    checksum="5a99f26f57246dc9319dd294803313197a0f34d33c525b3ea8b655db5916ece0"
    executable_name="kind"
    ;;
  Darwin-arm64)
    artifact="kind-darwin-arm64"
    checksum="0c8c7dbe5e23594a198b786c4bc13dacc101fa6196b0cb0b23a1ca44e61f4b4f"
    executable_name="kind"
    ;;
  *)
    echo "Unsupported platform: $(uname -s) $(uname -m)" >&2
    exit 1
    ;;
esac

KIND_EXECUTABLE="$TOOLS_DIRECTORY/$executable_name"

if [[ "$KIND_VERSION" != "v0.33.0" ]]; then
  echo "No trusted checksums are configured for kind $KIND_VERSION." >&2
  exit 1
fi

sha256() {
  if command -v sha256sum >/dev/null 2>&1; then
    sha256sum "$1" | awk '{print $1}'
  else
    shasum -a 256 "$1" | awk '{print $1}'
  fi
}

if [[ -f "$KIND_EXECUTABLE" ]]; then
  if [[ "$(sha256 "$KIND_EXECUTABLE")" != "$checksum" ]]; then
    echo "Existing $KIND_EXECUTABLE does not match the trusted checksum. Delete it and rerun this script." >&2
    exit 1
  fi
  echo "kind $KIND_VERSION is already installed at $KIND_EXECUTABLE"
  exit 0
fi

mkdir -p "$TOOLS_DIRECTORY"
download="$KIND_EXECUTABLE.download"
trap 'rm -f "$download"' EXIT

url="https://github.com/kubernetes-sigs/kind/releases/download/$KIND_VERSION/$artifact"
echo "Downloading kind $KIND_VERSION from $url"
if command -v curl >/dev/null 2>&1; then
  curl --fail --location --show-error --silent "$url" --output "$download"
elif command -v wget >/dev/null 2>&1; then
  wget --quiet "$url" --output-document="$download"
else
  echo "Either 'curl' or 'wget' is required to download kind." >&2
  exit 1
fi

if [[ "$(sha256 "$download")" != "$checksum" ]]; then
  echo "Downloaded kind binary failed SHA-256 verification." >&2
  exit 1
fi

chmod +x "$download"
mv "$download" "$KIND_EXECUTABLE"
trap - EXIT
echo "Installed kind $KIND_VERSION at $KIND_EXECUTABLE"
"$KIND_EXECUTABLE" version
