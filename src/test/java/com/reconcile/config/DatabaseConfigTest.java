package com.reconcile.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("DatabaseConfig Record Tests")
class DatabaseConfigTest {

  private DataSourceType PostgresType;
  private DataSourceType OracleType;

  @BeforeEach
  void setUp() {
    PostgresType = DataSourceType.POSTGRESQL;
    OracleType = DataSourceType.ORACLE;
  }

  @Test
  @DisplayName("Should create DatabaseConfig with all fields")
  void testCreateWithAllFields() {
    // Arrange
    List<FieldDefinition> hashFields =
        new ArrayList<>(
            Arrays.asList(
                FieldDefinition.decimal("amount"), FieldDefinition.string("description")));

    // Act
    DatabaseConfig config =
        new DatabaseConfig(
            PostgresType,
            "public",
            "invoices",
            "_hash",
            List.of(FieldDefinition.string("invoice_id")),
            hashFields,
            null,
            null,
            null);

    // Assert
    assertEquals(PostgresType, config.type());
    assertEquals("public", config.schema());
    assertEquals("invoices", config.table());
    assertEquals("_hash", config.viewSuffix());
    assertEquals("invoice_id", config.getIdFieldName());
    assertEquals(2, config.hashFields().size());
    assertEquals(FieldDefinition.decimal("amount"), config.hashFields().get(0));
  }

  @Test
  @DisplayName("Should normalize null viewSuffix to empty string")
  void testNullViewSuffixNormalized() {
    // Act
    DatabaseConfig config =
        new DatabaseConfig(
            PostgresType,
            "public",
            "invoices",
            null, // null viewSuffix
            List.of(FieldDefinition.string("id")),
            new ArrayList<>(Arrays.asList(FieldDefinition.decimal("amount"))),
            null,
            null,
            null);

    // Assert
    assertEquals("", config.viewSuffix());
  }

  @Test
  @DisplayName("Should provide effective view suffix")
  void testEffectiveViewSuffix() {
    // Arrange
    DatabaseConfig withSuffix =
        new DatabaseConfig(
            PostgresType,
            "public",
            "invoices",
            "_hash",
            List.of(FieldDefinition.string("id")),
            null,
            null,
            null,
            null);
    DatabaseConfig withoutSuffix =
        new DatabaseConfig(
            PostgresType, "public", "invoices", null, List.of(FieldDefinition.string("id")), null,
            null,
            null,
            null);

    // Act & Assert
    assertEquals("_hash", withSuffix.effectiveViewSuffix());
    assertEquals("", withoutSuffix.effectiveViewSuffix());
  }

  @Test
  @DisplayName("Should generate full table name with schema")
  void testGetFullTableName() {
    // Arrange
    DatabaseConfig config =
        new DatabaseConfig(
            PostgresType,
            "source_schema",
            "vendor_invoices",
            "",
            List.of(FieldDefinition.string("id")),
            new ArrayList<>(Arrays.asList(FieldDefinition.decimal("amount"))),
            null,
            null,
            null);

    // Act
    String fullName = config.getFullTableName();

    // Assert
    assertEquals("source_schema.vendor_invoices", fullName);
  }

  @Test
  @DisplayName("Should generate full view name with hash suffix")
  void testGetFullViewName() {
    // Arrange
    DatabaseConfig config =
        new DatabaseConfig(
            PostgresType,
            "public",
            "invoices",
            "_with_hash",
            List.of(FieldDefinition.string("id")),
            null,
            null,
            null,
            null);

    // Act
    String fullViewName = config.getFullViewName();

    // Assert
    assertEquals("public.invoices_with_hash", fullViewName);
  }

  @Test
  @DisplayName("Should generate full view name without suffix")
  void testGetFullViewNameNoSuffix() {
    // Arrange
    DatabaseConfig config =
        new DatabaseConfig(
            PostgresType, "public", "invoices", null, List.of(FieldDefinition.string("id")), null,
            null,
            null,
            null);

    // Act
    String fullViewName = config.getFullViewName();

    // Assert
    assertEquals("public.invoices", fullViewName);
  }

  @Test
  @DisplayName("Should use factory method for minimal config")
  void testFactoryOfMinimal() {
    // Act
    DatabaseConfig config = DatabaseConfig.of("public", "users");

    // Assert
    assertEquals("public", config.schema());
    assertEquals("users", config.table());
    assertNull(config.type());
    assertNull(config.idFields());
    assertEquals("", config.viewSuffix());
  }

  @Test
  @DisplayName("Should use factory method with view suffix")
  void testFactoryOfWithSuffix() {
    // Act
    DatabaseConfig config = DatabaseConfig.of("public", "invoices", "_hash");

    // Assert
    assertEquals("public", config.schema());
    assertEquals("invoices", config.table());
    assertEquals("_hash", config.viewSuffix());
    assertNull(config.type());
  }

  @Test
  @DisplayName("Should handle null suffix in factory method")
  void testFactoryOfWithNullSuffix() {
    // Act
    DatabaseConfig config = DatabaseConfig.of("public", "invoices", null);

    // Assert
    assertEquals("", config.viewSuffix());
  }

  @Test
  @DisplayName("Should be immutable (records)")
  void testImmutability() {
    // Act
    DatabaseConfig config1 =
        new DatabaseConfig(
            PostgresType,
            "schema1",
            "table1",
            "",
            List.of(FieldDefinition.string("id")),
            new ArrayList<>(
                Arrays.asList(FieldDefinition.string("f1"), FieldDefinition.string("f2"))),
            null,
            null,
            null);

    DatabaseConfig config2 =
        new DatabaseConfig(
            PostgresType,
            "schema1",
            "table1",
            "",
            List.of(FieldDefinition.string("id")),
            new ArrayList<>(
                Arrays.asList(FieldDefinition.string("f1"), FieldDefinition.string("f2"))),
            null,
            null,
            null);

    // Assert - same values should produce equal records
    assertEquals(config1, config2);
  }

  @Test
  @DisplayName("Should support heterogeneous database types")
  void testHeterogeneousDatabases() {
    // Arrange
    DatabaseConfig postgresConfig =
        new DatabaseConfig(
            PostgresType,
            "public",
            "invoices",
            "",
            List.of(FieldDefinition.string("id")),
            new ArrayList<>(Arrays.asList(FieldDefinition.decimal("amount"))),
            null,
            null,
            null);
    DatabaseConfig oracleConfig =
        new DatabaseConfig(
            OracleType,
            "SOURCE",
            "INVOICES",
            "",
            List.of(FieldDefinition.string("INV_ID")),
            new ArrayList<>(Arrays.asList(FieldDefinition.decimal("AMT"))),
            null,
            null,
            null);

    // Assert
    assertTrue(postgresConfig.type() == DataSourceType.POSTGRESQL);
    assertTrue(oracleConfig.type() == DataSourceType.ORACLE);
    assertNotEquals(postgresConfig.type(), oracleConfig.type());
  }

  @Test
  @DisplayName("Should demonstrate different hash field configurations")
  void testDifferentHashFields() {
    // Arrange - source has different hash fields than target
    List<FieldDefinition> sourceFields =
        new ArrayList<>(
            Arrays.asList(
                FieldDefinition.decimal("invoice_amount"),
                FieldDefinition.string("invoice_desc"),
                FieldDefinition.string("invoice_date")));
    List<FieldDefinition> targetFields =
        new ArrayList<>(
            Arrays.asList(
                FieldDefinition.decimal("amount"),
                FieldDefinition.string("description"),
                FieldDefinition.string("date")));

    DatabaseConfig sourceConfig =
        new DatabaseConfig(
            PostgresType,
            "source",
            "inv",
            "",
            List.of(FieldDefinition.string("source_id")),
            sourceFields,
            null,
            null,
            null);
    DatabaseConfig targetConfig =
        new DatabaseConfig(
            OracleType,
            "target",
            "INV",
            "",
            List.of(FieldDefinition.string("target_id")),
            targetFields,
            null,
            null,
            null);

    // Assert
    assertEquals(3, sourceConfig.hashFields().size());
    assertEquals(3, targetConfig.hashFields().size());
    assertNotEquals(sourceConfig.hashFields().get(0), targetConfig.hashFields().get(0));
  }

  @Test
  @DisplayName("Should support case-sensitive schema/table names")
  void testCaseSensitiveNames() {
    // Act
    DatabaseConfig config1 =
        new DatabaseConfig(
            PostgresType, "MySchema", "MyTable", "", List.of(FieldDefinition.string("id")), null,
            null,
            null,
            null);
    DatabaseConfig config2 =
        new DatabaseConfig(
            PostgresType, "myschema", "mytable", "", List.of(FieldDefinition.string("id")), null,
            null,
            null,
            null);

    // Assert
    assertEquals("MySchema.MyTable", config1.getFullTableName());
    assertEquals("myschema.mytable", config2.getFullTableName());
    assertNotEquals(config1.getFullTableName(), config2.getFullTableName());
  }
}
