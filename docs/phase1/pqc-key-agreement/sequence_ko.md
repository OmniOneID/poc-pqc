# Key Agreement 시퀀스: ECDH → ML-KEM-768

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
    participant A as Alice
    participant B as Bob

    rect rgb(255, 230, 230)
        Note over A, B: 🔴 알고리즘 변경
        A->>A: 임시 키쌍 생성 (ECDH → ML-KEM-768)
        A->>B: Request(ephemeralPubKey 91B → 1,184B) + 서명
    end

    rect rgb(255, 230, 230)
        Note over A, B: 🔴 연산 변경
        B->>B: ECDH(bobPriv, alicePub) → ML-KEM Encapsulate(alicePub)
        B->>A: Response(pubKey 91B → ciphertext 1,088B) + 서명
    end

    rect rgb(255, 230, 230)
        Note over A, B: 🔴 연산 변경
        A->>A: ECDH(alicePriv, bobPub) → ML-KEM Decapsulate(alicePriv, ciphertext)
    end

    rect rgb(245, 245, 245)
        Note over A, B: 변경 없음
        A->>A: Session Key 유도 — SHA-256(secret ‖ nonce)
        Note over A, B: Session Key Established (32B, AES-256)
    end
```

## 변경 요약

| 구간 | 변경 여부 | 상세 |
|------|-----------|------|
| 🔴 Request | **변경** | ephemeralPubKey 91B → 1,184B |
| 🔴 Response | **변경** | ECDH → ML-KEM encapsulate, 응답 91B → 1,088B |
| 🔴 Key Derive | **변경** | ECDH → ML-KEM decapsulate |
| Session Key 유도 | 변경 없음 | 동일 방식, 프로토콜 2.6배 빠름 |
