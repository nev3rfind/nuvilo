package com.example.nuvilo.domain.ppc

import java.time.LocalDate

/**
 * Official NHS Prescription Prepayment Certificate (PPC) and Free Prescription Exemption models.
 *
 * Note: Nuvilo tracks certificate dates locally based on user-entered records ("Based on your
 * recorded certificate dates") and never performs official NHS verification.
 */
enum class PpcCoverageType(
    val displayName: String,
    val shortLabel: String,
    val durationMonths: Int?
) {
    PPC_3_MONTH(
        displayName = "3-Month Prescription Prepayment Certificate (PPC)",
        shortLabel = "3-Month PPC",
        durationMonths = 3
    ),
    PPC_12_MONTH(
        displayName = "12-Month Prescription Prepayment Certificate (PPC)",
        shortLabel = "12-Month PPC",
        durationMonths = 12
    ),
    FREE_EXEMPTION(
        displayName = "Free NHS Prescription Exemption",
        shortLabel = "Free Exemption",
        durationMonths = null
    )
}

enum class ExemptionCategory(
    val displayName: String,
    val description: String
) {
    MEDICAL(
        displayName = "Medical Exemption Certificate",
        description = "Qualifying medical condition with a valid medical exemption certificate"
    ),
    MATERNITY(
        displayName = "Maternity Exemption Certificate",
        description = "Pregnant or have given birth in the last 12 months"
    ),
    HC2(
        displayName = "HC2 Certificate (Full Help)",
        description = "NHS Low Income Scheme HC2 certificate for full help with health costs"
    ),
    AGE_RELATED(
        displayName = "Age-Related Exemption",
        description = "Under 16, aged 16–18 in full-time education, or aged 60 and over"
    ),
    QUALIFYING_BENEFITS(
        displayName = "Qualifying Benefits",
        description = "Income Support, Universal Credit, Pension Credit, or other qualifying benefit"
    ),
    OTHER(
        displayName = "Other Exemption",
        description = "Other valid NHS prescription charge exemption"
    )
}

enum class PpcStatus(val displayName: String) {
    ACTIVE("Active"),
    EXPIRING_SOON("Expiring Soon"),
    EXPIRED("Expired"),
    NOT_YET_VALID("Not Yet Valid"),
    NO_RECORD("No Record")
}

data class PpcReminderConfig(
    val remind30DaysBefore: Boolean = true,
    val remind14DaysBefore: Boolean = true,
    val remind7DaysBefore: Boolean = true,
    val remindOnExpiryDay: Boolean = true,
    val reminderHour: Int = 9,
    val reminderMinute: Int = 0
) {
    fun activeOffsetsDays(): List<Int> = buildList {
        if (remind30DaysBefore) add(30)
        if (remind14DaysBefore) add(14)
        if (remind7DaysBefore) add(7)
        if (remindOnExpiryDay) add(0)
    }

    companion object {
        val ALL_STANDARD_OFFSETS = listOf(30, 14, 7, 0)
    }
}

data class PpcCertificate(
    val id: String,
    val coverageType: PpcCoverageType,
    val exemptionCategory: ExemptionCategory? = null,
    val startDate: LocalDate,
    val expiryDate: LocalDate? = null,
    val isOngoing: Boolean = false,
    val isManualExpiryOverride: Boolean = false,
    val referenceNumber: String? = null,
    val notes: String? = null,
    val reminderConfig: PpcReminderConfig = PpcReminderConfig(),
    val isArchived: Boolean = false,
    val renewedFromId: String? = null,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val updatedAtEpochMs: Long = System.currentTimeMillis()
)

data class PpcEvaluation(
    val certificate: PpcCertificate?,
    val status: PpcStatus,
    val remainingDays: Long?,
    val daysUntilStart: Long?,
    val effectiveExpiryDate: LocalDate?,
    val statusBasisNotice: String = BASIS_NOTICE
) {
    companion object {
        const val BASIS_NOTICE = "Based on your recorded certificate dates"
    }
}

data class PpcOverlapResult(
    val overlappingCertificate: PpcCertificate,
    val overlapStart: LocalDate,
    val overlapEnd: LocalDate?
)
