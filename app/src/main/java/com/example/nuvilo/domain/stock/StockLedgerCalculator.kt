package com.example.nuvilo.domain.stock

import com.example.nuvilo.domain.model.DepletionReason
import com.example.nuvilo.domain.model.EarlyDepletionOutcome
import com.example.nuvilo.domain.model.MovementType
import com.example.nuvilo.domain.model.PackageStatus
import com.example.nuvilo.domain.model.ReopenDepletionOutcome
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

    /**
     * Package-aware FEFO allocation:
     * 1. Excludes EMPTY, EXPIRED, DISCARDED, past manufacturer expiry, and past discard-after-opening lots.
     * 2. Consumes from already OPENED eligible packages first (nearest discard/expiry first).
     * 3. Automatically transitions UNOPENED -> OPENED in FEFO order and stamps discardAfterOpeningEpochMs.
     */
    fun allocateDoseAgainstPackages(
        doseQuantity: ScaledQuantity,
        lots: List<StockLot>,
        nowEpochMs: Long = System.currentTimeMillis()
    ): List<PackageAllocation> {
        if (!doseQuantity.isPositive()) return emptyList()

        val eligibleOpened = mutableListOf<StockLot>()
        val eligibleUnopened = mutableListOf<StockLot>()

        for (lot in lots) {
            if (!lot.remainingQuantity.isPositive()) continue
            val mfgExpired = lot.expiryDateEpochMs != null && lot.expiryDateEpochMs <= nowEpochMs
            if (mfgExpired) continue

            when (lot.packageStatus) {
                com.example.nuvilo.domain.model.PackageStatus.EMPTY,
                com.example.nuvilo.domain.model.PackageStatus.EXPIRED,
                com.example.nuvilo.domain.model.PackageStatus.DISCARDED -> continue
                com.example.nuvilo.domain.model.PackageStatus.OPENED -> {
                    val effectiveDiscardMs = lot.effectiveDiscardAfterOpeningEpochMs
                    if (effectiveDiscardMs != null && effectiveDiscardMs <= nowEpochMs) continue
                    eligibleOpened.add(lot)
                }
                com.example.nuvilo.domain.model.PackageStatus.UNOPENED -> {
                    eligibleUnopened.add(lot)
                }
            }
        }

        val orderedQueue = eligibleOpened.sortedWith(
            compareBy<StockLot>(
                { it.effectiveDiscardAfterOpeningEpochMs ?: Long.MAX_VALUE },
                { it.expiryDateEpochMs ?: Long.MAX_VALUE },
                { it.createdAtEpochMs }
            )
        ) + eligibleUnopened.sortedWith(
            compareBy<StockLot>(
                { it.expiryDateEpochMs ?: Long.MAX_VALUE },
                { it.createdAtEpochMs }
            )
        )

        var remainingToDeductRaw = doseQuantity.rawScaledValue
        val results = mutableListOf<PackageAllocation>()

        for (candidate in orderedQueue) {
            if (remainingToDeductRaw <= 0L) break
            val wasUnopened = candidate.packageStatus == com.example.nuvilo.domain.model.PackageStatus.UNOPENED
            val openedLot = if (wasUnopened) {
                openPackage(candidate, nowEpochMs)
            } else {
                candidate
            }

            val takeRaw = minOf(remainingToDeductRaw, openedLot.remainingQuantity.rawScaledValue)
            val newRemaining = ScaledQuantity(openedLot.remainingQuantity.rawScaledValue - takeRaw)
            val nextStatus = if (!newRemaining.isPositive()) {
                com.example.nuvilo.domain.model.PackageStatus.EMPTY
            } else {
                com.example.nuvilo.domain.model.PackageStatus.OPENED
            }

            results.add(
                PackageAllocation(
                    updatedLot = openedLot.copy(
                        remainingQuantity = newRemaining,
                        packageStatus = nextStatus
                    ),
                    deductedQuantity = ScaledQuantity(takeRaw),
                    newlyOpened = wasUnopened
                )
            )
            remainingToDeductRaw -= takeRaw
        }

        return results
    }

    /**
     * Opens an unopened package idempotently, stamping [openedAtEpochMs] and [discardAfterOpeningEpochMs].
     */
    fun openPackage(lot: StockLot, nowEpochMs: Long = System.currentTimeMillis()): StockLot {
        if (lot.packageStatus == com.example.nuvilo.domain.model.PackageStatus.OPENED) {
            return if (lot.openedAtEpochMs != null) {
                lot
            } else {
                val discardEpoch = lot.discardAfterOpeningDays?.let { nowEpochMs + it.toLong() * 86_400_000L }
                lot.copy(
                    openedAtEpochMs = nowEpochMs,
                    discardAfterOpeningEpochMs = lot.discardAfterOpeningEpochMs ?: discardEpoch
                )
            }
        }
        check(lot.packageStatus == com.example.nuvilo.domain.model.PackageStatus.UNOPENED) {
            "Cannot open package in status ${lot.packageStatus}"
        }
        check(lot.remainingQuantity.isPositive()) {
            "Cannot open an empty package"
        }
        check(lot.expiryDateEpochMs == null || lot.expiryDateEpochMs > nowEpochMs) {
            "Cannot open an expired package"
        }
        val discardEpoch = lot.discardAfterOpeningDays?.let { nowEpochMs + it.toLong() * 86_400_000L }
        return lot.copy(
            packageStatus = com.example.nuvilo.domain.model.PackageStatus.OPENED,
            openedAtEpochMs = nowEpochMs,
            discardAfterOpeningEpochMs = discardEpoch
        )
    }

    /**
     * Marks a package DISCARDED and emits an exact WASTAGE ledger entry for any remaining units.
     */
    fun discardPackage(
        lot: StockLot,
        reason: String,
        nowEpochMs: Long = System.currentTimeMillis(),
        movementId: String = java.util.UUID.randomUUID().toString()
    ): PackageMutationOutcome {
        check(lot.packageStatus != com.example.nuvilo.domain.model.PackageStatus.DISCARDED) {
            "Package is already discarded"
        }
        val wastedQty = lot.remainingQuantity
        val updatedLot = lot.copy(
            remainingQuantity = ScaledQuantity.ZERO,
            packageStatus = com.example.nuvilo.domain.model.PackageStatus.DISCARDED,
            discardedAtEpochMs = nowEpochMs,
            discardReason = reason.ifBlank { "Package discarded" }
        )
        val wastageEntry = if (wastedQty.isPositive()) {
            StockLedgerEntry(
                id = movementId,
                medicationId = lot.medicationId,
                timestampEpochMs = nowEpochMs,
                movementType = MovementType.WASTAGE,
                quantity = wastedQty,
                unit = lot.unit,
                reason = reason.ifBlank { "Package discarded" },
                lotId = lot.id
            )
        } else null

        return PackageMutationOutcome(
            updatedLot = updatedLot,
            wastageEntry = wastageEntry,
            ledgerEntry = wastageEntry
        )
    }

    /**
     * Scoped recount for a single package/bottle without affecting unopened packages.
     */
    fun recountPackage(
        lot: StockLot,
        observedRemaining: ScaledQuantity,
        reason: String,
        nowEpochMs: Long = System.currentTimeMillis(),
        movementId: String = java.util.UUID.randomUUID().toString()
    ): PackageMutationOutcome {
        require(!observedRemaining.isNegative()) { "Observed package remaining cannot be negative" }
        check(lot.packageStatus != com.example.nuvilo.domain.model.PackageStatus.DISCARDED) {
            "Cannot recount a discarded package"
        }

        val currentRemaining = lot.remainingQuantity
        val deltaRaw = observedRemaining.rawScaledValue - currentRemaining.rawScaledValue
        val nextStatus = when {
            !observedRemaining.isPositive() -> com.example.nuvilo.domain.model.PackageStatus.EMPTY
            lot.packageStatus == com.example.nuvilo.domain.model.PackageStatus.EMPTY -> com.example.nuvilo.domain.model.PackageStatus.OPENED
            else -> lot.packageStatus
        }

        val updatedInitial = if (observedRemaining > lot.initialQuantity) observedRemaining else lot.initialQuantity
        val updatedLot = lot.copy(
            initialQuantity = updatedInitial,
            remainingQuantity = observedRemaining,
            packageStatus = nextStatus
        )

        val ledgerEntry = if (deltaRaw != 0L) {
            val isIn = deltaRaw > 0L
            StockLedgerEntry(
                id = movementId,
                medicationId = lot.medicationId,
                timestampEpochMs = nowEpochMs,
                movementType = if (isIn) MovementType.ADJUSTMENT_IN else MovementType.ADJUSTMENT_OUT,
                quantity = ScaledQuantity(kotlin.math.abs(deltaRaw)),
                unit = lot.unit,
                reason = reason.ifBlank { "Opened package recount" },
                lotId = lot.id
            )
        } else null

        return PackageMutationOutcome(
            updatedLot = updatedLot,
            wastageEntry = null,
            ledgerEntry = ledgerEntry
        )
    }

    /**
     * Identifies the next eligible unopened package according to FEFO policy.
     * Staged for display ("Ready to Open") without auto-opening until confirmed.
     */
    fun findStagedNextPackage(
        lots: List<StockLot>,
        nowEpochMs: Long = System.currentTimeMillis()
    ): StockLot? {
        return lots
            .filter { it.packageStatus == PackageStatus.UNOPENED }
            .filter { it.expiryDateEpochMs == null || it.expiryDateEpochMs > nowEpochMs }
            .sortedWith(
                compareBy<StockLot>(
                    { it.expiryDateEpochMs ?: Long.MAX_VALUE },
                    { it.createdAtEpochMs }
                )
            )
            .firstOrNull()
    }

    /**
     * Marks an opened package as finished early (Early Package Depletion).
     *
     * Invariants:
     * 1. Preserves original capacity ([initialQuantity]) and consumed history.
     * 2. Writes off remaining unharvested units as compensating [MovementType.WASTAGE] with explicit depletion reason/note.
     * 3. Transitions packageStatus to [PackageStatus.EMPTY] with [depletedAtEpochMs], [depletionReason], [depletionNote].
     * 4. Queries next eligible unopened FEFO lot as [stagedNextLot] (remains UNOPENED until confirmed).
     * 5. Zero dose fabrication: Dose records and adherence remain 100% untouched.
     */
    fun depletePackageEarly(
        lot: StockLot,
        reason: DepletionReason,
        userNote: String? = null,
        availableLots: List<StockLot> = emptyList(),
        nowEpochMs: Long = System.currentTimeMillis(),
        movementId: String = java.util.UUID.randomUUID().toString()
    ): EarlyDepletionOutcome {
        check(lot.packageStatus != PackageStatus.EMPTY) { "Package is already empty" }
        check(lot.packageStatus != PackageStatus.DISCARDED) { "Cannot deplete a discarded package" }

        val wastedQty = lot.remainingQuantity
        val noteSuffix = if (!userNote.isNullOrBlank()) " ($userNote)" else ""
        val reasonText = "Early package depletion [${reason.ledgerReasonCode}]: ${reason.displayName}$noteSuffix"

        val updatedLot = lot.copy(
            remainingQuantity = ScaledQuantity.ZERO,
            packageStatus = PackageStatus.EMPTY,
            depletedAtEpochMs = nowEpochMs,
            depletionReason = reason.ledgerReasonCode,
            depletionNote = userNote?.takeIf { it.isNotBlank() }
        )

        val wastageEntry = if (wastedQty.isPositive()) {
            StockLedgerEntry(
                id = movementId,
                medicationId = lot.medicationId,
                timestampEpochMs = nowEpochMs,
                movementType = MovementType.WASTAGE,
                quantity = wastedQty,
                unit = lot.unit,
                reason = reasonText,
                lotId = lot.id,
                isEstimate = true
            )
        } else null

        val stagedNext = findStagedNextPackage(availableLots.filter { it.id != lot.id }, nowEpochMs)

        return EarlyDepletionOutcome(
            depletedLot = updatedLot,
            compensatingEntry = wastageEntry,
            stagedNextLot = stagedNext
        )
    }

    /**
     * Reversal workflow if a package was accidentally marked as finished.
     *
     * Invariants:
     * 1. Reverts package status from [PackageStatus.EMPTY] back to [PackageStatus.OPENED].
     * 2. Restores the previously written-off balance.
     * 3. Records a compensating [MovementType.ADJUSTMENT_IN] ledger movement.
     * 4. Clears depletion audit fields.
     */
    fun reopenDepletedPackage(
        lot: StockLot,
        depletionLedgerEntry: StockLedgerEntry? = null,
        reason: String = "Accidental depletion correction",
        nowEpochMs: Long = System.currentTimeMillis(),
        movementId: String = java.util.UUID.randomUUID().toString(),
        restoredQuantity: ScaledQuantity? = null,
        reversalReason: String? = null
    ): ReopenDepletionOutcome {
        check(lot.packageStatus == PackageStatus.EMPTY) { "Only empty packages can be reopened" }

        val actualReason = reversalReason ?: reason
        val restoredQty = restoredQuantity
            ?: depletionLedgerEntry?.quantity
            ?: lot.initialQuantity
        val updatedLot = lot.copy(
            remainingQuantity = restoredQty,
            packageStatus = PackageStatus.OPENED,
            depletedAtEpochMs = null,
            depletionReason = null,
            depletionNote = null
        )

        val reversalEntry = StockLedgerEntry(
            id = movementId,
            medicationId = lot.medicationId,
            timestampEpochMs = nowEpochMs,
            movementType = MovementType.ADJUSTMENT_IN,
            quantity = restoredQty,
            unit = lot.unit,
            reason = "Reversal of early package depletion: $actualReason",
            lotId = lot.id,
            isEstimate = depletionLedgerEntry?.isEstimate ?: true
        )

        return ReopenDepletionOutcome(
            reopenedLot = updatedLot,
            reversalEntry = reversalEntry
        )
    }
}

data class PackageAllocation(
    val updatedLot: StockLot,
    val deductedQuantity: ScaledQuantity,
    val newlyOpened: Boolean
)

data class PackageMutationOutcome(
    val updatedLot: StockLot,
    val wastageEntry: StockLedgerEntry?,
    val ledgerEntry: StockLedgerEntry?
)
