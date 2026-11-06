package com.example.nuvilo.regression

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.nuvilo.data.local.NoveloDatabase
import com.example.nuvilo.data.local.entity.DoseEventEntity
import com.example.nuvilo.data.local.entity.MedicationEntity
import com.example.nuvilo.data.local.entity.PrescriptionEntity
import com.example.nuvilo.data.local.entity.PrescriptionEventEntity
import com.example.nuvilo.data.local.entity.ScheduleEntity
import com.example.nuvilo.data.local.entity.StockLedgerEntryEntity
import com.example.nuvilo.data.local.entity.StockLotEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

/**
 * Regression Test Suite: Category B (Room, Repositories & Migrations)
 * Tests T056 to T065: Direct DAO Operations and Queries
 */
@RunWith(AndroidJUnit4::class)
class RoomDaoRegressionTest {

    private lateinit var db: NoveloDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = NoveloDatabase.createInMemory(context)
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    private fun createSampleMedication(id: String = "med_1", isArchived: Boolean = false) = MedicationEntity(
        id = id,
        name = "Omeprazole",
        strength = "20mg",
        form = "CAPSULE",
        stockUnit = "CAPSULES",
        openingStockRaw = 28000L,
        lowStockThresholdRaw = 7000L,
        recordingMode = "CONFIRM",
        notes = "Take before breakfast",
        colorTagHex = "#005EB8",
        isArchived = isArchived,
        createdAtEpochMs = 1700000000000L
    )

    @Test
    fun t056_insertMedicationAndRetrieveById() = runBlocking {
        // T056: Insert medication and retrieve by ID
        val med = createSampleMedication("med_t056")
        db.medicationDao().insertMedication(med)

        val retrieved = db.medicationDao().getMedicationById("med_t056")
        assertNotNull(retrieved)
        assertEquals("Omeprazole", retrieved?.name)
        assertEquals("20mg", retrieved?.strength)
        assertEquals(28000L, retrieved?.openingStockRaw)
    }

    @Test
    fun t057_observeActiveMedicationsFiltersArchivedItems() = runBlocking {
        // T057: Observe active medications filters archived items
        val activeMed = createSampleMedication("med_active", isArchived = false)
        val archivedMed = createSampleMedication("med_archived", isArchived = true)

        db.medicationDao().insertMedication(activeMed)
        db.medicationDao().insertMedication(archivedMed)

        val activeList = db.medicationDao().observeActiveMedications().first()
        assertEquals(1, activeList.size)
        assertEquals("med_active", activeList[0].id)

        val allList = db.medicationDao().observeAllMedications().first()
        assertEquals(2, allList.size)
    }

    @Test
    fun t058_insertScheduleAndRetrieveByMedicationId() = runBlocking {
        // T058: Insert schedule and retrieve by medication ID
        val med = createSampleMedication("med_sch")
        db.medicationDao().insertMedication(med)

        val schedule = ScheduleEntity(
            id = "sch_t058",
            medicationId = "med_sch",
            scheduleType = "DAILY",
            targetTimesCsv = "480,1200",
            daysOfWeekCsv = "1,2,3,4,5,6,7",
            doseQuantityRaw = 1000L,
            startDateEpochDay = 19500L,
            endDateEpochDay = null,
            instructions = "With glass of water"
        )
        db.scheduleDao().insertSchedule(schedule)

        val retrieved = db.scheduleDao().getSchedulesForMedication("med_sch")
        assertEquals(1, retrieved.size)
        assertEquals("sch_t058", retrieved[0].id)
        assertEquals("480,1200", retrieved[0].targetTimesCsv)
    }

    @Test
    fun t059_insertDoseEventAndObserveInRange() = runBlocking {
        // T059: Insert dose event and observe in range
        val med = createSampleMedication("med_dose")
        db.medicationDao().insertMedication(med)

        val dose = DoseEventEntity(
            occurrenceId = "occ_t059",
            medicationId = "med_dose",
            scheduledEpochMs = 1700000500000L,
            doseQuantityRaw = 1000L,
            unit = "CAPSULES",
            status = "TAKEN",
            recordedEpochMs = 1700000510000L,
            notes = "Morning dose"
        )
        db.doseDao().insertDoseEvent(dose)

        val doses = db.doseDao().observeDosesInRange(1700000000000L, 1700001000000L).first()
        assertEquals(1, doses.size)
        assertEquals("occ_t059", doses[0].occurrenceId)
        assertEquals("TAKEN", doses[0].status)
    }

    @Test
    fun t060_insertStockLotWithInitialAndRemainingQuantities() = runBlocking {
        // T060: Insert stock lot with initial and remaining quantities
        val med = createSampleMedication("med_lot")
        db.medicationDao().insertMedication(med)

        val lot = StockLotEntity(
            id = "lot_t060",
            medicationId = "med_lot",
            lotNumber = "LOT-2026-X",
            initialQuantityRaw = 30000L,
            remainingQuantityRaw = 25000L,
            unit = "CAPSULES",
            expiryDateEpochMs = 1750000000000L,
            createdAtEpochMs = 1700000000000L
        )
        db.stockLotDao().insertLot(lot)

        val lots = db.stockLotDao().observeLotsForMedication("med_lot").first()
        assertEquals(1, lots.size)
        assertEquals("lot_t060", lots[0].id)
        assertEquals(25000L, lots[0].remainingQuantityRaw)
    }

    @Test
    fun t061_insertStockLedgerEntryAndVerifyQueryByMedicationId() = runBlocking {
        // T061: Insert stock ledger entry and verify query by medication ID
        val med = createSampleMedication("med_ledger")
        db.medicationDao().insertMedication(med)

        val entry = StockLedgerEntryEntity(
            id = "entry_t061",
            medicationId = "med_ledger",
            timestampEpochMs = 1700000000000L,
            movementType = "OPENING_BALANCE",
            quantityRaw = 28000L,
            unit = "CAPSULES",
            reason = "Initial inventory"
        )
        db.stockLedgerDao().insertMovement(entry)

        val entries = db.stockLedgerDao().observeMovementsForMedication("med_ledger").first()
        assertEquals(1, entries.size)
        assertEquals("entry_t061", entries[0].id)
        assertEquals("OPENING_BALANCE", entries[0].movementType)
    }

    @Test
    fun t062_insertPrescriptionAndRetrieveByMedicationId() = runBlocking {
        // T062: Insert prescription and retrieve by medication ID
        val med = createSampleMedication("med_rx")
        db.medicationDao().insertMedication(med)

        val rx = PrescriptionEntity(
            id = "rx_t062",
            medicationId = "med_rx",
            referenceNumber = "NHS-RX-99",
            issueDateEpochDay = 19500L,
            expiryDateEpochDay = 19800L,
            quantityPrescribedRaw = 28000L,
            unit = "CAPSULES",
            repeatsAuthorised = 3,
            repeatsRemaining = 3,
            hasPendingRepeatRequest = false,
            status = "ACTIVE",
            createdAtEpochMs = 1700000000000L,
            updatedAtEpochMs = 1700000000000L
        )
        db.prescriptionDao().insertPrescription(rx)

        val prescriptions = db.prescriptionDao().observePrescriptionsForMedication("med_rx").first()
        assertEquals(1, prescriptions.size)
        assertEquals("rx_t062", prescriptions[0].id)
        assertEquals(3, prescriptions[0].repeatsRemaining)
    }

    @Test
    fun t063_insertPrescriptionEventAndRetrieveByPrescriptionId() = runBlocking {
        // T063: Insert prescription event and retrieve by prescription ID
        val med = createSampleMedication("med_rx_event")
        db.medicationDao().insertMedication(med)

        val rx = PrescriptionEntity(
            id = "rx_parent",
            medicationId = "med_rx_event",
            referenceNumber = "NHS-RX-100",
            issueDateEpochDay = 19500L,
            expiryDateEpochDay = 19800L,
            quantityPrescribedRaw = 28000L,
            unit = "CAPSULES",
            repeatsAuthorised = 2,
            repeatsRemaining = 2,
            hasPendingRepeatRequest = false,
            status = "ACTIVE",
            createdAtEpochMs = 1700000000000L,
            updatedAtEpochMs = 1700000000000L
        )
        db.prescriptionDao().insertPrescription(rx)

        val event = PrescriptionEventEntity(
            id = "event_t063",
            prescriptionId = "rx_parent",
            medicationId = "med_rx_event",
            eventType = "REPEAT_REQUESTED",
            timestampEpochMs = 1700000100000L,
            repeatsRemainingAfter = 2,
            actionIdempotencyKey = "key_1"
        )
        db.prescriptionDao().insertPrescriptionEvent(event)

        val events = db.prescriptionDao().observeEventsForPrescription("rx_parent").first()
        assertEquals(1, events.size)
        assertEquals("event_t063", events[0].id)
        assertEquals("REPEAT_REQUESTED", events[0].eventType)
    }

    @Test
    fun t064_updateMedicationChangesNameAndLowStockThreshold() = runBlocking {
        // T064: Update medication changes name and low stock threshold
        val original = createSampleMedication("med_t064")
        db.medicationDao().insertMedication(original)

        val updated = original.copy(
            name = "Omeprazole Gastro-resistant",
            lowStockThresholdRaw = 10000L
        )
        db.medicationDao().updateMedication(updated)

        val retrieved = db.medicationDao().getMedicationById("med_t064")
        assertEquals("Omeprazole Gastro-resistant", retrieved?.name)
        assertEquals(10000L, retrieved?.lowStockThresholdRaw)
    }

    @Test
    fun t065_softDeleteArchivingOfMedicationPreservesHistory() = runBlocking {
        // T065: Soft-delete / archiving of medication preserves history
        val med = createSampleMedication("med_t065", isArchived = false)
        db.medicationDao().insertMedication(med)

        db.medicationDao().setArchived("med_t065", true)

        val activeList = db.medicationDao().observeActiveMedications().first()
        assertTrue(activeList.none { it.id == "med_t065" })

        val allList = db.medicationDao().observeAllMedications().first()
        assertTrue(allList.any { it.id == "med_t065" && it.isArchived })
    }
}
