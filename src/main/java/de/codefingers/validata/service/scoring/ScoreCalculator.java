package de.codefingers.validata.service.scoring;

import de.codefingers.validata.model.domain.RedFlag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service zur Berechnung des Risk Scores.
 *
 * Scoring-Modell:
 *   Base Score (25) + Summe aller RedFlag.scoreImpact
 *   Capped at 100.
 *
 * Risk Levels:
 *   0-25:   GREEN    → Kein Handlungsbedarf
 *   26-50:  YELLOW   → Manuelle Prüfung empfohlen
 *   51-75:  RED      → Manuelle Prüfung erforderlich
 *   76-100: CRITICAL → Sofortige Eskalation
 */
@Slf4j
@Service
public class ScoreCalculator implements ScoreCalculatorService {

    private static final int BASE_SCORE = 25;
    private static final int MAX_SCORE = 100;

    /**
     * Berechne Risk Score aus Red Flags.
     *
     * Formel: Base(25) + Summe aller RedFlag.scoreImpact
     *
     * @param flags Liste aller erkannten Red Flags
     * @return Score 0-100
     */
    public int calculateScore(List<RedFlag> flags) {
        if (flags == null || flags.isEmpty()) {
            return BASE_SCORE;
        }

        int flagPoints = flags.stream()
                .mapToInt(RedFlag::getScoreImpact)
                .sum();

        int totalScore = Math.min(BASE_SCORE + flagPoints, MAX_SCORE);

        log.info("📊 Score: Base({}) + Flags({}) = {} [{}]",
                BASE_SCORE, flagPoints, totalScore, calculateLevel(totalScore));

        return totalScore;
    }

    /**
     * Bestimme Risk Level aus Score.
     *
     * @param score Risk Score (0-100)
     * @return Level als String (GREEN, YELLOW, RED, CRITICAL)
     */
    public String calculateLevel(int score) {
        if (score >= 76) return "CRITICAL";
        if (score >= 51) return "RED";
        if (score >= 26) return "YELLOW";
        return "GREEN";
    }

    /**
     * Bestimme Empfehlung aus Level.
     *
     * @param level Risk Level (GREEN, YELLOW, RED, CRITICAL)
     * @return Empfehlung als String
     */
    public String calculateRecommendation(String level) {
        return switch (level) {
            case "GREEN" -> "AUTO_APPROVE";
            case "YELLOW" -> "MANUAL_REVIEW";
            case "RED" -> "DETAILED_REVIEW";
            case "CRITICAL" -> "REJECT_AND_FLAG";
            default -> "MANUAL_REVIEW";
        };
    }
}