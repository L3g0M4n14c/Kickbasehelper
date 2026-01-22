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

# -------------------------------------------------------------------------
# 3. MOCK 'skip' PACKAGE to prevent Plugin Validation
# -------------------------------------------------------------------------
# Even if disabled in Package.swift, transitive dependencies (skip-ui) bring in 'skip'.
# Xcode Cloud validates all plugins in resolved graph. 'skip' contains a plugin.
# We replace 'skip' with a local dummy package that has NO plugin using git config redirects.

echo "👻 Creating Mock 'skip' package to bypass plugin validation..."

# Location for the mock package (outside ci_scripts to avoid clutter)
# ci_scripts is inside the repo, so ../MockSkip puts it in the repo root temporarily or ignored
MOCK_DIR="../MockSkip"
mkdir -p "$MOCK_DIR/Sources/Skip"

# Create dummy Package.swift
# We provide the 'Skip' library product because 'skip-ui' likely links against it.
cat > "$MOCK_DIR/Package.swift" <<EOF
// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "skip",
    products: [
        .library(name: "Skip", targets: ["Skip"]),
    ],
    targets: [
        .target(name: "Skip"),
    ]
)
EOF

# Create dummy source file to make it a valid target
echo "public struct Skip {}" > "$MOCK_DIR/Sources/Skip/Skip.swift"

# Initialize Git repo and tag it to match the version dependencies expect (e.g. 1.7.0)
CURRENT_PWD=$(pwd)
cd "$MOCK_DIR" || exit 1
git init
git config user.email "ci@example.com"
git config user.name "CI Bot"
git add .
git commit -m "Initial commit of Mock Skip"
git tag 1.7.0
git tag 1.0.0
# Save absolute path
GIT_MOCK_PATH=$(pwd)
cd "$CURRENT_PWD"

echo "📍 Redirecting 'skip' repo to local mock at: $GIT_MOCK_PATH"

# 1. Clean up SPM caches to ensure the real package isn't used from cache
echo "🧹 Clearing SPM caches..."
rm -rf ~/Library/Caches/org.swift.swiftpm
rm -rf ~/Library/org.swift.swiftpm

# 2. Remove Package.resolved to force re-resolution against the mock
# (Since the mock repo has different commit hashes than the real one, the lockfile must be regenerated)
RESOLVED_FILE="${TARGET_FILE%.swift}.resolved"
if [ -f "$RESOLVED_FILE" ]; then
    echo "🗑 Deleting $RESOLVED_FILE to force dependency resolution using mock..."
    rm "$RESOLVED_FILE"
fi

# 3. Redirect both source and github URLs to the local mock
# Using file:// schema is critical for SPM to respect the local path override correctly
git config --global url."file://$GIT_MOCK_PATH".insteadOf "https://source.skip.tools/skip.git"
git config --global url."file://$GIT_MOCK_PATH".insteadOf "https://github.com/skiptools/skip.git"

# -------------------------------------------------------------------------
# 4. DISABLE PLUGIN VALIDATION (Safety Net)
# -------------------------------------------------------------------------
echo "🛡 Setting defaults to disable plugin validation..."
defaults write com.apple.dt.Xcode IDESkipPackagePluginFingerprintValidation -bool YES
defaults write com.apple.dt.Xcode IDEDisablePluginValidation -bool YES

echo "✅ CI setup complete."
