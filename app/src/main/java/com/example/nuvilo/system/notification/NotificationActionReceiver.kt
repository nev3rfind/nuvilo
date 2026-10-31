package com.example.nuvilo.system.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.nuvilo.NoveloApplication
import com.example.nuvilo.domain.model.DoseStatus
import com.example.nuvilo.domain.model.RecordingMode
import com.example.nuvilo.domain.model.ScaledQuantity
import com.example.nuvilo.domain.model.StockUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_TAKE = "com.example.nuvilo.ACTION_TAKE"
        const val ACTION_SKIP = "com.example.nuvilo.ACTION_SKIP"
        const val ACTION_SNOOZE = "com.example.nuvilo.ACTION_SNOOZE"

        const val EXTRA_OCCURRENCE_ID = "extra_occurrence_id"
        const val EXTRA_MEDICATION_ID = "extra_medication_id"
        const val EXTRA_SCHEDULED_EPOCH_MS = "extra_scheduled_epoch_ms"
        const val EXTRA_DOSE_QUANTITY_RAW = "extra_dose_quantity_raw"
        const val EXTRA_STOCK_UNIT = "extra_stock_unit"
        const val EXTRA_RECORDING_MODE = "extra_recording_mode"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val occurrenceId = intent.getStringExtra(EXTRA_OCCURRENCE_ID) ?: return
        val medicationId = intent.getStringExtra(EXTRA_MEDICATION_ID) ?: return
        val scheduledEpochMs = intent.getLongExtra(EXTRA_SCHEDULED_EPOCH_MS, System.currentTimeMillis())
        val doseQuantityRaw = intent.getLongExtra(EXTRA_DOSE_QUANTITY_RAW, 1000L)
        val unitStr = intent.getStringExtra(EXTRA_STOCK_UNIT) ?: StockUnit.TABLETS.name
        val modeStr = intent.getStringExtra(EXTRA_RECORDING_MODE) ?: RecordingMode.CONFIRM.name
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, occurrenceId.hashCode())

        val unit = runCatching { StockUnit.valueOf(unitStr) }.getOrDefault(StockUnit.TABLETS)
        val recordingMode = runCatching { RecordingMode.valueOf(modeStr) }.getOrDefault(RecordingMode.CONFIRM)
        val quantity = ScaledQuantity(doseQuantityRaw)

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as? NoveloApplication ?: return@launch
                val container = app.container

                when (action) {
                    ACTION_TAKE -> {
                        container.repository.recordDose(
                            occurrenceId = occurrenceId,
                            medicationId = medicationId,
                            scheduledEpochMs = scheduledEpochMs,
                            quantity = quantity,
                            unit = unit,
                            status = DoseStatus.TAKEN,
                            notes = "Confirmed taken via notification"
                        )
                        container.notificationHelper.cancelNotification(notificationId)
                    }

                    ACTION_SKIP -> {
                        val existingDose = container.database.doseDao().getDoseByOccurrenceId(occurrenceId)
                        if (existingDose != null && existingDose.status == DoseStatus.ASSUMED.name) {
                            // If it was already assumed, use compensating reversal to restore stock
                            container.repository.correctDose(
                                occurrenceId = occurrenceId,
                                targetStatus = DoseStatus.SKIPPED,
                                reason = "User marked skipped from notification"
                            )
                        } else {
                            container.repository.recordDose(
                                occurrenceId = occurrenceId,
                                medicationId = medicationId,
                                scheduledEpochMs = scheduledEpochMs,
                                quantity = quantity,
                                unit = unit,
                                status = DoseStatus.SKIPPED,
                                notes = "Skipped via notification"
                            )
                        }
                        container.notificationHelper.cancelNotification(notificationId)
                    }

                    ACTION_SNOOZE -> {
                        container.alarmScheduler.snoozeOccurrence(
                            occurrenceId = occurrenceId,
                            medicationId = medicationId,
                            doseQuantity = quantity,
                            unit = unit,
                            recordingMode = recordingMode,
                            snoozeMinutes = 15
                        )
                        container.notificationHelper.cancelNotification(notificationId)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
