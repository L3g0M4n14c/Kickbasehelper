#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR=$(cd "$(dirname "$0")/.." && pwd)
CANON_DIR="$ROOT_DIR/snapshots/skipstone"
GEN_TAR="$1"

if [ ! -f "$GEN_TAR" ]; then
  echo "Generated snapshot not found: $GEN_TAR" >&2
  exit 1
fi

if [ ! -d "$CANON_DIR" ]; then
  echo "Canonical snapshot directory not found: $CANON_DIR" >&2
  exit 1
fi

TMP_GEN="/tmp/skipstone_snapshot_gen"
rm -rf "$TMP_GEN" && mkdir -p "$TMP_GEN"

# Extract generated
tar -xzf "$GEN_TAR" -C "$TMP_GEN"

# Compare
if diff -ru "$CANON_DIR" "$TMP_GEN"; then
  echo "No diffs between canonical snapshots and generated outputs."
  exit 0
else
  echo "Snapshot diff detected. Failing." >&2
  exit 2
fi
