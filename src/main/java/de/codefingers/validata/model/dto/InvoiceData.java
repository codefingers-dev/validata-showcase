package de.codefingers.validata.model.dto;


import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Extrahierte Rechnungsdaten (DTO)
 *
 * Wird von ExtractionService (Textract/Mock) zurückgegeben
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder  // ← FIX: HINZUFÜGEN!
public class InvoiceData {

    @Column
    private String vehicleIdentificationNumber;  // ← Neu

    @Column(columnDefinition = "TEXT")
    private String additionalInfo;  // ← Neu


    // ===== WORKSHOP INFO =====
    private String workshopName;
    private String workshopAddress;
    private String taxNumber;
    private String vatId;
    private String iban;

    // ===== INVOICE INFO =====
    private String invoiceNumber;
    private LocalDate invoiceDate;
    private LocalDate serviceDate;

    // ===== VEHICLE INFO =====
    private String licensePlate;
    private String vehicleInfo;
    private String mileage;

    // ===== AMOUNTS =====
    private BigDecimal netAmount;
    private BigDecimal vatAmount;
    private BigDecimal grossAmount;
    private BigDecimal vatRate;

    // ===== LINE ITEMS =====
    private List<LineItem> lineItems;

    // ===== RAW DATA =====
    private String rawText;  // Kompletter extrahierter Text

    // ===== INNER CLASS: LineItem =====
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder  // ← LineItem braucht auch @Builder!
    public static class LineItem {
        private String description;
        private ItemCategory category;  // ← Nutze Enum!
        private BigDecimal quantity;
        private BigDecimal unitPrice;
        private BigDecimal totalPrice;
        private String unit;

        // ===== INNER ENUM: ItemCategory =====
        public enum ItemCategory {
            LABOR,       // Arbeitsleistung
            PARTS,       // Ersatzteile
            MATERIAL,    // Material
            OTHER        // Sonstiges
        }
    }

    @Column
    private String kundenNummer;  // Kunden-Nr

    @Column
    private String belegNummer;  // Beleg-Nr

    @Column
    private LocalDate annahmeDatum;  // Annahmedatum

    @Column
    private String leistungszeitraum;  // Leistungserstellung von-bis

    @Column
    private String identNummer;  // Ident-Nr (VIN)

    @Column
    private LocalDate zulassungsDatum;  // Zulassungsdatum

    @Column
    private String fahrzeugHersteller;  // Hersteller (VW, Audi, etc)

    @Column
    private String fahrzeugModell;  // Modell (Golf, A4, etc)

    @Column
    private String kmStand;  // KM Stand bei Reparatur

    @Column
    private LocalDate naechsteHuAu;  // Nächste HU/AU Datum

    @Column
    private BigDecimal nettoOhneDurchlaufer;  // Netto ohne Durchläufer

    @Column
    private BigDecimal mwst19Prozent;  // MwSt 19%

    @Column
    private BigDecimal mwst7Prozent;  // MwSt 7%
}