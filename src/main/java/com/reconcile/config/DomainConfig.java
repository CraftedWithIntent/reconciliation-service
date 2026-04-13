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
    boolean caseSensitive, // Case sensitivity for hash comparisons
    Double sloTarget, // Service Level Objective threshold % (default: 95.0)
    Double varianceThreshold // Field variance threshold % (default: 1.0)
    ) {

  /** Compact constructor with default case sensitivity and thresholds */
  public DomainConfig {
    // Apply defaults if not specified
    if (sloTarget == null) {
      sloTarget = 95.0;
    }
    if (varianceThreshold == null) {
      varianceThreshold = 1.0;
    }
    // Validate ranges
    if (sloTarget < 0 || sloTarget > 100) {
      throw new IllegalArgumentException("SLO target must be between 0 and 100, got: " + sloTarget);
    }
    if (varianceThreshold < 0) {
      throw new IllegalArgumentException(
          "Variance threshold cannot be negative, got: " + varianceThreshold);
    }
  }

  /** Static factory for common case with default case-insensitive matching and thresholds */
  public static DomainConfig of(String name, DatabaseConfig source, DatabaseConfig target) {
    return new DomainConfig(name, source, target, null, false, 95.0, 1.0);
  }
}
