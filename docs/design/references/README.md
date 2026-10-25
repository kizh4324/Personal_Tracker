# Design Reference Assets Directory

This directory serves as the canonical repository of visual design target references for **Personal-Tracker**. These assets are used by autonomous agents and Gemini multimodal vision pipelines for automated visual regression checking, tactile geometry verification, and taste alignment against our **Duolingo Minimal Tactile** design system.

---

## 1. Expected Reference Assets

Place the target reference mocks or golden screen renders directly in this folder:

| Filename | Screen Target | Core Elements to Validate |
| :--- | :--- | :--- |
| `home_ref.png` | **Home Feed** | Persistent Top Bar (`DayTypeChip`, `StreakFlameBadge`, `CoinPill`), 6-state `HeroActionCard`, 16dp outer gutters, and floating quick-capture FAB. |
| `habits_ref.png` | **Habits Screen** | `HabitStreakCard` hierarchy, weekly dot matrices, 4dp bottom lip tactile check buttons, and zero-guilt frozen streak states. |
| `study_ref.png` | **Study & Focus Screen** | Minimal circular timer display, delivery intensity pill tags (Routine, Important, Urgent), tactile session controls, and resting card geometry. |
| `companion_ref.png` | **Companion & Rewards Screen** | Centered companion avatar viewport, 7 non-punitive emotional registers, level milestone progress bar, and hoisted reward particle stage. |

---

## 2. Image Specifications

To optimize ingestion by Gemini multimodal models:

1. **Format:** Lossless PNG (`.png`). Avoid lossy JPEGs that introduce compression artifacts along solid lip borders.
2. **Resolution:** Native Android mobile viewport (recommended `1080 x 2400` px or `1080 x 2340` px at 400–440 dpi).
3. **Canvas Baseline:**
   * Light theme reference: `#FFFFFF` pure white canvas.
   * Dark theme reference: `#131F24` charcoal navy canvas (optional suffix: `_dark.png`).
4. **File Size:** Keep under 2.5 MB per image.

---

## 3. Gemini Multimodal Ingestion & Verification Workflow

Agents and engineers use these references to perform automated Visual QA during UI refinement loops:

### Step 1: Capture Live Device Screenshot
Always use intermediate file pulling to avoid PowerShell UTF-16 binary corruption:
```powershell
adb shell screencap -p /sdcard/screen_actual.png
adb pull /sdcard/screen_actual.png docs/design/references/screen_actual.png
```

### Step 2: Multimodal Prompting
Provide both `screen_actual.png` and `{target}_ref.png` to Gemini vision tools to evaluate:
* **Tactile 3D Lip Geometry:** Verify 2dp solid perimeter border and 4dp solid bottom extrusion without blurry drop shadows.
* **Margin & Spacing:** Confirm 16dp outer margins (`marginMobile`) and generous whitespace between cards.
* **Semantic Restraint:** Ensure vibrant accents are restricted exclusively to Streak Flames (`#FF9600`), Coins (`#FFC800`), DayType chips, and primary CTAs (`#58CC02`).
* **Visual De-Cluttering:** Verify zero 1px hairline dividers and zero gradient noise.
