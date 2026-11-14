package com.example.nuvilo.phase10

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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Phase 10 Acceptance Test: Physical Package Inventory, Multi-Bottle Lifecycle & Intelligent Stock Management.
 *
 * Implements the mandatory 7-bottle Hydro Forte clinical acceptance scenario:
 * 1. Initial Restock: 7 physical bottles (300 drops/bottle, 1 opened + 6 unopened = 7 physical bottles,
 *    2,100 drops total, 210 days of supply at 10 drops/day).
 * 2. Regular Consumption: 12 doses (24 drops) taken. Bottle 1 = 276 drops, unopened count = 6 (300 drops each),
 *    total physical bottles = 7, total supply = 2,076 drops, 12 dose events, adherence 100%.
 * 3. Bottle Exhaustion & Anti-Auto-Open Invariant: 138 more doses taken (total 150 doses = 300 drops).
 *    Bottle 1 transitions to EMPTY. Bottle 2 is NOT auto-opened (remains strictly UNOPENED).
 *    Bottle 2 is staged as "Ready to Open". User confirms physical opening of Bottle 2 -> Bottle 2 OPENED (300 drops),
 *    unopened count = 5, active physical bottles = 6.
 * 4. Early Bottle Depletion: 120 doses taken from Bottle 2 (240 drops consumed, 60 drops remaining).
 *    User marks Bottle 2 finished early. Bottle 2 transitions to EMPTY. Compensating WASTAGE entry of 60 drops created.
 *    ZERO DOSE FABRICATION: dose events remain strictly 270 (150 + 120). Adherence 100% untouched.
 *    Bottle 3 staged next as UNOPENED. Total supply recalculated to 1,500 drops (5 unopened x 300 drops).
 * 5. Accidental Opening Reversal: User opens Bottle 3, realizes mistake, calls revertOpenedPackage.
 *    Bottle 3 safely reverts to UNOPENED with timestamps cleared.
 * 6. Partial Consumption Protection Guard: User opens Bottle 3, takes 1 dose (2 drops taken, 298 drops remaining).
 *    Attempting revertOpenedPackage throws IllegalStateException / fails.
 * 7. Accidental Depletion Reversal: Reopening Bottle 2 restores status to OPENED (60 drops) via ADJUSTMENT_IN.
 */
@RunWith(AndroidJUnit4::class)
class Phase10PackageInventoryAcceptanceTest {

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
    fun hydroForte7BottleScenario_lifecycle_depletion_and_reversals() = runBlocking {
        val medId = "med-hydro-forte-p10"
        val med = Medication(
            id = medId,
            name = "Hydro Forte",
            strength = "0.3%",
            form = MedicationForm.DROPS,
            stockUnit = StockUnit.DROPS,
            openingStock = ScaledQuantity.ZERO, // Will be restocked via package inventory
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
            id = "sch-hydro-forte-p10",
            medicationId = medId,
            scheduleType = ScheduleType.DAILY,
            targetTimesMinuteOfDay = listOf(7 * 60, 11 * 60, 15 * 60, 19 * 60, 22 * 60), // 5 times daily = 10 drops/day
            daysOfWeek = emptySet(),
            doseQuantityPerOccurrence = ScaledQuantity.ofWhole(2), // 2 drops per dose
            startDateEpochDay = 19000L
        )

        // --------------------------------------------------------------------------------
        // STEP 1: Medication Creation & 7-Bottle Restock (1 Opened + 6 Unopened = 7 Physical Bottles)
        // --------------------------------------------------------------------------------
        val createMedResult = repository.createMedication(med, schedule)
        assertTrue("Medication creation should succeed", createMedResult.isSuccess)

        val restockResult = repository.restockPackages(
            medicationId = medId,
            packageCount = 7,
            contentsPerPackage = ScaledQuantity.ofWhole(300),
            consumptionUnit = StockUnit.DROPS,
            packageType = PackageType.BOTTLE,
            discardAfterOpeningDays = 28,
            lotNumber = "LOT-HF-2026",
            expiryDateEpochMs = System.currentTimeMillis() + 180L * 86_400_000L,
            notes = "Initial prescription delivery (7 bottles)",
            openFirstPackageImmediately = true
        )
        assertTrue("7-bottle restock should succeed", restockResult.isSuccess)

        var lots = repository.observeLotsForMedication(medId).first().sortedBy { it.createdAtEpochMs }
        assertEquals("Should have exactly 7 packages in database", 7, lots.size)

        val bottle1 = lots[0]
        val unopenedBottles = lots.filter { it.packageStatus == PackageStatus.UNOPENED }
        val openedBottles = lots.filter { it.packageStatus == PackageStatus.OPENED }

        // Assert Step 1 initial state:
        assertEquals("Bottle 1 must be OPENED", PackageStatus.OPENED, bottle1.packageStatus)
        assertEquals("Bottle 1 remaining quantity must be 300 drops", 300L * 1000L, bottle1.remainingQuantity.rawScaledValue)
        assertNotNull("Bottle 1 must have opened timestamp", bottle1.openedAtEpochMs)
        assertNotNull("Bottle 1 must have discard cutoff timestamp", bottle1.discardAfterOpeningEpochMs)

        assertEquals("Exactly 1 opened bottle", 1, openedBottles.size)
        assertEquals("Exactly 6 unopened bottles", 6, unopenedBottles.size)
        assertEquals("Total physical bottles count must be 7 (1 opened + 6 unopened)", 7, openedBottles.size + unopenedBottles.size)

        for (unopened in unopenedBottles) {
            assertEquals("Unopened bottle must have 300 drops", 300L * 1000L, unopened.remainingQuantity.rawScaledValue)
            assertNull("Unopened bottle must not have opened timestamp", unopened.openedAtEpochMs)
        }

        val initialBalance = repository.observeStockBalance(med).first()
        assertEquals("Total usable supply must be 2,100 drops", 2100L * 1000L, initialBalance.currentBalance.rawScaledValue)

        // --------------------------------------------------------------------------------
        // STEP 2: Regular Consumption: 12 doses (24 drops) taken
        // --------------------------------------------------------------------------------
        val nowMs = System.currentTimeMillis()
        for (i in 1..12) {
            val doseResult = repository.recordDose(
                occurrenceId = "dose-occ-p10-$i",
                medicationId = medId,
                scheduledEpochMs = nowMs - (150 - i) * 7_200_000L,
                quantity = ScaledQuantity.ofWhole(2),
                unit = StockUnit.DROPS,
                status = DoseStatus.TAKEN
            )
            assertTrue("Dose $i record should succeed", doseResult.isSuccess)
        }

        // Verify state after 12 doses:
        lots = repository.observeLotsForMedication(medId).first().sortedBy { it.createdAtEpochMs }
        val updatedBottle1 = lots.first { it.id == bottle1.id }
        assertEquals("Bottle 1 remaining drops must be 276 drops (300 - 24)", 276L * 1000L, updatedBottle1.remainingQuantity.rawScaledValue)
        assertEquals("Bottle 1 must remain OPENED", PackageStatus.OPENED, updatedBottle1.packageStatus)

        val unopenedAfter12 = lots.filter { it.packageStatus == PackageStatus.UNOPENED }
        assertEquals("Unopened count must still be strictly 6", 6, unopenedAfter12.size)
        for (unopened in unopenedAfter12) {
            assertEquals("Unopened bottles must remain untouched at 300 drops", 300L * 1000L, unopened.remainingQuantity.rawScaledValue)
        }

        val dosesAfter12 = repository.observeDosesForMedication(medId).first()
        assertEquals("Dose event count must be exactly 12", 12, dosesAfter12.size)

        val balanceAfter12 = repository.observeStockBalance(med).first()
        assertEquals("Total usable balance must be 2,076 drops (2100 - 24)", 2076L * 1000L, balanceAfter12.currentBalance.rawScaledValue)

        // --------------------------------------------------------------------------------
        // STEP 3: Exhaust Bottle 1: 138 more doses taken (total 150 doses = 300 drops)
        // --------------------------------------------------------------------------------
        for (i in 13..150) {
            val doseResult = repository.recordDose(
                occurrenceId = "dose-occ-p10-$i",
                medicationId = medId,
                scheduledEpochMs = nowMs - (150 - i) * 7_200_000L,
                quantity = ScaledQuantity.ofWhole(2),
                unit = StockUnit.DROPS,
                status = DoseStatus.TAKEN
            )
            assertTrue("Dose $i record should succeed", doseResult.isSuccess)
        }

        // Verify state after 150 doses (Bottle 1 exhausted):
        lots = repository.observeLotsForMedication(medId).first().sortedBy { it.createdAtEpochMs }
        val exhaustedBottle1 = lots.first { it.id == bottle1.id }
        assertEquals("Bottle 1 must now be EMPTY", PackageStatus.EMPTY, exhaustedBottle1.packageStatus)
        assertEquals("Bottle 1 remaining drops must be 0", ScaledQuantity.ZERO, exhaustedBottle1.remainingQuantity)

        val bottle2 = lots[1]
        // MANDATORY INVARIANT: Bottle 2 must NOT be auto-opened by dose consumption!
        assertEquals("Bottle 2 must remain strictly UNOPENED", PackageStatus.UNOPENED, bottle2.packageStatus)
        assertEquals("Bottle 2 must have full 300 drops", 300L * 1000L, bottle2.remainingQuantity.rawScaledValue)

        // Bottle 2 must be staged as "Ready to Open"
        val stagedNext = repository.getStagedNextPackage(medId)
        assertNotNull("Next package must be staged", stagedNext)
        assertEquals("Staged package must be Bottle 2", bottle2.id, stagedNext?.id)
        assertEquals("Staged package status must be UNOPENED", PackageStatus.UNOPENED, stagedNext?.packageStatus)

        // User physically unseals Bottle 2:
        val openBottle2Result = repository.openPackage(bottle2.id)
        assertTrue("Opening Bottle 2 should succeed", openBottle2Result.isSuccess)
        val openedBottle2 = openBottle2Result.getOrThrow()

        assertEquals("Bottle 2 status must now be OPENED", PackageStatus.OPENED, openedBottle2.packageStatus)
        assertEquals("Bottle 2 remaining must be 300 drops", 300L * 1000L, openedBottle2.remainingQuantity.rawScaledValue)
        assertNotNull("Bottle 2 openedAtEpochMs must be recorded", openedBottle2.openedAtEpochMs)

        // Check active physical bottles count:
        lots = repository.observeLotsForMedication(medId).first().sortedBy { it.createdAtEpochMs }
        val activeOpened = lots.filter { it.packageStatus == PackageStatus.OPENED }
        val activeUnopened = lots.filter { it.packageStatus == PackageStatus.UNOPENED }
        assertEquals("Currently opened bottles count is 1 (Bottle 2)", 1, activeOpened.size)
        assertEquals("Unopened bottles count is 5 (Bottles 3..7)", 5, activeUnopened.size)
        assertEquals("Total active physical bottles is 6 (1 opened + 5 unopened)", 6, activeOpened.size + activeUnopened.size)

        // --------------------------------------------------------------------------------
        // STEP 4: Early Depletion of Bottle 2 (120 doses = 240 drops consumed, 60 drops left)
        // --------------------------------------------------------------------------------
        for (i in 151..270) {
            val doseResult = repository.recordDose(
                occurrenceId = "dose-occ-p10-$i",
                medicationId = medId,
                scheduledEpochMs = nowMs + (i - 150) * 7_200_000L,
                quantity = ScaledQuantity.ofWhole(2),
                unit = StockUnit.DROPS,
                status = DoseStatus.TAKEN
            )
            assertTrue("Dose $i record should succeed", doseResult.isSuccess)
        }

        lots = repository.observeLotsForMedication(medId).first().sortedBy { it.createdAtEpochMs }
        val partiallyConsumedBottle2 = lots.first { it.id == bottle2.id }
        assertEquals("Bottle 2 remaining drops must be 60 drops", 60L * 1000L, partiallyConsumedBottle2.remainingQuantity.rawScaledValue)

        // User marks Bottle 2 finished early:
        val earlyDepletionResult = repository.depletePackage(
            lotId = bottle2.id,
            reason = DepletionReason.EMPTY_NO_DROPS_LEFT,
            userNote = "No more drops dispensing from Bottle 2",
            nowEpochMs = System.currentTimeMillis()
        )
        assertTrue("Early depletion of Bottle 2 should succeed", earlyDepletionResult.isSuccess)
        val depletionOutcome = earlyDepletionResult.getOrThrow()

        // Verify Bottle 2 is EMPTY:
        assertEquals("Bottle 2 must be EMPTY", PackageStatus.EMPTY, depletionOutcome.depletedLot.packageStatus)
        assertEquals("Bottle 2 remaining quantity must be 0", ScaledQuantity.ZERO, depletionOutcome.depletedLot.remainingQuantity)

        // Verify compensating WASTAGE entry of exactly 60 drops:
        assertNotNull("Compensating ledger entry must be created", depletionOutcome.compensatingEntry)
        assertEquals("Compensating entry type must be WASTAGE", MovementType.WASTAGE, depletionOutcome.compensatingEntry?.movementType)
        assertEquals("Wastage quantity must be exactly 60 drops", 60L * 1000L, depletionOutcome.compensatingEntry?.quantity?.rawScaledValue)
        assertTrue("Wastage entry must be marked as estimate", depletionOutcome.compensatingEntry?.isEstimate == true)
        assertNull("Wastage must not have doseOccurrenceId", depletionOutcome.compensatingEntry?.doseOccurrenceId)

        // ZERO DOSE FABRICATION INVARIANT:
        val doseEventsAfterDepletion = repository.observeDosesForMedication(medId).first()
        assertEquals("Dose events count must remain strictly 270 (150 from Bottle 1 + 120 from Bottle 2)", 270, doseEventsAfterDepletion.size)

        // Next bottle staged: Bottle 3 (UNOPENED, 300 drops)
        val bottle3 = lots[2]
        val stagedBottle3 = repository.getStagedNextPackage(medId)
        assertNotNull("Next bottle must be staged", stagedBottle3)
        assertEquals("Staged bottle must be Bottle 3", bottle3.id, stagedBottle3?.id)
        assertEquals("Staged bottle must remain UNOPENED", PackageStatus.UNOPENED, stagedBottle3?.packageStatus)

        // Total usable supply recalculated: 5 unopened bottles x 300 drops = 1,500 drops
        val balanceAfterDepletion = repository.observeStockBalance(med).first()
        assertEquals("Total usable supply must be 1,500 drops", 1500L * 1000L, balanceAfterDepletion.currentBalance.rawScaledValue)

        // --------------------------------------------------------------------------------
        // STEP 5: Accidental Opening Reversal (`revertOpenedPackage`)
        // --------------------------------------------------------------------------------
        // Patient accidentally taps "Unseal / Open" on Bottle 3:
        val openBottle3Result = repository.openPackage(bottle3.id)
        assertTrue("Opening Bottle 3 should succeed", openBottle3Result.isSuccess)
        val openedBottle3 = openBottle3Result.getOrThrow()
        assertEquals("Bottle 3 status is OPENED", PackageStatus.OPENED, openedBottle3.packageStatus)
        assertNotNull("Bottle 3 openedAtEpochMs is set", openedBottle3.openedAtEpochMs)

        // Patient immediately reverts accidental opening:
        val revertResult = repository.revertOpenedPackage(bottle3.id)
        assertTrue("Revert opening Bottle 3 should succeed", revertResult.isSuccess)
        val revertedBottle3 = revertResult.getOrThrow()

        assertEquals("Bottle 3 status restored to UNOPENED", PackageStatus.UNOPENED, revertedBottle3.packageStatus)
        assertNull("Bottle 3 openedAtEpochMs must be cleared", revertedBottle3.openedAtEpochMs)
        assertNull("Bottle 3 discardAfterOpeningEpochMs must be cleared", revertedBottle3.discardAfterOpeningEpochMs)
        assertEquals("Bottle 3 quantity remains 300 drops", 300L * 1000L, revertedBottle3.remainingQuantity.rawScaledValue)

        // --------------------------------------------------------------------------------
        // STEP 6: Partial Consumption Guard on Reversal
        // --------------------------------------------------------------------------------
        // Patient legitimately opens Bottle 3:
        val openLegitResult = repository.openPackage(bottle3.id)
        assertTrue("Legitimate opening should succeed", openLegitResult.isSuccess)

        // Patient takes 1 dose (2 drops deducted):
        val dose271Result = repository.recordDose(
            occurrenceId = "dose-occ-p10-271",
            medicationId = medId,
            scheduledEpochMs = System.currentTimeMillis(),
            quantity = ScaledQuantity.ofWhole(2),
            unit = StockUnit.DROPS,
            status = DoseStatus.TAKEN
        )
        assertTrue("Dose 271 should succeed", dose271Result.isSuccess)

        val bottle3AfterDose = repository.observeLotsForMedication(medId).first().first { it.id == bottle3.id }
        assertEquals("Bottle 3 remaining drops must be 298 drops", 298L * 1000L, bottle3AfterDose.remainingQuantity.rawScaledValue)

        // INVARIANT GUARD: Cannot revert an opened package once any quantity has been consumed!
        val invalidRevertResult = repository.revertOpenedPackage(bottle3.id)
        assertTrue("Revert on partially consumed package must fail", invalidRevertResult.isFailure)
        assertTrue(
            "Failure exception must be IllegalStateException",
            invalidRevertResult.exceptionOrNull() is IllegalStateException
        )

        // --------------------------------------------------------------------------------
        // STEP 7: Accidental Depletion Reversal (`reopenDepletedPackage`)
        // --------------------------------------------------------------------------------
        // User realizes they accidentally marked Bottle 2 depleted:
        val reopenBottle2Result = repository.reopenDepletedPackage(
            lotId = bottle2.id,
            reason = "Found drops remaining in Bottle 2",
            nowEpochMs = System.currentTimeMillis()
        )
        assertTrue("Reopen Bottle 2 should succeed", reopenBottle2Result.isSuccess)
        val reopenOutcome = reopenBottle2Result.getOrThrow()

        assertEquals("Bottle 2 status must be restored to OPENED", PackageStatus.OPENED, reopenOutcome.reopenedLot.packageStatus)
        assertEquals("Bottle 2 remaining drops restored to 60 drops", 60L * 1000L, reopenOutcome.reopenedLot.remainingQuantity.rawScaledValue)
        assertEquals("Reversal entry must be ADJUSTMENT_IN", MovementType.ADJUSTMENT_IN, reopenOutcome.reversalEntry.movementType)
        assertEquals("Reversal quantity must be 60 drops", 60L * 1000L, reopenOutcome.reversalEntry.quantity.rawScaledValue)

        // Total doses recorded remains strictly 271 throughout reopening
        val totalDosesFinal = repository.observeDosesForMedication(medId).first()
        assertEquals("Total doses remains strictly 271", 271, totalDosesFinal.size)
    }

    @Test
    fun packageMetadataUpdate_and_discardPackageWastage_lifecycle() = runBlocking {
        val medId = "med-package-mgmt"
        val med = Medication(
            id = medId,
            name = "Systane Ultra",
            strength = "10ml",
            form = MedicationForm.DROPS,
            stockUnit = StockUnit.DROPS,
            openingStock = ScaledQuantity.ZERO,
            lowStockThreshold = ScaledQuantity.ofWhole(50),
            recordingMode = RecordingMode.CONFIRM,
            packagingConfig = PackagingConfig(
                packageType = PackageType.BOTTLE,
                consumptionUnit = StockUnit.DROPS,
                contentsPerPackage = ScaledQuantity.ofWhole(300),
                discardAfterOpeningDays = 30
            )
        )

        repository.createMedication(
            med,
            ScheduleDefinition(
                id = "sch-package-mgmt",
                medicationId = medId,
                scheduleType = ScheduleType.DAILY,
                targetTimesMinuteOfDay = listOf(8 * 60),
                daysOfWeek = emptySet(),
                doseQuantityPerOccurrence = ScaledQuantity.ofWhole(1),
                startDateEpochDay = 20000L
            )
        )
        repository.restockPackages(
            medicationId = medId,
            packageCount = 2,
            contentsPerPackage = ScaledQuantity.ofWhole(300),
            consumptionUnit = StockUnit.DROPS,
            packageType = PackageType.BOTTLE,
            discardAfterOpeningDays = 30,
            lotNumber = "BATCH-A",
            openFirstPackageImmediately = false
        )

        var lots = repository.observeLotsForMedication(medId).first().sortedBy { it.createdAtEpochMs }
        assertEquals(2, lots.size)
        val pkg1 = lots[0]
        val pkg2 = lots[1]

        // 1. Update metadata of pkg1:
        val updatedExpiry = System.currentTimeMillis() + 90L * 86_400_000L
        val updateResult = repository.updatePackageMetadata(
            lotId = pkg1.id,
            packageLabel = "Bathroom Cabinet Bottle",
            lotNumber = "BATCH-A-REV",
            expiryDateEpochMs = updatedExpiry,
            discardAfterOpeningDays = 45,
            notes = "Stored in cool dry medicine cabinet"
        )
        assertTrue("Metadata update should succeed", updateResult.isSuccess)
        val updatedPkg1 = updateResult.getOrThrow()

        assertEquals("Bathroom Cabinet Bottle", updatedPkg1.packageLabel)
        assertEquals("BATCH-A-REV", updatedPkg1.lotNumber)
        assertEquals(updatedExpiry, updatedPkg1.expiryDateEpochMs)
        assertEquals(45, updatedPkg1.discardAfterOpeningDays)
        assertEquals("Stored in cool dry medicine cabinet", updatedPkg1.notes)

        // 2. Discard unopened pkg2 (e.g. dropped and shattered):
        val discardResult = repository.discardPackage(
            lotId = pkg2.id,
            reason = "Bottle shattered on tiled floor",
            nowEpochMs = System.currentTimeMillis()
        )
        assertTrue("Discarding pkg2 should succeed", discardResult.isSuccess)
        val discardedPkg2 = discardResult.getOrThrow()

        assertEquals(PackageStatus.DISCARDED, discardedPkg2.packageStatus)
        assertEquals(ScaledQuantity.ZERO, discardedPkg2.remainingQuantity)

        // Verify WASTAGE ledger entry for 300 drops:
        val movements = repository.observeMovementsForMedication(medId).first()
        val wastageMovement = movements.firstOrNull { it.movementType == MovementType.WASTAGE }
        assertNotNull("Wastage movement must exist for discarded package", wastageMovement)
        assertEquals(300L * 1000L, wastageMovement?.quantity?.rawScaledValue)
        assertEquals("Bottle shattered on tiled floor", wastageMovement?.reason)

        // Total usable balance now only 300 drops (from pkg1):
        val balance = repository.observeStockBalance(med).first()
        assertEquals(300L * 1000L, balance.currentBalance.rawScaledValue)
    }
}
