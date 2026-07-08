package de.codefingers.validata.model.domain;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Represents a human reviewer's decision on a fraud analysis
 */
@Entity
@Table(name = "review_decision")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewDecision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String fraudAnalysisId;  // Reference to FraudAnalysisResult

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Decision decision;  // APPROVED, REJECTED, ESCALATED

    @Column(columnDefinition = "TEXT")
    private String comment;  // Reviewer comment

    @Column(nullable = false)
    private String reviewerName;  // Who reviewed it?

    @Column(nullable = false)
    private Instant reviewedAt;  // When?

    @Column(nullable = false)
    private String invoiceNumber;  // For reference

    @Column(nullable = false)
    private int fraudScore;  // The score that was reviewed

    public enum Decision {
        APPROVED,    // Claim will be paid
        REJECTED,    // Fraud detected - claim denied
        ESCALATED    // Manual inspection needed - escalate to SIU
    }
}
