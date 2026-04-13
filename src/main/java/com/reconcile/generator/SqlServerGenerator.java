package com.reconcile.generator;

/**
 * SQL Server-specific SQL view generator. Generates CREATE VIEW statements using SQL Server syntax.
 *
 * <p>SQL Server specifics: - MD5 hash: HASHBYTES('MD5', expression) then convert to hex string -
 * Type cast: CAST() - NULL handling: ISNULL() - Concatenation: + operator or CONCAT()
 */
public class SqlServerGenerator extends AbstractSqlGenerator {

  @Override
  public String generateHashFunction(String concatenatedFields) {
    // SQL Server: CONVERT(VARCHAR(32), HASHBYTES('MD5', expression), 2)
    // The 2 parameter converts binary to hex string
    return "LOWER(CONVERT(VARCHAR(32), HASHBYTES('MD5', " + concatenatedFields + "), 2))";
  }

  @Override
  public String generateCast(String fieldName, String fieldType) {
    // SQL Server: CAST(field AS type)
    String sqlServerType = mapToSqlServerType(fieldType);
    return "CAST(" + fieldName + " AS " + sqlServerType + ")";
  }

  @Override
  public String generateCoalesce(String expression, String defaultValue) {
    // SQL Server: ISNULL(expression, defaultValue)
    return "ISNULL(" + expression + ", " + defaultValue + ")";
  }

  @Override
  public String getConcatOperator() {
    // SQL Server: + for string concatenation
    return "+";
  }

  /** Map reconciliation field types to SQL Server types. */
  private String mapToSqlServerType(String fieldType) {
    if (fieldType == null) {
      return "VARCHAR(255)";
    }

    String normalized = fieldType.toUpperCase();
    switch (normalized) {
      case "STRING":
      case "VARCHAR":
      case "CHAR":
        return "VARCHAR(255)";
      case "INT":
      case "INTEGER":
        return "INT";
      case "BIGINT":
        return "BIGINT";
      case "SMALLINT":
        return "SMALLINT";
      case "DECIMAL":
      case "NUMERIC":
        return "DECIMAL(19,4)";
      case "FLOAT":
        return "FLOAT";
      case "DOUBLE":
        return "FLOAT(53)";
      case "DATE":
        return "DATE";
      case "TIMESTAMP":
      case "DATETIME":
        return "DATETIME2";
      case "UUID":
      case "UUIDV4":
        return "NVARCHAR(36)";
      case "BOOLEAN":
      case "BOOL":
        return "BIT";
      case "BYTES":
      case "BINARY":
        return "BINARY(255)";
      default:
        return "VARCHAR(255)";
    }
  }
}
