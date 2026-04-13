package com.reconcile.validator;

import com.reconcile.config.DatabaseConfig;
import com.reconcile.config.DomainConfig;
import java.util.ArrayList;
import java.util.List;

/**
 * Validator for domain configurations. Validates source and target database configurations, field
 * definitions, and consistency.
 */
public class DomainConfigValidator {

  /**
   * Validates a complete domain configuration
   *
   * @param domainConfig the domain configuration to validate
   * @return list of validation errors (empty if valid)
   */
  public static List<String> validate(DomainConfig domainConfig) {
    List<String> errors = new ArrayList<>();

    if (domainConfig == null) {
      errors.add("Domain configuration cannot be null");
      return errors;
    }

    // Validate domain name
    if (domainConfig.name() == null || domainConfig.name().trim().isEmpty()) {
      errors.add("Domain name cannot be empty");
    }

    // Validate source configuration
    if (domainConfig.source() == null) {
      errors.add(
          "Source database configuration is required for domain '" + domainConfig.name() + "'");
    } else {
      errors.addAll(validateDatabaseConfig(domainConfig.source(), "source"));
    }

    // Validate target configuration
    if (domainConfig.target() == null) {
      errors.add(
          "Target database configuration is required for domain '" + domainConfig.name() + "'");
    } else {
      errors.addAll(validateDatabaseConfig(domainConfig.target(), "target"));
    }

    // Cross-database validations (only if both exist)
    if (domainConfig.source() != null && domainConfig.target() != null) {
      errors.addAll(validateConsistency(domainConfig));
    }

    return errors;
  }

  /**
   * Validates a single database configuration
   *
   * @param config the database configuration
   * @param dbType "source" or "target"
   * @return list of validation errors for this database
   */
  private static List<String> validateDatabaseConfig(DatabaseConfig config, String dbType) {
    List<String> errors = new ArrayList<>();

    String prefix = "Database config (" + dbType + "): ";

    // Validate schema
    if (config.schema() == null || config.schema().trim().isEmpty()) {
      errors.add(prefix + "schema cannot be empty");
    }

    // Validate table
    if (config.table() == null || config.table().trim().isEmpty()) {
      errors.add(prefix + "table cannot be empty");
    }

    // Validate idFields (array of fields for composite IDs)
    if (config.idFields() == null || config.idFields().isEmpty()) {
      errors.add(prefix + "idFields is required (array of field definitions)");
    } else {
      for (int i = 0; i < config.idFields().size(); i++) {
        var field = config.idFields().get(i);
        if (field.name() == null || field.name().trim().isEmpty()) {
          errors.add(prefix + "idFields[" + i + "].name cannot be empty");
        }
        if (field.type() == null || field.type().trim().isEmpty()) {
          errors.add(prefix + "idFields[" + i + "].type cannot be empty");
        } else if (!isValidDataType(field.type())) {
          errors.add(
              prefix
                  + "idFields["
                  + i
                  + "].type '"
                  + field.type()
                  + "' is not a supported datatype");
        }
      }
    }

    // Validate hashFields
    if (config.hashFields() == null || config.hashFields().isEmpty()) {
      errors.add(prefix + "hashFields cannot be empty");
    } else {
      for (int i = 0; i < config.hashFields().size(); i++) {
        var field = config.hashFields().get(i);
        if (field.name() == null || field.name().trim().isEmpty()) {
          errors.add(prefix + "hashFields[" + i + "].name cannot be empty");
        }
        if (field.type() == null || field.type().trim().isEmpty()) {
          errors.add(prefix + "hashFields[" + i + "].type cannot be empty");
        } else if (!isValidDataType(field.type())) {
          errors.add(
              prefix
                  + "hashFields["
                  + i
                  + "].type '"
                  + field.type()
                  + "' is not a supported datatype");
        }
      }
    }

    // Validate viewSuffix
    if (config.viewSuffix() == null) {
      errors.add(prefix + "viewSuffix cannot be null (use empty string if not needed)");
    }

    return errors;
  }

  /**
   * Validates consistency between source and target configurations
   *
   * @param domainConfig the domain configuration with both source and target
   * @return list of validation errors
   */
  private static List<String> validateConsistency(DomainConfig domainConfig) {
    List<String> errors = new ArrayList<>();

    DatabaseConfig source = domainConfig.source();
    DatabaseConfig target = domainConfig.target();

    // Check hashFields size consistency
    int sourceHashSize = source.hashFields() != null ? source.hashFields().size() : 0;
    int targetHashSize = target.hashFields() != null ? target.hashFields().size() : 0;

    if (sourceHashSize != targetHashSize) {
      errors.add(
          "Hash field count mismatch: source has "
              + sourceHashSize
              + " fields, target has "
              + targetHashSize
              + " fields");
    }

    // Check if idFields are compatible for joining (composite key support)
    if (source.idFields() != null && target.idFields() != null) {
      int sourceIdCount = source.idFields().size();
      int targetIdCount = target.idFields().size();

      if (sourceIdCount != targetIdCount) {
        errors.add(
            "Composite ID field count mismatch: source has "
                + sourceIdCount
                + " ID field(s), target has "
                + targetIdCount
                + " ID field(s)");
      } else {
        // Check type compatibility for each ID field pair
        for (int i = 0; i < sourceIdCount; i++) {
          String sourceIdType = source.idFields().get(i).type();
          String targetIdType = target.idFields().get(i).type();

          if (!areTypesCompatible(sourceIdType, targetIdType)) {
            errors.add(
                "Composite ID field["
                    + i
                    + "] types may not be compatible for joining: "
                    + "source="
                    + sourceIdType
                    + ", target="
                    + targetIdType);
          }
        }
      }
    }

    return errors;
  }

  /**
   * Checks if a datatype is valid
   *
   * @param type the datatype string
   * @return true if valid
   */
  private static boolean isValidDataType(String type) {
    if (type == null || type.trim().isEmpty()) {
      return false;
    }

    String normalized = type.toUpperCase();

    return normalized.matches(
        "^(STRING|VARCHAR|CHAR|INT|INTEGER|BIGINT|SMALLINT|"
            + "DECIMAL|NUMERIC|FLOAT|DOUBLE|DATE|TIMESTAMP|DATETIME|"
            + "UUID|UUIDV4|BOOLEAN|BOOL|BYTES|BINARY)$");
  }

  /**
   * Checks if two datatypes are compatible for joining Some types can be auto-coerced during joins
   *
   * @param sourceType the source field type
   * @param targetType the target field type
   * @return true if types can be joined with coercion
   */
  private static boolean areTypesCompatible(String sourceType, String targetType) {
    if (sourceType == null || targetType == null) {
      return false;
    }

    String src = sourceType.toUpperCase();
    String tgt = targetType.toUpperCase();

    // Same types are always compatible
    if (src.equals(tgt)) {
      return true;
    }

    // STRING/VARCHAR/CHAR are compatible with most types (can be coerced)
    if (isStringType(src) && isStringType(tgt)) {
      return true;
    }

    // Integer types are compatible with each other
    if (isIntegerType(src) && isIntegerType(tgt)) {
      return true;
    }

    // Numeric types are compatible with each other
    if (isNumericType(src) && isNumericType(tgt)) {
      return true;
    }

    // Date/time types are compatible with each other
    if (isDateTimeType(src) && isDateTimeType(tgt)) {
      return true;
    }

    // UUID can be coerced to/from STRING
    if ((src.equals("UUID") || src.equals("UUIDV4")) && isStringType(tgt)) {
      return true;
    }
    if (isStringType(src) && (tgt.equals("UUID") || tgt.equals("UUIDV4"))) {
      return true;
    }

    // UUID can be coerced to/from other UUID types
    if ((src.equals("UUID") || src.equals("UUIDV4"))
        && (tgt.equals("UUID") || tgt.equals("UUIDV4"))) {
      return true;
    }

    return false;
  }

  private static boolean isStringType(String type) {
    return type.matches("^(STRING|VARCHAR|CHAR)$");
  }

  private static boolean isIntegerType(String type) {
    return type.matches("^(INT|INTEGER|BIGINT|SMALLINT)$");
  }

  private static boolean isNumericType(String type) {
    return type.matches("^(DECIMAL|NUMERIC|FLOAT|DOUBLE|INT|INTEGER|BIGINT|SMALLINT)$");
  }

  private static boolean isDateTimeType(String type) {
    return type.matches("^(DATE|TIMESTAMP|DATETIME)$");
  }

  /**
   * Throws an exception if domain configuration is invalid
   *
   * @param domainConfig the domain configuration to validate
   * @throws IllegalArgumentException if validation fails
   */
  public static void validateOrThrow(DomainConfig domainConfig) {
    List<String> errors = validate(domainConfig);
    if (!errors.isEmpty()) {
      throw new IllegalArgumentException(
          "Invalid domain configuration: " + String.join("; ", errors));
    }
  }
}
