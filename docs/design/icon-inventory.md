# Personal-Tracker Icon Inventory Specification

Sourced 1:1 from `design-system.md` Iconography, Component Specs, and Navigation sections.

---

## 1. Iconography System Rules
- **Base Geometry**: 24dp bounding box (standard), 20dp (compact inline), 32dp (prominent / hero).
- **Stroke & Weight**: Material Symbols Rounded (weight 300 equivalent), 1.5dp standard stroke weight, rounded line caps and joins.
- **Variants**: Outline by default across all screens; Filled variant used exclusively for active selected navigation states.
- **Grid Alignment**: 4dp-aligned inner padding, optical centering.

---

## 2. Navigation Bar Icons (24dp)

| Navigation Destination | Outline Asset (Inactive) | Filled Asset (Active) | Source / Spec |
|---|---|---|---|
| **Timeline / Schedule** | `ic_nav_timeline_outline.xml` | `ic_nav_timeline_filled.xml` | Material Symbols `view_timeline` (Rounded, w300) |
| **Habits** | `ic_nav_habits_outline.xml` | `ic_nav_habits_filled.xml` | Material Symbols `repeat` / `check_circle` (Rounded, w300) |
| **Tasks** | `ic_nav_tasks_outline.xml` | `ic_nav_tasks_filled.xml` | Material Symbols `checklist` / `task_alt` (Rounded, w300) |
| **Focus** | `ic_nav_focus_outline.xml` | `ic_nav_focus_filled.xml` | Material Symbols `timer` / `self_improvement` (Rounded, w300) |
| **Companion** | `ic_nav_companion_outline.xml`| `ic_nav_companion_filled.xml`| Material Symbols `pets` / `emoji_nature` or Custom Silhouette |

---

## 3. Core Component & Action Icons

| Icon Name | Dimensions | Color Mapping | Usage |
|---|---|---|---|
| **Checkmark / Complete** | 20dp / 24dp | `colors.success` / `Color.White` | Habit tick, task checkoff, onboarding confirmation |
| **Add / Plus** | 20dp / 24dp | `colors.primary` / `Color.White` | FAB creation, "+ Add Habit", "+ Add Task" |
| **Clock / Time** | 20dp | `colors.inkSecondary` | Timeline time markers, duration indicators |
| **Calendar / DayType** | 20dp | `colors.inkSecondary` | Date picker, DayType switcher |
| **Urgent / Special (Lightning)**| 20dp | `colors.danger` | Urgent task indicator, Special Reminders |
| **Lock / Shield** | 20dp | `colors.danger` / `colors.warning` | App blocking rules, screen time lockouts |
| **Waveform (Voice FAB)** | 24dp | `Color.White` on `colors.primary` | Voice task quick capture ("Hi Gemini...") |
| **Chevron Right** | 20dp | `colors.inkTertiary` | List row drill-downs, settings rows |
| **Close / Dismiss (X)** | 20dp / 24dp | `colors.inkSecondary` | Modal sheet dismissal, cancel unblock |
| **Settings / Controls** | 24dp | `colors.inkSecondary` | Configuration and user preferences |
| **Filter / Sort** | 20dp | `colors.inkSecondary` | Task list filtering, DayType filters |
| **Priority Flags (P1–P4)** | 20dp | P1: `danger`, P2: `accentWarm`, P3: `primary`, P4: `inkTertiary` | Task Action Priority Matrix |
| **Streak Freeze (Snowflake)** | 16dp / 20dp | `colors.info` (#38BDF8 / #60A5FA) | Active streak freeze badge on flame icon and calendar |

---

## 4. Custom Bespoke Icon Assets (Design Required)

| Asset Name | Target Format | Visual Description | File Path |
|---|---|---|---|
| **Streak Flame (Cold)** | VectorDrawable (24dp) | Inactive ember silhouette, single gray stroke (`streakCool` #94A3B8) | `res/drawable/ic_flame_cold.xml` |
| **Streak Flame (Warming)** | VectorDrawable (24dp) | Small single-tone orange flame (`streakFlameCore` #F97316) | `res/drawable/ic_flame_warming.xml` |
| **Streak Flame (Burning)** | VectorDrawable (24dp) | Dual-tone flame with core (#F97316) and yellow tip (#FDE047) | `res/drawable/ic_flame_burning.xml` |
| **Streak Flame (Blazing)** | VectorDrawable (24dp) | Blazing flame with sparks and dual-tone intensity | `res/drawable/ic_flame_blazing.xml` |
| **Coin Currency Token** | VectorDrawable (20dp) | Flat gold disc with embossed "C" glyph and inner rim | `res/drawable/ic_coin.xml` |
| **Companion Notification** | VectorDrawable (24dp) | Simplified high-contrast companion face silhouette | `res/drawable/ic_companion_notification.xml` |
| **Voice Waveform** | VectorDrawable (24dp) | 5-bar dynamic acoustic waveform | `res/drawable/ic_waveform.xml` |
