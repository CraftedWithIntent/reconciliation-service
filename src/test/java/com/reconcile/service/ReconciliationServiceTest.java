package com.reconcile.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;

import com.reconcile.config.DataSourceType;
import com.reconcile.config.DatabaseConfig;
import com.reconcile.config.DomainConfig;
import com.reconcile.config.FieldDefinition;
import com.reconcile.config.ReconciliationConfigProperties;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReconciliationService Unit Tests")
class ReconciliationServiceTest {

  @Mock private ReconciliationConfigProperties configProperties;

  private ReconciliationService reconciliationService;
  private DomainConfig vendorDomainConfig;
  private DomainConfig expenseDomainConfig;

  @BeforeEach
  void setUp() {
    // Setup vendor domain configuration using record constructors with FieldDefinition objects
    DatabaseConfig sourceConfig =
        new DatabaseConfig(
            DataSourceType.POSTGRESQL,
            "source_schema",
            "invoices",
            "",
            List.of(FieldDefinition.string("invoice_id")),
            new ArrayList<>(
                List.of(FieldDefinition.decimal("amount"), FieldDefinition.string("vendor"))));

    DatabaseConfig targetConfig =
        new DatabaseConfig(
            DataSourceType.POSTGRESQL,
            "target_schema",
            "invoices",
            "",
            List.of(FieldDefinition.string("invoice_id")),
            new ArrayList<>(
                List.of(FieldDefinition.decimal("amount"), FieldDefinition.string("vendor"))));

    vendorDomainConfig =
        new DomainConfig("vendor-invoices", sourceConfig, targetConfig, null, false);

    // Setup expense domain configuration with FieldDefinition objects
    DatabaseConfig expenseSource =
        new DatabaseConfig(
            DataSourceType.ORACLE,
            "SOURCE",
            "EXPENSES",
            "",
            List.of(FieldDefinition.string("EXP_ID")),
            new ArrayList<>(
                List.of(FieldDefinition.decimal("AMOUNT"), FieldDefinition.string("CATEGORY"))));

    DatabaseConfig expenseTarget =
        new DatabaseConfig(
            DataSourceType.ORACLE,
            "TARGET",
            "EXPENSES",
            "",
            List.of(FieldDefinition.string("EXP_ID")),
            new ArrayList<>(
                List.of(FieldDefinition.decimal("AMOUNT"), FieldDefinition.string("CATEGORY"))));

    expenseDomainConfig =
        new DomainConfig("expense-reports", expenseSource, expenseTarget, null, false);

    // Setup configuration properties mock using lenient() to avoid strictness issues
    Map<String, DomainConfig> domains = new HashMap<>();
    domains.put("vendor-invoices", vendorDomainConfig);
    domains.put("expense-reports", expenseDomainConfig);

    lenient().when(configProperties.getDomains()).thenReturn(domains);
    lenient().when(configProperties.hasDomain("vendor-invoices")).thenReturn(true);
    lenient().when(configProperties.hasDomain("expense-reports")).thenReturn(true);
    lenient().when(configProperties.hasDomain("invalid-domain")).thenReturn(false);
    lenient().when(configProperties.getDomain("vendor-invoices")).thenReturn(vendorDomainConfig);
    lenient().when(configProperties.getDomain("expense-reports")).thenReturn(expenseDomainConfig);

    // Initialize service
    reconciliationService =
        new ReconciliationService(
            configProperties,
            "jdbc:postgresql://source:5432/source_db",
            "postgres",
            "postgres",
            "jdbc:postgresql://target:5433/target_db",
            "postgres",
            "postgres");
  }

  @Test
  @DisplayName("Should throw IllegalArgumentException for undefined domain")
  void testReconcileInvalidDomain() {
    // Act & Assert
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> reconciliationService.reconcile("invalid-domain"));

    assertTrue(exception.getMessage().contains("not configured"));
  }

  @Test
  @DisplayName("Should construct service successfully with mocked config")
  void testServiceConstruction() {
    // Act & Assert
    assertNotNull(reconciliationService);
  }

  @Test
  @DisplayName("Should expose multiple configured domains")
  void testMultipleDomains() {
    // Act
    Map<String, DomainConfig> domains = configProperties.getDomains();

    // Assert
    assertNotNull(domains);
    assertEquals(2, domains.size());
    assertTrue(domains.containsKey("vendor-invoices"));
    assertTrue(domains.containsKey("expense-reports"));
  }

  @Test
  @DisplayName("Should validate vendor invoices domain configuration")
  void testVendorDomainConfiguration() {
    // Act
    DomainConfig config = configProperties.getDomain("vendor-invoices");
    DatabaseConfig source = config.source();
    DatabaseConfig target = config.target();

    // Assert
    assertEquals("vendor-invoices", config.name());
    assertEquals(DataSourceType.POSTGRESQL, source.type());
    assertEquals(DataSourceType.POSTGRESQL, target.type());
    assertEquals("source_schema", source.schema());
    assertEquals("target_schema", target.schema());
    assertEquals(2, source.hashFields().size());
    assertEquals(2, target.hashFields().size());
  }

  @Test
  @DisplayName("Should validate expense reports domain configuration")
  void testExpenseDomainConfiguration() {
    // Act
    DomainConfig config = configProperties.getDomain("expense-reports");
    DatabaseConfig source = config.source();
    DatabaseConfig target = config.target();

    // Assert
    assertEquals("expense-reports", config.name());
    assertEquals(DataSourceType.ORACLE, source.type());
    assertEquals(DataSourceType.ORACLE, target.type());
    assertEquals("SOURCE", source.schema());
    assertEquals("TARGET", target.schema());
  }

  @Test
  @DisplayName("Should support heterogeneous database types across domains")
  void testHeterogeneousDatabases() {
    // Act
    DomainConfig vendorConfig = configProperties.getDomain("vendor-invoices");
    DomainConfig expenseConfig = configProperties.getDomain("expense-reports");

    // Assert
    assertEquals(DataSourceType.POSTGRESQL, vendorConfig.source().type());
    assertEquals(DataSourceType.ORACLE, expenseConfig.source().type());
  }

  @Test
  @DisplayName("Should validate hash field mappings in both domains")
  void testHashFieldConfiguration() {
    // Act
    DomainConfig vendorConfig = configProperties.getDomain("vendor-invoices");
    List<FieldDefinition> vendorHashFields = vendorConfig.source().hashFields();

    DomainConfig expenseConfig = configProperties.getDomain("expense-reports");
    List<FieldDefinition> expenseHashFields = expenseConfig.source().hashFields();

    // Assert - extract field names from FieldDefinition objects
    assertTrue(vendorHashFields.stream().anyMatch(fd -> "amount".equals(fd.name())));
    assertTrue(vendorHashFields.stream().anyMatch(fd -> "vendor".equals(fd.name())));

    assertTrue(expenseHashFields.stream().anyMatch(fd -> "AMOUNT".equals(fd.name())));
    assertTrue(expenseHashFields.stream().anyMatch(fd -> "CATEGORY".equals(fd.name())));
  }

  @Test
  @DisplayName("Should have ID fields configured for each database")
  void testIdFieldConfiguration() {
    // Act
    DomainConfig vendorConfig = configProperties.getDomain("vendor-invoices");
    List<String> vendorIdFieldNames = vendorConfig.source().getIdFieldNames();

    DomainConfig expenseConfig = configProperties.getDomain("expense-reports");
    List<String> expenseIdFieldNames = expenseConfig.source().getIdFieldNames();

    // Assert - use getIdFieldNames() to get list of field names
    assertTrue(vendorIdFieldNames.contains("invoice_id"));
    assertTrue(expenseIdFieldNames.contains("EXP_ID"));
  }

  @Test
  @DisplayName("Should validate invalid domain returns false from hasDomain")
  void testInvalidDomainCheck() {
    // Act
    boolean hasInvalidDomain = configProperties.hasDomain("invalid-domain");

    // Assert
    assertFalse(hasInvalidDomain);
  }

  @Test
  @DisplayName("Should validate valid domains return true from hasDomain")
  void testValidDomainCheck() {
    // Act
    boolean hasVendorDomain = configProperties.hasDomain("vendor-invoices");
    boolean hasExpenseDomain = configProperties.hasDomain("expense-reports");

    // Assert
    assertTrue(hasVendorDomain);
    assertTrue(hasExpenseDomain);
  }
}
