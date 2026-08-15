# TECHSTACK.md — Personal-Tracker v1

## 0. Document Status

**Project:** Personal-Tracker  
**Platform:** Native Android mobile application  
**Scope:** v1, single-user, personal-use, local-first  
**Primary goal:** Reliable offline-first routine, task, habit, study, focus, reminder, recovery, companion, and reward workflows.

### Status tags used in this document

- **[CONFIRMED]** — required by the approved PRD / frozen product scope.
- **[RECOMMENDED]** — selected engineering technology or pattern because it fits the confirmed requirements and current Android guidance.
- **[OPTIONAL]** — may be added without becoming a v1 dependency.
- **[OPEN DECISION]** — requires an explicit product/architecture decision before implementation.
- **[TECHNICAL CONSTRAINT]** — imposed by Android/platform behavior or distribution rules.
- **[ARCHITECTURE PHASE]** — implementation detail intentionally not frozen by the PRD.
- **[DEFERRED]** — outside the v1 build, but may return in a future version.
- **[EXCLUDED]** — ruled out by a v1 product decision (not merely postponed; see PRD §8 Non-Goals).

Tags may combine (e.g. `[CONFIRMED / TECHNICAL CONSTRAINT]`) when a requirement
is fixed by the PRD but its implementation is additionally bounded by
platform behavior.

> **Correction:** the source document used this eight-tag legend inconsistently —
> `[EXCLUDED]` appeared in tables (§23, §26) without ever being defined here, and
> a ninth, undefined tag `[NOT REQUIRED]` appeared once (§23, Paging row). This
> revision defines `[EXCLUDED]` above and reclassifies the Paging row under
> `[OPTIONAL]`, since "not justified by current dataset size" is an optional-not-now
> case, not a distinct status category.

---

# 1. Executive Stack Decision

## 1.1 Final v1 stack

| Layer | Selected technology | Status | Reason |
|---|---|---|---|
| Platform | Native Android | [CONFIRMED] | Product is Android-only |
| Language | Kotlin | [RECOMMENDED] | First-class Android language and ecosystem |
| IDE | Android Studio | [RECOMMENDED] | Official Android development environment |
| UI | Jetpack Compose + Material 3 | [RECOMMENDED] | Current Android-native UI toolkit and strong animation support |
| UI state | ViewModel + StateFlow + UDF | [RECOMMENDED] | Matches current Android architecture guidance |
| Architecture | Layered architecture + Repository pattern | [RECOMMENDED] | Separates UI, business logic, and data sources |
| Domain logic | Kotlin use-case/domain classes where complexity justifies them | [RECOMMENDED] | Keeps scheduling, recovery, rewards, and intervention rules testable |
| DI | Hilt | [RECOMMENDED] | Android-focused DI with generated dependency graph |
| Local database | Room over SQLite | [CONFIRMED] | Primary local source for structured app data |
| Database encryption | SQLCipher for Android + Room | [CONFIRMED] | Required encrypted local database direction |
| Preferences | Jetpack DataStore | [RECOMMENDED] | Small settings/state, separate from Room |
| Async | Kotlin Coroutines | [RECOMMENDED] | Native async model |
| Reactive state | Kotlin Flow / StateFlow | [RECOMMENDED] | Reactive local data and UI state |
| Navigation | Navigation Compose | [RECOMMENDED] | Native Compose navigation |
| Notifications | Android Notification APIs + Notification Channels | [CONFIRMED] | Routine/Important/Urgent delivery |
| Exact reminders | AlarmManager | [CONFIRMED] | Required only for genuinely time-critical reminders |
| Deferrable background work | WorkManager | [CONFIRMED] | Persistent deferrable work |
| Focus intervention | AccessibilityService, explicit user opt-in | [CONFIRMED / TECHNICAL CONSTRAINT] | Product requires distraction intervention; Android controls service enablement |
| Usage analytics | UsageStatsManager | [CONFIRMED] | Historical usage analytics, not primary real-time blocking |
| Voice capture | Android SpeechRecognizer, on-device when available | [CONFIRMED / ARCHITECTURE PHASE] | Local voice capture with manual text fallback |
| Parsing | Deterministic Kotlin parser + schema validation | [CONFIRMED] | Core parsing must remain deterministic and offline-capable |
| Serialization | kotlinx.serialization | [RECOMMENDED] | Kotlin-native structured data/backup serialization |
| Optional cloud AI | Firebase AI Logic → Gemini Developer API | [OPTIONAL / DEFERRED] | Only if cloud AI is enabled later |
| Optional on-device AI | Firebase AI Logic on-device / ML Kit Prompt API where supported | [OPTIONAL / DEFERRED] | Capability varies by device and is not a v1 dependency |
| Local encryption keys | Android Keystore | [CONFIRMED] | Key-management boundary |
| Backup | Encrypted `.ptbackup` export/import | [CONFIRMED] | User-initiated local backup |
| Testing | JUnit + Compose UI tests + instrumentation + Turbine | [RECOMMENDED] | Covers business logic, flows, UI, and Android behavior |
| Build | Gradle Kotlin DSL + Android Gradle Plugin | [RECOMMENDED] | Standard Android build system |
| Code generation | KSP where required | [RECOMMENDED] | Current annotation/code-generation path for relevant libraries |

---

# 2. Critical Correction From the Previous TECHSTACK

The previous document described:

> **Supabase/PostgreSQL + cloud synchronization + online source of truth**

That is **not compatible with the approved Personal-Tracker v1 PRD**.

The frozen product scope is local-first, network-optional, 100% functional offline, with no required cloud backend or cloud synchronization.

Therefore:

- Supabase is **removed from the v1 core stack**.
- PostgreSQL is **removed from the v1 core stack**.
- Cloud synchronization is **removed from v1**.
- Remote/cloud data is **not the source of truth**.
- Room is the **v1 source of truth on the device**.
- Cloud Gemini is **optional/deferred**, not required for core functionality.
- No authentication system is required for v1.
- No Supabase RLS requirement exists in v1.

This is a deliberate consistency correction, not a technology preference.

---

# 3. Platform & Language

## 3.1 Android

**[CONFIRMED] Native Android only.**

No web application, iOS application, Flutter application, or React Native application is part of v1.

## 3.2 Kotlin

**[RECOMMENDED] Kotlin**

Kotlin is the application language for:

- UI state
- business rules
- repositories
- Room
- scheduling
- notifications
- AccessibilityService
- voice capture integration
- backup/restore
- security integration
- tests

Python is not part of the production Android runtime. It may be used externally for research or experimentation only.

---

# 4. UI Layer

## 4.1 Jetpack Compose

**[RECOMMENDED] Jetpack Compose**

Compose should be the primary UI toolkit.

It is appropriate for Personal-Tracker because the product requires:

- dynamic Hero Card states
- DayType timelines
- Focus states
- interactive task cards
- Companion states
- reward animations
- confirmation surfaces
- bottom sheets
- animated transitions
- responsive layouts

Compose provides native animation APIs including `AnimatedVisibility`, `AnimatedContent`, transitions, and value-based animation.

## 4.2 Material 3

**[RECOMMENDED] Material 3 as the structural foundation**

Material 3 should provide the baseline:

- typography
- buttons
- surfaces
- navigation
- dialogs
- sheets
- accessibility behavior
- theming

The Personal-Tracker design system can customize Material 3 rather than replacing the entire component foundation.

---

# 5. Application Architecture

## 5.1 Do not freeze "MVVM" as the entire architecture

The previous document treated MVVM as the architecture itself.

That is too narrow.

**[RECOMMENDED] Use:**

```text
Presentation
    ↓
ViewModel / State Holder
    ↓
Optional Domain / Use Cases
    ↓
Repository
    ↓
Data Sources
    ↓
Room / Android APIs / Optional Network
```

Android's current architecture guidance emphasizes layered architecture, repositories, UDF, ViewModels, coroutines/Flow, and a domain layer when complexity justifies it.

## 5.2 Repository pattern

**[RECOMMENDED]**

Repositories are the boundary through which application logic accesses data sources.

The UI must not directly access:

- Room DAOs
- SQLite
- network clients
- backup files
- Android system data sources

This keeps business rules centralized and testable.

## 5.3 Domain layer

**[RECOMMENDED] Use selectively**

Use cases are appropriate for complex reusable rules such as:

- DayType resolution
- item-type-aware rescheduling
- intervention policy
- urgent notification classification
- completion validation
- Coin awarding
- streak recovery
- backup validation

Do not create a use-case class for every trivial CRUD operation.

---

# 6. State Management

## 6.1 Kotlin Coroutines

**[RECOMMENDED]**

Use coroutines for:

- Room operations
- backup/restore
- parsing
- scheduling work
- system-service coordination
- optional network/AI calls

## 6.2 Flow / StateFlow

**[RECOMMENDED]**

Use:

```text
Flow
    ↓
Repository
    ↓
ViewModel
    ↓
StateFlow
    ↓
Compose UI
```

UI state should be exposed through immutable state.

Android recommends coroutines/Flow and screen-level ViewModels with UDF.

---

# 7. Dependency Injection

## 7.1 Hilt

**[RECOMMENDED] Hilt**

Use Hilt for:

- repositories
- Room database/DAOs
- use cases
- ViewModels
- backup services
- notification/scheduling services
- parser services
- voice services
- security services

Hilt is Android-focused, built on Dagger, and is officially recommended for Android dependency injection.

---

# 8. Local Data Architecture

## 8.1 Room + SQLite

**[CONFIRMED]**

Room is the application database abstraction.

```text
Kotlin
  ↓
Repository
  ↓
Room
  ↓
SQLite
  ↓
Device storage
```

Room provides compile-time query verification, entities, DAOs, and migrations, and Android recommends Room instead of using SQLite APIs directly.

Room should store:

- DayType templates
- date-specific DayType overrides
- tasks
- routines
- routine steps
- habits
- study sessions
- focus sessions
- completion history
- interruption/recovery state
- notification state
- Coins
- companion progression
- shop inventory/unlocks
- local capture/inbox records
- backup metadata where needed

## 8.2 SQLCipher

**[CONFIRMED]**

Use the current `sqlcipher-android` library rather than the deprecated `android-database-sqlcipher` package.

The original `android-database-sqlcipher` project is deprecated and identifies `sqlcipher-android` as its long-term replacement. SQLCipher supports integration with Room through the AndroidX SQLite layer.

Exact:

- cipher parameters
- KDF configuration
- key derivation
- Keystore wrapping
- migration strategy

remain **[ARCHITECTURE PHASE]** decisions.

**[TECHNICAL CONSTRAINT] — verified addition:** the now-deprecated
`android-database-sqlcipher` package stopped receiving updates specifically
because newer Android devices require native libraries to support 16KB
memory page sizes, and that library was never rebuilt for it. This isn't
only a Play Store submission rule — it affects whether SQLCipher's native
library loads correctly on newer-hardware devices at all, regardless of
distribution channel. `sqlcipher-android` (`net.zetetic:sqlcipher-android`)
is the actively maintained replacement and already addresses this; confirm
you're on a current release of it at implementation time rather than
copying an example that still references the old package name.

---

# 9. Preferences

## 9.1 Jetpack DataStore

**[RECOMMENDED]**

Use DataStore for small settings and app preferences:

- theme
- notification preferences
- selected UI preferences
- feature flags
- onboarding completion
- local configuration

Do not put relational application data in DataStore.

Android specifically positions DataStore for small key-value or typed data and recommends Room for larger/relational data.

---

# 10. Navigation

## 10.1 Navigation Compose

**[RECOMMENDED]**

Use Navigation Compose for screen/destination navigation.

Potential destinations:

```text
Home / Hero
Schedule
Tasks
Habits
Study
Focus
Capture Inbox
Companion
Shop
Settings
Backup & Restore
```

Navigation remains separate from business logic.

Android's navigation guidance specifically recommends Navigation Compose for apps built entirely with Compose.

---

# 11. Notifications & Reminders

## 11.1 Android Notification APIs

**[CONFIRMED]**

Use Android notifications and notification channels for:

```text
Routine
Important
Urgent
```

The app controls content and classification, while Android controls final presentation behavior such as heads-up behavior.

## 11.2 AlarmManager

**[CONFIRMED / TECHNICAL CONSTRAINT]**

Use AlarmManager only when precise user-facing timing is actually required.

Do not use exact alarms for ordinary background processing.

Android recommends inexact alarms whenever possible and reserves exact alarms for precise user-facing timing. Exact-alarm access is subject to Android permission/special-access rules.

The implementation must check exact-alarm capability before scheduling and provide a graceful fallback.

---

# 12. Background Work

## 12.1 WorkManager

**[CONFIRMED]**

Use WorkManager for persistent, deferrable work such as:

- cleanup
- maintenance
- retryable local operations
- non-urgent background processing
- optional future network work

Do not use WorkManager as a replacement for precise user-facing alarms.

WorkManager is Android's recommended library for persistent work.

---

# 13. Focus & Distraction Intervention

## 13.1 AccessibilityService

**[CONFIRMED / TECHNICAL CONSTRAINT]**

AccessibilityService is the selected technical mechanism for the v1 intervention concept, but it is **not a normal background API**.

Requirements:

- explicit user enablement
- minimum necessary data collection
- no screen-text persistence
- no password/OTP/private-message collection
- emergency/telecom/system-critical interfaces must bypass intervention
- OEM behavior must be tested
- service may be disabled by the user/system

Android states that AccessibilityService is a specialized service intended for assistive tools and that the user explicitly enables it in system settings.

Therefore the product must not promise universal or unbypassable blocking.

## 13.2 UsageStatsManager

**[CONFIRMED]**

Use UsageStatsManager for:

- historical usage statistics
- daily/weekly distraction analytics

Do not treat it as the primary real-time blocking mechanism.

---

# 14. Voice Capture & NLP

## 14.1 Speech recognition

**[CONFIRMED / ARCHITECTURE PHASE]**

Primary Android voice-capture interface:

```text
Android SpeechRecognizer
```

Prefer on-device recognition when the device reports that it is available.

Android provides `createOnDeviceSpeechRecognizer()` and an availability check. On-device availability is device-dependent.

If speech recognition is unavailable:

```text
Voice unavailable
      ↓
Manual text entry
```

Do not create a fake transcript when speech recognition fails.

## 14.2 Parsing

**[CONFIRMED] Deterministic Kotlin parser**

Pipeline:

```text
Voice
  ↓
Speech-to-text
  ↓
Text
  ↓
Deterministic parser
  ↓
Confidence evaluation
  ↓
Preview / Inbox
  ↓
Schema validation
  ↓
Business rules
  ↓
Repository
```

The parser should extract structured fields such as:

- title
- date
- time
- duration
- item type
- subject
- tags
- urgency

DayType is extracted only when explicitly stated; otherwise the scheduler derives it.

## 14.3 Serialization

**[RECOMMENDED] kotlinx.serialization**

Use it for:

- structured parser output
- backup serialization
- strongly typed JSON
- optional AI structured responses

AI or parser output must never directly mutate persistent records.

---

# 15. AI

## 15.1 Core v1 rule

**[CONFIRMED] AI is optional, not required.**

The application must remain fully functional without cloud AI.

Core functionality must not depend on:

- Gemini availability
- internet connectivity
- cloud authentication
- cloud model quotas

## 15.2 Cloud Gemini

**[OPTIONAL / DEFERRED]**

If cloud AI is enabled later, prefer **Firebase AI Logic for Android** as the client integration layer rather than placing a raw Gemini API key directly in the application.

Firebase AI Logic provides Android Kotlin/Java SDKs for Gemini access and supports App Check protection.

**[ARCHITECTURE PHASE] — verified update:** as of July 2026, Firebase's
console-guided AI Logic setup automatically enforces App Check rather than
leaving it optional. If cloud Gemini is ever enabled, local development
will need the App Check debug provider configured to bypass attestation —
otherwise even local test calls fail, not just production ones. This is a
small but easy-to-miss setup step beyond what most existing tutorials
describe.

This is a future capability, not a v1 dependency.

## 15.3 Gemini Live API

**[DEFERRED]**

Do not make Gemini Live API part of the v1 core voice architecture.

Personal-Tracker's v1 requirement is user-initiated local voice capture plus deterministic parsing. Real-time bidirectional Gemini audio interaction is a separate capability and should only be introduced if a later product decision requires it. Google's Live API is a real-time bidirectional API using persistent sessions/WebSockets and is not necessary for basic quick capture.

## 15.4 On-device generative AI

**[OPTIONAL / DEFERRED]**

On-device generative AI may be evaluated later through supported Android/Firebase AI Logic/ML Kit capabilities.

It must remain capability-detected and must never become a hardware requirement.

Current Google documentation describes on-device generative inference as device-dependent and currently limited in supported capabilities.

---

# 16. Security

## 16.1 Android Keystore

**[CONFIRMED]**

Use Android Keystore as the key-management boundary.

Do not:

- hard-code encryption keys
- store plaintext database keys in source
- transmit local encryption keys to a backend

Exact key-wrapping and lifecycle decisions remain **[ARCHITECTURE PHASE]**.

## 16.2 Local-first security boundary

**[CONFIRMED]**

v1 should have:

- encrypted local database
- encrypted user-initiated backup
- no default cloud sync
- no telemetry requirement
- no advertising
- raw voice audio disposal after processing
- minimum AccessibilityService data collection

---

# 17. Backup & Restore

## 17.1 Backup format

**[CONFIRMED]**

Use:

```text
.ptbackup
```

Pipeline:

```text
Room
 ↓
Validated export model
 ↓
Serialization
 ↓
Encryption + authentication/integrity protection
 ↓
.ptbackup
```

## 17.2 Restore

```text
.ptbackup
 ↓
Password verification
 ↓
Cryptographic integrity/authentication verification
 ↓
Format validation
 ↓
Schema validation
 ↓
Temporary safety snapshot
 ↓
Transactional restore
```

If restoration fails:

```text
Rollback
 ↓
Existing database remains intact
```

Exact cryptographic algorithm/KDF/parameter selection is **[ARCHITECTURE PHASE]**.

---

# 18. Animation & Motion

## 18.1 Primary animation system

**[RECOMMENDED] Compose Animation**

Use native Compose animation APIs for:

- Hero Card state changes
- task completion
- reward feedback
- Companion reactions
- progress changes
- timeline transitions
- navigation transitions
- intervention state changes

Compose provides built-in APIs for visibility, content, size, value, and transition animation.

## 18.2 Lottie

**[OPTIONAL]**

Lottie should not be a default dependency.

Use it only when a specific pre-rendered animation cannot be efficiently implemented with Compose.

This keeps the core stack smaller and avoids unnecessary animation dependencies.

---

# 19. Testing

## 19.1 Unit tests

**[RECOMMENDED] JUnit + Kotlin test utilities**

Test:

- DayType resolution
- scheduling
- rescheduling
- completion validation
- Coin rules
- streak recovery
- urgency classification
- intervention policy
- parser behavior
- backup validation
- schema migration logic

## 19.2 Flow tests

**[RECOMMENDED] Turbine**

Use Turbine where useful for testing Kotlin Flow/StateFlow behavior.

## 19.3 UI tests

**[RECOMMENDED] Compose UI tests**

Test:

- Hero Card
- DayType switching
- quick capture
- confirmation cards
- Focus flow
- recovery
- Companion/shop
- backup/restore screens

Android recommends automated UI tests for validating user interactions and critical flows.

## 19.4 Instrumented / real-device tests

**[CONFIRMED] Required**

Real-device validation is mandatory for:

- AccessibilityService
- notifications
- exact alarms
- reboot recovery
- battery restrictions
- voice recognition
- device-specific behavior
- backup/restore
- Android version compatibility

---

# 20. Build & Toolchain

## 20.1 Android Studio

**[RECOMMENDED]**

Use the latest stable Android Studio release available when implementation begins.

**Verified:** as of August 2026, Android Studio Quail 3 (Patch 1) is the current stable release, with Quail 4 already in Canary — confirming this document's "latest stable at implementation time" approach is the right one rather than pinning a version, since Android Studio ships a new stable roughly every 4-6 weeks.

## 20.2 Gradle

**[RECOMMENDED] Gradle + Android Gradle Plugin + Kotlin DSL**

Use:

```text
build.gradle.kts
settings.gradle.kts
libs.versions.toml
```

Use the Gradle Version Catalog to centralize dependency versions.

Do not hard-code dependency versions throughout module build files.

## 20.3 Kotlin

**[RECOMMENDED] Kotlin 2.4.x**

**Verified:** Kotlin 2.4.x was confirmed the current stable line as of August 2026 (2.4.20 scheduled for September 2026, 2.5.0 for December 2026). Use the latest 2.4.x patch at implementation time rather than pinning 2.4.10 specifically — by the time this project starts coding, a newer bug-fix release will likely exist.

Exact Kotlin/AGP compatibility should be resolved together during project bootstrap.

## 20.4 KSP

**[RECOMMENDED]**

Use Kotlin Symbol Processing where required by libraries such as Room/Hilt.

Do not introduce KAPT unless a required dependency still needs it.

---

# 21. Android SDK Strategy

The PRD previously defined Android 13–16 / API 33–36 as a **provisional technical target**.

Do not silently convert that provisional statement into a permanent support contract.

### Recommended implementation strategy

```text
compileSdk → latest stable Android SDK available at implementation time
targetSdk  → latest stable target supported by the selected AGP/Android Studio
minSdk     → API 33 for the current v1 scope, unless architecture validation changes it
```

Android 17 (API 37) is confirmed real and in active rollout as of mid-2026, though its exact GA date relative to this project's implementation start should be re-checked rather than assumed — Android major versions have occasionally shifted their public release timing. This is exactly why this document's compileSdk/targetSdk/minSdk strategy below correctly says "latest stable at implementation time" instead of hardcoding 37 as a target now.

**[ARCHITECTURE PHASE]** Final min/target/compile SDK combination must be validated against:

- AccessibilityService behavior
- exact alarms
- notifications
- SQLCipher native compatibility
- voice recognition
- OEM behavior
- Play/distribution requirements

---

# 22. Network Boundary

## v1

**[CONFIRMED] Network is optional.**

The app must work without:

- internet
- account
- backend
- cloud database
- cloud AI

If network access exists, it may be used only by explicitly enabled optional capabilities.

```text
Core application
      ↓
100% local
      ↓
Network optional
```

---

# 23. What Is Intentionally NOT in the v1 Stack

| Technology / Capability | Status | Reason |
|---|---|---|
| Supabase | [DEFERRED] | No v1 cloud backend |
| PostgreSQL | [DEFERRED] | No v1 cloud database |
| Cloud synchronization | [DEFERRED] | Outside frozen v1 scope |
| Authentication | [DEFERRED] | Single-user personal application |
| Firebase Analytics | [DEFERRED] | No telemetry requirement |
| Cloud storage | [DEFERRED] | Manual local backup is the v1 model |
| Gemini Live API | [DEFERRED] | Not required for local quick capture |
| Always-listening wake word | [EXCLUDED] | Explicit v1 non-goal |
| Flutter | [EXCLUDED] | Native Android requirement |
| React Native | [EXCLUDED] | Native Android requirement |
| Web frontend | [EXCLUDED] | Mobile-only product |
| Retrofit | [DEFERRED] | No v1 remote REST backend |
| Ktor client | [DEFERRED] | No v1 remote backend requirement |
| Paging (androidx.paging) | [OPTIONAL] | Current personal-scale dataset does not justify paging complexity; revisit only if a list view becomes large enough to affect scroll performance |
| Lottie | [OPTIONAL] | Compose animation is sufficient unless a specific asset requires Lottie |

---

# 24. Architecture Dependency Map

```text
                         PERSONAL-TRACKER v1
                                  │
                         Native Android / Kotlin
                                  │
                         Jetpack Compose + M3
                                  │
                    ViewModel + UDF + StateFlow
                                  │
                    ┌─────────────┴─────────────┐
                    │                           │
              Domain / Rules                 UI State
                    │
               Repositories
                    │
        ┌───────────┼───────────────────┐
        │           │                   │
      Room       DataStore        Android APIs
        │                              │
    SQLite +                      ┌────┼──────────────┐
   SQLCipher                      │    │              │
                                  │    │              │
                           Notifications AlarmManager  WorkManager
                                  │
                           AccessibilityService
                                  │
                           UsageStatsManager
                                  │
                           SpeechRecognizer
                                  │
                           Android Keystore
                                  │
                         Backup / Restore
                                  │
                    ┌─────────────┴─────────────┐
                    │                           │
              Local parser                Optional AI
                    │                           │
             kotlinx.serialization      Firebase AI Logic
                    │                           │
                    └────────── Optional ───────┘
```

---

# 25. Core Data Ownership

For v1:

```text
Room / SQLCipher
        ↓
PRIMARY SOURCE OF TRUTH
        ↓
Repositories
        ↓
ViewModels
        ↓
Compose
```

There is no remote source of truth.

There is no cloud synchronization layer.

There is no last-write-wins conflict-resolution system in v1 because there are no competing cloud/local replicas.

If cloud sync is introduced in a future version, conflict resolution becomes a new architecture decision.

---

# 26. Status Audit of the Previous File

## Explicit status tags previously present

The previous file did **not** use formal bracketed status tags such as `[CONFIRMED]` or `[OPEN DECISION]`.

It used only the prose section:

```text
Confirmed requirements
Recommended engineering choices
Keep open until validated
```

### Revised status system

This document standardizes those concepts into explicit tags:

```text
[CONFIRMED]
[RECOMMENDED]
[OPTIONAL]
[OPEN DECISION]
[TECHNICAL CONSTRAINT]
[ARCHITECTURE PHASE]
[DEFERRED]
[EXCLUDED]
```

This prevents implementation recommendations from being mistaken for product requirements.

---

# 27. Final Recommended Stack

## Production v1

```text
Android
Kotlin
Android Studio
Gradle Kotlin DSL
KSP

Jetpack Compose
Material 3
Navigation Compose

ViewModel
StateFlow
Coroutines
Flow
Repository pattern
UDF
Selective domain/use-case layer
Hilt

Room
SQLite
SQLCipher for Android
DataStore
kotlinx.serialization

Notification APIs
AlarmManager
WorkManager

AccessibilityService
UsageStatsManager

SpeechRecognizer
Deterministic Kotlin parser

Android Keystore
Encrypted .ptbackup backup/restore

JUnit
Turbine
Compose UI Test
Instrumented / real-device testing
```

## Optional / future

```text
Firebase AI Logic
Gemini Developer API
On-device generative AI where supported
Gemini Live API if a future product decision requires real-time AI conversation
Lottie for specific animation assets
Cloud backend / synchronization only after a new product decision
```

---

# 28. Final Engineering Principles

1. **Local-first is real, not a cache strategy.**
2. **Room is the v1 source of truth.**
3. **The UI never writes directly to data sources.**
4. **Business rules stay outside Composables.**
5. **AI never directly mutates persistent data.**
6. **Voice failure always has a manual-entry fallback.**
7. **Android system permissions and OEM behavior are treated as constraints, not guarantees.**
8. **Exact alarms are reserved for genuinely precise user-facing reminders.**
9. **AccessibilityService is explicit opt-in and minimum-data.**
10. **Cloud AI is optional and never required for core operation.**
11. **Cryptographic implementation details belong to the Architecture/Security phase.**
12. **The technology stack must not expand the frozen v1 product scope.**
