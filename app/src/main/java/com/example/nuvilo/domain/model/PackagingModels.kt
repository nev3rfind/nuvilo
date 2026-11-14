package com.example.nuvilo.domain.model

/**
 * Physical container / packaging format purchased or dispensed at the pharmacy.
 * Intentionally separated from [StockUnit] (which measures usable consumption doses).
 */
enum class PackageType(
    val displayName: String,
    val pluralName: String,
    val defaultStockUnit: StockUnit
) {
    BOTTLE("Bottle", "Bottles", StockUnit.ML),
    BOX_SINGLE_DOSE("Box of single-dose containers", "Boxes", StockUnit.CONTAINERS),
    SINGLE_DOSE_UNIT("Single-dose container", "Containers", StockUnit.CONTAINERS),
    TUBE("Tube", "Tubes", StockUnit.GRAMS),
    JAR("Jar", "Jars", StockUnit.GRAMS),
    BLISTER_PACK("Blister pack", "Packs", StockUnit.TABLETS),
    INHALER("Inhaler", "Inhalers", StockUnit.PUFFS),
    VIAL("Vial / Pen", "Vials", StockUnit.UNITS),
    PATCH_BOX("Box of patches", "Boxes", StockUnit.PATCHES),
    CUSTOM("Package", "Packages", StockUnit.OTHER);

    /**
     * Whether an unopened package of this type must not be silently unsealed/auto-opened
     * by software upon dose administration without explicit user confirmation.
     * Bottles, tubes, jars, inhalers, and vials have physical seals and post-opening expiry timers.
     */
    val requiresExplicitOpeningConfirmation: Boolean
        get() = this == BOTTLE || this == TUBE || this == JAR || this == INHALER || this == VIAL
}

/**
 * Lifecycle state of an individual physical package / lot.
 */
enum class PackageStatus(val displayName: String) {
    UNOPENED("Unopened"),
    OPENED("Opened"),
    EMPTY("Empty"),
    EXPIRED("Expired"),
    DISCARDED("Discarded"),
    DAMAGED("Damaged"),
    LOST("Lost");

    val isEligibleForConsumption: Boolean
        get() = this == UNOPENED || this == OPENED
}

/**
 * Administration target / anatomical site for medications such as eye drops or topicals.
 */
enum class AdministrationSite(val displayName: String) {
    NONE("Standard / Oral"),
    LEFT_EYE("Left eye"),
    RIGHT_EYE("Right eye"),
    BOTH_EYES("Both eyes"),
    CUSTOM("Custom site")
}

/**
 * Tracking mode for gels, ointments, and creams where dose weight in grams may not be measurable.
 */
enum class GelTrackingMode(val displayName: String) {
    GRAMS("Measured grams"),
    ESTIMATED_APPLICATIONS("Estimated applications per tube"),
    TUBE_COUNT_ONLY("Tube count & expiry only")
}

/**
 * Communicates clearly to the user how accurate the displayed balance and days-remaining forecast are.
 */
enum class TrackingPrecision(val badgeLabel: String, val description: String) {
    EXACT("Exact", "Tracked in discrete countable units."),
    ESTIMATED("Estimated", "Estimated from configured drops or applications per package."),
    APPROXIMATE("Approximate", "Approximate measurement or liquid volume estimate."),
    UNKNOWN("Unknown", "Package capacity or consumption rate is not yet configured.")
}

/**
 * Real-world medication packaging and shelf-life configuration.
 */
data class PackagingConfig(
    val packageType: PackageType = PackageType.CUSTOM,
    val consumptionUnit: StockUnit = StockUnit.TABLETS,
    val contentsPerPackage: ScaledQuantity? = null,
    val netContentLabel: String? = null,
    val discardAfterOpeningDays: Int? = null,
    val trackingPrecision: TrackingPrecision = TrackingPrecision.EXACT,
    val gelTrackingMode: GelTrackingMode? = null
)

/**
 * Returns a clinical, sensible default [PackagingConfig] for a given [MedicationForm] and [StockUnit].
 * Ensures that 100% of medications in Nuvilo understand their packaging format (e.g. bottles for eye drops,
 * blister packs for tablets, tubes for ointments) rather than exposing raw drop or milliliter counters.
 */
fun inferDefaultPackagingConfig(form: MedicationForm, stockUnit: StockUnit): PackagingConfig {
    return when (form) {
        MedicationForm.DROPS -> PackagingConfig(
            packageType = PackageType.BOTTLE,
            consumptionUnit = StockUnit.DROPS,
            contentsPerPackage = ScaledQuantity.ofWhole(300),
            netContentLabel = "10 ml",
            discardAfterOpeningDays = 28,
            trackingPrecision = TrackingPrecision.ESTIMATED
        )
        MedicationForm.LIQUID_ML -> PackagingConfig(
            packageType = PackageType.BOTTLE,
            consumptionUnit = StockUnit.ML,
            contentsPerPackage = ScaledQuantity.ofWhole(100),
            netContentLabel = "100 ml",
            discardAfterOpeningDays = null,
            trackingPrecision = TrackingPrecision.EXACT
        )
        MedicationForm.TABLET, MedicationForm.CAPSULE -> PackagingConfig(
            packageType = PackageType.BLISTER_PACK,
            consumptionUnit = if (form == MedicationForm.CAPSULE) StockUnit.CAPSULES else StockUnit.TABLETS,
            contentsPerPackage = ScaledQuantity.ofWhole(28),
            netContentLabel = "28 units",
            discardAfterOpeningDays = null,
            trackingPrecision = TrackingPrecision.EXACT
        )
        MedicationForm.INHALER_PUFFS -> PackagingConfig(
            packageType = PackageType.INHALER,
            consumptionUnit = StockUnit.PUFFS,
            contentsPerPackage = ScaledQuantity.ofWhole(120),
            netContentLabel = "120 actuations",
            discardAfterOpeningDays = null,
            trackingPrecision = TrackingPrecision.ESTIMATED
        )
        MedicationForm.TOPICAL_CREAM -> PackagingConfig(
            packageType = PackageType.TUBE,
            consumptionUnit = StockUnit.GRAMS,
            contentsPerPackage = ScaledQuantity.ofWhole(30),
            netContentLabel = "30 g",
            discardAfterOpeningDays = 28,
            trackingPrecision = TrackingPrecision.ESTIMATED,
            gelTrackingMode = GelTrackingMode.ESTIMATED_APPLICATIONS
        )
        MedicationForm.INJECTION -> PackagingConfig(
            packageType = PackageType.VIAL,
            consumptionUnit = StockUnit.UNITS,
            contentsPerPackage = ScaledQuantity.ofWhole(100),
            netContentLabel = "100 units",
            discardAfterOpeningDays = null,
            trackingPrecision = TrackingPrecision.EXACT
        )
        MedicationForm.PATCH -> PackagingConfig(
            packageType = PackageType.PATCH_BOX,
            consumptionUnit = StockUnit.PATCHES,
            contentsPerPackage = ScaledQuantity.ofWhole(7),
            netContentLabel = "7 patches",
            discardAfterOpeningDays = null,
            trackingPrecision = TrackingPrecision.EXACT
        )
        else -> PackagingConfig(
            packageType = when (stockUnit) {
                StockUnit.TABLETS, StockUnit.CAPSULES -> PackageType.BLISTER_PACK
                StockUnit.ML, StockUnit.DROPS -> PackageType.BOTTLE
                StockUnit.PUFFS -> PackageType.INHALER
                StockUnit.GRAMS -> PackageType.TUBE
                StockUnit.CONTAINERS -> PackageType.BOX_SINGLE_DOSE
                StockUnit.PATCHES -> PackageType.PATCH_BOX
                StockUnit.UNITS -> PackageType.VIAL
                else -> PackageType.CUSTOM
            },
            consumptionUnit = stockUnit,
            contentsPerPackage = ScaledQuantity.ofWhole(28),
            trackingPrecision = TrackingPrecision.EXACT
        )
    }
}

/**
 * Real-world administration parameters (eye selection, asymmetric drop counts, single-dose containers).
 */
data class AdministrationProfile(
    val site: AdministrationSite = AdministrationSite.NONE,
    val leftEyeQuantity: ScaledQuantity? = null,
    val rightEyeQuantity: ScaledQuantity? = null,
    val containersPerAdministration: ScaledQuantity? = null
)

/**
 * Structured clinical / patient reasons for marking a package as depleted early.
 */
enum class DepletionReason(val displayName: String, val ledgerReasonCode: String) {
    EMPTY_NO_DROPS_LEFT("Bottle is completely empty", "BOTTLE_EMPTY"),
    EXCESSIVE_DISPENSING("Multiple drops / accidental spillage", "EXCESSIVE_DISPENSING"),
    LEAKING_OR_DAMAGED("Dropper damaged or bottle leaking", "BOTTLE_DAMAGED"),
    CONTAMINATED("Tip touched eye / contaminated", "CONTAMINATED"),
    LOST_OR_DISCARDED("Lost or discarded", "LOST_DISCARDED"),
    OTHER("Other reason", "OTHER");

    companion object {
        fun fromCodeOrName(codeOrName: String?): DepletionReason {
            if (codeOrName.isNullOrBlank()) return OTHER
            return entries.find {
                it.ledgerReasonCode.equals(codeOrName, ignoreCase = true) ||
                it.name.equals(codeOrName, ignoreCase = true) ||
                it.displayName.equals(codeOrName, ignoreCase = true)
            } ?: OTHER
        }
    }
}

/**
 * Result of early package depletion:
 * - [depletedLot]: The updated package lot now in EMPTY state with remaining quantity zeroed.
 * - [compensatingEntry]: Compensating WASTAGE ledger entry writing off remaining unharvested units (null if remaining was already 0).
 * - [stagedNextLot]: Next unopened FEFO lot ready for unsealing (not yet opened until confirmed).
 */
data class EarlyDepletionOutcome(
    val depletedLot: StockLot,
    val compensatingEntry: StockLedgerEntry?,
    val stagedNextLot: StockLot?
)

/**
 * Result of reversing an early package depletion:
 * - [reopenedLot]: The restored package lot back in OPENED state with previous balance restored.
 * - [reversalEntry]: Compensating ADJUSTMENT_IN ledger entry restoring the written-off balance.
 */
data class ReopenDepletionOutcome(
    val reopenedLot: StockLot,
    val reversalEntry: StockLedgerEntry
)
