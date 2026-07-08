package de.codefingers.validata.service.scoring;

import de.codefingers.validata.model.domain.RedFlag;
import java.util.List;



public interface ScoreCalculatorService {
    int calculateScore(List<RedFlag> flags);
    String calculateLevel(int score);
    String calculateRecommendation(String level);
}