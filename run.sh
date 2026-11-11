#!/usr/bin/env bash
set -euo pipefail
# Usage:
#   ./run.sh              # normal mode
#   ./run.sh debug        # debug mode (more verbose)
#   ./run.sh --gui        # GUI mode
#   ./run.sh debug --gui  # debug mode with GUI

DEBUG=0
APP_ARGS=()

# Parse arguments
for arg in "$@"; do
  if [ "$arg" = "debug" ]; then
    DEBUG=1
  else
    APP_ARGS+=("$arg")
  fi
done

echo "=== run.sh starting (debug=${DEBUG}) ==="

GRADLE_ARGS=(run)
if [ "$DEBUG" -eq 0 ]; then
  GRADLE_ARGS+=(--quiet)
fi

# Only pass --args when there is something to pass
if [ "${#APP_ARGS[@]}" -gt 0 ]; then
  GRADLE_ARGS+=(--args="${APP_ARGS[*]}")
fi

./gradlew "${GRADLE_ARGS[@]}"