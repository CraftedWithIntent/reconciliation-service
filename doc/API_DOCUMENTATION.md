# API Documentation

## Base URL
```
http://localhost:8080/api/reconcile
```

## Single Generic Endpoint

The reconciliation service provides a single domain-aware endpoint that works with any configured domain.

### GET /api/reconcile?domain={domain-name}

Performs reconciliation for the specified domain using Spark SQL with MD5-based hash comparison.

**Description:** 
Reconciles data between source and target databases for a specific domain. The engine automatically:
1. Detects source and target database types
2. Selects appropriate JDBC drivers (Oracle, PostgreSQL, MySQL, SQL Server)
3. Reads data via Spark JDBC connector
4. Performs full outer join on ID field
5. Compares record hashes and generates discrepancy report

**Query Parameters:**
| Parameter | Type | Required | Description | Example |
|-----------|------|----------|-------------|---------|
| domain | String | Yes | Domain name from configuration | vendor-invoices, expense-reports |

**Request:**
```bash
# Reconcile vendor invoices
curl -X GET "http://localhost:8080/api/reconcile?domain=vendor-invoices"

# Reconcile expense reports
curl -X GET "http://localhost:8080/api/reconcile?domain=expense-reports"
```

**Response (200 OK):**
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
        "source": "source",
        "data": {
          "vendor_name": "Vendor A",
          "amount": "1200.00",
          "billing_cycle": "2026-03-01",
          "line_item_id": "LINE001"
        }
      },
      "targetRecord": {
        "id": "550e8400-e29b-41d4-a716-446655440000",
        "recordHash": "6512bd43d9caa6e02c990b0a82652dca",
        "source": "target",
        "data": {
          "vendor_name": "Vendor A",
          "amount": "1250.00",
          "billing_cycle": "2026-03-01",
          "line_item_id": "LINE001"
        }
      },
      "details": "Record hash differs between source and target"
    },
    {
      "discrepancyType": "SOURCE_ONLY",
      "sourceRecord": {
        "id": "550e8400-e29b-41d4-a716-446655440001",
        "recordHash": "7c13219b75f5b7b0c8f8e6d5c4b3a2b1",
        "source": "source",
        "data": {
          "vendor_name": "Vendor B",
          "amount": "500.00",
          "billing_cycle": "2026-03-15",
          "line_item_id": "LINE002"
        }
      },
      "targetRecord": null,
      "details": "Record exists only in source"
    },
    {
      "discrepancyType": "TARGET_ONLY",
      "sourceRecord": null,
      "targetRecord": {
        "id": "550e8400-e29b-41d4-a716-446655440002",
        "recordHash": "8d32320b86f6a8a0d9g9f7e6d5c4b3a2c",
        "source": "target",
        "data": {
          "vendor_name": "Vendor C",
          "amount": "300.00",
          "billing_cycle": "2026-03-20",
          "line_item_id": "LINE003"
        }
      },
      "details": "Record exists only in target"
    }
  ]
}
```

**Response Fields:**

| Field | Type | Description |
|-------|------|-------------|
| totalSourceRecords | Integer | Total records in source database |
| totalTargetRecords | Integer | Total records in target database |
| matchedRecords | Integer | Records with identical hashes in both |
| mismatchedRecords | Integer | Records in both with different hashes |
| sourceOnlyRecords | Integer | Records only in source |
| targetOnlyRecords | Integer | Records only in target |
| matchPercentage | Double | Percentage match (matchedRecords / max(source, target) * 100) |
| discrepancies | Array | List of detailed mismatches |

**Discrepancy Types:**

| Type | Meaning | Source | Target |
|------|---------|--------|--------|
| MATCH | Record present in both with same hash | ✓ | ✓ |
| HASH_MISMATCH | Record in both but hashes differ | ✓ | ✓ |
| SOURCE_ONLY | Record only in source | ✓ | ✗ |
| TARGET_ONLY | Record only in target | ✗ | ✓ |

**Error Responses:**

```bash
# Domain not found (400 Bad Request)
curl -X GET "http://localhost:8080/api/reconcile?domain=nonexistent"
```

Response:
```json
{
  "error": "Domain 'nonexistent' not configured. Available domains: [vendor-invoices, expense-reports]"
}
```

```bash
# Missing domain parameter (400 Bad Request)
curl -X GET "http://localhost:8080/api/reconcile"
```

Response:
```json
{
  "error": "Query parameter 'domain' is required"
}
```

```bash
# Database connection error (503 Service Unavailable)
```

Response:
```json
{
  "error": "Reconciliation failed for domain 'vendor-invoices': Connection refused to source database"
}
```

## Database Type Detection

The engine automatically detects database types from JDBC URLs:

| URL Pattern | Detected Type | JDBC Driver |
|-------------|---------------|-------------|
| `jdbc:postgresql://...` | PostgreSQL | `org.postgresql.Driver` |
| `jdbc:oracle:thin:@...` | Oracle | `oracle.jdbc.OracleDriver` |
| `jdbc:mysql://...` | MySQL | `com.mysql.cj.jdbc.Driver` |
| `jdbc:sqlserver://...` | SQL Server | `com.microsoft.sqlserver.jdbc.SQLServerDriver` |

**Example: Heterogeneous Reconciliation**
```yaml
spring:
  datasource:
    source:
      url: jdbc:postgresql://localhost:5432/source_db  # PostgreSQL
    target:
      url: jdbc:oracle:thin:@localhost:1521:ORCL       # Oracle
```

The reconciliation engine automatically uses PostgreSQL driver for source and Oracle driver for target.

## Usage Examples

### Example 1: Full Reconciliation with Pretty Print
```bash
curl -s "http://localhost:8080/api/reconcile?domain=vendor-invoices" | jq .
```

### Example 2: Get Match Percentage Only
```bash
curl -s "http://localhost:8080/api/reconcile?domain=vendor-invoices" | jq '.matchPercentage'
```

Output:
```
95.0
```

### Example 3: List All Discrepancies
```bash
curl -s "http://localhost:8080/api/reconcile?domain=vendor-invoices" | jq '.discrepancies[] | "\(.discrepancyType): \(.details)"'
```

Output:
```
HASH_MISMATCH: Record hash differs between source and target
SOURCE_ONLY: Record exists only in source
TARGET_ONLY: Record exists only in target
```

### Example 4: Count Discrepancies by Type
```bash
curl -s "http://localhost:8080/api/reconcile?domain=vendor-invoices" | \
  jq '[.discrepancies[] | .discrepancyType] | group_by(.) | map({type: .[0], count: length})'
```

### Example 5: Get Records with Hashes
```bash
curl -s "http://localhost:8080/api/reconcile?domain=vendor-invoices" | \
  jq '.discrepancies[] | select(.discrepancyType=="HASH_MISMATCH") | {id: .sourceRecord.id, sourceHash: .sourceRecord.recordHash, targetHash: .targetRecord.recordHash}'
```

### Example 6: Export to CSV (via jq)
```bash
curl -s "http://localhost:8080/api/reconcile?domain=vendor-invoices" | \
  jq -r '.discrepancies[] | [.discrepancyType, .sourceRecord.id, .details] | @csv'
```

## Configuration for Domains

Add new domains to `application.yml` without any code changes:

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

## Performance Notes

- Hash computation happens at the database level (faster)
- Spark loads data via JDBC partitioning for large tables
- Results cached during single request cycle
- Large discrepancy lists returned as JSON array
- Typical performance: 1000 records in ~1 second

## Rate Limiting

- No rate limiting applied (can be added via Spring Security)
- Long-running reconciliations may timeout (configurable)
- Concurrent requests to different domains are supported
