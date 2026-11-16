package com.example.nuvilo.ui.theme

import android.animation.ValueAnimator
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
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
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext

/**
 * CompositionLocal indicating whether non-essential motion is enabled.
 * Respects Android's global animator duration scale (`ValueAnimator.areAnimatorsEnabled()`).
 */
val LocalMotionEnabled = staticCompositionLocalOf { true }

@Composable
fun rememberMotionEnabled(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ValueAnimator.areAnimatorsEnabled()
            } else {
                Settings.Global.getFloat(
                    context.contentResolver,
                    Settings.Global.ANIMATOR_DURATION_SCALE,
                    1f
                ) != 0f
            }
        }.getOrDefault(true)
    }
}

/**
 * Centralized motion & micro-animation specifications for Novelo.
 * Uses calm, clinical, non-distracting durations and spring physics,
 * with deterministic reduced-motion fallbacks.
 */
object NoveloMotion {
    const val DURATION_FAST = 160
    const val DURATION_STANDARD = 220
    const val DURATION_EMPHASIZED = 280

    fun <T> standardFloatSpring() = spring<T>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow
    )

    fun screenTransition(motionEnabled: Boolean = true): ContentTransform =
        if (!motionEnabled) {
            EnterTransition.None togetherWith ExitTransition.None
        } else {
            (fadeIn(tween(DURATION_STANDARD, easing = FastOutSlowInEasing)) +
                slideInVertically(tween(DURATION_STANDARD, easing = FastOutSlowInEasing)) { it / 24 })
                .togetherWith(
                    fadeOut(tween(DURATION_FAST, easing = FastOutSlowInEasing))
                )
        }

    fun heroTransition(motionEnabled: Boolean = true): ContentTransform =
        if (!motionEnabled) {
            EnterTransition.None togetherWith ExitTransition.None
        } else {
            (fadeIn(tween(DURATION_EMPHASIZED, easing = FastOutSlowInEasing)) +
                scaleIn(tween(DURATION_EMPHASIZED, easing = FastOutSlowInEasing), initialScale = 0.96f))
                .togetherWith(
                    fadeOut(tween(DURATION_FAST, easing = FastOutSlowInEasing)) +
                        scaleOut(tween(DURATION_FAST, easing = FastOutSlowInEasing), targetScale = 0.98f)
                )
        }

    fun directionalHorizontalTransition(forward: Boolean, motionEnabled: Boolean = true): ContentTransform {
        if (!motionEnabled) {
            return EnterTransition.None togetherWith ExitTransition.None
        }
        val dir = if (forward) 1 else -1
        return (fadeIn(tween(DURATION_STANDARD, easing = FastOutSlowInEasing)) +
            slideInHorizontally(tween(DURATION_STANDARD, easing = FastOutSlowInEasing)) { (it / 8) * dir })
            .togetherWith(
                fadeOut(tween(DURATION_FAST, easing = FastOutSlowInEasing)) +
                    slideOutHorizontally(tween(DURATION_FAST, easing = FastOutSlowInEasing)) { -(it / 8) * dir }
            )
    }

    fun sectionExpand(motionEnabled: Boolean = true): EnterTransition =
        if (!motionEnabled) {
            EnterTransition.None
        } else {
            expandVertically(tween(DURATION_STANDARD, easing = FastOutSlowInEasing)) +
                fadeIn(tween(DURATION_STANDARD, easing = FastOutSlowInEasing))
        }

    fun sectionCollapse(motionEnabled: Boolean = true): ExitTransition =
        if (!motionEnabled) {
            ExitTransition.None
        } else {
            shrinkVertically(tween(DURATION_FAST, easing = FastOutSlowInEasing)) +
                fadeOut(tween(DURATION_FAST, easing = FastOutSlowInEasing))
        }
}
