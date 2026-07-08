package de.codefingers.validata.model.error;

import lombok.Getter;

/**
 * Standardisierte Error Codes für API
 */
@Getter
public enum ErrorCode {
    INVALID_ARGUMENT("INVALID_ARGUMENT", "Ungültiges Argument", 400),
    MISSING_FILE("MISSING_FILE", "Datei ist erforderlich", 400),
    UNSUPPORTED_FORMAT("UNSUPPORTED_FORMAT", "Dateiformat wird nicht unterstützt", 400),
    EMPTY_FILE("EMPTY_FILE", "Datei ist leer", 400),
    FILE_TOO_LARGE("FILE_TOO_LARGE", "Datei ist zu groß (max 20MB)", 400),

    ANALYSIS_FAILED("ANALYSIS_FAILED", "Analysefehler während der Verarbeitung", 500),
    OCR_FAILED("OCR_FAILED", "OCR-Fehler beim Text-Extrahieren", 500),
    BEDROCK_ERROR("BEDROCK_ERROR", "AWS Bedrock Fehler", 500),
    DATABASE_ERROR("DATABASE_ERROR", "Datenbankfehler", 500),

    INTERNAL_SERVER_ERROR("INTERNAL_SERVER_ERROR", "Interner Serverfehler", 500),
    UNEXPECTED_ERROR("UNEXPECTED_ERROR", "Unerwarteter Fehler", 500);

    private final String code;
    private final String message;
    private final int httpStatus;

    ErrorCode(String code, String message, int httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }
}