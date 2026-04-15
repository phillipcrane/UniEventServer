#!/usr/bin/env bash
# UniEvent tools CLI - Linux/Mac wrapper for tools.ps1
# Requires: pwsh (PowerShell Core) - https://learn.microsoft.com/en-us/powershell/scripting/install/installing-powershell
set -e

SELF="$(realpath "$0" 2>/dev/null || readlink -f "$0" 2>/dev/null || echo "$0")"
DIR="$(cd "$(dirname "$SELF")" && pwd)"

if ! command -v pwsh > /dev/null 2>&1; then
  echo "  ERROR: pwsh (PowerShell Core) is not installed or not on PATH"
  echo ""
  echo "  Install it with:"
  echo "    Ubuntu/Debian:  sudo apt install -y powershell"
  echo "    Fedora/RHEL:    sudo dnf install -y powershell"
  echo "    macOS:          brew install --cask powershell"
  echo ""
  echo "  Or see: https://learn.microsoft.com/en-us/powershell/scripting/install/installing-powershell"
  exit 1
fi

# Translate --flags to -flags for PowerShell.
# Special case: --verbose -> -v  (PowerShell reserves -Verbose as a common parameter)
args=()
for arg in "$@"; do
  case "$arg" in
    --verbose) args+=("-v") ;;
    --*)       args+=("-${arg#--}") ;;
    *)         args+=("$arg") ;;
  esac
done

exec pwsh -NoLogo -NoProfile -ExecutionPolicy Bypass -File "$DIR/tools.ps1" "${args[@]}"
