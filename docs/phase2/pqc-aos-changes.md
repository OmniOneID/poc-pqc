# AOS PQC Adoption Changes

A summary of the changes — broken down by SDK and app — that came out of applying ML-DSA-44 / ML-KEM-768 to the official Open DID AOS components.

- **Upstream baselines**: [`OmniOneID/did-client-sdk-aos`](https://github.com/OmniOneID/did-client-sdk-aos), [`OmniOneID/did-ca-aos`](https://github.com/OmniOneID/did-ca-aos)
- **Change footprint**: SDK +761 / -188 (19 files), CA app +426 / -50 (14 files)

## Common Change Pattern

- **Crypto libraries used in parallel** — The existing SpongyCastle 1.54.0.0 (`org.spongycastle.*`) is left in place, while only the PQC branches use BouncyCastle 1.80 (`org.bouncycastle:bcprov-jdk18on`). SpongyCastle 1.54 does not support ML-DSA / ML-KEM (which require BC 1.78+), so the dual-stack arrangement is unavoidable.
- **Branch-extension approach** — No new abstractions are introduced; instead, `MlDsa44Manager` and `MlKem768Manager` are added next to the existing `Secp256R1Manager`, and the call sites are unified via the `SignableInterface` polymorphism.
- **Algorithm selection** — Toggled via `Config` static fields (`SIGNATURE_ALGORITHM`, `KEY_AGREEMENT_ALGORITHM`). Existing keys/DIDs must be re-registered after a change.

---

## 1. did-client-sdk-aos

### 1.1 Build (`source/did-wallet-sdk-aos/build.gradle`)

```groovy
// Existing SpongyCastle is kept
implementation 'com.madgag.spongycastle:core:1.54.0.0'
// ... (prov / pkix / pg)

// BouncyCastle 1.80 for ML-DSA-44 (PQC) support
implementation 'org.bouncycastle:bcprov-jdk18on:1.80'
```

### 1.2 Enum Additions

| File | Added |
|---|---|
| `datamodel/common/enums/AlgorithmType.java` | `ML_DSA_44("MlDsa44")`, `ML_KEM_768("MlKem768")` + getValue / DIDKeyType mapping branch |
| `datamodel/common/enums/ProofType.java` | `mlDsa44Signature2024("MlDsa44Signature2024")` + AlgorithmType ↔ ProofType mapping |
| `datamodel/did/DIDKeyType.java` | `mlDsa44VerificationKey2024`, `mlKem768AgreementKey2024` + bidirectional mapping |

### 1.3 New Key Managers

Added under `core/keymanager/supportalgorithm/`, alongside `Secp256R1Manager` / `Secp256K1Manager`:

- `MlDsa44Manager.java` — key generation, sign, verify, `checkKeyPairMatch`
- `MlKem768Manager.java` — key generation, encap / decap, `checkKeyPairMatch`

Both managers, in their static initializers, run `Security.removeProvider("BC")` and then register `BouncyCastleProvider` (1.80). SpongyCastle (`SC`) is left untouched.

### 1.4 Data Model / Protocol VOs

- New: `datamodel/security/AccMlKem.java`, `datamodel/security/ReqMlKem.java`
- Modified:
  - `datamodel/profile/ReqE2e.java` — added `algorithm` field + `isMlKem()` helper
  - `datamodel/security/AccE2e.java` — added fields for the ML-KEM branch
  - Six `datamodel/protocol/P132/P210/P220 Request·Response Vo` classes — added `reqMlKem` / `accMlKem` fields with getters/setters

### 1.5 Core API (Major Changes)

| File | Lines changed | Key change |
|---|---|---|
| `core/api/KeyManager.java` | +229 | Direct `Secp256R1Manager` calls replaced with polymorphic `SignableInterface` calls. Added handling for the ML-DSA-44 biometric wrap format (`[IV(12B) ‖ AES-GCM ciphertext]`). |
| `core/api/WalletCore.java` | +203 | Per-key-type branching, integration of the ML-KEM flow |
| `core/api/WalletService.java` | +125 | Call branching driven by algorithm selection |

### 1.6 Common Configuration (`wallet/walletservice/config/Config.java`)

```java
private static AlgorithmType.ALGORITHM_TYPE SIGNATURE_ALGORITHM    = AlgorithmType.ALGORITHM_TYPE.ML_DSA_44;
private static AlgorithmType.ALGORITHM_TYPE KEY_AGREEMENT_ALGORITHM = AlgorithmType.ALGORITHM_TYPE.ML_KEM_768;

public static AlgorithmType.ALGORITHM_TYPE getSignatureAlgorithm() { ... }
public static boolean isMlKemKeyAgreement() {
    return KEY_AGREEMENT_ALGORITHM == AlgorithmType.ALGORITHM_TYPE.ML_KEM_768;
}
```

### 1.7 BIO Hybrid Key Protection (Core Design Change for PQC Adoption)

The existing Open DID Android implementation generated the BIO key as a **Secp256r1 ECDSA key created directly inside AndroidKeystore** (`setUserAuthenticationRequired(true)`). The key never left the Keystore, signing was permitted only at the moment a BiometricPrompt was satisfied, and the hardware isolation of TEE/StrongBox was used as-is.

**Problem** — AndroidKeystore does not support ML-DSA-44 as an algorithm. There is no path to keep a PQC key directly in hardware.

**Solution: a hybrid structure that wraps the PQC private key with an AES-GCM Keystore key**

```
┌─────────────────────────────────────────────────────┐
│  AndroidKeystore (TEE / StrongBox)                  │
│  ┌─────────────────────────────────────────────┐    │
│  │  AES-256-GCM wrapping key                   │    │   ← Cannot leave the Keystore
│  │  - PURPOSE_ENCRYPT | PURPOSE_DECRYPT        │    │   ← per-use biometric auth (timeout=0,
│  │  - userAuthenticationRequired = true        │    │     AUTH_BIOMETRIC_STRONG)
│  │  - invalidatedByBiometricEnrollment = false │    │   ← survives fingerprint re-enrollment
│  │  - StrongBox (when available)                │    │
│  └─────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────┘
                       ↑ wrap / unwrap
                       │  (only Cipher instances authenticated through BiometricPrompt.CryptoObject)
┌─────────────────────────────────────────────────────┐
│  App general storage (DetailKeyInfo)                │
│  ┌─────────────────────────────────────────────┐    │
│  │  Wrapped Blob = [ IV (12B) ‖ Ciphertext+Tag(16B) ] │
│  │   - plaintext: ML-DSA-44 PrivateKey (~2560B)│    │
│  │   - encrypted with AES/GCM/NoPadding         │    │
│  └─────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────┘
```

**Why "hybrid"** — Two algorithms and two protection layers are intentionally combined:

- **Hardware side**: a classic symmetric (AES-GCM) key locked in the Keystore, usable only after a BiometricPrompt passes.
- **Software side**: the PQC (ML-DSA-44) private key exists in memory or on disk in wrapped form.
- Net effect: even though the PQC key itself cannot live inside the Keystore, the **authority to unlock the PQC key** is gated by Keystore + biometrics.

**Implementation Points**

| Location | Added/Changed |
|---|---|
| `KeystoreManager.BIO_WRAPPING_KEY_ALIAS_PREFIX` | Wrapping AES key alias prefix (`opendid_wallet_bio_wrap_`) |
| `KeystoreManager.generateOrGetBioWrappingKey(ctx, alias)` | Creates/retrieves the AES-256-GCM Keystore key. Uses `setUserAuthenticationParameters(0, AUTH_BIOMETRIC_STRONG)`, with StrongBox enabled when available. |
| `KeyManager.generateBioMlDsaKey(id, algo, encCipher)` | Generates the PQC key via `MlDsa44Manager` → wraps it with the authenticated `Cipher` (ENCRYPT_MODE) → stores the `[IV ‖ ct]` blob in `DetailKeyInfo`. |
| `KeyManager.getBioWrappedIv(id)` | Extracts only the 12-byte IV right before signing. Used in `Cipher.init(DECRYPT_MODE, key, GCMParameterSpec(128, iv))`. |
| `KeyManager.unlockBioMlDsaKey(id, decCipher)` | Restores the wrapped PQC private key with the authenticated DECRYPT Cipher. Held temporarily in the `unlockedBioPrivateKey` field. |
| `KeyManager.clearUnlockedBioKey()` | Immediately zeroes the in-memory plaintext private key with `Arrays.fill(..., 0x00)`. |
| `WalletCore.registerBioKeyHybrid(ctx)` | The PQC branch. Selected (vs. the existing `registerBioKeyLegacy(ctx)` — Secp256r1 + direct Keystore storage) according to algorithm choice. |
| `BioPromptHelper.getLastAuthenticatedCipher()` / `clearLastAuthenticatedCipher()` | Extensions that surface the authenticated Cipher instance from the BiometricPrompt callback to the caller. |

**One-Sign Flow**

1. From the stored wrapped blob, peel off the IV (12B) → `Cipher.init(DECRYPT_MODE, wrapKey, gcmSpec(IV))`
2. Authenticate the user with `BiometricPrompt.CryptoObject(cipher)`
3. On success the cipher becomes active → `cipher.doFinal(ct)` → recover the plaintext ML-DSA-44 private key
4. Run `MlDsa44Manager.sign(...)`
5. Call `clearUnlockedBioKey()` to zeroize the plaintext private key

**Security Properties / Limits**

- **Strengths**: The AES wrap key is locked inside the Keystore, so without biometric authentication the PQC private key cannot be decrypted on any path. Even on a rooted device, the window in which the plaintext sits in memory is bounded to *just before signing*.
- **Limits**: At signing time the plaintext PQC private key inevitably lives in RAM. This is an unavoidable trade-off until AndroidKeystore supports PQC directly.
- **Re-authentication frequency**: per-use (`timeout=0`) requires a BiometricPrompt for every signature. For cases like the PIN+BIO combined flow right after enrollment, `unlockedBioPrivateKey` is briefly retained to skip an extra prompt (a deliberate optimization).

---

## 2. did-ca-aos

### 2.1 Build (`source/did-ca-aos/app/build.gradle`)

```groovy
android {
    packagingOptions {
        resources { pickFirsts += ['org/bouncycastle/**'] }
    }
}

configurations.all {
    // Block transitive pulls of older BC modules
    exclude group: 'org.bouncycastle', module: 'bcprov-jdk15to18'
}

dependencies {
    implementation project(':did-wallet-sdk-aos')   // existing fileTree(libs/*.jar) is commented out

    implementation('org.bitcoinj:bitcoinj-core:0.15.7') {
        exclude group: 'org.bouncycastle'           // block the BC pulled in by bitcoinj
    }

    // BouncyCastle 1.80 for ML-DSA-44 (PQC) support
    implementation 'org.bouncycastle:bcprov-jdk18on:1.80'
}
```

The Dev/Prod URL split block in `app/build.gradle` is also collapsed into a single `serverUrl + ":8084"`, switching the configuration to the Phase 2 mock server integration.

### 2.2 Config (`app/src/main/java/org/omnione/did/ca/config/Config.java`)

- Adds `SIGNATURE_ALGORITHM`, `KEY_AGREEMENT_ALGORITHM` constants (`"MlDsa44"`, `"MlKem768"`)
- Introduces the TAS endpoint `REQUEST_ML_KEM = "/tas/api/v1/request-ml-kem"`

### 2.3 Protocol Behavior Changes

ML-KEM flow branches were added in classes under `network/protocol/`:

- `BaseOperation.java` (+92) — Branches on `Config.isMlKemKeyAgreement()` to call the new `requestMLKEM()` method. Coexists with the existing ECDH path.
- `protocol/user/RegUser.java` (+74), `protocol/vc/IssueVc.java` (+79), `protocol/vc/RevokeVc.java` (+69), `RestoreUser.java`, etc. — Generate an ephemeral ML-KEM-768 key pair → derive the shared secret with `MlKem768Manager.decapsulate(...)` from the server's `accMlKem.ciphertext` → establish the subsequent E2E channel.
- `ProtocolData.java`, `UserDidUpdate.java`, `UserRegistration.java`, `VcIssuance.java`, `TokenUtil.java`, `SplashActivity.java`, etc. — Add helper/utility branches that follow the algorithm chosen in Config.

### 2.4 ML-KEM Key Exchange Flow (Simplified)

1. The Wallet generates an ephemeral ML-KEM-768 key pair.
2. It sends its public key inside `reqMlKem` (unlike Phase 1, no prior TAS DID Doc lookup — Mock simplification).
3. The server runs encap and returns `accMlKem.ciphertext`.
4. The Wallet derives the shared secret via `decapsulate`, immediately discarding the ephemeral sk.

---

## 3. Known Issues / Operational Notes

- **Algorithm toggling is at compile-time-constant level** — After changing the `Config` static fields, existing keys/DIDs must be re-registered. The runtime dynamic-switch case has not been validated.
- **SpongyCastle ↔ BC 1.80 dual registration** — The two providers coexist in the JCE registry. Call sites are responsible for explicitly routing the ECDSA path through `SC` and the PQC path through `BC`.
