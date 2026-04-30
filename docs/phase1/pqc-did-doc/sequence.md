# DID Document Creation Sequence: Secp256r1 → ML-DSA-44

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
    participant SW as Server Wallet
    participant E as Server Entity
    participant TA as TA
    participant L as Ledger

    rect rgb(255, 230, 230)
        Note over SW, E: 🔴 Key algorithm change
        E->>SW: Generate key (SECP256r1 → ML_DSA_44)
        SW-->>E: Public key (33B → 1,334B)
    end

    rect rgb(245, 245, 245)
        Note over E, TA: No change
        E->>E: Build DID Document
        E->>TA: Register DID Document
    end

    rect rgb(230, 240, 255)
        Note over TA, L: 🔵 Further analysis needed (Besu, Hyperledger, etc.)
        TA->>L: Register DID Document on ledger
        L-->>TA: Registration complete
    end

        TA-->>E: Registration complete
```

## Change Summary

| Stage | Changed? | Detail |
|-------|----------|--------|
| 🔴 Key generation · public key | **Changed** | Algorithm change, public key 40.4× larger (33B → 1,334B), key generation ~7× faster |
| DID Doc build · registration | No change | Same flow |
| 🔵 Ledger registration | **Further analysis needed** | TA → Ledger DID Document registration |
