package com.example.nuvilo.security

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class LockGracePeriod(val durationMs: Long, val displayName: String) {
    IMMEDIATELY(0L, "Immediately"),
    THIRTY_SECONDS(30_000L, "30 seconds"),
    ONE_MINUTE(60_000L, "1 minute"),
    FIVE_MINUTES(300_000L, "5 minutes"),
    FIFTEEN_MINUTES(900_000L, "15 minutes")
}

interface MonotonicClock {
    fun elapsedRealtimeMs(): Long
}

class SystemMonotonicClock : MonotonicClock {
    override fun elapsedRealtimeMs(): Long = android.os.SystemClock.elapsedRealtime()
}

class AppLockManager(
    private val clock: MonotonicClock = SystemMonotonicClock()
) {
    private val _isLocked = MutableStateFlow(false)
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    private var lastBackgroundedMs: Long = 0L
    private var isSystemDialogActive: Boolean = false
    private var isInitialized: Boolean = false

    /**
     * Called on initial application/activity launch.
     * Starts locked if lock is enabled in user preferences.
     */
    fun initialize(lockEnabled: Boolean) {
        if (!isInitialized) {
            isInitialized = true
            if (lockEnabled) {
                _isLocked.value = true
            }
        }
    }

    /**
     * Called when the app moves to background (e.g. onStop without configuration change).
     */
    fun onAppBackgrounded() {
        if (!isSystemDialogActive) {
            lastBackgroundedMs = clock.elapsedRealtimeMs()
        }
    }

    fun onAppForegrounded(lockEnabled: Boolean, gracePeriod: LockGracePeriod) {
        if (!lockEnabled) {
            _isLocked.value = false
            return
        }

        if (_isLocked.value) {
            return
        }

        if (isSystemDialogActive) {
            isSystemDialogActive = false
            return
        }

        val now = clock.elapsedRealtimeMs()
        if (lastBackgroundedMs > 0L) {
            val elapsed = now - lastBackgroundedMs
            if (elapsed >= gracePeriod.durationMs) {
                _isLocked.value = true
            }
        }
    }

    /**
     * Flag to suppress locking when a system dialog (e.g. BiometricPrompt or SAF document picker)
     * triggers activity pause/stop events.
     */
    fun setSystemDialogActive(active: Boolean) {
        isSystemDialogActive = active
    }

    /**
     * Explicitly unlocks the app upon successful biometric or credential verification.
     */
    fun unlock() {
        _isLocked.value = false
        lastBackgroundedMs = 0L
    }

    /**
     * Explicitly locks the app (e.g. user action or immediate lock triggered).
     */
    fun lock() {
        _isLocked.value = true
    }
}
