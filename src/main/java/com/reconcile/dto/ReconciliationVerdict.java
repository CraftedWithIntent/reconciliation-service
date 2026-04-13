package com.reconcile.dto;

/**
 * Verdict classification for discrepancies Based on hash match and field-level variance analysis
 */
public enum ReconciliationVerdict {
  /** PRISTINE: Record hashes match exactly (100% match) */
  PRISTINE("Pristine - Record hashes match exactly"),

  /**
   * NOISY: Record hashes differ, but all field variances are <= 1% Match percentage 95% or above
   */
  NOISY("Noisy - Minor differences (variance <= 1% on all fields)"),

  /**
   * MATERIAL: Record hashes differ and at least one field has > 1% variance Match percentage below
   * 95%
   */
  MATERIAL("Material - Significant differences (variance > 1% on some fields)");

  private final String description;

  ReconciliationVerdict(String description) {
    this.description = description;
  }

  public String getDescription() {
    return description;
  }
}
