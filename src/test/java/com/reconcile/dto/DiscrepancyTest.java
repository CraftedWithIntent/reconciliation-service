package com.reconcile.dto;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Discrepancy Tests")
class DiscrepancyTest {

  private ReconciliationRecord sourceRecord;
  private ReconciliationRecord targetRecord;

  @BeforeEach
  void setUp() {
    Map<String, Object> sourceFields = new HashMap<>();
    sourceFields.put("amount", 1000.00);
    sourceFields.put("description", "invoice");

    sourceRecord =
        new ReconciliationRecord(UUID.randomUUID(), "abc123def456", "source", sourceFields);

    Map<String, Object> targetFields = new HashMap<>();
    targetFields.put("amount", 1000.00);
    targetFields.put("description", "invoice");

    targetRecord =
        new ReconciliationRecord(UUID.randomUUID(), "abc123def456", "target", targetFields);
  }

  @Test
  @DisplayName("Should create Discrepancy with default constructor")
  void testDefaultConstructor() {
    // Act
    Discrepancy discrepancy = new Discrepancy();

    // Assert
    assertNull(discrepancy.getDiscrepancyType());
    assertNull(discrepancy.getSourceRecord());
    assertNull(discrepancy.getTargetRecord());
    assertNull(discrepancy.getDetails());
    assertNull(discrepancy.getVerdict());
    assertNotNull(discrepancy.getFieldVariances());
    assertEquals(0, discrepancy.getFieldVariances().size());
  }

  @Test
  @DisplayName("Should create Discrepancy with basic constructor")
  void testBasicConstructor() {
    // Act
    Discrepancy discrepancy =
        new Discrepancy("HASH_MATCH", sourceRecord, targetRecord, "Records match perfectly");

    // Assert
    assertEquals("HASH_MATCH", discrepancy.getDiscrepancyType());
    assertEquals(sourceRecord, discrepancy.getSourceRecord());
    assertEquals(targetRecord, discrepancy.getTargetRecord());
    assertEquals("Records match perfectly", discrepancy.getDetails());
    assertNull(discrepancy.getVerdict());
    assertNotNull(discrepancy.getFieldVariances());
    assertEquals(0, discrepancy.getFieldVariances().size());
  }

  @Test
  @DisplayName("Should create Discrepancy with full constructor")
  void testFullConstructor() {
    // Arrange
    List<FieldVariance> variances = new ArrayList<>();
    variances.add(new FieldVariance("amount", 1000.0, 1000.0, 0.0, "EXACT_MATCH"));

    // Act
    Discrepancy discrepancy =
        new Discrepancy(
            "HASH_MATCH",
            sourceRecord,
            targetRecord,
            "Perfect match",
            ReconciliationVerdict.PRISTINE,
            variances);

    // Assert
    assertEquals("HASH_MATCH", discrepancy.getDiscrepancyType());
    assertEquals(sourceRecord, discrepancy.getSourceRecord());
    assertEquals(targetRecord, discrepancy.getTargetRecord());
    assertEquals("Perfect match", discrepancy.getDetails());
    assertEquals(ReconciliationVerdict.PRISTINE, discrepancy.getVerdict());
    assertEquals(1, discrepancy.getFieldVariances().size());
  }

  @Test
  @DisplayName("Should handle null field variances in full constructor")
  void testFullConstructorWithNullVariances() {
    // Act
    Discrepancy discrepancy =
        new Discrepancy(
            "HASH_MISMATCH",
            sourceRecord,
            targetRecord,
            "Has differences",
            ReconciliationVerdict.NOISY,
            null);

    // Assert
    assertNotNull(discrepancy.getFieldVariances());
    assertEquals(0, discrepancy.getFieldVariances().size());
  }

  @Test
  @DisplayName("Should set and get discrepancy type")
  void testSetGetDiscrepancyType() {
    // Arrange
    Discrepancy discrepancy = new Discrepancy();

    // Act
    discrepancy.setDiscrepancyType("MISSING_IN_TARGET");

    // Assert
    assertEquals("MISSING_IN_TARGET", discrepancy.getDiscrepancyType());
  }

  @Test
  @DisplayName("Should set and get source record")
  void testSetGetSourceRecord() {
    // Arrange
    Discrepancy discrepancy = new Discrepancy();

    // Act
    discrepancy.setSourceRecord(sourceRecord);

    // Assert
    assertEquals(sourceRecord, discrepancy.getSourceRecord());
  }

  @Test
  @DisplayName("Should set and get target record")
  void testSetGetTargetRecord() {
    // Arrange
    Discrepancy discrepancy = new Discrepancy();

    // Act
    discrepancy.setTargetRecord(targetRecord);

    // Assert
    assertEquals(targetRecord, discrepancy.getTargetRecord());
  }

  @Test
  @DisplayName("Should set and get details")
  void testSetGetDetails() {
    // Arrange
    Discrepancy discrepancy = new Discrepancy();
    String details = "Records differ in amount field";

    // Act
    discrepancy.setDetails(details);

    // Assert
    assertEquals(details, discrepancy.getDetails());
  }

  @Test
  @DisplayName("Should set and get verdict")
  void testSetGetVerdict() {
    // Arrange
    Discrepancy discrepancy = new Discrepancy();

    // Act
    discrepancy.setVerdict(ReconciliationVerdict.MATERIAL);

    // Assert
    assertEquals(ReconciliationVerdict.MATERIAL, discrepancy.getVerdict());
  }

  @Test
  @DisplayName("Should set and get field variances")
  void testSetGetFieldVariances() {
    // Arrange
    Discrepancy discrepancy = new Discrepancy();
    List<FieldVariance> variances = new ArrayList<>();
    variances.add(new FieldVariance("amount", 1000.0, 950.0, 5.0, "NUMERIC"));
    variances.add(new FieldVariance("description", "inv1", "inv2", 0.0, "TEXT"));

    // Act
    discrepancy.setFieldVariances(variances);

    // Assert
    assertEquals(2, discrepancy.getFieldVariances().size());
    assertEquals("amount", discrepancy.getFieldVariances().get(0).fieldName());
  }

  @Test
  @DisplayName("Should handle null field variances in setter")
  void testSetNullFieldVariances() {
    // Arrange
    Discrepancy discrepancy = new Discrepancy();

    // Act
    discrepancy.setFieldVariances(null);

    // Assert
    assertNotNull(discrepancy.getFieldVariances());
    assertEquals(0, discrepancy.getFieldVariances().size());
  }

  @Test
  @DisplayName("Should demonstrate PRISTINE discrepancy (hash match)")
  void testPristineDiscrepancy() {
    // Arrange
    List<FieldVariance> variances = new ArrayList<>();
    variances.add(new FieldVariance("amount", 1000.0, 1000.0, 0.0, "EXACT_MATCH"));
    variances.add(new FieldVariance("description", "invoice", "invoice", 0.0, "EXACT_MATCH"));

    // Act
    Discrepancy discrepancy =
        new Discrepancy(
            "HASH_MATCH",
            sourceRecord,
            targetRecord,
            "Perfect hash match",
            ReconciliationVerdict.PRISTINE,
            variances);

    // Assert
    assertEquals(ReconciliationVerdict.PRISTINE, discrepancy.getVerdict());
    assertEquals(2, discrepancy.getFieldVariances().size());
    assertTrue(
        discrepancy.getFieldVariances().stream().allMatch(fv -> fv.variancePercentage() == 0.0));
  }

  @Test
  @DisplayName("Should demonstrate NOISY discrepancy (minor differences)")
  void testNoisyDiscrepancy() {
    // Arrange
    List<FieldVariance> variances = new ArrayList<>();
    variances.add(new FieldVariance("amount", 1000.0, 1000.05, 0.005, "NUMERIC"));
    variances.add(new FieldVariance("description", "invoice", "invoice", 0.0, "EXACT_MATCH"));

    // Act
    Discrepancy discrepancy =
        new Discrepancy(
            "HASH_MISMATCH",
            sourceRecord,
            targetRecord,
            "Minor differences detected",
            ReconciliationVerdict.NOISY,
            variances);

    // Assert
    assertEquals(ReconciliationVerdict.NOISY, discrepancy.getVerdict());
    Double maxVariance =
        discrepancy.getFieldVariances().stream()
            .map(FieldVariance::variancePercentage)
            .max(Double::compareTo)
            .orElse(0.0);
    assertTrue(maxVariance <= 1.0);
  }

  @Test
  @DisplayName("Should demonstrate MATERIAL discrepancy (significant differences)")
  void testMaterialDiscrepancy() {
    // Arrange
    List<FieldVariance> variances = new ArrayList<>();
    variances.add(new FieldVariance("amount", 1000.0, 950.0, 5.0, "NUMERIC"));
    variances.add(new FieldVariance("description", "invoice A", "invoice B", 0.5, "TEXT"));

    // Act
    Discrepancy discrepancy =
        new Discrepancy(
            "HASH_MISMATCH",
            sourceRecord,
            targetRecord,
            "Significant differences detected",
            ReconciliationVerdict.MATERIAL,
            variances);

    // Assert
    assertEquals(ReconciliationVerdict.MATERIAL, discrepancy.getVerdict());
    Double maxVariance =
        discrepancy.getFieldVariances().stream()
            .map(FieldVariance::variancePercentage)
            .max(Double::compareTo)
            .orElse(0.0);
    assertTrue(maxVariance > 1.0);
  }

  @Test
  @DisplayName("Should demonstrate MISSING_IN_TARGET discrepancy")
  void testMissingInTargetDiscrepancy() {
    // Act
    Discrepancy discrepancy =
        new Discrepancy(
            "MISSING_IN_TARGET",
            sourceRecord,
            null, // No target record
            "Record exists in source but missing in target",
            ReconciliationVerdict.MATERIAL,
            new ArrayList<>());

    // Assert
    assertEquals("MISSING_IN_TARGET", discrepancy.getDiscrepancyType());
    assertNotNull(discrepancy.getSourceRecord());
    assertNull(discrepancy.getTargetRecord());
    assertEquals(ReconciliationVerdict.MATERIAL, discrepancy.getVerdict());
  }

  @Test
  @DisplayName("Should demonstrate MISSING_IN_SOURCE discrepancy")
  void testMissingInSourceDiscrepancy() {
    // Act
    Discrepancy discrepancy =
        new Discrepancy(
            "MISSING_IN_SOURCE",
            null, // No source record
            targetRecord,
            "Record exists in target but missing in source",
            ReconciliationVerdict.MATERIAL,
            new ArrayList<>());

    // Assert
    assertEquals("MISSING_IN_SOURCE", discrepancy.getDiscrepancyType());
    assertNull(discrepancy.getSourceRecord());
    assertNotNull(discrepancy.getTargetRecord());
    assertEquals(ReconciliationVerdict.MATERIAL, discrepancy.getVerdict());
  }
}
