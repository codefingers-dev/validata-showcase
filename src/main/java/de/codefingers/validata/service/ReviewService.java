package de.codefingers.validata.service;

import de.codefingers.validata.model.domain.ReviewDecision;
import de.codefingers.validata.repository.ReviewDecisionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

/**
 * Service for managing human review decisions on fraud analysis results
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewDecisionRepository reviewDecisionRepository;

    /**
     * Get review decision for a fraud analysis
     */
    public Optional<ReviewDecision> getReview(String fraudAnalysisId) {
        return reviewDecisionRepository.findByFraudAnalysisId(fraudAnalysisId);
    }

    /**
     * Save a new review decision
     */
    public ReviewDecision submitReview(
            String fraudAnalysisId,
            String invoiceNumber,
            int fraudScore,
            ReviewDecision.Decision decision,
            String comment,
            String reviewerName) {

        log.info("Submitting review for fraud analysis {}: decision = {}",
                fraudAnalysisId, decision);

        ReviewDecision review = ReviewDecision.builder()
                .fraudAnalysisId(fraudAnalysisId)
                .invoiceNumber(invoiceNumber)
                .fraudScore(fraudScore)
                .decision(decision)
                .comment(comment)
                .reviewerName(reviewerName)
                .reviewedAt(Instant.now())
                .build();

        ReviewDecision saved = reviewDecisionRepository.save(review);

        log.info("Review saved for {} with decision {}",
                fraudAnalysisId, decision);

        return saved;
    }

    /**
     * Check if a fraud analysis has been reviewed
     */
    public boolean isReviewed(String fraudAnalysisId) {
        return reviewDecisionRepository.findByFraudAnalysisId(fraudAnalysisId)
                .isPresent();
    }
}