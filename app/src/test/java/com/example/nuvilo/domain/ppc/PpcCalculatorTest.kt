package com.example.nuvilo.domain.ppc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class PpcCalculatorTest {

    private val calculator = PpcCalculator()

    @Test
    fun calculateExpiryDate_3MonthPpc_standardAndYearTransition() {
        assertEquals(
            LocalDate.of(2026, 3, 31),
            calculator.calculateExpiryDate(PpcCoverageType.PPC_3_MONTH, LocalDate.of(2026, 1, 1))
        )
        assertEquals(
            LocalDate.of(2027, 2, 14),
            calculator.calculateExpiryDate(PpcCoverageType.PPC_3_MONTH, LocalDate.of(2026, 11, 15))
        )
    }

    @Test
    fun calculateExpiryDate_3MonthPpc_monthEndAndLeapYear() {
        // Jan 31 + 3 months -> Apr 30 - 1 day = Apr 29
        assertEquals(
            LocalDate.of(2026, 4, 29),
            calculator.calculateExpiryDate(PpcCoverageType.PPC_3_MONTH, LocalDate.of(2026, 1, 31))
        )
        // Nov 30 + 3 months in leap year 2024 -> Feb 29 - 1 day = Feb 28
        assertEquals(
            LocalDate.of(2024, 2, 28),
            calculator.calculateExpiryDate(PpcCoverageType.PPC_3_MONTH, LocalDate.of(2023, 11, 30))
        )
        // Nov 30 + 3 months in non-leap year 2026 -> Feb 28 - 1 day = Feb 27
        assertEquals(
            LocalDate.of(2026, 2, 27),
            calculator.calculateExpiryDate(PpcCoverageType.PPC_3_MONTH, LocalDate.of(2025, 11, 30))
        )
    }

    @Test
    fun calculateExpiryDate_12MonthPpc_standardAndLeapYear() {
        assertEquals(
            LocalDate.of(2025, 3, 31),
            calculator.calculateExpiryDate(PpcCoverageType.PPC_12_MONTH, LocalDate.of(2024, 4, 1))
        )
        // Crossing into leap day 2024-02-29
        assertEquals(
            LocalDate.of(2024, 2, 29),
            calculator.calculateExpiryDate(PpcCoverageType.PPC_12_MONTH, LocalDate.of(2023, 3, 1))
        )
        // Starting on leap day 2024-02-29 -> 2025-02-28 - 1 day = 2025-02-27
        assertEquals(
            LocalDate.of(2025, 2, 27),
            calculator.calculateExpiryDate(PpcCoverageType.PPC_12_MONTH, LocalDate.of(2024, 2, 29))
        )
    }

    @Test
    fun calculateExpiryDate_manualOverrideAndOngoingExemption() {
        val override = LocalDate.of(2026, 10, 15)
        assertEquals(
            override,
            calculator.calculateExpiryDate(
                coverageType = PpcCoverageType.PPC_12_MONTH,
                startDate = LocalDate.of(2026, 1, 1),
                manualExpiryOverride = override
            )
        )
        assertNull(
            calculator.calculateExpiryDate(
                coverageType = PpcCoverageType.FREE_EXEMPTION,
                startDate = LocalDate.of(2026, 1, 1),
                isOngoing = true
            )
        )
    }

    @Test
    fun evaluateCertificate_allStatusesAndBoundaries() {
        val today = LocalDate.of(2026, 6, 1)

        // NO_RECORD
        val noRecord = calculator.evaluateCertificate(null, today)
        assertEquals(PpcStatus.NO_RECORD, noRecord.status)
        assertEquals(PpcEvaluation.BASIS_NOTICE, noRecord.statusBasisNotice)

        // NOT_YET_VALID
        val futureCert = PpcCertificate(
            id = "future",
            coverageType = PpcCoverageType.PPC_3_MONTH,
            startDate = LocalDate.of(2026, 6, 10),
            expiryDate = LocalDate.of(2026, 9, 9)
        )
        val futureEval = calculator.evaluateCertificate(futureCert, today)
        assertEquals(PpcStatus.NOT_YET_VALID, futureEval.status)
        assertEquals(9L, futureEval.daysUntilStart)

        // ACTIVE (31 days remaining)
        val activeCert = PpcCertificate(
            id = "active",
            coverageType = PpcCoverageType.PPC_3_MONTH,
            startDate = LocalDate.of(2026, 4, 3),
            expiryDate = LocalDate.of(2026, 7, 2)
        )
        val activeEval = calculator.evaluateCertificate(activeCert, today)
        assertEquals(PpcStatus.ACTIVE, activeEval.status)
        assertEquals(31L, activeEval.remainingDays)

        // EXPIRING_SOON (30 days remaining boundary)
        val soon30 = activeCert.copy(expiryDate = LocalDate.of(2026, 7, 1))
        val soon30Eval = calculator.evaluateCertificate(soon30, today)
        assertEquals(PpcStatus.EXPIRING_SOON, soon30Eval.status)
        assertEquals(30L, soon30Eval.remainingDays)

        // EXPIRING_SOON (0 days remaining - expires today)
        val expiresToday = activeCert.copy(expiryDate = today)
        val expiresTodayEval = calculator.evaluateCertificate(expiresToday, today)
        assertEquals(PpcStatus.EXPIRING_SOON, expiresTodayEval.status)
        assertEquals(0L, expiresTodayEval.remainingDays)

        // EXPIRED (-1 day)
        val expiredYesterday = activeCert.copy(expiryDate = LocalDate.of(2026, 5, 31))
        val expiredEval = calculator.evaluateCertificate(expiredYesterday, today)
        assertEquals(PpcStatus.EXPIRED, expiredEval.status)
        assertEquals(-1L, expiredEval.remainingDays)

        // Ongoing FREE_EXEMPTION
        val ongoingExemption = PpcCertificate(
            id = "exempt",
            coverageType = PpcCoverageType.FREE_EXEMPTION,
            exemptionCategory = ExemptionCategory.MEDICAL,
            startDate = LocalDate.of(2025, 1, 1),
            expiryDate = null,
            isOngoing = true
        )
        val ongoingEval = calculator.evaluateCertificate(ongoingExemption, today)
        assertEquals(PpcStatus.ACTIVE, ongoingEval.status)
        assertNull(ongoingEval.remainingDays)
        assertNull(ongoingEval.effectiveExpiryDate)
    }

    @Test
    fun selectPrimaryEvaluation_prioritizesActiveAndIgnoresArchived() {
        val today = LocalDate.of(2026, 6, 1)
        val archivedActive = PpcCertificate(
            id = "arch",
            coverageType = PpcCoverageType.PPC_12_MONTH,
            startDate = LocalDate.of(2026, 1, 1),
            expiryDate = LocalDate.of(2026, 12, 31),
            isArchived = true
        )
        val expired = PpcCertificate(
            id = "exp",
            coverageType = PpcCoverageType.PPC_3_MONTH,
            startDate = LocalDate.of(2026, 1, 1),
            expiryDate = LocalDate.of(2026, 3, 31)
        )
        val soon = PpcCertificate(
            id = "soon",
            coverageType = PpcCoverageType.PPC_3_MONTH,
            startDate = LocalDate.of(2026, 3, 20),
            expiryDate = LocalDate.of(2026, 6, 19)
        )

        val selected = calculator.selectPrimaryEvaluation(listOf(archivedActive, expired, soon), today)
        assertEquals("soon", selected.certificate?.id)
        assertEquals(PpcStatus.EXPIRING_SOON, selected.status)
    }

    @Test
    fun findOverlaps_detectsOverlapsAndAllowsBackToBack() {
        val existing = PpcCertificate(
            id = "c1",
            coverageType = PpcCoverageType.PPC_3_MONTH,
            startDate = LocalDate.of(2026, 1, 1),
            expiryDate = LocalDate.of(2026, 3, 31)
        )

        // Back-to-back starting April 1 -> no overlap
        val backToBack = calculator.findOverlaps(
            candidateStart = LocalDate.of(2026, 4, 1),
            candidateExpiry = LocalDate.of(2026, 6, 30),
            existingCertificates = listOf(existing)
        )
        assertTrue(backToBack.isEmpty())

        // Overlapping on March 15..31
        val overlapping = calculator.findOverlaps(
            candidateStart = LocalDate.of(2026, 3, 15),
            candidateExpiry = LocalDate.of(2026, 6, 14),
            existingCertificates = listOf(existing)
        )
        assertEquals(1, overlapping.size)
        assertEquals(LocalDate.of(2026, 3, 15), overlapping.first().overlapStart)
        assertEquals(LocalDate.of(2026, 3, 31), overlapping.first().overlapEnd)
    }

    @Test
    fun buildRenewalDraft_startsDayAfterExpiryOrTodayIfExpired() {
        val today = LocalDate.of(2026, 6, 1)
        val expiringSoon = PpcCertificate(
            id = "old1",
            coverageType = PpcCoverageType.PPC_12_MONTH,
            startDate = LocalDate.of(2025, 6, 15),
            expiryDate = LocalDate.of(2026, 6, 14)
        )
        val draft = calculator.buildRenewalDraft(expiringSoon, newId = "new1", today = today)
        assertEquals(LocalDate.of(2026, 6, 15), draft.startDate)
        assertEquals(LocalDate.of(2027, 6, 14), draft.expiryDate)
        assertEquals("old1", draft.renewedFromId)
    }
}
