package com.example.nuvilo.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MedicationEditAndSafeDeleteDomainTest {

    private fun baseMedication(): Medication = Medication(
        id = "med_1",
        name = "Latanoprost",
        strength = "50 mcg/ml",
        form = MedicationForm.DROPS,
        stockUnit = StockUnit.DROPS,
        openingStock = ScaledQuantity.ofWhole(300),
        lowStockThreshold = ScaledQuantity.ofWhole(30),
        packagingConfig = PackagingConfig(
            packageType = PackageType.BOTTLE,
            consumptionUnit = StockUnit.DROPS,
            contentsPerPackage = ScaledQuantity.ofWhole(300),
            discardAfterOpeningDays = 28,
            trackingPrecision = TrackingPrecision.ESTIMATED
        ),
        administrationProfile = AdministrationProfile(
            site = AdministrationSite.BOTH_EYES,
            leftEyeQuantity = ScaledQuantity.ofWhole(1),
            rightEyeQuantity = ScaledQuantity.ofWhole(1)
        ),
        administrationRoute = AdministrationRoute.OPHTHALMIC,
        notificationsEnabled = true
    )

    private fun baseSchedule(): ScheduleDefinition = ScheduleDefinition(
        id = "sch_1",
        medicationId = "med_1",
        scheduleType = ScheduleType.DAILY,
        targetTimesMinuteOfDay = listOf(21 * 60),
        daysOfWeek = emptySet(),
        doseQuantityPerOccurrence = ScaledQuantity.ofWhole(2),
        startDateEpochDay = 20_000L
    )

    @Test
    fun editImpactAnalyzer_explainsPackageCapacityAndDoseChanges_withoutMutatingHistory() {
        val prevMed = baseMedication()
        val nextMed = prevMed.copy(
            packagingConfig = prevMed.packagingConfig?.copy(
                contentsPerPackage = ScaledQuantity.ofWhole(250),
                discardAfterOpeningDays = 14
            ),
            notificationsEnabled = false
        )
        val prevSch = baseSchedule()
        val nextSch = prevSch.copy(doseQuantityPerOccurrence = ScaledQuantity.ofWhole(1))

        val impact = MedicationEditImpactAnalyzer.analyze(
            previousMedication = prevMed,
            updatedMedication = nextMed,
            previousSchedules = listOf(prevSch),
            updatedSchedules = listOf(nextSch)
        )

        assertTrue(impact.hasPackageConfigChanged)
        assertTrue(impact.hasDoseOrScheduleChanged)
        assertFalse(impact.hasUnitChanged)
        assertTrue(impact.hasAnyCalculationImpact)
        assertTrue(impact.impactSummaries.any { it.contains("250") && it.contains("not rewritten") })
        assertTrue(impact.impactSummaries.any { it.contains("14 days after opening") })
        assertTrue(impact.impactSummaries.any { it.contains("Historical dose events remain unchanged") })
        assertTrue(impact.impactSummaries.any { it.contains("reminders are paused") })
    }

    @Test
    fun medicationHistoryCounts_distinguishesZeroHistoryVsAuditHistory() {
        val emptyCounts = MedicationHistoryCounts(medicationId = "med_1", scheduleCount = 1)
        assertFalse(emptyCounts.hasAnyHistory)
        assertFalse(emptyCounts.hasDoseHistory)
        assertFalse(emptyCounts.hasStockMovements)
        assertFalse(emptyCounts.hasPrescriptionHistory)

        val withDose = emptyCounts.copy(doseEventCount = 3)
        assertTrue(withDose.hasDoseHistory)
        assertTrue(withDose.hasAnyHistory)

        val withStock = emptyCounts.copy(stockMovementCount = 1)
        assertTrue(withStock.hasStockMovements)
        assertTrue(withStock.hasAnyHistory)

        val withRx = emptyCounts.copy(prescriptionCount = 1)
        assertTrue(withRx.hasPrescriptionHistory)
        assertTrue(withRx.hasAnyHistory)
    }

    @Test
    fun administrationRoute_defaultForForm_mapsSensibleClinicalRoutes() {
        assertEquals(AdministrationRoute.ORAL, AdministrationRoute.defaultForForm(MedicationForm.TABLET))
        assertEquals(AdministrationRoute.OPHTHALMIC, AdministrationRoute.defaultForForm(MedicationForm.DROPS))
        assertEquals(AdministrationRoute.INHALATION, AdministrationRoute.defaultForForm(MedicationForm.INHALER_PUFFS))
        assertEquals(AdministrationRoute.TOPICAL, AdministrationRoute.defaultForForm(MedicationForm.TOPICAL_CREAM))
        assertEquals(AdministrationRoute.TRANSDERMAL, AdministrationRoute.defaultForForm(MedicationForm.PATCH))
    }
}
