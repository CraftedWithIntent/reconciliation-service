package com.reconcile.generator;

import com.reconcile.config.DatabaseConfig;
import com.reconcile.config.DomainConfig;

/**
 * Interface for database-specific SQL view generation. Each database dialect implements this to
 * generate proper MD5 hash computation.
 */
public interface SqlGenerator {

  /**
   * Generate CREATE VIEW statement with MD5 hash column for a database config. This creates the
   * main view that includes the hash column.
   *
   * @param domainConfig the domain configuration
   * @param dbConfig the database configuration (source or target)
   * @param viewName the view name (typically {table}{viewSuffix})
   * @return SQL CREATE VIEW statement
   */
  String generateHashView(DomainConfig domainConfig, DatabaseConfig dbConfig, String viewName);

  /**
   * Get the MD5 hash function call for this database. Example outputs: - PostgreSQL: "md5(...)" -
   * Oracle: "LOWER(DBMS_CRYPTO.Hash(UTL_RAW.CAST_TO_RAW(...), 2))" - MySQL: "MD5(...)" - SQL
   * Server: "HASHBYTES('MD5', ...)"
   *
   * @param concatenatedFields the SQL expression to hash (already concatenated)
   * @return SQL hash function call
   */
  String generateHashFunction(String concatenatedFields);

  /**
   * Generate type cast for a field to string for concatenation. Example outputs: - PostgreSQL:
   * "field::text" - Oracle: "TO_CHAR(field)" - MySQL: "CAST(field AS CHAR)" - SQL Server:
   * "CAST(field AS VARCHAR(MAX))"
   *
   * @param fieldName the field name
   * @param fieldType the field type from FieldDefinition
   * @return SQL cast expression
   */
  String generateCast(String fieldName, String fieldType);

  /**
   * Generate COALESCE/NVL expression for NULL handling. Example outputs: - PostgreSQL:
   * "COALESCE(field, '')" - Oracle: "NVL(field, '')" - MySQL: "COALESCE(field, '')" - SQL Server:
   * "ISNULL(field, '')"
   *
   * @param expression the expression to coalesce
   * @param defaultValue the default value for NULLs
   * @return SQL null-handling expression
   */
  String generateCoalesce(String expression, String defaultValue);

  /**
   * Get the separator character for hash field concatenation.
   *
   * @return separator like '|' or other delimiter
   */
  String getHashFieldSeparator();

  /**
   * Get the database-specific column concatenation operator. Example outputs: - PostgreSQL: "||" -
   * Oracle: "||" - MySQL: "CONCAT(...)" - SQL Server: "+"
   *
   * @return concatenation operator
   */
  String getConcatOperator();
}
