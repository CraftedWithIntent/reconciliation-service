package com.reconcile.config;

/**
 * Defines a field with its name and datatype. Used to explicitly specify field types for proper
 * type coercion during reconciliation.
 *
 * <p>Supported datatypes: - STRING, VARCHAR, CHAR (text fields) - INT, INTEGER, BIGINT, SMALLINT
 * (integer types) - DECIMAL, NUMERIC, FLOAT, DOUBLE (numeric types) - DATE, TIMESTAMP, DATETIME
 * (date/time types) - UUID, UUIDV4 (Universal Unique Identifier) - BOOLEAN, BOOL (boolean type) -
 * BYTES, BINARY (binary type)
 *
 * <p>Type coercion rules: - STRING ↔ INT: parsed/cast automatically - INT ↔ BIGINT: width
 * difference handled automatically - DECIMAL ↔ FLOAT: precision difference handled, may lose
 * precision - DATE ↔ TIMESTAMP: timestamp truncated to date, or date time set to 00:00:00 - UUID ↔
 * STRING: UUID cast to/from string representation
 *
 * <p>Example: Source: orderid (Oracle, CHAR(10)) + amount (DECIMAL(12,2)) Target: order_uuid
 * (Postgres, UUID) + amount (NUMERIC(14,4))
 *
 * <p>Configuration: source.idField.name: orderid source.idField.type: varchar # Oracle CHAR as
 * varchar target.idField.name: order_uuid target.idField.type: uuid # Postgres UUID
 *
 * <p>Reconciliation will: 1. Cast source.orderid (VARCHAR) to STRING for joining 2. Cast
 * target.order_uuid (UUID) to STRING for joining 3. Compare as strings: "ABC123" = "ABC123"
 */
public record FieldDefinition(
    String name, // Field name in database
    String type // Datatype: STRING, INT, DECIMAL, DATE, TIMESTAMP, UUID, etc.
    ) {

  /** Constructor with defaults */
  public FieldDefinition {
    if (type == null) {
      type = "STRING"; // Default to STRING if not specified
    }
    type = type.toUpperCase();
  }

  /** Factory method for common usage */
  public static FieldDefinition of(String name, String type) {
    return new FieldDefinition(name, type);
  }

  /** Factory for string fields */
  public static FieldDefinition string(String name) {
    return new FieldDefinition(name, "STRING");
  }

  /** Factory for integer fields */
  public static FieldDefinition integer(String name) {
    return new FieldDefinition(name, "INT");
  }

  /** Factory for decimal fields */
  public static FieldDefinition decimal(String name) {
    return new FieldDefinition(name, "DECIMAL");
  }

  /** Factory for UUID fields */
  public static FieldDefinition uuid(String name) {
    return new FieldDefinition(name, "UUID");
  }

  /** Factory for date fields */
  public static FieldDefinition date(String name) {
    return new FieldDefinition(name, "DATE");
  }

  /** Factory for timestamp fields */
  public static FieldDefinition timestamp(String name) {
    return new FieldDefinition(name, "TIMESTAMP");
  }

  /** Factory for numeric fields */
  public static FieldDefinition numeric(String name) {
    return new FieldDefinition(name, "NUMERIC");
  }

  /** Check if this is a numeric type */
  public boolean isNumeric() {
    return type.matches("INT|INTEGER|BIGINT|SMALLINT|DECIMAL|NUMERIC|FLOAT|DOUBLE");
  }

  /** Check if this is a date/time type */
  public boolean isDateTime() {
    return type.matches("DATE|TIMESTAMP|DATETIME");
  }

  /** Check if this is a string type */
  public boolean isString() {
    return type.matches("STRING|VARCHAR|CHAR|TEXT");
  }

  /** Check if this is a UUID type */
  public boolean isUUID() {
    return type.matches("UUID|UUIDV4");
  }
}
