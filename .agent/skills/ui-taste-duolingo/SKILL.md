---
name: ui-taste-duolingo
description: Duolingo Minimal Tactile design language rules and Framer-to-Compose motion translation for Personal-Tracker UI styling, tactile 3D lip geometry, and unclipped reward motion.
---

# Duolingo Minimal Tactile: Design & Motion Language

The **Duolingo Minimal Tactile** design skill governs the visual styling, tactile geometry, color semantics, and spring physics across **Personal-Tracker**. It delivers high-contrast clarity, playful physical tactility, and zero cognitive fatigue while adhering strictly to Personal-Tracker's architectural invariants.

---

## 1. Canvas, Framing & Spacing

A hyper-clean canvas ensures that interactive tactile elements and semantic accents feel prominent, inviting, and uncluttered.

* **Canvas Colors:**
  * **Light Theme:** Pristine high-contrast white `#FFFFFF` (`surfaceRaised` / pure canvas).
  * **Dark Theme:** Deep obsidian navy `#131F24` (`surfaceBase`).
* **Outer Margins:**
  * Strict **16dp** outer gutters (`PersonalTrackerTheme.spacing.marginMobile` or `spacing.screenPaddingHorizontal`).
  * Never stretch interactive cards edge-to-edge on mobile viewports; cards float cleanly within the 16dp frame.
* **Vertical Rhythm & Gutters:**
  * Card-to-card gap: **12dp - 16dp** (`spacing.spacing3` or `spacing.spacing4`).
  * Section gap: **24dp** (`spacing.sectionGap`).
* **Visual De-Cluttering:**
  * **Zero 1px hair dividers:** Separation is achieved through distinct card surfaces and whitespace, not hairline strokes.
  * **Zero noisy gradients:** All backgrounds, cards, and containers use solid, confident fill colors.

---

## 2. Tactile 3D Lip Geometry

The signature aesthetic is physical, chunky, and toy-like ("Duolingo Tactile"), characterized by flat surfaces with extruded solid bottom edges rather than diffuse drop shadows.

```
+───────────────────────────+
│   2dp Solid Border        │  ▲
│                           │  │  Top Surface
│   [ CTA / Action Icon ]   │  │
│                           │  ▼
├───────────────────────────┤
│   4dp Solid Bottom Lip    │  ▼ Extrusion (Darker shade)
+───────────────────────────+
```

### Geometry Specification:
1. **0dp Shadow Blur:** Drop-shadow blur radius is strictly `0.dp`. Absolutely no fuzzy ambient Gaussian blur.
2. **2dp Solid Border:** Clean perimeter stroke of `2.dp` using border tokens (`PersonalTrackerTheme.colors.borderDefault` or a darker tone of the element's primary hue).
3. **4dp Bottom Lip Extrusion:** A solid 4dp offset lip at the bottom of cards and buttons. The lip color is a calculated 15–20% darker tone of the primary surface.
4. **Physical Press Dynamics:**
   * **Resting State:** Elevation is 4dp (`bottomLip = 4.dp`, `offsetY = 0.dp`).
   * **Pressed State:** The top surface translates down by **4dp** (`offsetY = 4.dp`), visually compressing the bottom lip to 0dp.
   * **Spring Settle:** Releases with an instant mechanical snap back to resting position.

### Canonical Compose Pattern:
```kotlin
@Composable
fun TactileLipButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = PersonalTrackerTheme.colors.primary,
    lipColor: Color = PersonalTrackerTheme.colors.primaryDark,
    borderColor: Color = PersonalTrackerTheme.colors.borderDefault,
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val spacing = PersonalTrackerTheme.spacing
    val shapes = PersonalTrackerTheme.shapes

    val translationY by animateDpAsState(
        targetValue = if (isPressed) 4.dp else 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "TactileLipPress"
    )

    Box(
        modifier = modifier
            .defaultMinSize(minWidth = spacing.minTouchTarget, minHeight = spacing.minTouchTarget)
            .clickable(
                interactionSource = interactionSource,
                indication = null, // Custom physical translation replaces standard ripple
                onClick = onClick
            )
    ) {
        // 1. Bottom Extrusion Lip (Fixed underneath)
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(top = 4.dp)
                .background(color = lipColor, shape = shapes.md)
                .border(width = 2.dp, color = borderColor, shape = shapes.md)
        )

        // 2. Interactive Surface (Translates down by 4dp on press)
        Box(
            modifier = Modifier
                .offset(y = translationY)
                .matchParentSize()
                .padding(bottom = 4.dp)
                .background(color = containerColor, shape = shapes.md)
                .border(width = 2.dp, color = borderColor, shape = shapes.md),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                content = content
            )
        }
    }
}
```

---

## 3. Semantic Restraint & Palette Discipline

Bright colors command attention; therefore, vibrant saturated colors are strictly reserved for achievement, progress, and conversion moments.

* **Neutral Ground:**
  * Card bodies, task containers, routine lists, and dialogs use neutral tones (`surfaceRaised`, `surfaceBase`, `surfaceSunken`).
* **Protected Vibrant Accents:**
  1. **Streak Flame:** `#FF9600` (`streakFlameCore` / fiery energetic orange) — strictly reserved for active streaks, fire badges, and heat indicators.
  2. **Coin & Economy:** `#FFC800` (`coinGold` / rich canary yellow) — strictly reserved for coin icons, balance counters, and gamification rewards.
  3. **Primary Action / Affirmation CTA:** `#58CC02` (iconic Duolingo electric leaf green) — strictly reserved for the primary Hero card CTA, completion checkboxes, and "Continue" buttons.
  4. **DayType Indicators:** Specific functional tones (`daytypeWeekday`, `daytypeWeekend`, `daytypeCollege`, `daytypeSpecial`).
* **Anti-Pattern Trap:** Never tint background panels or body text with vibrant saturated greens or yellows. Keep the backdrop peaceful so the reward moments pop.

---

## 4. Framer-to-Compose Motion Translation

When prototyping spring dynamics in **Framer** or translating Framer motion curves to Jetpack Compose, use the deterministic translation mapping below.

### Dynamic Mapping Matrix:

| Interaction Type | Framer Parameters (`type: "spring"`) | Compose Equivalent Spec | Damping Ratio ($\zeta$) | Stiffness ($k$) |
| :--- | :--- | :--- | :--- | :--- |
| **Tactile Button Press** | `stiffness: 500, damping: 30, mass: 1` | `spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessMedium)` | `0.65f` (Snappy, no wallow) | `StiffnessMedium` (~1500f) |
| **Card Expand / Sheet Snap** | `stiffness: 350, damping: 28, mass: 1` | `spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)` | `0.75f` (Low bounce) | `StiffnessLow` (~300f) |
| **Coin Arc Arrival** | `stiffness: 280, damping: 18, mass: 0.8` | `spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)` | `0.50f` (Playful pop) | `StiffnessLow` (~200f) |
| **Reward Milestone Pop** | `stiffness: 220, damping: 12, mass: 1` | `spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessVeryLow)` | `0.35f` (Elastic overshoot) | `StiffnessVeryLow` (~50f) |

### Conversion Heuristics:
* **Natural Frequency $\omega_0$:** $\omega_0 = \sqrt{\frac{k}{m}}$
* **Damping Ratio $\zeta$:** $\zeta = \frac{c}{2 \sqrt{k \cdot m}}$
* In Compose, `dampingRatio < 1f` is underdamped (bouncy). Personal-Tracker tactile interactions should standardise on $\zeta \in [0.5f, 0.75f]$ for controls and $\zeta \in [0.35f, 0.5f]$ for celebration bursts.
* **Hoisting Invariant (AD-7):** All celebration particles, coin arcs, and full-screen bursts must be hoisted to `RewardMotionStage` at the root scaffold level to prevent viewport and scroll clipping.

---

## 5. Architectural Guardrails (EXP-016, AD-2, AD-7)

Whenever this skill is engaged to restyle or animate UI components:

1. **Target Boundary:**
   * **Allowed Touches:**
     * `app/src/main/java/com/personaltracker/ui/theme/` (Colors, Shapes, Spacings, AnimationTokens)
     * `app/src/main/java/com/personaltracker/ui/components/` (Stateless atoms, cards, overlays, animation stages)
     * Stateless `*ScreenContent(state, onIntent)` composables in `app/src/main/java/com/personaltracker/ui/screens/`
   * **Strictly Prohibited Touches:**
     * `*ViewModel.kt` files (State logic is immutable to design passes)
     * `com.personaltracker.domain` (Pure Kotlin invariant)
     * `com.personaltracker.data` (Room / SQLCipher / Repositories)
     * `com.personaltracker.platform` (Services / Workers / Alarms)
2. **Subagent Mode:**
   * Any exploratory design swap, variant generation, or substantial restyle must run via `invoke_subagent` in **`Workspace: "branch"`** mode to keep the main tree clean until verified.
3. **Token Conformance (AD-7):**
   * Never introduce hardcoded hex colors (`Color(0xFF...)`) or raw integer dp/sp into composables.
   * Add tokens to `Color.kt`, `Spacing.kt`, or `AnimationTokens.kt` first, then consume via `PersonalTrackerTheme`.
   * Enforce the 48dp interactive floor (`spacing.minTouchTarget`).

---

## 6. MCP Toolchain Workflow (Figma + Framer + Stitch)

1. **Figma Inspection & Extraction:**
   * Use `figma-developer` MCP to inspect node styles, bounding boxes, and component hierarchies.
   * Use `figma-write-bridge` to synchronize tokens and layout variants back to canvas.
2. **Framer Motion Synthesis:**
   * Prototype complex transitions in Framer to verify spring curves and mass balance.
   * Export spring stiffness and damping parameters, then map to Compose specs via Section 4 above.
3. **Stitch Component Generation:**
   * Use `stitch` MCP (`generate_screen_from_text`, `generate_variants`) to prototype screen layouts against `docs/design/references/`.
4. **Multimodal Verification:**
   * Feed screenshots from `docs/design/references/` alongside device screenshots to Gemini multimodal models for automated pixel & taste verification.
