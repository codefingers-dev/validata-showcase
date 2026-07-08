package de.codefingers.validata.service.analysis.rules;

import de.codefingers.validata.model.domain.RedFlag;
import de.codefingers.validata.model.domain.ValidationResult;
import de.codefingers.validata.model.dto.InvoiceData;
import de.codefingers.validata.service.provider.VehicleHistoryProvider;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
/**
 * LAYER 5 VALIDATION: Vehicle History Anomaly Detection
 *
 * Detects:
 * - High Risk vehicles (via VehicleHistoryProvider)
 * - Excessive accident patterns (3+ accidents)
 * - Major component repairs on wrong vehicle age
 *   (too young < 5 years, too old > 25 years)
 * - Odometer fraud (mileage vs vehicle age)
 *
 * NOT handled here (other Layers):
 * - Labor time validation → Layer 3 (KfzStandardLaborTimes)
 * - Parts price validation → Layer 4 (PartsPriceValidator)
 * - Phantom work detection → Layer 4 (PhantomWorkValidator)
 * - Duplication detection → Layer 6 (InvoiceDuplicationDetector)
 *
 * Impact: 6% of KFZ fraud
 *
 * @see KfzStandardLaborTimes Layer 3
 * @see PartsPriceValidator Layer 4
 * @see PhantomWorkValidator Layer 4
 * @see InvoiceDuplicationDetector Layer 6
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleHistoryValidator implements RuleEngine {

    private final VehicleHistoryProvider vehicleHistoryProvider;

    private final Map<String, Integer> COMPONENT_MAX_AGE = Map.ofEntries(
            Map.entry("motor", 15),
            Map.entry("engine", 15),
            Map.entry("getriebe", 18),
            Map.entry("transmission", 18),
            Map.entry("karosserie", 20)
    );

    // ========================================================================
    // RULE ENGINE INTERFACE
    // ========================================================================

    @Override
    public ValidationResult validate(InvoiceData invoiceData) {
        log.info("🔍 VehicleHistoryValidator: Prüfe Fahrzeug-Historie");

        ValidationResult result = ValidationResult.allValid();

        try {
            if (invoiceData == null || invoiceData.getLicensePlate() == null) {
                return result;
            }

            int vehicleAge = estimateVehicleAge(invoiceData);
            int mileage = parseMileage(invoiceData.getKmStand());

            // Provider-Check
            Optional<VehicleHistoryProvider.VehicleHistory> vehicleOpt =
                    vehicleHistoryProvider.getVehicleHistory(
                            invoiceData.getLicensePlate(),
                            LocalDate.now().getYear() - vehicleAge);

            if (vehicleOpt.isPresent() && vehicleOpt.get().isHighRisk) {
                result.setLicensePlateValid(false);
                result.getErrors().add("Vehicle marked HIGH RISK");
            }

        } catch (Exception e) {
            log.error("Layer 5: Error in validation: {}", e.getMessage(), e);
            result.getErrors().add("ERROR: " + e.getMessage());
        }

        return result;
    }

    @Override
    public List<RedFlag> detectRedFlags(InvoiceData invoiceData) {
        List<RedFlag> flags = new ArrayList<>();

        try {
            if (invoiceData == null) return flags;

            String licensePlate = invoiceData.getLicensePlate();
            int vehicleAge = estimateVehicleAge(invoiceData);
            int mileage = parseMileage(invoiceData.getKmStand());

            // CHECK 1: Provider Data (High Risk, Accidents)
            if (licensePlate != null && !licensePlate.isBlank()) {
                checkProviderData(licensePlate, vehicleAge, flags);
            }

            // CHECK 2: Major Component on Wrong Age Vehicle
            if (invoiceData.getLineItems() != null) {
                checkComponentAge(invoiceData.getLineItems(), vehicleAge, flags);
            }

            // CHECK 3: Odometer Fraud
            if (mileage > 0 && vehicleAge > 0) {
                checkOdometerFraud(mileage, vehicleAge, flags);
            }

        } catch (Exception e) {
            log.error("Layer 5: Error in detection: {}", e.getMessage(), e);
        }

        return flags;
    }

    @Override
    public int getRuleCount() {
        return COMPONENT_MAX_AGE.size();
    }

    @Override
    public String getVersion() {
        return "1.0.0-vehicle-history";
    }

    // ========================================================================
    // 3 FOKUSSIERTE CHECK METHODS
    // ========================================================================

    private void checkProviderData(String licensePlate, int vehicleAge,
                                   List<RedFlag> flags) {
        try {
            Optional<VehicleHistoryProvider.VehicleHistory> vehicleOpt =
                    vehicleHistoryProvider.getVehicleHistory(
                            licensePlate, LocalDate.now().getYear() - vehicleAge);

            if (vehicleOpt.isEmpty()) return;

            VehicleHistoryProvider.VehicleHistory vehicle = vehicleOpt.get();

            if (vehicle.isHighRisk) {
                flags.add(RedFlag.builder()
                        .code("HIGH_RISK_VEHICLE")
                        .category(RedFlag.Category.VEHICLE)
                        .severity(RedFlag.Severity.HIGH)
                        .description("Vehicle " + licensePlate + " is HIGH RISK")
                        .evidence(String.format("Vehicle: %s %s | Provider: %s",
                                vehicle.brand, vehicle.model,
                                vehicleHistoryProvider.getProviderName()))
                        .scoreImpact(25)
                        .source(RedFlag.Source.RULE)
                        .layer("LAYER_5_VEHICLE_HISTORY")
                        .confidence(1.0)
                        .build());
            }

            if (vehicle.accidentCount >= 3) {
                flags.add(RedFlag.builder()
                        .code("EXCESSIVE_ACCIDENTS")
                        .category(RedFlag.Category.VEHICLE)
                        .severity(RedFlag.Severity.HIGH)
                        .description(licensePlate + " has " + vehicle.accidentCount + " accidents")
                        .evidence(String.format("Accidents: %d | Vehicle: %s %s",
                                vehicle.accidentCount, vehicle.brand, vehicle.model))
                        .scoreImpact(30)
                        .source(RedFlag.Source.RULE)
                        .layer("LAYER_5_VEHICLE_HISTORY")
                        .confidence(1.0)
                        .build());
            }

        } catch (Exception e) {
            log.error("Layer 5: Provider check failed: {}", e.getMessage(), e);
        }
    }

    private void checkComponentAge(List<InvoiceData.LineItem> lineItems,
                                   int vehicleAge, List<RedFlag> flags) {

        for (InvoiceData.LineItem item : lineItems) {
            if (item == null || item.getDescription() == null || item.getCategory() == null) {
                continue;
            }

            if (item.getCategory() != InvoiceData.LineItem.ItemCategory.LABOR) {
                continue;
            }

            String lowerDesc = item.getDescription().toLowerCase();

            if (!isMajorComponent(lowerDesc)) continue;

            // Too YOUNG for major repair
            if (vehicleAge < 5) {
                flags.add(RedFlag.builder()
                        .code("MAJOR_REPAIR_YOUNG_VEHICLE")
                        .category(RedFlag.Category.VEHICLE)
                        .severity(RedFlag.Severity.HIGH)
                        .description(String.format(
                                "Major repair '%s' on %d year old vehicle",
                                item.getDescription(), vehicleAge))
                        .evidence(String.format(
                                "Repair: %s | Age: %d years",
                                item.getDescription(), vehicleAge))
                        .scoreImpact(25)
                        .source(RedFlag.Source.RULE)
                        .layer("LAYER_5_VEHICLE_HISTORY")
                        .confidence(1.0)
                        .build());
            }

            // Too OLD for major repair (wirtschaftlicher Totalschaden)
            if (vehicleAge > 25) {
                flags.add(RedFlag.builder()
                        .code("MAJOR_REPAIR_OLD_VEHICLE")
                        .category(RedFlag.Category.VEHICLE)
                        .severity(RedFlag.Severity.MEDIUM)
                        .description(String.format(
                                "Major repair '%s' on %d year old vehicle (write-off?)",
                                item.getDescription(), vehicleAge))
                        .evidence(String.format(
                                "Repair: %s | Age: %d years",
                                item.getDescription(), vehicleAge))
                        .scoreImpact(15)
                        .source(RedFlag.Source.RULE)
                        .layer("LAYER_5_VEHICLE_HISTORY")
                        .confidence(0.85)
                        .build());
            }
        }
    }

    private void checkOdometerFraud(int mileage, int vehicleAge, List<RedFlag> flags) {
        int expectedMax = vehicleAge * 15000;  // ~15k km/year

        if (mileage > expectedMax * 2) {
            flags.add(RedFlag.builder()
                    .code("ODOMETER_ANOMALY")
                    .category(RedFlag.Category.VEHICLE)
                    .severity(RedFlag.Severity.HIGH)
                    .description(String.format(
                            "Mileage %dkm unusual for %d year old vehicle (expected max: %dkm)",
                            mileage, vehicleAge, expectedMax))
                    .evidence(String.format(
                            "Mileage: %dkm | Age: %d years | Expected: %dkm",
                            mileage, vehicleAge, expectedMax))
                    .scoreImpact(20)
                    .source(RedFlag.Source.RULE)
                    .layer("LAYER_5_VEHICLE_HISTORY")
                    .confidence(0.85)
                    .build());
        }
    }

    // ========================================================================
    // HELPERS
    // ========================================================================

    private boolean isMajorComponent(String description) {
        return description.contains("motor")
                || description.contains("getriebe")
                || description.contains("karosserie")
                || description.contains("transmission")
                || description.contains("engine");
    }

    private int estimateVehicleAge(InvoiceData invoiceData) {
        if (invoiceData.getZulassungsDatum() != null) {
            return LocalDate.now().getYear() - invoiceData.getZulassungsDatum().getYear();
        }
        if (invoiceData.getVehicleInfo() != null) {
            try {
                for (String part : invoiceData.getVehicleInfo().split("\\s+")) {
                    if (part.matches("\\d{4}") && Integer.parseInt(part) > 1990) {
                        return LocalDate.now().getYear() - Integer.parseInt(part);
                    }
                }
            } catch (Exception e) {
                log.trace("Could not parse year from: {}", invoiceData.getVehicleInfo());
            }
        }
        return 0;
    }

    private int parseMileage(String kmStand) {
        if (kmStand == null || kmStand.isBlank()) return 0;
        try {
            return Integer.parseInt(kmStand.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    record MileageRange(int minKm, int maxKm) {}
}