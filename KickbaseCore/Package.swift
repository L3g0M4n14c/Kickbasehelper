// swift-tools-version: 5.9
import PackageDescription
import Foundation

// -------------------------------------------------------------------------
// CONFIGURATION
// -------------------------------------------------------------------------
// This variable controls whether the Skipstone plugin is loaded.
// It is set to 'true' by default for local development.
// It will be replaced with 'false' by ci_scripts/ci_post_clone.sh in CI.
var enableSkipPlugin = true
// -------------------------------------------------------------------------

var plugins: [Target.PluginUsage] = []

// Only enable the Skip plugin if required
if enableSkipPlugin {
    plugins.append(.plugin(name: "skipstone", package: "skip"))
}

let package = Package(
    name: "KickbaseCore",
    defaultLocalization: "en",
    platforms: [
        .iOS(.v17), .macOS(.v14),
    ],
    products: [
        .library(
            name: "KickbaseCore",
            targets: ["KickbaseCore"])
    ],
    dependencies: [
        .package(url: "https://source.skip.tools/skip.git", from: "1.0.0"),
        .package(url: "https://source.skip.tools/skip-ui.git", from: "1.0.0"),
        .package(url: "https://source.skip.tools/skip-foundation.git", from: "1.0.0"),
    ],
    targets: [
        .target(
            name: "KickbaseCore",
            dependencies: [
                .product(name: "SkipUI", package: "skip-ui"),
                .product(name: "SkipFoundation", package: "skip-foundation"),
            ],
            plugins: plugins
        )
    ]
)
