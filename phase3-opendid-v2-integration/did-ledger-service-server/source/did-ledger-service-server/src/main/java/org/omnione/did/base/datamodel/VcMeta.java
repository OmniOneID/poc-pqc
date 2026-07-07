package org.omnione.did.base.datamodel;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.Instant;

/**
 * Description...
 *
 * @author : gwnam
 * @fileName : VcMeta
 * @since : 6/12/24
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class VcMeta {
    @NotNull(message = "vcMeta.id cannot be null")
    private String id;
    @Valid
    @NotNull(message = "vcMeta.issuer cannot be null")
    private ProviderDetails issuer;
    @NotNull(message = "vcMeta.subject cannot be null")
    private String subject;
    @Valid
    @NotNull(message = "vcMeta.credentialSchema cannot be null")
    private CredentialSchema credentialSchema;
    @NotNull(message = "vcMeta.status cannot be null")
    private String status;
    @NotNull(message = "vcMeta.issuanceDate cannot be null")
    private String issuanceDate;
    @NotNull(message = "vcMeta.validFrom cannot be null")
    private String validFrom;
    @NotNull(message = "vcMeta.validUntil cannot be null")
    private String validUntil;
    @NotNull(message = "vcMeta.formatVersion cannot be null")
    private String formatVersion;
    @NotNull(message = "vcMeta.language cannot be null")
    private String language;
}
