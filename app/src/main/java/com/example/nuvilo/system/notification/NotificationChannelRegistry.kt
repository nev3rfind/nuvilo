package com.example.nuvilo.system.notification

import android.app.NotificationManager
import android.os.Build

object NotificationChannelIds {
    const val MEDICATION_REMINDERS = "novelo_medication_reminders"
    const val UPCOMING_REMINDERS = "novelo_upcoming_reminders"
    const val LOW_STOCK_ALERTS = "novelo_low_stock_alerts"
    const val EXPIRY_WARNINGS = "novelo_expiry_warnings"
    const val OPENED_DISCARD_WARNINGS = "novelo_opened_discard_warnings"
    const val PRESCRIPTION_REMINDERS = "novelo_prescription_reminders"
    const val SECURITY_EVENTS = "novelo_security_events"
}

enum class NuviloNotificationCategory(
    val channelId: String,
    val title: String,
    val summary: String,
    val defaultSound: Boolean,
    val defaultVibration: Boolean
) {
    MEDICATION_REMINDERS(
        channelId = NotificationChannelIds.MEDICATION_REMINDERS,
        title = "Medication Reminders",
        summary = "Silent with vibration by default for due and snoozed doses.",
        defaultSound = false,
        defaultVibration = true
    ),
    UPCOMING_REMINDERS(
        channelId = NotificationChannelIds.UPCOMING_REMINDERS,
        title = "Upcoming Reminders",
        summary = "Gentle advance notices before scheduled administration windows.",
        defaultSound = false,
        defaultVibration = true
    ),
    LOW_STOCK_WARNINGS(
        channelId = NotificationChannelIds.LOW_STOCK_ALERTS,
        title = "Low Stock Alerts",
        summary = "Restock alerts based on usable packages and forecast runway.",
        defaultSound = true,
        defaultVibration = true
    ),
    EXPIRY_WARNINGS(
        channelId = NotificationChannelIds.EXPIRY_WARNINGS,
        title = "Expiry Warnings",
        summary = "Alerts when packages are nearing or past manufacturer expiry.",
        defaultSound = false,
        defaultVibration = true
    ),
    OPENED_DISCARD_WARNINGS(
        channelId = NotificationChannelIds.OPENED_DISCARD_WARNINGS,
        title = "Opened-Package Discard Warnings",
        summary = "Safety alerts when opened drops or gels near their post-opening discard limit.",
        defaultSound = false,
        defaultVibration = true
    ),
    PRESCRIPTION_REMINDERS(
        channelId = NotificationChannelIds.PRESCRIPTION_REMINDERS,
        title = "Prescription Reminders",
        summary = "Repeat prescription renewals, pending orders, and prescription expiry notices.",
        defaultSound = true,
        defaultVibration = true
    ),
    SECURITY_EVENTS(
        channelId = NotificationChannelIds.SECURITY_EVENTS,
        title = "Security Events",
        summary = "App lock and local security protection notifications.",
        defaultSound = true,
        defaultVibration = true
    )
}

data class NotificationChannelSpec(
    val id: String,
    val name: String,
    val description: String,
    val importanceLevel: Int,
    val defaultSilent: Boolean,
    val defaultVibrationEnabled: Boolean,
    val customSoundUri: String? = null
)

data class ChannelRuntimeDiagnostic(
    val category: NuviloNotificationCategory,
    val isBlockedInSystem: Boolean,
    val hasSoundInSystem: Boolean,
    val hasVibrationInSystem: Boolean
)

object NotificationChannelRegistry {

    fun allChannelSpecs(): List<NotificationChannelSpec> {
        val impHigh = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) NotificationManager.IMPORTANCE_HIGH else 4
        val impDefault = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) NotificationManager.IMPORTANCE_DEFAULT else 3
        return listOf(
            NotificationChannelSpec(
                id = NotificationChannelIds.MEDICATION_REMINDERS,
                name = "Medication Reminders",
                description = "Scheduled and snoozed medication dose alerts (silent with vibration by default)",
                importanceLevel = impHigh,
                defaultSilent = true,
                defaultVibrationEnabled = true,
                customSoundUri = null
            ),
            NotificationChannelSpec(
                id = NotificationChannelIds.UPCOMING_REMINDERS,
                name = "Upcoming Reminders",
                description = "Advance notifications ahead of upcoming doses",
                importanceLevel = impDefault,
                defaultSilent = true,
                defaultVibrationEnabled = true,
                customSoundUri = null
            ),
            NotificationChannelSpec(
                id = NotificationChannelIds.LOW_STOCK_ALERTS,
                name = "Low Stock Alerts",
                description = "Supply runway and restock threshold reminders",
                importanceLevel = impDefault,
                defaultSilent = false,
                defaultVibrationEnabled = true
            ),
            NotificationChannelSpec(
                id = NotificationChannelIds.EXPIRY_WARNINGS,
                name = "Expiry Warnings",
                description = "Manufacturer expiry warnings for unopened or active packs",
                importanceLevel = impDefault,
                defaultSilent = true,
                defaultVibrationEnabled = true,
                customSoundUri = null
            ),
            NotificationChannelSpec(
                id = NotificationChannelIds.OPENED_DISCARD_WARNINGS,
                name = "Opened-Package Discard Warnings",
                description = "Discard-after-opening countdown alerts for opened eye drops and gels",
                importanceLevel = impHigh,
                defaultSilent = true,
                defaultVibrationEnabled = true,
                customSoundUri = null
            ),
            NotificationChannelSpec(
                id = NotificationChannelIds.PRESCRIPTION_REMINDERS,
                name = "Prescription Reminders",
                description = "Prescription expiry and repeat refill alerts",
                importanceLevel = impDefault,
                defaultSilent = false,
                defaultVibrationEnabled = true
            ),
            NotificationChannelSpec(
                id = NotificationChannelIds.SECURITY_EVENTS,
                name = "Security Alerts",
                description = "Biometric app lock and local security notifications",
                importanceLevel = impHigh,
                defaultSilent = false,
                defaultVibrationEnabled = true
            )
        )
    }
}
