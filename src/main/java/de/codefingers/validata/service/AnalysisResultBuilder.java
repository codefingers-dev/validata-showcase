package de.codefingers.validata.service;

import de.codefingers.validata.model.domain.AnalysisMode;
import de.codefingers.validata.model.domain.FraudAnalysisResult;
import de.codefingers.validata.model.domain.RedFlag;
import de.codefingers.validata.model.domain.ValidationResult;
import de.codefingers.validata.model.dto.InvoiceData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Baut FraudAnalysisResult Objekte.
 * Extrahiert aus Orchestrator für Single Responsibility.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisResultBuilder {

    /**
     * Baut ein erfolgreiches Analyse-Ergebnis.
     */
    public FraudAnalysisResult buildSuccess(InvoiceData data,
                                            List<RedFlag> flags,
                                            int riskScore,
                                            String riskLevel,
                                            String recommendation, ValidationResult formalValidation) {
        return FraudAnalysisResult.builder()
                .riskScore(riskScore)
                .riskLevel(riskLevel)
                .recommendation(recommendation)
                .redFlags(flags)
                .extractedData(data)
                .formalValidation(formalValidation)
                .summary(buildSummary(flags, riskLevel))
                .confidence(calculateConfidence(flags))
                .engineUsed("Pure Rules-Based (v2.0)")
                .promptVersion("N/A")
                .analysisMode(AnalysisMode.RULES_ONLY)
                .analyzedAt(Instant.now())
                .build();
    }

    /**
     * Baut ein Duplikat-Ergebnis (Layer 6 Early Exit).
     */
    public FraudAnalysisResult buildDuplicate(InvoiceData data,
                                              List<RedFlag> redFlags,
                                              String type,
                                              double confidence,
                                              int points) {
        return FraudAnalysisResult.builder()
                .riskScore(Math.min(100, 100 + points))
                .riskLevel("CRITICAL")
                .recommendation("REJECT")
                .redFlags(redFlags)
                .extractedData(data)
                .summary("Rechnung wurde bereits eingereicht (" + type + ")")
                .confidence(confidence)
                .engineUsed("Duplication Detection")
                .analysisMode(AnalysisMode.RULES_ONLY)
                .analyzedAt(Instant.now())
                .build();
    }

    /**
     * Baut ein Invalid-Invoice Ergebnis.
     */
    public FraudAnalysisResult buildInvalid(String errorMessage,
                                            String source,
                                            long processingTimeMs) {
        return FraudAnalysisResult.builder()
                .riskScore(0)
                .riskLevel("INVALID")
                .recommendation("REJECT")
                .redFlags(List.of(
                        RedFlag.invalidInvoice("INVALID_INVOICE", errorMessage)))
                .extractedData(null)
                .summary("Rechnung ist ungültig: " + errorMessage)
                .confidence(1.0)
                .engineUsed(source)
                .analysisMode(AnalysisMode.RULES_ONLY)
                .analyzedAt(Instant.now())
                .processingTimeMs(processingTimeMs)
                .build();
    }

    /**
     * Baut ein Graceful-Degradation Ergebnis.
     */
    public FraudAnalysisResult buildFallback(String reason, long processingTimeMs) {
        return FraudAnalysisResult.builder()
                .riskScore(25)
                .riskLevel("MANUAL_REVIEW")
                .recommendation("MANUAL_REVIEW_REQUIRED")
                .redFlags(List.of(
                        RedFlag.systemError("PDF_EXTRACTION_FAILED", reason)))
                .extractedData(null)
                .summary(reason + " Bitte manuell überprüfen.")
                .confidence(0.0)
                .engineUsed("Manual Review (Fallback)")
                .analysisMode(AnalysisMode.RULES_ONLY)
                .analyzedAt(Instant.now())
                .processingTimeMs(processingTimeMs)
                .build();
    }

    // ========================================================================
    // PRIVATE HELPERS
    // ========================================================================

    private String buildSummary(List<RedFlag> flags, String riskLevel) {
        if (flags.isEmpty()) {
            return "Keine verdächtigen Muster erkannt.";
        }

        String topIssues = flags.stream()
                .limit(3)
                .map(f -> "- " + f.getDescription())
                .collect(Collectors.joining("\n"));

        return "Gefundene Probleme:\n" + topIssues +
                "\nRisk Level: " + riskLevel;
    }

    private double calculateConfidence(List<RedFlag> flags) {
        if (flags == null || flags.isEmpty()) return 0.90;

        int totalImpact = flags.stream()
                .mapToInt(RedFlag::getScoreImpact)
                .sum();

        if (totalImpact >= 100) return 0.99;
        if (totalImpact >= 50) return 0.95;
        if (totalImpact >= 20) return 0.85;
        return 0.90;
    }
}