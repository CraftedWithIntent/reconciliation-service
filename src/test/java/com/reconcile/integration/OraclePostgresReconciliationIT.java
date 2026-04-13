package com.reconcile.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.reconcile.config.DataSourceType;
import com.reconcile.config.DatabaseConfig;
import com.reconcile.config.DomainConfig;
import com.reconcile.config.FieldDefinition;
import com.reconcile.engine.SparkReconciliationEngine;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import org.apache.spark.sql.SparkSession;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.OracleContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Integration test: Oracle source + PostgreSQL target reconciliation Tests heterogeneous database
 * reconciliation with various data discrepancy scenarios.
 *
 * <p>Uses Testcontainers to spin up real Oracle and PostgreSQL instances for testing. REQUIRES
 * Docker to be installed and running.
 *
 * <p>Run with: ./gradlew test --tests '*IT' Or exclude by default when running regular test suite.
 */
@Testcontainers
@Tag("integration")
@DisplayName("Oracle → PostgreSQL Heterogeneous Reconciliation Integration Tests")
class OraclePostgresReconciliationIT {

  @Container
  static OracleContainer oracleContainer =
      new OracleContainer("gvenzl/oracle-xe:21-slim-faststart")
          .withPassword("oracle")
          .withReuse(true);

  @Container
  static PostgreSQLContainer<?> postgresContainer =
      new PostgreSQLContainer<>("postgres:15-alpine").withPassword("postgres").withReuse(true);

  private static Connection oracleConnection;
  private static Connection postgresConnection;
  private static SparkSession sparkSession;

  @BeforeAll
  static void setUp() throws SQLException {
    System.out.println("\n=== Integration Test Setup Starting ===");
    System.out.println("Attempting to connect to Docker Desktop on macOS...");
    System.out.println("Docker socket: /Users/philipthomas/.docker/run/docker.sock");

    try {
      // Initialize SparkSession for reconciliation engine
      sparkSession =
          SparkSession.builder()
              .master("local[2]")
              .appName("OraclePostgresReconciliationIT")
              .getOrCreate();
      sparkSession.sparkContext().setLogLevel("ERROR");
      System.out.println("✓ SparkSession initialized");

      // Setup Oracle connection
      System.out.println("Starting Oracle container (gvenzl/oracle-xe:21-slim-faststart)...");
      oracleConnection =
          DriverManager.getConnection(oracleContainer.getJdbcUrl(), "system", "oracle");
      System.out.println("✓ Oracle connection established");

      // Setup Postgres connection
      System.out.println("Starting PostgreSQL container (postgres:15-alpine)...");
      postgresConnection =
          DriverManager.getConnection(
              postgresContainer.getJdbcUrl(),
              postgresContainer.getUsername(),
              postgresContainer.getPassword());
      System.out.println("✓ PostgreSQL connection established");

      // Create tables and initial data on both databases
      initializeOracleDatabase();
      initializePostgresDatabase();
      System.out.println("✓ Database schemas initialized");
      System.out.println("=== Integration Test Setup Complete ===\n");

    } catch (Exception e) {
      System.err.println("❌ Integration test setup failed!");
      System.err.println("ERROR: " + e.getClass().getSimpleName() + ": " + e.getMessage());
      System.err.println("\nTo run integration tests:");
      System.err.println("  1. Ensure Docker Desktop is running on macOS");
      System.err.println("  2. Run: ./gradlew integrationTest");
      System.err.println("\nOr manually:");
      System.err.println("  export DOCKER_HOST=unix:///Users/$USER/.docker/run/docker.sock");
      System.err.println("  ./gradlew integrationTest");
      e.printStackTrace();
      throw e;
    }
  }

  /** Create tables in Oracle (source database) */
  private static void initializeOracleDatabase() throws SQLException {
    try (Statement stmt = oracleConnection.createStatement()) {
      stmt.execute(
          "CREATE TABLE vendor_invoices ("
              + "  id VARCHAR2(36) PRIMARY KEY, "
              + "  vendor_name VARCHAR2(255) NOT NULL, "
              + "  amount NUMBER(15,2), "
              + "  billing_cycle DATE NOT NULL, "
              + "  line_item_id VARCHAR2(255) NOT NULL, "
              + "  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
              + "  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
              + ")");
    }
  }

  /** Create tables in PostgreSQL (target database) */
  private static void initializePostgresDatabase() throws SQLException {
    try (Statement stmt = postgresConnection.createStatement()) {
      stmt.execute(
          "CREATE TABLE vendor_invoices ("
              + "  id VARCHAR(36) PRIMARY KEY, "
              + "  vendor_name TEXT NOT NULL, "
              + "  amount NUMERIC(15,2), "
              + "  billing_cycle DATE NOT NULL, "
              + "  line_item_id TEXT NOT NULL, "
              + "  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
              + "  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
              + ")");
    }
  }

  /**
   * Scenario 1: Pristine Data - Both databases have identical data Expected: 100% match, zero
   * discrepancies
   */
  @Test
  @DisplayName("Should report zero discrepancies for pristine data")
  void testPristineDataReconciliation() throws SQLException {
    // Arrange
    String testId = "550e8400-e29b-41d4-a716-446655440001";

    // Insert identical data into both databases
    insertOracleData(testId, "Acme Corp", 1500.50, "2024-01-15", "LI001");
    insertPostgresData(testId, "Acme Corp", 1500.50, "2024-01-15", "LI001");

    // Create domain configuration for heterogeneous sources
    DomainConfig domainConfig = createVendorInvoicesDomain();

    // Act
    var result = executeReconciliation(domainConfig);

    // Assert
    assertEquals(1, result.totalSource, "Should have 1 record in source");
    assertEquals(1, result.totalTarget, "Should have 1 record in target");
    assertEquals(1, result.matchedCount, "All records should match");
    assertEquals(0, result.mismatchedCount, "Zero mismatches for pristine data");
  }

  /**
   * Scenario 2: Noisy Data - Minor differences that don't affect reconciliation hash Expected:
   * Discrepancies detected when non-hash fields differ
   */
  @Test
  @DisplayName("Should detect noisy discrepancies in non-hash fields")
  void testNoisyDataReconciliation() throws SQLException {
    // Arrange
    String testId = "550e8400-e29b-41d4-a716-446655440002";

    // Insert same hash fields, different metadata
    insertOracleData(testId, "Acme Corp", 1500.50, "2024-01-15", "LI002");
    insertPostgresData(testId, "Acme Corp", 1500.50, "2024-01-15", "LI002");

    // Update one record's non-hash field (created_at)
    updateOracleCreatedAt(testId, "2024-01-16 10:30:00");

    // Create domain configuration
    DomainConfig domainConfig = createVendorInvoicesDomain();

    // Act
    var result = executeReconciliation(domainConfig);

    // Assert
    assertEquals(1, result.totalSource, "Should have 1 record in source");
    assertEquals(1, result.totalTarget, "Should have 1 record in target");
    assertEquals(1, result.matchedCount, "Record should match (hash fields identical)");
    assertEquals(0, result.mismatchedCount, "Noisy mismatch should not be counted");
  }

  /**
   * Scenario 3: Material Discrepancies - Significant differences in hash-contributing fields
   * Expected: Hash mismatch, clear discrepancy detection
   */
  @Test
  @DisplayName("Should detect material discrepancies in hash fields")
  void testMaterialDiscrepanciesReconciliation() throws SQLException {
    // Arrange
    String testId = "550e8400-e29b-41d4-a716-446655440003";

    // Insert same ID but different amount (hash field)
    insertOracleData(testId, "Acme Corp", 1500.50, "2024-01-15", "LI003");
    insertPostgresData(testId, "Acme Corp", 1600.75, "2024-01-15", "LI003"); // Different amount

    // Create domain configuration
    DomainConfig domainConfig = createVendorInvoicesDomain();

    // Act
    var result = executeReconciliation(domainConfig);

    // Assert
    assertEquals(1, result.totalSource, "Should have 1 record in source");
    assertEquals(1, result.totalTarget, "Should have 1 record in target");
    assertEquals(0, result.matchedCount, "Record should not match due to amount difference");
    assertEquals(1, result.mismatchedCount, "Should detect material discrepancy");
  }

  /**
   * Scenario 4: Multi-record reconciliation with mixed results Expected: Some pristine, some
   * material
   */
  @Test
  @DisplayName("Should handle multi-record reconciliation with mixed discrepancies")
  void testMultiRecordMixedDiscrepancies() throws SQLException {
    // Arrange - Clear previous data
    clearAllData();

    // Pristine record
    String id1 = "550e8400-e29b-41d4-a716-446655440004";
    insertOracleData(id1, "Pristine Inc", 2000.00, "2024-02-01", "LI004");
    insertPostgresData(id1, "Pristine Inc", 2000.00, "2024-02-01", "LI004");

    // Material discrepancy record
    String id2 = "550e8400-e29b-41d4-a716-446655440005";
    insertOracleData(id2, "Material Corp", 5000.00, "2024-02-02", "LI005");
    insertPostgresData(id2, "Material Corp", 5500.00, "2024-02-02", "LI005"); // Amount differs

    // Another pristine record
    String id3 = "550e8400-e29b-41d4-a716-446655440006";
    insertOracleData(id3, "Echo LLC", 3500.25, "2024-02-03", "LI006");
    insertPostgresData(id3, "Echo LLC", 3500.25, "2024-02-03", "LI006");

    // Create domain configuration
    DomainConfig domainConfig = createVendorInvoicesDomain();

    // Act
    var result = executeReconciliation(domainConfig);

    // Assert
    assertEquals(3, result.totalSource, "Should have 3 records in source");
    assertEquals(3, result.totalTarget, "Should have 3 records in target");
    assertEquals(2, result.matchedCount, "2 records should match");
    assertEquals(1, result.mismatchedCount, "1 record should have mismatches");
  }

  /** Scenario 5: Source-only and target-only records */
  @Test
  @DisplayName("Should detect source-only and target-only records")
  void testSourceTargetOnlyRecords() throws SQLException {
    // Arrange - Clear previous data
    clearAllData();

    // Source-only record
    String sourceId = "550e8400-e29b-41d4-a716-446655440007";
    insertOracleData(sourceId, "Source Only Corp", 1000.00, "2024-03-01", "LI007");

    // Target-only record
    String targetId = "550e8400-e29b-41d4-a716-446655440008";
    insertPostgresData(targetId, "Target Only Inc", 2000.00, "2024-03-02", "LI008");

    // Matching record
    String matchId = "550e8400-e29b-41d4-a716-446655440009";
    insertOracleData(matchId, "Match Corp", 3000.00, "2024-03-03", "LI009");
    insertPostgresData(matchId, "Match Corp", 3000.00, "2024-03-03", "LI009");

    // Create domain configuration
    DomainConfig domainConfig = createVendorInvoicesDomain();

    // Act
    var result = executeReconciliation(domainConfig);

    // Assert
    assertEquals(2, result.totalSource, "Should have 2 source records");
    assertEquals(2, result.totalTarget, "Should have 2 target records");
    assertEquals(1, result.matchedCount, "1 record should match");
    assertEquals(1, result.sourceOnlyCount, "1 record source-only");
    assertEquals(1, result.targetOnlyCount, "1 record target-only");
  }

  // ============ Helper Methods ============

  /** Execute reconciliation using SparkReconciliationEngine */
  private SparkReconciliationEngine.ReconciliationResult executeReconciliation(
      DomainConfig domainConfig) {
    SparkReconciliationEngine engine =
        new SparkReconciliationEngine(
            sparkSession,
            domainConfig,
            oracleContainer.getJdbcUrl(),
            "system",
            "oracle",
            postgresContainer.getJdbcUrl(),
            postgresContainer.getUsername(),
            postgresContainer.getPassword());

    return engine.reconcile();
  }

  /** Create DomainConfig for vendor_invoices with Oracle source and Postgres target */
  private DomainConfig createVendorInvoicesDomain() {
    DatabaseConfig sourceConfig =
        new DatabaseConfig(
            DataSourceType.ORACLE,
            "SYSTEM",
            "vendor_invoices",
            "_with_hash",
            List.of(FieldDefinition.string("id")),
            new ArrayList<>(
                List.of(
                    FieldDefinition.string("vendor_name"),
                    FieldDefinition.decimal("amount"),
                    FieldDefinition.string("line_item_id"))));

    DatabaseConfig targetConfig =
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
                    FieldDefinition.string("line_item_id"))));

    return new DomainConfig("vendor_invoices", sourceConfig, targetConfig, null, false);
  }

  /** Helper: Insert row into Oracle */
  private static void insertOracleData(
      String id, String vendorName, double amount, String billingCycle, String lineItemId)
      throws SQLException {
    String sql =
        "INSERT INTO vendor_invoices (id, vendor_name, amount, billing_cycle, line_item_id) "
            + "VALUES (?, ?, ?, TO_DATE(?, 'YYYY-MM-DD'), ?)";
    try (PreparedStatement pstmt = oracleConnection.prepareStatement(sql)) {
      pstmt.setString(1, id);
      pstmt.setString(2, vendorName);
      pstmt.setDouble(3, amount);
      pstmt.setString(4, billingCycle);
      pstmt.setString(5, lineItemId);
      pstmt.executeUpdate();
      oracleConnection.commit();
    }
  }

  /** Helper: Insert row into PostgreSQL */
  private static void insertPostgresData(
      String id, String vendorName, double amount, String billingCycle, String lineItemId)
      throws SQLException {
    String sql =
        "INSERT INTO vendor_invoices (id, vendor_name, amount, billing_cycle, line_item_id) "
            + "VALUES (?, ?, ?, ?::date, ?)";
    try (PreparedStatement pstmt = postgresConnection.prepareStatement(sql)) {
      pstmt.setString(1, id);
      pstmt.setString(2, vendorName);
      pstmt.setDouble(3, amount);
      pstmt.setString(4, billingCycle);
      pstmt.setString(5, lineItemId);
      pstmt.executeUpdate();
      postgresConnection.commit();
    }
  }

  /** Helper: Update Oracle record's created_at timestamp */
  private static void updateOracleCreatedAt(String id, String timestamp) throws SQLException {
    String sql =
        "UPDATE vendor_invoices SET created_at = TO_TIMESTAMP(?, 'YYYY-MM-DD HH24:MI:SS') "
            + "WHERE id = ?";
    try (PreparedStatement pstmt = oracleConnection.prepareStatement(sql)) {
      pstmt.setString(1, timestamp);
      pstmt.setString(2, id);
      pstmt.executeUpdate();
      oracleConnection.commit();
    }
  }

  /** Helper: Clear all data from both databases */
  private static void clearAllData() throws SQLException {
    try (Statement stmt = oracleConnection.createStatement()) {
      stmt.execute("DELETE FROM vendor_invoices");
      oracleConnection.commit();
    }
    try (Statement stmt = postgresConnection.createStatement()) {
      stmt.execute("DELETE FROM vendor_invoices");
      postgresConnection.commit();
    }
  }

  @AfterAll
  static void tearDown() throws SQLException {
    if (oracleConnection != null && !oracleConnection.isClosed()) {
      oracleConnection.close();
    }
    if (postgresConnection != null && !postgresConnection.isClosed()) {
      postgresConnection.close();
    }
    if (sparkSession != null) {
      sparkSession.stop();
    }
  }
}
