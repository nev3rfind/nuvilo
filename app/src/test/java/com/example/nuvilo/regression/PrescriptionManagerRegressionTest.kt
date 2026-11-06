package com.example.nuvilo.regression

import com.example.nuvilo.domain.model.Prescription
import com.example.nuvilo.domain.model.PrescriptionEvent
import com.example.nuvilo.domain.model.PrescriptionEventType
import com.example.nuvilo.domain.model.PrescriptionStatus
import com.example.nuvilo.domain.prescription.PrescriptionManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

/**
 * Regression Test Suite: Category A (Domain Models & Accounting)
 * Tests T048 to T055: Prescription Lifecycle, Repeat Requests, Collections, and Idempotency
 */
class PrescriptionManagerRegressionTest {

    private lateinit var manager: PrescriptionManager
    private val todayEpochDay = LocalDate.of(2026, 11, 6).toEpochDay()

    @Before
    fun setUp() {
        manager = PrescriptionManager()
    }

    private fun createPrescription(
        id: String = "rx_1",
        repeatsAuthorised: Int = 3,
        repeatsRemaining: Int = 3,
        hasPendingRepeatRequest: Boolean = false,
        status: PrescriptionStatus = PrescriptionStatus.ACTIVE,
        expiryDateEpochDay: Long? = LocalDate.of(2027, 1, 1).toEpochDay()
    ): Prescription = Prescription(
        id = id,
        medicationId = "med_1",
        referenceNumber = "NHS-RX-12345",
        issueDateEpochDay = LocalDate.of(2026, 1, 1).toEpochDay(),
        expiryDateEpochDay = expiryDateEpochDay,
        repeatsAuthorised = repeatsAuthorised,
        repeatsRemaining = repeatsRemaining,
        hasPendingRepeatRequest = hasPendingRepeatRequest,
        status = status
    )

    @Test
    fun t048_requestRepeatSetsPendingTrueAndPreservesRepeatsRemaining() {
        // T048: Request repeat sets hasPendingRepeatRequest = true and preserves repeatsRemaining
        val rx = createPrescription(repeatsRemaining = 3, hasPendingRepeatRequest = false)
        val result = manager.requestRepeat(
            prescription = rx,
            existingEvents = emptyList(),
            currentDateEpochDay = todayEpochDay,
            actionIdempotencyKey = "req_key_1"
        )

        assertTrue(result.updatedPrescription.hasPendingRepeatRequest)
        assertEquals(3, result.updatedPrescription.repeatsRemaining)
        assertNotNull(result.auditEvent)
        assertEquals(PrescriptionEventType.REPEAT_REQUESTED, result.auditEvent?.eventType)
        assertEquals(3, result.auditEvent?.repeatsRemainingAfter)
    }

    @Test
    fun t049_requestRepeatOnNonActivePrescriptionThrowsException() {
        // T049: Request repeat on non-active prescription throws IllegalArgumentException
        val expiredRx = createPrescription(
            status = PrescriptionStatus.EXPIRED,
            expiryDateEpochDay = LocalDate.of(2026, 1, 1).toEpochDay()
        )

        assertThrows(IllegalArgumentException::class.java) {
            manager.requestRepeat(
                prescription = expiredRx,
                existingEvents = emptyList(),
                currentDateEpochDay = todayEpochDay
            )
        }
    }

    @Test
    fun t050_requestRepeatWithZeroRepeatsRemainingThrowsException() {
        // T050: Request repeat with 0 repeats remaining throws IllegalArgumentException
        val rxNoRepeats = createPrescription(repeatsRemaining = 0)

        assertThrows(IllegalArgumentException::class.java) {
            manager.requestRepeat(
                prescription = rxNoRepeats,
                existingEvents = emptyList(),
                currentDateEpochDay = todayEpochDay
            )
        }
    }

    @Test
    fun t051_requestRepeatWhenAlreadyPendingReturnsUnchangedPrescription() {
        // T051: Request repeat when repeat is already pending returns unchanged prescription (no duplicate)
        val rxAlreadyPending = createPrescription(hasPendingRepeatRequest = true)
        val result = manager.requestRepeat(
            prescription = rxAlreadyPending,
            existingEvents = emptyList(),
            currentDateEpochDay = todayEpochDay
        )

        assertEquals(rxAlreadyPending, result.updatedPrescription)
        assertNull(result.auditEvent)
    }

    @Test
    fun t052_requestRepeatWithDuplicateIdempotencyKeyReturnsUnchangedPrescription() {
        // T052: Request repeat with duplicate actionIdempotencyKey returns unchanged prescription
        val rx = createPrescription(hasPendingRepeatRequest = false)
        val existingEvent = PrescriptionEvent(
            id = "ev_1",
            prescriptionId = rx.id,
            medicationId = rx.medicationId,
            eventType = PrescriptionEventType.REPEAT_REQUESTED,
            timestampEpochMs = 1000L,
            repeatsRemainingAfter = 3,
            actionIdempotencyKey = "key_duplicate"
        )

        val result = manager.requestRepeat(
            prescription = rx,
            existingEvents = listOf(existingEvent),
            currentDateEpochDay = todayEpochDay,
            actionIdempotencyKey = "key_duplicate"
        )

        assertEquals(rx, result.updatedPrescription)
        assertNull(result.auditEvent)
    }

    @Test
    fun t053_collectRepeatDecrementsRepeatsRemainingByOneAndEmitsEvent() {
        // T053: Collect repeat decrements repeatsRemaining by 1 and emits REPEAT_COLLECTED event
        val rx = createPrescription(repeatsRemaining = 3, hasPendingRepeatRequest = true)
        val result = manager.collectRepeat(
            prescription = rx,
            existingEvents = emptyList(),
            currentDateEpochDay = todayEpochDay,
            actionIdempotencyKey = "col_key_1"
        )

        assertEquals(2, result.updatedPrescription.repeatsRemaining)
        assertFalse(result.updatedPrescription.hasPendingRepeatRequest) // cleared!
        assertEquals(PrescriptionStatus.ACTIVE, result.updatedPrescription.status)
        assertNotNull(result.auditEvent)
        assertEquals(PrescriptionEventType.REPEAT_COLLECTED, result.auditEvent?.eventType)
        assertEquals(2, result.auditEvent?.repeatsRemainingAfter)
    }

    @Test
    fun t054_collectLastRepeatSetsStatusToCompletedAndZeroRemaining() {
        // T054: Collect repeat with last repeat sets status to COMPLETED and repeatsRemaining = 0
        val rxLastRepeat = createPrescription(repeatsRemaining = 1, hasPendingRepeatRequest = true)
        val result = manager.collectRepeat(
            prescription = rxLastRepeat,
            existingEvents = emptyList(),
            currentDateEpochDay = todayEpochDay
        )

        assertEquals(0, result.updatedPrescription.repeatsRemaining)
        assertEquals(PrescriptionStatus.COMPLETED, result.updatedPrescription.status)
        assertNotNull(result.auditEvent)
        assertEquals(0, result.auditEvent?.repeatsRemainingAfter)
    }

    @Test
    fun t055_collectRepeatWithDuplicateIdempotencyKeyDoesNotDoubleDecrement() {
        // T055: Collect repeat with duplicate actionIdempotencyKey does not double-decrement
        val rx = createPrescription(repeatsRemaining = 2)
        val priorCollectionEvent = PrescriptionEvent(
            id = "ev_col_1",
            prescriptionId = rx.id,
            medicationId = rx.medicationId,
            eventType = PrescriptionEventType.REPEAT_COLLECTED,
            timestampEpochMs = 1000L,
            repeatsRemainingAfter = 2,
            actionIdempotencyKey = "key_collect_once"
        )

        val result = manager.collectRepeat(
            prescription = rx,
            existingEvents = listOf(priorCollectionEvent),
            currentDateEpochDay = todayEpochDay,
            actionIdempotencyKey = "key_collect_once"
        )

        assertEquals(rx, result.updatedPrescription)
        assertEquals(2, result.updatedPrescription.repeatsRemaining)
        assertNull(result.auditEvent)
    }
}
