package com.example.nuvilo.domain.stock

import com.example.nuvilo.data.mapper.toDomain
import com.example.nuvilo.data.mapper.toEntity
import com.example.nuvilo.domain.model.DepletionReason
import com.example.nuvilo.domain.model.MovementType
import com.example.nuvilo.domain.model.PackageStatus
import com.example.nuvilo.domain.model.PackageType
import com.example.nuvilo.domain.model.ScaledQuantity
import com.example.nuvilo.domain.model.StockLot
import com.example.nuvilo.domain.model.StockUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class EarlyPackageDepletionTest {

    private val calculator = StockLedgerCalculator()
    private val dayMs = 86_400_000L
    private val nowMs = 100L * dayMs

    private fun createLot(
        id: String,
        initialQuantity: Long = 300,
        remainingQuantity: Long = 60,
        status: PackageStatus = PackageStatus.OPENED,
        expiryDateEpochMs: Long? = nowMs + 180L * dayMs,
        createdAtEpochMs: Long = nowMs - 10L * dayMs,
        openedAtEpochMs: Long? = nowMs - 10L * dayMs,
        discardAfterOpeningDays: Int? = 28
    ) = StockLot(
        id = id,
        medicationId = "med-glaucoma-drops",
        lotNumber = "LOT-$id",
        initialQuantity = ScaledQuantity.ofWhole(initialQuantity),
        remainingQuantity = ScaledQuantity.ofWhole(remainingQuantity),
        unit = StockUnit.DROPS,
        expiryDateEpochMs = expiryDateEpochMs,
        createdAtEpochMs = createdAtEpochMs,
        packageType = PackageType.BOTTLE,
        packageLabel = "Bottle $id",
        packageStatus = status,
        openedAtEpochMs = openedAtEpochMs,
        discardAfterOpeningDays = discardAfterOpeningDays,
        discardAfterOpeningEpochMs = openedAtEpochMs?.let { it + (discardAfterOpeningDays ?: 28).toLong() * dayMs }
    )

    @Test
    fun depletePackageEarly_writesOffRemainingUnitsAsWastageAndEmptiesPackage() {
        // Given: Bottle A with 300 nominal drops, 240 drops taken, 60 drops remaining
        val bottleA = createLot(
            id = "bottle-a",
            initialQuantity = 300,
            remainingQuantity = 60,
            status = PackageStatus.OPENED
        )

        // When: User marks bottle as finished early
        val outcome = calculator.depletePackageEarly(
            lot = bottleA,
            reason = DepletionReason.EMPTY_NO_DROPS_LEFT,
            userNote = "Bottle ran dry early after 120 doses",
            nowEpochMs = nowMs
        )

        // Then: Remaining quantity is zeroed and status is EMPTY
        assertEquals(ScaledQuantity.ZERO, outcome.depletedLot.remainingQuantity)
        assertEquals(PackageStatus.EMPTY, outcome.depletedLot.packageStatus)
        assertEquals(ScaledQuantity.ofWhole(300), outcome.depletedLot.initialQuantity) // Preserved capacity
        assertEquals(nowMs, outcome.depletedLot.depletedAtEpochMs)
        assertEquals(DepletionReason.EMPTY_NO_DROPS_LEFT.ledgerReasonCode, outcome.depletedLot.depletionReason)
        assertEquals("Bottle ran dry early after 120 doses", outcome.depletedLot.depletionNote)

        // And: Compensating WASTAGE movement writes off remaining 60 drops
        val wastage = outcome.compensatingEntry
        assertNotNull(wastage)
        assertEquals(MovementType.WASTAGE, wastage!!.movementType)
        assertEquals(ScaledQuantity.ofWhole(60), wastage.quantity)
        assertEquals("bottle-a", wastage.lotId)
        assertEquals("med-glaucoma-drops", wastage.medicationId)
        assertTrue(wastage.isEstimate)
        assertTrue(wastage.reason?.contains("BOTTLE_EMPTY") == true)
        assertTrue(wastage.reason?.contains("Bottle ran dry early after 120 doses") == true)
    }

    @Test
    fun depletePackageEarly_whenAlreadyZeroRemaining_emitsNullWastageMovement() {
        val emptyBottle = createLot(
            id = "bottle-zero",
            initialQuantity = 300,
            remainingQuantity = 0,
            status = PackageStatus.OPENED
        )

        val outcome = calculator.depletePackageEarly(
            lot = emptyBottle,
            reason = DepletionReason.EMPTY_NO_DROPS_LEFT,
            nowEpochMs = nowMs
        )

        assertEquals(PackageStatus.EMPTY, outcome.depletedLot.packageStatus)
        assertEquals(ScaledQuantity.ZERO, outcome.depletedLot.remainingQuantity)
        assertNull(outcome.compensatingEntry)
    }

    @Test
    fun depletePackageEarly_stagesNextUnopenedFefoBottleWithoutAutoOpening() {
        val bottleA = createLot(id = "bA", remainingQuantity = 60, status = PackageStatus.OPENED)
        val bottleBSooner = createLot(
            id = "bB",
            remainingQuantity = 300,
            status = PackageStatus.UNOPENED,
            expiryDateEpochMs = nowMs + 60L * dayMs,
            openedAtEpochMs = null
        )
        val bottleCLater = createLot(
            id = "bC",
            remainingQuantity = 300,
            status = PackageStatus.UNOPENED,
            expiryDateEpochMs = nowMs + 180L * dayMs,
            openedAtEpochMs = null
        )
        val bottleDExpired = createLot(
            id = "bD",
            remainingQuantity = 300,
            status = PackageStatus.UNOPENED,
            expiryDateEpochMs = nowMs - dayMs,
            openedAtEpochMs = null
        )

        val outcome = calculator.depletePackageEarly(
            lot = bottleA,
            reason = DepletionReason.EMPTY_NO_DROPS_LEFT,
            availableLots = listOf(bottleA, bottleCLater, bottleBSooner, bottleDExpired),
            nowEpochMs = nowMs
        )

        // Then: Bottle B (earliest expiry among valid unopened) is staged next
        val staged = outcome.stagedNextLot
        assertNotNull(staged)
        assertEquals("bB", staged!!.id)
        assertEquals(PackageStatus.UNOPENED, staged.packageStatus) // Invariant: Not auto-opened!
        assertNull(staged.openedAtEpochMs)
    }

    @Test
    fun depletePackageEarly_whenNoUnopenedBottlesRemain_stagesNull() {
        val soleBottle = createLot(id = "sole", remainingQuantity = 40, status = PackageStatus.OPENED)

        val outcome = calculator.depletePackageEarly(
            lot = soleBottle,
            reason = DepletionReason.EXCESSIVE_DISPENSING,
            availableLots = listOf(soleBottle),
            nowEpochMs = nowMs
        )

        assertNull(outcome.stagedNextLot)
    }

    @Test
    fun reopenDepletedPackage_restoresPreviousBalanceAndOpensPackage() {
        val bottleA = createLot(
            id = "bottle-a",
            initialQuantity = 300,
            remainingQuantity = 60,
            status = PackageStatus.OPENED
        )

        val depletion = calculator.depletePackageEarly(
            lot = bottleA,
            reason = DepletionReason.LEAKING_OR_DAMAGED,
            userNote = "Fell on floor",
            nowEpochMs = nowMs
        )

        // When: User accidentally clicked finish and reverses
        val reopenOutcome = calculator.reopenDepletedPackage(
            lot = depletion.depletedLot,
            depletionLedgerEntry = depletion.compensatingEntry,
            reason = "Accidental click in UI",
            nowEpochMs = nowMs + 1000L
        )

        // Then: Status is restored to OPENED, balance restored to 60 drops
        assertEquals(PackageStatus.OPENED, reopenOutcome.reopenedLot.packageStatus)
        assertEquals(ScaledQuantity.ofWhole(60), reopenOutcome.reopenedLot.remainingQuantity)
        assertNull(reopenOutcome.reopenedLot.depletedAtEpochMs)
        assertNull(reopenOutcome.reopenedLot.depletionReason)
        assertNull(reopenOutcome.reopenedLot.depletionNote)

        // And: Compensating ADJUSTMENT_IN entry created in ledger
        val reversal = reopenOutcome.reversalEntry
        assertEquals(MovementType.ADJUSTMENT_IN, reversal.movementType)
        assertEquals(ScaledQuantity.ofWhole(60), reversal.quantity)
        assertEquals("bottle-a", reversal.lotId)
        assertTrue(reversal.reason?.contains("Accidental click in UI") == true)
    }

    @Test
    fun depletePackageEarly_rejectsAlreadyEmptyOrDiscardedPackage() {
        val emptyLot = createLot(id = "e1", remainingQuantity = 0, status = PackageStatus.EMPTY)
        val discardedLot = createLot(id = "d1", remainingQuantity = 0, status = PackageStatus.DISCARDED)

        assertThrows(IllegalStateException::class.java) {
            calculator.depletePackageEarly(emptyLot, DepletionReason.EMPTY_NO_DROPS_LEFT)
        }
        assertThrows(IllegalStateException::class.java) {
            calculator.depletePackageEarly(discardedLot, DepletionReason.LOST_OR_DISCARDED)
        }
    }

    @Test
    fun reopenDepletedPackage_rejectsNonEmptyPackage() {
        val openLot = createLot(id = "o1", remainingQuantity = 100, status = PackageStatus.OPENED)
        val unopenedLot = createLot(id = "u1", remainingQuantity = 300, status = PackageStatus.UNOPENED)

        assertThrows(IllegalStateException::class.java) {
            calculator.reopenDepletedPackage(openLot, null)
        }
        assertThrows(IllegalStateException::class.java) {
            calculator.reopenDepletedPackage(unopenedLot, null)
        }
    }

    @Test
    fun mapper_preservesDepletionAuditMetadataInSchemaV4() {
        val domainLot = StockLot(
            id = "lot-mapper-test",
            medicationId = "med-eye",
            lotNumber = "LOT-123",
            initialQuantity = ScaledQuantity.ofWhole(300),
            remainingQuantity = ScaledQuantity.ZERO,
            unit = StockUnit.DROPS,
            packageType = PackageType.BOTTLE,
            packageLabel = "Bottle #1",
            packageStatus = PackageStatus.EMPTY,
            openedAtEpochMs = nowMs - 15L * dayMs,
            discardAfterOpeningDays = 28,
            discardAfterOpeningEpochMs = nowMs + 13L * dayMs,
            depletedAtEpochMs = nowMs,
            depletionReason = "EXCESSIVE_DISPENSING",
            depletionNote = "Spilled several drops during admin"
        )

        // Convert to Room entity (v4 compatible)
        val entity = domainLot.toEntity()
        assertEquals(PackageStatus.EMPTY.name, entity.packageStatus)
        assertEquals(nowMs, entity.discardedAtEpochMs)
        assertEquals("DEPLETED[EXCESSIVE_DISPENSING]: Spilled several drops during admin", entity.discardReason)

        // Convert back to domain
        val mappedBack = entity.toDomain()
        assertEquals(PackageStatus.EMPTY, mappedBack.packageStatus)
        assertEquals(nowMs, mappedBack.depletedAtEpochMs)
        assertEquals("EXCESSIVE_DISPENSING", mappedBack.depletionReason)
        assertEquals("Spilled several drops during admin", mappedBack.depletionNote)
        assertNull(mappedBack.discardedAtEpochMs)
        assertNull(mappedBack.discardReason)
    }
}
