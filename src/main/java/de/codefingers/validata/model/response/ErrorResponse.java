package de.codefingers.validata.model.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Standard Error Response für alle API Fehler
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(
        name = "ErrorResponse",
        description = "Standard Error Response für API Fehler"
)
public class ErrorResponse {

    @Schema(
            description = "Eindeutige Analyse-ID für Error Tracking",
            example = "550e8400-e29b-41d4-a716-446655440000"
    )
    private String analysisId;

    @Schema(
            description = "Status der Antwort",
            example = "ERROR",
            allowableValues = {"ERROR", "FAILED"}
    )
    private String status;

    @Schema(
            description = "Error Code für Client-seitige Fehlerbehandlung",
            example = "INVALID_ARGUMENT",
            allowableValues = {
                    "INVALID_ARGUMENT",
                    "MISSING_FILE",
                    "UNSUPPORTED_FORMAT",
                    "INTERNAL_SERVER_ERROR",
                    "ANALYSIS_FAILED"
            }
    )
    private String errorCode;

    @Schema(
            description = "Human-readable Fehlernachricht",
            example = "Ungültige Eingabedaten: workshopName ist erforderlich"
    )
    private String message;

    @Schema(
            description = "Zusätzliche Details (optional)",
            example = "Field 'workshopName' failed validation"
    )
    private String details;
}