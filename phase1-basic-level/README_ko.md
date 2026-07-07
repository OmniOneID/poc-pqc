# Phase 1 — Basic Level PoC (기본 컴포넌트 단위 검증)

Open DID를 구성하는 **기본 컴포넌트(Server SDK 4종 + DID Doc / VC / Key Agreement)** 에 PQC(ML-DSA-44 / ML-KEM-768)를 적용해, **"PQC가 기본 컴포넌트로서 사용될 수 있는가?"** 를 검증한 단계입니다.

- **범위**: Server SDK 4종 기반의 DID Doc, VC 발급/검증, Key Agreement 컴포넌트 실행
- **비교 대상**: 기존 Secp256r1 / ECDH 대비 PQC 알고리즘의 크기·성능 특성
- **산출물**: 3개 컴포넌트 서버 PoC + 4개 SDK 수정본 + 결과/시퀀스 문서

상위 컨텍스트 및 다른 Phase는 루트 [README](../README.md) 참고.

## PoC 주요 테스트 결과 (JVM & Bouncy Castle 기준)

| 컴포넌트 | 비교 대상 | 주요 결과 | 문서 |
|:----|:----------|:----------|:-----|
| [**DID Document**](servers/pqc-did-doc) | Secp256r1 vs ML-DSA-44 | 공개키 40.4x 크기 증가 / 키 생성 7배 빠름 | [결과](../docs/phase1/pqc-did-doc/result_ko.md) |
| [**Key Agreement**](servers/pqc-key-agreement) | ECDH vs ML-KEM-768 | 메시지 10~12x 크기 증가 / 프로토콜 2.6배 빠름 | [결과](../docs/phase1/pqc-key-agreement/result_ko.md) |
| [**VC Issue/Verify**](servers/pqc-vc) | Secp256r1 vs ML-DSA-44 | 서명 37.2x 크기 증가 / 서명 3~6배 느림, 검증 동등 | [결과](../docs/phase1/pqc-vc/result_ko.md) |

> PQC 알고리즘은 크기(공개키/서명/메시지)가 크게 증가하지만, 키 생성·검증·키 교환은 오히려 빠르고 서명만 3~6배 느린 결과를 보여줍니다.

## 컴포넌트별 도입 시 주요 변경 사항

| 문서 | 설명 |
|:-----|:-----|
| [DID Document 생성 시퀀스](../docs/phase1/pqc-did-doc/sequence_ko.md) | DID Document 생성 시퀀스 비교 (Secp256r1 → ML-DSA-44) |
| [Key Agreement 시퀀스](../docs/phase1/pqc-key-agreement/sequence_ko.md) | Key Agreement 시퀀스 비교 (ECDH → ML-KEM-768) |
| [VC Issue/Verify 시퀀스](../docs/phase1/pqc-vc/sequence_ko.md) | VC 발급/검증 시퀀스 비교 (Secp256r1 → ML-DSA-44) |
| [PQC Server SDK 변경 사항 분석](../docs/phase1/pqc-server-sdk-changes_ko.md) | 4개 SDK 모듈의 PQC 적용 변경 내역 |

## 기술 스택

| 구분 | 버전/사양 |
|:-----|:----------|
| Java | 21 |
| Spring Boot | 3.x |
| Bouncy Castle | 1.79+ (`bcprov-jdk18on`) |
| 빌드 도구 | Gradle |
| PQC 알고리즘 | ML-DSA-44 (FIPS 204), ML-KEM-768 (FIPS 203) |
| DID Document 포맷 | Open DID DID Document |
| VC 포맷 | Open DID VC |

## 빌드 구성

기존 Open DID 2.0.0 빌드 환경에서 Bouncy Castle 업그레이드가 필요합니다.

```groovy
// build.gradle
implementation 'org.bouncycastle:bcprov-jdk18on:1.79' // 1.78.1 → 1.79
```

## 폴더 구조

```
phase1-basic-level
├── sdks                              # PQC 적용 SDK 컴포넌트 4종
│   ├── pqc-did-crypto-sdk-server     # 서명 생성/검증, 키 생성 (ML-DSA-44 추가)
│   ├── pqc-did-datamodel-sdk-server  # 데이터 모델 (enum 추가)
│   ├── pqc-did-core-sdk-server       # DID/VC 관리 (proof 매핑, 검증 경로 분리)
│   └── pqc-did-wallet-sdk-server     # 키 관리/서명 위임
├── servers                           # 컴포넌트 실행 서버 3종
│   ├── pqc-did-doc                   # DID Document 생성/관리 컴포넌트
│   ├── pqc-vc                        # VC 발급/검증 컴포넌트
│   └── pqc-key-agreement             # ML-KEM 기반 Key Agreement 컴포넌트
└── etc                               # 보조 PoC (서버 SDK 외 환경에서의 검증)
    ├── solidity                      # 온체인(EVM) 측 PQC 검증용 Foundry 프로젝트
    └── swift                         # iOS/Swift 측 PQC 벤치마크 (SwiftPM)
```

| 이름 | 설명 |
|:-----|:-----|
| **`sdks/`** | Open DID SDK 컴포넌트에 ML-DSA-44를 적용한 수정본입니다. |
| ┖ `pqc-did-crypto-sdk-server` | ML-DSA-44 키 생성, 서명 생성/검증 로직을 추가합니다. |
| ┖ `pqc-did-datamodel-sdk-server` | `MlDsa44VerificationKey2024`, `MlDsa44Signature2024` 등 enum을 추가합니다. |
| ┖ `pqc-did-core-sdk-server` | DID/VC proof 타입 매핑 및 ML-DSA-44 검증 경로를 분리합니다. |
| ┖ `pqc-did-wallet-sdk-server` | 키 관리에 ML-DSA-44 분기를 추가합니다. |
| **`servers/`** | 각 컴포넌트의 PQC 동작을 검증하는 실행 서버입니다. |
| ┖ `pqc-did-doc` | ML-DSA-44 기반 DID Document 생성/관리 컴포넌트를 검증합니다. |
| ┖ `pqc-vc` | ML-DSA-44 기반 VC 발급/검증 컴포넌트를 검증합니다. |
| ┖ `pqc-key-agreement` | ML-KEM 기반 Key Agreement 컴포넌트를 검증합니다. |
| **`etc/`** | JVM/Bouncy Castle 외 환경에서 동일 알고리즘을 보조적으로 검증한 결과물입니다. |
| ┖ `etc/solidity` | Foundry 기반 Solidity 0.8.27+ 프로젝트. 온체인(EVM)에서의 ML-DSA 검증 흐름(`PqcDidRegistry`, `PqcVcVerifier`, `StubDilithiumVerifier`)을 다룹니다. |
| ┖ `etc/swift` | SwiftPM 기반 Swift 5.8 벤치마크. `SwiftDilithium` / `SwiftKyber`를 이용해 iOS 측 DID/VC/Key Agreement 시나리오의 크기·성능 지표를 측정합니다. |

> 관련 문서는 모두 루트 [`docs/phase1/`](../docs/phase1) 하위에 있습니다.

## 설치 가이드

각 서브 폴더의 빌드 환경, 의존성 설정, 실행 커맨드는 폴더별 가이드를 참고하세요. Server SDK 4종 / Server 3종의 공통 환경(JDK 21, Spring Boot 3.x, Bouncy Castle 1.79+)은 본 README 상단의 "기술 스택"·"빌드 구성" 절을 함께 참고하시기 바랍니다.

**Server SDK 4종** — 각 모듈은 독립 Gradle 프로젝트입니다. 빌드는 해당 폴더에서 `./gradlew clean build`, JAR 산출은 `./gradlew jar`로 수행하며, 결과 JAR은 `servers/` 하위 컴포넌트의 `libs/`에 배치됩니다.

- [`sdks/pqc-did-crypto-sdk-server`](sdks/pqc-did-crypto-sdk-server) — ML-DSA-44 키 생성, 서명 생성/검증 로직 (Bouncy Castle 1.80).
- [`sdks/pqc-did-datamodel-sdk-server`](sdks/pqc-did-datamodel-sdk-server) — `MlDsa44VerificationKey2024`, `MlDsa44Signature2024` 등 PQC enum 추가.
- [`sdks/pqc-did-core-sdk-server`](sdks/pqc-did-core-sdk-server) — DID/VC proof 타입 매핑 및 ML-DSA-44 검증 경로 분리.
- [`sdks/pqc-did-wallet-sdk-server`](sdks/pqc-did-wallet-sdk-server) — 키 관리에 ML-DSA-44 분기 추가.

**Server 3종** — 각 컴포넌트는 Spring Boot 3.x 기반 실행 서버입니다. `libs/`에 위 SDK JAR을 배치한 뒤 해당 폴더에서 `./gradlew bootRun`으로 기동합니다.

- [`servers/pqc-did-doc`](servers/pqc-did-doc) — ML-DSA-44 기반 DID Document 생성/관리 컴포넌트.
- [`servers/pqc-vc`](servers/pqc-vc) — ML-DSA-44 기반 VC 발급/검증 컴포넌트.
- [`servers/pqc-key-agreement`](servers/pqc-key-agreement) — ML-KEM-768 기반 Key Agreement 컴포넌트.

**etc 보조 PoC** — JVM 외 환경에서의 보조 검증으로, 폴더별 README에 권장 환경과 커맨드가 정리되어 있습니다.

- [`etc/solidity/README.md`](etc/solidity/README.md) — 온체인(EVM) PQC 검증용 Foundry 프로젝트의 권장 구성 환경(Solidity 0.8.27+, cancun EVM)과 빌드/테스트 커맨드(`forge build`, `forge test`).
- [`etc/swift/README.md`](etc/swift/README.md) — iOS/Swift PQC 벤치마크의 권장 구성 환경(Swift 5.8, Xcode 26.0.1, iOS 15.0+)과 SwiftPM 기반 빌드/실행 커맨드(`swift build`, `swift run -c release PqcBench`).
