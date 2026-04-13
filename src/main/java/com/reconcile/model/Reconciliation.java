package com.reconcile.model;

import com.reconcile.dto.ReconciliationVerdict;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Internal domain model for a complete reconciliation result. Encapsulates reconciliation
 * statistics, discrepancies, and verdict aggregation logic. This is the source of truth for
 * reconciliation data internally.
 */
public class Reconciliation {

  private final String domain;
  private final Instant executedAt;

  // Record counts
  private final long totalSourceRecords;
  private final long totalTargetRecords;
  private final long matchedRecords;
  private final long mismatchedRecords;
  private final long sourceOnlyRecords;
  private final long targetOnlyRecords;

  // Discrepancies and verdicts
  private final List<Discrepancy> discrepancies;
  private final long pristineCount;
  private final long noisyCount;
  private final long materialCount;

  /** Constructor with all parameters */
  public Reconciliation(
      String domain,
      Instant executedAt,
      long totalSourceRecords,
      long totalTargetRecords,
      long matchedRecords,
      long mismatchedRecords,
      long sourceOnlyRecords,
      long targetOnlyRecords,
      List<Discrepancy> discrepancies,
      long pristineCount,
      long noisyCount,
      long materialCount) {
    this.domain = Objects.requireNonNull(domain, "Domain must not be null");
    this.executedAt = Objects.requireNonNull(executedAt, "Execution time must not be null");
    this.totalSourceRecords = totalSourceRecords;
    this.totalTargetRecords = totalTargetRecords;
    this.matchedRecords = matchedRecords;
    this.mismatchedRecords = mismatchedRecords;
    this.sourceOnlyRecords = sourceOnlyRecords;
    this.targetOnlyRecords = targetOnlyRecords;
    this.discrepancies =
        discrepancies != null
            ? Collections.unmodifiableList(new ArrayList<>(discrepancies))
            : Collections.emptyList();
    this.pristineCount = pristineCount;
    this.noisyCount = noisyCount;
    this.materialCount = materialCount;
  }

  /** Builder for convenient construction */
  public static Builder builder(String domain) {
    return new Builder(domain);
  }

  /** Calculate match percentage */
  public double getMatchPercentage() {
    if (totalSourceRecords == 0 && totalTargetRecords == 0) {
      return 100.0; // Both empty = match
    }
    long totalRecords = Math.max(totalSourceRecords, totalTargetRecords);
    if (totalRecords == 0) return 0.0;
    return (matchedRecords * 100.0) / totalRecords;
  }

  /** Get total discrepancies (all verdict levels) */
  public long getTotalDiscrepancies() {
    return mismatchedRecords + sourceOnlyRecords + targetOnlyRecords;
  }

  /** Get count of discrepancies by verdict type */
  public long getDiscrepanciesByVerdict(ReconciliationVerdict verdict) {
    return switch (verdict) {
      case PRISTINE -> pristineCount;
      case NOISY -> noisyCount;
      case MATERIAL -> materialCount;
    };
  }

  /** Check if reconciliation passed (all discrepancies are noisy or pristine) */
  public boolean isPassed() {
    return materialCount == 0;
  }

  /** Check if reconciliation is acceptable (>99% match percentage) */
  public boolean isAcceptable() {
    return getMatchPercentage() >= 99.0;
  }

  /** Get breakdown of records */
  public Map<String, Long> getRecordBreakdown() {
    return Map.of(
        "matched", matchedRecords,
        "sourceOnly", sourceOnlyRecords,
        "targetOnly", targetOnlyRecords,
        "mismatched", mismatchedRecords);
  }

  /** Get verdict breakdown */
  public Map<ReconciliationVerdict, Long> getVerdictBreakdown() {
    return Map.of(
        ReconciliationVerdict.PRISTINE, pristineCount,
        ReconciliationVerdict.NOISY, noisyCount,
        ReconciliationVerdict.MATERIAL, materialCount);
  }

  /** Get only material discrepancies */
  public List<Discrepancy> getMaterialDiscrepancies() {
    return discrepancies.stream()
        .filter(Discrepancy::isMaterial)
        .collect(Collectors.toUnmodifiableList());
  }

  /** Get only noisy discrepancies */
  public List<Discrepancy> getNoisyDiscrepancies() {
    return discrepancies.stream()
        .filter(Discrepancy::isNoisy)
        .collect(Collectors.toUnmodifiableList());
  }

  /** Get discrepancies by type */
  public List<Discrepancy> getDiscrepanciesByType(Discrepancy.Type type) {
    return discrepancies.stream()
        .filter(d -> d.getType() == type)
        .collect(Collectors.toUnmodifiableList());
  }

  // Getters

  public String getDomain() {
    return domain;
  }

  public Instant getExecutedAt() {
    return executedAt;
  }

  public long getTotalSourceRecords() {
    return totalSourceRecords;
  }

  public long getTotalTargetRecords() {
    return totalTargetRecords;
  }

  public long getMatchedRecords() {
    return matchedRecords;
  }

  public long getMismatchedRecords() {
    return mismatchedRecords;
  }

  public long getSourceOnlyRecords() {
    return sourceOnlyRecords;
  }

  public long getTargetOnlyRecords() {
    return targetOnlyRecords;
  }

  public List<Discrepancy> getDiscrepancies() {
    return discrepancies;
  }

  public long getPristineCount() {
    return pristineCount;
  }

  public long getNoisyCount() {
    return noisyCount;
  }

  public long getMaterialCount() {
    return materialCount;
  }

  /** Builder for Reconciliation */
  public static class Builder {
    private final String domain;
    private Instant executedAt = Instant.now();
    private long totalSourceRecords;
    private long totalTargetRecords;
    private long matchedRecords;
    private long mismatchedRecords;
    private long sourceOnlyRecords;
    private long targetOnlyRecords;
    private List<Discrepancy> discrepancies = new ArrayList<>();
    private long pristineCount;
    private long noisyCount;
    private long materialCount;

    public Builder(String domain) {
      this.domain = domain;
    }

    public Builder executedAt(Instant instant) {
      this.executedAt = instant;
      return this;
    }

    public Builder totalSourceRecords(long count) {
      this.totalSourceRecords = count;
      return this;
    }

    public Builder totalTargetRecords(long count) {
      this.totalTargetRecords = count;
      return this;
    }

    public Builder matchedRecords(long count) {
      this.matchedRecords = count;
      return this;
    }

    public Builder mismatchedRecords(long count) {
      this.mismatchedRecords = count;
      return this;
    }

    public Builder sourceOnlyRecords(long count) {
      this.sourceOnlyRecords = count;
      return this;
    }

    public Builder targetOnlyRecords(long count) {
      this.targetOnlyRecords = count;
      return this;
    }

    public Builder discrepancies(List<Discrepancy> discrepancies) {
      this.discrepancies = discrepancies;
      return this;
    }

    public Builder addDiscrepancy(Discrepancy discrepancy) {
      this.discrepancies.add(discrepancy);
      return this;
    }

    public Builder pristineCount(long count) {
      this.pristineCount = count;
      return this;
    }

    public Builder noisyCount(long count) {
      this.noisyCount = count;
      return this;
    }

    public Builder materialCount(long count) {
      this.materialCount = count;
      return this;
    }

    public Reconciliation build() {
      return new Reconciliation(
          domain,
          executedAt,
          totalSourceRecords,
          totalTargetRecords,
          matchedRecords,
          mismatchedRecords,
          sourceOnlyRecords,
          targetOnlyRecords,
          discrepancies,
          pristineCount,
          noisyCount,
          materialCount);
    }
  }
}
