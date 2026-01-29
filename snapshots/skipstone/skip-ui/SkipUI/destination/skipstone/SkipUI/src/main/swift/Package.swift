// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "skip-ui",
    platforms: [.iOS(.v16), .macOS(.v13), .tvOS(.v16), .watchOS(.v9), .macCatalyst(.v16)],
    products: [
        .library(name: "SkipUI", targets: ["SkipUI"]),
    ],
    dependencies: [ 
        .package(url: "https://source.skip.tools/skip.git", from: "1.6.21"),
        .package(url: "https://source.skip.tools/skip-model.git", from: "1.6.2"),
    ],
    targets: [
        .target(name: "SkipUI", dependencies: [.product(name: "SkipModel", package: "skip-model")], plugins: [.plugin(name: "skipstone", package: "skip")]),
        .testTarget(name: "SkipUITests", dependencies: ["SkipUI", .product(name: "SkipTest", package: "skip")], resources: [.process("Resources")], plugins: [.plugin(name: "skipstone", package: "skip")]),
    ]
)

if Context.environment["SKIP_BRIDGE"] ?? "0" != "0" {
    package.dependencies += [.package(url: "https://source.skip.tools/skip-bridge.git", "0.0.0"..<"2.0.0")]
    package.targets.forEach({ target in
        target.dependencies += [.product(name: "SkipBridge", package: "skip-bridge")]
    })
    // all library types must be dynamic to support bridging
    package.products = package.products.map({ product in
        guard let libraryProduct = product as? Product.Library else { return product }
        return .library(name: libraryProduct.name, type: .dynamic, targets: libraryProduct.targets)
    })
}

/// Convert remote dependencies into their locally-cached versions.
/// This allows us to re-use dependencies from the parent
/// Xcode/SwiftPM process without redundently cloning them.
func useLocalPackage(named packageName: String, id packageID: String, dependencies: inout [Package.Dependency]) {
    func localDependency(name: String?, location: String) -> Package.Dependency? {
        if name == packageID || location.hasSuffix("/" + packageID) || location.hasSuffix("/" + packageID + ".git") {
            return Package.Dependency.package(path: "Packages/" + packageID)
        } else {
            return nil
        }
    }
    dependencies = dependencies.map { dep in
        switch dep.kind {
        case let .sourceControl(name: name, location: location, requirement: _):
            return localDependency(name: name, location: location) ?? dep
        case let .fileSystem(name: name, path: location):
            return localDependency(name: name, location: location) ?? dep
        default:
            return dep
        }
    }
}
useLocalPackage(named: "skip-unit", id: "skip-unit", dependencies: &package.dependencies)
useLocalPackage(named: "skip-lib", id: "skip-lib", dependencies: &package.dependencies)
useLocalPackage(named: "skip-foundation", id: "skip-foundation", dependencies: &package.dependencies)
useLocalPackage(named: "skip-model", id: "skip-model", dependencies: &package.dependencies)
