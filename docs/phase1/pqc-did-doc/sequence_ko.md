# DID Document 생성 시퀀스: Secp256r1 → ML-DSA-44

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
    participant E as Server Entity
    participant TA as TA
    participant L as Ledger

    rect rgb(255, 230, 230)
        Note over SW, E: 🔴 키 알고리즘 변경
        E->>SW: 키 생성 (SECP256r1 → ML_DSA_44)
        SW-->>E: 공개키 (33B → 1,334B)
    end

    rect rgb(245, 245, 245)
        Note over E, TA: 변경 없음
        E->>E: DID Document 생성
        E->>TA: DID Document 등록
    end

    rect rgb(230, 240, 255)
        Note over TA, L: 🔵 추가 분석 필요 (Besu, Hyperledger 등)
        TA->>L: DID Document 원장 등록
        L-->>TA: 등록 완료
    end

        TA-->>E: 등록 완료
```

## 변경 요약

| 구간 | 변경 여부 | 상세 |
|------|-----------|------|
| 🔴 키 생성 · 공개키 | **변경** | 키 알고리즘 변경, 공개키 40.4x 증가 (33B → 1,334B), 키 생성 ~7배 빠름 |
| DID Doc 생성 · 등록 | 변경 없음 | 동일 흐름 |
| 🔵 원장 등록 | **추가 분석 필요** | TA → Ledger DID Document 등록 |
