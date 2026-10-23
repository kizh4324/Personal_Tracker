package com.personaltracker.ui.navigation

import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.personaltracker.ui.screens.companion.CompanionRoute
import com.personaltracker.ui.screens.habits.HabitsRoute
import com.personaltracker.ui.screens.home.HomeRoute
import com.personaltracker.ui.screens.home.HomeUiIntent
import com.personaltracker.ui.screens.home.HomeViewModel
import com.personaltracker.ui.screens.quickcapture.QuickCaptureViewModel
import com.personaltracker.ui.screens.settings.SettingsRoute
import com.personaltracker.ui.screens.study.StudyRoute
import com.personaltracker.ui.theme.PersonalTrackerTheme

/**
 * Maps Navigation destination enum to default Material Icons.
 */
fun PersonalTrackerDestination.icon(): ImageVector {
    return when (this) {
        PersonalTrackerDestination.HOME -> Icons.Default.Home
        PersonalTrackerDestination.HABITS -> Icons.Default.DateRange
        PersonalTrackerDestination.STUDY -> Icons.Default.PlayArrow
        PersonalTrackerDestination.COMPANION -> Icons.Default.Face
        PersonalTrackerDestination.SETTINGS -> Icons.Default.Settings
    }
}

/**
 * Root Composable scaffolding the AppShellScaffold container and 5-tab Screen navigation graph.
 * Strictly adheres to AD-7 with zero hardcoded literals and viewport-hoisted animations.
 */
@Composable
fun PersonalTrackerApp(
    navController: NavHostController = rememberNavController(),
    appShellViewModel: AppShellViewModel = hiltViewModel(),
    quickCaptureViewModel: QuickCaptureViewModel = hiltViewModel(),
    homeViewModel: HomeViewModel = hiltViewModel()
) {
    val shellState by appShellViewModel.uiState.collectAsStateWithLifecycle()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val currentDestination = PersonalTrackerDestination.fromRoute(currentRoute)

    val navigateTo: (PersonalTrackerDestination) -> Unit = { destination ->
        navController.navigate(destination.route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    AppShellScaffold(
        currentDestination = currentDestination,
        onNavigateToDestination = navigateTo,
        onDayTypeClick = {
            if (currentDestination == PersonalTrackerDestination.HOME) {
                homeViewModel.onIntent(HomeUiIntent.OpenDayTypeSelector)
            } else {
                navigateTo(PersonalTrackerDestination.SETTINGS)
            }
        },
        activeDayTypeName = shellState.activeDayTypeName,
        streakCount = shellState.streakCount,
        coinBalance = shellState.coinBalance,
        quickCaptureViewModel = quickCaptureViewModel
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = PersonalTrackerDestination.HOME.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable(PersonalTrackerDestination.HOME.route) {
                HomeRoute(
                    viewModel = homeViewModel,
                    onNavigate = navigateTo
                )
            }
            composable(PersonalTrackerDestination.HABITS.route) {
                HabitsRoute(onNavigate = navigateTo)
            }
            composable(PersonalTrackerDestination.STUDY.route) {
                StudyRoute(onNavigate = navigateTo)
            }
            composable(PersonalTrackerDestination.COMPANION.route) {
                CompanionRoute(onNavigate = navigateTo)
            }
            composable(PersonalTrackerDestination.SETTINGS.route) {
                SettingsRoute(onNavigate = navigateTo)
            }
        }
    }
}

/**
 * 5-tab Bottom Navigation Bar adhering to 48dp minimum touch targets and design tokens.
 */
@Composable
fun PersonalTrackerBottomBar(
    currentDestination: PersonalTrackerDestination,
    onNavigateToDestination: (PersonalTrackerDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = PersonalTrackerTheme.colors
    val typography = PersonalTrackerTheme.typography
    val spacing = PersonalTrackerTheme.spacing

    NavigationBar(
        modifier = modifier.defaultMinSize(minHeight = spacing.minTouchTarget),
        containerColor = colors.surfaceRaised,
        contentColor = colors.inkPrimary
    ) {
        PersonalTrackerDestination.bottomNavTabs.forEach { destination ->
            val selected = destination == currentDestination
            NavigationBarItem(
                selected = selected,
                onClick = { onNavigateToDestination(destination) },
                icon = {
                    Icon(
                        imageVector = destination.icon(),
                        contentDescription = destination.contentDescription
                    )
                },
                label = {
                    Text(
                        text = destination.label,
                        style = typography.caption
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = colors.primary,
                    selectedTextColor = colors.primary,
                    unselectedIconColor = colors.inkSecondary,
                    unselectedTextColor = colors.inkSecondary,
                    indicatorColor = colors.surfaceOverlay
                ),
                modifier = Modifier.defaultMinSize(
                    minWidth = spacing.minTouchTarget,
                    minHeight = spacing.minTouchTarget
                )
            )
        }
    }
}
