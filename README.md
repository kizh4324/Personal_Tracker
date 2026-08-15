# Personal-Tracker 🚀

A private, local-first Android productivity assistant designed to solve fragmented daily routines, inconsistent study hours, unstructured screen-time drift, and notification fatigue through adaptive DayType scheduling, distraction interception, non-punishing companion gamification, and dual-engine voice capture (online Gemini Live WebSocket + offline on-device STT).

---

## 📌 Core Features & Philosophy

* **Adaptive DayType Routine Engine**: Auto-resolves daily schedules (`Weekday`, `Weekend`, `Heavy Study`) with zero mandatory launch taps and 1-tap single-date swapping.
* **Dual Voice Capture Pipeline**: Real-time streaming voice via Gemini Live API over WebSocket with seamless fallback to on-device `SpeechRecognizer` or manual text quick-add, strictly governed by an AI Action Boundary.
* **Focus Mode & Event-Driven Distraction Interception**: Opt-in `AccessibilityService` overlay blocking for distracting apps with deliberate hold-to-override friction, dynamic emergency/telecom dialer immunity, and reboot state reconstruction.
* **Non-Punishing Companion & Idempotent Coins Economy**: 7-state Rive virtual companion on the Hero Card (zero HP loss, zero death, zero guilt) and an auditable, anti-farming Coin ledger spent exclusively on cosmetics.
* **Tiered Delivery Notifications & Scarcity Guardrails**: Delivery channels (`Routine` quiet, `Important` standard, `Urgent` heads-up) with bounded escalation and advisory guidance against urgent overbooking.
* **100% Local Data Ownership & Encrypted Backup**: SQLCipher database with Android Keystore isolation, zero telemetry SDKs, and password-protected `.ptbackup` (`PBKDF2-HMAC-SHA512` 256,000 iterations + `AES-256-GCM` 128-bit tag) with pre-restore safety snapshot rollback.

---

## 📂 Repository Structure

```
Personal-Tracker/
├── app/                                       # Android Application Source Code
│   └── src/main/
│       ├── assets/animations/                 # Lottie & Micro-interaction Assets
│       ├── java/com/personaltracker/          # Kotlin UI Theme, Components & Domain Code
│       └── res/drawable/                      # VectorDrawables & Icon Assets
│
├── docs/                                      # Complete Project Documentation & Specs
│   ├── specifications/                        # TECHSTACK, Architecture, Problem Briefs
│   ├── design/                                # Design System, Companion Brief, Icon Inventory
│   ├── status/                                # Frontend Token Implementation Verification
│   └── README.md                              # Detailed Documentation Index
│
├── _bmad-output/                              # Planning & Implementation Output Artifacts
│   ├── planning-artifacts/                    # PRDs, Briefs, Research, UX Spines, Epics
│   │   ├── epics.md                           # Authoritative 6-Epic / 29-Story Breakdown
│   │   ├── sprint-summary.md                  # Sprint Governance & Invariants Registry
│   │   └── ux-designs/                        # DESIGN.md & EXPERIENCE.md Spines
│   └── implementation-artifacts/              # Sprint Status Tracking
│       └── sprint-status.yaml                 # Machine-Readable Sprint Status Tracker
│
├── _bmad/                                     # BMAD Framework Tools, Scripts & Workflows
├── graphify-out/                              # Knowledge Graph & Codebase Analysis Artifacts
└── README.md                                  # Repository Entry Point & Quick Reference
```

---

## 📑 Key Documentation Links

* **Technical Blueprint**: [`docs/specifications/TECHSTACK.md`](docs/specifications/TECHSTACK.md)
* **Architecture Specification**: [`docs/specifications/architecture.md`](docs/specifications/architecture.md)
* **Visual Design Tokens**: [`docs/design/design-system.md`](docs/design/design-system.md)
* **UX Interaction Spine**: [`_bmad-output/planning-artifacts/ux-designs/ux-Personal-Tracker-2026-08-15/EXPERIENCE.md`](_bmad-output/planning-artifacts/ux-designs/ux-Personal-Tracker-2026-08-15/EXPERIENCE.md)
* **Sprint Epics & Stories**: [`_bmad-output/planning-artifacts/epics.md`](_bmad-output/planning-artifacts/epics.md)
* **Sprint Summary & Status**: [`_bmad-output/planning-artifacts/sprint-summary.md`](_bmad-output/planning-artifacts/sprint-summary.md)
* **Documentation Hub**: [`docs/README.md`](docs/README.md)

---

## 🛠️ Technology Stack Overview

* **Platform**: Android 13–16 (API 33–36 baseline `[PROVISIONAL TECHNICAL TARGET]`)
* **Core Language & UI**: Kotlin 2.4.0, Jetpack Compose BOM 2026.02.00, Material 3
* **Architecture**: MVI / Clean Architecture, Kotlin Coroutines, StateFlow, Hilt DI 2.55
* **Local Database**: Room 2.7.0 + SQLCipher 4.6.1 (16 KB page-aligned, 256k PBKDF2 iterations)
* **Security**: Android Keystore (`AES-256-GCM`), PBKDF2-HMAC-SHA512 KDF (256,000 iterations)
* **Voice Assistant**: Dual-engine (Gemini Live over WebSocket + on-device `SpeechRecognizer`)
* **Vector & Motion**: Rive Android Runtime 9.1.0, Compose VSYNC-synchronized Bézier curves

---

*Maintained by [@kizh4324](https://github.com/kizh4324)*
