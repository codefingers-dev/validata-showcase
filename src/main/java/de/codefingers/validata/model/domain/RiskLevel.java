package de.codefingers.validata.model.domain;


/**
 * Risk levels für Fraud Analysis Results
 */
public enum RiskLevel {
    LOW,      // 0-30 Punkte
    MEDIUM,   // 31-60 Punkte
    HIGH,     // 61-100 Punkte
    UNKNOWN   // Kann nicht bewertet werden
}