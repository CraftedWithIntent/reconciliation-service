package com.reconcile.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("DomainConfig Record Tests")
class DomainConfigTest {

  private DatabaseConfig sourceConfig;
  private DatabaseConfig targetConfig;

  @BeforeEach
  void setUp() {
    sourceConfig =
        new DatabaseConfig(
            DataSourceType.POSTGRESQL,
            "source_schema",
            "invoices",
            "",
            List.of(FieldDefinition.string("invoice_id")),
            new ArrayList<>(
                Arrays.asList(
                    FieldDefinition.decimal("amount"), FieldDefinition.string("description"))));

    targetConfig =
        new DatabaseConfig(
            DataSourceType.POSTGRESQL,
            "target_schema",
            "invoices",
            "",
            List.of(FieldDefinition.string("invoice_id")),
            new ArrayList<>(
                Arrays.asList(
                    FieldDefinition.decimal("amount"), FieldDefinition.string("description"))));
  }

  @Test
  @DisplayName("Should create DomainConfig with all fields")
  void testCreateWithAllFields() {
    // Arrange
    Map<String, String> fieldMappings = new HashMap<>();
    fieldMappings.put("source_amount", "target_amount");
    fieldMappings.put("source_desc", "target_description");

    // Act
    DomainConfig config =
        new DomainConfig(
            "vendor-invoices", sourceConfig, targetConfig, fieldMappings, true, 95.0, 1.0);

    // Assert
    assertEquals("vendor-invoices", config.name());
    assertEquals(sourceConfig, config.source());
    assertEquals(targetConfig, config.target());
    assertEquals(2, config.fieldMappings().size());
    assertTrue(config.caseSensitive());
  }

  @Test
  @DisplayName("Should create DomainConfig with null field mappings")
  void testCreateWithNullMappings() {
    // Act
    DomainConfig config =
        new DomainConfig("vendor-invoices", sourceConfig, targetConfig, null, false, 95.0, 1.0);

    // Assert
    assertEquals("vendor-invoices", config.name());
    assertNull(config.fieldMappings());
    assertFalse(config.caseSensitive());
  }

  @Test
  @DisplayName("Should use factory method for common case")
  void testFactoryOfCommonCase() {
    // Act
    DomainConfig config = DomainConfig.of("vendor-invoices", sourceConfig, targetConfig);

    // Assert
    assertEquals("vendor-invoices", config.name());
    assertEquals(sourceConfig, config.source());
    assertEquals(targetConfig, config.target());
    assertNull(config.fieldMappings());
    assertFalse(config.caseSensitive()); // Default is case-insensitive
  }

  @Test
  @DisplayName("Should support heterogeneous source and target databases")
  void testHeterogeneousDatabases() {
    // Arrange
    DatabaseConfig postgresSource =
        new DatabaseConfig(
            DataSourceType.POSTGRESQL,
            "public",
            "invoices",
            "",
            List.of(FieldDefinition.string("invoice_id")),
            new ArrayList<>(Arrays.asList(FieldDefinition.decimal("amount"))));

    DatabaseConfig oracleTarget =
        new DatabaseConfig(
            DataSourceType.ORACLE,
            "SOURCE",
            "INVOICES",
            "",
            List.of(FieldDefinition.string("INV_ID")),
            new ArrayList<>(Arrays.asList(FieldDefinition.decimal("AMT"))));

    // Act
    DomainConfig config =
        new DomainConfig(
            "hetero-domain", postgresSource, oracleTarget, new HashMap<>(), false, 95.0, 1.0);

    // Assert
    assertEquals(DataSourceType.POSTGRESQL, config.source().type());
    assertEquals(DataSourceType.ORACLE, config.target().type());
    assertNotEquals(config.source().type(), config.target().type());
  }

  @Test
  @DisplayName("Should support field mappings for renamed columns")
  void testFieldMappingsForRenamedColumns() {
    // Arrange
    Map<String, String> mappings = new HashMap<>();
    mappings.put("invoice_amount", "amt");
    mappings.put("invoice_description", "desc");
    mappings.put("invoice_date", "posting_date");

    // Act
    DomainConfig config =
        new DomainConfig("mapped-domain", sourceConfig, targetConfig, mappings, false, 95.0, 1.0);

    // Assert
    assertEquals(3, config.fieldMappings().size());
    assertEquals("amt", config.fieldMappings().get("invoice_amount"));
    assertEquals("desc", config.fieldMappings().get("invoice_description"));
    assertEquals("posting_date", config.fieldMappings().get("invoice_date"));
  }

  @Test
  @DisplayName("Should support case-sensitive field matching")
  void testCaseSensitiveFlag() {
    // Arrange
    DomainConfig caseInsensitiveConfig =
        new DomainConfig("domain1", sourceConfig, targetConfig, null, false, 95.0, 1.0);

    DomainConfig caseSensitiveConfig =
        new DomainConfig("domain2", sourceConfig, targetConfig, null, true, 95.0, 1.0);

    // Assert
    assertFalse(caseInsensitiveConfig.caseSensitive());
    assertTrue(caseSensitiveConfig.caseSensitive());
  }

  @Test
  @DisplayName("Should demonstrate multiple domain configurations")
  void testMultipleDomainConfigs() {
    // Arrange - vendor invoices domain with FieldDefinition objects
    DatabaseConfig vendorSource =
        new DatabaseConfig(
            DataSourceType.POSTGRESQL,
            "vendor_db",
            "invoices",
            "",
            List.of(FieldDefinition.string("vendor_invoice_id")),
            new ArrayList<>(
                Arrays.asList(
                    FieldDefinition.decimal("vendor_amount"),
                    FieldDefinition.string("vendor_desc"))));
    DatabaseConfig vendorTarget =
        new DatabaseConfig(
            DataSourceType.POSTGRESQL,
            "accounting_db",
            "ap_invoices",
            "",
            List.of(FieldDefinition.string("ap_invoice_id")),
            new ArrayList<>(
                Arrays.asList(
                    FieldDefinition.decimal("ap_amount"),
                    FieldDefinition.string("ap_description"))));

    // Arrange - expense reports domain with FieldDefinition objects
    DatabaseConfig expenseSource =
        new DatabaseConfig(
            DataSourceType.ORACLE,
            "HR",
            "EXPENSE_REPORTS",
            "",
            List.of(FieldDefinition.string("EXP_ID")),
            new ArrayList<>(Arrays.asList(FieldDefinition.decimal("EXP_AMOUNT"))));
    DatabaseConfig expenseTarget =
        new DatabaseConfig(
            DataSourceType.ORACLE,
            "GL",
            "EXPENSES",
            "",
            List.of(FieldDefinition.string("GL_EXP_ID")),
            new ArrayList<>(Arrays.asList(FieldDefinition.decimal("GL_AMOUNT"))));

    // Act
    DomainConfig vendorDomain = DomainConfig.of("vendor-invoices", vendorSource, vendorTarget);
    DomainConfig expenseDomain = DomainConfig.of("expense-reports", expenseSource, expenseTarget);

    // Assert
    assertEquals("vendor-invoices", vendorDomain.name());
    assertEquals("expense-reports", expenseDomain.name());
    assertEquals(DataSourceType.POSTGRESQL, vendorDomain.source().type());
    assertEquals(DataSourceType.ORACLE, expenseDomain.source().type());
    assertNotEquals(vendorDomain.name(), expenseDomain.name());
  }

  @Test
  @DisplayName("Should be immutable (records)")
  void testImmutability() {
    // Act
    DomainConfig config1 =
        new DomainConfig(
            "test-domain", sourceConfig, targetConfig, new HashMap<>(), false, 95.0, 1.0);

    DomainConfig config2 =
        new DomainConfig(
            "test-domain", sourceConfig, targetConfig, new HashMap<>(), false, 95.0, 1.0);

    // Assert - same values should produce equal records
    assertEquals(config1, config2);
  }

  @Test
  @DisplayName("Should support domain with empty field mappings")
  void testEmptyFieldMappings() {
    // Act
    DomainConfig config =
        new DomainConfig(
            "simple-domain", sourceConfig, targetConfig, new HashMap<>(), false, 95.0, 1.0);

    // Assert
    assertNotNull(config.fieldMappings());
    assertEquals(0, config.fieldMappings().size());
  }

  @Test
  @DisplayName("Should support domains with different table names in source/target")
  void testDifferentTableNames() {
    // Arrange
    DatabaseConfig source =
        new DatabaseConfig(
            DataSourceType.POSTGRESQL,
            "public",
            "invoice_source",
            "",
            List.of(FieldDefinition.string("id")),
            new ArrayList<>(Arrays.asList(FieldDefinition.decimal("amount"))));
    DatabaseConfig target =
        new DatabaseConfig(
            DataSourceType.POSTGRESQL,
            "public",
            "invoice_target",
            "",
            List.of(FieldDefinition.string("id")),
            new ArrayList<>(Arrays.asList(FieldDefinition.decimal("amount"))));

    // Act
    DomainConfig config = DomainConfig.of("mapped-tables", source, target);

    // Assert
    assertEquals("invoice_source", config.source().table());
    assertEquals("invoice_target", config.target().table());
    assertNotEquals(config.source().table(), config.target().table());
  }
}
