package com.reconcile.dto;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ReconciliationRecord Record Tests")
class ReconciliationRecordTest {

  private UUID testId;
  private String testHash;
  private String testSource;
  private Map<String, Object> testFields;

  @BeforeEach
  void setUp() {
    testId = UUID.randomUUID();
    testHash = "abc123def456xyz789";
    testSource = "postgres_source";
    testFields = new HashMap<>();
    testFields.put("amount", 1000.00);
    testFields.put("description", "Invoice #123");
    testFields.put("date", "2026-04-13");
  }

  @Test
  @DisplayName("Should create ReconciliationRecord with all fields")
  void testCreateWithAllFields() {
    // Act
    ReconciliationRecord record =
        new ReconciliationRecord(testId, testHash, testSource, testFields);

    // Assert
    assertEquals(testId, record.id());
    assertEquals(testHash, record.recordHash());
    assertEquals(testSource, record.source());
    assertEquals(testFields, record.fields());
    assertEquals(3, record.fields().size());
  }

  @Test
  @DisplayName("Should provide field access via getField()")
  void testGetField() {
    // Arrange
    ReconciliationRecord record =
        new ReconciliationRecord(testId, testHash, testSource, testFields);

    // Act & Assert
    assertEquals(1000.00, record.getField("amount"));
    assertEquals("Invoice #123", record.getField("description"));
    assertEquals("2026-04-13", record.getField("date"));
  }

  @Test
  @DisplayName("Should return null for non-existent field")
  void testGetFieldNotFound() {
    // Arrange
    ReconciliationRecord record =
        new ReconciliationRecord(testId, testHash, testSource, testFields);

    // Act & Assert
    assertNull(record.getField("non_existent_field"));
  }

  @Test
  @DisplayName("Should be immutable (record)")
  void testImmunability() {
    // Act
    ReconciliationRecord record1 =
        new ReconciliationRecord(testId, testHash, testSource, testFields);

    ReconciliationRecord record2 =
        new ReconciliationRecord(testId, testHash, testSource, testFields);

    // Assert - records with same values should be equal
    assertEquals(record1, record2);
    assertEquals(record1.hashCode(), record2.hashCode());
  }

  @Test
  @DisplayName("Should generate proper toString()")
  void testToString() {
    // Arrange
    ReconciliationRecord record =
        new ReconciliationRecord(testId, testHash, testSource, testFields);

    // Act
    String toString = record.toString();

    // Assert
    assertNotNull(toString);
    assertTrue(toString.contains("ReconciliationRecord"));
    assertTrue(toString.contains(testHash));
    assertTrue(toString.contains(testSource));
  }

  @Test
  @DisplayName("Should support different source identifiers")
  void testDifferentSources() {
    // Act
    ReconciliationRecord sourceRecord =
        new ReconciliationRecord(UUID.randomUUID(), "hash1", "source", testFields);
    ReconciliationRecord targetRecord =
        new ReconciliationRecord(UUID.randomUUID(), "hash2", "target", testFields);

    // Assert
    assertEquals("source", sourceRecord.source());
    assertEquals("target", targetRecord.source());
    assertNotEquals(sourceRecord.source(), targetRecord.source());
  }

  @Test
  @DisplayName("Should support different record hashes")
  void testDifferentHashes() {
    // Act
    ReconciliationRecord record1 =
        new ReconciliationRecord(testId, "hash_abc123", testSource, testFields);
    ReconciliationRecord record2 =
        new ReconciliationRecord(testId, "hash_xyz789", testSource, testFields);

    // Assert
    assertEquals("hash_abc123", record1.recordHash());
    assertEquals("hash_xyz789", record2.recordHash());
    assertNotEquals(record1.recordHash(), record2.recordHash());
  }

  @Test
  @DisplayName("Should support different field maps")
  void testDifferentFields() {
    // Arrange
    Map<String, Object> fields1 = new HashMap<>();
    fields1.put("amount", 1000.0);

    Map<String, Object> fields2 = new HashMap<>();
    fields2.put("amount", 950.0);

    // Act
    ReconciliationRecord record1 = new ReconciliationRecord(testId, testHash, testSource, fields1);
    ReconciliationRecord record2 = new ReconciliationRecord(testId, testHash, testSource, fields2);

    // Assert
    assertEquals(1000.0, record1.getField("amount"));
    assertEquals(950.0, record2.getField("amount"));
    assertNotEquals(record1, record2);
  }

  @Test
  @DisplayName("Should handle empty fields map")
  void testEmptyFields() {
    // Act
    ReconciliationRecord record =
        new ReconciliationRecord(testId, testHash, testSource, new HashMap<>());

    // Assert
    assertEquals(0, record.fields().size());
    assertNull(record.getField("any_field"));
  }

  @Test
  @DisplayName("Should handle various field data types")
  void testVariousFieldTypes() {
    // Arrange
    Map<String, Object> fields = new HashMap<>();
    fields.put("string_field", "text value");
    fields.put("numeric_field", 123.45);
    fields.put("integer_field", 42);
    fields.put("boolean_field", true);
    fields.put("null_field", null);

    // Act
    ReconciliationRecord record = new ReconciliationRecord(testId, testHash, testSource, fields);

    // Assert
    assertEquals("text value", record.getField("string_field"));
    assertEquals(123.45, record.getField("numeric_field"));
    assertEquals(42, record.getField("integer_field"));
    assertEquals(true, record.getField("boolean_field"));
    assertNull(record.getField("null_field"));
  }

  @Test
  @DisplayName("Should maintain record integrity across operations")
  void testRecordIntegrity() {
    // Arrange
    ReconciliationRecord original =
        new ReconciliationRecord(testId, testHash, testSource, testFields);

    // Act - create a copy with same data
    ReconciliationRecord copy =
        new ReconciliationRecord(
            original.id(), original.recordHash(), original.source(), original.fields());

    // Assert - they should be equal
    assertEquals(original, copy);
    assertEquals(original.id(), copy.id());
    assertEquals(original.recordHash(), copy.recordHash());
    assertEquals(original.source(), copy.source());
    assertEquals(original.fields(), copy.fields());
  }

  @Test
  @DisplayName("Should demonstrate source vs target record pattern")
  void testSourceTargetPattern() {
    // Arrange
    Map<String, Object> sourceFields = new HashMap<>();
    sourceFields.put("amount", 1000.0);
    sourceFields.put("vendor", "Vendor A");

    Map<String, Object> targetFields = new HashMap<>();
    targetFields.put("amount", 1000.0);
    targetFields.put("vendor", "Vendor A");

    // Act
    ReconciliationRecord sourceRecord =
        new ReconciliationRecord(UUID.randomUUID(), "source_hash_123", "source", sourceFields);

    ReconciliationRecord targetRecord =
        new ReconciliationRecord(UUID.randomUUID(), "source_hash_123", "target", targetFields);

    // Assert - same data but different UUIDs and sources
    assertEquals(sourceRecord.recordHash(), targetRecord.recordHash());
    assertNotEquals(sourceRecord.id(), targetRecord.id());
    assertEquals("source", sourceRecord.source());
    assertEquals("target", targetRecord.source());
  }
}
