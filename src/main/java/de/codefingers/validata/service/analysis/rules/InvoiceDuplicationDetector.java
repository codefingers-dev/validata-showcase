package de.codefingers.validata.service.analysis.rules;

import de.codefingers.validata.model.domain.RedFlag;
import de.codefingers.validata.model.dto.InvoiceData;
import de.codefingers.validata.service.provider.InvoiceDataProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * LAYER 6: Invoice Duplication Detection
 *
 * Erkennt 4 Patterns von Rechnungs-Duplikaten:
 *
 * 1. EXACT_DUPLICATE: SHA-256 Hash identisch (+30 Punkte)
 * 2. PARTIAL_DUPLICATE: >95% Ähnlichkeit (+25 Punkte)
 * 3. SERIAL_DUPLICATE: 3+ ähnliche in 14 Tagen (+15 Punkte)
 * 4. AMOUNT_CLONE: Gleicher Betrag auf verschiedenen Fahrzeugen (+10 Punkte)
 *
 * ← FIX: Jetzt mit Clean Code Pattern (keine Side Effects!)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceDuplicationDetector implements DuplicationDetectorService {

    private final InvoiceDataProvider invoiceDataProvider;
    private final InvoiceHasher invoiceHasher;
    private final InvoiceSimilarityCalculator similarityCalculator;

    /**
     * ← FIX: Neue Hauptmethode (Clean Code!)
     *
     * Gibt DuplicationCheckResult zurück (keine Side Effects)
     * Orchestrator entscheidet was zu tun ist
     *
     * @param currentInvoice Die zu prüfende Rechnung
     * @return DuplicationCheckResult mit allen Infos
     */
    public DuplicationCheckResult detectDuplication(InvoiceData currentInvoice) {

        try {
            // Validiere Input
            if (!isValidInvoice(currentInvoice)) {
                log.debug("Layer 6: Skipping duplication detection - missing critical fields");
                return DuplicationCheckResult.noMatch();
            }

            log.info("Layer 6: Checking duplication for invoice: {}",
                    currentInvoice.getInvoiceNumber());

            List<InvoiceData> allInvoices = invoiceDataProvider.getAllInvoices();

            // Prüfe alle 4 Pattern
            DuplicationCheckResult exactMatch = checkExactDuplicates(currentInvoice, allInvoices);
            if (exactMatch.isDuplicate()) return exactMatch;  // Early exit!

            DuplicationCheckResult partialMatch = checkPartialDuplicates(currentInvoice, allInvoices);
            if (partialMatch.isDuplicate()) return partialMatch;  // Early exit!

            DuplicationCheckResult serialMatch = checkSerialDuplicates(currentInvoice, allInvoices);
            if (serialMatch.isDuplicate()) return serialMatch;  // Early exit!

            DuplicationCheckResult amountMatch = checkAmountClones(currentInvoice, allInvoices);
            if (amountMatch.isDuplicate()) return amountMatch;  // Early exit!

            // ✓ Kein Duplikat gefunden
            log.debug("✓ Kein Duplikat gefunden für {}", currentInvoice.getInvoiceNumber());
            return DuplicationCheckResult.noMatch();

        } catch (NullPointerException e) {
            log.error("Layer 6: NullPointerException: {}", e.getMessage(), e);
            return DuplicationCheckResult.noMatch();  // Graceful degradation
        } catch (Exception e) {
            log.error("Layer 6: Unexpected error: {}", e.getMessage(), e);
            return DuplicationCheckResult.noMatch();  // Graceful degradation
        }
    }

    /**
     * ← FIX: Validiere kritische Felder
     */
    private boolean isValidInvoice(InvoiceData invoice) {
        return invoice != null &&
                invoice.getInvoiceNumber() != null &&
                invoice.getLicensePlate() != null &&
                invoice.getGrossAmount() != null &&
                invoice.getInvoiceDate() != null;
    }

    // ===== PATTERN 1: EXACT_DUPLICATE =====

    /**
     * ← FIX: Gibt DuplicationCheckResult zurück (nicht void)
     */
    private DuplicationCheckResult checkExactDuplicates(InvoiceData currentInvoice,
                                                        List<InvoiceData> allInvoices) {
        String currentHash = invoiceHasher.calculateHash(currentInvoice);

        for (InvoiceData existingInvoice : allInvoices) {
            // Skip: Nicht gegen sich selbst prüfen
            if (currentInvoice.getInvoiceNumber().equals(existingInvoice.getInvoiceNumber())) {
                continue;
            }

            String existingHash = invoiceHasher.calculateHash(existingInvoice);

            if (currentHash.equals(existingHash)) {
                log.warn("⚠️  EXACT_DUPLICATE detected: {} == {}",
                        currentInvoice.getInvoiceNumber(),
                        existingInvoice.getInvoiceNumber());

                RedFlag flag = RedFlag.duplicateHigh(  // ← FIX: Nutze Helper Method!
                        "EXACT_DUPLICATE",
                        "Identische Rechnung erneut eingereicht",
                        String.format("Rechnung %s vom %s ist identisch mit %s vom %s",
                                existingInvoice.getInvoiceNumber(),
                                existingInvoice.getInvoiceDate(),
                                currentInvoice.getInvoiceNumber(),
                                currentInvoice.getInvoiceDate()),
                        30
                );

                return DuplicationCheckResult.builder()
                        .duplicate(true)
                        .type("EXACT_DUPLICATE")
                        .points(30)
                        .redFlags(List.of(flag))
                        .confidence(1.0)
                        .build();
            }
        }

        return DuplicationCheckResult.noMatch();
    }

    // ===== PATTERN 2: PARTIAL_DUPLICATE =====

    private DuplicationCheckResult checkPartialDuplicates(InvoiceData currentInvoice,
                                                          List<InvoiceData> allInvoices) {
        final double SIMILARITY_THRESHOLD = 0.95;

        for (InvoiceData existingInvoice : allInvoices) {
            if (currentInvoice.getInvoiceNumber().equals(existingInvoice.getInvoiceNumber())) {
                continue;
            }

            double similarity = similarityCalculator.calculateSimilarity(currentInvoice, existingInvoice);

            if (similarity >= SIMILARITY_THRESHOLD) {
                log.warn("⚠️  PARTIAL_DUPLICATE detected: {} ~= {} ({}%)",
                        currentInvoice.getInvoiceNumber(),
                        existingInvoice.getInvoiceNumber(),
                        Math.round(similarity * 100));

                RedFlag flag = RedFlag.duplicateMedium(  // ← FIX: Nutze Helper Method!
                        "PARTIAL_DUPLICATE",
                        String.format("%.0f%% ähnlich zu Rechnung %s",
                                similarity * 100,
                                existingInvoice.getInvoiceNumber()),
                        String.format("Rechnungen %s und %s sind zu %.0f%% identisch",
                                currentInvoice.getInvoiceNumber(),
                                existingInvoice.getInvoiceNumber(),
                                similarity * 100),
                        25
                );

                return DuplicationCheckResult.builder()
                        .duplicate(true)
                        .type("PARTIAL_DUPLICATE")
                        .points(25)
                        .redFlags(List.of(flag))
                        .confidence(similarity)
                        .build();
            }
        }

        return DuplicationCheckResult.noMatch();
    }

    // ===== PATTERN 3: SERIAL_DUPLICATE =====

    private DuplicationCheckResult checkSerialDuplicates(InvoiceData currentInvoice,
                                                         List<InvoiceData> allInvoices) {
        final double SIMILARITY_THRESHOLD = 0.80;
        final int TIME_WINDOW_DAYS = 14;
        final int MIN_COUNT = 3;

        List<InvoiceData> similarAndRecent = allInvoices.stream()
                .filter(inv -> {
                    if (!inv.getLicensePlate().equals(currentInvoice.getLicensePlate())) {
                        return false;
                    }

                    double similarity = similarityCalculator.calculateSimilarity(currentInvoice, inv);
                    if (similarity < SIMILARITY_THRESHOLD) {
                        return false;
                    }

                    long daysBetween = Math.abs(ChronoUnit.DAYS.between(
                            currentInvoice.getInvoiceDate(),
                            inv.getInvoiceDate()
                    ));

                    return daysBetween <= TIME_WINDOW_DAYS;
                })
                .toList();

        if (similarAndRecent.size() >= MIN_COUNT) {
            log.warn("⚠️  SERIAL_DUPLICATE detected: {} similar invoices for {} in {} days",
                    similarAndRecent.size(),
                    currentInvoice.getLicensePlate(),
                    TIME_WINDOW_DAYS);

            String invoiceList = similarAndRecent.stream()
                    .map(inv -> String.format("%s (%s)",
                            inv.getInvoiceNumber(),
                            inv.getInvoiceDate()))
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("");

            long daySpan = Math.max(1, similarAndRecent.stream()
                    .mapToLong(inv -> ChronoUnit.DAYS.between(
                            currentInvoice.getInvoiceDate(),
                            inv.getInvoiceDate()))
                    .max()
                    .orElse(1));

            RedFlag flag = RedFlag.duplicateMedium(  // ← FIX: Nutze Helper Method!
                    "SERIAL_DUPLICATE",
                    String.format("%d ähnliche Rechnungen in %d Tagen",
                            similarAndRecent.size(),
                            daySpan),
                    String.format("Fahrzeug %s hat %d Rechnungen eingereicht: %s",
                            currentInvoice.getLicensePlate(),
                            similarAndRecent.size(),
                            invoiceList),
                    15
            );

            return DuplicationCheckResult.builder()
                    .duplicate(true)
                    .type("SERIAL_DUPLICATE")
                    .points(15)
                    .redFlags(List.of(flag))
                    .confidence(1.0)
                    .build();
        }

        return DuplicationCheckResult.noMatch();
    }

    // ===== PATTERN 4: AMOUNT_CLONE =====

    private DuplicationCheckResult checkAmountClones(InvoiceData currentInvoice,
                                                     List<InvoiceData> allInvoices) {

        List<InvoiceData> sameAmountDifferentVehicles = allInvoices.stream()
                .filter(inv -> {
                    if (inv.getLicensePlate().equals(currentInvoice.getLicensePlate())) {
                        return false;
                    }

                    BigDecimal currentAmount = currentInvoice.getGrossAmount();
                    BigDecimal existingAmount = inv.getGrossAmount();

                    if (currentAmount == null || existingAmount == null) {
                        return false;
                    }

                    return currentAmount.compareTo(existingAmount) == 0;
                })
                .toList();

        if (sameAmountDifferentVehicles.size() >= 2) {
            log.warn("⚠️  AMOUNT_CLONE detected: Amount {} on {} different vehicles",
                    currentInvoice.getGrossAmount(),
                    sameAmountDifferentVehicles.size() + 1);

            StringBuilder licensePlateList = new StringBuilder();
            licensePlateList.append(currentInvoice.getLicensePlate());
            for (InvoiceData inv : sameAmountDifferentVehicles) {
                licensePlateList.append(", ").append(inv.getLicensePlate());
            }

            RedFlag flag = RedFlag.duplicateMedium(  // ← FIX: Nutze Helper Method!
                    "AMOUNT_CLONE",
                    String.format("€%.2f auf %d verschiedenen Fahrzeugen",
                            currentInvoice.getGrossAmount(),
                            sameAmountDifferentVehicles.size() + 1),
                    String.format("Identischer Betrag €%.2f eingereicht für: %s",
                            currentInvoice.getGrossAmount(),
                            licensePlateList.toString()),
                    10
            );

            return DuplicationCheckResult.builder()
                    .duplicate(true)
                    .type("AMOUNT_CLONE")
                    .points(10)
                    .redFlags(List.of(flag))
                    .confidence(1.0)
                    .build();
        }

        return DuplicationCheckResult.noMatch();
    }
}