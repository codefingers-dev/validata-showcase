package de.codefingers.validata.controller;

import de.codefingers.validata.model.domain.ReviewDecision;
import de.codefingers.validata.model.request.ReviewDecisionRequest;
import de.codefingers.validata.service.FraudDetectionOrchestrator;
import de.codefingers.validata.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for human review dashboard
 */
@Controller
@Slf4j
@RequiredArgsConstructor
@Tag(name = "Review Dashboard", description = "Human Review Interface for Fraud Analysis")
public class ReviewController {

    private final ReviewService reviewService;
    private final FraudDetectionOrchestrator fraudDetectionOrchestrator;

    // ===== WEB ENDPOINTS (Thymeleaf) =====

    /**
     * Show review dashboard (list of pending reviews)
     */
    @GetMapping("/reviews/dashboard")
    public String dashboard(Model model) {
        log.info("Loading review dashboard");
        // TODO: Load pending reviews from database
        model.addAttribute("title", "Fraud Analysis Review Dashboard");
        return "reviews/dashboard";
    }

    /**
     * Show detail view for a specific fraud analysis
     */
    @GetMapping("/reviews/{fraudAnalysisId}")
    public String reviewDetail(
            @PathVariable String fraudAnalysisId,
            Model model) {

        log.info("Loading review detail for fraud analysis: {}", fraudAnalysisId);

        // TODO: Load fraud analysis result from database
        // For now: add placeholder
        model.addAttribute("fraudAnalysisId", fraudAnalysisId);
        model.addAttribute("title", "Review Fraud Analysis");

        return "reviews/detail";
    }

    // ===== REST API ENDPOINTS =====

    /**
     * GET: Get fraud analysis for review
     */
    @GetMapping("/api/v1/reviews/{fraudAnalysisId}")
    @Operation(summary = "Get fraud analysis for review")
    public ResponseEntity<ReviewDetailResponse> getReview(
            @PathVariable String fraudAnalysisId) {

        log.info("Retrieving fraud analysis: {}", fraudAnalysisId);

        // TODO: Load from database
        ReviewDetailResponse response = ReviewDetailResponse.builder()
                .fraudAnalysisId(fraudAnalysisId)
                .riskScore(45)
                .riskLevel("MEDIUM")
                .invoiceNumber("RE-2025-12345")
                .status("PENDING")
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * POST: Submit a review decision
     */
    @PostMapping("/api/v1/reviews/{fraudAnalysisId}/decision")
    @Operation(summary = "Submit a review decision")
    public ResponseEntity<ReviewDecision> submitDecision(
            @PathVariable String fraudAnalysisId,
            @RequestBody ReviewDecisionRequest request) {

        try {
            log.info("Submitting decision for {}: {}",
                    fraudAnalysisId, request.getDecision());

            // TODO: Load actual fraud analysis data
            ReviewDecision decision = reviewService.submitReview(
                    fraudAnalysisId,
                    "RE-2025-12345",  // TODO: Load from DB
                    45,               // TODO: Load from DB
                    request.getDecision(),
                    request.getComment(),
                    request.getReviewerName()
            );

            log.info("Decision saved for {}", fraudAnalysisId);
            return ResponseEntity.ok(decision);

        } catch (Exception e) {
            log.error("Error submitting decision: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ===== RESPONSE DTOs =====

    @lombok.Data
    @lombok.Builder
    public static class ReviewDetailResponse {
        private String fraudAnalysisId;
        private int riskScore;
        private String riskLevel;
        private String invoiceNumber;
        private String status;  // PENDING, APPROVED, REJECTED
    }
}