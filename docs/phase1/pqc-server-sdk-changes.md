# PQC Server SDK Change Analysis

## Table of Contents

- [Change Summary](#change-summary)
  - [Common Change Pattern](#common-change-pattern)
  - [Common Configuration](#common-configuration)
- [1. did-crypto-sdk-server](#1-did-crypto-sdk-server)
  - [1.1 Type Definitions](#11-type-definitions)
  - [1.2 Error Codes](#12-error-codes)
  - [1.3 Logic Additions (New Files)](#13-logic-additions-new-files)
  - [1.4 Logic Additions (Branches in Existing Files)](#14-logic-additions-branches-in-existing-files)
- [2. did-wallet-sdk-server](#2-did-wallet-sdk-server)
  - [2.1 Type Definitions](#21-type-definitions)
  - [2.2 Logic Additions (New Files)](#22-logic-additions-new-files)
  - [2.3 Logic Additions (Key Restoration)](#23-logic-additions-key-restoration)
  - [2.4 Logic Additions (Sign · Key Generation · Storage)](#24-logic-additions-sign--key-generation--storage)
- [3. did-datamodel-sdk-server](#3-did-datamodel-sdk-server)
  - [3.1 Type Definitions](#31-type-definitions)
- [4. did-core-sdk-server](#4-did-core-sdk-server)
  - [4.1 Logic Additions (Mapping)](#41-logic-additions-mapping)
  - [4.2 Logic Additions (Verification Branch)](#42-logic-additions-verification-branch)
---

## Change Summary

### Common Change Pattern

Because BouncyCastle 1.79+ exposes ML-DSA-44 and ML-KEM-768 through the standard `java.security` API, integration is possible by simply adding `else if` / `case` branches alongside the existing ECC/RSA branches — no separate PQC library integration is required:

```java
// Existing
if (SECP256r1) { ... }
else if (RSA2048) { ... }

// PQC addition — uses the java.security API, only the algorithm name "ML-DSA-44" changes
else if (ML_DSA_44) { ... }
```

> In other words, the heart of the PQC migration is **extending existing branches, not introducing a new cryptographic system** — algorithms can be added without any change to the SDK architecture.

### Common Configuration

| Target | Change |
|--------|--------|
| `build.gradle` | Upgrade BouncyCastle 1.78.1 → **1.79** (ML-DSA-44, ML-KEM-768 support) |

```groovy
implementation 'org.bouncycastle:bcprov-jdk18on:1.79' // 1.78.1 → 1.79
```

> ML-DSA-44 and ML-KEM-768 are supported starting with BouncyCastle 1.79+.

---

## 1. did-crypto-sdk-server

Adds ML-DSA-44 key generation, signing, and verification.

### 1.1 Type Definitions

| File | Change |
|------|--------|
| `enums/DidKeyType.java` | Add ML-DSA-44 key type enum |

```java
// Existing: RSA_VERIFICATION_KEY_2018, SECP256K1_..., SECP256R1_...
ML_DSA_44_VERIFICATION_KEY_2024("MlDsa44VerificationKey2024");
```

### 1.2 Error Codes

| File | Change |
|------|--------|
| `exception/CryptoErrorCode.java` | Add error codes specific to PQC sign/verify |

```java
ERR_CODE_SIGNATUREUTIL_PQC_SIGN_FAIL(ERR_CODE_SIGNATUREUTIL_BASE, "007", "Failed to generate PQC signature"),
ERR_CODE_SIGNATUREUTIL_PQC_VERIFY_FAIL(ERR_CODE_SIGNATUREUTIL_BASE, "008", "Failed to verify PQC signature"),
```

> The existing ECC path runs inside `EccSignatureProvider` and surfaces only EC-related errors, but the PQC path calls the `java.security.Signature` API directly, so dedicated error codes are required.

### 1.3 Logic Additions (New Files)

| File | Change |
|------|--------|
| `keypair/MlDsaKeyPair.java` | ML-DSA-44 KeyPair implementation **(new)** |
| `generator/MlDsaKeyPairGenerator.java` | ML-DSA-44 key pair generator **(new)** |

```java
// MlDsaKeyPair.java — implements KeyPairInterface
public class MlDsaKeyPair implements KeyPairInterface {
    private PublicKey publicKey;
    private PrivateKey privateKey;
    // getter/setter + getBase58PubKey()
}

// MlDsaKeyPairGenerator.java — BouncyCastle ML-DSA-44 key generation
public KeyPairInterface generateKeyPair() throws CryptoException {
    KeyPairGenerator kpg = KeyPairGenerator.getInstance("ML-DSA-44", "BC");
    java.security.KeyPair keyPair = kpg.generateKeyPair();
    return new MlDsaKeyPair(keyPair.getPublic(), keyPair.getPrivate());
}
```

### 1.4 Logic Additions (Branches in Existing Files)

| File | Change |
|------|--------|
| `util/CryptoUtils.java` | Add ML-DSA-44 branch in key generation |
| `util/SignatureUtils.java` | Add ML-DSA-44 sign and verify methods |

```java
// CryptoUtils.java — inside generateKeyPair()
else if (DidKeyType.ML_DSA_44_VERIFICATION_KEY_2024 == didKeyType)
{
    return mlDsaKeyPairGenerator.generateKeyPair();
}

// SignatureUtils.java — ML-DSA-44 sign
public static byte[] generateMlDsa44Signature(PrivateKey privateKey, byte[] data) throws CryptoException {
    Signature signer = Signature.getInstance("ML-DSA-44", "BC");
    signer.initSign(privateKey);
    signer.update(data);
    return signer.sign();  // unlike ECC, this is direct sign, not hash-then-sign
}

// SignatureUtils.java — ML-DSA-44 verify
public static void verifyMlDsa44Signature(byte[] publicKeyBytes, byte[] data, byte[] signatureBytes) throws CryptoException {
    KeyFactory keyFactory = KeyFactory.getInstance("ML-DSA-44", "BC");
    PublicKey publicKey = keyFactory.generatePublic(new X509EncodedKeySpec(publicKeyBytes));
    Signature verifier = Signature.getInstance("ML-DSA-44", "BC");
    verifier.initVerify(publicKey);
    verifier.update(data);
    if (!verifier.verify(signatureBytes)) {
        throw new CryptoException(CryptoErrorCode.ERR_CODE_SIGNATUREUTIL_PQC_VERIFY_FAIL, "...");
    }
}
```

---

## 2. did-wallet-sdk-server

Adds ML-DSA-44 key generation, storage, restoration, and signing support.

### 2.1 Type Definitions

| File | Change |
|------|--------|
| `key/data/CryptoKeyPairInfo.java` | Add ML-DSA-44 to `KeyAlgorithmType` enum and branch in KeyPair construction |

```java
// enum addition
SECP256k1("Secp256k1"), SECP256r1("Secp256r1"), RSA2048("Rsa2048"),
ML_DSA_44("MlDsa44");

// PQC/EC KeyPair branch in the constructor
if (KeyAlgorithmType.ML_DSA_44.getRawValue().equals(algorithm)) {
    keyPair = new MlDsaKeyPair(publicKey, privateKey);
} else {
    keyPair = new EcKeyPair(publicKey, privateKey);
}
```

### 2.2 Logic Additions (New Files)

| File | Change |
|------|--------|
| `key/data/MlDsaKeyPair.java` | ML-DSA-44 KeyPair data class **(new)** |

```java
// Same shape as the crypto SDK's MlDsaKeyPair, for the wallet layer
public class MlDsaKeyPair {
    private PublicKey publicKey;
    private PrivateKey privateKey;
    // getter/setter
}
```

### 2.3 Logic Additions (Key Restoration)

| File | Change |
|------|--------|
| `crypto/encryption/AbstractEncryptionHelper.java` | Handle PQC algorithms without an ECC curve |
| `crypto/encryption/EncryptionHelper.java` | Add ML-DSA-44 PrivateKey/PublicKey restoration methods |

```java
// AbstractEncryptionHelper.java — ECC curve resolution branch
} else if ("MlDsa44".equals(algorithm)) { // PQC has no ECC curve
    return null;
}

// EncryptionHelper.java — PQC PrivateKey restoration (PKCS8 → PrivateKey)
public PrivateKey getMlDsaPrivateKeyObject(byte[] privateKeyBytes) throws WalletException {
    KeyFactory keyFactory = KeyFactory.getInstance("ML-DSA-44", "BC");
    return keyFactory.generatePrivate(new PKCS8EncodedKeySpec(privateKeyBytes));
}

// EncryptionHelper.java — PQC PublicKey restoration (X509 → PublicKey)
public PublicKey getMlDsaPublicKeyObject(byte[] publicKeyBytes) throws WalletException {
    KeyFactory keyFactory = KeyFactory.getInstance("ML-DSA-44", "BC");
    return keyFactory.generatePublic(new X509EncodedKeySpec(publicKeyBytes));
}
```

### 2.4 Logic Additions (Sign · Key Generation · Storage)

| File | Change |
|------|--------|
| `crypto/sign/AbstractSignatureHelper.java` | Branch mapping key algorithm → DidKeyType |
| `key/adapter/WalletManagerAdapter.java` | Branches for PQC key restoration, signing, public key storage |
| `key/impl/WalletManagerImpl.java` | Branch mapping DidKeyType during key generation |

```java
// AbstractSignatureHelper.java — algorithm → DidKeyType mapping
} else if (keyAlgorithm.equals(KeyAlgorithmType.ML_DSA_44.getRawValue()))
{
    return DidKeyType.ML_DSA_44_VERIFICATION_KEY_2024;
}

// WalletManagerAdapter.java — PQC sign (calls SignatureUtils directly instead of ECC CompactSign)
if (KeyAlgorithmType.ML_DSA_44.getRawValue().equals(keyPairInfo.getAlgorithm())) {
    return SignatureUtils.generateMlDsa44Signature(
            keyPairInfo.getKeyPair().getPrivateKey(), hashedSource);
}
// Existing: signatureHelper.sign() + getCompactSignature() — ECC only

// WalletManagerAdapter.java — PQC public key storage (X509 as-is, no compression)
if (KeyAlgorithmType.ML_DSA_44.getRawValue().equals(info.getAlgorithm()))
{
    return info.getPublicKey().getEncoded();  // no compression
}
// Existing: encryptionHelper.getCompressedPublicKey() — ECC compression

// WalletManagerImpl.java — key generation branch
} else if (KeyAlgorithmType.ML_DSA_44.equals(keyAlgorithmType)) {
    return DidKeyType.ML_DSA_44_VERIFICATION_KEY_2024;
}
```

---

## 3. did-datamodel-sdk-server

Defines ML-DSA-44 related enum types.

### 3.1 Type Definitions

| File | Change |
|------|--------|
| `enums/did/DidKeyType.java` | Add ML-DSA-44 key type enum |
| `enums/did/ProofType.java` | Add ML-DSA-44 signature type enum |

```java
// DidKeyType.java — existing: RSA_..., SECP256K1_..., SECP256R1_...
ML_DSA_44_VERIFICATION_KEY_2024("MlDsa44VerificationKey2024"); // forten

// ProofType.java — existing: RSA_SIGNATURE_2018, SECP256K1_..., SECP256R1_...
ML_DSA_44_SIGNATURE_2024("MlDsa44Signature2024"); // forten
```

---

## 4. did-core-sdk-server

ML-DSA-44 proof type mapping and signature verification branches.

### 4.1 Logic Additions (Mapping)

| File | Change |
|------|--------|
| `manager/DidManager.java` | Add a key type → signature type mapping case in DID proof creation |
| `manager/VcManager.java` | Add a key type → signature type mapping case in VC proof creation |

```java
// DidManager.java — DID Document proof creation switch
case "MlDsa44VerificationKey2024":
    proof.setType(ProofType.ML_DSA_44_SIGNATURE_2024.getRawValue());
    break;

// VcManager.java — VC proof creation switch
case ML_DSA_44_VERIFICATION_KEY_2024:
    proofType = ProofType.ML_DSA_44_SIGNATURE_2024;
    return proofType.getRawValue();
```

### 4.2 Logic Additions (Verification Branch)

| File | Change |
|------|--------|
| `util/VerifyUtil.java` | Add ML-DSA-44 detection helper + signature verification branch |

```java
// ML-DSA-44 algorithm detection
private static boolean isMlDsa44(String sigAlgorithm) {
    ProofType proofType = ProofType.fromString(sigAlgorithm);
    return proofType == ProofType.ML_DSA_44_SIGNATURE_2024;
}

// Inside verifySignature() — insert PQC branch before ECC verification
if (isMlDsa44(sigParams.getAlgorithm()))
{
    byte[] originData = sigParams.getOriginData().getBytes(UTF_8);
    SignatureUtils.verifyMlDsa44Signature(publicKeyBytes, originData, signatureByte);
    return;  // skip the ECC path
}
// Existing ECC signature verification (continues below)
```

---
