#!/usr/bin/env bash
set -euo pipefail

# Check that skipstone artifacts exist and are non-empty
ROOT_DIR=$(cd "$(dirname "$0")/.." && pwd)
BUILD_OUTPUT="$ROOT_DIR/KickbaseCore/.build/plugins/outputs"

echo "Checking skipstone outputs in: $BUILD_OUTPUT"

# Attempt to generate if missing
if [ ! -d "$BUILD_OUTPUT" ] || [ -z "$(ls -A "$BUILD_OUTPUT" 2>/dev/null)" ]; then
  echo "skipstone outputs missing or empty - attempting to generate (swift build)"
  (cd "$ROOT_DIR/KickbaseCore" && swift build) || {
    echo "swift build failed to generate skipstone outputs" >&2
    exit 1
  }
fi

# Re-check
if [ ! -d "$BUILD_OUTPUT" ] || [ -z "$(ls -A "$BUILD_OUTPUT" 2>/dev/null)" ]; then
  echo "skipstone outputs still missing after generation - aborting" >&2
  exit 1
fi

echo "skipstone outputs present. You can now run Android build: cd Android && ./gradlew assembleDebug"
