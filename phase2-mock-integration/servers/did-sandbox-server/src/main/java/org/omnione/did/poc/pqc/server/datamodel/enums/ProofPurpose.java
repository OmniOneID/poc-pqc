/*
 * Decompiled with CFR 0.152.
 */
package org.omnione.did.poc.pqc.server.datamodel.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ProofPurpose {
    ASSERTION_METHOD("assertionMethod"),
    AUTHENTICATION("authentication"),
    KEY_AGREEMENT("keyAgreement"),
    CAPABILITY_INVOCATION("capabilityInvocation"),
    CAPABILITY_DELEGATION("capabilityDelegation");

    private final String displayName;

    private ProofPurpose(String displayName) {
        this.displayName = displayName;
    }

    @JsonValue
    public String toString() {
        return this.displayName;
    }

    public String toKeyId() {
        switch (this.ordinal()) {
            case 0: {
                return "assert";
            }
            case 1: {
                return "auth";
            }
            case 2: {
                return "keyagree";
            }
            case 3: {
                return "invoke";
            }
        }
        throw new RuntimeException("Invalid ProofPurpose");
    }

    public static ProofPurpose fromDisplayName(String displayName) {
        for (ProofPurpose purpose : ProofPurpose.values()) {
            if (!purpose.displayName.equalsIgnoreCase(displayName)) continue;
            return purpose;
        }
        throw new RuntimeException("Invalid ProofPurpose");
    }
}

