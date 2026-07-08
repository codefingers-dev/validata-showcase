package de.codefingers.validata.exception;

/**
 * Exception wird geworfen wenn Invoice ungültig ist.
 *
 * HTTP Status: 400 (Bad Request)
 *
 * Gründe:
 * - Erforderliche Felder fehlen (InvoiceNumber, Amount, etc.)
 * - Datentypen ungültig (z.B. negativer Betrag)
 * - Datum ungültig
 * - Fahrzeug-Daten fehlen
 *
 * @see GlobalExceptionHandler
 */
public class InvalidInvoiceException extends RuntimeException {

    private final String fieldName;
    private final String reason;

    public InvalidInvoiceException(String message) {
        this("UNKNOWN", message, null);
    }

    public InvalidInvoiceException(String message, Throwable cause) {
        this("UNKNOWN", message, cause);
    }

    public InvalidInvoiceException(String fieldName, String reason) {
        this(fieldName, reason, null);
    }

    public InvalidInvoiceException(String fieldName, String reason, Throwable cause) {
        super("Invalid field '" + fieldName + "': " + reason, cause);
        this.fieldName = fieldName;
        this.reason = reason;
    }

    public String getFieldName() {
        return fieldName;
    }

    public String getReason() {
        return reason;
    }
}