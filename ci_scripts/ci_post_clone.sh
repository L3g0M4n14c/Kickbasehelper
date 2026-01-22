#!/bin/sh

#  ci_post_clone.sh
#  Kickbasehelper
#
#  Updated for Skip plugin trust issues
#

echo "🔧 Configuring Xcode Cloud specific settings..."

# Disable plugin validation for both Xcode and xcodebuild
# This is required for Skipstone (and other plugins) to run in CI without interaction
defaults write com.apple.dt.Xcode IDESkipPackagePluginFingerprintValidation -bool YES
defaults write com.apple.dt.xcodebuild IDESkipPackagePluginFingerprintValidation -bool YES

# Disable macro validation as well, as Skip uses macros
defaults write com.apple.dt.Xcode IDESkipMacroFingerprintValidation -bool YES
defaults write com.apple.dt.xcodebuild IDESkipMacroFingerprintValidation -bool YES

# Force preferences to flush/reload
killall cfprefsd || true

# Verify the settings
echo "IDE settings updated:"
defaults read com.apple.dt.Xcode IDESkipPackagePluginFingerprintValidation
defaults read com.apple.dt.xcodebuild IDESkipPackagePluginFingerprintValidation
defaults read com.apple.dt.Xcode IDESkipMacroFingerprintValidation
defaults read com.apple.dt.xcodebuild IDESkipMacroFingerprintValidation

# Pre-resolve dependencies to ensure plugins are loaded with validation skipped
echo "📦 Resolving package dependencies..."
xcodebuild -resolvePackageDependencies -project Kickbasehelper.xcodeproj -skipPackagePluginValidation || true

echo "✅ Deployment preparation complete."
