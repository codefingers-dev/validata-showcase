package de.codefingers.validata.service.analysis.rules;

import de.codefingers.validata.model.domain.RedFlag;
import de.codefingers.validata.model.domain.ValidationResult;
import de.codefingers.validata.model.dto.InvoiceData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * LAYER 3 VALIDATION: KFZ Standard Labor Times
 *
 * Data source: DEKRA/TÜV/DIN 66001 Industry Standards
 * Detects: Excessive labor hour charging (~40% of fraud)
 * Example: "Ölwechsel" charged 3h vs standard 0.5-1.0h
 *
 * ─────────────────────────────────────────────────────────────
 * NOTE (Public Showcase Version):
 * This repository demonstrates architecture and detection logic.
 * The full production dataset (98 DEKRA/TÜV-based repair tasks
 * across 8 categories) resides in the private repository.
 * The samples below are sufficient to demonstrate the pattern.
 * ─────────────────────────────────────────────────────────────
 */
@Slf4j
@Service
public class KfzStandardLaborTimes implements RuleEngine {

    private final Map<String, LaborTask> STANDARD_TIMES;

    public KfzStandardLaborTimes() {
        this.STANDARD_TIMES = initializeLaborTimes();
    }

    private Map<String, LaborTask> initializeLaborTimes() {
        Map<String, LaborTask> times = new HashMap<>();

        // ====================================================================
        // SAMPLE ENTRIES — full dataset (98 tasks) in private repository
        // Categories in production: Wartung/Service, Bremsen, Motor/Antrieb,
        // Fahrwerk, Karosserie, Lackierung, Elektronik, Unfallreparatur
        // ====================================================================

        // Wartung / Service
        times.put("Ölwechsel (mit Filter)", new LaborTask(0.5, 1.0, "Wartung / Service", "DEKRA-Orientierung", "Standard oil and filter change"));
        times.put("Luftfilter wechseln", new LaborTask(0.2, 0.5, "Wartung / Service", "DEKRA-Orientierung", "Air filter replacement"));

        // Bremsen
        times.put("Bremsbeläge vorne wechseln", new LaborTask(0.8, 1.5, "Bremsen", "DEKRA-Orientierung", "Front brake pad replacement (per axle)"));
        times.put("Bremsscheiben und Beläge vorne wechseln", new LaborTask(1.5, 2.5, "Bremsen", "DEKRA-Orientierung", "Front brake discs and pads replacement"));

        // Motor / Antrieb
        times.put("Zahnriemenwechsel", new LaborTask(3.0, 6.0, "Motor / Antrieb", "DEKRA-Orientierung", "Timing belt replacement"));
        times.put("Kupplung wechseln", new LaborTask(4.0, 8.0, "Motor / Antrieb", "DEKRA-Orientierung", "Clutch replacement"));

        // Lackierung
        times.put("Ganzlackierung", new LaborTask(25.0, 50.0, "Lackierung", "DEKRA-Orientierung", "Full vehicle respray"));

        // Unfallreparatur
        times.put("Unfallreparatur mittel (Frontschaden)", new LaborTask(10.0, 25.0, "Unfallreparatur", "DEKRA-Orientierung", "Medium front-end collision repair"));

        log.info("✅ Initialized KfzStandardLaborTimes with {} repair tasks (public sample)", times.size());
        return times;
    }

    @Override
    public ValidationResult validate(InvoiceData invoiceData) {
        log.info("🔍 KfzStandardLaborTimes: Validiere Labor-Zeiten");

        ValidationResult result = ValidationResult.allValid();

        if (invoiceData.getLineItems() == null) {
            return result;
        }

        for (InvoiceData.LineItem item : invoiceData.getLineItems()) {

            if (item == null || item.getCategory() == null || item.getQuantity() == null) {
                continue;
            }

            if (item.getCategory() == InvoiceData.LineItem.ItemCategory.LABOR) {
                LaborTask standard = STANDARD_TIMES.get(item.getDescription());

                if (standard == null) continue;

                double chargedHours = item.getQuantity().doubleValue();
                double maxAllowed = standard.maxHours() * 1.5;

                if (chargedHours > maxAllowed) {
                    result.setSumCalculationCorrect(false);
                    result.getErrors().add(String.format(
                            "Excessive labor time: '%s' charged %.1fh but standard max is %.1fh",
                            item.getDescription(), chargedHours, standard.maxHours()));
                }
            }
        }

        return result;
    }

    @Override
    public List<RedFlag> detectRedFlags(InvoiceData invoiceData) {
        List<RedFlag> flags = new ArrayList<>();

        if (invoiceData.getLineItems() == null) {
            return flags;
        }

        for (InvoiceData.LineItem item : invoiceData.getLineItems()) {

            if (item == null || item.getCategory() == null || item.getQuantity() == null) {
                continue;
            }

            if (item.getCategory() == InvoiceData.LineItem.ItemCategory.LABOR) {
                LaborTask standard = STANDARD_TIMES.get(item.getDescription());

                if (standard == null) continue;

                double chargedHours = item.getQuantity().doubleValue();
                double maxAllowed = standard.maxHours() * 1.5;

                if (chargedHours > maxAllowed) {
                    double overage = ((chargedHours / standard.maxHours()) - 1.0) * 100;

                    RedFlag.Severity severity;
                    int scoreImpact;

                    if (overage > 200) {
                        severity = RedFlag.Severity.HIGH;
                        scoreImpact = 25;
                    } else if (overage > 100) {
                        severity = RedFlag.Severity.MEDIUM;
                        scoreImpact = 15;
                    } else {
                        severity = RedFlag.Severity.LOW;
                        scoreImpact = 8;
                    }

                    flags.add(RedFlag.builder()
                            .code("EXCESSIVE_LABOR_TIME")
                            .category(RedFlag.Category.LABOR)
                            .severity(severity)
                            .description(String.format(
                                    "Labor time %.1fh exceeds standard %.1fh by %.0f%%",
                                    chargedHours, standard.maxHours(), overage))
                            .evidence(String.format(
                                    "Task: %s | Charged: %.1fh | Standard max: %.1fh | Category: %s",
                                    item.getDescription(), chargedHours,
                                    standard.maxHours(), standard.category()))
                            .scoreImpact(scoreImpact)
                            .source(RedFlag.Source.RULE)
                            .layer("LAYER_3_LABOR_VALIDATION")
                            .confidence(1.0)
                            .build());

                    log.warn("  ⚠️  EXCESSIVE: '{}' {}h > max {}h",
                            item.getDescription(), chargedHours, standard.maxHours());
                }
            }
        }

        return flags;
    }

    @Override
    public int getRuleCount() {
        return STANDARD_TIMES.size();
    }

    @Override
    public String getVersion() {
        return "1.0.0-labor-times";
    }
}

// ============================================================================
// RECORD DEFINITIONS
// ============================================================================

/**
 * Represents a standard labor task with time ranges.
 */
record LaborTask(
        double minHours,
        double maxHours,
        String category,
        String source,
        String description
) {}