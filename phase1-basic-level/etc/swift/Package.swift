// swift-tools-version:5.8
import PackageDescription

let package = Package(
    name: "PqcBench",
    platforms: [.macOS(.v13)],
    dependencies: [
        .package(url: "https://github.com/leif-ibsen/SwiftDilithium.git", from: "3.6.0"),
        .package(url: "https://github.com/leif-ibsen/SwiftKyber.git", from: "3.5.0"),
    ],
    targets: [
        .executableTarget(
            name: "PqcBench",
            dependencies: [
                .product(name: "SwiftDilithium", package: "SwiftDilithium"),
                .product(name: "SwiftKyber", package: "SwiftKyber"),
            ]
        )
    ]
)
