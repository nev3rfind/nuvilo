package com.example.nuvilo.system.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.nuvilo.data.local.dao.DoseDao
import com.example.nuvilo.data.preferences.UserPreferencesRepository
import com.example.nuvilo.data.repository.MedicationRepository
import com.example.nuvilo.domain.model.DoseOccurrence
import com.example.nuvilo.domain.model.DoseStatus
import com.example.nuvilo.domain.model.RecordingMode
import com.example.nuvilo.domain.model.ScaledQuantity
import com.example.nuvilo.domain.model.StockUnit
import com.example.nuvilo.domain.schedule.ScheduleOccurrenceCalculator
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.ZoneId

interface AlarmScheduler {
    fun canScheduleExactAlarms(): Boolean
    fun scheduleOccurrence(occurrence: DoseOccurrence, recordingMode: RecordingMode)
    fun cancelOccurrence(occurrenceId: String)
    fun snoozeOccurrence(
        occurrenceId: String,
        medicationId: String,
        doseQuantity: ScaledQuantity,
        unit: StockUnit,
        recordingMode: RecordingMode,
        snoozeMinutes: Int = 15
    )
    suspend fun reconcileUpcomingReminders(horizonDays: Int = 3): Result<Int>
}

class AlarmSchedulerImpl(
    private val context: Context,
    private val medicationRepository: MedicationRepository,
    private val doseDao: DoseDao,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val scheduleCalculator: ScheduleOccurrenceCalculator = ScheduleOccurrenceCalculator()
) : AlarmScheduler {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    override fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    override fun scheduleOccurrence(occurrence: DoseOccurrence, recordingMode: RecordingMode) {
        val now = System.currentTimeMillis()
        if (occurrence.scheduledEpochMs <= now) return

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_DOSE_ALARM
            putExtra(AlarmReceiver.EXTRA_OCCURRENCE_ID, occurrence.occurrenceId)
            putExtra(AlarmReceiver.EXTRA_MEDICATION_ID, occurrence.medicationId)
            putExtra(AlarmReceiver.EXTRA_SCHEDULED_EPOCH_MS, occurrence.scheduledEpochMs)
            putExtra(AlarmReceiver.EXTRA_DOSE_QUANTITY_RAW, occurrence.doseQuantity.rawScaledValue)
            putExtra(AlarmReceiver.EXTRA_STOCK_UNIT, occurrence.unit.name)
            putExtra(AlarmReceiver.EXTRA_RECORDING_MODE, recordingMode.name)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            occurrence.occurrenceId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    occurrence.scheduledEpochMs,
                    pendingIntent
                )
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    occurrence.scheduledEpochMs,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            // Graceful fallback if exact alarm permission was revoked between check and schedule
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                occurrence.scheduledEpochMs,
                pendingIntent
            )
        }
    }

    override fun cancelOccurrence(occurrenceId: String) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_DOSE_ALARM
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            occurrenceId.hashCode(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    override fun snoozeOccurrence(
        occurrenceId: String,
        medicationId: String,
        doseQuantity: ScaledQuantity,
        unit: StockUnit,
        recordingMode: RecordingMode,
        snoozeMinutes: Int
    ) {
        val triggerEpoch = System.currentTimeMillis() + (snoozeMinutes * 60 * 1000L)

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_DOSE_ALARM
            putExtra(AlarmReceiver.EXTRA_OCCURRENCE_ID, occurrenceId)
            putExtra(AlarmReceiver.EXTRA_MEDICATION_ID, medicationId)
            putExtra(AlarmReceiver.EXTRA_SCHEDULED_EPOCH_MS, triggerEpoch)
            putExtra(AlarmReceiver.EXTRA_DOSE_QUANTITY_RAW, doseQuantity.rawScaledValue)
            putExtra(AlarmReceiver.EXTRA_STOCK_UNIT, unit.name)
            putExtra(AlarmReceiver.EXTRA_RECORDING_MODE, recordingMode.name)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            occurrenceId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerEpoch,
                    pendingIntent
                )
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerEpoch,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerEpoch,
                pendingIntent
            )
        }
    }

    override suspend fun reconcileUpcomingReminders(horizonDays: Int): Result<Int> = runCatching {
        val zoneId = ZoneId.systemDefault()
        val today = LocalDate.now(zoneId)
        val nowMs = System.currentTimeMillis()

        val activeMeds = medicationRepository.observeActiveMedications().first()
        val allSchedules = medicationRepository.observeAllSchedules().first()
        val schedulesByMed = allSchedules.groupBy { it.medicationId }

        var scheduledCount = 0

        for (med in activeMeds) {
            val medSchedules = schedulesByMed[med.id].orEmpty()
            for (sch in medSchedules) {
                for (dayOffset in 0 until horizonDays) {
                    val targetDate = today.plusDays(dayOffset.toLong())
                    val planned = scheduleCalculator.generateOccurrencesForDate(
                        schedule = sch,
                        date = targetDate,
                        zoneId = zoneId,
                        unit = med.stockUnit
                    )

                    for (occ in planned) {
                        if (occ.scheduledEpochMs > nowMs) {
                            val existingDose = doseDao.getDoseByOccurrenceId(occ.occurrenceId)
                            // Skip if already confirmed, assumed, or skipped
                            val isCompleted = existingDose != null && (
                                existingDose.status == DoseStatus.TAKEN.name ||
                                existingDose.status == DoseStatus.ASSUMED.name ||
                                existingDose.status == DoseStatus.SKIPPED.name
                            )
                            if (!isCompleted) {
                                scheduleOccurrence(occ, med.recordingMode)
                                scheduledCount++
                            }
                        }
                    }
                }
            }
        }

        userPreferencesRepository.setLastReconciliationEpochMs(nowMs)
        scheduledCount
    }
}
