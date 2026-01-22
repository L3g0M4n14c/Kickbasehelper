#!/bin/sh

#  ci_post_clone.sh
#  Kickbasehelper
#
#  Created by GitHub Copilot on 22.01.26.
#

# Skip package plugin validation to avoid "Plugin must be enabled" error in Xcode Cloud
defaults write com.apple.dt.Xcode IDESkipPackagePluginFingerprintValidation -bool YES

echo "✅ IDESkipPackagePluginFingerprintValidation set to YES"
