package org.omnione.did.poc.pqc.crypto;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;

import org.bouncycastle.jcajce.SecretKeyWithEncapsulation;
import org.bouncycastle.jcajce.spec.KEMExtractSpec;
import org.bouncycastle.jcajce.spec.KEMGenerateSpec;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.crypto.KeyGenerator;
import java.security.PrivateKey;

/**
 * ML-KEM (Module-Lattice-based Key Encapsulation Mechanism) 키 관리자.
 * FIPS 203 (ML-KEM-768) 기반 포스트양자 키 합의를 수행한다.
 *
 * 흐름:
 *  1) 양측이 ML-KEM 키쌍을 생성하여 DID Document의 keyAgreement에 공개키를 등록
 *  2) 발신자가 수신자의 공개키(encapsulation key)로 encapsulate → (ciphertext, sharedSecret)
 *  3) 수신자가 자신의 개인키(decapsulation key)로 decapsulate → sharedSecret 도출
 */
public class MLKEMKeyManager {

    private static final String ALGORITHM = "ML-KEM-768";
    private static final String PROVIDER = "BC";

    static {
        if (Security.getProvider(PROVIDER) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    /**
     * ML-KEM-768 키쌍 생성
     */
    public KeyPair generateKeyPair() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance(ALGORITHM, PROVIDER);
        return kpg.generateKeyPair();
    }

    /**
     * 상대방의 공개키(encapsulation key)로 캡슐화하여 공유 비밀과 암호문을 생성한다.
     *
     * @param recipientPublicKey 수신자의 ML-KEM 공개키
     * @return EncapsulationResult (sharedSecret + ciphertext)
     */
    public EncapsulationResult encapsulate(PublicKey recipientPublicKey) throws Exception {
        KEMGenerateSpec kemGenerateSpec = new KEMGenerateSpec(recipientPublicKey, "AES");
        KeyGenerator keyGenerator = KeyGenerator.getInstance(ALGORITHM, PROVIDER);
        keyGenerator.init(kemGenerateSpec, new SecureRandom());

        SecretKeyWithEncapsulation secretKey = (SecretKeyWithEncapsulation) keyGenerator.generateKey();

        return new EncapsulationResult(
                secretKey.getEncoded(),
                secretKey.getEncapsulation()
        );
    }

    /**
     * 자신의 개인키(decapsulation key)와 수신한 암호문으로부터 공유 비밀을 복원한다.
     *
     * @param privateKey 수신자의 ML-KEM 개인키
     * @param ciphertext 발신자가 보낸 캡슐화된 암호문
     * @return 복원된 공유 비밀 키 (바이트 배열)
     */
    public byte[] decapsulate(PrivateKey privateKey, byte[] ciphertext) throws Exception {
        KEMExtractSpec kemExtractSpec = new KEMExtractSpec(privateKey, ciphertext, "AES");
        KeyGenerator keyGenerator = KeyGenerator.getInstance(ALGORITHM, PROVIDER);
        keyGenerator.init(kemExtractSpec);

        SecretKeyWithEncapsulation secretKey = (SecretKeyWithEncapsulation) keyGenerator.generateKey();
        return secretKey.getEncoded();
    }

    /**
     * 캡슐화 결과를 담는 레코드
     */
    public record EncapsulationResult(
            byte[] sharedSecret,
            byte[] ciphertext
    ) {}
}
