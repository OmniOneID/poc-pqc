/*
 * Decompiled with CFR 0.152.
 */
package org.omnione.did.poc.pqc.server.datamodel.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ProofType {
    RSA_SIGNATURE_2018("RsaSignature2018"),
    SECP_256K1_SIGNATURE_2018("Secp256k1Signature2018"),
    SECP_256R1_SIGNATURE_2018("Secp256r1Signature2018"),
    ML_DSA_44_SIGNATURE_2024("MlDsa44Signature2024");

    private final String displayName;

    private ProofType(String displayName) {
        this.displayName = displayName;
    }

    @JsonValue
    public String getDisplayName() {
        return this.displayName;
    }
}

