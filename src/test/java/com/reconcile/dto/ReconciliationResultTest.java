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

@DisplayName("ReconciliationResult Tests")
class ReconciliationResultTest {

  private List<Discrepancy> discrepancies;

  @BeforeEach
  void setUp() {
    discrepancies = new ArrayList<>();

    // Create sample discrepancies
    Map<String, Object> fields = new HashMap<>();
    fields.put("amount", 1000.0);

    ReconciliationRecord sourceRecord =
        new ReconciliationRecord(UUID.randomUUID(), "hash1", "source", fields);

    ReconciliationRecord targetRecord =
        new ReconciliationRecord(UUID.randomUUID(), "hash1", "target", fields);

    Discrepancy pristineDiscrepancy =
        new Discrepancy(
            "HASH_MATCH",
            sourceRecord,
            targetRecord,
            "Perfect match",
            ReconciliationVerdict.PRISTINE,
            new ArrayList<>());

    discrepancies.add(pristineDiscrepancy);
  }

  @Test
  @DisplayName("Should create ReconciliationResult with default constructor")
  void testDefaultConstructor() {
    // Act
    ReconciliationResult result = new ReconciliationResult();

    // Assert
    assertNull(result.getTotalSourceRecords());
    assertNull(result.getTotalTargetRecords());
    assertNull(result.getMatchedRecords());
    assertNull(result.getMismatchedRecords());
    assertNull(result.getSourceOnlyRecords());
    assertNull(result.getTargetOnlyRecords());
    assertNull(result.getMatchPercentage());
    assertNull(result.getDiscrepancies());
  }

  @Test
  @DisplayName("Should create ReconciliationResult with all fields")
  void testFullConstructor() {
    // Act
    ReconciliationResult result =
        new ReconciliationResult(1000L, 1000L, 990L, 10L, 0L, 0L, 99.0, discrepancies);

    // Assert
    assertEquals(1000L, result.getTotalSourceRecords());
    assertEquals(1000L, result.getTotalTargetRecords());
    assertEquals(990L, result.getMatchedRecords());
    assertEquals(10L, result.getMismatchedRecords());
    assertEquals(0L, result.getSourceOnlyRecords());
    assertEquals(0L, result.getTargetOnlyRecords());
    assertEquals(99.0, result.getMatchPercentage());
    assertEquals(1, result.getDiscrepancies().size());
  }

  @Test
  @DisplayName("Should set and get total source records")
  void testSetGetTotalSourceRecords() {
    // Arrange
    ReconciliationResult result = new ReconciliationResult();

    // Act
    result.setTotalSourceRecords(5000L);

    // Assert
    assertEquals(5000L, result.getTotalSourceRecords());
  }

  @Test
  @DisplayName("Should set and get total target records")
  void testSetGetTotalTargetRecords() {
    // Arrange
    ReconciliationResult result = new ReconciliationResult();

    // Act
    result.setTotalTargetRecords(5100L);

    // Assert
    assertEquals(5100L, result.getTotalTargetRecords());
  }

  @Test
  @DisplayName("Should set and get matched records")
  void testSetGetMatchedRecords() {
    // Arrange
    ReconciliationResult result = new ReconciliationResult();

    // Act
    result.setMatchedRecords(4950L);

    // Assert
    assertEquals(4950L, result.getMatchedRecords());
  }

  @Test
  @DisplayName("Should set and get mismatched records")
  void testSetGetMismatchedRecords() {
    // Arrange
    ReconciliationResult result = new ReconciliationResult();

    // Act
    result.setMismatchedRecords(50L);

    // Assert
    assertEquals(50L, result.getMismatchedRecords());
  }

  @Test
  @DisplayName("Should set and get source only records")
  void testSetGetSourceOnlyRecords() {
    // Arrange
    ReconciliationResult result = new ReconciliationResult();

    // Act
    result.setSourceOnlyRecords(10L);

    // Assert
    assertEquals(10L, result.getSourceOnlyRecords());
  }

  @Test
  @DisplayName("Should set and get target only records")
  void testSetGetTargetOnlyRecords() {
    // Arrange
    ReconciliationResult result = new ReconciliationResult();

    // Act
    result.setTargetOnlyRecords(25L);

    // Assert
    assertEquals(25L, result.getTargetOnlyRecords());
  }

  @Test
  @DisplayName("Should set and get match percentage")
  void testSetGetMatchPercentage() {
    // Arrange
    ReconciliationResult result = new ReconciliationResult();

    // Act
    result.setMatchPercentage(98.5);

    // Assert
    assertEquals(98.5, result.getMatchPercentage());
  }

  @Test
  @DisplayName("Should set and get discrepancies")
  void testSetGetDiscrepancies() {
    // Arrange
    ReconciliationResult result = new ReconciliationResult();

    // Act
    result.setDiscrepancies(discrepancies);

    // Assert
    assertEquals(1, result.getDiscrepancies().size());
  }

  @Test
  @DisplayName("Should set and get pristine count with default")
  void testSetGetPristineCount() {
    // Arrange
    ReconciliationResult result = new ReconciliationResult();

    // Act & Assert - default should be 0L
    assertEquals(0L, result.getPristineCount());

    // Act
    result.setPristineCount(850L);

    // Assert
    assertEquals(850L, result.getPristineCount());
  }

  @Test
  @DisplayName("Should set and get noisy count with default")
  void testSetGetNoisyCount() {
    // Arrange
    ReconciliationResult result = new ReconciliationResult();

    // Act & Assert - default should be 0L
    assertEquals(0L, result.getNoisyCount());

    // Act
    result.setNoisyCount(120L);

    // Assert
    assertEquals(120L, result.getNoisyCount());
  }

  @Test
  @DisplayName("Should set and get material count with default")
  void testSetGetMaterialCount() {
    // Arrange
    ReconciliationResult result = new ReconciliationResult();

    // Act & Assert - default should be 0L
    assertEquals(0L, result.getMaterialCount());

    // Act
    result.setMaterialCount(30L);

    // Assert
    assertEquals(30L, result.getMaterialCount());
  }

  @Test
  @DisplayName("Should demonstrate perfect reconciliation (100% match)")
  void testPerfectReconciliation() {
    // Act
    ReconciliationResult result =
        new ReconciliationResult(
            1000L, // totalSourceRecords
            1000L, // totalTargetRecords
            1000L, // matchedRecords
            0L, // mismatchedRecords
            0L, // sourceOnlyRecords
            0L, // targetOnlyRecords
            100.0, // matchPercentage
            new ArrayList<>() // no discrepancies
            );

    // Set verdict counts
    result.setPristineCount(1000L);
    result.setNoisyCount(0L);
    result.setMaterialCount(0L);

    // Assert
    assertEquals(1000L, result.getTotalSourceRecords());
    assertEquals(1000L, result.getTotalTargetRecords());
    assertEquals(1000L, result.getMatchedRecords());
    assertEquals(0L, result.getMismatchedRecords());
    assertEquals(100.0, result.getMatchPercentage());
    assertEquals(1000L, result.getPristineCount());
  }

  @Test
  @DisplayName("Should demonstrate partial reconciliation with discrepancies")
  void testPartialReconciliation() {
    // Act
    ReconciliationResult result =
        new ReconciliationResult(
            1000L, // totalSourceRecords
            980L, // totalTargetRecords
            920L, // matchedRecords (92% match)
            80L, // mismatchedRecords
            20L, // sourceOnlyRecords
            0L, // targetOnlyRecords
            92.0, // matchPercentage
            discrepancies);

    // Set verdict counts
    result.setPristineCount(800L);
    result.setNoisyCount(100L);
    result.setMaterialCount(20L);

    // Assert
    assertEquals(92.0, result.getMatchPercentage());
    assertEquals(80L, result.getMismatchedRecords());
    assertEquals(20L, result.getSourceOnlyRecords());
    assertEquals(800L, result.getPristineCount());
    assertEquals(100L, result.getNoisyCount());
    assertEquals(20L, result.getMaterialCount());
  }

  @Test
  @DisplayName("Should handle reconciliation with source-only records")
  void testReconciliationWithSourceOnly() {
    // Act
    ReconciliationResult result =
        new ReconciliationResult(
            1100L, // totalSourceRecords
            1000L, // totalTargetRecords
            980L, // matchedRecords
            20L, // mismatchedRecords
            100L, // sourceOnlyRecords (100 extra in source)
            0L, // targetOnlyRecords
            98.0, // matchPercentage
            new ArrayList<>());

    // Assert
    assertEquals(100L, result.getSourceOnlyRecords());
    assertEquals(0L, result.getTargetOnlyRecords());
    assertTrue(result.getTotalSourceRecords() > result.getTotalTargetRecords());
  }

  @Test
  @DisplayName("Should handle reconciliation with target-only records")
  void testReconciliationWithTargetOnly() {
    // Act
    ReconciliationResult result =
        new ReconciliationResult(
            1000L, // totalSourceRecords
            1050L, // totalTargetRecords
            980L, // matchedRecords
            20L, // mismatchedRecords
            0L, // sourceOnlyRecords
            50L, // targetOnlyRecords (50 extra in target)
            98.0, // matchPercentage
            new ArrayList<>());

    // Assert
    assertEquals(0L, result.getSourceOnlyRecords());
    assertEquals(50L, result.getTargetOnlyRecords());
    assertTrue(result.getTotalTargetRecords() > result.getTotalSourceRecords());
  }

  @Test
  @DisplayName("Should demonstrate verdict distribution")
  void testVerdictDistribution() {
    // Act
    ReconciliationResult result = new ReconciliationResult();
    result.setTotalSourceRecords(1000L);
    result.setPristineCount(750L); // 75%
    result.setNoisyCount(200L); // 20%
    result.setMaterialCount(50L); // 5%

    // Assert
    Long total = result.getPristineCount() + result.getNoisyCount() + result.getMaterialCount();
    assertEquals(1000L, total);
    assertEquals(75.0, (double) result.getPristineCount() / total * 100, 0.01);
    assertEquals(20.0, (double) result.getNoisyCount() / total * 100, 0.01);
    assertEquals(5.0, (double) result.getMaterialCount() / total * 100, 0.01);
  }

  @Test
  @DisplayName("Should handle null discrepancies list")
  void testNullDiscrepancies() {
    // Act
    ReconciliationResult result = new ReconciliationResult(100L, 100L, 95L, 5L, 0L, 0L, 95.0, null);

    // Assert
    assertNull(result.getDiscrepancies());
  }

  @Test
  @DisplayName("Should default verdict counters to 0L when null")
  void testNullVerdictCounters() {
    // Arrange
    ReconciliationResult result = new ReconciliationResult();

    // Act doesn't set counters - they should default to 0L

    // Assert
    assertEquals(0L, result.getPristineCount());
    assertEquals(0L, result.getNoisyCount());
    assertEquals(0L, result.getMaterialCount());
  }
}
