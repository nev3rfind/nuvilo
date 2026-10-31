package com.example.nuvilo.system.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.nuvilo.NoveloApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootAndChangeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action !in listOf(
                Intent.ACTION_BOOT_COMPLETED,
                Intent.ACTION_MY_PACKAGE_REPLACED,
                Intent.ACTION_TIMEZONE_CHANGED,
                Intent.ACTION_TIME_CHANGED
            )
        ) return

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as? NoveloApplication ?: return@launch
                val container = app.container

                // Reconcile and reschedule upcoming dose reminders
                container.alarmScheduler.reconcileUpcomingReminders()

                // Re-enqueue deferrable background maintenance
                container.enqueuePeriodicMaintenance()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
