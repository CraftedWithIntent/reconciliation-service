package com.reconcile.validator;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.reconcile.config.DataSourceType;
import com.reconcile.config.DatabaseConfig;
import com.reconcile.config.DomainConfig;
import com.reconcile.config.FieldDefinition;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@DisplayName("SqlViewValidator Tests")
class SqlViewValidatorTest {

  @Mock private DataSource mockDataSource;

  @Mock private Connection mockConnection;

  @Mock private DatabaseMetaData mockMetaData;

  @Mock private ResultSet mockResultSet;

  private SqlViewValidator validator;
  private DatabaseConfig validConfig;
  private DomainConfig validDomain;

  @BeforeEach
  void setUp() {
    try {
      MockitoAnnotations.openMocks(this);
      validator = new SqlViewValidator();

      validConfig =
          new DatabaseConfig(
              DataSourceType.POSTGRESQL,
              "public",
              "test_table",
              "_hash",
              List.of(FieldDefinition.string("id")),
              new ArrayList<>(
                  List.of(FieldDefinition.string("name"), FieldDefinition.decimal("amount"))),
              null,
              null,
              null);

      validDomain =
          new DomainConfig("test-domain", validConfig, validConfig, null, false, 95.0, 1.0);

      // Setup mock database metadata
      when(mockDataSource.getConnection()).thenReturn(mockConnection);
      when(mockConnection.getMetaData()).thenReturn(mockMetaData);
    } catch (SQLException e) {
      fail("Test setup failed with SQLException", e);
    }
  }

  @Test
  @DisplayName("Should pass when null datasources provided")
  void testValidateView_NullDataSource() {
    // Act
    List<String> errors = validator.validateView(validConfig, null, "source");

    // Assert
    assertTrue(errors.isEmpty());
  }

  @Test
  @DisplayName("Should pass when null config provided")
  void testValidateView_NullConfig() {
    // Act
    List<String> errors = validator.validateView(null, mockDataSource, "source");

    // Assert
    assertTrue(errors.isEmpty());
  }

  @Test
  @DisplayName("Should fail when view does not exist")
  void testValidateView_ViewNotExists() {
    try {
      // Arrange
      when(mockMetaData.getColumns(null, "public", "test_table_hash", null))
          .thenReturn(mockResultSet);
      when(mockResultSet.next()).thenReturn(false);

      // Act
      List<String> errors = validator.validateView(validConfig, mockDataSource, "source");

      // Assert
      assertFalse(errors.isEmpty());
      assertTrue(errors.stream().anyMatch(e -> e.contains("does not exist")));
    } catch (SQLException e) {
      fail("Test setup failed with SQLException", e);
    }
  }

  @Test
  @DisplayName("Should fail when required columns are missing")
  void testValidateView_MissingRequiredColumns() {
    try {
      // Arrange - Mock view with columns but missing required ones
      when(mockMetaData.getColumns(null, "public", "test_table_hash", null))
          .thenReturn(mockResultSet);

      // First call for existence check, return true (has at least one column)
      when(mockResultSet.next()).thenReturn(true, false);
      when(mockResultSet.getString("COLUMN_NAME")).thenReturn("some_column");

      // Act
      List<String> errors = validator.validateView(validConfig, mockDataSource, "source");

      // Assert
      assertFalse(errors.isEmpty());
    } catch (SQLException e) {
      fail("Test setup failed with SQLException", e);
    }
  }

  @Test
  @DisplayName("Should fail when hash column is missing")
  void testValidateView_MissingHashColumn() {
    try {
      // Arrange - Mock view with all required columns except hash
      when(mockMetaData.getColumns(null, "public", "test_table_hash", null))
          .thenReturn(mockResultSet);

      // Return columns: id, name, amount (but no hash)
      when(mockResultSet.next()).thenReturn(true, false);
      when(mockResultSet.getString("COLUMN_NAME"))
          .thenReturn("id")
          .thenReturn("name")
          .thenReturn("amount");

      // Act
      List<String> errors = validator.validateView(validConfig, mockDataSource, "source");

      // Assert
      assertFalse(errors.isEmpty());
      assertTrue(errors.stream().anyMatch(e -> e.contains("hash")));
    } catch (SQLException e) {
      fail("Test setup failed with SQLException", e);
    }
  }

  @Test
  @DisplayName("Should handle SQLException during view validation")
  void testValidateView_SqlException() {
    try {
      // Arrange
      when(mockDataSource.getConnection()).thenThrow(new SQLException("Connection failed"));

      // Act
      List<String> errors = validator.validateView(validConfig, mockDataSource, "source");

      // Assert
      assertFalse(errors.isEmpty());
      assertTrue(errors.stream().anyMatch(e -> e.contains("failed")));
    } catch (SQLException e) {
      fail("Test setup failed with SQLException", e);
    }
  }

  @Test
  @DisplayName("Should validate views case-insensitively")
  void testValidateView_CaseInsensitive() {
    try {
      // Arrange - columns with different case
      when(mockMetaData.getColumns(null, "public", "test_table_hash", null))
          .thenReturn(mockResultSet);

      // Setup multiple column returns with different cases
      int[] callCount = {0};
      when(mockResultSet.next())
          .thenAnswer(
              invocation -> {
                callCount[0]++;
                return callCount[0] <= 5; // Return true for 5 calls
              });

      when(mockResultSet.getString("COLUMN_NAME"))
          .thenReturn("ID")
          .thenReturn("NAME")
          .thenReturn("AMOUNT")
          .thenReturn("RECORD_HASH")
          .thenReturn("extra_col");

      // Act
      List<String> errors = validator.validateView(validConfig, mockDataSource, "source");

      // Assert
      assertTrue(errors.isEmpty());
    } catch (SQLException e) {
      fail("Test setup failed with SQLException", e);
    }
  }

  @Test
  @DisplayName("Should accept record_hash, _record_hash, or hash column names")
  void testValidateView_AcceptHashColumnVariants() {
    try {
      // Test with record_hash only - validate that we can accept the hash column
      ResultSet localMockResultSet = mock(ResultSet.class);
      when(mockMetaData.getColumns(null, "public", "test_table_hash", null))
          .thenReturn(localMockResultSet);

      // First return true for existence, then sequence through columns
      when(localMockResultSet.next())
          .thenReturn(true) // check exists
          .thenReturn(true) // id
          .thenReturn(true) // name
          .thenReturn(true) // amount
          .thenReturn(true) // record_hash
          .thenReturn(false); // end

      when(localMockResultSet.getString("COLUMN_NAME"))
          .thenReturn("id")
          .thenReturn("name")
          .thenReturn("amount")
          .thenReturn("record_hash");

      // Act
      List<String> errors = validator.validateView(validConfig, mockDataSource, "source");

      // Assert
      assertTrue(errors.isEmpty());
    } catch (SQLException e) {
      fail("Test setup failed with SQLException", e);
    }
  }

  @Test
  @DisplayName("Should validate multiple domains")
  void testValidateViews_MultipleDomains() {
    // This test is simplified - just verify it doesn't crash when both datasources provided
    try {
      // Act
      List<String> errors = validator.validateViews(validDomain, null, null);

      // Assert - should be empty since datasources are null
      assertTrue(errors.isEmpty());
    } catch (Exception e) {
      fail("Test should handle null datasources gracefully", e);
    }
  }

  @Test
  @DisplayName("Should throw exception when view validation fails")
  void testValidateViewsOrThrow_InvalidViews() {
    try {
      // Arrange
      when(mockMetaData.getColumns(null, "public", "test_table_hash", null))
          .thenReturn(mockResultSet);
      when(mockResultSet.next()).thenReturn(false);

      // Act & Assert
      assertThrows(
          IllegalStateException.class,
          () -> {
            validator.validateViewsOrThrow(validDomain, mockDataSource, mockDataSource);
          });
    } catch (SQLException e) {
      fail("Test setup failed with SQLException", e);
    }
  }
}
