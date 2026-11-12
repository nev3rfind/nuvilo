package com.example.nuvilo.system.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.nuvilo.MainActivity
import com.example.nuvilo.data.preferences.UserPreferences
import com.example.nuvilo.domain.model.Medication
import com.example.nuvilo.domain.model.ScaledQuantity
import com.example.nuvilo.domain.model.StockBalance
import com.example.nuvilo.domain.model.StockLot
import com.example.nuvilo.domain.model.StockUnit
import com.example.nuvilo.domain.model.TrackingPrecision
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_REMINDERS_ID = NotificationChannelIds.MEDICATION_REMINDERS
        const val CHANNEL_LOW_STOCK_ID = NotificationChannelIds.LOW_STOCK_ALERTS
        const val EXTRA_SELECTED_MEDICATION_ID = "selected_medication_id"
    }

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannels()
    }

    fun areNotificationsEnabled(): Boolean {
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channels = NotificationChannelRegistry.allChannelSpecs().map { spec ->
                NotificationChannel(
                    spec.id,
                    spec.name,
                    spec.importanceLevel
                ).apply {
                    description = spec.description
                    enableVibration(spec.defaultVibrationEnabled)
                    if (spec.defaultSilent) {
                        setSound(null, null)
                    }
                    lockscreenVisibility = if (spec.id == NotificationChannelIds.MEDICATION_REMINDERS ||
                        spec.id == NotificationChannelIds.SECURITY_EVENTS
                    ) {
                        NotificationCompat.VISIBILITY_PRIVATE
                    } else {
                        NotificationCompat.VISIBILITY_PUBLIC
                    }
                }
            }
            notificationManager.createNotificationChannels(channels)
        }
    }

    fun getChannelDiagnostics(prefs: UserPreferences = UserPreferences()): List<ChannelRuntimeDiagnostic> {
        return NuviloNotificationCategory.entries.map { category ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val systemChannel = notificationManager.getNotificationChannel(category.channelId)
                if (systemChannel != null) {
                    ChannelRuntimeDiagnostic(
                        category = category,
                        isBlockedInSystem = systemChannel.importance == NotificationManager.IMPORTANCE_NONE,
                        hasSoundInSystem = systemChannel.sound != null,
                        hasVibrationInSystem = systemChannel.shouldVibrate()
                    )
                } else {
                    ChannelRuntimeDiagnostic(
                        category = category,
                        isBlockedInSystem = false,
                        hasSoundInSystem = prefs.isCategorySoundEnabled(category),
                        hasVibrationInSystem = prefs.isCategoryVibrationEnabled(category)
                    )
                }
            } else {
                ChannelRuntimeDiagnostic(
                    category = category,
                    isBlockedInSystem = !areNotificationsEnabled(),
                    hasSoundInSystem = prefs.isCategorySoundEnabled(category),
                    hasVibrationInSystem = prefs.isCategoryVibrationEnabled(category)
                )
            }
        }
    }

    fun buildChannelSettingsIntent(channelId: String): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                putExtra(Settings.EXTRA_CHANNEL_ID, channelId)
            }
        } else {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra("app_package", context.packageName)
                putExtra("app_uid", context.applicationInfo.uid)
            }
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
        pendingOrderNotes: String? = null,
        medication: Medication? = null,
        prefs: UserPreferences? = null
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
            val instruction = if (medication != null) {
                NotificationTextBuilder.buildDoseInstructionLine(medication, doseQuantity)
            } else {
                "${doseQuantity.toDisplayString()} ${unit.abbreviation}"
            }
            val remainingPart = if (stockBalance != null) {
                " · Supply: ${stockBalance.currentBalance.toDisplayString()} ${unit.abbreviation} left"
            } else ""
            "Take $instruction$remainingPart"
        }

        val reminderCategory = com.example.nuvilo.system.notification.NuviloNotificationCategory.MEDICATION_REMINDERS
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

        // Pre-O fallback for sound/vibration preferences (API 26+ channels are owned by the OS)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O && prefs != null) {
            val soundOn = prefs.isCategorySoundEnabled(reminderCategory)
            val vibOn = prefs.isCategoryVibrationEnabled(reminderCategory)
            var defaults = 0
            if (soundOn) defaults = defaults or NotificationCompat.DEFAULT_SOUND
            if (vibOn) defaults = defaults or NotificationCompat.DEFAULT_VIBRATE
            builder.setDefaults(defaults)
            if (!soundOn && !vibOn) builder.setSilent(true)
        }

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
                pendingOrderNotes = pendingOrderNotes,
                medication = medication
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
        pendingOrderQuantity: ScaledQuantity? = null,
        isPrivacyMasked: Boolean = false,
        precision: TrackingPrecision? = null
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

        val alert = NotificationTextBuilder.buildLowStockAlert(
            medicationName = medicationName,
            currentBalance = currentBalance,
            unit = unit,
            daysRemaining = daysRemaining,
            hasPendingOrder = hasPendingOrder,
            pendingOrderQuantity = pendingOrderQuantity,
            isPrivacyMasked = isPrivacyMasked,
            precision = precision
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_LOW_STOCK_ID)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle(alert.title)
            .setContentText(alert.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(alert.body))
            .setVisibility(if (isPrivacyMasked) NotificationCompat.VISIBILITY_SECRET else NotificationCompat.VISIBILITY_PRIVATE)
            .setContentIntent(contentPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(notificationId, notification)
    }

    fun showExpiryWarningNotification(
        medication: Medication,
        lot: StockLot,
        daysUntilExpiry: Int,
        isPrivacyMasked: Boolean
    ) {
        if (!areNotificationsEnabled()) return

        val notificationId = ("expiry_" + lot.id).hashCode()
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_SELECTED_MEDICATION_ID, medication.id)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val payload = NotificationTextBuilder.buildExpiryAlert(
            medication = medication,
            lot = lot,
            daysUntilExpiry = daysUntilExpiry,
            isPrivacyMasked = isPrivacyMasked
        )

        val notification = NotificationCompat.Builder(context, payload.channelId)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle(payload.title)
            .setContentText(payload.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(payload.body))
            .setVisibility(if (isPrivacyMasked) NotificationCompat.VISIBILITY_SECRET else NotificationCompat.VISIBILITY_PRIVATE)
            .setContentIntent(contentPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(notificationId, notification)
    }

    fun showOpenedDiscardWarningNotification(
        medication: Medication,
        lot: StockLot,
        daysUntilDiscard: Int,
        isPrivacyMasked: Boolean
    ) {
        if (!areNotificationsEnabled()) return

        val notificationId = ("discard_" + lot.id).hashCode()
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_SELECTED_MEDICATION_ID, medication.id)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val payload = NotificationTextBuilder.buildOpenedDiscardAlert(
            medication = medication,
            lot = lot,
            daysUntilDiscard = daysUntilDiscard,
            isPrivacyMasked = isPrivacyMasked
        )

        val notification = NotificationCompat.Builder(context, payload.channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(payload.title)
            .setContentText(payload.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(payload.body))
            .setVisibility(if (isPrivacyMasked) NotificationCompat.VISIBILITY_SECRET else NotificationCompat.VISIBILITY_PRIVATE)
            .setContentIntent(contentPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(notificationId, notification)
    }

    fun showTestNotification(category: NuviloNotificationCategory = NuviloNotificationCategory.MEDICATION_REMINDERS) {
        if (!areNotificationsEnabled()) return

        val notificationId = 99990 + category.ordinal
        val contentIntent = Intent(context, MainActivity::class.java)
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, category.channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Nuvilo · ${category.title}")
            .setContentText("Test notification delivered on the ${category.title} channel.")
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
        pendingOrderNotes: String? = null,
        medication: Medication? = null
    ): String {
        val instructionLine = if (medication != null) {
            buildDoseInstructionLine(medication, doseQuantity)
        } else {
            "${doseQuantity.toDisplayString()} ${unit.abbreviation}"
        }
        return buildString {
            append("Take $instructionLine ($medicationStrength) · Scheduled for $scheduledTimeStr")
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

    fun buildDoseInstructionLine(
        medication: com.example.nuvilo.domain.model.Medication,
        quantity: ScaledQuantity
    ): String {
        val unit = medication.effectiveConsumptionUnit
        val admin = medication.administrationProfile
        val pkg = medication.packagingConfig
        if (pkg?.packageType == com.example.nuvilo.domain.model.PackageType.BOX_SINGLE_DOSE ||
            pkg?.packageType == com.example.nuvilo.domain.model.PackageType.SINGLE_DOSE_UNIT ||
            unit == StockUnit.CONTAINERS
        ) {
            val containerQty = admin.containersPerAdministration ?: quantity
            val siteStr = when (admin.site) {
                com.example.nuvilo.domain.model.AdministrationSite.LEFT_EYE -> "Left eye"
                com.example.nuvilo.domain.model.AdministrationSite.RIGHT_EYE -> "Right eye"
                com.example.nuvilo.domain.model.AdministrationSite.BOTH_EYES -> "Both eyes"
                else -> null
            }
            return buildString {
                append("${containerQty.toDisplayString()} ${unit.abbreviation}")
                if (siteStr != null) append(" · $siteStr")
            }
        }
        if (medication.form == com.example.nuvilo.domain.model.MedicationForm.DROPS &&
            admin.site != com.example.nuvilo.domain.model.AdministrationSite.NONE &&
            admin.site != com.example.nuvilo.domain.model.AdministrationSite.CUSTOM
        ) {
            return when (admin.site) {
                com.example.nuvilo.domain.model.AdministrationSite.LEFT_EYE -> {
                    val l = admin.leftEyeQuantity ?: quantity
                    "${l.toDisplayString()} ${unit.abbreviation} · Left eye"
                }
                com.example.nuvilo.domain.model.AdministrationSite.RIGHT_EYE -> {
                    val r = admin.rightEyeQuantity ?: quantity
                    "${r.toDisplayString()} ${unit.abbreviation} · Right eye"
                }
                com.example.nuvilo.domain.model.AdministrationSite.BOTH_EYES -> {
                    val l = admin.leftEyeQuantity ?: ScaledQuantity.ofWhole(1)
                    val r = admin.rightEyeQuantity ?: ScaledQuantity.ofWhole(1)
                    "${quantity.toDisplayString()} ${unit.abbreviation} · Both eyes (${l.toDisplayString()} left, ${r.toDisplayString()} right)"
                }
                else -> "${quantity.toDisplayString()} ${unit.abbreviation}"
            }
        }
        return "${quantity.toDisplayString()} ${unit.abbreviation}"
    }

    data class AlertMessagePayload(
        val title: String,
        val body: String,
        val channelId: String
    )

    fun buildExpiryAlert(
        medication: com.example.nuvilo.domain.model.Medication,
        lot: com.example.nuvilo.domain.model.StockLot,
        daysUntilExpiry: Int,
        isPrivacyMasked: Boolean
    ): AlertMessagePayload {
        if (isPrivacyMasked) {
            val title = if (daysUntilExpiry <= 0) "Medication expiry notice" else "Upcoming expiry reminder"
            val body = if (daysUntilExpiry <= 0) {
                "One of your stored medication packs has reached its expiry date. Open Nuvilo to review."
            } else {
                "A stored medication pack expires in $daysUntilExpiry day(s). Open Nuvilo to check details."
            }
            return AlertMessagePayload(title = title, body = body, channelId = NotificationChannelIds.EXPIRY_WARNINGS)
        }

        val packLabel = lot.packageLabel ?: lot.lotNumber ?: (lot.packageType?.displayName ?: "Pack")
        val title = if (daysUntilExpiry <= 0) {
            "Expired Pack: ${medication.name}"
        } else {
            "Expiring Soon: ${medication.name}"
        }
        val body = if (daysUntilExpiry <= 0) {
            "$packLabel (${lot.remainingQuantity.toDisplayString()} ${lot.unit.abbreviation}) has reached its expiry date. Replace before next dose."
        } else {
            "$packLabel expires in $daysUntilExpiry day(s) with ${lot.remainingQuantity.toDisplayString()} ${lot.unit.abbreviation} remaining."
        }
        return AlertMessagePayload(title = title, body = body, channelId = NotificationChannelIds.EXPIRY_WARNINGS)
    }

    fun buildOpenedDiscardAlert(
        medication: com.example.nuvilo.domain.model.Medication,
        lot: com.example.nuvilo.domain.model.StockLot,
        daysUntilDiscard: Int,
        isPrivacyMasked: Boolean
    ): AlertMessagePayload {
        if (isPrivacyMasked) {
            val title = if (daysUntilDiscard <= 0) "Opened pack discard notice" else "Opened pack reminder"
            val body = if (daysUntilDiscard <= 0) {
                "An opened medication container has reached its post-opening discard window. Open Nuvilo to review."
            } else {
                "An opened medication container should be replaced within $daysUntilDiscard day(s)."
            }
            return AlertMessagePayload(title = title, body = body, channelId = NotificationChannelIds.OPENED_DISCARD_WARNINGS)
        }

        val packLabel = lot.packageLabel ?: (lot.packageType?.displayName ?: "Opened bottle")
        val title = if (daysUntilDiscard <= 0) {
            "Discard After Opening: ${medication.name}"
        } else {
            "Replace Opened Pack Soon: ${medication.name}"
        }
        val body = if (daysUntilDiscard <= 0) {
            "$packLabel has reached its after-opening discard limit (${lot.discardAfterOpeningDays ?: 28} days). Discard safely and open your next sealed pack."
        } else {
            "$packLabel reaches its after-opening discard limit in $daysUntilDiscard day(s) (${lot.remainingQuantity.toDisplayString()} ${lot.unit.abbreviation} left)."
        }
        return AlertMessagePayload(title = title, body = body, channelId = NotificationChannelIds.OPENED_DISCARD_WARNINGS)
    }

    fun buildLowStockAlert(
        medicationName: String,
        currentBalance: ScaledQuantity,
        unit: StockUnit,
        daysRemaining: Int?,
        hasPendingOrder: Boolean = false,
        pendingOrderQuantity: ScaledQuantity? = null,
        isPrivacyMasked: Boolean = false,
        precision: TrackingPrecision? = null
    ): AlertMessagePayload {
        if (isPrivacyMasked) {
            return AlertMessagePayload(
                title = "Medication Low-Stock Notice",
                body = "One of your tracked medications is running low on supply. Open Nuvilo to review.",
                channelId = NotificationChannelIds.LOW_STOCK_ALERTS
            )
        }
        val prefix = if (precision == TrackingPrecision.ESTIMATED || precision == TrackingPrecision.APPROXIMATE) {
            "approx. "
        } else ""
        val daysText = if (daysRemaining != null) " (${prefix}$daysRemaining days left)" else ""
        val title = "Low Stock: $medicationName"
        val body = buildString {
            append("Supply for $medicationName is running low: ${currentBalance.toDisplayString()} ${unit.abbreviation} left$daysText.")
            if (hasPendingOrder) {
                val qtyStr = pendingOrderQuantity?.let { " for ${it.toDisplayString()} ${unit.abbreviation}" } ?: ""
                append("\nA repeat order$qtyStr is currently pending. Tap to review or record collection.")
            } else {
                append("\nConsider requesting a repeat prescription soon.")
            }
        }
        return AlertMessagePayload(title = title, body = body, channelId = NotificationChannelIds.LOW_STOCK_ALERTS)
    }
}
