package com.personaltracker.data.repository

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
import com.personaltracker.domain.model.CoinTransaction
import com.personaltracker.domain.model.CompanionProfile
import com.personaltracker.domain.model.DayOverride
import com.personaltracker.domain.model.DayType
import com.personaltracker.domain.model.Habit
import com.personaltracker.domain.model.HabitLog
import com.personaltracker.domain.model.InterventionRule
import com.personaltracker.domain.model.Routine
import com.personaltracker.domain.model.RoutineStep
import com.personaltracker.domain.model.ShopItem
import com.personaltracker.domain.model.StudySession
import com.personaltracker.domain.model.Task
import com.personaltracker.domain.model.UnfiledInboxItem

internal fun DayTypeEntity.toDomain(): DayType = DayType(
    id = id,
    name = name,
    colorHex = colorHex,
    targetStudyMinutes = targetStudyMinutes,
    isDefault = isDefault,
    defaultDaysOfWeek = defaultDaysOfWeek
)

internal fun DayType.toEntity(): DayTypeEntity = DayTypeEntity(
    id = id,
    name = name,
    colorHex = colorHex,
    targetStudyMinutes = targetStudyMinutes,
    isDefault = isDefault,
    defaultDaysOfWeek = defaultDaysOfWeek
)

internal fun DayOverrideEntity.toDomain(): DayOverride = DayOverride(
    date = date,
    dayTypeId = dayTypeId,
    isUserModified = isUserModified,
    createdTimestamp = createdTimestamp
)

internal fun DayOverride.toEntity(): DayOverrideEntity = DayOverrideEntity(
    date = date,
    dayTypeId = dayTypeId,
    isUserModified = isUserModified,
    createdTimestamp = createdTimestamp
)

internal fun TaskEntity.toDomain(): Task = Task(
    id = id,
    title = title,
    description = description,
    scheduledDate = scheduledDate,
    scheduledTime = scheduledTime,
    estimatedDurationMinutes = estimatedDurationMinutes,
    deliveryIntensity = deliveryIntensity,
    state = state,
    isCarryForward = isCarryForward,
    carryForwardCount = carryForwardCount,
    completionTimestamp = completionTimestamp,
    createdTimestamp = createdTimestamp
)

internal fun Task.toEntity(): TaskEntity = TaskEntity(
    id = id,
    title = title,
    description = description,
    scheduledDate = scheduledDate,
    scheduledTime = scheduledTime,
    estimatedDurationMinutes = estimatedDurationMinutes,
    deliveryIntensity = deliveryIntensity,
    state = state,
    isCarryForward = isCarryForward,
    carryForwardCount = carryForwardCount,
    completionTimestamp = completionTimestamp,
    createdTimestamp = createdTimestamp
)

internal fun RoutineEntity.toDomain(steps: List<RoutineStep> = emptyList()): Routine = Routine(
    id = id,
    dayTypeId = dayTypeId,
    title = title,
    targetStartTime = targetStartTime,
    totalEstimatedMinutes = totalEstimatedMinutes,
    steps = steps
)

internal fun Routine.toEntity(): RoutineEntity = RoutineEntity(
    id = id,
    dayTypeId = dayTypeId,
    title = title,
    targetStartTime = targetStartTime,
    totalEstimatedMinutes = totalEstimatedMinutes
)

internal fun RoutineStepEntity.toDomain(): RoutineStep = RoutineStep(
    id = id,
    routineId = routineId,
    orderIndex = orderIndex,
    title = title,
    durationMinutes = durationMinutes
)

internal fun RoutineStep.toEntity(): RoutineStepEntity = RoutineStepEntity(
    id = id,
    routineId = routineId,
    orderIndex = orderIndex,
    title = title,
    durationMinutes = durationMinutes
)

internal fun HabitEntity.toDomain(): Habit = Habit(
    id = id,
    name = name,
    description = description,
    targetDaysOfWeek = targetDaysOfWeek,
    currentStreak = currentStreak,
    bestStreak = bestStreak,
    slackBankDays = slackBankDays,
    isFreezeActive = isFreezeActive,
    createdTimestamp = createdTimestamp
)

internal fun Habit.toEntity(): HabitEntity = HabitEntity(
    id = id,
    name = name,
    description = description,
    targetDaysOfWeek = targetDaysOfWeek,
    currentStreak = currentStreak,
    bestStreak = bestStreak,
    slackBankDays = slackBankDays,
    isFreezeActive = isFreezeActive,
    createdTimestamp = createdTimestamp
)

internal fun HabitLogEntity.toDomain(): HabitLog = HabitLog(
    id = id,
    habitId = habitId,
    logDate = logDate,
    status = status,
    loggedTimestamp = loggedTimestamp
)

internal fun HabitLog.toEntity(): HabitLogEntity = HabitLogEntity(
    id = id,
    habitId = habitId,
    logDate = logDate,
    status = status,
    loggedTimestamp = loggedTimestamp
)

internal fun StudySessionEntity.toDomain(): StudySession = StudySession(
    id = id,
    subjectTag = subjectTag,
    sessionDate = sessionDate,
    startTime = startTime,
    durationMinutes = durationMinutes,
    coinsEarned = coinsEarned,
    linkedTaskId = linkedTaskId,
    isInterrupted = isInterrupted,
    createdTimestamp = createdTimestamp
)

internal fun StudySession.toEntity(): StudySessionEntity = StudySessionEntity(
    id = id,
    subjectTag = subjectTag,
    sessionDate = sessionDate,
    startTime = startTime,
    durationMinutes = durationMinutes,
    coinsEarned = coinsEarned,
    linkedTaskId = linkedTaskId,
    isInterrupted = isInterrupted,
    createdTimestamp = createdTimestamp
)

internal fun CoinLedgerEntity.toDomain(): CoinTransaction = CoinTransaction(
    id = id,
    idempotencyKey = idempotencyKey,
    deltaAmount = deltaAmount,
    eventType = eventType,
    sourceEntityId = sourceEntityId,
    balanceAfter = balanceAfter,
    timestamp = timestamp
)

internal fun CoinTransaction.toEntity(): CoinLedgerEntity = CoinLedgerEntity(
    id = id,
    idempotencyKey = idempotencyKey,
    deltaAmount = deltaAmount,
    eventType = eventType,
    sourceEntityId = sourceEntityId,
    balanceAfter = balanceAfter,
    timestamp = timestamp
)

internal fun CompanionProfileEntity.toDomain(): CompanionProfile = CompanionProfile(
    id = id,
    speciesName = speciesName,
    level = level,
    currentEnergyUnits = currentEnergyUnits,
    activeAccessoryId = activeAccessoryId,
    activeBackgroundId = activeBackgroundId,
    currentExpressionOrdinal = currentExpressionOrdinal
)

internal fun CompanionProfile.toEntity(): CompanionProfileEntity = CompanionProfileEntity(
    id = id,
    speciesName = speciesName,
    level = level,
    currentEnergyUnits = currentEnergyUnits,
    activeAccessoryId = activeAccessoryId,
    activeBackgroundId = activeBackgroundId,
    currentExpressionOrdinal = currentExpressionOrdinal
)

internal fun ShopItemEntity.toDomain(): ShopItem = ShopItem(
    id = id,
    category = category,
    name = name,
    priceCoins = priceCoins,
    isUnlocked = isUnlocked,
    unlockedTimestamp = unlockedTimestamp
)

internal fun ShopItem.toEntity(): ShopItemEntity = ShopItemEntity(
    id = id,
    category = category,
    name = name,
    priceCoins = priceCoins,
    isUnlocked = isUnlocked,
    unlockedTimestamp = unlockedTimestamp
)

internal fun UnfiledCaptureInboxEntity.toDomain(): UnfiledInboxItem = UnfiledInboxItem(
    id = id,
    rawTranscriptText = rawTranscriptText,
    draftAttributesJson = draftAttributesJson,
    parserConfidence = parserConfidence,
    captureSource = captureSource,
    createdTimestamp = createdTimestamp
)

internal fun UnfiledInboxItem.toEntity(): UnfiledCaptureInboxEntity = UnfiledCaptureInboxEntity(
    id = id,
    rawTranscriptText = rawTranscriptText,
    draftAttributesJson = draftAttributesJson,
    parserConfidence = parserConfidence,
    captureSource = captureSource,
    createdTimestamp = createdTimestamp
)

internal fun InterventionRuleEntity.toDomain(): InterventionRule = InterventionRule(
    packageName = packageName,
    appDisplayName = appDisplayName,
    isStrictBlocked = isStrictBlocked,
    dailyLimitMinutes = dailyLimitMinutes
)

internal fun InterventionRule.toEntity(): InterventionRuleEntity = InterventionRuleEntity(
    packageName = packageName,
    appDisplayName = appDisplayName,
    isStrictBlocked = isStrictBlocked,
    dailyLimitMinutes = dailyLimitMinutes
)
