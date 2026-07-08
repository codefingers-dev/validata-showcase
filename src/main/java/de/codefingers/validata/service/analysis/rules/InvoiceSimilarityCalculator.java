package de.codefingers.validata.service.analysis.rules;

import de.codefingers.validata.model.dto.InvoiceData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Similarity Calculator (Helper) für Layer 6 Duplication Detection.
 *
 * Berechnet Ähnlichkeit zwischen Rechnungen mittels:
 * - Levenshtein Distance (Text-Ähnlichkeit)
 * - Date Proximity (zeitliche Nähe)
 * - Amount Similarity (Betrags-Ähnlichkeit)
 *
 * Rückgabe: 0.0 (völlig unterschiedlich) bis 1.0 (identisch)
 */
@Slf4j
@Service
public class InvoiceSimilarityCalculator {

    private static final double WEIGHT_DESCRIPTION = 0.4;
    private static final double WEIGHT_AMOUNT = 0.3;
    private static final double WEIGHT_DATE = 0.2;
    private static final double WEIGHT_VEHICLE = 0.1;

    /**
     * Berechnet gewichtete Ähnlichkeit zwischen zwei Rechnungen.
     *
     * Gewichte:
     * - 40%: Beschreibung (Levenshtein)
     * - 30%: Betrag
     * - 20%: Datum
     * - 10%: Fahrzeug Info
     */
    public double calculateSimilarity(InvoiceData inv1, InvoiceData inv2) {
        // FIX: rawText statt description!
        double descriptionSim = calculateLevenshteinSimilarity(
                inv1.getRawText(),
                inv2.getRawText()
        );

        // FIX: BigDecimal statt double!
        double amountSim = calculateAmountSimilarity(
                inv1.getGrossAmount(),
                inv2.getGrossAmount()
        );

        // FIX: LocalDate statt String!
        double dateSim = calculateDateProximity(
                inv1.getInvoiceDate(),
                inv2.getInvoiceDate()
        );

        double vehicleSim = inv1.getLicensePlate().equals(inv2.getLicensePlate()) ? 1.0 : 0.0;

        // Gewichtete Summe
        return (descriptionSim * WEIGHT_DESCRIPTION) +
                (amountSim * WEIGHT_AMOUNT) +
                (dateSim * WEIGHT_DATE) +
                (vehicleSim * WEIGHT_VEHICLE);
    }

    /**
     * Levenshtein Distance - Textähnlichkeit (0.0-1.0)
     *
     * Berechnet Anzahl der Editierungen (Insert/Delete/Replace)
     * die notwendig sind um String1 zu String2 zu machen.
     */
    private double calculateLevenshteinSimilarity(String s1, String s2) {
        if (s1 == null || s2 == null) return 0.0;

        int distance = levenshteinDistance(s1, s2);
        int maxLength = Math.max(s1.length(), s2.length());

        if (maxLength == 0) return 1.0;
        return 1.0 - ((double) distance / maxLength);
    }

    /**
     * Levenshtein Distance - Implementierung
     */
    private int levenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];

        for (int i = 0; i <= s1.length(); i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= s2.length(); j++) {
            dp[0][j] = j;
        }

        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                int cost = s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(Math.min(
                                dp[i - 1][j] + 1,      // Deletion
                                dp[i][j - 1] + 1),    // Insertion
                        dp[i - 1][j - 1] + cost  // Substitution
                );
            }
        }

        return dp[s1.length()][s2.length()];
    }

    /**
     * Betrags-Ähnlichkeit (0.0-1.0)
     * FIX: Parameter sind jetzt BigDecimal!
     *
     * Je näher die Beträge beieinander, desto höher die Ähnlichkeit.
     * Identische Beträge: 1.0
     * > 20% Unterschied: < 0.3
     */
    private double calculateAmountSimilarity(BigDecimal amount1, BigDecimal amount2) {
        if (amount1 == null || amount2 == null) return 0.0;

        if (amount1.compareTo(amount2) == 0) return 1.0;

        BigDecimal maxAmount = amount1.max(amount2);
        if (maxAmount.compareTo(BigDecimal.ZERO) == 0) return 0.0;

        BigDecimal diff = amount1.subtract(amount2).abs();
        BigDecimal diffPercentage = diff.divide(maxAmount, 4, java.math.RoundingMode.HALF_UP);

        // Linear: Bei 20% Differenz: 0.0
        double percentage = diffPercentage.doubleValue();
        return Math.max(0.0, 1.0 - (percentage * 5));
    }

    /**
     * Zeitliche Nähe (0.0-1.0)
     * FIX: Parameter sind jetzt LocalDate!
     *
     * Identisches Datum: 1.0
     * 14+ Tage Differenz: 0.0
     */
    private double calculateDateProximity(LocalDate date1, LocalDate date2) {
        try {
            if (date1 == null || date2 == null) return 0.0;

            long daysDifference = Math.abs(ChronoUnit.DAYS.between(date1, date2));

            // Linear: max 14 Tage Differenz
            return Math.max(0.0, 1.0 - (daysDifference / 14.0));
        } catch (Exception e) {
            log.warn("Date comparison failed: {} vs {}", date1, date2);
            return 0.0;
        }
    }
}