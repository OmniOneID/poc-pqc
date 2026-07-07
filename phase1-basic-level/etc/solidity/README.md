# PQC Phase 1 — Solidity Contracts

Foundry project for the Phase 1 on-chain components: PQC DID registry and VC verifier wired against a real ML-DSA-44 (ETH-friendly variant) signature verifier from [ZKNOX/ETHDILITHIUM](https://github.com/ZKNoxHQ/ETHDILITHIUM).

## Recommended Environment

| Category        | Details          |
| --------------- | ---------------- |
| Smart Contract  | Solidity 0.8.27+ |
| Toolchain       | Foundry (forge)  |
| EVM Version     | cancun           |

Compiler / build settings are pinned in `foundry.toml` (`solc = "0.8.27"`, optimizer enabled, runs = 200, `ffi = true`).

## Setup

Install Foundry if you don't have it:

```bash
curl -L https://foundry.paradigm.xyz | bash
foundryup
```

Then, from this directory:

```bash
# Pull git submodule libraries (forge-std, ETHDILITHIUM, etc.)
git submodule update --init --recursive

# Set up the off-chain Python signer used by the verify test (one-off)
( cd lib/ETHDILITHIUM/pythonref && make install )

# Build
forge build
```

The Python signer is required because the test harness signs a fresh ML-DSA-44 signature off-chain via Foundry's FFI cheatcode (`PythonSigner`). It is invoked only inside tests; production deployments do not need it.

## Test

```bash
# Run all tests
forge test --match-contract PqcPhase1Test -vv

# With gas report
forge test --match-contract PqcPhase1Test --gas-report
```

## Measured On-Chain Cost (ML-DSA-44 ETH variant)

Gas figures from `forge test --gas-report` against the local EVM (cancun, optimizer runs = 200):

| Operation                                | Gas        | Notes                                                    |
| ---------------------------------------- | ---------- | -------------------------------------------------------- |
| `ZKNOX_ethdilithium` deployment          | ~2,280,000 | One-off                                                  |
| `setKey` (publish public key via SSTORE2)| ~4,838,000 | Per issuer, one-off                                      |
| `verify` (raw verifier)                  | ~4,832,000 | Per signature                                            |
| `PqcVcVerifier.verifyVc`                 | ~4,846,000 | Adds ~14k registry-lookup overhead                       |
| `PqcDidRegistry.register`                | ~68,000    | Stores 20-byte SSTORE2 pointer, not the full ~22 KB key  |
| `PqcDidRegistry.pubKeyOf`                | ~5,800     | Lookup                                                   |

Signature size on the wire is **2,420 bytes** (32 cTilde + 2,304 z + 84 h).

**Implication.** A single VC verification consumes roughly **16% of an Ethereum mainnet block** (30M gas limit) and would cost on the order of dollars per verification at typical gas prices. For Phase 1 this is acceptable as a feasibility result, but production-scale on-chain verification of every VC is not viable on L1; realistic deployments would need an L2, a ZK-proof of verification, or off-chain verification with on-chain commitment.
