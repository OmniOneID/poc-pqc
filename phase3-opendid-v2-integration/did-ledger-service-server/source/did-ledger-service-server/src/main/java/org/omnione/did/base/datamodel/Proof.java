package org.omnione.did.base.datamodel;

import lombok.*;

/**
 * Description...
 *
 * @author : gwnam
 * @fileName : Proof
 * @since : 6/12/24
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class Proof {
    private String type;
    private String created;
    private String verificationMethod;
    private String proofPurpose;
    private String proofValue;
}
