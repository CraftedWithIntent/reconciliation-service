package com.reconcile.config;

/** Supported database source types for reconciliation */
public enum DataSourceType {
  POSTGRESQL("postgres", "org.postgresql.Driver"),
  ORACLE("oracle", "oracle.jdbc.driver.OracleDriver"),
  MYSQL("mysql", "com.mysql.cj.jdbc.Driver"),
  SQL_SERVER("sqlserver", "com.microsoft.sqlserver.jdbc.SQLServerDriver");

  private final String name;
  private final String driverClass;

  DataSourceType(String name, String driverClass) {
    this.name = name;
    this.driverClass = driverClass;
  }

  public String getName() {
    return name;
  }

  public String getDriverClass() {
    return driverClass;
  }

  /** Find DataSourceType by name (case-insensitive) */
  public static DataSourceType fromName(String name) {
    for (DataSourceType type : DataSourceType.values()) {
      if (type.name.equalsIgnoreCase(name)) {
        return type;
      }
    }
    throw new IllegalArgumentException("Unknown data source type: " + name);
  }

  /** Detect DataSourceType from JDBC URL (fallback for backwards compatibility) */
  public static DataSourceType fromUrl(String url) {
    if (url == null || url.isEmpty()) {
      throw new IllegalArgumentException("JDBC URL cannot be null or empty");
    }

    if (url.contains(":postgresql:")) {
      return POSTGRESQL;
    } else if (url.contains(":oracle:")) {
      return ORACLE;
    } else if (url.contains(":mysql:")) {
      return MYSQL;
    } else if (url.contains(":sqlserver:")) {
      return SQL_SERVER;
    }

    throw new IllegalArgumentException("Cannot detect data source type from URL: " + url);
  }
}
