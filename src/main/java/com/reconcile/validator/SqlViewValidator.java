package com.reconcile.validator;

import com.reconcile.config.DatabaseConfig;
import com.reconcile.config.DomainConfig;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Validator for SQL views against configuration. Checks if views exist in source and target
 * databases and have expected columns.
 */
@Component
public class SqlViewValidator {

  @Autowired(required = false)
  private Map<String, DataSource> dataSources;

  /**
   * Validates SQL views exist and have expected structure for a domain
   *
   * @param domainConfig the domain configuration
   * @param sourceDataSource the source database datasource
   * @param targetDataSource the target database datasource
   * @return list of validation errors (empty if valid)
   */
  public List<String> validateViews(
      DomainConfig domainConfig, DataSource sourceDataSource, DataSource targetDataSource) {

    List<String> errors = new ArrayList<>();

    if (domainConfig == null) {
      errors.add("Domain configuration cannot be null");
      return errors;
    }

    // Validate source view
    if (sourceDataSource != null && domainConfig.source() != null) {
      errors.addAll(validateView(domainConfig.source(), sourceDataSource, "source"));
    }

    // Validate target view
    if (targetDataSource != null && domainConfig.target() != null) {
      errors.addAll(validateView(domainConfig.target(), targetDataSource, "target"));
    }

    return errors;
  }

  /**
   * Validates a single view in a database
   *
   * @param dbConfig the database configuration (with view suffix)
   * @param dataSource the database datasource
   * @param dbType "source" or "target"
   * @return list of validation errors for this view
   */
  public List<String> validateView(DatabaseConfig dbConfig, DataSource dataSource, String dbType) {
    List<String> errors = new ArrayList<>();

    if (dbConfig == null || dataSource == null) {
      return errors;
    }

    String schema = dbConfig.schema();
    String table = dbConfig.table();
    String viewSuffix = dbConfig.effectiveViewSuffix();
    String viewName = table + viewSuffix;
    String fullViewName = schema + "." + viewName;

    try (Connection conn = dataSource.getConnection()) {
      DatabaseMetaData metaData = conn.getMetaData();

      // Check if view exists
      if (!viewExists(metaData, schema, viewName)) {
        errors.add(dbType + " view does not exist: " + fullViewName);
        return errors; // Can't validate columns if view doesn't exist
      }

      // Get expected columns
      Set<String> expectedColumns = new HashSet<>();
      expectedColumns.add(dbConfig.getIdFieldName());
      if (dbConfig.hashFields() != null) {
        for (var field : dbConfig.hashFields()) {
          expectedColumns.add(field.name());
        }
      }
      // Also expect a hash column
      expectedColumns.add("record_hash");
      expectedColumns.add("_record_hash");
      expectedColumns.add("hash"); // Accept multiple possible hash column names

      // Get actual columns in view
      Set<String> actualColumns = getViewColumns(metaData, schema, viewName);

      if (actualColumns.isEmpty()) {
        errors.add(dbType + " view exists but has no columns: " + fullViewName);
        return errors;
      }

      // Validate idField exists
      if (!hasColumn(actualColumns, dbConfig.getIdFieldName())) {
        errors.add(
            dbType + " view '" + fullViewName + "' missing idField: " + dbConfig.getIdFieldName());
      }

      // Validate hashFields exist
      if (dbConfig.hashFields() != null) {
        for (int i = 0; i < dbConfig.hashFields().size(); i++) {
          String fieldName = dbConfig.hashFields().get(i).name();
          if (!hasColumn(actualColumns, fieldName)) {
            errors.add(
                dbType + " view '" + fullViewName + "' missing hashField[" + i + "]: " + fieldName);
          }
        }
      }

      // Validate hash column exists (at least one)
      if (!hasAnyColumn(actualColumns, "record_hash", "_record_hash", "hash")) {
        errors.add(
            dbType
                + " view '"
                + fullViewName
                + "' missing hash column (expected: record_hash, _record_hash, or hash)");
      }

    } catch (SQLException e) {
      errors.add(dbType + " view validation failed: " + e.getMessage());
    }

    return errors;
  }

  /**
   * Checks if a view exists in the database
   *
   * @param metaData database metadata
   * @param schema schema name
   * @param viewName view name
   * @return true if view exists
   */
  private boolean viewExists(DatabaseMetaData metaData, String schema, String viewName) {
    try {
      // Try to get view columns
      ResultSet rs = metaData.getColumns(null, schema, viewName, null);
      boolean exists = rs.next();
      rs.close();
      return exists;
    } catch (SQLException e) {
      return false;
    }
  }

  /**
   * Gets all column names from a view
   *
   * @param metaData database metadata
   * @param schema schema name
   * @param viewName view name
   * @return set of column names (case-insensitive)
   */
  private Set<String> getViewColumns(DatabaseMetaData metaData, String schema, String viewName) {
    Set<String> columns = new HashSet<>();

    try {
      ResultSet rs = metaData.getColumns(null, schema, viewName, null);
      while (rs.next()) {
        String columnName = rs.getString("COLUMN_NAME");
        if (columnName != null) {
          columns.add(columnName.toLowerCase());
        }
      }
      rs.close();
    } catch (SQLException e) {
      // Silently fail - will be caught by viewExists check
    }

    return columns;
  }

  /**
   * Checks if a column exists in the set (case-insensitive)
   *
   * @param columns set of actual column names (lowercase)
   * @param columnName column to find
   * @return true if column exists
   */
  private boolean hasColumn(Set<String> columns, String columnName) {
    if (columnName == null) {
      return false;
    }
    return columns.contains(columnName.toLowerCase());
  }

  /**
   * Checks if any of the given columns exist in the set (case-insensitive)
   *
   * @param columns set of actual column names (lowercase)
   * @param columnNames potential column names to find
   * @return true if any column exists
   */
  private boolean hasAnyColumn(Set<String> columns, String... columnNames) {
    for (String name : columnNames) {
      if (hasColumn(columns, name)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Throws an exception if view validation fails
   *
   * @param domainConfig the domain configuration
   * @param sourceDataSource source database
   * @param targetDataSource target database
   */
  public void validateViewsOrThrow(
      DomainConfig domainConfig, DataSource sourceDataSource, DataSource targetDataSource) {

    List<String> errors = validateViews(domainConfig, sourceDataSource, targetDataSource);
    if (!errors.isEmpty()) {
      throw new IllegalStateException("Invalid view configuration: " + String.join("; ", errors));
    }
  }
}
