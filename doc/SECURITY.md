# Security Guide - Secrets Management

## Overview

This document outlines how to safely manage sensitive data (passwords, API keys, credentials) in this project. **Never commit secrets to version control.**

---

## Local Development Setup

### 1. Create `.env` File

Copy `.env.example` to `.env` (which is gitignored):

```bash
cp .env.example .env
```

### 2. Set Your Local Credentials

Edit `.env` with your local database passwords:

```bash
# .env
SOURCE_DB_PASSWORD=your_secure_password
TARGET_DB_PASSWORD=your_secure_password
```

### 3. Load Environment Variables

When running locally:

```bash
# Using docker-compose with .env file
docker-compose up

# Or export manually
export $(cat .env | xargs)
./gradlew bootRun
```

---

## Files to NEVER Commit Secrets To

❌ **DO NOT ADD CREDENTIALS TO:**
- ✗ `docker-compose.yml` - Use environment variables with `${VAR_NAME}` syntax
- ✗ `application.yml` - Use SpEL or environment variables, never hardcode
- ✗ `application.properties` - Same as above
- ✗ Code comments or documentation
- ✗ Git history (use BFG or git-filter-branch if already committed)

---

## Correct Patterns

### Environment Variables

```yaml
# ✓ CORRECT - Use environment variable
password: ${DB_PASSWORD}

# ✓ CORRECT - With fallback to env var only (no default)
password: ${DB_PASSWORD:#{T(java.lang.System).getenv('DB_PASSWORD')}}

# ✗ WRONG - Hardcoded default
password: ${DB_PASSWORD:hardcoded123}
```

### Docker Compose

```yaml
# ✓ CORRECT - Reference env variables
environment:
  DB_PASSWORD: ${SOURCE_DB_PASSWORD}
  
# ✗ WRONG - Hardcoded credentials
environment:
  DB_PASSWORD: postgres123
```

### Java Code

```java
// ✓ CORRECT - Spring Value annotation pulls from env
@Value("${spring.datasource.password}")
private String password;

// ✓ CORRECT - System.getenv()
String password = System.getenv("DB_PASSWORD");

// ✗ WRONG - Hardcoded
private String password = "postgres123";
```

---

## Production Deployment

For production environments, use one of these solutions:

### AWS (Recommended)

```yaml
# Use AWS Secrets Manager
environment:
  - name: DB_PASSWORD
    valueFrom:
      secretKeyRef:
        name: db-credentials
        key: password
```

### Kubernetes Secrets

```bash
# Create secret
kubectl create secret generic db-credentials \
  --from-literal=password=your_secure_password

# Reference in deployment
env:
  - name: SOURCE_DB_PASSWORD
    valueFrom:
      secretKeyRef:
        name: db-credentials
        key: source-password
```

### HashiCorp Vault

```bash
# Retrieve at runtime
vault read secret/database/credentials
```

### Environment Variables in CI/CD

```yaml
# GitHub Actions example
env:
  SOURCE_DB_PASSWORD: ${{ secrets.SOURCE_DB_PASSWORD }}
  TARGET_DB_PASSWORD: ${{ secrets.TARGET_DB_PASSWORD }}
```

---

## Configuration Priority

The application reads configuration in this order (first found wins):

1. **System Properties** - `-Dspring.datasource.password=value`
2. **Environment Variables** - `SOURCE_DB_PASSWORD=value`
3. **Application Config** - `application.yml` (NO DEFAULTS FOR SECRETS)
4. **Fail** - If required secret not provided

---

## Security Checklist

Before committing or deploying:

- [ ] No passwords in `.yml` or `.properties` files
- [ ] No secrets in docker-compose.yml (except as env var references)
- [ ] No API keys or tokens in code
- [ ] `.env` is in `.gitignore` and not tracked
- [ ] All credentials sourced from environment variables
- [ ] Production uses secret management service
- [ ] Developers use `.env` file for local work
- [ ] Code review includes security check

---

## Scanning for Secrets

Run these commands to detect if secrets were accidentally committed:

```bash
# Detect common secret patterns
grep -r "password\|secret\|token\|api.key" src/ --include="*.java" \
  | grep -v "//" | grep -v ":\s*String" | grep -v "getPassword"

# Check if .env was committed
git log --all --full-history -- ".env" | head -20

# Use git-secrets (third-party tool)
cd .git/hooks
curl https://raw.githubusercontent.com/awslabs/git-secrets/master/install.sh | sh
git secrets --scan
```

---

## Remediation

If secrets were accidentally committed:

```bash
# Option 1: Remove from history with BFG
bfg --delete-files .env --no-blob-protection

# Option 2: Using git-filter-branch (slower)
git filter-branch --tree-filter 'rm -f .env' -- --all

# Option 3: If not yet pushed, just amend
git rm --cached .env
echo ".env" >> .gitignore
git add .gitignore
git commit --amend --no-edit
```

---

## Current Status

✅ **Cleaned Up**
- docker-compose.yml - Uses environment variables
- application.yml - Passwords reference env vars only
- .env - Created (gitignored) for local development
- .env.example - Template without actual credentials

✅ **Protected**
- `.env` is in `.gitignore`
- `.env.local` is in `.gitignore`
- Gradle build files don't contain secrets
- Docker build doesn't bake in credentials

---

## References

- [Spring Boot Configuration](https://spring.io/guides/gs/spring-boot-docker/)
- [12-Factor App Secrets](https://12factor.net/config)
- [AWS Secrets Manager](https://aws.amazon.com/secrets-manager/)
- [OWASP Secrets Management](https://cheatsheetseries.owasp.org/cheatsheets/Secrets_Management_Cheat_Sheet.html)
