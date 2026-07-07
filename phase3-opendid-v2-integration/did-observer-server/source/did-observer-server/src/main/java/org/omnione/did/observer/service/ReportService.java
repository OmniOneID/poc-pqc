package org.omnione.did.observer.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.omnione.did.observer.dto.TestReport;
import org.omnione.did.observer.entity.TestReportEntity;
import org.omnione.did.observer.repository.TestReportRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final TestReportRepository repository;
    private final ObjectMapper objectMapper;

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(0L);
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(error -> emitters.remove(emitter));
        for (TestReport report : getReports()) {
            sendEvent(emitter, report);
        }
        return emitter;
    }

    @Transactional
    public TestReport publish(TestReport report) {
        if (report.getTimestamp() == null) {
            report.setTimestamp(OffsetDateTime.now());
        }
        repository.save(toEntity(report));
        for (SseEmitter emitter : emitters) {
            sendEvent(emitter, report);
        }
        return report;
    }

    @Transactional(readOnly = true)
    public List<TestReport> getReports() {
        List<TestReportEntity> entities = repository.findAllByOrderByIdAsc();
        List<TestReport> result = new ArrayList<>(entities.size());
        for (TestReportEntity entity : entities) {
            result.add(toDto(entity));
        }
        return result;
    }

    private void sendEvent(SseEmitter emitter, TestReport report) {
        try {
            emitter.send(SseEmitter.event().name("report").data(report));
        } catch (IOException e) {
            emitters.remove(emitter);
        }
    }

    private TestReportEntity toEntity(TestReport dto) {
        TestReportEntity entity = new TestReportEntity();
        entity.setScenario(dto.getScenario());
        entity.setAlgorithm(dto.getAlgorithm());
        entity.setVcPlanId(dto.getVcPlanId());
        entity.setVpPolicyId(dto.getVpPolicyId());
        entity.setWarmupCount(dto.getWarmupCount());
        entity.setMeasurementCount(dto.getMeasurementCount());
        entity.setTimestamp(dto.getTimestamp());
        try {
            entity.setMetricsJson(objectMapper.writeValueAsString(dto.getMetrics()));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize metrics", e);
        }
        return entity;
    }

    private TestReport toDto(TestReportEntity entity) {
        TestReport dto = new TestReport();
        dto.setScenario(entity.getScenario());
        dto.setAlgorithm(entity.getAlgorithm());
        dto.setVcPlanId(entity.getVcPlanId());
        dto.setVpPolicyId(entity.getVpPolicyId());
        dto.setWarmupCount(entity.getWarmupCount());
        dto.setMeasurementCount(entity.getMeasurementCount());
        dto.setTimestamp(entity.getTimestamp());
        if (entity.getMetricsJson() != null) {
            try {
                Map<String, TestReport.MetricSummary> metrics = objectMapper.readValue(
                        entity.getMetricsJson(),
                        new TypeReference<LinkedHashMap<String, TestReport.MetricSummary>>() {}
                );
                dto.setMetrics(metrics);
            } catch (IOException e) {
                throw new IllegalStateException("Failed to deserialize metrics", e);
            }
        }
        return dto;
    }
}
