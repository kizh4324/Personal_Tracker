# Personal-Tracker — Documentation Index

This directory houses the complete specifications, design systems, creative briefs, and status reports for **Personal-Tracker v1**.

---

## 📂 Documentation Directory Structure

```
docs/
├── specifications/                           # Core Technical & Product Specifications
│   ├── TECHSTACK.md                          # Authoritative Android Tech Stack Specification
│   ├── architecture.md                       # Comprehensive System Architecture & Security Spec
│   ├── PROBLEM_AND_SOLUTION_BRIEF.md         # 10 Core Problem Statements & Identified Outcomes
│   ├── PROJECT_MASTER_OUTLINE.md             # Master Roadmap, Feature Boundaries & Build Plan
│   └── archive/                              # Superseded Drafts & Evolution History
│       └── Personal-Tracker-TECHSTACK-v2.md  # Historical Tech Stack v2 Draft
│
├── design/                                   # Visual Design System & Asset Briefs
│   ├── design-system.md                      # Authoritative Design System (Tokens, Typography, Motion)
│   ├── companion-asset-brief.md              # Rive 2D Vector Character Design Creative Brief
│   ├── icon-inventory.md                     # System Vector Icon & Flame State Catalog
│   ├── personal-tracker-design-system-research.md # Psychological Research & Visual Foundations
│   └── prompts/                              # Extraction & Implementation Guides
│       ├── antigravity-design-system-extraction-prompt.md # Design Token Extraction Prompt
│       └── antigravity-frontend-implementation-prompt.md  # Multi-Track Frontend Build Instructions
│
└── status/                                   # Implementation & Quality Verification Reports
    └── frontend-implementation-status.md     # Production-Ready Verification Report (Tracks A–F)
```

---

## 📑 Document Catalog & Purpose

### 1. Specifications (`docs/specifications/`)
* **[`TECHSTACK.md`](./specifications/TECHSTACK.md)**: The single source of technical truth for Android dependencies (Kotlin 2.4.0, Compose BOM 2026.02.00, Hilt 2.55, SQLCipher 4.6.1, Rive 9.1.0) and build configuration.
* **[`architecture.md`](./specifications/architecture.md)**: Comprehensive architectural blueprint covering SQLCipher Keystore isolation, AI Action Boundary, dual voice router, event-driven accessibility interceptor, and AES-256-GCM `.ptbackup` engine.
* **[`PROBLEM_AND_SOLUTION_BRIEF.md`](./specifications/PROBLEM_AND_SOLUTION_BRIEF.md)**: Deep breakdown of user pain points across routine tracking, screen-time drift, delivery urgency, and non-punitive gamification.
* **[`PROJECT_MASTER_OUTLINE.md`](./specifications/PROJECT_MASTER_OUTLINE.md)**: Master outline defining the product vision, solo builder model, and feature scope.

### 2. Design & Assets (`docs/design/`)
* **[`design-system.md`](./design/design-system.md)**: The foundational token dictionary (Colors, 4dp Grid, Plus Jakarta Sans, Inter tabular scale, 4-tier elevation, Bézier VSYNC motion invariants).
* **[`companion-asset-brief.md`](./design/companion-asset-brief.md)**: The creative brief for human Rive animators detailing the 7-state mascot machine (`companion_sm` states 0–6).
* **[`icon-inventory.md`](./design/icon-inventory.md)**: Mapping of Material Symbol icons, custom VectorDrawables, and the 4-state streak flame system.
* **[`personal-tracker-design-system-research.md`](./design/personal-tracker-design-system-research.md)**: Research foundation connecting color psychology, attention economics, and habit-formation mechanics to UI choices.

### 3. Implementation Status (`docs/status/`)
* **[`frontend-implementation-status.md`](./status/frontend-implementation-status.md)**: Detailed audit verifying production-readiness of UI tokens (`Color.kt`, `Type.kt`, `Shape.kt`, `Spacing.kt`, `Elevation.kt`, `Theme.kt`), vector drawables, and Bézier motion scripts.

---

## 🔗 Related Planning Artifacts

For sprint planning, PRDs, and UX spines, refer to:
* **[`_bmad-output/planning-artifacts/epics.md`](../_bmad-output/planning-artifacts/epics.md)** — Authoritative 6-Epic / 29-Story breakdown.
* **[`_bmad-output/planning-artifacts/sprint-summary.md`](../_bmad-output/planning-artifacts/sprint-summary.md)** — Sprint governance and invariant registry.
* **[`_bmad-output/planning-artifacts/ux-designs/ux-Personal-Tracker-2026-08-15/`](../_bmad-output/planning-artifacts/ux-designs/ux-Personal-Tracker-2026-08-15/)** — Visual (`DESIGN.md`) and Interaction (`EXPERIENCE.md`) UX spines.
* **[`_bmad-output/planning-artifacts/prds/prd-personal-tracker-2026-08-14/`](../_bmad-output/planning-artifacts/prds/prd-personal-tracker-2026-08-14/)** — Product Requirements Document.
