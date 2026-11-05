package com.example.nuvilo.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// =========================================================================
// 1. PRIMARY & INTERACTION PALETTE (UI/UX Pro Max Layered Medical Blue)
// =========================================================================

// Novelo Primary Brand Anchor (NHS Clinical Blue — trust, credibility, heritage)
val NoveloBlue = Color(0xFF005EB8)
val NoveloDarkBlue = Color(0xFF003087)
val NoveloBrightBlue = Color(0xFF0072CE)

// UI/UX Pro Max Clinical Interaction Blue (Buttons, focused borders, active rings)
val NoveloInteractionBlue = Color(0xFF0284C7)
val NoveloLightBlueContainer = Color(0xFFE0F2FE)
val NoveloOnPrimaryContainer = Color(0xFF002B5B)

// UI/UX Pro Max Secondary Cyan/Teal (Accents, active pills, secondary KPI bars)
val NoveloCyanAccent = Color(0xFF0891B2)
val NoveloTealContainer = Color(0xFFDDF5F4)
val NoveloOnTealContainer = Color(0xFF003735)

// Dark Mode Primary & Interaction Palette
val NoveloBlueDark = Color(0xFF6EB4FF)
val NoveloInteractionBlueDark = Color(0xFF38BDF8)
val NoveloBlueContainerDark = Color(0xFF0C4A6E)
val NoveloOnPrimaryDark = Color(0xFF002F64)
val NoveloOnPrimaryContainerDark = Color(0xFFE0F2FE)

val NoveloCyanAccentDark = Color(0xFF22D3EE)
val NoveloTealContainerDark = Color(0xFF164E63)
val NoveloOnTealDark = Color(0xFF083344)
val NoveloOnTealContainerDark = Color(0xFFCFFAFE)

// =========================================================================
// 2. CLINICAL SEMANTIC STATUS PALETTE (WCAG AA >= 5.0:1 Contrast Guaranteed)
// =========================================================================

// Light Mode Semantics
val StatusTaken = Color(0xFF0D6832)
val StatusTakenContainer = Color(0xFFE6F4EA)
val StatusTakenBorder = Color(0xFFA7F3D0)

val StatusAssumed = Color(0xFF005A8D)
val StatusAssumedContainer = Color(0xFFE0F2FE)
val StatusAssumedBorder = Color(0xFFBAE6FD)

val StatusSkipped = Color(0xFF475569)
val StatusSkippedContainer = Color(0xFFF1F5F9)
val StatusSkippedBorder = Color(0xFFCBD5E1)

val StatusMissed = Color(0xFFB91C1C)
val StatusMissedContainer = Color(0xFFFEE2E2)
val StatusMissedBorder = Color(0xFFFECACA)

val StatusWarning = Color(0xFF92400E)
val StatusWarningContainer = Color(0xFFFEF3C7)
val StatusWarningBorder = Color(0xFFFDE68A)

// Dark Mode Semantics
val StatusTakenDark = Color(0xFF6EE7B7)
val StatusTakenContainerDark = Color(0xFF064E3B)
val StatusTakenBorderDark = Color(0xFF047857)

val StatusAssumedDark = Color(0xFF7DD3FC)
val StatusAssumedContainerDark = Color(0xFF0C4A6E)
val StatusAssumedBorderDark = Color(0xFF0369A1)

val StatusSkippedDark = Color(0xFFCBD5E1)
val StatusSkippedContainerDark = Color(0xFF1E293B)
val StatusSkippedBorderDark = Color(0xFF334155)

val StatusMissedDark = Color(0xFFFCA5A5)
val StatusMissedContainerDark = Color(0xFF7F1D1D)
val StatusMissedBorderDark = Color(0xFF991B1B)

val StatusWarningDark = Color(0xFFFCD34D)
val StatusWarningContainerDark = Color(0xFF78350F)
val StatusWarningBorderDark = Color(0xFF92400E)

// =========================================================================
// 3. NEUTRAL SURFACES & SLATE TYPOGRAPHY HIERARCHY
// =========================================================================

// Light Mode Neutrals (Slate 50 background + White cards)
val NeutralBackgroundLight = Color(0xFFF8FAFC)
val NeutralSurfaceLight = Color(0xFFFFFFFF)
val NeutralSurfaceVariantLight = Color(0xFFF1F5F9)
val NeutralTextPrimaryLight = Color(0xFF0F172A)
val NeutralTextSecondaryLight = Color(0xFF475569)
val NeutralTextTertiaryLight = Color(0xFF64748B)
val NeutralOutlineLight = Color(0xFFCBD5E1)
val NeutralOutlineVariantLight = Color(0xFFE2E8F0)

// Dark Mode Neutrals (Slate 900 background + Slate 800 cards)
val NeutralBackgroundDark = Color(0xFF0F172A)
val NeutralSurfaceDark = Color(0xFF1E293B)
val NeutralSurfaceVariantDark = Color(0xFF334155)
val NeutralTextPrimaryDark = Color(0xFFF8FAFC)
val NeutralTextSecondaryDark = Color(0xFF94A3B8)
val NeutralTextTertiaryDark = Color(0xFF64748B)
val NeutralOutlineDark = Color(0xFF475569)
val NeutralOutlineVariantDark = Color(0xFF334155)

/**
 * Curated medication accent tags for visual differentiation across regimens.
 */
val MedicationAccentSwatches: List<Pair<String, String>> = listOf(
    "#005EB8" to "Clinical Blue",
    "#0891B2" to "Clinical Cyan",
    "#4F46E5" to "Indigo",
    "#0D6832" to "Emerald",
    "#B45309" to "Warm Amber",
    "#9333EA" to "Plum",
    "#BE123C" to "Crimson"
)

/**
 * Theme-aware semantic color tokens with UI/UX Pro Max extensions.
 */
@Immutable
data class NoveloSemanticColors(
    val taken: Color,
    val takenContainer: Color,
    val takenBorder: Color,
    val assumed: Color,
    val assumedContainer: Color,
    val assumedBorder: Color,
    val skipped: Color,
    val skippedContainer: Color,
    val skippedBorder: Color,
    val missed: Color,
    val missedContainer: Color,
    val missedBorder: Color,
    val warning: Color,
    val warningContainer: Color,
    val warningBorder: Color,
    val cardBorder: Color,
    val elevatedSurface: Color,
    val heroGradientStart: Color,
    val heroGradientEnd: Color,
    val interactionBlue: Color,
    val accentCyan: Color,
    val bulletBenchmark: Color,
    val isDark: Boolean
)

val LightNoveloSemanticColors = NoveloSemanticColors(
    taken = StatusTaken,
    takenContainer = StatusTakenContainer,
    takenBorder = StatusTakenBorder,
    assumed = StatusAssumed,
    assumedContainer = StatusAssumedContainer,
    assumedBorder = StatusAssumedBorder,
    skipped = StatusSkipped,
    skippedContainer = StatusSkippedContainer,
    skippedBorder = StatusSkippedBorder,
    missed = StatusMissed,
    missedContainer = StatusMissedContainer,
    missedBorder = StatusMissedBorder,
    warning = StatusWarning,
    warningContainer = StatusWarningContainer,
    warningBorder = StatusWarningBorder,
    cardBorder = NeutralOutlineVariantLight,
    elevatedSurface = NeutralSurfaceLight,
    heroGradientStart = Color(0xFFE0F2FE),
    heroGradientEnd = Color(0xFFBAE6FD),
    interactionBlue = NoveloInteractionBlue,
    accentCyan = NoveloCyanAccent,
    bulletBenchmark = Color(0xFF0F172A),
    isDark = false
)

val DarkNoveloSemanticColors = NoveloSemanticColors(
    taken = StatusTakenDark,
    takenContainer = StatusTakenContainerDark,
    takenBorder = StatusTakenBorderDark,
    assumed = StatusAssumedDark,
    assumedContainer = StatusAssumedContainerDark,
    assumedBorder = StatusAssumedBorderDark,
    skipped = StatusSkippedDark,
    skippedContainer = StatusSkippedContainerDark,
    skippedBorder = StatusSkippedBorderDark,
    missed = StatusMissedDark,
    missedContainer = StatusMissedContainerDark,
    missedBorder = StatusMissedBorderDark,
    warning = StatusWarningDark,
    warningContainer = StatusWarningContainerDark,
    warningBorder = StatusWarningBorderDark,
    cardBorder = NeutralOutlineVariantDark,
    elevatedSurface = Color(0xFF243248),
    heroGradientStart = Color(0xFF0C4A6E),
    heroGradientEnd = Color(0xFF075985),
    interactionBlue = NoveloInteractionBlueDark,
    accentCyan = NoveloCyanAccentDark,
    bulletBenchmark = Color(0xFFF8FAFC),
    isDark = true
)

val LocalNoveloColors = staticCompositionLocalOf { LightNoveloSemanticColors }
