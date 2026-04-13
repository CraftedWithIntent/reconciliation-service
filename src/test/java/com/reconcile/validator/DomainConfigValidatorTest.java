package com.reconcile.validator;

import static org.junit.jupiter.api.Assertions.*;

import com.reconcile.config.DataSourceType;
import com.reconcile.config.DatabaseConfig;
import com.reconcile.config.DomainConfig;
import com.reconcile.config.FieldDefinition;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("DomainConfigValidator Tests")
class DomainConfigValidatorTest {

  private DatabaseConfig validSourceConfig;
  private DatabaseConfig validTargetConfig;

  @BeforeEach
  void setUp() {
    validSourceConfig =
        new DatabaseConfig(
            DataSourceType.POSTGRESQL,
            "public",
            "source_table",
            "_hash",
            List.of(FieldDefinition.string("id")),
            new ArrayList<>(
                List.of(FieldDefinition.string("name"), FieldDefinition.decimal("amount"))));

    validTargetConfig =
        new DatabaseConfig(
            DataSourceType.POSTGRESQL,
            "public",
            "target_table",
            "_hash",
            List.of(FieldDefinition.string("id")),
            new ArrayList<>(
                List.of(FieldDefinition.string("name"), FieldDefinition.decimal("amount"))));
  }

  @Test
  @DisplayName("Should validate a correct domain configuration")
  void testValidatePage_CorrectConfiguration() {
    // Arrange
    DomainConfig config =
        new DomainConfig("test-domain", validSourceConfig, validTargetConfig, null, false);

    // Act
    List<String> errors = DomainConfigValidator.validate(config);

    // Assert
    assertTrue(errors.isEmpty());
  }

  @Test
  @DisplayName("Should reject null domain configuration")
  void testValidate_NullConfiguration() {
    // Act
    List<String> errors = DomainConfigValidator.validate(null);

    // Assert
    assertFalse(errors.isEmpty());
    assertTrue(errors.stream().anyMatch(e -> e.contains("null")));
  }

  @Test
  @DisplayName("Should reject empty domain name")
  void testValidate_EmptyDomainName() {
    // Arrange
    DomainConfig config = new DomainConfig("", validSourceConfig, validTargetConfig, null, false);

    // Act
    List<String> errors = DomainConfigValidator.validate(config);

    // Assert
    assertFalse(errors.isEmpty());
    assertTrue(errors.stream().anyMatch(e -> e.contains("name")));
  }

  @Test
  @DisplayName("Should reject missing source configuration")
  void testValidate_MissingSource() {
    // Arrange
    DomainConfig config = new DomainConfig("test-domain", null, validTargetConfig, null, false);

    // Act
    List<String> errors = DomainConfigValidator.validate(config);

    // Assert
    assertFalse(errors.isEmpty());
    assertTrue(errors.stream().anyMatch(e -> e.contains("Source")));
  }

  @Test
  @DisplayName("Should reject missing target configuration")
  void testValidate_MissingTarget() {
    // Arrange
    DomainConfig config = new DomainConfig("test-domain", validSourceConfig, null, null, false);

    // Act
    List<String> errors = DomainConfigValidator.validate(config);

    // Assert
    assertFalse(errors.isEmpty());
    assertTrue(errors.stream().anyMatch(e -> e.contains("Target")));
  }

  @Test
  @DisplayName("Should reject empty schema")
  void testValidate_EmptySchema() {
    // Arrange
    DatabaseConfig badConfig =
        new DatabaseConfig(
            DataSourceType.POSTGRESQL,
            "",
            "table",
            "",
            List.of(FieldDefinition.string("id")),
            new ArrayList<>(List.of(FieldDefinition.string("name"))));

    DomainConfig config =
        new DomainConfig("test-domain", badConfig, validTargetConfig, null, false);

    // Act
    List<String> errors = DomainConfigValidator.validate(config);

    // Assert
    assertFalse(errors.isEmpty());
    assertTrue(errors.stream().anyMatch(e -> e.contains("schema")));
  }

  @Test
  @DisplayName("Should reject empty table")
  void testValidate_EmptyTable() {
    // Arrange
    DatabaseConfig badConfig =
        new DatabaseConfig(
            DataSourceType.POSTGRESQL,
            "public",
            "",
            "",
            List.of(FieldDefinition.string("id")),
            new ArrayList<>(List.of(FieldDefinition.string("name"))));

    DomainConfig config =
        new DomainConfig("test-domain", badConfig, validTargetConfig, null, false);

    // Act
    List<String> errors = DomainConfigValidator.validate(config);

    // Assert
    assertFalse(errors.isEmpty());
    assertTrue(errors.stream().anyMatch(e -> e.contains("table")));
  }

  @Test
  @DisplayName("Should reject missing idField")
  void testValidate_MissingIdField() {
    // Arrange
    DatabaseConfig badConfig =
        new DatabaseConfig(
            DataSourceType.POSTGRESQL,
            "public",
            "table",
            "",
            null,
            new ArrayList<>(List.of(FieldDefinition.string("name"))));

    DomainConfig config =
        new DomainConfig("test-domain", badConfig, validTargetConfig, null, false);

    // Act
    List<String> errors = DomainConfigValidator.validate(config);

    // Assert
    assertFalse(errors.isEmpty());
    assertTrue(errors.stream().anyMatch(e -> e.contains("idField")));
  }

  @Test
  @DisplayName("Should reject empty hashFields")
  void testValidate_EmptyHashFields() {
    // Arrange
    DatabaseConfig badConfig =
        new DatabaseConfig(
            DataSourceType.POSTGRESQL,
            "public",
            "table",
            "",
            List.of(FieldDefinition.string("id")),
            new ArrayList<>());

    DomainConfig config =
        new DomainConfig("test-domain", badConfig, validTargetConfig, null, false);

    // Act
    List<String> errors = DomainConfigValidator.validate(config);

    // Assert
    assertFalse(errors.isEmpty());
    assertTrue(errors.stream().anyMatch(e -> e.contains("hashFields")));
  }

  @Test
  @DisplayName("Should reject invalid datatype")
  void testValidate_InvalidDataType() {
    // Arrange
    List<FieldDefinition> badFields =
        new ArrayList<>(List.of(FieldDefinition.of("name", "INVALID_TYPE")));

    DatabaseConfig badConfig =
        new DatabaseConfig(
            DataSourceType.POSTGRESQL,
            "public",
            "table",
            "",
            List.of(FieldDefinition.string("id")),
            badFields);

    DomainConfig config =
        new DomainConfig("test-domain", badConfig, validTargetConfig, null, false);

    // Act
    List<String> errors = DomainConfigValidator.validate(config);

    // Assert
    assertFalse(errors.isEmpty());
    assertTrue(errors.stream().anyMatch(e -> e.contains("INVALID_TYPE")));
  }

  @Test
  @DisplayName("Should reject mismatched hashField counts")
  void testValidate_MismatchedHashFieldCounts() {
    // Arrange
    DatabaseConfig sourceConfig =
        new DatabaseConfig(
            DataSourceType.POSTGRESQL,
            "public",
            "source_table",
            "",
            List.of(FieldDefinition.string("id")),
            new ArrayList<>(
                List.of(FieldDefinition.string("field1"), FieldDefinition.string("field2"))));

    DatabaseConfig targetConfig =
        new DatabaseConfig(
            DataSourceType.POSTGRESQL,
            "public",
            "target_table",
            "",
            List.of(FieldDefinition.string("id")),
            new ArrayList<>(List.of(FieldDefinition.string("field1"))));

    DomainConfig config = new DomainConfig("test-domain", sourceConfig, targetConfig, null, false);

    // Act
    List<String> errors = DomainConfigValidator.validate(config);

    // Assert
    assertFalse(errors.isEmpty());
    assertTrue(errors.stream().anyMatch(e -> e.contains("count")));
  }

  @Test
  @DisplayName("Should accept compatible integer ID types")
  void testValidate_CompatibleIntegerIdTypes() {
    // Arrange
    DatabaseConfig sourceConfig =
        new DatabaseConfig(
            DataSourceType.POSTGRESQL,
            "public",
            "source_table",
            "",
            List.of(FieldDefinition.integer("id")),
            new ArrayList<>(List.of(FieldDefinition.string("name"))));

    DatabaseConfig targetConfig =
        new DatabaseConfig(
            DataSourceType.POSTGRESQL,
            "public",
            "target_table",
            "",
            List.of(FieldDefinition.of("id", "BIGINT")),
            new ArrayList<>(List.of(FieldDefinition.string("name"))));

    DomainConfig config = new DomainConfig("test-domain", sourceConfig, targetConfig, null, false);

    // Act
    List<String> errors = DomainConfigValidator.validate(config);

    // Assert
    assertTrue(errors.isEmpty());
  }

  @Test
  @DisplayName("Should accept UUID to STRING ID type compatibility")
  void testValidate_CompatibleUuidStringIdTypes() {
    // Arrange
    DatabaseConfig sourceConfig =
        new DatabaseConfig(
            DataSourceType.POSTGRESQL,
            "public",
            "source_table",
            "",
            List.of(FieldDefinition.uuid("id")),
            new ArrayList<>(List.of(FieldDefinition.string("name"))));

    DatabaseConfig targetConfig =
        new DatabaseConfig(
            DataSourceType.POSTGRESQL,
            "public",
            "target_table",
            "",
            List.of(FieldDefinition.string("id")),
            new ArrayList<>(List.of(FieldDefinition.string("name"))));

    DomainConfig config = new DomainConfig("test-domain", sourceConfig, targetConfig, null, false);

    // Act
    List<String> errors = DomainConfigValidator.validate(config);

    // Assert
    assertTrue(errors.isEmpty());
  }

  @Test
  @DisplayName("Should throw exception when validation fails")
  void testValidateOrThrow_InvalidConfiguration() {
    // Arrange
    DatabaseConfig badConfig =
        new DatabaseConfig(
            DataSourceType.POSTGRESQL,
            "",
            "table",
            "",
            List.of(FieldDefinition.string("id")),
            new ArrayList<>(List.of(FieldDefinition.string("name"))));

    DomainConfig config =
        new DomainConfig("test-domain", badConfig, validTargetConfig, null, false);

    // Act & Assert
    assertThrows(
        IllegalArgumentException.class,
        () -> {
          DomainConfigValidator.validateOrThrow(config);
        });
  }

  @Test
  @DisplayName("Should validate complex heterogeneous configuration")
  void testValidate_HeterogeneousDatabaseConfiguration() {
    // Arrange
    DatabaseConfig sourceConfig =
        new DatabaseConfig(
            DataSourceType.ORACLE,
            "SOURCE",
            "vendor_invoices",
            "_hash",
            List.of(FieldDefinition.string("invoice_id")),
            new ArrayList<>(
                List.of(
                    FieldDefinition.string("vendor_name"),
                    FieldDefinition.decimal("amount"),
                    FieldDefinition.date("invoice_date"))));

    DatabaseConfig targetConfig =
        new DatabaseConfig(
            DataSourceType.POSTGRESQL,
            "TARGET",
            "ledger_invoices",
            "_hash",
            List.of(FieldDefinition.uuid("ledger_id")),
            new ArrayList<>(
                List.of(
                    FieldDefinition.string("vendor_info"),
                    FieldDefinition.numeric("total"),
                    FieldDefinition.timestamp("created_date"))));

    DomainConfig config =
        new DomainConfig("vendor-reconciliation", sourceConfig, targetConfig, null, false);

    // Act
    List<String> errors = DomainConfigValidator.validate(config);

    // Assert
    assertTrue(errors.isEmpty());
  }
}
