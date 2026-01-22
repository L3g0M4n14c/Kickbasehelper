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
# 3. SURGICAL STRIKE: Force Resolve & Modify 'skip'
# -------------------------------------------------------------------------
# Strategy: Instead of mocking, we let Xcode resolve the real package, 
# find where it checked it out in DerivedData, and overwrite its Package.swift 
# to remove the plugin definition before the build starts.

echo "🏥 SURGICAL STRIKE: Force resolving and patching 'skip'..."

# Define project and scheme paths relative to ci_scripts
PROJECT_PATH="../Kickbasehelper.xcodeproj"
SCHEME_NAME="Kickbasehelper"

# 1. Resolve Dependencies to populate DerivedData
echo "🔄 Resolving package dependencies to populate DerivedData..."
# Note: This might take a moment as it fetches the real package
if xcodebuild -resolvePackageDependencies -project "$PROJECT_PATH" -scheme "$SCHEME_NAME"; then
    echo "✅ Resolution command finished."
else
    echo "⚠️ Resolution command reported failure (could be due to plugin validation). Proceeding to find checkouts anyway..."
fi

# 2. Locate DerivedData
echo "🔍 Locating DerivedData path..."
BUILD_SETTINGS=$(xcodebuild -project "$PROJECT_PATH" -scheme "$SCHEME_NAME" -showBuildSettings -skipUnavailableActions 2>/dev/null)
BUILD_DIR=$(echo "$BUILD_SETTINGS" | grep "\bBUILD_DIR =" | awk -F ' = ' '{print $2}' | xargs)

if [ -z "$BUILD_DIR" ]; then
    echo "❌ Error: Could not determine BUILD_DIR."
    echo "⚠️ Falling back to searching standard DerivedData location..."
    DD_ROOT="$HOME/Library/Developer/Xcode/DerivedData"
    # Find the most recently modified Kickbasehelper directory?
    # We'll try to find the one containing SourcePackages/checkouts/skip
    SKIP_DIR=$(find "$DD_ROOT" -type d -path "*/SourcePackages/checkouts/skip" -print -quit)
else
    # BUILD_DIR is .../DerivedData/Kickbasehelper-hash/Build/Products
    DERIVED_DATA_ROOT=$(dirname "$(dirname "$BUILD_DIR")")
    SKIP_DIR="$DERIVED_DATA_ROOT/SourcePackages/checkouts/skip"
fi

echo "📂 Target Skip Directory: $SKIP_DIR"

if [ -z "$SKIP_DIR" ] || [ ! -d "$SKIP_DIR" ]; then
    echo "❌ Fatal: 'skip' checkout directory not found."
    echo "  Expected path: $SKIP_DIR"
    echo "  Listing SourcePackages (if BUILD_DIR known):"
    [ ! -z "$DERIVED_DATA_ROOT" ] && ls -R "$DERIVED_DATA_ROOT/SourcePackages"
    exit 1
fi

echo "✅ Found 'skip' checkout."

# 3. Overwrite Package.swift
SKIP_MANIFEST="$SKIP_DIR/Package.swift"
echo "💉 Injecting neutered Package.swift into $SKIP_MANIFEST"

# Make writable first (SPM checkouts are read-only)
echo "🔓 Making $SKIP_MANIFEST writable..."
chmod +w "$SKIP_MANIFEST"
# Verify permissions
ls -l "$SKIP_MANIFEST"

# Preserve original for debugging
cp "$SKIP_MANIFEST" "$SKIP_MANIFEST.bak"

# 4. Write the Sanitized Manifest
# This manifest is based on skip v1.7.0 but drops all 'plugin' products and targets.
cat > "$SKIP_MANIFEST" << 'EOF'
// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "skip",
    defaultLocalization: "en",
    platforms: [
        .iOS(.v16),
        .macOS(.v13),
        .tvOS(.v16),
        .watchOS(.v9),
        .macCatalyst(.v16),
    ],
    products: [
        // REMOVED: .plugin(name: "skipstone", targets: ["skipstone"]),
        // REMOVED: .plugin(name: "skiplink", targets: ["Create SkipLink"]),
        .library(name: "SkipDrive", targets: ["SkipDrive"]),
        .library(name: "SkipTest", targets: ["SkipTest"]),
    ],
    targets: [
        // REMOVED: .plugin(name: "skipstone", ...),
        // REMOVED: .plugin(name: "Create SkipLink", ...),
        
        // MODIFIED: 'SkipDrive' originally depended on "skipstone". We remove that dependency.
        // Original: .target(name: "SkipDrive", dependencies: ["skipstone", .target(name: "skip")]),
        .target(name: "SkipDrive", dependencies: [
            .target(name: "skip")
        ]),
        
        .target(name: "SkipTest", dependencies: [
             .target(name: "SkipDrive", condition: .when(platforms: [.macOS, .linux]))
        ]),
        
        .testTarget(name: "SkipTestTests", dependencies: ["SkipTest"]),
        .testTarget(name: "SkipDriveTests", dependencies: ["SkipDrive"]),
        
        // UNCHANGED: Binary target (macOS)
        .binaryTarget(name: "skip", url: "https://source.skip.tools/skip/releases/download/1.7.0/skip-macos.zip", checksum: "b4e5a62b3cc436824dc9555bf71938d9e9aebcbd992c77ca96c7165dddb3b836")
    ]
)
EOF

echo "✅ Package.swift successfully overwritten."
echo "📄 Content check (head):"
head -n 20 "$SKIP_MANIFEST"

echo "🏁 Surgical strike script completed."

