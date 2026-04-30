package org.omnione.did.poc.pqc.crypto;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.spec.ECGenParameterSpec;

import javax.crypto.KeyAgreement;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

/**
 * ECDH (Secp256r1) 키 합의 관리자.
 */
public class ECDHKeyManager {

    private static final String KEY_ALGORITHM = "EC";
    private static final String KEY_AGREEMENT_ALGORITHM = "ECDH";
    private static final String CURVE = "secp256r1";
    private static final String PROVIDER = "BC";

    static {
        if (Security.getProvider(PROVIDER) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    public KeyPair generateKeyPair() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance(KEY_ALGORITHM, PROVIDER);
        kpg.initialize(new ECGenParameterSpec(CURVE), new SecureRandom());
        return kpg.generateKeyPair();
    }

    /**
     * ECDH 키 합의를 수행하여 공유 비밀을 도출한다.
     *
     * @param myPrivateKey 자신의 개인키
     * @param peerPublicKey 상대방의 공개키
     * @return 공유 비밀 (바이트 배열)
     */
    public byte[] deriveSharedSecret(PrivateKey myPrivateKey, PublicKey peerPublicKey) throws Exception {
        KeyAgreement keyAgreement = KeyAgreement.getInstance(KEY_AGREEMENT_ALGORITHM, PROVIDER);
        keyAgreement.init(myPrivateKey);
        keyAgreement.doPhase(peerPublicKey, true);
        return keyAgreement.generateSecret();
    }
}
