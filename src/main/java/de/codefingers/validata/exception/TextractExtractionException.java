package de.codefingers.validata.exception;


/**
 * Exception wird geworfen wenn AWS Textract PDF nicht lesen kann.
 *
 * HTTP Status: 502 (Bad Gateway)
 *
 * Gründe:
 * - PDF ist beschädigt
 * - PDF ist zu groß
 * - Textract hat Timeout
 * - Textract Service nicht erreichbar
 * - OCR fehlgeschlagen
 *
 *
 * @see GlobalExceptionHandler
 */
public class TextractExtractionException extends RuntimeException {

    private final String errorCode;
    private final String awsMessage;

    public TextractExtractionException(String message) {
        this("TEXTRACT_UNKNOWN", message, null);
    }

    public TextractExtractionException(String message, Throwable cause) {
        this("TEXTRACT_UNKNOWN", message, cause);
    }

    public TextractExtractionException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.awsMessage = message;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getAwsMessage() {
        return awsMessage;
    }
}