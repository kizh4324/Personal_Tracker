<!-- bmad:context -->
<!-- Verified 2026-10-15 against 1c56a4fe83d78ae6a365c4fda70962168c19ab0f. Managed by bmad-project-context; edits inside this block are replaced on refresh. Keep anything you want preserved outside the markers. -->

## Personal-Tracker

Local-first, network-optional native Android personal productivity application with encrypted persistence, behavioral engines, voice capture, and a non-punishing companion. Kotlin 2.1.0, JDK 21, Compose BOM 2026.02.00, Hilt 2.58, Room 2.7.0 over SQLCipher 4.6.1. Complete architecture spine in `ARCHITECTURE-SPINE.md`, detailed project context in `_bmad-output/project-context.md`, UX specs in `_bmad-output/planning-artifacts/ux-designs/ux-Personal-Tracker-2026-08-15/`.

## Policy

- Never commit directly without passing `.\gradlew test` and `.\gradlew lint`.
- Never use KAPT; use KSP only (`ksp` plugin and configurations).
- Never import Android framework classes (`android.*`, `androidx.*`) into `com.personaltracker.domain`; domain logic must remain pure Kotlin.
- Never write hardcoded colors, `dp`, or `sp` literals in UI composables; resolve exclusively from `com.personaltracker.ui.theme` tokens.
- Never use `OnConflictStrategy.REPLACE` on Room entities with cascading foreign keys (use `@Upsert` or `@Insert(onConflict = ABORT)`).
- Never nag or shame the user: zero guilt copy in UI strings or notifications; companion pet has 0 HP loss, hunger decay, or death mechanics.
- Never allow AI engines to directly mutate the database without deterministic confidence routing or user confirmation.

## Where things are

- Architecture Spine: `ARCHITECTURE-SPINE.md` (AD-1 to AD-8 invariants)
- Engineering Context & Rules: `_bmad-output/project-context.md`
- Visual Design Tokens & Identity: `_bmad-output/planning-artifacts/ux-designs/ux-Personal-Tracker-2026-08-15/DESIGN.md`
- UX Interaction & Microcopy Rules: `_bmad-output/planning-artifacts/ux-designs/ux-Personal-Tracker-2026-08-15/EXPERIENCE.md`
- Domain Behavioral Engines: `app/src/main/java/com/personaltracker/domain/engine/`
- Database, DAOs & Entities: `app/src/main/java/com/personaltracker/data/local/db/`
- Hardware Cryptography: `app/src/main/java/com/personaltracker/data/local/security/`
- Compose Theme Tokens: `app/src/main/java/com/personaltracker/ui/theme/`

## Running and verifying

- Build and execute unit test suite: `.\gradlew test` (runs on JVM, ~1-2 min).
- Execute Android lint: `.\gradlew lint` (verifies accessibility, permissions, layout rules).
- Assemble debug APK: `.\gradlew assembleDebug`.
- Verify architectural spine mechanical integrity: `python .agent/skills/bmad-architecture/scripts/lint_spine.py --workspace .`
- Daemon JVM note: Ensure `JAVA_HOME` or `org.gradle.java.home` points to JDK 21 (Java 26 on PATH breaks tooling sync).

## Conventions that differ from defaults

- Multi-entity database operations across domain boundaries must execute inside Room `@Transaction` blocks on `Dispatchers.IO`.
- Every coin earning/deduction must include a deterministic compound `idempotencyKey` (`{DOMAIN}_{ACTION}_{ID}`).
- ViewModels strictly implement MVI: single `StateFlow<UiState>`, single `UiIntent` handler, and Channel-backed `Flow<UiEffect>`.
- Reward particle/arc animations must be hoisted to `RewardMotionStage` at root scaffold level to prevent viewport clipping.
- Minimum interactive touch target is 48dp floor for all clickable composables.

## Known pitfalls

- Using `OnConflictStrategy.REPLACE` on `DayTypeEntity` or `HabitEntity` silently deletes child completion logs via SQLite cascade delete.
- Running Gradle with system default `java.exe` on Windows (Java 26) causes `The supplied phased action failed with an exception. 26`.
- Bypassing Repository interfaces in ViewModels causes state divergence and circumvents SQLCipher encryption hooks.

<!-- /bmad:context -->
