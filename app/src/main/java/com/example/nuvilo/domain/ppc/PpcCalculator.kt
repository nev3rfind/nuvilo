package com.example.nuvilo.domain.ppc

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Pure Kotlin java.time.LocalDate calculator for NHS Prescription Prepayment Certificates (PPC)
 * and Free Prescription Exemptions.
 *
 * All calculations are deterministic, calendar-month accurate, and strictly offline.
 */
class PpcCalculator {

    companion object {
        const val DEFAULT_EXPIRING_SOON_DAYS = 30L
    }

    /**
     * Calculates the inclusive expiry date for a certificate starting on [startDate].
     *
     * For 3-month and 12-month PPCs, NHSBSA validity runs for N calendar months inclusive
     * of the start date: `startDate.plusMonths(N).minusDays(1)`.
     * Example: 1 April 2024 + 12 months -> 31 March 2025.
     *
     * If [manualExpiryOverride] is provided, it takes precedence over the calculated date.
     * For [PpcCoverageType.FREE_EXEMPTION] with [isOngoing] = true and no manual override,
     * returns null (no expiry date).
     */
    fun calculateExpiryDate(
        coverageType: PpcCoverageType,
        startDate: LocalDate,
        isOngoing: Boolean = false,
        manualExpiryOverride: LocalDate? = null
    ): LocalDate? {
        if (manualExpiryOverride != null) {
            return manualExpiryOverride
        }
        val months = coverageType.durationMonths ?: return if (isOngoing) null else null
        return startDate.plusMonths(months.toLong()).minusDays(1)
    }

    /**
     * Evaluates the status and remaining days of a single [certificate] relative to [today].
     *
     * - `remainingDays == 0L`: expires today (`EXPIRING_SOON`).
     * - `remainingDays < 0L`: expired (`EXPIRED`).
     * - `today < startDate`: not yet valid (`NOT_YET_VALID`).
     * - Ongoing exemption without expiry date: `ACTIVE` with `remainingDays == null`.
     */
    fun evaluateCertificate(
        certificate: PpcCertificate?,
        today: LocalDate = LocalDate.now(),
        expiringSoonThresholdDays: Long = DEFAULT_EXPIRING_SOON_DAYS
    ): PpcEvaluation {
        if (certificate == null) {
            return PpcEvaluation(
                certificate = null,
                status = PpcStatus.NO_RECORD,
                remainingDays = null,
                daysUntilStart = null,
                effectiveExpiryDate = null
            )
        }

        val effectiveExpiry = certificate.expiryDate ?: calculateExpiryDate(
            coverageType = certificate.coverageType,
            startDate = certificate.startDate,
            isOngoing = certificate.isOngoing
        )

        if (today.isBefore(certificate.startDate)) {
            val daysUntilStart = ChronoUnit.DAYS.between(today, certificate.startDate)
            val remainingDays = effectiveExpiry?.let { ChronoUnit.DAYS.between(today, it) }
            return PpcEvaluation(
                certificate = certificate,
                status = PpcStatus.NOT_YET_VALID,
                remainingDays = remainingDays,
                daysUntilStart = daysUntilStart,
                effectiveExpiryDate = effectiveExpiry
            )
        }

        if (effectiveExpiry == null) {
            return PpcEvaluation(
                certificate = certificate,
                status = PpcStatus.ACTIVE,
                remainingDays = null,
                daysUntilStart = null,
                effectiveExpiryDate = null
            )
        }

        val remainingDays = ChronoUnit.DAYS.between(today, effectiveExpiry)
        val status = when {
            remainingDays < 0L -> PpcStatus.EXPIRED
            remainingDays <= expiringSoonThresholdDays -> PpcStatus.EXPIRING_SOON
            else -> PpcStatus.ACTIVE
        }

        return PpcEvaluation(
            certificate = certificate,
            status = status,
            remainingDays = remainingDays,
            daysUntilStart = null,
            effectiveExpiryDate = effectiveExpiry
        )
    }

    /**
     * Selects the primary certificate evaluation to surface on the Dashboard and management header
     * from all recorded certificates (ignoring archived ones).
     *
     * Priority:
     * 1. ACTIVE (ongoing or longest remaining validity)
     * 2. EXPIRING_SOON (longest remaining validity)
     * 3. NOT_YET_VALID (nearest upcoming start date)
     * 4. EXPIRED (most recently expired)
     * 5. NO_RECORD
     */
    fun selectPrimaryEvaluation(
        certificates: List<PpcCertificate>,
        today: LocalDate = LocalDate.now(),
        expiringSoonThresholdDays: Long = DEFAULT_EXPIRING_SOON_DAYS
    ): PpcEvaluation {
        val activePool = certificates.filter { !it.isArchived }
        if (activePool.isEmpty()) {
            return evaluateCertificate(null, today, expiringSoonThresholdDays)
        }

        val evaluations = activePool.map { evaluateCertificate(it, today, expiringSoonThresholdDays) }

        evaluations
            .filter { it.status == PpcStatus.ACTIVE }
            .maxWithOrNull(compareBy<PpcEvaluation> { it.remainingDays == null }.thenBy { it.remainingDays ?: Long.MAX_VALUE })
            ?.let { return it }

        evaluations
            .filter { it.status == PpcStatus.EXPIRING_SOON }
            .maxByOrNull { it.remainingDays ?: Long.MIN_VALUE }
            ?.let { return it }

        evaluations
            .filter { it.status == PpcStatus.NOT_YET_VALID }
            .minByOrNull { it.daysUntilStart ?: Long.MAX_VALUE }
            ?.let { return it }

        return evaluations
            .filter { it.status == PpcStatus.EXPIRED }
            .maxByOrNull { it.remainingDays ?: Long.MIN_VALUE }
            ?: evaluateCertificate(null, today, expiringSoonThresholdDays)
    }

    /**
     * Checks whether a candidate certificate date range overlaps any existing non-archived certificate.
     */
    fun findOverlaps(
        candidateStart: LocalDate,
        candidateExpiry: LocalDate?,
        existingCertificates: List<PpcCertificate>,
        excludeCertificateId: String? = null
    ): List<PpcOverlapResult> {
        val candidateEnd = candidateExpiry ?: LocalDate.MAX
        if (candidateEnd.isBefore(candidateStart)) return emptyList()

        return existingCertificates
            .filter { !it.isArchived && it.id != excludeCertificateId }
            .mapNotNull { existing ->
                val existingEnd = existing.expiryDate ?: LocalDate.MAX
                if (existingEnd.isBefore(existing.startDate)) return@mapNotNull null

                val overlapStart = maxOf(candidateStart, existing.startDate)
                val overlapEnd = minOf(candidateEnd, existingEnd)

                if (!overlapStart.isAfter(overlapEnd)) {
                    PpcOverlapResult(
                        overlappingCertificate = existing,
                        overlapStart = overlapStart,
                        overlapEnd = if (overlapEnd == LocalDate.MAX) null else overlapEnd
                    )
                } else {
                    null
                }
            }
    }

    /**
     * Builds a renewal draft from an existing certificate.
     * Starts the day after the previous certificate's expiry (or [today] if already expired/ongoing).
     */
    fun buildRenewalDraft(
        previous: PpcCertificate,
        newId: String,
        today: LocalDate = LocalDate.now(),
        nowEpochMs: Long = System.currentTimeMillis()
    ): PpcCertificate {
        val nextStart = previous.expiryDate
            ?.plusDays(1)
            ?.let { if (it.isBefore(today)) today else it }
            ?: today

        val nextExpiry = calculateExpiryDate(
            coverageType = previous.coverageType,
            startDate = nextStart,
            isOngoing = previous.isOngoing,
            manualExpiryOverride = null
        )

        return PpcCertificate(
            id = newId,
            coverageType = previous.coverageType,
            exemptionCategory = previous.exemptionCategory,
            startDate = nextStart,
            expiryDate = nextExpiry,
            isOngoing = previous.isOngoing,
            isManualExpiryOverride = false,
            referenceNumber = null,
            notes = previous.notes,
            reminderConfig = previous.reminderConfig,
            isArchived = false,
            renewedFromId = previous.id,
            createdAtEpochMs = nowEpochMs,
            updatedAtEpochMs = nowEpochMs
        )
    }
}
