package com.example.nuvilo.domain.stock

import com.example.nuvilo.domain.model.AdministrationProfile
import com.example.nuvilo.domain.model.AdministrationSite
import com.example.nuvilo.domain.model.GelTrackingMode
import com.example.nuvilo.domain.model.Medication
import com.example.nuvilo.domain.model.MedicationForm
import com.example.nuvilo.domain.model.PackageType
import com.example.nuvilo.domain.model.PackagingConfig
import com.example.nuvilo.domain.model.RecordingMode
import com.example.nuvilo.domain.model.ScaledQuantity
import com.example.nuvilo.domain.model.ScheduleDefinition
import com.example.nuvilo.domain.model.ScheduleType
import com.example.nuvilo.domain.model.StockUnit
import com.example.nuvilo.domain.model.TrackingPrecision
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ConsumptionCalculatorTest {

    private val calculator = ConsumptionCalculator()
    private val validator = UnitIntegrityValidator()

    private fun eyeDropMedication(
        site: AdministrationSite = AdministrationSite.BOTH_EYES,
        leftQty: ScaledQuantity? = ScaledQuantity.ofWhole(1),
        rightQty: ScaledQuantity? = ScaledQuantity.ofWhole(1),
        containersPerAdmin: ScaledQuantity? = null,
        packageType: PackageType = PackageType.BOTTLE,
        unit: StockUnit = StockUnit.DROPS,
        contentsPerPackage: ScaledQuantity? = ScaledQuantity.ofWhole(300),
        discardDays: Int? = 28,
        precision: TrackingPrecision = TrackingPrecision.ESTIMATED
    ) = Medication(
        id = "med-eye-1",
        name = "Hydro Forte",
        strength = "0.3%",
        form = MedicationForm.DROPS,
        stockUnit = unit,
        openingStock = ScaledQuantity.ZERO,
        lowStockThreshold = ScaledQuantity.ofWhole(30),
        recordingMode = RecordingMode.CONFIRM,
        packagingConfig = PackagingConfig(
            packageType = packageType,
            consumptionUnit = unit,
            contentsPerPackage = contentsPerPackage,
            netContentLabel = "10 ml",
            discardAfterOpeningDays = discardDays,
            trackingPrecision = precision
        ),
        administrationProfile = AdministrationProfile(
            site = site,
            leftEyeQuantity = leftQty,
            rightEyeQuantity = rightQty,
            containersPerAdministration = containersPerAdmin
        )
    )

    private fun dailySchedule(
        medId: String = "med-eye-1",
        doseQty: ScaledQuantity = ScaledQuantity.ofWhole(1),
        times: List<Int> = listOf(480, 1200)
    ) = ScheduleDefinition(
        id = "sch-1",
        medicationId = medId,
        scheduleType = ScheduleType.DAILY,
        targetTimesMinuteOfDay = times,
        daysOfWeek = emptySet(),
        doseQuantityPerOccurrence = doseQty,
        startDateEpochDay = 20_000L
    )

    @Test
    fun multiDoseEyeDrops_bothEyes_sumsLeftAndRightDrops() {
        val med = eyeDropMedication(
            site = AdministrationSite.BOTH_EYES,
            leftQty = ScaledQuantity.ofWhole(1),
            rightQty = ScaledQuantity.ofWhole(1)
        )
        val result = calculator.calculateOccurrenceConsumption(med, dailySchedule())

        assertEquals(ScaledQuantity.ofWhole(2), result.quantity)
        assertEquals(StockUnit.DROPS, result.unit)
        assertEquals(TrackingPrecision.ESTIMATED, result.precision)
        assertTrue(result.summaryLabel.contains("1 left"))
        assertTrue(result.summaryLabel.contains("1 right"))
    }

    @Test
    fun multiDoseEyeDrops_asymmetricEyes_sumsDistinctLeftAndRightDrops() {
        val med = eyeDropMedication(
            site = AdministrationSite.BOTH_EYES,
            leftQty = ScaledQuantity.ofWhole(2),
            rightQty = ScaledQuantity.ofWhole(1)
        )
        val result = calculator.calculateOccurrenceConsumption(med, dailySchedule())

        assertEquals(ScaledQuantity.ofWhole(3), result.quantity)
        assertEquals(StockUnit.DROPS, result.unit)
    }

    @Test
    fun multiDoseEyeDrops_leftEyeOnly_consumesLeftQuantityOnly() {
        val med = eyeDropMedication(
            site = AdministrationSite.LEFT_EYE,
            leftQty = ScaledQuantity.ofWhole(2),
            rightQty = null
        )
        val result = calculator.calculateOccurrenceConsumption(med, dailySchedule())

        assertEquals(ScaledQuantity.ofWhole(2), result.quantity)
        assertEquals(StockUnit.DROPS, result.unit)
    }

    @Test
    fun singleDoseEyeContainers_bothEyes_doesNotMultiplyContainersByTwo() {
        val med = eyeDropMedication(
            site = AdministrationSite.BOTH_EYES,
            leftQty = ScaledQuantity.ofWhole(1),
            rightQty = ScaledQuantity.ofWhole(1),
            containersPerAdmin = ScaledQuantity.ofWhole(1),
            packageType = PackageType.BOX_SINGLE_DOSE,
            unit = StockUnit.CONTAINERS,
            contentsPerPackage = ScaledQuantity.ofWhole(30),
            discardDays = null,
            precision = TrackingPrecision.EXACT
        )
        val result = calculator.calculateOccurrenceConsumption(med, dailySchedule(doseQty = ScaledQuantity.ofWhole(1)))

        // Crucial safety & real-world rule: 1 container used to treat both eyes = 1 container consumed!
        assertEquals(ScaledQuantity.ofWhole(1), result.quantity)
        assertEquals(StockUnit.CONTAINERS, result.unit)
        assertEquals(TrackingPrecision.EXACT, result.precision)
    }

    @Test
    fun singleDoseEyeContainers_explicitTwoContainersPerAdmin_consumesTwo() {
        val med = eyeDropMedication(
            site = AdministrationSite.BOTH_EYES,
            leftQty = ScaledQuantity.ofWhole(1),
            rightQty = ScaledQuantity.ofWhole(1),
            containersPerAdmin = ScaledQuantity.ofWhole(2),
            packageType = PackageType.BOX_SINGLE_DOSE,
            unit = StockUnit.CONTAINERS,
            contentsPerPackage = ScaledQuantity.ofWhole(60),
            discardDays = null,
            precision = TrackingPrecision.EXACT
        )
        val result = calculator.calculateOccurrenceConsumption(med, dailySchedule(doseQty = ScaledQuantity.ofWhole(2)))

        assertEquals(ScaledQuantity.ofWhole(2), result.quantity)
        assertEquals(StockUnit.CONTAINERS, result.unit)
    }

    @Test
    fun gelOrOintment_byApplications_consumesApplicationsWithEstimatedPrecision() {
        val gel = Medication(
            id = "med-gel-1",
            name = "Night Eye Gel",
            strength = "0.2%",
            form = MedicationForm.TOPICAL_CREAM,
            stockUnit = StockUnit.APPLICATIONS,
            openingStock = ScaledQuantity.ZERO,
            lowStockThreshold = ScaledQuantity.ofWhole(5),
            recordingMode = RecordingMode.CONFIRM,
            packagingConfig = PackagingConfig(
                packageType = PackageType.TUBE,
                consumptionUnit = StockUnit.APPLICATIONS,
                contentsPerPackage = ScaledQuantity.ofWhole(30),
                netContentLabel = "5 g",
                discardAfterOpeningDays = 28,
                trackingPrecision = TrackingPrecision.ESTIMATED,
                gelTrackingMode = GelTrackingMode.ESTIMATED_APPLICATIONS
            ),
            administrationProfile = AdministrationProfile(site = AdministrationSite.BOTH_EYES)
        )

        val result = calculator.calculateOccurrenceConsumption(
            gel,
            dailySchedule(medId = gel.id, doseQty = ScaledQuantity.ofWhole(1), times = listOf(1320))
        )

        assertEquals(ScaledQuantity.ofWhole(1), result.quantity)
        assertEquals(StockUnit.APPLICATIONS, result.unit)
        assertEquals(TrackingPrecision.ESTIMATED, result.precision)
    }

    @Test
    fun gelOrOintment_tubeCountOnly_consumesZeroPerDoseAndMarksUnknownPrecision() {
        val gel = Medication(
            id = "med-gel-2",
            name = "Barrier Ointment",
            strength = "5 g",
            form = MedicationForm.TOPICAL_CREAM,
            stockUnit = StockUnit.APPLICATIONS,
            openingStock = ScaledQuantity.ZERO,
            lowStockThreshold = ScaledQuantity.ofWhole(1),
            recordingMode = RecordingMode.CONFIRM,
            packagingConfig = PackagingConfig(
                packageType = PackageType.TUBE,
                consumptionUnit = StockUnit.APPLICATIONS,
                contentsPerPackage = null,
                netContentLabel = "5 g",
                discardAfterOpeningDays = 28,
                trackingPrecision = TrackingPrecision.UNKNOWN,
                gelTrackingMode = GelTrackingMode.TUBE_COUNT_ONLY
            )
        )

        val result = calculator.calculateOccurrenceConsumption(
            gel,
            dailySchedule(medId = gel.id, doseQty = ScaledQuantity.ofWhole(1), times = listOf(1320))
        )

        assertEquals(ScaledQuantity.ZERO, result.quantity)
        assertEquals(TrackingPrecision.UNKNOWN, result.precision)
    }

    @Test
    fun unitIntegrityValidator_rejectsImplicitMlToDropsWithoutExplicitDropCapacity() {
        val validation = validator.validatePackagePurchaseConfig(
            packageType = PackageType.BOTTLE,
            consumptionUnit = StockUnit.DROPS,
            contentsPerPackage = null
        )
        assertFalse(validation.isValid)
        assertTrue(validation.reason!!.contains("drops per"))
    }

    @Test
    fun unitIntegrityValidator_rejectsCrossUnitPackageArithmetic() {
        assertThrows(IncompatiblePackageUnitException::class.java) {
            validator.requireCompatibleConsumptionUnit(
                purchaseOrLotUnit = StockUnit.BOTTLES,
                consumptionUnit = StockUnit.DROPS
            )
        }
        assertThrows(IncompatiblePackageUnitException::class.java) {
            validator.requireCompatibleConsumptionUnit(
                purchaseOrLotUnit = StockUnit.ML,
                consumptionUnit = StockUnit.DROPS
            )
        }
        assertThrows(IncompatiblePackageUnitException::class.java) {
            validator.requireCompatibleConsumptionUnit(
                purchaseOrLotUnit = StockUnit.GRAMS,
                consumptionUnit = StockUnit.APPLICATIONS
            )
        }
    }

    @Test
    fun expandPackagesToConsumptionUnits_computesExactScaledTotal() {
        val total = validator.expandPackageCountToUnits(
            packageCount = 3,
            contentsPerPackage = ScaledQuantity.ofWhole(300)
        )
        assertEquals(ScaledQuantity.ofWhole(900), total)
    }
}
