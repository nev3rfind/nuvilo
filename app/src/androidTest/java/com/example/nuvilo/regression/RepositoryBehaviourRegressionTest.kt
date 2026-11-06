package com.example.nuvilo.regression

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
import com.example.nuvilo.domain.model.RecordingMode
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

/**
 * Regression Test Suite: Category B (Room, Repositories & Migrations)
 * Tests T079 to T085: High-Level Repository Operations and Flow Observables
 */
@RunWith(AndroidJUnit4::class)
class RepositoryBehaviourRegressionTest {

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

    private val sampleMed = Medication(
        id = "med_repo_test",
        name = "Lisinopril",
        strength = "10mg",
        form = MedicationForm.TABLET,
        stockUnit = StockUnit.TABLETS,
        openingStock = ScaledQuantity.ofWhole(30),
        lowStockThreshold = ScaledQuantity.ofWhole(7),
        recordingMode = RecordingMode.CONFIRM
    )

    private val sampleSchedule = ScheduleDefinition(
        id = "sch_repo_test",
        medicationId = "med_repo_test",
        scheduleType = ScheduleType.DAILY,
        targetTimesMinuteOfDay = listOf(9 * 60),
        daysOfWeek = emptySet(),
        doseQuantityPerOccurrence = ScaledQuantity.ofWhole(1),
        startDateEpochDay = 19500L
    )

    @Test
    fun t079_createMedicationAtomicallyInsertsMedicationScheduleAndOpeningBalance() = runBlocking {
        // T079: createMedication with schedule atomically inserts both and creates opening balance ledger entry
        val result = repository.createMedication(sampleMed, sampleSchedule)
        assertTrue(result.isSuccess)

        val retrievedMed = repository.getMedication("med_repo_test")
        assertNotNull(retrievedMed)
        assertEquals("Lisinopril", retrievedMed?.name)

        val schedules = repository.getSchedulesForMedication("med_repo_test")
        assertEquals(1, schedules.size)

        val movements = repository.observeMovementsForMedication("med_repo_test").first()
        assertEquals(1, movements.size)
        assertEquals(MovementType.OPENING_BALANCE, movements[0].movementType)
        assertEquals(ScaledQuantity.ofWhole(30), movements[0].quantity)
    }

    @Test
    fun t080_recordDoseConfirmedDeductsStockViaLedgerEntry() = runBlocking {
        // T080: recordDose with CONFIRM records dose and inserts DOSE_TAKEN ledger entry
        repository.createMedication(sampleMed, sampleSchedule)

        val recordResult = repository.recordDose(
            occurrenceId = "occ_taken_1",
            medicationId = "med_repo_test",
            scheduledEpochMs = 1700000000000L,
            quantity = ScaledQuantity.ofWhole(1),
            unit = StockUnit.TABLETS,
            status = DoseStatus.TAKEN
        )
        assertTrue(recordResult.isSuccess)

        val balance = repository.observeStockBalance(sampleMed).first()
        // 30 - 1 = 29
        assertEquals(ScaledQuantity.ofWhole(29), balance.currentBalance)
    }

    @Test
    fun t081_recordDoseSkippedDoesNotDeductStock() = runBlocking {
        // T081: recordDose with SKIPPED records dose without deducting stock
        repository.createMedication(sampleMed, sampleSchedule)

        val recordResult = repository.recordDose(
            occurrenceId = "occ_skipped_1",
            medicationId = "med_repo_test",
            scheduledEpochMs = 1700000000000L,
            quantity = ScaledQuantity.ofWhole(1),
            unit = StockUnit.TABLETS,
            status = DoseStatus.SKIPPED
        )
        assertTrue(recordResult.isSuccess)

        val balance = repository.observeStockBalance(sampleMed).first()
        // Balance remains unchanged: 30
        assertEquals(ScaledQuantity.ofWhole(30), balance.currentBalance)
    }

    @Test
    fun t082_correctDoseCreatesCompensatingDoseReversal() = runBlocking {
        // T082: correctDose creates compensating DOSE_REVERSAL ledger entry
        repository.createMedication(sampleMed, sampleSchedule)

        // First record dose taken
        repository.recordDose(
            occurrenceId = "occ_to_correct",
            medicationId = "med_repo_test",
            scheduledEpochMs = 1700000000000L,
            quantity = ScaledQuantity.ofWhole(2),
            unit = StockUnit.TABLETS,
            status = DoseStatus.TAKEN
        )
        var balance = repository.observeStockBalance(sampleMed).first()
        assertEquals(ScaledQuantity.ofWhole(28), balance.currentBalance)

        // Correct dose to SKIPPED (user clicked by mistake)
        val correctResult = repository.correctDose(
            occurrenceId = "occ_to_correct",
            targetStatus = DoseStatus.SKIPPED,
            reason = "Logged by mistake"
        )
        assertTrue(correctResult.isSuccess)

        balance = repository.observeStockBalance(sampleMed).first()
        // Compensating reversal restored stock: 28 + 2 = 30
        assertEquals(ScaledQuantity.ofWhole(30), balance.currentBalance)
    }

    @Test
    fun t083_requestPrescriptionRepeatUpdatesPrescriptionAndEmitsEvent() = runBlocking {
        // T083: requestPrescriptionRepeat updates prescription and records audit event
        repository.createMedication(sampleMed, sampleSchedule)

        val rx = Prescription(
            id = "rx_repeat_test",
            medicationId = "med_repo_test",
            issueDateEpochDay = 19500L,
            expiryDateEpochDay = 20000L,
            repeatsAuthorised = 3,
            repeatsRemaining = 3,
            status = PrescriptionStatus.ACTIVE
        )
        repository.createPrescription(rx)

        val requestResult = repository.requestPrescriptionRepeat(
            prescriptionId = "rx_repeat_test",
            currentDateEpochDay = 19600L,
            actionIdempotencyKey = "req_101"
        )
        assertTrue(requestResult.isSuccess)

        val updatedRx = repository.observePrescriptionsForMedication("med_repo_test").first().first()
        assertTrue(updatedRx.hasPendingRepeatRequest)
        assertEquals(3, updatedRx.repeatsRemaining)

        val events = repository.observeEventsForPrescription("rx_repeat_test").first()
        assertTrue(events.any { it.eventType == PrescriptionEventType.REPEAT_REQUESTED })
    }

    @Test
    fun t084_collectPrescriptionRepeatDecrementsRepeatsRemaining() = runBlocking {
        // T084: collectPrescriptionRepeat updates prescription and decrements repeat count
        repository.createMedication(sampleMed, sampleSchedule)

        val rx = Prescription(
            id = "rx_collect_test",
            medicationId = "med_repo_test",
            issueDateEpochDay = 19500L,
            expiryDateEpochDay = 20000L,
            repeatsAuthorised = 3,
            repeatsRemaining = 3,
            hasPendingRepeatRequest = true,
            status = PrescriptionStatus.ACTIVE
        )
        repository.createPrescription(rx)

        val collectResult = repository.collectPrescriptionRepeat(
            prescriptionId = "rx_collect_test",
            currentDateEpochDay = 19600L,
            actionIdempotencyKey = "col_101"
        )
        assertTrue(collectResult.isSuccess)

        val updatedRx = repository.observePrescriptionsForMedication("med_repo_test").first().first()
        assertFalse(updatedRx.hasPendingRepeatRequest)
        assertEquals(2, updatedRx.repeatsRemaining)
    }

    @Test
    fun t085_observeStockBalanceCalculatesLiveBalanceReactively() = runBlocking {
        // T085: observeStockBalance calculates live balance as movements occur
        repository.createMedication(sampleMed, sampleSchedule)

        // Live balance before any additional movements
        val initialBalance = repository.observeStockBalance(sampleMed).first()
        assertEquals(ScaledQuantity.ofWhole(30), initialBalance.currentBalance)
        assertFalse(initialBalance.isLowStock)

        // Add restock
        repository.adjustStock(
            medicationId = "med_repo_test",
            quantity = ScaledQuantity.ofWhole(20),
            unit = StockUnit.TABLETS,
            reason = "Restocked 20 tablets",
            isIncrease = true
        )

        val newBalance = repository.observeStockBalance(sampleMed).first()
        assertEquals(ScaledQuantity.ofWhole(50), newBalance.currentBalance)
    }
}
