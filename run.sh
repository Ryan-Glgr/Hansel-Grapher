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

if [ "$DEBUG" -eq 1 ]; then
  ./gradlew run --args="${APP_ARGS[*]}"
else
  ./gradlew run --quiet --args="${APP_ARGS[*]}"
fi