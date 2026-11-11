#!/usr/bin/env bash
set -euo pipefail

if [ "$#" -lt 1 ] || [ "$#" -gt 2 ]; then
  echo "Usage: $0 path/to/file.dot [engine]" >&2
  exit 1
fi

dotfile="$1"
engine="${2:-dot}"

if [ ! -f "$dotfile" ]; then
  echo "DOT file not found: $dotfile" >&2
  exit 1
fi

out_dir="$(dirname "$dotfile")"
base="$(basename "$dotfile" .dot)"
pdf="${out_dir}/${base}.pdf"

mkdir -p "$out_dir"
ulimit -s unlimited

if [ "$engine" = "neato" ]; then
  neato -n2 -Gpack=false -Tpdf "$dotfile" -o "$pdf"
else
  dot -Tpdf "$dotfile" -o "$pdf"
fi

rm "$dotfile"