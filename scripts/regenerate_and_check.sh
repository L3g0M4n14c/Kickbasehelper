#!/usr/bin/env bash
set -euo pipefail

# Regenerate Skip outputs (transpile) and run Kotlin compile smoke test.
# Usage: ./scripts/regenerate_and_check.sh

ROOT_DIR=$(cd "$(dirname "$0")/.." && pwd)
cd "$ROOT_DIR/KickbaseCore"

echo "Running Xcode build/transpile..."
# This assumes the KickbaseCore scheme builds the skip outputs used by Android
xcodebuild -project "$ROOT_DIR/Kickbasehelper.xcodeproj" -scheme KickbaseCore -configuration Debug -sdk macosx -destination 'platform=macOS' build

echo "Running Android Kotlin compile (smoke)..."
cd "$ROOT_DIR/Android"
./gradlew :app:compileDebugKotlin --no-daemon --stacktrace

echo "Regenerate and check completed. If failures, inspect logs and follow docs/SKIP_SKILL.md for SKIP directives."