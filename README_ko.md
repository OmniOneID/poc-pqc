# Open DID PQC(Post-Quantum Cryptography) PoC

Open DID PQC PoC에 오신 것을 환영합니다.
이 저장소는 Open DID 플랫폼에 양자내성암호(PQC)를 적용하기 위한 PoC(Proof of Concept) 프로젝트입니다.

## 프로젝트 목표

* 양자 컴퓨팅 위협에 대비한 Open DID 내 DID/VC 생태계의 PQC 전환 가능성 검증
* **ML-DSA-44(FIPS 204)** 기반 전자서명의 DID Document 및 VC 발급/검증 구현
* **ML-KEM-768(FIPS 203)** 기반 양자내성 키 교환(Key Encapsulation) 구현
* Open DID 레이어별 PQC 변경 범위 확인 및 기술적 타당성 분석

## 알고리즘 선택 배경

W3C CCG의 [Data Integrity Quantum Safe Cryptosuites](https://w3c-ccg.github.io/di-quantum-safe/)는 PQC 서명 알고리즘 4종을 정의합니다.

| 알고리즘 | 기반 | 표준화 상태 |
|:---------|:-----|:------------|
| **ML-DSA** | 모듈 격자 (Module Lattice) | NIST 표준 |
| SLH-DSA | 상태 없는 해시 (Stateless Hash-based) | NIST 표준 |
| Falcon | NTRU 격자 (NTRU Lattice) | 진행 중 |
| SQISign | 아이소제니 (Isogeny-based) | 학술 연구 단계 |

이 중 본 PoC는 **ML-DSA-44**를 채택했습니다. 향후 사실상의 표준(de facto)이 될 가능성이 가장 높으며 아래와 같은 이유로 PoC 수행을 위한 대표 알고리즘으로서 타당할 것으로 판단하였습니다.

- **NIST 표준화 완료** — Falcon은 진행 중, SQISign은 학술 연구 단계.
- **BouncyCastle 지원** — 1.79부터 ML-DSA-44 지원. SLH-DSA도 지원하지만 서명이 ML-DSA의 약 3배라 VC 페이로드에 넣기 비현실적. Falcon / SQISign은 BC 미지원.
- **합리적인 서명 크기** — ML-DSA-44 기준 2,420 bytes로 SLH-DSA의 약 1/3.

NIST 표준 + BC 지원 + 합리적 크기, 세 조건을 동시에 만족하는 건 ML-DSA가 유일합니다.

키 교환(KEM) 측에서도 같은 모듈 격자 계열인 **ML-KEM-768**을 채택했습니다. ML-KEM은 2024년 이후 주요 웹 브라우저와 CDN에 의해 TLS 1.3 하이브리드 키 교환의 기본 알고리즘으로 채택되어, 이미 전체 웹 트래픽의 상당 비중을 처리하는 수준으로 배포되어 있습니다.

> 현재 대규모 프로덕션에 배포된 PQC 알고리즘은 격자 기반이 유일합니다.

## PoC 단계 구성 (Phase Framework)

PoC는 **검증 범위를 점진적으로 확장하는 3단계**로 구성되어 있습니다.

| Phase | 목적 | 구성 | 경로 |
|:------|:-----|:-----|:-----|
| **1** | Open DID 기본 컴포넌트 단위 기술 타당성 검증 | Server 기준 DID Doc, VC/VP, Key Agreement 컴포넌트 실행 | [phase1-basic-level/](phase1-basic-level/README_ko.md) |
| **2** | Wallet 중심 Open DID 프로토콜 검증 | AOS CA 앱, Client SDK, Sandbox(Mock) Server | [phase2-mock-integration/](phase2-mock-integration) |
| **3** | Open DID v2.0 실서버 기준 검증 + 성능 측정 |  AOS CA 앱, Client SDK, Open DID v2.0 Servers + 성능 지표 측정| [phase3-opendid-v2-integration/](phase3-opendid-v2-integration) |

> Phase 1은 "PQC가 기본 컴포넌트로서 사용될 수 있는가?"를 검증했고, Phase 2는 "단말/서버 간 실제 프로토콜에서도 동작하는가?"를, Phase 3는 "실 운영 환경에서 쓸 만한가(성능 포함)?"를 검증합니다.

## 폴더 구조

```
poc-pqc
├── phase1-basic-level/           # 기본 컴포넌트 단위 PoC
│   ├── sdks/                     # PQC 적용 Server SDK 
│   ├── servers/                  # PQC 컴포넌트 서버 
│   └── etc/                      # Swift, Solidity 등 주요 컴포넌트 Test 
├── phase2-mock-integration/      # AOS 앱 ↔ Mock Server 연동 (Open DID 프로토콜 검증)
│   ├── apps/                     # did-ca-aos (Android CA 앱)
│   ├── sdks/                     # did-client-sdk-aos (Android Client SDK)
│   └── servers/                  # did-sandbox-server (Mock Server)
├── phase3-opendid-v2-integration/ # AOS 앱 ↔ 실제 Open DID v2.0 서버 연동 + 성능 측정
└── docs/                         # Phase별 결과·시퀀스·변경 분석 문서 모음
```

## 공통 기술 스택

| 구분 | 버전/사양 |
|:-----|:----------|
| PQC 알고리즘 | ML-DSA-44 (FIPS 204), ML-KEM-768 (FIPS 203) |
| DID Document 포맷 | Open DID DID Document |
| VC 포맷 | Open DID VC |

## 관련 스펙 및 자료

| 구분 | 링크 |
|:-----|:-----|
| NIST FIPS 203 (ML-KEM) | [스펙 문서](https://csrc.nist.gov/pubs/fips/203/final) |
| NIST FIPS 204 (ML-DSA) | [스펙 문서](https://csrc.nist.gov/pubs/fips/204/final) |
| W3C CCG Data Integrity Quantum Safe | [스펙 문서](https://w3c-ccg.github.io/di-quantum-safe/) |
| ML-DSA for JOSE and COSE | [IETF Draft](https://datatracker.ietf.org/doc/draft-ietf-cose-dilithium) |
| ML-DSA in X.509 | [RFC 9881](https://datatracker.ietf.org/doc/rfc9881/) |

## 기여

기여 절차와 행동 강령에 대한 자세한 내용은 [CONTRIBUTING.md](CONTRIBUTING.md)와 [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md)를 참조해 주십시오.

## 라이선스

[Apache 2.0](LICENSE)
