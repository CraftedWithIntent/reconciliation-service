# Architecture Overview: DTO vs Model Separation

## Directory Structure

```
src/main/java/com/reconcile/
├── controller/
│   └── ReconciliationController.java       # HTTP endpoints
│       └── Returns: ReconciliationResultDTO
│
├── service/
│   └── ReconciliationService.java          # Business orchestration
│       ├── Calls: SparkReconciliationEngine
│       ├── Uses: ReconciliationMapper
│       └── Returns: ReconciliationResultDTO
│
├── engine/
│   └── SparkReconciliationEngine.java      # Data processing
│       ├── Calls: Spark SQL
│       ├── Creates: DiscrepancyDTO, ReconciliationRecordDTO
│       └── Returns: ReconciliationResult (internal class)
│
├── model/                                  # ✨ NEW: Domain Models
│   ├── Reconciliation.java                 # Main domain model
│   ├── Discrepancy.java                    # Discrepancy domain model
│   ├── ReconciliationRecord.java           # Record domain model
│   └── ReconciliationMapper.java           # DTO ↔ Model conversion
│
├── dto/                                    # API Data Transfer Objects
│   ├── ReconciliationResultDTO.java        # API response
│   ├── DiscrepancyDTO.java                 # API data
│   ├── ReconciliationRecordDTO.java        # API data (record)
│   ├── FieldVariance.java                  # Variance analysis
│   └── DiscrepancyVerdict.java             # Verdict enum
│
└── config/                                 # Configuration
```

## Data Flow Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                     HTTP REQUEST                                │
│                   GET /api/reconcile                            │
└────────────────────────┬────────────────────────────────────────┘
                         │
                         ▼
        ┌────────────────────────────────────┐
        │   ReconciliationController         │
        │   (HTTP Adapter)                   │
        │                                    │
        │  - Handles web requests           │
        │  - Parses domain parameter        │
        │  - Returns JSON                   │
        └────────────┬───────────────────────┘
                     │ calls reconcile(domain)
                     ▼
        ┌─────────────────────────────────────────────┐
        │   ReconciliationService                     │
        │   (Orchestration & Conversion)              │
        │                                             │
        │  - Validates domain config                 │
        │  - Creates Spark engine                    │
        │  - Calls mapper for conversions            │
        └─────────────────┬─────────────────────────┘
                          │ engine.reconcile()
                          ▼
        ┌────────────────────────────────────────────┐
        │   SparkReconciliationEngine                │
        │   (Data Processing)                        │
        │                                            │
        │  - Reads source/target data               │
        │  - Compares records via SQL              │
        │  - Analyzes field variances              │
        │  - Returns DiscrepancyDTOs               │
        └────────────────┬─────────────────────────┘
                         │ ReconciliationResult
                         │ (DiscrepancyDTO list)
                         ▼
        ┌──────────────────────────────────────────────────┐
        │   ReconciliationMapper                          │
        │   (Conversion: Engine DTO → Domain Model)       │
        │                                                  │
        │  engineResultToDomain()                         │
        │    - Converts engine DTOs to Discrepancy       │
        │    - Wraps in Reconciliation model             │
        │    - Adds business logic                       │
        └────────────────┬─────────────────────────────────┘
                         │ Reconciliation model
                         ▼
        ┌──────────────────────────────────────────────────┐
        │   Domain Model Layer                            │
        │   (Business Logic & State)                      │
        │                                                  │
        │  • Reconciliation                              │
        │    - Record statistics                         │
        │    - Business methods (isPassed, etc)         │
        │  • Discrepancy                                 │
        │    - Verdict determination                     │
        │    - Variance analysis                         │
        │  • ReconciliationRecord                         │
        │    - Field access                              │
        │    - Immutable record data                      │
        └────────────────┬─────────────────────────────────┘
                         │ toDTO(reconciliation)
                         ▼
        ┌──────────────────────────────────────────────────┐
        │   ReconciliationMapper                          │
        │   (Conversion: Domain Model → API DTO)         │
        │                                                  │
        │  toDTO(reconciliation)                          │
        │    - Converts models to DTOs                    │
        │    - Prepares for serialization                 │
        └────────────────┬─────────────────────────────────┘
                         │ ReconciliationResultDTO
                         ▼
        ┌──────────────────────────────────────────────────┐
        │   DTO Layer                                      │
        │   (API Contracts)                               │
        │                                                  │
        │  • ReconciliationResultDTO                     │
        │  • DiscrepancyDTO                              │
        │  • ReconciliationRecordDTO                      │
        │  • FieldVariance                               │
        └────────────────┬─────────────────────────────────┘
                         │ Spring serializes to JSON
                         ▼
┌─────────────────────────────────────────────────────┐
│          HTTP RESPONSE (JSON)                       │
│                                                     │
│  {                                                 │
│    "totalSourceRecords": 1000,                     │
│    "totalTargetRecords": 1000,                     │
│    "matchedRecords": 990,                          │
│    "matchPercentage": 99.0,                        │
│    "discrepancies": [...]                          │
│  }                                                 │
└─────────────────────────────────────────────────────┘
```

## Layered Architecture

```
┌─────────────────────────────────────────────────────┐
│                 PRESENTATION                        │
│  ┌────────────────────────────────────────────────┐ │
│  │  ReconciliationController                      │ │
│  │  - HTTP GET /api/reconcile                     │ │
│  │  - Converts 'domain' param to call            │ │
│  │  - Returns ReconciliationResultDTO            │ │
│  └────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────┘
                         ↓
┌─────────────────────────────────────────────────────┐
│              APPLICATION/SERVICE                    │
│  ┌────────────────────────────────────────────────┐ │
│  │  ReconciliationService                         │ │
│  │  - Orchestrates engine execution               │ │
│  │  - Handles errors and validation              │ │
│  │  - Uses ReconciliationMapper for conversion   │ │
│  │  - Returns DTOs to controller                 │ │
│  └────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────┘
                         ↓
┌─────────────────────────────────────────────────────┐
│                  DOMAIN MODEL                       │
│  ┌────────────────────────────────────────────────┐ │
│  │  BusinessLogic:                                │ │
│  │  • Reconciliation model                       │ │
│  │  • Discrepancy with verdict logic            │ │
│  │  • ReconciliationRecord with field ops       │ │
│  │                                                │ │
│  │  Conversion:                                   │ │
│  │  • ReconciliationMapper                       │ │
│  │    - Engine DTO ↔ Domain Model               │ │
│  │    - Domain Model ↔ API DTO                  │ │
│  └────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────┘
                         ↓
┌─────────────────────────────────────────────────────┐
│              DATA/INTEGRATION                       │
│  ┌────────────────────────────────────────────────┐ │
│  │  SparkReconciliationEngine                     │ │
│  │  - Reads from source/target databases        │ │
│  │  - Performs reconciliation logic             │ │
│  │  - Creates DTOs (internal transfer)          │ │
│  │  - Returns ReconciliationResult              │ │
│  └────────────────────────────────────────────────┘ │
│                                                     │
│  ┌────────────────────────────────────────────────┐ │
│  │  Databases                                     │ │
│  │  - Source DB (PostgreSQL, Oracle, etc)       │ │
│  │  - Target DB (PostgreSQL, Oracle, etc)       │ │
│  └────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────┘
```

## Class Relationships

```
ReconciliationController
        │
        ├─ uses ─► ReconciliationService
        │                   │
        │                   ├─ uses ─► SparkReconciliationEngine
        │                   │                   │
        │                   │                   ├─ creates ─► DiscrepancyDTO
        │                   │                   ├─ creates ─► ReconciliationRecordDTO
        │                   │                   └─ returns ─► ReconciliationResult
        │                   │
        │                   └─ uses ─► ReconciliationMapper
        │                                       │
        │                                       ├─ engineResultToDomain()
        │                                       │   • DiscrepancyDTO ─► Discrepancy
        │                                       │   • Wraps in ─► Reconciliation
        │                                       │
        │                                       └─ toDTO()
        │                                           • Reconciliation ─► ReconciliationResultDTO
        │                                           • Discrepancy ─► DiscrepancyDTO
        │                                           • ReconciliationRecord ─► ReconciliationRecordDTO
        │
        └─ returns ─► ReconciliationResultDTO (JSON serialized)
```

## Key Design Decisions

### 1. **Mapper as Central Hub**
- All conversions go through `ReconciliationMapper`
- Easier to add logging, validation, or transformation
- Single place to modify conversion logic

### 2. **Immutable Domain Models**
- Thread-safe for concurrent Spark processing
- Prevents accidental mutations
- Easier to reason about state

### 3. **Builder Pattern for Models**
- Readable construction
- Optional fields supported
- Validation can be added

### 4. **Enum-based Classifications**
- Type-safe instead of strings
- Compile-time checking
- IDE autocomplete

### 5. **Engine DTOs Remain Internal**
- Engine still creates DTOs (internal transfer objects)
- Service layer converts to real business models
- Minimal changes to existing high-performing engine

## Comparison: Before vs After

| Aspect | Before | After |
|--------|--------|-------|
| **DTO Scope** | Used everywhere | API only |
| **Domain Logic** | Scattered in engine | Encapsulated in models |
| **Verdict Determination** | Hardcoded in engine | `Discrepancy.determineVerdict()` |
| **Business Queries** | Manual aggregation | `Reconciliation.getMaterialDiscrepancies()` |
| **Testability** | Difficult | Easy unit tests on models |
| **API Changes** | Affects internal code | Mapper isolates changes |
| **Type Safety** | String-based | Enum-based |
| **Immutability** | DTOs mutable | Models immutable |
| **Code Clarity** | Mixed concerns | Clear separation |

## Benefits Summary

✅ **Clean Architecture:** Distinct layers with clear responsibilities  
✅ **Maintainability:** Easy to understand data flow and transformations  
✅ **Testability:** Business logic can be unit tested independently  
✅ **Flexibility:** API can evolve without changing internal models  
✅ **Type Safety:** Enums prevent string-based errors  
✅ **Extensibility:** Easy to add new business logic to models  
✅ **Backward Compatibility:** API contracts unchanged  
✅ **Thread-Safe:** Immutable models are concurrent-safe  

## Migration Strategy if Needed

If you decide to use domain models more extensively:

1. **Phase 1:** (Current) Introduce models, keep engine DTOs
   - Mapper handles conversion
   - Minimal risk

2. **Phase 2:** Add domain services for complex logic
   ```java
   public class DiscrepancyAnalysisService {
       public void categor</div>izeByRisk(List<Discrepancy> discs) { ... }
   }
   ```

3. **Phase 3:** Add repository for persistence (if needed)
   ```java
   public interface ReconciliationRepository {
       void save(Reconciliation recon);
   }
   ```

4. **Phase 4:** Add domain events for async processing
   ```java
   eventPublisher.publishEvent(new ReconciliationCompleted(reconciliation));
   ```
