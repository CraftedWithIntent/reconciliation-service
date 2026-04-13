# Environment Configuration Guide

This guide explains how to configure the reconciliation service for different environments: local, development (dev), QA, and production (prod).

## Overview

Spring Boot uses **profile-specific configuration files** to manage environment-specific settings:

- `application.yml` - Default/shared configuration (base settings)
- `application-{profile}.yml` - Environment-specific overrides
- `domains-{profile}.yml` - Environment-specific domains (reference files)

## Selecting an Environment Profile

### Method 1: Command Line (JAR)
```bash
java -jar reconciliation-service.jar --spring.profiles.active=dev
```

### Method 2: Command Line (Gradle)
```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

### Method 3: Environment Variable
```bash
export SPRING_PROFILES_ACTIVE=prod
java -jar reconciliation-service.jar
```

### Method 4: application.yml (set default)
```yaml
spring:
  profiles:
    active: dev
```

## Environment Profiles

### 1. Local (`application-local.yml`)
**Purpose:** Development machine, easy startup, fast iteration  
**Characteristics:**
- Database: `localhost:5432` with default credentials
- Port: `8080`
- Logging: DEBUG level (verbose)
- Connection pools: Small (5 max)
- Use Case: `./gradlew bootRun --args='--spring.profiles.active=local'`

**Environment Setup:**
```bash
# Create local PostgreSQL (Docker example):
docker run --name pg-local -e POSTGRES_PASSWORD=postgres -p 5432:5432 postgres:15

# Start service:
./gradlew bootRun --args='--spring.profiles.active=local'
```

### 2. Development (`application-dev.yml`)
**Purpose:** Shared development server, team collaboration  
**Characteristics:**
- Database: `dev-source-db` / `dev-target-db` (via environment variables)
- Port: `8080`
- Logging: INFO level
- Connection pools: Medium (10 max)
- Credentials: From `SOURCE_DB_USER`, `SOURCE_DB_PASSWORD` env vars

**Environment Setup:**
```bash
export SOURCE_DB_HOST=dev-source-db
export SOURCE_DB_USER=dev_user
export SOURCE_DB_PASSWORD=secure_password_here
export TARGET_DB_HOST=dev-target-db
export TARGET_DB_USER=dev_user
export TARGET_DB_PASSWORD=secure_password_here

java -jar reconciliation-service.jar --spring.profiles.active=dev
```

### 3. QA (`application-qa.yml`)
**Purpose:** Quality assurance environment, strict testing  
**Characteristics:**
- Database: `qa-source-db` / `qa-target-db`
- Port: `8080`
- Logging: WARN level
- Connection pools: Large (15 max)
- Thresholds: Standard (95% SLO, 1% variance)
- Use Case: Integration testing, pre-production validation

**Environment Setup:**
```bash
export SOURCE_DB_HOST=qa-source-db
export SOURCE_DB_USER=qa_user
export SOURCE_DB_PASSWORD=secure_qa_password
export TARGET_DB_HOST=qa-target-db
export TARGET_DB_USER=qa_user
export TARGET_DB_PASSWORD=secure_qa_password

java -jar reconciliation-service.jar --spring.profiles.active=qa
```

### 4. Production (`application-prod.yml`)
**Purpose:** Live environment, maximum security and performance  
**Characteristics:**
- Database: Full URLs from environment variables (no defaults)
- Port: Configurable via `SERVER_PORT`
- Logging: ERROR level only
- Connection pools: Large (20 max) with leak detection
- Credentials: **All from env vars** (required, no hardcoded defaults)
- Thresholds: Strictest (99% SLO, 0.5% variance)

**Security Requirements:**
```bash
# MANDATORY environment variables - all credentials and URLs
export SOURCE_DB_URL=jdbc:postgresql://prod-source.example.com:5432/reconciliation
export SOURCE_DB_USER=prod_user
export SOURCE_DB_PASSWORD=VERY_SECURE_PASSWORD
export TARGET_DB_URL=jdbc:postgresql://prod-target.example.com:5432/reconciliation
export TARGET_DB_USER=prod_user
export TARGET_DB_PASSWORD=VERY_SECURE_PASSWORD
export SERVER_PORT=8080

# Optional per-domain database overrides
export VENDOR_SOURCE_DB_URL=jdbc:postgresql://vendor-prod-source:5432/vendor
export VENDOR_SOURCE_DB_USER=vendor_user
export VENDOR_SOURCE_DB_PASS=vendor_password
export VENDOR_TARGET_DB_URL=jdbc:postgresql://vendor-prod-target:5432/vendor
export VENDOR_TARGET_DB_USER=vendor_user
export VENDOR_TARGET_DB_PASS=vendor_password

java -jar reconciliation-service.jar --spring.profiles.active=prod
```

## Domain Configuration by Environment

### Using Environment-Specific Domains

The `domains-{profile}.yml` files are **reference templates**. To use them:

**Option 1: Manual Merge (Recommended for Production)**
```bash
# Edit application-{profile}.yml to include domains:
# Copy the reconciliation.domains section from domains-{profile}.yml
# into application-{profile}.yml under spring.application
```

**Option 2: Spring Cloud Config (Enterprise)**
Use Spring Cloud Config Server to dynamically load different domains per environment.

**Option 3: Custom Property Source**
Implement a `@Configuration` class to load domains from classpath based on profile.

### Threshold Comparison by Environment

| Environment | SLO Target | Variance Threshold | Purpose |
|------------|-----------|-------------------|---------|
| **Local** | 95.0% | 1.0% | Development/testing |
| **Dev** | 93.0% | 1.5% | Flexibility during development |
| **QA** | 95.0% | 1.0% | Standard reconciliation |
| **Prod** | 99.0% | 0.5% | Strict reconciliation |

Higher thresholds = stricter verdicts (more discrepancies flagged as MATERIAL)

## Database Configuration Hierarchy

The service checks datasources in this order:

1. **Per-domain datasource** (if configured in domains/domain config)
   ```yaml
   source:
     url: jdbc:postgresql://...
     username: user
     password: pass
   ```

2. **Global application datasource** (from `application-{profile}.yml`)
   ```yaml
   spring.datasource.source.url: jdbc:postgresql://...
   ```

3. **Environment variables** (if value is not hardcoded)
   ```bash
   SOURCE_DB_URL=jdbc:...
   ```

## Docker/Kubernetes Deployment

### Docker Example
```dockerfile
FROM openjdk:17-slim
ARG PROFILE=prod
ENV SPRING_PROFILES_ACTIVE=$PROFILE
COPY reconciliation-service.jar /app/
ENTRYPOINT ["java", "-jar", "/app/reconciliation-service.jar"]
```

Build and run:
```bash
docker build -t reconciliation-service:prod --build-arg PROFILE=prod .
docker run -e SOURCE_DB_URL=... -e SOURCE_DB_USER=... reconciliation-service:prod
```

### Kubernetes ConfigMap Example
```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: reconciliation-prod-config
data:
  application.yml: |
    spring:
      profiles:
        active: prod
      datasource:
        source:
          url: jdbc:postgresql://prod-db-primary:5432/source
          username: prod_user
        target:
          url: jdbc:postgresql://prod-db-secondary:5432/target
          username: prod_user
---
apiVersion: v1
kind: Secret
metadata:
  name: reconciliation-prod-secrets
type: Opaque
stringData:
  source-db-password: ${SOURCE_DB_PASSWORD}
  target-db-password: ${TARGET_DB_PASSWORD}
```

## Troubleshooting

### Check Which Profile Is Active
```bash
curl http://localhost:8080/actuator/env | grep spring.profiles
```

### Verify Configuration Loaded
```bash
# View all Spring properties
curl http://localhost:8080/actuator/configprops

# Check datasource configuration
curl http://localhost:8080/actuator/env | grep -i datasource
```

### Profile Not Taking Effect
1. Verify no typos: `--spring.profiles.active=dev` (not `spring.profiles.active`)
2. Check order: profile-specific files must exist
3. Environment variables: May override YAML config

## Best Practices

✅ **DO:**
- Use environment variables for all sensitive data (passwords, URLs)
- Keep `application.yml` generic with only defaults
- Use profile-specific files for environment differences
- Test locally with `local` profile before deploying
- Use per-domain datasource overrides in production for maximum flexibility

❌ **DON'T:**
- Commit passwords to git (even in application files)
- Use production URLs in non-prod files
- Share environment profiles across teams without documentation
- Forget to set `SPRING_PROFILES_ACTIVE` in Kubernetes/Docker

## Quick Start Reference

```bash
# Local development
./gradlew bootRun --args='--spring.profiles.active=local'

# Dev server
export SOURCE_DB_HOST=dev-db PORT=8080
java -jar app.jar --spring.profiles.active=dev

# QA/Staging
java -jar app.jar --spring.profiles.active=qa

# Production (with all required credentials)
export SOURCE_DB_URL=... SOURCE_DB_USER=... SOURCE_DB_PASSWORD=...
export TARGET_DB_URL=... TARGET_DB_USER=... TARGET_DB_PASSWORD=...
java -jar app.jar --spring.profiles.active=prod
```
