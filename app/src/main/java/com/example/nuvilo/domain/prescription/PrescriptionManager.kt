package com.example.nuvilo.domain.prescription

import com.example.nuvilo.domain.model.Prescription
import com.example.nuvilo.domain.model.PrescriptionEvent
import com.example.nuvilo.domain.model.PrescriptionEventType
import com.example.nuvilo.domain.model.PrescriptionStatus
import java.util.UUID

data class PrescriptionTransitionResult(
    val updatedPrescription: Prescription,
    val auditEvent: PrescriptionEvent?
)

/**
 * Pure Kotlin domain state machine enforcing Novelo's prescription and repeat tracking invariants:
 * 1. Requesting a repeat never decrements repeatsRemaining.
 * 2. Collecting a repeat decrements repeatsRemaining by 1 (never below 0).
 * 3. Duplicate actions (via idempotency key or repeated pending requests) never double-decrement.
 * 4. Expired prescriptions are never presented as ACTIVE and cannot have repeats requested/collected.
 * 5. Every state transition emits an immutable PrescriptionEvent audit record.
 */
class PrescriptionManager {

    fun requestRepeat(
        prescription: Prescription,
        existingEvents: List<PrescriptionEvent>,
        currentDateEpochDay: Long,
        nowEpochMs: Long = System.currentTimeMillis(),
        actionIdempotencyKey: String? = null,
        notes: String? = null
    ): PrescriptionTransitionResult {
        // Idempotency check against action key
        if (!actionIdempotencyKey.isNullOrBlank() &&
            existingEvents.any { it.actionIdempotencyKey == actionIdempotencyKey }
        ) {
            return PrescriptionTransitionResult(prescription, null)
        }

        // Duplicate request protection: if a repeat request is already pending, do not record another
        if (prescription.hasPendingRepeatRequest) {
            return PrescriptionTransitionResult(prescription, null)
        }

        val effective = prescription.effectiveStatus(currentDateEpochDay)
        require(effective == PrescriptionStatus.ACTIVE) {
            "Cannot request repeat on a ${effective.displayName.lowercase()} prescription."
        }
        require(prescription.repeatsRemaining > 0) {
            "No authorised repeats remaining on this prescription."
        }

        val updated = prescription.copy(
            hasPendingRepeatRequest = true,
            updatedAtEpochMs = nowEpochMs
        )
        val event = PrescriptionEvent(
            id = UUID.randomUUID().toString(),
            prescriptionId = prescription.id,
            medicationId = prescription.medicationId,
            eventType = PrescriptionEventType.REPEAT_REQUESTED,
            timestampEpochMs = nowEpochMs,
            repeatsRemainingAfter = updated.repeatsRemaining, // Unchanged!
            actionIdempotencyKey = actionIdempotencyKey,
            notes = notes ?: "Repeat prescription requested (${updated.repeatsRemaining} repeats remaining)"
        )
        return PrescriptionTransitionResult(updated, event)
    }

    fun collectRepeat(
        prescription: Prescription,
        existingEvents: List<PrescriptionEvent>,
        currentDateEpochDay: Long,
        nowEpochMs: Long = System.currentTimeMillis(),
        actionIdempotencyKey: String? = null,
        notes: String? = null
    ): PrescriptionTransitionResult {
        // Idempotency check against action key: never decrement twice if repeated
        if (!actionIdempotencyKey.isNullOrBlank() &&
            existingEvents.any { it.actionIdempotencyKey == actionIdempotencyKey }
        ) {
            return PrescriptionTransitionResult(prescription, null)
        }

        val effective = prescription.effectiveStatus(currentDateEpochDay)
        require(effective == PrescriptionStatus.ACTIVE) {
            "Cannot collect repeat for a ${effective.displayName.lowercase()} prescription."
        }
        require(prescription.repeatsRemaining > 0) {
            "No authorised repeats remaining to collect."
        }

        val newRemaining = (prescription.repeatsRemaining - 1).coerceAtLeast(0)
        val newStatus = if (newRemaining == 0) {
            PrescriptionStatus.COMPLETED
        } else {
            PrescriptionStatus.ACTIVE
        }

        val updated = prescription.copy(
            repeatsRemaining = newRemaining,
            hasPendingRepeatRequest = false,
            status = newStatus,
            updatedAtEpochMs = nowEpochMs
        )
        val event = PrescriptionEvent(
            id = UUID.randomUUID().toString(),
            prescriptionId = prescription.id,
            medicationId = prescription.medicationId,
            eventType = PrescriptionEventType.REPEAT_COLLECTED,
            timestampEpochMs = nowEpochMs,
            repeatsRemainingAfter = newRemaining,
            actionIdempotencyKey = actionIdempotencyKey,
            notes = notes ?: "Repeat collected ($newRemaining of ${prescription.repeatsAuthorised} repeats remaining)"
        )
        return PrescriptionTransitionResult(updated, event)
    }
}
