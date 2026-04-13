package com.reconcile.dto;

/**
 * Represents variance analysis for a single field between source and target records Immutable
 * record containing field-level comparison data
 */
public record FieldVariance(
    String fieldName, // Field name
    Object sourceValue, // Value from source
    Object targetValue, // Value from target
    Double variancePercentage, // Percentage variance (for numeric fields)
    String varianceType // Type: "NUMERIC", "TEXT", "NULL_MISMATCH", "EXACT_MATCH"
    ) {

  /** Check if this field variance exceeds the 1% threshold */
  public boolean exceedsThreshold() {
    if (variancePercentage == null) {
      return false; // Exact match or non-numeric
    }
    return variancePercentage > 1.0; // 1% threshold
  }
}
