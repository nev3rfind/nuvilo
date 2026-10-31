package com.example.nuvilo.domain.model

data class Medication(
    val id: String,
    val name: String,
    val strength: String,
    val form: MedicationForm,
    val stockUnit: StockUnit,
    val openingStock: ScaledQuantity,
    val lowStockThreshold: ScaledQuantity,
    val recordingMode: RecordingMode = RecordingMode.CONFIRM,
    val notes: String = "",
    val colorTagHex: String = "#005EB8",
    val isArchived: Boolean = false,
    val createdAtEpochMs: Long = System.currentTimeMillis()
)

data class StockLot(
    val id: String,
    val medicationId: String,
    val lotNumber: String? = null,
    val initialQuantity: ScaledQuantity,
    val remainingQuantity: ScaledQuantity,
    val unit: StockUnit,
    val expiryDateEpochMs: Long? = null,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val notes: String? = null
)

data class StockLedgerEntry(
    val id: String,
    val medicationId: String,
    val timestampEpochMs: Long,
    val movementType: MovementType,
    val quantity: ScaledQuantity,
    val unit: StockUnit,
    val reason: String? = null,
    val doseOccurrenceId: String? = null,
    val restockLotNumber: String? = null,
    val lotId: String? = null,
    val isEstimate: Boolean = false
)

data class StockBalance(
    val currentBalance: ScaledQuantity,
    val unit: StockUnit,
    val isEstimate: Boolean,
    val isNegative: Boolean,
    val daysRemaining: Int?,
    val isLowStock: Boolean
)

data class DoseOccurrence(
    val occurrenceId: String,
    val medicationId: String,
    val scheduledEpochMs: Long,
    val doseQuantity: ScaledQuantity,
    val unit: StockUnit,
    val status: DoseStatus,
    val recordedEpochMs: Long? = null,
    val notes: String? = null,
    val correctionNotes: String? = null,
    val correctedAtEpochMs: Long? = null
)

data class ScheduleDefinition(
    val id: String,
    val medicationId: String,
    val scheduleType: ScheduleType,
    val targetTimesMinuteOfDay: List<Int>,
    val daysOfWeek: Set<Int>,
    val doseQuantityPerOccurrence: ScaledQuantity,
    val startDateEpochDay: Long,
    val endDateEpochDay: Long? = null,
    val instructions: String = ""
)

data class Prescription(
    val id: String,
    val medicationId: String,
    val referenceNumber: String? = null,
    val issueDateEpochDay: Long,
    val expiryDateEpochDay: Long? = null,
    val quantityPrescribed: ScaledQuantity? = null,
    val unit: StockUnit? = null,
    val repeatsAuthorised: Int = 0,
    val repeatsRemaining: Int = 0,
    val hasPendingRepeatRequest: Boolean = false,
    val pharmacyName: String? = null,
    val notes: String? = null,
    val status: PrescriptionStatus = PrescriptionStatus.ACTIVE,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val updatedAtEpochMs: Long = System.currentTimeMillis()
) {
    /**
     * Computes the authoritative status at [currentDateEpochDay].
     * Invariant: An expired prescription must never be presented as active.
     */
    fun effectiveStatus(currentDateEpochDay: Long): PrescriptionStatus {
        if (status == PrescriptionStatus.ARCHIVED) return PrescriptionStatus.ARCHIVED
        if (expiryDateEpochDay != null && currentDateEpochDay > expiryDateEpochDay) {
            return PrescriptionStatus.EXPIRED
        }
        if (status == PrescriptionStatus.EXPIRED) return PrescriptionStatus.EXPIRED
        if (status == PrescriptionStatus.COMPLETED) return PrescriptionStatus.COMPLETED
        return PrescriptionStatus.ACTIVE
    }

    fun isExpired(currentDateEpochDay: Long): Boolean =
        effectiveStatus(currentDateEpochDay) == PrescriptionStatus.EXPIRED

    fun daysUntilExpiry(currentDateEpochDay: Long): Long? =
        expiryDateEpochDay?.let { it - currentDateEpochDay }

    fun isExpiringSoon(currentDateEpochDay: Long, warningWindowDays: Long = 14L): Boolean {
        if (effectiveStatus(currentDateEpochDay) != PrescriptionStatus.ACTIVE) return false
        val daysLeft = daysUntilExpiry(currentDateEpochDay) ?: return false
        return daysLeft in 0L..warningWindowDays
    }

    fun canRequestRepeat(currentDateEpochDay: Long): Boolean =
        effectiveStatus(currentDateEpochDay) == PrescriptionStatus.ACTIVE &&
            repeatsRemaining > 0 &&
            !hasPendingRepeatRequest

    fun canCollectRepeat(currentDateEpochDay: Long): Boolean =
        effectiveStatus(currentDateEpochDay) == PrescriptionStatus.ACTIVE &&
            repeatsRemaining > 0
}

data class PrescriptionEvent(
    val id: String,
    val prescriptionId: String,
    val medicationId: String,
    val eventType: PrescriptionEventType,
    val timestampEpochMs: Long,
    val repeatsRemainingAfter: Int,
    val actionIdempotencyKey: String? = null,
    val notes: String? = null
)
