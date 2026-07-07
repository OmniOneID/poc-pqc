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
import org.omnione.did.data.model.provider.Provider;

public class DidDoc {
    @NotNull(message="didDoc.ownerDidDoc cannot be null")
    private String ownerDidDoc;
    private Provider provider;
    private String did;
    @Valid
    @NotNull(message="didDoc.proof cannot be null")
    private Proof proof;

    @Generated
    protected DidDoc(DidDocBuilder<?, ?> b) {
        this.ownerDidDoc = b.ownerDidDoc;
        this.provider = b.provider;
        this.did = b.did;
        this.proof = b.proof;
    }

    @Generated
    public static DidDocBuilder<?, ?> builder() {
        return new DidDocBuilderImpl();
    }

    @Generated
    public String getOwnerDidDoc() {
        return this.ownerDidDoc;
    }

    @Generated
    public Provider getProvider() {
        return this.provider;
    }

    @Generated
    public String getDid() {
        return this.did;
    }

    @Generated
    public Proof getProof() {
        return this.proof;
    }

    @Generated
    public void setOwnerDidDoc(String ownerDidDoc) {
        this.ownerDidDoc = ownerDidDoc;
    }

    @Generated
    public void setProvider(Provider provider) {
        this.provider = provider;
    }

    @Generated
    public void setDid(String did) {
        this.did = did;
    }

    @Generated
    public void setProof(Proof proof) {
        this.proof = proof;
    }

    @Generated
    public DidDoc() {
    }

    @Generated
    public DidDoc(String ownerDidDoc, Provider provider, String did, Proof proof) {
        this.ownerDidDoc = ownerDidDoc;
        this.provider = provider;
        this.did = did;
        this.proof = proof;
    }

    @Generated
    public String toString() {
        return "DidDoc(ownerDidDoc=" + this.getOwnerDidDoc() + ", provider=" + String.valueOf(this.getProvider()) + ", did=" + this.getDid() + ", proof=" + String.valueOf(this.getProof()) + ")";
    }

    @Generated
    public static abstract class DidDocBuilder<C extends DidDoc, B extends DidDocBuilder<C, B>> {
        @Generated
        private String ownerDidDoc;
        @Generated
        private Provider provider;
        @Generated
        private String did;
        @Generated
        private Proof proof;

        @Generated
        public B ownerDidDoc(String ownerDidDoc) {
            this.ownerDidDoc = ownerDidDoc;
            return this.self();
        }

        @Generated
        public B provider(Provider provider) {
            this.provider = provider;
            return this.self();
        }

        @Generated
        public B did(String did) {
            this.did = did;
            return this.self();
        }

        @Generated
        public B proof(Proof proof) {
            this.proof = proof;
            return this.self();
        }

        @Generated
        protected abstract B self();

        @Generated
        public abstract C build();

        @Generated
        public String toString() {
            return "DidDoc.DidDocBuilder(ownerDidDoc=" + this.ownerDidDoc + ", provider=" + String.valueOf(this.provider) + ", did=" + this.did + ", proof=" + String.valueOf(this.proof) + ")";
        }
    }

    @Generated
    private static final class DidDocBuilderImpl
    extends DidDocBuilder<DidDoc, DidDocBuilderImpl> {
        @Generated
        private DidDocBuilderImpl() {
        }

        @Override
        @Generated
        protected DidDocBuilderImpl self() {
            return this;
        }

        @Override
        @Generated
        public DidDoc build() {
            return new DidDoc(this);
        }
    }
}

