package com.reconcile.generator;

/**
 * MySQL-specific SQL view generator. Generates CREATE VIEW statements using MySQL syntax.
 *
 * <p>MySQL specifics: - MD5 hash: MD5() function - Type cast: CAST(), CONCAT() - NULL handling:
 * COALESCE() - Concatenation: CONCAT() function (for MySQL 5.7+) or + operator
 */
public class MySqlGenerator extends AbstractSqlGenerator {

  @Override
  public String generateHashFunction(String concatenatedFields) {
    // MySQL: MD5(expression)
    return "MD5(" + concatenatedFields + ")";
  }

  @Override
  public String generateCast(String fieldName, String fieldType) {
    // MySQL: CAST(field AS type)
    String mysqlType = mapToMysqlType(fieldType);
    return "CAST(" + fieldName + " AS " + mysqlType + ")";
  }

  @Override
  public String generateCoalesce(String expression, String defaultValue) {
    // MySQL: COALESCE(expression, defaultValue)
    return "COALESCE(" + expression + ", " + defaultValue + ")";
  }

  @Override
  public String getConcatOperator() {
    // MySQL uses CONCAT() function instead of operator
    // This is handled specially in buildConcatenation()
    return "CONCAT";
  }

  /**
   * Build concatenation for MySQL using CONCAT() function. MySQL's CONCAT() takes multiple
   * arguments rather than binary operators.
   */
  @Override
  protected String buildConcatenation(java.util.List<String> expressions) {
    // MySQL: CONCAT(field1, '|', field2, '|', field3, ...)
    StringBuilder concat = new StringBuilder("CONCAT(");
    String separator = "'" + getHashFieldSeparator() + "'";

    for (int i = 0; i < expressions.size(); i++) {
      concat.append(expressions.get(i));
      if (i < expressions.size() - 1) {
        concat.append(", ").append(separator).append(", ");
      }
    }

    concat.append(")");
    return concat.toString();
  }

  /** Map reconciliation field types to MySQL types. */
  private String mapToMysqlType(String fieldType) {
    if (fieldType == null) {
      return "CHAR(255)";
    }

    String normalized = fieldType.toUpperCase();
    switch (normalized) {
      case "STRING":
      case "VARCHAR":
      case "CHAR":
        return "CHAR(255)";
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
        return "DOUBLE";
      case "DATE":
        return "DATE";
      case "TIMESTAMP":
      case "DATETIME":
        return "DATETIME";
      case "UUID":
      case "UUIDV4":
        return "CHAR(36)";
      case "BOOLEAN":
      case "BOOL":
        return "BOOLEAN";
      case "BYTES":
      case "BINARY":
        return "BINARY(255)";
      default:
        return "CHAR(255)";
    }
  }
}
