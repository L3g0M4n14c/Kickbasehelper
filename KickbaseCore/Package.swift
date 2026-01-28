// swift-tools-version: 5.9
import Foundation
import PackageDescription

// -------------------------------------------------------------------------
// CONFIGURATION
// -------------------------------------------------------------------------
// This variable controls whether the Skipstone plugin is loaded.
// It is set to 'true' by default for local development.
// It will be replaced with 'false' by ci_scripts/ci_post_clone.sh in CI.
// Temporarily disable Skip plugin to allow ViewInspector tests to run locally
// Re-enabled locally to produce build tool plugin outputs (.sourcehash) required by Xcode
var enableSkipPlugin = true
// -------------------------------------------------------------------------

var plugins: [Target.PluginUsage] = []
var packageDependencies: [Package.Dependency] = [
    .package(url: "https://source.skip.tools/skip-ui.git", from: "1.0.0"),
    .package(url: "https://source.skip.tools/skip-foundation.git", from: "1.0.0"),
    // ViewInspector for SwiftUI view unit testing (Phase A)
    .package(url: "https://github.com/nalexn/ViewInspector.git", from: "0.9.0"),
]

// Only enable the Skip plugin if required
if enableSkipPlugin {
    plugins.append(.plugin(name: "skipstone", package: "skip"))
    packageDependencies.append(.package(url: "https://source.skip.tools/skip.git", from: "1.0.0"))
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
    dependencies: packageDependencies,
    targets: [
        .target(
            name: "KickbaseCore",
            dependencies: [
                .product(name: "SkipUI", package: "skip-ui"),
                .product(name: "SkipFoundation", package: "skip-foundation"),
            ],
            resources: [.process("Resources")],
            plugins: plugins
        ),
        .testTarget(
            name: "KickbaseCoreTests",
            dependencies: [
                "KickbaseCore",
                .product(name: "ViewInspector", package: "ViewInspector"),
            ],
            path: "Tests/KickbaseCoreTests"
        ),
    ]
)
