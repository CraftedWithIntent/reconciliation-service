# How to Build and Deploy

## Prerequisites

### System Requirements
- Docker and Docker Compose
- Java 17 (only for local builds)
- At least 4GB free disk space
- 2GB available RAM

### Ports Required
- 5432: Source PostgreSQL Database
- 5433: Target PostgreSQL Database  
- 8080: Reconciliation Service API

## Build Instructions

### Step 1: Clone and Navigate
```bash
cd /Users/philipthomas/repo/cost-reconcile
```

### Step 2: Verify Project Structure
```bash
ls -la
# Expected files:
# - build.gradle
# - settings.gradle
# - gradlew (Unix wrapper)
# - gradlew.bat (Windows wrapper)
# - docker-compose.yml
# - Dockerfile
# - sql/
# - src/
```

### Step 3: Build Docker Image
```bash
docker-compose build
```

Expected output:
```
Building reconciliation-app
Step 1/12 : FROM eclipse-temurin:17 AS builder
Step 2/12 : WORKDIR /app
...
Successfully tagged cost-reconcile_reconciliation-app:latest
```

### Step 4: Start Services
```bash
docker-compose up -d
```

### Step 5: Monitor Startup
```bash
docker-compose logs -f reconciliation-app
```

Wait until you see:
```
Started ReconciliationApplication
```

## Deployment Verification

### Health Check
```bash
curl http://localhost:8080/api/reconcile/health
```

Expected response: `Reconciliation Service is running`

### Test Full Reconciliation
```bash
curl http://localhost:8080/api/reconcile | jq '.'
```

### Check Database Connectivity
Source DB:
```bash
docker exec reconcile-source-db psql -U postgres -d source_db -c "SELECT COUNT(*) FROM source_schema.vendor_invoices;"
```

Target DB:
```bash
docker exec reconcile-target-db psql -U postgres -d target_db -c "SELECT COUNT(*) FROM target_schema.internal_ledger;"
```

## Stopping Services

### Graceful Shutdown
```bash
docker-compose down
```

### Complete Cleanup (remove volumes)
```bash
docker-compose down -v
```

## Logs and Monitoring

### View Application Logs
```bash
docker-compose logs reconciliation-app
```

### Follow Logs in Real-time
```bash
docker-compose logs -f reconciliation-app
```

### View Database Logs
```bash
docker-compose logs -f source-db
docker-compose logs -f target-db
```

## Environment Configuration

### Custom Database Credentials

⚠️ **SECURITY**: Keep credentials in `.env` file (gitignored). Never commit passwords to version control.

1. Create `.env` file from template:
```bash
cp .env.example .env
```

2. Edit `.env` with your secure credentials:
```bash
# DO NOT share or commit this file
SOURCE_DB_PASSWORD=your_secure_password_here
TARGET_DB_PASSWORD=your_secure_password_here
```

3. Run with env file:
```bash
docker-compose --env-file .env up -d
```

For production, use a secrets management system (AWS Secrets Manager, Kubernetes Secrets, Vault, etc.) instead of `.env` files. See [SECURITY.md](SECURITY.md) for details.

### Custom Ports
Edit `docker-compose.yml`:
```yaml
services:
  reconciliation-app:
    ports:
      - "8081:8080"  # Use port 8081 instead
```

## Production Deployment

### Step 1: Security Configuration
**Use a secrets management system** for production credentials:

- **AWS**: AWS Secrets Manager or AWS Systems Manager Parameter Store
- **Kubernetes**: Native Kubernetes Secrets
- **HashiCorp**: Vault
- **CI/CD**: GitHub Secrets, GitLab CI/CD variables, Jenkins credentials
- **On-Premises**: Environment variables from secure configuration management

⚠️ **NEVER use `.env` files in production**

Comprehensive guidelines: See [SECURITY.md](SECURITY.md)

### Step 2: Resource Limits
Edit `docker-compose.yml`:
```yaml
services:
  reconciliation-app:
    deploy:
      resources:
        limits:
          cpus: '2'
          memory: 2G
        reservations:
          cpus: '1'
          memory: 1G
```

### Step 3: Persistent Data
```yaml
volumes:
  source_data:
    driver: local
  target_data:
    driver: local
```

### Step 4: Database Backups
```bash
# Backup source database
docker exec reconcile-source-db pg_dump -U postgres source_db > source_backup.sql

# Backup target database
docker exec reconcile-target-db pg_dump -U postgres target_db > target_backup.sql
```

## Troubleshooting Build Issues

### Gradle Build Fails
```bash
# Clear Gradle cache
rm -rf ~/.gradle/caches

# Rebuild
docker-compose build --no-cache
```

### Port Already in Use
```bash
# Find process using port
lsof -i :8080

# Kill process
kill -9 <PID>
```

### Insufficient Disk Space
```bash
# Clean up Docker
docker system prune -a --volumes
```

## Rollback Procedure

### To Previous Version
```bash
# Stop current containers
docker-compose down

# Remove image
docker image remove cost-reconcile_reconciliation-app

# Checkout previous version
git checkout <previous-commit>

# Rebuild and restart
docker-compose up -d
```

## Performance Tuning

### Increase Database Connection Pool
Edit `application.yml`:
```yaml
datasource:
  source:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 10
```

### Increase JVM Memory
Edit `docker-compose.yml`:
```yaml
environment:
  JAVA_OPTS: "-Xmx2g -Xms1g"
```

## Monitoring and Metrics

### Docker Stats
```bash
docker stats reconciliation-app
```

### Database Performance
```bash
# Connect to database
docker exec -it reconcile-source-db psql -U postgres -d source_db

# Check table size
SELECT pg_size_pretty(pg_total_relation_size('source_schema.vendor_invoices'));
```

## Scaling Considerations

### Load Balancing
For multiple instances, use a reverse proxy (Nginx):
```yaml
services:
  nginx:
    image: nginx:alpine
    ports:
      - "80:80"
    depends_on:
      - reconciliation-app-1
      - reconciliation-app-2
```

### Database Replication
For high availability:
1. Set up PostgreSQL replication
2. Configure read replicas
3. Update connection strings

## Maintenance Tasks

### Daily
- Check application logs
- Monitor disk space
- Verify API health

### Weekly
- Backup databases
- Review reconciliation reports
- Check performance metrics

### Monthly
- Update dependencies
- Run full reconciliation
- Clean up old logs

## Support

For issues or questions:
1. Check [README.md](README.md)
2. Review [IMPLEMENTATION_NOTES.md](IMPLEMENTATION_NOTES.md)
3. Check [API_DOCUMENTATION.md](API_DOCUMENTATION.md)
4. Review Docker logs: `docker-compose logs`
