# Quick Start Guide

## Option 1: Docker Compose (Recommended)

### Prerequisites
- Docker Desktop installed and running
- Docker Compose installed (usually included with Docker Desktop)

### Steps

1. **Navigate to project directory:**
   ```bash
   cd /Users/philipthomas/repo/cost-reconcile
   ```

2. **Start all services:**
   ```bash
   docker-compose up -d
   ```

   This will:
   - Build the Spring Boot application
   - Start PostgreSQL source database on port 5432
   - Start PostgreSQL target database on port 5433
   - Start the application on port 8080
   - Initialize both databases with sample data

3. **Verify services are running:**
   ```bash
   docker-compose ps
   ```

   Expected output:
   ```
   NAME                    STATUS
   reconcile-source-db     running
   reconcile-target-db     running
   reconcile-app           running
   ```

4. **Test the API:**
   ```bash
   curl http://localhost:8080/api/reconcile/health
   ```

   Expected response:
   ```
   Reconciliation Service is running
   ```

5. **Run full reconciliation:**
   ```bash
   curl http://localhost:8080/api/reconcile | jq '.'
   ```

6. **Stop all services:**
   ```bash
   docker-compose down -v
   ```

---

## Option 2: Local Development

### Prerequisites
- Java 17 or higher
- Gradle 8.0+ or use included Gradle wrapper
- PostgreSQL 15 installed locally
- Port 5432 and 5433 available

### Steps

1. **Create databases:**
   ```bash
   createdb -U postgres source_db
   createdb -U postgres target_db
   ```

2. **Initialize source database:**
   ```bash
   psql -U postgres -d source_db -f sql/source-db-init.sql
   ```

3. **Initialize target database:**
   ```bash
   psql -U postgres -d target_db -f sql/target-db-init.sql
   ```

4. **Build the application:**
   ```bash
   ./gradlew clean bootJar
   ```

5. **Run the application:**
   ```bash
   java -jar build/libs/reconciliation-service-1.0.0.jar
   ```

   Application will start on http://localhost:8080

6. **Test the API:**
   ```bash
   curl http://localhost:8080/api/reconcile/health
   ```

---

## Testing the Reconciliation

### Test 1: Full Reconciliation
```bash
curl -X GET http://localhost:8080/api/reconcile | jq '.'
```

### Test 2: By Billing Cycle
```bash
curl -X GET "http://localhost:8080/api/reconcile/by-billing-cycle?date=2026-04-01" | jq '.'
```

### Test 3: By Vendor
```bash
curl -X GET "http://localhost:8080/api/reconcile/by-vendor?name=Vendor%20A" | jq '.'
```

### Test 4: By Date Range
```bash
curl -X GET "http://localhost:8080/api/reconcile/by-date-range?startDate=2026-03-01&endDate=2026-04-30" | jq '.'
```

---

## Viewing Logs

### Docker Compose
```bash
# View application logs
docker-compose logs -f reconciliation-app

# View source database logs
docker-compose logs -f source-db

# View target database logs
docker-compose logs -f target-db
```

### Local Development
Logs are written to: `logs/reconciliation-service.log`

---

## Accessing Databases

### Docker Compose
Source database:
```bash
docker exec -it reconcile-source-db psql -U postgres -d source_db
```

Target database:
```bash
docker exec -it reconcile-target-db psql -U postgres -d target_db
```

### Local Development
Source database:
```bash
psql -U postgres -d source_db
```

Target database:
```bash
psql -U postgres -d target_db
```

---

## Sample SQL Queries

### View source records:
```sql
SELECT * FROM source_schema.vendor_invoices;
```

### View target records:
```sql
SELECT * FROM target_schema.internal_ledger;
```

### Filter by vendor:
```sql
SELECT * FROM source_schema.vendor_invoices 
WHERE vendor_name = 'Vendor A';
```

### Filter by billing cycle:
```sql
SELECT * FROM source_schema.vendor_invoices 
WHERE billing_cycle = '2026-04-01';
```

---

## Troubleshooting

### Issue: Port 5432 already in use
**Solution:** Change source database port in docker-compose.yml
```yaml
ports:
  - "5434:5432"  # Changed from 5432
```

### Issue: Port 8080 already in use
**Solution:** Kill process or change port in docker-compose.yml
```yaml
ports:
  - "8081:8080"  # Changed from 8080
```

### Issue: Application won't connect to database
**Solution:** Check Docker network
```bash
docker network inspect reconcile-network
```

### Issue: Database initialization failed
**Solution:** Check SQL scripts
```bash
docker-compose logs source-db
docker-compose logs target-db
```

### Issue: "Cannot connect to Docker daemon"
**Solution:** Ensure Docker Desktop is running

---

## Next Steps

1. **Read the full documentation:** See [README.md](README.md)
2. **API Documentation:** See [API_DOCUMENTATION.md](API_DOCUMENTATION.md)
3. **Implementation Details:** See [IMPLEMENTATION_NOTES.md](IMPLEMENTATION_NOTES.md)
4. **Modify sample data:** Edit `sql/source-db-init.sql` and `sql/target-db-init.sql`
5. **Integrate with your systems:** See API endpoints in API_DOCUMENTATION.md

---

## Performance Tuning

For large datasets (> 100k records):

1. **Increase JVM memory:**
   ```bash
   java -Xmx2g -Xms1g -jar target/reconciliation-service-1.0.0.jar
   ```

2. **Adjust database pool:**
   Edit `application.yml`:
   ```yaml
   hikari:
     maximum-pool-size: 20
   ```

3. **Consider date-range filtering:**
   ```bash
   curl "http://localhost:8080/api/reconcile/by-date-range?startDate=2026-04-01&endDate=2026-04-30"
   ```

---

## Need Help?

- Check logs: `docker-compose logs -f`
- Visit API docs: [API_DOCUMENTATION.md](API_DOCUMENTATION.md)
- Check implementation: [IMPLEMENTATION_NOTES.md](IMPLEMENTATION_NOTES.md)
