package com.example.nuvilo.domain.stock

import com.example.nuvilo.domain.model.PackageStatus
import com.example.nuvilo.domain.model.PackageType
import com.example.nuvilo.domain.model.ScaledQuantity
import com.example.nuvilo.domain.model.StockLot
import com.example.nuvilo.domain.model.StockUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class PackageRestockAndRevertTest {

    private val calculator = StockLedgerCalculator()
    private val dayMs = 86_400_000L
    private val nowMs = 100L * dayMs

    private fun sampleLot(
        id: String,
        initial: Long = 300,
        remaining: Long = 300,
        status: PackageStatus = PackageStatus.UNOPENED,
        openedAt: Long? = null,
        discardDays: Int? = 28
    ) = StockLot(
        id = id,
        medicationId = "med-1",
        lotNumber = "LOT-1",
        initialQuantity = ScaledQuantity.ofWhole(initial),
        remainingQuantity = ScaledQuantity.ofWhole(remaining),
        unit = StockUnit.DROPS,
        packageType = PackageType.BOTTLE,
        packageLabel = "Bottle $id",
        packageStatus = status,
        openedAtEpochMs = openedAt,
        discardAfterOpeningDays = discardDays,
        discardAfterOpeningEpochMs = openedAt?.let { it + (discardDays ?: 28).toLong() * dayMs }
    )

    @Test
    fun openPackage_stampsOpenedAtAndDiscardWindow() {
        val sealed = sampleLot("b1", status = PackageStatus.UNOPENED)

        val opened = calculator.openPackage(sealed, nowEpochMs = nowMs)

        assertEquals(PackageStatus.OPENED, opened.packageStatus)
        assertEquals(nowMs, opened.openedAtEpochMs)
        assertEquals(nowMs + 28L * dayMs, opened.discardAfterOpeningEpochMs)
    }

    @Test
    fun revertOpenedPackage_revertsUnconsumedBottleToUnopened() {
        val opened = sampleLot(
            id = "b1",
            initial = 300,
            remaining = 300,
            status = PackageStatus.OPENED,
            openedAt = nowMs
        )

        val reverted = calculator.revertOpenedPackage(opened)

        assertEquals(PackageStatus.UNOPENED, reverted.packageStatus)
        assertNull(reverted.openedAtEpochMs)
        assertNull(reverted.discardAfterOpeningEpochMs)
        assertEquals(ScaledQuantity.ofWhole(300), reverted.remainingQuantity)
    }

    @Test
    fun revertOpenedPackage_failsWhenPartiallyConsumed() {
        val consumed = sampleLot(
            id = "b1",
            initial = 300,
            remaining = 280, // 20 drops taken!
            status = PackageStatus.OPENED,
            openedAt = nowMs
        )

        val error = assertThrows(IllegalStateException::class.java) {
            calculator.revertOpenedPackage(consumed)
        }
        assertEquals(
            "Cannot revert package that has already been partially consumed (280 of 300 remaining)",
            error.message
        )
    }

    @Test
    fun revertOpenedPackage_failsWhenNotOpened() {
        val unopened = sampleLot("b1", status = PackageStatus.UNOPENED)
        val empty = sampleLot("b1", remaining = 0, status = PackageStatus.EMPTY)

        assertThrows(IllegalStateException::class.java) {
            calculator.revertOpenedPackage(unopened)
        }
        assertThrows(IllegalStateException::class.java) {
            calculator.revertOpenedPackage(empty)
        }
    }
}
