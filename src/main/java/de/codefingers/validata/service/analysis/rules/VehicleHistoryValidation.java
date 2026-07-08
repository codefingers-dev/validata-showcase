package de.codefingers.validata.service.analysis.rules;
/**
 * Validation result for vehicle history anomaly analysis
 */
public record VehicleHistoryValidation(
        boolean isValid,
        String status,
        String message,
        int fraudScore,
        String vehicle,
        int vehicleAge,
        int repairCount,
        int maxExpectedRepairs
) {

    public static VehicleHistoryValidation excessiveRepairs(int repairCount, int maxExpected,
                                                            int vehicleAge, int score) {
        return new VehicleHistoryValidation(
                false,
                "EXCESSIVE_REPAIRS",
                String.format("Vehicle age %d years: %d repairs in 12 months (expected max %d)",
                        vehicleAge, repairCount, maxExpected),
                score,
                "",
                vehicleAge,
                repairCount,
                maxExpected
        );
    }

    public static VehicleHistoryValidation unusualReplacement(String component, int vehicleAge,
                                                              String reason, int score) {
        return new VehicleHistoryValidation(
                false,
                "UNUSUAL_REPLACEMENT",
                String.format("Replacement of '%s' is unusual for %d-year-old vehicle: %s",
                        component, vehicleAge, reason),
                score,
                component,
                vehicleAge,
                0,
                0
        );
    }

    public static VehicleHistoryValidation mileageAnomaly(String repairType, int actualMileage,
                                                          int minExpected, int maxExpected, int score) {
        return new VehicleHistoryValidation(
                false,
                "MILEAGE_ANOMALY",
                String.format("Repair '%s' at %d km is unusual (expected %d-%d km)",
                        repairType, actualMileage, minExpected, maxExpected),
                score,
                repairType,
                0,
                0,
                maxExpected
        );
    }

    public static VehicleHistoryValidation repeatedRepairs(String component, int count, int score) {
        return new VehicleHistoryValidation(
                false,
                "REPEATED_REPAIRS",
                String.format("Component '%s' repaired %d times in history (suspicious pattern)",
                        component, count),
                score,
                component,
                0,
                count,
                1
        );
    }

    public static VehicleHistoryValidation suspiciousAccidentPattern(int accidentCount, int score) {
        return new VehicleHistoryValidation(
                false,
                "SUSPICIOUS_ACCIDENT_PATTERN",
                String.format("Vehicle has %d accident repairs in history (suspicious pattern)",
                        accidentCount),
                score,
                "accident",
                0,
                accidentCount,
                2
        );
    }

    public static VehicleHistoryValidation normal(String vehicle, int age) {
        return new VehicleHistoryValidation(
                true,
                "NORMAL_HISTORY",
                String.format("Vehicle age %d years - repair history appears normal", age),
                0,
                vehicle,
                age,
                0,
                0
        );
    }

    public static VehicleHistoryValidation unknown(String message) {
        return new VehicleHistoryValidation(
                true,
                "UNKNOWN",
                message,
                0,
                "",
                0,
                0,
                0
        );
    }
}
