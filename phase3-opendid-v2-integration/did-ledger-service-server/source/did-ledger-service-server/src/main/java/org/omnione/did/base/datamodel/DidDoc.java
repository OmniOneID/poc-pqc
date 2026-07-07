package org.omnione.did.base.datamodel;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

/**
 * Description...
 *
 * @author : gwnam
 * @fileName : DidDoc
 * @since : 6/14/24
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class DidDoc {
    private String id;
}
