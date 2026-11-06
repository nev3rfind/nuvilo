package com.example.nuvilo.regression

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.nuvilo.system.receiver.BootAndChangeReceiver
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Regression Test Suite: Category D (Scheduling, Notifications & Lifecycle)
 * Tests T126 to T130: System Broadcast Receivers and Lifecycle Rescheduling
 */
@RunWith(AndroidJUnit4::class)
class SystemEventsRegressionTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun t126_bootCompletedBroadcastIsRegisteredInManifest() {
        // T126: BootAndChangeReceiver is declared to receive ACTION_BOOT_COMPLETED
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_BOOT_COMPLETED).setPackage(context.packageName)
        val receivers = pm.queryBroadcastReceivers(intent, PackageManager.GET_RESOLVED_FILTER)

        assertTrue(
            "BootAndChangeReceiver must be registered for ACTION_BOOT_COMPLETED",
            receivers.any { it.activityInfo.name == BootAndChangeReceiver::class.java.name }
        )
    }

    @Test
    fun t127_timeAndZoneChangeBroadcastsAreRegisteredInManifest() {
        // T127: BootAndChangeReceiver is declared to receive TIME_CHANGED and TIMEZONE_CHANGED
        val pm = context.packageManager
        val timeIntent = Intent(Intent.ACTION_TIME_CHANGED).setPackage(context.packageName)
        val tzIntent = Intent(Intent.ACTION_TIMEZONE_CHANGED).setPackage(context.packageName)

        val timeReceivers = pm.queryBroadcastReceivers(timeIntent, PackageManager.GET_RESOLVED_FILTER)
        val tzReceivers = pm.queryBroadcastReceivers(tzIntent, PackageManager.GET_RESOLVED_FILTER)

        assertTrue(
            "BootAndChangeReceiver must handle ACTION_TIME_CHANGED",
            timeReceivers.any { it.activityInfo.name == BootAndChangeReceiver::class.java.name }
        )
        assertTrue(
            "BootAndChangeReceiver must handle ACTION_TIMEZONE_CHANGED",
            tzReceivers.any { it.activityInfo.name == BootAndChangeReceiver::class.java.name }
        )
    }

    @Test
    fun t128_unrelatedSystemBroadcastsAreSafelyIgnored() {
        // T128: Receiver ignores unrelated broadcasts without initiating background work
        val receiver = BootAndChangeReceiver()
        val unrelatedIntent = Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED)

        // Should return early and safely without calling goAsync()
        receiver.onReceive(context, unrelatedIntent)
    }

    @Test
    fun t129_packageReplacedBroadcastIsRegisteredInManifest() {
        // T129: Package replaced action triggers reminder reconciliation
        val pm = context.packageManager
        val replacedIntent = Intent(Intent.ACTION_MY_PACKAGE_REPLACED).setPackage(context.packageName)
        val receivers = pm.queryBroadcastReceivers(replacedIntent, PackageManager.GET_RESOLVED_FILTER)

        assertTrue(
            "BootAndChangeReceiver must handle ACTION_MY_PACKAGE_REPLACED",
            receivers.any { it.activityInfo.name == BootAndChangeReceiver::class.java.name }
        )
    }

    @Test
    fun t130_bootReceiverIsDeclaredInManifest() {
        // T130: Receiver component configuration is exported and enabled in AndroidManifest
        val pm = context.packageManager
        val component = ComponentName(context, BootAndChangeReceiver::class.java)
        val receiverInfo = pm.getReceiverInfo(component, PackageManager.GET_META_DATA)

        assertNotNull(receiverInfo)
        assertTrue("BootAndChangeReceiver must be exported to receive system broadcasts", receiverInfo.exported)
        assertTrue("BootAndChangeReceiver must be enabled", receiverInfo.enabled)
    }

    @Test
    fun t130b_receiverOnReceiveNullSafetyWhenGoAsyncReturnsNull() {
        // Verifies BUG-001 fix: direct invocation when goAsync() is null must never throw NPE
        val receiver = BootAndChangeReceiver()
        receiver.onReceive(context, Intent(Intent.ACTION_BOOT_COMPLETED))
    }
}
