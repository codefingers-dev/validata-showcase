package de.codefingers.validata.service;

import de.codefingers.validata.model.domain.ReviewDecision;
import de.codefingers.validata.repository.ReviewDecisionRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit Tests für ReviewService
 * Nutzt Mockito für Repository Mock (Best Practice!)
 */
@Slf4j
@ExtendWith(MockitoExtension.class)  // ← Enable Mockito!
class ReviewServiceTest {

    @Mock
    private ReviewDecisionRepository mockRepository;  // ← Mock statt Implementation!

    private ReviewService reviewService;

    @BeforeEach
    void setup() {
        // ReviewService mit Mock Repository
        reviewService = new ReviewService(mockRepository);
    }

    @Test
    void testSubmitReview_Success() {
        // Arrange
        String fraudAnalysisId = "FA-001";
        String invoiceNumber = "RE-2025-12345";
        int fraudScore = 45;

        // Setup mock: wenn save() aufgerufen wird, return die entity
        when(mockRepository.save(any(ReviewDecision.class)))
                .thenAnswer(invocation -> {
                    ReviewDecision arg = invocation.getArgument(0);
                    arg.setId(1L);
                    return arg;
                });

        // Act
        ReviewDecision decision = reviewService.submitReview(
                fraudAnalysisId,
                invoiceNumber,
                fraudScore,
                ReviewDecision.Decision.APPROVED,
                "Looks good",
                "John Smith"
        );

        // Assert
        assertNotNull(decision);
        assertEquals(fraudAnalysisId, decision.getFraudAnalysisId());
        assertEquals(ReviewDecision.Decision.APPROVED, decision.getDecision());
        log.info("✅ Review submitted successfully");
    }

    @Test
    void testGetReview() {
        // Arrange
        String fraudAnalysisId = "FA-001";
        ReviewDecision review = ReviewDecision.builder()
                .id(1L)
                .fraudAnalysisId(fraudAnalysisId)
                .invoiceNumber("RE-2025-12345")
                .fraudScore(45)
                .decision(ReviewDecision.Decision.REJECTED)
                .build();

        // Setup mock: findByFraudAnalysisId returns review
        when(mockRepository.findByFraudAnalysisId(fraudAnalysisId))
                .thenReturn(Optional.of(review));

        // Act
        Optional<ReviewDecision> result = reviewService.getReview(fraudAnalysisId);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(ReviewDecision.Decision.REJECTED, result.get().getDecision());
        log.info("✅ Review retrieved successfully");
    }

    @Test
    void testIsReviewed_True() {
        // Arrange
        String fraudAnalysisId = "FA-001";
        ReviewDecision review = ReviewDecision.builder()
                .fraudAnalysisId(fraudAnalysisId)
                .build();

        when(mockRepository.findByFraudAnalysisId(fraudAnalysisId))
                .thenReturn(Optional.of(review));

        // Act
        boolean result = reviewService.isReviewed(fraudAnalysisId);

        // Assert
        assertTrue(result);
        log.info("✅ Review status check working");
    }

    @Test
    void testIsReviewed_False() {
        // Arrange
        String fraudAnalysisId = "FA-999";

        when(mockRepository.findByFraudAnalysisId(fraudAnalysisId))
                .thenReturn(Optional.empty());

        // Act
        boolean result = reviewService.isReviewed(fraudAnalysisId);

        // Assert
        assertFalse(result);
        log.info("✅ Review not found correctly");
    }
}