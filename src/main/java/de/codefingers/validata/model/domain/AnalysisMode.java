package de.codefingers.validata.model.domain;

/**
 * Analyse-Modi für FraudDetectionOrchestrator
 *
 * RULES_ONLY:  Nur regelbasierte Checks (Layer 1-6) - AKTUELL!
 * AI_ONLY:     Nur AI-basierte Analyse (Bedrock Claude)
 * HYBRID:      Kombination aus Rules + AI (zukünftig)
 */
public enum AnalysisMode {

    /**
     * Pure Rules-Based Detection (v2.0)
     *
     * LAYERS:
     * ├─ Layer 1: KfzStandardLaborTimes
     * ├─ Layer 2: PartsPriceValidator
     * ├─ Layer 3: PhantomWorkValidator
     * ├─ Layer 4: VehicleHistoryValidator
     * ├─ Layer 5: Reserved
     * └─ Layer 6: InvoiceDuplicationDetector
     *
     * VORTEILE:
     * ├─ Transparent & nachvollziehbar
     * ├─ Deterministic (gleiche Input = gleiche Output)
     * ├─ 10x schneller als AI
     * └─ 100x günstiger
     *
     * AKTUELLE PRODUCTION MODE!
     */
    RULES_ONLY,

    /**
     * Pure AI-Based Detection (mit Bedrock Claude)
     *
     * VORTEILE:
     * ├─ Holistisches Denken
     * ├─ Kann subtile Muster sehen
     * └─ Weniger Rules zu konfigurieren
     *
     * NACHTEILE:
     * ├─ Weniger transparent
     * ├─ "Black Box" für Auditors
     * └─ Teurer (Claude API Kosten)
     *
     * STATUS: Zukünftig (nach Hybrid)
     */
    AI_ONLY,

    /**
     * Hybrid: Rules + AI Kombination (empfohlen!)
     *
     * FLOW:
     * 1. Rules laufen (schnell, deterministic)
     * 2. Claude validiert/erweitert (holistisch)
     * 3. Beide Ergebnisse kombinieren
     *
     * VORTEILE:
     * ├─ Bestes von beiden Welten
     * ├─ Redundanz (Sicherheit)
     * ├─ Claude kann Rules fehler fangen
     * └─ Rules schnell, Claude nur bei Bedarf
     *
     * STATUS: Zukünftig (nach Production-Ready RULES_ONLY)
     */
    HYBRID
}