package de.codefingers.validata.model.domain;

/**
 * Handlungsempfehlungen für Sachbearbeiter
 */
public enum Recommendation {
    APPROVE,     // ✅ Claim akzeptieren
    REJECT,      // ❌ Fraud vermutlich - ablehnen
    REVIEW,      // 🔍 Manuelle Überprüfung erforderlich
    ESCALATE     // ⚠️ Zur SIU (Special Investigation Unit) eskalieren
}