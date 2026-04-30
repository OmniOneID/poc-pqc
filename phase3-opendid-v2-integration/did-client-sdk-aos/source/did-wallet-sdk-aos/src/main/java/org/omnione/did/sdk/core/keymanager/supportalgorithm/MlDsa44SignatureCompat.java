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

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.omnione.did.sdk.core.exception.WalletCoreErrorCode;
import org.omnione.did.sdk.core.exception.WalletCoreException;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.PublicKey;
import java.security.Security;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

/**
 * Server-compatible ML-DSA-44 sign/verify helper.
 *
 * This mirrors the did-crypto-sdk-server SignatureUtils ML-DSA path so the
 * Android SDK verifies signatures through the same code path semantics.
 */
public final class MlDsa44SignatureCompat {

    private static final String ALGORITHM = "ML-DSA-44";
    private static final String PROVIDER = "BC";

    static {
        Provider provider = Security.getProvider(PROVIDER);
        if (provider == null || !provider.getClass().equals(BouncyCastleProvider.class)) {
            if (provider != null) {
                Security.removeProvider(PROVIDER);
            }
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    private MlDsa44SignatureCompat() {
    }

    public static byte[] sign(byte[] privateKeyBytes, byte[] data) throws WalletCoreException {
        if (privateKeyBytes == null || privateKeyBytes.length == 0) {
            throw new WalletCoreException(WalletCoreErrorCode.ERR_CODE_SIGNABLE_CREATE_SIGNATURE, "privateKeyBytes");
        }
        if (data == null || data.length == 0) {
            throw new WalletCoreException(WalletCoreErrorCode.ERR_CODE_SIGNABLE_CREATE_SIGNATURE, "data");
        }
        try {
            KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM, PROVIDER);
            PrivateKey privateKey = keyFactory.generatePrivate(new PKCS8EncodedKeySpec(privateKeyBytes));
            Signature signer = Signature.getInstance(ALGORITHM, PROVIDER);
            signer.initSign(privateKey);
            signer.update(data);
            return signer.sign();
        } catch (Exception e) {
            throw new WalletCoreException(WalletCoreErrorCode.ERR_CODE_SIGNABLE_CREATE_SIGNATURE, e.getMessage());
        }
    }

    public static boolean verify(byte[] publicKeyBytes, byte[] data, byte[] signatureBytes) throws WalletCoreException {
        if (publicKeyBytes == null || publicKeyBytes.length == 0) {
            throw new WalletCoreException(WalletCoreErrorCode.ERR_CODE_SIGNABLE_VERIFY_SIGNATURE_FAILED, "publicKeyBytes");
        }
        if (data == null || data.length == 0) {
            throw new WalletCoreException(WalletCoreErrorCode.ERR_CODE_SIGNABLE_VERIFY_SIGNATURE_FAILED, "data");
        }
        if (signatureBytes == null || signatureBytes.length == 0) {
            throw new WalletCoreException(WalletCoreErrorCode.ERR_CODE_SIGNABLE_VERIFY_SIGNATURE_FAILED, "signatureBytes");
        }
        try {
            KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM, PROVIDER);
            PublicKey publicKey = keyFactory.generatePublic(new X509EncodedKeySpec(publicKeyBytes));
            Signature verifier = Signature.getInstance(ALGORITHM, PROVIDER);
            verifier.initVerify(publicKey);
            verifier.update(data);
            return verifier.verify(signatureBytes);
        } catch (Exception e) {
            throw new WalletCoreException(WalletCoreErrorCode.ERR_CODE_SIGNABLE_VERIFY_SIGNATURE_FAILED, e.getMessage());
        }
    }

    public static String diagnoseVerify(byte[] publicKeyBytes, byte[] data, byte[] signatureBytes) {
        StringBuilder sb = new StringBuilder();
        sb.append("provider=").append(describeProvider());
        sb.append(" / publicKeyBytes=").append(publicKeyBytes != null ? publicKeyBytes.length : -1);
        sb.append(" / dataBytes=").append(data != null ? data.length : -1);
        sb.append(" / signatureBytes=").append(signatureBytes != null ? signatureBytes.length : -1);
        try {
            KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM, PROVIDER);
            PublicKey publicKey = keyFactory.generatePublic(new X509EncodedKeySpec(publicKeyBytes));
            sb.append(" / keyFactory=").append(keyFactory.getProvider().getName());
            sb.append(" / publicKeyClass=").append(publicKey.getClass().getName());
            sb.append(" / publicKeyAlgorithm=").append(publicKey.getAlgorithm());
            sb.append(" / publicKeyFormat=").append(publicKey.getFormat());

            Signature verifier = Signature.getInstance(ALGORITHM, PROVIDER);
            sb.append(" / signatureProvider=").append(verifier.getProvider().getName());
            verifier.initVerify(publicKey);
            verifier.update(data);
            boolean result = verifier.verify(signatureBytes);
            sb.append(" / directVerify=").append(result);
        } catch (Exception e) {
            sb.append(" / directVerifyException=").append(e.getClass().getSimpleName())
                    .append(":").append(e.getMessage());
        }
        return sb.toString();
    }

    private static String describeProvider() {
        Provider provider = Security.getProvider(PROVIDER);
        if (provider == null) {
            return "null";
        }
        return provider.getName() + ":" + provider.getClass().getName() + ":" + provider.getVersion();
    }
}
