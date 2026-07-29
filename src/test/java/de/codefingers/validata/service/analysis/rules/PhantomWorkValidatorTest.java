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

@DisplayName("PhantomWorkValidator - Layer 4: Phantom-Arbeiten-Erkennung")
class PhantomWorkValidatorTest {

    private PhantomWorkValidator validator;

    @BeforeEach
    void setUp() {
        validator = new PhantomWorkValidator();
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
    @DisplayName("Normale einzelne Arbeit → allValid")
    void validate_singleNormalWork_returnsAllValid() {
        InvoiceData data = buildInvoiceWithLabor("ölwechsel", 1.0);
        ValidationResult result = validator.validate(data);
        assertTrue(result.isSumCalculationCorrect());
    }

    @Test
    @DisplayName("Doppelte Arbeit → sumCalculationCorrect false")
    void validate_duplicateWork_flagsSumCalculation() {
        // Zwei identische Positionen → Duplikat
        InvoiceData.LineItem item1 = buildLaborItem("ölwechsel", 1.0);
        InvoiceData.LineItem item2 = buildLaborItem("ölwechsel", 1.0);
        InvoiceData data = InvoiceData.builder()
                .invoiceNumber("TEST-DUP")
                .grossAmount(BigDecimal.valueOf(200))
                .lineItems(List.of(item1, item2))
                .build();

        ValidationResult result = validator.validate(data);
        assertFalse(result.isSumCalculationCorrect());
        assertFalse(result.getErrors().isEmpty());
    }

    // ========================================================================
    // detectRedFlags() - CHECK 1: Incompatible Pairs
    // ========================================================================

    @Test
    @DisplayName("Inkompatible Arbeiten → INCOMPATIBLE_WORK Flag")
    void detectRedFlags_incompatiblePair_returnsFlag() {
        // "ölwechsel" + "motorüberholung" = unmöglich zusammen
        InvoiceData.LineItem item1 = buildLaborItem("ölwechsel durchgeführt", 1.0);
        InvoiceData.LineItem item2 = buildLaborItem("motorüberholung komplett", 8.0);
        InvoiceData data = InvoiceData.builder()
                .invoiceNumber("TEST-INCOMPAT")
                .grossAmount(BigDecimal.valueOf(1000))
                .lineItems(List.of(item1, item2))
                .build();

        List<RedFlag> flags = validator.detectRedFlags(data);

        assertTrue(flags.stream()
                .anyMatch(f -> f.getCode().equals("INCOMPATIBLE_WORK")));
    }

    // ========================================================================
    // detectRedFlags() - CHECK 2: Unrealistic Times
    // ========================================================================

    @Test
    @DisplayName("Unrealistisch lange Zeit → UNREALISTIC_TIME_LONG Flag")
    void detectRedFlags_unrealisticLong_returnsFlag() {
        // "ölwechsel" max 1.5h * 1.5 = 2.25h, charged 8h → LONG
        InvoiceData data = buildInvoiceWithLabor("ölwechsel", 8.0);
        List<RedFlag> flags = validator.detectRedFlags(data);

        assertTrue(flags.stream()
                .anyMatch(f -> f.getCode().equals("UNREALISTIC_TIME_LONG")));
    }

    @Test
    @DisplayName("Unrealistisch kurze Zeit → UNREALISTIC_TIME_SHORT Flag")
    void detectRedFlags_unrealisticShort_returnsFlag() {
        // "zahnriemen" min 2.0h * 0.5 = 1.0h, charged 0.3h → SHORT
        InvoiceData data = buildInvoiceWithLabor("zahnriemen", 0.3);
        List<RedFlag> flags = validator.detectRedFlags(data);

        assertTrue(flags.stream()
                .anyMatch(f -> f.getCode().equals("UNREALISTIC_TIME_SHORT")));
    }

    @Test
    @DisplayName("Realistische Zeit → keine Zeit-Flag")
    void detectRedFlags_realisticTime_noTimeFlag() {
        // "ölwechsel" range 0.5-1.5h, charged 1.0h → OK
        InvoiceData data = buildInvoiceWithLabor("ölwechsel", 1.0);
        List<RedFlag> flags = validator.detectRedFlags(data);

        assertTrue(flags.stream()
                .noneMatch(f -> f.getCode().startsWith("UNREALISTIC_TIME")));
    }

    // ========================================================================
    // detectRedFlags() - CHECK 3: Vehicle Incompatibility
    // ========================================================================

    @Test
    @DisplayName("Fahrzeug-inkompatible Arbeit → VEHICLE_INCOMPATIBLE_WORK Flag")
    void detectRedFlags_vehicleIncompatible_returnsFlag() {
        // Elektro-Fahrzeug + "kupplung" = unmöglich
        InvoiceData.LineItem item = buildLaborItem("kupplung gewechselt", 4.0);
        InvoiceData data = InvoiceData.builder()
                .invoiceNumber("TEST-VEHICLE")
                .grossAmount(BigDecimal.valueOf(500))
                .vehicleInfo("elektro fahrzeug tesla")
                .lineItems(List.of(item))
                .build();

        List<RedFlag> flags = validator.detectRedFlags(data);

        assertTrue(flags.stream()
                .anyMatch(f -> f.getCode().equals("VEHICLE_INCOMPATIBLE_WORK")));
    }

    @Test
    @DisplayName("Kein vehicleInfo → kein Vehicle-Check (kein Crash)")
    void detectRedFlags_noVehicleInfo_noVehicleFlag() {
        InvoiceData.LineItem item = buildLaborItem("kupplung gewechselt", 4.0);
        InvoiceData data = InvoiceData.builder()
                .invoiceNumber("TEST-NOVEHICLE")
                .grossAmount(BigDecimal.valueOf(500))
                .vehicleInfo(null)  // null!
                .lineItems(List.of(item))
                .build();

        List<RedFlag> flags = validator.detectRedFlags(data);

        assertTrue(flags.stream()
                .noneMatch(f -> f.getCode().equals("VEHICLE_INCOMPATIBLE_WORK")));
    }

    // ========================================================================
    // detectRedFlags() - CHECK 4: Duplicate Work
    // ========================================================================

    @Test
    @DisplayName("Doppelte Arbeit → DUPLICATE_WORK_ITEM Flag")
    void detectRedFlags_duplicateWork_returnsFlag() {
        InvoiceData.LineItem item1 = buildLaborItem("ölwechsel", 1.0);
        InvoiceData.LineItem item2 = buildLaborItem("ölwechsel", 1.0);
        InvoiceData data = InvoiceData.builder()
                .invoiceNumber("TEST-DUP2")
                .grossAmount(BigDecimal.valueOf(200))
                .lineItems(List.of(item1, item2))
                .build();

        List<RedFlag> flags = validator.detectRedFlags(data);

        assertTrue(flags.stream()
                .anyMatch(f -> f.getCode().equals("DUPLICATE_WORK_ITEM")));
    }

    // ========================================================================
    // Edge Cases
    // ========================================================================

    @Test
    @DisplayName("Keine LineItems → leere Flags")
    void detectRedFlags_nullLineItems_returnsEmpty() {
        InvoiceData data = InvoiceData.builder()
                .invoiceNumber("TEST-EMPTY")
                .grossAmount(BigDecimal.valueOf(100))
                .build();
        List<RedFlag> flags = validator.detectRedFlags(data);
        assertTrue(flags.isEmpty());
    }

    @Test
    @DisplayName("Nur PARTS Items → keine Phantom-Flags (nur LABOR geprüft)")
    void detectRedFlags_onlyParts_noFlags() {
        InvoiceData.LineItem item = InvoiceData.LineItem.builder()
                .description("Bremsbeläge vorne")
                .category(InvoiceData.LineItem.ItemCategory.PARTS)
                .quantity(BigDecimal.ONE)
                .unitPrice(BigDecimal.valueOf(75.0))
                .build();
        InvoiceData data = InvoiceData.builder()
                .invoiceNumber("TEST-PARTS")
                .grossAmount(BigDecimal.valueOf(75))
                .lineItems(List.of(item))
                .build();

        List<RedFlag> flags = validator.detectRedFlags(data);
        assertTrue(flags.isEmpty());
    }

    @Test
    @DisplayName("Unbekannte Arbeit → keine Flags (kein Match)")
    void detectRedFlags_unknownWork_noFlags() {
        InvoiceData data = buildInvoiceWithLabor("spezialarbeit xyz unbekannt", 5.0);
        List<RedFlag> flags = validator.detectRedFlags(data);
        assertTrue(flags.isEmpty());
    }

    // ========================================================================
    // getRuleCount() + getVersion()
    // ========================================================================

    @Test
    @DisplayName("RuleCount summiert alle drei Maps")
    void getRuleCount_returnsPositiveNumber() {
        int count = validator.getRuleCount();
        assertTrue(count > 0, "Should have rules, got: " + count);
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

    private InvoiceData buildInvoiceWithLabor(String description, double hours) {
        InvoiceData data = InvoiceData.builder()
                .invoiceNumber("TEST-PHANTOM")
                .grossAmount(BigDecimal.valueOf(hours * 80.0))
                .lineItems(List.of(buildLaborItem(description, hours)))
                .build();
        return data;
    }

    private InvoiceData.LineItem buildLaborItem(String description, double hours) {
        return InvoiceData.LineItem.builder()
                .description(description)
                .category(InvoiceData.LineItem.ItemCategory.LABOR)
                .quantity(BigDecimal.valueOf(hours))
                .unitPrice(BigDecimal.valueOf(80.0))
                .build();
    }
}