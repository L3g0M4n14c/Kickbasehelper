#!/bin/sh

#  ci_post_clone.sh
#  Kickbasehelper
#
#  Cleaned up for CI Skip plugin exclusion strategy.

echo "🔧 Configuring Xcode Cloud specific settings..."

# No special plugin validation overrides needed anymore as we disable the plugin in CI.
# The Package.swift now checks for the CI environment variable.

echo "✅ Deployment preparation complete."
