#!/bin/sh

#  ci_post_clone.sh
#  Kickbasehelper

echo "🔧 Configuring Xcode Cloud specific settings..."

if [ "$SKIP_DISABLE_PLUGIN" = "true" ]; then
    echo "ℹ️ SKIP_DISABLE_PLUGIN is set to 'true'. Skipstone plugin should be disabled in Package.swift."
else
    echo "ℹ️ SKIP_DISABLE_PLUGIN is NOT set to 'true' (Current value: '${SKIP_DISABLE_PLUGIN}'). Skipstone plugin will be enabled."
fi

echo "✅ CI setup complete."
