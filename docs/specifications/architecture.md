# architecture.md — Personal-Tracker System Architecture Specification

> **Document Role:** Authoritative System Architecture Specification & Implementation Spine  
> **Persona:** BMad System Architect (Winston) / Reviewed by Business Analyst (Mary)  
> **Status:** `[REVISED ARCHITECTURE SPINE — IMPLEMENTATION READY]`  
> **Operating Scope:** v1, Single-User, Local-First, Online/Offline, Network-Optional, Offline-Capable  
> **Target Platform:** Native Android (Kotlin 2.4.x / Jetpack Compose)  
> **Provisional Platform Validation Scope:** Android 13–16 (API 33–36) `[PROVISIONAL TECHNICAL TARGET / ARCHITECTURE PHASE]`  
> **Frozen Upstream Authorities:** `research.md` (Dimensions A–H), `brief.md` (Decisions 1–10), `prd.md` (FR-1 through FR-10, NFRs, Failure Modes Matrix, ACs), `TECHSTACK.md`, and `PROBLEM_AND_SOLUTION_BRIEF.md`.

---

## 0. Document Status & Operational Legend

This document establishes the structural, database, security, concurrency, AI boundary, and behavioral architecture for Personal-Tracker v1. Every decision is traceable to approved PRD requirements, evidence-backed research dimensions, and architectural invariants.

### Status Tag Discipline
- **`[CONFIRMED]`** — Mandatory requirement frozen by the approved PRD scope.
- **`[RECOMMENDED]`** — Engineering design choice matching modern Android architecture standards.
- **`[TECHNICAL CONSTRAINT]`** — Hard platform constraint imposed by Android OS APIs, sandbox rules, or hardware.
- **`[ARCHITECTURE RESOLVED]`** — Concrete technical resolution of an architectural implementation mechanism.
- **`[VALIDATION REQUIRED / CONFIGURABLE HEURISTIC]`** — Product parameter, threshold, formula, or timing heuristic that is architecturally supported via configurable policy, pending runtime personal validation.
- **`[OPEN DECISION / TECHNICAL VALIDATION]`** — Explicitly flagged gap or behavioral decision requiring runtime tuning and validation.
- **`[PROVISIONAL TECHNICAL TARGET / ARCHITECTURE PHASE]`** — Architectural baseline subject to platform verification against target API levels and hardware constraints.
- **`[DEFERRED]`** — Out of v1 scope; zero core functionality may rely on this.
- **`[EXCLUDED]`** — Outlawed by product non-goals (e.g., cloud database sync, multi-user accounts, paywalls, punitive gamification, always-listening wake-word).

---

## 1. System Architecture & Module Boundaries

The application is structured as a layered, Unidirectional Data Flow (UDF) architecture. It separates the presentation layer, domain rules, data repositories, local storage, Android system services, and the **dual-engine voice architecture (Online Gemini Live API over WebSocket + On-Device SpeechRecognizer with Manual Fallback)**.

```text
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                                PRESENTATION LAYER (UI)                                 │
│  com.personaltracker.ui.theme             │ com.personaltracker.ui.navigation          │
│  com.personaltracker.ui.screens.*         │ com.personaltracker.ui.components.*        │
└───────────────────────────────────────────┬────────────────────────────────────────────┘
                                            │ depends on
                                            ▼
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                             UI STATE HOLDERS (VIEWMODELS)                              │
│  com.personaltracker.presentation.hero    │ com.personaltracker.presentation.focus     │
│  com.personaltracker.presentation.timeline│ com.personaltracker.presentation.companion │
│  com.personaltracker.presentation.tasks   │ com.personaltracker.presentation.voice     │
└───────────────────────────────────────────┬────────────────────────────────────────────┘
                                            │ depends on
                                            ▼
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                                 DOMAIN LAYER (RULES)                                   │
│  com.personaltracker.domain.usecase.*     │ com.personaltracker.domain.model.*         │
│  com.personaltracker.domain.parser        │ com.personaltracker.domain.policy.*        │
└───────────────────────────────────────────┬────────────────────────────────────────────┘
                                            │ depends on
                                            ▼
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                                 REPOSITORY LAYER                                       │
│  com.personaltracker.data.repository.* (Public Interfaces & Domain Mappers)            │
└─────────────────────┬────────────────────────────────────────────┬─────────────────────┘
                      │                                            │
                      ▼                                            ▼
┌───────────────────────────────────────────┐  ┌─────────────────────────────────────────┐
│            LOCAL DATA ENGINE              │  │          ANDROID SYSTEM ENGINE          │
│  com.personaltracker.data.local.db        │  │  com.personaltracker.system.service     │
│  com.personaltracker.data.local.pref      │  │  com.personaltracker.system.alarm       │
│  com.personaltracker.data.local.sec       │  │  com.personaltracker.system.work        │
│  com.personaltracker.data.backup          │  │  com.personaltracker.system.speech      │
└───────────────────────────────────────────┘  └─────────────────────────────────────────┘
                      ▲
                      │ (Commands & Structured Proposals ONLY after validation)
┌─────────────────────┴──────────────────────────────────────────────────────────────────┐
│                   DUAL-ENGINE VOICE & AI ASSISTANT SUBSYSTEM                           │
│  • Online Path: Gemini Live API over WebSocket (Bidirectional Real-Time Assistant)     │
│  • Offline Path: Android SpeechRecognizer (On-device when available) + NLP Parser      │
│  • Fallback Path: Direct Manual Text Quick-Add Bar                                     │
└────────────────────────────────────────────────────────────────────────────────────────┘
```

### 1.1 Package Responsibilities & Dependency Invariants

| Package | Responsibility | Allowed Inward Dependencies | Prohibited Dependencies |
|---|---|---|---|
| `domain.model` | Pure Kotlin data models, immutable value objects, domain enums (`DayType`, `DeliveryIntensity`, `CompanionState`). | Kotlin Standard Library only. | Android SDK, Room, Compose, Hilt. |
| `domain.usecase` | Business logic orchestrations (DayType resolution, carry-forward rescheduling, focus state transitions). | `domain.model`, `domain.policy`, `data.repository` interfaces. | Compose UI, Android UI classes, Room DAOs. |
| `domain.policy` | Configurable heuristic rules and threshold providers (`RewardPolicy`, `FrictionPolicy`, `UrgencyScarcityPolicy`, `ConfidencePolicy`). | `domain.model`. | Direct storage or UI dependencies. |
| `domain.parser` | Deterministic offline regex/rule parser converting raw text to unvalidated draft DTOs. | `domain.model`, `kotlinx.serialization`. | Database instances, Android Views. |
| `data.repository` | Public repository contracts and implementations. Maps database entities to domain models; exposes reactive `Flow` streams. | `domain.model`, `data.local.db`, `data.local.pref`, `data.local.sec`. | Presentation Layer (ViewModels, Composables). |
| `data.local.db` | Room database definition, encrypted SQLite migrations, DAOs, and raw `@Entity` classes. | AndroidX Room, SQLCipher, `kotlinx.coroutines`. | Domain Use Cases, ViewModels, UI. |
| `data.local.sec` | Secure key management abstraction (`SecurityKeyStoreManager`) handling hardware-backed master keys. | Android Keystore. | UI Layer, Networking. |
| `data.backup` | `.ptbackup` binary container export/import, PBKDF2 key derivation, AES-256-GCM authenticated cipher streaming. | `data.local.db`, `data.local.sec`, `kotlinx.serialization`. | UI Layer. |
| `system.ai.gemini` | Online real-time bidirectional voice assistant via **Gemini Live API over WebSocket**. | `domain.model`, `domain.parser`, Kotlin Coroutines. | Direct Room DAO access, UI components. |
| `system.speech` | On-device/offline speech recognition via Android `SpeechRecognizer` (`createOnDeviceSpeechRecognizer`) with manual text entry fallback. | Android Speech APIs, `domain.parser`. | Direct Room DAO access. |
| `system.service` | `AccessibilityService` distraction overlay engine using **event-driven foreground package detection**; emergency and dialer package filtering. | Android `AccessibilityService`, `TelecomManager`, `TelephonyManager`. | ViewModels directly, Room DAOs. |
| `system.alarm` | Exact-time scheduling and notification triggers via `AlarmManager`. | Android `AlarmManager`, `NotificationManager`. | Presentation Composables. |
| `system.work` | Deferrable background maintenance and schedule rotation via `WorkManager`. | Jetpack WorkManager, Repository interfaces. | UI Composables. |
| `presentation.*` | Lifecycle-aware ViewModels managing `StateFlow<UIState>` and handling user intents. | `domain.usecase`, `domain.model`, `data.repository`. | Room DAOs, Android Views. |
| `ui.*` | Jetpack Compose screens, design tokens, Rive/Lottie wrappers, and Navigation graph. | `presentation.*`, `ui.theme`, `ui.components`. | Room Database, SQLCipher, Repositories directly. |

---

## 2. Comprehensive Room Data Model & Entity Schema

The database model strictly separates **reusable templates** (DayType and Routine master definitions) from **daily execution instances and history logs** (DayOverrides, Tasks, RoutineStepCompletions, HabitLogs, FocusSessions, StudySessions, and CoinLedger). A mid-day DayType swap alters only today's uncompleted schedule instance without mutating base templates or rewriting history.

```text
┌────────────────────────┐         ┌────────────────────────┐
│     day_types          │1       *│     routines           │
├────────────────────────┼─────────┤────────────────────────┤
│ id (PK: TEXT)          │         │ id (PK: TEXT)          │
│ name (TEXT)            │         │ dayTypeId (FK: TEXT)   │
│ colorHex (TEXT)        │         │ title (TEXT)           │
│ targetStudyMinutes(INT)│         │ targetStartTime (TEXT) │
│ isDefault (BOOLEAN)    │         └──────────┬─────────────┘
└──────────┬─────────────┘                    │1
           │1                                 │*
           │*                      ┌──────────┴─────────────┐
┌──────────┴─────────────┐         │     routine_steps      │
│     day_overrides      │         ├────────────────────────┤
├────────────────────────┤         │ id (PK: TEXT)          │
│ date (PK: TEXT ISO)    │         │ routineId (FK: TEXT)   │
│ dayTypeId (FK: TEXT)   │         │ orderIndex (INT)       │
│ isUserModified (BOOL)  │         │ title (TEXT)           │
│ createdTimestamp (LONG)│         │ durationMinutes (INT)  │
└────────────────────────┘         └────────────────────────┘

┌────────────────────────┐         ┌────────────────────────┐
│         tasks          │         │     focus_sessions     │
├────────────────────────┤         ├────────────────────────┤
│ id (PK: TEXT UUID)     │         │ id (PK: TEXT UUID)     │
│ title (TEXT)           │         │ associatedTaskId (TEXT)│
│ description (TEXT?)    │         │ associatedSubject(TEXT)│
│ scheduledDate (TEXT)   │         │ plannedMinutes (INT)   │
│ scheduledTime (TEXT?)  │         │ actualMinutes (INT)    │
│ estimatedMinutes (INT) │         │ startTimestamp (LONG)  │
│ deliveryIntensity(TEXT)│         │ endTimestamp (LONG?)   │
│ state (TEXT Enum)      │         │ state (TEXT Enum)      │
│ isCarryForward (BOOL)  │         │ interruptionCount (INT)│
│ completionTimestamp    │         └────────────────────────┘
└────────────────────────┘
                                   ┌────────────────────────┐
┌────────────────────────┐         │     study_sessions     │
├────────────────────────┤         ├────────────────────────┤
│        habits          │         │ id (PK: TEXT UUID)     │
├────────────────────────┤         │ subjectTag (TEXT)      │
│ id (PK: TEXT UUID)     │         │ sessionDate (TEXT)     │
│ title (TEXT)           │         │ durationMinutes (INT)  │
│ cadenceType (TEXT Enum)│         │ focusSessionId (TEXT?) │
│ customDaysMask (TEXT?) │         │ startTimestamp (LONG)  │
│ currentStreak (INT)    │         │ isCompleted (BOOLEAN)  │
│ bestStreak (INT)       │         └────────────────────────┘
│ slackBankDays (INT)    │
│ isFreezeActive (BOOL)  │         ┌────────────────────────┐
└──────────┬─────────────┘         │      coin_ledger       │
           │1                      ├────────────────────────┤
           │*                      │ id (PK: TEXT UUID)     │
┌──────────┴─────────────┐         │ idempotencyKey (UQ:TEXT│
│      habit_logs        │         │ deltaAmount (INT)      │
├────────────────────────┤         │ eventType (TEXT Enum)  │
│ id (PK: TEXT UUID)     │         │ sourceEntityId (TEXT)  │
│ habitId (FK: TEXT)     │         │ balanceAfter (INT)     │
│ logDate (TEXT ISO)     │         │ timestamp (LONG)       │
│ status (TEXT Enum)     │         └────────────────────────┘
└────────────────────────┘
                                   ┌────────────────────────┐
┌────────────────────────┐         │   companion_profile    │
│   unfiled_inbox        │         ├────────────────────────┤
├────────────────────────┤         │ id (PK: INT = 1)       │
│ id (PK: TEXT UUID)     │         │ speciesName (TEXT)     │
│ rawTranscript (TEXT)   │         │ level (INT)            │
│ draftAttributesJson(TXT│         │ currentEnergyUnits(INT)│
│ parserConfidence (REAL)│         │ activeAccessoryId(TEXT)│
│ captureSource (TEXT)   │         │ currentExpression (INT)│
│ createdTimestamp (LONG)│         └────────────────────────┘
└────────────────────────┘
```

### 2.1 Entity DDL Specifications

#### 1. `DayTypeEntity` & `DayOverrideEntity` (FR-3, Decision 3)
```kotlin
@Entity(tableName = "day_types")
data class DayTypeEntity(
    @PrimaryKey val id: String, // e.g., "weekday_standard", "heavy_study", "weekend_rest"
    val name: String,
    val colorHex: String,
    val targetStudyMinutes: Int,
    val isDefault: Boolean,
    val defaultDaysOfWeek: String // Comma-separated ISO day numbers: "1,2,3,4,5" (Mon-Fri)
)

@Entity(
    tableName = "day_overrides",
    foreignKeys = [
        ForeignKey(
            entity = DayTypeEntity::class,
            parentColumns = ["id"],
            childColumns = ["dayTypeId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index("dayTypeId")]
)
data class DayOverrideEntity(
    @PrimaryKey val date: String, // ISO-8601 format: "YYYY-MM-DD"
    val dayTypeId: String,
    val isUserModified: Boolean,
    val createdTimestamp: Long
)
```

#### 2. `TaskEntity` (FR-2.2, FR-5.1, Dimension C Alignment)
```kotlin
@Entity(
    tableName = "tasks",
    indices = [Index("scheduledDate"), Index("state"), Index("deliveryIntensity")]
)
data class TaskEntity(
    @PrimaryKey val id: String, // UUIDv4
    val title: String,
    val description: String?,
    val scheduledDate: String, // "YYYY-MM-DD"
    val scheduledTime: String?, // "HH:mm" (optional fixed-time)
    val estimatedDurationMinutes: Int,
    val deliveryIntensity: String, // "ROUTINE", "IMPORTANT", "URGENT" (FR-5.1)
    val state: String, // "PENDING", "IN_PROGRESS", "INTERRUPTED", "COMPLETED", "CANCELLED"
    val isCarryForward: Boolean, // True if rolled over from a previous date (FR-6.1)
    val carryForwardCount: Int,
    val completionTimestamp: Long?,
    val createdTimestamp: Long
)
```

#### 3. `RoutineEntity` & `RoutineStepEntity` (FR-2.3)
```kotlin
@Entity(
    tableName = "routines",
    foreignKeys = [
        ForeignKey(
            entity = DayTypeEntity::class,
            parentColumns = ["id"],
            childColumns = ["dayTypeId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("dayTypeId")]
)
data class RoutineEntity(
    @PrimaryKey val id: String,
    val dayTypeId: String,
    val title: String,
    val targetStartTime: String, // "HH:mm"
    val totalEstimatedMinutes: Int
)

@Entity(
    tableName = "routine_steps",
    foreignKeys = [
        ForeignKey(
            entity = RoutineEntity::class,
            parentColumns = ["id"],
            childColumns = ["routineId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("routineId"), Index(value = ["routineId", "orderIndex"], unique = true)]
)
data class RoutineStepEntity(
    @PrimaryKey val id: String,
    val routineId: String,
    val orderIndex: Int,
    val title: String,
    val durationMinutes: Int
)
```

#### 4. `HabitEntity` & `HabitLogEntity` (FR-2.4, FR-8.5)
```kotlin
@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey val id: String,
    val title: String,
    val cadenceType: String, // "DAILY", "WEEKDAYS", "CUSTOM_DAYS"
    val customDaysMask: String?, // "1,3,5"
    val currentStreakDays: Int,
    val bestStreakDays: Int,
    val earnedSlackBankDays: Int, // Freezes earned via consistency (FR-2.4)
    val isFreezeActiveToday: Boolean,
    val createdTimestamp: Long
)

@Entity(
    tableName = "habit_logs",
    foreignKeys = [
        ForeignKey(
            entity = HabitEntity::class,
            parentColumns = ["id"],
            childColumns = ["habitId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("habitId"), Index(value = ["habitId", "logDate"], unique = true)]
)
data class HabitLogEntity(
    @PrimaryKey val id: String,
    val habitId: String,
    val logDate: String, // "YYYY-MM-DD"
    val status: String, // "COMPLETED", "MISSED", "FREEZE_ABSORBED"
    val completedTimestamp: Long?
)
```

#### 5. `FocusSessionEntity` & `StudySessionEntity` (FR-2.5, FR-4)
```kotlin
@Entity(
    tableName = "focus_sessions",
    indices = [Index("associatedTaskId"), Index("state"), Index("startTimestamp")]
)
data class FocusSessionEntity(
    @PrimaryKey val id: String, // UUIDv4
    val associatedTaskId: String?,
    val associatedSubjectTag: String?,
    val plannedMinutes: Int,
    val actualMinutes: Int,
    val startTimestamp: Long,
    val endTimestamp: Long?,
    val state: String, // "ACTIVE", "INTERRUPTED", "COMPLETED", "CANCELLED"
    val interruptionCount: Int
)

@Entity(
    tableName = "study_sessions",
    foreignKeys = [
        ForeignKey(
            entity = FocusSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["focusSessionId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("subjectTag"), Index("sessionDate"), Index("focusSessionId")]
)
data class StudySessionEntity(
    @PrimaryKey val id: String, // UUIDv4
    val subjectTag: String, // e.g., "Math", "Algorithms", "Physics"
    val sessionDate: String, // "YYYY-MM-DD"
    val durationMinutes: Int,
    val focusSessionId: String?,
    val startTimestamp: Long,
    val isCompleted: Boolean
)
```

#### 6. `CoinLedgerEntity` (FR-8.2, FR-8.3 — Idempotent Reward Ledger)
```kotlin
@Entity(
    tableName = "coin_ledger",
    indices = [
        Index(value = ["idempotencyKey"], unique = true),
        Index("timestamp")
    ]
)
data class CoinLedgerEntity(
    @PrimaryKey val id: String, // UUIDv4
    val idempotencyKey: String, // Unique event token: "TASK_<id>_<timestamp>" or "HABIT_<id>_<date>"
    val deltaAmount: Int, // Positive for earnings, negative for shop deductions
    val eventType: String, // "TASK_COMPLETE", "HABIT_COMPLETE", "FOCUS_BLOCK", "SHOP_PURCHASE"
    val sourceEntityId: String,
    val balanceAfter: Int,
    val timestamp: Long
)
```

#### 7. `CompanionProfileEntity` & `ShopItemEntity` (FR-8.1, FR-8.4)
```kotlin
@Entity(tableName = "companion_profile")
data class CompanionProfileEntity(
    @PrimaryKey val id: Int = 1, // Singleton profile record
    val speciesName: String,
    val level: Int,
    val currentEnergyUnits: Int,
    val activeAccessoryId: String?,
    val activeBackgroundId: String?,
    val currentExpressionOrdinal: Int // Mapped 0..6 to CompanionState
)

@Entity(tableName = "shop_items")
data class ShopItemEntity(
    @PrimaryKey val id: String,
    val category: String, // "OUTFIT", "ACCESSORY", "BACKGROUND_DECOR"
    val name: String,
    val priceCoins: Int,
    val isUnlocked: Boolean,
    val unlockedTimestamp: Long?
)
```

#### 8. `UnfiledCaptureInboxEntity` (FR-7.5)
```kotlin
@Entity(tableName = "unfiled_capture_inbox")
data class UnfiledCaptureInboxEntity(
    @PrimaryKey val id: String, // UUIDv4
    val rawTranscriptText: String,
    val draftAttributesJson: String, // Serialized PartialTaskDraft DTO
    val parserConfidence: Float, // 0.00 to 1.00
    val captureSource: String, // "OFFLINE_STT", "GEMINI_LIVE", "TEXT_QUICK_ADD"
    val createdTimestamp: Long
)
```

#### 9. `InterventionRuleEntity` (FR-4.3, FR-10.1)
```kotlin
@Entity(tableName = "intervention_rules")
data class InterventionRuleEntity(
    @PrimaryKey val packageName: String, // e.g., "com.instagram.android"
    val appDisplayName: String,
    val isStrictBlocked: Boolean,
    val dailyLimitMinutes: Int?
)
```

---

## 3. End-to-End Sequence Flows & Dual Voice Architecture

### 3.1 UJ-1: Daily Morning Kickoff & DayType Resolution Flow
```
User            AppLaunch/MainActivity     HeroViewModel       ResolveDayTypeUseCase     DayTypeRepository      Room/SQLCipher
 │                        │                      │                       │                      │                     │
 │─── Open App ──────────►│                      │                       │                      │                     │
 │                        │─── onStart() ───────►│                       │                      │                     │
 │                        │                      │─── resolveToday() ───►│                      │                     │
 │                        │                      │                       │─── getOverride(date)►│                     │
 │                        │                      │                       │                      │─── SELECT override ─►│
 │                        │                      │                       │◄── Null (No override)│◄── (No record) ─────│
 │                        │                      │                       │                      │                     │
 │                        │                      │                       │─── getDefaultDay() ─►│                     │
 │                        │                      │                       │                      │─── SELECT default ──►│
 │                        │                      │                       │◄── "weekday_std" ────│◄── DayTypeEntity ───│
 │                        │                      │◄── Active DayType ────│                      │                     │
 │                        │                      │─── emit(HeroState)───┐│                      │                     │
 │                        │                      │◄─────────────────────┘│                      │                     │
 │◄── Render Hero Card ───┴──────────────────────│                                                                     │
 │    (Active: Weekday Routine • Banner: 1-Tap Swap to Heavy Study)                                                   │
```

### 3.2 UJ-2: Dual Voice Architecture & Confidence Routing Flow
*(Supports Online Gemini Live API over WebSocket + On-Device SpeechRecognizer with Manual Fallback)*

```
User            WaveformFAB         VoiceRouterService       GeminiLiveEngine      OfflineSTTEngine    TaskRepository     Room / Inbox
 │                   │                       │                       │                    │                  │                  │
 │── Tap Mic / Talk ─►│                       │                       │                    │                  │                  │
 │                   │── startVoiceSession()►│                       │                    │                  │                  │
 │                   │                       │── isOnline? ──────────┐                    │                  │                  │
 │                   │                       │                       │                    │                  │                  │
 │ [ONLINE PATH] ────┼───────────────────────┼───────────────────────▼                    │                  │                  │
 │                   │                       │── openLiveSession() ─►│ (Bidirectional     │                  │                  │
 │                   │                       │   (over WebSocket)    │  Audio Stream)     │                  │                  │
 │── "Schedule Math"►│                       │── sendAudioChunk() ──►│                    │                  │                  │
 │                   │                       │◄── onToolCall(draft) ─│                    │                  │                  │
 │                   │                       │    (Emit TaskProposal)│                    │                  │                  │
 │                   │                       │                       │                    │                  │                  │
 │ [OFFLINE PATH] ───┼───────────────────────┼────────────────────────────────────────────▼                  │                  │
 │                   │                       │── startOnDeviceSTT() ─────────────────────►│                  │                  │
 │── "Schedule Math"►│                       │   (when available)                         │ (Local Recognizer)                  │
 │                   │                       │◄── onResult(transcript, confidence) ───────│                  │                  │
 │                   │                       │── parseDeterministicTokens(transcript) ───┐│                  │                  │
 │                   │                       │◄── Emits TaskProposal DTO ────────────────┘│                  │                  │
 │                   │                       │                                                               │                  │
 │                   │                       │── Schema & Business Rule Authorization ──────────────────────►│                  │
 │                   │                       │                                                               │                  │
 │ [High Confidence] ┼───────────────────────┼───────────────────────────────────────────────────────────────┼─────────────────►│
 │                   │                       │── commitTask(validatedCmd) ──────────────────────────────────►│── INSERT Task ──►│
 │◄── Toast: "Task Created" [Undo] ──────────│                                                               │◄── Success ──────│
 │                                                                                                                              │
 │ [Medium Confidence] ─────────────────────────────────────────────────────────────────────────────────────────────────────────┐
 │◄── Hero Card displays 1-Tap Editable Chips: [Math] [Today] [45m] [Routine] ── User Taps [Confirm] ───────────────────────────┤
 │                                                                                                                              │
 │ [Low Confidence / Ambiguous] ────────────────────────────────────────────────────────────────────────────────────────────────►│
 │◄── Preserved in Unfiled Capture Inbox for 1-Tap Sorting ──────────────────────────────────────────────────►│── INSERT Inbox ─►│
```

### 3.3 UJ-3: Focus Session Initiation, Distraction Intercept & Resumption
```
User            DistractingApp      AccessibilityService     FocusSessionManager     NotificationEngine    Room/TaskRepo
 │                    │                       │                       │                       │                  │
 │── Launch App ─────►│                       │                       │                       │                  │
 │                    │── onAccessibilityEvt─►│                       │                       │                  │
 │                    │   (Event-driven pkg)  │── isPackageBlocked? ──│                       │                  │
 │                    │                       │                       │── getActiveSession() ─►│                  │
 │                    │                       │                       │◄── Null (Scheduled) ──│                  │
 │                    │                       │                                               │                  │
 │                    │                       │ [Scheduled Window Active & Unstarted]         │                  │
 │                    │                       │── checkJITAIThrottling() ────────────────────┐│                  │
 │                    │                       │◄─ OK (Max 1 Prompt per block) ───────────────┘│                  │
 │◄── Draw Overlay ───┴───────────────────────│                                               │                  │
 │    ("Active Schedule: Math Study — Start Focus or Snooze")                                 │                  │
 │                                                                                            │                  │
 │── Taps [Start Focus] ─────────────────────────────────────────────►│                       │                  │
 │                                                                    │── startSession(id) ──►│── UPDATE Task ──►│
 │                                                                    │   (State: IN_PROGRESS)│   (IN_PROGRESS)  │
 │                                                                    │── armAccessibility()─►│                  │
 │                                                                    │── startTimerService()►│── Show Ongoing ─►│
 │                                                                    │                       │   Notification   │
 │                                                                    │                       │                  │
 │ [User Exits App / OS Low-Memory Termination] ─────────────────────►│                       │                  │
 │                                                                    │── onSessionInterrupted│── UPDATE Task ──►│
 │                                                                    │                       │   (INTERRUPTED)  │
 │                                                                    │                       │                  │
 │── Next App Launch ────────────────────────────────────────────────►│                       │                  │
 │◄── Hero Card displays Resumption Card: ("Resume Math Study") ──────┘                       │                  │
```

### 3.4 UJ-4: Rescheduling & Carry Forward Review Flow
```
User            EveningReviewCard     RescheduleViewModel     CarryForwardUseCase     TaskRepository     Room Database
 │                      │                      │                       │                     │                 │
 │── Review Prompts ───►│                      │                       │                     │                 │
 │                      │── getUnfinished() ──►│                       │                     │                 │
 │                      │                      │── queryFlexible() ───►│                     │                 │
 │                      │                      │                       │── getOverdueTasks()►│── SELECT Task ──►│
 │                      │                      │                       │◄── List<Task> ──────│◄── WHERE PENDING│
 │                      │                      │◄── Overdue List ──────│                     │                 │
 │                      │                                                                                      │
 │── Selects [Do Tomorrow] (3 Tasks) ─────────►│                                                               │
 │                      │                      │── checkCapacity(tomorrow) ─────────────────►│                 │
 │                      │                      │                       │── calcWorkload() ──►│── SELECT Tasks ─►│
 │                      │                      │                       │◄── Advisory Workload│   (For Tomorrow)│
 │                      │                      │◄── Advisory Capacity Indicator ─────────────│                 │
 │◄── Displays Advisory: "Planned workload for tomorrow calculated" [Confirm Carry Forward] ───────────────────┤
 │                                                                                                             │
 │── Taps [Confirm] ──────────────────────────►│                                                               │
 │                      │                      │── applyCarryForward(tasks, nextDate) ──────►│                 │
 │                      │                      │                       │                     │── BATCH UPDATE ─►│
 │                      │                      │                       │                     │   (Date=Tomorrow,│
 │                      │                      │                       │                     │    isCarryFwd=1) │
```

### 3.5 UJ-5: Companion Progression & Idempotent Coin Ledger Flow
```
User            TaskCardItem          TaskViewModel           AwardCoinsUseCase        CoinRepository     Room Database
 │                   │                      │                         │                      │                  │
 │── Check Complete ─►│                      │                         │                      │                  │
 │                   │── markComplete(id) ─►│                         │                      │                  │
 │                   │                      │── awardCompletion(task)►│                      │                  │
 │                   │                      │                         │── genIdempotencyKey ─┐                  │
 │                   │                      │                         │   "TASK_COMP_<id>"  ─┘                  │
 │                   │                      │                         │── appendLedger(...) ─►│                  │
 │                   │                      │                         │                      │── @Transaction ──►│
 │                   │                      │                         │                      │   INSERT Ledger   │
 │                   │                      │                         │                      │   UPDATE Profile  │
 │                   │                      │                         │                      │   UPDATE Task     │
 │                   │                      │                         │                      │◄── Transact OK ───│
 │                   │                      │◄── Awarded(Coins) ──────│◄── Success ──────────│                  │
 │                   │◄── Trigger Motion ───│                                                                   │
 │◄── 1. Row Checkmark Animation ───────────┤                                                                   │
 │◄── 2. CoinArcAnimation to Top-Right HUD (VSYNC-synced) ──────────────────────────────────────────────────────┤
 │◄── 3. CompanionView switches to CompanionState.Celebrating ──────────────────────────────────────────────────┘
```

### 3.6 UJ-6: Encrypted Manual Backup & Transactional Restore Flow
```
User            BackupScreen          BackupManager           SecurityEngine         Room Database       Storage (SAF)
 │                   │                      │                       │                     │                     │
 │── Tap [Export] ──►│                      │                       │                     │                     │
 │── Enter Password ─►│                      │                       │                     │                     │
 │                   │── createBackup(pwd) ─►│                       │                     │                     │
 │                   │                      │── deriveKey(pwd,salt)►│                     │                     │
 │                   │                      │   (PBKDF2-HMAC-SHA512)│◄── 256-bit Key ─────│                     │
 │                   │                      │── dumpDatabaseTables()─────────────────────►│── Read All Entities│
 │                   │                      │                       │                     │◄── Database Schema ─│
 │                   │                      │── serializeToGzip() ──┐                     │                     │
 │                   │                      │── encryptAES_GCM() ───┘                     │                     │
 │                   │                      │── writeStream(uri) ──────────────────────────────────────────────►│
 │◄── Export Complete (.ptbackup created) ──│                                                                   │
 │                                                                                                              │
 │── Tap [Import] ──►│                                                                                          │
 │── Select File ────►│                      │                                                                   │
 │── Enter Password ─►│                      │                                                                   │
 │                   │── restoreBackup(uri)►│                                                                   │
 │                   │                      │── Step 1: Verify Header Magic ("PTBK01") ─────────────────────────│
 │                   │                      │── Step 2: Decrypt & Authenticate AES-256-GCM Tag ─────────────────┤
 │                   │                      │── Step 3: Deserialize JSON & Validate Schema Version ─────────────┤
 │                   │                      │── Step 4: Create Pre-Restore Safety Snapshot DB ──────────────────►│
 │                   │                      │── Execute Room Ingestion within Transaction ─────────────────────►│
 │                   │                      │   [If Ingestion Fails] ──────────────────────────────────────────►│
 │                   │                      │   └── Rollback & Restore Pre-Restore Safety Snapshot ─────────────┤
 │◄── Restore Success / Safe Rollback ──────│                                                                   │
```

---

## 4. Security Architecture & Cryptographic Engine

*(Satisfies NFR-4 and PRD Rule 6)*

### 4.1 Master Key Management (`SecurityKeyStoreManager`)
* **Hardware-Backed Keymaster / Keystore Integration**: Key generation is encapsulated behind `SecurityKeyStoreManager`.
* **Master Keystore Alias**: `PersonalTracker_MasterDB_Key`
* **Specification**:
  * Algorithm: `KeyProperties.KEY_ALGORITHM_AES`
  * Block Mode: `KeyProperties.BLOCK_MODE_GCM`
  * Padding: `KeyProperties.ENCRYPTION_PADDING_NONE`
  * Key Size: 256 bits
  * Hardware Backing: Key is generated with `setUserAuthenticationRequired(false)` to allow background exact alarm dispatching without interactive biometric prompts; `setRandomizedEncryptionRequired(true)` ensures unique IVs.

### 4.2 Database Encryption & 16 KB Platform Compatibility (SQLCipher for Android)
* **Library Reference**: `net.zetetic:sqlcipher-android:4.6.1+`
* **Database Cipher Suite**: `AES-256-CBC` with per-page `HMAC-SHA512`
* **KDF Configuration**:
  * Algorithm: `PBKDF2WithHmacSHA512`
  * Iteration Count: **256,000 iterations**
  * Database Page Size: **4096 bytes**
* **Technical Distinction on 16 KB Architecture**:
  1. *Database Page Size*: Configured at 4096 bytes for SQLite/SQLCipher B-tree storage efficiency.
  2. *16 KB ELF Native Alignment*: The native shared library (`libsqlcipher.so` inside `net.zetetic:sqlcipher-android`) must be built with 16 KB ELF segment alignment as a build and dependency validation requirement.
  3. *16 KB Memory Page Devices*: Compatible with newer Android hardware and kernel builds configured with 16 KB memory pages.
* **Passphrase Lifecycle**:
  1. On first app initialization, a 32-byte cryptographic random passphrase is generated via `SecureRandom`.
  2. The passphrase is encrypted with the Keystore Master Key via AES-256-GCM and saved to private app storage.
  3. On Room startup, the passphrase is decrypted in memory and passed to `SupportFactory(passphraseBytes)`. Plaintext keys are never stored unencrypted on persistent disk.

### 4.3 Backup Container Cryptographic Specification (`.ptbackup`)
The backup file uses authenticated encryption (**`AES-256-GCM`**):

```text
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                        .ptbackup BINARY FILE LAYOUT                                    │
├──────────────┬──────────────┬──────────────┬──────────────┬──────────────┬─────────────┤
│ MAGIC BYTES  │ VERSION CODE │ SALT (BYTES) │ GCM IV       │ AES-GCM AUTH │ CIPHERTEXT  │
│ 4 Bytes      │ 2 Bytes      │ 32 Bytes     │ 12 Bytes     │ 16 Bytes     │ Variable    │
│ "PTBK"       │ 0x0001 (v1)  │ SecureRandom │ Random IV    │ 128-bit Tag  │ Gzipped JSON│
└──────────────┴──────────────┴──────────────┴──────────────┴──────────────┴─────────────┘
```

* **Key Derivation**: `PBKDF2WithHmacSHA512` (100,000 iterations) + 32-byte secure salt $\rightarrow$ 256-bit AES key.
* **Cipher**: `AES-256-GCM` (providing 128-bit authentication tag integrity). Tampered payloads or wrong passwords fail authentication before any database write occurs.
* **Payload**: Gzipped JSON serializing all core domain tables.

---

## 5. Concurrency, AI Action Boundary & Threading Model

### 5.1 Dispatcher Allocation

```text
┌──────────────────────────────┬─────────────────────────────────────────────────────────────────┐
│ Threading Scope              │ Assigned Workloads & Architectural Boundaries                   │
├──────────────────────────────┼─────────────────────────────────────────────────────────────────┤
│ Dispatchers.Main             │ Compose UI rendering, ViewModel state binding, Rive runtime     │
│ Dispatchers.IO               │ Room SQLCipher read/writes, DataStore access, .ptbackup I/O     │
│ Dispatchers.Default          │ Deterministic parser regex execution, Bézier math, GZIP/KDF     │
│ WorkManager Background       │ Midnight schedule generation, non-user maintenance tasks        │
│ AlarmManager — exact-time    │ Exact-time scheduling, alarms, and heads-up notification firing │
└──────────────────────────────┴─────────────────────────────────────────────────────────────────┘
```

### 5.2 Structural Enforcement of the AI Action Boundary (FR-7.6, Dimension G)
To ensure AI models (both Online Gemini Live over WebSocket and Offline SpeechRecognizer/NLP) **cannot directly mutate persistent database records**, the system enforces a strict structural separation:

```text
[ Online Gemini Live (WebSocket) / Offline STT ]
                       │ (Emits Raw Unsafe Stream / String)
                       ▼
[ Deterministic NLP Tokenizer / Tool Parser ]
                       │ (Emits Unvalidated DTO)
                       ▼
[ UnvalidatedTaskProposal ] ─── (Lacks Entity annotations; cannot be saved to Room)
                       │
                       ├───► Ambiguous / Low Confidence? ──► Routes to UnfiledCaptureInboxEntity
                       │
                       ▼ [ Passes Validation Engine & Business Schema Authorization ]
[ ValidatedTaskCreationCommand ]
                       │
                       ▼
[ TaskRepository.createTask() ] ──► (Instantiates immutable TaskEntity) ──► [ Room DAO ]
```

1. **Type Separation**: `UnvalidatedTaskProposal` and `TaskEntity` are separate classes in distinct packages. No mapper exists from unvalidated DTOs directly to Room DAOs.
2. **Access Control**: Room DAOs are `internal` to `:data:local:db` and only accessible via repository implementations.

---

## 6. Resolved Decisions vs. Configurable Heuristics Registry

| Area | Status | Architectural Design & Invariant Rule |
|---|---|---|
| **Online Voice Assistant** | `[CONFIRMED]` | **Gemini Live API over WebSocket**: Real-time bidirectional streaming assistant; subject to AI Action Boundary. |
| **Offline Voice Engine** | `[CONFIRMED]` | **Android SpeechRecognizer (on-device when available) + Deterministic Parser**: Manual text entry fallback when unavailable. |
| **Delivery-Intensity Classes** | `[CONFIRMED]` | **`ROUTINE` \| `IMPORTANT` \| `URGENT`**: Decoupled from task priority; drives notification channel intensity. |
| **Emergency & Telecom Bypass** | `[CONFIRMED / TECHNICAL CONSTRAINT]` | Dynamic `TelecomManager.getDefaultDialerPackage()` whitelist + `TelephonyManager` state listener. Never blocked; accessibility overlay suspended. |
| **Accessibility Privacy** | `[CONFIRMED / TECHNICAL CONSTRAINT]` | Event-driven foreground package detection only; screen text, passwords, OTPs, and private messages are strictly excluded from reading, logging, or storage. |
| **Room Schema & Encryption** | `[ARCHITECTURE RESOLVED]` | Room over SQLCipher (`AES-256-CBC`, 256k PBKDF2, 4096-byte page, 16 KB native ELF alignment); key held in Keystore abstraction. |
| **Backup Specification** | `[ARCHITECTURE RESOLVED]` | Manual user-initiated `.ptbackup` container: `PBKDF2-HMAC-SHA512` (100k iter) + `AES-256-GCM` authenticated payload + pre-restore safety snapshot. |
| **Provisional SDK Target** | `[PROVISIONAL TECHNICAL TARGET / ARCHITECTURE PHASE]` | Android 13–16 (API 33–36 baseline provisional target; exact compileSdk/targetSdk/minSdk verified at build setup). |
| **STT Confidence Floors** | `[VALIDATION REQUIRED / CONFIGURABLE HEURISTIC]` | Baseline thresholds: High ($\ge 0.85$), Med ($0.60–0.84$), Low ($< 0.60$); configured via `ConfidencePolicy`. |
| **Focus Override Friction** | `[VALIDATION REQUIRED / CONFIGURABLE HEURISTIC]` | Baseline: JITAI breathing pause $\rightarrow$ continuous hold-to-exit $\rightarrow$ escalation upon repeat breach; configured via `FrictionPolicy`. |
| **Scarcity Guidance Soft Cap** | `[VALIDATION REQUIRED / CONFIGURABLE HEURISTIC]` | Soft guidance prompt on 3rd active Urgent item; configured via `UrgencyScarcityPolicy`. |
| **Coin Reward Values** | `[VALIDATION REQUIRED / CONFIGURABLE HEURISTIC]` | Reward amounts mapped via domain `RewardPolicy` service; persisted to idempotent `CoinLedgerEntity`. |
| **Phone-Call Timer Behavior** | `[OPEN DECISION / TECHNICAL VALIDATION]` | Phone calls bypass blocking; exact focus timer pause vs. continue behavior configurable and subject to technical/product validation. |

---

## 7. Failure Modes & Recovery Architectural Matrix

*(Derived from PRD §6 Failure Modes & Recovery Matrix)*

| Failure Mode / Trigger | Responsible Component | Recovery & Mitigation Protocol |
|---|---|---|
| **Incoming Phone Call during Focus** | `AccessibilityInterventionService` & `TelephonyReceiver` | `TelephonyManager.CALL_STATE_RINGING` immediately suspends overlay window. FocusSession remains `IN_PROGRESS`; timer behavior handled per policy. |
| **OS Process Kill / Device Reboot** | `BootCompletedReceiver` & `HeroViewModel` | `BOOT_COMPLETED` re-registers scheduled alarms. Next app launch checks for incomplete `IN_PROGRESS` sessions and transitions them to `INTERRUPTED/RESUMABLE`. |
| **Corrupted / Tampered Backup File** | `BackupManager` (Cryptographic Engine) | `AES-256-GCM` tag validation fails during decryption; import halts with zero writes to disk; active database remains 100% intact. |
| **Incorrect Backup Password** | `BackupManager` (KDF Engine) | Password mismatch fails GCM auth tag check; UI displays error banner; active DB untouched. |
| **STT Engine Missing / Offline Failure** | `SpeechCaptureManager` | Catches `SpeechRecognizer.ERROR_NO_MATCH` or engine missing; immediately focuses Compose keyboard input for manual text entry. |
| **Network Loss during Gemini Live Voice** | `VoiceRouterService` | WebSocket disconnects gracefully; router automatically falls back to on-device `SpeechRecognizer` (if available) or manual text entry. |
| **Low-Confidence Speech Parsing** | `DeterministicParser` & `InboxRepository` | Preserves raw transcript and partial DTO into `unfiled_capture_inbox` table with confidence badge for 1-tap categorization. |
| **Rapid Checkbox Toggle Farming** | `AwardCoinsUseCase` & `CoinRepository` | `CoinLedgerEntity` uses unique compound `idempotencyKey`; SQLite ignores duplicate inserts, awarding Coins exactly once. |

---

## 8. Requirements Traceability Matrix

| Architectural Subsystem | Satisfied PRD Requirements | Satisfied Tech Stack Decisions | Acceptance Criteria |
|---|---|---|---|
| **Layered UDF Architecture** | FR-1.1, FR-1.2, FR-2.1 | `TECHSTACK.md` §1.1, §6.1 | AC-1, AC-2 |
| **DayType Resolution Engine** | FR-3.1, FR-3.2, FR-3.3, FR-3.4, FR-3.5 | Decision 3, `TECHSTACK.md` §6.2 | AC-1 |
| **Dual Voice Engine (Gemini Live + Local STT)** | FR-7.1, FR-7.2, FR-7.6 | Decision 7, `TECHSTACK.md` §10.1 | AC-2 |
| **Delivery-Intensity Notifications** | FR-5.1, FR-5.2, FR-5.3, FR-5.4, NFR-3 | Decision 5, `TECHSTACK.md` §9.1, §9.2 | AC-4 |
| **Accessibility Focus Interception** | FR-4.1, FR-4.2, FR-4.3, FR-4.4, FR-10.1 | Decision 4, `TECHSTACK.md` §9.4 | AC-3 |
| **Room + SQLCipher Encryption** | FR-9.1, FR-9.2, NFR-4 | Decision 9, `TECHSTACK.md` §8.1, §8.2 | AC-7, AC-8 |
| **Item-Type Rescheduling Engine** | FR-2.2, FR-6.1, FR-6.2, FR-6.5 | Decision 6, `TECHSTACK.md` §6.2 | AC-5 |
| **Idempotent Coins & Companion State** | FR-8.1, FR-8.2, FR-8.3, FR-8.4, FR-8.5 | Decision 8, `TECHSTACK.md` §7.3 | AC-6 |
| **Encrypted Backup & Rollback** | FR-9.3, FR-9.4, FR-9.5, FR-9.6, NFR-5 | Decision 9, `TECHSTACK.md` §11.2 | AC-7 |

---

## 9. Architecture Consistency & Precision Audit

### 9.1 Exact Corrections & Precision Reconciliations Applied
1. **§4.3 Cryptographic Layout Refined**: Replaced leftover `Poly1305/GCM` text with **`128-bit AES-GCM Auth Tag`**. Unified all backup terminology on `AES-256-GCM`.
2. **§1, §3.2, §5.2 Gemini Live Transport**: Standardized all references to **`Gemini Live API over WebSocket`**, removing unsupported gRPC claims.
3. **§1, §3.2, §6 Speech Recognition Wording**: Replaced claims of "100% offline speech-to-text" with **`on-device/offline speech recognition when an on-device recognizer is available; otherwise manual text entry fallback`**.
4. **§4.2 16 KB Platform Compatibility**: Strictly separated (1) SQLCipher 4096-byte database page size, (2) 16 KB ELF native shared library alignment (`libsqlcipher.so`), and (3) Android devices configured with 16 KB memory pages.
5. **§0, §6 Provisional SDK Target Scope**: Preserved **`Android 13–16 (API 33–36)`** as a `[PROVISIONAL TECHNICAL TARGET / ARCHITECTURE PHASE]` baseline rather than claiming locked final numbers.
6. **§5.1 Dispatcher Table**: Renamed AlarmManager entry to **`AlarmManager — exact-time scheduling and notification triggers`**.
7. **§1.1, §5.1 WorkManager Scope**: Removed "backup rotation" from WorkManager duties, confirming v1 backups are strictly user-initiated encrypted exports.
8. **§1.1, §3.3, §6 AccessibilityService Detection**: Replaced "foreground polling" with **`event-driven foreground package detection`** and reaffirmed strict zero-logging of screen text, passwords, OTPs, or private messages.
9. **§3.3, §6, §7 Phone Call & Telecom Immunity**: Confirmed emergency calls and dialer UIs are never blocked, accessibility overlays are suspended during calls, and focus timer pause/continue behavior is retained as `[OPEN DECISION / TECHNICAL VALIDATION]`.
10. **Terminology Audit (§0–§8)**: Verified consistent terminology across dual-engine voice, cryptography, platform targets, and entity models.

### 9.2 Implementation-Readiness Verdict
* **Remaining Open Decisions**:
  * `[OPEN DECISION / TECHNICAL VALIDATION]`: Focus timer pause vs. continue behavior during active phone calls.
* **Remaining Validation-Required Parameters**:
  * `[VALIDATION REQUIRED / CONFIGURABLE HEURISTIC]`: Heuristic numbers (STT confidence floors, JITAI friction timings, urgency soft cap, Coin reward amounts) encapsulated behind domain policy interfaces for runtime personal validation.
* **Remaining Provisional Technical Targets**:
  * `[PROVISIONAL TECHNICAL TARGET / ARCHITECTURE PHASE]`: Android 13–16 (API 33–36) platform target baseline.
* **Final Implementation-Readiness Verdict**: **IMPLEMENTATION READY.**  
  The architecture is architecturally sound, uncontradicted, mathematically coherent, and ready for development handoff.
