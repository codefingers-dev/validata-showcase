package de.codefingers.validata.service.extraction;

import de.codefingers.validata.model.dto.InvoiceData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.textract.model.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.*;

/**
 * Parst Textract-Output zu InvoiceData.
 *
 * Verantwortlichkeiten:
 * - Regex-basiertes Parsing von OCR-Text
 * - Confidence Scoring pro Feld
 * - Form/Table Mapping
 * - Datum-Parsing (6 Formate)
 *
 * Unterstützte Felder:
 * - Betrag, Netto, MwSt (19%, 7%)
 * - Rechnungsnummer, Belegnummer, Kundennummer
 * - Kennzeichen, VIN, KM-Stand
 * - Werkstatt, Steuernummer, Datum
 *
 * @see TextractExtractionService
 */
@Slf4j
@Service
public class InvoiceTextParser {

    private static final float CONFIDENCE_THRESHOLD = 0.80f;

    /**
     * Parse aus analyzeDocument Blocks (Forms + Tables).
     */
    public InvoiceData parseFromBlocks(List<Block> blocks) {
        InvoiceData invoice = new InvoiceData();

        for (Block block : blocks) {
            if (block.blockType().equals(BlockType.KEY_VALUE_SET)) {
                String key = block.text();
                String value = extractValueForKey(blocks, block.id());
                mapFormField(invoice, key, value);
            }
        }

        return invoice.getGrossAmount() != null ? invoice : null;
    }

    /**
     * Parse aus detectDocumentText Blocks (Raw Text).
     */
    public InvoiceData parseFromTextBlocks(List<Block> blocks) {
        InvoiceData invoice = new InvoiceData();
        List<String> allLines = new ArrayList<>();

        for (Block block : blocks) {
            if (block.blockType().equals(BlockType.LINE)) {
                float confidence = block.confidence() != null ? block.confidence() : 0.99f;
                if (confidence >= CONFIDENCE_THRESHOLD) {
                    allLines.add(block.text());
                }
            }
        }

        String fullText = String.join("\n", allLines);
        Map<String, Float> fieldConfidence = new HashMap<>();
        parseAllFields(invoice, fullText, fieldConfidence);

        log.info("📊 OCR Confidence Scores: {}", fieldConfidence);

        return invoice.getGrossAmount() != null ? invoice : null;
    }

    /**
     * Parse aus analyzeExpense Documents (Fallback).
     */
    public InvoiceData parseFromExpense(List<ExpenseDocument> expenseDocs) {
        InvoiceData invoice = new InvoiceData();

        for (ExpenseDocument expenseDoc : expenseDocs) {
            for (ExpenseField field : expenseDoc.summaryFields()) {
                String type = field.type().toString();
                String value = field.valueDetection().text();

                if ("TOTAL".equals(type)) {
                    try {
                        invoice.setGrossAmount(new BigDecimal(
                                value.replaceAll("[^0-9.,]", "").replace(",", ".")));
                    } catch (Exception e) {
                        log.warn("Cannot parse expense amount: {}", value);
                    }
                }
            }
        }

        return invoice.getGrossAmount() != null ? invoice : null;
    }

    // ========================================================================
    // REGEX PARSING
    // ========================================================================

    private void parseAllFields(InvoiceData invoice, String fullText,
                                Map<String, Float> confidence) {

        // BETRAG (kritisch)
        extractAmount(invoice, fullText, confidence,
                "(?:Gesamtbetrag|Summe|GESAMT|Brutto|Total)\\s*[:\\-]?\\s*([0-9]+[.,][0-9]{2})",
                "grossAmount", 0.95f);

        // NETTO
        extractAmount(invoice, fullText, confidence,
                "(?:Netto|Summe ohne|Zwischensumme)\\s*[:\\-]?\\s*([0-9]+[.,][0-9]{2})",
                "netAmount", 0.85f);

        // MwSt
        extractAmount(invoice, fullText, confidence,
                "(?:MwSt|VAT|Mehrwertsteuer)\\s*[:\\-]?\\s*([0-9]+[.,][0-9]{2})",
                "vatAmount", 0.80f);

        // MwSt 19%
        extractAmount(invoice, fullText, confidence,
                "(?:MwSt|VAT)\\s*19\\s*%[.:\\-]?\\s*([0-9]+[.,][0-9]{2})",
                "mwst19", 0.85f);

        // RECHNUNGSNUMMER
        extractField(invoice, fullText, confidence,
                "(?:Rechnung\\s+|RechnungsNr\\s+)[:\\-]?\\s*([A-Z0-9\\-]{5,30})",
                "invoiceNumber", 0.85f);

        // KUNDENNUMMER
        extractField(invoice, fullText, confidence,
                "(?:Kunden-?Nr|Customer Number)[.:\\-]?\\s*([0-9A-Z\\-]+)",
                "kundenNummer", 0.85f);

        // BELEGNUMMER
        extractField(invoice, fullText, confidence,
                "(?:Beleg-?Nr|Beleg Number)[.:\\-]?\\s*([0-9A-Z\\-]+)",
                "belegNummer", 0.85f);

        // KENNZEICHEN
        extractField(invoice, fullText, confidence,
                "([A-Z]{1,3}-[A-Z]{2}\\s?\\d{1,4})",
                "licensePlate", 0.80f);

        // VIN
        extractField(invoice, fullText, confidence,
                "(?:FZ-Ident|VIN|Identnummer)\\s*[:\\-]?\\s*([A-Z0-9]{17})",
                "vin", 0.90f);

        // KM-STAND
        extractField(invoice, fullText, confidence,
                "(?:KM[\\-\\s]?Stand|Mileage)[.:\\-]?\\s*([0-9.,]+)\\s*km",
                "kmStand", 0.85f);

        // WERKSTATT
        extractField(invoice, fullText, confidence,
                "^([A-Z][A-Za-z\\s&.,GmbH]+?)(?:\\n|$)",
                "workshopName", 0.75f);

        // STEUERNUMMER
        extractField(invoice, fullText, confidence,
                "(?:Steuernummer|Tax ID|USt-IdNr)[.:\\-]?\\s*([0-9]{3}\\s?[0-9]{3}\\s?[0-9]{3}|DE[0-9]{9})",
                "taxNumber", 0.85f);

        // DATUM
        extractDate(invoice, fullText, confidence,
                "(\\d{1,2}[.\\-/]\\d{1,2}[.\\-/]\\d{4})",
                "invoiceDate", 0.90f);

        // ANNAHMEDATUM
        extractDate(invoice, fullText, confidence,
                "(?:Annahme|Auftragsannahme)[.:\\-]?\\s*(\\d{1,2}[.\\-/]\\d{1,2}[.\\-/]\\d{4})",
                "annahmeDatum", 0.85f);

        // INSPEKTIONSERGEBNIS
        if (fullText.contains("Erhebliche Mängel") || fullText.contains("Mängel erforderlich")) {
            invoice.setAdditionalInfo("Inspektionsergebnis: Erhebliche Mängel erkannt");
            confidence.put("inspection", 0.95f);
        }
    }

    // ========================================================================
    // EXTRACT HELPERS (DRY!)
    // ========================================================================

    private void extractAmount(InvoiceData invoice, String text,
                               Map<String, Float> confidence,
                               String regex, String fieldName,
                               float minConfidence) {
        PatternResult result = matchPattern(text, regex);

        if (result.value == null || result.confidence < minConfidence) return;

        try {
            String amount = result.value.replaceAll("[^0-9.,]", "").replace(",", ".");
            BigDecimal value = new BigDecimal(amount);

            switch (fieldName) {
                case "grossAmount" -> invoice.setGrossAmount(value);
                case "netAmount" -> invoice.setNetAmount(value);
                case "vatAmount" -> invoice.setVatAmount(value);
                case "mwst19" -> invoice.setMwst19Prozent(value);
            }
            confidence.put(fieldName, result.confidence);
        } catch (Exception e) {
            log.warn("{} parsing fehlgeschlagen: {}", fieldName, e.getMessage());
        }
    }

    private void extractField(InvoiceData invoice, String text,
                              Map<String, Float> confidence,
                              String regex, String fieldName,
                              float minConfidence) {
        PatternResult result = matchPattern(text, regex);

        if (result.value == null || result.confidence < minConfidence) return;

        String value = result.value.trim();
        switch (fieldName) {
            case "invoiceNumber" -> invoice.setInvoiceNumber(value);
            case "kundenNummer" -> invoice.setKundenNummer(value);
            case "belegNummer" -> invoice.setBelegNummer(value);
            case "licensePlate" -> invoice.setLicensePlate(value);
            case "vin" -> invoice.setVehicleIdentificationNumber(value);
            case "kmStand" -> invoice.setKmStand(value);
            case "workshopName" -> invoice.setWorkshopName(value);
            case "taxNumber" -> invoice.setTaxNumber(value);
        }
        confidence.put(fieldName, result.confidence);
    }

    private void extractDate(InvoiceData invoice, String text,
                             Map<String, Float> confidence,
                             String regex, String fieldName,
                             float minConfidence) {
        PatternResult result = matchPattern(text, regex);

        if (result.value == null || result.confidence < minConfidence) return;

        try {
            LocalDate date = parseDate(result.value);
            switch (fieldName) {
                case "invoiceDate" -> invoice.setInvoiceDate(date);
                case "annahmeDatum" -> invoice.setAnnahmeDatum(date);
            }
            confidence.put(fieldName, result.confidence);
        } catch (Exception e) {
            log.warn("{} parsing fehlgeschlagen", fieldName);
        }
    }

    // ========================================================================
    // PATTERN MATCHING + CONFIDENCE
    // ========================================================================

    private PatternResult matchPattern(String text, String regex) {
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(text);

        if (matcher.find()) {
            String value = matcher.group(1);
            float confidence = 0.85f;

            if (value != null && !value.contains("?")) confidence += 0.10f;
            if (value != null && value.matches("[0-9]+[.,][0-9]{2}")) confidence = 0.99f;

            return new PatternResult(value, Math.min(confidence, 1.0f));
        }

        return new PatternResult(null, 0.0f);
    }

    private LocalDate parseDate(String dateStr) {
        String[] formats = {"dd.MM.yyyy", "dd-MM-yyyy", "dd/MM/yyyy",
                "yyyy.MM.dd", "yyyy-MM-dd", "yyyy/MM/dd"};

        for (String format : formats) {
            try {
                return LocalDate.parse(dateStr, DateTimeFormatter.ofPattern(format));
            } catch (Exception e) {
                // Try next
            }
        }
        throw new IllegalArgumentException("Cannot parse date: " + dateStr);
    }

    private void mapFormField(InvoiceData invoice, String key, String value) {
        if (key == null || value == null) return;

        if (key.contains("Amount") || key.contains("Total")) {
            try {
                invoice.setGrossAmount(new BigDecimal(
                        value.replaceAll("[^0-9.,]", "").replace(",", ".")));
            } catch (Exception e) {
                log.warn("Cannot parse form amount: {}", value);
            }
        } else if (key.contains("Invoice") || key.contains("Number")) {
            invoice.setInvoiceNumber(value);
        } else if (key.contains("Vendor") || key.contains("Supplier")) {
            invoice.setWorkshopName(value);
        }
    }

    private String extractValueForKey(List<Block> blocks, String keyId) {
        return "";
    }

    // ========================================================================
    // INNER CLASS
    // ========================================================================

    record PatternResult(String value, float confidence) {}
}