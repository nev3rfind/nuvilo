package com.example.nuvilo.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.nuvilo.data.local.NoveloDatabase
import com.example.nuvilo.data.repository.NoveloRepositoryImpl
import com.example.nuvilo.domain.model.DoseStatus
import com.example.nuvilo.domain.model.Medication
import com.example.nuvilo.domain.model.MedicationForm
import com.example.nuvilo.domain.model.MovementType
import com.example.nuvilo.domain.model.Prescription
import com.example.nuvilo.domain.model.PrescriptionEventType
import com.example.nuvilo.domain.model.PrescriptionStatus
import com.example.nuvilo.domain.model.ScaledQuantity
import com.example.nuvilo.domain.model.ScheduleDefinition
import com.example.nuvilo.domain.model.ScheduleType
import com.example.nuvilo.domain.model.StockUnit
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class NoveloDatabaseTest {

    private lateinit var db: NoveloDatabase
    private lateinit var repository: NoveloRepositoryImpl

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = NoveloDatabase.createInMemory(context)
        repository = NoveloRepositoryImpl(db)
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun createMedication_createsMedicationAndInitialStockLedgerMovement() = runBlocking {
        val med = Medication(
            id = "med_amox",
            name = "Amoxicillin",
            strength = "500 mg",
            form = MedicationForm.CAPSULE,
            stockUnit = StockUnit.CAPSULES,
            openingStock = ScaledQuantity.ofWhole(21),
            lowStockThreshold = ScaledQuantity.ofWhole(6)
        )
        val schedule = ScheduleDefinition(
            id = "sch_amox",
            medicationId = "med_amox",
            scheduleType = ScheduleType.DAILY,
            targetTimesMinuteOfDay = listOf(8 * 60, 14 * 60, 20 * 60),
            daysOfWeek = emptySet(),
            doseQuantityPerOccurrence = ScaledQuantity.ofWhole(1),
            startDateEpochDay = 20000L
        )

        val result = repository.createMedication(med, schedule)
        assertTrue(result.isSuccess)

        val fetched = repository.getMedication("med_amox")
        assertNotNull(fetched)
        assertEquals("Amoxicillin", fetched?.name)

        // Verify initial stock balance is 21
        val balance = repository.observeStockBalance(med).first()
        assertEquals(21_000L, balance.currentBalance.rawScaledValue)
    }

    @Test
    fun recordDose_isStrictlyIdempotent() = runBlocking {
        val med = Medication(
            id = "med_test",
            name = "Test Pill",
            strength = "10 mg",
            form = MedicationForm.TABLET,
            stockUnit = StockUnit.TABLETS,
            openingStock = ScaledQuantity.ofWhole(10),
            lowStockThreshold = ScaledQuantity.ofWhole(2)
        )
        val schedule = ScheduleDefinition(
            id = "sch_test",
            medicationId = "med_test",
            scheduleType = ScheduleType.DAILY,
            targetTimesMinuteOfDay = listOf(8 * 60),
            daysOfWeek = emptySet(),
            doseQuantityPerOccurrence = ScaledQuantity.ofWhole(1),
            startDateEpochDay = 20000L
        )
        repository.createMedication(med, schedule)

        val occurrenceId = "occ_med_test_1000"

        // First click: Take Dose
        repository.recordDose(
            occurrenceId = occurrenceId,
            medicationId = "med_test",
            scheduledEpochMs = 1000L,
            quantity = ScaledQuantity.ofWhole(1),
            unit = StockUnit.TABLETS,
            status = DoseStatus.TAKEN
        )

        // Second click: Rapid duplicate tap on "Take Dose"
        repository.recordDose(
            occurrenceId = occurrenceId,
            medicationId = "med_test",
            scheduledEpochMs = 1000L,
            quantity = ScaledQuantity.ofWhole(1),
            unit = StockUnit.TABLETS,
            status = DoseStatus.TAKEN
        )

        // Third click: Redundant broadcast
        repository.recordDose(
            occurrenceId = occurrenceId,
            medicationId = "med_test",
            scheduledEpochMs = 1000L,
            quantity = ScaledQuantity.ofWhole(1),
            unit = StockUnit.TABLETS,
            status = DoseStatus.TAKEN
        )

        // Balance should be exactly 9 (10 - 1), NOT 7!
        val balance = repository.observeStockBalance(med).first()
        assertEquals(9_000L, balance.currentBalance.rawScaledValue)
        assertEquals("9", balance.currentBalance.toDisplayString())

        // And ledger movements for this occurrence must be exactly 1
        val deductionCount = db.stockLedgerDao().countDeductionsForOccurrence(occurrenceId)
        assertEquals(1, deductionCount)
    }

    @Test
    fun skippedDose_doesNotDeductStock() = runBlocking {
        val med = Medication(
            id = "med_skip",
            name = "Skip Pill",
            strength = "20 mg",
            form = MedicationForm.TABLET,
            stockUnit = StockUnit.TABLETS,
            openingStock = ScaledQuantity.ofWhole(15),
            lowStockThreshold = ScaledQuantity.ofWhole(3)
        )
        val schedule = ScheduleDefinition(
            id = "sch_skip",
            medicationId = "med_skip",
            scheduleType = ScheduleType.DAILY,
            targetTimesMinuteOfDay = listOf(8 * 60),
            daysOfWeek = emptySet(),
            doseQuantityPerOccurrence = ScaledQuantity.ofWhole(1),
            startDateEpochDay = 20000L
        )
        repository.createMedication(med, schedule)

        // User marks dose as SKIPPED
        repository.recordDose(
            occurrenceId = "occ_skip_1",
            medicationId = "med_skip",
            scheduledEpochMs = 1000L,
            quantity = ScaledQuantity.ofWhole(1),
            unit = StockUnit.TABLETS,
            status = DoseStatus.SKIPPED
        )

        // Stock must remain untouched at 15
        val balance = repository.observeStockBalance(med).first()
        assertEquals(15_000L, balance.currentBalance.rawScaledValue)

        // Dose event must be recorded as SKIPPED
        val dose = db.doseDao().getDoseByOccurrenceId("occ_skip_1")
        assertNotNull(dose)
        assertEquals(DoseStatus.SKIPPED.name, dose?.status)
    }

    @Test
    fun restockWithLot_createsLotAndUpdatesBalance() = runBlocking {
        val med = Medication(
            id = "med_restock",
            name = "Metformin",
            strength = "500 mg",
            form = MedicationForm.TABLET,
            stockUnit = StockUnit.TABLETS,
            openingStock = ScaledQuantity.ofWhole(10),
            lowStockThreshold = ScaledQuantity.ofWhole(5)
        )
        val schedule = ScheduleDefinition(
            id = "sch_restock",
            medicationId = "med_restock",
            scheduleType = ScheduleType.DAILY,
            targetTimesMinuteOfDay = listOf(8 * 60),
            daysOfWeek = emptySet(),
            doseQuantityPerOccurrence = ScaledQuantity.ofWhole(1),
            startDateEpochDay = 20000L
        )
        repository.createMedication(med, schedule)

        val restockResult = repository.restockWithLot(
            medicationId = "med_restock",
            quantity = ScaledQuantity.ofWhole(30),
            unit = StockUnit.TABLETS,
            lotNumber = "LOT-MET-2026",
            expiryDateEpochMs = 1800000000000L,
            notes = "Monthly pharmacy refill"
        )
        assertTrue(restockResult.isSuccess)

        // Verify lot created in database
        val lots = repository.observeLotsForMedication("med_restock").first()
        assertEquals(1, lots.size)
        val lot = lots[0]
        assertEquals("LOT-MET-2026", lot.lotNumber)
        assertEquals(30_000L, lot.initialQuantity.rawScaledValue)
        assertEquals(30_000L, lot.remainingQuantity.rawScaledValue)
        assertEquals(1800000000000L, lot.expiryDateEpochMs)

        // Verify ledger entry has lot link
        val movements = repository.observeMovementsForMedication("med_restock").first()
        val restockEntry = movements.find { it.movementType == MovementType.RESTOCK }
        assertNotNull(restockEntry)
        assertEquals("LOT-MET-2026", restockEntry?.restockLotNumber)
        assertEquals(lot.id, restockEntry?.lotId)

        // Verify total balance is 10 + 30 = 40
        val balance = repository.observeStockBalance(med).first()
        assertEquals(40_000L, balance.currentBalance.rawScaledValue)
    }

    @Test
    fun adjustStockToTarget_computesExactDeltaAndAuditLedger() = runBlocking {
        val med = Medication(
            id = "med_adjust",
            name = "Sertraline",
            strength = "50 mg",
            form = MedicationForm.TABLET,
            stockUnit = StockUnit.TABLETS,
            openingStock = ScaledQuantity.ofWhole(20),
            lowStockThreshold = ScaledQuantity.ofWhole(5)
        )
        val schedule = ScheduleDefinition(
            id = "sch_adjust",
            medicationId = "med_adjust",
            scheduleType = ScheduleType.DAILY,
            targetTimesMinuteOfDay = listOf(8 * 60),
            daysOfWeek = emptySet(),
            doseQuantityPerOccurrence = ScaledQuantity.ofWhole(1),
            startDateEpochDay = 20000L
        )
        repository.createMedication(med, schedule)

        // 1. Upward adjustment: actual count observed 25 (delta +5)
        val resUp = repository.adjustStockToTarget(
            medicationId = "med_adjust",
            currentBalance = ScaledQuantity.ofWhole(20),
            targetBalance = ScaledQuantity.ofWhole(25),
            unit = StockUnit.TABLETS,
            reason = "Found extra blister pack"
        )
        assertTrue(resUp.isSuccess)

        val balance1 = repository.observeStockBalance(med).first()
        assertEquals(25_000L, balance1.currentBalance.rawScaledValue)

        // 2. Downward adjustment: actual count observed 18 (delta -7)
        val resDown = repository.adjustStockToTarget(
            medicationId = "med_adjust",
            currentBalance = ScaledQuantity.ofWhole(25),
            targetBalance = ScaledQuantity.ofWhole(18),
            unit = StockUnit.TABLETS,
            reason = "Damaged tablets discarded"
        )
        assertTrue(resDown.isSuccess)

        val balance2 = repository.observeStockBalance(med).first()
        assertEquals(18_000L, balance2.currentBalance.rawScaledValue)

        // Check movements
        val movements = repository.observeMovementsForMedication("med_adjust").first()
        val inMove = movements.find { it.movementType == MovementType.ADJUSTMENT_IN }
        assertNotNull(inMove)
        assertEquals(5_000L, inMove?.quantity?.rawScaledValue)
        assertEquals("Found extra blister pack", inMove?.reason)

        val outMove = movements.find { it.movementType == MovementType.ADJUSTMENT_OUT }
        assertNotNull(outMove)
        assertEquals(7_000L, outMove?.quantity?.rawScaledValue)
        assertEquals("Damaged tablets discarded", outMove?.reason)
    }

    @Test
    fun correctDose_reversesLedgerCompensatingMovementIdempotently() = runBlocking {
        val med = Medication(
            id = "med_corr",
            name = "Lisinopril",
            strength = "10 mg",
            form = MedicationForm.TABLET,
            stockUnit = StockUnit.TABLETS,
            openingStock = ScaledQuantity.ofWhole(10),
            lowStockThreshold = ScaledQuantity.ofWhole(2)
        )
        val schedule = ScheduleDefinition(
            id = "sch_corr",
            medicationId = "med_corr",
            scheduleType = ScheduleType.DAILY,
            targetTimesMinuteOfDay = listOf(8 * 60),
            daysOfWeek = emptySet(),
            doseQuantityPerOccurrence = ScaledQuantity.ofWhole(1),
            startDateEpochDay = 20000L
        )
        repository.createMedication(med, schedule)

        val occId = "occ_corr_1"

        // 1. Dose recorded as TAKEN -> stock becomes 9
        repository.recordDose(
            occurrenceId = occId,
            medicationId = "med_corr",
            scheduledEpochMs = 1000L,
            quantity = ScaledQuantity.ofWhole(1),
            unit = StockUnit.TABLETS,
            status = DoseStatus.TAKEN
        )
        val balanceAfterTaken = repository.observeStockBalance(med).first()
        assertEquals(9_000L, balanceAfterTaken.currentBalance.rawScaledValue)

        // 2. User corrects dose to SKIPPED ("Accidentally tapped Take")
        val corrRes = repository.correctDose(
            occurrenceId = occId,
            targetStatus = DoseStatus.SKIPPED,
            reason = "Accidentally tapped Take"
        )
        assertTrue(corrRes.isSuccess)

        // Stock must be restored to 10 via DOSE_REVERSAL
        val balanceAfterCorr = repository.observeStockBalance(med).first()
        assertEquals(10_000L, balanceAfterCorr.currentBalance.rawScaledValue)

        val movements = repository.observeMovementsForMedication("med_corr").first()
        val reversalMove = movements.find { it.movementType == MovementType.DOSE_REVERSAL }
        assertNotNull(reversalMove)
        assertEquals(1_000L, reversalMove?.quantity?.rawScaledValue)
        assertEquals(occId, reversalMove?.doseOccurrenceId)

        // Verify dose event metadata has correction notes
        val dose = db.doseDao().getDoseByOccurrenceId(occId)
        assertNotNull(dose)
        assertEquals(DoseStatus.SKIPPED.name, dose?.status)
        assertEquals("Accidentally tapped Take", dose?.correctionNotes)
        assertNotNull(dose?.correctedAtEpochMs)

        // 3. Repeated correction to SKIPPED must be idempotent and not create duplicate DOSE_REVERSAL
        repository.correctDose(
            occurrenceId = occId,
            targetStatus = DoseStatus.SKIPPED,
            reason = "Repeated correction click"
        )
        val balanceAfterRepeat = repository.observeStockBalance(med).first()
        assertEquals(10_000L, balanceAfterRepeat.currentBalance.rawScaledValue)

        val reversalsCount = db.stockLedgerDao().countReversalsForOccurrence(occId)
        assertEquals(1, reversalsCount)
    }

    @Test
    fun doseTaking_debitsLotsUsingFEFO() = runBlocking {
        val med = Medication(
            id = "med_fefo",
            name = "Omeprazole",
            strength = "20 mg",
            form = MedicationForm.CAPSULE,
            stockUnit = StockUnit.CAPSULES,
            openingStock = ScaledQuantity.ZERO,
            lowStockThreshold = ScaledQuantity.ofWhole(2)
        )
        val schedule = ScheduleDefinition(
            id = "sch_fefo",
            medicationId = "med_fefo",
            scheduleType = ScheduleType.DAILY,
            targetTimesMinuteOfDay = listOf(8 * 60),
            daysOfWeek = emptySet(),
            doseQuantityPerOccurrence = ScaledQuantity.ofWhole(1),
            startDateEpochDay = 20000L
        )
        repository.createMedication(med, schedule)

        // Supply 1: Expiring sooner (in 10 days) -> 5 capsules
        repository.restockWithLot(
            medicationId = "med_fefo",
            quantity = ScaledQuantity.ofWhole(5),
            unit = StockUnit.CAPSULES,
            lotNumber = "LOT-SOON",
            expiryDateEpochMs = 100000L
        )

        // Supply 2: Expiring later (in 30 days) -> 10 capsules
        repository.restockWithLot(
            medicationId = "med_fefo",
            quantity = ScaledQuantity.ofWhole(10),
            unit = StockUnit.CAPSULES,
            lotNumber = "LOT-LATER",
            expiryDateEpochMs = 300000L
        )

        // Take dose of 2 capsules
        repository.recordDose(
            occurrenceId = "occ_fefo_1",
            medicationId = "med_fefo",
            scheduledEpochMs = 1000L,
            quantity = ScaledQuantity.ofWhole(2),
            unit = StockUnit.CAPSULES,
            status = DoseStatus.TAKEN
        )

        // LOT-SOON should have 3 remaining, LOT-LATER should still have 10
        val lotsAfterDose1 = repository.observeLotsForMedication("med_fefo").first()
        val lotSoon = lotsAfterDose1.find { it.lotNumber == "LOT-SOON" }
        val lotLater = lotsAfterDose1.find { it.lotNumber == "LOT-LATER" }
        assertEquals(3_000L, lotSoon?.remainingQuantity?.rawScaledValue)
        assertEquals(10_000L, lotLater?.remainingQuantity?.rawScaledValue)

        // Take dose of 4 capsules (3 from LOT-SOON, 1 from LOT-LATER)
        repository.recordDose(
            occurrenceId = "occ_fefo_2",
            medicationId = "med_fefo",
            scheduledEpochMs = 2000L,
            quantity = ScaledQuantity.ofWhole(4),
            unit = StockUnit.CAPSULES,
            status = DoseStatus.TAKEN
        )

        val lotsAfterDose2 = repository.observeLotsForMedication("med_fefo").first()
        val lotSoon2 = lotsAfterDose2.find { it.lotNumber == "LOT-SOON" }
        val lotLater2 = lotsAfterDose2.find { it.lotNumber == "LOT-LATER" }
        assertEquals(0L, lotSoon2?.remainingQuantity?.rawScaledValue)
        assertEquals(9_000L, lotLater2?.remainingQuantity?.rawScaledValue)
    }

    @Test
    fun prescriptionRepository_supportsCreateUpdateRequestCollectAndAuditHistory() = runBlocking {
        val med = Medication(
            id = "med_rx",
            name = "Ramipril",
            strength = "5 mg",
            form = MedicationForm.CAPSULE,
            stockUnit = StockUnit.CAPSULES,
            openingStock = ScaledQuantity.ofWhole(28),
            lowStockThreshold = ScaledQuantity.ofWhole(7)
        )
        val schedule = ScheduleDefinition(
            id = "sch_rx",
            medicationId = "med_rx",
            scheduleType = ScheduleType.DAILY,
            targetTimesMinuteOfDay = listOf(8 * 60),
            daysOfWeek = emptySet(),
            doseQuantityPerOccurrence = ScaledQuantity.ofWhole(1),
            startDateEpochDay = 20000L
        )
        repository.createMedication(med, schedule)

        val todayEpochDay = 20010L
        val rx = Prescription(
            id = "rx_ramipril_1",
            medicationId = "med_rx",
            referenceNumber = "NHS-RX-551",
            issueDateEpochDay = 20000L,
            expiryDateEpochDay = 20180L,
            quantityPrescribed = ScaledQuantity.ofWhole(28),
            unit = StockUnit.CAPSULES,
            repeatsAuthorised = 2,
            repeatsRemaining = 2,
            pharmacyName = "Station Pharmacy",
            notes = "Order 7 days early"
        )

        val createRes = repository.createPrescription(rx)
        assertTrue(createRes.isSuccess)

        // 1. Request Repeat -> must NOT decrement repeatsRemaining
        val reqRes = repository.requestPrescriptionRepeat(
            prescriptionId = "rx_ramipril_1",
            currentDateEpochDay = todayEpochDay,
            actionIdempotencyKey = "req_key_1"
        )
        assertTrue(reqRes.isSuccess)

        var storedRx = repository.observePrescriptionsForMedication("med_rx").first().first()
        assertEquals(2, storedRx.repeatsRemaining)
        assertTrue(storedRx.hasPendingRepeatRequest)

        // 2. Collect Repeat -> decrements repeatsRemaining from 2 to 1 and clears pending flag
        val colRes = repository.collectPrescriptionRepeat(
            prescriptionId = "rx_ramipril_1",
            currentDateEpochDay = todayEpochDay,
            actionIdempotencyKey = "col_key_1"
        )
        assertTrue(colRes.isSuccess)

        storedRx = repository.observePrescriptionsForMedication("med_rx").first().first()
        assertEquals(1, storedRx.repeatsRemaining)
        assertFalse(storedRx.hasPendingRepeatRequest)

        // 3. Duplicate Collect Repeat with same actionIdempotencyKey -> must NOT decrement again
        val dupColRes = repository.collectPrescriptionRepeat(
            prescriptionId = "rx_ramipril_1",
            currentDateEpochDay = todayEpochDay,
            actionIdempotencyKey = "col_key_1"
        )
        assertTrue(dupColRes.isSuccess)
        storedRx = repository.observePrescriptionsForMedication("med_rx").first().first()
        assertEquals(1, storedRx.repeatsRemaining)

        // 4. Verify audit events (CREATED, REPEAT_REQUESTED, REPEAT_COLLECTED)
        val events = repository.observeEventsForPrescription("rx_ramipril_1").first()
        assertEquals(3, events.size)
        assertTrue(events.any { it.eventType == PrescriptionEventType.CREATED })
        assertTrue(events.any { it.eventType == PrescriptionEventType.REPEAT_REQUESTED })
        assertTrue(events.any { it.eventType == PrescriptionEventType.REPEAT_COLLECTED })
    }
}
