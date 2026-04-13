package com.reconcile.model;

import com.reconcile.dto.ReconciliationResult;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper for converting between domain models and DTOs. Provides explicit conversion layer between
 * internal business logic and API contracts. Decouples DTO changes from internal model changes.
 */
public class ReconciliationMapper {

  private ReconciliationMapper() {
    // Utility class
  }

  /**
   * Convert engine result to domain Reconciliation model. This is called by the service layer after
   * engine execution.
   */
  public static Reconciliation engineResultToDomain(
      String domain,
      long totalSource,
      long totalTarget,
      long matchedCount,
      long mismatchedCount,
      long sourceOnlyCount,
      long targetOnlyCount,
      long pristineCount,
      long noisyCount,
      long materialCount,
      List<com.reconcile.dto.Discrepancy> discrepancyDTOs) {
    // Convert DTOs (from engine) to domain Discrepancy objects
    List<Discrepancy> discrepancies =
        discrepancyDTOs.stream()
            .map(ReconciliationMapper::dtoDomainDiscrepancy)
            .collect(Collectors.toList());

    return Reconciliation.builder(domain)
        .executedAt(Instant.now())
        .totalSourceRecords(totalSource)
        .totalTargetRecords(totalTarget)
        .matchedRecords(matchedCount)
        .mismatchedRecords(mismatchedCount)
        .sourceOnlyRecords(sourceOnlyCount)
        .targetOnlyRecords(targetOnlyCount)
        .discrepancies(discrepancies)
        .pristineCount(pristineCount)
        .noisyCount(noisyCount)
        .materialCount(materialCount)
        .build();
  }

  /** Convert API Discrepancy DTO to domain Discrepancy model */
  private static Discrepancy dtoDomainDiscrepancy(com.reconcile.dto.Discrepancy dto) {
    // Parse the discrepancy type string to enum
    Discrepancy.Type type = Discrepancy.Type.valueOf(dto.getDiscrepancyType());

    // Convert record DTOs to domain models
    ReconciliationRecord sourceRecord =
        dto.getSourceRecord() != null ? toDomain(dto.getSourceRecord()) : null;
    ReconciliationRecord targetRecord =
        dto.getTargetRecord() != null ? toDomain(dto.getTargetRecord()) : null;

    return new Discrepancy(
        type, sourceRecord, targetRecord, dto.getDetails(), dto.getFieldVariances());
  }

  /** Convert domain Reconciliation to API DTO */
  public static ReconciliationResult toDTO(Reconciliation reconciliation) {
    ReconciliationResult dto =
        new ReconciliationResult(
            reconciliation.getTotalSourceRecords(),
            reconciliation.getTotalTargetRecords(),
            reconciliation.getMatchedRecords(),
            reconciliation.getMismatchedRecords(),
            reconciliation.getSourceOnlyRecords(),
            reconciliation.getTargetOnlyRecords(),
            reconciliation.getMatchPercentage(),
            reconciliation.getDiscrepancies().stream()
                .map(ReconciliationMapper::toDTO)
                .collect(Collectors.toList()));

    dto.setPristineCount(reconciliation.getPristineCount());
    dto.setNoisyCount(reconciliation.getNoisyCount());
    dto.setMaterialCount(reconciliation.getMaterialCount());

    return dto;
  }

  /** Convert domain Discrepancy to API DTO */
  public static com.reconcile.dto.Discrepancy toDTO(Discrepancy discrepancy) {
    return new com.reconcile.dto.Discrepancy(
        discrepancy.getTypeAsString(),
        discrepancy.getSourceRecord() != null ? toDTO(discrepancy.getSourceRecord()) : null,
        discrepancy.getTargetRecord() != null ? toDTO(discrepancy.getTargetRecord()) : null,
        discrepancy.getDetails(),
        discrepancy.getVerdict(),
        discrepancy.getFieldVariances());
  }

  /** Convert domain ReconciliationRecord to API DTO */
  public static com.reconcile.dto.ReconciliationRecord toDTO(ReconciliationRecord record) {
    return new com.reconcile.dto.ReconciliationRecord(
        record.getId(), record.getRecordHash(), record.getSource(), record.getFieldsImmutable());
  }

  /** Convert API ReconciliationRecord DTO to domain ReconciliationRecord */
  public static ReconciliationRecord toDomain(com.reconcile.dto.ReconciliationRecord dto) {
    return new ReconciliationRecord(dto.id(), dto.recordHash(), dto.source(), dto.fields());
  }
}
