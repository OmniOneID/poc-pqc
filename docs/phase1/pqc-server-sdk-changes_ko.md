# PQC Server SDK 변경 사항 분석

## 목차

- [변경 사항 요약](#변경-사항-요약)
  - [공통 변경 패턴](#공통-변경-패턴)
  - [공통 설정](#공통-설정)
- [1. did-crypto-sdk-server](#1-did-crypto-sdk-server)
  - [1.1 타입 정의](#11-타입-정의)
  - [1.2 에러코드](#12-에러코드)
  - [1.3 로직 추가 (신규 파일)](#13-로직-추가-신규-파일)
  - [1.4 로직 추가 (기존 파일 분기)](#14-로직-추가-기존-파일-분기)
- [2. did-wallet-sdk-server](#2-did-wallet-sdk-server)
  - [2.1 타입 정의](#21-타입-정의)
  - [2.2 로직 추가 (신규 파일)](#22-로직-추가-신규-파일)
  - [2.3 로직 추가 (키 복원)](#23-로직-추가-키-복원)
  - [2.4 로직 추가 (서명·키 생성·저장)](#24-로직-추가-서명키-생성저장)
- [3. did-datamodel-sdk-server](#3-did-datamodel-sdk-server)
  - [3.1 타입 정의](#31-타입-정의)
- [4. did-core-sdk-server](#4-did-core-sdk-server)
  - [4.1 로직 추가 (매핑)](#41-로직-추가-매핑)
  - [4.2 로직 추가 (검증 분기)](#42-로직-추가-검증-분기)
---

## 변경 사항 요약

### 공통 변경 패턴

BouncyCastle 1.79+에서 ML-DSA-44, ML-KEM-768을 `java.security` 표준 API로 지원하기 때문에, 별도의 PQC 라이브러리 통합 없이 기존 ECC/RSA 분기에 `else if` / `case` 를 추가하는 수준으로 구현이 가능합니다:

```java
// 기존
if (SECP256r1) { ... }
else if (RSA2048) { ... }

// PQC 추가 — java.security API 사용, 알고리즘명만 "ML-DSA-44"로 변경
else if (ML_DSA_44) { ... }
```

> 즉, PQC 전환의 핵심은 **새로운 암호 체계 도입이 아니라 기존 분기 확장**이며, SDK 아키텍처 변경 없이 알고리즘 추가만으로 대응 가능합니다.

### 공통 설정

| 대상 | 변경 내용                                                         |
|------|---------------------------------------------------------------|
| `build.gradle` | BouncyCastle 1.78.1 → **1.79** 업그레이드 (ML-DSA-44, ML-KEM-768 지원) |

```groovy
implementation 'org.bouncycastle:bcprov-jdk18on:1.79' // 1.78.1 → 1.79
```

> ML-DSA-44, ML-KEM-768는 BouncyCastle 1.79+ 에서 지원됩니다.

---

## 1. did-crypto-sdk-server

ML-DSA-44 키 생성·서명·검증 추가.

### 1.1 타입 정의

| 파일 | 변경 내용 |
|------|-----------|
| `enums/DidKeyType.java` | ML-DSA-44 키 타입 enum 추가 |

```java
// 기존: RSA_VERIFICATION_KEY_2018, SECP256K1_..., SECP256R1_...
ML_DSA_44_VERIFICATION_KEY_2024("MlDsa44VerificationKey2024");
```

### 1.2 에러코드

| 파일 | 변경 내용 |
|------|-----------|
| `exception/CryptoErrorCode.java` | PQC 서명/검증 전용 에러코드 추가 |

```java
ERR_CODE_SIGNATUREUTIL_PQC_SIGN_FAIL(ERR_CODE_SIGNATUREUTIL_BASE, "007", "Failed to generate PQC signature"),
ERR_CODE_SIGNATUREUTIL_PQC_VERIFY_FAIL(ERR_CODE_SIGNATUREUTIL_BASE, "008", "Failed to verify PQC signature"),
```

> 기존 ECC는 `EccSignatureProvider` 내부에서 동작하며 EC 관련 에러만 리턴되었지만, PQC는 `java.security.Signature` API를 직접 호출하므로 별도 에러코드가 필요합니다.

### 1.3 로직 추가 (신규 파일)

| 파일 | 변경 내용 |
|------|-----------|
| `keypair/MlDsaKeyPair.java` | ML-DSA-44 KeyPair 구현체 **(신규)** |
| `generator/MlDsaKeyPairGenerator.java` | ML-DSA-44 키 쌍 생성기 **(신규)** |

```java
// MlDsaKeyPair.java — KeyPairInterface 구현
public class MlDsaKeyPair implements KeyPairInterface {
    private PublicKey publicKey;
    private PrivateKey privateKey;
    // getter/setter + getBase58PubKey()
}

// MlDsaKeyPairGenerator.java — BouncyCastle ML-DSA-44 키 생성
public KeyPairInterface generateKeyPair() throws CryptoException {
    KeyPairGenerator kpg = KeyPairGenerator.getInstance("ML-DSA-44", "BC");
    java.security.KeyPair keyPair = kpg.generateKeyPair();
    return new MlDsaKeyPair(keyPair.getPublic(), keyPair.getPrivate());
}
```

### 1.4 로직 추가 (기존 파일 분기)

| 파일 | 변경 내용 |
|------|-----------|
| `util/CryptoUtils.java` | 키 생성 시 ML-DSA-44 분기 추가 |
| `util/SignatureUtils.java` | ML-DSA-44 서명 생성·검증 메서드 추가 |

```java
// CryptoUtils.java — generateKeyPair() 내부
else if (DidKeyType.ML_DSA_44_VERIFICATION_KEY_2024 == didKeyType)
{
    return mlDsaKeyPairGenerator.generateKeyPair();
}

// SignatureUtils.java — ML-DSA-44 서명 생성
public static byte[] generateMlDsa44Signature(PrivateKey privateKey, byte[] data) throws CryptoException {
    Signature signer = Signature.getInstance("ML-DSA-44", "BC");
    signer.initSign(privateKey);
    signer.update(data);
    return signer.sign();  // ECC와 달리 hash-then-sign이 아닌 직접 서명
}

// SignatureUtils.java — ML-DSA-44 서명 검증
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

ML-DSA-44 키 생성·저장·복원·서명 지원.

### 2.1 타입 정의

| 파일 | 변경 내용 |
|------|-----------|
| `key/data/CryptoKeyPairInfo.java` | `KeyAlgorithmType` enum에 ML-DSA-44 추가, KeyPair 생성 분기 |

```java
// enum 추가
SECP256k1("Secp256k1"), SECP256r1("Secp256r1"), RSA2048("Rsa2048"),
ML_DSA_44("MlDsa44");

// 생성자에서 PQC/EC KeyPair 분기
if (KeyAlgorithmType.ML_DSA_44.getRawValue().equals(algorithm)) {
    keyPair = new MlDsaKeyPair(publicKey, privateKey);
} else {
    keyPair = new EcKeyPair(publicKey, privateKey);
}
```

### 2.2 로직 추가 (신규 파일)

| 파일 | 변경 내용 |
|------|-----------|
| `key/data/MlDsaKeyPair.java` | ML-DSA-44 KeyPair 데이터 클래스 **(신규)** |

```java
// crypto SDK의 MlDsaKeyPair와 동일 구조, wallet 계층용
public class MlDsaKeyPair {
    private PublicKey publicKey;
    private PrivateKey privateKey;
    // getter/setter
}
```

### 2.3 로직 추가 (키 복원)

| 파일 | 변경 내용 |
|------|-----------|
| `crypto/encryption/AbstractEncryptionHelper.java` | PQC 알고리즘은 ECC 커브 없이 처리 |
| `crypto/encryption/EncryptionHelper.java` | ML-DSA-44 PrivateKey·PublicKey 복원 메서드 추가 |

```java
// AbstractEncryptionHelper.java — ECC 커브 결정 분기
} else if ("MlDsa44".equals(algorithm)) { PQC는 ECC 커브가 없음
    return null;
}

// EncryptionHelper.java — PQC PrivateKey 복원 (PKCS8 → PrivateKey)
public PrivateKey getMlDsaPrivateKeyObject(byte[] privateKeyBytes) throws WalletException {
    KeyFactory keyFactory = KeyFactory.getInstance("ML-DSA-44", "BC");
    return keyFactory.generatePrivate(new PKCS8EncodedKeySpec(privateKeyBytes));
}

// EncryptionHelper.java — PQC PublicKey 복원 (X509 → PublicKey)
public PublicKey getMlDsaPublicKeyObject(byte[] publicKeyBytes) throws WalletException {
    KeyFactory keyFactory = KeyFactory.getInstance("ML-DSA-44", "BC");
    return keyFactory.generatePublic(new X509EncodedKeySpec(publicKeyBytes));
}
```

### 2.4 로직 추가 (서명·키 생성·저장)

| 파일 | 변경 내용 |
|------|-----------|
| `crypto/sign/AbstractSignatureHelper.java` | 키 알고리즘 → DidKeyType 매핑 분기 |
| `key/adapter/WalletManagerAdapter.java` | PQC 키 복원, 서명, 공개키 저장 분기 |
| `key/impl/WalletManagerImpl.java` | 키 생성 시 DidKeyType 매핑 분기 |

```java
// AbstractSignatureHelper.java — 알고리즘 → DidKeyType 매핑
} else if (keyAlgorithm.equals(KeyAlgorithmType.ML_DSA_44.getRawValue()))
{
    return DidKeyType.ML_DSA_44_VERIFICATION_KEY_2024;
}

// WalletManagerAdapter.java — PQC 서명 (ECC CompactSign 대신 SignatureUtils 직접 호출)
if (KeyAlgorithmType.ML_DSA_44.getRawValue().equals(keyPairInfo.getAlgorithm())) {
    return SignatureUtils.generateMlDsa44Signature(
            keyPairInfo.getKeyPair().getPrivateKey(), hashedSource);
}
// 기존: signatureHelper.sign() + getCompactSignature() — ECC 전용

// WalletManagerAdapter.java — PQC 공개키 저장 (압축 없이 X509 그대로)
if (KeyAlgorithmType.ML_DSA_44.getRawValue().equals(info.getAlgorithm()))
{
    return info.getPublicKey().getEncoded();  // 압축 없이 그대로
}
// 기존: encryptionHelper.getCompressedPublicKey() — ECC 압축

// WalletManagerImpl.java — 키 생성 분기
} else if (KeyAlgorithmType.ML_DSA_44.equals(keyAlgorithmType)) {
    return DidKeyType.ML_DSA_44_VERIFICATION_KEY_2024;
}
```

---

## 3. did-datamodel-sdk-server

ML-DSA-44 관련 enum 타입 정의.

### 3.1 타입 정의

| 파일 | 변경 내용 |
|------|-----------|
| `enums/did/DidKeyType.java` | ML-DSA-44 키 타입 enum 추가 |
| `enums/did/ProofType.java` | ML-DSA-44 서명 타입 enum 추가 |

```java
// DidKeyType.java — 기존: RSA_..., SECP256K1_..., SECP256R1_...
ML_DSA_44_VERIFICATION_KEY_2024("MlDsa44VerificationKey2024"); // forten

// ProofType.java — 기존: RSA_SIGNATURE_2018, SECP256K1_..., SECP256R1_...
ML_DSA_44_SIGNATURE_2024("MlDsa44Signature2024"); // forten
```

---

## 4. did-core-sdk-server

ML-DSA-44 proof 타입 매핑 및 서명 검증 분기.

### 4.1 로직 추가 (매핑)

| 파일 | 변경 내용 |
|------|-----------|
| `manager/DidManager.java` | DID proof 생성 시 키 타입 → 서명 타입 매핑 case 추가 |
| `manager/VcManager.java` | VC proof 생성 시 키 타입 → 서명 타입 매핑 case 추가 |

```java
// DidManager.java — DID Document proof 생성 switch문
case "MlDsa44VerificationKey2024":
    proof.setType(ProofType.ML_DSA_44_SIGNATURE_2024.getRawValue());
    break;

// VcManager.java — VC proof 생성 switch문
case ML_DSA_44_VERIFICATION_KEY_2024:
    proofType = ProofType.ML_DSA_44_SIGNATURE_2024;
    return proofType.getRawValue();
```

### 4.2 로직 추가 (검증 분기)

| 파일 | 변경 내용 |
|------|-----------|
| `util/VerifyUtil.java` | ML-DSA-44 판별 메서드 + 서명 검증 분기 추가 |

```java
// ML-DSA-44 알고리즘 판별
private static boolean isMlDsa44(String sigAlgorithm) {
    ProofType proofType = ProofType.fromString(sigAlgorithm);
    return proofType == ProofType.ML_DSA_44_SIGNATURE_2024;
}

// verifySignature() 내부 — ECC 검증 전에 PQC 분기 삽입
if (isMlDsa44(sigParams.getAlgorithm()))
{
    byte[] originData = sigParams.getOriginData().getBytes(UTF_8);
    SignatureUtils.verifyMlDsa44Signature(publicKeyBytes, originData, signatureByte);
    return;  // ECC 경로 스킵
}
// 기존 ECC 서명 검증 (아래 계속)
```

---
