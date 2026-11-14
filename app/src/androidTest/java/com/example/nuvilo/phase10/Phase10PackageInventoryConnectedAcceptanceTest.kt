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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

/**
 * Phase 10 Connected Instrumentation Test on Android Runtime:
 * Demonstrates the full Hydro Forte 7-bottle physical purchasing, opening, isolated consumption,
 * explicit bottle opening confirmation, early depletion, and reversal workflow against an in-memory
 * Room SQLite database on device.
 */
@RunWith(AndroidJUnit4::class)
class Phase10PackageInventoryConnectedAcceptanceTest {

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
    fun hydroForte7BottlePhysicalInventoryEndToEndScenario() = runBlocking {
        val medId = "med-hydro-phase10-connected"
        val med = Medication(
            id = medId,
            name = "Hydro Forte",
            strength = "0.3%",
            form = MedicationForm.DROPS,
            stockUnit = StockUnit.DROPS,
            openingStock = ScaledQuantity.ZERO,
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
            id = "sch-hydro-phase10-connected",
            medicationId = medId,
            scheduleType = ScheduleType.DAILY,
            targetTimesMinuteOfDay = listOf(8 * 60, 11 * 60, 14 * 60, 17 * 60, 20 * 60), // 5 times daily
            daysOfWeek = emptySet(),
            doseQuantityPerOccurrence = ScaledQuantity.ofWhole(2), // 2 drops per administration
            startDateEpochDay = 20000L
        )

        repository.createMedication(med, schedule)

        // Step 1: Restock 7 bottles with openFirstPackageImmediately = true
        val restockResult = repository.restockPackages(
            medicationId = medId,
            packageCount = 7,
            contentsPerPackage = ScaledQuantity.ofWhole(300),
            consumptionUnit = StockUnit.DROPS,
            packageType = PackageType.BOTTLE,
            discardAfterOpeningDays = 28,
            lotNumber = "BATCH-HF-7",
            expiryDateEpochMs = System.currentTimeMillis() + 365L * 86_400_000L,
            notes = "Initial 7 bottles prescription delivery",
            openFirstPackageImmediately = true
        )
        assertTrue("Restock of 7 bottles must succeed", restockResult.isSuccess)

        // Observe initial lots and balance
        val initialLots = repository.observeLotsForMedication(medId).first()
        assertEquals(7, initialLots.size)
        val openedLots = initialLots.filter { it.packageStatus == PackageStatus.OPENED }
        val unopenedLots = initialLots.filter { it.packageStatus == PackageStatus.UNOPENED }
        assertEquals("Exactly 1 opened bottle initially", 1, openedLots.size)
        assertEquals("Exactly 6 unopened bottles initially", 6, unopenedLots.size)
        assertEquals(ScaledQuantity.ofWhole(300), openedLots.first().remainingQuantity)

        val initialBalance = repository.observeStockBalance(med).first()
        assertEquals(ScaledQuantity.ofWhole(2100), initialBalance.currentBalance)

        // Step 2: Record 12 administrations (24 drops consumed)
        val baseTime = System.currentTimeMillis()
        for (i in 1..12) {
            repository.recordDose(
                occurrenceId = "dose-phase10-$i",
                medicationId = medId,
                scheduledEpochMs = baseTime + (i * 3600_000L),
                quantity = ScaledQuantity.ofWhole(2),
                unit = StockUnit.DROPS,
                status = DoseStatus.TAKEN,
                recordedEpochMs = baseTime + (i * 3600_000L),
                notes = "Dose $i taken"
            )
        }

        val lotsAfter12 = repository.observeLotsForMedication(medId).first()
        val bottle1After12 = lotsAfter12.first { it.id == openedLots.first().id }
        assertEquals(ScaledQuantity.ofWhole(276), bottle1After12.remainingQuantity)
        assertEquals(PackageStatus.OPENED, bottle1After12.packageStatus)

        // Check that all 6 unopened bottles remain 100% untouched
        val unopenedAfter12 = lotsAfter12.filter { it.packageStatus == PackageStatus.UNOPENED }
        assertEquals(6, unopenedAfter12.size)
        for (lot in unopenedAfter12) {
            assertEquals(ScaledQuantity.ofWhole(300), lot.remainingQuantity)
        }

        // Step 3: Record remaining 138 administrations from Bottle #1 (total 150 doses = 300 drops)
        for (i in 13..150) {
            repository.recordDose(
                occurrenceId = "dose-phase10-$i",
                medicationId = medId,
                scheduledEpochMs = baseTime + (i * 3600_000L),
                quantity = ScaledQuantity.ofWhole(2),
                unit = StockUnit.DROPS,
                status = DoseStatus.TAKEN,
                recordedEpochMs = baseTime + (i * 3600_000L),
                notes = "Dose $i taken"
            )
        }

        val lotsAfter150 = repository.observeLotsForMedication(medId).first()
        val bottle1Exhausted = lotsAfter150.first { it.id == openedLots.first().id }
        assertEquals(ScaledQuantity.ZERO, bottle1Exhausted.remainingQuantity)
        assertEquals(PackageStatus.EMPTY, bottle1Exhausted.packageStatus)

        // Invariant: Unopened bottles MUST NOT have been auto-opened!
        val unopenedAfter150 = lotsAfter150.filter { it.packageStatus == PackageStatus.UNOPENED }
        assertEquals("All 6 bottles must remain UNOPENED", 6, unopenedAfter150.size)

        // Step 4: Next bottle staged and confirmed open
        val stagedNext = stockCalculator.findStagedNextPackage(
            lotsAfter150,
            System.currentTimeMillis()
        )
        assertNotNull("Next unopened bottle must be staged", stagedNext)
        assertEquals(PackageStatus.UNOPENED, stagedNext?.packageStatus)

        val openBottle2Result = repository.openPackage(stagedNext!!.id)
        assertTrue(openBottle2Result.isSuccess)
        val openedBottle2 = openBottle2Result.getOrThrow()
        assertEquals(PackageStatus.OPENED, openedBottle2.packageStatus)
        assertEquals(ScaledQuantity.ofWhole(300), openedBottle2.remainingQuantity)

        val lotsAfterOpenBottle2 = repository.observeLotsForMedication(medId).first()
        assertEquals(1, lotsAfterOpenBottle2.count { it.packageStatus == PackageStatus.OPENED })
        assertEquals(5, lotsAfterOpenBottle2.count { it.packageStatus == PackageStatus.UNOPENED })

        // Step 5: Consume 120 administrations from Bottle #2 (240 drops) -> 60 drops left
        for (i in 151..270) {
            repository.recordDose(
                occurrenceId = "dose-phase10-$i",
                medicationId = medId,
                scheduledEpochMs = baseTime + (i * 3600_000L),
                quantity = ScaledQuantity.ofWhole(2),
                unit = StockUnit.DROPS,
                status = DoseStatus.TAKEN,
                recordedEpochMs = baseTime + (i * 3600_000L),
                notes = "Dose $i taken"
            )
        }

        val lotsAfter270 = repository.observeLotsForMedication(medId).first()
        val bottle2 = lotsAfter270.first { it.id == openedBottle2.id }
        assertEquals(ScaledQuantity.ofWhole(60), bottle2.remainingQuantity)

        // Step 6: Mark Bottle #2 finished early with 60 drops remaining
        val depletionResult = repository.depletePackage(
            lotId = bottle2.id,
            reason = DepletionReason.EXCESSIVE_DISPENSING,
            userNote = "Dropper tip damaged, remaining drops discarded"
        )
        assertTrue(depletionResult.isSuccess)
        val depletionOutcome = depletionResult.getOrThrow()
        assertEquals(PackageStatus.EMPTY, depletionOutcome.depletedLot.packageStatus)
        assertEquals(ScaledQuantity.ZERO, depletionOutcome.depletedLot.remainingQuantity)
        assertNotNull(depletionOutcome.compensatingEntry)
        assertEquals(MovementType.WASTAGE, depletionOutcome.compensatingEntry?.movementType)
        assertEquals(ScaledQuantity.ofWhole(60), depletionOutcome.compensatingEntry?.quantity)

        // Verify adherence records remain exactly 270 (zero fabricated doses)
        val allDoses = repository.observeDosesForMedication(medId).first()
        assertEquals("Dose events table must remain strictly at 270", 270, allDoses.size)

        // Step 7: Accidental Depletion Reversal
        val reversalResult = repository.reopenDepletedPackage(bottle2.id)
        assertTrue(reversalResult.isSuccess)
        val restoredLot = reversalResult.getOrThrow().reopenedLot
        assertEquals(PackageStatus.OPENED, restoredLot.packageStatus)
        assertEquals(ScaledQuantity.ofWhole(60), restoredLot.remainingQuantity)

        val finalLots = repository.observeLotsForMedication(medId).first()
        val finalBottle2 = finalLots.first { it.id == bottle2.id }
        assertEquals(PackageStatus.OPENED, finalBottle2.packageStatus)
        assertEquals(ScaledQuantity.ofWhole(60), finalBottle2.remainingQuantity)
    }
}
