/*
 * Decompiled with CFR 0.152.
 */
package org.omnione.did.poc.pqc.server.datamodel.data;

import java.security.PrivateKey;
import java.security.PublicKey;
import org.omnione.did.crypto.keypair.EcKeyPair;
import org.omnione.did.crypto.keypair.KeyPairInterface;

public class KeyPairInfo {
    private String algorithm;
    private KeyPairInterface keyPair;

    public KeyPairInfo(String algorithm, PrivateKey privateKey, PublicKey publicKey) {
        this.algorithm = algorithm;
        this.keyPair = new EcKeyPair(publicKey, privateKey);
    }

    public String getAlgorithm() {
        return this.algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    public KeyPairInterface getKeyPair() {
        return this.keyPair;
    }

    public void setKeyPair(KeyPairInterface keyPair) {
        this.keyPair = keyPair;
    }
}

