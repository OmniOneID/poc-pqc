/*
 * Copyright 2025 OmniOne.
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

// PQC: ML-DSA-44 (FIPS 204) key management using BouncyCastle 1.80
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.omnione.did.sdk.core.exception.WalletCoreErrorCode;
import org.omnione.did.sdk.core.exception.WalletCoreException;
import org.omnione.did.sdk.core.keymanager.datamodel.KeyGenerationInfo;
import org.omnione.did.sdk.datamodel.common.enums.AlgorithmType;
import org.omnione.did.sdk.utility.DataModels.MultibaseType;
import org.omnione.did.sdk.utility.MultibaseUtils;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Security;

/**
 * ML-DSA-44 (FIPS 204) key manager.
 *
 * Key difference from EC: ML-DSA-44 signs raw data directly (no pre-hashing).
 * Keys are encoded as PKCS#8 (private) and X.509 (public).
 */
public class MlDsa44Manager implements SignableInterface {

    private static final String ALGORITHM = "ML-DSA-44";
    private static final String PROVIDER = "BC";

    static {
        // Android has a built-in "BC" provider that does not support ML-DSA-44.
        // Replace it with the app-bundled BouncyCastle 1.80 provider when needed.
        java.security.Provider provider = Security.getProvider(PROVIDER);
        if (provider == null || !provider.getClass().equals(BouncyCastleProvider.class)) {
            if (provider != null) {
                Security.removeProvider(PROVIDER);
            }
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    @Override
    public KeyGenerationInfo generateKey() {
        try {
            // "ML-DSA-44" algorithm name already specifies the parameter set — no separate init needed
            KeyPairGenerator kpg = KeyPairGenerator.getInstance(ALGORITHM, PROVIDER);
            KeyPair kp = kpg.generateKeyPair();

            // Store PKCS#8 private key and X.509 public key as Base58
            byte[] privateKeyBytes = kp.getPrivate().getEncoded();
            byte[] publicKeyBytes = kp.getPublic().getEncoded();

            KeyGenerationInfo info = new KeyGenerationInfo();
            info.setAlgoType(AlgorithmType.ALGORITHM_TYPE.ML_DSA_44);
            info.setPrivateKey(MultibaseUtils.encode(MultibaseType.MULTIBASE_TYPE.BASE_58_BTC, privateKeyBytes));
            info.setPublicKey(MultibaseUtils.encode(MultibaseType.MULTIBASE_TYPE.BASE_58_BTC, publicKeyBytes));
            return info;
        } catch (Exception e) {
            throw new RuntimeException("ML-DSA-44 key generation failed: " + e.getMessage(), e);
        }
    }

    /**
     * Signs data with ML-DSA-44.
     * NOTE: data must be the raw message — ML-DSA-44 handles hashing internally.
     * Do NOT pass a pre-hashed digest.
     */
    @Override
    public byte[] sign(byte[] privateKeyBytes, byte[] data) throws WalletCoreException {
        return MlDsa44SignatureCompat.sign(privateKeyBytes, data);
    }

    /**
     * Verifies a ML-DSA-44 signature.
     * NOTE: data must be the raw message — same as sign().
     */
    @Override
    public boolean verify(byte[] publicKeyBytes, byte[] data, byte[] signature) throws WalletCoreException {
        return MlDsa44SignatureCompat.verify(publicKeyBytes, data, signature);
    }

    /**
     * Verifies that a private key corresponds to a public key by signing and verifying a test message.
     */
    public void checkKeyPairMatch(byte[] privateKey, byte[] publicKey) throws WalletCoreException {
        byte[] testMessage = "pqc-key-check".getBytes();
        byte[] sig;
        try {
            sig = sign(privateKey, testMessage);
        } catch (WalletCoreException e) {
            throw new WalletCoreException(WalletCoreErrorCode.ERR_CODE_SIGNABLE_NOT_DERIVED_KEY_FROM_PRIVATE_KEY);
        }
        try {
            if (!verify(publicKey, testMessage, sig)) {
                throw new WalletCoreException(WalletCoreErrorCode.ERR_CODE_SIGNABLE_NOT_DERIVED_KEY_FROM_PRIVATE_KEY);
            }
        } catch (WalletCoreException e) {
            throw new WalletCoreException(WalletCoreErrorCode.ERR_CODE_SIGNABLE_NOT_DERIVED_KEY_FROM_PRIVATE_KEY);
        }
    }
}
