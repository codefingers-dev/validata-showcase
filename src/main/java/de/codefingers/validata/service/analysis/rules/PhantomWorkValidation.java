package de.codefingers.validata.service.analysis.rules;

/**
 * Validation result for phantom/fictitious work analysis
 */
public record PhantomWorkValidation(
        boolean isValid,
        String status,
        String message,
        int fraudScore,
        String workType,
        double allocatedHours,
        double minRealisticHours,
        double maxRealisticHours
) {

    public static PhantomWorkValidation unrealisticTime(String workType, double allocated,
                                                        double minRealistic, double maxRealistic,
                                                        int score, String message) {
        return new PhantomWorkValidation(
                false,
                "UNREALISTIC_TIME",
                message,
                score,
                workType,
                allocated,
                minRealistic,
                maxRealistic
        );
    }

    public static PhantomWorkValidation vehicleIncompatible(String workType, String vehicleType,
                                                            String incompatibleWork, int score) {
        return new PhantomWorkValidation(
                false,
                "VEHICLE_INCOMPATIBLE",
                String.format("Work '%s' is incompatible with vehicle type '%s'", incompatibleWork, vehicleType),
                score,
                workType,
                0,
                0,
                0
        );
    }

    public static PhantomWorkValidation duplicateWork(String workType, int count, int score) {
        return new PhantomWorkValidation(
                false,
                "DUPLICATE_WORK",
                String.format("Work '%s' appears %d times in invoice (should only appear once)", workType, count),
                score,
                workType,
                0,
                0,
                0
        );
    }

    public static PhantomWorkValidation normal(String workType, double hours) {
        return new PhantomWorkValidation(
                true,
                "NORMAL_WORK",
                String.format("Work '%s' allocated %.1fh - realistic", workType, hours),
                0,
                workType,
                hours,
                0,
                0
        );
    }

    public static PhantomWorkValidation unknown(String message) {
        return new PhantomWorkValidation(
                true,
                "UNKNOWN_WORK",
                message,
                0,
                "",
                0,
                0,
                0
        );
    }
}