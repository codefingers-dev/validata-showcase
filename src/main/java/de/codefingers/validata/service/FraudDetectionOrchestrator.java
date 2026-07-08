package de.codefingers.validata.service;

import de.codefingers.validata.exception.AnalysisFailedException;
import de.codefingers.validata.exception.InvalidInvoiceException;
import de.codefingers.validata.exception.TextractExtractionException;  // ← FIX: Richtig importiert!
import de.codefingers.validata.metrics.AnalysisMetricsService;
import de.codefingers.validata.model.domain.FraudAnalysisResult;
import de.codefingers.validata.model.domain.RedFlag;
import de.codefingers.validata.model.domain.ValidationResult;
import de.codefingers.validata.model.dto.InvoiceData;
import de.codefingers.validata.service.analysis.rules.DuplicationCheckResult;
import de.codefingers.validata.service.analysis.rules.DuplicationDetectorService;
import de.codefingers.validata.service.analysis.rules.RuleEngine;
import de.codefingers.validata.service.extraction.ExtractionService;
import de.codefingers.validata.service.scoring.ScoreCalculatorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


/**
 * Zentraler Orchestrator für Rules-Based Fraud Detection (v2.0)
 *
 * Koordiniert den gesamten Analyse-Flow:
 * 1. Input-Validierung (File/JSON)
 * 2. Extraktion (Textract OCR)
 * 3. Duplikats-Erkennung (Layer 6, Early Exit)
 * 4. Rule-Engine Loop (Layers 3-5)
 * 5. Score-Berechnung (via ScoreCalculatorService)
 * 6. Result-Building (via AnalysisResultBuilder)
 *
 * Delegation:
 * - ExtractionService       → OCR/Textract
 * - List<RuleEngine>        → KfzStandardLaborTimes, PartsPriceValidator,
 *                              PhantomWorkValidator, VehicleHistoryValidator
 * - ScoreCalculatorService  → Score + Level + Recommendation
 * - DuplicationDetectorService → Layer 6 Early Exit
 * - AnalysisResultBuilder   → FraudAnalysisResult bauen
 * - AnalysisMetricsService  → Monitoring + Logging
 *
 * - nach SOLID Principles
 * @see AnalysisResultBuilder Result-Building
 * @see ScoreCalculatorService Score-Berechnung
 * @see RuleEngine Rule-Interface
 * @see DuplicationDetectorService Duplikats-Erkennung
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FraudDetectionOrchestrator {



    private final ExtractionService extractionService;
    private final List<RuleEngine> ruleEngines;
    private final ScoreCalculatorService scoreCalculator;
    private final DuplicationDetectorService duplicationDetector;
    private final AnalysisMetricsService metrics;
    private final AnalysisResultBuilder resultBuilder;

    public FraudAnalysisResult analyze(MultipartFile file) {
        long startTime = System.currentTimeMillis();

        try {
            validateInput(file);

            long extractStart = System.currentTimeMillis();
            InvoiceData data = extractionService.extract(file);
            long extractMs = System.currentTimeMillis() - extractStart;

            if (data == null) {
                throw new InvalidInvoiceException("Extraktion ergab keine Daten");
            }

            long analysisStart = System.currentTimeMillis();
            FraudAnalysisResult result = analyzeRulesBased(data);
            long analysisMs = System.currentTimeMillis() - analysisStart;

            long totalMs = System.currentTimeMillis() - startTime;
            result = result.toBuilder()
                    .processingTimeMs(totalMs)
                    .analyzedAt(Instant.now())
                    .build();

            metrics.recordAnalysis(extractMs, analysisMs,
                    result.getRedFlags().size(), result.getRiskLevel());

            return result;

        } catch (TextractExtractionException e) {
            metrics.recordExtractionError();
            return resultBuilder.buildFallback(e.getMessage(),
                    System.currentTimeMillis() - startTime);

        } catch (InvalidInvoiceException e) {
            metrics.recordError("INVALID_INVOICE");
            return resultBuilder.buildInvalid(e.getMessage(),
                    "Validation", System.currentTimeMillis() - startTime);

        } catch (Exception e) {
            metrics.recordError("UNEXPECTED_ERROR");
            throw new AnalysisFailedException(e.getMessage(), e);
        }
    }

    public FraudAnalysisResult analyzeExtracted(InvoiceData data) {
        long startTime = System.currentTimeMillis();

        try {
            if (data == null) throw new InvalidInvoiceException("InvoiceData ist null");
            if (data.getInvoiceNumber() == null || data.getInvoiceNumber().isBlank())
                throw new InvalidInvoiceException("Rechnungsnummer fehlt");
            if (data.getGrossAmount() == null)
                throw new InvalidInvoiceException("Gesamtbetrag fehlt");

            long analysisStart = System.currentTimeMillis();
            FraudAnalysisResult result = analyzeRulesBased(data);
            long analysisMs = System.currentTimeMillis() - analysisStart;

            long totalMs = System.currentTimeMillis() - startTime;
            result = result.toBuilder()
                    .processingTimeMs(totalMs)
                    .analyzedAt(Instant.now())
                    .build();

            metrics.recordAnalysis(0, analysisMs,
                    result.getRedFlags().size(), result.getRiskLevel());

            return result;

        } catch (InvalidInvoiceException e) {
            metrics.recordError("INVALID_INVOICE");
            return resultBuilder.buildInvalid(e.getMessage(),
                    "Validation (JSON)", System.currentTimeMillis() - startTime);

        } catch (Exception e) {
            metrics.recordError("UNEXPECTED_ERROR");
            throw new AnalysisFailedException(e.getMessage(), e);
        }
    }

    private FraudAnalysisResult analyzeRulesBased(InvoiceData data) {
        // Layer 6: Duplication
        Optional<FraudAnalysisResult> dupResult = checkDuplication(data);
        if (dupResult.isPresent()) return dupResult.get();

        // NEU: Formale Validierung
        ValidationResult formalValidation = runValidation(data);

        // Layers 3-5: Rules
        List<RedFlag> flags = detectRedFlags(data);

        // Score
        int score = scoreCalculator.calculateScore(flags);
        String level = scoreCalculator.calculateLevel(score);
        String recommendation = scoreCalculator.calculateRecommendation(level);

        return resultBuilder.buildSuccess(data, flags, score, level, recommendation, formalValidation);
    }

    // NEU: Validation Loop
    private ValidationResult runValidation(InvoiceData data) {
        ValidationResult merged = ValidationResult.allValid();

        for (RuleEngine engine : ruleEngines) {
            try {
                ValidationResult result = engine.validate(data);

                // Merge: Wenn EIN Validator false sagt → bleibt false
                if (!result.isTaxNumberValid()) merged.setTaxNumberValid(false);
                if (!result.isVatIdValid()) merged.setVatIdValid(false);
                if (!result.isVatCalculationCorrect()) merged.setVatCalculationCorrect(false);
                if (!result.isSumCalculationCorrect()) merged.setSumCalculationCorrect(false);
                if (!result.isLicensePlateValid()) merged.setLicensePlateValid(false);
                if (!result.isMandatoryFieldsPresent()) merged.setMandatoryFieldsPresent(false);
                merged.getErrors().addAll(result.getErrors());

            } catch (Exception e) {
                log.error("Validation failed for {}: {}",
                        engine.getClass().getSimpleName(), e.getMessage());
                merged.getErrors().add("ERROR: " + engine.getClass().getSimpleName());
            }
        }

        return merged;
    }

    private Optional<FraudAnalysisResult> checkDuplication(InvoiceData data) {
        DuplicationCheckResult dupCheck = duplicationDetector.detectDuplication(data);

        if (dupCheck.isDuplicate()) {
            metrics.recordDuplication(dupCheck.getType());
            return Optional.of(resultBuilder.buildDuplicate(
                    data, dupCheck.getRedFlags(), dupCheck.getType(),
                    dupCheck.getConfidence(), dupCheck.getPoints()));
        }

        return Optional.empty();
    }

    private List<RedFlag> detectRedFlags(InvoiceData data) {
        List<RedFlag> allFlags = new ArrayList<>();

        for (RuleEngine engine : ruleEngines) {
            try {
                List<RedFlag> flags = engine.detectRedFlags(data);
                allFlags.addAll(flags);
            } catch (Exception e) {
                log.error("{} FAILED: {}", engine.getClass().getSimpleName(), e.getMessage());
                allFlags.add(RedFlag.systemError("LAYER_FAILED",
                        engine.getClass().getSimpleName() + ": " + e.getMessage()));
            }
        }

        return allFlags;
    }

    private void validateInput(MultipartFile file) {
        if (file == null || file.isEmpty())
            throw new InvalidInvoiceException("Datei ist leer");
        if (file.getOriginalFilename() == null)
            throw new InvalidInvoiceException("Dateiname fehlt");
    }


    /**
     * Calculate Risk Score from Red Flags
     */
    private int calculateRiskScore(List<RedFlag> flags) {
        return scoreCalculator.calculateScore(flags);
    }




}