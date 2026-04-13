package com.reconcile.config;

import org.springframework.context.annotation.Configuration;

/**
 * Application configuration for monitoring and observability. Integrates Spring Boot Actuator for
 * health checks and metrics.
 */
@Configuration
public class ActuatorConfig {

  // Configuration is applied via application.yml
  // See: management.endpoints.web.exposure
}
