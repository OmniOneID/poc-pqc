# Key Agreement Sequence: ECDH → ML-KEM-768

> The sequence below assumes the PQC modules are applied to `did-core-sdk-server`, `did-crypto-sdk-server`, `did-datamodel-sdk-server`, and `did-wallet-sdk-server`.

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
        Note over A, B: 🔴 Algorithm change
        A->>A: Generate ephemeral key pair (ECDH → ML-KEM-768)
        A->>B: Request(ephemeralPubKey 91B → 1,184B) + signature
    end

    rect rgb(255, 230, 230)
        Note over A, B: 🔴 Operation change
        B->>B: ECDH(bobPriv, alicePub) → ML-KEM Encapsulate(alicePub)
        B->>A: Response(pubKey 91B → ciphertext 1,088B) + signature
    end

    rect rgb(255, 230, 230)
        Note over A, B: 🔴 Operation change
        A->>A: ECDH(alicePriv, bobPub) → ML-KEM Decapsulate(alicePriv, ciphertext)
    end

    rect rgb(245, 245, 245)
        Note over A, B: No change
        A->>A: Derive Session Key — SHA-256(secret ‖ nonce)
        Note over A, B: Session Key Established (32B, AES-256)
    end
```

## Change Summary

| Stage | Changed? | Detail |
|-------|----------|--------|
| 🔴 Request | **Changed** | ephemeralPubKey 91B → 1,184B |
| 🔴 Response | **Changed** | ECDH → ML-KEM encapsulate, response 91B → 1,088B |
| 🔴 Key Derive | **Changed** | ECDH → ML-KEM decapsulate |
| Session Key derivation | No change | Same method; protocol ~2.6× faster overall |
