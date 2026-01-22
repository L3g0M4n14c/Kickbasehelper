#!/bin/sh

#  ci_post_clone.sh
#  Kickbasehelper

echo "🔧 Configuring Xcode Cloud specific settings..."

# 1. Fix Dependency Resolution likely caused by source.skip.tools redirects
echo "🌐 Configuring git to use github.com instead of source.skip.tools..."
# Skip's vanity URL just redirects to GitHub, but this fails often in CI
git config --global url."https://github.com/skiptools/".insteadOf "https://source.skip.tools/"

# 2. Disable Skip Plugin to avoid 'Plugin must be enabled' trust issues
# In Xcode Cloud, this script runs inside the ci_scripts directory
# So we need to go up one level to reach KickbaseCore
TARGET_FILE="../KickbaseCore/Package.swift"

if [ ! -f "$TARGET_FILE" ]; then
    # Fallback to current directory just in case context changes
    echo "⚠️ File not found at ../KickbaseCore/Package.swift, checking current dir..."
    TARGET_FILE="KickbaseCore/Package.swift"
fi

if [ ! -f "$TARGET_FILE" ]; then
    echo "❌ Error: Could not find Package.swift. Current dir: $(pwd)"
    ls -la
    exit 1
fi

echo "📝 Modifying $TARGET_FILE to disable Skip plugin..."

# MacOS sed requires empty string for backup extension
if sed -i '' 's/var enableSkipPlugin = true/var enableSkipPlugin = false/' "$TARGET_FILE"; then
    echo "✅ Successfully executed sed command."
else
    echo "❌ sed command failed."
    exit 1
fi

# Verify the change
if grep -q "var enableSkipPlugin = false" "$TARGET_FILE"; then
    echo "✅ CONFIRMED: Skip plugin is disabled in Package.swift"
else
    echo "❌ ERROR: Verification failed. Package.swift was not updated correctly."
    grep "var enableSkipPlugin" "$TARGET_FILE"
    exit 1
fi

echo "✅ CI setup complete."
