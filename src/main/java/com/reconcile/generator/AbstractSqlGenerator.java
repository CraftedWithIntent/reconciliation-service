package com.reconcile.generator;

import com.reconcile.config.DatabaseConfig;
import com.reconcile.config.DomainConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Base class for SQL view generation. Contains common logic for building hash expressions and
 * views.
 */
public abstract class AbstractSqlGenerator implements SqlGenerator {

  protected static final String HASH_COLUMN_NAME = "record_hash";

  /**
   * Generate CREATE VIEW statement with MD5 hash column. This builds the full view definition with
   * hash computation.
   */
  @Override
  public String generateHashView(
      DomainConfig domainConfig, DatabaseConfig dbConfig, String viewName) {
    if (dbConfig.idFields() == null
        || dbConfig.idFields().isEmpty()
        || dbConfig.hashFields() == null
        || dbConfig.hashFields().isEmpty()) {
      throw new IllegalArgumentException(
          "Database config must have idFields and hashFields for view generation");
    }

    String tableName = dbConfig.getFullTableName();

    // Build the hash expression
    String hashExpression = buildHashExpression(dbConfig);

    // Build the SELECT clause columns (id + hash fields)
    List<String> selectColumns = new ArrayList<>();
    selectColumns.add(dbConfig.getIdFieldName());
    selectColumns.addAll(
        dbConfig.hashFields().stream().map(field -> field.name()).collect(Collectors.toList()));

    // Build the view definition
    StringBuilder sql = new StringBuilder();
    sql.append("CREATE OR REPLACE VIEW ").append(viewName).append(" AS\n");
    sql.append("SELECT\n");

    // Add column selections
    for (String column : selectColumns) {
      sql.append("    ").append(column).append(",\n");
    }

    // Add hash column
    sql.append("    ").append(hashExpression).append(" AS ").append(HASH_COLUMN_NAME).append("\n");
    sql.append("FROM ").append(tableName).append(";");

    return sql.toString();
  }

  /**
   * Build the hash expression for this database. Concatenates hash fields with proper casting and
   * null handling.
   */
  protected String buildHashExpression(DatabaseConfig dbConfig) {
    List<String> hashFieldExpressions = new ArrayList<>();

    for (var field : dbConfig.hashFields()) {
      String fieldName = field.name();
      String fieldType = field.type();

      // Build: COALESCE(CAST(field AS VARCHAR), 'NULL')
      String cast = generateCast(fieldName, fieldType);
      String coalesced = generateCoalesce(cast, "'NULL'");
      hashFieldExpressions.add(coalesced);
    }

    // Concatenate all fields with separator
    String concatenated = buildConcatenation(hashFieldExpressions);

    // Apply hash function
    return generateHashFunction(concatenated);
  }

  /**
   * Build concatenation expression specific to this database. Subclasses can override for
   * database-specific syntax.
   */
  protected String buildConcatenation(List<String> expressions) {
    String operator = getConcatOperator();
    String separator = "'" + getHashFieldSeparator() + "'";

    StringBuilder concat = new StringBuilder();
    for (int i = 0; i < expressions.size(); i++) {
      concat.append(expressions.get(i));
      if (i < expressions.size() - 1) {
        concat
            .append(" ")
            .append(operator)
            .append(" ")
            .append(separator)
            .append(" ")
            .append(operator)
            .append(" ");
      }
    }

    return concat.toString();
  }

  /** Default hash field separator - can be overridden by subclasses. */
  @Override
  public String getHashFieldSeparator() {
    return "|";
  }
}
