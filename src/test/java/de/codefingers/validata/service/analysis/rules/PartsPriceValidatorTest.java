package de.codefingers.validata.service.analysis.rules;


import de.codefingers.validata.model.domain.RedFlag;
import de.codefingers.validata.model.domain.ValidationResult;
import de.codefingers.validata.model.dto.InvoiceData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PartsPriceValidator - Layer 4: Ersatzteil-Preis-Validierung")
class PartsPriceValidatorTest {

    private PartsPriceValidator validator;

    @BeforeEach
    void setUp() {
        validator = new PartsPriceValidator();
    }

    // ========================================================================
    // validate() - Frontend ✓/× Tests
    // ========================================================================

    @Test
    @DisplayName("Keine LineItems → allValid")
    void validate_nullLineItems_returnsAllValid() {
        InvoiceData data = InvoiceData.builder()
                .invoiceNumber("TEST-001")
                .grossAmount(BigDecimal.valueOf(100))
                .build();
        ValidationResult result = validator.validate(data);
        assertTrue(result.isAllValid());
    }

    @Test
    @DisplayName("Normaler Teile-Preis → allValid")
    void validate_normalPrice_returnsAllValid() {
        // Bremsbeläge vorne: max 90€, 75€ ist OK
        InvoiceData data = buildInvoiceWithPart("Bremsbeläge vorne", 75.0);
        ValidationResult result = validator.validate(data);
        assertTrue(result.isSumCalculationCorrect());
        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    @DisplayName("Überteuerter Teil → sumCalculationCorrect false")
    void validate_overpricedPart_flagsSumCalculation() {
        // Bremsbeläge vorne: max 90€ * 1.5 = 135€, 450€ ist WEIT drüber
        InvoiceData data = buildInvoiceWithPart("Bremsbeläge vorne", 450.0);
        ValidationResult result = validator.validate(data);
        assertFalse(result.isSumCalculationCorrect());
        assertFalse(result.getErrors().isEmpty());
    }

    @Test
    @DisplayName("Unbekannter Teil → allValid (kein Match)")
    void validate_unknownPart_returnsAllValid() {
        InvoiceData data = buildInvoiceWithPart("Spezial-Bauteil XYZ", 500.0);
        ValidationResult result = validator.validate(data);
        assertTrue(result.isSumCalculationCorrect());
    }

    // ========================================================================
    // detectRedFlags() - Scoring Tests
    // ========================================================================

    @Test
    @DisplayName("Keine LineItems → leere Flags")
    void detectRedFlags_nullLineItems_returnsEmpty() {
        InvoiceData data = InvoiceData.builder()
                .invoiceNumber("TEST-001")
                .grossAmount(BigDecimal.valueOf(100))
                .build();
        List<RedFlag> flags = validator.detectRedFlags(data);
        assertTrue(flags.isEmpty());
    }

    @Test
    @DisplayName("Normaler Preis → keine Flags")
    void detectRedFlags_normalPrice_noFlags() {
        InvoiceData data = buildInvoiceWithPart("Bremsbeläge vorne", 75.0);
        List<RedFlag> flags = validator.detectRedFlags(data);
        assertTrue(flags.isEmpty());
    }

    @Test
    @DisplayName("Grenzwert: Genau am max * 1.5 → keine Flag")
    void detectRedFlags_atBoundary_noFlag() {
        // Bremsbeläge vorne max: 90€ * 1.5 = 135€ (genau am Limit)
        InvoiceData data = buildInvoiceWithPart("Bremsbeläge vorne", 135.0);
        List<RedFlag> flags = validator.detectRedFlags(data);
        assertTrue(flags.isEmpty());
    }

    @Test
    @DisplayName("Überteuerter Teil → OVERPRICED_PART Flag")
    void detectRedFlags_overpriced_returnsFlag() {
        // Bremsbeläge vorne: max 90€, charged 450€ = 400% over
        InvoiceData data = buildInvoiceWithPart("Bremsbeläge vorne", 450.0);
        List<RedFlag> flags = validator.detectRedFlags(data);

        assertFalse(flags.isEmpty());
        assertEquals(1, flags.size());

        RedFlag flag = flags.get(0);
        assertEquals("OVERPRICED_PART", flag.getCode());
        assertEquals(RedFlag.Category.PARTS, flag.getCategory());
        assertEquals(RedFlag.Source.RULE, flag.getSource());
        assertTrue(flag.getScoreImpact() > 0);
    }

    @Test
    @DisplayName("Severity LOW bei 50-100% Überschreitung")
    void detectRedFlags_moderateOverprice_severityLow() {
        // Bremsbeläge vorne max 90€ * 1.5 = 135€, charged 155€ = ~72% over
        InvoiceData data = buildInvoiceWithPart("Bremsbeläge vorne", 155.0);
        List<RedFlag> flags = validator.detectRedFlags(data);

        assertFalse(flags.isEmpty());
        assertEquals(RedFlag.Severity.LOW, flags.get(0).getSeverity());
    }

    @Test
    @DisplayName("Severity HIGH bei 200%+ Überschreitung")
    void detectRedFlags_extremeOverprice_severityHigh() {
        // Bremsbeläge vorne max 90€, charged 450€ = 400% over
        InvoiceData data = buildInvoiceWithPart("Bremsbeläge vorne", 450.0);
        List<RedFlag> flags = validator.detectRedFlags(data);

        assertFalse(flags.isEmpty());
        assertEquals(RedFlag.Severity.HIGH, flags.get(0).getSeverity());
    }

    @Test
    @DisplayName("LABOR LineItem → wird ignoriert (nur PARTS)")
    void detectRedFlags_laborItem_ignored() {
        InvoiceData data = buildInvoiceWithLabor("Ölwechsel (mit Filter)", 1.0);
        List<RedFlag> flags = validator.detectRedFlags(data);
        assertTrue(flags.isEmpty());
    }

    @Test
    @DisplayName("Null unitPrice → wird übersprungen (kein Crash)")
    void detectRedFlags_nullUnitPrice_skipped() {
        InvoiceData.LineItem item = InvoiceData.LineItem.builder()
                .description("Bremsbeläge vorne")
                .category(InvoiceData.LineItem.ItemCategory.PARTS)
                .quantity(BigDecimal.ONE)
                .unitPrice(null)  // null!
                .build();
        InvoiceData data = InvoiceData.builder()
                .invoiceNumber("TEST-002")
                .grossAmount(BigDecimal.valueOf(100))
                .lineItems(List.of(item))
                .build();

        List<RedFlag> flags = validator.detectRedFlags(data);
        assertTrue(flags.isEmpty());
    }

    // ========================================================================
    // extractPriceFromDescription() - Regex Tests
    // ========================================================================

    @Test
    @DisplayName("Preis-Extraktion: €-Symbol vorne")
    void extractPrice_euroSymbolFront_extractsCorrectly() {
        assertEquals(new BigDecimal("100"), validator.extractPriceFromDescription("€100"));
    }

    @Test
    @DisplayName("Preis-Extraktion: €-Symbol hinten mit Komma")
    void extractPrice_euroSymbolBackComma_extractsCorrectly() {
        assertEquals(new BigDecimal("100.50"), validator.extractPriceFromDescription("100,50€"));
    }

    @Test
    @DisplayName("Preis-Extraktion: kein Preis → null")
    void extractPrice_noPrice_returnsNull() {
        assertNull(validator.extractPriceFromDescription("Bremsbeläge ohne Preis"));
    }

    @Test
    @DisplayName("Preis-Extraktion: null Input → null")
    void extractPrice_nullInput_returnsNull() {
        assertNull(validator.extractPriceFromDescription(null));
    }

    // ========================================================================
    // getRuleCount() + getVersion()
    // ========================================================================

    @Test
    @DisplayName("RuleCount gibt Anzahl der Parts zurück")
    void getRuleCount_returnsPositiveNumber() {
        int count = validator.getRuleCount();
        assertTrue(count > 0, "Should have parts, got: " + count);
    }

    @Test
    @DisplayName("Version ist nicht leer")
    void getVersion_returnsVersion() {
        String version = validator.getVersion();
        assertNotNull(version);
        assertFalse(version.isBlank());
    }

    // ========================================================================
    // HELPER METHODS
    // ========================================================================

    private InvoiceData buildInvoiceWithPart(String description, double price) {
        InvoiceData.LineItem item = InvoiceData.LineItem.builder()
                .description(description)
                .category(InvoiceData.LineItem.ItemCategory.PARTS)
                .quantity(BigDecimal.ONE)
                .unitPrice(BigDecimal.valueOf(price))
                .build();

        return InvoiceData.builder()
                .invoiceNumber("TEST-PART")
                .grossAmount(BigDecimal.valueOf(price))
                .lineItems(List.of(item))
                .build();
    }

    private InvoiceData buildInvoiceWithLabor(String description, double hours) {
        InvoiceData.LineItem item = InvoiceData.LineItem.builder()
                .description(description)
                .category(InvoiceData.LineItem.ItemCategory.LABOR)
                .quantity(BigDecimal.valueOf(hours))
                .unitPrice(BigDecimal.valueOf(80.0))
                .build();

        return InvoiceData.builder()
                .invoiceNumber("TEST-LABOR")
                .grossAmount(BigDecimal.valueOf(hours * 80.0))
                .lineItems(List.of(item))
                .build();
    }
}