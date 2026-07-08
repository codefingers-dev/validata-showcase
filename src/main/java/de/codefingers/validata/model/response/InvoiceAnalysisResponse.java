package de.codefingers.validata.model.response;

import de.codefingers.validata.model.domain.FraudAnalysisResult;
import de.codefingers.validata.model.domain.RedFlag;
import de.codefingers.validata.model.domain.ValidationResult;
import de.codefingers.validata.model.dto.InvoiceData;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * API Response für Fraud Analysis
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(
        name = "InvoiceAnalysisResponse",
        description = "Vollständige Fraud Analysis Result"
)
public class InvoiceAnalysisResponse {

    // ===== REQUEST INFO =====

    @Schema(
            description = "Eindeutige Analyse-ID",
            example = "550e8400-e29b-41d4-a716-446655440000"
    )
    private String analysisId;
    private String invoiceNumber;
    private String workshopName;

    // ===== HAUPTERGEBNIS =====

    @Schema(
            description = "Risk Score von 0-100",
            minimum = "0",
            maximum = "100",
            example = "45"
    )
    private int riskScore;

    @Schema(
            description = "Risk Level basierend auf Score",
            example = "MEDIUM",
            allowableValues = {"LOW", "MEDIUM", "HIGH", "UNKNOWN"}
    )
    private String riskLevel;

    @Schema(
            description = "Handlungsempfehlung für Sachbearbeiter",
            example = "REVIEW",
            allowableValues = {"APPROVE", "REJECT", "REVIEW", "ESCALATE"}
    )
    private String recommendation;


    @Schema(
            description = "Status der Analyse",
            example = "SUCCESS",
            allowableValues = {"SUCCESS", "ERROR", "FAILED"}
    )
    private String status;

    // ===== ERROR HANDLING =====

    @Schema(
            description = "Fehlernachricht (bei ERROR Status)"

    )
    private String errorMessage;        // Falls error

    // ===== RED FLAGS =====
    private List<RedFlagDto> redFlags;
    private int redFlagCount;

    // ===== VALIDIERUNG =====
    private ValidationDto validation;

    // ===== EXTRAHIERTE DATEN =====
    private ExtractedDataDto extractedData;

    // ===== OPTIONAL DETAILS =====

    private Long processingTimeMs;      // How long did analysis take?

    // ===== AI-ZUSAMMENFASSUNG =====
    private String summary;
    private Double confidence;

    // ===== METADATEN =====
    private MetadataDto metadata;

    // ===== NESTED DTOs =====

    @Data
    @Builder
    public static class RedFlagDto {
        private String code;
        private String category;
        private String severity;
        private String description;
        private String evidence;
        private int scoreImpact;
        private String source;
    }

    @Data
    @Builder
    public static class ValidationDto {
        private boolean allValid;
        private boolean taxNumberValid;
        private boolean vatIdValid;
        private boolean vatCalculationCorrect;
        private boolean sumCalculationCorrect;
        private boolean mandatoryFieldsPresent;
        private boolean licensePlateValid;
        private List<String> errors;
    }

    @Data
    @Builder
    public static class ExtractedDataDto {
        private String workshopName;
        private String invoiceNumber;
        private String invoiceDate;
        private String licensePlate;
        private String netAmount;
        private String vatAmount;
        private String grossAmount;
        private int lineItemCount;
    }

    @Data
    @Builder
    public static class MetadataDto {
        private String analysisId;
        private Instant analyzedAt;
        private long processingTimeMs;
        private String modelUsed;
        private String promptVersion;
        private String analysisMode;
        private String apiVersion;
    }

    // ===== FACTORY METHOD =====

    public static InvoiceAnalysisResponse from(FraudAnalysisResult result, String analysisId) {
        return InvoiceAnalysisResponse.builder()
                // ===== REQUEST INFO =====
                .analysisId(analysisId)
                .invoiceNumber(result.getExtractedData() != null ?
                        result.getExtractedData().getInvoiceNumber() : "unknown")
                .workshopName(result.getExtractedData() != null ?
                        result.getExtractedData().getWorkshopName() : "unknown")

                // ===== HAUPTERGEBNIS =====
                .riskScore(result.getRiskScore())
                .riskLevel(result.getRiskLevel())  // ← String, nicht .name()!
                .recommendation(result.getRecommendation())  // ← String, nicht .name()!
                .status("SUCCESS")  // ← ADD: Status hinzufügen

                // ===== RED FLAGS =====
                .redFlags(mapRedFlags(result.getRedFlags()))
                .redFlagCount(result.getRedFlags().size())

                .validation(mapValidation(result.getFormalValidation()))

                // ===== EXTRAHIERTE DATEN =====
                .extractedData(mapExtractedData(result.getExtractedData()))

                // ===== PROCESSING TIME =====
                .processingTimeMs(result.getProcessingTimeMs())

                // ===== AI-ZUSAMMENFASSUNG =====
                .summary(result.getSummary())
                .confidence(result.getConfidence())

                // ===== METADATEN =====
                .metadata(MetadataDto.builder()
                        .analysisId(analysisId)
                        .analyzedAt(result.getAnalyzedAt())
                        .processingTimeMs(result.getProcessingTimeMs())
                        .modelUsed(result.getEngineUsed())
                        .promptVersion(result.getPromptVersion())
                        .analysisMode(result.getAnalysisMode().name())
                        .apiVersion("1.0.0")
                        .build())

                .build();
    }

    private static List<RedFlagDto> mapRedFlags(List<RedFlag> flags) {
        if (flags == null) return List.of();
        return flags.stream()
                .map(rf -> RedFlagDto.builder()
                        .code(rf.getCode())
                        .category(rf.getCategory().name())
                        .severity(rf.getSeverity().name())
                        .description(rf.getDescription())
                        .evidence(rf.getEvidence())
                        .scoreImpact(rf.getScoreImpact())
                        .source(rf.getSource().name())
                        .build())
                .toList();
    }


    private static ExtractedDataDto mapExtractedData(InvoiceData d) {
        if (d == null) return null;
        return ExtractedDataDto.builder()
                .workshopName(d.getWorkshopName())
                .invoiceNumber(d.getInvoiceNumber())
                .invoiceDate(d.getInvoiceDate() != null ? d.getInvoiceDate().toString() : null)
                .licensePlate(d.getLicensePlate())
                .netAmount(d.getNetAmount() != null ? d.getNetAmount().toString() : null)
                .vatAmount(d.getVatAmount() != null ? d.getVatAmount().toString() : null)
                .grossAmount(d.getGrossAmount() != null ? d.getGrossAmount().toString() : null)
                .lineItemCount(d.getLineItems() != null ? d.getLineItems().size() : 0)
                .build();
    }

    private static ValidationDto mapValidation(ValidationResult validation) {
        if (validation == null) return null;

        return ValidationDto.builder()
                .allValid(validation.isAllValid())
                .taxNumberValid(validation.isTaxNumberValid())
                .vatIdValid(validation.isVatIdValid())
                .vatCalculationCorrect(validation.isVatCalculationCorrect())
                .sumCalculationCorrect(validation.isSumCalculationCorrect())
                .mandatoryFieldsPresent(validation.isMandatoryFieldsPresent())
                .licensePlateValid(validation.isLicensePlateValid())
                .errors(validation.getErrors())
                .build();
    }
}
