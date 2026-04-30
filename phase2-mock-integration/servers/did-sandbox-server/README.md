# did-sandbox-server

Open DID Sandbox (Mock) Server used in Phase 2 to validate the AOS app ↔ server integration flow with PQC enabled. It bundles the PQC-applied Server SDK JARs from Phase 1 in `libs/`.

## Runtime Environment

| Category       | Details                                  |
| -------------- | ---------------------------------------- |
| Language       | Java 21 (source/target)                  |
| Framework      | Spring Boot 3.2.4                        |
| Build Tool     | Gradle (wrapper included)                |
| Crypto         | Bouncy Castle 1.80 (`bcprov`, `bcpkix`)  |
| Default Port   | `8084` (see `src/main/resources/application.yml`) |

Bundled local SDK JARs (`libs/`):
- `did-op-sdk-server-2.0.0.jar`
- `did-sdk-common-2.0.0.jar`
- `pqc-did-core-sdk-server-2.0.0.jar`
- `pqc-did-crypto-sdk-server-2.0.0.jar`
- `pqc-did-datamodel-sdk-server-2.0.0.jar`
- `pqc-did-wallet-sdk-server-2.0.0.jar`

## PQC Configuration

`application.yml` controls which algorithms the server uses:

```yaml
pqc:
  signature-algorithm: MlDsa44       # Secp256r1 | MlDsa44
  key-agreement-algorithm: MlKem768  # Secp256r1 | MlKem768
```

## Run

From this directory:

```bash
# Build
./gradlew clean build

# Run (Spring Boot)
./gradlew bootRun
```

Or run the produced jar directly:

```bash
java -jar build/libs/did-sandbox-server-1.0.0.jar
```

The server starts on `http://localhost:8084` by default.
