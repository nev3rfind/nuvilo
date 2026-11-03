package com.example.nuvilo.ui.theme

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith

/**
 * Centralized motion & micro-animation specifications for Novelo.
 * Uses calm, clinical, non-distracting durations and spring physics.
 */
object NoveloMotion {
    const val DURATION_FAST = 160
    const val DURATION_STANDARD = 220
    const val DURATION_EMPHASIZED = 280

    fun <T> standardFloatSpring() = spring<T>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow
    )

    fun screenTransition(): ContentTransform =
        (fadeIn(tween(DURATION_STANDARD, easing = FastOutSlowInEasing)) +
            slideInVertically(tween(DURATION_STANDARD, easing = FastOutSlowInEasing)) { it / 24 })
            .togetherWith(
                fadeOut(tween(DURATION_FAST, easing = FastOutSlowInEasing))
            )

    fun heroTransition(): ContentTransform =
        (fadeIn(tween(DURATION_EMPHASIZED, easing = FastOutSlowInEasing)) +
            scaleIn(tween(DURATION_EMPHASIZED, easing = FastOutSlowInEasing), initialScale = 0.96f))
            .togetherWith(
                fadeOut(tween(DURATION_FAST, easing = FastOutSlowInEasing)) +
                    scaleOut(tween(DURATION_FAST, easing = FastOutSlowInEasing), targetScale = 0.98f)
            )

    fun directionalHorizontalTransition(forward: Boolean): ContentTransform {
        val dir = if (forward) 1 else -1
        return (fadeIn(tween(DURATION_STANDARD, easing = FastOutSlowInEasing)) +
            slideInHorizontally(tween(DURATION_STANDARD, easing = FastOutSlowInEasing)) { (it / 8) * dir })
            .togetherWith(
                fadeOut(tween(DURATION_FAST, easing = FastOutSlowInEasing)) +
                    slideOutHorizontally(tween(DURATION_FAST, easing = FastOutSlowInEasing)) { -(it / 8) * dir }
            )
    }

    fun sectionExpand() =
        expandVertically(tween(DURATION_STANDARD, easing = FastOutSlowInEasing)) +
            fadeIn(tween(DURATION_STANDARD, easing = FastOutSlowInEasing))

    fun sectionCollapse() =
        shrinkVertically(tween(DURATION_FAST, easing = FastOutSlowInEasing)) +
            fadeOut(tween(DURATION_FAST, easing = FastOutSlowInEasing))
}
