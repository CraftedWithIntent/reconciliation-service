package com.reconcile.model;

import com.reconcile.dto.FieldVariance;
import com.reconcile.dto.ReconciliationVerdict;
import java.util.*;

/**
 * Internal domain model for a discrepancy between source and target records. Encapsulates business
 * logic for verdict determination and discrepancy classification.
 */
public class Discrepancy {

  public enum Type {
    MISSING_IN_TARGET("Record exists in source but not in target"),
    MISSING_IN_SOURCE("Record exists in target but not in source"),
    HASH_MISMATCH("Record hashes differ between source and target");

    private final String description;

    Type(String description) {
      this.description = description;
    }

    public String getDescription() {
      return description;
    }
  }

  private final Type type;
  private final ReconciliationRecord sourceRecord;
  private final ReconciliationRecord targetRecord;
  private final String details;
  private final List<FieldVariance> fieldVariances;
  private ReconciliationVerdict verdict;

  /** Constructor for discrepancies with field-level variance analysis */
  public Discrepancy(
      Type type,
      ReconciliationRecord sourceRecord,
      ReconciliationRecord targetRecord,
      String details,
      List<FieldVariance> fieldVariances) {
    this.type = type;
    this.sourceRecord = sourceRecord;
    this.targetRecord = targetRecord;
    this.details = details;
    this.fieldVariances =
        fieldVariances != null ? new ArrayList<>(fieldVariances) : new ArrayList<>();
    this.verdict = determineVerdict();
  }

  /** Constructor for simple discrepancies without variance analysis */
  public Discrepancy(
      Type type,
      ReconciliationRecord sourceRecord,
      ReconciliationRecord targetRecord,
      String details) {
    this(type, sourceRecord, targetRecord, details, new ArrayList<>());
  }

  /** Determine verdict based on hash match and field variance analysis */
  private ReconciliationVerdict determineVerdict() {
    // If hashes match, it's pristine (shouldn't have discrepancy but for completeness)
    if (sourceRecord != null
        && targetRecord != null
        && Objects.equals(sourceRecord.getRecordHash(), targetRecord.getRecordHash())) {
      return ReconciliationVerdict.PRISTINE;
    }

    // If no variance data, conservatively assume material
    if (fieldVariances.isEmpty()) {
      return ReconciliationVerdict.MATERIAL;
    }

    // Check if any variance exceeds threshold
    boolean hasExcessiveVariance =
        fieldVariances.stream().anyMatch(FieldVariance::exceedsThreshold);

    return hasExcessiveVariance ? ReconciliationVerdict.MATERIAL : ReconciliationVerdict.NOISY;
  }

  /** Recalculate verdict (useful if field variances are updated) */
  public void recalculateVerdict() {
    this.verdict = determineVerdict();
  }

  // Getters (immutable pattern for domain model)

  public Type getType() {
    return type;
  }

  public String getTypeAsString() {
    return type.name();
  }

  public ReconciliationRecord getSourceRecord() {
    return sourceRecord;
  }

  public ReconciliationRecord getTargetRecord() {
    return targetRecord;
  }

  public String getDetails() {
    return details;
  }

  public List<FieldVariance> getFieldVariances() {
    return Collections.unmodifiableList(fieldVariances);
  }

  public ReconciliationVerdict getVerdict() {
    return verdict;
  }

  /** Check if this discrepancy represents a material difference */
  public boolean isMaterial() {
    return verdict == ReconciliationVerdict.MATERIAL;
  }

  /** Check if this discrepancy is just noise (minor differences) */
  public boolean isNoisy() {
    return verdict == ReconciliationVerdict.NOISY;
  }

  /** Get count of fields exceeding variance threshold */
  public long getExcessiveVarianceCount() {
    return fieldVariances.stream().filter(FieldVariance::exceedsThreshold).count();
  }

  /** Get maximum variance percentage across all fields */
  public Optional<Double> getMaxVariance() {
    return fieldVariances.stream()
        .map(FieldVariance::variancePercentage)
        .filter(Objects::nonNull)
        .max(Double::compareTo);
  }
}
