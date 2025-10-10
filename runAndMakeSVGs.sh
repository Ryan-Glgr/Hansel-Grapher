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
  mvn exec:java
else
  mvn exec:java --quiet
fi

echo "=== generating SVGs from .dot files ==="

# enable nullglob so the pattern expands to zero files (no literal "out/*.dot")
shopt -s nullglob

for dotfile in out/*.dot; do
    svg="${dotfile%.dot}.svg"
    if [ "$DEBUG" -eq 1 ]; then
        # write SVG output and print Graphviz messages to stdout
        dot -Tsvg "$dotfile" -o "$svg"
    else
        # write SVG output and append Graphviz messages to log; continue on non-zero return
        dot -Tsvg "$dotfile" -o "$svg" 2>> out/dot_errors.log || true
    fi

    # optional cleanup: remove .dot only if svg was produced
    if [ -f "$svg" ]; then
        rm "$dotfile"
    else
        echo "WARNING: expected $svg but it was not created for $dotfile"
    fi
done

# restore default globbing behavior if you want (optional)
shopt -u nullglob

echo "=== opening generated SVGs (from out/) ==="
if [ "$DEBUG" -eq 1 ]; then
  open out/*.svg || true
else
  open -g out/*.svg >/dev/null 2>&1 || true
  if [ -f out/dot_errors.log ] && [ -s out/dot_errors.log ]; then
    echo "NOTE: Graphviz wrote messages to out/dot_errors.log. Run './run.sh debug' to view them."
  fi
fi

echo "=== done ==="
