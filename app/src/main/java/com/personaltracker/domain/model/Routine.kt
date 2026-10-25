package com.personaltracker.domain.model

/**
 * Pure Kotlin domain model representing a routine template (FR-2.3, AD-1).
 */
data class Routine(
    val id: String,
    val dayTypeId: String,
    val title: String,
    val targetStartTime: String, // "HH:mm"
    val totalEstimatedMinutes: Int,
    val steps: List<RoutineStep> = emptyList()
) {
    companion object {
        val ROUTINE_STANDARD_MORNING = Routine(
            id = "routine_standard_morning",
            dayTypeId = "dt_standard",
            title = "Morning Kickoff Routine",
            targetStartTime = "08:00",
            totalEstimatedMinutes = 25,
            steps = listOf(
                RoutineStep(
                    id = "step_std_1",
                    routineId = "routine_standard_morning",
                    orderIndex = 1,
                    title = "Hydrate & gentle stretch",
                    durationMinutes = 5
                ),
                RoutineStep(
                    id = "step_std_2",
                    routineId = "routine_standard_morning",
                    orderIndex = 2,
                    title = "Review daily focus plan",
                    durationMinutes = 10
                ),
                RoutineStep(
                    id = "step_std_3",
                    routineId = "routine_standard_morning",
                    orderIndex = 3,
                    title = "Quick environment reset",
                    durationMinutes = 10
                )
            )
        )

        val ROUTINE_CRUNCH_FOCUS = Routine(
            id = "routine_crunch_focus",
            dayTypeId = "dt_crunch",
            title = "Crunch Focus Launch",
            targetStartTime = "08:00",
            totalEstimatedMinutes = 60,
            steps = listOf(
                RoutineStep(
                    id = "step_crunch_1",
                    routineId = "routine_crunch_focus",
                    orderIndex = 1,
                    title = "Eliminate distractions",
                    durationMinutes = 5
                ),
                RoutineStep(
                    id = "step_crunch_2",
                    routineId = "routine_crunch_focus",
                    orderIndex = 2,
                    title = "Prepare workspace & notes",
                    durationMinutes = 10
                ),
                RoutineStep(
                    id = "step_crunch_3",
                    routineId = "routine_crunch_focus",
                    orderIndex = 3,
                    title = "Priority focus block launch",
                    durationMinutes = 45
                )
            )
        )

        val ROUTINE_REST_RECOVERY = Routine(
            id = "routine_rest_recovery",
            dayTypeId = "dt_rest",
            title = "Rest & Recovery Routine",
            targetStartTime = "09:00",
            totalEstimatedMinutes = 30,
            steps = listOf(
                RoutineStep(
                    id = "step_rest_1",
                    routineId = "routine_rest_recovery",
                    orderIndex = 1,
                    title = "Morning walk or breathing",
                    durationMinutes = 15
                ),
                RoutineStep(
                    id = "step_rest_2",
                    routineId = "routine_rest_recovery",
                    orderIndex = 2,
                    title = "Reflective journaling",
                    durationMinutes = 15
                )
            )
        )

        val ROUTINE_LOW_CAPACITY = Routine(
            id = "routine_low_capacity",
            dayTypeId = "dt_low_capacity",
            title = "Gentle Pace Activation",
            targetStartTime = "09:30",
            totalEstimatedMinutes = 10,
            steps = listOf(
                RoutineStep(
                    id = "step_low_1",
                    routineId = "routine_low_capacity",
                    orderIndex = 1,
                    title = "Sip warm water & rest eyes",
                    durationMinutes = 5
                ),
                RoutineStep(
                    id = "step_low_2",
                    routineId = "routine_low_capacity",
                    orderIndex = 2,
                    title = "Identify single micro-goal",
                    durationMinutes = 5
                )
            )
        )

        val BUILT_IN_ROUTINES: List<Routine> = listOf(
            ROUTINE_STANDARD_MORNING,
            ROUTINE_CRUNCH_FOCUS,
            ROUTINE_REST_RECOVERY,
            ROUTINE_LOW_CAPACITY
        )
    }
}
