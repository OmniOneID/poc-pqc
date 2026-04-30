# OpenDID PQC 적용 최종 요약

> 작성일: 2026-04-28  
> 범위: OpenDID 2.0 플랫폼 전체 (서버 5종 + Android 앱)

---

## 1. 한 줄 요약

OpenDID 2.0 플랫폼의 **모든 핵심 암호 연산**(전자 서명 + 키 교환)을, 양자 컴퓨터 시대에도 안전한 **NIST 표준 PQC 알고리즘**으로 교체했다.

---

## 2. 배경 — PQC가 무엇인가

| 구분 | 기존 (고전 암호) | 변경 후 (PQC) |
|------|----------------|--------------|
| 전자 서명 | `Secp256r1` (타원곡선 기반) | **`ML-DSA-44`** (FIPS 204) |
| 키 교환 | `ECDH` (타원곡선 기반) | **`ML-KEM-768`** (FIPS 203) |
| 양자 내성 | 없음 (Shor 알고리즘으로 해독 가능) | **있음** (NIST 표준 양자 내성 알고리즘) |

> **왜 바꿔야 하는가?** 충분히 강력한 양자 컴퓨터가 등장하면 현재의 RSA/ECC 기반 암호는 한순간에 깨진다. 이를 대비해 NIST가 표준화한 알고리즘이 ML-DSA, ML-KEM이다.

---

## 3. 적용 범위

### 3.1 서버 (5종 모두 적용 완료)

| 서버 | 역할 | PQC 적용 상태 |
|------|------|-------------|
| **TA(Trust Anchor) 서버** | DID 등록·검증의 신뢰점 | 완료 |
| **CA(Certificate App) 서버** | 사용자 인증 | 완료 |
| **Issuer 서버** | VC(증명서) 발급 | 완료 |
| **Verifier 서버** | VP(증명서 제시) 검증 | 완료 |
| **Wallet 서버** | Wallet 인증 | 완료 |

### 3.2 Android 앱

| 컴포넌트 | 역할 | PQC 적용 상태 |
|---------|------|-------------|
| **did-client-sdk-aos** | 앱이 사용하는 OpenDID SDK | 완료 |
| **did-ca-aos** | 사용자 단말 CA 앱 | 완료 |

### 3.3 검증된 정상 흐름

- 월렛 등록 → 사용자 등록 → VC 발급 → VP 제출까지 **End-to-End 동작 확인 완료**
- CA / Issuer / Verifier / Wallet 4개 서버가 모두 TA에 PQC로 등록됨

---

## 4. 키 종류별 적용 정책

OpenDID는 사용자/서버마다 여러 종류의 키를 가진다. 키마다 용도가 달라서 **모두 PQC로 바꾸지는 않았다.**

| 키 ID | 용도 | 알고리즘 | 변경 여부 | 비고 |
|-------|------|---------|---------|------|
| `assert` | VC·VP 등 문서 서명 | **ML-DSA-44** | 변경 | 가장 빈번하게 사용 |
| `auth` | DID 인증 | **ML-DSA-44** | 변경 | |
| `invoke` | 서버 행위 서명 | **ML-DSA-44** | 변경 | |
| `pin` | 사용자 PIN 서명 | **ML-DSA-44** | 변경 | |
| `keyagree` | 세션 키 교환 | Secp256r1 유지 | 유지 | 키 교환은 별도로 ML-KEM-768 적용 |
| `bio` | 생체 인증 서명 | Secp256r1 유지 | 유지 | Android KeyStore가 PQC 미지원 (OS 제약) |

> **중요:** `keyagree`라는 키 자체는 `Secp256r1`을 유지하되, **실제 키 교환 프로토콜은 ML-KEM-768로 교체**했다. 즉 키 저장 방식과 통신 방식은 분리되어 있다.

---

## 5. 무엇이 어떻게 바뀌었나 — 비개발자용 설명

### 5.1 전자 서명 (ML-DSA-44 적용)

OpenDID는 모든 중요한 데이터(DID 문서, VC, VP)에 **전자 서명**을 붙인다. 이 서명을 만드는 방식이 바뀌었다.

```
[ 기존 ]                           [ 변경 후 ]
원문 → SHA-256 해시 → ECC 서명     원문 → ML-DSA-44 서명
      (해시를 따로 계산)                 (알고리즘 내부에서 처리)
```

**핵심 차이:** ML-DSA-44는 알고리즘 내부에서 해싱을 처리하므로, **외부에서 SHA-256 해시를 미리 적용하면 안 된다.** 이 차이가 마이그레이션 과정의 모든 버그의 근원이었다.

### 5.2 키 교환 (ML-KEM-768 적용)

서버와 클라이언트가 안전하게 통신하려면 **공유 비밀키**를 만들어야 한다. 그 방식이 바뀌었다.

```
[ 기존 ECDH 방식 ]
클라이언트: 임시 EC 공개키 전송 →
서버: 자기 EC 공개키로 응답 ←
양쪽: 서로의 공개키로 같은 비밀키 계산

[ 변경 후 ML-KEM-768 방식 ]
클라이언트: ML-KEM 공개키 전송 →
서버: 비밀키를 캡슐화한 ciphertext로 응답 ←
클라이언트: ciphertext를 풀어서 비밀키 획득
```

### 5.3 통신 메시지 형식 변경

키 교환 요청·응답 메시지에 새 필드가 추가됐다. **기존 `curve` 필드는 제거되지 않고 유지**되며, PQC 요청 시에는 `algorithm` 필드를 사용한다 (둘은 상호배타적, 한쪽이 채워지면 다른 쪽은 null).

```diff
  {
    "client": "did:omn:...",
    "clientNonce": "...",
    "curve": "Secp256r1",          ← ECDH 요청 시 사용 (PQC 요청이면 null)
+   "algorithm": "ML-KEM-768",     ← PQC 요청 시 사용 (ECDH 요청이면 null)
    "publicKey": "...",
    "candidate": {
      "ciphers": [...],
+     "keyAgreements": ["ML-KEM-768", "Secp256r1"]   ← 협상 가능 알고리즘
    }
  }
```

서버 응답에도 새 필드가 추가됐다. 응답의 `publicKey` 필드 역시 그대로 남아있고, ML-KEM 흐름에서만 null로 채워진다.

```diff
  {
    "server": "did:omn:...",
    "serverNonce": "...",
    "publicKey": "...",            ← ECDH 응답 시 서버 EC 공개키 (PQC 응답이면 null)
+   "ciphertext": "...",           ← ML-KEM 캡슐화 결과 (ECDH 응답이면 null)
+   "selectedAlgorithm": "ML-KEM-768",
    "cipher": "AES-256-CBC"
  }
```

> **하위 호환:** `curve` 필드를 그대로 유지하고 `algorithm`을 추가하는 방식이라, 구버전 클라이언트가 보낸 ECDH 요청도 그대로 파싱·처리된다. 신버전 클라이언트는 `keyAgreements` 목록 협상으로 서버가 지원하는 알고리즘을 선택한다 (자동 fallback).

---

## 6. 핵심 코드 변경 예시

### 6.1 서버 — 알고리즘 분기 (검증 측)

기존엔 EC 검증 한 가지만 있었지만, PQC proof type일 때는 별도 경로로 분기하도록 변경했다.

```java
// did-ta-server, did-issuer-server, did-verifier-server 등
if (ProofType.ML_DSA_44_SIGNATURE_2024.equals(proofType)) {
    SignatureUtils.verifyMlDsa44Signature(publicKeyBytes, rawData, signatureBytes);
} else {
    BaseCryptoUtil.verifySignature(encodedPublicKey, signature, rawData,
                                   proofType.toEccCurveType());
}
```

### 6.2 서버 — ML-DSA-44 서명 (해시 생략)

ML-DSA-44는 외부에서 해싱하지 않고 원문 바이트를 그대로 넘긴다.

```java
// FileWalletService.java (CA, Issuer, Verifier, Wallet 서버 공통)
if ("MlDsa44".equals(walletProperty.getKeyAlgorithm())
        && !"keyagree".equals(keyId)) {
    // [임의 수정] 메서드명은 ...FromHash이지만 ML-DSA-44 분기에서는
    // SDK 내부가 해싱을 건너뛰고 raw bytes를 그대로 서명하도록 고쳐 놓았다.
    // 즉 여기서 넘기는 plainText는 "해시"가 아니라 원문이며, 메서드 이름과
    // 파라미터명(hashedSource)이 실제 동작과 일치하지 않는다.
    signature = walletManager.generateCompactSignatureFromHash(keyId, plainText);
} else {
    // EC 경로: 내부에서 SHA-256 후 서명 (이름과 동작이 일치)
    signature = BaseWalletUtil.generateCompactSignature(walletManager, keyId, plainText);
}
```

### 6.3 서버 — ML-KEM-768 캡슐화 (TA 서버)

```java
// EcdhServiceImpl.java
if ("ML-KEM-768".equals(selectedAlgorithm)) {
    MlKemUtils.EncapsulateResult result = MlKemUtils.encapsulate(clientPubKeyBytes);
    accEcdh.setCiphertext(BaseMultibaseUtil.encode(result.ciphertext()));
    accEcdh.setSelectedAlgorithm("ML-KEM-768");
    sharedSecret = result.sharedSecret();
}
```

### 6.4 앱 — 알고리즘 단일 제어 포인트

앱은 SDK 설정 한 줄로 전체 서명 알고리즘을 전환할 수 있다.

```java
// did-client-sdk-aos/Config.java
public class Config {
    public static String KEY_ALGORITHM = "MlDsa44";  // "Secp256r1"으로 복원 가능
}
```

### 6.5 앱 — keyagree만 EC 고정

```java
// WalletCore.java
keyGenInfo = new WalletKeyGenRequest(
        Constants.KEY_ID_KEY_AGREE,
        AlgorithmType.ALGORITHM_TYPE.SECP256R1,  // keyagree는 항상 EC
        StorageOption.STORAGE_OPTION.WALLET,
        keyGenWalletMethodType
);
```

### 6.6 앱 — ML-KEM-768 키쌍 생성 및 복호화

```java
// BaseOperation.java — ECDH 요청 시 ML-KEM 키쌍 생성
KeyPair kemKeyPair = CryptoUtils.mlKemGenerateKeyPair();
reqEcdh.setAlgorithm("ML-KEM-768");
reqEcdh.setPublicKey(MultibaseUtils.encode(..., kemKeyPair.getPublic().getEncoded()));

// TokenUtil.java — 서버 응답에서 세션키 추출
if ("ML-KEM-768".equals(accEcdh.getSelectedAlgorithm())) {
    byte[] ciphertext = MultibaseUtils.decode(accEcdh.getCiphertext());
    byte[] sharedSecret = CryptoUtils.mlKemDecapsulate(kemPrivateKey, ciphertext);
}
```

---

## 7. 수정 파일 전체 통계

### 7.1 서버 (총 26개 파일)

| 분류 | 파일 수 | 내용 |
|------|--------|------|
| 데이터 모델 (5개 서버 × 3 파일) | 15개 | `EcdhReqData`, `AccEcdh`, `Candidate`에 PQC 필드 추가 |
| TA 서버 비즈니스 로직 | 4개 | ML-KEM 캡슐화, ML-DSA-44 검증 분기 |
| 엔티티 등록 서비스 (4개 서버) | 4개 | ML-KEM 키 생성, 디캡슐화 |
| 서명 경로 (3개 서버) | 3개 | ML-DSA-44 raw bytes 서명 |
| 기타 | 그 외 |

### 7.2 SDK (Java 서버 측, 4종 모두 변경)

- `did-crypto-sdk-server` — `MlDsaKeyPairGenerator`, `MlKemUtils` 신규
- `did-datamodel-sdk-server` — `MlDsa44Signature2024`, `MlDsa44VerificationKey2024` enum 추가
- `did-core-sdk-server` — DID Document / VC proof 생성·검증 분기
- `did-wallet-sdk-server` — `ML_DSA_44` 키 알고리즘 추가, X.509/PKCS8 직렬화

### 7.3 Android (총 7+ 파일)

- `did-client-sdk-aos`
  - `Config.java` — 알고리즘 단일 제어 포인트
  - `WalletCore.java`, `WalletService.java`, `WalletToken.java` — 키 생성·서명·검증 분기
  - `MlDsa44Manager.java` — BouncyCastle 1.80 강제 등록
  - `KeystoreManager.java` — Android KeyStore EC 공개키 안정 추출
  - `ReqEcdh.java`, `AccEcdh.java`, `CryptoUtils.java` — ML-KEM 통신 지원
- `did-ca-aos`
  - `BaseOperation.java`, `ProtocolData.java`, `TokenUtil.java` — ML-KEM 흐름 통합
  - `SplashActivity.java` — 예외 처리 NPE 방어
  - `AutoTestActivity.java` — 런타임 알고리즘 선택 UI (성능 비교용)

### 7.4 라이브러리 의존성

```groovy
implementation 'org.bouncycastle:bcprov-jdk18on:1.80'
// ML-DSA-44, ML-KEM-768 지원 최소 버전 (기존 1.78.1에서 업그레이드)
```

---

## 8. 마이그레이션 중 발견된 주요 버그 (참고용)

| # | 증상 | 원인 | 수정 |
|---|------|------|------|
| 1 | `Invalid ProofType: MlDsa44Signature2024` | TA 서버가 모든 proof를 EC 검증 경로로만 처리 | proof type 분기 추가 |
| 2 | `Failed to verify signature` | Issuer가 ML-DSA-44에 SHA-256 사전 해싱 | raw bytes 직접 서명으로 변경 |
| 3 | `Invalid parameter: publicKey` | 앱이 PQC 1,300 byte 공개키를 33 byte EC 키로 가정 | proof type → key type → 공개키 길이 우선순위로 알고리즘 판별 |
| 4 | `no such algorithm: ML-DSA-44 for provider BC` | Android 기본 BC 프로바이더 충돌 | BouncyCastle 1.80 강제 재등록 |
| 5 | ECDH 검증 실패 (`Compressed PublicKeyBytes is null`) | `keyagree`까지 PQC 키로 생성 | `keyagree`만 항상 SECP256R1 고정 |
| 6 | TA 서버 서명 검증 실패 | `signEcdhReq()` 반환 빌더에 `algorithm` 필드 누락 | builder에 `.algorithm()` 추가 |

> **공통 교훈:** PQC 적용은 "ProofType 문자열만 바꾸면 끝"이 아니다. **실제 서명 생성 경로 / 검증 입력 형식 / 키 직렬화 방식**까지 모두 일관되게 PQC에 맞춰야 한다.

---

## 9. 데이터 크기 영향

PQC 알고리즘은 고전 암호 대비 키와 서명 크기가 훨씬 크다. 이는 저장소 / 네트워크 / 블록체인 비용에 직접적 영향이 있다.

| 항목 | 기존 (Secp256r1) | PQC (ML-DSA-44) | 비율 |
|------|------------------|-----------------|------|
| 공개키 | 33 B (압축) | 1,323 B (X.509) | **×40** |
| 서명 | 65 B (compact) | 2,420 B | **×37** |
| Device DID Document | ~1 KB | ~5 KB | ×5 |
| Holder DID Document | ~2 KB | ~6 KB | ×3 |
| VP `proofValue` (Base64) | 88 chars | ~3,230 chars | ×37 |

> **블록체인 검토 필요:** DID Document를 원장에 기록하는 비용이 5배 이상 증가하므로, `did-ledger-service-server`의 가스/스토리지 영향 검토가 후속 과제.

---

## 10. 미적용 영역 및 후속 과제

| 항목 | 현재 상태 | 향후 과제 |
|------|---------|----------|
| 생체 인증 키(`bio`) | Secp256r1 유지 | Android KeyStore가 PQC 지원할 때까지 대기 (OS 제약) |
| 블록체인 비용 검토 | 미수행 | DID Document 5배 증가에 따른 가스/스토리지 영향 분석 |

---

## 11. 결론

- OpenDID 2.0 플랫폼은 **서명(ML-DSA-44) + 키 교환(ML-KEM-768)** 양쪽 모두에서 PQC로 전환됐다.
- 서버 5종 + Android 앱 + Java SDK 4종 모두 적용 완료.
- 사용자 등록 → VC 발급 → VP 제출까지 End-to-End 동작이 검증됐다.
- 알고리즘 전환은 **앱 SDK의 설정값 한 줄**(`Config.KEY_ALGORITHM`)로 통합 제어된다.
- 데이터 크기가 평균 30~40배 증가하므로 운영 환경 영향 검토는 후속 과제.


