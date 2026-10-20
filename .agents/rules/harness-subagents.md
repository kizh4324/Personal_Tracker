# Harness Subagent Delegation & Context Optimization Rules

This rule guides `agy` and all Antigravity agents in `Personal-Tracker` on when and how to delegate tasks to subagents using `invoke_subagent`.

---

## 1. Context Window Conservation

To keep the primary conversation context fast, responsive, and within token limits:
- **Never flood the parent context** with voluminous build output, verbose test stack traces, or repetitive multi-file boilerplate generation.
- **Delegate multi-step, verbose, or multi-component tasks** to subagents via `invoke_subagent`.
- The parent agent remains the coordinator, synthesizer, and guardian of architectural invariants.

---

## 2. When to Invoke Subagents

### A. Multi-Component Stories (e.g., Story 1.6: Platform Runtime Services)
When an implementation story entails creating multiple distinct platform or domain services:
- Example (Story 1.6):
  1. `StudySessionForegroundService` (Foreground service & lock-screen countdown)
  2. `DayRolloverWorker` (WorkManager midnight streak & schedule rollover)
  3. `ExactAlarmScheduler` (AlarmManager exact alarm clocks)
  4. `QuickCaptureVoiceRecorder` (In-memory audio recording & amplitude streaming)
  5. `BootCompletedReceiver` (Device reboot alarm & session restoration)
- **Protocol:** Invoke a specialized subagent (`invoke_subagent`) to implement or verify each distinct platform service or cluster, returning a concise summary of changes and test outcomes.

### B. Parallel Unit Test & Lint Verification
- When running extensive test suites (`.\gradlew test`, `.\gradlew lintDebug`) or diagnostic builds, invoke a subagent to execute the verification and summarize failures.
- Keeps hundreds of lines of Gradle compiler and test runner output outside the main thread.

### C. Deep Codebase Reconnaissance & Exploration
- Delegate deep file searches, symbol tracking, or multi-spec cross-referencing to the `research` subagent.

---

## 3. Workspace Mode Selection (`inherit` vs. `branch`)

Choose the appropriate `Workspace` parameter when invoking subagents:

Mode | When to Use | Behavior
:--- | :--- | :---
`inherit` | **Standard Implementation & Sequential Work** | Subagent operates directly in the primary workspace. Ideal for adding new files (e.g. creating `PlatformModule.kt`, `ExactAlarmScheduler.kt`) where edits must immediately persist in the project.
`branch` | **Exploratory, High-Risk, or Parallel Experiments** | Subagent operates in an isolated workspace branch (git worktree / clone). Ideal for speculative architecture tests, schema migrations, or adversarial refactoring where failed attempts must not dirty the main working tree.
`share` | **Lightweight Branching** | Shares underlying repository storage while maintaining an independent branch.

---

## 4. Subagent Communication & Wakeup Protocol

1. **Clear Task Prompts:** Provide self-contained, actionable prompts specifying:
   - Target files to create/edit.
   - Architectural invariants to enforce (`AD-1` to `AD-8`).
   - Expected return format (succinct summary of changes and test results).
2. **Reactive Resumption (No Polling):**
   - The system automatically wakes up the parent agent when subagent tasks complete.
   - **Never poll** in a loop waiting for subagent completion.
3. **Agent Channels:**
   - Use `send_message` strictly for communicating with subagents using their `conversationId`.
   - Never use `send_message` to talk to the human user; communicate directly via standard markdown output.

---

## 5. Architectural Invariants for Subagents

All delegated subagents must strictly obey `ARCHITECTURE-SPINE.md`:
- **AD-1 (Domain Purity):** Zero `android.*` or `androidx.*` imports in `com.personaltracker.domain`.
- **AD-2 (Persistence):** Room `@Transaction` for multi-boundary mutations; `@Upsert` or `@Insert(onConflict = ABORT)` (never `OnConflictStrategy.REPLACE` with cascade keys).
- **AD-7 (Design Tokens):** Zero hardcoded hex colors or `dp`/`sp` literals in `ui/components/` and `ui/screens/`.
- **AD-8 (Zero-Disk Audio):** Audio recorded in RAM only; purge raw buffers immediately post-transcription.
