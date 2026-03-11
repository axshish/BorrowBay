package com.example.borrowbay.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ── Fallback font families (using system defaults until custom fonts are added) ──
val ManropeFontFamily     = FontFamily.SansSerif
val SourceSans3FontFamily = FontFamily.SansSerif

// ── Material 3 Typography ────────────────────────────────────────────
val BorrowBayTypography = Typography(

    // ── Display ──────────────────────────────────────────────────────
    displayLarge = TextStyle(
        fontFamily  = ManropeFontFamily,
        fontWeight  = FontWeight.Bold,
        fontSize    = 57.sp,
        lineHeight  = 64.sp,
        letterSpacing = (-0.25).sp,
    ),
    displayMedium = TextStyle(
        fontFamily  = ManropeFontFamily,
        fontWeight  = FontWeight.Bold,
        fontSize    = 45.sp,
        lineHeight  = 52.sp,
    ),
    displaySmall = TextStyle(
        fontFamily  = ManropeFontFamily,
        fontWeight  = FontWeight.Bold,
        fontSize    = 36.sp,
        lineHeight  = 44.sp,
    ),

    // ── Headline ─────────────────────────────────────────────────────
    headlineLarge = TextStyle(
        fontFamily  = ManropeFontFamily,
        fontWeight  = FontWeight.Bold,
        fontSize    = 32.sp,
        lineHeight  = 40.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily  = ManropeFontFamily,
        fontWeight  = FontWeight.Bold,
        fontSize    = 28.sp,
        lineHeight  = 36.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily  = ManropeFontFamily,
        fontWeight  = FontWeight.SemiBold,
        fontSize    = 24.sp,
        lineHeight  = 32.sp,
    ),

    // ── Title ────────────────────────────────────────────────────────
    titleLarge = TextStyle(
        fontFamily  = ManropeFontFamily,
        fontWeight  = FontWeight.SemiBold,
        fontSize    = 22.sp,
        lineHeight  = 28.sp,
    ),
    titleMedium = TextStyle(
        fontFamily  = ManropeFontFamily,
        fontWeight  = FontWeight.Medium,
        fontSize    = 16.sp,
        lineHeight  = 24.sp,
        letterSpacing = 0.15.sp,
    ),
    titleSmall = TextStyle(
        fontFamily  = ManropeFontFamily,
        fontWeight  = FontWeight.Medium,
        fontSize    = 14.sp,
        lineHeight  = 20.sp,
        letterSpacing = 0.1.sp,
    ),

    // ── Body ─────────────────────────────────────────────────────────
    bodyLarge = TextStyle(
        fontFamily  = SourceSans3FontFamily,
        fontWeight  = FontWeight.Normal,
        fontSize    = 16.sp,
        lineHeight  = 24.sp,
        letterSpacing = 0.5.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily  = SourceSans3FontFamily,
        fontWeight  = FontWeight.Normal,
        fontSize    = 14.sp,
        lineHeight  = 20.sp,
        letterSpacing = 0.25.sp,
    ),
    bodySmall = TextStyle(
        fontFamily  = SourceSans3FontFamily,
        fontWeight  = FontWeight.Normal,
        fontSize    = 12.sp,
        lineHeight  = 16.sp,
        letterSpacing = 0.4.sp,
    ),

    // ── Label ────────────────────────────────────────────────────────
    labelLarge = TextStyle(
        fontFamily  = SourceSans3FontFamily,
        fontWeight  = FontWeight.SemiBold,
        fontSize    = 14.sp,
        lineHeight  = 20.sp,
        letterSpacing = 0.1.sp,
    ),
    labelMedium = TextStyle(
        fontFamily  = SourceSans3FontFamily,
        fontWeight  = FontWeight.Medium,
        fontSize    = 12.sp,
        lineHeight  = 16.sp,
        letterSpacing = 0.5.sp,
    ),
    labelSmall = TextStyle(
        fontFamily  = SourceSans3FontFamily,
        fontWeight  = FontWeight.Medium,
        fontSize    = 10.sp,
        lineHeight  = 14.sp,
        letterSpacing = 0.5.sp,
    ),
)
