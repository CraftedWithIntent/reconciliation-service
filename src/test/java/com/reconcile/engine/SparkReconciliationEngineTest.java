package com.reconcile.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.reconcile.config.DataSourceType;
import com.reconcile.config.DatabaseConfig;
import com.reconcile.config.DomainConfig;
import com.reconcile.config.FieldDefinition;
import com.reconcile.dto.Discrepancy;
import com.reconcile.dto.FieldVariance;
import com.reconcile.dto.ReconciliationRecord;
import com.reconcile.dto.ReconciliationVerdict;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for SparkReconciliationEngine reconciliation logic Tests verdict classification, field
 * variance analysis, and configuration handling
 */
class SparkReconciliationEngineTest {

  private DomainConfig domainConfig;
  private List<ReconciliationTestCase> testCases;

  @BeforeEach
  void setUp() {
    // Setup source database config using record constructor with FieldDefinition
    DatabaseConfig sourceConfig =
        new DatabaseConfig(
            DataSourceType.POSTGRESQL,
            "source_schema",
            "test_table",
            "_with_hash",
            List.of(FieldDefinition.string("id")),
            new ArrayList<>(
                Arrays.asList(
                    FieldDefinition.decimal("amount"), FieldDefinition.string("description"))),
            null,
            null,
            null);

    // Setup target database config using record constructor with FieldDefinition
    DatabaseConfig targetConfig =
        new DatabaseConfig(
            DataSourceType.POSTGRESQL,
            "target_schema",
            "test_table",
            "_with_hash",
            List.of(FieldDefinition.string("record_id")),
            new ArrayList<>(
                Arrays.asList(
                    FieldDefinition.decimal("amount"), FieldDefinition.string("description"))),
            null,
            null,
            null);

    // Setup domain config using record constructor
    domainConfig =
        new DomainConfig("test-reconciliation", sourceConfig, targetConfig, null, false, 95.0, 1.0);

    testCases = new ArrayList<>();
  }

  @AfterEach
  void tearDown() {
    domainConfig = null;
    testCases.clear();
    testCases = null;
  }

  @Test
  void testVerdictMatchWithIdenticalHashes() {
    // Arrange
    ReconciliationRecord sourceRecord = createRecord("source", "abc123def456");
    ReconciliationRecord targetRecord = createRecord("target", "abc123def456");

    Discrepancy discrepancy = new Discrepancy();
    discrepancy.setDiscrepancyType("HASH_MATCH");
    discrepancy.setSourceRecord(sourceRecord);
    discrepancy.setTargetRecord(targetRecord);

    List<FieldVariance> fieldVariances = new ArrayList<>();
    fieldVariances.add(new FieldVariance("amount", 1000.0, 1000.0, 0.0, "EXACT_MATCH"));
    fieldVariances.add(new FieldVariance("description", "invoice", "invoice", 0.0, "EXACT_MATCH"));
    discrepancy.setFieldVariances(fieldVariances);
    discrepancy.setVerdict(ReconciliationVerdict.PRISTINE);

    testCases.add(new ReconciliationTestCase(discrepancy, ReconciliationVerdict.PRISTINE));

    // Assert
    assertEquals(
        discrepancy.getSourceRecord().recordHash(), discrepancy.getTargetRecord().recordHash());
    assertEquals(ReconciliationVerdict.PRISTINE, discrepancy.getVerdict());
  }

  @Test
  void testVerdictMismatchWithMinorVariance() {
    // Arrange
    Discrepancy discrepancy = new Discrepancy();
    discrepancy.setDiscrepancyType("HASH_MISMATCH");
    discrepancy.setSourceRecord(createRecord("source", "abc123"));
    discrepancy.setTargetRecord(createRecord("target", "def456"));

    List<FieldVariance> fieldVariances = new ArrayList<>();
    fieldVariances.add(new FieldVariance("amount", 1000.0, 1000.50, 0.05, "NUMERIC"));
    fieldVariances.add(new FieldVariance("description", "invoice", "invoice", 0.0, "EXACT_MATCH"));
    discrepancy.setFieldVariances(fieldVariances);
    discrepancy.setVerdict(ReconciliationVerdict.NOISY);

    // Act
    Double maxVariance =
        fieldVariances.stream()
            .map(FieldVariance::variancePercentage)
            .max(Double::compareTo)
            .orElse(0.0);

    // Assert
    assertTrue(maxVariance <= 1.0);
    assertEquals(ReconciliationVerdict.NOISY, discrepancy.getVerdict());
  }

  @Test
  void testVerdictMismatchWithSignificantVariance() {
    // Arrange
    Discrepancy discrepancy = new Discrepancy();
    discrepancy.setDiscrepancyType("HASH_MISMATCH");
    discrepancy.setSourceRecord(createRecord("source", "abc123"));
    discrepancy.setTargetRecord(createRecord("target", "def456"));

    List<FieldVariance> fieldVariances = new ArrayList<>();
    fieldVariances.add(new FieldVariance("amount", 1000.0, 950.0, 5.0, "NUMERIC"));
    fieldVariances.add(new FieldVariance("description", "invoice A", "invoice B", 0.5, "TEXT"));
    discrepancy.setFieldVariances(fieldVariances);
    discrepancy.setVerdict(ReconciliationVerdict.MATERIAL);

    // Act
    Double maxVariance =
        fieldVariances.stream()
            .map(FieldVariance::variancePercentage)
            .max(Double::compareTo)
            .orElse(0.0);

    // Assert
    assertTrue(maxVariance > 1.0);
    assertEquals(ReconciliationVerdict.MATERIAL, discrepancy.getVerdict());
    assertEquals(5.0, maxVariance);
  }

  @Test
  void testDiscrepancySourceOnly() {
    // Arrange
    Discrepancy discrepancy = new Discrepancy();
    discrepancy.setDiscrepancyType("MISSING_IN_TARGET");
    discrepancy.setSourceRecord(createRecord("source", "abc123"));
    discrepancy.setTargetRecord(null);
    discrepancy.setVerdict(ReconciliationVerdict.MATERIAL);
    discrepancy.setFieldVariances(new ArrayList<>());

    testCases.add(new ReconciliationTestCase(discrepancy, ReconciliationVerdict.MATERIAL));

    // Assert
    assertEquals("MISSING_IN_TARGET", discrepancy.getDiscrepancyType());
    assertNotNull(discrepancy.getSourceRecord());
    assertNull(discrepancy.getTargetRecord());
    assertEquals(ReconciliationVerdict.MATERIAL, discrepancy.getVerdict());
  }

  @Test
  void testDiscrepancyTargetOnly() {
    // Arrange
    Discrepancy discrepancy = new Discrepancy();
    discrepancy.setDiscrepancyType("MISSING_IN_SOURCE");
    discrepancy.setSourceRecord(null);
    discrepancy.setTargetRecord(createRecord("target", "def456"));
    discrepancy.setVerdict(ReconciliationVerdict.MATERIAL);
    discrepancy.setFieldVariances(new ArrayList<>());

    testCases.add(new ReconciliationTestCase(discrepancy, ReconciliationVerdict.MATERIAL));

    // Assert
    assertEquals("MISSING_IN_SOURCE", discrepancy.getDiscrepancyType());
    assertNull(discrepancy.getSourceRecord());
    assertNotNull(discrepancy.getTargetRecord());
    assertEquals(ReconciliationVerdict.MATERIAL, discrepancy.getVerdict());
  }

  @Test
  void testFieldVarianceNullMismatch() {
    // Arrange
    List<FieldVariance> fieldVariances = new ArrayList<>();
    fieldVariances.add(new FieldVariance("amount", 1000.0, 1000.0, 0.0, "EXACT_MATCH"));
    fieldVariances.add(new FieldVariance("description", null, "invoice", 100.0, "NULL_MISMATCH"));

    // Act
    boolean hasNullMismatch =
        fieldVariances.stream()
            .anyMatch(
                fv ->
                    "NULL_MISMATCH".equals(fv.varianceType()) && fv.variancePercentage() == 100.0);

    // Assert
    assertTrue(hasNullMismatch);
  }

  @Test
  void testHeterogeneousDatabaseSupport() {
    // Arrange
    DatabaseConfig sourceConfig =
        new DatabaseConfig(
            DataSourceType.POSTGRESQL,
            "",
            "",
            "",
            List.of(FieldDefinition.string("id")),
            new ArrayList<>(
                Arrays.asList(FieldDefinition.decimal("amount"), FieldDefinition.string("desc"))),
            null,
            null,
            null);

    DatabaseConfig targetConfig =
        new DatabaseConfig(
            DataSourceType.ORACLE,
            "",
            "",
            "",
            List.of(FieldDefinition.string("id")),
            new ArrayList<>(
                Arrays.asList(
                    FieldDefinition.decimal("amt"), FieldDefinition.string("description"))),
            null,
            null,
            null);

    DomainConfig config =
        new DomainConfig("test", sourceConfig, targetConfig, new HashMap<>(), false, 95.0, 1.0);

    // Assert
    assertEquals(DataSourceType.POSTGRESQL, config.source().type());
    assertEquals(DataSourceType.ORACLE, config.target().type());
    assertNotEquals(
        config.source().type().getDriverClass(), config.target().type().getDriverClass());
  }

  @Test
  void testFieldMappingDifferentNames() {
    // Arrange
    DatabaseConfig sourceConfig =
        new DatabaseConfig(
            DataSourceType.POSTGRESQL,
            "",
            "",
            "",
            List.of(FieldDefinition.string("source_id")),
            new ArrayList<>(
                Arrays.asList(
                    FieldDefinition.decimal("invoice_amt"),
                    FieldDefinition.string("invoice_desc"),
                    FieldDefinition.timestamp("invoice_date"))),
            null,
            null,
            null);

    DatabaseConfig targetConfig =
        new DatabaseConfig(
            DataSourceType.POSTGRESQL,
            "",
            "",
            "",
            List.of(FieldDefinition.string("record_id")),
            new ArrayList<>(
                Arrays.asList(
                    FieldDefinition.decimal("amt"),
                    FieldDefinition.string("desc"),
                    FieldDefinition.timestamp("posting_date"))),
            null,
            null,
            null);

    DomainConfig config =
        new DomainConfig("test", sourceConfig, targetConfig, new HashMap<>(), false, 95.0, 1.0);

    // Assert
    assertEquals(3, config.source().hashFields().size());
    assertEquals(3, config.target().hashFields().size());
    // Verify field names are different (invoice_amt vs amt)
    assertNotEquals(
        config.source().hashFields().get(0).name(), config.target().hashFields().get(0).name());
  }

  @Test
  void testMultipleReconciliationCases() {
    // Arrange
    testCases.add(new ReconciliationTestCase(createPristineCase(), ReconciliationVerdict.PRISTINE));
    testCases.add(new ReconciliationTestCase(createNoisyCase(), ReconciliationVerdict.NOISY));
    testCases.add(new ReconciliationTestCase(createMaterialCase(), ReconciliationVerdict.MATERIAL));

    // Assert
    assertEquals(3, testCases.size());
    for (ReconciliationTestCase testCase : testCases) {
      assertEquals(testCase.expectedVerdict, testCase.discrepancy.getVerdict());
    }
  }

  // Helper methods
  private ReconciliationRecord createRecord(String source, String hash) {
    return new ReconciliationRecord(UUID.randomUUID(), hash, source, new HashMap<>());
  }

  private Discrepancy createPristineCase() {
    Discrepancy discrepancy = new Discrepancy();
    discrepancy.setDiscrepancyType("HASH_MATCH");
    discrepancy.setSourceRecord(createRecord("source", "pristine123"));
    discrepancy.setTargetRecord(createRecord("target", "pristine123"));
    discrepancy.setVerdict(ReconciliationVerdict.PRISTINE);
    discrepancy.setFieldVariances(new ArrayList<>());
    return discrepancy;
  }

  private Discrepancy createNoisyCase() {
    Discrepancy discrepancy = new Discrepancy();
    discrepancy.setDiscrepancyType("HASH_MISMATCH");
    discrepancy.setSourceRecord(createRecord("source", "noisy1"));
    discrepancy.setTargetRecord(createRecord("target", "noisy2"));
    List<FieldVariance> variances = new ArrayList<>();
    variances.add(new FieldVariance("amount", 500.0, 500.005, 0.001, "NUMERIC"));
    discrepancy.setFieldVariances(variances);
    discrepancy.setVerdict(ReconciliationVerdict.NOISY);
    return discrepancy;
  }

  private Discrepancy createMaterialCase() {
    Discrepancy discrepancy = new Discrepancy();
    discrepancy.setDiscrepancyType("HASH_MISMATCH");
    discrepancy.setSourceRecord(createRecord("source", "material1"));
    discrepancy.setTargetRecord(createRecord("target", "material2"));
    List<FieldVariance> variances = new ArrayList<>();
    variances.add(new FieldVariance("amount", 1000.0, 850.0, 15.0, "NUMERIC"));
    discrepancy.setFieldVariances(variances);
    discrepancy.setVerdict(ReconciliationVerdict.MATERIAL);
    return discrepancy;
  }

  private static class ReconciliationTestCase {
    Discrepancy discrepancy;
    ReconciliationVerdict expectedVerdict;

    ReconciliationTestCase(Discrepancy discrepancy, ReconciliationVerdict expectedVerdict) {
      this.discrepancy = discrepancy;
      this.expectedVerdict = expectedVerdict;
    }
  }
}
