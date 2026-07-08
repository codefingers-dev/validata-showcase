package de.codefingers.validata.exception;

/**
 * Exception wird geworfen wenn Analyse komplett fehlgeschlagen ist.
 *
 * HTTP Status: 500 (Internal Server Error)
 *
 * Gründe:
 * - Unerwarteter Fehler
 * - Database Connection Error
 * - Rule Engine Crash
 * - Runtime Exception
 *
 * @see GlobalExceptionHandler
 */
public class AnalysisFailedException extends RuntimeException {

    private final String failureReason;
    private final long processingTimeMs;

    public AnalysisFailedException(String message) {
        this(message, null, 0);
    }

    public AnalysisFailedException(String message, Throwable cause) {
        this(message, cause, 0);
    }

    public AnalysisFailedException(String message, Throwable cause, long processingTimeMs) {
        super(message, cause);
        this.failureReason = message;
        this.processingTimeMs = processingTimeMs;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public long getProcessingTimeMs() {
        return processingTimeMs;
    }
}