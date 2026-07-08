package de.codefingers.validata.model.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Ergebnis der formalen Validierung einer Rechnung.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ValidationResult {
    
    /**
     * Steuernummer vorhanden und Format korrekt (XXX/XXX/XXXXX).
     */
    private boolean taxNumberValid;
    
    /**
     * USt-IdNr vorhanden und Format korrekt (DE + 9 Ziffern).
     */
    private boolean vatIdValid;
    
    /**
     * MwSt-Berechnung korrekt (19% vom Netto = MwSt).
     */
    private boolean vatCalculationCorrect;
    
    /**
     * Summenprüfung korrekt (Positionen = Netto).
     */
    private boolean sumCalculationCorrect;
    
    /**
     * Alle Pflichtfelder nach §14 UStG vorhanden.
     */
    private boolean mandatoryFieldsPresent;
    
    /**
     * Kfz-Kennzeichen vorhanden und Format korrekt.
     */
    private boolean licensePlateValid;
    
    /**
     * IBAN vorhanden und Format korrekt (falls angegeben).
     */
    private boolean ibanValid;
    
    /**
     * Liste der konkreten Validierungsfehler.
     */
    @Builder.Default
    private List<String> errors = new ArrayList<>();
    
    /**
     * Prüft ob alle Validierungen bestanden wurden.
     */
    public boolean isAllValid() {
        return taxNumberValid && vatIdValid && vatCalculationCorrect 
                && sumCalculationCorrect && mandatoryFieldsPresent;
    }
    
    /**
     * Erstellt ein ValidationResult mit allen Feldern auf true (keine Fehler).
     */
    public static ValidationResult allValid() {
        return ValidationResult.builder()
                .taxNumberValid(true)
                .vatIdValid(true)
                .vatCalculationCorrect(true)
                .sumCalculationCorrect(true)
                .mandatoryFieldsPresent(true)
                .licensePlateValid(true)
                .ibanValid(true)
                .errors(new ArrayList<>())
                .build();
    }
}
