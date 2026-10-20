---
type: index
title: "Personal-Tracker Knowledge Base Index"
description: "Root Open Knowledge Format (OKF v0.2) index cross-linking architectural invariants, engineering standards, sprint tracking, and active session handoffs."
tags:
  - okf
  - index
  - architecture
  - catalog
  - android
verified: machine-confirmed
status: active
sources:
  - uri: "ARCHITECTURE-SPINE.md"
  - uri: "_bmad-output/project-context.md"
  - uri: "HANDOFF.md"
  - uri: "_bmad-output/implementation-artifacts/sprint-status.yaml"
---

# Personal-Tracker Knowledge Base Index (OKF v0.2)

Welcome to the **Open Knowledge Format (OKF v0.2)** index for **Personal-Tracker**, a local-first, network-optional native Android productivity application.

This index serves as the machine-readable and human-navigable entrypoint for autonomous agents and human developers alike, cataloging authoritative architectural rules, engineering standards, project history, and active sprint state.

---

## 1. Authoritative Knowledge Catalog

| Document | OKF Type | Role & Scope | Verification Status |
| :--- | :--- | :--- | :--- |
| [**`ARCHITECTURE-SPINE.md`**](../../ARCHITECTURE-SPINE.md) | `architecture-spine` | Canonical architectural decisions (`AD-1` through `AD-8`), layer boundaries, and structural invariants. | Verified (Mechanical Linter: 0 findings) |
| [**`_bmad-output/project-context.md`**](../../_bmad-output/project-context.md) | `system-context` | Engineering guidelines, technology stack baseline (Kotlin 2.1, Compose BOM, Room/SQLCipher), token rules, and pitfalls. | Verified (Human-Reviewed) |
| [**`HANDOFF.md`**](../../HANDOFF.md) | `handoff` | Session snapshot, completed story inventory, exact kickoff instructions for Story 1.6, and runbook. | Verified (Human-Reviewed) |
| [**`_bmad-output/implementation-artifacts/sprint-status.yaml`**](../../_bmad-output/implementation-artifacts/sprint-status.yaml) | `sprint-status` | Dynamic single source of truth for story lifecycle states across Epics 1 through 4. | Machine-Updated |

---

## 2. Architectural Core at a Glance

All components and agent tasks must conform to the 8 Architectural Decisions documented in [`ARCHITECTURE-SPINE.md`](../../ARCHITECTURE-SPINE.md):

* **AD-1 (Layer Dependency Direction & UI Isolation):** `Presentation (ui/) -> Domain (domain/) -> Data (data/) & Platform (platform/)`. The `domain/` layer is **100% pure Kotlin** with zero `android.*` or `androidx.*` dependencies.
* **AD-2 (Unidirectional Reactive MVI Contract):** Screens implement `UiState`, `UiIntent`, and `UiEffect` driven by ViewModels exposing `StateFlow<UiState>` and Channel-backed `Flow<UiEffect>`.
* **AD-3 (Offline-First Encrypted Persistence):** Room 2.7.0 over SQLCipher 4.6.1 with hardware MasterKey AES-GCM-256. Multi-entity operations across domain boundaries execute in `@Transaction` blocks on `Dispatchers.IO`. Never use `OnConflictStrategy.REPLACE` on entities with cascading foreign keys.
* **AD-4 (Non-Punishing UX & Microcopy):** Zero guilt, nag, or shaming copy. Companion pet has 0 HP loss, hunger decay, or death mechanics. Missed habits freeze peacefully.
* **AD-5 (Multi-Modal Voice Quick Capture):** RAM-only streaming audio transcription. Raw audio buffers purged immediately after transcription with zero disk writes.
* **AD-6 (Adaptive Day Scheduling & Routines):** Dynamic schedule resolution based on DayType templates with non-destructive carry-forward.
* **AD-7 (Strict Design Token Conformance):** Zero hardcoded hex colors, `dp`, or `sp` literals in `ui/components/` and `ui/screens/`. Minimum interactive touch target is 48dp floor. Particle/arc animations hoisted to `RewardMotionStage`.
* **AD-8 (Deterministic Confidence Routing):** 3-tier routing: High ($\ge 0.85$) with 5s undo toast; Medium ($0.50..0.85$) with 1-tap confirmation; Low ($< 0.50$) to unfiled inbox.

---

## 3. Active Sprint & Transition

* **Current Epic:** Epic 1 — Core Architecture & Persistence
* **Current Status:** Stories 1.1 through 1.5 completed and verified on physical hardware (`iQOO I2223`).
* **Active Story:** Story 1.6 — Platform Runtime Services & Background Workers (`com.personaltracker.platform`).
* **Sprint Tracker:** Refer to [`_bmad-output/implementation-artifacts/sprint-status.yaml`](../../_bmad-output/implementation-artifacts/sprint-status.yaml) for real-time status transitions.
* **Next Steps:** Follow the kickoff checklist in [`HANDOFF.md`](../../HANDOFF.md) § 12.

---

## 4. Verification & Linting Runbook

To verify codebase health and architectural integrity:

```powershell
# 1. Architectural Spine Linter (0 findings required)
python .agent/skills/bmad-architecture/scripts/lint_spine.py --workspace .

# 2. Harness Domain Purity & Token Conformance Check
python .agents/scripts/domain_purity_check.py

# 3. Harness PreToolUse Safety Gate Self-Test Suite
python .agents/scripts/harness_gate.py --test

# 4. JVM Unit Test Suite
.\gradlew test

# 5. Android Lint Check
.\gradlew lintDebug
```
