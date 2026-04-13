package com.reconcile.config;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("DataSourceType Enum Tests")
class DataSourceTypeTest {

  @Test
  @DisplayName("Should have all required database types")
  void testEnumValues() {
    assertEquals(4, DataSourceType.values().length);
    assertNotNull(DataSourceType.POSTGRESQL);
    assertNotNull(DataSourceType.ORACLE);
    assertNotNull(DataSourceType.MYSQL);
    assertNotNull(DataSourceType.SQL_SERVER);
  }

  @Test
  @DisplayName("Should have correct name for PostgreSQL")
  void testPostgresqlName() {
    assertEquals("postgres", DataSourceType.POSTGRESQL.getName());
    assertEquals("org.postgresql.Driver", DataSourceType.POSTGRESQL.getDriverClass());
  }

  @Test
  @DisplayName("Should have correct name for Oracle")
  void testOracleName() {
    assertEquals("oracle", DataSourceType.ORACLE.getName());
    assertEquals("oracle.jdbc.driver.OracleDriver", DataSourceType.ORACLE.getDriverClass());
  }

  @Test
  @DisplayName("Should have correct name for MySQL")
  void testMysqlName() {
    assertEquals("mysql", DataSourceType.MYSQL.getName());
    assertEquals("com.mysql.cj.jdbc.Driver", DataSourceType.MYSQL.getDriverClass());
  }

  @Test
  @DisplayName("Should have correct name for SQL Server")
  void testSqlServerName() {
    assertEquals("sqlserver", DataSourceType.SQL_SERVER.getName());
    assertEquals(
        "com.microsoft.sqlserver.jdbc.SQLServerDriver", DataSourceType.SQL_SERVER.getDriverClass());
  }

  @Test
  @DisplayName("Should find type by name (case-insensitive)")
  void testFromNameCaseInsensitive() {
    assertEquals(DataSourceType.POSTGRESQL, DataSourceType.fromName("postgres"));
    assertEquals(DataSourceType.POSTGRESQL, DataSourceType.fromName("POSTGRES"));
    assertEquals(DataSourceType.POSTGRESQL, DataSourceType.fromName("PoStGrEs"));

    assertEquals(DataSourceType.ORACLE, DataSourceType.fromName("oracle"));
    assertEquals(DataSourceType.ORACLE, DataSourceType.fromName("ORACLE"));

    assertEquals(DataSourceType.MYSQL, DataSourceType.fromName("mysql"));
    assertEquals(DataSourceType.MYSQL, DataSourceType.fromName("MYSQL"));

    assertEquals(DataSourceType.SQL_SERVER, DataSourceType.fromName("sqlserver"));
    assertEquals(DataSourceType.SQL_SERVER, DataSourceType.fromName("SQLSERVER"));
  }

  @Test
  @DisplayName("Should throw exception for unknown type name")
  void testFromNameUnknown() {
    assertThrows(IllegalArgumentException.class, () -> DataSourceType.fromName("unknown"));
    assertThrows(IllegalArgumentException.class, () -> DataSourceType.fromName("mssql"));
    assertThrows(IllegalArgumentException.class, () -> DataSourceType.fromName(""));
  }

  @Test
  @DisplayName("Should detect type from PostgreSQL JDBC URL")
  void testFromUrlPostgresql() {
    assertEquals(
        DataSourceType.POSTGRESQL,
        DataSourceType.fromUrl("jdbc:postgresql://localhost:5432/testdb"));
    assertEquals(
        DataSourceType.POSTGRESQL,
        DataSourceType.fromUrl("jdbc:postgresql://192.168.1.1:5432/mydb"));
    assertEquals(
        DataSourceType.POSTGRESQL, DataSourceType.fromUrl("jdbc:postgresql://db.example.com/prod"));
  }

  @Test
  @DisplayName("Should detect type from Oracle JDBC URL")
  void testFromUrlOracle() {
    assertEquals(
        DataSourceType.ORACLE, DataSourceType.fromUrl("jdbc:oracle:thin:@localhost:1521:testdb"));
    assertEquals(
        DataSourceType.ORACLE,
        DataSourceType.fromUrl("jdbc:oracle:thin:@//db.example.com:1521/prod"));
  }

  @Test
  @DisplayName("Should detect type from MySQL JDBC URL")
  void testFromUrlMysql() {
    assertEquals(
        DataSourceType.MYSQL, DataSourceType.fromUrl("jdbc:mysql://localhost:3306/testdb"));
    assertEquals(
        DataSourceType.MYSQL,
        DataSourceType.fromUrl("jdbc:mysql://db.example.com/prod?useSSL=true"));
  }

  @Test
  @DisplayName("Should detect type from SQL Server JDBC URL")
  void testFromUrlSqlServer() {
    assertEquals(
        DataSourceType.SQL_SERVER,
        DataSourceType.fromUrl("jdbc:sqlserver://localhost:1433;databaseName=testdb"));
    assertEquals(
        DataSourceType.SQL_SERVER,
        DataSourceType.fromUrl("jdbc:sqlserver://db.example.com:1433;databaseName=prod"));
  }

  @Test
  @DisplayName("Should throw exception for null URL")
  void testFromUrlNullThrows() {
    assertThrows(IllegalArgumentException.class, () -> DataSourceType.fromUrl(null));
  }

  @Test
  @DisplayName("Should throw exception for empty URL")
  void testFromUrlEmptyThrows() {
    assertThrows(IllegalArgumentException.class, () -> DataSourceType.fromUrl(""));
  }

  @Test
  @DisplayName("Should throw exception for unknown JDBC URL format")
  void testFromUrlUnknownThrows() {
    assertThrows(
        IllegalArgumentException.class,
        () -> DataSourceType.fromUrl("jdbc:derby://localhost/testdb"));
    assertThrows(IllegalArgumentException.class, () -> DataSourceType.fromUrl("not-a-jdbc-url"));
  }
}
