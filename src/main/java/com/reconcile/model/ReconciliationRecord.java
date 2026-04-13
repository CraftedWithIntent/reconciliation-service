package com.reconcile.model;

import java.util.*;

/**
 * Internal domain model for a reconciliation record. Represents a single record from source or
 * target database with its fields and hash. Immutable by design for safe concurrent access in Spark
 * processing.
 */
public class ReconciliationRecord {

  private final UUID id;
  private final String recordHash;
  private final String source; // "source" or "target"
  private final Map<String, Object> fields;

  /** Constructor */
  public ReconciliationRecord(
      UUID id, String recordHash, String source, Map<String, Object> fields) {
    this.id = Objects.requireNonNull(id, "Record ID must not be null");
    this.recordHash = Objects.requireNonNull(recordHash, "Record hash must not be null");
    this.source = Objects.requireNonNull(source, "Source must not be null");
    this.fields =
        fields != null
            ? Collections.unmodifiableMap(new HashMap<>(fields))
            : Collections.emptyMap();
  }

  /** Get field value by name */
  public Object getField(String fieldName) {
    return fields.get(fieldName);
  }

  /** Get all field names */
  public Set<String> getFieldNames() {
    return fields.keySet();
  }

  /** Check if field exists */
  public boolean hasField(String fieldName) {
    return fields.containsKey(fieldName);
  }

  /** Copy with new fields (builder pattern) */
  public ReconciliationRecord withFields(Map<String, Object> newFields) {
    return new ReconciliationRecord(this.id, this.recordHash, this.source, newFields);
  }

  // Getters

  public UUID getId() {
    return id;
  }

  public String getRecordHash() {
    return recordHash;
  }

  public String getSource() {
    return source;
  }

  public Map<String, Object> getFields() {
    return fields;
  }

  /** Get all fields as immutable map */
  public Map<String, Object> getFieldsImmutable() {
    return Collections.unmodifiableMap(fields);
  }

  @Override
  public String toString() {
    return "ReconciliationRecord{"
        + "id="
        + id
        + ", recordHash='"
        + recordHash
        + '\''
        + ", source='"
        + source
        + '\''
        + ", fieldCount="
        + fields.size()
        + '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ReconciliationRecord that = (ReconciliationRecord) o;
    return id.equals(that.id) && recordHash.equals(that.recordHash);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, recordHash);
  }
}
