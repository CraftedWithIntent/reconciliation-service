# Continuous Integration & Deployment (CI/CD) Guide

## Overview

This project uses **GitHub Actions** for automated testing and quality assurance. Every push to `main` or `develop` branches, and every pull request, automatically triggers a comprehensive CI/CD pipeline.

## GitHub Actions Workflow

### Pipeline Stages

**1. Build and Test**
- Checks out code
- Sets up JDK 17
- Compiles project with `./gradlew clean build -x test`
- Runs all 219 unit tests with `./gradlew test`
- Publishes test results as workflow artifacts
- Generates test and coverage reports

**2. Code Quality**
- Runs Spotless formatting checks
- Runs Checkstyle linting
- Validates code formatting standards

**3. Notifications**
- Alerts on pipeline failures
- Provides GitHub workflow run links for debugging

### Build Status Badge

Copy this badge to your README:

```markdown
[![CI/CD Pipeline](https://github.com/CraftedWithIntent/reconciliation-service/actions/workflows/ci-cd.yml/badge.svg)](https://github.com/CraftedWithIntent/reconciliation-service/actions/workflows/ci-cd.yml)
```

Displays as: [![CI/CD Pipeline](https://github.com/CraftedWithIntent/reconciliation-service/actions/workflows/ci-cd.yml/badge.svg)](https://github.com/CraftedWithIntent/reconciliation-service/actions/workflows/ci-cd.yml)

## Monitoring CI/CD

### View Workflow Runs

1. Go to repository: https://github.com/CraftedWithIntent/reconciliation-service
2. Click **Actions** tab in top navigation
3. See all workflow runs with status:
   - ✅ **Passed** (green checkmark)
   - ❌ **Failed** (red X)
   - ⏳ **In Progress** (yellow circle)

### Check Latest Commit Status

On the main branch, commits show status indicator:
- Green checkmark = All CI checks passed
- Red X = CI pipeline failed
- Yellow dot = CI in progress

### View Test Results

After a workflow completes:

1. Click the workflow run
2. Click **Build and Test** job
3. Scroll to "Publish test results" section:
   - See total tests run
   - See pass/fail counts
   - Click "Test Results" to see detailed report

### Download Artifacts

Workflow runs save artifacts for 90 days:

1. Click workflow run
2. Scroll to bottom "Artifacts" section
3. Download:
   - `test-results` - JUnit XML test reports
   - `coverage-report` - Jacoco/Gradle coverage reports

### View Logs

To debug a failed workflow:

1. Click the failed workflow run
2. Click the failed job (e.g., "Build and Test")
3. Expand step to see full output
4. Look for error messages and stack traces

## Continuous Integration Triggers

| Event | Branch | Action |
|-------|--------|--------|
| Push | `main` | Run full pipeline |
| Push | `develop` | Run full pipeline |
| Pull Request | → `main` | Run full pipeline |
| Pull Request | → `develop` | Run full pipeline |
| Manual trigger | Any | Optional (future) |

## Local CI Testing

### Run Tests Locally Before Push

```bash
# Run all tests
./gradlew test

# Run specific test class
./gradlew test --tests ReconciliationServiceTest

# Run with coverage
./gradlew test jacocoTestReport

# Run code quality checks
./gradlew spotlessCheck
./gradlew checkstyleMain
```

### Fix Formatting Before Commit

If Spotless fails in CI, fix locally:

```bash
./gradlew spotlessApply
git add .
git commit "Apply formatting fixes"
git push
```

## Workflow Configuration

The CI/CD workflow is defined in: `.github/workflows/ci-cd.yml`

### Key Configuration

```yaml
on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main, develop]
```

Triggers on:
- Push to `main` or `develop`
- Pull requests targeting `main` or `develop`

### Add New CI Checks

To add additional checks to the pipeline, edit `.github/workflows/ci-cd.yml`:

```yaml
- name: Run new check
  run: ./gradlew myNewCheck
```

## Troubleshooting CI Failures

### Common Failure Scenarios

**Compilation Error**
- Check Java version compatibility
- Run locally: `./gradlew clean build -x test`
- View logs in workflow run

**Test Failure**
- Identify failed test in "Test Results" section
- Run locally to reproduce: `./gradlew test --tests FailedTestName`
- Fix and commit

**Spotless Formatting Error**
- Run locally: `./gradlew spotlessApply`
- Commit the formatted code
- Re-push

**Timeout/Resource Error**
- Workflow has 360-minute (6 hour) timeout
- Check if Gradle cache is working
- Review large file uploads

### Get Help

- Check workflow run details for specific error
- Review GitHub Actions logs
- Check recent commits for conflicts
- Verify branch protection rules aren't too strict

## Branch Protection Rules (Recommended)

Configure GitHub to require CI passes before merging:

1. Go to repository **Settings**
2. Navigate to **Branches**
3. Add rule for `main` branch:
   - ✅ Require status checks to pass
   - ✅ Require branches to be up to date
   - Select CI checks: `build-and-test`, `code-quality`

This prevents merging when:
- Tests fail
- Code quality checks fail
- Branch is out of date with main

## Performance Optimization

### Gradle Build Cache

The workflow uses `actions/setup-java@v4` with `cache: gradle`:

- Caches gradle dependencies
- Speeds up builds by ~40%
- Automatically managed by GitHub

To clear cache if needed:
1. Go to **Settings** → **Actions** → **General**
2. Scroll to "Caches"
3. Click delete button for gradle cache

### Parallel Jobs

The workflow runs some jobs in parallel:
- `build-and-test` runs first
- `code-quality` runs after (depends on build-and-test success)
- `notify-on-failure` runs on any failure

This keeps total pipeline time reasonable.

## Integration with Pull Requests

### PR Checks

When you create a PR, GitHub automatically:
1. Runs CI pipeline
2. Shows status at bottom of PR
3. Blocks merge if checks fail (if rules configured)
4. Updates status as pipeline progresses

### Required Checks

If branch protection requires CI checks:
1. PR cannot be merged until checks pass
2. Dismissing reviews requires re-running checks
3. Force push disables status checks

## Email Notifications

GitHub can email you about CI status:

1. Go to your GitHub **Settings** → **Notifications**
2. Under "Subscriptions":
   - Check "Automatically watch repositories"
   - Email on "Workflow runs"
3. You'll receive emails on:
   - Workflow failures
   - Action required (manual review)

## Advanced: Custom Workflows

To add more CI checks:

1. Create new file in `.github/workflows/`
2. Define trigger events
3. Add jobs with steps
4. Commit and push

Example: Add nightly performance tests
```yaml
name: Nightly Performance Tests
on:
  schedule:
    - cron: '0 2 * * *'  # 2 AM daily
jobs:
  performance-test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Run performance suite
        run: ./gradlew performanceTest
```

## Status Page

View real-time status: https://www.githubstatus.com

If GitHub Actions is unavailable, workflows won't run. Check status page before reporting issues.

## Related Documentation

- [GitHub Actions Documentation](https://docs.github.com/en/actions)
- [GitHub Actions for Java](https://docs.github.com/en/actions/guides/building-and-testing-java-with-gradle)
- [Gradle Build Caching](https://docs.gradle.org/current/userguide/build_cache.html)
- Project: [ENVIRONMENT_CONFIGURATION.md](ENVIRONMENT_CONFIGURATION.md)
