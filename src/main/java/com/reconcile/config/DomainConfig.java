package com.reconcile.config;

import java.util.Map;

/**
 * Configuration for a specific reconciliation domain. Defines source and target database
 * configurations independently.
 *
 * <p>CRITICAL DESIGN: Source and target are completely decoupled. - Different database types:
 * source=PostgreSQL, target=Oracle - Different field names: source.idField="id",
 * target.idField="pk_id" - Different schemas: source="public", target="ledger_schema" - Different
 * tables: source="orders", target="order_ledger"
 *
 * <p>No assumptions are made about field name matching between source and target. Field mapping is
 * done via order-matching in hashFields lists: source.hashFields[i] semantically corresponds to
 * target.hashFields[i]
 *
 * <p>Example: Source PostgreSQL: idField="order_id", hashFields=["customer_id", "amount", "date"]
 * Target Oracle: idField="order_no", hashFields=["cust_no", "txn_amt", "txn_date"]
 *
 * <p>This is valid because: - JOIN is done: source.order_id = target.order_no - Hash comparison:
 * source["customer_id"] vs target["cust_no"] (semantic: customer identifier) - Hash comparison:
 * source["amount"] vs target["txn_amt"] (semantic: amount) - Hash comparison: source["date"] vs
 * target["txn_date"] (semantic: date)
 */
public record DomainConfig(
    String name, // Domain identifier (e.g., "vendor-invoices")
    DatabaseConfig source, // Source database configuration (independent)
    DatabaseConfig target, // Target database configuration (independent)
    Map<String, String> fieldMappings, // Optional explicit field mapping for documentation
    boolean caseSensitive // Case sensitivity for hash comparisons
    ) {

  /** Compact constructor with default case sensitivity */
  public DomainConfig {
    // Nothing to validate in compact constructor
  }

  /** Static factory for common case with default case-insensitive matching */
  public static DomainConfig of(String name, DatabaseConfig source, DatabaseConfig target) {
    return new DomainConfig(name, source, target, null, false);
  }
}
