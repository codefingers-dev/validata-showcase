package de.codefingers.validata.service.extraction;


import de.codefingers.validata.exception.AnalysisFailedException;
import de.codefingers.validata.exception.TextractExtractionException;
import de.codefingers.validata.model.dto.InvoiceData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.services.textract.TextractClient;
import software.amazon.awssdk.services.textract.model.*;
import software.amazon.awssdk.core.SdkBytes;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.util.*;
import java.util.regex.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@Profile("aws")
public class TextractExtractionService implements ExtractionService {

    private final TextractClient textractClient;
    private final InvoiceTextParser textParser;

    public TextractExtractionService(InvoiceTextParser textParser) {
        this.textractClient = TextractClient.builder()
                .region(software.amazon.awssdk.regions.Region.EU_CENTRAL_1)
                .build();
        this.textParser = textParser;
    }

    @Override
    public InvoiceData extract(MultipartFile file) throws Exception {
        byte[] fileBytes = file.getBytes();

        try {
            // Pass 1: analyzeDocument
            InvoiceData result = tryAnalyzeDocument(fileBytes);
            if (hasRequiredFields(result)) return result;

            // Pass 2: detectDocumentText
            result = tryDetectDocumentText(fileBytes);
            if (hasRequiredFields(result)) return result;

            // Pass 3: analyzeExpense
            result = tryAnalyzeExpense(fileBytes);
            if (hasRequiredFields(result)) return result;

            throw new TextractExtractionException("Alle Textract-Methoden fehlgeschlagen");

        } catch (TextractException e) {
            throw e;
        } catch (Exception e) {
            throw new TextractExtractionException("TEXTRACT_ERROR", e.getMessage(), e);
        }
    }

    private InvoiceData tryAnalyzeDocument(byte[] fileBytes) {
        try {
            AnalyzeDocumentResponse response = textractClient.analyzeDocument(
                    AnalyzeDocumentRequest.builder()
                            .document(Document.builder()
                                    .bytes(SdkBytes.fromByteArray(fileBytes))
                                    .build())
                            .featureTypes(FeatureType.TABLES, FeatureType.FORMS)
                            .build());

            return textParser.parseFromBlocks(response.blocks());

        } catch (Exception e) {
            log.warn("analyzeDocument fehlgeschlagen: {}", e.getMessage());
            return null;
        }
    }

    private InvoiceData tryDetectDocumentText(byte[] fileBytes) {
        try {
            DetectDocumentTextResponse response = textractClient.detectDocumentText(
                    DetectDocumentTextRequest.builder()
                            .document(Document.builder()
                                    .bytes(SdkBytes.fromByteArray(fileBytes))
                                    .build())
                            .build());

            return textParser.parseFromTextBlocks(response.blocks());

        } catch (Exception e) {
            log.warn("detectDocumentText fehlgeschlagen: {}", e.getMessage());
            return null;
        }
    }

    private InvoiceData tryAnalyzeExpense(byte[] fileBytes) {
        try {
            AnalyzeExpenseResponse response = textractClient.analyzeExpense(
                    AnalyzeExpenseRequest.builder()
                            .document(Document.builder()
                                    .bytes(SdkBytes.fromByteArray(fileBytes))
                                    .build())
                            .build());

            return textParser.parseFromExpense(response.expenseDocuments());

        } catch (Exception e) {
            log.warn("analyzeExpense fehlgeschlagen: {}", e.getMessage());
            return null;
        }
    }

    private boolean hasRequiredFields(InvoiceData invoice) {
        return invoice != null
                && invoice.getGrossAmount() != null
                && invoice.getGrossAmount().compareTo(BigDecimal.ZERO) > 0;
    }
}