/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  lombok.Generated
 */
package org.omnione.did.poc.pqc.server.datamodel.data;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Generated;

@JsonPropertyOrder(value={"walletId", "ownerDidDoc", "provider", "nonce", "proof"})
public class AttestedDidDoc
extends DidDoc {
    private String walletId;
    private String nonce;

    @Generated
    protected AttestedDidDoc(AttestedDidDocBuilder<?, ?> b) {
        super(b);
        this.walletId = b.walletId;
        this.nonce = b.nonce;
    }

    @Generated
    public static AttestedDidDocBuilder<?, ?> builder() {
        return new AttestedDidDocBuilderImpl();
    }

    @Generated
    public String getWalletId() {
        return this.walletId;
    }

    @Generated
    public String getNonce() {
        return this.nonce;
    }

    @Generated
    public void setWalletId(String walletId) {
        this.walletId = walletId;
    }

    @Generated
    public void setNonce(String nonce) {
        this.nonce = nonce;
    }

    @Generated
    public AttestedDidDoc() {
    }

    @Generated
    public AttestedDidDoc(String walletId, String nonce) {
        this.walletId = walletId;
        this.nonce = nonce;
    }

    @Override
    @Generated
    public String toString() {
        return "AttestedDidDoc(walletId=" + this.getWalletId() + ", nonce=" + this.getNonce() + ")";
    }

    @Generated
    public static abstract class AttestedDidDocBuilder<C extends AttestedDidDoc, B extends AttestedDidDocBuilder<C, B>>
    extends DidDoc.DidDocBuilder<C, B> {
        @Generated
        private String walletId;
        @Generated
        private String nonce;

        @Generated
        public B walletId(String walletId) {
            this.walletId = walletId;
            return (B)this.self();
        }

        @Generated
        public B nonce(String nonce) {
            this.nonce = nonce;
            return (B)this.self();
        }

        @Override
        @Generated
        protected abstract B self();

        @Override
        @Generated
        public abstract C build();

        @Override
        @Generated
        public String toString() {
            return "AttestedDidDoc.AttestedDidDocBuilder(super=" + super.toString() + ", walletId=" + this.walletId + ", nonce=" + this.nonce + ")";
        }
    }

    @Generated
    private static final class AttestedDidDocBuilderImpl
    extends AttestedDidDocBuilder<AttestedDidDoc, AttestedDidDocBuilderImpl> {
        @Generated
        private AttestedDidDocBuilderImpl() {
        }

        @Override
        @Generated
        protected AttestedDidDocBuilderImpl self() {
            return this;
        }

        @Override
        @Generated
        public AttestedDidDoc build() {
            return new AttestedDidDoc(this);
        }
    }
}

