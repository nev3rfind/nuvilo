package com.example.nuvilo.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Novelo Shape System — M3 Expressive tactile corner radii.
 *
 * Provides harmonious geometry across micro-badges, pill buttons,
 * standard content cards, and modal sheets.
 */
val NoveloShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

/**
 * Modern M3 Expressive Pill shape (fully rounded ends) for status badges,
 * action chips, quick dosage increments, and pill buttons.
 */
val NoveloPillShape = RoundedCornerShape(999.dp)

/**
 * Tonal container card shape for grouped list items and hero sections.
 */
val NoveloCardShape = RoundedCornerShape(18.dp)

/**
 * Dialog and bottom sheet surface shape.
 */
val NoveloSheetShape = RoundedCornerShape(24.dp)
