package de.codefingers.validata.service.analysis.rules;

import de.codefingers.validata.model.domain.RedFlag;
import de.codefingers.validata.model.domain.ValidationResult;
import de.codefingers.validata.model.dto.InvoiceData;

import java.util.List;

/**
 * Interface für die deterministische Regel-Engine.
 * 
 * STATUS: Interface definiert, Implementierung für Production geplant.
 * 
 * VORTEILE gegenüber AI-Only:
 * - 100% deterministisch (gleiche Eingabe = gleiches Ergebnis)
 * - Nachvollziehbar für Audit/Compliance
 * - Keine Kosten pro Aufruf
 * - Schneller (<10ms vs. 500-2000ms)
 * 
 * IMPLEMENTIERUNG (Production):
 * 
 * @Service
 * @Profile("aws")
 * public class InvoiceRuleEngine implements RuleEngine {
 *     private final List<Rule> rules;
 *     
 *     public List<RedFlag> detectRedFlags(InvoiceData data) {
 *         return rules.stream()
 *             .map(rule -> rule.evaluate(data))
 *             .filter(Optional::isPresent)
 *             .map(Optional::get)
 *             .toList();
 *     }
 * }
 */
public interface RuleEngine {

    /**
     * Führt deterministische Validierungen durch.
     * Diese Prüfungen liefern IMMER das gleiche Ergebnis für gleiche Eingaben.
     *
     * @param invoiceData Die extrahierten Rechnungsdaten
     * @return Validierungsergebnis mit Fehlerliste
     */
    ValidationResult validate(InvoiceData invoiceData);

    /**
     * Erkennt Red Flags basierend auf festen Regeln.
     * Jede Regel hat einen definierten Score-Impact.
     *
     * @param invoiceData Die extrahierten Rechnungsdaten
     * @return Liste der erkannten Red Flags (mit Source=RULE)
     */
    List<RedFlag> detectRedFlags(InvoiceData invoiceData);

    /**
     * Gibt die Anzahl der konfigurierten Regeln zurück.
     */
    default int getRuleCount() {
        return 0;
    }

    /**
     * Gibt die Version der Regel-Engine zurück (für Audit).
     */
    default String getVersion() {
        return "1.0.0";
    }
}
