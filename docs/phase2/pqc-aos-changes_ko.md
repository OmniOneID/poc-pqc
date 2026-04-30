# AOS PQC 적용 변경 사항

Open DID 공식 AOS 컴포넌트에 ML-DSA-44 / ML-KEM-768을 적용하면서 발생한 변경을 SDK / 앱별로 정리한 요약본입니다.

- **업스트림 기준**: [`OmniOneID/did-client-sdk-aos`](https://github.com/OmniOneID/did-client-sdk-aos), [`OmniOneID/did-ca-aos`](https://github.com/OmniOneID/did-ca-aos)
- **변경 규모**: SDK +761 / -188 (19 파일), CA 앱 +426 / -50 (14 파일)

## 공통 변경 패턴

- **암호 라이브러리 병행 도입** — 기존 SpongyCastle 1.54.0.0(`org.spongycastle.*`)는 그대로 두고, PQC 분기만 BouncyCastle 1.80(`org.bouncycastle:bcprov-jdk18on`)을 사용. SpongyCastle 1.54는 ML-DSA / ML-KEM을 지원하지 않으므로(요구: BC 1.78+) 이중 스택이 불가피.
- **분기 확장 방식** — 새 추상화를 만들지 않고 기존 `Secp256R1Manager` 옆에 `MlDsa44Manager`, `MlKem768Manager`를 추가하고, 호출부는 `SignableInterface` 다형으로 통일.
- **알고리즘 선택** — `Config` 정적 변수(`SIGNATURE_ALGORITHM`, `KEY_AGREEMENT_ALGORITHM`)로 토글. 변경 후 기존 키/DID는 재등록 필요.

---

## 1. did-client-sdk-aos

### 1.1 빌드 (`source/did-wallet-sdk-aos/build.gradle`)

```groovy
// 기존 SpongyCastle은 유지
implementation 'com.madgag.spongycastle:core:1.54.0.0'
// ... (prov / pkix / pg)

// BouncyCastle 1.80 for ML-DSA-44 (PQC) support
implementation 'org.bouncycastle:bcprov-jdk18on:1.80'
```

### 1.2 enum 추가

| 파일 | 추가 항목 |
|---|---|
| `datamodel/common/enums/AlgorithmType.java` | `ML_DSA_44("MlDsa44")`, `ML_KEM_768("MlKem768")` + getValue / DIDKeyType 매핑 분기 |
| `datamodel/common/enums/ProofType.java` | `mlDsa44Signature2024("MlDsa44Signature2024")` + AlgorithmType ↔ ProofType 매핑 |
| `datamodel/did/DIDKeyType.java` | `mlDsa44VerificationKey2024`, `mlKem768AgreementKey2024` + 양방향 매핑 |

### 1.3 신규 키 매니저

`core/keymanager/supportalgorithm/` 하위에 `Secp256R1Manager` / `Secp256K1Manager`와 같은 위치에 신규 추가:

- `MlDsa44Manager.java` — 키 생성, 서명, 검증, `checkKeyPairMatch`
- `MlKem768Manager.java` — 키 생성, encap / decap, `checkKeyPairMatch`

두 매니저 모두 클래스 정적 초기화 시점에 `Security.removeProvider("BC")` → `BouncyCastleProvider`(1.80) 등록. SpongyCastle(`SC`)는 건드리지 않음.

### 1.4 데이터 모델 / 프로토콜 VO

- 신규: `datamodel/security/AccMlKem.java`, `datamodel/security/ReqMlKem.java`
- 수정:
  - `datamodel/profile/ReqE2e.java` — `algorithm` 필드 + `isMlKem()` 헬퍼
  - `datamodel/security/AccE2e.java` — ML-KEM 분기용 필드 추가
  - `datamodel/protocol/P132/P210/P220 Request·Response Vo` 6종 — `reqMlKem` / `accMlKem` 필드와 getter/setter 추가

### 1.5 핵심 API (큰 변경)

| 파일 | 변경 라인 | 핵심 변경 |
|---|---|---|
| `core/api/KeyManager.java` | +229 | 기존 `Secp256R1Manager` 직접 호출을 `SignableInterface` 다형 호출로 전환. ML-DSA-44 바이오 인증용 wrap 포맷(`[IV(12B) ‖ AES-GCM ciphertext]`) 처리 등 추가 |
| `core/api/WalletCore.java` | +203 | 키 종류별 분기, ML-KEM 흐름 통합 |
| `core/api/WalletService.java` | +125 | 알고리즘 선택에 따른 호출 분기 |

### 1.6 공통 설정 (`wallet/walletservice/config/Config.java`)

```java
private static AlgorithmType.ALGORITHM_TYPE SIGNATURE_ALGORITHM    = AlgorithmType.ALGORITHM_TYPE.ML_DSA_44;
private static AlgorithmType.ALGORITHM_TYPE KEY_AGREEMENT_ALGORITHM = AlgorithmType.ALGORITHM_TYPE.ML_KEM_768;

public static AlgorithmType.ALGORITHM_TYPE getSignatureAlgorithm() { ... }
public static boolean isMlKemKeyAgreement() {
    return KEY_AGREEMENT_ALGORITHM == AlgorithmType.ALGORITHM_TYPE.ML_KEM_768;
}
```

### 1.7 BIO 하이브리드 키 보호 (PQC 도입의 핵심 설계 변경)

기존 Open DID Android는 BIO 키를 **Secp256r1 ECDSA 키를 AndroidKeystore에 직접 생성**하는 방식이었습니다(`setUserAuthenticationRequired(true)`). Keystore 내부에서 키가 절대 빠져나오지 않고 BiometricPrompt 통과 시점에만 서명 가능해, TEE/StrongBox의 하드웨어 격리를 그대로 활용할 수 있었죠.

**문제** — AndroidKeystore는 ML-DSA-44를 알고리즘으로 지원하지 않습니다. PQC 키를 하드웨어에 직접 보관할 길이 막힌 셈입니다.

**해법: AES-GCM Keystore 키로 PQC 개인키를 wrap하는 하이브리드 구조**

```
┌─────────────────────────────────────────────────────┐
│  AndroidKeystore (TEE / StrongBox)                  │
│  ┌─────────────────────────────────────────────┐    │
│  │  AES-256-GCM wrapping key                   │    │   ← Keystore에서 못 나옴
│  │  - PURPOSE_ENCRYPT | PURPOSE_DECRYPT        │    │   ← per-use 바이오 인증 (timeout=0,
│  │  - userAuthenticationRequired = true        │    │     AUTH_BIOMETRIC_STRONG)
│  │  - invalidatedByBiometricEnrollment = false │    │   ← 지문 재등록에도 살아남음
│  │  - StrongBox(가능 시)                        │    │
│  └─────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────┘
                       ↑ wrap / unwrap
                       │  (BiometricPrompt.CryptoObject로 인증된 Cipher만)
┌─────────────────────────────────────────────────────┐
│  앱 일반 저장소 (DetailKeyInfo)                       │
│  ┌─────────────────────────────────────────────┐    │
│  │  Wrapped Blob = [ IV (12B) ‖ Ciphertext+Tag(16B) ] │
│  │   - plaintext: ML-DSA-44 PrivateKey (~2560B)│    │
│  │   - AES/GCM/NoPadding으로 암호화              │    │
│  └─────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────┘
```

**왜 "하이브리드"인가** — 두 알고리즘 / 두 보호 계층을 의도적으로 결합한 구조이기 때문입니다:

- **하드웨어 측**: 클래식 대칭(AES-GCM) 키가 Keystore에 잠겨 있고, BiometricPrompt 통과 시에만 사용 가능
- **소프트웨어 측**: PQC(ML-DSA-44) 개인키는 메모리 / 파일에 wrap된 형태로 존재
- 결과: PQC 키 자체는 Keystore에 못 들어가도, **PQC 키를 풀 수 있는 권한**은 Keystore + 생체인증이 통제

**구현 포인트**

| 위치 | 추가/변경 |
|---|---|
| `KeystoreManager.BIO_WRAPPING_KEY_ALIAS_PREFIX` | wrapping AES 키 별칭 prefix (`opendid_wallet_bio_wrap_`) |
| `KeystoreManager.generateOrGetBioWrappingKey(ctx, alias)` | AES-256-GCM Keystore 키 생성/조회. `setUserAuthenticationParameters(0, AUTH_BIOMETRIC_STRONG)`, StrongBox 가능 시 활성화 |
| `KeyManager.generateBioMlDsaKey(id, algo, encCipher)` | `MlDsa44Manager`로 PQC 키 생성 → 인증된 `Cipher`(ENCRYPT_MODE)로 wrap → `[IV ‖ ct]` 블롭을 `DetailKeyInfo`에 저장 |
| `KeyManager.getBioWrappedIv(id)` | 서명 직전 12바이트 IV만 추출. `Cipher.init(DECRYPT_MODE, key, GCMParameterSpec(128, iv))`에 사용 |
| `KeyManager.unlockBioMlDsaKey(id, decCipher)` | 인증된 DECRYPT Cipher로 wrap된 PQC 개인키 복원. `unlockedBioPrivateKey` 필드에 임시 보관 |
| `KeyManager.clearUnlockedBioKey()` | 메모리의 평문 개인키를 `Arrays.fill(..., 0x00)`로 즉시 0초기화 |
| `WalletCore.registerBioKeyHybrid(ctx)` | PQC 분기. 기존 `registerBioKeyLegacy(ctx)`(Secp256r1 + 직접 Keystore 저장)와 알고리즘 선택에 따라 갈림 |
| `BioPromptHelper.getLastAuthenticatedCipher()` / `clearLastAuthenticatedCipher()` | BiometricPrompt 콜백에서 인증된 Cipher 인스턴스를 외부로 전달하기 위한 확장 |

**서명 1회 흐름**

1. 저장된 wrapped blob에서 IV(12B) 분리 → `Cipher.init(DECRYPT_MODE, wrapKey, gcmSpec(IV))`
2. `BiometricPrompt.CryptoObject(cipher)`로 사용자 인증
3. 인증 성공 시 cipher가 활성화 → `cipher.doFinal(ct)` → ML-DSA-44 평문 개인키 복원
4. `MlDsa44Manager.sign(...)` 수행
5. `clearUnlockedBioKey()`로 평문 개인키 0초기화

**보안 특성 / 한계**

- **장점**: AES wrap 키가 Keystore에 갇혀 있어, 지문 인증 없이는 어떤 경로로도 PQC 개인키를 복호화할 수 없음. 단말 루팅 등 평문 메모리 노출 시점은 *서명 직전 짧은 윈도우*로 한정.
- **한계**: 서명 순간엔 결국 평문 PQC 개인키가 RAM에 존재. AndroidKeystore가 PQC를 직접 지원하기 전까지는 피할 수 없는 트레이드오프.
- **재인증 빈도**: per-use(`timeout=0`)이므로 매 서명마다 BiometricPrompt 필요. 등록 직후의 PIN+BIO 결합 동작 같은 케이스에선 `unlockedBioPrivateKey`를 잠시 유지해 재프롬프트 회피 (의도적 최적화).

---

## 2. did-ca-aos

### 2.1 빌드 (`source/did-ca-aos/app/build.gradle`)

```groovy
android {
    packagingOptions {
        resources { pickFirsts += ['org/bouncycastle/**'] }
    }
}

configurations.all {
    // 옛 BC 모듈이 트랜지티브로 따라오는 것을 차단
    exclude group: 'org.bouncycastle', module: 'bcprov-jdk15to18'
}

dependencies {
    implementation project(':did-wallet-sdk-aos')   // 기존 fileTree(libs/*.jar)는 주석 처리

    implementation('org.bitcoinj:bitcoinj-core:0.15.7') {
        exclude group: 'org.bouncycastle'           // bitcoinj가 끌어오는 BC 차단
    }

    // BouncyCastle 1.80 for ML-DSA-44 (PQC) support
    implementation 'org.bouncycastle:bcprov-jdk18on:1.80'
}
```

또한 `app/build.gradle`의 Dev/Prod URL 분기 블록은 단일 `serverUrl + ":8084"`로 단순화되어 Phase 2 mock 서버 연동 형태로 변경됨.

### 2.2 Config (`app/src/main/java/org/omnione/did/ca/config/Config.java`)

- `SIGNATURE_ALGORITHM`, `KEY_AGREEMENT_ALGORITHM` 상수 추가 (`"MlDsa44"`, `"MlKem768"`)
- TAS 엔드포인트 `REQUEST_ML_KEM = "/tas/api/v1/request-ml-kem"` 신설

### 2.3 프로토콜 동작 변경

`network/protocol/` 하위 클래스들에 ML-KEM 흐름 분기를 추가:

- `BaseOperation.java` (+92) — `Config.isMlKemKeyAgreement()` 분기로 `requestMLKEM()` 신규 메서드 호출. 기존 ECDH 경로와 병존.
- `protocol/user/RegUser.java` (+74), `protocol/vc/IssueVc.java` (+79), `protocol/vc/RevokeVc.java` (+69), `RestoreUser.java` 등 — 임시(ephemeral) ML-KEM-768 키페어 생성 → 서버에서 받은 `accMlKem.ciphertext`를 `MlKem768Manager.decapsulate(...)`로 풀어 shared secret 도출 → 후속 E2E 채널 수립.
- `ProtocolData.java`, `UserDidUpdate.java`, `UserRegistration.java`, `VcIssuance.java`, `TokenUtil.java`, `SplashActivity.java` 등 — Config 알고리즘 선택을 따르는 헬퍼/유틸 분기 추가.

### 2.4 ML-KEM 키 교환 흐름 (간이)

1. Wallet이 ephemeral ML-KEM-768 키페어 생성
2. `reqMlKem`에 자기 publicKey 실어 전송 (Phase 1과 달리 TAS DID Doc 사전 조회 없음 — Mock 단순화)
3. 서버가 encap 수행 후 `accMlKem.ciphertext` 반환
4. Wallet이 `decapsulate`로 shared secret 도출, ephemeral sk는 즉시 폐기

---

## 3. Known Issues / 운영 시 유의사항

- **알고리즘 토글은 컴파일타임 상수 수준** — `Config`의 정적 변수 변경 후 기존 키/DID 재등록 필요. 런타임 동적 전환 케이스는 검증되지 않음.
- **SpongyCastle ↔ BC 1.80 이중 등록** — 두 프로바이더가 JCE 레지스트리에 공존. ECDSA 경로는 `SC`, PQC 경로는 `BC`로 명시적으로 갈리도록 호출부에서 보장.
