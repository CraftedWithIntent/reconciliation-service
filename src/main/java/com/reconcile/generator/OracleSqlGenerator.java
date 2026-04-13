package com.reconcile.generator;

/**
 * Oracle-specific SQL view generator. Generates CREATE VIEW statements using Oracle syntax.
 *
 * <p>Oracle specifics: - MD5 hash: LOWER(DBMS_CRYPTO.Hash(..., 2)) where 2 = MD5 algorithm - Type
 * cast: TO_CHAR(), CAST() - NULL handling: NVL() - Concatenation: || operator
 */
public class OracleSqlGenerator extends AbstractSqlGenerator {

  @Override
  public String generateHashFunction(String concatenatedFields) {
    // Oracle: LOWER(DBMS_CRYPTO.Hash(UTL_RAW.CAST_TO_RAW(...), 2))
    // Where 2 = DBMS_CRYPTO.HASH_MD5
    return "LOWER(RAWTOHEX(DBMS_CRYPTO.HASH(UTL_RAW.CAST_TO_RAW(" + concatenatedFields + "), 2)))";
  }

  @Override
  public String generateCast(String fieldName, String fieldType) {
    // Oracle: TO_CHAR() for most types, CAST() for some
    String oracleType = mapToOracleType(fieldType);

    String normalized = fieldType.toUpperCase();
    if (normalized.matches("^(INT|INTEGER|BIGINT|SMALLINT)$")) {
      return "TO_CHAR(" + fieldName + ")";
    } else if (normalized.matches("^(DECIMAL|NUMERIC|FLOAT|DOUBLE)$")) {
      return "TO_CHAR(" + fieldName + ")";
    } else if (normalized.matches("^(DATE|TIMESTAMP|DATETIME)$")) {
      return "TO_CHAR(" + fieldName + ", 'YYYY-MM-DD HH24:MI:SS')";
    } else if (normalized.matches("^(UUID|UUIDV4)$")) {
      return "CAST(" + fieldName + " AS VARCHAR2(36))";
    } else {
      // Default: string types
      return "CAST(" + fieldName + " AS VARCHAR2(4000))";
    }
  }

  @Override
  public String generateCoalesce(String expression, String defaultValue) {
    // Oracle: NVL(expression, defaultValue)
    return "NVL(" + expression + ", " + defaultValue + ")";
  }

  @Override
  public String getConcatOperator() {
    // Oracle: || for string concatenation
    return "||";
  }

  /** Map reconciliation field types to Oracle types. */
  private String mapToOracleType(String fieldType) {
    if (fieldType == null) {
      return "VARCHAR2(4000)";
    }

    String normalized = fieldType.toUpperCase();
    switch (normalized) {
      case "STRING":
      case "VARCHAR":
      case "CHAR":
        return "VARCHAR2(4000)";
      case "INT":
      case "INTEGER":
        return "NUMBER(10)";
      case "BIGINT":
        return "NUMBER(19)";
      case "SMALLINT":
        return "NUMBER(5)";
      case "DECIMAL":
      case "NUMERIC":
        return "NUMBER(19,4)";
      case "FLOAT":
      case "DOUBLE":
        return "NUMBER";
      case "DATE":
        return "DATE";
      case "TIMESTAMP":
      case "DATETIME":
        return "TIMESTAMP";
      case "UUID":
      case "UUIDV4":
        return "VARCHAR2(36)";
      case "BOOLEAN":
      case "BOOL":
        return "NUMBER(1)";
      case "BYTES":
      case "BINARY":
        return "RAW(2000)";
      default:
        return "VARCHAR2(4000)";
    }
  }
}
