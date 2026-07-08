package de.codefingers.validata.model.request;

import de.codefingers.validata.model.domain.ReviewDecision;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for submitting a review decision
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewDecisionRequest {

    private ReviewDecision.Decision decision;  // APPROVED, REJECTED, ESCALATED
    private String comment;                     // Reviewer's comment
    private String reviewerName;                // Who is reviewing?
}