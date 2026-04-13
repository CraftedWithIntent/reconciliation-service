package com.reconcile.engine;

import com.reconcile.config.DataSourceType;
import com.reconcile.config.DomainConfig;
import com.reconcile.dto.Discrepancy;
import com.reconcile.dto.FieldVariance;
import com.reconcile.dto.ReconciliationRecord;
import com.reconcile.dto.ReconciliationVerdict;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;

/**
 * Generic Spark Reconciliation Engine Supports heterogeneous data sources: Oracle, PostgreSQL,
 * MySQL, and other JDBC sources Uses MD5-based row hashing for efficient reconciliation with
 * distributed processing
 */
public class SparkReconciliationEngine {

  private final SparkSession sparkSession;
  private final DomainConfig domainConfig;
  private final String sourceUrl;
  private final String sourceUsername;
  private final String sourcePassword;
  private final String targetUrl;
  private final String targetUsername;
  private final String targetPassword;

  public SparkReconciliationEngine(
      SparkSession sparkSession,
      DomainConfig domainConfig,
      String sourceUrl,
      String sourceUsername,
      String sourcePassword,
      String targetUrl,
      String targetUsername,
      String targetPassword) {
    this.sparkSession = sparkSession;
    this.domainConfig = domainConfig;
    this.sourceUrl = sourceUrl;
    this.sourceUsername = sourceUsername;
    this.sourcePassword = sourcePassword;
    this.targetUrl = targetUrl;
    this.targetUsername = targetUsername;
    this.targetPassword = targetPassword;
  }

  /** Detect database type from JDBC URL string */
  private String detectDatabaseType(String url) {
    if (url.contains("oracle")) return "oracle";
    if (url.contains("postgresql")) return "postgresql";
    if (url.contains("mysql")) return "mysql";
    if (url.contains("sqlserver")) return "sqlserver";
    return "unknown";
  }

  /** Get appropriate JDBC driver class based on database type */
  private String getDriverClass(DataSourceType dataSourceType) {
    return dataSourceType.getDriverClass();
  }

  /** Read source data with MD5 hashes from configured view/table */
  private Dataset<Row> readSourceData() {
    String view = domainConfig.source().getFullViewName();
    DataSourceType dbType =
        domainConfig.source().type() != null
            ? domainConfig.source().type()
            : DataSourceType.fromUrl(sourceUrl);
    System.out.println(
        "[SparkReconciliationEngine] Reading source data (" + dbType.getName() + ") from " + view);

    return sparkSession
        .read()
        .format("jdbc")
        .option("url", sourceUrl)
        .option("user", sourceUsername)
        .option("password", sourcePassword)
        .option("dbtable", view)
        .option("driver", getDriverClass(dbType))
        .load();
  }

  /** Read target data with MD5 hashes from configured view/table */
  private Dataset<Row> readTargetData() {
    String view = domainConfig.target().getFullViewName();
    // Use explicit type from target config, fallback to URL detection if not specified
    DataSourceType dbType =
        domainConfig.target().type() != null
            ? domainConfig.target().type()
            : DataSourceType.fromUrl(targetUrl);
    System.out.println(
        "[SparkReconciliationEngine] Reading target data (" + dbType.getName() + ") from " + view);

    return sparkSession
        .read()
        .format("jdbc")
        .option("url", targetUrl)
        .option("user", targetUsername)
        .option("password", targetPassword)
        .option("dbtable", view)
        .option("driver", getDriverClass(dbType))
        .load();
  }

  /**
   * Calculate percentage variance for numeric values Returns null if either value is not numeric
   */
  private Double calculateNumericVariance(Object sourceValue, Object targetValue) {
    try {
      if (sourceValue == null || targetValue == null) {
        return null; // One is null, not comparable numerically
      }

      double src = Double.parseDouble(sourceValue.toString());
      double tgt = Double.parseDouble(targetValue.toString());

      if (src == 0 && tgt == 0) {
        return 0.0; // Both zero
      }

      double baseValue = Math.abs(src);
      if (baseValue == 0) {
        baseValue = Math.abs(tgt); // Use target if source is zero
      }

      if (baseValue == 0) {
        return 0.0; // Both are effectively zero
      }

      double variance = Math.abs(src - tgt) / baseValue * 100.0;
      return variance;
    } catch (NumberFormatException e) {
      return null; // Not numeric
    }
  }

  /** Analyze field-level variance between source and target records (for raw row objects) */
  private List<FieldVariance> analyzeFieldVariances(Row srcRow, Row tgtRow) {
    List<FieldVariance> variances = new ArrayList<>();
    // Extract field names from FieldDefinition objects
    List<String> srcHashFields =
        domainConfig.source().hashFields() != null
            ? domainConfig.source().hashFields().stream().map(fd -> fd.name()).toList()
            : null;
    List<String> tgtHashFields =
        domainConfig.target().hashFields() != null
            ? domainConfig.target().hashFields().stream().map(fd -> fd.name()).toList()
            : null;

    if (srcHashFields == null
        || srcHashFields.isEmpty()
        || tgtHashFields == null
        || tgtHashFields.isEmpty()) {
      return variances;
    }

    // Source and target hash fields should be in corresponding order
    for (int i = 0; i < srcHashFields.size() && i < tgtHashFields.size(); i++) {
      String srcFieldName = srcHashFields.get(i);
      String tgtFieldName = tgtHashFields.get(i);

      try {
        Object srcValue = null;
        Object tgtValue = null;
        String varianceType = "UNKNOWN";
        Double variancePercent = null;

        // Extract field values
        try {
          srcValue = srcRow.get(srcRow.fieldIndex(srcFieldName));
        } catch (Exception e) {
          srcValue = null;
        }

        try {
          tgtValue = tgtRow.get(tgtRow.fieldIndex(tgtFieldName));
        } catch (Exception e) {
          tgtValue = null;
        }

        // Determine variance type and percentage
        if (srcValue == null && tgtValue == null) {
          varianceType = "EXACT_MATCH";
          variancePercent = 0.0;
        } else if (srcValue == null || tgtValue == null) {
          varianceType = "NULL_MISMATCH";
          variancePercent = 100.0; // Null vs non-null is 100% different
        } else if (srcValue.toString().equals(tgtValue.toString())) {
          varianceType = "EXACT_MATCH";
          variancePercent = 0.0;
        } else {
          // Try numeric variance first
          Double numericVariance = calculateNumericVariance(srcValue, tgtValue);
          if (numericVariance != null) {
            varianceType = "NUMERIC";
            variancePercent = numericVariance;
          } else {
            varianceType = "TEXT"; // Text fields are either match or don't - no percentage
            variancePercent = null; // No variance percentage for text
          }
        }

        FieldVariance variance =
            new FieldVariance(srcFieldName, srcValue, tgtValue, variancePercent, varianceType);
        variances.add(variance);
      } catch (Exception e) {
        // Skip fields that can't be analyzed
        System.err.println(
            "[SparkReconciliationEngine] Error analyzing field '"
                + srcFieldName
                + "': "
                + e.getMessage());
      }
    }

    return variances;
  }

  /**
   * Determine verdict based on hash match and field variance analysis PRISTINE: Hash matches (100%)
   * NOISY: Fields variance all <= 1% MATERIAL: Any field variance > 1%
   */
  private ReconciliationVerdict determineVerdict(
      String srcHash, String tgtHash, List<FieldVariance> fieldVariances) {
    // If hashes match exactly, it's PRISTINE (100% match)
    if (srcHash != null && tgtHash != null && srcHash.equals(tgtHash)) {
      return ReconciliationVerdict.PRISTINE;
    }

    // Calculate overall record match percentage
    // Fields match if variance is <= 1% or exact match
    int matchingFields = 0;
    for (FieldVariance variance : fieldVariances) {
      // Field matches if it doesn't exceed the 1% threshold
      if (!variance.exceedsThreshold()) {
        matchingFields++;
      }
    }

    double totalFields = fieldVariances.size();
    double matchPercentage = (totalFields > 0) ? (matchingFields / totalFields) * 100.0 : 0.0;

    // Apply SLO thresholds: 95% = NOISY, <95% = MATERIAL
    if (matchPercentage >= 95.0) {
      return ReconciliationVerdict.NOISY;
    } else {
      return ReconciliationVerdict.MATERIAL;
    }
  }

  /** Result object containing reconciliation statistics and discrepancies */
  public static class ReconciliationResult {
    public final long totalSource;
    public final long totalTarget;
    public final long matchedCount;
    public final long mismatchedCount;
    public final long sourceOnlyCount;
    public final long targetOnlyCount;
    public final long pristineCount;
    public final long noisyCount;
    public final long materialCount;
    public final double matchPercentage;
    public final List<Discrepancy> discrepancies;

    public ReconciliationResult(
        long totalSource,
        long totalTarget,
        long matchedCount,
        long mismatchedCount,
        long sourceOnlyCount,
        long targetOnlyCount,
        long pristineCount,
        long noisyCount,
        long materialCount,
        double matchPercentage,
        List<Discrepancy> discrepancies) {
      this.totalSource = totalSource;
      this.totalTarget = totalTarget;
      this.matchedCount = matchedCount;
      this.mismatchedCount = mismatchedCount;
      this.sourceOnlyCount = sourceOnlyCount;
      this.targetOnlyCount = targetOnlyCount;
      this.pristineCount = pristineCount;
      this.noisyCount = noisyCount;
      this.materialCount = materialCount;
      this.matchPercentage = matchPercentage;
      this.discrepancies = discrepancies;
    }
  }

  /** Result object aggregating verdict counts and discrepancy from a single row */
  private static class ReconciliationRowResult {
    final String status;
    final long matchedCount;
    final long mismatchedCount;
    final long sourceOnlyCount;
    final long targetOnlyCount;
    final long pristineCount;
    final long noisyCount;
    final long materialCount;
    final Discrepancy discrepancy;

    private ReconciliationRowResult(
        String status,
        long matched,
        long mismatched,
        long sourceOnly,
        long targetOnly,
        long pristine,
        long noisy,
        long material,
        Discrepancy discrepancy) {
      this.status = status;
      this.matchedCount = matched;
      this.mismatchedCount = mismatched;
      this.sourceOnlyCount = sourceOnly;
      this.targetOnlyCount = targetOnly;
      this.pristineCount = pristine;
      this.noisyCount = noisy;
      this.materialCount = material;
      this.discrepancy = discrepancy;
    }
  }

  /** Process a single reconciliation row and return verdict statistics and discrepancy */
  private ReconciliationRowResult processReconciliationRow(Row row) {
    String status = row.getString(row.fieldIndex("status"));
    String srcHash = getHashFromRow(row, "src_hash");
    String tgtHash = getHashFromRow(row, "tgt_hash");

    return switch (status) {
      case "MATCH" -> new ReconciliationRowResult(status, 1, 0, 0, 0, 1, 0, 0, null);

      case "MISMATCH" -> {
        ReconciliationRecord srcRecord =
            new ReconciliationRecord(
                UUID.randomUUID(), srcHash != null ? srcHash : "", "source", null);
        ReconciliationRecord tgtRecord =
            new ReconciliationRecord(
                UUID.randomUUID(), tgtHash != null ? tgtHash : "", "target", null);

        // Analyze field variances for all hash fields in this row
        List<FieldVariance> fieldVariances = analyzeFieldVariancesForRow(row);

        // Determine verdict based on SLO (95%) and 1% field variance threshold
        ReconciliationVerdict verdict = determineVerdict(srcHash, tgtHash, fieldVariances);

        long pristine = verdict == ReconciliationVerdict.PRISTINE ? 1 : 0;
        long noisy = verdict == ReconciliationVerdict.NOISY ? 1 : 0;
        long material = verdict == ReconciliationVerdict.MATERIAL ? 1 : 0;

        Discrepancy discrepancy =
            new Discrepancy(
                "HASH_MISMATCH",
                srcRecord,
                tgtRecord,
                "Record hash differs between source and target");
        discrepancy.setVerdict(verdict);
        discrepancy.setFieldVariances(fieldVariances);

        yield new ReconciliationRowResult(
            status, 0, 1, 0, 0, pristine, noisy, material, discrepancy);
      }

      case "SOURCE_ONLY" -> {
        ReconciliationRecord srcRecord =
            new ReconciliationRecord(
                UUID.randomUUID(), srcHash != null ? srcHash : "", "source", null);
        Discrepancy discrepancy =
            new Discrepancy("SOURCE_ONLY", srcRecord, null, "Record exists only in source");
        discrepancy.setVerdict(ReconciliationVerdict.MATERIAL);
        discrepancy.setFieldVariances(new ArrayList<>());

        yield new ReconciliationRowResult(status, 0, 0, 1, 0, 0, 0, 1, discrepancy);
      }

      case "TARGET_ONLY" -> {
        ReconciliationRecord tgtRecord =
            new ReconciliationRecord(
                UUID.randomUUID(), tgtHash != null ? tgtHash : "", "target", null);
        Discrepancy discrepancy =
            new Discrepancy("TARGET_ONLY", null, tgtRecord, "Record exists only in target");
        discrepancy.setVerdict(ReconciliationVerdict.MATERIAL);
        discrepancy.setFieldVariances(new ArrayList<>());

        yield new ReconciliationRowResult(status, 0, 0, 0, 1, 0, 0, 1, discrepancy);
      }

      default -> new ReconciliationRowResult(status, 0, 0, 0, 0, 0, 0, 0, null);
    };
  }

  /** Safely extract a hash value from a row */
  private String getHashFromRow(Row row, String hashField) {
    try {
      return row.getString(row.fieldIndex(hashField));
    } catch (Exception e) {
      return null;
    }
  }

  /** Extract and analyze field variances from a reconciliation row using streams */
  private List<FieldVariance> analyzeFieldVariancesForRow(Row row) {
    // Extract field names from FieldDefinition objects
    List<String> srcHashFields =
        domainConfig.source().hashFields() != null
            ? domainConfig.source().hashFields().stream().map(fd -> fd.name()).toList()
            : null;
    List<String> tgtHashFields =
        domainConfig.target().hashFields() != null
            ? domainConfig.target().hashFields().stream().map(fd -> fd.name()).toList()
            : null;

    if (srcHashFields == null
        || srcHashFields.isEmpty()
        || tgtHashFields == null
        || tgtHashFields.isEmpty()) {
      return new ArrayList<>();
    }

    // Create a parallel list of field pairs (source field, target field)
    int size = Math.min(srcHashFields.size(), tgtHashFields.size());
    java.util.List<java.util.AbstractMap.SimpleEntry<String, String>> fieldPairs =
        new java.util.ArrayList<>();
    for (int i = 0; i < size; i++) {
      fieldPairs.add(
          new java.util.AbstractMap.SimpleEntry<>(srcHashFields.get(i), tgtHashFields.get(i)));
    }

    return fieldPairs.stream()
        .flatMap(
            fieldPair -> {
              try {
                String srcFieldName = fieldPair.getKey();
                String tgtFieldName = fieldPair.getValue();
                // Note: columns in the row are named "src_{srcFieldName}" and "tgt_{tgtFieldName}"
                Object srcValue = row.get(row.fieldIndex("src_" + srcFieldName));
                Object tgtValue = row.get(row.fieldIndex("tgt_" + tgtFieldName));

                // Determine variance type and percentage
                String varianceType;
                Double variancePercent;

                if (srcValue == null && tgtValue == null) {
                  varianceType = "EXACT_MATCH";
                  variancePercent = 0.0;
                } else if (srcValue == null || tgtValue == null) {
                  varianceType = "NULL_MISMATCH";
                  variancePercent = 100.0;
                } else if (srcValue.toString().equals(tgtValue.toString())) {
                  varianceType = "EXACT_MATCH";
                  variancePercent = 0.0;
                } else {
                  // Try numeric variance first
                  Double numericVariance = calculateNumericVariance(srcValue, tgtValue);
                  if (numericVariance != null) {
                    varianceType = "NUMERIC";
                    variancePercent = numericVariance;
                  } else {
                    varianceType = "TEXT";
                    variancePercent = null;
                  }
                }

                return java.util.stream.Stream.of(
                    new FieldVariance(
                        srcFieldName, srcValue, tgtValue, variancePercent, varianceType));
              } catch (Exception e) {
                // Field not found in row, skip
                return java.util.stream.Stream.empty();
              }
            })
        .toList();
  }

  /**
   * Execute the reconciliation using Spark SQL Returns detailed reconciliation results with
   * discrepancies
   */
  public ReconciliationResult reconcile() {
    System.out.println(
        "[SparkReconciliationEngine] Starting reconciliation for domain: " + domainConfig.name());

    try {
      // Load source and target datasets
      Dataset<Row> sourceData = readSourceData();
      Dataset<Row> targetData = readTargetData();

      // Register as Spark SQL temporary views
      sourceData.createOrReplaceTempView("source_data");
      targetData.createOrReplaceTempView("target_data");

      long totalSource = sourceData.count();
      long totalTarget = targetData.count();
      System.out.println(
          "[SparkReconciliationEngine] Source: "
              + totalSource
              + " records, Target: "
              + totalTarget
              + " records");

      // Get ID field names from FieldDefinition (ignore types for SQL - database handles type
      // coercion)
      String sourceIdField = domainConfig.source().getIdFieldName();
      String targetIdField = domainConfig.target().getIdFieldName();

      // Get hash field names from FieldDefinition list (ignore types for SQL)
      List<String> srcHashFields =
          domainConfig.source().hashFields() != null
              ? domainConfig.source().hashFields().stream().map(fd -> fd.name()).toList()
              : null;
      List<String> tgtHashFields =
          domainConfig.target().hashFields() != null
              ? domainConfig.target().hashFields().stream().map(fd -> fd.name()).toList()
              : null;

      // Build field selection for all hash fields to enable variance analysis
      StringBuilder fieldSelection = new StringBuilder();
      if (srcHashFields != null
          && !srcHashFields.isEmpty()
          && tgtHashFields != null
          && !tgtHashFields.isEmpty()) {
        // Source and target hash fields should be in corresponding order
        for (int i = 0; i < srcHashFields.size() && i < tgtHashFields.size(); i++) {
          String srcField = srcHashFields.get(i);
          String tgtField = tgtHashFields.get(i);
          fieldSelection.append(", src.").append(srcField).append(" as src_").append(srcField);
          fieldSelection.append(", tgt.").append(tgtField).append(" as tgt_").append(tgtField);
        }
      }

      // SQL-based reconciliation: full outer join on ID, compare record hashes
      String reconciliationQuery =
          String.format(
              "SELECT COALESCE(src.%s, tgt.%s) as rec_id, "
                  + "       src.record_hash as src_hash, tgt.record_hash as tgt_hash, "
                  + "       CASE WHEN src.%s IS NULL THEN 'TARGET_ONLY' "
                  + "            WHEN tgt.%s IS NULL THEN 'SOURCE_ONLY' "
                  + "            WHEN src.record_hash = tgt.record_hash THEN 'MATCH' "
                  + "            ELSE 'MISMATCH' END as status "
                  + "       %s "
                  + "FROM source_data src "
                  + "FULL OUTER JOIN target_data tgt ON src.%s = tgt.%s",
              sourceIdField,
              targetIdField,
              sourceIdField,
              targetIdField,
              fieldSelection.toString(),
              sourceIdField,
              targetIdField);

      // Execute query and collect results
      Dataset<Row> reconciliationResults = sparkSession.sql(reconciliationQuery);
      // Process reconciliation results using Java streams
      var results =
          reconciliationResults.collectAsList().stream()
              .map(this::processReconciliationRow)
              .toList();

      // Aggregate counts from result streams
      long matchedCount = results.stream().mapToLong(r -> r.matchedCount).sum();
      long mismatchedCount = results.stream().mapToLong(r -> r.mismatchedCount).sum();
      long sourceOnlyCount = results.stream().mapToLong(r -> r.sourceOnlyCount).sum();
      long targetOnlyCount = results.stream().mapToLong(r -> r.targetOnlyCount).sum();
      long pristineCount = results.stream().mapToLong(r -> r.pristineCount).sum();
      long noisyCount = results.stream().mapToLong(r -> r.noisyCount).sum();
      long materialCount = results.stream().mapToLong(r -> r.materialCount).sum();

      // Collect all non-null discrepancies
      List<Discrepancy> discrepancies =
          new ArrayList<>(
              results.stream().filter(r -> r.discrepancy != null).map(r -> r.discrepancy).toList());

      double matchPercentage =
          (totalSource + totalTarget > 0)
              ? (100.0 * matchedCount / Math.max(totalSource, totalTarget))
              : 0.0;

      System.out.println(
          "[SparkReconciliationEngine] Reconciliation complete - Matched: "
              + matchedCount
              + ", Mismatched: "
              + mismatchedCount
              + ", SourceOnly: "
              + sourceOnlyCount
              + ", TargetOnly: "
              + targetOnlyCount);

      return new ReconciliationResult(
          totalSource,
          totalTarget,
          matchedCount,
          mismatchedCount,
          sourceOnlyCount,
          targetOnlyCount,
          pristineCount,
          noisyCount,
          materialCount,
          matchPercentage,
          discrepancies);

    } catch (Exception e) {
      System.err.println("[SparkReconciliationEngine] Reconciliation failed: " + e.getMessage());
      e.printStackTrace();
      throw new RuntimeException("Spark reconciliation failed: " + e.getMessage(), e);
    }
  }
}
