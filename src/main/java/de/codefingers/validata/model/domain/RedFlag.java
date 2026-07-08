package de.codefingers.validata.model.domain;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Repräsentiert eine einzelne Auffälligkeit (Red Flag) in einer Rechnung.
 *
 * Wird durch alle Layer erzeugt:
 * Layer 1: Bedrock AI (Source.AI)
 * Layer 3: Labor-Preise Validation (Source.RULE)
 * Layer 4: Teile-Preise Validation (Source.RULE)
 * Layer 5: Fahrzeug-Historie Validation (Source.RULE)
 * Layer 6: Duplication Detection (Source.RULE)
 * Validation: Input Validation (Source.RULE)
 * System: System Fehler (Source.RULE)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RedFlag {

    /**
     * Eindeutiger Code für die Regel.
     * z.B. "EXACT_DUPLICATE", "INVALID_INVOICE", "SYSTEM_ERROR"
     */
    private String code;

    /**
     * Kategorie der Auffälligkeit.
     */
    private Category category;

    /**
     * Schweregrad der Auffälligkeit.
     */
    private Severity severity;

    /**
     * Menschenlesbare Beschreibung auf Deutsch.
     */
    private String description;

    /**
     * Konkrete Fundstelle/Beweis aus der Rechnung.
     */
    private String evidence;

    /**
     * Punkte, die diese Flag zum Risk Score beiträgt.
     */
    private int scoreImpact;

    /**
     * Quelle der Erkennung (RULE, AI, HYBRID).
     */
    @Builder.Default
    private Source source = Source.RULE;

    /**
     * Layer, von dem diese Flag erkannt wurde.
     * z.B. "LAYER_6_DUPLICATION", "VALIDATION", "SYSTEM_ERROR"
     */
    private String layer;

    /**
     * Confidence Level des Detectors (0.0 - 1.0).
     * Bei Rule-basierten Flags: 1.0 (deterministic)
     * Bei AI-basierten Flags: 0.0-1.0 (LLM confidence)
     */
    @Builder.Default
    private double confidence = 1.0;

    // ===== ENUMS =====

    public enum Category {
        AMOUNT,         // Betrags-Anomalien (Layer 3-4)
        FORMAL,         // Formale Fehler (Validation)
        CONTENT,        // Inhalts-Anomalien (Layer 1-2)
        DOCUMENT,       // Dokument-Anomalien (Layer 1-2)
        DUPLICATE,      // Duplikate (Layer 6)
        VEHICLE,
        VALIDATION,
        DUPLICATION, // Fahrzeug-Anomalien (Layer 5)
        LABOR,
        PARTS,
        SYSTEM          // System-Fehler (Graceful Degradation)
    }

    public enum Severity {
        LOW,            // Geringes Risiko, +5-10 Score
        MEDIUM,         // Mittleres Risiko, +10-20 Score
        HIGH            // Hohes Risiko, +20-30 Score
    }

    public enum Source {
        RULE,           // Von deterministischer Regel erkannt
        AI,             // Von LLM erkannt
        HYBRID          // Von beiden bestätigt
    }

    public static RedFlag amountLow(String code, String description, String evidence, int scoreImpact) {
        return RedFlag.builder()
                .code(code)
                .category(Category.AMOUNT)
                .severity(Severity.LOW)
                .description(description)
                .evidence(evidence)
                .scoreImpact(scoreImpact)
                .source(Source.RULE)
                .build();
    }



    public static RedFlag amountHigh(String code, String description, String evidence, int scoreImpact) {
        return RedFlag.builder()
                .code(code)
                .category(Category.AMOUNT)
                .severity(Severity.HIGH)
                .description(description)
                .evidence(evidence)
                .scoreImpact(scoreImpact)
                .source(Source.RULE)
                .build();
    }


    // ===== HELPER METHODS =====

    /**
     * Erstelle ein HIGH RedFlag für Duplicate Detection (Layer 6)
     *
     * Codes:
     * - EXACT_DUPLICATE: SHA256 identisch
     * - PARTIAL_DUPLICATE: >95% ähnlich
     * - SERIAL_DUPLICATE: 3+ in 14 Tagen
     * - AMOUNT_CLONE: Gleicher Betrag, anderes Auto
     */
    public static RedFlag duplicateHigh(String code, String description, String evidence, int scoreImpact) {
        return RedFlag.builder()
                .code(code)
                .category(Category.DUPLICATE)
                .severity(Severity.HIGH)
                .description(description)
                .evidence(evidence)
                .scoreImpact(scoreImpact)
                .source(Source.RULE)
                .layer("LAYER_6_DUPLICATION_DETECTION")
                .confidence(1.0)  // Deterministic
                .build();
    }

    /**
     * Erstelle ein MEDIUM RedFlag für Duplicate Detection (Layer 6)
     */
    public static RedFlag duplicateMedium(String code, String description, String evidence, int scoreImpact) {
        return RedFlag.builder()
                .code(code)
                .category(Category.DUPLICATE)
                .severity(Severity.MEDIUM)
                .description(description)
                .evidence(evidence)
                .scoreImpact(scoreImpact)
                .source(Source.RULE)
                .layer("LAYER_6_DUPLICATION_DETECTION")
                .confidence(1.0)
                .build();
    }

    /**
     * Erstelle ein HIGH RedFlag für Vehicle History (Layer 5)
     */
    public static RedFlag vehicleHigh(String code, String description, String evidence, int scoreImpact) {
        return RedFlag.builder()
                .code(code)
                .category(Category.VEHICLE)
                .severity(Severity.HIGH)
                .description(description)
                .evidence(evidence)
                .scoreImpact(scoreImpact)
                .source(Source.RULE)
                .layer("LAYER_5_VEHICLE_HISTORY")
                .confidence(1.0)
                .build();
    }

    /**
     * Erstelle ein MEDIUM RedFlag für Amount/Labor (Layer 3-4)
     */
    public static RedFlag amountMedium(String code, String description, String evidence, int scoreImpact) {
        return RedFlag.builder()
                .code(code)
                .category(Category.AMOUNT)
                .severity(Severity.MEDIUM)
                .description(description)
                .evidence(evidence)
                .scoreImpact(scoreImpact)
                .source(Source.RULE)
                .confidence(1.0)
                .build();
    }

    /**
     * Erstelle ein AI-basiertes Flag von Bedrock Claude (Layer 1)
     *
     * ← NEW (für künftige AI Integration)
     */
    public static RedFlag fromBedrock(String code, String description, String evidence,
                                      int scoreImpact, double confidence) {
        return RedFlag.builder()
                .code(code)
                .category(Category.CONTENT)
                .severity(Severity.MEDIUM)
                .description(description)
                .evidence(evidence)
                .scoreImpact(scoreImpact)
                .source(Source.AI)
                .layer("LAYER_1_BEDROCK_ANALYSIS")
                .confidence(confidence)
                .build();
    }

    /**
     * ← NEW: Erstelle HIGH RedFlag für Input Validation Fehler
     *
     * Codes:
     * - INVALID_INVOICE: Allgemeiner Validation Fehler
     * - MISSING_FILE: Datei ist leer
     * - MISSING_FILENAME: Dateiname fehlt
     * - INVALID_FORMAT: Datei ist kein PDF
     */
    public static RedFlag invalidInvoice(String code, String reason) {
        return RedFlag.builder()
                .code(code)
                .category(Category.FORMAL)
                .severity(Severity.HIGH)
                .description("Rechnung ist ungültig")
                .evidence(reason)
                .scoreImpact(0)  // Wird nicht zum Score addiert
                .source(Source.RULE)
                .layer("VALIDATION")
                .confidence(1.0)  // Deterministic
                .build();
    }

    /**
     * ← NEW: Erstelle HIGH RedFlag für System Fehler
     *
     * Codes:
     * - PDF_EXTRACTION_FAILED: Textract konnte PDF nicht lesen
     * - EXTRACTION_TIMEOUT: Textract hat timeout
     * - UNKNOWN_ERROR: Unerwarteter Fehler
     */
    public static RedFlag systemError(String code, String reason) {
        return RedFlag.builder()
                .code(code)
                .category(Category.SYSTEM)
                .severity(Severity.HIGH)
                .description("Sistem-Fehler bei der Analyse")
                .evidence(reason)
                .scoreImpact(0)  // Wird nicht zum Score addiert
                .source(Source.RULE)
                .layer("SYSTEM_ERROR")
                .confidence(1.0)  // Deterministic
                .build();
    }

    /**
     * Gibt eine lesbare Zusammenfassung zurück
     */
    @Override
    public String toString() {
        return String.format("[%s] %s (Score: +%d, Severity: %s, Confidence: %.0f%%)",
                code, description, scoreImpact, severity, confidence * 100);
    }
}