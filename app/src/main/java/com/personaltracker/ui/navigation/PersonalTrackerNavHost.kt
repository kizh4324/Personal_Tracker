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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.personaltracker.ui.screens.CompanionScreen
import com.personaltracker.ui.screens.HabitsScreen
import com.personaltracker.ui.screens.HomeScreen
import com.personaltracker.ui.screens.SettingsScreen
import com.personaltracker.ui.screens.StudyScreen
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
 * Root Composable scaffolding the 5-tab Bottom Navigation bar and Screen container.
 */
@Composable
fun PersonalTrackerApp(
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val currentDestination = PersonalTrackerDestination.fromRoute(currentRoute)

    val colors = PersonalTrackerTheme.colors

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = colors.surfaceBase,
        bottomBar = {
            PersonalTrackerBottomBar(
                currentDestination = currentDestination,
                onNavigateToDestination = { destination ->
                    navController.navigate(destination.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = PersonalTrackerDestination.HOME.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable(PersonalTrackerDestination.HOME.route) {
                HomeScreen()
            }
            composable(PersonalTrackerDestination.HABITS.route) {
                HabitsScreen()
            }
            composable(PersonalTrackerDestination.STUDY.route) {
                StudyScreen()
            }
            composable(PersonalTrackerDestination.COMPANION.route) {
                CompanionScreen()
            }
            composable(PersonalTrackerDestination.SETTINGS.route) {
                SettingsScreen()
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

    NavigationBar(
        modifier = modifier.defaultMinSize(minHeight = 48.dp),
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
                modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            )
        }
    }
}
