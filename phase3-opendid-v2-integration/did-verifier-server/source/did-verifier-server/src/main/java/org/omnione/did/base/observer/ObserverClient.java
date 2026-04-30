package org.omnione.did.base.observer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.OffsetDateTime;

@Slf4j
@Service
public class ObserverClient {

    private final RestClient restClient;
    private final String serviceName;

    public ObserverClient(
            @Value("${observer.url:http://127.0.0.1:18080}") String observerUrl,
            @Value("${observer.service-name:did-verifier-server}") String serviceName
    ) {
        this.restClient = RestClient.builder()
                .baseUrl(observerUrl)
                .build();
        this.serviceName = serviceName;
    }

    @Async
    public void send(String type, String method, String uri, String payload, OffsetDateTime timestamp) {
        ObservedLogEvent event = new ObservedLogEvent();
        event.setServiceName(serviceName);
        event.setType(type);
        event.setMethod(method);
        event.setUri(uri);
        event.setPayload(payload);
        event.setTimestamp(timestamp);

        try {
            restClient.post()
                    .uri("/api/logs")
                    .body(event)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception exception) {
            log.debug("Failed to send observer event: {}", exception.getMessage());
        }
    }
}
