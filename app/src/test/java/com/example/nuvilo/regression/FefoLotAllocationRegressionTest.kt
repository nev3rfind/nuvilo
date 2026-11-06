package com.example.nuvilo.regression

import com.example.nuvilo.domain.model.ScaledQuantity
import com.example.nuvilo.domain.model.StockLot
import com.example.nuvilo.domain.model.StockUnit
import com.example.nuvilo.domain.stock.StockLedgerCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Regression Test Suite: Category A (Domain Models & Accounting)
 * Tests T023 to T030: First-Expired First-Out (FEFO) Lot Allocation Logic
 */
class FefoLotAllocationRegressionTest {

    private lateinit var calculator: StockLedgerCalculator

    @Before
    fun setUp() {
        calculator = StockLedgerCalculator()
    }

    private fun createLot(
        id: String,
        initial: ScaledQuantity,
        remaining: ScaledQuantity,
        expiryEpochMs: Long? = null,
        createdAtEpochMs: Long = 1000L
    ): StockLot = StockLot(
        id = id,
        medicationId = "med_test",
        lotNumber = "LOT-$id",
        initialQuantity = initial,
        remainingQuantity = remaining,
        unit = StockUnit.TABLETS,
        expiryDateEpochMs = expiryEpochMs,
        createdAtEpochMs = createdAtEpochMs
    )

    @Test
    fun t023_zeroDoseQuantityReturnsEmptyLotAllocation() {
        // T023: Zero dose quantity returns empty lot allocation
        val lots = listOf(createLot("lot1", ScaledQuantity.ofWhole(10), ScaledQuantity.ofWhole(10), 5000L))
        val allocation = calculator.allocateDoseAgainstLots(ScaledQuantity.ZERO, lots)
        assertTrue(allocation.isEmpty())
    }

    @Test
    fun t024_emptyAvailableLotsReturnsEmptyAllocation() {
        // T024: Empty available lots returns empty allocation
        val allocation = calculator.allocateDoseAgainstLots(ScaledQuantity.ofWhole(2), emptyList())
        assertTrue(allocation.isEmpty())
    }

    @Test
    fun t025_singleLotWithSufficientQuantityDebitedExactRequiredAmount() {
        // T025: Single lot with sufficient quantity debited exact required amount
        val lot = createLot("lot1", ScaledQuantity.ofWhole(30), ScaledQuantity.ofWhole(30), 10000L)
        val dose = ScaledQuantity.ofWhole(2)

        val allocation = calculator.allocateDoseAgainstLots(dose, listOf(lot))

        assertEquals(1, allocation.size)
        assertEquals("lot1", allocation[0].first.id)
        assertEquals(ScaledQuantity.ofWhole(2), allocation[0].second)
    }

    @Test
    fun t026_fefoSortedByAscendingExpiryDateDebitsEarliestExpiryFirst() {
        // T026: FEFO sorts lots by ascending expiry date (earliest expiry debited first)
        val lotExpiringNextMonth = createLot("lot_soon", ScaledQuantity.ofWhole(10), ScaledQuantity.ofWhole(10), 1000L)
        val lotExpiringNextYear = createLot("lot_later", ScaledQuantity.ofWhole(10), ScaledQuantity.ofWhole(10), 2000L)

        // Pass in reverse order to ensure sorting logic takes effect
        val allocation = calculator.allocateDoseAgainstLots(
            ScaledQuantity.ofWhole(5),
            listOf(lotExpiringNextYear, lotExpiringNextMonth)
        )

        assertEquals(1, allocation.size)
        assertEquals("lot_soon", allocation[0].first.id)
        assertEquals(ScaledQuantity.ofWhole(5), allocation[0].second)
    }

    @Test
    fun t027_fefoPlacesLotsWithNullExpiryDateAfterLotsWithSpecifiedExpiry() {
        // T027: FEFO places lots with null expiry date after lots with specified expiry dates
        val lotWithExpiry = createLot("lot_with_exp", ScaledQuantity.ofWhole(10), ScaledQuantity.ofWhole(10), 5000L)
        val lotNoExpiry = createLot("lot_no_exp", ScaledQuantity.ofWhole(10), ScaledQuantity.ofWhole(10), null)

        val allocation = calculator.allocateDoseAgainstLots(
            ScaledQuantity.ofWhole(5),
            listOf(lotNoExpiry, lotWithExpiry)
        )

        assertEquals(1, allocation.size)
        assertEquals("lot_with_exp", allocation[0].first.id)
    }

    @Test
    fun t028_equalExpiryDatesFallBackToFifoByCreationTimestamp() {
        // T028: Equal expiry dates fall back to FIFO by creation timestamp (createdAtEpochMs)
        val olderLot = createLot("older", ScaledQuantity.ofWhole(10), ScaledQuantity.ofWhole(10), 5000L, createdAtEpochMs = 100L)
        val newerLot = createLot("newer", ScaledQuantity.ofWhole(10), ScaledQuantity.ofWhole(10), 5000L, createdAtEpochMs = 200L)

        val allocation = calculator.allocateDoseAgainstLots(
            ScaledQuantity.ofWhole(4),
            listOf(newerLot, olderLot)
        )

        assertEquals(1, allocation.size)
        assertEquals("older", allocation[0].first.id)
        assertEquals(ScaledQuantity.ofWhole(4), allocation[0].second)
    }

    @Test
    fun t029_multiLotSplitAllocationWhenFirstLotHasPartialQuantity() {
        // T029: Multi-lot split allocation when first lot has partial quantity
        val lot1 = createLot("lot1", ScaledQuantity.ofWhole(10), ScaledQuantity.ofWhole(2), 1000L) // only 2 left
        val lot2 = createLot("lot2", ScaledQuantity.ofWhole(10), ScaledQuantity.ofWhole(10), 2000L) // 10 left

        val dose = ScaledQuantity.ofWhole(5) // Need 5: 2 from lot1, 3 from lot2
        val allocation = calculator.allocateDoseAgainstLots(dose, listOf(lot1, lot2))

        assertEquals(2, allocation.size)
        assertEquals("lot1", allocation[0].first.id)
        assertEquals(ScaledQuantity.ofWhole(2), allocation[0].second)
        assertEquals("lot2", allocation[1].first.id)
        assertEquals(ScaledQuantity.ofWhole(3), allocation[1].second)
    }

    @Test
    fun t030_exhaustedLotsWithZeroOrNegativeRemainingAreFilteredOut() {
        // T030: Exhausted lots (remainingQuantity <= 0) are filtered out from allocation
        val exhaustedLot = createLot("exhausted", ScaledQuantity.ofWhole(10), ScaledQuantity.ZERO, 500L)
        val negativeLot = createLot("negative", ScaledQuantity.ofWhole(10), ScaledQuantity.ofScaled(-1000L), 600L)
        val healthyLot = createLot("healthy", ScaledQuantity.ofWhole(10), ScaledQuantity.ofWhole(10), 5000L)

        val allocation = calculator.allocateDoseAgainstLots(
            ScaledQuantity.ofWhole(3),
            listOf(exhaustedLot, negativeLot, healthyLot)
        )

        assertEquals(1, allocation.size)
        assertEquals("healthy", allocation[0].first.id)
        assertEquals(ScaledQuantity.ofWhole(3), allocation[0].second)
    }
}
