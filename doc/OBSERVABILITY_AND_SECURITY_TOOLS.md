# Observability & Security Tools Integration

This project includes industry-standard tools for code quality, security scanning, and observability.

---

## 1. Spotless - Code Formatting & Consistency

### Overview

Spotless is a code formatter that ensures consistent code style across the entire project using Google Java Format.

### Configuration

**Build System Integration:**
```gradle
spotless {
    java {
        target 'src/**/*.java'
        googleJavaFormat('1.17.0')
        trimTrailingWhitespace()
        endWithNewline()
    }
    yaml { /* YAML formatting */ }
    json  { /* JSON formatting */ }
}
```

### Usage

#### Check code formatting
```bash
./gradlew spotlessCheck
```

#### Auto-format code
```bash
./gradlew spotlessApply
```

#### Format all code types
```bash
./gradlew formatCode
```

### Features

- ✅ **Google Java Format** - Industry-standard Java formatting
- ✅ **YAML Format** - Consistent configuration files
- ✅ **JSON Format** - Consistent JSON files
- ✅ **Whitespace Management** - Removes trailing spaces
- ✅ **Line Endings** - Enforces Unix line endings (LF)

### CI/CD Integration

Add to your CI/CD pipeline to fail builds with formatting issues:

```yaml
# GitHub Actions example
- name: Check code formatting
  run: ./gradlew spotlessCheck
```

---

## 2. Snyk - Vulnerability Scanning

### Overview

Snyk scans your dependencies for known security vulnerabilities and provides remediation guidance.

### Setup

#### 1. Install Snyk CLI (Optional)
```bash
npm install -g snyk
snyk auth
```

#### 2. Configure Gradle Token

Set your Snyk API token as an environment variable:

```bash
export SNYK_TOKEN=your_snyk_api_token
```

**Get your token:**
1. Go to https://app.snyk.io
2. Create a free account
3. Navigate to Settings → API Token
4. Copy your token

### Usage

#### Scan for vulnerabilities
```bash
./gradlew snykTest
```

#### Security check (includes Snyk)
```bash
./gradlew securityCheck
```

#### Full security analysis (Snyk CLI)
```bash
snyk test
```

#### Generate SBOM (Software Bill of Materials)
```bash
snyk sbom --format=cyclonedx > sbom.xml
```

### Vulnerability Parameters

Snyk reports vulnerabilities with:
- **Severity**: Critical, High, Medium, Low
- **CVSS Score**: 0-10 scale
- **Fix Available**: Version or patch availability
- **Exploit Maturity**: Proof of Concept, Functional, Unproven
- **Remediation Path**: Upgrade recommendations

### Handling Results

**If vulnerabilities found:**

1. **Review findings** at: https://app.snyk.io
2. **Prioritize by severity**
3. **Apply recommended upgrades**
4. **Re-test after fixes**

**Example output:**
```
✗ High severity vulnerability found in org.apache.spark:spark-sql_2.13:3.5.0
  Issue: Unsafe serialization in Spark
  Fix: Upgrade to 3.5.1 or later
  References: CVE-2024-XXXXX
```

### Ignoring False Positives

Create `.snyk` file:
```json
{
  "version": "1.0.0",
  "ignore": {
    "CVE-2024-XXXXX": {
      "reason": "Package updated in downstream",
      "expires": "2025-01-01T00:00:00.000Z"
    }
  }
}
```

---

## 3. OpenTelemetry - Distributed Tracing & Observability

### Overview

OpenTelemetry provides:
- **Distributed Tracing** - Track requests across services
- **Metrics Collection** - Monitor performance metrics
- **Log Correlation** - Link logs to traces
- **Observability** - Unified visibility into system behavior

### Architecture

```
┌──────────────────────────────────────┐
│    Spring Boot Application            │
│  (OpenTelemetry Instrumentation)      │
├──────────────────────────────────────┤
│  Tracer    Spans    Metrics    Logs   │
├──────────────────────────────────────┤
│         Batch Span Processor          │ ← Collects spans
├──────────────────────────────────────┤
│  Jaeger Exporter   Prometheus Export  │
└──────────────────────────────────────┘
        ↓                    ↓
    Jaeger UI          Prometheus
  (Traces View)      (Metrics View)
```

### Configuration

**Properties file:** `src/main/resources/opentelemetry.properties`

Key settings:
```properties
# Service identification
otel.service.name=reconciliation-service
otel.service.version=1.0.0

# Jaeger exporter (tracing)
otel.exporter.jaeger.endpoint=http://localhost:14268/api/traces

# Prometheus metrics
otel.exporter.prometheus.port=8888

# Sampling rate (1.0 = 100%, 0.1 = 10%)
otel.traces.sampler=parentbased_always_on
otel.traces.sampler.arg=1.0
```

### Deployment with Docker Compose

**Setup Jaeger + Prometheus:**

```yaml
version: '3.8'
services:
  jaeger:
    image: jaegertracing/all-in-one:latest
    ports:
      - "6831:6831/udp"  # Agent receiver
      - "14268:14268"    # Collector endpoint
      - "16686:16686"    # UI: http://localhost:16686
    environment:
      - COLLECTOR_ENV=development

  prometheus:
    image: prom/prometheus:latest
    ports:
      - "9090:9090"  # UI: http://localhost:9090
    volumes:
      - ./prometheus.yml:/etc/prometheus/prometheus.yml
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'

  reconciliation-app:
    build: .
    ports:
      - "8080:8080"
      - "8888:8888"  # Prometheus metrics export
    environment:
      - OTEL_EXPORTER_JAEGER_ENDPOINT=http://jaeger:14268/api/traces
      - OTEL_EXPORTER_PROMETHEUS_PORT=8888
      - OTEL_DEPLOYMENT_ENV=docker
    depends_on:
      - jaeger
      - prometheus
```

**Prometheus configuration (`prometheus.yml`):**

```yaml
global:
  scrape_interval: 15s

scrape_configs:
  - job_name: 'reconciliation-service'
    static_configs:
      - targets: ['localhost:8888']
```

### Using Tracer in Code

Inject and use the tracer:

```java
@Service
public class ReconciliationService {
    
    private final Tracer tracer;
    
    public ReconciliationService(Tracer tracer) {
        this.tracer = tracer;
    }
    
    public void reconcile(String domain) {
        try (Scope scope = tracer.spanBuilder("reconcile_domain")
                .setAttribute("domain", domain)
                .startSpan()
                .makeCurrent()) {
            // Span automatically created and exported
            performReconciliation(domain);
        }
    }
}
```

### Accessing Dashboards

After starting with docker-compose:

- **Jaeger UI (Traces):** http://localhost:16686
  - Search by service, operation, tags
  - View latency, error rates, dependencies
  
- **Prometheus UI (Metrics):** http://localhost:9090
  - Query custom metrics
  - Create alerts
  - Visualize performance

### Sampling Strategies

**Development (100% sampling):**
```properties
otel.traces.sampler=always_on
```

**Production (10% sampling):**
```properties
otel.traces.sampler=traceidratio
otel.traces.sampler.arg=0.1
```

**Parent-based (inherit from parent):**
```properties
otel.traces.sampler=parentbased_always_on
otel.traces.sampler.arg=0.5  # Fallback for root spans
```

### Metrics Exported

**JVM Metrics:**
- Memory usage (heap, non-heap)
- Thread count and states
- Garbage collection stats

**Database Metrics:**
- Connection pool status
- Query latencies
- Connection errors

**Custom Application Metrics:**
- Reconciliation records processed
- Hash mismatches found
- Processing duration

---

## Integration Workflow

### Local Development

```bash
# 1. Format code before committing
./gradlew formatCode

# 2. Check for vulnerabilities
./gradlew snykTest

# 3. Start observability stack
docker-compose -f docker-compose-observability.yml up

# 4. Run application
./gradlew bootRun

# 5. Access dashboards
# Jaeger: http://localhost:16686
# Prometheus: http://localhost:9090
```

### CI/CD Pipeline

```yaml
# GitHub Actions example
jobs:
  code-quality:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Check formatting
        run: ./gradlew spotlessCheck
      
      - name: Scan vulnerabilities
        env:
          SNYK_TOKEN: ${{ secrets.SNYK_TOKEN }}
        run: ./gradlew snykTest
      
      - name: Build and test
        run: ./gradlew build
```

---

## Best Practices

### Spotless
- ✅ Run `spotlessApply` before committing
- ✅ Enable pre-commit hook for automatic formatting
- ✅ Include in CI/CD to fail on formatting issues

### Snyk
- ✅ Scan regularly (daily or per-commit)
- ✅ Monitor vulnerabilities in dashboard
- ✅ Prioritize critical and high-severity issues
- ✅ Review dependency upgrades carefully
- ✅ Document ignored vulnerabilities with expiration

### OpenTelemetry
- ✅ Use meaningful span names for tracing
- ✅ Add attributes for debugging (domain, recordId, etc.)
- ✅ Adjust sampling based on environment
- ✅ Set resource attributes for better identification
- ✅ Monitor P50, P95, P99 latencies
- ✅ Alert on error rate increases

---

## References

- [Spotless Documentation](https://github.com/diffplug/spotless)
- [Snyk Documentation](https://docs.snyk.io/)
- [OpenTelemetry Java](https://opentelemetry.io/docs/instrumentation/java/)
- [Jaeger Tracing](https://www.jaegertracing.io/)
- [Prometheus](https://prometheus.io/)

---

## Troubleshooting

### Spotless not formatting

```bash
# Full reformat
./gradlew spotlessApply --rerun-tasks

# Check formatting issues
./gradlew spotlessCheck -i
```

### Snyk failing with authentication

```bash
# Verify token is set
echo $SNYK_TOKEN

# Re-authenticate
snyk auth
```

### OpenTelemetry not exporting traces

```bash
# Verify Jaeger is running
curl http://localhost:14268/api/traces

# Check application logs for export errors
./gradlew bootRun -i | grep -i "telemetry\|jaeger"
```

