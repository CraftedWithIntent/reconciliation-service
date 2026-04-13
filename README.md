# Cost Reconciliation Service

A distributed reconciliation microservice built with Spring Boot 3.1.5 and Apache Spark 3.5.0 for high-performance hash-based reconciliation across heterogeneous data sources (PostgreSQL, Oracle, MySQL, SQL Server).

## Key Features

- **Multi-Domain Support**: Reconcile different data types via YAML configuration
- **Heterogeneous Data Sources**: Connect to PostgreSQL, Oracle, MySQL, and SQL Server simultaneously
- **Distributed Processing**: Apache Spark for efficient large-scale reconciliation
- **MD5-Based Hashing**: Fast, deterministic record comparison
- **Generic Architecture**: No domain-specific code—fully configurable
- **Dynamic Database Selection**: Automatic JDBC driver detection based on URL patterns
- **Spring Boot 3.x Compatible**: Includes servlet API compatibility for Spark 3.5.0

## Architecture Overview

```
┌──────────────────────────────────────────────────────────────┐
│               Reconciliation Service (Spring Boot)           │
│                    REST API (port 8080)                      │
├──────────────────────────────────────────────────────────────┤
│  ReconciliationController                                    │
│  ↓ routes domain request                                     │
│  ReconciliationService                                       │
│  ↓ validates domain config                                   │
│  SparkReconciliationEngine                                   │
│  ├─ detectDatabaseType() → identifies Oracle/PostgreSQL      │
│  ├─ getDriverClass() → loads appropriate JDBC driver        │
│  ├─ readSourceData() → Spark JDBC reader                    │
│  ├─ readTargetData() → Spark JDBC reader                    │
│  └─ reconcile() → SQL-based join with MD5 comparison        │
└──────────────────────────────────────────────────────────────┘
         │                │                │                │
         ▼                ▼                ▼                ▼
    PostgreSQL         Oracle           MySQL         SQL Server
    (Port 5432)       (Port 1521)     (Port 3306)    (Port 1433)
    via Spark JDBC Connector (supports heterogeneous combinations)
```

## Domain Configuration

Domains are defined in a separate `domains.yml` manifest file for clean separation of concerns.

**File Location:** `src/main/resources/domains.yml`

Spring Boot automatically loads all YAML files from the resources folder, so both `application.yml` and `domains.yml` are merged during startup.

**Example Domain Configuration (from domains.yml):**

```yaml
reconciliation:
  domains:
    vendor-invoices:
      name: vendor-invoices
      source:
        schema: source_schema
        table: vendor_invoices
        viewSuffix: _with_hash      # Views must be named {table}{viewSuffix}
      target:
        schema: target_schema
        table: internal_ledger
        viewSuffix: _with_hash
      hashFields:
        - vendor_name
        - amount
        - billing_cycle
        - line_item_id
      idField: id
      caseSensitive: false
```

**Adding a New Domain:**

Edit `src/main/resources/domains.yml` and add a new entry:

```yaml
reconciliation:
  domains:
    my-new-domain:
      name: my-new-domain
      source:
        schema: my_source_schema
        table: my_source_table
        viewSuffix: _with_hash
      target:
        schema: my_target_schema
        table: my_target_table
        viewSuffix: _with_hash
      hashFields: [field1, field2, field3]
      idField: id
      caseSensitive: false
```

Then reconcile with:
```bash
curl "http://localhost:8080/api/reconcile?domain=my-new-domain"
```

**No code changes required!** Domain management is purely YAML-based.

## Data Schema

### Source Database: vendor_invoices

| Column       | Type        | Constraints    | Description                  |
|--------------|-------------|----------------|------------------------------|
| id           | UUID        | PRIMARY KEY    | Unique record identifier     |
| vendor_name  | TEXT        | NOT NULL       | Name of the vendor           |
| amount       | NUMERIC     | NULLABLE       | Invoice amount               |
| billing_cycle| DATE        | NOT NULL       | Billing period               |
| line_item_id | TEXT        | NOT NULL       | Line item reference          |
| created_at   | TIMESTAMP   | DEFAULT NOW()  | Record creation timestamp    |
| updated_at   | TIMESTAMP   | DEFAULT NOW()  | Record update timestamp      |

### Target Database: internal_ledger

| Column       | Type        | Constraints    | Description                  |
|--------------|-------------|----------------|------------------------------|
| id           | UUID        | PRIMARY KEY    | Unique record identifier     |
| vendor_name  | TEXT        | NOT NULL       | Name of the vendor           |
| amount       | NUMERIC     | NULLABLE       | Ledger amount                |
| billing_cycle| DATE        | NOT NULL       | Billing period               |
| line_item_id | TEXT        | NOT NULL       | Line item reference          |
| created_at   | TIMESTAMP   | DEFAULT NOW()  | Record creation timestamp    |
| updated_at   | TIMESTAMP   | DEFAULT NOW()  | Record update timestamp      |

## Reconciliation Logic

The SparkReconciliationEngine performs distributed reconciliation using Spark SQL:

1. **Database Type Detection**: Analyzes JDBC URL to identify Oracle/PostgreSQL/etc.
2. **Driver Selection**: Loads appropriate JDBC driver dynamically
3. **Data Ingestion**: Spark reads source and target via JDBC connector
4. **Hash Comparison**: Full outer join on ID field, compares record hashes
5. **Result Classification**:
   - **MATCH**: ID present in both with identical record hash
   - **HASH_MISMATCH**: ID present in both but hashes differ
   - **SOURCE_ONLY**: ID only in source
   - **TARGET_ONLY**: ID only in target

### Hash Generation

MD5 hashes are pre-computed in database views:

**PostgreSQL:**
```sql
CREATE VIEW source_schema.vendor_invoices_with_hash AS
SELECT 
  id,
  md5(CAST(COALESCE(vendor_name, '') || 
           COALESCE(amount::text, 'NULL') || 
           COALESCE(billing_cycle::text, '') || 
           COALESCE(line_item_id, '') AS BYTEA)) as record_hash,
  vendor_name, amount, billing_cycle, line_item_id
FROM source_schema.vendor_invoices;
```

**Oracle:**
```sql
CREATE VIEW source_schema.vendor_invoices_with_hash AS
SELECT 
  id,
  LOWER(DBMS_CRYPTO.Hash(UTL_RAW.CAST_TO_RAW(
    NVL(vendor_name, '') || NVL(TO_CHAR(amount), 'NULL') || 
    NVL(TO_CHAR(billing_cycle), '') || NVL(line_item_id, '')
  ), 2)) as record_hash,
  vendor_name, amount, billing_cycle, line_item_id
FROM source_schema.vendor_invoices;
```

## API Endpoints

### GET /api/reconcile?domain={domain-name}

Generic reconciliation endpoint that works for any configured domain.

**Query Parameters:**
- `domain` (required): Domain name from configuration (e.g., `vendor-invoices`, `expense-reports`)

**Response:**
```json
{
  "totalSourceRecords": 1000,
  "totalTargetRecords": 995,
  "matchedRecords": 950,
  "mismatchedRecords": 45,
  "sourceOnlyRecords": 5,
  "targetOnlyRecords": 0,
  "matchPercentage": 95.0,
  "discrepancies": [
    {
      "discrepancyType": "HASH_MISMATCH",
      "sourceRecord": {
        "id": "550e8400-e29b-41d4-a716-446655440000",
        "recordHash": "5d41402abc4b2a76b9719d911017c592",
        "source": "source"
      },
      "targetRecord": {
        "id": "550e8400-e29b-41d4-a716-446655440000",
        "recordHash": "6512bd43d9caa6e02c990b0a82652dca",
        "source": "target"
      },
      "details": "Record hash differs between source and target"
    }
  ]
}
```

## Technology Stack

- **Framework**: Spring Boot 3.1.5, Java 17
- **Big Data**: Apache Spark 3.5.0 with Spark SQL
- **Build**: Gradle 8.4
- **Databases**: PostgreSQL, Oracle, MySQL, SQL Server (via JDBC)
- **Container**: Docker & Docker Compose

## Setup and Deployment

### Quick Start with Docker

```bash
cd /Users/philipthomas/repo/cost-reconcile

# Start services
docker-compose up -d

# Wait for initialization
sleep 30

# Test reconciliation
curl -s "http://localhost:8080/api/reconcile?domain=vendor-invoices" | jq .

# View logs
docker-compose logs -f reconcile-app
```

### Local Development Setup

1. **Prerequisites:**
   - Java 17+
   - Gradle 8.4+ (or use included wrapper)
   - PostgreSQL 15 or other databases

2. **Set environment variables for your local credentials:**
```bash
# Create .env file from template
cp .env.example .env

# Edit .env with your local passwords
# SOURCE_DB_PASSWORD=your_password
# TARGET_DB_PASSWORD=your_password
```

3. **Datasources are configured in `application.yml` using environment variables:**
```yaml
spring:
  datasource:
    source:
      url: jdbc:postgresql://localhost:5432/source_db
      username: ${SOURCE_DB_USER:postgres}
      password: ${SOURCE_DB_PASSWORD}  # Set via environment variable
    target:
      url: jdbc:postgresql://localhost:5433/target_db
      username: ${TARGET_DB_USER:postgres}
      password: ${TARGET_DB_PASSWORD}  # Set via environment variable
```

⚠️ **IMPORTANT**: Never commit `.env` file—it's gitignored for security. See [SECURITY.md](doc/SECURITY.md) for details.

4. **Build and run with environment variables:**
```bash
./gradlew clean build -x test

# Load .env variables and run
set -a
source .env
set +a
./gradlew bootRun
```

## Heterogeneous Database Support

Supports any combination of databases as source and target:

| Scenario | Status |
|----------|--------|
| PostgreSQL → PostgreSQL | ✅ Built-in |
| PostgreSQL → Oracle | ✅ Heterogeneous |
| Oracle → PostgreSQL | ✅ Heterogeneous |
| Oracle → Oracle | ✅ Heterogeneous |
| PostgreSQL → MySQL | ✅ Heterogeneous |
| Any → Any | ✅ Dynamic driver support |

### Enabling Oracle

**Option 1: Maven Central (Recommended)**
```gradle
runtimeOnly 'com.oracle.database.jdbc:ojdbc11:23.2.0.0'
```

**Option 2: Local JAR**
Place `ojdbc11.jar` in `libs/` folder:
```gradle
runtimeOnly files('libs/ojdbc11.jar')
```

Rebuild:
```bash
./gradlew clean build -x test
```

## Project Structure

```
cost-reconcile/
├── src/main/java/com/reconcile/
│   ├── controller/
│   │   └── ReconciliationController.java       # Single generic endpoint
│   ├── service/
│   │   └── ReconciliationService.java          # Orchestration & Spark management
│   ├── engine/
│   │   └── SparkReconciliationEngine.java      # Spark SQL, DB detection
│   ├── config/
│   │   ├── DomainConfig.java                   # Domain structure
│   │   ├── DatabaseConfig.java                 # DB configuration
│   │   └── ReconciliationConfigProperties.java # YAML binding
│   ├── dto/
│   │   ├── ReconciliationRecordDTO.java        # Generic record
│   │   ├── ReconciliationResultDTO.java        # Result summary
│   │   └── DiscrepancyDTO.java                 # Discrepancy details
│   └── ReconciliationApplication.java
├── src/main/resources/
│   ├── application.yml                         # Spring Boot config (datasources, logging, etc.)
│   └── domains.yml                             # Domain manifest (reconciliation configuration)
├── build.gradle
├── docker-compose.yml
└── README.md
```

## Configuration via Environment Variables

All sensitive configuration should be managed through environment variables. Never commit `.env` files.

```bash
# Source Database (required)
SOURCE_DB_HOST=source-db
SOURCE_DB_PORT=5432
SOURCE_DB_NAME=source_db
SOURCE_DB_USER=postgres
SOURCE_DB_PASSWORD=your_secure_password  # Required - set in .env or secrets manager

# Target Database (required)
TARGET_DB_HOST=target-db
TARGET_DB_PORT=5433
TARGET_DB_NAME=target_db
TARGET_DB_USER=postgres
TARGET_DB_PASSWORD=your_secure_password  # Required - set in .env or secrets manager

# Server
SERVER_PORT=8080
```

**For local development:** Store these in `.env` (gitignored)  
**For production:** Use AWS Secrets Manager, Kubernetes Secrets, or HashiCorp Vault  

📖 **See [SECURITY.md](doc/SECURITY.md) for comprehensive security guidance**

## Performance

| Dataset Size | Expected Runtime | Memory |
|--------------|-------------------|--------|
| 1K records | < 1 sec | 512 MB |
| 10K records | 1-2 sec | 1 GB |
| 100K records | 5-10 sec | 2 GB |
| 1M records | 30-60 sec | 4+ GB |

## Troubleshooting

### Build Errors
```bash
./gradlew clean build -x test --refresh-dependencies
```

### Connection Issues
```bash
# Verify database is running
docker-compose ps

# Check connectivity
docker exec source-db psql -U postgres -c "SELECT 1"
```

### Reconciliation Not Finding Domain
1. Verify domain exists in `application.yml`
2. Check YAML indentation
3. Restart: `docker-compose restart reconcile-app`

### Slow Performance
1. Check database indexes on hash fields
2. Verify views exist in both databases
3. Increase executor memory if needed

## Stopping Services

```bash
# Stop containers
docker-compose down

# Stop and clean volumes
docker-compose down -v

# View logs
docker-compose logs reconcile-app | tail -50
```

## License

MIT

## Support

For detailed information, see [IMPLEMENTATION_NOTES.md](doc/IMPLEMENTATION_NOTES.md) or [BUILD_AND_DEPLOY.md](doc/BUILD_AND_DEPLOY.md).
