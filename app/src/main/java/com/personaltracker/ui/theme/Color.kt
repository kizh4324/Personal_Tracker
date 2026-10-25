package com.personaltracker.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Personal-Tracker Color Design Tokens.
 * Synchronized with Figma reference frames (`s57DZjLfKqmqAJrdU8LwxB`) and `design-system.md`.
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
    val primaryDark: Color,
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

    // Figma Reference & Co-Design Tokens
    val bannerPeach: Color,
    val accentLime: Color,
    val barDeepCocoa: Color,
    val barWarmRust: Color,
    val barMatchaOlive: Color,
    val barSoftOrchid: Color,
    val iconSoftPeach: Color,
    val iconSoftMatcha: Color,
    val iconSoftLavender: Color,

    // Theme Mode Flag
    val isDark: Boolean
)

val LightPersonalTrackerColors = PersonalTrackerColors(
    surfaceBase = Color(0xFFF9F6F0), // Warm Cream Figma Canvas
    surfaceRaised = Color(0xFFFFFFFF),
    surfaceSunken = Color(0xFFEBE5DC), // Hatched Track background / sand
    surfaceOverlay = Color(0x66000000), // rgba(0, 0, 0, 0.4)

    primary = Color(0xFFF26B1D), // Vibrant Orange CTA
    primaryLight = Color(0xFFF98E4E),
    primaryDark = Color(0xFF59280E), // Deep Espresso FAB / Button
    primarySubtle = Color(0xFFFFE8D6),

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

    inkPrimary = Color(0xFF1E1E1E), // Charcoal Black
    inkSecondary = Color(0xFF7A7267), // Muted Olive / Gray Subtitle
    inkTertiary = Color(0xFFA1A1AA),
    inkDisabled = Color(0xFFD4D4D8),

    borderDefault = Color(0xFFE4E4E7),
    borderSubtle = Color(0xFFD9D2C9), // Hollow Stepper border

    daytypeWeekday = Color(0xFF3B82F6),
    daytypeWeekend = Color(0xFF8B5CF6),
    daytypeCollege = Color(0xFF06B6D4),
    daytypeSpecial = Color(0xFFF59E0B),

    coinGold = Color(0xFFF59E0B),
    coinGlow = Color(0x33F59E0B), // rgba(245, 158, 11, 0.2)

    streakFlameCore = Color(0xFFF26B1D),
    streakFlameTip = Color(0xFFFDE047),
    streakCool = Color(0xFF94A3B8),

    bannerPeach = Color(0xFFFCD9B8),
    accentLime = Color(0xFFA3D929),
    barDeepCocoa = Color(0xFF4B322C),
    barWarmRust = Color(0xFF8C3F0B),
    barMatchaOlive = Color(0xFF7FA62D),
    barSoftOrchid = Color(0xFFDC75C2),
    iconSoftPeach = Color(0xFFFFE8D6),
    iconSoftMatcha = Color(0xFFE4F2C3),
    iconSoftLavender = Color(0xFFF5DDF7),

    isDark = false
)

val DarkPersonalTrackerColors = PersonalTrackerColors(
    surfaceBase = Color(0xFF121214),
    surfaceRaised = Color(0xFF1C1C1F),
    surfaceSunken = Color(0xFF262422),
    surfaceOverlay = Color(0x99000000), // rgba(0, 0, 0, 0.6)

    primary = Color(0xFFF26B1D),
    primaryLight = Color(0xFFF98E4E),
    primaryDark = Color(0xFF7A3816),
    primarySubtle = Color(0xFF3D2314),

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
    borderSubtle = Color(0xFF3E3B37),

    daytypeWeekday = Color(0xFF3B82F6),
    daytypeWeekend = Color(0xFF8B5CF6),
    daytypeCollege = Color(0xFF06B6D4),
    daytypeSpecial = Color(0xFFF59E0B),

    coinGold = Color(0xFFFBBF24),
    coinGlow = Color(0x33FBBF24), // rgba(251, 191, 36, 0.2)

    streakFlameCore = Color(0xFFF26B1D),
    streakFlameTip = Color(0xFFFDE047),
    streakCool = Color(0xFF94A3B8),

    bannerPeach = Color(0xFF4A3423),
    accentLime = Color(0xFFA3D929),
    barDeepCocoa = Color(0xFF8D685F),
    barWarmRust = Color(0xFFD46C26),
    barMatchaOlive = Color(0xFFA5D14B),
    barSoftOrchid = Color(0xFFECA0DA),
    iconSoftPeach = Color(0xFF4D382B),
    iconSoftMatcha = Color(0xFF374624),
    iconSoftLavender = Color(0xFF432D47),

    isDark = true
)

val LocalPersonalTrackerColors = staticCompositionLocalOf { LightPersonalTrackerColors }
