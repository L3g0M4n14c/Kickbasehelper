#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR=$(cd "$(dirname "$0")/.." && pwd)
SKIP_OUT="$ROOT_DIR/KickbaseCore/.build/plugins/outputs"
SNAP_TMP="/tmp/skipstone_snapshot"
SNAP_TAR="/tmp/skipstone_snapshot.tar.gz"

rm -rf "$SNAP_TMP" "$SNAP_TAR"
mkdir -p "$SNAP_TMP"

if [ ! -d "$SKIP_OUT" ]; then
  echo "No skipstone outputs found at $SKIP_OUT" >&2
  exit 1
fi

# Copy outputs to tmp dir
cp -R "$SKIP_OUT"/* "$SNAP_TMP"/ || true

# Create deterministic tar using sorted file list
cd "$SNAP_TMP"
find . -type f | sort > /tmp/skipstone_files_sorted.txt
# tar -T reads newline separated file list relative to current dir
tar -czf "$SNAP_TAR" -T /tmp/skipstone_files_sorted.txt

echo "$SNAP_TAR"
