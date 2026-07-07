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

import org.omnione.did.sdk.core.exception.WalletCoreErrorCode;
import org.omnione.did.sdk.core.exception.WalletCoreException;
import org.omnione.did.sdk.core.keymanager.datamodel.KeyGenerationInfo;
import org.omnione.did.sdk.datamodel.common.enums.AlgorithmType;
import org.omnione.did.sdk.utility.DataModels.MultibaseType;
import org.omnione.did.sdk.utility.MultibaseUtils;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

/**
 * ML-DSA-44 (FIPS 204) post-quantum digital signature manager.
 *
 * Key differences from ECC (Secp256r1):
 * - Signs raw data directly (no pre-hashing needed)
 * - Public key: X509-encoded, no compression (~1312 bytes)
 * - Private key: PKCS8-encoded (~2560 bytes)
 * - Signature: ~2420 bytes (not compact format)
 * - Requires BouncyCastle 1.78+ provider
 */
public class MlDsa44Manager implements SignableInterface {

    private static final String ALGORITHM = "ML-DSA-44";
    private static final String PROVIDER = "BC";

    static {
        // Android OS의 제한적 BC 프로바이더를 제거하고 BouncyCastle 1.80으로 교체
        // ML-DSA-44 지원을 위해 필수
        Security.removeProvider("BC");
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
        // SC(SpongyCastle)는 제거하지 않음 — Secp256R1Manager가 ECDSA 검증에 사용
    }

    @Override
    public KeyGenerationInfo generateKey() {
        try {
            KeyGenerationInfo keyGenerationInfo = new KeyGenerationInfo();
            keyGenerationInfo.setAlgoType(AlgorithmType.ALGORITHM_TYPE.ML_DSA_44);

            KeyPairGenerator kpg = KeyPairGenerator.getInstance(ALGORITHM, PROVIDER);
            KeyPair keyPair = kpg.generateKeyPair();

            byte[] privateKeyBytes = keyPair.getPrivate().getEncoded();
            byte[] publicKeyBytes = keyPair.getPublic().getEncoded();

            keyGenerationInfo.setPrivateKey(MultibaseUtils.encode(MultibaseType.MULTIBASE_TYPE.BASE_58_BTC, privateKeyBytes));
            keyGenerationInfo.setPublicKey(MultibaseUtils.encode(MultibaseType.MULTIBASE_TYPE.BASE_58_BTC, publicKeyBytes));

            return keyGenerationInfo;
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate ML-DSA-44 key pair", e);
        }
    }

    /**
     * Signs data using ML-DSA-44.
     * Note: ML-DSA-44 signs raw data directly — the digest parameter here is
     * the raw data to sign (NOT a pre-computed hash).
     */
    @Override
    public byte[] sign(byte[] privateKeyBytes, byte[] digest) throws WalletCoreException {
        try {
            KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM, PROVIDER);
            PrivateKey privateKey = keyFactory.generatePrivate(new PKCS8EncodedKeySpec(privateKeyBytes));

            Signature signer = Signature.getInstance(ALGORITHM, PROVIDER);
            signer.initSign(privateKey);
            signer.update(digest);
            return signer.sign();
        } catch (Exception e) {
            throw new WalletCoreException(WalletCoreErrorCode.ERR_CODE_SIGNABLE_CREATE_SIGNATURE, e.getMessage());
        }
    }

    @Override
    public boolean verify(byte[] publicKeyBytes, byte[] digest, byte[] signature) throws WalletCoreException {
        try {
            KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM, PROVIDER);
            PublicKey publicKey = keyFactory.generatePublic(new X509EncodedKeySpec(publicKeyBytes));

            Signature verifier = Signature.getInstance(ALGORITHM, PROVIDER);
            verifier.initVerify(publicKey);
            verifier.update(digest);
            return verifier.verify(signature);
        } catch (Exception e) {
            throw new WalletCoreException(WalletCoreErrorCode.ERR_CODE_SIGNABLE_VERIFY_SIGNATURE_FAILED, e.getMessage());
        }
    }

    /**
     * ML-DSA-44 uses X509/PKCS8 encoded keys, so key pair matching is done
     * by signing a test message and verifying it.
     */
    public void checkKeyPairMatch(byte[] privateKeyBytes, byte[] publicKeyBytes) throws WalletCoreException {
        try {
            byte[] testData = "key-pair-match-test".getBytes();
            byte[] signature = sign(privateKeyBytes, testData);
            boolean match = verify(publicKeyBytes, testData, signature);
            if (!match) {
                throw new WalletCoreException(WalletCoreErrorCode.ERR_CODE_SIGNABLE_NOT_DERIVED_KEY_FROM_PRIVATE_KEY);
            }
        } catch (WalletCoreException e) {
            throw e;
        } catch (Exception e) {
            throw new WalletCoreException(WalletCoreErrorCode.ERR_CODE_SIGNABLE_NOT_DERIVED_KEY_FROM_PRIVATE_KEY, e.getMessage());
        }
    }
}
