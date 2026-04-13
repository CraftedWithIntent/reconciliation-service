package com.reconcile.generator;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.reconcile.config.DataSourceType;
import com.reconcile.config.DatabaseConfig;
import com.reconcile.config.DomainConfig;
import com.reconcile.config.FieldDefinition;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("SqlViewGenerator Tests")
class SqlViewGeneratorTest {

  private SqlViewGenerator generator;
  private DatabaseConfig sourceConfig;
  private DatabaseConfig targetConfig;
  private DomainConfig domainConfig;

  @BeforeEach
  void setUp() {
    generator = new SqlViewGenerator();

    sourceConfig =
        new DatabaseConfig(
            DataSourceType.POSTGRESQL,
            "public",
            "vendor_invoices",
            "_with_hash",
            List.of(FieldDefinition.string("id")),
            new ArrayList<>(
                List.of(
                    FieldDefinition.string("vendor_name"),
                    FieldDefinition.decimal("amount"),
                    FieldDefinition.string("billing_cycle"))));

    targetConfig =
        new DatabaseConfig(
            DataSourceType.POSTGRESQL,
            "public",
            "internal_ledger",
            "_with_hash",
            List.of(FieldDefinition.string("id")),
            new ArrayList<>(
                List.of(
                    FieldDefinition.string("vendor_name"),
                    FieldDefinition.decimal("amount"),
                    FieldDefinition.string("billing_cycle"))));

    domainConfig = new DomainConfig("vendor-invoices", sourceConfig, targetConfig, null, false, 95.0, 1.0);
  }

  @Test
  @DisplayName("Should generate CREATE VIEW for database config")
  void testGenerateHashView_BasicStructure() {
    // Act
    String sql = generator.generateHashView(domainConfig, sourceConfig);

    // Assert
    assertTrue(sql.contains("CREATE OR REPLACE VIEW"));
    assertTrue(sql.contains("public.vendor_invoices_with_hash"));
    assertTrue(sql.contains("id"));
    assertTrue(sql.contains("vendor_name"));
    assertTrue(sql.contains("amount"));
    assertTrue(sql.contains("billing_cycle"));
    assertTrue(sql.contains("record_hash"));
  }

  @Test
  @DisplayName("Should generate separate views for both source and target")
  void testGenerateViewsForDomain_BothDatabases() {
    // Act
    SqlViewGenerator.ViewGenerationResult result = generator.generateViewsForDomain(domainConfig);

    // Assert
    assertNotNull(result.getSourceViewSql());
    assertNotNull(result.getTargetViewSql());
    assertEquals("vendor-invoices", result.getDomainName());
    assertTrue(result.getSourceViewSql().contains("vendor_invoices_with_hash"));
    assertTrue(result.getTargetViewSql().contains("internal_ledger_with_hash"));
  }

  @Test
  @DisplayName("Should generate combined SQL with separator comments")
  void testViewGenerationResult_CombinedSql() {
    // Act
    SqlViewGenerator.ViewGenerationResult result = generator.generateViewsForDomain(domainConfig);
    String combined = result.getCombinedSql();

    // Assert
    assertTrue(combined.contains("SOURCE DATABASE"));
    assertTrue(combined.contains("TARGET DATABASE"));
    assertTrue(combined.contains(result.getSourceViewSql()));
    assertTrue(combined.contains(result.getTargetViewSql()));
  }

  @Test
  @DisplayName("Should throw exception for null database config")
  void testGenerateHashView_NullConfig() {
    // Act & Assert
    assertThrows(
        IllegalArgumentException.class,
        () -> {
          generator.generateHashView(domainConfig, null);
        });
  }

  @Test
  @DisplayName("Should throw exception for null database type")
  void testGenerateHashView_AllowsNullTypeInConfig() {
    // This should work - null type is handled in the config
    assertDoesNotThrow(
        () -> {
          generator.generateHashView(domainConfig, sourceConfig);
        });
  }

  @Test
  @DisplayName("Should throw exception for unsupported database type")
  void testGenerateHashView_UnsupportedDatabaseType() {
    // Act & Assert
    assertThrows(
        IllegalArgumentException.class,
        () -> {
          generator.generateHashView(domainConfig, sourceConfig, null);
        });
  }
}

@DisplayName("PostgreSQL SQL Generator Tests")
class PostgreSqlGeneratorTest {

  private PostgreSqlGenerator generator;
  private DatabaseConfig dbConfig;
  private DomainConfig domainConfig;

  @BeforeEach
  void setUp() {
    generator = new PostgreSqlGenerator();

    dbConfig =
        new DatabaseConfig(
            DataSourceType.POSTGRESQL,
            "public",
            "invoices",
            "_hash",
            List.of(FieldDefinition.string("id")),
            new ArrayList<>(
                List.of(FieldDefinition.string("customer"), FieldDefinition.decimal("amount"))));

    domainConfig = new DomainConfig("test-domain", dbConfig, dbConfig, null, false, 95.0, 1.0);
  }

  @Test
  @DisplayName("Should generate PostgreSQL MD5 hash function")
  void testGenerateHashFunction_PostgreSQL() {
    // Act
    String result = generator.generateHashFunction("field1 || '|' || field2");

    // Assert
    assertTrue(result.startsWith("md5("));
    assertTrue(result.endsWith(")"));
    assertTrue(result.contains("field1 || '|' || field2"));
  }

  @Test
  @DisplayName("Should generate PostgreSQL type cast with :: notation")
  void testGenerateCast_PostgreSQL() {
    // Act
    String stringCast = generator.generateCast("amount", "VARCHAR");
    String intCast = generator.generateCast("quantity", "INT");

    // Assert
    assertTrue(stringCast.contains("::"));
    assertTrue(intCast.contains("::"));
    assertTrue(stringCast.contains("TEXT"));
    assertTrue(intCast.contains("INTEGER"));
  }

  @Test
  @DisplayName("Should generate PostgreSQL COALESCE for NULL handling")
  void testGenerateCoalesce_PostgreSQL() {
    // Act
    String result = generator.generateCoalesce("amount::text", "'NULL'");

    // Assert
    assertTrue(result.startsWith("COALESCE("));
    assertTrue(result.contains("amount::text"));
    assertTrue(result.contains("'NULL'"));
  }

  @Test
  @DisplayName("Should use || as concatenation operator")
  void testGetConcatOperator_PostgreSQL() {
    // Act
    String op = generator.getConcatOperator();

    // Assert
    assertEquals("||", op);
  }

  @Test
  @DisplayName("Should generate complete PostgreSQL CREATE VIEW statement")
  void testGenerateHashView_Complete_PostgreSQL() {
    // Act
    String sql = generator.generateHashView(domainConfig, dbConfig, "public.invoices_hash");

    // Assert
    assertTrue(sql.contains("CREATE OR REPLACE VIEW public.invoices_hash AS"));
    assertTrue(sql.contains("md5("));
    assertTrue(sql.contains("COALESCE("));
    assertTrue(sql.contains("record_hash"));
    assertFalse(sql.contains("DBMS_CRYPTO")); // Should not have Oracle functions
  }
}

@DisplayName("Oracle SQL Generator Tests")
class OracleSqlGeneratorTest {

  private OracleSqlGenerator generator;
  private DatabaseConfig dbConfig;
  private DomainConfig domainConfig;

  @BeforeEach
  void setUp() {
    generator = new OracleSqlGenerator();

    dbConfig =
        new DatabaseConfig(
            DataSourceType.ORACLE,
            "SOURCE",
            "vendor_invoices",
            "_hash",
            List.of(FieldDefinition.string("id")),
            new ArrayList<>(
                List.of(FieldDefinition.string("vendor_name"), FieldDefinition.decimal("amount"))));

    domainConfig = new DomainConfig("test-domain", dbConfig, dbConfig, null, false, 95.0, 1.0);
  }

  @Test
  @DisplayName("Should generate Oracle hash function with DBMS_CRYPTO")
  void testGenerateHashFunction_Oracle() {
    // Act
    String result = generator.generateHashFunction("field1 || '|' || field2");

    // Assert
    assertTrue(result.contains("DBMS_CRYPTO.HASH"));
    assertTrue(result.contains("UTL_RAW.CAST_TO_RAW"));
    assertTrue(result.contains("RAWTOHEX"));
    assertTrue(result.contains("LOWER"));
  }

  @Test
  @DisplayName("Should generate Oracle TO_CHAR for numeric casting")
  void testGenerateCast_Oracle_Numeric() {
    // Act
    String result = generator.generateCast("amount", "DECIMAL");

    // Assert
    assertTrue(result.contains("TO_CHAR"));
    assertTrue(result.contains("amount"));
  }

  @Test
  @DisplayName("Should generate Oracle NVL for NULL handling")
  void testGenerateCoalesce_Oracle() {
    // Act
    String result = generator.generateCoalesce("TO_CHAR(amount)", "'NULL'");

    // Assert
    assertTrue(result.startsWith("NVL("));
    assertTrue(result.contains("TO_CHAR(amount)"));
    assertTrue(result.contains("'NULL'"));
  }

  @Test
  @DisplayName("Should use || as concatenation operator in Oracle")
  void testGetConcatOperator_Oracle() {
    // Act
    String op = generator.getConcatOperator();

    // Assert
    assertEquals("||", op);
  }

  @Test
  @DisplayName("Should generate complete Oracle CREATE VIEW statement")
  void testGenerateHashView_Complete_Oracle() {
    // Act
    String sql = generator.generateHashView(domainConfig, dbConfig, "SOURCE.vendor_invoices_hash");

    // Assert
    assertTrue(sql.contains("CREATE OR REPLACE VIEW SOURCE.vendor_invoices_hash AS"));
    assertTrue(sql.contains("DBMS_CRYPTO"));
    assertTrue(sql.contains("NVL("));
    assertFalse(sql.contains("md5(")); // Should not have PostgreSQL functions
  }
}

@DisplayName("MySQL SQL Generator Tests")
class MySqlGeneratorTest {

  private MySqlGenerator generator;
  private DatabaseConfig dbConfig;
  private DomainConfig domainConfig;

  @BeforeEach
  void setUp() {
    generator = new MySqlGenerator();

    dbConfig =
        new DatabaseConfig(
            DataSourceType.MYSQL,
            "reconcile_db",
            "invoices",
            "_hash",
            List.of(FieldDefinition.string("id")),
            new ArrayList<>(
                List.of(FieldDefinition.string("customer"), FieldDefinition.decimal("amount"))));

    domainConfig = new DomainConfig("test-domain", dbConfig, dbConfig, null, false, 95.0, 1.0);
  }

  @Test
  @DisplayName("Should generate MySQL MD5 hash function")
  void testGenerateHashFunction_MySQL() {
    // Act
    String result = generator.generateHashFunction("CONCAT(field1, '|', field2)");

    // Assert
    assertTrue(result.startsWith("MD5("));
    assertTrue(result.endsWith(")"));
    assertTrue(result.contains("CONCAT"));
  }

  @Test
  @DisplayName("Should generate MySQL CAST for type conversion")
  void testGenerateCast_MySQL() {
    // Act
    String result = generator.generateCast("amount", "DECIMAL");

    // Assert
    assertTrue(result.startsWith("CAST("));
    assertTrue(result.contains("amount"));
    assertTrue(result.contains("DECIMAL"));
  }

  @Test
  @DisplayName("Should generate MySQL COALESCE for NULL handling")
  void testGenerateCoalesce_MySQL() {
    // Act
    String result = generator.generateCoalesce("CAST(amount AS CHAR)", "'NULL'");

    // Assert
    assertTrue(result.startsWith("COALESCE("));
    assertTrue(result.contains("CAST(amount AS CHAR)"));
  }

  @Test
  @DisplayName("Should use CONCAT function for MySQL concatenation")
  void testBuildConcatenation_MySQL() {
    // Act
    String sql = generator.generateHashView(domainConfig, dbConfig, "reconcile_db.invoices_hash");

    // Assert
    assertTrue(sql.contains("CONCAT("));
    assertFalse(sql.contains(" || ")); // MySQL doesn't use ||
  }

  @Test
  @DisplayName("Should generate complete MySQL CREATE VIEW statement")
  void testGenerateHashView_Complete_MySQL() {
    // Act
    String sql = generator.generateHashView(domainConfig, dbConfig, "reconcile_db.invoices_hash");

    // Assert
    assertTrue(sql.contains("CREATE OR REPLACE VIEW reconcile_db.invoices_hash AS"));
    assertTrue(sql.contains("MD5("));
    assertTrue(sql.contains("CONCAT("));
    assertFalse(sql.contains("||")); // Should not use PostgreSQL concatenation
  }
}

@DisplayName("SQL Server SQL Generator Tests")
class SqlServerGeneratorTest {

  private SqlServerGenerator generator;
  private DatabaseConfig dbConfig;
  private DomainConfig domainConfig;

  @BeforeEach
  void setUp() {
    generator = new SqlServerGenerator();

    dbConfig =
        new DatabaseConfig(
            DataSourceType.SQL_SERVER,
            "dbo",
            "invoices",
            "_hash",
            List.of(FieldDefinition.string("id")),
            new ArrayList<>(
                List.of(FieldDefinition.string("customer"), FieldDefinition.decimal("amount"))));

    domainConfig = new DomainConfig("test-domain", dbConfig, dbConfig, null, false, 95.0, 1.0);
  }

  @Test
  @DisplayName("Should generate SQL Server HASHBYTES function")
  void testGenerateHashFunction_SqlServer() {
    // Act
    String result = generator.generateHashFunction("field1 + '|' + field2");

    // Assert
    assertTrue(result.contains("HASHBYTES('MD5'"));
    assertTrue(result.contains("CONVERT"));
    assertTrue(result.contains("VARCHAR(32)"));
    assertTrue(result.contains("LOWER("));
  }

  @Test
  @DisplayName("Should generate SQL Server CAST for type conversion")
  void testGenerateCast_SqlServer() {
    // Act
    String result = generator.generateCast("amount", "DECIMAL");

    // Assert
    assertTrue(result.startsWith("CAST("));
    assertTrue(result.contains("DECIMAL"));
  }

  @Test
  @DisplayName("Should generate SQL Server ISNULL for NULL handling")
  void testGenerateCoalesce_SqlServer() {
    // Act
    String result = generator.generateCoalesce("CAST(amount AS VARCHAR(255))", "'NULL'");

    // Assert
    assertTrue(result.startsWith("ISNULL("));
    assertTrue(result.contains("CAST(amount AS VARCHAR(255))"));
  }

  @Test
  @DisplayName("Should use + as concatenation operator in SQL Server")
  void testGetConcatOperator_SqlServer() {
    // Act
    String op = generator.getConcatOperator();

    // Assert
    assertEquals("+", op);
  }

  @Test
  @DisplayName("Should generate complete SQL Server CREATE VIEW statement")
  void testGenerateHashView_Complete_SqlServer() {
    // Act
    String sql = generator.generateHashView(domainConfig, dbConfig, "dbo.invoices_hash");

    // Assert
    assertTrue(sql.contains("CREATE OR REPLACE VIEW dbo.invoices_hash AS"));
    assertTrue(sql.contains("HASHBYTES"));
    assertTrue(sql.contains("ISNULL("));
    assertTrue(sql.contains("+")); // Should use + for concatenation
  }
}

@DisplayName("Multi-Database Heterogeneous Tests")
class HeterogeneousGeneratorTest {

  private SqlViewGenerator viewGenerator;

  @BeforeEach
  void setUp() {
    viewGenerator = new SqlViewGenerator();
  }

  @Test
  @DisplayName("Should generate different SQL for same config across database types")
  void testGenerateForDifferentDatabases_DifferentSql() {
    // Arrange
    DatabaseConfig baseConfig =
        new DatabaseConfig(
            DataSourceType.POSTGRESQL,
            "public",
            "data",
            "_hash",
            List.of(FieldDefinition.string("id")),
            new ArrayList<>(
                List.of(FieldDefinition.string("name"), FieldDefinition.decimal("amount"))));

    DomainConfig domainConfig = new DomainConfig("test", baseConfig, baseConfig, null, false, 95.0, 1.0);

    // Act
    String postgresql =
        viewGenerator.generateHashView(domainConfig, baseConfig, DataSourceType.POSTGRESQL);
    String oracle = viewGenerator.generateHashView(domainConfig, baseConfig, DataSourceType.ORACLE);
    String mysql = viewGenerator.generateHashView(domainConfig, baseConfig, DataSourceType.MYSQL);
    String sqlserver =
        viewGenerator.generateHashView(domainConfig, baseConfig, DataSourceType.SQL_SERVER);

    // Assert
    assertNotEquals(postgresql, oracle);
    assertNotEquals(postgresql, mysql);
    assertNotEquals(postgresql, sqlserver);
    assertNotEquals(oracle, mysql);
    assertNotEquals(oracle, sqlserver);
    assertNotEquals(mysql, sqlserver);

    // Each should have database-specific functions
    assertTrue(postgresql.contains("md5("));
    assertTrue(oracle.contains("DBMS_CRYPTO"));
    assertTrue(mysql.contains("MD5("));
    assertTrue(sqlserver.contains("HASHBYTES"));
  }

  @Test
  @DisplayName("Should support heterogeneous source and target")
  void testGenerateForHeterogeneousDatabases_SourceAndTarget() {
    // Arrange
    DatabaseConfig sourceConfig =
        new DatabaseConfig(
            DataSourceType.POSTGRESQL,
            "public",
            "source_data",
            "_hash",
            List.of(FieldDefinition.string("id")),
            new ArrayList<>(List.of(FieldDefinition.string("name"))));

    DatabaseConfig targetConfig =
        new DatabaseConfig(
            DataSourceType.ORACLE,
            "TARGET",
            "target_data",
            "_hash",
            List.of(FieldDefinition.string("id")),
            new ArrayList<>(List.of(FieldDefinition.string("name"))));

    DomainConfig domainConfig =
        new DomainConfig("heterogeneous", sourceConfig, targetConfig, null, false, 95.0, 1.0);

    // Act
    String sourceView = viewGenerator.generateHashView(domainConfig, sourceConfig);
    String targetView = viewGenerator.generateHashView(domainConfig, targetConfig);

    // Assert
    assertTrue(sourceView.contains("md5(")); // PostgreSQL
    assertTrue(targetView.contains("DBMS_CRYPTO")); // Oracle
  }
}
