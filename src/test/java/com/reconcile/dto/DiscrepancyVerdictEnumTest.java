package com.reconcile.dto;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ReconciliationVerdict Enum Tests")
class ReconciliationVerdictTest {

  @Test
  @DisplayName("Should have three verdict values")
  void testVerdictValues() {
    assertEquals(3, ReconciliationVerdict.values().length);
    assertNotNull(ReconciliationVerdict.PRISTINE);
    assertNotNull(ReconciliationVerdict.NOISY);
    assertNotNull(ReconciliationVerdict.MATERIAL);
  }

  @Test
  @DisplayName("Should have description for PRISTINE verdict")
  void testPristineDescription() {
    // Act
    String description = ReconciliationVerdict.PRISTINE.getDescription();

    // Assert
    assertNotNull(description);
    assertFalse(description.isEmpty());
    assertTrue(description.contains("Pristine"));
    assertTrue(description.contains("match"));
  }

  @Test
  @DisplayName("Should have description for NOISY verdict")
  void testNoisyDescription() {
    // Act
    String description = ReconciliationVerdict.NOISY.getDescription();

    // Assert
    assertNotNull(description);
    assertFalse(description.isEmpty());
    assertTrue(description.contains("Noisy"));
    assertTrue(description.toLowerCase().contains("minor"));
  }

  @Test
  @DisplayName("Should have description for MATERIAL verdict")
  void testMaterialDescription() {
    // Act
    String description = ReconciliationVerdict.MATERIAL.getDescription();

    // Assert
    assertNotNull(description);
    assertFalse(description.isEmpty());
    assertTrue(description.contains("Material"));
    assertTrue(description.toLowerCase().contains("significant"));
  }

  @Test
  @DisplayName("Should define PRISTINE as perfect match (100% match)")
  void testPristineDefinition() {
    // Assert
    assertEquals(ReconciliationVerdict.PRISTINE, ReconciliationVerdict.PRISTINE);
    assertTrue(ReconciliationVerdict.PRISTINE.getDescription().contains("exactly"));
  }

  @Test
  @DisplayName("Should define NOISY as minor differences (95%+ match or <=1% variance)")
  void testNoisyDefinition() {
    // Assert
    assertEquals(ReconciliationVerdict.NOISY, ReconciliationVerdict.NOISY);
    assertTrue(ReconciliationVerdict.NOISY.getDescription().contains("variance"));
    assertTrue(ReconciliationVerdict.NOISY.getDescription().contains("1%"));
  }

  @Test
  @DisplayName("Should define MATERIAL as significant differences (>1% variance)")
  void testMaterialDefinition() {
    // Assert
    assertEquals(ReconciliationVerdict.MATERIAL, ReconciliationVerdict.MATERIAL);
    assertTrue(ReconciliationVerdict.MATERIAL.getDescription().contains("Significant"));
  }

  @Test
  @DisplayName("Should be comparable")
  void testVerdictComparison() {
    // Assert
    assertNotEquals(ReconciliationVerdict.PRISTINE, ReconciliationVerdict.NOISY);
    assertNotEquals(ReconciliationVerdict.NOISY, ReconciliationVerdict.MATERIAL);
    assertNotEquals(ReconciliationVerdict.PRISTINE, ReconciliationVerdict.MATERIAL);
    assertEquals(ReconciliationVerdict.PRISTINE, ReconciliationVerdict.PRISTINE);
  }

  @Test
  @DisplayName("Should support valueOf() method")
  void testValueOf() {
    // Assert
    assertEquals(ReconciliationVerdict.PRISTINE, ReconciliationVerdict.valueOf("PRISTINE"));
    assertEquals(ReconciliationVerdict.NOISY, ReconciliationVerdict.valueOf("NOISY"));
    assertEquals(ReconciliationVerdict.MATERIAL, ReconciliationVerdict.valueOf("MATERIAL"));
  }

  @Test
  @DisplayName("Should throw exception for invalid verdict name")
  void testValueOfInvalid() {
    assertThrows(IllegalArgumentException.class, () -> ReconciliationVerdict.valueOf("INVALID"));
    assertThrows(IllegalArgumentException.class, () -> ReconciliationVerdict.valueOf("pristine"));
  }

  @Test
  @DisplayName("Should support toString() method")
  void testToString() {
    // Assert
    assertEquals("PRISTINE", ReconciliationVerdict.PRISTINE.toString());
    assertEquals("NOISY", ReconciliationVerdict.NOISY.toString());
    assertEquals("MATERIAL", ReconciliationVerdict.MATERIAL.toString());
  }

  @Test
  @DisplayName("Should demonstrate verdict progression")
  void testVerdictProgression() {
    // PRISTINE = best (100% match)
    // NOISY = acceptable (95%+ match or ≤1% variance)
    // MATERIAL = unacceptable (>1% variance)

    // Assert
    assertTrue(ReconciliationVerdict.PRISTINE.ordinal() < ReconciliationVerdict.NOISY.ordinal());
    assertTrue(ReconciliationVerdict.NOISY.ordinal() < ReconciliationVerdict.MATERIAL.ordinal());
  }

  @Test
  @DisplayName("Should have unique descriptions")
  void testUniqueDescriptions() {
    // Assert
    assertNotEquals(
        ReconciliationVerdict.PRISTINE.getDescription(),
        ReconciliationVerdict.NOISY.getDescription());
    assertNotEquals(
        ReconciliationVerdict.NOISY.getDescription(),
        ReconciliationVerdict.MATERIAL.getDescription());
    assertNotEquals(
        ReconciliationVerdict.PRISTINE.getDescription(),
        ReconciliationVerdict.MATERIAL.getDescription());
  }

  @Test
  @DisplayName("Should work in switch statements")
  void testSwitchStatement() {
    // Arrange
    ReconciliationVerdict verdict = ReconciliationVerdict.NOISY;
    String result;

    // Act
    result =
        switch (verdict) {
          case PRISTINE -> "Perfect match";
          case NOISY -> "Minor differences";
          case MATERIAL -> "Significant differences";
        };

    // Assert
    assertEquals("Minor differences", result);
  }
}
