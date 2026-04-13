package com.reconcile.config;

import java.util.List;

/**
 * Database configuration for source or target in a reconciliation domain. Immutable record
 * containing database connection info, schema, table, and reconciliation fields with explicit
 * datatypes.
 *
 * <p>CRITICAL: Each database (source and target) is completely independent. - idFields can differ
 * between source and target (e.g., source="id" STRING vs target="pk_id" UUID) - idFields can be
 * single or composite (multiple fields that together uniquely identify a row) - idFieldTypes must
 * be explicitly specified for type coercion (e.g., VARCHAR vs UUID) - hashFields can have different
 * names between source and target - hashFields can have different datatypes (e.g., DECIMAL vs
 * NUMERIC) - hashFields list is order-matched: hashFields[i] in source corresponds to hashFields[i]
 * in target
 *
 * <p>Type coercion example with COMPOSITE IDs: Source: Oracle with idFields=["vendor_id"(VARCHAR),
 * "invoice_date"(DATE)] Target: Postgres with idFields=["supplier_id"(VARCHAR), "bill_date"(DATE)]
 * Result: Composite key (vendor_id + invoice_date) in source matches (supplier_id + bill_date) in
 * target
 *
 * <p>Configuration handles: - COMPOSITE JOIN: source.(vendor_id, invoice_date) matches
 * target.(supplier_id, bill_date) - Comparison: DECIMAL amounts coerced to compatible numeric types
 * - String representation: UUID converted to string for hashing
 *
 * <p>In this example: - idFields differ: [vendor_id(VARCHAR), invoice_date(DATE)] vs
 * [supplier_id(VARCHAR), bill_date(DATE)] - hashFields[0] same name, same type: vendor_name vs
 * vendor_name - hashFields[1] differ name, differ type: amount (DECIMAL) vs total (NUMERIC)
 */
public record DatabaseConfig(
    DataSourceType
        type, // Database type: postgresql, oracle, mysql, sqlserver, or null for auto-detect
    String schema, // Schema name in this database
    String table, // Table name in this database
    String viewSuffix, // e.g., "_with_hash" for views with MD5 hashes
    List<FieldDefinition>
        idFields, // Composite ID field definitions with explicit datatypes IN THIS DATABASE (can be
    // single or multiple)
    List<FieldDefinition>
        hashFields, // Fields for MD5 hash IN THIS DATABASE with explicit datatypes
    // (order-important)
    String url, // JDBC URL (optional - can be inherited from application.yml if null)
    String username, // Database username (optional - can be inherited from application.yml if null)
    String
        password // Database password via env var (optional - can be inherited from application.yml
    // if null)
    ) {

  /** Compact constructor with default values */
  public DatabaseConfig {
    if (viewSuffix == null) {
      viewSuffix = "";
    }
  }

  /** Get first ID field name (convenience for single-ID case) */
  public String getIdFieldName() {
    return (idFields != null && !idFields.isEmpty()) ? idFields.get(0).name() : null;
  }

  /** Get first ID field type (convenience for single-ID case) */
  public String getIdFieldType() {
    return (idFields != null && !idFields.isEmpty()) ? idFields.get(0).type() : null;
  }

  /** Get composite ID field names as list */
  public List<String> getIdFieldNames() {
    return idFields != null ? idFields.stream().map(FieldDefinition::name).toList() : List.of();
  }

  /** Whether this config uses composite IDs (more than one ID field) */
  public boolean isCompositeId() {
    return idFields != null && idFields.size() > 1;
  }

  /** Static factory for minimal configuration */
  public static DatabaseConfig of(String schema, String table) {
    return new DatabaseConfig(null, schema, table, "", null, null, null, null, null);
  }

  /** Static factory with schema, table, and viewSuffix */
  public static DatabaseConfig of(String schema, String table, String viewSuffix) {
    return new DatabaseConfig(
        null, schema, table, viewSuffix != null ? viewSuffix : "", null, null, null, null, null);
  }

  /** Get the effective view suffix (empty string if null) */
  public String effectiveViewSuffix() {
    return viewSuffix == null ? "" : viewSuffix;
  }

  /** Get the full table name with schema */
  public String getFullTableName() {
    return schema + "." + table;
  }

  /** Get the full view name with hash suffix */
  public String getFullViewName() {
    return schema + "." + table + effectiveViewSuffix();
  }

  /** Check if this database has its own datasource configuration (not inherited) */
  public boolean hasDatasourceConfig() {
    return url != null && username != null;
  }
}
