package com.example.nuvilo.system.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.nuvilo.MainActivity
import com.example.nuvilo.domain.model.ScaledQuantity
import com.example.nuvilo.domain.model.StockBalance
import com.example.nuvilo.domain.model.StockUnit
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_REMINDERS_ID = "novelo_medication_reminders"
        const val CHANNEL_LOW_STOCK_ID = "novelo_low_stock_alerts"
        const val EXTRA_SELECTED_MEDICATION_ID = "selected_medication_id"
    }

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannels()
    }

    fun areNotificationsEnabled(): Boolean {
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val remindersChannel = NotificationChannel(
                CHANNEL_REMINDERS_ID,
                "Medication Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts for scheduled medication doses"
                enableVibration(true)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PRIVATE
            }

            val lowStockChannel = NotificationChannel(
                CHANNEL_LOW_STOCK_ID,
                "Low Stock Alerts",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications when medication supply runs low"
                enableVibration(false)
            }

            notificationManager.createNotificationChannel(remindersChannel)
            notificationManager.createNotificationChannel(lowStockChannel)
        }
    }

    fun showDoseReminderNotification(
        occurrenceId: String,
        medicationId: String,
        medicationName: String,
        medicationStrength: String,
        scheduledEpochMs: Long,
        doseQuantity: ScaledQuantity,
        unit: StockUnit,
        isPrivacyMasked: Boolean,
        stockBalance: StockBalance?,
        hasPendingOrder: Boolean = false,
        pendingOrderQuantity: ScaledQuantity? = null,
        pendingOrderNotes: String? = null
    ) {
        if (!areNotificationsEnabled()) return

        val notificationId = occurrenceId.hashCode()

        // Content intent: Opens MainActivity directly to medication detail
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_SELECTED_MEDICATION_ID, medicationId)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action 1: Take (Confirmed dose)
        val takeIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_TAKE
            putExtra(NotificationActionReceiver.EXTRA_OCCURRENCE_ID, occurrenceId)
            putExtra(NotificationActionReceiver.EXTRA_MEDICATION_ID, medicationId)
            putExtra(NotificationActionReceiver.EXTRA_SCHEDULED_EPOCH_MS, scheduledEpochMs)
            putExtra(NotificationActionReceiver.EXTRA_DOSE_QUANTITY_RAW, doseQuantity.rawScaledValue)
            putExtra(NotificationActionReceiver.EXTRA_STOCK_UNIT, unit.name)
            putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
        }
        val takePendingIntent = PendingIntent.getBroadcast(
            context,
            (occurrenceId + "_take").hashCode(),
            takeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action 2: Skip (Skipped dose, no stock deduction)
        val skipIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_SKIP
            putExtra(NotificationActionReceiver.EXTRA_OCCURRENCE_ID, occurrenceId)
            putExtra(NotificationActionReceiver.EXTRA_MEDICATION_ID, medicationId)
            putExtra(NotificationActionReceiver.EXTRA_SCHEDULED_EPOCH_MS, scheduledEpochMs)
            putExtra(NotificationActionReceiver.EXTRA_DOSE_QUANTITY_RAW, doseQuantity.rawScaledValue)
            putExtra(NotificationActionReceiver.EXTRA_STOCK_UNIT, unit.name)
            putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
        }
        val skipPendingIntent = PendingIntent.getBroadcast(
            context,
            (occurrenceId + "_skip").hashCode(),
            skipIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action 3: Snooze (15 minutes delay without recording dose)
        val snoozeIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_SNOOZE
            putExtra(NotificationActionReceiver.EXTRA_OCCURRENCE_ID, occurrenceId)
            putExtra(NotificationActionReceiver.EXTRA_MEDICATION_ID, medicationId)
            putExtra(NotificationActionReceiver.EXTRA_SCHEDULED_EPOCH_MS, scheduledEpochMs)
            putExtra(NotificationActionReceiver.EXTRA_DOSE_QUANTITY_RAW, doseQuantity.rawScaledValue)
            putExtra(NotificationActionReceiver.EXTRA_STOCK_UNIT, unit.name)
            putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            (occurrenceId + "_snooze").hashCode(),
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
        val scheduledTimeStr = Instant.ofEpochMilli(scheduledEpochMs)
            .atZone(ZoneId.systemDefault())
            .format(timeFormatter)

        val title = if (isPrivacyMasked) {
            "Medication Reminder"
        } else {
            "Time for $medicationName ($medicationStrength)"
        }

        val shortText = if (isPrivacyMasked) {
            "Time for your scheduled medication dose"
        } else {
            val remainingPart = if (stockBalance != null) {
                " · Supply: ${stockBalance.currentBalance.toDisplayString()} ${unit.abbreviation} left"
            } else ""
            "Take ${doseQuantity.toDisplayString()} ${unit.abbreviation}$remainingPart"
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_REMINDERS_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(shortText)
            .setContentIntent(contentPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(if (isPrivacyMasked) NotificationCompat.VISIBILITY_SECRET else NotificationCompat.VISIBILITY_PRIVATE)
            .setAutoCancel(true)
            .addAction(android.R.drawable.checkbox_on_background, "Take", takePendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Skip", skipPendingIntent)
            .addAction(android.R.drawable.ic_popup_sync, "Snooze (15m)", snoozePendingIntent)

        if (!isPrivacyMasked) {
            val subText = NotificationTextBuilder.buildSubText(
                stockBalance = stockBalance,
                unit = unit,
                hasPendingOrder = hasPendingOrder
            )
            subText?.let { builder.setSubText(it) }

            val bigText = NotificationTextBuilder.buildBigText(
                doseQuantity = doseQuantity,
                unit = unit,
                medicationStrength = medicationStrength,
                scheduledTimeStr = scheduledTimeStr,
                stockBalance = stockBalance,
                hasPendingOrder = hasPendingOrder,
                pendingOrderQuantity = pendingOrderQuantity,
                pendingOrderNotes = pendingOrderNotes
            )
            builder.setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
        }

        notificationManager.notify(notificationId, builder.build())
    }

    fun showLowStockNotification(
        medicationId: String,
        medicationName: String,
        currentBalance: ScaledQuantity,
        unit: StockUnit,
        daysRemaining: Int?,
        hasPendingOrder: Boolean = false,
        pendingOrderQuantity: ScaledQuantity? = null
    ) {
        if (!areNotificationsEnabled()) return

        val notificationId = ("low_stock_" + medicationId).hashCode()

        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_SELECTED_MEDICATION_ID, medicationId)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val daysText = if (daysRemaining != null) " (approx. $daysRemaining days left)" else ""
        val title = "Low Stock: $medicationName"
        val contentText = "Only ${currentBalance.toDisplayString()} ${unit.abbreviation} left$daysText."

        val bigText = buildString {
            append("Supply for $medicationName is running low: only ${currentBalance.toDisplayString()} ${unit.abbreviation} left$daysText.")
            if (hasPendingOrder) {
                val qtyStr = pendingOrderQuantity?.let { "for ${it.toDisplayString()} ${unit.abbreviation}" } ?: ""
                append("\n📋 A repeat order $qtyStr is currently pending with your GP/pharmacy. Tap to review or record restock upon collection.")
            } else {
                append("\n⚠️ Recommendation: Contact your GP surgery or submit a repeat prescription order now. Repeat requests typically take 2-3 working days to dispense.")
            }
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_LOW_STOCK_ID)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle(title)
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setContentIntent(contentPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(notificationId, notification)
    }

    fun showTestNotification() {
        if (!areNotificationsEnabled()) return

        val notificationId = 99999
        val contentIntent = Intent(context, MainActivity::class.java)
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDERS_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Novelo Reminder Test")
            .setContentText("Notifications and reminder sound/vibration are working correctly.")
            .setContentIntent(contentPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(notificationId, notification)
    }

    fun cancelNotification(notificationId: Int) {
        notificationManager.cancel(notificationId)
    }
}

object NotificationTextBuilder {
    fun buildSubText(
        stockBalance: StockBalance?,
        unit: StockUnit,
        hasPendingOrder: Boolean
    ): String? {
        return when {
            hasPendingOrder -> "GP Order Pending · ${stockBalance?.currentBalance?.toDisplayString() ?: ""} left"
            stockBalance != null -> "${stockBalance.currentBalance.toDisplayString()} ${unit.abbreviation} left"
            else -> null
        }
    }

    fun buildBigText(
        doseQuantity: ScaledQuantity,
        unit: StockUnit,
        medicationStrength: String,
        scheduledTimeStr: String,
        stockBalance: StockBalance?,
        hasPendingOrder: Boolean = false,
        pendingOrderQuantity: ScaledQuantity? = null,
        pendingOrderNotes: String? = null
    ): String {
        return buildString {
            append("Take ${doseQuantity.toDisplayString()} ${unit.abbreviation} ($medicationStrength) · Scheduled for $scheduledTimeStr")
            if (stockBalance != null) {
                val runwayText = stockBalance.daysRemaining?.let { "approx. $it days runway" } ?: "supply unmetered"
                append("\n📦 Remaining Supply: ${stockBalance.currentBalance.toDisplayString()} ${unit.abbreviation} ($runwayText)")

                if (hasPendingOrder) {
                    val qtyStr = pendingOrderQuantity?.let { "${it.toDisplayString()} ${unit.abbreviation}" } ?: "Repeat"
                    val noteStr = if (!pendingOrderNotes.isNullOrBlank()) " ($pendingOrderNotes)" else ""
                    append("\n📋 GP Order Pending: $qtyStr requested$noteStr — in progress with surgery/pharmacy")
                } else if (stockBalance.isLowStock || (stockBalance.daysRemaining != null && stockBalance.daysRemaining <= 7)) {
                    val daysRemaining = stockBalance.daysRemaining ?: 7
                    val urgency = if (daysRemaining <= 3) "Urgent: " else ""
                    append("\n⚠️ ${urgency}GP Recommendation: Approx. $daysRemaining days supply left. Order repeat prescription now (takes 2-3 working days).")
                } else {
                    val days = stockBalance.daysRemaining
                    if (days != null) {
                        append("\n✅ Supply Healthy: ~$days days runway remaining before next GP reorder.")
                    }
                }
            }
        }
    }
}
