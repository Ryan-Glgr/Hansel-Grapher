#!/usr/bin/env bash
set -euo pipefail

# Usage:
#   ./run.sh         # normal mode (shows normal output; dot errors are hidden into out/dot_errors.log)
#   ./run.sh debug   # debug mode (prints dot errors and is more verbose)

DEBUG=0
if [ "${1-}" = "debug" ]; then
  DEBUG=1
fi

echo "=== run.sh starting (debug=${DEBUG}) ==="

# Forward DEBUG to maven's compile
if [ "$DEBUG" -eq 1 ]; then
  mvn clean compile
else
  mvn clean compile --quiet
fi

echo "=== running Java ==="
# Forward DEBUG to maven's exec
if [ "$DEBUG" -eq 1 ]; then
  mvn exec:java -e
else
  mvn exec:java --quiet
fi

echo "=== done ==="
