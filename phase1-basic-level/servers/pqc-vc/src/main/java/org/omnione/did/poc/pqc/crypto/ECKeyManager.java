package org.omnione.did.poc.pqc.crypto;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.Signature;
import java.security.spec.ECGenParameterSpec;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

public class ECKeyManager {

    private static final String KEY_ALGORITHM = "EC";
    private static final String SIGNATURE_ALGORITHM = "SHA256withECDSA";
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

    public byte[] sign(PrivateKey privateKey, byte[] data) throws Exception {
        Signature signer = Signature.getInstance(SIGNATURE_ALGORITHM, PROVIDER);
        signer.initSign(privateKey, new SecureRandom());
        signer.update(data);
        return signer.sign();
    }

    public boolean verify(PublicKey publicKey, byte[] data, byte[] signature) throws Exception {
        Signature verifier = Signature.getInstance(SIGNATURE_ALGORITHM, PROVIDER);
        verifier.initVerify(publicKey);
        verifier.update(data);
        return verifier.verify(signature);
    }
}
