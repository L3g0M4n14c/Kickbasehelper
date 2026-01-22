#!/bin/sh

#  ci_post_clone.sh
#  Kickbasehelper

echo "🔧 Configuring Xcode Cloud specific settings..."

# HARD DISABLE: Modify Package.swift to disable the Skip plugin
# Xcode Cloud creates a fresh clone, so this modification is local to the build agent.
# We use sed to replace the variable directly in the file.

TARGET_FILE="KickbaseCore/Package.swift"

if [ -f "$TARGET_FILE" ]; then
    echo "📝 Disabling Skip plugin in $TARGET_FILE"
    # MacOS sed syntax (requires empty string for backup extension)
    sed -i '' 's/var enableSkipPlugin = true/var enableSkipPlugin = false/' "$TARGET_FILE"
    
    # Verify the change
    if grep -q "var enableSkipPlugin = false" "$TARGET_FILE"; then
        echo "✅ Successfully disabled Skip plugin"
    else
        echo "❌ Failed to update Package.swift"
        grep "var enableSkipPlugin" "$TARGET_FILE"
        exit 1
    fi
else
    echo "⚠️ Could not find Package.swift at $TARGET_FILE"
    ls -R
fi

echo "✅ CI setup complete."
