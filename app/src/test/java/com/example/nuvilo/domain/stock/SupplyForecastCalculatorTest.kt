package com.example.nuvilo.domain.stock

import com.example.nuvilo.domain.model.AdministrationProfile
import com.example.nuvilo.domain.model.AdministrationSite
import com.example.nuvilo.domain.model.Medication
import com.example.nuvilo.domain.model.MedicationForm
import com.example.nuvilo.domain.model.PackageStatus
import com.example.nuvilo.domain.model.PackageType
import com.example.nuvilo.domain.model.PackagingConfig
import com.example.nuvilo.domain.model.RecordingMode
import com.example.nuvilo.domain.model.ScaledQuantity
import com.example.nuvilo.domain.model.ScheduleDefinition
import com.example.nuvilo.domain.model.ScheduleType
import com.example.nuvilo.domain.model.StockLot
import com.example.nuvilo.domain.model.StockUnit
import com.example.nuvilo.domain.model.TrackingPrecision
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class SupplyForecastCalculatorTest {

    private val forecastCalculator = SupplyForecastCalculator()
    private val zoneId = ZoneOffset.UTC
    private val today = LocalDate.of(2026, 11, 12)
    private val nowMs = today.atTime(9, 0).atZone(zoneId).toInstant().toEpochMilli()
    private val dayMs = 86_400_000L

    private fun epochForDaysFromToday(days: Long): Long =
        today.plusDays(days).atTime(23, 59).atZone(zoneId).toInstant().toEpochMilli()

    private fun hydroForteMed(
        discardAfterOpeningDays: Int? = 28,
        lowStockThresholdDrops: Long = 20,
        lowStockDaysThreshold: Int? = 7,
        precision: TrackingPrecision = TrackingPrecision.ESTIMATED
    ) = Medication(
        id = "med-hydro",
        name = "Hydro Forte",
        strength = "0.3%",
        form = MedicationForm.DROPS,
        stockUnit = StockUnit.DROPS,
        openingStock = ScaledQuantity.ZERO,
        lowStockThreshold = ScaledQuantity.ofWhole(lowStockThresholdDrops),
        recordingMode = RecordingMode.CONFIRM,
        packagingConfig = PackagingConfig(
            packageType = PackageType.BOTTLE,
            consumptionUnit = StockUnit.DROPS,
            contentsPerPackage = ScaledQuantity.ofWhole(300),
            netContentLabel = "10 ml",
            discardAfterOpeningDays = discardAfterOpeningDays,
            trackingPrecision = precision
        ),
        administrationProfile = AdministrationProfile(
            site = AdministrationSite.BOTH_EYES,
            leftEyeQuantity = ScaledQuantity.ofWhole(1),
            rightEyeQuantity = ScaledQuantity.ofWhole(1)
        ),
        lowStockDaysThreshold = lowStockDaysThreshold
    )

    private fun onceDailySchedule(medId: String = "med-hydro") = ScheduleDefinition(
        id = "sch-1",
        medicationId = medId,
        scheduleType = ScheduleType.DAILY,
        targetTimesMinuteOfDay = listOf(480),
        daysOfWeek = emptySet(),
        doseQuantityPerOccurrence = ScaledQuantity.ofWhole(1),
        startDateEpochDay = today.toEpochDay() - 5
    )

    @Test
    fun bottleWith28DayDiscardRule_capsRunwayAt28DaysPerBottleAndReportsWastedDrops() {
        val med = hydroForteMed(discardAfterOpeningDays = 28)
        // 2 sealed bottles x 300 drops = 600 total drops, consumed at 2 drops/day.
        // Naive math: 600 / 2 = 300 days (WRONG!).
        // Realistic math: Bottle 1 lasts 28 days (56 drops used, 244 discarded), Bottle 2 opens day 29 and lasts 28 days (56 drops used, 244 discarded) = 56 days total!
        val b1 = StockLot(
            id = "b1",
            medicationId = med.id,
            lotNumber = "B1",
            initialQuantity = ScaledQuantity.ofWhole(300),
            remainingQuantity = ScaledQuantity.ofWhole(300),
            unit = StockUnit.DROPS,
            expiryDateEpochMs = epochForDaysFromToday(365),
            createdAtEpochMs = nowMs - dayMs,
            packageType = PackageType.BOTTLE,
            packageLabel = "Bottle #1",
            packageStatus = PackageStatus.UNOPENED,
            discardAfterOpeningDays = 28
        )
        val b2 = b1.copy(id = "b2", lotNumber = "B2", packageLabel = "Bottle #2")

        val forecast = forecastCalculator.calculateSupplyForecast(
            medication = med,
            schedules = listOf(onceDailySchedule()),
            lots = listOf(b1, b2),
            ledgerTotalUsable = ScaledQuantity.ofWhole(600),
            nowEpochMs = nowMs,
            zoneId = zoneId
        )

        assertEquals(56, forecast.daysOfSupply)
        assertEquals(ScaledQuantity.ofWhole(600), forecast.totalUsableUnits)
        assertEquals(ScaledQuantity.ofWhole(488), forecast.unitsLikelyLostToExpiryOrDiscard)
        assertEquals(0, forecast.openedPackagesCount)
        assertEquals(2, forecast.unopenedPackagesCount)
        assertEquals(TrackingPrecision.ESTIMATED, forecast.precision)
    }

    @Test
    fun alreadyOpenedBottle_exposesDiscardCountdownAndRespectsRemainingDaysInForecast() {
        val med = hydroForteMed(discardAfterOpeningDays = 28)
        // Bottle 1 opened 23 days ago -> 5 days remaining until discard limit (56 drops already consumed out of 300 -> 244 left in bottle 1).
        // 1 sealed bottle #2 (300 drops) will yield 28 more days. Total runway = 5 + 28 = 33 days!
        val openedAt = today.minusDays(23).atTime(8, 0).atZone(zoneId).toInstant().toEpochMilli()
        val discardAt = today.plusDays(5).atTime(23, 59).atZone(zoneId).toInstant().toEpochMilli()

        val b1 = StockLot(
            id = "b1",
            medicationId = med.id,
            lotNumber = "B1",
            initialQuantity = ScaledQuantity.ofWhole(300),
            remainingQuantity = ScaledQuantity.ofWhole(244),
            unit = StockUnit.DROPS,
            expiryDateEpochMs = epochForDaysFromToday(180),
            createdAtEpochMs = openedAt,
            packageType = PackageType.BOTTLE,
            packageLabel = "Bottle #1",
            packageStatus = PackageStatus.OPENED,
            openedAtEpochMs = openedAt,
            discardAfterOpeningDays = 28,
            discardAfterOpeningEpochMs = discardAt
        )
        val b2 = b1.copy(
            id = "b2",
            lotNumber = "B2",
            packageLabel = "Bottle #2",
            remainingQuantity = ScaledQuantity.ofWhole(300),
            packageStatus = PackageStatus.UNOPENED,
            openedAtEpochMs = null,
            discardAfterOpeningEpochMs = null
        )

        val forecast = forecastCalculator.calculateSupplyForecast(
            medication = med,
            schedules = listOf(onceDailySchedule()),
            lots = listOf(b1, b2),
            ledgerTotalUsable = ScaledQuantity.ofWhole(544),
            nowEpochMs = nowMs,
            zoneId = zoneId
        )

        assertEquals(33, forecast.daysOfSupply)
        assertEquals(1, forecast.openedPackagesCount)
        assertEquals(1, forecast.unopenedPackagesCount)
        assertNotNull(forecast.activeOpenedPackage)
        assertEquals("b1", forecast.activeOpenedPackage!!.id)
        assertEquals(5, forecast.daysUntilActivePackageDiscard)
    }

    @Test
    fun singleDoseContainers_200ContainersOnePerDay_returns200DaysExact() {
        val med = Medication(
            id = "med-sd",
            name = "Single-Dose Eye Drops",
            strength = "Preservative-free",
            form = MedicationForm.DROPS,
            stockUnit = StockUnit.CONTAINERS,
            openingStock = ScaledQuantity.ZERO,
            lowStockThreshold = ScaledQuantity.ofWhole(14),
            recordingMode = RecordingMode.CONFIRM,
            packagingConfig = PackagingConfig(
                packageType = PackageType.BOX_SINGLE_DOSE,
                consumptionUnit = StockUnit.CONTAINERS,
                contentsPerPackage = ScaledQuantity.ofWhole(200),
                discardAfterOpeningDays = null,
                trackingPrecision = TrackingPrecision.EXACT
            ),
            administrationProfile = AdministrationProfile(
                site = AdministrationSite.BOTH_EYES,
                containersPerAdministration = ScaledQuantity.ofWhole(1)
            )
        )
        val box = StockLot(
            id = "box1",
            medicationId = med.id,
            lotNumber = "BOX-1",
            initialQuantity = ScaledQuantity.ofWhole(200),
            remainingQuantity = ScaledQuantity.ofWhole(200),
            unit = StockUnit.CONTAINERS,
            expiryDateEpochMs = epochForDaysFromToday(500),
            createdAtEpochMs = nowMs,
            packageType = PackageType.BOX_SINGLE_DOSE,
            packageLabel = "Box #1",
            packageStatus = PackageStatus.UNOPENED
        )

        val forecast = forecastCalculator.calculateSupplyForecast(
            medication = med,
            schedules = listOf(onceDailySchedule(med.id)),
            lots = listOf(box),
            ledgerTotalUsable = ScaledQuantity.ofWhole(200),
            nowEpochMs = nowMs,
            zoneId = zoneId
        )

        assertEquals(200, forecast.daysOfSupply)
        assertEquals(ScaledQuantity.ZERO, forecast.unitsLikelyLostToExpiryOrDiscard)
        assertEquals(TrackingPrecision.EXACT, forecast.precision)
    }

    @Test
    fun prnOrUnscheduledMedication_returnsNullDaysOfSupplyWithoutCrashing() {
        val med = hydroForteMed()
        val prn = ScheduleDefinition(
            id = "sch-prn",
            medicationId = med.id,
            scheduleType = ScheduleType.PRN,
            targetTimesMinuteOfDay = emptyList(),
            daysOfWeek = emptySet(),
            doseQuantityPerOccurrence = ScaledQuantity.ofWhole(1),
            startDateEpochDay = today.toEpochDay()
        )
        val b1 = StockLot(
            id = "b1",
            medicationId = med.id,
            lotNumber = null,
            initialQuantity = ScaledQuantity.ofWhole(300),
            remainingQuantity = ScaledQuantity.ofWhole(300),
            unit = StockUnit.DROPS,
            expiryDateEpochMs = null,
            createdAtEpochMs = nowMs,
            packageType = PackageType.BOTTLE,
            packageStatus = PackageStatus.UNOPENED
        )

        val forecast = forecastCalculator.calculateSupplyForecast(
            medication = med,
            schedules = listOf(prn),
            lots = listOf(b1),
            ledgerTotalUsable = ScaledQuantity.ofWhole(300),
            nowEpochMs = nowMs,
            zoneId = zoneId
        )

        assertNull(forecast.daysOfSupply)
        assertEquals(ScaledQuantity.ofWhole(300), forecast.totalUsableUnits)
    }

    @Test
    fun expiredOrPastDiscardLot_isExcludedFromUsableSupply() {
        val med = hydroForteMed(discardAfterOpeningDays = 28)
        val expiredOpen = StockLot(
            id = "b-expired",
            medicationId = med.id,
            lotNumber = "B1",
            initialQuantity = ScaledQuantity.ofWhole(300),
            remainingQuantity = ScaledQuantity.ofWhole(250),
            unit = StockUnit.DROPS,
            expiryDateEpochMs = epochForDaysFromToday(90),
            createdAtEpochMs = nowMs - 30L * dayMs,
            packageType = PackageType.BOTTLE,
            packageStatus = PackageStatus.OPENED,
            openedAtEpochMs = nowMs - 30L * dayMs,
            discardAfterOpeningDays = 28,
            discardAfterOpeningEpochMs = nowMs - 2L * dayMs
        )
        val sealedGood = StockLot(
            id = "b-good",
            medicationId = med.id,
            lotNumber = "B2",
            initialQuantity = ScaledQuantity.ofWhole(300),
            remainingQuantity = ScaledQuantity.ofWhole(300),
            unit = StockUnit.DROPS,
            expiryDateEpochMs = epochForDaysFromToday(180),
            createdAtEpochMs = nowMs - dayMs,
            packageType = PackageType.BOTTLE,
            packageStatus = PackageStatus.UNOPENED,
            discardAfterOpeningDays = 28
        )

        val forecast = forecastCalculator.calculateSupplyForecast(
            medication = med,
            schedules = listOf(onceDailySchedule()),
            lots = listOf(expiredOpen, sealedGood),
            ledgerTotalUsable = ScaledQuantity.ofWhole(550),
            nowEpochMs = nowMs,
            zoneId = zoneId
        )

        // Only b-good (28 days max because of 28-day discard rule) is usable! b-expired must be flagged for discard.
        assertEquals(ScaledQuantity.ofWhole(300), forecast.totalUsableUnits)
        assertEquals(28, forecast.daysOfSupply)
        assertEquals(1, forecast.packagesNeedingDiscardCount)
        assertTrue(forecast.needsAttentionForExpiredOrOpenLimit)
    }
}
