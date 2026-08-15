# Personal-Tracker — PRD Addendum

> [!NOTE]
> **Document Purpose**: Supplementary reference document containing complete decision traceability, research cross-references, memlog audit, and open technical decision registries for downstream architecture (`bmad-architecture`) and epic creation (`bmad-create-epics-and-stories`) phases.

---

## Appendix A: Detailed PRD Functional Decisions & Memlog Audit

| PRD Rule # | Topic | Confirmed PRD Functional Behavior | Source Traceability |
|---|---|---|---|
| **PRD Rule 1** | **Interrupted / Resumable State Triggers** | **Dual-Trigger**: 1) Active `FocusSession` exited early before completion (or switching into a distracting app intervention state), OR 2) Scheduled study/focus block window expires with item `IN_PROGRESS`. Hero Card presents Resumption Card (`Resume`, `Review/Adjust`, `Mark Complete`). Never auto-completes or silently reschedules. | `[EVIDENCE-BACKED: Brief Decision 1 & Dimension H]` |
| **PRD Rule 2** | **System Immunity & State Reconstruction** | **System Immunity & State Reconstruction**: Emergency calls, phone dialer UIs, and OS-critical interfaces bypass blocking completely. Unexpected OS kills, low-memory terminations, battery exhaustion, or reboots reconstruct session state into `INTERRUPTED/RESUMABLE` on next launch when persisted state supports that an active session was incomplete. Explicit Cancel sets `CANCELLED` without resumption prompt. Exact call pause/resume mechanics are `[OPEN DECISION / TECHNICAL VALIDATION]`. | `[EVIDENCE-BACKED: Brief Decision 9 & Dimension G]` |
| **PRD Rule 3** | **Capture Routing & Confirmation Card UX** | **Inline Hero Card Confirmation Card (1-Tap Chips)**: Medium-confidence captures display an inline Hero Card confirmation card with 1-tap editable chips (`Title`, `Date`, `Time`, `Type`, `Subject`, `Duration`, `Urgency`), requiring explicit *Confirm* before persistence. Unset fields remain unpopulated. Low-confidence captures with successful STT save transcript to *Unfiled Capture Inbox* for 1-tap categorization. If on-device STT is unavailable or fails, system falls back directly to manual text entry. Direct DB auto-saves via temporary toasts are prohibited. | `[EVIDENCE-BACKED: Brief Decision 7 & Dimension E/G]` |
| **PRD Rule 4** | **Mid-Day DayType Template Swap State Handling** | **Preserve Completed Steps + Load Remaining New Schedule**: Completed/in-progress items preserved as immutable. Recalculates only remaining unstarted schedule for today from newly selected `DayType`. Single-date override persisted locally; tomorrow automatically reverts to standard resolution hierarchy (`Date Override > Day-of-Week Default`). | `[EVIDENCE-BACKED: Brief Decision 3 & Dimension A]` |
| **PRD Rule 5** | **Completion Validation & Anti-Farming Model** | **Completion Validation & Anti-Farming Model**: Idempotent reward ledger prevents duplicate Coins on repeated UI toggles or app reboots. Service-validated completions required. Duration thresholds and Coin amounts marked as hypotheses. | `[EVIDENCE-BACKED: Brief Decision 8 & Dimension F]` |
| **PRD Rule 6** | **Backup Verification & Transactional Restore** | **Password Prompt + Transactional Restore Protection**: Export requires user-created password and produces encrypted `.ptbackup` artifact. Import executes 4-step validation (password, cryptographic integrity/authentication verification, format check, schema check). Creates pre-restore safety snapshot before DB replacement and rolls back automatically if restore fails. Portable across devices with password. | `[EVIDENCE-BACKED: Brief Decision 9 & Dimension G]` |

---

## Appendix B: Traceability Matrix (Product Brief & Research to PRD FRs)

| Functional Requirement (FR) | Product Brief Decision | Research Dimension Grounding | Status |
|---|---|---|---|
| **FR-1**: Home Surface & Hero Card | Decision 1 (State-Aware Hub) | Dimension A (Schedule Engine) & H (Ovsiankina Resumption) | Confirmed |
| **FR-2**: Domain Item Models | Decision 2 (Specialized Models) | Dimension A (Study), D (Rescheduling), E (Capture), F (Rewards) | Confirmed |
| **FR-3**: DayType Engine & Swapping | Decision 3 (DayType Resolution) | Dimension A (First-Class DayType) & H (If-Then Planning) | Confirmed |
| **FR-4**: Focus Sessions & Interventions | Decision 4 (State-Aware Prompting) | Dimension B (Adaptive Intervention Ladder) & G (Accessibility) | Confirmed |
| **FR-5**: Notification Urgency & Scarcity | Decision 5 (Soft-Cap Guidance) | Dimension C (Delivery Classes & Scarcity Rule) | Confirmed |
| **FR-6**: Rescheduling & Carry Forward | Decision 6 (Item-Type Carry Forward) | Dimension D (Transparent Rescheduling & Capacity Guardrails) | Confirmed |
| **FR-7**: Low-Friction Capture & Voice | Decision 7 (Confidence Routing) | Dimension E (One-Line / Voice) & G (AI Action Boundary) | Confirmed |
| **FR-8**: Companion & Coins Economy | Decision 8 (Cosmetic Companion) | Dimension F (Non-Punishing Companion & Single Currency) | Confirmed |
| **FR-9**: Encrypted DB & Backup | Decision 9 (Encrypted Local Storage) | Dimension G (Local-First Security & Zero Telemetry) | Confirmed |
| **FR-10**: System Immunity & OS Handlers | Decision 9 & 10 (OS Reliability) | Dimension G (OEM Constraints & System Reliability) | Confirmed |

---

## Appendix C: Complete Open Decision & Hypothesis Registry

### A. Preliminary Product Hypotheses `[HYPOTHESIS]` (Personal Use Validation)
1. **State-Aware Hero Card Transitions & Animations**: Card-swapping gestures, transition physics, and styling.
2. **STT Confidence Floor Numbers**: Numeric thresholds for high/medium/low confidence routing.
3. **Voice Undo Toast Duration**: 5-second default display window for tap-to-revert toast.
4. **Urgent Item Quota Ceiling**: Soft-cap at 2 active Urgent items and active vs completed accounting rules.
5. **Acknowledgement Escalation Cadence**: 5-minute retry interval and max 3 re-fires.
6. **Workload Capacity Formula & Prompt Copy**: Algorithm for calculating planned daily hours and advisory copy.
7. **Session Duration Validity Threshold**: 10-minute minimum focus session threshold for Coins.
8. **Focus Override Friction Parameters**: Exact duration, cooldown period, confirmation behavior, and escalation ladder timing for deliberate override friction.

### B. Open Decisions `[OPEN DECISION]` (Product / Behavioral Validation)
9. **Routine Reward Granularity**: Per-step Coin allocation vs full-sequence completion bonus.
10. **Phone-Call Interruption Mechanics**: Exact pause/resume/interruption behavior and timer buffering when incoming calls occur during focus blocks `[OPEN DECISION / TECHNICAL VALIDATION]`.
11. **DayType Calendar Exceptions**: Future optional calendar exception detection and auto-assignment rules `[FUTURE / OPEN DECISION]`.

### C. Engineering Performance Targets `[ENGINEERING TARGET]`
12. **Application Performance Targets**: Cold app launch < 1.5s, quick-add NLP parsing < 300ms, and idle `AccessibilityService` background CPU < 2%.
13. **Provisional Android Version Scope**: Target range API 33–36 / Android 13–16 `[PROVISIONAL TECHNICAL TARGET]`.

### D. Architecture / Security Phase Decisions `[ARCHITECTURE PHASE]`
14. **Cryptographic Implementation & Key Derivation**: Room + SQLCipher cipher configuration, KDF algorithms/parameters, Android Keystore integration, backup integrity/authentication algorithm, secure deletion protocols, and schema versioning.
15. **OS Telecom Package Detection & Reliability**: Specific Android package detection rules for emergency/dialer bypass and OEM-specific background service execution rules.
