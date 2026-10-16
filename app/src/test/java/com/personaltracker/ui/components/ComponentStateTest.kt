package com.personaltracker.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import com.personaltracker.R
import com.personaltracker.domain.engine.FlameState
import com.personaltracker.domain.parser.CommandIntent
import com.personaltracker.domain.parser.ConfidenceTier
import com.personaltracker.domain.parser.ParsedCommand
import com.personaltracker.ui.components.animation.CoinRewardEvent
import com.personaltracker.ui.components.animation.RewardAnimationType
import com.personaltracker.ui.components.animation.RewardMotionStageState
import com.personaltracker.ui.components.atoms.clampAmplitude
import com.personaltracker.ui.components.atoms.getFlameColor
import com.personaltracker.ui.components.atoms.getFlameDrawableRes
import com.personaltracker.ui.components.atoms.resolveDayTypeColor
import com.personaltracker.ui.components.atoms.resolveFlameState
import com.personaltracker.ui.components.cards.HeroCardState
import com.personaltracker.ui.components.cards.formatCadence
import com.personaltracker.ui.components.cards.formatCarryForwardText
import com.personaltracker.ui.components.cards.resolveHeroPriority
import com.personaltracker.ui.components.cards.resolveIntensityStripeSpec
import com.personaltracker.ui.components.overlays.ProposalChipData
import com.personaltracker.ui.components.overlays.resolveProposalChips
import com.personaltracker.ui.theme.LightPersonalTrackerColors
import com.personaltracker.ui.theme.PersonalTrackerSpacing
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit test suite verifying UI shell component state models, flame transitions,
 * confidence tier routing, delivery intensity stripes, and hero card priority evaluations.
 * Executes on JVM without emulator overhead (AD-3, AD-7).
 */
class ComponentStateTest {

    private val colors = LightPersonalTrackerColors
    private val spacing = PersonalTrackerSpacing()

    // ─── 1. Streak Flame Badge 4-State Progression & Clamping ───

    @Test
    fun `resolveFlameState maps streak counts accurately across all 4 tiers`() {
        assertEquals(FlameState.COLD, resolveFlameState(0))
        assertEquals(FlameState.WARMING, resolveFlameState(1))
        assertEquals(FlameState.WARMING, resolveFlameState(2))
        assertEquals(FlameState.BURNING, resolveFlameState(3))
        assertEquals(FlameState.BURNING, resolveFlameState(5))
        assertEquals(FlameState.BURNING, resolveFlameState(6))
        assertEquals(FlameState.BLAZING, resolveFlameState(7))
        assertEquals(FlameState.BLAZING, resolveFlameState(14))
        assertEquals(FlameState.BLAZING, resolveFlameState(100))
    }

    @Test
    fun `resolveFlameState clamps negative streak counts to 0 and COLD`() {
        assertEquals(FlameState.COLD, resolveFlameState(-1))
        assertEquals(FlameState.COLD, resolveFlameState(-42))
    }

    @Test
    fun `flame drawables bind accurately to each FlameState`() {
        assertEquals(R.drawable.ic_flame_cold, getFlameDrawableRes(FlameState.COLD))
        assertEquals(R.drawable.ic_flame_warming, getFlameDrawableRes(FlameState.WARMING))
        assertEquals(R.drawable.ic_flame_burning, getFlameDrawableRes(FlameState.BURNING))
        assertEquals(R.drawable.ic_flame_blazing, getFlameDrawableRes(FlameState.BLAZING))
    }

    @Test
    fun `flame colors resolve from theme tokens without hardcoded literals`() {
        assertEquals(colors.streakCool, getFlameColor(FlameState.COLD, colors))
        assertEquals(colors.streakFlameTip, getFlameColor(FlameState.WARMING, colors))
        assertEquals(colors.streakFlameCore, getFlameColor(FlameState.BURNING, colors))
        assertEquals(colors.streakFlameCore, getFlameColor(FlameState.BLAZING, colors))
    }

    // ─── 2. DayType Chip Semantic Color Mapping ───

    @Test
    fun `resolveDayTypeColor resolves template colors accurately`() {
        assertEquals(colors.daytypeWeekday, resolveDayTypeColor("Weekday Routine", colors))
        assertEquals(colors.daytypeWeekend, resolveDayTypeColor("Weekend Chill", colors))
        assertEquals(colors.daytypeCollege, resolveDayTypeColor("College Lecture Day", colors))
        assertEquals(colors.daytypeSpecial, resolveDayTypeColor("Special Exam Day", colors))
    }

    @Test
    fun `resolveDayTypeColor falls back to weekday for unknown template names`() {
        assertEquals(colors.daytypeWeekday, resolveDayTypeColor("Custom Schedule", colors))
        assertEquals(colors.daytypeWeekday, resolveDayTypeColor("", colors))
    }

    // ─── 3. Hero Action Card 6-Priority Hierarchy & Conflict Resolution ───

    @Test
    fun `hero card states have correct descending priorities from 1 to 6`() {
        val activeFocus = HeroCardState.ActiveFocus("Study Math", "25:00")
        val interrupted = HeroCardState.InterruptedResumable("Read Chapter", "12m ago")
        val morning = HeroCardState.MorningBanner("Weekday Routine")
        val upcoming = HeroCardState.UpcomingItem("Workout", 15)
        val carryForward = HeroCardState.CarryForwardReview(3)
        val idle = HeroCardState.IntentionalIdle()

        assertEquals(1, activeFocus.priority)
        assertEquals(2, interrupted.priority)
        assertEquals(3, morning.priority)
        assertEquals(4, upcoming.priority)
        assertEquals(5, carryForward.priority)
        assertEquals(6, idle.priority)
    }

    @Test
    fun `resolveHeroPriority resolves conflicts to the highest active priority state`() {
        val activeFocus = HeroCardState.ActiveFocus("Study Math", "25:00")
        val interrupted = HeroCardState.InterruptedResumable("Read Chapter", "12m ago")
        val morning = HeroCardState.MorningBanner("Weekday Routine")
        val upcoming = HeroCardState.UpcomingItem("Workout", 15)
        val carryForward = HeroCardState.CarryForwardReview(3)
        val idle = HeroCardState.IntentionalIdle()

        // ActiveFocus wins over everything
        val result1 = resolveHeroPriority(listOf(idle, carryForward, activeFocus, upcoming))
        assertEquals(activeFocus, result1)

        // InterruptedResumable wins over lower states
        val result2 = resolveHeroPriority(listOf(carryForward, morning, interrupted))
        assertEquals(interrupted, result2)

        // MorningBanner wins over upcoming and carryForward
        val result3 = resolveHeroPriority(listOf(carryForward, upcoming, morning))
        assertEquals(morning, result3)

        // Upcoming wins over carryForward and idle
        val result4 = resolveHeroPriority(listOf(idle, carryForward, upcoming))
        assertEquals(upcoming, result4)

        // CarryForward wins over idle
        val result5 = resolveHeroPriority(listOf(idle, carryForward))
        assertEquals(carryForward, result5)

        // Empty list defaults to IntentionalIdle
        val resultEmpty = resolveHeroPriority(emptyList())
        assertTrue(resultEmpty is HeroCardState.IntentionalIdle)
    }

    // ─── 4. Task Delivery Intensity Stripe Specifications ───

    @Test
    fun `resolveIntensityStripeSpec produces 3dp info stripe for ROUTINE`() {
        val spec = resolveIntensityStripeSpec("ROUTINE", colors, spacing)
        assertEquals(3.dp, spec.width)
        assertEquals(colors.info, spec.color)
        assertFalse(spec.isUrgent)
    }

    @Test
    fun `resolveIntensityStripeSpec produces 3dp warning stripe for IMPORTANT`() {
        val spec = resolveIntensityStripeSpec("IMPORTANT", colors, spacing)
        assertEquals(3.dp, spec.width)
        assertEquals(colors.warning, spec.color)
        assertFalse(spec.isUrgent)
    }

    @Test
    fun `resolveIntensityStripeSpec produces 6dp danger stripe with urgent flag for URGENT`() {
        val spec = resolveIntensityStripeSpec("URGENT", colors, spacing)
        assertEquals(6.dp, spec.width)
        assertEquals(colors.danger, spec.color)
        assertTrue(spec.isUrgent)
    }

    @Test
    fun `resolveIntensityStripeSpec gracefully falls back to ROUTINE for unknown or invalid strings`() {
        val specInvalid = resolveIntensityStripeSpec("UNKNOWN_LEVEL", colors, spacing)
        assertEquals(3.dp, specInvalid.width)
        assertEquals(colors.info, specInvalid.color)
        assertFalse(specInvalid.isUrgent)

        val specEmpty = resolveIntensityStripeSpec("", colors, spacing)
        assertEquals(3.dp, specEmpty.width)
        assertEquals(colors.info, specEmpty.color)
        assertFalse(specEmpty.isUrgent)
    }

    // ─── 5. Quick Capture Confidence Tier Routing & Chip Verification ───

    @Test
    fun `medium confidence command proposal contains all required chip attributes`() {
        val parsed = ParsedCommand(
            rawText = "Buy groceries tomorrow at 5pm urgent",
            intent = CommandIntent.CREATE_TASK,
            title = "Buy groceries",
            date = "2026-10-17",
            time = "17:00",
            durationMinutes = 30,
            deliveryIntensity = "URGENT",
            confidence = 0.72f,
            tier = ConfidenceTier.MEDIUM
        )

        assertEquals("Buy groceries", parsed.title)
        assertEquals("2026-10-17", parsed.date)
        assertEquals("17:00", parsed.time)
        assertEquals(CommandIntent.CREATE_TASK, parsed.intent)
        assertEquals("URGENT", parsed.deliveryIntensity)
        assertEquals(ConfidenceTier.MEDIUM, parsed.tier)
    }

    // ─── 6. Reward Motion Stage State Controller ───

    @Test
    fun `RewardMotionStageState manages coin event and target lifecycle accurately`() {
        val controller = RewardMotionStageState()
        val target = Offset(950f, 150f)
        val source = Offset(200f, 500f)

        // 1. Update target offset from CoinPill layout
        controller.updateDestinationTarget(target)
        assertEquals(target, controller.state.destinationTargetOffset)
        assertNull(controller.state.activeCoinEvent)

        // 2. Dispatch coin reward event
        var callbackInvoked = false
        controller.dispatchCoinReward(
            startOffset = source,
            coinCount = 5,
            onComplete = { callbackInvoked = true }
        )

        val activeEvent = controller.state.activeCoinEvent
        assertNotNull(activeEvent)
        assertEquals(source, activeEvent!!.startOffset)
        assertEquals(5, activeEvent.coinCount)
        assertFalse(callbackInvoked)

        // 3. Complete coin arc
        controller.onCoinArcCompleted()
        assertTrue(callbackInvoked)
        assertNull(controller.state.activeCoinEvent)
    }

    @Test
    fun `RewardMotionStageState manages lottie reward animation lifecycle`() {
        val controller = RewardMotionStageState()

        controller.dispatchLottieReward(RewardAnimationType.MilestoneFullReveal)
        assertEquals(RewardAnimationType.MilestoneFullReveal, controller.state.activeLottieAnimation)
        assertTrue(controller.state.isLottiePlaying)

        controller.onLottieCompleted()
        assertNull(controller.state.activeLottieAnimation)
        assertFalse(controller.state.isLottiePlaying)
    }

    @Test
    fun `clampAmplitude handles Float NaN and clamps values between 0f and 1f`() {
        assertEquals(0f, clampAmplitude(Float.NaN), 0.001f)
        assertEquals(0f, clampAmplitude(-0.5f), 0.001f)
        assertEquals(0.42f, clampAmplitude(0.42f), 0.001f)
        assertEquals(1.0f, clampAmplitude(1.5f), 0.001f)
        assertEquals(1.0f, clampAmplitude(1.0f), 0.001f)
        assertEquals(0.0f, clampAmplitude(0.0f), 0.001f)
    }

    @Test
    fun `formatCadence maps day strings and standard identifiers to expected labels`() {
        assertEquals("DAILY", formatCadence("1,2,3,4,5,6,7"))
        assertEquals("DAILY", formatCadence("1, 2, 3, 4, 5, 6, 7"))
        assertEquals("WEEKDAYS", formatCadence("1,2,3,4,5"))
        assertEquals("WEEKDAYS", formatCadence("1, 2, 3, 4, 5"))
        assertEquals("DAILY", formatCadence("DAILY"))
        assertEquals("DAILY", formatCadence("daily"))
        assertEquals("WEEKDAYS", formatCadence("WEEKDAYS"))
        assertEquals("CUSTOM", formatCadence("1,3,5"))
        assertEquals("CUSTOM", formatCadence(""))
    }

    @Test
    fun `formatCarryForwardText formats singular and plural items accurately without guilt`() {
        assertEquals("1 item still open. What would you like to do?", formatCarryForwardText(1))
        assertEquals("3 items still open. What would you like to do?", formatCarryForwardText(3))
        assertEquals("0 items still open. What would you like to do?", formatCarryForwardText(0))
    }

    @Test
    fun `resolveProposalChips creates 5 editable chips with proper defaults for null date and time`() {
        val fullProposal = ParsedCommand(
            rawText = "Study algorithms tomorrow at 3pm",
            intent = CommandIntent.CREATE_TASK,
            title = "Study algorithms",
            date = "2026-10-17",
            time = "15:00",
            durationMinutes = 45,
            deliveryIntensity = "IMPORTANT",
            confidence = 0.78f,
            tier = ConfidenceTier.MEDIUM
        )

        val fullChips = resolveProposalChips(fullProposal)
        assertEquals(5, fullChips.size)
        assertEquals(ProposalChipData("Title", "Title", "Study algorithms"), fullChips[0])
        assertEquals(ProposalChipData("Date", "Date", "2026-10-17"), fullChips[1])
        assertEquals(ProposalChipData("Time", "Time", "15:00"), fullChips[2])
        assertEquals(ProposalChipData("Type", "Type", "CREATE_TASK"), fullChips[3])
        assertEquals(ProposalChipData("Urgency", "Urgency", "IMPORTANT"), fullChips[4])

        val minimalProposal = ParsedCommand(
            rawText = "Drink water",
            intent = CommandIntent.LOG_HABIT,
            title = "Drink water",
            date = null,
            time = null,
            durationMinutes = null,
            deliveryIntensity = "ROUTINE",
            confidence = 0.65f,
            tier = ConfidenceTier.MEDIUM
        )

        val fallbackChips = resolveProposalChips(minimalProposal)
        assertEquals(5, fallbackChips.size)
        assertEquals(ProposalChipData("Title", "Title", "Drink water"), fallbackChips[0])
        assertEquals(ProposalChipData("Date", "Date", "Today"), fallbackChips[1])
        assertEquals(ProposalChipData("Time", "Time", "Flexible"), fallbackChips[2])
        assertEquals(ProposalChipData("Type", "Type", "LOG_HABIT"), fallbackChips[3])
        assertEquals(ProposalChipData("Urgency", "Urgency", "ROUTINE"), fallbackChips[4])
    }

    @Test
    fun `RewardMotionStageState immediately completes on invalid coin reward parameters`() {
        val controller = RewardMotionStageState()
        var completedZeroCoins = false
        var completedZeroOffset = false

        controller.dispatchCoinReward(
            startOffset = Offset(100f, 100f),
            coinCount = 0,
            onComplete = { completedZeroCoins = true }
        )
        assertTrue(completedZeroCoins)
        assertNull(controller.state.activeCoinEvent)

        controller.dispatchCoinReward(
            startOffset = Offset.Zero,
            coinCount = 5,
            onComplete = { completedZeroOffset = true }
        )
        assertTrue(completedZeroOffset)
        assertNull(controller.state.activeCoinEvent)
    }

    @Test
    fun `RewardMotionStageState completes previous event when preempted by new coin dispatch`() {
        val controller = RewardMotionStageState()
        var firstEventCompleted = false
        var secondEventCompleted = false

        controller.dispatchCoinReward(
            startOffset = Offset(100f, 200f),
            coinCount = 3,
            onComplete = { firstEventCompleted = true }
        )
        assertFalse(firstEventCompleted)
        assertEquals(3, controller.state.activeCoinEvent?.coinCount)

        controller.dispatchCoinReward(
            startOffset = Offset(300f, 400f),
            coinCount = 10,
            onComplete = { secondEventCompleted = true }
        )
        // First event was completed on preemption
        assertTrue(firstEventCompleted)
        assertFalse(secondEventCompleted)
        assertEquals(10, controller.state.activeCoinEvent?.coinCount)

        controller.onCoinArcCompleted()
        assertTrue(secondEventCompleted)
        assertNull(controller.state.activeCoinEvent)
    }
}
