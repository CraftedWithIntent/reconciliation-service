# Reconciliation Service

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

Domains are defined in a separate `domains.yml` manifest file for clean separation of concerns. Each domain maps a source and target table for reconciliation.

**File Location:** `src/main/resources/domains.yml`

Spring Boot automatically loads all YAML files from the resources folder, so both `application.yml` and `domains.yml` are merged during startup.

**Domain Configuration Structure:**

```yaml
reconciliation:
  domains:
    {domain-name}:
      name: {domain-name}
      source:
        schema: {source_schema}
        table: {source_table}
        viewSuffix: _with_hash      # Views must be named {table}{viewSuffix}
      target:
        schema: {target_schema}
        table: {target_table}
        viewSuffix: _with_hash
      hashFields: [field1, field2, field3, ...]  # Fields to include in hash
      idField: id                                  # Unique identifier field
      caseSensitive: false
      sloTarget: 95.0                             # Optional: Verdict threshold % (default: 95.0)
      varianceThreshold: 1.0                      # Optional: Field variance threshold % (default: 1.0)
```

**Adding a New Domain:**

Edit `src/main/resources/domains.yml` and add:

```yaml
reconciliation:
  domains:
    my-domain:
      name: my-domain
      source:
        schema: source_schema
        table: source_table
        viewSuffix: _with_hash
      target:
        schema: target_schema
        table: target_table
        viewSuffix: _with_hash
      hashFields: [field1, field2, field3]
      idField: id
      caseSensitive: false
      sloTarget: 95.0              # Optional: Sets verdict threshold (default: 95.0%)
      varianceThreshold: 1.0       # Optional: Field variance threshold (default: 1.0%)
```

Then reconcile with:
```bash
curl "http://localhost:8080/api/reconcile?domain=my-domain"
```

**No code changes required!** Domain management is purely YAML-based.

## Example Domain Configuration

Here is a practical example using a sales reconciliation domain:

### Example: Sales Orders vs Accounting Records

Reconcile sales orders from a sales system with corresponding accounting records.

**Source Table:** `sales_schema.sales_orders`
| Column       | Type        | Constraints    | Description              |
|--------------|-------------|----------------|------------------------------|
| id           | UUID        | PRIMARY KEY    | Unique order identifier  |
| customer_id  | TEXT        | NOT NULL       | Customer reference       |
| order_amount | NUMERIC     | NULLABLE       | Order total              |
| order_date   | DATE        | NOT NULL       | Order date               |
| line_item_id | TEXT        | NOT NULL       | Line item reference      |
| created_at   | TIMESTAMP   | DEFAULT NOW()  | Record creation time     |
| updated_at   | TIMESTAMP   | DEFAULT NOW()  | Record update time       |

**Target Table:** `accounting_schema.accounting_records`
| Column       | Type        | Constraints    | Description              |
|--------------|-------------|----------------|------------------------------|
| id           | UUID        | PRIMARY KEY    | Unique record identifier |
| customer_id  | TEXT        | NOT NULL       | Customer reference       |
| amount       | NUMERIC     | NULLABLE       | Transaction amount       |
| trans_date   | DATE        | NOT NULL       | Transaction date         |
| line_item_id | TEXT        | NOT NULL       | Line item reference      |
| created_at   | TIMESTAMP   | DEFAULT NOW()  | Record creation time     |
| updated_at   | TIMESTAMP   | DEFAULT NOW()  | Record update time       |

**Configuration:**
```yaml
reconciliation:
  domains:
    sales-to-accounting:
      name: sales-to-accounting
      source:
        schema: sales_schema
        table: sales_orders
        viewSuffix: _with_hash
      target:
        schema: accounting_schema
        table: accounting_records
        viewSuffix: _with_hash
      hashFields:
        - customer_id
        - order_amount  # maps to 'amount' in target
        - order_date    # maps to 'trans_date' in target
        - line_item_id
      idField: id
      caseSensitive: false
      sloTarget: 95.0              # 95% match = NOISY, <95% = MATERIAL
      varianceThreshold: 1.0       # Fields >1% variance = MATERIAL
```

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

### Verdict Classification

For HASH_MISMATCH discrepancies, the engine assigns a verdict based on field-level variance analysis:

**PRISTINE**: Record hashes match exactly (100% identical)

**NOISY**: Record hashes differ slightly
- All fields have variance ≤ `varianceThreshold` (default: 1.0%)
- Field-level match percentage ≥ `sloTarget` (default: 95.0%)
- Example: 95% of fields match within 1% variance

**MATERIAL**: Record hashes differ significantly
- At least one field has variance > `varianceThreshold` (default: 1.0%)
- Field-level match percentage < `sloTarget` (default: 95.0%)
- Represents substantive differences requiring investigation

**Customizing Thresholds**: Both `sloTarget` and `varianceThreshold` are configurable per-domain:
- Increase `sloTarget` for stricter reconciliation (e.g., 98.0 for financial data)
- Increase `varianceThreshold` for more tolerance (e.g., 2.0% for rounding differences)

### Hash Generation

MD5 hashes are pre-computed in database views. Each source and target table requires a corresponding `{table}_with_hash` view that concatenates the hash fields.

**PostgreSQL Example:**
```sql
CREATE VIEW {schema}.{table}_with_hash AS
SELECT 
  id,
  md5(CAST(COALESCE(field1, '') || 
           COALESCE(field2::text, 'NULL') || 
           COALESCE(field3::text, '') || 
           COALESCE(field4, '') AS BYTEA)) as record_hash,
  field1, field2, field3, field4
FROM {schema}.{table};
```

**Oracle Example:**
```sql
CREATE VIEW {schema}.{table}_with_hash AS
SELECT 
  id,
  LOWER(DBMS_CRYPTO.Hash(UTL_RAW.CAST_TO_RAW(
    NVL(field1, '') || NVL(TO_CHAR(field2), 'NULL') || 
    NVL(TO_CHAR(field3), '') || NVL(field4, '')
  ), 2)) as record_hash,
  field1, field2, field3, field4
FROM {schema}.{table};
```

Replace placeholders with your actual schema, table, and field names.

## API Endpoints

### GET /api/reconcile?domain={domain-name}

Generic reconciliation endpoint that works for any configured domain.

**Query Parameters:**
- `domain` (required): Domain name from configuration (defined in `domains.yml`)

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
cd /Users/philipthomas/repo/reconciliation-service

# Start services
docker-compose up -d

# Wait for initialization
sleep 30

# Test reconciliation
curl -s "http://localhost:8080/api/reconcile?domain=your-domain-name" | jq .

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
reconciliation-service/
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
