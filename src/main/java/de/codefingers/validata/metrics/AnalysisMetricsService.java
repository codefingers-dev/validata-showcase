package de.codefingers.validata.metrics;

public interface AnalysisMetricsService {

    void recordAnalysis(long extractMs, long analysisMs, int flagCount, String riskLevel);
    void recordExtractionError();
    void recordError(String errorType);
    void recordDuplication(String type);
    void recordManualReview(String reason);
}
