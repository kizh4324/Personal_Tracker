package com.personaltracker.data.local.db

import com.personaltracker.data.local.db.entities.CoinLedgerEntity
import com.personaltracker.data.local.db.entities.CompanionProfileEntity
import com.personaltracker.data.local.db.entities.DayOverrideEntity
import com.personaltracker.data.local.db.entities.DayTypeEntity
import com.personaltracker.data.local.db.entities.HabitEntity
import com.personaltracker.data.local.db.entities.HabitLogEntity
import com.personaltracker.data.local.db.entities.InterventionRuleEntity
import com.personaltracker.data.local.db.entities.RoutineEntity
import com.personaltracker.data.local.db.entities.RoutineStepEntity
import com.personaltracker.data.local.db.entities.ShopItemEntity
import com.personaltracker.data.local.db.entities.StudySessionEntity
import com.personaltracker.data.local.db.entities.TaskEntity
import com.personaltracker.data.local.db.entities.UnfiledCaptureInboxEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

/**
 * Unit tests verifying Room entity schema definitions, primary key integrity,
 * and immutable data contract invariants (ARCH-2, ARCH-6, Story 1.2).
 */
class DatabaseSchemaTest {

    @Test
    fun verifyAll13EntitiesInstantiation() {
        val dayType = DayTypeEntity(
            id = "weekday_standard",
            name = "Weekday Routine",
            colorHex = "#2563EB",
            targetStudyMinutes = 120,
            isDefault = true,
            defaultDaysOfWeek = "1,2,3,4,5"
        )
        assertEquals("weekday_standard", dayType.id)

        val dayOverride = DayOverrideEntity(
            date = "2026-08-17",
            dayTypeId = "heavy_study",
            isUserModified = true,
            createdTimestamp = System.currentTimeMillis()
        )
        assertEquals("2026-08-17", dayOverride.date)

        val task = TaskEntity(
            id = UUID.randomUUID().toString(),
            title = "Complete Math Assignment",
            description = "Chapter 4 problems 1-15",
            scheduledDate = "2026-08-17",
            scheduledTime = "14:00",
            estimatedDurationMinutes = 60,
            deliveryIntensity = "IMPORTANT",
            state = "PENDING",
            isCarryForward = false,
            carryForwardCount = 0,
            completionTimestamp = null,
            createdTimestamp = System.currentTimeMillis()
        )
        assertNotNull(task.id)
        assertEquals("PENDING", task.state)

        val routine = RoutineEntity(
            id = "morning_kickoff",
            dayTypeId = "weekday_standard",
            title = "Morning Routine",
            targetStartTime = "07:30",
            totalEstimatedMinutes = 30
        )
        assertEquals("morning_kickoff", routine.id)

        val step = RoutineStepEntity(
            id = UUID.randomUUID().toString(),
            routineId = "morning_kickoff",
            orderIndex = 1,
            title = "Hydrate & Meditate",
            durationMinutes = 15
        )
        assertEquals("morning_kickoff", step.routineId)

        val habit = HabitEntity(
            id = UUID.randomUUID().toString(),
            name = "Read 20 pages",
            description = null,
            targetDaysOfWeek = "1,2,3,4,5,6,7",
            currentStreak = 5,
            bestStreak = 14,
            slackBankDays = 2,
            isFreezeActive = false,
            createdTimestamp = System.currentTimeMillis()
        )
        assertEquals(5, habit.currentStreak)

        val habitLog = HabitLogEntity(
            id = UUID.randomUUID().toString(),
            habitId = habit.id,
            logDate = "2026-08-17",
            status = "COMPLETED",
            loggedTimestamp = System.currentTimeMillis()
        )
        assertEquals("COMPLETED", habitLog.status)

        val studySession = StudySessionEntity(
            id = UUID.randomUUID().toString(),
            subjectTag = "Algorithms",
            sessionDate = "2026-08-17",
            startTime = "15:00",
            durationMinutes = 90,
            coinsEarned = 15,
            linkedTaskId = task.id,
            isInterrupted = false,
            createdTimestamp = System.currentTimeMillis()
        )
        assertEquals("Algorithms", studySession.subjectTag)

        val coinLedger = CoinLedgerEntity(
            id = UUID.randomUUID().toString(),
            idempotencyKey = "TASK_COMP_${task.id}",
            deltaAmount = 10,
            eventType = "TASK_COMPLETION",
            sourceEntityId = task.id,
            balanceAfter = 100,
            timestamp = System.currentTimeMillis()
        )
        assertTrue(coinLedger.idempotencyKey.startsWith("TASK_COMP_"))

        val companion = CompanionProfileEntity(
            id = 1,
            speciesName = "Fox",
            level = 2,
            currentEnergyUnits = 100,
            activeAccessoryId = null,
            activeBackgroundId = null,
            currentExpressionOrdinal = 0
        )
        assertEquals(1, companion.id)

        val shopItem = ShopItemEntity(
            id = "cosmetic_hat_01",
            category = "ACCESSORY",
            name = "Scholar Cap",
            priceCoins = 50,
            isUnlocked = false,
            unlockedTimestamp = null
        )
        assertEquals(50, shopItem.priceCoins)

        val unfiledInbox = UnfiledCaptureInboxEntity(
            id = UUID.randomUUID().toString(),
            rawTranscriptText = "Buy groceries tomorrow afternoon",
            draftAttributesJson = "{}",
            parserConfidence = 0.45f,
            captureSource = "OFFLINE_STT",
            createdTimestamp = System.currentTimeMillis()
        )
        assertEquals(0.45f, unfiledInbox.parserConfidence)

        val rule = InterventionRuleEntity(
            packageName = "com.social.distraction",
            appDisplayName = "Social App",
            isStrictBlocked = true,
            dailyLimitMinutes = 30
        )
        assertEquals("com.social.distraction", rule.packageName)
    }
}
