# Contributing to Reconciliation Service

Thank you for your interest in contributing! We welcome all contributions, from bug fixes to feature implementations to documentation improvements.

## Getting Started

### Prerequisites
- Java 17+
- Gradle 8.x (or use `./gradlew`)
- Docker & Docker Compose (for integration testing)
- Git

### Setting Up Development Environment

1. **Fork the repository** on GitHub
2. **Clone your fork locally:**
   ```bash
   git clone https://github.com/YOUR_USERNAME/reconciliation-service.git
   cd reconciliation-service
   ```

3. **Create a feature branch:**
   ```bash
   git checkout -b feature/your-feature-name
   ```

4. **Set up the development environment:**
   ```bash
   # Using local profile (default)
   ./gradlew bootRun
   
   # Or with Docker Compose
   docker-compose up
   ```

5. **Run tests locally:**
   ```bash
   ./gradlew clean build
   ```

## Development Workflow

### Code Style
This project uses **Spotless** for code formatting and **Checkstyle** for style enforcement:

```bash
# Format code
./gradlew spotlessApply

# Check formatting
./gradlew spotlessCheck

# Run style checks
./gradlew checkstyleMain checkstyleTest
```

All PRs must pass formatting checks before merging.

### Testing Requirements
- Write unit tests for new functionality
- Update existing tests when modifying behavior
- Ensure all 219+ tests pass: `./gradlew test`
- For database-specific tests: `./gradlew test --tests '*PostgreSQL*'` (etc.)

### Configuration

This project supports environment-specific configurations:

- **local** - Development (localhost, DEBUG logging)
- **dev** - Team development server
- **qa** - Quality assurance environment  
- **prod** - Production (strict validation)

See [ENVIRONMENT_CONFIGURATION.md](ENVIRONMENT_CONFIGURATION.md) for details.

### Per-Domain Configuration

Reconciliation thresholds are configurable per domain in `domains-{env}.yml`:

```yaml
domains:
  sales:
    sloTarget: 95          # Service level objective %
    varianceThreshold: 1   # Max variance %
    # Optional: override global datasource
    datasource:
      url: jdbc:postgresql://custom-host:5432/sales_db
```

## Submitting Changes

### 1. Commit Guidelines
- Make atomic commits with clear messages
- Reference issues when relevant: `Fix #123: Description`
- Keep commits focused on a single concern

### 2. Push to Your Fork
```bash
git push origin feature/your-feature-name
```

### 3. Create a Pull Request
- Use the PR template (auto-populated)
- Link related issues
- Describe your changes clearly
- Ensure all checks pass

### Branch Protection Rules
- All PRs require at least **1 approval**
- GitHub Actions CI/CD must pass
- Code formatting must pass (Spotless, Checkstyle)
- No direct pushes to `main` branch

## Reporting Issues

### Bug Reports
Include:
- Clear description of the bug
- Steps to reproduce
- Expected vs actual behavior
- Environment details (Java version, OS, database)
- Error logs/stack traces

### Feature Requests
Include:
- Clear use case
- Expected behavior
- Why it's valuable
- Any relevant examples

## Documentation

- Update `README.md` for user-facing changes
- Update `ARCHITECTURE_GUIDE.md` for architectural changes
- Update relevant guides in the `docs/` folder
- Add code comments for complex logic
- Include examples in docstrings

## Project Structure

```
src/main/java/com/reconcile/
├── config/          # Configuration classes
├── controller/      # REST API endpoints
├── dto/             # Data transfer objects
├── engine/          # Reconciliation logic
├── generator/       # SQL query generators
├── service/         # Business logic
└── util/            # Utilities

src/test/java/com/reconcile/
└── (mirror structure with *Test.java files)
```

## Key Technologies

- **Spring Boot 3.1.5** - Application framework
- **Apache Spark 3.5.0** - Distributed data processing
- **PostgreSQL/Oracle/MySQL/SQL Server** - Database support
- **JUnit 5** - Testing framework
- **Spotless** - Code formatting
- **Checkstyle** - Style enforcement

## Need Help?

- Check [README.md](README.md) for quick start
- Read [ARCHITECTURE_GUIDE.md](ARCHITECTURE_GUIDE.md) for technical details
- Review [API_DOCUMENTATION.md](API_DOCUMENTATION.md) for endpoints
- See [QUICK_REFERENCE.md](QUICK_REFERENCE.md) for common tasks

## Code of Conduct

- Be respectful and inclusive
- Provide constructive feedback
- Welcome diverse perspectives
- Report issues through proper channels

Thank you for contributing! 🚀
