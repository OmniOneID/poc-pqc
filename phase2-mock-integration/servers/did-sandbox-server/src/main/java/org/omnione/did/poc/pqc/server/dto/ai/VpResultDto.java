/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  lombok.Generated
 */
package org.omnione.did.poc.pqc.server.dto.ai;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Arrays;
import lombok.Generated;

@JsonInclude(value=JsonInclude.Include.NON_NULL)
public class VpResultDto {
    private String txId;
    private String payload;
    private String payloadType;
    private String validUntil;
    private String offerId;
    private byte[] qrImage;
    private String institution;

    @Generated
    public static VpResultDtoBuilder builder() {
        return new VpResultDtoBuilder();
    }

    @Generated
    public String getTxId() {
        return this.txId;
    }

    @Generated
    public String getPayload() {
        return this.payload;
    }

    @Generated
    public String getPayloadType() {
        return this.payloadType;
    }

    @Generated
    public String getValidUntil() {
        return this.validUntil;
    }

    @Generated
    public String getOfferId() {
        return this.offerId;
    }

    @Generated
    public byte[] getQrImage() {
        return this.qrImage;
    }

    @Generated
    public String getInstitution() {
        return this.institution;
    }

    @Generated
    public void setTxId(String txId) {
        this.txId = txId;
    }

    @Generated
    public void setPayload(String payload) {
        this.payload = payload;
    }

    @Generated
    public void setPayloadType(String payloadType) {
        this.payloadType = payloadType;
    }

    @Generated
    public void setValidUntil(String validUntil) {
        this.validUntil = validUntil;
    }

    @Generated
    public void setOfferId(String offerId) {
        this.offerId = offerId;
    }

    @Generated
    public void setQrImage(byte[] qrImage) {
        this.qrImage = qrImage;
    }

    @Generated
    public void setInstitution(String institution) {
        this.institution = institution;
    }

    @Generated
    public VpResultDto() {
    }

    @Generated
    public VpResultDto(String txId, String payload, String payloadType, String validUntil, String offerId, byte[] qrImage, String institution) {
        this.txId = txId;
        this.payload = payload;
        this.payloadType = payloadType;
        this.validUntil = validUntil;
        this.offerId = offerId;
        this.qrImage = qrImage;
        this.institution = institution;
    }

    @Generated
    public String toString() {
        return "VpResultDto(txId=" + this.getTxId() + ", payload=" + this.getPayload() + ", payloadType=" + this.getPayloadType() + ", validUntil=" + this.getValidUntil() + ", offerId=" + this.getOfferId() + ", qrImage=" + Arrays.toString(this.getQrImage()) + ", institution=" + this.getInstitution() + ")";
    }

    @Generated
    public static class VpResultDtoBuilder {
        @Generated
        private String txId;
        @Generated
        private String payload;
        @Generated
        private String payloadType;
        @Generated
        private String validUntil;
        @Generated
        private String offerId;
        @Generated
        private byte[] qrImage;
        @Generated
        private String institution;

        @Generated
        VpResultDtoBuilder() {
        }

        @Generated
        public VpResultDtoBuilder txId(String txId) {
            this.txId = txId;
            return this;
        }

        @Generated
        public VpResultDtoBuilder payload(String payload) {
            this.payload = payload;
            return this;
        }

        @Generated
        public VpResultDtoBuilder payloadType(String payloadType) {
            this.payloadType = payloadType;
            return this;
        }

        @Generated
        public VpResultDtoBuilder validUntil(String validUntil) {
            this.validUntil = validUntil;
            return this;
        }

        @Generated
        public VpResultDtoBuilder offerId(String offerId) {
            this.offerId = offerId;
            return this;
        }

        @Generated
        public VpResultDtoBuilder qrImage(byte[] qrImage) {
            this.qrImage = qrImage;
            return this;
        }

        @Generated
        public VpResultDtoBuilder institution(String institution) {
            this.institution = institution;
            return this;
        }

        @Generated
        public VpResultDto build() {
            return new VpResultDto(this.txId, this.payload, this.payloadType, this.validUntil, this.offerId, this.qrImage, this.institution);
        }

        @Generated
        public String toString() {
            return "VpResultDto.VpResultDtoBuilder(txId=" + this.txId + ", payload=" + this.payload + ", payloadType=" + this.payloadType + ", validUntil=" + this.validUntil + ", offerId=" + this.offerId + ", qrImage=" + Arrays.toString(this.qrImage) + ", institution=" + this.institution + ")";
        }
    }
}

