package com.reconcile.generator;

import com.reconcile.config.DataSourceType;
import com.reconcile.config.DatabaseConfig;
import com.reconcile.config.DomainConfig;
import org.springframework.stereotype.Component;

/**
 * Factory and coordinator for SQL view generation. Generates database-specific CREATE VIEW
 * statements from domain configurations.
 *
 * <p>Supports: - PostgreSQL - Oracle - MySQL - SQL Server
 *
 * <p>Usage: SqlViewGenerator generator = new SqlViewGenerator(); String createViewSql =
 * generator.generateHashView(domainConfig, sourceConfig);
 */
@Component
public class SqlViewGenerator {

  /**
   * Generate CREATE VIEW statement for a domain's database configuration. Uses the database type
   * from the config to select appropriate SQL dialect.
   *
   * @param domainConfig the domain configuration with source and target
   * @param dbConfig the database configuration (source or target)
   * @return SQL CREATE VIEW statement
   */
  public String generateHashView(DomainConfig domainConfig, DatabaseConfig dbConfig) {
    if (dbConfig == null) {
      throw new IllegalArgumentException("Database config cannot be null");
    }

    SqlGenerator generator = getGeneratorForType(dbConfig.type());
    String viewName = dbConfig.getFullViewName();

    return generator.generateHashView(domainConfig, dbConfig, viewName);
  }

  /**
   * Generate both source and target CREATE VIEW statements for a domain.
   *
   * @param domainConfig the domain configuration
   * @return object containing both source and target CREATE VIEW statements
   */
  public ViewGenerationResult generateViewsForDomain(DomainConfig domainConfig) {
    String sourceView = generateHashView(domainConfig, domainConfig.source());
    String targetView = generateHashView(domainConfig, domainConfig.target());

    return new ViewGenerationResult(domainConfig.name(), sourceView, targetView);
  }

  /**
   * Generate CREATE VIEW statement for a specific database type.
   *
   * @param domainConfig the domain configuration
   * @param dbConfig the database configuration
   * @param databaseType the database type (may override dbConfig.type())
   * @return SQL CREATE VIEW statement
   */
  public String generateHashView(
      DomainConfig domainConfig, DatabaseConfig dbConfig, DataSourceType databaseType) {
    SqlGenerator generator = getGeneratorForType(databaseType);
    String viewName = dbConfig.getFullViewName();

    return generator.generateHashView(domainConfig, dbConfig, viewName);
  }

  /**
   * Get the appropriate SQL generator for a database type.
   *
   * @param dbType the database type
   * @return SQL generator for that database
   * @throws IllegalArgumentException if database type is not supported
   */
  private SqlGenerator getGeneratorForType(DataSourceType dbType) {
    if (dbType == null) {
      throw new IllegalArgumentException("Database type cannot be null");
    }

    switch (dbType) {
      case POSTGRESQL:
        return new PostgreSqlGenerator();
      case ORACLE:
        return new OracleSqlGenerator();
      case MYSQL:
        return new MySqlGenerator();
      case SQL_SERVER:
        return new SqlServerGenerator();
      default:
        throw new IllegalArgumentException("Unsupported database type: " + dbType);
    }
  }

  /** Result class for view generation. Contains both source and target view creation statements. */
  public static class ViewGenerationResult {
    private final String domainName;
    private final String sourceViewSql;
    private final String targetViewSql;

    public ViewGenerationResult(String domainName, String sourceViewSql, String targetViewSql) {
      this.domainName = domainName;
      this.sourceViewSql = sourceViewSql;
      this.targetViewSql = targetViewSql;
    }

    public String getDomainName() {
      return domainName;
    }

    public String getSourceViewSql() {
      return sourceViewSql;
    }

    public String getTargetViewSql() {
      return targetViewSql;
    }

    /** Get combined SQL for both views with separator comment. */
    public String getCombinedSql() {
      return "-- ========== SOURCE DATABASE ==========\n"
          + sourceViewSql
          + "\n\n"
          + "-- ========== TARGET DATABASE ==========\n"
          + targetViewSql;
    }

    /** Print the combined SQL to stdout. */
    public void printCombinedSql() {
      System.out.println(getCombinedSql());
    }
  }
}
