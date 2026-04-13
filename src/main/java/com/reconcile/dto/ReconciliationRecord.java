package com.reconcile.dto;

import java.util.Map;
import java.util.UUID;

/**
 * Generic record DTO for any reconciliation domain. Holds arbitrary field values defined by domain
 * configuration. Immutable record for serialization and data transfer.
 */
public record ReconciliationRecord(
    UUID id,
    String recordHash,
    String source, // "source" or "target"
    Map<String, Object> fields // Domain-specific field values
    ) {

  /** Get a specific field value by name */
  public Object getField(String fieldName) {
    return fields != null ? fields.get(fieldName) : null;
  }
}
