package de.codefingers.validata.service.analysis.rules;

import de.codefingers.validata.model.domain.RedFlag;
import de.codefingers.validata.model.domain.ValidationResult;
import de.codefingers.validata.model.dto.InvoiceData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * LAYER 4 VALIDATION: Automotive Parts Price Validation
 *
 * Data source: Market research (realistic workshop prices)
 * Detects: Overpriced parts (~12% of fraud)
 * Example: "Bremsbeläge €200" (normal €50-90) → +points
 *
 * ─────────────────────────────────────────────────────────────
 * NOTE (Public Showcase Version):
 * This repository demonstrates architecture and detection logic.
 * The full production dataset (55 market-researched parts across
 * 7 categories) resides in the private repository. The samples
 * below are sufficient to demonstrate the pattern.
 * ─────────────────────────────────────────────────────────────
 */
@Slf4j
@Service
public class PartsPriceValidator implements RuleEngine {

    private final Map<String, AutoPart> PARTS_DATABASE;

    public PartsPriceValidator() {
        this.PARTS_DATABASE = initializePartsDatabase();
    }

    private Map<String, AutoPart> initializePartsDatabase() {
        Map<String, AutoPart> parts = new HashMap<>();

        // ====================================================================
        // SAMPLE ENTRIES — full dataset (55 parts) in private repository
        // Categories in production: Bremsen, Motor/Antrieb, Fahrwerk,
        // Reifen/Räder, Karosserie/Lackierung, Elektronik, Verschleißteile
        // ====================================================================

        // Bremsen
        parts.put("Bremsbeläge vorne", new AutoPart(50.0, 90.0, "Bremsen", "Brake pads front axle"));
        parts.put("Bremsscheiben vorne", new AutoPart(80.0, 150.0, "Bremsen", "Brake discs front"));

        // Motor / Antrieb
        parts.put("Ölfilter", new AutoPart(10.0, 25.0, "Motor / Antrieb", "Oil filter"));
        parts.put("Zahnriemen", new AutoPart(40.0, 100.0, "Motor / Antrieb", "Timing belt"));
        parts.put("Lichtmaschine", new AutoPart(120.0, 300.0, "Motor / Antrieb", "Alternator"));

        // Fahrwerk
        parts.put("Stoßdämpfer", new AutoPart(80.0, 200.0, "Fahrwerk", "Shock absorber (per unit)"));

        // Elektronik
        parts.put("Batterie", new AutoPart(80.0, 200.0, "Elektronik", "Car battery"));

        // Reifen / Räder
        parts.put("Reifen", new AutoPart(60.0, 200.0, "Reifen / Räder", "Tire (per unit, average)"));

        log.info("✅ Initialized PartsPriceValidator with {} parts (public sample)", parts.size());
        return parts;
    }

    /**
     * Validates part price against market standards.
     * Returns validation result with error if overpriced.
     */
    @Override
    public ValidationResult validate(InvoiceData invoiceData) {
        log.info("🔍 PartsPriceValidator: Validiere Teile-Preise");

        ValidationResult result = ValidationResult.allValid();

        try {
            if (invoiceData == null || invoiceData.getLineItems() == null) {
                return result;
            }

            for (InvoiceData.LineItem item : invoiceData.getLineItems()) {

                if (item == null || item.getCategory() == null) {
                    continue;
                }

                if (item.getCategory() == InvoiceData.LineItem.ItemCategory.PARTS) {
                    AutoPart part = PARTS_DATABASE.get(item.getDescription());

                    if (part == null) {
                        log.trace("Unknown part: {}", item.getDescription());
                        continue;
                    }

                    if (item.getUnitPrice() == null) {
                        continue;
                    }

                    double chargedPrice = item.getUnitPrice().doubleValue();
                    double maxAllowed = part.maxPrice() * 1.5;

                    if (chargedPrice > maxAllowed) {
                        result.setSumCalculationCorrect(false);
                        result.getErrors().add(String.format(
                                "Overpriced part: '%s' charged €%.2f but standard max is €%.2f",
                                item.getDescription(),
                                chargedPrice,
                                part.maxPrice()
                        ));
                    }
                }
            }

        } catch (NullPointerException e) {
            log.error("Layer 4: NullPointerException in price validation: {}", e.getMessage(), e);
            result.setSumCalculationCorrect(false);
            result.getErrors().add("ERROR: Null value in price validation");

        } catch (Exception e) {
            log.error("Layer 4: Unexpected error in price validation: {}", e.getMessage(), e);
            result.setSumCalculationCorrect(false);
            result.getErrors().add("ERROR: " + e.getMessage());
        }

        return result;
    }

    /**
     * Tries to find part by fuzzy matching (partial names).
     */
    private AutoPart findFuzzyMatch(String partName) {
        String searchTerm = partName.toLowerCase();

        for (Map.Entry<String, AutoPart> entry : PARTS_DATABASE.entrySet()) {
            String key = entry.getKey().toLowerCase();
            if (key.contains(searchTerm) || searchTerm.contains(key)) {
                return entry.getValue();
            }
        }

        return null;
    }

    /**
     * Extract price from line item description.
     * Patterns: "€100", "100€", "100,50€", "$100", "EUR 100"
     */
    public BigDecimal extractPriceFromDescription(String description) {
        if (description == null || description.isBlank()) {
            return null;
        }

        Pattern[] patterns = {
                Pattern.compile("€\\s*(\\d+[\\.\\,]?\\d*)"),
                Pattern.compile("(\\d+[\\.\\,]?\\d*)\\s*€"),
                Pattern.compile("\\$(\\d+[\\.\\,]?\\d*)"),
                Pattern.compile("EUR\\s*(\\d+[\\.\\,]?\\d*)")
        };

        for (Pattern pattern : patterns) {
            Matcher matcher = pattern.matcher(description);
            if (matcher.find()) {
                try {
                    String priceStr = matcher.group(1).replace(",", ".");
                    BigDecimal price = new BigDecimal(priceStr);
                    log.trace("Extracted €{} from: '{}'", price, description);
                    return price;
                } catch (NumberFormatException e) {
                    log.trace("Could not parse price from match: {}", matcher.group(1));
                }
            }
        }

        log.trace("No price found in description: '{}'", description);
        return null;
    }

    @Override
    public List<RedFlag> detectRedFlags(InvoiceData invoiceData) {
        List<RedFlag> flags = new ArrayList<>();

        try {
            if (invoiceData == null || invoiceData.getLineItems() == null) {
                return flags;
            }

            for (InvoiceData.LineItem item : invoiceData.getLineItems()) {

                if (item == null || item.getCategory() == null || item.getUnitPrice() == null) {
                    continue;
                }

                if (item.getCategory() == InvoiceData.LineItem.ItemCategory.PARTS) {
                    AutoPart part = PARTS_DATABASE.get(item.getDescription());

                    if (part == null) continue;

                    double chargedPrice = item.getUnitPrice().doubleValue();
                    double maxAllowed = part.maxPrice() * 1.5;

                    if (chargedPrice > maxAllowed) {
                        double overage = ((chargedPrice / part.maxPrice()) - 1.0) * 100;

                        RedFlag.Severity severity;
                        int scoreImpact;

                        if (overage > 200) {
                            severity = RedFlag.Severity.HIGH;
                            scoreImpact = 20;
                        } else if (overage > 100) {
                            severity = RedFlag.Severity.MEDIUM;
                            scoreImpact = 12;
                        } else {
                            severity = RedFlag.Severity.LOW;
                            scoreImpact = 6;
                        }

                        flags.add(RedFlag.builder()
                                .code("OVERPRICED_PART")
                                .category(RedFlag.Category.PARTS)
                                .severity(severity)
                                .description(String.format(
                                        "Part price €%.2f exceeds standard €%.2f by %.0f%%",
                                        chargedPrice, part.maxPrice(), overage))
                                .evidence(String.format(
                                        "Part: %s | Charged: €%.2f | Max: €%.2f | Category: %s",
                                        item.getDescription(), chargedPrice,
                                        part.maxPrice(), part.category()))
                                .scoreImpact(scoreImpact)
                                .source(RedFlag.Source.RULE)
                                .layer("LAYER_4_PARTS_PRICE_VALIDATION")
                                .confidence(1.0)
                                .build());

                        log.warn("  ⚠️  OVERPRICED: {} €{} > max €{}",
                                item.getDescription(), chargedPrice, part.maxPrice());
                    }
                }
            }

        } catch (NullPointerException e) {
            log.error("Layer 4: NullPointerException: {}", e.getMessage(), e);
        } catch (Exception e) {
            log.error("Layer 4: Unexpected error: {}", e.getMessage(), e);
        }

        return flags;
    }

    @Override
    public int getRuleCount() {
        return PARTS_DATABASE.size();
    }

    @Override
    public String getVersion() {
        return "1.0.0-parts-price";
    }
}

// ============================================================================
// RECORD DEFINITIONS
// ============================================================================

/**
 * Represents an automotive part with price range.
 */
record AutoPart(
        double minPrice,
        double maxPrice,
        String category,
        String description
) {}