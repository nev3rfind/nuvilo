package com.example.nuvilo.domain.stock

import com.example.nuvilo.domain.model.MovementType
import com.example.nuvilo.domain.model.PackageStatus
import com.example.nuvilo.domain.model.PackageType
import com.example.nuvilo.domain.model.ScaledQuantity
import com.example.nuvilo.domain.model.StockLedgerEntry
import com.example.nuvilo.domain.model.StockLot
import com.example.nuvilo.domain.model.StockUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class PackageLifecycleAndFefoTest {

    private val calculator = StockLedgerCalculator()
    private val dayMs = 86_400_000L
    private val nowMs = 100L * dayMs

    private fun bottleLot(
        id: String,
        remainingDrops: Long,
        initialDrops: Long = 300,
        status: PackageStatus = PackageStatus.UNOPENED,
        expiryEpochMs: Long? = nowMs + 180L * dayMs,
        createdAtEpochMs: Long = nowMs - 10L * dayMs,
        openedAtEpochMs: Long? = null,
        discardAfterOpeningDays: Int? = 28,
        discardAfterOpeningEpochMs: Long? = null
    ) = StockLot(
        id = id,
        medicationId = "med-eye",
        lotNumber = "LOT-$id",
        initialQuantity = ScaledQuantity.ofWhole(initialDrops),
        remainingQuantity = ScaledQuantity.ofWhole(remainingDrops),
        unit = StockUnit.DROPS,
        expiryDateEpochMs = expiryEpochMs,
        createdAtEpochMs = createdAtEpochMs,
        packageType = PackageType.BOTTLE,
        packageLabel = "Bottle $id",
        packageStatus = status,
        openedAtEpochMs = openedAtEpochMs,
        discardAfterOpeningDays = discardAfterOpeningDays,
        discardAfterOpeningEpochMs = discardAfterOpeningEpochMs
    )

    @Test
    fun allocateAgainstPackages_usesOpenedBottleBeforeUnopenedFefoBottle() {
        // Bottle 2 expires sooner (90 days), but Bottle 1 is ALREADY opened (expires 150 days, discard in 14 days).
        // Must finish opened Bottle 1 before opening Bottle 2!
        val b1 = bottleLot(
            id = "b1",
            remainingDrops = 10,
            status = PackageStatus.OPENED,
            expiryEpochMs = nowMs + 150L * dayMs,
            openedAtEpochMs = nowMs - 14L * dayMs,
            discardAfterOpeningEpochMs = nowMs + 14L * dayMs
        )
        val b2 = bottleLot(
            id = "b2",
            remainingDrops = 300,
            status = PackageStatus.UNOPENED,
            expiryEpochMs = nowMs + 90L * dayMs
        )

        val allocations = calculator.allocateDoseAgainstPackages(
            doseQuantity = ScaledQuantity.ofWhole(2),
            lots = listOf(b2, b1),
            nowEpochMs = nowMs
        )

        assertEquals(1, allocations.size)
        assertEquals("b1", allocations[0].updatedLot.id)
        assertEquals(ScaledQuantity.ofWhole(8), allocations[0].updatedLot.remainingQuantity)
        assertEquals(PackageStatus.OPENED, allocations[0].updatedLot.packageStatus)
        assertEquals(false, allocations[0].newlyOpened)
    }

    @Test
    fun allocateAgainstPackages_whenOpenedExhausted_opensNextFefoBottleAndStampsDiscardWindow() {
        val b1 = bottleLot(
            id = "b1",
            remainingDrops = 1,
            status = PackageStatus.OPENED,
            openedAtEpochMs = nowMs - 20L * dayMs,
            discardAfterOpeningEpochMs = nowMs + 8L * dayMs
        )
        val b2FefoFirst = bottleLot(
            id = "b2",
            remainingDrops = 300,
            status = PackageStatus.UNOPENED,
            expiryEpochMs = nowMs + 60L * dayMs,
            discardAfterOpeningDays = 28
        )
        val b3Later = bottleLot(
            id = "b3",
            remainingDrops = 300,
            status = PackageStatus.UNOPENED,
            expiryEpochMs = nowMs + 300L * dayMs,
            discardAfterOpeningDays = 28
        )

        // Need 2 drops: takes remaining 1 drop from b1 (now EMPTY) and opens b2 taking 1 drop (299 left)
        val allocations = calculator.allocateDoseAgainstPackages(
            doseQuantity = ScaledQuantity.ofWhole(2),
            lots = listOf(b3Later, b1, b2FefoFirst),
            nowEpochMs = nowMs
        )

        assertEquals(2, allocations.size)

        val first = allocations[0]
        assertEquals("b1", first.updatedLot.id)
        assertEquals(ScaledQuantity.ZERO, first.updatedLot.remainingQuantity)
        assertEquals(PackageStatus.EMPTY, first.updatedLot.packageStatus)

        val second = allocations[1]
        assertEquals("b2", second.updatedLot.id)
        assertEquals(ScaledQuantity.ofWhole(299), second.updatedLot.remainingQuantity)
        assertEquals(PackageStatus.OPENED, second.updatedLot.packageStatus)
        assertEquals(true, second.newlyOpened)
        assertEquals(nowMs, second.updatedLot.openedAtEpochMs)
        assertEquals(nowMs + 28L * dayMs, second.updatedLot.discardAfterOpeningEpochMs)
    }

    @Test
    fun allocateAgainstPackages_skipsExpiredAndDiscardExpiredAndDiscardedLots() {
        val expiredMfg = bottleLot(
            id = "expired-mfg",
            remainingDrops = 100,
            status = PackageStatus.UNOPENED,
            expiryEpochMs = nowMs - dayMs
        )
        val pastDiscardWindow = bottleLot(
            id = "past-discard",
            remainingDrops = 200,
            status = PackageStatus.OPENED,
            expiryEpochMs = nowMs + 180L * dayMs,
            openedAtEpochMs = nowMs - 30L * dayMs,
            discardAfterOpeningEpochMs = nowMs - 2L * dayMs
        )
        val manuallyDiscarded = bottleLot(
            id = "discarded",
            remainingDrops = 150,
            status = PackageStatus.DISCARDED
        )
        val goodSealed = bottleLot(
            id = "good",
            remainingDrops = 300,
            status = PackageStatus.UNOPENED,
            expiryEpochMs = nowMs + 120L * dayMs
        )

        val allocations = calculator.allocateDoseAgainstPackages(
            doseQuantity = ScaledQuantity.ofWhole(2),
            lots = listOf(expiredMfg, pastDiscardWindow, manuallyDiscarded, goodSealed),
            nowEpochMs = nowMs
        )

        assertEquals(1, allocations.size)
        assertEquals("good", allocations[0].updatedLot.id)
        assertEquals(ScaledQuantity.ofWhole(298), allocations[0].updatedLot.remainingQuantity)
        assertTrue(allocations[0].newlyOpened)
    }

    @Test
    fun openPackage_isIdempotentIfAlreadyOpened() {
        val alreadyOpen = bottleLot(
            id = "b1",
            remainingDrops = 250,
            status = PackageStatus.OPENED,
            openedAtEpochMs = nowMs - 5L * dayMs,
            discardAfterOpeningDays = 28,
            discardAfterOpeningEpochMs = nowMs + 23L * dayMs
        )

        val result = calculator.openPackage(alreadyOpen, nowEpochMs = nowMs)

        // Preserves original openedAt and discardAfterOpening timestamps
        assertEquals(PackageStatus.OPENED, result.packageStatus)
        assertEquals(nowMs - 5L * dayMs, result.openedAtEpochMs)
        assertEquals(nowMs + 23L * dayMs, result.discardAfterOpeningEpochMs)
    }

    @Test
    fun openPackage_rejectsExpiredOrDiscardedOrEmpty() {
        assertThrows(IllegalStateException::class.java) {
            calculator.openPackage(bottleLot("e1", 0, status = PackageStatus.EMPTY), nowMs)
        }
        assertThrows(IllegalStateException::class.java) {
            calculator.openPackage(bottleLot("d1", 100, status = PackageStatus.DISCARDED), nowMs)
        }
        assertThrows(IllegalStateException::class.java) {
            calculator.openPackage(bottleLot("x1", 100, expiryEpochMs = nowMs - 1000L), nowMs)
        }
    }

    @Test
    fun discardPackage_producesExactWastageLedgerEntryAndZeroesRemaining() {
        val openBottle = bottleLot(
            id = "b1",
            remainingDrops = 42,
            status = PackageStatus.OPENED,
            openedAtEpochMs = nowMs - 29L * dayMs,
            discardAfterOpeningEpochMs = nowMs - dayMs
        )

        val outcome = calculator.discardPackage(
            lot = openBottle,
            reason = "28-day post-opening limit reached",
            nowEpochMs = nowMs
        )

        assertEquals(PackageStatus.DISCARDED, outcome.updatedLot.packageStatus)
        assertEquals(ScaledQuantity.ZERO, outcome.updatedLot.remainingQuantity)
        assertEquals(nowMs, outcome.updatedLot.discardedAtEpochMs)
        assertEquals("28-day post-opening limit reached", outcome.updatedLot.discardReason)

        assertNotNull(outcome.wastageEntry)
        assertEquals(MovementType.WASTAGE, outcome.wastageEntry!!.movementType)
        assertEquals(ScaledQuantity.ofWhole(42), outcome.wastageEntry!!.quantity)
        assertEquals("b1", outcome.wastageEntry!!.lotId)
    }

    @Test
    fun recountOpenedPackage_adjustsOnlyTargetOpenedPackageAndPreservesUnopened() {
        val openBottle = bottleLot(
            id = "b1",
            remainingDrops = 120,
            initialDrops = 300,
            status = PackageStatus.OPENED,
            openedAtEpochMs = nowMs - 10L * dayMs
        )
        val sealedBottle = bottleLot(
            id = "b2",
            remainingDrops = 300,
            initialDrops = 300,
            status = PackageStatus.UNOPENED
        )

        val recountOut = calculator.recountPackage(
            lot = openBottle,
            observedRemaining = ScaledQuantity.ofWhole(95),
            reason = "Visual check of opened bottle",
            nowEpochMs = nowMs
        )

        assertEquals(ScaledQuantity.ofWhole(95), recountOut.updatedLot.remainingQuantity)
        assertEquals(PackageStatus.OPENED, recountOut.updatedLot.packageStatus)
        assertEquals(MovementType.ADJUSTMENT_OUT, recountOut.ledgerEntry!!.movementType)
        assertEquals(ScaledQuantity.ofWhole(25), recountOut.ledgerEntry!!.quantity)
        assertEquals("b1", recountOut.ledgerEntry!!.lotId)

        // Recounting opened bottle down to 0 marks only b1 EMPTY
        val emptyRecount = calculator.recountPackage(
            lot = openBottle,
            observedRemaining = ScaledQuantity.ZERO,
            reason = "Bottle ran dry early",
            nowEpochMs = nowMs
        )
        assertEquals(PackageStatus.EMPTY, emptyRecount.updatedLot.packageStatus)
        assertEquals(ScaledQuantity.ZERO, emptyRecount.updatedLot.remainingQuantity)
        assertEquals(ScaledQuantity.ofWhole(300), sealedBottle.remainingQuantity)
        assertNull(sealedBottle.openedAtEpochMs)
    }
}
