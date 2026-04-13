# Reconciliation Implementation Notes

## Skill Application: Reconciliation Domain

This document outlines the application of a reconciliation skill to the vendor invoice and internal ledger domain.

### Domain Analysis

**Source System:** PostgreSQL (vendor_invoices table)
- Tracks vendor invoices as they are received
- Primary key: UUID id
- Key fields: vendor_name, amount, billing_cycle, line_item_id

**Target System:** PostgreSQL (internal_ledger table)
- Internal financial ledger entries
- Primary key: UUID id
- Same schema as source for data compatibility

**Reconciliation Goal:** Ensure all vendor invoices are accurately reflected in the internal ledger

### Reconciliation Strategy

#### 1. Hash-Based Comparison
- **Why:** Eliminates need for record-by-record manual comparison
- **Algorithm:** SHA-256 hash of composite key (excluding id)
- **Input Fields:** vendor_name, amount, billing_cycle, line_item_id
- **Benefits:** Fast, deterministic, handles complex matching logic

#### 2. Field Exclusion
- **Excluded:** id (UUIDs differ between systems)
- **Included:** All other fields for matching
- **Rationale:** Business data should match; system identifiers will differ

#### 3. Null Safety for Amount
- **Challenge:** Amount field can be NULL
- **Solution:** Treat nulls as distinct value in hash
- **Implementation:** NULL amounts explicitly marked as "NULL" string in hash input
- **Benefit:** Null vs non-null amounts are correctly distinguished

### Implementation Architecture

#### Layer 1: Controller (ReconciliationController)
- REST endpoints for various reconciliation scenarios
- Input validation
- Response formatting
- HTTP status code handling

#### Layer 2: Service (ReconciliationService)
- Core reconciliation logic
- Hash generation with null-safe handling
- Record comparison algorithms
- Statistical calculations
- Multiple filtering strategies (by date, vendor, date range)

#### Layer 3: Repository (VendorInvoiceRepository, InternalLedgerRepository)
- Database access
- Entity mapping
- Query optimization with indexes
- Support for filtered queries

#### Layer 4: Models and DTOs
- VendorInvoice: JPA entity for source data
- InternalLedger: JPA entity for target data
- ReconciliationRecordDTO: Standardized record representation
- ReconciliationResultDTO: Aggregated reconciliation results
- DiscrepancyDTO: Identified differences

### Key Design Decisions

1. **Multi-Datasource Configuration**
   - Separate DataSource beans for source and target
   - Isolated EntityManagerFactory instances
   - Allows true comparison between systems

2. **Hash Generation**
   ```
   SHA-256(vendorName | amount | billingCycle | lineItemId)
   ```
   - Platform-independent
   - Detects any data difference
   - Efficient comparison

3. **Discrepancy Classification**
   - MISSING_IN_TARGET: Source record absent in target
   - MISSING_IN_SOURCE: Target record absent in source
   - DATA_MISMATCH: (reserved for future enhanced comparison)

4. **Reconciliation Modes**
   - Full reconciliation: All records
   - By billing cycle: Single period
   - By vendor: Single vendor
   - By date range: Multiple periods

## Domain Manifest Configuration

The reconciliation service uses a **domain manifest** (separate YAML file) for clean separation of concerns.

**Files:**
- `src/main/resources/application.yml` - Spring Boot configuration (datasources, logging, server settings)
- `src/main/resources/domains.yml` - Domain manifest (reconciliation domain definitions)

**Why Separate?**
- Application configuration remains focused on infrastructure
- Domain configuration is isolated and easy to maintain
- Domains can be updated without affecting application settings
- Clear separation of responsibilities

**How It Works:**
1. Spring Boot loads `application.yml` on startup
2. Spring Boot automatically includes all YAML files from `resources/` folder
3. Spring merges both configurations at runtime
4. `ReconciliationConfigProperties` loads `reconciliation.domains` from merged configuration
5. `@PostConstruct` logs: `"Loaded X domain(s): [...]"`

**Adding a New Domain:**
Simply add an entry to `domains.yml` - no code changes needed:

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

Then immediately available:
```bash
curl "http://localhost:8080/api/reconcile?domain=my-new-domain"
```

### Data Flow

```
Controller (REST Request)
    ↓
Service (Reconciliation Logic)
    ├→ Fetch source records (Repository)
    ├→ Fetch target records (Repository)
    ├→ Generate hashes (all records)
    ├→ Build hash maps (in-memory)
    ├→ Compare sets
    └→ Aggregate statistics
    ↓
DTOs (Response Formatting)
    ↓
Controller (HTTP Response)
```

### Performance Optimizations

1. **Database Indexes**
   - Index on vendor_name, billing_cycle, line_item_id
   - Accelerates filtered queries

2. **Hash Map Usage**
   - O(1) lookup for record comparison
   - Scales efficiently for large datasets

3. **Query Filtering**
   - Custom queries for specific reconciliation scenarios
   - Reduce data transfer from database

4. **Connection Pooling**
   - Spring Boot default pool for both datasources
   - Configurable pool size in application.yml

### Null Handling Strategy

**Problem:** Amount field can be NULL in both source and target
**Solution:** Null-safe hashing

1. Check if amount is null
2. If null: use string "NULL" in hash
3. If not null: strip trailing zeros for consistency
4. Example:
   - NULL amount + Vendor A + 2026-04-01 → hash1
   - 1500.00 + Vendor A + 2026-04-01 → hash2
   - Hashes differ, correctly identifying discrepancy

### Testing Scenarios

1. **Identical Records**
   - Source and target have same data
   - Expected: 100% match

2. **Missing in Target**
   - Record exists in source, absent in target
   - Type: MISSING_IN_TARGET

3. **Extra in Target**
   - Record exists in target, absent in source
   - Type: MISSING_IN_SOURCE

4. **Null Amount Handling**
   - Records with NULL amounts
   - Compared accurately without errors

5. **Multiple Vendors**
   - Different vendors across both systems
   - Vendor-specific reconciliation

6. **Date Range Filtering**
   - Reconciliation limited to specific period
   - Useful for month-end close

### Deployment Configuration

**Docker Compose Setup:**
- Source DB: PostgreSQL on port 5432
- Target DB: PostgreSQL on port 5433
- Application: Spring Boot on port 8080
- Network: Internal Docker network for communication

**Database Initialization:**
- SQL scripts create schemas and tables
- Indexes created for performance
- Sample data loaded for testing

### Future Enhancements

1. **Enhanced Comparison**
   - Detect field-level mismatches
   - Provide detailed difference reports

2. **Pagination**
   - Handle very large result sets
   - Memory efficiency

3. **Scheduled Reconciliation**
   - Automated reconciliation runs
   - Reports and alerting

4. **Data Export**
   - CSV/Excel export of discrepancies
   - Integration with data quality tools

5. **Audit Trail**
   - Log all reconciliation runs
   - Track remediation actions

6. **Performance Metrics**
   - Execution time tracking
   - Query optimization insights

### Security Considerations

- Database credentials in environment variables
- CORS properly configured
- Input validation on all parameters
- No sensitive data in logs
- SQL injection prevention via JPA

### Monitoring and Observability

- Actuator endpoints for health checks
- Detailed logging with SLF4J
- Performance metrics
- Database connection pool monitoring

### Maintenance Notes

1. **Database Growth:** Add partitioning for tables > 1M rows
2. **Hash Collisions:** SHA-256 provides sufficient uniqueness
3. **Null Variations:** Ensure consistent null-to-"NULL" mapping
4. **Date Handling:** Use ISO 8601 format consistently
