package de.codefingers.validata.service.analysis.rules;

/**
 * Validation result for parts price analysis
 */
public record PartPriceValidation(
        boolean isValid,
        String status,
        String message,
        int fraudScore,
        String partName,
        double chargedPrice,
        double standardMinPrice,
        double standardMaxPrice,
        String category
) {

    public static PartPriceValidation overpriced(String partName, double chargedPrice,
                                                 double minPrice, double maxPrice,
                                                 String category, int score) {
        return new PartPriceValidation(
                false,
                "OVERPRICED_PART",
                String.format(
                        "Overpriced part: €%.2f charged vs standard €%.2f-€%.2f (Category: %s)",
                        chargedPrice, minPrice, maxPrice, category
                ),
                score,
                partName,
                chargedPrice,
                minPrice,
                maxPrice,
                category
        );
    }

    public static PartPriceValidation normal(String partName, double chargedPrice, double maxPrice) {
        return new PartPriceValidation(
                true,
                "NORMAL_PRICE",
                String.format("Normal price: €%.2f (within standard €%.2f max)", chargedPrice, maxPrice),
                0,
                partName,
                chargedPrice,
                0.0,
                maxPrice,
                ""
        );
    }

    public static PartPriceValidation unknown(String message) {
        return new PartPriceValidation(
                true,
                "UNKNOWN_PART",
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
