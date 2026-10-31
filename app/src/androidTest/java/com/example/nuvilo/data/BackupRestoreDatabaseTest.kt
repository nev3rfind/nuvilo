package com.example.nuvilo.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.nuvilo.data.backup.BackupRestoreRepositoryImpl
import com.example.nuvilo.data.backup.NoveloDataBackup
import com.example.nuvilo.data.local.NoveloDatabase
import com.example.nuvilo.data.local.entity.DoseEventEntity
import com.example.nuvilo.data.local.entity.MedicationEntity
import com.example.nuvilo.data.local.entity.PrescriptionEntity
import com.example.nuvilo.data.local.entity.PrescriptionEventEntity
import com.example.nuvilo.data.local.entity.ScheduleEntity
import com.example.nuvilo.data.local.entity.StockLedgerEntryEntity
import com.example.nuvilo.data.local.entity.StockLotEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class BackupRestoreDatabaseTest {

    private lateinit var db: NoveloDatabase
    private lateinit var backupRepo: BackupRestoreRepositoryImpl

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = NoveloDatabase.createInMemory(context)
        backupRepo = BackupRestoreRepositoryImpl(db)
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun createBackup_retrievesAllEntitiesFromDatabase() = runBlocking {
        // Seed database
        val med = MedicationEntity(
            id = "med_1",
            name = "Paracetamol",
            strength = "500 mg",
            form = "TABLET",
            stockUnit = "TABLETS",
            openingStockRaw = 32_000L,
            lowStockThresholdRaw = 8_000L,
            recordingMode = "CONFIRM",
            notes = "Post-surgery pain relief",
            colorTagHex = "#005EB8",
            isArchived = false,
            createdAtEpochMs = 1700000000000L
        )
        db.medicationDao().insertMedications(listOf(med))

        val sched = ScheduleEntity(
            id = "sch_1",
            medicationId = "med_1",
            scheduleType = "DAILY",
            targetTimesCsv = "480,1200",
            daysOfWeekCsv = "",
            doseQuantityRaw = 1_000L,
            startDateEpochDay = 20000L,
            endDateEpochDay = null,
            instructions = "With water"
        )
        db.scheduleDao().insertSchedules(listOf(sched))

        val dose = DoseEventEntity(
            occurrenceId = "med_1:20000:480",
            medicationId = "med_1",
            scheduledEpochMs = 1700000000000L,
            doseQuantityRaw = 1_000L,
            unit = "TABLETS",
            status = "TAKEN",
            recordedEpochMs = 1700000005000L,
            notes = "On time"
        )
        db.doseDao().insertDoses(listOf(dose))

        val lot = StockLotEntity(
            id = "lot_1",
            medicationId = "med_1",
            lotNumber = "LOT-2026-X",
            initialQuantityRaw = 32_000L,
            remainingQuantityRaw = 32_000L,
            unit = "TABLETS",
            expiryDateEpochMs = 1800000000000L,
            createdAtEpochMs = 1700000000000L,
            notes = "Pharmacy refill"
        )
        db.stockLotDao().insertLots(listOf(lot))

        val ledger = StockLedgerEntryEntity(
            id = "mov_1",
            medicationId = "med_1",
            timestampEpochMs = 1700000000000L,
            movementType = "OPENING_BALANCE",
            quantityRaw = 32_000L,
            unit = "TABLETS",
            reason = "Initial inventory",
            doseOccurrenceId = null,
            restockLotNumber = null,
            lotId = "lot_1",
            isEstimate = false
        )
        db.stockLedgerDao().insertMovements(listOf(ledger))

        val rx = PrescriptionEntity(
            id = "rx_1",
            medicationId = "med_1",
            referenceNumber = "RX-9988",
            issueDateEpochDay = 20000L,
            expiryDateEpochDay = 20180L,
            quantityPrescribedRaw = 64_000L,
            unit = "TABLETS",
            repeatsAuthorised = 2,
            repeatsRemaining = 2,
            hasPendingRepeatRequest = false,
            pharmacyName = "St. Thomas Clinic",
            notes = "60 day supply with 2 repeats",
            status = "ACTIVE"
        )
        db.prescriptionDao().insertPrescriptions(listOf(rx))

        val rxEvent = PrescriptionEventEntity(
            id = "rx_evt_1",
            prescriptionId = "rx_1",
            medicationId = "med_1",
            eventType = "CREATED",
            timestampEpochMs = 1700000000000L,
            repeatsRemainingAfter = 2,
            actionIdempotencyKey = null,
            notes = "Issued by doctor"
        )
        db.prescriptionDao().insertPrescriptionEvents(listOf(rxEvent))

        // Capture backup
        val backup = backupRepo.createBackup()

        assertEquals(1, backup.medications.size)
        assertEquals("Paracetamol", backup.medications[0].name)
        assertEquals(1, backup.schedules.size)
        assertEquals(1, backup.doseEvents.size)
        assertEquals(1, backup.stockLots.size)
        assertEquals(1, backup.stockLedgerEntries.size)
        assertEquals(1, backup.prescriptions.size)
        assertEquals(1, backup.prescriptionEvents.size)
        assertEquals(7, backup.totalRecordsCount)
    }

    @Test
    fun executeRestore_clearsAndRestoresEntitiesTransactionally() = runBlocking {
        // 1. Initial State: Seed med_old
        val oldMed = MedicationEntity(
            id = "med_old",
            name = "Old Aspirin",
            strength = "100 mg",
            form = "TABLET",
            stockUnit = "TABLETS",
            openingStockRaw = 10_000L,
            lowStockThresholdRaw = 2_000L,
            recordingMode = "CONFIRM",
            notes = "",
            colorTagHex = "#005EB8",
            isArchived = false,
            createdAtEpochMs = 1690000000000L
        )
        db.medicationDao().insertMedications(listOf(oldMed))
        assertEquals(1, db.medicationDao().getAllMedications().size)

        // 2. Prepare target backup with 2 new medications and associated records
        val medA = MedicationEntity(
            id = "med_a",
            name = "Metformin",
            strength = "500 mg",
            form = "TABLET",
            stockUnit = "TABLETS",
            openingStockRaw = 60_000L,
            lowStockThresholdRaw = 14_000L,
            recordingMode = "CONFIRM",
            notes = "Twice daily",
            colorTagHex = "#005EB8",
            isArchived = false,
            createdAtEpochMs = 1700000000000L
        )
        val medB = MedicationEntity(
            id = "med_b",
            name = "Atorvastatin",
            strength = "20 mg",
            form = "TABLET",
            stockUnit = "TABLETS",
            openingStockRaw = 28_000L,
            lowStockThresholdRaw = 7_000L,
            recordingMode = "AUTO_ASSUME",
            notes = "At bedtime",
            colorTagHex = "#005EB8",
            isArchived = false,
            createdAtEpochMs = 1700000000000L
        )
        val schedA = ScheduleEntity(
            id = "sch_a",
            medicationId = "med_a",
            scheduleType = "DAILY",
            targetTimesCsv = "480,1200",
            daysOfWeekCsv = "",
            doseQuantityRaw = 1_000L,
            startDateEpochDay = 20000L,
            endDateEpochDay = null,
            instructions = "With meals"
        )
        val backup = NoveloDataBackup(
            medications = listOf(medA, medB),
            schedules = listOf(schedA),
            doseEvents = emptyList(),
            stockLots = emptyList(),
            stockLedgerEntries = emptyList(),
            prescriptions = emptyList(),
            prescriptionEvents = emptyList()
        )

        // 3. Restore
        val result = backupRepo.executeRestore(backup)
        assertTrue(result.isSuccess)

        // 4. Verify old data replaced and new data restored
        val restoredMeds = db.medicationDao().getAllMedications()
        assertEquals(2, restoredMeds.size)
        assertTrue(restoredMeds.any { it.id == "med_a" && it.name == "Metformin" })
        assertTrue(restoredMeds.any { it.id == "med_b" && it.name == "Atorvastatin" })
        assertTrue(restoredMeds.none { it.id == "med_old" })

        val restoredScheds = db.scheduleDao().getAllSchedules()
        assertEquals(1, restoredScheds.size)
        assertEquals("sch_a", restoredScheds[0].id)
    }

    @Test
    fun executeRestore_withInvalidReferentialIntegrity_failsAndRollsBack() = runBlocking {
        // Seed valid initial medication
        val initialMed = MedicationEntity(
            id = "med_keeper",
            name = "Keeper Med",
            strength = "10 mg",
            form = "TABLET",
            stockUnit = "TABLETS",
            openingStockRaw = 50_000L,
            lowStockThresholdRaw = 10_000L,
            recordingMode = "CONFIRM",
            notes = "",
            colorTagHex = "#005EB8",
            isArchived = false,
            createdAtEpochMs = 1700000000000L
        )
        db.medicationDao().insertMedications(listOf(initialMed))

        // Backup has an orphaned schedule referencing "med_non_existent"
        val invalidSched = ScheduleEntity(
            id = "sch_orphan",
            medicationId = "med_non_existent",
            scheduleType = "DAILY",
            targetTimesCsv = "480",
            daysOfWeekCsv = "",
            doseQuantityRaw = 1_000L,
            startDateEpochDay = 20000L,
            endDateEpochDay = null,
            instructions = ""
        )
        val invalidBackup = NoveloDataBackup(
            medications = emptyList(),
            schedules = listOf(invalidSched)
        )

        val result = backupRepo.executeRestore(invalidBackup)
        assertTrue(result.isFailure)

        // Assert initial data was NOT wiped or corrupted
        val medsAfterFailure = db.medicationDao().getAllMedications()
        assertEquals(1, medsAfterFailure.size)
        assertEquals("med_keeper", medsAfterFailure[0].id)
    }
}
