package org.omnione.did.base.datamodel;

import lombok.*;

/**
 * Description...
 *
 * @author : gwnam
 * @fileName : InvocationDidDoc
 * @since : 6/14/24
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class InvokedDidDoc {
    private String didDoc;
    private Provider controller;
    private String nonce;
    private Proof proof;
}
