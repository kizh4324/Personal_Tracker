package com.personaltracker.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Personal-Tracker Color Design Tokens.
 * Sourced 1:1 from `design-system.md` YAML frontmatter.
 */
@Immutable
data class PersonalTrackerColors(
    // Surfaces
    val surfaceBase: Color,
    val surfaceRaised: Color,
    val surfaceSunken: Color,
    val surfaceOverlay: Color,

    // Primary
    val primary: Color,
    val primaryLight: Color,
    val primarySubtle: Color,

    // Secondary
    val secondary: Color,
    val secondaryLight: Color,
    val secondarySubtle: Color,

    // Accent / Reward (Warm)
    val accentWarm: Color,
    val accentWarmLight: Color,
    val accentWarmSubtle: Color,

    // Companion / Growth
    val companion: Color,
    val companionLight: Color,
    val companionSubtle: Color,

    // Semantic: Success
    val success: Color,
    val successSubtle: Color,

    // Semantic: Warning
    val warning: Color,
    val warningSubtle: Color,

    // Semantic: Danger
    val danger: Color,
    val dangerSubtle: Color,

    // Semantic: Info
    val info: Color,
    val infoSubtle: Color,

    // Ink / Text
    val inkPrimary: Color,
    val inkSecondary: Color,
    val inkTertiary: Color,
    val inkDisabled: Color,

    // Borders
    val borderDefault: Color,
    val borderSubtle: Color,

    // DayType Color Coding
    val daytypeWeekday: Color,
    val daytypeWeekend: Color,
    val daytypeCollege: Color,
    val daytypeSpecial: Color,

    // Coin / Currency
    val coinGold: Color,
    val coinGlow: Color,

    // Streak / Fire
    val streakFlameCore: Color,
    val streakFlameTip: Color,
    val streakCool: Color,

    // Theme Mode Flag
    val isDark: Boolean
)

val LightPersonalTrackerColors = PersonalTrackerColors(
    surfaceBase = Color(0xFFF8F7F4),
    surfaceRaised = Color(0xFFFFFFFF),
    surfaceSunken = Color(0xFFF0EEEA),
    surfaceOverlay = Color(0x66000000), // rgba(0, 0, 0, 0.4)

    primary = Color(0xFF2563EB),
    primaryLight = Color(0xFF3B82F6),
    primarySubtle = Color(0xFFDBEAFE),

    secondary = Color(0xFF6366F1),
    secondaryLight = Color(0xFF818CF8),
    secondarySubtle = Color(0xFFE0E7FF),

    accentWarm = Color(0xFFD97706),
    accentWarmLight = Color(0xFFF59E0B),
    accentWarmSubtle = Color(0xFFFEF3C7),

    companion = Color(0xFF059669),
    companionLight = Color(0xFF10B981),
    companionSubtle = Color(0xFFD1FAE5),

    success = Color(0xFF16A34A),
    successSubtle = Color(0xFFDCFCE7),

    warning = Color(0xFFCA8A04),
    warningSubtle = Color(0xFFFEF9C3),

    danger = Color(0xFFDC2626),
    dangerSubtle = Color(0xFFFEE2E2),

    info = Color(0xFF0284C7),
    infoSubtle = Color(0xFFE0F2FE),

    inkPrimary = Color(0xFF18181B),
    inkSecondary = Color(0xFF52525B),
    inkTertiary = Color(0xFFA1A1AA),
    inkDisabled = Color(0xFFD4D4D8),

    borderDefault = Color(0xFFE4E4E7),
    borderSubtle = Color(0xFFF4F4F5),

    daytypeWeekday = Color(0xFF3B82F6),
    daytypeWeekend = Color(0xFF8B5CF6),
    daytypeCollege = Color(0xFF06B6D4),
    daytypeSpecial = Color(0xFFF59E0B),

    coinGold = Color(0xFFF59E0B),
    coinGlow = Color(0x33F59E0B), // rgba(245, 158, 11, 0.2)

    streakFlameCore = Color(0xFFF97316),
    streakFlameTip = Color(0xFFFDE047),
    streakCool = Color(0xFF94A3B8),

    isDark = false
)

val DarkPersonalTrackerColors = PersonalTrackerColors(
    surfaceBase = Color(0xFF121214),
    surfaceRaised = Color(0xFF1C1C1F),
    surfaceSunken = Color(0xFF0A0A0C),
    surfaceOverlay = Color(0x99000000), // rgba(0, 0, 0, 0.6)

    primary = Color(0xFF60A5FA),
    primaryLight = Color(0xFF3B82F6),
    primarySubtle = Color(0xFF1E3A5F),

    secondary = Color(0xFFA5B4FC),
    secondaryLight = Color(0xFF818CF8),
    secondarySubtle = Color(0xFF272566),

    accentWarm = Color(0xFFFBBF24),
    accentWarmLight = Color(0xFFF59E0B),
    accentWarmSubtle = Color(0xFF422006),

    companion = Color(0xFF34D399),
    companionLight = Color(0xFF10B981),
    companionSubtle = Color(0xFF064E3B),

    success = Color(0xFF4ADE80),
    successSubtle = Color(0xFF14532D),

    warning = Color(0xFFFACC15),
    warningSubtle = Color(0xFF422006),

    danger = Color(0xFFF87171),
    dangerSubtle = Color(0xFF450A0A),

    info = Color(0xFF38BDF8),
    infoSubtle = Color(0xFF0C4A6E),

    inkPrimary = Color(0xFFFAFAFA),
    inkSecondary = Color(0xFFA1A1AA),
    inkTertiary = Color(0xFF71717A),
    inkDisabled = Color(0xFF3F3F46),

    borderDefault = Color(0xFF27272A),
    borderSubtle = Color(0xFF1C1C1F),

    daytypeWeekday = Color(0xFF3B82F6),
    daytypeWeekend = Color(0xFF8B5CF6),
    daytypeCollege = Color(0xFF06B6D4),
    daytypeSpecial = Color(0xFFF59E0B),

    coinGold = Color(0xFFFBBF24),
    coinGlow = Color(0x33FBBF24), // rgba(251, 191, 36, 0.2)

    streakFlameCore = Color(0xFFF97316),
    streakFlameTip = Color(0xFFFDE047),
    streakCool = Color(0xFF94A3B8),

    isDark = true
)

val LocalPersonalTrackerColors = staticCompositionLocalOf { LightPersonalTrackerColors }
