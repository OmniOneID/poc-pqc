package org.omnione.did.observer.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
public class ObservedLogEvent {

    @NotBlank
    private String serviceName;

    @NotBlank
    private String type;

    private String method;

    private String uri;

    private String payload;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    private OffsetDateTime timestamp;
}
