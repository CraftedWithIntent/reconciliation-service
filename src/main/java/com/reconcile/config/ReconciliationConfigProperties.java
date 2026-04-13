package com.reconcile.config;

import com.reconcile.validator.DomainConfigValidator;
import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Spring configuration properties for reconciliation domains Loads configuration from domains.yml
 * under 'reconciliation.domains.*' Spring Boot automatically includes all YAML files from resources
 * folder
 */
@ConfigurationProperties(prefix = "reconciliation")
public class ReconciliationConfigProperties {

  private Map<String, DomainConfig> domains = new HashMap<>();

  @PostConstruct
  public void init() {
    System.out.println(
        "[ReconciliationConfigProperties] Loaded "
            + domains.size()
            + " domain(s): "
            + domains.keySet());

    // Validate all domain configurations
    validateAllDomains();
  }

  /**
   * Validates all loaded domain configurations Throws IllegalArgumentException if any domain
   * configuration is invalid
   */
  private void validateAllDomains() {
    List<String> allErrors = new ArrayList<>();

    for (Map.Entry<String, DomainConfig> entry : domains.entrySet()) {
      String domainName = entry.getKey();
      DomainConfig config = entry.getValue();

      List<String> errors = DomainConfigValidator.validate(config);
      for (String error : errors) {
        allErrors.add("Domain '" + domainName + "': " + error);
      }
    }

    if (!allErrors.isEmpty()) {
      throw new IllegalArgumentException(
          "Invalid domain configuration(s):\n  - " + String.join("\n  - ", allErrors));
    }

    System.out.println(
        "[ReconciliationConfigProperties] All "
            + domains.size()
            + " domain(s) validated successfully");
  }

  public Map<String, DomainConfig> getDomains() {
    return domains;
  }

  public void setDomains(Map<String, DomainConfig> domains) {
    this.domains = domains;
  }

  /** Get a specific domain configuration by name */
  public DomainConfig getDomain(String domainName) {
    return domains.get(domainName);
  }

  /** Check if a domain is configured */
  public boolean hasDomain(String domainName) {
    return domains.containsKey(domainName);
  }

  @Override
  public String toString() {
    return "ReconciliationConfigProperties{" + "domains=" + domains.keySet() + '}';
  }
}
