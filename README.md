# Open DID PQC (Post-Quantum Cryptography) PoC

Welcome to the Open DID PQC PoC.
This repository is a Proof-of-Concept project for applying Post-Quantum Cryptography (PQC) to the Open DID platform.

## Goals

* Validate the feasibility of migrating the Open DID DID/VC ecosystem to PQC in preparation for the quantum-computing threat
* Implement DID Document and VC issuance/verification on top of **ML-DSA-44 (FIPS 204)** digital signatures
* Implement quantum-safe Key Encapsulation using **ML-KEM-768 (FIPS 203)**
* Identify the per-layer scope of PQC changes across Open DID and assess technical viability

## Algorithm Selection Rationale

The W3C CCG [Data Integrity Quantum Safe Cryptosuites](https://w3c-ccg.github.io/di-quantum-safe/) specification defines four PQC signature algorithms.

| Algorithm | Basis | Standardization Status |
|:----------|:------|:-----------------------|
| **ML-DSA** | Module Lattice | NIST standard |
| SLH-DSA | Stateless Hash-based | NIST standard |
| Falcon | NTRU Lattice | In progress |
| SQISign | Isogeny-based | Academic research stage |

This PoC selects **ML-DSA-44**. It is the most likely candidate to become the de facto standard going forward, and we consider it a reasonable representative algorithm for this PoC for the following reasons:

- **NIST standardization complete** — Falcon is still in progress; SQISign is at the academic research stage.
- **BouncyCastle support** — ML-DSA-44 is supported from BC 1.79. SLH-DSA is also supported, but its signatures are roughly 3× the size of ML-DSA's, which is impractical for VC payloads. Falcon and SQISign are not supported by BC.
- **Reasonable signature size** — ~2,420 bytes for ML-DSA-44, about one-third the size of SLH-DSA's.

ML-DSA is the only algorithm that simultaneously satisfies all three: NIST standard + BC support + reasonable size.

For key exchange (KEM), this PoC adopts **ML-KEM-768**, which sits in the same module-lattice family. Since 2024, ML-KEM has been adopted as the default algorithm for hybrid key exchange in TLS 1.3 by major web browsers and CDNs, and is already deployed at a scale that handles a substantial share of overall web traffic.

> The only PQC algorithms deployed at production scale today are lattice-based.

## Phase Framework

The PoC is structured as **three phases that progressively widen the validation scope**.

| Phase | Purpose | Components | Path |
|:------|:--------|:-----------|:-----|
| **1** | Per-component technical feasibility on Open DID basics | Server-side DID Doc, VC/VP, Key Agreement components | [phase1-basic-level/](phase1-basic-level/README.md) |
| **2** | Wallet-centric Open DID protocol validation | AOS CA app, Client SDK, Sandbox (Mock) Server | [phase2-mock-integration/](phase2-mock-integration) |
| **3** | Validation against production Open DID v2.0 servers + performance measurement | AOS CA app, Client SDK, Open DID v2.0 Servers + performance metrics | [phase3-opendid-v2-integration/](phase3-opendid-v2-integration) |

> Phase 1 answers "can PQC be used as a basic component?", Phase 2 answers "does it work over the real device/server protocol?", and Phase 3 answers "is it usable in a real operational environment, performance included?"

## Folder Structure

```
poc-pqc
├── phase1-basic-level/           # Per-component PoC
│   ├── sdks/                     # Server SDKs with PQC applied
│   ├── servers/                  # PQC component servers
│   └── etc/                      # Swift, Solidity, and other component tests
├── phase2-mock-integration/      # AOS app ↔ Mock Server integration (Open DID protocol)
│   ├── apps/                     # did-ca-aos (Android CA app)
│   ├── sdks/                     # did-client-sdk-aos (Android Client SDK)
│   └── servers/                  # did-sandbox-server (Mock Server)
├── phase3-opendid-v2-integration/ # AOS app ↔ real Open DID v2.0 servers + performance measurement
└── docs/                         # Per-phase result, sequence, and change-analysis documents
```

## Common Tech Stack

| Category | Version / Spec |
|:---------|:---------------|
| PQC algorithms | ML-DSA-44 (FIPS 204), ML-KEM-768 (FIPS 203) |
| DID Document format | Open DID DID Document |
| VC format | Open DID VC |

## References

| Category | Link |
|:---------|:-----|
| NIST FIPS 203 (ML-KEM) | [Specification](https://csrc.nist.gov/pubs/fips/203/final) |
| NIST FIPS 204 (ML-DSA) | [Specification](https://csrc.nist.gov/pubs/fips/204/final) |
| W3C CCG Data Integrity Quantum Safe | [Specification](https://w3c-ccg.github.io/di-quantum-safe/) |
| ML-DSA for JOSE and COSE | [IETF Draft](https://datatracker.ietf.org/doc/draft-ietf-cose-dilithium) |
| ML-DSA in X.509 | [RFC 9881](https://datatracker.ietf.org/doc/rfc9881/) |

## Contributing

For contribution procedures and the code of conduct, please refer to [CONTRIBUTING.md](CONTRIBUTING.md) and [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md).

## License

[Apache 2.0](LICENSE)
