package com.personaltracker.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personaltracker.domain.repository.DayTypeRepository
import com.personaltracker.domain.repository.EconomyRepository
import com.personaltracker.domain.repository.HabitRepository
import com.personaltracker.domain.usecase.ResolveDayTypeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.time.LocalDate
import javax.inject.Inject

/**
 * Top Context Bar persistent state model.
 */
data class AppShellUiState(
    val activeDayTypeName: String = "Weekday Routine",
    val streakCount: Int = 0,
    val coinBalance: Int = 0
)

/**
 * ViewModel driving persistent Top Context Bar (DayType, Streak, Coins) across all screens (AD-1, AD-2).
 */
@HiltViewModel
class AppShellViewModel @Inject constructor(
    private val economyRepository: EconomyRepository,
    private val habitRepository: HabitRepository,
    private val dayTypeRepository: DayTypeRepository,
    private val resolveDayTypeUseCase: ResolveDayTypeUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppShellUiState())
    val uiState: StateFlow<AppShellUiState> = _uiState.asStateFlow()

    init {
        observeTopBarContext()
    }

    private fun observeTopBarContext() {
        val today = LocalDate.now().toString()
        val dayTypesFlow = dayTypeRepository.getAllDayTypes()
        val overrideFlow = dayTypeRepository.observeOverrideForDate(today)
        val balanceFlow = economyRepository.observeLatestBalance()
        val habitsFlow = habitRepository.getAllHabits()

        combine(dayTypesFlow, overrideFlow, balanceFlow, habitsFlow) { _, _, balance, habits ->
            val currentDate = LocalDate.now().toString()
            val dayType = runCatching { resolveDayTypeUseCase(currentDate) }.getOrNull()
            val dayTypeName = dayType?.name ?: "Weekday Routine"
            val maxStreak = habits.maxOfOrNull { it.currentStreak } ?: 0

            AppShellUiState(
                activeDayTypeName = dayTypeName,
                streakCount = maxStreak,
                coinBalance = balance
            )
        }.distinctUntilChanged()
        .onEach { state ->
            _uiState.value = state
        }.launchIn(viewModelScope)
    }
}
