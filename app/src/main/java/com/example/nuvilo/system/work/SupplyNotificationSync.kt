package com.example.nuvilo.system.work

import android.content.Context
import com.example.nuvilo.NoveloApplication
import com.example.nuvilo.domain.model.PackageStatus
import com.example.nuvilo.domain.model.PrescriptionStatus
import com.example.nuvilo.system.notification.NotificationHelper
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Supply Intelligence synchronizer that reconciles notification drawer alerts immediately
 * upon inventory mutations (early package depletion, recounts, restocks, package openings, and discards)
 * without waiting for periodic WorkManager execution.
 */
class SupplyNotificationSync(
    private val context: Context,
    private val notificationHelper: NotificationHelper
) {

    suspend fun syncSupplyNotifications(
        medicationId: String,
        isDepletedEarly: Boolean = false,
        depletedLotLabel: String? = null
    ) {
        val app = context.applicationContext as? NoveloApplication ?: return
        val container = app.container

        val med = container.repository.getMedication(medicationId) ?: return
        if (med.isArchived) return

        val nowMs = System.currentTimeMillis()
        val zoneId = ZoneId.systemDefault()
        val today = Instant.ofEpochMilli(nowMs).atZone(zoneId).toLocalDate()

        val balance = container.repository.observeStockBalance(med).first()
        val lots = container.repository.observeLotsForMedication(medicationId).first()
        val schedules = container.repository.getSchedulesForMedication(medicationId)
        val forecast = container.supplyForecastCalculator.calculateSupplyForecast(
            medication = med,
            schedules = schedules,
            lots = lots,
            ledgerTotalUsable = balance.currentBalance,
            nowEpochMs = nowMs,
            zoneId = zoneId
        )

        val preferences = container.userPreferencesRepository.userPreferencesFlow.first()
        val unopenedLots = lots.filter { it.packageStatus == PackageStatus.UNOPENED && it.remainingQuantity.isPositive() }
        val nextLot = unopenedLots.firstOrNull()

        val isLowStock = forecast.isLowStock || balance.isLowStock || (isDepletedEarly && unopenedLots.isEmpty())
        if (isLowStock) {
            val prescriptions = container.repository.observePrescriptionsForMedication(medicationId).first()
            val todayEpochDay = today.toEpochDay()
            val activeRx = prescriptions.firstOrNull {
                it.effectiveStatus(todayEpochDay) == PrescriptionStatus.ACTIVE
            }
            val hasPendingOrder = activeRx?.hasPendingRepeatRequest == true
            val pendingQuantity = if (hasPendingOrder) activeRx?.quantityPrescribed else null

            notificationHelper.showLowStockNotification(
                medicationId = med.id,
                medicationName = med.name,
                currentBalance = forecast.totalUsableUnits,
                unit = forecast.unit,
                daysRemaining = forecast.daysOfSupply ?: balance.daysRemaining,
                hasPendingOrder = hasPendingOrder,
                pendingOrderQuantity = pendingQuantity,
                isPrivacyMasked = preferences.isNotificationPrivacyMasked,
                precision = forecast.precision,
                isDepletedEarly = isDepletedEarly,
                depletedPackageLabel = depletedLotLabel,
                unopenedPackagesCount = unopenedLots.size,
                nextPackageLabel = nextLot?.packageLabel ?: nextLot?.lotNumber
            )
            container.userPreferencesRepository.markLowStockNotified(med.id)
        } else {
            notificationHelper.cancelLowStockNotification(med.id)
            container.userPreferencesRepository.clearLowStockNotified(med.id)
        }

        // Cancel any active expiry or discard notifications for lots that are now EMPTY or DISCARDED
        for (lot in lots) {
            if (lot.packageStatus == PackageStatus.EMPTY || lot.packageStatus == PackageStatus.DISCARDED || !lot.remainingQuantity.isPositive()) {
                notificationHelper.cancelExpiryNotification(lot.id)
                notificationHelper.cancelDiscardNotification(lot.id)
                container.userPreferencesRepository.clearLotExpiryNotified("exp_${lot.id}_expired")
                container.userPreferencesRepository.clearLotExpiryNotified("exp_${lot.id}_soon")
                container.userPreferencesRepository.clearLotDiscardNotified("disc_${lot.id}_due")
                container.userPreferencesRepository.clearLotDiscardNotified("disc_${lot.id}_soon")
            }
        }
    }
}
