package com.example.nuvilo.data.backup

object BackupValidator {

    fun validate(backup: NoveloDataBackup): BackupValidationResult {
        // 1. Format and Schema version checks
        if (backup.formatVersion > NoveloDataBackup.CURRENT_FORMAT_VERSION) {
            return BackupValidationResult.Invalid(
                "Unsupported backup format version ${backup.formatVersion} (current supported version is ${NoveloDataBackup.CURRENT_FORMAT_VERSION})"
            )
        }

        if (backup.schemaVersion > NoveloDataBackup.CURRENT_SCHEMA_VERSION) {
            return BackupValidationResult.Invalid(
                "Unsupported database schema version ${backup.schemaVersion} (current supported version is ${NoveloDataBackup.CURRENT_SCHEMA_VERSION})"
            )
        }

        // 2. Primary Key Uniqueness Checks
        val medIds = mutableSetOf<String>()
        for (med in backup.medications) {
            if (med.id.isBlank()) return BackupValidationResult.Invalid("Medication record with blank ID detected")
            if (med.name.isBlank()) return BackupValidationResult.Invalid("Medication record with blank name detected")
            if (!medIds.add(med.id)) {
                return BackupValidationResult.Invalid("Duplicate medication ID detected: ${med.id}")
            }
        }

        val scheduleIds = mutableSetOf<String>()
        for (sched in backup.schedules) {
            if (sched.id.isBlank()) return BackupValidationResult.Invalid("Schedule record with blank ID detected")
            if (!scheduleIds.add(sched.id)) {
                return BackupValidationResult.Invalid("Duplicate schedule ID detected: ${sched.id}")
            }
            if (!medIds.contains(sched.medicationId)) {
                return BackupValidationResult.Invalid("Schedule ${sched.id} references non-existent medication ${sched.medicationId}")
            }
        }

        val doseOccurrenceIds = mutableSetOf<String>()
        for (dose in backup.doseEvents) {
            if (dose.occurrenceId.isBlank()) return BackupValidationResult.Invalid("Dose event with blank occurrence ID detected")
            if (!doseOccurrenceIds.add(dose.occurrenceId)) {
                return BackupValidationResult.Invalid("Duplicate dose occurrence ID detected: ${dose.occurrenceId}")
            }
            if (!medIds.contains(dose.medicationId)) {
                return BackupValidationResult.Invalid("Dose event ${dose.occurrenceId} references non-existent medication ${dose.medicationId}")
            }
        }

        val lotIds = mutableSetOf<String>()
        for (lot in backup.stockLots) {
            if (lot.id.isBlank()) return BackupValidationResult.Invalid("Stock lot with blank ID detected")
            if (!lotIds.add(lot.id)) {
                return BackupValidationResult.Invalid("Duplicate stock lot ID detected: ${lot.id}")
            }
            if (!medIds.contains(lot.medicationId)) {
                return BackupValidationResult.Invalid("Stock lot ${lot.id} references non-existent medication ${lot.medicationId}")
            }
        }

        val ledgerIds = mutableSetOf<String>()
        for (entry in backup.stockLedgerEntries) {
            if (entry.id.isBlank()) return BackupValidationResult.Invalid("Stock ledger entry with blank ID detected")
            if (!ledgerIds.add(entry.id)) {
                return BackupValidationResult.Invalid("Duplicate stock ledger entry ID detected: ${entry.id}")
            }
            if (!medIds.contains(entry.medicationId)) {
                return BackupValidationResult.Invalid("Stock ledger entry ${entry.id} references non-existent medication ${entry.medicationId}")
            }
        }

        val rxIds = mutableSetOf<String>()
        for (rx in backup.prescriptions) {
            if (rx.id.isBlank()) return BackupValidationResult.Invalid("Prescription with blank ID detected")
            if (!rxIds.add(rx.id)) {
                return BackupValidationResult.Invalid("Duplicate prescription ID detected: ${rx.id}")
            }
            if (!medIds.contains(rx.medicationId)) {
                return BackupValidationResult.Invalid("Prescription ${rx.id} references non-existent medication ${rx.medicationId}")
            }
        }

        val rxEventIds = mutableSetOf<String>()
        for (ev in backup.prescriptionEvents) {
            if (ev.id.isBlank()) return BackupValidationResult.Invalid("Prescription event with blank ID detected")
            if (!rxEventIds.add(ev.id)) {
                return BackupValidationResult.Invalid("Duplicate prescription event ID detected: ${ev.id}")
            }
            if (!rxIds.contains(ev.prescriptionId)) {
                return BackupValidationResult.Invalid("Prescription event ${ev.id} references non-existent prescription ${ev.prescriptionId}")
            }
            if (!medIds.contains(ev.medicationId)) {
                return BackupValidationResult.Invalid("Prescription event ${ev.id} references non-existent medication ${ev.medicationId}")
            }
        }

        val preview = BackupPreview(
            exportTimestampEpochMs = backup.exportTimestampEpochMs,
            schemaVersion = backup.schemaVersion,
            appVersion = backup.appVersion,
            medicationCount = backup.medications.size,
            scheduleCount = backup.schedules.size,
            doseEventCount = backup.doseEvents.size,
            stockLotCount = backup.stockLots.size,
            stockLedgerEntryCount = backup.stockLedgerEntries.size,
            prescriptionCount = backup.prescriptions.size,
            prescriptionEventCount = backup.prescriptionEvents.size
        )

        return BackupValidationResult.Valid(backup, preview)
    }
}
