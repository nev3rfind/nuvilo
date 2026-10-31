package com.example.nuvilo.data.backup

import com.example.nuvilo.data.local.entity.DoseEventEntity
import com.example.nuvilo.data.local.entity.MedicationEntity
import com.example.nuvilo.data.local.entity.PrescriptionEntity
import com.example.nuvilo.data.local.entity.PrescriptionEventEntity
import com.example.nuvilo.data.local.entity.ScheduleEntity
import com.example.nuvilo.data.local.entity.StockLedgerEntryEntity
import com.example.nuvilo.data.local.entity.StockLotEntity
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BackupSerializationTest {

    private lateinit var sampleBackup: NoveloDataBackup

    @Before
    fun setUp() {
        val med = MedicationEntity(
            id = "med-amox-1",
            name = "Amoxicilline 500mg · 抗生物質",
            strength = "500mg",
            form = "TABLET",
            stockUnit = "TABLETS",
            openingStockRaw = 30_000L,
            lowStockThresholdRaw = 10_000L,
            recordingMode = "CONFIRM",
            notes = "Take after breakfast, with water.\nNever with alcohol.",
            colorTagHex = "#005EB8",
            isArchived = false,
            createdAtEpochMs = 1700000000000L
        )

        val sched = ScheduleEntity(
            id = "sched-amox-1",
            medicationId = "med-amox-1",
            scheduleType = "DAILY",
            targetTimesCsv = "08:00,20:00",
            daysOfWeekCsv = "",
            doseQuantityRaw = 1_000L,
            startDateEpochDay = 19500L,
            endDateEpochDay = null,
            instructions = "Take with food"
        )

        val dose = DoseEventEntity(
            occurrenceId = "med-amox-1_1700035200000",
            medicationId = "med-amox-1",
            scheduledEpochMs = 1700035200000L,
            doseQuantityRaw = 1_000L,
            unit = "TABLETS",
            status = "TAKEN",
            recordedEpochMs = 1700035300000L,
            notes = "Taken with orange juice, feeling fine.",
            correctionNotes = null,
            correctedAtEpochMs = null
        )

        val lot = StockLotEntity(
            id = "lot-amox-1",
            medicationId = "med-amox-1",
            lotNumber = "LOT-ABC-999",
            initialQuantityRaw = 30_000L,
            remainingQuantityRaw = 29_000L,
            unit = "TABLETS",
            expiryDateEpochMs = 1750000000000L,
            createdAtEpochMs = 1700000000000L,
            notes = "From Boots Pharmacy"
        )

        val entry = StockLedgerEntryEntity(
            id = "mvt-amox-1",
            medicationId = "med-amox-1",
            timestampEpochMs = 1700000000000L,
            movementType = "OPENING_BALANCE",
            quantityRaw = 30_000L,
            unit = "TABLETS",
            reason = "Initial prescription fill",
            doseOccurrenceId = null,
            restockLotNumber = null,
            lotId = "lot-amox-1",
            isEstimate = false
        )

        val rx = PrescriptionEntity(
            id = "rx-amox-1",
            medicationId = "med-amox-1",
            referenceNumber = "RX-2026-999",
            issueDateEpochDay = 19500L,
            expiryDateEpochDay = 19865L,
            quantityPrescribedRaw = 30_000L,
            unit = "TABLETS",
            repeatsAuthorised = 2,
            repeatsRemaining = 2,
            hasPendingRepeatRequest = false,
            pharmacyName = "NHS Community Pharmacy",
            notes = "Dr. Jane Smith",
            status = "ACTIVE",
            createdAtEpochMs = 1700000000000L,
            updatedAtEpochMs = 1700000000000L
        )

        val rxEvent = PrescriptionEventEntity(
            id = "rx-ev-1",
            prescriptionId = "rx-amox-1",
            medicationId = "med-amox-1",
            eventType = "INITIAL_AUTHORISED",
            timestampEpochMs = 1700000000000L,
            repeatsRemainingAfter = 2,
            actionIdempotencyKey = "init-rx-amox-1",
            notes = "Authorised with 2 repeats"
        )

        sampleBackup = NoveloDataBackup(
            formatVersion = 1,
            appVersion = "1.0",
            schemaVersion = 3,
            exportTimestampEpochMs = 1700000000000L,
            medications = listOf(med),
            schedules = listOf(sched),
            doseEvents = listOf(dose),
            stockLots = listOf(lot),
            stockLedgerEntries = listOf(entry),
            prescriptions = listOf(rx),
            prescriptionEvents = listOf(rxEvent)
        )
    }

    @Test
    fun jsonBackup_roundTripPreservesAllEntitiesAndFields() {
        val json = JsonBackupManager.serialize(sampleBackup)
        val restored = JsonBackupManager.deserialize(json)

        assertEquals(sampleBackup.formatVersion, restored.formatVersion)
        assertEquals(sampleBackup.schemaVersion, restored.schemaVersion)
        assertEquals(sampleBackup.medications.size, restored.medications.size)
        assertEquals(sampleBackup.medications[0].name, restored.medications[0].name)
        assertEquals(sampleBackup.medications[0].notes, restored.medications[0].notes)
        assertEquals(sampleBackup.schedules.size, restored.schedules.size)
        assertEquals(sampleBackup.doseEvents.size, restored.doseEvents.size)
        assertEquals(sampleBackup.stockLots.size, restored.stockLots.size)
        assertEquals(sampleBackup.stockLots[0].remainingQuantityRaw, restored.stockLots[0].remainingQuantityRaw)
        assertEquals(sampleBackup.stockLedgerEntries.size, restored.stockLedgerEntries.size)
        assertEquals(sampleBackup.prescriptions.size, restored.prescriptions.size)
        assertEquals(sampleBackup.prescriptionEvents.size, restored.prescriptionEvents.size)

        val validation = BackupValidator.validate(restored)
        assertTrue(validation is BackupValidationResult.Valid)
    }

    @Test
    fun csvZipBackup_roundTripPreservesAllEntitiesAndFields() {
        val baos = ByteArrayOutputStream()
        CsvZipBackupManager.exportToZip(sampleBackup, baos)
        val zipBytes = baos.toByteArray()
        assertTrue(zipBytes.isNotEmpty())

        val bais = ByteArrayInputStream(zipBytes)
        val restored = CsvZipBackupManager.importFromZip(bais)

        assertEquals(sampleBackup.medications.size, restored.medications.size)
        assertEquals(sampleBackup.medications[0].id, restored.medications[0].id)
        assertEquals(sampleBackup.medications[0].name, restored.medications[0].name)
        assertEquals(sampleBackup.schedules.size, restored.schedules.size)
        assertEquals(sampleBackup.doseEvents.size, restored.doseEvents.size)
        assertEquals(sampleBackup.stockLots.size, restored.stockLots.size)
        assertEquals(sampleBackup.stockLedgerEntries.size, restored.stockLedgerEntries.size)
        assertEquals(sampleBackup.prescriptions.size, restored.prescriptions.size)
        assertEquals(sampleBackup.prescriptionEvents.size, restored.prescriptionEvents.size)

        val validation = BackupValidator.validate(restored)
        assertTrue(validation is BackupValidationResult.Valid)
    }

    @Test
    fun validatorRejectsOrphanSchedule() {
        val orphanSched = sampleBackup.schedules[0].copy(medicationId = "non-existent-med-id")
        val brokenBackup = sampleBackup.copy(schedules = listOf(orphanSched))

        val result = BackupValidator.validate(brokenBackup)
        assertTrue(result is BackupValidationResult.Invalid)
        assertTrue((result as BackupValidationResult.Invalid).reason.contains("references non-existent medication"))
    }

    @Test
    fun validatorRejectsDuplicateMedicationId() {
        val duplicateMed = sampleBackup.medications[0].copy()
        val brokenBackup = sampleBackup.copy(medications = listOf(sampleBackup.medications[0], duplicateMed))

        val result = BackupValidator.validate(brokenBackup)
        assertTrue(result is BackupValidationResult.Invalid)
        assertTrue((result as BackupValidationResult.Invalid).reason.contains("Duplicate medication ID"))
    }
}
