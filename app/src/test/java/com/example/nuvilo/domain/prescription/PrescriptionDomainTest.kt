package com.example.nuvilo.domain.prescription

import com.example.nuvilo.domain.model.Prescription
import com.example.nuvilo.domain.model.PrescriptionEventType
import com.example.nuvilo.domain.model.PrescriptionStatus
import com.example.nuvilo.domain.model.ScaledQuantity
import com.example.nuvilo.domain.model.StockUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class PrescriptionDomainTest {

    private val manager = PrescriptionManager()
    private val todayEpochDay = LocalDate.of(2026, 10, 30).toEpochDay()

    private fun samplePrescription(
        repeatsAuthorised: Int = 3,
        repeatsRemaining: Int = 3,
        expiryDateEpochDay: Long? = LocalDate.of(2027, 4, 30).toEpochDay(),
        status: PrescriptionStatus = PrescriptionStatus.ACTIVE,
        hasPendingRepeatRequest: Boolean = false
    ) = Prescription(
        id = "rx_1",
        medicationId = "med_1",
        referenceNumber = "RX-90012",
        issueDateEpochDay = LocalDate.of(2026, 10, 1).toEpochDay(),
        expiryDateEpochDay = expiryDateEpochDay,
        quantityPrescribed = ScaledQuantity.ofWhole(28),
        unit = StockUnit.TABLETS,
        repeatsAuthorised = repeatsAuthorised,
        repeatsRemaining = repeatsRemaining,
        hasPendingRepeatRequest = hasPendingRepeatRequest,
        pharmacyName = "High Street Pharmacy",
        notes = "Collect 5 days before running out",
        status = status
    )

    @Test
    fun requestRepeat_doesNotDecrementRepeatsRemaining_andPreventsDuplicatePendingRequest() {
        val rx = samplePrescription(repeatsAuthorised = 4, repeatsRemaining = 4)

        val firstRequest = manager.requestRepeat(
            prescription = rx,
            existingEvents = emptyList(),
            currentDateEpochDay = todayEpochDay,
            actionIdempotencyKey = "req_1"
        )

        assertEquals(4, firstRequest.updatedPrescription.repeatsRemaining)
        assertTrue(firstRequest.updatedPrescription.hasPendingRepeatRequest)
        assertNotNull(firstRequest.auditEvent)
        assertEquals(PrescriptionEventType.REPEAT_REQUESTED, firstRequest.auditEvent?.eventType)

        // Attempting to request again while pending does not create duplicate event or change count
        val duplicateRequest = manager.requestRepeat(
            prescription = firstRequest.updatedPrescription,
            existingEvents = listOfNotNull(firstRequest.auditEvent),
            currentDateEpochDay = todayEpochDay,
            actionIdempotencyKey = "req_2"
        )
        assertEquals(4, duplicateRequest.updatedPrescription.repeatsRemaining)
        assertNull(duplicateRequest.auditEvent)
    }

    @Test
    fun collectRepeat_decrementsRemainingOnce_protectsAgainstDuplicateActionKey_andTransitionsToCompletedAtZero() {
        val rx = samplePrescription(
            repeatsAuthorised = 2,
            repeatsRemaining = 2,
            hasPendingRepeatRequest = true
        )

        val firstCollect = manager.collectRepeat(
            prescription = rx,
            existingEvents = emptyList(),
            currentDateEpochDay = todayEpochDay,
            actionIdempotencyKey = "collect_action_1"
        )

        assertEquals(1, firstCollect.updatedPrescription.repeatsRemaining)
        assertFalse(firstCollect.updatedPrescription.hasPendingRepeatRequest)
        assertEquals(PrescriptionStatus.ACTIVE, firstCollect.updatedPrescription.status)
        assertEquals(PrescriptionEventType.REPEAT_COLLECTED, firstCollect.auditEvent?.eventType)

        // Re-submitting with the same actionIdempotencyKey must not decrement twice
        val duplicateCollect = manager.collectRepeat(
            prescription = firstCollect.updatedPrescription,
            existingEvents = listOfNotNull(firstCollect.auditEvent),
            currentDateEpochDay = todayEpochDay,
            actionIdempotencyKey = "collect_action_1"
        )
        assertEquals(1, duplicateCollect.updatedPrescription.repeatsRemaining)
        assertNull(duplicateCollect.auditEvent)

        // Collect final repeat -> transitions to COMPLETED and remaining is 0
        val finalCollect = manager.collectRepeat(
            prescription = firstCollect.updatedPrescription,
            existingEvents = listOfNotNull(firstCollect.auditEvent),
            currentDateEpochDay = todayEpochDay,
            actionIdempotencyKey = "collect_action_2"
        )
        assertEquals(0, finalCollect.updatedPrescription.repeatsRemaining)
        assertEquals(PrescriptionStatus.COMPLETED, finalCollect.updatedPrescription.status)
        assertFalse(finalCollect.updatedPrescription.canRequestRepeat(todayEpochDay))
        assertFalse(finalCollect.updatedPrescription.canCollectRepeat(todayEpochDay))
    }

    @Test(expected = IllegalArgumentException::class)
    fun collectRepeat_whenZeroRemaining_throwsAndNeverBecomesNegative() {
        val exhaustedRx = samplePrescription(repeatsAuthorised = 2, repeatsRemaining = 0)
        manager.collectRepeat(
            prescription = exhaustedRx,
            existingEvents = emptyList(),
            currentDateEpochDay = todayEpochDay
        )
    }

    @Test
    fun expiredPrescription_isNeverPresentedAsActive_andDisallowsRepeatActions() {
        val pastExpiry = todayEpochDay - 1L
        val rx = samplePrescription(
            expiryDateEpochDay = pastExpiry,
            status = PrescriptionStatus.ACTIVE
        )

        assertEquals(PrescriptionStatus.EXPIRED, rx.effectiveStatus(todayEpochDay))
        assertTrue(rx.isExpired(todayEpochDay))
        assertFalse(rx.canRequestRepeat(todayEpochDay))
        assertFalse(rx.canCollectRepeat(todayEpochDay))
    }

    @Test
    fun expiringSoon_detectsUpcomingExpiryWithinWindow() {
        val expiringIn5Days = samplePrescription(expiryDateEpochDay = todayEpochDay + 5L)
        val expiringIn30Days = samplePrescription(expiryDateEpochDay = todayEpochDay + 30L)

        assertTrue(expiringIn5Days.isExpiringSoon(todayEpochDay, warningWindowDays = 14L))
        assertEquals(5L, expiringIn5Days.daysUntilExpiry(todayEpochDay))
        assertFalse(expiringIn30Days.isExpiringSoon(todayEpochDay, warningWindowDays = 14L))
    }
}
