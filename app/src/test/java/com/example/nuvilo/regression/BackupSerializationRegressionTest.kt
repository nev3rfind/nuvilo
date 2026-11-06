package com.example.nuvilo.regression

import com.example.nuvilo.data.backup.CsvZipBackupManager
import com.example.nuvilo.data.backup.JsonBackupManager
import com.example.nuvilo.data.backup.NoveloDataBackup
import com.example.nuvilo.data.local.entity.DoseEventEntity
import com.example.nuvilo.data.local.entity.MedicationEntity
import com.example.nuvilo.data.local.entity.PrescriptionEntity
import com.example.nuvilo.data.local.entity.PrescriptionEventEntity
import com.example.nuvilo.data.local.entity.ScheduleEntity
import com.example.nuvilo.data.local.entity.StockLedgerEntryEntity
import com.example.nuvilo.data.local.entity.StockLotEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipInputStream

/**
 * Regression Test Suite: Category C (Backup, Restoration & Security)
 * Tests T094 to T101: Backup Serialization Round-Trips (JSON & CSV-ZIP)
 */
class BackupSerializationRegressionTest {

    private val sampleMedication = MedicationEntity(
        id = "med_atorvastatin",
        name = "Atorvastatin",
        strength = "20mg",
        form = "TABLET",
        stockUnit = "TABLETS",
        openingStockRaw = 28000L,
        lowStockThresholdRaw = 7000L,
        recordingMode = "CONFIRM",
        notes = "Take at evening",
        colorTagHex = "#005EB8",
        isArchived = false,
        createdAtEpochMs = 1700000000000L
    )

    private val sampleSchedule = ScheduleEntity(
        id = "sch_1",
        medicationId = "med_atorvastatin",
        scheduleType = "DAILY",
        targetTimesCsv = "1200",
        daysOfWeekCsv = "1,2,3,4,5,6,7",
        doseQuantityRaw = 1000L,
        startDateEpochDay = 19500L,
        endDateEpochDay = null,
        instructions = "Take after meal"
    )

    private val sampleLot = StockLotEntity(
        id = "lot_1",
        medicationId = "med_atorvastatin",
        lotNumber = "LOT-ABC-123",
        initialQuantityRaw = 28000L,
        remainingQuantityRaw = 20000L,
        unit = "TABLETS",
        expiryDateEpochMs = 1750000000000L,
        createdAtEpochMs = 1700000000000L,
        notes = "Supplied by Boots"
    )

    private val sampleLedgerEntry = StockLedgerEntryEntity(
        id = "ledger_1",
        medicationId = "med_atorvastatin",
        timestampEpochMs = 1700000100000L,
        movementType = "DOSE_TAKEN",
        quantityRaw = 1000L,
        unit = "TABLETS",
        reason = "Daily dose",
        doseOccurrenceId = "occ_1",
        restockLotNumber = null,
        lotId = "lot_1",
        isEstimate = false
    )

    private val samplePrescription = PrescriptionEntity(
        id = "rx_1",
        medicationId = "med_atorvastatin",
        referenceNumber = "NHS-RX-777",
        issueDateEpochDay = 19500L,
        expiryDateEpochDay = 19800L,
        quantityPrescribedRaw = 28000L,
        unit = "TABLETS",
        repeatsAuthorised = 3,
        repeatsRemaining = 2,
        hasPendingRepeatRequest = false,
        pharmacyName = "St Mary's Pharmacy",
        notes = "Reviewed by GP",
        status = "ACTIVE",
        createdAtEpochMs = 1700000000000L,
        updatedAtEpochMs = 1700000000000L
    )

    private val samplePrescriptionEvent = PrescriptionEventEntity(
        id = "rx_event_1",
        prescriptionId = "rx_1",
        medicationId = "med_atorvastatin",
        eventType = "REPEAT_COLLECTED",
        timestampEpochMs = 1700000200000L,
        repeatsRemainingAfter = 2,
        actionIdempotencyKey = "key_collect_1",
        notes = "Collected from pharmacy"
    )

    private val sampleDoseEvent = DoseEventEntity(
        occurrenceId = "occ_1",
        medicationId = "med_atorvastatin",
        scheduledEpochMs = 1700000050000L,
        doseQuantityRaw = 1000L,
        unit = "TABLETS",
        status = "TAKEN",
        recordedEpochMs = 1700000055000L,
        notes = "Taken on time",
        correctionNotes = "Corrected timestamp",
        correctedAtEpochMs = 1700000060000L
    )

    @Test
    fun t094_jsonRoundTripPreservesAllMedicationFields() {
        // T094: JSON export and import round-trip preserves all medication fields and types
        val backup = NoveloDataBackup(
            medications = listOf(sampleMedication)
        )

        val jsonString = JsonBackupManager.serialize(backup)
        val deserialized = JsonBackupManager.deserialize(jsonString)

        assertEquals(1, deserialized.medications.size)
        val med = deserialized.medications[0]
        assertEquals(sampleMedication.id, med.id)
        assertEquals(sampleMedication.name, med.name)
        assertEquals(sampleMedication.strength, med.strength)
        assertEquals(sampleMedication.form, med.form)
        assertEquals(sampleMedication.stockUnit, med.stockUnit)
        assertEquals(sampleMedication.openingStockRaw, med.openingStockRaw)
        assertEquals(sampleMedication.lowStockThresholdRaw, med.lowStockThresholdRaw)
        assertEquals(sampleMedication.recordingMode, med.recordingMode)
        assertEquals(sampleMedication.notes, med.notes)
        assertEquals(sampleMedication.colorTagHex, med.colorTagHex)
        assertEquals(sampleMedication.isArchived, med.isArchived)
    }

    @Test
    fun t095_jsonRoundTripPreservesStockLotEntitiesAndQuantities() {
        // T095: JSON export and import round-trip preserves stock lot entities and remaining quantities
        val backup = NoveloDataBackup(
            medications = listOf(sampleMedication),
            stockLots = listOf(sampleLot)
        )

        val jsonString = JsonBackupManager.serialize(backup)
        val deserialized = JsonBackupManager.deserialize(jsonString)

        assertEquals(1, deserialized.stockLots.size)
        val lot = deserialized.stockLots[0]
        assertEquals(sampleLot.id, lot.id)
        assertEquals(sampleLot.lotNumber, lot.lotNumber)
        assertEquals(sampleLot.initialQuantityRaw, lot.initialQuantityRaw)
        assertEquals(sampleLot.remainingQuantityRaw, lot.remainingQuantityRaw)
        assertEquals(sampleLot.expiryDateEpochMs, lot.expiryDateEpochMs)
    }

    @Test
    fun t096_jsonRoundTripPreservesStockLedgerMovementRecords() {
        // T096: JSON export and import round-trip preserves stock ledger movement records
        val backup = NoveloDataBackup(
            medications = listOf(sampleMedication),
            stockLedgerEntries = listOf(sampleLedgerEntry)
        )

        val jsonString = JsonBackupManager.serialize(backup)
        val deserialized = JsonBackupManager.deserialize(jsonString)

        assertEquals(1, deserialized.stockLedgerEntries.size)
        val entry = deserialized.stockLedgerEntries[0]
        assertEquals(sampleLedgerEntry.id, entry.id)
        assertEquals(sampleLedgerEntry.movementType, entry.movementType)
        assertEquals(sampleLedgerEntry.quantityRaw, entry.quantityRaw)
        assertEquals(sampleLedgerEntry.doseOccurrenceId, entry.doseOccurrenceId)
        assertEquals(sampleLedgerEntry.lotId, entry.lotId)
    }

    @Test
    fun t097_jsonRoundTripPreservesPrescriptionsAndAuditEvents() {
        // T097: JSON export and import round-trip preserves prescription and prescription event entities
        val backup = NoveloDataBackup(
            medications = listOf(sampleMedication),
            prescriptions = listOf(samplePrescription),
            prescriptionEvents = listOf(samplePrescriptionEvent)
        )

        val jsonString = JsonBackupManager.serialize(backup)
        val deserialized = JsonBackupManager.deserialize(jsonString)

        assertEquals(1, deserialized.prescriptions.size)
        val rx = deserialized.prescriptions[0]
        assertEquals(samplePrescription.id, rx.id)
        assertEquals(samplePrescription.repeatsRemaining, rx.repeatsRemaining)
        assertEquals(samplePrescription.referenceNumber, rx.referenceNumber)

        assertEquals(1, deserialized.prescriptionEvents.size)
        val event = deserialized.prescriptionEvents[0]
        assertEquals(samplePrescriptionEvent.id, event.id)
        assertEquals(samplePrescriptionEvent.eventType, event.eventType)
        assertEquals(samplePrescriptionEvent.actionIdempotencyKey, event.actionIdempotencyKey)
    }

    @Test
    fun t098_jsonRoundTripPreservesDoseOccurrencesAndCorrectionNotes() {
        // T098: JSON export and import round-trip preserves dose occurrences and correction notes
        val backup = NoveloDataBackup(
            medications = listOf(sampleMedication),
            doseEvents = listOf(sampleDoseEvent)
        )

        val jsonString = JsonBackupManager.serialize(backup)
        val deserialized = JsonBackupManager.deserialize(jsonString)

        assertEquals(1, deserialized.doseEvents.size)
        val dose = deserialized.doseEvents[0]
        assertEquals(sampleDoseEvent.occurrenceId, dose.occurrenceId)
        assertEquals(sampleDoseEvent.status, dose.status)
        assertEquals(sampleDoseEvent.notes, dose.notes)
        assertEquals(sampleDoseEvent.correctionNotes, dose.correctionNotes)
        assertEquals(sampleDoseEvent.correctedAtEpochMs, dose.correctedAtEpochMs)
    }

    @Test
    fun t099_jsonSerializerEncodesCurrentFormatAndSchemaVersions() {
        // T099: JSON serializer encodes current format version and schema version
        val backup = NoveloDataBackup()
        val jsonString = JsonBackupManager.serialize(backup)
        val deserialized = JsonBackupManager.deserialize(jsonString)

        assertEquals(NoveloDataBackup.CURRENT_FORMAT_VERSION, deserialized.formatVersion)
        assertEquals(NoveloDataBackup.CURRENT_SCHEMA_VERSION, deserialized.schemaVersion)
        assertTrue(deserialized.exportTimestampEpochMs > 0)
    }

    @Test
    fun t100_csvZipExportCreatesValidZipStreamWithAllTables() {
        // T100: CSV zip export creates valid zip stream containing all expected CSV table entries
        val fullBackup = NoveloDataBackup(
            medications = listOf(sampleMedication),
            schedules = listOf(sampleSchedule),
            doseEvents = listOf(sampleDoseEvent),
            stockLots = listOf(sampleLot),
            stockLedgerEntries = listOf(sampleLedgerEntry),
            prescriptions = listOf(samplePrescription),
            prescriptionEvents = listOf(samplePrescriptionEvent)
        )

        val outputStream = ByteArrayOutputStream()
        CsvZipBackupManager.exportToZip(fullBackup, outputStream)
        val zipBytes = outputStream.toByteArray()
        assertTrue(zipBytes.isNotEmpty())

        val zipInput = ZipInputStream(ByteArrayInputStream(zipBytes))
        val entryNames = mutableSetOf<String>()
        var entry = zipInput.nextEntry
        while (entry != null) {
            entryNames.add(entry.name)
            zipInput.closeEntry()
            entry = zipInput.nextEntry
        }

        assertTrue(entryNames.contains("manifest.json"))
        assertTrue(entryNames.contains("medications.csv"))
        assertTrue(entryNames.contains("schedules.csv"))
        assertTrue(entryNames.contains("dose_events.csv"))
        assertTrue(entryNames.contains("stock_lots.csv"))
        assertTrue(entryNames.contains("stock_ledger_entries.csv"))
        assertTrue(entryNames.contains("prescriptions.csv"))
        assertTrue(entryNames.contains("prescription_events.csv"))
    }

    @Test
    fun t101_emptyDatabaseExportSerializesWithZeroCountsAndValidHeader() {
        // T101: Empty database export serializes with zero record counts and valid schema header
        val emptyBackup = NoveloDataBackup()
        val jsonString = JsonBackupManager.serialize(emptyBackup)
        val deserialized = JsonBackupManager.deserialize(jsonString)

        assertEquals(0, deserialized.totalRecordsCount)
        assertTrue(deserialized.medications.isEmpty())
        assertTrue(deserialized.schedules.isEmpty())
        assertTrue(deserialized.doseEvents.isEmpty())
        assertTrue(deserialized.stockLots.isEmpty())
        assertTrue(deserialized.stockLedgerEntries.isEmpty())
        assertTrue(deserialized.prescriptions.isEmpty())
        assertTrue(deserialized.prescriptionEvents.isEmpty())
    }
}
