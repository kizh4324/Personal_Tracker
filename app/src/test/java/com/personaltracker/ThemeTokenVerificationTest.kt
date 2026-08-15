package com.personaltracker

import com.personaltracker.ui.theme.PTColors
import com.personaltracker.ui.theme.PTSpacing
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * Unit verification tests for Personal-Tracker design tokens.
 * Sourced from `docs/design/design-system.md` and verified against `Color.kt` / `Spacing.kt`.
 */
class ThemeTokenVerificationTest {

    @Test
    fun verifyLightModePrimaryColors() {
        assertNotNull("Light colors instance should not be null", PTColors.Light)
        assertEquals(0xFF2563EB, PTColors.Light.primary.value.toLong() shr 32 or (PTColors.Light.primary.value.toLong() and 0xFFFFFFFFL))
    }

    @Test
    fun verifyDarkModePrimaryColors() {
        assertNotNull("Dark colors instance should not be null", PTColors.Dark)
        assertEquals(0xFF60A5FA, PTColors.Dark.primary.value.toLong() shr 32 or (PTColors.Dark.primary.value.toLong() and 0xFFFFFFFFL))
    }

    @Test
    fun verifySpacingGridInvariants() {
        // Base 4dp grid verification
        assertEquals(4, PTSpacing.xs.value.toInt())
        assertEquals(8, PTSpacing.sm.value.toInt())
        assertEquals(12, PTSpacing.md.value.toInt())
        assertEquals(16, PTSpacing.lg.value.toInt())
        assertEquals(24, PTSpacing.xl.value.toInt())
        assertEquals(32, PTSpacing.xxl.value.toInt())
    }
}
