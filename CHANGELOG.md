# Changelog

## v1.0.0 (2026-04-30)

### 🚀 New Features

- Phase 1 — Basic Level PoC

    - Applied ML-DSA-44 (FIPS 204) to DID Document and VC issuance/verification components on top of the four Open DID Server SDKs.

    - Applied ML-KEM-768 (FIPS 203) to the Key Agreement component, with size and performance comparisons against Secp256r1 / ECDH.

    - Added auxiliary validation outside the JVM (Foundry/Solidity on-chain verification, SwiftPM/iOS benchmark).

- Phase 2 — Mock Integration PoC

    - Applied ML-DSA-44 / ML-KEM-768 to the AOS Client SDK (`MlDsa44Manager`, `MlKem768Manager`) and the AOS CA app, alongside the existing Secp256r1 path.

    - Provided a Sandbox (Mock) Server with PQC algorithm toggles, validating end-to-end Wallet/User registration, VC issuance, and VP submission.

    - Introduced a hybrid BIO key protection scheme that wraps ML-DSA-44 private keys with an AES-GCM key from AndroidKeystore.

- Phase 3 — Open DID v2.0 Integration PoC

    - Applied ML-DSA-44 / ML-KEM-768 to the production Open DID v2.0 server stack (TA, CA, Issuer, Verifier, Wallet) and validated end-to-end with the AOS CA app.

    - Provided automation scripts (`server-scripts/`) for wallet, DID, and per-server configuration setup with PQC algorithm toggles.

- Documentation

    - Added per-phase result, sequence, and change-analysis documents under `docs/phase{1,2,3}` (Korean / English).

    - Added a final summary covering applied scope, code change examples, payload-size impact, and follow-up tasks.
