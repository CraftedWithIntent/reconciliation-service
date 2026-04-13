package com.reconcile.dto;

import java.util.ArrayList;
import java.util.List;

public class Discrepancy {
  private String discrepancyType; // MISSING_IN_TARGET, MISSING_IN_SOURCE, HASH_MISMATCH
  private ReconciliationRecord sourceRecord;
  private ReconciliationRecord targetRecord;
  private String details;
  private ReconciliationVerdict verdict; // PRISTINE, NOISY, or MATERIAL
  private List<FieldVariance> fieldVariances; // Detailed field-level variance analysis

  // Constructors
  public Discrepancy() {
    this.fieldVariances = new ArrayList<>();
  }

  public Discrepancy(
      String discrepancyType,
      ReconciliationRecord sourceRecord,
      ReconciliationRecord targetRecord,
      String details) {
    this.discrepancyType = discrepancyType;
    this.sourceRecord = sourceRecord;
    this.targetRecord = targetRecord;
    this.details = details;
    this.fieldVariances = new ArrayList<>();
  }

  public Discrepancy(
      String discrepancyType,
      ReconciliationRecord sourceRecord,
      ReconciliationRecord targetRecord,
      String details,
      ReconciliationVerdict verdict,
      List<FieldVariance> fieldVariances) {
    this.discrepancyType = discrepancyType;
    this.sourceRecord = sourceRecord;
    this.targetRecord = targetRecord;
    this.details = details;
    this.verdict = verdict;
    this.fieldVariances = fieldVariances != null ? fieldVariances : new ArrayList<>();
  }

  // Getters and Setters
  public String getDiscrepancyType() {
    return discrepancyType;
  }

  public void setDiscrepancyType(String discrepancyType) {
    this.discrepancyType = discrepancyType;
  }

  public ReconciliationRecord getSourceRecord() {
    return sourceRecord;
  }

  public void setSourceRecord(ReconciliationRecord sourceRecord) {
    this.sourceRecord = sourceRecord;
  }

  public ReconciliationRecord getTargetRecord() {
    return targetRecord;
  }

  public void setTargetRecord(ReconciliationRecord targetRecord) {
    this.targetRecord = targetRecord;
  }

  public String getDetails() {
    return details;
  }

  public void setDetails(String details) {
    this.details = details;
  }

  public ReconciliationVerdict getVerdict() {
    return verdict;
  }

  public void setVerdict(ReconciliationVerdict verdict) {
    this.verdict = verdict;
  }

  public List<FieldVariance> getFieldVariances() {
    return fieldVariances;
  }

  public void setFieldVariances(List<FieldVariance> fieldVariances) {
    this.fieldVariances = fieldVariances != null ? fieldVariances : new ArrayList<>();
  }
}
