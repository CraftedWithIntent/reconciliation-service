# Code Coverage Guide

This document explains how code coverage is measured and reported in the reconciliation service.

## Overview

The project uses **JaCoCo (Java Code Coverage)** to measure unit test coverage. Coverage reports are generated during the build process and uploaded to **Codecov** for tracking and reporting.

## Local Coverage Reports

### Generate Coverage Locally

```bash
# Run tests with coverage report
./gradlew test jacocoTestReport

# Open HTML coverage report (macOS)
open build/reports/jacoco/test/html/index.html

# Or Linux
xdg-open build/reports/jacoco/test/html/index.html

# Or Windows
start build/reports/jacoco/test/html/index.html
```

### Coverage Report Location

- **HTML Report**: `build/reports/jacoco/test/html/`
- **XML Report**: `build/reports/jacoco/test/jacocoTestReport.xml` (used by CI/CD)
- **CSV Report**: Can be enabled if needed

## CI/CD Integration

### GitHub Actions Pipeline

The CI/CD workflow automatically:

1. **Runs tests with JaCoCo**: `./gradlew test jacocoTestReport`
2. **Generates coverage report**: XML format
3. **Uploads to Codecov**: Via `codecov/codecov-action@v3`
4. **Creates badge**: Displayed in README

### Viewing Coverage

Coverage reports are available at:
- **GitHub Actions Artifacts**: Download coverage-report.zip from workflow run
- **Codecov Dashboard**: https://codecov.io (if connected)

## Coverage Configuration

### JaCoCo Settings

Located in `build.gradle`:

```gradle
jacocoTestReport {
    dependsOn test
    reports {
        xml.required = true       // For CI/CD reporting
        html.required = true      // For local review
        csv.required = false      // Disabled by default
    }
    afterEvaluate {
        classDirectories.setFrom(files(classDirectories.files.collect {
            fileTree(dir: it, exclude: [
                    '**/config/**',      // Configuration classes
                    '**/dto/**',         // Data transfer objects
                    '**/entity/**',      // Entity classes
                    '**/Application.class',
                    '**/Application$*.class'
            ])
        }))
    }
}
```

### Excluded Packages

The following are excluded from coverage requirements:

- **`config/**`** - Spring configuration (hard to unit test)
- **`dto/**`** - Simple data transfer objects (trivial getters/setters)
- **`entity/**`** - Entity classes (framework-managed)
- **`Application`** - Spring Boot entry point

## Target Coverage

### Recommended Minimums

- **Overall**: 75%+
- **Business Logic**: 85%+
- **Controllers**: 70%+
- **Services**: 80%+

These thresholds can be enforced via git hooks or CI workflows.

## Checkstyle Integration

Code style is enforced via **Checkstyle** alongside coverage:

```bash
# Run checkstyle
./gradlew checkstyleMain checkstyleTest

# Fix formatting with Spotless
./gradlew spotlessApply

# Combined quality check
./gradlew clean build
```

## Quality Gates

### Pre-Merge Requirements

All pull requests must:

1. ✅ Pass all unit tests (`./gradlew test`)
2. ✅ Generate coverage report (`jacocoTestReport`)
3. ✅ Pass Checkstyle validation
4. ✅ Pass Spotless formatting
5. ✅ Pass GitHub Actions CI/CD

### Codecov Settings (Optional)

You can configure Codecov to require minimum coverage:

1. Connect repo to codecov.io
2. Set branch protection rule requiring Codecov check
3. Configure coverage thresholds in codecov.yml

Example `codecov.yml`:

```yaml
codecov:
  require_ci_to_pass: yes

coverage:
  precision: 2
  round: down
  range: "70...100"

ignore:
  - "src/main/java/com/reconcile/config"
  - "src/main/java/com/reconcile/dto"
  - "src/main/java/com/reconcile/entity"
```

## Improving Coverage

### Tips for Higher Coverage

1. **Test edge cases**: Null checks, error paths, boundary conditions
2. **Test integration points**: Database interactions, API calls
3. **Mock external dependencies**: Use Mockito for isolation
4. **Test configuration**: Spring beans, property loading
5. **Use integration tests**: Tag with `@Tag("integration")`

### Common Uncovered Scenarios

- Exception handling paths
- Feature flags/conditional logic
- Deprecated code
- Platform-specific code

## Viewing Coverage Details

### HTML Report Sections

- **Coverage Summary**: Overall line and branch coverage
- **Package View**: Coverage by package
- **Class View**: Coverage by individual class
- **Source Code**: Line-by-line coverage highlighting

### Color Coding

- 🟢 **Green**: Fully covered lines
- 🔴 **Red**: Uncovered lines
- 🟡 **Yellow**: Partially covered (branch coverage)

## Badges

Add to README:

```markdown
[![codecov](https://codecov.io/gh/CraftedWithIntent/reconciliation-service/branch/main/graph/badge.svg)](https://codecov.io/gh/CraftedWithIntent/reconciliation-service)
```

## Troubleshooting

### Coverage Report Not Generated

```bash
# Check if tests ran
./gradlew test --info | grep -i jacoco

# Verify JaCoCo configuration
./gradlew jacocoTestReport --debug

# Check for test failures
./gradlew test
```

### CI/CD Upload Failing

```bash
# Verify XML report exists
ls -la build/reports/jacoco/test/jacocoTestReport.xml

# Check Codecov token (GitHub Actions should auto-detect)
echo $CODECOV_TOKEN
```

### Low Coverage From CI/CD

- Ensure tests run with same JDK version
- Check for skipped tests
- Verify no platform-specific exclusions
- Compare local vs. CI coverage reports

## Related Documentation

- [CONTRIBUTING.md](CONTRIBUTING.md) - Testing requirements
- [build.gradle](build.gradle) - Build configuration
- [.github/workflows/ci-cd.yml](.github/workflows/ci-cd.yml) - CI/CD pipeline
- [JaCoCo Docs](https://www.jacoco.org/jacoco/)
- [Codecov Docs](https://docs.codecov.io/)
