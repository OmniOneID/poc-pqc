/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  lombok.Generated
 */
package org.omnione.did.poc.pqc.server.datamodel.data;

import java.util.List;
import lombok.Generated;
import org.omnione.did.crypto.enums.SymmetricCipherType;

public class Candidate {
    private List<SymmetricCipherType> ciphers;

    @Generated
    public static CandidateBuilder builder() {
        return new CandidateBuilder();
    }

    @Generated
    public List<SymmetricCipherType> getCiphers() {
        return this.ciphers;
    }

    @Generated
    public void setCiphers(List<SymmetricCipherType> ciphers) {
        this.ciphers = ciphers;
    }

    @Generated
    public Candidate() {
    }

    @Generated
    public Candidate(List<SymmetricCipherType> ciphers) {
        this.ciphers = ciphers;
    }

    @Generated
    public String toString() {
        return "Candidate(ciphers=" + String.valueOf(this.getCiphers()) + ")";
    }

    @Generated
    public static class CandidateBuilder {
        @Generated
        private List<SymmetricCipherType> ciphers;

        @Generated
        CandidateBuilder() {
        }

        @Generated
        public CandidateBuilder ciphers(List<SymmetricCipherType> ciphers) {
            this.ciphers = ciphers;
            return this;
        }

        @Generated
        public Candidate build() {
            return new Candidate(this.ciphers);
        }

        @Generated
        public String toString() {
            return "Candidate.CandidateBuilder(ciphers=" + String.valueOf(this.ciphers) + ")";
        }
    }
}

