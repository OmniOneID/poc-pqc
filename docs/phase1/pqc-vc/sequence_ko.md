# VC Issue/Verify 시퀀스: Secp256r1 → ML-DSA-44

> 아래 시퀀스는 PQC 모듈이 적용된 `did-core-sdk-server`, `did-crypto-sdk-server`, `did-datamodel-sdk-server`, `did-wallet-sdk-server` 적용된 것을 전제로 합니다.

```mermaid
%%{init: {
  'theme': 'base',
  'themeVariables': {
    'background': '#ffffff',
    'mainBkg': '#ffffff',
    'noteBkgColor': '#fff9e6',
    'noteTextColor': '#333333',
    'noteBorderColor': '#cccccc',
    'actorBkg': '#e8eef4',
    'actorBorder': '#7a8ea0',
    'actorTextColor': '#2c3e50',
    'signalColor': '#444444',
    'signalTextColor': '#333333',
    'sequenceNumberColor': '#ffffff',
    'labelBoxBkgColor': '#ffffff',
    'labelTextColor': '#333333'
  }
}}%%
sequenceDiagram
    participant SW as Server Wallet
    participant E as Issuer
    participant H as Holder
    participant V as Verifier

    rect rgb(255, 230, 230)
        Note over SW, E: 🔴 키 알고리즘 변경
        E->>SW: 키 생성 (SECP256r1 → ML_DSA_44)
        SW-->>E: 공개키 (33B → 1,334B)
        E->>E: DID Document 생성
    end

    rect rgb(245, 245, 245)
        Note over E, E: 변경 없음
        E->>E: VC 구조 생성 (claims, schema)
    end

    rect rgb(255, 230, 230)
        Note over SW, E: 🔴 서명 알고리즘 변경
        E->>SW: 서명 요청 (SECP256r1 → ML_DSA_44)
        SW-->>E: 서명 응답 (65B → 2,420B)
    end

    rect rgb(230, 240, 255)
        Note over E, V: 🔵 추가 분석 필요
        E->>H: VC 발급 (Holder 보관)
        H->>V: VP 제출 (VC 포함)
    end

    rect rgb(245, 245, 245)
        Note over V, V: 변경 없음
        V->>V: proofValue + proofValueList 검증
        V-->>H: Verify OK
    end
```

## 변경 요약

| 구간 | 변경 여부 | 상세 |
|------|-----------|------|
| 🔴 키 알고리즘 변경 | **변경** | 공개키 40.4x 증가 (33B → 1,334B) |
| VC 구조 생성 | 변경 없음 | claims, schema 동일 |
| 🔴 서명 알고리즘 변경 | **변경** | 서명 37.2x 증가 (65B → 2,420B), 서명 시간 ~4x 느림 |
| 🔵 VC 발급 · VP 제출 | **추가 분석 필요** | Holder 보관, VP 구성 흐름 |
| VC 검증 | 변경 없음 | 동일 흐름 |
