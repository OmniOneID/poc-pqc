/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  lombok.Generated
 */
package org.omnione.did.poc.pqc.server.datamodel.data;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Generated;
import org.omnione.did.data.model.did.Proof;

public class DidAuth {
    @NotNull(message="didAuth.id cannot be null")
    private String did;
    @NotNull(message="didAuth.authNonce cannot be null")
    private String authNonce;
    @Valid
    private Proof proof;

    @Generated
    public static DidAuthBuilder builder() {
        return new DidAuthBuilder();
    }

    @Generated
    public String getDid() {
        return this.did;
    }

    @Generated
    public String getAuthNonce() {
        return this.authNonce;
    }

    @Generated
    public Proof getProof() {
        return this.proof;
    }

    @Generated
    public void setDid(String did) {
        this.did = did;
    }

    @Generated
    public void setAuthNonce(String authNonce) {
        this.authNonce = authNonce;
    }

    @Generated
    public void setProof(Proof proof) {
        this.proof = proof;
    }

    @Generated
    public DidAuth() {
    }

    @Generated
    public DidAuth(String did, String authNonce, Proof proof) {
        this.did = did;
        this.authNonce = authNonce;
        this.proof = proof;
    }

    @Generated
    public String toString() {
        return "DidAuth(did=" + this.getDid() + ", authNonce=" + this.getAuthNonce() + ", proof=" + String.valueOf(this.getProof()) + ")";
    }

    @Generated
    public static class DidAuthBuilder {
        @Generated
        private String did;
        @Generated
        private String authNonce;
        @Generated
        private Proof proof;

        @Generated
        DidAuthBuilder() {
        }

        @Generated
        public DidAuthBuilder did(String did) {
            this.did = did;
            return this;
        }

        @Generated
        public DidAuthBuilder authNonce(String authNonce) {
            this.authNonce = authNonce;
            return this;
        }

        @Generated
        public DidAuthBuilder proof(Proof proof) {
            this.proof = proof;
            return this;
        }

        @Generated
        public DidAuth build() {
            return new DidAuth(this.did, this.authNonce, this.proof);
        }

        @Generated
        public String toString() {
            return "DidAuth.DidAuthBuilder(did=" + this.did + ", authNonce=" + this.authNonce + ", proof=" + String.valueOf(this.proof) + ")";
        }
    }
}

