package com.example.nuvilo.system.alarm

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.nuvilo.MainActivity
import com.example.nuvilo.NoveloApplication
import com.example.nuvilo.R
import com.example.nuvilo.domain.ppc.PpcCalculator
import com.example.nuvilo.domain.ppc.PpcCertificate
import com.example.nuvilo.domain.ppc.PpcCoverageType
import com.example.nuvilo.domain.ppc.PpcReminderConfig
import com.example.nuvilo.system.notification.NotificationChannelIds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

data class PpcAlarmTrigger(
    val certificateId: String,
    val offsetDays: Int,
    val triggerEpochMs: Long,
    val requestCode: Int
)

data class PpcNotificationContent(
    val title: String,
    val body: String
)

interface PpcAlarmScheduler {
    fun canScheduleExactAlarms(): Boolean
    fun scheduleRemindersForCertificate(certificate: PpcCertificate)
    fun cancelAlarmsForCertificate(certificateId: String)
    suspend fun reconcileAllPpcAlarms(): Result<Int>
}

object PpcAlarmPlanner {

    private val dateFormatter = DateTimeFormatter.ofPattern("d MMM yyyy")

    fun requestCodeFor(certificateId: String, offsetDays: Int): Int {
        return "ppc_alarm_${certificateId}_$offsetDays".hashCode()
    }

    fun computeTriggers(
        certificate: PpcCertificate,
        calculator: PpcCalculator = PpcCalculator(),
        nowEpochMs: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): List<PpcAlarmTrigger> {
        if (certificate.isArchived) return emptyList()
        if (certificate.isOngoing && certificate.expiryDate == null) return emptyList()

        val effectiveExpiry = certificate.expiryDate ?: calculator.calculateExpiryDate(
            coverageType = certificate.coverageType,
            startDate = certificate.startDate,
            isOngoing = certificate.isOngoing
        ) ?: return emptyList()

        val reminderTime = LocalTime.of(
            certificate.reminderConfig.reminderHour.coerceIn(0, 23),
            certificate.reminderConfig.reminderMinute.coerceIn(0, 59)
        )

        return certificate.reminderConfig.activeOffsetsDays().mapNotNull { offsetDays ->
            val triggerDate = effectiveExpiry.minusDays(offsetDays.toLong())
            val triggerEpochMs = ZonedDateTime.of(triggerDate, reminderTime, zoneId)
                .toInstant()
                .toEpochMilli()

            if (triggerEpochMs > nowEpochMs) {
                PpcAlarmTrigger(
                    certificateId = certificate.id,
                    offsetDays = offsetDays,
                    triggerEpochMs = triggerEpochMs,
                    requestCode = requestCodeFor(certificate.id, offsetDays)
                )
            } else {
                null
            }
        }
    }

    fun buildNotificationContent(
        coverageType: PpcCoverageType,
        expiryDate: LocalDate?,
        offsetDays: Int,
        isPrivacyMasked: Boolean
    ): PpcNotificationContent {
        if (isPrivacyMasked) {
            return PpcNotificationContent(
                title = "Certificate Reminder",
                body = "Open Nuvilo to review your recorded certificate dates."
            )
        }

        val label = when (coverageType) {
            PpcCoverageType.PPC_3_MONTH -> "3-Month Prescription Prepayment Certificate (PPC)"
            PpcCoverageType.PPC_12_MONTH -> "12-Month Prescription Prepayment Certificate (PPC)"
            PpcCoverageType.FREE_EXEMPTION -> "NHS Prescription Exemption"
        }

        val dateText = expiryDate?.format(dateFormatter) ?: "recorded expiry date"

        return when {
            offsetDays <= 0 -> PpcNotificationContent(
                title = "$label expires today",
                body = "Based on your recorded certificate dates, your certificate expires today ($dateText)."
            )
            offsetDays == 1 -> PpcNotificationContent(
                title = "$label expires tomorrow",
                body = "Based on your recorded certificate dates, your certificate expires on $dateText."
            )
            else -> PpcNotificationContent(
                title = "$label expires in $offsetDays days",
                body = "Based on your recorded certificate dates, your certificate expires on $dateText."
            )
        }
    }
}

class PpcAlarmSchedulerImpl(
    private val context: Context,
    private val loadAllCertificates: suspend () -> List<PpcCertificate>,
    private val calculator: PpcCalculator = PpcCalculator()
) : PpcAlarmScheduler {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    override fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    override fun scheduleRemindersForCertificate(certificate: PpcCertificate) {
        // Always cancel existing offsets first to prevent stale or duplicate alarms
        cancelAlarmsForCertificate(certificate.id)

        val triggers = PpcAlarmPlanner.computeTriggers(
            certificate = certificate,
            calculator = calculator
        )
        if (triggers.isEmpty()) return

        val effectiveExpiry = certificate.expiryDate ?: calculator.calculateExpiryDate(
            coverageType = certificate.coverageType,
            startDate = certificate.startDate,
            isOngoing = certificate.isOngoing
        )

        triggers.forEach { trigger ->
            val intent = Intent(context, PpcAlarmReceiver::class.java).apply {
                action = PpcAlarmReceiver.ACTION_PPC_EXPIRY_ALARM
                putExtra(PpcAlarmReceiver.EXTRA_CERTIFICATE_ID, certificate.id)
                putExtra(PpcAlarmReceiver.EXTRA_OFFSET_DAYS, trigger.offsetDays)
                putExtra(PpcAlarmReceiver.EXTRA_COVERAGE_TYPE, certificate.coverageType.name)
                if (effectiveExpiry != null) {
                    putExtra(PpcAlarmReceiver.EXTRA_EXPIRY_EPOCH_DAY, effectiveExpiry.toEpochDay())
                }
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                trigger.requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            try {
                if (canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        trigger.triggerEpochMs,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        trigger.triggerEpochMs,
                        pendingIntent
                    )
                }
            } catch (_: SecurityException) {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    trigger.triggerEpochMs,
                    pendingIntent
                )
            }
        }
    }

    override fun cancelAlarmsForCertificate(certificateId: String) {
        PpcReminderConfig.ALL_STANDARD_OFFSETS.forEach { offsetDays ->
            val intent = Intent(context, PpcAlarmReceiver::class.java).apply {
                action = PpcAlarmReceiver.ACTION_PPC_EXPIRY_ALARM
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                PpcAlarmPlanner.requestCodeFor(certificateId, offsetDays),
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        }
    }

    override suspend fun reconcileAllPpcAlarms(): Result<Int> = runCatching {
        val allCertificates = loadAllCertificates()
        var scheduledCount = 0
        allCertificates.forEach { cert ->
            cancelAlarmsForCertificate(cert.id)
            if (!cert.isArchived) {
                val triggers = PpcAlarmPlanner.computeTriggers(cert, calculator)
                if (triggers.isNotEmpty()) {
                    scheduleRemindersForCertificate(cert)
                    scheduledCount += triggers.size
                }
            }
        }
        scheduledCount
    }
}

class PpcAlarmReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_PPC_EXPIRY_ALARM = "com.example.nuvilo.ACTION_PPC_EXPIRY_ALARM"
        const val EXTRA_CERTIFICATE_ID = "extra_ppc_certificate_id"
        const val EXTRA_OFFSET_DAYS = "extra_ppc_offset_days"
        const val EXTRA_COVERAGE_TYPE = "extra_ppc_coverage_type"
        const val EXTRA_EXPIRY_EPOCH_DAY = "extra_ppc_expiry_epoch_day"
        const val EXTRA_OPEN_PPC_MANAGEMENT = "extra_open_ppc_management"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_PPC_EXPIRY_ALARM) return

        val certificateId = intent.getStringExtra(EXTRA_CERTIFICATE_ID) ?: return
        val offsetDays = intent.getIntExtra(EXTRA_OFFSET_DAYS, 0)
        val coverageTypeStr = intent.getStringExtra(EXTRA_COVERAGE_TYPE) ?: PpcCoverageType.PPC_12_MONTH.name
        val coverageType = runCatching { PpcCoverageType.valueOf(coverageTypeStr) }
            .getOrDefault(PpcCoverageType.PPC_12_MONTH)
        val expiryDate = if (intent.hasExtra(EXTRA_EXPIRY_EPOCH_DAY)) {
            LocalDate.ofEpochDay(intent.getLongExtra(EXTRA_EXPIRY_EPOCH_DAY, 0L))
        } else {
            null
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as? NoveloApplication
                val isPrivacyMasked = app?.container?.userPreferencesRepository
                    ?.userPreferencesFlow?.first()?.isNotificationPrivacyMasked ?: false

                val content = PpcAlarmPlanner.buildNotificationContent(
                    coverageType = coverageType,
                    expiryDate = expiryDate,
                    offsetDays = offsetDays,
                    isPrivacyMasked = isPrivacyMasked
                )

                val tapIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra(EXTRA_OPEN_PPC_MANAGEMENT, true)
                }
                val tapPendingIntent = PendingIntent.getActivity(
                    context,
                    PpcAlarmPlanner.requestCodeFor(certificateId, offsetDays),
                    tapIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val granted = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                    if (!granted) return@launch
                }

                val notification = NotificationCompat.Builder(
                    context,
                    NotificationChannelIds.PRESCRIPTION_REMINDERS
                )
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle(content.title)
                    .setContentText(content.body)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(content.body))
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .setContentIntent(tapPendingIntent)
                    .build()

                val notificationId = PpcAlarmPlanner.requestCodeFor(certificateId, offsetDays)
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                if (manager != null) {
                    manager.notify(notificationId, notification)
                } else {
                    NotificationManagerCompat.from(context).notify(notificationId, notification)
                }
            } finally {
                pendingResult?.finish()
            }
        }
    }
}
