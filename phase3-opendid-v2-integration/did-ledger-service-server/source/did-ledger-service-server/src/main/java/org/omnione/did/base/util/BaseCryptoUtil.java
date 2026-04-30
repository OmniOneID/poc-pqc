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

package org.omnione.did.base.util;

import org.omnione.did.crypto.enums.EccCurveType;
import lombok.extern.slf4j.Slf4j;
import org.omnione.did.base.exception.ErrorCode;
import org.omnione.did.base.exception.OpenDidException;
import org.omnione.did.crypto.exception.CryptoException;
import org.omnione.did.crypto.util.SignatureUtils;

/**
 * Utility class for cryptographic operations.
 * This class provides methods for generating key pairs, nonces, shared secrets, and signatures,
 * as well as encrypting and decrypting data using symmetric and asymmetric encryption algorithms.
 */
@Slf4j
public class BaseCryptoUtil {

    /**
     * Verifies a digital signature using the provided public key and ECC curve type.
     *
     * @param encodedPublicKey The encoded public key.
     * @param encodedSignature The encoded signature.
     * @param signData The data to verify.
     * @param eccCurveType The ECC curve type.
     * @throws OpenDidException if signature verification fails.
     */
    public static void verifySignature(String encodedPublicKey, String encodedSignature, byte[] signData, EccCurveType eccCurveType) {
        try {
            // Decode the public key
            byte[] publicKeyBytes = BaseMultibaseUtil.decode(encodedPublicKey);
            byte[] signatureBytes = BaseMultibaseUtil.decode(encodedSignature);

            // Verify the signature
            SignatureUtils.verifyCompactSignWithCompressedKey(publicKeyBytes, signData, signatureBytes, eccCurveType);
        }  catch (CryptoException e) {
            log.error("Failed to verify signature: {}", e.getMessage());
            throw new OpenDidException(ErrorCode.VERIFY_SIGN_FAIL);
        }
    }

}
