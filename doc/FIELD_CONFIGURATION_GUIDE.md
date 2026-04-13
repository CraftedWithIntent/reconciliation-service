# Field Configuration Guide: Independent Source and Target

## Overview

The reconciliation service treats **source and target databases as completely independent**. This means:

- ✅ Field names can differ
- ✅ Data types can differ
- ✅ ID fields can differ
- ✅ Database types can differ
- ✅ Schemas and tables can be completely different

**No assumptions are made about matching field names or structures.**

---

## Key Concepts

### 1. ID Fields (JOIN Key)

The `idField` uniquely identifies records in each database. These can be completely different:

```yaml
source:
  idField: order_id        # Source primary key
target:
  idField: order_no        # Target primary key (different name!)
```

**Under the hood:**
```sql
-- Join uses different field names
ON source.order_id = target.order_no
```

### 2. Hash Fields (Comparison Fields)

The `hashFields` list defines which fields to include in the MD5 hash. **Order matters**, not names:

```yaml
source:
  hashFields:
    - customer_id         # Position 0
    - invoice_amount      # Position 1  
    - billing_date        # Position 2

target:
  hashFields:
    - cust_no             # Position 0 (compares to source[0])
    - amount              # Position 1 (compares to source[1])
    - dated               # Position 2 (compares to source[2])
```

**Comparison Rules:**
- `source[0]` vs `target[0]` → `customer_id` vs `cust_no` (customer identifier)
- `source[1]` vs `target[1]` → `invoice_amount` vs `amount` (amount)
- `source[2]` vs `target[2]` → `billing_date` vs `dated` (date)

**Names don't have to match!** Only semantic equivalence and order matter.

---

## Configuration Examples

### Example 1: Same Field Names (Simplest Case)

```yaml
vendor-invoices:
  source:
    type: postgresql
    schema: source_db
    table: invoices
    idField: invoice_id
    hashFields:
      - vendor_name
      - amount
      - date
  target:
    type: postgresql
    schema: target_db
    table: invoices
    idField: invoice_id      # Same name as source
    hashFields:
      - vendor_name          # Same name as source
      - amount               # Same name as source
      - date                 # Same name as source
```

**Result:** Fields match by name (but that's just a coincidence—names don't matter!)

---

### Example 2: Different Field Names (Heterogeneous)

```yaml
expense-reports:
  source:
    type: postgresql                # PostgreSQL
    schema: operational
    table: expenses
    idField: exp_id                 # Different ID field name
    hashFields:
      - emp_code                    # Employee identifier
      - expense_amount              # Transaction amount
      - entry_date                  # Date field
  target:
    type: oracle                    # Oracle (different database!)
    schema: financial
    table: ledger
    idField: expense_number         # Completely different name
    hashFields:
      - employee_num                # Different name, same semantic meaning
      - amount                      # Different name, same semantic meaning
      - date_recorded               # Different name, same semantic meaning
```

**Result:**
- JOIN: `source.exp_id = target.expense_number`
- Hash[0]: Compare `emp_code` (source) vs `employee_num` (target)
- Hash[1]: Compare `expense_amount` (source) vs `amount` (target)
- Hash[2]: Compare `entry_date` (source) vs `date_recorded` (target)

---

### Example 3: Extreme Heterogeneity

Source: PostgreSQL with financial schema
Target: Oracle with legacy ledger

```yaml
order-reconciliation:
  source:
    type: postgresql
    schema: ecommerce
    table: orders_v2                # Modern table
    idField: order_pk               # UUID primary key
    hashFields:
      - customer_email              # Modern: email as customer identifier
      - order_total                 # Modern: total_amount field
      - created_timestamp           # Modern: timestamp

  target:
    type: oracle
    schema: legacy
    table: ORDER_HDR                # Legacy table (uppercase!)
    idField: ORD_NUMBER             # Legacy: numeric order number
    hashFields:
      - CUST_ID                     # Legacy: customer code (integer)
      - GROSS_TOTAL                 # Legacy: different column name
      - ORDER_DT                    # Legacy: DATE datatype
```

**Result:**
- JOIN: `source.order_pk = target.ORD_NUMBER` (UUID → number coercion in the database)
- Hash[0]: `customer_email` (source) vs `CUST_ID` (target)
- Hash[1]: `order_total` (source) vs `GROSS_TOTAL` (target)
- Hash[2]: `created_timestamp` (source) vs `ORDER_DT` (target)

**Data type differences are handled automatically by Spark SQL.**

---

## Configuration Rules

### ✅ DO:

1. **Set different ID field names when needed**
   ```yaml
   source.idField: customer_id
   target.idField: cust_no
   ```

2. **Set different hash field names when needed**
   ```yaml
   source.hashFields: [customer_id, amount, date]
   target.hashFields: [cust_no, txn_amt, txn_dt]
   ```

3. **Maintain order consistency**
   ```yaml
   source.hashFields[0] semantically = target.hashFields[0]
   source.hashFields[1] semantically = target.hashFields[1]
   # Order MUST match semantic meaning
   ```

4. **Use different database types**
   ```yaml
   source: postgresql
   target: oracle  # Completely fine!
   ```

5. **Use completely different schemas**
   ```yaml
   source.schema: operational_db
   target.schema: reporting_warehouse  # No relation required!
   ```

### ❌ DON'T:

1. **Don't assume field names must match**
   ```yaml
   # ❌ WRONG: Assumes "amount" must exist in both
   # ✅ RIGHT: Configure differently per database
   source.hashFields: [amount]
   target.hashFields: [txn_amt]  # Different name is OK
   ```

2. **Don't mismatch hash field count**
   ```yaml
   # ❌ WRONG: Different counts
   source.hashFields: [a, b, c]
   target.hashFields: [x, y]     # Mismatch!
   
   # ✅ RIGHT: Same count
   source.hashFields: [a, b, c]
   target.hashFields: [x, y, z]  # Both have 3
   ```

3. **Don't shuffle hash field order**
   ```yaml
   # ❌ WRONG: Order doesn't match semantic meaning
   # Order in source: customer_id, amount, date
   # Order in target: date, amount, customer_id  # WRONG ORDER!
   
   # ✅ RIGHT: Maintain semantic order
   # Both: customer identifier, amount, date
   ```

4. **Don't assume ID field types match**
   ```yaml
   # ⚠️ RISKY: Mixing types (Spark will try to coerce)
   source.idField: order_id (UUID)
   target.idField: order_no (INT)
   
   # ✅ SAFER: Use same types
   source.idField: order_code (VARCHAR)
   target.idField: order_code (VARCHAR)
   ```

---

## Field Mapping Documentation

For complex heterogeneous reconciliations, you can optionally document field mappings in YAML comments:

```yaml
order-reconciliation:
  name: order-reconciliation
  source:
    # Source fields:
    #   order_pk → primary key
    #   cust_email → customer identifier
    #   total_amount → order total
    #   created_ts → timestamp
    idField: order_pk
    hashFields:
      - cust_email
      - total_amount
      - created_ts
  target:
    # Target fields (different names):
    #   order_number → primary key
    #   customer_id → customer identifier
    #   amount → order total
    #   date_created → timestamp
    idField: order_number
    hashFields:
      - customer_id        # Maps to source.cust_email
      - amount             # Maps to source.total_amount
      - date_created       # Maps to source.created_ts
  caseSensitive: false
```

---

## Common Patterns

### Pattern 1: Legacy to Modern System

**Source:** New modern system (PostgreSQL)
**Target:** Old legacy system (Oracle)

```yaml
inventory:
  source:
    type: postgresql
    schema: ecommerce
    table: sku_inventory
    idField: sku_code
    hashFields:
      - warehouse_location
      - quantity_on_hand
  target:
    type: oracle
    schema: legacy
    table: INV_MASTER
    idField: ITEM_NUMBER      # Different ID
    hashFields:
      - WAREHOUSE             # Different name
      - QTY_AVAILABLE         # Different name
```

### Pattern 2: Merger/Acquisition

**Source:** Company A's system
**Target:** Company B's system (after merger)

```yaml
employee-records:
  source:
    type: mysql
    schema: company_a_hr
    table: employees
    idField: employee_id
    hashFields:
      - first_name
      - last_name
      - salary
  target:
    type: postgresql
    schema: company_b_hr
    table: staff
    idField: staff_number     # Different naming convention
    hashFields:
      - fname                 # Abbreviated
      - lname                 # Abbreviated
      - annual_compensation   # Different name
```

### Pattern 3: ETL Validation

**Source:** Raw data source
**Target:** Data warehouse (after transforms)

```yaml
sales-validation:
  source:
    type: postgresql
    schema: raw_sales
    table: transactions
    idField: transaction_id
    hashFields:
      - customer_id
      - sale_amount
      - sale_date
  target:
    type: oracle
    schema: data_warehouse
    table: FACT_SALES
    idField: TRANSACTION_KEY   # Surrogate key
    hashFields:
      - CUSTOMER_DIM_ID        # Dimension key
      - AMOUNT                 # After conversions
      - DATE_DIM_ID            # After date conversion
```

---

## Troubleshooting

### Issue: "Field not found in source"

**Cause:** Field name doesn't exist in the actual table/view

**Solution:** Check the actual column name in each database:
```sql
-- Check source
SELECT column_name FROM information_schema.columns 
WHERE table_name = 'vendor_invoices';

-- Check target
SELECT column_name FROM all_tab_columns
WHERE table_name = 'INTERNAL_LEDGER';
```

Then update `domains.yml` with correct names.

### Issue: "Type mismatch in join"

**Cause:** ID field types are incompatible (e.g., UUID vs INT)

**Solution:** Ensure both `idField` values are compatible types:
```yaml
# ❌ WRONG
source.idField: order_id (UUID)
target.idField: order_no (INT)

# ✅ RIGHT
source.idField: order_code (VARCHAR)
target.idField: order_code (VARCHAR)
```

### Issue: "Hash values don't match but should"

**Cause:** 
- Field names don't correspond semantically
- Wrong hashField order
- Different data types not coercing properly

**Solution:**
1. Verify field meanings are equivalent
2. Verify order matches between source and target
3. Check data type compatibility

---

## Testing Your Configuration

After adding a new domain, test with:

```bash
# Run reconciliation for your domain
curl "http://localhost:8080/api/reconcile?domain=your-domain-name"

# Check response for:
# - totalSourceRecords: should be > 0
# - totalTargetRecords: should be > 0
# - matchPercentage: check if expected
# - discrepancies: review any mismatches
```

---

## Summary

**Key Takeaway:** Source and target databases are **completely independent**.

| Aspect | Can Be Different? |
|--------|-------------------|
| Database type | ✅ Yes (PostgreSQL ↔ Oracle) |
| ID field name | ✅ Yes (id vs pk_id) |
| Hash field names | ✅ Yes (amount vs txn_amt) |
| Schema names | ✅ Yes |
| Table names | ✅ Yes |
| Field order | ❌ No (must match semantically) |
| Hash field count | ❌ No (must be equal) |

**No field names need to match. Only semantic meaning and order matter.**
