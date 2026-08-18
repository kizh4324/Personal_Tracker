package com.personaltracker.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/**
 * Font Families for Personal-Tracker.
 * Plus Jakarta Sans for primary UI, Inter for data-dense and numeric displays.
 * Sourced 1:1 from `design-system.md` Typography section.
 */
val PlusJakartaSansFamily = FontFamily.Default // In production: FontFamily(Font(R.font.plus_jakarta_sans_regular, ...))
val InterFamily = FontFamily.Default // In production: FontFamily(Font(R.font.inter_regular, ...))

@Immutable
data class PersonalTrackerTypography(
    val display: TextStyle = TextStyle(
        fontFamily = PlusJakartaSansFamily,
        fontSize = 28.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 36.sp,
        letterSpacing = (-0.02).em
    ),
    val headline: TextStyle = TextStyle(
        fontFamily = PlusJakartaSansFamily,
        fontSize = 22.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 28.sp,
        letterSpacing = (-0.01).em
    ),
    val title: TextStyle = TextStyle(
        fontFamily = PlusJakartaSansFamily,
        fontSize = 18.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 24.sp,
        letterSpacing = 0.em
    ),
    val body: TextStyle = TextStyle(
        fontFamily = PlusJakartaSansFamily,
        fontSize = 15.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 22.sp,
        letterSpacing = 0.01.em
    ),
    val bodyMedium: TextStyle = TextStyle(
        fontFamily = PlusJakartaSansFamily,
        fontSize = 15.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 22.sp,
        letterSpacing = 0.01.em
    ),
    val bodySecondary: TextStyle = TextStyle(
        fontFamily = PlusJakartaSansFamily,
        fontSize = 14.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 20.sp,
        letterSpacing = 0.01.em
    ),
    val label: TextStyle = TextStyle(
        fontFamily = PlusJakartaSansFamily,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 18.sp,
        letterSpacing = 0.02.em
    ),
    val caption: TextStyle = TextStyle(
        fontFamily = PlusJakartaSansFamily,
        fontSize = 11.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 16.sp,
        letterSpacing = 0.03.em
    ),
    val data: TextStyle = TextStyle(
        fontFamily = InterFamily,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 18.sp,
        letterSpacing = 0.01.em
    ),
    val dataLarge: TextStyle = TextStyle(
        fontFamily = InterFamily,
        fontSize = 32.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 40.sp,
        letterSpacing = (-0.02).em
    )
)

val LocalPersonalTrackerTypography = staticCompositionLocalOf { PersonalTrackerTypography() }
