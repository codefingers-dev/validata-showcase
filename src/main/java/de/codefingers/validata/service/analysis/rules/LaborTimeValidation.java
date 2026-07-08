package de.codefingers.validata.service.analysis.rules;

/**
 * Validation result for labor hours analysis
 */
public record LaborTimeValidation(
        boolean isValid,
        String status,
        String message,
        int fraudScore,
        String workType,
        double chargedHours,
        double standardMinHours,
        double standardMaxHours,
        String category
) {
    // Factory methods for different validation outcomes

    public static LaborTimeValidation excessive(String workType, double chargedHours,
                                                double minHours, double maxHours,
                                                String category, int score) {
        return new LaborTimeValidation(
                false,
                "EXCESSIVE_LABOR_HOURS",
                String.format(
                        "Excessive labor: %.1fh charged vs standard %.1f-%.1fh (Category: %s)",
                        chargedHours, minHours, maxHours, category
                ),
                score,
                workType,
                chargedHours,
                minHours,
                maxHours,
                category
        );
    }

    public static LaborTimeValidation normal(String workType, double chargedHours, double maxHours) {
        return new LaborTimeValidation(
                true,
                "NORMAL_LABOR_HOURS",
                String.format("Normal labor hours: %.1fh (within standard %.1fh max)",
                        chargedHours, maxHours),
                0,
                workType,
                chargedHours,
                0.0,
                maxHours,
                ""
        );
    }

    public static LaborTimeValidation unknown(String message) {
        return new LaborTimeValidation(
                true,
                "UNKNOWN_WORK_TYPE",
                message,
                0,
                "",
                0.0,
                0.0,
                0.0,
                ""
        );
    }
}
