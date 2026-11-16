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
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val packagingConfig: PackagingConfig? = null,
    val administrationProfile: AdministrationProfile = AdministrationProfile(),
    val lowStockDaysThreshold: Int? = null,
    val administrationRoute: AdministrationRoute? = null,
    val notificationsEnabled: Boolean = true
) {
    val effectivePackagingConfig: PackagingConfig
        get() = packagingConfig ?: inferDefaultPackagingConfig(form, stockUnit)

    val effectiveConsumptionUnit: StockUnit
        get() = effectivePackagingConfig.consumptionUnit

    val effectiveAdministrationRoute: AdministrationRoute
        get() = administrationRoute ?: AdministrationRoute.defaultForForm(form)
}

data class StockLot(
    val id: String,
    val medicationId: String,
    val lotNumber: String? = null,
    val initialQuantity: ScaledQuantity,
    val remainingQuantity: ScaledQuantity,
    val unit: StockUnit,
    val expiryDateEpochMs: Long? = null,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val notes: String? = null,
    val packageType: PackageType? = null,
    val packageLabel: String? = null,
    val packageStatus: PackageStatus = PackageStatus.UNOPENED,
    val openedAtEpochMs: Long? = null,
    val discardAfterOpeningDays: Int? = null,
    val discardAfterOpeningEpochMs: Long? = null,
    val discardedAtEpochMs: Long? = null,
    val discardReason: String? = null,
    val depletedAtEpochMs: Long? = null,
    val depletionReason: String? = null,
    val depletionNote: String? = null
) {
    val effectiveDiscardAfterOpeningEpochMs: Long?
        get() = discardAfterOpeningEpochMs ?: if (openedAtEpochMs != null && discardAfterOpeningDays != null) {
            openedAtEpochMs + discardAfterOpeningDays.toLong() * 86_400_000L
        } else null
}

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
    val updatedAtEpochMs: Long = System.currentTimeMillis(),
    val repeatRequestedDateEpochDay: Long? = null,
    val dispensedDateEpochDay: Long? = null
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

/**
 * Counts of historical and configuration records associated with a medication,
 * used by the Safe Delete workflow to prevent accidental cascade deletion of audit history.
 */
data class MedicationHistoryCounts(
    val medicationId: String,
    val scheduleCount: Int = 0,
    val doseEventCount: Int = 0,
    val stockMovementCount: Int = 0,
    val stockLotCount: Int = 0,
    val prescriptionCount: Int = 0,
    val prescriptionEventCount: Int = 0
) {
    val hasDoseHistory: Boolean get() = doseEventCount > 0
    val hasStockMovements: Boolean get() = stockMovementCount > 0
    val hasPrescriptionHistory: Boolean get() = prescriptionCount > 0 || prescriptionEventCount > 0
    val hasAnyHistory: Boolean get() = hasDoseHistory || hasStockMovements || hasPrescriptionHistory
}

/**
 * Snapshot of a medication and its schedules when safely deleted with no historical records,
 * enabling a safe Undo via Snackbar.
 */
data class DeletedMedicationSnapshot(
    val medication: Medication,
    val schedules: List<ScheduleDefinition>
)

sealed interface MedicationDeleteOutcome {
    data class Deleted(
        val medicationId: String,
        val medicationName: String,
        val snapshot: DeletedMedicationSnapshot?,
        val canUndo: Boolean
    ) : MedicationDeleteOutcome

    data class RequiresExplicitConsent(
        val medicationId: String,
        val medicationName: String,
        val counts: MedicationHistoryCounts
    ) : MedicationDeleteOutcome
}

data class MedicationEditImpact(
    val hasPackageConfigChanged: Boolean,
    val hasDoseOrScheduleChanged: Boolean,
    val hasUnitChanged: Boolean,
    val impactSummaries: List<String>
) {
    val hasAnyCalculationImpact: Boolean
        get() = hasPackageConfigChanged || hasDoseOrScheduleChanged || hasUnitChanged
}

object MedicationEditImpactAnalyzer {
    fun analyze(
        previousMedication: Medication,
        updatedMedication: Medication,
        previousSchedules: List<ScheduleDefinition> = emptyList(),
        updatedSchedules: List<ScheduleDefinition> = emptyList()
    ): MedicationEditImpact {
        val summaries = mutableListOf<String>()

        val prevPkg = previousMedication.effectivePackagingConfig
        val nextPkg = updatedMedication.effectivePackagingConfig
        val unitChanged = previousMedication.effectiveConsumptionUnit != updatedMedication.effectiveConsumptionUnit
        val packageConfigChanged =
            prevPkg.packageType != nextPkg.packageType ||
                prevPkg.contentsPerPackage != nextPkg.contentsPerPackage ||
                prevPkg.discardAfterOpeningDays != nextPkg.discardAfterOpeningDays

        val prevDoseSum = previousSchedules.sumOf { it.doseQuantityPerOccurrence.rawScaledValue }
        val nextDoseSum = updatedSchedules.sumOf { it.doseQuantityPerOccurrence.rawScaledValue }
        val prevTimes = previousSchedules.flatMap { it.targetTimesMinuteOfDay }
        val nextTimes = updatedSchedules.flatMap { it.targetTimesMinuteOfDay }
        val doseOrScheduleChanged =
            previousSchedules.isNotEmpty() && updatedSchedules.isNotEmpty() && (
                prevDoseSum != nextDoseSum ||
                    prevTimes != nextTimes ||
                    previousMedication.administrationProfile != updatedMedication.administrationProfile
                )

        if (unitChanged) {
            summaries += "Stock unit changed from ${previousMedication.effectiveConsumptionUnit.abbreviation} to ${updatedMedication.effectiveConsumptionUnit.abbreviation} for future entries; past ledger entries keep their original recorded units."
        }
        if (prevPkg.packageType != nextPkg.packageType || prevPkg.contentsPerPackage != nextPkg.contentsPerPackage) {
            val capText = nextPkg.contentsPerPackage?.toDisplayString() ?: "configured"
            summaries += "Future package restocks and days-of-supply forecasts will use $capText ${updatedMedication.effectiveConsumptionUnit.abbreviation} per ${nextPkg.packageType.displayName.lowercase()}. Existing physical lots and past ledger entries are not rewritten."
        }
        if (prevPkg.discardAfterOpeningDays != nextPkg.discardAfterOpeningDays) {
            val daysText = nextPkg.discardAfterOpeningDays?.let { "$it days after opening" } ?: "no post-opening expiry limit"
            summaries += "Newly opened packages will use $daysText. Already-opened packages keep their existing open-expiry date unless edited individually."
        }
        if (doseOrScheduleChanged) {
            summaries += "Future scheduled doses and estimated days remaining will use the updated dose/schedule. Historical dose events remain unchanged."
        }
        if (previousMedication.notificationsEnabled != updatedMedication.notificationsEnabled) {
            summaries += if (updatedMedication.notificationsEnabled) {
                "Scheduled dose reminders are enabled for upcoming doses."
            } else {
                "Future dose reminders are paused while keeping schedule and stock tracking active."
            }
        }

        return MedicationEditImpact(
            hasPackageConfigChanged = packageConfigChanged,
            hasDoseOrScheduleChanged = doseOrScheduleChanged,
            hasUnitChanged = unitChanged,
            impactSummaries = summaries
        )
    }
}
