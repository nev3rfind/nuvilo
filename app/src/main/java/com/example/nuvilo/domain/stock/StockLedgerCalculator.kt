package com.example.nuvilo.domain.stock

import com.example.nuvilo.domain.model.MovementType
import com.example.nuvilo.domain.model.ScaledQuantity
import com.example.nuvilo.domain.model.StockBalance
import com.example.nuvilo.domain.model.StockLedgerEntry
import com.example.nuvilo.domain.model.StockLot
import com.example.nuvilo.domain.model.StockUnit

class IncompatibleUnitsException(expected: StockUnit, found: StockUnit) :
    IllegalArgumentException("Incompatible stock units in ledger: expected $expected but found $found")

class DuplicateDoseDeductionException(occurrenceId: String) :
    IllegalStateException("Duplicate dose deduction detected for occurrence ID: $occurrenceId")

/**
 * Pure Kotlin calculator for the append-only stock ledger.
 *
 * Derives current inventory balances, lot allocations, and days remaining strictly from
 * opening inventory and recorded stock movements using exact scaled arithmetic.
 */
class StockLedgerCalculator {

    fun calculateBalance(
        openingStock: ScaledQuantity,
        openingUnit: StockUnit,
        movements: List<StockLedgerEntry>,
        lowStockThreshold: ScaledQuantity,
        dailyConsumptionRate: ScaledQuantity? = null,
        isEstimatedConsumption: Boolean = false
    ): StockBalance {
        var currentRaw = openingStock.rawScaledValue
        val netDeductionsPerOccurrence = mutableMapOf<String, Int>()
        var hasEstimate = isEstimatedConsumption

        for (entry in movements) {
            // Guard against incompatible units
            if (entry.unit != openingUnit) {
                throw IncompatibleUnitsException(openingUnit, entry.unit)
            }

            // Track dose deductions and reversals per occurrence
            if (entry.doseOccurrenceId != null) {
                val occId = entry.doseOccurrenceId
                when (entry.movementType) {
                    MovementType.DOSE_TAKEN, MovementType.DOSE_ASSUMED -> {
                        val currentNet = netDeductionsPerOccurrence.getOrDefault(occId, 0) + 1
                        if (currentNet > 1) {
                            throw DuplicateDoseDeductionException(occId)
                        }
                        netDeductionsPerOccurrence[occId] = currentNet
                    }
                    MovementType.DOSE_REVERSAL -> {
                        val currentNet = netDeductionsPerOccurrence.getOrDefault(occId, 0) - 1
                        if (currentNet < 0) {
                            throw IllegalStateException("Unmatched dose reversal for occurrence ID: $occId")
                        }
                        netDeductionsPerOccurrence[occId] = currentNet
                    }
                    else -> {}
                }
            }

            if (entry.isEstimate) {
                hasEstimate = true
            }

            if (entry.movementType.isInflow) {
                currentRaw += entry.quantity.rawScaledValue
            } else {
                currentRaw -= entry.quantity.rawScaledValue
            }
        }

        val balance = ScaledQuantity(currentRaw)
        val isNegative = balance.isNegative()
        val isLow = isNegative || balance <= lowStockThreshold

        val daysRemaining = if (dailyConsumptionRate != null && dailyConsumptionRate.isPositive() && !isNegative) {
            val wholeDays = (balance.rawScaledValue / dailyConsumptionRate.rawScaledValue).toInt()
            kotlin.math.max(0, wholeDays)
        } else {
            null
        }

        return StockBalance(
            currentBalance = balance,
            unit = openingUnit,
            isEstimate = hasEstimate,
            isNegative = isNegative,
            daysRemaining = daysRemaining,
            isLowStock = isLow
        )
    }

    /**
     * Allocates a required dose quantity against available lots using First Expired, First Out (FEFO).
     * If expiry dates are equal or null, falls back to First In, First Out (FIFO) by createdAt.
     *
     * @return List of Pairs: (StockLot, QuantityDebitedFromThisLot)
     */
    fun allocateDoseAgainstLots(
        doseQuantity: ScaledQuantity,
        availableLots: List<StockLot>
    ): List<Pair<StockLot, ScaledQuantity>> {
        if (doseQuantity.isZero() || availableLots.isEmpty()) return emptyList()

        // Sort FEFO: non-null expiry date ascending, null expiry dates placed at the end, then createdAt ascending
        val sortedLots = availableLots.filter { it.remainingQuantity.isPositive() }.sortedWith(
            compareBy<StockLot> { it.expiryDateEpochMs ?: Long.MAX_VALUE }
                .thenBy { it.createdAtEpochMs }
        )

        val result = mutableListOf<Pair<StockLot, ScaledQuantity>>()
        var remainingNeeded = doseQuantity.rawScaledValue

        for (lot in sortedLots) {
            if (remainingNeeded <= 0L) break
            val lotAvailable = lot.remainingQuantity.rawScaledValue
            val debitRaw = kotlin.math.min(lotAvailable, remainingNeeded)

            if (debitRaw > 0L) {
                result.add(Pair(lot, ScaledQuantity(debitRaw)))
                remainingNeeded -= debitRaw
            }
        }

        return result
    }
}
