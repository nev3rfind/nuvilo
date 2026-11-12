package com.example.nuvilo.ui.haptics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NuviloHapticsControllerTest {

    @Test
    fun whenHapticsEnabled_emitsSemanticEvents() {
        val controller = RecordingNuviloHapticController(
            settings = HapticSettings(enabled = true, intensity = HapticIntensity.HIGH),
            systemHapticsEnabled = true,
            hasVibrator = true
        )

        controller.perform(NuviloHapticEvent.DOSE_TAKEN)
        controller.perform(NuviloHapticEvent.PACKAGE_OPENED)
        controller.perform(NuviloHapticEvent.RESTOCK_SAVED)

        assertEquals(
            listOf(
                NuviloHapticEvent.DOSE_TAKEN,
                NuviloHapticEvent.PACKAGE_OPENED,
                NuviloHapticEvent.RESTOCK_SAVED
            ),
            controller.recordedEvents
        )
    }

    @Test
    fun whenHapticsDisabledInPreferences_suppressesAllEventsIncludingTest() {
        val controller = RecordingNuviloHapticController(
            settings = HapticSettings(enabled = false, intensity = HapticIntensity.MEDIUM),
            systemHapticsEnabled = true,
            hasVibrator = true
        )

        controller.perform(NuviloHapticEvent.DOSE_TAKEN)
        controller.perform(NuviloHapticEvent.TEST_HAPTIC)

        assertTrue(controller.recordedEvents.isEmpty())
    }

    @Test
    fun whenSystemHapticsDisabled_suppressesRoutineInteractions() {
        val controller = RecordingNuviloHapticController(
            settings = HapticSettings(enabled = true, intensity = HapticIntensity.MEDIUM),
            systemHapticsEnabled = false,
            hasVibrator = true
        )

        controller.perform(NuviloHapticEvent.SELECTION_TICK)
        controller.perform(NuviloHapticEvent.DOSE_TAKEN)
        assertTrue(controller.recordedEvents.isEmpty())
    }

    @Test
    fun whenNoHardwareVibrator_returnsFalse() {
        assertFalse(
            HapticPolicyEvaluator.shouldTrigger(
                settings = HapticSettings(enabled = true, intensity = HapticIntensity.HIGH),
                systemHapticsEnabled = true,
                hasVibrator = false,
                event = NuviloHapticEvent.DOSE_TAKEN
            )
        )
    }

    @Test
    fun intensityScalesPulseDurationDeterministically() {
        val low = HapticPolicyEvaluator.patternFor(NuviloHapticEvent.DOSE_TAKEN, HapticIntensity.LOW)
        val high = HapticPolicyEvaluator.patternFor(NuviloHapticEvent.DOSE_TAKEN, HapticIntensity.HIGH)
        assertTrue(high[1] > low[1])
    }
}
