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

package org.omnione.did.crypto.util;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.X509EncodedKeySpec;
import javax.crypto.KeyGenerator;
import org.bouncycastle.jcajce.SecretKeyWithEncapsulation;
import org.bouncycastle.jcajce.spec.KEMExtractSpec;
import org.bouncycastle.jcajce.spec.KEMGenerateSpec;
import org.omnione.did.crypto.constant.CryptoConstant;
import org.omnione.did.crypto.exception.CryptoErrorCode;
import org.omnione.did.crypto.exception.CryptoException;

// PQC: ML-KEM-768 (FIPS 203) 키 교환 유틸리티
public class MlKemUtils {

    private static final String ALGORITHM = "ML-KEM-768";

    /**
     * ML-KEM-768 임시 키쌍 생성.
     * 매 세션마다 새로 생성해야 한다.
     */
    public static KeyPair generateKeyPair() throws CryptoException {
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance(ALGORITHM, CryptoConstant.PROVIDER_BC);
            return kpg.generateKeyPair();
        } catch (Exception e) {
            throw new CryptoException(CryptoErrorCode.ERR_CODE_CRYPTOUTIL_GEN_RANDOM_KEY_FAIL, e.getMessage());
        }
    }

    /**
     * X.509 인코딩 바이트로 ML-KEM 공개키 복원.
     */
    public static PublicKey restorePublicKey(byte[] encoded) throws CryptoException {
        try {
            KeyFactory kf = KeyFactory.getInstance(ALGORITHM, CryptoConstant.PROVIDER_BC);
            return kf.generatePublic(new X509EncodedKeySpec(encoded));
        } catch (Exception e) {
            throw new CryptoException(CryptoErrorCode.ERR_CODE_CRYPTOUTIL_GEN_RANDOM_KEY_FAIL, e.getMessage());
        }
    }

    /**
     * 상대방 공개키로 캡슐화(Encapsulate).
     * 서버(TAS)가 클라이언트의 ML-KEM 공개키로 sharedSecret을 봉투에 담아 반환한다.
     *
     * @param recipientPublicKey 클라이언트 임시 ML-KEM 공개키
     * @return EncapsulationResult (sharedSecret 32B + ciphertext 1,088B)
     */
    public static EncapsulationResult encapsulate(PublicKey recipientPublicKey) throws CryptoException {
        try {
            KEMGenerateSpec spec = new KEMGenerateSpec(recipientPublicKey, CryptoConstant.ALG_AES);
            KeyGenerator kg = KeyGenerator.getInstance(ALGORITHM, CryptoConstant.PROVIDER_BC);
            kg.init(spec, new SecureRandom());
            SecretKeyWithEncapsulation secretKey = (SecretKeyWithEncapsulation) kg.generateKey();
            return new EncapsulationResult(secretKey.getEncoded(), secretKey.getEncapsulation());
        } catch (Exception e) {
            throw new CryptoException(CryptoErrorCode.ERR_CODE_CRYPTOUTIL_GEN_SECRET_FAIL, e.getMessage());
        }
    }

    /**
     * 자신의 개인키와 ciphertext로 공유 비밀 복원(Decapsulate).
     * 클라이언트가 서버 응답의 ciphertext를 자신의 임시 개인키로 풀어 sharedSecret을 도출한다.
     *
     * @param privateKey 클라이언트 임시 ML-KEM 개인키
     * @param ciphertext 서버가 반환한 캡슐화 결과
     * @return sharedSecret (32B)
     */
    public static byte[] decapsulate(PrivateKey privateKey, byte[] ciphertext) throws CryptoException {
        try {
            KEMExtractSpec spec = new KEMExtractSpec(privateKey, ciphertext, CryptoConstant.ALG_AES);
            KeyGenerator kg = KeyGenerator.getInstance(ALGORITHM, CryptoConstant.PROVIDER_BC);
            kg.init(spec);
            SecretKeyWithEncapsulation secretKey = (SecretKeyWithEncapsulation) kg.generateKey();
            return secretKey.getEncoded();
        } catch (Exception e) {
            throw new CryptoException(CryptoErrorCode.ERR_CODE_CRYPTOUTIL_GEN_SECRET_FAIL, e.getMessage());
        }
    }

    /** 캡슐화 결과 */
    public record EncapsulationResult(byte[] sharedSecret, byte[] ciphertext) {}
}
