package de.codefingers.validata.service.analysis.rules;

import de.codefingers.validata.model.dto.InvoiceData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Hash-Utility für Layer 6 Duplication Detection.
 *
 * Berechnet SHA-256 Hashes von Rechnungen für:
 * - Exakte Duplikat-Erkennung
 * - Deterministische Vergleiche
 */
@Slf4j
@Service
public class InvoiceHasher {

    private static final String ALGORITHM = "SHA-256";

    /**
     * Berechnet SHA-256 Hash einer Rechnung.
     *
     * Hash wird aus folgenden Feldern berechnet (deterministic):
     * - invoiceNumber
     * - invoiceDate
     * - licensePlate
     * - grossAmount
     * - workshopName
     *
     * Änderungen an diesen Feldern → anderer Hash
     */
    public String calculateHash(InvoiceData invoice) {
        try {
            // Erstelle String aus kritischen Feldern
            String hashInput = String.format(
                    "%s|%s|%s|%.2f|%s",
                    invoice.getInvoiceNumber(),
                    invoice.getInvoiceDate(),
                    invoice.getLicensePlate(),
                    invoice.getGrossAmount(),
                    invoice.getWorkshopName()
            );

            // Berechne SHA-256 Hash
            MessageDigest digest = MessageDigest.getInstance(ALGORITHM);
            byte[] hash = digest.digest(hashInput.getBytes(StandardCharsets.UTF_8));

            // Konvertiere zu Hex String
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            log.error("SHA-256 Algorithmus nicht verfügbar", e);
            throw new RuntimeException("Hash Berechnung fehlgeschlagen", e);
        }
    }

    /**
     * Konvertiert Bytes zu Hex String
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }

    /**
     * Vergleicht zwei Rechnungen auf exakte Identität
     */
    public boolean isExactDuplicate(InvoiceData inv1, InvoiceData inv2) {
        String hash1 = calculateHash(inv1);
        String hash2 = calculateHash(inv2);
        return hash1.equals(hash2);
    }
}
