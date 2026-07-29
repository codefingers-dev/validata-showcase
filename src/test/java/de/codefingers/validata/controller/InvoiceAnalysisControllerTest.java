package de.codefingers.validata.controller;


import com.fasterxml.jackson.databind.ObjectMapper;
import de.codefingers.validata.model.domain.FraudAnalysisResult;
import de.codefingers.validata.model.domain.RedFlag;
import de.codefingers.validata.model.dto.InvoiceData;
import de.codefingers.validata.service.FraudDetectionOrchestrator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Web-Layer Tests für InvoiceAnalysisController.
 *
 * @WebMvcTest lädt nur die Web-Schicht (kein voller Context, keine DB).
 * Der Orchestrator wird als @MockBean bereitgestellt.
 */
@WebMvcTest(InvoiceAnalysisController.class)
@DisplayName("InvoiceAnalysisController - Web Layer")
class InvoiceAnalysisControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private FraudDetectionOrchestrator orchestrator;

    // ========================================================================
    // Health Check
    // ========================================================================

    @Test
    @DisplayName("GET /health → 200 OK")
    void healthCheck_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/invoices/health"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("running")));
    }

    // ========================================================================
    // JSON Endpoint
    // ========================================================================

    @Test
    @DisplayName("POST /analyze/json → 200 mit Risk Score")
    void analyzeJson_validData_returns200WithScore() throws Exception {
        FraudAnalysisResult mockResult = buildMockResult(85, "CRITICAL");
        when(orchestrator.analyzeExtracted(any(InvoiceData.class))).thenReturn(mockResult);

        InvoiceData input = InvoiceData.builder()
                .invoiceNumber("RE-2025-TEST")
                .grossAmount(BigDecimal.valueOf(1500))
                .build();

        mockMvc.perform(post("/api/v1/invoices/analyze/json")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.riskScore").value(85))
                .andExpect(jsonPath("$.riskLevel").value("CRITICAL"));
    }

    @Test
    @DisplayName("POST /analyze/json → 200 mit Red Flags")
    void analyzeJson_withFlags_returnsFlags() throws Exception {
        FraudAnalysisResult mockResult = buildMockResult(85, "CRITICAL");
        when(orchestrator.analyzeExtracted(any(InvoiceData.class))).thenReturn(mockResult);

        InvoiceData input = InvoiceData.builder()
                .invoiceNumber("RE-2025-TEST")
                .grossAmount(BigDecimal.valueOf(1500))
                .build();

        mockMvc.perform(post("/api/v1/invoices/analyze/json")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.redFlags").isArray())
                .andExpect(jsonPath("$.redFlags[0].code").value("EXCESSIVE_LABOR_TIME"));
    }

    @Test
    @DisplayName("POST /analyze/json ohne Body → 400")
    void analyzeJson_noBody_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/invoices/analyze/json")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(""))
                .andExpect(status().isBadRequest());
    }

    // ========================================================================
    // File Upload Endpoint
    // ========================================================================

    @Test
    @DisplayName("POST /analyze mit Datei → 200")
    void analyzeFile_validFile_returns200() throws Exception {
        FraudAnalysisResult mockResult = buildMockResult(25, "GREEN");
        when(orchestrator.analyze(any())).thenReturn(mockResult);

        MockMultipartFile file = new MockMultipartFile(
                "file", "invoice.pdf", "application/pdf",
                "dummy pdf content".getBytes());

        mockMvc.perform(multipart("/api/v1/invoices/analyze").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.riskScore").value(25))
                .andExpect(jsonPath("$.riskLevel").value("GREEN"));
    }

    @Test
    @DisplayName("POST /analyze mit leerer Datei → 400")
    void analyzeFile_emptyFile_returns400() throws Exception {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file", "empty.pdf", "application/pdf", new byte[0]);

        mockMvc.perform(multipart("/api/v1/invoices/analyze").file(emptyFile))
                .andExpect(status().isBadRequest());
    }

    // ========================================================================
    // HELPER
    // ========================================================================

    private FraudAnalysisResult buildMockResult(int score, String level) {
        RedFlag flag = RedFlag.builder()
                .code("EXCESSIVE_LABOR_TIME")
                .category(RedFlag.Category.LABOR)
                .severity(RedFlag.Severity.HIGH)
                .scoreImpact(25)
                .source(RedFlag.Source.RULE)
                .build();

        return FraudAnalysisResult.builder()
                .riskScore(score)
                .riskLevel(level)
                .recommendation(level.equals("GREEN") ? "AUTO_APPROVE" : "REJECT_AND_FLAG")
                .redFlags(level.equals("GREEN") ? List.of() : List.of(flag))
                .extractedData(InvoiceData.builder().invoiceNumber("RE-2025-TEST").build())
                .summary("Test summary")
                .confidence(0.9)
                .engineUsed("Pure Rules-Based (v2.0)")
                .analysisMode(de.codefingers.validata.model.domain.AnalysisMode.RULES_ONLY)
                .analyzedAt(Instant.now())
                .build();
    }
}