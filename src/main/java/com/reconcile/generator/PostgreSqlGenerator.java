package com.reconcile.generator;

/**
 * PostgreSQL-specific SQL view generator. Generates CREATE VIEW statements using PostgreSQL syntax.
 *
 * <p>PostgreSQL specifics: - MD5 hash: md5() function - Type cast: field::type - NULL handling:
 * COALESCE() - Concatenation: || operator
 */
public class PostgreSqlGenerator extends AbstractSqlGenerator {

  @Override
  public String generateHashFunction(String concatenatedFields) {
    // PostgreSQL: md5(expression)
    return "md5(" + concatenatedFields + ")";
  }

  @Override
  public String generateCast(String fieldName, String fieldType) {
    // PostgreSQL: field::type
    String pgType = mapToPostgresType(fieldType);
    return fieldName + "::" + pgType;
  }

  @Override
  public String generateCoalesce(String expression, String defaultValue) {
    // PostgreSQL: COALESCE(expression, defaultValue)
    return "COALESCE(" + expression + ", " + defaultValue + ")";
  }

  @Override
  public String getConcatOperator() {
    // PostgreSQL: || for string concatenation
    return "||";
  }

  /** Map reconciliation field types to PostgreSQL types. */
  private String mapToPostgresType(String fieldType) {
    if (fieldType == null) {
      return "TEXT";
    }

    String normalized = fieldType.toUpperCase();
    switch (normalized) {
      case "STRING":
      case "VARCHAR":
      case "CHAR":
        return "TEXT";
      case "INT":
      case "INTEGER":
        return "INTEGER";
      case "BIGINT":
        return "BIGINT";
      case "SMALLINT":
        return "SMALLINT";
      case "DECIMAL":
      case "NUMERIC":
        return "NUMERIC";
      case "FLOAT":
      case "DOUBLE":
        return "FLOAT";
      case "DATE":
        return "DATE";
      case "TIMESTAMP":
      case "DATETIME":
        return "TIMESTAMP";
      case "UUID":
      case "UUIDV4":
        return "UUID";
      case "BOOLEAN":
      case "BOOL":
        return "BOOLEAN";
      case "BYTES":
      case "BINARY":
        return "BYTEA";
      default:
        return "TEXT";
    }
  }
}
