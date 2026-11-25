package com.personaltracker.domain

import com.personaltracker.domain.engine.BoundedEscalationEngine
import com.personaltracker.domain.model.EscalationPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class BoundedEscalationEngineTest {

    private lateinit var engine: BoundedEscalationEngine

    @Before
    fun setUp() {
        engine = BoundedEscalationEngine()
    }

    @Test
    fun `EscalationPolicy defaults to 5 minute interval and 3 max re-fires`() {
        val policy = EscalationPolicy()
        assertEquals(5, policy.retryIntervalMinutes)
        assertEquals(3, policy.maxReFires)
    }

    @Test
    fun `EscalationPolicy rejects out of bounds interval or maxReFires via require`() {
        val invalidConfigs = listOf(
            0 to 3,
            61 to 3,
            5 to -1,
            5 to 6
        )
        invalidConfigs.forEach { (interval, maxReFires) ->
            try {
                EscalationPolicy(retryIntervalMinutes = interval, maxReFires = maxReFires)
                fail("Expected IllegalArgumentException for interval=$interval, maxReFires=$maxReFires")
            } catch (_: IllegalArgumentException) {
                // Expected
            }
        }
    }

    @Test
    fun `URGENT pending unacknowledged task schedules attempt 1 through maxReFires`() {
        val now = 1_700_000_000_000L
        val policy = EscalationPolicy(retryIntervalMinutes = 5, maxReFires = 3)

        for (attempt in 0..2) {
            val decision = engine.evaluateNextEscalation(
                taskId = "task-1",
                deliveryIntensity = "URGENT",
                taskState = "PENDING",
                currentAttemptNumber = attempt,
                isAcknowledged = false,
                currentEpochMillis = now,
                policy = policy
            )

            assertTrue("Attempt $attempt should schedule follow-up", decision.shouldScheduleNext)
            assertEquals(attempt + 1, decision.nextAttemptNumber)
            assertEquals(now + 5 * 60_000L, decision.nextTriggerEpochMillis)
            assertNull(decision.haltReason)
        }
    }

    @Test
    fun `URGENT task halts cleanly once maxReFires cap is reached`() {
        val decision = engine.evaluateNextEscalation(
            taskId = "task-1",
            deliveryIntensity = "URGENT",
            taskState = "PENDING",
            currentAttemptNumber = 3,
            isAcknowledged = false,
            currentEpochMillis = 1_700_000_000_000L,
            policy = EscalationPolicy(retryIntervalMinutes = 5, maxReFires = 3)
        )

        assertFalse(decision.shouldScheduleNext)
        assertEquals(0, decision.nextAttemptNumber)
        assertEquals("MAX_RE_FIRES_REACHED", decision.haltReason)
    }

    @Test
    fun `maxReFires zero disables follow-up scheduling on attempt zero`() {
        val decision = engine.evaluateNextEscalation(
            taskId = "task-1",
            deliveryIntensity = "URGENT",
            taskState = "PENDING",
            currentAttemptNumber = 0,
            isAcknowledged = false,
            currentEpochMillis = 1_700_000_000_000L,
            policy = EscalationPolicy(retryIntervalMinutes = 5, maxReFires = 0)
        )

        assertFalse(decision.shouldScheduleNext)
        assertEquals("MAX_RE_FIRES_REACHED", decision.haltReason)
    }

    @Test
    fun `ROUTINE and IMPORTANT tasks never schedule follow-ups`() {
        listOf("ROUTINE", "IMPORTANT", "routine", "important").forEach { intensity ->
            val decision = engine.evaluateNextEscalation(
                taskId = "task-1",
                deliveryIntensity = intensity,
                taskState = "PENDING",
                currentAttemptNumber = 0,
                isAcknowledged = false,
                currentEpochMillis = 1_700_000_000_000L
            )

            assertFalse("$intensity should never escalate", decision.shouldScheduleNext)
            assertEquals("NON_URGENT_INTENSITY", decision.haltReason)
        }
    }

    @Test
    fun `completed, cancelled, and non-pending tasks halt escalation`() {
        listOf("COMPLETED", "CANCELLED", "IN_PROGRESS", "INTERRUPTED").forEach { state ->
            val decision = engine.evaluateNextEscalation(
                taskId = "task-1",
                deliveryIntensity = "URGENT",
                taskState = state,
                currentAttemptNumber = 0,
                isAcknowledged = false,
                currentEpochMillis = 1_700_000_000_000L
            )

            assertFalse(decision.shouldScheduleNext)
            assertEquals("TASK_NOT_PENDING", decision.haltReason)
        }
    }

    @Test
    fun `acknowledged task halts escalation immediately`() {
        val decision = engine.evaluateNextEscalation(
            taskId = "task-1",
            deliveryIntensity = "URGENT",
            taskState = "PENDING",
            currentAttemptNumber = 0,
            isAcknowledged = true,
            currentEpochMillis = 1_700_000_000_000L
        )

        assertFalse(decision.shouldScheduleNext)
        assertEquals("ACKNOWLEDGED", decision.haltReason)
    }

    @Test
    fun `blank taskId or negative attempt number halts safely`() {
        val blankDecision = engine.evaluateNextEscalation(
            taskId = " ",
            deliveryIntensity = "URGENT",
            taskState = "PENDING",
            currentAttemptNumber = 0,
            isAcknowledged = false,
            currentEpochMillis = 1_700_000_000_000L
        )
        assertFalse(blankDecision.shouldScheduleNext)
        assertEquals("BLANK_TASK_ID", blankDecision.haltReason)

        val negativeDecision = engine.evaluateNextEscalation(
            taskId = "task-1",
            deliveryIntensity = "URGENT",
            taskState = "PENDING",
            currentAttemptNumber = -1,
            isAcknowledged = false,
            currentEpochMillis = 1_700_000_000_000L
        )
        assertFalse(negativeDecision.shouldScheduleNext)
        assertEquals("INVALID_ATTEMPT", negativeDecision.haltReason)
    }
}
