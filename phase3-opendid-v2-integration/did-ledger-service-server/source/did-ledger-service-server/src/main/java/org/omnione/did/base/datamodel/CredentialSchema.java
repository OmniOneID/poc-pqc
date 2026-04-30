package org.omnione.did.base.datamodel;

import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * Description...
 *
 * @author : gwnam
 * @fileName : CredentialSchema
 * @since : 6/12/24
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class CredentialSchema {
    @NotNull(message = "vcMeta.credentialSchema.id cannot be null")
    private String id;
    @NotNull(message = "vcMeta.credentialSchema.type cannot be null")
    private String type;
}
