package com.reconcile.config;

import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.exporter.jaeger.JaegerGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenTelemetry Configuration for Distributed Tracing
 *
 * <p>Configures OpenTelemetry for: - Distributed tracing via Jaeger - Span creation and context
 * propagation - Resource attributes for better observability
 *
 * @see <a href="https://opentelemetry.io/">OpenTelemetry</a>
 * @see <a href="https://www.jaegertracing.io/">Jaeger Tracing</a>
 */
@Configuration
public class OpenTelemetryConfig {

  private static final String SERVICE_NAME = "reconciliation-service";
  private static final String SERVICE_VERSION = "1.0.0";

  @Value("${otel.exporter.jaeger.endpoint:localhost:14250}")
  private String jaegerEndpoint;

  /**
   * Creates and configures the Jaeger gRPC span exporter
   *
   * @return JaegerGrpcSpanExporter configured with endpoint
   */
  @Bean
  public JaegerGrpcSpanExporter jaegerExporter() {
    return JaegerGrpcSpanExporter.builder().setEndpoint(jaegerEndpoint).build();
  }

  /**
   * Creates the SDK Tracer Provider with Jaeger exporter
   *
   * @return SdkTracerProvider with span processors configured
   */
  @Bean
  public SdkTracerProvider tracerProvider(JaegerGrpcSpanExporter jaegerExporter) {
    Resource resource =
        Resource.getDefault()
            .merge(
                Resource.builder()
                    .put("service.name", SERVICE_NAME)
                    .put("service.version", SERVICE_VERSION)
                    .put("deployment.environment", getEnvironment())
                    .build());

    return SdkTracerProvider.builder()
        .addSpanProcessor(SimpleSpanProcessor.create(jaegerExporter))
        .setResource(resource)
        .build();
  }

  /**
   * Creates OpenTelemetry SDK instance
   *
   * @return Configured OpenTelemetrySdk
   */
  @Bean
  public OpenTelemetrySdk openTelemetry(SdkTracerProvider tracerProvider) {
    OpenTelemetrySdk sdk = OpenTelemetrySdk.builder().setTracerProvider(tracerProvider).build();

    // For graceful shutdown
    Runtime.getRuntime()
        .addShutdownHook(
            new Thread(
                () -> {
                  tracerProvider.close();
                }));

    return sdk;
  }

  /**
   * Creates a Tracer bean for dependency injection
   *
   * @return Tracer for creating spans
   */
  @Bean
  public Tracer tracer(OpenTelemetrySdk openTelemetry) {
    return openTelemetry.getTracer(SERVICE_NAME);
  }

  /** Gets deployment environment from environment variable or defaults to 'development' */
  private String getEnvironment() {
    return System.getenv().getOrDefault("OTEL_DEPLOYMENT_ENV", "development");
  }
}
