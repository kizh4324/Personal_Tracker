package com.personaltracker.domain.model

/**
 * 4-tier streak progression driving tiered habit coin bonuses (FR-2.4, FR-8.1, AD-4).
 *
 * Tier definitions:
 * - COLD: 0 days (+0 coins)
 * - WARMING: 1..2 days (+1 coin)
 * - BURNING: 3..6 days (+2 coins)
 * - BLAZING: 7+ days (+3 coins)
 */
enum class StreakTier {
    COLD,
    WARMING,
    BURNING,
    BLAZING;

    companion object {
        fun fromStreak(streak: Int): StreakTier {
            require(streak >= 0) { "Streak must be non-negative: $streak" }
            return when (streak) {
                0 -> COLD
                in 1..2 -> WARMING
                in 3..6 -> BURNING
                else -> BLAZING
            }
        }
    }
}
