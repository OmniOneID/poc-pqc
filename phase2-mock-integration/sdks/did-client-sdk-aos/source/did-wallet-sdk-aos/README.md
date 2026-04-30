Android Client SDK
==

Welcome to the Client SDK Repository. <br>
This repository provides an SDK for developing an Android mobile wallet.

## S/W Specifications
| Category         | Details                                                |
| ---------------- | ------------------------------------------------------ |
| OS               | Android 14                                             |
| Language         | Java 21                                                |
| IDE              | Android Studio 4                                       |
| Build System     | Gradle 8.2                                             |
| Compatibility    | Android API level 34 or higher                         |
| Test Environment | Minimum Requirements: Android 8.0 (Oreo, API Level 26) |
|                  | Recommended Requirements: Android 14 (API Level 34)    |

## Build Method
Execute the export JAR task in the `build.gradle` file of this SDK project to generate a JAR file.

1. Open the project's `build.gradle` file and add the following `exportJar` task.
   ```groovy
   ext {
       version = "2.0.1"
   }

   task exportJar(type: Copy){
       from('build/intermediates/aar_main_jar/release/')
       into('../release/')
       include('classes.jar')
       rename('classes.jar', 'did-wallet-sdk-aos-${version}.jar')
   }
   ```
2. Open the `Gradle` window in Android Studio, and run the `Tasks > other > exportJar` task of the project.
3. Once the execution is complete, the `did-wallet-sdk-aos-2.0.1.jar` file will be generated in the [`release/`](../release) folder.

## SDK Application Method
1. Copy the `did-wallet-sdk-aos-2.0.1.jar` file to the `libs` of the app project.
2. Add the following dependencies to the `build.gradle` of the app project.

```groovy
    implementation files('libs/did-wallet-sdk-aos-2.0.1.jar')
    implementation 'androidx.appcompat:appcompat:1.6.1'
    implementation 'com.google.android.material:material:1.11.0'
    implementation 'androidx.constraintlayout:constraintlayout:2.1.4'
    implementation 'androidx.navigation:navigation-fragment:2.7.7'

    testImplementation 'junit:junit:4.13.2'
    androidTestImplementation 'androidx.test.ext:junit:1.1.5'
    androidTestImplementation 'androidx.test.espresso:espresso-core:3.5.1'

    implementation 'com.google.firebase:firebase-messaging:20.0.0'
    implementation 'com.google.android.gms:play-services-vision:20.1.3'

    implementation "androidx.navigation:navigation-fragment-ktx:2.7.7"
    implementation "androidx.navigation:navigation-ui-ktx:2.7.7"

    implementation 'com.google.code.gson:gson:2.10.1'
    implementation 'androidx.biometric:biometric:1.1.0'
    implementation 'org.bitcoinj:bitcoinj-core:0.15.7'

    implementation 'com.madgag.spongycastle:core:1.54.0.0'
    implementation 'com.madgag.spongycastle:prov:1.54.0.0'
    implementation 'com.madgag.spongycastle:pkix:1.54.0.0'
    implementation 'com.madgag.spongycastle:pg:1.54.0.0'

    // BouncyCastle 1.80 for ML-DSA-44 / ML-KEM-768 (PQC) support
    implementation 'org.bouncycastle:bcprov-jdk18on:1.80'

    api "androidx.room:room-runtime:2.6.1"
    annotationProcessor "androidx.room:room-compiler:2.6.1"
```
3. Sync `Gradle` to ensure the dependencies are properly added.



## PQC / Cryptography Notes

This SDK ships with PQC support for **ML-DSA-44 (FIPS 204)** signatures and **ML-KEM-768 (FIPS 203)** key encapsulation, in addition to the existing classical (Secp256r1 / Secp256k1 / ECDH) algorithms.

### Provider stack

The wallet SDK runs **two crypto stacks side-by-side**:

| Library                         | Used for                                              |
| ------------------------------- | ----------------------------------------------------- |
| SpongyCastle 1.54.0.0 (`org.spongycastle.*`) | Existing classical paths — ECDSA, Secp256r1/k1 key handling, PGP utilities. |
| Bouncy Castle 1.80 (`org.bouncycastle:bcprov-jdk18on`) | PQC paths — ML-DSA-44 signing/verifying and ML-KEM-768 key agreement. FIPS 203 / 204 are only available in BC 1.78+. |

SpongyCastle is intentionally **not** removed: rewriting every classical path is out of scope for the PoC. Both providers coexist in the JCE registry.

### PQC key managers

Two new managers were added under `org.omnione.did.sdk.core.keymanager.supportalgorithm/`, parallel to `Secp256R1Manager` / `Secp256K1Manager`:

- `MlDsa44Manager` — ML-DSA-44 key generation, signing, verification.
- `MlKem768Manager` — ML-KEM-768 key generation, encapsulation/decapsulation.

Both managers register the BC 1.80 provider at class-init time before any JCE call:

```java
// MlDsa44Manager.java (excerpt)
static {
    // Replace Android's bundled, limited "BC" provider with BouncyCastle 1.80.
    // Required so that KeyPairGenerator.getInstance("ML-DSA-44", "BC") resolves
    // to the FIPS 204 implementation. SpongyCastle ("SC") is left untouched
    // so existing Secp256R1Manager paths keep working.
    Security.removeProvider("BC");
    Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
}
```

### Algorithm characteristics (vs. classical)

ML-DSA-44 keys and signatures are significantly larger than Secp256r1 — keep this in mind when sizing storage / payloads:

| Item               | Secp256r1 | ML-DSA-44       |
| ------------------ | --------- | --------------- |
| Public key         | 33 B (compressed) | ~1312 B (X.509-encoded, no compression) |
| Private key        | 32 B      | ~2560 B (PKCS#8) |
| Signature          | ~64–72 B  | ~2420 B         |
| Pre-hash required? | Yes       | No (signs raw data) |

For the broader Phase 1 measurement results (sizes, generation/sign/verify timing) refer to the root `docs/phase1/`.

### App-side notes

When integrating this SDK into an Android app (e.g. `did-ca-aos`), you typically also need:

```groovy
configurations.all {
    exclude group: 'org.bouncycastle', module: 'bcprov-jdk15to18'
}

dependencies {
    implementation('org.bitcoinj:bitcoinj-core:0.15.7') {
        exclude group: 'org.bouncycastle'
    }
    implementation 'org.bouncycastle:bcprov-jdk18on:1.80'
}
```

These excludes prevent older `org.bouncycastle.*` artifacts (pulled transitively by `bitcoinj` or other libs) from shadowing BC 1.80 at runtime, which would silently break PQC algorithm lookup.



## OpenDID Demonstration Videos
To watch our demonstration videos of the OpenDID system in action, please visit our [Demo Repository](https://github.com/OmniOneID/did-demo-server).

These videos showcase key features including user registration, VC issuance, and VP submission processes.


## License
[Apache 2.0](../../LICENSE)
