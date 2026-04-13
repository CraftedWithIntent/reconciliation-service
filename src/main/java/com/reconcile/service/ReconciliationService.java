package com.reconcile.service;

import com.reconcile.config.DomainConfig;
import com.reconcile.config.ReconciliationConfigProperties;
import com.reconcile.dto.ReconciliationResult;
import com.reconcile.engine.SparkReconciliationEngine;
import com.reconcile.model.Reconciliation;
import com.reconcile.model.ReconciliationMapper;
import org.apache.spark.sql.SparkSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Generic Spark-based reconciliation service Supports multiple domains via configuration, works
 * with heterogeneous data sources (PostgreSQL, Oracle, etc.) Uses Spark SQL with MD5-based row
 * hashing for efficient reconciliation
 *
 * <p>Layered architecture: - Converts engine results to domain models (Reconciliation) - Converts
 * domain models to DTOs for API responses - Maintains clean separation between internal logic and
 * external contracts
 */
@Service
public class ReconciliationService {

  private final ReconciliationConfigProperties configProperties;
  private SparkSession sparkSession;
  private final String sourceUrl;
  private final String sourceUsername;
  private final String sourcePassword;
  private final String targetUrl;
  private final String targetUsername;
  private final String targetPassword;
  private final Object sparkLock = new Object();

  @Autowired
  public ReconciliationService(
      ReconciliationConfigProperties configProperties,
      @Value("${spring.datasource.source.url:jdbc:postgresql://source-db:5432/source_db}")
          String sourceUrl,
      @Value("${spring.datasource.source.username:postgres}") String sourceUsername,
      @Value("${spring.datasource.source.password:postgres}") String sourcePassword,
      @Value("${spring.datasource.target.url:jdbc:postgresql://target-db:5433/target_db}")
          String targetUrl,
      @Value("${spring.datasource.target.username:postgres}") String targetUsername,
      @Value("${spring.datasource.target.password:postgres}") String targetPassword) {

    this.configProperties = configProperties;
    this.sourceUrl = sourceUrl;
    this.sourceUsername = sourceUsername;
    this.sourcePassword = sourcePassword;
    this.targetUrl = targetUrl;
    this.targetUsername = targetUsername;
    this.targetPassword = targetPassword;

    System.out.println(
        "[ReconciliationService] Initialized - will create Spark session on first use");
    System.out.println(
        "[ReconciliationService] Configured domains: " + configProperties.getDomains().keySet());
  }

  /** Lazy-initialize Spark session on first use with servlet compatibility configuration */
  private synchronized SparkSession getSparkSession() {
    if (sparkSession == null) {
      synchronized (sparkLock) {
        if (sparkSession == null) {
          System.out.println("[ReconciliationService] Creating Spark session...");
          sparkSession =
              SparkSession.builder()
                  .appName("ReconciliationEngine")
                  .master("local[*]")
                  .config("spark.sql.shuffle.partitions", 4)
                  .config(
                      "spark.driver.extraJavaOptions",
                      "--add-opens=java.base/sun.nio.ch=ALL-UNNAMED")
                  .config("spark.ui.enabled", "false")
                  .config("spark.metrics.enabled", "true")
                  .config(
                      "spark.metrics.conf.*.sink.servlet.class",
                      "org.apache.spark.metrics.sink.MetricsServlet") // Allow servlet
                  .config("spark.metrics.staticSources.enabled", "false")
                  .config("spark.metrics.appStatusSource.enabled", "false")
                  .getOrCreate();
          System.out.println("[ReconciliationService] Spark session created successfully");
        }
      }
    }
    return sparkSession;
  }

  /**
   * Perform reconciliation for a specific domain using Spark SQL with MD5-based row hashing
   * Supports heterogeneous data sources (PostgreSQL, Oracle, MySQL, etc.)
   *
   * @param domainName name of the domain to reconcile (e.g., "vendor-invoices")
   * @return ReconciliationResult with counts and discrepancies
   */
  public ReconciliationResult reconcile(String domainName) {
    System.out.println(
        "[ReconciliationService] Starting reconciliation request for domain: " + domainName);

    if (!configProperties.hasDomain(domainName)) {
      throw new IllegalArgumentException(
          "Domain '"
              + domainName
              + "' not configured. Available domains: "
              + configProperties.getDomains().keySet());
    }

    try {
      DomainConfig domainConfig = configProperties.getDomain(domainName);
      SparkSession ss = getSparkSession();

      // Create and execute engine with domain configuration and domain-specific thresholds
      SparkReconciliationEngine engine =
          new SparkReconciliationEngine(
              ss,
              domainConfig,
              sourceUrl,
              sourceUsername,
              sourcePassword,
              targetUrl,
              targetUsername,
              targetPassword,
              domainConfig.sloTarget(),
              domainConfig.varianceThreshold());

      SparkReconciliationEngine.ReconciliationResult engineResult = engine.reconcile();

      // Convert engine result to domain Reconciliation model
      // This provides a clean business logic layer between engine and API
      Reconciliation reconciliation =
          ReconciliationMapper.engineResultToDomain(
              domainName,
              engineResult.totalSource,
              engineResult.totalTarget,
              engineResult.matchedCount,
              engineResult.mismatchedCount,
              engineResult.sourceOnlyCount,
              engineResult.targetOnlyCount,
              engineResult.pristineCount,
              engineResult.noisyCount,
              engineResult.materialCount,
              engineResult.discrepancies);

      // Convert domain model to DTO for API response
      ReconciliationResult resultDTO = ReconciliationMapper.toDTO(reconciliation);

      return resultDTO;

    } catch (Exception e) {
      System.err.println(
          "[ReconciliationService] ERROR during reconciliation for domain '"
              + domainName
              + "': "
              + e.getMessage());
      e.printStackTrace();
      throw new RuntimeException(
          "Reconciliation failed for domain '" + domainName + "': " + e.getMessage(), e);
    }
  }

  /** Perform reconciliation using default domain (for backwards compatibility) */
  public ReconciliationResult reconcile() {
    String defaultDomain = "vendor-invoices";
    return reconcile(defaultDomain);
  }
}
