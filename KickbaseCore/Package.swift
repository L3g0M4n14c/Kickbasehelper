// swift-tools-version: 5.9
import PackageDescription
import Foundation

// Check if running in a CI environment (Xcode Cloud sets CI=TRUE)
// Also check for explicit opt-out via SKIP_DISABLE_PLUGIN
let isCI = ProcessInfo.processInfo.environment["CI"] == "TRUE" || 
           ProcessInfo.processInfo.environment["CI"] == "true" ||
           ProcessInfo.processInfo.environment["SKIP_DISABLE_PLUGIN"] == "true"

var plugins: [Target.PluginUsage] = []

// Only enable the Skip plugin if NOT running in CI
if !isCI {
    plugins.append(.plugin(name: "skipstone", package: "skip"))
} else {
    print("⚠️ Skipstone plugin disabled for CI build")
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
