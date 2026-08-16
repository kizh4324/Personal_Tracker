# Epic 1 Context: Project Foundation, DayType Schedule Engine & Core Home Experience

<!-- Compiled from planning artifacts. Edit freely. Regenerate with compile-epic-context if planning docs change. -->

## Goal

Establish the Android greenfield project architecture with modern Kotlin/Compose design tokens, SQLCipher encrypted local database, automated DayType daily schedule resolution (Weekday, Weekend, Heavy Study), dynamic state-aware Hero Card container, core task management with delivery-intensity classification (Routine, Important, Urgent), and sequential routine execution with mid-day recalculation.

## Stories

- Story 1.1: Android Greenfield Project Setup, Compose Theme & Design Tokens
- Story 1.2: SQLCipher Encrypted Database Baseline & Master Key Management
- Story 1.3: DayType Entity Models, Resolution Engine & Morning Swap
- Story 1.4: Dynamic State-Aware Hero Card Container & Intentional Idle State
- Story 1.5: Core Task Management & Delivery-Intensity Classification
- Story 1.6: Sequential Routine Domain Engine & Mid-Day Schedule Recalculation

## Requirements & Constraints

- Cold launch to interactive Hero Card must execute in < 1.5s on modern hardware.
- Core tracking, schedule resolution, and task execution must operate 100% offline.
- Database must be encrypted at rest using SQLCipher with AES-256-CBC, 256,000 PBKDF2 iterations, 4096-byte DB page size, and 16 KB native ELF page alignment.
- Master key lifecycle managed via Android Keystore (`AES-256-GCM`, `setUserAuthenticationRequired(false)`).
- DayType resolution hierarchy: Date-Specific User Override > Day-of-Week Default. Calendar sync is excluded from v1.
- DayType swaps applied from Home apply strictly to the current date and do not mutate base templates.
- Mid-day template swaps preserve all completed and in-progress items as immutable historical records, recalculating only unstarted blocks.
- Task delivery intensity classes are strictly `ROUTINE`, `IMPORTANT`, and `URGENT` (numeric P1–P4 scales prohibited).
- Delivery intensity drives left accent visual stripes (Routine: 3dp info, Important: 3dp warning, Urgent: 6dp danger + badge).
- No guilt mechanics, punitive language, or silent task rescheduling.

## Technical Decisions

- Target platform: Android 13–16 (API 33–36 baseline).
- Languages & Frameworks: Kotlin 2.4.0, Jetpack Compose BOM 2026.02.00, Material 3 foundation with custom token extensions.
- Dependency Injection: Hilt DI (`com.google.dagger:hilt-android:2.55`).
- Architecture: MVI / Clean Architecture, Kotlin Coroutines, StateFlow.
- Room over SQLCipher (`net.zetetic:sqlcipher-android:4.6.1`).
- Threading rules: Compose rendering on `Dispatchers.Main`, DB/Crypto/IO on `Dispatchers.IO`, deterministic regex and math on `Dispatchers.Default`.
- Package structure: `:app`, `:core:designsystem`, `:core:database`, `:core:model`, `:feature:home`, `:feature:tasks`, `:feature:routines`.

## UX & Interaction Patterns

- 5-tab bottom navigation (`Home`, `Habits`, `Study`, `Companion`, `Settings`) with 48dp minimum touch targets.
- Hero Card manages 6 priority states: Active Focus, Interrupted/Resumable, Morning DayType Banner, Upcoming Scheduled Item, Carry-Forward Review, Intentional Idle.
- State transitions animate with 300ms `FastOutSlowIn` cross-fade.
- Persistent bottom sheet timeline for DayType schedule with Peek (~100dp), Half (~50%), Full (~100%) snap points.
- Plus Jakarta Sans for UI text, Inter for tabular data.
- Light and Dark mode parity with WCAG 2.1 AA contrast compliance.

## Cross-Story Dependencies

- Story 1.1 provides the Compose theme, typography, and color tokens required by all subsequent UI components.
- Story 1.2 provides the encrypted Room database baseline required by entities in Stories 1.3, 1.5, and 1.6.
- Story 1.3 provides DayType resolution logic consumed by Hero Card (Story 1.4) and Routines (Story 1.6).
- Story 1.5 provides Task CRUD and delivery intensity models integrated into Routine recalculation (Story 1.6).
