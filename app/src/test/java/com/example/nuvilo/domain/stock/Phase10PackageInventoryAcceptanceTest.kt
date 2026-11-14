package com.example.nuvilo.domain.stock

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
import com.example.nuvilo.domain.model.StockLot
import com.example.nuvilo.domain.model.StockUnit
import com.example.nuvilo.domain.model.TrackingPrecision
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.UUID

/**
 * Phase 10 Master Acceptance Test:
 * Demonstrates the complete 7-bottle Hydro Forte physical package lifecycle,
 * opened/unopened stock tracking, isolated consumption, explicit next-bottle confirmation,
 * early package depletion, and accurate supply forecasting.
 */
class Phase10PackageInventoryAcceptanceTest {

    private val calculator = StockLedgerCalculator()

    private lateinit var hydroForte: Medication
    private lateinit var dailySchedule: ScheduleDefinition

    @Before
    fun setUp() {
        hydroForte = Medication(
            id = "med-hydro-phase10",
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

        dailySchedule = ScheduleDefinition(
            id = "sch-hydro-phase10",
            medicationId = hydroForte.id,
            scheduleType = ScheduleType.DAILY,
            targetTimesMinuteOfDay = listOf(8 * 60, 11 * 60, 14 * 60, 17 * 60, 20 * 60), // 5 times daily
            daysOfWeek = emptySet(),
            doseQuantityPerOccurrence = ScaledQuantity.ofWhole(2), // 2 drops per administration
            startDateEpochDay = 20000L
        )
    }

    @Test
    fun completeEndToEnd7BottleAcceptanceScenario() {
        val nowMs = 1731600000000L // Baseline epoch

        // 1. Initial Purchase: 7 bottles received, user confirms opening Bottle #1 immediately
        val bottleCapacity = ScaledQuantity.ofWhole(300)
        val initialPackages = mutableListOf<StockLot>()
        for (i in 1..7) {
            val isOpenFirst = (i == 1)
            initialPackages.add(
                StockLot(
                    id = "bottle-$i",
                    medicationId = hydroForte.id,
                    lotNumber = "LOT-HF-2026",
                    initialQuantity = bottleCapacity,
                    remainingQuantity = bottleCapacity,
                    unit = StockUnit.DROPS,
                    expiryDateEpochMs = nowMs + 365L * 86_400_000L,
                    createdAtEpochMs = nowMs + i,
                    packageType = PackageType.BOTTLE,
                    packageLabel = "Bottle #$i",
                    packageStatus = if (isOpenFirst) PackageStatus.OPENED else PackageStatus.UNOPENED,
                    openedAtEpochMs = if (isOpenFirst) nowMs else null,
                    discardAfterOpeningDays = 28,
                    discardAfterOpeningEpochMs = if (isOpenFirst) nowMs + 28L * 86_400_000L else null
                )
            )
        }

        // Verify Step 1: Initial inventory breakdown
        val initialOpened = initialPackages.filter { it.packageStatus == PackageStatus.OPENED }
        val initialUnopened = initialPackages.filter { it.packageStatus == PackageStatus.UNOPENED }
        val initialPhysicalCount = initialPackages.count { it.packageStatus == PackageStatus.OPENED || it.packageStatus == PackageStatus.UNOPENED }

        assertEquals(1, initialOpened.size)
        assertEquals(6, initialUnopened.size)
        assertEquals(7, initialPhysicalCount)
        assertEquals(ScaledQuantity.ofWhole(300), initialOpened.first().remainingQuantity)

        // Daily consumption = 2 drops × 5 administrations = 10 drops/day
        val dailyConsumption = ScaledQuantity.ofWhole(10)
        val totalUsableDrops = ScaledQuantity.ofWhole(2100)
        val initialDaysSupply = (totalUsableDrops.rawScaledValue / dailyConsumption.rawScaledValue).toInt()
        assertEquals(210, initialDaysSupply)

        // 2. After 12 administrations (24 drops consumed):
        // Doses must deduct strictly from Bottle #1. Unopened bottles must remain 100% untouched.
        var currentLots = initialPackages.toList()
        for (doseIndex in 1..12) {
            val allocations = calculator.allocateDoseAgainstPackages(
                doseQuantity = ScaledQuantity.ofWhole(2),
                lots = currentLots,
                nowEpochMs = nowMs + (doseIndex * 3600_000L)
            )
            assertEquals(1, allocations.size)
            assertEquals("bottle-1", allocations.first().updatedLot.id)

            currentLots = currentLots.map { lot ->
                if (lot.id == allocations.first().updatedLot.id) allocations.first().updatedLot else lot
            }
        }

        // Verify Step 2:
        val bottle1After12 = currentLots.first { it.id == "bottle-1" }
        assertEquals(ScaledQuantity.ofWhole(276), bottle1After12.remainingQuantity)
        assertEquals(PackageStatus.OPENED, bottle1After12.packageStatus)
        // Verify all 6 unopened bottles remain completely untouched at 300 drops
        for (i in 2..7) {
            val unopenedBottle = currentLots.first { it.id == "bottle-$i" }
            assertEquals(ScaledQuantity.ofWhole(300), unopenedBottle.remainingQuantity)
            assertEquals(PackageStatus.UNOPENED, unopenedBottle.packageStatus)
        }
        val physicalCountAfter12 = currentLots.count { it.packageStatus == PackageStatus.OPENED || it.packageStatus == PackageStatus.UNOPENED }
        assertEquals(7, physicalCountAfter12)

        // 3. After another 138 administrations (150 total administrations = 300 drops consumed):
        // Bottle #1 has 276 drops left; 138 doses × 2 drops = 276 drops consumed.
        // Bottle #1 becomes exhausted (0 drops remaining, status = EMPTY).
        for (doseIndex in 13..150) {
            val allocations = calculator.allocateDoseAgainstPackages(
                doseQuantity = ScaledQuantity.ofWhole(2),
                lots = currentLots,
                nowEpochMs = nowMs + (doseIndex * 3600_000L)
            )
            currentLots = currentLots.map { lot ->
                val matching = allocations.find { it.updatedLot.id == lot.id }
                matching?.updatedLot ?: lot
            }
        }

        val bottle1Exhausted = currentLots.first { it.id == "bottle-1" }
        assertEquals(ScaledQuantity.ZERO, bottle1Exhausted.remainingQuantity)
        assertEquals(PackageStatus.EMPTY, bottle1Exhausted.packageStatus)

        // INVARIANT CHECK: Unopened bottles MUST NOT have been auto-opened!
        for (i in 2..7) {
            val unopenedBottle = currentLots.first { it.id == "bottle-$i" }
            assertEquals("Bottle $i must remain UNOPENED", PackageStatus.UNOPENED, unopenedBottle.packageStatus)
            assertEquals(ScaledQuantity.ofWhole(300), unopenedBottle.remainingQuantity)
        }

        // Check staged next package
        val stagedNext = calculator.findStagedNextPackage(currentLots, nowMs + 150 * 3600_000L)
        assertNotNull(stagedNext)
        assertEquals("bottle-2", stagedNext?.id)
        assertEquals(PackageStatus.UNOPENED, stagedNext?.packageStatus)

        // 4. User confirms physically opening Bottle #2
        val openEpoch = nowMs + 151 * 3600_000L
        val openedBottle2 = calculator.openPackage(stagedNext!!, openEpoch)
        assertEquals(PackageStatus.OPENED, openedBottle2.packageStatus)
        assertEquals(ScaledQuantity.ofWhole(300), openedBottle2.remainingQuantity)
        assertEquals(openEpoch, openedBottle2.openedAtEpochMs)

        currentLots = currentLots.map { if (it.id == "bottle-2") openedBottle2 else it }

        // Inventory now: Bottle #2 is OPENED (300 drops), Bottles #3..#7 are UNOPENED (5 bottles), Bottle #1 is EMPTY
        val remainingPhysicalCount = currentLots.count { it.packageStatus == PackageStatus.OPENED || it.packageStatus == PackageStatus.UNOPENED }
        assertEquals(6, remainingPhysicalCount)
        assertEquals(5, currentLots.count { it.packageStatus == PackageStatus.UNOPENED })

        // 5. Consume 120 administrations (240 drops) from Bottle #2 -> 60 drops remaining
        for (doseIndex in 1..120) {
            val allocations = calculator.allocateDoseAgainstPackages(
                doseQuantity = ScaledQuantity.ofWhole(2),
                lots = currentLots,
                nowEpochMs = openEpoch + (doseIndex * 3600_000L)
            )
            assertEquals(1, allocations.size)
            assertEquals("bottle-2", allocations.first().updatedLot.id)
            currentLots = currentLots.map { lot ->
                if (lot.id == allocations.first().updatedLot.id) allocations.first().updatedLot else lot
            }
        }

        val bottle2BeforeDepletion = currentLots.first { it.id == "bottle-2" }
        assertEquals(ScaledQuantity.ofWhole(60), bottle2BeforeDepletion.remainingQuantity)
        assertEquals(PackageStatus.OPENED, bottle2BeforeDepletion.packageStatus)

        // 6. User marks Bottle #2 finished early with 60 drops remaining (accidental spillage / dropper damage)
        val depletionEpoch = openEpoch + 121 * 3600_000L
        val depletionOutcome = calculator.depletePackageEarly(
            lot = bottle2BeforeDepletion,
            reason = DepletionReason.EXCESSIVE_DISPENSING,
            userNote = "Dropper tip damaged, remaining 60 drops discarded",
            availableLots = currentLots,
            nowEpochMs = depletionEpoch
        )

        // Verify accounting: 60 drops written off as WASTAGE; zero dose events created
        val depletedLot = depletionOutcome.depletedLot
        assertEquals(PackageStatus.EMPTY, depletedLot.packageStatus)
        assertEquals(ScaledQuantity.ZERO, depletedLot.remainingQuantity)
        assertNotNull(depletionOutcome.compensatingEntry)
        assertEquals(MovementType.WASTAGE, depletionOutcome.compensatingEntry?.movementType)
        assertEquals(ScaledQuantity.ofWhole(60), depletionOutcome.compensatingEntry?.quantity)
        assertNull("Depletion movement must NOT reference any dose occurrence", depletionOutcome.compensatingEntry?.doseOccurrenceId)
        assertTrue(depletionOutcome.compensatingEntry!!.isEstimate)

        // Verify next bottle staged
        assertNotNull(depletionOutcome.stagedNextLot)
        assertEquals("bottle-3", depletionOutcome.stagedNextLot?.id)
        assertEquals(PackageStatus.UNOPENED, depletionOutcome.stagedNextLot?.packageStatus)

        currentLots = currentLots.map { if (it.id == "bottle-2") depletedLot else it }

        // 7. Verify Accidental Depletion Reversal:
        // User realizes Bottle #2 was not actually finished and reverts the depletion
        val reversalOutcome = calculator.reopenDepletedPackage(
            lot = depletedLot,
            depletionLedgerEntry = depletionOutcome.compensatingEntry,
            nowEpochMs = depletionEpoch + 60_000L
        )

        val restoredBottle2 = reversalOutcome.reopenedLot
        assertEquals(PackageStatus.OPENED, restoredBottle2.packageStatus)
        assertEquals(ScaledQuantity.ofWhole(60), restoredBottle2.remainingQuantity)
        assertEquals(MovementType.ADJUSTMENT_IN, reversalOutcome.reversalEntry.movementType)
        assertEquals(ScaledQuantity.ofWhole(60), reversalOutcome.reversalEntry.quantity)

        currentLots = currentLots.map { if (it.id == "bottle-2") restoredBottle2 else it }
        val finalOpenLot = currentLots.first { it.id == "bottle-2" }
        assertEquals(ScaledQuantity.ofWhole(60), finalOpenLot.remainingQuantity)
    }

    @Test
    fun testRevertAccidentallyOpenedPackage() {
        val nowMs = System.currentTimeMillis()
        val unopened = StockLot(
            id = "bottle-revert",
            medicationId = hydroForte.id,
            initialQuantity = ScaledQuantity.ofWhole(300),
            remainingQuantity = ScaledQuantity.ofWhole(300),
            unit = StockUnit.DROPS,
            packageType = PackageType.BOTTLE,
            packageStatus = PackageStatus.UNOPENED
        )

        // User opens bottle by mistake
        val opened = calculator.openPackage(unopened, nowMs)
        assertEquals(PackageStatus.OPENED, opened.packageStatus)
        assertEquals(nowMs, opened.openedAtEpochMs)

        // User reverts the accidental opening
        val reverted = calculator.revertOpenedPackage(opened)
        assertEquals(PackageStatus.UNOPENED, reverted.packageStatus)
        assertNull(reverted.openedAtEpochMs)
        assertNull(reverted.discardAfterOpeningEpochMs)
    }
}
