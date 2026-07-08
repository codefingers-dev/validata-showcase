package de.codefingers.validata.service.analysis.rules;

import de.codefingers.validata.model.domain.RedFlag;
import de.codefingers.validata.model.domain.ValidationResult;
import de.codefingers.validata.model.dto.InvoiceData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * LAYER 4 VALIDATION: Phantom/Fictitious Work Detection
 *
 * Detects:
 * - Logically incompatible work combinations
 * - Unrealistic time allocations
 * - Vehicle-incompatible repairs
 * - Duplicate work items on same invoice
 *
 * Impact: ~8% of KFZ fraud
 *
 * ─────────────────────────────────────────────────────────────
 * NOTE (Public Showcase Version):
 * This repository demonstrates architecture and detection logic.
 * The full production rulesets (incompatibility matrix, timing
 * ranges, vehicle-type constraints) reside in the private
 * repository. The samples below demonstrate the pattern.
 * ─────────────────────────────────────────────────────────────
 */
@Slf4j
@Service
public class PhantomWorkValidator implements RuleEngine {

    private final Map<String, String[]> INCOMPATIBLE_WORK_PAIRS;
    private final Map<String, TimeRange> REALISTIC_WORK_TIMES;
    private final Map<String, String[]> VEHICLE_INCOMPATIBILITIES;

    public PhantomWorkValidator() {
        this.INCOMPATIBLE_WORK_PAIRS = initializeIncompatiblePairs();
        this.REALISTIC_WORK_TIMES = initializeRealisticTimes();
        this.VEHICLE_INCOMPATIBILITIES = initializeVehicleIncompatibilities();

        log.info("✅ Initialized PhantomWorkValidator with {} incompatibility patterns, {} time ranges",
                INCOMPATIBLE_WORK_PAIRS.size(), REALISTIC_WORK_TIMES.size());
    }

    // ========================================================================
    // RULE ENGINE INTERFACE
    // ========================================================================

    @Override
    public ValidationResult validate(InvoiceData invoiceData) {
        log.info("🔍 PhantomWorkValidator: Prüfe auf Phantom-Arbeiten");

        ValidationResult result = ValidationResult.allValid();

        try {
            if (invoiceData == null || invoiceData.getLineItems() == null) {
                return result;
            }

            List<InvoiceData.LineItem> laborItems = invoiceData.getLineItems().stream()
                    .filter(item -> item != null && item.getCategory() != null)
                    .filter(item -> item.getCategory() == InvoiceData.LineItem.ItemCategory.LABOR)
                    .collect(Collectors.toList());

            // CHECK 1: Incompatible Work Pairs
            List<String> descriptions = laborItems.stream()
                    .map(item -> item.getDescription() != null ? item.getDescription().toLowerCase() : "")
                    .collect(Collectors.toList());

            for (Map.Entry<String, String[]> entry : INCOMPATIBLE_WORK_PAIRS.entrySet()) {
                boolean hasFirst = descriptions.stream()
                        .anyMatch(d -> d.contains(entry.getKey()));

                if (hasFirst) {
                    for (String incompatible : entry.getValue()) {
                        boolean hasSecond = descriptions.stream()
                                .anyMatch(d -> d.contains(incompatible));

                        if (hasSecond) {
                            result.setSumCalculationCorrect(false);
                            result.getErrors().add(String.format(
                                    "Incompatible work: '%s' + '%s' cannot occur together",
                                    entry.getKey(), incompatible));
                        }
                    }
                }
            }

            // CHECK 2: Duplicate Work Items
            Map<String, Long> duplicates = descriptions.stream()
                    .filter(d -> !d.isBlank())
                    .collect(Collectors.groupingBy(d -> d, Collectors.counting()));

            for (Map.Entry<String, Long> dup : duplicates.entrySet()) {
                if (dup.getValue() > 1) {
                    result.setSumCalculationCorrect(false);
                    result.getErrors().add(String.format(
                            "Duplicate work: '%s' appears %d times",
                            dup.getKey(), dup.getValue()));
                }
            }

        } catch (NullPointerException e) {
            log.error("Layer 4: NullPointerException in phantom validation: {}", e.getMessage(), e);
            result.setSumCalculationCorrect(false);
            result.getErrors().add("ERROR: Null value in phantom validation");

        } catch (Exception e) {
            log.error("Layer 4: Unexpected error in phantom validation: {}", e.getMessage(), e);
            result.setSumCalculationCorrect(false);
            result.getErrors().add("ERROR: " + e.getMessage());
        }

        return result;
    }

    @Override
    public List<RedFlag> detectRedFlags(InvoiceData invoiceData) {
        List<RedFlag> flags = new ArrayList<>();

        try {
            if (invoiceData == null || invoiceData.getLineItems() == null) {
                return flags;
            }

            List<InvoiceData.LineItem> laborItems = invoiceData.getLineItems().stream()
                    .filter(item -> item != null && item.getCategory() != null)
                    .filter(item -> item.getCategory() == InvoiceData.LineItem.ItemCategory.LABOR)
                    .collect(Collectors.toList());

            List<String> descriptions = laborItems.stream()
                    .map(item -> item.getDescription() != null ? item.getDescription().toLowerCase() : "")
                    .collect(Collectors.toList());

            // ===== CHECK 1: Incompatible Work Pairs =====
            checkIncompatiblePairs(descriptions, flags);

            // ===== CHECK 2: Unrealistic Time =====
            checkUnrealisticTimes(laborItems, flags);

            // ===== CHECK 3: Vehicle Incompatibility =====
            if (invoiceData.getVehicleInfo() != null) {
                checkVehicleIncompatibility(descriptions, invoiceData.getVehicleInfo(), flags);
            }

            // ===== CHECK 4: Duplicate Work Items =====
            checkDuplicateWork(descriptions, flags);

        } catch (NullPointerException e) {
            log.error("Layer 4: NullPointerException in phantom detection: {}", e.getMessage(), e);
        } catch (Exception e) {
            log.error("Layer 4: Unexpected error in phantom detection: {}", e.getMessage(), e);
        }

        return flags;
    }

    @Override
    public int getRuleCount() {
        return INCOMPATIBLE_WORK_PAIRS.size()
                + REALISTIC_WORK_TIMES.size()
                + VEHICLE_INCOMPATIBILITIES.size();
    }

    @Override
    public String getVersion() {
        return "1.0.0-phantom-work";
    }

    // ========================================================================
    // PRIVATE CHECK METHODS
    // ========================================================================

    private void checkIncompatiblePairs(List<String> descriptions, List<RedFlag> flags) {
        for (Map.Entry<String, String[]> entry : INCOMPATIBLE_WORK_PAIRS.entrySet()) {
            boolean hasFirst = descriptions.stream()
                    .anyMatch(d -> d.contains(entry.getKey()));

            if (hasFirst) {
                for (String incompatible : entry.getValue()) {
                    boolean hasSecond = descriptions.stream()
                            .anyMatch(d -> d.contains(incompatible));

                    if (hasSecond) {
                        flags.add(RedFlag.builder()
                                .code("INCOMPATIBLE_WORK")
                                .category(RedFlag.Category.LABOR)
                                .severity(RedFlag.Severity.HIGH)
                                .description(String.format(
                                        "Incompatible work: '%s' + '%s' cannot occur together",
                                        entry.getKey(), incompatible))
                                .evidence(String.format(
                                        "Work1: %s | Work2: %s | Rule: logically impossible combination",
                                        entry.getKey(), incompatible))
                                .scoreImpact(25)
                                .source(RedFlag.Source.RULE)
                                .layer("LAYER_4_PHANTOM_WORK")
                                .confidence(1.0)
                                .build());

                        log.warn("  ⚠️  INCOMPATIBLE: '{}' + '{}'", entry.getKey(), incompatible);
                    }
                }
            }
        }
    }

    private void checkUnrealisticTimes(List<InvoiceData.LineItem> laborItems, List<RedFlag> flags) {
        for (InvoiceData.LineItem item : laborItems) {
            if (item.getDescription() == null || item.getQuantity() == null) {
                continue;
            }

            String lowerDesc = item.getDescription().toLowerCase();
            double timeSpent = item.getQuantity().doubleValue();

            for (Map.Entry<String, TimeRange> entry : REALISTIC_WORK_TIMES.entrySet()) {
                if (lowerDesc.contains(entry.getKey())) {
                    TimeRange range = entry.getValue();

                    // Too short (phantom: charged but not done)
                    if (timeSpent < range.minHours * 0.5) {
                        flags.add(RedFlag.builder()
                                .code("UNREALISTIC_TIME_SHORT")
                                .category(RedFlag.Category.LABOR)
                                .severity(RedFlag.Severity.MEDIUM)
                                .description(String.format(
                                        "Unrealistically short: '%s' %.1fh (minimum: %.1fh)",
                                        entry.getKey(), timeSpent, range.minHours))
                                .evidence(String.format(
                                        "Task: %s | Time: %.1fh | Min: %.1fh | Max: %.1fh",
                                        item.getDescription(), timeSpent, range.minHours, range.maxHours))
                                .scoreImpact(15)
                                .source(RedFlag.Source.RULE)
                                .layer("LAYER_4_PHANTOM_WORK")
                                .confidence(1.0)
                                .build());

                        log.warn("  ⚠️  TOO SHORT: '{}' {}h < min {}h",
                                entry.getKey(), timeSpent, range.minHours);
                    }

                    // Too long (inflated hours)
                    if (timeSpent > range.maxHours * 1.5) {
                        flags.add(RedFlag.builder()
                                .code("UNREALISTIC_TIME_LONG")
                                .category(RedFlag.Category.LABOR)
                                .severity(RedFlag.Severity.MEDIUM)
                                .description(String.format(
                                        "Unrealistically long: '%s' %.1fh (maximum: %.1fh)",
                                        entry.getKey(), timeSpent, range.maxHours))
                                .evidence(String.format(
                                        "Task: %s | Time: %.1fh | Min: %.1fh | Max: %.1fh",
                                        item.getDescription(), timeSpent, range.minHours, range.maxHours))
                                .scoreImpact(15)
                                .source(RedFlag.Source.RULE)
                                .layer("LAYER_4_PHANTOM_WORK")
                                .confidence(1.0)
                                .build());

                        log.warn("  ⚠️  TOO LONG: '{}' {}h > max {}h",
                                entry.getKey(), timeSpent, range.maxHours);
                    }

                    break;  // Found match, stop searching
                }
            }
        }
    }

    private void checkVehicleIncompatibility(List<String> descriptions,
                                             String vehicleInfo, List<RedFlag> flags) {
        String lowerVehicle = vehicleInfo.toLowerCase();

        for (Map.Entry<String, String[]> entry : VEHICLE_INCOMPATIBILITIES.entrySet()) {
            if (lowerVehicle.contains(entry.getKey())) {
                for (String incompatibleWork : entry.getValue()) {
                    boolean found = descriptions.stream()
                            .anyMatch(d -> d.contains(incompatibleWork));

                    if (found) {
                        flags.add(RedFlag.builder()
                                .code("VEHICLE_INCOMPATIBLE_WORK")
                                .category(RedFlag.Category.VEHICLE)
                                .severity(RedFlag.Severity.HIGH)
                                .description(String.format(
                                        "Work '%s' impossible for vehicle type '%s'",
                                        incompatibleWork, entry.getKey()))
                                .evidence(String.format(
                                        "Vehicle: %s | Type: %s | Work: %s | Rule: type-incompatible",
                                        vehicleInfo, entry.getKey(), incompatibleWork))
                                .scoreImpact(30)
                                .source(RedFlag.Source.RULE)
                                .layer("LAYER_4_PHANTOM_WORK")
                                .confidence(1.0)
                                .build());

                        log.warn("  ⚠️  VEHICLE INCOMPATIBLE: '{}' on '{}'",
                                incompatibleWork, entry.getKey());
                    }
                }
            }
        }
    }

    private void checkDuplicateWork(List<String> descriptions, List<RedFlag> flags) {
        Map<String, Long> duplicates = descriptions.stream()
                .filter(d -> !d.isBlank())
                .collect(Collectors.groupingBy(d -> d, Collectors.counting()));

        for (Map.Entry<String, Long> dup : duplicates.entrySet()) {
            if (dup.getValue() > 1) {
                flags.add(RedFlag.builder()
                        .code("DUPLICATE_WORK_ITEM")
                        .category(RedFlag.Category.LABOR)
                        .severity(RedFlag.Severity.HIGH)
                        .description(String.format(
                                "Duplicate work: '%s' appears %d times on same invoice",
                                dup.getKey(), dup.getValue()))
                        .evidence(String.format(
                                "Work: %s | Count: %d | Rule: same work cannot appear multiple times",
                                dup.getKey(), dup.getValue()))
                        .scoreImpact(20)
                        .source(RedFlag.Source.RULE)
                        .layer("LAYER_4_PHANTOM_WORK")
                        .confidence(1.0)
                        .build());

                log.warn("  ⚠️  DUPLICATE: '{}' x{}", dup.getKey(), dup.getValue());
            }
        }
    }

    // ========================================================================
    // DATA INITIALIZATION
    // Sample entries — full rulesets in private repository
    // ========================================================================

    private Map<String, String[]> initializeIncompatiblePairs() {
        // Sample entries - full incompatibility matrix in private repository
        return Map.ofEntries(
                Map.entry("inspektionen", new String[]{"vollrevision", "complete overhaul"}),
                Map.entry("ölwechsel", new String[]{"motorüberholung", "engine overhaul"}),
                Map.entry("einzelteil lackiert", new String[]{"ganzlackierung", "full paint"})
        );
    }

    private Map<String, TimeRange> initializeRealisticTimes() {
        // Sample entries - full timing dataset in private repository
        return Map.ofEntries(
                Map.entry("ölwechsel", new TimeRange(0.5, 1.5)),
                Map.entry("bremsbeläge", new TimeRange(1.0, 2.5)),
                Map.entry("zahnriemen", new TimeRange(2.0, 5.0)),
                Map.entry("ganzlackierung", new TimeRange(16.0, 32.0))
        );
    }

    private Map<String, String[]> initializeVehicleIncompatibilities() {
        // Sample entries - full vehicle constraint matrix in private repository
        return Map.ofEntries(
                Map.entry("elektro", new String[]{"getriebe", "kupplung", "diesel"}),
                Map.entry("diesel", new String[]{"zündkerzen", "zündsystem"})
        );
    }

    // ========================================================================
    // RECORDS
    // ========================================================================

    record TimeRange(double minHours, double maxHours) {}
}