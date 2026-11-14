package com.personaltracker.domain.engine

/**
 * Sourced 1:1 from docs/design/companion-asset-brief.md and design-system.md.
 * Canonical pure Kotlin 7-state companion emotional register (AD-1, AD-4).
 * Rive State Machine Input: "state" (Number, 0.0 to 6.0).
 */
enum class CompanionState(val ordinalValue: Int, val stateName: String, val emoji: String) {
    IDLE_COZY(0, "Idle / Content", "🦊"),
    CELEBRATING_REWARD(1, "Celebrating", "🎉"),
    ENCOURAGING_WARM(2, "Encouraging", "✨"),
    CONCERNED_CHECKIN(3, "Concerned", "🧐"),
    FOCUSING_STUDY(4, "Focused", "🧘"),
    RESTING_PEACEFUL(5, "Sleeping / Resting", "💤"),
    HAPPY_BOUNCE(6, "Excited", "⚡");

    val riveValue: Int get() = ordinalValue

    companion object {
        fun fromOrdinal(ordinal: Int): CompanionState =
            entries.firstOrNull { it.ordinalValue == ordinal } ?: IDLE_COZY

        fun fromRiveValue(value: Int): CompanionState = fromOrdinal(value)
    }
}
