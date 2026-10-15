# Sprint Change Proposal — Personal-Tracker (2026-10-15)

## Section 1: Issue Summary

- **Trigger:** Architectural alignment audit against `ARCHITECTURE-SPINE.md` (AD-1 through AD-8), `_bmad-output/project-context.md`, `DESIGN.md`, `EXPERIENCE.md`, and the existing implementation in `app/src/`.
- **Discovery Context:** During sprint execution of Epic 1, an audit of `app/src/` revealed that foundational infrastructure was already implemented (Compose theme tokens in `ui/theme/`, SQLCipher database with Keystore master key in `data/local/security/`, 8 Room entities, 9 DAOs, navigation routing in `ui/navigation/`, and Lottie/coin arc animations in `ui/components/animation/`). However, the story backlog leaped directly from database creation to feature screens, skipping the critical 4-layer architectural bridge substrate mandated by AD-1 through AD-8:
  1. Data & Schema Completion (Standalone `InterventionRuleEntity`, completion audit logs, Flow-based SSOT Repositories with `{DOMAIN}_{ACTION}_{ID}` idempotency keys).
  2. Pure Kotlin Behavioral Engines (`AdaptiveScheduleEngine`, `StreakStateMachine` 4-state flame, `GamificationLedgerEngine`, `FocusRescheduleEngine`, `InterventionRuleEngine`, `NaturalLanguageCommandParser`).
  3. UI Shell & Atomic Component Library (`AppShellScaffold`, Top Context Bar, hoisted `RewardMotionStage`, `QuickCaptureSheet`, `HeroCard` container state machine, atomic domain cards).
  4. Platform Runtime Services (`StudySessionForegroundService`, `DayRolloverWorker`, `ExactAlarmScheduler`, `QuickCaptureVoiceRecorder`).
  5. Unidirectional MVI Screen Wiring (`HomeScreen`, `HabitsScreen`, `StudyScreen`, `CompanionScreen`, `SettingsScreen` paired with `UiState`, `UiIntent`, `Flow<UiEffect>`, and ViewModels).
- **Evidence:** `sprint-status.yaml` had Story 1.1 in `review` and Story 1.2 in `backlog`, while both are already implemented and pass `.\gradlew test`. Furthermore, jumping into feature stories without repositories, pure Kotlin domain engines, or the UI shell violates AD-1 (strict layer isolation), AD-2 (MVI contracts), AD-3 (pure Kotlin engines), AD-4 (idempotent ledgering), AD-6 (cross-domain transactional boundaries), and AD-7 (hoisted motion and token-strict components).

---

## Section 2: Impact Analysis

- **Epic Impact:**
  - **Epic 1:** Expanded from 6 stories to 11 stories to systematically insert the 5 architectural bridge stories between the completed foundations (Stories 1.1 and 1.2) and the feature-screen implementations (Stories 1.8 to 1.11).
  - **Epics 2 through 6:** Remain stable in scope and numbering (2.1–2.5, 3.1–3.6, 4.1–4.5, 5.1–5.5, 6.1–6.4), but gain rock-solid foundations:
    - Epic 2 (Voice) builds on `QuickCaptureVoiceRecorder` (Story 1.6) and `NaturalLanguageCommandParser` (Story 1.4).
    - Epic 3 (Focus & Resumption) builds on `StudySessionForegroundService` (Story 1.6), `FocusRescheduleEngine` (Story 1.4), and `InterventionRuleEngine` (Story 1.4).
    - Epic 4 (Habits & Companion) builds on `StreakStateMachine` (Story 1.4), `GamificationLedgerEngine` (Story 1.4), and `RewardMotionStage` (Story 1.5).
    - Epic 5 (Notifications & Review) builds on `ExactAlarmScheduler` (Story 1.6) and `DayRolloverWorker` (Story 1.6).
    - All feature screens build on the MVI contracts wired in Story 1.7.
- **Story Impact:**
  - Story 1.1: Updated from `review` to `done`.
  - Story 1.2: Updated from `backlog` to `done`.
  - Story 1.3: Newly sequenced as `Data & Schema Completion, Flow-Based SSOT Repositories & Idempotency Architecture` (`ready-for-dev`).
  - Story 1.4: Newly sequenced as `Pure Kotlin Behavioral Engines & Deterministic NLP Command Parser` (`backlog`).
  - Story 1.5: Newly sequenced as `UI Shell & Atomic Component Library` (`backlog`).
  - Story 1.6: Newly sequenced as `Platform Runtime Services & Background Workers` (`backlog`).
  - Story 1.7: Newly sequenced as `Unidirectional MVI Screen Wiring & ViewModels Baseline` (`backlog`).
  - Stories 1.8 through 1.11: Resequenced feature stories (DayType Resolution, Hero Card, Tasks, Routines).
- **Artifact Conflicts & Resolutions:**
  - `_bmad-output/planning-artifacts/epics.md`: Updated Epic 1 overview, FR coverage, and story specifications to include Stories 1.1 through 1.11.
  - `_bmad-output/implementation-artifacts/epic-1-context.md`: Updated to document the expanded 11-story structure, architectural constraints, and cross-story dependencies.
  - `_bmad-output/implementation-artifacts/sprint-status.yaml`: Updated keys and statuses, validated via `sprint_plan.py`.
- **Technical Impact:** Prevents technical debt, prevents UI logic from coupling to database DAOs (AD-1 violation), guarantees deterministic pure-Kotlin unit testing for all business logic, and establishes unidirectional data flow before UI development.

---

## Section 3: Recommended Approach

- **Chosen Path:** **Direct Adjustment & Bridge Insertion**.
  - Retain all completed foundation work (`ui/theme/`, `data/local/security/`, `AppDatabase.kt`, 8 entities, 9 DAOs, navigation, animations).
  - Insert the 5 architectural bridge stories immediately after Story 1.2 to construct the complete 4-layer architecture.
  - Advance the immediate next story to Story 1.3 (`ready-for-dev`).
- **Rationale:** Building feature screens without repositories or domain engines forces ViewModels to access DAOs directly or duplicate state machine logic inside Composables, directly violating AD-1, AD-2, AD-3, and AD-7. Inserting bridge stories now guarantees that every subsequent feature story is a straightforward implementation of a well-defined domain interface.
- **Risk Assessment:** Low risk. No code is deleted or rolled back. Foundation code remains 100% intact and passing tests.
- **Effort Estimate:** 5 focused bridge stories that provide the architectural substrate for all remaining epics.

---

## Section 4: Detailed Change Proposals

### 1. `_bmad-output/implementation-artifacts/sprint-status.yaml`
```yaml
# OLD:
development_status:
  epic-1: in-progress
  1-1-android-greenfield-project-setup-compose-theme-design-tokens: review
  1-2-sqlcipher-encrypted-database-baseline-master-key-management: backlog
  1-3-daytype-entity-models-resolution-engine-morning-swap: backlog
  1-4-dynamic-state-aware-hero-card-container-intentional-idle-sta: backlog
  1-5-core-task-management-delivery-intensity-classification: backlog
  1-6-sequential-routine-domain-engine-mid-day-schedule-recalculat: backlog
  epic-1-retrospective: optional

# NEW:
development_status:
  epic-1: in-progress
  1-1-android-greenfield-project-setup-compose-theme-design-tokens: done
  1-2-sqlcipher-encrypted-database-baseline-master-key-management: done
  1-3-data-schema-completion-flow-based-ssot-repositories-idempote: ready-for-dev
  1-4-pure-kotlin-behavioral-engines-deterministic-nlp-command-par: backlog
  1-5-ui-shell-atomic-component-library: backlog
  1-6-platform-runtime-services-background-workers: backlog
  1-7-unidirectional-mvi-screen-wiring-viewmodels-baseline: backlog
  1-8-daytype-schedule-resolution-morning-template-swap: backlog
  1-9-dynamic-state-aware-hero-card-integration-action-list: backlog
  1-10-core-task-management-delivery-intensity-classification: backlog
  1-11-sequential-routine-domain-engine-mid-day-schedule-recalculat: backlog
  epic-1-retrospective: optional
```

### 2. `_bmad-output/implementation-artifacts/epic-1-context.md`
- Updated goal, requirements, and technical decisions to incorporate the complete 4-layer architecture.
- Documented Stories 1.1 through 1.11 with explicit tracking of completed items and newly sequenced bridge stories.
- Expanded cross-story dependencies to show how data, domain engines, UI shell, platform runtime, and MVI wiring feed into the feature stories.

### 3. `_bmad-output/planning-artifacts/epics.md`
- Updated Epic 1 section to define all 11 user stories with detailed Gherkin acceptance criteria.
- Updated FR Coverage Map and Epic List to trace requirements accurately to the new story numbers.

---

## Section 5: Implementation Handoff

- **Change Scope Classification:** Moderate (Backlog reorganization aligning with architecture spine; no PRD changes).
- **Handoff Target:** Developer Agent (`bmad-build` / `bmad-agent-dev`).
- **Immediate Next Story:** Story 1.3: Data & Schema Completion, Flow-Based SSOT Repositories & Idempotency Architecture (`1-3-data-schema-completion-flow-based-ssot-repositories-idempote`).
- **Success Criteria:**
  1. `sprint_plan.py validate --status-file _bmad-output/implementation-artifacts/sprint-status.yaml` reports `valid: true` with 0 problems.
  2. All planning and sprint tracking artifacts are committed and pushed to GitHub.
