package de.codefingers.validata.controller;

import de.codefingers.validata.model.domain.FraudAnalysisResult;
import de.codefingers.validata.model.dto.InvoiceData;
import de.codefingers.validata.model.response.InvoiceAnalysisResponse;
import de.codefingers.validata.service.FraudDetectionOrchestrator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import de.codefingers.validata.model.response.ErrorResponse;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


import java.util.UUID;

/**
 * REST API für die Kfz-Werkstattrechnungs-Betrugserkennungs-API.
 *
 * Bietet zwei Analyse-Modi:
 * 1. File Upload Mode: PDF/Bild wird hochgeladen, OCR + Analyse
 * 2. JSON Mode: Bereits extrahierte Daten werden direkt analysiert
 */
@Slf4j
@RestController
@Tag(name = "Invoice Analysis v1")
@RequestMapping("/api/v1/invoices")
@RequiredArgsConstructor
@Tag(
        name = "Invoice Analysis",
        description = "Kfz-Werkstattrechnungs-Betrugserkennungs-API\n\n" +
                "Analysiert Werkstattabrechnungen auf Betrugsmerkmale mit 6 Detection Layers:\n" +
                "- Layer 1: Bedrock AI (Generalist Analysis)\n" +
                "- Layer 2: Hallucination Removal\n" +
                "- Layer 3: Labor Price Validation\n" +
                "- Layer 4: Parts Price Validation\n" +
                "- Layer 5: Vehicle History Check\n" +
                "- Layer 6: Duplication Detection"
)
public class InvoiceAnalysisController {

    private final FraudDetectionOrchestrator orchestrator;

    // ===== ENDPOINT 1: FILE UPLOAD MODE =====

    @PostMapping(value = "/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Analysiere Werkstattrechnung (File Upload)",
            description = "Lädt eine Werkstattrechnung hoch (PDF/Bild), extrahiert Daten mit OCR/Bedrock, " +
                    "und führt 6-Layer Betrugsanalyse durch.\n\n" +
                    "**Workflow:**\n" +
                    "1. File wird hochgeladen (PDF, PNG, JPEG)\n" +
                    "2. AWS Textract extrahiert Text\n" +
                    "3. Bedrock AI analysiert Inhalt (Layer 1)\n" +
                    "4. 5 weitere Validierungsschichten\n" +
                    "5. Risk Score 0-100 wird berechnet\n" +
                    "6. Handlungsempfehlung wird gegeben",
            tags = {"Invoice Analysis"}
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Analyse erfolgreich - Fraud Analysis Result zurückgegeben",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = InvoiceAnalysisResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Ungültiges Dokument - Datei ist leer oder falsches Format",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Server Error - Analysefehler (OCR, Bedrock, etc)",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    public ResponseEntity<InvoiceAnalysisResponse> analyzeInvoice(
            @Parameter(
                    name = "file",
                    description = "Werkstattrechnung als PDF oder Bild (PNG, JPEG)\n\n" +
                            "Unterstützte Formate:\n" +
                            "- PDF (Multi-page)\n" +
                            "- PNG (High resolution)\n" +
                            "- JPEG (RGB color)\n\n" +
                            "Max Dateigröße: 20 MB",
                    required = true,
                    content = @Content(mediaType = "application/octet-stream")
            )
            @RequestParam("file") MultipartFile file,

            @Parameter(
                    name = "includeExtractedData",
                    description = "Ob extrahierte Rechnungsdaten in der Response enthalten sein sollen\n\n" +
                            "- true: Volles InvoiceData Objekt in Response\n" +
                            "- false: Nur Risk Score + Red Flags (schneller)",
                    required = false,
                    example = "true"
            )
            @RequestParam(value = "includeExtractedData", defaultValue = "true") boolean includeExtractedData

    ) {

        String analysisId = UUID.randomUUID().toString();


            // NULL & EMPTY CHECK
            if (file == null || file.isEmpty()) {
                log.warn("analyzeInvoice - ID: {}: Datei ist leer", analysisId);
                return ResponseEntity.badRequest().build();
            }

            log.info("analyzeInvoice - ID: {}, File: {} ({} bytes)",
                    analysisId, file.getOriginalFilename(), file.getSize());

            FraudAnalysisResult result = orchestrator.analyze(file);
            InvoiceAnalysisResponse response = InvoiceAnalysisResponse.from(result, analysisId);

            log.info("analyzeInvoice - ID: {}: Analyse erfolgreich. Risk Score: {}",
                    analysisId, result.getRiskScore());

            return ResponseEntity.ok(response);

    }


    // ===== ENDPOINT 2: JSON MODE =====

    @PostMapping(value = "/analyze/json", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Analysiere bereits extrahierte Daten (JSON)",
            description = "Führt nur die Betrugsanalyse (Layers 1-6) auf bereits extrahierten Rechnungsdaten durch.\n\n" +
                    "**Use Cases:**\n" +
                    "- Daten wurden bereits von anderer Quelle extrahiert\n" +
                    "- Batch Processing von vielen Rechnungen\n" +
                    "- Schnellere Verarbeitung (kein OCR)\n\n" +
                    "**Erforderliche Felder:**\n" +
                    "- invoiceNumber: Eindeutige Rechnungsnummer\n" +
                    "- workshopName: Name der Werkstatt\n" +
                    "- grossAmount: Gesamtbetrag in EUR\n" +
                    "- licensePlate: Kfz-Kennzeichen (optional aber empfohlen)",
            tags = {"Invoice Analysis"}
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Analyse erfolgreich",
                    content = @Content(schema = @Schema(implementation = InvoiceAnalysisResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Ungültige/unvollständige Eingabedaten",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Server Error - Analysefehler",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<InvoiceAnalysisResponse> analyzeExtractedData(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Bereits extrahierte Rechnungsdaten",
                    required = true,
                    content = @Content(schema = @Schema(implementation = InvoiceData.class))
            )
            @RequestBody InvoiceData invoiceData
    ) {
        String analysisId = UUID.randomUUID().toString();

            // NULL CHECK
            if (invoiceData == null) {
                log.warn("analyzeExtractedData - ID: {}: InvoiceData ist null", analysisId);
                return ResponseEntity.badRequest().build();
            }

            log.info("analyzeExtractedData - ID: {}, Invoice: {}",
                    analysisId, invoiceData.getInvoiceNumber());

            FraudAnalysisResult result = orchestrator.analyzeExtracted(invoiceData);
            InvoiceAnalysisResponse response = InvoiceAnalysisResponse.from(result, analysisId);

            log.info("analyzeExtractedData - ID: {}: Analyse erfolgreich. Risk Score: {}",
                    analysisId, result.getRiskScore());

            return ResponseEntity.ok(response);

    }

    // ===== HEALTH CHECK =====

    @GetMapping("/health")
    @Operation(
            summary = "Health Check",
            description = "Prüft ob die API verfügbar und healthy ist",
            tags = {"System"}
    )
    @ApiResponse(
            responseCode = "200",
            description = "API ist healthy"
    )
    public ResponseEntity<String> healthCheck() {
        log.debug("Health check request");
        return ResponseEntity.ok("FraudLens API is running");
    }



// ===== EXCEPTION HANDLERS =====

    @ExceptionHandler(IllegalArgumentException.class)
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Ungültiges Argument"
    )
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException e) {
        String analysisId = UUID.randomUUID().toString();
        log.error("IllegalArgumentException - ID: {}: {}", analysisId, e.getMessage(), e);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.builder()
                        .analysisId(analysisId)
                        .status("ERROR")
                        .errorCode("INVALID_ARGUMENT")  // ✅ ErrorCode enum!
                        .message("Ungültiges Argument: " + e.getMessage())
                        .build());
    }

    @ExceptionHandler(Exception.class)
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "500",
            description = "Interner Serverfehler"
    )
    public ResponseEntity<ErrorResponse> handleGeneralException(Exception e) {
        String analysisId = UUID.randomUUID().toString();
        log.error("Unerwartete Exception - ID: {}: {}", analysisId, e.getMessage(), e);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.builder()
                        .analysisId(analysisId)
                        .status("ERROR")
                        .errorCode("INTERNAL_SERVER_ERROR")  // ✅ ErrorCode enum!
                        .message("Unerwarteter Fehler: " + e.getMessage())
                        .build());
    }
}
