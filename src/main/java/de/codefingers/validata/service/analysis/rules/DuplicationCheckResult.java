package de.codefingers.validata.service.analysis.rules;

import de.codefingers.validata.model.domain.RedFlag;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Result von InvoiceDuplicationDetector.detectDuplication()
 *
 * Gibt zurück:
 * - Ist es ein Duplikat?
 * - Welcher Typ?
 * - Wie viele Punkte?
 * - Welche Red Flags?
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DuplicationCheckResult {

    /**
     * Ist ein Duplikat gefunden?
     */
    private boolean duplicate;

    /**
     * Typ des Duplikats (wenn duplicate = true)
     * - EXACT_DUPLICATE: SHA256 identisch
     * - PARTIAL_DUPLICATE: >95% ähnlich
     * - SERIAL_DUPLICATE: 3+ in 14 Tagen
     * - AMOUNT_CLONE: Gleicher Betrag, anderes Auto
     * - NONE: Kein Duplikat
     */
    private String type;

    /**
     * Punkte zum Risk Score hinzufügen
     * - EXACT: +30
     * - PARTIAL: +25
     * - SERIAL: +15
     * - AMOUNT: +10
     * - NONE: 0
     */
    private int points;

    /**
     * Red Flags die gefunden wurden
     */
    private List<RedFlag> redFlags;

    /**
     * Confidence (0.0 - 1.0)
     * - EXACT: 1.0 (deterministic)
     * - PARTIAL: 0.80-0.99 (ähnlichkeits-basiert)
     * - SERIAL: 1.0 (deterministic)
     * - AMOUNT: 1.0 (deterministic)
     */
    @Builder.Default
    private double confidence = 1.0;

    /**
     * Helper: Erstelle "kein Duplikat" Result
     */
    public static DuplicationCheckResult noMatch() {
        return DuplicationCheckResult.builder()
                .duplicate(false)
                .type("NONE")
                .points(0)
                .redFlags(List.of())
                .confidence(1.0)
                .build();
    }
}
