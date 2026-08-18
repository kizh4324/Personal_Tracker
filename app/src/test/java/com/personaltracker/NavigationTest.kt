package com.personaltracker

import com.personaltracker.ui.navigation.PersonalTrackerDestination
import com.personaltracker.ui.navigation.icon
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests verifying 5-tab Navigation destination routes, accessibility descriptions, and tab order.
 */
class NavigationTest {

    @Test
    fun verifyBottomNavTabCountAndOrder() {
        val tabs = PersonalTrackerDestination.bottomNavTabs
        assertEquals("Bottom navigation must have exactly 5 tabs", 5, tabs.size)
        assertEquals("Tab 1 must be Home", PersonalTrackerDestination.HOME, tabs[0])
        assertEquals("Tab 2 must be Habits", PersonalTrackerDestination.HABITS, tabs[1])
        assertEquals("Tab 3 must be Study", PersonalTrackerDestination.STUDY, tabs[2])
        assertEquals("Tab 4 must be Companion", PersonalTrackerDestination.COMPANION, tabs[3])
        assertEquals("Tab 5 must be Settings", PersonalTrackerDestination.SETTINGS, tabs[4])
    }

    @Test
    fun verifyRouteResolution() {
        assertEquals(PersonalTrackerDestination.HOME, PersonalTrackerDestination.fromRoute("home"))
        assertEquals(PersonalTrackerDestination.HABITS, PersonalTrackerDestination.fromRoute("habits"))
        assertEquals(PersonalTrackerDestination.STUDY, PersonalTrackerDestination.fromRoute("study"))
        assertEquals(PersonalTrackerDestination.COMPANION, PersonalTrackerDestination.fromRoute("companion"))
        assertEquals(PersonalTrackerDestination.SETTINGS, PersonalTrackerDestination.fromRoute("settings"))
        
        // Null or unknown fallback to HOME
        assertEquals(PersonalTrackerDestination.HOME, PersonalTrackerDestination.fromRoute(null))
        assertEquals(PersonalTrackerDestination.HOME, PersonalTrackerDestination.fromRoute("unknown_route"))
    }

    @Test
    fun verifyAccessibilityContentDescriptions() {
        PersonalTrackerDestination.entries.forEach { destination ->
            assertTrue(
                "Content description for ${destination.name} must not be blank for TalkBack",
                destination.contentDescription.isNotBlank()
            )
            assertTrue(
                "Label for ${destination.name} must not be blank",
                destination.label.isNotBlank()
            )
            assertNotNull(
                "Icon for ${destination.name} must be defined",
                destination.icon()
            )
        }
    }
}
