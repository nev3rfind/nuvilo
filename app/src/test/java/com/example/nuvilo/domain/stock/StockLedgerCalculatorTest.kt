package com.example.nuvilo.domain.stock

import com.example.nuvilo.domain.model.DoseStatus
import com.example.nuvilo.domain.model.MedicationForm
import com.example.nuvilo.domain.model.MovementType
import com.example.nuvilo.domain.model.ScaledQuantity
import com.example.nuvilo.domain.model.StockLedgerEntry
import com.example.nuvilo.domain.model.StockLot
import com.example.nuvilo.domain.model.StockUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class StockLedgerCalculatorTest {

    private val calculator = StockLedgerCalculator()

    @Test
    fun initialStockOnly_returnsOpeningBalance() {
        val opening = ScaledQuantity.ofWhole(30)
        val balance = calculator.calculateBalance(
            openingStock = opening,
            openingUnit = StockUnit.TABLETS,
            movements = emptyList(),
            lowStockThreshold = ScaledQuantity.ofWhole(5)
        )

        assertEquals(30_000L, balance.currentBalance.rawScaledValue)
        assertEquals("30", balance.currentBalance.toDisplayString())
        assertFalse(balance.isNegative)
        assertFalse(balance.isLowStock)
        assertFalse(balance.isEstimate)
    }

    @Test
    fun confirmedAndAssumedDoses_deductFromBalance() {
        val opening = ScaledQuantity.ofWhole(20)
        val movements = listOf(
            StockLedgerEntry(
                id = "m1",
                medicationId = "med1",
                timestampEpochMs = 1000L,
                movementType = MovementType.DOSE_TAKEN,
                quantity = ScaledQuantity.ofWhole(1),
                unit = StockUnit.TABLETS,
                doseOccurrenceId = "occ_1"
            ),
            StockLedgerEntry(
                id = "m2",
                medicationId = "med1",
                timestampEpochMs = 2000L,
                movementType = MovementType.DOSE_ASSUMED,
                quantity = ScaledQuantity.ofWhole(2),
                unit = StockUnit.TABLETS,
                doseOccurrenceId = "occ_2"
            )
        )

        val balance = calculator.calculateBalance(
            openingStock = opening,
            openingUnit = StockUnit.TABLETS,
            movements = movements,
            lowStockThreshold = ScaledQuantity.ofWhole(5)
        )

        // 20 - 1 - 2 = 17
        assertEquals(17_000L, balance.currentBalance.rawScaledValue)
        assertEquals("17", balance.currentBalance.toDisplayString())
    }

    @Test
    fun restockingAndAdjustments_reconcileAccurately() {
        val opening = ScaledQuantity.ofWhole(10)
        val movements = listOf(
            // Restock +30
            StockLedgerEntry(
                id = "r1",
                medicationId = "med1",
                timestampEpochMs = 1000L,
                movementType = MovementType.RESTOCK,
                quantity = ScaledQuantity.ofWhole(30),
                unit = StockUnit.TABLETS,
                restockLotNumber = "LOT-1234"
            ),
            // Dose -2
            StockLedgerEntry(
                id = "d1",
                medicationId = "med1",
                timestampEpochMs = 2000L,
                movementType = MovementType.DOSE_TAKEN,
                quantity = ScaledQuantity.ofWhole(2),
                unit = StockUnit.TABLETS,
                doseOccurrenceId = "occ_1"
            ),
            // Manual adjustment: dropped 1 tablet (-1)
            StockLedgerEntry(
                id = "a1",
                medicationId = "med1",
                timestampEpochMs = 3000L,
                movementType = MovementType.ADJUSTMENT_OUT,
                quantity = ScaledQuantity.ofWhole(1),
                unit = StockUnit.TABLETS,
                reason = "Dropped tablet on floor"
            )
        )

        val balance = calculator.calculateBalance(
            openingStock = opening,
            openingUnit = StockUnit.TABLETS,
            movements = movements,
            lowStockThreshold = ScaledQuantity.ofWhole(5)
        )

        // 10 + 30 - 2 - 1 = 37
        assertEquals(37_000L, balance.currentBalance.rawScaledValue)
        assertEquals("37", balance.currentBalance.toDisplayString())
    }

    @Test
    fun duplicateDoseOccurrences_throwDuplicateException() {
        val opening = ScaledQuantity.ofWhole(10)
        val movements = listOf(
            StockLedgerEntry(
                id = "d1",
                medicationId = "med1",
                timestampEpochMs = 1000L,
                movementType = MovementType.DOSE_TAKEN,
                quantity = ScaledQuantity.ofWhole(1),
                unit = StockUnit.TABLETS,
                doseOccurrenceId = "occ_SAME"
            ),
            StockLedgerEntry(
                id = "d2",
                medicationId = "med1",
                timestampEpochMs = 1050L,
                movementType = MovementType.DOSE_TAKEN,
                quantity = ScaledQuantity.ofWhole(1),
                unit = StockUnit.TABLETS,
                doseOccurrenceId = "occ_SAME" // Duplicate occurrence ID
            )
        )

        assertThrows(DuplicateDoseDeductionException::class.java) {
            calculator.calculateBalance(
                openingStock = opening,
                openingUnit = StockUnit.TABLETS,
                movements = movements,
                lowStockThreshold = ScaledQuantity.ofWhole(2)
            )
        }
    }

    @Test
    fun doseReversal_reimbursesStockAccurately() {
        val opening = ScaledQuantity.ofWhole(10)
        val movements = listOf(
            // Dose taken -2
            StockLedgerEntry(
                id = "d1",
                medicationId = "med1",
                timestampEpochMs = 1000L,
                movementType = MovementType.DOSE_TAKEN,
                quantity = ScaledQuantity.ofWhole(2),
                unit = StockUnit.TABLETS,
                doseOccurrenceId = "occ_rev_1"
            ),
            // User corrected: marked dose as mistake -> DOSE_REVERSAL +2
            StockLedgerEntry(
                id = "rev1",
                medicationId = "med1",
                timestampEpochMs = 2000L,
                movementType = MovementType.DOSE_REVERSAL,
                quantity = ScaledQuantity.ofWhole(2),
                unit = StockUnit.TABLETS,
                doseOccurrenceId = "occ_rev_1",
                reason = "Corrected mistakenly logged dose"
            )
        )

        val balance = calculator.calculateBalance(
            openingStock = opening,
            openingUnit = StockUnit.TABLETS,
            movements = movements,
            lowStockThreshold = ScaledQuantity.ofWhole(2)
        )

        // 10 - 2 + 2 = 10 (Restored to 10!)
        assertEquals(10_000L, balance.currentBalance.rawScaledValue)
        assertEquals("10", balance.currentBalance.toDisplayString())
    }

    @Test
    fun duplicateDoseReversals_throwException() {
        val opening = ScaledQuantity.ofWhole(10)
        val movements = listOf(
            StockLedgerEntry(
                id = "d1",
                medicationId = "med1",
                timestampEpochMs = 1000L,
                movementType = MovementType.DOSE_TAKEN,
                quantity = ScaledQuantity.ofWhole(2),
                unit = StockUnit.TABLETS,
                doseOccurrenceId = "occ_rev_2"
            ),
            // First reversal
            StockLedgerEntry(
                id = "rev1",
                medicationId = "med1",
                timestampEpochMs = 2000L,
                movementType = MovementType.DOSE_REVERSAL,
                quantity = ScaledQuantity.ofWhole(2),
                unit = StockUnit.TABLETS,
                doseOccurrenceId = "occ_rev_2"
            ),
            // Second duplicate reversal attempt (should fail)
            StockLedgerEntry(
                id = "rev2",
                medicationId = "med1",
                timestampEpochMs = 3000L,
                movementType = MovementType.DOSE_REVERSAL,
                quantity = ScaledQuantity.ofWhole(2),
                unit = StockUnit.TABLETS,
                doseOccurrenceId = "occ_rev_2"
            )
        )

        assertThrows(IllegalStateException::class.java) {
            calculator.calculateBalance(
                openingStock = opening,
                openingUnit = StockUnit.TABLETS,
                movements = movements,
                lowStockThreshold = ScaledQuantity.ofWhole(2)
            )
        }
    }

    @Test
    fun fefoLotAllocation_debitsEarliestExpiringLotFirst() {
        val lotA = StockLot(
            id = "lotA",
            medicationId = "med1",
            lotNumber = "LOT-EARLY",
            initialQuantity = ScaledQuantity.ofWhole(10),
            remainingQuantity = ScaledQuantity.ofWhole(10),
            unit = StockUnit.TABLETS,
            expiryDateEpochMs = 100_000L, // Expires sooner
            createdAtEpochMs = 1000L
        )
        val lotB = StockLot(
            id = "lotB",
            medicationId = "med1",
            lotNumber = "LOT-LATE",
            initialQuantity = ScaledQuantity.ofWhole(20),
            remainingQuantity = ScaledQuantity.ofWhole(20),
            unit = StockUnit.TABLETS,
            expiryDateEpochMs = 200_000L, // Expires later
            createdAtEpochMs = 2000L
        )

        // Request dose of 15 tablets
        val allocations = calculator.allocateDoseAgainstLots(
            doseQuantity = ScaledQuantity.ofWhole(15),
            availableLots = listOf(lotB, lotA) // Unsorted input
        )

        assertEquals(2, allocations.size)
        // First allocation: 10 tablets from Lot A (completely depleted)
        assertEquals("lotA", allocations[0].first.id)
        assertEquals(10_000L, allocations[0].second.rawScaledValue)

        // Second allocation: 5 tablets from Lot B
        assertEquals("lotB", allocations[1].first.id)
        assertEquals(5_000L, allocations[1].second.rawScaledValue)
    }

    @Test
    fun incompatibleUnits_throwIncompatibleUnitsException() {
        val opening = ScaledQuantity.ofWhole(10) // Tablets
        val movements = listOf(
            StockLedgerEntry(
                id = "d1",
                medicationId = "med1",
                timestampEpochMs = 1000L,
                movementType = MovementType.DOSE_TAKEN,
                quantity = ScaledQuantity.ofWhole(5),
                unit = StockUnit.ML, // Incompatible ML with Tablets!
                doseOccurrenceId = "occ_1"
            )
        )

        assertThrows(IncompatibleUnitsException::class.java) {
            calculator.calculateBalance(
                openingStock = opening,
                openingUnit = StockUnit.TABLETS,
                movements = movements,
                lowStockThreshold = ScaledQuantity.ofWhole(2)
            )
        }
    }

    @Test
    fun insufficientStock_producesVisibleNegativeBalanceWithoutCrash() {
        val opening = ScaledQuantity.ofWhole(2)
        val movements = listOf(
            StockLedgerEntry(
                id = "d1",
                medicationId = "med1",
                timestampEpochMs = 1000L,
                movementType = MovementType.DOSE_TAKEN,
                quantity = ScaledQuantity.ofWhole(5),
                unit = StockUnit.TABLETS,
                doseOccurrenceId = "occ_1"
            )
        )

        val balance = calculator.calculateBalance(
            openingStock = opening,
            openingUnit = StockUnit.TABLETS,
            movements = movements,
            lowStockThreshold = ScaledQuantity.ofWhole(2)
        )

        // 2 - 5 = -3
        assertTrue(balance.isNegative)
        assertEquals(-3_000L, balance.currentBalance.rawScaledValue)
        assertEquals("-3", balance.currentBalance.toDisplayString())
        assertTrue(balance.isLowStock)
        assertNull(balance.daysRemaining) // Cannot calculate positive days for negative stock
    }

    @Test
    fun eyeDropsAndInhalers_areExplicitlyMarkedAsEstimated() {
        val opening = ScaledQuantity.ofWhole(1)
        val balance = calculator.calculateBalance(
            openingStock = opening,
            openingUnit = StockUnit.BOTTLES,
            movements = emptyList(),
            lowStockThreshold = ScaledQuantity.ofWhole(1),
            isEstimatedConsumption = true
        )

        assertTrue(balance.isEstimate)
    }

    @Test
    fun daysRemaining_computesAccurateIntegerFloor() {
        val opening = ScaledQuantity.ofWhole(25) // 25 tablets
        val dailyRate = ScaledQuantity.ofWhole(2) // 2 tablets per day

        val balance = calculator.calculateBalance(
            openingStock = opening,
            openingUnit = StockUnit.TABLETS,
            movements = emptyList(),
            lowStockThreshold = ScaledQuantity.ofWhole(5),
            dailyConsumptionRate = dailyRate
        )

        // 25 / 2 = 12 days remaining
        assertEquals(12, balance.daysRemaining)
    }
}
