// swift-tools-version: 5.9
import PackageDescription
import Foundation

// Check environment variable to optionally disable Skip plugin (e.g. in Xcode Cloud)
// Set 'SKIP_DISABLE_PLUGIN' to 'true' in your CI environment to disable the plugin
let enableSkipPlugin = ProcessInfo.processInfo.environment["SKIP_DISABLE_PLUGIN"] != "true"

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
        .package(url: "https://github.com/skiptools/skip.git", from: "1.0.0"),
        .package(url: "https://github.com/skiptools/skip-ui.git", from: "1.0.0"),
        .package(url: "https://github.com/skiptools/skip-foundation.git", from: "1.0.0"),
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
