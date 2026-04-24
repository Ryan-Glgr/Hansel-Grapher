#!/usr/bin/env bash
set -euo pipefail
# Usage:
#   ./run.sh         # normal mode
#   ./run.sh debug   # debug mode (more verbose)
DEBUG=0
if [ "${1-}" = "debug" ]; then
  DEBUG=1
fi
echo "=== run.sh starting (debug=${DEBUG}) ==="

if [ "$DEBUG" -eq 1 ]; then
  ./gradlew shadowJar
else
  ./gradlew shadowJar --quiet
fi

echo "=== running ==="
java -jar build/libs/hansel-grapher-1.0-SNAPSHOT-all.jar

echo "=== done ==="