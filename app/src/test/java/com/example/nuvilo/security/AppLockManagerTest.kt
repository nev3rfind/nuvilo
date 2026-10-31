package com.example.nuvilo.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AppLockManagerTest {

    private class FakeMonotonicClock(var currentTimeMs: Long = 100_000L) : MonotonicClock {
        override fun elapsedRealtimeMs(): Long = currentTimeMs

        fun advance(ms: Long) {
            currentTimeMs += ms
        }
    }

    private lateinit var fakeClock: FakeMonotonicClock
    private lateinit var lockManager: AppLockManager

    @Before
    fun setUp() {
        fakeClock = FakeMonotonicClock(100_000L)
        lockManager = AppLockManager(fakeClock)
    }

    @Test
    fun appStartsLockedWhenEnabled() {
        lockManager.initialize(lockEnabled = true)
        assertTrue(lockManager.isLocked.value)
    }

    @Test
    fun appStartsUnlockedWhenDisabled() {
        lockManager.initialize(lockEnabled = false)
        assertFalse(lockManager.isLocked.value)
    }

    @Test
    fun unlockClearsLockState() {
        lockManager.initialize(lockEnabled = true)
        assertTrue(lockManager.isLocked.value)

        lockManager.unlock()
        assertFalse(lockManager.isLocked.value)
    }

    @Test
    fun immediateGracePeriod_locksImmediatelyOnForeground() {
        lockManager.initialize(lockEnabled = true)
        lockManager.unlock()
        assertFalse(lockManager.isLocked.value)

        lockManager.onAppBackgrounded()
        fakeClock.advance(100L) // 100ms
        lockManager.onAppForegrounded(lockEnabled = true, gracePeriod = LockGracePeriod.IMMEDIATELY)

        assertTrue(lockManager.isLocked.value)
    }

    @Test
    fun thirtySecondsGracePeriod_remainsUnlockedBefore30Seconds() {
        lockManager.initialize(lockEnabled = true)
        lockManager.unlock()

        lockManager.onAppBackgrounded()
        fakeClock.advance(29_000L) // 29s < 30s
        lockManager.onAppForegrounded(lockEnabled = true, gracePeriod = LockGracePeriod.THIRTY_SECONDS)

        assertFalse("Should remain unlocked under 30s", lockManager.isLocked.value)

        // Now advance past 30s
        lockManager.onAppBackgrounded()
        fakeClock.advance(31_000L) // 31s > 30s
        lockManager.onAppForegrounded(lockEnabled = true, gracePeriod = LockGracePeriod.THIRTY_SECONDS)

        assertTrue("Should lock after 30s", lockManager.isLocked.value)
    }

    @Test
    fun oneMinuteGracePeriod_boundaryCheck() {
        lockManager.initialize(lockEnabled = true)
        lockManager.unlock()

        lockManager.onAppBackgrounded()
        fakeClock.advance(59_999L)
        lockManager.onAppForegrounded(lockEnabled = true, gracePeriod = LockGracePeriod.ONE_MINUTE)
        assertFalse(lockManager.isLocked.value)

        lockManager.onAppBackgrounded()
        fakeClock.advance(60_000L)
        lockManager.onAppForegrounded(lockEnabled = true, gracePeriod = LockGracePeriod.ONE_MINUTE)
        assertTrue(lockManager.isLocked.value)
    }

    @Test
    fun fiveMinutesGracePeriod_boundaryCheck() {
        lockManager.initialize(lockEnabled = true)
        lockManager.unlock()

        lockManager.onAppBackgrounded()
        fakeClock.advance(299_000L) // ~5m
        lockManager.onAppForegrounded(lockEnabled = true, gracePeriod = LockGracePeriod.FIVE_MINUTES)
        assertFalse(lockManager.isLocked.value)

        lockManager.onAppBackgrounded()
        fakeClock.advance(300_001L)
        lockManager.onAppForegrounded(lockEnabled = true, gracePeriod = LockGracePeriod.FIVE_MINUTES)
        assertTrue(lockManager.isLocked.value)
    }

    @Test
    fun fifteenMinutesGracePeriod_boundaryCheck() {
        lockManager.initialize(lockEnabled = true)
        lockManager.unlock()

        lockManager.onAppBackgrounded()
        fakeClock.advance(899_000L) // 14m 59s
        lockManager.onAppForegrounded(lockEnabled = true, gracePeriod = LockGracePeriod.FIFTEEN_MINUTES)
        assertFalse(lockManager.isLocked.value)

        lockManager.onAppBackgrounded()
        fakeClock.advance(900_001L) // 15m 1s
        lockManager.onAppForegrounded(lockEnabled = true, gracePeriod = LockGracePeriod.FIFTEEN_MINUTES)
        assertTrue(lockManager.isLocked.value)
    }

    @Test
    fun systemDialogSuppressesLocking() {
        lockManager.initialize(lockEnabled = true)
        lockManager.unlock()

        // When biometric prompt or SAF picker is active:
        lockManager.setSystemDialogActive(true)
        lockManager.onAppBackgrounded()
        fakeClock.advance(60_000L) // 1 min passed
        lockManager.onAppForegrounded(lockEnabled = true, gracePeriod = LockGracePeriod.IMMEDIATELY)

        assertFalse("System dialog should prevent accidental lock loops", lockManager.isLocked.value)
    }

    @Test
    fun disablingLock_immediatelyUnlocks() {
        lockManager.initialize(lockEnabled = true)
        assertTrue(lockManager.isLocked.value)

        lockManager.onAppForegrounded(lockEnabled = false, gracePeriod = LockGracePeriod.IMMEDIATELY)
        assertFalse(lockManager.isLocked.value)
    }
}
