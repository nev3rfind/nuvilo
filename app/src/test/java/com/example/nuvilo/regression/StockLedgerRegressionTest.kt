package com.example.nuvilo.regression

import com.example.nuvilo.domain.model.MovementType
import com.example.nuvilo.domain.model.ScaledQuantity
import com.example.nuvilo.domain.model.StockLedgerEntry
import com.example.nuvilo.domain.model.StockUnit
import com.example.nuvilo.domain.stock.DuplicateDoseDeductionException
import com.example.nuvilo.domain.stock.IncompatibleUnitsException
import com.example.nuvilo.domain.stock.StockLedgerCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Regression Test Suite: Category A (Domain Models & Accounting)
 * Tests T011 to T022: Append-Only Stock Ledger, Balances, and Invariants
 */
class StockLedgerRegressionTest {

    private lateinit var calculator: StockLedgerCalculator

    @Before
    fun setUp() {
        calculator = StockLedgerCalculator()
    }

    private fun createEntry(
        id: String,
        medicationId: String = "med_1",
        movementType: MovementType,
        quantity: ScaledQuantity,
        unit: StockUnit = StockUnit.TABLETS,
        doseOccurrenceId: String? = null,
        isEstimate: Boolean = false
    ): StockLedgerEntry = StockLedgerEntry(
        id = id,
        medicationId = medicationId,
        timestampEpochMs = System.currentTimeMillis(),
        movementType = movementType,
        quantity = quantity,
        unit = unit,
        doseOccurrenceId = doseOccurrenceId,
        isEstimate = isEstimate
    )

    @Test
    fun t011_openingStockBalanceWithZeroMovementsReturnsOpeningStock() {
        // T011: Opening stock balance with zero movements returns opening stock
        val opening = ScaledQuantity.ofWhole(30)
        val threshold = ScaledQuantity.ofWhole(5)

        val balance = calculator.calculateBalance(
            openingStock = opening,
            openingUnit = StockUnit.TABLETS,
            movements = emptyList(),
            lowStockThreshold = threshold
        )

        assertEquals(opening, balance.currentBalance)
        assertEquals(StockUnit.TABLETS, balance.unit)
        assertFalse(balance.isLowStock)
        assertFalse(balance.isNegative)
        assertFalse(balance.isEstimate)
    }

    @Test
    fun t012_inflowMovementsIncreaseCurrentBalance() {
        // T012: Inflow movements (RESTOCK, ADJUSTMENT_IN) increase current balance
        val opening = ScaledQuantity.ofWhole(10)
        val movements = listOf(
            createEntry("e1", movementType = MovementType.RESTOCK, quantity = ScaledQuantity.ofWhole(20)),
            createEntry("e2", movementType = MovementType.ADJUSTMENT_IN, quantity = ScaledQuantity.ofWhole(5))
        )

        val balance = calculator.calculateBalance(
            openingStock = opening,
            openingUnit = StockUnit.TABLETS,
            movements = movements,
            lowStockThreshold = ScaledQuantity.ofWhole(5)
        )

        assertEquals(ScaledQuantity.ofWhole(35), balance.currentBalance)
    }

    @Test
    fun t013_outflowMovementsDecreaseCurrentBalance() {
        // T013: Outflow movements (DOSE_TAKEN, DOSE_ASSUMED, WASTAGE, ADJUSTMENT_OUT) decrease balance
        val opening = ScaledQuantity.ofWhole(50)
        val movements = listOf(
            createEntry("e1", movementType = MovementType.DOSE_TAKEN, quantity = ScaledQuantity.ofWhole(2), doseOccurrenceId = "occ_1"),
            createEntry("e2", movementType = MovementType.DOSE_ASSUMED, quantity = ScaledQuantity.ofWhole(2), doseOccurrenceId = "occ_2"),
            createEntry("e3", movementType = MovementType.WASTAGE, quantity = ScaledQuantity.ofWhole(1)),
            createEntry("e4", movementType = MovementType.ADJUSTMENT_OUT, quantity = ScaledQuantity.ofWhole(5))
        )

        val balance = calculator.calculateBalance(
            openingStock = opening,
            openingUnit = StockUnit.TABLETS,
            movements = movements,
            lowStockThreshold = ScaledQuantity.ofWhole(10)
        )

        // 50 - 2 - 2 - 1 - 5 = 40
        assertEquals(ScaledQuantity.ofWhole(40), balance.currentBalance)
    }

    @Test
    fun t014_incompatibleUnitsBetweenOpeningAndMovementThrowsException() {
        // T014: Incompatible units between opening and movement throws IncompatibleUnitsException
        val opening = ScaledQuantity.ofWhole(10)
        val movements = listOf(
            createEntry("e1", movementType = MovementType.RESTOCK, quantity = ScaledQuantity.ofWhole(10), unit = StockUnit.ML)
        )

        assertThrows(IncompatibleUnitsException::class.java) {
            calculator.calculateBalance(
                openingStock = opening,
                openingUnit = StockUnit.TABLETS,
                movements = movements,
                lowStockThreshold = ScaledQuantity.ofWhole(5)
            )
        }
    }

    @Test
    fun t015_duplicateDoseDeductionOnIdenticalOccurrenceIdThrowsException() {
        // T015: Duplicate dose deduction on identical doseOccurrenceId throws DuplicateDoseDeductionException
        val opening = ScaledQuantity.ofWhole(20)
        val movements = listOf(
            createEntry("e1", movementType = MovementType.DOSE_TAKEN, quantity = ScaledQuantity.ofWhole(1), doseOccurrenceId = "occ_duplicate"),
            createEntry("e2", movementType = MovementType.DOSE_TAKEN, quantity = ScaledQuantity.ofWhole(1), doseOccurrenceId = "occ_duplicate")
        )

        assertThrows(DuplicateDoseDeductionException::class.java) {
            calculator.calculateBalance(
                openingStock = opening,
                openingUnit = StockUnit.TABLETS,
                movements = movements,
                lowStockThreshold = ScaledQuantity.ofWhole(5)
            )
        }
    }

    @Test
    fun t016_doseReversalMatchedWithPriorDoseDeductionRestoresStock() {
        // T016: Dose reversal matched with prior dose deduction restores stock and does not duplicate
        val opening = ScaledQuantity.ofWhole(20)
        val movements = listOf(
            createEntry("e1", movementType = MovementType.DOSE_TAKEN, quantity = ScaledQuantity.ofWhole(2), doseOccurrenceId = "occ_100"),
            createEntry("e2", movementType = MovementType.DOSE_REVERSAL, quantity = ScaledQuantity.ofWhole(2), doseOccurrenceId = "occ_100")
        )

        val balance = calculator.calculateBalance(
            openingStock = opening,
            openingUnit = StockUnit.TABLETS,
            movements = movements,
            lowStockThreshold = ScaledQuantity.ofWhole(5)
        )

        // 20 - 2 + 2 = 20
        assertEquals(ScaledQuantity.ofWhole(20), balance.currentBalance)
    }

    @Test
    fun t017_unmatchedDoseReversalThrowsIllegalStateException() {
        // T017: Unmatched dose reversal throws IllegalStateException
        val opening = ScaledQuantity.ofWhole(20)
        val movements = listOf(
            createEntry("e1", movementType = MovementType.DOSE_REVERSAL, quantity = ScaledQuantity.ofWhole(1), doseOccurrenceId = "occ_never_taken")
        )

        assertThrows(IllegalStateException::class.java) {
            calculator.calculateBalance(
                openingStock = opening,
                openingUnit = StockUnit.TABLETS,
                movements = movements,
                lowStockThreshold = ScaledQuantity.ofWhole(5)
            )
        }
    }

    @Test
    fun t018_lowStockThresholdCalculationIdentifiesWhenBalanceAtOrBelowThreshold() {
        // T018: Low stock threshold calculation identifies when balance <= threshold
        val threshold = ScaledQuantity.ofWhole(10)

        // Balance 10 (equal to threshold) -> isLowStock = true
        val balanceEqual = calculator.calculateBalance(
            openingStock = ScaledQuantity.ofWhole(10),
            openingUnit = StockUnit.TABLETS,
            movements = emptyList(),
            lowStockThreshold = threshold
        )
        assertTrue(balanceEqual.isLowStock)

        // Balance 11 (above threshold) -> isLowStock = false
        val balanceAbove = calculator.calculateBalance(
            openingStock = ScaledQuantity.ofWhole(11),
            openingUnit = StockUnit.TABLETS,
            movements = emptyList(),
            lowStockThreshold = threshold
        )
        assertFalse(balanceAbove.isLowStock)

        // Balance 9 (below threshold) -> isLowStock = true
        val balanceBelow = calculator.calculateBalance(
            openingStock = ScaledQuantity.ofWhole(9),
            openingUnit = StockUnit.TABLETS,
            movements = emptyList(),
            lowStockThreshold = threshold
        )
        assertTrue(balanceBelow.isLowStock)
    }

    @Test
    fun t019_negativeBalanceCorrectlyMarkedWithIsNegativeAndIsLowStock() {
        // T019: Negative balance correctly marked with isNegative = true and isLowStock = true
        val opening = ScaledQuantity.ofWhole(2)
        val movements = listOf(
            createEntry("e1", movementType = MovementType.DOSE_TAKEN, quantity = ScaledQuantity.ofWhole(5), doseOccurrenceId = "occ_overdraft")
        )

        val balance = calculator.calculateBalance(
            openingStock = opening,
            openingUnit = StockUnit.TABLETS,
            movements = movements,
            lowStockThreshold = ScaledQuantity.ofWhole(5)
        )

        assertEquals(ScaledQuantity.ofWhole(-3), balance.currentBalance)
        assertTrue(balance.isNegative)
        assertTrue(balance.isLowStock)
        assertNull(balance.daysRemaining) // negative balances cannot have positive days remaining
    }

    @Test
    fun t020_daysRemainingCalculationWithValidDailyConsumptionRate() {
        // T020: Days remaining calculation with valid daily consumption rate
        val opening = ScaledQuantity.ofWhole(30)
        val dailyRate = ScaledQuantity.ofWhole(2) // 2 tablets per day

        val balance = calculator.calculateBalance(
            openingStock = opening,
            openingUnit = StockUnit.TABLETS,
            movements = emptyList(),
            lowStockThreshold = ScaledQuantity.ofWhole(5),
            dailyConsumptionRate = dailyRate
        )

        assertEquals(15, balance.daysRemaining)
    }

    @Test
    fun t021_daysRemainingCalculationWithZeroOrNegativeRateReturnsNull() {
        // T021: Days remaining calculation with zero or negative daily consumption rate returns null
        val opening = ScaledQuantity.ofWhole(30)

        val balanceZeroRate = calculator.calculateBalance(
            openingStock = opening,
            openingUnit = StockUnit.TABLETS,
            movements = emptyList(),
            lowStockThreshold = ScaledQuantity.ofWhole(5),
            dailyConsumptionRate = ScaledQuantity.ZERO
        )
        assertNull(balanceZeroRate.daysRemaining)

        val balanceNullRate = calculator.calculateBalance(
            openingStock = opening,
            openingUnit = StockUnit.TABLETS,
            movements = emptyList(),
            lowStockThreshold = ScaledQuantity.ofWhole(5),
            dailyConsumptionRate = null
        )
        assertNull(balanceNullRate.daysRemaining)
    }

    @Test
    fun t022_estimatedConsumptionFlagPropagatesWhenEntryOrMedicationIsEstimate() {
        // T022: Estimated consumption flag propagates when any entry or medication is marked estimate
        val opening = ScaledQuantity.ofWhole(100)

        val balanceFromMedEstimate = calculator.calculateBalance(
            openingStock = opening,
            openingUnit = StockUnit.DROPS,
            movements = emptyList(),
            lowStockThreshold = ScaledQuantity.ofWhole(20),
            isEstimatedConsumption = true
        )
        assertTrue(balanceFromMedEstimate.isEstimate)

        val movementsWithEstimate = listOf(
            createEntry("e1", movementType = MovementType.DOSE_TAKEN, quantity = ScaledQuantity.ofWhole(5), unit = StockUnit.DROPS, doseOccurrenceId = "occ_drop", isEstimate = true)
        )
        val balanceFromEntryEstimate = calculator.calculateBalance(
            openingStock = opening,
            openingUnit = StockUnit.DROPS,
            movements = movementsWithEstimate,
            lowStockThreshold = ScaledQuantity.ofWhole(20),
            isEstimatedConsumption = false
        )
        assertTrue(balanceFromEntryEstimate.isEstimate)
    }
}
