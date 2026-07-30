package de.codefingers.validata.service.analysis.rules;

import de.codefingers.validata.service.analysis.rules.DuplicationCheckResult;
import de.codefingers.validata.model.dto.InvoiceData;
import de.codefingers.validata.service.provider.InvoiceDataProvider;
import de.codefingers.validata.service.analysis.rules.InvoiceHasher;
import de.codefingers.validata.service.analysis.rules.InvoiceSimilarityCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit-Tests für InvoiceDuplicationDetector (Layer 6).
 *
 * Der Detector vergleicht eine Rechnung gegen alle vorhandenen Rechnungen,
 * die er über den InvoiceDataProvider bezieht. Der Provider wird gemockt,
 * um für jeden der 4 Duplikat-Pattern kontrollierte Datensätze vorzugeben.
 *
 * Die Hilfsdienste (Hasher, SimilarityCalculator) sind reine, deterministische
 * Logik und werden real verwendet — sie brauchen kein Mocking.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("InvoiceDuplicationDetector - Layer 6: Duplikat-Erkennung")
class InvoiceDuplicationDetectorTest {

    private InvoiceDataProvider mockProvider;
    private InvoiceDuplicationDetector detector;

    @BeforeEach
    void setUp() {
        // Provider mocken (Datenquelle), echte Hilfsdienste verwenden
        mockProvider = mock(InvoiceDataProvider.class);
        detector = new InvoiceDuplicationDetector(
                mockProvider,
                new InvoiceHasher(),
                new InvoiceSimilarityCalculator()
        );
    }

    // ========================================================================
    // Input-Validierung / Edge Cases
    // ========================================================================

    @Test
    @DisplayName("Null-Rechnung → noMatch (kein Crash)")
    void detectDuplication_nullInvoice_returnsNoMatch() {
        DuplicationCheckResult result = detector.detectDuplication(null);
        assertFalse(result.isDuplicate());
        assertEquals("NONE", result.getType());
    }

    @Test
    @DisplayName("Rechnung mit fehlenden Pflichtfeldern → noMatch")
    void detectDuplication_missingFields_returnsNoMatch() {
        InvoiceData incomplete = InvoiceData.builder()
                .invoiceNumber("INV-X")
                // licensePlate, grossAmount, invoiceDate fehlen
                .build();

        DuplicationCheckResult result = detector.detectDuplication(incomplete);
        assertFalse(result.isDuplicate());
        assertEquals("NONE", result.getType());
    }

    @Test
    @DisplayName("Keine anderen Rechnungen → noMatch")
    void detectDuplication_noOtherInvoices_returnsNoMatch() {
        InvoiceData current = validInvoice("INV-001", "DE1234",
                "1000.00", "2024-01-15");
        when(mockProvider.getAllInvoices()).thenReturn(new ArrayList<>());

        DuplicationCheckResult result = detector.detectDuplication(current);
        assertFalse(result.isDuplicate());
    }

    // ========================================================================
    // Pattern 1: EXACT_DUPLICATE
    // ========================================================================

    @Test
    @DisplayName("Identische Rechnung (anderer Nummer) → EXACT_DUPLICATE, +30")
    void detectDuplication_exactMatch_returnsExactDuplicate() {
        InvoiceData current = validInvoice("INV-NEW", "DE1234",
                "1011.50", "2024-01-15");
        current.setVehicleInfo("Mercedes E-Klasse W204 (2012)");
        current.setWorkshopName("Frenzel KFZ");

        // Existierende Rechnung mit identischen Daten, andere Nummer
        InvoiceData existing = validInvoice("INV-OLD", "DE1234",
                "1011.50", "2024-01-15");
        existing.setVehicleInfo("Mercedes E-Klasse W204 (2012)");
        existing.setWorkshopName("Frenzel KFZ");

        when(mockProvider.getAllInvoices()).thenReturn(List.of(existing));

        DuplicationCheckResult result = detector.detectDuplication(current);

        assertTrue(result.isDuplicate());
        assertEquals("EXACT_DUPLICATE", result.getType());
        assertEquals(30, result.getPoints());
        assertFalse(result.getRedFlags().isEmpty());
    }

    @Test
    @DisplayName("Nicht gegen sich selbst (gleiche Nummer) → kein Exact-Match")
    void detectDuplication_sameInvoiceNumber_skipsSelf() {
        InvoiceData current = validInvoice("INV-SAME", "DE1234",
                "1011.50", "2024-01-15");

        // Gleiche Nummer → wird als "sich selbst" übersprungen
        InvoiceData self = validInvoice("INV-SAME", "DE1234",
                "1011.50", "2024-01-15");

        when(mockProvider.getAllInvoices()).thenReturn(List.of(self));

        DuplicationCheckResult result = detector.detectDuplication(current);
        assertFalse(result.isDuplicate());
    }

    // ========================================================================
    // Pattern 4: AMOUNT_CLONE
    // ========================================================================

    @Test
    @DisplayName("Gleicher Betrag auf 3 verschiedenen Fahrzeugen → AMOUNT_CLONE, +10")
    void detectDuplication_amountClone_returnsAmountClone() {
        InvoiceData current = validInvoice("INV-A", "DE1111",
                "1500.00", "2024-02-05");

        // Zwei weitere Fahrzeuge mit identischem Betrag
        InvoiceData clone1 = validInvoice("INV-B", "DE2222",
                "1500.00", "2024-02-06");
        InvoiceData clone2 = validInvoice("INV-C", "DE3333",
                "1500.00", "2024-02-07");

        when(mockProvider.getAllInvoices()).thenReturn(List.of(clone1, clone2));

        DuplicationCheckResult result = detector.detectDuplication(current);

        assertTrue(result.isDuplicate());
        assertEquals("AMOUNT_CLONE", result.getType());
        assertEquals(10, result.getPoints());
    }

    @Test
    @DisplayName("Gleicher Betrag nur auf 1 anderem Fahrzeug → kein AMOUNT_CLONE")
    void detectDuplication_amountOnlyOneOther_noClone() {
        InvoiceData current = validInvoice("INV-A", "DE1111",
                "1500.00", "2024-02-05");
        InvoiceData other = validInvoice("INV-B", "DE2222",
                "1500.00", "2024-02-06");

        // Nur 1 anderes Fahrzeug (Detector braucht >= 2)
        when(mockProvider.getAllInvoices()).thenReturn(List.of(other));

        DuplicationCheckResult result = detector.detectDuplication(current);
        assertFalse(result.isDuplicate());
    }

    // ========================================================================
    // Kein Duplikat (Rauschen)
    // ========================================================================

    @Test
    @DisplayName("Verschiedene Rechnungen ohne Muster → noMatch")
    void detectDuplication_distinctInvoices_returnsNoMatch() {
        InvoiceData current = validInvoice("INV-UNIQUE", "DE9999",
                "333.33", "2024-03-01");

        InvoiceData other1 = validInvoice("INV-OTHER1", "DE8888",
                "450.00", "2024-01-10");
        InvoiceData other2 = validInvoice("INV-OTHER2", "DE7777",
                "780.00", "2024-01-20");

        when(mockProvider.getAllInvoices()).thenReturn(List.of(other1, other2));

        DuplicationCheckResult result = detector.detectDuplication(current);
        assertFalse(result.isDuplicate());
        assertEquals("NONE", result.getType());
    }

    // ========================================================================
    // Graceful Degradation
    // ========================================================================

    @Test
    @DisplayName("Provider wirft Exception → noMatch (Graceful Degradation)")
    void detectDuplication_providerThrows_returnsNoMatch() {
        InvoiceData current = validInvoice("INV-001", "DE1234",
                "1000.00", "2024-01-15");
        when(mockProvider.getAllInvoices())
                .thenThrow(new RuntimeException("DB unavailable"));

        DuplicationCheckResult result = detector.detectDuplication(current);
        assertFalse(result.isDuplicate());
        assertEquals("NONE", result.getType());
    }

    // ========================================================================
    // HELPER
    // ========================================================================

    private InvoiceData validInvoice(String number, String plate,
                                     String amount, String date) {
        return InvoiceData.builder()
                .invoiceNumber(number)
                .licensePlate(plate)
                .grossAmount(new BigDecimal(amount))
                .invoiceDate(LocalDate.parse(date))
                .vehicleInfo("Test Vehicle")
                .workshopName("Test Workshop")
                .build();
    }
}
