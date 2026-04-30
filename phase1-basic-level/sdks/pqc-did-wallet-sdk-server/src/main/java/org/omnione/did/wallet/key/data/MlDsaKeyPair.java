// ML-DSA-44 KeyPair wrapper
package org.omnione.did.wallet.key.data;

import java.security.PrivateKey;
import java.security.PublicKey;

import org.omnione.did.crypto.keypair.KeyPairInterface;

public class MlDsaKeyPair implements KeyPairInterface {

    private PublicKey publicKey;
    private PrivateKey privateKey;

    public MlDsaKeyPair(PublicKey publicKey, PrivateKey privateKey) {
        this.publicKey = publicKey;
        this.privateKey = privateKey;
    }

    @Override
    public PublicKey getPublicKey() {
        return publicKey;
    }

    @Override
    public void setPublicKey(PublicKey publicKey) {
        this.publicKey = publicKey;
    }

    @Override
    public PrivateKey getPrivateKey() {
        return privateKey;
    }

    @Override
    public void setPrivateKey(PrivateKey privateKey) {
        this.privateKey = privateKey;
    }
}
