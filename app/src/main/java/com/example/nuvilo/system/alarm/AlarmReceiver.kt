package com.example.nuvilo.system.alarm

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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_DOSE_ALARM = "com.example.nuvilo.ACTION_DOSE_ALARM"
        const val EXTRA_OCCURRENCE_ID = "extra_occurrence_id"
        const val EXTRA_MEDICATION_ID = "extra_medication_id"
        const val EXTRA_SCHEDULED_EPOCH_MS = "extra_scheduled_epoch_ms"
        const val EXTRA_DOSE_QUANTITY_RAW = "extra_dose_quantity_raw"
        const val EXTRA_STOCK_UNIT = "extra_stock_unit"
        const val EXTRA_RECORDING_MODE = "extra_recording_mode"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_DOSE_ALARM) return

        val occurrenceId = intent.getStringExtra(EXTRA_OCCURRENCE_ID) ?: return
        val medicationId = intent.getStringExtra(EXTRA_MEDICATION_ID) ?: return
        val scheduledEpochMs = intent.getLongExtra(EXTRA_SCHEDULED_EPOCH_MS, System.currentTimeMillis())
        val doseQuantityRaw = intent.getLongExtra(EXTRA_DOSE_QUANTITY_RAW, 1000L)
        val unitStr = intent.getStringExtra(EXTRA_STOCK_UNIT) ?: StockUnit.TABLETS.name
        val modeStr = intent.getStringExtra(EXTRA_RECORDING_MODE) ?: RecordingMode.CONFIRM.name

        val unit = runCatching { StockUnit.valueOf(unitStr) }.getOrDefault(StockUnit.TABLETS)
        val recordingMode = runCatching { RecordingMode.valueOf(modeStr) }.getOrDefault(RecordingMode.CONFIRM)
        val quantity = ScaledQuantity(doseQuantityRaw)

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as? NoveloApplication ?: return@launch
                val container = app.container

                val medication = container.repository.getMedication(medicationId)
                if (medication == null || medication.isArchived) return@launch

                val existingDose = container.database.doseDao().getDoseByOccurrenceId(occurrenceId)
                if (existingDose != null && (existingDose.status == DoseStatus.TAKEN.name || existingDose.status == DoseStatus.SKIPPED.name)) {
                    return@launch
                }

                // If AUTO_ASSUME, record assumed dose adherence in database idempotently
                if (recordingMode == RecordingMode.AUTO_ASSUME) {
                    container.repository.recordDose(
                        occurrenceId = occurrenceId,
                        medicationId = medicationId,
                        scheduledEpochMs = scheduledEpochMs,
                        quantity = quantity,
                        unit = unit,
                        status = DoseStatus.ASSUMED,
                        recordedEpochMs = scheduledEpochMs,
                        notes = "Assumed adherence recorded per schedule"
                    )
                }

                val preferences = container.userPreferencesRepository.userPreferencesFlow.first()
                val stockBalance = container.repository.observeStockBalance(medication).first()

                container.notificationHelper.showDoseReminderNotification(
                    occurrenceId = occurrenceId,
                    medicationId = medicationId,
                    medicationName = medication.name,
                    medicationStrength = medication.strength,
                    scheduledEpochMs = scheduledEpochMs,
                    doseQuantity = quantity,
                    unit = unit,
                    isPrivacyMasked = preferences.isNotificationPrivacyMasked,
                    stockBalance = stockBalance
                )
            } finally {
                pendingResult.finish()
            }
        }
    }
}
