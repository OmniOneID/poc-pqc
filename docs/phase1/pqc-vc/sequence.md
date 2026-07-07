# VC Issue/Verify Sequence: Secp256r1 → ML-DSA-44

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
    participant E as Issuer
    participant H as Holder
    participant V as Verifier

    rect rgb(255, 230, 230)
        Note over SW, E: 🔴 Key algorithm change
        E->>SW: Generate key (SECP256r1 → ML_DSA_44)
        SW-->>E: Public key (33B → 1,334B)
        E->>E: Build DID Document
    end

    rect rgb(245, 245, 245)
        Note over E, E: No change
        E->>E: Build VC structure (claims, schema)
    end

    rect rgb(255, 230, 230)
        Note over SW, E: 🔴 Signature algorithm change
        E->>SW: Sign request (SECP256r1 → ML_DSA_44)
        SW-->>E: Signature response (65B → 2,420B)
    end

    rect rgb(230, 240, 255)
        Note over E, V: 🔵 Further analysis needed
        E->>H: Issue VC (held by Holder)
        H->>V: Submit VP (containing VC)
    end

    rect rgb(245, 245, 245)
        Note over V, V: No change
        V->>V: Verify proofValue + proofValueList
        V-->>H: Verify OK
    end
```

## Change Summary

| Stage | Changed? | Detail |
|-------|----------|--------|
| 🔴 Key algorithm change | **Changed** | Public key 40.4× larger (33B → 1,334B) |
| VC structure build | No change | claims, schema unchanged |
| 🔴 Signature algorithm change | **Changed** | Signature 37.2× larger (65B → 2,420B), signing ~4× slower |
| 🔵 VC issuance · VP submission | **Further analysis needed** | Holder storage, VP composition flow |
| VC verification | No change | Same flow |
