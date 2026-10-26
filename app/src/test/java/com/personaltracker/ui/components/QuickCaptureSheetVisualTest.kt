package com.personaltracker.ui.components

import androidx.compose.animation.core.SpringSpec
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.personaltracker.domain.parser.CommandIntent
import com.personaltracker.domain.parser.ConfidenceTier
import com.personaltracker.domain.parser.ParsedCommand
import com.personaltracker.ui.components.atoms.clampAmplitude
import com.personaltracker.ui.components.overlays.ProposalChipData
import com.personaltracker.ui.components.overlays.resolveProposalChips
import com.personaltracker.ui.theme.AnimationTokens
import com.personaltracker.ui.theme.DarkPersonalTrackerColors
import com.personaltracker.ui.theme.LightPersonalTrackerColors
import com.personaltracker.ui.theme.PersonalTrackerSpacing
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * JVM unit test suite verifying Warm Organic Tactile visual tokens, touch target floors,
 * Framer spring physics invariants, and 3-tier confidence routing UI schemas for Story 2.1.
 * Conforms to AD-7 and AD-8 without emulator overhead.
 */
class QuickCaptureSheetVisualTest {

    private val lightColors = LightPersonalTrackerColors
    private val darkColors = DarkPersonalTrackerColors
    private val spacing = PersonalTrackerSpacing()

    @Test
    fun `fabToModalSpringSpec matches Framer physics exactly`() {
        val springSpec = AnimationTokens.fabToModalSpringSpec<Float>()
        assertEquals(0.75f, springSpec.dampingRatio, 0.001f)
        assertEquals(320f, springSpec.stiffness, 0.001f)
    }

    @Test
    fun `fab colors conform to Warm Organic Tactile design tokens in light and dark modes`() {
        // FAB container rests in Deep Espresso (#59280E) with surfaceRaised (#FFFFFF) content
        assertEquals(Color(0xFF59280E), lightColors.primaryDark)
        assertEquals(Color(0xFFFFFFFF), lightColors.surfaceRaised)

        // Dark mode keeps semantic tokens intact
        assertNotNull(darkColors.primaryDark)
        assertNotNull(darkColors.surfaceRaised)
    }

    @Test
    fun `waveform visualizer active color uses Vibrant Orange primary`() {
        // Active audio RMS waveform bars render in colors.primary (#F26B1D)
        assertEquals(Color(0xFFF26B1D), lightColors.primary)
    }

    @Test
    fun `interactive elements enforce 48dp minimum touch target floor`() {
        // AD-7: 48dp floor for all clickable elements
        assertEquals(48.dp, spacing.minTouchTarget)
        assertTrue(spacing.fabSize >= spacing.minTouchTarget)
    }

    @Test
    fun `resolveProposalChips creates exactly 5 editable chips for medium confidence`() {
        val proposal = ParsedCommand(
            rawText = "Study algorithms for 45 minutes tomorrow",
            intent = CommandIntent.CREATE_TASK,
            title = "Study algorithms",
            date = "Tomorrow",
            time = "14:00",
            durationMinutes = 45,
            deliveryIntensity = "HIGH",
            confidence = 0.72f,
            tier = ConfidenceTier.MEDIUM
        )

        val chips = resolveProposalChips(proposal)
        assertEquals(5, chips.size)

        assertEquals("Title", chips[0].chipType)
        assertEquals("Study algorithms", chips[0].value)

        assertEquals("Date", chips[1].chipType)
        assertEquals("Tomorrow", chips[1].value)

        assertEquals("Time", chips[2].chipType)
        assertEquals("14:00", chips[2].value)

        assertEquals("Type", chips[3].chipType)
        assertEquals("CREATE_TASK", chips[3].value)

        assertEquals("Urgency", chips[4].chipType)
        assertEquals("HIGH", chips[4].value)
    }

    @Test
    fun `resolveProposalChips falls back gracefully on null fields`() {
        val proposal = ParsedCommand(
            rawText = "Quick note",
            intent = CommandIntent.UNKNOWN,
            title = "Quick note",
            date = null,
            time = null,
            durationMinutes = null,
            deliveryIntensity = "NORMAL",
            confidence = 0.60f,
            tier = ConfidenceTier.MEDIUM
        )

        val chips = resolveProposalChips(proposal)
        assertEquals(5, chips.size)
        assertEquals("Today", chips[1].value)
        assertEquals("Flexible", chips[2].value)
    }

    @Test
    fun `clampAmplitude handles edge cases and protects from NaN`() {
        assertEquals(0f, clampAmplitude(Float.NaN), 0.001f)
        assertEquals(0f, clampAmplitude(-0.5f), 0.001f)
        assertEquals(1f, clampAmplitude(1.5f), 0.001f)
        assertEquals(0.65f, clampAmplitude(0.65f), 0.001f)
    }

    @Test
    fun `low confidence notice uses semantic info tokens`() {
        assertNotNull(lightColors.infoSubtle)
        assertNotNull(lightColors.info)
    }

    @Test
    fun `high confidence undo toast uses bannerPeach and inkPrimary tokens`() {
        assertEquals(Color(0xFFFCD9B8), lightColors.bannerPeach)
        assertEquals(Color(0xFF1E1E1E), lightColors.inkPrimary)
    }
}
