package org.omnione.did.observer.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.Map;

@Getter
@Setter
public class TestReport {

    private String scenario;
    private String algorithm;
    private String vcPlanId;
    private String vpPolicyId;
    private int warmupCount;
    private int measurementCount;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    private OffsetDateTime timestamp;

    private Map<String, MetricSummary> metrics;

    @Getter
    @Setter
    public static class MetricSummary {
        private double mean;
        private double p50;
        private double p95;
        private double min;
        private double max;
    }
}
