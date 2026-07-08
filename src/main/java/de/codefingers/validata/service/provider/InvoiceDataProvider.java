package de.codefingers.validata.service.provider;

import de.codefingers.validata.model.dto.InvoiceData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Mock Service für Rechnungsdaten (Phase 1-2).
 *
 * Enthält Test-Daten für Layer 6 Duplication Detection:
 * - Scenario 1: EXACT_DUPLICATE (Rechnung 2x eingereicht)
 * - Scenario 2: PARTIAL_DUPLICATE (96% ähnlich)
 * - Scenario 3: SERIAL_DUPLICATE (4 ähnliche in 6 Tagen)
 * - Scenario 4: AMOUNT_CLONE (€1500 auf 3 verschiedene Fahrzeuge)
 *
 * Phase 3: Wird durch DatabaseInvoiceRepository ersetzt
 */
@Slf4j
@Service
public class InvoiceDataProvider {

    private final List<InvoiceData> mockInvoices;

    public InvoiceDataProvider() {
        this.mockInvoices = createMockInvoices();
        log.info("InvoiceDataProvider initialized with {} mock invoices", mockInvoices.size());
    }

    /**
     * Gibt alle Rechnungen zurück (für Duplikat-Prüfung)
     */
    public List<InvoiceData> getAllInvoices() {
        return new ArrayList<>(mockInvoices);
    }

    /**
     * Sucht Rechnungen nach Kennzeichen
     */
    public List<InvoiceData> findByLicensePlate(String licensePlate) {
        return mockInvoices.stream()
                .filter(inv -> inv.getLicensePlate().equalsIgnoreCase(licensePlate))
                .collect(Collectors.toList());
    }

    /**
     * Sucht Rechnungen nach Bruttobetrag
     */
    public List<InvoiceData> findByGrossAmount(double amount) {
        return mockInvoices.stream()
                .filter(inv -> {
                    BigDecimal invAmount = inv.getGrossAmount();
                    BigDecimal targetAmount = new BigDecimal(amount);
                    return invAmount.subtract(targetAmount).abs().compareTo(new BigDecimal("0.01")) < 0;
                })
                .collect(Collectors.toList());
    }

    /**
     * Sucht Rechnungen in Datumsbereich
     */
    public List<InvoiceData> findByDateRange(LocalDate from, LocalDate to) {
        return mockInvoices.stream()
                .filter(inv -> {
                    LocalDate invDate = inv.getInvoiceDate();
                    return !invDate.isBefore(from) && !invDate.isAfter(to);
                })
                .collect(Collectors.toList());
    }

    /**
     * Sucht Rechnungen nach Werkstatt
     */
    public List<InvoiceData> findByWorkshop(String workshopName) {
        return mockInvoices.stream()
                .filter(inv -> inv.getWorkshopName().toLowerCase().contains(workshopName.toLowerCase()))
                .collect(Collectors.toList());
    }

    // ===== MOCK DATA =====

    /**
     * Erstellt Mock-Daten für Layer 6 Test-Szenarien
     */
    private List<InvoiceData> createMockInvoices() {
        List<InvoiceData> invoices = new ArrayList<>();

        // ===== SCENARIO 1: EXACT_DUPLICATE =====
        // Rechnung INV-2024-001 ist 2x in System
        invoices.add(InvoiceData.builder()
                .invoiceNumber("INV-2024-001")
                .invoiceDate(LocalDate.parse("2024-01-15"))
                .licensePlate("DE1234")
                .vehicleInfo("Mercedes E-Klasse W204 (2012)")
                .grossAmount(new BigDecimal("1011.50"))
                .workshopName("Frenzel KFZ")
                .build());

        invoices.add(InvoiceData.builder()
                .invoiceNumber("INV-2024-001")
                .invoiceDate(LocalDate.parse("2024-01-15"))
                .licensePlate("DE1234")
                .vehicleInfo("Mercedes E-Klasse W204 (2012)")
                .grossAmount(new BigDecimal("1011.50"))
                .workshopName("Frenzel KFZ")
                .build());

        // ===== SCENARIO 2: PARTIAL_DUPLICATE =====
        // Rechnung INV-2024-005 ähnelt INV-2024-006 zu 96%
        invoices.add(InvoiceData.builder()
                .invoiceNumber("INV-2024-005")
                .invoiceDate(LocalDate.parse("2024-01-20"))
                .licensePlate("DE5678")
                .vehicleInfo("BMW 3er F30 (2015)")
                .grossAmount(new BigDecimal("203.49"))
                .workshopName("Auto Müller")
                .build());

        invoices.add(InvoiceData.builder()
                .invoiceNumber("INV-2024-006")
                .invoiceDate(LocalDate.parse("2024-01-21"))
                .licensePlate("DE5678")
                .vehicleInfo("BMW 3er F30 (2015)")
                .grossAmount(new BigDecimal("203.49"))
                .workshopName("Auto Müller")
                .build());

        // ===== SCENARIO 3: SERIAL_DUPLICATE =====
        // 4 ähnliche Rechnungen in 6 Tagen auf SAME Fahrzeug
        invoices.add(InvoiceData.builder()
                .invoiceNumber("INV-2024-101")
                .invoiceDate(LocalDate.parse("2024-02-01"))
                .licensePlate("DE1234")
                .vehicleInfo("Mercedes E-Klasse W204 (2012)")
                .grossAmount(new BigDecimal("450.00"))
                .workshopName("Frenzel KFZ")
                .build());

        invoices.add(InvoiceData.builder()
                .invoiceNumber("INV-2024-102")
                .invoiceDate(LocalDate.parse("2024-02-02"))
                .licensePlate("DE1234")
                .vehicleInfo("Mercedes E-Klasse W204 (2012)")
                .grossAmount(new BigDecimal("445.00"))
                .workshopName("Frenzel KFZ")
                .build());

        invoices.add(InvoiceData.builder()
                .invoiceNumber("INV-2024-103")
                .invoiceDate(LocalDate.parse("2024-02-03"))
                .licensePlate("DE1234")
                .vehicleInfo("Mercedes E-Klasse W204 (2012)")
                .grossAmount(new BigDecimal("455.00"))
                .workshopName("Frenzel KFZ")
                .build());

        // ===== SCENARIO 4: AMOUNT_CLONE =====
        // €1500 auf 3 verschiedene Fahrzeuge (verdächtig!)
        invoices.add(InvoiceData.builder()
                .invoiceNumber("INV-2024-201")
                .invoiceDate(LocalDate.parse("2024-02-05"))
                .licensePlate("DE1111")
                .vehicleInfo("Audi A4 (2018)")
                .grossAmount(new BigDecimal("1500.00"))
                .workshopName("Premium Werkstatt")
                .build());

        invoices.add(InvoiceData.builder()
                .invoiceNumber("INV-2024-202")
                .invoiceDate(LocalDate.parse("2024-02-06"))
                .licensePlate("DE2222")
                .vehicleInfo("VW Passat (2017)")
                .grossAmount(new BigDecimal("1500.00"))
                .workshopName("Premium Werkstatt")
                .build());

        invoices.add(InvoiceData.builder()
                .invoiceNumber("INV-2024-203")
                .invoiceDate(LocalDate.parse("2024-02-07"))
                .licensePlate("DE3333")
                .vehicleInfo("Ford Focus (2016)")
                .grossAmount(new BigDecimal("1500.00"))
                .workshopName("Premium Werkstatt")
                .build());

        // ===== NORMAL INVOICES =====
        // Ein paar normale Rechnungen als "Rauschen"
        invoices.add(InvoiceData.builder()
                .invoiceNumber("INV-2024-999")
                .invoiceDate(LocalDate.parse("2024-02-10"))
                .licensePlate("DE9999")
                .vehicleInfo("Audi A6 (2018)")
                .grossAmount(new BigDecimal("450.00"))
                .workshopName("Auto Müller")
                .build());

        invoices.add(InvoiceData.builder()
                .invoiceNumber("INV-2024-998")
                .invoiceDate(LocalDate.parse("2024-02-11"))
                .licensePlate("DE8888")
                .vehicleInfo("BMW 5er (2016)")
                .grossAmount(new BigDecimal("350.00"))
                .workshopName("Frenzel KFZ")
                .build());

        return invoices;
    }
}
