# EGM Pre-Code Review Synthesis: Story 3.4 — Frictionless Interruption Handling & Zero-Guilt Resumption

**Story ID:** `3-4-frictionless-interruption-handling-zero-guilt-resumption`  
**Date:** 2026-11-14  
**Synthesizer:** Intermediate Swarm Compiler / Elephant  
**Swarm Reviewers:**
1. `egm-comprehension` (Comprehension Auditor) — *Verdict: REVISE*
2. `egm-design` (Hostile Design Critic) — *Verdict: FAIL*
3. `egm-implementation-readiness` (Tech Lead / Implementing Engineer) — *Verdict: NO-GO*

---

## 1. Executive Summary & Review Verdict

All three stateless Goldfish reviewers completed independent, zero-parent-context audits of [`spec-3-4-frictionless-interruption-handling-zero-guilt-resumption.md`](file:///C:/Users/User/OneDrive/Music/Personal-Tracker/_bmad-output/implementation-artifacts/spec-3-4-frictionless-interruption-handling-zero-guilt-resumption.md). While praising the behavioral psychology (Ovsiankina effect, AD-4 non-punishing microcopy, AD-1 domain purity), the swarm identified **three Blocker architectural flaws**, **five Major design gaps**, and **three Minor/Nit issues** that would cause silent persistence data loss, timer ticker collisions, zombie resumption loops, and task regression.

**Composite Verdict:** **REVISION REQUIRED (PASS WITH REVISIONS UPON UPDATING SPEC)**.

---

## 2. Consolidated Findings Matrix

| Finding ID | Severity | Category | Description | Remediation Plan |
| :--- | :--- | :--- | :--- | :--- |
| **B-1** | 🔴 Blocker | Concurrency & Persistence | `serviceScope.cancel()` on `stopSelf()` aborts in-flight Room database writes over SQLCipher, dropping banked focus minutes. | Wrap session banking in `withContext(NonCancellable)` inside `stopSession` and use `Dispatchers.IO`. |
| **B-2** | 🔴 Blocker | Android Architecture & Concurrency | Static `startBreak` in companion object cannot access injected repository; calling `startSession` while running causes ticker collisions and race conditions. | Introduce `ACTION_START_BREAK` intent handled atomically in `onStartCommand` of the running service to bank focus and start break without service cycling. |
| **B-3** | 🔴 Blocker | Persistence & Schema | `StudySessionEntity` in Room v1 lacks `targetDurationMinutes`, `actualElapsedMinutes`, and `intervalType`. Reading from DB collapsed target duration to elapsed duration, forcing resumption remaining time to 1 minute. | Bump Room DB to version 2, add `targetDurationMinutes`, `actualElapsedMinutes`, and `intervalType` to `StudySessionEntity` and `DataMappers.kt`, and provide `MIGRATION_1_2`. |
| **M-1** | 🟠 Major | Runtime Lifecycle | Ambiguity between State A (in-memory paused service) and State B (stopped service requiring cold start resumption). | Formally distinguish State A (`ACTION_RESUME`) vs State B (`startService` with remaining duration) in `HomeViewModel` and `StudyViewModel`. |
| **M-2** | 🟠 Major | Domain & Clean Architecture | `PauseOrInterruptSessionUseCase` diagrammed but orphaned (never invoked by `StudySessionForegroundService`). | Inject and invoke `PauseOrInterruptSessionUseCase` directly inside `StudySessionForegroundService.stopSession` and `ACTION_START_BREAK`. |
| **M-3** | 🟠 Major | Domain Priority | `ResolveHeroStateUseCase` missing `interruptedSession` parameter and Priority 2 precedence between tasks and study blocks is undefined. | Add `interruptedSession: StudySession?` to `resolve()` and `invoke()`. Precedence: (1) Live Paused Timer -> (2) Interrupted Study Block -> (3) Interrupted Task. |
| **M-4** | 🟠 Major | Presentation & DI | `HomeViewModel` does not observe `StudySessionRepository` or handle cold-start session resumption. | Inject `StudySessionRepository` and `ResumeInterruptedSessionUseCase` into `HomeViewModel`; observe latest interrupted session in `baseDataFlow`. |
| **M-5** | 🟠 Major | UI Regression | Modifying `HeroCardState.InterruptedResumable` breaks existing Story 1.1 task callbacks (`onReview`, `onMarkComplete`). | Retain dual-mode support in `HeroCardState.InterruptedResumable` with `isStudySession: Boolean` flag or optional callbacks. |
| **M-6** | 🟠 Major | Edge Case / Zombie Loop | Sessions where `bankedMinutes >= targetMinutes` could create infinite 1-minute zombie resumption loops. | Filter out sessions where `actualElapsedMinutes >= targetDurationMinutes` in `ResumeInterruptedSessionUseCase`. |
| **N-1** | 🟡 Nit | DI Compilation | `FocusRescheduleEngine` missing `@Inject constructor()` annotation. | Add `@Inject constructor()` to `FocusRescheduleEngine`. |
| **N-2** | 🟡 Nit | UI Models | `StudyUiIntent.TakeFiveMinuteBreak` and `StudyUiState.interruptedSession` missing from spec file map. | Add to file map and define in respective UI files. |
| **N-3** | 🟡 Nit | Telephony Semantics | Unstated semantics on telephony `CALL_STATE_IDLE`. | Explicitly state timer remains paused upon call completion per AD-4; resumption requires manual user action. |

---

## 3. Spec Revision Approval

The Elephant has synthesized all Goldfish findings and updated `spec-3-4-frictionless-interruption-handling-zero-guilt-resumption.md` with exact resolutions for B-1 through B-3, M-1 through M-6, and N-1 through N-3.
