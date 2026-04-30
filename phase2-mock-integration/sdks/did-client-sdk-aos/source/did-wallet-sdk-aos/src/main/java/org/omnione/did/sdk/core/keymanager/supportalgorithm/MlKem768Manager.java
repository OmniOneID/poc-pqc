/*
 * Copyright 2024-2025 OmniOne.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.omnione.did.sdk.core.keymanager.supportalgorithm;

import org.bouncycastle.crypto.SecretWithEncapsulation;
import org.bouncycastle.jcajce.interfaces.MLKEMPrivateKey;
import org.bouncycastle.jcajce.interfaces.MLKEMPublicKey;
import org.bouncycastle.pqc.crypto.mlkem.MLKEMExtractor;
import org.bouncycastle.pqc.crypto.mlkem.MLKEMGenerator;
import org.bouncycastle.pqc.crypto.mlkem.MLKEMParameters;
import org.bouncycastle.pqc.crypto.mlkem.MLKEMPrivateKeyParameters;
import org.bouncycastle.pqc.crypto.mlkem.MLKEMPublicKeyParameters;

import org.omnione.did.sdk.core.exception.WalletCoreErrorCode;
import org.omnione.did.sdk.core.exception.WalletCoreException;
import org.omnione.did.sdk.core.keymanager.datamodel.KeyGenerationInfo;
import org.omnione.did.sdk.datamodel.common.enums.AlgorithmType;
import org.omnione.did.sdk.utility.DataModels.MultibaseType;
import org.omnione.did.sdk.utility.MultibaseUtils;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

/**
 * ML-KEM-768 (FIPS 203) post-quantum key encapsulation mechanism manager.
 *
 * Uses BouncyCastle low-level API (MLKEMGenerator/MLKEMExtractor) with
 * raw key data extracted via JCA interfaces (MLKEMPublicKey.getPublicData(),
 * MLKEMPrivateKey.getPrivateData()) to reconstruct low-level parameters.
 *
 * Key characteristics:
 * - Encapsulation key (public): X509-encoded (~1184 bytes)
 * - Decapsulation key (private): PKCS8-encoded (~2400 bytes)
 * - Ciphertext: ~1088 bytes
 * - Shared secret: 32 bytes
 * - Requires BouncyCastle 1.78+ provider
 */
public class MlKem768Manager implements SignableInterface {

    private static final String ALGORITHM = "ML-KEM-768";
    private static final String PROVIDER = "BC";

    static {
        Security.removeProvider("BC");
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
    }

    @Override
    public KeyGenerationInfo generateKey() {
        try {
            KeyGenerationInfo keyGenerationInfo = new KeyGenerationInfo();
            keyGenerationInfo.setAlgoType(AlgorithmType.ALGORITHM_TYPE.ML_KEM_768);

            KeyPairGenerator kpg = KeyPairGenerator.getInstance(ALGORITHM, PROVIDER);
            KeyPair keyPair = kpg.generateKeyPair();

            byte[] privateKeyBytes = keyPair.getPrivate().getEncoded();
            byte[] publicKeyBytes = keyPair.getPublic().getEncoded();

            keyGenerationInfo.setPrivateKey(MultibaseUtils.encode(MultibaseType.MULTIBASE_TYPE.BASE_58_BTC, privateKeyBytes));
            keyGenerationInfo.setPublicKey(MultibaseUtils.encode(MultibaseType.MULTIBASE_TYPE.BASE_58_BTC, publicKeyBytes));

            return keyGenerationInfo;
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate ML-KEM-768 key pair", e);
        }
    }

    /**
     * KEM keys are not used for signing.
     */
    @Override
    public byte[] sign(byte[] privateKey, byte[] digest) throws WalletCoreException {
        throw new WalletCoreException(WalletCoreErrorCode.ERR_CODE_KEY_MANAGER_UNSUPPORTED_ALGORITHM,
                "ML-KEM-768 does not support signing");
    }

    /**
     * KEM keys are not used for verification.
     */
    @Override
    public boolean verify(byte[] publicKey, byte[] digest, byte[] signature) throws WalletCoreException {
        throw new WalletCoreException(WalletCoreErrorCode.ERR_CODE_KEY_MANAGER_UNSUPPORTED_ALGORITHM,
                "ML-KEM-768 does not support verification");
    }

    /**
     * Encapsulates a shared secret using the peer's ML-KEM encapsulation key (public key).
     *
     * @param encapsulationKeyBytes X509-encoded ML-KEM-768 public key
     * @return EncapsulationResult containing ciphertext and shared secret
     */
    public EncapsulationResult encapsulate(byte[] encapsulationKeyBytes) throws WalletCoreException {
        try {
            // Decode X509 → JCA PublicKey → extract raw public data → build low-level params
            KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM, PROVIDER);
            PublicKey publicKey = keyFactory.generatePublic(new X509EncodedKeySpec(encapsulationKeyBytes));
            byte[] rawPublicData = ((MLKEMPublicKey) publicKey).getPublicData();
            MLKEMPublicKeyParameters publicKeyParams = new MLKEMPublicKeyParameters(MLKEMParameters.ml_kem_768, rawPublicData);

            MLKEMGenerator generator = new MLKEMGenerator(new SecureRandom());
            SecretWithEncapsulation encapsulated = generator.generateEncapsulated(publicKeyParams);

            byte[] ciphertext = encapsulated.getEncapsulation();
            byte[] sharedSecret = encapsulated.getSecret();

            return new EncapsulationResult(ciphertext, sharedSecret);
        } catch (Exception e) {
            throw new WalletCoreException(WalletCoreErrorCode.ERR_CODE_KEY_MANAGER_UNSUPPORTED_ALGORITHM,
                    "ML-KEM-768 encapsulation failed: " + e.getMessage());
        }
    }

    /**
     * Decapsulates a ciphertext using the ML-KEM decapsulation key (private key)
     * to recover the shared secret.
     *
     * @param decapsulationKeyBytes PKCS8-encoded ML-KEM-768 private key
     * @param ciphertext The ciphertext from encapsulation
     * @return The shared secret (32 bytes)
     */
    public byte[] decapsulate(byte[] decapsulationKeyBytes, byte[] ciphertext) throws WalletCoreException {
        try {
            // Decode PKCS8 → JCA PrivateKey → extract raw private data → build low-level params
            KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM, PROVIDER);
            PrivateKey privateKey = keyFactory.generatePrivate(new PKCS8EncodedKeySpec(decapsulationKeyBytes));
            byte[] rawPrivateData = ((MLKEMPrivateKey) privateKey).getPrivateData();
            MLKEMPrivateKeyParameters privateKeyParams = new MLKEMPrivateKeyParameters(MLKEMParameters.ml_kem_768, rawPrivateData);

            MLKEMExtractor extractor = new MLKEMExtractor(privateKeyParams);
            return extractor.extractSecret(ciphertext);
        } catch (Exception e) {
            throw new WalletCoreException(WalletCoreErrorCode.ERR_CODE_KEY_MANAGER_UNSUPPORTED_ALGORITHM,
                    "ML-KEM-768 decapsulation failed: " + e.getMessage());
        }
    }

    /**
     * ML-KEM key pair matching: encapsulate with public key,
     * decapsulate with private key, compare shared secrets.
     */
    public void checkKeyPairMatch(byte[] privateKeyBytes, byte[] publicKeyBytes) throws WalletCoreException {
        try {
            EncapsulationResult result = encapsulate(publicKeyBytes);
            byte[] decapsulatedSecret = decapsulate(privateKeyBytes, result.getCiphertext());

            byte[] encapsulatedSecret = result.getSharedSecret();
            if (encapsulatedSecret.length != decapsulatedSecret.length) {
                throw new WalletCoreException(WalletCoreErrorCode.ERR_CODE_SIGNABLE_NOT_DERIVED_KEY_FROM_PRIVATE_KEY);
            }
            for (int i = 0; i < encapsulatedSecret.length; i++) {
                if (encapsulatedSecret[i] != decapsulatedSecret[i]) {
                    throw new WalletCoreException(WalletCoreErrorCode.ERR_CODE_SIGNABLE_NOT_DERIVED_KEY_FROM_PRIVATE_KEY);
                }
            }
        } catch (WalletCoreException e) {
            throw e;
        } catch (Exception e) {
            throw new WalletCoreException(WalletCoreErrorCode.ERR_CODE_SIGNABLE_NOT_DERIVED_KEY_FROM_PRIVATE_KEY, e.getMessage());
        }
    }

    /**
     * Result of ML-KEM encapsulation containing both the ciphertext and shared secret.
     */
    public static class EncapsulationResult {
        private final byte[] ciphertext;
        private final byte[] sharedSecret;

        public EncapsulationResult(byte[] ciphertext, byte[] sharedSecret) {
            this.ciphertext = ciphertext;
            this.sharedSecret = sharedSecret;
        }

        public byte[] getCiphertext() {
            return ciphertext;
        }

        public byte[] getSharedSecret() {
            return sharedSecret;
        }
    }
}
