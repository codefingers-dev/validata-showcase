package de.codefingers.validata.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.Instant;
import java.util.Map;

/**
 * Globaler Exception Handler für die Validata API.
 *
 * Fängt alle Exceptions ab und gibt strukturierte JSON-Responses zurück.
 *
 * HTTP Status Mapping:
 * - InvalidInvoiceException     → 400 Bad Request
 * - TextractException           → 502 Bad Gateway
 * - AnalysisFailedException     → 500 Internal Server Error
 * - MaxUploadSizeExceeded       → 413 Payload Too Large
 * - IllegalArgumentException    → 400 Bad Request
 * - Exception                   → 500 Internal Server Error
 *
 * @see AnalysisFailedException
 * @see InvalidInvoiceException
 * @see TextractException
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ========================================================================
    // BUSINESS EXCEPTIONS (Validata-spezifisch)
    // ========================================================================

    @ExceptionHandler(InvalidInvoiceException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidInvoice(InvalidInvoiceException e) {
        log.warn("Ungültige Rechnung: {}", e.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, "INVALID_INVOICE", e.getMessage());
    }

    @ExceptionHandler(TextractExtractionException.class)
    public ResponseEntity<Map<String, Object>> handleTextractException(TextractExtractionException e) {
        log.error("Textract-Fehler: {}", e.getMessage());
        return buildResponse(HttpStatus.BAD_GATEWAY, "EXTRACTION_FAILED",
                "PDF-Verarbeitung fehlgeschlagen. Bitte erneut versuchen.");
    }

    @ExceptionHandler(AnalysisFailedException.class)
    public ResponseEntity<Map<String, Object>> handleAnalysisFailed(AnalysisFailedException e) {
        log.error("Analyse fehlgeschlagen: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                        "error", "ANALYSIS_FAILED",
                        "message", "Analyse konnte nicht abgeschlossen werden",
                        "reason", e.getFailureReason(),
                        "processingTimeMs", e.getProcessingTimeMs(),
                        "timestamp", Instant.now().toString()
                ));
    }

    // ========================================================================
    // FRAMEWORK EXCEPTIONS
    // ========================================================================

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleMaxSizeException(MaxUploadSizeExceededException e) {
        log.warn("Datei zu groß: {}", e.getMessage());
        return buildResponse(HttpStatus.PAYLOAD_TOO_LARGE, "FILE_TOO_LARGE",
                "Die hochgeladene Datei überschreitet das Maximum von 10MB");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("Ungültige Anfrage: {}", e.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", e.getMessage());
    }

    // ========================================================================
    // FALLBACK (Alles andere)
    // ========================================================================

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception e) {
        log.error("Unerwarteter Fehler: {}", e.getMessage(), e);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "Ein unerwarteter Fehler ist aufgetreten");
    }

    // ========================================================================
    // HELPER
    // ========================================================================

    private ResponseEntity<Map<String, Object>> buildResponse(
            HttpStatus status, String error, String message) {
        return ResponseEntity.status(status)
                .body(Map.of(
                        "error", error,
                        "message", message,
                        "status", status.value(),
                        "timestamp", Instant.now().toString()
                ));
    }
}