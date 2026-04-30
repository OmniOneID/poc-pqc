# Phase 1 — Basic Level PoC (Per-Component Validation)

This phase applies PQC (ML-DSA-44 / ML-KEM-768) to the **basic Open DID components (4 Server SDKs + DID Doc / VC / Key Agreement)** to answer: **"can PQC be used as a basic component?"**

- **Scope**: Run DID Doc, VC issuance/verification, and Key Agreement components on top of the four Server SDKs
- **Comparison**: Size and performance characteristics of PQC algorithms vs. existing Secp256r1 / ECDH
- **Deliverables**: 3 component server PoCs + 4 modified SDKs + result/sequence documents

For broader context and other phases, see the root [README](../README.md).

## Key PoC Results (JVM & Bouncy Castle)

| Component | Comparison | Highlights | Document |
|:----------|:-----------|:-----------|:---------|
| [**DID Document**](servers/pqc-did-doc) | Secp256r1 vs ML-DSA-44 | Public key 40.4× larger / key generation 7× faster | [Result](../docs/phase1/pqc-did-doc/result.md) |
| [**Key Agreement**](servers/pqc-key-agreement) | ECDH vs ML-KEM-768 | Messages 10–12× larger / protocol 2.6× faster | [Result](../docs/phase1/pqc-key-agreement/result.md) |
| [**VC Issue/Verify**](servers/pqc-vc) | Secp256r1 vs ML-DSA-44 | Signature 37.2× larger / signing 3–6× slower, verification on par | [Result](../docs/phase1/pqc-vc/result.md) |

> PQC algorithms grow significantly in size (public key / signature / message), but key generation, verification, and key exchange are actually faster — only signing is 3–6× slower.

## Per-Component Migration Notes

| Document | Description |
|:---------|:------------|
| [DID Document creation sequence](../docs/phase1/pqc-did-doc/sequence.md) | DID Document creation sequence comparison (Secp256r1 → ML-DSA-44) |
| [Key Agreement sequence](../docs/phase1/pqc-key-agreement/sequence.md) | Key Agreement sequence comparison (ECDH → ML-KEM-768) |
| [VC Issue/Verify sequence](../docs/phase1/pqc-vc/sequence.md) | VC issuance/verification sequence comparison (Secp256r1 → ML-DSA-44) |
| [PQC Server SDK change analysis](../docs/phase1/pqc-server-sdk-changes.md) | PQC migration changes across the four SDK modules |

## Tech Stack

| Category | Version / Spec |
|:---------|:---------------|
| Java | 21 |
| Spring Boot | 3.x |
| Bouncy Castle | 1.79+ (`bcprov-jdk18on`) |
| Build tool | Gradle |
| PQC algorithms | ML-DSA-44 (FIPS 204), ML-KEM-768 (FIPS 203) |
| DID Document format | Open DID DID Document |
| VC format | Open DID VC |

## Build Configuration

Existing Open DID 2.0.0 build environments need to upgrade Bouncy Castle.

```groovy
// build.gradle
implementation 'org.bouncycastle:bcprov-jdk18on:1.79' // 1.78.1 → 1.79
```

## Folder Structure

```
phase1-basic-level
├── sdks                              # 4 PQC-applied SDK components
│   ├── pqc-did-crypto-sdk-server     # Signing/verification, key generation (ML-DSA-44 added)
│   ├── pqc-did-datamodel-sdk-server  # Data model (enum additions)
│   ├── pqc-did-core-sdk-server       # DID/VC management (proof mapping, verification path split)
│   └── pqc-did-wallet-sdk-server     # Key management / signing delegation
├── servers                           # 3 component runtime servers
│   ├── pqc-did-doc                   # DID Document creation/management component
│   ├── pqc-vc                        # VC issuance/verification component
│   └── pqc-key-agreement             # ML-KEM-based Key Agreement component
└── etc                               # Auxiliary PoCs (validation outside Server SDKs)
    ├── solidity                      # On-chain (EVM) PQC validation — Foundry project
    └── swift                         # iOS / Swift PQC benchmark (SwiftPM)
```

| Name | Description |
|:-----|:------------|
| **`sdks/`** | Open DID SDK components modified to apply ML-DSA-44. |
| ┖ `pqc-did-crypto-sdk-server` | Adds ML-DSA-44 key generation and signing/verification logic. |
| ┖ `pqc-did-datamodel-sdk-server` | Adds enums such as `MlDsa44VerificationKey2024`, `MlDsa44Signature2024`. |
| ┖ `pqc-did-core-sdk-server` | Splits DID/VC proof-type mapping and ML-DSA-44 verification paths. |
| ┖ `pqc-did-wallet-sdk-server` | Adds an ML-DSA-44 branch in key management. |
| **`servers/`** | Runtime servers that exercise each component's PQC behavior. |
| ┖ `pqc-did-doc` | Validates an ML-DSA-44-based DID Document creation/management component. |
| ┖ `pqc-vc` | Validates an ML-DSA-44-based VC issuance/verification component. |
| ┖ `pqc-key-agreement` | Validates an ML-KEM-based Key Agreement component. |
| **`etc/`** | Auxiliary validation of the same algorithms outside the JVM / Bouncy Castle environment. |
| ┖ `etc/solidity` | Foundry-based Solidity 0.8.27+ project covering on-chain (EVM) ML-DSA verification flows (`PqcDidRegistry`, `PqcVcVerifier`, `StubDilithiumVerifier`). |
| ┖ `etc/swift` | SwiftPM-based Swift 5.8 benchmark. Uses `SwiftDilithium` / `SwiftKyber` to measure size and performance for DID/VC/Key Agreement scenarios on iOS. |

> All related documents live under the root [`docs/phase1/`](../docs/phase1).

## Setup Guides

For each subfolder's build environment, dependency setup, and run commands, refer to the per-folder guide. The four Server SDKs and three runtime servers share a common environment (JDK 21, Spring Boot 3.x, Bouncy Castle 1.79+) — see "Tech Stack" and "Build Configuration" above.

**Server SDKs (×4)** — each module is an independent Gradle project. Build with `./gradlew clean build` from the module folder; emit a JAR with `./gradlew jar`. Drop the resulting JAR into the `libs/` directory of the corresponding `servers/` component.

- [`sdks/pqc-did-crypto-sdk-server`](sdks/pqc-did-crypto-sdk-server) — ML-DSA-44 key generation and signing/verification (Bouncy Castle 1.80).
- [`sdks/pqc-did-datamodel-sdk-server`](sdks/pqc-did-datamodel-sdk-server) — Adds PQC enums such as `MlDsa44VerificationKey2024`, `MlDsa44Signature2024`.
- [`sdks/pqc-did-core-sdk-server`](sdks/pqc-did-core-sdk-server) — DID/VC proof-type mapping and ML-DSA-44 verification path split.
- [`sdks/pqc-did-wallet-sdk-server`](sdks/pqc-did-wallet-sdk-server) — ML-DSA-44 branch in key management.

**Servers (×3)** — each component is a Spring Boot 3.x runtime server. Place the SDK JARs in `libs/` and start with `./gradlew bootRun` from the component folder.

- [`servers/pqc-did-doc`](servers/pqc-did-doc) — ML-DSA-44-based DID Document creation/management component.
- [`servers/pqc-vc`](servers/pqc-vc) — ML-DSA-44-based VC issuance/verification component.
- [`servers/pqc-key-agreement`](servers/pqc-key-agreement) — ML-KEM-768-based Key Agreement component.

**etc auxiliary PoCs** — auxiliary validation outside the JVM. Recommended environments and commands are documented in each folder's README.

- [`etc/solidity/README.md`](etc/solidity/README.md) — Recommended environment (Solidity 0.8.27+, cancun EVM) and build/test commands (`forge build`, `forge test`) for the on-chain (EVM) PQC verification Foundry project.
- [`etc/swift/README.md`](etc/swift/README.md) — Recommended environment (Swift 5.8, Xcode 26.0.1, iOS 15.0+) and SwiftPM-based build/run commands (`swift build`, `swift run -c release PqcBench`) for the iOS / Swift PQC benchmark.
