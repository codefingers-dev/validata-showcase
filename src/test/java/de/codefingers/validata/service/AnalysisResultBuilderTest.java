package de.codefingers.validata.service;


import de.codefingers.validata.model.domain.FraudAnalysisResult;
import de.codefingers.validata.model.domain.RedFlag;
import de.codefingers.validata.model.domain.ValidationResult;
import de.codefingers.validata.model.dto.InvoiceData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AnalysisResultBuilder - Result Erstellung")
class AnalysisResultBuilderTest {

    private AnalysisResultBuilder builder;

    @BeforeEach
    void setUp() {
        builder = new AnalysisResultBuilder();
    }

    @Test
    @DisplayName("buildSuccess → Korrektes Result mit Score + Flags")
    void buildSuccess_returnsCompleteResult() {
        // Arrange
        InvoiceData data = InvoiceData.builder()
                .invoiceNumber("RE-2025-001")
                .grossAmount(BigDecimal.valueOf(500))
                .build();

        List<RedFlag> flags = List.of(
                RedFlag.builder()
                        .code("EXCESSIVE_LABOR_TIME")
                        .category(RedFlag.Category.LABOR)
                        .severity(RedFlag.Severity.HIGH)
                        .scoreImpact(25)
                        .build()
        );

        ValidationResult validation = ValidationResult.allValid();

        // Act
        FraudAnalysisResult result = builder.buildSuccess(
                data, flags, 50, "YELLOW", "MANUAL_REVIEW", validation);

        // Assert
        assertEquals(50, result.getRiskScore());
        assertEquals("YELLOW", result.getRiskLevel());
        assertEquals("MANUAL_REVIEW", result.getRecommendation());
        assertEquals(1, result.getRedFlags().size());
        assertNotNull(result.getExtractedData());
        assertEquals("RE-2025-001", result.getExtractedData().getInvoiceNumber());
        assertNotNull(result.getAnalyzedAt());
    }

    @Test
    @DisplayName("buildInvalid → Score 0, Level INVALID, REJECT")
    void buildInvalid_returnsRejectResult() {
        // Act
        FraudAnalysisResult result = builder.buildInvalid(
                "Rechnungsnummer fehlt", "Validation", 150);

        // Assert
        assertEquals(0, result.getRiskScore());
        assertEquals("INVALID", result.getRiskLevel());
        assertEquals("REJECT", result.getRecommendation());
        assertEquals(1, result.getRedFlags().size());
        assertEquals("INVALID_INVOICE", result.getRedFlags().get(0).getCode());
        assertEquals(150, result.getProcessingTimeMs());
        assertNull(result.getExtractedData());
    }

    @Test
    @DisplayName("buildFallback → Score 25, MANUAL_REVIEW")
    void buildFallback_returnsManualReviewResult() {
        // Act
        FraudAnalysisResult result = builder.buildFallback(
                "Textract fehlgeschlagen", 200);

        // Assert
        assertEquals(25, result.getRiskScore());
        assertEquals("MANUAL_REVIEW", result.getRiskLevel());
        assertEquals("MANUAL_REVIEW_REQUIRED", result.getRecommendation());
        assertEquals(200, result.getProcessingTimeMs());
        assertTrue(result.getSummary().contains("manuell"));
        assertNull(result.getExtractedData());
    }

    @Test
    @DisplayName("buildDuplicate → Score 100, CRITICAL, REJECT")
    void buildDuplicate_returnsCriticalResult() {
        // Arrange
        InvoiceData data = InvoiceData.builder()
                .invoiceNumber("RE-2025-DUPLICATE")
                .grossAmount(BigDecimal.valueOf(1000))
                .build();

        List<RedFlag> flags = List.of(
                RedFlag.duplicateHigh("EXACT_DUPLICATE",
                        "Rechnung bereits eingereicht",
                        "SHA256 Match", 50)
        );

        // Act
        FraudAnalysisResult result = builder.buildDuplicate(
                data, flags, "EXACT_DUPLICATE", 1.0, 50);

        // Assert
        assertEquals(100, result.getRiskScore());
        assertEquals("CRITICAL", result.getRiskLevel());
        assertEquals("REJECT", result.getRecommendation());
        assertNotNull(result.getExtractedData());
    }
}