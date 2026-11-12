package com.example.nuvilo.domain.stock

import com.example.nuvilo.domain.model.AdministrationSite
import com.example.nuvilo.domain.model.GelTrackingMode
import com.example.nuvilo.domain.model.Medication
import com.example.nuvilo.domain.model.MedicationForm
import com.example.nuvilo.domain.model.PackageType
import com.example.nuvilo.domain.model.ScaledQuantity
import com.example.nuvilo.domain.model.ScheduleDefinition
import com.example.nuvilo.domain.model.StockUnit
import com.example.nuvilo.domain.model.TrackingPrecision

class IncompatiblePackageUnitException(message: String) : IllegalArgumentException(message)

data class OccurrenceConsumption(
    val quantity: ScaledQuantity,
    val unit: StockUnit,
    val precision: TrackingPrecision,
    val summaryLabel: String
)

data class PackageValidationResult(
    val isValid: Boolean,
    val reason: String? = null
)

/**
 * Validates that Nuvilo never fabricates medically unsafe or dimensionally invalid conversions
 * (such as assuming 1 ml == 20 drops or converting grams to applications automatically).
 */
class UnitIntegrityValidator {

    fun validatePackagePurchaseConfig(
        packageType: PackageType,
        consumptionUnit: StockUnit,
        contentsPerPackage: ScaledQuantity?
    ): PackageValidationResult {
        if (packageType == PackageType.BOTTLE && consumptionUnit == StockUnit.DROPS) {
            if (contentsPerPackage == null || !contentsPerPackage.isPositive()) {
                return PackageValidationResult(
                    isValid = false,
                    reason = "Please specify how many usable drops per bottle this medication contains (check leaflet or estimate) instead of assuming drops per ml."
                )
            }
        }
        if ((packageType == PackageType.TUBE || packageType == PackageType.JAR) &&
            consumptionUnit == StockUnit.APPLICATIONS &&
            (contentsPerPackage == null || !contentsPerPackage.isPositive())
        ) {
            return PackageValidationResult(
                isValid = false,
                reason = "Please enter estimated applications per tube/jar, or switch gel tracking to tube-count-only."
            )
        }
        if ((packageType == PackageType.BOX_SINGLE_DOSE || packageType == PackageType.BLISTER_PACK || packageType == PackageType.PATCH_BOX) &&
            (contentsPerPackage == null || !contentsPerPackage.isPositive())
        ) {
            return PackageValidationResult(
                isValid = false,
                reason = "Please enter the number of usable ${consumptionUnit.displayName.lowercase()} per package."
            )
        }
        return PackageValidationResult(isValid = true)
    }

    fun requireCompatibleConsumptionUnit(
        purchaseOrLotUnit: StockUnit,
        consumptionUnit: StockUnit
    ) {
        if (purchaseOrLotUnit != consumptionUnit) {
            throw IncompatiblePackageUnitException(
                "Cannot combine ${purchaseOrLotUnit.displayName} directly with ${consumptionUnit.displayName} without an explicit user-configured package capacity."
            )
        }
    }

    fun expandPackageCountToUnits(
        packageCount: Int,
        contentsPerPackage: ScaledQuantity
    ): ScaledQuantity {
        require(packageCount > 0) { "Package count must be positive" }
        require(contentsPerPackage.isPositive()) { "Package contents must be positive" }
        return ScaledQuantity(contentsPerPackage.rawScaledValue * packageCount.toLong())
    }
}

/**
 * Computes how much usable medication each scheduled administration actually consumes,
 * cleanly handling multi-dose eye drops (left/right/both), single-dose eye containers
 * (without blindly multiplying containers by 2 eyes), and topical gels/ointments.
 */
class ConsumptionCalculator {

    fun calculateOccurrenceConsumption(
        medication: Medication,
        schedule: ScheduleDefinition?
    ): OccurrenceConsumption {
        val baseScheduleQty = schedule?.doseQuantityPerOccurrence ?: ScaledQuantity.ofWhole(1)
        val unit = medication.effectiveConsumptionUnit
        val packaging = medication.packagingConfig
        val admin = medication.administrationProfile

        val defaultPrecision = packaging?.trackingPrecision ?: if (medication.form.isEstimatedConsumption) {
            TrackingPrecision.ESTIMATED
        } else {
            TrackingPrecision.EXACT
        }

        // 1. Single-dose containers: treated eyes and container count are distinct!
        if (packaging?.packageType == PackageType.BOX_SINGLE_DOSE ||
            packaging?.packageType == PackageType.SINGLE_DOSE_UNIT ||
            unit == StockUnit.CONTAINERS
        ) {
            val containerQty = admin.containersPerAdministration ?: baseScheduleQty
            val siteNote = when (admin.site) {
                AdministrationSite.LEFT_EYE -> "Left eye"
                AdministrationSite.RIGHT_EYE -> "Right eye"
                AdministrationSite.BOTH_EYES -> "Both eyes"
                else -> null
            }
            val label = buildString {
                append("${containerQty.toDisplayString()} ${unit.abbreviation}")
                if (siteNote != null) append(" ($siteNote)")
            }
            return OccurrenceConsumption(
                quantity = containerQty,
                unit = unit,
                precision = packaging?.trackingPrecision ?: TrackingPrecision.EXACT,
                summaryLabel = label
            )
        }

        // 2. Topical gels / ointments (estimated applications vs grams vs tube-count-only)
        if (medication.form == MedicationForm.TOPICAL_CREAM && packaging?.gelTrackingMode != null) {
            return when (packaging.gelTrackingMode) {
                GelTrackingMode.TUBE_COUNT_ONLY -> OccurrenceConsumption(
                    quantity = ScaledQuantity.ZERO,
                    unit = unit,
                    precision = TrackingPrecision.UNKNOWN,
                    summaryLabel = "Apply as directed (tube-level tracking)"
                )
                GelTrackingMode.ESTIMATED_APPLICATIONS -> OccurrenceConsumption(
                    quantity = baseScheduleQty,
                    unit = StockUnit.APPLICATIONS,
                    precision = TrackingPrecision.ESTIMATED,
                    summaryLabel = "${baseScheduleQty.toDisplayString()} app (~estimated)"
                )
                GelTrackingMode.GRAMS -> OccurrenceConsumption(
                    quantity = baseScheduleQty,
                    unit = StockUnit.GRAMS,
                    precision = TrackingPrecision.APPROXIMATE,
                    summaryLabel = "${baseScheduleQty.toDisplayString()} g"
                )
            }
        }

        // 3. Multi-dose ophthalmic drops with anatomical site support
        if (medication.form == MedicationForm.DROPS &&
            unit == StockUnit.DROPS &&
            admin.site != AdministrationSite.NONE &&
            admin.site != AdministrationSite.CUSTOM
        ) {
            return when (admin.site) {
                AdministrationSite.LEFT_EYE -> {
                    val left = admin.leftEyeQuantity ?: baseScheduleQty
                    OccurrenceConsumption(
                        quantity = left,
                        unit = StockUnit.DROPS,
                        precision = defaultPrecision,
                        summaryLabel = "${left.toDisplayString()} drops (left eye)"
                    )
                }
                AdministrationSite.RIGHT_EYE -> {
                    val right = admin.rightEyeQuantity ?: baseScheduleQty
                    OccurrenceConsumption(
                        quantity = right,
                        unit = StockUnit.DROPS,
                        precision = defaultPrecision,
                        summaryLabel = "${right.toDisplayString()} drops (right eye)"
                    )
                }
                AdministrationSite.BOTH_EYES -> {
                    val left = admin.leftEyeQuantity ?: ScaledQuantity.ofWhole(1)
                    val right = admin.rightEyeQuantity ?: ScaledQuantity.ofWhole(1)
                    val total = left + right
                    OccurrenceConsumption(
                        quantity = total,
                        unit = StockUnit.DROPS,
                        precision = defaultPrecision,
                        summaryLabel = "${total.toDisplayString()} drops (${left.toDisplayString()} left + ${right.toDisplayString()} right)"
                    )
                }
                else -> OccurrenceConsumption(
                    quantity = baseScheduleQty,
                    unit = unit,
                    precision = defaultPrecision,
                    summaryLabel = "${baseScheduleQty.toDisplayString()} ${unit.abbreviation}"
                )
            }
        }

        // 4. Standard tablets, capsules, liquid ml, patches, puffs, injections
        return OccurrenceConsumption(
            quantity = baseScheduleQty,
            unit = unit,
            precision = defaultPrecision,
            summaryLabel = "${baseScheduleQty.toDisplayString()} ${unit.abbreviation}"
        )
    }
}
