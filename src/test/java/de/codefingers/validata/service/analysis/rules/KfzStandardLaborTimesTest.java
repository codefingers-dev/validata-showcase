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

@DisplayName("KfzStandardLaborTimes - Layer 3: Arbeitszeit-Validierung")
class KfzStandardLaborTimesTest {

    private KfzStandardLaborTimes validator;

    @BeforeEach
    void setUp() {
        validator = new KfzStandardLaborTimes();
    }

    // ========================================
    // validate() - Frontend ✓/× Tests
    // ========================================



    @Test
    @DisplayName("Keine LineItems → allValid")
    void validate_noLineItems_returnsAllValid() {
        InvoiceData data = InvoiceData.builder().build();
        ValidationResult result = validator.validate(data);
        assertTrue(result.isAllValid());
    }

    @Test
    @DisplayName("Normale Arbeitszeit → allValid")
    void validate_normalLaborTime_returnsAllValid() {
        InvoiceData data = buildInvoiceWithLabor("Ölwechsel (mit Filter)", 1.0);
        ValidationResult result = validator.validate(data);
        assertTrue(result.isSumCalculationCorrect());
        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    @DisplayName("Überhöhte Arbeitszeit → sumCalculationCorrect false")
    void validate_excessiveLaborTime_flagsSumCalculation() {
        // Ölwechsel: Standard max 1.0h * 1.5 = 1.5h
        // 8h ist WEIT drüber!
        InvoiceData data = buildInvoiceWithLabor("Ölwechsel (mit Filter)", 8.0);
        ValidationResult result = validator.validate(data);
        assertFalse(result.isSumCalculationCorrect());
        assertFalse(result.getErrors().isEmpty());
    }

    @Test
    @DisplayName("Unbekannter Arbeitstyp → allValid (kein Match)")
    void validate_unknownWorkType_returnsAllValid() {
        InvoiceData data = buildInvoiceWithLabor("Spezial-Reparatur XYZ", 5.0);
        ValidationResult result = validator.validate(data);
        assertTrue(result.isSumCalculationCorrect());
    }


    @Test
    @DisplayName("InvoiceData ohne LineItems → allValid")
    void validate_nullLineItems_returnsAllValid() {
        InvoiceData data = InvoiceData.builder()
                .invoiceNumber("TEST-001")
                .grossAmount(BigDecimal.valueOf(100))
                .build();  // lineItems = null!
        ValidationResult result = validator.validate(data);
        assertTrue(result.isAllValid());
    }

    @Test
    @DisplayName("InvoiceData ohne LineItems → leere Flags")
    void detectRedFlags_nullLineItems_returnsEmpty() {
        InvoiceData data = InvoiceData.builder()
                .invoiceNumber("TEST-001")
                .grossAmount(BigDecimal.valueOf(100))
                .build();
        List<RedFlag> flags = validator.detectRedFlags(data);
        assertTrue(flags.isEmpty());
    }

    // ✅ UND Severity Fix:
    @Test
    @DisplayName("Severity MEDIUM bei 100-200% Überschreitung")
    void detectRedFlags_moderateExcess_severityMedium() {
        InvoiceData data = buildInvoiceWithLabor("Ölwechsel (mit Filter)", 2.5);
        List<RedFlag> flags = validator.detectRedFlags(data);

        assertFalse(flags.isEmpty());
        assertEquals(RedFlag.Severity.MEDIUM, flags.get(0).getSeverity());
    }



    // ========================================
    // detectRedFlags() - Scoring Tests
    // ========================================



    @Test
    @DisplayName("Keine LineItems → leere Flags")
    void detectRedFlags_noLineItems_returnsEmpty() {
        InvoiceData data = InvoiceData.builder().build();
        List<RedFlag> flags = validator.detectRedFlags(data);
        assertTrue(flags.isEmpty());
    }

    @Test
    @DisplayName("Normale Arbeitszeit → keine Flags")
    void detectRedFlags_normalTime_noFlags() {
        // Ölwechsel: Standard 0.5-1.0h, 1.0h ist OK
        InvoiceData data = buildInvoiceWithLabor("Ölwechsel (mit Filter)", 1.0);
        List<RedFlag> flags = validator.detectRedFlags(data);
        assertTrue(flags.isEmpty());
    }

    @Test
    @DisplayName("Grenzwert: Genau am Maximum * 1.5 → keine Flag")
    void detectRedFlags_atBoundary_noFlag() {
        // Ölwechsel max: 1.0h * 1.5 = 1.5h (genau am Limit)
        InvoiceData data = buildInvoiceWithLabor("Ölwechsel (mit Filter)", 1.5);
        List<RedFlag> flags = validator.detectRedFlags(data);
        assertTrue(flags.isEmpty());
    }

    @Test
    @DisplayName("Überhöhte Arbeitszeit → EXCESSIVE_LABOR_TIME Flag")
    void detectRedFlags_excessiveTime_returnsFlag() {
        // Ölwechsel: max 1.0h, charged 8.0h = 700% over!
        InvoiceData data = buildInvoiceWithLabor("Ölwechsel (mit Filter)", 8.0);
        List<RedFlag> flags = validator.detectRedFlags(data);

        assertFalse(flags.isEmpty());
        assertEquals(1, flags.size());

        RedFlag flag = flags.get(0);
        assertEquals("EXCESSIVE_LABOR_TIME", flag.getCode());
        assertEquals(RedFlag.Category.LABOR, flag.getCategory());
        assertEquals(RedFlag.Source.RULE, flag.getSource());
        assertTrue(flag.getScoreImpact() > 0);
    }


    @Test
    @DisplayName("Severity HIGH bei 200%+ Überschreitung")
    void detectRedFlags_extremeExcess_severityHigh() {
        // Ölwechsel max 1.0h, charged 10.0h = 900% over
        InvoiceData data = buildInvoiceWithLabor("Ölwechsel (mit Filter)", 10.0);
        List<RedFlag> flags = validator.detectRedFlags(data);

        assertFalse(flags.isEmpty());
        assertEquals(RedFlag.Severity.HIGH, flags.get(0).getSeverity());
    }

    @Test
    @DisplayName("PARTS LineItem → wird ignoriert (nur LABOR)")
    void detectRedFlags_partsItem_ignored() {
        InvoiceData data = buildInvoiceWithParts("Bremsbeläge vorne", 90.0);
        List<RedFlag> flags = validator.detectRedFlags(data);
        assertTrue(flags.isEmpty());
    }

    // ========================================
    // getRuleCount() + getVersion()
    // ========================================

    @Test
    @DisplayName("RuleCount gibt Anzahl der Labor Tasks zurück")
    void getRuleCount_returnsPositiveNumber() {
        int count = validator.getRuleCount();
        assertTrue(count > 90, "Should have 90+ labor tasks, got: " + count);
    }

    @Test
    @DisplayName("Version ist nicht leer")
    void getVersion_returnsVersion() {
        String version = validator.getVersion();
        assertNotNull(version);
        assertFalse(version.isBlank());
    }

    // ========================================
    // HELPER METHODS
    // ========================================

    private InvoiceData buildInvoiceWithLabor(String description, double hours) {
        InvoiceData.LineItem item = InvoiceData.LineItem.builder()
                .description(description)
                .category(InvoiceData.LineItem.ItemCategory.LABOR)
                .quantity(BigDecimal.valueOf(hours))
                .unitPrice(BigDecimal.valueOf(80.0))  // 80€/h
                .build();

        return InvoiceData.builder()
                .invoiceNumber("TEST-001")
                .grossAmount(BigDecimal.valueOf(hours * 80.0))
                .lineItems(List.of(item))
                .build();
    }

    private InvoiceData buildInvoiceWithParts(String description, double price) {
        InvoiceData.LineItem item = InvoiceData.LineItem.builder()
                .description(description)
                .category(InvoiceData.LineItem.ItemCategory.PARTS)
                .quantity(BigDecimal.ONE)
                .unitPrice(BigDecimal.valueOf(price))
                .build();

        return InvoiceData.builder()
                .invoiceNumber("TEST-002")
                .grossAmount(BigDecimal.valueOf(price))
                .lineItems(List.of(item))
                .build();
    }
}
