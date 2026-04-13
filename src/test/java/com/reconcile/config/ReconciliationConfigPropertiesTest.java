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

@DisplayName("ReconciliationConfigProperties Tests")
class ReconciliationConfigPropertiesTest {

  private ReconciliationConfigProperties configProperties;
  private DomainConfig vendorDomain;
  private DomainConfig expenseDomain;

  @BeforeEach
  void setUp() {
    configProperties = new ReconciliationConfigProperties();

    // Setup vendor domain
    DatabaseConfig vendorSource =
        new DatabaseConfig(
            DataSourceType.POSTGRESQL,
            "source_schema",
            "vendor_invoices",
            "",
            List.of(FieldDefinition.string("invoice_id")),
            new ArrayList<>(
                Arrays.asList(
                    FieldDefinition.decimal("amount"), FieldDefinition.string("vendor_id"))),
            null,
            null,
            null);
    DatabaseConfig vendorTarget =
        new DatabaseConfig(
            DataSourceType.POSTGRESQL,
            "target_schema",
            "invoices",
            "",
            List.of(FieldDefinition.string("invoice_id")),
            new ArrayList<>(
                Arrays.asList(
                    FieldDefinition.decimal("amount"), FieldDefinition.string("vendor_id"))),
            null,
            null,
            null);
    vendorDomain = DomainConfig.of("vendor-invoices", vendorSource, vendorTarget);

    // Setup expense domain
    DatabaseConfig expenseSource =
        new DatabaseConfig(
            DataSourceType.ORACLE,
            "HR",
            "EXPENSE_REPORTS",
            "",
            List.of(FieldDefinition.string("EXP_ID")),
            new ArrayList<>(
                Arrays.asList(
                    FieldDefinition.decimal("EXP_AMOUNT"), FieldDefinition.string("DEPT_ID"))),
            null,
            null,
            null);
    DatabaseConfig expenseTarget =
        new DatabaseConfig(
            DataSourceType.ORACLE,
            "GL",
            "EXPENSES",
            "",
            List.of(FieldDefinition.string("GL_EXP_ID")),
            new ArrayList<>(
                Arrays.asList(
                    FieldDefinition.decimal("AMOUNT"), FieldDefinition.string("DEPARTMENT"))),
            null,
            null,
            null);
    expenseDomain = DomainConfig.of("expense-reports", expenseSource, expenseTarget);
  }

  @Test
  @DisplayName("Should initialize with empty domains map")
  void testEmptyInitialization() {
    // Assert
    assertNotNull(configProperties.getDomains());
    assertEquals(0, configProperties.getDomains().size());
  }

  @Test
  @DisplayName("Should add single domain to properties")
  void testAddSingleDomain() {
    // Arrange
    Map<String, DomainConfig> domains = new HashMap<>();
    domains.put("vendor-invoices", vendorDomain);

    // Act
    configProperties.setDomains(domains);

    // Assert
    assertEquals(1, configProperties.getDomains().size());
    assertNotNull(configProperties.getDomain("vendor-invoices"));
    assertEquals(vendorDomain, configProperties.getDomain("vendor-invoices"));
  }

  @Test
  @DisplayName("Should retrieve domain by name")
  void testGetDomainByName() {
    // Arrange
    Map<String, DomainConfig> domains = new HashMap<>();
    domains.put("vendor-invoices", vendorDomain);
    configProperties.setDomains(domains);

    // Act
    DomainConfig retrieved = configProperties.getDomain("vendor-invoices");

    // Assert
    assertNotNull(retrieved);
    assertEquals("vendor-invoices", retrieved.name());
    assertEquals(vendorDomain.source().schema(), retrieved.source().schema());
  }

  @Test
  @DisplayName("Should return null for non-existent domain")
  void testGetNonExistentDomain() {
    // Arrange
    Map<String, DomainConfig> domains = new HashMap<>();
    domains.put("vendor-invoices", vendorDomain);
    configProperties.setDomains(domains);

    // Act
    DomainConfig retrieved = configProperties.getDomain("non-existent");

    // Assert
    assertNull(retrieved);
  }

  @Test
  @DisplayName("Should check if domain exists")
  void testHasDomain() {
    // Arrange
    Map<String, DomainConfig> domains = new HashMap<>();
    domains.put("vendor-invoices", vendorDomain);
    configProperties.setDomains(domains);

    // Act & Assert
    assertTrue(configProperties.hasDomain("vendor-invoices"));
    assertFalse(configProperties.hasDomain("non-existent"));
    assertFalse(configProperties.hasDomain("expense-reports"));
  }

  @Test
  @DisplayName("Should add multiple domains")
  void testAddMultipleDomains() {
    // Arrange
    Map<String, DomainConfig> domains = new HashMap<>();
    domains.put("vendor-invoices", vendorDomain);
    domains.put("expense-reports", expenseDomain);

    // Act
    configProperties.setDomains(domains);

    // Assert
    assertEquals(2, configProperties.getDomains().size());
    assertTrue(configProperties.hasDomain("vendor-invoices"));
    assertTrue(configProperties.hasDomain("expense-reports"));
    assertNotNull(configProperties.getDomain("vendor-invoices"));
    assertNotNull(configProperties.getDomain("expense-reports"));
  }

  @Test
  @DisplayName("Should retrieve all domain names")
  void testGetAllDomainNames() {
    // Arrange
    Map<String, DomainConfig> domains = new HashMap<>();
    domains.put("vendor-invoices", vendorDomain);
    domains.put("expense-reports", expenseDomain);
    configProperties.setDomains(domains);

    // Act
    Map<String, DomainConfig> retrieved = configProperties.getDomains();

    // Assert
    assertEquals(2, retrieved.size());
    assertTrue(retrieved.containsKey("vendor-invoices"));
    assertTrue(retrieved.containsKey("expense-reports"));
  }

  @Test
  @DisplayName("Should support updating domains")
  void testUpdateDomains() {
    // Arrange
    Map<String, DomainConfig> domains = new HashMap<>();
    domains.put("vendor-invoices", vendorDomain);
    configProperties.setDomains(domains);

    // Act
    Map<String, DomainConfig> updatedDomains = new HashMap<>();
    updatedDomains.put("vendor-invoices", vendorDomain);
    updatedDomains.put("expense-reports", expenseDomain);
    configProperties.setDomains(updatedDomains);

    // Assert
    assertEquals(2, configProperties.getDomains().size());
  }

  @Test
  @DisplayName("Should handle heterogeneous database configurations across domains")
  void testHeterogeneousConfigurations() {
    // Arrange
    Map<String, DomainConfig> domains = new HashMap<>();
    domains.put("vendor-invoices", vendorDomain); // PostgreSQL
    domains.put("expense-reports", expenseDomain); // Oracle
    configProperties.setDomains(domains);

    // Act & Assert
    DomainConfig vendor = configProperties.getDomain("vendor-invoices");
    DomainConfig expense = configProperties.getDomain("expense-reports");

    assertEquals(DataSourceType.POSTGRESQL, vendor.source().type());
    assertEquals(DataSourceType.ORACLE, expense.source().type());
    assertNotEquals(vendor.source().type(), expense.source().type());
  }

  @Test
  @DisplayName("Should provide string representation")
  void testToString() {
    // Arrange
    Map<String, DomainConfig> domains = new HashMap<>();
    domains.put("vendor-invoices", vendorDomain);
    domains.put("expense-reports", expenseDomain);
    configProperties.setDomains(domains);

    // Act
    String toString = configProperties.toString();

    // Assert
    assertNotNull(toString);
    assertTrue(toString.contains("ReconciliationConfigProperties"));
    assertTrue(toString.contains("vendor-invoices"));
    assertTrue(toString.contains("expense-reports"));
  }

  @Test
  @DisplayName("Should validate domain access patterns")
  void testDomainAccessPatterns() {
    // Arrange
    Map<String, DomainConfig> domains = new HashMap<>();
    domains.put("vendor-invoices", vendorDomain);
    configProperties.setDomains(domains);

    // Act
    DomainConfig config = configProperties.getDomain("vendor-invoices");
    String domainName = config.name();
    String sourceSchema = config.source().schema();
    String targetSchema = config.target().schema();

    // Assert
    assertEquals("vendor-invoices", domainName);
    assertEquals("source_schema", sourceSchema);
    assertEquals("target_schema", targetSchema);
  }

  @Test
  @DisplayName("Should support domain-specific hash field configurations")
  void testDomainSpecificHashFields() {
    // Arrange
    Map<String, DomainConfig> domains = new HashMap<>();
    domains.put("vendor-invoices", vendorDomain);
    domains.put("expense-reports", expenseDomain);
    configProperties.setDomains(domains);

    // Act
    DomainConfig vendor = configProperties.getDomain("vendor-invoices");
    DomainConfig expense = configProperties.getDomain("expense-reports");

    // Assert
    assertEquals(2, vendor.source().hashFields().size());
    assertEquals(2, expense.source().hashFields().size());
    assertEquals(FieldDefinition.decimal("amount"), vendor.source().hashFields().get(0));
    assertEquals(FieldDefinition.decimal("EXP_AMOUNT"), expense.source().hashFields().get(0));
  }

  @Test
  @DisplayName("Should verify null safety for domains map")
  void testNullSafety() {
    // Act
    configProperties.setDomains(new HashMap<>());
    DomainConfig retrieved = configProperties.getDomain("missing");

    // Assert
    assertNull(retrieved);
    assertFalse(configProperties.hasDomain("missing"));
  }
}
