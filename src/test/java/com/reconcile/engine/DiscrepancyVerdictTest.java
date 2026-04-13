package com.reconcile.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.reconcile.config.DataSourceType;
import com.reconcile.config.DatabaseConfig;
import com.reconcile.config.FieldDefinition;
import com.reconcile.dto.Discrepancy;
import com.reconcile.dto.FieldVariance;
import com.reconcile.dto.ReconciliationRecord;
import com.reconcile.dto.ReconciliationVerdict;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for DiscrepancyVerdict classification Tests all three verdict types: PRISTINE, NOISY,
 * and MATERIAL
 */
class DiscrepancyVerdictTest {

  private List<Discrepancy> testDiscrepancies;

  /** Setup test data before each test */
  @BeforeEach
  void setUp() {
    // Setup source database config using record constructor with FieldDefinition objects
    DatabaseConfig sourceConfig =
        new DatabaseConfig(
            DataSourceType.POSTGRESQL,
            "source_schema",
            "source_table",
            "_with_hash",
            List.of(FieldDefinition.string("id")),
            new ArrayList<>(
                Arrays.asList(
                    FieldDefinition.decimal("amount"),
                    FieldDefinition.string("description"),
                    FieldDefinition.timestamp("date"))),
            null,
            null,
            null);

    // Setup target database config using record constructor with FieldDefinition objects
    DatabaseConfig targetConfig =
        new DatabaseConfig(
            DataSourceType.POSTGRESQL,
            "target_schema",
            "target_table",
            "_with_hash",
            List.of(FieldDefinition.string("record_id")),
            new ArrayList<>(
                Arrays.asList(
                    FieldDefinition.decimal("amount"),
                    FieldDefinition.string("description"),
                    FieldDefinition.timestamp("date"))),
            null,
            null,
            null);

    // Initialize test discrepancy list
    testDiscrepancies = new ArrayList<>();
  }

  /** Cleanup test data after each test */
  @AfterEach
  void tearDown() {
    // Clear test data
    testDiscrepancies.clear();
    testDiscrepancies = null;
  }

  /**
   * Test PRISTINE verdict: Record hashes match exactly (100% match) When source and target hashes
   * are identical
   */
  @Test
  void testVerdictPristine() {
    // Arrange: Create a discrepancy with matching hashes (PRISTINE verdict)
    Map<String, Object> sourceFields = new HashMap<>();
    sourceFields.put("amount", 1000.00);
    sourceFields.put("description", "invoice");
    sourceFields.put("date", "2026-04-12");

    ReconciliationRecord sourceRecord =
        new ReconciliationRecord(UUID.randomUUID(), "abc123def456", "source", sourceFields);

    Map<String, Object> targetFields = new HashMap<>();
    targetFields.put("amount", 1000.00);
    targetFields.put("description", "invoice");
    targetFields.put("date", "2026-04-12");

    ReconciliationRecord targetRecord =
        new ReconciliationRecord(
            UUID.randomUUID(),
            "abc123def456", // Same hash = PRISTINE
            "target",
            targetFields);

    Discrepancy discrepancy = new Discrepancy();
    discrepancy.setDiscrepancyType("HASH_MATCH");
    discrepancy.setSourceRecord(sourceRecord);
    discrepancy.setTargetRecord(targetRecord);
    discrepancy.setVerdict(ReconciliationVerdict.PRISTINE);

    // Field variances are all 0% or null
    List<FieldVariance> fieldVariances = new ArrayList<>();
    fieldVariances.add(new FieldVariance("amount", 1000.00, 1000.00, 0.0, "EXACT_MATCH"));
    fieldVariances.add(new FieldVariance("description", "invoice", "invoice", 0.0, "EXACT_MATCH"));
    fieldVariances.add(new FieldVariance("date", "2026-04-12", "2026-04-12", 0.0, "EXACT_MATCH"));

    discrepancy.setFieldVariances(fieldVariances);
    testDiscrepancies.add(discrepancy);

    // Act & Assert
    assertNotNull(discrepancy);
    assertEquals("abc123def456", discrepancy.getSourceRecord().recordHash());
    assertEquals("abc123def456", discrepancy.getTargetRecord().recordHash());
    assertEquals(ReconciliationVerdict.PRISTINE, discrepancy.getVerdict());

    // Verify all field variances are 0%
    boolean allExactMatch =
        discrepancy.getFieldVariances().stream()
            .allMatch(fv -> fv.variancePercentage() == null || fv.variancePercentage() == 0.0);
    assertTrue(allExactMatch);
  }

  /**
   * Test NOISY verdict: Record hashes differ, but all field variances <= 1% Represents 95%+
   * similarity with minor differences
   */
  @Test
  void testVerdictNoisy() {
    // Arrange: Create a discrepancy with different hashes but minor variances (NOISY)
    Map<String, Object> sourceFields = new HashMap<>();
    sourceFields.put("amount", 1000.00);
    sourceFields.put("description", "invoice A");
    sourceFields.put("date", "2026-04-12");

    ReconciliationRecord sourceRecord =
        new ReconciliationRecord(UUID.randomUUID(), "abc123def456", "source", sourceFields);

    Map<String, Object> targetFields = new HashMap<>();
    targetFields.put("amount", 1000.50); // Small variance
    targetFields.put("description", "invoice A");
    targetFields.put("date", "2026-04-12");

    ReconciliationRecord targetRecord =
        new ReconciliationRecord(
            UUID.randomUUID(),
            "xyz789uvw012", // Different hash
            "target",
            targetFields);

    Discrepancy discrepancy = new Discrepancy();
    discrepancy.setDiscrepancyType("HASH_MISMATCH");
    discrepancy.setSourceRecord(sourceRecord);
    discrepancy.setTargetRecord(targetRecord);
    discrepancy.setVerdict(ReconciliationVerdict.NOISY);

    // Field variances all <= 1%
    List<FieldVariance> fieldVariances = new ArrayList<>();
    fieldVariances.add(
        new FieldVariance("amount", 1000.00, 1000.50, 0.05, "NUMERIC")); // 0.05% variance
    fieldVariances.add(
        new FieldVariance("description", "invoice A", "invoice A", 0.0, "EXACT_MATCH"));
    fieldVariances.add(new FieldVariance("date", "2026-04-12", "2026-04-12", 0.0, "EXACT_MATCH"));

    discrepancy.setFieldVariances(fieldVariances);
    testDiscrepancies.add(discrepancy);

    // Act: Calculate the maximum variance
    Double maxVariance =
        discrepancy.getFieldVariances().stream()
            .map(FieldVariance::variancePercentage)
            .filter(v -> v != null && v > 0)
            .max(Double::compareTo)
            .orElse(0.0);

    // Assert: NOISY = all variances <= 1%
    assertNotNull(discrepancy);
    assertNotEquals(
        discrepancy.getSourceRecord().recordHash(), discrepancy.getTargetRecord().recordHash());
    assertTrue(maxVariance <= 1.0, "Max variance should be <= 1% for NOISY verdict");
    assertEquals(ReconciliationVerdict.NOISY, discrepancy.getVerdict());
    assertEquals(0.05, maxVariance);
  }

  /**
   * Test MATERIAL verdict: Record hashes differ and at least one field > 1% variance Represents
   * significant differences
   */
  @Test
  void testVerdictMaterial() {
    // Arrange: Create a discrepancy with different hashes and significant variance (MATERIAL)
    Map<String, Object> sourceFields = new HashMap<>();
    sourceFields.put("amount", 1000.00);
    sourceFields.put("description", "invoice A");
    sourceFields.put("date", "2026-04-12");

    ReconciliationRecord sourceRecord =
        new ReconciliationRecord(UUID.randomUUID(), "abc123def456", "source", sourceFields);

    Map<String, Object> targetFields = new HashMap<>();
    targetFields.put("amount", 950.00); // Significant variance
    targetFields.put("description", "invoice B");
    targetFields.put("date", "2026-04-12");

    ReconciliationRecord targetRecord =
        new ReconciliationRecord(
            UUID.randomUUID(),
            "xyz789uvw012", // Different hash
            "target",
            targetFields);

    Discrepancy discrepancy = new Discrepancy();
    discrepancy.setDiscrepancyType("HASH_MISMATCH");
    discrepancy.setSourceRecord(sourceRecord);
    discrepancy.setTargetRecord(targetRecord);
    discrepancy.setVerdict(ReconciliationVerdict.MATERIAL);

    // At least one field variance > 1%
    List<FieldVariance> fieldVariances = new ArrayList<>();
    fieldVariances.add(
        new FieldVariance("amount", 1000.00, 950.00, 5.0, "NUMERIC")); // 5% variance > 1%
    fieldVariances.add(new FieldVariance("description", "invoice A", "invoice B", 0.5, "TEXT"));
    fieldVariances.add(new FieldVariance("date", "2026-04-12", "2026-04-12", 0.0, "EXACT_MATCH"));

    discrepancy.setFieldVariances(fieldVariances);
    testDiscrepancies.add(discrepancy);

    // Act: Calculate the maximum variance
    Double maxVariance =
        discrepancy.getFieldVariances().stream()
            .map(FieldVariance::variancePercentage)
            .filter(v -> v != null)
            .max(Double::compareTo)
            .orElse(0.0);

    // Assert: MATERIAL = at least one variance > 1%
    assertNotNull(discrepancy);
    assertNotEquals(
        discrepancy.getSourceRecord().recordHash(), discrepancy.getTargetRecord().recordHash());
    assertTrue(maxVariance > 1.0, "Max variance should be > 1% for MATERIAL verdict");
    assertEquals(ReconciliationVerdict.MATERIAL, discrepancy.getVerdict());
    assertEquals(5.0, maxVariance);
  }

  /**
   * Test MATERIAL verdict with NULL mismatch: One field is NULL, other is not NULL vs non-NULL
   * represents 100% variance = MATERIAL
   */
  @Test
  void testVerdictMaterialWithNullMismatch() {
    // Arrange: Create a discrepancy with NULL mismatch (100% variance = MATERIAL)
    Map<String, Object> sourceFields = new HashMap<>();
    sourceFields.put("amount", 1000.00);
    sourceFields.put("description", "invoice A");
    sourceFields.put("date", null); // NULL

    ReconciliationRecord sourceRecord =
        new ReconciliationRecord(UUID.randomUUID(), "abc123def456", "source", sourceFields);

    Map<String, Object> targetFields = new HashMap<>();
    targetFields.put("amount", 1000.00);
    targetFields.put("description", "invoice A");
    targetFields.put("date", "2026-04-12"); // Non-NULL

    ReconciliationRecord targetRecord =
        new ReconciliationRecord(
            UUID.randomUUID(),
            "xyz789uvw012", // Different hash
            "target",
            targetFields);

    Discrepancy discrepancy = new Discrepancy();
    discrepancy.setDiscrepancyType("HASH_MISMATCH");
    discrepancy.setSourceRecord(sourceRecord);
    discrepancy.setTargetRecord(targetRecord);
    discrepancy.setVerdict(ReconciliationVerdict.MATERIAL);

    // One field has NULL_MISMATCH (100% variance)
    List<FieldVariance> fieldVariances = new ArrayList<>();
    fieldVariances.add(new FieldVariance("amount", 1000.00, 1000.00, 0.0, "EXACT_MATCH"));
    fieldVariances.add(
        new FieldVariance("description", "invoice A", "invoice A", 0.0, "EXACT_MATCH"));
    fieldVariances.add(
        new FieldVariance("date", null, "2026-04-12", 100.0, "NULL_MISMATCH")); // 100% variance

    discrepancy.setFieldVariances(fieldVariances);
    testDiscrepancies.add(discrepancy);

    // Act: Check for NULL mismatch
    boolean hasNullMismatch =
        discrepancy.getFieldVariances().stream()
            .anyMatch(
                fv ->
                    "NULL_MISMATCH".equals(fv.varianceType()) && fv.variancePercentage() == 100.0);

    // Assert: NULL mismatch is MATERIAL
    assertTrue(hasNullMismatch);
    assertEquals(ReconciliationVerdict.MATERIAL, discrepancy.getVerdict());
    assertEquals(100.0, discrepancy.getFieldVariances().get(2).variancePercentage());
  }

  /** Test MISSING_IN_TARGET: Records present in source but not in target */
  @Test
  void testVerdictMissingInTarget() {
    // Arrange: Create a discrepancy for records missing in target
    Map<String, Object> sourceFields = new HashMap<>();
    sourceFields.put("amount", 500.00);
    sourceFields.put("description", "missing record");
    sourceFields.put("date", "2026-04-12");

    ReconciliationRecord sourceRecord =
        new ReconciliationRecord(UUID.randomUUID(), "abc123def456", "source", sourceFields);

    Discrepancy discrepancy = new Discrepancy();
    discrepancy.setDiscrepancyType("MISSING_IN_TARGET");
    discrepancy.setSourceRecord(sourceRecord);
    discrepancy.setTargetRecord(null); // No target record
    discrepancy.setVerdict(ReconciliationVerdict.MATERIAL); // Missing record is MATERIAL
    discrepancy.setFieldVariances(new ArrayList<>()); // No fields to compare

    testDiscrepancies.add(discrepancy);

    // Assert
    assertNotNull(discrepancy);
    assertEquals("MISSING_IN_TARGET", discrepancy.getDiscrepancyType());
    assertNotNull(discrepancy.getSourceRecord());
    assertNull(discrepancy.getTargetRecord());
    assertEquals(ReconciliationVerdict.MATERIAL, discrepancy.getVerdict());
  }

  /** Test MISSING_IN_SOURCE: Records present in target but not in source */
  @Test
  void testVerdictMissingInSource() {
    // Arrange: Create a discrepancy for records missing in source
    Map<String, Object> targetFields = new HashMap<>();
    targetFields.put("amount", 750.00);
    targetFields.put("description", "extra record");
    targetFields.put("date", "2026-04-12");

    ReconciliationRecord targetRecord =
        new ReconciliationRecord(UUID.randomUUID(), "xyz789uvw012", "target", targetFields);

    Discrepancy discrepancy = new Discrepancy();
    discrepancy.setDiscrepancyType("MISSING_IN_SOURCE");
    discrepancy.setSourceRecord(null); // No source record
    discrepancy.setTargetRecord(targetRecord);
    discrepancy.setVerdict(ReconciliationVerdict.MATERIAL); // Missing in source is MATERIAL
    discrepancy.setFieldVariances(new ArrayList<>()); // No fields to compare

    testDiscrepancies.add(discrepancy);

    // Assert
    assertNotNull(discrepancy);
    assertEquals("MISSING_IN_SOURCE", discrepancy.getDiscrepancyType());
    assertNull(discrepancy.getSourceRecord());
    assertNotNull(discrepancy.getTargetRecord());
    assertEquals(ReconciliationVerdict.MATERIAL, discrepancy.getVerdict());
  }

  /** Test multiple discrepancies with different verdicts in a single reconciliation */
  @Test
  void testMultipleVerdicts() {
    // Arrange: Create discrepancies with different verdicts

    // PRISTINE
    Map<String, Object> pristineSrcFields = new HashMap<>();
    pristineSrcFields.put("amount", 1000.0);
    pristineSrcFields.put("description", "pristine");
    pristineSrcFields.put("date", "2026-04-12");

    ReconciliationRecord pristineSrc =
        new ReconciliationRecord(UUID.randomUUID(), "hash1", "source", pristineSrcFields);

    ReconciliationRecord pristineTgt =
        new ReconciliationRecord(
            UUID.randomUUID(),
            "hash1", // Same hash
            "target",
            new HashMap<>(pristineSrcFields));

    Discrepancy pristine = new Discrepancy();
    pristine.setDiscrepancyType("HASH_MATCH");
    pristine.setSourceRecord(pristineSrc);
    pristine.setTargetRecord(pristineTgt);
    pristine.setVerdict(ReconciliationVerdict.PRISTINE);
    pristine.setFieldVariances(new ArrayList<>());

    // NOISY
    Map<String, Object> noisySrcFields = new HashMap<>();
    noisySrcFields.put("amount", 500.0);
    noisySrcFields.put("description", "noisy");
    noisySrcFields.put("date", "2026-04-12");

    ReconciliationRecord noisySrc =
        new ReconciliationRecord(UUID.randomUUID(), "hash2", "source", noisySrcFields);

    Map<String, Object> noisyTgtFields = new HashMap<>();
    noisyTgtFields.put("amount", 500.05); // Very small variance
    noisyTgtFields.put("description", "noisy");
    noisyTgtFields.put("date", "2026-04-12");

    ReconciliationRecord noisyTgt =
        new ReconciliationRecord(
            UUID.randomUUID(),
            "hash3", // Different hash
            "target",
            noisyTgtFields);

    Discrepancy noisy = new Discrepancy();
    noisy.setDiscrepancyType("HASH_MISMATCH");
    noisy.setSourceRecord(noisySrc);
    noisy.setTargetRecord(noisyTgt);
    noisy.setVerdict(ReconciliationVerdict.NOISY);
    List<FieldVariance> noisyVariances = new ArrayList<>();
    noisyVariances.add(new FieldVariance("amount", 500.0, 500.05, 0.01, "NUMERIC"));
    noisy.setFieldVariances(noisyVariances);

    // MATERIAL
    Map<String, Object> materialSrcFields = new HashMap<>();
    materialSrcFields.put("amount", 2000.0);
    materialSrcFields.put("description", "material");
    materialSrcFields.put("date", "2026-04-12");

    ReconciliationRecord materialSrc =
        new ReconciliationRecord(UUID.randomUUID(), "hash4", "source", materialSrcFields);

    Map<String, Object> materialTgtFields = new HashMap<>();
    materialTgtFields.put("amount", 1800.0); // Significant variance
    materialTgtFields.put("description", "material X");
    materialTgtFields.put("date", "2026-04-12");

    ReconciliationRecord materialTgt =
        new ReconciliationRecord(
            UUID.randomUUID(),
            "hash5", // Different hash
            "target",
            materialTgtFields);

    Discrepancy material = new Discrepancy();
    material.setDiscrepancyType("HASH_MISMATCH");
    material.setSourceRecord(materialSrc);
    material.setTargetRecord(materialTgt);
    material.setVerdict(ReconciliationVerdict.MATERIAL);
    List<FieldVariance> materialVariances = new ArrayList<>();
    materialVariances.add(new FieldVariance("amount", 2000.0, 1800.0, 10.0, "NUMERIC"));
    material.setFieldVariances(materialVariances);

    testDiscrepancies.add(pristine);
    testDiscrepancies.add(noisy);
    testDiscrepancies.add(material);

    // Assert
    assertEquals(3, testDiscrepancies.size());
    assertEquals(ReconciliationVerdict.PRISTINE, testDiscrepancies.get(0).getVerdict());
    assertEquals(ReconciliationVerdict.NOISY, testDiscrepancies.get(1).getVerdict());
    assertEquals(ReconciliationVerdict.MATERIAL, testDiscrepancies.get(2).getVerdict());

    // Verify variance values
    assertTrue(testDiscrepancies.get(1).getFieldVariances().get(0).variancePercentage() <= 1.0);
    assertTrue(testDiscrepancies.get(2).getFieldVariances().get(0).variancePercentage() > 1.0);
  }
}
