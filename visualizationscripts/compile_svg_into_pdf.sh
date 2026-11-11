#!/usr/bin/env bash
set -euo pipefail

if [ "$#" -ne 1 ]; then
  echo "Usage: $0 path/to/file.svg" >&2
  exit 1
fi

svgfile="$1"
if [ ! -f "$svgfile" ]; then
  echo "SVG file not found: $svgfile" >&2
  exit 1
fi

out_dir="$(dirname "$svgfile")"
base="$(basename "$svgfile" .svg)"
pdf="${out_dir}/${base}.pdf"

mkdir -p "$out_dir"

cairosvg "$svgfile" -o "$pdf"
rm "$svgfile"