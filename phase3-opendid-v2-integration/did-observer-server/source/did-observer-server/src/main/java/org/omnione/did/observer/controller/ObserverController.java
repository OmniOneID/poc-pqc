package org.omnione.did.observer.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.omnione.did.observer.dto.ObservedLogEvent;
import org.omnione.did.observer.dto.TestReport;
import org.omnione.did.observer.service.LogStreamService;
import org.omnione.did.observer.service.ReportService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class ObserverController {

    private final LogStreamService logStreamService;
    private final ReportService reportService;

    @GetMapping({"/", "/observer"})
    public String observer() {
        return "forward:/observer.html";
    }

    @GetMapping("/report")
    public String report() {
        return "forward:/report.html";
    }

    @GetMapping(value = "/api/log-stream", produces = "text/event-stream")
    @ResponseBody
    public SseEmitter logStream() {
        return logStreamService.subscribe();
    }

    @GetMapping("/api/logs")
    @ResponseBody
    public List<ObservedLogEvent> logs() {
        return logStreamService.getHistory();
    }

    @PostMapping("/api/logs")
    @ResponseBody
    public ObservedLogEvent publish(@Valid @RequestBody ObservedLogEvent event) {
        return logStreamService.publish(event);
    }

    @PostMapping("/api/logs/clear")
    @ResponseBody
    public void clear() {
        logStreamService.clear();
    }

    @GetMapping(value = "/api/report-stream", produces = "text/event-stream")
    @ResponseBody
    public SseEmitter reportStream() {
        return reportService.subscribe();
    }

    @GetMapping("/api/reports")
    @ResponseBody
    public List<TestReport> reports() {
        return reportService.getReports();
    }

    @PostMapping("/api/reports")
    @ResponseBody
    public TestReport publishReport(@RequestBody TestReport report) {
        return reportService.publish(report);
    }
}
