package com.example.nuvilo

import android.app.Application
import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.nuvilo.data.backup.BackupRestoreRepository
import com.example.nuvilo.data.backup.BackupRestoreRepositoryImpl
import com.example.nuvilo.data.local.NoveloDatabase
import com.example.nuvilo.data.preferences.UserPreferencesRepository
import com.example.nuvilo.data.repository.NoveloRepositoryImpl
import com.example.nuvilo.domain.adherence.AdherenceCalculator
import com.example.nuvilo.domain.prescription.PrescriptionManager
import com.example.nuvilo.domain.schedule.ScheduleOccurrenceCalculator
import com.example.nuvilo.domain.stock.StockLedgerCalculator
import com.example.nuvilo.security.AppLockManager
import com.example.nuvilo.security.BiometricAuthManager
import com.example.nuvilo.system.alarm.AlarmScheduler
import com.example.nuvilo.system.alarm.AlarmSchedulerImpl
import com.example.nuvilo.system.notification.NotificationHelper
import com.example.nuvilo.system.work.LowStockCheckWorker
import java.util.concurrent.TimeUnit

interface AppContainer {
    val database: NoveloDatabase
    val repository: NoveloRepositoryImpl
    val userPreferencesRepository: UserPreferencesRepository
    val stockCalculator: StockLedgerCalculator
    val scheduleCalculator: ScheduleOccurrenceCalculator
    val adherenceCalculator: AdherenceCalculator
    val prescriptionManager: PrescriptionManager
    val notificationHelper: NotificationHelper
    val alarmScheduler: AlarmScheduler
    val appLockManager: AppLockManager
    val biometricAuthManager: BiometricAuthManager
    val backupRestoreRepository: BackupRestoreRepository
    fun enqueuePeriodicMaintenance()
}

class DefaultAppContainer(private val context: Context) : AppContainer {
    override val database: NoveloDatabase by lazy {
        NoveloDatabase.create(context)
    }

    override val stockCalculator: StockLedgerCalculator by lazy {
        StockLedgerCalculator()
    }

    override val scheduleCalculator: ScheduleOccurrenceCalculator by lazy {
        ScheduleOccurrenceCalculator()
    }

    override val adherenceCalculator: AdherenceCalculator by lazy {
        AdherenceCalculator(scheduleCalculator)
    }

    override val prescriptionManager: PrescriptionManager by lazy {
        PrescriptionManager()
    }

    override val repository: NoveloRepositoryImpl by lazy {
        NoveloRepositoryImpl(database, stockCalculator, prescriptionManager)
    }

    override val userPreferencesRepository: UserPreferencesRepository by lazy {
        UserPreferencesRepository(context)
    }

    override val notificationHelper: NotificationHelper by lazy {
        NotificationHelper(context)
    }

    override val appLockManager: AppLockManager by lazy {
        AppLockManager()
    }

    override val biometricAuthManager: BiometricAuthManager by lazy {
        BiometricAuthManager(context)
    }

    override val backupRestoreRepository: BackupRestoreRepository by lazy {
        BackupRestoreRepositoryImpl(database)
    }

    override val alarmScheduler: AlarmScheduler by lazy {
        AlarmSchedulerImpl(
            context = context,
            medicationRepository = repository,
            doseDao = database.doseDao(),
            userPreferencesRepository = userPreferencesRepository,
            scheduleCalculator = scheduleCalculator
        )
    }

    override fun enqueuePeriodicMaintenance() {
        val workRequest = PeriodicWorkRequestBuilder<LowStockCheckWorker>(
            repeatInterval = 12,
            repeatIntervalTimeUnit = TimeUnit.HOURS
        ).build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            LowStockCheckWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }
}

class NoveloApplication : Application() {
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
        container.enqueuePeriodicMaintenance()
    }
}
