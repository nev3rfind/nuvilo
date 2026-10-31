package com.example.nuvilo.system.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.nuvilo.NoveloApplication
import kotlinx.coroutines.flow.first

class LowStockCheckWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val WORK_NAME = "novelo_low_stock_check_periodic"
    }

    override suspend fun doWork(): Result {
        val app = applicationContext as? NoveloApplication ?: return Result.failure()
        val container = app.container

        try {
            val activeMeds = container.repository.observeActiveMedications().first()
            val preferences = container.userPreferencesRepository.userPreferencesFlow.first()
            val alreadyNotifiedIds = preferences.lowStockNotifiedMedIds

            for (med in activeMeds) {
                val balance = container.repository.observeStockBalance(med).first()
                if (balance.isLowStock) {
                    if (!alreadyNotifiedIds.contains(med.id)) {
                        container.notificationHelper.showLowStockNotification(
                            medicationId = med.id,
                            medicationName = med.name,
                            currentBalance = balance.currentBalance,
                            unit = balance.unit,
                            daysRemaining = balance.daysRemaining
                        )
                        container.userPreferencesRepository.markLowStockNotified(med.id)
                    }
                } else {
                    if (alreadyNotifiedIds.contains(med.id)) {
                        // User restocked above threshold; reset alert deduplication state
                        container.userPreferencesRepository.clearLowStockNotified(med.id)
                    }
                }
            }
            return Result.success()
        } catch (e: Exception) {
            return Result.retry()
        }
    }
}
