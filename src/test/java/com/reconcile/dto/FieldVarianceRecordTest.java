package com.reconcile.dto;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("FieldVariance Record Tests")
class FieldVarianceRecordTest {

  @Test
  @DisplayName("Should create FieldVariance with all fields")
  void testCreateWithAllFields() {
    // Act
    FieldVariance variance = new FieldVariance("amount", 1000.0, 950.0, 5.0, "NUMERIC");

    // Assert
    assertEquals("amount", variance.fieldName());
    assertEquals(1000.0, variance.sourceValue());
    assertEquals(950.0, variance.targetValue());
    assertEquals(5.0, variance.variancePercentage());
    assertEquals("NUMERIC", variance.varianceType());
  }

  @Test
  @DisplayName("Should handle exact match (0% variance)")
  void testExactMatch() {
    // Act
    FieldVariance variance =
        new FieldVariance("description", "Invoice ABC", "Invoice ABC", 0.0, "EXACT_MATCH");

    // Assert
    assertEquals("description", variance.fieldName());
    assertEquals("Invoice ABC", variance.sourceValue());
    assertEquals("Invoice ABC", variance.targetValue());
    assertEquals(0.0, variance.variancePercentage());
    assertEquals("EXACT_MATCH", variance.varianceType());
  }

  @Test
  @DisplayName("Should handle small variance (minor differences)")
  void testSmallVariance() {
    // Act
    FieldVariance variance = new FieldVariance("amount", 1000.0, 1000.50, 0.05, "NUMERIC");

    // Assert - variance <= 1%
    assertFalse(variance.exceedsThreshold()); // Returns false because 0.05 <= 1.0%
    assertEquals(0.05, variance.variancePercentage());
  }

  @Test
  @DisplayName("Should handle significant variance (exceeds threshold)")
  void testSignificantVariance() {
    // Act
    FieldVariance variance = new FieldVariance("amount", 1000.0, 950.0, 5.0, "NUMERIC");

    // Assert - variance > 1%
    assertTrue(variance.exceedsThreshold()); // 5.0 > 1.0
    assertEquals(5.0, variance.variancePercentage());
  }

  @Test
  @DisplayName("Should correctly identify threshold at 1%")
  void testThresholdAtOne() {
    // Act - exactly at threshold
    FieldVariance atThreshold = new FieldVariance("amount", 1000.0, 990.0, 1.0, "NUMERIC");

    // Act - below threshold
    FieldVariance belowThreshold = new FieldVariance("amount", 1000.0, 990.99, 0.99, "NUMERIC");

    // Assert
    assertFalse(atThreshold.exceedsThreshold()); // 1.0 > 1.0 is false
    assertFalse(belowThreshold.exceedsThreshold()); // 0.99 > 1.0 is false
  }

  @Test
  @DisplayName("Should handle TEXT variance type")
  void testTextVariance() {
    // Act
    FieldVariance variance =
        new FieldVariance("description", "Invoice A", "Invoice B", 0.5, "TEXT");

    // Assert
    assertEquals("description", variance.fieldName());
    assertEquals("TEXT", variance.varianceType());
    assertEquals(0.5, variance.variancePercentage());
  }

  @Test
  @DisplayName("Should handle NULL_MISMATCH variance type")
  void testNullMismatch() {
    // Act
    FieldVariance variance =
        new FieldVariance(
            "optional_field",
            null,
            "some_value",
            100.0, // 100% variance for null mismatch
            "NULL_MISMATCH");

    // Assert
    assertEquals("optional_field", variance.fieldName());
    assertNull(variance.sourceValue());
    assertEquals("some_value", variance.targetValue());
    assertEquals(100.0, variance.variancePercentage());
    assertEquals("NULL_MISMATCH", variance.varianceType());
    assertTrue(variance.exceedsThreshold()); // 100.0 > 1.0
  }

  @Test
  @DisplayName("Should be immutable (record)")
  void testImmutability() {
    // Act
    FieldVariance variance1 = new FieldVariance("amount", 1000.0, 950.0, 5.0, "NUMERIC");

    FieldVariance variance2 = new FieldVariance("amount", 1000.0, 950.0, 5.0, "NUMERIC");

    // Assert - records with same values should be equal
    assertEquals(variance1, variance2);
    assertEquals(variance1.hashCode(), variance2.hashCode());
  }

  @Test
  @DisplayName("Should generate proper toString()")
  void testToString() {
    // Arrange
    FieldVariance variance = new FieldVariance("amount", 1000.0, 950.0, 5.0, "NUMERIC");

    // Act
    String toString = variance.toString();

    // Assert
    assertNotNull(toString);
    assertTrue(toString.contains("FieldVariance"));
    assertTrue(toString.contains("amount"));
  }

  @Test
  @DisplayName("Should support different variance types")
  void testDifferentVarianceTypes() {
    // Act
    FieldVariance numericVariance = new FieldVariance("amount", 1000.0, 950.0, 5.0, "NUMERIC");
    FieldVariance textVariance =
        new FieldVariance("description", "InvoiceA", "InvoiceB", 0.5, "TEXT");
    FieldVariance exactMatch =
        new FieldVariance("date", "2026-04-13", "2026-04-13", 0.0, "EXACT_MATCH");
    FieldVariance nullMismatch =
        new FieldVariance("notes", null, "some text", 100.0, "NULL_MISMATCH");

    // Assert
    assertEquals("NUMERIC", numericVariance.varianceType());
    assertEquals("TEXT", textVariance.varianceType());
    assertEquals("EXACT_MATCH", exactMatch.varianceType());
    assertEquals("NULL_MISMATCH", nullMismatch.varianceType());
  }

  @Test
  @DisplayName("Should demonstrate variance progression")
  void testVarianceProgression() {
    // Act - create variances with increasing percentages
    FieldVariance pristine = new FieldVariance("f1", "v1", "v1", 0.0, "EXACT_MATCH");
    FieldVariance noisy = new FieldVariance("f2", 1000.0, 1000.50, 0.05, "NUMERIC");
    FieldVariance material = new FieldVariance("f3", 1000.0, 950.0, 5.0, "NUMERIC");

    // Assert
    assertEquals(0.0, pristine.variancePercentage());
    assertEquals(0.05, noisy.variancePercentage());
    assertEquals(5.0, material.variancePercentage());

    assertFalse(pristine.exceedsThreshold()); // 0.0 > 1.0 is false
    assertFalse(noisy.exceedsThreshold()); // 0.05 > 1.0 is false
    assertTrue(material.exceedsThreshold()); // 5.0 > 1.0 is true
  }

  @Test
  @DisplayName("Should handle negative values")
  void testNegativeValues() {
    // Act
    FieldVariance variance = new FieldVariance("balance", -500.0, -450.0, 10.0, "NUMERIC");

    // Assert
    assertEquals(-500.0, variance.sourceValue());
    assertEquals(-450.0, variance.targetValue());
    assertEquals(10.0, variance.variancePercentage());
  }

  @Test
  @DisplayName("Should handle large variance percentages")
  void testLargeVariancePercentages() {
    // Act
    FieldVariance variance = new FieldVariance("value", 100.0, 0.0, 100.0, "NUMERIC");

    // Assert
    assertEquals(100.0, variance.variancePercentage());
    assertTrue(variance.exceedsThreshold());
  }

  @Test
  @DisplayName("Should demonstrate threshold boundary behavior")
  void testThresholdBoundary() {
    // Test values around the 1.0% threshold
    double[] testValues = {0.99, 0.999, 1.0, 1.001, 1.01};

    for (double value : testValues) {
      FieldVariance variance = new FieldVariance("test", 1000.0, 1000.0, value, "NUMERIC");

      if (value > 1.0) {
        assertTrue(variance.exceedsThreshold(), "Value " + value + " should exceed threshold");
      } else {
        assertFalse(variance.exceedsThreshold(), "Value " + value + " should not exceed threshold");
      }
    }
  }

  @Test
  @DisplayName("Should maintain fields integrity across operations")
  void testFieldIntegrity() {
    // Arrange
    FieldVariance original = new FieldVariance("amount", 1000.0, 950.0, 5.0, "NUMERIC");

    // Act - record properties are immutable, access them
    String fieldName = original.fieldName();
    Object sourceValue = original.sourceValue();
    Object targetValue = original.targetValue();
    Double variancePercentage = original.variancePercentage();
    String varianceType = original.varianceType();

    // Assert - create new record with same values
    FieldVariance copy =
        new FieldVariance(fieldName, sourceValue, targetValue, variancePercentage, varianceType);

    assertEquals(original, copy);
    assertEquals(original.exceedsThreshold(), copy.exceedsThreshold());
  }
}
