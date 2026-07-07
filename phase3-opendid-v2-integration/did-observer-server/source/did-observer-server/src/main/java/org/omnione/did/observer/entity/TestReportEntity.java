package org.omnione.did.observer.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "test_report")
@Getter
@Setter
public class TestReportEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String scenario;
    private String algorithm;
    private String vcPlanId;
    private String vpPolicyId;
    private int warmupCount;
    private int measurementCount;
    private OffsetDateTime timestamp;

    @Column(columnDefinition = "TEXT")
    private String metricsJson;
}
