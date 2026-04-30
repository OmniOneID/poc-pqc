/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  lombok.Generated
 */
package org.omnione.did.poc.pqc.server.datamodel.data;

import org.omnione.did.poc.pqc.server.datamodel.enums.EccCurveType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Generated;
import org.omnione.did.data.model.DataObject;
import org.omnione.did.data.model.did.Proof;
import org.omnione.did.data.model.util.json.GsonWrapper;

@Deprecated // ML-KEM-768 모드에서는 미사용. ECDH(Secp256r1) 모드 전용.
public class EcdhReqData
extends DataObject {
    @NotNull(message="reqEcdh.client cannot be null")
    private String client;
    @NotNull(message="reqEcdh.clientNonce cannot be null")
    private String clientNonce;
    @NotNull(message="reqEcdh.curve cannot be null")
    private EccCurveType curve;
    @NotNull(message="reqEcdh.publicKey cannot be null")
    private String publicKey;
    @Valid
    private Candidate candidate;
    @Valid
    private Proof proof;

    @Override
    public void fromJson(String s) {
        GsonWrapper gson = new GsonWrapper();
        EcdhReqData reqData = (EcdhReqData)gson.fromJson(s, this.getClass());
        this.client = reqData.client;
        this.clientNonce = reqData.clientNonce;
        this.curve = reqData.curve;
        this.publicKey = reqData.publicKey;
        this.candidate = reqData.candidate;
        this.proof = reqData.proof;
    }

    @Generated
    public static EcdhReqDataBuilder builder() {
        return new EcdhReqDataBuilder();
    }

    @Generated
    public String getClient() {
        return this.client;
    }

    @Generated
    public String getClientNonce() {
        return this.clientNonce;
    }

    @Generated
    public EccCurveType getCurve() {
        return this.curve;
    }

    @Generated
    public String getPublicKey() {
        return this.publicKey;
    }

    @Generated
    public Candidate getCandidate() {
        return this.candidate;
    }

    @Generated
    public Proof getProof() {
        return this.proof;
    }

    @Generated
    public void setClient(String client) {
        this.client = client;
    }

    @Generated
    public void setClientNonce(String clientNonce) {
        this.clientNonce = clientNonce;
    }

    @Generated
    public void setCurve(EccCurveType curve) {
        this.curve = curve;
    }

    @Generated
    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    @Generated
    public void setCandidate(Candidate candidate) {
        this.candidate = candidate;
    }

    @Generated
    public void setProof(Proof proof) {
        this.proof = proof;
    }

    @Generated
    public EcdhReqData() {
    }

    @Generated
    public EcdhReqData(String client, String clientNonce, EccCurveType curve, String publicKey, Candidate candidate, Proof proof) {
        this.client = client;
        this.clientNonce = clientNonce;
        this.curve = curve;
        this.publicKey = publicKey;
        this.candidate = candidate;
        this.proof = proof;
    }

    @Generated
    public String toString() {
        return "EcdhReqData(client=" + this.getClient() + ", clientNonce=" + this.getClientNonce() + ", curve=" + String.valueOf(this.getCurve()) + ", publicKey=" + this.getPublicKey() + ", candidate=" + String.valueOf(this.getCandidate()) + ", proof=" + String.valueOf(this.getProof()) + ")";
    }

    @Generated
    public static class EcdhReqDataBuilder {
        @Generated
        private String client;
        @Generated
        private String clientNonce;
        @Generated
        private EccCurveType curve;
        @Generated
        private String publicKey;
        @Generated
        private Candidate candidate;
        @Generated
        private Proof proof;

        @Generated
        EcdhReqDataBuilder() {
        }

        @Generated
        public EcdhReqDataBuilder client(String client) {
            this.client = client;
            return this;
        }

        @Generated
        public EcdhReqDataBuilder clientNonce(String clientNonce) {
            this.clientNonce = clientNonce;
            return this;
        }

        @Generated
        public EcdhReqDataBuilder curve(EccCurveType curve) {
            this.curve = curve;
            return this;
        }

        @Generated
        public EcdhReqDataBuilder publicKey(String publicKey) {
            this.publicKey = publicKey;
            return this;
        }

        @Generated
        public EcdhReqDataBuilder candidate(Candidate candidate) {
            this.candidate = candidate;
            return this;
        }

        @Generated
        public EcdhReqDataBuilder proof(Proof proof) {
            this.proof = proof;
            return this;
        }

        @Generated
        public EcdhReqData build() {
            return new EcdhReqData(this.client, this.clientNonce, this.curve, this.publicKey, this.candidate, this.proof);
        }

        @Generated
        public String toString() {
            return "EcdhReqData.EcdhReqDataBuilder(client=" + this.client + ", clientNonce=" + this.clientNonce + ", curve=" + String.valueOf(this.curve) + ", publicKey=" + this.publicKey + ", candidate=" + String.valueOf(this.candidate) + ", proof=" + String.valueOf(this.proof) + ")";
        }
    }
}

