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

# Forward DEBUG to make so Makefile can flip dot behavior
if [ "$DEBUG" -eq 1 ]; then
  make all DEBUG=1
else
  make all DEBUG=0 >/dev/null
  # above: silence repetitive make output in normal mode; adjust if you prefer visible make logs
fi

echo "=== running Java ==="
java -cp bin Main


echo "=== generating PDFs from .dot files ==="
for dotfile in out/*.dot; do
    pdf="${dotfile%.dot}.pdf"
    if [ "$DEBUG" -eq 1 ]; then
        dot -Tpdf "$dotfile" -o "$pdf"
    else
        dot -Tpdf "$dotfile" -o "$pdf" 2>> out/dot_errors.log || true
    fi

    # optional cleanup
    if [ -f "$pdf" ]; then
        rm "$dotfile"
    fi
done

echo "=== opening generated PDFs (from out/) ==="
if [ "$DEBUG" -eq 1 ]; then
  # debug: open normally so you can see messages from Preview (if any)
  open out/*.pdf || true
else
  # normal: open in background and suppress output to keep terminal clean
  open -g out/*.pdf >/dev/null 2>&1 || true
  if [ -f out/dot_errors.log ] && [ -s out/dot_errors.log ]; then
    echo "NOTE: Graphviz wrote messages to out/dot_errors.log. Run './run.sh debug' to view them."
  fi
fi

echo "=== done ==="
