package com.example.nuvilo.regression

import com.example.nuvilo.data.backup.BackupValidationResult
import com.example.nuvilo.data.backup.BackupValidator
import com.example.nuvilo.data.backup.NoveloDataBackup
import com.example.nuvilo.data.local.entity.MedicationEntity
import com.example.nuvilo.data.local.entity.PrescriptionEntity
import com.example.nuvilo.data.local.entity.PrescriptionEventEntity
import com.example.nuvilo.data.local.entity.ScheduleEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regression Test Suite: Category C (Backup, Restoration & Security)
 * Tests T102 to T106: Foreign Key and Structural Validation of Backups
 */
class BackupValidationRegressionTest {

    private val validMedication = MedicationEntity(
        id = "med_1",
        name = "Amoxicillin",
        strength = "500mg",
        form = "CAPSULE",
        stockUnit = "CAPSULES",
        openingStockRaw = 21000L,
        lowStockThresholdRaw = 6000L,
        recordingMode = "CONFIRM",
        notes = "",
        colorTagHex = "#005EB8",
        isArchived = false,
        createdAtEpochMs = 1700000000000L
    )

    private val validSchedule = ScheduleEntity(
        id = "sch_1",
        medicationId = "med_1",
        scheduleType = "DAILY",
        targetTimesCsv = "480,1200",
        daysOfWeekCsv = "1,2,3,4,5,6,7",
        doseQuantityRaw = 1000L,
        startDateEpochDay = 19500L,
        endDateEpochDay = null,
        instructions = ""
    )

    private val validPrescription = PrescriptionEntity(
        id = "rx_1",
        medicationId = "med_1",
        referenceNumber = "NHS-RX-1",
        issueDateEpochDay = 19500L,
        expiryDateEpochDay = 19800L,
        quantityPrescribedRaw = 21000L,
        unit = "CAPSULES",
        repeatsAuthorised = 2,
        repeatsRemaining = 2,
        hasPendingRepeatRequest = false,
        pharmacyName = null,
        notes = null,
        status = "ACTIVE",
        createdAtEpochMs = 1700000000000L,
        updatedAtEpochMs = 1700000000000L
    )

    private val validPrescriptionEvent = PrescriptionEventEntity(
        id = "rx_event_1",
        prescriptionId = "rx_1",
        medicationId = "med_1",
        eventType = "CREATED",
        timestampEpochMs = 1700000000000L,
        repeatsRemainingAfter = 2,
        actionIdempotencyKey = null,
        notes = null
    )

    @Test
    fun t102_validBackupPassesValidationWithCorrectPreviewCounts() {
        // T102: Valid backup passes validation with correct record count preview
        val backup = NoveloDataBackup(
            medications = listOf(validMedication),
            schedules = listOf(validSchedule),
            prescriptions = listOf(validPrescription),
            prescriptionEvents = listOf(validPrescriptionEvent)
        )

        val result = BackupValidator.validate(backup)
        assertTrue(result is BackupValidationResult.Valid)
        val validResult = result as BackupValidationResult.Valid
        assertEquals(1, validResult.preview.medicationCount)
        assertEquals(1, validResult.preview.scheduleCount)
        assertEquals(1, validResult.preview.prescriptionCount)
        assertEquals(1, validResult.preview.prescriptionEventCount)
    }

    @Test
    fun t103_futureFormatVersionIsRejectedAsInvalid() {
        // T103: Backup with future format version is rejected as invalid
        val backup = NoveloDataBackup(
            formatVersion = 999
        )

        val result = BackupValidator.validate(backup)
        assertTrue(result is BackupValidationResult.Invalid)
        val invalidResult = result as BackupValidationResult.Invalid
        assertTrue(invalidResult.reason.contains("Unsupported backup format version 999"))
    }

    @Test
    fun t104_duplicateMedicationIdsAreRejected() {
        // T104: Backup with duplicate medication IDs is rejected
        val duplicateMed = validMedication.copy(name = "Duplicate Med")
        val backup = NoveloDataBackup(
            medications = listOf(validMedication, duplicateMed)
        )

        val result = BackupValidator.validate(backup)
        assertTrue(result is BackupValidationResult.Invalid)
        val invalidResult = result as BackupValidationResult.Invalid
        assertTrue(invalidResult.reason.contains("Duplicate medication ID detected"))
    }

    @Test
    fun t105_orphanScheduleReferencingNonExistentMedicationIsRejected() {
        // T105: Backup with orphan schedule referencing nonexistent medication is rejected
        val orphanSchedule = validSchedule.copy(medicationId = "non_existent_med")
        val backup = NoveloDataBackup(
            medications = listOf(validMedication),
            schedules = listOf(orphanSchedule)
        )

        val result = BackupValidator.validate(backup)
        assertTrue(result is BackupValidationResult.Invalid)
        val invalidResult = result as BackupValidationResult.Invalid
        assertTrue(invalidResult.reason.contains("references non-existent medication"))
    }

    @Test
    fun t106_orphanPrescriptionEventReferencingNonExistentPrescriptionIsRejected() {
        // T106: Backup with orphan prescription event referencing nonexistent prescription is rejected
        val orphanEvent = validPrescriptionEvent.copy(prescriptionId = "non_existent_rx")
        val backup = NoveloDataBackup(
            medications = listOf(validMedication),
            prescriptions = listOf(validPrescription),
            prescriptionEvents = listOf(orphanEvent)
        )

        val result = BackupValidator.validate(backup)
        assertTrue(result is BackupValidationResult.Invalid)
        val invalidResult = result as BackupValidationResult.Invalid
        assertTrue(invalidResult.reason.contains("references non-existent prescription"))
    }
}
