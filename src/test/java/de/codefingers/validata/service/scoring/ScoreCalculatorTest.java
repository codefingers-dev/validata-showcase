package de.codefingers.validata.service.scoring;

import de.codefingers.validata.model.domain.RedFlag;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ScoreCalculator - Risk Score Berechnung")
class ScoreCalculatorTest {

    private ScoreCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new ScoreCalculator();
    }

    // ========================================
    // calculateScore()
    // ========================================

    @Test
    @DisplayName("Null Flags → Base Score 25")
    void calculateScore_nullFlags_returnsBaseScore() {
        assertEquals(25, calculator.calculateScore(null));
    }

    @Test
    @DisplayName("Empty Flags → Base Score 25")
    void calculateScore_emptyFlags_returnsBaseScore() {
        assertEquals(25, calculator.calculateScore(List.of()));
    }

    @Test
    @DisplayName("Single Flag → Base + Impact")
    void calculateScore_singleFlag_addsToBase() {
        RedFlag flag = RedFlag.builder().scoreImpact(20).build();
        assertEquals(45, calculator.calculateScore(List.of(flag)));
    }

    @Test
    @DisplayName("Multiple Flags → Base + Sum of Impacts")
    void calculateScore_multipleFlags_sumsImpacts() {
        List<RedFlag> flags = List.of(
                RedFlag.builder().scoreImpact(15).build(),
                RedFlag.builder().scoreImpact(20).build(),
                RedFlag.builder().scoreImpact(10).build()
        );
        assertEquals(70, calculator.calculateScore(flags));
    }

    @Test
    @DisplayName("Score capped at 100")
    void calculateScore_highImpact_cappedAt100() {
        RedFlag flag = RedFlag.builder().scoreImpact(200).build();
        assertEquals(100, calculator.calculateScore(List.of(flag)));
    }

    // ========================================
    // calculateLevel()
    // ========================================

    @Test
    @DisplayName("Level mapping korrekt")
    void calculateLevel_allLevels_correct() {
        assertEquals("GREEN", calculator.calculateLevel(0));
        assertEquals("GREEN", calculator.calculateLevel(25));
        assertEquals("YELLOW", calculator.calculateLevel(26));
        assertEquals("YELLOW", calculator.calculateLevel(50));
        assertEquals("RED", calculator.calculateLevel(51));
        assertEquals("RED", calculator.calculateLevel(75));
        assertEquals("CRITICAL", calculator.calculateLevel(76));
        assertEquals("CRITICAL", calculator.calculateLevel(100));
    }

    // ========================================
    // calculateRecommendation()
    // ========================================

    @Test
    @DisplayName("Recommendation mapping korrekt")
    void calculateRecommendation_allLevels_correct() {
        assertEquals("AUTO_APPROVE", calculator.calculateRecommendation("GREEN"));
        assertEquals("MANUAL_REVIEW", calculator.calculateRecommendation("YELLOW"));
        assertEquals("DETAILED_REVIEW", calculator.calculateRecommendation("RED"));
        assertEquals("REJECT_AND_FLAG", calculator.calculateRecommendation("CRITICAL"));
    }

    @Test
    @DisplayName("Unknown Level → MANUAL_REVIEW (safe default)")
    void calculateRecommendation_unknownLevel_defaultsToManualReview() {
        assertEquals("MANUAL_REVIEW", calculator.calculateRecommendation("UNKNOWN"));
    }
}