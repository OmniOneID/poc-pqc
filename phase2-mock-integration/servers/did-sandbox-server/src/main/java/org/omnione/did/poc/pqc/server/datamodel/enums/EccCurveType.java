/*
 * Decompiled with CFR 0.152.
 */
package org.omnione.did.poc.pqc.server.datamodel.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import org.omnione.did.crypto.enums.DidKeyType;

public enum EccCurveType implements OpenDidEnum
{
    SECP_256_K1("Secp256k1"),
    SECP_256_R1("Secp256r1"),
    ML_DSA_44("MlDsa44");

    private final String displayName;

    private EccCurveType(String displayName) {
        this.displayName = displayName;
    }

    @JsonValue
    public String toString() {
        return this.displayName;
    }

    public org.omnione.did.crypto.enums.EccCurveType toOmnioneEccCurveType() {
        switch (this) {
            case SECP_256_K1: {
                return org.omnione.did.crypto.enums.EccCurveType.Secp256k1;
            }
            case SECP_256_R1: {
                return org.omnione.did.crypto.enums.EccCurveType.Secp256r1;
            }
        }
        throw new RuntimeException("Invalid ECC curve type: " + this.displayName);
    }

    public DidKeyType toOmnioneDidKeyType() {
        switch (this) {
            case SECP_256_K1: {
                return DidKeyType.SECP256K1_VERIFICATION_KEY_2018;
            }
            case SECP_256_R1: {
                return DidKeyType.SECP256R1_VERIFICATION_KEY_2018;
            }
            case ML_DSA_44: {
                return DidKeyType.ML_DSA_44_VERIFICATION_KEY_2024;
            }
        }
        return DidKeyType.RSA_VERIFICATION_KEY_2018;
    }

    @Override
    public String getValue() {
        return this.displayName;
    }
}

