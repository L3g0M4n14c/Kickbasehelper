#!/usr/bin/env bash
set -euo pipefail

BRANCH=${1:-skipstone-snapshots}
ROOT_DIR=$(cd "$(dirname "$0")/.." && pwd)
SNAP_DIR="$ROOT_DIR/snapshots/skipstone"
GEN_DIR="/tmp/skipstone_snapshot_gen"

if [ ! -d "$GEN_DIR" ]; then
  echo "No generated snapshot directory at $GEN_DIR" >&2
  exit 1
fi

if [ -z "${GITHUB_TOKEN:-}" ]; then
  echo "Warning: GITHUB_TOKEN not set, exiting without committing." >&2
  exit 0
fi

git config user.email "ci@github-actions"
git config user.name "CI Snapshot Bot"

# Create branch and update snapshots
git fetch origin
git checkout -B "$BRANCH"
mkdir -p "$SNAP_DIR"
rsync -a "$GEN_DIR"/ "$SNAP_DIR"/

git add "$SNAP_DIR"
if git diff --quiet --cached; then
  echo "No snapshot changes to commit"
  exit 0
fi

git commit -m "Update skipstone snapshots [ci skip]" || true
git push -u origin "$BRANCH"

echo "Snapshots updated and pushed to $BRANCH"
