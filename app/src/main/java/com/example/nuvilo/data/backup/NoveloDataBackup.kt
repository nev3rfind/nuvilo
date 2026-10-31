package com.example.nuvilo.data.backup

import com.example.nuvilo.data.local.entity.DoseEventEntity
import com.example.nuvilo.data.local.entity.MedicationEntity
import com.example.nuvilo.data.local.entity.PrescriptionEntity
import com.example.nuvilo.data.local.entity.PrescriptionEventEntity
import com.example.nuvilo.data.local.entity.ScheduleEntity
import com.example.nuvilo.data.local.entity.StockLedgerEntryEntity
import com.example.nuvilo.data.local.entity.StockLotEntity

data class NoveloDataBackup(
    val formatVersion: Int = CURRENT_FORMAT_VERSION,
    val appVersion: String = "1.0",
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val exportTimestampEpochMs: Long = System.currentTimeMillis(),
    val medications: List<MedicationEntity> = emptyList(),
    val schedules: List<ScheduleEntity> = emptyList(),
    val doseEvents: List<DoseEventEntity> = emptyList(),
    val stockLots: List<StockLotEntity> = emptyList(),
    val stockLedgerEntries: List<StockLedgerEntryEntity> = emptyList(),
    val prescriptions: List<PrescriptionEntity> = emptyList(),
    val prescriptionEvents: List<PrescriptionEventEntity> = emptyList()
) {
    companion object {
        const val CURRENT_FORMAT_VERSION = 1
        const val CURRENT_SCHEMA_VERSION = 3
    }

    val totalRecordsCount: Int
        get() = medications.size +
                schedules.size +
                doseEvents.size +
                stockLots.size +
                stockLedgerEntries.size +
                prescriptions.size +
                prescriptionEvents.size
}

data class BackupPreview(
    val exportTimestampEpochMs: Long,
    val schemaVersion: Int,
    val appVersion: String,
    val medicationCount: Int,
    val scheduleCount: Int,
    val doseEventCount: Int,
    val stockLotCount: Int,
    val stockLedgerEntryCount: Int,
    val prescriptionCount: Int,
    val prescriptionEventCount: Int
)

sealed interface BackupValidationResult {
    data class Valid(val backup: NoveloDataBackup, val preview: BackupPreview) : BackupValidationResult
    data class Invalid(val reason: String) : BackupValidationResult
    data class Encrypted(val formatVersion: Int, val envelopeJson: String) : BackupValidationResult
}
