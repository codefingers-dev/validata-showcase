package de.codefingers.validata.model.domain;

/**
 * Analyse-Modi für den FraudDetectionOrchestrator.
 *
 * Aktueller Production-Modus: RULES_ONLY.
 * AI_ONLY und HYBRID sind als zukünftige Erweiterungen vorgesehen.
 */
public enum AnalysisMode {

    /**
     * Rein regelbasierte Erkennung (v2.0) — aktueller Production-Modus.
     *
     * Detection-Layer:
     * ├─ Layer 3: KfzStandardLaborTimes (Arbeitszeiten)
     * ├─ Layer 4: PartsPriceValidator (Ersatzteilpreise)
     * ├─ Layer 4: PhantomWorkValidator (Phantom-Arbeiten)
     * ├─ Layer 5: VehicleHistoryValidator (Fahrzeug-Historie)
     * └─ Layer 6: InvoiceDuplicationDetector (Doppeleinreichungen)
     *
     * Vorteile:
     * ├─ Transparent und nachvollziehbar (auditierbar für BaFin/MaRisk)
     * ├─ Deterministisch (gleiche Eingabe → gleiches Ergebnis)
     * ├─ Keine Halluzinationen
     * └─ Keine Kosten pro Anfrage
     */
    RULES_ONLY,

    /**
     * Rein KI-basierte Erkennung (via AWS Bedrock).
     *
     * Vorteile:  holistische Musterkennung, weniger Regeln zu pflegen.
     * Nachteile: weniger transparent ("Black Box"), Halluzinationsrisiko,
     *            Kosten pro Anfrage.
     *
     * Status: Nicht aktiv. Für den Versicherungskontext wurde bewusst
     *         RULES_ONLY gewählt (Determinismus, Auditierbarkeit).
     */
    AI_ONLY,

    /**
     * Hybrid: Regeln + KI kombiniert.
     *
     * Ablauf:
     * 1. Regeln laufen zuerst (schnell, deterministisch)
     * 2. KI validiert/erweitert nur bei Bedarf
     * 3. Ergebnisse werden kombiniert
     *
     * Vorteile: Redundanz und breitere Abdeckung, ohne die Regel-Basis
     *           aufzugeben.
     *
     * Status: Mögliche zukünftige Erweiterung.
     */
    HYBRID
}