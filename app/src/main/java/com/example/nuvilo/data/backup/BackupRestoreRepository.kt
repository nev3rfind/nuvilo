package com.example.nuvilo.data.backup

import androidx.room.withTransaction
import com.example.nuvilo.data.local.NoveloDatabase

interface BackupRestoreRepository {
    suspend fun createBackup(): NoveloDataBackup
    suspend fun validateBackup(backup: NoveloDataBackup): BackupValidationResult
    suspend fun executeRestore(backup: NoveloDataBackup): Result<Unit>
}

class BackupRestoreRepositoryImpl(
    private val database: NoveloDatabase
) : BackupRestoreRepository {

    override suspend fun createBackup(): NoveloDataBackup {
        return NoveloDataBackup(
            formatVersion = NoveloDataBackup.CURRENT_FORMAT_VERSION,
            appVersion = "1.0",
            schemaVersion = NoveloDataBackup.CURRENT_SCHEMA_VERSION,
            exportTimestampEpochMs = System.currentTimeMillis(),
            medications = database.medicationDao().getAllMedications(),
            schedules = database.scheduleDao().getAllSchedules(),
            doseEvents = database.doseDao().getAllDoses(),
            stockLots = database.stockLotDao().getAllLots(),
            stockLedgerEntries = database.stockLedgerDao().getAllMovements(),
            prescriptions = database.prescriptionDao().getAllPrescriptions(),
            prescriptionEvents = database.prescriptionDao().getAllPrescriptionEvents()
        )
    }

    override suspend fun validateBackup(backup: NoveloDataBackup): BackupValidationResult {
        return BackupValidator.validate(backup)
    }

    override suspend fun executeRestore(backup: NoveloDataBackup): Result<Unit> = runCatching {
        val validation = validateBackup(backup)
        if (validation !is BackupValidationResult.Valid) {
            val reason = (validation as? BackupValidationResult.Invalid)?.reason ?: "Invalid backup"
            throw IllegalArgumentException("Backup validation failed: $reason")
        }

        database.withTransaction {
            // 1. Delete in reverse dependency order
            database.prescriptionDao().deleteAllPrescriptionEvents()
            database.prescriptionDao().deleteAllPrescriptions()
            database.stockLedgerDao().deleteAllMovements()
            database.stockLotDao().deleteAllLots()
            database.doseDao().deleteAllDoses()
            database.scheduleDao().deleteAllSchedules()
            database.medicationDao().deleteAllMedications()

            // 2. Insert in forward dependency order
            if (backup.medications.isNotEmpty()) {
                database.medicationDao().insertMedications(backup.medications)
            }
            if (backup.schedules.isNotEmpty()) {
                database.scheduleDao().insertSchedules(backup.schedules)
            }
            if (backup.doseEvents.isNotEmpty()) {
                database.doseDao().insertDoses(backup.doseEvents)
            }
            if (backup.stockLots.isNotEmpty()) {
                database.stockLotDao().insertLots(backup.stockLots)
            }
            if (backup.stockLedgerEntries.isNotEmpty()) {
                database.stockLedgerDao().insertMovements(backup.stockLedgerEntries)
            }
            if (backup.prescriptions.isNotEmpty()) {
                database.prescriptionDao().insertPrescriptions(backup.prescriptions)
            }
            if (backup.prescriptionEvents.isNotEmpty()) {
                database.prescriptionDao().insertPrescriptionEvents(backup.prescriptionEvents)
            }
        }
    }
}
