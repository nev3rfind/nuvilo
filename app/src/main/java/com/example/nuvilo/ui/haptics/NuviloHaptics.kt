package com.example.nuvilo.ui.haptics

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

enum class HapticIntensity(
    val displayName: String,
    val baseDurationMs: Long,
    val amplitude: Int
) {
    LOW("Gentle", 18L, 80),
    MEDIUM("Balanced", 32L, 150),
    HIGH("Distinct", 48L, 230)
}

enum class NuviloHapticEvent {
    DOSE_TAKEN,
    DOSE_SKIPPED,
    DOSE_SNOOZED,
    PACKAGE_OPENED,
    RESTOCK_SAVED,
    RECOUNT_SAVED,
    PACKAGE_DISCARDED,
    WARNING_CONFIRMED,
    SELECTION_TICK,
    TEST_HAPTIC
}

data class HapticSettings(
    val enabled: Boolean = true,
    val intensity: HapticIntensity = HapticIntensity.MEDIUM
)

interface NuviloHapticController {
    fun perform(event: NuviloHapticEvent)
    fun currentSettings(): HapticSettings
    fun isHardwareVibratorAvailable(): Boolean
}

/**
 * Pure Kotlin test/policy engine separating decision rules (app preference + system haptic setting + hardware)
 * from Android Vibrator calls so behavior can be exhaustively unit-tested.
 */
object HapticPolicyEvaluator {
    fun shouldTrigger(
        settings: HapticSettings,
        systemHapticsEnabled: Boolean,
        hasVibrator: Boolean,
        event: NuviloHapticEvent
    ): Boolean {
        if (!hasVibrator) return false
        // Allow explicit test feedback in Settings only if app haptics & system allow it
        if (!settings.enabled) return false
        // Respect Android system haptic setting for routine ticks; still allow explicit confirmation pulses if system permits
        return systemHapticsEnabled || event == NuviloHapticEvent.TEST_HAPTIC
    }

    fun patternFor(event: NuviloHapticEvent, intensity: HapticIntensity): LongArray {
        val base = intensity.baseDurationMs
        return when (event) {
            NuviloHapticEvent.SELECTION_TICK -> longArrayOf(0L, (base / 2).coerceAtLeast(10L))
            NuviloHapticEvent.DOSE_SNOOZED -> longArrayOf(0L, (base * 2 / 3).coerceAtLeast(12L))
            NuviloHapticEvent.DOSE_SKIPPED -> longArrayOf(0L, base, 35L, (base / 2).coerceAtLeast(10L))
            NuviloHapticEvent.DOSE_TAKEN,
            NuviloHapticEvent.RESTOCK_SAVED,
            NuviloHapticEvent.RECOUNT_SAVED -> longArrayOf(0L, base)
            NuviloHapticEvent.PACKAGE_OPENED -> longArrayOf(0L, base, 45L, base)
            NuviloHapticEvent.PACKAGE_DISCARDED,
            NuviloHapticEvent.WARNING_CONFIRMED -> longArrayOf(0L, (base + 15L), 40L, (base + 15L))
            NuviloHapticEvent.TEST_HAPTIC -> longArrayOf(0L, base, 50L, base)
        }
    }
}

class AndroidNuviloHapticController(
    private val context: Context,
    private val settingsProvider: () -> HapticSettings,
    private val composeFallback: HapticFeedback? = null
) : NuviloHapticController {

    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vm?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    override fun currentSettings(): HapticSettings = settingsProvider()

    override fun isHardwareVibratorAvailable(): Boolean = vibrator?.hasVibrator() == true

    private fun isSystemHapticFeedbackEnabled(): Boolean {
        return runCatching {
            Settings.System.getInt(context.contentResolver, Settings.System.HAPTIC_FEEDBACK_ENABLED, 1) == 1
        }.getOrDefault(true)
    }

    override fun perform(event: NuviloHapticEvent) {
        val settings = settingsProvider()
        val hasVib = isHardwareVibratorAvailable()
        if (!HapticPolicyEvaluator.shouldTrigger(settings, isSystemHapticFeedbackEnabled(), hasVib, event)) {
            return
        }

        val vib = vibrator ?: run {
            composeFallback?.performHapticFeedback(
                if (event == NuviloHapticEvent.SELECTION_TICK) HapticFeedbackType.TextHandleMove else HapticFeedbackType.LongPress
            )
            return
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = HapticPolicyEvaluator.patternFor(event, settings.intensity)
                val amp = settings.intensity.amplitude.coerceIn(1, 255)
                val amplitudes = IntArray(timings.size) { index ->
                    if (index % 2 == 0) 0 else amp
                }
                if (timings.size == 2 && vib.hasAmplitudeControl()) {
                    vib.vibrate(VibrationEffect.createOneShot(timings[1], amp))
                } else {
                    vib.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                }
            } else {
                @Suppress("DEPRECATION")
                vib.vibrate(settings.intensity.baseDurationMs)
            }
        } catch (_: SecurityException) {
            composeFallback?.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }
}

class RecordingNuviloHapticController(
    var settings: HapticSettings = HapticSettings(),
    var systemHapticsEnabled: Boolean = true,
    var hasVibrator: Boolean = true
) : NuviloHapticController {
    val recordedEvents = mutableListOf<NuviloHapticEvent>()

    override fun perform(event: NuviloHapticEvent) {
        if (HapticPolicyEvaluator.shouldTrigger(settings, systemHapticsEnabled, hasVibrator, event)) {
            recordedEvents.add(event)
        }
    }

    override fun currentSettings(): HapticSettings = settings
    override fun isHardwareVibratorAvailable(): Boolean = hasVibrator
}

val LocalNuviloHaptics = staticCompositionLocalOf<NuviloHapticController> {
    object : NuviloHapticController {
        override fun perform(event: NuviloHapticEvent) {}
        override fun currentSettings(): HapticSettings = HapticSettings()
        override fun isHardwareVibratorAvailable(): Boolean = false
    }
}
