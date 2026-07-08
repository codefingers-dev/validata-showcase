package de.codefingers.validata.metrics;


import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.Counter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Prometheus Metrics für Validata Fraud Detection
 *
 * Tracked:
 * - Extraction Time (Textract Performance)
 * - Rules Processing Time
 * - Red Flags Count
 * - Analysis Count by Risk Level
 * - Error Count by Type
 *
 * Accessible via: http://localhost:8080/actuator/prometheus
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AnalysisMetrics implements AnalysisMetricsService {

    private final MeterRegistry meterRegistry;

    /**
     * Record eine komplette Analyse
     */
    public void recordAnalysis(long extractionMs,
                               long rulesMs,
                               int redFlagCount,
                               String riskLevel) {

        // 1. Extraction Time
        meterRegistry.timer("validata.extraction.time")
                .record(extractionMs, TimeUnit.MILLISECONDS);

        // 2. Rules Processing Time
        meterRegistry.timer("validata.rules.time")
                .record(rulesMs, TimeUnit.MILLISECONDS);

        // 3. Red Flags Count by Risk Level
        meterRegistry.counter("validata.redflags.total",
                        "level", riskLevel)
                .increment(redFlagCount);

        // 4. Total Analysis Count by Risk Level
        meterRegistry.counter("validata.analysis.total",
                        "level", riskLevel)
                .increment();

        // 5. Total Processing Time (extraction + rules)
        long totalMs = extractionMs + rulesMs;
        meterRegistry.timer("validata.analysis.total.time")
                .record(totalMs, TimeUnit.MILLISECONDS);

        log.debug("Metrics recorded: extraction={}ms, rules={}ms, flags={}, level={}",
                extractionMs, rulesMs, redFlagCount, riskLevel);
    }

    /**
     * Record Fehler
     */
    public void recordError(String errorType) {
        meterRegistry.counter("validata.errors.total",
                        "type", errorType)
                .increment();

        log.warn("Error recorded: {}", errorType);
    }

    /**
     * Record Textract-spezifische Fehler
     */
    public void recordExtractionError() {
        recordError("EXTRACTION_FAILED");
    }

    /**
     * Record Duplication Detection
     */
    public void recordDuplication(String duplicationType) {
        meterRegistry.counter("validata.duplicates.total",
                        "type", duplicationType)
                .increment();

        log.info("Duplication recorded: {}", duplicationType);
    }

    /**
     * Record Manual Review (Graceful Degradation)
     */
    public void recordManualReview(String reason) {
        meterRegistry.counter("validata.manual.review.total",
                        "reason", reason)
                .increment();

        log.info("Manual review triggered: {}", reason);
    }

    /**
     * Gauge: Aktive Analysen (optional, für fortgeschrittenes Monitoring)
     */
    public void recordConcurrentAnalysis(int count) {
        meterRegistry.gauge("validata.analysis.concurrent", count);
    }
}
