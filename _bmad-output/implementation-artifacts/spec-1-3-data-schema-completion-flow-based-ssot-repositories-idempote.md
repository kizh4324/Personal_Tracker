---
title: 'Story 1.3: Data & Schema Completion, Flow-Based SSOT Repositories & Idempotency Architecture'
type: 'feature'
created: '2026-10-15'
status: 'done'
baseline_commit: 'eb2586c633d0bf63dbc6178a29fc82fbc0bcbdfa'
review_loop_iteration: 0
context:
  - ARCHITECTURE-SPINE.md
  - _bmad-output/project-context.md
  - _bmad-output/implementation-artifacts/epic-1-context.md
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** The Room database schema has `InterventionRuleEntity` co-located inside `UnfiledCaptureInboxEntity.kt`, lacks TypeConverters for `EncryptedPayload` and `Instant`/`LocalDate`, and completely lacks the pure Kotlin domain models, repository interfaces, and Flow-based Single Source of Truth (SSOT) repository implementations required by AD-1, AD-4, AD-5, and AD-6. Without these facades, UI and platform components would be forced to interact with DAOs directly or lack reactive updates and transaction boundaries.

**Approach:** Extract `InterventionRuleEntity.kt` into a standalone entity, implement and register `EncryptedPayloadConverter` and `DateConverters` on `AppDatabase`, define pure Kotlin domain models and repository interfaces in `com.personaltracker.domain` (with zero Android/Room imports), and implement Flow-based SSOT repositories in `com.personaltracker.data.repository` bound via Hilt `RepositoryModule` with atomic `{DOMAIN}_{ACTION}_{ID}` compound idempotency keys and `AppDatabase.withTransaction` on `Dispatchers.IO`.

## Boundaries & Constraints

**Always:**
- Strictly preserve AD-1: Domain models in `com.personaltracker.domain.model` and repository interfaces in `com.personaltracker.domain.repository` must be 100% pure Kotlin with zero imports from `android.*` or `androidx.room.*`.
- Strictly preserve AD-4 & AD-6: Every coin transaction must enforce compound idempotency key format `{DOMAIN}_{ACTION}_{ID}` (e.g. `TASK_COMP_<id>`, `SHOP_BUY_<id>_<time>`) with `@Insert(onConflict = OnConflictStrategy.ABORT)`.
- Multi-entity cross-domain mutations (e.g., completing task with coin rewards, buying cosmetics) must execute inside `AppDatabase.withTransaction` on `Dispatchers.IO`.
- Strictly avoid `OnConflictStrategy.REPLACE` on entities with cascading foreign keys (`HabitEntity`, `DayTypeEntity`, `RoutineEntity`, `StudySessionEntity`); use `@Upsert` or `@Insert(onConflict = ABORT)`.
- Flow emissions in repositories must map Room entities to domain models reactively.

**Ask First:**
- Any change to existing Room table names, column names, or existing primary keys.
- Any change to the 5-tab navigation destinations or existing UI theme tokens.

**Never:**
- Never import Android framework classes (`android.*`, `androidx.*`) into `com.personaltracker.domain`.
- Never expose Room DAOs or Entity classes directly to domain interfaces or presentation ViewModels.
- Never allow coin farming, duplicate idempotency keys, or unverified ledger balance mutations.
- Never use KAPT; use KSP only.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Duplicate Coin Idempotency Key | CoinTransaction with existing idempotencyKey | Insert fails atomically via `OnConflictStrategy.ABORT` without corrupting ledger | Throws `IllegalStateException` or rethrows `SQLiteConstraintException` without ledger mutation |
| Flow-Based Task Observation | Query tasks for "2026-10-15" via `TaskRepository.getTasksForDate()` | Emits updated `List<Task>` whenever Room task table changes | Emits empty list if no tasks exist |
| Standalone Intervention Rule Deletion | Delete rule for "com.distraction.app" | Rule removed; active blocked rules Flow emits updated list without deleted package | Gracefully completes if rule already absent |
| EncryptedPayload Round-Trip | `EncryptedPayload(iv, ciphertext)` passed to `EncryptedPayloadConverter` | Serialized to `ByteArray` (IV + Ciphertext) and deserialized back to identical `EncryptedPayload` | Returns null on null input or truncated bytes |
| Atomic Task Completion with Reward | Task marked completed + coin transaction created inside `withTransaction` | Task state updated to `COMPLETED`, coin transaction recorded, balance updated atomically | If coin insertion fails (e.g. duplicate key), entire transaction rolls back |

</frozen-after-approval>

## Code Map

- `app/src/main/java/com/personaltracker/data/local/db/entities/InterventionRuleEntity.kt` -- Standalone Room entity for focus intervention rules (`packageName` PK, `appDisplayName`, `isStrictBlocked`, `dailyLimitMinutes`).
- `app/src/main/java/com/personaltracker/data/local/db/entities/UnfiledCaptureInboxEntity.kt` -- Remove co-located `InterventionRuleEntity` definition (lines 20-28).
- `app/src/main/java/com/personaltracker/data/local/db/converters/EncryptedPayloadConverter.kt` -- Room `@TypeConverter` converting `EncryptedPayload` to/from `ByteArray` (12-byte IV prepended to ciphertext).
- `app/src/main/java/com/personaltracker/data/local/db/converters/DateConverters.kt` -- Room `@TypeConverter` for `Instant` and `LocalDate`.
- `app/src/main/java/com/personaltracker/data/local/db/AppDatabase.kt` -- Add `@TypeConverters(EncryptedPayloadConverter::class, DateConverters::class)` and ensure all 13 entities and 11 DAOs are properly registered.
- `app/src/main/java/com/personaltracker/data/local/db/daos/StudySessionDao.kt` -- Replace `@Insert(onConflict = OnConflictStrategy.REPLACE)` with `@Upsert` to prevent cascade deletion pitfalls.
- `app/src/main/java/com/personaltracker/domain/model/` -- Pure Kotlin models: `DayType.kt`, `DayOverride.kt`, `Task.kt`, `Routine.kt`, `RoutineStep.kt`, `Habit.kt`, `HabitLog.kt`, `StudySession.kt`, `CoinTransaction.kt`, `CompanionProfile.kt`, `ShopItem.kt`, `UnfiledInboxItem.kt`, `InterventionRule.kt`. Zero Android framework imports.
- `app/src/main/java/com/personaltracker/domain/repository/` -- Pure Kotlin interfaces: `DayTypeRepository.kt`, `TaskRepository.kt`, `RoutineRepository.kt`, `HabitRepository.kt`, `StudySessionRepository.kt`, `EconomyRepository.kt`, `UnfiledInboxRepository.kt`, `InterventionRuleRepository.kt`.
- `app/src/main/java/com/personaltracker/data/repository/` -- Repository implementations (`*RepositoryImpl.kt`) dispatching on `Dispatchers.IO`, observing Room DAOs via `Flow`, mapping to domain models, and wrapping multi-entity mutations in `AppDatabase.withTransaction`.
- `app/src/main/java/com/personaltracker/di/RepositoryModule.kt` -- Hilt DI module binding all 8 repository interfaces to their implementations as `@Singleton`.
- `app/src/test/java/com/personaltracker/data/repository/RepositoryFlowTest.kt` -- Unit tests verifying repository Flow emissions, entity-domain mapping, and Room transaction behavior.
- `app/src/test/java/com/personaltracker/data/local/db/DatabaseSchemaTest.kt` -- Update to verify standalone `InterventionRuleEntity`, TypeConverters registration, and schema completeness.

## Tasks & Acceptance

**Execution:**
- [x] `app/src/main/java/com/personaltracker/data/local/db/entities/InterventionRuleEntity.kt` -- Create standalone entity file -- Cleans up architecture and resolves entity extraction from UnfiledCaptureInboxEntity.
- [x] `app/src/main/java/com/personaltracker/data/local/db/entities/UnfiledCaptureInboxEntity.kt` -- Remove co-located InterventionRuleEntity -- Eliminates duplicate declaration.
- [x] `app/src/main/java/com/personaltracker/data/local/db/converters/EncryptedPayloadConverter.kt` -- Create Room TypeConverter for EncryptedPayload -- Enables envelope field encryption support in Room.
- [x] `app/src/main/java/com/personaltracker/data/local/db/converters/DateConverters.kt` -- Create Room TypeConverter for Date/Instant -- Provides standard date serialization.
- [x] `app/src/main/java/com/personaltracker/data/local/db/AppDatabase.kt` -- Register converters and verify 13 entities -- Fulfills AD-5 and Room schema integrity.
- [x] `app/src/main/java/com/personaltracker/data/local/db/daos/StudySessionDao.kt` -- Change REPLACE to @Upsert -- Prevents cascade delete pitfalls.
- [x] `app/src/main/java/com/personaltracker/domain/model/*.kt` -- Create 13 pure Kotlin domain models in `com.personaltracker.domain.model` -- Fulfills AD-1 and layer isolation.
- [x] `app/src/main/java/com/personaltracker/domain/repository/*.kt` -- Create 8 pure Kotlin repository interfaces in `com.personaltracker.domain.repository` -- Establishes domain contracts.
- [x] `app/src/main/java/com/personaltracker/data/repository/*.kt` -- Implement 8 repositories in `com.personaltracker.data.repository` -- Establishes Flow-based SSOT and transactional boundaries.
- [x] `app/src/main/java/com/personaltracker/di/RepositoryModule.kt` -- Create Hilt module binding repositories -- Exposes singleton repository dependencies.
- [x] `app/src/test/java/com/personaltracker/data/local/db/converters/ConvertersTest.kt` -- Test TypeConverters round-trip -- Verifies payload serialization integrity.
- [x] `app/src/test/java/com/personaltracker/data/repository/RepositoryTest.kt` -- Test repository mappings, Flow emissions, and idempotency key enforcement -- Verifies AD-1, AD-4, AD-6 invariants.

### Review Findings

- [x] [Review][Patch] Fix HabitLog unique index collision by preserving existing row ID during updates [`HabitRepositoryImpl.kt:59`]
- [x] [Review][Patch] Reject task completion on cancelled tasks and enforce positive completionTimestamp [`TaskRepositoryImpl.kt:79`]
- [x] [Review][Patch] Persist embedded routine steps when saving Routine via RoutineRepository [`RoutineRepositoryImpl.kt:34`]
- [x] [Review][Patch] Prevent integer overflow in coin balance computations with Math.addExact [`EconomyRepositoryImpl.kt:60`, `HabitRepositoryImpl.kt:82`, `TaskRepositoryImpl.kt:95`, `StudySessionRepositoryImpl.kt:65`]
- [x] [Review][Patch] Wrap Instant.toEpochMilli() in try-catch to protect against ArithmeticException on out-of-range Instant values [`DateConverters.kt:25`]
- [x] [Review][Patch] Replace deprecated OnConflictStrategy.REPLACE with @Upsert in UnfiledInboxDao [`UnfiledInboxDao.kt:20`]
- [x] [Review][Patch] Validate durationMinutes and createdTimestamp non-negative in recordSessionWithReward [`StudySessionRepositoryImpl.kt:57`]

**Acceptance Criteria:**
- Given the Room database entities, when `InterventionRuleEntity.kt` is extracted and converters are registered, then Room database builds cleanly with all 13 entities and converters.
- Given domain models and repository interfaces in `com.personaltracker.domain`, when compiled, then there are zero imports from `android.*` or `androidx.room.*`.
- Given `EconomyRepositoryImpl`, when a coin transaction with an existing compound idempotency key is inserted, then `OnConflictStrategy.ABORT` rejects the insertion and preserves ledger balance integrity.
- Given multi-entity operations in repositories, when executed, then mutations run inside `AppDatabase.withTransaction` on `Dispatchers.IO`.
- Given repository Flow methods, when database tables emit updates, then repositories emit mapped immutable domain models to observers.

## Spec Change Log

_None._

## Design Notes

### Pure Domain Layer Invariant (AD-1)
All classes in `com.personaltracker.domain` must only import `kotlin.*`, `kotlinx.coroutines.*`, and `java.time.*`. Mapping between Room entities and domain models occurs strictly within `com.personaltracker.data.repository.*Impl`.

### Idempotency Key Convention (AD-4)
All coin transactions require an `idempotencyKey` formatted as `{DOMAIN}_{ACTION}_{ID}`:
- `TASK_COMP_{taskId}`
- `ROUTINE_COMP_{routineId}_{yyyyMMdd}`
- `HABIT_LOG_{habitId}_{yyyyMMdd}`
- `STUDY_SESSION_{sessionId}`
- `SHOP_BUY_{shopItemId}_{timestamp}`

### EncryptedPayloadConverter Binary Protocol
`EncryptedPayload` consists of a 12-byte GCM IV and variable-length ciphertext (with 128-bit tag). The TypeConverter combines them into `[12 bytes IV][Ciphertext]` as a `ByteArray` BLOB.

## Verification

**Commands:**
- `.\gradlew test` -- expected: BUILD SUCCESSFUL, all unit tests pass on JVM.
- `python .agent/skills/bmad-architecture/scripts/lint_spine.py --workspace .` -- expected: `ok: true`, `total_findings: 0`.
- `git status` -- expected: clean after git commit.

## Suggested Review Order

**Schema & TypeConverters**

- Room database configuration registering converters across all 13 entities.
  [`AppDatabase.kt:58`](../../app/src/main/java/com/personaltracker/data/local/db/AppDatabase.kt#L58)

- Standalone entity extraction matching InterventionRuleDao.
  [`InterventionRuleEntity.kt:11`](../../app/src/main/java/com/personaltracker/data/local/db/entities/InterventionRuleEntity.kt#L11)

- AES-GCM 12-byte IV and 16-byte tag binary serialization converter.
  [`EncryptedPayloadConverter.kt:11`](../../app/src/main/java/com/personaltracker/data/local/db/converters/EncryptedPayloadConverter.kt#L11)

- Safe Upsert migration avoiding SQLite cascade deletion traps.
  [`StudySessionDao.kt:25`](../../app/src/main/java/com/personaltracker/data/local/db/daos/StudySessionDao.kt#L25)

**Pure Kotlin Domain Contracts (AD-1)**

- Core domain models with zero Android framework dependencies.
  [`Task.kt:8`](../../app/src/main/java/com/personaltracker/domain/model/Task.kt#L8)

- Pure reactive Flow repository contract for Task SSOT operations.
  [`TaskRepository.kt:9`](../../app/src/main/java/com/personaltracker/domain/repository/TaskRepository.kt#L9)

- Economy and coin transaction contracts with compound idempotency.
  [`EconomyRepository.kt:10`](../../app/src/main/java/com/personaltracker/domain/repository/EconomyRepository.kt#L10)

**Flow-Based Repositories & Idempotency (AD-4, AD-6)**

- Enforces compound `{DOMAIN}_{ACTION}_{ID}` key formatting on all transactions.
  [`IdempotencyValidator.kt:6`](../../app/src/main/java/com/personaltracker/data/repository/IdempotencyValidator.kt#L6)

- Atomic transaction runner delegating to Room withTransaction on Dispatchers.IO.
  [`TransactionRunner.kt:13`](../../app/src/main/java/com/personaltracker/data/repository/TransactionRunner.kt#L13)

- Atomic task completion with non-negative rewards and duplicate key rejection.
  [`TaskRepositoryImpl.kt:71`](../../app/src/main/java/com/personaltracker/data/repository/TaskRepositoryImpl.kt#L71)

- Economy ledger implementation enforcing zero-floor balance and idempotency.
  [`EconomyRepositoryImpl.kt:37`](../../app/src/main/java/com/personaltracker/data/repository/EconomyRepositoryImpl.kt#L37)

- Hilt module binding all 8 repository interfaces as singletons.
  [`RepositoryModule.kt:18`](../../app/src/main/java/com/personaltracker/di/RepositoryModule.kt#L18)

**Verification Tests**

- Round-trip serialization and edge-case boundary tests for converters.
  [`ConvertersTest.kt:12`](../../app/src/test/java/com/personaltracker/data/local/db/converters/ConvertersTest.kt#L12)

- Comprehensive unit tests verifying repository Flows, atomicity, and edge cases.
  [`RepositoryTest.kt:54`](../../app/src/test/java/com/personaltracker/data/repository/RepositoryTest.kt#L54)

- Reactive stream observation tests using CashApp Turbine.
  [`RepositoryFlowTest.kt:30`](../../app/src/test/java/com/personaltracker/data/repository/RepositoryFlowTest.kt#L30)

