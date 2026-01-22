// swift-tools-version: 5.9
import PackageDescription
import Foundation

// This variable will be set to false by ci_scripts/ci_post_clone.sh in Xcode Cloud
var enableSkipPlugin = true

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
