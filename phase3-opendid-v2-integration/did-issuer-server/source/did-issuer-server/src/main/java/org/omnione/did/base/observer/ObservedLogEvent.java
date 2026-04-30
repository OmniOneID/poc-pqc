package org.omnione.did.base.observer;

import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
public class ObservedLogEvent {

    private String serviceName;
    private String type;
    private String method;
    private String uri;
    private String payload;
    private OffsetDateTime timestamp;
}
