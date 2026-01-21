// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "KickbaseCore",
    platforms: [
        .iOS(.v17), .macOS(.v14)
    ],
    products: [
        .library(
            name: "KickbaseCore",
            targets: ["KickbaseCore"]),
    ],
    targets: [
        .target(
            name: "KickbaseCore",
            dependencies: []),
    ]
)
