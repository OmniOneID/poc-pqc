package org.omnione.did.base.datamodel;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * Description...
 *
 * @author : gwnam
 * @fileName : ProviderDetails
 * @since : 6/12/24
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@SuperBuilder
public class Provider {
    @NotNull(message = "vcMeta.provider.did cannot be null")
    private String did;
    private String certVcRef;
}
