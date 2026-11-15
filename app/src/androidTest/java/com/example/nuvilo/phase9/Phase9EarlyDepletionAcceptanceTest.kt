package com.example.nuvilo.phase9

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.nuvilo.data.local.NoveloDatabase
import com.example.nuvilo.data.repository.NoveloRepositoryImpl
import com.example.nuvilo.domain.model.AdministrationProfile
import com.example.nuvilo.domain.model.AdministrationSite
import com.example.nuvilo.domain.model.DepletionReason
import com.example.nuvilo.domain.model.DoseStatus
import com.example.nuvilo.domain.model.Medication
import com.example.nuvilo.domain.model.MedicationForm
import com.example.nuvilo.domain.model.MovementType
import com.example.nuvilo.domain.model.PackageStatus
import com.example.nuvilo.domain.model.PackageType
import com.example.nuvilo.domain.model.PackagingConfig
import com.example.nuvilo.domain.model.RecordingMode
import com.example.nuvilo.domain.model.ScaledQuantity
import com.example.nuvilo.domain.model.ScheduleDefinition
import com.example.nuvilo.domain.model.ScheduleType
import com.example.nuvilo.domain.model.StockUnit
import com.example.nuvilo.domain.model.TrackingPrecision
import com.example.nuvilo.domain.stock.StockLedgerCalculator
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
import java.util.UUID

/**
 * Phase 9 Acceptance Test: Early Package Depletion, Intelligent Replacement & Accidental Correction.
 *
 * Implements the mandatory acceptance scenario:
 * - Hydro Forte eye drops in 2 Multi-dose Bottles (300 drops each).
 * - 120 administrations recorded = 240 drops consumed from Bottle A.
 * - 60 drops remaining in Bottle A when the user observes it is physically finished.
 * - Early depletion writes off 60 drops as WASTAGE, marks Bottle A EMPTY, and stages Bottle B as next unopened.
 * - Confirms zero dose fabrication (dose events remain exactly 120) and zero adherence contamination.
 * - Reversal restores Bottle A balance to 60 drops via compensating ADJUSTMENT_IN.
 */
@RunWith(AndroidJUnit4::class)
class Phase9EarlyDepletionAcceptanceTest {

    private lateinit var database: NoveloDatabase
    private lateinit var repository: NoveloRepositoryImpl
    private val stockCalculator = StockLedgerCalculator()

    @Before
    fun setUp() {
        val context: Context = ApplicationProvider.getApplicationContext()
        database = NoveloDatabase.createInMemory(context)
        repository = NoveloRepositoryImpl(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun hydroForteEarlyDepletion_recordsWastage_preservesAdherence_stagesNextBottle() = runBlocking {
        val medId = "med-hydro-acceptance"
        val med = Medication(
            id = medId,
            name = "Hydro Forte",
            strength = "0.3%",
            form = MedicationForm.DROPS,
            stockUnit = StockUnit.DROPS,
            openingStock = ScaledQuantity.ofWhole(600),
            lowStockThreshold = ScaledQuantity.ofWhole(40),
            recordingMode = RecordingMode.CONFIRM,
            packagingConfig = PackagingConfig(
                packageType = PackageType.BOTTLE,
                consumptionUnit = StockUnit.DROPS,
                contentsPerPackage = ScaledQuantity.ofWhole(300),
                netContentLabel = "10 ml",
                discardAfterOpeningDays = 28,
                trackingPrecision = TrackingPrecision.ESTIMATED
            ),
            administrationProfile = AdministrationProfile(
                site = AdministrationSite.BOTH_EYES,
                leftEyeQuantity = ScaledQuantity.ofWhole(1),
                rightEyeQuantity = ScaledQuantity.ofWhole(1)
            )
        )

        val schedule = ScheduleDefinition(
            id = "sch-hydro-acceptance",
            medicationId = medId,
            scheduleType = ScheduleType.DAILY,
            targetTimesMinuteOfDay = listOf(8 * 60, 14 * 60, 20 * 60), // 3 times daily
            daysOfWeek = emptySet(),
            doseQuantityPerOccurrence = ScaledQuantity.ofWhole(2), // 2 drops per dose
            startDateEpochDay = 19000L
        )

        // Step 1: Create medication with 2 packages (Bottle A and Bottle B, 300 drops each)
        val createResult = repository.createMedicationWithPackages(
            medication = med,
            initialSchedule = schedule,
            initialPackagesCount = 2,
            expiryDateEpochMs = System.currentTimeMillis() + 180L * 86400000L, // 6 months out
            lotNumber = "LOT-HF-001"
        )
        assertTrue("Medication creation should succeed", createResult.isSuccess)

        var lots = repository.observeLotsForMedication(medId).first()
        assertEquals("Should have exactly 2 packages", 2, lots.size)

        val bottleA = lots[0]
        val bottleB = lots[1]

        // Open Bottle A explicitly for consumption
        val openResult = repository.openPackage(bottleA.id)
        assertTrue("Bottle A open should succeed", openResult.isSuccess)

        // Step 2: Record 120 administrations (2 drops each = 240 drops consumed)
        for (i in 1..120) {
            val occId = "dose-occ-$i"
            val doseResult = repository.recordDose(
                occurrenceId = occId,
                medicationId = medId,
                scheduledEpochMs = System.currentTimeMillis() - (120 - i) * 3600000L,
                quantity = ScaledQuantity.ofWhole(2),
                unit = StockUnit.DROPS,
                status = DoseStatus.TAKEN
            )
            assertTrue("Dose $i recording should succeed", doseResult.isSuccess)
        }

        // Verify state after 120 doses:
        lots = repository.observeLotsForMedication(medId).first()
        val updatedBottleA = lots.first { it.id == bottleA.id }
        val updatedBottleB = lots.first { it.id == bottleB.id }

        assertEquals("Bottle A status should be OPENED", PackageStatus.OPENED, updatedBottleA.packageStatus)
        assertEquals("Bottle A remaining drops should be 60", 60L * 1000L, updatedBottleA.remainingQuantity.rawScaledValue)
        assertEquals("Bottle B status should be UNOPENED", PackageStatus.UNOPENED, updatedBottleB.packageStatus)
        assertEquals("Bottle B remaining drops should be 300", 300L * 1000L, updatedBottleB.remainingQuantity.rawScaledValue)

        val doseEventsBefore = repository.observeDosesForMedication(medId).first()
        assertEquals("Dose event count should be exactly 120", 120, doseEventsBefore.size)

        // Step 3: Trigger Early Package Depletion on Bottle A (User marks bottle finished early)
        val nowMs = System.currentTimeMillis()
        val depletionResult = repository.depletePackage(
            lotId = bottleA.id,
            reason = DepletionReason.EMPTY_NO_DROPS_LEFT,
            userNote = "Bottle dry after 120 doses",
            nowEpochMs = nowMs
        )
        assertTrue("Early depletion should succeed", depletionResult.isSuccess)
        val outcome = depletionResult.getOrThrow()

        // Invariant Verification:
        // 1. Bottle A is EMPTY and has zero remaining quantity
        assertEquals("Bottle A status should be EMPTY", PackageStatus.EMPTY, outcome.depletedLot.packageStatus)
        assertEquals("Bottle A remaining quantity should be 0", ScaledQuantity.ZERO, outcome.depletedLot.remainingQuantity)
        assertNotNull("Depleted timestamp should be recorded", outcome.depletedLot.depletedAtEpochMs)
        assertEquals("Depletion reason code should match", "BOTTLE_EMPTY", outcome.depletedLot.depletionReason)

        // 2. Compensating WASTAGE entry of exactly 60 drops
        assertNotNull("Compensating ledger entry must be created", outcome.compensatingEntry)
        assertEquals("Compensating entry type must be WASTAGE", MovementType.WASTAGE, outcome.compensatingEntry?.movementType)
        assertEquals("Wastage quantity should be exactly 60 drops", 60L * 1000L, outcome.compensatingEntry?.quantity?.rawScaledValue)

        // 3. ZERO DOSE FABRICATION: Dose event count must remain exactly 120!
        val doseEventsAfter = repository.observeDosesForMedication(medId).first()
        assertEquals("Dose event count must remain exactly 120 without synthetic doses", 120, doseEventsAfter.size)

        // 4. Bottle B staged next as "Ready to Open", NOT auto-opened
        val stagedNext = outcome.stagedNextLot ?: stockCalculator.findStagedNextPackage(
            repository.observeLotsForMedication(medId).first(),
            nowMs
        )
        assertNotNull("Next bottle must be staged", stagedNext)
        assertEquals("Staged bottle must be Bottle B", bottleB.id, stagedNext?.id)
        assertEquals("Staged bottle must remain UNOPENED", PackageStatus.UNOPENED, stagedNext?.packageStatus)
        assertEquals("Staged bottle must have 300 drops available", 300L * 1000L, stagedNext?.remainingQuantity?.rawScaledValue)

        // 5. Total usable supply recalculation: 300 drops (from Bottle B)
        val balance = repository.observeStockBalance(med).first()
        assertEquals("Usable balance should be exactly 300 drops", 300L * 1000L, balance.currentBalance.rawScaledValue)

        // Step 4: Accidental Depletion Correction (Reopen Bottle A)
        val reopenResult = repository.reopenDepletedPackage(
            lotId = bottleA.id,
            reason = "Found 1 extra day of drops in Bottle A",
            nowEpochMs = System.currentTimeMillis()
        )
        assertTrue("Reopen package should succeed", reopenResult.isSuccess)
        val reopenOutcome = reopenResult.getOrThrow()

        // Verify restored Bottle A:
        assertEquals("Bottle A status should be restored to OPENED", PackageStatus.OPENED, reopenOutcome.reopenedLot.packageStatus)
        assertEquals("Bottle A remaining balance should be restored to 60 drops", 60L * 1000L, reopenOutcome.reopenedLot.remainingQuantity.rawScaledValue)
        assertNull("Depleted timestamp should be cleared", reopenOutcome.reopenedLot.depletedAtEpochMs)
        assertNull("Depletion reason should be cleared", reopenOutcome.reopenedLot.depletionReason)

        // Verify compensating ADJUSTMENT_IN entry:
        assertEquals("Reversal entry must be ADJUSTMENT_IN", MovementType.ADJUSTMENT_IN, reopenOutcome.reversalEntry.movementType)
        assertEquals("Reversal quantity must be 60 drops", 60L * 1000L, reopenOutcome.reversalEntry.quantity.rawScaledValue)

        // Verify total usable balance restored to 360 drops:
        val restoredBalance = repository.observeStockBalance(med).first()
        assertEquals("Restored usable balance should be exactly 360 drops", 360L * 1000L, restoredBalance.currentBalance.rawScaledValue)

        // Dose count is still strictly 120!
        val finalDoses = repository.observeDosesForMedication(medId).first()
        assertEquals("Dose count is strictly unchanged throughout reopening", 120, finalDoses.size)
    }

    @Test
    fun earlyDepletion_terminalStateIdempotencyGuards() = runBlocking {
        val medId = "med-guard-test"
        val med = Medication(
            id = medId,
            name = "Test Ointment",
            strength = "1%",
            form = MedicationForm.TOPICAL_CREAM,
            stockUnit = StockUnit.GRAMS,
            openingStock = ScaledQuantity.ofWhole(30),
            lowStockThreshold = ScaledQuantity.ofWhole(5),
            recordingMode = RecordingMode.CONFIRM,
            packagingConfig = PackagingConfig(
                packageType = PackageType.TUBE,
                consumptionUnit = StockUnit.GRAMS,
                contentsPerPackage = ScaledQuantity.ofWhole(30),
                trackingPrecision = TrackingPrecision.ESTIMATED
            )
        )

        repository.createMedicationWithPackages(
            medication = med,
            initialSchedule = ScheduleDefinition(
                id = "sch-guard",
                medicationId = medId,
                scheduleType = ScheduleType.DAILY,
                targetTimesMinuteOfDay = listOf(9 * 60),
                daysOfWeek = emptySet(),
                doseQuantityPerOccurrence = ScaledQuantity.ofWhole(1),
                startDateEpochDay = 19000L
            ),
            initialPackagesCount = 1
        )

        val lot = repository.observeLotsForMedication(medId).first()[0]
        repository.openPackage(lot.id)

        // First depletion succeeds
        val dep1 = repository.depletePackage(lot.id, DepletionReason.EMPTY_NO_DROPS_LEFT)
        assertTrue("First depletion should succeed", dep1.isSuccess)

        // Second depletion on already EMPTY package must fail gracefully
        val dep2 = repository.depletePackage(lot.id, DepletionReason.EMPTY_NO_DROPS_LEFT)
        assertTrue("Second depletion on empty package must fail", dep2.isFailure)

        // Reopen succeeds
        val reopen1 = repository.reopenDepletedPackage(lot.id)
        assertTrue("First reopen should succeed", reopen1.isSuccess)

        // Second reopen on already OPENED package must fail gracefully
        val reopen2 = repository.reopenDepletedPackage(lot.id)
        assertTrue("Second reopen on already open package must fail", reopen2.isFailure)
    }
}
