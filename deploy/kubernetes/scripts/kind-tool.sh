#!/usr/bin/env bash

get_kind_executable() {
  local script_dir repository_root executable
  script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
  repository_root="$(cd -- "$script_dir/../../.." && pwd)"
  case "$(uname -s)" in
    MINGW*|MSYS*|CYGWIN*) executable="$repository_root/.tools/kind.exe" ;;
    *) executable="$repository_root/.tools/kind" ;;
  esac

  if [[ ! -f "$executable" ]]; then
    bash "$script_dir/install-kind.sh" >&2
  fi

  printf '%s\n' "$executable"
}
