package org.omnione.did.observer.service;

import org.omnione.did.observer.dto.TestReport;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class ReportService {

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();
    private final List<TestReport> reports = new CopyOnWriteArrayList<>();

    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(0L);
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(error -> emitters.remove(emitter));
        for (TestReport report : reports) {
            sendEvent(emitter, report);
        }
        return emitter;
    }

    public TestReport publish(TestReport report) {
        if (report.getTimestamp() == null) {
            report.setTimestamp(OffsetDateTime.now());
        }
        reports.add(report);
        for (SseEmitter emitter : emitters) {
            sendEvent(emitter, report);
        }
        return report;
    }

    public List<TestReport> getReports() {
        return new ArrayList<>(reports);
    }

    private void sendEvent(SseEmitter emitter, TestReport report) {
        try {
            emitter.send(SseEmitter.event().name("report").data(report));
        } catch (IOException e) {
            emitters.remove(emitter);
        }
    }
}
