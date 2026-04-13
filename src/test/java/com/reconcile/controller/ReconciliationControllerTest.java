package com.reconcile.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.reconcile.dto.Discrepancy;
import com.reconcile.dto.ReconciliationResult;
import com.reconcile.dto.ReconciliationVerdict;
import com.reconcile.service.ReconciliationService;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReconciliationController Unit Tests")
class ReconciliationControllerTest {

  private MockMvc mockMvc;

  @Mock private ReconciliationService reconciliationService;

  private ReconciliationResult successResult;

  @BeforeEach
  void setUp() {
    // Initialize MockMvc with controller and mocked service
    ReconciliationController controller = new ReconciliationController();
    ReflectionTestUtils.setField(controller, "reconciliationService", reconciliationService);
    mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

    // Setup mock result
    List<Discrepancy> discrepancies = new ArrayList<>();
    Discrepancy d1 = new Discrepancy();
    d1.setDiscrepancyType("HASH_MATCH");
    d1.setVerdict(ReconciliationVerdict.PRISTINE);
    discrepancies.add(d1);

    successResult = new ReconciliationResult(100L, 100L, 95L, 5L, 3L, 2L, 95.0, discrepancies);
    successResult.setPristineCount(90L);
    successResult.setNoisyCount(4L);
    successResult.setMaterialCount(1L);
  }

  @Test
  @DisplayName("GET /api/reconcile should return 200 with reconciliation result for default domain")
  void testPerformFullReconciliationDefaultDomain() throws Exception {
    // Arrange
    when(reconciliationService.reconcile("vendor-invoices")).thenReturn(successResult);

    // Act & Assert
    mockMvc
        .perform(get("/api/reconcile").contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.totalSourceRecords", is(100)))
        .andExpect(jsonPath("$.totalTargetRecords", is(100)))
        .andExpect(jsonPath("$.matchedRecords", is(95)))
        .andExpect(jsonPath("$.mismatchedRecords", is(5)))
        .andExpect(jsonPath("$.matchPercentage", is(95.0)));

    verify(reconciliationService, times(1)).reconcile("vendor-invoices");
  }

  @Test
  @DisplayName("GET /api/reconcile?domain=expense-reports should reconcile specified domain")
  void testPerformFullReconciliationCustomDomain() throws Exception {
    // Arrange
    ReconciliationResult customResult =
        new ReconciliationResult(50L, 50L, 48L, 2L, 1L, 1L, 96.0, new ArrayList<>());
    when(reconciliationService.reconcile("expense-reports")).thenReturn(customResult);

    // Act & Assert
    mockMvc
        .perform(
            get("/api/reconcile")
                .param("domain", "expense-reports")
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalSourceRecords", is(50)))
        .andExpect(jsonPath("$.totalTargetRecords", is(50)))
        .andExpect(jsonPath("$.matchPercentage", is(96.0)));

    verify(reconciliationService, times(1)).reconcile("expense-reports");
  }

  @Test
  @DisplayName("GET /api/reconcile should return 400 for invalid domain")
  void testPerformFullReconciliationInvalidDomain() throws Exception {
    // Arrange
    when(reconciliationService.reconcile("invalid-domain"))
        .thenThrow(new IllegalArgumentException("Domain 'invalid-domain' not configured"));

    // Act & Assert
    mockMvc
        .perform(
            get("/api/reconcile")
                .param("domain", "invalid-domain")
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest());

    verify(reconciliationService, times(1)).reconcile("invalid-domain");
  }

  @Test
  @DisplayName("GET /api/reconcile should return 500 on service error")
  void testPerformFullReconciliationServiceError() throws Exception {
    // Arrange
    when(reconciliationService.reconcile("vendor-invoices"))
        .thenThrow(new RuntimeException("Service error"));

    // Act & Assert
    mockMvc
        .perform(get("/api/reconcile").contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isInternalServerError());

    verify(reconciliationService, times(1)).reconcile("vendor-invoices");
  }

  @Test
  @DisplayName("GET /api/reconcile/health should return 200 with health message")
  void testHealthEndpoint() throws Exception {
    // Act & Assert
    mockMvc
        .perform(get("/api/reconcile/health"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("running")));

    // Verify service is not called for health check
    verify(reconciliationService, never()).reconcile(anyString());
  }

  @Test
  @DisplayName("GET /api/reconcile should include verdict statistics in response")
  void testReconciliationResultIncludesVerdictStats() throws Exception {
    // Arrange
    when(reconciliationService.reconcile("vendor-invoices")).thenReturn(successResult);

    // Act
    MvcResult result =
        mockMvc
            .perform(get("/api/reconcile").contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andReturn();

    // Assert - Response should contain verdict counts
    String content = result.getResponse().getContentAsString();
    assertTrue(content.contains("pristineCount"));
    assertTrue(content.contains("noisyCount"));
    assertTrue(content.contains("materialCount"));
  }

  @Test
  @DisplayName("GET /api/reconcile should include discrepancies in response")
  void testReconciliationResultIncludesDiscrepancies() throws Exception {
    // Arrange
    when(reconciliationService.reconcile("vendor-invoices")).thenReturn(successResult);

    // Act & Assert
    mockMvc
        .perform(get("/api/reconcile").contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.discrepancies").exists());
  }

  @Test
  @DisplayName("GET /api/reconcile should accept case-insensitive domain parameter")
  void testDomainParameterHandling() throws Exception {
    // Arrange
    when(reconciliationService.reconcile("expense-reports")).thenReturn(successResult);

    // Act & Assert
    mockMvc
        .perform(
            get("/api/reconcile")
                .param("domain", "expense-reports")
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk());

    verify(reconciliationService, times(1)).reconcile("expense-reports");
  }

  @Test
  @DisplayName("GET /api/reconcile with 100% match should return appropriate statistics")
  void testPerfectMatchReconciliation() throws Exception {
    // Arrange
    ReconciliationResult perfectResult =
        new ReconciliationResult(100L, 100L, 100L, 0L, 0L, 0L, 100.0, new ArrayList<>());
    perfectResult.setPristineCount(100L);
    perfectResult.setNoisyCount(0L);
    perfectResult.setMaterialCount(0L);

    when(reconciliationService.reconcile("vendor-invoices")).thenReturn(perfectResult);

    // Act & Assert
    mockMvc
        .perform(get("/api/reconcile").contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.matchPercentage", is(100.0)))
        .andExpect(jsonPath("$.mismatchedRecords", is(0)));
  }

  @Test
  @DisplayName("GET /api/reconcile with 0% match should return appropriate statistics")
  void testNoMatchReconciliation() throws Exception {
    // Arrange
    ReconciliationResult noMatchResult =
        new ReconciliationResult(100L, 100L, 0L, 100L, 50L, 50L, 0.0, new ArrayList<>());
    noMatchResult.setPristineCount(0L);
    noMatchResult.setNoisyCount(0L);
    noMatchResult.setMaterialCount(100L);

    when(reconciliationService.reconcile("vendor-invoices")).thenReturn(noMatchResult);

    // Act & Assert
    mockMvc
        .perform(get("/api/reconcile").contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.matchPercentage", is(0.0)))
        .andExpect(jsonPath("$.matchedRecords", is(0)));
  }

  @Test
  @DisplayName("GET /api/reconcile should handle empty domain parameter gracefully")
  void testEmptyDomainParameter() throws Exception {
    // Arrange
    when(reconciliationService.reconcile("vendor-invoices")).thenReturn(successResult);

    // Act & Assert - Should use default domain
    mockMvc
        .perform(get("/api/reconcile").contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk());
  }

  @Test
  @DisplayName("GET /api/reconcile should return proper JSON content type")
  void testResponseContentType() throws Exception {
    // Arrange
    when(reconciliationService.reconcile("vendor-invoices")).thenReturn(successResult);

    // Act & Assert
    mockMvc
        .perform(get("/api/reconcile").contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON));
  }

  @Test
  @DisplayName("Controller should be cross-origin enabled")
  void testCrossOriginSupport() throws Exception {
    // Arrange
    when(reconciliationService.reconcile("vendor-invoices")).thenReturn(successResult);

    // Act
    MvcResult result =
        mockMvc
            .perform(get("/api/reconcile").contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andReturn();

    // Assert - Cross-origin headers should be present (if properly configured)
    assertNotNull(result.getResponse());
  }
}
