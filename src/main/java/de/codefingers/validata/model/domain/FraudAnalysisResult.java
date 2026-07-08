package de.codefingers.validata.model.domain;


import de.codefingers.validata.model.dto.InvoiceData;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class FraudAnalysisResult {
    private String id;
    private int riskScore;
    private String riskLevel;
    private String recommendation;
    private List<RedFlag> redFlags;
    private InvoiceData extractedData;
    private String summary;
    private double confidence;
    private String engineUsed;
    private String promptVersion;
    private AnalysisMode analysisMode;
    private Instant analyzedAt;
    private long processingTimeMs;

    // ===== NEU: Layer Results =====
    @Builder.Default
    private List<LayerResult> layerResults = new ArrayList<>();

    // ===== NEU: Score Breakdown =====
    @Builder.Default
    private List<ScoreBreakdown> scoreBreakdown = new ArrayList<>();

    // ===== NEU: Processing Details =====
    @Builder.Default
    private int layersExecuted = 0;
    @Builder.Default
    private int layersPassed = 0;
    @Builder.Default
    private int layersFailed = 0;
    @Builder.Default
    private int totalRulesEvaluated = 0;

    // ===== NEU: Formale Validierung =====
    private ValidationResult formalValidation;

    // ===== NEU: Financial =====
    @Builder.Default
    private int lineItemsChecked = 0;
    @Builder.Default
    private int lineItemsFlagged = 0;

    // ===== NEU: Audit Trail =====
    private String triggeredBy;
    private Decision decision;
    private String decisionReason;
    private Instant decisionTimestamp;
    private String decidedBy;

    // ===== NEU: Engine Version =====
    @Builder.Default
    private String engineVersion = "2.0.0";




    // ===== NEU: Query Methods =====
    public boolean hasCriticalFlags() {
        return redFlags.stream()
                .anyMatch(f -> f.getSeverity() == RedFlag.Severity.HIGH);
    }

    public boolean hasSystemErrors() {
        return redFlags.stream()
                .anyMatch(f -> f.getCategory() == RedFlag.Category.SYSTEM);
    }

    // ===== NEU: Enums =====
    public enum Decision {
        PENDING, APPROVED, REJECTED, ESCALATED, INVESTIGATION
    }

    // ===== NEU: Inner Classes =====
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LayerResult {
        private String layerName;
        private String implementationClass;
        private String version;
        @Builder.Default
        private boolean passed = true;
        @Builder.Default
        private int flagsDetected = 0;
        @Builder.Default
        private int rulesEvaluated = 0;
        @Builder.Default
        private long processingTimeMs = 0;
        @Builder.Default
        private int scoreContribution = 0;
        @Builder.Default
        private List<String> details = new ArrayList<>();
        private String errorMessage;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ScoreBreakdown {
        private String layerName;
        private int scoreContribution;
        private int flagCount;
        @Builder.Default
        private List<String> details = new ArrayList<>();
    }
}