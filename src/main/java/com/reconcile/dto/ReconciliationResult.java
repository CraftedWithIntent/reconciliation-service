package com.reconcile.dto;

import java.util.List;

public class ReconciliationResult {
  private Long totalSourceRecords;
  private Long totalTargetRecords;
  private Long matchedRecords;
  private Long mismatchedRecords;
  private Long sourceOnlyRecords;
  private Long targetOnlyRecords;
  private Double matchPercentage;
  private List<Discrepancy> discrepancies;

  // Verdict statistics
  private Long pristineCount; // Records with PRISTINE verdict (100% match)
  private Long noisyCount; // Records with NOISY verdict (95%+ match or ≤1% variance)
  private Long materialCount; // Records with MATERIAL verdict (>1% variance)

  // Constructors
  public ReconciliationResult() {}

  public ReconciliationResult(
      Long totalSourceRecords,
      Long totalTargetRecords,
      Long matchedRecords,
      Long mismatchedRecords,
      Long sourceOnlyRecords,
      Long targetOnlyRecords,
      Double matchPercentage,
      List<Discrepancy> discrepancies) {
    this.totalSourceRecords = totalSourceRecords;
    this.totalTargetRecords = totalTargetRecords;
    this.matchedRecords = matchedRecords;
    this.mismatchedRecords = mismatchedRecords;
    this.sourceOnlyRecords = sourceOnlyRecords;
    this.targetOnlyRecords = targetOnlyRecords;
    this.matchPercentage = matchPercentage;
    this.discrepancies = discrepancies;
  }

  // Getters and Setters
  public Long getTotalSourceRecords() {
    return totalSourceRecords;
  }

  public void setTotalSourceRecords(Long totalSourceRecords) {
    this.totalSourceRecords = totalSourceRecords;
  }

  public Long getTotalTargetRecords() {
    return totalTargetRecords;
  }

  public void setTotalTargetRecords(Long totalTargetRecords) {
    this.totalTargetRecords = totalTargetRecords;
  }

  public Long getMatchedRecords() {
    return matchedRecords;
  }

  public void setMatchedRecords(Long matchedRecords) {
    this.matchedRecords = matchedRecords;
  }

  public Long getMismatchedRecords() {
    return mismatchedRecords;
  }

  public void setMismatchedRecords(Long mismatchedRecords) {
    this.mismatchedRecords = mismatchedRecords;
  }

  public Long getSourceOnlyRecords() {
    return sourceOnlyRecords;
  }

  public void setSourceOnlyRecords(Long sourceOnlyRecords) {
    this.sourceOnlyRecords = sourceOnlyRecords;
  }

  public Long getTargetOnlyRecords() {
    return targetOnlyRecords;
  }

  public void setTargetOnlyRecords(Long targetOnlyRecords) {
    this.targetOnlyRecords = targetOnlyRecords;
  }

  public Double getMatchPercentage() {
    return matchPercentage;
  }

  public void setMatchPercentage(Double matchPercentage) {
    this.matchPercentage = matchPercentage;
  }

  public List<Discrepancy> getDiscrepancies() {
    return discrepancies;
  }

  public void setDiscrepancies(List<Discrepancy> discrepancies) {
    this.discrepancies = discrepancies;
  }

  public Long getPristineCount() {
    return pristineCount != null ? pristineCount : 0L;
  }

  public void setPristineCount(Long pristineCount) {
    this.pristineCount = pristineCount;
  }

  public Long getNoisyCount() {
    return noisyCount != null ? noisyCount : 0L;
  }

  public void setNoisyCount(Long noisyCount) {
    this.noisyCount = noisyCount;
  }

  public Long getMaterialCount() {
    return materialCount != null ? materialCount : 0L;
  }

  public void setMaterialCount(Long materialCount) {
    this.materialCount = materialCount;
  }
}
