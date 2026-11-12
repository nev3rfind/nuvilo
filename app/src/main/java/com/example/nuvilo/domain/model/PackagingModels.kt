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
    CUSTOM("Package", "Packages", StockUnit.OTHER)
}

/**
 * Lifecycle state of an individual physical package / lot.
 */
enum class PackageStatus(val displayName: String) {
    UNOPENED("Unopened"),
    OPENED("Opened"),
    EMPTY("Empty"),
    EXPIRED("Expired"),
    DISCARDED("Discarded");

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
 * Real-world administration parameters (eye selection, asymmetric drop counts, single-dose containers).
 */
data class AdministrationProfile(
    val site: AdministrationSite = AdministrationSite.NONE,
    val leftEyeQuantity: ScaledQuantity? = null,
    val rightEyeQuantity: ScaledQuantity? = null,
    val containersPerAdministration: ScaledQuantity? = null
)
