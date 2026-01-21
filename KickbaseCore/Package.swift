// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "KickbaseCore",
    defaultLocalization: "en",
    platforms: [
        .iOS(.v17), .macOS(.v14)
    ],
    products: [
        .library(
            name: "KickbaseCore",
            targets: ["KickbaseCore"]),
    ],
    dependencies: [
        .package(url: "https://source.skip.tools/skip.git", from: "1.0.0"),
        .package(url: "https://source.skip.tools/skip-ui.git", from: "1.0.0"),
        .package(url: "https://source.skip.tools/skip-foundation.git", from: "1.0.0")
    ],
    targets: [
        .target(
            name: "KickbaseCore",
            dependencies: [
                .product(name: "SkipUI", package: "skip-ui"),
                .product(name: "SkipFoundation", package: "skip-foundation")
            ],
            plugins: [
                .plugin(name: "skipstone", package: "skip")
            ]),
    ]
)
