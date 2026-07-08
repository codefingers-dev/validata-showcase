package de.codefingers.validata.service.extraction;

import de.codefingers.validata.model.dto.InvoiceData;
import org.springframework.web.multipart.MultipartFile;

/**
 * Service-Interface für die Dokumenten-Extraktion (OCR).
 * 
 * Implementierungen:
 * - TextractExtractionService (Profile: aws) - AWS Textract AnalyzeExpense
 * - MockExtractionService (Profile: local) - Mock für lokale Entwicklung
 */
public interface ExtractionService {
    
    /**
     * Extrahiert strukturierte Daten aus einem Werkstattrechnungs-Dokument.
     *
     * @param file Das hochgeladene Dokument (PDF, PNG, JPEG)
     * @return Extrahierte Rechnungsdaten
     * @throws RuntimeException bei Extraktionsfehlern
     */



    InvoiceData extract(MultipartFile file) throws Exception;
    /**
     * Gibt den Namen des Extraction-Providers zurück.
     */
    default String getProviderName() {
        return this.getClass().getSimpleName();
    }
}
