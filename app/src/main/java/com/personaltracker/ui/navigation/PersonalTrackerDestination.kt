package com.personaltracker.ui.navigation

/**
 * Navigation destinations for the 5-tab Bottom Navigation Bar.
 * Sourced 1:1 from `EXPERIENCE.md` §2 Information Architecture and `UX-DR18`.
 */
enum class PersonalTrackerDestination(
    val route: String,
    val label: String,
    val contentDescription: String
) {
    HOME(
        route = "home",
        label = "Home",
        contentDescription = "Navigate to Home schedule and active hero card"
    ),
    HABITS(
        route = "habits",
        label = "Habits",
        contentDescription = "Navigate to Habit cadences and streak tracking"
    ),
    STUDY(
        route = "study",
        label = "Study",
        contentDescription = "Navigate to Study timers and subject heatmaps"
    ),
    COMPANION(
        route = "companion",
        label = "Companion",
        contentDescription = "Navigate to Companion pet and cosmetic shop"
    ),
    SETTINGS(
        route = "settings",
        label = "Settings",
        contentDescription = "Navigate to App settings, diagnostics, and backup"
    );

    companion object {
        val bottomNavTabs = listOf(HOME, HABITS, STUDY, COMPANION, SETTINGS)

        fun fromRoute(route: String?): PersonalTrackerDestination {
            return entries.firstOrNull { it.route == route } ?: HOME
        }
    }
}
