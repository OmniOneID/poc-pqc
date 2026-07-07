# PQC Phase 1 — Swift Benchmark

Swift benchmark harness for Dilithium / Kyber on the client side, used to measure DID/VC and key-agreement scenarios in Phase 1.

## Recommended Environment

| Category      | Details             |
| ------------- | ------------------- |
| Language      | Swift 5.8           |
| IDE           | Xcode 26.0.1        |
| Compatibility | iOS 15.0 and higher |

Dependencies are resolved via SwiftPM (`Package.swift`):
- [SwiftDilithium](https://github.com/leif-ibsen/SwiftDilithium) 3.6.0+
- [SwiftKyber](https://github.com/leif-ibsen/SwiftKyber) 3.5.0+

## Run

From this directory:

```bash
# Resolve dependencies
swift package resolve

# Build
swift build -c release

# Run the benchmark executable
swift run -c release PqcBench
```

To open the package in Xcode:

```bash
open Package.swift
```
