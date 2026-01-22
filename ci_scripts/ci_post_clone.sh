#!/bin/sh

#  ci_post_clone.sh
#  Kickbasehelper

echo "🔧 Configuring Xcode Cloud specific settings..."

# HARD REPLACEMENT for Skip Plugin
# We use sed to physically modify Package.swift before the build starts.
# This bypasses any issues with environment variables not propagating.

TARGET_FILE="KickbaseCore/Package.swift"

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
