package com.example.nuvilo.regression

import com.example.nuvilo.domain.adherence.AdherenceSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * Regression Test Suite: Category A (Domain Models & Accounting)
 * Tests T039 to T047: Adherence Metric Calculation, Rounding, and Narrative Invariants
 */
class AdherenceCalculatorRegressionTest {

    private val startDate = LocalDate.of(2026, 11, 1)
    private val endDate = LocalDate.of(2026, 11, 7)

    private fun createSummary(
        confirmed: Int = 0,
        assumed: Int = 0,
        skipped: Int = 0,
        missed: Int = 0,
        pendingFuture: Int = 0,
        corrected: Int = 0
    ): AdherenceSummary = AdherenceSummary(
        periodLabel = "7 Days",
        startDate = startDate,
        endDate = endDate,
        confirmedCount = confirmed,
        assumedCount = assumed,
        skippedCount = skipped,
        missedCount = missed,
        pendingFutureCount = pendingFuture,
        correctedCount = corrected
    )

    @Test
    fun t039_emptyPeriodReturnsNullAdherencePercentageAndZeroCounts() {
        // T039: Empty period returns null adherence percentage and zero counts
        val summary = createSummary()
        assertEquals(0, summary.eligibleOccurrencesCount)
        assertEquals(0, summary.adherentCount)
        assertNull(summary.adherencePercentage)
        assertNull(summary.confirmedOnlyPercentage)
        assertTrue(summary.explanation.contains("No eligible past scheduled doses"))
    }

    @Test
    fun t040_onlyFutureOccurrencesInPeriodReturnsNullAdherencePercentage() {
        // T040: Only future occurrences in period returns null adherence percentage (future excluded)
        val summary = createSummary(pendingFuture = 14)
        assertEquals(0, summary.eligibleOccurrencesCount)
        assertNull(summary.adherencePercentage)
        assertNull(summary.confirmedOnlyPercentage)
        assertTrue(summary.explanation.contains("Future scheduled occurrences are excluded"))
    }

    @Test
    fun t041_allPastDosesConfirmedYields100PercentAdherence() {
        // T041: 100% adherence when all past doses are CONFIRMED
        val summary = createSummary(confirmed = 10)
        assertEquals(10, summary.eligibleOccurrencesCount)
        assertEquals(10, summary.adherentCount)
        assertEquals(100, summary.adherencePercentage)
        assertEquals(100, summary.confirmedOnlyPercentage)
    }

    @Test
    fun t042_overallAdherenceIncludesBothConfirmedAndAssumedInNumerator() {
        // T042: Overall adherence includes both CONFIRMED and ASSUMED doses in numerator
        // 5 confirmed + 5 assumed out of 10 total = 10/10 = 100% overall
        val summary = createSummary(confirmed = 5, assumed = 5)
        assertEquals(10, summary.eligibleOccurrencesCount)
        assertEquals(10, summary.adherentCount)
        assertEquals(100, summary.adherencePercentage)
    }

    @Test
    fun t043_confirmedOnlyPercentageExcludesAssumedFromNumerator() {
        // T043: confirmedOnlyPercentage excludes ASSUMED doses from numerator
        // 5 confirmed, 5 assumed out of 10 total = 5/10 = 50% confirmed only
        val summary = createSummary(confirmed = 5, assumed = 5)
        assertEquals(50, summary.confirmedOnlyPercentage)
    }

    @Test
    fun t044_skippedDosesCountInDenominatorLoweringAdherencePercentage() {
        // T044: SKIPPED doses count in denominator (lowers adherence percentage)
        // 8 confirmed, 2 skipped out of 10 total = 8/10 = 80%
        val summary = createSummary(confirmed = 8, skipped = 2)
        assertEquals(10, summary.eligibleOccurrencesCount)
        assertEquals(80, summary.adherencePercentage)
        assertEquals(80, summary.confirmedOnlyPercentage)
    }

    @Test
    fun t045_missedDosesCountInDenominatorLoweringAdherencePercentage() {
        // T045: MISSED doses count in denominator
        // 7 confirmed, 3 missed out of 10 total = 7/10 = 70%
        val summary = createSummary(confirmed = 7, missed = 3)
        assertEquals(10, summary.eligibleOccurrencesCount)
        assertEquals(70, summary.adherencePercentage)
    }

    @Test
    fun t046_halfUpIntegerRoundingInAdherencePercentageFormula() {
        // T046: Half-up integer rounding in adherence percentage formula
        // Formula: ((adherentCount * 100) + (eligibleOccurrencesCount / 2)) / eligibleOccurrencesCount
        // 2 adherent out of 3 eligible = 66.666...% -> rounds to 67%
        val summaryRoundUp = createSummary(confirmed = 2, missed = 1)
        assertEquals(3, summaryRoundUp.eligibleOccurrencesCount)
        assertEquals(67, summaryRoundUp.adherencePercentage)

        // 1 adherent out of 3 eligible = 33.333...% -> rounds to 33%
        val summaryRoundDown = createSummary(confirmed = 1, missed = 2)
        assertEquals(33, summaryRoundDown.adherencePercentage)
    }

    @Test
    fun t047_explanationTextAccuratelyFormatsSingularVsPluralDoses() {
        // T047: AdherenceSummary explanation text accurately formats singular vs plural doses
        val singular = createSummary(confirmed = 1)
        assertTrue(singular.explanation.contains("1 eligible past scheduled dose:"))

        val plural = createSummary(confirmed = 5)
        assertTrue(plural.explanation.contains("5 eligible past scheduled doses:"))
    }
}
