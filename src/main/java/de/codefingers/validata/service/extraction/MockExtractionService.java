package de.codefingers.validata.service.extraction;

import de.codefingers.validata.model.dto.InvoiceData;
import de.codefingers.validata.model.dto.InvoiceData.LineItem;  // ← Für LineItem
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Random;

/**
 * Mock-Implementierung für lokale Entwicklung ohne AWS-Kosten.
 * Generiert realistische Test-Rechnungsdaten basierend auf Dateinamen.
 */
@Slf4j
@Service
@Profile("local")
public class MockExtractionService implements ExtractionService {

    private final Random random = new Random();

    @Override
    public InvoiceData extract(MultipartFile file) {
        try {
            // NULL-CHECK
            if (file == null || file.isEmpty()) {
                log.warn("Mock-Extraktion: Datei ist null oder leer");
                throw new RuntimeException("Datei ist null oder leer");
            }

            String filename = file.getOriginalFilename() != null
                    ? file.getOriginalFilename().toLowerCase()
                    : "";

            log.info("Mock-Extraktion: {} ({} bytes)", filename, file.getSize());

            // Szenario-basierte Generierung
            if (filename.contains("fraud") || filename.contains("suspicious")) {
                return generateSuspiciousInvoice();
            } else if (filename.contains("clean") || filename.contains("valid")) {
                return generateCleanInvoice();
            }
            return generateRandomInvoice();

        } catch (RuntimeException e) {
            log.error("Mock-Extraktion fehlgeschlagen: {}", e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            log.error("Unerwarteter Fehler bei Mock-Extraktion: {}", e.getMessage(), e);
            throw new RuntimeException("Mock-Extraktion fehlgeschlagen: " + e.getMessage(), e);
        }
    }

    private InvoiceData generateCleanInvoice() {
        BigDecimal net = new BigDecimal("850.00");
        BigDecimal vat = new BigDecimal("161.50");
        BigDecimal gross = new BigDecimal("1011.50");

        return InvoiceData.builder()
                .workshopName("Auto Müller GmbH")
                .workshopAddress("Hauptstraße 42, 80331 München")
                .taxNumber("143/158/12345")
                .vatId("DE123456789")
                .iban("DE89370400440532013000")
                .invoiceNumber("RE-2025-" + (1000 + random.nextInt(9000)))
                .invoiceDate(LocalDate.now().minusDays(5))
                .serviceDate(LocalDate.now().minusDays(7))
                .licensePlate("M-AB " + (1000 + random.nextInt(9000)))
                .vehicleInfo("VW Golf 8 1.5 TSI")
                .mileage("45.230 km")
                .netAmount(net)
                .vatAmount(vat)
                .grossAmount(gross)
                .vatRate(new BigDecimal("19"))
                .lineItems(List.of(
                        LineItem.builder()
                                .description("Inspektion nach Herstellervorgabe")
                                .category(LineItem.ItemCategory.LABOR)
                                .quantity(new BigDecimal("2.5"))
                                .unitPrice(new BigDecimal("125.00"))
                                .totalPrice(new BigDecimal("312.50"))
                                .unit("Stunden")
                                .build(),
                        LineItem.builder()
                                .description("Motoröl 5W-30 Longlife")
                                .category(LineItem.ItemCategory.PARTS)
                                .quantity(new BigDecimal("5"))
                                .unitPrice(new BigDecimal("22.50"))
                                .totalPrice(new BigDecimal("112.50"))
                                .unit("Liter")
                                .build()
                ))
                .rawText("Auto Müller GmbH - Rechnung für Inspektion VW Golf")
                .build();
    }

    private InvoiceData generateSuspiciousInvoice() {
        // Betrag knapp unter €750 Gutachter-Grenze
        BigDecimal gross = new BigDecimal("745.00");
        BigDecimal net = new BigDecimal("626.05");
        BigDecimal vat = new BigDecimal("118.95");

        return InvoiceData.builder()
                .workshopName("KFZ-Express Reparatur")
                .workshopAddress("Industriestr. 99, 10115 Berlin")
                .taxNumber(null)  // FEHLT!
                .vatId("DE999888777")
                .invoiceNumber("R-" + random.nextInt(100))
                .invoiceDate(LocalDate.now().minusDays(5))
                .serviceDate(null)  // FEHLT!
                .licensePlate(null)  // FEHLT!
                .vehicleInfo("BMW")
                .netAmount(net)
                .vatAmount(vat)
                .grossAmount(gross)
                .vatRate(new BigDecimal("19"))
                .lineItems(List.of(
                        LineItem.builder()
                                .description("Reparatur pauschal")
                                .category(LineItem.ItemCategory.LABOR)
                                .quantity(new BigDecimal("1"))
                                .unitPrice(gross)
                                .totalPrice(gross)
                                .unit("pauschal")
                                .build()
                ))
                .rawText("KFZ-Express - Reparatur pauschal. Bar bezahlt.")
                .build();
    }

    private InvoiceData generateRandomInvoice() {
        BigDecimal net = new BigDecimal(300 + random.nextInt(1200));
        BigDecimal vat = net.multiply(new BigDecimal("0.19")).setScale(2, java.math.RoundingMode.HALF_UP);
        BigDecimal gross = net.add(vat);

        String taxNumber = random.nextDouble() < 0.7
                ? String.format("%03d/%03d/%05d", random.nextInt(999), random.nextInt(999), random.nextInt(99999))
                : null;

        String licensePlate = random.nextDouble() < 0.8
                ? "M-" + (char)('A' + random.nextInt(26)) + (char)('A' + random.nextInt(26)) + " " + (100 + random.nextInt(9000))
                : null;

        return InvoiceData.builder()
                .workshopName("Werkstatt " + (char)('A' + random.nextInt(26)) + random.nextInt(100))
                .workshopAddress("Musterweg " + random.nextInt(200) + ", 12345 Stadt")
                .taxNumber(taxNumber)
                .vatId("DE" + (100000000 + random.nextInt(899999999)))
                .invoiceNumber("RE-" + (10000 + random.nextInt(90000)))
                .invoiceDate(LocalDate.now().minusDays(random.nextInt(60)))
                .licensePlate(licensePlate)
                .vehicleInfo("Fahrzeug")
                .netAmount(net)
                .vatAmount(vat)
                .grossAmount(gross)
                .vatRate(new BigDecimal("19"))
                .rawText("Werkstattrechnung - Reparatur")
                .build();
    }
}
