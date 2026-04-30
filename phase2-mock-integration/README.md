# Phase 2 — Mock Integration PoC (AOS App ↔ Mock Server)

Building on the basic components verified in Phase 1, this phase validates **whether PQC operates correctly in the real Open DID integration flow between an AOS app and a Mock Server**.

- **Scope**: End-to-end integration across the AOS CA app, Client SDK (AOS), and Sandbox (Mock) Server
- **Core question**: Does PQC work as expected on an actual device/server integration flow?

For overall context and other phases, see the root [README](../README.md).

## Folder Structure

```
phase2-mock-integration
├── apps
│   └── did-ca-aos              # PQC-based AOS CA app
├── sdks
│   └── did-client-sdk-aos      # PQC-based AOS Client SDK
└── servers
    └── did-sandbox-server      # PQC-based Mock Server (Sandbox)
```

| Name | Description |
|:-----|:------------|
| **`apps/did-ca-aos`** | **PQC-based** Open DID Android CA app — wires the PQC-enabled Client SDK and bundles BouncyCastle 1.80 alongside the legacy SpongyCastle stack so ML-DSA-44 / ML-KEM-768 flows resolve at runtime. |
| **`sdks/did-client-sdk-aos`** | **PQC-based** Open DID Android Client SDK — adds `MlDsa44Manager` and `MlKem768Manager` (FIPS 204 / FIPS 203) parallel to the existing Secp256r1 key managers, keeping classical paths working. |
| **`servers/did-sandbox-server`** | **PQC-based** Open DID Sandbox Mock Server — bundles the Phase 1 PQC-applied Server SDK JARs and toggles algorithms via `pqc.signature-algorithm` (`MlDsa44`) / `pqc.key-agreement-algorithm` (`MlKem768`) in `application.yml`. |

## PQC Change Summary

For a categorized rundown of what changed in the upstream OmniOne ID AOS components when applying ML-DSA-44 / ML-KEM-768, see [`docs/phase2/pqc-aos-changes.md`](../docs/phase2/pqc-aos-changes.md).

For a step-by-step comparison of how the actual request/response payloads change between the ECC and ML-DSA/KEM paths across the main Open DID protocols (Wallet/User registration, VC issuance, VP submission), see [`docs/phase2/pqc-payload-compare.md`](../docs/phase2/pqc-payload-compare.md).

## Setup Guides

For build environment, dependency setup, and run instructions of each component, refer to the per-folder README:

- [`apps/did-ca-aos/README.md`](apps/did-ca-aos/README.md) — PQC-based AOS CA app: build/run via Android Studio, BouncyCastle 1.80 setup, and PQC-related Gradle configuration.
- [`sdks/did-client-sdk-aos/source/did-wallet-sdk-aos/README.md`](sdks/did-client-sdk-aos/source/did-wallet-sdk-aos/README.md) — PQC-based AOS Client SDK: jar build (`./gradlew exportJar`), provider stack notes (SpongyCastle + BC 1.80), and `MlDsa44Manager` / `MlKem768Manager` integration details.
- [`servers/did-sandbox-server/README.md`](servers/did-sandbox-server/README.md) — PQC-based Mock Server: Java 21 / Spring Boot 3.2.4 runtime, bundled Phase 1 PQC Server SDK JARs, and `application.yml` PQC algorithm toggles.
