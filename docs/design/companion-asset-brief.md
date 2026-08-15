# Companion Asset & Animation Creative Brief
### Personal-Tracker Android Application

This document provides a **complete, implementation-ready technical and artistic specification** for a human illustrator, 2D rigger, or Rive/Lottie motion designer.

---

## 1. Technical Contract (Non-Negotiable)

Any artwork delivered for the companion mascot **must conform 100% to this exact technical interface**. Android Compose code in `CompanionView.kt` already interfaces directly with these names and types.

| Parameter | Required Value | Notes |
|---|---|---|
| **Delivery File Format** | `.riv` (Rive Runtime Binary, runtime version 9+) | Deliver to `app/src/main/res/raw/companion.riv` |
| **Artboard Name** | `companion` | Case-sensitive |
| **State Machine Name** | `companion_sm` | Case-sensitive |
| **State Machine Input Name** | `state` | **Type: Number** |
| **Input Value Range** | `0.0` to `6.0` (Integer steps 0 through 6) | Mapped directly to `CompanionState` enum |
| **State Mappings** | `0` = Idle / Content<br>`1` = Celebrating<br>`2` = Encouraging<br>`3` = Concerned<br>`4` = Focused<br>`5` = Sleeping / Resting<br>`6` = Excited | Must support smooth cross-fade / interpolation between all states |
| **Target Render Sizes** | **24dp** (Simplified Notification Icon)<br>**64dp** (Home Widget / Header)<br>**120dp–200dp** (Full-Screen Focus & Reward Dialogs) | Must remain crisp and legible across all three scales |
| **File Size Budget** | **< 500 KB** total `.riv` binary | Optimized vector paths, minimal clipping masks |

---

## 2. Visual Style & Aesthetic Direction

*Sourced from `design-system.md` Illustration & Companion/Mascot Style Section.*

### Core Visual Principles
1. **Semi-Flat Vector Art**: Clean, precise geometric lines with soft organic contours. Avoid heavy 3D skeuomorphism, noisy bitmap textures, or overly glossy reflections.
2. **Proportions & Demographics**: **Avoid extreme chibi / infant proportions**. The companion should feel like an agile, wise, friendly guide (similar to an expressive creature/spirit), ensuring appeal across adult productivity users rather than feeling childish.
3. **Face-First Communication**: The face (eyes, eyebrows, mouth) carries 80% of the emotional state signal. Body posture provides secondary framing. At 24dp notification size, the silhouette and facial expression must read instantaneously.
4. **Shame-Free Safety Rule**: **NEVER design a "crying", "angry", "disappointed", or "guilt-inducing" expression**. Missed routines or streak interruptions trigger the **Concerned** state (caring check-in) or **Encouraging** state (warm welcome back), never punishment.

### Palette Constraints (Maximum 5 Colors)
The companion character must be built using ONLY these design token colors to ensure harmony with the dark and light mode UI:

| Role | Light Mode Hex | Dark Mode Hex | Usage |
|---|---|---|---|
| **Companion Body / Primary** | `#059669` (Emerald 600) | `#34D399` (Emerald 400) | Main body, ears/crest, primary silhouette |
| **Companion Light Accent** | `#10B981` (Emerald 500) | `#10B981` (Emerald 500) | Belly patch, inner ear, wingtips, highlights |
| **Warm Accent (Glow / Energy)** | `#D97706` (Amber 600) | `#FBBF24` (Amber 400) | Celebration aura, blush cheeks, energy sparks |
| **Contour & Features** | `#18181B` (Zinc 900) | `#FAFAFA` (Zinc 50) | Eye pupils, smiling mouth stroke, contours |
| **Eye Highlights** | `#FFFFFF` | `#FFFFFF` | Specular eye catchlights |

---

## 3. Per-State Detailed Specifications (The 7 Expression States)

```
       ┌───────────► [1. Celebrating] ───────────┐
       │                   ▲                     │
       │                   │                     ▼
[0. Idle] ◄────────► [2. Encouraging] ◄────► [6. Excited]
       │                   │                     ▲
       │                   ▼                     │
       └───────────► [3. Concerned] ─────────────┘
       │
       ├───────────► [4. Focused]
       │
       └───────────► [5. Sleeping]
```

### State 0: Idle / Content (`state = 0`)
- **Trigger**: Default resting state on dashboard and timeline screens.
- **Pose & Expression**: Relaxed standing or hovering posture, soft smiling eyes with specular catchlights, slight head tilt.
- **Animation Loop**: Continuous, slow, rhythmic breathing cycle (1.8s loop). Subtle ear/feather micro-flutter every 4–5 seconds.
- **Notification Icon**: High-contrast smiling face silhouette.

### State 1: Celebrating (`state = 1`)
- **Trigger**: Single task checked off, routine item completed, daily goal hit.
- **Pose & Expression**: Arms/wings raised in triumph, joyful eye crescents (`^ ^`), open smiling mouth.
- **Animation Motion**: Upward hop with spring overshoot (`spring(0.34, 1.56)`), emitting 3–4 tiny floating stars/particles in `accentWarm` (#D97706) and `coinGold` (#F59E0B). Duration: ~800ms before returning to Idle.
- **Notification Icon**: Companion with star sparkles around head.

### State 2: Encouraging (`state = 2`)
- **Trigger**: Returning to the app after an absence, activating a Streak Freeze, opening a rescheduling flow.
- **Pose & Expression**: Welcoming wave gesture, soft reassuring smile, open posture leaning slightly forward.
- **Animation Loop**: Gentle two-handed wave or welcoming gesture (1.2s loop) with a warm amber cheek blush.
- **Notification Icon**: Companion with a friendly waving hand.

### State 3: Concerned (`state = 3`)
- **Trigger**: Routine running behind schedule, streak at risk before day's end, screen-time limit approaching.
- **Pose & Expression**: Caring, attentive gaze (one raised eyebrow, head tilted forward, small rounded mouth ("o") of concern). **MUST NOT LOOK SAD, CRYING, OR ANGRY**.
- **Animation Loop**: Subtle investigative lean, soft blinking, hands held together (1.5s loop).
- **Notification Icon**: Companion face with raised inquisitive eyebrows.

### State 4: Focused (`state = 4`)
- **Trigger**: Active Focus / Deep Work timer running.
- **Pose & Expression**: Calm meditative pose (seated or gently floating), eyes closed in peaceful concentration, centered posture.
- **Animation Loop**: Very slow, deep breathing loop (4.0s cycle: 2s in, 2s out), faint ambient aura pulse in `primary` (#2563EB).
- **Notification Icon**: Meditating companion silhouette.

### State 5: Sleeping / Resting (`state = 5`)
- **Trigger**: Past configured shutdown time, off-day status, sleep routine active.
- **Pose & Expression**: Curled up comfortably, eyes peacefully closed, gentle smile, resting cap/blanket accessory.
- **Animation Loop**: Slow sleep breathing with tiny floating "Zzz" vector glyphs floating upward and fading out (2.5s loop).
- **Notification Icon**: Sleeping face with small crescent moon.

### State 6: Excited (`state = 6`)
- **Trigger**: Major milestone achieved (7-day streak, 100 Coins accumulated, level-up, companion evolution).
- **Pose & Expression**: High-energy celebratory pose, glowing eyes, continuous joyful bouncing or 360° spin.
- **Animation Motion**: Multi-stage celebration with energetic particle burst (confetti ribbons in DayType colors), radiant sunburst aura. Duration: 1.5s loop.
- **Notification Icon**: Companion wearing a crown or laurels.

---

## 4. Adventuring Illustrations (Secondary Priority — Static Vector Scenes)

Per Finch-inspired adventure mechanics, completed activities grant energy that sends the companion on brief narrative "adventures".

### Style Specifications:
- **Format**: Static SVG / VectorDrawable (Rendered at `320dp × 180dp` aspect ratio 16:9).
- **Color Backgrounds**: Uses DayType palette tints with 15% opacity:
  - *Weekday Study/Work*: Tinted in `daytypeWeekday` (`#3B82F6`) — Companion reading a book at a minimalist wooden desk.
  - *Weekend Leisure/Nature*: Tinted in `daytypeWeekend` (`#8B5CF6`) — Companion hiking a gentle hillside trail at sunset.
  - *College / Academy*: Tinted in `daytypeCollege` (`#06B6D4`) — Companion standing in a campus courtyard with a backpack.
  - *Special / Milestone*: Tinted in `daytypeSpecial` (`#F59E0B`) — Companion gazing at a glowing campfire with starry night sky.

---

## 5. Lottie Reward Animations Creative Brief

| Asset Name | Target File | Dimensions | Target Duration | Description & Palette |
|---|---|---|---|---|
| **Coin Earn Micro** | `app/src/main/assets/animations/coin_earn_micro.json` | 48dp × 48dp | **250ms–300ms** | Single coin scaling up with spring overshoot, emitting a soft amber pulse (`#F59E0B`), fading up and out. |
| **Milestone Full Reveal** | `app/src/main/assets/animations/milestone_full_reveal.json` | 200dp × 200dp | **800ms–1000ms** | Concentric radiant burst ring, rotating badge disc, and 12–16 fluttering confetti flakes in `accentWarm`, `coinGold`, and `companion` green. |

---

## 6. Deliverable Checklist for Artist
- [ ] `companion.riv` compiled with artboard `companion` and state machine `companion_sm`
- [ ] Number input `state` responding correctly to values `0.0` through `6.0`
- [ ] Verified clean rendering at 24dp notification scale
- [ ] `coin_earn_micro.json` Lottie JSON (< 40 KB)
- [ ] `milestone_full_reveal.json` Lottie JSON (< 120 KB)
- [ ] All assets verified under 60fps and 120fps playback
