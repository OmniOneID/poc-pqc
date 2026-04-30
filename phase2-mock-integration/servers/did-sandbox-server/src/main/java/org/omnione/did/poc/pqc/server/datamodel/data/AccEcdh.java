/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  lombok.Generated
 */
package org.omnione.did.poc.pqc.server.datamodel.data;

import lombok.Generated;
import org.omnione.did.crypto.enums.SymmetricCipherType;
import org.omnione.did.crypto.enums.SymmetricPaddingType;
import org.omnione.did.data.model.did.Proof;

@Deprecated // ML-KEM-768 모드에서는 미사용. ECDH(Secp256r1) 모드 전용.
public class AccEcdh {
    private String server;
    private String serverNonce;
    private String publicKey;
    private SymmetricCipherType cipher;
    private SymmetricPaddingType padding;
    private Proof proof;

    @Generated
    public static AccEcdhBuilder builder() {
        return new AccEcdhBuilder();
    }

    @Generated
    public String getServer() {
        return this.server;
    }

    @Generated
    public String getServerNonce() {
        return this.serverNonce;
    }

    @Generated
    public String getPublicKey() {
        return this.publicKey;
    }

    @Generated
    public SymmetricCipherType getCipher() {
        return this.cipher;
    }

    @Generated
    public SymmetricPaddingType getPadding() {
        return this.padding;
    }

    @Generated
    public Proof getProof() {
        return this.proof;
    }

    @Generated
    public void setServer(String server) {
        this.server = server;
    }

    @Generated
    public void setServerNonce(String serverNonce) {
        this.serverNonce = serverNonce;
    }

    @Generated
    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    @Generated
    public void setCipher(SymmetricCipherType cipher) {
        this.cipher = cipher;
    }

    @Generated
    public void setPadding(SymmetricPaddingType padding) {
        this.padding = padding;
    }

    @Generated
    public void setProof(Proof proof) {
        this.proof = proof;
    }

    @Generated
    public AccEcdh() {
    }

    @Generated
    public AccEcdh(String server, String serverNonce, String publicKey, SymmetricCipherType cipher, SymmetricPaddingType padding, Proof proof) {
        this.server = server;
        this.serverNonce = serverNonce;
        this.publicKey = publicKey;
        this.cipher = cipher;
        this.padding = padding;
        this.proof = proof;
    }

    @Generated
    public String toString() {
        return "AccEcdh(server=" + this.getServer() + ", serverNonce=" + this.getServerNonce() + ", publicKey=" + this.getPublicKey() + ", cipher=" + String.valueOf((Object)this.getCipher()) + ", padding=" + String.valueOf((Object)this.getPadding()) + ", proof=" + String.valueOf(this.getProof()) + ")";
    }

    @Generated
    public static class AccEcdhBuilder {
        @Generated
        private String server;
        @Generated
        private String serverNonce;
        @Generated
        private String publicKey;
        @Generated
        private SymmetricCipherType cipher;
        @Generated
        private SymmetricPaddingType padding;
        @Generated
        private Proof proof;

        @Generated
        AccEcdhBuilder() {
        }

        @Generated
        public AccEcdhBuilder server(String server) {
            this.server = server;
            return this;
        }

        @Generated
        public AccEcdhBuilder serverNonce(String serverNonce) {
            this.serverNonce = serverNonce;
            return this;
        }

        @Generated
        public AccEcdhBuilder publicKey(String publicKey) {
            this.publicKey = publicKey;
            return this;
        }

        @Generated
        public AccEcdhBuilder cipher(SymmetricCipherType cipher) {
            this.cipher = cipher;
            return this;
        }

        @Generated
        public AccEcdhBuilder padding(SymmetricPaddingType padding) {
            this.padding = padding;
            return this;
        }

        @Generated
        public AccEcdhBuilder proof(Proof proof) {
            this.proof = proof;
            return this;
        }

        @Generated
        public AccEcdh build() {
            return new AccEcdh(this.server, this.serverNonce, this.publicKey, this.cipher, this.padding, this.proof);
        }

        @Generated
        public String toString() {
            return "AccEcdh.AccEcdhBuilder(server=" + this.server + ", serverNonce=" + this.serverNonce + ", publicKey=" + this.publicKey + ", cipher=" + String.valueOf((Object)this.cipher) + ", padding=" + String.valueOf((Object)this.padding) + ", proof=" + String.valueOf(this.proof) + ")";
        }
    }
}

