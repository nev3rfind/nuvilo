package com.example.nuvilo.system.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.nuvilo.NoveloApplication
import com.example.nuvilo.domain.model.PackageStatus
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

class LowStockCheckWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val WORK_NAME = "novelo_low_stock_check_periodic"
        private const val EXPIRY_WARNING_WINDOW_DAYS = 14
        private const val OPENED_DISCARD_WARNING_WINDOW_DAYS = 3
    }

    override suspend fun doWork(): Result {
        val app = applicationContext as? NoveloApplication ?: return Result.failure()
        val container = app.container

        try {
            val nowMs = System.currentTimeMillis()
            val zoneId = ZoneId.systemDefault()
            val today = Instant.ofEpochMilli(nowMs).atZone(zoneId).toLocalDate()
            val activeMeds = container.repository.observeActiveMedications().first()
            val preferences = container.userPreferencesRepository.userPreferencesFlow.first()
            val alreadyNotifiedIds = preferences.lowStockNotifiedMedIds
            val activeExpiryKeys = mutableSetOf<String>()
            val activeDiscardKeys = mutableSetOf<String>()

            for (med in activeMeds) {
                val balance = container.repository.observeStockBalance(med).first()
                val lots = container.repository.observeLotsForMedication(med.id).first()
                val schedules = container.repository.getSchedulesForMedication(med.id)
                val forecast = container.supplyForecastCalculator.calculateSupplyForecast(
                    medication = med,
                    schedules = schedules,
                    lots = lots,
                    ledgerTotalUsable = balance.currentBalance,
                    nowEpochMs = nowMs,
                    zoneId = zoneId
                )

                val isLowStock = forecast.isLowStock || balance.isLowStock
                val unopenedLots = lots.filter { it.packageStatus == PackageStatus.UNOPENED && it.remainingQuantity.isPositive() }
                val nextLot = unopenedLots.firstOrNull()

                if (isLowStock) {
                    if (!alreadyNotifiedIds.contains(med.id)) {
                        val prescriptions = container.repository.observePrescriptionsForMedication(med.id).first()
                        val todayEpochDay = today.toEpochDay()
                        val activeRx = prescriptions.firstOrNull {
                            it.effectiveStatus(todayEpochDay) == com.example.nuvilo.domain.model.PrescriptionStatus.ACTIVE
                        }
                        val hasPendingOrder = activeRx?.hasPendingRepeatRequest == true
                        val pendingQuantity = if (hasPendingOrder) activeRx?.quantityPrescribed else null

                        container.notificationHelper.showLowStockNotification(
                            medicationId = med.id,
                            medicationName = med.name,
                            currentBalance = forecast.totalUsableUnits,
                            unit = forecast.unit,
                            daysRemaining = forecast.daysOfSupply ?: balance.daysRemaining,
                            hasPendingOrder = hasPendingOrder,
                            pendingOrderQuantity = pendingQuantity,
                            isPrivacyMasked = preferences.isNotificationPrivacyMasked,
                            precision = forecast.precision,
                            unopenedPackagesCount = unopenedLots.size,
                            nextPackageLabel = nextLot?.packageLabel ?: nextLot?.lotNumber
                        )
                        container.userPreferencesRepository.markLowStockNotified(med.id)
                    }
                } else {
                    if (alreadyNotifiedIds.contains(med.id)) {
                        container.notificationHelper.cancelLowStockNotification(med.id)
                        container.userPreferencesRepository.clearLowStockNotified(med.id)
                    }
                }

                // Package-level expiry & post-opening discard safety alerts
                for (lot in lots) {
                    if (lot.packageStatus != PackageStatus.OPENED && lot.packageStatus != PackageStatus.UNOPENED) {
                        continue
                    }
                    if (!lot.remainingQuantity.isPositive()) continue

                    val expiryMs = lot.expiryDateEpochMs
                    if (expiryMs != null) {
                        val expiryDate = Instant.ofEpochMilli(expiryMs).atZone(zoneId).toLocalDate()
                        val daysUntilExpiry = ChronoUnit.DAYS.between(today, expiryDate).toInt()
                        if (daysUntilExpiry <= EXPIRY_WARNING_WINDOW_DAYS) {
                            val tier = if (daysUntilExpiry <= 0) "expired" else "soon"
                            val key = "exp_${lot.id}_$tier"
                            activeExpiryKeys += key
                            if (!preferences.expiringLotNotifiedKeys.contains(key)) {
                                container.notificationHelper.showExpiryWarningNotification(
                                    medication = med,
                                    lot = lot,
                                    daysUntilExpiry = daysUntilExpiry,
                                    isPrivacyMasked = preferences.isNotificationPrivacyMasked
                                )
                                container.userPreferencesRepository.markLotExpiryNotified(key)
                            }
                        }
                    }

                    val discardCutoffMs = lot.effectiveDiscardAfterOpeningEpochMs
                    if (lot.packageStatus == PackageStatus.OPENED && discardCutoffMs != null) {
                        val discardDate = Instant.ofEpochMilli(discardCutoffMs).atZone(zoneId).toLocalDate()
                        val daysUntilDiscard = ChronoUnit.DAYS.between(today, discardDate).toInt()
                        if (daysUntilDiscard <= OPENED_DISCARD_WARNING_WINDOW_DAYS) {
                            val tier = if (daysUntilDiscard <= 0) "due" else "soon"
                            val key = "disc_${lot.id}_$tier"
                            activeDiscardKeys += key
                            if (!preferences.discardLotNotifiedKeys.contains(key)) {
                                container.notificationHelper.showOpenedDiscardWarningNotification(
                                    medication = med,
                                    lot = lot,
                                    daysUntilDiscard = daysUntilDiscard,
                                    isPrivacyMasked = preferences.isNotificationPrivacyMasked
                                )
                                container.userPreferencesRepository.markLotDiscardNotified(key)
                            }
                        }
                    }
                }
            }

            // Clear stale expiry / discard deduplication keys and cancel system notifications once packages are closed/replaced
            for (staleKey in preferences.expiringLotNotifiedKeys - activeExpiryKeys) {
                container.userPreferencesRepository.clearLotExpiryNotified(staleKey)
                val lotId = staleKey.removePrefix("exp_").substringBeforeLast("_")
                container.notificationHelper.cancelExpiryNotification(lotId)
            }
            for (staleKey in preferences.discardLotNotifiedKeys - activeDiscardKeys) {
                container.userPreferencesRepository.clearLotDiscardNotified(staleKey)
                val lotId = staleKey.removePrefix("disc_").substringBeforeLast("_")
                container.notificationHelper.cancelDiscardNotification(lotId)
            }

            return Result.success()
        } catch (e: Exception) {
            return Result.retry()
        }
    }
}
