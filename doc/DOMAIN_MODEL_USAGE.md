# Domain Model Usage Guide

## Quick Reference

### Working with the Domain Models

#### 1. Reconciliation Model
The main container for reconciliation results with business logic.

```java
// Creating (in service layer)
Reconciliation reconciliation = Reconciliation.builder("vendor-invoices")
    .executedAt(Instant.now())
    .totalSourceRecords(1000)
    .totalTargetRecords(1000)
    .matchedRecords(990)
    .mismatchedRecords(10)
    .sourceOnlyRecords(0)
    .targetOnlyRecords(0)
    .pristineCount(985)
    .noisyCount(5)
    .materialCount(0)
    .discrepancies(discrepancies)
    .build();

// Querying statistics
long totalRecords = reconciliation.getTotalSourceRecords();
double matchPercentage = reconciliation.getMatchPercentage();
boolean passed = reconciliation.isPassed(); // No material discrepancies

// Getting specific discrepancies
List<Discrepancy> material = reconciliation.getMaterialDiscrepancies();
List<Discrepancy> noisy = reconciliation.getNoisyDiscrepancies();
Map<String, Long> breakdown = reconciliation.getRecordBreakdown();
```

#### 2. Discrepancy Model
Individual discrepancies with automatic verdict calculation.

```java
// Creating a discrepancy
Discrepancy discrepancy = new Discrepancy(
    Discrepancy.Type.HASH_MISMATCH,
    sourceRecord,
    targetRecord,
    "Record hash differs",
    fieldVariances
);

// Checking verdict (automatically calculated)
switch (discrepancy.getVerdict()) {
    case PRISTINE -> System.out.println("Exact match");
    case NOISY -> System.out.println("Minor differences");
    case MATERIAL -> System.out.println("Significant differences");
}

// Getting discrepancy details
Discrepancy.Type type = discrepancy.getType();
ReconciliationRecord source = discrepancy.getSourceRecord();
ReconciliationRecord target = discrepancy.getTargetRecord();
List<FieldVariance> variances = discrepancy.getFieldVariances();

// Business logic queries
boolean isMaterial = discrepancy.isMaterial();
long excessiveFields = discrepancy.getExcessiveVarianceCount();
Optional<Double> maxVariance = discrepancy.getMaxVariance();
```

#### 3. ReconciliationRecord Model
Single record from source or target database.

```java
// Creating a record
ReconciliationRecord record = new ReconciliationRecord(
    UUID.randomUUID(),
    "abc123hash",
    "source",
    Map.of(
        "vendor_id", 123,
        "amount", 1000.50,
        "date", "2024-01-15"
    )
);

// Accessing fields
Object vendorId = record.getField("vendor_id");
Set<String> fieldNames = record.getFieldNames();
boolean hasAmount = record.hasField("amount");

// Immutable copy with different fields
ReconciliationRecord updated = record.withFields(
    Map.of("amount", 1050.50)
);
```

#### 4. ReconciliationMapper
Converting between beans, models, and DTOs.

```java
// Engine result → Domain Model
Reconciliation model = ReconciliationMapper.engineResultToDomain(
    "vendor-invoices",
    100, 100, 95, 5, 0, 0,
    90, 5, 0,
    engineResultDTOs
);

// Domain Model → API DTO
ReconciliationResultDTO resultDTO = ReconciliationMapper.toDTO(model);

// API DTO → Domain Model (for incoming requests if needed)
ReconciliationRecord domainRecord = ReconciliationMapper.toDomain(recordDTO);
```

## Common Scenarios

### 1. Processing Reconciliation Results

```java
// Service layer receives engine result
SparkReconciliationEngine.ReconciliationResult engineResult = engine.reconcile();

// Convert to domain
Reconciliation reconciliation = ReconciliationMapper.engineResultToDomain(
    domainName,
    engineResult.totalSource,
    engineResult.totalTarget,
    engineResult.matchedCount,
    engineResult.mismatchedCount,
    engineResult.sourceOnlyCount,
    engineResult.targetOnlyCount,
    engineResult.pristineCount,
    engineResult.noisyCount,
    engineResult.materialCount,
    engineResult.discrepancies
);

// Check if acceptable
if (reconciliation.getMatchPercentage() >= 99.0) {
    logger.info("Reconciliation passed SLA");
}

// Process material issues
for (Discrepancy disc : reconciliation.getMaterialDiscrepancies()) {
    handleMaterialDiscrepancy(disc);
}

// Return DTO for API
return ReconciliationMapper.toDTO(reconciliation);
```

### 2. Adding Custom Business Logic

```java
// Extend with business methods in the model or create a service
public class ReconciliationAnalyzer {
    
    public boolean requiresApproval(Reconciliation recon) {
        return recon.getMaterialCount() > 0 || 
               recon.getMatchPercentage() < 95.0;
    }
    
    public String getSummary(Reconciliation recon) {
        return String.format(
            "Reconciliation: %d/%d matched (%.1f%%), " +
            "%d pristine, %d noisy, %d material",
            recon.getMatchedRecords(),
            Math.max(recon.getTotalSourceRecords(), recon.getTotalTargetRecords()),
            recon.getMatchPercentage(),
            recon.getPristineCount(),
            recon.getNoisyCount(),
            recon.getMaterialCount()
        );
    }
}
```

### 3. Testing Business Logic

```java
@Test
public void testReconciliationPassed() {
    Reconciliation recon = Reconciliation.builder("test")
        .materialCount(0)
        .build();
    
    assertTrue(recon.isPassed());
}

@Test
public void testDiscrepancyVerdict() {
    List<FieldVariance> variances = List.of(
        new FieldVariance("amount", 100, 101, 1.0, "NUMERIC")
    );
    
    Discrepancy disc = new Discrepancy(
        Discrepancy.Type.HASH_MISMATCH,
        sourceRecord,
        targetRecord,
        "Test",
        variances
    );
    
    assertEquals(DiscrepancyVerdict.NOISY, disc.getVerdict());
}
```

### 4. Filtering and Analyzing Discrepancies

```java
Reconciliation recon = mapper.engineResultToDomain(...);

// Get specific types
List<Discrepancy> missing = recon.getDiscrepanciesByType(
    Discrepancy.Type.MISSING_IN_TARGET
);

// Get by verdict
List<Discrepancy> materialsOnly = recon.getMaterialDiscrepancies();

// Get statistics
Map<DiscrepancyVerdict, Long> breakdown = recon.getVerdictBreakdown();
Map<String, Long> recordBreakdown = recon.getRecordBreakdown();

// Custom filtering
List<Discrepancy> highVariance = recon.getDiscrepancies().stream()
    .filter(d -> d.getMaxVariance()
        .map(v -> v > 10.0)
        .orElse(false))
    .collect(Collectors.toList());
```

## Key Design Principles

### 1. **Immutability**
Models are immutable after construction. Use builders for construction.
```java
// ✅ Correct
Reconciliation recon = Reconciliation.builder("domain")
    .totalSourceRecords(100)
    .build();

// ❌ Wrong - no setters on immutable models
// recon.setTotalSourceRecords(200);
```

### 2. **Type Safety**
Use enums instead of strings for classifications.
```java
// ✅ Correct
Discrepancy.Type.HASH_MISMATCH

// ❌ Avoid
"HASH_MISMATCH" // String comparison error-prone
```

### 3. **Unidirectional Flow**
Data flows from engine → models → DTOs.
```
Engine → DiscrepancyDTO → Discrepancy → DiscrepancyDTO → API Response
```

### 4. **Single Responsibility**
- Models: Encapsulate data and business logic
- Mappers: Handle conversions  
- Service: Orchestrate workflow
- Controller: Handle HTTP concerns

## Next Steps (Optional Future Enhancements)

1. **Add validation** in model constructors
   ```java
   public Reconciliation(/* params */) {
       Objects.requireNonNull(domain);
       if (totalSourceRecords < 0) throw new IllegalArgumentException();
   }
   ```

2. **Add domain events**
   ```java
   public List<DomainEvent> getEvents() {
       return List.of(new ReconciliationCompleted(this));
   }
   ```

3. **Add to factory pattern** for complex creation
   ```java
   ReconciliationFactory.createFromEngine(engineResult, config)
   ```

4. **Add repository** for persistence
   ```java
   reconciliationRepository.save(reconciliation);
   ```
