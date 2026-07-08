package de.codefingers.validata.repository;

import de.codefingers.validata.model.domain.ReviewDecision;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewDecisionRepository extends JpaRepository<ReviewDecision, Long> {

    Optional<ReviewDecision> findByFraudAnalysisId(String fraudAnalysisId);

    List<ReviewDecision> findByDecision(ReviewDecision.Decision decision);

    List<ReviewDecision> findByReviewerName(String reviewerName);
}