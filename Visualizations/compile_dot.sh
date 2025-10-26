#!/usr/bin/env bash
set -euo pipefail

if [ "$#" -ne 1 ]; then
  echo "Usage: $0 path/to/file.dot" >&2
  exit 1
fi

dotfile="$1"
if [ ! -f "$dotfile" ]; then
  echo "DOT file not found: $dotfile" >&2
  exit 1
fi

out_dir="$(dirname "$dotfile")"
base="$(basename "$dotfile" .dot)"
pdf="${out_dir}/${base}.pdf"

mkdir -p "$out_dir"

dot -Tpdf "$dotfile" -o "$pdf"
rm "$dotfile"
